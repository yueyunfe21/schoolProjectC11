package com.bot.dhxy.cloud.remote;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class RemoteClientSessionRef {
    String tenantId;
    String userId;
    String deviceId;
    String clientSessionId;
}
