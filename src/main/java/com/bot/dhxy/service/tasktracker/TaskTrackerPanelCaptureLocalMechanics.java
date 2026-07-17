package com.bot.dhxy.service.tasktracker;

import com.bot.dhxy.core.ImageFinder;
import com.bot.dhxy.driver.BoundWindowCaptureService;
import com.bot.dhxy.input.InputProvider;
import com.bot.dhxy.window.model.WindowNativeBinding;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;
import java.util.Optional;

/**
 * Exact-window local mechanics for producing the committed task-tracker detail panel image. This is
 * the drag+capture sibling of {@link TaskTrackerPanelRectLocalObservationMechanics}: where the
 * rect-only mechanics stops at {@code REPOSITION_REQUIRED}, this entry reproduces the committed
 * {@code 696a12b0} {@code TaskTrackerPanelService.resolveTrackerPanelRect} authority — narrow anchor
 * search, then on a miss the expanded full-window vision search, then, when the anchor is outside the
 * safe area, the original {@code DRAG_AND_DROP -> SLEEP(500)} executed inside one already-held input
 * worker, then a final panel capture in the same mechanics call.
 *
 * <p>The panel bytes and origin always come from the single frame captured after the (optional) drag.
 * Title match, green-chain splitting, fingerprint/cache, candidate sorting, classification and result
 * construction remain in the Cloud algorithm; this entry only performs the local capture/input.</p>
 */
@Slf4j
@Service
public final class TaskTrackerPanelCaptureLocalMechanics {

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
    // Committed drag gate: TRACKER_PANEL_ANCHOR_MAX_REL_X/Y and TRACKER_PANEL_DRAG_TARGET_REL_X/Y.
    private static final int SAFE_ANCHOR_MAX_X = 164;
    private static final int SAFE_ANCHOR_MAX_Y = 353;
    private static final int DRAG_TARGET_X = 104;
    private static final int DRAG_TARGET_Y = 221;
    private static final int DRAG_SETTLE_MS = 500;
    private static final String INPUT_WORKER_THREAD_NAME_TOKEN = "dhxy-input-action-worker";

    private final BoundWindowCaptureService captureService;
    private final InputProvider inputProvider;

    public TaskTrackerPanelCaptureLocalMechanics(BoundWindowCaptureService captureService,
                                                 InputProvider inputProvider) {
        this.captureService = Objects.requireNonNull(captureService, "captureService");
        this.inputProvider = Objects.requireNonNull(inputProvider, "inputProvider");
    }

