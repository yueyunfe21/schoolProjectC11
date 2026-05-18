package com.bot.dhxy.runner;

import lombok.Builder;
import lombok.Getter;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * 单个任务的一次执行记录。
 *
 * 后面界面的运行记录表可以直接展示这些字段。
 */
@Getter
@Builder
public class TaskRunRecord {

    private final String taskCode;
    private final String taskName;
    private final LocalDateTime startTime;
    private final LocalDateTime endTime;
    private final com.bot.dhxy.model.TaskRunResult result;
    private final String message;

    public long getCostMillis() {
        if (startTime == null || endTime == null) {
            return 0L;
        }
        return Duration.between(startTime, endTime).toMillis();
    }

    public String toLogText() {
        String extra = message == null || message.isBlank() ? "" : " | message=" + message;
        return String.format(
                "[%s] %s | result=%s | cost=%dms%s",
                taskCode,
                taskName,
                result,
                getCostMillis(),
                extra
        );
    }
}
