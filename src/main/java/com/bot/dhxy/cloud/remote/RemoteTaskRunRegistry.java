package com.bot.dhxy.cloud.remote;

import com.bot.dhxy.runner.stop.TaskPauseToken;
import com.bot.dhxy.input.action.InputActionExecutionResult;
import com.bot.dhxy.input.action.InputActionQueue;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Explicit in-memory safety binding for cloud-owned task runs.
 *
 * <p>The registry does not create registrations from commands, start tasks, or advance business
 * phases. Each explicitly registered taskRunId owns one stable pause token until unregister.</p>
 */
public final class RemoteTaskRunRegistry {

    public static final int DEFAULT_GLOBAL_CAPACITY = 10_000;
    public static final int DEFAULT_OWNER_CAPACITY = 1_000;

    private final Object mutationLock = new Object();
    private final ConcurrentMap<String, RegistryEntry> registrations = new ConcurrentHashMap<>();
    private final Map<OwnerKey, Integer> ownerUsage = new HashMap<>();
    private final int globalCapacity;
    private final int ownerCapacity;
    private long nextEntryGeneration;

    public RemoteTaskRunRegistry() {
        this(DEFAULT_GLOBAL_CAPACITY, DEFAULT_OWNER_CAPACITY);
    }

    public RemoteTaskRunRegistry(int globalCapacity, int ownerCapacity) {
        if (globalCapacity <= 0 || ownerCapacity <= 0 || ownerCapacity > globalCapacity) {
            throw new IllegalArgumentException(
                    "registry capacities must be positive and ownerCapacity <= globalCapacity");
        }
        this.globalCapacity = globalCapacity;
        this.ownerCapacity = ownerCapacity;
    }

    /**
     * Explicitly registers one cloud-provided binding or applies a newer confirmed binding.
     *
     * @param registration complete tenant/user/device/session/run/window lifecycle binding
     * @return stored immutable registration
     */
    public RemoteTaskRunRegistration register(RemoteTaskRunRegistration registration) {
        RemoteTaskRunRegistration normalized = normalizeAndValidate(registration);
        synchronized (mutationLock) {
            RegistryEntry existing = registrations.get(normalized.getTaskRunId());
            if (existing == null) {
                rejectCompetingNonTerminal(normalized, null);
                ensureCapacity(normalized);
                TaskPauseToken pauseToken = new TaskPauseToken();
                long entryGeneration = Math.incrementExact(nextEntryGeneration);
                RegistryEntry created = new RegistryEntry(
                        normalized, pauseToken, entryGeneration);
                nextEntryGeneration = entryGeneration;
                registrations.put(normalized.getTaskRunId(), created);
                incrementUsage(normalized);
                if (normalized.getStatus() == RemoteTaskRunStatus.PAUSED) {
                    pauseToken.requestPause(
                            "cloud-confirmed pause revision=" + normalized.getRunRevision());
                } else if (normalized.getStatus() == RemoteTaskRunStatus.STOPPING
                        || normalized.getStatus().isTerminal()) {
                    pauseToken.resume();
                }
                return normalized;
            }
            requireSameBinding(existing.registration, normalized);
            RemoteTaskRunRegistration previous = existing.registration;
            RemoteTaskRunRegistration updated = applyConfirmedTransition(previous, normalized);
            rejectCompetingNonTerminal(updated, updated.getTaskRunId());
            publishTransition(existing, previous, updated);
            return updated;
        }
    }

    /**
     * Applies a cloud-confirmed lifecycle binding to an already registered run.
     *
     * <p>A confirmed PAUSED binding requests the stable token. The token resumes only after a
     * revision-advancing PAUSED-to-ACTIVE transition has been accepted.</p>
     */
    public Optional<RemoteTaskRunRegistration> applyConfirmedBinding(
            RemoteClientSessionRef clientSession,
            RemoteTaskRunRegistration confirmed) {
        validateClientSession(clientSession);
        RemoteTaskRunRegistration normalized = normalizeAndValidate(confirmed);
        synchronized (mutationLock) {
            RegistryEntry entry = registrations.get(normalized.getTaskRunId());
            if (entry == null) {
                return Optional.empty();
            }
            requireMatches(entry.registration, clientSession, normalized.getWindowId());
            requireSameBinding(entry.registration, normalized);
            RemoteTaskRunRegistration previous = entry.registration;
            RemoteTaskRunRegistration updated = applyConfirmedTransition(previous, normalized);
            rejectCompetingNonTerminal(updated, updated.getTaskRunId());
            publishTransition(entry, previous, updated);
            return Optional.of(updated);
        }
    }

    /**
     * Fails closed locally before any network stop call and wakes an in-flight pause wait.
     *
     * @param clientSession exact tenant/user/device/session owner
     * @param taskRunId cloud task run id
     * @param windowId logical window id
     * @param stopEpoch exact current registration stop epoch; pre-incremented or stale values are invalid
     * @return local STOPPING binding, or empty when the run is not registered
     */
    public Optional<RemoteTaskRunRegistration> beginStop(
            RemoteClientSessionRef clientSession,
            String taskRunId,
            String windowId,
            long stopEpoch) {
        validateClientSession(clientSession);
        requireText(taskRunId, "taskRunId");
        requireText(windowId, "windowId");
        if (stopEpoch < 0L) {
            throw new IllegalArgumentException("stopEpoch must not be negative");
        }
        synchronized (mutationLock) {
            RegistryEntry entry = registrations.get(taskRunId.trim());
            if (entry == null) {
                return Optional.empty();
            }
            requireMatches(entry.registration, clientSession, windowId.trim());
            if (stopEpoch != entry.registration.getStopEpoch()) {
                throw new IllegalArgumentException(
                        "stopEpoch must exactly match the current registration stopEpoch");
            }
            if (entry.registration.getStatus().isTerminal()) {
                return Optional.of(entry.registration);
            }
            RemoteTaskRunRegistration previous = entry.registration;
            RemoteTaskRunRegistration stopping = previous.toBuilder()
                    .stopEpoch(stopEpoch)
                    .status(RemoteTaskRunStatus.STOPPING)
                    .build();
            publishTransition(entry, previous, stopping);
            return Optional.of(stopping);
        }
    }

    /**
     * Advances only the cloud revision retained by one exact local STOPPING binding.
     *
     * <p>The observed cloud state must remain non-terminal at the same canonical stop epoch.
     * Its ACTIVE, PAUSED, or PREPARED status is deliberately not published locally, and the
     * stable pause token is not changed. This lets a later STOP retry use the latest cloud CAS
     * revision without reopening local command authorization.</p>
     *
     * @param clientSession exact tenant/user/device/session owner
     * @param observed cloud-observed non-terminal binding whose revision may advance
     * @return updated local STOPPING registration, or empty when the run is not registered
     */
    public Optional<RemoteTaskRunRegistration> advanceStoppingProgress(
            RemoteClientSessionRef clientSession,
            RemoteTaskRunRegistration observed) {
        validateClientSession(clientSession);
        RemoteTaskRunRegistration normalized = normalizeAndValidate(observed);
        synchronized (mutationLock) {
            RegistryEntry entry = registrations.get(normalized.getTaskRunId());
            if (entry == null) {
                return Optional.empty();
            }
            requireMatches(entry.registration, clientSession, normalized.getWindowId());
            requireSameBinding(entry.registration, normalized);
            RemoteTaskRunRegistration current = entry.registration;
            if (current.getStatus() != RemoteTaskRunStatus.STOPPING) {
                throw new IllegalStateException("only a local STOPPING run can advance stop progress");
            }
            if (normalized.getStatus() == RemoteTaskRunStatus.STOPPING
                    || normalized.getStatus().isTerminal()) {
                throw new IllegalArgumentException(
                        "stopping progress requires a remote PREPARED, ACTIVE, or PAUSED binding");
            }
            if (normalized.getStopEpoch() != current.getStopEpoch()) {
                throw new IllegalArgumentException(
                        "non-terminal stopping progress must keep the canonical stopEpoch");
            }
            if (normalized.getRunRevision() < current.getRunRevision()) {
                throw new IllegalArgumentException("runRevision must not move backwards");
            }
            if (normalized.getRunRevision() == current.getRunRevision()) {
                publishTransition(entry, current, current);
                return Optional.of(current);
            }
            RemoteTaskRunRegistration advanced = current.toBuilder()
                    .runRevision(normalized.getRunRevision())
                    .build();
            publishTransition(entry, current, advanced);
            return Optional.of(advanced);
        }
    }

    public Optional<RemoteTaskRunRegistration> find(String taskRunId) {
        if (taskRunId == null || taskRunId.isBlank()) {
            return Optional.empty();
        }
        RegistryEntry entry = registrations.get(taskRunId.trim());
        return entry == null ? Optional.empty() : Optional.of(entry.registration);
    }

