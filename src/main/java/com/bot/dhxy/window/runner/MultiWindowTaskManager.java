package com.bot.dhxy.window.runner;

import com.bot.dhxy.task.TaskFactory;
import com.bot.dhxy.task.model.TaskType;
import com.bot.dhxy.window.policy.WindowCapacityPolicy;
import com.bot.dhxy.window.runtime.WindowRegistrationRequest;
import com.bot.dhxy.window.runtime.WindowRuntimeContext;
import com.bot.dhxy.window.runtime.WindowRuntimeContextFactory;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 多窗口任务管理器。
 *
 * 每个独立游戏窗口对应一个 WindowTaskRunner。
 * 窗口内部串行执行；窗口之间可以并行执行。
 */
@Component
public class MultiWindowTaskManager {

    private final TaskFactory taskFactory;
    private final WindowRuntimeContextFactory windowRuntimeContextFactory;
    private final WindowCapacityPolicy windowCapacityPolicy;
    private final Map<String, WindowTaskRunner> runnersByWindowId = new ConcurrentHashMap<>();

    public MultiWindowTaskManager(TaskFactory taskFactory,
                                  WindowRuntimeContextFactory windowRuntimeContextFactory,
                                  WindowCapacityPolicy windowCapacityPolicy) {
        this.taskFactory = taskFactory;
        this.windowRuntimeContextFactory = windowRuntimeContextFactory;
        this.windowCapacityPolicy = windowCapacityPolicy;
    }

    public WindowTaskRunner registerWindow(WindowRegistrationRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("window registration request must not be null");
        }
        return runnersByWindowId.compute(request.getWindowId(), (windowId, existingRunner) -> {
            if (existingRunner != null) {
                existingRunner.refreshRegistration(request);
                return existingRunner;
            }
            if (!windowCapacityPolicy.canRegister(runnersByWindowId.size())) {
                return null;
            }
            WindowRuntimeContext windowContext = windowRuntimeContextFactory.create(request);
            return new WindowTaskRunner(windowContext, taskFactory);
        });
    }

    public WindowTaskRunner registerWindow(WindowRuntimeContext windowContext) {
        if (runnersByWindowId.containsKey(windowContext.getWindowId())) {
            return runnersByWindowId.get(windowContext.getWindowId());
        }
        if (!windowCapacityPolicy.canRegister(runnersByWindowId.size())) {
            return null;
        }
        return runnersByWindowId.computeIfAbsent(
                windowContext.getWindowId(),
                ignored -> new WindowTaskRunner(windowContext, taskFactory)
        );
    }

    public int registerWindows(Collection<WindowRegistrationRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            return 0;
        }
        int registered = 0;
        for (WindowRegistrationRequest request : requests) {
            if (registerWindow(request) != null) {
                registered++;
            }
        }
        return registered;
    }

    public boolean submit(String windowId, TaskType taskType) {
        WindowTaskRunner runner = runnersByWindowId.get(windowId);
        if (runner == null) {
            return false;
        }
        return runner.submit(taskType);
    }

    public boolean submitSelectedTask(String windowId) {
        WindowTaskRunner runner = runnersByWindowId.get(windowId);
        if (runner == null) {
            return false;
        }
        TaskType selectedTaskType = runner.getWindowContext().getSelectedTaskType();
        if (selectedTaskType == null || selectedTaskType == TaskType.UNKNOWN) {
            return false;
        }
        return runner.submit(selectedTaskType);
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
        WindowTaskRunner runner = runnersByWindowId.get(windowId);
        if (runner != null) {
            runner.stopCurrentTask();
        }
    }

    public void stopAll() {
        runnersByWindowId.values().forEach(WindowTaskRunner::stopCurrentTask);
    }

    public Optional<WindowTaskRunner> getRunner(String windowId) {
        return Optional.ofNullable(runnersByWindowId.get(windowId));
    }

    public Optional<WindowTaskSnapshot> getSnapshot(String windowId) {
        return getRunner(windowId).map(WindowTaskRunner::snapshot);
    }

    public List<WindowTaskSnapshot> getAllSnapshots() {
        return runnersByWindowId.values().stream()
                .map(WindowTaskRunner::snapshot)
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
        WindowTaskRunner runner = runnersByWindowId.remove(windowId);
        if (runner != null) {
            runner.shutdownNow();
        }
    }
}
