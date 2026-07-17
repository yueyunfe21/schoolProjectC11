package com.bot.dhxy.cloud.remote;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Explicit, inactive local coordinator for remote task-run lifecycle state.
 *
 * <p>This service never starts the poller, creates a business task, dispatches a command, sends
 * input, unregisters terminal runs, or binds a replacement session to retained state.</p>
 */
public final class RemoteTaskRunLifecycleService {

    public static final int DEFAULT_GLOBAL_START_RESERVATIONS = 10_000;
    public static final int DEFAULT_OWNER_START_RESERVATIONS = 1_000;

    private static final BigInteger MAX_UNSIGNED_LONG = BigInteger.ONE.shiftLeft(64).subtract(BigInteger.ONE);

    private final RemoteTaskRunApiClient apiClient;
    private final RemoteTaskRunRegistry registry;
    private final RemoteCommandPollingLoop pollingLoop;
    private final Object reservationMonitor = new Object();
    private final Map<StartKey, StartReservation> startReservations = new HashMap<>();
    private final Map<OwnerKey, Integer> startReservationOwnerUsage = new HashMap<>();
    private final int globalStartReservationCapacity;
    private final int ownerStartReservationCapacity;

    public RemoteTaskRunLifecycleService(
            RemoteTaskRunApiClient apiClient,
            RemoteTaskRunRegistry registry,
            RemoteCommandPollingLoop pollingLoop) {
        this(
                apiClient,
                registry,
                pollingLoop,
                DEFAULT_GLOBAL_START_RESERVATIONS,
                DEFAULT_OWNER_START_RESERVATIONS);
    }

    public RemoteTaskRunLifecycleService(
            RemoteTaskRunApiClient apiClient,
            RemoteTaskRunRegistry registry,
            RemoteCommandPollingLoop pollingLoop,
            int globalStartReservationCapacity,
            int ownerStartReservationCapacity) {
        this.apiClient = Objects.requireNonNull(apiClient, "apiClient");
        this.registry = Objects.requireNonNull(registry, "registry");
        this.pollingLoop = Objects.requireNonNull(pollingLoop, "pollingLoop");
        if (globalStartReservationCapacity <= 0
                || ownerStartReservationCapacity <= 0
                || ownerStartReservationCapacity > globalStartReservationCapacity) {
            throw new IllegalArgumentException(
                    "start reservation capacities must be positive and owner <= global");
        }
        this.globalStartReservationCapacity = globalStartReservationCapacity;
        this.ownerStartReservationCapacity = ownerStartReservationCapacity;
    }

    /**
     * Prepares one run, reads its latest status, verifies local poll readiness, and activates it.
     *
     * @param scope exact tenant/user/device/client-session owner
     * @param startRequestId stable idempotency key for this requested run
     * @param taskType exact task type text preserved by the cloud coordinator
     * @param window indivisible logical/native window identity
     * @return locally published ACTIVE registration
     */
    public RemoteTaskRunRegistration prepareAndActivate(
            RemoteTaskRunScope scope,
            String startRequestId,
            String taskType,
            RemoteTaskRunWindow window) {
        ExpectedBinding expected = new ExpectedBinding(
                requireScope(scope),
                requireText(startRequestId, "startRequestId"),
                requireOriginalText(taskType, "taskType"),
                requireWindow(window),
                null);
        StartReservation reservation = acquireReservation(expected, true);
        try {
            boolean cleanupPendingBeforePrepare = reservationCleanupPending(reservation);
            RemoteTaskRunBinding prepared;
            try {
                prepared = apiClient.prepare(scope, startRequestId, taskType, window);
            } catch (RemoteTaskRunClientException e) {
                if (!e.isOutcomeUncertain()) {
                    RemoteTaskRunRegistration recovery = reservationRecoveryBinding(reservation);
                    if (cleanupPendingBeforePrepare) {
                        throw lifecycle(
                                RemoteTaskRunLifecycleException.Reason.REMOTE_STATE_UNCERTAIN,
                                true,
                                recovery,
                                true,
                                "PREPARE was rejected after an earlier uncertain outcome; "
                                        + "re-enter the same startRequestId",
                                e);
                    }
                    if (recovery != null) {
                        boolean cleanupPending = reservationCleanupPending(reservation);
                        throw lifecycle(
                                RemoteTaskRunLifecycleException.Reason.INVALID_REMOTE_STATE,
                                false,
                                recovery,
                                cleanupPending,
                                "PREPARE was rejected for an already retained start reservation",
                                e);
                    }
                    releaseUnboundStartReservation(reservation);
                    throw e;
                }
                RemoteTaskRunRegistration recovery = reservationRecoveryBinding(reservation);
                if (recovery == null) {
                    markReservationCleanupPending(reservation);
                }
                boolean cleanupPending = recovery == null || reservationCleanupPending(reservation);
                throw lifecycle(
                        RemoteTaskRunLifecycleException.Reason.REMOTE_STATE_UNCERTAIN,
                        true,
                        recovery,
                        cleanupPending,
                        "PREPARE outcome is uncertain; re-enter the same startRequestId",
                        e);
            } catch (RuntimeException e) {
                RemoteTaskRunRegistration recovery = reservationRecoveryBinding(reservation);
                if (recovery == null) {
                    markReservationCleanupPending(reservation);
                }
                boolean cleanupPending = recovery == null || reservationCleanupPending(reservation);
                throw lifecycle(
                        RemoteTaskRunLifecycleException.Reason.REMOTE_STATE_UNCERTAIN,
                        true,
                        recovery,
                        cleanupPending,
                        "PREPARE failed without proof that remote state was not created",
                        e);
            }
            try {
                validateExpected(prepared, expected);
                if (prepared.getStatus() != RemoteTaskRunWireStatus.PREPARED) {
                    throw lifecycle(
                            RemoteTaskRunLifecycleException.Reason.INVALID_REMOTE_STATE,
                            false,
                            "PREPARE must return the first PREPARED snapshot",
                            null);
                }
            } catch (RemoteTaskRunLifecycleException e) {
                markReservationCleanupPending(reservation);
                RemoteTaskRunRegistration recovery = reservationRecoveryBinding(reservation);
                throw lifecycle(
                        e.getReason(),
                        true,
                        recovery,
                        true,
                        "PREPARE response failed exact validation; re-enter the same startRequestId",
                        e);
            } catch (RuntimeException e) {
                markReservationCleanupPending(reservation);
                RemoteTaskRunRegistration recovery = reservationRecoveryBinding(reservation);
                throw lifecycle(
                        RemoteTaskRunLifecycleException.Reason.REMOTE_STATE_UNCERTAIN,
                        true,
                        recovery,
                        true,
                        "PREPARE response could not be validated; re-enter the same startRequestId",
                        e);
            }

            RemoteTaskRunRegistration preparedRegistration = toRegistration(prepared);
            bindPreparedReservation(reservation, preparedRegistration);
            ExpectedBinding runExpected = expected.withTaskRunId(prepared.getTaskRunId());
            RemoteTaskRunBinding latest;
            try {
                latest = apiClient.status(scope, prepared.getTaskRunId());
            } catch (RemoteTaskRunClientException e) {
                RemoteTaskRunRegistration recovery = reservationRecoveryBinding(reservation);
                if (recovery == null) {
                    recovery = preparedRegistration;
                }
                boolean cleanupPending = recovery.getStatus() == RemoteTaskRunStatus.STOPPING
                        || reservationCleanupPending(reservation);
                bindReservation(reservation, recovery, cleanupPending);
                boolean uncertain = e.isOutcomeUncertain();
                throw lifecycle(
                        uncertain
                                ? RemoteTaskRunLifecycleException.Reason.REMOTE_STATE_UNCERTAIN
                                : RemoteTaskRunLifecycleException.Reason.INVALID_REMOTE_STATE,
                        uncertain,
                        recovery,
                        cleanupPending,
                        uncertain
                                ? "initial STATUS did not converge; use the same startRequestId or taskRunId"
                                : "initial STATUS was rejected deterministically",
                        e);
            } catch (RuntimeException e) {
                RemoteTaskRunRegistration recovery = reservationRecoveryBinding(reservation);
                if (recovery == null) {
                    recovery = preparedRegistration;
                }
                boolean cleanupPending = recovery.getStatus() == RemoteTaskRunStatus.STOPPING
                        || reservationCleanupPending(reservation);
                bindReservation(reservation, recovery, cleanupPending);
                throw lifecycle(
                        RemoteTaskRunLifecycleException.Reason.REMOTE_STATE_UNCERTAIN,
                        true,
                        recovery,
                        cleanupPending,
                        "initial STATUS did not converge; use the same startRequestId or taskRunId",
                        e);
            }
            try {
                validateExpected(latest, runExpected);
                requireMonotonic(latest, prepared, false);
            } catch (RemoteTaskRunLifecycleException validationFailure) {
                bindReservation(reservation, preparedRegistration, true);
                throw lifecycle(
                        validationFailure.getReason(),
                        true,
                        preparedRegistration,
                        true,
                        "initial STATUS failed exact progress validation; "
                                + "use the same startRequestId or taskRunId",
                        validationFailure);
            }

            boolean cleanupRequired = reservationCleanupPending(reservation);
            RemoteTaskRunRegistration latestRegistration = toRegistration(latest);
            RemoteTaskRunRegistration local = registry.find(latest.getTaskRunId()).orElse(null);
            if (local != null) {
                try {
                    validateRegistrationExact(local, ExpectedBinding.from(latest));
                } catch (RemoteTaskRunLifecycleException validationFailure) {
                    bindReservation(reservation, preparedRegistration, true);
                    throw lifecycle(
                            validationFailure.getReason(),
                            true,
                            preparedRegistration,
                            true,
                            "existing local registration failed exact identity validation; "
                                    + "retained the last exact PREPARED recovery",
                            validationFailure);
                }
                bindReservation(reservation, local, cleanupRequired);
                if (local.getStatus().isTerminal()) {
                    try {
                        requireStatusProgress(latest, local);
                    } catch (RemoteTaskRunLifecycleException validationFailure) {
                        bindReservation(reservation, local, true);
                        throw lifecycle(
                                validationFailure.getReason(),
                                true,
                                local,
                                true,
                                "terminal local registration rejected remote STATUS progress",
                                validationFailure);
                    }
                    bindReservation(reservation, local, false);
                    throw lifecycle(
                            RemoteTaskRunLifecycleException.Reason.INVALID_REMOTE_STATE,
                            false,
                            local,
                            false,
                            "startRequestId already resolves to a terminal run",
                            null);
                }
                if (latest.getStatus().isTerminal()) {
                    try {
                        requireStatusProgress(latest, local);
                    } catch (RemoteTaskRunLifecycleException validationFailure) {
                        bindReservation(reservation, local, true);
                        throw lifecycle(
                                validationFailure.getReason(),
                                true,
                                local,
                                true,
                                "remote terminal STATUS failed canonical progress validation",
                                validationFailure);
                    }
                    RemoteTaskRunRegistration terminal = applyConfirmed(scope, latest);
                    bindReservation(reservation, terminal, false);
                    throw lifecycle(
                            RemoteTaskRunLifecycleException.Reason.INVALID_REMOTE_STATE,
                            false,
                            terminal,
                            false,
                            "startRequestId converged fail closed to terminal",
                            null);
                }
                if (cleanupRequired || local.getStatus() == RemoteTaskRunStatus.STOPPING) {
                    RemoteTaskRunRegistration stopping = local;
                    if (stopping.getStatus() != RemoteTaskRunStatus.STOPPING) {
                        stopping = beginStopPublication(
                                scope, stopping, "startRequestId cleanup");
                    }
                    bindReservation(reservation, stopping, true);
                    RemoteTaskRunRegistration trustedStopping = stopping;
                    try {
                        requireStatusProgress(latest, trustedStopping);
                    } catch (RemoteTaskRunLifecycleException cleanupFailure) {
                        throw lifecycle(
                                cleanupFailure.getReason(),
                                true,
                                trustedStopping,
                                true,
                                "startRequestId cleanup could not retain validated STOPPING progress",
                                cleanupFailure);
                    } catch (RuntimeException cleanupFailure) {
                        throw lifecycle(
                                RemoteTaskRunLifecycleException.Reason.REMOTE_STATE_UNCERTAIN,
                                true,
                                trustedStopping,
                                true,
                                "startRequestId cleanup could not retain STOPPING progress",
                                cleanupFailure);
                    }
                    stopping = retainStoppingProgress(scope, latestRegistration, trustedStopping);
                    bindReservation(reservation, stopping, true);
                    RemoteTaskRunRegistration terminal = stop(scope, latest.getTaskRunId());
                    bindReservation(reservation, terminal, false);
                    throw lifecycle(
                            RemoteTaskRunLifecycleException.Reason.INVALID_REMOTE_STATE,
                            false,
                            terminal,
                            false,
                            "startRequestId recovery completed fail-closed cleanup",
                            null);
                }
            } else if (latest.getStatus().isTerminal()) {
                try {
                    requireTerminalStopProgress(
                            latest, prepared.getRunRevision(), prepared.getStopEpoch());
                } catch (RemoteTaskRunLifecycleException validationFailure) {
                    bindReservation(reservation, preparedRegistration, true);
                    throw lifecycle(
                            validationFailure.getReason(),
                            true,
                            preparedRegistration,
                            true,
                            "initial terminal STATUS failed canonical progress validation",
                            validationFailure);
                }
                RemoteTaskRunRegistration terminal = publishTerminalWithoutLocalRegistration(
                        reservation, latestRegistration, preparedRegistration);
                throw lifecycle(
                        RemoteTaskRunLifecycleException.Reason.INVALID_REMOTE_STATE,
                        false,
                        terminal,
                        false,
                        "startRequestId recovered a terminal run",
                        null);
            } else {
                bindReservation(reservation, latestRegistration, cleanupRequired);
                if (cleanupRequired
                        || latest.getStatus() != RemoteTaskRunWireStatus.PREPARED) {
                    throw failWithoutLocalRegistration(
                            runExpected,
                            latest,
                            lifecycle(
                                    RemoteTaskRunLifecycleException.Reason.LOCAL_REGISTRATION_MISSING,
                                    false,
                                    "remote run is non-terminal without an exact local registration",
                                    null),
                            reservation);
                }
            }

            return switch (latest.getStatus()) {
                case PREPARED -> activatePrepared(runExpected, latest, reservation);
                case ACTIVE -> convergeExistingActive(runExpected, latest, reservation);
                case PAUSED -> {
                    bindReservation(reservation, local, true);
                    throw lifecycle(
                            RemoteTaskRunLifecycleException.Reason.INVALID_REMOTE_STATE,
                            true,
                            local,
                            true,
                            "latest prepared run is already PAUSED and requires fail-closed cleanup",
                            null);
                }
                case STOPPED, COMPLETED -> throw lifecycle(
                        RemoteTaskRunLifecycleException.Reason.INVALID_REMOTE_STATE,
                        false,
                        latestRegistration,
                        false,
                        "latest prepared run is terminal and cannot be reused",
                        null);
            };
        } finally {
            reservation.operationLock.unlock();
        }
    }

