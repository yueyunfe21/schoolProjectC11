package com.bot.dhxy.cloud.turn.protocol;

import com.fasterxml.jackson.annotation.JsonInclude;

public record TurnInputSpec(
        Integer x,
        Integer y,
        Integer endX,
        Integer endY,
        Integer scrollDelta,
        String key,
        String text,
        @JsonInclude(JsonInclude.Include.NON_NULL)
        Integer clickDelayMs,
        @JsonInclude(JsonInclude.Include.NON_NULL)
        Integer queueHoldMs) {

    public TurnInputSpec(
            Integer x,
            Integer y,
            Integer endX,
            Integer endY,
            Integer scrollDelta,
            String key,
            String text) {
        this(x, y, endX, endY, scrollDelta, key, text, null, null);
    }
}
