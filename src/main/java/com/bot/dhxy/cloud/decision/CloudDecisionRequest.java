package com.bot.dhxy.cloud.decision;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.Map;

@Value
@Builder
public class CloudDecisionRequest {
    CloudDecisionServiceId serviceId;
    String traceId;
    String taskCode;
    String phase;
    String windowId;
    String taskRunId;
    String policyVersion;
    String localDecision;
    @Builder.Default
    Map<String, String> context = Map.of();
    @Builder.Default
    Instant createdAt = Instant.now();
}
