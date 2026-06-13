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
     * Whether the mini-map physical click point may be jittered after coordinate conversion.
     *
     * <p>Normal walking should keep this true. Precision cells such as shop entrances can set this
     * false so the click lands on the exact transformed mini-map point while still using the normal
     * navigation retry and handoff flow.</p>
     */
    @Builder.Default
    boolean randomizeMiniMapClickPoint = true;

    /**
     * Logical coordinate tolerance for deciding arrival.
     *
     * <p>Normal map/NPC navigation can accept a small logical-coordinate delta because the game often
     * stops beside the clicked logical point.</p>
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
