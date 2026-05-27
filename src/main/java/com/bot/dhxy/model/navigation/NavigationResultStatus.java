package com.bot.dhxy.model.navigation;

/**
 * Coarse navigation outcome for task code.
 *
 * <p>Detailed OCR/input evidence should stay in service logs. This status is intentionally small so
 * task state machines can decide whether to continue, retry, or fail without parsing log text.</p>
 */
public enum NavigationResultStatus {
    ARRIVED,
    PATHING_STARTED,
    SUCCESS,
    FAILED,
    STOPPED,
    INTERRUPTED,
    MAP_NOT_REACHED,
    POINT_NOT_REACHED,
    DIALOG_OPENED
}
