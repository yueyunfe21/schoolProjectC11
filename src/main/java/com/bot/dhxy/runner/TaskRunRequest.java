package com.bot.dhxy.runner;

import com.bot.dhxy.runner.parameter.TaskParameterValue;
import lombok.Builder;
import lombok.Getter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 一次任务队列启动请求。
 *
 * 作用：把启动任务需要的参数集中到一个对象里。
 */
@Getter
@Builder
public class TaskRunRequest {

    /**
     * 本次要执行的任务编码列表，例如：wuhuan、zhuagui、xiuluo。
     */
    private final List<String> taskCodes;

    /**
     * 是否循环执行任务队列。
     */
    private final boolean loop;

    /**
     * 是否测试模式，只验证调度，不执行真实任务逻辑。
     */
    private final boolean testMode;

    /**
     * 是否在启动任务前初始化游戏窗口。
     */
    private final boolean initGameWindow;

    /**
     * 任务参数。key 为 taskCode，value 为该任务的参数列表。
     */
    private final Map<String, List<TaskParameterValue>> parameters;

    /**
     * 清洗后的任务编码列表，去掉 null、空白项和前后空格。
     */
    public List<String> getNormalizedTaskCodes() {
        if (taskCodes == null || taskCodes.isEmpty()) {
            return Collections.emptyList();
        }
        return taskCodes.stream()
                .filter(code -> code != null && !code.isBlank())
                .map(String::trim)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    public boolean hasTasks() {
        return !getNormalizedTaskCodes().isEmpty();
    }

    public boolean isEmpty() {
        return !hasTasks();
    }

    public int getTaskCount() {
        return getNormalizedTaskCodes().size();
    }

    public Map<String, List<TaskParameterValue>> getSafeParameters() {
        return parameters == null ? Collections.emptyMap() : parameters;
    }

    public String toLogText() {
        return "taskCount=" + getTaskCount()
                + " | tasks=" + getNormalizedTaskCodes()
                + " | loop=" + loop
                + " | testMode=" + testMode
                + " | initGameWindow=" + initGameWindow
                + " | parameterTasks=" + getSafeParameters().keySet();
    }
}
