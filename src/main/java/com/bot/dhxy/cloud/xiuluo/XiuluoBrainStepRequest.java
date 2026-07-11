package com.bot.dhxy.cloud.xiuluo;

import com.bot.dhxy.task.xiuluo.XiuluoPhase;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.Map;

@Value
@Builder
public class XiuluoBrainStepRequest {
    @Builder.Default
    String taskCode = "xiuluo_v2";
    String source;
    String windowId;
    String taskRunId;
    String sessionId;
    long stateSeq;
    String phaseToken;
    XiuluoPhase phase;
    String lastActionId;
    @Builder.Default
    Map<String, String> context = Map.of();
    @Builder.Default
    Instant createdAt = Instant.now();
}
