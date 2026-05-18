package com.bot.dhxy.task.template;

import com.bot.dhxy.runner.TaskExecutionContext;

@FunctionalInterface
public interface TaskStep {

    TaskStepResult execute(TaskExecutionContext context);

    default String getStepName() {
        return getClass().getSimpleName();
    }
}
