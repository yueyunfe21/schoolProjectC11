package com.bot.dhxy.runner;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 任务运行时状态快照。
 *
 * 作用：给 UI 查询当前任务系统状态。
 * 包含当前是否运行、最近启动请求、最近运行结果、开始结束时间和状态说明。
 */
@Getter
@Builder
public class TaskRuntimeState {

    private final boolean running;
    private final TaskRunRequest currentRequest;
    private final TaskRunSummary lastSummary;
    private final LocalDateTime startedAt;
    private final LocalDateTime finishedAt;
    private final String statusText;

    public static TaskRuntimeState idle() {
        return TaskRuntimeState.builder()
                .running(false)
                .statusText("空闲")
                .build();
    }
}
