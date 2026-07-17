package com.bot.dhxy.input.action;

/** Typed detailed-input safety decision used by the remote execution path. */
public enum InputActionSafetyReason {
    CLEAR,
    STOP_REQUESTED,
    TASK_RUN_MISMATCH,
    WINDOW_BINDING_CHANGED;

    public boolean blocksInput() {
        return this != CLEAR;
    }

    String diagnosticPrefix() {
        return switch (this) {
            case CLEAR -> "safety-clear";
            case STOP_REQUESTED -> "external-stop-requested";
            case TASK_RUN_MISMATCH -> "task-run-mismatch";
            case WINDOW_BINDING_CHANGED -> "window-binding-changed";
        };
    }
}
