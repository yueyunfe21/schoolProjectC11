package com.bot.dhxy.runner;

import com.bot.dhxy.task.GameTask;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
    }

    public void run(TaskQueue queue) {
        if (queue == null || queue.isEmpty()) {
            log.warn("⚠️ 没有选择任何任务，TaskRunner 不执行。");
            return;
        }

        stopRequested = false;
        log.info("🚀 启动任务队列: {} | loop={}", queue.getSelectedTaskCodes(), queue.isLoop());

        do {
            for (String taskCode : queue.getSelectedTaskCodes()) {
                if (stopRequested) {
                    log.info("🛑 收到停止信号，任务队列中止。");
                    return;
                }

                GameTask task = taskMap.get(taskCode);
                if (task == null) {
                    log.warn("⚠️ 未注册的任务编码: [{}]，已跳过。", taskCode);
                    continue;
                }

                log.info("▶️ 开始执行任务: [{}] {}", task.getTaskCode(), task.getTaskName());
                task.execute();
                log.info("✅ 任务执行结束: [{}] {}", task.getTaskCode(), task.getTaskName());
            }
        } while (queue.isLoop() && !stopRequested);

        log.info("🎉 任务队列执行完毕。");
    }

    public void stop() {
        stopRequested = true;
        taskMap.values().forEach(GameTask::stop);
    }
}
