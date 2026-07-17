package com.bot.dhxy.cloud.remote;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder
@Jacksonized
public class RemoteLeftTopStatusFact {
    State state;
    double openScore;
    double closedScore;
    Integer clickX;
    Integer clickY;
    RemoteCoordinateSpace coordinateSpace;

    public enum State {
        OPEN,
        CLOSED,
        UNKNOWN,
        CAPTURE_FAILED
    }
}
