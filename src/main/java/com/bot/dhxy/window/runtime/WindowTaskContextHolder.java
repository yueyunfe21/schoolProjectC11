package com.bot.dhxy.window.runtime;

import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * 当前任务线程绑定的窗口上下文。
 *
 * 多窗口并发时，业务服务里很多旧代码还没有显式传 TaskExecutionContext。
 * 这里先用 ThreadLocal 让底层输入层知道“当前线程正在操作哪个窗口”。
 */
@Component
public class WindowTaskContextHolder {

    private final ThreadLocal<WindowRuntimeContext> currentContext = new ThreadLocal<>();

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