    /**
     * Captures the exact immutable registry generation used for command-ledger admission.
     *
     * <p>This is a lock-free identity snapshot. It performs no ledger call and exposes no live
     * window object; the operation ledger validates the complete command tuple before reserving
     * any detail, and {@link #isCurrent(CommandAdmissionSnapshot, RemoteGameCommand)} repeats the
     * same generation check immediately before desktop side effects.</p>
     */
    CommandAdmissionSnapshot commandAdmissionSnapshot(
            RemoteClientSessionRef clientSession,
            RemoteGameCommand command) {
        validateClientSession(clientSession);
        if (command == null || command.getTaskRunId() == null
                || command.getTaskRunId().isBlank()) {
            return null;
        }
        RegistryEntry entry = registrations.get(command.getTaskRunId().trim());
        if (entry == null) {
            return null;
        }
        RemoteTaskRunRegistration observed = entry.registration;
        return new CommandAdmissionSnapshot(
                this, entry, entry.entryGeneration, observed, clientSession);
    }

    /** Returns true only while the exact admission generation and immutable binding remain current. */
    boolean isCurrent(CommandAdmissionSnapshot snapshot, RemoteGameCommand command) {
        if (snapshot == null || snapshot.owner != this || command == null) {
            return false;
        }
        RegistryEntry current = registrations.get(snapshot.registration.getTaskRunId());
        return current == snapshot.entry
                && current.entryGeneration == snapshot.entryGeneration
                && current.registration == snapshot.registration
                && snapshot.matches(command);
    }

    InFlightExclusiveHandle openInFlightExclusive(
            CommandAdmissionSnapshot admissionSnapshot,
            RemoteGameCommand command,
            String exclusiveSessionId) {
        String sessionId = requireText(exclusiveSessionId, "exclusiveSessionId");
        synchronized (mutationLock) {
            if (admissionSnapshot == null || admissionSnapshot.owner != this
                    || command == null || !admissionSnapshot.matches(command)) {
                throw new IllegalStateException(
                        "exclusive continuation requires an exact command admission snapshot");
            }
            RegistryEntry entry = admissionSnapshot.entry;
            RemoteTaskRunRegistration registration = admissionSnapshot.registration;
            if (registrations.get(registration.getTaskRunId()) != entry
                    || entry.entryGeneration != admissionSnapshot.entryGeneration
                    || entry.registration != registration
                    || registration.getStatus() != RemoteTaskRunStatus.ACTIVE) {
                throw new IllegalStateException(
                        "exclusive continuation admission is no longer current ACTIVE");
            }
            if (entry.inFlightExclusiveHandle != null) {
                throw new IllegalStateException(
                        "task run already owns an in-flight exclusive callback");
            }

            long transitionGeneration = Math.incrementExact(
                    entry.nextContinuationGeneration);
            ContinuationIdentity identity = new ContinuationIdentity(
                    this,
                    entry,
                    entry.entryGeneration,
                    entry.pauseToken,
                    registration.getTenantId(),
                    registration.getUserId(),
                    registration.getDeviceId(),
                    registration.getClientSessionId(),
                    registration.getTaskRunId(),
                    registration.getTaskType(),
                    registration.getWindowId(),
                    registration.getNativeHandle(),
                    registration.getProcessId(),
                    registration.getPlayerIdentityEpoch(),
                    registration.getStopEpoch(),
                    sessionId);
            QueuedActiveSnapshot queued = new QueuedActiveSnapshot(
                    identity, registration, transitionGeneration);
            InFlightExclusiveHandle handle = new InFlightExclusiveHandle(
                    this, entry, identity, queued);

            entry.nextContinuationGeneration = transitionGeneration;
            entry.inFlightExclusiveHandle = handle;
            return handle;
        }
    }

    InFlightExclusiveHandle openGenericExclusive(
            CommandAdmissionSnapshot admissionSnapshot,
            RemoteGameCommand command,
            RemoteExclusiveInteractionControlCommandPayload control) {
        RemoteExclusiveInteractionControlCommandPayload exact = Objects.requireNonNull(
                control, "control");
        if (exact.getCommand()
                        != RemoteExclusiveInteractionControlCommandPayload.Command.ACQUIRE
                || exact.getStep() != 1L) {
            throw new IllegalStateException(
                    "generic exclusive ACQUIRE must open at step 1");
        }
        synchronized (mutationLock) {
            // Keep publication and generic cursor initialization in one registry mutation. The
            // nested monitor acquisition is reentrant, so lifecycle cannot observe a
            // whole-pass-shaped handle between these two commits.
            InFlightExclusiveHandle handle = openInFlightExclusive(
                    admissionSnapshot, command, exact.getExclusiveSessionId());
            InFlightExclusiveHandle retained = requireOwnedHandle(handle);
            retained.generic = true;
            retained.genericBindingGeneration = exact.getBindingGeneration();
            retained.genericNextStep = exact.getStep();
            return retained;
        }
    }

    InFlightExclusiveHandle bindGenericExclusiveStep(
            CommandAdmissionSnapshot admissionSnapshot,
            RemoteGameCommand command,
            RemoteExclusiveSessionStepRef stepRef) {
        RemoteExclusiveSessionStepRef ref = Objects.requireNonNull(stepRef, "stepRef");
        synchronized (mutationLock) {
            if (admissionSnapshot == null || admissionSnapshot.owner != this
                    || command == null || !admissionSnapshot.matches(command)) {
                throw new IllegalStateException(
                        "generic exclusive step requires the exact admission snapshot");
            }
            InFlightExclusiveHandle handle = admissionSnapshot.entry.inFlightExclusiveHandle;
            if (handle == null || !handle.generic
                    || handle.entry != admissionSnapshot.entry
                    || !handle.identity.exclusiveSessionId().equals(
                            ref.getExclusiveSessionId())) {
                throw new IllegalStateException(
                        "generic exclusive session/generation/step is stale or foreign");
            }
            if (handle.genericBoundRequestId != null) {
                if (!handle.genericBoundRequestId.equals(command.getRequestId())
                        || !handle.genericBoundRequestDigest.equals(command.getRequestDigest())
                        || handle.genericBoundStepGeneration != ref.getBindingGeneration()
                        || handle.genericBoundStep != ref.getStep()) {
                    throw new IllegalStateException(
                            "generic exclusive session already has another bound step");
                }
                if (checkInFlightExclusiveLocked(handle) != InFlightExclusiveCheck.CLEAR) {
                    throw new IllegalStateException(
                            "generic exclusive bound step is not currently executable");
                }
                return handle;
            }
            if (handle.genericBindingGeneration != ref.getBindingGeneration()
                    || handle.genericNextStep != ref.getStep()
                    || checkInFlightExclusiveLocked(handle) != InFlightExclusiveCheck.CLEAR) {
                throw new IllegalStateException(
                        "generic exclusive session/generation/step is stale or foreign");
            }
            handle.genericBoundRequestId = command.getRequestId();
            handle.genericBoundRequestDigest = command.getRequestDigest();
            handle.genericBoundStepGeneration = ref.getBindingGeneration();
            handle.genericBoundStep = ref.getStep();
            return handle;
        }
    }

    InFlightExclusiveHandle requireGenericExclusiveControl(
            CommandAdmissionSnapshot admissionSnapshot,
            RemoteGameCommand command,
            RemoteExclusiveInteractionControlCommandPayload control) {
        RemoteExclusiveInteractionControlCommandPayload exact = Objects.requireNonNull(
                control, "control");
        synchronized (mutationLock) {
            if (admissionSnapshot == null || admissionSnapshot.owner != this
                    || command == null || !admissionSnapshot.matches(command)) {
                throw new IllegalStateException(
                        "generic exclusive control requires the exact admission snapshot");
            }
            InFlightExclusiveHandle handle = admissionSnapshot.entry.inFlightExclusiveHandle;
            boolean currentCursor = handle != null
                    && handle.genericNextStep == exact.getStep();
            boolean exactCompletedCursor = handle != null
                    && handle.genericBoundRequestId == null
                    && handle.genericLastCompletedStep == exact.getStep()
                    && exact.getBindingGeneration()
                            >= handle.genericLastCompletedStepGeneration
                    && exact.getStep() < Long.MAX_VALUE
                    && handle.genericNextStep == exact.getStep() + 1L;
            if (handle == null || !handle.generic
                    || handle.entry != admissionSnapshot.entry
                    || !handle.identity.exclusiveSessionId().equals(
                            exact.getExclusiveSessionId())
                    || handle.genericBindingGeneration != exact.getBindingGeneration()
                    || (!currentCursor && !exactCompletedCursor)
                    || checkInFlightExclusiveLocked(handle) != InFlightExclusiveCheck.CLEAR) {
                throw new IllegalStateException(
                        "generic exclusive control session/generation/step is stale or foreign");
            }
            return handle;
        }
    }

