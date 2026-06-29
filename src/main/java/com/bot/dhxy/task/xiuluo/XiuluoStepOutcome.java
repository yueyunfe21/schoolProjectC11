package com.bot.dhxy.task.xiuluo;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.Accessors;

import com.bot.dhxy.task.transaction.TaskTransactionResult;
import com.bot.dhxy.task.transaction.TaskYieldPolicy;

/**
 * Result of executing exactly one Xiuluo phase.
 *
 * @param nextState state to execute after this phase.
 * @param transactionResult task-turn result the phase wants to report.
 * @param yieldPolicy whether the task should keep the business turn or yield after this step.
 * @param message short diagnostic message for logs.
 * @param waitSpec optional scheduling-only wait policy used after this outcome releases the turn.
 */
@Value
@Builder
@AllArgsConstructor(access = AccessLevel.PUBLIC)
@Accessors(fluent = true)
public class XiuluoStepOutcome {
    XiuluoRoundContext nextState;
    TaskTransactionResult transactionResult;
    TaskYieldPolicy yieldPolicy;
    String message;
    XiuluoWaitSpec waitSpec;

    public static XiuluoStepOutcome continueTo(XiuluoRoundContext nextState, String message) {
        return new XiuluoStepOutcome(
                nextState,
                TaskTransactionResult.READY_TO_CONTINUE,
                TaskYieldPolicy.CONTINUE_CHAIN,
                message,
                null);
    }

    public static XiuluoStepOutcome pathingStarted(XiuluoRoundContext nextState, String message) {
        return new XiuluoStepOutcome(
                nextState,
                TaskTransactionResult.PATHING_STARTED,
                TaskYieldPolicy.MUST_YIELD,
                message,
                null);
    }

    public static XiuluoStepOutcome sharedState(XiuluoRoundContext nextState, String message) {
        return new XiuluoStepOutcome(
                nextState,
                TaskTransactionResult.SHARED_STATE_TRIGGERED,
                TaskYieldPolicy.MUST_YIELD,
                message,
                null);
    }

    public static XiuluoStepOutcome failed(XiuluoRoundContext state, String message) {
        return new XiuluoStepOutcome(
                state.next(XiuluoPhase.FAILED, "failed"),
                TaskTransactionResult.FAILED,
                TaskYieldPolicy.MUST_YIELD,
                message,
                null);
    }

    public static XiuluoStepOutcome stopped(XiuluoRoundContext state, String message) {
        return new XiuluoStepOutcome(
                state.next(XiuluoPhase.STOPPED, "stopped"),
                TaskTransactionResult.STOPPED,
                TaskYieldPolicy.MUST_YIELD,
                message,
                null);
    }

    public XiuluoStepOutcome withWaitSpec(XiuluoWaitSpec waitSpec) {
        return new XiuluoStepOutcome(nextState, transactionResult, yieldPolicy, message, waitSpec);
    }

    public XiuluoStepOutcome withNextState(XiuluoRoundContext nextState) {
        return new XiuluoStepOutcome(nextState, transactionResult, yieldPolicy, message, waitSpec);
    }

}
