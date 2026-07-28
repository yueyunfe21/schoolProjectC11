package com.bot.dhxy.window.observation;

import com.bot.dhxy.cloud.turn.protocol.observation.ObservationFact;
import com.bot.dhxy.cloud.turn.protocol.observation.ObservationInterest;
import com.bot.dhxy.cloud.turn.protocol.observation.ObservationKeyEvent;
import com.bot.dhxy.cloud.turn.protocol.observation.ObservationPathingFact;
import com.bot.dhxy.cloud.turn.protocol.observation.ObservationPreparedFrame;
import com.bot.dhxy.cloud.turn.protocol.observation.ObservationPreparedFrameDemand;
import com.bot.dhxy.cloud.turn.protocol.observation.ObservationProtocolValidator;
import com.bot.dhxy.cloud.turn.protocol.observation.ObservationRequest;
import com.bot.dhxy.cloud.turn.protocol.observation.ObservationResponse;
import com.bot.dhxy.cloud.turn.protocol.observation.ObservationRoi;
import com.bot.dhxy.cloud.turn.protocol.observation.ObservationTerminalFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * TURN-40G: one resident observation runner per Cloud-acknowledged window. The runner communicates exclusively over
 * the independent observation plane — it never touches the turn command slot and never focuses a window. Its sampler
 * performs exact-HWND background ROI capture and local mechanical state detection but no input. It maintains a
 * monotonic {@code observerSeq}, keeps at most one
 * request in flight by construction (a single worker thread with synchronous sends), retains key events until the
 * Cloud acknowledges them, parks at a slow heartbeat when the Cloud has issued no interests, and treats transport
 * failure as transport failure only — never as a business fact.
 */
public final class WindowObservationRunner {

    private static final Logger log = LoggerFactory.getLogger(WindowObservationRunner.class);

    /** Slow keep-alive/interest-poll period while the Cloud has issued no observation interests (parked). */
    static final long PARKED_HEARTBEAT_PERIOD_MS = 5_000L;
    /** Fastest allowed sampling period regardless of interest configuration. */
    static final long MIN_SAMPLE_PERIOD_MS = 100L;
    /** Bounded unacknowledged key-event retention (oldest evicted with a warning; never an unbounded queue). */
    static final int MAX_PENDING_KEY_EVENTS = 64;

    private static final String RUNNER_SOURCE = "window-observation-runner";

    private final Object lifecycleMonitor = new Object();
    private final Object pacer = new Object();
    private final ObservationClient client;
    private final String tenantId;
    private final String deviceId;
    private final String windowId;
    private final String hwnd;
    private final String taskCode;
    private final String taskRunId;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicBoolean stopRequested = new AtomicBoolean(false);
    private final AtomicBoolean retainStateOnStop = new AtomicBoolean(false);
    private final AtomicLong observerSeq = new AtomicLong();

    private volatile Thread workerThread;
    private volatile long interestRevision;
    private volatile List<ObservationInterest> interests = List.of();
    private volatile long lastTransportFailureAtMs;
    private volatile long successfulSendCount;
    private volatile long consecutiveTransportFailures;

    /** Unacknowledged key events by eventId, insertion-ordered; guarded by its own monitor. */
    private final LinkedHashMap<String, ObservationKeyEvent> pendingKeyEvents = new LinkedHashMap<>();
    /** Set when new work arrives while a send is in progress so the next pace skips its wait; guarded by pacer. */
    private boolean wakePending;

    private final long parkedHeartbeatPeriodMs;
    /** Optional local sampler executing Cloud-issued interests; null runs a pure heartbeat/event transport. */
    private final WindowObservationSampler sampler;
    /** Optional local-only patrol. It has no observation transport and cannot add request payload. */
    private final LocalMaintenanceBroadcastRunner localMaintenanceRunner;
    /** Dedicated exact-window one-shot path; ordinary ROI capture remains bounded. */
    private final PreparedFrameCapture preparedFrameCapture;
    private volatile ObservationPreparedFrameDemand preparedFrameDemand;
    private volatile ObservationPreparedFrame retainedPreparedFrame;

