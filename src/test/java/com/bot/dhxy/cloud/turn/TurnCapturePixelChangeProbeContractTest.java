package com.bot.dhxy.cloud.turn;

import com.bot.dhxy.cloud.turn.protocol.TurnCaptureSpec;
import com.bot.dhxy.cloud.turn.protocol.TurnFramePurpose;
import com.bot.dhxy.cloud.turn.protocol.TurnRegion;
import com.bot.dhxy.cloud.turn.protocol.TurnAction;
import com.bot.dhxy.cloud.turn.protocol.TurnStep;
import com.bot.dhxy.cloud.turn.protocol.TurnStepType;
import com.bot.dhxy.cloud.turn.protocol.TurnWindowRect;
import com.bot.dhxy.config.WindowIsolationProperties;
import com.bot.dhxy.core.GameContext;
import com.bot.dhxy.driver.BoundWindowCaptureService;
import com.bot.dhxy.driver.BoundWindowKeyboardService;
import com.bot.dhxy.input.GlobalInputLock;
import com.bot.dhxy.input.InputProvider;
import com.bot.dhxy.input.InputSequences;
import com.bot.dhxy.input.WindowAwareInputCoordinator;
import com.bot.dhxy.input.action.InputActionDeadLetter;
import com.bot.dhxy.input.action.InputActionExecutionResult;
import com.bot.dhxy.input.action.InputActionQueue;
import com.bot.dhxy.input.action.InputActionWorker;
import com.bot.dhxy.runner.context.TaskExecutionContext;
import com.bot.dhxy.runner.context.TaskExecutionContextHolder;
import com.bot.dhxy.runner.stop.TaskStopToken;
import com.bot.dhxy.window.diagnostics.WindowInteractionMetricsService;
import com.bot.dhxy.window.execution.MultiWindowTaskManager;
import com.bot.dhxy.window.execution.RunningTaskHandle;
import com.bot.dhxy.window.execution.WindowTaskRunner;
import com.bot.dhxy.window.interaction.WindowFocusService;
import com.bot.dhxy.window.model.WindowNativeBinding;
import com.bot.dhxy.window.model.WindowRuntimeStatus;
import com.bot.dhxy.window.runtime.WindowNativeBindingRefreshService;
import com.bot.dhxy.window.runtime.WindowRuntimeContext;
import com.bot.dhxy.window.runtime.WindowTaskContextHolder;
import org.junit.jupiter.api.Test;
import sun.misc.Unsafe;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.LongPredicate;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TurnCapturePixelChangeProbeContractTest {

    private static final Unsafe UNSAFE = findUnsafe();
    private static final TurnWindowRect WINDOW_RECT = new TurnWindowRect(100, 200, 20, 20);
    private static final TurnRegion ROI = new TurnRegion(104, 205, 2, 2);
    private static final TurnCaptureSpec.PixelChangeProbe PROBE =
            new TurnCaptureSpec.PixelChangeProbe(105, 206, 80, 280, 100, 0.05D);

    @Test
    void unchangedProbeUsesOneExclusiveCallbackAndReturnsOnlyTheExactAfterPng() throws Exception {
        Harness harness = new Harness(0xff112233, 0xff112233);

        TurnCaptureStepExecutor.Execution execution = harness.execute();

        assertEquals(TurnCaptureStepExecutor.Status.COMPLETED, execution.status());
        assertEquals(TurnCaptureStepExecutor.Code.PIXELS_UNCHANGED, execution.code());
        assertCompletedFrame(execution, 0xff112233);
        assertExactSuccessfulTrace(harness);
    }

    @Test
    void changedProbePreservesUnscaledAfterPixelsAndNeverClicksOrRepeats() throws Exception {
        Harness harness = new Harness(0xff112233, 0xff778899);

        TurnCaptureStepExecutor.Execution execution = harness.execute();

        assertEquals(TurnCaptureStepExecutor.Status.COMPLETED, execution.status());
        assertEquals(TurnCaptureStepExecutor.Code.PIXELS_CHANGED, execution.code());
        assertCompletedFrame(execution, 0xff778899);
        assertExactSuccessfulTrace(harness);
        assertEquals(1, harness.input.moves);
    }

    @Test
    void everyFailureAfterCtrlDownAttemptsExactlyOneReleaseAndReturnsNoProbeFrame() {
        List<Harness> failures = new ArrayList<>();

        Harness downFailed = new Harness(0xff112233, 0xff112233);
        downFailed.keyboard.down = attempt(false, "down-failed");
        failures.add(downFailed);

        Harness downThrew = new Harness(0xff112233, 0xff112233);
        downThrew.keyboard.downFailure = new IllegalStateException("down-threw");
        failures.add(downThrew);

        Harness downSettleFailed = new Harness(0xff112233, 0xff112233);
        downSettleFailed.waits.failCall = 1;
        failures.add(downSettleFailed);

        Harness moveFailed = new Harness(0xff112233, 0xff112233);
        moveFailed.input.moveFailure = new IllegalStateException("move-failed");
        failures.add(moveFailed);

        Harness moveSettleFailed = new Harness(0xff112233, 0xff112233);
        moveSettleFailed.waits.failCall = 2;
        failures.add(moveSettleFailed);

        Harness afterCaptureFailed = new Harness(0xff112233, 0xff112233);
        afterCaptureFailed.capture.failCall = 2;
        failures.add(afterCaptureFailed);

        Harness compareFailed = new Harness(0xff112233, 0xff112233);
        compareFailed.capture.explodeAfterRead = true;
        failures.add(compareFailed);

        Harness releaseSettleFailed = new Harness(0xff112233, 0xff112233);
        releaseSettleFailed.waits.failCall = 3;
        failures.add(releaseSettleFailed);

        for (Harness harness : failures) {
            TurnCaptureStepExecutor.Execution execution = harness.execute();

            assertEquals(TurnCaptureStepExecutor.Status.FAILED, execution.status());
            assertEquals(TurnCaptureStepExecutor.Code.PIXEL_PROBE_FAILED, execution.code());
            assertNull(execution.frame());
            assertEquals(1, harness.queue.probeSubmissions());
            assertEquals(1, harness.keyboard.downCalls);
            assertEquals(1, harness.keyboard.upCalls,
                    "every path that attempts Ctrl DOWN must attempt Ctrl UP exactly once");
            assertTrue(harness.contextHolder.rawCurrent().isEmpty());
        }
    }

    @Test
    void beforeCaptureFailureAndQueueFailureNeverInventCtrlOrChangedState() {
        Harness beforeFailed = new Harness(0xff112233, 0xff112233);
        beforeFailed.capture.failCall = 1;

        TurnCaptureStepExecutor.Execution beforeResult = beforeFailed.execute();

        assertEquals(TurnCaptureStepExecutor.Status.FAILED, beforeResult.status());
        assertEquals(TurnCaptureStepExecutor.Code.PIXEL_PROBE_FAILED, beforeResult.code());
        assertNull(beforeResult.frame());
        assertEquals(0, beforeFailed.keyboard.downCalls);
        assertEquals(0, beforeFailed.keyboard.upCalls);

        Harness queueFailed = new Harness(0xff112233, 0xff112233);
        queueFailed.invalidateResolvedGeneration();

        TurnCaptureStepExecutor.Execution queueResult = queueFailed.execute();

        assertEquals(TurnCaptureStepExecutor.Status.FAILED, queueResult.status());
        assertEquals(TurnCaptureStepExecutor.Code.PIXEL_PROBE_FAILED, queueResult.code());
        assertNull(queueResult.frame());
        assertEquals(1, queueFailed.queue.probeSubmissions());
        assertEquals(0, queueFailed.capture.calls);
        assertEquals(0, queueFailed.keyboard.downCalls);
        assertEquals(0, queueFailed.keyboard.upCalls);
    }

    @Test
    void downUncertainStillReleasesAndReleaseFailureOverridesAllCompletedEvidence() {
        Harness downUncertain = new Harness(0xff112233, 0xff112233);
        downUncertain.keyboard.down = new BoundWindowKeyboardService.KeyTransitionAttempt(
                false, false, "down-uncertain");

        TurnCaptureStepExecutor.Execution downResult = downUncertain.execute();

        assertEquals(TurnCaptureStepExecutor.Status.FAILED, downResult.status());
        assertEquals(TurnCaptureStepExecutor.Code.PIXEL_PROBE_FAILED, downResult.code());
        assertEquals(1, downUncertain.keyboard.downCalls);
        assertEquals(1, downUncertain.keyboard.upCalls);
        assertNull(downResult.frame());

        Harness releaseFailed = new Harness(0xff112233, 0xff778899);
        releaseFailed.keyboard.up = attempt(false, "release-failed");

        TurnCaptureStepExecutor.Execution releaseResult = releaseFailed.execute();

        assertEquals(TurnCaptureStepExecutor.Status.FAILED, releaseResult.status());
        assertEquals(TurnCaptureStepExecutor.Code.CTRL_RELEASE_FAILED, releaseResult.code());
        assertEquals(1, releaseFailed.keyboard.downCalls);
        assertEquals(1, releaseFailed.keyboard.upCalls);
        assertNull(releaseResult.frame(), "release failure must discard changed/unchanged evidence");

        Harness releaseThrew = new Harness(0xff112233, 0xff778899);
        releaseThrew.keyboard.upFailure = new IllegalStateException("release-threw");

        TurnCaptureStepExecutor.Execution releaseThrowResult = releaseThrew.execute();

        assertEquals(TurnCaptureStepExecutor.Status.FAILED, releaseThrowResult.status());
        assertEquals(TurnCaptureStepExecutor.Code.CTRL_RELEASE_FAILED, releaseThrowResult.code());
        assertEquals(1, releaseThrew.keyboard.upCalls);
        assertNull(releaseThrowResult.frame());

        Harness releaseNonRuntimeThrew = new Harness(0xff112233, 0xff778899);
        releaseNonRuntimeThrew.keyboard.upNonRuntimeFailure =
                new AssertionError("release-non-runtime-threw");

        TurnCaptureStepExecutor.Execution releaseNonRuntimeResult = releaseNonRuntimeThrew.execute();

        assertEquals(TurnCaptureStepExecutor.Status.FAILED, releaseNonRuntimeResult.status());
        assertEquals(TurnCaptureStepExecutor.Code.CTRL_RELEASE_FAILED, releaseNonRuntimeResult.code());
        assertEquals(1, releaseNonRuntimeThrew.keyboard.upCalls);
        assertNull(releaseNonRuntimeResult.frame(),
                "a non-Runtime Ctrl UP throwable must record release uncertainty before worker normalization");
    }

    @Test
    void interruptionAfterMoveStillReleasesAndProjectsStoppedWithoutAFrame() {
        Harness harness = new Harness(0xff112233, 0xff778899);
        harness.waits.interruptCall = 2;

        TurnCaptureStepExecutor.Execution execution = harness.execute();

        assertEquals(TurnCaptureStepExecutor.Status.STOPPED, execution.status());
        assertEquals(TurnCaptureStepExecutor.Code.STOPPED, execution.code());
        assertNull(execution.frame());
        assertEquals(1, harness.keyboard.downCalls);
        assertEquals(1, harness.keyboard.upCalls);
        assertFalse(Thread.currentThread().isInterrupted(),
                "the real worker owns its interruption; it must not leak to the caller thread");
    }

    @Test
    void preexistingStopRequestReturnsStoppedBeforeQueueCaptureOrCtrl() {
        Harness harness = new Harness(0xff112233, 0xff778899, true);

        TurnCaptureStepExecutor.Execution execution = harness.execute();

        assertEquals(TurnCaptureStepExecutor.Status.STOPPED, execution.status());
        assertEquals(TurnCaptureStepExecutor.Code.STOPPED, execution.code());
        assertNull(execution.frame());
        assertEquals(0, harness.queue.probeSubmissions());
        assertEquals(0, harness.capture.calls);
        assertEquals(0, harness.keyboard.downCalls);
        assertEquals(0, harness.keyboard.upCalls);
    }

    /**
     * P1-2 seam: a stop that closes the token after the action resolved but before worker admission never
     * reaches the callback, so no probe state is written and {@code window.metadata().stopRequested()} is
     * still the false value captured at resolve time, and this thread is not interrupted. The only witness
     * is the worker's typed {@code safetyReason=STOP_REQUESTED}. Flattening the queue result to a boolean
     * loses it and degrades a real stop into {@code FAILED/PIXEL_PROBE_FAILED}.
     */
    @Test
    void stopClosedBeforeWorkerAdmissionProjectsStoppedInsteadOfAMechanicsFailure() throws Exception {
        Harness harness = new Harness(0xff112233, 0xff112233);
        BlockingRequest blocker = harness.blockWorker();
        TaskStopToken stopToken = new TaskStopToken();
        TaskExecutionContext taskContext = TaskExecutionContext.builder()
                .taskCode("turn-probe")
                .taskName("turn-probe")
                .windowId("window-probe")
                .stopToken(stopToken)
                .windowRuntimeContext(harness.window.context())
                .build();
        AsyncExecution async = harness.executeAsync(taskContext);
        awaitQueued(harness.queue);

        stopToken.requestStop("contract-stop-before-admission");
        blocker.release().countDown();
        assertTrue(async.finished().await(2, TimeUnit.SECONDS));
        blocker.waiter().join(TimeUnit.SECONDS.toMillis(2));
        async.waiter().join(TimeUnit.SECONDS.toMillis(2));

        assertFalse(blocker.waiter().isAlive());
        assertFalse(async.waiter().isAlive());
        assertNull(async.failure().get());
        TurnCaptureStepExecutor.Execution execution = async.result().get();
        assertEquals(TurnCaptureStepExecutor.Status.STOPPED, execution.status());
        assertEquals(TurnCaptureStepExecutor.Code.STOPPED, execution.code());
        assertNull(execution.frame());
        assertEquals(1, harness.queue.probeSubmissions(),
                "the request must still have crossed the real queue boundary once");
        assertEquals(0, harness.capture.calls, "an unadmitted request must never capture");
        assertEquals(0, harness.keyboard.downCalls);
        assertEquals(0, harness.keyboard.upCalls);
    }

    /**
     * Guards the ordering itself: a non-stop incomplete queue result must NOT be laundered into STOPPED by
     * the new typed projection; it stays a mechanics failure with no frame.
     */
    @Test
    void nonStopIncompleteQueueResultStillProjectsProbeFailureNotStopped() {
        Harness harness = new Harness(0xff112233, 0xff112233);
        harness.capture.nonRuntimeFailCall = 1;

        TurnCaptureStepExecutor.Execution execution = harness.execute();

        assertEquals(TurnCaptureStepExecutor.Status.FAILED, execution.status());
        assertEquals(TurnCaptureStepExecutor.Code.PIXEL_PROBE_FAILED, execution.code());
        assertNull(execution.frame());
        assertEquals(1, harness.queue.probeSubmissions());
        assertEquals(1, harness.capture.calls,
                "the callback must start before its non-Runtime failure reaches the real worker");
        assertEquals(0, harness.keyboard.upCalls);
    }

    @Test
    void queuedAtoBtoARebindRejectsTheResolvedGenerationBeforeAnyProbeMechanics() throws Exception {
        Harness harness = new Harness(0xff112233, 0xff778899);
        BlockingRequest blocker = harness.blockWorker();
        AsyncExecution async = harness.executeAsync(null);
        awaitQueued(harness.queue);

        WindowNativeBinding original = harness.window.binding();
        synchronized (harness.window.context()) {
            harness.window.context().setNativeBinding(original.withGeometry(
                    original.getX() + 7,
                    original.getY(),
                    original.getWidth(),
                    original.getHeight()));
            harness.window.context().setNativeBinding(original.withGeometry(
                    original.getX(),
                    original.getY(),
                    original.getWidth(),
                    original.getHeight()));
        }
        assertNotSame(original, harness.window.context().getNativeBinding());
        blocker.release().countDown();
        assertTrue(async.finished().await(2, TimeUnit.SECONDS));
        blocker.waiter().join(TimeUnit.SECONDS.toMillis(2));
        async.waiter().join(TimeUnit.SECONDS.toMillis(2));

        assertFalse(blocker.waiter().isAlive());
        assertFalse(async.waiter().isAlive());
        assertNull(async.failure().get());
        assertEquals(TurnCaptureStepExecutor.Status.FAILED, async.result().get().status());
        assertEquals(TurnCaptureStepExecutor.Code.PIXEL_PROBE_FAILED, async.result().get().code());
        assertEquals(1, harness.queue.probeSubmissions());
        assertEquals(0, harness.capture.calls);
        assertEquals(0, harness.keyboard.downCalls);
        assertEquals(0, harness.input.moves);
        assertEquals(1, harness.focus.calls.get(),
                "only the blocker may focus; the rebound probe must fail before focus");
        assertEquals(1, harness.refresh.calls.get(),
                "the public resolver owns the sole binding refresh");
    }

    @Test
    void startedCancellationReturnsOnlyAfterCtrlUpSettleAndRunsNoLaterMechanics() throws Exception {
        Harness harness = new Harness(0xff112233, 0xff778899);
        harness.waits.blockCtrlDownSettle = true;
        harness.waits.blockCtrlUpSettle = true;
        AsyncExecution async = harness.executeAsync(null);
        assertTrue(harness.waits.ctrlDownSettleEntered.await(2, TimeUnit.SECONDS));

        async.waiter().interrupt();
        harness.waits.allowCtrlDownSettle.countDown();
        assertTrue(harness.waits.ctrlUpSettleEntered.await(2, TimeUnit.SECONDS));
        assertEquals(1L, async.finished().getCount(),
                "the waiter must remain behind the worker-owned Ctrl UP settle barrier");
        assertTrue(async.waiter().isAlive());

        harness.waits.allowCtrlUpSettle.countDown();
        assertTrue(async.finished().await(2, TimeUnit.SECONDS));
        async.waiter().join(TimeUnit.SECONDS.toMillis(2));

        assertFalse(async.waiter().isAlive());
        assertNull(async.failure().get());
        assertEquals(TurnCaptureStepExecutor.Status.STOPPED, async.result().get().status());
        assertEquals(TurnCaptureStepExecutor.Code.STOPPED, async.result().get().code());
        assertNull(async.result().get().frame());
        assertEquals(1, harness.keyboard.downCalls);
        assertEquals(1, harness.keyboard.upCalls);
        assertTrue(harness.waits.mechanicsFinished.await(2, TimeUnit.SECONDS));
        assertEquals(harness.events.size(), harness.waits.lastMechanicAt.get(),
                "the closed result may return only after the callback publishes its final mechanic");
        assertEquals(0, harness.events.size() - harness.waits.lastMechanicAt.get(),
                "no mechanics may run after the closed result returns");
    }

    @Test
    void geometryAndMechanicsAvailabilityFailBeforeAnyDesktopBoundary() {
        Harness outside = new Harness(0xff112233, 0xff112233);
        TurnCaptureSpec invalid = new TurnCaptureSpec(
                ROI,
                TurnCaptureSpec.ResultMode.UPLOAD_IMAGE,
                null,
                new TurnCaptureSpec.PixelChangeProbe(103, 206, 80, 280, 100, 0.05D));

        IllegalArgumentException expected = assertThrows(
                IllegalArgumentException.class,
                () -> outside.executor.execute(outside.window, invalid, 0));
        assertEquals("pixelChangeProbe target is outside the capture region", expected.getMessage());
        assertEquals(0, outside.queue.probeSubmissions());
        assertEquals(0, outside.capture.calls);

        TurnCaptureStepExecutor unavailable = new TurnCaptureStepExecutor(
                outside.capture, new TurnPngCodec());
        TurnCaptureStepExecutor.Execution unavailableResult = unavailable.execute(
                outside.window, captureSpec(), 0);
        assertEquals(TurnCaptureStepExecutor.Status.FAILED, unavailableResult.status());
        assertEquals(TurnCaptureStepExecutor.Code.PIXEL_PROBE_FAILED, unavailableResult.code());
        assertNull(unavailableResult.frame());
    }

    private static BoundWindowKeyboardService.KeyTransitionAttempt attempt(boolean success, String reason) {
        return new BoundWindowKeyboardService.KeyTransitionAttempt(true, success, reason);
    }

    private static TurnCaptureSpec captureSpec() {
        return new TurnCaptureSpec(ROI, TurnCaptureSpec.ResultMode.UPLOAD_IMAGE, null, PROBE);
    }

    private static void assertExactSuccessfulTrace(Harness harness) {
        assertEquals(1, harness.queue.probeSubmissions());
        assertEquals(2, harness.capture.calls);
        assertEquals(1, harness.keyboard.downCalls);
        assertEquals(1, harness.keyboard.upCalls);
        assertSame(harness.window.binding(), harness.capture.lastBinding);
        assertSame(harness.window.binding(), harness.keyboard.lastBinding);
        assertEquals(105, harness.input.lastX);
        assertEquals(206, harness.input.lastY);
        assertEquals(List.of(80L, 280L, 100L), harness.waits.values);
        assertEquals(1, harness.refresh.calls.get(),
                "the public resolver must own the only native-binding refresh");
        assertEquals(1, harness.focus.calls.get());
        assertSame(harness.window.binding(), harness.focus.binding.get());
        assertEquals(List.of(
                        "capture:before",
                        "key:DOWN",
                        "wait:80",
                        "move:105,206",
                        "wait:280",
                        "capture:after",
                        "key:UP",
                        "wait:100"),
                harness.events);
        assertTrue(harness.contextHolder.rawCurrent().isEmpty());
    }

    private static void assertCompletedFrame(
            TurnCaptureStepExecutor.Execution execution,
            int expectedPixel) throws Exception {
        assertNotNull(execution.frame());
        assertEquals(TurnFramePurpose.CAPTURE, execution.frame().metadata().purpose());
        assertEquals(ROI, execution.frame().metadata().region());
        assertEquals(2, execution.frame().metadata().width());
        assertEquals(2, execution.frame().metadata().height());
        assertEquals(0, execution.frame().metadata().sourceStepIndex());
        BufferedImage decoded = ImageIO.read(new ByteArrayInputStream(execution.frame().pngBytes()));
        assertNotNull(decoded);
        try {
            assertEquals(2, decoded.getWidth());
            assertEquals(2, decoded.getHeight());
            assertEquals(expectedPixel, decoded.getRGB(0, 0));
        } finally {
            decoded.flush();
        }
    }

    private static void awaitQueued(InputActionQueue queue) {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
        while (queue.size() == 0 && System.nanoTime() < deadline) {
            Thread.onSpinWait();
        }
        assertTrue(queue.size() > 0, "request did not reach the real input queue");
    }

    private static boolean awaitLatch(CountDownLatch latch) {
        try {
            return latch.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private static final class Harness {
        private final List<String> events = new CopyOnWriteArrayList<>();
        private final CountingRefreshService refresh = new CountingRefreshService();
        private final TaskExecutionContextHolder taskContextHolder = new TaskExecutionContextHolder();
        private final WindowTaskContextHolder contextHolder;
        private final RecordingCaptureService capture;
        private final CountingInputActionQueue queue;
        private final CountingFocusService focus;
        private final RecordingKeyboardService keyboard;
        private final RecordingInput input;
        private final RecordingWaits waits;
        private final TurnExecutionWindow window;
        private final TurnCaptureStepExecutor executor;

        private Harness(int beforePixel, int afterPixel) {
            this(beforePixel, afterPixel, false);
        }

        private Harness(int beforePixel, int afterPixel, boolean stopRequested) {
            WindowIsolationProperties properties = new WindowIsolationProperties();
            properties.setIsolationEnabled(true);
            properties.setInputFocusEnabled(true);
            contextHolder = new WindowTaskContextHolder(properties);
            capture = new RecordingCaptureService(events, contextHolder, beforePixel, afterPixel);
            keyboard = new RecordingKeyboardService(events);
            input = new RecordingInput(events);
            waits = new RecordingWaits(events);
            queue = new CountingInputActionQueue(contextHolder, refresh, taskContextHolder);
            GlobalInputLock inputLock = new GlobalInputLock();
            focus = new CountingFocusService(inputLock);
            WindowAwareInputCoordinator coordinator = new WindowAwareInputCoordinator(
                    inputLock,
                    contextHolder,
                    focus,
                    properties,
                    new NoOpInteractionMetricsService(),
                    refresh);
            new InputActionWorker(
                    queue,
                    new InputActionDeadLetter(),
                    input.provider,
                    coordinator,
                    contextHolder,
                    null).start();

            WindowRuntimeContext context = new WindowRuntimeContext("window-probe", new GameContext());
            WindowNativeBinding binding = new WindowNativeBinding(
                    "12345", "game-window-probe", "GameWindow", 88L,
                    WINDOW_RECT.left(), WINDOW_RECT.top(), WINDOW_RECT.width(), WINDOW_RECT.height());
            context.setNativeBinding(binding);
            if (stopRequested) {
                context.setStatus(WindowRuntimeStatus.STOPPED);
            }
            BareWindowTaskRunner runner = allocate(BareWindowTaskRunner.class);
            runner.initialize(context);
            refresh.binding = binding;
            TurnAction action = new TurnAction(
                    1,
                    "fb68ba07-9cb7-47d2-bc7e-8ab31ae72555",
                    "device-1",
                    "window-probe",
                    List.of(new TurnStep(
                            0, TurnStepType.CAPTURE, null, null, null, captureSpec(), null, null)),
                    false);
            window = TurnExecutionWindow.resolveForAction(
                    action,
                    new TestTaskManager(runner),
                    refresh);
            executor = new TurnCaptureStepExecutor(
                    capture,
                    new TurnPngCodec(),
                    new InputSequences(queue),
                    contextHolder,
                    keyboard,
                    input.provider,
                    () -> null,
                    waits);
        }

        private TurnCaptureStepExecutor.Execution execute() {
            return executor.execute(window, captureSpec(), 0);
        }

        private void invalidateResolvedGeneration() {
            WindowNativeBinding binding = window.binding();
            synchronized (window.context()) {
                window.context().setNativeBinding(binding.withGeometry(
                        binding.getX() + 1,
                        binding.getY(),
                        binding.getWidth(),
                        binding.getHeight()));
                window.context().setNativeBinding(binding.withGeometry(
                        binding.getX(),
                        binding.getY(),
                        binding.getWidth(),
                        binding.getHeight()));
            }
        }

        private AsyncExecution executeAsync(TaskExecutionContext taskContext) {
            AtomicReference<TurnCaptureStepExecutor.Execution> result = new AtomicReference<>();
            AtomicReference<Throwable> failure = new AtomicReference<>();
            CountDownLatch finished = new CountDownLatch(1);
            Thread waiter = new Thread(() -> {
                try {
                    TurnCaptureStepExecutor.Execution execution = taskContext == null
                            ? execute()
                            : taskContextHolder.callWith(taskContext, this::execute);
                    result.set(execution);
                } catch (Throwable throwable) {
                    failure.set(throwable);
                } finally {
                    finished.countDown();
                }
            }, "turn-probe-waiter");
            waiter.start();
            return new AsyncExecution(waiter, result, failure, finished);
        }

        private BlockingRequest blockWorker() throws InterruptedException {
            WindowRuntimeContext context = new WindowRuntimeContext("window-blocker", new GameContext());
            WindowNativeBinding binding = new WindowNativeBinding(
                    "99999", "game-window-blocker", "GameWindow", 999L,
                    100, 200, 20, 20);
            context.setNativeBinding(binding);
            CountDownLatch entered = new CountDownLatch(1);
            CountDownLatch release = new CountDownLatch(1);
            Thread waiter = new Thread(() -> queue.submitFrozenExactWindowExclusiveAndWait(
                    "frozen-blocker",
                    context,
                    binding,
                    () -> {
                        entered.countDown();
                        return awaitLatch(release);
                    }), "turn-probe-blocker");
            waiter.start();
            assertTrue(entered.await(2, TimeUnit.SECONDS));
            return new BlockingRequest(waiter, release);
        }
    }

    private static final class CountingInputActionQueue extends InputActionQueue {
        private final AtomicInteger probeSubmissions = new AtomicInteger();

        private CountingInputActionQueue(
                WindowTaskContextHolder contextHolder,
                WindowNativeBindingRefreshService refresh,
                TaskExecutionContextHolder taskContextHolder) {
            super(contextHolder, refresh, taskContextHolder);
        }

        @Override
        public InputActionExecutionResult submitFrozenExactWindowExclusiveAndWait(
                String description,
                WindowRuntimeContext context,
                WindowNativeBinding binding,
                Supplier<Boolean> callback) {
            if (description.startsWith("turn:capture:pixel-change:")) {
                probeSubmissions.incrementAndGet();
            }
            return super.submitFrozenExactWindowExclusiveAndWait(
                    description, context, binding, callback);
        }

        private int probeSubmissions() {
            return probeSubmissions.get();
        }
    }

    private static final class CountingRefreshService extends WindowNativeBindingRefreshService {
        private final AtomicInteger calls = new AtomicInteger();
        private WindowNativeBinding binding;

        @Override
        public Optional<WindowNativeBinding> refreshAndCommit(WindowRuntimeContext context) {
            calls.incrementAndGet();
            if (context == null || binding == null) {
                return Optional.empty();
            }
            context.setNativeBinding(binding);
            return Optional.of(binding);
        }
    }

    private static final class CountingFocusService extends WindowFocusService {
        private final AtomicInteger calls = new AtomicInteger();
        private final AtomicReference<WindowNativeBinding> binding = new AtomicReference<>();

        private CountingFocusService(GlobalInputLock inputLock) {
            super(inputLock);
        }

        @Override
        public boolean focusWithoutLock(WindowNativeBinding exactBinding) {
            calls.incrementAndGet();
            binding.set(exactBinding);
            return true;
        }
    }

    private static final class NoOpInteractionMetricsService extends WindowInteractionMetricsService {
        @Override
        public void recordFocus(String windowId, String actionName, boolean success) {
        }
    }

    private static final class TestTaskManager extends MultiWindowTaskManager {
        private final WindowTaskRunner runner;

        private TestTaskManager(WindowTaskRunner runner) {
            super(
                    null, null, null, null, null, null, null, null, null, null, null,
                    null, null, null, null, null, null, null, null, null, List.of(), null);
            this.runner = runner;
        }

        @Override
        public Optional<WindowTaskRunner> getRunner(String windowId) {
            return runner != null && runner.getWindowContext().getWindowId().equals(windowId)
                    ? Optional.of(runner)
                    : Optional.empty();
        }
    }

    private static final class BareWindowTaskRunner extends WindowTaskRunner {
        private WindowRuntimeContext context;

        private BareWindowTaskRunner() {
            super(
                    null, null, null, null, null, null, null, null, null, null,
                    null, null, null, null, null, null, null, null, List.of(), null);
        }

        private void initialize(WindowRuntimeContext context) {
            this.context = context;
        }

        @Override
        public WindowRuntimeContext getWindowContext() {
            return context;
        }

        @Override
        public RunningTaskHandle getCurrentTask() {
            return null;
        }
    }

    private static <T> T allocate(Class<T> type) {
        try {
            return type.cast(UNSAFE.allocateInstance(type));
        } catch (InstantiationException failure) {
            throw new AssertionError("cannot allocate inert probe test double " + type.getName(), failure);
        }
    }

    private static Unsafe findUnsafe() {
        try {
            Field field = Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            return (Unsafe) field.get(null);
        } catch (ReflectiveOperationException failure) {
            throw new ExceptionInInitializerError(failure);
        }
    }

    private record AsyncExecution(
            Thread waiter,
            AtomicReference<TurnCaptureStepExecutor.Execution> result,
            AtomicReference<Throwable> failure,
            CountDownLatch finished) {
    }

    private record BlockingRequest(Thread waiter, CountDownLatch release) {
    }

    private static final class RecordingKeyboardService extends BoundWindowKeyboardService {
        private final List<String> events;
        private KeyTransitionAttempt down = attempt(true, "OK");
        private KeyTransitionAttempt up = attempt(true, "OK");
        private RuntimeException downFailure;
        private RuntimeException upFailure;
        private AssertionError upNonRuntimeFailure;
        private int downCalls;
        private int upCalls;
        private WindowNativeBinding lastBinding;

        private RecordingKeyboardService(List<String> events) {
            super(null, null, null, null);
            this.events = events;
        }

        @Override
        public KeyTransitionAttempt transitionModifier(
                WindowNativeBinding binding,
                String windowId,
                ModifierKey key,
                KeyTransition transition) {
            assertEquals("window-probe", windowId);
            assertEquals(ModifierKey.CONTROL, key);
            lastBinding = binding;
            events.add("key:" + transition.name());
            if (transition == KeyTransition.DOWN) {
                downCalls++;
                if (downFailure != null) {
                    throw downFailure;
                }
                return down;
            }
            upCalls++;
            if (upFailure != null) {
                throw upFailure;
            }
            if (upNonRuntimeFailure != null) {
                throw upNonRuntimeFailure;
            }
            return up;
        }
    }

    private static final class RecordingInput {
        private final List<String> events;
        private final InputProvider provider;
        private RuntimeException moveFailure;
        private int moves;
        private int lastX;
        private int lastY;

        private RecordingInput(List<String> events) {
            this.events = events;
            provider = (InputProvider) Proxy.newProxyInstance(
                    InputProvider.class.getClassLoader(),
                    new Class<?>[] {InputProvider.class},
                    (proxy, method, args) -> {
                        if (method.getDeclaringClass() == Object.class) {
                            return switch (method.getName()) {
                                case "toString" -> "RecordingInputProvider";
                                case "hashCode" -> System.identityHashCode(proxy);
                                case "equals" -> proxy == args[0];
                                default -> null;
                            };
                        }
                        if ("moveMouse".equals(method.getName())) {
                            moves++;
                            lastX = (Integer) args[0];
                            lastY = (Integer) args[1];
                            events.add("move:" + lastX + "," + lastY);
                            if (moveFailure != null) {
                                throw moveFailure;
                            }
                            return null;
                        }
                        throw new AssertionError("unexpected InputProvider call: " + method.getName());
                    });
        }
    }

    private static final class RecordingWaits implements LongPredicate {
        private final List<String> events;
        private final List<Long> values = new CopyOnWriteArrayList<>();
        private final CountDownLatch ctrlDownSettleEntered = new CountDownLatch(1);
        private final CountDownLatch allowCtrlDownSettle = new CountDownLatch(1);
        private final CountDownLatch ctrlUpSettleEntered = new CountDownLatch(1);
        private final CountDownLatch allowCtrlUpSettle = new CountDownLatch(1);
        private final CountDownLatch mechanicsFinished = new CountDownLatch(1);
        private final AtomicInteger lastMechanicAt = new AtomicInteger(-1);
        private int calls;
        private int failCall;
        private int interruptCall;
        private boolean blockCtrlDownSettle;
        private boolean blockCtrlUpSettle;

        private RecordingWaits(List<String> events) {
            this.events = events;
        }

        @Override
        public boolean test(long value) {
            calls++;
            values.add(value);
            events.add("wait:" + value);
            if (calls == interruptCall) {
                Thread.currentThread().interrupt();
                return false;
            }
            if (blockCtrlDownSettle && value == PROBE.ctrlDownSettleMs()) {
                ctrlDownSettleEntered.countDown();
                if (!awaitLatch(allowCtrlDownSettle)) {
                    return false;
                }
            }
            if (blockCtrlUpSettle && value == PROBE.ctrlUpSettleMs()) {
                ctrlUpSettleEntered.countDown();
                boolean completed = awaitLatch(allowCtrlUpSettle);
                lastMechanicAt.set(events.size());
                mechanicsFinished.countDown();
                return completed && calls != failCall;
            }
            return calls != failCall;
        }
    }

    private static final class RecordingCaptureService extends BoundWindowCaptureService {
        private final List<String> events;
        private final WindowTaskContextHolder contextHolder;
        private final int beforePixel;
        private final int afterPixel;
        private int calls;
        private int failCall;
        private int nonRuntimeFailCall;
        private boolean explodeAfterRead;
        private WindowNativeBinding lastBinding;

        private RecordingCaptureService(
                List<String> events,
                WindowTaskContextHolder contextHolder,
                int beforePixel,
                int afterPixel) {
            this.events = events;
            this.contextHolder = contextHolder;
            this.beforePixel = beforePixel;
            this.afterPixel = afterPixel;
        }

        @Override
        public Optional<CaptureResult> captureRegion(
                WindowNativeBinding binding,
                int windowBaseX,
                int windowBaseY,
                int x1,
                int y1,
                int x2,
                int y2) {
            WindowRuntimeContext workerContext = contextHolder.rawCurrent().orElseThrow(
                    () -> new AssertionError("probe callback ran without the exact worker window context"));
            assertEquals("window-probe", workerContext.getWindowId());
            assertSame(binding, workerContext.getNativeBinding());
            calls++;
            lastBinding = binding;
            events.add(calls == 1 ? "capture:before" : "capture:after");
            if (calls == nonRuntimeFailCall) {
                throw new AssertionError("capture-non-runtime-failed-" + calls);
            }
            if (calls == failCall) {
                throw new IllegalStateException("capture-failed-" + calls);
            }
            assertEquals(WINDOW_RECT.left(), windowBaseX);
            assertEquals(WINDOW_RECT.top(), windowBaseY);
            assertEquals(ROI.x(), x1);
            assertEquals(ROI.y(), y1);
            assertEquals(ROI.x() + ROI.width(), x2);
            assertEquals(ROI.y() + ROI.height(), y2);
            int pixel = calls == 1 ? beforePixel : afterPixel;
            BufferedImage image = calls == 2 && explodeAfterRead
                    ? explodingImage(pixel)
                    : image(pixel);
            return Optional.of(new CaptureResult(image, CaptureProvider.HWND_PRINTWINDOW));
        }

        private static BufferedImage image(int pixel) {
            BufferedImage image = new BufferedImage(2, 2, BufferedImage.TYPE_INT_ARGB);
            for (int y = 0; y < 2; y++) {
                for (int x = 0; x < 2; x++) {
                    image.setRGB(x, y, pixel);
                }
            }
            return image;
        }

        private static BufferedImage explodingImage(int pixel) {
            BufferedImage image = new BufferedImage(2, 2, BufferedImage.TYPE_INT_ARGB) {
                @Override
                public int getRGB(int x, int y) {
                    throw new IllegalStateException("compare-failed");
                }
            };
            image.setRGB(0, 0, pixel);
            return image;
        }
    }
}
