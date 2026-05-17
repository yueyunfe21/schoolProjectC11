package com.bot.dhxy.runner;

import com.bot.dhxy.model.TaskRunResult;
import com.bot.dhxy.task.GameTask;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

/**
 * 根据用户勾选结果调度任务。
 *
 * 注意：runner 只负责调度，不写五环、修罗、抓鬼等具体业务逻辑。
 */
@Slf4j
@Component
public class TaskRunner {

    private final TaskRegistryService taskRegistryService;
    private final TaskRunHistoryService taskRunHistoryService;
    private final TaskLogService taskLogService;
    private volatile boolean stopRequested = false;

    public TaskRunner(TaskRegistryService taskRegistryService,
                      TaskRunHistoryService taskRunHistoryService,
                      TaskLogService taskLogService) {
        this.taskRegistryService = taskRegistryService;
        this.taskRunHistoryService = taskRunHistoryService;
        this.taskLogService = taskLogService;
    }

    public TaskRunSummary run(TaskQueue queue) {
        return run(queue, false);
    }

    public TaskRunSummary run(TaskQueue queue, boolean testMode) {
        TaskRunSummary summary = new TaskRunSummary();

        if (queue == null || queue.isEmpty()) {
            log.warn("⚠️ 没有选择任何任务，TaskRunner 不执行。");
            taskLogService.warn(null, null, "没有选择任何任务，TaskRunner 不执行");
            return summary;
        }

        stopRequested = false;
        log.info("🚀 启动任务队列: {} | loop={} | testMode={}", queue.getSelectedTaskCodes(), queue.isLoop(), testMode);
        taskLogService.info(null, null, "启动任务队列: " + queue.getSelectedTaskCodes() + " | loop=" + queue.isLoop() + " | testMode=" + testMode);

        do {
            for (String taskCode : queue.getSelectedTaskCodes()) {
                if (stopRequested) {
                    log.info("🛑 收到停止信号，任务队列中止。");
                    taskLogService.warn(null, null, "收到停止信号，任务队列中止");
                    logSummary(summary);
                    return summary;
                }

                GameTask task = taskRegistryService.getTaskByCode(taskCode);
                if (task == null) {
                    log.warn("⚠️ 未注册的任务编码: [{}]，已跳过。已注册任务: {}", taskCode, taskRegistryService.getRegisteredTaskSummary());
                    taskLogService.warn(taskCode, "未注册任务", "未注册的任务编码，已跳过");
                    TaskRunRecord record = TaskRunRecord.builder()
                            .taskCode(taskCode)
                            .taskName("未注册任务")
                            .startTime(LocalDateTime.now())
                            .endTime(LocalDateTime.now())
                            .result(TaskRunResult.SKIPPED)
                            .message("未注册的任务编码")
                            .build();
                    summary.record(record);
                    taskRunHistoryService.addRecord(record);
                    continue;
                }

                TaskRunRecord record = runSingleTask(task, testMode);
                summary.record(record);
                taskRunHistoryService.addRecord(record);
                TaskRunResult result = record.getResult();

                if (shouldStopQueue(result)) {
                    log.info("🛑 任务结果为 STOPPED，终止后续任务队列。");
                    taskLogService.warn(task.getTaskCode(), task.getTaskName(), "任务结果为 STOPPED，终止后续任务队列");
                    logSummary(summary);
                    return summary;
                }
                if (result == TaskRunResult.FAILED) {
                    log.warn("⚠️ 任务失败，但根据当前策略继续执行后续任务。");
                    taskLogService.warn(task.getTaskCode(), task.getTaskName(), "任务失败，但继续执行后续任务");
                }
            }
        } while (queue.isLoop() && !stopRequested && !testMode);

        if (testMode && queue.isLoop()) {
            log.info("🧪 测试模式下不会真的循环执行，避免无限刷日志。");
            taskLogService.info(null, null, "测试模式下不会真的循环执行，避免无限刷日志");
        }
        log.info("🎉 任务队列执行完毕。");
        taskLogService.info(null, null, "任务队列执行完毕");
        logSummary(summary);
        return summary;
    }

    public void stop() {
        stopRequested = true;
        taskLogService.warn(null, null, "收到停止任务队列请求");
        taskRegistryService.getAllTasks().forEach(GameTask::stop);
    }

    private TaskRunRecord runSingleTask(GameTask task, boolean testMode) {
        LocalDateTime startTime = LocalDateTime.now();
        LocalDateTime endTime;
        TaskRunResult result;
        String message = null;

        log.info("▶️ 开始执行任务: [{}] {}", task.getTaskCode(), task.getTaskName());
        taskLogService.info(task.getTaskCode(), task.getTaskName(), "开始执行任务");
        if (testMode) {
            log.info("🧪 测试模式：跳过真实任务逻辑，仅验证任务队列调度。");
            taskLogService.info(task.getTaskCode(), task.getTaskName(), "测试模式：跳过真实任务逻辑，仅验证任务队列调度");
            result = TaskRunResult.SKIPPED;
            message = "测试模式跳过真实执行";
        } else {
            try {
                result = task.execute();
                if (result == null) {
                    log.warn("⚠️ 任务 [{}] {} 返回了 null，自动按 FAILED 处理。", task.getTaskCode(), task.getTaskName());
                    taskLogService.warn(task.getTaskCode(), task.getTaskName(), "任务返回 null，自动按 FAILED 处理");
                    result = TaskRunResult.FAILED;
                    message = "任务返回 null";
                }
            } catch (Exception e) {
                log.error("💥 任务执行异常: [{}] {}，本任务记为 FAILED，继续执行后续任务。",
                        task.getTaskCode(), task.getTaskName(), e);
                result = TaskRunResult.FAILED;
                message = e.getClass().getSimpleName() + ": " + e.getMessage();
                taskLogService.fail(task.getTaskCode(), task.getTaskName(), "任务执行异常: " + message);
            }
        }
        endTime = LocalDateTime.now();

        TaskRunRecord record = TaskRunRecord.builder()
                .taskCode(task.getTaskCode())
                .taskName(task.getTaskName())
                .startTime(startTime)
                .endTime(endTime)
                .result(result)
                .message(message)
                .build();

        log.info("✅ 任务执行结束: {}", record.toLogText());
        taskLogService.info(task.getTaskCode(), task.getTaskName(), "任务执行结束: result=" + result + ", cost=" + record.getCostMillis() + "ms");
        return record;
    }

    private boolean shouldStopQueue(TaskRunResult result) {
        return result == TaskRunResult.STOPPED;
    }

    private void logSummary(TaskRunSummary summary) {
        log.info("📊 任务汇总: {}", summary.toLogText());
        taskLogService.info(null, null, "任务汇总: " + summary.toLogText());
        for (TaskRunRecord record : summary.getRecords()) {
            log.info("📌 任务记录: {}", record.toLogText());
        }
    }
}
