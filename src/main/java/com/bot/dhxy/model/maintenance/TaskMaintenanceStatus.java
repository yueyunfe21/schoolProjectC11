package com.bot.dhxy.model.maintenance;

/**
 * Stable status returned by the shared task-maintenance scheduler.
 */
public enum TaskMaintenanceStatus {
    NO_ACTION,
    BROADCAST_HANDLED,
    BROADCAST_FAILED,
    INTERRUPTED,
    CLOUD_REQUIRED_FAILURE,
    SUMMON_SKILL_DISABLED,
    SUMMON_SKILL_NOT_DUE,
    SUMMON_SKILL_DEFERRED,
    SUMMON_SKILL_ROUND_ALREADY_CLAIMED,
    SUMMON_SKILL_CLEANED,
    SUMMON_SKILL_FAILED_RETRY_LATER
}
