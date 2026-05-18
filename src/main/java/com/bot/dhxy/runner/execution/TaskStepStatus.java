package com.bot.dhxy.runner.execution;

/**
 * 单个任务步骤的执行状态。
 */
public enum TaskStepStatus {
    SUCCESS,
    FAILED,
    SKIPPED,
    RETRY,
    STOPPED
}
