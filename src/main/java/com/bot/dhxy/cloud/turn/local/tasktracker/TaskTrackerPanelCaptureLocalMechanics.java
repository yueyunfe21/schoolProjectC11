package com.bot.dhxy.cloud.turn.local.tasktracker;

import com.bot.dhxy.core.ImageFinder;
import com.bot.dhxy.driver.BoundWindowCaptureService;
import com.bot.dhxy.input.InputProvider;
import com.bot.dhxy.input.WindowAwareInputCoordinator;
import com.bot.dhxy.window.model.WindowNativeBinding;
import com.bot.dhxy.window.runtime.WindowRuntimeContext;
import com.bot.dhxy.window.runtime.WindowScopedTempPath;
import com.bot.dhxy.window.runtime.WindowTrackerAnchorMemory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;
import java.util.Optional;

/** Exact-window local mechanics for cached tracker-anchor resolution, optional drag, and panel capture. */
@Slf4j
@Component
public final class TaskTrackerPanelCaptureLocalMechanics {

    private static final String ANCHOR_TEMPLATE_PATH = "images/template/task/wubei_tracker_anchor.png";
    private static final double ANCHOR_THRESHOLD = 0.82D;
    private static final int LOCAL_LEFT = -100;
    private static final int LOCAL_TOP = -75;
    private static final int LOCAL_RIGHT = 100;
    private static final int LOCAL_BOTTOM = 75;
    private static final int DRAG_TARGET_X = 119;
    private static final int DRAG_TARGET_Y = 221;
    private static final int PANEL_LEFT = -112;
    private static final int PANEL_TOP = 12;
    private static final int PANEL_RIGHT = 102;
    private static final int PANEL_BOTTOM = 350;
    private static final int DRAG_SETTLE_MS = 500;
    private static final String INPUT_WORKER_THREAD_NAME_TOKEN = "dhxy-input-action-worker";

    private final BoundWindowCaptureService captureService;
    private final InputProvider inputProvider;
    private final WindowAwareInputCoordinator inputCoordinator;
    private final WindowScopedTempPath windowScopedTempPath;

    public TaskTrackerPanelCaptureLocalMechanics(BoundWindowCaptureService captureService,
                                                 InputProvider inputProvider,
                                                 WindowAwareInputCoordinator inputCoordinator,
                                                 WindowScopedTempPath windowScopedTempPath) {
        this.captureService = Objects.requireNonNull(captureService, "captureService");
        this.inputProvider = Objects.requireNonNull(inputProvider, "inputProvider");
        this.inputCoordinator = Objects.requireNonNull(inputCoordinator, "inputCoordinator");
        this.windowScopedTempPath = Objects.requireNonNull(windowScopedTempPath, "windowScopedTempPath");
    }

    /**
     * Resolve and capture one tracker panel while the caller holds the single input worker.
     *
     * @param context exact window runtime owning the window-relative anchor cache
     * @param binding exact HWND binding; geometry is screen-absolute pixels
     * @param source nonblank diagnostic source
     * @return closed mechanics result; only CAPTURED carries one defensive raw PNG
     */
    public CaptureResultDto capturePanel(WindowRuntimeContext context,
                                         WindowNativeBinding binding,
                                         String source) {
        Objects.requireNonNull(context, "context");
        String safeSource = source == null || source.isBlank() ? "unknown" : source;
        if (!isInputWorkerThread()) {
            throw new IllegalStateException("tracker panel mechanics requires the held input worker");
        }
        if (binding == null || !binding.hasNativeHandle() || !binding.hasGeometry()
                || binding.getWidth() <= 0 || binding.getHeight() <= 0) {
            return CaptureResultDto.nonCaptured(State.CAPTURE_UNAVAILABLE);
        }

        BufferedImage template = readTemplate();
        if (template == null) {
            return CaptureResultDto.nonCaptured(State.TEMPLATE_UNAVAILABLE);
        }
        try {
            Point anchor = findCachedAnchor(context, binding, template, safeSource);
            if (anchor == null) {
                context.setTaskTrackerAnchorMemory(null);
                anchor = findRawFullWindowAnchor(binding, template, safeSource);
                if (anchor == null) {
                    return CaptureResultDto.nonCaptured(State.ABSENT);
                }

                boolean originallyInsideDefaultRoi = isInsideDefaultRoi(anchor);
                if (!originallyInsideDefaultRoi) {
                    inputCoordinator.focusCurrentWindowInActiveTransaction(
                            "task-tracker-capture:drag-anchor");
                    int fromX = Math.addExact(binding.getX(), anchor.x);
                    int fromY = Math.addExact(binding.getY(), anchor.y);
                    int targetX = Math.addExact(binding.getX(), DRAG_TARGET_X);
                    int targetY = Math.addExact(binding.getY(), DRAG_TARGET_Y);
                    inputProvider.dragAndDrop(fromX, fromY, targetX, targetY);
                    if (!sleepAfterDrag()) {
                        return CaptureResultDto.nonCaptured(State.MECHANICS_FAILED);
                    }
                    anchor = new Point(DRAG_TARGET_X, DRAG_TARGET_Y);
                }
                rememberAnchor(context, anchor);

                if (!originallyInsideDefaultRoi) {
                    Point confirmed = findCachedAnchor(context, binding, template, safeSource + ":post-drag");
                    if (confirmed != null) {
                        anchor = confirmed;
                    } else {
                        log.warn("[task-tracker-capture] post-drag cached ROI did not confirm anchor: source={}",
                                safeSource);
                    }
                }
            }
            return capturePanelAtAnchor(binding, anchor, safeSource);
        } catch (RuntimeException failure) {
            log.warn("[task-tracker-capture] mechanics failed: windowId={} hwnd={} source={} reason={}",
                    context.getWindowId(), binding.getNativeHandle(), safeSource, failure.getMessage(), failure);
            return CaptureResultDto.nonCaptured(State.MECHANICS_FAILED);
        } finally {
            template.flush();
        }
    }

