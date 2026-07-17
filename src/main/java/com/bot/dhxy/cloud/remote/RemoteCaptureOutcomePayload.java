package com.bot.dhxy.cloud.remote;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder
@Jacksonized
public class RemoteCaptureOutcomePayload {
    String captureId;
    byte[] imageBytes;
    String imageSha256;
    Integer width;
    Integer height;
    RemoteCaptureProvider captureProvider;
    Double systemScaleRatio;
    RemoteObservedWindowBinding observedWindow;
}
