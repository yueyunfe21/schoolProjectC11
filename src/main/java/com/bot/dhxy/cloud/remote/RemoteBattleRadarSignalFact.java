package com.bot.dhxy.cloud.remote;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

/**
 * W-696-BATTLE-RADAR-DHXY-FACT-1: closed read-only projection of one bound window's battle-radar
 * signal probe (auto flag / selection buttons / top icons). Mirrors the Cloud
 * {@code WindowFact.BattleRadarSignalFact} contract exactly: only a closed {@code state} with no
 * coordinates. A mechanics/transport failure is never disguised as {@code NOT_VISIBLE}.
 */
@Value
@Jacksonized
public class RemoteBattleRadarSignalFact {
    State state;

    @Builder
    public RemoteBattleRadarSignalFact(State state) {
        this.state = requireNonNull(state, "state");
    }

    public enum State {
        VISIBLE,
        NOT_VISIBLE,
        CAPTURE_UNAVAILABLE,
        MECHANICS_FAILED
    }

    private static <T> T requireNonNull(T value, String field) {
        if (value == null) {
            throw new IllegalArgumentException(field + " must not be null");
        }
        return value;
    }
}
