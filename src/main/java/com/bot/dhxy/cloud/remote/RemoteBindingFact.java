package com.bot.dhxy.cloud.remote;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder
@Jacksonized
public class RemoteBindingFact {
    String windowId;
    String nativeHandle;
    long processId;
    long playerIdentityEpoch;
    String title;
    String className;
}
