package com.bot.dhxy.service.battleradar;

import com.bot.dhxy.config.TeamTaskProperties;
import com.bot.dhxy.core.ImageFinder;
import com.bot.dhxy.driver.BoundWindowCaptureService;
import com.bot.dhxy.vision.MiniMapCoordinateReader;
import com.bot.dhxy.window.model.WindowNativeBinding;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * W-696-BATTLE-RADAR-DHXY-FACT-1: local mechanical producer for the closed battle-radar window facts.
 *
 * <p>This class only copies the exact local mechanical detections of the committed {@code 696a12b0}
 * {@code BattleRadarService}: the auto-combat flag template, the selection buttons ({@code zhaohuan}
 * OR {@code chehui}), the top icons ({@code nu} AND {@code yuan}), minimap readability, and the 20x20
 * leader-avatar baseline/probe/refresh. It reads only from an exact native-window binding through the
 * per-HWND capture service and never calls the local BattleRadar business transition/signal logic and
 * never touches {@code GameContext}. Template match thresholds, ROI values, template paths, and the
 * OR/AND short-circuit ordering are preserved byte-for-byte from the baseline; only the capture surface
 * moves to the binding-scoped {@link BoundWindowCaptureService}, mirroring the approved common-box/
 * team-return local observation mechanics (raw window-client ROI, no separate scale step).</p>
 *
 * <p>Templates are loaded at the exact evaluation point of each call and flushed before that call
 * returns, exactly like the baseline's per-find load; there is no persistent template cache, and the
 * second OR/AND template is never loaded once the first template has already decided the result.</p>
 */
@Slf4j
@Service
public final class BattleRadarLocalObservationMechanics {

    private static final String BATTLE_FLAG_PATH = "images/template/battle/flag_battle.png";
    private static final String ZHAOHUAN_PATH = "images/template/battle/zhaohuan.png";
    private static final String CHEHUI_PATH = "images/template/battle/chehui.png";
    private static final String NU_PATH = "images/template/battle/nu.png";
    private static final String YUAN_PATH = "images/template/battle/yuan.png";

    // Baseline auto-combat flag ROI (974,630 size 51x20) as window-client left/top/right/bottom.
    private static final int AUTO_ROI_LEFT = 974;
    private static final int AUTO_ROI_TOP = 630;
    private static final int AUTO_ROI_RIGHT = 974 + 51;
    private static final int AUTO_ROI_BOTTOM = 630 + 20;
    private static final double AUTO_THRESHOLD = 0.85D;

    // Baseline selection-button ROI (927,302 size 100x225).
    private static final int SELECTION_ROI_LEFT = 927;
    private static final int SELECTION_ROI_TOP = 302;
    private static final int SELECTION_ROI_RIGHT = 927 + 100;
    private static final int SELECTION_ROI_BOTTOM = 302 + 225;
    private static final double SELECTION_THRESHOLD = 0.8D;

    // Baseline top-icon ROI (456,62 size 123x39).
    private static final int TOP_ROI_LEFT = 456;
    private static final int TOP_ROI_TOP = 62;
    private static final int TOP_ROI_RIGHT = 456 + 123;
    private static final int TOP_ROI_BOTTOM = 62 + 39;
    private static final double TOP_THRESHOLD = 0.8D;

    // Baseline 20x20 fast-expected-exit avatar probe around the configured team hover point.
    private static final int AVATAR_ROI_SIZE = 20;
    private static final int AVATAR_ROI_HALF = AVATAR_ROI_SIZE / 2;
    private static final double AVATAR_DIFF_THRESHOLD = 0.35D;

    private final BoundWindowCaptureService captureService;
    private final MiniMapCoordinateReader miniMapCoordinateReader;
    private final TeamTaskProperties teamTaskProperties;

    private final Map<AvatarBaselineKey, BufferedImage> avatarBaselineCache = new ConcurrentHashMap<>();

    public BattleRadarLocalObservationMechanics(
            BoundWindowCaptureService captureService,
            MiniMapCoordinateReader miniMapCoordinateReader,
            TeamTaskProperties teamTaskProperties) {
        this.captureService = Objects.requireNonNull(captureService, "captureService");
        this.miniMapCoordinateReader = Objects.requireNonNull(
                miniMapCoordinateReader, "miniMapCoordinateReader");
        this.teamTaskProperties = Objects.requireNonNull(teamTaskProperties, "teamTaskProperties");
    }

