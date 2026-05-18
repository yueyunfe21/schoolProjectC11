package com.bot.dhxy.runner;

import com.bot.dhxy.config.TaskRunProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 任务控制门面服务。
 *
 * 后面 UI 不需要直接依赖 TaskRunner、TaskRegistryService、TaskLogService、TaskRunHistoryService。
 * 统一通过这个类完成任务启动、停止、查询任务列表、查询日志、查询历史记录。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TaskControlService {

    private final TaskRunner taskRunner;
    private final TaskRegistryService taskRegistryService;
    private final TaskRunHistoryService taskRunHistoryService;
    private final TaskLogService taskLogService;
    private final TaskRunProperties taskRunProperties;

    /**
     * 防止重复启动任务队列。
     */
    private final AtomicBoolean running = new AtomicBoolean(false);

    /**
     * 按 application.properties 里的 bot.run 配置启动任务队列。
     */
    public TaskRunSummary startConfiguredTasks() {
        return startTasks(
                taskRunProperties.getNormalizedTasks(),
                taskRunProperties.isLoop(),
                taskRunProperties.isTestMode()
        );
    }

    /**
     * 按指定任务列表启动任务队列。
     *
     * 后面 UI 勾选任务后，可以直接调用这个方法。
     */
    public TaskRunSummary startTasks(List<String> taskCodes, boolean loop, boolean testMode) {
        if (!running.compareAndSet(false, true)) {
            log.warn("⚠️ 当前已有任务队列正在运行，忽略重复启动请求。{}");
            taskLogService.warn(null, null, "当前已有任务队列正在运行，忽略重复启动请求");
            return new TaskRunSummary();
        }

        try {
            TaskQueue queue = new TaskQueue(taskCodes, loop);
            return taskRunner.run(queue, testMode);
        } finally {
            running.set(false);
        }
    }

    /**
     * 请求停止当前任务队列。
     */
    public void stop() {
        taskRunner.stop();
    }

    /**
     * 当前是否有任务队列正在运行。
     */
    public boolean isRunning() {
        return running.get();
    }

    /**
     * 获取当前程序支持的任务列表，用于 UI 生成勾选框。
     */
    public List<TaskDefinition> getAvailableTasks() {
        return taskRegistryService.getAllTaskDefinitions();
    }

    /**
     * 获取最近任务执行记录，用于 UI 任务记录表。
     */
    public List<TaskRunRecord> getRecentTaskRecords() {
        return taskRunHistoryService.getRecentRecords();
    }

    /**
     * 获取最近任务日志，用于 UI 日志面板。
     */
    public List<TaskLogEntry> getRecentLogs() {
        return taskLogService.getRecentLogs();
    }

    /**
     * 清空任务运行记录和任务日志。
     */
    public void clearRuntimeLogs() {
        taskRunHistoryService.clear();
        taskLogService.clear();
    }

    /**
     * 获取当前程序支持任务的一行说明。
     */
    public String getRegisteredTaskSummary() {
        return taskRegistryService.getRegisteredTaskSummary();
    }
}
