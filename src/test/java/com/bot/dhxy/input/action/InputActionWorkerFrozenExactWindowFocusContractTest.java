package com.bot.dhxy.input.action;

import com.bot.dhxy.config.WindowIsolationProperties;
import com.bot.dhxy.core.GameContext;
import com.bot.dhxy.driver.BoundWindowKeyboardService;
import com.bot.dhxy.input.InputProvider;
import com.bot.dhxy.input.WindowAwareInputCoordinator;
import com.bot.dhxy.window.model.WindowNativeBinding;
import com.bot.dhxy.window.runtime.WindowRuntimeContext;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InputActionWorkerFrozenExactWindowFocusContractTest {

    @Test
    void backgroundFrozenExclusiveKeepsExactCallbackWithoutFocus() throws Exception {
        Harness harness = new Harness();
        InputActionRequest request = harness.backgroundRequest();

        assertTrue(harness.invoke(request));
        assertEquals(1, harness.callbackCalls.get());
        assertEquals(0, harness.focusCalls.get());
        assertTrue(request.isFrozenExactWindowGenerationCurrent());
    }

    @Test
    void focusedFrozenExclusiveStillFocusesBeforeCaptureCallback() throws Exception {
        Harness harness = new Harness();
        InputActionRequest request = harness.focusedRequest();

        assertTrue(harness.invoke(request));
        assertEquals(1, harness.callbackCalls.get());
        assertEquals(1, harness.focusCalls.get());
        assertTrue(request.isFrozenExactWindowGenerationCurrent());
    }

    private static final class Harness {
        private final AtomicInteger focusCalls = new AtomicInteger();
        private final AtomicInteger callbackCalls = new AtomicInteger();
        private final InputActionWorker worker;
        private final WindowRuntimeContext context;
        private final WindowNativeBinding binding;

        private Harness() {
            InputProvider input = (InputProvider) Proxy.newProxyInstance(
                    InputProvider.class.getClassLoader(),
                    new Class<?>[]{InputProvider.class},
                    (proxy, method, args) -> null);
            WindowAwareInputCoordinator coordinator = new WindowAwareInputCoordinator(
                    null, null, null, new WindowIsolationProperties(), null, null) {
                @Override
                public boolean focusFrozenBindingInActiveTransaction(
                        String actionName,
                        String windowId,
                        WindowNativeBinding frozenBinding) {
                    focusCalls.incrementAndGet();
                    return true;
                }
            };
            worker = new InputActionWorker(
                    null,
                    null,
                    input,
                    coordinator,
                    null,
                    new BoundWindowKeyboardService(null, null, null, null));
            context = new WindowRuntimeContext("window-1", new GameContext());
            binding = new WindowNativeBinding(
                    "12345", "title", "class", 77L, 10, 20, 800, 600);
            context.setNativeBinding(binding);
        }

        private InputActionRequest backgroundRequest() {
            return InputActionRequest.frozenExactWindowBackgroundExclusive(
                    context,
                    binding,
                    context.getPlayerIdentityEpoch(),
                    "xinshou:ordinary-background",
                    () -> {
                        callbackCalls.incrementAndGet();
                        return true;
                    },
                    null,
                    null);
        }

        private InputActionRequest focusedRequest() {
            return InputActionRequest.frozenExactWindowExclusive(
                    context,
                    binding,
                    context.getPlayerIdentityEpoch(),
                    "xinshou:capture-focused",
                    () -> {
                        callbackCalls.incrementAndGet();
                        return true;
                    },
                    null,
                    null);
        }

        private boolean invoke(InputActionRequest request) throws Exception {
            Method method = InputActionWorker.class.getDeclaredMethod(
                    "runFrozenExactWindowExclusive",
                    InputActionRequest.class);
            method.setAccessible(true);
            return (boolean) method.invoke(worker, request);
        }
    }
}
