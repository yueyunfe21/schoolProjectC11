package com.bot.dhxy.window.observation;

import com.bot.dhxy.config.WindowIsolationProperties;
import com.bot.dhxy.core.GameContext;
import com.bot.dhxy.window.runtime.WindowRuntimeContext;
import com.bot.dhxy.window.runtime.WindowTaskContextHolder;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LocalMaintenanceBroadcastRunnerTest {

    @Test
    void patrolRunsOnlyWhileFreeAndStopsWithoutAnyCloudTransport() throws Exception {
        WindowRuntimeContext context = new WindowRuntimeContext("window-1", new GameContext());
        WindowTaskContextHolder holder = new WindowTaskContextHolder(new WindowIsolationProperties());
        AtomicInteger handled = new AtomicInteger();
        CountDownLatch freeTick = new CountDownLatch(1);
        LocalMaintenanceBroadcastRunner runner = new LocalMaintenanceBroadcastRunner(
                context,
                holder,
                () -> {
                    handled.incrementAndGet();
                    freeTick.countDown();
                    return true;
                },
                20L);

        context.getGameState().setCurrentActionState(GameContext.ActionState.IN_COMBAT);
        runner.start();
        try {
            Thread.sleep(80L);
            assertEquals(0, handled.get(), "in-combat windows do not run passive maintenance input");

            context.getGameState().setCurrentActionState(GameContext.ActionState.FREE);
            assertTrue(freeTick.await(1, TimeUnit.SECONDS), "a free window receives a local patrol tick");
            assertTrue(handled.get() >= 1);
        } finally {
            runner.requestStop();
            assertTrue(runner.awaitStopped(Duration.ofSeconds(1)));
        }
    }

    @Test
    void oneFailedTickDoesNotTerminateThePatrol() throws Exception {
        WindowRuntimeContext context = new WindowRuntimeContext("window-2", new GameContext());
        WindowTaskContextHolder holder = new WindowTaskContextHolder(new WindowIsolationProperties());
        AtomicInteger attempts = new AtomicInteger();
        CountDownLatch recoveredTick = new CountDownLatch(1);
        LocalMaintenanceBroadcastRunner runner = new LocalMaintenanceBroadcastRunner(
                context,
                holder,
                () -> {
                    if (attempts.incrementAndGet() == 1) {
                        throw new IllegalStateException("scripted capture failure");
                    }
                    recoveredTick.countDown();
                    return false;
                },
                20L);

        context.getGameState().setCurrentActionState(GameContext.ActionState.FREE);
        runner.start();
        try {
            assertTrue(recoveredTick.await(1, TimeUnit.SECONDS),
                    "a transient capture/matcher failure must not kill the resident patrol");
            assertTrue(attempts.get() >= 2);
        } finally {
            runner.requestStop();
            assertTrue(runner.awaitStopped(Duration.ofSeconds(1)));
        }
    }

    @Test
    void successfulClickCooldownIsIndependentPerWindow() throws Exception {
        WindowTaskContextHolder holder = new WindowTaskContextHolder(new WindowIsolationProperties());
        int memberCount = 4;
        WindowRuntimeContext[] contexts = new WindowRuntimeContext[memberCount];
        LocalMaintenanceBroadcastRunner[] runners = new LocalMaintenanceBroadcastRunner[memberCount];
        AtomicInteger handled = new AtomicInteger();
        CountDownLatch allMembersHandled = new CountDownLatch(memberCount);
        LocalMaintenanceBroadcastHandler sharedHandler = () -> {
            handled.incrementAndGet();
            allMembersHandled.countDown();
            return true;
        };
        for (int i = 0; i < memberCount; i++) {
            contexts[i] = new WindowRuntimeContext("member-" + i, new GameContext());
            contexts[i].getGameState().setCurrentActionState(GameContext.ActionState.FREE);
            runners[i] = new LocalMaintenanceBroadcastRunner(
                    contexts[i], holder, sharedHandler, 20L);
            runners[i].start();
        }
        try {
            assertTrue(allMembersHandled.await(1, TimeUnit.SECONDS),
                    "one member's successful click must not suppress another member window");
            Thread.sleep(100L);
            assertEquals(memberCount, handled.get(),
                    "each runner keeps its own cooldown and suppresses only its own immediate repeats");
        } finally {
            for (LocalMaintenanceBroadcastRunner runner : runners) {
                if (runner != null) {
                    runner.requestStop();
                    assertTrue(runner.awaitStopped(Duration.ofSeconds(1)));
                }
            }
        }
    }

    @Test
    void maintenanceStartsOnlyForAnAutoBattleOnlyEffectiveQueue() {
        assertTrue(SpringObservationRunnerFactory.isAutoBattleOnly("AUTO_BATTLE"));
        assertTrue(SpringObservationRunnerFactory.isAutoBattleOnly("AUTO_BATTLE,AUTO_BATTLE"));
        org.junit.jupiter.api.Assertions.assertFalse(
                SpringObservationRunnerFactory.isAutoBattleOnly("XIULUO_V2"));
        org.junit.jupiter.api.Assertions.assertFalse(
                SpringObservationRunnerFactory.isAutoBattleOnly("AUTO_BATTLE,WUHUAN_V3"));
        org.junit.jupiter.api.Assertions.assertFalse(
                SpringObservationRunnerFactory.isAutoBattleOnly(""));
    }
}
