package com.bot.dhxy.service.autocombat;

import com.bot.dhxy.core.ImageFinder;
import com.bot.dhxy.driver.BoundWindowCaptureService;
import com.bot.dhxy.input.InputProvider;
import com.bot.dhxy.runner.stop.TaskSleep;
import com.bot.dhxy.tools.ImagePreprocessor;
import com.bot.dhxy.window.model.WindowNativeBinding;
import com.bot.dhxy.window.runtime.WindowNativeBindingRefreshService;
import com.bot.dhxy.window.runtime.WindowScopedTempPath;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Objects;
import java.util.Optional;

/**
 * Whole continuous local mechanics for the committed {@code 696a12b0} auto-combat panel
 * visibility/alignment, extracted from {@code AutoCombatPanelService:69-156,269-320}
 * ({@code findAutoCombatBox}, {@code ensurePanelMatchVisible}, {@code alignPanelIfNeeded}).
 *
 * <p>Three closed operations only observe/open/align the panel and return a typed result. The class
 * never owns business mode, rounds refresh, missing counters, timestamps or {@code GameContext} state;
 * those stay in the future Cloud {@code AutoCombatPanelService}. It read-only reuses
 * {@link BoundWindowCaptureService}, {@link WindowNativeBindingRefreshService}, {@link ImageFinder},
 * {@link ImagePreprocessor#countGreenPixelsHSV}, {@link InputProvider}, {@link TaskSleep} and
 * {@link WindowScopedTempPath}. Fresh exact HWND geometry is taken before every real capture; the
 * anchor-first then green-marker fallback order, the {@code 0.80} thresholds, the {@code (30,30)}
 * anchor/marker offset, the {@code (489,726)} target offset, the {@code >20.0} drag distance, the
 * {@code Alt+8}/{@code waitAfterOpenMs} open and the {@code 500ms} drag settle are preserved. Every
 * frame/template/mask has a single owner and is flushed once; no owner/session/ledger/TTL/retry is
 * added, and no rounds OCR/refresh is implemented.</p>
 */
@Slf4j
@Service
public final class AutoCombatPanelVisibilityLocalMacroMechanics {

    private static final String QUXIAO_ZIDONG_PATH = "images/template/battle/quxiao_zidong_green.png";
    private static final String ZIDONG_GREEN_PATH = "images/template/battle/zidong_green.png";
    private static final String AUTO_PANEL_FALLBACK_ANCHOR_PATH = "images/template/battle/auto_panel_fallback_anchor.png";
    private static final String GREEN_MASK_DEBUG_NAME = "debug_hsv_mask_green.png";

    private static final int TARGET_PANEL_X_OFFSET = 489;
    private static final int TARGET_PANEL_Y_OFFSET = 726;
    private static final int FALLBACK_ANCHOR_TO_GREEN_MARKER_X = 30;
    private static final int FALLBACK_ANCHOR_TO_GREEN_MARKER_Y = 30;
    private static final double ANCHOR_MATCH_RATE = 0.80D;
    private static final double GREEN_MARKER_MATCH_RATE = 0.80D;
    private static final double ALIGN_DISTANCE_THRESHOLD = 20.0D;
    private static final long DRAG_SETTLE_MS = 500L;
    private static final String INPUT_ACTION_WORKER_THREAD = "dhxy-input-action-worker";

    private final BoundWindowCaptureService captureService;
    private final WindowNativeBindingRefreshService bindingRefreshService;
    private final InputProvider inputProvider;
    private final WindowScopedTempPath windowScopedTempPath;

    public AutoCombatPanelVisibilityLocalMacroMechanics(
            BoundWindowCaptureService captureService,
            WindowNativeBindingRefreshService bindingRefreshService,
            InputProvider inputProvider,
            WindowScopedTempPath windowScopedTempPath) {
        this.captureService = Objects.requireNonNull(captureService, "captureService");
        this.bindingRefreshService = Objects.requireNonNull(bindingRefreshService, "bindingRefreshService");
        this.inputProvider = Objects.requireNonNull(inputProvider, "inputProvider");
        this.windowScopedTempPath = Objects.requireNonNull(windowScopedTempPath, "windowScopedTempPath");
    }

