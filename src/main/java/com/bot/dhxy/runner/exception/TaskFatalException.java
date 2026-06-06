package com.bot.dhxy.runner.exception;

/**
 * Fatal task failure that should stop the current window task and surface to the UI.
 *
 * <p>Use this only when normal retry/recovery has been exhausted and continuing the same task would
 * likely repeat the same bad state. Recoverable game-state misses should still return structured
 * task results instead of throwing.</p>
 */
public class TaskFatalException extends RuntimeException {

    public TaskFatalException(String message) {
        super(message);
    }

    public TaskFatalException(String message, Throwable cause) {
        super(message, cause);
    }
}
