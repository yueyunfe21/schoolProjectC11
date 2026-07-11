package com.bot.dhxy.service;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Source-level guard for the 五环 tracker green-link selector.
 *
 * <p>五环 tracker links cannot use the generic cloud `FIRST_LINK` policy. The business rule is to
 * keep the old dedicated selector that chooses the pathing-name segment from the 五环 green text.</p>
 */
public final class WuhuanTrackerDedicatedLinkSourceGuard {

    private static final Path TASK_TRACKER_PANEL_SERVICE = Path.of(
            "src/main/java/com/bot/dhxy/service/TaskTrackerPanelService.java");

    private WuhuanTrackerDedicatedLinkSourceGuard() {
    }

    public static void main(String[] args) throws Exception {
        String source = Files.readString(TASK_TRACKER_PANEL_SERVICE, StandardCharsets.UTF_8);

        require(!source.contains("readWuhuanClickFromCloudPanel("),
                "五环 must not have a helper that routes tracker links through cloud FIRST_LINK");

        String nextGreen = between(source,
                "public Point findWuhuanNextGreenClickPoint()",
                "public TaskTrackerPanelReadResult readWuhuanTrackerTitle(");
        requireDedicatedWuhuanSelector(nextGreen, "findWuhuanNextGreenClickPoint");

        String prepare = between(source,
                "public TaskTrackerPanelPrepareResult prepareWuhuanPathingLink(",
                "private TaskTrackerPanelNegativeResult wuhuanNegativeFromCloudDecision(");
        requireDedicatedWuhuanSelector(prepare, "prepareWuhuanPathingLink");

        String dedicatedSelector = between(source,
                "private Point findWuhuanTrackerGreenClickPoint(",
                "private WubeiGreenLinkScan scanWubeiTrackerGreenLinks(");
        require(dedicatedSelector.contains("scanWuhuanTrackerGreenLinks("),
                "五环 dedicated selector must scan 五环 green links");
        require(dedicatedSelector.contains("findWuhuanPathingNameSegment(scan)"),
                "五环 dedicated selector must choose the pathing-name segment, not first link");
    }

    private static void requireDedicatedWuhuanSelector(String methodBody, String methodName) {
        require(methodBody.contains("findWuhuanTrackerGreenClickPoint("),
                methodName + " must use 五环 dedicated green-link selector");
        require(!methodBody.contains("readTrackerPanelImageFromCloud("),
                methodName + " must not call cloud tracker reader for 五环 green-link selection");
        require(!methodBody.contains("\"FIRST_LINK\""),
                methodName + " must not use generic FIRST_LINK for 五环");
        require(!methodBody.contains("\"TASK_AWARE_FIRST_LINK\""),
                methodName + " must not use generic task-aware first-link policy for 五环");
    }

    private static String between(String source, String start, String end) {
        int startIndex = source.indexOf(start);
        if (startIndex < 0) {
            throw new AssertionError("Missing source marker: " + start);
        }
        int endIndex = source.indexOf(end, startIndex);
        if (endIndex < 0) {
            throw new AssertionError("Missing source end marker: " + end);
        }
        return source.substring(startIndex, endIndex);
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
