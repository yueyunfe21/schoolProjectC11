package com.bot.dhxy.cloud.turn;

import com.bot.dhxy.cloud.turn.protocol.TurnAction;
import com.bot.dhxy.cloud.turn.protocol.TurnCaptureSpec;
import com.bot.dhxy.cloud.turn.protocol.TurnFramePurpose;
import com.bot.dhxy.cloud.turn.protocol.TurnInputAction;
import com.bot.dhxy.cloud.turn.protocol.TurnInputSpec;
import com.bot.dhxy.cloud.turn.protocol.TurnOutcome;
import com.bot.dhxy.cloud.turn.protocol.TurnPathingIntent;
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
import com.bot.dhxy.window.execution.WindowTaskRunner;
import com.bot.dhxy.window.policy.WindowCapacityPolicy;
import com.bot.dhxy.window.runtime.WindowRuntimeContextFactory;
import com.bot.dhxy.window.interaction.WindowFocusService;
import com.bot.dhxy.window.model.WindowNativeBinding;
import com.bot.dhxy.window.model.WindowExpectedCombatEnterClaim;
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
import java.util.function.Function;
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

        assertEquals(TurnOutcome.Status.COMPLETED, executed.outcome().status(), executed.outcome().toString());
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
    void pathingBaselineCaptureCannotInvalidateTheFinalMouseQueueGeneration() {
        TurnContractFixtures.ActionHarness harness = TurnContractFixtures.actionHarness(
                true,
                false,
                context -> new TurnContractFixtures.BindingReplacingPathingProof(context));
        TurnAction action = new TurnAction(
                1,
                "action-pathing-baseline-before-freeze",
                TurnContractFixtures.DEVICE_ID,
                TurnContractFixtures.WINDOW_ID,
                List.of(
                        TurnContractFixtures.moveStep(0, 102, 202),
                        TurnContractFixtures.waitStep(1, 150L),
                        TurnContractFixtures.clickStep(2, 103, 203)),
                false,
                new TurnPathingIntent("test", "intent-1", "灵兽村", 112, 93, 5, "TARGETED"));

        ExecutedTurn executed = harness.executor().execute(action);

        assertEquals(TurnOutcome.Status.COMPLETED, executed.outcome().status());
        assertEquals(1, harness.refresh().calls,
                "the exact action generation must be frozen once, after baseline capture");
        assertSame(harness.binding(), harness.context().getNativeBinding());
        assertEquals(1, harness.queue().submissions.size(),
                "the pathing MOVE/WAIT/CLICK must reach the physical input queue");
        assertEquals(List.of(InputActionType.MOVE_MOUSE, InputActionType.SLEEP, InputActionType.CLICK_LEFT),
                harness.queue().submissions.get(0).stream().map(InputAction::getType).toList());
    }

    @Test
    void equivalentCaptureRefreshBetweenMouseFragmentsPreservesTheFrozenGeneration() {
        TurnContractFixtures.ActionHarness harness = TurnContractFixtures.actionHarness(true, false);
        harness.capture().refreshContext = harness.context();
        TurnAction action = new TurnAction(
                1,
                "action-capture-refresh-between-clicks",
                TurnContractFixtures.DEVICE_ID,
                TurnContractFixtures.WINDOW_ID,
                List.of(
                        TurnContractFixtures.moveStep(0, 102, 202),
                        TurnContractFixtures.waitStep(1, 150L),
                        TurnContractFixtures.clickStep(2, 102, 202),
                        TurnContractFixtures.captureStep(
                                3,
                                new TurnRegion(102, 202, 2, 2),
                                TurnCaptureSpec.ResultMode.UPLOAD_IMAGE),
                        TurnContractFixtures.moveStep(4, 103, 203),
                        TurnContractFixtures.waitStep(5, 150L),
                        TurnContractFixtures.clickStep(6, 103, 203)),
                false);

        ExecutedTurn executed = harness.executor().execute(action);

        assertEquals(TurnOutcome.Status.COMPLETED, executed.outcome().status(), executed.outcome().toString());
        assertSame(harness.binding(), harness.context().getNativeBinding(),
                "an equivalent capture refresh must not replace the action generation");
        assertEquals(2, harness.queue().submissions.size(),
                "both the yellow-result click and final minimap click must reach the input queue");
        assertEquals(List.of("input:submit", "capture:roi", "input:submit"), harness.events());
        assertEquals(List.of(InputActionType.MOVE_MOUSE, InputActionType.SLEEP, InputActionType.CLICK_LEFT),
                harness.queue().submissions.get(1).stream().map(InputAction::getType).toList());
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

    // Retained contract: an atomic mouse-queue that fails after a completed click prefix reports the executed
    // prefix as COMPLETED (never NOT_RUN) and only the failing step as FAILED, via the pure expansion helper.
    @Test
    void completedClickPrefixIsReportedCompletedAndNeverNotRun() {
        List<TurnStep> steps = List.of(clickStep(0), clickStep(1), clickStep(2));
        TurnInputStepExecutor.MouseSequenceResult partial = new TurnInputStepExecutor.MouseSequenceResult(
                new TurnInputStepExecutor.Result(
                        TurnInputStepExecutor.Status.FAILED,
                        TurnInputStepExecutor.Code.INPUT_QUEUE_FAILED,
                        "worker terminal after click"),
                2);

        List<TurnStepExecution> expanded =
                LocalTurnActionExecutor.expandMouseSequenceExecutions(steps, partial);

        assertEquals(List.of(
                        TurnStepResult.Status.COMPLETED,
                        TurnStepResult.Status.COMPLETED,
                        TurnStepResult.Status.FAILED),
                expanded.stream().map(execution -> execution.result().status()).toList());
    }

    @Test
    void directCombatTicketSurvivesAltATurnAndOnlyMarkedNextTurnClickRegistersExpectedCombat() {
        TurnContractFixtures.ActionHarness harness = TurnContractFixtures.actionHarness(true, false);
        WindowExpectedCombatEnterClaim claim = new WindowExpectedCombatEnterClaim(
                "direct-claim", "observation-run", "business-run", "XIULUO_V2", "attempt-1",
                TurnContractFixtures.WINDOW_ID, harness.binding().getNativeHandle(), "local-alt-a", null);
        assertTrue(harness.context().armPendingDirectCombatEnterClaim(claim));

        TurnAction altATurn = new TurnAction(
                1, "direct-alt-a", TurnContractFixtures.DEVICE_ID, TurnContractFixtures.WINDOW_ID,
                List.of(new TurnStep(0, TurnStepType.INPUT, TurnInputAction.KEY_TAP,
                        new TurnInputSpec(null, null, null, null, null, "ALT_A", null), null, null, null, null)),
                false);
        assertEquals(TurnOutcome.Status.COMPLETED, harness.executor().execute(altATurn).outcome().status());
        assertNotNull(harness.context().currentPendingDirectCombatEnterClaim(),
                "the ticket must cross from the Alt+A turn to its separately delivered target-click turn");

        TurnAction targetClickTurn = new TurnAction(
                1, "direct-target-click", TurnContractFixtures.DEVICE_ID, TurnContractFixtures.WINDOW_ID,
                List.of(new TurnStep(0, TurnStepType.INPUT, TurnInputAction.CLICK_LEFT,
                        new TurnInputSpec(102, 202, null, null, null, null, null, null, null, true),
                        null, null, null, null)), false);
        assertEquals(TurnOutcome.Status.COMPLETED, harness.executor().execute(targetClickTurn).outcome().status());

        assertNull(harness.context().currentPendingDirectCombatEnterClaim());
        assertNotNull(harness.context().bindExpectedCombatEnterClaim("observation-run", 1L),
                "only a completed marked target click turns the pending ticket into an expected-combat claim");
    }

    @Test
    void ordinaryClickCannotConsumeDirectCombatTicket() {
        TurnContractFixtures.ActionHarness harness = TurnContractFixtures.actionHarness(true, false);
        WindowExpectedCombatEnterClaim claim = new WindowExpectedCombatEnterClaim(
                "direct-claim", "observation-run", "business-run", "XIULUO_V2", "attempt-1",
                TurnContractFixtures.WINDOW_ID, harness.binding().getNativeHandle(), "local-alt-a", null);
        assertTrue(harness.context().armPendingDirectCombatEnterClaim(claim));

        TurnAction ordinaryClick = new TurnAction(
                1, "ordinary-click", TurnContractFixtures.DEVICE_ID, TurnContractFixtures.WINDOW_ID,
                List.of(TurnContractFixtures.clickStep(0, 102, 202)), false);
        assertEquals(TurnOutcome.Status.COMPLETED, harness.executor().execute(ordinaryClick).outcome().status());

        assertNull(harness.context().currentPendingDirectCombatEnterClaim(),
                "an unmarked click invalidates rather than consumes the direct-combat ticket");
        assertNull(harness.context().bindExpectedCombatEnterClaim("observation-run", 1L));
    }

    @Test
    void expiredDirectCombatTicketCannotRegisterFromAMarkedTargetClick() {
        TurnContractFixtures.ActionHarness harness = TurnContractFixtures.actionHarness(true, false);
        WindowExpectedCombatEnterClaim claim = new WindowExpectedCombatEnterClaim(
                "expired-direct-claim", "observation-run", "business-run", "XIULUO_V2", "attempt-1",
                TurnContractFixtures.WINDOW_ID, harness.binding().getNativeHandle(), "local-alt-a", null);
        assertTrue(harness.context().armPendingDirectCombatEnterClaim(claim));
        TurnContractFixtures.expirePendingDirectCombatTicket(harness.context());

        TurnAction targetClickTurn = new TurnAction(
                1, "expired-direct-target-click", TurnContractFixtures.DEVICE_ID, TurnContractFixtures.WINDOW_ID,
                List.of(new TurnStep(0, TurnStepType.INPUT, TurnInputAction.CLICK_LEFT,
                        new TurnInputSpec(102, 202, null, null, null, null, null, null, null, true),
                        null, null, null, null)), false);
        assertEquals(TurnOutcome.Status.COMPLETED, harness.executor().execute(targetClickTurn).outcome().status());

        assertNull(harness.context().currentPendingDirectCombatEnterClaim());
        assertNull(harness.context().bindExpectedCombatEnterClaim("observation-run", 1L),
                "an expired ticket must not authorize the later marked click as expected combat");
    }

    private static TurnStep clickStep(int index) {
        return new TurnStep(
                index, TurnStepType.INPUT, TurnInputAction.CLICK_LEFT,
                new TurnInputSpec(10 + index, 20 + index, null, null, null, null, null),
                null, null, null, null);
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
        return actionHarness(queueComplete, stopRequested, context -> null);
    }

    static void expirePendingDirectCombatTicket(WindowRuntimeContext context) {
        try {
            Field ticketField = WindowRuntimeContext.class.getDeclaredField("pendingDirectCombatEnterClaim");
            ticketField.setAccessible(true);
            Object ticket = ((AtomicReference<?>) ticketField.get(context)).get();
            assertNotNull(ticket, "test precondition: direct-combat ticket is armed");
            Field armedAtMs = ticket.getClass().getDeclaredField("armedAtMs");
            UNSAFE.putLong(ticket, UNSAFE.objectFieldOffset(armedAtMs),
                    System.currentTimeMillis() - 10_001L);
        } catch (ReflectiveOperationException failure) {
            throw new AssertionError("cannot expire direct-combat ticket fixture", failure);
        }
    }

    static ActionHarness actionHarness(
            boolean queueComplete,
            boolean stopRequested,
            Function<WindowRuntimeContext, LocalPathingStartProofMechanics> proofFactory) {
        WindowRuntimeContext context = new WindowRuntimeContext(WINDOW_ID, new GameContext());
        WindowNativeBinding binding = binding();
        context.setNativeBinding(binding);
        if (stopRequested) {
            context.setStatus(WindowRuntimeStatus.STOPPED);
        }

        WindowTaskRunner runner = bareRunner(context);
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
        LocalPathingStartProofMechanics proof = proofFactory.apply(context);
        LocalTurnActionExecutor executor = new LocalTurnActionExecutor(
                manager,
                refresh,
                contextHolder,
                captureExecutor,
                matchExecutor,
                inputExecutor,
                dispatcher,
                new PoisonTurnClient(),
                new TurnOutcomeAssembler(),
                proof == null ? allocate(LocalPathingStartProofMechanics.class) : proof);
        return new ActionHarness(
                executor, manager, runner, context, binding, refresh, contextHolder, capture, queue, events);
    }

    static ProbeActionHarness probeHarness(int beforePixel, int afterPixel) {
        WindowRuntimeContext context = new WindowRuntimeContext(WINDOW_ID, new GameContext());
        WindowNativeBinding binding = binding();
        context.setNativeBinding(binding);

        WindowTaskRunner runner = bareRunner(context);
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
                new PoisonTurnClient(),
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

    static WindowTaskRunner bareRunner(WindowRuntimeContext context) {
        // WindowTaskRunner is final now; a real runner bound to the test context is exactly what
        // TurnExecutionWindow.resolveForAction reads (getWindowContext); no running task is needed for these
        // mechanical action contracts, so getRemoteTaskHandle() stays null (no stop/pause tokens).
        return new WindowTaskRunner(context);
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
                         WindowTaskRunner runner,
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
            // MultiWindowTaskManager gained a 3-arg constructor; getRunner is overridden below, so the
            // injected collaborators are never invoked — inert allocations keep the real capacity policy.
            super(allocate(WindowRuntimeContextFactory.class),
                    new WindowCapacityPolicy(5),
                    allocate(WindowNativeBindingRefreshService.class));
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
        WindowRuntimeContext refreshContext;
        int failRegionCall;
        int fullWindowCalls;
        int regionCalls;
        WindowNativeBinding lastBinding;

        RecordingCaptureService() {
            this(new CopyOnWriteArrayList<>());
        }

        RecordingCaptureService(List<String> events) {
            super(null);
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
            if (refreshContext != null) {
                refreshContext.setNativeBinding(new WindowNativeBinding(
                        binding.getNativeHandle(), binding.getTitle(), binding.getClassName(),
                        binding.getProcessId(), binding.getX(), binding.getY(),
                        binding.getWidth(), binding.getHeight()));
            }
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
        public InputActionExecutionResult submitFrozenExactWindowActionsAndWait(
                String description,
                WindowRuntimeContext context,
                WindowNativeBinding binding,
                List<InputAction> actions) {
            if (context.getNativeBinding() != binding) {
                return notStarted(description);
            }
            events.add("input:submit");
            submissions.add(List.copyOf(actions));
            boundWindowIds.add(contextHolder.rawCurrent()
                    .map(WindowRuntimeContext::getWindowId)
                    .orElse(null));
            if (interruptOnSubmit) {
                // A worker-side stop: interrupt so the executor maps this to a typed STOPPED terminal, with
                // no completed prefix (the whole tail stays NOT_RUN).
                Thread.currentThread().interrupt();
                return notStarted(description);
            }
            if (complete) {
                return InputActionExecutionResult.builder()
                        .requestId(description)
                        .started(true)
                        .startedStepIndex(0)
                        .lastCompletedStepIndex(Math.max(0, actions.size() - 1))
                        .status(InputActionExecutionResult.Status.COMPLETED)
                        .build();
            }
            // A non-completing queue with no completed prefix: the first Turn step is FAILED, tail NOT_RUN.
            return notStarted(description);
        }

        private static InputActionExecutionResult notStarted(String description) {
            return InputActionExecutionResult.builder()
                    .requestId(description)
                    .started(false)
                    .startedStepIndex(-1)
                    .lastCompletedStepIndex(-1)
                    .status(InputActionExecutionResult.Status.NOT_STARTED)
                    .build();
        }
    }

    static final class BindingReplacingPathingProof extends LocalPathingStartProofMechanics {
        private final WindowRuntimeContext context;

        BindingReplacingPathingProof(WindowRuntimeContext context) {
            super(allocate(com.bot.dhxy.cloud.turn.local.LocalMovementFactMechanics.class));
            this.context = context;
        }

        @Override
        public BufferedImage readBaseline() {
            WindowNativeBinding current = context.getNativeBinding();
            context.setNativeBinding(new WindowNativeBinding(
                    current.getNativeHandle(), current.getTitle(), current.getClassName(),
                    current.getProcessId(), current.getX(), current.getY(),
                    current.getWidth(), current.getHeight()));
            return new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        }

        @Override
        public void proveAndRegister(WindowRuntimeContext context,
                                     TurnPathingIntent intent,
                                     BufferedImage baseline) {
            if (baseline != null) {
                baseline.flush();
            }
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
