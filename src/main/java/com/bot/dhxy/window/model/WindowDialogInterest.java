package com.bot.dhxy.window.model;

import com.bot.dhxy.service.dialog.DialogOperation;
import com.bot.dhxy.task.model.TaskType;
import lombok.Builder;
import lombok.Value;

import java.util.List;

/**
 * Per-window declaration of task-specific dialog operations the current task is willing to accept.
 *
 * <p>The window watcher is generic and must not know 五倍/修罗 template catalogs. Task code writes
 * this interest before it expects a business dialog, and watcher-side preparation providers may then
 * prepare only the listed operations for the selected task. Expiration is epoch millis; {@code 0}
 * means the interest should be treated as already expired.</p>
 */
@Value
@Builder(toBuilder = true)
public class WindowDialogInterest {
    TaskType taskType;
    List<DialogOperation> operations;
    String source;
    @Builder.Default
    long createdAtMs = System.currentTimeMillis();
    long expiresAtMs;

    public boolean isExpired(long nowMs) {
        return expiresAtMs <= 0L || nowMs > expiresAtMs;
    }

    public boolean supports(TaskType expectedTaskType, DialogOperation operation) {
        return taskType == expectedTaskType
                && operation != null
                && operations != null
                && operations.contains(operation);
    }
}
