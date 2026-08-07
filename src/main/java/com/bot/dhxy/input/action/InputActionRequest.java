package com.bot.dhxy.input.action;

import com.bot.dhxy.runner.stop.TaskPauseToken;
import com.bot.dhxy.runner.stop.TaskStopToken;
import com.bot.dhxy.window.model.WindowNativeBinding;
import com.bot.dhxy.window.runtime.WindowRuntimeContext;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/**
 * One input request captured for the worker queue.
 *
 * <p>The request stores both the {@link WindowRuntimeContext} and its native binding at submission
 * time, so the worker focuses/sends input to the same window even if another task thread later binds
 * a different context. It contains either a list of {@link InputAction}s or one exclusive callback.</p>
 */
public class InputActionRequest {

    private final String requestId = UUID.randomUUID().toString();
    private final WindowRuntimeContext windowContext;
    private final String windowId;
    private final WindowNativeBinding nativeBinding;
    private final long playerIdentityEpoch;
    private final TaskPauseToken pauseToken;
    private final TaskStopToken stopToken;
    private final String description;
    private final List<InputAction> actions;
    private final Supplier<Boolean> exclusiveCallback;
    private final boolean deadlineAware;
    private final long deadlineNanos;
    private final boolean excludePauseFromDeadline;
    private final BooleanSupplier externalStopRequested;
    private final Supplier<InputActionSafetyReason> externalSafetyReason;
    private final Supplier<InputActionSafetyReason> workerAdmission;
    private final boolean frozenExactWindow;
    private boolean exclusiveCallbackFocusRequired;
    private final CompletableFuture<InputActionExecutionResult> result = new CompletableFuture<>();
    private final CompletableFuture<SessionAdmission> sessionAdmitted = new CompletableFuture<>();
    private final ArrayBlockingQueue<RetainedSessionSignal> retainedSessionLane =
            new ArrayBlockingQueue<>(1);
    private final AtomicBoolean cancelled = new AtomicBoolean(false);
    /** G008: true only after the worker commits this request to one atomic input transaction. */
    private final AtomicBoolean executionStarted = new AtomicBoolean(false);
    private final AtomicReference<String> cancellationReason = new AtomicReference<>();
    private final Object progressLock = new Object();
    private InputActionSafetyReason cancellationSafetyReason = InputActionSafetyReason.CLEAR;
    private boolean workerAdmitted;
    private long accountedPauseNanos;
    private long deadlineCompensationNanos;
    private int startedStepIndex = -1;
    private int lastStartedStepIndex = -1;
    private int lastCompletedStepIndex = -1;
    private boolean terminal;
    private InputActionExecutionResult terminalCompletion;
    private boolean retainedSessionMode;
    private boolean retainedTerminalPublicationOwnedByWorker;
    private Runnable retainedSessionCleanup;
    private boolean retainedSessionWorkStarted;
    private boolean retainedSessionCleanupCompleted;
    private int nextRetainedPhysicalActionIndex;

    /**
     * Create a normal action-list request.
     *
     * @param windowContext submitting window context; should have a native binding.
     * @param description diagnostic label.
     * @param actions ordered physical actions. The list is copied and null becomes empty.
     */
    public InputActionRequest(WindowRuntimeContext windowContext,
                              String description,
                              List<InputAction> actions) {
        this(windowContext, description, actions, null, null);
    }

    /**
     * Create a normal action-list request with the submitting task's pause token.
     *
     * @param windowContext submitting window context; should have a native binding.
     * @param description diagnostic label.
     * @param actions ordered physical actions. The list is copied and null becomes empty.
     * @param pauseToken pause token captured on the submitting task thread; nullable for debug paths.
     */
    public InputActionRequest(WindowRuntimeContext windowContext,
                              String description,
                              List<InputAction> actions,
                              TaskPauseToken pauseToken) {
        this(windowContext, description, actions, pauseToken, null);
    }

    /**
     * Create a normal action-list request with the submitting task's pause/stop tokens.
     *
     * @param windowContext submitting window context; should have a native binding.
     * @param description diagnostic label.
     * @param actions ordered physical actions. The list is copied and null becomes empty.
     * @param pauseToken pause token captured on the submitting task thread; nullable for debug paths.
     * @param stopToken stop token captured on the submitting task thread; nullable for debug paths.
     */
    public InputActionRequest(WindowRuntimeContext windowContext,
                              String description,
                              List<InputAction> actions,
                              TaskPauseToken pauseToken,
                              TaskStopToken stopToken) {
        this(windowContext, description, actions, null, pauseToken, stopToken,
                null, null, null, null, false);
    }

    InputActionRequest(WindowRuntimeContext windowContext,
                       String description,
                       List<InputAction> actions,
                       TaskPauseToken pauseToken,
                       TaskStopToken stopToken,
                       long deadlineNanos,
                       BooleanSupplier externalStopRequested) {
        this(windowContext, description, actions, null, pauseToken, stopToken,
                deadlineNanos, externalStopRequested, null, null, false);
    }

    InputActionRequest(WindowRuntimeContext windowContext,
                       String description,
                       List<InputAction> actions,
                       TaskPauseToken pauseToken,
                       long deadlineNanos,
                       Supplier<InputActionSafetyReason> externalSafetyReason,
                       Supplier<InputActionSafetyReason> workerAdmission) {
        this(windowContext, description, actions, null, pauseToken, null,
                deadlineNanos, null, externalSafetyReason, workerAdmission, true);
    }

    InputActionRequest(WindowRuntimeContext windowContext,
                       String description,
                       Supplier<Boolean> exclusiveCallback,
                       TaskPauseToken pauseToken,
                       long deadlineNanos,
                       Supplier<InputActionSafetyReason> externalSafetyReason,
                       Supplier<InputActionSafetyReason> workerAdmission) {
        this(windowContext, description, List.of(), exclusiveCallback, pauseToken, null,
                deadlineNanos, null, externalSafetyReason, workerAdmission, false);
    }

