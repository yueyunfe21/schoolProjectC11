package com.bot.dhxy.window.execution;

/**
 * Controls whether a multi-task window queue continues after one task fails.
 */
public enum WindowTaskFailurePolicy {
    /** Continue to the next task even when the current task returns FAILED. */
    CONTINUE_ON_FAILURE,
    /** Stop the remaining queue as soon as one task returns FAILED. */
    STOP_ON_FAILURE
}