    /** Closed terminal. Success statuses carry the panel center; failure statuses carry no point. */
    public enum Status {
        FOUND,
        FOUND_AFTER_OPEN,
        ALIGNED,
        ALIGNED_WITH_DROP_TARGET_FALLBACK,
        NOT_FOUND,
        CAPTURE_UNAVAILABLE,
        BINDING_UNAVAILABLE,
        NON_INPUT_WORKER,
        INTERRUPTED,
        MECHANICS_FAILED
    }

    /** Closed immutable intent for the open/align operations. */
    public record PanelVisibilityIntent(String source, int waitAfterOpenMs) {
        public PanelVisibilityIntent {
            if (waitAfterOpenMs < 0) {
                throw new IllegalArgumentException("waitAfterOpenMs must be non-negative");
            }
        }
    }

    /**
     * Immutable closed result. A success status carries the screen-absolute panel center, the nullable
     * green-marker pair, the template width and the detection source; a failure status carries none of
     * those, so no fabricated point can escape. The green marker is always a full pair or fully absent.
     */
    public record PanelVisibilityResult(
            Status status,
            Integer panelCenterX,
            Integer panelCenterY,
            Integer greenMarkerX,
            Integer greenMarkerY,
            Integer templateWidth,
            String detectionSource,
            String reason) {

        public PanelVisibilityResult {
            Objects.requireNonNull(status, "status");
            boolean success = isSuccess(status);
            boolean hasCenter = panelCenterX != null && panelCenterY != null;
            boolean anyCenter = panelCenterX != null || panelCenterY != null;
            if (anyCenter != hasCenter) {
                throw new IllegalArgumentException("panel center must be a full pair or fully absent");
            }
            boolean hasMarker = greenMarkerX != null && greenMarkerY != null;
            boolean anyMarker = greenMarkerX != null || greenMarkerY != null;
            if (anyMarker != hasMarker) {
                throw new IllegalArgumentException("green marker must be a full pair or fully absent");
            }
            if (success) {
                if (!hasCenter || templateWidth == null || detectionSource == null) {
                    throw new IllegalArgumentException("success result must carry panel center, template width and source");
                }
                if (templateWidth < 0) {
                    throw new IllegalArgumentException("template width must be non-negative");
                }
            } else if (hasCenter || hasMarker || templateWidth != null || detectionSource != null) {
                throw new IllegalArgumentException("failure result must not carry any panel field");
            }
        }
    }

    /** Internal screen-absolute panel match. */
    private record PanelMatch(
            int panelCenterX,
            int panelCenterY,
            Integer greenMarkerX,
            Integer greenMarkerY,
            int templateWidth,
            String detectionSource) {
    }

    /** Internal find outcome: exactly one of a match or a failure status. */
    private record FindOutcome(PanelMatch match, Status failure) {
        private static FindOutcome matched(PanelMatch match) {
            return new FindOutcome(match, null);
        }

        private static FindOutcome failed(Status failure) {
            return new FindOutcome(null, failure);
        }
    }

    /** Internal ensure outcome carrying the resolved match on success. */
    private record EnsureOutcome(Status status, PanelMatch match, String reason) {
    }

    /**
     * {@code OBSERVE_PANEL}: one fresh full-window capture on the exact binding, anchor template first at
     * {@code 0.80}, and only on an anchor miss the green mask + green-marker template at {@code 0.80};
     * zero input. Mirrors {@code findAutoCombatBox}.
     */
    public PanelVisibilityResult observePanel(WindowNativeBinding binding, String source) {
        String safeSource = safeSource(source);
        FindOutcome outcome = findAutoCombatBox(binding, safeSource);
        if (outcome.match() != null) {
            return success(Status.FOUND, outcome.match(), "observed source=" + safeSource);
        }
        return failure(outcome.failure(), "observe-" + outcome.failure() + " source=" + safeSource);
    }

