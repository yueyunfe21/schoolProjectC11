package com.bot.dhxy.input.action;

import com.bot.dhxy.runner.context.TaskExecutionContext;
import com.bot.dhxy.runner.context.TaskExecutionContextHolder;
import com.bot.dhxy.runner.stop.TaskPauseToken;
import com.bot.dhxy.runner.stop.TaskStopToken;
import com.bot.dhxy.window.model.WindowNativeBinding;
import com.bot.dhxy.window.runtime.WindowNativeBindingRefreshService;
import com.bot.dhxy.window.runtime.WindowRuntimeContext;
import com.bot.dhxy.window.runtime.WindowTaskContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/**
 * Producer-side queue for serialized physical input requests.
 *
 * <p>Requests are bound to the current {@link WindowRuntimeContext} at submission time. This
 * captures the native hwnd/title before another task thread changes its context. The single
 * {@link InputActionWorker} consumes requests in order and performs best-effort focus plus the actual
 * mouse/keyboard actions.</p>
 */
@Slf4j
@Component
public class InputActionQueue {

    private static final long RETAINED_SESSION_BUDGET_NANOS = TimeUnit.SECONDS.toNanos(120L);

    private final BlockingQueue<InputActionRequest> queue = new LinkedBlockingQueue<>();
    private final WindowTaskContextHolder windowTaskContextHolder;
    private final WindowNativeBindingRefreshService bindingRefreshService;
    private final TaskExecutionContextHolder taskExecutionContextHolder;

    /**
     * @param windowTaskContextHolder thread-local holder used to capture the submitting window.
     * @param bindingRefreshService live HWND/title/geometry refresher used before input is queued.
     * @param taskExecutionContextHolder thread-local holder used to capture the submitting pause token.
     */
    public InputActionQueue(WindowTaskContextHolder windowTaskContextHolder,
                            WindowNativeBindingRefreshService bindingRefreshService,
                            TaskExecutionContextHolder taskExecutionContextHolder) {
        this.windowTaskContextHolder = windowTaskContextHolder;
        this.bindingRefreshService = bindingRefreshService;
        this.taskExecutionContextHolder = taskExecutionContextHolder;
    }

    /**
     * Submit a finite list of physical input actions and wait for completion.
     *
     * <p>This legacy path waits for at most 120 seconds of unpaused time. A managed task pause
     * freezes that wait budget and does not split or release the atomic request.</p>
     *
     * @param description nullable diagnostic label for logs/dead-letter records.
     * @param actions nullable ordered actions; coordinates must already be screen-absolute pixels.
     * @return true when the worker completed the request successfully, false when no bound window or
     * native binding exists, the waiter is interrupted, or the worker reports failure.
     */
    public boolean submitAndWait(String description, List<InputAction> actions) {
        Optional<WindowRuntimeContext> current = windowTaskContextHolder.rawCurrent();
        if (current.isEmpty()) {
            log.warn("Input action rejected because no window context exists: {}", description);
            return false;
        }
        WindowRuntimeContext context = current.get();
        if (!refreshAndValidateNativeBinding(context, description, true)) {
            return false;
        }
        CapturedTaskTokens taskTokens = captureTaskTokens();
        AwaitOutcome outcome = await(new InputActionRequest(context, description, actions,
                taskTokens.pauseToken(), taskTokens.stopToken()));
        return outcome.waiterCompletedNormally() && outcome.executionResult().isCompleted();
    }

    /**
     * Submit one atomic action bundle with a JVM-local monotonic deadline and wait for a structured result.
     *
     * <p>{@code deadlineNanos} must be an absolute value computed from {@link System#nanoTime()} in
     * this JVM. It cannot be persisted, sent over the network, or compared with another process's
     * clock. Convert a transported duration to a local deadline after receipt. The deadline continues
     * to advance while paused. Once a detailed safety gate closes, the worker will not start another
     * action step. Coordinates in {@code actions} must already be screen-absolute pixels.</p>
     *
     * @param description nullable diagnostic label; null is normalized to an empty string.
     * @param actions nullable ordered physical actions; null is treated as an empty atomic bundle.
     * @param deadlineNanos absolute {@link System#nanoTime()} deadline from this JVM, in nanoseconds.
     * @param stopRequested nullable, thread-safe, side-effect-free and non-blocking external stop gate;
     *                      true prevents the next detailed input boundary from starting.
     * @return non-null terminal result, including a request id for pre-enqueue rejection.
     */
    public InputActionExecutionResult submitAndWaitDetailed(String description,
                                                             List<InputAction> actions,
                                                             long deadlineNanos,
                                                             BooleanSupplier stopRequested) {
        return submitDetailed(
                description, actions, deadlineNanos, stopRequested, null, null, null);
    }

