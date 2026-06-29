package com.bot.dhxy.service;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Source-level guard for CR102 cleanup pass reuse.
 */
public class UICleanerCleanupPassReuseWiringTest {

    public static void main(String[] args) throws Exception {
        String source = Files.readString(
                Path.of("src/main/java/com/bot/dhxy/service/UICleanerService.java"),
                StandardCharsets.UTF_8);

        require(source.contains("CleanupPass cleanupPass = CleanupPass.start()"),
                "cleanUpAll must create one same-pass cleanup frame cache");
        require(source.contains("isWorldMapOpened(cleanupPass)"),
                "cleanUpAll must use the cleanup pass for world-map detection");
        require(source.contains("closeAllGenericWindows(cleanupPass)"),
                "cleanUpAll must pass the same cleanup frame into generic close scanning");
        require(source.contains("private boolean closeAllGenericWindows(CleanupPass cleanupPass)"),
                "generic cleanup must have an internal pass-aware overload");
        require(source.contains("findGenericCloseButtonPoint(description, cleanupPass)"),
                "generic close scanning must reuse the same pass frame");
        require(source.contains("findImageInCachedRegion("),
                "cached world-map checkbox fallback must still match inside the popup ROI");
        require(source.contains("ImagePreprocessor.cropAbsoluteRect("),
                "cached ROI matching must crop from the pass frame instead of doing full-frame first-match filtering");
        require(source.contains("cleanupPass.invalidateFrame(\"map window closed\")"),
                "real map-window close must invalidate the cached frame before another close layer");
        require(source.contains("cleanupPass.invalidateFrame(\"generic window closed\")"),
                "real generic-window close must invalidate the cached frame before another close layer");
        require(!between(source,
                "private boolean closeAllGenericWindows(CleanupPass cleanupPass)",
                "private boolean clickCloseButtonOnce(")
                .contains("isWorldMapOpened()"),
                "pass-aware generic cleanup must not re-run the old duplicate world-map detector");
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
