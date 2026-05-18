package com.bot.dhxy.input;

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
 * 多窗口并行时，真实鼠标/键盘仍然只有一套。
 * 所以所有真实输入必须：
 * 1. 进入全局输入锁
 * 2. 尝试激活当前任务绑定的窗口
 * 3. 执行真实输入
 */
@Slf4j
@Component
public class WindowAwareInputCoordinator {

    private final GlobalInputLock globalInputLock;
    private final WindowTaskContextHolder windowTaskContextHolder;
    private final WindowFocusService windowFocusService;

    public WindowAwareInputCoordinator(GlobalInputLock globalInputLock,
                                       WindowTaskContextHolder windowTaskContextHolder,
                                       WindowFocusService windowFocusService) {
        this.globalInputLock = globalInputLock;
        this.windowTaskContextHolder = windowTaskContextHolder;
        this.windowFocusService = windowFocusService;
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