    public WindowObservationRunner(ObservationClient client,
                                   String tenantId,
                                   String deviceId,
                                   String windowId,
                                   String hwnd,
                                   String taskCode,
                                   String taskRunId) {
        this(client, tenantId, deviceId, windowId, hwnd, taskCode, taskRunId,
                null, null, PARKED_HEARTBEAT_PERIOD_MS);
    }

    public WindowObservationRunner(ObservationClient client,
                                   String tenantId,
                                   String deviceId,
                                   String windowId,
                                   String hwnd,
                                   String taskCode,
                                   String taskRunId,
                                   WindowObservationSampler sampler) {
        this(client, tenantId, deviceId, windowId, hwnd, taskCode, taskRunId,
                sampler, null, PARKED_HEARTBEAT_PERIOD_MS);
    }

    /** Package-visible pacing override so lifecycle contract tests run without multi-second parked waits. */
    WindowObservationRunner(ObservationClient client,
                            String tenantId,
                            String deviceId,
                            String windowId,
                            String hwnd,
                            String taskCode,
                            String taskRunId,
                            WindowObservationSampler sampler,
                            long parkedHeartbeatPeriodMs) {
        this(client, tenantId, deviceId, windowId, hwnd, taskCode, taskRunId,
                sampler, null, parkedHeartbeatPeriodMs);
    }

    WindowObservationRunner(ObservationClient client,
                            String tenantId,
                            String deviceId,
                            String windowId,
                            String hwnd,
                            String taskCode,
                            String taskRunId,
                            WindowObservationSampler sampler,
                            LocalMaintenanceBroadcastRunner localMaintenanceRunner,
                            long parkedHeartbeatPeriodMs) {
        this(client, tenantId, deviceId, windowId, hwnd, taskCode, taskRunId,
                sampler, localMaintenanceRunner, null, parkedHeartbeatPeriodMs);
    }

    WindowObservationRunner(ObservationClient client,
                            String tenantId,
                            String deviceId,
                            String windowId,
                            String hwnd,
                            String taskCode,
                            String taskRunId,
                            WindowObservationSampler sampler,
                            LocalMaintenanceBroadcastRunner localMaintenanceRunner,
                            PreparedFrameCapture preparedFrameCapture,
                            long parkedHeartbeatPeriodMs) {
        this.client = Objects.requireNonNull(client, "client");
        this.tenantId = requireIdentity(tenantId, "tenantId");
        this.deviceId = requireIdentity(deviceId, "deviceId");
        this.windowId = requireIdentity(windowId, "windowId");
        this.hwnd = requireIdentity(hwnd, "hwnd");
        this.taskCode = requireIdentity(taskCode, "taskCode");
        this.taskRunId = requireIdentity(taskRunId, "taskRunId");
        this.sampler = sampler;
        if (sampler != null) {
            sampler.bindAsyncEventPublisher(this::publishKeyEvent);
        }
        this.localMaintenanceRunner = localMaintenanceRunner;
        this.preparedFrameCapture = preparedFrameCapture;
        if (parkedHeartbeatPeriodMs <= 0L) {
            throw new IllegalArgumentException("parkedHeartbeatPeriodMs must be positive");
        }
        this.parkedHeartbeatPeriodMs = parkedHeartbeatPeriodMs;
    }

    /** Starts this window's observation daemon. Called only after the matching start acknowledgement. */
    public void start() {
        synchronized (lifecycleMonitor) {
            if (!running.compareAndSet(false, true)) {
                throw new IllegalStateException("observation runner is already running for windowId=" + windowId);
            }
            retainStateOnStop.set(false);
            stopRequested.set(false);
            Thread thread = new Thread(this::runLoop, "dhxy-observe-" + windowId);
            thread.setDaemon(true);
            workerThread = thread;
            try {
                if (localMaintenanceRunner != null) {
                    localMaintenanceRunner.start();
                }
                thread.start();
            } catch (RuntimeException | Error startFailure) {
                if (localMaintenanceRunner != null) {
                    localMaintenanceRunner.requestStop();
                }
                workerThread = null;
                running.set(false);
                lifecycleMonitor.notifyAll();
                throw startFailure;
            }
        }
    }

