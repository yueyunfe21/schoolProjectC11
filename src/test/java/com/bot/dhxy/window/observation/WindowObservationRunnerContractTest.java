package com.bot.dhxy.window.observation;

import com.bot.dhxy.cloud.turn.protocol.observation.ObservationInterest;
import com.bot.dhxy.cloud.turn.protocol.observation.ObservationAnalysisResult;
import com.bot.dhxy.cloud.turn.protocol.observation.ObservationKeyEvent;
import com.bot.dhxy.cloud.turn.protocol.observation.ObservationKeyEventType;
import com.bot.dhxy.cloud.turn.protocol.observation.ObservationPathingFact;
import com.bot.dhxy.cloud.turn.protocol.observation.ObservationPathingState;
import com.bot.dhxy.cloud.turn.protocol.observation.ObservationPathingTransition;
import com.bot.dhxy.cloud.turn.protocol.observation.ObservationPathingType;
import com.bot.dhxy.cloud.turn.protocol.observation.ObservationPreparedFrame;
import com.bot.dhxy.cloud.turn.protocol.observation.ObservationPreparedFrameDemand;
import com.bot.dhxy.cloud.turn.protocol.observation.ObservationProtocolValidator;
import com.bot.dhxy.cloud.turn.protocol.observation.ObservationRequest;
import com.bot.dhxy.cloud.turn.protocol.observation.ObservationResponse;
import com.bot.dhxy.config.WindowIsolationProperties;
import com.bot.dhxy.core.GameClientTracker;
import com.bot.dhxy.core.GameContext;
import com.bot.dhxy.input.InputSequences;
import com.bot.dhxy.model.PlayerCharacter;
import com.bot.dhxy.model.dialog.DialogOperation;
import com.bot.dhxy.model.job.XiuluoGreenChainSchedule;
import com.bot.dhxy.service.DialogService;
import com.bot.dhxy.task.model.TaskType;
import com.bot.dhxy.tools.CoordinateHelper;
import com.bot.dhxy.window.model.WindowNativeBinding;
import com.bot.dhxy.window.model.WindowDialogInterest;
import com.bot.dhxy.window.model.WindowPathingIntent;
import com.bot.dhxy.window.model.WindowPathingIntentType;
import com.bot.dhxy.window.model.WindowPathingSnapshot;
import com.bot.dhxy.window.model.WindowPathingState;
import com.bot.dhxy.window.runtime.WindowRuntimeContext;
import com.bot.dhxy.window.runtime.WindowTaskContextHolder;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.Field;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TURN-40G step-2 lifecycle contracts for the per-window observation runner: monotonic sequencing, interest
 * adoption, key-event retention/resend until acknowledgement, bounded backlog, transport-failure neutrality and
 * bounded stop. No runtime, no capture, no input.
 */
class WindowObservationRunnerContractTest {

    private static final String TENANT = "tenant-1";
    private static final String DEVICE = "device-1";
    private static final String WINDOW = "window-7";
    private static final String HWND = "12345";
    private static final String TASK_CODE = "XIULUO_V2";
    private static final String TASK_RUN = "start-req-1";

    @Test
    void runnerMovementComparisonUsesOnlyExactXY() {
        assertFalse(WindowObservationSampler.hasRecognizedPathingCoordinateChanged(
                        155, 108, 155, 108),
                "unchanged X/Y must remain no-movement regardless of map OCR");
        assertTrue(WindowObservationSampler.hasRecognizedPathingCoordinateChanged(
                155, 108, 156, 108));
        assertTrue(WindowObservationSampler.hasRecognizedPathingCoordinateChanged(
                155, 108, 155, 109));
        assertTrue(WindowObservationSampler.hasRecognizedPathingCoordinateChanged(
                null, null, 155, 108), "the first coordinate only establishes a baseline");
    }

    @Test
    void changedMapTextWithSameXYTerminatesAsNoMovement() throws Exception {
        WindowRuntimeContext context = new WindowRuntimeContext(WINDOW, new GameContext());
        context.setNativeBinding(new WindowNativeBinding(HWND, "title", "class", 7L, 0, 0, 1024, 768));
        long now = System.currentTimeMillis();
        WindowPathingIntent intent = WindowPathingIntent.builder()
                .intentId("intent-same-xy")
                .source("tracker:test")
                .type(WindowPathingIntentType.UNTARGETED_TRACKER)
                .createdAtMs(now - 5_000L)
                .build();
        context.updatePathingSnapshot(WindowPathingSnapshot.builder()
                .state(WindowPathingState.ACTIVE)
                .intent(intent)
                .locationChangedAtMs(now - 3_000L)
                .coordinateMovementObserved(false)
                .updatedAtMs(now)
                .build());
        WindowObservationSampler sampler = sampler(context);
        setSamplerField(sampler, "localPathingCoordinatePending", true);
        setSamplerField(sampler, "localPathingLastChangedAtMs", 1L);
        setSamplerField(sampler, "localPathingCoordinateRequestedChangedAtMs", 1L);
        setSamplerField(sampler, "localPathingCoordinateRequestedAtMs", now);
        setSamplerField(sampler, "localPathingCoordinateRequestedStableMs", 3_000L);
        setSamplerField(sampler, "localPathingCoordinateRequestedIntentAgeMs", 5_000L);
        setSamplerField(sampler, "localPathingRecognizedMapName", "old-map-ocr");
        setSamplerField(sampler, "localPathingRecognizedX", 155);
        setSamplerField(sampler, "localPathingRecognizedY", 108);
        setSamplerField(sampler, "localPathingRecognizedChangedAtMs", now - 3_000L);

        sampler.acceptAnalysisResults(List.of(new ObservationAnalysisResult(
                "analysis-same-xy", "PATHING_COORDINATE_RESOLVED", "coordinate-strip",
                intent.getIntentId(), null, null, null, "different-map-ocr", 155, 108, null)));

        assertEquals(WindowPathingState.STOPPED_AWAY, context.getPathingSnapshot().getState());
        assertFalse(context.getPathingSnapshot().isCoordinateMovementObserved(),
                "map OCR changes and pixel differences must not become logical movement proof");
    }