    /**
     * Create an exclusive callback request.
     *
     * @param windowContext submitting window context; should have a native binding.
     * @param description diagnostic label.
     * @param exclusiveCallback callback executed on the input worker thread.
     */
    public InputActionRequest(WindowRuntimeContext windowContext,
                              String description,
                              Supplier<Boolean> exclusiveCallback) {
        this(windowContext, description, List.of(), exclusiveCallback, null, null,
                null, null, null, null, false);
    }

    /**
     * Create an exclusive callback request with the submitting task's pause token.
     *
     * @param windowContext submitting window context; should have a native binding.
     * @param description diagnostic label.
     * @param exclusiveCallback callback executed on the input worker thread.
     * @param pauseToken pause token captured on the submitting task thread; nullable for debug paths.
     */
    public InputActionRequest(WindowRuntimeContext windowContext,
                              String description,
                              Supplier<Boolean> exclusiveCallback,
                              TaskPauseToken pauseToken) {
        this(windowContext, description, exclusiveCallback, pauseToken, null);
    }

    /**
     * Create an exclusive callback request with the submitting task's pause/stop tokens.
     *
     * @param windowContext submitting window context; should have a native binding.
     * @param description diagnostic label.
     * @param exclusiveCallback callback executed on the input worker thread.
     * @param pauseToken pause token captured on the submitting task thread; nullable for debug paths.
     * @param stopToken stop token captured on the submitting task thread; nullable for debug paths.
     */
    public InputActionRequest(WindowRuntimeContext windowContext,
                              String description,
                              Supplier<Boolean> exclusiveCallback,
                              TaskPauseToken pauseToken,
                              TaskStopToken stopToken) {
        this(windowContext, description, List.of(), exclusiveCallback, pauseToken, stopToken,
                null, null, null, null, false);
    }

    private InputActionRequest(WindowRuntimeContext windowContext,
                               String description,
                               List<InputAction> actions,
                               Supplier<Boolean> exclusiveCallback,
                               TaskPauseToken pauseToken,
                               TaskStopToken stopToken,
                               Long deadlineNanos,
                               BooleanSupplier externalStopRequested,
                               Supplier<InputActionSafetyReason> externalSafetyReason,
                               Supplier<InputActionSafetyReason> workerAdmission,
                               boolean excludePauseFromDeadline) {
        this(windowContext, description, actions, exclusiveCallback, pauseToken, stopToken,
                deadlineNanos, externalStopRequested, externalSafetyReason, workerAdmission,
                excludePauseFromDeadline, null, null, false);
    }

    private InputActionRequest(WindowRuntimeContext windowContext,
                               String description,
                               List<InputAction> actions,
                               Supplier<Boolean> exclusiveCallback,
                               TaskPauseToken pauseToken,
                               TaskStopToken stopToken,
                               Long deadlineNanos,
                               BooleanSupplier externalStopRequested,
                               Supplier<InputActionSafetyReason> externalSafetyReason,
                               Supplier<InputActionSafetyReason> workerAdmission,
                               boolean excludePauseFromDeadline,
                               WindowNativeBinding frozenNativeBinding,
                               Long frozenPlayerIdentityEpoch,
                               boolean frozenExactWindow) {
        this.windowContext = windowContext;
        this.windowId = windowContext == null ? null : windowContext.getWindowId();
        this.nativeBinding = frozenExactWindow
                ? frozenNativeBinding
                : windowContext == null ? null : windowContext.getNativeBinding();
        this.playerIdentityEpoch = frozenExactWindow
                ? frozenPlayerIdentityEpoch == null ? -1L : frozenPlayerIdentityEpoch
                : windowContext == null ? -1L : windowContext.getPlayerIdentityEpoch();
        this.pauseToken = pauseToken;
        this.stopToken = stopToken;
        this.description = description == null ? "" : description;
        this.actions = actions == null ? List.of() : List.copyOf(actions);
        this.exclusiveCallback = exclusiveCallback;
        this.exclusiveCallbackFocusRequired = exclusiveCallback != null;
        this.deadlineAware = deadlineNanos != null;
        this.deadlineNanos = deadlineNanos == null ? 0L : deadlineNanos;
        this.excludePauseFromDeadline = deadlineNanos != null && excludePauseFromDeadline;
        this.accountedPauseNanos = this.excludePauseFromDeadline && pauseToken != null
                ? pauseToken.pauseProgress().cumulativePauseNanos()
                : 0L;
        this.externalStopRequested = externalStopRequested;
        this.externalSafetyReason = externalSafetyReason;
        this.workerAdmission = workerAdmission;
        this.frozenExactWindow = frozenExactWindow;
    }

    static InputActionRequest frozenExactWindowExclusive(
            WindowRuntimeContext windowContext,
            WindowNativeBinding frozenNativeBinding,
            long frozenPlayerIdentityEpoch,
            String description,
            Supplier<Boolean> exclusiveCallback,
            TaskPauseToken pauseToken,
            TaskStopToken stopToken) {
        return frozenExactWindowExclusive(
                windowContext, frozenNativeBinding, frozenPlayerIdentityEpoch, description,
                exclusiveCallback, pauseToken, stopToken, null);
    }

    static InputActionRequest frozenExactWindowExclusive(
            WindowRuntimeContext windowContext,
            WindowNativeBinding frozenNativeBinding,
            long frozenPlayerIdentityEpoch,
            String description,
            Supplier<Boolean> exclusiveCallback,
            TaskPauseToken pauseToken,
            TaskStopToken stopToken,
            Supplier<InputActionSafetyReason> externalSafetyReason) {
        return new InputActionRequest(
                windowContext, description, List.of(), exclusiveCallback, pauseToken, stopToken,
                null, null, externalSafetyReason, null, false,
                frozenNativeBinding, frozenPlayerIdentityEpoch, true);
    }

