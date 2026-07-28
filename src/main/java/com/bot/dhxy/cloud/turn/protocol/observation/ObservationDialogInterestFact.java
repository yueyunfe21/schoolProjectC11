package com.bot.dhxy.cloud.turn.protocol.observation;

import java.util.List;

/**
 * Exact local dialog-interest snapshot carried by the observation plane.
 *
 * <p>The fact declares only what the current task run is willing to have prepared. It grants no
 * execution authority. A cleared fact retains the replaced interest id so Cloud can reject frames
 * captured for the previous interest without maintaining a second interest store.</p>
 */
public record ObservationDialogInterestFact(
        String taskRunId,
        String windowId,
        String hwnd,
        String interestId,
        boolean active,
        String taskCode,
        List<String> operations,
        String source,
        long createdAtMs,
        long expiresAtMs,
        long absentAllowedAtMs,
        long probeStartAtMs,
        boolean probeOnly,
        String attemptId,
        Integer round,
        String intentId,
        boolean enterBattleClaimed) {

    /** Backward-compatible constructor for pre-demand-gate payloads. */
    public ObservationDialogInterestFact(
            String taskRunId,
            String windowId,
            String hwnd,
            String interestId,
            boolean active,
            String taskCode,
            List<String> operations,
            String source,
            long createdAtMs,
            long expiresAtMs,
            long absentAllowedAtMs,
            long probeStartAtMs,
            boolean probeOnly,
            String attemptId,
            Integer round,
            String intentId) {
        this(taskRunId, windowId, hwnd, interestId, active, taskCode, operations, source,
                createdAtMs, expiresAtMs, absentAllowedAtMs, probeStartAtMs, probeOnly,
                attemptId, round, intentId, false);
    }
}
