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

    /**
     * 游戏窗口标题关键词。
     *
     * 注意：不要使用 dhxy 作为关键词，因为浏览器页面标题、控制台标题、项目名都可能包含 DHXY，容易误判。
     */
    private static final String[] GAME_TITLE_KEYWORDS = {
            "大话西游", "大话西游2", "xy2", "xy3", "西游"
    };

    /**
     * 明显不是游戏窗口的标题 / className 关键词。
     */
    private static final String[] EXCLUDED_WINDOW_KEYWORDS = {
            "dhxy robot",
            "robot 控制台",
            "控制台",
            "google chrome",
            "chrome",
            "intellij",
            "idea",
            "github",
            "codex",
            "任务执行流程",
            "chatgpt",
            "microsoft edge",
            "edge"
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
        if (containsAny(text, EXCLUDED_WINDOW_KEYWORDS)) {
            return false;
        }
        return containsAny(text, GAME_TITLE_KEYWORDS);
    }

    private boolean containsAny(String text, String[] keywords) {
        if (text == null || text.isBlank() || keywords == null || keywords.length == 0) {
            return false;
        }
        for (String keyword : keywords) {
            if (keyword != null && !keyword.isBlank() && text.contains(keyword.toLowerCase(Locale.ROOT))) {
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
