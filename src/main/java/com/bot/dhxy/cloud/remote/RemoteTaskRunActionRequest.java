package com.bot.dhxy.cloud.remote;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

/** Flat request shape consumed by /api/cloud/remote/task-run. */
@Value
@Builder(toBuilder = true)
@Jacksonized
public class RemoteTaskRunActionRequest {
    int contractVersion;
    RemoteTaskRunAction action;
    String tenantId;
    String userId;
    String deviceId;
    String clientSessionId;
    String startRequestId;
    String taskType;
    String taskRunId;
    Long expectedRevision;
    RemoteTaskRunWindow window;
    String requestId;
    String requestDigest;
    ResumeExecutorReadinessFact fact;
}
