package com.bot.dhxy.window.control;

import com.bot.dhxy.cloud.turn.TurnModeGuard;
import com.bot.dhxy.cloud.turn.CloudTurnSidecarLauncher;
import com.bot.dhxy.cloud.turn.WindowTurnLoop;
import com.bot.dhxy.cloud.turn.local.LocalTeamRolePreflightService;
import com.bot.dhxy.cloud.turn.protocol.TurnTaskCode;
import com.bot.dhxy.cloud.turn.protocol.TurnTaskQueueFailurePolicy;
import com.bot.dhxy.cloud.turn.protocol.TurnTaskStartAck;
import com.bot.dhxy.cloud.turn.protocol.TurnTaskStartRequest;
import com.bot.dhxy.cloud.turn.protocol.TurnTaskTerminalResult;
import com.bot.dhxy.cloud.turn.protocol.TurnWindowMetadata;
import com.bot.dhxy.cloud.turn.protocol.TurnWindowRect;
import com.bot.dhxy.cloud.turn.protocol.TurnMapSurveyCommand;
import com.bot.dhxy.cloud.turn.protocol.TurnMapSurveyResult;
import com.bot.dhxy.config.BotProperties;
import com.bot.dhxy.runner.context.TaskStartupMode;
import com.bot.dhxy.task.model.TaskType;
import com.bot.dhxy.window.execution.MultiWindowTaskManager;
import com.bot.dhxy.window.execution.RemoteTaskHandle;
import com.bot.dhxy.window.execution.WindowTaskFailurePolicy;
import com.bot.dhxy.window.execution.WindowTaskQueue;
import com.bot.dhxy.window.execution.WindowTaskRunner;
import com.bot.dhxy.window.execution.WindowTaskSnapshot;
import com.bot.dhxy.window.model.WindowNativeBinding;
import com.bot.dhxy.window.model.WindowRuntimeStatus;
import com.bot.dhxy.window.runtime.WindowNativeBindingRefreshService;
import com.bot.dhxy.window.runtime.WindowRegistrationRequest;
import com.bot.dhxy.window.runtime.WindowRuntimeContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

/** Production window control whose only executable task path is the HTTPS turn loop. */
@Service
@Slf4j
public class WindowTaskControlService {

    private static final Duration REMOTE_START_ACK_TIMEOUT = Duration.ofSeconds(10L);
    private final MultiWindowTaskManager taskManager;
    private final TurnModeGuard turnModeGuard;
    private final CloudTurnSidecarLauncher sidecarLauncher;
    private final WindowNativeBindingRefreshService bindingRefreshService;
    private final BotProperties botProperties;
    private final LocalTeamRolePreflightService localTeamRolePreflightService;
    private final AtomicBoolean remoteStartInFlight = new AtomicBoolean(false);
    private final AtomicLong remoteStartEpoch = new AtomicLong(0L);
    private final Object remoteStartLifecycleMonitor = new Object();

    @Autowired
    public WindowTaskControlService(MultiWindowTaskManager taskManager,
                                    TurnModeGuard turnModeGuard,
                                    CloudTurnSidecarLauncher sidecarLauncher,
                                    WindowNativeBindingRefreshService bindingRefreshService,
                                    BotProperties botProperties,
                                    LocalTeamRolePreflightService localTeamRolePreflightService) {
        this.taskManager = Objects.requireNonNull(taskManager, "taskManager");
        this.turnModeGuard = Objects.requireNonNull(turnModeGuard, "turnModeGuard");
        this.sidecarLauncher = Objects.requireNonNull(sidecarLauncher, "sidecarLauncher");
        this.bindingRefreshService = Objects.requireNonNull(bindingRefreshService, "bindingRefreshService");
        this.botProperties = Objects.requireNonNull(botProperties, "botProperties");
        this.localTeamRolePreflightService = Objects.requireNonNull(
                localTeamRolePreflightService, "localTeamRolePreflightService");
    }

    public WindowSystemSnapshot getSystemSnapshot() {
        return new WindowSystemSnapshot(
                taskManager.getRegisteredWindowCount(),
                taskManager.getRunningWindowCount(),
                taskManager.getMaxWindowCount(),
                taskManager.getRemainingWindowCapacity(),
                getSnapshots());
    }

    public List<WindowTaskSnapshot> getSnapshots() {
        synchronizeRemoteRuntimeStates();
        return taskManager.getAllSnapshots();
    }

