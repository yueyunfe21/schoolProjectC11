package com.bot.dhxy.cloud.turn;

import com.bot.dhxy.task.model.TaskType;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 2026-08-23 用户报障（UI"当前"列一直显示鬼王）：快照里的运行中任务取的是
 * {@code remoteQueue.firstTaskType()}——永远是队列第一个任务，云端队列推进到第二个任务
 * （鬼王→修罗）后 UI 不会变。云端每个队列元素启动时都会推 {@code TASK_STARTED} 队列事件
 * 到客户端（此前只进 metrics），本类把该事件里的 taskCode 按窗口存下来，快照优先读它。
 *
 * <p>纯展示用途：无控制权、无输入权；查不到映射时回退队列第一个任务的旧行为。</p>
 */
public final class RemoteRunningTaskDisplayStore {

    private static final ConcurrentHashMap<String, TaskType> CURRENT_BY_WINDOW = new ConcurrentHashMap<>();

    private RemoteRunningTaskDisplayStore() {
    }

    /** 记录云端队列事件宣布的当前元素；未知 taskCode 忽略（保持旧显示）。 */
    public static void recordStarted(String windowId, String taskCode) {
        if (windowId == null || windowId.isBlank()) {
            return;
        }
        TaskType resolved = resolve(taskCode);
        if (resolved != TaskType.UNKNOWN) {
            CURRENT_BY_WINDOW.put(windowId, resolved);
        }
    }

    /** 新一轮远程启动时清掉上一轮的残留显示。 */
    public static void clear(String windowId) {
        if (windowId != null) {
            CURRENT_BY_WINDOW.remove(windowId);
        }
    }

    /** 展示读取：有云端宣布的当前任务用它，否则回退调用方给的旧值。 */
    public static TaskType currentOrDefault(String windowId, TaskType fallback) {
        if (windowId == null) {
            return fallback;
        }
        TaskType current = CURRENT_BY_WINDOW.get(windowId);
        return current == null ? fallback : current;
    }

    private static TaskType resolve(String taskCode) {
        if (taskCode == null || taskCode.isBlank()) {
            return TaskType.UNKNOWN;
        }
        for (TaskType type : TaskType.values()) {
            if (type.getCode().equalsIgnoreCase(taskCode)) {
                return type;
            }
        }
        return TaskType.UNKNOWN;
    }
}
