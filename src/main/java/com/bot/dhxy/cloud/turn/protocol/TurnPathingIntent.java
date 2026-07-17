package com.bot.dhxy.cloud.turn.protocol;

/**
 * Typed pathing intent the Cloud business layer attaches to a start action so the DHXY local runner
 * can register it against the existing {@code WindowRuntimeContext} pathing slot after the action
 * completes with a positive local movement proof.
 *
 * <p>This is a pure wire record on the Local Pathing Fact Bridge: Cloud produces it, DHXY consumes
 * it. Cloud never observes movement/arrival itself; the intent only travels down. Coordinates are the
 * logical in-game map coordinates; {@code targetX}/{@code targetY} may be null for map-only routes.
 * {@code type} is the intent's semantic label name (for example {@code TARGETED} or {@code UNTARGETED}),
 * carried as text so the protocol record stays independent of any local window model type.</p>
 *
 * @param source diagnostic source that started pathing.
 * @param intentId per-navigation trace id used to reject facts from older route attempts.
 * @param targetMapName destination map name, when known.
 * @param targetX destination logical X coordinate, nullable for map-only routes.
 * @param targetY destination logical Y coordinate, nullable for map-only routes.
 * @param tolerance logical coordinate tolerance used when the local runner decides arrival.
 * @param type semantic pathing intent type name.
 */
public record TurnPathingIntent(
        String source,
        String intentId,
        String targetMapName,
        Integer targetX,
        Integer targetY,
        int tolerance,
        String type) {
}
