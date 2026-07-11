package com.bot.dhxy.task.xiuluo;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.Accessors;

import com.bot.dhxy.task.transaction.TaskTransactionResult;
import com.bot.dhxy.task.transaction.TaskYieldPolicy;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Result of executing exactly one Xiuluo phase.
 *
 * @param nextState state to execute after this phase.
 * @param transactionResult task-turn result the phase wants to report.
 * @param yieldPolicy whether the task should keep the business turn or yield after this step.
 * @param message short diagnostic message for logs.
 * @param waitSpec optional scheduling-only wait policy used after this outcome releases the turn.
 * @param facts optional structured facts for the cloud brain action-outcome report (CR230). Cloud
 *              decisions must key on these instead of matching human log messages. Never null.
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
    @Builder.Default
    Map<String, String> facts = Map.of();

    public XiuluoStepOutcome(XiuluoRoundContext nextState,
                             TaskTransactionResult transactionResult,
                             TaskYieldPolicy yieldPolicy,
                             String message,
                             XiuluoWaitSpec waitSpec) {
        this(nextState, transactionResult, yieldPolicy, message, waitSpec, Map.of());
    }

    /** Returns a copy of this outcome with one structured cloud fact added. */
    public XiuluoStepOutcome withFact(String key, String value) {
        Map<String, String> merged = new LinkedHashMap<>(facts == null ? Map.of() : facts);
        merged.put(key, value);
        return new XiuluoStepOutcome(nextState, transactionResult, yieldPolicy, message, waitSpec,
                Map.copyOf(merged));
    }

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
        return new XiuluoStepOutcome(nextState, transactionResult, yieldPolicy, message, waitSpec, facts);
    }

    public XiuluoStepOutcome withNextState(XiuluoRoundContext nextState) {
        return new XiuluoStepOutcome(nextState, transactionResult, yieldPolicy, message, waitSpec, facts);
    }

}
