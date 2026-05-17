package com.bot.dhxy.runner;

import com.bot.dhxy.model.TaskRunResult;
import lombok.Getter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 一次任务队列执行的结果汇总。
 */
@Getter
public class TaskRunSummary {

    private int total;
    private final Map<TaskRunResult, Integer> resultCounts = new EnumMap<>(TaskRunResult.class);
    private final List<TaskRunRecord> records = new ArrayList<>();

    public TaskRunSummary() {
        for (TaskRunResult result : TaskRunResult.values()) {
            resultCounts.put(result, 0);
        }
    }

    public void record(TaskRunResult result) {
        if (result == null) {
            result = TaskRunResult.FAILED;
        }
        total++;
        resultCounts.put(result, resultCounts.getOrDefault(result, 0) + 1);
    }

    public void record(TaskRunRecord record) {
        if (record == null) {
            record(TaskRunResult.FAILED);
            return;
        }
        records.add(record);
        record(record.getResult());
    }

    public List<TaskRunRecord> getRecords() {
        return Collections.unmodifiableList(records);
    }

    public int getSuccessCount() {
        return resultCounts.getOrDefault(TaskRunResult.SUCCESS, 0);
    }

    public int getFailedCount() {
        return resultCounts.getOrDefault(TaskRunResult.FAILED, 0);
    }

    public int getStoppedCount() {
        return resultCounts.getOrDefault(TaskRunResult.STOPPED, 0);
    }

    public int getSkippedCount() {
        return resultCounts.getOrDefault(TaskRunResult.SKIPPED, 0);
    }

    public String toLogText() {
        return String.format(
                "total=%d, success=%d, failed=%d, skipped=%d, stopped=%d",
                total,
                getSuccessCount(),
                getFailedCount(),
                getSkippedCount(),
                getStoppedCount()
        );
    }
}
