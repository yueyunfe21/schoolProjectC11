package com.bot.dhxy.model;

/**
 * 单次任务执行结果。
 *
 * 注意：这不是全局 BotStatus，也不是角色 ActionState。
 * 它只描述某一个 GameTask.execute() 执行完之后的结果。
 */
public enum TaskRunResult {
    /**
     * 任务正常完成。
     */
    SUCCESS,

    /**
     * 任务执行失败。
     */
    FAILED,

    /**
     * 任务被外部停止。
     */
    STOPPED,

    /**
     * 任务被跳过，例如配置不满足、当前阶段不适合执行。
     */
    SKIPPED
}
