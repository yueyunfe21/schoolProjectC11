package com.bot.dhxy.cloud.turn;

import com.bot.dhxy.cloud.turn.protocol.TurnAction;
import com.bot.dhxy.cloud.turn.protocol.TurnCaptureSpec;
import com.bot.dhxy.cloud.turn.protocol.TurnFramePurpose;
import com.bot.dhxy.cloud.turn.protocol.TurnInputAction;
import com.bot.dhxy.cloud.turn.protocol.TurnInputSpec;
import com.bot.dhxy.cloud.turn.protocol.TurnOutcome;
import com.bot.dhxy.cloud.turn.protocol.TurnRegion;
import com.bot.dhxy.cloud.turn.protocol.TurnRequest;
import com.bot.dhxy.cloud.turn.protocol.TurnResponse;
import com.bot.dhxy.cloud.turn.protocol.TurnStep;
import com.bot.dhxy.cloud.turn.protocol.TurnStepResult;
import com.bot.dhxy.cloud.turn.protocol.TurnStepType;
import com.bot.dhxy.cloud.turn.protocol.TurnWindowMetadata;
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
import com.bot.dhxy.input.action.InputAction;
import com.bot.dhxy.input.action.InputActionQueue;
import com.bot.dhxy.input.action.InputActionType;
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
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LocalTurnActionExecutorContractTest {

    @Test
    void moveWaitClickCompletesEveryOriginalStepWithOneOrderedQueueSubmission() {
        TurnContractFixtures.ActionHarness harness = TurnContractFixtures.actionHarness(true, false);
        TurnAction action = new TurnAction(
                1,
                "action-atomic-move-click",
                TurnContractFixtures.DEVICE_ID,
                TurnContractFixtures.WINDOW_ID,
                List.of(
                        TurnContractFixtures.moveStep(0, 102, 202),
                        TurnContractFixtures.waitStep(1, 150L),
                        TurnContractFixtures.clickStep(2, 103, 203)),
                false);

        ExecutedTurn executed = harness.executor().execute(action);

        assertEquals(TurnOutcome.Status.COMPLETED, executed.outcome().status());
        assertEquals(List.of(
                        TurnStepResult.Status.COMPLETED,
                        TurnStepResult.Status.COMPLETED,
                        TurnStepResult.Status.COMPLETED),
                executed.outcome().stepResults().stream().map(TurnStepResult::status).toList());
        assertEquals(1, harness.queue().submissions.size());
        List<InputAction> submitted = harness.queue().submissions.get(0);
        assertEquals(List.of(InputActionType.MOVE_MOUSE, InputActionType.SLEEP, InputActionType.CLICK_LEFT),
                submitted.stream().map(InputAction::getType).toList());
        assertEquals(102, submitted.get(0).getX());
        assertEquals(202, submitted.get(0).getY());
        assertEquals(150, submitted.get(1).getDelayMs());
        assertEquals(103, submitted.get(2).getX());
        assertEquals(203, submitted.get(2).getY());
        assertEquals(List.of(TurnContractFixtures.WINDOW_ID), harness.queue().boundWindowIds);
    }

    @Test
    void trailingWaitAfterClosedMouseFragmentRunsAsOriginalStepBeforeCapture() {
        TurnContractFixtures.ActionHarness harness = TurnContractFixtures.actionHarness(true, false);
        TurnAction action = new TurnAction(
                1,
                "action-atomic-trailing-wait",
                TurnContractFixtures.DEVICE_ID,
                TurnContractFixtures.WINDOW_ID,
                List.of(
                        TurnContractFixtures.moveStep(0, 102, 202),
                        TurnContractFixtures.waitStep(1, 150L),
                        TurnContractFixtures.clickStep(2, 103, 203),
                        TurnContractFixtures.waitStep(3, 1L),
                        TurnContractFixtures.captureStep(
                                4,
                                new TurnRegion(102, 202, 2, 2),
                                TurnCaptureSpec.ResultMode.UPLOAD_IMAGE)),
                false);

        ExecutedTurn executed = harness.executor().execute(action);

        assertEquals(TurnOutcome.Status.COMPLETED, executed.outcome().status());
        List<TurnStepResult> results = executed.outcome().stepResults();
        assertEquals(List.of(0, 1, 2, 3, 4), results.stream().map(TurnStepResult::index).toList());
        assertEquals(List.of(
                        TurnStepType.INPUT,
                        TurnStepType.WAIT,
                        TurnStepType.INPUT,
                        TurnStepType.WAIT,
                        TurnStepType.CAPTURE),
                results.stream().map(TurnStepResult::type).toList());
        assertTrue(results.stream().allMatch(result -> result.status() == TurnStepResult.Status.COMPLETED));

        assertEquals(1, harness.queue().submissions.size());
        List<InputAction> submitted = harness.queue().submissions.get(0);
        assertEquals(List.of(InputActionType.MOVE_MOUSE, InputActionType.SLEEP, InputActionType.CLICK_LEFT),
                submitted.stream().map(InputAction::getType).toList());
        assertEquals(3, submitted.size(), "trailing WAIT must remain outside the closed mouse queue fragment");
        assertEquals(150, submitted.get(1).getDelayMs());
        assertEquals(1, harness.capture().regionCalls);
        assertEquals(0, harness.capture().fullWindowCalls);
        assertEquals(List.of("input:submit", "capture:roi"), harness.events());
    }

    @Test
    void multiClickWaitClosedActionStillUsesOneQueueSubmission() {
        TurnContractFixtures.ActionHarness harness = TurnContractFixtures.actionHarness(true, false);
        TurnAction action = new TurnAction(
                1,
                "action-atomic-multi-click",
                TurnContractFixtures.DEVICE_ID,
                TurnContractFixtures.WINDOW_ID,
                List.of(
                        TurnContractFixtures.clickStep(0, 102, 202),
                        TurnContractFixtures.waitStep(1, 40L),
                        TurnContractFixtures.clickStep(2, 103, 203),
                        TurnContractFixtures.waitStep(3, 60L),
                        TurnContractFixtures.clickStep(4, 104, 204)),
                false);

        ExecutedTurn executed = harness.executor().execute(action);

        assertEquals(TurnOutcome.Status.COMPLETED, executed.outcome().status());
        assertTrue(executed.outcome().stepResults().stream()
                .allMatch(result -> result.status() == TurnStepResult.Status.COMPLETED));
        assertEquals(1, harness.queue().submissions.size());
        List<InputAction> submitted = harness.queue().submissions.get(0);
        assertEquals(List.of(
                        InputActionType.CLICK_LEFT,
                        InputActionType.SLEEP,
                        InputActionType.CLICK_LEFT,
                        InputActionType.SLEEP,
                        InputActionType.CLICK_LEFT),
                submitted.stream().map(InputAction::getType).toList());
        assertEquals(40, submitted.get(1).getDelayMs());
        assertEquals(60, submitted.get(3).getDelayMs());
    }

    @Test
    void atomicMouseQueueFailureFailsTheFirstStepAndLeavesTheWholeTailNotRun() {
        TurnContractFixtures.ActionHarness harness = TurnContractFixtures.actionHarness(false, false);
        TurnAction action = new TurnAction(
                1,
                "action-atomic-queue-failure",
                TurnContractFixtures.DEVICE_ID,
                TurnContractFixtures.WINDOW_ID,
                List.of(
                        TurnContractFixtures.moveStep(0, 102, 202),
                        TurnContractFixtures.waitStep(1, 150L),
                        TurnContractFixtures.clickStep(2, 103, 203),
                        TurnContractFixtures.captureStep(
                                3,
                                new TurnRegion(102, 202, 2, 2),
                                TurnCaptureSpec.ResultMode.UPLOAD_IMAGE)),
                false);

        ExecutedTurn executed = harness.executor().execute(action);

        assertEquals(TurnOutcome.Status.FAILED, executed.outcome().status());
        assertEquals(0, executed.outcome().failedStepIndex());
        assertEquals("INPUT_QUEUE_FAILED", executed.outcome().code());
        assertEquals(List.of(
                        TurnStepResult.Status.FAILED,
                        TurnStepResult.Status.NOT_RUN,
                        TurnStepResult.Status.NOT_RUN,
                        TurnStepResult.Status.NOT_RUN),
                executed.outcome().stepResults().stream().map(TurnStepResult::status).toList());
        assertEquals(1, harness.queue().submissions.size());
        assertEquals(0, harness.capture().totalCalls());
        assertEquals(List.of("input:submit"), harness.events());
    }

    @Test
    void stoppedAtomicMouseQueueMarksTheFirstStepStoppedAndLeavesTheTailNotRun() {
        TurnContractFixtures.ActionHarness harness = TurnContractFixtures.actionHarness(true, false);
        harness.queue().setInterruptOnSubmit(true);
        TurnAction action = new TurnAction(
                1,
                "action-atomic-stopped",
                TurnContractFixtures.DEVICE_ID,
                TurnContractFixtures.WINDOW_ID,
                List.of(
                        TurnContractFixtures.moveStep(0, 102, 202),
                        TurnContractFixtures.waitStep(1, 150L),
                        TurnContractFixtures.clickStep(2, 103, 203),
                        TurnContractFixtures.captureStep(
                                3,
                                new TurnRegion(102, 202, 2, 2),
                                TurnCaptureSpec.ResultMode.UPLOAD_IMAGE)),
                true);

        try {
            ExecutedTurn executed = harness.executor().execute(action);

            assertEquals(TurnOutcome.Status.STOPPED, executed.outcome().status());
            assertNull(executed.outcome().failedStepIndex());
            assertEquals("STOPPED", executed.outcome().code());
            assertEquals(List.of(
                            TurnStepResult.Status.FAILED,
                            TurnStepResult.Status.NOT_RUN,
                            TurnStepResult.Status.NOT_RUN,
                            TurnStepResult.Status.NOT_RUN),
                    executed.outcome().stepResults().stream().map(TurnStepResult::status).toList());
            assertEquals(1, harness.queue().submissions.size());
            assertEquals(0, harness.capture().totalCalls(), "STOPPED must not execute tail capture or failure evidence");
            assertTrue(Thread.currentThread().isInterrupted());
        } finally {
            Thread.interrupted();
        }
    }

    @Test
    void nthStepFailureMarksTheTailNotRunAndReplacesThePriorFrameOnce() throws Exception {
        TurnContractFixtures.ActionHarness harness = TurnContractFixtures.actionHarness(false, false);
        TurnAction action = new TurnAction(
                1,
                "action-failure-frame",
                TurnContractFixtures.DEVICE_ID,
                TurnContractFixtures.WINDOW_ID,
                List.of(
                        TurnContractFixtures.waitStep(0, 1L),
                        TurnContractFixtures.captureStep(
                                1,
                                new TurnRegion(102, 202, 2, 2),
                                TurnCaptureSpec.ResultMode.UPLOAD_IMAGE),
                        TurnContractFixtures.clickStep(2, 103, 203),
                        TurnContractFixtures.waitStep(3, 1L)),
                true);

        ExecutedTurn executed = harness.executor().execute(action);
        TurnOutcome outcome = executed.outcome();

        assertEquals(TurnOutcome.Status.FAILED, outcome.status());
        assertEquals(2, outcome.failedStepIndex());
        assertEquals("INPUT_QUEUE_FAILED", outcome.code());
        assertEquals(List.of(
                        TurnStepResult.Status.COMPLETED,
                        TurnStepResult.Status.COMPLETED,
                        TurnStepResult.Status.FAILED,
                        TurnStepResult.Status.NOT_RUN),
                outcome.stepResults().stream().map(TurnStepResult::status).toList());

        assertEquals(1, harness.capture().regionCalls);
        assertEquals(1, harness.capture().fullWindowCalls);
        assertSame(harness.binding(), harness.capture().lastBinding);
        assertEquals(TurnFramePurpose.FAILURE_EVIDENCE, outcome.frame().purpose());
        assertEquals(new TurnRegion(100, 200, 8, 6), outcome.frame().region());
        assertNull(outcome.frame().sourceStepIndex(), "replacement evidence belongs to the failed turn, not step 1");
        assertNotNull(executed.optionalPng());
        BufferedImage returned = ImageIO.read(new ByteArrayInputStream(executed.optionalPng()));
        try {
            assertEquals(8, returned.getWidth());
            assertEquals(6, returned.getHeight());
            assertEquals(TurnContractFixtures.FULL_WINDOW_PIXEL, returned.getRGB(0, 0));
        } finally {
            returned.flush();
        }

        assertEquals(1, harness.queue().submissions.size());
        assertEquals(TurnContractFixtures.WINDOW_ID, harness.queue().boundWindowIds.get(0));
        assertEquals(List.of(
                        "capture:roi",
                        "input:submit",
                        "capture:failure-evidence"),
                harness.events(),
                "mechanics must execute in step order before replacing the frame after failure");
        assertEquals(1, harness.refresh().calls);
        assertSame(harness.context(), harness.refresh().lastContext);
        assertTrue(harness.contextHolder().rawCurrent().isEmpty(), "exact window binding must be restored");
    }

    @Test
    void stopRequestedReturnsStoppedAndNeverExecutesLaterMechanicsOrFailureCapture() {
        TurnContractFixtures.ActionHarness harness = TurnContractFixtures.actionHarness(true, true);
        TurnAction action = new TurnAction(
                1,
                "action-stopped",
                TurnContractFixtures.DEVICE_ID,
                TurnContractFixtures.WINDOW_ID,
                List.of(
                        TurnContractFixtures.waitStep(0, 5_000L),
                        TurnContractFixtures.clickStep(1, 103, 203)),
                true);

        ExecutedTurn executed = harness.executor().execute(action);

        assertEquals(TurnOutcome.Status.STOPPED, executed.outcome().status());
        assertNull(executed.outcome().failedStepIndex());
        assertEquals("STOPPED", executed.outcome().code());
        assertEquals(TurnStepResult.Status.FAILED, executed.outcome().stepResults().get(0).status());
        assertEquals("STOPPED", executed.outcome().stepResults().get(0).code());
        assertEquals(TurnStepResult.Status.NOT_RUN, executed.outcome().stepResults().get(1).status());
        assertNull(executed.outcome().frame());
        assertNull(executed.optionalPng());
        assertEquals(0, harness.queue().submissions.size());
        assertEquals(0, harness.capture().totalCalls());
    }

    @Test
    void pixelProbeChangedAndUnchangedCodesCarryOnlyTheExactAfterFrameIntoOutcome() throws Exception {
        List<Integer> afterPixels = List.of(0xff112233, 0xff778899);
        List<String> expectedCodes = List.of("PIXELS_UNCHANGED", "PIXELS_CHANGED");

        for (int index = 0; index < afterPixels.size(); index++) {
            TurnContractFixtures.ProbeActionHarness harness = TurnContractFixtures.probeHarness(
                    0xff112233, afterPixels.get(index));

            ExecutedTurn executed = harness.executor().execute(
                    TurnContractFixtures.pixelProbeAction("action-probe-" + index, false));

            assertEquals(TurnOutcome.Status.COMPLETED, executed.outcome().status());
            assertEquals("OK", executed.outcome().code());
            assertEquals(expectedCodes.get(index), executed.outcome().stepResults().get(0).code());
            assertEquals(TurnStepResult.Status.COMPLETED, executed.outcome().stepResults().get(0).status());
            assertEquals(TurnFramePurpose.CAPTURE, executed.outcome().frame().purpose());
            assertEquals(TurnContractFixtures.PROBE_ROI, executed.outcome().frame().region());
            assertEquals(0, executed.outcome().frame().sourceStepIndex());
            assertNotNull(executed.optionalPng());
            BufferedImage returned = ImageIO.read(new ByteArrayInputStream(executed.optionalPng()));
            assertNotNull(returned);
            try {
                assertEquals(afterPixels.get(index).intValue(), returned.getRGB(0, 0));
            } finally {
                returned.flush();
            }
            assertEquals(2, harness.capture().regionCalls);
            assertEquals(0, harness.capture().fullWindowCalls);
            assertSame(harness.binding(), harness.capture().lastBinding);
            assertEquals(1, harness.probeQueue().probeSubmissions());
            assertEquals(1, harness.keyboard().downCalls);
            assertEquals(1, harness.keyboard().upCalls);
            assertEquals(1, harness.manager().getRunnerCalls(),
                    "the Local executor must enter through the public action resolver exactly once");
            assertEquals(1, harness.refresh().calls);
            assertSame(harness.context(), harness.refresh().lastContext);
            assertEquals(1, harness.focus().calls.get());
            assertSame(harness.binding(), harness.focus().binding.get());
            assertEquals(List.of(
                            "capture:roi",
                            "key:DOWN",
                            "wait:80",
                            "move:103,203",
                            "wait:280",
                            "capture:roi",
                            "key:UP",
                            "wait:100"),
                    harness.events());
            assertTrue(harness.contextHolder().rawCurrent().isEmpty());
        }
    }

    @Test
    void pixelProbeMechanicsFailureAndStopNeverProjectACompletedProbeCodeOrFrame() {
        TurnContractFixtures.ProbeActionHarness failed = TurnContractFixtures.probeHarness(
                0xff112233, 0xff778899);
        failed.input().moveFailure = new IllegalStateException("move-failed");

        ExecutedTurn failedTurn = failed.executor().execute(
                TurnContractFixtures.pixelProbeAction("action-probe-failed", false));

        assertEquals(TurnOutcome.Status.FAILED, failedTurn.outcome().status());
        assertEquals("PIXEL_PROBE_FAILED", failedTurn.outcome().code());
        assertEquals("PIXEL_PROBE_FAILED", failedTurn.outcome().stepResults().get(0).code());
        assertNull(failedTurn.outcome().frame());
        assertNull(failedTurn.optionalPng());
        assertEquals(1, failed.keyboard().upCalls);

        TurnContractFixtures.ProbeActionHarness stopped = TurnContractFixtures.probeHarness(
                0xff112233, 0xff778899);
        stopped.waits().interruptCall = 2;
        ExecutedTurn stoppedTurn = stopped.executor().execute(
                TurnContractFixtures.pixelProbeAction("action-probe-stopped", false));

        assertEquals(TurnOutcome.Status.STOPPED, stoppedTurn.outcome().status());
        assertEquals("STOPPED", stoppedTurn.outcome().code());
        assertEquals("STOPPED", stoppedTurn.outcome().stepResults().get(0).code());
        assertNull(stoppedTurn.outcome().frame());
        assertNull(stoppedTurn.optionalPng());
        assertEquals(1, stopped.keyboard().upCalls);
        assertFalse(Thread.currentThread().isInterrupted(),
                "the real input worker owns the mechanics interruption");
    }

    @Test
    void pixelProbeStopClosedBeforeRealWorkerAdmissionProjectsStoppedThroughLocalOutcome() throws Exception {
        TurnContractFixtures.ProbeActionHarness harness = TurnContractFixtures.probeHarness(
                0xff112233, 0xff778899);
        TurnContractFixtures.BlockingRequest blocker = harness.blockWorker();
        TaskStopToken stopToken = new TaskStopToken();
        TaskExecutionContext taskContext = TaskExecutionContext.builder()
                .taskCode("local-turn-probe")
                .taskName("local-turn-probe")
                .windowId(TurnContractFixtures.WINDOW_ID)
                .stopToken(stopToken)
                .windowRuntimeContext(harness.context())
                .build();
        TurnContractFixtures.AsyncExecutedTurn async = harness.executeAsync(
                TurnContractFixtures.pixelProbeAction("action-probe-stop-before-admission", false),
                taskContext);
        TurnContractFixtures.awaitQueued(harness.probeQueue());

        stopToken.requestStop("contract-stop-before-admission");
        blocker.release().countDown();
        assertTrue(async.finished().await(2, TimeUnit.SECONDS));
        blocker.waiter().join(TimeUnit.SECONDS.toMillis(2));
        async.waiter().join(TimeUnit.SECONDS.toMillis(2));

        assertFalse(blocker.waiter().isAlive());
        assertFalse(async.waiter().isAlive());
        assertNull(async.failure().get());
        ExecutedTurn executed = async.result().get();
        assertNotNull(executed);
        assertEquals(TurnOutcome.Status.STOPPED, executed.outcome().status());
        assertEquals("STOPPED", executed.outcome().code());
        assertEquals("STOPPED", executed.outcome().stepResults().get(0).code());
        assertNull(executed.outcome().frame());
        assertNull(executed.optionalPng());
        assertEquals(1, harness.probeQueue().probeSubmissions());
        assertEquals(0, harness.capture().totalCalls());
        assertEquals(0, harness.keyboard().downCalls);
        assertEquals(0, harness.keyboard().upCalls);
        assertEquals(1, harness.focus().calls.get(),
                "only the blocker may focus; the stopped probe must fail admission first");
        assertEquals(1, harness.manager().getRunnerCalls());
        assertEquals(1, harness.refresh().calls);
        assertTrue(harness.contextHolder().rawCurrent().isEmpty());
    }

    @Test
    void pixelProbeFailureEvidenceIsCapturedOnlyAfterCtrlReleaseAndReplacesTheProbeFrame() {
        TurnContractFixtures.ProbeActionHarness harness = TurnContractFixtures.probeHarness(
                0xff112233, 0xff778899);
        harness.capture().failRegionCall = 2;

        ExecutedTurn executed = harness.executor().execute(
                TurnContractFixtures.pixelProbeAction("action-probe-evidence", true));

        assertEquals(TurnOutcome.Status.FAILED, executed.outcome().status());
        assertEquals("PIXEL_PROBE_FAILED", executed.outcome().code());
        assertEquals(TurnFramePurpose.FAILURE_EVIDENCE, executed.outcome().frame().purpose());
        assertNull(executed.outcome().frame().sourceStepIndex());
        assertNotNull(executed.optionalPng());
        assertEquals(1, harness.keyboard().upCalls);
        assertEquals(1, harness.capture().fullWindowCalls);
        assertTrue(
                harness.events().indexOf("key:UP") < harness.events().indexOf("capture:failure-evidence"),
                "full-window failure evidence must be captured only after the one Ctrl UP attempt");
    }

    @Test
    void unknownWindowFailsBeforeRefreshCaptureOrInput() {
        TurnContractFixtures.ActionHarness harness = TurnContractFixtures.actionHarness(true, false);
        TurnAction action = new TurnAction(
                1,
                "action-wrong-window",
                TurnContractFixtures.DEVICE_ID,
                "window-missing",
                List.of(TurnContractFixtures.clickStep(0, 103, 203)),
                false);

        IllegalStateException failure = assertThrows(
                IllegalStateException.class,
                () -> harness.executor().execute(action));

        assertEquals("Turn window is not registered: window-missing", failure.getMessage());
        assertEquals(0, harness.refresh().calls);
        assertEquals(0, harness.capture().totalCalls());
        assertEquals(0, harness.queue().submissions.size());
    }
}

