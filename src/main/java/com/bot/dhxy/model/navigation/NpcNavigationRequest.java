package com.bot.dhxy.model.navigation;

import lombok.Builder;
import lombok.Value;

/**
 * Request to navigate to a fixed logical coordinate associated with an NPC.
 */
@Value
@Builder(toBuilder = true)
public class NpcNavigationRequest {
    /**
     * Destination map name.
     */
    String targetMapName;

    /**
     * Logical in-game X coordinate of the NPC/target.
     */
    int targetX;

    /**
     * Logical in-game Y coordinate of the NPC/target.
     */
    int targetY;

    /**
     * NPC name used for diagnostics.
     */
    String targetName;

    /**
     * Whether the caller must keep the task turn after pathing starts and handle the next dialog/action itself.
     */
    @Builder.Default
    boolean keepTaskTurnUntilHandled = false;

    /**
     * Short log source for diagnostics.
     */
    @Builder.Default
    String source = "navigateToNPC";

    public static NpcNavigationRequest npc(String targetMapName, int targetX, int targetY, String targetName) {
        return NpcNavigationRequest.builder()
                .targetMapName(targetMapName)
                .targetX(targetX)
                .targetY(targetY)
                .targetName(targetName)
                .source("navigateToNPC")
                .build();
    }

}
