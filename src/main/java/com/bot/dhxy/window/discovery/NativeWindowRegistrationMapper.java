package com.bot.dhxy.window.discovery;

import com.bot.dhxy.task.model.TaskType;
import com.bot.dhxy.window.model.WindowRole;
import com.bot.dhxy.window.runtime.WindowNativeBinding;
import com.bot.dhxy.window.runtime.WindowRegistrationRequest;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class NativeWindowRegistrationMapper {

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
                window.getHandle(),
                window.getTitle(),
                window.getClassName(),
                window.getProcessId(),
                window.getX(),
                window.getY(),
                window.getWidth(),
                window.getHeight()
        );
    }

    private TaskType normalizeLeaderTask(TaskType taskType) {
        if (taskType == null || taskType == TaskType.UNKNOWN || taskType == TaskType.AUTO_BATTLE) {
            return TaskType.WUHuan;
        }
        return taskType;
    }
}
