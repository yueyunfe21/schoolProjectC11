package com.bot.dhxy.cloud.remote;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

/** Closed read-only projection of one bound window's auto-combat panel probe. */
@Value
@Jacksonized
public class RemoteAutoCombatPanelFact {
    State state;
    Integer panelCenterX;
    Integer panelCenterY;
    Integer greenMarkerX;
    Integer greenMarkerY;
    int greenTemplateWidth;
    String detectionSource;
    RemoteCoordinateSpace coordinateSpace;

    @Builder
    public RemoteAutoCombatPanelFact(
            State state,
            Integer panelCenterX,
            Integer panelCenterY,
            Integer greenMarkerX,
            Integer greenMarkerY,
            int greenTemplateWidth,
            String detectionSource,
            RemoteCoordinateSpace coordinateSpace) {
        this.state = requireNonNull(state, "state");
        require(greenTemplateWidth >= 0, "greenTemplateWidth must be non-negative");
        this.detectionSource = requireText(detectionSource, "detectionSource");
        this.coordinateSpace = requireNonNull(coordinateSpace, "coordinateSpace");
        require(coordinateSpace == RemoteCoordinateSpace.SCREEN_ABSOLUTE_PX,
                "coordinateSpace must be SCREEN_ABSOLUTE_PX");

        boolean hasPanelX = panelCenterX != null;
        boolean hasPanelY = panelCenterY != null;
        boolean hasMarkerX = greenMarkerX != null;
        boolean hasMarkerY = greenMarkerY != null;
        require(hasPanelX == hasPanelY,
                "panel center must contain both coordinates or neither");
        require(hasMarkerX == hasMarkerY,
                "green marker must contain both coordinates or neither");
        if (state == State.FOUND) {
            require(hasPanelX, "FOUND requires panel center coordinates");
        } else {
            require(!hasPanelX && !hasMarkerX,
                    "non-FOUND fact must not contain coordinates");
            require(greenTemplateWidth == 0,
                    "non-FOUND greenTemplateWidth must be zero");
        }
        this.panelCenterX = panelCenterX;
        this.panelCenterY = panelCenterY;
        this.greenMarkerX = greenMarkerX;
        this.greenMarkerY = greenMarkerY;
        this.greenTemplateWidth = greenTemplateWidth;
    }

    public enum State {
        FOUND,
        NOT_FOUND,
        CAPTURE_FAILED
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value.trim();
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
