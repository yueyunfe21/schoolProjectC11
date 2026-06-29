package com.bot.dhxy.model.navigation;

import lombok.Builder;
import lombok.Value;

/**
 * Live world-map route-result click waiting for watcher settlement.
 *
 * @param fromMap canonical source map name captured before the route search.
 * @param targetMap canonical destination map name typed into the route search.
 * @param routeMode route-result click mode. Null keeps old legacy green-link semantics.
 * @param relativeX game-window-relative X clicked in the route-result list.
 * @param relativeY game-window-relative Y clicked in the route-result list.
 * @param matchedText OCR/template text associated with the clicked row, nullable.
 * @param source diagnostic source that created this pending memory.
 * @param usedMemory true when the click came from a clean remembered point; false for OCR path.
 * @param intentId active {@link com.bot.dhxy.window.model.WindowPathingIntent} id that owns settlement.
 * @param createdAtMs wall-clock timestamp when the pending record was created.
 */
@Value
@Builder(toBuilder = true)
public class WorldMapRouteResultPendingMemory {
    String fromMap;
    String targetMap;
    WorldMapRouteResultMode routeMode;
    Integer relativeX;
    Integer relativeY;
    String matchedText;
    String source;
    boolean usedMemory;
    String intentId;
    long createdAtMs;
}