    /**
     * {@code ENSURE_VISIBLE}: observe once; only on a genuine panel miss (NOT_FOUND) press {@code Alt+8}
     * directly inside the existing input worker, wait {@code waitAfterOpenMs} once, and re-observe once.
     * No nested input queue and no auto retry. Mirrors {@code ensurePanelMatchVisible}.
     */
    public PanelVisibilityResult ensureVisible(WindowNativeBinding binding, PanelVisibilityIntent intent) {
        return toResult(ensureVisibleInternal(binding, intent));
    }

    /**
     * {@code ENSURE_VISIBLE_AND_ALIGN}: reuse the ensure result; only when the panel center is farther
     * than {@code 20.0} from {@code freshWindowOrigin + (489,726)} drag directly, wait {@code 500ms} and
     * observe once. On a post-drag miss keep the {@code drag-target-fallback} drop-target terminal; when
     * no drag is needed no extra capture is taken. Mirrors {@code alignPanelIfNeeded}.
     */
    public PanelVisibilityResult ensureVisibleAndAlign(WindowNativeBinding binding, PanelVisibilityIntent intent) {
        EnsureOutcome ensured = ensureVisibleInternal(binding, intent);
        if (!isSuccess(ensured.status())) {
            return toResult(ensured);
        }
        String safeSource = safeSource(intent.source());
        PanelMatch match = ensured.match();

        Optional<WindowNativeBinding> fresh = bindingRefreshService.refreshGeometry(binding);
        if (fresh.isEmpty()) {
            return failure(Status.BINDING_UNAVAILABLE, "align-binding-unavailable source=" + safeSource);
        }
        int dropX = fresh.get().getX() + TARGET_PANEL_X_OFFSET;
        int dropY = fresh.get().getY() + TARGET_PANEL_Y_OFFSET;
        double distance = Math.hypot(
                (double) match.panelCenterX() - dropX, (double) match.panelCenterY() - dropY);
        if (distance <= ALIGN_DISTANCE_THRESHOLD) {
            // Already in the safe area: no drag and no additional capture.
            return success(Status.ALIGNED, match, "aligned-already source=" + safeSource);
        }

        if (!isInputWorkerThread()) {
            return failure(Status.NON_INPUT_WORKER, "align-non-input-worker source=" + safeSource);
        }
        inputProvider.dragAndDrop(match.panelCenterX(), match.panelCenterY(), dropX, dropY);
        // Baseline alignPanelIfNeeded ignores the drag sequence result and unconditionally does exactly
        // one post-drag observe; the 500ms settle boolean is not a terminal here (no INTERRUPTED from
        // this branch), and no extra capture/retry is added.
        TaskSleep.sleep(DRAG_SETTLE_MS);
        FindOutcome recheck = findAutoCombatBox(binding, safeSource);
        if (recheck.match() != null) {
            return success(Status.ALIGNED, recheck.match(), "aligned-after-drag source=" + safeSource);
        }
        // Baseline drag-target-fallback: report the drop target itself, no green marker, width 0.
        PanelMatch fallback = new PanelMatch(dropX, dropY, null, null, 0, "drag-target-fallback");
        return success(Status.ALIGNED_WITH_DROP_TARGET_FALLBACK, fallback, "aligned-drop-fallback source=" + safeSource);
    }

