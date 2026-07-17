package com.bot.dhxy.cloud.turn;

import com.bot.dhxy.core.GameContext;
import com.bot.dhxy.window.runtime.WindowRuntimeContext;
import org.junit.jupiter.api.Test;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TurnModeGuardContractTest {

    @Test
    void localStartHoldsTheModeMonitorThroughSubmissionSoRemoteCannotAlsoWin() throws Exception {
        Fixture fixture = fixture(false, false);
        CountDownLatch localSupplierEntered = new CountDownLatch(1);
        CountDownLatch releaseLocalSupplier = new CountDownLatch(1);
        CountDownLatch remoteAttempted = new CountDownLatch(1);
        AtomicInteger localSupplierCalls = new AtomicInteger();
        AtomicReference<String> localResult = new AtomicReference<>();
        AtomicReference<Throwable> localFailure = new AtomicReference<>();
        AtomicReference<Throwable> remoteFailure = new AtomicReference<>();

        Thread local = new Thread(() -> {
            try {
                localResult.set(fixture.guard.startLocal(List.of(TurnContractFixtures.WINDOW_ID), () -> {
                    localSupplierCalls.incrementAndGet();
                    localSupplierEntered.countDown();
                    awaitUnchecked(releaseLocalSupplier);
                    fixture.runner.setRunning(true);
                    return "local-started";
                }));
            } catch (Throwable thrown) {
                localFailure.set(thrown);
            }
        }, "turn-mode-local-winner");
        Thread remote = new Thread(() -> {
            remoteAttempted.countDown();
            try {
                fixture.guard.startRemote(
                        TurnContractFixtures.DEVICE_ID,
                        TurnContractFixtures.WINDOW_ID,
                        () -> TurnContractFixtures.metadata(false));
            } catch (Throwable thrown) {
                remoteFailure.set(thrown);
            }
        }, "turn-mode-remote-contender");

        try {
            local.start();
            assertTrue(localSupplierEntered.await(3, TimeUnit.SECONDS));
            remote.start();
            assertTrue(remoteAttempted.await(3, TimeUnit.SECONDS));
            awaitBlockedOn(remote, local, Duration.ofSeconds(3));
            assertEquals(Thread.State.BLOCKED, remote.getState(),
                    "remote must be contending for the exact monitor held through local submission");

            releaseLocalSupplier.countDown();
            joinWithin(local, Duration.ofSeconds(3));
            joinWithin(remote, Duration.ofSeconds(3));

            assertEquals(1, localSupplierCalls.get());
            assertEquals("local-started", localResult.get());
            assertNull(localFailure.get());
            assertTrue(remoteFailure.get() instanceof TurnModeGuard.ModeConflictException);
            assertEquals(TurnContractFixtures.WINDOW_ID,
                    ((TurnModeGuard.ModeConflictException) remoteFailure.get()).windowId());
            assertEquals(0, fixture.registry.size());
        } finally {
            releaseLocalSupplier.countDown();
            joinOrInterrupt(local, Duration.ofSeconds(3));
            joinOrInterrupt(remote, Duration.ofSeconds(3));
            fixture.runner.setRunning(false);
            stopAndRemoveRegisteredRemote(fixture);
            assertFalse(local.isAlive(), "owned local helper thread must not remain alive");
            assertFalse(remote.isAlive(), "owned remote helper thread must not remain alive");
        }
    }

    @Test
    void registeredRemoteLoopMakesLaterLocalSubmissionLoseWithoutCallingItsSupplier() throws Exception {
        Fixture fixture = fixture(false, false);
        WindowTurnLoop remote = null;
        try {
            remote = fixture.guard.startRemote(
                    TurnContractFixtures.DEVICE_ID,
                    TurnContractFixtures.WINDOW_ID,
                    () -> TurnContractFixtures.metadata(false));
            assertTrue(fixture.client.awaitEntered(Duration.ofSeconds(3)));
            AtomicInteger localSupplierCalls = new AtomicInteger();

            TurnModeGuard.ModeConflictException conflict = assertThrows(
                    TurnModeGuard.ModeConflictException.class,
                    () -> fixture.guard.startLocal(List.of(TurnContractFixtures.WINDOW_ID), () -> {
                        localSupplierCalls.incrementAndGet();
                        fixture.runner.setRunning(true);
                        return "should-not-run";
                    }));

            assertEquals(TurnContractFixtures.WINDOW_ID, conflict.windowId());
            assertEquals(0, localSupplierCalls.get(), "the losing local supplier must never run");
            assertSame(remote, fixture.registry.find(TurnContractFixtures.WINDOW_ID).orElseThrow());
        } finally {
            stopAndRemoveRegisteredRemote(fixture);
            if (remote != null) {
                assertFalse(remote.isRunning(), "owned remote loop must not remain running");
            }
        }
    }

    @Test
    void remoteStartRequiresTheExactRegisteredNonShutdownLocalRunnerBeforeCreatingLoop() {
        Fixture missing = fixture(false, false);
        missing.manager.removeRunner(TurnContractFixtures.WINDOW_ID);

        TurnModeGuard.ModeConflictException absent = assertThrows(
                TurnModeGuard.ModeConflictException.class,
                () -> missing.guard.startRemote(
                        TurnContractFixtures.DEVICE_ID,
                        TurnContractFixtures.WINDOW_ID,
                        () -> TurnContractFixtures.metadata(false)));
        assertTrue(absent.getMessage().contains("no local runner is registered"));
        assertEquals(0, missing.registry.size());

        Fixture shutdown = fixture(false, true);
        TurnModeGuard.ModeConflictException closed = assertThrows(
                TurnModeGuard.ModeConflictException.class,
                () -> shutdown.guard.startRemote(
                        TurnContractFixtures.DEVICE_ID,
                        TurnContractFixtures.WINDOW_ID,
                        () -> TurnContractFixtures.metadata(false)));
        assertTrue(closed.getMessage().contains("local runner is shut down"));
        assertEquals(0, shutdown.registry.size());
    }

    private static Fixture fixture(boolean running, boolean shutdown) {
        TurnContractFixtures.TestTaskManager manager = new TurnContractFixtures.TestTaskManager();
        WindowRuntimeContext context = new WindowRuntimeContext(
                TurnContractFixtures.WINDOW_ID, new GameContext());
        context.setNativeBinding(TurnContractFixtures.binding());
        TurnContractFixtures.BareWindowTaskRunner runner = TurnContractFixtures.bareRunner(
                context, running, shutdown);
        manager.putRunner(TurnContractFixtures.WINDOW_ID, runner);

        TurnContractFixtures.ActionHarness actionHarness = TurnContractFixtures.actionHarness(true, false);
        TurnContractFixtures.BlockingTurnClient client = new TurnContractFixtures.BlockingTurnClient();
        TurnLoopRegistry registry = new TurnLoopRegistry(new TurnLoopFactory(client, actionHarness.executor()));
        TurnModeGuard guard = new TurnModeGuard(manager, registry, 60_000L);
        return new Fixture(manager, runner, client, registry, guard);
    }

    private static void awaitUnchecked(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new AssertionError("test coordination interrupted", interrupted);
        }
    }

    private static void awaitBlockedOn(Thread contender,
                                       Thread owner,
                                       Duration timeout) {
        ThreadMXBean threads = ManagementFactory.getThreadMXBean();
        long deadline = System.nanoTime() + timeout.toNanos();
        Thread.State observed = contender.getState();
        long observedOwnerId = -1L;
        while (true) {
            ThreadInfo info = threads.getThreadInfo(contender.getId());
            if (info != null) {
                observed = info.getThreadState();
                observedOwnerId = info.getLockOwnerId();
                if (observed == Thread.State.BLOCKED && observedOwnerId == owner.getId()) {
                    return;
                }
            }
            if (!contender.isAlive()) {
                throw new AssertionError(
                        "contender terminated before blocking on the owned monitor; finalState=" + observed);
            }
            if (System.nanoTime() >= deadline) {
                throw new AssertionError(
                        "contender did not block on the owned monitor before timeout; observed="
                                + observed + ", lockOwnerId=" + observedOwnerId
                                + ", expectedOwnerId=" + owner.getId());
            }
            LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(1));
        }
    }

    private static void joinWithin(Thread thread, Duration timeout) throws InterruptedException {
        thread.join(timeout.toMillis());
        assertFalse(thread.isAlive(), "helper thread did not finish before timeout: " + thread.getName());
    }

    private static void joinOrInterrupt(Thread thread, Duration timeout) throws InterruptedException {
        thread.join(timeout.toMillis());
        if (thread.isAlive()) {
            thread.interrupt();
            thread.join(timeout.toMillis());
        }
    }

    private static void stopAndRemoveRegisteredRemote(Fixture fixture) throws InterruptedException {
        WindowTurnLoop registered = fixture.registry.find(TurnContractFixtures.WINDOW_ID).orElse(null);
        boolean stopped = true;
        if (registered != null) {
            registered.stop();
            stopped = registered.awaitStopped(Duration.ofSeconds(3));
            if (stopped
                    && fixture.registry.find(TurnContractFixtures.WINDOW_ID).orElse(null) == registered) {
                fixture.registry.remove(TurnContractFixtures.WINDOW_ID);
            }
        }
        Thread worker = fixture.client.lastExchangeThread();
        if (worker != null) {
            worker.join(Duration.ofSeconds(3).toMillis());
        }
        assertTrue(stopped, "owned remote loop must stop during unconditional cleanup");
        if (registered != null) {
            assertFalse(registered.isRunning());
        }
        if (worker != null) {
            assertFalse(worker.isAlive(), "owned remote loop worker must not remain alive");
        }
        assertEquals(0, fixture.registry.size());
    }

    private record Fixture(TurnContractFixtures.TestTaskManager manager,
                           TurnContractFixtures.BareWindowTaskRunner runner,
                           TurnContractFixtures.BlockingTurnClient client,
                           TurnLoopRegistry registry,
                           TurnModeGuard guard) {
    }
}
