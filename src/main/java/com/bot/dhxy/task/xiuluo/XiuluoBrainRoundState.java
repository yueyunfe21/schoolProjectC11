package com.bot.dhxy.task.xiuluo;

import com.bot.dhxy.task.transaction.TaskTransactionResult;
import com.bot.dhxy.task.transaction.TaskYieldPolicy;

/**
 * Local state carrier for one enabled `XIULUO_BRAIN` round.
 *
 * <p>Cloud remains the phase-transition authority. This class only preserves the state produced by
 * local phase handlers so the next cloud-commanded phase does not lose accept-time futures,
 * prepath/pathing flags, objective facts, or tracker shortcut facts.</p>
 */
final class XiuluoBrainRoundState {
    private XiuluoRoundContext current;
    private XiuluoWaitSpec pendingWaitSpec;
    private int consecutiveImmediateLoopCount;
    private boolean startupReturnItemTriedAndUnverified;

    private XiuluoBrainRoundState(XiuluoRoundContext current) {
        this.current = current;
    }

    static XiuluoBrainRoundState start(int round) {
        return new XiuluoBrainRoundState(XiuluoRoundContext.start(round));
    }

    XiuluoRoundContext current() {
        return current;
    }

    void adjustCurrent(java.util.function.UnaryOperator<XiuluoRoundContext> adjuster) {
        XiuluoRoundContext adjusted = adjuster.apply(current);
        if (adjusted != null) {
            current = adjusted;
        }
    }

    XiuluoRoundContext executionStateFor(XiuluoPhase cloudPhase, String actionId) {
        if (cloudPhase == null) {
            return current;
        }
        return current.next(cloudPhase, "xiuluo-brain:" + safeActionId(actionId));
    }

    void recordOutcome(XiuluoStepOutcome outcome) {
        if (outcome != null && outcome.nextState() != null) {
            current = outcome.nextState();
        }
        pendingWaitSpec = outcome == null ? null : outcome.waitSpec();
    }

    boolean startupReturnItemTriedAndUnverified() {
        return startupReturnItemTriedAndUnverified;
    }

    void markStartupReturnItemTriedAndUnverified() {
        startupReturnItemTriedAndUnverified = true;
    }

    XiuluoWaitSpec consumePendingWaitSpec() {
        XiuluoWaitSpec waitSpec = pendingWaitSpec;
        pendingWaitSpec = null;
        return waitSpec;
    }

    boolean noteImmediateLoopAndCheckExceeded(int maxImmediateLoops) {
        consecutiveImmediateLoopCount++;
        return consecutiveImmediateLoopCount >= maxImmediateLoops;
    }

    void noteRealEventWaitCompleted() {
        consecutiveImmediateLoopCount = 0;
    }

    boolean noteCommandCycleAndCheckExceeded(boolean realEventWaitCompleted, int maxImmediateLoops) {
        if (realEventWaitCompleted) {
            noteRealEventWaitCompleted();
            return false;
        }
        return noteImmediateLoopAndCheckExceeded(maxImmediateLoops);
    }

    static boolean mayRequestCloudStepAfter(XiuluoStepOutcome outcome) {
        if (outcome == null || outcome.transactionResult() == null) {
            return false;
        }
        if (outcome.yieldPolicy() == TaskYieldPolicy.MUST_YIELD
                || outcome.yieldPolicy() == TaskYieldPolicy.RETRY_LATER
                || outcome.yieldPolicy() == TaskYieldPolicy.STOP_CHAIN) {
            return false;
        }
        return outcome.transactionResult() == TaskTransactionResult.READY_TO_CONTINUE
                || outcome.transactionResult() == TaskTransactionResult.TASK_FINISHED;
    }

    static boolean mustReportBeforeLocalYield(XiuluoPhase commandPhase, XiuluoStepOutcome outcome) {
        /*
         * The cloud owns the next decision after a shared-state yield. In particular, a leader
         * waiting for team return must release its turn once, then report the structured outcome;
         * retrying that same phase inside the command shell can starve the member return flow and
         * incorrectly trip the local yield guard.
         */
        return (commandPhase == XiuluoPhase.WAIT_COMBAT
                || commandPhase == XiuluoPhase.WAIT_TEAM_RETURN)
                && outcome != null
                && outcome.waitSpec() == null
                && outcome.transactionResult() == TaskTransactionResult.SHARED_STATE_TRIGGERED
                && outcome.yieldPolicy() == TaskYieldPolicy.MUST_YIELD;
    }

    private static String safeActionId(String actionId) {
        return actionId == null || actionId.isBlank() ? "missing-action" : actionId;
    }
}
