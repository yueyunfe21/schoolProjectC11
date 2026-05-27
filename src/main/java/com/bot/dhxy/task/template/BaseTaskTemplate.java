package com.bot.dhxy.task.template;

import com.bot.dhxy.core.GameContext;
import com.bot.dhxy.model.TaskRunResult;
import com.bot.dhxy.runner.context.TaskExecutionContext;
import com.bot.dhxy.runner.policy.TaskRetryPolicy;
import com.bot.dhxy.runner.stop.TaskSleep;
import com.bot.dhxy.runner.stop.TaskStopRequestedException;
import com.bot.dhxy.task.GameTask;
import com.bot.dhxy.window.interaction.TaskWindowRuntimeService;
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
        TaskSleep.sleepOrStop(context, millis, "任务等待被中断");
    }

    protected void logWindowContext(TaskExecutionContext context) {
        if (context == null || !context.hasWindow()) {
            log.info("{} 未绑定多窗口上下文，按单窗口兼容模式执行。", getTaskName());
            return;
        }
        log.info("{} 窗口上下文：windowId={}，role={}，hwnd={}，geometry={}，title={}",
                getTaskName(),
                context.getWindowId(),
                context.getWindowRole(),
                context.hasNativeWindow() ? context.getNativeWindowHandle() : "-",
                context.getNativeWindowGeometryText(),
                context.getNativeWindowTitle() == null || context.getNativeWindowTitle().isBlank() ? "-" : context.getNativeWindowTitle());
    }

    protected boolean activateWindowIfReady(TaskWindowRuntimeService taskWindowRuntimeService,
                                            TaskExecutionContext context,
                                            String actionName) {
        if (taskWindowRuntimeService == null) {
            log.info("{} 未注入窗口运行服务，跳过窗口激活。", actionName);
            return false;
        }
        if (context == null || !taskWindowRuntimeService.ready(context)) {
            log.info("{} 未绑定可用真实窗口，跳过窗口激活。", actionName);
            return false;
        }
        boolean activated = taskWindowRuntimeService.activate(context);
        log.info("{} 窗口激活结果：{} | {}", actionName, activated, taskWindowRuntimeService.describe(context));
        return activated;
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
