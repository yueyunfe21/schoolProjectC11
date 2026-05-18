package com.bot.dhxy.runner.execution;

/**
 * 单个任务步骤动作。
 */
@FunctionalInterface
public interface TaskStepAction {

    TaskStepResult execute() throws Exception;
}
