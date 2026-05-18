package com.bot.dhxy.window.service;

import com.bot.dhxy.task.model.TaskType;
import com.bot.dhxy.window.model.WindowRole;
import com.bot.dhxy.window.runner.WindowTaskSnapshot;
import org.springframework.stereotype.Component;

@Component
public class WindowTaskAssignmentPolicy {

    public WindowTaskAssignment assignDefaultTask(WindowTaskSnapshot snapshot, TaskType leaderTaskType) {
        WindowTaskAssignment blocked = checkSnapshot(snapshot);
        if (blocked != null) {
            return blocked;
        }

        WindowRole role = snapshot.getRole();
        return switch (role) {
            case LEADER -> assignLeaderTask(snapshot, leaderTaskType);
            case MEMBER -> WindowTaskAssignment.executable(snapshot.getWindowId(), role, TaskType.AUTO_BATTLE);
            case UNKNOWN -> WindowTaskAssignment.skipped(snapshot.getWindowId(), role, TaskType.UNKNOWN, "窗口身份未知");
        };
    }

    public WindowTaskAssignment assignSelectedTask(WindowTaskSnapshot snapshot) {
        WindowTaskAssignment blocked = checkSnapshot(snapshot);
        if (blocked != null) {
            return blocked;
        }

        TaskType selectedTaskType = snapshot.getSelectedTaskType();
        if (selectedTaskType == null || selectedTaskType == TaskType.UNKNOWN) {
            return WindowTaskAssignment.skipped(snapshot.getWindowId(), snapshot.getRole(), TaskType.UNKNOWN, "窗口没有选择有效任务");
        }
        return WindowTaskAssignment.executable(snapshot.getWindowId(), snapshot.getRole(), selectedTaskType);
    }

    private WindowTaskAssignment checkSnapshot(WindowTaskSnapshot snapshot) {
        if (snapshot == null) {
            return WindowTaskAssignment.skipped(null, WindowRole.UNKNOWN, TaskType.UNKNOWN, "窗口快照为空");
        }
        if (snapshot.isBusy()) {
            return WindowTaskAssignment.skipped(snapshot.getWindowId(), snapshot.getRole(), snapshot.getSelectedTaskType(), "窗口正在运行或切换状态");
        }
        return null;
    }

    private WindowTaskAssignment assignLeaderTask(WindowTaskSnapshot snapshot, TaskType leaderTaskType) {
        TaskType taskType = leaderTaskType;
        if (taskType == null || taskType == TaskType.UNKNOWN || taskType == TaskType.AUTO_BATTLE) {
            taskType = snapshot.getSelectedTaskType();
        }
        if (taskType == null || taskType == TaskType.UNKNOWN || taskType == TaskType.AUTO_BATTLE) {
            return WindowTaskAssignment.skipped(snapshot.getWindowId(), WindowRole.LEADER, TaskType.UNKNOWN, "队长没有有效主任务");
        }
        return WindowTaskAssignment.executable(snapshot.getWindowId(), WindowRole.LEADER, taskType);
    }
}
