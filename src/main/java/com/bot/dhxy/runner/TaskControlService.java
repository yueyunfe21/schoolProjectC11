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
     * 是否已经发送停止请求。
     */
    private final AtomicBoolean stopping = new AtomicBoolean(false);

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
                    .status(TaskRunStatus.REJECTED)
                    .running(false)
                    .stopping(false)
                    .statusText("启动被拒绝：没有有效任务")
                    .finishedAt(LocalDateTime.now())
                    .build();
            return new TaskRunSummary();
        }

        if (!running.compareAndSet(false, true)) {
            log.warn("⚠️ 当前已有任务队列正在运行，忽略重复启动请求。");
            taskLogService.warn(null, null, "当前已有任务队列正在运行，忽略重复启动请求");
            runtimeState = TaskRuntimeState.builder()
                    .status(TaskRunStatus.REJECTED)
                    .running(true)
                    .stopping(stopping.get())
                    .currentRequest(runtimeState.getCurrentRequest())
                    .lastSummary(runtimeState.getLastSummary())
                    .startedAt(runtimeState.getStartedAt())
                    .finishedAt(runtimeState.getFinishedAt())
                    .statusText("启动被拒绝：当前已有任务正在运行")
                    .build();
            return new TaskRunSummary();
        }

        stopping.set(false);
        LocalDateTime startedAt = LocalDateTime.now();
        runtimeState = TaskRuntimeState.builder()
                .status(TaskRunStatus.STARTING)
                .running(true)
                .stopping(false)
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
                        .status(TaskRunStatus.INITIALIZING_WINDOW)
                        .running(true)
                        .stopping(false)
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
                            .status(TaskRunStatus.FAILED)
                            .running(false)
                            .stopping(false)
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
                    .status(TaskRunStatus.RUNNING)
                    .running(true)
                    .stopping(false)
                    .currentRequest(request)
                    .startedAt(startedAt)
                    .statusText("运行中：任务队列执行中")
                    .build();

            TaskQueue queue = new TaskQueue(request.getNormalizedTaskCodes(), request.isLoop());
            summary = taskRunner.run(queue, request.isTestMode());
            boolean stopped = stopping.get();
            runtimeState = TaskRuntimeState.builder()
                    .status(stopped ? TaskRunStatus.STOPPING : TaskRunStatus.COMPLETED)
                    .running(false)
                    .stopping(false)
                    .currentRequest(request)
                    .lastSummary(summary)
                    .startedAt(startedAt)
                    .finishedAt(LocalDateTime.now())
                    .statusText(stopped ? "空闲：任务队列已停止" : "空闲：任务队列执行完毕")
                    .build();
            return summary;
        } catch (Exception e) {
            log.error("💥 任务启动或执行流程发生异常。", e);
            taskLogService.fail(null, null, "任务启动或执行流程发生异常: " + e.getClass().getSimpleName() + ": " + e.getMessage());
            runtimeState = TaskRuntimeState.builder()
                    .status(TaskRunStatus.FAILED)
                    .running(false)
                    .stopping(false)
                    .currentRequest(request)
                    .lastSummary(summary)
                    .startedAt(startedAt)
                    .finishedAt(LocalDateTime.now())
                    .statusText("异常结束：" + e.getClass().getSimpleName())
                    .build();
            return summary;
        } finally {
            stopping.set(false);
            running.set(false);
        }
    }

    /**
     * 请求停止当前任务队列。
     *
     * 注意：这里只发送停止请求，不立刻把 running 改成 false。
     * running 会在任务线程真正退出 startTasks() 时释放，避免旧任务未停完又启动新任务。
     */
    public void stop() {
        if (!running.get()) {
            runtimeState = TaskRuntimeState.builder()
                    .status(TaskRunStatus.IDLE)
                    .running(false)
                    .stopping(false)
                    .currentRequest(runtimeState.getCurrentRequest())
                    .lastSummary(runtimeState.getLastSummary())
                    .startedAt(runtimeState.getStartedAt())
                    .finishedAt(runtimeState.getFinishedAt())
                    .statusText("空闲：当前没有正在运行的任务")
                    .build();
            return;
        }

        stopping.set(true);
        taskRunner.stop();
        runtimeState = TaskRuntimeState.builder()
                .status(TaskRunStatus.STOPPING)
                .running(true)
                .stopping(true)
                .currentRequest(runtimeState.getCurrentRequest())
                .lastSummary(runtimeState.getLastSummary())
                .startedAt(runtimeState.getStartedAt())
                .statusText("停止中：已发送停止请求，等待任务退出")
                .build();
        taskLogService.warn(null, null, "已发送停止请求，等待任务退出");
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
        if (running.get()) {
            return;
        }
        runtimeState = TaskRuntimeState.idle();
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
}
