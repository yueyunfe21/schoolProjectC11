package com.bot.dhxy.cloud.turn.protocol.observation;

/**
 * TURN-40G: mechanical, business-free fact kinds a local window observation runner may report. Facts carry no task
 * interpretation; the Cloud consumes them to advance its own state machines.
 */
public enum ObservationFactType {
    /** Fixed-template or pixel-difference combat signal sample (no business hysteresis attached). */
    COMBAT_SIGNAL,
    /** Local position fast-path sample (template-first minimap coordinate strip result). */
    POSITION_SAMPLE,
    /** Local runtime timer edge (e.g. pre-battle timer progression) without consumption semantics. */
    TIMER_EDGE
}