    /**
     * Fences new sampling and requests bounded termination: no new request starts after this, and an in-flight
     * send is interrupted rather than awaited.
     */
    public void requestStop() {
        retainStateOnStop.set(false);
        requestThreadStop();
        if (!running.get()) {
            resetRetainedState();
        }
    }

    /**
     * Stops observation traffic for a user pause while retaining this acknowledged run's sequence, interests,
     * unacknowledged events and sampler lineage. The same runner must be restarted after resume validation.
     */
    public void requestSuspend() {
        retainStateOnStop.set(true);
        if (sampler != null) {
            sampler.invalidateTerminalFrameForSuspend();
        }
        requestThreadStop();
    }

    private void requestThreadStop() {
        synchronized (lifecycleMonitor) {
            stopRequested.set(true);
            Thread thread = workerThread;
            if (thread != null) {
                thread.interrupt();
            }
        }
        synchronized (pacer) {
            pacer.notifyAll();
        }
        if (localMaintenanceRunner != null) {
            localMaintenanceRunner.requestStop();
        }
    }

    /** Waits for the runner thread to terminate within the bound. */
    public boolean awaitStopped(Duration timeout) throws InterruptedException {
        if (timeout == null || timeout.isZero() || timeout.isNegative()) {
            throw new IllegalArgumentException("timeout must be positive");
        }
        long deadlineNanos = System.nanoTime() + timeout.toNanos();
        synchronized (lifecycleMonitor) {
            while (running.get()) {
                long remainingNanos = deadlineNanos - System.nanoTime();
                if (remainingNanos <= 0L) {
                    return false;
                }
                lifecycleMonitor.wait(Math.max(1L, Duration.ofNanos(remainingNanos).toMillis()));
            }
            if (localMaintenanceRunner == null) {
                return true;
            }
        }
        long remainingNanos = deadlineNanos - System.nanoTime();
        return remainingNanos > 0L
                && localMaintenanceRunner.awaitStopped(Duration.ofNanos(remainingNanos));
    }

    public boolean isRunning() {
        return running.get();
    }

    public String windowId() {
        return windowId;
    }

    public String taskRunId() {
        return taskRunId;
    }

    public long observerSeq() {
        return observerSeq.get();
    }

    public long currentInterestRevision() {
        return interestRevision;
    }

    public List<ObservationInterest> currentInterests() {
        return interests;
    }

    public long lastTransportFailureAtMs() {
        return lastTransportFailureAtMs;
    }

    /** Number of key events still awaiting Cloud acknowledgement. */
    public int pendingKeyEventCount() {
        synchronized (pendingKeyEvents) {
            return pendingKeyEvents.size();
        }
    }

    /**
     * Publishes one critical edge. The event is retained and resent on every request until the Cloud acknowledges
     * its id; retention is bounded, and a duplicate id replaces the retained copy (idempotent on the Cloud side).
     */
    public void publishKeyEvent(ObservationKeyEvent event) {
        Objects.requireNonNull(event, "event");
        requireIdentity(event.eventId(), "event.eventId");
        synchronized (pendingKeyEvents) {
            pendingKeyEvents.put(event.eventId(), event);
            while (pendingKeyEvents.size() > MAX_PENDING_KEY_EVENTS) {
                Iterator<String> eldest = pendingKeyEvents.keySet().iterator();
                String dropped = eldest.next();
                eldest.remove();
                log.warn("Observation runner dropped oldest unacknowledged key event (bounded retention): "
                        + "windowId={} eventId={}", windowId, dropped);
            }
        }
        wakeForLocalStateChange();
    }

    /**
     * Wakes this runner after the bound runtime records local state that changes its sampling cadence.
     * The wake carries no business fact by itself; the next sampler pass reads the authoritative runtime state.
     */
    public void wakeForLocalStateChange() {
        synchronized (pacer) {
            wakePending = true;
            pacer.notifyAll();
        }
    }

