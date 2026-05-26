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
    private volatile String reason;

    public void requestPause(String reason) {
        synchronized (monitor) {
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

    public void waitIfPaused(TaskStopToken stopToken) {
        if (!pauseRequested) {
            return;
        }
        log.info("task pause checkpoint reached: reason={}", reason);
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
    }

    private String normalize(String value) {
        if (value == null) {
            return "pause requested";
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? "pause requested" : trimmed;
    }
}