    private EnsureOutcome ensureVisibleInternal(WindowNativeBinding binding, PanelVisibilityIntent intent) {
        Objects.requireNonNull(intent, "intent");
        String safeSource = safeSource(intent.source());
        FindOutcome first = findAutoCombatBox(binding, safeSource);
        if (first.match() != null) {
            return new EnsureOutcome(Status.FOUND, first.match(), "visible source=" + safeSource);
        }
        // Baseline ensurePanelMatchVisible sends the single Alt+8 + wait + re-observe for ANY first null
        // from findAutoCombatBox, which is both a genuine miss and an unreadable first capture. So both
        // NOT_FOUND and CAPTURE_UNAVAILABLE enter that one open/re-observe; only binding/mechanics/
        // non-input failures short-circuit, and MECHANICS_FAILED is never silently retried.
        if (first.failure() != Status.NOT_FOUND && first.failure() != Status.CAPTURE_UNAVAILABLE) {
            return new EnsureOutcome(first.failure(), null, "ensure-" + first.failure() + " source=" + safeSource);
        }
        if (!isInputWorkerThread()) {
            return new EnsureOutcome(Status.NON_INPUT_WORKER, null, "ensure-non-input-worker source=" + safeSource);
        }
        inputProvider.pressAlt8();
        if (!TaskSleep.sleep(intent.waitAfterOpenMs())) {
            return new EnsureOutcome(Status.INTERRUPTED, null, "ensure-interrupted-after-alt8 source=" + safeSource);
        }
        FindOutcome second = findAutoCombatBox(binding, safeSource);
        if (second.match() != null) {
            return new EnsureOutcome(Status.FOUND_AFTER_OPEN, second.match(), "visible-after-alt8 source=" + safeSource);
        }
        return new EnsureOutcome(second.failure(), null,
                "ensure-" + second.failure() + "-after-alt8 source=" + safeSource);
    }

    /**
     * Fresh-geometry capture then anchor-first / green-marker fallback detection. An unreadable template
     * is never a terminal: an unreadable anchor still falls through to the green mask, and only when both
     * candidates miss is the result {@code NOT_FOUND}. The captured frame and any loaded template are
     * flushed exactly once.
     */
    private FindOutcome findAutoCombatBox(WindowNativeBinding binding, String safeSource) {
        Optional<WindowNativeBinding> fresh = bindingRefreshService.refreshGeometry(binding);
        if (fresh.isEmpty()) {
            return FindOutcome.failed(Status.BINDING_UNAVAILABLE);
        }
        WindowNativeBinding live = fresh.get();
        Optional<BoundWindowCaptureService.CaptureResult> captured;
        try {
            captured = captureService.captureWindow(live);
        } catch (RuntimeException e) {
            log.warn("auto-combat panel capture mechanics failed: source={} reason={}", safeSource, e.getMessage(), e);
            return FindOutcome.failed(Status.MECHANICS_FAILED);
        }
        if (captured == null || captured.isEmpty() || captured.get().image() == null) {
            return FindOutcome.failed(Status.CAPTURE_UNAVAILABLE);
        }
        BufferedImage frame = captured.get().image();
        try {
            int originX = live.getX();
            int originY = live.getY();

            // Anchor template first at 0.80. An unreadable anchor template falls through to green.
            BufferedImage anchorTemplate = readTemplate(AUTO_PANEL_FALLBACK_ANCHOR_PATH);
            if (anchorTemplate != null) {
                double[] anchorMatch;
                try {
                    anchorMatch = ImageFinder.find(frame, anchorTemplate, ANCHOR_MATCH_RATE);
                } finally {
                    anchorTemplate.flush();
                }
                if (anchorMatch != null && anchorMatch.length >= 2) {
                    int panelX = originX + (int) Math.round(anchorMatch[0]);
                    int panelY = originY + (int) Math.round(anchorMatch[1]);
                    int greenMarkerX = panelX + FALLBACK_ANCHOR_TO_GREEN_MARKER_X;
                    int greenMarkerY = panelY + FALLBACK_ANCHOR_TO_GREEN_MARKER_Y;
                    int templateWidth = readImageWidth(QUXIAO_ZIDONG_PATH);
                    log.info("auto-combat panel anchor matched: source={} center=({}, {}) marker=({}, {})",
                            safeSource, panelX, panelY, greenMarkerX, greenMarkerY);
                    return FindOutcome.matched(new PanelMatch(
                            panelX, panelY, greenMarkerX, greenMarkerY, templateWidth, "panel-anchor"));
                }
            }

            // Green mask + green-marker template fallback at 0.80.
            String washedGreenPath = windowScopedTempPath.resolve(GREEN_MASK_DEBUG_NAME);
            ImagePreprocessor.countGreenPixelsHSV(frame, washedGreenPath);
            double[] greenMatch = ImageFinder.find(washedGreenPath, ZIDONG_GREEN_PATH, GREEN_MARKER_MATCH_RATE);
            if (greenMatch == null || greenMatch.length < 2) {
                log.warn("auto-combat panel not found: source={} (anchor and green-marker both miss)", safeSource);
                return FindOutcome.failed(Status.NOT_FOUND);
            }
            int greenMarkerX = originX + (int) Math.round(greenMatch[0]);
            int greenMarkerY = originY + (int) Math.round(greenMatch[1]);
            int panelX = greenMarkerX - FALLBACK_ANCHOR_TO_GREEN_MARKER_X;
            int panelY = greenMarkerY - FALLBACK_ANCHOR_TO_GREEN_MARKER_Y;
            int templateWidth = readImageWidth(QUXIAO_ZIDONG_PATH);
            log.info("auto-combat panel green marker matched: source={} marker=({}, {}) inferredCenter=({}, {})",
                    safeSource, greenMarkerX, greenMarkerY, panelX, panelY);
            return FindOutcome.matched(new PanelMatch(
                    panelX, panelY, greenMarkerX, greenMarkerY, templateWidth, "green-auto"));
        } catch (RuntimeException e) {
            log.warn("auto-combat panel detection mechanics failed: source={} reason={}", safeSource, e.getMessage(), e);
            return FindOutcome.failed(Status.MECHANICS_FAILED);
        } finally {
            frame.flush();
        }
    }

