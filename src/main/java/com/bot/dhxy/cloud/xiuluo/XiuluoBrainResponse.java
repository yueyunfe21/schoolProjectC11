package com.bot.dhxy.cloud.xiuluo;

import com.bot.dhxy.task.xiuluo.XiuluoPhase;
import lombok.Builder;
import lombok.Value;

import java.util.Map;

@Value
@Builder
public class XiuluoBrainResponse {
    String windowId;
    String taskRunId;
    String sessionId;
    long stateSeq;
    String phaseToken;
    String acceptedPhaseToken;
    XiuluoPhase phase;
    XiuluoBrainActionType actionType;
    String actionId;
    String cleanupType;
    String retryKey;
    int attempt;
    int maxAttempts;
    String reason;
    @Builder.Default
    Map<String, String> diagnostics = Map.of();
}
