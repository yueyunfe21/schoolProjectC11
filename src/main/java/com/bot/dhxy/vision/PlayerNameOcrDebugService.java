package com.bot.dhxy.vision;



import com.bot.dhxy.model.ocr.LocationInfo;
import com.bot.dhxy.model.ocr.OcrLineResult;
import com.bot.dhxy.model.ocr.OcrWordResult;
import com.bot.dhxy.model.ocr.PlayerAnchorMatch;
import com.bot.dhxy.model.ocr.RecordResult;
import com.bot.dhxy.core.TextRecognizer;
import com.bot.dhxy.driver.BoundWindowCaptureService;
import com.bot.dhxy.input.InputSequences;
import com.bot.dhxy.input.action.InputAction;
import com.bot.dhxy.window.execution.WindowTaskSnapshot;
import com.bot.dhxy.window.execution.MultiWindowTaskManager;
import com.bot.dhxy.window.interaction.WindowFocusService;
import com.bot.dhxy.window.model.WindowNativeBinding;
import com.bot.dhxy.window.runtime.WindowRuntimeContext;
import com.bot.dhxy.window.runtime.WindowTaskContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlayerNameOcrDebugService {

    private static final int SCAN_WIDTH = 760;
    private static final int SCAN_HEIGHT = 320;
    private static final int FOCUS_SETTLE_MS = 500;
    private static final int WORD_SUMMARY_LIMIT = 12;
    private static final int ENHANCED_OCR_SCALE = 4;
    private static final int ENHANCED_OCR_DILATE_RADIUS = 0;
    private static final int LINE_CROP_MARGIN = 8;
    private static final int LINE_PACK_GAP = 18;
    private static final int LINE_MERGE_Y_TOLERANCE = 8;
    private static final int COMPONENT_MIN_PIXELS = 3;
    private static final int COMPONENT_MIN_WIDTH = 1;
    private static final int COMPONENT_MIN_HEIGHT = 2;
    private static final int COMPONENT_MAX_WIDTH = 120;
    private static final int COMPONENT_MAX_HEIGHT = 48;
    private static final int COMPONENT_MAX_PIXELS = 1200;
    private static final Path DEBUG_DIR = Path.of("images", "temp", "player_name_ocr");

    private final BoundWindowCaptureService boundWindowCaptureService;
    private final TextRecognizer textRecognizer;
    private final LocationVisionService locationVisionService;
    private final WindowFocusService windowFocusService;
    private final MultiWindowTaskManager multiWindowTaskManager;
    private final WindowTaskContextHolder windowTaskContextHolder;
    private final InputSequences inputSequences;
    private final OcrRoiMemoryService ocrRoiMemoryService;
    private final GameTextLineOcrService gameTextLineOcrService;

    public DebugResult debugLocalNameOcr(WindowTaskSnapshot snapshot, String expectedName) {
        String windowId = snapshot == null ? null : snapshot.getWindowId();
        WindowNativeBinding binding = snapshot == null ? null : snapshot.getNativeBinding();
        if (binding == null || !binding.hasNativeHandle()) {
            return DebugResult.failed(windowId, expectedName, "window native binding is empty");
        }
        if (expectedName == null || expectedName.isBlank() || "-".equals(expectedName.trim())) {
            return DebugResult.failed(windowId, expectedName, "expected player name is empty");
        }

        boolean focusRequested = windowFocusService.focus(binding);
        log.info("[player-name-ocr-debug] focus-before-capture windowId={} hwnd={} requested={} waitMs={}",
                windowId, binding.getNativeHandle(), focusRequested, FOCUS_SETTLE_MS);
        if (!sleepQuietly(FOCUS_SETTLE_MS)) {
            return DebugResult.failed(windowId, expectedName, "interrupted while waiting after focus");
        }

        Optional<BoundWindowCaptureService.CaptureResult> capture = boundWindowCaptureService.captureWindow(binding);
        if (capture.isEmpty()) {
            return DebugResult.failed(windowId, expectedName, "window capture failed");
        }

        BufferedImage windowImage = capture.get().image();
        try {
            ScanRect rect = centeredScanRect(windowImage.getWidth(), windowImage.getHeight());
            Path outputDir = DEBUG_DIR.resolve(safeFileName(windowId == null ? "unknown" : windowId));
            Files.createDirectories(outputDir);
            clearOldDebugPngs(outputDir);
            Path purpleEnhancedPath = outputDir.resolve("latest_purple_line_enhanced.png").normalize();
            Path yellowEnhancedPath = outputDir.resolve("latest_yellow_line_enhanced.png").normalize();

            BufferedImage raw = copyImage(windowImage.getSubimage(rect.x(), rect.y(), rect.width(), rect.height()));
            try {
                OcrVariant purpleEnhanced = toOcrVariant(
                        gameTextLineOcrService.scanPurpleLines(raw, purpleEnhancedPath));
                OcrVariant yellowEnhanced = toOcrVariant(
                        gameTextLineOcrService.scanYellowLines(raw, yellowEnhancedPath));
                List<OcrWordResult> mappedPurpleWords =
                        mapRawWordsToWindow(purpleEnhanced.words(), rect);
                PlayerAnchorMatch segmentedMatch =
                        locationVisionService.extractPlayerAnchorMatch(mappedPurpleWords, expectedName, 0, 0, 0);
                PlayerAnchorMatch selectedMatch = segmentedMatch;
                String anchorSource = segmentedMatch != null ? "SEGMENTED_CENTER" : "NONE";
                Point anchorRel = selectedMatch == null ? null : selectedMatch.anchor();
                Point anchorAbs = anchorRel == null
                        ? null
                        : new Point(binding.getX() + anchorRel.x, binding.getY() + anchorRel.y);
                LocationInfo locationInfo = selectedMatch == null
                        ? null
                        : scanCurrentLocationWithContext(snapshot);
                RecordResult visionRecord = selectedMatch == null
                        ? null
                        : ocrRoiMemoryService.recordPlayerAnchorSuccess(
                        memoryKeyForPlayerName(expectedName),
                        anchorSource,
                        selectedMatch,
                        locationInfo,
                        windowImage.getWidth(),
                        windowImage.getHeight(),
                        "local-only",
                        "segmented-purple-line",
                        purpleEnhanced.path(),
                        yellowEnhanced.path(),
                        "minimap-ocr");
                log.info("[player-name-ocr-debug] name-anchor-result windowId={} expectedName={} provider=local "
                                + "captureProvider={} windowSize={}x{} scanRect=({}, {}) {}x{} "
                                + "purpleEnhanced={} yellowEnhanced={} anchorSource={} anchorRel={} anchorAbs={} "
                                + "location={} segmentedMatch={} visionMemory={}",
                        windowId, expectedName, capture.get().provider(),
                        windowImage.getWidth(), windowImage.getHeight(),
                        rect.x(), rect.y(), rect.width(), rect.height(),
                        purpleEnhanced.toDetailText(), yellowEnhanced.toDetailText(),
                        anchorSource, pointText(anchorRel), pointText(anchorAbs),
                        locationText(locationInfo), anchorDetail(segmentedMatch), visionRecordText(visionRecord));

                if (anchorRel == null) {
                    return DebugResult.of(false, windowId, expectedName, null, null,
                            null, null, null, rect,
                            0, "-", 0, 0, "-", 0, 0, 0, "-",
                            purpleEnhanced, yellowEnhanced,
                            null, null, false,
                            "OCR did not locate expected name fragment"
                                    + " | segmented=" + anchorDetail(segmentedMatch));
                }

                boolean mouseMoved = submitMoveMouseWithWindowContext(snapshot, anchorAbs.x, anchorAbs.y);
                return DebugResult.of(true, windowId, expectedName, null, null,
                        null, null, null, rect,
                        0, "-", 0, 0, "-", 0, 0, 0, "-",
                        purpleEnhanced, yellowEnhanced,
                        anchorRel, anchorAbs, mouseMoved,
                        "OCR located player name anchor"
                                + " | anchorSource=" + anchorSource
                                + " | selected=" + anchorDetail(selectedMatch)
                                + " | segmented=" + anchorDetail(segmentedMatch)
                                + " | location=" + locationText(locationInfo)
                                + " | visionMemory=" + visionRecordText(visionRecord));
            } finally {
                raw.flush();
            }
        } catch (Exception e) {
            log.warn("[player-name-ocr-debug] failed: windowId={} expectedName={} reason={}",
                    windowId, expectedName, e.getMessage(), e);
            return DebugResult.failed(windowId, expectedName, e.getMessage());
        } finally {
            windowImage.flush();
        }
    }

    private OcrVariant toOcrVariant(OcrLineResult result) {
        if (result == null) {
            return OcrVariant.empty();
        }
        return OcrVariant.of(result.path(), result.blackPixelCount(), result.wordCount(),
                result.wordsSummary(), result.words());
    }

    private void clearOldDebugPngs(Path outputDir) {
        if (outputDir == null || !Files.isDirectory(outputDir)) {
            return;
        }
        try (var stream = Files.list(outputDir)) {
            stream.filter(path -> path != null
                            && Files.isRegularFile(path)
                            && path.getFileName().toString().toLowerCase().endsWith(".png"))
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (Exception e) {
                            log.warn("[player-name-ocr-debug] delete old debug image failed: path={} reason={}",
                                    path, e.getMessage());
                        }
                    });
        } catch (Exception e) {
            log.warn("[player-name-ocr-debug] clear old debug images failed: dir={} reason={}",
                    outputDir, e.getMessage());
        }
    }

    private OcrVariant buildSegmentedEnhancedVariant(BufferedImage raw,
                                                     Path outputPath,
                                                     TextColorMode mode) throws Exception {
        boolean[][] filteredMask = buildFilteredMask(raw, mode);
        List<TextLineBox> lines = groupTextLines(filteredMask);
        List<PackedLineBox> packedLines = new ArrayList<>();
        int blackPixelCount = writePackedLineMask(filteredMask, lines, packedLines, outputPath);
        List<OcrWordResult> packedWords = Files.exists(outputPath)
                ? textRecognizer.getAllTextResultsLocalOnly(outputPath.toString())
                : List.of();
        List<OcrWordResult> rawWords = mapPackedWordsToRaw(packedWords, packedLines);
        return OcrVariant.of(outputPath.toString(), blackPixelCount,
                rawWords.size(), summarizeWords(rawWords), rawWords);
    }

    private boolean[][] buildFilteredMask(BufferedImage raw, TextColorMode mode) {
        int width = raw.getWidth();
        int height = raw.getHeight();
        boolean[][] sourceMask = new boolean[height][width];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                sourceMask[y][x] = isTargetTextPixel(raw.getRGB(x, y), mode);
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
                    for (Point point : component.points()) {
                        keptMask[point.y][point.x] = true;
                    }
                }
            }
        }
        return keptMask;
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
        components.sort((left, right) -> {
            int y = Integer.compare(left.centerY(), right.centerY());
            return y != 0 ? y : Integer.compare(left.minX(), right.minX());
        });

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
        lines.removeIf(line -> line.pixelCount() < 8 || line.width() < 8 || line.height() < 4);
        lines.sort((left, right) -> {
            int y = Integer.compare(left.centerY(), right.centerY());
            return y != 0 ? y : Integer.compare(left.minX(), right.minX());
        });
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

    private int writePackedLineMask(boolean[][] mask,
                                    List<TextLineBox> lines,
                                    List<PackedLineBox> packedLines,
                                    Path outputPath) throws Exception {
        int height = mask.length;
        int width = mask[0].length;
        if (lines == null || lines.isEmpty()) {
            BufferedImage blank = new BufferedImage(1, 1, BufferedImage.TYPE_BYTE_BINARY);
            try {
                blank.setRGB(0, 0, 0xFFFFFF);
                Path parent = outputPath.getParent();
                if (parent != null) {
                    Files.createDirectories(parent);
                }
                ImageIO.write(blank, "png", outputPath.toFile());
            } finally {
                blank.flush();
            }
            return 0;
        }

        int outputWidth = 1;
        int outputHeight = 0;
        for (TextLineBox line : lines) {
            int sourceX = clamp(line.minX() - LINE_CROP_MARGIN, 0, width - 1);
            int sourceY = clamp(line.minY() - LINE_CROP_MARGIN, 0, height - 1);
            int sourceRight = clamp(line.maxX() + LINE_CROP_MARGIN, 0, width - 1);
            int sourceBottom = clamp(line.maxY() + LINE_CROP_MARGIN, 0, height - 1);
            int lineWidth = sourceRight - sourceX + 1;
            int lineHeight = sourceBottom - sourceY + 1;
            int packedWidth = lineWidth * ENHANCED_OCR_SCALE;
            int packedHeight = lineHeight * ENHANCED_OCR_SCALE;
            outputWidth = Math.max(outputWidth, packedWidth);
            packedLines.add(new PackedLineBox(sourceX, sourceY, lineWidth, lineHeight,
                    0, outputHeight, packedWidth, packedHeight));
            outputHeight += packedHeight + LINE_PACK_GAP;
        }
        outputHeight = Math.max(1, outputHeight - LINE_PACK_GAP);

        BufferedImage output = new BufferedImage(outputWidth, outputHeight, BufferedImage.TYPE_BYTE_BINARY);
        int blackPixelCount = 0;
        try {
            for (int y = 0; y < output.getHeight(); y++) {
                for (int x = 0; x < output.getWidth(); x++) {
                    output.setRGB(x, y, 0xFFFFFF);
                }
            }

            for (PackedLineBox line : packedLines) {
                for (int y = 0; y < line.packedHeight(); y++) {
                    for (int x = 0; x < line.packedWidth(); x++) {
                        int sourceX = line.sourceX() + x / ENHANCED_OCR_SCALE;
                        int sourceY = line.sourceY() + y / ENHANCED_OCR_SCALE;
                        boolean black = hasKeptNeighbor(mask, sourceX, sourceY, ENHANCED_OCR_DILATE_RADIUS);
                        if (black) {
                            blackPixelCount++;
                        }
                        output.setRGB(line.packedX() + x, line.packedY() + y, black ? 0x000000 : 0xFFFFFF);
                    }
                }
            }
            Path parent = outputPath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            ImageIO.write(output, "png", outputPath.toFile());
            return blackPixelCount;
        } finally {
            output.flush();
        }
    }

    private List<OcrWordResult> mapPackedWordsToRaw(List<OcrWordResult> words,
                                                                   List<PackedLineBox> packedLines) {
        if (words == null || words.isEmpty() || packedLines == null || packedLines.isEmpty()) {
            return List.of();
        }
        List<OcrWordResult> mapped = new ArrayList<>();
        for (OcrWordResult word : words) {
            if (word == null) {
                continue;
            }
            PackedLineBox line = findPackedLine(word, packedLines);
            if (line == null) {
                continue;
            }
            int rawLeft = line.sourceX() + Math.round((float) (word.getLeft() - line.packedX()) / ENHANCED_OCR_SCALE);
            int rawTop = line.sourceY() + Math.round((float) (word.getTop() - line.packedY()) / ENHANCED_OCR_SCALE);
            int rawWidth = Math.max(1, Math.round((float) word.getWidth() / ENHANCED_OCR_SCALE));
            int rawHeight = Math.max(1, Math.round((float) word.getHeight() / ENHANCED_OCR_SCALE));
            mapped.add(new OcrWordResult(
                    word.getText(), rawLeft, rawTop, rawWidth, rawHeight));
        }
        return mapped;
    }

    private PackedLineBox findPackedLine(OcrWordResult word, List<PackedLineBox> packedLines) {
        int centerY = word.getY();
        int centerX = word.getX();
        for (PackedLineBox line : packedLines) {
            boolean insideY = centerY >= line.packedY() && centerY <= line.packedY() + line.packedHeight();
            boolean insideX = centerX >= line.packedX() && centerX <= line.packedX() + line.packedWidth();
            if (insideY && insideX) {
                return line;
            }
        }
        return null;
    }

    private boolean hasKeptNeighbor(boolean[][] mask, int x, int y, int radius) {
        int width = mask[0].length;
        int height = mask.length;
        for (int dy = -radius; dy <= radius; dy++) {
            for (int dx = -radius; dx <= radius; dx++) {
                int nx = x + dx;
                int ny = y + dy;
                if (nx >= 0 && ny >= 0 && nx < width && ny < height && mask[ny][nx]) {
                    return true;
                }
            }
        }
        return false;
    }

    private List<OcrWordResult> mapRawWordsToWindow(List<OcrWordResult> words,
                                                                   ScanRect scanRect) {
        if (words == null || words.isEmpty() || scanRect == null) {
            return List.of();
        }
        List<OcrWordResult> mapped = new ArrayList<>();
        for (OcrWordResult word : words) {
            if (word == null) {
                continue;
            }
            int left = scanRect.x() + word.getLeft();
            int top = scanRect.y() + word.getTop();
            int width = Math.max(1, word.getWidth());
            int height = Math.max(1, word.getHeight());
            mapped.add(new OcrWordResult(word.getText(), left, top, width, height));
        }
        return mapped;
    }

    private boolean isTargetTextPixel(int rgb, TextColorMode mode) {
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        if (mode == TextColorMode.YELLOW) {
            return isNpcYellowTextPixel(r, g, b);
        }
        float[] hsb = Color.RGBtoHSB(r, g, b, null);
        float hueDegrees = hsb[0] * 360.0f;
        return hueDegrees >= 240.0f
                && hueDegrees <= 320.0f
                && hsb[1] >= 0.20f
                && hsb[2] >= 0.18f
                && b >= 80
                && r >= 60
                && g <= 170;
    }

    private boolean isNpcYellowTextPixel(int r, int g, int b) {
        boolean strictYellow = r >= 150
                && g >= 110
                && b <= 110
                && Math.abs(r - g) <= 110
                && r > b + 60
                && g > b + 40;
        if (strictYellow) {
            return true;
        }

        float[] hsb = Color.RGBtoHSB(r, g, b, null);
        float hueDegrees = hsb[0] * 360.0f;
        return hueDegrees >= 28.0f
                && hueDegrees <= 68.0f
                && hsb[1] >= 0.38f
                && hsb[2] >= 0.30f
                && r >= 95
                && g >= 75
                && b <= 135
                && r > b + 28
                && g > b + 16;
    }

    private String anchorDetail(PlayerAnchorMatch match) {
        return match == null ? "-" : match.toDetailText();
    }

    private LocationInfo scanCurrentLocationWithContext(WindowTaskSnapshot snapshot) {
        String windowId = snapshot == null ? null : snapshot.getWindowId();
        if (windowId == null || windowId.isBlank()) {
            return locationVisionService.scanCurrentLocation();
        }
        Optional<WindowRuntimeContext> context = multiWindowTaskManager.getRunner(windowId)
                .map(runner -> runner.getWindowContext());
        if (context.isEmpty()) {
            return locationVisionService.scanCurrentLocation();
        }
        return windowTaskContextHolder.callWith(context.get(), locationVisionService::scanCurrentLocation);
    }

    private String memoryKeyForPlayerName(String expectedName) {
        String normalized = expectedName == null ? "" : expectedName.trim();
        return normalized.isEmpty() ? "player-name|unknown" : "player-name|" + normalized;
    }

    private String locationText(LocationInfo locationInfo) {
        if (locationInfo == null) {
            return "-";
        }
        return (locationInfo.mapName == null ? "-" : locationInfo.mapName)
                + "(" + locationInfo.x + "," + locationInfo.y + ")";
    }

    private String visionRecordText(RecordResult result) {
        if (result == null) {
            return "-";
        }
        return result.recorded() ? result.summary() : "skipped:" + result.summary();
    }

    private boolean submitMoveMouseWithWindowContext(WindowTaskSnapshot snapshot, int absoluteX, int absoluteY) {
        String windowId = snapshot == null ? null : snapshot.getWindowId();
        if (windowId == null || windowId.isBlank()) {
            log.warn("[player-name-ocr-debug] cannot move mouse without window id: point=({}, {})",
                    absoluteX, absoluteY);
            return false;
        }
        Optional<WindowRuntimeContext> context = multiWindowTaskManager.getRunner(windowId)
                .map(runner -> runner.getWindowContext());
        if (context.isEmpty()) {
            log.warn("[player-name-ocr-debug] cannot move mouse without window context: windowId={} point=({}, {})",
                    windowId, absoluteX, absoluteY);
            return false;
        }
        boolean moved = windowTaskContextHolder.callWith(context.get(), () ->
                inputSequences.submitAndWait("playerNameOcrDebug:moveAnchor",
                        List.of(InputAction.moveMouse(absoluteX, absoluteY), InputAction.sleep(300))));
        log.info("[player-name-ocr-debug] move-anchor windowId={} point=({}, {}) moved={}",
                windowId, absoluteX, absoluteY, moved);
        return moved;
    }

    private ScanRect centeredScanRect(int imageWidth, int imageHeight) {
        int width = Math.min(SCAN_WIDTH, Math.max(1, imageWidth));
        int height = Math.min(SCAN_HEIGHT, Math.max(1, imageHeight));
        int x = clamp((imageWidth - width) / 2, 0, Math.max(0, imageWidth - width));
        int y = clamp((imageHeight - height) / 2, 0, Math.max(0, imageHeight - height));
        return new ScanRect(x, y, width, height);
    }

    private BufferedImage copyImage(BufferedImage source) {
        BufferedImage copy = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = copy.createGraphics();
        try {
            graphics.drawImage(source, 0, 0, null);
        } finally {
            graphics.dispose();
        }
        return copy;
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private boolean sleepQuietly(long ms) {
        try {
            Thread.sleep(ms);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private String summarizeWords(List<OcrWordResult> words) {
        if (words == null || words.isEmpty()) {
            return "-";
        }
        return words.stream()
                .limit(WORD_SUMMARY_LIMIT)
                .map(word -> {
                    if (word == null) {
                        return "null";
                    }
                    return String.format("%s@(%d,%d,%d,%d,%d,%d,%.3f)",
                            word.getText(), word.getX(), word.getY(), word.getLeft(), word.getTop(),
                            word.getWidth(), word.getHeight(), word.getScore());
                })
                .collect(Collectors.joining(" | "));
    }

    private static String safeFileName(String value) {
        String safe = value == null ? "" : value.replaceAll("[\\\\/:*?\"<>|\\s]+", "_");
        return safe.isBlank() ? "unknown" : safe;
    }

    private static String pointText(Point point) {
        return point == null ? "null" : point.x + "," + point.y;
    }

    public record ScanRect(int x, int y, int width, int height) {
    }

    private enum TextColorMode {
        PURPLE,
        YELLOW
    }

    private record ComponentBox(int minX, int minY, int maxX, int maxY, List<Point> points) {
        int centerX() {
            return (minX + maxX) / 2;
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
            return new TextLineBox(component.minX(), component.minY(), component.maxX(), component.maxY(),
                    component.pixelCount());
        }

        boolean isSameLine(ComponentBox component) {
            int centerDelta = Math.abs(centerY() - component.centerY());
            boolean yOverlaps = component.maxY() + LINE_MERGE_Y_TOLERANCE >= minY
                    && component.minY() - LINE_MERGE_Y_TOLERANCE <= maxY;
            return yOverlaps || centerDelta <= Math.max(LINE_MERGE_Y_TOLERANCE, height() / 2);
        }

        void include(ComponentBox component) {
            minX = Math.min(minX, component.minX());
            minY = Math.min(minY, component.minY());
            maxX = Math.max(maxX, component.maxX());
            maxY = Math.max(maxY, component.maxY());
            pixelCount += component.pixelCount();
        }

        int minX() {
            return minX;
        }

        int minY() {
            return minY;
        }

        int maxX() {
            return maxX;
        }

        int maxY() {
            return maxY;
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
            return pixelCount;
        }
    }

    private record PackedLineBox(int sourceX,
                                 int sourceY,
                                 int sourceWidth,
                                 int sourceHeight,
                                 int packedX,
                                 int packedY,
                                 int packedWidth,
                                 int packedHeight) {
    }

    public record OcrVariant(String path,
                             int blackPixelCount,
                             int wordCount,
                             String wordsSummary,
                             List<OcrWordResult> words) {
        public static OcrVariant empty() {
            return new OcrVariant(null, 0, 0, "-", List.of());
        }

        public static OcrVariant of(String path, int blackPixelCount, int wordCount, String wordsSummary) {
            return new OcrVariant(path, blackPixelCount, wordCount,
                    wordsSummary == null || wordsSummary.isBlank() ? "-" : wordsSummary, List.of());
        }

        public static OcrVariant of(String path,
                                    int blackPixelCount,
                                    int wordCount,
                                    String wordsSummary,
                                    List<OcrWordResult> words) {
            return new OcrVariant(path, blackPixelCount, wordCount,
                    wordsSummary == null || wordsSummary.isBlank() ? "-" : wordsSummary,
                    words == null ? List.of() : List.copyOf(words));
        }

        public String toDetailText() {
            return "path=" + path
                    + ", blackPixels=" + blackPixelCount
                    + ", words=" + wordCount
                    + ", text=" + wordsSummary;
        }
    }

    public record DebugResult(boolean success,
                              String windowId,
                              String expectedName,
                              String rawPath,
                              String washedPath,
                              String yellowWashedPath,
                              String purpleClosedPath,
                              String yellowClosedPath,
                              ScanRect scanRect,
                              int wordCount,
                              String wordsSummary,
                              int yellowPixelCount,
                              int yellowWordCount,
                              String yellowWordsSummary,
                              int purpleClosedPixelCount,
                              int yellowClosedPixelCount,
                              int yellowClosedWordCount,
                              String yellowClosedWordsSummary,
                              OcrVariant purpleEnhanced,
                              OcrVariant yellowEnhanced,
                              Point anchorRelative,
                              Point anchorAbsolute,
                              boolean mouseMoved,
                              String message) {

        public static DebugResult failed(String windowId, String expectedName, String message) {
            return of(false, windowId, expectedName, null, null, null, null, null, null,
                    0, "-", 0, 0, "-", 0, 0, 0, "-",
                    OcrVariant.empty(), OcrVariant.empty(), null, null, false, message);
        }

        public static DebugResult of(boolean success,
                                     String windowId,
                                     String expectedName,
                                     String rawPath,
                                     String washedPath,
                                     String yellowWashedPath,
                                     String purpleClosedPath,
                                     String yellowClosedPath,
                                     ScanRect scanRect,
                                     int wordCount,
                                     String wordsSummary,
                                     int yellowPixelCount,
                                     int yellowWordCount,
                                     String yellowWordsSummary,
                                     int purpleClosedPixelCount,
                                     int yellowClosedPixelCount,
                                     int yellowClosedWordCount,
                                     String yellowClosedWordsSummary,
                                     OcrVariant purpleEnhanced,
                                     OcrVariant yellowEnhanced,
                                     Point anchorRelative,
                                     Point anchorAbsolute,
                                     boolean mouseMoved,
                                     String message) {
            return new DebugResult(success, windowId, expectedName, rawPath, washedPath,
                    yellowWashedPath, purpleClosedPath, yellowClosedPath, scanRect, wordCount,
                    wordsSummary == null || wordsSummary.isBlank() ? "-" : wordsSummary,
                    yellowPixelCount, yellowWordCount,
                    yellowWordsSummary == null || yellowWordsSummary.isBlank() ? "-" : yellowWordsSummary,
                    purpleClosedPixelCount, yellowClosedPixelCount, yellowClosedWordCount,
                    yellowClosedWordsSummary == null || yellowClosedWordsSummary.isBlank() ? "-" : yellowClosedWordsSummary,
                    purpleEnhanced == null ? OcrVariant.empty() : purpleEnhanced,
                    yellowEnhanced == null ? OcrVariant.empty() : yellowEnhanced,
                    anchorRelative, anchorAbsolute, mouseMoved, message == null ? "" : message);
        }

        public String toDetailMessage() {
            String scan = scanRect == null
                    ? "-"
                    : scanRect.x + "," + scanRect.y + " " + scanRect.width + "x" + scanRect.height;
            return message
                    + " | expected=" + expectedName
                    + " | rel=" + pointText(anchorRelative)
                    + " | abs=" + pointText(anchorAbsolute)
                    + " | mouseMoved=" + mouseMoved
                    + " | scan=" + scan
                    + " | purpleSegmentedEnhanced=" + purpleEnhanced.toDetailText()
                    + " | yellowSegmentedEnhanced=" + yellowEnhanced.toDetailText();
        }
    }
}
