package com.bot.dhxy.runner.control;

import com.bot.dhxy.config.TaskRunProperties;
import com.bot.dhxy.runner.definition.TaskDefinition;
import com.bot.dhxy.runner.definition.TaskRegistryService;
import com.bot.dhxy.runner.execution.TaskQueue;
import com.bot.dhxy.runner.execution.TaskRunner;
import com.bot.dhxy.runner.history.TaskRunHistoryService;
import com.bot.dhxy.runner.history.TaskRunRecord;
import com.bot.dhxy.runner.log.TaskLogEntry;
import com.bot.dhxy.runner.log.TaskLogService;
import com.bot.dhxy.runner.model.TaskRunRequest;
import com.bot.dhxy.runner.model.TaskRunResult;
import com.bot.dhxy.runner.model.TaskRunStatus;
import com.bot.dhxy.runner.model.TaskRunSummary;
import com.bot.dhxy.runner.model.TaskRuntimeState;
import com.bot.dhxy.runner.plan.TaskExecutionPlan;
import com.bot.dhxy.runner.plan.TaskPlanService;
import com.bot.dhxy.service.GameWindowService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 任务控制门面服务。
 *
 * UI、自动启动、后续接口层都统一通过这里启动、停止、查询任务状态。
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
    private final GameWindowService gameWindowService;
    private final TaskPlanService taskPlanService;

    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicBoolean stopping = new AtomicBoolean(false);
    private volatile TaskRuntimeState runtimeState = TaskRuntimeState.idle();

    public TaskRunResult startConfiguredTasks() {
        return startTasks(TaskRunRequest.builder()
                .taskCodes(taskRunProperties.getNormalizedTasks())
                .loop(taskRunProperties.isLoop())
                .testMode(taskRunProperties.isTestMode())
                .initGameWindow(taskRunProperties.isInitGameWindow())
                .build());
    }

    public TaskRunResult startTasks(List<String> taskCodes, boolean loop, boolean testMode) {
        return startTasks(TaskRunRequest.builder()
                .taskCodes(taskCodes)
                .loop(loop)
                .testMode(testMode)
                .build());
    }

    public TaskRunResult startTasks(TaskRunRequest request) {
        TaskExecutionPlan plan = taskPlanService.buildPlan(request);
        if (plan.isEmpty()) {
            return rejectStart(request, plan, "启动被拒绝：没有可执行任务");
        }
        if (!running.compareAndSet(false, true)) {
            return rejectStart(request, plan, "启动被拒绝：当前已有任务正在运行");
        }

        stopping.set(false);
        LocalDateTime startedAt = LocalDateTime.now();
        runtimeState = buildState(TaskRunStatus.STARTING, true, false, request, null, startedAt, null, "运行中：准备启动任务");

        TaskRunSummary summary = new TaskRunSummary();
        try {
            log.info("接收到任务启动请求: {}", request.toLogText());
            log.info("生成任务执行计划: {}", plan.toLogText());
            taskLogService.info(null, null, "接收到任务启动请求: " + request.toLogText());
            taskLogService.info(null, null, "生成任务执行计划: " + plan.toLogText());
            logPlanWarnings(plan);

            if (!prepareBeforeRun(plan, startedAt, summary)) {
                return TaskRunResult.accepted(TaskRunStatus.FAILED, "启动失败：游戏窗口初始化失败", request, plan, summary);
            }

            runtimeState = buildState(TaskRunStatus.RUNNING, true, false, request, null, startedAt, null, "运行中：任务队列执行中");
            TaskQueue queue = new TaskQueue(plan.getTaskCodes(), plan.isLoop());
            summary = taskRunner.run(queue, plan.isTestMode(), request, plan);

            boolean stopped = stopping.get();
            TaskRunStatus finalStatus = stopped ? TaskRunStatus.STOPPED : TaskRunStatus.COMPLETED;
            String finalMessage = stopped ? "空闲：任务队列已停止" : "空闲：任务队列执行完毕";
            runtimeState = buildState(finalStatus, false, false, request, summary, startedAt, LocalDateTime.now(), finalMessage);
            return TaskRunResult.accepted(finalStatus, finalMessage, request, plan, summary);
        } catch (Exception e) {
            String message = "异常结束：" + e.getClass().getSimpleName();
            log.error("任务启动或执行流程发生异常。", e);
            taskLogService.fail(null, null, "任务启动或执行流程发生异常: " + e.getClass().getSimpleName() + ": " + e.getMessage());
            runtimeState = buildState(TaskRunStatus.FAILED, false, false, request, summary, startedAt, LocalDateTime.now(), message);
            return TaskRunResult.accepted(TaskRunStatus.FAILED, message, request, plan, summary);
        } finally {
            stopping.set(false);
            running.set(false);
        }
    }

    public TaskRunResult stop() {
        if (!running.get()) {
            String message = "空闲：当前没有正在运行的任务";
            runtimeState = TaskRuntimeState.builder()
                    .status(TaskRunStatus.IDLE)
                    .running(false)
                    .stopping(false)
                    .currentRequest(runtimeState.getCurrentRequest())
                    .lastSummary(runtimeState.getLastSummary())
                    .startedAt(runtimeState.getStartedAt())
                    .finishedAt(runtimeState.getFinishedAt())
                    .statusText(message)
                    .build();
            return TaskRunResult.rejected(TaskRunStatus.IDLE, message, runtimeState.getCurrentRequest());
        }

        stopping.set(true);
        taskRunner.stop();
        String message = "停止中：已发送停止请求，等待任务退出";
        runtimeState = TaskRuntimeState.builder()
                .status(TaskRunStatus.STOPPING)
                .running(true)
                .stopping(true)
                .currentRequest(runtimeState.getCurrentRequest())
                .lastSummary(runtimeState.getLastSummary())
                .startedAt(runtimeState.getStartedAt())
                .statusText(message)
                .build();
        taskLogService.warn(null, null, "已发送停止请求，等待任务退出");
        return TaskRunResult.accepted(TaskRunStatus.STOPPING, message, runtimeState.getCurrentRequest(), runtimeState.getLastSummary());
    }

    private TaskRunResult rejectStart(TaskRunRequest request, TaskExecutionPlan plan, String message) {
        log.warn(message);
        taskLogService.warn(null, null, message);
        if (plan != null && plan.hasIgnoredTasks()) {
            logPlanWarnings(plan);
        }
        runtimeState = TaskRuntimeState.builder()
                .status(TaskRunStatus.REJECTED)
                .running(running.get())
                .stopping(stopping.get())
                .currentRequest(runtimeState.getCurrentRequest())
                .lastSummary(runtimeState.getLastSummary())
                .startedAt(runtimeState.getStartedAt())
                .finishedAt(runtimeState.getFinishedAt())
                .statusText(message)
                .build();
        return TaskRunResult.rejected(TaskRunStatus.REJECTED, message, request, plan);
    }

    private void logPlanWarnings(TaskExecutionPlan plan) {
        if (plan == null || !plan.hasIgnoredTasks()) {
            return;
        }
        String warningText = plan.getWarningText();
        log.warn(warningText);
        taskLogService.warn(null, null, warningText);
    }

    private boolean prepareBeforeRun(TaskExecutionPlan plan, LocalDateTime startedAt, TaskRunSummary summary) {
        TaskRunRequest request = plan.getRequest();
        if (!plan.isInitGameWindow()) {
            log.warn("本次任务启动请求跳过游戏窗口初始化，仅适合测试任务队列或 UI。");
            taskLogService.warn(null, null, "本次任务启动请求跳过游戏窗口初始化");
            return true;
        }

        runtimeState = buildState(TaskRunStatus.INITIALIZING_WINDOW, true, false, request, null, startedAt, null, "运行中：正在初始化游戏窗口");
        taskLogService.info(null, null, "准备初始化游戏窗口");
        boolean ready = gameWindowService.initGameWindow();
        if (!ready) {
            String message = "启动失败：游戏窗口初始化失败";
            log.error(message);
            taskLogService.fail(null, null, message);
            runtimeState = buildState(TaskRunStatus.FAILED, false, false, request, summary, startedAt, LocalDateTime.now(), message);
        }
        return ready;
    }

    public boolean isRunning() {
        return running.get();
    }

    public boolean isStopping() {
        return stopping.get();
    }

    public TaskRuntimeState getRuntimeState() {
        return runtimeState;
    }

    public void resetRuntimeState() {
        if (!running.get()) {
            runtimeState = TaskRuntimeState.idle();
        }
    }

    public List<TaskDefinition> getAvailableTasks() {
        return taskRegistryService.getAllTaskDefinitions();
    }

    public List<TaskRunRecord> getRecentTaskRecords() {
        return taskRunHistoryService.getRecentRecords();
    }

    public List<TaskLogEntry> getRecentLogs() {
        return taskLogService.getRecentLogs();
    }

    public void clearRuntimeLogs() {
        taskRunHistoryService.clear();
        taskLogService.clear();
        resetRuntimeState();
    }

    public String getRegisteredTaskSummary() {
        return taskRegistryService.getRegisteredTaskSummary();
    }

    private TaskRuntimeState buildState(TaskRunStatus status,
                                        boolean running,
                                        boolean stopping,
                                        TaskRunRequest request,
                                        TaskRunSummary summary,
                                        LocalDateTime startedAt,
                                        LocalDateTime finishedAt,
                                        String statusText) {
        return TaskRuntimeState.builder()
                .status(status)
                .running(running)
                .stopping(stopping)
                .currentRequest(request)
                .lastSummary(summary)
                .startedAt(startedAt)
                .finishedAt(finishedAt)
                .statusText(statusText)
                .build();
    }
}