    /**
     * One frozen exact-window callback whose mechanics are fully HWND-background capable.
     *
     * <p>This request keeps the same immutable binding/epoch witness and the same pause and stop
     * gates as {@link #frozenExactWindowExclusive}. The only difference is focus: the worker must
     * not foreground the window before invoking the callback.</p>
     */
    static InputActionRequest frozenExactWindowBackgroundExclusive(
            WindowRuntimeContext windowContext,
            WindowNativeBinding frozenNativeBinding,
            long frozenPlayerIdentityEpoch,
            String description,
            Supplier<Boolean> exclusiveCallback,
            TaskPauseToken pauseToken,
            TaskStopToken stopToken) {
        InputActionRequest request = frozenExactWindowExclusive(
                windowContext,
                frozenNativeBinding,
                frozenPlayerIdentityEpoch,
                description,
                exclusiveCallback,
                pauseToken,
                stopToken);
        request.suppressExclusiveCallbackFocus();
        return request;
    }

    /**
     * One frozen exact-window request carrying the complete ordered action list instead of a callback.
     *
     * <p>Identical generation semantics to {@link #frozenExactWindowExclusive}: the {@code (binding,
     * epoch)} pair is frozen by the queue under the context monitor and witnessed by
     * {@link #isFrozenExactWindowGenerationCurrent()}. The whole list lives in this single request, so
     * the worker executes it inside one input transaction and one generation monitor and no binding
     * commit can interleave between two elements. {@code exclusiveCallback} stays null, which is what
     * routes the worker to the frozen action-list path rather than the frozen callback path.</p>
     *
     * @param windowContext exact resolved window context
     * @param frozenNativeBinding exact binding object that is the context's current generation witness
     * @param frozenPlayerIdentityEpoch epoch read under the same context monitor as the binding
     * @param description diagnostic label
     * @param actions complete ordered action list; copied immutably into this one request
     * @param pauseToken captured task pause token, nullable
     * @param stopToken captured task stop token, nullable
     * @param externalSafetyReason live safety gate rechecked before focus and each physical action
     * @return frozen exact-window action-list request
     */
    static InputActionRequest frozenExactWindowActions(
            WindowRuntimeContext windowContext,
            WindowNativeBinding frozenNativeBinding,
            long frozenPlayerIdentityEpoch,
            String description,
            List<InputAction> actions,
            TaskPauseToken pauseToken,
            TaskStopToken stopToken,
            Supplier<InputActionSafetyReason> externalSafetyReason) {
        return new InputActionRequest(
                windowContext, description, actions, null, pauseToken, stopToken,
                null, null, externalSafetyReason, null, false,
                frozenNativeBinding, frozenPlayerIdentityEpoch, true);
    }

    static InputActionRequest retainedSession(
            WindowRuntimeContext windowContext,
            String description,
            TaskPauseToken pauseToken,
            TaskStopToken stopToken,
            long deadlineNanos,
            Supplier<InputActionSafetyReason> externalSafetyReason,
            Supplier<InputActionSafetyReason> workerAdmission,
            Runnable retainedSessionCleanup) {
        InputActionRequest request = new InputActionRequest(
                windowContext, description, List.of(), null, pauseToken, stopToken,
                deadlineNanos, null, externalSafetyReason, workerAdmission, true);
        request.retainedSessionMode = true;
        request.retainedSessionCleanup = retainedSessionCleanup;
        return request;
    }

    /**
     * One-shot worker-admission gate for remote detailed requests.
     *
     * <p>Evaluated by the input worker after the pause wait and immediately before the first
     * focus/physical step. The first invocation evaluates the supplied predicate exactly once: a
     * blocking reason cancels the request with a typed safety reason while no step has started
     * (NOT_STARTED, startedStepIndex=-1), so the remote handler reports
     * {@code NOT_EXECUTED/TASK_RUN_MISMATCH}. After one successful admission every later call
     * returns true without re-evaluating, so the predicate is never re-applied between bundle
     * steps; mid-bundle safety stays owned by the existing pause-token and safety-gate logic.</p>
     *
     * @param stage diagnostic stage label for the cancellation reason
     * @return true when the worker may proceed to focus/first step
     */
    boolean admitWorkerStart(String stage) {
        if (workerAdmission == null) {
            if (!retainedSessionMode) {
                return true;
            }
            synchronized (progressLock) {
                if (terminal || isCancelled()) {
                    return false;
                }
                workerAdmitted = true;
                return true;
            }
        }
        InputActionSafetyReason blockingReason;
        /*
         * Check, evaluation, and the ADMITTED commit form one atomic section: a queue-waiter
         * cancellation can never interleave between them, so admission cannot return true for a
         * request whose terminal/cancelled state already committed. The detailed-safety path
         * already evaluates external suppliers under progressLock, so lock ordering toward the
         * task-run registry is unchanged.
         */
        synchronized (progressLock) {
            if (terminal || isCancelled()) {
                return false;
            }
            if (workerAdmitted) {
                return true;
            }
            InputActionSafetyReason admissionReason;
            try {
                admissionReason = workerAdmission.get();
            } catch (RuntimeException e) {
                admissionReason = InputActionSafetyReason.TASK_RUN_MISMATCH;
            }
            if (admissionReason == null) {
                admissionReason = InputActionSafetyReason.TASK_RUN_MISMATCH;
            }
            if (!admissionReason.blocksInput()) {
                workerAdmitted = true;
                return true;
            }
            blockingReason = admissionReason;
        }
        cancel(blockingReason,
                blockingReason.diagnosticPrefix() + ":worker-admission:"
                        + normalizeReason(stage, "unknown-stage"));
        return false;
    }

    /** @return queue correlation id generated even when submission is rejected before enqueueing. */
    public String getRequestId() { return requestId; }

    /** @return submitting window context captured at queue time. */
    public WindowRuntimeContext getWindowContext() { return windowContext; }

    /** @return submitting window id, or null when no context was supplied. */
    public String getWindowId() { return windowId; }

    /** @return native binding captured at queue time, possibly null for rejected/debug paths. */
    public WindowNativeBinding getNativeBinding() { return nativeBinding; }

