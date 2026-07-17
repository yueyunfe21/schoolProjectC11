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
import com.bot.dhxy.runner.stop.TaskSleep;
import com.bot.dhxy.window.runtime.WindowTaskContextHolder;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Executes closed turn input mechanics without introducing a foreground keyboard fallback. */
@Component
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

            if (action != TurnInputAction.KEY_TAP) {
                return Result.failed(
                        Code.BACKGROUND_KEY_UNSUPPORTED,
                        "existing HWND keyboard API cannot express " + action);
            }
            BoundWindowKeyboardService.AltShortcut shortcut = keyMapper
                    .findBackgroundTap(input.key())
                    .orElse(null);
            if (shortcut == null) {
                return Result.failed(
                        Code.BACKGROUND_KEY_UNSUPPORTED,
                        "key is not background-validated: " + input.key());
            }
            BoundWindowKeyboardService.ShortcutAttempt attempt = keyboardService.pressShortcut(
                    window.binding(), window.metadata().windowId(), shortcut);
            if (Thread.currentThread().isInterrupted()) {
                return Result.stopped("background keyboard delivery interrupted");
            }
            if (attempt.success()) {
                return Result.completed();
            }
            return Result.failed(
                    Code.BACKGROUND_KEY_FAILED,
                    attempt.reason() == null ? "background keyboard delivery failed" : attempt.reason());
        } catch (IllegalArgumentException invalid) {
            return Result.failed(Code.INVALID_INPUT, invalid.getMessage());
        }
    }

    /**
     * Submit one closed mouse/positive-WAIT fragment as one indivisible input-queue request.
     *
     * @param window immutable per-action window snapshot; all input coordinates are screen-absolute pixels.
     * @param steps ordered original action steps; the first and last must be mouse INPUT steps and every
     *              interior step must be either mouse INPUT or a positive WAIT.
     * @return one typed result for the whole queue request; callers expand success to the original step indexes.
     */
    public Result executeMouseSequence(TurnExecutionWindow window, List<TurnStep> steps) {
        Objects.requireNonNull(window, "window");
        Objects.requireNonNull(steps, "steps");
        if (window.metadata().stopRequested() || Thread.currentThread().isInterrupted()) {
            return Result.stopped("stop requested before mouse input sequence");
        }
        if (steps.isEmpty()) {
            return Result.failed(Code.INVALID_INPUT, "mouse input sequence must not be empty");
        }

        List<TurnStep> sequence = List.copyOf(steps);
        TurnStep first = sequence.get(0);
        TurnStep last = sequence.get(sequence.size() - 1);
        if (!isMouseInput(first) || !isMouseInput(last)) {
            return Result.failed(Code.INVALID_INPUT, "mouse input sequence must start and end with mouse INPUT");
        }

        try {
            List<InputAction> actions = new ArrayList<>();
            for (TurnStep step : sequence) {
                if (isMouseInput(step)) {
                    actions.addAll(inputActionMapper.mapMouse(
                            step.inputAction(), step.input(), window.metadata().windowRect()));
                    continue;
                }
                if (step.type() != TurnStepType.WAIT
                        || step.waitMs() == null
                        || step.waitMs() <= 0L
                        || step.waitMs() > Integer.MAX_VALUE) {
                    return Result.failed(
                            Code.INVALID_INPUT,
                            "mouse input sequence may contain only mouse INPUT and positive bounded WAIT steps");
                }
                actions.add(InputAction.sleep(step.waitMs().intValue()));
            }
            return submitMouseActions(
                    window,
                    "turn:input:steps-" + first.index() + "-" + last.index() + ":mouse-sequence",
                    List.copyOf(actions));
        } catch (IllegalArgumentException invalid) {
            return Result.failed(Code.INVALID_INPUT, invalid.getMessage());
        }
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
        InputActionExecutionResult result = contextHolder.callWith(
                window.context(),
                () -> inputActionQueue.submitFrozenExactWindowActionsAndWait(
                        description, window.context(), window.binding(), actions));
        if (result.isCompleted()) {
            return Result.completed();
        }
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
