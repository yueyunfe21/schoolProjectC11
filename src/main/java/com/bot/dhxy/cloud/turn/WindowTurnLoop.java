package com.bot.dhxy.cloud.turn;

import com.bot.dhxy.cloud.turn.protocol.TurnAction;
import com.bot.dhxy.cloud.turn.protocol.TurnLocalOperation;
import com.bot.dhxy.cloud.turn.protocol.TurnOutcome;
import com.bot.dhxy.cloud.turn.protocol.TurnProtocolValidator;
import com.bot.dhxy.cloud.turn.protocol.TurnMapSurveyCommand;
import com.bot.dhxy.cloud.turn.protocol.TurnMapSurveyResult;
import com.bot.dhxy.cloud.turn.protocol.TurnRequest;
import com.bot.dhxy.cloud.turn.protocol.TurnResponse;
import com.bot.dhxy.cloud.turn.protocol.TurnTaskCode;
import com.bot.dhxy.cloud.turn.protocol.TurnTaskStartAck;
import com.bot.dhxy.cloud.turn.protocol.TurnTaskStartRequest;
import com.bot.dhxy.cloud.turn.protocol.TurnTaskTerminalResult;
import com.bot.dhxy.cloud.turn.protocol.TurnWindowMetadata;
import com.bot.dhxy.window.observation.WindowObservationRunner;
import com.bot.dhxy.window.observation.WindowObservationRunnerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/** Explicit long-wait turn lifecycle for one immutable device/window identity. */
public final class WindowTurnLoop {

    private static final Logger log = LoggerFactory.getLogger(WindowTurnLoop.class);
    private static final int CONTRACT_VERSION = 1;

    private final Object lifecycleMonitor = new Object();
    private final Object pauseMonitor = new Object();
    private final String deviceId;
    private final String windowId;
    private final long waitTimeoutMs;
    private final Supplier<TurnWindowMetadata> windowMetadataSupplier;
    private final TurnClient turnClient;
    private final TurnActionRunner actionExecutor;
    private final AtomicBoolean stopRequested = new AtomicBoolean(false);
    private final AtomicBoolean running = new AtomicBoolean(false);

    private volatile Thread workerThread;
    private volatile Throwable lastFailure;
    private boolean retired;
    /** Guarded by {@link #lifecycleMonitor}; distinguishes an interruptible Cloud wait from local action work. */
    private boolean exchangeInFlight;

    // These values remain in memory across an explicitly restarted loop so uncertain transport never re-executes.
    private TurnOutcome previousOutcome;
    private byte[] previousPng;
    private String lastExecutedActionId;
    private ExecutedTurn lastExecutedTurn;

    // TURN-40D: the one immutable remote start request is carried on every turn until its matching TurnTaskStartAck
    // is accepted exactly once. Both survive an explicit restart so an accepted start is never re-minted or re-sent.
    private volatile TurnTaskStartRequest pendingStartRequest;
    private volatile boolean startAckAccepted;
    private volatile TurnTaskStartAck acceptedStartAck;
    private final CompletableFuture<TurnTaskTerminalResult> taskTerminalResult = new CompletableFuture<>();

    // One manual MapSurvey command may ride an otherwise task-free loop. The same immutable command survives
    // uncertain transport until Cloud returns a terminal result; that result is delivered once and acknowledged
    // on the next successful turn. This is transport state in the existing loop, not a second command store.
    private TurnMapSurveyCommand pendingMapSurveyCommand;
    private String pendingMapSurveyAckId;
    private CompletableFuture<TurnMapSurveyResult> pendingMapSurveyResult;

    // TURN-40D (R1): the live loop owns the Cloud checkpoint flags. Remote pause/resume flip pauseCheckpoint and the
    // loop projects both flags onto every turn's metadata. A checkpoint stop drives exactly one final
    // stopRequested=true turn before the loop exits (see runTurns), distinct from the hard stop() exit flag.
    private final AtomicBoolean pauseCheckpoint = new AtomicBoolean(false);
    private final AtomicBoolean resumeRequested = new AtomicBoolean(false);
    private final AtomicBoolean stopCheckpoint = new AtomicBoolean(false);
    private volatile boolean pausePublished;
    private volatile TurnWindowMetadata acknowledgedWindowMetadata;

    // TURN-40G: exactly one per-window observation runner, started only after the matching TurnTaskStartAck is
    // accepted (never before) and closed with a bounded join before this loop reports stopped. A null factory means
    // this process has no observation plane and the loop behaves exactly as before.
    private final WindowObservationRunnerFactory observationRunnerFactory;
    private volatile WindowObservationRunner observationRunner;

