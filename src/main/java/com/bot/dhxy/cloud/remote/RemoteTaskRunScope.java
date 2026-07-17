package com.bot.dhxy.cloud.remote;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder
@Jacksonized
public class RemoteTaskRunScope {
    String tenantId;
    String userId;
    String deviceId;
    String clientSessionId;
}
