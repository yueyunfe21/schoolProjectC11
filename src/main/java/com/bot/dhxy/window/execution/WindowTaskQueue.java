package com.bot.dhxy.window.execution;

import com.bot.dhxy.task.model.TaskType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Immutable queue of tasks submitted to one registered window.
 *
 * <p>The queue stores task types only; individual {@code GameTask} instances are created later by
 * {@link WindowTaskRunner} after role detection and startup checks. Null and UNKNOWN task types are
 * removed during normalization so an empty queue is always invalid for execution.</p>
 */
public class WindowTaskQueue {

    private final List<TaskType> taskTypes;
    private final WindowTaskFailurePolicy failurePolicy;

    /**
     * Create a queue that continues after individual task failures.
     *
     * @param taskTypes requested task types; null, empty, and UNKNOWN entries are ignored.
     */
    public WindowTaskQueue(Collection<TaskType> taskTypes) {
        this(taskTypes, WindowTaskFailurePolicy.CONTINUE_ON_FAILURE);
    }

    /**
     * Create a task queue with an explicit failure policy.
     *
     * @param taskTypes requested task types; null, empty, and UNKNOWN entries are ignored.
     * @param failurePolicy queue behavior after a task returns FAILED; null defaults to continue.
     */
    public WindowTaskQueue(Collection<TaskType> taskTypes, WindowTaskFailurePolicy failurePolicy) {
        this.taskTypes = normalize(taskTypes);
        this.failurePolicy = failurePolicy == null ? WindowTaskFailurePolicy.CONTINUE_ON_FAILURE : failurePolicy;
    }

    /** @return empty queue used for validation failures and UI placeholders. */
    public static WindowTaskQueue empty() {
        return new WindowTaskQueue(List.of());
    }

    /**
     * @param taskTypes requested task types.
     * @return normalized queue with default failure policy.
     */
    public static WindowTaskQueue of(Collection<TaskType> taskTypes) {
        return new WindowTaskQueue(taskTypes);
    }

    /**
     * @param taskTypes requested task types.
     * @return normalized queue with default failure policy.
     */
    public static WindowTaskQueue of(TaskType... taskTypes) {
        if (taskTypes == null || taskTypes.length == 0) {
            return empty();
        }
        return new WindowTaskQueue(List.of(taskTypes));
    }

    /**
     * @param taskType single requested task type.
     * @return one-task queue, or empty when the task type is invalid.
     */
    public static WindowTaskQueue single(TaskType taskType) {
        return of(taskType);
    }

    /**
     * @param failurePolicy replacement failure policy.
     * @return a new queue with the same task list and requested failure policy.
     */
    public WindowTaskQueue withFailurePolicy(WindowTaskFailurePolicy failurePolicy) {
        return new WindowTaskQueue(taskTypes, failurePolicy);
    }

    /** @return immutable normalized task type list. */
    public List<TaskType> getTaskTypes() {
        return taskTypes;
    }

    /** @return queue failure policy. */
    public WindowTaskFailurePolicy getFailurePolicy() {
        return failurePolicy;
    }

    /** @return first task type, or UNKNOWN when the queue is empty. */
    public TaskType firstTaskType() {
        return taskTypes.isEmpty() ? TaskType.UNKNOWN : taskTypes.get(0);
    }

    /** @return true when the queue has no runnable task types. */
    public boolean isEmpty() {
        return taskTypes.isEmpty();
    }

    /** @return number of runnable task types. */
    public int size() {
        return taskTypes.size();
    }

    /** @return compact task-code list and policy text for logs. */
    public String toLogText() {
        if (taskTypes.isEmpty()) {
            return "[] policy=" + failurePolicy;
        }
        String queueText = taskTypes.stream()
                .map(TaskType::getCode)
                .toList()
                .toString();
        return queueText + " policy=" + failurePolicy;
    }

    /** @return display-name list for UI, or {@code -} for an empty queue. */
    public String toDisplayText() {
        if (taskTypes.isEmpty()) {
            return "-";
        }
        return taskTypes.stream()
                .map(TaskType::getDisplayName)
                .toList()
                .toString();
    }

    private static List<TaskType> normalize(Collection<TaskType> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        List<TaskType> normalized = new ArrayList<>();
        for (TaskType value : values) {
            if (value != null && value != TaskType.UNKNOWN) {
                normalized.add(value);
            }
        }
        return List.copyOf(normalized);
    }
}
