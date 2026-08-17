package com.bot.dhxy.window.observation;

import com.bot.dhxy.cloud.turn.local.XinshouCombatLocalMechanics;
import com.bot.dhxy.core.GameContext;
import com.bot.dhxy.window.runtime.WindowRuntimeContext;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class XinshouRunnerAutoCombatStateTest {

    @Test
    void ordinaryTaskStartsArmedAndMaintainsWithoutBusinessPhasePermission() {
        RecordingPort port = new RecordingPort();
        XinshouRunnerAutoCombatState state = new XinshouRunnerAutoCombatState(port);
        WindowRuntimeContext context = context("window-1");

        state.begin(context, "XIULUO_V2", "run-1");

        assertTrue(state.isArmed(context, "run-1"));
        assertEquals(
                XinshouRunnerAutoCombatState.MaintenanceResult.COMPLETED,
                state.maintain(context, "run-1", 1L, 1_000L));
        assertEquals(1, port.calls.get());
    }

    @Test
    void maintainsExactlyOncePerCombatGeneration() {
        RecordingPort port = new RecordingPort();
        XinshouRunnerAutoCombatState state = new XinshouRunnerAutoCombatState(port);
        WindowRuntimeContext context = context("window-1");
        state.begin(context, "XINSHOU", "run-1");

        assertTrue(state.isArmed(context, "run-1"));
        assertTrue(state.arm(context));
        assertEquals(
                XinshouRunnerAutoCombatState.MaintenanceResult.COMPLETED,
                state.maintain(context, "run-1", 1L, 1_000L));
        assertEquals(
                XinshouRunnerAutoCombatState.MaintenanceResult.SKIPPED,
                state.maintain(context, "run-1", 1L, 2_000L));
        assertEquals(
                XinshouRunnerAutoCombatState.MaintenanceResult.COMPLETED,
                state.maintain(context, "run-1", 2L, 3_000L));
        assertEquals(2, port.calls.get());
    }

    @Test
    void wildBattleUsesTheSameGenericRunnerOwnership() {
        RecordingPort port = new RecordingPort();
        XinshouRunnerAutoCombatState state = new XinshouRunnerAutoCombatState(port);
        WindowRuntimeContext context = context("window-1");

        state.begin(context, "WILD_BATTLE", "run-1");

        assertTrue(state.isArmed(context, "run-1"));
        assertEquals(
                XinshouRunnerAutoCombatState.MaintenanceResult.COMPLETED,
                state.maintain(context, "run-1", 1L, 1_000L));
        assertEquals(1, port.calls.get());
    }

    @Test
    void failedLocalEvidenceRetriesAfterBoundWithoutCompletingGeneration() {
        RecordingPort port = new RecordingPort();
        port.results.add(result(XinshouCombatLocalMechanics.Status.CAPTURE_UNAVAILABLE));
        port.results.add(result(XinshouCombatLocalMechanics.Status.COMPLETED));
        XinshouRunnerAutoCombatState state = new XinshouRunnerAutoCombatState(port);
        WindowRuntimeContext context = context("window-1");
        state.begin(context, "XINSHOU", "run-1");
        assertEquals(
                XinshouRunnerAutoCombatState.MaintenanceResult.RETRY_LATER,
                state.maintain(context, "run-1", 1L, 1_000L));
        assertEquals(
                XinshouRunnerAutoCombatState.MaintenanceResult.SKIPPED,
                state.maintain(context, "run-1", 1L, 2_999L));
        assertEquals(
                XinshouRunnerAutoCombatState.MaintenanceResult.COMPLETED,
                state.maintain(context, "run-1", 1L, 3_000L));
        assertEquals(2, port.calls.get());
    }

    @Test
    void thrownMaintenanceReleasesGenerationAndRetriesAfterDeadline() {
        RecordingPort port = new RecordingPort();
        port.nextFailure = new IllegalStateException("first call failed");
        XinshouRunnerAutoCombatState state = new XinshouRunnerAutoCombatState(port);
        WindowRuntimeContext context = context("window-1");
        state.begin(context, "XINSHOU", "run-1");
        assertThrows(IllegalStateException.class,
                () -> state.maintain(context, "run-1", 1L, 1_000L));
        assertEquals(
                XinshouRunnerAutoCombatState.MaintenanceResult.SKIPPED,
                state.maintain(context, "run-1", 1L, 2_999L));
        assertEquals(
                XinshouRunnerAutoCombatState.MaintenanceResult.COMPLETED,
                state.maintain(context, "run-1", 1L, 3_000L));
        assertEquals(2, port.calls.get());
    }

    @Test
    void replacementRunMakesOldRunAndCloseStale() {
        RecordingPort port = new RecordingPort();
        XinshouRunnerAutoCombatState state = new XinshouRunnerAutoCombatState(port);
        WindowRuntimeContext context = context("window-1");
        state.begin(context, "XINSHOU", "run-1");
        state.begin(context, "XINSHOU", "run-2");

        assertTrue(state.isArmed(context, "run-2"));
        assertEquals(
                XinshouRunnerAutoCombatState.MaintenanceResult.SKIPPED,
                state.maintain(context, "run-1", 1L, 1_000L));
        state.close(context, "run-1");
        assertTrue(state.arm(context));
        assertTrue(state.isArmed(context, "run-2"));
        assertEquals(0, port.calls.get());
    }

    @Test
    void closingCurrentRunStopsFurtherMaintenance() {
        RecordingPort port = new RecordingPort();
        XinshouRunnerAutoCombatState state = new XinshouRunnerAutoCombatState(port);
        WindowRuntimeContext context = context("window-1");
        state.begin(context, "WUHUAN_V3", "run-1");

        state.close(context, "run-1");

        assertEquals(
                XinshouRunnerAutoCombatState.MaintenanceResult.SKIPPED,
                state.maintain(context, "run-1", 1L, 1_000L));
        assertEquals(0, port.calls.get());
    }

    private static WindowRuntimeContext context(String windowId) {
        return new WindowRuntimeContext(windowId, new GameContext());
    }

    private static XinshouCombatLocalMechanics.Result result(
            XinshouCombatLocalMechanics.Status status) {
        return new XinshouCombatLocalMechanics.Result(status, null, status.name());
    }

    private static final class RecordingPort
            implements XinshouRunnerAutoCombatState.MaintenancePort {
        private final AtomicInteger calls = new AtomicInteger();
        private final Deque<XinshouCombatLocalMechanics.Result> results =
                new ArrayDeque<>();
        private RuntimeException nextFailure;

        @Override
        public XinshouCombatLocalMechanics.Result maintain() {
            calls.incrementAndGet();
            if (nextFailure != null) {
                RuntimeException failure = nextFailure;
                nextFailure = null;
                throw failure;
            }
            return results.isEmpty()
                    ? result(XinshouCombatLocalMechanics.Status.COMPLETED)
                    : results.removeFirst();
        }
    }
}
