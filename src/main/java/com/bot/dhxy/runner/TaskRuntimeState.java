package com.bot.dhxy.runner;

import lombok.Builder;
import lombok.Getter;

import java.time.Duration;
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

    /**
     * 获取当前状态持续时间。
     *
     * 运行中：从 startedAt 到当前时间。
     * 已结束：从 startedAt 到 finishedAt。
     */
    public long getElapsedMillis() {
        if (startedAt == null) {
            return 0L;
        }
        LocalDateTime end = running || finishedAt == null ? LocalDateTime.now() : finishedAt;
        return Math.max(0L, Duration.between(startedAt, end).toMillis());
    }

    public String getElapsedText() {
        long costMillis = getElapsedMillis();
        if (costMillis < 1000) {
            return costMillis + "ms";
        }
        long seconds = costMillis / 1000;
        long minutes = seconds / 60;
        long remainSeconds = seconds % 60;
        if (minutes <= 0) {
            return seconds + "s";
        }
        return minutes + "m " + remainSeconds + "s";
    }
}