    /**
     * Submit one remote atomic bundle whose deadline excludes confirmed pause intervals.
     *
     * <p>The explicit token remains attached to the same queue request for its whole lifetime.
     * Resume continues the original request id and action sequence; no action is re-enqueued.</p>
     *
     * @param description nullable diagnostic label
     * @param actions ordered screen-absolute actions kept in one atomic queue request
     * @param deadlineNanos absolute monotonic JVM deadline
     * @param pauseToken stable token owned by the registered remote task run; non-null
     * @param safetyReason thread-safe, side-effect-free typed lifecycle/window safety gate
     * @param workerAdmission one-shot worker-start admission gate evaluated after the pause wait
     *                        and immediately before focus/first physical step; non-null
     * @return structured terminal progress for the original queue request
     */
    public InputActionExecutionResult submitRemoteAndWaitDetailed(
            String description,
            List<InputAction> actions,
            long deadlineNanos,
            TaskPauseToken pauseToken,
            Supplier<InputActionSafetyReason> safetyReason,
            Supplier<InputActionSafetyReason> workerAdmission) {
        return submitDetailed(
                description,
                actions,
                deadlineNanos,
                null,
                Objects.requireNonNull(safetyReason, "safetyReason"),
                Objects.requireNonNull(workerAdmission, "workerAdmission"),
                Objects.requireNonNull(pauseToken, "pauseToken"));
    }

    /**
     * Submit one remote whole-pass callback under the existing single physical-input owner.
     *
     * <p>The wall-clock deadline continues while PAUSED. The stable token blocks the same queued
     * callback, while the detailed safety supplier is re-read by every callback checkpoint after
     * that wait. The callback must use direct input APIs and must not enqueue nested input work.</p>
     *
     * @param description nullable diagnostic label
     * @param exclusiveCallback callback run on the single input worker; non-null
     * @param deadlineNanos absolute monotonic JVM deadline
     * @param pauseToken stable token of the exact remote task-run registry entry
     * @param safetyReason per-boundary continuation and lifecycle safety gate
     * @param workerAdmission one-shot original-command admission gate
     * @return structured terminal progress for this exact callback request
     */
    public InputActionExecutionResult submitRemoteExclusiveAndWaitDetailed(
            String description,
            Supplier<Boolean> exclusiveCallback,
            long deadlineNanos,
            TaskPauseToken pauseToken,
            Supplier<InputActionSafetyReason> safetyReason,
            Supplier<InputActionSafetyReason> workerAdmission) {
        Supplier<Boolean> callback = Objects.requireNonNull(
                exclusiveCallback, "exclusiveCallback");
        TaskPauseToken stablePauseToken = Objects.requireNonNull(pauseToken, "pauseToken");
        Supplier<InputActionSafetyReason> currentSafety = Objects.requireNonNull(
                safetyReason, "safetyReason");
        Supplier<InputActionSafetyReason> admission = Objects.requireNonNull(
                workerAdmission, "workerAdmission");
        Optional<WindowRuntimeContext> current = windowTaskContextHolder.rawCurrent();
        if (current.isEmpty()) {
            log.warn("Input exclusive action rejected because no window context exists: {}",
                    description);
            InputActionRequest rejected = new InputActionRequest(
                    null, description, callback, stablePauseToken, deadlineNanos,
                    currentSafety, admission);
            rejected.cancel(
                    InputActionSafetyReason.WINDOW_BINDING_CHANGED, "no-window-context");
            return rejected.getResult().join();
        }

        WindowRuntimeContext context = current.get();
        InputActionRequest request = new InputActionRequest(
                context, description, callback, stablePauseToken, deadlineNanos,
                currentSafety, admission);
        stablePauseToken.waitIfPausedRevision(null, request::shouldAbortPauseWait);
        if (!request.checkDetailedSafety("before-refresh")) {
            return request.getResult().join();
        }
        if (!refreshAndValidateNativeBinding(context, description, false)) {
            String reason = context.isIdentitySuspended()
                    ? "identity-suspended:after-refresh"
                    : "native-binding-unavailable";
            request.cancel(InputActionSafetyReason.WINDOW_BINDING_CHANGED, reason);
            return request.getResult().join();
        }
        stablePauseToken.waitIfPausedRevision(null, request::shouldAbortPauseWait);
        if (!request.checkDetailedSafety("after-refresh")) {
            return request.getResult().join();
        }
        return await(request).executionResult();
    }