    @Test
    void clickTimeCoordinateSeedsTheIntentAndMovementStaysLatchedAfterReturningToStart() throws Exception {
        WindowRuntimeContext context = new WindowRuntimeContext(WINDOW, new GameContext());
        context.setNativeBinding(new WindowNativeBinding(HWND, "title", "class", 7L, 0, 0, 1024, 768));
        PlayerCharacter me = new PlayerCharacter("队长", "leader", "server");
        me.setCurrentMapName("御马监");
        me.setX(183);
        me.setY(94);
        context.getGameState().setMe(me);
        long now = System.currentTimeMillis();
        WindowPathingIntent intent = WindowPathingIntent.builder()
                .intentId("intent-dark-thunder")
                .source("tianting:tracker-green-click:advance")
                .type(WindowPathingIntentType.UNTARGETED_TRACKER)
                .createdAtMs(now - 5_000L)
                .build();

        context.markPathingStarted(intent);
        assertEquals(183, context.getPathingSnapshot().getCurrentX());
        assertEquals(94, context.getPathingSnapshot().getCurrentY());

        WindowObservationSampler sampler = sampler(context);
        sampler.collect(List.of());
        assertEquals(183, getSamplerField(sampler, "localPathingRecognizedX"));
        assertEquals(94, getSamplerField(sampler, "localPathingRecognizedY"));

        setSamplerField(sampler, "localPathingCoordinatePending", true);
        setSamplerField(sampler, "localPathingCoordinateRequestedChangedAtMs",
                getSamplerField(sampler, "localPathingLastChangedAtMs"));
        setSamplerField(sampler, "localPathingCoordinateRequestedAtMs", now);
        setSamplerField(sampler, "localPathingCoordinateRequestedStableMs", 3_000L);
        setSamplerField(sampler, "localPathingCoordinateRequestedIntentAgeMs", 5_000L);
        sampler.acceptAnalysisResults(List.of(new ObservationAnalysisResult(
                "analysis-dark-thunder", "PATHING_COORDINATE_RESOLVED", "coordinate-strip",
                intent.getIntentId(), null, null, null, "瑶池", 94, 83, null)));

        assertEquals(WindowPathingState.ACTIVE, context.getPathingSnapshot().getState());
        assertTrue(context.getPathingSnapshot().isCoordinateMovementObserved(),
                "the first destination coordinate must compare with the click-time baseline");

        setSamplerField(sampler, "localPathingCoordinatePending", true);
        setSamplerField(sampler, "localPathingCoordinateRequestedChangedAtMs",
                getSamplerField(sampler, "localPathingLastChangedAtMs"));
        setSamplerField(sampler, "localPathingCoordinateRequestedAtMs", now + 1_000L);
        sampler.acceptAnalysisResults(List.of(new ObservationAnalysisResult(
                "analysis-returned-to-start", "PATHING_COORDINATE_RESOLVED", "coordinate-strip",
                intent.getIntentId(), null, null, null, "御马监", 183, 94, null)));

        assertEquals(183, context.getPathingSnapshot().getCurrentX());
        assertEquals(94, context.getPathingSnapshot().getCurrentY());
        assertTrue(context.getPathingSnapshot().isCoordinateMovementObserved(),
                "returning to the original coordinate must not erase movement already observed in this intent");
    }

    @Test
    void exactFrameDemandDoesNotExpireAndResendsUntilCloudClearsIt() throws Exception {
        long issuedLongBeforeTheOldLease = System.currentTimeMillis() - 60_000L;
        ObservationPreparedFrameDemand demand = new ObservationPreparedFrameDemand(
                "demand-persistent", "wuhuan-dialog", "dialog-episode-1",
                WINDOW, HWND, TASK_RUN, 41L, issuedLongBeforeTheOldLease);
        AtomicInteger captureCount = new AtomicInteger();
        byte[] png = fullWindowPng();
        PreparedFrameCapture capture = requested -> {
            captureCount.incrementAndGet();
            return new ObservationPreparedFrame(
                    requested.demandId(), requested.purpose(), requested.generation(),
                    0, 0,
                    ObservationProtocolValidator.TERMINAL_FRAME_WIDTH,
                    ObservationProtocolValidator.TERMINAL_FRAME_HEIGHT,
                    "PNG", System.currentTimeMillis(), png);
        };
        ScriptedObservationClient client = new ScriptedObservationClient()
                .thenRespond(request -> response(request, List.of(), 0L, List.of(), List.of(demand)))
                .thenRespond(request -> response(request, List.of(), 0L, List.of(), List.of(demand)))
                .thenRespond(request -> response(request, List.of(), 0L, List.of(), List.of()));
        WindowObservationRunner runner = new WindowObservationRunner(
                client, TENANT, DEVICE, WINDOW, HWND, TASK_CODE, TASK_RUN,
                null, null, capture, 50L);
        try {
            runner.start();
            assertTrue(client.awaitHandled(3, Duration.ofSeconds(3)));
            assertTrue(client.requests.get(0).preparedFrames().isEmpty());
            assertEquals(1, client.requests.get(1).preparedFrames().size());
            assertEquals(1, client.requests.get(2).preparedFrames().size(),
                    "the exact frame must be retained and resent until Cloud clears the demand");
            assertArrayEquals(client.requests.get(1).preparedFrames().getFirst().pngBytes(),
                    client.requests.get(2).preparedFrames().getFirst().pngBytes());
            assertEquals(1, captureCount.get(), "one demand generation is captured only once");
        } finally {
            stopAndAssertStopped(runner);
        }
    }

