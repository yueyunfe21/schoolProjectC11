package com.bot.dhxy.ui.viewmodel;

import lombok.Builder;
import lombok.Getter;

/**
 * UI 任务运行状态展示模型。
 */
@Getter
@Builder
public class TaskRuntimeStateView {

    private final boolean running;
    private final String statusText;
    private final String startedAt;
    private final String finishedAt;
    private final String elapsedText;
    private final String requestText;
    private final String summaryText;
}
