package com.bot.dhxy.cloud.turn;

import com.bot.dhxy.cloud.turn.protocol.TurnRequest;
import com.bot.dhxy.cloud.turn.protocol.TurnResponse;
import com.bot.dhxy.cloud.turn.protocol.TurnTaskCode;
import com.bot.dhxy.cloud.turn.protocol.TurnTaskQueueFailurePolicy;
import com.bot.dhxy.cloud.turn.protocol.TurnTaskStartAck;
import com.bot.dhxy.cloud.turn.protocol.TurnTaskStartRequest;
import com.bot.dhxy.cloud.turn.protocol.TurnWindowMetadata;
import com.bot.dhxy.cloud.turn.protocol.TurnWindowRect;
import com.bot.dhxy.cloud.turn.protocol.observation.ObservationProtocolValidator;
import com.bot.dhxy.cloud.turn.protocol.observation.ObservationKeyEvent;
import com.bot.dhxy.cloud.turn.protocol.observation.ObservationKeyEventType;
import com.bot.dhxy.cloud.turn.protocol.observation.ObservationRequest;
import com.bot.dhxy.cloud.turn.protocol.observation.ObservationResponse;
import com.bot.dhxy.window.observation.ObservationClient;
import com.bot.dhxy.window.observation.ObservationTransportException;
import com.bot.dhxy.window.observation.WindowObservationRunner;
import com.bot.dhxy.window.observation.WindowObservationRunnerFactory;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TURN-40G step-2 loop-lifecycle contracts: a runner starts only after the matching start acknowledgement, exactly
 * one runner exists per acknowledged window, loop stop terminates the runner with no leak, restart mints a fresh
 * runner, and five concurrent windows keep fully isolated runners while observation traffic travels a transport
 * object physically distinct from the turn client (so it can never occupy the command action slot).
 */
class WindowTurnLoopObservationContractTest {

    @Test
    void loopWithoutStartRequestNeverStartsAnObservationRunner() throws Exception {
        RecordingRunnerFactory factory = new RecordingRunnerFactory();
        AckEchoTurnClient turnClient = new AckEchoTurnClient(2);
        WindowTurnLoop loop = loop(turnClient, factory, "window-7");
        try {
            loop.start();
            assertTrue(turnClient.awaitBlocking(Duration.ofSeconds(3)), "the loop must reach its long wait");
            assertEquals(0, factory.createCalls.size(),
                    "a loop with no task start request must never start an observation runner");
        } finally {
            loop.stop();
            assertTrue(loop.awaitStopped(Duration.ofSeconds(3)));
        }
    }

    @Test
    void acknowledgedLoopStartsExactlyOneRunnerBoundToTheAcknowledgedIdentity() throws Exception {
        RecordingRunnerFactory factory = new RecordingRunnerFactory();
        AckEchoTurnClient turnClient = new AckEchoTurnClient(3);
        WindowTurnLoop loop = loop(turnClient, factory, "window-7");
        loop.attachStartRequest(startRequest("start-obs-1"));
        try {
            loop.start();
            assertTrue(loop.awaitStartAcknowledged(Duration.ofSeconds(3)));
            assertTrue(turnClient.awaitBlocking(Duration.ofSeconds(3)),
                    "the loop must keep turning after the acknowledgement");
            assertEquals(1, factory.createCalls.size(),
                    "exactly one observation runner starts per acknowledged window across multiple turns");
            RunnerCreation creation = factory.createCalls.get(0);
            assertEquals("window-7", creation.windowId());
            assertEquals("12345", creation.hwnd(), "the runner binds the acknowledged native handle");
            assertEquals("start-obs-1", creation.taskRunId(),
                    "the acknowledged start request id is the observation run identity");
            assertEquals(TurnTaskCode.XIULUO_V2.name(), creation.taskCode());
            WindowObservationRunner runner = loop.observationRunner();
            assertNotNull(runner);
            assertTrue(runner.isRunning(), "the runner must be live while the acknowledged loop runs");
        } finally {
            loop.stop();
            assertTrue(loop.awaitStopped(Duration.ofSeconds(3)));
        }
        assertEquals(1, factory.createCalls.size());
    }

