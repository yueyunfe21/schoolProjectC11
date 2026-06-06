package com.bot.dhxy.model.dialog;

/**
 * Per-window lifecycle for a background dialog preparation request.
 */
public enum DialogPreparationPhase {
    NONE,
    REQUESTED,
    PREPARING,
    READY,
    FAILED
}
