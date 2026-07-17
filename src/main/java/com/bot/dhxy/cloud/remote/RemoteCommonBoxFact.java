package com.bot.dhxy.cloud.remote;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

/**
 * W-CBOX-LOCAL-DTO-IMP1: closed read-only projection of one bound window's common-box template probe.
 * Mirrors the Cloud common-box fact contract exactly: a {@code MATCHED} fact carries the full clickable
 * result and validates it; every negative state carries only {@code state} and {@code coordinateSpace}.
 */
@Value
@Jacksonized
public class RemoteCommonBoxFact {
    State state;
    Integer clickX;
    Integer clickY;
    Double matchScore;
    Long matchedAtEpochMs;
    RemoteCoordinateSpace coordinateSpace;

    @Builder
    public RemoteCommonBoxFact(
            State state,
            Integer clickX,
            Integer clickY,
            Double matchScore,
            Long matchedAtEpochMs,
            RemoteCoordinateSpace coordinateSpace) {
        this.state = requireNonNull(state, "state");
        this.coordinateSpace = requireNonNull(coordinateSpace, "coordinateSpace");
        require(coordinateSpace == RemoteCoordinateSpace.SCREEN_ABSOLUTE_PX,
                "coordinateSpace must be SCREEN_ABSOLUTE_PX");

        if (state == State.MATCHED) {
            this.clickX = requireNonNegative(clickX, "clickX");
            this.clickY = requireNonNegative(clickY, "clickY");
            requireNonNull(matchScore, "matchScore");
            require(Double.isFinite(matchScore) && matchScore >= 0.86d,
                    "matchScore must be finite and at least 0.86");
            this.matchScore = matchScore;
            this.matchedAtEpochMs = requirePositive(matchedAtEpochMs, "matchedAtEpochMs");
        } else {
            require(clickX == null && clickY == null && matchScore == null && matchedAtEpochMs == null,
                    "non-MATCHED fact must not contain click, score, or timestamp fields");
            this.clickX = null;
            this.clickY = null;
            this.matchScore = null;
            this.matchedAtEpochMs = null;
        }
    }

    public enum State {
        MATCHED,
        NOT_MATCHED,
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

    private static Long requirePositive(Long value, String field) {
        if (value == null) {
            throw new IllegalArgumentException(field + " must not be null");
        }
        if (value <= 0L) {
            throw new IllegalArgumentException(field + " must be positive");
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
