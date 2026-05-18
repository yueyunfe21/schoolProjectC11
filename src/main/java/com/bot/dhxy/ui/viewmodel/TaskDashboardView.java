package com.bot.dhxy.ui.viewmodel;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * UI 首页/任务面板展示模型。
 *
 * 一次性打包任务界面常用数据。
 */
@Getter
@Builder
public class TaskDashboardView {

    /**
     * 当前程序支持的任务选项。
     */
    private final List<TaskOptionView> taskOptions;

    /**
     * 最近任务运行记录。
     */
    private final List<TaskRecordView> recentRecords;

    /**
     * 最近任务日志。
     */
    private final List<TaskLogView> recentLogs;

    /**
     * 当前任务运行状态。
     */
    private final TaskRuntimeStateView runtimeState;
}
