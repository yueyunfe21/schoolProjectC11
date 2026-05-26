package com.bot.dhxy.input;

import com.bot.dhxy.config.WindowIsolationProperties;
import com.bot.dhxy.window.diagnostics.WindowInteractionMetricsService;
import com.bot.dhxy.window.interaction.WindowFocusService;
import com.bot.dhxy.window.runtime.WindowRuntimeContext;
import com.bot.dhxy.window.runtime.WindowTaskContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * Window-aware input coordinator.
 *
 * By default it only serializes global input. When window isolation and input
 * focus are enabled, it focuses the hwnd bound to the current window task before
 * real input is sent.
 */
@Slf4j
@Component
public class WindowAwareInputCoordinator {

    private final GlobalInputLock globalInputLock;
    private final WindowTaskContextHolder windowTaskContextHolder;
    private final WindowFocusService windowFocusService;
    private final WindowIsolationProperties windowIsolationProperties;
    private final WindowInteractionMetricsService windowInteractionMetricsService;

    private final ThreadLocal<Boolean> inputTransactionActive = ThreadLocal.withInitial(() -> false);
    private final ThreadLocal<String> currentInputActionName = new ThreadLocal<>();

    public WindowAwareInputCoordinator(GlobalInputLock globalInputLock,
                                       WindowTaskContextHolder windowTaskContextHolder,
                                       WindowFocusService windowFocusService,
                                       WindowIsolationProperties windowIsolationProperties,
                                       WindowInteractionMetricsService windowInteractionMetricsService) {
        this.globalInputLock = globalInputLock;
        this.windowTaskContextHolder = windowTaskContextHolder;
        this.windowFocusService = windowFocusService;
        this.windowIsolationProperties = windowIsolationProperties;
        this.windowInteractionMetricsService = windowInteractionMetricsService;
    }

    public void runInput(String actionName, Runnable action) {
        if (inputTransactionActive.get()) {
            action.run();
            return;
        }
        globalInputLock.runWithLock(() -> {
            String previousActionName = currentInputActionName.get();
            currentInputActionName.set(actionName);
            try {
                focusCurrentWindowWithoutLock(actionName);
                action.run();
            } finally {
                restoreActionName(previousActionName);
            }
        });
    }

    public <T> T callInput(String actionName, Supplier<T> action) {
        if (inputTransactionActive.get()) {
            return action.get();
        }
        return globalInputLock.callWithLock(() -> {
            String previousActionName = currentInputActionName.get();
            currentInputActionName.set(actionName);
            try {
                focusCurrentWindowWithoutLock(actionName);
                return action.get();
            } finally {
                restoreActionName(previousActionName);
            }
        });
    }

    public void runInputTransaction(String actionName, Runnable action) {
        runInputTransaction(actionName, true, action);
    }

    public void runInputTransaction(String actionName, boolean focusBeforeInput, Runnable action) {
        globalInputLock.runWithLock(() -> {
            boolean previous = inputTransactionActive.get();
            String previousActionName = currentInputActionName.get();
            inputTransactionActive.set(true);
            currentInputActionName.set(actionName);
            try {
                if (focusBeforeInput) {
                    focusCurrentWindowWithoutLock(actionName);
                }
                action.run();
            } finally {
                inputTransactionActive.set(previous);
                restoreActionName(previousActionName);
            }
        });
    }

    public <T> T callInputTransaction(String actionName, Supplier<T> action) {
        return callInputTransaction(actionName, true, action);
    }

    public <T> T callInputTransaction(String actionName, boolean focusBeforeInput, Supplier<T> action) {
        return globalInputLock.callWithLock(() -> {
            boolean previous = inputTransactionActive.get();
            String previousActionName = currentInputActionName.get();
            inputTransactionActive.set(true);
            currentInputActionName.set(actionName);
            try {
                if (focusBeforeInput) {
                    focusCurrentWindowWithoutLock(actionName);
                }
                return action.get();
            } finally {
                inputTransactionActive.set(previous);
                restoreActionName(previousActionName);
            }
        });
    }

    public String currentInputActionName() {
        return currentInputActionName.get();
    }

    public boolean focusCurrentWindowInActiveTransaction(String actionName) {
        if (!inputTransactionActive.get()) {
            throw new IllegalStateException("focusCurrentWindowInActiveTransaction must run inside input transaction");
        }
        return focusCurrentWindowWithoutLock(actionName);
    }

    private boolean focusCurrentWindowWithoutLock(String actionName) {
        if (!windowIsolationProperties.isInputFocusActive()) {
            return false;
        }
        Optional<WindowRuntimeContext> contextOptional = windowTaskContextHolder.rawCurrent();
        if (contextOptional.isEmpty()) {
            return false;
        }
        WindowRuntimeContext context = contextOptional.get();
        if (!context.hasNativeBinding()) {
            return false;
        }
        boolean focused = windowFocusService.focusWithoutLock(context.getNativeBinding());
        windowInteractionMetricsService.recordFocus(context.getWindowId(), actionName, focused);
        if (!focused) {
            log.debug("Input window focus failed before action: windowId={} action={}", context.getWindowId(), actionName);
        }
        return focused;
    }

    private void restoreActionName(String previousActionName) {
        if (previousActionName == null) {
            currentInputActionName.remove();
        } else {
            currentInputActionName.set(previousActionName);
        }
    }
}
