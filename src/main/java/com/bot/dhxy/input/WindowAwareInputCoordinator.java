package com.bot.dhxy.input;

import com.bot.dhxy.config.WindowIsolationProperties;
import com.bot.dhxy.window.interaction.WindowFocusService;
import com.bot.dhxy.window.runtime.WindowRuntimeContext;
import com.bot.dhxy.window.runtime.WindowTaskContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * 窗口感知输入协调器。
 *
 * 默认只做全局输入串行化；只有 bot.window.isolation-enabled=true 时，
 * 才会在输入前按当前任务线程绑定的 hwnd 自动激活窗口。
 */
@Slf4j
@Component
public class WindowAwareInputCoordinator {

    private final GlobalInputLock globalInputLock;
    private final WindowTaskContextHolder windowTaskContextHolder;
    private final WindowFocusService windowFocusService;
    private final WindowIsolationProperties windowIsolationProperties;

    public WindowAwareInputCoordinator(GlobalInputLock globalInputLock,
                                       WindowTaskContextHolder windowTaskContextHolder,
                                       WindowFocusService windowFocusService,
                                       WindowIsolationProperties windowIsolationProperties) {
        this.globalInputLock = globalInputLock;
        this.windowTaskContextHolder = windowTaskContextHolder;
        this.windowFocusService = windowFocusService;
        this.windowIsolationProperties = windowIsolationProperties;
    }

    public void runInput(String actionName, Runnable action) {
        globalInputLock.runWithLock(() -> {
            focusCurrentWindowWithoutLock(actionName);
            action.run();
        });
    }

    public <T> T callInput(String actionName, Supplier<T> action) {
        return globalInputLock.callWithLock(() -> {
            focusCurrentWindowWithoutLock(actionName);
            return action.get();
        });
    }

    private void focusCurrentWindowWithoutLock(String actionName) {
        if (!windowIsolationProperties.isIsolationEnabled()) {
            return;
        }
        Optional<WindowRuntimeContext> contextOptional = windowTaskContextHolder.current();
        if (contextOptional.isEmpty()) {
            return;
        }
        WindowRuntimeContext context = contextOptional.get();
        if (!context.hasNativeBinding()) {
            return;
        }
        boolean focused = windowFocusService.focusWithoutLock(context.getNativeBinding());
        if (!focused) {
            log.debug("输入前窗口激活失败：windowId={} action={}", context.getWindowId(), actionName);
        }
    }
}
