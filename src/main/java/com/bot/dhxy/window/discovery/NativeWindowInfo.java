package com.bot.dhxy.window.discovery;

/**
 * 操作系统中的一个顶层窗口信息。
 */
public class NativeWindowInfo {

    private final String handle;
    private final String title;
    private final String className;
    private final long processId;
    private final int x;
    private final int y;
    private final int width;
    private final int height;
    private final boolean minimized;
    private final boolean foreground;
    private final int zOrderIndex;

    public NativeWindowInfo(String handle,
                            String title,
                            String className,
                            long processId,
                            int x,
                            int y,
                            int width,
                            int height) {
        this(handle, title, className, processId, x, y, width, height, false, false, Integer.MAX_VALUE);
    }

    public NativeWindowInfo(String handle,
                            String title,
                            String className,
                            long processId,
                            int x,
                            int y,
                            int width,
                            int height,
                            boolean minimized,
                            boolean foreground,
                            int zOrderIndex) {
        this.handle = handle;
        this.title = title == null ? "" : title;
        this.className = className == null ? "" : className;
        this.processId = processId;
        this.x = x;
        this.y = y;
        this.width = Math.max(width, 0);
        this.height = Math.max(height, 0);
        this.minimized = minimized;
        this.foreground = foreground;
        this.zOrderIndex = Math.max(zOrderIndex, 0);
    }

    public String getHandle() { return handle; }

    public String getTitle() { return title; }

    public String getClassName() { return className; }

    public long getProcessId() { return processId; }

    public int getX() { return x; }

    public int getY() { return y; }

    public int getWidth() { return width; }

    public int getHeight() { return height; }

    public boolean isMinimized() { return minimized; }

    public boolean isForeground() { return foreground; }

    public int getZOrderIndex() { return zOrderIndex; }

    public boolean hasTitle() { return !title.isBlank(); }

    public String toWindowId() {
        if (handle != null && !handle.isBlank()) {
            return "hwnd-" + handle;
        }
        return "pid-" + processId + "-" + Math.abs(title.hashCode());
    }

    public String toDisplayName() {
        if (!title.isBlank()) {
            return title;
        }
        if (!className.isBlank()) {
            return className;
        }
        return toWindowId();
    }
}
