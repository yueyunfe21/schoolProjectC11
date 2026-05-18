package com.bot.dhxy.task.startup;

import com.bot.dhxy.model.TaskRunResult;

/**
 * 任务启动前置判断结果。
 *
 * window 层只负责启动任务；任务是否真的继续执行，由任务内部根据游戏状态判断。
 */
public class TaskStartupCheckResult {

    private final boolean allowed;
    private final TaskRunResult blockedResult;
    private final String reason;

    private TaskStartupCheckResult(boolean allowed, TaskRunResult blockedResult, String reason) {
        this.allowed = allowed;
        this.blockedResult = blockedResult == null ? TaskRunResult.SKIPPED : blockedResult;
        this.reason = reason == null || reason.isBlank() ? "-" : reason;
    }

    public static TaskStartupCheckResult allow(String reason) {
        return new TaskStartupCheckResult(true, TaskRunResult.SUCCESS, reason);
    }

    public static TaskStartupCheckResult allow() {
        return allow("允许执行");
    }

    public static TaskStartupCheckResult skip(String reason) {
        return new TaskStartupCheckResult(false, TaskRunResult.SKIPPED, reason);
    }

    public static TaskStartupCheckResult fail(String reason) {
        return new TaskStartupCheckResult(false, TaskRunResult.FAILED, reason);
    }

    public static TaskStartupCheckResult stop(String reason) {
        return new TaskStartupCheckResult(false, TaskRunResult.STOPPED, reason);
    }

    public boolean isAllowed() {
        return allowed;
    }

    public boolean isBlocked() {
        return !allowed;
    }

    public TaskRunResult getBlockedResult() {
        return blockedResult;
    }

    public String getReason() {
        return reason;
    }
}
