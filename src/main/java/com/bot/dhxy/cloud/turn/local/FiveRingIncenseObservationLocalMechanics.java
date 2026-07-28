package com.bot.dhxy.cloud.turn.local;

import com.bot.dhxy.cloud.turn.TurnFrame;
import com.bot.dhxy.cloud.turn.TurnPngCodec;
import com.bot.dhxy.cloud.turn.protocol.TurnFramePurpose;
import com.bot.dhxy.cloud.turn.protocol.TurnRegion;
import com.bot.dhxy.core.GameClientTracker;
import com.bot.dhxy.tools.CoordinateHelper;
import org.springframework.stereotype.Component;

import java.awt.image.BufferedImage;
import java.util.Objects;

/** Fixed exact-window crop mechanics for the raw incense status observation. */
@Component
public final class FiveRingIncenseObservationLocalMechanics {

    private static final int STATUS_PANEL_X = 901;
    private static final int STATUS_PANEL_Y = 123;
    private static final int STATUS_PANEL_W = 123;
    private static final int STATUS_PANEL_H = 34;

    private final GameClientTracker tracker;
    private final CoordinateHelper coordinateHelper;
    private final TurnPngCodec pngCodec;

    public FiveRingIncenseObservationLocalMechanics(
            GameClientTracker tracker,
            CoordinateHelper coordinateHelper,
            TurnPngCodec pngCodec) {
        this.tracker = Objects.requireNonNull(tracker, "tracker");
        this.coordinateHelper = Objects.requireNonNull(coordinateHelper, "coordinateHelper");
        this.pngCodec = Objects.requireNonNull(pngCodec, "pngCodec");
    }

    /** Capture exactly one raw 123x34 baseline status-panel ROI while the caller keeps the bag open. */
    public TurnFrame capture(int sourceStepIndex) {
        int[] rect = coordinateHelper.getScaledRect(
                STATUS_PANEL_X, STATUS_PANEL_Y, STATUS_PANEL_W, STATUS_PANEL_H);
        BufferedImage image = tracker.captureToMemory(
                "five-ring-incense-continuation", rect[0], rect[1], rect[2], rect[3]);
        if (image == null) {
            throw new IllegalStateException("incense status observation capture failed");
        }
        try {
            TurnRegion region = new TurnRegion(
                    rect[0], rect[1], image.getWidth(), image.getHeight());
            return pngCodec.encode(
                    image,
                    TurnFramePurpose.FIVERING_INCENSE_OBSERVATION,
                    region,
                    sourceStepIndex);
        } finally {
            image.flush();
        }
    }
}
