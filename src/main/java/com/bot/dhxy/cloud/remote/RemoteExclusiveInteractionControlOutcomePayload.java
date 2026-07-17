package com.bot.dhxy.cloud.remote;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

/** Exact local result projected into the cloud exclusive-control outcome. */
@Value
@Builder
@Jacksonized
public class RemoteExclusiveInteractionControlOutcomePayload {
    RemoteExclusiveInteractionControlCommandPayload.Command command;
    String exclusiveSessionId;
    long bindingGeneration;
    long step;
    MechanicalStatus mechanicalStatus;
    boolean ownerReleased;

    public enum MechanicalStatus {
        ACQUIRED,
        RELEASED,
        ABORTED,
        NOT_EXECUTED,
        STOPPED,
        UNKNOWN
    }
}
