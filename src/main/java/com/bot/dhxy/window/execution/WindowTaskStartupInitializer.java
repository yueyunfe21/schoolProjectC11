package com.bot.dhxy.window.execution;

import com.bot.dhxy.runner.context.TaskExecutionContext;
import com.bot.dhxy.window.runtime.WindowRuntimeContext;

/**
 * Per-window hook executed after a task is accepted by {@link WindowTaskRunner} and before the
 * concrete task logic starts.
 */
public interface WindowTaskStartupInitializer {

    /**
     * Prepare the bound game window for the next task.
     *
     * @param windowContext registered runtime context for the window that is about to run the task;
     *                      null only in legacy/debug paths.
     * @param executionContext task execution context containing task code, stop token, role, and
     *                         native-window metadata; null only before a task is fully attached.
     * @return true when task execution may continue; false means the runner should skip the task as
     * failed.
     */
    boolean beforeTask(WindowRuntimeContext windowContext, TaskExecutionContext executionContext);
}
