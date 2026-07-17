package com.bot.dhxy.cloud.remote;

import com.bot.dhxy.input.action.InputAction;

import java.util.ArrayList;
import java.util.List;

public final class RemoteInputActionMapper {

    public List<InputAction> toInputActions(List<RemoteInputActionDto> actions) {
        if (actions == null) {
            throw new RemotePayloadException("actions are required");
        }
        List<InputAction> mapped = new ArrayList<>(actions.size());
        for (RemoteInputActionDto action : actions) {
            action.validate();
            mapped.add(toInputAction(action));
        }
        return List.copyOf(mapped);
    }

    private static InputAction toInputAction(RemoteInputActionDto action) {
        return switch (action.getType()) {
            case CLICK_LEFT -> InputAction.clickLeft(action.getX(), action.getY(), action.getDelayMs());
            case CLICK_RIGHT -> InputAction.clickRight(action.getX(), action.getY(), action.getDelayMs());
            case DOUBLE_RIGHT_CLICK -> InputAction.doubleRightClick(
                    action.getX(), action.getY(), action.getDelayMs(), action.getIntervalMs());
            case MOVE_MOUSE -> InputAction.moveMouse(action.getX(), action.getY());
            case DRAG_AND_DROP -> InputAction.dragAndDrop(
                    action.getX(), action.getY(), action.getEndX(), action.getEndY());
            case HOLD_CTRL -> InputAction.holdCtrl();
            case RELEASE_CTRL -> InputAction.releaseCtrl();
            case PRESS_CTRL_U -> InputAction.pressCtrlU();
            case TYPE_TEXT_UNICODE -> InputAction.typeTextUnicode(action.getText());
            case PASTE_TEXT -> InputAction.pasteText(action.getText());
            case PRESS_ENTER -> InputAction.pressEnter();
            case PRESS_ALT_1 -> InputAction.pressAlt1();
            case PRESS_ALT_2 -> InputAction.pressAlt2();
            case PRESS_ALT_4 -> InputAction.pressAlt4();
            case PRESS_ALT_6 -> InputAction.pressAlt6();
            case PRESS_ALT_8 -> InputAction.pressAlt8();
            case PRESS_ALT_T -> InputAction.pressAltT();
            case PRESS_ALT_O -> InputAction.pressAltO();
            case PRESS_ALT_E -> InputAction.pressAltE();
            case PRESS_ALT_Q -> InputAction.pressAltQ();
            case PRESS_ALT_A -> InputAction.pressAltA();
            case PRESS_ALT_C -> InputAction.pressAltC();
            case PRESS_ALT_U -> InputAction.pressAltU();
            case SCROLL_DOWN -> InputAction.scrollDown(action.getClicks());
            case SCROLL_UP -> InputAction.scrollUp(action.getClicks());
            case SLEEP -> InputAction.sleep(action.getDelayMs());
        };
    }
}
