package com.bot.dhxy.cloud.turn.protocol.observation;

/**
 * TURN-40G: one mechanical fact sample carried by an observation request. Facts are plain data with no business
 * interpretation attached; a newer ordinary sample for the same window may replace an unsent older one locally.
 *
 * @param factType mechanical fact kind
 * @param value textual sample payload (e.g. a signal name or "x,y" coordinate pair)
 * @param observedAtMs epoch millis the sample was taken locally
 */
public record ObservationFact(
        ObservationFactType factType,
        String value,
        long observedAtMs) {
}
