package com.bot.dhxy.window.observation;

import com.bot.dhxy.cloud.turn.protocol.observation.ObservationPreparedFrame;
import com.bot.dhxy.cloud.turn.protocol.observation.ObservationPreparedFrameDemand;
import com.bot.dhxy.cloud.turn.protocol.observation.ObservationProtocolValidator;
import com.bot.dhxy.core.GameClientTracker;
import com.bot.dhxy.window.runtime.WindowRuntimeContext;
import com.bot.dhxy.window.runtime.WindowTaskContextHolder;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Objects;

/** Exact-HWND one-shot capture owner for non-pathing prepared work. */
final class ExactWindowPreparedFrameCapture implements PreparedFrameCapture {
    private final WindowRuntimeContext context;
    private final WindowTaskContextHolder contextHolder;
    private final GameClientTracker tracker;

    ExactWindowPreparedFrameCapture(WindowRuntimeContext context,
                                    WindowTaskContextHolder contextHolder,
                                    GameClientTracker tracker) {
        this.context = Objects.requireNonNull(context, "context");
        this.contextHolder = Objects.requireNonNull(contextHolder, "contextHolder");
        this.tracker = Objects.requireNonNull(tracker, "tracker");
    }

    @Override
    public ObservationPreparedFrame capture(ObservationPreparedFrameDemand demand) {
        Objects.requireNonNull(demand, "demand");
        BufferedImage image = contextHolder.callWith(context, () -> {
            // Tracker state is thread-scoped and this runs on the observation transport thread,
            // whose fresh state still carries the -1 defaults. Refresh first — exactly like the
            // shared-cycle-frame path via getScaledRect — or the capture rect starts at (-1,-1).
            if (!tracker.refreshWindowState()) {
                return null;
            }
            int baseX = tracker.getWindowBaseX();
            int baseY = tracker.getWindowBaseY();
            return tracker.captureToMemory(
                    "observation-prepared-frame-" + demand.demandId(),
                    baseX,
                    baseY,
                    baseX + ObservationProtocolValidator.TERMINAL_FRAME_WIDTH,
                    baseY + ObservationProtocolValidator.TERMINAL_FRAME_HEIGHT);
        });
        if (image == null) {
            throw new IllegalStateException("exact prepared frame capture returned null");
        }
        int width = image.getWidth();
        int height = image.getHeight();
        try {
            if (width != ObservationProtocolValidator.TERMINAL_FRAME_WIDTH
                    || height != ObservationProtocolValidator.TERMINAL_FRAME_HEIGHT) {
                throw new IllegalStateException("exact prepared frame capture returned unexpected geometry");
            }
            byte[] png = encodePng(image);
            ObservationProtocolValidator.requireExactTerminalFramePng(png, "preparedFrame.pngBytes");
            return new ObservationPreparedFrame(
                    demand.demandId(), demand.purpose(), demand.generation(),
                    0, 0, width, height, "PNG",
                    System.currentTimeMillis(), png);
        } finally {
            image.flush();
        }
    }

    private static byte[] encodePng(BufferedImage image) {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            if (!ImageIO.write(image, "png", output)) {
                throw new IllegalStateException("PNG writer unavailable");
            }
            return output.toByteArray();
        } catch (IOException failure) {
            throw new IllegalStateException("prepared frame PNG encoding failed", failure);
        }
    }
}
