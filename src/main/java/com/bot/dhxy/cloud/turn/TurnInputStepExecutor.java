package com.bot.dhxy.cloud.turn;

import com.bot.dhxy.cloud.turn.protocol.TurnInputAction;
import com.bot.dhxy.cloud.turn.protocol.TurnInputSpec;
import com.bot.dhxy.cloud.turn.protocol.TurnStep;
import com.bot.dhxy.cloud.turn.protocol.TurnStepType;
import com.bot.dhxy.driver.BoundWindowKeyboardService;
import com.bot.dhxy.input.action.InputAction;
import com.bot.dhxy.input.action.InputActionExecutionResult;
import com.bot.dhxy.input.action.InputActionQueue;
import com.bot.dhxy.input.action.InputActionSafetyReason;
import com.bot.dhxy.runner.stop.TaskPauseToken;
import com.bot.dhxy.runner.stop.TaskSleep;
import com.bot.dhxy.runner.stop.TaskStopRequestedException;
import com.bot.dhxy.runner.stop.TaskStopToken;
import com.bot.dhxy.window.runtime.WindowTaskContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/** Executes closed turn input mechanics through the existing exact-window serialization boundaries. */
@Component
@Slf4j
public final class TurnInputStepExecutor {

    private final InputActionQueue inputActionQueue;
    private final BoundWindowKeyboardService keyboardService;
    private final WindowTaskContextHolder contextHolder;
    private final TurnInputActionMapper inputActionMapper;
    private final TurnKeyMapper keyMapper;

    public TurnInputStepExecutor(InputActionQueue inputActionQueue,
                                 BoundWindowKeyboardService keyboardService,
                                 WindowTaskContextHolder contextHolder,
                                 TurnInputActionMapper inputActionMapper,
                                 TurnKeyMapper keyMapper) {
        this.inputActionQueue = Objects.requireNonNull(inputActionQueue, "inputActionQueue");
        this.keyboardService = Objects.requireNonNull(keyboardService, "keyboardService");
        this.contextHolder = Objects.requireNonNull(contextHolder, "contextHolder");
        this.inputActionMapper = Objects.requireNonNull(inputActionMapper, "inputActionMapper");
        this.keyMapper = Objects.requireNonNull(keyMapper, "keyMapper");
    }

    /**
     * Execute one typed INPUT step for the exact refreshed action window.
     *
     * @param window immutable per-action window snapshot; coordinates are screen-absolute pixels.
     * @param action closed input operation.
     * @param input operation-specific typed fields; non-null.
     * @param sourceStepIndex zero-based action step index used only for diagnostics.
     * @return typed mechanical result; unsupported keyboard forms never fall back to foreground input.
     */
    public Result execute(TurnExecutionWindow window,
                          TurnInputAction action,
                          TurnInputSpec input,
                          int sourceStepIndex) {
        Objects.requireNonNull(window, "window");
        Objects.requireNonNull(action, "action");
        Objects.requireNonNull(input, "input");
        if (window.metadata().stopRequested() || Thread.currentThread().isInterrupted()) {
            return Result.stopped("stop requested before input step");
        }

        try {
            if (inputActionMapper.isMouse(action)) {
                List<InputAction> actions = inputActionMapper.mapMouse(
                        action, input, window.metadata().windowRect());
                return submitMouseActions(
                        window,
                        "turn:input:step-" + sourceStepIndex + ":" + action,
                        actions);
            }

            // Baseline-supported Alt taps use the existing serialized worker so its HWND attempt and
            // focused fallback stay in one input transaction. Other background-only keyboard forms
            // keep their exact-HWND admission path and never invent a second queue.
            switch (action) {
                case KEY_TAP:
                    return deliverKeyTap(window, input);
                case KEY_DOWN:
                    return deliverModifierDown(window, input);
                case KEY_UP:
                    return deliverModifierRelease(window, input);
                case TEXT_INPUT:
                    return deliverText(window, input);
                default:
                    return Result.failed(
                            Code.BACKGROUND_KEY_UNSUPPORTED,
                            "existing HWND keyboard API cannot express " + action);
            }
        } catch (IllegalArgumentException invalid) {
            return Result.failed(Code.INVALID_INPUT, invalid.getMessage());
        }
    }