/** Shared fake-only fixtures for the six TURN-T03B tests; none starts a real runner or desktop boundary. */
final class TurnContractFixtures {

    static final String DEVICE_ID = "device-1";
    static final String WINDOW_ID = "window-7";
    static final int FULL_WINDOW_PIXEL = 0xff336699;
    static final int ROI_PIXEL = 0xffaa5500;
    static final TurnRegion PROBE_ROI = new TurnRegion(102, 202, 2, 2);
    private static final TurnCaptureSpec.PixelChangeProbe PIXEL_CHANGE_PROBE =
            new TurnCaptureSpec.PixelChangeProbe(103, 203, 80, 280, 100, 0.05D);

    private static final Unsafe UNSAFE = findUnsafe();

    private TurnContractFixtures() {
    }

    static ActionHarness actionHarness(boolean queueComplete, boolean stopRequested) {
        WindowRuntimeContext context = new WindowRuntimeContext(WINDOW_ID, new GameContext());
        WindowNativeBinding binding = binding();
        context.setNativeBinding(binding);
        if (stopRequested) {
            context.setStatus(WindowRuntimeStatus.STOPPED);
        }

        BareWindowTaskRunner runner = bareRunner(context, false, false);
        TestTaskManager manager = new TestTaskManager();
        manager.putRunner(WINDOW_ID, runner);
        RecordingBindingRefreshService refresh = new RecordingBindingRefreshService(binding);
        WindowTaskContextHolder contextHolder = new WindowTaskContextHolder(new WindowIsolationProperties());
        List<String> events = new CopyOnWriteArrayList<>();
        RecordingCaptureService capture = new RecordingCaptureService(events);
        TurnCaptureStepExecutor captureExecutor = new TurnCaptureStepExecutor(capture, new TurnPngCodec());
        RecordingInputQueue queue = new RecordingInputQueue(contextHolder, queueComplete, events);
        TurnInputStepExecutor inputExecutor = new TurnInputStepExecutor(
                queue,
                new RecordingKeyboardService(),
                contextHolder,
                new TurnInputActionMapper(),
                new TurnKeyMapper());
        TurnTemplateCache templateCache = new TurnTemplateCache(
                Path.of("images", "template"),
                new PoisonTurnClient());
        TurnMatchStepExecutor matchExecutor = new TurnMatchStepExecutor(templateCache, captureExecutor);
        LocalServiceStepDispatcher dispatcher = allocate(LocalServiceStepDispatcher.class);
        LocalTurnActionExecutor executor = new LocalTurnActionExecutor(
                manager,
                refresh,
                contextHolder,
                captureExecutor,
                matchExecutor,
                inputExecutor,
                dispatcher,
                new TurnOutcomeAssembler(),
                allocate(LocalPathingStartProofMechanics.class));
        return new ActionHarness(
                executor, manager, runner, context, binding, refresh, contextHolder, capture, queue, events);
    }

