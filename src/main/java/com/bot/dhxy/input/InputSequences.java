package com.bot.dhxy.input;

import com.bot.dhxy.input.action.InputAction;
import com.bot.dhxy.input.action.InputActionExecutionResult;
import com.bot.dhxy.input.action.InputActionQueue;
import com.bot.dhxy.window.model.WindowNativeBinding;
import com.bot.dhxy.window.runtime.WindowRuntimeContext;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Supplier;

/**
 * Convenience API for submitting atomic physical input sequences.
 *
 * <p>Every method captures the current bound window and waits for the single input worker to finish.
 * Coordinates are screen-absolute pixels. Use {@link #submitAndWait(String, List)} when move/click or
 * key/click steps must stay together; splitting them into separate requests allows another window to
 * interleave focus or mouse movement.</p>
 */
@Component
public class InputSequences {

    private final InputActionQueue inputActionQueue;

    /**
     * @param inputActionQueue serialized queue backing all physical input.
     */
    public InputSequences(InputActionQueue inputActionQueue) {
        this.inputActionQueue = inputActionQueue;
    }

    /**
     * Submit an ordered physical input sequence.
     *
     * @param description diagnostic label for logs.
     * @param actions ordered actions with screen-absolute coordinates where applicable.
     * @return true when the worker completes the whole sequence successfully.
     */
    public boolean submitAndWait(String description, List<InputAction> actions) {
        return inputActionQueue.submitAndWait(description, actions);
    }

    /**
     * Submit an ordered physical input sequence without waiting for completion.
     *
     * @param description diagnostic label for logs.
     * @param actions ordered actions with screen-absolute coordinates where applicable.
     * @return true when the request was accepted into the serialized input queue.
     */
    public boolean submit(String description, List<InputAction> actions) {
        return inputActionQueue.submit(description, actions);
    }

    /**
     * Run a callback inside exclusive input-worker ownership.
     *
     * @param description diagnostic label for logs.
     * @param callback direct-input callback; it must not submit nested input queue requests.
     * @return true when the callback returns true and the worker completes it.
     */
    public boolean submitExclusiveAndWait(String description, Supplier<Boolean> callback) {
        return inputActionQueue.submitExclusiveAndWait(description, callback);
    }

    /**
     * Run a direct-input callback against one immutable action-resolver window snapshot.
     *
     * <p>No caller-supplied identity epoch is accepted: the queue freezes the {@code (binding, epoch)}
     * generation itself under the context monitor. The worker's typed terminal result is returned
     * verbatim so callers can project a real {@code STOP_REQUESTED} instead of guessing from a boolean.</p>
     *
     * @param description diagnostic label for logs
     * @param context exact resolved window context
     * @param binding exact HWND/process/screen-rectangle snapshot; must still be the context's current
     *                generation object
     * @param callback worker callback; it must not submit nested input requests
     * @return the worker's typed terminal result: status, safety reason and detail, never flattened
     */
    public InputActionExecutionResult submitFrozenExactWindowExclusiveAndWait(
            String description,
            WindowRuntimeContext context,
            WindowNativeBinding binding,
            Supplier<Boolean> callback) {
        return inputActionQueue.submitFrozenExactWindowExclusiveAndWait(
                description, context, binding, callback);
    }

    /**
     * Submit one complete ordered action list against one immutable action-resolver window snapshot.
     *
     * <p>Same frozen boundary as
     * {@link #submitFrozenExactWindowExclusiveAndWait(String, WindowRuntimeContext, WindowNativeBinding, Supplier)}:
     * no caller-supplied identity epoch is accepted, the queue freezes the {@code (binding, epoch)}
     * generation itself under the context monitor, and the worker's typed terminal result is returned
     * verbatim instead of a flattened boolean.</p>
     *
     * <p>Use this when the whole list must stay under the frozen boundary. The list is submitted once, as
     * one request; the worker executes every action and delay inside one input transaction and one
     * generation monitor, so no binding commit interleaves between elements. Do not wrap
     * {@link #submitAndWait(String, List)} inside the frozen callback: that is a nested queue submission.</p>
     *
     * @param description diagnostic label for logs
     * @param context exact resolved window context
     * @param binding exact HWND/process/screen-rectangle snapshot; must still be the context's current
     *                generation object
     * @param actions complete ordered action list with screen-absolute coordinates where applicable
     * @return the worker's typed terminal result: status, safety reason and detail, never flattened
     */
    public InputActionExecutionResult submitFrozenExactWindowActionsAndWait(
            String description,
            WindowRuntimeContext context,
            WindowNativeBinding binding,
            List<InputAction> actions) {
        return inputActionQueue.submitFrozenExactWindowActionsAndWait(
                description, context, binding, actions);
    }