    @Test
    void observationRunnerUsesCloudEffectiveTaskProjectionInsteadOfRequestedTeamTask() throws Exception {
        RecordingRunnerFactory factory = new RecordingRunnerFactory();
        AckEchoTurnClient turnClient = new AckEchoTurnClient(3, List.of(TurnTaskCode.AUTO_BATTLE));
        WindowTurnLoop loop = loop(turnClient, factory, "window-member");
        loop.attachStartRequest(startRequest("start-member-effective"));
        try {
            loop.start();
            assertTrue(loop.awaitStartAcknowledged(Duration.ofSeconds(3)));
            assertTrue(turnClient.awaitBlocking(Duration.ofSeconds(3)));
            assertEquals(1, factory.createCalls.size());
            assertEquals(TurnTaskCode.AUTO_BATTLE.name(), factory.createCalls.get(0).taskCode(),
                    "local runner wiring must use the effective member assignment carried by Cloud ACK");
        } finally {
            loop.stop();
            assertTrue(loop.awaitStopped(Duration.ofSeconds(3)));
        }
    }

    @Test
    void loopStopTerminatesTheRunnerWithNoLeak() throws Exception {
        RecordingRunnerFactory factory = new RecordingRunnerFactory();
        AckEchoTurnClient turnClient = new AckEchoTurnClient(2);
        WindowTurnLoop loop = loop(turnClient, factory, "window-7");
        loop.attachStartRequest(startRequest("start-obs-stop"));
        loop.start();
        assertTrue(loop.awaitStartAcknowledged(Duration.ofSeconds(3)));
        assertTrue(turnClient.awaitBlocking(Duration.ofSeconds(3)));
        WindowObservationRunner runner = loop.observationRunner();
        assertNotNull(runner);

        loop.stop();
        assertTrue(loop.awaitStopped(Duration.ofSeconds(3)));

        assertFalse(runner.isRunning(), "the loop's terminal path must terminate its observation runner");
        assertTrue(factory.observationRequestsFor("window-7").stream()
                        .allMatch(request -> "start-obs-stop".equals(request.taskRunId())),
                "every observation request carries the exact acknowledged run identity");
    }

    @Test
    void userPauseStopsObservationAndKeepsItStoppedUntilResumeValidation() throws Exception {
        RecordingRunnerFactory factory = new RecordingRunnerFactory();
        AckEchoTurnClient turnClient = new AckEchoTurnClient(2);
        WindowTurnLoop loop = loop(turnClient, factory, "window-pause");
        loop.attachStartRequest(startRequest("start-obs-pause"));
        try {
            loop.start();
            assertTrue(loop.awaitStartAcknowledged(Duration.ofSeconds(3)));
            assertTrue(turnClient.awaitBlocking(Duration.ofSeconds(3)));
            WindowObservationRunner runner = loop.observationRunner();
            assertNotNull(runner);

            loop.requestPause();

            assertFalse(runner.isRunning(), "user pause must stop the resident observer immediately");
            assertSame(runner, loop.observationRunner(),
                    "pause retains the acknowledged runner so resume cannot reset sequence or typed-fact lineage");
            int requestCountAtPause = factory.observationRequestsFor("window-pause").size();
            Thread.sleep(150L);
            assertEquals(requestCountAtPause, factory.observationRequestsFor("window-pause").size(),
                    "paused observation must not emit another heartbeat or sample");
        } finally {
            loop.stop();
            assertTrue(loop.awaitStopped(Duration.ofSeconds(3)));
        }
    }