    private Point findCachedAnchor(WindowRuntimeContext context,
                                   WindowNativeBinding binding,
                                   BufferedImage template,
                                   String source) {
        Optional<WindowTrackerAnchorMemory> memory = context.getTaskTrackerAnchorMemory();
        if (memory.isEmpty()) {
            return null;
        }
        WindowTrackerAnchorMemory cached = memory.get();
        int left = Math.max(0, cached.relativeX() + LOCAL_LEFT);
        int top = Math.max(0, cached.relativeY() + LOCAL_TOP);
        int right = Math.min(binding.getWidth(), cached.relativeX() + LOCAL_RIGHT);
        int bottom = Math.min(binding.getHeight(), cached.relativeY() + LOCAL_BOTTOM);
        BufferedImage roi = capture(binding, left, top, right, bottom);
        if (roi == null) {
            return null;
        }
        try {
            double[] match = ImageFinder.find(roi, template, ANCHOR_THRESHOLD);
            if (!validMatch(match, roi)) {
                log.info("[task-tracker-capture] cached ROI miss; use masked full-window fallback: source={}", source);
                return null;
            }
            Point anchor = new Point(left + (int) Math.round(match[0]), top + (int) Math.round(match[1]));
            rememberAnchor(context, anchor);
            return anchor;
        } finally {
            roi.flush();
        }
    }

    private Point findRawFullWindowAnchor(WindowNativeBinding binding,
                                          BufferedImage template,
                                          String source) {
        BufferedImage raw = captureWindow(binding);
        if (raw == null) {
            return null;
        }
        try {
            double[] match = ImageFinder.find(raw, template, ANCHOR_THRESHOLD);
            if (!validMatch(match, raw)) {
                persistAbsentEvidence(raw, raw, source);
                return null;
            }
            return new Point((int) Math.round(match[0]), (int) Math.round(match[1]));
        } finally {
            raw.flush();
        }
    }

    private void persistAbsentEvidence(BufferedImage raw, BufferedImage matchSource, String source) {
        Path rawPath = Path.of(windowScopedTempPath.resolve("task_tracker_anchor_absent_raw.png"))
                .toAbsolutePath().normalize();
        Path matchSourcePath = Path.of(windowScopedTempPath.resolve(
                        "task_tracker_anchor_absent_match_source.png"))
                .toAbsolutePath().normalize();
        try {
            Files.createDirectories(rawPath.getParent());
            boolean rawWritten = ImageIO.write(raw, "png", rawPath.toFile());
            boolean matchSourceWritten = ImageIO.write(matchSource, "png", matchSourcePath.toFile());
            log.warn("[task-tracker-capture] anchor absent in full window: source={} raw={} "
                            + "matchSource={} rawWritten={} matchSourceWritten={}",
                    source, rawPath, matchSourcePath, rawWritten, matchSourceWritten);
        } catch (IOException | RuntimeException failure) {
            log.warn("[task-tracker-capture] anchor absent; failure evidence could not be saved: "
                            + "source={} raw={} matchSource={} reason={}",
                    source, rawPath, matchSourcePath, failure.getMessage(), failure);
        }
    }

