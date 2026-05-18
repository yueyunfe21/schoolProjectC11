package com.bot.dhxy.runner;

import com.bot.dhxy.runner.parameter.TaskParameterValue;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 单次任务执行上下文。
 *
 * 后面所有具体任务逻辑都应该通过这个对象获取本次运行信息、参数和停止信号。
 */
@Getter
@Builder
public class TaskExecutionContext {

    private final String taskCode;
    private final String taskName;
    /**
     * 多窗口模式下的窗口标识。
     * 单窗口兼容模式下可以为空。
     */
    private final String windowId;
    /**
     * 多窗口模式下当前窗口识别到的角色身份，例如 LEADER / MEMBER / UNKNOWN。
     * 这里使用字符串，避免通用 runner 包反向依赖 window 包。
     */
    private final String windowRole;
    private final TaskRunRequest request;
    private final TaskExecutionPlan plan;
    private final TaskStopToken stopToken;
    private final TaskErrorPolicy errorPolicy;
    private final TaskRetryPolicy retryPolicy;
    private final Map<String, List<TaskParameterValue>> parameters;
    private final LocalDateTime startedAt;

    public boolean isStopRequested() {
        return stopToken != null && stopToken.isStopRequested();
    }

    public void throwIfStopRequested() {
        if (stopToken != null) {
            stopToken.throwIfStopRequested();
        }
    }

    public boolean hasWindow() {
        return windowId != null && !windowId.isBlank();
    }

    public String getLogPrefix() {
        if (!hasWindow()) {
            return "[single-window]";
        }
        return "[window=" + windowId + ", role=" + (windowRole == null ? "UNKNOWN" : windowRole) + "]";
    }

    public List<TaskParameterValue> getTaskParameters(String code) {
        if (parameters == null || code == null) {
            return Collections.emptyList();
        }
        return parameters.getOrDefault(code, Collections.emptyList());
    }

    public Optional<TaskParameterValue> getParameter(String code, String key) {
        if (key == null) {
            return Optional.empty();
        }
        return getTaskParameters(code).stream()
                .filter(value -> key.equals(value.getKey()))
                .findFirst();
    }

    public boolean getBooleanParameter(String code, String key, boolean defaultValue) {
        return getParameter(code, key)
                .map(value -> value.asBoolean(defaultValue))
                .orElse(defaultValue);
    }

    public int getIntParameter(String code, String key, int defaultValue) {
        return getParameter(code, key)
                .map(value -> value.asInt(defaultValue))
                .orElse(defaultValue);
    }

    public String getStringParameter(String code, String key, String defaultValue) {
        return getParameter(code, key)
                .map(value -> value.asString(defaultValue))
                .orElse(defaultValue);
    }
}
