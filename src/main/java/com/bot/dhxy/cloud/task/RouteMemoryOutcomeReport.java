package com.bot.dhxy.cloud.task;

import lombok.Builder;
import lombok.Value;

/**
 * Route-memory outcome observed locally after a route click was submitted.
 *
 * @param routeDecisionId cloud-issued route decision id for authoritative memory HIT outcomes;
 *                        nullable only for non-memory learn-candidate reports.
 * @param intentId local pathing intent id that owned watcher settlement.
 * @param fromMap canonical source map name before route submission.
 * @param targetMap canonical destination map name.
 * @param routeMode route-memory mode, for example {@code YELLOW_DESTINATION_MINI_MAP}.
 * @param clickX game-window-relative X of the route-result click.
 * @param clickY game-window-relative Y of the route-result click.
 * @param observedMap map observed by the runner watcher when settling.
 * @param observedX observed current X coordinate, nullable when OCR/template location is unavailable.
 * @param observedY observed current Y coordinate, nullable when OCR/template location is unavailable.
 * @param result watcher-settled outcome.
 * @param elapsedMs elapsed milliseconds between pending creation and settlement.
 * @param reason diagnostic settlement reason.
 * @param source local source that created the pending token.
 */
@Value
@Builder
public class RouteMemoryOutcomeReport {

    public enum Result {
        SUCCESS,
        FAILURE,
        ABANDONED,
        LEARN_CANDIDATE
    }

    String routeDecisionId;
    String intentId;
    String fromMap;
    String targetMap;
    String routeMode;
    Integer clickX;
    Integer clickY;
    String observedMap;
    Integer observedX;
    Integer observedY;
    Result result;
    long elapsedMs;
    String reason;
    String source;
}
