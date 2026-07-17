package com.bot.dhxy.service.playerstate;

import com.bot.dhxy.core.ImageFinder;
import com.bot.dhxy.core.TextRecognizer;
import com.bot.dhxy.driver.BoundWindowCaptureService;
import com.bot.dhxy.input.InputProvider;
import com.bot.dhxy.input.InputSequences;
import com.bot.dhxy.input.action.InputAction;
import com.bot.dhxy.model.ocr.OcrWordResult;
import com.bot.dhxy.tools.CoordinateHelper;
import com.bot.dhxy.tools.ImagePreprocessor;
import com.bot.dhxy.vision.SheyaoxiangDigitTemplateReader;
import com.bot.dhxy.window.model.WindowNativeBinding;
import com.bot.dhxy.window.runtime.WindowScopedTempPath;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.PointerInfo;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * W-696-LOCAL-OCR-INCENSE-COHORT-1: closed local observation mechanics for the incense (摄妖香) status,
 * extracted from the committed {@code 696a12b0} {@code PlayerStateService:1002-1297}.
 *
 * <p>The two original caller operations are kept as two distinct public mechanical operations and are
 * never merged behind one entry:</p>
 * <ul>
 *   <li>{@link #probeIncenseStatus} mirrors {@code probeIncenseStatus:1002-1054}: exactly one
 *       full-status-rect capture, icon template match, matched-column crop, and cyan-hour-first /
 *       green-minute-fallback time read.</li>
 *   <li>{@link #probeIncenseIconPresence} mirrors {@code probeIncenseIconPresence:1056-1068}: a cached
 *       narrow probe that returns immediately on {@code PRESENT} or {@code UNKNOWN} and only falls back
 *       to the full status rect on {@code ABSENT}.</li>
 * </ul>
 *
 * <p>Both operations validate the complete screen-absolute rectangle with overflow-safe comparisons,
 * and that it lies fully inside the binding, before any mouse move, sleep, or capture; a cached offset
 * only ever produces a legal probe rectangle inside the binding/status rect. Capture that returns no
 * frame is reported as unavailable, while a capture mechanics exception reaches the mechanics-failure
 * (status) or unknown (presence) terminal and is never downgraded to a plain visual miss. The icon
 * template threshold {@code 0.85}, matched column, cyan/green order, digit template learning, image
 * flushes, and millisecond units are preserved. It never calls Bag, never decides whether to use
 * incense, never updates PlayerState/cache/cooldown, and owns no retry/TTL/owner/session/ledger.</p>
 */
@Slf4j
@Service
public final class PlayerStateIncenseStatusLocalObservationMechanics {

    private static final String SHEYAOXIANG_STATUS_TEMPLATE = "images/template/status/sheyaoxiang_buff.png";
    private static final double SHEYAOXIANG_STATUS_MATCH_RATE = 0.85;
    private static final int SHEYAOXIANG_DIGIT_OCR_SCALE = 6;
    private static final Pattern SHEYAOXIANG_REMAINING_HOUR_PATTERN = Pattern.compile("\\d{1,2}");
    private static final long ONE_HOUR_MS = 60 * 60 * 1000L;

    private static final int INCENSE_CACHED_ICON_PROBE_WIDTH = 48;
    private static final int INCENSE_CACHED_ICON_PROBE_LEFT_PADDING = 6;

    private static final int GAME_CLIENT_WIDTH = 1024;
    private static final int GAME_CLIENT_HEIGHT = 768;
    private static final int SAFE_MOUSE_FORBIDDEN_LEFT_REL_X = 761;
    private static final int SAFE_MOUSE_FORBIDDEN_BOTTOM_REL_Y = 147;
    private static final int SAFE_MOUSE_HOVER_CLEAR_DELAY_MS = 300;
    private static final int PLAYER_STATE_MOUSE_OBSTRUCTION_PADDING = 12;
    private static final String INPUT_WORKER_THREAD_MARK = "dhxy-input-action-worker";

    private final BoundWindowCaptureService captureService;
    private final InputProvider inputProvider;
    private final InputSequences inputSequences;
    private final CoordinateHelper coordinateHelper;
    private final TextRecognizer textRecognizer;
    private final WindowScopedTempPath windowScopedTempPath;
    private final SheyaoxiangDigitTemplateReader sheyaoxiangDigitTemplateReader = new SheyaoxiangDigitTemplateReader();

    public PlayerStateIncenseStatusLocalObservationMechanics(BoundWindowCaptureService captureService,
                                                             InputProvider inputProvider,
                                                             InputSequences inputSequences,
                                                             CoordinateHelper coordinateHelper,
                                                             TextRecognizer textRecognizer,
                                                             WindowScopedTempPath windowScopedTempPath) {
        this.captureService = Objects.requireNonNull(captureService, "captureService");
        this.inputProvider = Objects.requireNonNull(inputProvider, "inputProvider");
        this.inputSequences = Objects.requireNonNull(inputSequences, "inputSequences");
        this.coordinateHelper = Objects.requireNonNull(coordinateHelper, "coordinateHelper");
        this.textRecognizer = Objects.requireNonNull(textRecognizer, "textRecognizer");
        this.windowScopedTempPath = Objects.requireNonNull(windowScopedTempPath, "windowScopedTempPath");
    }

    /**
     * Full incense-status read mirroring baseline {@code probeIncenseStatus}: exactly one full-rect
     * capture, template match, matched-column crop, and cyan/green time read.
     *
     * @param binding exact native-window binding; screen-absolute base with a handle and geometry
     * @param statusRect screen-absolute status panel rectangle {@code [x1,y1,x2,y2]}
     * @param source diagnostic source label
     * @return non-null closed observation result
     */
    public IncenseStatusObservation probeIncenseStatus(WindowNativeBinding binding, int[] statusRect, String source) {
        if (!isCompleteRectWithinBinding(binding, statusRect)) {
            return IncenseStatusObservation.mechanicsFailure("invalid-rect source=" + safe(source));
        }
        BufferedImage statusImage;
        try {
            moveMouseAwayBeforePlayerStateSnapshotIfNeeded(binding, "sheyaoxiang-status", statusRect);
            statusImage = capture(binding, statusRect);
        } catch (RuntimeException e) {
            log.warn("sheyaoxiang status capture mechanics failed: source={} reason={}", safe(source), e.getMessage(), e);
            return IncenseStatusObservation.mechanicsFailure(e.getMessage());
        }
        if (statusImage == null) {
            log.warn("sheyaoxiang status capture unavailable: rect=({}, {})-({}, {})",
                    statusRect[0], statusRect[1], statusRect[2], statusRect[3]);
            return IncenseStatusObservation.captureUnavailable("capture-null source=" + safe(source));
        }
        try {
            double[] match = matchIconTemplate(statusImage, "sheyaoxiang_status_raw.png");
            if (match == null) {
                log.info("sheyaoxiang status template not matched: template={}", SHEYAOXIANG_STATUS_TEMPLATE);
                return IncenseStatusObservation.templateAbsent("template-miss source=" + safe(source));
            }
            int iconAbsX = statusRect[0] + (int) Math.round(match[0]);
            int iconAbsY = statusRect[1] + (int) Math.round(match[1]);
            int iconOffsetX = Math.max(0, iconAbsX - statusRect[0]);
            int iconOffsetY = Math.max(0, iconAbsY - statusRect[1]);

            TimeReadResult timeRead;
            BufferedImage matchedColumn = cropSheyaoxiangMatchedColumn(statusImage, match, statusRect, safe(source));
            if (matchedColumn != null) {
                try {
                    String columnPath = windowScopedTempPath.resolve("sheyaoxiang_status_matched_column_raw.png");
                    writeImage(matchedColumn, columnPath, "sheyaoxiang matched column raw");
                    timeRead = readSheyaoxiangRemainingTime(matchedColumn);
                } finally {
                    matchedColumn.flush();
                }
            } else {
                timeRead = readSheyaoxiangRemainingTime(statusImage);
            }

            log.info("sheyaoxiang status matched: point=({}, {}) offset=({}, {}) timeStatus={} remaining={}",
                    iconAbsX, iconAbsY, iconOffsetX, iconOffsetY, timeRead.status(), timeRead.describe());
            return switch (timeRead.status()) {
                case FOUND -> IncenseStatusObservation.remainingTimeFound(
                        iconAbsX, iconAbsY, iconOffsetX, iconOffsetY, timeRead.remainingMs(), timeRead.describe());
                case OCR_UNAVAILABLE -> IncenseStatusObservation.ocrUnavailable(
                        iconAbsX, iconAbsY, iconOffsetX, iconOffsetY, timeRead.describe());
                case UNREADABLE -> IncenseStatusObservation.iconPresentTimeUnreadable(
                        iconAbsX, iconAbsY, iconOffsetX, iconOffsetY, timeRead.describe());
            };
        } catch (RuntimeException e) {
            log.warn("sheyaoxiang status read mechanics failed: source={} reason={}", safe(source), e.getMessage(), e);
            return IncenseStatusObservation.mechanicsFailure(e.getMessage());
        } finally {
            statusImage.flush();
        }
    }

    /**
     * Cached icon-presence gate mirroring baseline {@code probeIncenseIconPresence}: probe the cached
     * narrow rectangle first, return immediately when it is PRESENT or UNKNOWN, and only fall back to
     * the full status rectangle when the cached probe is ABSENT.
     *
     * @param binding exact native-window binding; screen-absolute base with a handle and geometry
     * @param statusRect screen-absolute status panel rectangle {@code [x1,y1,x2,y2]}
     * @param cachedIconOffsetX optional cached icon X offset from the status rect origin; null/negative disables the narrow gate
     * @param cachedIconOffsetY optional cached icon Y offset from the status rect origin; null/negative disables the narrow gate
     * @param source diagnostic source label
     * @return non-null closed presence result
     */
    public IncenseIconPresenceResult probeIncenseIconPresence(WindowNativeBinding binding,
                                                             int[] statusRect,
                                                             Integer cachedIconOffsetX,
                                                             Integer cachedIconOffsetY,
                                                             String source) {
        if (!isCompleteRectWithinBinding(binding, statusRect)) {
            return IncenseIconPresenceResult.unknown("invalid-rect source=" + safe(source));
        }
        try {
            boolean hasCachedOffset = cachedIconOffsetX != null && cachedIconOffsetY != null
                    && cachedIconOffsetX >= 0 && cachedIconOffsetY >= 0;
            if (hasCachedOffset) {
                int[] cachedRect = cachedIncenseIconProbeRect(statusRect, cachedIconOffsetX);
                if (isCompleteRectWithinBinding(binding, cachedRect)) {
                    IncenseIconPresenceResult cachedProbe = probeIconPresenceInRect(
                            binding, statusRect, cachedRect, "cached-point");
                    if (cachedProbe.presence() != IconPresence.ABSENT) {
                        return cachedProbe;
                    }
                    log.info("sheyaoxiang cached icon probe missed; fallback to full status rect. cachedOffset=({}, {}) cachedRect=({}, {})-({}, {})",
                            cachedIconOffsetX, cachedIconOffsetY,
                            cachedRect[0], cachedRect[1], cachedRect[2], cachedRect[3]);
                }
            }
            return probeIconPresenceInRect(binding, statusRect, statusRect, "status-rect");
        } catch (RuntimeException e) {
            log.warn("sheyaoxiang icon presence mechanics failed: source={} reason={}", safe(source), e.getMessage(), e);
            return IncenseIconPresenceResult.unknown("exception");
        }
    }

    private IncenseIconPresenceResult probeIconPresenceInRect(WindowNativeBinding binding,
                                                             int[] statusRect,
                                                             int[] probeRect,
                                                             String mode) {
        moveMouseAwayBeforePlayerStateSnapshotIfNeeded(binding, "sheyaoxiang-status-icon-gate-" + mode, probeRect);
        BufferedImage statusImage = capture(binding, probeRect);
        if (statusImage == null) {
            log.warn("sheyaoxiang memory-gate icon capture unavailable: mode={} rect=({}, {})-({}, {})",
                    mode, probeRect[0], probeRect[1], probeRect[2], probeRect[3]);
            return IncenseIconPresenceResult.unknown("capture-failed");
        }
        try {
            double[] match = matchIconTemplate(statusImage, "sheyaoxiang_status_icon_gate_" + mode + ".png");
            if (match == null) {
                log.info("sheyaoxiang memory-gate icon template absent: mode={} template={}",
                        mode, SHEYAOXIANG_STATUS_TEMPLATE);
                return IncenseIconPresenceResult.absent("template-miss");
            }
            int iconAbsX = probeRect[0] + (int) Math.round(match[0]);
            int iconAbsY = probeRect[1] + (int) Math.round(match[1]);
            int iconOffsetX = Math.max(0, iconAbsX - statusRect[0]);
            int iconOffsetY = Math.max(0, iconAbsY - statusRect[1]);
            log.info("sheyaoxiang memory-gate icon present: mode={} point=({}, {}) offset=({}, {}) score={}",
                    mode, iconAbsX, iconAbsY, iconOffsetX, iconOffsetY, match.length >= 3 ? match[2] : -1);
            return IncenseIconPresenceResult.present(iconAbsX, iconAbsY, iconOffsetX, iconOffsetY);
        } finally {
            statusImage.flush();
        }
    }

    private BufferedImage capture(WindowNativeBinding binding, int[] rect) {
        Optional<BoundWindowCaptureService.CaptureResult> captured = captureService.captureRegion(
                binding, binding.getX(), binding.getY(), rect[0], rect[1], rect[2], rect[3]);
        if (captured == null || captured.isEmpty() || captured.get().image() == null) {
            return null;
        }
        return captured.get().image();
    }

    private double[] matchIconTemplate(BufferedImage image, String rawFileName) {
        String rawPath = windowScopedTempPath.resolve(rawFileName);
        writeImage(image, rawPath, "sheyaoxiang raw " + rawFileName);
        double[] match = ImageFinder.find(rawPath, SHEYAOXIANG_STATUS_TEMPLATE, SHEYAOXIANG_STATUS_MATCH_RATE);
        if (match == null || match.length < 2) {
            return null;
        }
        return match;
    }

    private int[] cachedIncenseIconProbeRect(int[] statusRect, int cachedIconOffsetX) {
        int panelLeft = statusRect[0];
        int panelTop = statusRect[1];
        int panelRight = statusRect[2];
        int panelBottom = statusRect[3];
        int panelWidth = Math.max(1, panelRight - panelLeft);
        int probeWidth = Math.min(panelWidth, INCENSE_CACHED_ICON_PROBE_WIDTH);
        int cachedAbsX = panelLeft + cachedIconOffsetX;
        int left = cachedAbsX - INCENSE_CACHED_ICON_PROBE_LEFT_PADDING;
        left = Math.max(panelLeft, Math.min(left, panelRight - probeWidth));
        return new int[]{left, panelTop, left + probeWidth, panelBottom};
    }

    private static boolean isCompleteRectWithinBinding(WindowNativeBinding binding, int[] rect) {
        if (binding == null || !binding.hasNativeHandle() || !binding.hasGeometry()
                || rect == null || rect.length < 4) {
            return false;
        }
        long x1 = rect[0];
        long y1 = rect[1];
        long x2 = rect[2];
        long y2 = rect[3];
        if (x2 <= x1 || y2 <= y1) {
            return false;
        }
        long baseX = binding.getX();
        long baseY = binding.getY();
        long right = baseX + binding.getWidth();
        long bottom = baseY + binding.getHeight();
        return x1 >= baseX && y1 >= baseY && x2 <= right && y2 <= bottom;
    }

    private void moveMouseAwayBeforePlayerStateSnapshotIfNeeded(WindowNativeBinding binding,
                                                               String source,
                                                               int[] captureRect) {
        Point mouse = currentLogicalMousePoint();
        if (!mouseOverCaptureRect(mouse, captureRect)) {
            log.debug("player-state snapshot mouse clear: source={} mouse={} rect={}",
                    source, formatPoint(mouse), formatRect(captureRect));
            return;
        }
        if (binding.getX() == -1 || binding.getY() == -1) {
            return;
        }
        Point safePoint = randomMouseAwayPoint(binding.getX(), binding.getY());
        if (isInputWorkerThread()) {
            log.info("player-state snapshot mouse overlaps capture; move away directly before snapshot: source={} mouse={} rect={} target={}",
                    source, formatPoint(mouse), formatRect(captureRect), formatPoint(safePoint));
            inputProvider.moveMouse(safePoint.x, safePoint.y);
            sleep(SAFE_MOUSE_HOVER_CLEAR_DELAY_MS);
        } else {
            log.info("player-state snapshot mouse overlaps capture; move away before snapshot: source={} mouse={} rect={} target={}",
                    source, formatPoint(mouse), formatRect(captureRect), formatPoint(safePoint));
            inputSequences.submitAndWait("playerState:moveMouseAwayBeforeSnapshot", List.of(
                    InputAction.moveMouse(safePoint.x, safePoint.y),
                    InputAction.sleep(SAFE_MOUSE_HOVER_CLEAR_DELAY_MS)
            ));
        }
    }

    private boolean mouseOverCaptureRect(Point mouse, int[] captureRect) {
        if (mouse == null || captureRect == null || captureRect.length < 4) {
            return false;
        }
        int left = Math.min(captureRect[0], captureRect[2]) - PLAYER_STATE_MOUSE_OBSTRUCTION_PADDING;
        int right = Math.max(captureRect[0], captureRect[2]) + PLAYER_STATE_MOUSE_OBSTRUCTION_PADDING;
        int top = Math.min(captureRect[1], captureRect[3]) - PLAYER_STATE_MOUSE_OBSTRUCTION_PADDING;
        int bottom = Math.max(captureRect[1], captureRect[3]) + PLAYER_STATE_MOUSE_OBSTRUCTION_PADDING;
        return mouse.x >= left && mouse.x <= right && mouse.y >= top && mouse.y <= bottom;
    }

    private Point randomMouseAwayPoint(int baseX, int baseY) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        int relX;
        int relY;
        do {
            relX = random.nextInt(GAME_CLIENT_WIDTH);
            relY = random.nextInt(GAME_CLIENT_HEIGHT);
        } while (relX >= SAFE_MOUSE_FORBIDDEN_LEFT_REL_X && relY <= SAFE_MOUSE_FORBIDDEN_BOTTOM_REL_Y);
        return new Point(baseX + relX, baseY + relY);
    }

    private Point currentLogicalMousePoint() {
        PointerInfo pointerInfo = MouseInfo.getPointerInfo();
        if (pointerInfo == null) {
            return null;
        }
        double scale = coordinateHelper.getScaleRatio();
        Point physical = pointerInfo.getLocation();
        return new Point((int) Math.round(physical.x / scale), (int) Math.round(physical.y / scale));
    }

    private String formatPoint(Point point) {
        return point == null ? "unknown" : "(" + point.x + ", " + point.y + ")";
    }

    private String formatRect(int[] rect) {
        if (rect == null || rect.length < 4) {
            return "unknown";
        }
        return "(" + rect[0] + ", " + rect[1] + ")-(" + rect[2] + ", " + rect[3] + ")";
    }

    private BufferedImage cropSheyaoxiangMatchedColumn(BufferedImage statusImage,
                                                       double[] match,
                                                       int[] statusRect,
                                                       String source) {
        BufferedImage template = null;
        try {
            template = ImageIO.read(new File(SHEYAOXIANG_STATUS_TEMPLATE));
            if (template == null) {
                log.warn("sheyaoxiang matched column crop skipped: source={} template not readable path={}",
                        source, SHEYAOXIANG_STATUS_TEMPLATE);
                return null;
            }

            int left = Math.max(0, (int) Math.round(match[0] - template.getWidth() / 2.0));
            int right = Math.min(statusImage.getWidth(), left + template.getWidth());
            if (right <= left) {
                log.warn("sheyaoxiang matched column crop skipped: source={} localLeft={} localRight={} imageSize={}x{}",
                        source, left, right, statusImage.getWidth(), statusImage.getHeight());
                return null;
            }

            int absLeft = statusRect[0] + left;
            int absRight = statusRect[0] + right;
            log.info("sheyaoxiang matched column crop: source={} localX=({}, {}) absX=({}, {}) height={} templateSize={}x{}",
                    source, left, right, absLeft, absRight, statusImage.getHeight(),
                    template.getWidth(), template.getHeight());
            return ImagePreprocessor.cropCopy(
                    statusImage, left, 0, right - left, statusImage.getHeight());
        } catch (IOException e) {
            log.warn("sheyaoxiang matched column crop failed: source={} reason={}", source, e.getMessage(), e);
            return null;
        } finally {
            if (template != null) {
                template.flush();
            }
        }
    }

    private TimeReadResult readSheyaoxiangRemainingTime(BufferedImage statusImage) {
        BufferedImage washed = new BufferedImage(
                statusImage.getWidth() * SHEYAOXIANG_DIGIT_OCR_SCALE,
                statusImage.getHeight() * SHEYAOXIANG_DIGIT_OCR_SCALE,
                BufferedImage.TYPE_INT_RGB);
        try {
            for (int y = 0; y < statusImage.getHeight(); y++) {
                for (int x = 0; x < statusImage.getWidth(); x++) {
                    int rgb = statusImage.getRGB(x, y) & 0xFFFFFF;
                    int outputRgb = isSheyaoxiangCyanDigitPixel(rgb) ? 0x000000 : 0xFFFFFF;
                    for (int dy = 0; dy < SHEYAOXIANG_DIGIT_OCR_SCALE; dy++) {
                        for (int dx = 0; dx < SHEYAOXIANG_DIGIT_OCR_SCALE; dx++) {
                            washed.setRGB(
                                    x * SHEYAOXIANG_DIGIT_OCR_SCALE + dx,
                                    y * SHEYAOXIANG_DIGIT_OCR_SCALE + dy,
                                    outputRgb);
                        }
                    }
                }
            }

            String washedPath = windowScopedTempPath.resolve("sheyaoxiang_status_cyan_digits.png");
            writeImage(washed, washedPath, "sheyaoxiang cyan digit OCR");
            Optional<List<OcrWordResult>> wordsOptional = textRecognizer.getAllTextResultsLocalOnly(washedPath);
            if (wordsOptional.isEmpty()) {
                log.info("sheyaoxiang cyan digit OCR unavailable: path={}", washedPath);
                return TimeReadResult.ocrUnavailable("cyan-ocr-unavailable");
            }
            List<OcrWordResult> words = wordsOptional.get();
            String text = words.stream()
                    .map(OcrWordResult::getText)
                    .filter(value -> value != null && !value.isBlank())
                    .reduce("", String::concat);
            Matcher matcher = SHEYAOXIANG_REMAINING_HOUR_PATTERN.matcher(text == null ? "" : text);
            if (!matcher.find()) {
                log.info("sheyaoxiang cyan digit OCR returned no hour digits: path={} text='{}'",
                        washedPath, text);
                return readSheyaoxiangRemainingMinutesGreen(statusImage);
            }
            int hours = Integer.parseInt(matcher.group());
            if (hours <= 0) {
                log.info("sheyaoxiang cyan digit OCR ignored non-positive hour value: text='{}'", text);
                return readSheyaoxiangRemainingMinutesGreen(statusImage);
            }
            long remainingMs = hours * ONE_HOUR_MS;
            log.info("sheyaoxiang cyan digit OCR matched remainingHours={} remainingMinutes={} path={} text='{}'",
                    hours, remainingMs / 60000, washedPath, text);
            return TimeReadResult.found(remainingMs, "cyan-hours=" + hours);
        } finally {
            washed.flush();
        }
    }

    private TimeReadResult readSheyaoxiangRemainingMinutesGreen(BufferedImage statusImage) {
        BufferedImage washed = new BufferedImage(
                statusImage.getWidth() * SHEYAOXIANG_DIGIT_OCR_SCALE,
                statusImage.getHeight() * SHEYAOXIANG_DIGIT_OCR_SCALE,
                BufferedImage.TYPE_INT_RGB);
        try {
            for (int y = 0; y < statusImage.getHeight(); y++) {
                for (int x = 0; x < statusImage.getWidth(); x++) {
                    int rgb = statusImage.getRGB(x, y) & 0xFFFFFF;
                    int outputRgb = isSheyaoxiangGreenDigitPixel(rgb) ? 0x000000 : 0xFFFFFF;
                    for (int dy = 0; dy < SHEYAOXIANG_DIGIT_OCR_SCALE; dy++) {
                        for (int dx = 0; dx < SHEYAOXIANG_DIGIT_OCR_SCALE; dx++) {
                            washed.setRGB(
                                    x * SHEYAOXIANG_DIGIT_OCR_SCALE + dx,
                                    y * SHEYAOXIANG_DIGIT_OCR_SCALE + dy,
                                    outputRgb);
                        }
                    }
                }
            }

            String washedPath = windowScopedTempPath.resolve("sheyaoxiang_status_green_digits.png");
            writeImage(washed, washedPath, "sheyaoxiang green digit OCR");
            Optional<List<OcrWordResult>> wordsOptional = textRecognizer.getAllTextResultsLocalOnly(washedPath);
            if (wordsOptional.isEmpty()) {
                log.info("sheyaoxiang green digit OCR unavailable: path={}", washedPath);
                return TimeReadResult.ocrUnavailable("green-ocr-unavailable");
            }
            List<OcrWordResult> words = wordsOptional.get();
            String text = words.stream()
                    .map(OcrWordResult::getText)
                    .filter(value -> value != null && !value.isBlank())
                    .reduce("", String::concat);
            SheyaoxiangDigitTemplateReader.Result templateResult =
                    sheyaoxiangDigitTemplateReader.recognizeAndLearn(washed, words, washedPath);
            if (!templateResult.learnedSymbols().isEmpty()) {
                log.info("sheyaoxiang green digit template learned symbols={} digitCount={} path={} ocrText='{}'",
                        templateResult.learnedSymbols(), templateResult.digitCount(), washedPath, text);
            }
            if (templateResult.reliable() && templateResult.text() != null && !templateResult.text().isBlank()) {
                int minutes = Integer.parseInt(templateResult.text());
                if (minutes > 0) {
                    long remainingMs = minutes * 60000L;
                    log.info("sheyaoxiang green digit template matched remainingMinutes={} path={} text='{}' ocrText='{}'",
                            minutes, washedPath, templateResult.text(), text);
                    return TimeReadResult.found(remainingMs, "green-minutes-template=" + minutes);
                }
            }
            String digitsOnly = text == null ? "" : text.replaceAll("\\D+", "");
            if (templateResult.digitCount() > 1 && digitsOnly.length() < templateResult.digitCount()) {
                log.info("sheyaoxiang green digit OCR partial while templates are learning: digitCount={} path={} text='{}'",
                        templateResult.digitCount(), washedPath, text);
                return TimeReadResult.unreadable("green-digits-learning=" + templateResult.digitCount());
            }
            Matcher matcher = SHEYAOXIANG_REMAINING_HOUR_PATTERN.matcher(text == null ? "" : text);
            if (!matcher.find()) {
                log.info("sheyaoxiang green digit OCR returned no minute digits: path={} text='{}'",
                        washedPath, text);
                return TimeReadResult.unreadable("none");
            }
            int minutes = Integer.parseInt(matcher.group());
            if (minutes <= 0) {
                log.info("sheyaoxiang green digit OCR ignored non-positive minute value: text='{}'", text);
                return TimeReadResult.unreadable("green-minutes<=0");
            }
            long remainingMs = minutes * 60000L;
            log.info("sheyaoxiang green digit OCR matched remainingMinutes={} path={} text='{}'",
                    minutes, washedPath, text);
            return TimeReadResult.found(remainingMs, "green-minutes=" + minutes);
        } finally {
            washed.flush();
        }
    }

    private boolean isSheyaoxiangCyanDigitPixel(int rgb) {
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        return r <= 120 && g >= 130 && b >= 130 && Math.abs(g - b) <= 80;
    }

    private boolean isSheyaoxiangGreenDigitPixel(int rgb) {
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        return g >= 120 && r <= 120 && b <= 120 && g >= r + 50 && g >= b + 50;
    }

    private void writeImage(BufferedImage image, String path, String label) {
        try {
            ImageIO.write(image, "png", new File(path));
        } catch (IOException e) {
            log.warn("write {} image failed: path={} reason={}", label, path, e.getMessage(), e);
        }
    }

    private boolean isInputWorkerThread() {
        return Thread.currentThread().getName().contains(INPUT_WORKER_THREAD_MARK);
    }

    private static void sleep(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static String safe(String value) {
        return value == null ? "unknown" : value;
    }

    private enum TimeReadStatus {
        FOUND,
        UNREADABLE,
        OCR_UNAVAILABLE
    }

    private record TimeReadResult(TimeReadStatus status, Long remainingMs, String describe) {

        private static TimeReadResult found(long remainingMs, String describe) {
            return new TimeReadResult(TimeReadStatus.FOUND, remainingMs, describe);
        }

        private static TimeReadResult unreadable(String describe) {
            return new TimeReadResult(TimeReadStatus.UNREADABLE, null, describe);
        }

        private static TimeReadResult ocrUnavailable(String describe) {
            return new TimeReadResult(TimeReadStatus.OCR_UNAVAILABLE, null, describe);
        }
    }

    public enum Status {
        CAPTURE_UNAVAILABLE,
        TEMPLATE_ABSENT,
        OCR_UNAVAILABLE,
        ICON_PRESENT_TIME_UNREADABLE,
        REMAINING_TIME_FOUND,
        MECHANICS_FAILURE
    }

    /**
     * Closed immutable incense-status observation from {@link #probeIncenseStatus}. The icon point and
     * offset are all present together only for the icon-bearing statuses ({@code OCR_UNAVAILABLE},
     * {@code ICON_PRESENT_TIME_UNREADABLE}, {@code REMAINING_TIME_FOUND}) and all absent otherwise.
     * {@code remainingMs} is present only for {@link Status#REMAINING_TIME_FOUND}. Coordinates are
     * screen-absolute pixels except the offset, which is relative to the status rect origin.
     */
    public record IncenseStatusObservation(Status status,
                                           Integer iconScreenAbsX,
                                           Integer iconScreenAbsY,
                                           Integer iconOffsetX,
                                           Integer iconOffsetY,
                                           Long remainingMs,
                                           String source) {

        public IncenseStatusObservation {
            Objects.requireNonNull(status, "status");
            boolean hasAnyIcon = iconScreenAbsX != null || iconScreenAbsY != null
                    || iconOffsetX != null || iconOffsetY != null;
            boolean hasCompleteIcon = iconScreenAbsX != null && iconScreenAbsY != null
                    && iconOffsetX != null && iconOffsetY != null;
            if (hasAnyIcon != hasCompleteIcon) {
                throw new IllegalArgumentException("icon point and offset must be all present or all absent");
            }
            boolean iconBearing = status == Status.OCR_UNAVAILABLE
                    || status == Status.ICON_PRESENT_TIME_UNREADABLE
                    || status == Status.REMAINING_TIME_FOUND;
            if (iconBearing != hasCompleteIcon) {
                throw new IllegalArgumentException(status + " icon presence does not match its icon fields");
            }
            if (remainingMs != null && status != Status.REMAINING_TIME_FOUND) {
                throw new IllegalArgumentException("remaining time is only valid for REMAINING_TIME_FOUND");
            }
            if (status == Status.REMAINING_TIME_FOUND && remainingMs == null) {
                throw new IllegalArgumentException("REMAINING_TIME_FOUND requires a remaining time");
            }
        }

        static IncenseStatusObservation captureUnavailable(String source) {
            return new IncenseStatusObservation(Status.CAPTURE_UNAVAILABLE, null, null, null, null, null, source);
        }

        static IncenseStatusObservation templateAbsent(String source) {
            return new IncenseStatusObservation(Status.TEMPLATE_ABSENT, null, null, null, null, null, source);
        }

        static IncenseStatusObservation mechanicsFailure(String source) {
            return new IncenseStatusObservation(Status.MECHANICS_FAILURE, null, null, null, null, null, source);
        }

        static IncenseStatusObservation ocrUnavailable(int iconX, int iconY, int offsetX, int offsetY, String source) {
            return new IncenseStatusObservation(Status.OCR_UNAVAILABLE, iconX, iconY, offsetX, offsetY, null, source);
        }

        static IncenseStatusObservation iconPresentTimeUnreadable(int iconX, int iconY, int offsetX, int offsetY, String source) {
            return new IncenseStatusObservation(
                    Status.ICON_PRESENT_TIME_UNREADABLE, iconX, iconY, offsetX, offsetY, null, source);
        }

        static IncenseStatusObservation remainingTimeFound(int iconX, int iconY, int offsetX, int offsetY,
                                                           long remainingMs, String source) {
            return new IncenseStatusObservation(
                    Status.REMAINING_TIME_FOUND, iconX, iconY, offsetX, offsetY, remainingMs, source);
        }
    }

    public enum IconPresence {
        PRESENT,
        ABSENT,
        UNKNOWN
    }

    /**
     * Closed immutable icon-presence result from {@link #probeIncenseIconPresence}. The icon point and
     * offset are all present together only for {@link IconPresence#PRESENT} and all absent otherwise.
     * Coordinates are screen-absolute pixels except the offset, which is relative to the status rect
     * origin.
     */
    public record IncenseIconPresenceResult(IconPresence presence,
                                            Integer iconScreenAbsX,
                                            Integer iconScreenAbsY,
                                            Integer iconOffsetX,
                                            Integer iconOffsetY,
                                            String reason) {

        public IncenseIconPresenceResult {
            Objects.requireNonNull(presence, "presence");
            boolean hasAnyIcon = iconScreenAbsX != null || iconScreenAbsY != null
                    || iconOffsetX != null || iconOffsetY != null;
            boolean hasCompleteIcon = iconScreenAbsX != null && iconScreenAbsY != null
                    && iconOffsetX != null && iconOffsetY != null;
            if (hasAnyIcon != hasCompleteIcon) {
                throw new IllegalArgumentException("icon point and offset must be all present or all absent");
            }
            if ((presence == IconPresence.PRESENT) != hasCompleteIcon) {
                throw new IllegalArgumentException("only PRESENT may carry the icon point and offset");
            }
        }

        static IncenseIconPresenceResult present(int iconX, int iconY, int offsetX, int offsetY) {
            return new IncenseIconPresenceResult(IconPresence.PRESENT, iconX, iconY, offsetX, offsetY, "template-hit");
        }

        static IncenseIconPresenceResult absent(String reason) {
            return new IncenseIconPresenceResult(IconPresence.ABSENT, null, null, null, null, reason);
        }

        static IncenseIconPresenceResult unknown(String reason) {
            return new IncenseIconPresenceResult(IconPresence.UNKNOWN, null, null, null, null, reason);
        }
    }
}