    /**
     * Requests the stable local token before the cloud PAUSE CAS.
     *
     * @return locally published PAUSED registration after cloud confirmation
     */
    public RemoteTaskRunRegistration pause(RemoteTaskRunScope scope, String taskRunId) {
        RemoteTaskRunRegistration initial = requireLocal(scope, taskRunId);
        StartReservation reservation = acquireReservation(ExpectedBinding.from(initial), false);
        try {
            RemoteTaskRunRegistration current = requireLocal(scope, taskRunId);
            validateRegistrationExact(current, ExpectedBinding.from(initial));
            if (current.getStatus() != RemoteTaskRunStatus.ACTIVE) {
                throw invalidLocalState("pause requires local ACTIVE", false);
            }
            boolean cleanupPending = reservationCleanupPending(reservation);
            requestPausePublication(
                    scope,
                    current,
                    cleanupPending,
                    "remote pause pending revision=" + current.getRunRevision());

            RemoteTaskRunBinding paused;
            try {
                paused = apiClient.pause(scope, current.getTaskRunId(), current.getRunRevision());
            } catch (RemoteTaskRunClientException e) {
                if (!shouldConverge(e)) {
                    throw lifecycle(
                            RemoteTaskRunLifecycleException.Reason.INVALID_REMOTE_STATE,
                            false,
                            current,
                            cleanupPending,
                            "PAUSE was rejected; stable local token remains paused",
                            e);
                }
                paused = statusAfterUncertain(
                        scope,
                        current,
                        e,
                        "PAUSE",
                        cleanupPending);
            } catch (RuntimeException e) {
                throw lifecycle(
                        RemoteTaskRunLifecycleException.Reason.REMOTE_STATE_UNCERTAIN,
                        true,
                        current,
                        cleanupPending,
                        "PAUSE failed without a typed remote outcome; STATUS convergence is required",
                        e);
            }
            try {
                validateExpected(paused, ExpectedBinding.from(current));
                requireStatusProgress(paused, current);
            } catch (RemoteTaskRunLifecycleException validationFailure) {
                throw lifecycle(
                        validationFailure.getReason(),
                        validationFailure.isRetryable(),
                        current,
                        cleanupPending,
                        validationFailure.getMessage(),
                        validationFailure);
            }
            if (paused.getStatus().isTerminal()) {
                RemoteTaskRunRegistration terminal;
                try {
                    terminal = applyConfirmed(scope, paused);
                } catch (RemoteTaskRunLifecycleException publicationFailure) {
                    if (publicationFailure.getRecoveryBinding().isPresent()) {
                        throw publicationFailure;
                    }
                    throw lifecycle(
                            publicationFailure.getReason(),
                            publicationFailure.isRetryable(),
                            current,
                            cleanupPending,
                            publicationFailure.getMessage(),
                            publicationFailure);
                }
                throw lifecycle(
                        RemoteTaskRunLifecycleException.Reason.INVALID_REMOTE_STATE,
                        false,
                        terminal,
                        false,
                        "PAUSE converged to terminal",
                        null);
            }
            if (paused.getStatus() != RemoteTaskRunWireStatus.PAUSED) {
                throw lifecycle(
                        RemoteTaskRunLifecycleException.Reason.REMOTE_STATE_UNCERTAIN,
                        true,
                        current,
                        cleanupPending,
                        "PAUSE is not confirmed; use STATUS on the same taskRunId",
                        null);
            }
            if (paused.getRunRevision() <= current.getRunRevision()) {
                throw lifecycle(
                        RemoteTaskRunLifecycleException.Reason.BINDING_MISMATCH,
                        false,
                        current,
                        cleanupPending,
                        "PAUSE confirmation did not advance runRevision",
                        null);
            }
            try {
                return applyConfirmed(scope, paused);
            } catch (RemoteTaskRunLifecycleException publicationFailure) {
                if (publicationFailure.getRecoveryBinding().isPresent()) {
                    throw publicationFailure;
                }
                throw lifecycle(
                        publicationFailure.getReason(),
                        publicationFailure.isRetryable(),
                        current,
                        cleanupPending,
                        publicationFailure.getMessage(),
                        publicationFailure);
            }
        } finally {
            reservation.operationLock.unlock();
        }
    }

    /**
     * Resumes remotely first and wakes the stable local token only after a newer ACTIVE binding.
     *
     * @return locally published ACTIVE registration
     */
    public RemoteTaskRunRegistration resume(RemoteTaskRunScope scope, String taskRunId) {
        RemoteTaskRunRegistration initial = requireLocal(scope, taskRunId);
        StartReservation reservation = acquireReservation(ExpectedBinding.from(initial), false);
        try {
            RemoteTaskRunRegistration current = requireLocal(scope, taskRunId);
            validateRegistrationExact(current, ExpectedBinding.from(initial));
            if (current.getStatus() != RemoteTaskRunStatus.PAUSED) {
                throw invalidLocalState("resume requires local PAUSED", false);
            }
            boolean cleanupPending = reservationCleanupPending(reservation);
            RemoteTaskRunBinding active;
            try {
                active = apiClient.resume(scope, current.getTaskRunId(), current.getRunRevision());
            } catch (RemoteTaskRunClientException e) {
                if (!shouldConverge(e)) {
                    throw lifecycle(
                            RemoteTaskRunLifecycleException.Reason.INVALID_REMOTE_STATE,
                            false,
                            current,
                            cleanupPending,
                            "RESUME was rejected; local run remains PAUSED",
                            e);
                }
                active = statusAfterUncertain(
                        scope,
                        current,
                        e,
                        "RESUME",
                        cleanupPending);
            } catch (RuntimeException e) {
                throw lifecycle(
                        RemoteTaskRunLifecycleException.Reason.REMOTE_STATE_UNCERTAIN,
                        true,
                        current,
                        cleanupPending,
                        "RESUME failed without a typed remote outcome; STATUS convergence is required",
                        e);
            }
            try {
                validateExpected(active, ExpectedBinding.from(current));
                requireStatusProgress(active, current);
            } catch (RemoteTaskRunLifecycleException validationFailure) {
                throw lifecycle(
                        validationFailure.getReason(),
                        validationFailure.isRetryable(),
                        current,
                        cleanupPending,
                        validationFailure.getMessage(),
                        validationFailure);
            }
            if (active.getStatus().isTerminal()) {
                RemoteTaskRunRegistration terminal;
                try {
                    terminal = applyConfirmed(scope, active);
                } catch (RemoteTaskRunLifecycleException publicationFailure) {
                    if (publicationFailure.getRecoveryBinding().isPresent()) {
                        throw publicationFailure;
                    }
                    throw lifecycle(
                            publicationFailure.getReason(),
                            publicationFailure.isRetryable(),
                            current,
                            cleanupPending,
                            publicationFailure.getMessage(),
                            publicationFailure);
                }
                throw lifecycle(
                        RemoteTaskRunLifecycleException.Reason.INVALID_REMOTE_STATE,
                        false,
                        terminal,
                        false,
                        "RESUME converged to terminal",
                        null);
            }
            if (active.getStatus() != RemoteTaskRunWireStatus.ACTIVE) {
                throw lifecycle(
                        RemoteTaskRunLifecycleException.Reason.REMOTE_STATE_UNCERTAIN,
                        true,
                        current,
                        cleanupPending,
                        "RESUME is not confirmed; use STATUS on the same taskRunId",
                        null);
            }
            if (active.getRunRevision() <= current.getRunRevision()) {
                throw lifecycle(
                        RemoteTaskRunLifecycleException.Reason.BINDING_MISMATCH,
                        false,
                        current,
                        cleanupPending,
                        "RESUME confirmation did not advance runRevision",
                        null);
            }
            try {
                return applyConfirmed(scope, active);
            } catch (RemoteTaskRunLifecycleException publicationFailure) {
                if (publicationFailure.getRecoveryBinding().isPresent()) {
                    throw publicationFailure;
                }
                throw lifecycle(
                        publicationFailure.getReason(),
                        publicationFailure.isRetryable(),
                        current,
                        cleanupPending,
                        publicationFailure.getMessage(),
                        publicationFailure);
            }
        } finally {
            reservation.operationLock.unlock();
        }
    }

    /**
     * Fails closed locally before issuing the cloud STOP CAS.
     *
     * @return locally published STOPPED or COMPLETED registration
     */
    public RemoteTaskRunRegistration stop(RemoteTaskRunScope scope, String taskRunId) {
        RemoteTaskRunRegistration initial = requireLocal(scope, taskRunId);
        StartReservation reservation = acquireReservation(ExpectedBinding.from(initial), false);
        try {
            RemoteTaskRunRegistration previous = requireLocal(scope, taskRunId);
            validateRegistrationExact(previous, ExpectedBinding.from(initial));
            if (previous.getStatus().isTerminal()) {
                throw lifecycle(
                        RemoteTaskRunLifecycleException.Reason.INVALID_REMOTE_STATE,
                        false,
                        previous,
                        false,
                        "stop cannot change a terminal local run",
                        null);
            }
            RemoteTaskRunRegistration stopping;
            if (previous.getStatus() == RemoteTaskRunStatus.STOPPING) {
                stopping = previous;
            } else {
                stopping = beginStopPublication(scope, previous, "STOP");
            }
            bindReservation(reservation, stopping, true);

            RemoteTaskRunBinding terminal;
            try {
                terminal = apiClient.stop(
                        scope, stopping.getTaskRunId(), stopping.getRunRevision());
            } catch (RemoteTaskRunClientException e) {
                if (!shouldConverge(e)) {
                    throw lifecycle(
                            RemoteTaskRunLifecycleException.Reason.INVALID_REMOTE_STATE,
                            false,
                            stopping,
                            true,
                            "STOP was rejected; local run remains STOPPING",
                            e);
                }
                terminal = statusAfterUncertain(scope, stopping, e, "STOP", true);
            } catch (RuntimeException e) {
                throw lifecycle(
                        RemoteTaskRunLifecycleException.Reason.REMOTE_STATE_UNCERTAIN,
                        true,
                        stopping,
                        true,
                        "STOP failed without a typed remote outcome; local run remains STOPPING",
                        e);
            }
            try {
                validateExpected(terminal, ExpectedBinding.from(stopping));
                requireStatusProgress(terminal, stopping);
            } catch (RemoteTaskRunLifecycleException validationFailure) {
                throw lifecycle(
                        validationFailure.getReason(),
                        validationFailure.isRetryable(),
                        stopping,
                        true,
                        validationFailure.getMessage(),
                        validationFailure);
            }
            if (!terminal.getStatus().isTerminal()) {
                RemoteTaskRunRegistration recovery = retainStoppingProgress(
                        scope, toRegistration(terminal), stopping);
                bindReservation(reservation, recovery, true);
                throw lifecycle(
                        RemoteTaskRunLifecycleException.Reason.REMOTE_STATE_UNCERTAIN,
                        true,
                        recovery,
                        true,
                        "STOP remains non-terminal at the latest retained revision",
                        null);
            }
            RemoteTaskRunRegistration confirmed;
            try {
                confirmed = applyConfirmed(scope, terminal);
            } catch (RemoteTaskRunLifecycleException publicationFailure) {
                if (publicationFailure.getRecoveryBinding().isPresent()) {
                    throw publicationFailure;
                }
                throw lifecycle(
                        publicationFailure.getReason(),
                        publicationFailure.isRetryable(),
                        stopping,
                        true,
                        publicationFailure.getMessage(),
                        publicationFailure);
            }
            bindReservation(reservation, confirmed, false);
            return confirmed;
        } finally {
            reservation.operationLock.unlock();
        }
    }

