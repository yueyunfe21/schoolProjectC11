package com.bot.dhxy.window.discovery;

import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.ptr.IntByReference;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Windows 顶层窗口扫描实现。
 */
@Component
@Slf4j
public class WindowsNativeWindowScanner implements NativeWindowScanner {

    private static final String[] GAME_TITLE_KEYWORDS = {
            "大话西游", "大话西游2", "西游"
    };

    /**
     * 主游戏窗口通常带有 Revision / ID 等信息；聊天窗口、工具窗口即使命中游戏标题也不应注册。
     */
    private static final String[] MAIN_GAME_WINDOW_HINTS = {
            "revision", "id:", "id："
    };

    /**
     * xy2 / xy3 只允许作为独立词出现，避免 DHXY2Robot 这种项目窗口被误判。
     */
    private static final Pattern GAME_CODE_PATTERN = Pattern.compile("(^|[^a-z0-9])xy[23]([^a-z0-9]|$)");

    private static final String[] EXCLUDED_WINDOW_KEYWORDS = {
            "dhxy robot",
            "dhxy2robot",
            "robot 控制台",
            "控制台",
            "聊天窗口",
            "chat window",
            "chat",
            "google chrome",
            "chrome",
            "intellij",
            "idea",
            "github",
            "codex",
            "任务执行流程",
            "diff for file",
            ".java",
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
        long startedAt = System.currentTimeMillis();
        List<NativeWindowInfo> allWindows = scanWindows();
        List<NativeWindowInfo> gameWindows = allWindows.stream()
                .filter(this::looksLikeGameWindow)
                .toList();
        log.info("Native game window scan done: allVisible={} gameMatched={} elapsedMs={} matches={}",
                allWindows.size(), gameWindows.size(), System.currentTimeMillis() - startedAt,
                describeWindows(gameWindows));
        return gameWindows;
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

        boolean hasGameTitle = containsAny(text, GAME_TITLE_KEYWORDS) || GAME_CODE_PATTERN.matcher(text).find();
        if (!hasGameTitle) {
            return false;
        }

        // 大话西游会产生聊天窗口等子窗口。为了避免把子窗口注册成角色窗口，优先要求主窗口特征。
        return containsAny(text, MAIN_GAME_WINDOW_HINTS);
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

    private String describeWindows(List<NativeWindowInfo> windows) {
        if (windows == null || windows.isEmpty()) {
            return "[]";
        }
        return windows.stream()
                .map(window -> window.toWindowId() + "|" + window.getTitle()
                        + "|class=" + window.getClassName()
                        + "|pid=" + window.getProcessId()
                        + "|rect=" + window.getX() + "," + window.getY() + "," + window.getWidth() + "x" + window.getHeight())
                .toList()
                .toString();
    }

    private String cleanNativeString(char[] chars) {
        int len = 0;
        while (len < chars.length && chars[len] != 0) {
            len++;
        }
        return new String(chars, 0, len).trim();
    }
}
