package com.bot.dhxy.model.navigation;

import lombok.Builder;
import lombok.Value;

/**
 * Request to navigate across maps by map name.
 */
@Value
@Builder(toBuilder = true)
public class MapNavigationRequest {
    /**
     * Game map name to type/select in the world-map navigation UI.
     */
    String targetMapName;

    /**
     * Whether the caller must keep the task turn after pathing starts and handle the next step itself.
     */
    @Builder.Default
    boolean keepTaskTurnUntilHandled = false;

    /**
     * Short log source for diagnostics.
     */
    @Builder.Default
    String source = "navigateToMap";

    public static MapNavigationRequest toMap(String targetMapName) {
        return MapNavigationRequest.builder()
                .targetMapName(targetMapName)
                .build();
    }
}
