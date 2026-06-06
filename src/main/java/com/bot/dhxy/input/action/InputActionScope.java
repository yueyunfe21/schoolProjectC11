package com.bot.dhxy.input.action;

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
     * @return true when the current input request has been cancelled or the worker is interrupted.
     */
    public static boolean isCancelled() {
        InputActionRequest request = CURRENT.get();
        return Thread.currentThread().isInterrupted() || (request != null && request.isCancelled());
    }
}
