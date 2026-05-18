package com.bot.dhxy.window.runtime;

import com.bot.dhxy.task.model.TaskType;
import com.bot.dhxy.window.model.WindowRole;

import java.util.Objects;

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
        this.windowId = normalize(windowId);
        this.roleName = normalize(roleName);
        this.role = role == null ? WindowRole.UNKNOWN : role;
        this.selectedTaskType = selectedTaskType == null ? TaskType.UNKNOWN : selectedTaskType;
    }

    public static WindowRegistrationRequest of(String windowId) {
        return new WindowRegistrationRequest(windowId, null, WindowRole.UNKNOWN, TaskType.UNKNOWN);
    }

    public static WindowRegistrationRequest of(String windowId, WindowRole role, String roleName) {
        return new WindowRegistrationRequest(windowId, roleName, role, TaskType.UNKNOWN);
    }

    public static WindowRegistrationRequest of(String windowId,
                                               WindowRole role,
                                               String roleName,
                                               TaskType selectedTaskType) {
        return new WindowRegistrationRequest(windowId, roleName, role, selectedTaskType);
    }

    public WindowRegistrationRequest withRole(WindowRole newRole, String newRoleName) {
        return new WindowRegistrationRequest(windowId, newRoleName, newRole, selectedTaskType);
    }

    public WindowRegistrationRequest withSelectedTask(TaskType taskType) {
        return new WindowRegistrationRequest(windowId, roleName, role, taskType);
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

    public boolean hasWindowId() {
        return windowId != null && !windowId.isBlank();
    }

    public boolean hasSelectedTask() {
        return selectedTaskType != null && selectedTaskType != TaskType.UNKNOWN;
    }

    public void requireValid() {
        if (!hasWindowId()) {
            throw new IllegalArgumentException("windowId must not be blank");
        }
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    @Override
    public String toString() {
        return "WindowRegistrationRequest{" +
                "windowId='" + windowId + '\'' +
                ", roleName='" + roleName + '\'' +
                ", role=" + role +
                ", selectedTaskType=" + selectedTaskType +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof WindowRegistrationRequest that)) {
            return false;
        }
        return Objects.equals(windowId, that.windowId)
                && Objects.equals(roleName, that.roleName)
                && role == that.role
                && selectedTaskType == that.selectedTaskType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(windowId, roleName, role, selectedTaskType);
    }
}
