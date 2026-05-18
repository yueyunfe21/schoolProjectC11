package com.bot.dhxy.window;

/**
 * 单个窗口运行状态，用于 UI 展示和窗口级任务调度。
 */
public enum WindowRuntimeStatus {
    IDLE,
    QUEUED,
    RUNNING,
    STOPPING,
    STOPPED,
    ERROR
}
