package com.bot.dhxy.window.execution;

/**
 * Result status for attempting to submit a task or task queue to one window.
 */
public enum WindowTaskSubmitStatus {
    /** Submission was accepted by the window runner. */
    ACCEPTED,
    /** The provided window id was blank or null. */
    INVALID_WINDOW_ID,
    /** The selected task queue was empty or otherwise invalid. */
    INVALID_QUEUE,
    /** The window has not been registered in the manager. */
    WINDOW_NOT_REGISTERED,
    /** The runner has been shut down. */
    RUNNER_CLOSED,
    /** The stored hwnd/native binding is stale and must be rescanned. */
    STALE_NATIVE_BINDING,
    /** The window runner already has a live task. */
    WINDOW_BUSY,
    /** The runner rejected the submit request after validation. */
    SUBMIT_REJECTED
}