    @Test
    void movingPathingFactStaysLocalUntilARealEdgeNeedsTransport() {
        ObservationPathingFact first = activePathingFact("intent-1", 1_100L, true, false);
        ObservationPathingFact moving = activePathingFact("intent-1", 1_400L, true, false);
        ObservationPathingFact blocked = activePathingFact("intent-1", 1_700L, true, true);
        ObservationPathingFact arrived = new ObservationPathingFact(
                TASK_RUN, WINDOW, HWND, "intent-1", null, "test", "灵兽村",
                112, 93, 5, ObservationPathingType.TARGETED,
                1_000L, 2_000L, ObservationPathingState.ARRIVED, ObservationPathingTransition.CURRENT,
                "灵兽村", 112, 93, 2_000L, true, false, null, 0L);

        assertTrue(WindowObservationRunner.requiresImmediatePathingSend(first, null),
                "the first fact must register the intent");
        assertFalse(WindowObservationRunner.requiresImmediatePathingSend(moving, first),
                "movement-latch-only updates must remain local");
        assertTrue(WindowObservationRunner.requiresImmediatePathingSend(blocked, first),
                "dialog blocking changes must wake Cloud");
        assertTrue(WindowObservationRunner.requiresImmediatePathingSend(arrived, moving),
                "ARRIVED must be delivered immediately");
    }

    @Test
    void heartbeatCarriesMonotonicSequenceAndAdoptsInterests() throws Exception {
        ScriptedObservationClient client = new ScriptedObservationClient()
                .thenRespond(request -> response(request, List.of(), 3L,
                        List.of(new ObservationInterest("combat-signal", 1_000L, null))))
                .thenRespond(request -> response(request, List.of(), 3L,
                        List.of(new ObservationInterest("combat-signal", 1_000L, null))));
        WindowObservationRunner runner = runner(client);
        try {
            runner.start();
            assertTrue(client.awaitHandled(2, Duration.ofSeconds(3)), "two heartbeats must be exchanged");
            assertTrue(client.requests.get(0).observerSeq() < client.requests.get(1).observerSeq(),
                    "observer sequence must be strictly monotonic");
            assertEquals(WINDOW, client.requests.get(0).windowId());
            assertEquals(TASK_RUN, client.requests.get(0).taskRunId());
            assertEquals(3L, runner.currentInterestRevision(), "the Cloud interest revision must be adopted");
            assertEquals(1, runner.currentInterests().size());
            assertEquals("combat-signal", runner.currentInterests().get(0).interestKey());
        } finally {
            stopAndAssertStopped(runner);
        }
    }

    @Test
    void revisionMismatchDoesNotAcknowledgeChangeOnlyRoiAndTheNextRequestRetransmitsIt()
            throws Exception {
        ObservationInterest trackerInterest = new ObservationInterest(
                WindowObservationSampler.XINSHOU_TRACKER_INTEREST,
                1L,
                null,
                0,
                100,
                280,
                604);
        WindowRuntimeContext context = new WindowRuntimeContext(WINDOW, new GameContext());
        context.setNativeBinding(new WindowNativeBinding(
                HWND, "title", "class", 7L, 0, 0, 1024, 768));
        ScriptedObservationClient client = new ScriptedObservationClient()
                .thenRespond(request -> response(
                        request, List.of(), 1L, List.of(trackerInterest)))
                .thenRespond(request -> response(
                        request, List.of(), 2L, List.of(trackerInterest)))
                .thenRespond(request -> response(
                        request, List.of(), 2L, List.of(trackerInterest)))
                .thenRespond(request -> response(
                        request, List.of(), 2L, List.of(trackerInterest)));
        WindowObservationRunner runner = new WindowObservationRunner(
                client,
                TENANT,
                DEVICE,
                WINDOW,
                HWND,
                TASK_CODE,
                TASK_RUN,
                sampler(context),
                50L);

        try {
            runner.start();
            assertTrue(client.awaitHandled(4, Duration.ofSeconds(4)));
            ObservationRequest revisionMismatched = client.requests.get(1);
            ObservationRequest retransmitted = client.requests.get(2);
            ObservationRequest afterMatchingAck = client.requests.get(3);
            assertEquals(1L, revisionMismatched.interestRevision());
            assertEquals(1, revisionMismatched.rois().size());
            assertEquals(2L, retransmitted.interestRevision());
            assertEquals(1, retransmitted.rois().size(),
                    "revision mismatch must retain the hash for the next request");
            assertTrue(afterMatchingAck.rois().isEmpty(),
                    "the matching seq/revision response may acknowledge the retransmission");
        } finally {
            stopAndAssertStopped(runner);
        }
    }

