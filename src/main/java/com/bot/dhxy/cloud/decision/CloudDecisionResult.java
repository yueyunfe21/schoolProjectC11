package com.bot.dhxy.cloud.decision;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class CloudDecisionResult {
    CloudDecisionMode mode;
    CloudFallbackMode fallbackMode;
    CloudDecisionRequest request;
    CloudDecisionResponse response;
    String localDecision;
    String effectiveDecision;
    boolean cloudAvailable;
    boolean agreement;
    boolean executed;
    long elapsedMs;
    String reason;

    public boolean isRequiredExecuteFailure() {
        return mode == CloudDecisionMode.EXECUTE
                && fallbackMode == CloudFallbackMode.STOP
                && !executed;
    }
}
