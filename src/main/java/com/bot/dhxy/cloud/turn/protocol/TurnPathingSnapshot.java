package com.bot.dhxy.cloud.turn.protocol;

/**
 * Typed pathing snapshot the DHXY local runner maps from its authoritative
 * {@code WindowRuntimeContext.getPathingSnapshot()} into {@link TurnWindowMetadata} so the Cloud
 * business layer can read a fact mirror without ever observing movement itself.
 *
 * <p>This is a pure wire record on the Local Pathing Fact Bridge: DHXY produces it, Cloud consumes
 * it read-only. The Cloud mirror never registers, observes, clears, or overwrites; an absent, older,
 * or intent-mismatched snapshot is ignored, not treated as business truth. {@code state} is the local
 * pathing state name (for example {@code NONE}/{@code ACTIVE}/{@code ARRIVED}/{@code STOPPED_AWAY}/
 * {@code UNKNOWN}); coordinates are logical in-game map coordinates; timestamps are wall-clock
 * milliseconds. {@code coordinateMovementObserved} is Runner's latched verdict that at least one
 * logical coordinate changed during this exact intent; no derived movement timestamp is transported.</p>
 *
 * @param state local pathing state name.
 * @param intent the pathing intent this snapshot observes, or null when idle.
 * @param currentMapName last observed map name, when known.
 * @param currentX last observed logical X, nullable.
 * @param currentY last observed logical Y, nullable.
 * @param locationChangedAtMs wall-clock time the observed map/coordinate last changed.
 * @param coordinateMovementObserved whether Runner observed any logical-coordinate movement.
 * @param updatedAtMs wall-clock time of the latest observation.
 * @param dialogBlocking whether a fresh dialog needs task attention for this route.
 * @param dialogBlockingReason diagnostic reason for the dialog-blocking observation.
 * @param dialogBlockingDetectedAtMs wall-clock time the dialog-blocking observation was made, or 0.
 */
public record TurnPathingSnapshot(
        String state,
        TurnPathingIntent intent,
        String currentMapName,
        Integer currentX,
        Integer currentY,
        long locationChangedAtMs,
        boolean coordinateMovementObserved,
        long updatedAtMs,
        boolean dialogBlocking,
        String dialogBlockingReason,
        long dialogBlockingDetectedAtMs) {
}
