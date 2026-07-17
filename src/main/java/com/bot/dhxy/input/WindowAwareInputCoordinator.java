package com.bot.dhxy.input;

import com.bot.dhxy.config.WindowIsolationProperties;
import com.bot.dhxy.window.diagnostics.WindowInteractionMetricsService;
import com.bot.dhxy.window.interaction.WindowFocusService;
import com.bot.dhxy.window.model.WindowNativeBinding;
import com.bot.dhxy.window.runtime.WindowNativeBindingRefreshService;
import com.bot.dhxy.window.runtime.WindowRuntimeContext;
import com.bot.dhxy.window.runtime.WindowTaskContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.Objects;
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
    private final WindowNativeBindingRefreshService bindingRefreshService;

    private final ThreadLocal<Boolean> inputTransactionActive = ThreadLocal.withInitial(() -> false);
    private final ThreadLocal<String> currentInputActionName = new ThreadLocal<>();

    public WindowAwareInputCoordinator(GlobalInputLock globalInputLock,
                                       WindowTaskContextHolder windowTaskContextHolder,
                                       WindowFocusService windowFocusService,
                                       WindowIsolationProperties windowIsolationProperties,
                                       WindowInteractionMetricsService windowInteractionMetricsService,
                                       WindowNativeBindingRefreshService bindingRefreshService) {
        this.globalInputLock = globalInputLock;
        this.windowTaskContextHolder = windowTaskContextHolder;
        this.windowFocusService = windowFocusService;
        this.windowIsolationProperties = windowIsolationProperties;
        this.windowInteractionMetricsService = windowInteractionMetricsService;
        this.bindingRefreshService = bindingRefreshService;
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
                if (focusCurrentWindowWithoutLock(actionName).abortInput()) {
                    return;
                }
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
                if (focusCurrentWindowWithoutLock(actionName).abortInput()) {
                    return null;
                }
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
                if (focusBeforeInput && focusCurrentWindowWithoutLock(actionName).abortInput()) {
                    return;
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
                if (focusBeforeInput && focusCurrentWindowWithoutLock(actionName).abortInput()) {
                    return null;
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
        FocusPreparationResult result = focusCurrentWindowWithoutLock(actionName);
        if (result.abortInput()) {
            throw new IllegalStateException("live binding refresh unavailable before input focus: " + actionName);
        }
        return result.focused();
    }

    /**
     * Focus exactly the caller-supplied frozen binding without refreshing or searching for another window.
     *
     * <p>This coordinator holds no second mutable-context comparator: the caller ({@code InputActionWorker})
     * is the single authoritative exact-window checker and already owns the runtime-context generation
     * monitor across this focus, the callback and its {@code finally}. Re-comparing a mutable context here
     * would only add a second, weaker witness that can disagree with the one the monitor already froze.</p>
     *
     * @param actionName diagnostic action label
     * @param windowId exact logical window id captured with the binding
     * @param binding exact HWND/process/screen-rectangle binding snapshot; focused verbatim
     * @return best-effort focus result; false when focus isolation is disabled or Windows rejects focus
     */
    public boolean focusFrozenBindingInActiveTransaction(
            String actionName,
            String windowId,
            WindowNativeBinding binding) {
        if (!inputTransactionActive.get()) {
            throw new IllegalStateException(
                    "focusFrozenBindingInActiveTransaction must run inside input transaction");
        }
        Objects.requireNonNull(binding, "binding");
        if (!windowIsolationProperties.isInputFocusActive()) {
            return false;
        }
        boolean focused = windowFocusService.focusWithoutLock(binding);
        windowInteractionMetricsService.recordFocus(windowId, actionName, focused);
        if (!focused) {
            log.debug("Frozen input window focus failed before action: windowId={} action={}",
                    windowId, actionName);
        }
        return focused;
    }

    private FocusPreparationResult focusCurrentWindowWithoutLock(String actionName) {
        if (!windowIsolationProperties.isInputFocusActive()) {
            return FocusPreparationResult.SKIPPED;
        }
        Optional<WindowRuntimeContext> contextOptional = windowTaskContextHolder.rawCurrent();
        if (contextOptional.isEmpty()) {
            return FocusPreparationResult.SKIPPED;
        }
        WindowRuntimeContext context = contextOptional.get();
        if (!context.hasNativeBinding()) {
            return FocusPreparationResult.SKIPPED;
        }
        Optional<WindowNativeBinding> refreshedBinding = bindingRefreshService.refreshAndCommit(context);
        if (refreshedBinding.isEmpty()) {
            log.warn("Input window focus rejected because live binding refresh is unavailable: windowId={} action={}",
                    context.getWindowId(), actionName);
            return FocusPreparationResult.ABORT_INPUT;
        }
        WindowNativeBinding binding = context.getNativeBinding();
        if (binding == null || !binding.hasNativeHandle()) {
            return FocusPreparationResult.SKIPPED;
        }
        context.waitIfIdentitySuspended(null);
        boolean focused = windowFocusService.focusWithoutLock(binding);
        windowInteractionMetricsService.recordFocus(context.getWindowId(), actionName, focused);
        if (!focused) {
            log.debug("Input window focus failed before action: windowId={} action={}", context.getWindowId(), actionName);
        }
        return focused ? FocusPreparationResult.FOCUSED : FocusPreparationResult.SKIPPED;
    }

    private void restoreActionName(String previousActionName) {
        if (previousActionName == null) {
            currentInputActionName.remove();
        } else {
            currentInputActionName.set(previousActionName);
        }
    }


    private enum FocusPreparationResult {
        SKIPPED(false, false),
        FOCUSED(false, true),
        ABORT_INPUT(true, false);

        private final boolean abortInput;
        private final boolean focused;

        FocusPreparationResult(boolean abortInput, boolean focused) {
            this.abortInput = abortInput;
            this.focused = focused;
        }

        private boolean abortInput() {
            return abortInput;
        }

        private boolean focused() {
            return focused;
        }
    }
}
