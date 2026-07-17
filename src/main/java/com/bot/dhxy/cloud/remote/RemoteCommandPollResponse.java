package com.bot.dhxy.cloud.remote;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder
@Jacksonized
public class RemoteCommandPollResponse {
    RemoteCommandPollStatus status;
    String cloudIncarnationId;
    long retryAfterMs;
    RemoteGameCommand command;
    RemoteFinalConsumedAck finalConsumedAck;
}
