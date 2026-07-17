package com.bot.dhxy.cloud.turn.protocol;

public record TurnRequest(
        int contractVersion,
        TurnWindowMetadata window,
        long waitTimeoutMs,
        TurnOutcome previousOutcome,
        TurnTaskStartRequest taskStartRequest) {

    public TurnRequest(
            int contractVersion,
            TurnWindowMetadata window,
            long waitTimeoutMs,
            TurnOutcome previousOutcome) {
        this(contractVersion, window, waitTimeoutMs, previousOutcome, null);
    }
}
