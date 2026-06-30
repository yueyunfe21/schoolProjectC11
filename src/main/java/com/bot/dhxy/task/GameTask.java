package com.bot.dhxy.task;

import com.bot.dhxy.model.TaskRunResult;
import com.bot.dhxy.runner.context.TaskExecutionContext;

/**
 * 统一任务接口。
 *
 * task 包下面只放五环、五倍、修罗这类可以被用户勾选执行的独立业务任务。
 * Runner 只依赖这个接口，不直接关心每个任务内部怎么跑。
 */
public interface GameTask {

    /**
     * 程序内部使用的任务编码，例如：wuhuan、wubei、xiuluo。
     */
    String getTaskCode();

    /**
     * UI 或日志中展示的任务名称，例如：五环、五倍、修罗。
     */
    String getTaskName();

    /**
     * 执行任务主流程，并返回本次执行结果。
     */
    TaskRunResult execute();

    /**
     * 带上下文执行任务。
     *
     * 新任务建议实现这个方法；老任务可以继续只实现 execute()。
     */
    default TaskRunResult execute(TaskExecutionContext context) {
        return execute();
    }

    /**
     * 请求停止任务。
     */
    void stop();
}
