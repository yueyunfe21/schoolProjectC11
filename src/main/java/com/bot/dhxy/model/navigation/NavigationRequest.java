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
     * Pixel radius for randomized mini-map clicks when randomization is enabled.
     *
     * <p>The value is a screen-pixel radius applied after logical map coordinates are converted to
     * the mini-map click point. Most navigation should keep the default small radius. Task-specific
     * broad prepath clicks can set a larger radius without changing the logical target coordinate.</p>
     */
    @Builder.Default
    int miniMapClickRandomRadiusPx = 4;

    /**
     * Keep the current task turn when an in-map mini-map click starts movement.
     *
     * <p>This is for short 修罗 leader-only corrections such as returning home and walking a few
     * cells to the accept/heal-pet NPC. Long target navigation should keep the default false so
     * PATHING_STARTED can yield to other windows.</p>
     */
    @Builder.Default
    boolean keepTurnOnCurrentMapPathing = false;

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

    /**
     * Fresh map name already scanned by the caller immediately before this navigation request.
     *
     * <p>This is optional. It lets navigation skip a duplicate no-focus minimap/OCR scan when the
     * caller has just read the same window position for a nearby-NPC decision.</p>
     */
    String freshCurrentMapName;

    /**
     * Fresh logical X coordinate paired with {@link #freshCurrentMapName}; null when unavailable.
     */
    Integer freshCurrentX;

    /**
     * Fresh logical Y coordinate paired with {@link #freshCurrentMapName}; null when unavailable.
     */
    Integer freshCurrentY;

    /**
     * Epoch time in milliseconds when the caller captured the fresh current location.
     */
    @Builder.Default
    long freshCurrentLocationAtMs = 0L;

    /**
     * Treat the supplied fresh location as a task-owned snapshot instead of a short observation cache.
     *
     * <p>五倍和修罗的回城后接任务链会设置它。回程道具已经验证回到起始城后，导航必须一直
     * 复用这份任务事实，直到新的接任务 option 实际点击成功；云端 bookkeeping、重试或排队
     * 不能把它当作几秒后就失效的观察缓存。</p>
     */
    @Builder.Default
    boolean freshCurrentLocationPhaseBound = false;

    public static NavigationRequest target(String targetMapName, int targetX, int targetY, String targetName) {
        return NavigationRequest.builder()
                .targetMapName(targetMapName)
                .targetX(targetX)
                .targetY(targetY)
                .targetName(targetName)
                .build();
    }
}
