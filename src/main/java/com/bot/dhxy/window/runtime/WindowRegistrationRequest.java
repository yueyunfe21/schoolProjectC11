package com.bot.dhxy.window.runtime;

import com.bot.dhxy.task.model.TaskType;
import com.bot.dhxy.window.model.WindowRole;

/**
 * UI / 窗口扫描模块注册游戏窗口时使用的请求对象。
 */
public class WindowRegistrationRequest {

    private final String windowId;
    private final String roleName;
    private final WindowRole role;
    private final TaskType selectedTaskType;

    public WindowRegistrationRequest(String windowId,
                                     String roleName,
                                     WindowRole role,
                                     TaskType selectedTaskType) {
        this.windowId = windowId;
        this.roleName = roleName;
        this.role = role == null ? WindowRole.UNKNOWN : role;
        this.selectedTaskType = selectedTaskType == null ? TaskType.UNKNOWN : selectedTaskType;
    }

    public static WindowRegistrationRequest of(String windowId) {
        return new WindowRegistrationRequest(windowId, null, WindowRole.UNKNOWN, TaskType.UNKNOWN);
    }

    public static WindowRegistrationRequest of(String windowId, WindowRole role, String roleName) {
        return new WindowRegistrationRequest(windowId, roleName, role, TaskType.UNKNOWN);
    }

    public String getWindowId() {
        return windowId;
    }

    public String getRoleName() {
        return roleName;
    }

    public WindowRole getRole() {
        return role;
    }

    public TaskType getSelectedTaskType() {
        return selectedTaskType;
    }
}