    @Test
    void blockedExchangePauseResumePromptlyRestartsRetainedRunnerAndDeliversCombatEvent() throws Exception {
        RecordingRunnerFactory factory = new RecordingRunnerFactory();
        PauseResumeBlockingTurnClient turnClient = new PauseResumeBlockingTurnClient();
        WindowTurnLoop loop = loop(turnClient, factory, "window-pause-resume");
        loop.attachStartRequest(startRequest("start-pause-resume"));
        try {
            loop.start();
            assertTrue(loop.awaitStartAcknowledged(Duration.ofSeconds(3)));
            assertTrue(turnClient.awaitInitialLongWait(Duration.ofSeconds(3)),
                    "the acknowledged loop must be blocked in the normal Cloud long wait");
            WindowObservationRunner retained = loop.observationRunner();
            assertNotNull(retained);

            loop.requestPause();

            assertTrue(turnClient.awaitPauseCheckpoint(Duration.ofSeconds(3)),
                    "pause must cancel the blocked wait and promptly publish its checkpoint");
            assertFalse(retained.isRunning());
            assertSame(retained, loop.observationRunner(),
                    "pause must retain the acknowledged runner and its observation lineage");
            assertEquals(1, turnClient.pauseCheckpointCount(),
                    "the pause checkpoint must be published exactly once");
            assertEquals(0L, turnClient.pauseCheckpointWaitTimeoutMs(),
                    "the pause checkpoint must disable Cloud long waiting");

            loop.requestResume();

            assertTrue(turnClient.awaitResumedLongWait(Duration.ofSeconds(3)),
                    "resume must promptly reach a new normal Cloud wait");
            assertTrue(turnClient.resumedNormalWaitTimeoutMs() > 0L,
                    "the resumed normal turn must restore the configured positive long wait");
            assertSame(retained, loop.observationRunner(),
                    "resume must restart the same retained observation runner");
            assertTrue(retained.isRunning(),
                    "runner must already be running while the resumed Cloud exchange is blocked");

            String combatEventId = "combat-after-resume";
            retained.publishKeyEvent(new ObservationKeyEvent(
                    combatEventId,
                    ObservationKeyEventType.IN_COMBAT,
                    System.currentTimeMillis(),
                    null,
                    null,
                    null,
                    "test-after-resume",
                    "combat edge sampled after resume",
                    "claim-after-resume",
                    1L,
                    "XIULUO_V2",
                    "business-after-resume"));
            assertTrue(awaitCondition(Duration.ofSeconds(3), () ->
                            factory.observationRequestsFor("window-pause-resume").stream()
                                    .anyMatch(request -> request.events().stream()
                                            .anyMatch(event -> combatEventId.equals(event.eventId())
                                                    && event.eventType() == ObservationKeyEventType.IN_COMBAT))),
                    "a combat typed event published after resume must reach the existing observation transport");
            assertTrue(factory.observationRequestsFor("window-pause-resume").stream()
                            .allMatch(request -> "start-pause-resume".equals(request.taskRunId())),
                    "pause/resume must preserve the exact acknowledged task-run identity");
            assertEquals(1, turnClient.pauseCheckpointCount(),
                    "resume must not republish the pause checkpoint");
        } finally {
            loop.stop();
            assertTrue(loop.awaitStopped(Duration.ofSeconds(3)));
        }
    }

    @Test
    void restartOfAnAcknowledgedLoopMintsAFreshRunnerAndTheOldOneStaysStopped() throws Exception {
        RecordingRunnerFactory factory = new RecordingRunnerFactory();
        AckEchoTurnClient turnClient = new AckEchoTurnClient(2);
        WindowTurnLoop loop = loop(turnClient, factory, "window-7");
        loop.attachStartRequest(startRequest("start-obs-restart"));
        loop.start();
        assertTrue(loop.awaitStartAcknowledged(Duration.ofSeconds(3)));
        assertTrue(turnClient.awaitBlocking(Duration.ofSeconds(3)));
        WindowObservationRunner first = loop.observationRunner();
        assertNotNull(first);
        loop.stop();
        assertTrue(loop.awaitStopped(Duration.ofSeconds(3)));
        assertFalse(first.isRunning());

        turnClient.allowMore(2);
        loop.start();
        try {
            assertTrue(turnClient.awaitBlocking(Duration.ofSeconds(3)),
                    "the restarted acknowledged loop must turn again");
            WindowObservationRunner second = loop.observationRunner();
            assertNotNull(second, "an already-acknowledged restarted loop restarts observation");
            assertNotSame(first, second, "a stopped runner is never reused");
            assertTrue(second.isRunning());
            assertFalse(first.isRunning(), "the replaced runner must stay stopped");
            assertEquals(2, factory.createCalls.size());
        } finally {
            loop.stop();
            assertTrue(loop.awaitStopped(Duration.ofSeconds(3)));
        }
    }

