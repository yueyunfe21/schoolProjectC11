package com.bot.dhxy.cloud.remote;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

/**
 * W-TEAMRETURN-BUTTON-DHXY-WIRE-IMP1: closed read-only projection of one bound window's team-return
 * button template probe. Mirrors the Cloud team-return-button fact contract exactly: a {@code PRESENT}
 * fact carries the full clickable result and validates it; every other state carries only {@code state}
 * and {@code coordinateSpace} (the three conditional fields stay null and are omitted on the wire).
 */
@Value
@Jacksonized
public class RemoteTeamReturnButtonFact {
    State state;
    Integer clickX;
    Integer clickY;
    Double matchScore;
    RemoteCoordinateSpace coordinateSpace;

    @Builder
    public RemoteTeamReturnButtonFact(
            State state,
            Integer clickX,
            Integer clickY,
            Double matchScore,
            RemoteCoordinateSpace coordinateSpace) {
        this.state = requireNonNull(state, "state");
        this.coordinateSpace = requireNonNull(coordinateSpace, "coordinateSpace");
        require(coordinateSpace == RemoteCoordinateSpace.SCREEN_ABSOLUTE_PX,
                "coordinateSpace must be SCREEN_ABSOLUTE_PX");

        if (state == State.PRESENT) {
            this.clickX = requireNonNegative(clickX, "clickX");
            this.clickY = requireNonNegative(clickY, "clickY");
            requireNonNull(matchScore, "matchScore");
            require(Double.isFinite(matchScore), "matchScore must be finite");
            this.matchScore = matchScore;
        } else {
            require(clickX == null && clickY == null && matchScore == null,
                    "non-PRESENT fact must not contain click or score fields");
            this.clickX = null;
            this.clickY = null;
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
