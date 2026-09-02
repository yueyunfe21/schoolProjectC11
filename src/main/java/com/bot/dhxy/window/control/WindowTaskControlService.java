package com.bot.dhxy.window.control;

import com.bot.dhxy.cloud.turn.TurnModeGuard;
import com.bot.dhxy.cloud.turn.CloudTurnSidecarLauncher;
import com.bot.dhxy.cloud.turn.WindowTurnLoop;
import com.bot.dhxy.cloud.turn.local.LocalTeamRolePreflightService;
import com.bot.dhxy.cloud.turn.protocol.TurnTaskCode;
import com.bot.dhxy.cloud.turn.protocol.TurnTaskQueueFailurePolicy;
import com.bot.dhxy.cloud.turn.protocol.TurnTaskStartAck;
import com.bot.dhxy.cloud.turn.protocol.TurnTaskStartRequest;
import com.bot.dhxy.cloud.turn.protocol.TurnTaskRuntimeSettings;
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
import com.bot.dhxy.window.model.WindowTaskRunProgress;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
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
    private final LocalFreshStartReset localFreshStartReset;
    private final AtomicBoolean remoteStartInFlight = new AtomicBoolean(false);
    private final AtomicLong remoteStartEpoch = new AtomicLong(0L);
    private final Object remoteStartLifecycleMonitor = new Object();
    private final Map<String, String> remoteTerminalRecoveryPending = new ConcurrentHashMap<>();
    /** 每窗口生命周期 epoch 下限:恢复/续跑/ACK 的 startEpoch 低于它即视为已被显式取消或替换。 */
    private final ConcurrentHashMap<String, Long> remoteWindowEpochFloor = new ConcurrentHashMap<>();

    @Autowired
    public WindowTaskControlService(MultiWindowTaskManager taskManager,
                                    TurnModeGuard turnModeGuard,
                                    CloudTurnSidecarLauncher sidecarLauncher,
                                    WindowNativeBindingRefreshService bindingRefreshService,
                                    BotProperties botProperties,
                                    LocalTeamRolePreflightService localTeamRolePreflightService,
                                    StartupCombatGateService startupCombatGateService,
                                    BagService bagService,
                                    LocalFreshStartReset localFreshStartReset) {
        this.taskManager = Objects.requireNonNull(taskManager, "taskManager");
        this.turnModeGuard = Objects.requireNonNull(turnModeGuard, "turnModeGuard");
        this.sidecarLauncher = Objects.requireNonNull(sidecarLauncher, "sidecarLauncher");
        this.bindingRefreshService = Objects.requireNonNull(bindingRefreshService, "bindingRefreshService");
        this.botProperties = Objects.requireNonNull(botProperties, "botProperties");
        this.localTeamRolePreflightService = Objects.requireNonNull(
                localTeamRolePreflightService, "localTeamRolePreflightService");
        this.startupCombatGateService = Objects.requireNonNull(startupCombatGateService, "startupCombatGateService");
        this.bagService = Objects.requireNonNull(bagService, "bagService");
        this.localFreshStartReset = Objects.requireNonNull(localFreshStartReset, "localFreshStartReset");
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
                    startEpoch, 0L, TaskStartupMode.NORMAL, leaderWindowId, null));
            List<CompletableFuture<WindowTaskCommandDetail>> memberStarts = ids.stream()
                    .filter(windowId -> !leaderWindowId.equals(windowId))
                    .map(windowId -> CompletableFuture.supplyAsync(() -> startOneRemote(
                            turnModeGuard.deviceId(), windowId, taskCodes, taskMaxRuns,
                            TurnTaskQueueFailurePolicy.CONTINUE_ON_FAILURE, queue, teamSessionKey,
                            new LocalTeamRolePreflightService.Preflight(
                                    windowId, LocalTeamRolePreflightService.Role.SOLO, null, false, null),
                            startEpoch, 0L, TaskStartupMode.NORMAL, leaderWindowId, null)))
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

    /** Starts the one-shot G056 acceptance for one exact leader window. */
    public WindowTaskCommandResult startG056DoubleExperienceAcceptance(String windowId) {
        return startG056DoubleExperienceAcceptance(List.of(windowId), windowId);
    }

    /**
     * Starts a real G056 team acceptance: members enter their production AUTO_BATTLE queue first,
     * then the exact leader runs the one-shot maintenance chain that opens the game broadcast.
     */
    public WindowTaskCommandResult startG056DoubleExperienceAcceptance(Collection<String> windowIds,
                                                                       String leaderWindowId) {
        List<String> ids = normalizeWindowIds(windowIds);
        if (ids.isEmpty() || leaderWindowId == null || !ids.contains(leaderWindowId)) {
            return WindowTaskCommandResult.empty("G056验收缺少精确队长窗口", getSnapshots());
        }
        List<String> activeWindows = ids.stream()
                .filter(windowId -> turnModeGuard.remoteState(windowId).registered())
                .toList();
        if (!activeWindows.isEmpty()) {
            return remoteStartRejected(ids, "G056验收窗口已有任务运行：" + activeWindows);
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
            String teamSessionKey = "g056-double-experience-" + UUID.randomUUID();
            List<WindowTaskCommandDetail> details = new ArrayList<>();
            List<String> startedMembers = new ArrayList<>();
            for (String memberWindowId : ids) {
                if (leaderWindowId.equals(memberWindowId)) {
                    continue;
                }
                WindowTaskCommandDetail member = startOneRemote(
                        turnModeGuard.deviceId(), memberWindowId, List.of(TurnTaskCode.AUTO_BATTLE), List.of(0),
                        TurnTaskQueueFailurePolicy.CONTINUE_ON_FAILURE,
                        WindowTaskQueue.single(TaskType.AUTO_BATTLE), teamSessionKey,
                        new LocalTeamRolePreflightService.Preflight(
                                memberWindowId, LocalTeamRolePreflightService.Role.MEMBER, null, false, null),
                        startEpoch, 0L, TaskStartupMode.NORMAL, leaderWindowId, null);
                details.add(member);
                if (member.isSuccess()) {
                    startedMembers.add(memberWindowId);
                }
            }
            if (startedMembers.size() != ids.size() - 1) {
                stopRemoteWindows(startedMembers);
                return buildResult(ids.size(), startedMembers.size(),
                        "G056队员广播消费者启动失败", details);
            }

            WindowTaskCommandDetail leader = startOneRemote(
                    turnModeGuard.deviceId(), leaderWindowId,
                    List.of(TurnTaskCode.G056_DOUBLE_EXPERIENCE_ACCEPTANCE), List.of(1),
                    TurnTaskQueueFailurePolicy.CONTINUE_ON_FAILURE,
                    WindowTaskQueue.single(TaskType.G056_DOUBLE_EXPERIENCE_ACCEPTANCE), teamSessionKey,
                    new LocalTeamRolePreflightService.Preflight(
                            leaderWindowId, LocalTeamRolePreflightService.Role.LEADER, null, false, null),
                    startEpoch, 0L, TaskStartupMode.NORMAL, leaderWindowId, null);
            details.add(leader);
            if (!leader.isSuccess()) {
                stopRemoteWindows(startedMembers);
            }
            int successCount = (int) details.stream().filter(WindowTaskCommandDetail::isSuccess).count();
            return buildResult(ids.size(), successCount, "G056领双队伍验收启动完成", details);
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
                        roleResolutionDeadlineNanos, startupMode, null, null));
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
                        roleResolutionDeadlineNanos, startupMode, null, null)));
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
                    startEpoch, roleResolutionDeadlineNanos, startupMode, null, null));
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
                    startEpoch, roleResolutionDeadlineNanos, startupMode, null, null)));
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
        // 2026-08-23 用户契约（停止=彻底清空）：全局页签校准按"冷启动批"清一次并当场重新校准，
        // 不放进每窗口复位链（审查修正：否则会抹掉本方法刚学到的值）。
        bagService.forgetMainBagTaskTabCalibration();
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
                                                   String explicitLeaderWindowId,
                                                   TurnTaskRuntimeSettings retainedRuntimeSettings) {
        // 按窗口判取消:别的窗口停止/启动不再作废本窗口的启动与恢复重启(2026-08-22 定案)。
        if (startEpoch <= 0L || Thread.currentThread().isInterrupted()
                || !isWindowLifecycleCurrent(windowId, startEpoch)) {
            return WindowTaskCommandDetail.failed(windowId, "远程启动已被暂停或停止");
        }
        // 本窗口的新生命周期从此开始:抬 floor,旧 run 留在飞行中的恢复/续跑就此作废(替换语义)。
        remoteWindowEpochFloor.merge(windowId, startEpoch, Math::max);
        WindowTaskRunner runner = taskManager.getRunner(windowId).orElse(null);
        if (runner == null) {
            return WindowTaskCommandDetail.failed(windowId, "窗口不存在");
        }
        WindowRuntimeContext context = runner.getWindowContext();
        WindowTaskRunProgress resumeProgress = resolvePauseResumeProgress(
                startupMode, taskCodes, taskMaxRuns, context.getPausedTaskRunProgress());
        List<Integer> taskInitialCompletedRuns = toTaskInitialCompletedRuns(
                taskCodes, resumeProgress);
        if (startupMode != TaskStartupMode.PAUSE_RESUME) {
            context.clearPausedTaskRunProgress("cold task start");
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
        /*
         * 2026-08-23 用户契约（停止=彻底清空）：用户手动新开一轮（NORMAL，覆盖停止后/队列做完后
         * 再启动；含战斗门放行后的 AFTER_COMBAT_EXIT_STARTUP 冷启动）——把本窗口现实记忆清到
         * 进程刚启动的状态。审查修正：必须放在旧任务循环确认停止之后，否则旧线程会在清完后
         * 立刻把脏缓存写回去（清了等于没清）。崩溃自动重启在 recoverRemoteTerminal 单独清；
         * 暂停恢复（PAUSE_RESUME）与队列内衔接（CLEAN_QUEUE_TRANSITION）不清。
         */
        if (startupMode == TaskStartupMode.NORMAL
                || startupMode == TaskStartupMode.AFTER_COMBAT_EXIT_STARTUP) {
            localFreshStartReset.resetWindowRealityMemory(
                    windowId, context, "startupMode=" + startupMode);
        }
        // G008 phase 2: make the new Cloud acknowledgement and its observation runner see a clean task boundary.
        // This is deliberately before startRemote(); markRemoteStarted() runs after ACK and must not erase the
        // newly captured startup screen state.
        runner.prepareRemoteFreshStart("new remote task start pending Cloud acknowledgement");
        if (startupMode == TaskStartupMode.NORMAL
                || startupMode == TaskStartupMode.AFTER_COMBAT_EXIT_STARTUP) {
            /*
             * G143：UI 的新任务入口虽已重扫全部窗口，但任务 owner 不能再靠 ACK 线程稍后从旧上下文补抓。
             * 在提交本 run 前再读取一次 exact HWND 活标题，并立即固化 owner；客户端进程无需重启。
             * PAUSE_RESUME 不走这里，避免把运行中临时切入的角色改认成任务主人。
             */
            bindingRefreshService.refreshAndCommit(context);
            context.captureTaskOwnerForNewRun();
            log.info("Remote task owner captured from live window: windowId={} startupMode={} owner={}/{}",
                    windowId, startupMode,
                    context.getTaskOwnerPlayerName(), context.getTaskOwnerPlayerId());
        }
        Supplier<TurnWindowMetadata> metadataSupplier = explicitLeaderWindowId == null
                ? new RemoteTurnMetadataSupplier(deviceId, context, bindingRefreshService,
                        teamSessionKey, teamPreflight, startupMode)
                : new RemoteTurnMetadataSupplier(deviceId, context, bindingRefreshService, teamSessionKey,
                        context.getWindowId().equals(explicitLeaderWindowId) ? "LEADER" : "MEMBER",
                        explicitLeaderWindowId, !context.getWindowId().equals(explicitLeaderWindowId), startupMode);
        TurnTaskRuntimeSettings runtimeSettings = retainedRuntimeSettings == null
                ? buildRuntimeSettingsSnapshot(botProperties)
                : retainedRuntimeSettings;
        log.info("Remote task runtime settings: windowId={} healPetMs={} repairMs={} maintenanceImmediate={} "
                        + "summonClean={}/{} startupPreparation={} doubleExperienceClaim={} leaderBox={} memberBox={} "
                        + "xiuluoSkipBoss={} supply=playerHp:{}/{} playerMp:{}/{} petHp:{}/{} petMp:{}/{}",
                windowId,
                runtimeSettings.healPetMaintenanceIntervalMs(),
                runtimeSettings.repairEquipmentMaintenanceIntervalMs(),
                runtimeSettings.maintenanceRunImmediatelyOnStart(),
                runtimeSettings.summonSkillCleanEnabled(), runtimeSettings.summonSkillCleanIntervalMs(),
                runtimeSettings.taskStartupPreparationEnabled(),
                runtimeSettings.doubleExperienceClaimEnabled(),
                runtimeSettings.leaderCommonBoxEnabled(), runtimeSettings.memberCommonBoxEnabled(),
                runtimeSettings.xiuluoSkipBossEnabled(),
                runtimeSettings.playerHpSupplyEnabled(), runtimeSettings.playerHpSupplyThreshold(),
                runtimeSettings.playerMpSupplyEnabled(), runtimeSettings.playerMpSupplyThreshold(),
                runtimeSettings.petHpSupplyEnabled(), runtimeSettings.petHpSupplyThreshold(),
                runtimeSettings.petMpSupplyEnabled(), runtimeSettings.petMpSupplyThreshold());
        TurnTaskStartRequest startRequest = new TurnTaskStartRequest(
                "remote-turn-" + UUID.randomUUID(), taskCodes, taskMaxRuns, taskInitialCompletedRuns, failurePolicy,
                runtimeSettings);
        log.info("Remote task initial progress: windowId={} startupMode={} taskCodes={} initialCompletedRuns={}",
                windowId, startupMode, taskCodes, taskInitialCompletedRuns);
        RemoteTerminalRecoveryPlan recoveryPlan = new RemoteTerminalRecoveryPlan(
                deviceId, windowId, List.copyOf(taskCodes), List.copyOf(taskMaxRuns), failurePolicy, queue,
                teamSessionKey, teamPreflight, startEpoch, roleResolutionDeadlineNanos,
                explicitLeaderWindowId, runtimeSettings,
                context.getTaskOwnerPlayerId(), context.getTaskOwnerPlayerName());
        try {
            WindowTurnLoop loop = turnModeGuard.startRemote(deviceId, windowId, metadataSupplier, startRequest);
            if (isRemoteStartCancelled(startEpoch)) {
                turnModeGuard.stopRemote(windowId);
                return WindowTaskCommandDetail.failed(windowId, "远程启动已被暂停或停止");
            }
            CompletableFuture<Void> startProjection = loop.startAcknowledgement().thenAccept(startAck -> projectAcknowledgedRemoteStart(
                    runner, context, loop, queue, teamPreflight, explicitLeaderWindowId,
                    startEpoch, startRequest.startRequestId(), startAck, recoveryPlan, resumeProgress, 1));
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
            startProjection.join();
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

    /**
     * Projects one exact Cloud start ACK without blocking the batch-start caller. A local projection exception is
     * retried with the same bounded per-window policy; it never stops the already-live Turn loop.
     */
    private void projectAcknowledgedRemoteStart(WindowTaskRunner runner,
                                                WindowRuntimeContext context,
                                                WindowTurnLoop loop,
                                                WindowTaskQueue requestedQueue,
                                                LocalTeamRolePreflightService.Preflight teamPreflight,
                                                String explicitLeaderWindowId,
                                                long startEpoch,
                                                String startRequestId,
                                                TurnTaskStartAck startAck,
                                                RemoteTerminalRecoveryPlan recoveryPlan,
                                                WindowTaskRunProgress resumeProgress,
                                                int attempt) {
        if (!isWindowLifecycleCurrent(context.getWindowId(), startEpoch) || !loop.isRunning()) {
            log.info("Ignore late Cloud start ACK after cancellation/replacement: windowId={} startRequestId={} epoch={}",
                    context.getWindowId(), startRequestId, startEpoch);
            return;
        }
        try {
            WindowTaskQueue effectiveQueue = projectEffectiveQueue(requestedQueue, startAck);
            synchronized (remoteStartLifecycleMonitor) {
                if (!isWindowLifecycleCurrent(context.getWindowId(), startEpoch) || !loop.isRunning()) {
                    return;
                }
                WindowTaskRunProgress effectiveProgress = resumeProgress != null
                        && resumeProgress.getTaskType() == effectiveQueue.firstTaskType()
                        ? resumeProgress
                        : null;
                runner.markRemoteStarted(effectiveQueue, effectiveProgress);
                context.setRole(acknowledgedWindowRole(
                        context.getWindowId(), teamPreflight, explicitLeaderWindowId));
            }
            RemoteTaskHandle startedHandle = runner.getRemoteTaskHandle();
            loop.taskTerminalResult().thenAccept(
                    terminal -> projectRemoteTerminal(runner, startedHandle, loop, terminal, recoveryPlan));
            log.info("Cloud start ACK projected asynchronously: windowId={} startRequestId={} attempt={}",
                    context.getWindowId(), startRequestId, attempt);
        } catch (RuntimeException failure) {
            if (!isWindowLifecycleCurrent(context.getWindowId(), startEpoch) || !loop.isRunning()) {
                return;
            }
            long retryDelayMs = WindowTurnLoop.failureRetryDelayMs(context.getWindowId(), attempt);
            log.error("Cloud start ACK local projection failed; retained and retrying: windowId={} "
                            + "startRequestId={} attempt={} retryDelayMs={} type={} message={}",
                    context.getWindowId(), startRequestId, attempt, retryDelayMs,
                    failure.getClass().getName(), failure.getMessage(), failure);
            CompletableFuture.delayedExecutor(retryDelayMs, TimeUnit.MILLISECONDS).execute(
                    () -> projectAcknowledgedRemoteStart(
                            runner, context, loop, requestedQueue, teamPreflight, explicitLeaderWindowId,
                            startEpoch, startRequestId, startAck, recoveryPlan, resumeProgress, attempt + 1));
        }
    }

    /**
     * Freeze the current JavaFX-backed settings for one exact remote start.
     *
     * <p>The returned value travels with the start request. Cloud binds it to the task run instead
     * of reading or mutating a process-global configuration bean.</p>
     *
     * @param botProperties live Client configuration already updated by the JavaFX settings page.
     * @return immutable snapshot of every UI-editable setting whose behavior is owned by Cloud.
     */
    static TurnTaskRuntimeSettings buildRuntimeSettingsSnapshot(BotProperties botProperties) {
        Objects.requireNonNull(botProperties, "botProperties");
        return new TurnTaskRuntimeSettings(
                botProperties.isSummonSkillCleanEnabled(),
                botProperties.getSummonSkillCleanIntervalMs(),
                botProperties.getXiuluoHealPetMaintenanceIntervalMs(),
                botProperties.getXiuluoRepairEquipmentMaintenanceIntervalMs(),
                botProperties.isXiuluoMaintenanceRunImmediatelyOnStart(),
                botProperties.isLeaderCommonBoxEnabled(),
                botProperties.isMemberCommonBoxEnabled(),
                botProperties.isTaskStartupPreparationEnabled(),
                botProperties.isXiuluoSkipBossEnabled(),
                botProperties.isDoubleExperienceClaimEnabled(),
                botProperties.isPlayerHpSupplyEnabled(),
                botProperties.getPlayerHpSupplyThreshold(),
                botProperties.isPlayerMpSupplyEnabled(),
                botProperties.getPlayerMpSupplyThreshold(),
                botProperties.isPetHpSupplyEnabled(),
                botProperties.getPetHpSupplyThreshold(),
                botProperties.isPetMpSupplyEnabled(),
                botProperties.getPetMpSupplyThreshold(),
                botProperties.getLeaderDeathRecoveryMode());
    }

    private void projectRemoteTerminal(
            WindowTaskRunner runner,
            RemoteTaskHandle expectedHandle,
            WindowTurnLoop loop,
            TurnTaskTerminalResult terminal,
            RemoteTerminalRecoveryPlan recoveryPlan) {
        if (runner.getRemoteTaskHandle() != expectedHandle) {
            log.info("Ignore stale remote terminal after task replacement: windowId={} startRequestId={} status={}",
                    runner.getWindowContext().getWindowId(), terminal.startRequestId(), terminal.status());
            return;
        }
        if (terminal.status() == TurnTaskTerminalResult.Status.FAILED
                || terminal.status() == TurnTaskTerminalResult.Status.SKIPPED) {
            String windowId = runner.getWindowContext().getWindowId();
            RemoteTerminalRecoveryPlan remainingPlan = recoveryPlan.remainingFrom(loop.recoverableQueueIndex());
            if (!isWindowLifecycleCurrent(windowId, recoveryPlan.startEpoch())) {
                log.info("Ignore recoverable terminal after explicit cancellation/replacement: windowId={} "
                                + "startRequestId={} status={} epoch={}",
                        windowId, terminal.startRequestId(), terminal.status(), recoveryPlan.startEpoch());
                return;
            }
            String previousRecoveryStartRequestId = remoteTerminalRecoveryPending.put(
                    windowId, terminal.startRequestId());
            if (terminal.startRequestId().equals(previousRecoveryStartRequestId)) {
                log.info("Recoverable Cloud terminal already has a pending restart: windowId={} startRequestId={} "
                                + "status={}",
                        windowId, terminal.startRequestId(), terminal.status());
                return;
            }
            long retryDelayMs = WindowTurnLoop.failureRetryDelayMs(windowId, 1);
            log.warn("Recoverable Cloud terminal retained; restarting exact window task: windowId={} "
                            + "startRequestId={} status={} reason={} recoveryQueue={} retryDelayMs={}",
                    windowId, terminal.startRequestId(), terminal.status(), terminal.reason(),
                    remainingPlan.taskCodes(), retryDelayMs);
            CompletableFuture.delayedExecutor(retryDelayMs, TimeUnit.MILLISECONDS).execute(
                    () -> recoverRemoteTerminal(remainingPlan, terminal, 1));
            return;
        }
        if (terminal.status() == TurnTaskTerminalResult.Status.SUCCESS
                && scheduleNextConfiguredRun(runner, terminal, recoveryPlan)) {
            return;
        }
        runner.markRemoteStopped("Cloud任务终止：" + terminal.status()
                + " (" + terminal.startRequestId() + ")");
    }

    /**
     * SUCCESS is a per-run terminal, not a per-task one: a finite repeatable task configured for
     * N runs (五环 maxRuns=2 does one 环 per Cloud run and returns SUCCESS between them) expects
     * this layer to relaunch until the configured count is spent. The Cloud pushes no run counter
     * to the Client, so the counter lives here: each continuation seeds the pause-resume progress
     * snapshot that {@code startOneRemote} already consumes as {@code taskInitialCompletedRuns}.
     *
     * @return true when a continuation restart was scheduled and the window must stay owned.
     */
    private boolean scheduleNextConfiguredRun(WindowTaskRunner runner,
                                              TurnTaskTerminalResult terminal,
                                              RemoteTerminalRecoveryPlan recoveryPlan) {
        if (recoveryPlan.taskCodes().size() != 1 || recoveryPlan.taskMaxRuns().size() != 1) {
            return false;
        }
        Integer maxRuns = recoveryPlan.taskMaxRuns().get(0);
        if (maxRuns == null || maxRuns <= 1) {
            return false;
        }
        String windowId = recoveryPlan.windowId();
        if (!isWindowLifecycleCurrent(windowId, recoveryPlan.startEpoch())) {
            return false;
        }
        WindowRuntimeContext context = runner.getWindowContext();
        WindowTaskRunProgress paused = context.getPausedTaskRunProgress();
        TaskType primaryType = fromTurnTaskCode(recoveryPlan.taskCodes().get(0));
        int completedSoFar = paused != null
                && paused.getTaskType() == primaryType
                && paused.getTotalRuns() == maxRuns
                ? paused.getCompletedRuns()
                : 0;
        /*
         * 用户拍板（2026-08-20）：云端终止类收束（五环完成故事 STOP_ALL_RUNS/次数用完/冷却）会经
         * WHOLE_TASK_PROGRESS_UPDATE 把实时账本推满（completed=total）。这里必须同时认这本账，
         * 否则"全部做完"的语义跨不过云端边界——五环配 2 次时 00:37 判完成、00:38 本层仍按 1/2
         * 误排第 2 轮并跑去重新接任务（实证）。取两本账较大值，正常单轮成功的排班不受影响。
         */
        WindowTaskRunProgress live = context.getRunningTaskRunProgress();
        if (live != null
                && live.getTaskType() == primaryType
                && live.getTotalRuns() == maxRuns) {
            completedSoFar = Math.max(completedSoFar, live.getCompletedRuns());
        }
        int newCompleted = completedSoFar + 1;
        if (newCompleted >= maxRuns) {
            return false;
        }
        String previousPending = remoteTerminalRecoveryPending.put(windowId, terminal.startRequestId());
        if (terminal.startRequestId().equals(previousPending)) {
            return true;
        }
        context.updateTaskRunProgress(newCompleted, maxRuns);
        context.retainTaskRunProgressForPause();
        long retryDelayMs = WindowTurnLoop.failureRetryDelayMs(windowId, 1);
        log.info("Finite task run completed with remaining runs; scheduling next run: windowId={} task={} "
                        + "completedRuns={} totalRuns={} startRequestId={} retryDelayMs={}",
                windowId, recoveryPlan.taskCodes().get(0), newCompleted, maxRuns,
                terminal.startRequestId(), retryDelayMs);
        CompletableFuture.delayedExecutor(retryDelayMs, TimeUnit.MILLISECONDS).execute(
                () -> recoverRemoteTerminal(recoveryPlan, terminal, 1));
        return true;
    }

    private static final long OWNER_RETURN_RECHECK_MS = 3_000L;

    /**
     * 切号守门：上一个 run 的任务主人不在窗口标题栏里时，扣住自动重启并按固定间隔重查活标题，
     * 主人切回来才放行。只作用于自动重启链（recoverRemoteTerminal 的两个来源：可恢复终态重启、
     * 配置轮数续跑）；手动重新开任务不经过这里。
     *
     * @return true=本次重启已被扣住（已安排下一次重查），调用方直接返回
     */
    private boolean holdRestartUntilTaskOwnerReturns(RemoteTerminalRecoveryPlan recoveryPlan,
                                                     TurnTaskTerminalResult terminal,
                                                     int attempt) {
        String windowId = recoveryPlan.windowId();
        WindowRuntimeContext context = taskManager.getRunner(windowId)
                .map(WindowTaskRunner::getWindowContext).orElse(null);
        /*
         * G143：新任务提交时随 recovery plan 固化的 owner 是本 run 的第一权威，不受随后标题或上下文
         * 清理影响。老计划没有该字段时才走 G142 兜底：当前保留 run 的 taskOwner* 优先，只有当前
         * owner 已被终局真正清掉才使用 lastTaskOwner*，绝不能让更早一轮角色扣住本轮重启。
         */
        String expectedOwnerId = recoveryPlan.taskOwnerPlayerId();
        String expectedOwnerName = recoveryPlan.taskOwnerPlayerName();
        String activeOwnerId = context == null ? null : context.getTaskOwnerPlayerId();
        if (expectedOwnerId == null && context != null) {
            if (activeOwnerId != null) {
                expectedOwnerId = activeOwnerId;
                expectedOwnerName = context.getTaskOwnerPlayerName();
            } else {
                expectedOwnerId = context.getLastTaskOwnerPlayerId();
                expectedOwnerName = context.getLastTaskOwnerPlayerName();
            }
        }
        if (expectedOwnerId == null) {
            return false;
        }
        WindowNativeBinding liveBinding = bindingRefreshService.refreshAndCommit(context).orElse(null);
        String liveTitle = liveBinding == null ? null : liveBinding.getTitle();
        String visibleId = com.bot.dhxy.window.runtime.WindowTitleIdentityParser.parse(liveTitle)
                .map(identity -> identity.playerId() == null ? null : identity.playerId().trim())
                .orElse(null);
        if (expectedOwnerId.equals(visibleId)) {
            return false;
        }
        log.warn("Recoverable restart held: task owner not in window (switch-away contract): windowId={} "
                        + "expectedOwner={}/{} visible={} status={} recheckMs={}",
                windowId, expectedOwnerName, expectedOwnerId, visibleId,
                terminal.status(), OWNER_RETURN_RECHECK_MS);
        CompletableFuture.delayedExecutor(OWNER_RETURN_RECHECK_MS, TimeUnit.MILLISECONDS).execute(
                () -> recoverRemoteTerminal(recoveryPlan, terminal, attempt));
        return true;
    }

    /**
     * Replace a recoverable terminal Cloud run without exposing an abnormal/stopped window between runs.
     *
     * @param recoveryPlan immutable task, role, team and UI-setting snapshot from the accepted run
     * @param terminal terminal being recovered; used only for correlated diagnostics
     * @param attempt consecutive restart attempt starting at one
     */
    private void recoverRemoteTerminal(RemoteTerminalRecoveryPlan recoveryPlan,
                                       TurnTaskTerminalResult terminal,
                                       int attempt) {
        String windowId = recoveryPlan.windowId();
        if (!terminal.startRequestId().equals(remoteTerminalRecoveryPending.get(windowId))) {
            log.info("Skip superseded recoverable-terminal restart: windowId={} startRequestId={} status={}",
                    windowId, terminal.startRequestId(), terminal.status());
            return;
        }
        if (!isWindowLifecycleCurrent(windowId, recoveryPlan.startEpoch())) {
            remoteTerminalRecoveryPending.remove(windowId, terminal.startRequestId());
            log.info("Cancel recoverable-terminal restart after explicit lifecycle change: windowId={} "
                            + "startRequestId={} status={} epoch={}",
                    windowId, terminal.startRequestId(), terminal.status(), recoveryPlan.startEpoch());
            return;
        }
        /*
         * 用户契约(2026-08-18,切号事故):窗口被切到别的角色时,自动重启必须原地等主人切回来,
         * 绝不许把当前角色认作新主人接着跑。逐次重查活标题,直到主人回窗才放行;手动重开不走
         * 这条路,不受影响。
         */
        if (holdRestartUntilTaskOwnerReturns(recoveryPlan, terminal, attempt)) {
            return;
        }
        try {
            TurnModeGuard.RemoteLoopState previous = turnModeGuard.remoteState(windowId);
            if (previous.registered() && !turnModeGuard.awaitAndRemoveStoppedRemote(windowId)) {
                throw new IllegalStateException("terminal remote loop disappeared before exact removal");
            }
            // 2026-08-23 用户契约：崩溃自动重启按停止对待——现实记忆清光（进度照旧保留），
            // 否则毒缓存跟着一轮轮重启循环（3519 案连崩 8 次即此因）。
            // 审查修正：只有 FAILED 才是崩溃；SUCCESS 续跑（队列内下一轮）属于同一生命周期，不清。
            if (terminal.status() == TurnTaskTerminalResult.Status.FAILED) {
                localFreshStartReset.resetWindowRealityMemory(
                        windowId,
                        taskManager.getRunner(windowId)
                                .map(WindowTaskRunner::getWindowContext).orElse(null),
                        "crash-recovery:" + terminal.status());
            }
            WindowTaskCommandDetail restarted = startOneRemote(
                    recoveryPlan.deviceId(), windowId, recoveryPlan.taskCodes(), recoveryPlan.taskMaxRuns(),
                    recoveryPlan.failurePolicy(), recoveryPlan.queue(), recoveryPlan.teamSessionKey(),
                    recoveryPlan.teamPreflight(), recoveryPlan.startEpoch(),
                    recoveryPlan.roleResolutionDeadlineNanos(), TaskStartupMode.PAUSE_RESUME,
                    recoveryPlan.explicitLeaderWindowId(), recoveryPlan.runtimeSettings());
            if (!restarted.isSuccess()) {
                throw new IllegalStateException(restarted.getMessage());
            }
            remoteTerminalRecoveryPending.remove(windowId, terminal.startRequestId());
            log.info("Recoverable Cloud terminal restart submitted: windowId={} previousStartRequestId={} "
                            + "status={} attempt={}",
                    windowId, terminal.startRequestId(), terminal.status(), attempt);
        } catch (RuntimeException recoveryFailure) {
            if (!isWindowLifecycleCurrent(windowId, recoveryPlan.startEpoch())) {
                remoteTerminalRecoveryPending.remove(windowId, terminal.startRequestId());
                return;
            }
            if (!terminal.startRequestId().equals(remoteTerminalRecoveryPending.get(windowId))) {
                return;
            }
            long retryDelayMs = WindowTurnLoop.failureRetryDelayMs(windowId, attempt + 1);
            log.error("Recoverable Cloud terminal restart failed; window remains owned and will retry: "
                            + "windowId={} previousStartRequestId={} status={} attempt={} retryDelayMs={} "
                            + "type={} message={}",
                    windowId, terminal.startRequestId(), terminal.status(), attempt, retryDelayMs,
                    recoveryFailure.getClass().getName(), recoveryFailure.getMessage(), recoveryFailure);
            CompletableFuture.delayedExecutor(retryDelayMs, TimeUnit.MILLISECONDS).execute(
                    () -> recoverRemoteTerminal(recoveryPlan, terminal, attempt + 1));
        }
    }

    public WindowTaskCommandResult pauseRemoteWindows(Collection<String> windowIds) {
        synchronized (remoteStartLifecycleMonitor) {
            // G008 phase 1: pause is an abort boundary, never an in-place remote resume.  The old
            // observer/handle/token must be torn down before a later UI start creates a fresh turn run.
            cancelPendingRemoteStarts("pause", windowIds);
            return abortRemoteRuns(windowIds, "remote turn paused; fresh start required",
                    WindowRuntimeStatus.PAUSED, "远程暂停并清理旧运行完成", false);
        }
    }

    public WindowTaskCommandResult resumeRemoteWindows(Collection<String> windowIds) {
        return WindowTaskCommandResult.empty("暂停后的旧运行已清理；请使用启动创建新的远程运行",
                getSnapshots());
    }

    public WindowTaskCommandResult stopRemoteWindows(Collection<String> windowIds) {
        synchronized (remoteStartLifecycleMonitor) {
            cancelPendingRemoteStarts("stop", windowIds);
            /*
             * 停止是用户手里最后的出路：即使云端没确认终止也必须把本地 loop 摘掉。
             * 2026-08-21 实锤——两个窗口在"启动被拒 + 停止也被拒"里静止 13 分钟，
             * 唯一出路是重启客户端。详见 TurnModeGuard#awaitAndForceRemoveStoppedRemote。
             */
            return abortRemoteRuns(windowIds, "remote turn stopped",
                    WindowRuntimeStatus.STOPPED, "远程停止选中窗口完成", true);
        }
    }

    private WindowTaskCommandResult abortRemoteRuns(Collection<String> windowIds,
                                                     String reason,
                                                     WindowRuntimeStatus lifecycleStatus,
                                                     String summary,
                                                     boolean forceRemoveUnconfirmed) {
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
                if (forceRemoveUnconfirmed) {
                    TurnModeGuard.ForcedRemoval removal =
                            turnModeGuard.awaitAndForceRemoveStoppedRemote(windowId);
                    switch (removal) {
                        case NOT_REGISTERED -> details.add(WindowTaskCommandDetail.success(
                                windowId, "远程 turn loop 已不存在，本地运行边界已清理"));
                        case REMOVED_CONFIRMED -> details.add(WindowTaskCommandDetail.success(
                                windowId, "已停止并移除远程 turn loop"));
                        case REMOVED_UNCONFIRMED -> {
                            // 云端没确认，但停止必须落地：摘掉本地 loop 并留痕，不把用户堵死。
                            log.warn("远程停止：云端未确认终止，按停止语义强制移除本地 turn loop："
                                    + "windowId={}", windowId);
                            details.add(WindowTaskCommandDetail.success(
                                    windowId, "已停止；云端未确认终止，已强制清理本地 turn loop"));
                        }
                    }
                    continue;
                }
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
            case WUHUAN_V3 -> TurnTaskCode.WUHUAN_V3;
            case WUBEI -> TurnTaskCode.WUBEI;
            case XIULUO_V2 -> TurnTaskCode.XIULUO_V2;
            case XINSHOU -> TurnTaskCode.XINSHOU;
            case XINSHOU_TRAINING -> TurnTaskCode.XINSHOU_TRAINING;
            case CATCH_GHOST -> TurnTaskCode.CATCH_GHOST;
            case GHOST_KING -> TurnTaskCode.GHOST_KING;
            case YIPIN_GUARD_TEST -> TurnTaskCode.YIPIN_GUARD_TEST;
            case PATHING_TEST -> TurnTaskCode.PATHING_TEST;
            case DALISI_QUIZ -> TurnTaskCode.DALISI_QUIZ;
            case G056_DOUBLE_EXPERIENCE_ACCEPTANCE -> TurnTaskCode.G056_DOUBLE_EXPERIENCE_ACCEPTANCE;
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
            // 五环 V2 已下线：协议码保留（共享协议不动），客户端不再有对应任务类型。
            case WUHUAN_V2 -> TaskType.UNKNOWN;
            case WUHUAN_V3 -> TaskType.WUHUAN_V3;
            case WUBEI -> TaskType.WUBEI;
            case XIULUO_V2 -> TaskType.XIULUO_V2;
            case XINSHOU -> TaskType.XINSHOU;
            case XINSHOU_TRAINING -> TaskType.XINSHOU_TRAINING;
            case CATCH_GHOST -> TaskType.CATCH_GHOST;
            case GHOST_KING -> TaskType.GHOST_KING;
            case YIPIN_GUARD_TEST -> TaskType.YIPIN_GUARD_TEST;
            case PATHING_TEST -> TaskType.PATHING_TEST;
            case DALISI_QUIZ -> TaskType.DALISI_QUIZ;
            case G056_DOUBLE_EXPERIENCE_ACCEPTANCE -> TaskType.G056_DOUBLE_EXPERIENCE_ACCEPTANCE;
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
            case GHOST_KING -> botProperties.getGhostKingMaxRuns();
            case YIPIN_GUARD_TEST -> 1;
            case PATHING_TEST -> 1;
            case DALISI_QUIZ -> 1;
            case G056_DOUBLE_EXPERIENCE_ACCEPTANCE -> 1;
            case WUHUAN_V2 -> botProperties.getWuhuanMaxRuns();
            case WUHUAN_V3 -> botProperties.getWuhuanMaxRuns();
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
            String windowId = runner.getWindowContext().getWindowId();
            if (remoteTerminalRecoveryPending.containsKey(windowId)) {
                continue;
            }
            TurnModeGuard.RemoteLoopState state = turnModeGuard.remoteState(windowId);
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
        retained = localTeamRolePreflightService.attachConfirmedLeaderAnchorGroups(
                contexts, retained, () -> Thread.currentThread().isInterrupted());
        retained.values().stream()
                .filter(preflight -> preflight.groupHash() != null)
                .collect(java.util.stream.Collectors.groupingBy(
                        LocalTeamRolePreflightService.Preflight::groupHash))
                .forEach((groupHash, group) -> {
                    long leaders = group.stream()
                            .filter(preflight -> preflight.role() == LocalTeamRolePreflightService.Role.LEADER)
                            .count();
                    if (leaders != 1L) {
                        throw new IllegalStateException(
                                "暂停热恢复同队分组不自洽：groupHash=" + groupHash + " leaders=" + leaders);
                    }
                });
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
            retained.put(windowId, new LocalTeamRolePreflightService.Preflight(
                    windowId, role, null, false, null));
        }
        return Map.copyOf(retained);
    }

    private static boolean requiresUniqueLeader(TaskType taskType) {
        return switch (taskType) {
            case WUBEI, XIULUO_V2, XINSHOU_TRAINING, CATCH_GHOST, GHOST_KING, TIANTING -> true;
            default -> false;
        };
    }

    /** Accept a pause counter only for the same task and unchanged finite total. */
    static WindowTaskRunProgress resolvePauseResumeProgress(
            TaskStartupMode startupMode,
            List<TurnTaskCode> taskCodes,
            List<Integer> taskMaxRuns,
            WindowTaskRunProgress pausedProgress) {
        if (startupMode != TaskStartupMode.PAUSE_RESUME || pausedProgress == null
                || taskCodes == null || taskMaxRuns == null || taskCodes.size() != taskMaxRuns.size()) {
            return null;
        }
        for (int index = 0; index < taskCodes.size(); index++) {
            Integer maxRuns = taskMaxRuns.get(index);
            if (maxRuns != null && maxRuns > 0
                    && pausedProgress.getTaskType() == fromTurnTaskCode(taskCodes.get(index))
                    && pausedProgress.getTotalRuns() == maxRuns
                    && pausedProgress.getCompletedRuns() <= maxRuns) {
                return pausedProgress;
            }
        }
        return null;
    }

    /** Build the queue-aligned initial counter list; one pause snapshot may seed only one queue element. */
    static List<Integer> toTaskInitialCompletedRuns(
            List<TurnTaskCode> taskCodes, WindowTaskRunProgress resumeProgress) {
        List<Integer> initialCompletedRuns = new ArrayList<>(taskCodes.size());
        boolean applied = false;
        for (TurnTaskCode taskCode : taskCodes) {
            boolean matches = !applied && resumeProgress != null
                    && resumeProgress.getTaskType() == fromTurnTaskCode(taskCode);
            initialCompletedRuns.add(matches ? resumeProgress.getCompletedRuns() : 0);
            applied |= matches;
        }
        return List.copyOf(initialCompletedRuns);
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
        List<WindowRuntimeContext> allContexts = new ArrayList<>();
        for (String windowId : windowIds) {
            TaskType taskType = taskManager.getSnapshot(windowId)
                    .map(WindowTaskSnapshot::getSelectedTaskType)
                    .orElse(TaskType.UNKNOWN);
            LocalTeamRolePreflightService.Role fixedRole = switch (taskType) {
                case AUTO_BATTLE -> LocalTeamRolePreflightService.Role.MEMBER;
                case WUHUAN_V3, XINSHOU, PATHING_TEST, DALISI_QUIZ -> LocalTeamRolePreflightService.Role.SOLO;
                default -> null;
            };
            WindowRuntimeContext context = taskManager.getRunner(windowId)
                    .map(WindowTaskRunner::getWindowContext)
                    .orElse(null);
            if (context != null) {
                allContexts.add(context);
            }
            if (fixedRole != null) {
                knownRoles.put(windowId, new LocalTeamRolePreflightService.Preflight(
                        windowId, fixedRole, null, false, null));
                log.info("skip local team-role panel probe for fixed task role: windowId={} taskType={} role={}",
                        windowId, taskType, fixedRole);
            } else if (context != null) {
                contexts.add(context);
            }
        }
        try {
            knownRoles.putAll(localTeamRolePreflightService.prepareBatch(
                    contexts, teamSessionKey, () -> isRemoteStartCancelled(startEpoch), roleResolutionDeadlineNanos));
            return localTeamRolePreflightService.attachConfirmedLeaderAnchorGroups(
                    allContexts, knownRoles, () -> isRemoteStartCancelled(startEpoch));
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

    /*
     * 2026-08-22 用户批准修正(第二次咬人):生命周期作废原来是**全局** epoch——停任意一个窗口就把
     * 所有窗口飞行中的自动恢复/续跑全部判作废。实证两起:08-21 停/恢复 3465 吞掉了 3473 的第 2 轮
     * (scheduleNextConfiguredRun epoch 不等→false);08-22 10:42 停 3465 吞掉了 3511 的崩溃自动重启
     * (Ignore recoverable terminal ... epoch=1,全局已 2)。改为**按窗口**作废:全局 AtomicLong 只当
     * 单调发号器 + 批量启动过程的在飞守卫;每窗口一个 epoch 下限(floor),恢复/续跑/ACK 只比对
     * 自己窗口的 floor。停止/暂停只抬所选窗口的 floor;新启动某窗口时抬该窗口 floor(替换旧 run)。
     */
    private void cancelPendingRemoteStarts(String reason, Collection<String> windowIds) {
        long cancelledEpoch = remoteStartEpoch.incrementAndGet();
        List<String> ids = normalizeWindowIds(windowIds);
        for (String windowId : ids) {
            remoteWindowEpochFloor.merge(windowId, cancelledEpoch, Math::max);
            remoteTerminalRecoveryPending.remove(windowId);
        }
        log.warn("Remote start lifecycle invalidated: reason={} epoch={} windows={}",
                reason, cancelledEpoch, ids);
    }

    /** @return true while this window's exact start-epoch is still the newest lifecycle it has seen. */
    private boolean isWindowLifecycleCurrent(String windowId, long startEpoch) {
        return startEpoch > 0L
                && startEpoch >= remoteWindowEpochFloor.getOrDefault(windowId, 0L);
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

    /** Immutable authority and UI-setting snapshot used only to replace a recoverable Cloud terminal run. */
    private record RemoteTerminalRecoveryPlan(
            String deviceId,
            String windowId,
            List<TurnTaskCode> taskCodes,
            List<Integer> taskMaxRuns,
            TurnTaskQueueFailurePolicy failurePolicy,
            WindowTaskQueue queue,
            String teamSessionKey,
            LocalTeamRolePreflightService.Preflight teamPreflight,
            long startEpoch,
            long roleResolutionDeadlineNanos,
            String explicitLeaderWindowId,
            TurnTaskRuntimeSettings runtimeSettings,
            String taskOwnerPlayerId,
            String taskOwnerPlayerName) {

        /**
         * Returns the monotonic queue suffix beginning at the exact failed child. Successful predecessors are
         * permanently removed from the recovery submission, so a later task failure can never replay them.
         */
        private RemoteTerminalRecoveryPlan remainingFrom(int queueIndex) {
            if (queueIndex <= 0) {
                return this;
            }
            if (queueIndex >= taskCodes.size()
                    || taskMaxRuns.size() != taskCodes.size()) {
                throw new IllegalStateException("invalid remote recovery queue checkpoint: index=" + queueIndex
                        + " taskCodes=" + taskCodes.size() + " maxRuns=" + taskMaxRuns.size()
                        + " queue=" + queue.getTaskTypes().size());
            }
            List<TurnTaskCode> remainingTaskCodes = List.copyOf(taskCodes.subList(queueIndex, taskCodes.size()));
            List<Integer> remainingTaskMaxRuns = List.copyOf(
                    taskMaxRuns.subList(queueIndex, taskMaxRuns.size()));
            WindowTaskQueue remainingQueue = new WindowTaskQueue(
                    remainingTaskCodes.stream()
                            .map(WindowTaskControlService::fromTurnTaskCode)
                            .toList(),
                    queue.getFailurePolicy());
            return new RemoteTerminalRecoveryPlan(
                    deviceId, windowId, remainingTaskCodes, remainingTaskMaxRuns, failurePolicy, remainingQueue,
                    teamSessionKey, teamPreflight, startEpoch, roleResolutionDeadlineNanos,
                    explicitLeaderWindowId, runtimeSettings, taskOwnerPlayerId, taskOwnerPlayerName);
        }
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