    @Test
    void fiveConcurrentWindowsKeepExactlyOneIsolatedRunnerEach() throws Exception {
        int windows = 5;
        RecordingRunnerFactory factory = new RecordingRunnerFactory();
        WindowTurnLoop[] loops = new WindowTurnLoop[windows];
        AckEchoTurnClient[] clients = new AckEchoTurnClient[windows];
        try {
            for (int i = 0; i < windows; i++) {
                clients[i] = new AckEchoTurnClient(2);
                loops[i] = loop(clients[i], factory, "window-" + i);
                loops[i].attachStartRequest(startRequest("start-multi-" + i));
                loops[i].start();
            }
            for (int i = 0; i < windows; i++) {
                assertTrue(loops[i].awaitStartAcknowledged(Duration.ofSeconds(3)),
                        "window-" + i + " must be acknowledged");
            }
            for (int i = 0; i < windows; i++) {
                assertTrue(clients[i].awaitBlocking(Duration.ofSeconds(3)),
                        "window-" + i + " must keep turning after its acknowledgement");
            }
            assertEquals(windows, factory.createCalls.size(), "each acknowledged window has exactly one runner");
            for (int i = 0; i < windows; i++) {
                String windowId = "window-" + i;
                assertEquals(1, factory.createCalls.stream()
                                .filter(creation -> windowId.equals(creation.windowId())).count(),
                        windowId + " must have exactly one runner");
                assertTrue(factory.observationRequestsFor(windowId).stream()
                                .allMatch(request -> ("start-multi-" + windowId.charAt(windowId.length() - 1))
                                        .equals(request.taskRunId())),
                        windowId + " observation requests must never carry another window's run identity");
            }
        } finally {
            for (int i = 0; i < windows; i++) {
                if (loops[i] != null) {
                    loops[i].stop();
                    assertTrue(loops[i].awaitStopped(Duration.ofSeconds(3)));
                }
            }
        }
        for (RunnerCreation creation : factory.createCalls) {
            assertFalse(creation.runner().isRunning(), "no runner may leak after its loop stops");
        }
    }

    // ---- fixtures ----

    private static WindowTurnLoop loop(TurnClient turnClient,
                                       WindowObservationRunnerFactory factory,
                                       String windowId) {
        return new WindowTurnLoop(
                "device",
                windowId,
                60_000L,
                () -> authorityMetadata(windowId),
                turnClient,
                action -> {
                    throw new AssertionError("observation lifecycle contracts never dispatch an action");
                },
                factory);
    }

    private static TurnTaskStartRequest startRequest(String id) {
        return new TurnTaskStartRequest(
                id, List.of(TurnTaskCode.XIULUO_V2), List.of(0), TurnTaskQueueFailurePolicy.CONTINUE_ON_FAILURE);
    }

    private static boolean awaitCondition(Duration timeout, BooleanSupplier condition) throws InterruptedException {
        long deadlineNanos = System.nanoTime() + timeout.toNanos();
        while (System.nanoTime() < deadlineNanos) {
            if (condition.getAsBoolean()) {
                return true;
            }
            Thread.sleep(10L);
        }
        return condition.getAsBoolean();
    }

    /** Authority-complete metadata for a start-bearing turn (role present, team facts truthfully absent). */
    private static TurnWindowMetadata authorityMetadata(String windowId) {
        return new TurnWindowMetadata(
                "device",
                windowId,
                "game-window-7",
                "12345",
                88L,
                new TurnWindowRect(100, 200, 8, 6),
                false,
                false,
                null,
                "LEADER",
                null,
                null,
                false,
                false,
                "NORMAL");
    }

