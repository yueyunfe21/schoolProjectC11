package com.bot.dhxy.ui;

import com.sun.jna.Native;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.win32.StdCallLibrary;
import com.sun.jna.win32.W32APIOptions;
import lombok.extern.slf4j.Slf4j;

/**
 * Applies native Windows title-bar colors for the JavaFX main window.
 *
 * <p>JavaFX CSS only controls the scene content. The caption area with minimize/maximize/close is
 * drawn by Windows, so dark mode needs to be forwarded to DWM separately.
 */
@Slf4j
final class WindowsTitleBarTheme {

    private static final String WINDOWS = "win";
    private static final int S_OK = 0;
    private static final int DWMWA_USE_IMMERSIVE_DARK_MODE = 20;
    private static final int DWMWA_USE_IMMERSIVE_DARK_MODE_OLD = 19;
    private static final int DWMWA_CAPTION_COLOR = 35;
    private static final int DWMWA_TEXT_COLOR = 36;
    private static final int COLOR_DEFAULT = 0xFFFFFFFF;
    private static final int DARK_CAPTION_COLOR = 0x00221B17;
    private static final int DARK_TEXT_COLOR = 0x00E7DCD4;
    private static final int INT_SIZE = 4;

    private WindowsTitleBarTheme() {
    }

    static void applyToWindowTitle(String windowTitle, boolean dark) {
        if (!isWindows() || windowTitle == null || windowTitle.isBlank()) {
            return;
        }
        try {
            WinDef.HWND hwnd = User32.INSTANCE.FindWindow(null, windowTitle);
            if (hwnd == null) {
                log.debug("JavaFX native title bar theme skipped because window was not found: title={}", windowTitle);
                return;
            }
            apply(hwnd, dark);
        } catch (RuntimeException e) {
            log.debug("JavaFX native title bar theme update failed: title={} dark={}", windowTitle, dark, e);
        }
    }

    private static void apply(WinDef.HWND hwnd, boolean dark) {
        int darkValue = dark ? 1 : 0;
        boolean appliedDarkMode = setAttribute(hwnd, DWMWA_USE_IMMERSIVE_DARK_MODE, darkValue);
        if (!appliedDarkMode) {
            setAttribute(hwnd, DWMWA_USE_IMMERSIVE_DARK_MODE_OLD, darkValue);
        }

        int captionColor = dark ? DARK_CAPTION_COLOR : COLOR_DEFAULT;
        int textColor = dark ? DARK_TEXT_COLOR : COLOR_DEFAULT;
        setAttribute(hwnd, DWMWA_CAPTION_COLOR, captionColor);
        setAttribute(hwnd, DWMWA_TEXT_COLOR, textColor);
    }

    private static boolean setAttribute(WinDef.HWND hwnd, int attribute, int value) {
        int result = DwmApi.INSTANCE.DwmSetWindowAttribute(hwnd, attribute, new IntByReference(value), INT_SIZE);
        return result == S_OK;
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase().contains(WINDOWS);
    }

    private interface DwmApi extends StdCallLibrary {
        DwmApi INSTANCE = Native.load("dwmapi", DwmApi.class, W32APIOptions.DEFAULT_OPTIONS);

        int DwmSetWindowAttribute(WinDef.HWND hwnd, int dwAttribute, IntByReference pvAttribute, int cbAttribute);
    }
}
