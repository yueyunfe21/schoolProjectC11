package com.bot.dhxy.window.service;

import com.bot.dhxy.task.model.TaskType;
import com.bot.dhxy.window.model.WindowRole;
import com.bot.dhxy.window.runner.WindowTaskSnapshot;
import org.springframework.stereotype.Component;

/**
 * 根据窗口识别身份决定默认任务分配。
 *
 * 目前约定：
 * 队长窗口跑用户选择的主任务；
 * 队员窗口默认只跑自动战斗；
 * 未知身份不自动分配任务，避免误操作。
 */
@Component
public class WindowTaskAssignmentPolicy {

    public WindowTaskAssignment assignDefaultTask(WindowTaskSnapshot snapshot, TaskType leaderTaskType) {
        if (snapshot == null) {
            return WindowTaskAssignment.skipped(null, WindowRole.UNKNOWN, TaskType.UNKNOWN, "窗口快照为空");
        }

        WindowRole role = snapshot.getRole() == null ? WindowRole.UNKNOWN : snapshot.getRole();
        return switch (role) {
            case LEADER -> assignLeaderTask(snapshot, leaderTaskType);
            case MEMBER -> WindowTaskAssignment.executable(snapshot.getWindowId(), role, TaskType.AUTO_BATTLE);
            case UNKNOWN -> WindowTaskAssignment.skipped(snapshot.getWindowId(), role, TaskType.UNKNOWN, "窗口身份未知");
        };
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
