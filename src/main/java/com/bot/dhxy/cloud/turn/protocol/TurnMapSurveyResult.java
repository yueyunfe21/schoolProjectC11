package com.bot.dhxy.cloud.turn.protocol;

public record TurnMapSurveyResult(
        String commandId,
        Status status,
        String message,
        String mapName,
        Integer projectedX,
        Integer projectedY,
        String evidenceJson) {

    public TurnMapSurveyResult(String commandId,
                               Status status,
                               String message,
                               String mapName,
                               Integer projectedX,
                               Integer projectedY) {
        this(commandId, status, message, mapName, projectedX, projectedY, null);
    }

    public enum Status {
        ACCEPTED,
        COMPLETED,
        FAILED
    }
}
