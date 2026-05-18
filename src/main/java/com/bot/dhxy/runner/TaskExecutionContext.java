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
