package com.bot.dhxy.task.hotstart;

import com.bot.dhxy.model.dialog.DialogType;

public record TaskHotStartSnapshot(
        String taskCode,
        String source,
        TaskHotStartScreenState state,
        DialogType dialogType
) {
    public boolean hasDialog() {
        return state == TaskHotStartScreenState.OPTION_DIALOG
                || state == TaskHotStartScreenState.STORY_DIALOG;
    }
}
