package com.bot.dhxy.service;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Source-level guard for CR104 generic UI cleanup no-match diagnostics.
 */
public class UICleanerGenericCloseNoMatchLogWiringTest {

    public static void main(String[] args) throws Exception {
        String source = Files.readString(
                Path.of("src/main/java/com/bot/dhxy/service/UICleanerService.java"),
                StandardCharsets.UTF_8);

        String finder = between(source,
                "private Point findGenericCloseButtonPoint(String description, CleanupPass cleanupPass)",
                "private boolean clickCloseButtonOnceDirect(");
        require(finder.contains("UI cleanup close button not found"),
                "generic close-button scan must log when x1/x2/x3 are not found");
        require(finder.contains("screenPath"),
                "generic close-button no-match log must include the screenshot path for replay/debug");
        require(finder.contains("description"),
                "generic close-button no-match log must include the caller description");
    }

    private static String between(String source, String start, String end) {
        int startIndex = source.indexOf(start);
        if (startIndex < 0) {
            throw new AssertionError("Missing source marker: " + start);
        }
        int endIndex = source.indexOf(end, startIndex);
        if (endIndex < 0) {
            throw new AssertionError("Missing source marker: " + end);
        }
        return source.substring(startIndex, endIndex);
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
