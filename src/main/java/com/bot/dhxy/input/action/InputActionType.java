package com.bot.dhxy.input.action;

/**
 * Physical input operation types supported by the serialized input worker.
 */
public enum InputActionType {
    /** Left mouse click at a screen-absolute point. */
    CLICK_LEFT,
    /** Right mouse click at a screen-absolute point. */
    CLICK_RIGHT,
    /** Two right mouse clicks at the same screen-absolute point. */
    DOUBLE_RIGHT_CLICK,
    /** Move the physical cursor to a screen-absolute point. */
    MOVE_MOUSE,
    /** Drag from one screen-absolute point to another. */
    DRAG_AND_DROP,
    /** Press and hold Ctrl. */
    HOLD_CTRL,
    /** Release Ctrl. */
    RELEASE_CTRL,
    /** Press Ctrl+C once. */
    PRESS_CTRL_C,
    /** Press Ctrl+U once. */
    PRESS_CTRL_U,
    /** Type Unicode text through the active input provider. */
    TYPE_TEXT_UNICODE,
    /** Paste text through the active input provider. */
    PASTE_TEXT,
    /** Press Enter. */
    PRESS_ENTER,
    /** Press Alt+1. */
    PRESS_ALT_1,
    /** Press Alt+2. */
    PRESS_ALT_2,
    /** Press Alt+4. */
    PRESS_ALT_4,
    /** Press Alt+6. */
    PRESS_ALT_6,
    /** Press Alt+8. */
    PRESS_ALT_8,
    /** Press Alt+T. */
    PRESS_ALT_T,
    /** Press Alt+O. */
    PRESS_ALT_O,
    /** Press Alt+E. */
    PRESS_ALT_E,
    /** Press Alt+Q. */
    PRESS_ALT_Q,
    /** Press Alt+A. */
    PRESS_ALT_A,
    /** Press Alt+C. */
    PRESS_ALT_C,
    /** Press Alt+U. */
    PRESS_ALT_U,
    /** Mouse wheel down. */
    SCROLL_DOWN,
    /** Mouse wheel up. */
    SCROLL_UP,
    /** Worker-thread sleep that preserves sequence ordering. */
    SLEEP
}
