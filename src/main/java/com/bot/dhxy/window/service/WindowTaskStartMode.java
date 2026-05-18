package com.bot.dhxy.window.service;

/**
 * UI 启动窗口任务时的模式。
 */
public enum WindowTaskStartMode {
    /** 所有选中窗口启动同一个任务。 */
    SAME_TASK("统一任务"),

    /** 每个窗口启动自己已选择的任务。 */
    SELECTED_TASK("窗口已选任务"),

    /** 根据识别到的队长/队员身份自动分配任务。 */
    DETECTED_ROLE("按身份分配");

    private final String displayName;

    WindowTaskStartMode(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
