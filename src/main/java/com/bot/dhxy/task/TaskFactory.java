package com.bot.dhxy.task;

import com.bot.dhxy.window.WindowRuntimeContext;

/**
 * 根据窗口上下文和任务类型创建任务实例。
 *
 * 多窗口模式下，每个窗口都应该拿到自己的 GameTask 实例，不能多个窗口共享同一个任务对象。
 */
public interface TaskFactory {

    GameTask createTask(WindowRuntimeContext windowContext, TaskType taskType);
}
