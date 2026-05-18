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
@Component
public class WindowTaskContextHolder {

    private final WindowIsolationProperties windowIsolationProperties;
    private final ThreadLocal<WindowRuntimeContext> currentContext = new ThreadLocal<>();

    public WindowTaskContextHolder(WindowIsolationProperties windowIsolationProperties) {
        this.windowIsolationProperties = windowIsolationProperties;
    }

    public void bind(WindowRuntimeContext context) {
        if (context == null) {
            currentContext.remove();
        } else {
            currentContext.set(context);
        }
    }

    public void clear() {
        currentContext.remove();
    }

    public Optional<WindowRuntimeContext> current() {
        if (!windowIsolationProperties.isIsolationEnabled()) {
            return Optional.empty();
        }
        return Optional.ofNullable(currentContext.get());
    }

    public Optional<WindowRuntimeContext> rawCurrent() {
        return Optional.ofNullable(currentContext.get());
    }

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
