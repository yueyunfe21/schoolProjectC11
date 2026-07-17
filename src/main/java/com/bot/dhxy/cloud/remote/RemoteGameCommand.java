package com.bot.dhxy.cloud.remote;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder
@Jacksonized
public class RemoteGameCommand {
    int contractVersion;
    RemoteGameOperation operation;
    String requestId;
    String actionId;
    String taskRunId;
    /**
     * Coordinator run revision the cloud built this request against. Boxed so the strict
     * transport schema can reject an absent field instead of silently reading 0.
     */
    Long runRevision;
    /**
     * Optional paused read-only observation marker. Key absent = normal command. Explicit null
     * fails deserialization (Nulls.FAIL) so the wire has exactly one canonical representation
     * per digest; unknown enum values fail deserialization the same way.
     */
    @JsonSetter(nulls = Nulls.FAIL)
    RemoteObservationMode observationMode;
    RemoteSemanticAddress semanticAddress;
    RemoteWindowBindingRef window;
    RemoteStopRef stop;
    long timeoutMs;
    String requestDigest;
    JsonNode payload;
}
