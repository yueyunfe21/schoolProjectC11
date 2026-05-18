package com.bot.dhxy.runner.definition;

import com.bot.dhxy.runner.log.TaskLogService;
import com.bot.dhxy.task.GameTask;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 任务注册表。
 *
 * 负责统一收集所有 GameTask，并提供任务查询能力。
 * 后面界面可以通过它获取所有可勾选任务。
 */
@Slf4j
@Component
public class TaskRegistryService {

    private final Map<String, GameTask> taskMap = new LinkedHashMap<>();
    private final TaskLogService taskLogService;

    public TaskRegistryService(List<GameTask> tasks, TaskLogService taskLogService) {
        this.taskLogService = taskLogService;
        for (GameTask task : tasks) {
            GameTask old = taskMap.put(task.getTaskCode(), task);
            if (old != null) {
                throw new IllegalStateException("重复的任务编码: " + task.getTaskCode());
            }
            log.info("✅ 注册任务: [{}] {}", task.getTaskCode(), task.getTaskName());
            taskLogService.info(task.getTaskCode(), task.getTaskName(), "任务已注册");
        }
        log.info("📋 当前已注册任务清单: {}", getRegisteredTaskSummary());
        taskLogService.info(null, null, "当前已注册任务清单: " + getRegisteredTaskSummary());
    }

    public GameTask getTaskByCode(String taskCode) {
        if (taskCode == null) {
            return null;
        }
        return taskMap.get(taskCode.trim());
    }

    public List<TaskDefinition> getAllTaskDefinitions() {
        return taskMap.values().stream()
                .map(task -> new TaskDefinition(task.getTaskCode(), task.getTaskName()))
                .collect(Collectors.toList());
    }

    public String getRegisteredTaskSummary() {
        return taskMap.values().stream()
                .map(task -> task.getTaskCode() + "=" + task.getTaskName())
                .collect(Collectors.joining(", "));
    }

    public List<GameTask> getAllTasks() {
        return List.copyOf(taskMap.values());
    }
}
