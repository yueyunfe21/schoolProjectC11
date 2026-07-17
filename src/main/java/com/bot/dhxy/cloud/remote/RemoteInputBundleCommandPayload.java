package com.bot.dhxy.cloud.remote;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.List;

@Value
@Builder
@Jacksonized
public class RemoteInputBundleCommandPayload {
    String description;
    RemoteCoordinateSpace coordinateSpace;
    List<RemoteInputActionDto> actions;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    RemoteExclusiveSessionStepRef sessionRef;
}
