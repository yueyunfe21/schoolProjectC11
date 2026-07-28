package com.bot.dhxy.window.observation;

import com.bot.dhxy.core.GameContext;
import com.bot.dhxy.window.runtime.WindowRuntimeContext;
import com.bot.dhxy.window.runtime.WindowTaskContextHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/** Per-window local-only patrol for passive maintenance-broadcast prompts. */
final class LocalMaintenanceBroadcastRunner {

    private static final Logger log = LoggerFactory.getLogger(LocalMaintenanceBroadcastRunner.class);
    static final long DEFAULT_PERIOD_MS = 3_000L;
    static final long SUCCESS_DEDUP_MS = 1_500L;

    private final Object lifecycleMonitor = new Object();
    private final WindowRuntimeContext context;
    private final WindowTaskContextHolder contextHolder;
    private final LocalMaintenanceBroadcastHandler handler;
    private final long periodMs;
    private final AtomicBoolean running = new AtomicBoolean();
    private final AtomicBoolean stopRequested = new AtomicBoolean();
    private volatile Thread workerThread;
    private long lastSuccessfulClickAtMs;

    LocalMaintenanceBroadcastRunner(WindowRuntimeContext context,
                                    WindowTaskContextHolder contextHolder,
                                    LocalMaintenanceBroadcastHandler handler) {
        this(context, contextHolder, handler, DEFAULT_PERIOD_MS);
    }

    LocalMaintenanceBroadcastRunner(WindowRuntimeContext context,
                                    WindowTaskContextHolder contextHolder,
                                    LocalMaintenanceBroadcastHandler handler,
                                    long periodMs) {
        this.context = Objects.requireNonNull(context, "context");
        this.contextHolder = Objects.requireNonNull(contextHolder, "contextHolder");
        this.handler = Objects.requireNonNull(handler, "handler");
        if (periodMs <= 0L) {
            throw new IllegalArgumentException("periodMs must be positive");
        }
        this.periodMs = periodMs;
    }

    void start() {
        synchronized (lifecycleMonitor) {
            if (!running.compareAndSet(false, true)) {
                throw new IllegalStateException("local maintenance runner already started: " + context.getWindowId());
            }
            stopRequested.set(false);
            Thread thread = new Thread(this::runLoop, "dhxy-local-maintenance-" + context.getWindowId());
            thread.setDaemon(true);
            workerThread = thread;
            thread.start();
        }
    }

    void requestStop() {
        stopRequested.set(true);
        Thread thread = workerThread;
        if (thread != null) {
            thread.interrupt();
        }
    }

    boolean awaitStopped(Duration timeout) throws InterruptedException {
        long deadlineNanos = System.nanoTime() + timeout.toNanos();
        synchronized (lifecycleMonitor) {
            while (running.get()) {
                long remainingNanos = deadlineNanos - System.nanoTime();
                if (remainingNanos <= 0L) {
                    return false;
                }
                lifecycleMonitor.wait(Math.max(1L, remainingNanos / 1_000_000L));
            }
            return true;
        }
    }

    private void runLoop() {
        try {
            while (!stopRequested.get() && !Thread.currentThread().isInterrupted()) {
                try {
                    contextHolder.runWith(context, () -> {
                        long now = System.currentTimeMillis();
                        if (context.getGameState().getCurrentActionState() == GameContext.ActionState.FREE
                                && now - lastSuccessfulClickAtMs >= SUCCESS_DEDUP_MS
                                && handler.handleIfPresent()) {
                            lastSuccessfulClickAtMs = System.currentTimeMillis();
                        }
                    });
                } catch (RuntimeException failure) {
                    if (!stopRequested.get()) {
                        log.warn("Local maintenance patrol tick failed: windowId={} message={}",
                                context.getWindowId(), failure.getMessage());
                    }
                }
                Thread.sleep(periodMs);
            }
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        } finally {
            synchronized (lifecycleMonitor) {
                workerThread = null;
                running.set(false);
                lifecycleMonitor.notifyAll();
            }
        }
    }
}
