package com.bot.dhxy.cloud.turn.local;

import com.bot.dhxy.config.WindowIsolationProperties;
import com.bot.dhxy.core.GameContext;
import com.bot.dhxy.driver.BoundWindowKeyboardService;
import com.bot.dhxy.input.GlobalInputLock;
import com.bot.dhxy.input.InputProvider;
import com.bot.dhxy.input.InputSequences;
import com.bot.dhxy.input.WindowAwareInputCoordinator;
import com.bot.dhxy.input.action.InputActionDeadLetter;
import com.bot.dhxy.input.action.InputActionQueue;
import com.bot.dhxy.input.action.InputActionWorker;
import com.bot.dhxy.runner.context.TaskExecutionContextHolder;
import com.bot.dhxy.window.model.WindowNativeBinding;
import com.bot.dhxy.window.runtime.WindowRuntimeContext;
import com.bot.dhxy.window.runtime.WindowTaskContextHolder;

import java.lang.reflect.Proxy;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/** Real queue/worker harness with a recording provider; it never reaches physical input. */
final class FrozenExactInputHarness implements AutoCloseable {

    final WindowTaskContextHolder contextHolder =
            new WindowTaskContextHolder(new WindowIsolationProperties());
    final InputSequences inputSequences;
    final InputProvider inputProvider;
    final AtomicInteger moveCalls = new AtomicInteger();
    final AtomicInteger clickCalls = new AtomicInteger();
    volatile int moveX;
    volatile int moveY;
    volatile int clickX;
    volatile int clickY;
    volatile int clickDelayMs;
    volatile boolean failClick;
    volatile Runnable afterClick;

    private final InputActionQueue queue;
    private CountDownLatch blockerRelease;
    private Thread blockerSubmitter;

    FrozenExactInputHarness() {
        inputProvider = (InputProvider) Proxy.newProxyInstance(
                InputProvider.class.getClassLoader(),
                new Class<?>[]{InputProvider.class},
                (proxy, method, args) -> {
                    if ("moveMouse".equals(method.getName())) {
                        moveCalls.incrementAndGet();
                        moveX = (int) args[0];
                        moveY = (int) args[1];
                    } else if ("clickLeft".equals(method.getName())) {
                        clickCalls.incrementAndGet();
                        clickX = (int) args[0];
                        clickY = (int) args[1];
                        clickDelayMs = (int) args[2];
                        if (failClick) {
                            throw new IllegalStateException("synthetic click failure");
                        }
                        Runnable hook = afterClick;
                        if (hook != null) {
                            hook.run();
                        }
                    }
                    return null;
                });
        queue = new InputActionQueue(
                contextHolder,
                null,
                new TaskExecutionContextHolder());
        inputSequences = new InputSequences(queue);
        WindowAwareInputCoordinator coordinator = new WindowAwareInputCoordinator(
                new GlobalInputLock(),
                contextHolder,
                null,
                new WindowIsolationProperties(),
                null,
                null);
        InputActionWorker worker = new InputActionWorker(
                queue,
                new InputActionDeadLetter(),
                inputProvider,
                coordinator,
                contextHolder,
                new BoundWindowKeyboardService(null, null, null, null));
        worker.start();
    }

    WindowRuntimeContext newContext(String windowId, WindowNativeBinding binding) {
        WindowRuntimeContext context = new WindowRuntimeContext(windowId, new GameContext());
        context.setNativeBinding(binding);
        return context;
    }

    void blockWorker() {
        CountDownLatch entered = new CountDownLatch(1);
        blockerRelease = new CountDownLatch(1);
        WindowRuntimeContext blockerContext = newContext(
                "window-blocker",
                new WindowNativeBinding(
                        "99999", "blocker", "class", 99L, 5, 10, 800, 600));
        WindowNativeBinding blockerBinding = blockerContext.getNativeBinding();
        blockerSubmitter = new Thread(
                () -> inputSequences.submitFrozenExactWindowExclusiveAndWait(
                        "test:frozen-blocker",
                        blockerContext,
                        blockerBinding,
                        () -> {
                            entered.countDown();
                            return await(blockerRelease);
                        }),
                "test-frozen-input-blocker");
        blockerSubmitter.setDaemon(true);
        blockerSubmitter.start();
        if (!await(entered)) {
            throw new AssertionError("input worker did not enter blocker callback");
        }
    }

    void awaitQueuedRequest() {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(3);
        while (queue.size() == 0 && System.nanoTime() < deadline) {
            try {
                Thread.sleep(5L);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                throw new AssertionError("interrupted while waiting for queued request", interrupted);
            }
        }
        if (queue.size() == 0) {
            throw new AssertionError("target exact-window request was not queued");
        }
    }

    void releaseWorker() {
        if (blockerRelease != null) {
            blockerRelease.countDown();
        }
        if (blockerSubmitter != null) {
            try {
                blockerSubmitter.join(TimeUnit.SECONDS.toMillis(3));
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                throw new AssertionError("interrupted while releasing input worker", interrupted);
            }
        }
    }

    @Override
    public void close() {
        releaseWorker();
    }

    private static boolean await(CountDownLatch latch) {
        try {
            return latch.await(3, TimeUnit.SECONDS);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
}
