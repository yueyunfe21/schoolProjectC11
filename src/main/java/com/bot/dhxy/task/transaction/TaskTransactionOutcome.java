package com.bot.dhxy.task.transaction;

/**
 * Immutable result of one task transaction.
 *
 * @param name diagnostic transaction name used in logs.
 * @param expectedResult business result the caller considered successful.
 * @param yieldPolicy whether the window should keep or release the task turn after success.
 * @param result actual transaction result. Null is not expected; runners normalize null to FAILED.
 * @param completed false when the transaction did not finish, usually because exclusive input could
 *                  not complete or the waiting thread was interrupted.
 */
public record TaskTransactionOutcome(
        String name,
        TaskTransactionResult expectedResult,
        TaskYieldPolicy yieldPolicy,
        TaskTransactionResult result,
        boolean completed
) {
    /**
     * @return true only when the action completed and returned the caller's expected result.
     */
    public boolean reachedExpectedResult() {
        return completed && result == expectedResult;
    }
}
