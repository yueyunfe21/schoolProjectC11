package com.bot.dhxy.window.observation;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/** One-shot cancellation owned by one exact observation HTTP exchange. */
public final class ObservationSendCancellation {

    private final AtomicBoolean cancelled = new AtomicBoolean();
    private final AtomicReference<Runnable> cancelAction = new AtomicReference<>();

    public boolean isCancelled() {
        return cancelled.get();
    }

    /** Registers the currently cancellable transport stage and runs it immediately after an earlier cancel. */
    public void register(Runnable action) {
        Objects.requireNonNull(action, "action");
        if (!cancelAction.compareAndSet(null, action)) {
            throw new IllegalStateException("observation cancellation action is already registered");
        }
        if (cancelled.get() && cancelAction.compareAndSet(action, null)) {
            action.run();
        }
    }

    public void clear(Runnable action) {
        cancelAction.compareAndSet(action, null);
    }

    /** Cancels at most one registered transport stage without interrupting the runner thread. */
    public void cancel() {
        cancelled.set(true);
        Runnable action = cancelAction.getAndSet(null);
        if (action != null) {
            action.run();
        }
    }
}