    /**
     * Queries one exact remote run and publishes its validated state to the local registry.
     *
     * <p>This operation may pause or resume the stable token. Publishing ACTIVE requires the exact
     * session poller to be healthy; otherwise it first publishes local STOPPING and attempts a
     * fail-closed same-run STOP. A non-terminal remote snapshot never revives local STOPPING.</p>
     *
     * @param scope exact tenant/user/device/client-session owner
     * @param taskRunId exact retained cloud task-run id
     * @return resulting local registration
     */
    public RemoteTaskRunRegistration reconcile(RemoteTaskRunScope scope, String taskRunId) {
        RemoteTaskRunRegistration initial = requireLocal(scope, taskRunId);
        StartReservation reservation = acquireReservation(ExpectedBinding.from(initial), false);
        try {
            RemoteTaskRunRegistration current = requireLocal(scope, taskRunId);
            validateRegistrationExact(current, ExpectedBinding.from(initial));
            RemoteTaskRunBinding remote;
            try {
                remote = apiClient.status(scope, current.getTaskRunId());
            } catch (RemoteTaskRunClientException e) {
                boolean cleanupPending = current.getStatus() == RemoteTaskRunStatus.STOPPING
                        || reservationCleanupPending(reservation);
                bindReservation(reservation, current, cleanupPending);
                boolean uncertain = e.isOutcomeUncertain();
                throw lifecycle(
                        uncertain
                                ? RemoteTaskRunLifecycleException.Reason.REMOTE_STATE_UNCERTAIN
                                : RemoteTaskRunLifecycleException.Reason.INVALID_REMOTE_STATE,
                        uncertain,
                        current,
                        cleanupPending,
                        uncertain
                                ? "reconcile STATUS did not converge"
                                : "reconcile STATUS was rejected deterministically",
                        e);
            } catch (RuntimeException e) {
                boolean cleanupPending = current.getStatus() == RemoteTaskRunStatus.STOPPING
                        || reservationCleanupPending(reservation);
                bindReservation(reservation, current, cleanupPending);
                throw lifecycle(
                        RemoteTaskRunLifecycleException.Reason.REMOTE_STATE_UNCERTAIN,
                        true,
                        current,
                        cleanupPending,
                        "reconcile STATUS could not converge",
                        e);
            }
            try {
                validateExpected(remote, ExpectedBinding.from(current));
                requireStatusProgress(remote, current);
            } catch (RemoteTaskRunLifecycleException validationFailure) {
                boolean cleanupPending = current.getStatus() == RemoteTaskRunStatus.STOPPING
                        || reservationCleanupPending(reservation);
                throw lifecycle(
                        validationFailure.getReason(),
                        validationFailure.isRetryable(),
                        current,
                        cleanupPending,
                        validationFailure.getMessage(),
                        validationFailure);
            }
            if (current.getStatus() == RemoteTaskRunStatus.STOPPING
                    && !remote.getStatus().isTerminal()) {
                RemoteTaskRunRegistration recovery = retainStoppingProgress(
                        scope, toRegistration(remote), current);
                bindReservation(reservation, recovery, true);
                throw lifecycle(
                        RemoteTaskRunLifecycleException.Reason.REMOTE_STATE_UNCERTAIN,
                        true,
                        recovery,
                        true,
                        "local STOPPING retained non-terminal revision without publishing remote state",
                        null);
            }
            RemoteTaskRunRegistration confirmed;
            try {
                confirmed = applyConfirmed(scope, remote);
            } catch (RemoteTaskRunLifecycleException publicationFailure) {
                if (publicationFailure.getRecoveryBinding().isPresent()) {
                    throw publicationFailure;
                }
                boolean cleanupPending = current.getStatus() == RemoteTaskRunStatus.STOPPING
                        || reservationCleanupPending(reservation);
                throw lifecycle(
                        publicationFailure.getReason(),
                        publicationFailure.isRetryable(),
                        current,
                        cleanupPending,
                        publicationFailure.getMessage(),
                        publicationFailure);
            } catch (RuntimeException publicationFailure) {
                boolean cleanupPending = current.getStatus() == RemoteTaskRunStatus.STOPPING
                        || reservationCleanupPending(reservation);
                throw lifecycle(
                        RemoteTaskRunLifecycleException.Reason.REMOTE_STATE_UNCERTAIN,
                        true,
                        current,
                        cleanupPending,
                        "reconcile binding could not be published over the trusted local registration",
                        publicationFailure);
            }
            bindReservation(reservation, confirmed, false);
            return confirmed;
        } finally {
            reservation.operationLock.unlock();
        }
    }

    /**
     * Explicitly consumes one exact terminal run and releases both retained local capacities.
     *
     * @param scope exact tenant/user/device/client-session owner
     * @param expectedTerminal exact terminal recovery snapshot returned by lifecycle state
     * @return released terminal registration
     */
    public RemoteTaskRunRegistration consumeTerminal(
            RemoteTaskRunScope scope,
            RemoteTaskRunRegistration expectedTerminal) {
        RemoteTaskRunScope requiredScope = requireScope(scope);
        if (expectedTerminal == null || !expectedTerminal.getStatus().isTerminal()) {
            throw invalidLocalState("only an exact terminal recovery binding can be consumed", false);
        }
        ExpectedBinding expected = ExpectedBinding.from(expectedTerminal);
        if (!sameScope(requiredScope, expected.scope())) {
            throw lifecycle(
                    RemoteTaskRunLifecycleException.Reason.BINDING_MISMATCH,
                    false,
                    expectedTerminal,
                    false,
                    "terminal recovery binding does not belong to the supplied scope",
                    null);
        }
        StartReservation reservation = acquireReservation(expected, false);
        try {
            synchronized (reservationMonitor) {
                if (startReservations.get(reservation.key) != reservation
                        || reservation.released
                        || !Objects.equals(reservation.taskRunId, expectedTerminal.getTaskRunId())
                        || reservation.cleanupPending
                        || !expectedTerminal.equals(reservation.recoveryBinding)) {
                    throw lifecycle(
                            RemoteTaskRunLifecycleException.Reason.BINDING_MISMATCH,
                            false,
                            expectedTerminal,
                            false,
                            "terminal start reservation is missing or does not match",
                            null);
                }
                RemoteTaskRunRegistration current = registry.find(expectedTerminal.getTaskRunId())
                        .orElse(null);
                RemoteTaskRunRegistration released = expectedTerminal;
                if (current != null) {
                    if (!current.equals(expectedTerminal) || !current.getStatus().isTerminal()) {
                        throw lifecycle(
                                RemoteTaskRunLifecycleException.Reason.BINDING_MISMATCH,
                                false,
                                expectedTerminal,
                                false,
                                "registry does not contain the exact terminal recovery snapshot",
                                null);
                    }
                    released = releaseTerminalPublication(scope, expectedTerminal, reservation);
                }
                startReservations.remove(reservation.key, reservation);
                decrementStartReservationUsage(reservation.key.owner());
                reservation.released = true;
                return released;
            }
        } finally {
            reservation.operationLock.unlock();
        }
    }

    /** Finds retained state for explicit replacement cleanup without registering or taking it over. */
    public RemoteTaskRunBinding findReplacement(
            RemoteTaskRunScope replacementScope,
            String startRequestId) {
        requireScope(replacementScope);
        String expectedStartRequestId = requireText(startRequestId, "startRequestId");
        RemoteTaskRunBinding retained = apiClient.findReplacement(
                replacementScope, expectedStartRequestId);
        validateWireBinding(retained);
        if (!sameOwnerWithoutSession(replacementScope, retained.getScope())
                || !expectedStartRequestId.equals(retained.getStartRequestId())
                || replacementScope.getClientSessionId().equals(
                        retained.getScope().getClientSessionId())) {
            throw lifecycle(
                    RemoteTaskRunLifecycleException.Reason.BINDING_MISMATCH,
                    false,
                    "replacement lookup returned a non-matching retained owner",
                    null);
        }
        return retained;
    }

    /** Stops a retained replacement binding without registering, rebinding, or taking it over. */
    public RemoteTaskRunBinding stopReplacement(
            RemoteTaskRunScope replacementScope,
            RemoteTaskRunBinding retained) {
        requireScope(replacementScope);
        validateWireBinding(retained);
        if (!sameOwnerWithoutSession(replacementScope, retained.getScope())
                || replacementScope.getClientSessionId().equals(
                        retained.getScope().getClientSessionId())) {
            throw lifecycle(
                    RemoteTaskRunLifecycleException.Reason.BINDING_MISMATCH,
                    false,
                    "replacement STOP cannot take over the retained binding",
                    null);
        }
        if (retained.getStatus().isTerminal()) {
            return retained;
        }
        RemoteTaskRunRegistration retainedRegistration = toRegistration(retained);

        RemoteTaskRunBinding stopped;
        try {
            stopped = apiClient.stopReplacement(
                    replacementScope,
                    retained.getTaskRunId(),
                    retained.getRunRevision());
        } catch (RemoteTaskRunClientException e) {
            if (!shouldConverge(e)) {
                throw lifecycle(
                        RemoteTaskRunLifecycleException.Reason.INVALID_REMOTE_STATE,
                        false,
                        retainedRegistration,
                        true,
                        "STOP_REPLACEMENT was rejected; retained cleanup remains unresolved",
                        e);
            }
            if (e.getFailureType() == RemoteTaskRunClientException.FailureType.INTERRUPTED) {
                throw lifecycle(
                        RemoteTaskRunLifecycleException.Reason.REMOTE_STATE_UNCERTAIN,
                        true,
                        retainedRegistration,
                        true,
                        "STOP_REPLACEMENT was interrupted; FIND_REPLACEMENT convergence is still required",
                        e);
            }
            try {
                stopped = apiClient.findReplacement(
                        replacementScope, retained.getStartRequestId());
            } catch (RemoteTaskRunClientException findFailure) {
                findFailure.addSuppressed(e);
                boolean uncertain = findFailure.isOutcomeUncertain();
                throw lifecycle(
                        uncertain
                                ? RemoteTaskRunLifecycleException.Reason.REMOTE_STATE_UNCERTAIN
                                : RemoteTaskRunLifecycleException.Reason.INVALID_REMOTE_STATE,
                        uncertain,
                        retainedRegistration,
                        true,
                        uncertain
                                ? "STOP_REPLACEMENT is uncertain and FIND_REPLACEMENT did not converge"
                                : "STOP_REPLACEMENT convergence lookup was rejected deterministically",
                        findFailure);
            } catch (RuntimeException findFailure) {
                findFailure.addSuppressed(e);
                throw lifecycle(
                        RemoteTaskRunLifecycleException.Reason.REMOTE_STATE_UNCERTAIN,
                        true,
                        retainedRegistration,
                        true,
                        "STOP_REPLACEMENT is uncertain and FIND_REPLACEMENT did not converge",
                        findFailure);
            }
        } catch (RuntimeException e) {
            throw lifecycle(
                    RemoteTaskRunLifecycleException.Reason.REMOTE_STATE_UNCERTAIN,
                    true,
                    retainedRegistration,
                    true,
                    "STOP_REPLACEMENT failed without a typed remote outcome; retained cleanup is unresolved",
                    e);
        }
        try {
            validateExpected(stopped, ExpectedBinding.from(retained));
            requireMonotonic(stopped, retained, false);
        } catch (RemoteTaskRunLifecycleException validationFailure) {
            throw lifecycle(
                    validationFailure.getReason(),
                    validationFailure.isRetryable(),
                    retainedRegistration,
                    true,
                    validationFailure.getMessage(),
                    validationFailure);
        }
        if (!stopped.getStatus().isTerminal()) {
            throw lifecycle(
                    RemoteTaskRunLifecycleException.Reason.REMOTE_STATE_UNCERTAIN,
                    true,
                    toRegistration(stopped),
                    true,
                    "retained replacement run is still non-terminal; cleanup can be retried",
                    null);
        }
        try {
            requireTerminalStopProgress(
                    stopped, retained.getRunRevision(), retained.getStopEpoch());
        } catch (RemoteTaskRunLifecycleException validationFailure) {
            throw lifecycle(
                    validationFailure.getReason(),
                    validationFailure.isRetryable(),
                    retainedRegistration,
                    true,
                    validationFailure.getMessage(),
                    validationFailure);
        }
        return stopped;
    }

