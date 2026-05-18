package com.bot.dhxy.runner.execution;

import com.bot.dhxy.runner.context.TaskContextFactory;
import com.bot.dhxy.runner.context.TaskExecutionContext;
import com.bot.dhxy.runner.definition.TaskRegistryService;
import com.bot.dhxy.runner.history.TaskRunHistoryService;
import com.bot.dhxy.runner.history.TaskRunRecord;
import com.bot.dhxy.runner.log.TaskLogService;
import com.bot.dhxy.runner.model.TaskRunRequest;
import com.bot.dhxy.runner.model.TaskRunSummary;
import com.bot.dhxy.runner.plan.TaskExecutionPlan;
import com.bot.dhxy.runner.stop.TaskStopRequestedException;
import com.bot.dhxy.runner.stop.TaskStopToken;
import com.bot.dhxy.task.GameTask;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 根据用户勾选结果调度任务。
 *
 * Runner 只负责调度，不写五环、修罗、抓鬼等具体业务逻辑。
 */
@Slf4j
@Component
public class TaskRunner {

    private final TaskRegistryService taskRegistryService;
    private final TaskRunHistoryService taskRunHistoryService;
    private final TaskLogService taskLogService;
    private final TaskContextFactory taskContextFactory;

    private volatile boolean stopRequested = false;
    private volatile TaskStopToken currentStopToken;

    public TaskRunner(TaskRegistryService taskRegistryService,
                      TaskRunHistoryService taskRunHistoryService,
                      TaskLogService taskLogService,
                      TaskContextFactory taskContextFactory) {
        this.taskRegistryService = taskRegistryService;
        this.taskRunHistoryService = taskRunHistoryService;
        this.taskLogService = taskLogService;
        this.taskContextFactory = taskContextFactory;
    }

    public TaskRunSummary run(TaskQueue queue) {
        return run(queue, false);
    }

    public TaskRunSummary run(TaskQueue queue, boolean testMode) {
        return run(queue, testMode, null, null);
    }

    /**
     * 执行任务队列。
     *
     * request / plan 用于创建 TaskExecutionContext，后续具体任务可以从上下文中读取参数、停止信号和执行计划。
     */
    public TaskRunSummary run(TaskQueue queue,
                              boolean testMode,
                              TaskRunRequest request,
                              TaskExecutionPlan plan) {
        TaskRunSummary summary = new TaskRunSummary();

        if (queue == null || queue.isEmpty()) {
            log.warn("没有选择任何任务，TaskRunner 不执行。");
            taskLogService.warn(null, null, "没有选择任何任务，TaskRunner 不执行");
            return summary;
        }

        stopRequested = false;
        currentStopToken = new TaskStopToken();
        log.info("启动任务队列: {} | loop={} | testMode={}", queue.getSelectedTaskCodes(), queue.isLoop(), testMode);
        taskLogService.info(null, null, "启动任务队列: " + queue.getSelectedTaskCodes() + " | loop=" + queue.isLoop() + " | testMode=" + testMode);

        try {
            do {
                for (String taskCode : queue.getSelectedTaskCodes()) {
                    if (isStopRequested()) {
                        log.info("收到停止信号，任务队列中止。");
                        taskLogService.warn(null, null, "收到停止信号，任务队列中止");
                        logSummary(summary);
                        return summary;
                    }

                    GameTask task = taskRegistryService.getTaskByCode(taskCode);
                    if (task == null) {
                        TaskRunRecord record = buildSkippedRecord(taskCode, "未注册任务", "未注册的任务编码");
                        summary.record(record);
                        taskRunHistoryService.addRecord(record);
                        taskLogService.warn(taskCode, "未注册任务", "未注册的任务编码，已跳过");
                        continue;
                    }

                    TaskExecutionContext context = taskContextFactory.create(
                            task,
                            request,
                            plan,
                            currentStopToken,
                            request == null ? Map.of() : request.getSafeParameters()
                    );

                    TaskRunRecord record = runSingleTask(task, context, testMode);
                    summary.record(record);
                    taskRunHistoryService.addRecord(record);
                    com.bot.dhxy.model.TaskRunResult result = record.getResult();

                    if (shouldStopQueue(result)) {
                        log.info("任务结果为 STOPPED，终止后续任务队列。");
                        taskLogService.warn(task.getTaskCode(), task.getTaskName(), "任务结果为 STOPPED，终止后续任务队列");
                        logSummary(summary);
                        return summary;
                    }
                    if (result == com.bot.dhxy.model.TaskRunResult.FAILED) {
                        log.warn("任务失败，但根据当前策略继续执行后续任务。");
                        taskLogService.warn(task.getTaskCode(), task.getTaskName(), "任务失败，但继续执行后续任务");
                    }
                }
            } while (queue.isLoop() && !isStopRequested() && !testMode);
        } finally {
            currentStopToken = null;
        }

        if (testMode && queue.isLoop()) {
            log.info("测试模式下不会真的循环执行，避免无限刷日志。");
            taskLogService.info(null, null, "测试模式下不会真的循环执行，避免无限刷日志");
        }
        log.info("任务队列执行完毕。");
        taskLogService.info(null, null, "任务队列执行完毕");
        logSummary(summary);
        return summary;
    }