    /** @return player identity epoch captured at queue time. */
    public long getPlayerIdentityEpoch() { return playerIdentityEpoch; }

    /** @return task pause token captured at queue time, or null outside a managed task. */
    public TaskPauseToken getPauseToken() { return pauseToken; }

    /** @return task stop token captured at queue time, or null outside a managed task. */
    public TaskStopToken getStopToken() { return stopToken; }

    /** Mark the physical-input transaction boundary after admission/identity checks. */
    public void markExecutionStarted() {
        executionStarted.set(true);
    }

    /** @return true after the input worker committed to this request. */
    public boolean isExecutionStarted() {
        return executionStarted.get();
    }

    /**
     * @return true when the request still belongs to the same player identity epoch.
     */
    public boolean isPlayerIdentityEpochCurrent() {
        return windowContext == null || playerIdentityEpoch == windowContext.getPlayerIdentityEpoch();
    }

    /**
     * @return true when the submitting task has been paused after this request was queued.
     */
    public boolean isPauseRequested() {
        return pauseToken != null && pauseToken.isPauseRequested();
    }

    /** @return diagnostic label for logs. */
    public String getDescription() { return description; }

    /** @return ordered immutable action list. Empty when this is an exclusive callback request. */
    public List<InputAction> getActions() { return actions; }

    /** @return exclusive callback, or null for normal action-list requests. */
    public Supplier<Boolean> getExclusiveCallback() { return exclusiveCallback; }

    /** @return true when this request should run a callback instead of replaying action objects. */
    public boolean hasExclusiveCallback() { return exclusiveCallback != null; }

    boolean isExclusiveCallbackFocusRequired() { return exclusiveCallbackFocusRequired; }

    void suppressExclusiveCallbackFocus() {
        if (exclusiveCallback == null) {
            throw new IllegalStateException("only exclusive callbacks have a focus policy");
        }
        exclusiveCallbackFocusRequired = false;
    }

    /** @return true when this request must execute against its immutable exact-window snapshot. */
    boolean isFrozenExactWindow() { return frozenExactWindow; }

    /**
     * Verify the immutable window identity captured by the cloud action resolver without refreshing it.
     *
     * @return true only while window id, HWND, process, rectangle, and player identity epoch still match
     */
    boolean isFrozenExactWindowCurrent() {
        return !frozenExactWindow || detectFrozenExactWindowFailure("snapshot-check") == null;
    }

    /**
     * Single authoritative generation witness for the frozen exact-window path.
     *
     * <p>Callers MUST already hold {@code synchronized (windowContext)}. On top of the value comparison
     * done by {@link #isFrozenExactWindowCurrent()} this also requires the context to still publish the
     * very same {@link WindowNativeBinding} <em>object</em> that was frozen with this request's epoch.
     * Object identity is what makes an {@code A -> B -> A} rebind observable: a value-equal replacement
     * binding is a different generation and is rejected here, so a stale action snapshot can never be
     * recombined with a newer context generation into a pair that never atomically existed.</p>
     *
     * <p>This is a generation witness ONLY. It deliberately carries no safety meaning: a false answer here
     * means "this is a different window generation", never "the task stopped". Mixing the two made a closed
     * stop indistinguishable from binding drift, so every caller relabeled a real stop as
     * {@code WINDOW_BINDING_CHANGED}. Typed safety belongs to {@link #checkDetailedSafety(String)} /
     * {@link #frozenExactWindowFailure(String)}, which preserve stop and cancellation reasons; callers must
     * consult those first and use this witness only for the object-identity generation question they answer
     * by value and therefore cannot see.</p>
     *
     * @return true only while this request's frozen binding object is still the context's current generation
     */
    boolean isFrozenExactWindowGenerationCurrent() {
        if (!frozenExactWindow) {
            return true;
        }
        if (windowContext == null || nativeBinding == null) {
            return false;
        }
        if (windowContext.getNativeBinding() != nativeBinding) {
            return false;
        }
        if (windowId == null || !java.util.Objects.equals(windowId, windowContext.getWindowId())) {
            return false;
        }
        return !windowContext.isIdentitySuspended()
                && playerIdentityEpoch == windowContext.getPlayerIdentityEpoch();
    }

    /**
     * Typed frozen-window failure for callers that must publish a terminal themselves.
     *
     * <p>Returns the detector's own reason verbatim — {@code STOP_REQUESTED} stays a stop and a window change
     * stays a window change — so a caller can never flatten a stop into binding drift.</p>
     *
     * @param stage diagnostic stage label
     * @return the typed failure, or null when no frozen gate is closed
     */
    DetailedCancellation frozenExactWindowFailure(String stage) {
        synchronized (progressLock) {
            if (terminal) {
                return null;
            }
            return detectFrozenExactWindowFailure(stage);
        }
    }

    /**
     * @return absolute {@link System#nanoTime()} deadline meaningful only inside this JVM; never persist
     * or compare this value across processes. Check {@link #hasDeadline()} before reading it.
     */
    public long getDeadlineNanos() { return deadlineNanos; }

    /** @return true only for the new deadline-aware detailed request path. */
    public boolean hasDeadline() { return deadlineAware; }

    /** @return true only for remote detailed requests whose confirmed pauses freeze the deadline. */
    public boolean excludesPauseFromDeadline() { return excludePauseFromDeadline; }

    /** @return structured completion future used by both detailed and legacy waiters. */
    public CompletableFuture<InputActionExecutionResult> getResult() { return result; }

    boolean isRetainedSessionMode() { return retainedSessionMode; }

    CompletableFuture<SessionAdmission> getSessionAdmitted() { return sessionAdmitted; }

    void retainTerminalPublicationForWorker() {
        synchronized (progressLock) {
            if (!retainedSessionMode || terminal) {
                throw new IllegalStateException(
                        "retained terminal publication must be reserved before enqueue");
            }
            retainedTerminalPublicationOwnedByWorker = true;
        }
    }

