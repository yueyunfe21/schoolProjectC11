package com.bot.dhxy.input.action;

import com.bot.dhxy.runner.stop.TaskSleep;
import com.bot.dhxy.runner.stop.TaskPauseToken;
import com.bot.dhxy.runner.stop.TaskStopRequestedException;

import com.bot.dhxy.input.InputProvider;
import com.bot.dhxy.input.WindowAwareInputCoordinator;
import com.bot.dhxy.driver.BoundWindowKeyboardService;
import com.bot.dhxy.tools.LatencyMetrics;
import com.bot.dhxy.window.runtime.WindowRuntimeContext;
import com.bot.dhxy.window.runtime.WindowTaskContextHolder;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Single background worker that executes queued physical input requests.
 *
 * <p>The worker is the only consumer of {@link InputActionQueue}. Keyboard actions are delivered only
 * through the request's exact HWND and never fall back to foreground input. Mouse actions and exclusive
 * callbacks still require focused real input. The worker binds the request's captured window context
 * while executing so downstream capture/input helpers operate on the correct window.</p>
 */
@Slf4j
@Component
public class InputActionWorker {

    private static final long DETAILED_SLEEP_SAFETY_POLL_NANOS = TimeUnit.MILLISECONDS.toNanos(50L);

    private final InputActionQueue inputActionQueue;
    private final InputActionDeadLetter deadLetter;
    private final InputProvider inputProvider;
    private final WindowAwareInputCoordinator inputCoordinator;
    private final WindowTaskContextHolder windowTaskContextHolder;
    private final BoundWindowKeyboardService boundWindowKeyboardService;

    /**
     * Create the input worker.
     *
     * @param inputActionQueue producer queue.
     * @param deadLetter failed request recorder.
     * @param inputProvider real physical input provider.
     * @param inputCoordinator window-aware focus/input transaction coordinator.
     * @param windowTaskContextHolder current-window binding holder.
     * @param boundWindowKeyboardService hwnd/background keyboard helper for supported shortcuts.
     */
    public InputActionWorker(InputActionQueue inputActionQueue,
                             InputActionDeadLetter deadLetter,
                             InputProvider inputProvider,
                             WindowAwareInputCoordinator inputCoordinator,
                             WindowTaskContextHolder windowTaskContextHolder,
                             BoundWindowKeyboardService boundWindowKeyboardService) {
        this.inputActionQueue = inputActionQueue;
        this.deadLetter = deadLetter;
        this.inputProvider = inputProvider;
        this.inputCoordinator = inputCoordinator;
        this.windowTaskContextHolder = windowTaskContextHolder;
        this.boundWindowKeyboardService = boundWindowKeyboardService;
    }

    /**
     * Start the daemon worker thread after Spring construction.
     */
    @PostConstruct
    public void start() {
        Thread worker = new Thread(this::runLoop, "dhxy-input-action-worker");
        worker.setDaemon(true);
        worker.start();
        log.info("Input action worker started");
    }

