package com.bot.dhxy.runner;

import com.bot.dhxy.config.TaskRunProperties;
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
    private final GameWindowService gameWindowService;

    /**
     * 防止重复启动任务队列。
     */
    private final AtomicBoolean running = new AtomicBoolean(false);

    /**
     * 当前运行状态快照。
     */
    private volatile TaskRuntimeState runtimeState = TaskRuntimeState.idle();

    /**
     * 按 application.properties 里的 bot.run 配置启动任务队列。
     */
    public TaskRunSummary startConfiguredTasks() {
        return startTasks(TaskRunRequest.builder()
                .taskCodes(taskRunProperties.getNormalizedTasks())
                .loop(taskRunProperties.isLoop())
                .testMode(taskRunProperties.isTestMode())
                .initGameWindow(taskRunProperties.isInitGameWindow())
                .build());
    }

    /**
     * 按指定任务列表启动任务队列。
     *
     * 后面 UI 勾选任务后，可以直接调用这个方法。
     */
    public TaskRunSummary startTasks(List<String> taskCodes, boolean loop, boolean testMode) {
        return startTasks(TaskRunRequest.builder()
                .taskCodes(taskCodes)
                .loop(loop)
                .testMode(testMode)
                .build());
    }

    /**
     * 按启动请求对象启动任务队列。
     *
     * 以后如果增加任务运行次数、任务间隔、失败策略等参数，只需要扩展 TaskRunRequest。
     */
    public TaskRunSummary startTasks(TaskRunRequest request) {
        if (request == null || request.isEmpty()) {
            log.warn("⚠️ 任务启动请求为空，或者没有有效任务，忽略启动请求。");
            taskLogService.warn(null, null, "任务启动请求为空，或者没有有效任务，忽略启动请求");
            runtimeState = TaskRuntimeState.builder()
                    .running(false)
                    .statusText("启动失败：没有有效任务")
                    .finishedAt(LocalDateTime.now())
                    .build();
            return new TaskRunSummary();
        }

        if (!running.compareAndSet(false, true)) {
            log.warn("⚠️ 当前已有任务队列正在运行，忽略重复启动请求。");
            taskLogService.warn(null, null, "当前已有任务队列正在运行，忽略重复启动请求");
            return new TaskRunSummary();
        }

        LocalDateTime startedAt = LocalDateTime.now();
        runtimeState = TaskRuntimeState.builder()
                .running(true)
                .currentRequest(request)
                .startedAt(startedAt)
                .statusText("运行中：准备启动任务")
                .build();

        TaskRunSummary summary = new TaskRunSummary();
        try {
            log.info("🚀 接收到任务启动请求: {}", request.toLogText());
            taskLogService.info(null, null, "接收到任务启动请求: " + request.toLogText());

            if (request.isInitGameWindow()) {
                runtimeState = TaskRuntimeState.builder()
                        .running(true)
                        .currentRequest(request)
                        .startedAt(startedAt)
                        .statusText("运行中：正在初始化游戏窗口")
                        .build();
                taskLogService.info(null, null, "准备初始化游戏窗口");
                boolean ready = gameWindowService.initGameWindow();
                if (!ready) {
                    log.error("❌ 游戏窗口初始化失败，本次任务队列不启动。");
                    taskLogService.fail(null, null, "游戏窗口初始化失败，本次任务队列不启动");
                    runtimeState = TaskRuntimeState.builder()
                            .running(false)
                            .currentRequest(request)
                            .lastSummary(summary)
                            .startedAt(startedAt)
                            .finishedAt(LocalDateTime.now())
                            .statusText("启动失败：游戏窗口初始化失败")
                            .build();
                    return summary;
                }
            } else {
                log.warn("⚠️ 本次任务启动请求跳过游戏窗口初始化，仅适合测试任务队列或 UI。");
                taskLogService.warn(null, null, "本次任务启动请求跳过游戏窗口初始化");
            }

            runtimeState = TaskRuntimeState.builder()
                    .running(true)
                    .currentRequest(request)
                    .startedAt(startedAt)
                    .statusText("运行中：任务队列执行中")
                    .build();

            TaskQueue queue = new TaskQueue(request.getNormalizedTaskCodes(), request.isLoop());
            summary = taskRunner.run(queue, request.isTestMode());
            runtimeState = TaskRuntimeState.builder()
                    .running(false)
                    .currentRequest(request)
                    .lastSummary(summary)
                    .startedAt(startedAt)
                    .finishedAt(LocalDateTime.now())
                    .statusText("空闲：任务队列执行完毕")
                    .build();
            return summary;
        } finally {
            running.set(false);
        }
    }

    /**
     * 请求停止当前任务队列。
     */
    public void stop() {
        taskRunner.stop();
        runtimeState = TaskRuntimeState.builder()
                .running(false)
                .currentRequest(runtimeState.getCurrentRequest())
                .lastSummary(runtimeState.getLastSummary())
                .startedAt(runtimeState.getStartedAt())
                .finishedAt(LocalDateTime.now())
                .statusText("已请求停止")
                .build();
        running.set(false);
    }

    /**
     * 当前是否有任务队列正在运行。
     */
    public boolean isRunning() {
        return running.get();
    }

    /**
     * 获取当前任务运行状态快照。
     */
    public TaskRuntimeState getRuntimeState() {
        return runtimeState;
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