    /**
     * Locate the tracker anchor and, after the committed optional drag, capture the detail panel of one
     * caller-supplied exact native-window binding.
     *
     * <p>When the anchor sits outside the committed safe area the method drives one
     * {@code DRAG_AND_DROP -> SLEEP(500)} and therefore must be invoked from the already-held input
     * worker; a non-input-worker caller that reaches the drag is rejected. The pure no-drag path takes
     * no new retry/TTL.</p>
     *
     * @param binding exact binding whose origin is screen-absolute pixels and whose client geometry is
     *                native-handle backed and large enough for the fixed observation region
     * @param source  caller label for diagnostics
     * @return a non-null closed result; only {@link State#CAPTURED} carries the panel image, its
     *         SHA-256, width/height and screen-absolute origin
     */
    public CaptureResultDto capturePanel(WindowNativeBinding binding, String source) {
        String safeSource = source == null || source.isBlank() ? "unknown" : source;
        if (binding == null
                || !binding.hasNativeHandle()
                || !binding.hasGeometry()
                || binding.getWidth() < SEARCH_RIGHT
                || binding.getHeight() < SEARCH_BOTTOM) {
            return CaptureResultDto.nonCaptured(State.CAPTURE_UNAVAILABLE);
        }

        BufferedImage template;
        try {
            template = ImageIO.read(Path.of(ANCHOR_TEMPLATE_PATH).toFile());
        } catch (IOException | RuntimeException e) {
            log.warn("[task-tracker-capture] anchor template unavailable: path={} reason={}",
                    ANCHOR_TEMPLATE_PATH, e.getMessage(), e);
            return CaptureResultDto.nonCaptured(State.TEMPLATE_UNAVAILABLE);
        }
        if (template == null) {
            return CaptureResultDto.nonCaptured(State.TEMPLATE_UNAVAILABLE);
        }

        try {
            AnchorResolution anchorResolution = resolveAnchorClientPoint(binding, template, safeSource);
            if (anchorResolution.state != State.CAPTURED) {
                return CaptureResultDto.nonCaptured(anchorResolution.state);
            }

            int anchorClientX = anchorResolution.anchorClientX;
            int anchorClientY = anchorResolution.anchorClientY;
            if (anchorClientX > SAFE_ANCHOR_MAX_X || anchorClientY > SAFE_ANCHOR_MAX_Y) {
                if (!isInputWorkerThread()) {
                    throw new IllegalStateException(
                            "task-tracker panel drag must run inside the exclusive input worker section");
                }
                int dragFromScreenX = Math.addExact(binding.getX(), anchorClientX);
                int dragFromScreenY = Math.addExact(binding.getY(), anchorClientY);
                int dragToScreenX = Math.addExact(binding.getX(), DRAG_TARGET_X);
                int dragToScreenY = Math.addExact(binding.getY(), DRAG_TARGET_Y);
                log.info("[task-tracker-capture] anchor outside safe area; drag source={} anchor=({}, {}) max=({}, {}) target=({}, {})",
                        safeSource, anchorClientX, anchorClientY, SAFE_ANCHOR_MAX_X, SAFE_ANCHOR_MAX_Y,
                        DRAG_TARGET_X, DRAG_TARGET_Y);
                inputProvider.dragAndDrop(dragFromScreenX, dragFromScreenY, dragToScreenX, dragToScreenY);
                try {
                    Thread.sleep(DRAG_SETTLE_MS);
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    return CaptureResultDto.nonCaptured(State.MECHANICS_FAILED);
                }
            }

            // Committed authority: panel rect is anchor + fixed offsets, captured after the drag settle.
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
                return CaptureResultDto.nonCaptured(State.MECHANICS_FAILED);
            }

            int panelScreenLeft = Math.addExact(binding.getX(), panelClientLeft);
            int panelScreenTop = Math.addExact(binding.getY(), panelClientTop);
            int panelScreenRight = Math.addExact(binding.getX(), panelClientRight);
            int panelScreenBottom = Math.addExact(binding.getY(), panelClientBottom);

            Optional<BoundWindowCaptureService.CaptureResult> panelCapture;
            try {
                panelCapture = captureService.captureRegion(
                        binding, binding.getX(), binding.getY(),
                        panelScreenLeft, panelScreenTop, panelScreenRight, panelScreenBottom);
            } catch (RuntimeException e) {
                log.warn("[task-tracker-capture] panel capture failed: hwnd={} title={} reason={}",
                        binding.getNativeHandle(), binding.getTitle(), e.getMessage(), e);
                return CaptureResultDto.nonCaptured(State.CAPTURE_UNAVAILABLE);
            }
            if (panelCapture == null || panelCapture.isEmpty() || panelCapture.get().image() == null) {
                return CaptureResultDto.nonCaptured(State.CAPTURE_UNAVAILABLE);
            }

            BufferedImage panel = panelCapture.get().image();
            try {
                int width = panel.getWidth();
                int height = panel.getHeight();
                if (width <= 0 || height <= 0) {
                    return CaptureResultDto.nonCaptured(State.MECHANICS_FAILED);
                }
                byte[] pngBytes = pngBytes(panel);
                String sha256 = sha256Hex(pngBytes);
                return CaptureResultDto.captured(
                        pngBytes, sha256, width, height, panelScreenLeft, panelScreenTop);
            } catch (IOException | NoSuchAlgorithmException | RuntimeException e) {
                log.warn("[task-tracker-capture] panel encoding failed: source={} reason={}",
                        safeSource, e.getMessage(), e);
                return CaptureResultDto.nonCaptured(State.MECHANICS_FAILED);
            } finally {
                panel.flush();
            }
        } finally {
            template.flush();
        }
    }

    /**
     * Narrow anchor search, then the committed expanded full-window vision search on a miss. Returns a
     * CAPTURED resolution carrying the window-client anchor, or a terminal non-CAPTURED state.
     */
    private AnchorResolution resolveAnchorClientPoint(WindowNativeBinding binding,
                                                      BufferedImage template,
                                                      String safeSource) {
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
            return AnchorResolution.terminal(State.CAPTURE_UNAVAILABLE);
        }

        Optional<BoundWindowCaptureService.CaptureResult> narrow;
        try {
            narrow = captureService.captureRegion(
                    binding, binding.getX(), binding.getY(),
                    captureLeft, captureTop, captureRight, captureBottom);
        } catch (RuntimeException e) {
            log.warn("[task-tracker-capture] narrow capture failed: hwnd={} title={} reason={}",
                    binding.getNativeHandle(), binding.getTitle(), e.getMessage(), e);
            return AnchorResolution.terminal(State.CAPTURE_UNAVAILABLE);
        }
        if (narrow == null || narrow.isEmpty() || narrow.get().image() == null) {
            return AnchorResolution.terminal(State.CAPTURE_UNAVAILABLE);
        }

        BufferedImage narrowFrame = narrow.get().image();
        try {
            if (narrowFrame.getWidth() != SEARCH_WIDTH || narrowFrame.getHeight() != SEARCH_HEIGHT) {
                return AnchorResolution.terminal(State.MECHANICS_FAILED);
            }
            if (template.getWidth() > narrowFrame.getWidth() || template.getHeight() > narrowFrame.getHeight()) {
                return AnchorResolution.terminal(State.MECHANICS_FAILED);
            }
            double[] match = ImageFinder.find(narrowFrame, template, ANCHOR_THRESHOLD);
            if (match != null) {
                if (!isFiniteMatch(match)) {
                    return AnchorResolution.terminal(State.MECHANICS_FAILED);
                }
                int localX = (int) Math.round(match[0]);
                int localY = (int) Math.round(match[1]);
                if (localX < 0 || localX >= SEARCH_WIDTH || localY < 0 || localY >= SEARCH_HEIGHT) {
                    return AnchorResolution.terminal(State.MECHANICS_FAILED);
                }
                return AnchorResolution.anchor(SEARCH_LEFT + localX, SEARCH_TOP + localY);
            }
        } finally {
            narrowFrame.flush();
        }

        log.warn("[task-tracker-capture] anchor not found in narrow area, keep searching expanded: source={}",
                safeSource);
        return resolveExpandedAnchorClientPoint(binding, template, safeSource);
    }

    private AnchorResolution resolveExpandedAnchorClientPoint(WindowNativeBinding binding,
                                                             BufferedImage template,
                                                             String safeSource) {
        Optional<BoundWindowCaptureService.CaptureResult> fullWindow;
        try {
            fullWindow = captureService.captureWindow(binding);
        } catch (RuntimeException e) {
            log.warn("[task-tracker-capture] expanded full-window capture failed: hwnd={} title={} reason={}",
                    binding.getNativeHandle(), binding.getTitle(), e.getMessage(), e);
            return AnchorResolution.terminal(State.CAPTURE_UNAVAILABLE);
        }
        if (fullWindow == null || fullWindow.isEmpty() || fullWindow.get().image() == null) {
            return AnchorResolution.terminal(State.CAPTURE_UNAVAILABLE);
        }

        BufferedImage fullFrame = fullWindow.get().image();
        try {
            if (template.getWidth() > fullFrame.getWidth() || template.getHeight() > fullFrame.getHeight()) {
                return AnchorResolution.terminal(State.MECHANICS_FAILED);
            }
            double[] match = ImageFinder.find(fullFrame, template, ANCHOR_THRESHOLD);
            if (match == null) {
                log.warn("[task-tracker-capture] anchor not found in expanded full-window: source={}", safeSource);
                return AnchorResolution.terminal(State.ABSENT);
            }
            if (!isFiniteMatch(match)) {
                return AnchorResolution.terminal(State.MECHANICS_FAILED);
            }
            int localX = (int) Math.round(match[0]);
            int localY = (int) Math.round(match[1]);
            if (localX < 0 || localX >= fullFrame.getWidth() || localY < 0 || localY >= fullFrame.getHeight()) {
                return AnchorResolution.terminal(State.MECHANICS_FAILED);
            }
            // Committed expandedVisionAnchorToScreenAnchor is base + local; in window-client pixels the
            // full-window match origin is the client point itself.
            return AnchorResolution.anchor(localX, localY);
        } finally {
            fullFrame.flush();
        }
    }

    private boolean isInputWorkerThread() {
        return Thread.currentThread().getName().contains(INPUT_WORKER_THREAD_NAME_TOKEN);
    }

    private static boolean isFiniteMatch(double[] match) {
        return match.length >= 3
                && Double.isFinite(match[0])
                && Double.isFinite(match[1])
                && Double.isFinite(match[2])
                && match[2] >= ANCHOR_THRESHOLD;
    }

    private static byte[] pngBytes(BufferedImage image) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, "png", output);
        return output.toByteArray();
    }

    private static String sha256Hex(byte[] bytes) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hashed = digest.digest(bytes);
        StringBuilder result = new StringBuilder(hashed.length * 2);
        for (byte value : hashed) {
            result.append(String.format("%02x", value));
        }
        return result.toString();
    }

    private static final class AnchorResolution {
        private final State state;
        private final int anchorClientX;
        private final int anchorClientY;

        private AnchorResolution(State state, int anchorClientX, int anchorClientY) {
            this.state = state;
            this.anchorClientX = anchorClientX;
            this.anchorClientY = anchorClientY;
        }

        private static AnchorResolution anchor(int anchorClientX, int anchorClientY) {
            return new AnchorResolution(State.CAPTURED, anchorClientX, anchorClientY);
        }

        private static AnchorResolution terminal(State state) {
            return new AnchorResolution(state, 0, 0);
        }
    }

    public enum State {
        CAPTURED,
        ABSENT,
        CAPTURE_UNAVAILABLE,
        TEMPLATE_UNAVAILABLE,
        MECHANICS_FAILED
    }

    public record CaptureResultDto(
            State state,
            byte[] panelPngBytes,
            String panelSha256,
            Integer panelWidth,
            Integer panelHeight,
            Integer absoluteLeft,
            Integer absoluteTop) {

        public CaptureResultDto {
            Objects.requireNonNull(state, "state");
            boolean captured = state == State.CAPTURED;
            boolean hasAllImageFields = panelPngBytes != null
                    && panelSha256 != null
                    && panelWidth != null
                    && panelHeight != null
                    && absoluteLeft != null
                    && absoluteTop != null;
            boolean hasAnyImageField = panelPngBytes != null
                    || panelSha256 != null
                    || panelWidth != null
                    || panelHeight != null
                    || absoluteLeft != null
                    || absoluteTop != null;
            if (captured != hasAllImageFields || (!captured && hasAnyImageField)) {
                throw new IllegalArgumentException("image fields must be all present only for CAPTURED");
            }
            if (captured && (panelPngBytes.length == 0
                    || panelSha256.isBlank()
                    || panelWidth <= 0
                    || panelHeight <= 0)) {
                throw new IllegalArgumentException("invalid CAPTURED panel image, dimensions, or hash");
            }
        }

        private static CaptureResultDto captured(byte[] panelPngBytes, String panelSha256,
                                                 int panelWidth, int panelHeight,
                                                 int absoluteLeft, int absoluteTop) {
            return new CaptureResultDto(State.CAPTURED, panelPngBytes.clone(), panelSha256,
                    panelWidth, panelHeight, absoluteLeft, absoluteTop);
        }

        private static CaptureResultDto nonCaptured(State state) {
            return new CaptureResultDto(state, null, null, null, null, null, null);
        }

        @Override
        public byte[] panelPngBytes() {
            return panelPngBytes == null ? null : panelPngBytes.clone();
        }
    }
}
