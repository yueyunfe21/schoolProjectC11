package com.bot.dhxy.cloud.turn.local;

import com.bot.dhxy.cloud.turn.LocalServiceExecution;
import com.bot.dhxy.cloud.turn.protocol.TurnLocalOperation;
import com.bot.dhxy.cloud.turn.protocol.TurnLocalServiceCall;
import com.bot.dhxy.cloud.turn.protocol.TurnStepResult;
import com.bot.dhxy.cloud.turn.protocol.TurnXinshouTrackerChainArguments;
import com.bot.dhxy.cloud.turn.protocol.TurnXinshouTrackerLink;
import com.bot.dhxy.core.GameContext;
import com.bot.dhxy.window.model.WindowNativeBinding;
import com.bot.dhxy.window.model.WindowPathingIntent;
import com.bot.dhxy.window.model.WindowPathingIntentType;
import com.bot.dhxy.window.runtime.WindowRuntimeContext;
import com.bot.dhxy.window.runtime.WindowTaskContextHolder;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class XinshouTrackerLinkChainLocalOperationExecutorTest {

    @Test
    void oneCloudActionExecutesExactlyOneAtomicClick() {
        try (Fixture fixture = new Fixture()) {
            LocalServiceExecution result = fixture.execute(321, 654);

            assertEquals(TurnStepResult.Status.COMPLETED, result.status());
            assertEquals("XINSHOU_TRACKER_LINK_DISPATCHED", result.code());
            assertEquals(1, fixture.input.moveCalls.get());
            assertEquals(1, fixture.input.clickCalls.get());
            assertEquals(321, fixture.input.moveX);
            assertEquals(654, fixture.input.moveY);
            assertEquals(321, fixture.input.clickX);
            assertEquals(654, fixture.input.clickY);
            assertEquals(250, fixture.input.clickDelayMs);
            assertEquals(WindowPathingIntentType.UNTARGETED_TRACKER,
                    fixture.context.getActivePathingIntent().orElseThrow().getType());
        }
    }

    @Test
    void translatedClickTracksWindowMovementWithoutChangingSourceSize() {
        try (Fixture fixture = new Fixture()) {
            fixture.bindAt(300, 400, 1024, 768);

            LocalServiceExecution result = fixture.execute(
                    321, 654,
                    100, 200, 1024, 768);

            assertEquals(TurnStepResult.Status.COMPLETED, result.status());
            assertEquals(521, fixture.input.clickX);
            assertEquals(854, fixture.input.clickY);
            assertEquals(1, fixture.input.moveCalls.get());
            assertEquals(1, fixture.input.clickCalls.get());
            assertTrue(fixture.context.getActivePathingIntent().isPresent());
        }
    }

    @Test
    void negativeDesktopCoordinatesAreTranslatedWithinTheExactWindow() {
        try (Fixture fixture = new Fixture()) {
            fixture.bindAt(-900, 200, 1024, 768);

            LocalServiceExecution result = fixture.execute(
                    -1000, 300,
                    -1200, 100, 1024, 768);

            assertEquals(TurnStepResult.Status.COMPLETED, result.status());
            assertEquals(-700, fixture.input.clickX);
            assertEquals(400, fixture.input.clickY);
            assertEquals(1, fixture.input.clickCalls.get());
        }
    }

    @Test
    void changedSizeFailsClosedWithoutInputOrPathingIntent() {
        try (Fixture fixture = new Fixture()) {
            fixture.bindAt(300, 400, 1000, 768);

            LocalServiceExecution result = fixture.execute(
                    321, 654,
                    100, 200, 1024, 768);

            assertEquals(TurnStepResult.Status.FAILED, result.status());
            assertEquals("XINSHOU_TRACKER_CHAIN_WINDOW_SIZE_CHANGED", result.code());
            assertEquals(0, fixture.input.moveCalls.get());
            assertEquals(0, fixture.input.clickCalls.get());
            assertTrue(fixture.context.getActivePathingIntent().isEmpty());
        }
    }

    @Test
    void missingBindingAndOutOfSourcePointBothFailClosed() {
        try (Fixture missing = new Fixture();
             Fixture outside = new Fixture()) {
            missing.context.setNativeBinding(WindowNativeBinding.empty());

            LocalServiceExecution noWindow = missing.execute(321, 654);

            assertEquals(TurnStepResult.Status.FAILED, noWindow.status());
            assertEquals("XINSHOU_TRACKER_CHAIN_WINDOW_UNAVAILABLE", noWindow.code());
            assertEquals(0, missing.input.clickCalls.get());
            assertTrue(missing.context.getActivePathingIntent().isEmpty());

            LocalServiceExecution invalidPoint = outside.execute(
                    1024, 100,
                    0, 0, 1024, 768);

            assertEquals(TurnStepResult.Status.FAILED, invalidPoint.status());
            assertEquals("INVALID_XINSHOU_TRACKER_CHAIN", invalidPoint.code());
            assertEquals(0, outside.input.clickCalls.get());
            assertTrue(outside.context.getActivePathingIntent().isEmpty());
        }
    }

    @Test
    void failedInputIsReportedWithoutAnyLocalRetry() {
        try (Fixture fixture = new Fixture()) {
            fixture.input.failClick = true;

            LocalServiceExecution result = fixture.execute(123, 456);

            assertEquals(TurnStepResult.Status.FAILED, result.status());
            assertEquals("XINSHOU_TRACKER_LINK_INPUT_FAILED", result.code());
            assertEquals(1, fixture.input.clickCalls.get(),
                    "Client must not retry a Cloud business action on its own");
            assertTrue(fixture.context.getActivePathingIntent().isEmpty(),
                    "a click that never executed cannot leave a false pathing intent");
        }
    }

    @Test
    void queueWaitDoesNotAdvancePathingIntentAge() throws Exception {
        try (Fixture fixture = new Fixture()) {
            fixture.input.blockWorker();
            CompletableFuture<LocalServiceExecution> pending = CompletableFuture.supplyAsync(
                    () -> fixture.execute(321, 654));
            fixture.input.awaitQueuedRequest();
            long queuedClockMs = System.currentTimeMillis();
            while (System.currentTimeMillis() <= queuedClockMs) {
                Thread.onSpinWait();
            }
            long workerReleasedAtMs = System.currentTimeMillis();

            fixture.input.releaseWorker();
            LocalServiceExecution result = pending.get(3, TimeUnit.SECONDS);
            long createdAtMs = fixture.context.getActivePathingIntent()
                    .orElseThrow()
                    .getCreatedAtMs();

            assertEquals(TurnStepResult.Status.COMPLETED, result.status());
            assertTrue(createdAtMs >= workerReleasedAtMs,
                    "intent age must start after the queued click is allowed to run");
        }
    }

    @Test
    void bindingReplacementAfterClickCannotReceiveTheOldPathingIntent() throws Exception {
        RecordingWindowRuntimeContext context = new RecordingWindowRuntimeContext();
        try (Fixture fixture = new Fixture(context)) {
            WindowNativeBinding clickedBinding = fixture.context.getNativeBinding();
            CountDownLatch clickCompleted = new CountDownLatch(1);
            CountDownLatch driftAttempted = new CountDownLatch(1);
            Thread bindingDrift = new Thread(() -> {
                await(clickCompleted);
                driftAttempted.countDown();
                fixture.context.setNativeBinding(new WindowNativeBinding(
                        "54321", "replacement", "class", 88L,
                        20, 30, 1024, 768));
            }, "test-binding-drift-after-click");
            bindingDrift.setDaemon(true);
            fixture.input.afterClick = () -> {
                clickCompleted.countDown();
                await(driftAttempted);
            };
            bindingDrift.start();

            LocalServiceExecution result = fixture.execute(321, 654);
            bindingDrift.join(TimeUnit.SECONDS.toMillis(3));

            assertEquals(TurnStepResult.Status.COMPLETED, result.status());
            assertTrue(context.monitorHeldAtMark,
                    "pathing registration must execute under the exact context monitor");
            assertTrue(context.bindingAtMark == clickedBinding,
                    "pathing registration must still witness the clicked binding object");
            assertEquals("54321", fixture.context.getNativeBinding().getNativeHandle());
            assertTrue(fixture.context.getActivePathingIntent().isEmpty(),
                    "hard binding replacement must not inherit the old window's click intent");
        }
    }

    @Test
    void staleGenerationAfterEnqueueNeverClicksOrRegistersPathing() throws Exception {
        try (Fixture fixture = new Fixture()) {
            fixture.input.blockWorker();
            CompletableFuture<LocalServiceExecution> pending = CompletableFuture.supplyAsync(
                    () -> fixture.execute(321, 654));
            fixture.input.awaitQueuedRequest();

            fixture.bindAt(1, 0, 1024, 768);
            fixture.input.releaseWorker();
            LocalServiceExecution result = pending.get(3, TimeUnit.SECONDS);

            assertEquals(TurnStepResult.Status.FAILED, result.status());
            assertEquals("XINSHOU_TRACKER_LINK_STALE", result.code());
            assertEquals(0, fixture.input.moveCalls.get());
            assertEquals(0, fixture.input.clickCalls.get());
            assertTrue(fixture.context.getActivePathingIntent().isEmpty());
        }
    }

    private static final class Fixture implements AutoCloseable {
        private final FrozenExactInputHarness input = new FrozenExactInputHarness();
        private final WindowTaskContextHolder contextHolder = input.contextHolder;
        private final WindowRuntimeContext context;
        private final XinshouTrackerLinkChainLocalOperationExecutor executor;

        private Fixture() {
            this(new WindowRuntimeContext("window-1", new GameContext()));
        }

        private Fixture(WindowRuntimeContext context) {
            this.context = context;
            this.context.setNativeBinding(new WindowNativeBinding(
                    "12345", "game", "class", 77L, 0, 0, 1024, 768));
            executor = new XinshouTrackerLinkChainLocalOperationExecutor(
                    input.inputProvider, input.inputSequences, contextHolder);
        }

        private void bindAt(int left, int top, int width, int height) {
            context.setNativeBinding(new WindowNativeBinding(
                    "12345", "game", "class", 77L, left, top, width, height));
        }

        private LocalServiceExecution execute(int x, int y) {
            return execute(x, y, 0, 0, 1024, 768);
        }

        private LocalServiceExecution execute(
                int x,
                int y,
                int sourceLeft,
                int sourceTop,
                int sourceWidth,
                int sourceHeight) {
            TurnLocalServiceCall call = new TurnLocalServiceCall(
                    TurnLocalOperation.XINSHOU_TRACKER_LINK_CHAIN,
                    new TurnXinshouTrackerChainArguments(
                            "xinshou:prepared-action",
                            List.of(new TurnXinshouTrackerLink(x, y)),
                            sourceLeft,
                            sourceTop,
                            sourceWidth,
                            sourceHeight));
            return contextHolder.callWith(context, () -> executor.execute(call));
        }

        @Override
        public void close() {
            input.close();
        }
    }

    private static final class RecordingWindowRuntimeContext extends WindowRuntimeContext {
        private volatile boolean monitorHeldAtMark;
        private volatile WindowNativeBinding bindingAtMark;

        private RecordingWindowRuntimeContext() {
            super("window-1", new GameContext());
        }

        @Override
        public void markPathingStarted(WindowPathingIntent intent) {
            monitorHeldAtMark = Thread.holdsLock(this);
            bindingAtMark = getNativeBinding();
            super.markPathingStarted(intent);
        }
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(3, TimeUnit.SECONDS)) {
                throw new AssertionError("timed out waiting for test coordination");
            }
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new AssertionError("interrupted while coordinating test", interrupted);
        }
    }
}
