package com.bot.dhxy.cloud.turn.protocol;

import java.util.List;

/**
 * Arguments for the closed {@code WHOLE_TASK_*} local
 * runtime operations (TURN-35 Amendment #6 shared whole-task local-fact foundation).
 *
 * <p>{@code source} is required for every operation. Every other field is owned by exactly one
 * operation and must be null for all others; {@link TurnProtocolValidator} enforces the
 * exactly-one payload shape per operation. Field meanings mirror the baseline
 * {@code WindowRuntimeContext}/{@code GameStateUtil} method parameters one-to-one — no new
 * business inputs are introduced.</p>
 */
public record TurnWholeTaskRuntimeArguments(
        String source,
        TurnPathingIntent pathingIntent,
        String intentId,
        String sourcePrefix,
        Long protectionMs,
        String currentMapName,
        Integer currentX,
        Integer currentY,
        String targetMapName,
        Integer targetX,
        Integer targetY,
        Integer tolerance,
        Long confirmTimeoutMs,
        String taskCode,
        String targetKeyword,
        Long blockedMs,
        List<String> interestOperations,
        Long absentAllowedAtMs,
        Boolean probeOnly,
        Integer completedRuns,
        Integer totalRuns,
        Long dialogSnapshotMaxAgeMs,
        TurnPendingTransferChoice transferChoice,
        TurnPendingRouteOutcome routeOutcome,
        String routeOutcomeReplacementReason,
        String startupFlyingState,
        Long probeStartAtMs,
        String scheduleAttemptId,
        Integer scheduleRound,
        String scheduleTaskRunId,
        Long scheduleOpenedAtMs,
        String scheduleObservationRunId,
        String replayObservationRunId,
        String replayBusinessTaskRunId,
        String expectedCombatClaimId,
        String expectedCombatObservationRunId,
        String expectedCombatBusinessTaskRunId,
        String expectedCombatAttemptId,
        TurnNpcArrivalFrameFifoSpec npcArrivalFifo,
        String recoveryTaskRunId,
        Integer recoveryRound,
        String recoveryAttemptId,
        TurnDialogFollowUpClick dialogFollowUpClick) {

    /** Backward-compatible full constructor before exact recovery identity was added. */
    public TurnWholeTaskRuntimeArguments(
            String source,
            TurnPathingIntent pathingIntent,
            String intentId,
            String sourcePrefix,
            Long protectionMs,
            String currentMapName,
            Integer currentX,
            Integer currentY,
            String targetMapName,
            Integer targetX,
            Integer targetY,
            Integer tolerance,
            Long confirmTimeoutMs,
            String taskCode,
            String targetKeyword,
            Long blockedMs,
            List<String> interestOperations,
            Long absentAllowedAtMs,
            Boolean probeOnly,
            Integer completedRuns,
            Integer totalRuns,
            Long dialogSnapshotMaxAgeMs,
            TurnPendingTransferChoice transferChoice,
            TurnPendingRouteOutcome routeOutcome,
            String routeOutcomeReplacementReason,
            String startupFlyingState,
            Long probeStartAtMs,
            String scheduleAttemptId,
            Integer scheduleRound,
            String scheduleTaskRunId,
            Long scheduleOpenedAtMs,
            String scheduleObservationRunId,
            String replayObservationRunId,
            String replayBusinessTaskRunId,
            String expectedCombatClaimId,
            String expectedCombatObservationRunId,
            String expectedCombatBusinessTaskRunId,
            String expectedCombatAttemptId,
            TurnNpcArrivalFrameFifoSpec npcArrivalFifo) {
        this(source, pathingIntent, intentId, sourcePrefix, protectionMs, currentMapName, currentX, currentY,
                targetMapName, targetX, targetY, tolerance, confirmTimeoutMs, taskCode, targetKeyword, blockedMs,
                interestOperations, absentAllowedAtMs, probeOnly, completedRuns, totalRuns, dialogSnapshotMaxAgeMs,
                transferChoice, routeOutcome, routeOutcomeReplacementReason, startupFlyingState,
                probeStartAtMs, scheduleAttemptId, scheduleRound, scheduleTaskRunId, scheduleOpenedAtMs,
                scheduleObservationRunId, replayObservationRunId, replayBusinessTaskRunId,
                expectedCombatClaimId, expectedCombatObservationRunId, expectedCombatBusinessTaskRunId,
                expectedCombatAttemptId, npcArrivalFifo, null, null, null, null);
    }

    /**
     * Backward-compatible constructor for the 21 pre-Amendment-#12 fields. The Amendment #12
     * {@code dialogSnapshotMaxAgeMs} (optional visible-dialog freshness bound for
     * {@code WHOLE_TASK_DIALOG_RUNTIME_READ}) defaults to null so every existing operation payload is
     * unchanged.
     */
    public TurnWholeTaskRuntimeArguments(
            String source,
            TurnPathingIntent pathingIntent,
            String intentId,
            String sourcePrefix,
            Long protectionMs,
            String currentMapName,
            Integer currentX,
            Integer currentY,
            String targetMapName,
            Integer targetX,
            Integer targetY,
            Integer tolerance,
            Long confirmTimeoutMs,
            String taskCode,
            String targetKeyword,
            Long blockedMs,
            List<String> interestOperations,
            Long absentAllowedAtMs,
            Boolean probeOnly,
            Integer completedRuns,
            Integer totalRuns) {
        this(source, pathingIntent, intentId, sourcePrefix, protectionMs, currentMapName, currentX, currentY,
                targetMapName, targetX, targetY, tolerance, confirmTimeoutMs, taskCode, targetKeyword, blockedMs,
                interestOperations, absentAllowedAtMs, probeOnly, completedRuns, totalRuns, null);
    }

    /**
     * Backward-compatible constructor for the 22-field pre-P-PROTO payload set (through Amendment #12).
     * The P-PROTO pending transfer-choice / route-outcome carriers and the separate route-outcome
     * replacement reason default to null so every existing operation payload is unchanged.
     */
    public TurnWholeTaskRuntimeArguments(
            String source,
            TurnPathingIntent pathingIntent,
            String intentId,
            String sourcePrefix,
            Long protectionMs,
            String currentMapName,
            Integer currentX,
            Integer currentY,
            String targetMapName,
            Integer targetX,
            Integer targetY,
            Integer tolerance,
            Long confirmTimeoutMs,
            String taskCode,
            String targetKeyword,
            Long blockedMs,
            List<String> interestOperations,
            Long absentAllowedAtMs,
            Boolean probeOnly,
            Integer completedRuns,
            Integer totalRuns,
            Long dialogSnapshotMaxAgeMs) {
        this(source, pathingIntent, intentId, sourcePrefix, protectionMs, currentMapName, currentX, currentY,
                targetMapName, targetX, targetY, tolerance, confirmTimeoutMs, taskCode, targetKeyword, blockedMs,
                interestOperations, absentAllowedAtMs, probeOnly, completedRuns, totalRuns, dialogSnapshotMaxAgeMs,
                null, null, null, null);
    }

    public TurnWholeTaskRuntimeArguments(
            String source,
            TurnPathingIntent pathingIntent,
            String intentId,
            String sourcePrefix,
            Long protectionMs,
            String currentMapName,
            Integer currentX,
            Integer currentY,
            String targetMapName,
            Integer targetX,
            Integer targetY,
            Integer tolerance,
            Long confirmTimeoutMs,
            String taskCode,
            String targetKeyword,
            Long blockedMs,
            List<String> interestOperations,
            Long absentAllowedAtMs,
            Boolean probeOnly,
            Integer completedRuns,
            Integer totalRuns,
            Long dialogSnapshotMaxAgeMs,
            TurnPendingTransferChoice transferChoice,
            TurnPendingRouteOutcome routeOutcome,
            String routeOutcomeReplacementReason) {
        this(source, pathingIntent, intentId, sourcePrefix, protectionMs, currentMapName, currentX, currentY,
                targetMapName, targetX, targetY, tolerance, confirmTimeoutMs, taskCode, targetKeyword, blockedMs,
                interestOperations, absentAllowedAtMs, probeOnly, completedRuns, totalRuns, dialogSnapshotMaxAgeMs,
                transferChoice, routeOutcome, routeOutcomeReplacementReason, null);
    }

    /**
     * TURN-40G repair (baseline restoration): backward-compatible constructor for the 26-field pre-40G payload
     * set. The xiuluo shortcut probe anchor ({@code probeStartAtMs}) and the atomic green-chain schedule identity
     * ({@code scheduleAttemptId/scheduleRound/scheduleTaskRunId/scheduleOpenedAtMs}) ride only the
     * {@code WHOLE_TASK_DIALOG_INTEREST_UPDATE} operation and default to null everywhere else.
     */
    public TurnWholeTaskRuntimeArguments(
            String source,
            TurnPathingIntent pathingIntent,
            String intentId,
            String sourcePrefix,
            Long protectionMs,
            String currentMapName,
            Integer currentX,
            Integer currentY,
            String targetMapName,
            Integer targetX,
            Integer targetY,
            Integer tolerance,
            Long confirmTimeoutMs,
            String taskCode,
            String targetKeyword,
            Long blockedMs,
            List<String> interestOperations,
            Long absentAllowedAtMs,
            Boolean probeOnly,
            Integer completedRuns,
            Integer totalRuns,
            Long dialogSnapshotMaxAgeMs,
            TurnPendingTransferChoice transferChoice,
            TurnPendingRouteOutcome routeOutcome,
            String routeOutcomeReplacementReason,
            String startupFlyingState) {
        this(source, pathingIntent, intentId, sourcePrefix, protectionMs, currentMapName, currentX, currentY,
                targetMapName, targetX, targetY, tolerance, confirmTimeoutMs, taskCode, targetKeyword, blockedMs,
                interestOperations, absentAllowedAtMs, probeOnly, completedRuns, totalRuns, dialogSnapshotMaxAgeMs,
                transferChoice, routeOutcome, routeOutcomeReplacementReason, startupFlyingState,
                null, null, null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null);
    }

    public TurnWholeTaskRuntimeArguments(
            String source, TurnPathingIntent pathingIntent, String intentId, String sourcePrefix,
            Long protectionMs, String currentMapName, Integer currentX, Integer currentY,
            String targetMapName, Integer targetX, Integer targetY, Integer tolerance,
            Long confirmTimeoutMs, String taskCode, String targetKeyword, Long blockedMs,
            List<String> interestOperations, Long absentAllowedAtMs, Boolean probeOnly,
            Integer completedRuns, Integer totalRuns, Long dialogSnapshotMaxAgeMs,
            TurnPendingTransferChoice transferChoice, TurnPendingRouteOutcome routeOutcome,
            String routeOutcomeReplacementReason, String startupFlyingState, Long probeStartAtMs,
            String scheduleAttemptId, Integer scheduleRound, String scheduleTaskRunId,
            Long scheduleOpenedAtMs) {
        this(source, pathingIntent, intentId, sourcePrefix, protectionMs, currentMapName, currentX, currentY,
                targetMapName, targetX, targetY, tolerance, confirmTimeoutMs, taskCode, targetKeyword, blockedMs,
                interestOperations, absentAllowedAtMs, probeOnly, completedRuns, totalRuns, dialogSnapshotMaxAgeMs,
                transferChoice, routeOutcome, routeOutcomeReplacementReason, startupFlyingState,
                probeStartAtMs, scheduleAttemptId, scheduleRound, scheduleTaskRunId, scheduleOpenedAtMs,
                null, null, null, null, null, null, null,
                null, null, null, null, null);
    }
}
