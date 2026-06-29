package com.bot.dhxy.task.xiuluo;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Source guard for unifying normal 修罗 startup with the CR98 after-combat startup recovery.
 */
public class XiuluoStartupTrackerFirstHotStartWiringTest {

    public static void main(String[] args) throws Exception {
        Path taskSource = Path.of("src", "main", "java", "com", "bot", "dhxy", "task", "xiuluo",
                "XiuluoTaskV2.java");
        String task = Files.readString(taskSource, StandardCharsets.UTF_8);

        String execute = extractMethod(task, "public TaskRunResult execute(TaskExecutionContext executionContext)");
        require(execute.contains("resolveStartupTrackerOrReturnItem("),
                "normal startup must enter the tracker-first hot-start resolver");
        require(!execute.contains("resolveStartupTaskPanelHotStart("),
                "normal startup must not call the legacy Alt+Q task-panel hot-start resolver");

        String resolver = extractMethod(task, "private XiuluoRoundContext resolveStartupTrackerOrReturnItem(");
        String legacyResolver = extractMethod(task, "private XiuluoRoundContext resolveStartupTaskPanelHotStart(");
        require(task.substring(0, task.indexOf("private XiuluoRoundContext resolveStartupTaskPanelHotStart("))
                        .contains("@Deprecated"),
                "legacy Alt+Q task-panel hot-start resolver must be explicitly deprecated");
        require(legacyResolver.contains("Deprecated startup path"),
                "legacy Alt+Q task-panel hot-start resolver must explain it is retained only for debugging/comparison");

        require(resolver.contains("taskTrackerPanelService.readXiuluoTrackerPanel("),
                "startup resolver must read the left tracker panel first");
        require(resolver.contains("!panel.getGreenLinks().isEmpty()"),
                "startup resolver must require an actionable green tracker link");
        require(resolver.contains("withAcceptParseFutures(XiuluoPhase.AFTER_ACCEPT_MAINTENANCE_CHECK"),
                "tracker hit must continue through the existing shortcut/maintenance path");
        require(resolver.contains("tryUseStartupReturnItemOnce("),
                "tracker miss must try the task return item before the accept flow");
        String startupReturnItem = extractMethod(task, "private boolean tryUseStartupReturnItemOnce(");
        require(startupReturnItem.contains("findAndUseMainBagTaskPageItem(RETURN_ITEM_TEMPLATE"),
                "startup return-item fallback must use the one-shot task-page return item probe");
        require(startupReturnItem.contains("gameStateUtil.isSameMapName(afterReturn.mapName, START_MAP_NAME)"),
                "startup return-item fallback must verify it returned to the start map");
        require(!resolver.contains("tryReadObjectiveFromTaskPanel("),
                "startup resolver must not use the legacy Alt+Q task-panel objective OCR");

        System.out.println("XiuluoStartupTrackerFirstHotStartWiringTest passed");
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

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
