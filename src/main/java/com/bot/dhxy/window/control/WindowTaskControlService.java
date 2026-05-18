package com.bot.dhxy.window.control;

import com.bot.dhxy.task.model.TaskType;
import com.bot.dhxy.window.execution.MultiWindowTaskManager;
import com.bot.dhxy.window.execution.WindowTaskSnapshot;
import com.bot.dhxy.window.runtime.WindowRegistrationRequest;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * 面向 UI / 控制层的多窗口任务控制服务。
 */
@Service
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
                details.add(WindowTaskCommandDetail.success(request.getWindowId(), "窗口已注册或已刷新"));
            } else {
                details.add(WindowTaskCommandDetail.failed(request.getWindowId(), "窗口注册失败，可能已达到容量上限"));
            }
        }

        return buildResult(requests.size(), successCount, "窗口注册完成", Collections.emptyList(), details);
    }

    public WindowTaskCommandResult start(WindowTaskStartRequest request) {
        if (request == null || !request.hasWindows()) {
            return WindowTaskCommandResult.empty("没有选中的窗口", getSnapshots());
        }

        return switch (request.getStartMode()) {
            case SAME_TASK -> startSameTask(request.getWindowIds(), request.getTaskType());
            case SELECTED_TASK -> startSelectedTasks(request.getWindowIds());
            case DETECTED_ROLE -> startByDetectedRole(request.getWindowIds(), request.getTaskType());
        };
    }

    public WindowTaskCommandResult startSameTask(Collection<String> windowIds, TaskType taskType) {
        List<String> ids = normalizeWindowIds(windowIds);
        if (ids.isEmpty()) {
            return WindowTaskCommandResult.empty("没有选中的窗口", getSnapshots());
        }
        if (taskType == null || taskType == TaskType.UNKNOWN) {
            return buildResult(ids.size(), 0, "任务类型无效", Collections.emptyList(), List.of(WindowTaskCommandDetail.failed(null, "任务类型无效")));
        }

        int successCount = 0;
        List<WindowTaskCommandDetail> details = new ArrayList<>();
        for (String windowId : ids) {
            boolean success = taskManager.submit(windowId, taskType);
            if (success) {
                successCount++;
                details.add(WindowTaskCommandDetail.success(windowId, "已启动任务：" + taskType.getDisplayName()));
            } else {
                details.add(WindowTaskCommandDetail.failed(windowId, "启动失败：窗口不存在、已有任务运行或任务不可创建"));
            }
        }

        return buildResult(ids.size(), successCount, "批量启动统一任务完成", Collections.emptyList(), details);
    }

    public WindowTaskCommandResult startSelectedTasks(Collection<String> windowIds) {
        List<String> ids = normalizeWindowIds(windowIds);
        if (ids.isEmpty()) {
            return WindowTaskCommandResult.empty("没有选中的窗口", getSnapshots());
        }

        int successCount = 0;
        List<WindowTaskCommandDetail> details = new ArrayList<>();
        for (String windowId : ids) {
            boolean success = taskManager.submitSelectedTask(windowId);
            if (success) {
                successCount++;
                details.add(WindowTaskCommandDetail.success(windowId, "已启动窗口已选任务"));
            } else {
                details.add(WindowTaskCommandDetail.failed(windowId, "启动失败：窗口不存在、未选择任务或已有任务运行"));
            }
        }

        return buildResult(ids.size(), successCount, "按窗口已选任务启动完成", Collections.emptyList(), details);
    }

    public WindowTaskCommandResult startByDetectedRole(Collection<String> windowIds, TaskType leaderTaskType) {
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
                details.add(WindowTaskCommandDetail.failed(windowId, "跳过：" + assignment.getReason()));
                continue;
            }
            boolean success = taskManager.submit(assignment.getWindowId(), assignment.getTaskType());
            if (success) {
                successCount++;
                details.add(WindowTaskCommandDetail.success(windowId, "已按身份启动任务：" + assignment.getTaskDisplayName()));
            } else {
                details.add(WindowTaskCommandDetail.failed(windowId, "启动失败：窗口不存在、已有任务运行或任务不可创建"));
            }
        }

        return buildResult(ids.size(), successCount, "按识别身份启动完成", assignments, details);
    }

    public WindowTaskCommandResult stopWindows(Collection<String> windowIds) {
        List<String> ids = normalizeWindowIds(windowIds);
        if (ids.isEmpty()) {
            return WindowTaskCommandResult.empty("没有选中的窗口", getSnapshots());
        }

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
        taskManager.stopAll();
        return WindowTaskCommandResult.of(total, total, "已请求停止全部窗口任务：" + total, getSnapshots());
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
}
