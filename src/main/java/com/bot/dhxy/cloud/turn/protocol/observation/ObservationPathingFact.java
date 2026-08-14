package com.bot.dhxy.cloud.turn.protocol.observation;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Bounded typed mirror of one exact-window local pathing snapshot.
 *
 * <p>The fact is mechanical state only. It grants no action authority and carries no diagnostic detail string.
 * A clear retains the cleared intent identity; a replacement carries both the current and replaced intent ids.</p>
 */
public record ObservationPathingFact(
        String taskRunId,
        String windowId,
        String hwnd,
        String intentId,
        @JsonInclude(JsonInclude.Include.NON_NULL) String replacedIntentId,
        String source,
        @JsonInclude(JsonInclude.Include.NON_NULL) String targetMapName,
        @JsonInclude(JsonInclude.Include.NON_NULL) Integer targetX,
        @JsonInclude(JsonInclude.Include.NON_NULL) Integer targetY,
        int tolerance,
        ObservationPathingType pathingType,
        long pathingStartedAtMs,
        long pathingUpdatedAtMs,
        ObservationPathingState state,
        ObservationPathingTransition transition,
        @JsonInclude(JsonInclude.Include.NON_NULL) String currentMapName,
        @JsonInclude(JsonInclude.Include.NON_NULL) Integer currentX,
        @JsonInclude(JsonInclude.Include.NON_NULL) Integer currentY,
        long locationChangedAtMs,
        boolean coordinateMovementObserved,
        boolean dialogBlocking,
        @JsonInclude(JsonInclude.Include.NON_NULL) String dialogBlockingReason,
        long dialogBlockingDetectedAtMs,
        @JsonInclude(JsonInclude.Include.NON_NULL) Long terminalFrameId,
        @JsonInclude(JsonInclude.Include.NON_NULL) Long terminalFrameGeneration) {

    public ObservationPathingFact(
            String taskRunId,
            String windowId,
            String hwnd,
            String intentId,
            String replacedIntentId,
            String source,
            String targetMapName,
            Integer targetX,
            Integer targetY,
            int tolerance,
            ObservationPathingType pathingType,
            long pathingStartedAtMs,
            long pathingUpdatedAtMs,
            ObservationPathingState state,
            ObservationPathingTransition transition,
            String currentMapName,
            Integer currentX,
            Integer currentY,
            long locationChangedAtMs,
            boolean coordinateMovementObserved,
            boolean dialogBlocking,
            String dialogBlockingReason,
            long dialogBlockingDetectedAtMs) {
        this(taskRunId, windowId, hwnd, intentId, replacedIntentId, source, targetMapName,
                targetX, targetY, tolerance, pathingType, pathingStartedAtMs, pathingUpdatedAtMs,
                state, transition, currentMapName, currentX, currentY, locationChangedAtMs,
                coordinateMovementObserved,
                dialogBlocking, dialogBlockingReason, dialogBlockingDetectedAtMs,
                null, null);
    }
}
