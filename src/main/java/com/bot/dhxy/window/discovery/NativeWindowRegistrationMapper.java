package com.bot.dhxy.window.discovery;

import com.bot.dhxy.task.model.TaskType;
import com.bot.dhxy.window.model.WindowNativeBinding;
import com.bot.dhxy.window.model.WindowRole;
import com.bot.dhxy.window.runtime.WindowRegistrationRequest;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class NativeWindowRegistrationMapper {

    /**
     * 正式扫描注册逻辑：每个真实游戏窗口都是一个独立角色。
     *
     * window 层不判断队长/队员，也不默认第一个窗口是队长。
     * 后续是否队长、是否继续执行，由具体任务内部根据游戏状态自己判断。
     */
    public List<WindowRegistrationRequest> toIndependentRegistrationRequests(List<NativeWindowInfo> windows, TaskType taskType) {
        List<WindowRegistrationRequest> requests = new ArrayList<>();
        if (windows == null || windows.isEmpty()) {
            return requests;
        }
        TaskType safeTaskType = normalizeTask(taskType);
        for (NativeWindowInfo window : windows) {
            if (window == null) {
                continue;
            }
            WindowRole role = roleForIndependentTask(safeTaskType);
            requests.add(WindowRegistrationRequest.of(
                    window.toWindowId(),
                    role,
                    window.toDisplayName(),
                    safeTaskType,
                    toBinding(window)
            ));
        }
        return requests;
    }

    /**
     * @deprecated 仅保留给旧的“测试按身份启动”流程。正式流程请使用 toIndependentRegistrationRequests。
     */
    @Deprecated
    public List<WindowRegistrationRequest> toRegistrationRequests(List<NativeWindowInfo> windows, TaskType leaderTaskType) {
        List<WindowRegistrationRequest> requests = new ArrayList<>();
        if (windows == null || windows.isEmpty()) {
            return requests;
        }
        TaskType safeLeaderTask = normalizeLeaderTask(leaderTaskType);
        for (int i = 0; i < windows.size(); i++) {
            NativeWindowInfo window = windows.get(i);
            if (window == null) {
                continue;
            }
            WindowRole role = i == 0 ? WindowRole.LEADER : WindowRole.MEMBER;
            TaskType taskType = role.isLeader() ? safeLeaderTask : TaskType.AUTO_BATTLE;
            requests.add(WindowRegistrationRequest.of(
                    window.toWindowId(),
                    role,
                    window.toDisplayName(),
                    taskType,
                    toBinding(window)
            ));
        }
        return requests;
    }

    private WindowNativeBinding toBinding(NativeWindowInfo window) {
        return new WindowNativeBinding(
                scannerHandleForBinding(window.getHandle()),
                window.getTitle(),
                window.getClassName(),
                window.getProcessId(),
                window.getX(),
                window.getY(),
                window.getWidth(),
                window.getHeight()
        );
    }

    private String scannerHandleForBinding(String handle) {
        if (handle == null || handle.isBlank()) {
            return handle;
        }
        String value = handle.trim();
        if (value.startsWith("0x") || value.startsWith("0X")) {
            return value;
        }
        return "0x" + value;
    }

    private TaskType normalizeTask(TaskType taskType) {
        return taskType == null ? TaskType.UNKNOWN : taskType;
    }

    private WindowRole roleForIndependentTask(TaskType taskType) {
        if (taskType == TaskType.AUTO_BATTLE) {
            /*
             * 用户显式选择“自动战斗”时，含义就是这些窗口按队员挂机窗口处理。
             * 不再等待队伍身份探测，否则 UNKNOWN 会被自动战斗前置判断跳过。
             */
            return WindowRole.MEMBER;
        }
        return WindowRole.UNKNOWN;
    }

    private TaskType normalizeLeaderTask(TaskType taskType) {
        if (taskType == null || taskType == TaskType.UNKNOWN || taskType == TaskType.AUTO_BATTLE) {
            return TaskType.WUHuan;
        }
        return taskType;
    }
}
