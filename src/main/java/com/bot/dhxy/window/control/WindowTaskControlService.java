package com.bot.dhxy.window.control;

import com.bot.dhxy.task.model.TaskType;
import com.bot.dhxy.window.execution.MultiWindowTaskManager;
import com.bot.dhxy.window.execution.WindowTaskFailurePolicy;
import com.bot.dhxy.window.execution.WindowTaskQueue;
import com.bot.dhxy.window.execution.WindowTaskSnapshot;
import com.bot.dhxy.window.execution.WindowTaskSubmitResult;
import com.bot.dhxy.window.runtime.WindowRegistrationRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Service
@Slf4j
public class WindowTaskControlService {

    private final MultiWindowTaskManager taskManager;
    private final WindowTaskAssignmentPolicy assignmentPolicy;

    public WindowTaskControlService(MultiWindowTaskManager taskManager,
                                    WindowTaskAssignmentPolicy assignmentPolicy) {
        this.taskManager = taskManager;
        this.assignmentPolicy = assignmentPolicy;
    }

    public WindowSystemSnapshot getSystemSnapshot() {
        return new WindowSystemSnapshot(
                taskManager.getRegisteredWindowCount(),
                taskManager.getRunningWindowCount(),
                taskManager.getMaxWindowCount(),
                taskManager.getRemainingWindowCapacity(),
                taskManager.getAllSnapshots()
        );
    }

    public List<WindowTaskSnapshot> getSnapshots() {
        return taskManager.getAllSnapshots();
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
            boolean success = taskManager.registerWindow(request) != null;
            if (success) {
                successCount++;
                details.add(WindowTaskCommandDetail.success(request.getWindowId(),
                        "窗口已注册或已刷新，任务=" + getTaskDisplayName(request.getSelectedTaskType())));
            } else {
                details.add(WindowTaskCommandDetail.failed(request.getWindowId(), "窗口注册失败，可能已达到容量上限"));
            }
        }