    void attachGenericInputSession(
            InFlightExclusiveHandle handle,
            InputActionQueue.RetainedSessionHandle inputSession) {
        synchronized (mutationLock) {
            InFlightExclusiveHandle retained = requireOwnedHandle(handle);
            if (!retained.generic || retained.inputSession != null) {
                throw new IllegalStateException(
                        "generic input session is absent or already attached");
            }
            retained.inputSession = Objects.requireNonNull(inputSession, "inputSession");
        }
    }

    InputActionQueue.RetainedSessionHandle genericInputSession(
            InFlightExclusiveHandle handle) {
        synchronized (mutationLock) {
            InFlightExclusiveHandle retained = requireOwnedHandle(handle);
            if (!retained.generic || retained.inputSession == null) {
                throw new IllegalStateException("generic input session has not been attached");
            }
            return retained.inputSession;
        }
    }

    void completeGenericStep(
            InFlightExclusiveHandle handle,
            RemoteExclusiveSessionStepRef completedRef,
            boolean mechanicalExecutionProvablyNotStarted) {
        synchronized (mutationLock) {
            InFlightExclusiveHandle retained = requireOwnedHandle(handle);
            RemoteExclusiveSessionStepRef ref = Objects.requireNonNull(
                    completedRef, "completedRef");
            if (!retained.generic
                    || !retained.identity.exclusiveSessionId().equals(ref.getExclusiveSessionId())
                    || retained.genericBoundRequestId == null
                    || retained.genericBoundStepGeneration != ref.getBindingGeneration()
                    || retained.genericBoundStep != ref.getStep()
                    || retained.genericNextStep != ref.getStep()
                    || retained.inputSession == null
                    || (retained.inputSession.terminalSnapshot() != null
                            && !mechanicalExecutionProvablyNotStarted)
                    || checkInFlightExclusiveLocked(retained) != InFlightExclusiveCheck.CLEAR) {
                throw new IllegalStateException("generic step completion cursor is stale");
            }
            retained.genericLastCompletedStepGeneration = ref.getBindingGeneration();
            retained.genericLastCompletedStep = ref.getStep();
            retained.genericNextStep = Math.incrementExact(retained.genericNextStep);
            retained.genericBoundRequestId = null;
            retained.genericBoundRequestDigest = null;
            retained.genericBoundStepGeneration = -1L;
            retained.genericBoundStep = -1L;
        }
    }

    void requireGenericExclusiveStepCurrent(
            InFlightExclusiveHandle handle,
            RemoteExclusiveSessionStepRef stepRef) {
        synchronized (mutationLock) {
            InFlightExclusiveHandle retained = requireOwnedHandle(handle);
            RemoteExclusiveSessionStepRef ref = Objects.requireNonNull(stepRef, "stepRef");
            if (!retained.generic
                    || !retained.identity.exclusiveSessionId().equals(ref.getExclusiveSessionId())
                    || retained.genericBoundRequestId == null
                    || retained.genericBoundStepGeneration != ref.getBindingGeneration()
                    || retained.genericBoundStep != ref.getStep()
                    || retained.inputSession == null
                    || retained.inputSession.terminalSnapshot() != null
                    || checkInFlightExclusiveLocked(retained) != InFlightExclusiveCheck.CLEAR) {
                throw new IllegalStateException(
                        "generic bound step is no longer current for mechanical execution");
            }
        }
    }

    void retainGenericTerminalSnapshot(
            InFlightExclusiveHandle handle,
            InputActionExecutionResult terminalSnapshot) {
        synchronized (mutationLock) {
            InFlightExclusiveHandle retained = requireOwnedHandle(handle);
            retained.genericTerminalSnapshot = Objects.requireNonNull(
                    terminalSnapshot, "terminalSnapshot");
        }
    }

    InFlightExclusiveCheck admitInFlightExclusive(
            InFlightExclusiveHandle handle,
            RemoteGameCommand originalCommand) {
        synchronized (mutationLock) {
            InFlightExclusiveHandle retained = requireOwnedHandle(handle);
            ContinuationSnapshot observed = retained.currentSnapshot;
            if (observed instanceof CallbackActiveSnapshot) {
                return checkInFlightExclusiveLocked(retained);
            }
            if (!(observed instanceof QueuedActiveSnapshot queued)) {
                return classifyNonActiveSnapshot(observed);
            }
            RegistryEntry entry = retained.entry;
            if (registrations.get(retained.identity.taskRunId()) == entry
                    && entry.inFlightExclusiveHandle == retained
                    && entry.registration == queued.registration()
                    && queued.identity().matches(entry.registration)
                    && queued.registration().getStatus() == RemoteTaskRunStatus.ACTIVE
                    && originalCommand != null
                    && queued.matches(originalCommand)) {
                long transitionGeneration = Math.incrementExact(
                        entry.nextContinuationGeneration);
                CallbackActiveSnapshot active = new CallbackActiveSnapshot(
                        retained.identity,
                        entry.registration,
                        transitionGeneration,
                        -1L,
                        -1L);
                entry.nextContinuationGeneration = transitionGeneration;
                retained.currentSnapshot = active;
                return InFlightExclusiveCheck.CLEAR;
            }

            invalidateAndDetachLocked(retained, "STALE_BEFORE_ADMISSION", entry.registration);
            return entry.registration.getStatus() == RemoteTaskRunStatus.STOPPING
                    || entry.registration.getStatus().isTerminal()
                    ? InFlightExclusiveCheck.STOPPED
                    : InFlightExclusiveCheck.MISMATCH;
        }
    }

    InFlightExclusiveCheck checkInFlightExclusive(InFlightExclusiveHandle handle) {
        synchronized (mutationLock) {
            return checkInFlightExclusiveLocked(requireOwnedHandle(handle));
        }
    }

    void closeInFlightExclusive(InFlightExclusiveHandle handle) {
        synchronized (mutationLock) {
            InFlightExclusiveHandle retained = requireOwnedHandle(handle);
            ContinuationSnapshot observed = retained.currentSnapshot;
            if (observed instanceof ClosedSnapshot) {
                if (retained.entry.inFlightExclusiveHandle == retained) {
                    retained.entry.inFlightExclusiveHandle = null;
                }
                return;
            }
            long nextGeneration = observed.localTransitionGeneration() == Long.MAX_VALUE
                    ? Long.MAX_VALUE
                    : observed.localTransitionGeneration() + 1L;
            ClosedSnapshot closed = new ClosedSnapshot(
                    retained.identity,
                    observed.registration(),
                    nextGeneration);
            retained.currentSnapshot = closed;
            if (retained.entry.inFlightExclusiveHandle == retained) {
                retained.entry.inFlightExclusiveHandle = null;
            }
        }
    }

    void invalidateExclusiveOwnersForIncarnationChange(RemoteTaskRunScope scope) {
        RemoteTaskRunScope exactScope = Objects.requireNonNull(scope, "scope");
        synchronized (mutationLock) {
            for (RegistryEntry entry : registrations.values()) {
                RemoteTaskRunRegistration registration = entry.registration;
                InFlightExclusiveHandle handle = entry.inFlightExclusiveHandle;
                if (handle != null
                        && exactScope.getTenantId().equals(registration.getTenantId())
                        && exactScope.getUserId().equals(registration.getUserId())
                        && exactScope.getDeviceId().equals(registration.getDeviceId())
                        && exactScope.getClientSessionId().equals(
                                registration.getClientSessionId())) {
                    invalidateAndDetachLocked(
                            handle, "CLOUD_INCARNATION_CHANGED", registration);
                }
            }
        }
    }

    private InFlightExclusiveCheck checkInFlightExclusiveLocked(
            InFlightExclusiveHandle handle) {
        ContinuationSnapshot snapshot = handle.currentSnapshot;
        RegistryEntry entry = handle.entry;
        RegistryEntry current = registrations.get(handle.identity.taskRunId());
        if (current != entry
                || entry.entryGeneration != handle.identity.entryGeneration()
                || entry.pauseToken != handle.identity.pauseToken()
                || entry.inFlightExclusiveHandle != handle) {
            return classifyNonActiveSnapshot(snapshot);
        }
        if (snapshot instanceof CallbackPausedSnapshot paused
                && entry.registration == paused.registration()
                && paused.identity().matches(entry.registration)
                && entry.registration.getStatus() == RemoteTaskRunStatus.PAUSED) {
            return InFlightExclusiveCheck.PAUSED;
        }
        if (snapshot instanceof CallbackActiveSnapshot active
                && entry.registration == active.registration()
                && active.identity().matches(entry.registration)
                && entry.registration.getStatus() == RemoteTaskRunStatus.ACTIVE) {
            return InFlightExclusiveCheck.CLEAR;
        }
        if (entry.registration.getStatus() == RemoteTaskRunStatus.STOPPING
                || entry.registration.getStatus().isTerminal()) {
            return InFlightExclusiveCheck.STOPPED;
        }
        return InFlightExclusiveCheck.MISMATCH;
    }

