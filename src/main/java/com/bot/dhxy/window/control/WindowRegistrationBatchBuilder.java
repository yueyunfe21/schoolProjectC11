package com.bot.dhxy.window.control;

import com.bot.dhxy.task.model.TaskType;
import com.bot.dhxy.window.model.WindowRole;
import com.bot.dhxy.window.runtime.WindowRegistrationRequest;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 批量构建窗口注册请求。
 *
 * 默认模式：每个窗口都是独立角色，window 层不判断队长/队员。
 */
@Component
public class WindowRegistrationBatchBuilder {

    private static final int DEFAULT_WINDOW_COUNT = 5;

    public List<WindowRegistrationRequest> buildIndependentWindows(String windowIdPrefix,
                                                                    String roleNamePrefix,
                                                                    int count,
                                                                    TaskType taskType) {
        String normalizedWindowPrefix = normalizeWindowPrefix(windowIdPrefix);
        String normalizedRoleNamePrefix = normalizeRoleNamePrefix(roleNamePrefix);
        int safeCount = normalizeCount(count);
        TaskType safeTaskType = taskType == null ? TaskType.UNKNOWN : taskType;

        List<WindowRegistrationRequest> requests = new ArrayList<>();
        for (int i = 1; i <= safeCount; i++) {
            requests.add(WindowRegistrationRequest.of(
                    normalizedWindowPrefix + "-" + i,
                    WindowRole.UNKNOWN,
                    normalizedRoleNamePrefix + i,
                    safeTaskType
            ));
        }
        return requests;
    }

    /**
     * @deprecated 仅保留给旧的测试按身份流程。正式流程请使用 buildIndependentWindows。
     */
    @Deprecated
    public List<WindowRegistrationRequest> buildTeam(String windowIdPrefix,
                                                     String roleNamePrefix,
                                                     int count,
                                                     TaskType leaderTaskType) {
        String normalizedWindowPrefix = normalizeWindowPrefix(windowIdPrefix);
        String normalizedRoleNamePrefix = normalizeRoleNamePrefix(roleNamePrefix);
        int safeCount = normalizeCount(count);
        TaskType safeLeaderTaskType = leaderTaskType == null ? TaskType.UNKNOWN : leaderTaskType;

        List<WindowRegistrationRequest> requests = new ArrayList<>();
        for (int i = 1; i <= safeCount; i++) {
            WindowRole role = i == 1 ? WindowRole.LEADER : WindowRole.MEMBER;
            TaskType taskType = i == 1 ? safeLeaderTaskType : TaskType.AUTO_BATTLE;
            requests.add(WindowRegistrationRequest.of(
                    normalizedWindowPrefix + "-" + i,
                    role,
                    normalizedRoleNamePrefix + i,
                    taskType
            ));
        }
        return requests;
    }

    public String normalizeWindowPrefix(String text) {
        if (text == null || text.isBlank()) {
            return "window";
        }
        String value = text.trim();
        int dashIndex = value.lastIndexOf('-');
        if (dashIndex > 0 && dashIndex < value.length() - 1 && isDigits(value.substring(dashIndex + 1))) {
            return value.substring(0, dashIndex);
        }
        return value;
    }

    public String normalizeRoleNamePrefix(String text) {
        if (text == null || text.isBlank()) {
            return "角色";
        }
        String value = text.trim();
        int lastDigitStart = value.length();
        while (lastDigitStart > 0 && Character.isDigit(value.charAt(lastDigitStart - 1))) {
            lastDigitStart--;
        }
        String prefix = value.substring(0, lastDigitStart);
        return prefix.isBlank() ? "角色" : prefix;
    }

    public int normalizeCount(int count) {
        return count <= 0 ? DEFAULT_WINDOW_COUNT : count;
    }

    public int parseCount(String text) {
        if (text == null || text.isBlank()) {
            return DEFAULT_WINDOW_COUNT;
        }
        try {
            return normalizeCount(Integer.parseInt(text.trim()));
        } catch (NumberFormatException e) {
            return DEFAULT_WINDOW_COUNT;
        }
    }

    private boolean isDigits(String value) {
        return value != null && !value.isBlank() && value.chars().allMatch(Character::isDigit);
    }
}
