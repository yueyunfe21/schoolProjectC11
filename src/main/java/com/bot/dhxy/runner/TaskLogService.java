package com.bot.dhxy.runner;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * 任务日志服务。
 *
 * 目前先保存在内存中，默认只保留最近 200 条任务日志。
 * 后面接界面后，可以直接读取这里的数据展示滚动日志。
 */
@Component
public class TaskLogService {

    private static final int MAX_LOGS = 200;
    private final LinkedList<TaskLogEntry> logs = new LinkedList<>();

    public void info(String taskCode, String taskName, String message) {
        add(TaskLogType.INFO, taskCode, taskName, message);
    }

    public void warn(String taskCode, String taskName, String message) {
        add(TaskLogType.WARN, taskCode, taskName, message);
    }

    public void fail(String taskCode, String taskName, String message) {
        add(TaskLogType.FAIL, taskCode, taskName, message);
    }

    public synchronized void add(TaskLogType type, String taskCode, String taskName, String message) {
        TaskLogEntry entry = TaskLogEntry.builder()
                .time(LocalDateTime.now())
                .type(type)
                .taskCode(taskCode)
                .taskName(taskName)
                .message(message)
                .build();

        logs.addFirst(entry);
        while (logs.size() > MAX_LOGS) {
            logs.removeLast();
        }
    }

    public synchronized List<TaskLogEntry> getRecentLogs() {
        return Collections.unmodifiableList(new ArrayList<>(logs));
    }

    public synchronized void clear() {
        logs.clear();
    }

    public int getMaxLogs() {
        return MAX_LOGS;
    }
}
