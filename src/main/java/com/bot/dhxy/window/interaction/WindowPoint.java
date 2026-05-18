package com.bot.dhxy.window.interaction;

/**
 * 屏幕坐标点。
 */
public class WindowPoint {

    private final int x;
    private final int y;

    public WindowPoint(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public int getX() { return x; }

    public int getY() { return y; }

    public String toText() {
        return x + "," + y;
    }
}
