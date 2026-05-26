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

    public void checkpointIfPresent() {
        TaskExecutionContext context = current.get();
        if (context != null) {
            context.throwIfStopRequested();
        }
    }

    public boolean isPauseRequested() {
        TaskExecutionContext context = current.get();
        return context != null && context.isPauseRequested();
    }
}
