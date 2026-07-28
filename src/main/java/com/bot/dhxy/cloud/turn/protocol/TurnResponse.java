package com.bot.dhxy.cloud.turn.protocol;

public record TurnResponse(
        Status status,
        TurnAction action,
        TurnTaskStartAck taskStartAck,
        TurnContinuationDecision continuationDecision,
        TurnMapSurveyResult mapSurveyResult,
        TurnTaskTerminalResult taskTerminalResult) {

    public TurnResponse(Status status, TurnAction action, TurnTaskStartAck taskStartAck,
                        TurnContinuationDecision continuationDecision, TurnMapSurveyResult mapSurveyResult) {
        this(status, action, taskStartAck, continuationDecision, mapSurveyResult, null);
    }

    public TurnResponse(Status status, TurnAction action, TurnTaskStartAck taskStartAck,
                        TurnContinuationDecision continuationDecision) {
        this(status, action, taskStartAck, continuationDecision, null, null);
    }

    public TurnResponse(Status status, TurnAction action) {
        this(status, action, null, null, null, null);
    }

    public TurnResponse(Status status, TurnAction action, TurnTaskStartAck taskStartAck) {
        this(status, action, taskStartAck, null, null, null);
    }

    public enum Status {
        ACTION,
        IDLE,
        CONTINUATION
    }
}
