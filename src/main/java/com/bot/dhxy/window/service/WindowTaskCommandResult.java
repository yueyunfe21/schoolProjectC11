package com.bot.dhxy.window.service;

import com.bot.dhxy.window.runner.WindowTaskSnapshot;

import java.util.Collections;
import java.util.List;

/**
 * 窗口任务控制命令的返回结果。
 *
 * UI 层以后调用批量注册、启动、停止窗口任务时，可以直接拿这个对象展示执行结果，
 * 不需要直接接触 WindowTaskRunner / Future / Thread 这些内部实现。
 */
public class WindowTaskCommandResult {

    private final int requestedCount;
    private final int successCount;
    private final int failedCount;
    private final String message;
    private final List<WindowTaskSnapshot> snapshots;
    private final List<WindowTaskAssignment> assignments;
    private final List<WindowTaskCommandDetail> details;

    public WindowTaskCommandResult(int requestedCount,
                                   int successCount,
                                   String message,
                                   List<WindowTaskSnapshot> snapshots) {
        this(requestedCount, successCount, message, snapshots, Collections.emptyList(), Collections.emptyList());
    }

    public WindowTaskCommandResult(int requestedCount,
                                   int successCount,
                                   String message,
                                   List<WindowTaskSnapshot> snapshots,
                                   List<WindowTaskAssignment> assignments) {
        this(requestedCount, successCount, message, snapshots, assignments, Collections.emptyList());
    }

    public WindowTaskCommandResult(int requestedCount,
                                   int successCount,
                                   String message,
                                   List<WindowTaskSnapshot> snapshots,
                                   List<WindowTaskAssignment> assignments,
                                   List<WindowTaskCommandDetail> details) {
        this.requestedCount = Math.max(requestedCount, 0);
        this.successCount = Math.max(successCount, 0);
        this.failedCount = Math.max(this.requestedCount - this.successCount, 0);
        this.message = message;
        this.snapshots = snapshots == null ? Collections.emptyList() : List.copyOf(snapshots);
        this.assignments = assignments == null ? Collections.emptyList() : List.copyOf(assignments);
        this.details = details == null ? Collections.emptyList() : List.copyOf(details);
    }

    public static WindowTaskCommandResult of(int requestedCount,
                                             int successCount,
                                             String message,
                                             List<WindowTaskSnapshot> snapshots) {
        return new WindowTaskCommandResult(requestedCount, successCount, message, snapshots);
    }

    public static WindowTaskCommandResult of(int requestedCount,
                                             int successCount,
                                             String message,
                                             List<WindowTaskSnapshot> snapshots,
                                             List<WindowTaskAssignment> assignments) {
        return new WindowTaskCommandResult(requestedCount, successCount, message, snapshots, assignments);
    }

    public static WindowTaskCommandResult of(int requestedCount,
                                             int successCount,
                                             String message,
                                             List<WindowTaskSnapshot> snapshots,
                                             List<WindowTaskAssignment> assignments,
                                             List<WindowTaskCommandDetail> details) {
        return new WindowTaskCommandResult(requestedCount, successCount, message, snapshots, assignments, details);
    }

    public static WindowTaskCommandResult empty(String message, List<WindowTaskSnapshot> snapshots) {
        return new WindowTaskCommandResult(0, 0, message, snapshots);
    }

    public int getRequestedCount() {
        return requestedCount;
    }

    public int getSuccessCount() {
        return successCount;
    }

    public int getFailedCount() {
        return failedCount;
    }

    public String getMessage() {
        return message;
    }

    public List<WindowTaskSnapshot> getSnapshots() {
        return snapshots;
    }

    public List<WindowTaskAssignment> getAssignments() {
        return assignments;
    }

    public List<WindowTaskCommandDetail> getDetails() {
        return details;
    }

    public boolean hasAssignments() {
        return !assignments.isEmpty();
    }

    public boolean hasDetails() {
        return !details.isEmpty();
    }

    public boolean isAllSuccess() {
        return requestedCount > 0 && failedCount == 0;
    }

    public boolean hasAnySuccess() {
        return successCount > 0;
    }
}
