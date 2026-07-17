package com.bot.dhxy.cloud.remote;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

/** Exact generic-exclusive session cursor decoded from a mechanical command payload. */
@Value
@Builder
@Jacksonized
public class RemoteExclusiveSessionStepRef {
    String exclusiveSessionId;
    long bindingGeneration;
    long step;
}
