package com.bot.dhxy.cloud.remote;

import java.util.HashMap;
import java.util.HashSet;
import java.util.ArrayDeque;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/** Thread-safe in-memory request and input-action idempotency ledger. */
public final class RemoteOperationLedger {

    private static final int MAX_SEMANTIC_SLOTS = 1_000;
    private static final int MAX_CURRENT_DETAILS = 64;
    private static final int MAX_RECEIPT_OUTBOX = 64;

    private final Object monitor = new Object();
    private final Map<RequestKey, LedgerEntry> requests = new HashMap<>();
    // Terminal visibility for quiescence reads: marked in the SAME monitor transition that
    // advances ledgerRevision, so a snapshot can never observe done=true with a stale revision.
    private final Set<RequestKey> terminalRequests = new HashSet<>();
    private final Map<ActionKey, InputActionIdentity> inputActions = new HashMap<>();
    private final Map<String, RunState> runStates = new LinkedHashMap<>();
    private final ArrayDeque<TerminalCleanupCandidate> terminalCleanupCandidates =
            new ArrayDeque<>();
    private final Set<String> queuedCleanupRuns = new HashSet<>();
    private final Map<String, ReceiptOutboxEntry> receiptOutbox = new LinkedHashMap<>();
    private RemoteTaskRunScope boundScope;
    private String cloudIncarnationId;
    private boolean coordinatedRestartRequired;
    private long nextReceiptGeneration;
    private long ledgerRevision;
    private int semanticSlotCount;
    private int currentDetailCount;

    void bindSession(RemoteTaskRunScope scope) {
        if (scope == null) {
            throw new IllegalArgumentException("scope is required");
        }
        synchronized (monitor) {
            if (boundScope != null && !boundScope.equals(scope)) {
                throw new IllegalStateException("operation ledger is already bound to another session");
            }
            boundScope = scope;
        }
    }

    /** Binds this process-local ledger to one cloud process incarnation exactly once. */
    void bindCloudIncarnation(String incarnationId) {
        if (incarnationId == null || incarnationId.isBlank()) {
            throw new IllegalArgumentException("cloudIncarnationId must not be blank");
        }
        if (!incarnationId.equals(incarnationId.trim())) {
            throw new IllegalArgumentException("cloudIncarnationId must be canonical");
        }
        String canonical = incarnationId;
        synchronized (monitor) {
            if (coordinatedRestartRequired) {
                throw new IllegalStateException("coordinated restart is already required");
            }
            if (cloudIncarnationId == null) {
                cloudIncarnationId = canonical;
                ledgerRevision = Math.incrementExact(ledgerRevision);
                return;
            }
            if (!cloudIncarnationId.equals(canonical)) {
                coordinatedRestartRequired = true;
                ledgerRevision = Math.incrementExact(ledgerRevision);
                throw new IllegalStateException(
                        "cloud incarnation changed; coordinated restart is required");
            }
        }
    }

    boolean coordinatedRestartRequired() {
        synchronized (monitor) {
            return coordinatedRestartRequired;
        }
    }

