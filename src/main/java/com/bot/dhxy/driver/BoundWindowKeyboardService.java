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
import com.bot.dhxy.window.runtime.WindowNativeBindingRefreshService;
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
    private static final int WM_KEYDOWN = 0x0100;
    private static final int WM_KEYUP = 0x0101;
    private static final int WM_CHAR = 0x0102;
    private static final int VK_MENU = 0x12;
    private static final int VK_CONTROL = 0x11;
    private static final int VK_RETURN = 0x0D;
    private static final int VK_ESCAPE = 0x1B;
    private static final int SCAN_ALT = 0x38;
    private static final int SCAN_CONTROL = 0x1D;
    private static final int SCAN_RETURN = 0x1C;
    private static final int SCAN_ESCAPE = 0x01;

    private final WindowTaskContextHolder windowTaskContextHolder;
    private final WindowIsolationProperties windowIsolationProperties;
    private final WindowInteractionMetricsService windowInteractionMetricsService;
    private final WindowNativeBindingRefreshService bindingRefreshService;

    public BoundWindowKeyboardService(WindowTaskContextHolder windowTaskContextHolder,
                                      WindowIsolationProperties windowIsolationProperties,
                                      WindowInteractionMetricsService windowInteractionMetricsService,
                                      WindowNativeBindingRefreshService bindingRefreshService) {
        this.windowTaskContextHolder = windowTaskContextHolder;
        this.windowIsolationProperties = windowIsolationProperties;
        this.windowInteractionMetricsService = windowInteractionMetricsService;
        this.bindingRefreshService = bindingRefreshService;
    }

    public ShortcutAttempt pressAltQ() {
        return pressShortcut(AltShortcut.ALT_Q);
    }

    public ShortcutAttempt pressShortcut(AltShortcut shortcut) {
        if (shortcut == null) {
            return ShortcutAttempt.notAttempted("unsupported-shortcut");
        }
        if (!shortcut.backgroundHwndSupported()) {
            log.warn("HWND keyboard shortcut rejected because shortcut is not background-validated: shortcut={}",
                    shortcut.displayName());
            return ShortcutAttempt.terminalNotAttempted("unvalidated-background-shortcut");
        }
        if (!windowIsolationProperties.isHwndKeyboardActive()) {
            return ShortcutAttempt.notAttempted("disabled");
        }
        Optional<WindowRuntimeContext> contextOptional = windowTaskContextHolder.rawCurrent();
        if (contextOptional.isEmpty()) {
            return ShortcutAttempt.notAttempted("no-window-context");
        }
        WindowRuntimeContext context = contextOptional.get();
        long requestEpoch = context.getPlayerIdentityEpoch();
        Optional<WindowNativeBinding> refreshedBinding = bindingRefreshService.refreshAndCommit(context);
        if (refreshedBinding.isEmpty()) {
            log.warn("HWND keyboard shortcut skipped because live binding refresh is unavailable: windowId={} shortcut={} requestEpoch={}",
                    context.getWindowId(), shortcut.displayName(), requestEpoch);
            return ShortcutAttempt.terminalNotAttempted("live-binding-refresh-unavailable");
        }
        if (requestEpoch != context.getPlayerIdentityEpoch()) {
            log.warn("HWND keyboard shortcut skipped because live binding refresh changed player identity: windowId={} shortcut={} requestEpoch={} currentEpoch={}",
                    context.getWindowId(), shortcut.displayName(), requestEpoch, context.getPlayerIdentityEpoch());
            return ShortcutAttempt.terminalNotAttempted("player-identity-epoch-changed");
        }
        WindowNativeBinding binding = refreshedBinding.get();
        return pressShortcut(binding, context.getWindowId(), shortcut);
    }

    /**
     * Send one background-validated Alt shortcut to the supplied immutable binding.
     *
     * @param binding exact target HWND binding already frozen by the caller; never refreshed here.
     * @param windowId owning runtime window id used only for diagnostics.
     * @param shortcut closed Alt shortcut to deliver.
     * @return typed delivery attempt; no foreground fallback or retry is performed.
     */
    public ShortcutAttempt pressShortcut(WindowNativeBinding binding,
                                          String windowId,
                                          AltShortcut shortcut) {
        if (shortcut == null) {
            return ShortcutAttempt.notAttempted("unsupported-shortcut");
        }
        if (!shortcut.backgroundHwndSupported()) {
            log.warn("HWND keyboard shortcut rejected because shortcut is not background-validated: shortcut={}",
                    shortcut.displayName());
            return ShortcutAttempt.terminalNotAttempted("unvalidated-background-shortcut");
        }
        if (!windowIsolationProperties.isHwndKeyboardActive()) {
            return ShortcutAttempt.notAttempted("disabled");
        }
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
        windowInteractionMetricsService.recordHwndKeyboard(windowId, shortcut.displayName(), success);
        log.info("HWND keyboard shortcut: windowId={} hwnd={} shortcut={} result={} altDown={} keyDown={} keyUp={} altUp={} title={}",
                windowId, binding.getNativeHandle(), shortcut.displayName(), success,
                altDown.toLogText(), keyDown.toLogText(), keyUp.toLogText(), altUp.toLogText(), binding.getTitle());
        return new ShortcutAttempt(true, success, success ? "OK" : "post-message-failed", false);
    }

    /**
     * Post exactly one modifier transition to the supplied immutable HWND binding.
     *
     * @param binding exact target HWND binding already frozen by the turn action; never refreshed here.
     * @param windowId owning runtime window id used only for diagnostics.
     * @param key closed modifier key; V1 supports CONTROL only.
     * @param transition DOWN or UP transition; UP remains callable while the thread is interrupted.
     * @return typed one-post attempt with no retry or foreground fallback.
     */
    public KeyTransitionAttempt transitionModifier(WindowNativeBinding binding,
                                                   String windowId,
                                                   ModifierKey key,
                                                   KeyTransition transition) {
        if (key != ModifierKey.CONTROL || transition == null) {
            return KeyTransitionAttempt.notAttempted("unsupported-modifier-transition");
        }
        if (!windowIsolationProperties.isHwndKeyboardActive()) {
            return KeyTransitionAttempt.notAttempted("disabled");
        }
        WinDef.HWND hwnd = toHwnd(binding);
        if (hwnd == null) {
            return KeyTransitionAttempt.notAttempted("invalid-hwnd");
        }

        boolean keyUp = transition == KeyTransition.UP;
        PostResult post = postKey(
                hwnd,
                keyUp ? WM_KEYUP : WM_KEYDOWN,
                VK_CONTROL,
                SCAN_CONTROL,
                false,
                keyUp);
        windowInteractionMetricsService.recordHwndKeyboard(
                windowId, "Ctrl " + transition.name().toLowerCase(), post.success());
        log.info("HWND modifier transition: windowId={} hwnd={} key={} transition={} result={} post={} title={}",
                windowId, binding.getNativeHandle(), key, transition, post.success(), post.toLogText(), binding.getTitle());
        return new KeyTransitionAttempt(true, post.success(), post.success() ? "OK" : "post-message-failed");
    }

    /**
     * Send one background-validated Ctrl+letter chord to the supplied immutable binding.
     *
     * @param binding exact target HWND binding already frozen by the caller; never refreshed here.
     * @param windowId owning runtime window id used only for diagnostics.
     * @param shortcut closed Ctrl chord to deliver.
     * @return typed delivery attempt; no foreground fallback or retry is performed.
     */
    public ShortcutAttempt pressControlShortcut(WindowNativeBinding binding,
                                                String windowId,
                                                ControlShortcut shortcut) {
        if (shortcut == null) {
            return ShortcutAttempt.notAttempted("unsupported-control-shortcut");
        }
        if (!shortcut.backgroundHwndSupported()) {
            log.warn("HWND keyboard chord rejected because chord is not background-validated: chord={}",
                    shortcut.displayName());
            return ShortcutAttempt.terminalNotAttempted("unvalidated-background-chord");
        }
        if (!windowIsolationProperties.isHwndKeyboardActive()) {
            return ShortcutAttempt.notAttempted("disabled");
        }
        WinDef.HWND hwnd = toHwnd(binding);
        if (hwnd == null) {
            return ShortcutAttempt.notAttempted("invalid-hwnd");
        }

        PostResult ctrlDown = postKey(hwnd, WM_KEYDOWN, VK_CONTROL, SCAN_CONTROL, false, false);
        TaskSleep.sleep(40);
        PostResult keyDown = postKey(hwnd, WM_KEYDOWN, shortcut.virtualKey(), shortcut.scanCode(), false, false);
        TaskSleep.sleep(60);
        PostResult keyUp = postKey(hwnd, WM_KEYUP, shortcut.virtualKey(), shortcut.scanCode(), false, true);
        TaskSleep.sleep(40);
        PostResult ctrlUp = postKey(hwnd, WM_KEYUP, VK_CONTROL, SCAN_CONTROL, false, true);
        boolean success = ctrlDown.success() && keyDown.success() && keyUp.success() && ctrlUp.success();
        windowInteractionMetricsService.recordHwndKeyboard(windowId, shortcut.displayName(), success);
        log.info("HWND keyboard chord: windowId={} hwnd={} chord={} result={} ctrlDown={} keyDown={} keyUp={} ctrlUp={} title={}",
                windowId, binding.getNativeHandle(), shortcut.displayName(), success,
                ctrlDown.toLogText(), keyDown.toLogText(), keyUp.toLogText(), ctrlUp.toLogText(), binding.getTitle());
        return new ShortcutAttempt(true, success, success ? "OK" : "post-message-failed", false);
    }

    /**
     * Send one background Enter key tap to the supplied immutable binding.
     *
     * @param binding exact target HWND binding already frozen by the caller; never refreshed here.
     * @param windowId owning runtime window id used only for diagnostics.
     * @return typed delivery attempt; no foreground fallback or retry is performed.
     */
    public ShortcutAttempt pressEnter(WindowNativeBinding binding, String windowId) {
        if (!windowIsolationProperties.isHwndKeyboardActive()) {
            return ShortcutAttempt.notAttempted("disabled");
        }
        WinDef.HWND hwnd = toHwnd(binding);
        if (hwnd == null) {
            return ShortcutAttempt.notAttempted("invalid-hwnd");
        }

        PostResult keyDown = postKey(hwnd, WM_KEYDOWN, VK_RETURN, SCAN_RETURN, false, false);
        TaskSleep.sleep(40);
        PostResult keyUp = postKey(hwnd, WM_KEYUP, VK_RETURN, SCAN_RETURN, false, true);
        boolean success = keyDown.success() && keyUp.success();
        windowInteractionMetricsService.recordHwndKeyboard(windowId, "Enter", success);
        log.info("HWND keyboard Enter: windowId={} hwnd={} result={} keyDown={} keyUp={} title={}",
                windowId, binding.getNativeHandle(), success,
                keyDown.toLogText(), keyUp.toLogText(), binding.getTitle());
        return new ShortcutAttempt(true, success, success ? "OK" : "post-message-failed", false);
    }

    /**
     * Send one background Escape key tap to the supplied immutable binding.
     *
     * @param binding exact target HWND binding already frozen by the caller; never refreshed here.
     * @param windowId owning runtime window id used only for diagnostics.
     * @return typed delivery attempt; no foreground fallback or retry is performed.
     */
    public ShortcutAttempt pressEscape(WindowNativeBinding binding, String windowId) {
        if (!windowIsolationProperties.isHwndKeyboardActive()) {
            return ShortcutAttempt.notAttempted("disabled");
        }
        WinDef.HWND hwnd = toHwnd(binding);
        if (hwnd == null) {
            return ShortcutAttempt.notAttempted("invalid-hwnd");
        }

        PostResult keyDown = postKey(hwnd, WM_KEYDOWN, VK_ESCAPE, SCAN_ESCAPE, false, false);
        TaskSleep.sleep(40);
        PostResult keyUp = postKey(hwnd, WM_KEYUP, VK_ESCAPE, SCAN_ESCAPE, false, true);
        boolean success = keyDown.success() && keyUp.success();
        windowInteractionMetricsService.recordHwndKeyboard(windowId, "Escape", success);
        log.info("HWND keyboard Escape: windowId={} hwnd={} result={} keyDown={} keyUp={} title={}",
                windowId, binding.getNativeHandle(), success,
                keyDown.toLogText(), keyUp.toLogText(), binding.getTitle());
        return new ShortcutAttempt(true, success, success ? "OK" : "post-message-failed", false);
    }

    /**
     * Post one Unicode string to the supplied immutable binding as ordered background {@code WM_CHAR} messages.
     *
     * <p>Each code unit is delivered in order to the exact HWND; no clipboard, focus or foreground path is used.
     * A single failed post makes the whole attempt unsuccessful without any retry or fallback.</p>
     *
     * @param binding exact target HWND binding already frozen by the caller; never refreshed here.
     * @param windowId owning runtime window id used only for diagnostics.
     * @param text Unicode text to deliver; blank text is not attempted.
     * @return typed delivery attempt; no foreground fallback or retry is performed.
     */
    public ShortcutAttempt typeUnicodeText(WindowNativeBinding binding, String windowId, String text) {
        if (text == null || text.isEmpty()) {
            return ShortcutAttempt.notAttempted("empty-text");
        }
        if (!windowIsolationProperties.isHwndKeyboardActive()) {
            return ShortcutAttempt.notAttempted("disabled");
        }
        WinDef.HWND hwnd = toHwnd(binding);
        if (hwnd == null) {
            return ShortcutAttempt.notAttempted("invalid-hwnd");
        }

        boolean success = true;
        for (int index = 0; index < text.length(); index++) {
            if (index > 0) {
                TaskSleep.sleep(10);
            }
            char codeUnit = text.charAt(index);
            long lParam = 1L;
            boolean posted = User32Keyboard.INSTANCE.PostMessage(
                    hwnd, WM_CHAR, new WinDef.WPARAM(codeUnit), new WinDef.LPARAM(lParam));
            success = success && posted;
        }
        windowInteractionMetricsService.recordHwndKeyboard(windowId, "text[" + text.length() + "]", success);
        log.info("HWND keyboard text: windowId={} hwnd={} length={} result={} title={}",
                windowId, binding.getNativeHandle(), text.length(), success, binding.getTitle());
        return new ShortcutAttempt(true, success, success ? "OK" : "post-message-failed", false);
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


        boolean terminalFailure;

        private static ShortcutAttempt notAttempted(String reason) {
            return new ShortcutAttempt(false, false, reason, false);
        }

        private static ShortcutAttempt terminalNotAttempted(String reason) {
            return new ShortcutAttempt(false, false, reason, true);
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
        ALT_1("Alt+1", 0x31, 0x02, true),
        ALT_2("Alt+2", 0x32, 0x03, true),
        ALT_4("Alt+4", 0x34, 0x05, true),
        ALT_5("Alt+5", 0x35, 0x06, true),
        ALT_6("Alt+6", 0x36, 0x07, true),
        ALT_8("Alt+8", 0x38, 0x09, true),
        ALT_Q("Alt+Q", 0x51, 0x10, true),
        ALT_T("Alt+T", 0x54, 0x14, true),
        ALT_O("Alt+O", 0x4F, 0x18, true),
        ALT_E("Alt+E", 0x45, 0x12, true),
        ALT_A("Alt+A", 0x41, 0x1E, true),
        ALT_B("Alt+B", 0x42, 0x30, true),
        ALT_C("Alt+C", 0x43, 0x2E, true),
        ALT_U("Alt+U", 0x55, 0x16, true);

        private final String displayName;
        private final int virtualKey;
        private final int scanCode;
        private final boolean backgroundHwndSupported;

        AltShortcut(String displayName, int virtualKey, int scanCode, boolean backgroundHwndSupported) {
            this.displayName = displayName;
            this.virtualKey = virtualKey;
            this.scanCode = scanCode;
            this.backgroundHwndSupported = backgroundHwndSupported;
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

        public boolean backgroundHwndSupported() {
            return backgroundHwndSupported;
        }
    }

    public enum ControlShortcut {
        CTRL_A("Ctrl+A", 0x41, 0x1E, true),
        CTRL_U("Ctrl+U", 0x55, 0x16, true);

        private final String displayName;
        private final int virtualKey;
        private final int scanCode;
        private final boolean backgroundHwndSupported;

        ControlShortcut(String displayName, int virtualKey, int scanCode, boolean backgroundHwndSupported) {
            this.displayName = displayName;
            this.virtualKey = virtualKey;
            this.scanCode = scanCode;
            this.backgroundHwndSupported = backgroundHwndSupported;
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

        public boolean backgroundHwndSupported() {
            return backgroundHwndSupported;
        }
    }

    public enum ModifierKey {
        CONTROL
    }

    public enum KeyTransition {
        DOWN,
        UP
    }

    /** Result of one exact-HWND modifier transition attempt. */
    public record KeyTransitionAttempt(boolean attempted, boolean success, String reason) {

        private static KeyTransitionAttempt notAttempted(String reason) {
            return new KeyTransitionAttempt(false, false, reason);
        }
    }

}