    /** Stage 1: auto-combat flag template in the bottom-right battle UI (single template). */
    public SignalResult observeAutoFlag(WindowNativeBinding binding) {
        return observeSignal(binding, "autoFlag",
                AUTO_ROI_LEFT, AUTO_ROI_TOP, AUTO_ROI_RIGHT, AUTO_ROI_BOTTOM,
                frame -> matchSingle(frame, BATTLE_FLAG_PATH, AUTO_THRESHOLD));
    }

    /** Stage 2: right-side command buttons; visible when {@code zhaohuan} OR {@code chehui} matches. */
    public SignalResult observeSelectionSignal(WindowNativeBinding binding) {
        return observeSignal(binding, "selection",
                SELECTION_ROI_LEFT, SELECTION_ROI_TOP, SELECTION_ROI_RIGHT, SELECTION_ROI_BOTTOM,
                frame -> matchOr(frame, ZHAOHUAN_PATH, CHEHUI_PATH, SELECTION_THRESHOLD));
    }

    /** Stage 3: top combat icons; visible only when {@code nu} AND {@code yuan} both match. */
    public SignalResult observeTopSignal(WindowNativeBinding binding) {
        return observeSignal(binding, "topIcons",
                TOP_ROI_LEFT, TOP_ROI_TOP, TOP_ROI_RIGHT, TOP_ROI_BOTTOM,
                frame -> matchAnd(frame, NU_PATH, YUAN_PATH, TOP_THRESHOLD));
    }

    /** Stage 4: minimap readability via the current-coordinate reader on the bound window context. */
    public MinimapResult observeMinimapReadable(WindowNativeBinding binding) {
        if (binding == null || !binding.hasNativeHandle()) {
            return new MinimapResult(MinimapStatus.MECHANICS_FAILED);
        }
        try {
            boolean readable = miniMapCoordinateReader.readCurrentCoordinate().isPresent();
            return new MinimapResult(readable ? MinimapStatus.READABLE : MinimapStatus.UNREADABLE);
        } catch (RuntimeException e) {
            log.warn("[battle-radar] minimap readability mechanics failed: hwnd={} reason={}",
                    binding.getNativeHandle(), e.getMessage(), e);
            return new MinimapResult(MinimapStatus.MECHANICS_FAILED);
        }
    }

    /** Capture and store (overwrite) the 20x20 avatar baseline for the exact window identity. */
    public AvatarResult observeAvatarBaseline(
            WindowNativeBinding binding, String windowId, long playerIdentityEpoch) {
        return observeAvatar(binding, windowId, playerIdentityEpoch, AvatarMode.BASELINE);
    }

    /**
     * Compare the current 20x20 avatar frame against the stored baseline. When no baseline exists yet
     * this lazily captures and stores it (mirroring the baseline's committed first-call behaviour) and
     * reports {@code BASELINE_CAPTURED}.
     */
    public AvatarResult observeAvatarProbe(
            WindowNativeBinding binding, String windowId, long playerIdentityEpoch) {
        return observeAvatar(binding, windowId, playerIdentityEpoch, AvatarMode.PROBE);
    }

    /** Recapture and overwrite the 20x20 avatar baseline with the current trusted frame. */
    public AvatarResult observeAvatarRefresh(
            WindowNativeBinding binding, String windowId, long playerIdentityEpoch) {
        return observeAvatar(binding, windowId, playerIdentityEpoch, AvatarMode.REFRESH);
    }

    /**
     * Shared signal capture terminal: a missing binding or an absent capture is {@code
     * CAPTURE_UNAVAILABLE}; a capture or evaluation {@link RuntimeException} is {@code MECHANICS_FAILED}
     * and is never disguised as an absence. The captured frame is flushed before returning.
     */
    private SignalResult observeSignal(
            WindowNativeBinding binding, String source,
            int roiLeft, int roiTop, int roiRight, int roiBottom,
            SignalEvaluator evaluator) {
        if (binding == null || !binding.hasNativeHandle() || !binding.hasGeometry()) {
            return new SignalResult(SignalStatus.CAPTURE_UNAVAILABLE);
        }
        int baseX = binding.getX();
        int baseY = binding.getY();
        Optional<BoundWindowCaptureService.CaptureResult> captured;
        try {
            captured = captureService.captureRegion(binding, baseX, baseY,
                    baseX + roiLeft, baseY + roiTop, baseX + roiRight, baseY + roiBottom);
        } catch (RuntimeException e) {
            log.warn("[battle-radar] signal capture mechanics failed: source={} hwnd={} reason={}",
                    source, binding.getNativeHandle(), e.getMessage(), e);
            return new SignalResult(SignalStatus.MECHANICS_FAILED);
        }
        if (captured == null || captured.isEmpty() || captured.get().image() == null) {
            return new SignalResult(SignalStatus.CAPTURE_UNAVAILABLE);
        }
        BufferedImage frame = captured.get().image();
        try {
            return evaluator.evaluate(frame);
        } catch (RuntimeException e) {
            log.warn("[battle-radar] signal mechanics failed: source={} hwnd={} reason={}",
                    source, binding.getNativeHandle(), e.getMessage(), e);
            return new SignalResult(SignalStatus.MECHANICS_FAILED);
        } finally {
            frame.flush();
        }
    }