    /**
     * Atomically claims a request and, for input bundles, its window-scoped action id.
     *
     * @param command validated command envelope
     * @return OWNER, DUPLICATE, IDEMPOTENCY_CONFLICT, or ACTION_ID_REUSE claim
     */
    public Claim claim(
            RemoteGameCommand command,
            RemoteTaskRunRegistry.CommandAdmissionSnapshot admissionSnapshot) {
        if (command == null || command.getOperation() == null) {
            throw new IllegalArgumentException("command and operation are required");
        }
        RequestKey key = new RequestKey(command.getOperation(), command.getRequestId());
        synchronized (monitor) {
            if (coordinatedRestartRequired) {
                return new Claim(ClaimStatus.COORDINATED_RESTART_REQUIRED, key, null);
            }
            LedgerEntry existing = requests.get(key);
            if (existing != null) {
                if (!existing.requestDigest().equals(command.getRequestDigest())) {
                    return new Claim(ClaimStatus.IDEMPOTENCY_CONFLICT, key, null);
                }
                return new Claim(ClaimStatus.DUPLICATE, key, existing.outcome());
            }

            if (boundScope == null || admissionSnapshot == null
                    || command.getWindow() == null || command.getStop() == null
                    || command.getRunRevision() == null || command.getSemanticAddress() == null) {
                return new Claim(ClaimStatus.TASK_RUN_MISMATCH, key, null);
            }
            RemoteTaskRunRegistration registration = admissionSnapshot.registration();
            if (!sameScopeAndRun(command, registration, boundScope)) {
                return new Claim(ClaimStatus.TASK_RUN_MISMATCH, key, null);
            }
            if (!sameWindowAndRevision(command, registration)
                    || !admissionSnapshot.matches(command)) {
                return new Claim(ClaimStatus.WRONG_WINDOW, key, null);
            }

            SemanticSlotKey slotKey = SemanticSlotKey.from(command, registration);
            RunState runState = runStates.get(command.getTaskRunId());
            if (runState != null && !runState.matches(registration)) {
                return new Claim(ClaimStatus.TASK_RUN_MISMATCH, key, null);
            }
            SemanticFrontier frontier = runState == null
                    ? null : runState.semanticFrontiers.get(slotKey);
            boolean sparseOccurrences = usesSparseTerminalControlOccurrences(command);
            boolean newSlot = frontier == null;
            if (newSlot) {
                if (semanticSlotCount >= MAX_SEMANTIC_SLOTS) {
                    return new Claim(ClaimStatus.CAPACITY_EXCEEDED, key, null);
                }
                frontier = new SemanticFrontier(
                        registration.getTaskType(), sparseOccurrences);
            } else if (frontier.sparseOccurrences != sparseOccurrences) {
                return new Claim(ClaimStatus.SEMANTIC_CONFLICT, key, null);
            }
            AddressAdmission addressAdmission = frontier.classify(command.getSemanticAddress());
            if (addressAdmission == AddressAdmission.BELOW_FRONTIER) {
                return new Claim(ClaimStatus.FINAL_CONSUMED, key, null);
            }
            if (addressAdmission != AddressAdmission.NEXT_ALLOWED
                    || frontier.currentDetail != null) {
                return new Claim(ClaimStatus.SEMANTIC_CONFLICT, key, null);
            }
            if (currentDetailCount >= MAX_CURRENT_DETAILS) {
                return new Claim(ClaimStatus.CAPACITY_EXCEEDED, key, null);
            }

            if (isExclusiveInputOperation(command.getOperation())) {
                if (command.getWindow() == null
                        || command.getWindow().getWindowId() == null
                        || command.getWindow().getWindowId().isBlank()) {
                    throw new IllegalArgumentException("input command windowId is required");
                }
                ActionKey actionKey = new ActionKey(
                        command.getWindow().getWindowId(), command.getActionId());
                InputActionIdentity requested = new InputActionIdentity(
                        command.getRequestId(), command.getRequestDigest());
                InputActionIdentity reserved = inputActions.get(actionKey);
                if (reserved != null && !reserved.equals(requested)) {
                    return new Claim(ClaimStatus.ACTION_ID_REUSE, key, null);
                }
                inputActions.put(actionKey, requested);
            }

            CompletableFuture<RemoteGameOutcomeEnvelope> outcome = new CompletableFuture<>();
            SemanticDetailKey detailKey = new SemanticDetailKey(
                    slotKey, command.getSemanticAddress());
            LedgerEntry ledgerEntry = new LedgerEntry(command.getRequestDigest(), outcome, boundScope,
                    command.getTaskRunId(), command.getWindow(), command.getStop().getStopEpoch(),
                    command.getRunRevision(), command.getOperation(), detailKey);
            SemanticDetail detail = new SemanticDetail(
                    detailKey, key, ledgerEntry, command.getObservationMode(), registration);
            if (runState == null) {
                runState = new RunState(registration);
                runStates.put(command.getTaskRunId(), runState);
            }
            requests.put(key, ledgerEntry);
            runState.currentDetails.put(detailKey, detail);
            runState.currentDetailCount = Math.incrementExact(runState.currentDetailCount);
            runState.generation = Math.incrementExact(runState.generation);
            currentDetailCount = Math.incrementExact(currentDetailCount);
            frontier.currentDetail = detailKey;
            if (newSlot) {
                runState.semanticFrontiers.put(slotKey, frontier);
                semanticSlotCount = Math.incrementExact(semanticSlotCount);
            }
            ledgerRevision = Math.incrementExact(ledgerRevision);
            return new Claim(ClaimStatus.OWNER, key, outcome);
        }
    }

    /** Completes the shared terminal outcome exactly once for an OWNER claim. */
    public void complete(Claim claim, RemoteGameOutcomeEnvelope outcome) {
        if (claim == null || claim.status != ClaimStatus.OWNER || claim.outcome == null) {
            throw new IllegalArgumentException("only an OWNER claim can be completed");
        }
        if (outcome == null) {
            throw new IllegalArgumentException("terminal outcome is required");
        }
        /*
         * Terminal publication and ledgerRevision advance are ONE monitor-guarded transition.
         * The waiter-visible future completes afterwards, outside the monitor, so waiters keep
         * the existing wake-up semantics while a concurrent quiescence snapshot can never see
         * this entry as terminal while still carrying the pre-transition revision.
         */
        synchronized (monitor) {
            LedgerEntry entry = requests.get(claim.requestKey);
            if (entry == null || entry.outcome() != claim.outcome) {
                throw new IllegalStateException("ledger owner claim is no longer current");
            }
            RunState runState = runStates.get(entry.taskRunId());
            SemanticDetail detail = runState == null
                    ? null : runState.currentDetails.get(entry.detailKey());
            if (detail == null || detail.ledgerEntry != entry
                    || !entry.detailKey().semanticAddress().equals(outcome.getSemanticAddress())
                    || !entry.requestDigest().equals(outcome.getRequestDigest())) {
                throw new IllegalStateException(
                        "terminal outcome does not match the current semantic detail");
            }
            if (!terminalRequests.add(claim.requestKey)) {
                throw new IllegalStateException("ledger owner outcome is already complete");
            }
            detail.terminalOutcome = outcome;
            ledgerRevision = Math.incrementExact(ledgerRevision);
        }
        if (!claim.outcome.complete(outcome)) {
            throw new IllegalStateException("ledger owner outcome is already complete");
        }
    }

