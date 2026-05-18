package com.bot.dhxy.runner;

/**
 * 单个任务步骤动作。
 */
@FunctionalInterface
public interface TaskStepAction {

    TaskStepResult execute() throws Exception;
}
