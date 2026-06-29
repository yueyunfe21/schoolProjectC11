package com.bot.dhxy.task.xiuluo;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Source-level guard for CR122 stale 修罗 tracker-shortcut pathing intents.
 *
 * <p>Live proof needs a real tracker shortcut that enters combat and returns home. This guard keeps
 * the lifecycle cleanup wired at the boundaries that make a later STOPPED_AWAY terminal stale.</p>
 */
public class XiuluoCR122TrackerShortcutIntentLifecycleWiringTest {

    public static void main(String[] args) throws Exception {
        Path root = Path.of("").toAbsolutePath();
        String runtime = read(root, "src/main/java/com/bot/dhxy/window/runtime/WindowRuntimeContext.java");
        String runner = read(root, "src/main/java/com/bot/dhxy/window/execution/WindowTaskRunner.java");
        String xiuluo = read(root, "src/main/java/com/bot/dhxy/task/xiuluo/XiuluoTaskV2.java");

        runtimeHasSourceScopedPathingCleanup(runtime);
        runnerClearsShortcutIntentOnXiuluoCombatEntry(runner);
        xiuluoClearsShortcutIntentAtPreparedConsumeReturnAndRoundStart(xiuluo);
    }

    private static void runtimeHasSourceScopedPathingCleanup(String runtime) {
        String method = between(runtime,
                "public boolean clearPathingSignalIfSourcePrefix(",
                "public void clearPathingSignal(");
        require(method.contains("pathingSnapshot.compareAndSet(snapshot, clearedSnapshot)"),
                "runtime cleanup must atomically clear only the matching active snapshot");
        require(method.contains("activeSource.startsWith(normalizedSourcePrefix)"),
                "runtime cleanup must be scoped by source prefix");
        require(method.contains("clearPendingTransferChoiceMemory(\"pathing signal cleared\")"),
                "runtime cleanup must preserve clearPathingSignal pending route-memory cleanup");
    }

    private static void runnerClearsShortcutIntentOnXiuluoCombatEntry(String runner) {
        String publish = between(runner,
                "private void publishCombatStateChanged(",
                "private void clearWubeiDialogStateOnCombatEntry(");
        require(publish.contains("clearXiuluoTrackerShortcutPathingOnCombatEntry(taskType, oldTick, newTick, hwnd);"),
                "runner combat-state publish must clear 修罗 tracker-shortcut intent on IN_COMBAT");

        String clearMethod = between(runner,
                "private void clearXiuluoTrackerShortcutPathingOnCombatEntry(",
                "private void clearWubeiDialogStateOnCombatEntry(");
        require(clearMethod.contains("taskType != TaskType.XIULUO_V2"),
                "runner cleanup must be scoped to 修罗 only");
        require(clearMethod.contains("newTick != AutoCombatService.TickResult.IN_COMBAT"),
                "runner cleanup must only run on confirmed combat entry");
        require(clearMethod.contains("windowContext.clearPathingSignalIfSourcePrefix("),
                "runner cleanup must use runtime source-prefix cleanup");
        require(clearMethod.contains("xiuluo-v2:tracker-shortcut"),
                "runner cleanup must only target tracker-shortcut sources");
    }

    private static void xiuluoClearsShortcutIntentAtPreparedConsumeReturnAndRoundStart(String xiuluo) {
        require(xiuluo.contains("TRACKER_SHORTCUT_PATHING_SOURCE_PREFIX = \"xiuluo-v2:tracker-shortcut\""),
                "Xiuluo task must own a single tracker-shortcut source prefix constant");

        String execute = between(xiuluo,
                "public TaskRunResult execute(TaskExecutionContext executionContext)",
                "public void stop()");
        require(execute.contains("clearTrackerShortcutPathingIntent(context.getWindowRuntimeContext(), \"xiuluo-v2:round-start\")"),
                "round transition must clear any previous-round tracker-shortcut pathing intent");

        String consumePrepared = between(xiuluo,
                "private XiuluoStepOutcome consumePreparedXiuluoEnterBattle(",
                "private XiuluoStepOutcome waitForCombatStateWake(");
        require(consumePrepared.contains("clearTrackerShortcutPathingIntent(runtime, \"xiuluo enter battle prepared consumed\")"),
                "prepared enter-battle consume must clear stale tracker-shortcut pathing intent");

        String returnHome = between(xiuluo,
                "private XiuluoStepOutcome returnHome(",
                "private Optional<XiuluoStepOutcome> correctKnownCombatReturnFailureIfStillInCombat(");
        require(returnHome.contains("clearTrackerShortcutPathingIntent(context.getWindowRuntimeContext(), \"xiuluo-v2:return-home-verified\")"),
                "verified return-home must clear any surviving tracker-shortcut pathing intent");

        String helper = between(xiuluo,
                "private boolean clearTrackerShortcutPathingIntent(",
                "private XiuluoStepOutcome waitTrackerShortcutPathing(");
        require(helper.contains("runtime.clearPathingSignalIfSourcePrefix(TRACKER_SHORTCUT_PATHING_SOURCE_PREFIX, reason)"),
                "Xiuluo helper must call runtime source-prefix cleanup");
    }

    private static String read(Path root, String relativePath) throws Exception {
        return Files.readString(root.resolve(relativePath), StandardCharsets.UTF_8);
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
