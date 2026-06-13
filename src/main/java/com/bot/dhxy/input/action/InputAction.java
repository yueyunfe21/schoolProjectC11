package com.bot.dhxy.input.action;

/**
 * Immutable physical input step consumed by {@link InputActionWorker}.
 *
 * <p>Mouse coordinates are screen-absolute pixels at execution time. Callers that start from
 * window-relative or game-map coordinates must translate before creating an action. Multi-step
 * workflows that rely on mouse position, such as move-then-click, must be placed in the same queued
 * request so another window cannot insert focus or mouse movement between steps.</p>
 */
public class InputAction {

    private final InputActionType type;
    private final int x;
    private final int y;
    private final int endX;
    private final int endY;
    private final int delayMs;
    private final int intervalMs;
    private final int clicks;
    private final String text;

    private InputAction(InputActionType type,
                        int x,
                        int y,
                        int endX,
                        int endY,
                        int delayMs,
                        int intervalMs,
                        int clicks,
                        String text) {
        this.type = type;
        this.x = x;
        this.y = y;
        this.endX = endX;
        this.endY = endY;
        this.delayMs = delayMs;
        this.intervalMs = intervalMs;
        this.clicks = clicks;
        this.text = text;
    }

    /**
     * @param x screen-absolute X pixel.
     * @param y screen-absolute Y pixel.
     * @param delayMs delay after the click in milliseconds.
     * @return left-click action.
     */
    public static InputAction clickLeft(int x, int y, int delayMs) {
        return new InputAction(InputActionType.CLICK_LEFT, x, y, 0, 0, delayMs, 0, 0, null);
    }

    /**
     * @param x screen-absolute X pixel.
     * @param y screen-absolute Y pixel.
     * @param delayMs delay after the click in milliseconds.
     * @return right-click action.
     */
    public static InputAction clickRight(int x, int y, int delayMs) {
        return new InputAction(InputActionType.CLICK_RIGHT, x, y, 0, 0, delayMs, 0, 0, null);
    }

    /**
     * @param x screen-absolute X pixel.
     * @param y screen-absolute Y pixel.
     * @param clickDelayMs delay after each click in milliseconds.
     * @param intervalMs delay between the two right-clicks in milliseconds.
     * @return double-right-click action.
     */
    public static InputAction doubleRightClick(int x, int y, int clickDelayMs, int intervalMs) {
        return new InputAction(InputActionType.DOUBLE_RIGHT_CLICK, x, y, 0, 0, clickDelayMs, intervalMs, 0, null);
    }

    /**
     * @param x screen-absolute X pixel.
     * @param y screen-absolute Y pixel.
     * @return mouse-move action with no click.
     */
    public static InputAction moveMouse(int x, int y) {
        return new InputAction(InputActionType.MOVE_MOUSE, x, y, 0, 0, 0, 0, 0, null);
    }

    /**
     * @param startX screen-absolute drag start X pixel.
     * @param startY screen-absolute drag start Y pixel.
     * @param endX screen-absolute drag end X pixel.
     * @param endY screen-absolute drag end Y pixel.
     * @return drag-and-drop action.
     */
    public static InputAction dragAndDrop(int startX, int startY, int endX, int endY) {
        return new InputAction(InputActionType.DRAG_AND_DROP, startX, startY, endX, endY, 0, 0, 0, null);
    }

    /** @return action that presses and holds Ctrl until a later release action. */
    public static InputAction holdCtrl() {
        return simple(InputActionType.HOLD_CTRL);
    }

    /** @return action that releases Ctrl. */
    public static InputAction releaseCtrl() {
        return simple(InputActionType.RELEASE_CTRL);
    }

    /** @return Ctrl+C key action. */
    public static InputAction pressCtrlC() {
        return simple(InputActionType.PRESS_CTRL_C);
    }

    /** @return Ctrl+U key action. */
    public static InputAction pressCtrlU() {
        return simple(InputActionType.PRESS_CTRL_U);
    }

    /**
     * @param text Unicode text to type through the input provider. Null handling is provider-specific.
     * @return text typing action.
     */
    public static InputAction typeTextUnicode(String text) {
        return new InputAction(InputActionType.TYPE_TEXT_UNICODE, 0, 0, 0, 0, 0, 0, 0, text);
    }

