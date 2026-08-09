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
import com.bot.dhxy.service.BagService;
import com.bot.dhxy.task.model.TaskType;
import com.bot.dhxy.window.execution.MultiWindowTaskManager;
import com.bot.dhxy.window.execution.RemoteTaskHandle;
import com.bot.dhxy.window.execution.WindowTaskFailurePolicy;
import com.bot.dhxy.window.execution.WindowTaskQueue;
import com.bot.dhxy.window.execution.WindowTaskRunner;
import com.bot.dhxy.window.execution.WindowTaskSnapshot;
import com.bot.dhxy.window.model.WindowNativeBinding;
import com.bot.dhxy.window.model.WindowRole;
import com.bot.dhxy.window.model.WindowRuntimeStatus;
import com.bot.dhxy.window.observation.StartupCombatGateService;
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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
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
    private final StartupCombatGateService startupCombatGateService;
    private final BagService bagService;
    private final AtomicBoolean remoteStartInFlight = new AtomicBoolean(false);
    private final AtomicLong remoteStartEpoch = new AtomicLong(0L);
    private final Object remoteStartLifecycleMonitor = new Object();

    @Autowired
    public WindowTaskControlService(MultiWindowTaskManager taskManager,
                                    TurnModeGuard turnModeGuard,
                                    CloudTurnSidecarLauncher sidecarLauncher,
                                    WindowNativeBindingRefreshService bindingRefreshService,
                                    BotProperties botProperties,
                                    LocalTeamRolePreflightService localTeamRolePreflightService,
                                    StartupCombatGateService startupCombatGateService,
                                    BagService bagService) {
        this.taskManager = Objects.requireNonNull(taskManager, "taskManager");
        this.turnModeGuard = Objects.requireNonNull(turnModeGuard, "turnModeGuard");
        this.sidecarLauncher = Objects.requireNonNull(sidecarLauncher, "sidecarLauncher");
        this.bindingRefreshService = Objects.requireNonNull(bindingRefreshService, "bindingRefreshService");
        this.botProperties = Objects.requireNonNull(botProperties, "botProperties");
        this.localTeamRolePreflightService = Objects.requireNonNull(
                localTeamRolePreflightService, "localTeamRolePreflightService");
        this.startupCombatGateService = Objects.requireNonNull(startupCombatGateService, "startupCombatGateService");
        this.bagService = Objects.requireNonNull(bagService, "bagService");
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
        List<String> ids = normalizeWindowIds(request.getWindowIds());
        StartLifecycle lifecycle = classifyStartLifecycle(ids);
        if (lifecycle != StartLifecycle.COLD_START) {
            String reason = switch (lifecycle) {
                case PAUSE_RESUME -> "暂停窗口必须使用暂停热恢复入口";
                case MIXED -> "暂停窗口与非暂停窗口不能混合启动";
                case INVALID -> "启动窗口不存在或生命周期状态无效";
                case COLD_START -> throw new IllegalStateException("unreachable cold-start rejection");
            };
            return remoteStartRejected(ids, reason);
        }
        return switch (request.getStartMode()) {
            case SAME_TASK -> startRemoteSameTask(
                    turnModeGuard.deviceId(), request.getWindowIds(), request.getTaskQueue(), StartLifecycle.COLD_START);
            case SELECTED_TASK -> startRemoteSelectedTask(
                    turnModeGuard.deviceId(), request.getWindowIds(), StartLifecycle.COLD_START);
            case DETECTED_ROLE -> remoteDetectedRoleRejected(request.getWindowIds());
        };
    }

    /**
     * Starts a new remote run from retained PAUSED window identity without replaying cold-start UI preparation.
     *
     * @param request exact paused window ids and the task queue to start; must not mix paused and non-paused windows.
     * @return per-window start result; identity or authority gaps fail before any new remote loop is created.
     */
    public WindowTaskCommandResult resumePaused(WindowTaskStartRequest request) {
        if (request == null || !request.hasWindows()) {
            return WindowTaskCommandResult.empty("没有选中的暂停窗口", getSnapshots());
        }
        List<String> ids = normalizeWindowIds(request.getWindowIds());
        StartLifecycle lifecycle = classifyStartLifecycle(ids);
        if (lifecycle != StartLifecycle.PAUSE_RESUME) {
            String reason = lifecycle == StartLifecycle.MIXED
                    ? "暂停窗口与非暂停窗口不能混合恢复"
                    : "暂停热恢复只接受全部处于 PAUSED 的窗口";
            return remoteStartRejected(ids, reason);
        }
        return switch (request.getStartMode()) {
            case SAME_TASK -> startRemoteSameTask(
                    turnModeGuard.deviceId(), ids, request.getTaskQueue(), StartLifecycle.PAUSE_RESUME);
            case SELECTED_TASK -> startRemoteSelectedTask(
                    turnModeGuard.deviceId(), ids, StartLifecycle.PAUSE_RESUME);
            case DETECTED_ROLE -> remoteDetectedRoleRejected(ids);
        };
    }

    public WindowTaskCommandResult startSelectedTasks(Collection<String> windowIds) {
        return start(WindowTaskStartRequest.selectedTask(windowIds));
    }

    /** Starts the explicit one-shot 一品侍卫 test on all selected windows as one team run. */
    public WindowTaskCommandResult startYipinGuardTest(Collection<String> windowIds) {
        List<String> ids = normalizeWindowIds(windowIds);
        if (ids.isEmpty()) {
            return WindowTaskCommandResult.empty("没有选中的窗口", getSnapshots());
        }
        List<String> activeWindows = ids.stream()
                .filter(windowId -> turnModeGuard.remoteState(windowId).registered())
                .toList();
        if (!activeWindows.isEmpty()) {
            return remoteStartRejected(ids, "一品侍卫测试正在运行，不能重复启动：" + activeWindows);
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

            String leaderWindowId = ids.stream()
                    .filter(windowId -> taskManager.getRunner(windowId)
                            .map(WindowTaskRunner::getWindowContext)
                            .map(WindowRuntimeContext::isLeader)
                            .orElse(false))
                    .findFirst()
                    .orElse(ids.get(0));
            String teamSessionKey = "yipin-guard-team-" + UUID.randomUUID();
            WindowTaskQueue queue = WindowTaskQueue.single(TaskType.YIPIN_GUARD_TEST);
            List<TurnTaskCode> taskCodes = List.of(TurnTaskCode.YIPIN_GUARD_TEST);
            List<Integer> taskMaxRuns = toTaskMaxRuns(taskCodes);
            List<WindowTaskCommandDetail> details = new ArrayList<>();

            // This is an explicit UI diagnostic: do not make the test wait on the general hover-tooltip
            // preflight. The chosen leader is carried as task-start authority, then starts before members.
            details.add(startOneRemote(turnModeGuard.deviceId(), leaderWindowId, taskCodes, taskMaxRuns,
                    TurnTaskQueueFailurePolicy.CONTINUE_ON_FAILURE, queue, teamSessionKey,
                    new LocalTeamRolePreflightService.Preflight(
                            leaderWindowId, LocalTeamRolePreflightService.Role.SOLO, null, false, null),
                    startEpoch, 0L, TaskStartupMode.NORMAL, leaderWindowId));
            List<CompletableFuture<WindowTaskCommandDetail>> memberStarts = ids.stream()
                    .filter(windowId -> !leaderWindowId.equals(windowId))
                    .map(windowId -> CompletableFuture.supplyAsync(() -> startOneRemote(
                            turnModeGuard.deviceId(), windowId, taskCodes, taskMaxRuns,
                            TurnTaskQueueFailurePolicy.CONTINUE_ON_FAILURE, queue, teamSessionKey,
                            new LocalTeamRolePreflightService.Preflight(
                                    windowId, LocalTeamRolePreflightService.Role.SOLO, null, false, null),
                            startEpoch, 0L, TaskStartupMode.NORMAL, leaderWindowId)))
                    .toList();
            details.addAll(memberStarts.stream().map(CompletableFuture::join).toList());
            int successCount = (int) details.stream().filter(WindowTaskCommandDetail::isSuccess).count();
            log.info("Start explicit Yipin Guard test: leaderWindowId={} windows={} successCount={}",
                    leaderWindowId, ids, successCount);
            return buildResult(ids.size(), successCount, "一品侍卫测试启动完成", details);
        } finally {
            remoteStartInFlight.set(false);
        }
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
        StartLifecycle lifecycle = classifyStartLifecycle(ids);
        if (lifecycle != StartLifecycle.COLD_START) {
            return remoteStartRejected(ids, "该入口只接受非暂停窗口的冷启动");
        }
        return startRemoteSameTask(deviceId, ids, queue, StartLifecycle.COLD_START);
    }

    private WindowTaskCommandResult startRemoteSameTask(String deviceId,
                                                        Collection<String> windowIds,
                                                        WindowTaskQueue queue,
                                                        StartLifecycle lifecycle) {
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
                    lifecycle == StartLifecycle.PAUSE_RESUME ? "暂停热恢复启动完成" : "远程批量启动完成",
                    startEpoch, lifecycle);
        } finally {
            remoteStartInFlight.set(false);
        }
    }

    public WindowTaskCommandResult startRemoteSelectedTask(String deviceId, Collection<String> windowIds) {
        List<String> ids = normalizeWindowIds(windowIds);
        StartLifecycle lifecycle = classifyStartLifecycle(ids);
        if (lifecycle != StartLifecycle.COLD_START) {
            return remoteStartRejected(ids, "该入口只接受非暂停窗口的冷启动");
        }
        return startRemoteSelectedTask(deviceId, ids, StartLifecycle.COLD_START);
    }

    private WindowTaskCommandResult startRemoteSelectedTask(String deviceId,
                                                            Collection<String> windowIds,
                                                            StartLifecycle lifecycle) {
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
            Map<String, TaskType> taskTypes = selectedTaskTypes(ids);
            TaskStartupMode startupMode;
            try {
                startupMode = lifecycle == StartLifecycle.COLD_START
                        ? awaitColdStartCombatExit(ids, taskTypes, startEpoch)
                        : TaskStartupMode.PAUSE_RESUME;
            } catch (StartupCombatGateService.StartupCombatProbeException unavailable) {
                return remoteStartRejected(ids, unavailable.getMessage());
            }
            if (isRemoteStartCancelled(startEpoch)) {
                return remoteStartCancelled(ids);
            }
            String teamSessionKey = "cr212-team-" + UUID.randomUUID();
            long roleResolutionDeadlineNanos = lifecycle == StartLifecycle.COLD_START
                    ? localTeamRolePreflightService.newRoleResolutionDeadlineNanos()
                    : 0L;
            Map<String, LocalTeamRolePreflightService.Preflight> preflightByWindow;
            try {
                preflightByWindow = lifecycle == StartLifecycle.PAUSE_RESUME
                        ? preparePauseResumeTeamRoles(ids, taskTypes)
                        : prepareLocalTeamRoles(ids, teamSessionKey, startEpoch, roleResolutionDeadlineNanos);
            } catch (LocalTeamRolePreflightService.PreflightTimeoutException timeout) {
                return remoteStartRejected(ids, timeout.getMessage());
            } catch (IllegalStateException invalidRetainedIdentity) {
                return remoteStartRejected(ids, invalidRetainedIdentity.getMessage());
            }
            if (isRemoteStartCancelled(startEpoch)) {
                return remoteStartCancelled(ids);
            }
            if (lifecycle == StartLifecycle.COLD_START) {
                calibrateMainBagTaskTabBeforeRemoteStart(ids, startEpoch);
                if (isRemoteStartCancelled(startEpoch)) {
                    return remoteStartCancelled(ids);
                }
            }
            List<String> startOrder = ids.stream()
                    .sorted((left, right) -> Boolean.compare(
                            isLocalLeader(preflightByWindow.getOrDefault(right, unknownPreflight(right))),
                            isLocalLeader(preflightByWindow.getOrDefault(left, unknownPreflight(left)))))
                    .toList();
            List<WindowTaskCommandDetail> details = new ArrayList<>();
            // The locally recognized leader starts first. Cloud receives only this already-resolved role fact.
            for (String windowId : startOrder) {
                if (!isLocalLeader(preflightByWindow.getOrDefault(windowId, unknownPreflight(windowId)))) {
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
                        preflightByWindow.getOrDefault(windowId, unknownPreflight(windowId)), startEpoch,
                        roleResolutionDeadlineNanos, startupMode, null));
            }
            List<CompletableFuture<WindowTaskCommandDetail>> startFutures = new ArrayList<>();
            for (String windowId : startOrder) {
                if (isLocalLeader(preflightByWindow.getOrDefault(windowId, unknownPreflight(windowId)))) {
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
                        preflightByWindow.getOrDefault(windowId, unknownPreflight(windowId)), startEpoch,
                        roleResolutionDeadlineNanos, startupMode, null)));
            }
            details.addAll(startFutures.stream().map(CompletableFuture::join).toList());
            int successCount = (int) details.stream().filter(WindowTaskCommandDetail::isSuccess).count();
            return buildResult(ids.size(), successCount,
                    lifecycle == StartLifecycle.PAUSE_RESUME ? "暂停热恢复启动完成" : "远程选中任务启动完成",
                    details);
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
                                                     long startEpoch,
                                                     StartLifecycle lifecycle) {
        CloudTurnSidecarLauncher.Readiness readiness =
                sidecarLauncher.ensureReady(() -> isRemoteStartCancelled(startEpoch));
        if (!readiness.ready()) {
            return remoteStartUnavailable(windowIds, readiness.message());
        }
        if (isRemoteStartCancelled(startEpoch)) {
            return remoteStartCancelled(windowIds);
        }
        Map<String, TaskType> taskTypes = sameTaskTypes(windowIds, queue.firstTaskType());
        TaskStartupMode startupMode;
        try {
            startupMode = lifecycle == StartLifecycle.COLD_START
                    ? awaitColdStartCombatExit(windowIds, taskTypes, startEpoch)
                    : TaskStartupMode.PAUSE_RESUME;
        } catch (StartupCombatGateService.StartupCombatProbeException unavailable) {
            return remoteStartRejected(windowIds, unavailable.getMessage());
        }
        if (isRemoteStartCancelled(startEpoch)) {
            return remoteStartCancelled(windowIds);
        }
        String teamSessionKey = "cr212-team-" + UUID.randomUUID();
        long roleResolutionDeadlineNanos = lifecycle == StartLifecycle.COLD_START
                ? localTeamRolePreflightService.newRoleResolutionDeadlineNanos()
                : 0L;
        Map<String, LocalTeamRolePreflightService.Preflight> preflightByWindow;
        try {
            preflightByWindow = lifecycle == StartLifecycle.PAUSE_RESUME
                    ? preparePauseResumeTeamRoles(windowIds, taskTypes)
                    : prepareLocalTeamRoles(windowIds, teamSessionKey, startEpoch, roleResolutionDeadlineNanos);
        } catch (LocalTeamRolePreflightService.PreflightTimeoutException timeout) {
            return remoteStartRejected(windowIds, timeout.getMessage());
        } catch (IllegalStateException invalidRetainedIdentity) {
            return remoteStartRejected(windowIds, invalidRetainedIdentity.getMessage());
        }
        if (isRemoteStartCancelled(startEpoch)) {
            return remoteStartCancelled(windowIds);
        }
        if (lifecycle == StartLifecycle.COLD_START) {
            calibrateMainBagTaskTabBeforeRemoteStart(windowIds, startEpoch);
            if (isRemoteStartCancelled(startEpoch)) {
                return remoteStartCancelled(windowIds);
            }
        }
        List<String> startOrder = windowIds.stream()
                .sorted((left, right) -> Boolean.compare(
                        isLocalLeader(preflightByWindow.getOrDefault(right, unknownPreflight(right))),
                        isLocalLeader(preflightByWindow.getOrDefault(left, unknownPreflight(left)))))
                .toList();
        List<WindowTaskCommandDetail> details = new ArrayList<>();
        for (String windowId : startOrder) {
            if (!isLocalLeader(preflightByWindow.getOrDefault(windowId, unknownPreflight(windowId)))) {
                continue;
            }
            if (isRemoteStartCancelled(startEpoch)) {
                details.add(WindowTaskCommandDetail.failed(windowId, "远程启动已被暂停或停止"));
                continue;
            }
            details.add(startOneRemote(
                    deviceId, windowId, taskCodes, taskMaxRuns, failurePolicy, queue,
                    teamSessionKey, preflightByWindow.getOrDefault(windowId, unknownPreflight(windowId)),
                    startEpoch, roleResolutionDeadlineNanos, startupMode, null));
        }
        List<CompletableFuture<WindowTaskCommandDetail>> startFutures = new ArrayList<>();
        for (String windowId : startOrder) {
            if (isLocalLeader(preflightByWindow.getOrDefault(windowId, unknownPreflight(windowId)))) {
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
                    startEpoch, roleResolutionDeadlineNanos, startupMode, null)));
        }
        details.addAll(startFutures.stream().map(CompletableFuture::join).toList());
        int successCount = (int) details.stream().filter(WindowTaskCommandDetail::isSuccess).count();
        return buildResult(windowIds.size(), successCount, summary, details);
    }

    /**
     * Let the registered leader establish the process-wide main-bag task-tab index before any remote turn loop
     * can request a task-page item. A missing local leader is deliberately not guessed: the later bag action will
     * fail closed rather than clicking the historical fixed sixth tab in an arbitrary member window.
     */
    private void calibrateMainBagTaskTabBeforeRemoteStart(List<String> windowIds, long startEpoch) {
        if (isRemoteStartCancelled(startEpoch)) {
            return;
        }
        WindowRuntimeContext leader = windowIds.stream()
                .map(windowId -> taskManager.getRunner(windowId).map(WindowTaskRunner::getWindowContext).orElse(null))
                .filter(Objects::nonNull)
                .filter(WindowRuntimeContext::isLeader)
                .findFirst()
                .orElse(null);
        if (leader == null) {
            log.info("Skip startup main-bag task-tab calibration: no locally registered leader in windows={}", windowIds);
            return;
        }
        WindowNativeBinding binding = bindingRefreshService.refreshAndCommit(leader).orElse(null);
        if (!bagService.calibrateMainBagTaskTabAtStartup(leader, binding)) {
            log.warn("Startup main-bag task-tab calibration unavailable; task-page item actions will fail closed: windowId={}",
                    leader.getWindowId());
        }
    }

    private WindowTaskCommandDetail startOneRemote(String deviceId,
                                                   String windowId,
                                                   List<TurnTaskCode> taskCodes,
                                                   List<Integer> taskMaxRuns,
                                                   TurnTaskQueueFailurePolicy failurePolicy,
                                                   WindowTaskQueue queue,
                                                   String teamSessionKey,
                                                   LocalTeamRolePreflightService.Preflight teamPreflight,
                                                   long startEpoch,
                                                   long roleResolutionDeadlineNanos,
                                                   TaskStartupMode startupMode,
                                                   String explicitLeaderWindowId) {
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
        // G008 phase 2: make the new Cloud acknowledgement and its observation runner see a clean task boundary.
        // This is deliberately before startRemote(); markRemoteStarted() runs after ACK and must not erase the
        // newly captured startup screen state.
        runner.prepareRemoteFreshStart("new remote task start pending Cloud acknowledgement");
        WindowRuntimeContext context = runner.getWindowContext();
        Supplier<TurnWindowMetadata> metadataSupplier = explicitLeaderWindowId == null
                ? new RemoteTurnMetadataSupplier(deviceId, context, bindingRefreshService,
                        teamSessionKey, teamPreflight, startupMode)
                : new RemoteTurnMetadataSupplier(deviceId, context, bindingRefreshService, teamSessionKey,
                        context.getWindowId().equals(explicitLeaderWindowId) ? "LEADER" : "MEMBER",
                        explicitLeaderWindowId, !context.getWindowId().equals(explicitLeaderWindowId), startupMode);
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
                if (loop.wasTaskStartExplicitlyRejected()) {
                    if (!turnModeGuard.awaitAndRemoveStoppedRemote(windowId)) {
                        return WindowTaskCommandDetail.failed(windowId,
                                "Cloud已明确拒绝启动，但本地拒绝态 loop 未能移除");
                    }
                    context.clearTaskExecutionState("Cloud explicitly rejected remote start: " + reason);
                }
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
                context.setRole(acknowledgedWindowRole(windowId, teamPreflight, explicitLeaderWindowId));
            }
            RemoteTaskHandle startedHandle = runner.getRemoteTaskHandle();
            loop.taskTerminalResult().thenAccept(
                    terminal -> projectRemoteTerminal(runner, startedHandle, terminal));
            return WindowTaskCommandDetail.success(windowId, "Cloud已确认远程任务启动");
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            return WindowTaskCommandDetail.failed(windowId, "等待Cloud确认启动时被中断");
        } catch (RuntimeException failure) {
            log.warn("Remote task start failed: windowId={} tasks={} reason={}",
                    windowId, taskCodes, failure.getMessage(), failure);
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
            // G008 phase 1: pause is an abort boundary, never an in-place remote resume.  The old
            // observer/handle/token must be torn down before a later UI start creates a fresh turn run.
            cancelPendingRemoteStarts("pause");
            return abortRemoteRuns(windowIds, "remote turn paused; fresh start required",
                    WindowRuntimeStatus.PAUSED, "远程暂停并清理旧运行完成");
        }
    }

    public WindowTaskCommandResult resumeRemoteWindows(Collection<String> windowIds) {
        return WindowTaskCommandResult.empty("暂停后的旧运行已清理；请使用启动创建新的远程运行",
                getSnapshots());
    }

    public WindowTaskCommandResult stopRemoteWindows(Collection<String> windowIds) {
        synchronized (remoteStartLifecycleMonitor) {
            cancelPendingRemoteStarts("stop");
            return abortRemoteRuns(windowIds, "remote turn stopped",
                    WindowRuntimeStatus.STOPPED, "远程停止选中窗口完成");
        }
    }

    private WindowTaskCommandResult abortRemoteRuns(Collection<String> windowIds,
                                                     String reason,
                                                     WindowRuntimeStatus lifecycleStatus,
                                                     String summary) {
        List<String> ids = normalizeWindowIds(windowIds);
        if (ids.isEmpty()) {
            return WindowTaskCommandResult.empty("没有选中的窗口", getSnapshots());
        }

        // Fence every local run before Cloud can complete the old turn. Its late terminal callback must see a
        // different handle and therefore cannot overwrite PAUSED with STOPPED/SKIPPED.
        for (String windowId : ids) {
            taskManager.getRunner(windowId).ifPresent(
                    runner -> runner.abortRemoteRun(reason, lifecycleStatus));
        }

        // Broadcast before waiting: a slow Cloud task in one window must never delay the stop signal for its peers.
        List<WindowTaskCommandDetail> details = new ArrayList<>();
        List<String> requested = new ArrayList<>();
        for (String windowId : ids) {
            try {
                if (turnModeGuard.requestRemoteStop(windowId)) {
                    requested.add(windowId);
                } else {
                    details.add(WindowTaskCommandDetail.success(
                            windowId, "未找到远程 loop，本地运行边界已清理"));
                }
            } catch (RuntimeException failure) {
                details.add(WindowTaskCommandDetail.failed(windowId, "远程停止请求失败：" + failure.getMessage()));
            }
        }
        for (String windowId : requested) {
            try {
                if (!turnModeGuard.awaitAndRemoveStoppedRemote(windowId)) {
                    details.add(WindowTaskCommandDetail.success(
                            windowId, "远程 turn loop 已不存在，本地运行边界已清理"));
                    continue;
                }
                details.add(WindowTaskCommandDetail.success(windowId, "已停止并移除远程 turn loop"));
            } catch (RuntimeException failure) {
                details.add(WindowTaskCommandDetail.failed(windowId, "远程停止未确认：" + failure.getMessage()));
            }
        }
        int successCount = (int) details.stream().filter(WindowTaskCommandDetail::isSuccess).count();
        return buildResult(ids.size(), successCount, summary, details);
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
            case XINSHOU -> TurnTaskCode.XINSHOU;
            case XINSHOU_TRAINING -> TurnTaskCode.XINSHOU_TRAINING;
            case CATCH_GHOST -> TurnTaskCode.CATCH_GHOST;
            case YIPIN_GUARD_TEST -> TurnTaskCode.YIPIN_GUARD_TEST;
            case WILD_BATTLE -> TurnTaskCode.WILD_BATTLE;
            case TIANTING -> TurnTaskCode.TIANTING;
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
            case XINSHOU -> TaskType.XINSHOU;
            case XINSHOU_TRAINING -> TaskType.XINSHOU_TRAINING;
            case CATCH_GHOST -> TaskType.CATCH_GHOST;
            case YIPIN_GUARD_TEST -> TaskType.YIPIN_GUARD_TEST;
            case WILD_BATTLE -> TaskType.WILD_BATTLE;
            case TIANTING -> TaskType.TIANTING;
            case AUTO_BATTLE -> TaskType.AUTO_BATTLE;
            case SLEEP_COMPUTER -> TaskType.SLEEP_COMPUTER;
        };
    }

    private List<Integer> toTaskMaxRuns(List<TurnTaskCode> taskCodes) {
        return taskCodes.stream().map(code -> switch (code) {
            case WUBEI -> botProperties.getFivefoldMaxRuns();
            case XIULUO_V2 -> botProperties.getXiuluoMaxRuns();
            case XINSHOU_TRAINING -> botProperties.getXinshouTrainingMaxRuns();
            case CATCH_GHOST -> botProperties.getCatchGhostMaxRuns();
            case YIPIN_GUARD_TEST -> 1;
            case WUHUAN_V2 -> botProperties.getWuhuanMaxRuns();
            case XINSHOU -> 1;
            case WILD_BATTLE -> botProperties.getWildBattleDurationMinutes();
            case TIANTING -> botProperties.getTiantingMaxRuns();
            case AUTO_BATTLE -> botProperties.getAutoBattleDurationMinutes();
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
        /*
         * Log each failed window's own reason. The aggregate ("0/1") is the only thing that reached the UI and the
         * log before, while the reason each window actually produced was carried in the details and then dropped —
         * so a start that refused for a nameable cause was indistinguishable from one that vanished.
         */
        if (details != null && success < requested) {
            for (WindowTaskCommandDetail detail : details) {
                if (detail != null && !detail.isSuccess()) {
                    log.warn("{} failed: windowId={} reason={}", summary, detail.getWindowId(),
                            detail.getMessage());
                }
            }
        }
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

    private StartLifecycle classifyStartLifecycle(List<String> windowIds) {
        if (windowIds == null || windowIds.isEmpty()) {
            return StartLifecycle.INVALID;
        }
        List<WindowTaskSnapshot> snapshots = windowIds.stream()
                .map(windowId -> taskManager.getSnapshot(windowId).orElse(null))
                .filter(Objects::nonNull)
                .toList();
        if (snapshots.size() != windowIds.size()) {
            return StartLifecycle.INVALID;
        }
        return classifyStartLifecycleStatuses(snapshots.stream().map(WindowTaskSnapshot::getStatus).toList());
    }

    static StartLifecycle classifyStartLifecycleStatuses(List<WindowRuntimeStatus> statuses) {
        if (statuses == null || statuses.isEmpty() || statuses.stream().anyMatch(Objects::isNull)) {
            return StartLifecycle.INVALID;
        }
        boolean anyPaused = statuses.stream().anyMatch(status -> status == WindowRuntimeStatus.PAUSED);
        boolean allPaused = statuses.stream().allMatch(status -> status == WindowRuntimeStatus.PAUSED);
        if (allPaused) {
            return StartLifecycle.PAUSE_RESUME;
        }
        return anyPaused ? StartLifecycle.MIXED : StartLifecycle.COLD_START;
    }

    private Map<String, TaskType> selectedTaskTypes(List<String> windowIds) {
        Map<String, TaskType> taskTypes = new LinkedHashMap<>();
        for (String windowId : windowIds) {
            TaskType taskType = taskManager.getSnapshot(windowId)
                    .map(WindowTaskSnapshot::getSelectedTaskType)
                    .orElse(TaskType.UNKNOWN);
            taskTypes.put(windowId, taskType);
        }
        return Map.copyOf(taskTypes);
    }

    private static Map<String, TaskType> sameTaskTypes(List<String> windowIds, TaskType taskType) {
        Map<String, TaskType> taskTypes = new LinkedHashMap<>();
        for (String windowId : windowIds) {
            taskTypes.put(windowId, taskType);
        }
        return Map.copyOf(taskTypes);
    }

    private Map<String, LocalTeamRolePreflightService.Preflight> preparePauseResumeTeamRoles(
            List<String> windowIds,
            Map<String, TaskType> taskTypes) {
        List<WindowRuntimeContext> contexts = new ArrayList<>();
        for (String windowId : windowIds) {
            WindowRuntimeContext context = taskManager.getRunner(windowId)
                    .map(WindowTaskRunner::getWindowContext)
                    .orElseThrow(() -> new IllegalStateException(
                            "暂停热恢复缺少已注册窗口：" + windowId));
            WindowNativeBinding binding = bindingRefreshService.refreshAndCommit(context)
                    .orElseThrow(() -> new IllegalStateException(
                            "暂停热恢复缺少有效 HWND 绑定：" + windowId));
            if (!binding.hasNativeHandle() || !binding.hasGeometry()) {
                throw new IllegalStateException("暂停热恢复 HWND 绑定不完整：" + windowId);
            }
            contexts.add(context);
        }
        Map<String, LocalTeamRolePreflightService.Preflight> retained =
                retainedPauseResumePreflights(contexts, taskTypes);
        log.info("Pause-resume retained lifecycle accepted: windows={} roles={} tasks={}",
                windowIds,
                retained.entrySet().stream()
                        .map(entry -> entry.getKey() + ":" + entry.getValue().role())
                        .toList(),
                taskTypes);
        return retained;
    }

    static Map<String, LocalTeamRolePreflightService.Preflight> retainedPauseResumePreflights(
            List<WindowRuntimeContext> contexts,
            Map<String, TaskType> taskTypes) {
        if (contexts == null || contexts.isEmpty()) {
            throw new IllegalStateException("暂停热恢复没有保留的窗口身份");
        }
        Map<String, LocalTeamRolePreflightService.Preflight> retained = new LinkedHashMap<>();
        boolean teamAuthorityRequired = false;
        for (WindowRuntimeContext context : contexts) {
            if (context == null || context.getWindowId() == null || context.getWindowId().isBlank()) {
                throw new IllegalStateException("暂停热恢复存在无效窗口身份");
            }
            String windowId = context.getWindowId();
            WindowNativeBinding binding = context.getNativeBinding();
            if (binding == null || !binding.hasNativeHandle() || !binding.hasGeometry()) {
                throw new IllegalStateException("暂停热恢复缺少有效 HWND 绑定：" + windowId);
            }
            if (context.getRole() == null || context.getRole() == WindowRole.UNKNOWN) {
                throw new IllegalStateException("暂停热恢复缺少已确认角色：" + windowId);
            }
            if (context.getSelectedTaskType() == null || context.getSelectedTaskType() == TaskType.UNKNOWN) {
                throw new IllegalStateException("暂停热恢复缺少已选任务：" + windowId);
            }
            TaskType taskType = taskTypes == null ? null : taskTypes.get(windowId);
            if (taskType == null || taskType == TaskType.UNKNOWN) {
                throw new IllegalStateException("暂停热恢复缺少请求任务：" + windowId);
            }
            LocalTeamRolePreflightService.Role role;
            if (taskType.isSinglePlayer()) {
                role = LocalTeamRolePreflightService.Role.SOLO;
            } else if (taskType == TaskType.AUTO_BATTLE) {
                role = LocalTeamRolePreflightService.Role.MEMBER;
            } else {
                role = context.isLeader()
                        ? LocalTeamRolePreflightService.Role.LEADER
                        : LocalTeamRolePreflightService.Role.MEMBER;
            }
            teamAuthorityRequired |= requiresUniqueLeader(taskType);
            retained.put(windowId, new LocalTeamRolePreflightService.Preflight(
                    windowId, role, null, false, null));
        }
        if (teamAuthorityRequired) {
            long leaderCount = retained.values().stream()
                    .filter(preflight -> preflight.role() == LocalTeamRolePreflightService.Role.LEADER)
                    .count();
            boolean includesSolo = retained.values().stream()
                    .anyMatch(preflight -> preflight.role() == LocalTeamRolePreflightService.Role.SOLO);
            if (leaderCount != 1L || includesSolo) {
                throw new IllegalStateException(
                        "暂停热恢复组队权限不自洽：要求唯一 LEADER，actualLeaders=" + leaderCount);
            }
        }
        return Map.copyOf(retained);
    }

    private static boolean requiresUniqueLeader(TaskType taskType) {
        return switch (taskType) {
            case WUBEI, XIULUO_V2, XINSHOU_TRAINING, CATCH_GHOST, TIANTING -> true;
            default -> false;
        };
    }

    private TaskStartupMode awaitColdStartCombatExit(List<String> windowIds,
                                                      Map<String, TaskType> taskTypes,
                                                      long startEpoch) {
        Map<WindowRuntimeContext, TaskType> candidates = new LinkedHashMap<>();
        for (String windowId : windowIds) {
            TaskType taskType = taskTypes.getOrDefault(windowId, TaskType.UNKNOWN);
            if (!requiresUniqueLeader(taskType)) {
                continue;
            }
            taskManager.getRunner(windowId).ifPresent(
                    runner -> candidates.put(runner.getWindowContext(), taskType));
        }
        return startupCombatGateService.awaitCombatExit(
                candidates, () -> isRemoteStartCancelled(startEpoch));
    }

    private Map<String, LocalTeamRolePreflightService.Preflight> prepareLocalTeamRoles(
            List<String> windowIds, String teamSessionKey, long startEpoch, long roleResolutionDeadlineNanos) {
        Map<String, LocalTeamRolePreflightService.Preflight> knownRoles = new LinkedHashMap<>();
        List<WindowRuntimeContext> contexts = new ArrayList<>();
        for (String windowId : windowIds) {
            TaskType taskType = taskManager.getSnapshot(windowId)
                    .map(WindowTaskSnapshot::getSelectedTaskType)
                    .orElse(TaskType.UNKNOWN);
            LocalTeamRolePreflightService.Role fixedRole = switch (taskType) {
                case AUTO_BATTLE -> LocalTeamRolePreflightService.Role.MEMBER;
                case WUHuan_V2, XINSHOU -> LocalTeamRolePreflightService.Role.SOLO;
                default -> null;
            };
            if (fixedRole != null) {
                knownRoles.put(windowId, new LocalTeamRolePreflightService.Preflight(
                        windowId, fixedRole, null, false, null));
                log.info("skip local team-role panel probe for fixed task role: windowId={} taskType={} role={}",
                        windowId, taskType, fixedRole);
            } else {
                taskManager.getRunner(windowId).ifPresent(runner -> contexts.add(runner.getWindowContext()));
            }
        }
        try {
            knownRoles.putAll(localTeamRolePreflightService.prepareBatch(
                    contexts, teamSessionKey, () -> isRemoteStartCancelled(startEpoch), roleResolutionDeadlineNanos));
            return Map.copyOf(knownRoles);
        } catch (LocalTeamRolePreflightService.PreflightTimeoutException timeout) {
            throw timeout;
        } catch (RuntimeException failure) {
            // Do not turn a failed local identity proof into UNKNOWN and then start the task. That used to make
            // downstream leader-only startup work silently skip its owner. Hover/capture misses retry inside the
            // preflight service; this branch is only an unexpected infrastructure/programming failure.
            log.warn("CR212 local team-role preflight failed; reject this start instead of publishing UNKNOWN: "
                            + "session={} reason={}", teamSessionKey, failure.toString(), failure);
            throw new IllegalStateException("队伍身份预检失败，未启动任务", failure);
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
                windowId, LocalTeamRolePreflightService.Role.MEMBER, null, false, null);
    }

    private static boolean isLocalLeader(LocalTeamRolePreflightService.Preflight preflight) {
        return preflight != null && preflight.role() == LocalTeamRolePreflightService.Role.LEADER;
    }

    /**
     * Projects a role that Cloud has already acknowledged into the retained local window identity.
     * SOLO is stored as local leader authority because {@link WindowRole} has no separate solo value and the
     * window owns its own task flow. Callers must invoke this only after the start ACK and final cancellation fence.
     */
    static WindowRole acknowledgedWindowRole(
            String windowId,
            LocalTeamRolePreflightService.Preflight preflight,
            String explicitLeaderWindowId) {
        if (explicitLeaderWindowId != null) {
            return explicitLeaderWindowId.equals(windowId) ? WindowRole.LEADER : WindowRole.MEMBER;
        }
        if (preflight == null || preflight.role() == null) {
            throw new IllegalStateException("Cloud确认启动但本地角色预检缺失：" + windowId);
        }
        return switch (preflight.role()) {
            // WindowRole describes local execution authority; a solo task owns its main flow like a leader.
            case LEADER, SOLO -> WindowRole.LEADER;
            case MEMBER -> WindowRole.MEMBER;
        };
    }

    enum StartLifecycle {
        COLD_START,
        PAUSE_RESUME,
        MIXED,
        INVALID
    }

    static final class RemoteTurnMetadataSupplier implements Supplier<TurnWindowMetadata> {
        private final String deviceId;
        private final WindowRuntimeContext context;
        private final WindowNativeBindingRefreshService bindingRefreshService;
        private final String teamSessionKey;
        private final LocalTeamRolePreflightService.Preflight teamPreflight;
        private final boolean localTeamBatch;
        private final String explicitWindowRole;
        private final String explicitLeaderWindowId;
        private final boolean explicitSupportMember;
        private final TaskStartupMode startupMode;
        private final AtomicBoolean teamPreflightUnsent = new AtomicBoolean(true);

        RemoteTurnMetadataSupplier(String deviceId,
                                   WindowRuntimeContext context,
                                   WindowNativeBindingRefreshService bindingRefreshService) {
            this(deviceId, context, bindingRefreshService, "standalone", unknownPreflight(context.getWindowId()),
                    TaskStartupMode.NORMAL);
        }

        RemoteTurnMetadataSupplier(String deviceId,
                                   WindowRuntimeContext context,
                                   WindowNativeBindingRefreshService bindingRefreshService,
                                   String teamSessionKey,
                                   LocalTeamRolePreflightService.Preflight teamPreflight) {
            this(deviceId, context, bindingRefreshService, teamSessionKey, teamPreflight, TaskStartupMode.NORMAL);
        }

        RemoteTurnMetadataSupplier(String deviceId,
                                   WindowRuntimeContext context,
                                   WindowNativeBindingRefreshService bindingRefreshService,
                                   String teamSessionKey,
                                   LocalTeamRolePreflightService.Preflight teamPreflight,
                                   TaskStartupMode startupMode) {
            this.deviceId = Objects.requireNonNull(deviceId, "deviceId");
            this.context = Objects.requireNonNull(context, "context");
            this.bindingRefreshService = Objects.requireNonNull(bindingRefreshService, "bindingRefreshService");
            this.teamSessionKey = Objects.requireNonNull(teamSessionKey, "teamSessionKey");
            this.teamPreflight = Objects.requireNonNull(teamPreflight, "teamPreflight");
            this.localTeamBatch = !"standalone".equals(teamSessionKey);
            this.explicitWindowRole = null;
            this.explicitLeaderWindowId = null;
            this.explicitSupportMember = false;
            this.startupMode = Objects.requireNonNull(startupMode, "startupMode");
        }

        RemoteTurnMetadataSupplier(String deviceId,
                                   WindowRuntimeContext context,
                                   WindowNativeBindingRefreshService bindingRefreshService,
                                   String teamSessionKey,
                                   String explicitWindowRole,
                                   String explicitLeaderWindowId,
                                   boolean explicitSupportMember,
                                   TaskStartupMode startupMode) {
            this.deviceId = Objects.requireNonNull(deviceId, "deviceId");
            this.context = Objects.requireNonNull(context, "context");
            this.bindingRefreshService = Objects.requireNonNull(bindingRefreshService, "bindingRefreshService");
            this.teamSessionKey = Objects.requireNonNull(teamSessionKey, "teamSessionKey");
            this.teamPreflight = unknownPreflight(context.getWindowId());
            this.localTeamBatch = true;
            this.explicitWindowRole = Objects.requireNonNull(explicitWindowRole, "explicitWindowRole");
            this.explicitLeaderWindowId = Objects.requireNonNull(explicitLeaderWindowId, "explicitLeaderWindowId");
            this.explicitSupportMember = explicitSupportMember;
            this.startupMode = Objects.requireNonNull(startupMode, "startupMode");
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
            boolean explicitTeam = explicitWindowRole != null;
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
                    explicitTeam ? explicitWindowRole
                            : (publishPreflight ? teamPreflight.role().name() : context.getRole().name()),
                    localTeamBatch ? teamSessionKey : null,
                    explicitTeam ? explicitLeaderWindowId : null,
                    localTeamBatch,
                    explicitTeam && explicitSupportMember,
                    startupMode.name(),
                    explicitTeam ? Boolean.TRUE : (publishPreflight ? Boolean.TRUE : null),
                    explicitTeam ? teamSessionKey : (publishPreflight ? teamSessionKey : null),
                    explicitTeam ? null : (publishPreflight ? teamPreflight.groupHash() : null),
                    explicitTeam ? null : (publishPreflight ? teamPreflight.maskBase64() : null),
                    explicitTeam ? Boolean.FALSE
                            : (publishPreflight ? Boolean.valueOf(teamPreflight.representative()) : null));
        }
    }
}