    WindowTurnLoop(String deviceId,
                   String windowId,
                   long waitTimeoutMs,
                   Supplier<TurnWindowMetadata> windowMetadataSupplier,
                   TurnClient turnClient,
                   LocalTurnActionExecutor actionExecutor) {
        this(deviceId, windowId, waitTimeoutMs, windowMetadataSupplier, turnClient,
                Objects.requireNonNull(actionExecutor, "actionExecutor")::execute, null);
    }

    WindowTurnLoop(String deviceId,
                   String windowId,
                   long waitTimeoutMs,
                   Supplier<TurnWindowMetadata> windowMetadataSupplier,
                   TurnClient turnClient,
                   TurnActionRunner actionExecutor) {
        this(deviceId, windowId, waitTimeoutMs, windowMetadataSupplier, turnClient, actionExecutor, null);
    }

    WindowTurnLoop(String deviceId,
                   String windowId,
                   long waitTimeoutMs,
                   Supplier<TurnWindowMetadata> windowMetadataSupplier,
                   TurnClient turnClient,
                   TurnActionRunner actionExecutor,
                   WindowObservationRunnerFactory observationRunnerFactory) {
        this.deviceId = requireIdentity(deviceId, "deviceId");
        this.windowId = requireIdentity(windowId, "windowId");
        if (waitTimeoutMs <= 0L) {
            throw new IllegalArgumentException("waitTimeoutMs must be positive for long-wait exchange");
        }
        this.waitTimeoutMs = waitTimeoutMs;
        this.windowMetadataSupplier = Objects.requireNonNull(windowMetadataSupplier, "windowMetadataSupplier");
        this.turnClient = Objects.requireNonNull(turnClient, "turnClient");
        this.actionExecutor = Objects.requireNonNull(actionExecutor, "actionExecutor");
        this.observationRunnerFactory = observationRunnerFactory;
    }

    /**
     * TURN-40D: attaches the one immutable remote {@link TurnTaskStartRequest} to this stopped loop before it is
     * started. The registry calls this exactly once between creation and start; the request is then carried on every
     * turn until a matching {@code TurnTaskStartAck} is accepted. Never called on a running loop or a second time.
     */
    void attachStartRequest(TurnTaskStartRequest startRequest) {
        Objects.requireNonNull(startRequest, "startRequest");
        synchronized (lifecycleMonitor) {
            if (running.get()) {
                throw new IllegalStateException(
                        "cannot attach a start request to a running turn loop for windowId=" + windowId);
            }
            if (pendingStartRequest != null) {
                throw new IllegalStateException("start request already attached for windowId=" + windowId);
            }
            pendingStartRequest = startRequest;
        }
    }

    /** Starts this window's explicit daemon loop without clearing in-memory acknowledgement state. */
    public void start() {
        synchronized (lifecycleMonitor) {
            if (retired) {
                throw new IllegalStateException("turn loop is permanently retired for windowId=" + windowId);
            }
            if (!running.compareAndSet(false, true)) {
                throw new IllegalStateException("turn loop is already running for windowId=" + windowId);
            }
            stopRequested.set(false);
            lastFailure = null;
            Thread thread = new Thread(this::runLoop, "dhxy-turn-" + windowId);
            thread.setDaemon(true);
            workerThread = thread;
            try {
                thread.start();
            } catch (RuntimeException | Error startFailure) {
                workerThread = null;
                running.set(false);
                lifecycleMonitor.notifyAll();
                throw startFailure;
            }
        }
    }

    /** Requests cooperative stop and interrupts the current long-wait or local action. */
    public void stop() {
        synchronized (lifecycleMonitor) {
            stopRequested.set(true);
            Thread thread = workerThread;
            if (thread != null) {
                thread.interrupt();
            }
        }
    }

    /**
     * TURN-40D: flips the loop-owned pause checkpoint so the next turn's metadata carries {@code pauseRequested}.
     * The DHXY long-wait loop stays alive and no local permanent-service mechanic is parked.
     */
    public void requestPause() {
        pauseCheckpoint.set(true);
        resumeRequested.set(false);
        // Wake only an in-flight Cloud wait. Local action/input interruption remains owned by its existing
        // checkpoints, so a user pause cannot create a second cancellation policy for physical input.
        synchronized (lifecycleMonitor) {
            if (exchangeInFlight) {
                Thread thread = workerThread;
                if (thread != null) {
                    thread.interrupt();
                }
            }
        }
        // User pause is a local observation boundary, not permission for paused read-only traffic.
        suspendObservationRunner();
        synchronized (pauseMonitor) {
            pauseMonitor.notifyAll();
        }
    }

