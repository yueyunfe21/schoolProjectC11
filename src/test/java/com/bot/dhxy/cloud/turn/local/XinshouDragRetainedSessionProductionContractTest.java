package com.bot.dhxy.cloud.turn.local;

import com.bot.dhxy.cloud.turn.LocalServiceExecution;
import com.bot.dhxy.cloud.turn.protocol.TurnLocalOperation;
import com.bot.dhxy.cloud.turn.protocol.TurnLocalServiceCall;
import com.bot.dhxy.cloud.turn.protocol.TurnStepResult;
import com.bot.dhxy.cloud.turn.protocol.TurnXinshouDragArguments;
import com.bot.dhxy.config.WindowIsolationProperties;
import com.bot.dhxy.core.GameClientTracker;
import com.bot.dhxy.core.GameContext;
import com.bot.dhxy.driver.BoundWindowKeyboardService;
import com.bot.dhxy.driver.WinApiMouseController;
import com.bot.dhxy.input.GlobalInputLock;
import com.bot.dhxy.input.InputProvider;
import com.bot.dhxy.input.WindowAwareInputCoordinator;
import com.bot.dhxy.input.action.InputActionDeadLetter;
import com.bot.dhxy.input.action.InputActionExecutionResult;
import com.bot.dhxy.input.action.InputActionQueue;
import com.bot.dhxy.input.action.InputActionSafetyReason;
import com.bot.dhxy.input.action.InputActionWorker;
import com.bot.dhxy.runner.stop.TaskPauseToken;
import com.bot.dhxy.runner.stop.TaskStopToken;
import com.bot.dhxy.tools.CoordinateHelper;
import com.bot.dhxy.window.model.WindowNativeBinding;
import com.bot.dhxy.window.runtime.WindowNativeBindingRefreshService;
import com.bot.dhxy.window.runtime.WindowRuntimeContext;
import com.bot.dhxy.window.runtime.WindowTaskContextHolder;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Production-chain contract for the retained LEFT_DOWN session used by the Xinshou drag executor.
 */
class XinshouDragRetainedSessionProductionContractTest {

    private static final long AWAIT_SECONDS = 5L;

    @Test
    void firstAndLaterCallbacksShareOneRetainedRequestWithoutIntermediateLeftUp() {
        try (Harness harness = new Harness()) {
            assertCompleted(harness.sweep(1));
            assertCompleted(harness.sweep(2));
            assertCompleted(harness.sweep(3));

            assertEquals(1, harness.queue.openCount());
            assertEquals(List.of("LEFT_DOWN", "CONTINUE", "CONTINUE"), harness.mouse.events());
            assertEquals(3, harness.tracker.captureCalls());
            assertEquals(1, new HashSet<>(harness.mouse.callbackThreads()).size());
            assertEquals(0, harness.unexpectedInputCalls.get());
            assertNull(harness.queue.latestHandle().terminalSnapshot());
            assertNull(harness.queue.latestHandle().releasedTerminalSnapshot());
        }
    }

    @Test
    void releasePublishesTerminalOnlyAfterExactlyOneLeftUp() throws Exception {
        try (Harness harness = new Harness()) {
            assertCompleted(harness.sweep(1));
            ReleaseGate releaseGate = harness.mouse.blockNextRelease();
            ExecutorService releaseCaller = Executors.newSingleThreadExecutor();
            Future<LocalServiceExecution> releaseFuture = releaseCaller.submit(() ->
                    harness.contextHolder.callWith(harness.primaryContext, () -> {
                        harness.mouse.record("RELEASE_COMMAND");
                        LocalServiceExecution result = harness.executor.release(harness.stopToken);
                        harness.mouse.record("TERMINAL_RETURNED");
                        return result;
                    }));
            try {
                assertTrue(releaseGate.entered.await(AWAIT_SECONDS, TimeUnit.SECONDS));
                assertFalse(releaseFuture.isDone());
                assertNull(harness.queue.latestHandle().releasedTerminalSnapshot());
            } finally {
                releaseGate.proceed.countDown();
            }

            LocalServiceExecution released =
                    releaseFuture.get(AWAIT_SECONDS, TimeUnit.SECONDS);
            releaseCaller.shutdownNow();

            assertCompleted(released);
            assertEquals("XINSHOU_DRAG_RELEASED", released.code());
            assertEquals(1, harness.mouse.eventCount("LEFT_UP"));
            assertEquals(1, harness.queue.terminalCommandCount(
                    InputActionQueue.SessionTerminalCommand.RELEASE));
            assertTrue(harness.queue.latestHandle().releasedTerminalSnapshot().isCompleted());
            assertOrder(harness.mouse.events(),
                    "RELEASE_COMMAND", "LEFT_UP", "TERMINAL_RETURNED");

            assertCompleted(harness.executor.release(harness.stopToken));
            assertEquals(1, harness.mouse.eventCount("LEFT_UP"));
        }
    }