    private SignalResult matchSingle(BufferedImage frame, String templatePath, double threshold) {
        return new SignalResult(matchesTemplate(frame, templatePath, threshold)
                ? SignalStatus.VISIBLE
                : SignalStatus.NOT_VISIBLE);
    }

    /**
     * Real short-circuit OR: when the first template already matches the result is {@code VISIBLE} and
     * the second template is never loaded or matched.
     */
    private SignalResult matchOr(
            BufferedImage frame, String firstPath, String secondPath, double threshold) {
        if (matchesTemplate(frame, firstPath, threshold)) {
            return new SignalResult(SignalStatus.VISIBLE);
        }
        return new SignalResult(matchesTemplate(frame, secondPath, threshold)
                ? SignalStatus.VISIBLE
                : SignalStatus.NOT_VISIBLE);
    }

    /**
     * Real short-circuit AND: when the first template does not match the result is {@code NOT_VISIBLE}
     * and the second template is never loaded or matched.
     */
    private SignalResult matchAnd(
            BufferedImage frame, String firstPath, String secondPath, double threshold) {
        if (!matchesTemplate(frame, firstPath, threshold)) {
            return new SignalResult(SignalStatus.NOT_VISIBLE);
        }
        return new SignalResult(matchesTemplate(frame, secondPath, threshold)
                ? SignalStatus.VISIBLE
                : SignalStatus.NOT_VISIBLE);
    }

    /**
     * Loads one template at this exact evaluation point, matches it against the captured frame, and
     * flushes the loaded template before returning. A template that cannot be loaded is a mechanics
     * failure and is surfaced by throwing, so the caller maps it to {@code MECHANICS_FAILED}.
     */
    private boolean matchesTemplate(BufferedImage frame, String templatePath, double threshold) {
        BufferedImage template;
        try {
            template = ImageIO.read(Path.of(templatePath).toFile());
        } catch (Exception e) {
            throw new IllegalStateException("battle-radar template load failed: " + templatePath, e);
        }
        if (template == null) {
            throw new IllegalStateException("battle-radar template unavailable: " + templatePath);
        }
        try {
            return ImageFinder.find(frame, template, threshold) != null;
        } finally {
            template.flush();
        }
    }

