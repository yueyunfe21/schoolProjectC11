package com.bot.dhxy.window.observation;

import com.bot.dhxy.core.GameContext;
import com.bot.dhxy.runner.context.TaskStartupMode;
import com.bot.dhxy.task.model.TaskType;
import com.bot.dhxy.window.model.WindowRuntimeStatus;
import com.bot.dhxy.window.runtime.WindowRuntimeContext;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StartupCombatGateServiceTest {

    @Test
    void nonCombatColdStartPassesWithoutQueueingOrWaiting() {
        WindowRuntimeContext context = context("world");
        SequenceProbe probe = new SequenceProbe(
                LocalCombatSignalMechanics.Signal.absent(),
                LocalCombatSignalMechanics.Signal.visible("minimap-visible"));
        AtomicInteger sleeps = new AtomicInteger();
        StartupCombatGateService service = new StartupCombatGateService(ignored -> probe,
                millis -> sleeps.incrementAndGet());

        TaskStartupMode mode = service.awaitCombatExit(Map.of(context, TaskType.TIANTING), () -> false);

        assertEquals(TaskStartupMode.NORMAL, mode);
        assertEquals(WindowRuntimeStatus.IDLE, context.getStatus());
        assertEquals(0, sleeps.get());
        assertTrue(probe.closed);
    }

    @Test
    void combatColdStartQueuesUntilTheSameLocalMechanicsConfirmExit() {
        WindowRuntimeContext context = context("combat");
        SequenceProbe probe = new SequenceProbe(
                LocalCombatSignalMechanics.Signal.visible("combat-flag"),
                LocalCombatSignalMechanics.Signal.visible("combat-top"),
                LocalCombatSignalMechanics.Signal.absent());
        probe.minimapSignals.add(LocalCombatSignalMechanics.Signal.absent("minimap-visible"));
        probe.minimapSignals.add(LocalCombatSignalMechanics.Signal.visible("minimap-visible"));
        AtomicInteger sleeps = new AtomicInteger();
        StartupCombatGateService service = new StartupCombatGateService(ignored -> probe,
                millis -> sleeps.incrementAndGet());

        TaskStartupMode mode = service.awaitCombatExit(Map.of(context, TaskType.TIANTING), () -> false);

        assertEquals(TaskStartupMode.AFTER_COMBAT_EXIT_STARTUP, mode);
        assertEquals(WindowRuntimeStatus.QUEUED, context.getStatus());
        assertEquals("战斗中启动：等待战斗结束后继续", context.getLastMessage());
        assertEquals(1, sleeps.get(), "one still-in-combat tick must wait before the exit sample");
        assertEquals(3, probe.combatCalls.get());
        assertEquals(2, probe.minimapCalls.get());
        assertTrue(probe.closed);
    }

    @Test
    void cancellationStopsTheWaitWithoutInventingAnExit() {
        WindowRuntimeContext context = context("cancelled");
        SequenceProbe probe = new SequenceProbe(
                LocalCombatSignalMechanics.Signal.visible("combat-flag"),
                LocalCombatSignalMechanics.Signal.visible("combat-flag"));
        probe.minimapSignals.add(LocalCombatSignalMechanics.Signal.absent("minimap-visible"));
        AtomicBoolean cancelled = new AtomicBoolean();
        StartupCombatGateService service = new StartupCombatGateService(ignored -> probe,
                millis -> cancelled.set(true));

        TaskStartupMode mode = service.awaitCombatExit(
                Map.of(context, TaskType.XIULUO_V2), cancelled::get);

        assertEquals(TaskStartupMode.NORMAL, mode);
        assertEquals(WindowRuntimeStatus.QUEUED, context.getStatus());
        assertEquals(2, probe.combatCalls.get());
        assertTrue(probe.closed);
    }

    private static WindowRuntimeContext context(String windowId) {
        return new WindowRuntimeContext(windowId, new GameContext());
    }

    private static final class SequenceProbe implements StartupCombatGateService.CombatProbe {
        private final Deque<LocalCombatSignalMechanics.Signal> combatSignals = new ArrayDeque<>();
        private final Deque<LocalCombatSignalMechanics.Signal> minimapSignals = new ArrayDeque<>();
        private final AtomicInteger combatCalls = new AtomicInteger();
        private final AtomicInteger minimapCalls = new AtomicInteger();
        private LocalCombatSignalMechanics.Signal lastCombat;
        private LocalCombatSignalMechanics.Signal lastMinimap = LocalCombatSignalMechanics.Signal.absent();
        private boolean closed;

        private SequenceProbe(LocalCombatSignalMechanics.Signal... signals) {
            for (LocalCombatSignalMechanics.Signal signal : signals) {
                combatSignals.add(signal);
            }
        }

        @Override
        public LocalCombatSignalMechanics.Signal combat() {
            combatCalls.incrementAndGet();
            if (!combatSignals.isEmpty()) {
                lastCombat = combatSignals.removeFirst();
            }
            return lastCombat;
        }

        @Override
        public LocalCombatSignalMechanics.Signal minimap() {
            minimapCalls.incrementAndGet();
            if (!minimapSignals.isEmpty()) {
                lastMinimap = minimapSignals.removeFirst();
            }
            return lastMinimap;
        }

        @Override
        public void close() {
            closed = true;
        }
    }
}
