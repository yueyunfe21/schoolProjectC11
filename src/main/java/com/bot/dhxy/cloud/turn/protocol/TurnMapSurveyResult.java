package com.bot.dhxy.cloud.turn.protocol;

public record TurnMapSurveyResult(
        String commandId,
        Status status,
        String message,
        String mapName,
        Integer projectedX,
        Integer projectedY) {

    public enum Status {
        ACCEPTED,
        COMPLETED,
        FAILED
    }
}