    private static InFlightExclusiveCheck classifyNonActiveSnapshot(
            ContinuationSnapshot snapshot) {
        if (snapshot instanceof CallbackPausedSnapshot) {
            return InFlightExclusiveCheck.PAUSED;
        }
        if (snapshot instanceof InvalidatedSnapshot invalidated
                && (invalidated.reason().startsWith("STOPPING")
                        || invalidated.reason().startsWith("TERMINAL")
                        || invalidated.reason().startsWith("ENTRY_REMOVED"))) {
            return InFlightExclusiveCheck.STOPPED;
        }
        return InFlightExclusiveCheck.MISMATCH;
    }

    private InFlightExclusiveHandle requireOwnedHandle(InFlightExclusiveHandle handle) {
        InFlightExclusiveHandle retained = Objects.requireNonNull(handle, "handle");
        if (retained.owner != this
                || retained.identity.owner() != this
                || retained.identity.entry() != retained.entry
                || retained.identity.entryGeneration() != retained.entry.entryGeneration
                || retained.identity.pauseToken() != retained.entry.pauseToken) {
            throw new IllegalStateException(
                    "exclusive continuation handle belongs to another registry entry");
        }
        return retained;
    }

    private void invalidateAndDetachLocked(
            InFlightExclusiveHandle handle,
            String reason,
            RemoteTaskRunRegistration registration) {
        ContinuationSnapshot current = handle.currentSnapshot;
        if (current instanceof ClosedSnapshot) {
            if (handle.entry.inFlightExclusiveHandle == handle) {
                handle.entry.inFlightExclusiveHandle = null;
            }
            return;
        }
        long transitionGeneration = Math.incrementExact(
                handle.entry.nextContinuationGeneration);
        InvalidatedSnapshot invalidated = new InvalidatedSnapshot(
                handle.identity,
                Objects.requireNonNull(registration, "registration"),
                transitionGeneration,
                reason);
        handle.entry.nextContinuationGeneration = transitionGeneration;
        handle.currentSnapshot = invalidated;
        if (handle.entry.inFlightExclusiveHandle == handle) {
            handle.entry.inFlightExclusiveHandle = null;
        }
    }

    TerminalCleanupObservation observeTerminalCleanup(
            RemoteTaskRunScope scope,
            RemoteOperationLedger.TerminalCleanupCandidate candidate) {
        if (scope == null || candidate == null) {
            throw new IllegalArgumentException("terminal cleanup scope and candidate are required");
        }
        RegistryEntry entry = registrations.get(candidate.taskRunId());
        if (entry == null) {
            return new TerminalCleanupObservation(
                    candidate.taskRunId(), TerminalCleanupStatus.ABSENT, null);
        }
        RemoteTaskRunRegistration observed = entry.registration;
        if (!matchesScope(observed, scope) || !candidate.matchesStableBinding(observed)) {
            throw new IllegalStateException(
                    "terminal cleanup candidate does not match the current registry binding");
        }
        return new TerminalCleanupObservation(
                candidate.taskRunId(),
                observed.getStatus().isTerminal()
                        ? TerminalCleanupStatus.TERMINAL : TerminalCleanupStatus.NOT_READY,
                observed);
    }

    /**
     * Requests the stable token of one exact ACTIVE run without publishing PAUSED prematurely.
     *
     * @return unchanged ACTIVE registration whose token is now paused
     */
    public RemoteTaskRunRegistration requestPause(
            RemoteClientSessionRef clientSession,
            String taskRunId,
            String windowId,
            String reason) {
        validateClientSession(clientSession);
        requireText(taskRunId, "taskRunId");
        requireText(windowId, "windowId");
        synchronized (mutationLock) {
            RegistryEntry entry = registrations.get(taskRunId.trim());
            if (entry == null) {
                throw new IllegalStateException("remote task run is not registered");
            }
            requireMatches(entry.registration, clientSession, windowId.trim());
            if (entry.registration.getStatus() != RemoteTaskRunStatus.ACTIVE) {
                throw new IllegalStateException("only an ACTIVE remote task run can request pause");
            }
            entry.pauseToken.requestPause(reason);
            return entry.registration;
        }
    }

    /** Returns the stable pause token for one exact registered run. */
    public Optional<TaskPauseToken> pauseToken(
            RemoteClientSessionRef clientSession,
            String taskRunId,
            String windowId) {
        validateClientSession(clientSession);
        requireText(windowId, "windowId");
        RegistryEntry entry = taskRunId == null ? null : registrations.get(taskRunId.trim());
        if (entry == null || !matches(entry.registration, clientSession, windowId.trim())) {
            return Optional.empty();
        }
        return Optional.of(entry.pauseToken);
    }

    /**
     * Releases one exact terminal snapshot and its stable pause token.
     *
     * @return released terminal registration, or empty when taskRunId is absent
     */
    public Optional<RemoteTaskRunRegistration> releaseTerminal(
            RemoteClientSessionRef clientSession,
            RemoteTaskRunRegistration expectedTerminal) {
        validateClientSession(clientSession);
        RemoteTaskRunRegistration normalized = normalizeAndValidate(expectedTerminal);
        synchronized (mutationLock) {
            RegistryEntry entry = registrations.get(normalized.getTaskRunId());
            if (entry == null) {
                return Optional.empty();
            }
            requireMatches(entry.registration, clientSession, normalized.getWindowId());
            requireSameBinding(entry.registration, normalized);
            if (!entry.registration.getStatus().isTerminal()
                    || !entry.registration.equals(normalized)) {
                throw new IllegalStateException(
                        "terminal release requires the exact current terminal snapshot");
            }
            PreparedEntryTransition prepared = prepareEntryTransition(
                    entry, entry.registration, entry.registration);
            applyPreparedWithoutToken(entry, prepared);
            registrations.remove(entry.registration.getTaskRunId(), entry);
            decrementUsage(entry.registration);
            entry.pauseToken.resume();
            return Optional.of(entry.registration);
        }
    }

    /** Legacy explicit removal is terminal-only; non-terminal registrations are never discarded. */
    public boolean unregister(RemoteClientSessionRef clientSession, String taskRunId, String windowId) {
        validateClientSession(clientSession);
        requireText(windowId, "windowId");
        synchronized (mutationLock) {
            RegistryEntry entry = taskRunId == null ? null : registrations.get(taskRunId.trim());
            if (entry == null || !matches(entry.registration, clientSession, windowId.trim())) {
                return false;
            }
            if (!entry.registration.getStatus().isTerminal()) {
                throw new IllegalStateException("non-terminal remote task run cannot be unregistered");
            }
            PreparedEntryTransition prepared = prepareEntryTransition(
                    entry, entry.registration, entry.registration);
            applyPreparedWithoutToken(entry, prepared);
            boolean removed = registrations.remove(entry.registration.getTaskRunId(), entry);
            if (removed) {
                decrementUsage(entry.registration);
            }
            entry.pauseToken.resume();
            return removed;
        }
    }

    public int size() {
        return registrations.size();
    }

    PendingExecutorReadiness.DrainCandidate findDrainCandidate(RemoteTaskRunScope scope) {
        synchronized (mutationLock) {
            for (RegistryEntry entry : registrations.values()) {
                PendingExecutorReadiness slot = entry.pendingReadiness;
                if (slot != null && slot.state == PendingExecutorReadiness.State.AWAITING_DRAIN
                        && matchesScope(entry.registration, scope)) {
                    return new PendingExecutorReadiness.DrainCandidate(
                            entry.entryGeneration, slot.slotGeneration,
                            entry.registration.getTaskRunId(), slot.fromRevision, slot.toRevision,
                            entry.registration,
                            entry.pauseToken.pauseProgress().revision());
                }
            }
            return null;
        }
    }

