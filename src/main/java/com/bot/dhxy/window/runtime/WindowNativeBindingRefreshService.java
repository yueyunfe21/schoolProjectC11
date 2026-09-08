package com.bot.dhxy.window.runtime;

import com.bot.dhxy.window.model.WindowNativeBinding;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.ptr.IntByReference;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class WindowNativeBindingRefreshService {

    private static final int TITLE_BUFFER_CHARS = 512;
    private static final int CLASS_BUFFER_CHARS = 256;

    private final NativeWindowProbe nativeWindowProbe;
    private final ConcurrentHashMap<String, String> reportedGeometryMismatches = new ConcurrentHashMap<>();
    /** G148-M2：live 与注册尺寸连续不一致的次数（按 HWND）；一致即清零。 */
    private final ConcurrentHashMap<String, Integer> geometryMismatchStreaks = new ConcurrentHashMap<>();
    /** G148-M2：连续这么多次刷新都尺寸不一致才算熔断，容忍单次抖动。 */
    private static final int GEOMETRY_FUSE_CONSECUTIVE_TRIP = 3;

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
        int liveWidth = Math.max(live.width(), 0);
        int liveHeight = Math.max(live.height(), 0);
        int width = binding.hasGeometry() ? binding.getWidth() : liveWidth;
        int height = binding.hasGeometry() ? binding.getHeight() : liveHeight;
        if (width <= 0 || height <= 0) {
            return Optional.empty();
        }
        logGeometryMismatchOnce(binding, liveWidth, liveHeight, width, height);
        return Optional.of(binding.withLiveState(
                live.title(),
                live.className(),
                live.processId(),
                live.x(),
                live.y(),
                width,
                height));
    }

    private void logGeometryMismatchOnce(WindowNativeBinding binding,
                                         int liveWidth,
                                         int liveHeight,
                                         int adoptedWidth,
                                         int adoptedHeight) {
        String handle = binding.getNativeHandle();
        if (liveWidth == adoptedWidth && liveHeight == adoptedHeight) {
            reportedGeometryMismatches.remove(handle);
            geometryMismatchStreaks.remove(handle);
            return;
        }
        geometryMismatchStreaks.merge(handle, 1, Integer::sum);
        String mismatch = liveWidth + "x" + liveHeight + "->" + adoptedWidth + "x" + adoptedHeight;
        if (mismatch.equals(reportedGeometryMismatches.put(handle, mismatch))) {
            return;
        }
        log.warn("Native geometry refresh size mismatch; preserving registered exact-window size: "
                        + "hwnd={} title={} live={}x{} registered={}x{} position=({}, {})",
                handle, binding.getTitle(), liveWidth, liveHeight,
                adoptedWidth, adoptedHeight, binding.getX(), binding.getY());
    }

    /**
     * G148-M2：该 HWND 的 live 尺寸是否已连续多次偏离注册尺寸（中途漂移熔断电平）。
     *
     * <p>refreshGeometry 的既有语义是"保注册尺寸、只告警"——漂移窗口的捕获照旧用旧尺寸，
     * 模板全部失配（G124：771 模板集体失配还按错坐标乱点）。此电平供自动重启链熔断用；
     * 尺寸一旦恢复一致即自动复位。</p>
     */
    public boolean isGeometryDriftFused(String nativeHandle) {
        if (nativeHandle == null) {
            return false;
        }
        Integer streak = geometryMismatchStreaks.get(nativeHandle);
        return streak != null && streak >= GEOMETRY_FUSE_CONSECUTIVE_TRIP;
    }

    /** G148-M2：当前 live 与注册尺寸的差异描述（如 "1117x840->1036x783"），一致时为 null。 */
    public String geometryDriftDetail(String nativeHandle) {
        return nativeHandle == null ? null : reportedGeometryMismatches.get(nativeHandle);
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