    /**
     * Applies one exact cloud final-consumed acknowledgement and atomically publishes its receipt.
     * No callback, network operation, registry lookup, or window access runs under this monitor.
     */
    RemoteFinalConsumedReceipt applyFinalConsumedAck(RemoteFinalConsumedAck acknowledgement) {
        if (acknowledgement == null) {
            throw new IllegalArgumentException("final-consumed acknowledgement is required");
        }
        synchronized (monitor) {
            if (coordinatedRestartRequired) {
                throw new IllegalStateException("coordinated restart is required");
            }
            RemoteProtocolDigests digests = new RemoteProtocolDigests();
            if (!digests.finalConsumedAckDigestMatches(acknowledgement)) {
                throw new IllegalArgumentException("final-consumed ackDigest is invalid");
            }
            requireBoundAckScope(acknowledgement);
            RunState runState = runStates.get(acknowledgement.getTaskRunId());
            if (runState == null) {
                throw new IllegalStateException("final-consumed task run is unknown");
            }
            SemanticSlotKey slotKey = SemanticSlotKey.from(acknowledgement);
            SemanticFrontier frontier = runState.semanticFrontiers.get(slotKey);
            if (frontier == null) {
                throw new IllegalStateException("final-consumed semantic slot is unknown");
            }
            if (acknowledgement.getAckDigest().equals(frontier.lastAckDigest)
                    && frontier.lastReceipt != null) {
                return frontier.lastReceipt;
            }
            SemanticDetailKey detailKey = new SemanticDetailKey(
                    slotKey, acknowledgement.getSemanticAddress());
            SemanticDetail detail = runState.currentDetails.get(detailKey);
            if (detail == null || frontier.currentDetail == null
                    || !frontier.currentDetail.equals(detailKey)) {
                throw new IllegalStateException(
                        "final-consumed acknowledgement does not match a current detail");
            }
            requireExactAcknowledgement(detail, acknowledgement);
            if (receiptOutbox.size() >= MAX_RECEIPT_OUTBOX) {
                throw new IllegalStateException("final-consumed receipt outbox capacity is full");
            }
            ActionKey compactedActionKey = null;
            if (isExclusiveInputOperation(detail.ledgerEntry.operation())) {
                compactedActionKey = new ActionKey(
                        detail.registration.getWindowId(), detail.terminalOutcome.getActionId());
                InputActionIdentity retainedAction = inputActions.get(compactedActionKey);
                InputActionIdentity expectedAction = new InputActionIdentity(
                        detail.requestKey.requestId(), detail.ledgerEntry.requestDigest());
                if (!expectedAction.equals(retainedAction)) {
                    throw new IllegalStateException(
                            "final-consumed input action does not match the retained identity");
                }
            }

            long completedOccurrence = frontier.completedOccurrence;
            Long openOccurrence;
            int throughAttempt;
            if (acknowledgement.getDisposition()
                    == RemoteFinalConsumedAck.Disposition.OCCURRENCE_COMPLETE) {
                completedOccurrence = acknowledgement.getSemanticAddress().getOccurrence();
                openOccurrence = null;
                throughAttempt = -1;
            } else {
                openOccurrence = acknowledgement.getSemanticAddress().getOccurrence();
                throughAttempt = acknowledgement.getSemanticAddress().getAttempt();
            }

            RemoteFinalConsumedReceipt draft = RemoteFinalConsumedReceipt.builder()
                    .contractVersion(acknowledgement.getContractVersion())
                    .tenantId(acknowledgement.getTenantId())
                    .userId(acknowledgement.getUserId())
                    .deviceId(acknowledgement.getDeviceId())
                    .clientSessionId(acknowledgement.getClientSessionId())
                    .taskRunId(acknowledgement.getTaskRunId())
                    .semanticAddress(acknowledgement.getSemanticAddress())
                    .ackDigest(acknowledgement.getAckDigest())
                    .applyStatus(RemoteFinalConsumedReceipt.ApplyStatus.APPLIED)
                    .appliedCompletedOccurrence(completedOccurrence)
                    .appliedOpenOccurrence(openOccurrence)
                    .appliedThroughAttempt(throughAttempt)
                    .code(RemoteOutcomeCode.FINAL_CONSUMED)
                    .message("exact final outcome compacted locally")
                    .receiptDigest(RemoteProtocolDigests.ZERO_SHA256)
                    .build();
            RemoteFinalConsumedReceipt receipt = RemoteFinalConsumedReceipt.builder()
                    .contractVersion(draft.getContractVersion())
                    .tenantId(draft.getTenantId())
                    .userId(draft.getUserId())
                    .deviceId(draft.getDeviceId())
                    .clientSessionId(draft.getClientSessionId())
                    .taskRunId(draft.getTaskRunId())
                    .semanticAddress(draft.getSemanticAddress())
                    .ackDigest(draft.getAckDigest())
                    .applyStatus(draft.getApplyStatus())
                    .appliedCompletedOccurrence(draft.getAppliedCompletedOccurrence())
                    .appliedOpenOccurrence(draft.getAppliedOpenOccurrence())
                    .appliedThroughAttempt(draft.getAppliedThroughAttempt())
                    .code(draft.getCode())
                    .message(draft.getMessage())
                    .receiptDigest(digests.computeFinalConsumedReceiptDigest(draft))
                    .build();

            frontier.completedOccurrence = completedOccurrence;
            frontier.openOccurrence = openOccurrence;
            frontier.compactedThroughAttempt = throughAttempt;
            frontier.currentDetail = null;
            frontier.lastAckDigest = acknowledgement.getAckDigest();
            frontier.lastReceipt = receipt;
            runState.currentDetails.remove(detailKey);
            runState.currentDetailCount = Math.decrementExact(runState.currentDetailCount);
            runState.receiptOutboxCount = Math.incrementExact(runState.receiptOutboxCount);
            runState.generation = Math.incrementExact(runState.generation);
            currentDetailCount = Math.decrementExact(currentDetailCount);
            requests.remove(detail.requestKey, detail.ledgerEntry);
            terminalRequests.remove(detail.requestKey);
            if (compactedActionKey != null) {
                inputActions.remove(compactedActionKey);
            }
            receiptOutbox.put(receipt.getAckDigest(), new ReceiptOutboxEntry(
                    Math.incrementExact(nextReceiptGeneration), receipt));
            ledgerRevision = Math.incrementExact(ledgerRevision);
            return receipt;
        }
    }

