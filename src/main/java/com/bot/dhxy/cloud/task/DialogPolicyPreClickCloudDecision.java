package com.bot.dhxy.cloud.task;

import com.bot.dhxy.cloud.decision.CloudDecisionResult;
import lombok.Builder;
import lombok.Value;

import java.awt.Point;

@Value
@Builder
public class DialogPolicyPreClickCloudDecision {
    @Builder.Default
    Status status = Status.REQUIRED_FAILURE;
    @Builder.Default
    Action action = Action.ABORT;
    Point windowRelativeClickPoint;
    String actionId;
    String decisionId;
    String reason;
    String matchedText;
    String debugToken;
    String candidateBox;
    @Builder.Default
    boolean ctrl = false;
    @Builder.Default
    boolean alt = false;
    @Builder.Default
    double confidence = 0.0d;
    CloudDecisionResult cloudResult;

    public boolean isCloudExecuted() {
        return status == Status.CLOUD_EXECUTED && action == Action.CLICK && windowRelativeClickPoint != null;
    }

    public boolean isRequiredFailure() {
        return status == Status.REQUIRED_FAILURE;
    }

    public enum Status {
        CLOUD_EXECUTED,
        CLOUD_NO_ACTION,
        DISABLED,
        REQUIRED_FAILURE
    }

    public enum Action {
        CLICK,
        NO_ACTION,
        REQUEST_NEW_SCREENSHOT,
        ABORT
    }
}
