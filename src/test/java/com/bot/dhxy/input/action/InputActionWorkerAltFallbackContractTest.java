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
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InputActionWorkerAltFallbackContractTest {

    @Test
    void hwndNotAttemptedFailsClosedWithoutFocusedRealInput() throws Exception {
        Harness harness = new Harness(new BoundWindowKeyboardService.ShortcutAttempt(
                false, false, "disabled", false));

        assertFalse(harness.invoke());
        assertEquals(0, harness.focusCalls.get());
        assertEquals(0, harness.realAltCalls.get());
    }

    @Test
    void ordinaryHwndFailureFailsClosedWithoutFocusedRealInput() throws Exception {
        Harness harness = new Harness(new BoundWindowKeyboardService.ShortcutAttempt(
                true, false, "post-message-failed", false));

        assertFalse(harness.invoke());
        assertEquals(0, harness.focusCalls.get());
        assertEquals(0, harness.realAltCalls.get());
    }

    @Test
    void terminalHwndRejectionDoesNotFocusOrRetry() throws Exception {
        Harness harness = new Harness(new BoundWindowKeyboardService.ShortcutAttempt(
                false, false, "identity-rejected", true));

        assertFalse(harness.invoke());
        assertEquals(0, harness.focusCalls.get());
        assertEquals(0, harness.realAltCalls.get());
    }

    @Test
    void frozenPureKeyboardBundleNeverFocuses() throws Exception {
        Harness harness = new Harness(new BoundWindowKeyboardService.ShortcutAttempt(
                true, true, "OK", false));

        assertTrue(harness.invokeFrozenActions());
        assertEquals(0, harness.focusCalls.get());
        assertEquals(0, harness.realAltCalls.get());
    }

    @Test
    void altBundleIsClassifiedAsBackgroundKeyboard() throws Exception {
        Harness harness = new Harness(new BoundWindowKeyboardService.ShortcutAttempt(
                true, true, "OK", false));

        assertTrue(harness.canUseBackgroundKeyboard());
    }

    private static final class Harness {
        private final AtomicInteger focusCalls = new AtomicInteger();
        private final AtomicInteger realAltCalls = new AtomicInteger();
        private final InputActionWorker worker;
        private final InputActionRequest request;

        private Harness(BoundWindowKeyboardService.ShortcutAttempt attempt) {
            InputProvider input = (InputProvider) Proxy.newProxyInstance(
                    InputProvider.class.getClassLoader(),
                    new Class<?>[]{InputProvider.class},
                    (proxy, method, args) -> {
                        if (method.getName().equals("pressAlt1")) {
                            realAltCalls.incrementAndGet();
                        }
                        return null;
                    });
            WindowAwareInputCoordinator coordinator = new WindowAwareInputCoordinator(
                    null, null, null, new WindowIsolationProperties(), null, null) {
                @Override
                public boolean focusFrozenBindingInActiveTransaction(
                        String actionName, String windowId, WindowNativeBinding binding) {
                    focusCalls.incrementAndGet();
                    return true;
                }
            };
            BoundWindowKeyboardService keyboard = new BoundWindowKeyboardService(null, null, null, null) {
                @Override
                public ShortcutAttempt pressShortcut(
                        WindowNativeBinding binding, String windowId, AltShortcut shortcut) {
                    return attempt;
                }
            };
            worker = new InputActionWorker(null, null, input, coordinator, null, keyboard);

            WindowRuntimeContext context = new WindowRuntimeContext("window-1", new GameContext());
            WindowNativeBinding binding = new WindowNativeBinding(
                    "12345", "title", "class", 77L, 10, 20, 800, 600);
            context.setNativeBinding(binding);
            request = InputActionRequest.frozenExactWindowActions(
                    context, binding, context.getPlayerIdentityEpoch(), "alt-fallback",
                    List.of(InputAction.pressAlt1()), null, null);
        }

        private boolean invoke() throws Exception {
            Method method = InputActionWorker.class.getDeclaredMethod(
                    "pressAltShortcut", InputActionRequest.class, InputActionType.class,
                    boolean.class, String.class);
            method.setAccessible(true);
            return (boolean) method.invoke(worker, request, InputActionType.PRESS_ALT_1, true, "test");
        }

        private boolean invokeFrozenActions() throws Exception {
            Method method = InputActionWorker.class.getDeclaredMethod(
                    "runFrozenExactWindowActions", InputActionRequest.class, boolean.class);
            method.setAccessible(true);
            return (boolean) method.invoke(worker, request, true);
        }

        private boolean canUseBackgroundKeyboard() throws Exception {
            Method method = InputActionWorker.class.getDeclaredMethod(
                    "canUseBackgroundKeyboard", InputActionRequest.class);
            method.setAccessible(true);
            return (boolean) method.invoke(worker, request);
        }
    }
}