    private InputActionExecutionResult submitDetailed(
            String description,
            List<InputAction> actions,
            long deadlineNanos,
            BooleanSupplier stopRequested,
            Supplier<InputActionSafetyReason> safetyReason,
            Supplier<InputActionSafetyReason> workerAdmission,
            TaskPauseToken pauseToken) {
        boolean remotePauseAware = pauseToken != null;
        Optional<WindowRuntimeContext> current = windowTaskContextHolder.rawCurrent();
        if (current.isEmpty()) {
            log.warn("Input action rejected because no window context exists: {}", description);
            InputActionRequest rejected = remotePauseAware
                    ? new InputActionRequest(
                            null, description, actions, pauseToken, deadlineNanos, safetyReason,
                            workerAdmission)
                    : new InputActionRequest(
                            null, description, actions, null, null, deadlineNanos, stopRequested);
            if (remotePauseAware) {
                rejected.cancel(InputActionSafetyReason.WINDOW_BINDING_CHANGED, "no-window-context");
            } else {
                rejected.cancel("no-window-context");
            }
            return rejected.getResult().join();
        }
        WindowRuntimeContext context = current.get();
        CapturedTaskTokens taskTokens = remotePauseAware
                ? new CapturedTaskTokens(pauseToken, null)
                : captureTaskTokens();
        InputActionRequest request = remotePauseAware
                ? new InputActionRequest(
                        context, description, actions, pauseToken, deadlineNanos, safetyReason,
                        workerAdmission)
                : new InputActionRequest(
                        context, description, actions, taskTokens.pauseToken(), taskTokens.stopToken(),
                        deadlineNanos, stopRequested);
        if (remotePauseAware) {
            request.compensatePause(
                    pauseToken.waitIfPausedRevision(null, request::shouldAbortPauseWait));
        }
        if (!request.checkDetailedSafety("before-refresh")) {
            return request.getResult().join();
        }
        if (!refreshAndValidateNativeBinding(context, description, false)) {
            String reason = context.isIdentitySuspended()
                    ? "identity-suspended:after-refresh"
                    : "native-binding-unavailable";
            if (remotePauseAware) {
                request.cancel(InputActionSafetyReason.WINDOW_BINDING_CHANGED, reason);
            } else {
                request.cancel(reason);
            }
            return request.getResult().join();
        }
        if (remotePauseAware) {
            request.compensatePause(
                    pauseToken.waitIfPausedRevision(null, request::shouldAbortPauseWait));
        }
        if (!request.checkDetailedSafety("after-refresh")) {
            return request.getResult().join();
        }
        return await(request).executionResult();
    }

    /**
     * Submit a finite list of physical input actions without blocking the task thread.
     *
     * <p>The request still captures the current window binding and task pause/stop tokens exactly
     * like {@link #submitAndWait(String, List)}. Use this only for cheap cleanup that must stay
     * window-bound but should not hold the foreground task turn, such as closing a mini-map after a
     * fire-and-handoff navigation click.</p>
     *
     * @param description diagnostic label for logs/dead-letter records.
     * @param actions ordered actions with screen-absolute coordinates where applicable.
     * @return true when the request was accepted into the input queue.
     */
    public boolean submit(String description, List<InputAction> actions) {
        Optional<WindowRuntimeContext> current = windowTaskContextHolder.rawCurrent();
        if (current.isEmpty()) {
            log.warn("Input action rejected because no window context exists: {}", description);
            return false;
        }
        WindowRuntimeContext context = current.get();
        if (!refreshAndValidateNativeBinding(context, description, true)) {
            return false;
        }
        CapturedTaskTokens taskTokens = captureTaskTokens();
        queue.offer(new InputActionRequest(context, description, actions,
                taskTokens.pauseToken(), taskTokens.stopToken()));
        return true;
    }

    /**
     * Submit an exclusive callback to run on the input worker thread.
     *
     * @param description diagnostic label for logs/dead-letter records.
     * @param callback callback that may use direct input provider calls. It must not submit nested
     *                 input queue requests because the worker is already executing it.
     * @return true when the callback completes with true; false on missing binding, interruption, or
     * worker failure.
     */
    public boolean submitExclusiveAndWait(String description, Supplier<Boolean> callback) {
        Optional<WindowRuntimeContext> current = windowTaskContextHolder.rawCurrent();
        if (current.isEmpty()) {
            log.warn("Input exclusive action rejected because no window context exists: {}", description);
            return false;
        }
        WindowRuntimeContext context = current.get();
        if (!refreshAndValidateNativeBinding(context, description, true)) {
            return false;
        }
        CapturedTaskTokens taskTokens = captureTaskTokens();
        AwaitOutcome outcome = await(new InputActionRequest(context, description, callback,
                taskTokens.pauseToken(), taskTokens.stopToken()));
        return outcome.waiterCompletedNormally() && outcome.executionResult().isCompleted();
    }

