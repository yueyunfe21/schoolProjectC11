package com.bot.dhxy.input;

import com.bot.dhxy.input.action.InputAction;
import com.bot.dhxy.input.action.InputActionQueue;
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
