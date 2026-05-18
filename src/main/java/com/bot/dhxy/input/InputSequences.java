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