    void releaseRetainedTerminalPublication() {
        InputActionExecutionResult completion;
        synchronized (progressLock) {
            if (!retainedSessionMode) {
                return;
            }
            retainedTerminalPublicationOwnedByWorker = false;
            completion = terminalCompletion;
        }
        if (completion != null) {
            publishTerminalCompletion(completion);
        }
    }

    InputActionExecutionResult retainedTerminalSnapshot() {
        synchronized (progressLock) {
            return terminalCompletion;
        }
    }

    void completeRejectedSessionAdmission(boolean provablyNotExecuted) {
        if (retainedSessionMode) {
            sessionAdmitted.complete(provablyNotExecuted
                    ? SessionAdmission.REJECTED_NOT_EXECUTED
                    : SessionAdmission.ADMISSION_UNKNOWN);
        }
    }

    void ensureRetainedSessionAdmission() {
        if (!retainedSessionMode || sessionAdmitted.isDone()) {
            return;
        }
        synchronized (progressLock) {
            completeRejectedSessionAdmission(startedStepIndex < 0);
        }
    }

    boolean completeRetainedSessionAdmission() {
        if (!retainedSessionMode) {
            return true;
        }
        InputActionExecutionResult deadlineFailure = null;
        synchronized (progressLock) {
            SessionAdmission existing = sessionAdmitted.getNow(null);
            if (existing != null) {
                return existing == SessionAdmission.ADMITTED;
            }
            if (terminal || isCancelled() || !workerAdmitted) {
                completeRejectedSessionAdmission(startedStepIndex < 0);
                return false;
            }
            deadlineFailure = detailedSafetyFailure("retained-session-admission");
            if (deadlineFailure == null) {
                sessionAdmitted.complete(SessionAdmission.ADMITTED);
                return true;
            }
        }
        publishTerminalCompletion(deadlineFailure);
        return false;
    }

    boolean offerRetainedSessionSignal(RetainedSessionSignal signal) {
        synchronized (progressLock) {
            if (!retainedSessionMode || terminal || result.isDone()) {
                return false;
            }
            return retainedSessionLane.offer(signal);
        }
    }

    RetainedSessionSignal pollRetainedSessionSignal(long timeoutNanos)
            throws InterruptedException {
        return retainedSessionLane.poll(Math.max(1L, timeoutNanos), TimeUnit.NANOSECONDS);
    }

    boolean isRetainedSessionTerminalCommitted() {
        synchronized (progressLock) {
            return retainedSessionMode && terminal;
        }
    }

    void completePendingRetainedSessionStep() {
        InputActionExecutionResult terminalResult = retainedTerminalSnapshot();
        String reason = terminalResult == null
                ? normalizeReason(cancellationReason.get(), "retained-session-terminal")
                : terminalResult.getReason();
        InputActionSafetyReason safetyReason = terminalResult == null
                ? cancellationSafetyReason : terminalResult.getSafetyReason();
        RetainedSessionSignal pending;
        while ((pending = retainedSessionLane.poll()) != null) {
            if (pending instanceof RetainedSessionStep step) {
                step.complete(requestId, false, reason, safetyReason);
            }
        }
    }

    boolean tryStartRetainedAction(RetainedSessionStep step, int actionIndex, String stage) {
        int globalIndex;
        synchronized (progressLock) {
            globalIndex = nextRetainedPhysicalActionIndex;
        }
        if (!tryStartStep(globalIndex, stage)) {
            return false;
        }
        synchronized (progressLock) {
            nextRetainedPhysicalActionIndex = Math.incrementExact(globalIndex);
        }
        step.markStarted(actionIndex);
        return true;
    }

    void markRetainedActionCompleted(RetainedSessionStep step, int actionIndex) {
        int globalIndex;
        synchronized (progressLock) {
            globalIndex = nextRetainedPhysicalActionIndex - 1;
        }
        markStepCompleted(globalIndex);
        step.markCompleted(actionIndex);
    }

    void markRetainedSessionWorkStarted() {
        synchronized (progressLock) {
            retainedSessionWorkStarted = true;
        }
    }

    void runRetainedSessionCleanup() {
        Runnable cleanup;
        synchronized (progressLock) {
            if (!retainedSessionWorkStarted || retainedSessionCleanupCompleted) {
                return;
            }
            retainedSessionCleanupCompleted = true;
            cleanup = retainedSessionCleanup;
        }
        if (cleanup != null) {
            cleanup.run();
        }
    }

    /** @return first recorded cancellation reason, or null when the request has not been cancelled. */
    public String getCancellationReason() { return cancellationReason.get(); }

    /**
     * Mark the request cancelled and unblock the submitter.
     *
     * @param reason diagnostic reason stored for dead-letter logs.
     */
    public void cancel(String reason) {
        cancel(InputActionSafetyReason.CLEAR, reason);
    }

    /** Cancels with a typed remote safety reason while preserving the normal progress snapshot. */
    void cancel(InputActionSafetyReason safetyReason, String reason) {
        InputActionExecutionResult completion = null;
        synchronized (progressLock) {
            // G008 only protects a request once the worker committed it to the global input
            // transaction. Identity checks still run in the worker; this narrowly prevents a
            // pause/stop from tearing an already-started MOVE -> CLICK sequence in half.
            if (executionStarted.get() && safetyReason == InputActionSafetyReason.STOP_REQUESTED) {
                return;
            }
            cancellationReason.compareAndSet(null, normalizeReason(reason, "cancelled"));
            recordCancellationSafetyReason(safetyReason);
            cancelled.set(true);
            if (frozenExactWindow && startedStepIndex >= 0 && !terminal) {
                return;
            }
            if (!terminal) {
                terminal = true;
                completion = buildResult(false, cancellationReason.get());
                terminalCompletion = completion;
            }
        }
        if (completion != null) {
            publishTerminalCompletion(completion);
        }
    }

    /**
     * @return true when the request was cancelled or its completion future was cancelled.
     */
    public boolean isCancelled() {
        return cancelled.get() || result.isCancelled();
    }

