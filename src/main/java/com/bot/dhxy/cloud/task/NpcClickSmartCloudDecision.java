package com.bot.dhxy.cloud.task;

import com.bot.dhxy.cloud.decision.CloudDecisionResult;
import lombok.Builder;
import lombok.Value;

import java.awt.Point;

@Value
@Builder
public class NpcClickSmartCloudDecision {
    @Builder.Default
    Status status = Status.REQUIRED_FAILURE;
    Action action;
    Point windowRelativeClickPoint;
    String actionId;
    String decisionId;
    String reason;
    String debugToken;
    String candidateBox;
    String hotkey;
    String attemptToken;
    @Builder.Default
    boolean ctrl = false;
    @Builder.Default
    boolean alt = false;
    @Builder.Default
    double confidence = 0.0d;
    CloudDecisionResult cloudResult;

    public boolean isCloudExecuted() {
        return status == Status.CLOUD_EXECUTED
                && action != null
                && action.requiresClickPoint()
                && windowRelativeClickPoint != null;
    }

    public enum Status {
        CLOUD_EXECUTED,
        CLOUD_NO_ACTION,
        DISABLED,
        REQUIRED_FAILURE
    }

    public enum Action {
        CLICK,
        CTRL_HOVER,
        CTRL_CLICK,
        PRESS_HOTKEY_THEN_CLICK,
        RIGHT_CLICK,
        VERIFY_DIALOG,
        NOT_FOUND,
        NO_ACTION,
        REQUEST_NEW_SCREENSHOT,
        ABORT;

        public boolean requiresClickPoint() {
            return this == CLICK
                    || this == CTRL_HOVER
                    || this == CTRL_CLICK
                    || this == PRESS_HOTKEY_THEN_CLICK
                    || this == RIGHT_CLICK;
        }
    }
}
