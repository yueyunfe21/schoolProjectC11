package com.bot.dhxy.runner;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * 任务执行历史记录服务。
 *
 * 目前先保存在内存中，默认只保留最近 50 条任务记录。
 * 后面接 UI 后，可以直接读取这里的数据展示任务日志表。
 */
@Component
public class TaskRunHistoryService {

    private static final int MAX_RECORDS = 50;
    private final LinkedList<TaskRunRecord> records = new LinkedList<>();

    public synchronized void addRecord(TaskRunRecord record) {
        if (record == null) {
            return;
        }
        records.addFirst(record);
        while (records.size() > MAX_RECORDS) {
            records.removeLast();
        }
    }

    public synchronized void addRecords(List<TaskRunRecord> newRecords) {
        if (newRecords == null || newRecords.isEmpty()) {
            return;
        }
        for (TaskRunRecord record : newRecords) {
            addRecord(record);
        }
    }

    public synchronized List<TaskRunRecord> getRecentRecords() {
        return Collections.unmodifiableList(new ArrayList<>(records));
    }

    public synchronized void clear() {
        records.clear();
    }

    public int getMaxRecords() {
        return MAX_RECORDS;
    }
}
