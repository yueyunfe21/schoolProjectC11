package com.bot.dhxy.window.interaction;

/**
 * 屏幕矩形区域。
 */
public class WindowRect {

    private final int x;
    private final int y;
    private final int width;
    private final int height;

    public WindowRect(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = Math.max(width, 0);
        this.height = Math.max(height, 0);
    }

    public int getX() { return x; }

    public int getY() { return y; }

    public int getWidth() { return width; }

    public int getHeight() { return height; }

    public int getRight() { return x + width; }

    public int getBottom() { return y + height; }

    public boolean isEmpty() { return width <= 0 || height <= 0; }

    public WindowPoint center() {
        return new WindowPoint(x + width / 2, y + height / 2);
    }

    public WindowPoint relativePoint(int relativeX, int relativeY) {
        return new WindowPoint(x + relativeX, y + relativeY);
    }

    public WindowRect inset(int left, int top, int right, int bottom) {
        int newX = x + Math.max(left, 0);
        int newY = y + Math.max(top, 0);
        int newWidth = width - Math.max(left, 0) - Math.max(right, 0);
        int newHeight = height - Math.max(top, 0) - Math.max(bottom, 0);
        return new WindowRect(newX, newY, newWidth, newHeight);
    }

    public String toText() {
        if (isEmpty()) {
            return "-";
        }
        return x + "," + y + " " + width + "x" + height;
    }
}
