package com.bot.dhxy.driver;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.Accessors;

import com.bot.dhxy.runner.stop.TaskSleep;

import com.bot.dhxy.config.WindowIsolationProperties;
import com.bot.dhxy.window.diagnostics.WindowInteractionMetricsService;
import com.bot.dhxy.window.model.WindowNativeBinding;
import com.bot.dhxy.window.runtime.WindowHandleParser;
import com.bot.dhxy.window.runtime.WindowRuntimeContext;
import com.bot.dhxy.window.runtime.WindowTaskContextHolder;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.win32.StdCallLibrary;
import com.sun.jna.win32.W32APIOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Sends verified keyboard shortcuts directly to the bound game HWND.
 *
 * This is intentionally narrow. Mouse input remains on the real-input path.
 */
@Slf4j
@Service
public class BoundWindowKeyboardService {

    private static final int WM_SYSKEYDOWN = 0x0104;
    private static final int WM_SYSKEYUP = 0x0105;
    private static final int VK_MENU = 0x12;
    private static final int SCAN_ALT = 0x38;

    private final WindowTaskContextHolder windowTaskContextHolder;
    private final WindowIsolationProperties windowIsolationProperties;
    private final WindowInteractionMetricsService windowInteractionMetricsService;

    public BoundWindowKeyboardService(WindowTaskContextHolder windowTaskContextHolder,
                                      WindowIsolationProperties windowIsolationProperties,
                                      WindowInteractionMetricsService windowInteractionMetricsService) {
        this.windowTaskContextHolder = windowTaskContextHolder;
        this.windowIsolationProperties = windowIsolationProperties;
        this.windowInteractionMetricsService = windowInteractionMetricsService;
    }

    public ShortcutAttempt pressAltQ() {
        return pressShortcut(AltShortcut.ALT_Q);
    }

    public ShortcutAttempt pressShortcut(AltShortcut shortcut) {
        if (shortcut == null) {
            return ShortcutAttempt.notAttempted("unsupported-shortcut");
        }
        if (!windowIsolationProperties.isHwndKeyboardActive()) {
            return ShortcutAttempt.notAttempted("disabled");
        }
        Optional<WindowRuntimeContext> contextOptional = windowTaskContextHolder.rawCurrent();
        if (contextOptional.isEmpty()) {
            return ShortcutAttempt.notAttempted("no-window-context");
        }
        WindowNativeBinding binding = contextOptional.get().getNativeBinding();
        WinDef.HWND hwnd = toHwnd(binding);
        if (hwnd == null) {
            return ShortcutAttempt.notAttempted("invalid-hwnd");
        }

        PostResult altDown = postKey(hwnd, WM_SYSKEYDOWN, VK_MENU, SCAN_ALT, true, false);
        TaskSleep.sleep(40);
        PostResult keyDown = postKey(hwnd, WM_SYSKEYDOWN, shortcut.virtualKey(), shortcut.scanCode(), true, false);
        TaskSleep.sleep(60);
        PostResult keyUp = postKey(hwnd, WM_SYSKEYUP, shortcut.virtualKey(), shortcut.scanCode(), true, true);
        TaskSleep.sleep(40);
        PostResult altUp = postKey(hwnd, WM_SYSKEYUP, VK_MENU, SCAN_ALT, false, true);
        boolean success = altDown.success() && keyDown.success() && keyUp.success() && altUp.success();
        windowInteractionMetricsService.recordHwndKeyboard(contextOptional.get().getWindowId(), shortcut.displayName(), success);
        log.info("HWND keyboard shortcut: windowId={} hwnd={} shortcut={} result={} altDown={} keyDown={} keyUp={} altUp={} title={}",
                contextOptional.get().getWindowId(), binding.getNativeHandle(), shortcut.displayName(), success,
                altDown.toLogText(), keyDown.toLogText(), keyUp.toLogText(), altUp.toLogText(), binding.getTitle());
        return new ShortcutAttempt(true, success, success ? "OK" : "post-message-failed");
    }

    private PostResult postKey(WinDef.HWND hwnd, int message, int virtualKey, int scanCode, boolean altContext, boolean keyUp) {
        long lParam = 1L | ((long) scanCode << 16);
        if (altContext) {
            lParam |= 1L << 29;
        }
        if (keyUp) {
            lParam |= 1L << 30;
            lParam |= 1L << 31;
        }
        boolean success = User32Keyboard.INSTANCE.PostMessage(hwnd, message, new WinDef.WPARAM(virtualKey), new WinDef.LPARAM(lParam));
        int lastError = Native.getLastError();
        return new PostResult(success, lastError, message, virtualKey, scanCode, lParam);
    }

    private WinDef.HWND toHwnd(WindowNativeBinding binding) {
        if (binding == null || !binding.hasNativeHandle()) {
            return null;
        }
        Long handle = WindowHandleParser.parseHandle(binding.getNativeHandle());
        if (handle == null || handle <= 0) {
            return null;
        }
        return new WinDef.HWND(new Pointer(handle));
    }

    private interface User32Keyboard extends StdCallLibrary {
        User32Keyboard INSTANCE = Native.load("user32", User32Keyboard.class, W32APIOptions.DEFAULT_OPTIONS);

        boolean PostMessage(WinDef.HWND hwnd, int msg, WinDef.WPARAM wParam, WinDef.LPARAM lParam);
    }

    @Value


    @Builder


    @AllArgsConstructor(access = AccessLevel.PUBLIC)


    @Accessors(fluent = true)


    public static class ShortcutAttempt {


        boolean attempted;


        boolean success;


        String reason;

        private static ShortcutAttempt notAttempted(String reason) {
            return new ShortcutAttempt(false, false, reason);
        }
    


    }

    @Value


    @Builder


    @AllArgsConstructor(access = AccessLevel.PUBLIC)


    @Accessors(fluent = true)


    private static class PostResult {


        boolean success;


        int lastError;


        int message;


        int virtualKey;


        int scanCode;


        long lParam;

        private String toLogText() {
            return "success=" + success
                    + ",lastError=" + lastError
                    + ",msg=0x" + Integer.toHexString(message)
                    + ",vk=0x" + Integer.toHexString(virtualKey)
                    + ",scan=0x" + Integer.toHexString(scanCode)
                    + ",lParam=0x" + Long.toHexString(lParam);
        }
    


    }

    public enum AltShortcut {
        ALT_1("Alt+1", 0x31, 0x02),
        ALT_2("Alt+2", 0x32, 0x03),
        ALT_4("Alt+4", 0x34, 0x05),
        ALT_6("Alt+6", 0x36, 0x07),
        ALT_8("Alt+8", 0x38, 0x09),
        ALT_Q("Alt+Q", 0x51, 0x10),
        ALT_T("Alt+T", 0x54, 0x14),
        ALT_O("Alt+O", 0x4F, 0x18),
        ALT_E("Alt+E", 0x45, 0x12);

        private final String displayName;
        private final int virtualKey;
        private final int scanCode;

        AltShortcut(String displayName, int virtualKey, int scanCode) {
            this.displayName = displayName;
            this.virtualKey = virtualKey;
            this.scanCode = scanCode;
        }

        public String displayName() {
            return displayName;
        }

        public int virtualKey() {
            return virtualKey;
        }

        public int scanCode() {
            return scanCode;
        }
    }
}