    private AvatarResult observeAvatar(
            WindowNativeBinding binding, String windowId, long playerIdentityEpoch, AvatarMode mode) {
        int hoverX = teamTaskProperties.getTeamHoverX();
        int hoverY = teamTaskProperties.getTeamHoverY();
        if (hoverX <= 0 || hoverY <= 0) {
            return AvatarResult.stateOnly(AvatarStatus.NOT_CONFIGURED);
        }
        if (binding == null || !binding.hasNativeHandle() || !binding.hasGeometry()) {
            return AvatarResult.stateOnly(AvatarStatus.UNAVAILABLE);
        }

        int baseX = binding.getX();
        int baseY = binding.getY();
        int clientLeft = hoverX - AVATAR_ROI_HALF;
        int clientTop = hoverY - AVATAR_ROI_HALF;
        int clientRight = hoverX + AVATAR_ROI_HALF;
        int clientBottom = hoverY + AVATAR_ROI_HALF;
        int roiScreenLeft = baseX + clientLeft;
        int roiScreenTop = baseY + clientTop;
        int roiScreenRight = baseX + clientRight;
        int roiScreenBottom = baseY + clientBottom;

        try {
            Optional<BoundWindowCaptureService.CaptureResult> captured = captureService.captureRegion(
                    binding, baseX, baseY, roiScreenLeft, roiScreenTop, roiScreenRight, roiScreenBottom);
            if (captured == null || captured.isEmpty() || captured.get().image() == null) {
                return AvatarResult.located(AvatarStatus.UNAVAILABLE, hoverX, hoverY,
                        roiScreenLeft, roiScreenTop, roiScreenRight, roiScreenBottom);
            }
            BufferedImage current = captured.get().image();
            AvatarBaselineKey key = new AvatarBaselineKey(
                    windowId, binding.getNativeHandle(), playerIdentityEpoch);

            if (mode == AvatarMode.BASELINE || mode == AvatarMode.REFRESH) {
                storeBaseline(key, current);
                return AvatarResult.located(AvatarStatus.BASELINE_CAPTURED, hoverX, hoverY,
                        roiScreenLeft, roiScreenTop, roiScreenRight, roiScreenBottom);
            }

            BufferedImage baseline = avatarBaselineCache.get(key);
            if (baseline == null) {
                storeBaseline(key, current);
                return AvatarResult.located(AvatarStatus.BASELINE_CAPTURED, hoverX, hoverY,
                        roiScreenLeft, roiScreenTop, roiScreenRight, roiScreenBottom);
            }
            try {
                boolean unchanged = ImageFinder.isMatch(baseline, current, AVATAR_DIFF_THRESHOLD);
                return AvatarResult.located(
                        unchanged ? AvatarStatus.UNCHANGED : AvatarStatus.CHANGED,
                        hoverX, hoverY, roiScreenLeft, roiScreenTop, roiScreenRight, roiScreenBottom);
            } finally {
                current.flush();
            }
        } catch (RuntimeException e) {
            log.warn("[battle-radar] avatar mechanics failed: mode={} hwnd={} reason={}",
                    mode, binding.getNativeHandle(), e.getMessage(), e);
            return AvatarResult.located(AvatarStatus.MECHANICS_FAILED, hoverX, hoverY,
                    roiScreenLeft, roiScreenTop, roiScreenRight, roiScreenBottom);
        }
    }

    private void storeBaseline(AvatarBaselineKey key, BufferedImage image) {
        BufferedImage previous = avatarBaselineCache.put(key, image);
        if (previous != null && previous != image) {
            previous.flush();
        }
    }

    @FunctionalInterface
    private interface SignalEvaluator {
        SignalResult evaluate(BufferedImage frame);
    }

    private enum AvatarMode {
        BASELINE,
        PROBE,
        REFRESH
    }

    public enum SignalStatus {
        VISIBLE,
        NOT_VISIBLE,
        CAPTURE_UNAVAILABLE,
        MECHANICS_FAILED
    }

    public enum MinimapStatus {
        READABLE,
        UNREADABLE,
        MECHANICS_FAILED
    }

    public enum AvatarStatus {
        BASELINE_CAPTURED,
        UNCHANGED,
        CHANGED,
        UNAVAILABLE,
        NOT_CONFIGURED,
        MECHANICS_FAILED
    }

    public record SignalResult(SignalStatus status) {
        public SignalResult {
            Objects.requireNonNull(status, "status");
        }
    }

    public record MinimapResult(MinimapStatus status) {
        public MinimapResult {
            Objects.requireNonNull(status, "status");
        }
    }

    public record AvatarResult(
            AvatarStatus status,
            Integer hoverClientX,
            Integer hoverClientY,
            Integer roiScreenLeft,
            Integer roiScreenTop,
            Integer roiScreenRight,
            Integer roiScreenBottom) {

        public AvatarResult {
            Objects.requireNonNull(status, "status");
            boolean hasAny = hoverClientX != null || hoverClientY != null
                    || roiScreenLeft != null || roiScreenTop != null
                    || roiScreenRight != null || roiScreenBottom != null;
            boolean hasAll = hoverClientX != null && hoverClientY != null
                    && roiScreenLeft != null && roiScreenTop != null
                    && roiScreenRight != null && roiScreenBottom != null;
            if (hasAny && !hasAll) {
                throw new IllegalArgumentException("avatar result coordinates must be a full group");
            }
        }

        private static AvatarResult stateOnly(AvatarStatus status) {
            return new AvatarResult(status, null, null, null, null, null, null);
        }

        private static AvatarResult located(
                AvatarStatus status, int hoverClientX, int hoverClientY,
                int roiScreenLeft, int roiScreenTop, int roiScreenRight, int roiScreenBottom) {
            return new AvatarResult(status, hoverClientX, hoverClientY,
                    roiScreenLeft, roiScreenTop, roiScreenRight, roiScreenBottom);
        }
    }

    private record AvatarBaselineKey(String windowId, String nativeHandle, long playerIdentityEpoch) {
    }
}
