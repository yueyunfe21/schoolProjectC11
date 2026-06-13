package com.bot.dhxy.task.wubei;

/**
 * One explicit step in the 五倍 leader workflow.
 *
 * <p>Hot-start and fallback code should move to one of these phases instead of running a hidden
 * side path. That keeps "where do we resume" visible in logs and review.</p>
 */
public enum WubeiPhase {
    HOT_START_DETECT,
    ROUTE_TO_MAIN_TASK,
    ACCEPT_TASK,
    READ_TRACKER,
    AFTER_ACCEPT_MAINTENANCE_CHECK,
    BEFORE_TRACKER_PATHING_MAINTENANCE_CHECK,
    TRACKER_PATHING,
    RESOLVE_AFTER_PATHING,
    ENTER_BATTLE,
    WAIT_BATTLE_FINISH,
    POST_BATTLE_RECOVER,
    RETURN_HOME,
    WAIT_TEAM_RETURN,
    ROUND_DONE,
    FAILED,
    STOPPED;

    public boolean isTerminal() {
        return this == ROUND_DONE || this == FAILED || this == STOPPED;
    }
}