    void materializeReady(
            PendingExecutorReadiness.DrainCandidate candidate,
            RemoteOperationLedger.QuiescenceSnapshot snapshot) {
        if (candidate == null || snapshot == null || !snapshot.isQuiescent()) {
            return;
        }
        synchronized (mutationLock) {
            RegistryEntry entry = registrations.get(candidate.taskRunId());
            PendingExecutorReadiness slot = entry == null ? null : entry.pendingReadiness;
            if (entry == null || entry.entryGeneration != candidate.entryGeneration()
                    || slot == null || slot.slotGeneration != candidate.slotGeneration()
                    || slot.state != PendingExecutorReadiness.State.AWAITING_DRAIN
                    || entry.registration != candidate.registration()
                    || entry.registration.getStatus() != RemoteTaskRunStatus.ACTIVE
                    || entry.registration.getRunRevision() != candidate.toRevision()
                    || snapshot.ledgerRevision() != snapshot.expectedLedgerRevision()
                    || !snapshot.matches(candidate.registration(), candidate.toRevision())) {
                return;
            }
            RemoteTaskRunRegistration registration = entry.registration;
            ResumeExecutorReadinessFact draft = ResumeExecutorReadinessFact.builder()
                    .taskType(registration.getTaskType())
                    .windowId(registration.getWindowId())
                    .nativeHandle(registration.getNativeHandle())
                    .processId(registration.getProcessId())
                    .playerIdentityEpoch(registration.getPlayerIdentityEpoch())
                    .stopEpoch(registration.getStopEpoch())
                    .resumedFromRunRevision(slot.fromRevision)
                    .newActiveRunRevision(slot.toRevision)
                    .localRegistrationGeneration(entry.entryGeneration)
                    .localRegistrationStatus("ACTIVE")
                    .previousLocalStatus("PAUSED")
                    .pauseTokenMechanicalGeneration(candidate.pauseTokenMechanicalGeneration())
                    .operationLedgerRevision(snapshot.ledgerRevision())
                    .inFlightCaptureCount(snapshot.inFlightCaptureCount())
                    .inFlightFactCount(snapshot.inFlightFactCount())
                    .inFlightInputCount(snapshot.inFlightInputCount())
                    .observedAtEpochMs(System.currentTimeMillis())
                    .producer("REGISTRY_RESUME_PUBLISH")
                    .factDigest(RemoteProtocolDigests.ZERO_SHA256)
                    .build();
            RemoteProtocolDigests digests = new RemoteProtocolDigests();
            ResumeExecutorReadinessFact fact = draft.toBuilder()
                    .factDigest(digests.computeResumeFactDigest(draft))
                    .build();
            String requestId = java.util.UUID.randomUUID().toString();
            RemoteTaskRunActionRequest unsigned = RemoteTaskRunActionRequest.builder()
                    .contractVersion(1)
                    .action(RemoteTaskRunAction.CONFIRM_RESUMED_EXECUTOR_READY)
                    .tenantId(registration.getTenantId())
                    .userId(registration.getUserId())
                    .deviceId(registration.getDeviceId())
                    .clientSessionId(registration.getClientSessionId())
                    .taskRunId(registration.getTaskRunId())
                    .requestId(requestId)
                    .requestDigest(RemoteProtocolDigests.ZERO_SHA256)
                    .fact(fact)
                    .build();
            RemoteTaskRunActionRequest request = unsigned.toBuilder()
                    .requestDigest(digests.computeTaskRunActionDigest(unsigned))
                    .build();
            slot.retainedSend = new PendingExecutorReadiness.RetainedSend(
                    request, requestId, request.getRequestDigest(), fact.getFactDigest());
            slot.state = PendingExecutorReadiness.State.READY;
        }
    }

    PendingExecutorReadiness.PendingSendHandle claimReadyForSend(
            RemoteTaskRunScope scope,
            long nowNanos) {
        synchronized (mutationLock) {
            for (RegistryEntry entry : registrations.values()) {
                PendingExecutorReadiness slot = entry.pendingReadiness;
                if (slot != null && slot.state == PendingExecutorReadiness.State.READY
                        && slot.nextAttemptNotBeforeNanos <= nowNanos
                        && matchesScope(entry.registration, scope)) {
                    slot.state = PendingExecutorReadiness.State.SENDING;
                    return new PendingExecutorReadiness.PendingSendHandle(
                            entry.entryGeneration, slot.slotGeneration,
                            slot.retainedSend.requestId(), slot.toRevision, slot.retainedSend);
                }
            }
            return null;
        }
    }

    ResultWrite markAccepted(
            PendingExecutorReadiness.PendingSendHandle handle,
            RemoteTaskRunReceipt receipt) {
        synchronized (mutationLock) {
            RegistryEntry entry = currentSending(handle);
            if (entry == null || receipt == null
                    || !handle.requestId().equals(receipt.getRequestId())
                    || !handle.retainedSend().requestDigest().equals(receipt.getRequestDigest())
                    || !handle.retainedSend().factDigest().equals(receipt.getFactDigest())
                    || handle.toRevision() != receipt.getConfirmedRunRevision()
                    // Final local CAS gate must not trust client-side correlation: a receipt for
                    // another run may never clear this run's readiness slot.
                    || !handle.retainedSend().request().getTaskRunId()
                            .equals(receipt.getTaskRunId())) {
                return ResultWrite.STALE_HANDLE_IGNORED;
            }
            entry.pendingReadiness = null;
            return ResultWrite.APPLIED;
        }
    }

    ResultWrite markUnknownForRetry(
            PendingExecutorReadiness.PendingSendHandle handle,
            long nextAttemptNotBeforeNanos) {
        synchronized (mutationLock) {
            RegistryEntry entry = currentSending(handle);
            if (entry == null) {
                return ResultWrite.STALE_HANDLE_IGNORED;
            }
            entry.pendingReadiness.state = PendingExecutorReadiness.State.READY;
            entry.pendingReadiness.nextAttemptNotBeforeNanos = nextAttemptNotBeforeNanos;
            return ResultWrite.APPLIED;
        }
    }

    ResultWrite markPermanentRejected(
            PendingExecutorReadiness.PendingSendHandle handle,
            RemoteTaskRunErrorCode code) {
        synchronized (mutationLock) {
            RegistryEntry entry = currentSending(handle);
            if (entry == null) {
                return ResultWrite.STALE_HANDLE_IGNORED;
            }
            entry.pendingReadiness = null;
            return ResultWrite.APPLIED;
        }
    }

    private RegistryEntry currentSending(PendingExecutorReadiness.PendingSendHandle handle) {
        if (handle == null) {
            return null;
        }
        RegistryEntry entry = registrations.get(handle.retainedSend().request().getTaskRunId());
        PendingExecutorReadiness slot = entry == null ? null : entry.pendingReadiness;
        return entry != null && entry.entryGeneration == handle.entryGeneration()
                && slot != null && slot.state == PendingExecutorReadiness.State.SENDING
                && slot.slotGeneration == handle.slotGeneration()
                && slot.toRevision == handle.toRevision()
                && Objects.equals(slot.retainedSend.requestId(), handle.requestId())
                && slot.retainedSend == handle.retainedSend() ? entry : null;
    }

    private static boolean matchesScope(
            RemoteTaskRunRegistration registration,
            RemoteTaskRunScope scope) {
        return scope != null
                && Objects.equals(registration.getTenantId(), scope.getTenantId())
                && Objects.equals(registration.getUserId(), scope.getUserId())
                && Objects.equals(registration.getDeviceId(), scope.getDeviceId())
                && Objects.equals(registration.getClientSessionId(), scope.getClientSessionId());
    }

    enum ResultWrite { APPLIED, STALE_HANDLE_IGNORED }

    private void ensureCapacity(RemoteTaskRunRegistration candidate) {
        if (registrations.size() >= globalCapacity) {
            throw new CapacityExceededException("remote task-run registry global capacity exceeded");
        }
        OwnerKey owner = OwnerKey.from(candidate);
        if (ownerUsage.getOrDefault(owner, 0) >= ownerCapacity) {
            throw new CapacityExceededException("remote task-run registry owner capacity exceeded");
        }
    }

    private void incrementUsage(RemoteTaskRunRegistration registration) {
        OwnerKey owner = OwnerKey.from(registration);
        ownerUsage.put(owner, ownerUsage.getOrDefault(owner, 0) + 1);
    }

    private void decrementUsage(RemoteTaskRunRegistration registration) {
        OwnerKey owner = OwnerKey.from(registration);
        int remaining = ownerUsage.getOrDefault(owner, 0) - 1;
        if (remaining < 0) {
            throw new IllegalStateException("remote task-run registry owner usage underflow");
        }
        if (remaining == 0) {
            ownerUsage.remove(owner);
        } else {
            ownerUsage.put(owner, remaining);
        }
    }

    private void rejectCompetingNonTerminal(RemoteTaskRunRegistration candidate, String ignoredTaskRunId) {
        if (candidate.getStatus().isTerminal()) {
            return;
        }
        for (RegistryEntry entry : registrations.values()) {
            RemoteTaskRunRegistration existing = entry.registration;
            if (Objects.equals(existing.getTaskRunId(), ignoredTaskRunId) || existing.getStatus().isTerminal()) {
                continue;
            }
            if (existing.getTenantId().equals(candidate.getTenantId())
                    && existing.getUserId().equals(candidate.getUserId())
                    && existing.getDeviceId().equals(candidate.getDeviceId())
                    && existing.getWindowId().equals(candidate.getWindowId())) {
                throw new IllegalStateException(
                        "tenant/user/device/window already has a non-terminal remote task run");
            }
        }
    }

