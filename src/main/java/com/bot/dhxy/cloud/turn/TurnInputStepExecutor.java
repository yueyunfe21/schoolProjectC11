package com.bot.dhxy.cloud.turn;

import com.bot.dhxy.cloud.turn.protocol.TurnInputAction;
import com.bot.dhxy.cloud.turn.protocol.TurnInputCoordinateSpace;
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

    private static final int G051_ROUTE_INPUT_X = 560;
    private static final int G051_ROUTE_INPUT_Y = 337;
    private static final int G051_CANDIDATE_X = 502;
    private static final int G051_CANDIDATE_Y = 345;
    private static final int G051_CANDIDATE_WIDTH = 98;
    private static final int G051_CANDIDATE_HEIGHT = 117;
    private static final long G051_CANDIDATE_SETTLE_MS = 500L;
    private static final long G051_AFTER_CLICK_MS = 200L;

    private final InputActionQueue inputActionQueue;
    private final WindowTaskContextHolder contextHolder;
    private final TurnInputActionMapper inputActionMapper;
    private final TurnKeyMapper keyMapper;

    public TurnInputStepExecutor(InputActionQueue inputActionQueue,
                                 WindowTaskContextHolder contextHolder,
                                 TurnInputActionMapper inputActionMapper,
                                 TurnKeyMapper keyMapper) {
        this.inputActionQueue = Objects.requireNonNull(inputActionQueue, "inputActionQueue");
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
                        actions,
                        allowsCombatActiveMouse(action, input));
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
                    return deliverText(window, input, false);
                case ASCII_TEXT_INPUT:
                    return deliverText(window, input, true);
                default:
                    return Result.failed(
                            Code.BACKGROUND_KEY_UNSUPPORTED,
                            "existing HWND keyboard API cannot express " + action);
            }
        } catch (IllegalArgumentException invalid) {
            return Result.failed(Code.INVALID_INPUT, invalid.getMessage());
        }
    }

    /** Deliver one validated key tap through the frozen exact-window input queue. */
    private Result deliverKeyTap(TurnExecutionWindow window, TurnInputSpec input) {
        String key = input.key();
        BoundWindowKeyboardService.AltShortcut alt = keyMapper.findBackgroundTap(key).orElse(null);
        if (alt != null) {
            InputAction serializedAlt = toSerializedAltAction(alt);
            return submitKeyboardActions(
                    window,
                    "turn:input:keyboard:" + alt.name(),
                    List.of(serializedAlt));
        }
        BoundWindowKeyboardService.ControlShortcut ctrl = keyMapper.findControlShortcut(key).orElse(null);
        if (ctrl != null) {
            InputAction action = ctrl == BoundWindowKeyboardService.ControlShortcut.CTRL_A
                    ? InputAction.pressCtrlA()
                    : InputAction.pressCtrlU();
            return submitKeyboardActions(
                    window,
                    "turn:input:keyboard:" + ctrl.name(),
                    List.of(action));
        }
        if (keyMapper.isEnterKey(key)) {
            return submitKeyboardActions(window, "turn:input:keyboard:ENTER", List.of(InputAction.pressEnter()));
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
            case ALT_B -> InputAction.pressAltB();
            case ALT_C -> InputAction.pressAltC();
            case ALT_U -> InputAction.pressAltU();
            case ALT_5 -> InputAction.pressAlt5();
        };
    }

    /** Deliver one KEY_DOWN modifier press through the frozen exact-window input queue. */
    private Result deliverModifierDown(TurnExecutionWindow window, TurnInputSpec input) {
        BoundWindowKeyboardService.ModifierKey modifier = keyMapper.findModifierKey(input.key()).orElse(null);
        if (modifier == null) {
            return Result.failed(
                    Code.BACKGROUND_KEY_UNSUPPORTED,
                    "modifier is not background-validated: " + input.key());
        }
        return submitKeyboardActions(
                window,
                "turn:input:keyboard:" + modifier.name() + ":DOWN",
                List.of(InputAction.holdCtrl()));
    }

    /** Deliver one KEY_UP modifier release; an aborted request invokes provider release-all cleanup. */
    private Result deliverModifierRelease(TurnExecutionWindow window, TurnInputSpec input) {
        BoundWindowKeyboardService.ModifierKey modifier = keyMapper.findModifierKey(input.key()).orElse(null);
        if (modifier == null) {
            return Result.failed(
                    Code.BACKGROUND_KEY_UNSUPPORTED,
                    "modifier is not background-validated: " + input.key());
        }
        return submitKeyboardActions(
                window,
                "turn:input:keyboard:" + modifier.name() + ":UP",
                List.of(InputAction.releaseCtrl()));
    }

    /** Deliver one TEXT_INPUT through the frozen exact-window input queue. */
    private Result deliverText(TurnExecutionWindow window, TurnInputSpec input, boolean asciiOnly) {
        String text = input.text();
        if (text == null || text.isEmpty()) {
            return Result.failed(Code.INVALID_INPUT, "text input requires non-empty text");
        }
        if (asciiOnly && !text.matches("[a-z0-9]+")) {
            return Result.failed(Code.INVALID_INPUT, "ASCII text input accepts only lowercase a-z and 0-9");
        }
        return submitKeyboardActions(
                window,
                asciiOnly ? "turn:input:keyboard:ASCII" : "turn:input:keyboard:TEXT",
                List.of(asciiOnly ? InputAction.typeTextAscii(text) : InputAction.typeTextUnicode(text)));
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
                    List.copyOf(actions),
                    sequence.stream()
                            .filter(this::isMouseInput)
                            .allMatch(step -> allowsCombatActiveMouse(step.inputAction(), step.input())));
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

    /** Execute one preflight-approved G051 route-input transaction as one frozen input-queue request. */
    public MouseSequenceResult executeClosedInputSequence(TurnExecutionWindow window, List<TurnStep> steps) {
        Objects.requireNonNull(window, "window");
        Objects.requireNonNull(steps, "steps");
        if (window.metadata().stopRequested() || Thread.currentThread().isInterrupted()) {
            return MouseSequenceResult.beforeStart(Result.stopped(
                    "stop requested before G051 input sequence"));
        }
        int approvedEnd = findG051AsciiSequenceEndExclusive(window, steps, 0);
        if (approvedEnd != steps.size()) {
            return MouseSequenceResult.beforeStart(Result.failed(
                    Code.INVALID_INPUT, "unsupported G051 ASCII input sequence"));
        }
        List<InputAction> actions = new ArrayList<>();
        List<Integer> stepEndActionIndexes = new ArrayList<>();
        boolean hasMouse = false;
        boolean allMouseAllowedDuringCombat = true;
        try {
            for (TurnStep step : steps) {
                if (step.type() == TurnStepType.WAIT) {
                    if (step.waitMs() == null || step.waitMs() <= 0L || step.waitMs() > Integer.MAX_VALUE) {
                        return MouseSequenceResult.beforeStart(Result.failed(
                                Code.INVALID_INPUT, "closed input sequence requires positive bounded WAIT"));
                    }
                    actions.add(InputAction.sleep(step.waitMs().intValue()));
                } else if (step.type() == TurnStepType.INPUT && inputActionMapper.isMouse(step.inputAction())) {
                    hasMouse = true;
                    allMouseAllowedDuringCombat &= allowsCombatActiveMouse(step.inputAction(), step.input());
                    actions.addAll(inputActionMapper.mapMouse(
                            step.inputAction(), step.input(), window.metadata().windowRect()));
                } else if (step.type() == TurnStepType.INPUT) {
                    actions.add(mapClosedKeyboardAction(step.inputAction(), step.input()));
                } else {
                    return MouseSequenceResult.beforeStart(Result.failed(
                            Code.INVALID_INPUT, "closed input sequence accepts only INPUT/WAIT"));
                }
                stepEndActionIndexes.add(actions.size() - 1);
            }
            InputActionExecutionResult execution = submitMouseActionsRaw(
                    window, "turn:input:g051-closed-sequence", List.copyOf(actions),
                    !hasMouse || allMouseAllowedDuringCombat);
            Result result = toResult(execution);
            return result.status() == Status.COMPLETED
                    ? new MouseSequenceResult(result, steps.size())
                    : new MouseSequenceResult(result, completedTurnStepCount(stepEndActionIndexes, execution));
        } catch (IllegalArgumentException invalid) {
            return MouseSequenceResult.beforeStart(Result.failed(Code.INVALID_INPUT, invalid.getMessage()));
        }
    }

    private InputAction mapClosedKeyboardAction(TurnInputAction action, TurnInputSpec input) {
        return switch (action) {
            case ASCII_TEXT_INPUT -> {
                if (input == null || input.text() == null || !input.text().matches("[a-z0-9]+")) {
                    throw new IllegalArgumentException("ASCII text input accepts only lowercase a-z and 0-9");
                }
                yield InputAction.typeTextAscii(input.text());
            }
            case KEY_TAP -> {
                if (input != null && keyMapper.isEnterKey(input.key())) {
                    yield InputAction.pressEnter();
                }
                throw new IllegalArgumentException("G051 closed sequence supports Enter only");
            }
            default -> throw new IllegalArgumentException("not a closed keyboard input: " + action);
        };
    }

    /**
     * Validate every ASCII step before any physical input is enqueued.
     *
     * @param window immutable exact-window snapshot used to validate screen-absolute coordinates.
     * @param steps complete Cloud action steps.
     * @return null when every ASCII step belongs to one approved G051 transaction; otherwise a diagnostic.
     */
    public String validateG051AsciiSequences(TurnExecutionWindow window, List<TurnStep> steps) {
        Objects.requireNonNull(window, "window");
        Objects.requireNonNull(steps, "steps");
        for (int index = 0; index < steps.size(); index++) {
            if (!isAsciiAction(steps.get(index))) {
                continue;
            }
            int start = index - 1;
            if (start < 0 || findG051AsciiSequenceEndExclusive(window, steps, start) <= index) {
                return "ASCII_TEXT_INPUT is outside an approved G051 route-input transaction at step " + index;
            }
        }
        return null;
    }

    /** Return the exclusive end of one exact approved G051 sequence, or {@code startIndex} on mismatch. */
    public int findG051AsciiSequenceEndExclusive(TurnExecutionWindow window,
                                                  List<TurnStep> steps,
                                                  int startIndex) {
        if (startIndex < 0 || startIndex + 2 >= steps.size()) {
            return startIndex;
        }
        TurnStep clickInput = steps.get(startIndex);
        TurnStep ascii = steps.get(startIndex + 1);
        TurnStep settle = steps.get(startIndex + 2);
        if (!isClickAt(clickInput,
                window.metadata().windowRect().left() + G051_ROUTE_INPUT_X,
                window.metadata().windowRect().top() + G051_ROUTE_INPUT_Y)
                || !isAscii(ascii)
                || !isWait(settle, G051_CANDIDATE_SETTLE_MS)) {
            return startIndex;
        }
        if (startIndex + 4 < steps.size()
                && isCandidateClick(window, steps.get(startIndex + 3))
                && isWait(steps.get(startIndex + 4), G051_AFTER_CLICK_MS)) {
            return startIndex + 5;
        }
        return startIndex + 3;
    }

    private boolean isCandidateClick(TurnExecutionWindow window, TurnStep step) {
        if (step == null || step.type() != TurnStepType.INPUT
                || step.inputAction() != TurnInputAction.CLICK_LEFT || step.input() == null
                || step.input().x() == null || step.input().y() == null) {
            return false;
        }
        if (step.input().coordinateSpace() != TurnInputCoordinateSpace.SCREEN_ABSOLUTE) {
            return false;
        }
        int left = window.metadata().windowRect().left() + G051_CANDIDATE_X;
        int top = window.metadata().windowRect().top() + G051_CANDIDATE_Y;
        return step.input().x() >= left && step.input().x() < left + G051_CANDIDATE_WIDTH
                && step.input().y() >= top && step.input().y() < top + G051_CANDIDATE_HEIGHT;
    }

    private static boolean isClickAt(TurnStep step, int x, int y) {
        return step != null && step.type() == TurnStepType.INPUT
                && step.inputAction() == TurnInputAction.CLICK_LEFT
                && step.input() != null
                && step.input().coordinateSpace() == TurnInputCoordinateSpace.SCREEN_ABSOLUTE
                && Objects.equals(step.input().x(), x)
                && Objects.equals(step.input().y(), y);
    }

    private static boolean isAscii(TurnStep step) {
        return isAsciiAction(step)
                && step.input() != null && step.input().text() != null
                && step.input().text().matches("[a-z0-9]+");
    }

    private static boolean isAsciiAction(TurnStep step) {
        return step != null && step.type() == TurnStepType.INPUT
                && step.inputAction() == TurnInputAction.ASCII_TEXT_INPUT;
    }

    private static boolean isWait(TurnStep step, long waitMs) {
        return step != null && step.type() == TurnStepType.WAIT
                && Objects.equals(step.waitMs(), waitMs);
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
                                      List<InputAction> actions,
                                      boolean allowDuringCombat) {
        return toResult(submitMouseActionsRaw(window, description, actions, allowDuringCombat));
    }

    private InputActionExecutionResult submitMouseActionsRaw(TurnExecutionWindow window,
                                                              String description,
                                                              List<InputAction> actions,
                                                              boolean allowDuringCombat) {
        return submitThroughQueue(window, description, actions,
                () -> !allowDuringCombat && window.context().isLocalCombatVisible()
                        ? InputActionSafetyReason.COMBAT_ACTIVE
                        : InputActionSafetyReason.CLEAR);
    }

    /** Only the typed automatic-combat panel drag may bypass G004's combat mouse fence. */
    static boolean allowsCombatActiveMouse(TurnInputAction action, TurnInputSpec input) {
        return action == TurnInputAction.DRAG_LEFT
                && input != null
                && Boolean.TRUE.equals(input.autoCombatPanelDrag());
    }

    /**
     * Serialized keyboard delivery that shares the mouse queue and its exact-window freeze, but not the
     * combat gate.
     *
     * <p>G004 fenced mouse input during combat because queued walk/patrol clicks kept firing after the
     * Runner had already entered a battle. Its stated boundary is that auto-combat's {@code Alt+8} panel
     * maintenance is keyboard and stays exempt — and it must be: that maintenance exists precisely to run
     * <em>during</em> a fight. Routing it through the mouse gate blocked it whenever it mattered, which
     * reads from the outside as "auto-combat is not topping itself up".</p>
     */
    private Result submitKeyboardActions(TurnExecutionWindow window,
                                         String description,
                                         List<InputAction> actions) {
        return toResult(submitThroughQueue(
                window, description, actions, () -> InputActionSafetyReason.CLEAR));
    }

    private InputActionExecutionResult submitThroughQueue(TurnExecutionWindow window,
                                                         String description,
                                                         List<InputAction> actions,
                                                         Supplier<InputActionSafetyReason> safetyGate) {
        return contextHolder.callWith(
                window.context(),
                () -> inputActionQueue.submitFrozenExactWindowActionsAndWait(
                        description, window.context(), window.binding(), actions, safetyGate));
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
        if (result.getSafetyReason() == InputActionSafetyReason.COMBAT_ACTIVE) {
            return Result.failed(Code.LOCAL_COMBAT_ACTIVE,
                    describe("local Runner blocked mouse input during combat", result));
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
        LOCAL_COMBAT_ACTIVE,
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
