package com.bot.dhxy.runner.context;

import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * Holds the current task execution context for the window task thread.
 *
 * <p>This lets lower-level services add cooperative pause/stop checkpoints
 * without widening every business method signature.</p>
 */
@Component
public class TaskExecutionContextHolder {

    private final ThreadLocal<TaskExecutionContext> current = new ThreadLocal<>();

    public <T> T callWith(TaskExecutionContext context, Supplier<T> action) {
        TaskExecutionContext previous = current.get();
        current.set(context);
        try {
            return action.get();
        } finally {
            if (previous == null) {
                current.remove();
            } else {
                current.set(previous);
            }
        }
    }

    public Optional<TaskExecutionContext> current() {
        return Optional.ofNullable(current.get());
    }

    /**
     * Applies a checkpoint to the context bound to the current task thread, if any.
     *
     * @return milliseconds spent blocked by a user pause, or {@code 0} when no current context
     *         exists or no pause wait occurred.
     */
    public long checkpointIfPresent() {
        TaskExecutionContext context = current.get();
        if (context != null) {
            return context.throwIfStopRequested();
        }
        return 0L;
    }

    public boolean isPauseRequested() {
        TaskExecutionContext context = current.get();
        return context != null && context.isPauseRequested();
    }
}