    long remainingDeadlineNanos(long nowNanos) {
        if (!hasDeadline()) {
            return Long.MAX_VALUE;
        }
        long effectiveDeadlineNanos;
        synchronized (progressLock) {
            effectiveDeadlineNanos = saturatingAdd(deadlineNanos, deadlineCompensationNanos);
        }
        long remainingNanos = effectiveDeadlineNanos - nowNanos;
        return remainingNanos <= 0L ? 0L : remainingNanos;
    }

    /**
     * Extends a remote-only deadline by newly observed cumulative pause progress.
     *
     * @return nanoseconds newly added, or zero when another observer already accounted for it
     */
    long compensatePause(TaskPauseToken.PauseWaitSnapshot snapshot) {
        if (!excludePauseFromDeadline || snapshot == null) {
            return 0L;
        }
        synchronized (progressLock) {
            long observed = snapshot.cumulativePauseNanos();
            if (observed <= accountedPauseNanos) {
                return 0L;
            }
            long newlyObserved = observed - accountedPauseNanos;
            accountedPauseNanos = observed;
            long previous = deadlineCompensationNanos;
            deadlineCompensationNanos = saturatingAdd(previous, newlyObserved);
            return deadlineCompensationNanos - previous;
        }
    }

    /** @return true when a non-deadline safety gate requires leaving a remote pause wait. */
    boolean shouldAbortPauseWait() {
        if (isCancelled() || Thread.currentThread().isInterrupted()) {
            return true;
        }
        if (windowContext != null && windowContext.isIdentitySuspended()) {
            return true;
        }
        DetailedCancellation externalFailure = detectExternalSafetyReason("pause-wait");
        return externalFailure != null;
    }

    /**
     * Check deadline-aware or frozen-window safety gates before focus or action-bundle work.
     * Legacy requests return immediately without reading the clock, identity state, or external gate.
     */
    boolean checkDetailedSafety(String stage) {
        if (!hasDeadline() && !frozenExactWindow) {
            return true;
        }
        InputActionExecutionResult completion = null;
        synchronized (progressLock) {
            if (terminal || isCancelled()) {
                return false;
            }
            DetailedCancellation frozenFailure = detectFrozenExactWindowFailure(stage);
            if (frozenFailure != null) {
                requestCooperativeCancellation(frozenFailure);
                return false;
            }
            completion = detailedSafetyFailure(stage);
            if (completion == null) {
                return true;
            }
        }
        publishTerminalCompletion(completion);
        return false;
    }

    /**
     * Inspect detailed-only gates without completing the future. The queue waiter uses this after
     * enqueueing so worker ownership can be resolved with {@code queue.remove(request)} first.
     */
    DetailedCancellation detailedCancellation(String stage) {
        if (!hasDeadline()) {
            return null;
        }
        synchronized (progressLock) {
            if (terminal) {
                return null;
            }
            if (cancelled.get() && cancellationReason.get() != null) {
                return new DetailedCancellation(cancellationReason.get(), cancellationSafetyReason);
            }
            return detectDetailedSafetyReason(stage);
        }
    }

    /**
     * Request cooperative worker-owned cancellation without completing the future.
     * The owning worker remains responsible for freezing the final progress snapshot.
     */
    boolean requestDetailedCancellation(String reason) {
        return requestDetailedCancellation(
                new DetailedCancellation(normalizeReason(reason, "cancelled"), InputActionSafetyReason.CLEAR));
    }

    boolean requestDetailedCancellation(DetailedCancellation cancellation) {
        synchronized (progressLock) {
            if (terminal) {
                return false;
            }
            cancellationReason.compareAndSet(null, cancellation.reason());
            recordCancellationSafetyReason(cancellation.safetyReason());
            boolean newlyRequested = !cancelled.get();
            cancelled.set(true);
            return newlyRequested;
        }
    }

    /**
     * Atomically reserve one zero-based step start after its deadline check.
     * This lock is what makes a concurrent NOT_STARTED cancellation provable.
     */
    boolean tryStartStep(int stepIndex, String stage) {
        if (!hasDeadline() && !frozenExactWindow) {
            synchronized (progressLock) {
                if (startedStepIndex < 0) {
                    startedStepIndex = stepIndex;
                }
                lastStartedStepIndex = stepIndex;
            }
            return true;
        }
        InputActionExecutionResult completion = null;
        synchronized (progressLock) {
            if (terminal || isCancelled()) {
                return false;
            }
            DetailedCancellation frozenFailure = detectFrozenExactWindowFailure(stage);
            if (frozenFailure != null) {
                requestCooperativeCancellation(frozenFailure);
                return false;
            }
            completion = detailedSafetyFailure(stage);
            if (completion == null) {
                if (startedStepIndex < 0) {
                    startedStepIndex = stepIndex;
                }
                lastStartedStepIndex = stepIndex;
            }
        }
        if (completion != null) {
            publishTerminalCompletion(completion);
            return false;
        }
        return true;
    }

    /** Force a deadline terminal result after a deadline-limited sleep segment. */
    void expireDeadline(String stage) {
        if (!hasDeadline()) {
            return;
        }
        InputActionExecutionResult completion;
        synchronized (progressLock) {
            if (terminal) {
                return;
            }
            completion = expireAtDeadline(stage);
        }
        publishTerminalCompletion(completion);
    }

    /** Record that one previously started zero-based step returned definite success. */
    void markStepCompleted(int stepIndex) {
        synchronized (progressLock) {
            if (!terminal
                    && stepIndex <= lastStartedStepIndex
                    && stepIndex == lastCompletedStepIndex + 1) {
                lastCompletedStepIndex = stepIndex;
            }
        }
    }

    /** Complete the request exactly once with a frozen progress snapshot. */
    void complete(boolean successful, String reason) {
        InputActionExecutionResult completion;
        synchronized (progressLock) {
            if (terminal) {
                return;
            }
            boolean terminalSuccess = successful && !cancelled.get();
            String terminalReason = terminalSuccess
                    ? normalizeReason(reason, "completed")
                    : normalizeReason(cancellationReason.get(), normalizeReason(reason, "unknown"));
            terminal = true;
            completion = buildResult(terminalSuccess, terminalReason);
            terminalCompletion = completion;
        }
        publishTerminalCompletion(completion);
    }