    /**
     * Deliver one KEY_TAP through the closed background vocabulary: existing Alt shortcut, then Ctrl chord,
     * then Enter. Unsupported spellings fail without any foreground fallback. The actual irreversible post runs
     * through the live pre-delivery admission.
     */
    private Result deliverKeyTap(TurnExecutionWindow window, TurnInputSpec input) {
        String key = input.key();
        BoundWindowKeyboardService.AltShortcut alt = keyMapper.findBackgroundTap(key).orElse(null);
        if (alt != null) {
            InputAction serializedAlt = toSerializedAltAction(alt);
            if (serializedAlt != null) {
                return submitMouseActions(
                        window,
                        "turn:input:keyboard:" + alt.name(),
                        List.of(serializedAlt));
            }
            return deliverKeyboardWithLiveAdmission(window, () -> toResult(keyboardService.pressShortcut(
                    window.binding(), window.metadata().windowId(), alt)));
        }
        BoundWindowKeyboardService.ControlShortcut ctrl = keyMapper.findControlShortcut(key).orElse(null);
        if (ctrl != null) {
            return deliverKeyboardWithLiveAdmission(window, () -> toResult(keyboardService.pressControlShortcut(
                    window.binding(), window.metadata().windowId(), ctrl)));
        }
        if (keyMapper.isEnterKey(key)) {
            return deliverKeyboardWithLiveAdmission(window, () -> toResult(keyboardService.pressEnter(
                    window.binding(), window.metadata().windowId())));
        }
        return Result.failed(Code.BACKGROUND_KEY_UNSUPPORTED, "key is not background-validated: " + key);
    }

    /** Maps the baseline worker-supported Alt vocabulary to its existing serialized action. */
    private InputAction toSerializedAltAction(BoundWindowKeyboardService.AltShortcut shortcut) {
        return switch (shortcut) {
            case ALT_1 -> InputAction.pressAlt1();
            case ALT_2 -> InputAction.pressAlt2();
            case ALT_4 -> InputAction.pressAlt4();
            case ALT_6 -> InputAction.pressAlt6();
            case ALT_8 -> InputAction.pressAlt8();
            case ALT_T -> InputAction.pressAltT();
            case ALT_O -> InputAction.pressAltO();
            case ALT_E -> InputAction.pressAltE();
            case ALT_Q -> InputAction.pressAltQ();
            case ALT_A -> InputAction.pressAltA();
            case ALT_C -> InputAction.pressAltC();
            case ALT_U -> InputAction.pressAltU();
            case ALT_5 -> null;
        };
    }

    /** Deliver one KEY_DOWN modifier press through the live pre-delivery admission. */
    private Result deliverModifierDown(TurnExecutionWindow window, TurnInputSpec input) {
        BoundWindowKeyboardService.ModifierKey modifier = keyMapper.findModifierKey(input.key()).orElse(null);
        if (modifier == null) {
            return Result.failed(
                    Code.BACKGROUND_KEY_UNSUPPORTED,
                    "modifier is not background-validated: " + input.key());
        }
        return deliverKeyboardWithLiveAdmission(window, () -> toTransitionResult(keyboardService.transitionModifier(
                window.binding(), window.metadata().windowId(), modifier,
                BoundWindowKeyboardService.KeyTransition.DOWN)));
    }

    /**
     * Deliver one KEY_UP modifier release. Release is cleanup: it must remain callable even under a live stop,
     * pause or interrupt (mirroring the existing {@code transitionModifier} "UP remains callable while interrupted"
     * contract), so it deliberately bypasses the live-stop/pause/generation admission and posts to the frozen
     * binding directly, leaving no modifier held down.
     */
    private Result deliverModifierRelease(TurnExecutionWindow window, TurnInputSpec input) {
        BoundWindowKeyboardService.ModifierKey modifier = keyMapper.findModifierKey(input.key()).orElse(null);
        if (modifier == null) {
            return Result.failed(
                    Code.BACKGROUND_KEY_UNSUPPORTED,
                    "modifier is not background-validated: " + input.key());
        }
        return toTransitionResult(keyboardService.transitionModifier(
                window.binding(), window.metadata().windowId(), modifier,
                BoundWindowKeyboardService.KeyTransition.UP));
    }