    @Test
    void suspendInvalidatesChangeOnlyRoiAfterTheInFlightResponseReturns() throws Exception {
        ObservationInterest trackerInterest = new ObservationInterest(
                WindowObservationSampler.XINSHOU_TRACKER_INTEREST,
                1L,
                null,
                0,
                100,
                280,
                604);
        WindowRuntimeContext context = new WindowRuntimeContext(WINDOW, new GameContext());
        context.setNativeBinding(new WindowNativeBinding(
                HWND, "title", "class", 7L, 0, 0, 1024, 768));
        CountDownLatch inFlightRoiSend = new CountDownLatch(1);
        CountDownLatch releaseOldResponse = new CountDownLatch(1);
        ScriptedObservationClient client = new ScriptedObservationClient()
                .thenRespond(request -> response(
                        request, List.of(), 1L, List.of(trackerInterest)))
                .thenRespond(request -> {
                    inFlightRoiSend.countDown();
                    while (true) {
                        try {
                            if (releaseOldResponse.await(3, TimeUnit.SECONDS)) {
                                break;
                            }
                            throw new AssertionError("timed out releasing the in-flight observation response");
                        } catch (InterruptedException ignored) {
                            // Model a transport whose response still arrives after stop interrupts the sender.
                        }
                    }
                    return response(request, List.of(), 1L, List.of(trackerInterest));
                })
                .thenRespond(request -> response(
                        request, List.of(), 1L, List.of(trackerInterest)));
        WindowObservationRunner runner = new WindowObservationRunner(
                client,
                TENANT,
                DEVICE,
                WINDOW,
                HWND,
                TASK_CODE,
                TASK_RUN,
                sampler(context),
                50L);

        try {
            runner.start();
            assertTrue(inFlightRoiSend.await(3, TimeUnit.SECONDS));
            ObservationRequest oldRequest = client.requests.get(1);
            assertEquals(1, oldRequest.rois().size());

            runner.requestSuspend();
            releaseOldResponse.countDown();
            assertTrue(runner.awaitStopped(Duration.ofSeconds(3)));

            runner.start();
            assertTrue(client.awaitHandled(3, Duration.ofSeconds(4)));
            ObservationRequest resumed = client.requests.get(2);
            assertEquals(1, resumed.rois().size(),
                    "suspend exit must invalidate the old acknowledgement after the in-flight send");
            assertEquals(oldRequest.rois().getFirst().roiKey(),
                    resumed.rois().getFirst().roiKey());
            assertArrayEquals(oldRequest.rois().getFirst().pngBytes(),
                    resumed.rois().getFirst().pngBytes(),
                    "the unchanged Xinshou ROI must be retransmitted after resume");
        } finally {
            releaseOldResponse.countDown();
            stopAndAssertStopped(runner);
        }
        assertEquals(0L, runner.currentInterestRevision(),
                "ordinary stop after resume must still reset retained observation state");
        assertTrue(runner.currentInterests().isEmpty());
    }

    @Test
    void keyEventIsRetainedAndResentUntilAcknowledged() throws Exception {
        ScriptedObservationClient client = new ScriptedObservationClient()
                .thenRespond(request -> response(request, List.of(), 0L, List.of()))
                .thenRespond(request -> response(request, List.of("edge-1"), 0L, List.of()))
                .thenRespond(request -> response(request, List.of(), 0L, List.of()));
        WindowObservationRunner runner = runner(client);
        runner.publishKeyEvent(keyEvent("edge-1"));
        try {
            runner.start();
            assertTrue(client.awaitHandled(3, Duration.ofSeconds(3)));
            assertEquals(1, client.requests.get(0).events().size(),
                    "the unacknowledged key event must ride the first request");
            assertEquals("edge-1", client.requests.get(0).events().get(0).eventId());
            assertEquals(1, client.requests.get(1).events().size(),
                    "an unacknowledged key event must be resent unchanged");
            assertEquals(0, client.requests.get(2).events().size(),
                    "an acknowledged key event must never be resent");
            assertEquals(0, runner.pendingKeyEventCount());
        } finally {
            stopAndAssertStopped(runner);
        }
    }

    @Test
    void transportFailureRetainsKeyEventsAndIsNeverABusinessFact() throws Exception {
        ScriptedObservationClient client = new ScriptedObservationClient()
                .thenFail(new ObservationTransportException(
                        ObservationTransportException.Kind.NETWORK, "observation network down"))
                .thenFail(new ObservationTransportException(
                        ObservationTransportException.Kind.NETWORK, "observation network down"))
                .thenRespond(request -> response(request, List.of("edge-net-1"), 0L, List.of()))
                .thenRespond(request -> response(request, List.of(), 0L, List.of()));
        WindowObservationRunner runner = runner(client);
        runner.publishKeyEvent(keyEvent("edge-net-1"));
        try {
            runner.start();
            assertTrue(client.awaitHandled(4, Duration.ofSeconds(6)));
            for (ObservationRequest request : client.requests.subList(0, 3)) {
                assertEquals(1, request.events().size(),
                        "the key event must be retained across transport failures");
            }
            assertTrue(client.requests.get(2).observerSeq() > client.requests.get(0).observerSeq(),
                    "the sequence keeps advancing across transport failures");
            assertEquals(0, client.requests.get(3).events().size(),
                    "the request after the acknowledgement proves the runner applied it");
            assertEquals(0, runner.pendingKeyEventCount(), "the eventual acknowledgement clears retention");
            assertTrue(runner.lastTransportFailureAtMs() > 0L);
        } finally {
            stopAndAssertStopped(runner);
        }
    }

    @Test
    void pendingKeyEventRetentionIsBoundedByEvictingTheOldest() {
        ScriptedObservationClient client = new ScriptedObservationClient();
        WindowObservationRunner runner = runner(client);
        for (int i = 0; i < WindowObservationRunner.MAX_PENDING_KEY_EVENTS + 1; i++) {
            runner.publishKeyEvent(keyEvent("edge-bound-" + i));
        }
        assertEquals(WindowObservationRunner.MAX_PENDING_KEY_EVENTS, runner.pendingKeyEventCount(),
                "unacknowledged key-event retention must stay bounded");
    }

    @Test
    void stopFencesNewRequestsAndTerminatesWithinBound() throws Exception {
        ScriptedObservationClient client = new ScriptedObservationClient()
                .thenRespond(request -> response(request, List.of(), 0L, List.of()));
        WindowObservationRunner runner = runner(client);
        try {
            runner.start();
            assertTrue(client.awaitHandled(1, Duration.ofSeconds(3)));
        } finally {
            stopAndAssertStopped(runner);
        }
        int requestsAtStop = client.requests.size();
        Thread.sleep(120L);
        assertEquals(requestsAtStop, client.requests.size(), "no request may start after the stop fence");
        assertFalse(runner.isRunning());
    }

