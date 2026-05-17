package com.bot.dhxy.ui.viewmodel;

import lombok.Builder;
import lombok.Getter;

/**
 * UI 任务日志展示模型。
 */
@Getter
@Builder
public class TaskLogView {

    private final String time;
    private final String type;
    private final String taskCode;
    private final String taskName;
    private final String message;
}