    private static RemoteTaskRunRegistration applyConfirmedTransition(
            RemoteTaskRunRegistration previous,
            RemoteTaskRunRegistration confirmed) {
        if (previous.equals(confirmed)) {
            return previous;
        }
        if (previous.getStatus().isTerminal()) {
            if (!previous.equals(confirmed)) {
                throw new IllegalStateException(
                        "terminal remote task run only accepts an identical idempotent snapshot");
            }
            return previous;
        }
        if (confirmed.getRunRevision() < previous.getRunRevision()) {
            throw new IllegalArgumentException("runRevision must not move backwards");
        }
        if (confirmed.getStatus().isTerminal()) {
            long expectedStopEpoch;
            if (confirmed.getStatus() == RemoteTaskRunStatus.STOPPED) {
                if (previous.getStopEpoch() == Long.MAX_VALUE) {
                    throw new IllegalStateException("confirmed STOPPED stopEpoch cannot overflow");
                }
                expectedStopEpoch = previous.getStopEpoch() + 1L;
            } else {
                expectedStopEpoch = previous.getStopEpoch();
            }
            if (confirmed.getStopEpoch() != expectedStopEpoch
                    || confirmed.getRunRevision() <= previous.getRunRevision()) {
                throw new IllegalArgumentException(
                        "terminal binding has unexpected stopEpoch or non-advancing runRevision");
            }
        } else if (confirmed.getStopEpoch() != previous.getStopEpoch()) {
            throw new IllegalArgumentException(
                    "non-terminal binding must keep the canonical stopEpoch");
        }
        if (confirmed.getStatus() != previous.getStatus()
                && confirmed.getRunRevision() == previous.getRunRevision()) {
            throw new IllegalArgumentException("status change requires a newer runRevision");
        }
        if (!transitionAllowed(previous.getStatus(), confirmed.getStatus())) {
            throw new IllegalStateException(
                    "invalid remote task run transition " + previous.getStatus() + " -> " + confirmed.getStatus());
        }
        return confirmed;
    }

    static boolean transitionAllowed(RemoteTaskRunStatus from, RemoteTaskRunStatus to) {
        if (from == to) {
            return true;
        }
        return switch (from) {
            case PREPARED -> to == RemoteTaskRunStatus.ACTIVE
                    || to == RemoteTaskRunStatus.STOPPING
                    || to == RemoteTaskRunStatus.STOPPED
                    || to == RemoteTaskRunStatus.COMPLETED;
            case ACTIVE -> to == RemoteTaskRunStatus.PAUSED
                    || to == RemoteTaskRunStatus.STOPPING
                    || to == RemoteTaskRunStatus.STOPPED
                    || to == RemoteTaskRunStatus.COMPLETED;
            case PAUSED -> to == RemoteTaskRunStatus.ACTIVE
                    || to == RemoteTaskRunStatus.STOPPING
                    || to == RemoteTaskRunStatus.STOPPED
                    || to == RemoteTaskRunStatus.COMPLETED;
            case STOPPING -> to == RemoteTaskRunStatus.STOPPED || to == RemoteTaskRunStatus.COMPLETED;
            case STOPPED, COMPLETED -> false;
        };
    }

    private void publishTransition(
            RegistryEntry entry,
            RemoteTaskRunRegistration previous,
            RemoteTaskRunRegistration current) {
        PreparedEntryTransition prepared = prepareEntryTransition(entry, previous, current);
        applyPreparedWithoutToken(entry, prepared);
        if (prepared.tokenAction() == TokenAction.REQUEST_PAUSE) {
            entry.pauseToken.requestPause(prepared.tokenReason());
        } else if (prepared.tokenAction() == TokenAction.RESUME) {
            entry.pauseToken.resume();
        }
    }

    private static void applyPreparedWithoutToken(
            RegistryEntry entry,
        PreparedEntryTransition prepared) {
        entry.nextSlotGeneration = prepared.nextSlotGeneration();
        if (prepared.tokenAction() == TokenAction.RESUME
                && prepared.registration().getStatus() == RemoteTaskRunStatus.ACTIVE
                && prepared.pendingReadiness() != null) {
            entry.registration = prepared.registration();
            entry.pendingReadiness = prepared.pendingReadiness();
        } else {
            entry.pendingReadiness = prepared.pendingReadiness();
            entry.registration = prepared.registration();
        }
        if (prepared.handle() != null && prepared.nextSnapshot() != null) {
            if (prepared.tokenAction() == TokenAction.RESUME
                    && prepared.registration().getStatus() == RemoteTaskRunStatus.ACTIVE
                    && prepared.nextSnapshot() instanceof CallbackActiveSnapshot
                    && prepared.handle().generic) {
                prepared.handle().genericBindingGeneration = Math.incrementExact(
                        prepared.handle().genericBindingGeneration);
            }
            prepared.handle().currentSnapshot = prepared.nextSnapshot();
        }
        if (prepared.clearHandle()
                && entry.inFlightExclusiveHandle == prepared.handle()) {
            entry.inFlightExclusiveHandle = null;
        }
        entry.nextContinuationGeneration = prepared.nextContinuationGeneration();
    }

    private PreparedEntryTransition prepareEntryTransition(
            RegistryEntry entry,
            RemoteTaskRunRegistration previous,
            RemoteTaskRunRegistration current) {
        RemoteTaskRunRegistration target = Objects.requireNonNull(current, "current");
        PendingExecutorReadiness readiness = entry.pendingReadiness;
        long slotGeneration = entry.nextSlotGeneration;
        InFlightExclusiveHandle handle = entry.inFlightExclusiveHandle;
        ContinuationSnapshot nextSnapshot = null;
        boolean clearHandle = false;
        long continuationGeneration = entry.nextContinuationGeneration;
        TokenAction tokenAction = TokenAction.NONE;
        String tokenReason = null;

        if (target.getStatus() == RemoteTaskRunStatus.PAUSED) {
            readiness = null;
            tokenAction = TokenAction.REQUEST_PAUSE;
            tokenReason = "cloud-confirmed pause revision=" + target.getRunRevision();
            if (handle != null && previous != target) {
                continuationGeneration = Math.incrementExact(continuationGeneration);
                ContinuationSnapshot observed = handle.currentSnapshot;
                if (previous != null
                        && previous.getStatus() == RemoteTaskRunStatus.ACTIVE
                        && observed instanceof CallbackActiveSnapshot active
                        && active.registration() == previous
                        && active.identity().matches(previous)) {
                    nextSnapshot = new CallbackPausedSnapshot(
                            handle.identity,
                            target,
                            continuationGeneration,
                            previous.getRunRevision());
                } else {
                    String reason = observed instanceof QueuedActiveSnapshot
                            ? "PAUSED_BEFORE_ADMISSION"
                            : "STALE_ACTIVE_PREDECESSOR";
                    nextSnapshot = new InvalidatedSnapshot(
                            handle.identity,
                            target,
                            continuationGeneration,
                            reason);
                    clearHandle = true;
                }
            }
        } else if (previous != null
                && previous.getStatus() == RemoteTaskRunStatus.PAUSED
                && target.getStatus() == RemoteTaskRunStatus.ACTIVE
                && target.getRunRevision() > previous.getRunRevision()) {
            slotGeneration = Math.incrementExact(slotGeneration);
            readiness = new PendingExecutorReadiness(
                    slotGeneration, previous.getRunRevision(), target.getRunRevision());
            tokenAction = TokenAction.RESUME;
            if (handle != null) {
                continuationGeneration = Math.incrementExact(continuationGeneration);
                ContinuationSnapshot observed = handle.currentSnapshot;
                if (observed instanceof CallbackPausedSnapshot paused
                        && paused.registration() == previous
                        && paused.identity().matches(previous)) {
                    nextSnapshot = new CallbackActiveSnapshot(
                            handle.identity,
                            target,
                            continuationGeneration,
                            previous.getRunRevision(),
                            slotGeneration);
                } else {
                    nextSnapshot = new InvalidatedSnapshot(
                            handle.identity,
                            target,
                            continuationGeneration,
                            "STALE_PAUSED_PREDECESSOR");
                    clearHandle = true;
                }
            }
        } else if (target.getStatus() == RemoteTaskRunStatus.STOPPING
                || target.getStatus().isTerminal()) {
            readiness = null;
            tokenAction = TokenAction.RESUME;
            if (handle != null && !(handle.currentSnapshot instanceof ClosedSnapshot)) {
                continuationGeneration = Math.incrementExact(continuationGeneration);
                nextSnapshot = new InvalidatedSnapshot(
                        handle.identity,
                        target,
                        continuationGeneration,
                        target.getStatus() == RemoteTaskRunStatus.STOPPING
                                ? "STOPPING" : "TERMINAL");
                clearHandle = true;
            }
        } else if (handle != null && previous != target) {
            continuationGeneration = Math.incrementExact(continuationGeneration);
            nextSnapshot = new InvalidatedSnapshot(
                    handle.identity,
                    target,
                    continuationGeneration,
                    "STALE_REGISTRATION_TRANSITION");
            clearHandle = true;
        }

        return new PreparedEntryTransition(
                target,
                readiness,
                slotGeneration,
                handle,
                nextSnapshot,
                clearHandle,
                continuationGeneration,
                tokenAction,
                tokenReason);
    }

