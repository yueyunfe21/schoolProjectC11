package com.bot.dhxy.cloud.remote;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder
@Jacksonized
public class RemoteInputBundleOutcomePayload {
    int actionCount;
    int startedStepIndex;
    int lastCompletedStepIndex;
    String inputQueueRequestId;
    RemoteObservedWindowBinding observedWindow;
}
