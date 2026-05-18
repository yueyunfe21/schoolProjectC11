package com.bot.dhxy.input.action;

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

    public static InputAction clickLeft(int x, int y, int delayMs) {
        return new InputAction(InputActionType.CLICK_LEFT, x, y, 0, 0, delayMs, 0, 0, null);
    }

    public static InputAction clickRight(int x, int y, int delayMs) {
        return new InputAction(InputActionType.CLICK_RIGHT, x, y, 0, 0, delayMs, 0, 0, null);
    }

    public static InputAction doubleRightClick(int x, int y, int clickDelayMs, int intervalMs) {
        return new InputAction(InputActionType.DOUBLE_RIGHT_CLICK, x, y, 0, 0, clickDelayMs, intervalMs, 0, null);
    }

    public static InputAction typeTextUnicode(String text) {
        return new InputAction(InputActionType.TYPE_TEXT_UNICODE, 0, 0, 0, 0, 0, 0, 0, text);
    }

    public static InputAction pressEnter() {
        return new InputAction(InputActionType.PRESS_ENTER, 0, 0, 0, 0, 0, 0, 0, null);
    }

    public static InputAction scrollDown(int clicks) {
        return new InputAction(InputActionType.SCROLL_DOWN, 0, 0, 0, 0, 0, 0, clicks, null);
    }

    public static InputAction scrollUp(int clicks) {
        return new InputAction(InputActionType.SCROLL_UP, 0, 0, 0, 0, 0, 0, clicks, null);
    }

    public static InputAction sleep(int delayMs) {
        return new InputAction(InputActionType.SLEEP, 0, 0, 0, 0, delayMs, 0, 0, null);
    }

    public InputActionType getType() { return type; }

    public int getX() { return x; }

    public int getY() { return y; }

    public int getEndX() { return endX; }

    public int getEndY() { return endY; }

    public int getDelayMs() { return delayMs; }

    public int getIntervalMs() { return intervalMs; }

    public int getClicks() { return clicks; }

    public String getText() { return text; }

    @Override
    public String toString() {
        return "InputAction{" +
                "type=" + type +
                ", x=" + x +
                ", y=" + y +
                ", delayMs=" + delayMs +
                ", intervalMs=" + intervalMs +
                ", clicks=" + clicks +
                ", text='" + text + '\'' +
                '}';
    }
}
