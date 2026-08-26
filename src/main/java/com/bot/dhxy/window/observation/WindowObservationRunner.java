package com.bot.dhxy.window.observation;

import com.bot.dhxy.cloud.turn.protocol.observation.ObservationFact;
import com.bot.dhxy.cloud.turn.protocol.observation.ObservationFactType;
import com.bot.dhxy.cloud.turn.protocol.observation.ObservationInterest;
import com.bot.dhxy.cloud.turn.protocol.observation.ObservationKeyEvent;
import com.bot.dhxy.cloud.turn.protocol.observation.ObservationPathingFact;
import com.bot.dhxy.cloud.turn.protocol.observation.ObservationPathingState;
import com.bot.dhxy.cloud.turn.protocol.observation.ObservationPathingTransition;
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
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * TURN-40G: one resident observation runner per Cloud-acknowledged window. The runner communicates exclusively over
 * the independent observation plane — it never touches the turn command slot and never focuses a window. Its sampler
 * performs exact-HWND capture and may enqueue fenced input intents, but the global input worker alone executes them.
 * It maintains a
 * monotonic {@code observerSeq}, keeps at most one request in flight, and retains key events until the Cloud
 * acknowledges them. Physical observation and HTTPS transport are separate lanes: a slow Cloud response can
 * delay the next upload but can never stop local combat/pathing sampling. Transport failure remains transport
 * failure only — never a business fact.
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
    /**
     * Linearization boundary for child binding and installation of an exact cancellable HTTP call.
     * Network I/O stays outside the monitor; rebind cancels the installed old-child call and the
     * response commit applies the same revision fence after transport returns.
     */
    private final Object taskBindingMonitor = new Object();
    /** Serializes sampler collect/ack/reset; lock order is samplerMonitor then taskBindingMonitor. */
    private final Object samplerMonitor = new Object();
    private final Object pacer = new Object();
    private final ObservationClient client;
    private final String tenantId;
    private final String deviceId;
    private final String windowId;
    private final String hwnd;
    /** Exact child task currently owning this queue-scoped observation run. */
    private volatile String taskCode;
    private final String taskRunId;
    /** Prevents an in-flight response for the previous queue child from committing into the new child. */
    private final AtomicLong taskBindingRevision = new AtomicLong();
    private final AtomicBoolean running = new AtomicBoolean(false);
    /** One transport/batch cycle at a time; the resident observation thread never waits for it. */
    private final AtomicBoolean transportCycleRunning = new AtomicBoolean(false);
    /** G103-CR3：transport 期间到达的状态变化需要在 transport 结束后补一次定点唤醒。 */
    private final AtomicBoolean followupAfterTransport = new AtomicBoolean(false);
    private final AtomicBoolean stopRequested = new AtomicBoolean(false);
    private final AtomicBoolean retainStateOnStop = new AtomicBoolean(false);
    private final AtomicLong observerSeq = new AtomicLong();

    private volatile Thread workerThread;
    private volatile Thread transportThread;
    private volatile long interestRevision;
    private volatile List<ObservationInterest> interests = List.of();
    private volatile long lastTransportFailureAtMs;
    private volatile long successfulSendCount;
    private volatile long consecutiveTransportFailures;
    private volatile long lastSuccessfulSendAtMs;
    private volatile ObservationPathingFact lastDeliveredPathingFact;
    /** Exact currently installed network call; guarded by taskBindingMonitor. */
    private ObservationSendCancellation inFlightSendCancellation;

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
    /** Exact demand generation whose immutable payload already reached Cloud through a successful HTTP exchange. */
    private volatile PreparedFrameIdentity deliveredPreparedFrameIdentity;
    /** True until Cloud has accepted this new run's first observation-plane request. */
    private volatile boolean startupScreenObservationPending = true;
    /**
     * Rearmed on every runner start, including pause resume. It sends the first definitive local combat fact and,
     * when that fact is VISIBLE, remains armed until Cloud has accepted the Runner's first ABSENT frame.
     */
    private volatile boolean startupCombatObservationPending = true;

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
            if (transportCycleRunning.get()) {
                running.set(false);
                throw new IllegalStateException("previous observation transport is still running for windowId="
                        + windowId);
            }
            synchronized (taskBindingMonitor) {
                retainStateOnStop.set(false);
                stopRequested.set(false);
                startupCombatObservationPending = true;
            }
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
        requestThreadStop();
    }

    private void requestThreadStop() {
        // Set the flag and interrupt first, then cancel the dedicated transport without waiting for body I/O.
        stopRequested.set(true);
        synchronized (lifecycleMonitor) {
            Thread thread = workerThread;
            if (thread != null) {
                thread.interrupt();
            }
            Thread transport = transportThread;
            if (transport != null) {
                transport.interrupt();
            }
        }
        synchronized (taskBindingMonitor) {
            if (inFlightSendCancellation != null) {
                inFlightSendCancellation.cancel();
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
            while (running.get() || transportCycleRunning.get()) {
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

    public String taskCode() {
        return taskCode;
    }

    /**
     * Rebinds this queue-scoped runner to the exact child task announced by Cloud's {@code TASK_STARTED} event.
     * The observation run id and monotonically increasing observer sequence stay unchanged, while old child
     * interests/events are fenced and the new child receives its own startup-screen boundary.
     *
     * @param exactTaskCode canonical child task code; must be nonblank
     */
    public void rebindTaskCode(String exactTaskCode) {
        String normalized = requireIdentity(exactTaskCode, "exactTaskCode").toLowerCase(Locale.ROOT);
        String previous;
        long revision;
        synchronized (samplerMonitor) {
            synchronized (taskBindingMonitor) {
                previous = taskCode;
                if (previous.equals(normalized)) {
                    return;
                }
                if (inFlightSendCancellation != null) {
                    // Dedicated cancellation does not interrupt the resident runner thread.
                    inFlightSendCancellation.cancel();
                    inFlightSendCancellation = null;
                }
                revision = taskBindingRevision.incrementAndGet();
                taskCode = normalized;
                interestRevision = 0L;
                interests = List.of();
                preparedFrameDemand = null;
                retainedPreparedFrame = null;
                deliveredPreparedFrameIdentity = null;
                lastDeliveredPathingFact = null;
                startupScreenObservationPending = true;
                startupCombatObservationPending = true;
                lastSuccessfulSendAtMs = 0L;
                lastTransportFailureAtMs = 0L;
                if (sampler != null) {
                    sampler.reset();
                }
                synchronized (pendingKeyEvents) {
                    pendingKeyEvents.clear();
                }
            }
        }
        log.info("Observation child task rebound: windowId={} taskRunId={} previousTaskCode={} "
                        + "taskCode={} bindingRevision={}",
                windowId, taskRunId, previous, normalized, revision);
        wakeForLocalStateChange();
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
        /*
         * G103-CR3/CR5 P1（wake 竞态）：状态变化发生在 transport 仍 running 时（如 sendOnce 在
         * transport 线程上处理回包、发现新 demand 并调用本方法），runLoop 被吵醒后只能做
         * physical-only 一拍又睡回去；transport 随后结束但（按 G103 纪律）不再无条件唤醒——
         * parked 态下新 demand 最坏拖满 5s。置位 followupAfterTransport，由 transport 的
         * finally 在清掉 running 之后【有条件】补一次定点唤醒。
         *
         * CR5 原子化：running 检查+置标志与 finally 的清 running+消费标志共用 pacer 同一
         * 同步边界——不存在"读到 running=true 但旧 transport 已清并消费过标志"的交错，
         * 不会把 stale 标志遗留给下一次 transport。
         */
        synchronized (pacer) {
            if (transportCycleRunning.get()) {
                followupAfterTransport.set(true);
            }
            wakePending = true;
            pacer.notifyAll();
        }
    }

    private void runLoop() {
        log.info("Observation runner started: deviceId={} windowId={} taskRunId={}", deviceId, windowId, taskRunId);
        try {
            while (!stopRequested.get() && !Thread.currentThread().isInterrupted()) {
                if (sampler != null) {
                    // G103：把本拍的有效采样周期喂给共享帧限频门（战斗 1s / 寻路 300ms / 五倍 100ms）。
                    sampler.setEffectiveSharedFramePeriodMs(currentPeriodMs());
                }
                if (transportCycleRunning.get()) {
                    samplePhysicalStateWhileTransportRuns();
                } else {
                    startTransportCycle();
                }
                pace();
            }
        } catch (RuntimeException unexpected) {
            if (!stopRequested.get()) {
                log.error("Observation runner stopped after unexpected failure: windowId={} type={} message={}",
                        windowId, unexpected.getClass().getSimpleName(), unexpected.getMessage());
            }
        } finally {
            if (retainStateOnStop.get()) {
                if (sampler != null) {
                    synchronized (samplerMonitor) {
                        sampler.invalidateTerminalFrameForSuspend();
                    }
                }
            } else {
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
        synchronized (samplerMonitor) {
            synchronized (taskBindingMonitor) {
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
                deliveredPreparedFrameIdentity = null;
                lastSuccessfulSendAtMs = 0L;
                lastTransportFailureAtMs = 0L;
                lastDeliveredPathingFact = null;
            }
        }
    }

    private void sendOnce() {
        if (stopRequested.get()) {
            return;
        }
        long requestTaskBindingRevision;
        String requestTaskCode;
        boolean requestStartupScreenObservation;
        boolean requestStartupCombatObservation;
        long requestInterestRevision;
        List<ObservationInterest> requestInterests;
        ObservationPreparedFrameDemand demand;
        PreparedFrameIdentity deliveredIdentity;
        ObservationPreparedFrame retainedFrame;
        synchronized (taskBindingMonitor) {
            requestTaskBindingRevision = taskBindingRevision.get();
            requestTaskCode = taskCode;
            requestStartupScreenObservation = startupScreenObservationPending;
            requestStartupCombatObservation = startupCombatObservationPending;
            requestInterestRevision = interestRevision;
            requestInterests = interests;
            demand = preparedFrameDemand;
            deliveredIdentity = deliveredPreparedFrameIdentity;
            retainedFrame = retainedPreparedFrame;
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
        long requestObserverSeq = observerSeq.get() + 1L;
        // A Cloud-requested exact frame is the active protocol step. Capture it before ordinary sampling, retain it
        // across transport failure, then stop attaching the immutable bytes after one successful HTTP exchange.
        // Cloud may keep the business demand active while its prepared-action slot is occupied; that is not a request
        // to upload the same payload again.
        boolean demandAlreadyDelivered = deliveredIdentity != null && deliveredIdentity.matches(demand);
        if (preparedFrameCapture != null && demand != null && !demandAlreadyDelivered) {
            ObservationPreparedFrame retained = retainedFrame;
            if (retained == null
                    || !Objects.equals(retained.demandId(), demand.demandId())
                    || retained.generation() != demand.generation()) {
                try {
                    if (sampler != null && demand.purpose() != null
                            && demand.purpose().contains("dialog")) {
                        // disabled 2026-08-17 pending no-focus rework
                    }
                    retained = preparedFrameCapture.capture(demand);
                    synchronized (taskBindingMonitor) {
                        if (stopRequested.get()
                                || taskBindingRevision.get() != requestTaskBindingRevision
                                || !PreparedFrameIdentity.from(retained).matches(preparedFrameDemand)
                                || deliveredPreparedFrameIdentity != null
                                && deliveredPreparedFrameIdentity.matches(preparedFrameDemand)) {
                            wakeForLocalStateChange();
                            return;
                        }
                        retainedPreparedFrame = retained;
                    }
                    log.info("Prepared-frame captured for upload: windowId={} taskRunId={} demandId={} purpose={} "
                                    + "generation={} capturedAtMs={} size={}x{} pngBytes={}",
                            windowId, taskRunId, demand.demandId(), demand.purpose(), demand.generation(),
                            retained.capturedAtMs(), retained.width(), retained.height(), retained.pngBytes().length);
                } catch (RuntimeException captureFailure) {
                    log.warn("Prepared-frame capture failed; demand retained: windowId={} demandId={} message={}",
                            windowId, demand.demandId(), captureFailure.getMessage());
                }
            }
            if (retained != null) {
                preparedFramesForThisRequest = List.of(retained);
            }
        }
        if (sampler != null) {
            try {
                WindowObservationSampler.SampleBatch batch;
                synchronized (samplerMonitor) {
                    synchronized (taskBindingMonitor) {
                        if (stopRequested.get()
                                || taskBindingRevision.get() != requestTaskBindingRevision) {
                            wakeForLocalStateChange();
                            return;
                        }
                    }
                    batch = sampler.collect(
                            requestInterests,
                            requestObserverSeq,
                            requestStartupCombatObservation,
                            requestStartupScreenObservation && observesWuhuan(requestTaskCode));
                }
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
        long nowMs = System.currentTimeMillis();
        /*
         * G103-CR6 P1：发送门本身锚 max(成功, 失败/拒收)。只靠 pace 的完成锚不够——runLoop 先
         * startTransportCycle 再 pace，慢失败在 pace 计算之后才完成时，失败锚来不及约束下一拍，
         * 下一次 HTTP 已经发出（实测 parked 800ms 档失败→下一请求仅隔 15-17ms）。把 backoff 门
         * 放在真正调用 client.send 之前：无论 tick 以什么时序到达，无载荷发送距上一次完成
         * （成功或失败）至少一个 parked 周期；有载荷发送仍按采样节奏走（capture 门已限频）。
         */
        long sendCompletionAnchorMs = Math.max(lastSuccessfulSendAtMs, lastTransportFailureAtMs);
        boolean heartbeatDue = sendCompletionAnchorMs <= 0L
                || nowMs - sendCompletionAnchorMs >= parkedHeartbeatPeriodMs;
        boolean immediatePathingSend = pathingFactsForThisRequest.stream()
                .anyMatch(fact -> requiresImmediatePathingSend(fact, lastDeliveredPathingFact));
        boolean hasImmediatePayload = !factsForThisRequest.isEmpty()
                || !eventsForThisRequest.isEmpty()
                || !roisForThisRequest.isEmpty()
                || !dialogInterestsForThisRequest.isEmpty()
                || !preparedDialogsForThisRequest.isEmpty()
                || !terminalFramesForThisRequest.isEmpty()
                || !preparedFramesForThisRequest.isEmpty()
                || immediatePathingSend;
        if (!hasImmediatePayload && !heartbeatDue) {
            return;
        }
        if (!hasImmediatePayload) {
            // The slow connection/interest heartbeat is transport-only. Do not turn it back into a repeated
            // ACTIVE pathing update while the local movement ROI is still changing.
            pathingFactsForThisRequest = List.of();
            currentIntentId = null;
        }
        ObservationRequest request;
        synchronized (taskBindingMonitor) {
            if (stopRequested.get()
                    || Thread.currentThread().isInterrupted()
                    || taskBindingRevision.get() != requestTaskBindingRevision) {
                wakeForLocalStateChange();
                return;
            }
            long assignedObserverSeq = observerSeq.incrementAndGet();
            if (assignedObserverSeq != requestObserverSeq) {
                throw new IllegalStateException("Observation sequence changed outside the runner thread");
            }
            request = new ObservationRequest(
                    ObservationProtocolValidator.CONTRACT_VERSION,
                    tenantId,
                    deviceId,
                    windowId,
                    hwnd,
                    requestTaskCode,
                    taskRunId,
                    requestObserverSeq,
                    System.currentTimeMillis(),
                    requestInterestRevision,
                    currentIntentId,
                    null,
                    null,
                    requestStartupScreenObservation ? "startup-screen-observation" : RUNNER_SOURCE,
                    null,
                    pathingFactsForThisRequest,
                    factsForThisRequest,
                    eventsForThisRequest,
                    roisForThisRequest,
                    dialogInterestsForThisRequest,
                    preparedDialogsForThisRequest,
                    terminalFramesForThisRequest,
                    preparedFramesForThisRequest);
        }
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
            ObservationResponse response;
            ObservationSendCancellation sendCancellation = new ObservationSendCancellation();
            synchronized (taskBindingMonitor) {
                if (stopRequested.get()
                        || Thread.currentThread().isInterrupted()
                        || taskBindingRevision.get() != requestTaskBindingRevision) {
                    wakeForLocalStateChange();
                    return;
                }
                /*
                 * Installing this exact call is the send-start linearization point. Rebind/stop that
                 * wins first prevents installation; one that wins later cancels this token. The
                 * transport checks cancellation both before and immediately after creating its future.
                 */
                inFlightSendCancellation = sendCancellation;
            }
            try {
                response = client.send(request, sendCancellation);
            } finally {
                synchronized (taskBindingMonitor) {
                    if (inFlightSendCancellation == sendCancellation) {
                        inFlightSendCancellation = null;
                    }
                }
            }
            synchronized (samplerMonitor) {
                synchronized (taskBindingMonitor) {
                    if (stopRequested.get()
                            || taskBindingRevision.get() != requestTaskBindingRevision) {
                        log.info("Observation response ignored after child task rebind: windowId={} taskRunId={} "
                                        + "requestTaskCode={} currentTaskCode={} observerSeq={} "
                                        + "requestBindingRevision={} currentBindingRevision={}",
                                windowId, taskRunId, requestTaskCode, taskCode, requestObserverSeq,
                                requestTaskBindingRevision, taskBindingRevision.get());
                        wakeForLocalStateChange();
                        return;
                    }
                    boolean requestAccepted = response.acceptedObserverSeq() >= requestObserverSeq;
                    if (!requestAccepted) {
                        // Sequence coverage is the acceptance boundary for every payload type. A 2xx response
                        // below this request's sequence commits no ACK, interest, demand or sampler state.
                        lastTransportFailureAtMs = System.currentTimeMillis();
                        long failures = ++consecutiveTransportFailures;
                        if (failures == 1L || failures % 12L == 0L) {
                            log.warn("Observation response did not cover request; retained for retry: windowId={} "
                                            + "taskRunId={} observerSeq={} acceptedObserverSeq={} "
                                            + "preparedFrame={} consecutiveFailures={}",
                                    windowId, taskRunId, requestObserverSeq, response.acceptedObserverSeq(),
                                    !preparedFramesForThisRequest.isEmpty(), failures);
                        }
                        /*
                         * G103-CR5 P1：这里原来 wakeForLocalStateChange() 立即重试——云端连续低
                         * acceptedObserverSeq 时每个回包都唤醒，绕过全部 deadline 形成 HTTP/collect
                         * 唤醒热循环。重试改交给 pace 的节奏/失败锚 deadline（失败已记
                         * lastTransportFailureAtMs），下一次尝试按 cadence/backoff 到点自然发生；
                         * 保留的 payload/事件在那一拍原样重传。
                         */
                        return;
                    }
                    boolean carriedPreparedFrame = !preparedFramesForThisRequest.isEmpty();
                    if (carriedPreparedFrame) {
                        ObservationPreparedFrame deliveredFrame = preparedFramesForThisRequest.getFirst();
                        deliveredPreparedFrameIdentity = PreparedFrameIdentity.from(deliveredFrame);
                        retainedPreparedFrame = null;
                        log.info("Prepared-frame upload delivered once: windowId={} taskRunId={} observerSeq={} "
                                        + "demandId={} purpose={} generation={} capturedAtMs={} pngBytes={}",
                                windowId, taskRunId, request.observerSeq(), deliveredFrame.demandId(),
                                deliveredFrame.purpose(), deliveredFrame.generation(), deliveredFrame.capturedAtMs(),
                                deliveredFrame.pngBytes().length);
                    }
                    // A failed send deliberately keeps the startup marker so the next request describes the same
                    // fresh screen boundary. Only a sequence-covered request consumes it.
                    startupScreenObservationPending = false;
                    if (hasConfirmedOutOfCombatFact(factsForThisRequest)) {
                        startupCombatObservationPending = false;
                    }
                    synchronized (pendingKeyEvents) {
                        for (String acknowledged : response.acknowledgedEventIds()) {
                            pendingKeyEvents.remove(acknowledged);
                        }
                    }
                    long previousInterestRevision = interestRevision;
                    List<ObservationInterest> previousInterests = interests;
                    interestRevision = response.interestRevision();
                    interests = response.interests();
                    if (previousInterestRevision != interestRevision || !previousInterests.equals(interests)) {
                        log.info("Observation interests applied: windowId={} taskRunId={} observerSeq={} "
                                        + "revision={} interests={}",
                                windowId, taskRunId, request.observerSeq(), interestRevision,
                                interests.stream().map(ObservationInterest::interestKey).toList());
                    }
                    /*
                     * G103-CR2/CR3 P2：只有兴趣集把【真实有效节奏】变快才定点唤醒（如 parked 5s
                     * 心跳切战斗 1s）。有效节奏按 currentPeriodMs 同规则计算（parked/寻路车道/MIN
                     * 钳制）；变慢/等速/仅修订号变化不唤醒——多余唤醒只会喂 transport/collect 热循环。
                     */
                    if (shouldWakeForAppliedInterests(previousInterests, interests,
                            parkedHeartbeatPeriodMs, localLanePeriodMs())) {
                        wakeForLocalStateChange();
                    }
                    ObservationPreparedFrameDemand previousDemand = preparedFrameDemand;
                    ObservationPreparedFrameDemand nextDemand = response.preparedFrameDemands().isEmpty()
                            ? null : response.preparedFrameDemands().getFirst();
                    preparedFrameDemand = nextDemand;
                    PreparedFrameIdentity currentDeliveredIdentity = deliveredPreparedFrameIdentity;
                    if (currentDeliveredIdentity == null || !currentDeliveredIdentity.matches(nextDemand)) {
                        deliveredPreparedFrameIdentity = null;
                    }
                    if (nextDemand == null || retainedPreparedFrame != null
                            && (!Objects.equals(retainedPreparedFrame.demandId(), nextDemand.demandId())
                            || retainedPreparedFrame.generation() != nextDemand.generation())) {
                        retainedPreparedFrame = null;
                    }
                    /*
                     * G103-CR2 P2：只有【新到/换代】的 demand 才定点唤醒。同一 demand 在后续回包里
                     * 反复出现（客户端尚未交付前云端每次都会重发）不再唤醒——重试按正常采样节奏走，
                     * 否则每次回包都 wake，形成回包驱动的 transport/collect 热循环。
                     */
                    if (isNewPreparedFrameDemand(previousDemand, nextDemand)) {
                        log.info("Prepared-frame demand received: windowId={} taskRunId={} observerSeq={} "
                                        + "demandId={} purpose={} generation={} issuedAtMs={}",
                                windowId, taskRunId, request.observerSeq(), nextDemand.demandId(),
                                nextDemand.purpose(), nextDemand.generation(), nextDemand.issuedAtMs());
                        wakeForLocalStateChange();
                    }
                    if (sampler != null) {
                        sampler.acknowledgeTerminalFrames(terminalFramesForThisRequest.stream()
                                .map(ObservationTerminalFrame::frameId)
                                .toList());
                        sampler.acceptAnalysisResults(response.analysisResults());
                        sampler.acknowledgeDeliveredPathingFacts(pathingFactsForThisRequest);
                        sampler.acknowledgeDeliveredFacts(requestObserverSeq, factsForThisRequest);
                        if (response.interestRevision() == request.interestRevision()) {
                            sampler.acknowledgeDeliveredRois(requestObserverSeq, roisForThisRequest);
                        }
                    }
                    lastSuccessfulSendAtMs = System.currentTimeMillis();
                    if (!pathingFactsForThisRequest.isEmpty()) {
                        lastDeliveredPathingFact = pathingFactsForThisRequest.getFirst();
                    }
                    successfulSendCount++;
                    long recoveredFailures = consecutiveTransportFailures;
                    consecutiveTransportFailures = 0L;
                    if (recoveredFailures > 0L) {
                        log.info("Observation transport recovered: windowId={} taskRunId={} observerSeq={} "
                                        + "interestRevision={} interests={} previousConsecutiveFailures={}",
                                windowId, taskRunId, request.observerSeq(), interestRevision, interests.size(),
                                recoveredFailures);
                    }
                }
            }
        } catch (ObservationTransportException transportFailure) {
            // Transport failure is never a business fact: keep every unacknowledged key event and the bounded
            // latest state, and simply try again on the next cycle.
            synchronized (samplerMonitor) {
                synchronized (taskBindingMonitor) {
                    if (stopRequested.get()
                            || taskBindingRevision.get() != requestTaskBindingRevision) {
                        wakeForLocalStateChange();
                        return;
                    }
                    lastTransportFailureAtMs = System.currentTimeMillis();
                    long failures = ++consecutiveTransportFailures;
                    if (failures == 1L || failures % 12L == 0L) {
                        log.warn("Observation send failed (retained state, will retry): windowId={} taskRunId={} "
                                        + "observerSeq={} interestRevision={} interests={} rois={} "
                                        + "consecutiveFailures={} kind={} message={}",
                                windowId, taskRunId, request.observerSeq(), interestRevision, interests.size(),
                                roisForThisRequest.size(), failures, transportFailure.kind(),
                                transportFailure.getMessage());
                        // 2026-08-23 21:19 事故取证：400 semantic 拒收时必须能看到被拒批里
                        // pathing 事实的完整形态，否则只有云端一句校验文案，无法定位写家。
                        if (transportFailure.kind() == ObservationTransportException.Kind.HTTP_STATUS
                                && transportFailure.getMessage() != null
                                && transportFailure.getMessage().contains("INVALID_OBSERVATION_REQUEST")
                                && !pathingFactsForThisRequest.isEmpty()) {
                            ObservationPathingFact rejected = pathingFactsForThisRequest.getFirst();
                            log.warn("[observation] rejected batch pathing fact: windowId={} intentId={} state={} "
                                            + "transition={} type={} current={}({}, {}) target={}({}, {}) "
                                            + "frameId={} frameGen={} startedAt={} updatedAt={} locChangedAt={} "
                                            + "terminalFrames={}",
                                    windowId, rejected.intentId(), rejected.state(), rejected.transition(),
                                    rejected.pathingType(), rejected.currentMapName(), rejected.currentX(),
                                    rejected.currentY(), rejected.targetMapName(), rejected.targetX(),
                                    rejected.targetY(), rejected.terminalFrameId(),
                                    rejected.terminalFrameGeneration(), rejected.pathingStartedAtMs(),
                                    rejected.pathingUpdatedAtMs(), rejected.locationChangedAtMs(),
                                    terminalFramesForThisRequest.size());
                        }
                    }
                    /*
                     * A semantic 400 is not a transient transport hiccup: identical content can never
                     * succeed on retry. Suppression is serialized with collect/reset and belongs only
                     * to the still-current child revision.
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
        }
    }

    private void startTransportCycle() {
        if (!transportCycleRunning.compareAndSet(false, true)) {
            return;
        }
        Thread thread = new Thread(() -> {
            try {
                sendOnce();
            } catch (RuntimeException unexpected) {
                if (!stopRequested.get()) {
                    log.error("Observation transport cycle failed unexpectedly: windowId={} type={} message={}",
                            windowId, unexpected.getClass().getSimpleName(), unexpected.getMessage(), unexpected);
                }
            } finally {
                transportThread = null;
                /*
                 * G103（2026-08-25 用户确认）：这里原来无条件 wakeForLocalStateChange()——每次
                 * HTTPS 回包一到就跳过 pace() 直接开下一轮，采样节奏被回包速度接管（实测队长窗
                 * 5.7 次整窗 PrintWindow/秒）。回包完成本身不是状态变化，无条件唤醒已删。
                 *
                 * G103-CR3/CR5 P1：唯一例外是【transport 期间】到达过定点唤醒（followupAfterTransport
                 * 置位）——那次唤醒只换来一拍 physical-only，携带新 demand/新状态的下一次 transport
                 * 还没被安排。清 running 与消费标志在 pacer 同一同步边界内完成（与
                 * wakeForLocalStateChange 的检查+置位原子互斥），补一次唤醒且仅此一次；
                 * 无状态变化的回包依然零唤醒。
                 */
                synchronized (pacer) {
                    transportCycleRunning.set(false);
                    if (followupAfterTransport.compareAndSet(true, false)) {
                        wakePending = true;
                        pacer.notifyAll();
                    }
                }
                synchronized (lifecycleMonitor) {
                    lifecycleMonitor.notifyAll();
                }
            }
        }, "dhxy-observe-transport-" + windowId);
        thread.setDaemon(true);
        transportThread = thread;
        try {
            thread.start();
        } catch (RuntimeException | Error startFailure) {
            transportThread = null;
            transportCycleRunning.set(false);
            throw startFailure;
        }
    }

    private void samplePhysicalStateWhileTransportRuns() {
        if (sampler == null) {
            return;
        }
        try {
            synchronized (samplerMonitor) {
                sampler.collectPhysicalStateOnly(observerSeq.get());
            }
        } catch (RuntimeException sampleFailure) {
            log.debug("Physical observation continued with one failed tick while transport is in flight: "
                            + "windowId={} observerSeq={} message={}",
                    windowId, observerSeq.get(), sampleFailure.getMessage());
        }
    }

    /**
     * VISIBLE opens the common combat wait but deliberately keeps this startup duty armed; only an accepted
     * ABSENT frame proves the resumed task may leave the gate. UNAVAILABLE proves nothing.
     */
    private static boolean hasConfirmedOutOfCombatFact(List<ObservationFact> facts) {
        if (facts == null || facts.isEmpty()) {
            return false;
        }
        return facts.stream()
                .filter(fact -> fact != null && fact.factType() == ObservationFactType.COMBAT_SIGNAL)
                .map(ObservationFact::value)
                .filter(Objects::nonNull)
                .anyMatch(value -> value.startsWith("ABSENT:"));
    }

    private boolean observesWuhuan(String exactTaskCode) {
        return "WUHUAN_V3".equalsIgnoreCase(exactTaskCode);
    }

    /**
     * Returns whether a locally sampled pathing fact carries a semantic edge that Cloud must see immediately.
     * Timestamp-only movement updates remain local; Runner still samples them, but the transport stays quiet until
     * an identity, terminal, blocking, or coordinate-verdict payload needs delivery.
     */
    static boolean requiresImmediatePathingSend(ObservationPathingFact current,
                                                ObservationPathingFact lastDelivered) {
        if (current == null) {
            return false;
        }
        if (lastDelivered == null
                || current.transition() != ObservationPathingTransition.CURRENT
                || current.state() != ObservationPathingState.ACTIVE
                || lastDelivered.transition() != ObservationPathingTransition.CURRENT
                || lastDelivered.state() != ObservationPathingState.ACTIVE) {
            return true;
        }
        return !Objects.equals(current.taskRunId(), lastDelivered.taskRunId())
                || !Objects.equals(current.windowId(), lastDelivered.windowId())
                || !Objects.equals(current.hwnd(), lastDelivered.hwnd())
                || !Objects.equals(current.intentId(), lastDelivered.intentId())
                || !Objects.equals(current.source(), lastDelivered.source())
                || !Objects.equals(current.targetMapName(), lastDelivered.targetMapName())
                || !Objects.equals(current.targetX(), lastDelivered.targetX())
                || !Objects.equals(current.targetY(), lastDelivered.targetY())
                || current.tolerance() != lastDelivered.tolerance()
                || current.pathingType() != lastDelivered.pathingType()
                || current.dialogBlocking() != lastDelivered.dialogBlocking()
                || !Objects.equals(current.dialogBlockingReason(), lastDelivered.dialogBlockingReason());
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
        /*
         * G103-CR P1：绝对采样 deadline。等待时长不是固定一个周期，而是"距上次共享帧捕获满一个
         * 周期还剩多久"——周期末尾被定点 wake 提前吵醒过的拍，下一次等待自动缩短到剩余量，
         * 捕获间隔上界就是 periodMs 本身（旧行为最坏 ~1.9×周期）。无帧（清屏/捕获失败/静默成员）
         * 时回退整周期，避免失败自旋。
         */
        long waitMs = periodMs;
        if (sampler != null) {
            // 单调时钟（nanoTime 派生），与 sampler 记录捕获时刻同源；NTP/墙钟跳变免疫。
            waitMs = paceWaitMs(periodMs, sampler.sharedCycleFrameCapturedAtMs(),
                    WindowObservationSampler.monotonicMillis());
        }
        /*
         * G103-CR3/CR5：双锚 deadline。捕获 deadline 锚在捕获时刻，而 sendOnce 的心跳判据锚在
         * "上次成功发送"（晚一个处理间隙）——只按捕获锚起搏时，每个 parked tick 都恰好早到
         * 几十毫秒被 skip-send 守卫拦下，心跳被系统性拖向 2×parked。取两个 deadline 的较早者；
         * 心跳唤起的 tick 里共享帧门会复用现帧，不产生额外 PrintWindow。
         *
         * CR5 P1（失败热循环）：①transport in-flight 时心跳 deadline 完全退出 pace——反正不能
         * 再发，只按物理采样 deadline 起搏，不许 physical-only 自旋；②完成后锚取
         * max(上次成功, 上次失败/拒收)——心跳逾期期间一次失败后，下一次尝试至少再等一个
         * parked 周期（失败即 backoff），过期成功锚不再直接放行立即重试。失败记录本身已被
         * taskBindingRevision 守卫（rebind 后的旧回包在记录前早退），reset 链清零两锚。
         */
        if (!transportCycleRunning.get()) {
            long completionAnchorMs = Math.max(lastSuccessfulSendAtMs, lastTransportFailureAtMs);
            waitMs = Math.min(waitMs,
                    heartbeatWaitMs(parkedHeartbeatPeriodMs, completionAnchorMs, System.currentTimeMillis()));
        }
        synchronized (pacer) {
            if (stopRequested.get()) {
                return;
            }
            if (wakePending) {
                wakePending = false;
                return;
            }
            if (waitMs <= 0L) {
                // 捕获 deadline 已到期：立即进入下一拍（下一拍必然重拍共享帧，capturedAt 前移）。
                return;
            }
            try {
                pacer.wait(waitMs);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
            }
            wakePending = false;
        }
    }

    /**
     * G103-CR 合同点：本拍应当等待的毫秒数。{@code capturedAtMs<=0}（无共享帧）等满整周期；
     * 有帧时等到"捕获时刻+周期"的绝对 deadline，且永不超过一个整周期。到期返回 {@code <=0}。
     */
    static long paceWaitMs(long periodMs, long capturedAtMs, long nowMs) {
        if (capturedAtMs <= 0L) {
            return periodMs;
        }
        return Math.min(periodMs, capturedAtMs + periodMs - nowMs);
    }

    /**
     * G103-CR3 合同点：距 parked 心跳 deadline（上次成功发送+parked 周期，墙钟锚）的剩余毫秒，
     * 上界一个 parked 周期。从未发送过（{@code lastSuccessfulSendAtMs<=0}）返回整周期——
     * 首拍本来就会立即发送。
     */
    static long heartbeatWaitMs(long parkedHeartbeatPeriodMs, long lastSuccessfulSendAtMs, long nowWallMs) {
        if (lastSuccessfulSendAtMs <= 0L) {
            return parkedHeartbeatPeriodMs;
        }
        return Math.min(parkedHeartbeatPeriodMs,
                lastSuccessfulSendAtMs + parkedHeartbeatPeriodMs - nowWallMs);
    }

    /**
     * G103-CR2/CR3 合同点：仅当应用的兴趣集把【真实有效节奏】变快时才定点唤醒。有效节奏含
     * parked 心跳、寻路 300ms 车道与 MIN 钳制——例如活跃寻路（300ms）期间新到战斗兴趣（1s）
     * 不加速、不唤醒；变慢/等速/仅修订号变化不唤醒。
     */
    static boolean shouldWakeForAppliedInterests(List<ObservationInterest> previousInterests,
                                                 List<ObservationInterest> appliedInterests,
                                                 long parkedHeartbeatPeriodMs,
                                                 long localLanePeriodMs) {
        return effectiveCadenceMs(appliedInterests, parkedHeartbeatPeriodMs, localLanePeriodMs)
                < effectiveCadenceMs(previousInterests, parkedHeartbeatPeriodMs, localLanePeriodMs);
    }

    /** G103-CR2 合同点：prepared-frame demand 是否为新到/换代——只有它才值得定点唤醒。 */
    static boolean isNewPreparedFrameDemand(ObservationPreparedFrameDemand previousDemand,
                                            ObservationPreparedFrameDemand nextDemand) {
        return nextDemand != null
                && (previousDemand == null
                || !Objects.equals(previousDemand.demandId(), nextDemand.demandId())
                || previousDemand.generation() != nextDemand.generation());
    }

    long currentPeriodMs() {
        // Transport being in flight must not accelerate physical sampling: every local probe is
        // period-gated internally (>=1s), and each un-gated tick pays one whole-window PrintWindow
        // redraw per window.
        return effectiveCadenceMs(interests, parkedHeartbeatPeriodMs, localLanePeriodMs());
    }

    /**
     * G103-CR5 P2①：本地快车道统一入口——寻路 300ms 与五倍进战 100ms 都在这里，
     * current cadence 与 interest 加速判定共用同一 effectiveCadenceMs 计算，不再各走各的
     * early-return。无车道=Long.MAX_VALUE。
     */
    private long localLanePeriodMs() {
        long lane = Long.MAX_VALUE;
        if (sampler != null) {
            if (sampler.hasActiveWubeiEnterBattleInterest()) {
                lane = WindowObservationSampler.WUBEI_PREPARE_PERIOD_MS;
            }
            if (sampler.hasActivePathingIntent()) {
                lane = Math.min(lane, WindowObservationSampler.LOCAL_PATHING_SAMPLE_PERIOD_MS);
            }
        }
        return lane;
    }

    /**
     * G103-CR3 合同点：一组兴趣在给定 parked 心跳与本地车道（寻路 300ms/无=MAX）下的真实有效
     * 节奏——与 {@link #currentPeriodMs()} 同一套规则（空集=min(parked,本地)；非空=min(本地,最快兴趣)
     * 再按 {@link #MIN_SAMPLE_PERIOD_MS} 钳制）。加速唤醒判定必须用它，不得用原始兴趣周期。
     */
    static long effectiveCadenceMs(List<ObservationInterest> interests,
                                   long parkedHeartbeatPeriodMs,
                                   long localLanePeriodMs) {
        if (interests.isEmpty()) {
            return Math.min(parkedHeartbeatPeriodMs, localLanePeriodMs);
        }
        long fastest = localLanePeriodMs;
        for (ObservationInterest interest : interests) {
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

    private record PreparedFrameIdentity(String demandId, long generation) {

        private static PreparedFrameIdentity from(ObservationPreparedFrame frame) {
            return new PreparedFrameIdentity(frame.demandId(), frame.generation());
        }

        private boolean matches(ObservationPreparedFrameDemand demand) {
            return demand != null
                    && Objects.equals(demandId, demand.demandId())
                    && generation == demand.generation();
        }
    }
}