    static ProbeActionHarness probeHarness(int beforePixel, int afterPixel) {
        WindowRuntimeContext context = new WindowRuntimeContext(WINDOW_ID, new GameContext());
        WindowNativeBinding binding = binding();
        context.setNativeBinding(binding);

        BareWindowTaskRunner runner = bareRunner(context, false, false);
        TestTaskManager manager = new TestTaskManager();
        manager.putRunner(WINDOW_ID, runner);
        RecordingBindingRefreshService refresh = new RecordingBindingRefreshService(binding);
        WindowIsolationProperties properties = new WindowIsolationProperties();
        properties.setIsolationEnabled(true);
        properties.setInputFocusEnabled(true);
        WindowTaskContextHolder contextHolder = new WindowTaskContextHolder(properties);
        TaskExecutionContextHolder taskContextHolder = new TaskExecutionContextHolder();
        List<String> events = new CopyOnWriteArrayList<>();
        RecordingCaptureService capture = new RecordingCaptureService(events);
        capture.regionPixels = new int[] {beforePixel, afterPixel};
        ProbeKeyboardService keyboard = new ProbeKeyboardService(binding, contextHolder, events);
        ProbeInput input = new ProbeInput(events);
        ProbeWaits waits = new ProbeWaits(events);
        ProbeInputActionQueue probeQueue = new ProbeInputActionQueue(
                contextHolder, refresh, taskContextHolder);
        GlobalInputLock inputLock = new GlobalInputLock();
        ProbeFocusService focus = new ProbeFocusService(inputLock);
        WindowAwareInputCoordinator coordinator = new WindowAwareInputCoordinator(
                inputLock,
                contextHolder,
                focus,
                properties,
                new NoOpInteractionMetricsService(),
                refresh);
        new InputActionWorker(
                probeQueue,
                new InputActionDeadLetter(),
                input.provider,
                coordinator,
                contextHolder,
                null).start();
        TurnCaptureStepExecutor captureExecutor = new TurnCaptureStepExecutor(
                capture,
                new TurnPngCodec(),
                new InputSequences(probeQueue),
                contextHolder,
                keyboard,
                input.provider,
                () -> null,
                waits);
        RecordingInputQueue inputQueue = new RecordingInputQueue(contextHolder, true, events);
        TurnInputStepExecutor inputExecutor = new TurnInputStepExecutor(
                inputQueue,
                new RecordingKeyboardService(),
                contextHolder,
                new TurnInputActionMapper(),
                new TurnKeyMapper());
        TurnTemplateCache templateCache = new TurnTemplateCache(
                Path.of("images", "template"),
                new PoisonTurnClient());
        TurnMatchStepExecutor matchExecutor = new TurnMatchStepExecutor(templateCache, captureExecutor);
        LocalTurnActionExecutor executor = new LocalTurnActionExecutor(
                manager,
                refresh,
                contextHolder,
                captureExecutor,
                matchExecutor,
                inputExecutor,
                allocate(LocalServiceStepDispatcher.class),
                new TurnOutcomeAssembler(),
                allocate(LocalPathingStartProofMechanics.class));
        return new ProbeActionHarness(
                executor,
                manager,
                context,
                binding,
                refresh,
                contextHolder,
                taskContextHolder,
                capture,
                probeQueue,
                focus,
                keyboard,
                input,
                waits,
                events);
    }