    @Test
    void suspendResumePreservesSequenceInterestsAndUnacknowledgedEvents() throws Exception {
        ObservationInterest retainedInterest =
                new ObservationInterest("combat-signal", 1_000L, null);
        ScriptedObservationClient client = new ScriptedObservationClient()
                .thenRespond(request -> response(request, List.of(), 7L, List.of(retainedInterest)))
                .thenRespond(request -> response(request, List.of(), 7L, List.of(retainedInterest)))
                .thenRespond(request -> response(request, List.of("edge-resume"), 7L,
                        List.of(retainedInterest)))
                .thenRespond(request -> response(request, List.of(), 7L, List.of(retainedInterest)));
        WindowObservationRunner runner = new WindowObservationRunner(
                client, TENANT, DEVICE, WINDOW, HWND, TASK_CODE, TASK_RUN, null, 5_000L);
        runner.publishKeyEvent(keyEvent("edge-resume"));

        try {
            runner.start();
            assertTrue(client.awaitHandled(2, Duration.ofSeconds(3)));
            runner.requestSuspend();
            assertTrue(runner.awaitStopped(Duration.ofSeconds(3)));

            long sequenceAtPause = runner.observerSeq();
            assertEquals(7L, runner.currentInterestRevision());
            assertEquals(List.of(retainedInterest), runner.currentInterests());
            assertEquals(1, runner.pendingKeyEventCount(),
                    "pause must retain an event whose acknowledgement has not arrived");

            runner.start();
            assertTrue(client.awaitHandled(3, Duration.ofSeconds(4)));
            assertEquals(sequenceAtPause + 1L, client.requests.get(2).observerSeq(),
                    "resume must continue the acknowledged run's monotonic sequence");
            assertEquals(1, client.requests.get(2).events().size());
            assertEquals("edge-resume", client.requests.get(2).events().getFirst().eventId());
            assertEquals(0, runner.pendingKeyEventCount());
        } finally {
            stopAndAssertStopped(runner);
        }
        assertEquals(0L, runner.currentInterestRevision(),
                "terminal stop, unlike pause, clears retained observation state");
        assertTrue(runner.currentInterests().isEmpty());
    }

    @Test
    void suspendResumePreservesPathingReplacementLineage() throws Exception {
        WindowRuntimeContext context = new WindowRuntimeContext(WINDOW, new GameContext());
        context.setNativeBinding(new WindowNativeBinding(HWND, "title", "class", 7L, 0, 0, 1024, 768));
        context.markPathingStarted(pathingIntent("intent-A", "navigation:test-A"));
        ScriptedObservationClient client = new ScriptedObservationClient()
                .thenRespond(request -> response(request, List.of(), 0L, List.of()))
                .thenRespond(request -> response(request, List.of(), 0L, List.of()));
        WindowObservationRunner runner = new WindowObservationRunner(
                client, TENANT, DEVICE, WINDOW, HWND, TASK_CODE, TASK_RUN, sampler(context), 5_000L);

        try {
            runner.start();
            assertTrue(client.awaitHandled(1, Duration.ofSeconds(3)));
            assertEquals(ObservationPathingTransition.CURRENT,
                    client.requests.getFirst().pathingFacts().getFirst().transition());

            runner.requestSuspend();
            assertTrue(runner.awaitStopped(Duration.ofSeconds(3)));
            long sequenceAtPause = runner.observerSeq();
            context.markPathingStarted(pathingIntent("intent-B", "navigation:test-B"));

            runner.start();
            assertTrue(client.awaitHandled(2, Duration.ofSeconds(3)));
            ObservationRequest resumed = client.requests.get(1);
            ObservationPathingFact replacement = resumed.pathingFacts().getFirst();
            assertEquals(sequenceAtPause + 1L, resumed.observerSeq());
            assertEquals("intent-B", replacement.intentId());
            assertEquals("intent-A", replacement.replacedIntentId());
            assertEquals(ObservationPathingTransition.REPLACED, replacement.transition(),
                    "resume must not forget the Cloud-accepted predecessor and emit an invalid CURRENT");
        } finally {
            stopAndAssertStopped(runner);
        }
    }

