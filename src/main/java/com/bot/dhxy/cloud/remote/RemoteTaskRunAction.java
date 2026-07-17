package com.bot.dhxy.cloud.remote;

public enum RemoteTaskRunAction {
    PREPARE,
    STATUS,
    ACTIVATE,
    CONFIRM_EXECUTION,
    CONFIRM_RESUMED_EXECUTOR_READY,
    PAUSE,
    RESUME,
    STOP,
    COMPLETE,
    FIND_REPLACEMENT,
    STOP_REPLACEMENT
}
