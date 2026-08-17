package com.bot.dhxy.cloud.turn.local;

import com.bot.dhxy.cloud.turn.LocalServiceExecution;
import com.bot.dhxy.core.ImageFinder;
import com.bot.dhxy.driver.BoundWindowCaptureService;
import com.bot.dhxy.window.model.WindowNativeBinding;
import com.bot.dhxy.window.runtime.WindowNativeBindingRefreshService;
import com.bot.dhxy.window.runtime.WindowRuntimeContext;
import com.bot.dhxy.window.runtime.WindowTaskContextHolder;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;

/** Performs the paired left-top switch match locally for the exact Runner-bound window. */
@Slf4j
@Component
public final class LeftTopStatusLocalOperationExecutor {

    private static final int ROI_X = 8;
    private static final int ROI_Y = 147;
    private static final int ROI_WIDTH = 16;
    private static final int ROI_HEIGHT = 29;
    private static final double MATCH_RATE = 0.90D;
    private static final double MATCH_MARGIN = 0.02D;
    private static final Path OPEN_TEMPLATE = Path.of("images", "template", "status", "left_top_open.png");
    private static final Path CLOSED_TEMPLATE = Path.of("images", "template", "status", "left_top_closed.png");

    private final WindowTaskContextHolder contextHolder;
    private final WindowNativeBindingRefreshService bindingRefreshService;
    private final BoundWindowCaptureService captureService;
    private final ObjectMapper objectMapper;

    public LeftTopStatusLocalOperationExecutor(WindowTaskContextHolder contextHolder,
                                               WindowNativeBindingRefreshService bindingRefreshService,
                                               BoundWindowCaptureService captureService,
                                               ObjectMapper objectMapper) {
        this.contextHolder = Objects.requireNonNull(contextHolder, "contextHolder");
        this.bindingRefreshService = Objects.requireNonNull(bindingRefreshService, "bindingRefreshService");
        this.captureService = Objects.requireNonNull(captureService, "captureService");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
    }

    /**
     * Capture and match the verified window-relative {@code 8,147,16,29} ROI without sending input.
     *
     * @param deviceId nonblank action-envelope device identity.
     * @param windowId nonblank action-envelope logical window identity.
     * @return completed typed JSON observation; capture/mechanical uncertainty is represented as a terminal observation.
     */
    public LocalServiceExecution execute(String deviceId, String windowId) {
        WindowRuntimeContext context = contextHolder.rawCurrent().orElse(null);
        if (context == null || !Objects.equals(context.getWindowId(), windowId)
                || deviceId == null || deviceId.isBlank()) {
            return complete(Observation.captureFailed());
        }
        WindowNativeBinding binding = bindingRefreshService.refreshAndCommit(context).orElse(null);
        if (binding == null || !binding.hasNativeHandle() || !binding.hasGeometry()) {
            return complete(Observation.captureFailed());
        }
        BufferedImage roi = captureService.captureRegionFast(
                        binding, binding.getX(), binding.getY(),
                        binding.getX() + ROI_X, binding.getY() + ROI_Y,
                        binding.getX() + ROI_X + ROI_WIDTH, binding.getY() + ROI_Y + ROI_HEIGHT)
                .map(BoundWindowCaptureService.CaptureResult::image)
                .orElse(null);
        if (roi == null) {
            return complete(Observation.captureFailed());
        }
        try {
            BufferedImage open = ImageIO.read(OPEN_TEMPLATE.toFile());
            BufferedImage closed = ImageIO.read(CLOSED_TEMPLATE.toFile());
            if (!fits(roi, open) || !fits(roi, closed)) {
                return complete(Observation.unknown(-1.0D, -1.0D));
            }
            double[] openMatch = ImageFinder.find(roi, open, -1.0D);
            double[] closedMatch = ImageFinder.find(roi, closed, -1.0D);
            if (openMatch == null || closedMatch == null) {
                return complete(Observation.unknown(-1.0D, -1.0D));
            }
            Observation observation = resolve(binding, openMatch, closedMatch);
            log.info("[left-top-status] local runner probe windowId={} status={} openScore={} closedScore={}",
                    windowId, observation.status(), observation.openScore(), observation.closedScore());
            return complete(observation);
        } catch (IOException | RuntimeException failure) {
            log.warn("[left-top-status] local runner probe failed windowId={} reason={}",
                    windowId, failure.getMessage());
            return complete(Observation.unknown(-1.0D, -1.0D));
        } finally {
            roi.flush();
        }
    }

    private LocalServiceExecution complete(Observation observation) {
        try {
            return LocalServiceExecution.completed(
                    "LEFT_TOP_STATUS_OBSERVED", objectMapper.writeValueAsString(observation), null);
        } catch (JsonProcessingException failure) {
            return LocalServiceExecution.failed("LEFT_TOP_STATUS_SERIALIZATION_FAILED", null);
        }
    }

    private static Observation resolve(WindowNativeBinding binding, double[] open, double[] closed) {
        double openScore = open[2];
        double closedScore = closed[2];
        if (openScore >= MATCH_RATE && openScore >= closedScore + MATCH_MARGIN) {
            return new Observation(Status.OPEN, openScore, closedScore,
                    binding.getX() + ROI_X + (int) Math.round(open[0]),
                    binding.getY() + ROI_Y + (int) Math.round(open[1]));
        }
        if (closedScore >= MATCH_RATE && closedScore > openScore) {
            return new Observation(Status.CLOSED, openScore, closedScore, null, null);
        }
        return Observation.unknown(openScore, closedScore);
    }

    private static boolean fits(BufferedImage source, BufferedImage template) {
        return template != null && template.getWidth() > 0 && template.getHeight() > 0
                && template.getWidth() <= source.getWidth() && template.getHeight() <= source.getHeight();
    }

    private enum Status {
        OPEN,
        CLOSED,
        UNKNOWN,
        CAPTURE_FAILED
    }

    private record Observation(Status status,
                               double openScore,
                               double closedScore,
                               Integer clickX,
                               Integer clickY) {
        private static Observation captureFailed() {
            return new Observation(Status.CAPTURE_FAILED, -1.0D, -1.0D, null, null);
        }

        private static Observation unknown(double openScore, double closedScore) {
            return new Observation(Status.UNKNOWN, openScore, closedScore, null, null);
        }
    }
}