    @Test
    void samplerUploadsExactCurrentPathingSnapshotAndClearReplacementLineage() {
        WindowRuntimeContext context = new WindowRuntimeContext(WINDOW, new GameContext());
        context.setNativeBinding(new WindowNativeBinding(HWND, "title", "class", 7L, 0, 0, 1024, 768));
        WindowObservationSampler sampler = sampler(context);
        long startedAt = System.currentTimeMillis() - 2_000L;
        WindowPathingIntent firstIntent = WindowPathingIntent.builder()
                .intentId("intent-A")
                .source("navigation:test-A")
                .targetMapName("灵兽村")
                .targetX(120)
                .targetY(88)
                .tolerance(5)
                .type(WindowPathingIntentType.TARGETED)
                .createdAtMs(startedAt)
                .build();
        context.updatePathingSnapshot(WindowPathingSnapshot.builder()
                .state(WindowPathingState.ACTIVE)
                .intent(firstIntent)
                .currentMapName("长安")
                .currentX(100)
                .currentY(70)
                .locationChangedAtMs(startedAt + 500L)
                .coordinateMovementObserved(true)
                .updatedAtMs(startedAt + 1_000L)
                .dialogBlocking(true)
                .dialogBlockingReason("expected route dialog")
                .dialogBlockingDetectedAtMs(startedAt + 750L)
                .build());

        ObservationPathingFact current = sampler.collect(List.of()).pathingFacts().get(0);
        assertEquals(TASK_RUN, current.taskRunId());
        assertEquals(WINDOW, current.windowId());
        assertEquals(HWND, current.hwnd());
        assertEquals("intent-A", current.intentId());
        assertEquals(ObservationPathingState.ACTIVE, current.state());
        assertEquals(ObservationPathingTransition.CURRENT, current.transition());
        assertEquals(120, current.targetX());
        assertEquals(88, current.targetY());
        assertTrue(current.dialogBlocking());
        assertEquals("expected route dialog", current.dialogBlockingReason());
        assertEquals(startedAt + 750L, current.dialogBlockingDetectedAtMs());

        context.clearPathingSignal("test clear");
        ObservationPathingFact cleared = sampler.collect(List.of()).pathingFacts().get(0);
        assertEquals("intent-A", cleared.intentId(), "a clear retains the exact cleared identity");
        assertEquals(ObservationPathingState.NONE, cleared.state());
        assertEquals(ObservationPathingTransition.CLEARED, cleared.transition());
        assertNull(cleared.currentMapName());
        assertFalse(cleared.dialogBlocking());
        assertNull(cleared.dialogBlockingReason());
        assertEquals(0L, cleared.dialogBlockingDetectedAtMs());

        WindowPathingIntent replacement = WindowPathingIntent.builder()
                .intentId("intent-B")
                .source("tracker:test-B")
                .type(WindowPathingIntentType.UNTARGETED_TRACKER)
                .createdAtMs(System.currentTimeMillis())
                .build();
        context.markPathingStarted(replacement);
        ObservationPathingFact replaced = sampler.collect(List.of()).pathingFacts().get(0);
        assertEquals("intent-B", replaced.intentId());
        assertEquals("intent-A", replaced.replacedIntentId());
        assertEquals(ObservationPathingTransition.REPLACED, replaced.transition());

        ObservationPathingFact repeated = sampler.collect(List.of()).pathingFacts().get(0);
        assertEquals(ObservationPathingTransition.CURRENT, repeated.transition(),
                "REPLACED is a one-frame lineage edge; subsequent samples are CURRENT");
        assertNull(repeated.replacedIntentId());
    }

    @Test
    void samplerNormalizesAPathingIntentRegisteredAfterTheObservationCycleTimestamp() {
        WindowRuntimeContext context = new WindowRuntimeContext(WINDOW, new GameContext());
        context.setNativeBinding(new WindowNativeBinding(HWND, "title", "class", 7L, 0, 0, 1024, 768));
        WindowObservationSampler sampler = sampler(context);
        long snapshotAt = System.currentTimeMillis();
        long registeredAfterCycle = snapshotAt + 5_000L;
        WindowPathingIntent intent = WindowPathingIntent.builder()
                .intentId("intent-concurrent-registration")
                .source("navigation:concurrent-registration")
                .targetMapName("灵兽村")
                .targetX(112)
                .targetY(93)
                .type(WindowPathingIntentType.TARGETED)
                .createdAtMs(registeredAfterCycle)
                .build();
        context.updatePathingSnapshot(WindowPathingSnapshot.builder()
                .state(WindowPathingState.STOPPED_AWAY)
                .intent(intent)
                .locationChangedAtMs(snapshotAt)
                .coordinateMovementObserved(true)
                .updatedAtMs(snapshotAt)
                .build());

        ObservationPathingFact fact = sampler.collect(List.of()).pathingFacts().getFirst();

        assertTrue(fact.pathingStartedAtMs() > 0L);
        assertTrue(fact.pathingUpdatedAtMs() >= fact.pathingStartedAtMs());
        assertTrue(fact.locationChangedAtMs() >= fact.pathingStartedAtMs());
        assertTrue(fact.locationChangedAtMs() <= fact.pathingUpdatedAtMs());
        assertTrue(fact.coordinateMovementObserved());
    }

    @Test
    void delayedCoordinateResponseCannotTurnAYoungCapturedFrameIntoStoppedAway() throws Exception {
        WindowRuntimeContext context = new WindowRuntimeContext(WINDOW, new GameContext());
        context.setNativeBinding(new WindowNativeBinding(HWND, "title", "class", 7L, 0, 0, 1024, 768));
        long now = System.currentTimeMillis();
        WindowPathingIntent intent = WindowPathingIntent.builder()
                .intentId("intent-delayed-response")
                .source("navigation:delayed-response")
                .targetMapName("灵兽村")
                .targetX(117)
                .targetY(69)
                .tolerance(12)
                .type(WindowPathingIntentType.TARGETED)
                .createdAtMs(now - 5_000L)
                .build();
        context.updatePathingSnapshot(WindowPathingSnapshot.builder()
                .state(WindowPathingState.ACTIVE)
                .intent(intent)
                .locationChangedAtMs(now - 1_244L)
                .coordinateMovementObserved(true)
                .updatedAtMs(now)
                .build());
        WindowObservationSampler sampler = sampler(context);
        setSamplerField(sampler, "localPathingCoordinatePending", true);
        setSamplerField(sampler, "localPathingLastChangedAtMs", 1L);
        setSamplerField(sampler, "localPathingCoordinateRequestedChangedAtMs", 1L);
        setSamplerField(sampler, "localPathingCoordinateRequestedAtMs", now - 1_100L);
        setSamplerField(sampler, "localPathingCoordinateRequestedStableMs", 1_244L);
        setSamplerField(sampler, "localPathingCoordinateRequestedIntentAgeMs", 5_000L);

        sampler.acceptAnalysisResults(List.of(new ObservationAnalysisResult(
                "analysis-delayed", "PATHING_COORDINATE_RESOLVED", "coordinate-strip",
                intent.getIntentId(), null, null, null, "灵兽村", 173, 93, null)));

        assertEquals(WindowPathingState.ACTIVE, context.getPathingSnapshot().getState(),
                "HTTP response latency must not age the captured frame into STOPPED_AWAY");
    }

