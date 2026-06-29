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
 * prepare only the listed operations for the selected task. The interest is phase-owned by the
 * task, not a short cache entry; {@code expiresAtMs} is kept only for legacy diagnostics and must
 * not be used as a 五倍 business clear boundary.</p>
 *
 * <p>{@code absentAllowedAtMs} is optional and only meaningful for operations whose provider has an
 * explicit "dialog absent" business result. Keeping it closed lets a task observe real dialogs early
 * without letting the watcher publish an absent result before the triggering action actually ran.</p>
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
    @Builder.Default
    long absentAllowedAtMs = 0L;

    public boolean isExpired(long nowMs) {
        return expiresAtMs > 0L && nowMs > expiresAtMs;
    }

    public boolean supports(TaskType expectedTaskType, DialogOperation operation) {
        return taskType == expectedTaskType
                && operation != null
                && operations != null
                && operations.contains(operation);
    }

    public boolean isAbsentAllowed(long nowMs) {
        return absentAllowedAtMs > 0L && nowMs >= absentAllowedAtMs;
    }
}
