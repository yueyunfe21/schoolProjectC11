package com.bot.dhxy.cloud.turn.protocol;

public record TurnStep(
        int index,
        TurnStepType type,
        TurnInputAction inputAction,
        TurnInputSpec input,
        Long waitMs,
        TurnCaptureSpec capture,
        TurnMatchSpec match,
        TurnLocalServiceCall localService) {
}
