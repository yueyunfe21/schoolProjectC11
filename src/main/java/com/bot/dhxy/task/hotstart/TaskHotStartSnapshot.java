package com.bot.dhxy.task.hotstart;

import com.bot.dhxy.service.DialogService;

public record TaskHotStartSnapshot(
        String taskCode,
        String source,
        TaskHotStartScreenState state,
        DialogService.DialogType dialogType
) {
    public boolean hasDialog() {
        return state == TaskHotStartScreenState.OPTION_DIALOG
                || state == TaskHotStartScreenState.STORY_DIALOG;
    }
}
