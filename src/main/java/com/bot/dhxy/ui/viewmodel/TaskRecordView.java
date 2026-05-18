package com.bot.dhxy.ui.viewmodel;

import lombok.Builder;
import lombok.Getter;

/**
 * UI 任务运行记录展示模型。
 */
@Getter
@Builder
public class TaskRecordView {

    private final String taskCode;
    private final String taskName;
    private final String result;
    private final String startTime;
    private final String endTime;
    private final long costMillis;
    private final String costText;
    private final String message;
}
