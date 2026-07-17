package com.bot.dhxy.cloud.turn.protocol;

public record TurnGiveItemOperationArguments(
        String targetItemTemplate,
        Integer knownBagIndex) {
}