    /** Deliver one TEXT_INPUT as an ordered exact-HWND background Unicode post through the live admission. */
    private Result deliverText(TurnExecutionWindow window, TurnInputSpec input) {
        String text = input.text();
        if (text == null || text.isEmpty()) {
            return Result.failed(Code.INVALID_INPUT, "text input requires non-empty text");
        }
        return deliverKeyboardWithLiveAdmission(window, () -> toResult(keyboardService.typeUnicodeText(
                window.binding(), window.metadata().windowId(), text)));
    }

    /**
     * Reinstate the frozen queue's live safety admission for a direct keyboard post that deliberately bypasses the
     * mouse queue. The keyboard path still owns no queue, store, global lock or focus; this only restores the
     * pause/stop/binding-generation guarantees the queue used to provide.
     *
     * <p>First honor the existing pause contract with the existing event-based wait (no keyboard queue/store, no
     * poll-sleep): {@code waitIfPaused} blocks while paused and throws a typed stop if a stop lands during the
     * wait. Then, serialized against the context generation monitor so a binding commit cannot interleave between
     * the check and the first post, reject a live task stop and a thread interrupt as typed {@code STOPPED}, and
     * reject any binding generation whose current context binding object is no longer the exact frozen object as a
     * typed failure. Only after all checks pass is the irreversible post performed inside the same monitor.</p>
     */
    private Result deliverKeyboardWithLiveAdmission(TurnExecutionWindow window, Supplier<Result> post) {
        // Consume only the exact action window's frozen stop/pause tokens. Production turn threads
        // ({@code dhxy-turn-*}) never bind a TaskExecutionContextHolder, so the frozen snapshot is the sole
        // live-safety source; it is captured through the same production resolve seam the tests exercise.
        TaskStopToken stopToken = window.actionStopToken();
        TaskPauseToken pauseToken = window.actionPauseToken();

        if (pauseToken != null) {
            try {
                pauseToken.waitIfPaused(stopToken);
            } catch (TaskStopRequestedException stopped) {
                return Result.stopped("task stop during keyboard pause wait");
            }
        }

        synchronized (window.context()) {
            if (stopToken != null && stopToken.isStopRequested()) {
                return Result.stopped("live task stop before keyboard delivery");
            }
            if (Thread.currentThread().isInterrupted()) {
                return Result.stopped("interrupted before keyboard delivery");
            }
            if (window.context().getNativeBinding() != window.binding()) {
                return Result.failed(
                        Code.BACKGROUND_KEY_FAILED,
                        "frozen window generation changed before keyboard delivery");
            }
            return post.get();
        }
    }

    /** Map one modifier transition attempt to a typed result. */
    private Result toTransitionResult(BoundWindowKeyboardService.KeyTransitionAttempt attempt) {
        if (attempt.success()) {
            return Result.completed();
        }
        return Result.failed(
                Code.BACKGROUND_KEY_FAILED,
                attempt.reason() == null ? "background keyboard delivery failed" : attempt.reason());
    }

    /** Map one background delivery attempt to a typed result; a post-delivery interrupt is a stop, not a failure. */
    private Result toResult(BoundWindowKeyboardService.ShortcutAttempt attempt) {
        if (Thread.currentThread().isInterrupted()) {
            return Result.stopped("background keyboard delivery interrupted");
        }
        if (attempt.success()) {
            return Result.completed();
        }
        return Result.failed(
                Code.BACKGROUND_KEY_FAILED,
                attempt.reason() == null ? "background keyboard delivery failed" : attempt.reason());
    }

