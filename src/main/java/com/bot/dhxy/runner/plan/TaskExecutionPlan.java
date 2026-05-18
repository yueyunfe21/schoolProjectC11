package com.bot.dhxy.runner.plan;

import com.bot.dhxy.runner.model.TaskRunRequest;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * 任务执行计划。
 *
 * 作用：把 TaskRunRequest 转成真正要执行的任务计划。
 * 后面任务去重、过滤、排序、预览，都可以集中在计划层处理。
 */
@Getter
@Builder
public class TaskExecutionPlan {

    /**
     * 原始启动请求。
     */
    private final TaskRunRequest request;

    /**
     * 用户请求中清洗后的任务编码。
     */
    private final List<String> requestedTaskCodes;

    /**
     * 最终允许执行的任务编码。
     */
    private final List<String> executableTaskCodes;

    /**
     * 被忽略的任务编码，比如未注册、重复、空白项等。
     */
    private final List<String> ignoredTaskCodes;

    private final boolean loop;
    private final boolean testMode;
    private final boolean initGameWindow;
    private final String summaryText;
    private final String warningText;

    /**
     * 兼容旧调用：以前字段名叫 taskCodes。
     */
    public List<String> getTaskCodes() {
        return executableTaskCodes;
    }

    public int getTaskCount() {
        return executableTaskCodes == null ? 0 : executableTaskCodes.size();
    }

    public int getIgnoredTaskCount() {
        return ignoredTaskCodes == null ? 0 : ignoredTaskCodes.size();
    }

    public boolean hasIgnoredTasks() {
        return getIgnoredTaskCount() > 0;
    }

    public boolean isValid() {
        return getTaskCount() > 0;
    }

    public boolean isEmpty() {
        return !isValid();
    }

    public String toLogText() {
        return "taskCount=" + getTaskCount()
                + " | requested=" + requestedTaskCodes
                + " | executable=" + executableTaskCodes
                + " | ignored=" + ignoredTaskCodes
                + " | loop=" + loop
                + " | testMode=" + testMode
                + " | initGameWindow=" + initGameWindow
                + " | summary=" + summaryText
                + " | warning=" + warningText;
    }
}
