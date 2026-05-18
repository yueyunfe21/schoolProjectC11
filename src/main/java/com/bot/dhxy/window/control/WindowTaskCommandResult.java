package com.bot.dhxy.window.control;

import com.bot.dhxy.window.execution.WindowTaskSnapshot;

import java.util.Collections;
import java.util.List;

/**
 * 窗口任务控制命令的统一返回结果。
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
        this.successCount = Math.min(Math.max(successCount, 0), this.requestedCount);
        this.failedCount = Math.max(this.requestedCount - this.successCount, 0);
        this.message = message == null ? "" : message;
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

    public int getRequestedCount() { return requestedCount; }

    public int getSuccessCount() { return successCount; }

    public int getFailedCount() { return failedCount; }

    public String getMessage() { return message; }

    public List<WindowTaskSnapshot> getSnapshots() { return snapshots; }

    public List<WindowTaskAssignment> getAssignments() { return assignments; }

    public List<WindowTaskCommandDetail> getDetails() { return details; }

    public boolean hasAssignments() { return !assignments.isEmpty(); }

    public boolean hasDetails() { return !details.isEmpty(); }

    public boolean isAllSuccess() { return requestedCount > 0 && failedCount == 0; }

    public boolean hasAnySuccess() { return successCount > 0; }

    public boolean isEmpty() { return requestedCount == 0; }
}
