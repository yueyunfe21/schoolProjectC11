package com.bot.dhxy.cloud.remote;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder
@Jacksonized
public class RemoteWindowFactOutcomePayload {
    RemoteWindowFactKind factKind;
    JsonNode fact;
}
