package com.bot.dhxy.task;

import com.bot.dhxy.core.GameContext;
import com.bot.dhxy.model.TaskRunResult;
import com.bot.dhxy.runner.context.TaskExecutionContext;
import com.bot.dhxy.runner.policy.TaskRetryPolicy;
import com.bot.dhxy.service.BattleRadarService;
import com.bot.dhxy.task.template.BaseTaskTemplate;
import com.bot.dhxy.task.template.TaskStepExecutor;
import com.bot.dhxy.window.interaction.TaskWindowRuntimeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Slf4j
public class AutoBattleTask extends BaseTaskTemplate {

    private final BattleRadarService battleRadarService;
    private final TaskWindowRuntimeService taskWindowRuntimeService;

    public AutoBattleTask(GameContext gameContext,
                          TaskStepExecutor taskStepExecutor,
                          BattleRadarService battleRadarService,
                          TaskWindowRuntimeService taskWindowRuntimeService) {
        super(gameContext, taskStepExecutor);
        this.battleRadarService = battleRadarService;
        this.taskWindowRuntimeService = taskWindowRuntimeService;
    }

    @Override
    public String getTaskCode() {
        return "auto_battle";
    }

    @Override
    public String getTaskName() {
        return "自动战斗";
    }

    @Override
    public TaskRunResult execute() {
        return execute(null);
    }

    @Override
    public TaskRunResult execute(TaskExecutionContext executionContext) {
        TaskExecutionContext context = resolveExecutionContext(executionContext);
        log.info("====================================");
        log.info("⚔️ 启动自动战斗任务：{}", context.getLogPrefix());
        log.info("====================================");
        logWindowContext(context);

        gameContext.setBotStatus(GameContext.BotStatus.RUNNING);
        focusWindowIfPossible(context);

        while (gameContext.getBotStatus() == GameContext.BotStatus.RUNNING) {
            context.throwIfStopRequested();
            battleRadarService.checkAndSyncCombatState();
            sleepSafely(context, battleRadarService.getDynamicPollingIntervalMs());
        }

        log.info("自动战斗任务结束：{}", context.getLogPrefix());
        return TaskRunResult.STOPPED;
    }

    @Override
    public void stop() {
        log.info("🛑 收到停止自动战斗任务请求");
        gameContext.setBotStatus(GameContext.BotStatus.IDLE);
        gameContext.setCurrentActionState(GameContext.ActionState.FREE);
    }

    private void focusWindowIfPossible(TaskExecutionContext context) {
        activateWindowIfReady(taskWindowRuntimeService, context, "自动战斗");
        sleepSafely(context, 300);
    }

    @Override
    protected TaskRetryPolicy getRetryPolicy(TaskExecutionContext context, com.bot.dhxy.task.template.TaskStep step) {
        return TaskRetryPolicy.none();
    }
}
