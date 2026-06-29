package com.bot.dhxy.task.wubei;

/**
 * Scheduling-only reason that explains what external state a 五倍 phase is waiting for.
 *
 * <p>These values must not encode business success or failure. They only let the task runner park
 * after releasing the task turn and wake when a relevant runner/window event may have changed the
 * source-of-truth runtime state.</p>
 */
public enum WubeiWaitReason {
    WAIT_PATHING_TERMINAL,
    WAIT_ACCEPT_NPC_ROUTE,
    WAIT_PREPARED_DIALOG,
    WAIT_COMBAT_STATE_CHANGE,
    WAIT_TEAM_ATTENTION,
    WAIT_RETRY_TIMER
}
