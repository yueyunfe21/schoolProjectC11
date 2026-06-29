package com.bot.dhxy.task.xiuluo;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Source-level guard for CR91 accept-time snapshot and no-maintenance exit-prepath overlap.
 */
public class XiuluoCR91AcceptSnapshotOverlapWiringTest {

    public static void main(String[] args) throws Exception {
        Path taskSource = Path.of("src", "main", "java", "com", "bot", "dhxy", "task", "xiuluo",
                "XiuluoTaskV2.java");
        Path contextSource = Path.of("src", "main", "java", "com", "bot", "dhxy", "task", "xiuluo",
                "XiuluoRoundContext.java");
        Path trackerSource = Path.of("src", "main", "java", "com", "bot", "dhxy", "service",
                "TaskTrackerPanelService.java");
        String task = Files.readString(taskSource, StandardCharsets.UTF_8);
        String context = Files.readString(contextSource, StandardCharsets.UTF_8);
        String tracker = Files.readString(trackerSource, StandardCharsets.UTF_8);

        String scheduleMethod = extractMethod(task, "private XiuluoRoundContext scheduleAcceptObjectiveBackgroundParse(");
        assertContains(scheduleMethod, "captureAcceptWindowSnapshot");
        assertContains(scheduleMethod, "cropStoryObjectiveFromWindowSnapshotNoDetect");
        assertContains(scheduleMethod, "scheduleAcceptTrackerBackgroundParse");
        assertContains(scheduleMethod, "withAcceptParseFutures");

        String afterAcceptMethod = extractMethod(task, "private XiuluoStepOutcome afterAcceptMaintenanceCheck(");
        assertContains(afterAcceptMethod, "!isHealPetMaintenanceDue() && !isRepairEquipmentMaintenanceDue()");
        assertContains(afterAcceptMethod, "startLeavingStartMapIfPresent(");
        assertContains(afterAcceptMethod, "XiuluoPhase.TRY_TRACKER_SHORTCUT");
        assertContains(afterAcceptMethod, "after-accept-no-maintenance");

        String beforeRouteMethod = extractMethod(task, "private XiuluoStepOutcome beforeRouteMaintenanceCheck(");
        assertContains(beforeRouteMethod, "clearShortcutTrackerParseFuture");
        assertContains(beforeRouteMethod, "fresh-read tracker shortcut");

        String trackerResolveMethod = extractMethod(task, "private TaskTrackerPanelReadResult resolveShortcutTrackerPanel(");
        assertContains(trackerResolveMethod, "shortcutTrackerParseFuture()");
        assertContains(trackerResolveMethod, "state.shortcutTrackerRetryCount() == 0");
        assertContains(trackerResolveMethod, "waitForAcceptTrackerPanelResult");
        assertContains(trackerResolveMethod, "readXiuluoTrackerPanel(");

        assertContains(context, "CompletableFuture<TaskTrackerPanelReadResult> shortcutTrackerParseFuture");
        assertContains(context, "withAcceptParseFutures");
        assertContains(context, "clearShortcutTrackerParseFuture");
        assertContains(tracker, "readXiuluoTrackerPanelFromSnapshot");

        System.out.println("XiuluoCR91AcceptSnapshotOverlapWiringTest passed");
    }

    private static String extractMethod(String source, String signature) {
        int start = source.indexOf(signature);
        if (start < 0) {
            throw new AssertionError("Method signature not found: " + signature);
        }
        int brace = source.indexOf('{', start);
        if (brace < 0) {
            throw new AssertionError("Method body not found: " + signature);
        }
        int depth = 0;
        for (int i = brace; i < source.length(); i++) {
            char ch = source.charAt(i);
            if (ch == '{') {
                depth++;
            } else if (ch == '}') {
                depth--;
                if (depth == 0) {
                    return source.substring(start, i + 1);
                }
            }
        }
        throw new AssertionError("Method body not closed: " + signature);
    }

    private static void assertContains(String value, String token) {
        if (!value.contains(token)) {
            throw new AssertionError("Expected token missing: " + token);
        }
    }
}
