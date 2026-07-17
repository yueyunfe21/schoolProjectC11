package com.bot.dhxy.cloud.remote;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

/**
 * W-TTPS-RECT-DHXY-DTO-IMP1: closed read-only projection of one bound window's task-tracker
 * panel-rectangle probe. Only {@code PRESENT} carries the complete anchor, rectangle, and score.
 */
@Value
@Jacksonized
public class RemoteTaskTrackerPanelRectFact {
    State state;
    Integer anchorClientX;
    Integer anchorClientY;
    Integer panelClientLeft;
    Integer panelClientTop;
    Integer panelClientRight;
    Integer panelClientBottom;
    Double matchScore;
    RemoteCoordinateSpace coordinateSpace;

    @Builder
    public RemoteTaskTrackerPanelRectFact(
            State state,
            Integer anchorClientX,
            Integer anchorClientY,
            Integer panelClientLeft,
            Integer panelClientTop,
            Integer panelClientRight,
            Integer panelClientBottom,
            Double matchScore,
            RemoteCoordinateSpace coordinateSpace) {
        this.state = requireNonNull(state, "state");
        this.coordinateSpace = requireNonNull(coordinateSpace, "coordinateSpace");
        require(coordinateSpace == RemoteCoordinateSpace.WINDOW_CLIENT_PX,
                "coordinateSpace must be WINDOW_CLIENT_PX");

        if (state == State.PRESENT) {
            this.anchorClientX = requireNonNegative(anchorClientX, "anchorClientX");
            this.anchorClientY = requireNonNegative(anchorClientY, "anchorClientY");
            this.panelClientLeft = requireNonNegative(panelClientLeft, "panelClientLeft");
            this.panelClientTop = requireNonNegative(panelClientTop, "panelClientTop");
            this.panelClientRight = requireNonNegative(panelClientRight, "panelClientRight");
            this.panelClientBottom = requireNonNegative(panelClientBottom, "panelClientBottom");
            require(this.panelClientRight > this.panelClientLeft,
                    "panelClientRight must be greater than panelClientLeft");
            require(this.panelClientBottom > this.panelClientTop,
                    "panelClientBottom must be greater than panelClientTop");
            this.matchScore = requireNonNull(matchScore, "matchScore");
            require(Double.isFinite(this.matchScore), "matchScore must be finite");
        } else {
            require(anchorClientX == null
                            && anchorClientY == null
                            && panelClientLeft == null
                            && panelClientTop == null
                            && panelClientRight == null
                            && panelClientBottom == null
                            && matchScore == null,
                    "non-PRESENT fact must not contain observation fields");
            this.anchorClientX = null;
            this.anchorClientY = null;
            this.panelClientLeft = null;
            this.panelClientTop = null;
            this.panelClientRight = null;
            this.panelClientBottom = null;
            this.matchScore = null;
        }
    }

    public enum State {
        PRESENT,
        ABSENT,
        CAPTURE_UNAVAILABLE,
        TEMPLATE_UNAVAILABLE,
        REPOSITION_REQUIRED,
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
