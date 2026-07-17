package com.bot.dhxy.cloud.remote;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder
@Jacksonized
public class RemoteTaskRunWindow {
    String windowId;
    String nativeHandle;
    long processId;
    long playerIdentityEpoch;
}
