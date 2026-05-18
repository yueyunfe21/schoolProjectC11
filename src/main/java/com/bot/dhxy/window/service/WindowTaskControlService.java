package com.bot.dhxy.window.service;

import com.bot.dhxy.task.model.TaskType;
import com.bot.dhxy.window.runtime.WindowRegistrationRequest;
import com.bot.dhxy.window.runner.MultiWindowTaskManager;
import com.bot.dhxy.window.runner.WindowTaskSnapshot;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * 面向 UI / 控制层的多窗口任务控制服务。
 *
 * 这一层负责把 UI 操作翻译成 MultiWindowTaskManager 的注册、启动、停止命令。
 * UI 后面优先调用这个 service，而不是直接操作 WindowTaskRunner。
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

    public WindowTaskCommandResult registerWindows(Collection<WindowRegistrationRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            return WindowTaskCommandResult.empty("没有需要注册的窗口", taskManager.getAllSnapshots());
        }

        int registered = 0;
        List<WindowTaskCommandDetail> details = new ArrayList<>();
        for (WindowRegistrationRequest request : requests) {
            if (request == null || request.getWindowId() == null || request.getWindowId().isBlank()) {
                details.add(WindowTaskCommandDetail.failed(null, "窗口注册请求无效"));
                continue;
            }
            boolean success = taskManager.registerWindow(request) != null;
            if (success) {
                registered++;
                details.add(WindowTaskCommandDetail.success(request.getWindowId(), "窗口已注册或已刷新"));
            } else {
                details.add(WindowTaskCommandDetail.failed(request.getWindowId(), "窗口注册失败，可能已达到容量上限"));
            }
        }

        return WindowTaskCommandResult.of(
                requests.size(),
                registered,
                "窗口注册完成：" + registered + "/" + requests.size(),
                taskManager.getAllSnapshots(),
                Collections.emptyList(),
                details
        );
    }

    public WindowTaskCommandResult start(WindowTaskStartRequest request) {
        if (request == null || !request.hasWindows()) {
            return WindowTaskCommandResult.empty("没有选中的窗口", taskManager.getAllSnapshots());
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
            return WindowTaskCommandResult.empty("没有选中的窗口", taskManager.getAllSnapshots());
        }
        if (taskType == null || taskType == TaskType.UNKNOWN) {
            return WindowTaskCommandResult.of(ids.size(), 0, "任务类型无效", taskManager.getAllSnapshots());
        }

        int accepted = 0;
        List<WindowTaskCommandDetail> details = new ArrayList<>();
        for (String windowId : ids) {
            boolean success = taskManager.submit(windowId, taskType);
            if (success) {
                accepted++;
                details.add(WindowTaskCommandDetail.success(windowId, "已启动任务：" + taskType.getDisplayName()));
            } else {
                details.add(WindowTaskCommandDetail.failed(windowId, "启动失败：窗口不存在、已有任务运行或任务不可创建"));
            }
        }

        return WindowTaskCommandResult.of(
                ids.size(),
                accepted,
                "批量启动统一任务完成：" + accepted + "/" + ids.size(),
                taskManager.getAllSnapshots(),
                Collections.emptyList(),
                details
        );
    }

    public WindowTaskCommandResult startSelectedTasks(Collection<String> windowIds) {
        List<String> ids = normalizeWindowIds(windowIds);
        if (ids.isEmpty()) {
            return WindowTaskCommandResult.empty("没有选中的窗口", taskManager.getAllSnapshots());
        }

        int accepted = 0;
        List<WindowTaskCommandDetail> details = new ArrayList<>();
        for (String windowId : ids) {
            boolean success = taskManager.submitSelectedTask(windowId);
            if (success) {
                accepted++;
                details.add(WindowTaskCommandDetail.success(windowId, "已启动窗口已选任务"));
            } else {
                details.add(WindowTaskCommandDetail.failed(windowId, "启动失败：窗口不存在、未选择任务或已有任务运行"));
            }
        }

        return WindowTaskCommandResult.of(
                ids.size(),
                accepted,
                "按窗口已选任务启动完成：" + accepted + "/" + ids.size(),
                taskManager.getAllSnapshots(),
                Collections.emptyList(),
                details
        );
    }

    public WindowTaskCommandResult startByDetectedRole(Collection<String> windowIds, TaskType leaderTaskType) {
        List<String> ids = normalizeWindowIds(windowIds);
        if (ids.isEmpty()) {
            return WindowTaskCommandResult.empty("没有选中的窗口", taskManager.getAllSnapshots());
        }

        int accepted = 0;
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
                accepted++;
                details.add(WindowTaskCommandDetail.success(windowId, "已按身份启动任务：" + assignment.getTaskType().getDisplayName()));
            } else {
                details.add(WindowTaskCommandDetail.failed(windowId, "启动失败：窗口不存在、已有任务运行或任务不可创建"));
            }
        }

        return WindowTaskCommandResult.of(
                ids.size(),
                accepted,
                "按识别身份启动完成：" + accepted + "/" + ids.size(),
                taskManager.getAllSnapshots(),
                assignments,
                details
        );
    }

    public WindowTaskCommandResult stopWindows(Collection<String> windowIds) {
        List<String> ids = normalizeWindowIds(windowIds);
        if (ids.isEmpty()) {
            return WindowTaskCommandResult.empty("没有选中的窗口", taskManager.getAllSnapshots());
        }

        List<WindowTaskCommandDetail> details = new ArrayList<>();
        for (String windowId : ids) {
            if (taskManager.getRunner(windowId).isPresent()) {
                taskManager.stop(windowId);
                details.add(WindowTaskCommandDetail.success(windowId, "已请求停止"));
            } else {
                details.add(WindowTaskCommandDetail.failed(windowId, "窗口不存在"));
            }
        }
        return WindowTaskCommandResult.of(
                ids.size(),
                (int) details.stream().filter(WindowTaskCommandDetail::isSuccess).count(),
                "停止选中窗口任务完成",
                taskManager.getAllSnapshots(),
                Collections.emptyList(),
                details
        );
    }

    public WindowTaskCommandResult stopAll() {
        int total = taskManager.getRegisteredWindowCount();
        taskManager.stopAll();
        return WindowTaskCommandResult.of(
                total,
                total,
                "已请求停止全部窗口任务：" + total,
                taskManager.getAllSnapshots()
        );
    }

    public WindowTaskCommandResult unregisterWindows(Collection<String> windowIds) {
        List<String> ids = normalizeWindowIds(windowIds);
        if (ids.isEmpty()) {
            return WindowTaskCommandResult.empty("没有需要移除的窗口", taskManager.getAllSnapshots());
        }

        List<WindowTaskCommandDetail> details = new ArrayList<>();
        for (String windowId : ids) {
            if (taskManager.getRunner(windowId).isPresent()) {
                taskManager.unregisterWindow(windowId);
                details.add(WindowTaskCommandDetail.success(windowId, "窗口已移除"));
            } else {
                details.add(WindowTaskCommandDetail.failed(windowId, "窗口不存在"));
            }
        }
        return WindowTaskCommandResult.of(
                ids.size(),
                (int) details.stream().filter(WindowTaskCommandDetail::isSuccess).count(),
                "窗口移除完成",
                taskManager.getAllSnapshots(),
                Collections.emptyList(),
                details
        );
    }

    public List<WindowTaskSnapshot> getSnapshots() {
        return taskManager.getAllSnapshots();
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
