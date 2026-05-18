package com.bot.dhxy.runner;

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

    private final TaskRunRequest request;
    private final List<String> taskCodes;
    private final boolean loop;
    private final boolean testMode;
    private final boolean initGameWindow;
    private final String summaryText;

    public int getTaskCount() {
        return taskCodes == null ? 0 : taskCodes.size();
    }

    public boolean isEmpty() {
        return getTaskCount() <= 0;
    }

    public String toLogText() {
        return "taskCount=" + getTaskCount()
                + " | tasks=" + taskCodes
                + " | loop=" + loop
                + " | testMode=" + testMode
                + " | initGameWindow=" + initGameWindow
                + " | summary=" + summaryText;
    }
}
