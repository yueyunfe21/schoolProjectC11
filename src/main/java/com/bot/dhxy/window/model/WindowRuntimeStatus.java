package com.bot.dhxy.window.model;

/**
 * 单个窗口运行状态，用于 UI 展示和窗口级任务调度。
 */
public enum WindowRuntimeStatus {
    IDLE("空闲"),
    QUEUED("排队中"),
    RUNNING("运行中"),
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
        return this == QUEUED || this == RUNNING || this == STOPPING;
    }

    public boolean isTerminal() {
        return this == IDLE || this == STOPPED || this == ERROR;
    }
}
