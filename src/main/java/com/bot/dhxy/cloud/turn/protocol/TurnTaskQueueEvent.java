package com.bot.dhxy.cloud.turn.protocol;

/**
 * One immutable, diagnostic-only lifecycle event for a single queue element.
 *
 * <p>The Cloud retains these events for the exact start request and attaches a snapshot to normal turn responses.
 * They never acknowledge an input action, never wake a runner, and never alter queue policy. Their only purpose is
 * to make a child task failure and the following queue decision visible on the client that owns the game window.</p>
 */
public record TurnTaskQueueEvent(
        String eventId,
        String startRequestId,
        String taskRunId,
        String taskCode,
        String taskName,
        int queueIndex,
        Type type,
        String result,
        String reason,
        String exceptionType,
        String phase,
        Integer round,
        long elapsedMs) {

    public enum Type {
        TASK_STARTED,
        TASK_TERMINAL,
        QUEUE_DECISION
    }
}
