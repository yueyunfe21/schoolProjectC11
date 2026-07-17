package com.bot.dhxy.cloud.turn.protocol;

public record TurnResponse(
        Status status,
        TurnAction action,
        TurnTaskStartAck taskStartAck) {

    public TurnResponse(Status status, TurnAction action) {
        this(status, action, null);
    }

    public enum Status {
        ACTION,
        IDLE
    }
}
