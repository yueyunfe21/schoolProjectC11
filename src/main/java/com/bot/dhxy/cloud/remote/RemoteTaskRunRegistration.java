package com.bot.dhxy.cloud.remote;

import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
public class RemoteTaskRunRegistration {
    String tenantId;
    String userId;
    String deviceId;
    String clientSessionId;
    String taskRunId;
    String startRequestId;
    String taskType;
    String windowId;
    String nativeHandle;
    long processId;
    long playerIdentityEpoch;
    long stopEpoch;
    long runRevision;
    RemoteTaskRunStatus status;
}
