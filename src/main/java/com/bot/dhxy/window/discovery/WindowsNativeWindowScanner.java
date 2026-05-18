package com.bot.dhxy.window.discovery;

import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.ptr.IntByReference;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Windows 顶层窗口扫描实现。
 */
@Component
public class WindowsNativeWindowScanner implements NativeWindowScanner {

    private static final String[] GAME_TITLE_KEYWORDS = {
            "大话西游", "dhxy", "xy2", "xy3", "西游"
    };

    @Override
    public List<NativeWindowInfo> scanWindows() {
        List<NativeWindowInfo> windows = new ArrayList<>();
        User32.INSTANCE.EnumWindows((hwnd, data) -> {
            NativeWindowInfo info = readWindow(hwnd);
            if (info != null && info.hasTitle()) {
                windows.add(info);
            }
            return true;
        }, null);
        return windows.stream()
                .sorted(Comparator.comparing(NativeWindowInfo::getTitle, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    @Override
    public List<NativeWindowInfo> scanGameWindows() {
        return scanWindows().stream()
                .filter(this::looksLikeGameWindow)
                .toList();
    }

    private NativeWindowInfo readWindow(WinDef.HWND hwnd) {
        if (hwnd == null || !User32.INSTANCE.IsWindowVisible(hwnd)) {
            return null;
        }
        int titleLength = User32.INSTANCE.GetWindowTextLength(hwnd);
        if (titleLength <= 0) {
            return null;
        }

        char[] titleBuffer = new char[titleLength + 1];
        User32.INSTANCE.GetWindowText(hwnd, titleBuffer, titleBuffer.length);
        String title = cleanNativeString(titleBuffer);
        if (title.isBlank()) {
            return null;
        }

        char[] classBuffer = new char[512];
        User32.INSTANCE.GetClassName(hwnd, classBuffer, classBuffer.length);
        String className = cleanNativeString(classBuffer);

        IntByReference processIdRef = new IntByReference();
        User32.INSTANCE.GetWindowThreadProcessId(hwnd, processIdRef);

        WinDef.RECT rect = new WinDef.RECT();
        User32.INSTANCE.GetWindowRect(hwnd, rect);
        int width = Math.max(rect.right - rect.left, 0);
        int height = Math.max(rect.bottom - rect.top, 0);

        return new NativeWindowInfo(
                toHandleText(hwnd),
                title,
                className,
                Integer.toUnsignedLong(processIdRef.getValue()),
                rect.left,
                rect.top,
                width,
                height
        );
    }

    private String toHandleText(WinDef.HWND hwnd) {
        Pointer pointer = hwnd.getPointer();
        long value = pointer == null ? 0L : Pointer.nativeValue(pointer);
        return Long.toHexString(value).toUpperCase(Locale.ROOT);
    }

    private boolean looksLikeGameWindow(NativeWindowInfo info) {
        String text = (info.getTitle() + " " + info.getClassName()).toLowerCase(Locale.ROOT);
        for (String keyword : GAME_TITLE_KEYWORDS) {
            if (text.contains(keyword.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private String cleanNativeString(char[] chars) {
        int len = 0;
        while (len < chars.length && chars[len] != 0) {
            len++;
        }
        return new String(chars, 0, len).trim();
    }
}
