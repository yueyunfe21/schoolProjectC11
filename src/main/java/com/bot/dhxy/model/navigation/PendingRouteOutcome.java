package com.bot.dhxy.model.navigation;

import lombok.Builder;
import lombok.Value;

/**
 * In-memory evidence for one cloud-authorized world-map route click.
 *
 * <p>This object is scoped to a live window runtime only. It is not a route cache, never selects a
 * click locally, and is consumed once the pathing watcher reports the route outcome to cloud.</p>
 *
 * @param fromMap canonical source map captured before the route search.
 * @param targetMap canonical destination map typed into the route search.
 * @param routeMode explicit route-result click mode returned by the cloud decision.
 * @param relativeX window-relative X clicked in the route-result list.
 * @param relativeY window-relative Y clicked in the route-result list.
 * @param matchedText destination text associated with the clicked row, nullable.
 * @param source diagnostic source that created this live outcome record.
 * @param usedMemory true only when cloud declared the decision a memory hit.
 * @param routeDecisionId cloud-issued decision id that owns the eventual outcome report.
 * @param intentId active pathing intent id that owns settlement.
 * @param createdAtMs wall-clock timestamp when this live record was created.
 */
@Value
@Builder(toBuilder = true)
public class PendingRouteOutcome {
    String fromMap;
    String targetMap;
    WorldMapRouteResultMode routeMode;
    Integer relativeX;
    Integer relativeY;
    String matchedText;
    String source;
    boolean usedMemory;
    String routeDecisionId;
    String intentId;
    long createdAtMs;
}
