package com.bot.dhxy.task.xiuluo;

import com.bot.dhxy.task.transaction.TaskTransactionResult;
import com.bot.dhxy.task.transaction.TaskYieldPolicy;

/**
 * Result of executing exactly one Xiuluo phase.
 *
 * @param nextState state to execute after this phase.
 * @param transactionResult task-turn result the phase wants to report.
 * @param yieldPolicy whether the task should keep the business turn or yield after this step.
 * @param message short diagnostic message for logs.
 */
public record XiuluoStepOutcome(
        XiuluoRoundState nextState,
        TaskTransactionResult transactionResult,
        TaskYieldPolicy yieldPolicy,
        String message
) {
    public static XiuluoStepOutcome continueTo(XiuluoRoundState nextState, String message) {
        return new XiuluoStepOutcome(
                nextState,
                TaskTransactionResult.READY_TO_CONTINUE,
                TaskYieldPolicy.CONTINUE_CHAIN,
                message);
    }

    public static XiuluoStepOutcome pathingStarted(XiuluoRoundState nextState, String message) {
        return new XiuluoStepOutcome(
                nextState,
                TaskTransactionResult.PATHING_STARTED,
                TaskYieldPolicy.MUST_YIELD,
                message);
    }

    public static XiuluoStepOutcome sharedState(XiuluoRoundState nextState, String message) {
        return new XiuluoStepOutcome(
                nextState,
                TaskTransactionResult.SHARED_STATE_TRIGGERED,
                TaskYieldPolicy.MUST_YIELD,
                message);
    }

    public static XiuluoStepOutcome failed(XiuluoRoundState state, String message) {
        return new XiuluoStepOutcome(
                state.next(XiuluoPhase.FAILED, "failed"),
                TaskTransactionResult.FAILED,
                TaskYieldPolicy.MUST_YIELD,
                message);
    }

    public static XiuluoStepOutcome stopped(XiuluoRoundState state, String message) {
        return new XiuluoStepOutcome(
                state.next(XiuluoPhase.STOPPED, "stopped"),
                TaskTransactionResult.STOPPED,
                TaskYieldPolicy.MUST_YIELD,
                message);
    }
}
