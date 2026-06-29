package com.bot.dhxy.window.runtime;

import com.bot.dhxy.window.model.WindowNativeBinding;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.ptr.IntByReference;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class WindowNativeBindingRefreshService {

    private static final int TITLE_BUFFER_CHARS = 512;
    private static final int CLASS_BUFFER_CHARS = 256;

    private final NativeWindowProbe nativeWindowProbe;

    public WindowNativeBindingRefreshService() {
        this(new JnaNativeWindowProbe());
    }

    WindowNativeBindingRefreshService(NativeWindowProbe nativeWindowProbe) {
        this.nativeWindowProbe = nativeWindowProbe == null ? new JnaNativeWindowProbe() : nativeWindowProbe;
    }

    /**
     * Refresh the live native binding for a bound HWND.
     *
     * <p>CR95 needs title/class/process refreshed together with geometry. A transient blank title is
     * carried to {@link WindowRuntimeContext#setNativeBinding(WindowNativeBinding)} so preservation is
     * decided against the runtime's current binding, not a possibly stale caller-side binding.</p>
     *
     * @param binding current cached native binding.
     * @return refreshed binding, or empty when the HWND is invalid/unreadable.
     */
    public Optional<WindowNativeBinding> refreshGeometry(WindowNativeBinding binding) {
        if (binding == null || !binding.hasNativeHandle()) {
            return Optional.empty();
        }
        Long handle = WindowHandleParser.parseHandle(binding.getNativeHandle());
        if (handle == null || handle == 0L) {
            return Optional.empty();
        }
        Optional<LiveWindowSnapshot> snapshot = nativeWindowProbe.snapshot(handle);
        if (snapshot.isEmpty()) {
            return Optional.empty();
        }
        LiveWindowSnapshot live = snapshot.get();
        int width = Math.max(live.width(), 0);
        int height = Math.max(live.height(), 0);
        if (width <= 0 || height <= 0) {
            return Optional.empty();
        }
        return Optional.of(binding.withLiveState(
                live.title(),
                live.className(),
                live.processId(),
                live.x(),
                live.y(),
                width,
                height));
    }

    /**
     * Refresh and commit the live binding while holding the runtime context monitor.
     *
     * @param context window runtime that owns the current cached binding.
     * @return committed live binding, or empty when the HWND cannot be refreshed.
     */
    public Optional<WindowNativeBinding> refreshAndCommit(WindowRuntimeContext context) {
        if (context == null) {
            return Optional.empty();
        }
        /*
         * The read and commit must be one serialized section. Otherwise an older nonblank title
         * snapshot can finish after a newer drift commit and roll the runtime identity backward.
         */
        synchronized (context) {
            WindowNativeBinding binding = context.getNativeBinding();
            Optional<WindowNativeBinding> refreshed = refreshGeometry(binding);
            refreshed.ifPresent(context::setNativeBinding);
            return refreshed.map(ignored -> context.getNativeBinding());
        }
    }

    interface NativeWindowProbe {
        Optional<LiveWindowSnapshot> snapshot(long handle);
    }

    public record LiveWindowSnapshot(String title,
                                     String className,
                                     long processId,
                                     int x,
                                     int y,
                                     int width,
                                     int height) {
        public static LiveWindowSnapshot available(String title,
                                                   String className,
                                                   long processId,
                                                   int x,
                                                   int y,
                                                   int width,
                                                   int height) {
            return new LiveWindowSnapshot(title, className, processId, x, y, width, height);
        }
    }

    private static class JnaNativeWindowProbe implements NativeWindowProbe {
        @Override
        public Optional<LiveWindowSnapshot> snapshot(long handle) {
            WinDef.HWND hwnd = new WinDef.HWND(new Pointer(handle));
            if (!User32.INSTANCE.IsWindow(hwnd)) {
                return Optional.empty();
            }
            WinDef.RECT rect = new WinDef.RECT();
            if (!User32.INSTANCE.GetWindowRect(hwnd, rect)) {
                return Optional.empty();
            }
            char[] titleBuffer = new char[TITLE_BUFFER_CHARS];
            User32.INSTANCE.GetWindowText(hwnd, titleBuffer, TITLE_BUFFER_CHARS);
            char[] classBuffer = new char[CLASS_BUFFER_CHARS];
            User32.INSTANCE.GetClassName(hwnd, classBuffer, CLASS_BUFFER_CHARS);
            IntByReference processIdRef = new IntByReference();
            User32.INSTANCE.GetWindowThreadProcessId(hwnd, processIdRef);
            return Optional.of(new LiveWindowSnapshot(
                    NativeString.trimNullTerminated(titleBuffer),
                    NativeString.trimNullTerminated(classBuffer),
                    Integer.toUnsignedLong(processIdRef.getValue()),
                    rect.left,
                    rect.top,
                    Math.max(rect.right - rect.left, 0),
                    Math.max(rect.bottom - rect.top, 0)));
        }
    }

    private static class NativeString {
        private NativeString() {
        }

        private static String trimNullTerminated(char[] value) {
            if (value == null || value.length == 0) {
                return "";
            }
            int end = 0;
            while (end < value.length && value[end] != '\0') {
                end++;
            }
            return new String(value, 0, end).trim();
        }
    }
}
