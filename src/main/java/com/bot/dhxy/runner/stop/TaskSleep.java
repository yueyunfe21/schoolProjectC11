package com.bot.dhxy.runner.stop;

import com.bot.dhxy.runner.context.TaskExecutionContext;

/**
 * Shared sleep helpers for task and service code.
 *
 * <p>Task code must preserve the interrupted flag and stop promptly when the window runner
 * requests a stop. Keeping this behavior in one place avoids every service inventing a slightly
 * different interrupt policy.</p>
 */
public final class TaskSleep {

    private TaskSleep() {
        throw new AssertionError("No TaskSleep instances");
    }

    /**
     * Sleeps once and converts interruption into a boolean result.
     *
     * @param millis wait time in milliseconds; non-positive values return immediately.
     * @return true when the full wait completed; false when the current thread was interrupted.
     */
    public static boolean sleep(long millis) {
        if (millis <= 0) {
            return true;
        }
        try {
            Thread.sleep(millis);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    /**
     * Sleeps for task workflow code and checks the task stop/pause token before and after waiting.
     *
     * @param context current task context; may be null for legacy single-window paths.
     * @param millis wait time in milliseconds.
     * @param interruptedMessage message used when the wait is interrupted.
     */
    public static void sleepOrStop(TaskExecutionContext context, long millis, String interruptedMessage) {
        if (millis <= 0) {
            return;
        }
        throwIfStopRequested(context);
        if (!sleep(millis)) {
            throw new TaskStopRequestedException(interruptedMessage);
        }
        throwIfStopRequested(context);
    }

    /**
     * Checks the current thread and optional task context for stop requests.
     *
     * @param context current task context; may be null for helper code outside a task runner.
     * @param interruptedMessage message used when the current thread is already interrupted.
     */
    public static void throwIfStopRequested(TaskExecutionContext context, String interruptedMessage) {
        throwIfStopRequested(context);
        if (Thread.currentThread().isInterrupted()) {
            throw new TaskStopRequestedException(interruptedMessage);
        }
    }

    private static void throwIfStopRequested(TaskExecutionContext context) {
        if (context != null) {
            context.throwIfStopRequested();
        }
    }
}
