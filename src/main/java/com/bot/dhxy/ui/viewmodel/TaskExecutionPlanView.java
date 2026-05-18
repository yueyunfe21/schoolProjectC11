package com.bot.dhxy.ui.viewmodel;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * UI 任务执行计划展示模型。
 */
@Getter
@Builder
public class TaskExecutionPlanView {

    private final boolean valid;
    private final int taskCount;
    private final int ignoredTaskCount;
    private final List<String> requestedTaskCodes;
    private final List<String> executableTaskCodes;
    private final List<String> ignoredTaskCodes;
    private final boolean loop;
    private final boolean testMode;
    private final boolean initGameWindow;
    private final String summaryText;
    private final String warningText;
}
