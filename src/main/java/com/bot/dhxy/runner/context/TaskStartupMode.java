package com.bot.dhxy.runner.context;

/**
 * Describes how a task reached its startup boundary.
 */
public enum TaskStartupMode {
    NORMAL,
    AFTER_COMBAT_EXIT_STARTUP,
    CLEAN_QUEUE_TRANSITION
}
