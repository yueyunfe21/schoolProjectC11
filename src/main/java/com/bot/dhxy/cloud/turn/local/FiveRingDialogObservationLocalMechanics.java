package com.bot.dhxy.cloud.turn.local;

import com.bot.dhxy.cloud.turn.TurnFrame;
import com.bot.dhxy.cloud.turn.TurnPngCodec;
import com.bot.dhxy.cloud.turn.protocol.TurnFramePurpose;
import com.bot.dhxy.cloud.turn.protocol.TurnRegion;
import com.bot.dhxy.core.GameClientTracker;
import com.bot.dhxy.tools.CoordinateHelper;
import org.springframework.stereotype.Component;

import java.awt.image.BufferedImage;

/** Fixed dialog ROI capture for the five-ring current-action continuation. */
@Component
public final class FiveRingDialogObservationLocalMechanics {
    private final GameClientTracker tracker;
    private final CoordinateHelper coordinates;
    private final TurnPngCodec pngCodec;

    public FiveRingDialogObservationLocalMechanics(
            GameClientTracker tracker, CoordinateHelper coordinates, TurnPngCodec pngCodec) {
        this.tracker = tracker;
        this.coordinates = coordinates;
        this.pngCodec = pngCodec;
    }

    public TurnFrame captureOption(int stepIndex) {
        return capture("five-ring-accept-option", 250, 345, 529, 143, stepIndex);
    }

    private TurnFrame capture(String reason, int x, int y, int width, int height, int stepIndex) {
        int[] rect = coordinates.getScaledRect(x, y, width, height);
        BufferedImage image = tracker.captureToMemory(reason, rect[0], rect[1], rect[2], rect[3]);
        if (image == null) {
            throw new IllegalStateException(reason + " capture failed");
        }
        try {
            return pngCodec.encode(image, TurnFramePurpose.FIVERING_DIALOG_OBSERVATION,
                    new TurnRegion(rect[0], rect[1], image.getWidth(), image.getHeight()), stepIndex);
        } finally {
            image.flush();
        }
    }
}
