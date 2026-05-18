package com.bot.dhxy.window.control;

/**
 * 单个窗口在一次批量命令中的执行明细。
 */
public class WindowTaskCommandDetail {

    private final String windowId;
    private final boolean success;
    private final String message;

    private WindowTaskCommandDetail(String windowId, boolean success, String message) {
        this.windowId = windowId;
        this.success = success;
        this.message = message == null ? "" : message;
    }

    public static WindowTaskCommandDetail success(String windowId, String message) {
        return new WindowTaskCommandDetail(windowId, true, message);
    }

    public static WindowTaskCommandDetail failed(String windowId, String message) {
        return new WindowTaskCommandDetail(windowId, false, message);
    }

    public String getWindowId() { return windowId; }

    public boolean isSuccess() { return success; }

    public boolean isFailed() { return !success; }

    public String getMessage() { return message; }
}
