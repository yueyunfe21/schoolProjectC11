package com.bot.dhxy.input;

import com.bot.dhxy.input.action.InputAction;
import com.bot.dhxy.input.action.InputActionQueue;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Groups multi-step physical input sequences so another window cannot steal focus
 * between typing, Enter, click, and wheel actions.
 */
@Component
public class InputSequences {

    private final InputActionQueue inputActionQueue;

    public InputSequences(InputActionQueue inputActionQueue) {
        this.inputActionQueue = inputActionQueue;
    }

    public boolean submitAndWait(String description, List<InputAction> actions) {
        return inputActionQueue.submitAndWait(description, actions);
    }

    public boolean clickLeft(String description, int x, int y, int delayMs) {
        return submitAndWait(description, List.of(InputAction.clickLeft(x, y, delayMs)));
    }

    public boolean doubleRightClick(String description, int x, int y, int clickDelayMs, int intervalMs) {
        return submitAndWait(description, List.of(InputAction.doubleRightClick(x, y, clickDelayMs, intervalMs)));
    }

    public boolean pressAlt1(String description) {
        return submitAndWait(description, List.of(InputAction.pressAlt1()));
    }

    public boolean pressAlt2(String description) {
        return submitAndWait(description, List.of(InputAction.pressAlt2()));
    }

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