    @Test
    void callbackFailureStillReleasesHeldButton() throws Exception {
        try (Harness harness = new Harness()) {
            assertCompleted(harness.sweep(1));
            harness.mouse.failNextContinuation();

            LocalServiceExecution failed = harness.sweep(2);

            assertEquals(TurnStepResult.Status.FAILED, failed.status());
            assertEquals("XINSHOU_DRAG_SWEEP_FAILED", failed.code());
            assertTrue(harness.mouse.awaitLeftUp());
            assertEquals(1, harness.mouse.eventCount("LEFT_DOWN"));
            assertEquals(1, harness.mouse.eventCount("CALLBACK_FAILED"));
            assertEquals(1, harness.mouse.eventCount("LEFT_UP"));
            assertEquals(1, harness.queue.terminalCommandCount(
                    InputActionQueue.SessionTerminalCommand.ABORT));
        }
    }

    @Test
    void abortAfterCallbackSubmissionFailureStillReleasesHeldButton() throws Exception {
        try (Harness harness = new Harness()) {
            assertCompleted(harness.sweep(1));
            harness.queue.failNextCallbackSubmission();

            LocalServiceExecution failed = harness.sweep(2);

            assertEquals(TurnStepResult.Status.FAILED, failed.status());
            assertEquals("XINSHOU_DRAG_SWEEP_FAILED", failed.code());
            assertTrue(harness.mouse.awaitLeftUp());
            assertEquals(List.of("LEFT_DOWN", "LEFT_UP"), harness.mouse.events());
            assertEquals(1, harness.queue.terminalCommandCount(
                    InputActionQueue.SessionTerminalCommand.ABORT));
            assertTrue(harness.queue.latestHandle().releasedTerminalSnapshot().isCompleted());
        }
    }

    @Test
    void stopClosesSessionWithLeftUpBeforeReturningTypedStop() throws Exception {
        try (Harness harness = new Harness()) {
            assertCompleted(harness.sweep(1));
            harness.stopToken.requestStop("test-stop");

            LocalServiceExecution stopped = harness.sweep(2);

            assertTrue(stopped.stopRequested());
            assertEquals(LocalServiceExecution.STOPPED_CODE, stopped.code());
            assertTrue(harness.mouse.awaitLeftUp());
            assertEquals(List.of("LEFT_DOWN", "LEFT_UP"), harness.mouse.events());
            assertEquals(InputActionSafetyReason.STOP_REQUESTED,
                    harness.queue.latestHandle().releasedTerminalSnapshot().getSafetyReason());
        }
    }

