package com.bot.dhxy.task.wuhuan;

/**
 * One explicit step in the Five-ring V2 workflow.
 *
 * <p>The phase only decides where the task should resume next. Low-level navigation, dialog, bag,
 * and task-panel operations still stay in their existing services so the validated business actions
 * are reused instead of rewritten.</p>
 */
public enum FiveRingPhase {
    PREPARE,
    BUY_SHOES,
    HANDOVER_DETECT,
    ACCEPT_TASK,
    WAIT_PATHING,
    HANDLE_DIALOG,
    SYNC_TASK_PANEL,
    FINISHED,
    FAILED,
    STOPPED;

    public boolean isTerminal() {
        return this == FINISHED || this == FAILED || this == STOPPED;
    }
}
