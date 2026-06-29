package com.bot.dhxy.task;

import com.bot.dhxy.model.TaskRunResult;
import com.bot.dhxy.runner.context.TaskExecutionContext;
import com.bot.dhxy.runner.stop.TaskCheckpoint;
import com.bot.dhxy.runner.stop.TaskSleep;
import com.bot.dhxy.service.SystemPowerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * Explicit queue task that puts the host computer to sleep.
 *
 * <p>This task should only run when the user has placed it in the task queue. Queue submission uses
 * {@code STOP_ON_FAILURE} whenever this task is present, so a failed earlier game task will not
 * accidentally sleep the machine.</p>
 */
@Slf4j
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@RequiredArgsConstructor
public class SleepComputerTask implements GameTask {

    private static final long BEFORE_SLEEP_LOG_FLUSH_MS = 1_500L;

    private final SystemPowerService systemPowerService;

    @Override
    public String getTaskCode() {
        return "sleep_computer";
    }

    @Override
    public String getTaskName() {
        return "睡眠计算机";
    }

    @Override
    public TaskRunResult execute() {
        return execute(null);
    }

    @Override
    public TaskRunResult execute(TaskExecutionContext context) {
        TaskCheckpoint.throwIfStopRequested(context, "Sleep computer task interrupted");
        log.warn("[sleep-computer] task selected; Windows sleep will be requested after {}ms",
                BEFORE_SLEEP_LOG_FLUSH_MS);
        TaskSleep.sleepOrStop(context, BEFORE_SLEEP_LOG_FLUSH_MS, "Sleep computer task interrupted");
        systemPowerService.sleepComputer("task-queue");
        return TaskRunResult.SUCCESS;
    }

    @Override
    public void stop() {
        log.info("[sleep-computer] stop requested before system sleep");
    }
}
