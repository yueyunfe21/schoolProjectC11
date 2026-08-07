package com.bot.dhxy.cloud.turn.protocol;

/**
 * Structured acknowledgement for one exact-attempt recovery reset.
 *
 * <p>The identity fields echo the requested task run, one-based round and attempt. Clear flags
 * report only mutations owned by that exact identity. When {@code combatAlreadyConfirmed} is
 * true, combat is the winning fact and every clear flag is false.</p>
 */
public record TurnExactAttemptRecoveryResetAck(
        String taskRunId,
        int round,
        String attemptId,
        boolean exactAttemptMatched,
        boolean pathingCleared,
        boolean observationLineageCleared,
        boolean scheduleCleared,
        boolean clickClaimCleared,
        boolean clickProgressCleared,
        boolean expectedCombatClaimCleared,
        boolean pendingCombatTicketCleared,
        boolean preparedDialogActionCleared,
        boolean preparedActionJobCleared,
        boolean combatAlreadyConfirmed) {
}
