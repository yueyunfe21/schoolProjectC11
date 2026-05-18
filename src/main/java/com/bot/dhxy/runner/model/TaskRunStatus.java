package com.bot.dhxy.runner.model;

/**
 * 任务运行状态。
 *
 * 作用：用标准枚举表达任务系统当前状态，避免 UI 依赖中文 statusText 做判断。
 */
public enum TaskRunStatus {
    /** 空闲，没有任务运行。 */
    IDLE,

    /** 正在准备启动任务。 */
    STARTING,

    /** 正在初始化游戏窗口。 */
    INITIALIZING_WINDOW,

    /** 任务队列正在执行。 */
    RUNNING,

    /** 已发送停止请求，等待任务真正退出。 */
    STOPPING,

    /** 任务队列已经停止。 */
    STOPPED,

    /** 任务队列正常执行完成。 */
    COMPLETED,

    /** 启动请求被拒绝，例如没有任务或重复启动。 */
    REJECTED,

    /** 任务启动或执行流程失败。 */
    FAILED
}
