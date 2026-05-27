package com.bot.dhxy.task.hotstart;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.Accessors;

import com.bot.dhxy.model.dialog.DialogType;

@Value

@Builder

@AllArgsConstructor(access = AccessLevel.PUBLIC)

@Accessors(fluent = true)

public class TaskHotStartSnapshot {

    String taskCode;

    String source;

    TaskHotStartScreenState state;

    DialogType dialogType;

    public boolean hasDialog() {
        return state == TaskHotStartScreenState.OPTION_DIALOG
                || state == TaskHotStartScreenState.STORY_DIALOG;
    }


}