    ReceiptSendHandle claimReadyReceipt() {
        synchronized (monitor) {
            for (ReceiptOutboxEntry entry : receiptOutbox.values()) {
                if (entry.state == ReceiptState.READY) {
                    entry.state = ReceiptState.SENDING;
                    return new ReceiptSendHandle(
                            entry.generation, entry.receipt.getAckDigest(), entry.receipt);
                }
            }
            return null;
        }
    }

    void markReceiptAccepted(
            ReceiptSendHandle handle,
            RemoteFinalConsumedReceiptAck acknowledgement) {
        synchronized (monitor) {
            ReceiptOutboxEntry entry = currentSendingReceipt(handle);
            if (entry == null || acknowledgement == null
                    || (acknowledgement.getStatus()
                            != RemoteFinalConsumedReceiptAck.Status.ACCEPTED_COMPACTED
                        && acknowledgement.getStatus()
                            != RemoteFinalConsumedReceiptAck.Status.DUPLICATE_COMPACTED)
                    || !entry.receipt.getAckDigest().equals(acknowledgement.getAckDigest())
                    || !entry.receipt.getReceiptDigest().equals(
                            acknowledgement.getReceiptDigest())) {
                throw new IllegalStateException(
                        "receipt acknowledgement does not match the exact sending handle");
            }
            RunState runState = runStates.get(entry.receipt.getTaskRunId());
            if (runState == null || runState.receiptOutboxCount <= 0) {
                throw new IllegalStateException(
                        "receipt acknowledgement has no retained task-run outbox count");
            }
            receiptOutbox.remove(entry.receipt.getAckDigest(), entry);
            runState.receiptOutboxCount = Math.decrementExact(runState.receiptOutboxCount);
            runState.generation = Math.incrementExact(runState.generation);
            enqueueTerminalCleanupCandidate(runState);
            ledgerRevision = Math.incrementExact(ledgerRevision);
        }
    }

    /** Retains uncertain/rejected delivery without scheduling an automatic retry. */
    void markReceiptDeliveryUncertain(ReceiptSendHandle handle) {
        synchronized (monitor) {
            ReceiptOutboxEntry entry = currentSendingReceipt(handle);
            if (entry != null) {
                entry.state = ReceiptState.READY;
                ledgerRevision = Math.incrementExact(ledgerRevision);
            }
        }
    }

    /** Retains a typed permanent rejection and prevents automatic redelivery. */
    void markReceiptPermanentRejected(ReceiptSendHandle handle) {
        synchronized (monitor) {
            ReceiptOutboxEntry entry = currentSendingReceipt(handle);
            if (entry != null) {
                entry.state = ReceiptState.REJECTED_RETAINED;
                ledgerRevision = Math.incrementExact(ledgerRevision);
            }
        }
    }

    TerminalCleanupCandidate claimTerminalCleanupCandidate() {
        synchronized (monitor) {
            TerminalCleanupCandidate candidate = terminalCleanupCandidates.pollFirst();
            if (candidate == null) {
                return null;
            }
            queuedCleanupRuns.remove(candidate.taskRunId());
            RunState runState = runStates.get(candidate.taskRunId());
            if (runState == null) {
                return null;
            }
            if (runState.generation != candidate.generation() || !runState.cleanupReady()) {
                enqueueTerminalCleanupCandidate(runState);
                return null;
            }
            return candidate;
        }
    }

