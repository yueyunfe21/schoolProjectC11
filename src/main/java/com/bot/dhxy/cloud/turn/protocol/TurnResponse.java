package com.bot.dhxy.cloud.turn.protocol;

import java.util.List;

public record TurnResponse(
        Status status,
        TurnAction action,
        TurnTaskStartAck taskStartAck,
        TurnContinuationDecision continuationDecision,
        TurnMapSurveyResult mapSurveyResult,
        TurnTaskTerminalResult taskTerminalResult,
        List<TurnTaskQueueEvent> taskQueueEvents) {

    public TurnResponse {
        taskQueueEvents = taskQueueEvents == null ? List.of() : List.copyOf(taskQueueEvents);
    }

    public TurnResponse(Status status, TurnAction action, TurnTaskStartAck taskStartAck,
                        TurnContinuationDecision continuationDecision, TurnMapSurveyResult mapSurveyResult,
                        TurnTaskTerminalResult taskTerminalResult) {
        this(status, action, taskStartAck, continuationDecision, mapSurveyResult, taskTerminalResult, List.of());
    }

    public TurnResponse(Status status, TurnAction action, TurnTaskStartAck taskStartAck,
                        TurnContinuationDecision continuationDecision, TurnMapSurveyResult mapSurveyResult) {
        this(status, action, taskStartAck, continuationDecision, mapSurveyResult, null, List.of());
    }

    public TurnResponse(Status status, TurnAction action, TurnTaskStartAck taskStartAck,
                        TurnContinuationDecision continuationDecision) {
        this(status, action, taskStartAck, continuationDecision, null, null);
    }

    public TurnResponse(Status status, TurnAction action) {
        this(status, action, null, null, null, null, List.of());
    }

    public TurnResponse(Status status, TurnAction action, TurnTaskStartAck taskStartAck) {
        this(status, action, taskStartAck, null, null, null, List.of());
    }

    public enum Status {
        ACTION,
        IDLE,
        CONTINUATION
    }
}
