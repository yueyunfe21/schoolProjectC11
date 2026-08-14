package com.bot.dhxy.cloud.turn.protocol;

/**
 * Protocol mirror of the baseline {@code PendingRouteOutcome} carried by the
 * {@code WHOLE_TASK_PENDING_ROUTE_OUTCOME_READ} result and the
 * {@code WHOLE_TASK_PENDING_ROUTE_OUTCOME_REPLACE} argument (P-PROTO, parent Amendment #6).
 *
 * <p>The eleven fields map one-to-one to the baseline {@code WindowRuntimeContext} pending
 * route-outcome slot; {@code routeMode} is the {@code WorldMapRouteResultMode} enum constant name.
 * No new business inputs are introduced. Coordinate fields are nullable when the baseline value was
 * absent.</p>
 */
public record TurnPendingRouteOutcome(
        String fromMap,
        String targetMap,
        String routeMode,
        Integer relativeX,
        Integer relativeY,
        String matchedText,
        String source,
        boolean usedMemory,
        String routeDecisionId,
        String intentId,
        long createdAtMs,
        String searchToken,
        String searchGeometryProfile,
        Integer searchCandidateRelativeX,
        Integer searchCandidateRelativeY,
        Integer searchCandidateRow,
        String searchCandidateEvidence,
        boolean searchCandidateUsedMemory) {
}
