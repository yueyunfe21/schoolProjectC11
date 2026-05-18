package com.bot.dhxy.runner;

/**
 * 任务失败处理策略。
 */
public enum TaskErrorPolicy {
    /** 遇到失败后停止整个任务队列。 */
    STOP_ON_ERROR,

    /** 当前任务失败后继续执行下一个任务。 */
    CONTINUE_NEXT_TASK,

    /** 当前步骤失败后按重试策略重试。 */
    RETRY_STEP,

    /** 只记录错误，不改变流程。 */
    LOG_ONLY
}
