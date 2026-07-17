package com.bot.dhxy.cloud.turn.protocol;

public record TurnReturnItemCachePoint(
        String templatePath,
        int clickX,
        int clickY,
        long learnedAtMs,
        String source) {
}
