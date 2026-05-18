package com.bot.dhxy.task.template;

import com.bot.dhxy.core.GameContext;
import com.bot.dhxy.model.TaskRunResult;
import com.bot.dhxy.runner.TaskExecutionContext;
import com.bot.dhxy.runner.policy.TaskRetryPolicy;
import com.bot.dhxy.runner.stop.TaskStopRequestedException;
import com.bot.dhxy.task.GameTask;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

@Slf4j
public abstract class BaseTaskTemplate implements GameTask {

    protected final GameContext gameContext;
    protected final TaskStepExecutor taskStepExecutor;

    protected BaseTaskTemplate(GameContext gameContext, TaskStepExecutor taskStepExecutor) {
        this.gameContext = gameContext;
        this.taskStepExecutor = taskStepExecutor;
    }

    @Override
    public TaskRunResult execute() {
        return execute(buildExecutionContext());
    }

    @Override
    public TaskRunResult execute(TaskExecutionContext executionContext) {
        TaskExecutionContext context = resolveExecutionContext(executionContext);
        log.info("====================================");
        log.info("启动任务：{}({})", getTaskName(), getTaskCode());
        log.info("====================================");

        try {
            beforeTask(context);
            context.throwIfStopRequested();

            List<TaskStep> steps = buildSteps(context);
            if (steps == null || steps.isEmpty()) {
                log.warn("任务没有配置步骤，直接跳过：{}", getTaskName());
                return TaskRunResult.SKIPPED;
            }

            for (TaskStep step : steps) {
                context.throwIfStopRequested();
                TaskStepResult stepResult = taskStepExecutor.execute(context, step, getRetryPolicy(context, step));
                TaskRunResult runResult = convertStepResult(stepResult);

                if (runResult == TaskRunResult.SUCCESS || runResult == TaskRunResult.SKIPPED) {
                    continue;
                }

                afterTask(context, runResult);
                return runResult;
            }

            afterTask(context, TaskRunResult.SUCCESS);
            return TaskRunResult.SUCCESS;
        } catch (TaskStopRequestedException e) {
            log.info("任务收到停止信号：{}，原因：{}", getTaskName(), e.getMessage());
            afterTask(context, TaskRunResult.STOPPED);
            return TaskRunResult.STOPPED;
        } catch (Exception e) {
            log.error("任务执行异常：{}", getTaskName(), e);
            afterTask(context, TaskRunResult.FAILED);
            return TaskRunResult.FAILED;
        }
    }

    @Override
    public void stop() {
        log.info("收到停止任务请求：{}", getTaskName());
        if (gameContext != null) {
            gameContext.setBotStatus(GameContext.BotStatus.IDLE);
            gameContext.setCurrentActionState(GameContext.ActionState.FREE);
        }
    }

    protected List<TaskStep> buildSteps(TaskExecutionContext context) {
        return Collections.emptyList();
    }

    protected void beforeTask(TaskExecutionContext context) {
        if (gameContext != null) {
            gameContext.setBotStatus(GameContext.BotStatus.RUNNING);
        }
    }

    protected void afterTask(TaskExecutionContext context, TaskRunResult result) {
        if (gameContext == null) {
            return;
        }
        if (result == TaskRunResult.SUCCESS || result == TaskRunResult.STOPPED || result == TaskRunResult.SKIPPED) {
            gameContext.setBotStatus(GameContext.BotStatus.IDLE);
            gameContext.setCurrentActionState(GameContext.ActionState.FREE);
        } else if (result == TaskRunResult.FAILED) {
            gameContext.setBotStatus(GameContext.BotStatus.ERROR);
        }
    }

    protected TaskRetryPolicy getRetryPolicy(TaskExecutionContext context, TaskStep step) {
        if (context != null && context.getRetryPolicy() != null) {
            return context.getRetryPolicy();
        }
        return TaskRetryPolicy.none();
    }

    protected TaskStepResult executeStep(TaskExecutionContext executionContext,
                                         String stepName,
                                         Function<TaskExecutionContext, TaskStepResult> action) {
        return executeStep(executionContext, stepName, action, null);
    }

    protected TaskStepResult executeStep(TaskExecutionContext executionContext,
                                         String stepName,
                                         Function<TaskExecutionContext, TaskStepResult> action,
                                         TaskRetryPolicy retryPolicy) {
        return taskStepExecutor.execute(executionContext, namedStep(stepName, action), retryPolicy);
    }

    protected TaskStep namedStep(String stepName, Function<TaskExecutionContext, TaskStepResult> action) {
        return new TaskStep() {
            @Override
            public TaskStepResult execute(TaskExecutionContext context) {
                return action.apply(context);
            }

            @Override
            public String getStepName() {
                return stepName;
            }
        };
    }

    protected void sleepSafely(TaskExecutionContext context, long millis) {
        if (millis <= 0) {
            return;
        }
        if (context != null) {
            context.throwIfStopRequested();
        }
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new TaskStopRequestedException("任务等待被中断");
        }
        if (context != null) {
            context.throwIfStopRequested();
        }
    }

    protected TaskExecutionContext resolveExecutionContext(TaskExecutionContext executionContext) {
        return executionContext == null ? buildExecutionContext() : executionContext;
    }

    protected TaskExecutionContext buildExecutionContext() {
        return TaskExecutionContext.builder()
                .taskCode(getTaskCode())
                .taskName(getTaskName())
                .retryPolicy(TaskRetryPolicy.none())
                .startedAt(LocalDateTime.now())
                .build();
    }

    private TaskRunResult convertStepResult(TaskStepResult stepResult) {
        if (stepResult == null) {
            return TaskRunResult.SUCCESS;
        }
        return switch (stepResult) {
            case SUCCESS -> TaskRunResult.SUCCESS;
            case FAILED -> TaskRunResult.FAILED;
            case SKIPPED -> TaskRunResult.SKIPPED;
            case STOPPED -> TaskRunResult.STOPPED;
        };
    }
}