    private void publishTerminalCompletion(InputActionExecutionResult completion) {
        completeRejectedSessionAdmission(completion.getStartedStepIndex() < 0);
        boolean retainedByWorker;
        synchronized (progressLock) {
            if (terminalCompletion == null) {
                terminalCompletion = completion;
            }
            retainedByWorker = retainedSessionMode
                    && retainedTerminalPublicationOwnedByWorker;
        }
        if (!retainedByWorker) {
            result.complete(completion);
        }
    }

    private InputActionExecutionResult expireAtDeadline(String stage) {
        String reason = "deadline-exceeded:" + normalizeReason(stage, "unknown-stage");
        return cancelDetailed(new DetailedCancellation(reason, InputActionSafetyReason.CLEAR));
    }

    private InputActionExecutionResult detailedSafetyFailure(String stage) {
        DetailedCancellation cancellation = detectDetailedSafetyReason(stage);
        return cancellation == null ? null : cancelDetailed(cancellation);
    }

    private DetailedCancellation detectDetailedSafetyReason(String stage) {
        if (!hasDeadline()) {
            return null;
        }
        if (retainedSessionMode) {
            String normalizedStage = normalizeReason(stage, "unknown-stage");
            if (stopToken != null && stopToken.isStopRequested()) {
                return new DetailedCancellation(
                        "task-stop:" + normalizedStage,
                        InputActionSafetyReason.STOP_REQUESTED);
            }
            if (pauseToken != null && pauseToken.isPauseRequested()) {
                return new DetailedCancellation(
                        "task-pause:" + normalizedStage,
                        InputActionSafetyReason.CLEAR);
            }
            if (windowContext == null || windowId == null
                    || !java.util.Objects.equals(windowId, windowContext.getWindowId())
                    || windowContext.isIdentitySuspended()
                    || playerIdentityEpoch != windowContext.getPlayerIdentityEpoch()
                    || !sameExactWindow(nativeBinding, windowContext.getNativeBinding())) {
                return new DetailedCancellation(
                        "retained-window-generation-changed:" + normalizedStage,
                        InputActionSafetyReason.WINDOW_BINDING_CHANGED);
            }
        }
        DetailedCancellation externalFailure = detectExternalSafetyReason(stage);
        if (externalFailure != null) {
            return externalFailure;
        }
        if (windowContext != null && windowContext.isIdentitySuspended()) {
            return new DetailedCancellation(
                    "identity-suspended:" + normalizeReason(stage, "unknown-stage"),
                    InputActionSafetyReason.WINDOW_BINDING_CHANGED);
        }
        if (remainingDeadlineNanos(System.nanoTime()) <= 0L) {
            return new DetailedCancellation(
                    "deadline-exceeded:" + normalizeReason(stage, "unknown-stage"),
                    InputActionSafetyReason.CLEAR);
        }
        return null;
    }

    private DetailedCancellation detectFrozenExactWindowFailure(String stage) {
        if (!frozenExactWindow) {
            return null;
        }
        String normalizedStage = normalizeReason(stage, "unknown-stage");
        if (windowContext == null || windowId == null
                || !java.util.Objects.equals(windowId, windowContext.getWindowId())) {
            return new DetailedCancellation(
                    "frozen-window-id-changed:" + normalizedStage,
                    InputActionSafetyReason.WINDOW_BINDING_CHANGED);
        }
        if (stopToken != null && stopToken.isStopRequested()) {
            return new DetailedCancellation(
                    "task-stop:" + normalizedStage,
                    InputActionSafetyReason.STOP_REQUESTED);
        }
        DetailedCancellation externalFailure = detectExternalSafetyReason(stage);
        if (externalFailure != null) {
            return externalFailure;
        }
        if (windowContext.isIdentitySuspended()
                || playerIdentityEpoch != windowContext.getPlayerIdentityEpoch()) {
            return new DetailedCancellation(
                    "frozen-player-identity-changed:" + normalizedStage,
                    InputActionSafetyReason.WINDOW_BINDING_CHANGED);
        }
        WindowNativeBinding currentBinding = windowContext.getNativeBinding();
        if (!sameExactWindow(nativeBinding, currentBinding)) {
            return new DetailedCancellation(
                    "frozen-native-binding-changed:" + normalizedStage,
                    InputActionSafetyReason.WINDOW_BINDING_CHANGED);
        }
        return null;
    }

    private void requestCooperativeCancellation(DetailedCancellation cancellation) {
        cancellationReason.compareAndSet(null, cancellation.reason());
        recordCancellationSafetyReason(cancellation.safetyReason());
        cancelled.set(true);
    }

    private static boolean sameExactWindow(WindowNativeBinding expected, WindowNativeBinding actual) {
        return expected != null
                && actual != null
                && expected.hasNativeHandle()
                && actual.hasNativeHandle()
                && expected.hasGeometry()
                && actual.hasGeometry()
                && java.util.Objects.equals(expected.getNativeHandle(), actual.getNativeHandle())
                && expected.getProcessId() == actual.getProcessId()
                && expected.getX() == actual.getX()
                && expected.getY() == actual.getY()
                && expected.getWidth() == actual.getWidth()
                && expected.getHeight() == actual.getHeight();
    }

