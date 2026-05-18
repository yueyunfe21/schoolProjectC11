package com.bot.dhxy.input;

import org.springframework.stereotype.Component;

/**
 * Groups multi-step physical input sequences so another window cannot steal focus
 * between typing, Enter, click, and wheel actions.
 */
@Component
public class InputSequences {

    private final WindowAwareInputCoordinator inputCoordinator;
    private final InputProvider inputProvider;

    public InputSequences(WindowAwareInputCoordinator inputCoordinator, InputProvider inputProvider) {
        this.inputCoordinator = inputCoordinator;
        this.inputProvider = inputProvider;
    }

    public boolean typeTextEnterAndScroll(String text, int scrollFocusX, int scrollFocusY) {
        return inputCoordinator.callInputTransaction("typeTextEnterAndScroll", () -> {
            inputProvider.typeTextUnicode(text);
            if (!sleepInterruptible(100)) {
                return false;
            }
            inputProvider.pressEnter();
            inputProvider.clickLeft(scrollFocusX, scrollFocusY, 50);
            for (int i = 0; i < 2; i++) {
                inputProvider.scrollDown(2);
                if (!sleepInterruptible(100)) {
                    return false;
                }
            }
            return sleepInterruptible(500);
        });
    }

    private boolean sleepInterruptible(long ms) {
        try {
            Thread.sleep(ms);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
}
