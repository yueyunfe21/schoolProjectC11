package com.bot.dhxy.cloud.remote;

/** Cloud wire states. Local-only STOPPING is intentionally absent. */
public enum RemoteTaskRunWireStatus {
    PREPARED,
    ACTIVE,
    PAUSED,
    STOPPED,
    COMPLETED;

    public boolean isTerminal() {
        return this == STOPPED || this == COMPLETED;
    }
}
