package com.bot.dhxy.cloud.turn;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TurnLoopRegistryConcurrencyTest {

    @Test
    void registryAllowsOnlyOneLoopPerWindowAndRemovalPermanentlyRetiresIt() {
        Fixture fixture = fixture();
        WindowTurnLoop loop = fixture.registry.create(
                TurnContractFixtures.DEVICE_ID,
                TurnContractFixtures.WINDOW_ID,
                60_000L,
                () -> TurnContractFixtures.metadata(false));

        assertSame(loop, fixture.registry.find(TurnContractFixtures.WINDOW_ID).orElseThrow());
        assertEquals(1, fixture.registry.size());
        assertThrows(IllegalStateException.class, () -> fixture.registry.create(
                "other-device",
                TurnContractFixtures.WINDOW_ID,
                60_000L,
                () -> TurnContractFixtures.metadata(false)));

        WindowTurnLoop removed = fixture.registry.remove(TurnContractFixtures.WINDOW_ID);

        assertSame(loop, removed);
        assertEquals(0, fixture.registry.size());
        IllegalStateException retired = assertThrows(IllegalStateException.class, removed::start);
        assertTrue(retired.getMessage().contains("permanently retired"));
    }

    @Test
    void runningLoopMustStopBeforeRemoveAndCannotRestartAfterRemoval() throws Exception {
        Fixture fixture = fixture();
        WindowTurnLoop loop = fixture.registry.create(
                TurnContractFixtures.DEVICE_ID,
                TurnContractFixtures.WINDOW_ID,
                60_000L,
                () -> TurnContractFixtures.metadata(false));

        try {
            loop.start();
            assertTrue(fixture.client.awaitEntered(Duration.ofSeconds(3)));
            assertThrows(IllegalStateException.class,
                    () -> fixture.registry.remove(TurnContractFixtures.WINDOW_ID));
            assertEquals(1, fixture.registry.size());
        } finally {
            stopRemoveAndAssertRetired(fixture, loop);
        }
    }

    @Test
    void concurrentStartAndRemoveHaveOneWinnerAndNeverLeaveTwoUsableLoops() throws Exception {
        Fixture fixture = fixture();
        WindowTurnLoop original = fixture.registry.create(
                TurnContractFixtures.DEVICE_ID,
                TurnContractFixtures.WINDOW_ID,
                60_000L,
                () -> TurnContractFixtures.metadata(false));
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch go = new CountDownLatch(1);
        AtomicBoolean startSucceeded = new AtomicBoolean();
        AtomicBoolean removeSucceeded = new AtomicBoolean();
        AtomicReference<Throwable> startFailure = new AtomicReference<>();
        AtomicReference<Throwable> removeFailure = new AtomicReference<>();

        Thread starter = new Thread(() -> raceCall(
                ready, go, startSucceeded, startFailure, original::start), "turn-registry-start-race");
        Thread remover = new Thread(() -> raceCall(
                ready,
                go,
                removeSucceeded,
                removeFailure,
                () -> fixture.registry.remove(TurnContractFixtures.WINDOW_ID)),
                "turn-registry-remove-race");
        try {
            starter.start();
            remover.start();
            assertTrue(ready.await(3, TimeUnit.SECONDS));
            go.countDown();
            joinWithin(starter, Duration.ofSeconds(3));
            joinWithin(remover, Duration.ofSeconds(3));

            assertEquals(1,
                    (startSucceeded.get() ? 1 : 0) + (removeSucceeded.get() ? 1 : 0),
                    "the shared loop lifecycle monitor must choose exactly one winner");

            if (startSucceeded.get()) {
                assertTrue(removeFailure.get() instanceof IllegalStateException);
                assertNull(startFailure.get());
                assertEquals(1, fixture.registry.size());
                assertSame(original, fixture.registry.find(TurnContractFixtures.WINDOW_ID).orElseThrow());
                assertTrue(fixture.client.awaitEntered(Duration.ofSeconds(3)));
            } else {
                assertTrue(startFailure.get() instanceof IllegalStateException);
                assertNull(removeFailure.get());
                assertEquals(0, fixture.registry.size());
            }
        } finally {
            go.countDown();
            joinOrInterrupt(starter, Duration.ofSeconds(3));
            joinOrInterrupt(remover, Duration.ofSeconds(3));
            stopRemoveAndAssertRetired(fixture, original);
            assertFalse(starter.isAlive(), "owned start helper must not remain alive");
            assertFalse(remover.isAlive(), "owned remove helper must not remain alive");
        }
    }

    @Test
    void concurrentStopAndRemoveAfterExchangeAlwaysRetiresTheOldLoopWithoutSecondUsableLoop()
            throws Exception {
        Fixture fixture = fixture();
        WindowTurnLoop original = fixture.registry.create(
                TurnContractFixtures.DEVICE_ID,
                TurnContractFixtures.WINDOW_ID,
                60_000L,
                () -> TurnContractFixtures.metadata(false));
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch go = new CountDownLatch(1);
        AtomicBoolean stopSucceeded = new AtomicBoolean();
        AtomicBoolean removeSucceeded = new AtomicBoolean();
        AtomicReference<Throwable> stopFailure = new AtomicReference<>();
        AtomicReference<Throwable> removeFailure = new AtomicReference<>();
        Thread stopper = new Thread(() -> raceCall(
                ready, go, stopSucceeded, stopFailure, original::stop), "turn-registry-stop-race");
        Thread remover = new Thread(() -> raceCall(
                ready,
                go,
                removeSucceeded,
                removeFailure,
                () -> fixture.registry.remove(TurnContractFixtures.WINDOW_ID)),
                "turn-registry-remove-after-exchange-race");

        try {
            original.start();
            assertTrue(fixture.client.awaitEntered(Duration.ofSeconds(3)),
                    "the loop must enter the long-wait exchange before the stop/remove race");
            stopper.start();
            remover.start();
            assertTrue(ready.await(3, TimeUnit.SECONDS));
            go.countDown();
            joinWithin(stopper, Duration.ofSeconds(3));
            joinWithin(remover, Duration.ofSeconds(3));

            assertTrue(stopSucceeded.get(), "cooperative stop is always a legal winner");
            assertNull(stopFailure.get());
            if (removeSucceeded.get()) {
                assertNull(removeFailure.get());
                assertEquals(0, fixture.registry.size());
            } else {
                assertTrue(removeFailure.get() instanceof IllegalStateException,
                        "remove may lose only while the old loop is still running");
                assertSame(original, fixture.registry.find(TurnContractFixtures.WINDOW_ID).orElseThrow());
            }
        } finally {
            go.countDown();
            joinOrInterrupt(stopper, Duration.ofSeconds(3));
            joinOrInterrupt(remover, Duration.ofSeconds(3));
            stopRemoveAndAssertRetired(fixture, original);
            assertFalse(stopper.isAlive(), "owned stop helper must not remain alive");
            assertFalse(remover.isAlive(), "owned remove helper must not remain alive");
        }

        WindowTurnLoop replacement = fixture.registry.create(
                TurnContractFixtures.DEVICE_ID,
                TurnContractFixtures.WINDOW_ID,
                60_000L,
                () -> TurnContractFixtures.metadata(false));
        try {
            assertNotSame(original, replacement);
            assertThrows(IllegalStateException.class, original::start);
            assertSame(replacement, fixture.registry.find(TurnContractFixtures.WINDOW_ID).orElseThrow());
            assertEquals(1, fixture.registry.size(), "only the replacement may be usable");
        } finally {
            WindowTurnLoop removed = fixture.registry.remove(TurnContractFixtures.WINDOW_ID);
            assertSame(replacement, removed);
            assertEquals(0, fixture.registry.size());
            assertThrows(IllegalStateException.class, replacement::start);
        }
    }

    private static void raceCall(CountDownLatch ready,
                                 CountDownLatch go,
                                 AtomicBoolean succeeded,
                                 AtomicReference<Throwable> failure,
                                 ThrowingRunnable action) {
        ready.countDown();
        try {
            go.await();
            action.run();
            succeeded.set(true);
        } catch (Throwable thrown) {
            failure.set(thrown);
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

    private static void stopRemoveAndAssertRetired(Fixture fixture,
                                                   WindowTurnLoop original) throws InterruptedException {
        original.stop();
        boolean originalStopped = original.awaitStopped(Duration.ofSeconds(3));

        WindowTurnLoop registered = fixture.registry.find(TurnContractFixtures.WINDOW_ID).orElse(null);
        boolean registeredStopped = true;
        if (registered != null) {
            registered.stop();
            registeredStopped = registered.awaitStopped(Duration.ofSeconds(3));
            if (registeredStopped
                    && fixture.registry.find(TurnContractFixtures.WINDOW_ID).orElse(null) == registered) {
                fixture.registry.remove(TurnContractFixtures.WINDOW_ID);
            }
        }

        Thread worker = fixture.client.lastExchangeThread();
        if (worker != null) {
            worker.join(Duration.ofSeconds(3).toMillis());
        }
        assertTrue(originalStopped, "old loop must stop during unconditional cleanup");
        assertTrue(registeredStopped, "registered loop must stop during unconditional cleanup");
        assertFalse(original.isRunning());
        if (worker != null) {
            assertFalse(worker.isAlive(), "owned loop worker must not remain alive");
        }
        assertEquals(0, fixture.registry.size());
        IllegalStateException retired = assertThrows(IllegalStateException.class, original::start);
        assertTrue(retired.getMessage().contains("permanently retired"));
    }

    private static Fixture fixture() {
        TurnContractFixtures.ActionHarness actionHarness = TurnContractFixtures.actionHarness(true, false);
        TurnContractFixtures.BlockingTurnClient client = new TurnContractFixtures.BlockingTurnClient();
        TurnLoopFactory factory = new TurnLoopFactory(client, actionHarness.executor());
        return new Fixture(client, new TurnLoopRegistry(factory));
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }

    private record Fixture(TurnContractFixtures.BlockingTurnClient client, TurnLoopRegistry registry) {
    }
}