    private RemoteTaskRunRegistration activatePrepared(
            ExpectedBinding expected,
            RemoteTaskRunBinding prepared,
            StartReservation reservation) {
        RemoteTaskRunRegistration preparedRegistration = toRegistration(prepared);
        RemoteTaskRunRegistration localPrepared;
        try {
            localPrepared = registry.register(preparedRegistration);
        } catch (RuntimeException registrationFailure) {
            RemoteTaskRunRegistration existing = registry.find(prepared.getTaskRunId()).orElse(null);
            if (existing == null) {
                throw failWithoutLocalRegistration(
                        expected, prepared, registrationFailure, reservation);
            }
            try {
                validateRegistrationExact(existing, ExpectedBinding.from(prepared));
            } catch (RemoteTaskRunLifecycleException validationFailure) {
                validationFailure.addSuppressed(registrationFailure);
                bindReservation(reservation, preparedRegistration, true);
                throw lifecycle(
                        validationFailure.getReason(),
                        true,
                        preparedRegistration,
                        true,
                        "registration race produced a conflicting local identity; retained PREPARED recovery",
                        validationFailure);
            }
            RemoteTaskRunBinding latest;
            try {
                latest = apiClient.status(expected.scope(), prepared.getTaskRunId());
            } catch (RemoteTaskRunClientException statusFailure) {
                statusFailure.addSuppressed(registrationFailure);
                boolean cleanupPending = existing.getStatus() == RemoteTaskRunStatus.STOPPING
                        || reservationCleanupPending(reservation);
                bindReservation(reservation, existing, cleanupPending);
                boolean uncertain = statusFailure.isOutcomeUncertain();
                throw lifecycle(
                        uncertain
                                ? RemoteTaskRunLifecycleException.Reason.REMOTE_STATE_UNCERTAIN
                                : RemoteTaskRunLifecycleException.Reason.INVALID_REMOTE_STATE,
                        uncertain,
                        existing,
                        cleanupPending,
                        uncertain
                                ? "initial registration raced with an exact local run; STATUS did not converge"
                                : "initial registration race STATUS was rejected deterministically",
                        statusFailure);
            } catch (RuntimeException statusFailure) {
                statusFailure.addSuppressed(registrationFailure);
                boolean cleanupPending = existing.getStatus() == RemoteTaskRunStatus.STOPPING
                        || reservationCleanupPending(reservation);
                bindReservation(
                        reservation,
                        existing,
                        cleanupPending);
                throw lifecycle(
                        RemoteTaskRunLifecycleException.Reason.REMOTE_STATE_UNCERTAIN,
                        true,
                        existing,
                        cleanupPending,
                        "initial registration raced with an exact local run; STATUS did not converge",
                        statusFailure);
            }
            try {
                validateExpected(latest, expected);
                requireStatusProgress(latest, existing);
            } catch (RemoteTaskRunLifecycleException validationFailure) {
                validationFailure.addSuppressed(registrationFailure);
                bindReservation(reservation, existing, true);
                throw lifecycle(
                        validationFailure.getReason(),
                        true,
                        existing,
                        true,
                        "initial registration race STATUS failed exact progress validation",
                        validationFailure);
            }
            if (existing.getStatus().isTerminal()) {
                bindReservation(reservation, existing, false);
                throw lifecycle(
                        RemoteTaskRunLifecycleException.Reason.INVALID_REMOTE_STATE,
                        false,
                        existing,
                        false,
                        "initial registration found an exact terminal local run",
                        registrationFailure);
            }
            if (latest.getStatus().isTerminal()) {
                RemoteTaskRunRegistration terminal = applyConfirmed(expected.scope(), latest);
                bindReservation(reservation, terminal, false);
                throw lifecycle(
                        RemoteTaskRunLifecycleException.Reason.INVALID_REMOTE_STATE,
                        false,
                        terminal,
                        false,
                        "initial registration race converged to terminal",
                        registrationFailure);
            }
            if (existing.getStatus() == RemoteTaskRunStatus.STOPPING) {
                RemoteTaskRunRegistration stopping = retainStoppingProgress(
                        expected.scope(), toRegistration(latest), existing);
                bindReservation(reservation, stopping, true);
                RemoteTaskRunRegistration terminal = stop(
                        expected.scope(), stopping.getTaskRunId());
                throw lifecycle(
                        RemoteTaskRunLifecycleException.Reason.INVALID_REMOTE_STATE,
                        false,
                        terminal,
                        false,
                        "initial registration race completed existing STOPPING cleanup",
                        registrationFailure);
            }
            if (latest.getStatus() == RemoteTaskRunWireStatus.ACTIVE) {
                RemoteTaskRunRegistration active = convergeExistingActive(
                        expected, latest, reservation);
                bindReservation(reservation, active, false);
                return active;
            }
            if (latest.getStatus() != RemoteTaskRunWireStatus.PREPARED
                    || existing.getStatus() != RemoteTaskRunStatus.PREPARED) {
                bindReservation(reservation, existing, true);
                throw lifecycle(
                        RemoteTaskRunLifecycleException.Reason.INVALID_REMOTE_STATE,
                        true,
                        existing,
                        true,
                        "initial registration found incompatible exact lifecycle state",
                        registrationFailure);
            }
            localPrepared = existing;
        }
        bindReservation(reservation, localPrepared, false);
        if (!pollerReady(expected.scope())) {
            throw failForPollerNotReady(expected.scope(), prepared, localPrepared);
        }

        boolean cleanupPending = reservationCleanupPending(reservation);
        RemoteTaskRunBinding active;
        try {
            active = apiClient.activate(
                    expected.scope(), prepared.getTaskRunId(), prepared.getRunRevision());
        } catch (RemoteTaskRunClientException e) {
            if (!shouldConverge(e)) {
                throw lifecycle(
                        RemoteTaskRunLifecycleException.Reason.INVALID_REMOTE_STATE,
                        false,
                        localPrepared,
                        cleanupPending,
                        "ACTIVATE was rejected; local run remains PREPARED",
                        e);
            }
            active = statusAfterUncertain(
                    expected.scope(),
                    localPrepared,
                    e,
                    "ACTIVATE",
                    cleanupPending);
        } catch (RuntimeException e) {
            throw lifecycle(
                    RemoteTaskRunLifecycleException.Reason.REMOTE_STATE_UNCERTAIN,
                    true,
                    localPrepared,
                    cleanupPending,
                    "ACTIVATE failed without a typed remote outcome; local run remains PREPARED",
                    e);
        }
        try {
            validateExpected(active, expected);
            requireStatusProgress(active, localPrepared);
        } catch (RemoteTaskRunLifecycleException validationFailure) {
            bindReservation(reservation, localPrepared, true);
            throw lifecycle(
                    validationFailure.getReason(),
                    true,
                    localPrepared,
                    true,
                    validationFailure.getMessage(),
                    validationFailure);
        }
        if (active.getStatus().isTerminal()) {
            RemoteTaskRunRegistration terminal;
            try {
                terminal = applyConfirmed(expected.scope(), active);
            } catch (RemoteTaskRunLifecycleException publicationFailure) {
                if (publicationFailure.getRecoveryBinding().isPresent()) {
                    throw publicationFailure;
                }
                throw lifecycle(
                        publicationFailure.getReason(),
                        publicationFailure.isRetryable(),
                        localPrepared,
                        false,
                        publicationFailure.getMessage(),
                        publicationFailure);
            }
            throw lifecycle(
                    RemoteTaskRunLifecycleException.Reason.INVALID_REMOTE_STATE,
                    false,
                    terminal,
                    false,
                    "ACTIVATE converged to terminal",
                    null);
        }
        if (active.getStatus() == RemoteTaskRunWireStatus.PREPARED) {
            throw lifecycle(
                    RemoteTaskRunLifecycleException.Reason.REMOTE_STATE_UNCERTAIN,
                    true,
                    localPrepared,
                    false,
                    "ACTIVATE is not confirmed; retry the same run/revision after STATUS",
                    null);
        }
        if (active.getStatus() != RemoteTaskRunWireStatus.ACTIVE) {
            bindReservation(reservation, localPrepared, true);
            throw lifecycle(
                    RemoteTaskRunLifecycleException.Reason.INVALID_REMOTE_STATE,
                    true,
                    localPrepared,
                    true,
                    "ACTIVATE converged to " + active.getStatus(),
                    null);
        }
        if (active.getRunRevision() <= prepared.getRunRevision()) {
            bindReservation(reservation, localPrepared, true);
            throw lifecycle(
                    RemoteTaskRunLifecycleException.Reason.BINDING_MISMATCH,
                    true,
                    localPrepared,
                    true,
                    "ACTIVATE confirmation did not advance runRevision",
                    null);
        }
        if (!pollerReady(expected.scope())) {
            throw failForPollerNotReady(expected.scope(), active, localPrepared);
        }
        RemoteTaskRunRegistration confirmed;
        try {
            confirmed = applyConfirmed(expected.scope(), active);
        } catch (RemoteTaskRunLifecycleException publicationFailure) {
            if (publicationFailure.getRecoveryBinding().isPresent()) {
                throw publicationFailure;
            }
            throw lifecycle(
                    publicationFailure.getReason(),
                    publicationFailure.isRetryable(),
                    localPrepared,
                    false,
                    publicationFailure.getMessage(),
                    publicationFailure);
        }
        bindReservation(reservation, confirmed, false);
        return confirmed;
    }

    private RemoteTaskRunRegistration convergeExistingActive(
            ExpectedBinding expected,
            RemoteTaskRunBinding active,
            StartReservation reservation) {
        RemoteTaskRunRegistration local = registry.find(active.getTaskRunId()).orElseThrow(() -> lifecycle(
                RemoteTaskRunLifecycleException.Reason.LOCAL_REGISTRATION_MISSING,
                false,
                "remote ACTIVE has no existing complete local registration",
                null));
        try {
            validateRegistrationExact(local, ExpectedBinding.from(active));
        } catch (RemoteTaskRunLifecycleException validationFailure) {
            markReservationCleanupPending(reservation);
            RemoteTaskRunRegistration retained = reservationRecoveryBinding(reservation);
            throw lifecycle(
                    validationFailure.getReason(),
                    true,
                    retained,
                    true,
                    "remote ACTIVE conflicts with the local identity; retained the last exact recovery",
                    validationFailure);
        }
        try {
            if (local.getStatus() != RemoteTaskRunStatus.PREPARED
                    && local.getStatus() != RemoteTaskRunStatus.ACTIVE) {
                throw invalidLocalState("remote ACTIVE cannot revive local " + local.getStatus(), false);
            }
            requireRegistrationProgress(
                    active,
                    local,
                    local.getStatus() == RemoteTaskRunStatus.PREPARED);
        } catch (RemoteTaskRunLifecycleException validationFailure) {
            bindReservation(reservation, local, true);
            throw lifecycle(
                    validationFailure.getReason(),
                    true,
                    local,
                    true,
                    validationFailure.getMessage(),
                    validationFailure);
        }
        if (!pollerReady(expected.scope())) {
            throw failForPollerNotReady(expected.scope(), active, local);
        }
        try {
            return applyConfirmed(expected.scope(), active);
        } catch (RemoteTaskRunLifecycleException publicationFailure) {
            if (publicationFailure.getRecoveryBinding().isPresent()) {
                throw publicationFailure;
            }
            bindReservation(reservation, local, true);
            throw lifecycle(
                    publicationFailure.getReason(),
                    true,
                    local,
                    true,
                    publicationFailure.getMessage(),
                    publicationFailure);
        } catch (RuntimeException publicationFailure) {
            bindReservation(reservation, local, true);
            throw lifecycle(
                    RemoteTaskRunLifecycleException.Reason.REMOTE_STATE_UNCERTAIN,
                    true,
                    local,
                    true,
                    "remote ACTIVE could not be published over the trusted local registration",
                    publicationFailure);
        }
    }

