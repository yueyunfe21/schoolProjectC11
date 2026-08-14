package com.bot.dhxy.window.model;

import com.bot.dhxy.task.model.TaskType;
import lombok.Builder;
import lombok.Value;

/**
 * Numeric progress owned by one exact window and one concrete task type.
 *
 * <p>This value is durable only across the user PAUSE -> PAUSE_RESUME boundary. It carries no
 * task phase, click, pathing, dialog, observation, or combat state.</p>
 */
@Value
@Builder
public class WindowTaskRunProgress {
    TaskType taskType;
    int completedRuns;
    int totalRuns;

    public String toDisplayText() {
        return totalRuns > 0 ? completedRuns + "/" + totalRuns : "-";
    }
}
