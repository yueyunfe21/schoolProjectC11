package com.bot.dhxy.cloud.turn.protocol;

/** Result of the client-authoritative combat-entry cleanup transaction. */
public record TurnCombatCleanupFact(
        String clearedIntentId,
        boolean dialogInterestCleared,
        boolean dialogPreparationCleared,
        boolean targetMapGateCleared) {
}
