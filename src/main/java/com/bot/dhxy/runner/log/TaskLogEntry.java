package com.bot.dhxy.runner.log;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 单条任务日志。
 *
 * 后面界面的日志面板可以直接展示这些字段。
 */
@Getter
@Builder
public class TaskLogEntry {

    private final LocalDateTime time;
    private final TaskLogType type;
    private final String taskCode;
    private final String taskName;
    private final String message;

    public String toLogText() {
        String taskPart = taskCode == null || taskCode.isBlank()
                ? ""
                : "[" + taskCode + "] " + (taskName == null ? "" : taskName + " ");
        return String.format("%s %s%s", type, taskPart, message);
    }
}
