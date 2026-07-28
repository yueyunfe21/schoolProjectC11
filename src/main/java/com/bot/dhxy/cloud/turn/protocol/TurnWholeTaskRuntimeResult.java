package com.bot.dhxy.cloud.turn.protocol;

/**
 * Typed result payload for the closed {@code WHOLE_TASK_*} / {@code WUHUAN_ACCEPT_DIALOG_EXCLUSIVE}
 * local runtime operations (TURN-35 Amendment #6; extended by Amendment #12).
 *
 * <p>Only typed booleans, enum names, timestamps, cleared-intent identity and the flat dialog runtime
 * fact cross the wire — never local object references. Exactly one result field is populated per
 * operation and the rest must be null: register/clear hits, map confirmation, nearness and timer
 * start/pause outcomes use {@code booleanResult}; flying-state reads and the wuhuan exclusive accept
 * outcome use {@code enumResult} (the enum constant name); the pre-battle timer read uses
 * {@code timestampMs} (0 when no timer is active); pathing clears report the exact cleared intent via
 * {@code clearedIntentId} (null when the clear was a mismatch no-op); and the dialog runtime read
 * ({@code WHOLE_TASK_DIALOG_RUNTIME_READ}) uses {@code dialogRuntimeFact} — a {@link TurnDialogRuntimeFact}
 * carrying only flat visible-dialog and dialog-preparation fields. The dialog operation carries only the
 * fact and no other field; every non-dialog operation carries none of the dialog fact. The pending
 * route-outcome read ({@code WHOLE_TASK_PENDING_ROUTE_OUTCOME_READ}) uses {@code pendingRouteOutcome} — a
 * {@link TurnPendingRouteOutcome} carrier (null when the slot is empty) and no other field.</p>
 */
public record TurnWholeTaskRuntimeResult(
        Boolean booleanResult,
        String enumResult,
        Long timestampMs,
        String clearedIntentId,
        TurnDialogRuntimeFact dialogRuntimeFact,
        TurnPendingTransferChoice pendingTransferChoice,
        TurnPendingRouteOutcome pendingRouteOutcome,
        TurnPathingSnapshot pathingSnapshot,
        TurnPreBattleFact preBattleFact,
        TurnCombatCleanupFact combatCleanupFact) {

    /**
     * Backward-compatible constructor for the four pre-Amendment-#12 result fields. The Amendment #12
     * {@code dialogRuntimeFact} (the {@code WHOLE_TASK_DIALOG_RUNTIME_READ} typed fact) defaults to null
     * so every existing operation result is unchanged.
     */
    public TurnWholeTaskRuntimeResult(
            Boolean booleanResult,
            String enumResult,
            Long timestampMs,
            String clearedIntentId) {
        this(booleanResult, enumResult, timestampMs, clearedIntentId, null, null, null, null, null, null);
    }

    /**
     * Backward-compatible constructor for the five pre-P-PROTO result fields. The P-PROTO
     * {@code pendingRouteOutcome} (the {@code WHOLE_TASK_PENDING_ROUTE_OUTCOME_READ} typed carrier)
     * defaults to null so every existing operation result is unchanged.
     */
    public TurnWholeTaskRuntimeResult(
            Boolean booleanResult,
            String enumResult,
            Long timestampMs,
            String clearedIntentId,
            TurnDialogRuntimeFact dialogRuntimeFact) {
        this(booleanResult, enumResult, timestampMs, clearedIntentId, dialogRuntimeFact, null, null, null, null, null);
    }

    public TurnWholeTaskRuntimeResult(
            Boolean booleanResult,
            String enumResult,
            Long timestampMs,
            String clearedIntentId,
            TurnDialogRuntimeFact dialogRuntimeFact,
            TurnPendingRouteOutcome pendingRouteOutcome) {
        this(booleanResult, enumResult, timestampMs, clearedIntentId,
                dialogRuntimeFact, null, pendingRouteOutcome, null, null, null);
    }

    public TurnWholeTaskRuntimeResult(Boolean booleanResult, String enumResult, Long timestampMs,
                                      String clearedIntentId, TurnDialogRuntimeFact dialogRuntimeFact,
                                      TurnPendingRouteOutcome pendingRouteOutcome,
                                      TurnPathingSnapshot pathingSnapshot) {
        this(booleanResult, enumResult, timestampMs, clearedIntentId, dialogRuntimeFact,
                null, pendingRouteOutcome, pathingSnapshot, null, null);
    }
}