    /**
     * Consume input requests until the worker thread is interrupted.
     */
    private void runLoop() {
        while (!Thread.currentThread().isInterrupted()) {
            InputActionRequest request;
            try {
                request = inputActionQueue.take();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            handle(request);
        }
    }

    /**
     * Execute one queued request and complete its waiting future.
     */
    private void handle(InputActionRequest request) {
        long latencyStart = LatencyMetrics.start();
        boolean completed = false;
        try {
            if (!waitIfPaused(request, "before-focus")) {
                request.complete(false, "worker-stopped-before-focus");
                deadLetter.record(request, null);
                return;
            }
            if (request.isCancelled()) {
                log.info("Input request skipped before focus because it was cancelled: windowId={} description={}",
                        request.getWindowId(), request.getDescription());
                request.complete(false, "cancelled-before-focus");
                return;
            }
            /*
             * A frozen request must not meet the generic identity-epoch comparator before its own typed safety.
             * That comparator answers only "did the epoch move" and cancels as WINDOW_BINDING_CHANGED, so a
             * request whose STOP and epoch/generation drift closed together published binding drift and buried
             * the higher-priority stop. Frozen ownership is therefore decided by typed safety first and the
             * frozen witness second; legacy requests keep their existing order untouched.
             */
            if (request.isFrozenExactWindow()) {
                if (!isFrozenExactWindowStillOwned(request, "before-focus")) {
                    deadLetter.record(request, null);
                    return;
                }
            } else {
                if (!isPlayerIdentityEpochCurrent(request, "before-focus")) {
                    deadLetter.record(request, null);
                    return;
                }
                if (!request.checkDetailedSafety("before-input-coordinator")) {
                    deadLetter.record(request, null);
                    return;
                }
            }
            boolean preferBackgroundKeyboard = canUseBackgroundKeyboard(request);
            boolean focusBeforeInput = request.isRetainedSessionMode()
                    || (request.hasExclusiveCallback()
                    ? request.isExclusiveCallbackFocusRequired()
                    : !preferBackgroundKeyboard);
            Boolean ok = windowTaskContextHolder.callWith(request.getWindowContext(), () ->
                    inputCoordinator.callInputTransaction("queued:" + request.getDescription(), false, () -> {
                        /*
                         * A frozen exact-window request owns the runtime-context generation monitor from its
                         * single authoritative check through focus, the callback and the callback's own
                         * finally. WindowNativeBindingRefreshService.refreshAndCommit commits only while
                         * holding the same monitor, so no binding drift can be interleaved into that span.
                         */
                        if (request.isFrozenExactWindow()) {
                            return request.hasExclusiveCallback()
                                    ? runFrozenExactWindowExclusive(request)
                                    : runFrozenExactWindowActions(request, preferBackgroundKeyboard);
                        }
                        if (focusBeforeInput) {
                            if (!waitIfPaused(request, "before-transaction-focus")
                                    || request.isCancelled()
                                    || !isPlayerIdentityEpochCurrent(request, "before-transaction-focus")
                                    || !request.checkDetailedSafety("before-transaction-focus")
                                    || !request.admitWorkerStart("before-transaction-focus")) {
                                return false;
                            }
                            inputCoordinator.focusCurrentWindowInActiveTransaction(
                                    "queued:" + request.getDescription());
                        }
                        return InputActionScope.callWith(request, () -> {
                        if (!waitIfPaused(request, "before-actions")
                                || request.isCancelled()
                                || !isPlayerIdentityEpochCurrent(request, "before-actions")
                                || !request.checkDetailedSafety("before-actions")
                                || !request.admitWorkerStart("before-actions")) {
                            return false;
                        }

                        if (request.isRetainedSessionMode()) {
                            if (!request.completeRetainedSessionAdmission()) {
                                return false;
                            }
                            return runRetainedSession(request, preferBackgroundKeyboard);
                        }
                        if (request.hasExclusiveCallback()) {
                            if (!waitIfPaused(request, "before-exclusive-callback")
                                    || !isPlayerIdentityEpochCurrent(request, "before-exclusive-callback")
                                    || !request.tryStartStep(0, "before-exclusive-callback")) {
                                return false;
                            }
                            boolean callbackCompleted = Boolean.TRUE.equals(request.getExclusiveCallback().get());
                            if (callbackCompleted
                                    && request.isFrozenExactWindow()
                                    && !request.checkDetailedSafety("after-exclusive-callback-cleanup")) {
                                return false;
                            }
                            if (callbackCompleted) {
                                request.markStepCompleted(0);
                            }
                            return callbackCompleted;
                        }
                        int actionIndex = 0;
                        for (InputAction action : request.getActions()) {
                            int stepIndex = actionIndex;
                            actionIndex++;
                            if (!waitIfPaused(request, "action-" + actionIndex)
                                    || request.isCancelled()
                                    || Thread.currentThread().isInterrupted()
                                    || !isPlayerIdentityEpochCurrent(request, "action-" + actionIndex)) {
                                return false;
                            }
                            log.info("[INPUT_TRACE] queued-action request={} windowId={} actionIndex={}/{} action={}",
                                    request.getDescription(), request.getWindowId(), actionIndex,
                                    request.getActions().size(), action);
                            if (!request.tryStartStep(stepIndex, "action-" + actionIndex)) {
                                return false;
                            }
                            if (!execute(request, action, preferBackgroundKeyboard, "action-" + actionIndex)) {
                                return false;
                            }
                            request.markStepCompleted(stepIndex);
                            if (request.hasDeadline() && request.isCancelled()) {
                                return false;
                            }
                        }
                        return true;
                    });
                    })
            );
            completed = Boolean.TRUE.equals(ok);
            request.complete(completed, completed ? "completed" : "worker-returned-false");
            if (!completed) {
                deadLetter.record(request, null);
            }
        } catch (TaskStopRequestedException e) {
            request.cancel(InputActionSafetyReason.STOP_REQUESTED, "task-stop:" + e.getMessage());
            deadLetter.record(request, e);
        } catch (Throwable e) {
            request.complete(false, "exception:" + e.getClass().getSimpleName() + ":" + e.getMessage());
            deadLetter.record(request, e);
        } finally {
            request.complete(false, "worker-exited-without-terminal-result");
            request.ensureRetainedSessionAdmission();
            request.completePendingRetainedSessionStep();
            LatencyMetrics.info(log, "input.request", latencyStart,
                    "result=" + completed + " request=" + request.getDescription()
                            + " windowId=" + request.getWindowId()
                            + " actions=" + request.getActions().size()
                            + " exclusive=" + request.hasExclusiveCallback());
            request.releaseRetainedTerminalPublication();
        }
    }

    private boolean runRetainedSession(
            InputActionRequest request,
            boolean preferBackgroundKeyboard) {
        while (true) {
            if (!waitIfPaused(request, "retained-session-idle")
                    || request.isCancelled()
                    || !isPlayerIdentityEpochCurrent(request, "retained-session-idle")
                    || !request.checkDetailedSafety("retained-session-idle")) {
                return false;
            }
            long remaining = request.remainingDeadlineNanos(System.nanoTime());
            if (remaining <= 0L) {
                request.expireDeadline("retained-session-idle");
                return false;
            }
            InputActionRequest.RetainedSessionSignal signal;
            try {
                signal = request.pollRetainedSessionSignal(
                        Math.min(TimeUnit.SECONDS.toNanos(1L), remaining));
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                request.requestDetailedCancellation(
                        "worker-interrupted:retained-session-idle");
                return false;
            }
            if (signal == null) {
                continue;
            }
            if (signal instanceof InputActionRequest.RetainedSessionTerminal) {
                return true;
            }
            InputActionRequest.RetainedSessionStep step =
                    (InputActionRequest.RetainedSessionStep) signal;
            boolean successful = false;
            try {
                String focusStage = "retained-step-focus";
                if (!waitIfPaused(request, focusStage)
                        || request.isCancelled()
                        || Thread.currentThread().isInterrupted()
                        || !isPlayerIdentityEpochCurrent(request, focusStage)
                        || !request.checkDetailedSafety(focusStage)) {
                    return false;
                }
                inputCoordinator.focusCurrentWindowInActiveTransaction(
                        "queued-retained-step:" + request.getDescription());
                if (!isPlayerIdentityEpochCurrent(request, focusStage + "-after")
                        || !request.checkDetailedSafety(focusStage + "-after")) {
                    return false;
                }
                int actionIndex = 0;
                for (InputAction action : step.actions()) {
                    int currentIndex = actionIndex++;
                    String stage = "retained-step-action-" + actionIndex;
                    if (!waitIfPaused(request, stage)
                            || request.isCancelled()
                            || Thread.currentThread().isInterrupted()
                            || !isPlayerIdentityEpochCurrent(request, stage)
                            || !request.checkDetailedSafety(stage)
                            || !request.tryStartRetainedAction(step, currentIndex, stage)) {
                        return false;
                    }
                    if (!execute(request, action, preferBackgroundKeyboard, stage)) {
                        return false;
                    }
                    if (!waitIfPaused(request, stage + "-after")
                            || request.isCancelled()
                            || Thread.currentThread().isInterrupted()
                            || !isPlayerIdentityEpochCurrent(request, stage + "-after")
                            || !request.checkDetailedSafety(stage + "-after")) {
                        return false;
                    }
                    request.markRetainedActionCompleted(step, currentIndex);
                }
                successful = true;
            } finally {
                InputActionExecutionResult terminalSnapshot =
                        request.retainedTerminalSnapshot();
                step.complete(
                        request.getRequestId(),
                        successful,
                        successful ? "completed" : request.getCancellationReason(),
                        terminalSnapshot == null
                                ? InputActionSafetyReason.CLEAR
                                : terminalSnapshot.getSafetyReason());
            }
        }
    }

    /**
     * Execute one action. Coordinates in the action are already screen-absolute.
     */
    private boolean execute(InputActionRequest request, InputAction action, boolean preferBackgroundKeyboard, String stage) {
        InputActionType type = action.getType();
        if (type == InputActionType.CLICK_LEFT) {
            inputProvider.clickLeft(action.getX(), action.getY(), action.getDelayMs());
        } else if (type == InputActionType.CLICK_RIGHT) {
            inputProvider.clickRight(action.getX(), action.getY(), action.getDelayMs());
        } else if (type == InputActionType.DOUBLE_RIGHT_CLICK) {
            inputProvider.doubleRightClick(action.getX(), action.getY(), action.getDelayMs(), action.getIntervalMs());
        } else if (type == InputActionType.MOVE_MOUSE) {
            inputProvider.moveMouse(action.getX(), action.getY());
        } else if (type == InputActionType.DRAG_AND_DROP) {
            inputProvider.dragAndDrop(action.getX(), action.getY(), action.getEndX(), action.getEndY());
        } else if (isBackgroundKeyboardAction(type)) {
            return executeBackgroundKeyboard(request, action);
        } else if (type == InputActionType.PASTE_TEXT) {
            log.warn("Clipboard paste rejected because foreground keyboard fallback is disabled: windowId={} request={}",
                    request.getWindowId(), request.getDescription());
            return false;
        } else if (isAltShortcutAction(type)) {
            return pressAltShortcut(request, type, preferBackgroundKeyboard, stage);
        } else if (type == InputActionType.SCROLL_DOWN) {
            inputProvider.scrollDown(action.getClicks());
        } else if (type == InputActionType.SCROLL_UP) {
            inputProvider.scrollUp(action.getClicks());
        } else if (type == InputActionType.SLEEP) {
            if (!request.hasDeadline()) {
                TaskSleep.sleep(action.getDelayMs());
            } else {
                return executeDetailedSleep(request, action.getDelayMs(), stage);
            }
        } else {
            throw new IllegalArgumentException("Unsupported input action: " + type);
        }
        return !Thread.currentThread().isInterrupted();
    }

    private boolean waitIfPaused(InputActionRequest request, String stage) {
        if (request.excludesPauseFromDeadline()) {
            boolean pausedAtBoundary = request.isPauseRequested();
            if (pausedAtBoundary) {
                log.info("Input request paused; waiting before continuing: windowId={} description={} stage={}",
                        request.getWindowId(), request.getDescription(), stage);
            }
            TaskPauseToken.PauseWaitSnapshot snapshot = request.getPauseToken()
                    .waitIfPausedRevision(request.getStopToken(), request::shouldAbortPauseWait);
            long newlyAccountedNanos = request.compensatePause(snapshot);
            if (pausedAtBoundary || newlyAccountedNanos > 0L) {
                log.info("Input request pause progress accounted: windowId={} description={} stage={} newlyAccountedMs={}",
                        request.getWindowId(), request.getDescription(), stage,
                        TimeUnit.NANOSECONDS.toMillis(newlyAccountedNanos));
            }
            return !request.isCancelled() && !Thread.currentThread().isInterrupted();
        }
        /*
         * A frozen request has no deadline, so it would otherwise land on the legacy wait below, which only
         * observes the stop token. A waiter that gave up and cancelled the request could then never end this
         * wait: the worker would stay parked holding the global input transaction until an unrelated resume,
         * and the waiter would block on a terminal future that nobody was going to publish. The existing
         * revision wait already takes a wake condition, so cancellation ends the wait cooperatively — without
         * a resume, without re-enqueueing or replaying anything, and without releasing the frozen transaction
         * to another request.
         */
        if (request.isFrozenExactWindow()) {
            TaskPauseToken pauseToken = request.getPauseToken();
            if (pauseToken != null) {
                if (request.isPauseRequested()) {
                    log.info("Frozen input request paused; waiting cooperatively: windowId={} description={} stage={}",
                            request.getWindowId(), request.getDescription(), stage);
                }
                pauseToken.waitIfPausedRevision(request.getStopToken(), request::shouldAbortPauseWait);
            }
            return !request.isCancelled() && !Thread.currentThread().isInterrupted();
        }
        if (!request.isPauseRequested()) {
            return true;
        }
        log.info("Input request paused; waiting before continuing: windowId={} description={} stage={}",
                request.getWindowId(), request.getDescription(), stage);
        long blockedMs = request.getPauseToken().waitIfPaused(request.getStopToken());
        log.info("Input request resumed; continuing same sequence: windowId={} description={} stage={} blockedMs={}",
                request.getWindowId(), request.getDescription(), stage, blockedMs);
        return !request.isCancelled() && !Thread.currentThread().isInterrupted();
    }

    /**
     * Run one frozen exact-window exclusive callback under a single runtime-context generation monitor.
     *
     * <p>Pause waiting and worker admission stay outside the monitor so a paused or rejected request never
     * blocks binding commits. Everything that must observe one indivisible generation — the single
     * authoritative exact check, the explicit frozen focus, {@code tryStartStep(0)}, the callback and the
     * callback's own {@code finally} — runs while this thread holds {@code synchronized (context)}.
     * {@code WindowNativeBindingRefreshService.refreshAndCommit} commits only under the same monitor, so no
     * A -> B -> A drift can be interleaved between the check and the mechanics, and no action can bind an
     * old absolute ROI to a newer context generation.</p>
     *
     * @param request frozen exact-window request carrying an exclusive callback
     * @return true only when the callback completed and its post-callback cleanup safety still holds
     */
    private boolean runFrozenExactWindowExclusive(InputActionRequest request) {
        if (!waitIfPaused(request, "before-frozen-exclusive")
                || request.isCancelled()
                || !request.checkDetailedSafety("before-frozen-exclusive")
                || !request.admitWorkerStart("before-frozen-exclusive")) {
            return false;
        }
        WindowRuntimeContext context = request.getWindowContext();
        if (context == null) {
            request.cancel(InputActionSafetyReason.WINDOW_BINDING_CHANGED,
                    "frozen-window-context-missing:before-frozen-exclusive");
            return false;
        }
        synchronized (context) {
            /*
             * Taking the monitor is a fresh boundary: anything could have closed while this thread waited for
             * it. Recheck in priority order — typed safety, then the witness — so a stop that closed during the
             * wait is reported as a stop rather than as drift discovered by the witness first.
             */
            if (!isFrozenExactWindowStillOwned(request, "before-frozen-focus")) {
                return false;
            }
            inputCoordinator.focusFrozenBindingInActiveTransaction(
                    "queued:" + request.getDescription(),
                    request.getWindowId(),
                    request.getNativeBinding());
            return Boolean.TRUE.equals(InputActionScope.callWith(request, () -> {
                if (!request.tryStartStep(0, "before-exclusive-callback")) {
                    return false;
                }
                boolean callbackCompleted = Boolean.TRUE.equals(request.getExclusiveCallback().get());
                if (callbackCompleted && !request.checkDetailedSafety("after-exclusive-callback-cleanup")) {
                    return false;
                }
                if (callbackCompleted) {
                    request.markStepCompleted(0);
                }
                return callbackCompleted;
            }));
        }
    }

    /**
     * Executes one frozen exact-window request's complete action list under the same boundary the frozen
     * callback path uses.
     *
     * <p>The cancellation, detailed-safety and worker-admission gates run before the monitor is taken,
     * exactly as {@link #runFrozenExactWindowExclusive} does. Inside the monitor the single authoritative
     * generation check, the explicit frozen focus and every action and delay run without releasing it, so
     * {@code WindowNativeBindingRefreshService.refreshAndCommit} — which commits under the same monitor —
     * cannot interleave a binding drift between list elements.</p>
     *
     * <p>Every action re-checks the pause gate before it starts, so a pause that arrives mid-list stops the
     * list where it is: zero later actions run while paused. The wait happens inside the transaction and the
     * generation monitor on purpose. The request is atomic and is never split or re-enqueued: a resume
     * continues the very same request, at the very same step, under the very same frozen generation, so no
     * action is retried or replayed and no stale snapshot can be spliced onto a newer generation. Holding the
     * monitor across the wait is what makes that guarantee true, at the cost of blocking a concurrent
     * refresh/commit for this one context while a paused list is outstanding.</p>
     *
     * <p>Each action is dispatched by the shared {@link #execute} dispatcher and bracketed by the existing
     * per-step typed progress, so no input mechanics are duplicated here. Drift, cancellation, interruption
     * and safety failure are terminal and typed: the loop returns immediately and no later action runs. A
     * stop is projected by that same per-action pause wait as {@link TaskStopRequestedException} and is
     * mapped by the caller's existing catch to {@code STOP_REQUESTED}. Nothing is retried or replayed.</p>
     *
     * @param request frozen exact-window request carrying the complete action list
     * @param preferBackgroundKeyboard whether the complete action list needs no foreground mouse input
     * @return true only when every action completed under one unbroken frozen generation
     */
    private boolean runFrozenExactWindowActions(InputActionRequest request, boolean preferBackgroundKeyboard) {
        if (!waitIfPaused(request, "before-frozen-actions")
                || request.isCancelled()
                || !request.checkDetailedSafety("before-frozen-actions")
                || !request.admitWorkerStart("before-frozen-actions")) {
            return false;
        }
        WindowRuntimeContext context = request.getWindowContext();
        if (context == null) {
            request.cancel(InputActionSafetyReason.WINDOW_BINDING_CHANGED,
                    "frozen-window-context-missing:before-frozen-actions");
            return false;
        }
        synchronized (context) {
            /*
             * Same fresh-boundary rule as the callback path: typed safety first, then the witness, immediately
             * before exact focus.
             */
            if (!isFrozenExactWindowStillOwned(request, "before-frozen-focus")) {
                return false;
            }
            if (!preferBackgroundKeyboard) {
                inputCoordinator.focusFrozenBindingInActiveTransaction(
                        "queued:" + request.getDescription(),
                        request.getWindowId(),
                        request.getNativeBinding());
            }
            return Boolean.TRUE.equals(InputActionScope.callWith(request, () -> {
                int actionIndex = 0;
                for (InputAction action : request.getActions()) {
                    int stepIndex = actionIndex;
                    actionIndex++;
                    String stage = "frozen-action-" + actionIndex;
                    /*
                     * Re-read the pause gate before every action, not once before the list: a pause that
                     * arrives after an earlier action must stop the list here rather than let the rest of it
                     * through. This same wait projects a stop as TaskStopRequestedException.
                     */
                    if (!waitIfPaused(request, stage)
                            || request.isCancelled()
                            || Thread.currentThread().isInterrupted()) {
                        return false;
                    }
                    if (!isFrozenExactWindowStillOwned(request, stage)) {
                        return false;
                    }
                    log.info("[INPUT_TRACE] frozen-action request={} windowId={} actionIndex={}/{} action={}",
                            request.getDescription(), request.getWindowId(), actionIndex,
                            request.getActions().size(), action);
                    if (!request.tryStartStep(stepIndex, stage)) {
                        return false;
                    }
                    if (!execute(request, action, preferBackgroundKeyboard, stage)) {
                        return false;
                    }
                    request.markStepCompleted(stepIndex);
                }
                /*
                 * Final gate before success. Without it a stop or drift that closed during the LAST action has
                 * no boundary left to observe it, and the request would publish success for a list that
                 * finished under a window generation, or a task lifecycle, that no longer holds.
                 */
                return isFrozenExactWindowStillOwned(request, "frozen-actions-complete");
            }));
        }
    }

    /**
     * Frozen-window ownership gate: typed safety first, then the object-identity generation witness.
     *
     * <p>Order is the contract. {@code checkDetailedSafety} preserves the real reason — a closed stop stays
     * {@code STOP_REQUESTED} — while the witness answers only the question a value comparison cannot see: is
     * the context still publishing the very binding object this request froze. Consulting the witness first
     * would relabel every stop as binding drift.</p>
     *
     * @return true only when this request still owns the exact frozen window and no typed gate is closed
     */
    private boolean isFrozenExactWindowStillOwned(InputActionRequest request, String stage) {
        if (!request.checkDetailedSafety(stage)) {
            return false;
        }
        if (!request.isFrozenExactWindowGenerationCurrent()) {
            request.cancel(InputActionSafetyReason.WINDOW_BINDING_CHANGED,
                    "frozen-generation-changed:" + stage);
            return false;
        }
        return true;
    }

    private boolean isPlayerIdentityEpochCurrent(InputActionRequest request, String stage) {
        if (request.isPlayerIdentityEpochCurrent()) {
            return true;
        }
        long currentEpoch = request.getWindowContext() == null
                ? -1L
                : request.getWindowContext().getPlayerIdentityEpoch();
        log.warn("Input request skipped because player identity epoch changed: windowId={} description={} stage={} requestEpoch={} currentEpoch={}",
                request.getWindowId(), request.getDescription(), stage,
                request.getPlayerIdentityEpoch(), currentEpoch);
        request.cancel(
                InputActionSafetyReason.WINDOW_BINDING_CHANGED,
                "player-identity-epoch-changed:" + stage);
        return false;
    }

    /**
     * Press an Alt shortcut through exact-HWND delivery. Foreground keyboard fallback is forbidden.
     */
    private boolean pressAltShortcut(InputActionRequest request,
                                     InputActionType type,
                                     boolean preferBackgroundKeyboard,
                                     String stage) {
        BoundWindowKeyboardService.AltShortcut shortcut = toAltShortcut(type);
        /*
         * A frozen request owns an exact binding and must not let any keyboard/focus path re-resolve or
         * refresh the window from mutable current state: that would commit a new generation inside the very
         * monitor this request holds. Both exact-binding overloads already exist, so the frozen path uses them
         * and the legacy path keeps its existing behavior byte-for-byte.
         */
        boolean frozen = request.isFrozenExactWindow();
        BoundWindowKeyboardService.ShortcutAttempt attempt = frozen
                ? boundWindowKeyboardService.pressShortcut(
                        request.getNativeBinding(), request.getWindowId(), shortcut)
                : boundWindowKeyboardService.pressShortcut(
                        request.getNativeBinding(), request.getWindowId(), shortcut);
        if (attempt.attempted() && attempt.success()) {
            return true;
        }
        log.warn("HWND {} failed; foreground keyboard fallback is disabled: windowId={} reason={}",
                shortcutDisplayName(shortcut, type), request.getWindowId(), attempt.reason());
        return false;
    }

    /**
     * Sleep in short detailed-only segments so monotonic deadline, external stop, and identity
     * suspension can terminate the current step without marking it complete.
     */
    private boolean executeDetailedSleep(InputActionRequest request, int delayMs, String stage) {
        long requestedSleepNanos = TimeUnit.MILLISECONDS.toNanos(Math.max(0L, delayMs));
        long sleepPauseProgressNanos = request.excludesPauseFromDeadline()
                ? request.getPauseToken().pauseProgress().cumulativePauseNanos()
                : 0L;
        long sleepUntilNanos = saturatingAdd(System.nanoTime(), requestedSleepNanos);
        while (true) {
            if (request.excludesPauseFromDeadline()) {
                TaskPauseToken.PauseWaitSnapshot snapshot = request.getPauseToken()
                        .waitIfPausedRevision(request.getStopToken(), request::shouldAbortPauseWait);
                request.compensatePause(snapshot);
                if (snapshot.cumulativePauseNanos() > sleepPauseProgressNanos) {
                    sleepUntilNanos = saturatingAdd(
                            sleepUntilNanos,
                            snapshot.cumulativePauseNanos() - sleepPauseProgressNanos);
                    sleepPauseProgressNanos = snapshot.cumulativePauseNanos();
                }
            }
            if (!request.checkDetailedSafety(stage + "-sleep-segment")) {
                return false;
            }
            long nowNanos = System.nanoTime();
            long remainingSleepNanos = sleepUntilNanos - nowNanos;
            if (remainingSleepNanos <= 0L) {
                return true;
            }
            long remainingDeadlineNanos = request.remainingDeadlineNanos(nowNanos);
            if (remainingDeadlineNanos <= 0L) {
                request.expireDeadline(stage + "-sleep-segment");
                return false;
            }
            long segmentNanos = Math.min(
                    remainingSleepNanos,
                    Math.min(remainingDeadlineNanos, DETAILED_SLEEP_SAFETY_POLL_NANOS));
            long segmentMs = Math.max(1L, TimeUnit.NANOSECONDS.toMillis(segmentNanos));
            if (TimeUnit.MILLISECONDS.toNanos(segmentMs) < segmentNanos) {
                segmentMs++;
            }
            if (!TaskSleep.sleep(segmentMs)) {
                return false;
            }
        }
    }

    private static long saturatingAdd(long left, long right) {
        if (right <= 0L) {
            return left;
        }
        return left > Long.MAX_VALUE - right ? Long.MAX_VALUE : left + right;
    }

    /**
     * @return true when every action is sleep or an exact-HWND keyboard operation.
     */
    private boolean canUseBackgroundKeyboard(InputActionRequest request) {
        if (request.hasExclusiveCallback()) {
            return false;
        }
        if (request.getActions().isEmpty()) {
            return false;
        }
        for (InputAction action : request.getActions()) {
            InputActionType type = action.getType();
            if (type != InputActionType.SLEEP
                    && !isBackgroundKeyboardAction(type)
                    && toAltShortcut(type) == null
                    && type != InputActionType.PASTE_TEXT) {
                return false;
            }
        }
        return true;
    }

    private BoundWindowKeyboardService.AltShortcut toAltShortcut(InputActionType type) {
        if (type == InputActionType.PRESS_ALT_1) {
            return BoundWindowKeyboardService.AltShortcut.ALT_1;
        }
        if (type == InputActionType.PRESS_ALT_2) {
            return BoundWindowKeyboardService.AltShortcut.ALT_2;
        }
        if (type == InputActionType.PRESS_ALT_4) {
            return BoundWindowKeyboardService.AltShortcut.ALT_4;
        }
        if (type == InputActionType.PRESS_ALT_6) {
            return BoundWindowKeyboardService.AltShortcut.ALT_6;
        }
        if (type == InputActionType.PRESS_ALT_8) {
            return BoundWindowKeyboardService.AltShortcut.ALT_8;
        }
        if (type == InputActionType.PRESS_ALT_T) {
            return BoundWindowKeyboardService.AltShortcut.ALT_T;
        }
        if (type == InputActionType.PRESS_ALT_O) {
            return BoundWindowKeyboardService.AltShortcut.ALT_O;
        }
        if (type == InputActionType.PRESS_ALT_E) {
            return BoundWindowKeyboardService.AltShortcut.ALT_E;
        }
        if (type == InputActionType.PRESS_ALT_Q) {
            return BoundWindowKeyboardService.AltShortcut.ALT_Q;
        }
        if (type == InputActionType.PRESS_ALT_A) {
            return BoundWindowKeyboardService.AltShortcut.ALT_A;
        }
        if (type == InputActionType.PRESS_ALT_C) {
            return BoundWindowKeyboardService.AltShortcut.ALT_C;
        }
        if (type == InputActionType.PRESS_ALT_U) {
            return BoundWindowKeyboardService.AltShortcut.ALT_U;
        }
        return null;
    }

    /** Execute one non-Alt keyboard action against the request's immutable HWND without foreground fallback. */
    private boolean executeBackgroundKeyboard(InputActionRequest request, InputAction action) {
        BoundWindowKeyboardService.ShortcutAttempt attempt;
        InputActionType type = action.getType();
        if (type == InputActionType.HOLD_CTRL || type == InputActionType.RELEASE_CTRL) {
            BoundWindowKeyboardService.KeyTransition transition = type == InputActionType.HOLD_CTRL
                    ? BoundWindowKeyboardService.KeyTransition.DOWN
                    : BoundWindowKeyboardService.KeyTransition.UP;
            BoundWindowKeyboardService.KeyTransitionAttempt result = boundWindowKeyboardService.transitionModifier(
                    request.getNativeBinding(), request.getWindowId(),
                    BoundWindowKeyboardService.ModifierKey.CONTROL, transition);
            if (result.attempted() && result.success()) {
                return true;
            }
            log.warn("HWND Ctrl transition failed; foreground keyboard fallback is disabled: windowId={} transition={} reason={}",
                    request.getWindowId(), transition, result.reason());
            return false;
        }
        if (type == InputActionType.PRESS_CTRL_U) {
            attempt = boundWindowKeyboardService.pressControlShortcut(
                    request.getNativeBinding(), request.getWindowId(),
                    BoundWindowKeyboardService.ControlShortcut.CTRL_U);
        } else if (type == InputActionType.TYPE_TEXT_UNICODE) {
            attempt = boundWindowKeyboardService.typeUnicodeText(
                    request.getNativeBinding(), request.getWindowId(), action.getText());
        } else if (type == InputActionType.PRESS_ENTER) {
            attempt = boundWindowKeyboardService.pressEnter(request.getNativeBinding(), request.getWindowId());
        } else {
            throw new IllegalArgumentException("Unsupported background keyboard action: " + type);
        }
        if (attempt.attempted() && attempt.success()) {
            return true;
        }
        log.warn("HWND keyboard action failed; foreground keyboard fallback is disabled: windowId={} action={} reason={}",
                request.getWindowId(), type, attempt.reason());
        return false;
    }

    private boolean isBackgroundKeyboardAction(InputActionType type) {
        return type == InputActionType.HOLD_CTRL
                || type == InputActionType.RELEASE_CTRL
                || type == InputActionType.PRESS_CTRL_U
                || type == InputActionType.TYPE_TEXT_UNICODE
                || type == InputActionType.PRESS_ENTER;
    }

    private String shortcutDisplayName(BoundWindowKeyboardService.AltShortcut shortcut, InputActionType fallbackType) {
        return shortcut == null ? fallbackType.name() : shortcut.displayName();
    }

    private boolean isAltShortcutAction(InputActionType type) {
        return type == InputActionType.PRESS_ALT_1
                || type == InputActionType.PRESS_ALT_2
                || type == InputActionType.PRESS_ALT_4
                || type == InputActionType.PRESS_ALT_6
                || type == InputActionType.PRESS_ALT_8
                || type == InputActionType.PRESS_ALT_T
                || type == InputActionType.PRESS_ALT_O
                || type == InputActionType.PRESS_ALT_E
                || type == InputActionType.PRESS_ALT_Q
                || type == InputActionType.PRESS_ALT_A
                || type == InputActionType.PRESS_ALT_C
                || type == InputActionType.PRESS_ALT_U;
    }
}
