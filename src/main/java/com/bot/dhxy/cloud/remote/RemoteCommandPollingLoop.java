package com.bot.dhxy.cloud.remote;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

public final class RemoteCommandPollingLoop implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(RemoteCommandPollingLoop.class);
    private static final String THREAD_NAME = "dhxy-remote-command-poller";

    private final Object lifecycleMonitor = new Object();
    private final RemoteCommandTransport transport;
    private final RemoteCommandHandler handler;
    private final RemoteCommandPollRequest pollRequest;
    private final RemoteTaskRunRegistry taskRunRegistry;
    private final RemoteOperationLedger operationLedger;
    private final RemoteTaskRunApiClient taskRunApiClient;
    private final AtomicBoolean stopRequested = new AtomicBoolean(false);
    private final AtomicBoolean running = new AtomicBoolean(false);

    private volatile Thread workerThread;
    private volatile Throwable lastFailure;
    private String confirmedCloudIncarnationId;

    public RemoteCommandPollingLoop(
            RemoteCommandTransport transport,
            RemoteCommandHandler handler,
            RemoteCommandPollRequest pollRequest,
            RemoteTaskRunRegistry taskRunRegistry,
            RemoteOperationLedger operationLedger,
            RemoteTaskRunApiClient taskRunApiClient) {
        this.transport = Objects.requireNonNull(transport, "transport");
        this.handler = Objects.requireNonNull(handler, "handler");
        this.pollRequest = Objects.requireNonNull(pollRequest, "pollRequest");
        this.taskRunRegistry = Objects.requireNonNull(taskRunRegistry, "taskRunRegistry");
        this.operationLedger = Objects.requireNonNull(operationLedger, "operationLedger");
        this.taskRunApiClient = Objects.requireNonNull(taskRunApiClient, "taskRunApiClient");
        this.operationLedger.bindSession(pollScope());
    }

    /**
     * Explicitly starts one daemon polling thread. The class has no Spring lifecycle or automatic startup hook.
     *
     * @throws IllegalStateException when this loop is already running
     */
    public void start() {
        synchronized (lifecycleMonitor) {
            if (!running.compareAndSet(false, true)) {
                throw new IllegalStateException("remote command polling loop is already running");
            }
            stopRequested.set(false);
            lastFailure = null;
            Thread thread = new Thread(this::runLoop, THREAD_NAME);
            thread.setDaemon(true);
            workerThread = thread;
            try {
                thread.start();
            } catch (RuntimeException | Error e) {
                workerThread = null;
                running.set(false);
                lifecycleMonitor.notifyAll();
                throw e;
            }
        }
    }

    /**
     * Requests cooperative shutdown and interrupts the active poll, idle wait, or handler call.
     */
    public void stop() {
        stopRequested.set(true);
        Thread thread = workerThread;
        if (thread != null) {
            thread.interrupt();
        }
    }

    /**
     * Waits for the explicit polling thread to terminate.
     *
     * @param timeout maximum wait duration; must be positive
     * @return true when stopped before the timeout, otherwise false
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

    public boolean isRunning() {
        return running.get();
    }

    public boolean isStopRequested() {
        return stopRequested.get();
    }

    public Throwable getLastFailure() {
        return lastFailure;
    }

    /**
     * Checks whether this fixed polling loop consumes the exact lifecycle session route.
     *
     * @param scope tenant/user/device/client-session identity; nullable returns false
     * @return true only when all four routing fields equal the immutable poll request
     */
    public boolean matchesSession(RemoteTaskRunScope scope) {
        return scope != null
                && Objects.equals(pollRequest.getTenantId(), scope.getTenantId())
                && Objects.equals(pollRequest.getUserId(), scope.getUserId())
                && Objects.equals(pollRequest.getDeviceId(), scope.getDeviceId())
                && Objects.equals(pollRequest.getClientSessionId(), scope.getClientSessionId());
    }

    @Override
    public void close() {
        stop();
    }

    private void runLoop() {
        log.info(
                "Remote command polling started: tenantId={} userId={} deviceId={} clientSessionId={}",
                pollRequest.getTenantId(),
                pollRequest.getUserId(),
                pollRequest.getDeviceId(),
                pollRequest.getClientSessionId());
        try {
            while (!stopRequested.get() && !Thread.currentThread().isInterrupted()) {
                flushFinalConsumedReceiptOnce();
                cleanupTerminalLedgerOnce();
                RemoteCommandPollResponse response = transport.poll(pollRequest);
                String observedIncarnation = response.getCloudIncarnationId();
                if (confirmedCloudIncarnationId != null
                        && !confirmedCloudIncarnationId.equals(observedIncarnation)) {
                    taskRunRegistry.invalidateExclusiveOwnersForIncarnationChange(pollScope());
                }
                operationLedger.bindCloudIncarnation(observedIncarnation);
                confirmedCloudIncarnationId = observedIncarnation;
                if (stopRequested.get() || Thread.currentThread().isInterrupted()) {
                    break;
                }
                if (response.getStatus() == RemoteCommandPollStatus.IDLE) {
                    flushExecutorReadinessOnce();
                    waitForNextPoll(response.getRetryAfterMs());
                    continue;
                }
                if (response.getStatus() == RemoteCommandPollStatus.FINAL_CONSUMED) {
                    operationLedger.applyFinalConsumedAck(Objects.requireNonNull(
                            response.getFinalConsumedAck(),
                            "FINAL_CONSUMED poll response must include an acknowledgement"));
                    flushExecutorReadinessOnce();
                    continue;
                }

                RemoteGameCommand command = Objects.requireNonNull(
                        response.getCommand(),
                        "COMMAND poll response must include a command");
                RemoteGameOutcomeEnvelope outcome = Objects.requireNonNull(
                        handler.handle(command),
                        "remote command handler returned null outcome");
                validateOutcomeCorrelation(command, outcome);
                transport.submitOutcome(outcome);
                flushExecutorReadinessOnce();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            if (!stopRequested.get()) {
                lastFailure = e;
            }
        } catch (Exception e) {
            if (!stopRequested.get() && !Thread.currentThread().isInterrupted()) {
                lastFailure = e;
                log.error(
                        "Remote command polling stopped after failure: tenantId={} userId={} deviceId={} clientSessionId={} type={} message={}",
                        pollRequest.getTenantId(),
                        pollRequest.getUserId(),
                        pollRequest.getDeviceId(),
                        pollRequest.getClientSessionId(),
                        e.getClass().getSimpleName(),
                        e.getMessage());
            }
        } finally {
            synchronized (lifecycleMonitor) {
                workerThread = null;
                running.set(false);
                lifecycleMonitor.notifyAll();
            }
            log.info(
                    "Remote command polling stopped: tenantId={} userId={} deviceId={} clientSessionId={} stopRequested={} failed={}",
                    pollRequest.getTenantId(),
                    pollRequest.getUserId(),
                    pollRequest.getDeviceId(),
                    pollRequest.getClientSessionId(),
                    stopRequested.get(),
                    lastFailure != null);
        }
    }

    /** Sends at most one retained receipt before the next long-poll, with no automatic retry. */
    private void flushFinalConsumedReceiptOnce() {
        RemoteOperationLedger.ReceiptSendHandle handle = operationLedger.claimReadyReceipt();
        if (handle == null) {
            return;
        }
        try {
            RemoteFinalConsumedReceiptAck acknowledgement =
                    transport.submitFinalConsumedReceipt(handle.receipt());
            operationLedger.markReceiptAccepted(handle, acknowledgement);
        } catch (RemoteCommandTransportException failure) {
            if (isPermanentReceiptFailure(failure)) {
                operationLedger.markReceiptPermanentRejected(handle);
            } else {
                operationLedger.markReceiptDeliveryUncertain(handle);
            }
            throw failure;
        } catch (RuntimeException | Error failure) {
            operationLedger.markReceiptDeliveryUncertain(handle);
            throw failure;
        }
    }

    private static boolean isPermanentReceiptFailure(
            RemoteCommandTransportException failure) {
        return switch (failure.getFailureType()) {
            case INVALID_REQUEST, SERIALIZATION, OUTCOME_REJECTED -> true;
            case HTTP_STATUS -> failure.getStatusCode() != null
                    && failure.getStatusCode() >= 400
                    && failure.getStatusCode() < 500
                    && failure.getStatusCode() != 408
                    && failure.getStatusCode() != 429;
            case HTTP_TIMEOUT, INTERRUPTED, IO, EMPTY_RESPONSE, DESERIALIZATION,
                    SCHEMA_MISMATCH -> false;
        };
    }

    private void cleanupTerminalLedgerOnce() {
        RemoteOperationLedger.TerminalCleanupCandidate candidate =
                operationLedger.claimTerminalCleanupCandidate();
        if (candidate == null) {
            return;
        }
        RemoteTaskRunRegistry.TerminalCleanupObservation observation =
                taskRunRegistry.observeTerminalCleanup(pollScope(), candidate);
        if (!observation.cleanupAllowed()) {
            operationLedger.deferTerminalCleanupCandidate(candidate);
            return;
        }
        if (!operationLedger.commitTerminalCleanup(candidate, observation)) {
            operationLedger.deferTerminalCleanupCandidate(candidate);
        }
    }

    private static void waitForNextPoll(long retryAfterMs) throws InterruptedException {
        if (retryAfterMs > 0L) {
            Thread.sleep(retryAfterMs);
        }
    }

    private void flushExecutorReadinessOnce() {
        PendingExecutorReadiness.PendingSendHandle handle = null;
        try {
            RemoteTaskRunScope scope = pollScope();
            PendingExecutorReadiness.DrainCandidate candidate =
                    taskRunRegistry.findDrainCandidate(scope);
            if (candidate != null) {
                RemoteOperationLedger.QuiescenceSnapshot snapshot =
                        operationLedger.quiescenceSnapshot(
                                candidate.registration(), candidate.toRevision());
                operationLedger.withCurrentSnapshot(snapshot,
                        () -> taskRunRegistry.materializeReady(candidate, snapshot));
            }
            handle = taskRunRegistry.claimReadyForSend(scope, System.nanoTime());
            if (handle == null) {
                return;
            }
            RemoteTaskRunReceipt receipt = taskRunApiClient.confirmResumedExecutorReady(
                    handle.retainedSend().request());
            taskRunRegistry.markAccepted(handle, receipt);
        } catch (RemoteTaskRunClientException e) {
            if (handle == null) {
                log.warn("Executor readiness client failure before claim type={}",
                        e.getFailureType());
                return;
            }
            RemoteTaskRunErrorCode code = typedRemoteCode(e);
            if (e.isOutcomeUncertain() || code == null || code == RemoteTaskRunErrorCode.INTERNAL_ERROR) {
                taskRunRegistry.markUnknownForRetry(handle,
                        saturatingAdd(System.nanoTime(), Duration.ofSeconds(1).toNanos()));
            } else {
                taskRunRegistry.markPermanentRejected(handle, code);
            }
            log.warn("Executor readiness send retained/classified requestIdPrefix={} type={} code={}",
                    prefix(handle.requestId()), e.getFailureType(), code);
        } catch (RuntimeException e) {
            if (handle != null) {
                taskRunRegistry.markUnknownForRetry(handle,
                        saturatingAdd(System.nanoTime(), Duration.ofSeconds(1).toNanos()));
            }
            if (Thread.currentThread().isInterrupted() && stopRequested.get()) {
                return;
            }
            log.warn("Executor readiness hook failed without stopping command poll type={} requestIdPrefix={}",
                    e.getClass().getSimpleName(), handle == null ? "none" : prefix(handle.requestId()));
        }
    }

    private RemoteTaskRunScope pollScope() {
        return RemoteTaskRunScope.builder()
                .tenantId(pollRequest.getTenantId())
                .userId(pollRequest.getUserId())
                .deviceId(pollRequest.getDeviceId())
                .clientSessionId(pollRequest.getClientSessionId())
                .build();
    }

    private static RemoteTaskRunErrorCode typedRemoteCode(RemoteTaskRunClientException exception) {
        String value = exception.getRemoteErrorCode();
        if (value == null) {
            return null;
        }
        try {
            return RemoteTaskRunErrorCode.valueOf(value);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private static long saturatingAdd(long value, long delta) {
        return value > Long.MAX_VALUE - delta ? Long.MAX_VALUE : value + delta;
    }

    private static String prefix(String value) {
        return value == null ? "none" : value.substring(0, Math.min(8, value.length()));
    }

    private void validateOutcomeCorrelation(
            RemoteGameCommand command,
            RemoteGameOutcomeEnvelope outcome) {
        boolean correlated = command.getContractVersion() == outcome.getContractVersion()
                && Objects.equals(pollRequest.getTenantId(), outcome.getTenantId())
                && Objects.equals(pollRequest.getUserId(), outcome.getUserId())
                && Objects.equals(pollRequest.getDeviceId(), outcome.getDeviceId())
                && Objects.equals(pollRequest.getClientSessionId(), outcome.getClientSessionId())
                && command.getOperation() == outcome.getOperation()
                && Objects.equals(command.getRequestId(), outcome.getRequestId())
                && Objects.equals(command.getActionId(), outcome.getActionId())
                && Objects.equals(command.getTaskRunId(), outcome.getTaskRunId())
                && Objects.equals(command.getSemanticAddress(), outcome.getSemanticAddress())
                && Objects.equals(command.getRequestDigest(), outcome.getRequestDigest());
        if (!correlated) {
            throw new IllegalStateException(
                    "handler outcome does not correlate to command requestId=" + command.getRequestId()
                            + " actionId=" + command.getActionId());
        }
    }
}
