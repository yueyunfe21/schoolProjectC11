package com.bot.dhxy.runner;

import lombok.Builder;
import lombok.Getter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 一次任务队列启动请求。
 *
 * 作用：把启动任务需要的参数集中到一个对象里。
 * 为什么加：后面任务启动参数会越来越多，用对象承载比多个散参数更稳定。
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

    public String toLogText() {
        return "tasks=" + getNormalizedTaskCodes()
                + " | loop=" + loop
                + " | testMode=" + testMode;
    }
}
