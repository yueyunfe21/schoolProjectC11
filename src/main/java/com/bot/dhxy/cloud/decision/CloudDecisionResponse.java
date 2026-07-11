package com.bot.dhxy.cloud.decision;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.Map;

@Value
@Builder
public class CloudDecisionResponse {
    CloudDecisionServiceId serviceId;
    String traceId;
    String policyVersion;
    String decision;
    @Builder.Default
    double confidence = 0.0d;
    @Builder.Default
    long ttlMs = 0L;
    String fallbackReason;
    @Builder.Default
    Map<String, String> diagnostics = Map.of();
    @Builder.Default
    Instant createdAt = Instant.now();
}