        return buildResult(requests.size(), successCount, "独立窗口注册完成", Collections.emptyList(), details);
    }

    public WindowTaskCommandResult start(WindowTaskStartRequest request) {
        if (request == null || !request.hasWindows()) {
            return WindowTaskCommandResult.empty("没有选中的窗口", getSnapshots());
        }

        return switch (request.getStartMode()) {
            case SAME_TASK -> startSameQueue(request.getWindowIds(), request.getTaskQueue());
            case SELECTED_TASK -> startSelectedTasks(request.getWindowIds());
            case DETECTED_ROLE -> startByDetectedRoleForTest(request.getWindowIds(), request.getTaskType());
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
        List<String> ids = normalizeWindowIds(windowIds);
        if (ids.isEmpty()) {
            return WindowTaskCommandResult.empty("没有选中的窗口", getSnapshots());
        }
        WindowTaskQueue safeQueue = queue == null ? WindowTaskQueue.empty() : queue;
        if (safeQueue.isEmpty()) {
            return buildResult(ids.size(), 0, "任务队列无效", Collections.emptyList(),
                    List.of(WindowTaskCommandDetail.failed(null, "任务队列无效")));
        }

        int successCount = 0;
        List<WindowTaskCommandDetail> details = new ArrayList<>();
        for (String windowId : ids) {
            WindowTaskSubmitResult submitResult = taskManager.submitQueueWithResult(windowId, safeQueue);
            log.info("UI start same queue submit result: windowId={} success={} status={} message={} queue={}",
                    submitResult.getWindowId(),
                    submitResult.isSuccess(),
                    submitResult.getStatusDisplayName(),
                    submitResult.getMessage(),
                    submitResult.getTaskQueue().toLogText());
            if (submitResult.isSuccess()) {
                successCount++;
                details.add(WindowTaskCommandDetail.fromSubmitResult(submitResult,
                        "独立窗口已启动任务队列：" + submitResult.getTaskQueueDisplayText() + " | " + submitResult.getMessage()));
            } else {
                details.add(WindowTaskCommandDetail.fromSubmitResult(submitResult,
                        "启动失败：" + submitResult.getMessage() + " | 队列=" + submitResult.getTaskQueueDisplayText()));
            }
        }

        return buildResult(ids.size(), successCount, "独立窗口批量启动完成", Collections.emptyList(), details);
    }

    public WindowTaskCommandResult startSelectedTasks(Collection<String> windowIds) {
        List<String> ids = normalizeWindowIds(windowIds);
        if (ids.isEmpty()) {
            return WindowTaskCommandResult.empty("没有选中的窗口", getSnapshots());
        }

        int successCount = 0;
        List<WindowTaskCommandDetail> details = new ArrayList<>();
        for (String windowId : ids) {
            WindowTaskSubmitResult submitResult = taskManager.submitSelectedTaskWithResult(windowId);
            if (submitResult.isSuccess()) {
                successCount++;
                details.add(WindowTaskCommandDetail.fromSubmitResult(submitResult,
                        "独立窗口已启动已选任务：" + submitResult.getTaskDisplayName() + " | " + submitResult.getMessage()));
            } else {
                details.add(WindowTaskCommandDetail.fromSubmitResult(submitResult,
                        "启动失败：" + submitResult.getMessage() + " | 任务=" + submitResult.getTaskDisplayName()));
            }
        }

        return buildResult(ids.size(), successCount, "独立窗口已选任务启动完成", Collections.emptyList(), details);
    }

    @Deprecated
    public WindowTaskCommandResult startByDetectedRole(Collection<String> windowIds, TaskType leaderTaskType) {
        return startByDetectedRoleForTest(windowIds, leaderTaskType);
    }

    private WindowTaskCommandResult startByDetectedRoleForTest(Collection<String> windowIds, TaskType leaderTaskType) {
        List<String> ids = normalizeWindowIds(windowIds);
        if (ids.isEmpty()) {
            return WindowTaskCommandResult.empty("没有选中的窗口", getSnapshots());
        }

        int successCount = 0;
        List<WindowTaskAssignment> assignments = new ArrayList<>();
        List<WindowTaskCommandDetail> details = new ArrayList<>();
        for (String windowId : ids) {
            WindowTaskSnapshot snapshot = taskManager.getSnapshot(windowId).orElse(null);
            WindowTaskAssignment assignment = assignmentPolicy.assignDefaultTask(snapshot, leaderTaskType);
            assignments.add(assignment);
            if (!assignment.isExecutable()) {
                details.add(WindowTaskCommandDetail.failed(windowId, "测试按身份跳过：" + assignment.getReason()));
                continue;
            }
            WindowTaskSubmitResult submitResult = taskManager.submitWithResult(assignment.getWindowId(), assignment.getTaskType());
            if (submitResult.isSuccess()) {
                successCount++;
                details.add(WindowTaskCommandDetail.fromSubmitResult(submitResult,
                        "测试按身份已启动任务：" + assignment.getTaskDisplayName() + " | " + submitResult.getMessage()));
            } else {
                details.add(WindowTaskCommandDetail.fromSubmitResult(submitResult,
                        "测试按身份启动失败：" + submitResult.getMessage() + " | 任务=" + submitResult.getTaskDisplayName()));
            }
        }

        return buildResult(ids.size(), successCount, "测试按身份启动完成", assignments, details);
    }

    public WindowTaskCommandResult stopWindows(Collection<String> windowIds) {
        List<String> ids = normalizeWindowIds(windowIds);
        if (ids.isEmpty()) {
            return WindowTaskCommandResult.empty("没有选中的窗口", getSnapshots());
        }

        log.info("UI requested stop selected windows: {}", ids);
        int successCount = 0;
        List<WindowTaskCommandDetail> details = new ArrayList<>();
        for (String windowId : ids) {
            if (taskManager.getRunner(windowId).isPresent()) {
                taskManager.stop(windowId);
                successCount++;
                details.add(WindowTaskCommandDetail.success(windowId, "已请求停止"));
            } else {
                details.add(WindowTaskCommandDetail.failed(windowId, "窗口不存在"));
            }
        }
        return buildResult(ids.size(), successCount, "停止选中窗口任务完成", Collections.emptyList(), details);
    }

    public WindowTaskCommandResult stopAll() {
        int total = taskManager.getRegisteredWindowCount();
        log.info("UI requested stop all windows: total={}", total);
        taskManager.stopAll();
        return WindowTaskCommandResult.of(total, total, "已请求停止全部窗口任务：" + total, getSnapshots());
    }

    public WindowTaskCommandResult pauseWindows(Collection<String> windowIds) {
        List<String> ids = normalizeWindowIds(windowIds);
        if (ids.isEmpty()) {
            return WindowTaskCommandResult.empty("没有选中的窗口", getSnapshots());
        }

        log.info("UI requested pause selected windows: {}", ids);
        int successCount = 0;
        List<WindowTaskCommandDetail> details = new ArrayList<>();
        for (String windowId : ids) {
            if (taskManager.pause(windowId)) {
                successCount++;
                details.add(WindowTaskCommandDetail.success(windowId, "已请求暂停"));
            } else {
                details.add(WindowTaskCommandDetail.failed(windowId, "窗口不存在或当前没有运行任务"));
            }
        }
        return buildResult(ids.size(), successCount, "暂停选中窗口任务完成", Collections.emptyList(), details);
    }

    public WindowTaskCommandResult resumeWindows(Collection<String> windowIds) {
        List<String> ids = normalizeWindowIds(windowIds);
        if (ids.isEmpty()) {
            return WindowTaskCommandResult.empty("没有选中的窗口", getSnapshots());
        }

        log.info("UI requested resume selected windows: {}", ids);
        int successCount = 0;
        List<WindowTaskCommandDetail> details = new ArrayList<>();
        for (String windowId : ids) {
            if (taskManager.resume(windowId)) {
                successCount++;
                details.add(WindowTaskCommandDetail.success(windowId, "已请求继续"));
            } else {
                details.add(WindowTaskCommandDetail.failed(windowId, "窗口不存在或当前没有运行任务"));
            }
        }
        return buildResult(ids.size(), successCount, "继续选中窗口任务完成", Collections.emptyList(), details);
    }

    public WindowTaskCommandResult pauseAll() {
        int total = taskManager.getRegisteredWindowCount();
        log.info("UI requested pause all windows: total={}", total);
        int successCount = taskManager.pauseAll();
        return WindowTaskCommandResult.of(total, successCount, "已请求暂停全部运行中窗口任务：" + successCount + "/" + total, getSnapshots());
    }

    public WindowTaskCommandResult resumeAll() {
        int total = taskManager.getRegisteredWindowCount();
        log.info("UI requested resume all windows: total={}", total);
        int successCount = taskManager.resumeAll();
        return WindowTaskCommandResult.of(total, successCount, "已请求继续全部运行中窗口任务：" + successCount + "/" + total, getSnapshots());
    }

    public WindowTaskCommandResult unregisterWindows(Collection<String> windowIds) {
        List<String> ids = normalizeWindowIds(windowIds);
        if (ids.isEmpty()) {
            return WindowTaskCommandResult.empty("没有需要移除的窗口", getSnapshots());
        }

        int successCount = 0;
        List<WindowTaskCommandDetail> details = new ArrayList<>();
        for (String windowId : ids) {
            if (taskManager.getRunner(windowId).isPresent()) {
                taskManager.unregisterWindow(windowId);
                successCount++;
                details.add(WindowTaskCommandDetail.success(windowId, "窗口已移除"));
            } else {
                details.add(WindowTaskCommandDetail.failed(windowId, "窗口不存在"));
            }
        }
        return buildResult(ids.size(), successCount, "窗口移除完成", Collections.emptyList(), details);
    }

    public WindowTaskCommandResult unregisterAll() {
        int total = taskManager.getRegisteredWindowCount();
        taskManager.unregisterAll();
        return WindowTaskCommandResult.of(total, total, "已移除全部窗口：" + total, getSnapshots());
    }

    private WindowTaskCommandResult buildResult(int requestedCount,
                                                int successCount,
                                                String actionName,
                                                List<WindowTaskAssignment> assignments,
                                                List<WindowTaskCommandDetail> details) {
        String message = actionName + "：" + successCount + "/" + requestedCount;
        return WindowTaskCommandResult.of(requestedCount, successCount, message, getSnapshots(), assignments, details);
    }

    private List<String> normalizeWindowIds(Collection<String> windowIds) {
        if (windowIds == null || windowIds.isEmpty()) {
            return Collections.emptyList();
        }
        return windowIds.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(id -> !id.isEmpty())
                .distinct()
                .toList();
    }

    private String getTaskDisplayName(TaskType taskType) {
        return taskType == null ? "-" : taskType.getDisplayName();
    }
}
