package com.bot.dhxy.window.model;

/**
 * Soft wake signal types published by window-level observers.
 */
public enum WindowReadyEventType {
    PATHING_TERMINAL,
    TASK_ATTENTION_REQUIRED,
    PREPARED_ACTION_READY,
    TASK_TRACKER_NEGATIVE_READY,
    PRE_BATTLE_TIMEOUT,
    POST_COMBAT_IDLE_TIMEOUT,
    COMBAT_STATE_CHANGED,
    TEAM_RETURN_STATE_CHANGED,
    MAINTENANCE_BROADCAST_QUEUE_CHANGED,
    /**
     * CR255: the window observer confirmed a {@code DialogType.STORY} dialog covering this window.
     * This is a strongly-typed observation fact — unlike {@link #TASK_ATTENTION_REQUIRED} it has
     * exactly one meaning and may be consumed by an input-owning task boundary (修罗 accept-phase
     * NPC smart click) for one fast story click per event sequence. The observer never inputs.
     */
    STORY_DIALOG_VISIBLE
}
