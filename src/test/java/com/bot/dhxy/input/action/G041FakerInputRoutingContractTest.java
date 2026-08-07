package com.bot.dhxy.input.action;

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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

class G041FakerInputRoutingContractTest {

    @Test
    void onlyAltFiveSixEightRemainBackgroundWhenDriverBackendIsActive() throws Exception {
        Harness harness = new Harness();

        assertTrue(harness.canUseBackground(InputAction.pressAlt5()));
        assertTrue(harness.canUseBackground(InputAction.pressAlt6()));
        assertTrue(harness.canUseBackground(InputAction.pressAlt8()));
        assertFalse(harness.canUseBackground(InputAction.pressAlt1()));
        assertFalse(harness.canUseBackground(InputAction.pressCtrlA()));
        assertFalse(harness.canUseBackground(InputAction.pressEnter()));
        assertFalse(harness.canUseBackground(InputAction.clickLeft(100, 200, 50)));
    }

    @Test
    void ordinaryAltUsesDriverProviderButWhitelistedAltUsesExactHwnd() throws Exception {
        Harness harness = new Harness();

        assertTrue(harness.execute(InputAction.pressAlt1()));
        assertEquals(1, harness.driverAltOneCalls.get());
        assertEquals(0, harness.hwndCalls.get());

        assertTrue(harness.execute(InputAction.pressAlt6()));
        assertEquals(1, harness.driverAltOneCalls.get());
        assertEquals(1, harness.hwndCalls.get());
    }

    @Test
    void ordinaryDriverKeyboardFocusesFrozenWindowWhileWhitelistedAltDoesNot() throws Exception {
        Harness harness = new Harness();

        assertTrue(harness.executeFrozen(InputAction.pressAlt1(), false));
        assertEquals(1, harness.focusCalls.get());
        assertEquals(1, harness.driverAltOneCalls.get());

        assertTrue(harness.executeFrozen(InputAction.pressAlt8(), true));
        assertEquals(1, harness.focusCalls.get());
        assertEquals(1, harness.hwndCalls.get());
    }

    @Test
    void physicalCtrlHoldMustReleaseInsideTheSameQueueRequest() throws Exception {
        Harness harness = new Harness();

        assertFalse(harness.hasSafeModifierLifecycle(List.of(InputAction.holdCtrl())));
        assertTrue(harness.hasSafeModifierLifecycle(List.of(
                InputAction.holdCtrl(),
                InputAction.clickLeft(100, 200, 50),
                InputAction.releaseCtrl())));
        assertTrue(harness.hasSafeModifierLifecycle(List.of(InputAction.releaseCtrl())));
    }

    private static final class Harness {
        private final AtomicInteger driverAltOneCalls = new AtomicInteger();
        private final AtomicInteger hwndCalls = new AtomicInteger();
        private final AtomicInteger focusCalls = new AtomicInteger();
        private final InputActionWorker worker;
        private final WindowRuntimeContext context;
        private final WindowNativeBinding binding;

        private Harness() {
            InputProvider provider = (InputProvider) Proxy.newProxyInstance(
                    InputProvider.class.getClassLoader(),
                    new Class<?>[]{InputProvider.class},
                    (proxy, method, args) -> {
                        if (method.getName().equals("requiresForegroundKeyboard")) {
                            return true;
                        }
                        if (method.getName().equals("pressAlt1")) {
                            driverAltOneCalls.incrementAndGet();
                        }
                        return null;
                    });
            BoundWindowKeyboardService keyboard = new BoundWindowKeyboardService(null, null, null, null) {
                @Override
                public ShortcutAttempt pressShortcut(
                        WindowNativeBinding binding,
                        String windowId,
                        AltShortcut shortcut) {
                    hwndCalls.incrementAndGet();
                    return new ShortcutAttempt(true, true, "OK", false);
                }
            };
            WindowAwareInputCoordinator coordinator = new WindowAwareInputCoordinator(
                    null, null, null, null, null, null) {
                @Override
                public boolean focusFrozenBindingInActiveTransaction(
                        String actionName,
                        String windowId,
                        WindowNativeBinding binding) {
                    focusCalls.incrementAndGet();
                    return true;
                }
            };
            worker = new InputActionWorker(null, null, provider, coordinator, null, keyboard);
            context = new WindowRuntimeContext("window-1", new GameContext());
            binding = new WindowNativeBinding("12345", "title", "class", 77L, 10, 20, 800, 600);
            context.setNativeBinding(binding);
        }

        private boolean canUseBackground(InputAction action) throws Exception {
            Method method = InputActionWorker.class.getDeclaredMethod(
                    "canUseBackgroundKeyboard", InputActionRequest.class);
            method.setAccessible(true);
            return (boolean) method.invoke(worker, request(action));
        }

        private boolean execute(InputAction action) throws Exception {
            Method method = InputActionWorker.class.getDeclaredMethod(
                    "execute", InputActionRequest.class, InputAction.class, boolean.class, String.class);
            method.setAccessible(true);
            return (boolean) method.invoke(worker, request(action), action, false, "g041-test");
        }

        private boolean executeFrozen(InputAction action, boolean preferBackground) throws Exception {
            Method method = InputActionWorker.class.getDeclaredMethod(
                    "runFrozenExactWindowActions", InputActionRequest.class, boolean.class);
            method.setAccessible(true);
            return (boolean) method.invoke(worker, request(action), preferBackground);
        }

        private boolean hasSafeModifierLifecycle(List<InputAction> actions) throws Exception {
            Method method = InputActionWorker.class.getDeclaredMethod(
                    "hasSafeForegroundModifierLifecycle", InputActionRequest.class);
            method.setAccessible(true);
            return (boolean) method.invoke(worker, request(actions));
        }

        private InputActionRequest request(InputAction action) {
            return request(List.of(action));
        }

        private InputActionRequest request(List<InputAction> actions) {
            return InputActionRequest.frozenExactWindowActions(
                    context,
                    binding,
                    context.getPlayerIdentityEpoch(),
                    "g041-routing",
                    actions,
                    null,
                    null,
                    null);
        }
    }
}
