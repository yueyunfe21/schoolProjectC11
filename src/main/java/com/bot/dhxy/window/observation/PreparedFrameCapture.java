package com.bot.dhxy.window.observation;

import com.bot.dhxy.cloud.turn.protocol.observation.ObservationPreparedFrame;
import com.bot.dhxy.cloud.turn.protocol.observation.ObservationPreparedFrameDemand;

/** Captures one Cloud-demanded exact-window frame outside the ordinary ROI sampler. */
@FunctionalInterface
interface PreparedFrameCapture {
    ObservationPreparedFrame capture(ObservationPreparedFrameDemand demand);
}