    void deferTerminalCleanupCandidate(TerminalCleanupCandidate candidate) {
        if (candidate == null) {
            return;
        }
        synchronized (monitor) {
            RunState runState = runStates.get(candidate.taskRunId());
            if (runState != null && runState.generation == candidate.generation()
                    && runState.cleanupReady()) {
                enqueueTerminalCleanupCandidate(runState);
            }
        }
    }

    boolean commitTerminalCleanup(
            TerminalCleanupCandidate candidate,
            RemoteTaskRunRegistry.TerminalCleanupObservation observation) {
        if (candidate == null || observation == null
                || !candidate.taskRunId().equals(observation.taskRunId())
                || !observation.cleanupAllowed()) {
            return false;
        }
        synchronized (monitor) {
            RunState runState = runStates.get(candidate.taskRunId());
            if (runState == null || runState.generation != candidate.generation()
                    || !runState.cleanupReady()) {
                return false;
            }
            int nextSemanticSlotCount = Math.subtractExact(
                    semanticSlotCount, runState.semanticFrontiers.size());
            runStates.remove(candidate.taskRunId(), runState);
            queuedCleanupRuns.remove(candidate.taskRunId());
            semanticSlotCount = nextSemanticSlotCount;
            ledgerRevision = Math.incrementExact(ledgerRevision);
            return true;
        }
    }

    private void enqueueTerminalCleanupCandidate(RunState runState) {
        if (!runState.cleanupReady() || !queuedCleanupRuns.add(runState.taskRunId)) {
            return;
        }
        terminalCleanupCandidates.addLast(new TerminalCleanupCandidate(
                runState.taskRunId, runState.generation, runState.stableRegistration));
    }

    private ReceiptOutboxEntry currentSendingReceipt(ReceiptSendHandle handle) {
        if (handle == null) {
            return null;
        }
        ReceiptOutboxEntry entry = receiptOutbox.get(handle.ackDigest);
        return entry != null && entry.generation == handle.generation
                && entry.receipt == handle.receipt && entry.state == ReceiptState.SENDING
                ? entry : null;
    }

    private void requireBoundAckScope(RemoteFinalConsumedAck acknowledgement) {
        if (boundScope == null
                || !boundScope.getTenantId().equals(acknowledgement.getTenantId())
                || !boundScope.getUserId().equals(acknowledgement.getUserId())
                || !boundScope.getDeviceId().equals(acknowledgement.getDeviceId())
                || !boundScope.getClientSessionId().equals(
                        acknowledgement.getClientSessionId())) {
            throw new IllegalArgumentException(
                    "final-consumed acknowledgement scope does not match this ledger");
        }
    }

    private static void requireExactAcknowledgement(
            SemanticDetail detail,
            RemoteFinalConsumedAck acknowledgement) {
        RemoteGameOutcomeEnvelope outcome = detail.terminalOutcome;
        RemoteTaskRunRegistration registration = detail.registration;
        if (outcome == null || outcome.getExecutionState() == RemoteExecutionState.UNKNOWN
                || !detail.detailKey.semanticAddress().equals(
                        acknowledgement.getSemanticAddress())
                || registration.getRunRevision() != acknowledgement.getRunRevision()
                || !Objects.equals(registration.getWindowId(),
                        acknowledgement.getWindow().getWindowId())
                || !Objects.equals(registration.getNativeHandle(),
                        acknowledgement.getWindow().getNativeHandle())
                || registration.getProcessId() != acknowledgement.getWindow().getProcessId()
                || registration.getPlayerIdentityEpoch()
                        != acknowledgement.getWindow().getPlayerIdentityEpoch()
                || registration.getStopEpoch() != acknowledgement.getStopEpoch()
                || detail.ledgerEntry.operation() != acknowledgement.getOperation()
                || !detail.requestKey.requestId().equals(acknowledgement.getRequestId())
                || !Objects.equals(outcome.getActionId(), acknowledgement.getActionId())
                || !detail.ledgerEntry.requestDigest().equals(
                        acknowledgement.getRequestDigest())
                || !outcome.getOutcomeDigest().equals(acknowledgement.getOutcomeDigest())
                || outcome.getExecutionState().name().equals(
                        acknowledgement.getExecutionState().name()) == false
                || outcome.getCode().name().equals(acknowledgement.getOutcomeCode().name()) == false
                || detail.observationMode != acknowledgement.getObservationMode()) {
            throw new IllegalArgumentException(
                    "final-consumed acknowledgement does not match the retained terminal detail");
        }
    }

    private static boolean sameScopeAndRun(
            RemoteGameCommand command,
            RemoteTaskRunRegistration registration,
            RemoteTaskRunScope scope) {
        return registration != null
                && Objects.equals(registration.getTenantId(), scope.getTenantId())
                && Objects.equals(registration.getUserId(), scope.getUserId())
                && Objects.equals(registration.getDeviceId(), scope.getDeviceId())
                && Objects.equals(registration.getClientSessionId(), scope.getClientSessionId())
                && Objects.equals(registration.getTaskRunId(), command.getTaskRunId());
    }