    private static RemoteTaskRunRegistration normalizeAndValidate(RemoteTaskRunRegistration registration) {
        if (registration == null) {
            throw new IllegalArgumentException("registration is required");
        }
        if (registration.getProcessId() <= 0L) {
            throw new IllegalArgumentException("processId must be positive");
        }
        if (registration.getPlayerIdentityEpoch() < 0L
                || registration.getStopEpoch() < 0L
                || registration.getRunRevision() < 0L) {
            throw new IllegalArgumentException("epochs and runRevision must not be negative");
        }
        if (registration.getStatus() == null) {
            throw new IllegalArgumentException("status is required");
        }
        String nativeHandle = requireText(registration.getNativeHandle(), "nativeHandle");
        if (!nativeHandle.chars().allMatch(Character::isDigit)) {
            throw new IllegalArgumentException("nativeHandle must be an unsigned decimal string");
        }
        return registration.toBuilder()
                .tenantId(requireText(registration.getTenantId(), "tenantId"))
                .userId(requireText(registration.getUserId(), "userId"))
                .deviceId(requireText(registration.getDeviceId(), "deviceId"))
                .clientSessionId(requireText(registration.getClientSessionId(), "clientSessionId"))
                .taskRunId(requireText(registration.getTaskRunId(), "taskRunId"))
                .startRequestId(requireText(registration.getStartRequestId(), "startRequestId"))
                .taskType(requireOriginalText(registration.getTaskType(), "taskType"))
                .windowId(requireText(registration.getWindowId(), "windowId"))
                .nativeHandle(nativeHandle)
                .build();
    }

    private static void validateClientSession(RemoteClientSessionRef clientSession) {
        if (clientSession == null) {
            throw new IllegalArgumentException("clientSession is required");
        }
        requireText(clientSession.getTenantId(), "tenantId");
        requireText(clientSession.getUserId(), "userId");
        requireText(clientSession.getDeviceId(), "deviceId");
        requireText(clientSession.getClientSessionId(), "clientSessionId");
    }

    private static void requireSameBinding(
            RemoteTaskRunRegistration left,
            RemoteTaskRunRegistration right) {
        if (!left.getTenantId().equals(right.getTenantId())
                || !left.getUserId().equals(right.getUserId())
                || !left.getDeviceId().equals(right.getDeviceId())
                || !left.getClientSessionId().equals(right.getClientSessionId())
                || !left.getTaskRunId().equals(right.getTaskRunId())
                || !left.getStartRequestId().equals(right.getStartRequestId())
                || !left.getTaskType().equals(right.getTaskType())
                || !left.getWindowId().equals(right.getWindowId())
                || !left.getNativeHandle().equals(right.getNativeHandle())
                || left.getProcessId() != right.getProcessId()
                || left.getPlayerIdentityEpoch() != right.getPlayerIdentityEpoch()) {
            throw new IllegalStateException("taskRunId is already registered to another scope or binding");
        }
    }

    private static void requireMatches(
            RemoteTaskRunRegistration registration,
            RemoteClientSessionRef clientSession,
            String windowId) {
        if (!matches(registration, clientSession, windowId)) {
            throw new IllegalStateException("task run scope or window mismatch");
        }
    }

    private static boolean matches(
            RemoteTaskRunRegistration registration,
            RemoteClientSessionRef clientSession,
            String windowId) {
        return registration.getTenantId().equals(clientSession.getTenantId().trim())
                && registration.getUserId().equals(clientSession.getUserId().trim())
                && registration.getDeviceId().equals(clientSession.getDeviceId().trim())
                && registration.getClientSessionId().equals(clientSession.getClientSessionId().trim())
                && registration.getWindowId().equals(windowId.trim());
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value.trim();
    }