    /**
     * Run one callback through the global input worker against an already resolved window snapshot.
     * This path never refreshes or searches for a window; drift is rejected before focus or callback.
     *
     * <p>The frozen boundary deliberately does NOT accept a caller-supplied identity epoch. A caller that
     * resolves a binding and then reads the mutable epoch separately can splice a stale binding onto a
     * newer generation — a pair that never atomically existed. Instead this method takes the context
     * monitor once, requires the context to still publish the very same {@code binding} object (the
     * generation witness), and reads the epoch under that same monitor, so the {@code (binding, epoch)}
     * snapshot handed to the worker is always one indivisible generation.</p>
     *
     * @param description diagnostic label for logs/dead letters
     * @param context exact window context resolved by the cloud action boundary
     * @param binding immutable native binding snapshot used by focus, capture, and direct input; must
     *                still be the context's current generation object
     * @param callback callback executed by the global input worker; must not nest queue submissions
     * @return the worker's typed terminal result, carrying status, safety reason and detail verbatim
     */
    public InputActionExecutionResult submitFrozenExactWindowExclusiveAndWait(
            String description,
            WindowRuntimeContext context,
            WindowNativeBinding binding,
            Supplier<Boolean> callback) {
        WindowRuntimeContext exactContext = Objects.requireNonNull(context, "context");
        WindowNativeBinding exactBinding = Objects.requireNonNull(binding, "binding");
        Supplier<Boolean> exactCallback = Objects.requireNonNull(callback, "callback");
        CapturedTaskTokens taskTokens = captureTaskTokens();
        InputActionRequest request;
        synchronized (exactContext) {
            /*
             * Construct the one frozen request first, then gate it in priority order. A raw binding-object
             * comparison ahead of the typed detector answered a lower-priority question first: a caller that
             * stopped AND drifted was told the window changed, hiding the stop that actually ended it. Typed
             * safety therefore runs first and reports the detector's own reason verbatim; only then does the
             * pure witness answer the object-identity question the detector's value comparison cannot see.
             */
            request = InputActionRequest.frozenExactWindowExclusive(
                    exactContext, exactBinding, exactContext.getPlayerIdentityEpoch(), description,
                    exactCallback, taskTokens.pauseToken(), taskTokens.stopToken());
            InputActionRequest.DetailedCancellation frozenFailure =
                    request.frozenExactWindowFailure("before-enqueue");
            if (frozenFailure != null) {
                request.cancel(frozenFailure.safetyReason(), frozenFailure.reason());
                return request.getResult().join();
            }
            if (!request.isFrozenExactWindowGenerationCurrent()) {
                request.cancel(InputActionSafetyReason.WINDOW_BINDING_CHANGED,
                        "frozen-generation-changed-before-enqueue");
                return request.getResult().join();
            }
        }
        return await(request).executionResult();
    }

    /**
     * Submit one complete action list through the global input worker against an already resolved window
     * snapshot. This path never refreshes or searches for a window; drift is rejected before focus or the
     * first action.
     *
     * <p>Generation discipline is identical to
     * {@link #submitFrozenExactWindowExclusiveAndWait(String, WindowRuntimeContext, WindowNativeBinding, Supplier)}:
     * no caller-supplied identity epoch is accepted, the context monitor is taken once, the context must
     * still publish the very same {@code binding} object (the generation witness), and the epoch is read
     * under that same monitor, so the {@code (binding, epoch)} pair handed to the worker is one
     * indivisible generation.</p>
     *
     * <p>The difference is only what the single request carries. The whole {@code actions} list is stored
     * in one request and executed by the worker's existing action dispatcher inside one input transaction
     * and one context generation monitor, so a binding commit cannot interleave between two elements.
     * Callers that need the complete list under the frozen boundary use this method instead of wrapping
     * {@code submitAndWait} in the frozen callback, which would be a nested queue submission.</p>
     *
     * @param description diagnostic label for logs/dead letters
     * @param context exact window context resolved by the cloud action boundary
     * @param binding immutable native binding snapshot used by focus and direct input; must still be the
     *                context's current generation object
     * @param actions complete ordered action list with screen-absolute coordinates; kept in one request
     * @return the worker's typed terminal result, carrying status, safety reason and detail verbatim
     */
    public InputActionExecutionResult submitFrozenExactWindowActionsAndWait(
            String description,
            WindowRuntimeContext context,
            WindowNativeBinding binding,
            List<InputAction> actions) {
        WindowRuntimeContext exactContext = Objects.requireNonNull(context, "context");
        WindowNativeBinding exactBinding = Objects.requireNonNull(binding, "binding");
        List<InputAction> exactActions = List.copyOf(Objects.requireNonNull(actions, "actions"));
        CapturedTaskTokens taskTokens = captureTaskTokens();
        InputActionRequest request;
        synchronized (exactContext) {
            /*
             * Same priority order as the callback entry: construct the one frozen request, let typed safety
             * report its own reason first, and only then ask the pure object-identity witness. A raw binding
             * comparison ahead of the detector reported a window change for a request that had actually
             * stopped.
             */
            request = InputActionRequest.frozenExactWindowActions(
                    exactContext, exactBinding, exactContext.getPlayerIdentityEpoch(), description,
                    exactActions, taskTokens.pauseToken(), taskTokens.stopToken());
            InputActionRequest.DetailedCancellation frozenFailure =
                    request.frozenExactWindowFailure("before-enqueue");
            if (frozenFailure != null) {
                request.cancel(frozenFailure.safetyReason(), frozenFailure.reason());
                return request.getResult().join();
            }
            if (!request.isFrozenExactWindowGenerationCurrent()) {
                request.cancel(InputActionSafetyReason.WINDOW_BINDING_CHANGED,
                        "frozen-generation-changed-before-enqueue");
                return request.getResult().join();
            }
        }
        return await(request).executionResult();
    }