    public void stop() {
        stopRequested = true;
        TaskStopToken token = currentStopToken;
        if (token != null) {
            token.requestStop("收到停止任务队列请求");
        }
        taskLogService.warn(null, null, "收到停止任务队列请求");
        taskRegistryService.getAllTasks().forEach(GameTask::stop);
    }

    private TaskRunRecord runSingleTask(GameTask task,
                                        TaskExecutionContext context,
                                        boolean testMode) {
        LocalDateTime startTime = LocalDateTime.now();
        LocalDateTime endTime;
        com.bot.dhxy.model.TaskRunResult result;
        String message = null;

        log.info("开始执行任务: [{}] {}", task.getTaskCode(), task.getTaskName());
        taskLogService.info(task.getTaskCode(), task.getTaskName(), "开始执行任务");
        if (testMode) {
            log.info("测试模式：跳过真实任务逻辑，仅验证任务队列调度。");
            taskLogService.info(task.getTaskCode(), task.getTaskName(), "测试模式：跳过真实任务逻辑，仅验证任务队列调度");
            result = com.bot.dhxy.model.TaskRunResult.SKIPPED;
            message = "测试模式跳过真实执行";
        } else {
            try {
                context.throwIfStopRequested();
                result = task.execute(context);
                if (result == null) {
                    log.warn("任务 [{}] {} 返回了 null，自动按 FAILED 处理。", task.getTaskCode(), task.getTaskName());
                    taskLogService.warn(task.getTaskCode(), task.getTaskName(), "任务返回 null，自动按 FAILED 处理");
                    result = com.bot.dhxy.model.TaskRunResult.FAILED;
                    message = "任务返回 null";
                }
            } catch (TaskStopRequestedException e) {
                result = com.bot.dhxy.model.TaskRunResult.STOPPED;
                message = e.getMessage();
                taskLogService.warn(task.getTaskCode(), task.getTaskName(), "任务收到停止请求: " + message);
            } catch (Exception e) {
                log.error("任务执行异常: [{}] {}，本任务记为 FAILED，继续执行后续任务。",
                        task.getTaskCode(), task.getTaskName(), e);
                result = com.bot.dhxy.model.TaskRunResult.FAILED;
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

        log.info("任务执行结束: {}", record.toLogText());
        taskLogService.info(task.getTaskCode(), task.getTaskName(), "任务执行结束: result=" + result + ", cost=" + record.getCostMillis() + "ms");
        return record;
    }

    private TaskRunRecord buildSkippedRecord(String taskCode, String taskName, String message) {
        LocalDateTime now = LocalDateTime.now();
        return TaskRunRecord.builder()
                .taskCode(taskCode)
                .taskName(taskName)
                .startTime(now)
                .endTime(now)
                .result(com.bot.dhxy.model.TaskRunResult.SKIPPED)
                .message(message)
                .build();
    }

    private boolean isStopRequested() {
        TaskStopToken token = currentStopToken;
        return stopRequested || (token != null && token.isStopRequested());
    }

    private boolean shouldStopQueue(com.bot.dhxy.model.TaskRunResult result) {
        return result == com.bot.dhxy.model.TaskRunResult.STOPPED;
    }

    private void logSummary(TaskRunSummary summary) {
        log.info("任务汇总: {}", summary.toLogText());
        taskLogService.info(null, null, "任务汇总: " + summary.toLogText());
        for (TaskRunRecord record : summary.getRecords()) {
            log.info("任务记录: {}", record.toLogText());
        }
    }
}