    /** Submit one task-free MapSurvey command through the existing exact-window turn loop. */
    public CompletableFuture<TurnMapSurveyResult> submitMapSurvey(
            String windowId,
            TurnMapSurveyCommand.Operation operation,
            String mapName) {
        String exactWindowId = Objects.requireNonNull(windowId, "windowId").trim();
        if (exactWindowId.isEmpty()) {
            throw new IllegalArgumentException("windowId must be nonblank");
        }
        TurnMapSurveyCommand command = new TurnMapSurveyCommand(
                UUID.randomUUID().toString(), Objects.requireNonNull(operation, "operation"),
                mapName == null ? null : mapName.trim());
        if (!turnModeGuard.remoteState(exactWindowId).registered()) {
            WindowTaskRunner runner = taskManager.getRunner(exactWindowId).orElseThrow(() ->
                    new IllegalArgumentException("窗口不存在: " + exactWindowId));
            Supplier<TurnWindowMetadata> metadataSupplier = new RemoteTurnMetadataSupplier(
                    turnModeGuard.deviceId(), runner.getWindowContext(), bindingRefreshService);
            turnModeGuard.startRemote(turnModeGuard.deviceId(), exactWindowId, metadataSupplier);
        }
        return turnModeGuard.submitMapSurvey(exactWindowId, command);
    }

