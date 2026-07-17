package com.bot.dhxy.cloud.remote;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

/** Closed ACQUIRE/RELEASE/ABORT payload for a retained generic-exclusive session. */
@Value
@Builder
@Jacksonized
public class RemoteExclusiveInteractionControlCommandPayload {
    Command command;
    String exclusiveSessionId;
    long bindingGeneration;
    long step;

    public enum Command {
        ACQUIRE,
        RELEASE,
        ABORT
    }
}
