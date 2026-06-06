package com.bot.dhxy.window.execution;

import com.bot.dhxy.input.InputSequences;
import com.bot.dhxy.metrics.AutomationMetricsService;
import com.bot.dhxy.runner.context.TaskExecutionContextHolder;
import com.bot.dhxy.service.AutoCombatService;
import com.bot.dhxy.service.DialogService;
import com.bot.dhxy.task.TaskFactory;
import com.bot.dhxy.task.model.TaskType;
import com.bot.dhxy.task.startup.TaskTeamAssignmentPolicy;
import com.bot.dhxy.team.TeamRoleDetectionService;
import com.bot.dhxy.window.model.WindowNativeBinding;
import com.bot.dhxy.window.policy.WindowCapacityPolicy;
import com.bot.dhxy.window.runtime.WindowHandleParser;
import com.bot.dhxy.window.runtime.WindowNativeBindingRefreshService;
import com.bot.dhxy.window.runtime.WindowRegistrationRequest;
import com.bot.dhxy.window.runtime.WindowRuntimeContext;
import com.bot.dhxy.window.runtime.WindowRuntimeContextFactory;
import com.bot.dhxy.window.runtime.WindowTaskContextHolder;
import com.bot.dhxy.vision.MiniMapCoordinateReader;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry and control facade for all registered game-window task runners.
 *
 * <p>The manager owns the mapping from logical window id to {@link WindowTaskRunner}. It validates
 * native bindings before task submission, refreshes geometry for snapshots, and fans out stop/pause/
 * resume operations. It does not execute task logic itself; each window runner owns its own executor
 * and window context binding.</p>
 */
@Component
public class MultiWindowTaskManager {

    private final TaskFactory taskFactory;
    private final WindowRuntimeContextFactory windowRuntimeContextFactory;
    private final WindowCapacityPolicy windowCapacityPolicy;
    private final WindowTaskContextHolder windowTaskContextHolder;
    private final WindowTaskStartupInitializer startupInitializer;
    private final TaskExecutionContextHolder taskExecutionContextHolder;
    private final InputSequences inputSequences;
    private final TeamRoleDetectionService teamRoleDetectionService;
    private final TaskTeamAssignmentPolicy taskTeamAssignmentPolicy;
    private final WindowNativeBindingRefreshService bindingRefreshService;
    private final AutomationMetricsService automationMetricsService;
    private final AutoCombatService autoCombatService;
    private final MiniMapCoordinateReader miniMapCoordinateReader;
    private final DialogService dialogService;
    private final Map<String, WindowTaskRunner> runnersByWindowId = new ConcurrentHashMap<>();

    /**
     * Create the multi-window task manager.
     *
     * @param taskFactory task factory shared by all window runners.
     * @param windowRuntimeContextFactory creates runtime contexts from registration requests.
     * @param windowCapacityPolicy max-window registration policy.
     * @param windowTaskContextHolder thread-local window context holder passed to runners.
     * @param startupInitializer per-task startup initializer.
     * @param taskExecutionContextHolder task execution context holder.
     * @param inputSequences serialized input API.
     * @param teamRoleDetectionService live team-role detector.
     * @param taskTeamAssignmentPolicy role-based task reassignment policy.
     * @param bindingRefreshService native binding geometry refresh service.
     * @param automationMetricsService local business metrics sink for runner lifecycle events.
     * @param autoCombatService shared combat guard used by window runners.
     * @param miniMapCoordinateReader lightweight mini-map location reader used by runner watchers.
     * @param dialogService dialog detector used by runner watchers for prepare-only matching.
     */
    public MultiWindowTaskManager(TaskFactory taskFactory,
                                  WindowRuntimeContextFactory windowRuntimeContextFactory,
                                  WindowCapacityPolicy windowCapacityPolicy,
                                  WindowTaskContextHolder windowTaskContextHolder,
                                  WindowTaskStartupInitializer startupInitializer,
                                  TaskExecutionContextHolder taskExecutionContextHolder,
                                  InputSequences inputSequences,
                                  TeamRoleDetectionService teamRoleDetectionService,
                                  TaskTeamAssignmentPolicy taskTeamAssignmentPolicy,
                                  WindowNativeBindingRefreshService bindingRefreshService,
                                  AutomationMetricsService automationMetricsService,
                                  AutoCombatService autoCombatService,
                                  MiniMapCoordinateReader miniMapCoordinateReader,
                                  DialogService dialogService) {
        this.taskFactory = taskFactory;
        this.windowRuntimeContextFactory = windowRuntimeContextFactory;
        this.windowCapacityPolicy = windowCapacityPolicy;
        this.windowTaskContextHolder = windowTaskContextHolder;
        this.startupInitializer = startupInitializer;
        this.taskExecutionContextHolder = taskExecutionContextHolder;
        this.inputSequences = inputSequences;
        this.teamRoleDetectionService = teamRoleDetectionService;
        this.taskTeamAssignmentPolicy = taskTeamAssignmentPolicy;
        this.bindingRefreshService = bindingRefreshService;
        this.automationMetricsService = automationMetricsService;
        this.autoCombatService = autoCombatService;
        this.miniMapCoordinateReader = miniMapCoordinateReader;
        this.dialogService = dialogService;
    }

