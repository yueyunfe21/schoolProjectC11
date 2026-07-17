package com.bot.dhxy.service.npc;

import com.bot.dhxy.driver.BoundWindowCaptureService;
import com.bot.dhxy.window.model.WindowNativeBinding;
import com.bot.dhxy.window.runtime.WindowNativeBindingRefreshService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * W-696-NPC-YELLOW-TARGET-WHOLE-LOCAL-OBSERVATION-1: closed pure-image local observation for the
 * NPC yellow-target name candidate pass.
 *
 * <p>Byte-behaviour authority is the Git-readable {@code 696a12b0
 * vision/GameTextLineOcrService.java#findYellowTextCandidateResult} and its
 * {@code buildFilteredMask(YELLOW_NPC_TARGET) -> includeNearbyYellowShadow(2) -> toTextMaskImage ->
 * findTextLikeCandidates} pipeline (NOT the Cloud {@code ImageProcessorService}, NOT
 * {@code ImagePreprocessor.washYellowTextToBlackAndWhite}). On one exact caller-supplied window-relative
 * scan region it refreshes the exact HWND geometry, captures the region once from the fresh binding,
 * runs the exact baseline yellow predicate + stall-gold exclusion + radius-2 shadow expansion +
 * connected-component filter + text-line merge + horizontal-gap split + geometry scoring +
 * {@code score desc -> y1 -> x1} sort ({@code limit=3}, {@code minimumScore=25}), and maps the
 * image-local candidates to screen-absolute rect/text-center/click-point using the fresh binding origin
 * plus the supplied scan-region origin, keeping the baseline {@code region.y2()-50} click Y.</p>
 *
 * <p>This class produces zero target verdict, zero OCR, zero input and zero retry: the Cloud
 * {@code NpcClickService} keeps NPC name, OCR hit/strict-hit, candidate consumption timing, region loop,
 * player anchor and click/verify/fallback. It owns no owner/session/ledger/TTL. The borrowed binding is
 * never owned; every owned raw/mask/decoded validation image is flushed exactly once on the success,
 * empty and exception paths.</p>
 */
@Slf4j
@Service
public final class NpcClickYellowTargetLocalObservationMechanics {

    // Baseline GameTextLineOcrService constants, value-for-value.
    private static final int SHADOW_RADIUS = 2;
    private static final int LINE_MERGE_Y_TOLERANCE = 8;
    private static final int COMPONENT_MIN_PIXELS = 3;
    private static final int COMPONENT_MIN_WIDTH = 1;
    private static final int COMPONENT_MIN_HEIGHT = 2;
    private static final int COMPONENT_MAX_WIDTH = 120;
    private static final int COMPONENT_MAX_HEIGHT = 48;
    private static final int COMPONENT_MAX_PIXELS = 1200;
    private static final int CANDIDATE_LIMIT = 3;
    private static final int CANDIDATE_MINIMUM_SCORE = 25;
    private static final int CLICK_Y_OFFSET = 50;

    // Baseline OcrWindowScanService default full-window source region and HUD/chat/shortcut masks.
    private static final int WINDOW_WIDTH = 1024;
    private static final int WINDOW_HEIGHT = 768;
    private static final int[][] DEFAULT_MASKS = {
            {0, 0, 258, 200},
            {0, 0, 1024, 54},
            {768, 58, 1020, 160},
            {4, 735, 706, 768},
            {710, 700, 1024, 768}
    };

    private final BoundWindowCaptureService captureService;
    private final WindowNativeBindingRefreshService bindingRefreshService;

    public NpcClickYellowTargetLocalObservationMechanics(
            BoundWindowCaptureService captureService,
            WindowNativeBindingRefreshService bindingRefreshService) {
        this.captureService = Objects.requireNonNull(captureService, "captureService");
        this.bindingRefreshService = Objects.requireNonNull(bindingRefreshService, "bindingRefreshService");
    }

    /**
     * Observe the yellow-target name candidates in one exact window-relative scan region.
     *
     * @param binding exact borrowed native-window binding; never owned or mutated here
     * @param command the closed caller-supplied window-relative scan region
     * @return closed typed result; only {@code CAPTURED}/{@code NO_YELLOW_CANDIDATE} carry raw/mask
     *         evidence and scan rect, and only {@code CAPTURED} carries a non-empty ordered candidate list
     */
    public Result observe(WindowNativeBinding binding, ScanRegion command) {
        Objects.requireNonNull(command, "command");
        if (binding == null || !binding.hasNativeHandle() || !binding.hasGeometry()) {
            return Result.state(Terminal.BINDING_UNAVAILABLE);
        }
        if (isInterrupted()) {
            return Result.state(Terminal.INTERRUPTED);
        }

        Optional<WindowNativeBinding> refreshed;
        try {
            refreshed = bindingRefreshService.refreshGeometry(binding);
        } catch (RuntimeException e) {
            log.warn("npc yellow target geometry refresh failed: hwnd={} reason={}",
                    binding.getNativeHandle(), e.getMessage(), e);
            return Result.state(Terminal.MECHANICS_FAILED);
        }
        if (refreshed.isEmpty()) {
            return Result.state(Terminal.BINDING_UNAVAILABLE);
        }
        WindowNativeBinding fresh = refreshed.get();
        if (!fresh.hasNativeHandle() || !fresh.hasGeometry()) {
            return Result.state(Terminal.BINDING_UNAVAILABLE);
        }

        int baseX = fresh.getX();
        int baseY = fresh.getY();
        int screenLeft;
        int screenTop;
        int screenRight;
        int screenBottom;
        try {
            screenLeft = Math.addExact(baseX, command.left());
            screenTop = Math.addExact(baseY, command.top());
            screenRight = Math.addExact(baseX, command.right());
            screenBottom = Math.addExact(baseY, command.bottom());
        } catch (ArithmeticException e) {
            return Result.state(Terminal.MECHANICS_FAILED);
        }

        Optional<BoundWindowCaptureService.CaptureResult> captured;
        try {
            captured = captureService.captureRegion(fresh, baseX, baseY, screenLeft, screenTop, screenRight, screenBottom);
        } catch (RuntimeException e) {
            log.warn("npc yellow target capture failed: hwnd={} reason={}", fresh.getNativeHandle(), e.getMessage(), e);
            return Result.state(Terminal.MECHANICS_FAILED);
        }
        if (captured == null || captured.isEmpty() || captured.get().image() == null) {
            return Result.state(Terminal.CAPTURE_UNAVAILABLE);
        }

        BufferedImage raw = captured.get().image();
        BufferedImage source = null;
        boolean sourceIsSeparateCopy = false;
        BufferedImage maskImage = null;
        try {
            // Prepare the OCR source once: the default full-window fallback hides HUD/chat/shortcut
            // unless the caller skips the mask; every other region scans the raw crop directly.
            if (isDefaultMaskedWindowRegion(command) && !command.skipDefaultMask()) {
                source = copyWithDefaultMasks(raw);
                if (source == null) {
                    return Result.state(Terminal.MECHANICS_FAILED);
                }
                sourceIsSeparateCopy = true;
            } else {
                source = raw;
            }
            int width = source.getWidth();
            int height = source.getHeight();
            boolean[][] mask = buildFilteredMask(source);
            mask = includeNearbyYellowShadow(source, mask, SHADOW_RADIUS);
            maskImage = toTextMaskImage(mask);

            ImageEvidence sourceEvidence = ImageEvidence.of(source);
            ImageEvidence maskEvidence = ImageEvidence.of(maskImage);
            int[] scanRect = new int[]{screenLeft, screenTop, screenRight, screenBottom};

            List<TextCandidate> localCandidates =
                    findTextLikeCandidates(mask, maskImage, width, height, CANDIDATE_LIMIT, CANDIDATE_MINIMUM_SCORE);
            List<YellowCandidate> screenCandidates = new ArrayList<>(localCandidates.size());
            for (TextCandidate local : localCandidates) {
                screenCandidates.add(toScreenCandidate(local, baseX, baseY, command));
            }
            List<YellowCandidate> immutable = List.copyOf(screenCandidates);
            if (immutable.isEmpty()) {
                return Result.captured(Terminal.NO_YELLOW_CANDIDATE, List.of(), sourceEvidence, maskEvidence, scanRect);
            }
            return Result.captured(Terminal.CAPTURED, immutable, sourceEvidence, maskEvidence, scanRect);
        } catch (RuntimeException e) {
            log.warn("npc yellow target mechanics failed: hwnd={} reason={}", fresh.getNativeHandle(), e.getMessage(), e);
            return Result.state(Terminal.MECHANICS_FAILED);
        } finally {
            if (maskImage != null) {
                maskImage.flush();
            }
            if (sourceIsSeparateCopy && source != null) {
                source.flush();
            }
            raw.flush();
        }
    }

    private static boolean isDefaultMaskedWindowRegion(ScanRegion region) {
        return region.left() == 0 && region.top() == 0
                && region.right() == WINDOW_WIDTH && region.bottom() == WINDOW_HEIGHT;
    }

    /** Ported baseline OcrWindowScanService copy + default HUD/chat/shortcut masks. */
    private static BufferedImage copyWithDefaultMasks(BufferedImage source) {
        if (source == null) {
            return null;
        }
        BufferedImage copy = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
        boolean handedOff = false;
        try {
            Graphics2D drawGraphics = copy.createGraphics();
            try {
                drawGraphics.drawImage(source, 0, 0, null);
            } finally {
                drawGraphics.dispose();
            }
            Graphics2D maskGraphics = copy.createGraphics();
            try {
                maskGraphics.setColor(Color.WHITE);
                for (int[] mask : DEFAULT_MASKS) {
                    int left = clampValue(Math.min(mask[0], mask[2]), 0, Math.max(0, copy.getWidth()));
                    int right = clampValue(Math.max(mask[0], mask[2]), 0, Math.max(0, copy.getWidth()));
                    int top = clampValue(Math.min(mask[1], mask[3]), 0, Math.max(0, copy.getHeight()));
                    int bottom = clampValue(Math.max(mask[1], mask[3]), 0, Math.max(0, copy.getHeight()));
                    if (right > left && bottom > top) {
                        maskGraphics.fillRect(left, top, right - left, bottom - top);
                    }
                }
            } finally {
                maskGraphics.dispose();
            }
            handedOff = true;
            return copy;
        } finally {
            // On exceptional exit the local copy never reaches the outer owner, so flush it here
            // exactly once; on success ownership transfers to the caller's finally (no double flush).
            if (!handedOff) {
                copy.flush();
            }
        }
    }

    private static int clampValue(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    /** Image-local candidate -> screen-absolute via fresh binding origin + supplied scan-region origin. */
    private YellowCandidate toScreenCandidate(TextCandidate local, int baseX, int baseY, ScanRegion command) {
        int[] r = local.region();
        int absX1 = baseX + command.left() + r[0];
        int absY1 = baseY + command.top() + r[1];
        int absX2 = baseX + command.left() + r[2];
        int absY2 = baseY + command.top() + r[3];
        int textCenterX = (absX1 + absX2) / 2;
        int textCenterY = (absY1 + absY2) / 2;
        int clickX = baseX + command.left() + local.clickX();
        int clickY = baseY + command.top() + local.clickY();
        return new YellowCandidate(absX1, absY1, absX2, absY2,
                textCenterX, textCenterY, clickX, clickY, local.score(), local.reason());
    }

    // ===================== ported 696 GameTextLineOcrService pixel algorithm =====================

    private boolean[][] buildFilteredMask(BufferedImage raw) {
        int width = raw.getWidth();
        int height = raw.getHeight();
        boolean[][] sourceMask = new boolean[height][width];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                sourceMask[y][x] = isNpcTargetYellowTextPixel(raw.getRGB(x, y));
            }
        }
        boolean[][] keptMask = new boolean[height][width];
        boolean[][] visited = new boolean[height][width];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (!sourceMask[y][x] || visited[y][x]) {
                    continue;
                }
                ComponentBox component = collectComponent(sourceMask, visited, x, y);
                if (shouldKeepComponent(component)) {
                    for (Point point : component.points) {
                        keptMask[point.y][point.x] = true;
                    }
                }
            }
        }
        return keptMask;
    }

    private boolean isNpcTargetYellowTextPixel(int rgb) {
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        if (isStallVendorGoldPixel(r, g, b)) {
            return false;
        }
        float[] hsb = Color.RGBtoHSB(r, g, b, null);
        float hueDegrees = hsb[0] * 360.0f;
        return hueDegrees >= 55.0f
                && hueDegrees <= 64.5f
                && r >= 110
                && g >= 110
                && r <= 220
                && g <= 220
                && b >= 45
                && b <= 120
                && Math.abs(r - g) <= 8
                && r > b + 45
                && g > b + 45;
    }

    private boolean isStallVendorGoldPixel(int r, int g, int b) {
        return r >= 198
                && r <= 208
                && g >= 176
                && g <= 186
                && b >= 88
                && b <= 106
                && r - g >= 16
                && r - g <= 30;
    }

    private boolean[][] includeNearbyYellowShadow(BufferedImage raw, boolean[][] baseMask, int radius) {
        int height = baseMask.length;
        int width = baseMask[0].length;
        boolean[][] result = copyMask(baseMask);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (baseMask[y][x]) {
                    continue;
                }
                boolean near = false;
                for (int dy = -radius; dy <= radius && !near; dy++) {
                    for (int dx = -radius; dx <= radius; dx++) {
                        int nx = x + dx;
                        int ny = y + dy;
                        if (nx >= 0 && ny >= 0 && nx < width && ny < height && baseMask[ny][nx]) {
                            near = true;
                            break;
                        }
                    }
                }
                if (!near) {
                    continue;
                }
                int rgb = raw.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;
                if (isYellowShadowPixel(r, g, b)) {
                    result[y][x] = true;
                }
            }
        }
        return result;
    }

    private boolean isYellowShadowPixel(int r, int g, int b) {
        float[] hsb = Color.RGBtoHSB(r, g, b, null);
        float hueDegrees = hsb[0] * 360.0f;
        return hueDegrees >= 25.0f
                && hueDegrees <= 85.0f
                && hsb[1] >= 0.22f
                && hsb[2] >= 0.16f
                && r >= 45
                && g >= 42
                && b <= 150
                && Math.max(r, g) > b + 6;
    }

    private boolean[][] copyMask(boolean[][] mask) {
        boolean[][] copy = new boolean[mask.length][mask[0].length];
        for (int y = 0; y < mask.length; y++) {
            System.arraycopy(mask[y], 0, copy[y], 0, mask[y].length);
        }
        return copy;
    }

    private BufferedImage toTextMaskImage(boolean[][] mask) {
        int height = mask.length;
        int width = mask[0].length;
        BufferedImage output = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_BINARY);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                output.setRGB(x, y, mask[y][x] ? 0x000000 : 0xFFFFFF);
            }
        }
        return output;
    }

    private List<TextCandidate> findTextLikeCandidates(boolean[][] mask, BufferedImage sourceForContext,
                                                       int imageWidth, int imageHeight,
                                                       int candidateLimit, int minimumScore) {
        List<TextLineBox> lines = groupTextLines(mask);
        List<TextCandidate> candidates = new ArrayList<>();
        for (TextLineBox line : lines) {
            for (TextLineBox segment : splitLineByHorizontalGaps(mask, line)) {
                TextCandidate candidate = scoreWashedTextLine(mask, sourceForContext, segment, imageWidth, imageHeight);
                if (candidate.score() >= minimumScore) {
                    candidates.add(candidate);
                }
            }
        }
        candidates.sort(Comparator.comparingInt(TextCandidate::score).reversed()
                .thenComparing(candidate -> candidate.region()[1])
                .thenComparing(candidate -> candidate.region()[0]));
        int keepCount = Math.min(Math.max(1, candidateLimit), candidates.size());
        return List.copyOf(candidates.subList(0, keepCount));
    }

    private List<TextLineBox> groupTextLines(boolean[][] mask) {
        int width = mask[0].length;
        int height = mask.length;
        boolean[][] visited = new boolean[height][width];
        List<ComponentBox> components = new ArrayList<>();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (!mask[y][x] || visited[y][x]) {
                    continue;
                }
                ComponentBox component = collectComponent(mask, visited, x, y);
                if (shouldKeepComponent(component)) {
                    components.add(component);
                }
            }
        }
        components.sort(Comparator.comparingInt(ComponentBox::centerY).thenComparingInt(c -> c.minX));
        List<TextLineBox> lines = new ArrayList<>();
        for (ComponentBox component : components) {
            TextLineBox target = null;
            for (TextLineBox line : lines) {
                if (line.isSameLine(component)) {
                    target = line;
                    break;
                }
            }
            if (target == null) {
                lines.add(TextLineBox.from(component));
            } else {
                target.include(component);
            }
        }
        lines.removeIf(line -> line.pixelCount < 8 || line.width() < 8 || line.height() < 4);
        lines.sort(Comparator.comparingInt(TextLineBox::centerY).thenComparingInt(l -> l.minX));
        return lines;
    }

    private ComponentBox collectComponent(boolean[][] mask, boolean[][] visited, int startX, int startY) {
        int width = mask[0].length;
        int height = mask.length;
        ArrayDeque<Point> queue = new ArrayDeque<>();
        List<Point> points = new ArrayList<>();
        queue.add(new Point(startX, startY));
        visited[startY][startX] = true;
        int minX = startX;
        int minY = startY;
        int maxX = startX;
        int maxY = startY;
        while (!queue.isEmpty()) {
            Point point = queue.removeFirst();
            points.add(point);
            minX = Math.min(minX, point.x);
            minY = Math.min(minY, point.y);
            maxX = Math.max(maxX, point.x);
            maxY = Math.max(maxY, point.y);
            for (int dy = -1; dy <= 1; dy++) {
                for (int dx = -1; dx <= 1; dx++) {
                    if (dx == 0 && dy == 0) {
                        continue;
                    }
                    int nx = point.x + dx;
                    int ny = point.y + dy;
                    if (nx < 0 || ny < 0 || nx >= width || ny >= height || visited[ny][nx] || !mask[ny][nx]) {
                        continue;
                    }
                    visited[ny][nx] = true;
                    queue.addLast(new Point(nx, ny));
                }
            }
        }
        return new ComponentBox(minX, minY, maxX, maxY, points);
    }

    private boolean shouldKeepComponent(ComponentBox component) {
        if (component == null) {
            return false;
        }
        int width = component.width();
        int height = component.height();
        int pixels = component.pixelCount();
        return pixels >= COMPONENT_MIN_PIXELS
                && pixels <= COMPONENT_MAX_PIXELS
                && width >= COMPONENT_MIN_WIDTH
                && height >= COMPONENT_MIN_HEIGHT
                && width <= COMPONENT_MAX_WIDTH
                && height <= COMPONENT_MAX_HEIGHT;
    }

    private List<TextLineBox> splitLineByHorizontalGaps(boolean[][] mask, TextLineBox line) {
        int maxBlankGap = Math.max(16, Math.min(24, line.height() * 2));
        List<TextLineBox> segments = new ArrayList<>();
        int segmentStart = -1;
        int lastInkX = -1;
        int segmentMinY = Integer.MAX_VALUE;
        int segmentMaxY = Integer.MIN_VALUE;
        int segmentPixels = 0;
        for (int x = line.minX; x <= line.maxX; x++) {
            int columnPixels = 0;
            int columnMinY = Integer.MAX_VALUE;
            int columnMaxY = Integer.MIN_VALUE;
            for (int y = line.minY; y <= line.maxY; y++) {
                if (mask[y][x]) {
                    columnPixels++;
                    columnMinY = Math.min(columnMinY, y);
                    columnMaxY = Math.max(columnMaxY, y);
                }
            }
            if (columnPixels > 0) {
                if (segmentStart >= 0 && lastInkX >= 0 && x - lastInkX > maxBlankGap) {
                    addSplitSegment(segments, segmentStart, segmentMinY, lastInkX, segmentMaxY, segmentPixels);
                    segmentStart = -1;
                    segmentMinY = Integer.MAX_VALUE;
                    segmentMaxY = Integer.MIN_VALUE;
                    segmentPixels = 0;
                }
                if (segmentStart < 0) {
                    segmentStart = x;
                }
                lastInkX = x;
                segmentMinY = Math.min(segmentMinY, columnMinY);
                segmentMaxY = Math.max(segmentMaxY, columnMaxY);
                segmentPixels += columnPixels;
            }
        }
        if (segmentStart >= 0) {
            addSplitSegment(segments, segmentStart, segmentMinY, lastInkX, segmentMaxY, segmentPixels);
        }
        return segments.isEmpty() ? List.of(line) : segments;
    }

    private void addSplitSegment(List<TextLineBox> segments, int minX, int minY, int maxX, int maxY, int pixelCount) {
        if (pixelCount < 8 || maxX - minX + 1 < 8 || maxY - minY + 1 < 4) {
            return;
        }
        segments.add(new TextLineBox(minX, minY, maxX, maxY, pixelCount));
    }

    private TextCandidate scoreWashedTextLine(boolean[][] mask, BufferedImage source, TextLineBox line,
                                              int imageWidth, int imageHeight) {
        int[] region = expandRect(line.minX, line.minY, line.maxX + 1, line.maxY + 1, 4, 4, imageWidth, imageHeight);
        int width = region[2] - region[0];
        int height = region[3] - region[1];
        int pixels = countForeground(mask, region);
        int componentCount = countComponents(mask, region);
        double density = width <= 0 || height <= 0 ? 0.0 : (double) pixels / (double) (width * height);
        int longRowCount = countLongRuns(mask, region, true);
        int longColumnCount = countLongRuns(mask, region, false);
        int[] contextRegion = expandRect(region[0], region[1], region[2], region[3], 18, 18, imageWidth, imageHeight);
        int contextLongRowCount = Math.max(0, countLongRunsInWashedImage(source, contextRegion, true) - longRowCount);
        int contextLongColumnCount = Math.max(0, countLongRunsInWashedImage(source, contextRegion, false) - longColumnCount);
        int borderPenalty = longRowCount * 18 + longColumnCount * 14;
        int contextFramePenalty = contextLongRowCount * 35 + contextLongColumnCount * 18;
        int densityPenalty = density > 0.42 ? (int) Math.round((density - 0.42) * 160.0) : 0;
        int sizePenalty = height > 55 ? (height - 55) * 2 : 0;
        int sparsePenalty = density < 0.012 ? 22 : 0;
        int verticalFragmentPenalty = height > 24 && height > width * 1.25 ? 90 : 0;
        int tinyFragmentPenalty = width < 38 && (componentCount < 4 || pixels < 90) ? 70 : 0;
        int weakTextPenalty = pixels < 120 && componentCount < 5 && density < 0.08 ? 110 : 0;
        int score = (int) Math.round(width * 0.32 + Math.min(height, 40) * 1.8
                + componentCount * 7.0 + Math.min(pixels, 260) * 0.16)
                - borderPenalty - contextFramePenalty - densityPenalty - sizePenalty - sparsePenalty
                - verticalFragmentPenalty - tinyFragmentPenalty - weakTextPenalty;
        if (width < 24 || height < 6 || pixels < 12 || componentCount < 2) {
            score -= 35;
        }
        int clickX = (region[0] + region[2]) / 2;
        int clickY = region[3] - CLICK_Y_OFFSET;
        String reason = "components=" + componentCount
                + ",pixels=" + pixels
                + ",density=" + String.format(java.util.Locale.ROOT, "%.3f", density)
                + ",longRows=" + longRowCount
                + ",longCols=" + longColumnCount
                + ",contextLongRows=" + contextLongRowCount
                + ",contextLongCols=" + contextLongColumnCount
                + ",verticalPenalty=" + verticalFragmentPenalty
                + ",tinyPenalty=" + tinyFragmentPenalty
                + ",weakPenalty=" + weakTextPenalty;
        return new TextCandidate(region, clickX, clickY, score, reason);
    }

    private int[] expandRect(int x1, int y1, int x2, int y2, int padX, int padY, int imageWidth, int imageHeight) {
        int left = Math.max(0, x1 - padX);
        int top = Math.max(0, y1 - padY);
        int right = Math.min(imageWidth, x2 + padX);
        int bottom = Math.min(imageHeight, y2 + padY);
        return new int[]{left, top, right, bottom};
    }

    private int countForeground(boolean[][] mask, int[] region) {
        int count = 0;
        for (int y = region[1]; y < region[3]; y++) {
            for (int x = region[0]; x < region[2]; x++) {
                if (mask[y][x]) {
                    count++;
                }
            }
        }
        return count;
    }

    private int countComponents(boolean[][] mask, int[] region) {
        int regionHeight = region[3] - region[1];
        int regionWidth = region[2] - region[0];
        boolean[][] visited = new boolean[regionHeight][regionWidth];
        int count = 0;
        for (int y = region[1]; y < region[3]; y++) {
            for (int x = region[0]; x < region[2]; x++) {
                int localY = y - region[1];
                int localX = x - region[0];
                if (!mask[y][x] || visited[localY][localX]) {
                    continue;
                }
                floodLocal(mask, visited, region, x, y);
                count++;
            }
        }
        return count;
    }

    private void floodLocal(boolean[][] mask, boolean[][] visited, int[] region, int startX, int startY) {
        ArrayDeque<Point> queue = new ArrayDeque<>();
        queue.add(new Point(startX, startY));
        visited[startY - region[1]][startX - region[0]] = true;
        while (!queue.isEmpty()) {
            Point point = queue.removeFirst();
            for (int dy = -1; dy <= 1; dy++) {
                for (int dx = -1; dx <= 1; dx++) {
                    if (dx == 0 && dy == 0) {
                        continue;
                    }
                    int nx = point.x + dx;
                    int ny = point.y + dy;
                    if (nx < region[0] || ny < region[1] || nx >= region[2] || ny >= region[3]) {
                        continue;
                    }
                    int localX = nx - region[0];
                    int localY = ny - region[1];
                    if (visited[localY][localX] || !mask[ny][nx]) {
                        continue;
                    }
                    visited[localY][localX] = true;
                    queue.addLast(new Point(nx, ny));
                }
            }
        }
    }

    private int countLongRuns(boolean[][] mask, int[] region, boolean horizontal) {
        int longRuns = 0;
        int outerStart = horizontal ? region[1] : region[0];
        int outerEnd = horizontal ? region[3] : region[2];
        int innerStart = horizontal ? region[0] : region[1];
        int innerEnd = horizontal ? region[2] : region[3];
        int threshold = Math.max(12, (int) Math.round((innerEnd - innerStart) * 0.42));
        for (int outer = outerStart; outer < outerEnd; outer++) {
            int bestRun = 0;
            int currentRun = 0;
            for (int inner = innerStart; inner < innerEnd; inner++) {
                boolean black = horizontal ? mask[outer][inner] : mask[inner][outer];
                if (black) {
                    currentRun++;
                    bestRun = Math.max(bestRun, currentRun);
                } else {
                    currentRun = 0;
                }
            }
            if (bestRun >= threshold) {
                longRuns++;
            }
        }
        return longRuns;
    }

    private int countLongRunsInWashedImage(BufferedImage image, int[] region, boolean horizontal) {
        int longRuns = 0;
        int outerStart = horizontal ? region[1] : region[0];
        int outerEnd = horizontal ? region[3] : region[2];
        int innerStart = horizontal ? region[0] : region[1];
        int innerEnd = horizontal ? region[2] : region[3];
        int threshold = Math.max(18, (int) Math.round((innerEnd - innerStart) * 0.50));
        for (int outer = outerStart; outer < outerEnd; outer++) {
            int bestRun = 0;
            int currentRun = 0;
            for (int inner = innerStart; inner < innerEnd; inner++) {
                boolean black = horizontal ? isBlackWashedPixel(image.getRGB(inner, outer))
                        : isBlackWashedPixel(image.getRGB(outer, inner));
                if (black) {
                    currentRun++;
                    bestRun = Math.max(bestRun, currentRun);
                } else {
                    currentRun = 0;
                }
            }
            if (bestRun >= threshold) {
                longRuns++;
            }
        }
        return longRuns;
    }

    private boolean isBlackWashedPixel(int rgb) {
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        int luminance = (r * 30 + g * 59 + b * 11) / 100;
        return luminance < 150;
    }

    private static boolean isInterrupted() {
        return Thread.currentThread().isInterrupted();
    }

    private static byte[] encodePng(BufferedImage image) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            if (!ImageIO.write(image, "png", out)) {
                throw new IllegalStateException("no PNG writer available");
            }
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("failed to encode PNG evidence", e);
        }
    }

    private static String sha256Hex(byte[] bytes) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(bytes);
            StringBuilder hex = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                hex.append(Character.forDigit((b >> 4) & 0xF, 16));
                hex.append(Character.forDigit(b & 0xF, 16));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    // ===================== closed immutable nested types =====================

    /** Six closed terminals; only CAPTURED/NO_YELLOW_CANDIDATE carry evidence + rect. */
    public enum Terminal {
        CAPTURED,
        NO_YELLOW_CANDIDATE,
        CAPTURE_UNAVAILABLE,
        BINDING_UNAVAILABLE,
        INTERRUPTED,
        MECHANICS_FAILED
    }

    /**
     * Closed caller-supplied window-relative scan region (positive-area box) plus the caller's
     * default-mask skip flag. When the region is the default full-window fallback and the flag is
     * false, the baseline HUD/chat/shortcut masks are applied once before the yellow scan.
     */
    public record ScanRegion(int left, int top, int right, int bottom, boolean skipDefaultMask) {
        public ScanRegion {
            if (right <= left || bottom <= top) {
                throw new IllegalArgumentException("scan region must be a positive-area window-relative box");
            }
        }
    }

    /** One closed screen-absolute yellow-text candidate; pure geometry, no OCR text, no verdict. */
    public record YellowCandidate(
            int rectLeft, int rectTop, int rectRight, int rectBottom,
            int textCenterX, int textCenterY,
            int clickX, int clickY,
            int score, String reason) {
    }

    /** Raw or mask PNG evidence, structurally validated from the actual bytes. */
    public record ImageEvidence(byte[] pngBytes, String sha256, int width, int height) {
        public ImageEvidence {
            Objects.requireNonNull(pngBytes, "pngBytes");
            Objects.requireNonNull(sha256, "sha256");
            pngBytes = pngBytes.clone();
            if (pngBytes.length == 0) {
                throw new IllegalArgumentException("image evidence requires PNG bytes");
            }
            BufferedImage decoded;
            try {
                decoded = ImageIO.read(new ByteArrayInputStream(pngBytes));
            } catch (IOException e) {
                throw new IllegalArgumentException("image evidence bytes are not decodable PNG", e);
            }
            if (decoded == null) {
                throw new IllegalArgumentException("image evidence bytes are not a PNG image");
            }
            try {
                if (decoded.getWidth() != width || decoded.getHeight() != height) {
                    throw new IllegalArgumentException("image evidence dimensions do not match the PNG bytes");
                }
                if (!sha256Hex(pngBytes).equalsIgnoreCase(sha256)) {
                    throw new IllegalArgumentException("image evidence SHA-256 does not match the PNG bytes");
                }
            } finally {
                decoded.flush();
            }
        }

        private static ImageEvidence of(BufferedImage image) {
            byte[] bytes = encodePng(image);
            return new ImageEvidence(bytes, sha256Hex(bytes), image.getWidth(), image.getHeight());
        }

        @Override
        public byte[] pngBytes() {
            return pngBytes.clone();
        }
    }

    /**
     * Closed result. {@code CAPTURED} carries an immutable ordered candidate list plus raw/mask
     * evidence and the screen-absolute scan rect; {@code NO_YELLOW_CANDIDATE} carries the same evidence
     * and rect with an empty candidate list; every other terminal carries none of them.
     */
    public record Result(
            Terminal terminal,
            List<YellowCandidate> candidates,
            ImageEvidence rawEvidence,
            ImageEvidence maskEvidence,
            int[] scanRect) {

        public Result {
            Objects.requireNonNull(terminal, "terminal");
            candidates = candidates == null ? List.of() : List.copyOf(candidates);
            scanRect = scanRect == null ? null : scanRect.clone();
            boolean carriesEvidence = terminal == Terminal.CAPTURED || terminal == Terminal.NO_YELLOW_CANDIDATE;
            if (carriesEvidence) {
                Objects.requireNonNull(rawEvidence, "rawEvidence");
                Objects.requireNonNull(maskEvidence, "maskEvidence");
                if (scanRect == null || scanRect.length != 4) {
                    throw new IllegalArgumentException("evidence-carrying result requires a 4-element scan rect");
                }
                int spanWidth = scanRect[2] - scanRect[0];
                int spanHeight = scanRect[3] - scanRect[1];
                if (spanWidth <= 0 || spanHeight <= 0) {
                    throw new IllegalArgumentException("scan rect must be a positive-area span");
                }
                if (rawEvidence.width() != maskEvidence.width() || rawEvidence.height() != maskEvidence.height()) {
                    throw new IllegalArgumentException("raw and mask evidence dimensions must be identical");
                }
                if (rawEvidence.width() != spanWidth || rawEvidence.height() != spanHeight) {
                    throw new IllegalArgumentException("evidence dimensions must equal the scan rect span");
                }
                if (terminal == Terminal.CAPTURED && candidates.isEmpty()) {
                    throw new IllegalArgumentException("CAPTURED must carry a non-empty candidate list");
                }
                if (terminal == Terminal.NO_YELLOW_CANDIDATE && !candidates.isEmpty()) {
                    throw new IllegalArgumentException("NO_YELLOW_CANDIDATE must not carry candidates");
                }
            } else if (rawEvidence != null || maskEvidence != null || scanRect != null || !candidates.isEmpty()) {
                throw new IllegalArgumentException("non-evidence terminal must not carry candidates, evidence, or rect");
            }
        }

        private static Result state(Terminal terminal) {
            return new Result(terminal, List.of(), null, null, null);
        }

        private static Result captured(Terminal terminal, List<YellowCandidate> candidates,
                                       ImageEvidence rawEvidence, ImageEvidence maskEvidence, int[] scanRect) {
            return new Result(terminal, candidates, rawEvidence, maskEvidence, scanRect);
        }

        @Override
        public int[] scanRect() {
            return scanRect == null ? null : scanRect.clone();
        }
    }

    /** Ported baseline connected-component box (8-connectivity). */
    private static final class ComponentBox {
        private final int minX;
        private final int minY;
        private final int maxX;
        private final int maxY;
        private final List<Point> points;

        private ComponentBox(int minX, int minY, int maxX, int maxY, List<Point> points) {
            this.minX = minX;
            this.minY = minY;
            this.maxX = maxX;
            this.maxY = maxY;
            this.points = points;
        }

        int centerY() {
            return (minY + maxY) / 2;
        }

        int width() {
            return maxX - minX + 1;
        }

        int height() {
            return maxY - minY + 1;
        }

        int pixelCount() {
            return points == null ? 0 : points.size();
        }
    }

    /** Ported baseline text-line box with same-line merge tolerance. */
    private static final class TextLineBox {
        private int minX;
        private int minY;
        private int maxX;
        private int maxY;
        private int pixelCount;

        private TextLineBox(int minX, int minY, int maxX, int maxY, int pixelCount) {
            this.minX = minX;
            this.minY = minY;
            this.maxX = maxX;
            this.maxY = maxY;
            this.pixelCount = pixelCount;
        }

        static TextLineBox from(ComponentBox component) {
            return new TextLineBox(component.minX, component.minY, component.maxX, component.maxY,
                    component.pixelCount());
        }

        boolean isSameLine(ComponentBox component) {
            int centerDelta = Math.abs(centerY() - component.centerY());
            boolean yOverlaps = component.maxY + LINE_MERGE_Y_TOLERANCE >= minY
                    && component.minY - LINE_MERGE_Y_TOLERANCE <= maxY;
            return yOverlaps || centerDelta <= Math.max(LINE_MERGE_Y_TOLERANCE, height() / 2);
        }

        void include(ComponentBox component) {
            minX = Math.min(minX, component.minX);
            minY = Math.min(minY, component.minY);
            maxX = Math.max(maxX, component.maxX);
            maxY = Math.max(maxY, component.maxY);
            pixelCount += component.pixelCount();
        }

        int centerY() {
            return (minY + maxY) / 2;
        }

        int width() {
            return maxX - minX + 1;
        }

        int height() {
            return maxY - minY + 1;
        }
    }

    /** Ported baseline image-local candidate; region is {@code [x1,y1,x2,y2]} in scan-crop pixels. */
    private record TextCandidate(int[] region, int clickX, int clickY, int score, String reason) {
    }
}
