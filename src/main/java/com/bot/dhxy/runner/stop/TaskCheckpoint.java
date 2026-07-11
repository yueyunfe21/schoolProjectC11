package com.bot.dhxy.runner.stop;

import com.bot.dhxy.runner.context.TaskExecutionContext;
import com.bot.dhxy.runner.context.TaskExecutionContextHolder;

/**
 * Shared cooperative stop checkpoint for task and service code.
 *
 * <p>This is the single place for translating a task stop token or thread interruption into the
 * task-level stop exception. Callers may keep local helper methods for readability, but those
 * helpers should delegate here instead of reimplementing the stop policy.</p>
 */
public final class TaskCheckpoint {

    private TaskCheckpoint() {
        throw new AssertionError("No TaskCheckpoint instances");
    }

    /**
     * Checks an explicit task context and the current thread interruption flag.
     *
     * @param context current task context; nullable for legacy/debug paths outside a runner.
     * @param interruptedMessage exception message used when the thread has already been interrupted.
     * @return milliseconds spent blocked by a user pause, or {@code 0} when no pause wait occurred.
     */
    public static long throwIfStopRequested(TaskExecutionContext context, String interruptedMessage) {
        long pauseBlockedMs = 0L;
        if (context != null) {
            pauseBlockedMs = context.throwIfStopRequested();
        }
        throwIfInterrupted(interruptedMessage);
        return pauseBlockedMs;
    }

    /**
     * Checks only the explicit stop token and current thread interruption flag.
     *
     * <p>Use this for read-only observer paths that are intentionally allowed to keep running while
     * the owning task is paused. It must not consult the pause token, otherwise a user pause would
     * hide combat enter/exit evidence that the task needs after resume.</p>
     *
     * @param stopToken task stop token; nullable for legacy/debug paths outside a runner.
     * @param interruptedMessage exception message used when the thread has already been interrupted.
     */
    public static void throwIfStopRequested(TaskStopToken stopToken, String interruptedMessage) {
        if (stopToken != null) {
            stopToken.throwIfStopRequested();
        }
        throwIfInterrupted(interruptedMessage);
    }

    /**
     * Checks the task context stored for the current window task thread and the interruption flag.
     *
     * @param holder holder that may contain the current task context.
     * @param interruptedMessage exception message used when the thread has already been interrupted.
     * @return milliseconds spent blocked by a user pause, or {@code 0} when no pause wait occurred.
     */
    public static long throwIfStopRequested(TaskExecutionContextHolder holder, String interruptedMessage) {
        long pauseBlockedMs = 0L;
        if (holder != null) {
            pauseBlockedMs = holder.checkpointIfPresent();
        }
        throwIfInterrupted(interruptedMessage);
        return pauseBlockedMs;
    }

    /**
     * Checks both an explicit task context and the thread-local holder context.
     *
     * <p>Use this in transition code where a caller may receive an explicit context while lower
     * layers also rely on {@link TaskExecutionContextHolder}. This keeps the two-context policy in
     * one utility method instead of creating local wrapper methods in each task class.</p>
     *
     * @param context current task context; nullable for legacy/debug paths.
     * @param holder holder that may contain the current task context.
     * @param interruptedMessage exception message used when the thread has already been interrupted.
     * @return total milliseconds spent blocked by user pause checkpoints, or {@code 0} when no pause
     *         wait occurred. Existing callers may ignore this value; timeout-owning callers can use
     *         it to exclude user pause time from business watchdogs.
     */
    public static long throwIfStopRequested(TaskExecutionContext context,
                                            TaskExecutionContextHolder holder,
                                            String interruptedMessage) {
        long pauseBlockedMs = 0L;
        if (context != null) {
            pauseBlockedMs += context.throwIfStopRequested();
        }
        if (holder != null) {
            pauseBlockedMs += holder.checkpointIfPresent();
        }
        throwIfInterrupted(interruptedMessage);
        return pauseBlockedMs;
    }

    /**
     * Converts the current thread interruption flag into the normal task stop exception.
     *
     * @param message exception message used when the current thread is interrupted.
     */
    public static void throwIfInterrupted(String message) {
        if (Thread.currentThread().isInterrupted()) {
            throw new TaskStopRequestedException(message);
        }
    }
}
