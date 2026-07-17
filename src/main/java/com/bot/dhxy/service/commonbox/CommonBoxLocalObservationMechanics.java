package com.bot.dhxy.service.commonbox;

import com.bot.dhxy.core.ImageFinder;
import com.bot.dhxy.driver.BoundWindowCaptureService;
import com.bot.dhxy.window.model.WindowNativeBinding;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@Service
public final class CommonBoxLocalObservationMechanics {

    private static final int ROI_LEFT = 623;
    private static final int ROI_TOP = 590;
    private static final int ROI_RIGHT = 682;
    private static final int ROI_BOTTOM = 618;
    private static final int ROI_WIDTH = ROI_RIGHT - ROI_LEFT;
    private static final int ROI_HEIGHT = ROI_BOTTOM - ROI_TOP;
    private static final double TEMPLATE_THRESHOLD = 0.86D;
    private static final String TEMPLATE_PATH = "images/template/common/leader_box_marker.png";

    private final BoundWindowCaptureService captureService;
    private final Object templateLock = new Object();
    private volatile BufferedImage cachedTemplate;

    public CommonBoxLocalObservationMechanics(BoundWindowCaptureService captureService) {
        this.captureService = Objects.requireNonNull(captureService, "captureService");
    }

    /**
     * Observes the fixed common-box marker region for one exact native-window binding.
     *
     * @param binding exact binding accepted by the caller's current registration/fence gate; its geometry is
     *                screen-absolute pixels and must be non-null with a native handle and positive dimensions
     * @return a non-null closed result; matched coordinates are window-client pixels, while every non-matched
     *         status carries null coordinates, score, and timestamp
     */
    public ObservationResult observe(WindowNativeBinding binding) {
        if (binding == null || !binding.hasNativeHandle() || !binding.hasGeometry()) {
            return new ObservationResult(Status.CAPTURE_UNAVAILABLE, null, null, null, null);
        }

        int baseX = binding.getX();
        int baseY = binding.getY();
        Optional<BoundWindowCaptureService.CaptureResult> captured;
        try {
            captured = captureService.captureRegion(
                    binding,
                    baseX,
                    baseY,
                    baseX + ROI_LEFT,
                    baseY + ROI_TOP,
                    baseX + ROI_RIGHT,
                    baseY + ROI_BOTTOM);
        } catch (RuntimeException e) {
            log.warn("[common-box] local observation capture failed: hwnd={} title={} reason={}",
                    binding.getNativeHandle(), binding.getTitle(), e.getMessage(), e);
            return new ObservationResult(Status.CAPTURE_UNAVAILABLE, null, null, null, null);
        }

        if (captured == null || captured.isEmpty() || captured.get().image() == null) {
            return new ObservationResult(Status.CAPTURE_UNAVAILABLE, null, null, null, null);
        }

        BufferedImage frame = captured.get().image();
        try {
            BufferedImage template = cachedTemplate();
            if (template == null) {
                return new ObservationResult(Status.TEMPLATE_UNAVAILABLE, null, null, null, null);
            }

            double[] match = ImageFinder.find(frame, template, TEMPLATE_THRESHOLD);
            if (match == null) {
                return new ObservationResult(Status.NOT_MATCHED, null, null, null, null);
            }
            if (!isValidMatch(match)) {
                return new ObservationResult(Status.MECHANICS_FAILED, null, null, null, null);
            }

            int roundedX = (int) Math.round(match[0]);
            int roundedY = (int) Math.round(match[1]);
            long matchedAtEpochMs = System.currentTimeMillis();
            if (roundedX < 0 || roundedX >= ROI_WIDTH
                    || roundedY < 0 || roundedY >= ROI_HEIGHT
                    || matchedAtEpochMs <= 0L) {
                return new ObservationResult(Status.MECHANICS_FAILED, null, null, null, null);
            }
            return new ObservationResult(
                    Status.MATCHED,
                    ROI_LEFT + roundedX,
                    ROI_TOP + roundedY,
                    match[2],
                    matchedAtEpochMs);
        } catch (RuntimeException e) {
            log.warn("[common-box] local observation mechanics failed: hwnd={} title={} reason={}",
                    binding.getNativeHandle(), binding.getTitle(), e.getMessage(), e);
            return new ObservationResult(Status.MECHANICS_FAILED, null, null, null, null);
        } finally {
            frame.flush();
        }
    }

    private boolean isValidMatch(double[] match) {
        return match.length >= 3
                && Double.isFinite(match[0])
                && Double.isFinite(match[1])
                && Double.isFinite(match[2])
                && match[0] >= 0.0D
                && match[0] < ROI_WIDTH
                && match[1] >= 0.0D
                && match[1] < ROI_HEIGHT
                && match[2] >= TEMPLATE_THRESHOLD;
    }

    private BufferedImage cachedTemplate() {
        BufferedImage template = cachedTemplate;
        if (template != null) {
            return template;
        }
        synchronized (templateLock) {
            if (cachedTemplate != null) {
                return cachedTemplate;
            }
            try {
                BufferedImage loaded = ImageIO.read(Path.of(TEMPLATE_PATH).toFile());
                if (loaded != null) {
                    cachedTemplate = loaded;
                }
                return loaded;
            } catch (Exception e) {
                log.warn("[common-box] local observation template load failed: path={} reason={}",
                        TEMPLATE_PATH, e.getMessage(), e);
                return null;
            }
        }
    }

    public enum Status {
        MATCHED,
        NOT_MATCHED,
        CAPTURE_UNAVAILABLE,
        TEMPLATE_UNAVAILABLE,
        MECHANICS_FAILED
    }

    public record ObservationResult(
            Status status,
            Integer clientX,
            Integer clientY,
            Double matchScore,
            Long matchedAtEpochMs) {

        public ObservationResult {
            Objects.requireNonNull(status, "status");
            boolean matched = status == Status.MATCHED;
            boolean hasAllMatchFields = clientX != null
                    && clientY != null
                    && matchScore != null
                    && matchedAtEpochMs != null;
            if (matched != hasAllMatchFields) {
                throw new IllegalArgumentException("match fields must be all present only for MATCHED");
            }
            if (!matched && (clientX != null || clientY != null || matchScore != null || matchedAtEpochMs != null)) {
                throw new IllegalArgumentException("non-matched result must not carry match fields");
            }
            if (matched && (!Double.isFinite(matchScore)
                    || matchScore < TEMPLATE_THRESHOLD
                    || matchedAtEpochMs <= 0L
                    || clientX < ROI_LEFT
                    || clientX >= ROI_RIGHT
                    || clientY < ROI_TOP
                    || clientY >= ROI_BOTTOM)) {
                throw new IllegalArgumentException("invalid MATCHED coordinates, score, or timestamp");
            }
        }
    }
}
