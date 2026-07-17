package com.bot.dhxy.cloud.turn.protocol;

import java.util.List;

public record TurnOutcome(
        int contractVersion,
        String actionId,
        TurnWindowMetadata window,
        Status status,
        Integer failedStepIndex,
        String code,
        String message,
        List<TurnStepResult> stepResults,
        TurnFrameMetadata frame) {

    public enum Status {
        COMPLETED,
        FAILED,
        STOPPED,
        DUPLICATE_OR_UNCERTAIN
    }
}
