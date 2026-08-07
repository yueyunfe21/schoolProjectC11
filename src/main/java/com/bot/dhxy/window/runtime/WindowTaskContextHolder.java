package com.bot.dhxy.window.runtime;

import com.bot.dhxy.config.WindowIsolationProperties;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * 当前任务线程绑定的窗口上下文。
 *
 * 默认情况下隐藏绑定上下文，保持旧单窗口行为。
 * 只有 bot.window.isolation-enabled=true 时，底层输入/截图层才会读取当前窗口上下文。
 */
/**
 * Thread-local holder for the window currently executing a task.
 *
 * <p>The raw binding is always available to code that explicitly needs the current window identity.
 * {@link #current()} respects {@code bot.window.isolation-enabled} so legacy single-window behavior
 * can still ignore window isolation. Input, screenshot, OCR, and temp-path code should prefer the
 * raw/current binding instead of searching for the first matching game window by title.</p>
 */
@Component
public class WindowTaskContextHolder {

    private final WindowIsolationProperties windowIsolationProperties;
    private final ThreadLocal<WindowRuntimeContext> currentContext = new ThreadLocal<>();

    /**
     * @param windowIsolationProperties feature switch controlling whether {@link #current()} exposes
     *                                  the thread-local binding to legacy-aware callers.
     */
    public WindowTaskContextHolder(WindowIsolationProperties windowIsolationProperties) {
        this.windowIsolationProperties = windowIsolationProperties;
    }

    /**
     * Bind a window context to the current thread.
     *
     * @param context context to bind, or null to clear the binding.
     */
    public void bind(WindowRuntimeContext context) {
        if (context == null) {
            currentContext.remove();
        } else {
            currentContext.set(context);
        }
    }

    /**
     * Clear the current thread's window binding.
     */
    public void clear() {
        currentContext.remove();
    }

    /**
     * Return the raw current binding regardless of the isolation feature switch.
     *
     * @return current window context, or empty when no context is bound.
     */
    public Optional<WindowRuntimeContext> rawCurrent() {
        return Optional.ofNullable(currentContext.get());
    }

    /**
     * Run an action under a temporary window binding and restore the previous binding afterward.
     *
     * @param context temporary context to bind; null clears the binding for the action.
     * @param action action to execute on the current thread.
     */
    public void runWith(WindowRuntimeContext context, Runnable action) {
        WindowRuntimeContext previous = currentContext.get();
        bind(context);
        try {
            action.run();
        } finally {
            if (previous == null) {
                clear();
            } else {
                bind(previous);
            }
        }
    }

    /**
     * Call a supplier under a temporary window binding and restore the previous binding afterward.
     *
     * @param context temporary context to bind; null clears the binding for the supplier.
     * @param action supplier to execute on the current thread.
     * @param <T> supplier return type.
     * @return supplier result.
     */
    public <T> T callWith(WindowRuntimeContext context, Supplier<T> action) {
        WindowRuntimeContext previous = currentContext.get();
        bind(context);
        try {
            return action.get();
        } finally {
            if (previous == null) {
                clear();
            } else {
                bind(previous);
            }
        }
    }
}