    private static boolean sameWindowAndRevision(
            RemoteGameCommand command,
            RemoteTaskRunRegistration registration) {
        return Objects.equals(registration.getWindowId(), command.getWindow().getWindowId())
                && Objects.equals(registration.getNativeHandle(),
                        command.getWindow().getNativeHandle())
                && registration.getProcessId() == command.getWindow().getProcessId()
                && registration.getPlayerIdentityEpoch()
                        == command.getWindow().getPlayerIdentityEpoch()
                && registration.getStopEpoch() == command.getStop().getStopEpoch()
                && registration.getRunRevision() == command.getRunRevision();
    }

    QuiescenceSnapshot quiescenceSnapshot(
            RemoteTaskRunRegistration registration,
            long newActiveRunRevision) {
        synchronized (monitor) {
            long capture = 0L;
            long fact = 0L;
            long input = 0L;
            for (Map.Entry<RequestKey, LedgerEntry> mapEntry : requests.entrySet()) {
                LedgerEntry entry = mapEntry.getValue();
                if (!terminalRequests.contains(mapEntry.getKey())
                        && entry.runRevision() < newActiveRunRevision
                        && exactIdentity(entry, registration)) {
                    switch (entry.operation()) {
                        case CAPTURE -> capture++;
                        case WINDOW_FACT -> fact++;
                        case EXECUTE_INPUT_BUNDLE, EXCLUSIVE_INTERACTION_CONTROL,
                                SUMMON_SKILL_WHOLE_PASS, LOCAL_MACRO -> input++;
                    }
                }
            }
            return new QuiescenceSnapshot(ledgerRevision, ledgerRevision,
                    registration.getTenantId(), registration.getUserId(),
                    registration.getDeviceId(), registration.getClientSessionId(),
                    registration.getTaskRunId(), registration.getWindowId(),
                    registration.getNativeHandle(), registration.getProcessId(),
                    registration.getPlayerIdentityEpoch(), registration.getStopEpoch(),
                    newActiveRunRevision, capture, fact, input);
        }
    }

    void withCurrentSnapshot(QuiescenceSnapshot snapshot, Runnable action) {
        synchronized (monitor) {
            if (snapshot != null && snapshot.ledgerRevision() == ledgerRevision) {
                action.run();
            }
        }
    }

    private static boolean exactIdentity(
            LedgerEntry entry,
            RemoteTaskRunRegistration registration) {
        return entry.scope().getTenantId().equals(registration.getTenantId())
                && entry.scope().getUserId().equals(registration.getUserId())
                && entry.scope().getDeviceId().equals(registration.getDeviceId())
                && entry.scope().getClientSessionId().equals(registration.getClientSessionId())
                && entry.taskRunId().equals(registration.getTaskRunId())
                && entry.window().getWindowId().equals(registration.getWindowId())
                && entry.window().getNativeHandle().equals(registration.getNativeHandle())
                && entry.window().getProcessId() == registration.getProcessId()
                && entry.window().getPlayerIdentityEpoch() == registration.getPlayerIdentityEpoch()
                && entry.stopEpoch() == registration.getStopEpoch();
    }

    private static boolean isExclusiveInputOperation(RemoteGameOperation operation) {
        return operation == RemoteGameOperation.EXECUTE_INPUT_BUNDLE
                || operation == RemoteGameOperation.EXCLUSIVE_INTERACTION_CONTROL
                || operation == RemoteGameOperation.SUMMON_SKILL_WHOLE_PASS
                || operation == RemoteGameOperation.LOCAL_MACRO;
    }

    private static boolean usesSparseTerminalControlOccurrences(RemoteGameCommand command) {
        // RELEASE/ABORT are mutually exclusive parent branches, so an unselected child leaves no
        // wire detail to compact. Only those closed child slots may skip parent occurrences.
        if (command.getOperation() != RemoteGameOperation.EXCLUSIVE_INTERACTION_CONTROL
                || command.getSemanticAddress() == null) {
            return false;
        }
        String slot = command.getSemanticAddress().getActionSlot();
        return slot != null && (slot.endsWith(":exclusive-release")
                || slot.endsWith(":exclusive-abort"));
    }

    public enum ClaimStatus {
        OWNER,
        DUPLICATE,
        IDEMPOTENCY_CONFLICT,
        ACTION_ID_REUSE,
        FINAL_CONSUMED,
        TASK_RUN_MISMATCH,
        WRONG_WINDOW,
        SEMANTIC_CONFLICT,
        CAPACITY_EXCEEDED,
        COORDINATED_RESTART_REQUIRED
    }

    public static final class Claim {
        private final ClaimStatus status;
        private final RequestKey requestKey;
        private final CompletableFuture<RemoteGameOutcomeEnvelope> outcome;

        private Claim(
                ClaimStatus status,
                RequestKey requestKey,
                CompletableFuture<RemoteGameOutcomeEnvelope> outcome) {
            this.status = status;
            this.requestKey = requestKey;
            this.outcome = outcome;
        }

