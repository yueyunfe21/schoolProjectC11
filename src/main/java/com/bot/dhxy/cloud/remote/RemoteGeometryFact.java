package com.bot.dhxy.cloud.remote;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder
@Jacksonized
public class RemoteGeometryFact {
    int x;
    int y;
    int width;
    int height;
    RemoteCoordinateSpace coordinateSpace;
}
