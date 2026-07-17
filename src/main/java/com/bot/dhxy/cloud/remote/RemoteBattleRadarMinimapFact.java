package com.bot.dhxy.cloud.remote;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

/**
 * W-696-BATTLE-RADAR-DHXY-FACT-1: closed read-only projection of one bound window's minimap
 * readability probe. Mirrors the Cloud {@code WindowFact.BattleRadarMinimapFact} contract exactly:
 * only a closed {@code state}. A mechanics failure is never disguised as {@code UNREADABLE}.
 */
@Value
@Jacksonized
public class RemoteBattleRadarMinimapFact {
    State state;

    @Builder
    public RemoteBattleRadarMinimapFact(State state) {
        this.state = requireNonNull(state, "state");
    }

    public enum State {
        READABLE,
        UNREADABLE,
        MECHANICS_FAILED
    }

    private static <T> T requireNonNull(T value, String field) {
        if (value == null) {
            throw new IllegalArgumentException(field + " must not be null");
        }
        return value;
    }
}
