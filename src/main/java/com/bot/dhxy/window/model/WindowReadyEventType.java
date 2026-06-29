package com.bot.dhxy.window.model;

/**
 * Soft wake signal types published by window-level observers.
 */
public enum WindowReadyEventType {
    PATHING_TERMINAL,
    TASK_ATTENTION_REQUIRED,
    PREPARED_ACTION_READY,
    PRE_BATTLE_TIMEOUT,
    COMBAT_STATE_CHANGED
}
