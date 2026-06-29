package com.bot.dhxy.model.metrics;

/**
 * Stable business event names written to automation metrics JSONL.
 *
 * <p>These values cross task/service boundaries and may be consumed by the local dashboard or a
 * future uploader, so prefer adding a new enum value over changing an existing meaning.</p>
 */
public enum AutomationMetricEventType {
    TASK_STARTED,
    TASK_FINISHED,
    TASK_ROUND_STARTED,
    TASK_ROUND_FINISHED,
    TASK_ROUND_STAGE,
    TASK_TRANSACTION,
    XIULUO_FAILURE_CASE,
    AUTO_COMBAT_WARNING,
    SYSTEM_WARNING
}
