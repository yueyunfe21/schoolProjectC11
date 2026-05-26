package com.bot.dhxy.window.runtime;

/**
 * Parses native HWND text captured from the window scanner.
 *
 * The scanner stores handles in hexadecimal form without a 0x prefix, for example
 * 2FA0A0A or 970F56. Window bindings normalize those values to unsigned decimal
 * text, so consumers must accept both forms.
 */
public final class WindowHandleParser {

    private WindowHandleParser() {
    }

    public static Long parseHexHandle(String handleText) {
        if (handleText == null || handleText.isBlank()) {
            return null;
        }
        String value = handleText.trim();
        try {
            if (value.startsWith("0x") || value.startsWith("0X")) {
                value = value.substring(2);
            }
            return Long.parseUnsignedLong(value, 16);
        } catch (Exception e) {
            return null;
        }
    }

    public static Long parseHandle(String handleText) {
        if (handleText == null || handleText.isBlank()) {
            return null;
        }
        String value = handleText.trim();
        try {
            if (value.startsWith("0x") || value.startsWith("0X")) {
                return Long.parseUnsignedLong(value.substring(2), 16);
            }
            if (value.matches(".*[a-fA-F].*")) {
                return Long.parseUnsignedLong(value, 16);
            }
            return Long.parseUnsignedLong(value);
        } catch (Exception e) {
            return null;
        }
    }
}
