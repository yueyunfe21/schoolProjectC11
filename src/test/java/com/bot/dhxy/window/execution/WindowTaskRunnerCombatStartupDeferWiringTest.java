package com.bot.dhxy.window.execution;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Source-level guard for CR98 startup-in-combat deferral.
 *
 * <p>Runtime validation needs a live game combat. This guard protects the startup ordering:
 * leader-task startup must wait for combat exit before team-role detection or startup UI
 * preparation, then pass an explicit marker into the task execution context.</p>
 */
public class WindowTaskRunnerCombatStartupDeferWiringTest {

    public static void main(String[] args) throws Exception {
        Path root = Path.of("").toAbsolutePath();
        String runner = Files.readString(root.resolve(
                "src/main/java/com/bot/dhxy/window/execution/WindowTaskRunner.java"), StandardCharsets.UTF_8);
        String context = Files.readString(root.resolve(
                "src/main/java/com/bot/dhxy/runner/context/TaskExecutionContext.java"), StandardCharsets.UTF_8);
        String startupMode = Files.readString(root.resolve(
                "src/main/java/com/bot/dhxy/runner/context/TaskStartupMode.java"), StandardCharsets.UTF_8);

        String queueLoop = between(runner,
                "private void runQueueWithBoundGameState(",
                "private TaskRunResult runTaskWithBoundGameState(");
        require(queueLoop.contains("TaskStartupMode startupMode = deferStartupIfAlreadyInCombat("),
                "runner must check already-in-combat startup before resolving team role");
        require(queueLoop.indexOf("deferStartupIfAlreadyInCombat(")
                        < queueLoop.indexOf("resolveTaskTypeBeforeStart("),
                "startup combat defer must run before team-role detection");
        require(queueLoop.contains("buildExecutionContext(requestedTaskType, stopToken, pauseToken, startupMode)"),
                "preflight context after combat exit must carry the startup marker");
        require(queueLoop.contains("buildExecutionContext(requestedTaskType, task, stopToken, pauseToken, startupMode)"),
                "task execution context after combat exit must carry the startup marker");

        String deferHelper = between(runner,
                "private TaskStartupMode deferStartupIfAlreadyInCombat(",
                "private TaskRunResult runTaskWithBoundGameState(");
        require(deferHelper.contains("probeWindowCombatStateReadOnly("),
                "startup defer must use the no-input read-only combat probe");
        require(!deferHelper.contains("handleWindowCombatGuardTick("),
                "startup defer must not use guard tick because it can run combat-enter panel hotkeys");
        require(deferHelper.contains("TaskStartupMode.AFTER_COMBAT_EXIT_STARTUP"),
                "startup defer must return AFTER_COMBAT_EXIT_STARTUP after waiting for combat exit");
        require(!deferHelper.contains("teamRoleDetectionService.detectCurrentRole"),
                "startup defer must not detect team role while the window is in combat");
        require(!deferHelper.contains("startupInitializer.beforeTask"),
                "startup defer must not run startup UI preparation while the window is in combat");

        String autoCombat = Files.readString(root.resolve(
                "src/main/java/com/bot/dhxy/service/AutoCombatService.java"), StandardCharsets.UTF_8);
        String readOnlyProbe = between(autoCombat,
                "public TickResult probeWindowCombatStateReadOnly(",
                "public int getDynamicPollingIntervalMs()");
        require(readOnlyProbe.contains("battleRadarService.checkAndSyncCombatState()"),
                "read-only startup probe must refresh combat radar state");
        require(!readOnlyProbe.contains("maybeHandleCombatEnter("),
                "read-only startup probe must not consume combat enter or open auto-combat panel");
        require(!readOnlyProbe.contains("ensurePanelVisible("),
                "read-only startup probe must not trigger auto-combat panel hotkeys");

        require(context.contains("TaskStartupMode startupMode"),
                "TaskExecutionContext must carry a typed startup mode");
        require(context.contains("isAfterCombatExitStartup()"),
                "TaskExecutionContext must expose an explicit after-combat startup predicate");
        require(startupMode.contains("AFTER_COMBAT_EXIT_STARTUP"),
                "TaskStartupMode must define AFTER_COMBAT_EXIT_STARTUP");
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
