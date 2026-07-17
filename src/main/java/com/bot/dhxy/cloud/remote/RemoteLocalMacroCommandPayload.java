package com.bot.dhxy.cloud.remote;

/** Closed DHXY wire-command variants accepted by LOCAL_MACRO. */
public sealed interface RemoteLocalMacroCommandPayload
        permits RemoteBagReturnItemMacroCommandPayload,
                RemoteBagUseIncenseMacroCommandPayload,
                RemoteNavigateInCurrentMapMacroCommandPayload,
                RemoteUiCleanMacroCommandPayload,
                RemoteDialogDetectionMacroCommandPayload,
                RemotePlayerStateFirstAidMacroCommandPayload,
                RemoteDialogPreparedActionValidationMacroCommandPayload,
                RemoteDialogOptionOcrImageMacroCommandPayload,
                RemoteDialogOptionOcrWordsMacroCommandPayload,
                RemoteDialogWhiteStoryTemplateMacroCommandPayload {

    RemoteLocalMacroKind getMacroKind();
}