    private RemoteTaskRunLifecycleException failForPollerNotReady(
            RemoteTaskRunScope scope,
            RemoteTaskRunBinding remote,
            RemoteTaskRunRegistration local) {
        RemoteTaskRunRegistration stopping = beginStopPublication(
                scope, local, "poller cleanup STOP");
        bindExistingReservation(stopping, !stopping.getStatus().isTerminal());
        if (stopping.getStatus().isTerminal()) {
            return lifecycle(
                    RemoteTaskRunLifecycleException.Reason.POLLER_NOT_READY,
                    false,
                    stopping,
                    false,
                    "remote poller is not ready; cleanup was already terminal",
                    null);
        }

        try {
            validateExpected(remote, ExpectedBinding.from(stopping));
            requireStatusProgress(remote, stopping);
        } catch (RemoteTaskRunLifecycleException validationFailure) {
            throw lifecycle(
                    validationFailure.getReason(),
                    validationFailure.isRetryable(),
                    stopping,
                    true,
                    validationFailure.getMessage(),
                    validationFailure);
        }
        RemoteTaskRunRegistration beforeAdvance = stopping;
        stopping = retainStoppingProgress(scope, toRegistration(remote), beforeAdvance);
        bindExistingReservation(stopping, true);
        RemoteTaskRunRegistration canonicalStopping = stopping;

        RemoteTaskRunBinding stopped;
        try {
            stopped = apiClient.stop(
                    scope, canonicalStopping.getTaskRunId(), canonicalStopping.getRunRevision());
        } catch (RemoteTaskRunClientException e) {
            if (!shouldConverge(e)) {
                throw lifecycle(
                        RemoteTaskRunLifecycleException.Reason.INVALID_REMOTE_STATE,
                        false,
                        canonicalStopping,
                        true,
                        "poller cleanup STOP was rejected; local run remains STOPPING",
                        e);
            }
            try {
                stopped = statusAfterUncertain(
                        scope, canonicalStopping, e, "STOP", true);
            } catch (RemoteTaskRunLifecycleException convergenceFailure) {
                if (convergenceFailure.getReason()
                        != RemoteTaskRunLifecycleException.Reason.REMOTE_STATE_UNCERTAIN) {
                    throw convergenceFailure;
                }
                RemoteTaskRunRegistration recovery = convergenceFailure.getRecoveryBinding()
                        .orElse(canonicalStopping);
                bindExistingReservation(recovery, true);
                return lifecycle(
                        RemoteTaskRunLifecycleException.Reason.POLLER_NOT_READY,
                        true,
                        recovery,
                        true,
                        "remote poller is not ready and cleanup transport did not converge",
                        convergenceFailure);
            }
        } catch (RuntimeException e) {
            throw lifecycle(
                    RemoteTaskRunLifecycleException.Reason.REMOTE_STATE_UNCERTAIN,
                    true,
                    canonicalStopping,
                    true,
                    "poller cleanup STOP failed without a typed remote outcome; local run remains STOPPING",
                    e);
        }

        try {
            validateExpected(stopped, ExpectedBinding.from(canonicalStopping));
            requireStatusProgress(stopped, canonicalStopping);
        } catch (RemoteTaskRunLifecycleException validationFailure) {
            throw lifecycle(
                    validationFailure.getReason(),
                    validationFailure.isRetryable(),
                    canonicalStopping,
                    true,
                    validationFailure.getMessage(),
                    validationFailure);
        }
        if (stopped.getStatus().isTerminal()) {
            RemoteTaskRunRegistration terminal = applyConfirmed(scope, stopped);
            bindExistingReservation(terminal, false);
            return lifecycle(
                    RemoteTaskRunLifecycleException.Reason.POLLER_NOT_READY,
                    false,
                    terminal,
                    false,
                    "remote poller is not ready; cleanup reached terminal",
                    null);
        }

        RemoteTaskRunRegistration recovery = retainStoppingProgress(
                scope, toRegistration(stopped), canonicalStopping);
        bindExistingReservation(recovery, true);
        return lifecycle(
                RemoteTaskRunLifecycleException.Reason.POLLER_NOT_READY,
                true,
                recovery,
                true,
                "remote poller is not ready; cleanup remains non-terminal",
                null);
    }

    private RemoteTaskRunLifecycleException failWithoutLocalRegistration(
            ExpectedBinding expected,
            RemoteTaskRunBinding observed,
            RuntimeException registrationFailure,
            StartReservation reservation) {
        RemoteTaskRunRegistration observedRegistration = toRegistration(observed);
        bindReservation(reservation, observedRegistration, true);
        RemoteTaskRunBinding outcome;
        try {
            outcome = apiClient.stop(
                    expected.scope(),
                    observed.getTaskRunId(),
                    observed.getRunRevision());
        } catch (RemoteTaskRunClientException stopFailure) {
            registrationFailure.addSuppressed(stopFailure);
            if (!stopFailure.isOutcomeUncertain()) {
                return lifecycle(
                        RemoteTaskRunLifecycleException.Reason.INVALID_REMOTE_STATE,
                        false,
                        observedRegistration,
                        true,
                        "local registration is unavailable and cleanup STOP was rejected taskRunId="
                                + observed.getTaskRunId(),
                        registrationFailure);
            }
            if (stopFailure.getFailureType()
                    == RemoteTaskRunClientException.FailureType.INTERRUPTED) {
                return lifecycle(
                        RemoteTaskRunLifecycleException.Reason.REMOTE_STATE_UNCERTAIN,
                        true,
                        observedRegistration,
                        true,
                        "cleanup STOP was interrupted; re-enter the same startRequestId taskRunId="
                                + observed.getTaskRunId(),
                        registrationFailure);
            }
            try {
                outcome = apiClient.status(expected.scope(), observed.getTaskRunId());
            } catch (RemoteTaskRunClientException statusFailure) {
                statusFailure.addSuppressed(stopFailure);
                registrationFailure.addSuppressed(statusFailure);
                boolean uncertain = statusFailure.isOutcomeUncertain();
                return lifecycle(
                        uncertain
                                ? RemoteTaskRunLifecycleException.Reason.REMOTE_STATE_UNCERTAIN
                                : RemoteTaskRunLifecycleException.Reason.INVALID_REMOTE_STATE,
                        uncertain,
                        observedRegistration,
                        true,
                        uncertain
                                ? "cleanup STOP is uncertain and STATUS did not converge taskRunId="
                                + observed.getTaskRunId()
                                : "cleanup STATUS was rejected deterministically taskRunId="
                                + observed.getTaskRunId(),
                        registrationFailure);
            } catch (RuntimeException statusFailure) {
                statusFailure.addSuppressed(stopFailure);
                registrationFailure.addSuppressed(statusFailure);
                return lifecycle(
                        RemoteTaskRunLifecycleException.Reason.REMOTE_STATE_UNCERTAIN,
                        true,
                        observedRegistration,
                        true,
                        "cleanup STOP is uncertain and STATUS did not converge taskRunId="
                                + observed.getTaskRunId(),
                        registrationFailure);
            }
        } catch (RuntimeException stopFailure) {
            registrationFailure.addSuppressed(stopFailure);
            return lifecycle(
                    RemoteTaskRunLifecycleException.Reason.REMOTE_STATE_UNCERTAIN,
                    true,
                    observedRegistration,
                    true,
                    "cleanup STOP failed without a typed remote outcome taskRunId="
                            + observed.getTaskRunId(),
                    registrationFailure);
        }

        try {
            validateExpected(outcome, expected);
            requireMonotonic(outcome, observed, false);
            if (!outcome.getStatus().isTerminal()) {
                RemoteTaskRunRegistration recovery = toRegistration(outcome);
                bindReservation(reservation, recovery, true);
                return lifecycle(
                        RemoteTaskRunLifecycleException.Reason.REMOTE_STATE_UNCERTAIN,
                        true,
                        recovery,
                        true,
                        "cleanup remains non-terminal; re-enter the same startRequestId taskRunId="
                                + observed.getTaskRunId(),
                        registrationFailure);
            }
            requireTerminalStopProgress(
                    outcome, observed.getRunRevision(), observed.getStopEpoch());
            RemoteTaskRunRegistration terminalSnapshot = toRegistration(outcome);
            RemoteTaskRunRegistration terminal = publishTerminalWithoutLocalRegistration(
                    reservation, terminalSnapshot, observedRegistration);
            return lifecycle(
                    RemoteTaskRunLifecycleException.Reason.INVALID_REMOTE_STATE,
                    false,
                    terminal,
                    false,
                    "local registration failed; cloud cleanup reached " + terminal.getStatus()
                            + " taskRunId=" + terminal.getTaskRunId(),
                    registrationFailure);
        } catch (RemoteTaskRunLifecycleException cleanupFailure) {
            if (cleanupFailure.getRecoveryBinding().isPresent()) {
                cleanupFailure.addSuppressed(registrationFailure);
                return cleanupFailure;
            }
            registrationFailure.addSuppressed(cleanupFailure);
            bindReservation(reservation, observedRegistration, true);
            return lifecycle(
                    cleanupFailure.getReason(),
                    true,
                    observedRegistration,
                    true,
                    "local registration failed and cleanup response was rejected taskRunId="
                            + observed.getTaskRunId(),
                    registrationFailure);
        } catch (RuntimeException cleanupFailure) {
            registrationFailure.addSuppressed(cleanupFailure);
            bindReservation(reservation, observedRegistration, true);
            return lifecycle(
                    RemoteTaskRunLifecycleException.Reason.REMOTE_STATE_UNCERTAIN,
                    true,
                    observedRegistration,
                    true,
                    "local registration failed and terminal cleanup could not publish taskRunId="
                            + observed.getTaskRunId(),
                    registrationFailure);
        }
    }

    private RemoteTaskRunBinding statusAfterUncertain(
            RemoteTaskRunScope scope,
            RemoteTaskRunRegistration expected,
            RemoteTaskRunClientException original,
            String action,
            boolean cleanupPending) {
        if (original.getFailureType() == RemoteTaskRunClientException.FailureType.INTERRUPTED) {
            throw lifecycle(
                    RemoteTaskRunLifecycleException.Reason.REMOTE_STATE_UNCERTAIN,
                    true,
                    expected,
                    cleanupPending,
                    action + " was interrupted; STATUS convergence is still required",
                    original);
        }
        RemoteTaskRunBinding status;
        try {
            status = apiClient.status(scope, expected.getTaskRunId());
        } catch (RemoteTaskRunClientException statusFailure) {
            statusFailure.addSuppressed(original);
            boolean uncertain = statusFailure.isOutcomeUncertain();
            throw lifecycle(
                    uncertain
                            ? RemoteTaskRunLifecycleException.Reason.REMOTE_STATE_UNCERTAIN
                            : RemoteTaskRunLifecycleException.Reason.INVALID_REMOTE_STATE,
                    uncertain,
                    expected,
                    cleanupPending,
                    action + (uncertain
                            ? " result is uncertain and STATUS did not converge"
                            : " STATUS was rejected deterministically"),
                    statusFailure);
        } catch (RuntimeException statusFailure) {
            statusFailure.addSuppressed(original);
            throw lifecycle(
                    RemoteTaskRunLifecycleException.Reason.REMOTE_STATE_UNCERTAIN,
                    true,
                    expected,
                    cleanupPending,
                    action + " result is uncertain and STATUS did not converge",
                    statusFailure);
        }
        return status;
    }

    private static boolean shouldConverge(RemoteTaskRunClientException exception) {
        return exception.isOutcomeUncertain()
                || (exception.getFailureType()
                == RemoteTaskRunClientException.FailureType.REMOTE_REJECTED
                && "CONFLICT".equals(exception.getRemoteErrorCode()));
    }