    /**
     * Register or refresh one window from a registration request.
     *
     * @param request validated request containing window id, role/task metadata, and optional native binding.
     * @return existing or newly-created runner, or null when capacity policy rejects a new window.
     */
    public WindowTaskRunner registerWindow(WindowRegistrationRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("window registration request must not be null");
        }
        request.requireValid();
        return runnersByWindowId.compute(request.getWindowId(), (windowId, existingRunner) -> {
            if (existingRunner != null) {
                existingRunner.refreshRegistration(request);
                return existingRunner;
            }
            if (!windowCapacityPolicy.canRegister(runnersByWindowId.size())) {
                return null;
            }
            WindowRuntimeContext windowContext = windowRuntimeContextFactory.create(request);
            return new WindowTaskRunner(windowContext, taskFactory, windowTaskContextHolder, startupInitializer,
                    taskExecutionContextHolder, inputSequences, teamRoleDetectionService, taskTeamAssignmentPolicy,
                    automationMetricsService, autoCombatService, miniMapCoordinateReader, dialogService);
        });
    }

    /**
     * Register an already-built runtime context.
     *
     * @param windowContext runtime context to own.
     * @return existing or newly-created runner, or null when capacity is full.
     */
    public WindowTaskRunner registerWindow(WindowRuntimeContext windowContext) {
        if (windowContext == null) {
            throw new IllegalArgumentException("window context must not be null");
        }
        String windowId = windowContext.getWindowId();
        WindowTaskRunner existing = runnersByWindowId.get(windowId);
        if (existing != null) {
            return existing;
        }
        if (!windowCapacityPolicy.canRegister(runnersByWindowId.size())) {
            return null;
        }
        return runnersByWindowId.computeIfAbsent(windowId,
                ignored -> new WindowTaskRunner(windowContext, taskFactory, windowTaskContextHolder, startupInitializer,
                        taskExecutionContextHolder, inputSequences, teamRoleDetectionService, taskTeamAssignmentPolicy,
                        automationMetricsService, autoCombatService, miniMapCoordinateReader, dialogService));
    }

    /**
     * Register many windows.
     *
     * @param requests registration requests; invalid/null entries are skipped.
     * @return number of requests that ended with a runner.
     */
    public int registerWindows(Collection<WindowRegistrationRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            return 0;
        }
        int registered = 0;
        for (WindowRegistrationRequest request : requests) {
            if (request != null && request.hasWindowId() && registerWindow(request) != null) {
                registered++;
            }
        }
        return registered;
    }

    /**
     * Submit one task and return only success/failure.
     */
    public boolean submit(String windowId, TaskType taskType) {
        return submitWithResult(windowId, taskType).isSuccess();
    }

    /**
     * Submit one task and return a detailed status object.
     */
    public WindowTaskSubmitResult submitWithResult(String windowId, TaskType taskType) {
        return submitQueueWithResult(windowId, WindowTaskQueue.single(taskType));
    }

    public WindowTaskSubmitResult submitWithResult(String windowId,
                                                   TaskType taskType,
                                                   WindowTaskFailurePolicy failurePolicy) {
        return submitQueueWithResult(windowId, WindowTaskQueue.single(taskType).withFailurePolicy(failurePolicy));
    }

    /**
     * Submit a task queue after validating window id, runner state, and native binding liveness.
     *
     * @param windowId logical registered window id.
     * @param queue task queue to run.
     * @return detailed submit result for UI/logging.
     */
    public WindowTaskSubmitResult submitQueueWithResult(String windowId, WindowTaskQueue queue) {
        String normalizedWindowId = normalizeWindowId(windowId);
        WindowTaskQueue safeQueue = queue == null ? WindowTaskQueue.empty() : queue;
        if (normalizedWindowId == null) {
            return WindowTaskSubmitResult.failed(windowId, safeQueue, WindowTaskSubmitStatus.INVALID_WINDOW_ID, "窗口ID为空");
        }

        if (safeQueue.isEmpty()) {
            return WindowTaskSubmitResult.failed(normalizedWindowId, safeQueue, WindowTaskSubmitStatus.INVALID_QUEUE, "任务队列无效");
        }

        WindowTaskRunner runner = runnersByWindowId.get(normalizedWindowId);
        if (runner == null) {
            return WindowTaskSubmitResult.failed(normalizedWindowId, safeQueue, WindowTaskSubmitStatus.WINDOW_NOT_REGISTERED, "窗口不存在或尚未注册");
        }
        if (runner.isShutdown()) {
            return WindowTaskSubmitResult.failed(normalizedWindowId, safeQueue, WindowTaskSubmitStatus.RUNNER_CLOSED, "窗口执行器已关闭");
        }
        if (!hasLiveNativeBinding(runner)) {
            return WindowTaskSubmitResult.failed(normalizedWindowId, safeQueue, WindowTaskSubmitStatus.STALE_NATIVE_BINDING,
                    "stale native window binding; please rescan game windows");
        }
        if (!runner.canAcceptTaskQueue()) {
            return WindowTaskSubmitResult.failed(normalizedWindowId, safeQueue, WindowTaskSubmitStatus.WINDOW_BUSY, "窗口已有任务正在运行");
        }
        boolean submitted = runner.submit(safeQueue);
        if (!submitted) {
            return WindowTaskSubmitResult.failed(normalizedWindowId, safeQueue, WindowTaskSubmitStatus.SUBMIT_REJECTED, "任务队列提交失败");
        }
        return WindowTaskSubmitResult.success(normalizedWindowId, safeQueue,
                "任务队列已提交到窗口执行器 " + safeQueue.toDisplayText());
    }

    private boolean hasLiveNativeBinding(WindowTaskRunner runner) {
        return refreshLiveNativeBinding(runner);
    }

    private boolean refreshLiveNativeBinding(WindowTaskRunner runner) {
        if (runner == null || runner.getWindowContext() == null) {
            return false;
        }
        WindowNativeBinding binding = runner.getWindowContext().getNativeBinding();
        if (binding == null || !binding.hasNativeHandle()) {
            return true;
        }
        if (WindowHandleParser.parseHandle(binding.getNativeHandle()) == null) {
            return false;
        }
        Optional<WindowNativeBinding> refreshed = bindingRefreshService.refreshGeometry(binding);
        if (refreshed.isEmpty()) {
            return false;
        }
        WindowNativeBinding liveBinding = refreshed.get();
        if (!binding.hasSameGeometry(liveBinding)) {
            runner.getWindowContext().setNativeBinding(liveBinding);
        }
        return true;
    }

    /**
     * Submit the task currently selected on a window.
     *
     * @param windowId logical registered window id.
     * @return true when submission is accepted.
     */
    public boolean submitSelectedTask(String windowId) {
        return submitSelectedTaskWithResult(windowId).isSuccess();
    }

    /**
     * Submit the selected task and return a detailed status object.
     */
    public WindowTaskSubmitResult submitSelectedTaskWithResult(String windowId) {
        String normalizedWindowId = normalizeWindowId(windowId);
        if (normalizedWindowId == null) {
            return WindowTaskSubmitResult.failed(windowId, WindowTaskQueue.empty(), WindowTaskSubmitStatus.INVALID_WINDOW_ID, "窗口ID为空");
        }
        WindowTaskRunner runner = runnersByWindowId.get(normalizedWindowId);
        if (runner == null) {
            return WindowTaskSubmitResult.failed(normalizedWindowId, WindowTaskQueue.empty(), WindowTaskSubmitStatus.WINDOW_NOT_REGISTERED, "窗口不存在或尚未注册");
        }
        TaskType selectedTaskType = runner.getWindowContext().getSelectedTaskType();
        if (selectedTaskType == null || selectedTaskType == TaskType.UNKNOWN) {
            return WindowTaskSubmitResult.failed(normalizedWindowId, WindowTaskQueue.single(selectedTaskType), WindowTaskSubmitStatus.INVALID_QUEUE, "窗口未选择有效任务");
        }
        return submitWithResult(normalizedWindowId, selectedTaskType);
    }

    /**
     * Submit each window's selected task.
     *
     * @param windowIds window ids to submit.
     * @return number of accepted submissions.
     */
    public int submitSelectedTasks(Collection<String> windowIds) {
        if (windowIds == null || windowIds.isEmpty()) {
            return 0;
        }
        int accepted = 0;
        for (String windowId : windowIds) {
            if (submitSelectedTask(windowId)) {
                accepted++;
            }
        }
        return accepted;
    }

    /**
     * Submit the same task to many windows.
     *
     * @return number of accepted submissions.
     */
    public int submit(Collection<String> windowIds, TaskType taskType) {
        if (windowIds == null || windowIds.isEmpty()) {
            return 0;
        }

        int accepted = 0;
        for (String windowId : windowIds) {
            if (submit(windowId, taskType)) {
                accepted++;
            }
        }
        return accepted;
    }

    /**
     * Stop one registered window's current task queue.
     *
     * @return true when a runner existed and had a live/terminal task state to stop or clear.
     */
    public boolean stop(String windowId) {
        WindowTaskRunner runner = runnersByWindowId.get(normalizeWindowId(windowId));
        return runner != null && runner.stopCurrentTask();
    }

    /**
     * Stop all registered windows.
     *
     * @return number of runners that accepted a stop request.
     */
    public int stopAll() {
        int accepted = 0;
        for (WindowTaskRunner runner : runnersByWindowId.values()) {
            if (runner.stopCurrentTask()) {
                accepted++;
            }
        }
        return accepted;
    }

    /**
     * Request pause for one window.
     *
     * @return true when a live task accepted the pause request.
     */
    public boolean pause(String windowId) {
        WindowTaskRunner runner = runnersByWindowId.get(normalizeWindowId(windowId));
        return runner != null && runner.pauseCurrentTask();
    }

    /**
     * Request pause for all live windows.
     *
     * @return number of runners that accepted pause.
     */
    public int pauseAll() {
        int accepted = 0;
        for (WindowTaskRunner runner : runnersByWindowId.values()) {
            if (runner.pauseCurrentTask()) {
                accepted++;
            }
        }
        return accepted;
    }

    /**
     * Resume one paused window.
     *
     * @return true when a live task existed.
     */
    public boolean resume(String windowId) {
        WindowTaskRunner runner = runnersByWindowId.get(normalizeWindowId(windowId));
        return runner != null && runner.resumeCurrentTask();
    }

    /**
     * Resume all paused/live windows.
     *
     * @return number of runners that accepted resume.
     */
    public int resumeAll() {
        int accepted = 0;
        for (WindowTaskRunner runner : runnersByWindowId.values()) {
            if (runner.resumeCurrentTask()) {
                accepted++;
            }
        }
        return accepted;
    }

    /**
     * @param windowId logical registered window id.
     * @return runner if the window is registered.
     */
    public Optional<WindowTaskRunner> getRunner(String windowId) {
        return Optional.ofNullable(runnersByWindowId.get(normalizeWindowId(windowId)));
    }

    /**
     * Refresh native binding geometry and return one window snapshot.
     */
    public Optional<WindowTaskSnapshot> getSnapshot(String windowId) {
        return getRunner(windowId).map(runner -> {
            refreshLiveNativeBinding(runner);
            return runner.snapshot();
        });
    }

    /**
     * @return snapshots for all registered windows, sorted by window id after refreshing live geometry.
     */
    public List<WindowTaskSnapshot> getAllSnapshots() {
        return runnersByWindowId.values().stream()
                .map(runner -> {
                    refreshLiveNativeBinding(runner);
                    return runner.snapshot();
                })
                .sorted(Comparator.comparing(WindowTaskSnapshot::getWindowId, Comparator.nullsLast(String::compareTo)))
                .toList();
    }

    /**
     * @return unmodifiable view of registered runners.
     */
    public Collection<WindowTaskRunner> getAllRunners() {
        return Collections.unmodifiableCollection(runnersByWindowId.values());
    }

    /** @return number of registered windows. */
    public int getRegisteredWindowCount() {
        return runnersByWindowId.size();
    }

    /** @return number of windows with active task queues. */
    public int getRunningWindowCount() {
        return (int) runnersByWindowId.values().stream()
                .filter(WindowTaskRunner::isRunning)
                .count();
    }

    /** @return true when at least one registered window is running. */
    public boolean hasRunningTasks() {
        return getRunningWindowCount() > 0;
    }

    /** @return configured maximum number of registered windows. */
    public int getMaxWindowCount() {
        return windowCapacityPolicy.getMaxWindowCount();
    }

    /** @return remaining registration capacity. */
    public int getRemainingWindowCapacity() {
        return windowCapacityPolicy.remainingCapacity(runnersByWindowId.size());
    }

    /**
     * Unregister one window and shut down its runner.
     */
    public void unregisterWindow(String windowId) {
        WindowTaskRunner runner = runnersByWindowId.remove(normalizeWindowId(windowId));
        if (runner != null) {
            runner.shutdownNow();
        }
    }

    /**
     * Unregister all windows and shut down every runner.
     */
    public void unregisterAll() {
        runnersByWindowId.keySet().forEach(this::unregisterWindow);
    }

    private String normalizeWindowId(String windowId) {
        if (windowId == null) {
            return null;
        }
        String trimmed = windowId.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
