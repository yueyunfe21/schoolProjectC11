package com.bot.dhxy.ui;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UiRefreshExecutorTest {

    @Test
    void skipsOverlappingRefreshAndAcceptsNextRoundAfterCompletion() throws Exception {
        UiRefreshExecutor executor = new UiRefreshExecutor();
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        try {
            assertTrue(executor.submit(() -> {
                started.countDown();
                await(release);
            }));
            assertTrue(started.await(2, TimeUnit.SECONDS));
            assertFalse(executor.submit(() -> {
            }));

            release.countDown();
            assertTrue(awaitAccepted(executor));
        } finally {
            release.countDown();
            executor.shutdown();
        }
    }

    @Test
    void shutdownRejectsFutureRefreshesAndInterruptsCurrentWork() throws Exception {
        UiRefreshExecutor executor = new UiRefreshExecutor();
        CountDownLatch started = new CountDownLatch(1);
        AtomicBoolean interrupted = new AtomicBoolean();
        assertTrue(executor.submit(() -> {
            started.countDown();
            try {
                Thread.sleep(30_000L);
            } catch (InterruptedException expected) {
                interrupted.set(true);
                Thread.currentThread().interrupt();
            }
        }));
        assertTrue(started.await(2, TimeUnit.SECONDS));

        executor.shutdown();

        assertTrue(executor.isShutdown());
        assertFalse(executor.submit(() -> {
        }));
        assertTrue(awaitTrue(interrupted));
    }

    private static boolean awaitAccepted(UiRefreshExecutor executor) throws InterruptedException {
        long deadline = System.nanoTime() + Duration.ofSeconds(2).toNanos();
        while (System.nanoTime() < deadline) {
            if (executor.submit(() -> {
            })) {
                return true;
            }
            Thread.sleep(10L);
        }
        return false;
    }

    private static boolean awaitTrue(AtomicBoolean value) throws InterruptedException {
        long deadline = System.nanoTime() + Duration.ofSeconds(2).toNanos();
        while (System.nanoTime() < deadline) {
            if (value.get()) {
                return true;
            }
            Thread.sleep(10L);
        }
        return value.get();
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        }
    }
}
