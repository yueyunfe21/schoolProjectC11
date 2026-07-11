package com.bot.dhxy.task.xiuluo;

import com.bot.dhxy.cloud.xiuluo.XiuluoBrainActionType;
import lombok.Builder;
import lombok.Value;

import java.util.List;
import java.util.Map;

/**
 * Factual result of executing one local action requested by `XIULUO_BRAIN`.
 *
 * <p>This model reports what the client attempted and observed. It does not carry any successor
 * business phase/state because phase authority remains with the cloud brain.</p>
 */
@Value
@Builder(toBuilder = true)
public class XiuluoActionExecutionResult {
    String actionId;
    XiuluoBrainActionType actionType;
    Status status;
    String message;
    String errorReason;
    String safetyDeniedReason;
    long elapsedMs;

    @Builder.Default
    Map<String, String> facts = Map.of();

    @Builder.Default
    List<String> evidencePaths = List.of();

    public enum Status {
        NOT_RUN,
        EXECUTED,
        WAITING,
        LOCAL_SAFETY_DENIED,
        FAILED,
        STOPPED
    }
}
