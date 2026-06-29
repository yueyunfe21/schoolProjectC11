package com.bot.dhxy.task.wuhuan;

import com.bot.dhxy.task.transaction.TaskTransactionResult;
import com.bot.dhxy.task.transaction.TaskYieldPolicy;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.Accessors;

/**
 * Result of executing exactly one Five-ring V2 phase.
 *
 * @param nextState state to execute after this phase.
 * @param transactionResult task-turn result the phase wants to report.
 * @param yieldPolicy whether this phase should keep or release the task turn.
 * @param terminalTask true when this outcome should stop the whole configured Five-ring execution,
 *                     not only the current run.
 * @param message short diagnostic message for logs.
 */
@Value
@Builder
@AllArgsConstructor(access = AccessLevel.PUBLIC)
@Accessors(fluent = true)
public class FiveRingStepOutcome {
    FiveRingPhaseContext nextState;
    TaskTransactionResult transactionResult;
    TaskYieldPolicy yieldPolicy;
    boolean terminalTask;
    String message;

    public static FiveRingStepOutcome continueTo(FiveRingPhaseContext nextState, String message) {
        return new FiveRingStepOutcome(
                nextState,
                TaskTransactionResult.READY_TO_CONTINUE,
                TaskYieldPolicy.CONTINUE_CHAIN,
                false,
                message);
    }

    public static FiveRingStepOutcome pathingStarted(FiveRingPhaseContext nextState, String message) {
        return new FiveRingStepOutcome(
                nextState,
                TaskTransactionResult.PATHING_STARTED,
                TaskYieldPolicy.MUST_YIELD,
                false,
                message);
    }

    public static FiveRingStepOutcome sharedState(FiveRingPhaseContext nextState, String message) {
        return new FiveRingStepOutcome(
                nextState,
                TaskTransactionResult.SHARED_STATE_TRIGGERED,
                TaskYieldPolicy.MUST_YIELD,
                false,
                message);
    }

    public static FiveRingStepOutcome finished(FiveRingPhaseContext state, String message) {
        return finished(state, false, message);
    }

    public static FiveRingStepOutcome finishedTerminal(FiveRingPhaseContext state, String message) {
        return finished(state, true, message);
    }

    private static FiveRingStepOutcome finished(FiveRingPhaseContext state,
                                                boolean terminalTask,
                                                String message) {
        return new FiveRingStepOutcome(
                state.next(FiveRingPhase.FINISHED, "finished"),
                TaskTransactionResult.TASK_FINISHED,
                TaskYieldPolicy.MUST_YIELD,
                terminalTask,
                message);
    }

    public static FiveRingStepOutcome failed(FiveRingPhaseContext state, String message) {
        return new FiveRingStepOutcome(
                state.next(FiveRingPhase.FAILED, "failed"),
                TaskTransactionResult.FAILED,
                TaskYieldPolicy.MUST_YIELD,
                false,
                message);
    }

    public static FiveRingStepOutcome stopped(FiveRingPhaseContext state, String message) {
        return new FiveRingStepOutcome(
                state.next(FiveRingPhase.STOPPED, "stopped"),
                TaskTransactionResult.STOPPED,
                TaskYieldPolicy.MUST_YIELD,
                false,
                message);
    }
}