    /**
     * Enqueues one retained generic-exclusive request and waits only for worker admission.
     * The terminal future remains open so the synchronous command poller can deliver later steps.
     */
    public RetainedSessionHandle openRetainedSession(
            String description,
            TaskPauseToken pauseToken,
            Supplier<InputActionSafetyReason> safetyReason,
            Supplier<InputActionSafetyReason> workerAdmission) {
        Optional<WindowRuntimeContext> current = windowTaskContextHolder.rawCurrent();
        if (current.isEmpty()) {
            long rejectedAt = System.nanoTime();
            InputActionRequest request = InputActionRequest.retainedSession(
                    null, description, pauseToken, rejectedAt,
                    safetyReason, workerAdmission);
            request.cancel(InputActionSafetyReason.WINDOW_BINDING_CHANGED, "no-window-context");
            request.ensureRetainedSessionAdmission();
            return new RetainedSessionHandle(request,
                    request.getSessionAdmitted().join());
        }
        WindowRuntimeContext context = current.get();
        if (!refreshAndValidateNativeBinding(context, description, false)) {
            long rejectedAt = System.nanoTime();
            InputActionRequest request = InputActionRequest.retainedSession(
                    context, description, pauseToken, rejectedAt,
                    safetyReason, workerAdmission);
            request.cancel(InputActionSafetyReason.WINDOW_BINDING_CHANGED,
                    "retained-session-binding-unavailable");
            request.ensureRetainedSessionAdmission();
            return new RetainedSessionHandle(request,
                    request.getSessionAdmitted().join());
        }
        long now = System.nanoTime();
        long deadline = now > Long.MAX_VALUE - RETAINED_SESSION_BUDGET_NANOS
                ? Long.MAX_VALUE : now + RETAINED_SESSION_BUDGET_NANOS;
        InputActionRequest request = InputActionRequest.retainedSession(
                context, description, pauseToken, deadline,
                safetyReason, workerAdmission);
        if (!request.checkDetailedSafety("retained-session-before-enqueue")) {
            request.ensureRetainedSessionAdmission();
            return new RetainedSessionHandle(request,
                    request.getSessionAdmitted().join());
        }
        request.retainTerminalPublicationForWorker();
        if (!queue.offer(request)) {
            request.cancel("retained-session-queue-rejected");
            request.releaseRetainedTerminalPublication();
            request.ensureRetainedSessionAdmission();
            return new RetainedSessionHandle(request,
                    request.getSessionAdmitted().join());
        }
        try {
            while (true) {
                InputActionRequest.SessionAdmission admitted =
                        request.getSessionAdmitted().getNow(null);
                if (admitted != null) {
                    return new RetainedSessionHandle(request, admitted);
                }
                request.compensatePause(pauseToken.waitIfPausedRevision(
                        null, request::shouldAbortPauseWait));
                long remaining = request.remainingDeadlineNanos(System.nanoTime());
                if (remaining <= 0L) {
                    boolean removed = queue.remove(request);
                    if (removed) {
                        request.expireDeadline("retained-session-admission");
                        request.releaseRetainedTerminalPublication();
                        request.ensureRetainedSessionAdmission();
                    } else {
                        request.requestDetailedCancellation("deadline-exceeded:retained-session-admission");
                    }
                    continue;
                }
                try {
                    InputActionRequest.SessionAdmission result = request.getSessionAdmitted().get(
                            Math.min(TimeUnit.SECONDS.toNanos(1L), remaining),
                            TimeUnit.NANOSECONDS);
                    return new RetainedSessionHandle(request, result);
                } catch (TimeoutException ignored) {
                    // Re-check the same monotonic, pause-compensated budget.
                }
            }
        } catch (InterruptedException interrupted) {
            boolean removed = queue.remove(request);
            if (removed) {
                request.cancel("waiter interrupted before retained-session admission");
                request.releaseRetainedTerminalPublication();
            } else {
                request.requestDetailedCancellation(
                        "waiter interrupted during retained-session admission");
            }
            request.ensureRetainedSessionAdmission();
            Thread.currentThread().interrupt();
            return new RetainedSessionHandle(request,
                    request.getSessionAdmitted().join());
        } catch (java.util.concurrent.ExecutionException impossible) {
            throw new IllegalStateException("retained-session admission failed", impossible);
        }
    }