    private record RunnerCreation(String deviceId,
                                  String windowId,
                                  String hwnd,
                                  String taskCode,
                                  String taskRunId,
                                  WindowObservationRunner runner,
                                  RecordingObservationClient observationClient) {
    }

    /** Real runners over scripted per-window observation transports; records every creation. */
    private static final class RecordingRunnerFactory implements WindowObservationRunnerFactory {
        final List<RunnerCreation> createCalls = new CopyOnWriteArrayList<>();

        @Override
        public WindowObservationRunner create(String deviceId,
                                              String windowId,
                                              String hwnd,
                                              String taskCode,
                                              String taskRunId) {
            RecordingObservationClient observationClient = new RecordingObservationClient();
            WindowObservationRunner runner = new WindowObservationRunner(
                    observationClient, "tenant-1", deviceId, windowId, hwnd, taskCode, taskRunId);
            createCalls.add(new RunnerCreation(
                    deviceId, windowId, hwnd, taskCode, taskRunId, runner, observationClient));
            return runner;
        }

        List<ObservationRequest> observationRequestsFor(String windowId) {
            return createCalls.stream()
                    .filter(creation -> creation.windowId().equals(windowId))
                    .flatMap(creation -> creation.observationClient().requests.stream())
                    .toList();
        }
    }

    /** Accepts every observation request (no interests issued); physically separate from any turn client. */
    private static final class RecordingObservationClient implements ObservationClient {
        final List<ObservationRequest> requests = new CopyOnWriteArrayList<>();

        @Override
        public ObservationResponse send(ObservationRequest request) throws ObservationTransportException {
            requests.add(request);
            return ObservationProtocolValidator.requireValid(
                    new ObservationResponse(
                            ObservationProtocolValidator.CONTRACT_VERSION,
                            request.observerSeq(),
                            0L,
                            request.events().stream().map(event -> event.eventId()).toList(),
                            List.of(),
                            List.of()),
                    request);
        }
    }

    /**
     * Echoes a matching start ack on start-bearing turns, answers a bounded number of turns, then parks in the
     * long wait until interrupted. A blocked (interrupted) turn never consumes the response allowance, and
     * {@link #allowMore(int)} re-arms the client for an explicit loop restart.
     */
    private static final class AckEchoTurnClient implements TurnClient {
        final List<TurnRequest> requests = new CopyOnWriteArrayList<>();
        private final java.util.concurrent.atomic.AtomicInteger allowance;
        private final java.util.concurrent.atomic.AtomicInteger responded =
                new java.util.concurrent.atomic.AtomicInteger();
        private volatile CountDownLatch blocking = new CountDownLatch(1);
        private final List<TurnTaskCode> effectiveTaskCodes;

        AckEchoTurnClient(int initialAllowance) {
            this(initialAllowance, null);
        }

        AckEchoTurnClient(int initialAllowance, List<TurnTaskCode> effectiveTaskCodes) {
            this.allowance = new java.util.concurrent.atomic.AtomicInteger(initialAllowance);
            this.effectiveTaskCodes = effectiveTaskCodes;
        }

        void allowMore(int additionalTurns) {
            allowance.addAndGet(additionalTurns);
            blocking = new CountDownLatch(1);
        }

        boolean awaitBlocking(Duration timeout) throws InterruptedException {
            return blocking.await(timeout.toMillis(), TimeUnit.MILLISECONDS);
        }

        @Override
        public TurnExchangeResult exchange(TurnRequest request, byte[] optionalPng) throws TurnTransportException {
            requests.add(request);
            if (responded.get() >= allowance.get()) {
                blocking.countDown();
                try {
                    new CountDownLatch(1).await();
                    throw new AssertionError("blocking fake unexpectedly released");
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    throw new TurnTransportException(
                            TurnTransportException.Kind.INTERRUPTED, "test long wait interrupted", interrupted);
                }
            }
            responded.incrementAndGet();
            TurnTaskStartRequest startRequest = request.taskStartRequest();
            TurnResponse response = startRequest == null
                    ? new TurnResponse(TurnResponse.Status.IDLE, null)
                    : new TurnResponse(TurnResponse.Status.IDLE, null,
                            new TurnTaskStartAck(startRequest.startRequestId(), effectiveTaskCodes));
            return TurnExchangeResult.accepted(response);
        }

