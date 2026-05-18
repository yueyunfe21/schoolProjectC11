package com.bot.dhxy.window.service;

/**
 * UI 启动窗口任务时的模式。
 */
public enum WindowTaskStartMode {
    /** 所有选中窗口启动同一个任务。 */
    SAME_TASK,

    /** 每个窗口启动自己已选择的任务。 */
    SELECTED_TASK,

    /** 根据识别到的队长/队员身份自动分配任务。 */
    DETECTED_ROLE
}
