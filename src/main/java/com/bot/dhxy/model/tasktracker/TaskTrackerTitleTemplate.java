package com.bot.dhxy.model.tasktracker;

import lombok.Builder;
import lombok.Value;

/**
 * Template definition for a yellow task title in the left task-tracker panel.
 *
 * @param taskKey stable task-specific key used by the caller, for example {@code wubei.baoxiang_miqing}.
 * @param displayName human-readable title shown in logs.
 * @param templatePath repository-relative path to the washed black/white yellow-title template.
 * @param threshold OpenCV template-match threshold in the washed panel image.
 */
@Value
@Builder
public class TaskTrackerTitleTemplate {
    String taskKey;
    String displayName;
    String templatePath;

    @Builder.Default
    double threshold = 0.82;
}
