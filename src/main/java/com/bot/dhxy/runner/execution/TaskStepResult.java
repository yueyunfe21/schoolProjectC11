package com.bot.dhxy.runner.execution;

import lombok.Builder;
import lombok.Getter;

/**
 * 单个任务步骤的执行结果。
 *
 * 后面五环流程可以拆成很多 step：找 NPC、寻路、点击、战斗检测、交任务等，
 * 每一步都返回 TaskStepResult，方便统一处理成功、失败、跳过、重试、停止。
 */
@Getter
@Builder
public class TaskStepResult {

    private final TaskStepStatus status;
    private final String stepName;
    private final String message;
    private final Throwable error;

    public static TaskStepResult success(String stepName, String message) {
        return TaskStepResult.builder()
                .status(TaskStepStatus.SUCCESS)
                .stepName(stepName)
                .message(message)
                .build();
    }

    public static TaskStepResult failed(String stepName, String message) {
        return TaskStepResult.builder()
                .status(TaskStepStatus.FAILED)
                .stepName(stepName)
                .message(message)
                .build();
    }

    public static TaskStepResult failed(String stepName, String message, Throwable error) {
        return TaskStepResult.builder()
                .status(TaskStepStatus.FAILED)
                .stepName(stepName)
                .message(message)
                .error(error)
                .build();
    }

    public static TaskStepResult skipped(String stepName, String message) {
        return TaskStepResult.builder()
                .status(TaskStepStatus.SKIPPED)
                .stepName(stepName)
                .message(message)
                .build();
    }

    public static TaskStepResult retry(String stepName, String message) {
        return TaskStepResult.builder()
                .status(TaskStepStatus.RETRY)
                .stepName(stepName)
                .message(message)
                .build();
    }

    public static TaskStepResult stopped(String stepName, String message) {
        return TaskStepResult.builder()
                .status(TaskStepStatus.STOPPED)
                .stepName(stepName)
                .message(message)
                .build();
    }

    public boolean isSuccess() {
        return status == TaskStepStatus.SUCCESS;
    }

    public boolean shouldRetry() {
        return status == TaskStepStatus.RETRY;
    }

    public boolean isStopped() {
        return status == TaskStepStatus.STOPPED;
    }
}