    private CaptureResultDto capturePanelAtAnchor(WindowNativeBinding binding, Point anchor, String source) {
        int left = anchor.x + PANEL_LEFT;
        int top = anchor.y + PANEL_TOP;
        int right = anchor.x + PANEL_RIGHT;
        int bottom = anchor.y + PANEL_BOTTOM;
        BufferedImage panel = capture(binding, left, top, right, bottom);
        if (panel == null) {
            return CaptureResultDto.nonCaptured(State.CAPTURE_UNAVAILABLE);
        }
        try {
            byte[] png = pngBytes(panel);
            return CaptureResultDto.captured(
                    png,
                    sha256Hex(png),
                    panel.getWidth(),
                    panel.getHeight(),
                    Math.addExact(binding.getX(), left),
                    Math.addExact(binding.getY(), top));
        } catch (IOException | NoSuchAlgorithmException failure) {
            log.warn("[task-tracker-capture] panel encode failed: source={} reason={}",
                    source, failure.getMessage(), failure);
            return CaptureResultDto.nonCaptured(State.MECHANICS_FAILED);
        } finally {
            panel.flush();
        }
    }

    private BufferedImage capture(WindowNativeBinding binding, int left, int top, int right, int bottom) {
        if (left < 0 || top < 0 || right <= left || bottom <= top
                || right > binding.getWidth() || bottom > binding.getHeight()) {
            return null;
        }
        Optional<BoundWindowCaptureService.CaptureResult> result = captureService.captureRegion(
                binding,
                binding.getX(),
                binding.getY(),
                Math.addExact(binding.getX(), left),
                Math.addExact(binding.getY(), top),
                Math.addExact(binding.getX(), right),
                Math.addExact(binding.getY(), bottom));
        return result == null || result.isEmpty() ? null : result.get().image();
    }

    private BufferedImage captureWindow(WindowNativeBinding binding) {
        Optional<BoundWindowCaptureService.CaptureResult> result = captureService.captureWindow(binding);
        return result == null || result.isEmpty() ? null : result.get().image();
    }

    private static void rememberAnchor(WindowRuntimeContext context, Point anchor) {
        context.setTaskTrackerAnchorMemory(new WindowTrackerAnchorMemory(anchor.x, anchor.y));
    }

    private static boolean isInsideDefaultRoi(Point anchor) {
        return anchor.x >= DRAG_TARGET_X + LOCAL_LEFT
                && anchor.x <= DRAG_TARGET_X + LOCAL_RIGHT
                && anchor.y >= DRAG_TARGET_Y + LOCAL_TOP
                && anchor.y <= DRAG_TARGET_Y + LOCAL_BOTTOM;
    }

    private static boolean validMatch(double[] match, BufferedImage frame) {
        return match != null && match.length >= 3
                && Double.isFinite(match[0]) && Double.isFinite(match[1]) && Double.isFinite(match[2])
                && match[2] >= ANCHOR_THRESHOLD
                && match[0] >= 0 && match[0] < frame.getWidth()
                && match[1] >= 0 && match[1] < frame.getHeight();
    }

    private static boolean sleepAfterDrag() {
        try {
            Thread.sleep(DRAG_SETTLE_MS);
            return true;
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private static boolean isInputWorkerThread() {
        return Thread.currentThread().getName().contains(INPUT_WORKER_THREAD_NAME_TOKEN);
    }

    private static BufferedImage readTemplate() {
        try {
            return ImageIO.read(Path.of(ANCHOR_TEMPLATE_PATH).toFile());
        } catch (IOException | RuntimeException failure) {
            return null;
        }
    }

    private static byte[] pngBytes(BufferedImage image) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, "png", output);
        return output.toByteArray();
    }

    private static String sha256Hex(byte[] bytes) throws NoSuchAlgorithmException {
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(bytes);
        StringBuilder result = new StringBuilder(digest.length * 2);
        for (byte value : digest) {
            result.append(String.format("%02x", value));
        }
        return result.toString();
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
            boolean all = panelPngBytes != null && panelSha256 != null && panelWidth != null
                    && panelHeight != null && absoluteLeft != null && absoluteTop != null;
            boolean any = panelPngBytes != null || panelSha256 != null || panelWidth != null
                    || panelHeight != null || absoluteLeft != null || absoluteTop != null;
            if ((captured && !all) || (!captured && any)) {
                throw new IllegalArgumentException("image fields must be present only for CAPTURED");
            }
            panelPngBytes = panelPngBytes == null ? null : panelPngBytes.clone();
        }

        private static CaptureResultDto captured(byte[] png, String sha256, int width, int height,
                                                 int absoluteLeft, int absoluteTop) {
            return new CaptureResultDto(
                    State.CAPTURED, png, sha256, width, height, absoluteLeft, absoluteTop);
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