    private StartReservation acquireReservation(
            ExpectedBinding expected,
            boolean createIfMissing) {
        StartKey key = StartKey.from(expected);
        while (true) {
            StartReservation reservation;
            synchronized (reservationMonitor) {
                reservation = startReservations.get(key);
                if (reservation == null) {
                    if (createIfMissing) {
                        if (startReservations.size() >= globalStartReservationCapacity) {
                            throw lifecycle(
                                    RemoteTaskRunLifecycleException.Reason.CAPACITY_EXCEEDED,
                                    false,
                                    "global start reservation capacity exceeded",
                                    null);
                        }
                        OwnerKey owner = key.owner();
                        if (startReservationOwnerUsage.getOrDefault(owner, 0)
                                >= ownerStartReservationCapacity) {
                            throw lifecycle(
                                    RemoteTaskRunLifecycleException.Reason.CAPACITY_EXCEEDED,
                                    false,
                                    "owner start reservation capacity exceeded",
                                    null);
                        }
                        reservation = new StartReservation(key, expected);
                        startReservations.put(key, reservation);
                        startReservationOwnerUsage.put(
                                owner,
                                startReservationOwnerUsage.getOrDefault(owner, 0) + 1);
                    }
                } else {
                    requireReservationExact(reservation, expected);
                    if (!createIfMissing
                            && (expected.taskRunId() == null
                            || !expected.taskRunId().equals(reservation.taskRunId))) {
                        throw lifecycle(
                                RemoteTaskRunLifecycleException.Reason.BINDING_MISMATCH,
                                false,
                                "lifecycle operation requires an existing exact taskRunId reservation",
                                null);
                    }
                }
            }

            if (reservation == null) {
                RemoteTaskRunRegistration retained = expected.taskRunId() == null
                        ? null
                        : registry.find(expected.taskRunId()).orElse(null);
                if (retained != null) {
                    validateRegistrationExact(retained, expected);
                }
                throw lifecycle(
                        RemoteTaskRunLifecycleException.Reason.LOCAL_REGISTRATION_MISSING,
                        false,
                        retained,
                        retained != null && retained.getStatus() == RemoteTaskRunStatus.STOPPING,
                        "exact start reservation is not retained and cannot be recreated",
                        null);
            }

            reservation.operationLock.lock();
            boolean current;
            try {
                synchronized (reservationMonitor) {
                    current = startReservations.get(key) == reservation && !reservation.released;
                    if (current) {
                        requireReservationExact(reservation, expected);
                        if (!createIfMissing
                                && (expected.taskRunId() == null
                                || !expected.taskRunId().equals(reservation.taskRunId))) {
                            throw lifecycle(
                                    RemoteTaskRunLifecycleException.Reason.BINDING_MISMATCH,
                                    false,
                                    "lifecycle operation lost its exact taskRunId reservation",
                                    null);
                        }
                    }
                }
            } catch (RuntimeException e) {
                reservation.operationLock.unlock();
                throw e;
            }
            if (current) {
                return reservation;
            }
            reservation.operationLock.unlock();
            if (!createIfMissing) {
                RemoteTaskRunRegistration retained = expected.taskRunId() == null
                        ? null
                        : registry.find(expected.taskRunId()).orElse(null);
                if (retained != null) {
                    validateRegistrationExact(retained, expected);
                }
                throw lifecycle(
                        RemoteTaskRunLifecycleException.Reason.LOCAL_REGISTRATION_MISSING,
                        false,
                        retained,
                        retained != null && retained.getStatus() == RemoteTaskRunStatus.STOPPING,
                        "exact start reservation was consumed before lifecycle dispatch",
                        null);
            }
        }
    }

    private void bindReservation(
            StartReservation reservation,
            RemoteTaskRunRegistration binding,
            boolean cleanupPending) {
        synchronized (reservationMonitor) {
            if (startReservations.get(reservation.key) != reservation || reservation.released) {
                throw lifecycle(
                        RemoteTaskRunLifecycleException.Reason.BINDING_MISMATCH,
                        false,
                        binding,
                        cleanupPending,
                        "start reservation disappeared before binding update",
                        null);
            }
            bindReservationLocked(reservation, binding, cleanupPending, true);
        }
    }

    private void bindPreparedReservation(
            StartReservation reservation,
            RemoteTaskRunRegistration prepared) {
        synchronized (reservationMonitor) {
            if (startReservations.get(reservation.key) != reservation || reservation.released) {
                throw lifecycle(
                        RemoteTaskRunLifecycleException.Reason.BINDING_MISMATCH,
                        false,
                        prepared,
                        false,
                        "start reservation disappeared before PREPARED binding",
                        null);
            }
            bindReservationLocked(reservation, prepared, false, false);
        }
    }

    private boolean bindExistingReservation(
            RemoteTaskRunRegistration binding,
            boolean cleanupPending) {
        StartKey key = StartKey.from(binding);
        synchronized (reservationMonitor) {
            StartReservation reservation = startReservations.get(key);
            if (reservation != null && !reservation.released) {
                bindReservationLocked(reservation, binding, cleanupPending, true);
                return true;
            }
            return false;
        }
    }

    private void bindReservationLocked(
            StartReservation reservation,
            RemoteTaskRunRegistration binding,
            boolean cleanupPending,
            boolean replaceRecovery) {
        ExpectedBinding expected = ExpectedBinding.from(binding);
        requireReservationExact(reservation, expected);
        if (reservation.taskRunId != null
                && !reservation.taskRunId.equals(binding.getTaskRunId())) {
            throw lifecycle(
                    RemoteTaskRunLifecycleException.Reason.BINDING_MISMATCH,
                    false,
                    binding,
                    cleanupPending,
                    "start reservation is already bound to another taskRunId",
                    null);
        }
        reservation.taskRunId = binding.getTaskRunId();
        if (replaceRecovery || reservation.recoveryBinding == null) {
            reservation.recoveryBinding = binding;
            reservation.cleanupPending = cleanupPending;
        }
    }

    private void markReservationCleanupPending(StartReservation reservation) {
        synchronized (reservationMonitor) {
            if (startReservations.get(reservation.key) == reservation && !reservation.released) {
                reservation.cleanupPending = true;
            }
        }
    }

    private boolean reservationCleanupPending(StartReservation reservation) {
        synchronized (reservationMonitor) {
            if (startReservations.get(reservation.key) != reservation || reservation.released) {
                throw lifecycle(
                        RemoteTaskRunLifecycleException.Reason.BINDING_MISMATCH,
                        false,
                        "start reservation disappeared during lifecycle operation",
                        null);
            }
            return reservation.cleanupPending;
        }
    }

    private RemoteTaskRunRegistration reservationRecoveryBinding(StartReservation reservation) {
        synchronized (reservationMonitor) {
            return startReservations.get(reservation.key) == reservation && !reservation.released
                    ? reservation.recoveryBinding
                    : null;
        }
    }

    private void releaseUnboundStartReservation(StartReservation reservation) {
        synchronized (reservationMonitor) {
            if (startReservations.get(reservation.key) != reservation
                    || reservation.released
                    || reservation.taskRunId != null) {
                return;
            }
            startReservations.remove(reservation.key, reservation);
            decrementStartReservationUsage(reservation.key.owner());
            reservation.released = true;
        }
    }

    private void decrementStartReservationUsage(OwnerKey owner) {
        int remaining = startReservationOwnerUsage.getOrDefault(owner, 0) - 1;
        if (remaining < 0) {
            throw new IllegalStateException("start reservation owner usage underflow");
        }
        if (remaining == 0) {
            startReservationOwnerUsage.remove(owner);
        } else {
            startReservationOwnerUsage.put(owner, remaining);
        }
    }

    private void requireReservationExact(
            StartReservation reservation,
            ExpectedBinding expected) {
        if (!reservation.key.tenantId().equals(expected.scope().getTenantId())
                || !reservation.key.userId().equals(expected.scope().getUserId())
                || !reservation.key.deviceId().equals(expected.scope().getDeviceId())
                || !reservation.key.startRequestId().equals(expected.startRequestId())
                || !reservation.clientSessionId.equals(expected.scope().getClientSessionId())
                || !reservation.taskType.equals(expected.taskType())
                || !sameWindow(reservation.window, expected.window())
                || (expected.taskRunId() != null
                && reservation.taskRunId != null
                && !reservation.taskRunId.equals(expected.taskRunId()))) {
            throw lifecycle(
                    RemoteTaskRunLifecycleException.Reason.BINDING_MISMATCH,
                    false,
                    "startRequestId reservation conflicts with owner, startRequestId, session, taskType, "
                            + "window, or taskRunId",
                    null);
        }
    }

    private RemoteTaskRunRegistration requireLocal(RemoteTaskRunScope scope, String taskRunId) {
        RemoteTaskRunScope requiredScope = requireScope(scope);
        String requiredTaskRunId = requireText(taskRunId, "taskRunId");
        RemoteTaskRunRegistration registration = registry.find(requiredTaskRunId)
                .orElseThrow(() -> lifecycle(
                        RemoteTaskRunLifecycleException.Reason.LOCAL_REGISTRATION_MISSING,
                        false,
                        "local remote task run is not registered",
                        null));
        if (!registration.getTenantId().equals(requiredScope.getTenantId())
                || !registration.getUserId().equals(requiredScope.getUserId())
                || !registration.getDeviceId().equals(requiredScope.getDeviceId())
                || !registration.getClientSessionId().equals(requiredScope.getClientSessionId())
                || !registration.getTaskRunId().equals(requiredTaskRunId)) {
            throw lifecycle(
                    RemoteTaskRunLifecycleException.Reason.BINDING_MISMATCH,
                    false,
                    "local task run does not belong to the supplied scope",
                    null);
        }
        return registration;
    }

    private RemoteTaskRunRegistration applyConfirmed(
            RemoteTaskRunScope scope,
            RemoteTaskRunBinding binding) {
        RemoteTaskRunRegistration local = requireLocal(scope, binding.getTaskRunId());
        if (binding.getStatus() == RemoteTaskRunWireStatus.ACTIVE) {
            if (!pollerReady(scope)) {
                throw failForPollerNotReady(scope, binding, local);
            }
        }
        try {
            RemoteTaskRunRegistration confirmed = registry.applyConfirmedBinding(
                            clientSession(scope),
                            toRegistration(binding))
                    .orElse(null);
            if (confirmed == null) {
                bindExistingReservation(local, true);
                throw lifecycle(
                        RemoteTaskRunLifecycleException.Reason.LOCAL_REGISTRATION_MISSING,
                        true,
                        local,
                        true,
                        "local task run disappeared before confirmed state publication",
                        null);
            }
            if (!bindExistingReservation(confirmed, false)) {
                throw lifecycle(
                        RemoteTaskRunLifecycleException.Reason.BINDING_MISMATCH,
                        true,
                        confirmed,
                        true,
                        "confirmed binding was published without its retained start reservation",
                        null);
            }
            return confirmed;
        } catch (RemoteTaskRunLifecycleException publicationFailure) {
            if (publicationFailure.getRecoveryBinding().isPresent()) {
                throw publicationFailure;
            }
            boolean cleanupPending = !local.getStatus().isTerminal();
            throw lifecycle(
                    publicationFailure.getReason(),
                    publicationFailure.isRetryable(),
                    local,
                    cleanupPending,
                    publicationFailure.getMessage(),
                    publicationFailure);
        } catch (RuntimeException publicationFailure) {
            Optional<RemoteTaskRunRegistration> latest = latestExactRegistration(
                    local, publicationFailure);
            RemoteTaskRunRegistration recovery = latest.orElse(local);
            boolean cleanupPending = latest.isEmpty() || !recovery.getStatus().isTerminal();
            boolean reservationSynchronized = bindExistingReservation(recovery, cleanupPending);
            if (!reservationSynchronized) {
                cleanupPending = true;
            }
            throw lifecycle(
                    RemoteTaskRunLifecycleException.Reason.REMOTE_STATE_UNCERTAIN,
                    cleanupPending,
                    recovery,
                    cleanupPending,
                    "confirmed binding publication raced with local lifecycle state",
                    publicationFailure);
        }
    }

