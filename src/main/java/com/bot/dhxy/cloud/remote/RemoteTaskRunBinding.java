package com.bot.dhxy.cloud.remote;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

/** Exact nested lifecycle binding returned by the cloud coordinator. */
@Value
@Builder
@Jacksonized
public class RemoteTaskRunBinding {
    RemoteTaskRunScope scope;
    String taskRunId;
    String startRequestId;
    String taskType;
    RemoteTaskRunWindow window;
    long stopEpoch;
    long runRevision;
    RemoteTaskRunWireStatus status;
}
