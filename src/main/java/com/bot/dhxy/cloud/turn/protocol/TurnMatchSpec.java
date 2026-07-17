package com.bot.dhxy.cloud.turn.protocol;

public record TurnMatchSpec(
        TurnRegion region,
        String templateKey,
        String contentHash,
        double threshold,
        OnMatch onMatch,
        ResultMode resultMode) {

    public enum OnMatch {
        NONE,
        CLICK
    }

    public enum ResultMode {
        RETURN_MATCH_RESULT,
        RETURN_MATCH_RESULT_AND_IMAGE
    }
}