    /**
     * Submit one closed mouse/positive-WAIT fragment as one indivisible input-queue request.
     *
     * @param window immutable per-action window snapshot; all input coordinates are screen-absolute pixels.
     * @param steps ordered original action steps; the first and last must be mouse INPUT steps and every
     *              interior step must be either mouse INPUT or a positive WAIT.
     * @return one typed result for the whole queue request; callers expand success to the original step indexes.
     */
    public MouseSequenceResult executeMouseSequence(TurnExecutionWindow window, List<TurnStep> steps) {
        Objects.requireNonNull(window, "window");
        Objects.requireNonNull(steps, "steps");
        if (window.metadata().stopRequested() || Thread.currentThread().isInterrupted()) {
            return MouseSequenceResult.beforeStart(
                    Result.stopped("stop requested before mouse input sequence"));
        }
        if (steps.isEmpty()) {
            return MouseSequenceResult.beforeStart(
                    Result.failed(Code.INVALID_INPUT, "mouse input sequence must not be empty"));
        }

        List<TurnStep> sequence = List.copyOf(steps);
        TurnStep first = sequence.get(0);
        TurnStep last = sequence.get(sequence.size() - 1);
        if (!isMouseInput(first) || !isMouseInput(last)) {
            return MouseSequenceResult.beforeStart(Result.failed(
                    Code.INVALID_INPUT, "mouse input sequence must start and end with mouse INPUT"));
        }

        try {
            List<InputAction> actions = new ArrayList<>();
            List<Integer> stepEndActionIndexes = new ArrayList<>(sequence.size());
            for (TurnStep step : sequence) {
                if (isMouseInput(step)) {
                    actions.addAll(inputActionMapper.mapMouse(
                            step.inputAction(), step.input(), window.metadata().windowRect()));
                    stepEndActionIndexes.add(actions.size() - 1);
                    continue;
                }
                if (step.type() != TurnStepType.WAIT
                        || step.waitMs() == null
                        || step.waitMs() <= 0L
                        || step.waitMs() > Integer.MAX_VALUE) {
                    return MouseSequenceResult.beforeStart(Result.failed(
                            Code.INVALID_INPUT,
                            "mouse input sequence may contain only mouse INPUT and positive bounded WAIT steps"));
                }
                actions.add(InputAction.sleep(step.waitMs().intValue()));
                stepEndActionIndexes.add(actions.size() - 1);
            }
            InputActionExecutionResult execution = submitMouseActionsRaw(
                    window, "turn:input:steps-" + first.index() + "-" + last.index() + ":mouse-sequence",
                    List.copyOf(actions));
            Result result = toResult(execution);
            if (result.status() == Status.COMPLETED) {
                return new MouseSequenceResult(result, sequence.size());
            }
            int completedTurnSteps = completedTurnStepCount(stepEndActionIndexes, execution);
            return new MouseSequenceResult(result, Math.max(0, completedTurnSteps));
        } catch (IllegalArgumentException invalid) {
            return MouseSequenceResult.beforeStart(Result.failed(Code.INVALID_INPUT, invalid.getMessage()));
        }
    }

    /** Translate worker action progress into the completed prefix of original Turn steps. */
    static int completedTurnStepCount(List<Integer> stepEndActionIndexes,
                                      InputActionExecutionResult execution) {
        if (!execution.isStarted() || execution.getStartedStepIndex() < 0) {
            return 0;
        }
        int completedTurnSteps = 0;
        for (Integer stepEndActionIndex : stepEndActionIndexes) {
            if (stepEndActionIndex <= execution.getLastCompletedStepIndex()) {
                completedTurnSteps++;
            } else {
                break;
            }
        }
        // A non-completed request needs one terminal Turn step. If every mapped action ran but
        // terminal publication was uncertain, the final Turn step is FAILED/STOPPED, never NOT_RUN.
        return Math.min(completedTurnSteps, Math.max(0, stepEndActionIndexes.size() - 1));
    }

    /**
     * Execute one interruptible WAIT step without creating an input request.
     *
     * @param waitMs positive mechanical delay in milliseconds.
     * @return completed or stopped typed result.
     */
    public Result waitFor(long waitMs) {
        if (waitMs <= 0) {
            return Result.failed(Code.INVALID_INPUT, "waitMs must be positive");
        }
        return TaskSleep.sleep(waitMs)
                ? Result.completed()
                : Result.stopped("wait interrupted");
    }

