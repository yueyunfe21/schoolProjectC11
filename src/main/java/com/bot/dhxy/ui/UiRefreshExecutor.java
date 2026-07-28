package com.bot.dhxy.ui;

import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Runs JavaFX dashboard snapshot work on one background thread with single-flight admission.
 */
final class UiRefreshExecutor {

    private final AtomicBoolean inFlight = new AtomicBoolean();
    private final AtomicBoolean shutdown = new AtomicBoolean();
    private final ExecutorService executor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "main-window-snapshot-refresh");
        thread.setDaemon(true);
        return thread;
    });

    boolean submit(Runnable refresh) {
        Objects.requireNonNull(refresh, "refresh");
        if (shutdown.get() || !inFlight.compareAndSet(false, true)) {
            return false;
        }
        try {
            executor.execute(() -> {
                try {
                    refresh.run();
                } finally {
                    inFlight.set(false);
                }
            });
            return true;
        } catch (RejectedExecutionException rejected) {
            inFlight.set(false);
            return false;
        }
    }

    void shutdown() {
        if (shutdown.compareAndSet(false, true)) {
            executor.shutdownNow();
        }
    }

    boolean isShutdown() {
        return shutdown.get();
    }
}
