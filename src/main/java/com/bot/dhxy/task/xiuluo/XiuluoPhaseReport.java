package com.bot.dhxy.task.xiuluo;

import lombok.Builder;
import lombok.Value;

import java.util.List;
import java.util.Map;

/**
 * Factual report for one 修罗 phase handled by the enabled `XIULUO_BRAIN` path.
 *
 * <p>The report is deliberately limited to observed facts, action execution output, wait/error
 * details, safety-denial context, and evidence paths. It must not encode the client's choice of a
 * successor business phase/state.</p>
 */
@Value
@Builder(toBuilder = true)
public class XiuluoPhaseReport {
    XiuluoPhase phase;
    Status status;
    XiuluoActionExecutionResult actionResult;
    XiuluoWaitSpec waitSpec;
    String errorReason;
    String safetyDeniedReason;
    long elapsedMs;

    @Builder.Default
    Map<String, String> facts = Map.of();

    @Builder.Default
    List<String> evidencePaths = List.of();

    public enum Status {
        NOT_STARTED,
        ACTION_EXECUTED,
        WAITING,
        LOCAL_SAFETY_DENIED,
        FAILED,
        STOPPED
    }
}