    @Test
    void runnerCarriesSamplerPathingFactWithoutCloudInterestOrCommandCapture() throws Exception {
        WindowRuntimeContext context = new WindowRuntimeContext(WINDOW, new GameContext());
        context.setNativeBinding(new WindowNativeBinding(HWND, "title", "class", 7L, 0, 0, 1024, 768));
        context.markPathingStarted(WindowPathingIntent.builder()
                .intentId("intent-runner")
                .source("navigation:runner")
                .targetMapName("灵兽村")
                .type(WindowPathingIntentType.TARGETED)
                .createdAtMs(System.currentTimeMillis() - 100L)
                .build());
        ScriptedObservationClient client = new ScriptedObservationClient()
                .thenRespond(request -> response(request, List.of(), 0L, List.of()));
        WindowObservationRunner runner = new WindowObservationRunner(
                client, TENANT, DEVICE, WINDOW, HWND, TASK_CODE, TASK_RUN, sampler(context), 50L);
        try {
            runner.start();
            assertTrue(client.awaitHandled(1, Duration.ofSeconds(3)));
            ObservationRequest request = client.requests.get(0);
            assertEquals(1, request.pathingFacts().size());
            assertEquals("intent-runner", request.intentId());
            ObservationProtocolValidator.requireValid(request);
        } finally {
            stopAndAssertStopped(runner);
        }
    }

    @Test
    void samplerMapsExactDialogInterestIdentityAndEmitsOneTypedClear() {
        WindowRuntimeContext context = new WindowRuntimeContext(WINDOW, new GameContext());
        context.setNativeBinding(new WindowNativeBinding(HWND, "title", "class", 7L, 0, 0, 1024, 768));
        long now = System.currentTimeMillis();
        context.markPathingStarted(WindowPathingIntent.builder()
                .intentId("intent-dialog")
                .source("tracker:test")
                .type(WindowPathingIntentType.UNTARGETED_TRACKER)
                .createdAtMs(now - 2_000L)
                .build());
        context.updateDialogInterestWithXiuluoGreenChainSchedule(
                WindowDialogInterest.builder()
                        .taskType(TaskType.XIULUO_V2)
                        .operations(List.of(DialogOperation.XIULUO_ENTER_BATTLE))
                        .source("xiuluo:test")
                        .createdAtMs(now - 1_000L)
                        .expiresAtMs(now + 10_000L)
                        .absentAllowedAtMs(now + 1_000L)
                        .probeStartAtMs(now - 500L)
                        .localTemplateProbeOnly(true)
                        .build(),
                XiuluoGreenChainSchedule.builder()
                        .windowId(WINDOW)
                        .hwnd(HWND)
                        .observationRunId(TASK_RUN)
                        .taskRunId(TASK_RUN + ":0:XIULUO")
                        .attemptId("attempt-dialog")
                        .round(3)
                        .openedAtMs(now - 1_000L)
                        .build(),
                "observation mapping");
        WindowObservationSampler sampler = sampler(context);

        var active = sampler.collect(List.of()).dialogInterests().getFirst();
        assertEquals(TASK_RUN, active.taskRunId());
        assertEquals(WINDOW, active.windowId());
        assertEquals(HWND, active.hwnd());
        assertTrue(active.active());
        assertEquals(TaskType.XIULUO_V2.getCode(), active.taskCode());
        assertEquals(List.of(DialogOperation.XIULUO_ENTER_BATTLE.name()), active.operations());
        assertEquals("attempt-dialog", active.attemptId());
        assertEquals(3, active.round());
        assertEquals("intent-dialog", active.intentId());
        assertTrue(active.probeOnly());

        context.clearDialogInterest("observation clear");
        var cleared = sampler.collect(List.of()).dialogInterests().getFirst();
        assertFalse(cleared.active());
        assertEquals(active.interestId(), cleared.interestId());
        assertTrue(cleared.operations().isEmpty());
        assertNull(cleared.taskCode());
        assertNull(cleared.attemptId());
        assertNull(cleared.round());
        assertNull(cleared.intentId());
        assertTrue(sampler.collect(List.of()).dialogInterests().isEmpty(),
                "the typed clear is emitted exactly once");
    }

    private static WindowObservationRunner runner(ObservationClient client) {
        // 50ms parked pacing keeps the lifecycle contracts fast without changing production pacing semantics.
        return new WindowObservationRunner(client, TENANT, DEVICE, WINDOW, HWND, TASK_CODE, TASK_RUN, null, 50L);
    }

    private static WindowObservationSampler sampler(WindowRuntimeContext context) {
        WindowTaskContextHolder holder = new WindowTaskContextHolder(new WindowIsolationProperties());
        GameClientTracker tracker = new GameClientTracker(
                null, null, null, null, null, null, null, null, null, null, null) {
            @Override
            public boolean refreshWindowState() {
                return true;
            }

            @Override
            public int getWindowBaseX() {
                return 0;
            }

            @Override
            public int getWindowBaseY() {
                return 0;
            }

            @Override
            public BufferedImage captureToMemory(String elementName, int x1, int y1, int x2, int y2) {
                return new BufferedImage(x2 - x1, y2 - y1, BufferedImage.TYPE_INT_RGB);
            }
        };
        CoordinateHelper coordinateHelper = new CoordinateHelper(tracker, null);
        return new WindowObservationSampler(
                context,
                holder,
                tracker,
                coordinateHelper,
                new DialogService(tracker, coordinateHelper),
                new InputSequences(null),
                TASK_RUN,
                false);
    }

