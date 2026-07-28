package com.bot.dhxy.cloud.turn.protocol;

public record TurnMapSurveyCommand(
        String commandId,
        Operation operation,
        String mapName) {

    public enum Operation {
        SAVE_MAP_LABEL_SAMPLE,
        TEST_MAP_LABEL_SAMPLE,
        RECORD_LEFT_BOUNDARY,
        RECORD_RIGHT_BOUNDARY,
        RECORD_TOP_BOUNDARY,
        RECORD_BOTTOM_BOUNDARY,
        RECORD_CENTER_ANCHOR,
        TEST_PROJECTED_POINT,
        RECORD_CORRECTION,
        TEST_CORRECTED_POINT,
        UNDO_LAST_RECORD
    }
}
