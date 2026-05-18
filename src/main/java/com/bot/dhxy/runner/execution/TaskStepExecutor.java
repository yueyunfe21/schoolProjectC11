package com.bot.dhxy.runner.execution;

import com.bot.dhxy.runner.context.TaskExecutionContext;
import com.bot.dhxy.runner.log.TaskLogService;
import com.bot.dhxy.runner.policy.TaskRetryPolicy;
import com.bot.dhxy.runner.stop.TaskStopRequestedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 任务步骤执行器。
 *
 * 作用：统一处理任务步骤的停止检查、异常转换、重试和日志。
 */
@Slf4j
@Component("runnerTaskStepExecutor")
@RequiredArgsConstructor
public class TaskStepExecutor {

    private final TaskLogService taskLogService;

    public TaskStepResult runStep(TaskExecutionContext context, String stepName, TaskStepAction action) {
        TaskRetryPolicy retryPolicy = context == null || context.getRetryPolicy() == null
                ? TaskRetryPolicy.none()
                : context.getRetryPolicy();
        return runStep(context, stepName, retryPolicy, action);
    }

    public TaskStepResult runStep(TaskExecutionContext context,
                                  String stepName,
                                  TaskRetryPolicy retryPolicy,
                                  TaskStepAction action) {
        if (retryPolicy == null) {
            retryPolicy = TaskRetryPolicy.none();
        }

        int attemptedRetries = 0;
        while (true) {
            try {
                checkStop(context, stepName);
                logStepStart(context, stepName, attemptedRetries);

                TaskStepResult result = action == null
                        ? TaskStepResult.failed(stepName, "步骤动作为空")
                        : action.execute();

                if (result == null) {
                    result = TaskStepResult.failed(stepName, "步骤返回 null");
                }

                if (result.isStopped()) {
                    logStepStopped(context, stepName, result.getMessage());
                    return result;
                }

                if (result.shouldRetry() && retryPolicy.canRetry(attemptedRetries)) {
                    attemptedRetries++;
                    logStepRetry(context, stepName, result.getMessage(), attemptedRetries, retryPolicy);
                    sleepBeforeRetry(retryPolicy);
                    continue;
                }

                logStepFinish(context, result);
                return result;
            } catch (TaskStopRequestedException e) {
                TaskStepResult stopped = TaskStepResult.stopped(stepName, e.getMessage());
                logStepStopped(context, stepName, e.getMessage());
                return stopped;
            } catch (Exception e) {
                if (retryPolicy.canRetry(attemptedRetries)) {
                    attemptedRetries++;
                    logStepRetry(context, stepName, e.getClass().getSimpleName() + ": " + e.getMessage(), attemptedRetries, retryPolicy);
                    sleepBeforeRetry(retryPolicy);
                    continue;
                }
                TaskStepResult failed = TaskStepResult.failed(stepName, e.getClass().getSimpleName() + ": " + e.getMessage(), e);
                logStepFail(context, failed);
                return failed;
            }
        }
    }

    private void checkStop(TaskExecutionContext context, String stepName) {
        if (context != null) {
            context.throwIfStopRequested();
        }
    }

    private void logStepStart(TaskExecutionContext context, String stepName, int attemptedRetries) {
        String suffix = attemptedRetries <= 0 ? "" : "，重试第 " + attemptedRetries + " 次";
        taskLogService.info(taskCode(context), taskName(context), "开始步骤: " + stepName + suffix);
    }

    private void logStepFinish(TaskExecutionContext context, TaskStepResult result) {
        if (result.isSuccess()) {
            taskLogService.info(taskCode(context), taskName(context), "步骤成功: " + result.getStepName() + " | " + nullToEmpty(result.getMessage()));
            return;
        }
        if (result.getStatus() == TaskStepStatus.SKIPPED) {
            taskLogService.warn(taskCode(context), taskName(context), "步骤跳过: " + result.getStepName() + " | " + nullToEmpty(result.getMessage()));
            return;
        }
        if (result.shouldRetry()) {
            taskLogService.warn(taskCode(context), taskName(context), "步骤要求重试但已达到重试上限: " + result.getStepName() + " | " + nullToEmpty(result.getMessage()));
            return;
        }
        if (result.getStatus() == TaskStepStatus.FAILED) {
            logStepFail(context, result);
        }
    }

    private void logStepRetry(TaskExecutionContext context,
                              String stepName,
                              String message,
                              int attemptedRetries,
                              TaskRetryPolicy retryPolicy) {
        taskLogService.warn(taskCode(context), taskName(context),
                "步骤重试: " + stepName
                        + " | 第 " + attemptedRetries + " 次"
                        + " | delay=" + retryPolicy.getDelayMillis() + "ms"
                        + " | " + nullToEmpty(message));
    }

    private void logStepFail(TaskExecutionContext context, TaskStepResult result) {
        taskLogService.fail(taskCode(context), taskName(context), "步骤失败: " + result.getStepName() + " | " + nullToEmpty(result.getMessage()));
        if (result.getError() != null) {
            log.error("任务步骤异常: taskCode={} step={}", taskCode(context), result.getStepName(), result.getError());
        }
    }

    private void logStepStopped(TaskExecutionContext context, String stepName, String message) {
        taskLogService.warn(taskCode(context), taskName(context), "步骤停止: " + stepName + " | " + nullToEmpty(message));
    }

    private void sleepBeforeRetry(TaskRetryPolicy retryPolicy) {
        if (retryPolicy.getDelayMillis() <= 0) {
            return;
        }
        try {
            Thread.sleep(retryPolicy.getDelayMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new TaskStopRequestedException("重试等待被中断");
        }
    }

    private String taskCode(TaskExecutionContext context) {
        return context == null ? null : context.getTaskCode();
    }

    private String taskName(TaskExecutionContext context) {
        return context == null ? null : context.getTaskName();
    }

    private String nullToEmpty(String text) {
        return text == null ? "" : text;
    }
}
