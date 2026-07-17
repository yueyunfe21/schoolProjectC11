package com.bot.dhxy.service.tasktracker;

import com.bot.dhxy.core.ImageFinder;
import com.bot.dhxy.driver.BoundWindowCaptureService;
import com.bot.dhxy.window.model.WindowNativeBinding;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;

/**
 * Exact-window, screenshot-only mechanics for locating the task-tracker panel rectangle.
 */
@Slf4j
@Service
public final class TaskTrackerPanelRectLocalObservationMechanics {

    private static final String ANCHOR_TEMPLATE_PATH = "images/template/task/wubei_tracker_anchor.png";
    private static final int SEARCH_LEFT = 6;
    private static final int SEARCH_TOP = 196;
    private static final int SEARCH_RIGHT = 207;
    private static final int SEARCH_BOTTOM = 551;
    private static final int SEARCH_WIDTH = SEARCH_RIGHT - SEARCH_LEFT;
    private static final int SEARCH_HEIGHT = SEARCH_BOTTOM - SEARCH_TOP;
    private static final double ANCHOR_THRESHOLD = 0.82D;
    private static final int PANEL_FROM_ANCHOR_LEFT = -96;
    private static final int PANEL_FROM_ANCHOR_TOP = 12;
    private static final int PANEL_FROM_ANCHOR_RIGHT = 86;
    private static final int PANEL_FROM_ANCHOR_BOTTOM = 350;
    private static final int SAFE_ANCHOR_MAX_X = 164;
    private static final int SAFE_ANCHOR_MAX_Y = 353;

    private final BoundWindowCaptureService captureService;

    public TaskTrackerPanelRectLocalObservationMechanics(BoundWindowCaptureService captureService) {
        this.captureService = Objects.requireNonNull(captureService, "captureService");
    }

    /**
     * Observes the committed narrow anchor region of one caller-supplied exact native-window binding.
     *
     * @param binding exact binding whose origin is screen-absolute pixels and whose client geometry must be
     *                non-null, native-handle backed, and large enough for the fixed observation region
     * @return a non-null closed result; only {@link State#PRESENT} carries window-client-pixel anchor/panel
     *         coordinates and a finite score. This method never focuses the window or sends physical input.
     */
    public ObservationResult observe(WindowNativeBinding binding) {
        if (binding == null
                || !binding.hasNativeHandle()
                || !binding.hasGeometry()
                || binding.getWidth() < SEARCH_RIGHT
                || binding.getHeight() < SEARCH_BOTTOM) {
            return new ObservationResult(State.CAPTURE_UNAVAILABLE, null, null, null, null, null, null, null);
        }

        int captureLeft;
        int captureTop;
        int captureRight;
        int captureBottom;
        try {
            captureLeft = Math.addExact(binding.getX(), SEARCH_LEFT);
            captureTop = Math.addExact(binding.getY(), SEARCH_TOP);
            captureRight = Math.addExact(binding.getX(), SEARCH_RIGHT);
            captureBottom = Math.addExact(binding.getY(), SEARCH_BOTTOM);
        } catch (ArithmeticException e) {
            return new ObservationResult(State.CAPTURE_UNAVAILABLE, null, null, null, null, null, null, null);
        }

        Optional<BoundWindowCaptureService.CaptureResult> captured;
        try {
            captured = captureService.captureRegion(
                    binding,
                    binding.getX(),
                    binding.getY(),
                    captureLeft,
                    captureTop,
                    captureRight,
                    captureBottom);
        } catch (RuntimeException e) {
            log.warn("[task-tracker-rect] exact-window capture failed: hwnd={} title={} reason={}",
                    binding.getNativeHandle(), binding.getTitle(), e.getMessage(), e);
            return new ObservationResult(State.CAPTURE_UNAVAILABLE, null, null, null, null, null, null, null);
        }

        if (captured == null || captured.isEmpty() || captured.get().image() == null) {
            return new ObservationResult(State.CAPTURE_UNAVAILABLE, null, null, null, null, null, null, null);
        }

        BufferedImage frame = captured.get().image();
        BufferedImage template = null;
        try {
            if (frame.getWidth() != SEARCH_WIDTH || frame.getHeight() != SEARCH_HEIGHT) {
                return new ObservationResult(State.MECHANICS_FAILED, null, null, null, null, null, null, null);
            }

            try {
                template = ImageIO.read(Path.of(ANCHOR_TEMPLATE_PATH).toFile());
            } catch (IOException | RuntimeException e) {
                log.warn("[task-tracker-rect] anchor template unavailable: path={} reason={}",
                        ANCHOR_TEMPLATE_PATH, e.getMessage(), e);
                return new ObservationResult(State.TEMPLATE_UNAVAILABLE, null, null, null, null, null, null, null);
            }
            if (template == null) {
                return new ObservationResult(State.TEMPLATE_UNAVAILABLE, null, null, null, null, null, null, null);
            }
            if (template.getWidth() > frame.getWidth() || template.getHeight() > frame.getHeight()) {
                return new ObservationResult(State.MECHANICS_FAILED, null, null, null, null, null, null, null);
            }

            double[] match = ImageFinder.find(frame, template, ANCHOR_THRESHOLD);
            if (match == null) {
                return new ObservationResult(State.ABSENT, null, null, null, null, null, null, null);
            }
            if (!isValidMatch(match)) {
                return new ObservationResult(State.MECHANICS_FAILED, null, null, null, null, null, null, null);
            }

            int localAnchorX = (int) Math.round(match[0]);
            int localAnchorY = (int) Math.round(match[1]);
            if (localAnchorX < 0 || localAnchorX >= SEARCH_WIDTH
                    || localAnchorY < 0 || localAnchorY >= SEARCH_HEIGHT) {
                return new ObservationResult(State.MECHANICS_FAILED, null, null, null, null, null, null, null);
            }

            int anchorClientX = SEARCH_LEFT + localAnchorX;
            int anchorClientY = SEARCH_TOP + localAnchorY;
            if (anchorClientX > SAFE_ANCHOR_MAX_X || anchorClientY > SAFE_ANCHOR_MAX_Y) {
                return new ObservationResult(State.REPOSITION_REQUIRED, null, null, null, null, null, null, null);
            }

            int panelClientLeft = anchorClientX + PANEL_FROM_ANCHOR_LEFT;
            int panelClientTop = anchorClientY + PANEL_FROM_ANCHOR_TOP;
            int panelClientRight = anchorClientX + PANEL_FROM_ANCHOR_RIGHT;
            int panelClientBottom = anchorClientY + PANEL_FROM_ANCHOR_BOTTOM;
            if (panelClientLeft < 0
                    || panelClientTop < 0
                    || panelClientRight <= panelClientLeft
                    || panelClientBottom <= panelClientTop
                    || panelClientRight > binding.getWidth()
                    || panelClientBottom > binding.getHeight()) {
                return new ObservationResult(State.MECHANICS_FAILED, null, null, null, null, null, null, null);
            }

            return new ObservationResult(
                    State.PRESENT,
                    anchorClientX,
                    anchorClientY,
                    panelClientLeft,
                    panelClientTop,
                    panelClientRight,
                    panelClientBottom,
                    match[2]);
        } catch (RuntimeException e) {
            log.warn("[task-tracker-rect] local observation mechanics failed: hwnd={} title={} reason={}",
                    binding.getNativeHandle(), binding.getTitle(), e.getMessage(), e);
            return new ObservationResult(State.MECHANICS_FAILED, null, null, null, null, null, null, null);
        } finally {
            frame.flush();
            if (template != null) {
                template.flush();
            }
        }
    }

