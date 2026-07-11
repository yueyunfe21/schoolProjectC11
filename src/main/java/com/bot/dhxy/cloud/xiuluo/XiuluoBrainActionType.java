package com.bot.dhxy.cloud.xiuluo;

public enum XiuluoBrainActionType {
    EXECUTE_PHASE,
    RUN_CLEANUP,
    WAIT_FOR_EVENT,
    COMPLETE_ROUND,
    /**
     * CR230: first-class round restart. The client archives a failure case, cleans up, yields the
     * task turn and rebuilds the round context with the same round number, then reports EXECUTED.
     */
    RESTART_ROUND,
    FAIL_TASK,
    STOP_TASK
}