        public ClaimStatus getStatus() {
            return status;
        }

        public RemoteGameOutcomeEnvelope awaitTerminalOutcome() {
            if (status != ClaimStatus.DUPLICATE || outcome == null) {
                throw new IllegalStateException("only a DUPLICATE claim has a shared terminal outcome");
            }
            return outcome.join();
        }

        public RemoteGameOperation getOperation() {
            return requestKey.operation();
        }

        public String getRequestId() {
            return requestKey.requestId();
        }
    }

    private record RequestKey(RemoteGameOperation operation, String requestId) {
    }

    private record ActionKey(String windowId, String actionId) {
    }

    private record InputActionIdentity(String requestId, String requestDigest) {
    }

    private record LedgerEntry(
            String requestDigest,
            CompletableFuture<RemoteGameOutcomeEnvelope> outcome,
            RemoteTaskRunScope scope,
            String taskRunId,
            RemoteWindowBindingRef window,
            long stopEpoch,
            long runRevision,
            RemoteGameOperation operation,
            SemanticDetailKey detailKey) {
    }

    static final class ReceiptSendHandle {
        private final long generation;
        private final String ackDigest;
        private final RemoteFinalConsumedReceipt receipt;

        private ReceiptSendHandle(
                long generation,
                String ackDigest,
                RemoteFinalConsumedReceipt receipt) {
            this.generation = generation;
            this.ackDigest = ackDigest;
            this.receipt = receipt;
        }

        RemoteFinalConsumedReceipt receipt() {
            return receipt;
        }
    }

    private record SemanticSlotKey(
            String tenantId,
            String userId,
            String deviceId,
            String clientSessionId,
            String taskRunId,
            String windowId,
            String nativeHandle,
            long processId,
            long playerIdentityEpoch,
            long stopEpoch,
            RemoteGameOperation operation,
            String phaseCode,
            String actionSlot) {

        private static SemanticSlotKey from(
                RemoteGameCommand command,
                RemoteTaskRunRegistration registration) {
            RemoteSemanticAddress address = command.getSemanticAddress();
            return new SemanticSlotKey(
                    registration.getTenantId(), registration.getUserId(),
                    registration.getDeviceId(), registration.getClientSessionId(),
                    registration.getTaskRunId(), registration.getWindowId(),
                    registration.getNativeHandle(), registration.getProcessId(),
                    registration.getPlayerIdentityEpoch(), registration.getStopEpoch(),
                    command.getOperation(), address.getPhaseCode(), address.getActionSlot());
        }

        private static SemanticSlotKey from(RemoteFinalConsumedAck acknowledgement) {
            RemoteSemanticAddress address = acknowledgement.getSemanticAddress();
            return new SemanticSlotKey(
                    acknowledgement.getTenantId(), acknowledgement.getUserId(),
                    acknowledgement.getDeviceId(), acknowledgement.getClientSessionId(),
                    acknowledgement.getTaskRunId(), acknowledgement.getWindow().getWindowId(),
                    acknowledgement.getWindow().getNativeHandle(),
                    acknowledgement.getWindow().getProcessId(),
                    acknowledgement.getWindow().getPlayerIdentityEpoch(),
                    acknowledgement.getStopEpoch(), acknowledgement.getOperation(),
                    address.getPhaseCode(), address.getActionSlot());
        }
    }

    private record SemanticDetailKey(
            SemanticSlotKey slotKey,
            RemoteSemanticAddress semanticAddress) {
    }

    record TerminalCleanupCandidate(
            String taskRunId,
            long generation,
            RemoteTaskRunRegistration stableRegistration) {

        TerminalCleanupCandidate {
            Objects.requireNonNull(taskRunId, "taskRunId");
            Objects.requireNonNull(stableRegistration, "stableRegistration");
        }

        boolean matchesStableBinding(RemoteTaskRunRegistration registration) {
            return registration != null
                    && Objects.equals(stableRegistration.getTenantId(), registration.getTenantId())
                    && Objects.equals(stableRegistration.getUserId(), registration.getUserId())
                    && Objects.equals(stableRegistration.getDeviceId(), registration.getDeviceId())
                    && Objects.equals(stableRegistration.getClientSessionId(),
                            registration.getClientSessionId())
                    && Objects.equals(stableRegistration.getTaskRunId(), registration.getTaskRunId())
                    && Objects.equals(stableRegistration.getTaskType(), registration.getTaskType())
                    && Objects.equals(stableRegistration.getWindowId(), registration.getWindowId())
                    && Objects.equals(stableRegistration.getNativeHandle(),
                            registration.getNativeHandle())
                    && stableRegistration.getProcessId() == registration.getProcessId()
                    && stableRegistration.getPlayerIdentityEpoch()
                            == registration.getPlayerIdentityEpoch();
        }
    }

    private static final class RunState {
        private final String taskRunId;
        private final RemoteTaskRunRegistration stableRegistration;
        private final Map<SemanticSlotKey, SemanticFrontier> semanticFrontiers =
                new HashMap<>();
        private final Map<SemanticDetailKey, SemanticDetail> currentDetails = new HashMap<>();
        private long generation;
        private int currentDetailCount;
        private int receiptOutboxCount;