    private static void setSamplerField(WindowObservationSampler sampler,
                                        String fieldName,
                                        Object value) throws ReflectiveOperationException {
        Field field = WindowObservationSampler.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(sampler, value);
    }

    private static Object getSamplerField(WindowObservationSampler sampler,
                                          String fieldName) throws ReflectiveOperationException {
        Field field = WindowObservationSampler.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(sampler);
    }

    private static void stopAndAssertStopped(WindowObservationRunner runner) throws InterruptedException {
        runner.requestStop();
        assertTrue(runner.awaitStopped(Duration.ofSeconds(3)), "runner must stop within the bound");
        assertFalse(runner.isRunning());
    }

    private static ObservationResponse response(ObservationRequest request,
                                                List<String> acknowledgedEventIds,
                                                long interestRevision,
                                                List<ObservationInterest> interests) {
        return ObservationProtocolValidator.requireValid(
                new ObservationResponse(
                        ObservationProtocolValidator.CONTRACT_VERSION,
                        request.observerSeq(),
                        interestRevision,
                        acknowledgedEventIds,
                        interests,
                        List.of()),
                request);
    }

    private static ObservationResponse response(ObservationRequest request,
                                                List<String> acknowledgedEventIds,
                                                long interestRevision,
                                                List<ObservationInterest> interests,
                                                List<ObservationPreparedFrameDemand> demands) {
        return ObservationProtocolValidator.requireValid(
                new ObservationResponse(
                        ObservationProtocolValidator.CONTRACT_VERSION,
                        request.observerSeq(),
                        interestRevision,
                        acknowledgedEventIds,
                        interests,
                        List.of(),
                        demands),
                request);
    }

    private static byte[] fullWindowPng() throws Exception {
        BufferedImage image = new BufferedImage(
                ObservationProtocolValidator.TERMINAL_FRAME_WIDTH,
                ObservationProtocolValidator.TERMINAL_FRAME_HEIGHT,
                BufferedImage.TYPE_INT_RGB);
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            ImageIO.write(image, "PNG", output);
            return output.toByteArray();
        } finally {
            image.flush();
        }
    }

    private static ObservationKeyEvent keyEvent(String eventId) {
        return new ObservationKeyEvent(
                eventId, ObservationKeyEventType.PATHING_TERMINAL, System.currentTimeMillis(),
                null, null, null, "test", null);
    }

    private static ObservationPathingFact activePathingFact(String intentId,
                                                            long updatedAtMs,
                                                            boolean coordinateMovementObserved,
                                                            boolean dialogBlocking) {
        return new ObservationPathingFact(
                TASK_RUN, WINDOW, HWND, intentId, null, "test", "灵兽村",
                112, 93, 5, ObservationPathingType.TARGETED,
                1_000L, updatedAtMs, ObservationPathingState.ACTIVE, ObservationPathingTransition.CURRENT,
                null, null, null, 0L, coordinateMovementObserved,
                dialogBlocking, dialogBlocking ? "OPTION_DIALOG" : null,
                dialogBlocking ? updatedAtMs : 0L);
    }

    private static WindowPathingIntent pathingIntent(String intentId, String source) {
        return WindowPathingIntent.builder()
                .intentId(intentId)
                .source(source)
                .targetMapName("灵兽村")
                .targetX(112)
                .targetY(93)
                .tolerance(5)
                .type(WindowPathingIntentType.TARGETED)
                .createdAtMs(System.currentTimeMillis())
                .build();
    }

    @FunctionalInterface
    private interface ResponseStep {
        ObservationResponse respond(ObservationRequest request) throws ObservationTransportException;
    }

    /** Scripted observation transport: fixed steps, then parks until interrupted (typed INTERRUPTED failure). */
    private static final class ScriptedObservationClient implements ObservationClient {
        final List<ObservationRequest> requests = new CopyOnWriteArrayList<>();
        private final List<ResponseStep> steps = new CopyOnWriteArrayList<>();
        private final AtomicInteger handled = new AtomicInteger();
        private final Object handledMonitor = new Object();

        ScriptedObservationClient thenRespond(ResponseStep step) {
            steps.add(step);
            return this;
        }

        ScriptedObservationClient thenFail(ObservationTransportException failure) {
            steps.add(request -> {
                throw failure;
            });
            return this;
        }

        boolean awaitHandled(int count, Duration timeout) throws InterruptedException {
            long deadlineNanos = System.nanoTime() + timeout.toNanos();
            synchronized (handledMonitor) {
                while (handled.get() < count) {
                    long remainingNanos = deadlineNanos - System.nanoTime();
                    if (remainingNanos <= 0L) {
                        return false;
                    }
                    handledMonitor.wait(Math.max(1L, remainingNanos / 1_000_000L));
                }
                return true;
            }
        }

        @Override
        public ObservationResponse send(ObservationRequest request) throws ObservationTransportException {
            requests.add(request);
            int index = handled.get();
            try {
                if (index >= steps.size()) {
                    try {
                        new CountDownLatch(1).await(30, TimeUnit.SECONDS);
                        throw new AssertionError("scripted observation client exhausted without stop");
                    } catch (InterruptedException interrupted) {
                        Thread.currentThread().interrupt();
                        throw new ObservationTransportException(
                                ObservationTransportException.Kind.INTERRUPTED,
                                "test observation wait interrupted",
                                interrupted);
                    }
                }
                return steps.get(index).respond(request);
            } finally {
                synchronized (handledMonitor) {
                    handled.incrementAndGet();
                    handledMonitor.notifyAll();
                }
            }
        }
    }
}
