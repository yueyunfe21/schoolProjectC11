package com.bot.dhxy.window.model;

/**
 * Single game-window runtime status for UI display and window-level task scheduling.
 */
public enum WindowRuntimeStatus {
    IDLE("空闲"),
    QUEUED("排队中"),
    RUNNING("运行中"),
    PAUSED("暂停中"),
    STOPPING("停止中"),
    STOPPED("已停止"),
    ERROR("异常");

    private final String displayName;

    WindowRuntimeStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isBusy() {
        return this == QUEUED || this == RUNNING || this == PAUSED || this == STOPPING;
    }

    public boolean isTerminal() {
        return this == IDLE || this == STOPPED || this == ERROR;
    }
}
