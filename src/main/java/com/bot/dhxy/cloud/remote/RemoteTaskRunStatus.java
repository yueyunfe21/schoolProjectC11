package com.bot.dhxy.cloud.remote;

public enum RemoteTaskRunStatus {
    PREPARED,
    ACTIVE,
    PAUSED,
    STOPPING,
    STOPPED,
    COMPLETED;

    public boolean isTerminal() {
        return this == STOPPED || this == COMPLETED;
    }

    public boolean acceptsNewCommand() {
        return this == ACTIVE;
    }
}
