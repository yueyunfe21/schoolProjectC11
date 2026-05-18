package com.bot.dhxy.runner;

/**
 * 任务主动停止异常。
 *
 * 作用：任务内部发现停止信号后，可以用这个异常快速跳出当前流程。
 */
public class TaskStopRequestedException extends RuntimeException {

    public TaskStopRequestedException(String message) {
        super(message);
    }
}
