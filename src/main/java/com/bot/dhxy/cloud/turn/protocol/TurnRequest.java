package com.bot.dhxy.cloud.turn.protocol;

public record TurnRequest(
        int contractVersion,
        TurnWindowMetadata window,
        long waitTimeoutMs,
        TurnOutcome previousOutcome,
        TurnTaskStartRequest taskStartRequest,
        TurnContinuationRequest continuation,
        TurnMapSurveyCommand mapSurveyCommand,
        String mapSurveyResultAckId) {

    public TurnRequest(int contractVersion, TurnWindowMetadata window, long waitTimeoutMs,
                       TurnOutcome previousOutcome, TurnTaskStartRequest taskStartRequest,
                       TurnContinuationRequest continuation) {
        this(contractVersion, window, waitTimeoutMs, previousOutcome, taskStartRequest, continuation, null, null);
    }

    public TurnRequest(
            int contractVersion,
            TurnWindowMetadata window,
            long waitTimeoutMs,
            TurnOutcome previousOutcome) {
        this(contractVersion, window, waitTimeoutMs, previousOutcome, null, null, null, null);
    }

    public TurnRequest(
            int contractVersion,
            TurnWindowMetadata window,
            long waitTimeoutMs,
            TurnOutcome previousOutcome,
            TurnTaskStartRequest taskStartRequest) {
        this(contractVersion, window, waitTimeoutMs, previousOutcome, taskStartRequest, null, null, null);
    }
}
