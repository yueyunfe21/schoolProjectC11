package com.bot.dhxy.task.template;

import com.bot.dhxy.runner.context.TaskExecutionContext;
import com.bot.dhxy.runner.policy.TaskRetryPolicy;
import com.bot.dhxy.runner.stop.TaskSleep;
import com.bot.dhxy.runner.stop.TaskStopRequestedException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component("templateTaskStepExecutor")
@Slf4j
public class TaskStepExecutor {

    public TaskStepResult execute(TaskExecutionContext context, TaskStep step) {
        return execute(context, step, null);
    }

    public TaskStepResult execute(TaskExecutionContext context, TaskStep step, TaskRetryPolicy overrideRetryPolicy) {
        if (step == null) {
            log.warn("任务步骤为空，跳过执行");
            return TaskStepResult.SKIPPED;
        }

        String stepName = step.getStepName();
        TaskRetryPolicy retryPolicy = resolveRetryPolicy(context, overrideRetryPolicy);
        int attemptedRetries = 0;

        while (true) {
            try {
                checkStop(context);
                log.info("开始执行步骤：{}", stepName);

                TaskStepResult result = step.execute(context);
                if (result == null) {
                    result = TaskStepResult.SUCCESS;
                }

                logStepResult(stepName, result);
                return result;
            } catch (TaskStopRequestedException e) {
                log.info("步骤收到停止信号：{}，原因：{}", stepName, e.getMessage());
                return TaskStepResult.STOPPED;
            } catch (Exception e) {
                log.error("步骤执行异常：{}", stepName, e);

                if (retryPolicy.canRetry(attemptedRetries)) {
                    attemptedRetries++;
                    log.warn("步骤准备重试：{}，第 {}/{} 次", stepName, attemptedRetries, retryPolicy.getMaxRetries());
                    delayBeforeRetry(retryPolicy);
                    continue;
                }

                log.error("步骤最终失败：{}", stepName);
                return TaskStepResult.FAILED;
            }
        }
    }

    private TaskRetryPolicy resolveRetryPolicy(TaskExecutionContext context, TaskRetryPolicy overrideRetryPolicy) {
        if (overrideRetryPolicy != null) {
            return overrideRetryPolicy;
        }
        if (context != null && context.getRetryPolicy() != null) {
            return context.getRetryPolicy();
        }
        return TaskRetryPolicy.none();
    }

    private void checkStop(TaskExecutionContext context) {
        if (context != null) {
            context.throwIfStopRequested();
        }
    }

    private void delayBeforeRetry(TaskRetryPolicy retryPolicy) {
        long delayMillis = retryPolicy.getDelayMillis();
        if (delayMillis <= 0) {
            return;
        }
        TaskSleep.sleepOrStop(null, delayMillis, "重试等待被中断");
    }

    private void logStepResult(String stepName, TaskStepResult result) {
        switch (result) {
            case SUCCESS -> log.info("步骤执行成功：{}", stepName);
            case FAILED -> log.warn("步骤执行失败：{}", stepName);
            case SKIPPED -> log.info("步骤已跳过：{}", stepName);
            case STOPPED -> log.info("步骤已停止：{}", stepName);
        }
    }
}
