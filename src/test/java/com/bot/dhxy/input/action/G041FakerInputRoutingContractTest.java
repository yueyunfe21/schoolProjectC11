package com.bot.dhxy.input.action;

import com.bot.dhxy.core.GameContext;
import com.bot.dhxy.config.InputBackendProperties;
import com.bot.dhxy.driver.fakerinput.FakerInputProvider;
import com.bot.dhxy.input.InputProvider;
import com.bot.dhxy.input.WindowAwareInputCoordinator;
import com.bot.dhxy.task.model.TaskType;
import com.bot.dhxy.window.model.WindowNativeBinding;
import com.bot.dhxy.window.model.WindowRole;
import com.bot.dhxy.window.runtime.WindowRuntimeContext;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * G041 (updated by G146): FakerInput is the only input backend and every keyboard action —
 * the former Alt+8/Alt+5/Alt+6 PostMessage whitelist included — routes through the HID provider
 * behind a focused frozen-window transaction. The PostMessage keyboard path no longer exists.
 */
class G041FakerInputRoutingContractTest {

    @Test
    void fakerInputIsTheOnlyBackendAndIsDefault() {
        assertEquals(InputBackendProperties.Backend.FAKER_INPUT,
                new InputBackendProperties().getBackend());
        assertEquals(1, InputBackendProperties.Backend.values().length,
                "G146: the WIN_API backend is deleted; FAKER_INPUT must be the only backend");
        ConditionalOnProperty fakerCondition = FakerInputProvider.class
                .getAnnotation(ConditionalOnProperty.class);
        assertTrue(fakerCondition.matchIfMissing());
    }

    @Test
    void postMessageKeyboardServiceIsDeleted() {
        try {
            Class.forName("com.bot.dhxy.driver.BoundWindowKeyboardService");
            throw new AssertionError("G146: BoundWindowKeyboardService must not exist");
        } catch (ClassNotFoundException expected) {
            // the PostMessage keyboard path is gone
        }
        try {
            Class.forName("com.bot.dhxy.driver.WinApiMouseController");
            throw new AssertionError("G146: WinApiMouseController must not exist");
        } catch (ClassNotFoundException expected) {
            // the SendInput/SetCursorPos backend is gone
        }
    }

    @Test
    void everyAltShortcutRoutesThroughTheDriverProvider() throws Exception {
        Harness harness = new Harness();
        Map<InputActionType, String> altToProvider = Map.ofEntries(
                Map.entry(InputActionType.PRESS_ALT_1, "pressAlt1"),
                Map.entry(InputActionType.PRESS_ALT_2, "pressAlt2"),
                Map.entry(InputActionType.PRESS_ALT_4, "pressAlt4"),
                Map.entry(InputActionType.PRESS_ALT_5, "pressAlt5"),
                Map.entry(InputActionType.PRESS_ALT_6, "pressAlt6"),
                Map.entry(InputActionType.PRESS_ALT_8, "pressAlt8"),
                Map.entry(InputActionType.PRESS_ALT_T, "pressAltT"),
                Map.entry(InputActionType.PRESS_ALT_O, "pressAltO"),
                Map.entry(InputActionType.PRESS_ALT_E, "pressAltE"),
                Map.entry(InputActionType.PRESS_ALT_Q, "pressAltQ"),
                Map.entry(InputActionType.PRESS_ALT_A, "pressAltA"),
                Map.entry(InputActionType.PRESS_ALT_B, "pressAltB"),
                Map.entry(InputActionType.PRESS_ALT_C, "pressAltC"),
                Map.entry(InputActionType.PRESS_ALT_U, "pressAltU"));
        for (Map.Entry<InputActionType, String> entry : altToProvider.entrySet()) {
            harness.executeAlt(entry.getKey());
            assertEquals(1, harness.calls(entry.getValue()),
                    "Alt action must call exactly its provider method: " + entry.getKey());
        }
    }

    @Test
    void formerWhitelistedAltEightRoutesThroughDriverForLeaderTeamTask() throws Exception {
        Harness harness = new Harness();
        harness.asLeader(TaskType.TIANTING);

        assertTrue(harness.executeFrozen(InputAction.pressAlt8()));
        assertEquals(1, harness.calls("pressAlt8"));
        assertEquals(1, harness.focusCalls.get(),
                "G146: Alt+8 must focus the frozen window like every other keyboard action");

        assertTrue(harness.executeFrozen(InputAction.pressAlt5()));
        assertTrue(harness.executeFrozen(InputAction.pressAlt6()));
        assertEquals(3, harness.focusCalls.get());
        assertEquals(1, harness.calls("pressAlt5"));
        assertEquals(1, harness.calls("pressAlt6"));
    }

    @Test
    void memberContextAlsoFocusesEveryFrozenKeyboardAction() throws Exception {
        Harness harness = new Harness();
        harness.asMember(TaskType.TIANTING);

        assertTrue(harness.executeFrozen(InputAction.pressAlt5()));
        assertTrue(harness.executeFrozen(InputAction.pressAlt8()));
        assertEquals(2, harness.focusCalls.get());
    }

    @Test
    void nonAltKeyboardActionsRouteThroughTheDriverProvider() throws Exception {
        Harness harness = new Harness();
        harness.executeAction(InputAction.pressCtrlA());
        harness.executeAction(InputAction.pressEnter());
        assertEquals(1, harness.calls("pressCtrlA"));
        assertEquals(1, harness.calls("pressEnter"));
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
        private final Map<String, AtomicInteger> providerCalls = new ConcurrentHashMap<>();
        private final AtomicInteger focusCalls = new AtomicInteger();
        private final InputActionWorker worker;
        private final WindowRuntimeContext context;
        private final WindowNativeBinding binding;

        private Harness() {
            InputProvider provider = (InputProvider) Proxy.newProxyInstance(
                    InputProvider.class.getClassLoader(),
                    new Class<?>[]{InputProvider.class},
                    (proxy, method, args) -> {
                        providerCalls.computeIfAbsent(method.getName(), unused -> new AtomicInteger())
                                .incrementAndGet();
                        return null;
                    });
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
            worker = new InputActionWorker(null, null, provider, coordinator, null);
            context = new WindowRuntimeContext("window-1", new GameContext());
            binding = new WindowNativeBinding("12345", "title", "class", 77L, 10, 20, 800, 600);
            context.setNativeBinding(binding);
        }

        private int calls(String methodName) {
            AtomicInteger counter = providerCalls.get(methodName);
            return counter == null ? 0 : counter.get();
        }

        private void asLeader(TaskType taskType) {
            context.updateRole(WindowRole.LEADER, "leader");
            context.setSelectedTaskType(taskType);
        }

        private void asMember(TaskType taskType) {
            context.updateRole(WindowRole.MEMBER, "member");
            context.setSelectedTaskType(taskType);
        }

        private void executeAlt(InputActionType type) throws Exception {
            Method method = InputActionWorker.class.getDeclaredMethod(
                    "executeForegroundAltShortcut", InputActionType.class);
            method.setAccessible(true);
            method.invoke(worker, type);
        }

        private void executeAction(InputAction action) throws Exception {
            Method method = InputActionWorker.class.getDeclaredMethod(
                    "execute", InputActionRequest.class, InputAction.class, String.class);
            method.setAccessible(true);
            method.invoke(worker, request(action), action, "g041-test");
        }

        private boolean executeFrozen(InputAction action) throws Exception {
            Method method = InputActionWorker.class.getDeclaredMethod(
                    "runFrozenExactWindowActions", InputActionRequest.class);
            method.setAccessible(true);
            return (boolean) method.invoke(worker, request(action));
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
