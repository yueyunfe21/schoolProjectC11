package com.bot.dhxy.runner.stop;

import java.time.LocalDateTime;
import java.util.function.BooleanSupplier;
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
    private long pauseRevision;
    private long requestedAtNanos;
    private long cumulativeCompletedPauseNanos;
    private volatile String reason;

    public void requestPause(String reason) {
        synchronized (monitor) {
            long now = System.currentTimeMillis();
            if (!pauseRequested) {
                requestedAtMs = now;
                requestedAtNanos = System.nanoTime();
                pauseRevision = saturatingIncrement(pauseRevision);
            }
            pauseRequested = true;
            requestedAt = LocalDateTime.now();
            this.reason = normalize(reason);
        }
    }

    public void resume() {
        synchronized (monitor) {
            if (pauseRequested) {
                long blockedNanos = elapsedNanos(requestedAtNanos, System.nanoTime());
                cumulativeCompletedPauseNanos = saturatingAdd(
                        cumulativeCompletedPauseNanos, blockedNanos);
            }
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

    /**
     * Pause-accounting wait that remains blocked across pause revision changes.
     *
     * <p>The wait exits only after resume, a stop-token exception, or an affirmative wake
     * condition so the caller can run its existing stop/identity safety gate. Pause revisions are
     * retained only for snapshots and diagnostics; the wake condition does not resume or mutate
     * this token.</p>
     */
    public PauseWaitSnapshot waitIfPausedRevision(
            TaskStopToken stopToken,
            BooleanSupplier wakeCondition) {
        synchronized (monitor) {
            if (!pauseRequested) {
                return pauseProgressLocked(System.nanoTime());
            }
            long observedRevision = pauseRevision;
            log.info("task pause checkpoint reached: reason={} revision={}", reason, observedRevision);
            while (pauseRequested) {
                if (stopToken != null) {
                    stopToken.throwIfStopRequested();
                }
                if (wakeCondition != null && wakeCondition.getAsBoolean()) {
                    break;
                }
                try {
                    monitor.wait(250L);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new TaskStopRequestedException("task pause wait interrupted");
                }
            }
            if (stopToken != null) {
                stopToken.throwIfStopRequested();
            }
            PauseWaitSnapshot snapshot = pauseProgressLocked(System.nanoTime());
            log.info("task pause checkpoint released: revision={} cumulativePauseNanos={} stillPaused={}",
                    observedRevision, snapshot.cumulativePauseNanos(), snapshot.pauseRequested());
            return snapshot;
        }
    }

    /** Returns bounded cumulative monotonic pause progress without waiting. */
    public PauseWaitSnapshot pauseProgress() {
        synchronized (monitor) {
            return pauseProgressLocked(System.nanoTime());
        }
    }

    public record PauseWaitSnapshot(
            long revision,
            long cumulativePauseNanos,
            boolean pauseRequested) {
    }

    private PauseWaitSnapshot pauseProgressLocked(long nowNanos) {
        long cumulative = cumulativeCompletedPauseNanos;
        if (pauseRequested) {
            cumulative = saturatingAdd(cumulative, elapsedNanos(requestedAtNanos, nowNanos));
        }
        return new PauseWaitSnapshot(pauseRevision, cumulative, pauseRequested);
    }

    private static long elapsedNanos(long startNanos, long endNanos) {
        long elapsed = endNanos - startNanos;
        return elapsed < 0L ? Long.MAX_VALUE : elapsed;
    }

    private static long saturatingIncrement(long value) {
        return value == Long.MAX_VALUE ? Long.MAX_VALUE : value + 1L;
    }

    private static long saturatingAdd(long left, long right) {
        if (right <= 0L) {
            return left;
        }
        return left > Long.MAX_VALUE - right ? Long.MAX_VALUE : left + right;
    }

    private String normalize(String value) {
        if (value == null) {
            return "pause requested";
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? "pause requested" : trimmed;
    }
}