    private void runLoop() {
        log.info("Observation runner started: deviceId={} windowId={} taskRunId={}", deviceId, windowId, taskRunId);
        try {
            while (!stopRequested.get() && !Thread.currentThread().isInterrupted()) {
                sendOnce();
                pace();
            }
        } catch (RuntimeException unexpected) {
            if (!stopRequested.get()) {
                log.error("Observation runner stopped after unexpected failure: windowId={} type={} message={}",
                        windowId, unexpected.getClass().getSimpleName(), unexpected.getMessage());
            }
        } finally {
            if (!retainStateOnStop.get()) {
                resetRetainedState();
            }
            synchronized (lifecycleMonitor) {
                workerThread = null;
                running.set(false);
                lifecycleMonitor.notifyAll();
            }
            long coordinateFramesCaptured = sampler == null ? 0L : sampler.coordinateFramesCaptured();
            long coordinateFramesUnavailable = sampler == null ? 0L : sampler.coordinateFramesUnavailable();
            log.info("Observation runner stopped: deviceId={} windowId={} taskRunId={} stopRequested={} observerSeq={} interestRevision={} interests={} successfulSends={} coordinateFramesCaptured={} coordinateFramesUnavailable={} consecutiveTransportFailures={} lastTransportFailureAtMs={}",
                    deviceId, windowId, taskRunId, stopRequested.get(), observerSeq.get(), interestRevision,
                    interests.size(), successfulSendCount, coordinateFramesCaptured, coordinateFramesUnavailable,
                    consecutiveTransportFailures, lastTransportFailureAtMs);
        }
    }

    private void resetRetainedState() {
        if (sampler != null) {
            sampler.reset();
        }
        synchronized (pendingKeyEvents) {
            pendingKeyEvents.clear();
        }
        interests = List.of();
        interestRevision = 0L;
        preparedFrameDemand = null;
        retainedPreparedFrame = null;
    }

