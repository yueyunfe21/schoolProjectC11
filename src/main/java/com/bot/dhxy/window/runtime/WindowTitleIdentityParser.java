package com.bot.dhxy.window.runtime;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses player identity from the native DHXY window title.
 */
public final class WindowTitleIdentityParser {

    private static final Pattern TITLE_IDENTITY_PATTERN = Pattern.compile(
            "-\\s*(.+?)\\s*-\\s*(.+?)\\s*[（(]\\s*ID\\s*[:：]\\s*(\\d+)\\s*[）)]");

    private WindowTitleIdentityParser() {
    }

    /**
     * Parse server/name/id from a live game title.
     *
     * @param title native window title read from Windows.
     * @return parsed identity, or empty when the title is blank or not a DHXY player title.
     */
    public static Optional<WindowTitleIdentity> parse(String title) {
        if (title == null || title.isBlank()) {
            return Optional.empty();
        }
        Matcher matcher = TITLE_IDENTITY_PATTERN.matcher(title);
        if (!matcher.find()) {
            return Optional.empty();
        }
        return Optional.of(new WindowTitleIdentity(
                matcher.group(1).trim(),
                matcher.group(2).trim(),
                matcher.group(3).trim()));
    }
}
