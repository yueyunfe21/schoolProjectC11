package com.bot.dhxy.task.transaction;

/**
 * Business result returned by one task transaction.
 *
 * <p>The value is consumed by the turn coordination boundary to decide whether the current window keeps
 * the leader/task turn or releases it for other windows. These states describe task progress, not
 * low-level input success.</p>
 */
public enum TaskTransactionResult {
    /** Transaction finished a step and the same window may continue the chain. */
    READY_TO_CONTINUE,
    /** Navigation/pathing has started, so the current window can yield while it moves. */
    PATHING_STARTED,
    /** A shared team state such as combat was triggered and other windows should be allowed to react. */
    SHARED_STATE_TRIGGERED,
    /** The task or queue finished normally. */
    TASK_FINISHED,
    /** The step failed in a way that can be retried later after releasing the turn. */
    RETRYABLE_ERROR,
    /** The step failed and should release the turn. */
    FAILED,
    /** Stop was requested or the current thread was interrupted. */
    STOPPED
}
