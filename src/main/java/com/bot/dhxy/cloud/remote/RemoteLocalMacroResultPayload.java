package com.bot.dhxy.cloud.remote;

/** Closed DHXY wire-result variants emitted by an EXECUTED LOCAL_MACRO terminal. */
public sealed interface RemoteLocalMacroResultPayload
        permits RemoteBagReturnItemMacroResultPayload,
                RemoteBagUseIncenseMacroResultPayload,
                RemoteNavigateInCurrentMapMacroResultPayload,
                RemoteUiCleanMacroResultPayload,
                RemoteDialogDetectionMacroResultPayload,
                RemotePlayerStateFirstAidMacroResultPayload,
                RemoteDialogPreparedActionValidationMacroResultPayload,
                RemoteDialogOptionOcrImageMacroResultPayload,
                RemoteDialogOptionOcrWordsMacroResultPayload,
                RemoteDialogWhiteStoryTemplateMacroResultPayload {

    RemoteLocalMacroKind getMacroKind();
}
