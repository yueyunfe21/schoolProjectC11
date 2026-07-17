package com.bot.dhxy.cloud.remote;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder
@Jacksonized
public class RemoteTaskRunActionResponse {
    int contractVersion;
    RemoteTaskRunAction action;
    boolean success;
    RemoteTaskRunError error;
    RemoteTaskRunBinding binding;
    RemoteTaskRunReceipt receipt;
}