    @Test
    void staleTaskAndMissingWindowAreRejectedWithoutInput() {
        try (Harness harness = new Harness()) {
            LocalServiceExecution stale = harness.executor.sweep(
                    dragCall(1), harness.pauseToken, harness.stopToken, () -> false);

            assertEquals(TurnStepResult.Status.FAILED, stale.status());
            assertEquals("XINSHOU_DRAG_RETAINED_REQUEST_STALE", stale.code());
            assertTrue(harness.mouse.events().isEmpty());
            assertEquals(0, harness.tracker.captureCalls());

            harness.contextHolder.clear();
            LocalServiceExecution missingWindow = harness.executor.sweep(
                    dragCall(2), harness.pauseToken, harness.stopToken, () -> true);

            assertEquals(TurnStepResult.Status.FAILED, missingWindow.status());
            assertEquals("XINSHOU_DRAG_WINDOW_UNAVAILABLE", missingWindow.code());
            assertTrue(harness.mouse.events().isEmpty());
            assertEquals(0, harness.unexpectedInputCalls.get());
        }
    }

    @Test
    void wrongTaskOrWindowCannotSubmitAnotherRetainedCallback() {
        try (Harness harness = new Harness()) {
            assertCompleted(harness.sweep(1));
            List<String> beforeWrongOwner = harness.mouse.events();

            LocalServiceExecution wrongTask = harness.executor.sweep(
                    dragCall(2), harness.pauseToken, new TaskStopToken(), () -> true);
            assertEquals("XINSHOU_DRAG_RETAINED_REQUEST_BUSY", wrongTask.code());
            assertEquals(beforeWrongOwner, harness.mouse.events());

            harness.contextHolder.bind(harness.secondaryContext);
            LocalServiceExecution wrongWindow = harness.executor.sweep(
                    dragCall(3), harness.pauseToken, harness.stopToken, () -> true);
            assertEquals("XINSHOU_DRAG_RETAINED_REQUEST_BUSY", wrongWindow.code());
            assertEquals(beforeWrongOwner, harness.mouse.events());
            assertEquals(1, harness.tracker.captureCalls());
            assertEquals(0, harness.unexpectedInputCalls.get());
        }
    }

    private static void assertCompleted(LocalServiceExecution execution) {
        assertEquals(TurnStepResult.Status.COMPLETED, execution.status());
    }

    private static void assertOrder(List<String> events, String first, String second, String third) {
        int firstIndex = events.indexOf(first);
        int secondIndex = events.indexOf(second);
        int thirdIndex = events.indexOf(third);
        assertTrue(firstIndex >= 0);
        assertTrue(secondIndex > firstIndex);
        assertTrue(thirdIndex > secondIndex);
    }

    private static TurnLocalServiceCall dragCall(int segment) {
        TurnXinshouDragArguments drag = new TurnXinshouDragArguments(
                segment,
                100 + segment,
                200,
                90,
                140,
                220,
                7,
                300,
                400,
                20,
                10);
        return new TurnLocalServiceCall(
                TurnLocalOperation.XINSHOU_DRAG_SWEEP,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                drag);
    }

    private static final class Harness implements AutoCloseable {
        private final WindowTaskContextHolder contextHolder =
                new WindowTaskContextHolder(new WindowIsolationProperties());
        private final WindowRuntimeContext primaryContext = context("window-primary", "1001");
        private final WindowRuntimeContext secondaryContext = context("window-secondary", "1002");
        private final TaskPauseToken pauseToken = new TaskPauseToken();
        private final TaskStopToken stopToken = new TaskStopToken();
        private final RecordingMouseController mouse = new RecordingMouseController();
        private final NullCaptureTracker tracker = new NullCaptureTracker();
        private final AtomicInteger unexpectedInputCalls = new AtomicInteger();
        private final TrackingInputActionQueue queue;
        private final XinshouDragLocalOperationExecutor executor;

