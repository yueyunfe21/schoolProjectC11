package com.bot.dhxy.cloud.turn.protocol;

/**
 * Bounded in-action continuation carried by the existing HTTPS turn v1 endpoint.
 */
public record TurnContinuationRequest(
        String actionId,
        int sourceStepIndex,
        Kind kind,
        Stage stage,
        TurnFrameMetadata frame,
        String decisionId) {

    public enum Kind {
        FIVERING_INCENSE,
        FIVERING_ACCEPT_DIALOG
    }

    public enum Stage {
        TICK,
        STATUS_IMAGE,
        OUTCOME_USED,
        OUTCOME_NOT_FOUND,
        DIALOG_OPTION_IMAGE,
        DIALOG_STORY_IMAGE,
        DIALOG_STORY_CLOSED
    }
}