    private DetailedCancellation detectExternalSafetyReason(String stage) {
        if (externalSafetyReason != null) {
            InputActionSafetyReason safetyReason;
            try {
                safetyReason = externalSafetyReason.get();
            } catch (RuntimeException e) {
                safetyReason = InputActionSafetyReason.TASK_RUN_MISMATCH;
            }
            if (safetyReason == null) {
                safetyReason = InputActionSafetyReason.TASK_RUN_MISMATCH;
            }
            if (safetyReason.blocksInput()) {
                return new DetailedCancellation(
                        safetyReason.diagnosticPrefix() + ":" + normalizeReason(stage, "unknown-stage"),
                        safetyReason);
            }
        }
        if (externalStopRequested != null) {
            try {
                if (externalStopRequested.getAsBoolean()) {
                    return new DetailedCancellation(
                            "external-stop-requested",
                            InputActionSafetyReason.STOP_REQUESTED);
                }
            } catch (RuntimeException e) {
                return new DetailedCancellation(
                        "external-stop-check-failed:" + normalizeReason(stage, "unknown-stage"),
                        InputActionSafetyReason.CLEAR);
            }
        }
        return null;
    }

    private InputActionExecutionResult cancelDetailed(DetailedCancellation cancellation) {
        cancellationReason.compareAndSet(null, cancellation.reason());
        recordCancellationSafetyReason(cancellation.safetyReason());
        cancelled.set(true);
        terminal = true;
        terminalCompletion = buildResult(false, cancellationReason.get());
        return terminalCompletion;
    }

    private InputActionExecutionResult buildResult(boolean successful, String reason) {
        InputActionExecutionResult.Status status;
        if (successful) {
            status = InputActionExecutionResult.Status.COMPLETED;
        } else if (startedStepIndex < 0) {
            status = InputActionExecutionResult.Status.NOT_STARTED;
        } else if (lastStartedStepIndex > lastCompletedStepIndex) {
            status = InputActionExecutionResult.Status.STARTED_UNKNOWN;
        } else {
            status = InputActionExecutionResult.Status.PARTIALLY_COMPLETED;
        }
        return InputActionExecutionResult.builder()
                .requestId(requestId)
                .started(startedStepIndex >= 0)
                .startedStepIndex(startedStepIndex)
                .lastCompletedStepIndex(lastCompletedStepIndex)
                .status(status)
                .safetyReason(cancellationSafetyReason)
                .reason(normalizeReason(reason, successful ? "completed" : "unknown"))
                .build();
    }

    private void recordCancellationSafetyReason(InputActionSafetyReason safetyReason) {
        if (cancellationSafetyReason == InputActionSafetyReason.CLEAR
                && safetyReason != null
                && safetyReason.blocksInput()) {
            cancellationSafetyReason = safetyReason;
        }
    }

    private static String normalizeReason(String reason, String fallback) {
        return reason == null || reason.isBlank() ? fallback : reason;
    }

    private static long saturatingAdd(long left, long right) {
        if (right <= 0L) {
            return left;
        }
        return left > Long.MAX_VALUE - right ? Long.MAX_VALUE : left + right;
    }

    record DetailedCancellation(String reason, InputActionSafetyReason safetyReason) {
        DetailedCancellation {
            reason = normalizeReason(reason, "cancelled");
            safetyReason = safetyReason == null ? InputActionSafetyReason.CLEAR : safetyReason;
        }
    }

    enum SessionAdmission {
        ADMITTED,
        REJECTED_NOT_EXECUTED,
        ADMISSION_UNKNOWN
    }

    sealed interface RetainedSessionSignal permits RetainedSessionStep, RetainedSessionTerminal {
    }

    static final class RetainedSessionStep implements RetainedSessionSignal {
        private final List<InputAction> actions;
        private final Supplier<Boolean> callback;
        private final CompletableFuture<InputActionExecutionResult> completion =
                new CompletableFuture<>();
        private int startedStepIndex = -1;
        private int lastCompletedStepIndex = -1;

        RetainedSessionStep(List<InputAction> actions) {
            this.actions = actions == null ? List.of() : List.copyOf(actions);
            if (this.actions.isEmpty()) {
                throw new IllegalArgumentException("retained session input step must not be empty");
            }
            this.callback = null;
        }

        RetainedSessionStep(Supplier<Boolean> callback) {
            this.actions = List.of();
            this.callback = java.util.Objects.requireNonNull(callback, "callback");
        }

        List<InputAction> actions() {
            return actions;
        }

        boolean hasCallback() {
            return callback != null;
        }

        Supplier<Boolean> callback() {
            return callback;
        }

        int workItemCount() {
            return hasCallback() ? 1 : actions.size();
        }

        CompletableFuture<InputActionExecutionResult> completion() {
            return completion;
        }

        synchronized void markStarted(int actionIndex) {
            if (startedStepIndex < 0) {
                startedStepIndex = actionIndex;
            }
        }

        synchronized void markCompleted(int actionIndex) {
            if (actionIndex == lastCompletedStepIndex + 1) {
                lastCompletedStepIndex = actionIndex;
            }
        }

        synchronized void complete(String requestId, boolean successful, String reason,
                                   InputActionSafetyReason safetyReason) {
            InputActionExecutionResult.Status status;
            if (successful && lastCompletedStepIndex == workItemCount() - 1) {
                status = InputActionExecutionResult.Status.COMPLETED;
            } else if (startedStepIndex < 0) {
                status = InputActionExecutionResult.Status.NOT_STARTED;
            } else if (lastCompletedStepIndex < workItemCount() - 1) {
                status = InputActionExecutionResult.Status.STARTED_UNKNOWN;
            } else {
                status = InputActionExecutionResult.Status.PARTIALLY_COMPLETED;
            }
            completion.complete(InputActionExecutionResult.builder()
                    .requestId(requestId)
                    .started(startedStepIndex >= 0)
                    .startedStepIndex(startedStepIndex)
                    .lastCompletedStepIndex(lastCompletedStepIndex)
                    .status(status)
                    .safetyReason(safetyReason == null
                            ? InputActionSafetyReason.CLEAR : safetyReason)
                    .reason(normalizeReason(reason, successful ? "completed" : "unknown"))
                    .build());
        }
    }

    record RetainedSessionTerminal(SessionTerminalCommand command)
            implements RetainedSessionSignal {
        RetainedSessionTerminal {
            command = java.util.Objects.requireNonNull(command, "command");
        }
    }

    enum SessionTerminalCommand {
        RELEASE,
        ABORT
    }
}
