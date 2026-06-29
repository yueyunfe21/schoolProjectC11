package com.bot.dhxy.task.xiuluo;

/**
 * One explicit step in the Xiuluo leader workflow.
 *
 * <p>The phase is the only place where Xiuluo V2 decides "where to resume". Hot-start code should
 * return a {@link XiuluoRoundContext} with one of these phases instead of running task logic directly.</p>
 */
public enum XiuluoPhase {
    PREPARE_ROUND,
    ACCEPT_TASK_NAVIGATE_TO_NPC,
    ACCEPT_TASK_CLICK_NPC,
    ACCEPT_TASK_DIALOG,
    READ_OBJECTIVE,
    AFTER_ACCEPT_MAINTENANCE_CHECK,
    BEFORE_ROUTE_MAINTENANCE_CHECK,
    TRY_TRACKER_SHORTCUT,
    WAIT_TRACKER_SHORTCUT_PATHING,
    NAVIGATE_TO_TARGET,
    CLICK_TARGET_NPC,
    CONFIRM_ENTER_BATTLE,
    WAIT_COMBAT,
    RETURN_HOME,
    NAVIGATE_BACK_TO_START,
    WAIT_TEAM_READY,
    WAIT_TEAM_RETURN,
    ROUND_DONE,
    FAILED,
    STOPPED;

    public boolean isTerminal() {
        return this == ROUND_DONE || this == FAILED || this == STOPPED;
    }
}
