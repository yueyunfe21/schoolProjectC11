package com.bot.dhxy.cloud.task;

import com.bot.dhxy.cloud.decision.CloudDecisionResult;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class SheyaoxiangStatusCloudDecision {

    @Builder.Default
    Status status = Status.REQUIRED_FAILURE;
    @Builder.Default
    Action action = Action.FAIL_CLOSED;
    @Builder.Default
    Present present = Present.UNKNOWN;
    Long remainingMs;
    String remainingSource;
    Box iconBox;
    String text;
    @Builder.Default
    double confidence = 0.0d;
    String reason;
    String decisionId;
    CloudDecisionResult cloudResult;

    public boolean shouldCaptureStatus() {
        return status == Status.CLOUD_EXECUTED && action == Action.CAPTURE_STATUS;
    }

    public boolean shouldUseIncense() {
        return status == Status.CLOUD_EXECUTED && action == Action.USE_INCENSE;
    }

    public boolean failClosed() {
        return status == Status.REQUIRED_FAILURE || action == Action.FAIL_CLOSED;
    }

    public enum Status {
        CLOUD_EXECUTED,
        DISABLED,
        REQUIRED_FAILURE
    }

    public enum Action {
        NO_ACTION,
        CAPTURE_STATUS,
        USE_INCENSE,
        RETRY_LATER,
        FAIL_CLOSED
    }

    public enum Present {
        TRUE,
        FALSE,
        UNKNOWN
    }

    @Value
    @Builder
    public static class Box {
        int x;
        int y;
        int width;
        int height;
    }
}