        @Override
        public TurnTemplateDownload downloadTemplate(String templateKey, String ifNoneMatch) {
            throw new AssertionError("unexpected template download");
        }
    }

    /**
     * First exchange acknowledges the task, second blocks until pause interrupts it, the pause-bearing exchange
     * responds immediately, and the resumed normal exchange blocks again. This freezes the production ordering:
     * retained runner restart must happen before the resumed long wait is entered.
     */
    private static final class PauseResumeBlockingTurnClient implements TurnClient {
        private final List<TurnRequest> requests = new CopyOnWriteArrayList<>();
        private final CountDownLatch initialLongWait = new CountDownLatch(1);
        private final CountDownLatch pauseCheckpoint = new CountDownLatch(1);
        private final CountDownLatch resumedLongWait = new CountDownLatch(1);
        private final java.util.concurrent.atomic.AtomicBoolean acknowledged =
                new java.util.concurrent.atomic.AtomicBoolean();

        boolean awaitInitialLongWait(Duration timeout) throws InterruptedException {
            return initialLongWait.await(timeout.toMillis(), TimeUnit.MILLISECONDS);
        }

        boolean awaitPauseCheckpoint(Duration timeout) throws InterruptedException {
            return pauseCheckpoint.await(timeout.toMillis(), TimeUnit.MILLISECONDS);
        }

        boolean awaitResumedLongWait(Duration timeout) throws InterruptedException {
            return resumedLongWait.await(timeout.toMillis(), TimeUnit.MILLISECONDS);
        }

        long pauseCheckpointCount() {
            return requests.stream().filter(request -> request.window().pauseRequested()).count();
        }

        long pauseCheckpointWaitTimeoutMs() {
            return requests.stream()
                    .filter(request -> request.window().pauseRequested())
                    .mapToLong(TurnRequest::waitTimeoutMs)
                    .findFirst()
                    .orElseThrow();
        }

        long resumedNormalWaitTimeoutMs() {
            return requests.stream()
                    .filter(request -> request.taskStartRequest() == null)
                    .filter(request -> !request.window().pauseRequested())
                    .skip(1L)
                    .mapToLong(TurnRequest::waitTimeoutMs)
                    .findFirst()
                    .orElseThrow();
        }

        @Override
        public TurnExchangeResult exchange(TurnRequest request, byte[] optionalPng) throws TurnTransportException {
            requests.add(request);
            if (acknowledged.compareAndSet(false, true)) {
                TurnTaskStartRequest startRequest = request.taskStartRequest();
                if (request.waitTimeoutMs() <= 0L) {
                    throw new AssertionError("the normal start request must retain a positive wait");
                }
                return TurnExchangeResult.accepted(new TurnResponse(
                        TurnResponse.Status.IDLE,
                        null,
                        new TurnTaskStartAck(startRequest.startRequestId(), List.of(TurnTaskCode.XIULUO_V2))));
            }
            if (request.waitTimeoutMs() == 0L) {
                if (!request.window().pauseRequested()) {
                    throw new AssertionError("only a pause checkpoint may disable long waiting");
                }
                pauseCheckpoint.countDown();
                return TurnExchangeResult.accepted(new TurnResponse(TurnResponse.Status.IDLE, null));
            }
            if (request.window().pauseRequested()) {
                throw new AssertionError("a pause checkpoint with positive wait would block in production");
            }
            CountDownLatch entered = initialLongWait.getCount() > 0L ? initialLongWait : resumedLongWait;
            entered.countDown();
            try {
                new CountDownLatch(1).await();
                throw new AssertionError("blocking fake unexpectedly released");
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                throw new TurnTransportException(
                        TurnTransportException.Kind.INTERRUPTED,
                        "test long wait interrupted",
                        interrupted);
            }
        }

        @Override
        public TurnTemplateDownload downloadTemplate(String templateKey, String ifNoneMatch) {
            throw new AssertionError("unexpected template download");
        }
    }
}