    private void sendOnce() {
        if (stopRequested.get()) {
            return;
        }
        // Execute due Cloud-issued interests first: fresh facts/ROIs are latest-wins for this request, while key
        // edges enter the bounded retention so they survive transport failure until acknowledged.
        List<ObservationFact> factsForThisRequest = List.of();
        List<ObservationPathingFact> pathingFactsForThisRequest = List.of();
        List<com.bot.dhxy.cloud.turn.protocol.observation.ObservationDialogInterestFact>
                dialogInterestsForThisRequest = List.of();
        List<com.bot.dhxy.cloud.turn.protocol.observation.ObservationPreparedDialogFact>
                preparedDialogsForThisRequest = List.of();
        List<ObservationRoi> roisForThisRequest = List.of();
        List<ObservationTerminalFrame> terminalFramesForThisRequest = List.of();
        List<ObservationPreparedFrame> preparedFramesForThisRequest = List.of();
        long requestObserverSeq = observerSeq.incrementAndGet();
        if (sampler != null) {
            try {
                WindowObservationSampler.SampleBatch batch = sampler.collect(interests, requestObserverSeq);
                factsForThisRequest = batch.facts();
                pathingFactsForThisRequest = batch.pathingFacts().stream()
                        .filter(fact -> taskRunId.equals(fact.taskRunId())
                                && windowId.equals(fact.windowId())
                                && hwnd.equals(fact.hwnd()))
                        .toList();
                dialogInterestsForThisRequest = batch.dialogInterests().stream()
                        .filter(fact -> taskRunId.equals(fact.taskRunId())
                                && windowId.equals(fact.windowId())
                                && hwnd.equals(fact.hwnd()))
                        .toList();
                preparedDialogsForThisRequest = batch.preparedDialogs().stream()
                        .filter(fact -> taskRunId.equals(fact.taskRunId())
                                && windowId.equals(fact.windowId())
                                && hwnd.equals(fact.hwnd())
                                && requestObserverSeq == fact.observerSeq())
                        .toList();
                roisForThisRequest = batch.rois();
                String sampledIntentId = pathingFactsForThisRequest.isEmpty()
                        ? null : pathingFactsForThisRequest.getFirst().intentId();
                terminalFramesForThisRequest = batch.terminalFrames().stream()
                        .filter(frame -> frame != null
                                && Objects.equals(sampledIntentId, frame.intentId()))
                        .map(frame -> new ObservationTerminalFrame(
                                frame.frameId(),
                                frame.pathingGeneration(),
                                tenantId,
                                deviceId,
                                windowId,
                                hwnd,
                                taskRunId,
                                frame.intentId(),
                                0,
                                0,
                                frame.width(),
                                frame.height(),
                                "PNG",
                                frame.capturedAtMs(),
                                frame.pngBytes()))
                        .toList();
                for (ObservationKeyEvent edge : batch.events()) {
                    publishKeyEvent(edge);
                }
            } catch (RuntimeException samplerFailure) {
                log.debug("Observation sampling failed (heartbeat continues): windowId={} message={}",
                        windowId, samplerFailure.getMessage());
            }
        }
        List<ObservationKeyEvent> eventsForThisRequest;
        synchronized (pendingKeyEvents) {
            List<ObservationKeyEvent> pending = new ArrayList<>(pendingKeyEvents.values());
            eventsForThisRequest = pending.size() <= ObservationProtocolValidator.MAX_EVENTS_PER_REQUEST
                    ? List.copyOf(pending)
                    : List.copyOf(pending.subList(0, ObservationProtocolValidator.MAX_EVENTS_PER_REQUEST));
        }
        String currentIntentId = pathingFactsForThisRequest.isEmpty()
                ? null : pathingFactsForThisRequest.get(0).intentId();
        ObservationPreparedFrameDemand demand = preparedFrameDemand;
        if (preparedFrameCapture != null && demand != null
                && demand.expiresAtMs() >= System.currentTimeMillis()) {
            ObservationPreparedFrame retained = retainedPreparedFrame;
            if (retained == null
                    || !Objects.equals(retained.demandId(), demand.demandId())
                    || retained.generation() != demand.generation()) {
                try {
                    retained = preparedFrameCapture.capture(demand);
                    retainedPreparedFrame = retained;
                } catch (RuntimeException captureFailure) {
                    log.warn("Prepared-frame capture failed; demand retained: windowId={} demandId={} message={}",
                            windowId, demand.demandId(), captureFailure.getMessage());
                }
            }
            if (retained != null) {
                preparedFramesForThisRequest = List.of(retained);
            }
        }
        ObservationRequest request = new ObservationRequest(
                ObservationProtocolValidator.CONTRACT_VERSION,
                tenantId,
                deviceId,
                windowId,
                hwnd,
                taskCode,
                taskRunId,
                requestObserverSeq,
                System.currentTimeMillis(),
                interestRevision,
                currentIntentId,
                null,
                null,
                RUNNER_SOURCE,
                null,
                pathingFactsForThisRequest,
                factsForThisRequest,
                eventsForThisRequest,
                roisForThisRequest,
                dialogInterestsForThisRequest,
                preparedDialogsForThisRequest,
                terminalFramesForThisRequest,
                preparedFramesForThisRequest);
        for (ObservationRoi roi : roisForThisRequest) {
            if ("xiuluo-dialog".equals(roi.roiKey())) {
                log.info("Observation dialog ROI sending: windowId={} taskRunId={} observerSeq={} capturedAtMs={} "
                                + "roi=({},{} {}x{}) pngBytes={} sha256={}",
                        windowId, taskRunId, request.observerSeq(), request.capturedAtMs(),
                        roi.left(), roi.top(), roi.width(), roi.height(), roi.pngBytes().length,
                        sha256Hex(roi.pngBytes()));
            }
        }
        try {
            ObservationResponse response = client.send(request);
            synchronized (pendingKeyEvents) {
                for (String acknowledged : response.acknowledgedEventIds()) {
                    pendingKeyEvents.remove(acknowledged);
                }
            }
            interestRevision = response.interestRevision();
            interests = response.interests();
            ObservationPreparedFrameDemand nextDemand = response.preparedFrameDemands().isEmpty()
                    ? null : response.preparedFrameDemands().getFirst();
            preparedFrameDemand = nextDemand;
            if (nextDemand == null
                    || retainedPreparedFrame != null
                    && (!Objects.equals(retainedPreparedFrame.demandId(), nextDemand.demandId())
                    || retainedPreparedFrame.generation() != nextDemand.generation())) {
                retainedPreparedFrame = null;
            }
            if (nextDemand != null) {
                wakeForLocalStateChange();
            }
            if (sampler != null) {
                sampler.acknowledgeTerminalFrames(terminalFramesForThisRequest.stream()
                        .map(ObservationTerminalFrame::frameId)
                        .toList());
                sampler.acceptAnalysisResults(response.analysisResults());
                sampler.acknowledgeDeliveredPathingFacts(pathingFactsForThisRequest);
            }
            successfulSendCount++;
            long recoveredFailures = consecutiveTransportFailures;
            consecutiveTransportFailures = 0L;
            if (recoveredFailures > 0L) {
                log.info("Observation transport recovered: windowId={} taskRunId={} observerSeq={} interestRevision={} interests={} previousConsecutiveFailures={}",
                        windowId, taskRunId, request.observerSeq(), interestRevision, interests.size(),
                        recoveredFailures);
            }
        } catch (ObservationTransportException transportFailure) {
            // Transport failure is never a business fact: keep every unacknowledged key event and the bounded
            // latest state, and simply try again on the next cycle.
            lastTransportFailureAtMs = System.currentTimeMillis();
            long failures = ++consecutiveTransportFailures;
            if (failures == 1L || failures % 12L == 0L) {
                log.warn("Observation send failed (retained state, will retry): windowId={} taskRunId={} observerSeq={} interestRevision={} interests={} rois={} consecutiveFailures={} kind={} message={}",
                        windowId, taskRunId, request.observerSeq(), interestRevision, interests.size(),
                        roisForThisRequest.size(), failures, transportFailure.kind(), transportFailure.getMessage());
            }
            /*
             * A semantic 400 is not a transient transport hiccup: identical content can never
             * succeed on retry. When the rejected batch carried a retained pathing fact, suppress
             * it so one poisoned fact cannot dead-lock the whole observation plane (coordinates,
             * combat facts and key events all travel in the same batch).
             */
            if (sampler != null
                    && transportFailure.kind() == ObservationTransportException.Kind.HTTP_STATUS
                    && transportFailure.getMessage() != null
                    && transportFailure.getMessage().contains("INVALID_OBSERVATION_REQUEST")
                    && !pathingFactsForThisRequest.isEmpty()) {
                sampler.suppressRejectedPathingFact(transportFailure.getMessage());
            }
        }
    }