    private boolean isValidMatch(double[] match) {
        return match.length >= 3
                && Double.isFinite(match[0])
                && Double.isFinite(match[1])
                && Double.isFinite(match[2])
                && match[0] >= 0.0D
                && match[0] < SEARCH_WIDTH
                && match[1] >= 0.0D
                && match[1] < SEARCH_HEIGHT
                && match[2] >= ANCHOR_THRESHOLD;
    }

    public enum State {
        PRESENT,
        ABSENT,
        CAPTURE_UNAVAILABLE,
        TEMPLATE_UNAVAILABLE,
        REPOSITION_REQUIRED,
        MECHANICS_FAILED
    }

    public record ObservationResult(
            State state,
            Integer anchorClientX,
            Integer anchorClientY,
            Integer panelClientLeft,
            Integer panelClientTop,
            Integer panelClientRight,
            Integer panelClientBottom,
            Double matchScore) {

        public ObservationResult {
            Objects.requireNonNull(state, "state");
            boolean present = state == State.PRESENT;
            boolean hasAllObservationFields = anchorClientX != null
                    && anchorClientY != null
                    && panelClientLeft != null
                    && panelClientTop != null
                    && panelClientRight != null
                    && panelClientBottom != null
                    && matchScore != null;
            boolean hasAnyObservationField = anchorClientX != null
                    || anchorClientY != null
                    || panelClientLeft != null
                    || panelClientTop != null
                    || panelClientRight != null
                    || panelClientBottom != null
                    || matchScore != null;
            if (present != hasAllObservationFields || (!present && hasAnyObservationField)) {
                throw new IllegalArgumentException("observation fields must be all present only for PRESENT");
            }
            if (present && (!Double.isFinite(matchScore)
                    || matchScore < ANCHOR_THRESHOLD
                    || anchorClientX < 0
                    || anchorClientX > SAFE_ANCHOR_MAX_X
                    || anchorClientY < 0
                    || anchorClientY > SAFE_ANCHOR_MAX_Y
                    || panelClientLeft < 0
                    || panelClientTop < 0
                    || panelClientRight <= panelClientLeft
                    || panelClientBottom <= panelClientTop
                    || panelClientLeft != anchorClientX + PANEL_FROM_ANCHOR_LEFT
                    || panelClientTop != anchorClientY + PANEL_FROM_ANCHOR_TOP
                    || panelClientRight != anchorClientX + PANEL_FROM_ANCHOR_RIGHT
                    || panelClientBottom != anchorClientY + PANEL_FROM_ANCHOR_BOTTOM)) {
                throw new IllegalArgumentException("invalid PRESENT anchor, panel rectangle, or match score");
            }
        }
    }
}