    static void awaitQueued(InputActionQueue queue) {
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

    static TurnWindowMetadata metadata(boolean stopRequested) {
        return new TurnWindowMetadata(
                DEVICE_ID,
                WINDOW_ID,
                "game-window-7",
                "12345",
                88L,
                new TurnWindowRect(100, 200, 8, 6),
                false,
                stopRequested);
    }

    static TurnAction clickAction(String actionId) {
        return new TurnAction(
                1,
                actionId,
                DEVICE_ID,
                WINDOW_ID,
                List.of(clickStep(0, 103, 203)),
                false);
    }

    static TurnAction captureUploadClickAction(String actionId) {
        return new TurnAction(
                1,
                actionId,
                DEVICE_ID,
                WINDOW_ID,
                List.of(
                        captureStep(
                                0,
                                new TurnRegion(102, 202, 2, 2),
                                TurnCaptureSpec.ResultMode.UPLOAD_IMAGE),
                        clickStep(1, 103, 203)),
                false);
    }

    static TurnAction pixelProbeAction(String actionId, boolean fullWindowFailureEvidence) {
        return new TurnAction(
                1,
                actionId,
                DEVICE_ID,
                WINDOW_ID,
                List.of(new TurnStep(
                        0,
                        TurnStepType.CAPTURE,
                        null,
                        null,
                        null,
                        new TurnCaptureSpec(
                                PROBE_ROI,
                                TurnCaptureSpec.ResultMode.UPLOAD_IMAGE,
                                null,
                                PIXEL_CHANGE_PROBE),
                        null,
                        null)),
                fullWindowFailureEvidence);
    }

    static TurnStep clickStep(int index, int x, int y) {
        return new TurnStep(
                index,
                TurnStepType.INPUT,
                TurnInputAction.CLICK_LEFT,
                new TurnInputSpec(x, y, null, null, null, null, null),
                null,
                null,
                null,
                null);
    }

    static TurnStep moveStep(int index, int x, int y) {
        return new TurnStep(
                index,
                TurnStepType.INPUT,
                TurnInputAction.MOVE_MOUSE,
                new TurnInputSpec(x, y, null, null, null, null, null),
                null,
                null,
                null,
                null);
    }

    static TurnStep waitStep(int index, long waitMs) {
        return new TurnStep(index, TurnStepType.WAIT, null, null, waitMs, null, null, null);
    }

    static TurnStep captureStep(int index,
                                TurnRegion region,
                                TurnCaptureSpec.ResultMode resultMode) {
        return new TurnStep(
                index,
                TurnStepType.CAPTURE,
                null,
                null,
                null,
                new TurnCaptureSpec(region, resultMode),
                null,
                null);
    }

    static WindowNativeBinding binding() {
        return new WindowNativeBinding(
                "12345", "game-window-7", "GameWindow", 88L, 100, 200, 8, 6);
    }

    static BareWindowTaskRunner bareRunner(WindowRuntimeContext context,
                                            boolean running,
                                            boolean shutdown) {
        BareWindowTaskRunner runner = allocate(BareWindowTaskRunner.class);
        runner.initialize(context, running, shutdown);
        return runner;
    }

    static <T> T allocate(Class<T> type) {
        try {
            return type.cast(UNSAFE.allocateInstance(type));
        } catch (InstantiationException e) {
            throw new AssertionError("cannot allocate inert test double " + type.getName(), e);
        }
    }

    private static Unsafe findUnsafe() {
        try {
            Field field = Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            return (Unsafe) field.get(null);
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    record ActionHarness(LocalTurnActionExecutor executor,
                         TestTaskManager manager,
                         BareWindowTaskRunner runner,
                          WindowRuntimeContext context,
                          WindowNativeBinding binding,
                          RecordingBindingRefreshService refresh,
                          WindowTaskContextHolder contextHolder,
                          RecordingCaptureService capture,
                         RecordingInputQueue queue,
                         List<String> events) {
    }

    record ProbeActionHarness(LocalTurnActionExecutor executor,
                              TestTaskManager manager,
                              WindowRuntimeContext context,
                              WindowNativeBinding binding,
                              RecordingBindingRefreshService refresh,
                              WindowTaskContextHolder contextHolder,
                              TaskExecutionContextHolder taskContextHolder,
                              RecordingCaptureService capture,
                              ProbeInputActionQueue probeQueue,
                              ProbeFocusService focus,
                              ProbeKeyboardService keyboard,
                              ProbeInput input,
                              ProbeWaits waits,
                              List<String> events) {

        AsyncExecutedTurn executeAsync(TurnAction action, TaskExecutionContext taskContext) {
            AtomicReference<ExecutedTurn> result = new AtomicReference<>();
            AtomicReference<Throwable> failure = new AtomicReference<>();
            CountDownLatch finished = new CountDownLatch(1);
            Thread waiter = new Thread(() -> {
                try {
                    result.set(taskContextHolder.callWith(taskContext, () -> executor.execute(action)));
                } catch (Throwable throwable) {
                    failure.set(throwable);
                } finally {
                    finished.countDown();
                }
            }, "local-turn-probe-waiter");
            waiter.start();
            return new AsyncExecutedTurn(waiter, result, failure, finished);
        }

        BlockingRequest blockWorker() throws InterruptedException {
            WindowRuntimeContext blockerContext = new WindowRuntimeContext("window-blocker", new GameContext());
            WindowNativeBinding blockerBinding = new WindowNativeBinding(
                    "99999", "game-window-blocker", "GameWindow", 999L,
                    100, 200, 8, 6);
            blockerContext.setNativeBinding(blockerBinding);
            CountDownLatch entered = new CountDownLatch(1);
            CountDownLatch release = new CountDownLatch(1);
            Thread waiter = new Thread(() -> probeQueue.submitFrozenExactWindowExclusiveAndWait(
                    "frozen-blocker",
                    blockerContext,
                    blockerBinding,
                    () -> {
                        entered.countDown();
                        return awaitLatch(release);
                    }), "local-turn-probe-blocker");
            waiter.start();
            assertTrue(entered.await(2, TimeUnit.SECONDS));
            return new BlockingRequest(waiter, release);
        }
    }

    record AsyncExecutedTurn(Thread waiter,
                             AtomicReference<ExecutedTurn> result,
                             AtomicReference<Throwable> failure,
                             CountDownLatch finished) {
    }

    record BlockingRequest(Thread waiter, CountDownLatch release) {
    }

    static final class TestTaskManager extends MultiWindowTaskManager {
        private final Map<String, WindowTaskRunner> runners = new ConcurrentHashMap<>();
        private int getRunnerCalls;

        TestTaskManager() {
            super(
                    null, null, null, null, null, null, null, null, null, null, null,
                    null, null, null, null, null, null, null, null, null, List.of(), null);
        }

        void putRunner(String windowId, WindowTaskRunner runner) {
            runners.put(windowId, runner);
        }

        void removeRunner(String windowId) {
            runners.remove(windowId);
        }

        int getRunnerCalls() {
            return getRunnerCalls;
        }

        @Override
        public Optional<WindowTaskRunner> getRunner(String windowId) {
            getRunnerCalls++;
            return Optional.ofNullable(runners.get(windowId));
        }
    }

    static final class BareWindowTaskRunner extends WindowTaskRunner {
        private WindowRuntimeContext testContext;
        private boolean testRunning;
        private boolean testShutdown;

        private BareWindowTaskRunner() {
            super(
                    null, null, null, null, null, null, null, null, null, null,
                    null, null, null, null, null, null, null, null, List.of(), null);
        }

        void initialize(WindowRuntimeContext context, boolean running, boolean shutdown) {
            this.testContext = context;
            this.testRunning = running;
            this.testShutdown = shutdown;
        }

        void setRunning(boolean running) {
            this.testRunning = running;
        }

        void setShutdown(boolean shutdown) {
            this.testShutdown = shutdown;
        }

        @Override
        public RunningTaskHandle getCurrentTask() {
            return null;
        }

        @Override
        public WindowRuntimeContext getWindowContext() {
            return testContext;
        }

        @Override
        public boolean isRunning() {
            return testRunning;
        }

        @Override
        public boolean isShutdown() {
            return testShutdown;
        }
    }

    static final class RecordingBindingRefreshService extends WindowNativeBindingRefreshService {
        private final WindowNativeBinding binding;
        int calls;
        WindowRuntimeContext lastContext;

        RecordingBindingRefreshService(WindowNativeBinding binding) {
            this.binding = binding;
        }

        @Override
        public Optional<WindowNativeBinding> refreshAndCommit(WindowRuntimeContext context) {
            calls++;
            lastContext = context;
            context.setNativeBinding(binding);
            return Optional.of(binding);
        }
    }

    static final class RecordingCaptureService extends BoundWindowCaptureService {
        private final List<String> events;
        private int[] regionPixels = new int[] {ROI_PIXEL};
        int failRegionCall;
        int fullWindowCalls;
        int regionCalls;
        WindowNativeBinding lastBinding;

        RecordingCaptureService() {
            this(new CopyOnWriteArrayList<>());
        }

        RecordingCaptureService(List<String> events) {
            this.events = events;
        }

        @Override
        public Optional<CaptureResult> captureWindow(WindowNativeBinding binding) {
            events.add("capture:failure-evidence");
            fullWindowCalls++;
            lastBinding = binding;
            return Optional.of(result(8, 6, FULL_WINDOW_PIXEL));
        }

        @Override
        public Optional<CaptureResult> captureRegion(WindowNativeBinding binding,
                                                     int windowBaseX,
                                                     int windowBaseY,
                                                     int x1,
                                                     int y1,
                                                     int x2,
                                                     int y2) {
            events.add("capture:roi");
            regionCalls++;
            lastBinding = binding;
            if (regionCalls == failRegionCall) {
                throw new IllegalStateException("region-capture-failed-" + regionCalls);
            }
            int pixel = regionPixels[Math.min(regionCalls - 1, regionPixels.length - 1)];
            return Optional.of(result(Math.abs(x2 - x1), Math.abs(y2 - y1), pixel));
        }

        int totalCalls() {
            return fullWindowCalls + regionCalls;
        }

        private CaptureResult result(int width, int height, int pixel) {
            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    image.setRGB(x, y, pixel);
                }
            }
            return new CaptureResult(image, CaptureProvider.HWND_PRINTWINDOW);
        }
    }

    static final class RecordingInputQueue extends InputActionQueue {
        private final WindowTaskContextHolder contextHolder;
        private final List<String> events;
        final List<List<InputAction>> submissions = new ArrayList<>();
        final List<String> boundWindowIds = new ArrayList<>();
        private boolean complete;
        private boolean interruptOnSubmit;

        RecordingInputQueue(WindowTaskContextHolder contextHolder,
                            boolean complete,
                            List<String> events) {
            super(null, null, null);
            this.contextHolder = contextHolder;
            this.complete = complete;
            this.events = events;
        }

        void setComplete(boolean complete) {
            this.complete = complete;
        }

        void setInterruptOnSubmit(boolean interruptOnSubmit) {
            this.interruptOnSubmit = interruptOnSubmit;
        }

        @Override
        public boolean submitAndWait(String description, List<InputAction> actions) {
            events.add("input:submit");
            submissions.add(List.copyOf(actions));
            boundWindowIds.add(contextHolder.rawCurrent()
                    .map(WindowRuntimeContext::getWindowId)
                    .orElse(null));
            if (interruptOnSubmit) {
                Thread.currentThread().interrupt();
                return false;
            }
            return complete;
        }
    }

    static final class ProbeInputActionQueue extends InputActionQueue {
        private final AtomicInteger probeSubmissions = new AtomicInteger();

        ProbeInputActionQueue(WindowTaskContextHolder contextHolder,
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

        int probeSubmissions() {
            return probeSubmissions.get();
        }
    }

    static final class ProbeFocusService extends WindowFocusService {
        final AtomicInteger calls = new AtomicInteger();
        final AtomicReference<WindowNativeBinding> binding = new AtomicReference<>();

        ProbeFocusService(GlobalInputLock inputLock) {
            super(inputLock);
        }

        @Override
        public boolean focusWithoutLock(WindowNativeBinding exactBinding) {
            calls.incrementAndGet();
            binding.set(exactBinding);
            return true;
        }
    }

    static final class NoOpInteractionMetricsService extends WindowInteractionMetricsService {
        @Override
        public void recordFocus(String windowId, String actionName, boolean success) {
        }
    }

    static final class ProbeKeyboardService extends BoundWindowKeyboardService {
        private final WindowNativeBinding expectedBinding;
        private final WindowTaskContextHolder contextHolder;
        private final List<String> events;
        int downCalls;
        int upCalls;

        ProbeKeyboardService(WindowNativeBinding expectedBinding,
                             WindowTaskContextHolder contextHolder,
                             List<String> events) {
            super(null, null, null, null);
            this.expectedBinding = expectedBinding;
            this.contextHolder = contextHolder;
            this.events = events;
        }

        @Override
        public KeyTransitionAttempt transitionModifier(
                WindowNativeBinding binding,
                String windowId,
                ModifierKey key,
                KeyTransition transition) {
            WindowRuntimeContext workerContext = contextHolder.rawCurrent().orElseThrow(
                    () -> new AssertionError("probe mechanics ran without the exact worker window context"));
            assertEquals(WINDOW_ID, workerContext.getWindowId());
            assertSame(expectedBinding, workerContext.getNativeBinding());
            assertSame(expectedBinding, binding);
            assertEquals(WINDOW_ID, windowId);
            assertEquals(ModifierKey.CONTROL, key);
            events.add("key:" + transition.name());
            if (transition == KeyTransition.DOWN) {
                downCalls++;
            } else {
                upCalls++;
            }
            return new KeyTransitionAttempt(true, true, "OK");
        }
    }

    static final class ProbeInput {
        private final List<String> events;
        final InputProvider provider;
        RuntimeException moveFailure;

        ProbeInput(List<String> events) {
            this.events = events;
            this.provider = (InputProvider) Proxy.newProxyInstance(
                    InputProvider.class.getClassLoader(),
                    new Class<?>[] {InputProvider.class},
                    (proxy, method, args) -> {
                        if (method.getDeclaringClass() == Object.class) {
                            return switch (method.getName()) {
                                case "toString" -> "ProbeInputProvider";
                                case "hashCode" -> System.identityHashCode(proxy);
                                case "equals" -> proxy == args[0];
                                default -> null;
                            };
                        }
                        if ("moveMouse".equals(method.getName())) {
                            events.add("move:" + args[0] + "," + args[1]);
                            if (moveFailure != null) {
                                throw moveFailure;
                            }
                            return null;
                        }
                        throw new AssertionError("unexpected InputProvider call: " + method.getName());
                    });
        }
    }

    static final class ProbeWaits implements LongPredicate {
        private final List<String> events;
        private int calls;
        int interruptCall;

        ProbeWaits(List<String> events) {
            this.events = events;
        }

        @Override
        public boolean test(long value) {
            calls++;
            events.add("wait:" + value);
            if (calls == interruptCall) {
                Thread.currentThread().interrupt();
                return false;
            }
            return true;
        }
    }

    private static final class RecordingKeyboardService extends BoundWindowKeyboardService {
        private RecordingKeyboardService() {
            super(null, null, null, null);
        }

        @Override
        public ShortcutAttempt pressShortcut(AltShortcut shortcut) {
            return new ShortcutAttempt(true, true, null, false);
        }
    }

    static final class PoisonTurnClient implements TurnClient {
        @Override
        public TurnExchangeResult exchange(TurnRequest request, byte[] optionalPng) {
            throw new AssertionError("unexpected turn exchange");
        }

        @Override
        public TurnTemplateDownload downloadTemplate(String templateKey, String ifNoneMatch) {
            throw new AssertionError("unexpected template download");
        }
    }

    static final class BlockingTurnClient implements TurnClient {
        private final java.util.concurrent.CountDownLatch entered = new java.util.concurrent.CountDownLatch(1);
        private volatile Thread lastExchangeThread;

        @Override
        public TurnExchangeResult exchange(TurnRequest request, byte[] optionalPng)
                throws TurnTransportException {
            lastExchangeThread = Thread.currentThread();
            entered.countDown();
            try {
                java.util.concurrent.CountDownLatch never = new java.util.concurrent.CountDownLatch(1);
                never.await();
                throw new AssertionError("blocking turn exchange unexpectedly released");
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                throw new TurnTransportException(
                        TurnTransportException.Kind.INTERRUPTED,
                        "blocking fake interrupted",
                        interrupted);
            }
        }

        boolean awaitEntered(Duration timeout) throws InterruptedException {
            return entered.await(timeout.toMillis(), java.util.concurrent.TimeUnit.MILLISECONDS);
        }

        Thread lastExchangeThread() {
            return lastExchangeThread;
        }

        @Override
        public TurnTemplateDownload downloadTemplate(String templateKey, String ifNoneMatch) {
            throw new AssertionError("unexpected template download");
        }
    }
}
