package com.bot.dhxy.window.runtime;

import com.bot.dhxy.core.GameContext;
import com.bot.dhxy.model.navigation.PendingTransferChoiceMemory;
import com.bot.dhxy.model.navigation.PendingRouteOutcome;
import com.bot.dhxy.model.navigation.WorldMapRouteResultMode;
import com.bot.dhxy.model.dialog.DialogOperation;
import com.bot.dhxy.task.model.TaskType;
import com.bot.dhxy.window.model.WindowDialogInterest;
import com.bot.dhxy.window.model.WindowPathingIntent;
import com.bot.dhxy.window.model.WindowPathingIntentType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WindowRuntimeObserverClosureContractTest {

    @Test
    void targetMapGateAndInterestAreOneRetryableAtomicMutation() {
        WindowRuntimeContext runtime = new WindowRuntimeContext("window-40f", new GameContext());
        WindowDialogInterest interest = WindowDialogInterest.builder()
                .taskType(TaskType.WUBEI)
                .operations(java.util.List.of(DialogOperation.WUBEI_ENTER_BATTLE))
                .source("matched-map")
                .build();

        assertFalse(runtime.openOrdinaryEnterBattleTargetMapGateAndUpdateDialogInterest(
                interest, "before-gate", 10L));
        assertTrue(runtime.getDialogInterest().isEmpty());
        assertTrue(runtime.startOrdinaryEnterBattleTargetMapGate(
                TaskType.WUBEI, "tracker", "长安", 11L));
        assertTrue(runtime.openOrdinaryEnterBattleTargetMapGateAndUpdateDialogInterest(
                interest, "matched-map", 12L));
        assertTrue(runtime.getDialogInterest().orElseThrow()
                .supports(TaskType.WUBEI, DialogOperation.WUBEI_ENTER_BATTLE));
        assertTrue(runtime.openOrdinaryEnterBattleTargetMapGateAndUpdateDialogInterest(
                interest, "retry", 13L), "retry refreshes the same exact interest after an uncertain response");
        assertEquals(12L, runtime.getOrdinaryEnterBattleTargetMapOpenedAtMs(),
                "retry does not mint a second gate-open transition");
    }

    @Test
    void pendingSettlementsConsumeOnlyWhileExactPathingFenceIsCurrent() {
        WindowRuntimeContext runtime = new WindowRuntimeContext("window-40f", new GameContext());
        WindowPathingIntent intent = WindowPathingIntent.builder()
                .intentId("intent-1").source("route:one").targetMapName("长安")
                .type(WindowPathingIntentType.TARGETED).createdAtMs(1L).build();
        runtime.markPathingStarted(intent);
        PendingTransferChoiceMemory transfer = PendingTransferChoiceMemory.builder()
                .targetMap("长安").relativeX(10).relativeY(20).source("transfer").build();
        PendingRouteOutcome route = PendingRouteOutcome.builder()
                .targetMap("长安").relativeX(30).relativeY(40)
                .routeMode(WorldMapRouteResultMode.YELLOW_DESTINATION_MINI_MAP)
                .intentId("intent-1").source("world-map").build();
        runtime.updatePendingTransferChoiceMemory(transfer);
        runtime.updatePendingRouteOutcome(route);

        assertNull(runtime.consumePendingTransferChoiceMemoryIfPathingCurrent("stale", "route:one"));
        assertNull(runtime.consumePendingRouteOutcomeIfPathingCurrent("intent-1", "wrong-source"));
        assertSame(transfer, runtime.getPendingTransferChoiceMemory());
        assertSame(route, runtime.getPendingRouteOutcome());
        assertSame(transfer, runtime.consumePendingTransferChoiceMemoryIfPathingCurrent("intent-1", "route:one"));
        assertSame(route, runtime.consumePendingRouteOutcomeIfPathingCurrent("intent-1", "route:one"));
        assertNull(runtime.consumePendingTransferChoiceMemoryIfPathingCurrent("intent-1", "route:one"));
        assertNull(runtime.consumePendingRouteOutcomeIfPathingCurrent("intent-1", "route:one"));
    }

    @Test
    void combatPrefixClearMakesAuthoritativeSlotIdleAndDropsPendingMemory() {
        WindowRuntimeContext runtime = new WindowRuntimeContext("window-40f", new GameContext());
        WindowPathingIntent old = WindowPathingIntent.builder()
                .intentId("old-intent")
                .type(WindowPathingIntentType.UNTARGETED_TRACKER)
                .source("wubei:tracker-green-click:round-1")
                .createdAtMs(1L)
                .build();
        runtime.markPathingStarted(old);
        runtime.updatePendingTransferChoiceMemory(PendingTransferChoiceMemory.builder()
                .targetMap("长安").source("test").build());

        assertTrue(runtime.clearPathingSignalIfSourcePrefix(
                "wubei:tracker-green-click", "combat entered"));
        assertFalse(runtime.getPathingSnapshot().hasActiveIntent());
        assertTrue(runtime.getActivePathingIntent().isEmpty());
        assertNull(runtime.getPendingTransferChoiceMemory());
        assertFalse(runtime.clearPathingSignalIfSourcePrefix(
                "wubei:tracker-green-click", "duplicate combat"),
                "an already-cleared old intent cannot be rehydrated by a duplicate cleanup");
    }

    @Test
    void preBattleFencePublishesOnceAndClearRestartCreatesANewFence() {
        WindowRuntimeContext runtime = new WindowRuntimeContext("window-40f", new GameContext());
        long oldStart = System.currentTimeMillis() - 300_100L;
        assertTrue(runtime.startOrdinaryPreBattleTimer(
                TaskType.WUBEI, "wubei:test", "黄袍怪", oldStart));
        assertTrue(runtime.markOrdinaryPreBattleTimeoutPublished(System.currentTimeMillis()));
        assertFalse(runtime.markOrdinaryPreBattleTimeoutPublished(System.currentTimeMillis()),
                "duplicate publishers lose the same timer's CAS fence");

        runtime.clearOrdinaryPreBattleTimer("test restart");
        assertEquals(0L, runtime.getOrdinaryPreBattleStartedAtMs());
        assertEquals(0L, runtime.getOrdinaryPreBattleTimeoutPublishedAtMs());
        assertFalse(runtime.markOrdinaryPreBattleTimeoutPublished(System.currentTimeMillis()),
                "a missing timer cannot publish truth");
        assertTrue(runtime.startOrdinaryPreBattleTimer(
                TaskType.WUBEI, "wubei:test-2", "奎星", oldStart + 1L));
        assertTrue(runtime.markOrdinaryPreBattleTimeoutPublished(System.currentTimeMillis()),
                "a restarted timer owns a fresh single-publish fence");
    }
}