    /** Delivers one capacity-one physical step to the already-admitted retained request. */
    public InputActionExecutionResult submitRetainedSessionStepAndWait(
            RetainedSessionHandle handle,
            List<InputAction> actions) {
        InputActionRequest request = requireRetainedSession(handle);
        InputActionRequest.RetainedSessionStep step =
                new InputActionRequest.RetainedSessionStep(actions);
        if (!request.offerRetainedSessionSignal(step)) {
            InputActionExecutionResult terminal = request.getResult().getNow(null);
            if (terminal == null && request.isRetainedSessionTerminalCommitted()) {
                terminal = request.getResult().join();
            }
            if (terminal != null) {
                step.complete(
                        request.getRequestId(), false, terminal.getReason(),
                        terminal.getSafetyReason());
                return step.completion().join();
            }
            throw new IllegalStateException("retained-session step lane is occupied");
        }
        try {
            return step.completion().get();
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("retained-session step waiter interrupted", interrupted);
        } catch (java.util.concurrent.ExecutionException failure) {
            throw new IllegalStateException("retained-session step failed", failure.getCause());
        }
    }

    /**
     * Applies the retained request's pause-compensated deadline and lifecycle safety at a
     * non-input mechanical boundary such as session CAPTURE.
     *
     * @param handle opaque capability returned by {@link #openRetainedSession}
     * @param stage non-blank diagnostic boundary label
     * @return true only while the same retained request may start or publish that boundary
     */
    public boolean checkRetainedSessionBoundary(
            RetainedSessionHandle handle,
            String stage) {
        InputActionRequest request = requireRetainedSession(handle);
        String boundaryStage = Objects.requireNonNull(stage, "stage");
        if (boundaryStage.isBlank()) {
            throw new IllegalArgumentException("stage must not be blank");
        }
        TaskPauseToken pauseToken = request.getPauseToken();
        if (pauseToken != null) {
            request.compensatePause(pauseToken.pauseProgress());
        }
        return request.checkDetailedSafety("retained-session:" + boundaryStage);
    }

    /** Sends RELEASE/ABORT and waits for the request's terminal completion. */
    public InputActionExecutionResult terminateRetainedSessionAndWait(
            RetainedSessionHandle handle,
            SessionTerminalCommand command) {
        InputActionRequest request = requireRetainedSession(handle);
        SessionTerminalCommand exactCommand = Objects.requireNonNull(command, "command");
        if (!request.getResult().isDone()
                && !request.offerRetainedSessionSignal(
                        new InputActionRequest.RetainedSessionTerminal(
                                exactCommand == SessionTerminalCommand.RELEASE
                                        ? InputActionRequest.SessionTerminalCommand.RELEASE
                                        : InputActionRequest.SessionTerminalCommand.ABORT))
                && !request.isRetainedSessionTerminalCommitted()) {
            throw new IllegalStateException("retained-session terminal lane is occupied");
        }
        try {
            return request.getResult().get();
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            InputActionExecutionResult terminal = request.getResult().getNow(null);
            if (terminal != null) {
                return terminal;
            }
            throw new IllegalStateException("retained-session terminal waiter interrupted", interrupted);
        } catch (java.util.concurrent.ExecutionException failure) {
            throw new IllegalStateException("retained-session terminal failed", failure.getCause());
        }
    }

