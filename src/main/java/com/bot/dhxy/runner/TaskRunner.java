package com.bot.dhxy.runner;

import com.bot.dhxy.model.TaskRunResult;
import com.bot.dhxy.task.GameTask;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 根据用户勾选结果调度任务。
 *
 * 注意：runner 只负责调度，不写五环、修罗、抓鬼等具体业务逻辑。
 */
@Slf4j
@Component
public class TaskRunner {

    private final Map<String, GameTask> taskMap = new LinkedHashMap<>();
    private volatile boolean stopRequested = false;

    public TaskRunner(List<GameTask> tasks) {
        for (GameTask task : tasks) {
            GameTask old = taskMap.put(task.getTaskCode(), task);
            if (old != null) {
                throw new IllegalStateException("重复的任务编码: " + task.getTaskCode());
            }
            log.info("✅ 注册任务: [{}] {}", task.getTaskCode(), task.getTaskName());
        }
        log.info("📋 当前已注册任务清单: {}", getRegisteredTaskSummary());
    }

    public void run(TaskQueue queue) {
        run(queue, false);
    }

    public void run(TaskQueue queue, boolean testMode) {
        if (queue == null || queue.isEmpty()) {
            log.warn("⚠️ 没有选择任何任务，TaskRunner 不执行。");
            return;
        }

        stopRequested = false;
        log.info("🚀 启动任务队列: {} | loop={} | testMode={}", queue.getSelectedTaskCodes(), queue.isLoop(), testMode);

        do {
            for (String taskCode : queue.getSelectedTaskCodes()) {
                if (stopRequested) {
                    log.info("🛑 收到停止信号，任务队列中止。");
                    return;
                }

                GameTask task = taskMap.get(taskCode);
                if (task == null) {
                    log.warn("⚠️ 未注册的任务编码: [{}]，已跳过。已注册任务: {}", taskCode, getRegisteredTaskSummary());
                    continue;
                }

                log.info("▶️ 开始执行任务: [{}] {}", task.getTaskCode(), task.getTaskName());
                TaskRunResult result;
                if (testMode) {
                    log.info("🧪 测试模式：跳过真实任务逻辑，仅验证任务队列调度。");
                    result = TaskRunResult.SKIPPED;
                } else {
                    result = task.execute();
                }

                log.info("✅ 任务执行结束: [{}] {} | result={}", task.getTaskCode(), task.getTaskName(), result);
                if (shouldStopQueue(result)) {
                    log.info("🛑 任务结果为 STOPPED，终止后续任务队列。");
                    return;
                }
                if (result == TaskRunResult.FAILED) {
                    log.warn("⚠️ 任务失败，但根据当前策略继续执行后续任务。");
                }
            }
        } while (queue.isLoop() && !stopRequested && !testMode);

        if (testMode && queue.isLoop()) {
            log.info("🧪 测试模式下不会真的循环执行，避免无限刷日志。");
        }
        log.info("🎉 任务队列执行完毕。");
    }

    public void stop() {
        stopRequested = true;
        taskMap.values().forEach(GameTask::stop);
    }

    private boolean shouldStopQueue(TaskRunResult result) {
        return result == TaskRunResult.STOPPED;
    }

    private String getRegisteredTaskSummary() {
        return taskMap.values().stream()
                .map(task -> task.getTaskCode() + "=" + task.getTaskName())
                .collect(Collectors.joining(", "));
    }
}
