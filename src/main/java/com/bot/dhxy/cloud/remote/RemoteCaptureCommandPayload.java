package com.bot.dhxy.cloud.remote;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder
@Jacksonized
public class RemoteCaptureCommandPayload {
    String captureId;
    RemoteCaptureRegion region;
    RemoteCaptureImageFormat imageFormat;
    RemoteCapturePurpose capturePurpose;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    RemoteExclusiveSessionStepRef sessionRef;
}
