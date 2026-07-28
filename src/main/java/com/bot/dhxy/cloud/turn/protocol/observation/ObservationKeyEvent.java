package com.bot.dhxy.cloud.turn.protocol.observation;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * TURN-40G: one critical edge event. Key events are identified by their own {@code eventId}, are retained locally
 * until acknowledged by the Cloud, and are idempotent on the Cloud side (a resent duplicate changes nothing).
 * Correlation fields are carried per type: pathing edges carry {@code intentId}; xiuluo enter-battle edges carry
 * {@code attemptId}/{@code round}.
 *
 * @param eventId unique event identity (client-minted, stable across resends)
 * @param eventType key edge kind
 * @param occurredAtMs epoch millis the edge occurred locally
 * @param intentId optional pathing intent correlation
 * @param attemptId optional xiuluo green-chain attempt correlation
 * @param round optional xiuluo round correlation
 * @param source optional producer source tag
 * @param detail optional human-readable detail (e.g. click point and execution result)
 */
public record ObservationKeyEvent(
        String eventId,
        ObservationKeyEventType eventType,
        long occurredAtMs,
        @JsonInclude(JsonInclude.Include.NON_NULL) String intentId,
        @JsonInclude(JsonInclude.Include.NON_NULL) String attemptId,
        @JsonInclude(JsonInclude.Include.NON_NULL) Integer round,
        @JsonInclude(JsonInclude.Include.NON_NULL) String source,
        @JsonInclude(JsonInclude.Include.NON_NULL) String detail,
        @JsonInclude(JsonInclude.Include.NON_NULL) String expectedWaitId,
        @JsonInclude(JsonInclude.Include.NON_NULL) Long combatGeneration,
        @JsonInclude(JsonInclude.Include.NON_NULL) String taskCode,
        @JsonInclude(JsonInclude.Include.NON_NULL) String businessTaskRunId) {

    /** Backward-compatible constructor for existing observation edges. */
    public ObservationKeyEvent(String eventId, ObservationKeyEventType eventType, long occurredAtMs,
                               String intentId, String attemptId, Integer round, String source, String detail) {
        this(eventId, eventType, occurredAtMs, intentId, attemptId, round, source, detail,
                null, null, null, null);
    }

    /** Backward-compatible constructor for expected-combat edges carrying wait/generation correlation. */
    public ObservationKeyEvent(String eventId, ObservationKeyEventType eventType, long occurredAtMs,
                               String intentId, String attemptId, Integer round, String source, String detail,
                               String expectedWaitId, Long combatGeneration) {
        this(eventId, eventType, occurredAtMs, intentId, attemptId, round, source, detail,
                expectedWaitId, combatGeneration, null, null);
    }
}
