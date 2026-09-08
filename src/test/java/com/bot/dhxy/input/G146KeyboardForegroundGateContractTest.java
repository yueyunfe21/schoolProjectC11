package com.bot.dhxy.input;

import com.bot.dhxy.config.WindowIsolationProperties;
import com.bot.dhxy.core.GameContext;
import com.bot.dhxy.window.diagnostics.WindowInteractionMetricsService;
import com.bot.dhxy.window.interaction.WindowFocusService;
import com.bot.dhxy.window.model.WindowNativeBinding;
import com.bot.dhxy.window.runtime.WindowRuntimeContext;
import com.bot.dhxy.window.runtime.WindowTaskContextHolder;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * G146 keyboard foreground gate: HID keyboard reports land on the focused window, so
 * {@link WindowAwareInputCoordinator#runKeyboardInput} must verify the bound window is foreground,
 * self-heal with one refocus attempt, and otherwise refuse the keystroke instead of sending it
 * into whatever window happens to hold focus.
 */
class G146KeyboardForegroundGateContractTest {

    @Test
    void keystrokeRefusedWhenWindowNotForegroundAfterRefocus() {
        Harness harness = new Harness(true);
        harness.focus.foreground = false;
        harness.focus.refocusSucceeds = false;

        AtomicInteger ran = new AtomicInteger();
        harness.inTransaction(() -> assertThrows(IllegalStateException.class,
                () -> harness.coordinator.runKeyboardInput("test:altq", ran::incrementAndGet)));
        assertEquals(0, ran.get(), "a refused keystroke must never reach the device");
        assertEquals(1, harness.focus.refocusCalls.get(), "exactly one self-healing refocus attempt");
    }

    @Test
    void keystrokeRunsWhenWindowAlreadyForeground() {
        Harness harness = new Harness(true);
        harness.focus.foreground = true;

        AtomicInteger ran = new AtomicInteger();
        harness.inTransaction(() -> harness.coordinator.runKeyboardInput("test:altq", ran::incrementAndGet));
        assertEquals(1, ran.get());
        assertEquals(0, harness.focus.refocusCalls.get(), "no refocus when already foreground");
    }

    @Test
    void keystrokeSelfHealsWhenRefocusSucceeds() {
        Harness harness = new Harness(true);
        harness.focus.foreground = false;
        harness.focus.refocusSucceeds = true;

        AtomicInteger ran = new AtomicInteger();
        harness.inTransaction(() -> harness.coordinator.runKeyboardInput("test:alt8", ran::incrementAndGet));
        assertEquals(1, ran.get(), "a successful refocus must let the keystroke through");
        assertEquals(1, harness.focus.refocusCalls.get());
    }

    @Test
    void gateIsPermissiveWithoutWindowContext() {
        Harness harness = new Harness(true);
        harness.focus.foreground = false;
        harness.contextHolder.clear();

        AtomicInteger ran = new AtomicInteger();
        harness.inTransaction(() -> harness.coordinator.runKeyboardInput("test:noctx", ran::incrementAndGet));
        assertEquals(1, ran.get(), "an unverifiable keystroke keeps legacy permissive behaviour");
    }

    @Test
    void gateIsPermissiveWhenFocusIsolationDisabled() {
        Harness harness = new Harness(false);
        harness.focus.foreground = false;

        AtomicInteger ran = new AtomicInteger();
        harness.inTransaction(() -> harness.coordinator.runKeyboardInput("test:nofocus", ran::incrementAndGet));
        assertEquals(1, ran.get());
        assertEquals(0, harness.focus.refocusCalls.get());
    }

    private static final class Harness {
        final StubFocusService focus;
        final WindowTaskContextHolder contextHolder;
        final WindowAwareInputCoordinator coordinator;

        private Harness(boolean focusActive) {
            WindowIsolationProperties properties = new WindowIsolationProperties();
            properties.setIsolationEnabled(focusActive);
            properties.setInputFocusEnabled(focusActive);
            GlobalInputLock lock = new GlobalInputLock();
            focus = new StubFocusService(lock);
            contextHolder = new WindowTaskContextHolder(properties);
            WindowRuntimeContext context = new WindowRuntimeContext("window-1", new GameContext());
            context.setNativeBinding(new WindowNativeBinding(
                    "12345", "title", "class", 77L, 10, 20, 800, 600));
            contextHolder.bind(context);
            coordinator = new WindowAwareInputCoordinator(
                    lock,
                    contextHolder,
                    focus,
                    properties,
                    new NoOpMetrics(),
                    null);
        }

        private void inTransaction(Runnable body) {
            coordinator.runInputTransaction("test:transaction", false, body);
        }
    }

    private static final class StubFocusService extends WindowFocusService {
        volatile boolean foreground;
        volatile boolean refocusSucceeds;
        final AtomicInteger refocusCalls = new AtomicInteger();

        private StubFocusService(GlobalInputLock lock) {
            super(lock);
        }

        @Override
        public boolean isForeground(WindowNativeBinding binding) {
            return foreground;
        }

        @Override
        public boolean focusWithoutLock(WindowNativeBinding binding) {
            refocusCalls.incrementAndGet();
            if (refocusSucceeds) {
                foreground = true;
                return true;
            }
            return false;
        }
    }

    private static final class NoOpMetrics extends WindowInteractionMetricsService {
        @Override
        public void recordFocus(String windowId, String actionName, boolean success) {
        }
    }
}
