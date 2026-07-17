package com.bot.dhxy.cloud.remote;

/**
 * W-BAG-MACRO-DHXY-WIRE-IMP1: closed set of local-macro kinds carried by the {@code LOCAL_MACRO}
 * operation. Mirrors the Cloud {@code LocalMacroKind} exactly.
 */
public enum RemoteLocalMacroKind {
    BAG_RETURN_ITEM,
    BAG_USE_INCENSE,
    NAVIGATE_IN_CURRENT_MAP,
    UI_CLEAN,
    DIALOG_DETECTION,
    PLAYER_STATE_FIRST_AID,
    DIALOG_PREPARED_ACTION_VALIDATION,
    DIALOG_OPTION_OCR_IMAGE,
    DIALOG_OPTION_OCR_WORDS,
    DIALOG_WHITE_STORY_TEMPLATE
}