        private RunState(RemoteTaskRunRegistration registration) {
            this.taskRunId = Objects.requireNonNull(
                    registration.getTaskRunId(), "taskRunId");
            this.stableRegistration = registration;
        }

        private boolean matches(RemoteTaskRunRegistration registration) {
            return new TerminalCleanupCandidate(taskRunId, generation, stableRegistration)
                    .matchesStableBinding(registration);
        }

        private boolean cleanupReady() {
            return currentDetailCount == 0 && receiptOutboxCount == 0;
        }
    }

    private static final class SemanticFrontier {
        private final String taskType;
        private final boolean sparseOccurrences;
        private long completedOccurrence = -1L;
        private Long openOccurrence;
        private int compactedThroughAttempt = -1;
        private SemanticDetailKey currentDetail;
        private String lastAckDigest;
        private RemoteFinalConsumedReceipt lastReceipt;

        private SemanticFrontier(String taskType, boolean sparseOccurrences) {
            this.taskType = Objects.requireNonNull(taskType, "taskType");
            this.sparseOccurrences = sparseOccurrences;
        }

        private AddressAdmission classify(RemoteSemanticAddress address) {
            long occurrence = address.getOccurrence();
            int attempt = address.getAttempt();
            if (occurrence <= completedOccurrence) {
                return AddressAdmission.BELOW_FRONTIER;
            }
            if (openOccurrence == null) {
                if (completedOccurrence == Long.MAX_VALUE) {
                    throw new IllegalStateException("semantic occurrence frontier overflow");
                }
                boolean nextOccurrence = sparseOccurrences
                        ? occurrence > completedOccurrence
                        : occurrence == completedOccurrence + 1L;
                return nextOccurrence && attempt == 0
                        ? AddressAdmission.NEXT_ALLOWED : AddressAdmission.GAP_OR_FUTURE;
            }
            if (occurrence < openOccurrence
                    || (occurrence == openOccurrence && attempt <= compactedThroughAttempt)) {
                return AddressAdmission.BELOW_FRONTIER;
            }
            return occurrence == openOccurrence && attempt == compactedThroughAttempt + 1
                    ? AddressAdmission.NEXT_ALLOWED : AddressAdmission.GAP_OR_FUTURE;
        }
    }

    private static final class SemanticDetail {
        private final SemanticDetailKey detailKey;
        private final RequestKey requestKey;
        private final LedgerEntry ledgerEntry;
        private final RemoteObservationMode observationMode;
        private final RemoteTaskRunRegistration registration;
        private RemoteGameOutcomeEnvelope terminalOutcome;

        private SemanticDetail(
                SemanticDetailKey detailKey,
                RequestKey requestKey,
                LedgerEntry ledgerEntry,
                RemoteObservationMode observationMode,
                RemoteTaskRunRegistration registration) {
            this.detailKey = detailKey;
            this.requestKey = requestKey;
            this.ledgerEntry = ledgerEntry;
            this.observationMode = observationMode;
            this.registration = registration;
        }
    }

    private static final class ReceiptOutboxEntry {
        private final long generation;
        private final RemoteFinalConsumedReceipt receipt;
        private ReceiptState state = ReceiptState.READY;

        private ReceiptOutboxEntry(long generation, RemoteFinalConsumedReceipt receipt) {
            this.generation = generation;
            this.receipt = receipt;
        }
    }

    private enum AddressAdmission {
        NEXT_ALLOWED,
        BELOW_FRONTIER,
        GAP_OR_FUTURE
    }

    private enum ReceiptState {
        READY,
        SENDING,
        REJECTED_RETAINED
    }

    record QuiescenceSnapshot(
            long ledgerRevision,
            long expectedLedgerRevision,
            String tenantId,
            String userId,
            String deviceId,
            String clientSessionId,
            String taskRunId,
            String windowId,
            String nativeHandle,
            long processId,
            long playerIdentityEpoch,
            long stopEpoch,
            long newActiveRunRevision,
            long inFlightCaptureCount,
            long inFlightFactCount,
            long inFlightInputCount) {

        boolean isQuiescent() {
            return inFlightCaptureCount == 0L && inFlightFactCount == 0L
                    && inFlightInputCount == 0L;
        }

        boolean matches(RemoteTaskRunRegistration registration, long revision) {
            return revision == newActiveRunRevision
                    && tenantId.equals(registration.getTenantId())
                    && userId.equals(registration.getUserId())
                    && deviceId.equals(registration.getDeviceId())
                    && clientSessionId.equals(registration.getClientSessionId())
                    && taskRunId.equals(registration.getTaskRunId())
                    && windowId.equals(registration.getWindowId())
                    && nativeHandle.equals(registration.getNativeHandle())
                    && processId == registration.getProcessId()
                    && playerIdentityEpoch == registration.getPlayerIdentityEpoch()
                    && stopEpoch == registration.getStopEpoch();
        }
    }
}