    private static InputActionRequest requireRetainedSession(RetainedSessionHandle handle) {
        RetainedSessionHandle retained = Objects.requireNonNull(handle, "handle");
        if (!retained.request.isRetainedSessionMode()) {
            throw new IllegalStateException("input handle is not a retained session");
        }
        return retained.request;
    }

    public enum SessionTerminalCommand {
        RELEASE,
        ABORT
    }

    public enum SessionAdmission {
        ADMITTED,
        REJECTED_NOT_EXECUTED,
        ADMISSION_UNKNOWN
    }

    /** Opaque queue-owned capability for the one retained request. */
    public static final class RetainedSessionHandle {
        private final InputActionRequest request;
        private final SessionAdmission admission;

        private RetainedSessionHandle(
                InputActionRequest request,
                InputActionRequest.SessionAdmission admission) {
            this.request = Objects.requireNonNull(request, "request");
            this.admission = SessionAdmission.valueOf(admission.name());
        }

        public SessionAdmission admission() {
            return admission;
        }

        public String requestId() {
            return request.getRequestId();
        }

        public InputActionExecutionResult terminalSnapshot() {
            return request.retainedTerminalSnapshot();
        }

        public InputActionExecutionResult releasedTerminalSnapshot() {
            return request.getResult().getNow(null);
        }
    }

    private CapturedTaskTokens captureTaskTokens() {
        return taskExecutionContextHolder.current()
                .map(context -> new CapturedTaskTokens(context.getPauseToken(), context.getStopToken()))
                .orElseGet(() -> new CapturedTaskTokens(null, null));
    }

    private boolean refreshAndValidateNativeBinding(WindowRuntimeContext context,
                                                    String description,
                                                    boolean waitForIdentitySuspension) {
        if (!waitForIdentitySuspension && context.isIdentitySuspended()) {
            log.warn("Input action rejected because player identity is suspended before binding refresh: windowId={} description={}",
                    context.getWindowId(), description);
            return false;
        }
        WindowNativeBinding binding = context.getNativeBinding();
        if (binding == null || !binding.hasNativeHandle()) {
            log.warn("Input action rejected because no native binding exists: windowId={} description={}",
                    context.getWindowId(), description);
            return false;
        }
        /*
         * Pure input paths can be the first action after the same HWND is logged into another player.
         * Refresh before enqueueing so the worker carries the live title/player epoch, not a stale
         * task-start binding that only later screenshot/tracker code would update.
         */
        Optional<WindowNativeBinding> refreshedBinding = bindingRefreshService.refreshAndCommit(context);
        if (refreshedBinding.isEmpty()) {
            log.warn("Input action rejected because live binding refresh is unavailable: windowId={} description={}",
                    context.getWindowId(), description);
            return false;
        }
        WindowNativeBinding refreshed = context.getNativeBinding();
        if (refreshed == null || !refreshed.hasNativeHandle()) {
            log.warn("Input action rejected after live binding refresh: windowId={} description={}",
                    context.getWindowId(), description);
            return false;
        }
        if (!waitForIdentitySuspension && context.isIdentitySuspended()) {
            log.warn("Input action rejected because player identity is suspended after binding refresh: windowId={} description={}",
                    context.getWindowId(), description);
            return false;
        }
        if (waitForIdentitySuspension) {
            context.waitIfIdentitySuspended(null);
        }
        return true;
    }

