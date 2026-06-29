package com.bot.dhxy.task;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Source-level guard for CR98 task-side after-combat startup recovery.
 */
public class AfterCombatStartupRecoveryWiringTest {

    public static void main(String[] args) throws Exception {
        Path root = Path.of("").toAbsolutePath();
        String xiuluo = Files.readString(root.resolve(
                "src/main/java/com/bot/dhxy/task/xiuluo/XiuluoTaskV2.java"), StandardCharsets.UTF_8);
        String wubei = Files.readString(root.resolve(
                "src/main/java/com/bot/dhxy/task/wubei/WubeiTask.java"), StandardCharsets.UTF_8);

        String xiuluoExecute = between(xiuluo,
                "public TaskRunResult execute(TaskExecutionContext executionContext)",
                "private TaskRunResult runRoundPhases(");
        require(xiuluoExecute.contains("context.isAfterCombatExitStartup()"),
                "Xiuluo must detect AFTER_COMBAT_EXIT_STARTUP at first round startup");
        require(xiuluoExecute.contains("resolveStartupTrackerOrReturnItem(context, XiuluoRoundContext.start(round),"),
                "Xiuluo must route first-round startup through the unified tracker/return-item resolver");
        require(xiuluoExecute.contains("\"after-combat-exit-startup-screen-resume\""),
                "Xiuluo after-combat startup must use the explicit startup screen-resume source");

        String xiuluoRecovery = between(xiuluo,
                "private XiuluoRoundContext resolveStartupTrackerOrReturnItem(",
                "private XiuluoRoundContext resolveStartupTaskPanelHotStart(");
        require(xiuluoRecovery.indexOf("readXiuluoTrackerPanel(")
                        < xiuluoRecovery.indexOf("tryUseStartupReturnItemOnce("),
                "Xiuluo after-combat startup must read tracker before trying return item");
        require(xiuluoRecovery.contains("withAcceptParseFutures(XiuluoPhase.AFTER_ACCEPT_MAINTENANCE_CHECK"),
                "Xiuluo tracker hit must continue through existing maintenance + tracker shortcut flow");
        String xiuluoReturnFallback = between(xiuluo,
                "private boolean tryUseStartupReturnItemOnce(",
                "private boolean useReturnItemAndVerifyStartMap(");
        require(xiuluoReturnFallback.contains("findAndUseMainBagTaskPageItem(RETURN_ITEM_TEMPLATE"),
                "Xiuluo startup return-item fallback must use the task-page return item probe");
        require(xiuluoReturnFallback.contains("gameStateUtil.isSameMapName(afterReturn.mapName, START_MAP_NAME)"),
                "Xiuluo startup return-item fallback must verify the return map");

        String wubeiDispatch = between(wubei,
                "private WubeiStepOutcome runPhase(",
                "private WubeiStepOutcome runAfterAcceptMaintenanceCheck(");
        require(wubeiDispatch.contains("case HOT_START_DETECT -> runHotStartDetectPhase(context, state);"),
                "Wubei hot-start phase must receive TaskExecutionContext for the startup marker");

        String wubeiHotStart = between(wubei,
                "private WubeiStepOutcome runHotStartDetectPhase(",
                "private WubeiStepOutcome runAfterAcceptMaintenanceCheck(");
        require(wubeiHotStart.contains("currentTrackerPanel.isFound() && !currentTrackerPanel.getGreenLinks().isEmpty()"),
                "Wubei hot-start tracker hit must require an actionable green link before READ_TRACKER");
        require(wubeiHotStart.indexOf("currentTrackerPanel.isFound() && !currentTrackerPanel.getGreenLinks().isEmpty()")
                        < wubeiHotStart.indexOf("context.isAfterCombatExitStartup()"),
                "Wubei after-combat startup must evaluate actionable tracker before return item fallback");
        require(wubeiHotStart.contains("useReturnItemAndVerifyStartMap(context, \"after-combat-exit-startup\")"),
                "Wubei tracker miss after combat must verify completion with the return item");
    }

    private static String between(String source, String start, String end) {
        int startIndex = source.indexOf(start);
        if (startIndex < 0) {
            throw new AssertionError("Missing source marker: " + start);
        }
        int endIndex = source.indexOf(end, startIndex + start.length());
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
