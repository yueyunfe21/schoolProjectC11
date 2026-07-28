package com.bot.dhxy.cloud.turn.protocol;

/** Cloud-owned directive for one bounded in-action continuation stage. */
public record TurnContinuationDecision(
        Directive directive,
        String decisionId,
        String reason,
        Integer clickX,
        Integer clickY) {

    public TurnContinuationDecision(Directive directive, String decisionId, String reason) {
        this(directive, decisionId, reason, null, null);
    }

    public enum Directive {
        CAPTURE_STATUS,
        USE_INCENSE,
        KEEP_INCENSE,
        COMPLETE,
        CLICK_ACCEPT,
        CLOSE_STORY,
        COMPLETE_ACCEPTED,
        COMPLETE_NOT_ACCEPTED,
        COMPLETE_ALREADY_FINISHED
    }
}