    private AwaitOutcome await(InputActionRequest request) {
        if (request.hasDeadline() && !request.checkDetailedSafety("before-enqueue")) {
            return new AwaitOutcome(request.getResult().join(), false);
        }
        queue.offer(request);
        long remainingWaitMs = TimeUnit.SECONDS.toMillis(120);
        long lastCheckMs = System.currentTimeMillis();
        boolean detailedCancellationPending = false;
        try {
            while (true) {
                long pollMs;
                if (request.hasDeadline()) {
                    InputActionExecutionResult ready = request.getResult().getNow(null);
                    if (ready != null) {
                        return new AwaitOutcome(ready, true);
                    }
                    if (request.excludesPauseFromDeadline()) {
                        request.compensatePause(
                                request.getPauseToken().waitIfPausedRevision(
                                        request.getStopToken(), request::shouldAbortPauseWait));
                    }
                    if (!detailedCancellationPending) {
                        InputActionRequest.DetailedCancellation cancellation =
                                request.detailedCancellation("await");
                        if (cancellation != null) {
                            boolean removed = queue.remove(request);
                            if (removed) {
                                request.cancel(cancellation.safetyReason(), cancellation.reason());
                                log.info("Detailed input removed after safety gate closed: windowId={} description={} reason={}",
                                        request.getWindowId(), request.getDescription(), cancellation.reason());
                                return new AwaitOutcome(request.getResult().join(), false);
                            }
                            boolean newlyRequested = request.requestDetailedCancellation(cancellation);
                            detailedCancellationPending = true;
                            if (newlyRequested) {
                                log.info("Detailed input cancellation delegated to worker owner: windowId={} description={} reason={}",
                                        request.getWindowId(), request.getDescription(), cancellation.reason());
                            }
                        }
                    }
                    long pollNanos;
                    if (detailedCancellationPending) {
                        pollNanos = TimeUnit.SECONDS.toNanos(1L);
                    } else {
                        long deadlineRemainingNanos = request.remainingDeadlineNanos(System.nanoTime());
                        if (deadlineRemainingNanos <= 0L) {
                            continue;
                        }
                        pollNanos = Math.max(1L,
                                Math.min(TimeUnit.SECONDS.toNanos(1L), deadlineRemainingNanos));
                    }
                    try {
                        return new AwaitOutcome(
                                request.getResult().get(pollNanos, TimeUnit.NANOSECONDS), true);
                    } catch (TimeoutException e) {
                        continue;
                    }
                } else {
                    pollMs = Math.max(1L, Math.min(1000L, remainingWaitMs));
                }
                try {
                    return new AwaitOutcome(request.getResult().get(pollMs, TimeUnit.MILLISECONDS), true);
                } catch (TimeoutException e) {
                    if (request.hasDeadline()) {
                        continue;
                    }
                    long now = System.currentTimeMillis();
                    if (request.isPauseRequested()) {
                        lastCheckMs = now;
                        continue;
                    }
                    remainingWaitMs -= Math.max(1L, now - lastCheckMs);
                    lastCheckMs = now;
                    if (remainingWaitMs <= 0L) {
                        boolean removed;
                        if (request.isFrozenExactWindow()) {
                            removed = queue.remove(request);
                            if (removed) {
                                request.cancel("wait timed out");
                            } else {
                                request.requestDetailedCancellation("wait timed out");
                            }
                        } else {
                            request.cancel("wait timed out");
                            removed = queue.remove(request);
                        }
                        log.warn("Input action wait timed out: windowId={} description={} removedFromQueue={}",
                                request.getWindowId(), request.getDescription(), removed);
                        return new AwaitOutcome(request.getResult().join(), false);
                    }
                }
            }
        } catch (InterruptedException e) {
            if (request.hasDeadline() || request.isFrozenExactWindow()) {
                boolean removed = queue.remove(request);
                if (removed) {
                    request.cancel("waiter interrupted");
                } else {
                    request.requestDetailedCancellation("waiter interrupted");
                }
                InputActionExecutionResult terminalResult = request.getResult().join();
                Thread.currentThread().interrupt();
                log.warn("Input action wait interrupted: windowId={} description={} removedFromQueue={}",
                        request.getWindowId(), request.getDescription(), removed);
                return new AwaitOutcome(terminalResult, false);
            }
            request.cancel("waiter interrupted");
            boolean removed = queue.remove(request);
            Thread.currentThread().interrupt();
            log.warn("Input action wait interrupted: windowId={} description={} removedFromQueue={}",
                    request.getWindowId(), request.getDescription(), removed);
            return new AwaitOutcome(request.getResult().join(), false);
        } catch (Exception e) {
            if (request.hasDeadline() || request.isFrozenExactWindow()) {
                boolean removed = queue.remove(request);
                if (removed) {
                    request.cancel("wait failed");
                } else {
                    request.requestDetailedCancellation("wait failed");
                }
                InputActionExecutionResult terminalResult = request.getResult().join();
                log.warn("Input action wait failed: windowId={} description={} removedFromQueue={} reason={}",
                        request.getWindowId(), request.getDescription(), removed, e.getMessage());
                return new AwaitOutcome(terminalResult, false);
            }
            request.cancel("wait failed");
            boolean removed = queue.remove(request);
            log.warn("Input action wait failed: windowId={} description={} removedFromQueue={} reason={}",
                    request.getWindowId(), request.getDescription(), removed, e.getMessage());
            return new AwaitOutcome(request.getResult().join(), false);
        }
    }

    InputActionRequest take() throws InterruptedException {
        return queue.take();
    }

    /**
     * @return current queued request count, primarily for diagnostics.
     */
    public int size() {
        return queue.size();
    }

    private record CapturedTaskTokens(TaskPauseToken pauseToken, TaskStopToken stopToken) {
    }

    private record AwaitOutcome(InputActionExecutionResult executionResult, boolean waiterCompletedNormally) {
    }
}
