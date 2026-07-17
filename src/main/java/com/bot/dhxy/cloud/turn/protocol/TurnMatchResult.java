package com.bot.dhxy.cloud.turn.protocol;

public record TurnMatchResult(
        boolean found,
        double score,
        Integer centerX,
        Integer centerY,
        TurnRegion rectangle) {
}
