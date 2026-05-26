package com.bot.dhxy.task.transaction;

/**
 * Caller preference for task-turn ownership after a transaction result.
 *
 * <p>Final yield behavior also depends on {@link TaskTransactionResult}. For example STOPPED,
 * FAILED, PATHING_STARTED, and TASK_FINISHED always release the turn even when a caller accidentally
 * passes {@link #CONTINUE_CHAIN}.</p>
 */
public enum TaskYieldPolicy {
    /** Release the task turn after the transaction. */
    MUST_YIELD,
    /** Release when the transaction result indicates it is useful or safe to do so. */
    MAY_YIELD,
    /** Keep the task turn for the same window's next tightly-coupled step. */
    CONTINUE_CHAIN,
    /** Release now and let the caller retry from a later loop/tick. */
    RETRY_LATER,
    /** Release and stop the current transaction chain. */
    STOP_CHAIN
}
