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
