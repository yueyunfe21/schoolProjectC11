package com.bot.dhxy.input.action;

import com.bot.dhxy.runner.stop.TaskStopRequestedException;

import java.util.function.Supplier;

/**
 * Thread-local view of the input request currently running on the single input worker.
 *
 * <p>Exclusive callbacks use direct input provider calls, so the worker cannot inspect each internal
 * step the way it can inspect {@link InputAction} lists. This scope lets long callbacks cheaply stop
 * between direct-input steps when the waiting task was interrupted or cancelled.</p>
 */
public final class InputActionScope {

    private static final ThreadLocal<InputActionRequest> CURRENT = new ThreadLocal<>();

    private InputActionScope() {
    }

    /**
     * Run a callback with the given input request visible to direct-input helper code.
     *
     * @param request request currently executed by the input worker.
     * @param callback callback to run.
     * @return callback result.
     */
    public static Boolean callWith(InputActionRequest request, Supplier<Boolean> callback) {
        InputActionRequest previous = CURRENT.get();
        CURRENT.set(request);
        try {
            return callback.get();
        } finally {
            if (previous == null) {
                CURRENT.remove();
            } else {
                CURRENT.set(previous);
            }
        }
    }

    /**
     * Cooperative checkpoint for exclusive direct-input callbacks.
     *
     * <p>If the current request is paused, this method blocks until resume and returns true. Stop,
     * cancellation, or worker interruption returns false or throws the normal stop exception so the
     * owning callback can exit without converting user pause into a business failure.</p>
     *
     * @return true when the callback may continue sending direct input.
     */
    public static boolean checkpoint() {
        InputActionRequest request = CURRENT.get();
        if (Thread.currentThread().isInterrupted()) {
            return false;
        }
        if (request == null) {
            return true;
        }
        if (!request.isPlayerIdentityEpochCurrent()) {
            return false;
        }
        if (request.isExecutionStarted()) {
            return true;
        }
        if (request.isCancelled()) {
            return false;
        }
        if (request.isPauseRequested()) {
            try {
                request.getPauseToken().waitIfPaused(request.getStopToken());
            } catch (TaskStopRequestedException e) {
                request.cancel("task-stop:" + e.getMessage());
                throw e;
            }
        }
        if (Thread.currentThread().isInterrupted() || request.isCancelled()) {
            return false;
        }
        return request.checkDetailedSafety("exclusive-callback");
    }

    /**
     * Backward-compatible cancellation-style view used by older callbacks.
     *
     * @return true only when the current request should stop; pause waits and then returns false.
     */
    public static boolean isCancelled() {
        return !checkpoint();
    }

    /**
     * @return true only inside an admitted input-worker transaction with the same player identity.
     * G008 uses this narrow state to finish one atomic physical sequence after the old run is stopped.
     */
    public static boolean isActiveAtomicTransaction() {
        InputActionRequest request = CURRENT.get();
        return request != null
                && request.isExecutionStarted()
                && request.isPlayerIdentityEpochCurrent()
                && !Thread.currentThread().isInterrupted();
    }
}
