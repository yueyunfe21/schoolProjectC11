package com.bot.dhxy.cloud.turn.protocol;

public record TurnMapSurveyPointerSample(
        String deviceId,
        String windowId,
        long hwnd,
        int screenX,
        int screenY,
        long sampledAtMs) {
}