    /** TURN-40D: clears the loop-owned pause checkpoint. Resume mints no new start request. */
    public void requestResume() {
        if (!pauseCheckpoint.get()) {
            return;
        }
        // The loop clears the checkpoint only after it has revalidated the acknowledged window.
        resumeRequested.set(true);
        synchronized (pauseMonitor) {
            pauseMonitor.notifyAll();
        }
    }

    /**
     * TURN-40D: requests a graceful remote stop. Sets the loop-owned stop checkpoint and interrupts an in-flight
     * long wait as needed; the loop then publishes exactly one final turn whose live metadata carries
     * {@code stopRequested=true} and only afterwards exits (see {@code runTurns}). Unlike {@link #stop()} this never
     * skips the single final stop-bearing turn.
     */
    public void requestStop() {
        synchronized (lifecycleMonitor) {
            stopCheckpoint.set(true);
            Thread thread = workerThread;
            if (thread != null) {
                thread.interrupt();
            }
        }
        synchronized (pauseMonitor) {
            pauseMonitor.notifyAll();
        }
    }

    /**
     * Waits for the explicitly started loop thread to stop.
     *
     * @param timeout positive maximum duration to wait
     * @return true when no loop thread is running before the timeout, otherwise false
     * @throws InterruptedException when the waiting caller is interrupted
     */
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
                long waitMillis = Math.max(1L, Duration.ofNanos(remainingNanos).toMillis());
                lifecycleMonitor.wait(waitMillis);
            }
            return true;
        }
    }

    public String deviceId() {
        return deviceId;
    }

    public String windowId() {
        return windowId;
    }

    public boolean isRunning() {
        return running.get();
    }

    public boolean isStopRequested() {
        return stopRequested.get();
    }

    /**
     * Waits until Cloud accepts the immutable task start request attached to this loop.
     *
     * @param timeout positive maximum duration to wait for the matching start acknowledgement
     * @return true only after the matching acknowledgement is accepted; false when the loop stops or times out first
     * @throws InterruptedException when the waiting caller is interrupted
     */
    public boolean awaitStartAcknowledged(Duration timeout) throws InterruptedException {
        if (timeout == null || timeout.isZero() || timeout.isNegative()) {
            throw new IllegalArgumentException("timeout must be positive");
        }
        long deadlineNanos = System.nanoTime() + timeout.toNanos();
        synchronized (lifecycleMonitor) {
            if (pendingStartRequest == null) {
                throw new IllegalStateException("turn loop has no attached start request for windowId=" + windowId);
            }
            while (running.get() && !startAckAccepted) {
                long remainingNanos = deadlineNanos - System.nanoTime();
                if (remainingNanos <= 0L) {
                    return false;
                }
                lifecycleMonitor.wait(Math.max(1L, Duration.ofNanos(remainingNanos).toMillis()));
            }
            return startAckAccepted;
        }
    }

    /**
     * Attach one manual MapSurvey command to this running task-free loop.
     *
     * @param command immutable command with its stable retry identity.
     * @return future completed once for the exact terminal Cloud result.
     */
    public CompletableFuture<TurnMapSurveyResult> attachMapSurveyCommand(TurnMapSurveyCommand command) {
        Objects.requireNonNull(command, "command");
        synchronized (lifecycleMonitor) {
            if (!running.get() || stopRequested.get() || stopCheckpoint.get()) {
                throw new IllegalStateException("MapSurvey requires a running turn loop for windowId=" + windowId);
            }
            if (pendingStartRequest != null) {
                throw new IllegalStateException("MapSurvey conflicts with a task loop for windowId=" + windowId);
            }
            if (pendingMapSurveyCommand != null || pendingMapSurveyAckId != null) {
                throw new IllegalStateException("MapSurvey command already active for windowId=" + windowId);
            }
            pendingMapSurveyCommand = command;
            pendingMapSurveyResult = new CompletableFuture<>();
            return pendingMapSurveyResult;
        }
    }

    public boolean isPauseRequested() {
        return pauseCheckpoint.get();
    }

    public Throwable lastFailure() {
        return lastFailure;
    }

    /** @return the accepted Cloud start ACK, including its authoritative effective task projection. */
    public Optional<TurnTaskStartAck> acceptedStartAck() {
        return Optional.ofNullable(acceptedStartAck);
    }

    /** Completes only for the terminal result carrying this loop's exact accepted start-request identity. */
    public CompletableFuture<TurnTaskTerminalResult> taskTerminalResult() {
        return taskTerminalResult;
    }

    /**
     * @return true only after Cloud returned a terminal result for this loop's exact start-request identity.
     */
    public boolean hasAcceptedTaskTerminal() {
        return taskTerminalResult.isDone();
    }

    /** Permanently prevents any later start, or fails when start already won the lifecycle race. */
    void retireIfStopped() {
        synchronized (lifecycleMonitor) {
            if (running.get()) {
                throw new IllegalStateException("cannot retire running turn loop for windowId=" + windowId);
            }
            retired = true;
        }
    }

    private void runLoop() {
        log.info("Turn loop started: deviceId={} windowId={}", deviceId, windowId);
        try {
            runTurns();
        } catch (RuntimeException localFailure) {
            if (!stopRequested.get() && !stopCheckpoint.get() && !Thread.currentThread().isInterrupted()) {
                lastFailure = localFailure;
                log.error(
                        "Turn loop stopped after local failure: deviceId={} windowId={} type={} message={}",
                        deviceId,
                        windowId,
                        localFailure.getClass().getSimpleName(),
                        localFailure.getMessage());
            }
        } finally {
            // TURN-40G: fence and terminate this window's observation runner with a bounded join before the loop
            // reports stopped, so no late sample, event or input can be published after loop teardown.
            closeObservationRunner();
            synchronized (lifecycleMonitor) {
                workerThread = null;
                running.set(false);
                lifecycleMonitor.notifyAll();
            }
            log.info(
                    "Turn loop stopped: deviceId={} windowId={} stopRequested={} stopCheckpoint={} failed={}",
                    deviceId,
                    windowId,
                    stopRequested.get(),
                    stopCheckpoint.get(),
                    lastFailure != null);
        }
    }

    /**
     * TURN-40G: starts the per-window observation runner exactly once per loop run, and only while the matching
     * start acknowledgement has been accepted. The runner binds the exact acknowledged identity
     * {@code windowId + hwnd + taskRunId} (the acknowledged start request id is the observation-plane run identity).
     * A runner start failure never fails the turn loop — observation is strictly subordinate to the command plane.
     */
    private void maybeStartObservationRunner(TurnWindowMetadata metadata) {
        if (observationRunnerFactory == null || !startAckAccepted
                || taskTerminalResult.isDone() || stopRequested.get() || stopCheckpoint.get()
                || pauseCheckpoint.get()) {
            return;
        }
        WindowObservationRunner existing = observationRunner;
        if (existing != null) {
            if (!existing.isRunning()) {
                existing.start();
            }
            return;
        }
        TurnTaskStartRequest startRequest = pendingStartRequest;
        if (startRequest == null) {
            return;
        }
        String nativeHandle = metadata.nativeHandle();
        if (nativeHandle == null || nativeHandle.isBlank()) {
            return;
        }
        TurnTaskStartAck startAck = acceptedStartAck;
        List<TurnTaskCode> effectiveTaskCodes = startAck == null
                ? null
                : startAck.effectiveTaskCodes();
        List<TurnTaskCode> observationTaskCodes = effectiveTaskCodes == null
                ? startRequest.taskCodes()
                : effectiveTaskCodes;
        if (observationTaskCodes.isEmpty()) {
            log.info("Observation runner skipped for empty effective task queue: windowId={} startRequestId={}",
                    windowId, startRequest.startRequestId());
            return;
        }
        StringJoiner taskCodes = new StringJoiner(",");
        for (TurnTaskCode code : observationTaskCodes) {
            taskCodes.add(code.name());
        }
        try {
            WindowObservationRunner runner = observationRunnerFactory.create(
                    deviceId, windowId, nativeHandle, taskCodes.toString(), startRequest.startRequestId());
            if (runner == null) {
                return;
            }
            observationRunner = runner;
            runner.start();
        } catch (RuntimeException runnerFailure) {
            observationRunner = null;
            log.error("Observation runner failed to start (turn loop continues): windowId={} message={}",
                    windowId, runnerFailure.getMessage());
        }
    }

    /**
     * TURN-40G: fences new sampling, cancels the in-flight send, and joins the runner within a bound. The loop
     * worker usually reaches this with its interrupt flag already set (that is how the loop is stopped), so the
     * pending interrupt is cleared for the bounded join and restored afterwards — otherwise the join would abort
     * immediately and the runner would outlive its loop.
     */
    private void closeObservationRunner() {
        WindowObservationRunner runner = observationRunner;
        if (runner == null) {
            return;
        }
        observationRunner = null;
        runner.requestStop();
        boolean interrupted = Thread.interrupted();
        try {
            if (!runner.awaitStopped(Duration.ofSeconds(3))) {
                log.warn("Observation runner did not stop within bound: windowId={}", windowId);
            }
        } catch (InterruptedException interruptedAgain) {
            interrupted = true;
        } finally {
            if (interrupted) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /** TURN-40G: current per-window observation runner, or {@code null}; package-visible for contract tests. */
    WindowObservationRunner observationRunner() {
        return observationRunner;
    }

    /**
     * Runs normal turns until a hard {@link #stop()} or a checkpoint {@link #requestStop()} is requested. A
     * checkpoint stop (which interrupts an in-flight long wait) is honored by publishing exactly one final turn whose
     * metadata carries {@code stopRequested=true} before the loop exits; a hard stop or a real transport failure ends
     * the loop with no final turn.
     */
    private void runTurns() {
        while (!stopRequested.get() && !stopCheckpoint.get() && !Thread.currentThread().isInterrupted()) {
            try {
                if (pauseCheckpoint.get()) {
                    if (!pausePublished) {
                        exchangeOnce();
                        pausePublished = true;
                        continue;
                    }
                    if (!awaitResumeRequest()) {
                        /*
                         * A checkpoint stop interrupts the paused wait. Leave the normal loop, then
                         * run the shared final stop-bearing exchange below; returning here would
                         * strand a stopped local loop without the Cloud terminal acknowledgement.
                         */
                        break;
                    }
                    TurnWindowMetadata resumedMetadata = Objects.requireNonNull(
                            windowMetadataSupplier.get(), "windowMetadataSupplier returned null");
                    requireExpectedIdentity(resumedMetadata.deviceId(), resumedMetadata.windowId(),
                            "resume metadata");
                    if (!matchesAcknowledgedWindow(resumedMetadata)) {
                        resumeRequested.set(false);
                        log.warn("Remote resume remains paused because the original window is not present: "
                                        + "deviceId={} windowId={} expectedTitle={} actualTitle={} expectedHwnd={} "
                                        + "actualHwnd={} expectedPid={} actualPid={}",
                                deviceId, windowId,
                                acknowledgedWindowMetadata == null ? null : acknowledgedWindowMetadata.windowTitle(),
                                resumedMetadata.windowTitle(),
                                acknowledgedWindowMetadata == null ? null : acknowledgedWindowMetadata.nativeHandle(),
                                resumedMetadata.nativeHandle(),
                                acknowledgedWindowMetadata == null ? null : acknowledgedWindowMetadata.processId(),
                                resumedMetadata.processId());
                        continue;
                    }
                    pauseCheckpoint.set(false);
                    pausePublished = false;
                    resumeRequested.set(false);
                    // Restart the retained acknowledged runner before entering another Cloud long wait. The
                    // existing object owns observerSeq, interests and unacknowledged typed events across suspend.
                    maybeStartObservationRunner(resumedMetadata);
                    exchangeOnce(resumedMetadata);
                    continue;
                }
                exchangeOnce();
            } catch (TurnTransportException transportFailure) {
                if (stopRequested.get()) {
                    return;
                }
                if (pauseCheckpoint.get()
                        && !stopCheckpoint.get()
                        && transportFailure.kind() == TurnTransportException.Kind.INTERRUPTED) {
                    // requestPause deliberately interrupts only exchangeInFlight. Clear that one control interrupt
                    // and continue with the single pause-bearing checkpoint turn; the acknowledged loop is retained.
                    Thread.interrupted();
                    continue;
                }
                if (stopCheckpoint.get() || Thread.currentThread().isInterrupted()) {
                    break;
                }
                lastFailure = transportFailure;
                log.error(
                        "Turn loop stopped after transport failure: deviceId={} windowId={} kind={} message={}",
                        deviceId,
                        windowId,
                        transportFailure.kind(),
                        transportFailure.getMessage());
                return;
            }
        }
        if (stopCheckpoint.get() && !stopRequested.get()) {
            // Exactly one final stop-bearing turn, uninterrupted, before the loop becomes stopped and is removed.
            Thread.interrupted();
            try {
                exchangeOnce();
            } catch (TurnTransportException finalTransportFailure) {
                lastFailure = finalTransportFailure;
                log.error(
                        "Turn loop final stop-bearing turn failed: deviceId={} windowId={} kind={} message={}",
                        deviceId,
                        windowId,
                        finalTransportFailure.kind(),
                        finalTransportFailure.getMessage());
            }
        }
    }

    /**
     * Stops paused observation traffic without discarding the acknowledged run's transport sequence or typed-fact
     * lineage. Resume revalidates the HWND and restarts this exact runner; terminal shutdown still uses
     * {@link #closeObservationRunner()} and clears the retained state.
     */
    private void suspendObservationRunner() {
        WindowObservationRunner runner = observationRunner;
        if (runner == null || !runner.isRunning()) {
            return;
        }
        runner.requestSuspend();
        boolean interrupted = Thread.interrupted();
        try {
            if (!runner.awaitStopped(Duration.ofSeconds(3))) {
                log.warn("Observation runner did not suspend within bound: windowId={}", windowId);
            }
        } catch (InterruptedException interruptedAgain) {
            interrupted = true;
        } finally {
            if (interrupted) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private boolean awaitResumeRequest() {
        synchronized (pauseMonitor) {
            while (pauseCheckpoint.get() && !resumeRequested.get()
                    && !stopRequested.get() && !stopCheckpoint.get()) {
                try {
                    pauseMonitor.wait();
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            }
        }
        return !stopRequested.get() && !stopCheckpoint.get() && !Thread.currentThread().isInterrupted();
    }

    private void exchangeOnce() throws TurnTransportException {
        exchangeOnce(null);
    }

    private void exchangeOnce(TurnWindowMetadata suppliedMetadata) throws TurnTransportException {
        TurnWindowMetadata metadata = suppliedMetadata == null
                ? Objects.requireNonNull(windowMetadataSupplier.get(), "windowMetadataSupplier returned null")
                : suppliedMetadata;
        requireExpectedIdentity(metadata.deviceId(), metadata.windowId(), "request metadata");
        if (!pauseCheckpoint.get() && !matchesAcknowledgedWindow(metadata)) {
            // A live drift must never be sent into the active Cloud task. Pause using the last
            // acknowledged generation and wait for the user to switch back before retrying.
            pauseCheckpoint.set(true);
            resumeRequested.set(false);
            pausePublished = false;
            suspendObservationRunner();
            log.warn("Remote turn paused before sending drifted native metadata: deviceId={} windowId={} "
                            + "expectedTitle={} actualTitle={} expectedHwnd={} actualHwnd={} expectedPid={} actualPid={}",
                    deviceId, windowId,
                    acknowledgedWindowMetadata == null ? null : acknowledgedWindowMetadata.windowTitle(),
                    metadata.windowTitle(),
                    acknowledgedWindowMetadata == null ? null : acknowledgedWindowMetadata.nativeHandle(),
                    metadata.nativeHandle(),
                    acknowledgedWindowMetadata == null ? null : acknowledgedWindowMetadata.processId(),
                    metadata.processId());
            metadata = acknowledgedWindowMetadata;
        }
        // TURN-40D (R1): the live loop owns the checkpoint flags; project them onto this turn's metadata so remote
        // pause/resume and the single final stop-bearing turn are carried without any control-side store.
        metadata = withCheckpointFlags(metadata, pauseCheckpoint.get(), stopCheckpoint.get());

        // TURN-40D: carry the immutable start request on every turn until its matching ack is accepted; a null here
        // means either no remote start or an already-accepted one — never a second start intent.
        TurnTaskStartRequest startRequestForThisTurn =
                (pendingStartRequest != null && !startAckAccepted) ? pendingStartRequest : null;
        TurnMapSurveyCommand mapSurveyCommandForThisTurn;
        String mapSurveyAckForThisTurn;
        synchronized (lifecycleMonitor) {
            mapSurveyCommandForThisTurn = pendingMapSurveyCommand;
            mapSurveyAckForThisTurn = pendingMapSurveyAckId;
        }
        TurnRequest request = TurnProtocolValidator.requireValid(new TurnRequest(
                CONTRACT_VERSION,
                metadata,
                metadata.pauseRequested() ? 0L : waitTimeoutMs,
                previousOutcome,
                startRequestForThisTurn,
                null,
                mapSurveyCommandForThisTurn,
                mapSurveyAckForThisTurn));
        byte[] pngForRequest = previousPng == null ? null : previousPng.clone();

        // Exactly one exchange is attempted. A thrown transport failure leaves both retained values untouched, so
        // the exact start request is re-sent unchanged on the next turn after uncertain transport.
        TurnExchangeResult exchangeResult;
        synchronized (lifecycleMonitor) {
            // requestPause may win after this request captured pre-pause metadata but before transport admission.
            // In that case send nothing and let the next loop iteration publish the pause checkpoint.
            if (pauseCheckpoint.get() && !request.window().pauseRequested()) {
                return;
            }
            exchangeInFlight = true;
        }
        try {
            exchangeResult = Objects.requireNonNull(
                    turnClient.exchange(request, pngForRequest),
                    "turnClient returned null exchange result");
        } finally {
            synchronized (lifecycleMonitor) {
                exchangeInFlight = false;
            }
        }

        // Every successful ACTION or IDLE response accepts the exact previous result carried by this request.
        previousOutcome = null;
        previousPng = null;

        TurnResponse response = Objects.requireNonNull(exchangeResult.response(), "turn response");
        if (response.status() == null) {
            throw new IllegalStateException("turn response status must not be null");
        }
        // Enforce the full start/ack correlation for this exchange. When this turn carried the start request the
        // validator guarantees a matching TurnTaskStartAck, so record the one-time acceptance and stop attaching it.
        TurnProtocolValidator.requireValid(response, request);
        if (startRequestForThisTurn != null) {
            synchronized (lifecycleMonitor) {
                acceptedStartAck = response.taskStartAck();
                startAckAccepted = true;
                acknowledgedWindowMetadata = metadata;
                lifecycleMonitor.notifyAll();
            }
        }
        acceptMatchingTaskTerminal(response.taskTerminalResult());
        // TURN-40G: only an acknowledged window observes. Covers both the first acknowledgement and an explicit
        // restart of an already-acknowledged loop; a loop without a task start request never starts a runner.
        maybeStartObservationRunner(metadata);
        synchronized (lifecycleMonitor) {
            if (mapSurveyAckForThisTurn != null && mapSurveyAckForThisTurn.equals(pendingMapSurveyAckId)) {
                pendingMapSurveyAckId = null;
                pendingMapSurveyResult = null;
            }
            TurnMapSurveyResult mapResult = response.mapSurveyResult();
            if (mapSurveyCommandForThisTurn != null && mapResult != null
                    && mapResult.status() != TurnMapSurveyResult.Status.ACCEPTED
                    && pendingMapSurveyCommand != null
                    && pendingMapSurveyCommand.commandId().equals(mapResult.commandId())) {
                CompletableFuture<TurnMapSurveyResult> completion = pendingMapSurveyResult;
                pendingMapSurveyCommand = null;
                pendingMapSurveyAckId = mapResult.commandId();
                if (completion != null) {
                    completion.complete(mapResult);
                }
            }
        }
        // TURN-40D (R1) stop-action gate: once a hard stop, a checkpoint stop, or an interrupt is in effect this turn
        // never dispatches a returned ACTION. This is what makes the single final stop-bearing turn (driven by
        // stopCheckpoint with the interrupt flag already cleared) purely announce stopRequested=true and execute
        // nothing physical — the returned action, if any, is validated for correlation but never handed to the
        // executor, and no outcome/png is retained from it.
        if (stopRequested.get() || stopCheckpoint.get() || Thread.currentThread().isInterrupted()) {
            return;
        }
        if (response.status() == TurnResponse.Status.IDLE) {
            if (response.action() != null) {
                throw new IllegalStateException("IDLE turn response must not contain an action");
            }
            return;
        }

        TurnAction action = TurnProtocolValidator.requireValid(Objects.requireNonNull(
                response.action(),
                "ACTION turn response must contain an action"));
        requireExpectedIdentity(action.deviceId(), action.windowId(), "turn action");

        ExecutedTurn executed;
        if (lastExecutedActionId != null && lastExecutedTurn == null) {
            throw new IllegalStateException(
                    "prior action execution is uncertain; refusing further physical execution");
        }
        if (action.actionId().equals(lastExecutedActionId)) {
            executed = Objects.requireNonNull(lastExecutedTurn, "cached turn missing for repeated actionId");
        } else {
            // Reaching a different action here proves the prior request received a successful acknowledgement.
            lastExecutedActionId = action.actionId();
            lastExecutedTurn = null;
            executed = Objects.requireNonNull(actionExecutor.execute(action), "actionExecutor returned null");
            requireExecutedCorrelation(action, executed.outcome());
            lastExecutedTurn = executed;
            if (registersPathingIntent(action)) {
                WindowObservationRunner runner = observationRunner;
                if (runner != null) {
                    runner.wakeForLocalStateChange();
                }
            }
        }

        previousOutcome = executed.outcome();
        previousPng = executed.optionalPng();
    }

    private boolean registersPathingIntent(TurnAction action) {
        if (action.pathingIntent() != null) {
            return true;
        }
        return action.steps().stream()
                .map(step -> step.localService())
                .filter(Objects::nonNull)
                .anyMatch(call -> call.operation() == TurnLocalOperation.WHOLE_TASK_PATHING_REGISTER);
    }

    private boolean matchesAcknowledgedWindow(TurnWindowMetadata metadata) {
        TurnWindowMetadata expected = acknowledgedWindowMetadata;
        return expected == null
                || (expected.windowTitle().equals(metadata.windowTitle())
                && expected.nativeHandle().equals(metadata.nativeHandle())
                && expected.processId() == metadata.processId());
    }

    private void acceptMatchingTaskTerminal(TurnTaskTerminalResult terminal) {
        TurnTaskStartRequest startRequest = pendingStartRequest;
        if (terminal == null || startRequest == null
                || !startRequest.startRequestId().equals(terminal.startRequestId())) {
            return;
        }
        if (!taskTerminalResult.complete(terminal)) {
            return;
        }
        if (terminal.status() == TurnTaskTerminalResult.Status.FAILED
                || terminal.status() == TurnTaskTerminalResult.Status.SKIPPED) {
            lastFailure = new IllegalStateException("Cloud task ended as " + terminal.status()
                    + " for startRequestId=" + terminal.startRequestId());
        }
        stopRequested.set(true);
        log.info("Cloud task terminal accepted: deviceId={} windowId={} startRequestId={} status={}",
                deviceId, windowId, terminal.startRequestId(), terminal.status());
    }

    /**
     * Unions the loop-owned checkpoint pause/stop flags with the supplied metadata's flags, preserving every other
     * fact. The loop may assert pause/stop through its checkpoint; it never clears a flag the supplier legitimately
     * reported (e.g. a local stop), so the effective flag is the OR of both sources.
     */
    private static TurnWindowMetadata withCheckpointFlags(TurnWindowMetadata metadata,
                                                          boolean pauseCheckpoint,
                                                          boolean stopCheckpoint) {
        boolean pauseRequested = pauseCheckpoint || metadata.pauseRequested();
        boolean stopRequested = stopCheckpoint || metadata.stopRequested();
        if (metadata.pauseRequested() == pauseRequested && metadata.stopRequested() == stopRequested) {
            return metadata;
        }
        return new TurnWindowMetadata(
                metadata.deviceId(),
                metadata.windowId(),
                metadata.windowTitle(),
                metadata.nativeHandle(),
                metadata.processId(),
                metadata.windowRect(),
                pauseRequested,
                stopRequested,
                metadata.pathingSnapshot(),
                metadata.windowRole(),
                metadata.localTeamSessionKey(),
                metadata.localLeaderWindowId(),
                metadata.localLeaderPresent(),
                metadata.localSupportMember(),
                metadata.startupMode());
    }

    private void requireExpectedIdentity(String actualDeviceId, String actualWindowId, String source) {
        if (!deviceId.equals(actualDeviceId) || !windowId.equals(actualWindowId)) {
            throw new IllegalStateException(source + " does not match immutable loop deviceId/windowId");
        }
    }

    private void requireExecutedCorrelation(TurnAction action, TurnOutcome outcome) {
        TurnOutcome validated = TurnProtocolValidator.requireValid(Objects.requireNonNull(outcome, "outcome"));
        requireExpectedIdentity(validated.window().deviceId(), validated.window().windowId(), "turn outcome");
        if (!action.actionId().equals(validated.actionId())) {
            throw new IllegalStateException("turn outcome actionId does not match executed action");
        }
    }

    private static String requireIdentity(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must be nonblank");
        }
        return value;
    }

    @FunctionalInterface
    interface TurnActionRunner {
        ExecutedTurn execute(TurnAction action);
    }
}
