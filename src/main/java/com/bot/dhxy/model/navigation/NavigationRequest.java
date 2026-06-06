package com.bot.dhxy.model.navigation;

import lombok.Builder;
import lombok.Value;

/**
 * Request to navigate to one interactive target on a game map.
 */
@Value
@Builder(toBuilder = true)
public class NavigationRequest {
    /**
     * Destination map name visible in the game UI.
     */
    String targetMapName;

    /**
     * Logical in-game X coordinate of the target.
     */
    Integer targetX;

    /**
     * Logical in-game Y coordinate of the target.
     */
    Integer targetY;

    /**
     * Target name used only for diagnostics.
     */
    String targetName;

    /**
     * Return PATHING_STARTED as soon as navigation starts visible movement.
     */
    @Builder.Default
    boolean returnOnPathingStarted = false;

    /**
     * Publish PATHING_STARTED to the window-level background observer.
     *
     * <p>This is intentionally opt-in while the observer is being validated. Normal business tasks
     * should keep this false until the window-level pathing signal has been tested through the
     * navigation stress task and deliberately wired into that task's phase logic.</p>
     */
    @Builder.Default
    boolean publishWindowPathingIntent = false;

    /**
     * Click the requested mini-map coordinate exactly once and let the caller verify the result.
     *
     * <p>This is for interaction cells such as a shop entrance, where random jitter or fallback
     * logical offsets can miss the door. It should not be used for normal walk-to-coordinate
     * navigation because it does not prove coordinate arrival by itself.</p>
     */
    @Builder.Default
    boolean exactMiniMapClickOnly = false;

    /**
     * Logical coordinate tolerance for deciding arrival.
     *
     * <p>Normal map/NPC navigation can accept a small logical-coordinate delta because the game often
     * stops beside the clicked logical point. Precision cells such as shop entrances should either use
     * {@link #exactMiniMapClickOnly} or explicitly set this to {@code 0}.</p>
     */
    @Builder.Default
    int arrivalTolerance = 5;

    /**
     * Short log source for diagnostics.
     */
    @Builder.Default
    String source = "navigateToNPC";

    public static NavigationRequest target(String targetMapName, int targetX, int targetY, String targetName) {
        return NavigationRequest.builder()
                .targetMapName(targetMapName)
                .targetX(targetX)
                .targetY(targetY)
                .targetName(targetName)
                .build();
    }
}
