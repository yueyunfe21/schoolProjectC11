package com.bot.dhxy.service;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Source guard for left-top status switch wiring plus CR128 five-ring background startup checks.
 */
public class LeftTopStatusSwitchWiringTest {

    public static void main(String[] args) throws Exception {
        String service = source("src", "main", "java", "com", "bot", "dhxy", "service",
                "LeftTopStatusSwitchService.java");
        String initializer = source("src", "main", "java", "com", "bot", "dhxy", "window", "execution",
                "DefaultWindowTaskStartupInitializer.java");
        String autoBattle = source("src", "main", "java", "com", "bot", "dhxy", "task",
                "AutoBattleTask.java");
        String autoCombat = source("src", "main", "java", "com", "bot", "dhxy", "service",
                "AutoCombatService.java");
        String runtime = source("src", "main", "java", "com", "bot", "dhxy", "window", "runtime",
                "WindowRuntimeContext.java");
        String runner = source("src", "main", "java", "com", "bot", "dhxy", "window", "execution",
                "WindowTaskRunner.java");
        String startupPreparation = source("src", "main", "java", "com", "bot", "dhxy", "window", "startup",
                "TaskStartupWindowPreparationService.java");
        String keyboardService = source("src", "main", "java", "com", "bot", "dhxy", "driver",
                "BoundWindowKeyboardService.java");
        String debugMain = source("src", "main", "java", "com", "bot", "dhxy", "debug",
                "LeftTopStatusProbeDebugMain.java");

        assertContains(service, "LEFT_TOP_STATUS_RECT_X_OFFSET = 8");
        assertContains(service, "LEFT_TOP_STATUS_RECT_Y_OFFSET = 147");
        assertContains(service, "LEFT_TOP_STATUS_RECT_WIDTH = 11");
        assertContains(service, "LEFT_TOP_STATUS_RECT_HEIGHT = 19");
        assertContains(service, "LEFT_TOP_OPEN_TEMPLATE = \"images/template/status/left_top_open.png\"");
        assertContains(service, "LEFT_TOP_CLOSED_TEMPLATE = \"images/template/status/left_top_closed.png\"");
        assertContains(service, "isSupportedTaskCode");
        assertContains(service, "\"xiuluo_v2\".equalsIgnoreCase");
        assertContains(service, "\"wubei\".equalsIgnoreCase");
        assertContains(service, "\"wuhuan_v2\".equalsIgnoreCase");
        assertContains(service, "markLeftTopStatusSwitchClosePending");
        assertContains(service, "consumeLeftTopStatusSwitchClosePending");
        assertContains(service, "inputSequences.moveAndClickLeft");

        assertContains(initializer, "leftTopStatusSwitchService.handleLeaderStartup");
        assertContains(initializer, "leftTopStatusSwitchService.probeMemberStartup");
        String fiveRingLeftTopBranch = between(initializer,
                "五环多窗口启动必须保持后台探测语义",
                "else if (isMemberWindow");
        assertContains(fiveRingLeftTopBranch, "leftTopStatusSwitchService.probeMemberStartup");
        assertNotContains(fiveRingLeftTopBranch, "leftTopStatusSwitchService.handleLeaderStartup");

        String fiveRingStartupBranch = between(initializer,
                "五环五开启动仍要做完整启动 UI 检查",
                "if (isMemberWindow(windowContext, executionContext))");
        assertContains(initializer, "startup init skipped: five-ring queue startup preparation already completed");
        assertContains(fiveRingStartupBranch, "startupWindowPreparationService.prepareTaskStartupWindowBackgroundFirst()");
        assertContains(fiveRingStartupBranch, "if (ready && windowContext != null");
        assertContains(fiveRingStartupBranch, "windowContext.markTaskQueueStartupPreparationDone(taskCode)");
        assertNotContains(fiveRingStartupBranch, "startupWindowPreparationService.prepareTaskStartupWindow()");
        assertNotContains(fiveRingStartupBranch, "five-ring full preparation will check mini-map options");

        assertContains(startupPreparation, "prepareTaskStartupWindowBackgroundFirst");
        assertContains(startupPreparation, "probeStartupMapOptionsBackground");
        assertContains(startupPreparation, "probeExpandOptionBackground");
        assertContains(startupPreparation, "BoundWindowKeyboardService.AltShortcut.ALT_1");
        assertContains(startupPreparation, "BoundWindowKeyboardService.AltShortcut.ALT_U");
        assertContains(startupPreparation, "foregroundCorrectionNeeded()");
        assertContains(startupPreparation, "repairStartupMapOptionsForeground");
        assertContains(startupPreparation, "repairStartupExpandOptionForeground");
        assertContains(startupPreparation, "taskStartup:mapOptionsCorrection");
        assertContains(startupPreparation, "taskStartup:expandOptionCorrection");
        assertNotContains(startupPreparation, "return prepareTaskStartupWindow()");
        assertContains(keyboardService, "ALT_U(\"Alt+U\", 0x55, 0x16, true)");

        assertContains(autoBattle, "leftTopStatusSwitchService.consumeFollowerSafeWindow");
        assertContains(autoBattle, "taskMaintenanceService.isTeamPathingMaintenanceWindowOpen");
        assertContains(autoBattle, "followerSupportMode");

        assertContains(autoCombat, "COMBAT_UI_CLEAN_INTERVAL_MS = 40_000L");
        assertContains(autoCombat, "leftTopStatusSwitchService.handleCombatMaintenance");
        assertContains(autoCombat, "state.lastCombatUiCleanAt = System.currentTimeMillis()");

        assertContains(runtime, "leftTopStatusSwitchClosePending");
        assertContains(runtime, "markLeftTopStatusSwitchClosePending");
        assertContains(runtime, "consumeLeftTopStatusSwitchClosePending");
        assertContains(runtime, "taskQueueStartupPreparationDone");
        assertContains(runtime, "clearTaskQueueStartupPreparationState");
        assertContains(runtime, "isTaskQueueStartupPreparationDone");
        assertContains(runtime, "markTaskQueueStartupPreparationDone");
        assertContains(runner, "windowContext.clearTaskQueueStartupPreparationState(\"task queue started\")");

        assertContains(debugMain, "leftTopStatus.debug.windowTitleContains");
        assertContains(debugMain, "leftTopStatus.debug.click");
        assertContains(debugMain, "images/temp/left_top_status_probe");
        assertContains(debugMain, "LEFT_TOP_STATUS_RECT_X_OFFSET = 8");
        assertContains(debugMain, "LEFT_TOP_STATUS_RECT_Y_OFFSET = 147");
        assertContains(debugMain, "LEFT_TOP_STATUS_RECT_WIDTH = 11");
        assertContains(debugMain, "LEFT_TOP_STATUS_RECT_HEIGHT = 19");
        assertContains(debugMain, "openScore=");
        assertContains(debugMain, "closedScore=");

        System.out.println("LeftTopStatusSwitchWiringTest passed");
    }

    private static String source(String first, String... more) throws Exception {
        return Files.readString(Path.of(first, more), StandardCharsets.UTF_8);
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

    private static void assertContains(String value, String token) {
        if (!value.contains(token)) {
            throw new AssertionError("Expected token missing: " + token);
        }
    }

    private static void assertNotContains(String value, String token) {
        if (value.contains(token)) {
            throw new AssertionError("Unexpected token present: " + token);
        }
    }
}
