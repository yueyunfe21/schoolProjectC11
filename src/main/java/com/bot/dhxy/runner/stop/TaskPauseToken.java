package com.bot.dhxy.runner.stop;

import java.time.LocalDateTime;
import lombok.extern.slf4j.Slf4j;

/**
 * Per-task pause signal.
 *
 * <p>Pause is cooperative: the task waits only when it reaches a checkpoint.
 * It does not suspend the global input worker or interrupt an input sequence
 * that is already executing.</p>
 */
@Slf4j
public class TaskPauseToken {

    private final Object monitor = new Object();
    private volatile boolean pauseRequested;
    private volatile LocalDateTime requestedAt;
    private volatile long requestedAtMs;
    private volatile String reason;

    public void requestPause(String reason) {
        synchronized (monitor) {
            long now = System.currentTimeMillis();
            if (!pauseRequested) {
                requestedAtMs = now;
            }
            pauseRequested = true;
            requestedAt = LocalDateTime.now();
            this.reason = normalize(reason);
        }
    }

    public void resume() {
        synchronized (monitor) {
            pauseRequested = false;
            monitor.notifyAll();
        }
    }

    public boolean isPauseRequested() {
        return pauseRequested;
    }

    public LocalDateTime getRequestedAt() {
        return requestedAt;
    }

    public String getReason() {
        return reason;
    }

    /**
     * Blocks while the task is paused and returns the wall-clock time spent waiting.
     *
     * @param stopToken optional stop token checked before and during the pause wait; nullable for
     *                  legacy/debug callers outside a managed task.
     * @return milliseconds spent blocked by a user pause, or {@code 0} when no pause was active.
     */
    public long waitIfPaused(TaskStopToken stopToken) {
        if (!pauseRequested) {
            return 0L;
        }
        log.info("task pause checkpoint reached: reason={}", reason);
        long now = System.currentTimeMillis();
        long requestedMs = requestedAtMs;
        long blockedStartMs = requestedMs > 0L && requestedMs <= now ? requestedMs : now;
        synchronized (monitor) {
            while (pauseRequested) {
                if (stopToken != null) {
                    stopToken.throwIfStopRequested();
                }
                try {
                    monitor.wait(250L);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new TaskStopRequestedException("task pause wait interrupted");
                }
            }
        }
        log.info("task pause checkpoint resumed");
        if (stopToken != null) {
            stopToken.throwIfStopRequested();
        }
        return Math.max(0L, System.currentTimeMillis() - blockedStartMs);
    }

    private String normalize(String value) {
        if (value == null) {
            return "pause requested";
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? "pause requested" : trimmed;
    }
}
