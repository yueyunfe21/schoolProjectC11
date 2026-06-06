package com.bot.dhxy.model.metrics;

/**
 * Normalized status for one automation metric event.
 *
 * <p>This status is intentionally smaller than task-specific result enums so dashboard aggregation
 * can compare events from tasks, dialog handling, OCR, and maintenance with one vocabulary.</p>
 */
public enum AutomationMetricStatus {
    STARTED,
    SUCCESS,
    FAILED,
    STOPPED,
    SKIPPED,
    WARNING,
    FATAL,
    INFO
}
