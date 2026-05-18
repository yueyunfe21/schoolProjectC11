package com.bot.dhxy.runner;

import lombok.Builder;
import lombok.Getter;

/**
 * 一次任务启动请求的处理结果。
 *
 * 作用：让 UI / 自动启动入口能明确知道启动请求是否被接受、最终状态是什么、失败原因是什么。
 */
@Getter
@Builder
public class TaskRunResult {

    /**
     * 本次请求是否被接受进入执行流程。
     */
    private final boolean accepted;

    /**
     * 本次请求最终状态。
     */
    private final TaskRunStatus status;

    /**
     * 状态说明。
     */
    private final String message;

    /**
     * 本次启动请求。
     */
    private final TaskRunRequest request;

    /**
     * 任务执行汇总。
     */
    private final TaskRunSummary summary;

    public static TaskRunResult rejected(TaskRunStatus status, String message, TaskRunRequest request) {
        return TaskRunResult.builder()
                .accepted(false)
                .status(status)
                .message(message)
                .request(request)
                .summary(new TaskRunSummary())
                .build();
    }

    public static TaskRunResult accepted(TaskRunStatus status, String message, TaskRunRequest request, TaskRunSummary summary) {
        return TaskRunResult.builder()
                .accepted(true)
                .status(status)
                .message(message)
                .request(request)
                .summary(summary == null ? new TaskRunSummary() : summary)
                .build();
    }

    public String toLogText() {
        return "accepted=" + accepted
                + " | status=" + status
                + " | message=" + message
                + " | request=" + (request == null ? "-" : request.toLogText())
                + " | summary=" + (summary == null ? "-" : summary.toLogText());
    }
}
