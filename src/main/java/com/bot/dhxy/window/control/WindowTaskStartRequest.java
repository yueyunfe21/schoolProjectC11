package com.bot.dhxy.window.control;

import com.bot.dhxy.task.model.TaskType;
import com.bot.dhxy.window.execution.WindowTaskFailurePolicy;
import com.bot.dhxy.window.execution.WindowTaskQueue;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class WindowTaskStartRequest {

    private final List<String> windowIds;
    private final WindowTaskStartMode startMode;
    private final WindowTaskQueue taskQueue;

    public WindowTaskStartRequest(Collection<String> windowIds,
                                  WindowTaskStartMode startMode,
                                  TaskType taskType) {
        this(windowIds, startMode, WindowTaskQueue.single(taskType));
    }

    public WindowTaskStartRequest(Collection<String> windowIds,
                                  WindowTaskStartMode startMode,
                                  WindowTaskQueue taskQueue) {
        this.windowIds = normalizeWindowIds(windowIds);
        this.startMode = startMode == null ? WindowTaskStartMode.SELECTED_TASK : startMode;
        this.taskQueue = taskQueue == null ? WindowTaskQueue.empty() : taskQueue;
    }

    public static WindowTaskStartRequest sameTask(Collection<String> windowIds, TaskType taskType) {
        return new WindowTaskStartRequest(windowIds, WindowTaskStartMode.SAME_TASK, taskType);
    }

    public static WindowTaskStartRequest sameTask(Collection<String> windowIds,
                                                  TaskType taskType,
                                                  WindowTaskFailurePolicy failurePolicy) {
        return sameQueue(windowIds, WindowTaskQueue.single(taskType).withFailurePolicy(failurePolicy));
    }

    public static WindowTaskStartRequest sameQueue(Collection<String> windowIds, WindowTaskQueue taskQueue) {
        return new WindowTaskStartRequest(windowIds, WindowTaskStartMode.SAME_TASK, taskQueue);
    }

    public static WindowTaskStartRequest selectedTask(Collection<String> windowIds) {
        return new WindowTaskStartRequest(windowIds, WindowTaskStartMode.SELECTED_TASK, WindowTaskQueue.empty());
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
        return taskQueue.firstTaskType();
    }

    public WindowTaskQueue getTaskQueue() {
        return taskQueue;
    }

    public boolean hasWindows() {
        return !windowIds.isEmpty();
    }

    public int getWindowCount() {
        return windowIds.size();
    }

    public boolean isSameTaskMode() {
        return startMode == WindowTaskStartMode.SAME_TASK;
    }

    public boolean isSelectedTaskMode() {
        return startMode == WindowTaskStartMode.SELECTED_TASK;
    }

    public boolean isDetectedRoleMode() {
        return startMode == WindowTaskStartMode.DETECTED_ROLE;
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
