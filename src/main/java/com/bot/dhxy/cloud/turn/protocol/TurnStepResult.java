package com.bot.dhxy.cloud.turn.protocol;

public record TurnStepResult(
        int index,
        TurnStepType type,
        Status status,
        String code,
        TurnMatchResult match,
        String localResultJson) {

    public enum Status {
        COMPLETED,
        FAILED,
        NOT_RUN
    }
}
