package com.bot.dhxy.cloud.turn.protocol;

import com.fasterxml.jackson.annotation.JsonInclude;

/** Exact accepted task-start terminal result published by Cloud on the ordinary turn channel. */
public record TurnTaskTerminalResult(
        String startRequestId,
        Status status,
        @JsonInclude(JsonInclude.Include.NON_NULL) String reason) {

    public TurnTaskTerminalResult(String startRequestId, Status status) {
        this(startRequestId, status, null);
    }

    public enum Status {
        SUCCESS,
        FAILED,
        STOPPED,
        SKIPPED
    }
}
