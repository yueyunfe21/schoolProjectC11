package com.bot.dhxy.window.model;

import lombok.Builder;
import lombok.Value;

import java.util.UUID;

/**
 * Navigation target currently being watched by a window-level background probe.
 *
 * @param source diagnostic source that started pathing.
 * @param intentId per-navigation trace id used to reject prepared dialog actions from older route
 *                 attempts.
 * @param targetMapName destination map name, when known.
 * @param targetX destination logical X coordinate on the target map, nullable for map-only routes.
 * @param targetY destination logical Y coordinate on the target map, nullable for map-only routes.
 * @param tolerance logical coordinate tolerance used when deciding arrival.
 * @param type semantic pathing type. Untargeted tracker clicks do not have a coordinate arrival.
 * @param createdAtMs wall-clock timestamp when the pathing intent was registered.
 */
@Value
@Builder(toBuilder = true)
public class WindowPathingIntent {
    @Builder.Default
    String source = "navigation";
    @Builder.Default
    String intentId = UUID.randomUUID().toString();
    String targetMapName;
    Integer targetX;
    Integer targetY;
    @Builder.Default
    int tolerance = 5;
    @Builder.Default
    WindowPathingIntentType type = WindowPathingIntentType.TARGETED;
    @Builder.Default
    long createdAtMs = System.currentTimeMillis();
}
