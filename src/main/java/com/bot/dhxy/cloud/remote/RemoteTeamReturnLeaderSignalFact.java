package com.bot.dhxy.cloud.remote;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

/**
 * W-TEAMRETURN-LEADER-DHXY-WIRE-IMP1: closed read-only projection of one bound window's team-return
 * leader-signal template probe. Mirrors the Cloud team-return-leader-signal fact contract exactly: a
 * {@code PRESENT} fact carries the full signal result and validates it; every other state carries only
 * {@code state} and {@code coordinateSpace} (the three conditional fields stay null and are omitted on
 * the wire).
 */
@Value
@Jacksonized
public class RemoteTeamReturnLeaderSignalFact {
    State state;
    Integer signalX;
    Integer signalY;
    Double matchScore;
    RemoteCoordinateSpace coordinateSpace;

    @Builder
    public RemoteTeamReturnLeaderSignalFact(
            State state,
            Integer signalX,
            Integer signalY,
            Double matchScore,
            RemoteCoordinateSpace coordinateSpace) {
        this.state = requireNonNull(state, "state");
        this.coordinateSpace = requireNonNull(coordinateSpace, "coordinateSpace");
        require(coordinateSpace == RemoteCoordinateSpace.SCREEN_ABSOLUTE_PX,
                "coordinateSpace must be SCREEN_ABSOLUTE_PX");

        if (state == State.PRESENT) {
            this.signalX = requireNonNegative(signalX, "signalX");
            this.signalY = requireNonNegative(signalY, "signalY");
            requireNonNull(matchScore, "matchScore");
            require(Double.isFinite(matchScore), "matchScore must be finite");
            this.matchScore = matchScore;
        } else {
            require(signalX == null && signalY == null && matchScore == null,
                    "non-PRESENT fact must not contain signal or score fields");
            this.signalX = null;
            this.signalY = null;
            this.matchScore = null;
        }
    }

    public enum State {
        PRESENT,
        ABSENT,
        CAPTURE_UNAVAILABLE,
        TEMPLATE_UNAVAILABLE,
        MECHANICS_FAILED
    }

    private static Integer requireNonNegative(Integer value, String field) {
        if (value == null) {
            throw new IllegalArgumentException(field + " must not be null");
        }
        if (value < 0) {
            throw new IllegalArgumentException(field + " must be non-negative");
        }
        return value;
    }

    private static <T> T requireNonNull(T value, String field) {
        if (value == null) {
            throw new IllegalArgumentException(field + " must not be null");
        }
        return value;
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalArgumentException(message);
        }
    }
}
