package com.bot.dhxy.window.policy;

/**
 * 单个窗口注册容量判断结果。
 */
public class WindowCapacityDecision {

    private final String windowId;
    private final boolean allowed;
    private final String reason;

    private WindowCapacityDecision(String windowId, boolean allowed, String reason) {
        this.windowId = windowId;
        this.allowed = allowed;
        this.reason = reason == null ? "" : reason;
    }

    public static WindowCapacityDecision allowed(String windowId) {
        return allowed(windowId, "OK");
    }

    public static WindowCapacityDecision allowed(String windowId, String reason) {
        return new WindowCapacityDecision(windowId, true, reason);
    }

    public static WindowCapacityDecision rejected(String windowId, String reason) {
        return new WindowCapacityDecision(windowId, false, reason);
    }

    public String getWindowId() {
        return windowId;
    }

    public boolean isAllowed() {
        return allowed;
    }

    public boolean isRejected() {
        return !allowed;
    }

    public String getReason() {
        return reason;
    }
}