    private RemoteTaskRunRegistration beginStopPublication(
            RemoteTaskRunScope scope,
            RemoteTaskRunRegistration trusted,
            String operation) {
        try {
            RemoteTaskRunRegistration stopping = registry.beginStop(
                            clientSession(scope),
                            trusted.getTaskRunId(),
                            trusted.getWindowId(),
                            trusted.getStopEpoch())
                    .orElse(null);
            if (stopping == null) {
                bindExistingReservation(trusted, true);
                throw lifecycle(
                        RemoteTaskRunLifecycleException.Reason.LOCAL_REGISTRATION_MISSING,
                        true,
                        trusted,
                        true,
                        "local run disappeared before " + operation,
                        null);
            }
            if (stopping.getStatus().isTerminal()) {
                boolean synchronizedReservation = bindExistingReservation(stopping, false);
                boolean cleanupPending = !synchronizedReservation;
                throw lifecycle(
                        RemoteTaskRunLifecycleException.Reason.INVALID_REMOTE_STATE,
                        cleanupPending,
                        stopping,
                        cleanupPending,
                        operation + " raced with an already terminal local run",
                        null);
            }
            if (!bindExistingReservation(stopping, true)) {
                throw lifecycle(
                        RemoteTaskRunLifecycleException.Reason.BINDING_MISMATCH,
                        true,
                        stopping,
                        true,
                        operation + " published STOPPING without its retained start reservation",
                        null);
            }
            return stopping;
        } catch (RemoteTaskRunLifecycleException publicationFailure) {
            throw publicationFailure;
        } catch (RuntimeException publicationFailure) {
            Optional<RemoteTaskRunRegistration> latest = latestExactRegistration(
                    trusted, publicationFailure);
            RemoteTaskRunRegistration recovery = latest.orElse(trusted);
            boolean cleanupPending = latest.isEmpty() || !recovery.getStatus().isTerminal();
            boolean reservationSynchronized = bindExistingReservation(recovery, cleanupPending);
            if (!reservationSynchronized) {
                cleanupPending = true;
            }
            throw lifecycle(
                    RemoteTaskRunLifecycleException.Reason.REMOTE_STATE_UNCERTAIN,
                    cleanupPending,
                    recovery,
                    cleanupPending,
                    operation + " publication raced with local lifecycle state",
                    publicationFailure);
        }
    }

    private void requestPausePublication(
            RemoteTaskRunScope scope,
            RemoteTaskRunRegistration trustedActive,
            boolean existingCleanupPending,
            String reason) {
        try {
            registry.requestPause(
                    clientSession(scope),
                    trustedActive.getTaskRunId(),
                    trustedActive.getWindowId(),
                    reason);
        } catch (RuntimeException publicationFailure) {
            Optional<RemoteTaskRunRegistration> latest = latestExactRegistration(
                    trustedActive, publicationFailure);
            RemoteTaskRunRegistration recovery = latest.orElse(trustedActive);
            boolean cleanupPending = latest.isEmpty()
                    || recovery.getStatus() == RemoteTaskRunStatus.STOPPING
                    || (!recovery.getStatus().isTerminal() && existingCleanupPending);
            boolean reservationSynchronized = bindExistingReservation(recovery, cleanupPending);
            if (!reservationSynchronized) {
                cleanupPending = true;
            }
            throw lifecycle(
                    RemoteTaskRunLifecycleException.Reason.REMOTE_STATE_UNCERTAIN,
                    !recovery.getStatus().isTerminal() || cleanupPending,
                    recovery,
                    cleanupPending,
                    "PAUSE token publication raced with local lifecycle state",
                    publicationFailure);
        }
    }

    private RemoteTaskRunRegistration releaseTerminalPublication(
            RemoteTaskRunScope scope,
            RemoteTaskRunRegistration expectedTerminal,
            StartReservation reservation) {
        try {
            return registry.releaseTerminal(clientSession(scope), expectedTerminal)
                    .orElse(expectedTerminal);
        } catch (RuntimeException publicationFailure) {
            RemoteTaskRunRegistration latest = registry.find(expectedTerminal.getTaskRunId()).orElse(null);
            if (latest == null) {
                return expectedTerminal;
            }
            try {
                validateRegistrationExact(latest, ExpectedBinding.from(expectedTerminal));
            } catch (RemoteTaskRunLifecycleException identityFailure) {
                publicationFailure.addSuppressed(identityFailure);
                bindReservation(reservation, expectedTerminal, true);
                throw lifecycle(
                        RemoteTaskRunLifecycleException.Reason.BINDING_MISMATCH,
                        true,
                        expectedTerminal,
                        true,
                        "terminal release raced with a different local task-run identity",
                        publicationFailure);
            }
            if (latest.equals(expectedTerminal) && latest.getStatus().isTerminal()) {
                throw lifecycle(
                        RemoteTaskRunLifecycleException.Reason.REMOTE_STATE_UNCERTAIN,
                        true,
                        latest,
                        false,
                        "terminal release raced; retry consume without another lifecycle mutation",
                        publicationFailure);
            }
            bindReservation(reservation, latest, true);
            throw lifecycle(
                    RemoteTaskRunLifecycleException.Reason.BINDING_MISMATCH,
                    true,
                    latest,
                    true,
                    "terminal release raced with a non-identical local lifecycle snapshot",
                    publicationFailure);
        }
    }

    private RemoteTaskRunRegistration publishTerminalWithoutLocalRegistration(
            StartReservation reservation,
            RemoteTaskRunRegistration terminalSnapshot,
            RemoteTaskRunRegistration trustedFallback) {
        RemoteTaskRunRegistration terminal;
        try {
            terminal = registry.register(terminalSnapshot);
        } catch (RemoteTaskRunRegistry.CapacityExceededException publicationFailure) {
            bindReservation(reservation, trustedFallback, true);
            throw lifecycle(
                    RemoteTaskRunLifecycleException.Reason.CAPACITY_EXCEEDED,
                    true,
                    trustedFallback,
                    true,
                    "cloud cleanup reached terminal but local terminal capacity is exhausted",
                    publicationFailure);
        } catch (RuntimeException publicationFailure) {
            RemoteTaskRunRegistration latest = registry.find(terminalSnapshot.getTaskRunId()).orElse(null);
            if (latest == null) {
                bindReservation(reservation, trustedFallback, true);
                throw lifecycle(
                        RemoteTaskRunLifecycleException.Reason.REMOTE_STATE_UNCERTAIN,
                        true,
                        trustedFallback,
                        true,
                        "cloud cleanup reached terminal but no local publication survived the race",
                        publicationFailure);
            }
            try {
                validateRegistrationExact(latest, ExpectedBinding.from(terminalSnapshot));
            } catch (RemoteTaskRunLifecycleException identityFailure) {
                publicationFailure.addSuppressed(identityFailure);
                bindReservation(reservation, trustedFallback, true);
                throw lifecycle(
                        RemoteTaskRunLifecycleException.Reason.BINDING_MISMATCH,
                        true,
                        trustedFallback,
                        true,
                        "terminal publication raced with a different local task-run identity",
                        publicationFailure);
            }
            if (latest.getStatus().isTerminal() && latest.equals(terminalSnapshot)) {
                bindReservation(reservation, latest, false);
                return latest;
            }
            bindReservation(reservation, latest, true);
            throw lifecycle(
                    RemoteTaskRunLifecycleException.Reason.BINDING_MISMATCH,
                    true,
                    latest,
                    true,
                    "terminal publication raced with a non-identical local lifecycle snapshot",
                    publicationFailure);
        }
        bindReservation(reservation, terminal, false);
        return terminal;
    }

    private RemoteTaskRunRegistration retainStoppingProgress(
            RemoteTaskRunScope scope,
            RemoteTaskRunRegistration observed,
            RemoteTaskRunRegistration trustedStopping) {
        try {
            RemoteTaskRunRegistration retained = registry.advanceStoppingProgress(clientSession(scope), observed)
                    .orElse(null);
            if (retained == null) {
                bindExistingReservation(trustedStopping, true);
                throw lifecycle(
                        RemoteTaskRunLifecycleException.Reason.LOCAL_REGISTRATION_MISSING,
                        true,
                        trustedStopping,
                        true,
                        "local STOPPING run disappeared before progress publication",
                        null);
            }
            if (!bindExistingReservation(retained, true)) {
                throw lifecycle(
                        RemoteTaskRunLifecycleException.Reason.BINDING_MISMATCH,
                        true,
                        retained,
                        true,
                        "STOPPING progress was published without its retained start reservation",
                        null);
            }
            return retained;
        } catch (RemoteTaskRunLifecycleException publicationFailure) {
            throw publicationFailure;
        } catch (RuntimeException publicationFailure) {
            Optional<RemoteTaskRunRegistration> latest = latestExactRegistration(
                    trustedStopping, publicationFailure);
            RemoteTaskRunRegistration recovery = latest.orElse(trustedStopping);
            boolean cleanupPending = latest.isEmpty() || !recovery.getStatus().isTerminal();
            boolean reservationSynchronized = bindExistingReservation(recovery, cleanupPending);
            if (!reservationSynchronized) {
                cleanupPending = true;
            }
            throw lifecycle(
                    RemoteTaskRunLifecycleException.Reason.REMOTE_STATE_UNCERTAIN,
                    cleanupPending,
                    recovery,
                    cleanupPending,
                    "STOPPING progress publication raced with local lifecycle state",
                    publicationFailure);
        }
    }

    private Optional<RemoteTaskRunRegistration> latestExactRegistration(
            RemoteTaskRunRegistration fallback,
            RuntimeException publicationFailure) {
        RemoteTaskRunRegistration latest = registry.find(fallback.getTaskRunId()).orElse(null);
        if (latest == null) {
            return Optional.empty();
        }
        try {
            validateRegistrationExact(latest, ExpectedBinding.from(fallback));
            return Optional.of(latest);
        } catch (RemoteTaskRunLifecycleException validationFailure) {
            publicationFailure.addSuppressed(validationFailure);
            return Optional.empty();
        }
    }

    private void validateExpected(RemoteTaskRunBinding actual, ExpectedBinding expected) {
        validateWireBinding(actual);
        if (!sameScope(actual.getScope(), expected.scope())
                || (expected.taskRunId() != null
                && !expected.taskRunId().equals(actual.getTaskRunId()))
                || !expected.startRequestId().equals(actual.getStartRequestId())
                || !expected.taskType().equals(actual.getTaskType())
                || !sameWindow(actual.getWindow(), expected.window())) {
            throw lifecycle(
                    RemoteTaskRunLifecycleException.Reason.BINDING_MISMATCH,
                    false,
                    "remote lifecycle response does not match the exact requested binding",
                    null);
        }
    }

    private void validateRegistrationExact(
            RemoteTaskRunRegistration actual,
            ExpectedBinding expected) {
        if (!actual.getTenantId().equals(expected.scope().getTenantId())
                || !actual.getUserId().equals(expected.scope().getUserId())
                || !actual.getDeviceId().equals(expected.scope().getDeviceId())
                || !actual.getClientSessionId().equals(expected.scope().getClientSessionId())
                || !actual.getTaskRunId().equals(expected.taskRunId())
                || !actual.getStartRequestId().equals(expected.startRequestId())
                || !actual.getTaskType().equals(expected.taskType())
                || !actual.getWindowId().equals(expected.window().getWindowId())
                || !actual.getNativeHandle().equals(expected.window().getNativeHandle())
                || actual.getProcessId() != expected.window().getProcessId()
                || actual.getPlayerIdentityEpoch() != expected.window().getPlayerIdentityEpoch()) {
            throw lifecycle(
                    RemoteTaskRunLifecycleException.Reason.BINDING_MISMATCH,
                    false,
                    "existing local registration is not the same complete binding",
                    null);
        }
    }

    private void validateWireBinding(RemoteTaskRunBinding binding) {
        if (binding == null
                || binding.getScope() == null
                || binding.getWindow() == null
                || binding.getStatus() == null) {
            throw lifecycle(
                    RemoteTaskRunLifecycleException.Reason.BINDING_MISMATCH,
                    false,
                    "remote lifecycle binding is incomplete",
                    null);
        }
        requireScope(binding.getScope());
        requireText(binding.getTaskRunId(), "binding.taskRunId");
        requireText(binding.getStartRequestId(), "binding.startRequestId");
        requireOriginalText(binding.getTaskType(), "binding.taskType");
        requireWindow(binding.getWindow());
        if (binding.getStopEpoch() < 0L || binding.getRunRevision() < 0L) {
            throw lifecycle(
                    RemoteTaskRunLifecycleException.Reason.BINDING_MISMATCH,
                    false,
                    "remote binding epoch/revision must be non-negative",
                    null);
        }
    }

