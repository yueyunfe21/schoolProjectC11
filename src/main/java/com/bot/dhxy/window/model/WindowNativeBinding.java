package com.bot.dhxy.window.model;

/**
 * 游戏窗口与系统原生窗口的绑定信息。
 */
public class WindowNativeBinding {

    private final String nativeHandle;
    private final String title;
    private final String className;
    private final long processId;
    private final int x;
    private final int y;
    private final int width;
    private final int height;

    public WindowNativeBinding(String nativeHandle,
                               String title,
                               String className,
                               long processId,
                               int x,
                               int y,
                               int width,
                               int height) {
        this.nativeHandle = normalize(nativeHandle);
        this.title = title == null ? "" : title.trim();
        this.className = className == null ? "" : className.trim();
        this.processId = Math.max(processId, 0L);
        this.x = x;
        this.y = y;
        this.width = Math.max(width, 0);
        this.height = Math.max(height, 0);
    }

    public static WindowNativeBinding empty() {
        return new WindowNativeBinding(null, "", "", 0L, 0, 0, 0, 0);
    }

    public String getNativeHandle() { return nativeHandle; }

    public String getTitle() { return title; }

    public String getClassName() { return className; }

    public long getProcessId() { return processId; }

    public int getX() { return x; }

    public int getY() { return y; }

    public int getWidth() { return width; }

    public int getHeight() { return height; }

    public boolean hasNativeHandle() { return nativeHandle != null && !nativeHandle.isBlank(); }

    public boolean hasGeometry() { return width > 0 && height > 0; }

    public String getGeometryText() {
        if (!hasGeometry()) {
            return "-";
        }
        return x + "," + y + " " + width + "x" + height;
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
