package com.bot.dhxy.cloud.turn.protocol;

/** Flat authoritative ordinary pre-battle timer and target-map gate fact. */
public record TurnPreBattleFact(
        String taskCode,
        String source,
        String targetKeyword,
        long startedAtMs,
        long publishedAtMs,
        boolean newlyPublished,
        String gateTargetMapName,
        String gateSource,
        long gateStartedAtMs,
        long gateOpenedAtMs) {
}