    private PanelVisibilityResult toResult(EnsureOutcome outcome) {
        if (isSuccess(outcome.status())) {
            return success(outcome.status(), outcome.match(), outcome.reason());
        }
        return failure(outcome.status(), outcome.reason());
    }

    private static BufferedImage readTemplate(String templatePath) {
        try {
            return ImageIO.read(new File(templatePath));
        } catch (IOException e) {
            return null;
        }
    }

    private static int readImageWidth(String imagePath) {
        BufferedImage image;
        try {
            image = ImageIO.read(new File(imagePath));
        } catch (IOException e) {
            return 0;
        }
        if (image == null) {
            return 0;
        }
        int width = image.getWidth();
        image.flush();
        return width;
    }

    private static boolean isInputWorkerThread() {
        return Thread.currentThread().getName().contains(INPUT_ACTION_WORKER_THREAD);
    }

    private static boolean isSuccess(Status status) {
        return status == Status.FOUND
                || status == Status.FOUND_AFTER_OPEN
                || status == Status.ALIGNED
                || status == Status.ALIGNED_WITH_DROP_TARGET_FALLBACK;
    }

    private static PanelVisibilityResult success(Status status, PanelMatch match, String reason) {
        return new PanelVisibilityResult(status,
                match.panelCenterX(), match.panelCenterY(),
                match.greenMarkerX(), match.greenMarkerY(),
                match.templateWidth(), match.detectionSource(), reason);
    }

    private static PanelVisibilityResult failure(Status status, String reason) {
        return new PanelVisibilityResult(status, null, null, null, null, null, null, reason);
    }

    private static String safeSource(String source) {
        if (source == null || source.isBlank()) {
            return "unknown";
        }
        String value = source.trim().replaceAll("[^a-zA-Z0-9._-]", "_");
        return value.length() <= 120 ? value : value.substring(0, 120);
    }
}