    public WindowTaskCommandResult registerWindows(Collection<WindowRegistrationRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            return WindowTaskCommandResult.empty("没有需要注册的窗口", getSnapshots());
        }
        int successCount = 0;
        List<WindowTaskCommandDetail> details = new ArrayList<>();
        for (WindowRegistrationRequest request : requests) {
            if (request == null || !request.hasWindowId()) {
                details.add(WindowTaskCommandDetail.failed(null, "窗口注册请求无效"));
                continue;
            }
            if (taskManager.registerWindow(request) != null) {
                successCount++;
                details.add(WindowTaskCommandDetail.success(request.getWindowId(), "窗口已注册或已刷新"));
            } else {
                details.add(WindowTaskCommandDetail.failed(request.getWindowId(), "窗口注册失败，可能已达到容量上限"));
            }
        }
        return buildResult(requests.size(), successCount, "独立窗口注册完成", details);
    }

    /** Starts the requested queue exclusively through the configured HTTPS turn transport. */
    public WindowTaskCommandResult start(WindowTaskStartRequest request) {
        if (request == null || !request.hasWindows()) {
            return WindowTaskCommandResult.empty("没有选中的窗口", getSnapshots());
        }
        return switch (request.getStartMode()) {
            case SAME_TASK -> startRemoteSameTask(
                    turnModeGuard.deviceId(), request.getWindowIds(), request.getTaskQueue());
            case SELECTED_TASK -> startRemoteSelectedTask(turnModeGuard.deviceId(), request.getWindowIds());
            case DETECTED_ROLE -> remoteDetectedRoleRejected(request.getWindowIds());
        };
    }

    public WindowTaskCommandResult startIndependentWindows(Collection<String> windowIds, TaskType taskType) {
        return startSameTask(windowIds, taskType);
    }

    public WindowTaskCommandResult startSameTask(Collection<String> windowIds, TaskType taskType) {
        return startSameQueue(windowIds, WindowTaskQueue.single(taskType));
    }

    public WindowTaskCommandResult startSameTask(Collection<String> windowIds,
                                                 TaskType taskType,
                                                 WindowTaskFailurePolicy failurePolicy) {
        return startSameQueue(windowIds, WindowTaskQueue.single(taskType).withFailurePolicy(failurePolicy));
    }

    public WindowTaskCommandResult startSameQueue(Collection<String> windowIds, WindowTaskQueue queue) {
        return startRemoteSameTask(turnModeGuard.deviceId(), windowIds, queue);
    }

    public WindowTaskCommandResult startSelectedTasks(Collection<String> windowIds) {
        return startRemoteSelectedTask(turnModeGuard.deviceId(), windowIds);
    }

    @Deprecated
    public WindowTaskCommandResult startByDetectedRole(Collection<String> windowIds, TaskType ignoredLeaderTaskType) {
        return remoteDetectedRoleRejected(windowIds);
    }

    private WindowTaskCommandResult remoteDetectedRoleRejected(Collection<String> windowIds) {
        List<String> ids = normalizeWindowIds(windowIds);
        List<WindowTaskCommandDetail> details = ids.stream()
                .map(id -> WindowTaskCommandDetail.failed(
                        id, "远程启动已拒绝：按身份分配是已废弃测试入口"))
                .toList();
        return buildResult(ids.size(), 0, "远程按身份测试入口已禁用", details);
    }

    public WindowTaskCommandResult stopWindows(Collection<String> windowIds) {
        return stopRemoteWindows(windowIds);
    }

    public WindowTaskCommandResult stopAll() {
        return stopRemoteWindows(allWindowIds());
    }

    public WindowTaskCommandResult pauseWindows(Collection<String> windowIds) {
        return pauseRemoteWindows(windowIds);
    }

    public WindowTaskCommandResult resumeWindows(Collection<String> windowIds) {
        return resumeRemoteWindows(windowIds);
    }

    public WindowTaskCommandResult pauseAll() {
        return pauseRemoteWindows(allWindowIds());
    }

    public WindowTaskCommandResult resumeAll() {
        return resumeRemoteWindows(allWindowIds());
    }

    public WindowTaskCommandResult togglePauseResumeAll() {
        List<WindowTaskSnapshot> live = getSnapshots().stream().filter(WindowTaskSnapshot::isRunning).toList();
        if (live.isEmpty()) {
            return WindowTaskCommandResult.empty("当前没有运行中的窗口任务", getSnapshots());
        }
        boolean resume = live.stream().allMatch(snapshot -> snapshot.getStatus() == WindowRuntimeStatus.PAUSED);
        return resume ? resumeAll() : pauseAll();
    }

    public WindowTaskCommandResult unregisterWindows(Collection<String> windowIds) {
        List<String> ids = normalizeWindowIds(windowIds);
        if (ids.isEmpty()) {
            return WindowTaskCommandResult.empty("没有需要移除的窗口", getSnapshots());
        }
        int successCount = 0;
        List<WindowTaskCommandDetail> details = new ArrayList<>();
        for (String windowId : ids) {
            WindowTaskRunner runner = taskManager.getRunner(windowId).orElse(null);
            if (runner == null) {
                details.add(WindowTaskCommandDetail.failed(windowId, "窗口不存在"));
                continue;
            }
            try {
                if (turnModeGuard.remoteState(windowId).registered()) {
                    turnModeGuard.stopRemote(windowId);
                }
                taskManager.unregisterWindow(windowId);
                successCount++;
                details.add(WindowTaskCommandDetail.success(windowId, "远程 turn 已停止，窗口已移除"));
            } catch (RuntimeException failure) {
                details.add(WindowTaskCommandDetail.failed(windowId, "窗口移除失败：" + failure.getMessage()));
            }
        }
        return buildResult(ids.size(), successCount, "窗口移除完成", details);
    }

    public WindowTaskCommandResult unregisterAll() {
        return unregisterWindows(allWindowIds());
    }

    public WindowTaskCommandResult startRemoteSameTask(String deviceId,
                                                       Collection<String> windowIds,
                                                       WindowTaskQueue queue) {
        List<String> ids = normalizeWindowIds(windowIds);
        if (ids.isEmpty()) {
            return WindowTaskCommandResult.empty("没有选中的窗口", getSnapshots());
        }
        WindowTaskQueue safeQueue = queue == null ? WindowTaskQueue.empty() : queue;
        List<TurnTaskCode> taskCodes;
        List<Integer> taskMaxRuns;
        TurnTaskQueueFailurePolicy failurePolicy;
        try {
            taskCodes = toTurnTaskCodes(safeQueue);
            taskMaxRuns = toTaskMaxRuns(taskCodes);
            failurePolicy = toTurnFailurePolicy(safeQueue.getFailurePolicy());
        } catch (IllegalArgumentException unsupported) {
            return rejected(ids, "远程批量启动已拒绝", unsupported.getMessage());
        }
        long startEpoch = beginRemoteStart();
        if (startEpoch < 0L) {
            return remoteStartRejected(ids, "已有启动流程正在进行");
        }
        try {
            return startRemoteBatch(deviceId, ids, taskCodes, taskMaxRuns, failurePolicy, safeQueue,
                    "远程批量启动完成", startEpoch);
        } finally {
            remoteStartInFlight.set(false);
        }
    }

    public WindowTaskCommandResult startRemoteSelectedTask(String deviceId, Collection<String> windowIds) {
        List<String> ids = normalizeWindowIds(windowIds);
        if (ids.isEmpty()) {
            return WindowTaskCommandResult.empty("没有选中的窗口", getSnapshots());
        }
        long startEpoch = beginRemoteStart();
        if (startEpoch < 0L) {
            return remoteStartRejected(ids, "已有启动流程正在进行");
        }
        try {
            CloudTurnSidecarLauncher.Readiness readiness =
                    sidecarLauncher.ensureReady(() -> isRemoteStartCancelled(startEpoch));
            if (!readiness.ready()) {
                return remoteStartUnavailable(ids, readiness.message());
            }
            if (isRemoteStartCancelled(startEpoch)) {
                return remoteStartCancelled(ids);
            }
            String teamSessionKey = "cr212-team-" + UUID.randomUUID();
            Map<String, LocalTeamRolePreflightService.Preflight> preflightByWindow = prepareLocalTeamRoles(
                    ids, teamSessionKey, startEpoch);
            if (isRemoteStartCancelled(startEpoch)) {
                return remoteStartCancelled(ids);
            }
            List<String> startOrder = ids.stream()
                    .sorted((left, right) -> Boolean.compare(
                            preflightByWindow.getOrDefault(right, unknownPreflight(right)).representative(),
                            preflightByWindow.getOrDefault(left, unknownPreflight(left)).representative()))
                    .toList();
            List<WindowTaskCommandDetail> details = new ArrayList<>();
            // Cloud must OCR the sole representative mask first and cache the actual leader ID before member titles
            // can be compared. Only the remaining same-group starts may overlap.
            for (String windowId : startOrder) {
                if (!preflightByWindow.getOrDefault(windowId, unknownPreflight(windowId)).representative()) {
                    continue;
                }
                if (isRemoteStartCancelled(startEpoch)) {
                    details.add(WindowTaskCommandDetail.failed(windowId, "远程启动已被暂停或停止"));
                    continue;
                }
                TaskType selected = taskManager.getSnapshot(windowId)
                        .map(WindowTaskSnapshot::getSelectedTaskType).orElse(TaskType.UNKNOWN);
                WindowTaskQueue queue = WindowTaskQueue.single(selected);
                TurnTaskCode code;
                try {
                    code = toTurnTaskCode(selected);
                } catch (IllegalArgumentException unsupported) {
                    details.add(WindowTaskCommandDetail.failed(
                            windowId, "远程启动已拒绝：" + unsupported.getMessage()));
                    continue;
                }
                details.add(startOneRemote(deviceId, windowId, List.of(code), toTaskMaxRuns(List.of(code)),
                        TurnTaskQueueFailurePolicy.CONTINUE_ON_FAILURE, queue, teamSessionKey,
                        preflightByWindow.getOrDefault(windowId, unknownPreflight(windowId)), startEpoch));
            }
            List<CompletableFuture<WindowTaskCommandDetail>> startFutures = new ArrayList<>();
            for (String windowId : startOrder) {
                if (preflightByWindow.getOrDefault(windowId, unknownPreflight(windowId)).representative()) {
                    continue;
                }
                if (isRemoteStartCancelled(startEpoch)) {
                    startFutures.add(CompletableFuture.completedFuture(
                            WindowTaskCommandDetail.failed(windowId, "远程启动已被暂停或停止")));
                    continue;
                }
                TaskType selected = taskManager.getSnapshot(windowId)
                        .map(WindowTaskSnapshot::getSelectedTaskType).orElse(TaskType.UNKNOWN);
                WindowTaskQueue queue = WindowTaskQueue.single(selected);
                TurnTaskCode code;
                try {
                    code = toTurnTaskCode(selected);
                } catch (IllegalArgumentException unsupported) {
                    startFutures.add(CompletableFuture.completedFuture(WindowTaskCommandDetail.failed(
                            windowId, "远程启动已拒绝：" + unsupported.getMessage())));
                    continue;
                }
                startFutures.add(CompletableFuture.supplyAsync(() -> startOneRemote(
                        deviceId, windowId, List.of(code), toTaskMaxRuns(List.of(code)),
                        TurnTaskQueueFailurePolicy.CONTINUE_ON_FAILURE, queue, teamSessionKey,
                        preflightByWindow.getOrDefault(windowId, unknownPreflight(windowId)), startEpoch)));
            }
            details.addAll(startFutures.stream().map(CompletableFuture::join).toList());
            int successCount = (int) details.stream().filter(WindowTaskCommandDetail::isSuccess).count();
            return buildResult(ids.size(), successCount, "远程选中任务启动完成", details);
        } finally {
            remoteStartInFlight.set(false);
        }
    }

    private WindowTaskCommandResult startRemoteBatch(String deviceId,
                                                     List<String> windowIds,
                                                     List<TurnTaskCode> taskCodes,
                                                     List<Integer> taskMaxRuns,
                                                     TurnTaskQueueFailurePolicy failurePolicy,
                                                     WindowTaskQueue queue,
                                                     String summary,
                                                     long startEpoch) {
        CloudTurnSidecarLauncher.Readiness readiness =
                sidecarLauncher.ensureReady(() -> isRemoteStartCancelled(startEpoch));
        if (!readiness.ready()) {
            return remoteStartUnavailable(windowIds, readiness.message());
        }
        if (isRemoteStartCancelled(startEpoch)) {
            return remoteStartCancelled(windowIds);
        }
        String teamSessionKey = "cr212-team-" + UUID.randomUUID();
        Map<String, LocalTeamRolePreflightService.Preflight> preflightByWindow = prepareLocalTeamRoles(
                windowIds, teamSessionKey, startEpoch);
        if (isRemoteStartCancelled(startEpoch)) {
            return remoteStartCancelled(windowIds);
        }
        List<String> startOrder = windowIds.stream()
                .sorted((left, right) -> Boolean.compare(
                        preflightByWindow.getOrDefault(right, unknownPreflight(right)).representative(),
                        preflightByWindow.getOrDefault(left, unknownPreflight(left)).representative()))
                .toList();
        List<WindowTaskCommandDetail> details = new ArrayList<>();
        for (String windowId : startOrder) {
            if (!preflightByWindow.getOrDefault(windowId, unknownPreflight(windowId)).representative()) {
                continue;
            }
            if (isRemoteStartCancelled(startEpoch)) {
                details.add(WindowTaskCommandDetail.failed(windowId, "远程启动已被暂停或停止"));
                continue;
            }
            details.add(startOneRemote(
                    deviceId, windowId, taskCodes, taskMaxRuns, failurePolicy, queue,
                    teamSessionKey, preflightByWindow.getOrDefault(windowId, unknownPreflight(windowId)),
                    startEpoch));
        }
        List<CompletableFuture<WindowTaskCommandDetail>> startFutures = new ArrayList<>();
        for (String windowId : startOrder) {
            if (preflightByWindow.getOrDefault(windowId, unknownPreflight(windowId)).representative()) {
                continue;
            }
            if (isRemoteStartCancelled(startEpoch)) {
                startFutures.add(CompletableFuture.completedFuture(
                        WindowTaskCommandDetail.failed(windowId, "远程启动已被暂停或停止")));
                continue;
            }
            startFutures.add(CompletableFuture.supplyAsync(() -> startOneRemote(
                    deviceId, windowId, taskCodes, taskMaxRuns, failurePolicy, queue,
                    teamSessionKey, preflightByWindow.getOrDefault(windowId, unknownPreflight(windowId)),
                    startEpoch)));
        }
        details.addAll(startFutures.stream().map(CompletableFuture::join).toList());
        int successCount = (int) details.stream().filter(WindowTaskCommandDetail::isSuccess).count();
        return buildResult(windowIds.size(), successCount, summary, details);
    }

    private WindowTaskCommandDetail startOneRemote(String deviceId,
                                                   String windowId,
                                                   List<TurnTaskCode> taskCodes,
                                                   List<Integer> taskMaxRuns,
                                                   TurnTaskQueueFailurePolicy failurePolicy,
                                                   WindowTaskQueue queue,
                                                   String teamSessionKey,
                                                   LocalTeamRolePreflightService.Preflight teamPreflight,
                                                   long startEpoch) {
        if (isRemoteStartCancelled(startEpoch)) {
            return WindowTaskCommandDetail.failed(windowId, "远程启动已被暂停或停止");
        }
        WindowTaskRunner runner = taskManager.getRunner(windowId).orElse(null);
        if (runner == null) {
            return WindowTaskCommandDetail.failed(windowId, "窗口不存在");
        }
        TurnModeGuard.RemoteLoopState previous = turnModeGuard.remoteState(windowId);
        if (previous.registered()) {
            try {
                turnModeGuard.requestRemoteStop(windowId);
                if (!turnModeGuard.awaitAndRemoveStoppedRemote(windowId)) {
                    return WindowTaskCommandDetail.failed(windowId, "旧任务未能确认停止，拒绝覆盖启动");
                }
                runner.markRemoteStopped("replaced by a new remote task start");
            } catch (RuntimeException failure) {
                return WindowTaskCommandDetail.failed(
                        windowId, "清理旧任务失败，拒绝启动新任务：" + failure.getMessage());
            }
        }
        WindowRuntimeContext context = runner.getWindowContext();
        Supplier<TurnWindowMetadata> metadataSupplier =
                new RemoteTurnMetadataSupplier(deviceId, context, bindingRefreshService,
                        teamSessionKey, teamPreflight);
        TurnTaskStartRequest startRequest = new TurnTaskStartRequest(
                "remote-turn-" + UUID.randomUUID(), taskCodes, taskMaxRuns, failurePolicy);
        try {
            WindowTurnLoop loop = turnModeGuard.startRemote(deviceId, windowId, metadataSupplier, startRequest);
            if (isRemoteStartCancelled(startEpoch)) {
                turnModeGuard.stopRemote(windowId);
                return WindowTaskCommandDetail.failed(windowId, "远程启动已被暂停或停止");
            }
            if (!loop.awaitStartAcknowledged(REMOTE_START_ACK_TIMEOUT)) {
                Throwable failure = loop.lastFailure();
                if (loop.isRunning()) {
                    turnModeGuard.stopRemote(windowId);
                }
                String reason = failure == null ? "Cloud在10秒内未确认任务启动" : failure.getMessage();
                return WindowTaskCommandDetail.failed(windowId, "远程启动未确认：" + reason);
            }
            synchronized (remoteStartLifecycleMonitor) {
                if (isRemoteStartCancelled(startEpoch)) {
                    turnModeGuard.stopRemote(windowId);
                    return WindowTaskCommandDetail.failed(windowId, "远程启动已被暂停或停止");
                }
                WindowTaskQueue effectiveQueue = projectEffectiveQueue(queue,
                        loop.acceptedStartAck().orElseThrow(
                                () -> new IllegalStateException("Cloud确认启动但未保留start ACK")));
                runner.markRemoteStarted(effectiveQueue);
            }
            RemoteTaskHandle startedHandle = runner.getRemoteTaskHandle();
            loop.taskTerminalResult().thenAccept(
                    terminal -> projectRemoteTerminal(runner, startedHandle, terminal));
            return WindowTaskCommandDetail.success(windowId, "Cloud已确认远程任务启动");
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            return WindowTaskCommandDetail.failed(windowId, "等待Cloud确认启动时被中断");
        } catch (RuntimeException failure) {
            return WindowTaskCommandDetail.failed(windowId, "远程启动失败：" + failure.getMessage());
        }
    }

    private void projectRemoteTerminal(
            WindowTaskRunner runner,
            RemoteTaskHandle expectedHandle,
            TurnTaskTerminalResult terminal) {
        if (runner.getRemoteTaskHandle() != expectedHandle) {
            log.info("Ignore stale remote terminal after task replacement: windowId={} startRequestId={} status={}",
                    runner.getWindowContext().getWindowId(), terminal.startRequestId(), terminal.status());
            return;
        }
        if (terminal.status() == TurnTaskTerminalResult.Status.FAILED
                || terminal.status() == TurnTaskTerminalResult.Status.SKIPPED) {
            String reason = terminal.reason() == null || terminal.reason().isBlank()
                    ? ""
                    : "，原因：" + terminal.reason();
            runner.markRemoteFailed(new IllegalStateException(
                    "Cloud任务终止：" + terminal.status() + " (" + terminal.startRequestId() + ")" + reason));
            return;
        }
        runner.markRemoteStopped("Cloud任务终止：" + terminal.status()
                + " (" + terminal.startRequestId() + ")");
    }

    public WindowTaskCommandResult pauseRemoteWindows(Collection<String> windowIds) {
        synchronized (remoteStartLifecycleMonitor) {
            cancelPendingRemoteStarts("pause");
            return applyRemoteLifecycle(windowIds, turnModeGuard::pauseRemote, WindowTaskRunner::markRemotePaused,
                    "远程暂停选中窗口完成", "已请求远程暂停");
        }
    }

    public WindowTaskCommandResult resumeRemoteWindows(Collection<String> windowIds) {
        return applyRemoteLifecycle(windowIds, turnModeGuard::resumeRemote,
                runner -> runner.getWindowContext().markRuntimeWarning("正在校验原任务窗口后恢复"),
                "远程恢复选中窗口完成", "已请求校验恢复");
    }

    public WindowTaskCommandResult stopRemoteWindows(Collection<String> windowIds) {
        synchronized (remoteStartLifecycleMonitor) {
            cancelPendingRemoteStarts("stop");
            List<String> ids = normalizeWindowIds(windowIds);
            if (ids.isEmpty()) {
                return WindowTaskCommandResult.empty("没有选中的窗口", getSnapshots());
            }

            // Broadcast before waiting: a slow Cloud task in one window must never delay the stop signal for its peers.
            List<WindowTaskCommandDetail> details = new ArrayList<>();
            List<String> requested = new ArrayList<>();
            for (String windowId : ids) {
                try {
                    if (turnModeGuard.requestRemoteStop(windowId)) {
                        requested.add(windowId);
                    } else {
                        details.add(WindowTaskCommandDetail.failed(windowId, "当前没有远程 turn loop"));
                    }
                } catch (RuntimeException failure) {
                    details.add(WindowTaskCommandDetail.failed(windowId, "远程停止请求失败：" + failure.getMessage()));
                }
            }
            for (String windowId : requested) {
                try {
                    if (!turnModeGuard.awaitAndRemoveStoppedRemote(windowId)) {
                        details.add(WindowTaskCommandDetail.failed(windowId, "远程 turn loop 在停止前已不存在"));
                        continue;
                    }
                    taskManager.getRunner(windowId).ifPresent(
                            runner -> runner.markRemoteStopped("remote turn stopped"));
                    details.add(WindowTaskCommandDetail.success(windowId, "已停止并移除远程 turn loop"));
                } catch (RuntimeException failure) {
                    details.add(WindowTaskCommandDetail.failed(windowId, "远程停止未确认：" + failure.getMessage()));
                }
            }
            int successCount = (int) details.stream().filter(WindowTaskCommandDetail::isSuccess).count();
            return buildResult(ids.size(), successCount, "远程停止选中窗口完成", details);
        }
    }

    private WindowTaskCommandResult applyRemoteLifecycle(Collection<String> windowIds,
                                                         Predicate<String> action,
                                                         Consumer<WindowTaskRunner> projection,
                                                         String summary,
                                                         String successMessage) {
        List<String> ids = normalizeWindowIds(windowIds);
        if (ids.isEmpty()) {
            return WindowTaskCommandResult.empty("没有选中的窗口", getSnapshots());
        }
        int successCount = 0;
        List<WindowTaskCommandDetail> details = new ArrayList<>();
        for (String windowId : ids) {
            try {
                if (!action.test(windowId)) {
                    details.add(WindowTaskCommandDetail.failed(windowId, "当前没有远程 turn loop"));
                    continue;
                }
                successCount++;
                taskManager.getRunner(windowId).ifPresent(projection);
                details.add(WindowTaskCommandDetail.success(windowId, successMessage));
            } catch (RuntimeException failure) {
                details.add(WindowTaskCommandDetail.failed(windowId, "远程控制失败：" + failure.getMessage()));
            }
        }
        return buildResult(ids.size(), successCount, summary, details);
    }

    static List<TurnTaskCode> toTurnTaskCodes(WindowTaskQueue queue) {
        if (queue == null || queue.isEmpty()) {
            throw new IllegalArgumentException("队列没有可用于远程 turn 协议的任务");
        }
        return queue.getTaskTypes().stream().map(WindowTaskControlService::toTurnTaskCode).toList();
    }

    static TurnTaskCode toTurnTaskCode(TaskType type) {
        return switch (type) {
            case WUHuan_V2 -> TurnTaskCode.WUHUAN_V2;
            case WUBEI -> TurnTaskCode.WUBEI;
            case XIULUO_V2 -> TurnTaskCode.XIULUO_V2;
            case AUTO_BATTLE -> TurnTaskCode.AUTO_BATTLE;
            case SLEEP_COMPUTER -> TurnTaskCode.SLEEP_COMPUTER;
            default -> throw new IllegalArgumentException("任务不支持远程 turn 协议：" + type);
        };
    }

    static WindowTaskQueue projectEffectiveQueue(WindowTaskQueue requested, TurnTaskStartAck ack) {
        List<TurnTaskCode> effectiveCodes = ack.effectiveTaskCodes();
        if (effectiveCodes == null) {
            return requested;
        }
        List<TaskType> effectiveTypes = effectiveCodes.stream()
                .map(WindowTaskControlService::fromTurnTaskCode)
                .toList();
        return new WindowTaskQueue(effectiveTypes, requested.getFailurePolicy());
    }

    static TaskType fromTurnTaskCode(TurnTaskCode code) {
        return switch (code) {
            case WUHUAN_V2 -> TaskType.WUHuan_V2;
            case WUBEI -> TaskType.WUBEI;
            case XIULUO_V2 -> TaskType.XIULUO_V2;
            case AUTO_BATTLE -> TaskType.AUTO_BATTLE;
            case SLEEP_COMPUTER -> TaskType.SLEEP_COMPUTER;
        };
    }

    private List<Integer> toTaskMaxRuns(List<TurnTaskCode> taskCodes) {
        return taskCodes.stream().map(code -> switch (code) {
            case WUBEI -> botProperties.getFivefoldMaxRuns();
            case XIULUO_V2 -> botProperties.getXiuluoMaxRuns();
            case WUHUAN_V2 -> botProperties.getWuhuanMaxRuns();
            case AUTO_BATTLE -> 1;
            case SLEEP_COMPUTER -> 1;
        }).toList();
    }

    static TurnTaskQueueFailurePolicy toTurnFailurePolicy(WindowTaskFailurePolicy policy) {
        return policy == WindowTaskFailurePolicy.STOP_ON_FAILURE
                ? TurnTaskQueueFailurePolicy.STOP_ON_FAILURE
                : TurnTaskQueueFailurePolicy.CONTINUE_ON_FAILURE;
    }

    private void synchronizeRemoteRuntimeStates() {
        for (WindowTaskRunner runner : taskManager.getAllRunners()) {
            TurnModeGuard.RemoteLoopState state = turnModeGuard.remoteState(runner.getWindowContext().getWindowId());
            if (!state.registered()) {
                continue;
            }
            if (state.lastFailure() != null && runner.isRemoteRunning()) {
                runner.markRemoteFailed(state.lastFailure());
            } else if (!state.running() && state.terminalAcknowledged() && runner.isRemoteRunning()) {
                runner.markRemoteStopped("remote turn loop ended");
            } else if (!state.running() && runner.isRemoteRunning()) {
                runner.getWindowContext().markRuntimeWarning(
                        "远程 turn 已停止但云端尚未确认终态；保留窗口所有权，禁止新建启动请求");
            } else if (state.paused() && !runner.isRemotePaused()) {
                runner.markRemotePaused();
            } else if (!state.paused() && runner.isRemotePaused()) {
                runner.markRemoteResumed();
            }
        }
    }

    private List<String> allWindowIds() {
        return taskManager.getAllRunners().stream()
                .map(runner -> runner.getWindowContext().getWindowId()).distinct().toList();
    }

    private WindowTaskCommandResult rejected(List<String> ids, String summary, String reason) {
        List<WindowTaskCommandDetail> details = ids.stream()
                .map(id -> WindowTaskCommandDetail.failed(id, "远程启动已拒绝：" + reason)).toList();
        return buildResult(ids.size(), 0, summary, details);
    }

    private WindowTaskCommandResult buildResult(int requested,
                                                int success,
                                                String summary,
                                                List<WindowTaskCommandDetail> details) {
        return WindowTaskCommandResult.of(
                requested, success, summary + "：" + success + "/" + requested,
                getSnapshots(), Collections.emptyList(), details);
    }

    private List<String> normalizeWindowIds(Collection<String> windowIds) {
        if (windowIds == null || windowIds.isEmpty()) {
            return List.of();
        }
        return windowIds.stream().filter(Objects::nonNull).map(String::trim)
                .filter(id -> !id.isEmpty()).distinct().toList();
    }

    private Map<String, LocalTeamRolePreflightService.Preflight> prepareLocalTeamRoles(
            List<String> windowIds, String teamSessionKey, long startEpoch) {
        List<WindowRuntimeContext> contexts = new ArrayList<>();
        for (String windowId : windowIds) {
            taskManager.getRunner(windowId).ifPresent(runner -> contexts.add(runner.getWindowContext()));
        }
        try {
            return localTeamRolePreflightService.prepareBatch(
                    contexts, teamSessionKey, () -> isRemoteStartCancelled(startEpoch));
        } catch (RuntimeException failure) {
            log.warn("CR212 local team-role preflight failed; Cloud receives explicit UNKNOWN without re-capture: session={} reason={}",
                    teamSessionKey, failure.toString());
            return Map.of();
        }
    }

    private long beginRemoteStart() {
        if (!remoteStartInFlight.compareAndSet(false, true)) {
            return -1L;
        }
        return remoteStartEpoch.incrementAndGet();
    }

    private boolean isRemoteStartCancelled(long startEpoch) {
        return startEpoch <= 0L
                || remoteStartEpoch.get() != startEpoch
                || Thread.currentThread().isInterrupted();
    }

    private void cancelPendingRemoteStarts(String reason) {
        long cancelledEpoch = remoteStartEpoch.incrementAndGet();
        log.warn("Remote start lifecycle invalidated: reason={} epoch={}", reason, cancelledEpoch);
    }

    private WindowTaskCommandResult remoteStartCancelled(List<String> windowIds) {
        return remoteStartRejected(windowIds, "远程启动已被暂停或停止");
    }

    private WindowTaskCommandResult remoteStartUnavailable(List<String> windowIds, String message) {
        List<WindowTaskCommandDetail> unavailable = windowIds.stream()
                .map(windowId -> WindowTaskCommandDetail.failed(windowId, message))
                .toList();
        return buildResult(windowIds.size(), 0, "Cloud Brain 未就绪", unavailable);
    }

    private WindowTaskCommandResult remoteStartRejected(List<String> windowIds, String message) {
        List<WindowTaskCommandDetail> rejected = windowIds.stream()
                .map(windowId -> WindowTaskCommandDetail.failed(windowId, message))
                .toList();
        return buildResult(windowIds.size(), 0, "远程启动未执行", rejected);
    }

    private static LocalTeamRolePreflightService.Preflight unknownPreflight(String windowId) {
        return new LocalTeamRolePreflightService.Preflight(
                windowId, LocalTeamRolePreflightService.Role.UNKNOWN, null, false, null);
    }

    static final class RemoteTurnMetadataSupplier implements Supplier<TurnWindowMetadata> {
        private final String deviceId;
        private final WindowRuntimeContext context;
        private final WindowNativeBindingRefreshService bindingRefreshService;
        private final String teamSessionKey;
        private final LocalTeamRolePreflightService.Preflight teamPreflight;
        private final boolean localTeamBatch;
        private final AtomicBoolean teamPreflightUnsent = new AtomicBoolean(true);

        RemoteTurnMetadataSupplier(String deviceId,
                                   WindowRuntimeContext context,
                                   WindowNativeBindingRefreshService bindingRefreshService) {
            this(deviceId, context, bindingRefreshService, "standalone", unknownPreflight(context.getWindowId()));
        }

        RemoteTurnMetadataSupplier(String deviceId,
                                   WindowRuntimeContext context,
                                   WindowNativeBindingRefreshService bindingRefreshService,
                                   String teamSessionKey,
                                   LocalTeamRolePreflightService.Preflight teamPreflight) {
            this.deviceId = Objects.requireNonNull(deviceId, "deviceId");
            this.context = Objects.requireNonNull(context, "context");
            this.bindingRefreshService = Objects.requireNonNull(bindingRefreshService, "bindingRefreshService");
            this.teamSessionKey = Objects.requireNonNull(teamSessionKey, "teamSessionKey");
            this.teamPreflight = Objects.requireNonNull(teamPreflight, "teamPreflight");
            this.localTeamBatch = !"standalone".equals(teamSessionKey);
        }

        @Override
        public TurnWindowMetadata get() {
            WindowNativeBinding binding = bindingRefreshService.refreshAndCommit(context)
                    .orElseThrow(() -> new IllegalStateException(
                            "remote turn window has no live native binding: " + context.getWindowId()));
            if (!binding.hasNativeHandle() || !binding.hasGeometry()) {
                throw new IllegalStateException(
                        "remote turn window native binding incomplete: " + context.getWindowId());
            }
            boolean publishPreflight = teamPreflightUnsent.compareAndSet(true, false);
            return new TurnWindowMetadata(
                    deviceId,
                    context.getWindowId(),
                    binding.getTitle(),
                    binding.getNativeHandle(),
                    binding.getProcessId(),
                    new TurnWindowRect(binding.getX(), binding.getY(), binding.getWidth(), binding.getHeight()),
                    false,
                    false,
                    null,
                    publishPreflight ? teamPreflight.role().name() : context.getRole().name(),
                    localTeamBatch ? teamSessionKey : null,
                    null,
                    localTeamBatch,
                    false,
                    TaskStartupMode.NORMAL.name(),
                    publishPreflight ? true : null,
                    publishPreflight ? teamSessionKey : null,
                    publishPreflight ? teamPreflight.groupHash() : null,
                    publishPreflight ? teamPreflight.maskBase64() : null,
                    publishPreflight ? teamPreflight.representative() : null);
        }
    }
}