    /**
     * @param text text copied/pasted through the input provider.
     * @return paste action.
     */
    public static InputAction pasteText(String text) {
        return new InputAction(InputActionType.PASTE_TEXT, 0, 0, 0, 0, 0, 0, 0, text);
    }

    /** @return Enter key action. */
    public static InputAction pressEnter() {
        return new InputAction(InputActionType.PRESS_ENTER, 0, 0, 0, 0, 0, 0, 0, null);
    }

    /** @return Alt+1 key action. */
    public static InputAction pressAlt1() {
        return simple(InputActionType.PRESS_ALT_1);
    }

    /** @return Alt+2 key action. */
    public static InputAction pressAlt2() {
        return simple(InputActionType.PRESS_ALT_2);
    }

    /** @return Alt+4 key action. */
    public static InputAction pressAlt4() {
        return simple(InputActionType.PRESS_ALT_4);
    }

    /** @return Alt+6 key action. */
    public static InputAction pressAlt6() {
        return simple(InputActionType.PRESS_ALT_6);
    }

    /** @return Alt+8 key action. */
    public static InputAction pressAlt8() {
        return simple(InputActionType.PRESS_ALT_8);
    }

    /** @return Alt+T key action. */
    public static InputAction pressAltT() {
        return simple(InputActionType.PRESS_ALT_T);
    }

    /** @return Alt+O key action. */
    public static InputAction pressAltO() {
        return simple(InputActionType.PRESS_ALT_O);
    }

    /** @return Alt+E key action. */
    public static InputAction pressAltE() {
        return simple(InputActionType.PRESS_ALT_E);
    }

    /** @return Alt+Q key action. */
    public static InputAction pressAltQ() {
        return simple(InputActionType.PRESS_ALT_Q);
    }

    /** @return Alt+A key action. */
    public static InputAction pressAltA() {
        return simple(InputActionType.PRESS_ALT_A);
    }

    /** @return Alt+C key action. */
    public static InputAction pressAltC() {
        return simple(InputActionType.PRESS_ALT_C);
    }

    /** @return Alt+U key action. */
    public static InputAction pressAltU() {
        return simple(InputActionType.PRESS_ALT_U);
    }

    /**
     * @param clicks wheel click count.
     * @return mouse-wheel-down action.
     */
    public static InputAction scrollDown(int clicks) {
        return new InputAction(InputActionType.SCROLL_DOWN, 0, 0, 0, 0, 0, 0, clicks, null);
    }

    /**
     * @param clicks wheel click count.
     * @return mouse-wheel-up action.
     */
    public static InputAction scrollUp(int clicks) {
        return new InputAction(InputActionType.SCROLL_UP, 0, 0, 0, 0, 0, 0, clicks, null);
    }

    /**
     * @param delayMs sleep duration in milliseconds.
     * @return delay action executed inside the input worker, preserving sequence atomicity.
     */
    public static InputAction sleep(int delayMs) {
        return new InputAction(InputActionType.SLEEP, 0, 0, 0, 0, delayMs, 0, 0, null);
    }

    private static InputAction simple(InputActionType type) {
        return new InputAction(type, 0, 0, 0, 0, 0, 0, 0, null);
    }

    /** @return action type consumed by the input worker. */
    public InputActionType getType() { return type; }

    /** @return screen-absolute X pixel for point actions, or 0 when unused. */
    public int getX() { return x; }

    /** @return screen-absolute Y pixel for point actions, or 0 when unused. */
    public int getY() { return y; }

    /** @return screen-absolute drag end X pixel, or 0 when unused. */
    public int getEndX() { return endX; }

    /** @return screen-absolute drag end Y pixel, or 0 when unused. */
    public int getEndY() { return endY; }

    /** @return post-action delay in milliseconds, or sleep duration for SLEEP. */
    public int getDelayMs() { return delayMs; }

    /** @return interval between repeated clicks in milliseconds. */
    public int getIntervalMs() { return intervalMs; }

    /** @return wheel click count for scroll actions. */
    public int getClicks() { return clicks; }

    /** @return text payload for typing/paste actions, or null when unused. */
    public String getText() { return text; }

    @Override
    public String toString() {
        return "InputAction{" +
                "type=" + type +
                ", x=" + x +
                ", y=" + y +
                ", endX=" + endX +
                ", endY=" + endY +
                ", delayMs=" + delayMs +
                ", intervalMs=" + intervalMs +
                ", clicks=" + clicks +
                ", text='" + text + '\'' +
                '}';
    }
}
