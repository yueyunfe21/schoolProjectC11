package com.bot.dhxy.window.service;

import com.bot.dhxy.task.model.TaskType;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * UI 点击“启动”时传入的统一请求对象。
 */
public class WindowTaskStartRequest {

    private final List<String> windowIds;
    private final WindowTaskStartMode startMode;
    private final TaskType taskType;

    public WindowTaskStartRequest(Collection<String> windowIds,
                                  WindowTaskStartMode startMode,
                                  TaskType taskType) {
        this.windowIds = normalizeWindowIds(windowIds);
        this.startMode = startMode == null ? WindowTaskStartMode.SELECTED_TASK : startMode;
        this.taskType = taskType == null ? TaskType.UNKNOWN : taskType;
    }

    public static WindowTaskStartRequest sameTask(Collection<String> windowIds, TaskType taskType) {
        return new WindowTaskStartRequest(windowIds, WindowTaskStartMode.SAME_TASK, taskType);
    }

    public static WindowTaskStartRequest selectedTask(Collection<String> windowIds) {
        return new WindowTaskStartRequest(windowIds, WindowTaskStartMode.SELECTED_TASK, TaskType.UNKNOWN);
    }

    public static WindowTaskStartRequest detectedRole(Collection<String> windowIds, TaskType leaderTaskType) {
        return new WindowTaskStartRequest(windowIds, WindowTaskStartMode.DETECTED_ROLE, leaderTaskType);
    }

    public List<String> getWindowIds() {
        return windowIds;
    }

    public WindowTaskStartMode getStartMode() {
        return startMode;
    }

    public TaskType getTaskType() {
        return taskType;
    }

    public boolean hasWindows() {
        return !windowIds.isEmpty();
    }

    private static List<String> normalizeWindowIds(Collection<String> windowIds) {
        if (windowIds == null || windowIds.isEmpty()) {
            return Collections.emptyList();
        }
        return windowIds.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(id -> !id.isEmpty())
                .distinct()
                .toList();
    }
}