    /**
     * @param description diagnostic label.
     * @param x screen-absolute X pixel.
     * @param y screen-absolute Y pixel.
     * @param delayMs post-click delay in milliseconds.
     * @return true when the click sequence completes.
     */
    public boolean clickLeft(String description, int x, int y, int delayMs) {
        return submitAndWait(description, List.of(InputAction.clickLeft(x, y, delayMs)));
    }

    /**
     * Move to a known screen point and left-click it in one queued request.
     *
     * @param description diagnostic label.
     * @param x screen-absolute X pixel.
     * @param y screen-absolute Y pixel.
     * @param settleMs delay after moving before clicking, in milliseconds.
     * @param delayMs post-click delay in milliseconds.
     * @return true when the move and click complete without interruption.
     */
    public boolean moveAndClickLeft(String description, int x, int y, int settleMs, int delayMs) {
        return submitAndWait(description, List.of(
                InputAction.moveMouse(x, y),
                InputAction.sleep(settleMs),
                InputAction.clickLeft(x, y, delayMs)
        ));
    }

    /**
     * Submit one double-right-click sequence.
     *
     * @param description diagnostic label.
     * @param x screen-absolute X pixel.
     * @param y screen-absolute Y pixel.
     * @param clickDelayMs post-click delay in milliseconds.
     * @param intervalMs interval between clicks in milliseconds.
     * @return true when both clicks complete.
     */
    public boolean doubleRightClick(String description, int x, int y, int clickDelayMs, int intervalMs) {
        return submitAndWait(description, List.of(InputAction.doubleRightClick(x, y, clickDelayMs, intervalMs)));
    }

    public boolean pressAlt1(String description) {
        return submitAndWait(description, List.of(InputAction.pressAlt1()));
    }

    public boolean pressAlt2(String description) {
        return submitAndWait(description, List.of(InputAction.pressAlt2()));
    }

    public boolean pressAlt6(String description) {
        return submitAndWait(description, List.of(InputAction.pressAlt6()));
    }

    public boolean pressAltT(String description) {
        return submitAndWait(description, List.of(InputAction.pressAltT()));
    }

    public boolean pressAltU(String description) {
        return submitAndWait(description, List.of(InputAction.pressAltU()));
    }

    public boolean pressAltC(String description) {
        return submitAndWait(description, List.of(InputAction.pressAltC()));
    }

    public boolean pressCtrlU(String description) {
        return submitAndWait(description, List.of(InputAction.pressCtrlU()));
    }

    /**
     * Type text, press Enter, then click and scroll a result area as one atomic sequence.
     *
     * @param text Unicode text to type.
     * @param scrollFocusX screen-absolute X pixel that receives the scroll focus click.
     * @param scrollFocusY screen-absolute Y pixel that receives the scroll focus click.
     * @return true when the full type/enter/scroll sequence completes.
     */
    public boolean typeTextEnterAndScroll(String text, int scrollFocusX, int scrollFocusY) {
        return inputActionQueue.submitAndWait("typeTextEnterAndScroll", List.of(
                InputAction.typeTextUnicode(text),
                InputAction.sleep(100),
                InputAction.pressEnter(),
                InputAction.clickLeft(scrollFocusX, scrollFocusY, 50),
                InputAction.scrollDown(2),
                InputAction.sleep(100),
                InputAction.scrollDown(2),
                InputAction.sleep(500)
        ));
    }
}