        private Harness() {
            WindowNativeBindingRefreshService bindingRefresh =
                    new PassThroughBindingRefreshService();
            queue = new TrackingInputActionQueue(contextHolder, bindingRefresh);
            WindowAwareInputCoordinator coordinator = new WindowAwareInputCoordinator(
                    new GlobalInputLock(),
                    contextHolder,
                    null,
                    new WindowIsolationProperties(),
                    null,
                    bindingRefresh);
            InputProvider inputProvider = (InputProvider) Proxy.newProxyInstance(
                    InputProvider.class.getClassLoader(),
                    new Class<?>[]{InputProvider.class},
                    (proxy, method, args) -> {
                        if (method.getDeclaringClass() != Object.class) {
                            unexpectedInputCalls.incrementAndGet();
                            throw new AssertionError(
                                    "unexpected queued input call: " + method.getName());
                        }
                        return null;
                    });
            InputActionWorker worker = new InputActionWorker(
                    queue,
                    new InputActionDeadLetter(),
                    inputProvider,
                    coordinator,
                    contextHolder,
                    new BoundWindowKeyboardService(null, null, null, null));
            worker.start();
            executor = new XinshouDragLocalOperationExecutor(
                    mouse,
                    new IdentityCoordinateHelper(),
                    tracker,
                    queue,
                    contextHolder);
            contextHolder.bind(primaryContext);
        }

        private LocalServiceExecution sweep(int segment) {
            return executor.sweep(
                    dragCall(segment), pauseToken, stopToken, () -> true);
        }

        @Override
        public void close() {
            contextHolder.bind(primaryContext);
            try {
                executor.release(stopToken);
            } finally {
                contextHolder.clear();
            }
        }

        private static WindowRuntimeContext context(String windowId, String nativeHandle) {
            WindowRuntimeContext context =
                    new WindowRuntimeContext(windowId, new GameContext());
            context.setNativeBinding(new WindowNativeBinding(
                    nativeHandle,
                    "game-" + windowId,
                    "GameClass",
                    Long.parseLong(nativeHandle),
                    10,
                    20,
                    1024,
                    768));
            return context;
        }
    }

    private static final class TrackingInputActionQueue extends InputActionQueue {
        private final AtomicInteger openCount = new AtomicInteger();
        private final List<SessionTerminalCommand> terminalCommands =
                new CopyOnWriteArrayList<>();
        private final AtomicBoolean failNextCallbackSubmission = new AtomicBoolean();
        private volatile RetainedSessionHandle latestHandle;

        private TrackingInputActionQueue(
                WindowTaskContextHolder contextHolder,
                WindowNativeBindingRefreshService bindingRefreshService) {
            super(contextHolder, bindingRefreshService, null);
        }

        @Override
        public RetainedSessionHandle openRetainedSession(
                String description,
                TaskPauseToken pauseToken,
                TaskStopToken stopToken,
                Supplier<InputActionSafetyReason> safetyReason,
                Supplier<InputActionSafetyReason> workerAdmission,
                Runnable retainedSessionCleanup) {
            openCount.incrementAndGet();
            RetainedSessionHandle handle = super.openRetainedSession(
                    description,
                    pauseToken,
                    stopToken,
                    safetyReason,
                    workerAdmission,
                    retainedSessionCleanup);
            latestHandle = handle;
            return handle;
        }

        @Override
        public InputActionExecutionResult submitRetainedSessionCallbackAndWait(
                RetainedSessionHandle handle,
                Supplier<Boolean> callback) {
            if (failNextCallbackSubmission.compareAndSet(true, false)) {
                throw new IllegalStateException("synthetic callback submission failure");
            }
            return super.submitRetainedSessionCallbackAndWait(handle, callback);
        }

        @Override
        public InputActionExecutionResult terminateRetainedSessionAndWait(
                RetainedSessionHandle handle,
                SessionTerminalCommand command) {
            terminalCommands.add(command);
            return super.terminateRetainedSessionAndWait(handle, command);
        }

        private int openCount() {
            return openCount.get();
        }

        private RetainedSessionHandle latestHandle() {
            return latestHandle;
        }

        private void failNextCallbackSubmission() {
            failNextCallbackSubmission.set(true);
        }

        private int terminalCommandCount(SessionTerminalCommand command) {
            return (int) terminalCommands.stream()
                    .filter(command::equals)
                    .count();
        }
    }

