package com.bot.dhxy.window.runner;

import com.bot.dhxy.task.TaskFactory;
import com.bot.dhxy.task.model.TaskType;
import com.bot.dhxy.window.runtime.WindowRuntimeContext;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Collections;
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
    private final Map<String, WindowTaskRunner> runnersByWindowId = new ConcurrentHashMap<>();

    public MultiWindowTaskManager(TaskFactory taskFactory) {
        this.taskFactory = taskFactory;
    }

    public WindowTaskRunner registerWindow(WindowRuntimeContext windowContext) {
        return runnersByWindowId.computeIfAbsent(
                windowContext.getWindowId(),
                ignored -> new WindowTaskRunner(windowContext, taskFactory)
        );
    }

    public boolean submit(String windowId, TaskType taskType) {
        WindowTaskRunner runner = runnersByWindowId.get(windowId);
        if (runner == null) {
            return false;
        }
        return runner.submit(taskType);
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

    public Collection<WindowTaskRunner> getAllRunners() {
        return Collections.unmodifiableCollection(runnersByWindowId.values());
    }

    public void unregisterWindow(String windowId) {
        WindowTaskRunner runner = runnersByWindowId.remove(windowId);
        if (runner != null) {
            runner.shutdownNow();
        }
    }
}
