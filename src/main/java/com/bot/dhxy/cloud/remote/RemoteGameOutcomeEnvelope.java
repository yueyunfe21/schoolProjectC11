package com.bot.dhxy.cloud.remote;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder(toBuilder = true)
@Jacksonized
public class RemoteGameOutcomeEnvelope {
    int contractVersion;
    String tenantId;
    String userId;
    String deviceId;
    String clientSessionId;
    RemoteGameOperation operation;
    String requestId;
    String actionId;
    String taskRunId;
    RemoteSemanticAddress semanticAddress;
    String requestDigest;
    String outcomeDigest;
    RemoteExecutionState executionState;
    RemoteOutcomeCode code;
    String message;
    long acceptedAtEpochMs;
    long finishedAtEpochMs;
    JsonNode payload;

    /** Decodes only the dormant tracker variants through their closed operation-specific codec. */
    public Object decodeStrictTaskTrackerPayload(RemoteOperationPayloadCodec codec) {
        if (codec == null) {
            throw new IllegalArgumentException("codec is required");
        }
        if (operation == RemoteGameOperation.TASK_TRACKER_READ) {
            return codec.readTaskTrackerReadOutcome(this);
        }
        if (operation == RemoteGameOperation.TASK_TRACKER_MATERIALIZE_ACTION) {
            return codec.readTaskTrackerMaterializeOutcome(this);
        }
        throw new IllegalStateException("operation is not a task tracker operation");
    }
}