    private static String requireOriginalText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value;
    }

    private static final class RegistryEntry {
        private volatile RemoteTaskRunRegistration registration;
        private final TaskPauseToken pauseToken;
        private final long entryGeneration;
        private long nextSlotGeneration;
        private long nextContinuationGeneration;
        private PendingExecutorReadiness pendingReadiness;
        private InFlightExclusiveHandle inFlightExclusiveHandle;

        private RegistryEntry(
                RemoteTaskRunRegistration registration,
                TaskPauseToken pauseToken,
                long entryGeneration) {
            this.registration = registration;
            this.pauseToken = pauseToken;
            this.entryGeneration = entryGeneration;
        }
    }

    private record PreparedEntryTransition(
            RemoteTaskRunRegistration registration,
            PendingExecutorReadiness pendingReadiness,
            long nextSlotGeneration,
            InFlightExclusiveHandle handle,
            ContinuationSnapshot nextSnapshot,
            boolean clearHandle,
            long nextContinuationGeneration,
            TokenAction tokenAction,
            String tokenReason) {

        private PreparedEntryTransition {
            Objects.requireNonNull(registration, "registration");
            Objects.requireNonNull(tokenAction, "tokenAction");
            if (nextSlotGeneration < 0L || nextContinuationGeneration < 0L) {
                throw new IllegalArgumentException(
                        "prepared registry generations must not be negative");
            }
            if (nextSnapshot != null && handle == null) {
                throw new IllegalArgumentException(
                        "prepared continuation snapshot requires its exact handle");
            }
            if (clearHandle && (handle == null || nextSnapshot == null)) {
                throw new IllegalArgumentException(
                        "prepared handle clear requires an invalidated snapshot");
            }
            if (tokenAction == TokenAction.REQUEST_PAUSE
                    && (tokenReason == null || tokenReason.isBlank())) {
                throw new IllegalArgumentException(
                        "pause publication requires a diagnostic token reason");
            }
            if (tokenAction != TokenAction.REQUEST_PAUSE && tokenReason != null) {
                throw new IllegalArgumentException(
                        "only pause publication may carry a token reason");
            }
        }
    }

    private enum TokenAction {
        NONE,
        REQUEST_PAUSE,
        RESUME
    }

    enum InFlightExclusiveCheck {
        CLEAR,
        PAUSED,
        STOPPED,
        MISMATCH
    }

    sealed interface ContinuationSnapshot permits QueuedActiveSnapshot,
            CallbackActiveSnapshot, CallbackPausedSnapshot, InvalidatedSnapshot, ClosedSnapshot {

        ContinuationIdentity identity();

        RemoteTaskRunRegistration registration();

        long localTransitionGeneration();
    }

    record QueuedActiveSnapshot(
            ContinuationIdentity identity,
            RemoteTaskRunRegistration registration,
            long localTransitionGeneration) implements ContinuationSnapshot {

        QueuedActiveSnapshot {
            Objects.requireNonNull(identity, "identity");
            Objects.requireNonNull(registration, "registration");
            if (localTransitionGeneration <= 0L
                    || registration.getStatus() != RemoteTaskRunStatus.ACTIVE
                    || !identity.matches(registration)) {
                throw new IllegalArgumentException("invalid queued ACTIVE continuation snapshot");
            }
        }

        boolean matches(RemoteGameCommand command) {
            return identity.matches(command, registration);
        }
    }

    record CallbackActiveSnapshot(
            ContinuationIdentity identity,
            RemoteTaskRunRegistration registration,
            long localTransitionGeneration,
            long resumedFromPausedRevision,
            long readinessSlotGeneration) implements ContinuationSnapshot {

        CallbackActiveSnapshot {
            Objects.requireNonNull(identity, "identity");
            Objects.requireNonNull(registration, "registration");
            if (localTransitionGeneration <= 0L
                    || registration.getStatus() != RemoteTaskRunStatus.ACTIVE
                    || !identity.matches(registration)
                    || (resumedFromPausedRevision < 0L) != (readinessSlotGeneration < 0L)
                    || (resumedFromPausedRevision >= 0L
                            && resumedFromPausedRevision >= registration.getRunRevision())) {
                throw new IllegalArgumentException("invalid callback ACTIVE continuation snapshot");
            }
        }
    }

    record CallbackPausedSnapshot(
            ContinuationIdentity identity,
            RemoteTaskRunRegistration registration,
            long localTransitionGeneration,
            long pausedFromActiveRevision) implements ContinuationSnapshot {

        CallbackPausedSnapshot {
            Objects.requireNonNull(identity, "identity");
            Objects.requireNonNull(registration, "registration");
            if (localTransitionGeneration <= 0L
                    || registration.getStatus() != RemoteTaskRunStatus.PAUSED
                    || !identity.matches(registration)
                    || pausedFromActiveRevision < 0L
                    || pausedFromActiveRevision >= registration.getRunRevision()) {
                throw new IllegalArgumentException("invalid callback PAUSED continuation snapshot");
            }
        }
    }

    record InvalidatedSnapshot(
            ContinuationIdentity identity,
            RemoteTaskRunRegistration registration,
            long localTransitionGeneration,
            String reason) implements ContinuationSnapshot {

        InvalidatedSnapshot {
            Objects.requireNonNull(identity, "identity");
            Objects.requireNonNull(registration, "registration");
            reason = requireText(reason, "reason");
            if (localTransitionGeneration <= 0L) {
                throw new IllegalArgumentException(
                        "invalidated continuation generation must be positive");
            }
        }
    }

    record ClosedSnapshot(
            ContinuationIdentity identity,
            RemoteTaskRunRegistration registration,
            long localTransitionGeneration) implements ContinuationSnapshot {

        ClosedSnapshot {
            Objects.requireNonNull(identity, "identity");
            Objects.requireNonNull(registration, "registration");
            if (localTransitionGeneration <= 0L) {
                throw new IllegalArgumentException(
                        "closed continuation generation must be positive");
            }
        }
    }

    static final class InFlightExclusiveHandle {
        private final RemoteTaskRunRegistry owner;
        private final RegistryEntry entry;
        private final ContinuationIdentity identity;
        private volatile ContinuationSnapshot currentSnapshot;
        private boolean generic;
        private long genericBindingGeneration;
        private long genericNextStep;
        private long genericBoundStepGeneration = -1L;
        private long genericBoundStep = -1L;
        private long genericLastCompletedStepGeneration = -1L;
        private long genericLastCompletedStep = -1L;
        private String genericBoundRequestId;
        private String genericBoundRequestDigest;
        private InputActionQueue.RetainedSessionHandle inputSession;
        private InputActionExecutionResult genericTerminalSnapshot;

        private InFlightExclusiveHandle(
                RemoteTaskRunRegistry owner,
                RegistryEntry entry,
                ContinuationIdentity identity,
                ContinuationSnapshot currentSnapshot) {
            this.owner = Objects.requireNonNull(owner, "owner");
            this.entry = Objects.requireNonNull(entry, "entry");
            this.identity = Objects.requireNonNull(identity, "identity");
            this.currentSnapshot = Objects.requireNonNull(currentSnapshot, "currentSnapshot");
        }
    }

    private record ContinuationIdentity(
            RemoteTaskRunRegistry owner,
            RegistryEntry entry,
            long entryGeneration,
            TaskPauseToken pauseToken,
            String tenantId,
            String userId,
            String deviceId,
            String clientSessionId,
            String taskRunId,
            String taskType,
            String windowId,
            String nativeHandle,
            long processId,
            long playerIdentityEpoch,
            long stopEpoch,
            String exclusiveSessionId) {

        private ContinuationIdentity {
            Objects.requireNonNull(owner, "owner");
            Objects.requireNonNull(entry, "entry");
            Objects.requireNonNull(pauseToken, "pauseToken");
            tenantId = requireText(tenantId, "tenantId");
            userId = requireText(userId, "userId");
            deviceId = requireText(deviceId, "deviceId");
            clientSessionId = requireText(clientSessionId, "clientSessionId");
            taskRunId = requireText(taskRunId, "taskRunId");
            taskType = requireOriginalText(taskType, "taskType");
            windowId = requireText(windowId, "windowId");
            nativeHandle = requireText(nativeHandle, "nativeHandle");
            exclusiveSessionId = requireText(exclusiveSessionId, "exclusiveSessionId");
            if (entryGeneration <= 0L || processId <= 0L
                    || playerIdentityEpoch < 0L || stopEpoch < 0L) {
                throw new IllegalArgumentException("invalid continuation identity generations");
            }
        }

        private boolean matches(RemoteTaskRunRegistration registration) {
            return registration != null
                    && owner.registrations.get(taskRunId) == entry
                    && entry.entryGeneration == entryGeneration
                    && entry.pauseToken == pauseToken
                    && tenantId.equals(registration.getTenantId())
                    && userId.equals(registration.getUserId())
                    && deviceId.equals(registration.getDeviceId())
                    && clientSessionId.equals(registration.getClientSessionId())
                    && taskRunId.equals(registration.getTaskRunId())
                    && taskType.equals(registration.getTaskType())
                    && windowId.equals(registration.getWindowId())
                    && nativeHandle.equals(registration.getNativeHandle())
                    && processId == registration.getProcessId()
                    && playerIdentityEpoch == registration.getPlayerIdentityEpoch()
                    && stopEpoch == registration.getStopEpoch();
        }

        private boolean matches(
                RemoteGameCommand command,
                RemoteTaskRunRegistration registration) {
            if (!matches(registration) || command == null || command.getRunRevision() == null
                    || command.getWindow() == null || command.getStop() == null) {
                return false;
            }
            RemoteWindowBindingRef window = command.getWindow();
            return taskRunId.equals(command.getTaskRunId())
                    && windowId.equals(window.getWindowId())
                    && nativeHandle.equals(window.getNativeHandle())
                    && processId == window.getProcessId()
                    && playerIdentityEpoch == window.getPlayerIdentityEpoch()
                    && taskRunId.equals(command.getStop().getTaskRunId())
                    && stopEpoch == command.getStop().getStopEpoch()
                    && registration.getRunRevision() == command.getRunRevision();
        }
    }

    /** Non-mintable immutable provenance passed into the operation ledger admission gate. */
    static final class CommandAdmissionSnapshot {
        private final RemoteTaskRunRegistry owner;
        private final RegistryEntry entry;
        private final long entryGeneration;
        private final RemoteTaskRunRegistration registration;
        private final RemoteClientSessionRef clientSession;

        private CommandAdmissionSnapshot(
                RemoteTaskRunRegistry owner,
                RegistryEntry entry,
                long entryGeneration,
                RemoteTaskRunRegistration registration,
                RemoteClientSessionRef clientSession) {
            this.owner = owner;
            this.entry = entry;
            this.entryGeneration = entryGeneration;
            this.registration = registration;
            this.clientSession = clientSession;
        }

        RemoteTaskRunRegistration registration() {
            return registration;
        }

        boolean mintedBy(RemoteTaskRunRegistry expectedOwner) {
            return owner == expectedOwner;
        }

        boolean matches(RemoteGameCommand command) {
            if (command == null || command.getWindow() == null || command.getStop() == null
                    || command.getRunRevision() == null) {
                return false;
            }
            RemoteWindowBindingRef window = command.getWindow();
            boolean expectedStatus = command.getObservationMode() == null
                    ? registration.getStatus() == RemoteTaskRunStatus.ACTIVE
                    : command.getObservationMode() == RemoteObservationMode.PAUSED_READ_ONLY
                            && registration.getStatus() == RemoteTaskRunStatus.PAUSED;
            return expectedStatus
                    && Objects.equals(registration.getTenantId(), clientSession.getTenantId())
                    && Objects.equals(registration.getUserId(), clientSession.getUserId())
                    && Objects.equals(registration.getDeviceId(), clientSession.getDeviceId())
                    && Objects.equals(registration.getClientSessionId(),
                            clientSession.getClientSessionId())
                    && Objects.equals(registration.getTaskRunId(), command.getTaskRunId())
                    && Objects.equals(registration.getWindowId(), window.getWindowId())
                    && Objects.equals(registration.getNativeHandle(), window.getNativeHandle())
                    && registration.getProcessId() == window.getProcessId()
                    && registration.getPlayerIdentityEpoch() == window.getPlayerIdentityEpoch()
                    && Objects.equals(registration.getTaskRunId(), command.getStop().getTaskRunId())
                    && registration.getStopEpoch() == command.getStop().getStopEpoch()
                    && registration.getRunRevision() == command.getRunRevision();
        }
    }

    record TerminalCleanupObservation(
            String taskRunId,
            TerminalCleanupStatus status,
            RemoteTaskRunRegistration terminalRegistration) {

        TerminalCleanupObservation {
            Objects.requireNonNull(taskRunId, "taskRunId");
            Objects.requireNonNull(status, "status");
            if ((status == TerminalCleanupStatus.TERMINAL)
                    != (terminalRegistration != null)) {
                throw new IllegalArgumentException(
                        "only TERMINAL cleanup observation carries a registration");
            }
        }

        boolean cleanupAllowed() {
            return status == TerminalCleanupStatus.TERMINAL
                    || status == TerminalCleanupStatus.ABSENT;
        }
    }

    private enum TerminalCleanupStatus {
        TERMINAL,
        ABSENT,
        NOT_READY
    }

    private record OwnerKey(String tenantId, String userId, String deviceId) {

        private static OwnerKey from(RemoteTaskRunRegistration registration) {
            return new OwnerKey(
                    registration.getTenantId(),
                    registration.getUserId(),
                    registration.getDeviceId());
        }
    }

    public static final class CapacityExceededException extends IllegalStateException {

        private static final long serialVersionUID = 1L;

        private CapacityExceededException(String message) {
            super(message);
        }
    }
}