    private static final class RecordingMouseController extends WinApiMouseController {
        private final List<String> events = new CopyOnWriteArrayList<>();
        private final List<String> callbackThreads = new CopyOnWriteArrayList<>();
        private final AtomicBoolean leftHeld = new AtomicBoolean();
        private final AtomicBoolean failNextContinuation = new AtomicBoolean();
        private final AtomicReference<ReleaseGate> releaseGate = new AtomicReference<>();
        private final CountDownLatch leftUp = new CountDownLatch(1);

        private RecordingMouseController() {
            super(null, null, null);
        }

        @Override
        public void holdSweepWithoutRelease(
                int startX,
                int startY,
                int leftX,
                int rightX,
                int endY,
                int rowStepPx) {
            if (!leftHeld.compareAndSet(false, true)) {
                throw new IllegalStateException("LEFT_DOWN repeated while already held");
            }
            callbackThreads.add(Thread.currentThread().getName());
            events.add("LEFT_DOWN");
        }

        @Override
        public void sweepWhileLeftHeld(
                int startX,
                int startY,
                int leftX,
                int rightX,
                int endY,
                int rowStepPx) {
            callbackThreads.add(Thread.currentThread().getName());
            if (!leftHeld.get()) {
                throw new IllegalStateException("continuation ran without LEFT_DOWN");
            }
            if (failNextContinuation.compareAndSet(true, false)) {
                events.add("CALLBACK_FAILED");
                throw new IllegalStateException("synthetic retained callback failure");
            }
            events.add("CONTINUE");
        }

        @Override
        public void releaseLeftButton() {
            ReleaseGate gate = releaseGate.getAndSet(null);
            if (gate != null) {
                gate.entered.countDown();
                try {
                    if (!gate.proceed.await(AWAIT_SECONDS, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("release gate timed out");
                    }
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("release gate interrupted", interrupted);
                }
            }
            leftHeld.set(false);
            events.add("LEFT_UP");
            leftUp.countDown();
        }

        private List<String> events() {
            return new ArrayList<>(events);
        }

        private List<String> callbackThreads() {
            return new ArrayList<>(callbackThreads);
        }

        private int eventCount(String event) {
            return (int) events.stream().filter(event::equals).count();
        }

        private void record(String event) {
            events.add(event);
        }

        private void failNextContinuation() {
            failNextContinuation.set(true);
        }

        private ReleaseGate blockNextRelease() {
            ReleaseGate gate = new ReleaseGate();
            if (!releaseGate.compareAndSet(null, gate)) {
                throw new IllegalStateException("release gate already armed");
            }
            return gate;
        }

        private boolean awaitLeftUp() throws InterruptedException {
            return leftUp.await(AWAIT_SECONDS, TimeUnit.SECONDS);
        }
    }

    private static final class ReleaseGate {
        private final CountDownLatch entered = new CountDownLatch(1);
        private final CountDownLatch proceed = new CountDownLatch(1);
    }

    private static final class IdentityCoordinateHelper extends CoordinateHelper {
        private IdentityCoordinateHelper() {
            super(null, null);
        }

        @Override
        public int[] getScaledRect(int offsetX, int offsetY, int width, int height) {
            return new int[]{
                    offsetX,
                    offsetY,
                    offsetX + width,
                    offsetY + height
            };
        }
    }

    private static final class NullCaptureTracker extends GameClientTracker {
        private final AtomicInteger captureCalls = new AtomicInteger();

        private NullCaptureTracker() {
            super(null, null, null, null, null, null, null, null, null, null, null);
        }

        @Override
        public BufferedImage captureToMemory(
                String elementName,
                int x1,
                int y1,
                int x2,
                int y2) {
            captureCalls.incrementAndGet();
            return null;
        }

        private int captureCalls() {
            return captureCalls.get();
        }
    }

    private static final class PassThroughBindingRefreshService
            extends WindowNativeBindingRefreshService {

        @Override
        public Optional<WindowNativeBinding> refreshAndCommit(
                WindowRuntimeContext context) {
            return context == null
                    ? Optional.empty()
                    : Optional.ofNullable(context.getNativeBinding());
        }
    }
}
