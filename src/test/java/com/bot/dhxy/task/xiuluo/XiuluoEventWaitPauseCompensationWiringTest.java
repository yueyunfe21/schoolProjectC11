package com.bot.dhxy.task.xiuluo;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Source guard for CR92 修罗 event-wait pause compensation.
 */
public class XiuluoEventWaitPauseCompensationWiringTest {

    public static void main(String[] args) throws Exception {
        assertStatePauseStillShiftsOnlyTheBusinessTimer();
        assertEventWaitFeedsPauseBlockedTimeBackIntoNextState();
    }

    private static void assertStatePauseStillShiftsOnlyTheBusinessTimer() {
        XiuluoRoundContext start = XiuluoRoundContext.start(92)
                .next(XiuluoPhase.WAIT_TRACKER_SHORTCUT_PATHING, "cr92-test");
        long original = start.preCombatStartedAtMs();

        XiuluoRoundContext paused = start.pausePreCombatTimer(160_000L, "event-wait-pause");

        require(paused.preCombatStartedAtMs() == original + 160_000L,
                "event-wait pause must shift the pre-combat watchdog start");
        require(paused.phase() == XiuluoPhase.WAIT_TRACKER_SHORTCUT_PATHING,
                "event-wait pause must preserve the current wait phase");
        require(paused.round() == start.round(), "event-wait pause must preserve round identity");
    }

    private static void assertEventWaitFeedsPauseBlockedTimeBackIntoNextState() throws Exception {
        Path root = Path.of("").toAbsolutePath();
        String task = Files.readString(root.resolve(
                "src/main/java/com/bot/dhxy/task/xiuluo/XiuluoTaskV2.java"), StandardCharsets.UTF_8)
                .replace("\r\n", "\n");

        String runLoop = between(task,
                "if (outcome.transactionResult() == TaskTransactionResult.PATHING_STARTED) {",
                "if (outcome.transactionResult() == TaskTransactionResult.SHARED_STATE_TRIGGERED) {");
        require(runLoop.contains("outcome = yieldAfterMustYield(context, outcome);"),
                "PATHING_STARTED event waits must use the pause-compensated outcome before advancing state");
        require(runLoop.contains("roundContext = outcome.nextState();"),
                "PATHING_STARTED must advance with the compensated nextState");

        int sharedStart = task.indexOf(
                "if (outcome.transactionResult() == TaskTransactionResult.SHARED_STATE_TRIGGERED) {");
        require(sharedStart >= 0, "SHARED_STATE_TRIGGERED branch must exist");
        int sharedCompensate = task.indexOf("outcome = yieldAfterMustYield(context, outcome);", sharedStart);
        int sharedAdvance = task.indexOf("roundContext = outcome.nextState();", sharedStart);
        require(sharedCompensate > sharedStart && sharedAdvance > sharedCompensate,
                "SHARED_STATE_TRIGGERED event waits must use the pause-compensated outcome before advancing state");

        String genericYield = between(task,
                "if (outcome.yieldPolicy() == TaskYieldPolicy.MUST_YIELD) {",
                "consecutivePathingYields = 0;");
        require(genericYield.contains("outcome = yieldAfterMustYield(context, outcome);"),
                "generic MUST_YIELD waits must preserve any event-wait timer compensation");

        String yieldBlock = between(task,
                "private XiuluoStepOutcome yieldAfterMustYield(",
                "private long handoffDelayMs(");
        require(yieldBlock.contains("return parkAfterYieldIfNeeded(context, outcome);"),
                "event wait must return the park result instead of discarding the adjusted outcome");
        require(yieldBlock.contains("return outcome;"),
                "non-event handoff must return the original outcome");

        String parkBlock = between(task,
                "private XiuluoStepOutcome parkAfterYieldIfNeeded(",
                "private EnumSet<WindowReadyEventType> toWakeTypeEnumSet(");
        require(parkBlock.contains("long pauseBlockedMs = TaskCheckpoint.throwIfStopRequested"),
                "park must capture checkpoint pause blocked time after WindowReadyEventBus wait");
        require(parkBlock.contains("compensatePreCombatTimerAfterMaintenance("),
                "park must feed event-wait pause time into the 修罗 pre-combat timer");
        require(parkBlock.contains("outcome.withNextState("),
                "park must return an outcome whose nextState carries the compensated watchdog start");
        require(parkBlock.contains("pauseBlockedMs"),
                "park diagnostics must expose pauseBlockedMs beside wall-clock elapsedMs");
        require(parkBlock.contains("xiuluo-v2:event-wait:"),
                "修罗 event-wait compensation must have a distinct diagnostic source");

        String watchdogBlock = between(task,
                "private XiuluoStepOutcome checkPreCombatWatchdogTimeout(",
                "private boolean shouldApplyPreCombatWatchdog(");
        require(watchdogBlock.contains("clearPreCombatWaitOwnedRuntimeState(state,"),
                "pre-combat watchdog failure must clear stale wait-owned runtime state before restarting");

        String cleanupBlock = between(task,
                "private void clearPreCombatWaitOwnedRuntimeState(",
                "private boolean shouldApplyPreCombatWatchdog(");
        require(cleanupBlock.contains("runtime.clearPathingSignal("),
                "watchdog cleanup must clear stale pathing signal from the failed wait");
        require(cleanupBlock.contains("prepared.getSource().startsWith(\"xiuluo-v2:\")"),
                "watchdog cleanup must only clear Xiuluo-owned prepared actions");
        require(cleanupBlock.contains("runtime.clearPreparedDialogAction("),
                "watchdog cleanup must clear stale Xiuluo prepared action from the failed wait");
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
