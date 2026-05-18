package com.bot.dhxy.ui;

import com.bot.dhxy.config.TaskRunProperties;
import com.bot.dhxy.runner.TaskControlService;
import com.bot.dhxy.runner.TaskDefinition;
import com.bot.dhxy.runner.TaskLogEntry;
import com.bot.dhxy.runner.TaskRunRecord;
import com.bot.dhxy.runner.TaskRuntimeState;
import com.bot.dhxy.ui.viewmodel.TaskDashboardView;
import com.bot.dhxy.ui.viewmodel.TaskLogView;
import com.bot.dhxy.ui.viewmodel.TaskOptionView;
import com.bot.dhxy.ui.viewmodel.TaskRecordView;
import com.bot.dhxy.ui.viewmodel.TaskRuntimeStateView;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 任务 UI 数据服务。
 *
 * 负责把后端任务对象转换成界面可以直接展示的 ViewModel。
 */
@Component
@RequiredArgsConstructor
public class TaskViewService {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final TaskControlService taskControlService;
    private final TaskRunProperties taskRunProperties;

    public TaskDashboardView getDashboardView() {
        return TaskDashboardView.builder()
                .taskOptions(getTaskOptions())
                .recentRecords(getRecentRecords())
                .recentLogs(getRecentLogs())
                .runtimeState(getRuntimeState())
                .build();
    }

    public List<TaskOptionView> getTaskOptions() {
        Set<String> selectedCodes = new HashSet<>(taskRunProperties.getNormalizedTasks());
        return taskControlService.getAvailableTasks().stream()
                .map(task -> toTaskOptionView(task, selectedCodes))
                .toList();
    }

    public List<TaskRecordView> getRecentRecords() {
        return taskControlService.getRecentTaskRecords().stream()
                .map(this::toTaskRecordView)
                .toList();
    }

    public List<TaskLogView> getRecentLogs() {
        return taskControlService.getRecentLogs().stream()
                .map(this::toTaskLogView)
                .toList();
    }

    public TaskRuntimeStateView getRuntimeState() {
        return toTaskRuntimeStateView(taskControlService.getRuntimeState());
    }

    private TaskOptionView toTaskOptionView(TaskDefinition task, Set<String> selectedCodes) {
        return TaskOptionView.builder()
                .taskCode(task.getTaskCode())
                .taskName(task.getTaskName())
                .selected(selectedCodes.contains(task.getTaskCode()))
                .enabled(true)
                .build();
    }

    private TaskRecordView toTaskRecordView(TaskRunRecord record) {
        long costMillis = record.getCostMillis();
        return TaskRecordView.builder()
                .taskCode(record.getTaskCode())
                .taskName(record.getTaskName())
                .result(record.getResult() == null ? "" : record.getResult().name())
                .startTime(record.getStartTime() == null ? "" : TIME_FORMATTER.format(record.getStartTime()))
                .endTime(record.getEndTime() == null ? "" : TIME_FORMATTER.format(record.getEndTime()))
                .costMillis(costMillis)
                .costText(formatCost(costMillis))
                .message(record.getMessage())
                .build();
    }

    private TaskLogView toTaskLogView(TaskLogEntry entry) {
        return TaskLogView.builder()
                .time(entry.getTime() == null ? "" : TIME_FORMATTER.format(entry.getTime()))
                .type(entry.getType() == null ? "" : entry.getType().name())
                .taskCode(entry.getTaskCode())
                .taskName(entry.getTaskName())
                .message(entry.getMessage())
                .build();
    }

    private TaskRuntimeStateView toTaskRuntimeStateView(TaskRuntimeState state) {
        if (state == null) {
            return TaskRuntimeStateView.builder()
                    .status("IDLE")
                    .running(false)
                    .stopping(false)
                    .statusText("空闲")
                    .elapsedText("0ms")
                    .build();
        }
        return TaskRuntimeStateView.builder()
                .status(state.getStatus() == null ? "IDLE" : state.getStatus().name())
                .running(state.isRunning())
                .stopping(state.isStopping())
                .statusText(state.getStatusText())
                .startedAt(state.getStartedAt() == null ? "" : TIME_FORMATTER.format(state.getStartedAt()))
                .finishedAt(state.getFinishedAt() == null ? "" : TIME_FORMATTER.format(state.getFinishedAt()))
                .elapsedText(state.getElapsedText())
                .requestText(state.getCurrentRequest() == null ? "" : state.getCurrentRequest().toLogText())
                .summaryText(state.getLastSummary() == null ? "" : state.getLastSummary().toLogText())
                .build();
    }

    private String formatCost(long costMillis) {
        if (costMillis < 1000) {
            return costMillis + "ms";
        }
        long seconds = costMillis / 1000;
        long minutes = seconds / 60;
        long remainSeconds = seconds % 60;
        if (minutes <= 0) {
            return seconds + "s";
        }
        return minutes + "m " + remainSeconds + "s";
    }
}