    private void requireMonotonic(
            RemoteTaskRunBinding actual,
            RemoteTaskRunBinding previous,
            boolean revisionMustAdvance) {
        RemoteTaskRunStatus previousStatus = toLocalStatus(previous.getStatus());
        RemoteTaskRunStatus actualStatus = toLocalStatus(actual.getStatus());
        if (!RemoteTaskRunRegistry.transitionAllowed(previousStatus, actualStatus)) {
            throw lifecycle(
                    RemoteTaskRunLifecycleException.Reason.INVALID_REMOTE_STATE,
                    false,
                    "invalid remote task run transition " + previousStatus + " -> " + actualStatus,
                    null);
        }
        boolean statusChanged = actualStatus != previousStatus;
        boolean invalidStopEpoch = actual.getStatus().isTerminal()
                ? actual.getStopEpoch() < previous.getStopEpoch()
                : actual.getStopEpoch() != previous.getStopEpoch();
        if (invalidStopEpoch
                || actual.getRunRevision() < previous.getRunRevision()
                || ((revisionMustAdvance || statusChanged)
                && actual.getRunRevision() == previous.getRunRevision())) {
            throw lifecycle(
                    RemoteTaskRunLifecycleException.Reason.BINDING_MISMATCH,
                    false,
                    "remote lifecycle progress changed canonical stopEpoch, moved backwards, or did not advance",
                    null);
        }
    }

    private void requireRegistrationProgress(
            RemoteTaskRunBinding actual,
            RemoteTaskRunRegistration previous,
            boolean revisionMustAdvance) {
        RemoteTaskRunStatus actualStatus = toLocalStatus(actual.getStatus());
        if (!RemoteTaskRunRegistry.transitionAllowed(previous.getStatus(), actualStatus)) {
            throw lifecycle(
                    RemoteTaskRunLifecycleException.Reason.INVALID_REMOTE_STATE,
                    false,
                    "invalid remote task run transition " + previous.getStatus() + " -> " + actualStatus,
                    null);
        }
        boolean statusChanged = actualStatus != previous.getStatus();
        if (actual.getStopEpoch() != previous.getStopEpoch()
                || actual.getRunRevision() < previous.getRunRevision()
                || ((revisionMustAdvance || statusChanged)
                && actual.getRunRevision() == previous.getRunRevision())) {
            throw lifecycle(
                    RemoteTaskRunLifecycleException.Reason.BINDING_MISMATCH,
                    false,
                    "remote lifecycle progress changed canonical stopEpoch or is older than local registration",
                    null);
        }
    }

    private void requireStatusProgress(
            RemoteTaskRunBinding actual,
            RemoteTaskRunRegistration local) {
        if (local.getStatus().isTerminal()) {
            if (!local.equals(toRegistration(actual))) {
                throw lifecycle(
                        RemoteTaskRunLifecycleException.Reason.BINDING_MISMATCH,
                        false,
                        "local terminal run only accepts an identical idempotent snapshot",
                        null);
            }
            return;
        }
        if (actual.getStatus().isTerminal()) {
            requireTerminalStopProgress(
                    actual, local.getRunRevision(), local.getStopEpoch());
            return;
        }
        if (local.getStatus() != RemoteTaskRunStatus.STOPPING) {
            requireRegistrationProgress(actual, local, false);
            return;
        }
        if (actual.getRunRevision() < local.getRunRevision()
                || actual.getStopEpoch() != local.getStopEpoch()) {
            throw lifecycle(
                    RemoteTaskRunLifecycleException.Reason.BINDING_MISMATCH,
                    false,
                    "non-terminal remote STATUS cannot lower revision or change canonical stopEpoch",
                    null);
        }
    }

    private void requireTerminalStopProgress(
            RemoteTaskRunBinding actual,
            long previousRevision,
            long canonicalStopEpoch) {
        long expectedStopEpoch = switch (actual.getStatus()) {
            case STOPPED -> increment(canonicalStopEpoch, "confirmed stopEpoch");
            case COMPLETED -> canonicalStopEpoch;
            case PREPARED, ACTIVE, PAUSED -> throw lifecycle(
                    RemoteTaskRunLifecycleException.Reason.INVALID_REMOTE_STATE,
                    false,
                    "terminal STOP validation requires STOPPED or COMPLETED",
                    null);
        };
        if (actual.getStopEpoch() != expectedStopEpoch
                || actual.getRunRevision() <= previousRevision) {
            throw lifecycle(
                    RemoteTaskRunLifecycleException.Reason.BINDING_MISMATCH,
                    false,
                    actual.getStatus() + " has unexpected stopEpoch or revision",
                    null);
        }
    }

    private RemoteTaskRunRegistration toRegistration(RemoteTaskRunBinding binding) {
        return RemoteTaskRunRegistration.builder()
                .tenantId(binding.getScope().getTenantId())
                .userId(binding.getScope().getUserId())
                .deviceId(binding.getScope().getDeviceId())
                .clientSessionId(binding.getScope().getClientSessionId())
                .taskRunId(binding.getTaskRunId())
                .startRequestId(binding.getStartRequestId())
                .taskType(binding.getTaskType())
                .windowId(binding.getWindow().getWindowId())
                .nativeHandle(binding.getWindow().getNativeHandle())
                .processId(binding.getWindow().getProcessId())
                .playerIdentityEpoch(binding.getWindow().getPlayerIdentityEpoch())
                .stopEpoch(binding.getStopEpoch())
                .runRevision(binding.getRunRevision())
                .status(toLocalStatus(binding.getStatus()))
                .build();
    }

    private boolean pollerReady(RemoteTaskRunScope scope) {
        return pollingLoop.matchesSession(scope)
                && pollingLoop.isRunning()
                && !pollingLoop.isStopRequested()
                && pollingLoop.getLastFailure() == null;
    }

    private static RemoteTaskRunStatus toLocalStatus(RemoteTaskRunWireStatus status) {
        return switch (status) {
            case PREPARED -> RemoteTaskRunStatus.PREPARED;
            case ACTIVE -> RemoteTaskRunStatus.ACTIVE;
            case PAUSED -> RemoteTaskRunStatus.PAUSED;
            case STOPPED -> RemoteTaskRunStatus.STOPPED;
            case COMPLETED -> RemoteTaskRunStatus.COMPLETED;
        };
    }

    private static RemoteClientSessionRef clientSession(RemoteTaskRunScope scope) {
        return RemoteClientSessionRef.builder()
                .tenantId(scope.getTenantId())
                .userId(scope.getUserId())
                .deviceId(scope.getDeviceId())
                .clientSessionId(scope.getClientSessionId())
                .build();
    }

    private RemoteTaskRunScope requireScope(RemoteTaskRunScope scope) {
        if (scope == null) {
            throw invalidRequest("scope is required");
        }
        requireText(scope.getTenantId(), "scope.tenantId");
        requireText(scope.getUserId(), "scope.userId");
        requireText(scope.getDeviceId(), "scope.deviceId");
        requireText(scope.getClientSessionId(), "scope.clientSessionId");
        return scope;
    }

    private RemoteTaskRunWindow requireWindow(RemoteTaskRunWindow window) {
        if (window == null) {
            throw invalidRequest("window is required");
        }
        requireText(window.getWindowId(), "window.windowId");
        String nativeHandle = requireText(window.getNativeHandle(), "window.nativeHandle");
        if (!("0".equals(nativeHandle) || nativeHandle.matches("[1-9][0-9]*"))
                || new BigInteger(nativeHandle).compareTo(MAX_UNSIGNED_LONG) > 0
                || window.getProcessId() <= 0L
                || window.getPlayerIdentityEpoch() < 0L) {
            throw invalidRequest("window nativeHandle/processId/identityEpoch are invalid");
        }
        return window;
    }

    private String requireText(String value, String field) {
        if (value == null || value.isBlank() || !value.equals(value.trim())) {
            throw invalidRequest(field + " must be non-blank and normalized");
        }
        return value;
    }

    private String requireOriginalText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw invalidRequest(field + " must not be blank");
        }
        return value;
    }

    private RemoteTaskRunLifecycleException invalidRequest(String message) {
        return lifecycle(
                RemoteTaskRunLifecycleException.Reason.INVALID_REQUEST,
                false,
                message,
                null);
    }

    private RemoteTaskRunLifecycleException invalidLocalState(String message, boolean retryable) {
        return lifecycle(
                RemoteTaskRunLifecycleException.Reason.INVALID_REMOTE_STATE,
                retryable,
                message,
                null);
    }

    private static RemoteTaskRunLifecycleException lifecycle(
            RemoteTaskRunLifecycleException.Reason reason,
            boolean retryable,
            String message,
            Throwable cause) {
        return new RemoteTaskRunLifecycleException(reason, retryable, message, cause);
    }

    private static RemoteTaskRunLifecycleException lifecycle(
            RemoteTaskRunLifecycleException.Reason reason,
            boolean retryable,
            RemoteTaskRunRegistration recoveryBinding,
            boolean cleanupPending,
            String message,
            Throwable cause) {
        return new RemoteTaskRunLifecycleException(
                reason,
                retryable || cleanupPending,
                recoveryBinding,
                cleanupPending,
                message,
                cause);
    }

    private static long increment(long value, String field) {
        try {
            return Math.incrementExact(value);
        } catch (ArithmeticException e) {
            throw lifecycle(
                    RemoteTaskRunLifecycleException.Reason.INVALID_REMOTE_STATE,
                    false,
                    field + " cannot advance",
                    e);
        }
    }

    private static boolean sameScope(RemoteTaskRunScope left, RemoteTaskRunScope right) {
        return left.getTenantId().equals(right.getTenantId())
                && left.getUserId().equals(right.getUserId())
                && left.getDeviceId().equals(right.getDeviceId())
                && left.getClientSessionId().equals(right.getClientSessionId());
    }

    private static boolean sameOwnerWithoutSession(
            RemoteTaskRunScope left,
            RemoteTaskRunScope right) {
        return left.getTenantId().equals(right.getTenantId())
                && left.getUserId().equals(right.getUserId())
                && left.getDeviceId().equals(right.getDeviceId());
    }

    private static boolean sameWindow(RemoteTaskRunWindow left, RemoteTaskRunWindow right) {
        return left.getWindowId().equals(right.getWindowId())
                && left.getNativeHandle().equals(right.getNativeHandle())
                && left.getProcessId() == right.getProcessId()
                && left.getPlayerIdentityEpoch() == right.getPlayerIdentityEpoch();
    }

    private record OwnerKey(String tenantId, String userId, String deviceId) {
    }

    private record StartKey(
            String tenantId,
            String userId,
            String deviceId,
            String startRequestId) {

        private static StartKey from(ExpectedBinding expected) {
            return new StartKey(
                    expected.scope().getTenantId(),
                    expected.scope().getUserId(),
                    expected.scope().getDeviceId(),
                    expected.startRequestId());
        }

        private static StartKey from(RemoteTaskRunRegistration registration) {
            return new StartKey(
                    registration.getTenantId(),
                    registration.getUserId(),
                    registration.getDeviceId(),
                    registration.getStartRequestId());
        }

        private OwnerKey owner() {
            return new OwnerKey(tenantId, userId, deviceId);
        }
    }

    private static final class StartReservation {
        private final StartKey key;
        private final String clientSessionId;
        private final String taskType;
        private final RemoteTaskRunWindow window;
        private final ReentrantLock operationLock = new ReentrantLock();
        private String taskRunId;
        private RemoteTaskRunRegistration recoveryBinding;
        private boolean cleanupPending;
        private boolean released;

        private StartReservation(StartKey key, ExpectedBinding expected) {
            this.key = key;
            this.clientSessionId = expected.scope().getClientSessionId();
            this.taskType = expected.taskType();
            this.window = expected.window();
        }
    }

    private record ExpectedBinding(
            RemoteTaskRunScope scope,
            String startRequestId,
            String taskType,
            RemoteTaskRunWindow window,
            String taskRunId) {

        private ExpectedBinding withTaskRunId(String value) {
            return new ExpectedBinding(scope, startRequestId, taskType, window, value);
        }

        private static ExpectedBinding from(RemoteTaskRunBinding binding) {
            return new ExpectedBinding(
                    binding.getScope(),
                    binding.getStartRequestId(),
                    binding.getTaskType(),
                    binding.getWindow(),
                    binding.getTaskRunId());
        }

        private static ExpectedBinding from(RemoteTaskRunRegistration registration) {
            return new ExpectedBinding(
                    RemoteTaskRunScope.builder()
                            .tenantId(registration.getTenantId())
                            .userId(registration.getUserId())
                            .deviceId(registration.getDeviceId())
                            .clientSessionId(registration.getClientSessionId())
                            .build(),
                    registration.getStartRequestId(),
                    registration.getTaskType(),
                    RemoteTaskRunWindow.builder()
                            .windowId(registration.getWindowId())
                            .nativeHandle(registration.getNativeHandle())
                            .processId(registration.getProcessId())
                            .playerIdentityEpoch(registration.getPlayerIdentityEpoch())
                            .build(),
                    registration.getTaskRunId());
        }
    }
}
