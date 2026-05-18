package com.bot.dhxy.ui.viewmodel;

import lombok.Builder;
import lombok.Getter;

/**
 * UI 任务执行计划预览模型。
 */
@Getter
@Builder
public class TaskPlanView {

    private final boolean valid;
    private final int requestedCount;
    private final int executableCount;
    private final int ignoredCount;
    private final String requestedTasksText;
    private final String executableTasksText;
    private final String ignoredTasksText;
    private final String optionsText;
    private final String summaryText;
    private final String warningText;
}