    /**
     * Submit the complete mouse action list once, through the reviewed frozen exact-window boundary.
     *
     * <p>The legacy queue path resolved the window again from the thread-local context and refreshed the
     * native binding a second time, which could splice this action list onto a newer window generation. The
     * frozen boundary instead takes the action resolver's exact {@code context}/{@code binding}: the queue
     * witnesses that generation under the context monitor and rejects drift before focus or any input, and the
     * worker runs the whole list in one request under one focus. Nothing here refreshes, title-searches,
     * re-resolves or compares bindings a second time.</p>
     *
     * <p>The exact context is still established for the duration of the submission so that anything reading the
     * bound window during this call sees this action's window, and the caller's previous context is restored
     * afterwards by {@code callWith}.</p>
     */
    private Result submitMouseActions(TurnExecutionWindow window,
                                      String description,
                                      List<InputAction> actions) {
        return toResult(submitMouseActionsRaw(window, description, actions));
    }

    private InputActionExecutionResult submitMouseActionsRaw(TurnExecutionWindow window,
                                                              String description,
                                                              List<InputAction> actions) {
        return contextHolder.callWith(
                window.context(),
                () -> inputActionQueue.submitFrozenExactWindowActionsAndWait(
                        description, window.context(), window.binding(), actions));
    }

    private Result toResult(InputActionExecutionResult result) {
        if (result.isCompleted()) {
            return Result.completed();
        }
        log.warn("Turn input queue did not complete: requestId={} status={} safetyReason={} started={} startedStepIndex={} lastCompletedStepIndex={} reason={}",
                result.getRequestId(), result.getStatus(), result.getSafetyReason(), result.isStarted(),
                result.getStartedStepIndex(), result.getLastCompletedStepIndex(), result.getReason());
        /*
         * A typed stop is a stop, not a failure: it is reported verbatim rather than inferred from the
         * calling thread's interrupt flag, which the worker-side stop never sets.
         */
        if (result.getSafetyReason() == InputActionSafetyReason.STOP_REQUESTED) {
            return Result.stopped(describe("input stopped", result));
        }
        if (Thread.currentThread().isInterrupted()) {
            return Result.stopped("input queue wait interrupted");
        }
        return Result.failed(Code.INPUT_QUEUE_FAILED, describe("serialized input queue did not complete", result));
    }

    /** Diagnostic detail only: typed terminal facts, no business interpretation and no retry hint. */
    private static String describe(String summary, InputActionExecutionResult result) {
        return summary
                + " status=" + result.getStatus()
                + " safetyReason=" + result.getSafetyReason()
                + " startedStepIndex=" + result.getStartedStepIndex()
                + " lastCompletedStepIndex=" + result.getLastCompletedStepIndex()
                + (result.getReason() == null ? "" : " reason=" + result.getReason());
    }

    private boolean isMouseInput(TurnStep step) {
        return step != null
                && step.type() == TurnStepType.INPUT
                && step.inputAction() != null
                && step.input() != null
                && inputActionMapper.isMouse(step.inputAction());
    }

    /** Mechanical execution state consumed by the later turn outcome assembler. */
    public enum Status {
        COMPLETED,
        FAILED,
        STOPPED
    }

    /** Stable typed code consumed as the turn step result code. */
    public enum Code {
        OK,
        INVALID_INPUT,
        INPUT_QUEUE_FAILED,
        BACKGROUND_KEY_UNSUPPORTED,
        BACKGROUND_KEY_FAILED,
        STOPPED
    }

    /**
     * @param result terminal result for the atomic queue request
     * @param completedTurnStepCount count of original Turn steps whose complete mapped action range finished
     */
    public record MouseSequenceResult(Result result, int completedTurnStepCount) {
        public MouseSequenceResult {
            Objects.requireNonNull(result, "result");
            if (completedTurnStepCount < 0) {
                throw new IllegalArgumentException("completedTurnStepCount must not be negative");
            }
        }

        private static MouseSequenceResult beforeStart(Result result) {
            return new MouseSequenceResult(result, 0);
        }
    }

    /**
     * @param status terminal mechanical status.
     * @param code stable typed result code.
     * @param detail diagnostic detail without business interpretation.
     */
    public record Result(Status status, Code code, String detail) {

        public Result {
            Objects.requireNonNull(status, "status");
            Objects.requireNonNull(code, "code");
        }

        private static Result completed() {
            return new Result(Status.COMPLETED, Code.OK, null);
        }

        private static Result failed(Code code, String detail) {
            return new Result(Status.FAILED, code, detail);
        }

        private static Result stopped(String detail) {
            return new Result(Status.STOPPED, Code.STOPPED, detail);
        }
    }
}
