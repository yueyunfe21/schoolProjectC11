package com.bot.dhxy.task.wubei;

import com.bot.dhxy.task.transaction.TaskTransactionResult;
import com.bot.dhxy.task.transaction.TaskYieldPolicy;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.Accessors;

/**
 * Result of executing exactly one 五倍 phase.
 *
 * @param nextState state to execute after this phase.
 * @param transactionResult task-turn result reported to the shared turn coordinator.
 * @param yieldPolicy whether this phase should keep or release the task turn.
 * @param message short diagnostic message for logs.
 */
@Value
@Builder
@AllArgsConstructor(access = AccessLevel.PUBLIC)
@Accessors(fluent = true)
public class WubeiStepOutcome {
    WubeiRoundContext nextState;
    TaskTransactionResult transactionResult;
    TaskYieldPolicy yieldPolicy;
    String message;

    public static WubeiStepOutcome continueTo(WubeiRoundContext nextState, String message) {
        return new WubeiStepOutcome(
                nextState,
                TaskTransactionResult.READY_TO_CONTINUE,
                TaskYieldPolicy.CONTINUE_CHAIN,
                message);
    }

    public static WubeiStepOutcome pathingStarted(WubeiRoundContext nextState, String message) {
        return new WubeiStepOutcome(
                nextState,
                TaskTransactionResult.PATHING_STARTED,
                TaskYieldPolicy.MUST_YIELD,
                message);
    }

    public static WubeiStepOutcome sharedState(WubeiRoundContext nextState, String message) {
        return new WubeiStepOutcome(
                nextState,
                TaskTransactionResult.SHARED_STATE_TRIGGERED,
                TaskYieldPolicy.MUST_YIELD,
                message);
    }

    public static WubeiStepOutcome failed(WubeiRoundContext state, String message) {
        return new WubeiStepOutcome(
                state.next(WubeiPhase.FAILED, "failed"),
                TaskTransactionResult.FAILED,
                TaskYieldPolicy.MUST_YIELD,
                message);
    }

    public static WubeiStepOutcome stopped(WubeiRoundContext state, String message) {
        return new WubeiStepOutcome(
                state.next(WubeiPhase.STOPPED, "stopped"),
                TaskTransactionResult.STOPPED,
                TaskYieldPolicy.MUST_YIELD,
                message);
    }
}
