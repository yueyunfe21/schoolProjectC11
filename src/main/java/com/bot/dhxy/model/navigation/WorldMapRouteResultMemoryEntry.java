package com.bot.dhxy.model.navigation;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

/**
 * Persisted click memory for one world-map route-result row.
 *
 * @param fromMap canonical source map name; blank values are not persisted.
 * @param targetMap canonical destination map name; blank values are not persisted.
 * @param routeMode route-result click mode. Null persisted values are treated as legacy green-link
 *                  entries only.
 * @param relativeX game-window-relative X of the remembered route-result click point.
 * @param relativeY game-window-relative Y of the remembered route-result click point.
 * @param matchedText OCR/template text associated with the clicked route row, nullable.
 * @param successCount total live watcher-confirmed successes.
 * @param failureCount total live watcher-confirmed failures.
 * @param consecutiveSuccessCount consecutive live successes since the last failure.
 * @param consecutiveFailureCount consecutive live failures since the last success.
 * @param clean true only after enough consecutive live successes; only clean entries may fast-click.
 * @param disabled manual/future kill switch for this route key.
 * @param lastSuccessAt ISO local timestamp for the latest live success, nullable.
 * @param lastFailureAt ISO local timestamp for the latest live failure, nullable.
 * @param lastAbandonedAt ISO local timestamp for the latest abandoned pending attempt, nullable.
 * @param source diagnostic source that last updated this entry.
 */
@Value
@Builder(toBuilder = true)
@Jacksonized
public class WorldMapRouteResultMemoryEntry {
    String fromMap;
    String targetMap;
    WorldMapRouteResultMode routeMode;
    int relativeX;
    int relativeY;
    String matchedText;
    int successCount;
    int failureCount;
    int consecutiveSuccessCount;
    int consecutiveFailureCount;
    boolean clean;
    boolean disabled;
    String lastSuccessAt;
    String lastFailureAt;
    String lastAbandonedAt;
    String source;

    public boolean usable() {
        return clean && !disabled;
    }
}