    private static String sha256Hex(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 is unavailable", impossible);
        }
    }

    private void pace() {
        long periodMs = currentPeriodMs();
        synchronized (pacer) {
            if (stopRequested.get()) {
                return;
            }
            if (wakePending) {
                wakePending = false;
                return;
            }
            try {
                pacer.wait(periodMs);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
            }
            wakePending = false;
        }
    }

    long currentPeriodMs() {
        if (sampler != null && sampler.hasActiveWubeiEnterBattleInterest()) {
            return WindowObservationSampler.WUBEI_PREPARE_PERIOD_MS;
        }
        List<ObservationInterest> currentInterests = interests;
        long localPeriodMs = sampler != null && sampler.hasActivePathingIntent()
                ? WindowObservationSampler.LOCAL_PATHING_SAMPLE_PERIOD_MS
                : Long.MAX_VALUE;
        if (currentInterests.isEmpty()) {
            return Math.min(parkedHeartbeatPeriodMs, localPeriodMs);
        }
        long fastest = localPeriodMs;
        for (ObservationInterest interest : currentInterests) {
            fastest = Math.min(fastest, interest.samplePeriodMs());
        }
        return Math.max(MIN_SAMPLE_PERIOD_MS, fastest);
    }

    private static String requireIdentity(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must be nonblank");
        }
        return value;
    }
}
