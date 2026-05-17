package com.bot.dhxy.task;

/**
 * 统一任务接口。
 *
 * task 包下面只放五环、修罗、抓鬼这类可以被用户勾选执行的独立业务任务。
 * Runner 只依赖这个接口，不直接关心每个任务内部怎么跑。
 */
public interface GameTask {

    /**
     * 程序内部使用的任务编码，例如：wuhuan、xiuluo、zhuagui。
     */
    String getTaskCode();

    /**
     * UI 或日志中展示的任务名称，例如：五环、修罗、抓鬼。
     */
    String getTaskName();

    /**
     * 执行任务主流程。
     */
    void execute();

    /**
     * 请求停止任务。
     */
    void stop();
}
