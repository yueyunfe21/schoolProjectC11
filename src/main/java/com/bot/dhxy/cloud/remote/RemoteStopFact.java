package com.bot.dhxy.cloud.remote;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder
@Jacksonized
public class RemoteStopFact {
    String taskRunId;
    long stopEpoch;
    boolean stopRequested;
}
