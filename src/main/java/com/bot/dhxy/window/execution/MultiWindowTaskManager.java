package com.bot.dhxy.window.execution;

import com.bot.dhxy.task.TaskFactory;
import com.bot.dhxy.task.model.TaskType;
import com.bot.dhxy.window.policy.WindowCapacityPolicy;
import com.bot.dhxy.window.runtime.WindowRegistrationRequest;
import com.bot.dhxy.window.runtime.WindowRuntimeContext;
import com.bot.dhxy.window.runtime.WindowRuntimeContextFactory;
import com.bot.dhxy.window.runtime.WindowTaskContextHolder;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class MultiWindowTaskManager {

    private final TaskFactory taskFactory;
    private final WindowRuntimeContextFactory windowRuntimeContextFactory;
    private final WindowCapacityPolicy windowCapacityPolicy;
    private final WindowTaskContextHolder windowTaskContextHolder;
    private final Map<String, WindowTaskRunner> runnersByWindowId = new ConcurrentHashMap<>();

    public MultiWindowTaskManager(TaskFactory taskFactory,
                                  WindowRuntimeContextFactory windowRuntimeContextFactory,
                                  WindowCapacityPolicy windowCapacityPolicy,
                                  WindowTaskContextHolder windowTaskContextHolder) {
        this.taskFactory = taskFactory;
        this.windowRuntimeContextFactory = windowRuntimeContextFactory;
        this.windowCapacityPolicy = windowCapacityPolicy;
        this.windowTaskContextHolder = windowTaskContextHolder;
    }

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
            return new WindowTaskRunner(windowContext, taskFactory, windowTaskContextHolder);
        });
    }

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
        return runnersByWindowId.computeIfAbsent(windowId, ignored -> new WindowTaskRunner(windowContext, taskFactory, windowTaskContextHolder));
    }

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

    public boolean submit(String windowId, TaskType taskType) {
        return submitWithResult(windowId, taskType).isSuccess();
    }

    public WindowTaskSubmitResult submitWithResult(String windowId, TaskType taskType) {
        String normalizedWindowId = normalizeWindowId(windowId);
        if (normalizedWindowId == null) {
            return WindowTaskSubmitResult.failed(windowId, taskType, "窗口ID为空");
        }
        if (taskType == null || taskType == TaskType.UNKNOWN) {
            return WindowTaskSubmitResult.failed(normalizedWindowId, taskType, "任务类型无效");
        }
        WindowTaskRunner runner = runnersByWindowId.get(normalizedWindowId);
        if (runner == null) {
            return WindowTaskSubmitResult.failed(normalizedWindowId, taskType, "窗口不存在或尚未注册");
        }
        if (runner.isShutdown()) {
            return WindowTaskSubmitResult.failed(normalizedWindowId, taskType, "窗口执行器已关闭");
        }
        if (runner.isRunning()) {
            return WindowTaskSubmitResult.failed(normalizedWindowId, taskType, "窗口已有任务正在运行");
        }
        boolean submitted = runner.submit(taskType);
        if (!submitted) {
            return WindowTaskSubmitResult.failed(normalizedWindowId, taskType, "任务提交失败，可能无法创建任务或执行器拒绝提交");
        }
        return WindowTaskSubmitResult.success(normalizedWindowId, taskType, "任务已提交到窗口执行器");
    }

    public boolean submitSelectedTask(String windowId) {
        return submitSelectedTaskWithResult(windowId).isSuccess();
    }

    public WindowTaskSubmitResult submitSelectedTaskWithResult(String windowId) {
        String normalizedWindowId = normalizeWindowId(windowId);
        if (normalizedWindowId == null) {
            return WindowTaskSubmitResult.failed(windowId, TaskType.UNKNOWN, "窗口ID为空");
        }
        WindowTaskRunner runner = runnersByWindowId.get(normalizedWindowId);
        if (runner == null) {
            return WindowTaskSubmitResult.failed(normalizedWindowId, TaskType.UNKNOWN, "窗口不存在或尚未注册");
        }
        TaskType selectedTaskType = runner.getWindowContext().getSelectedTaskType();
        if (selectedTaskType == null || selectedTaskType == TaskType.UNKNOWN) {
            return WindowTaskSubmitResult.failed(normalizedWindowId, selectedTaskType, "窗口未选择有效任务");
        }
        return submitWithResult(normalizedWindowId, selectedTaskType);
    }

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

    public void stop(String windowId) {
        WindowTaskRunner runner = runnersByWindowId.get(normalizeWindowId(windowId));
        if (runner != null) {
            runner.stopCurrentTask();
        }
    }

    public void stopAll() {
        runnersByWindowId.values().forEach(WindowTaskRunner::stopCurrentTask);
    }

    public Optional<WindowTaskRunner> getRunner(String windowId) {
        return Optional.ofNullable(runnersByWindowId.get(normalizeWindowId(windowId)));
    }

    public Optional<WindowTaskSnapshot> getSnapshot(String windowId) {
        return getRunner(windowId).map(WindowTaskRunner::snapshot);
    }

    public List<WindowTaskSnapshot> getAllSnapshots() {
        return runnersByWindowId.values().stream()
                .map(WindowTaskRunner::snapshot)
                .sorted(Comparator.comparing(WindowTaskSnapshot::getWindowId, Comparator.nullsLast(String::compareTo)))
                .toList();
    }

    public Collection<WindowTaskRunner> getAllRunners() {
        return Collections.unmodifiableCollection(runnersByWindowId.values());
    }

    public int getRegisteredWindowCount() {
        return runnersByWindowId.size();
    }

    public int getRunningWindowCount() {
        return (int) runnersByWindowId.values().stream()
                .filter(WindowTaskRunner::isRunning)
                .count();
    }

    public boolean hasRunningTasks() {
        return getRunningWindowCount() > 0;
    }

    public int getMaxWindowCount() {
        return windowCapacityPolicy.getMaxWindowCount();
    }

    public int getRemainingWindowCapacity() {
        return windowCapacityPolicy.remainingCapacity(runnersByWindowId.size());
    }

    public void unregisterWindow(String windowId) {
        WindowTaskRunner runner = runnersByWindowId.remove(normalizeWindowId(windowId));
        if (runner != null) {
            runner.shutdownNow();
        }
    }

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
