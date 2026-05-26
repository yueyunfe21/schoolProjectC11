import com.bot.dhxy.driver.BoundWindowCaptureService;
import com.bot.dhxy.window.discovery.NativeWindowInfo;
import com.bot.dhxy.window.discovery.WindowsNativeWindowScanner;
import com.bot.dhxy.window.model.WindowNativeBinding;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class YellowOcrProbe {
    private static final int SCAN_WIDTH = 760;
    private static final int SCAN_HEIGHT = 320;
    private static final int SCALE = 4;
    private static final int LINE_CROP_MARGIN = 8;
    private static final int LINE_PACK_GAP = 18;
    private static final int LINE_MERGE_Y_TOLERANCE = 8;
    private static final int COMPONENT_MIN_PIXELS = 3;
    private static final int COMPONENT_MIN_WIDTH = 1;
    private static final int COMPONENT_MIN_HEIGHT = 2;
    private static final int COMPONENT_MAX_WIDTH = 120;
    private static final int COMPONENT_MAX_HEIGHT = 48;
    private static final int COMPONENT_MAX_PIXELS = 1200;
    private static final double DUPLICATE_LINE_OVERLAP_RATIO = 0.55;

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final HttpClient HTTP = HttpClient.newHttpClient();

    public static void main(String[] args) throws Exception {
        boolean fullWindowScan = false;
        String expected = "无名小妖";
        expected = "\u65e0\u540d\u5c0f\u5996";
        for (String arg : args) {
            if ("--full".equals(arg)) {
                fullWindowScan = true;
            } else if (arg != null && !arg.isBlank()) {
                expected = arg;
            }
        }
        NativeWindowInfo info = new WindowsNativeWindowScanner().scanGameWindows().stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No DHXY game window found"));
        WindowNativeBinding binding = new WindowNativeBinding(
                info.getHandle(),
                info.getTitle(),
                info.getClassName(),
                info.getProcessId(),
                info.getX(),
                info.getY(),
                info.getWidth(),
                info.getHeight());

        BoundWindowCaptureService.CaptureResult capture = new BoundWindowCaptureService()
                .captureWindow(binding)
                .orElseThrow(() -> new IllegalStateException("HWND capture failed: " + info.getTitle()));
        BufferedImage window = capture.image();
        Path outputDir = Path.of("images", "temp", "yellow_probe",
                DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").format(LocalDateTime.now()));
        Files.createDirectories(outputDir);
        ImageIO.write(window, "png", outputDir.resolve("full_window.png").toFile());

        Rect rect = fullWindowScan
                ? new Rect(0, 0, window.getWidth(), window.getHeight())
                : centeredScanRect(window.getWidth(), window.getHeight());
        BufferedImage raw = copy(window.getSubimage(rect.x(), rect.y(), rect.width(), rect.height()));
        ImageIO.write(raw, "png", outputDir.resolve("raw_center.png").toFile());

        System.out.printf("window=%s hwnd=%s provider=%s size=%dx%d scan=(%d,%d %dx%d) expected=%s%n",
                info.getTitle(), info.getHandle(), capture.provider(),
                window.getWidth(), window.getHeight(), rect.x(), rect.y(), rect.width(), rect.height(), expected);

        List<Variant> variants = List.of(
                new Variant("strict", YellowOcrProbe::strictYellow, 0, 0),
                new Variant("loose", YellowOcrProbe::looseYellow, 0, 0),
                new Variant("loose_bridge2", YellowOcrProbe::looseYellow, 2, 0),
                new Variant("loose_bridge2_dilateX", YellowOcrProbe::looseYellow, 2, 1),
                new Variant("wide_hsv", YellowOcrProbe::wideHsvYellow, 1, 0),
                new Variant("wide_hsv_bridge2", YellowOcrProbe::wideHsvYellow, 2, 0)
        );

        Result best = null;
        for (Variant variant : variants) {
            Result result = runVariant(raw, outputDir, variant);
            TargetMatch match = targetMatch(result.text(), expected);
            if (best == null || score(result, expected) > score(best, expected)) {
                best = result;
            }
            printResult(variant.name(), match, result);
        }
        for (Variant variant : List.of(
                new Variant("raw_lines_loose", YellowOcrProbe::looseYellow, 0, 0),
                new Variant("raw_lines_wide_hsv", YellowOcrProbe::wideHsvYellow, 0, 0),
                new Variant("shadow_loose", YellowOcrProbe::looseYellow, 0, 0))) {
            Result result = variant.name().startsWith("raw_lines")
                    ? runRawLineVariant(raw, outputDir, variant)
                    : runShadowVariant(raw, outputDir, variant);
            TargetMatch match = targetMatch(result.text(), expected);
            if (best == null || score(result, expected) > score(best, expected)) {
                best = result;
            }
            printResult(variant.name(), match, result);
        }
        for (LineVariant variant : List.of(
                new LineVariant("line_match_loose", YellowOcrProbe::looseYellow, false),
                new LineVariant("line_match_shadow", YellowOcrProbe::looseYellow, true),
                new LineVariant("line_match_raw", YellowOcrProbe::looseYellow, false))) {
            Result result = variant.name().endsWith("_raw")
                    ? runLineByLineRawVariant(raw, outputDir, variant, expected)
                    : runLineByLineMaskVariant(raw, outputDir, variant, expected);
            TargetMatch match = targetMatch(result.text(), expected);
            if (best == null || score(result, expected) > score(best, expected)) {
                best = result;
            }
            printResult(variant.name(), match, result);
        }
        if (best != null) {
            System.out.printf("best=%s text=%s path=%s%n", best.variantName(), best.text(), best.path());
        }

        raw.flush();
        window.flush();
    }

    private static Result runVariant(BufferedImage raw, Path outputDir, Variant variant) throws Exception {
        boolean[][] mask = buildMask(raw, variant.predicate());
        if (variant.bridgeGap() > 0) {
            mask = bridgeSmallGaps(mask, variant.bridgeGap());
        }
        if (variant.dilateX() > 0) {
            mask = dilateHorizontal(mask, variant.dilateX());
        }
        List<TextLineBox> lines = groupTextLines(mask);
        Path outputPath = outputDir.resolve(variant.name() + ".png").toAbsolutePath().normalize();
        int blackPixels = writePackedLineMask(mask, lines, outputPath);
        OcrText ocr = ocr(outputPath);
        return new Result(variant.name(), outputPath, blackPixels, ocr.wordCount(), ocr.text());
    }

    private static Result runLineByLineMaskVariant(BufferedImage raw,
                                                   Path outputDir,
                                                   LineVariant variant,
                                                   String expected) throws Exception {
        boolean[][] mask = buildMask(raw, variant.predicate());
        if (variant.shadow()) {
            mask = includeNearbyYellowShadow(raw, mask, 2);
        }
        List<TextLineBox> lines = groupTextLines(mask);
        Result best = null;
        for (int i = 0; i < lines.size(); i++) {
            Path outputPath = outputDir.resolve(variant.name() + "_" + i + ".png").toAbsolutePath().normalize();
            int blackPixels = writePackedLineMask(mask, List.of(lines.get(i)), outputPath);
            OcrText ocr = ocr(outputPath);
            Result result = new Result(variant.name(), outputPath, blackPixels, ocr.wordCount(), ocr.text());
            if (best == null || score(result, expected) > score(best, expected)) {
                best = result;
            }
        }
        return best == null
                ? new Result(variant.name(), outputDir.resolve(variant.name() + "_empty.png"), 0, 0, "")
                : best;
    }

    private static Result runLineByLineRawVariant(BufferedImage raw,
                                                  Path outputDir,
                                                  LineVariant variant,
                                                  String expected) throws Exception {
        boolean[][] mask = buildMask(raw, variant.predicate());
        List<TextLineBox> lines = groupTextLines(mask);
        Result best = null;
        for (int i = 0; i < lines.size(); i++) {
            Path outputPath = outputDir.resolve(variant.name() + "_" + i + ".png").toAbsolutePath().normalize();
            int pixels = writePackedRawLines(raw, List.of(lines.get(i)), outputPath);
            OcrText ocr = ocr(outputPath);
            Result result = new Result(variant.name(), outputPath, pixels, ocr.wordCount(), ocr.text());
            if (best == null || score(result, expected) > score(best, expected)) {
                best = result;
            }
        }
        return best == null
                ? new Result(variant.name(), outputDir.resolve(variant.name() + "_empty.png"), 0, 0, "")
                : best;
    }

    private static Result runRawLineVariant(BufferedImage raw, Path outputDir, Variant variant) throws Exception {
        boolean[][] mask = buildMask(raw, variant.predicate());
        List<TextLineBox> lines = groupTextLines(mask);
        Path outputPath = outputDir.resolve(variant.name() + ".png").toAbsolutePath().normalize();
        int pixels = writePackedRawLines(raw, lines, outputPath);
        OcrText ocr = ocr(outputPath);
        return new Result(variant.name(), outputPath, pixels, ocr.wordCount(), ocr.text());
    }

    private static Result runShadowVariant(BufferedImage raw, Path outputDir, Variant variant) throws Exception {
        boolean[][] baseMask = buildMask(raw, variant.predicate());
        boolean[][] shadowMask = includeNearbyYellowShadow(raw, baseMask, 2);
        List<TextLineBox> lines = groupTextLines(shadowMask);
        Path outputPath = outputDir.resolve(variant.name() + ".png").toAbsolutePath().normalize();
        int blackPixels = writePackedLineMask(shadowMask, lines, outputPath);
        OcrText ocr = ocr(outputPath);
        return new Result(variant.name(), outputPath, blackPixels, ocr.wordCount(), ocr.text());
    }

    private static boolean[][] buildMask(BufferedImage raw, YellowPredicate predicate) {
        int width = raw.getWidth();
        int height = raw.getHeight();
        boolean[][] source = new boolean[height][width];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = raw.getRGB(x, y);
                int r = (rgb >> 16) & 0xff;
                int g = (rgb >> 8) & 0xff;
                int b = rgb & 0xff;
                source[y][x] = predicate.test(r, g, b);
            }
        }

        boolean[][] kept = new boolean[height][width];
        boolean[][] visited = new boolean[height][width];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (!source[y][x] || visited[y][x]) {
                    continue;
                }
                ComponentBox component = collectComponent(source, visited, x, y);
                if (shouldKeepComponent(component)) {
                    for (Point point : component.points()) {
                        kept[point.y][point.x] = true;
                    }
                }
            }
        }
        return kept;
    }

    private static boolean strictYellow(int r, int g, int b) {
        return r >= 150
                && g >= 110
                && b <= 110
                && Math.abs(r - g) <= 110
                && r > b + 60
                && g > b + 40;
    }

    private static boolean looseYellow(int r, int g, int b) {
        if (strictYellow(r, g, b)) {
            return true;
        }
        float[] hsb = Color.RGBtoHSB(r, g, b, null);
        float hue = hsb[0] * 360.0f;
        return hue >= 28.0f
                && hue <= 68.0f
                && hsb[1] >= 0.38f
                && hsb[2] >= 0.30f
                && r >= 95
                && g >= 75
                && b <= 135
                && r > b + 28
                && g > b + 16;
    }

    private static boolean wideHsvYellow(int r, int g, int b) {
        float[] hsb = Color.RGBtoHSB(r, g, b, null);
        float hue = hsb[0] * 360.0f;
        return hue >= 24.0f
                && hue <= 74.0f
                && hsb[1] >= 0.28f
                && hsb[2] >= 0.24f
                && r >= 80
                && g >= 65
                && b <= 150
                && r > b + 18
                && g > b + 8;
    }

    private static boolean[][] bridgeSmallGaps(boolean[][] mask, int maxGap) {
        int height = mask.length;
        int width = mask[0].length;
        boolean[][] result = copyMask(mask);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (mask[y][x]) {
                    continue;
                }
                boolean horizontal = false;
                boolean vertical = false;
                for (int gap = 1; gap <= maxGap; gap++) {
                    horizontal |= x - gap >= 0 && x + gap < width && mask[y][x - gap] && mask[y][x + gap];
                    vertical |= y - gap >= 0 && y + gap < height && mask[y - gap][x] && mask[y + gap][x];
                }
                if (horizontal || vertical) {
                    result[y][x] = true;
                }
            }
        }
        return result;
    }

    private static boolean[][] dilateHorizontal(boolean[][] mask, int radius) {
        int height = mask.length;
        int width = mask[0].length;
        boolean[][] result = copyMask(mask);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (!mask[y][x]) {
                    continue;
                }
                for (int dx = -radius; dx <= radius; dx++) {
                    int nx = x + dx;
                    if (nx >= 0 && nx < width) {
                        result[y][nx] = true;
                    }
                }
            }
        }
        return result;
    }

    private static boolean[][] includeNearbyYellowShadow(BufferedImage raw, boolean[][] baseMask, int radius) {
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
                int r = (rgb >> 16) & 0xff;
                int g = (rgb >> 8) & 0xff;
                int b = rgb & 0xff;
                if (isYellowShadow(r, g, b)) {
                    result[y][x] = true;
                }
            }
        }
        return result;
    }

    private static boolean isYellowShadow(int r, int g, int b) {
        float[] hsb = Color.RGBtoHSB(r, g, b, null);
        float hue = hsb[0] * 360.0f;
        return hue >= 25.0f
                && hue <= 85.0f
                && hsb[1] >= 0.22f
                && hsb[2] >= 0.16f
                && r >= 45
                && g >= 42
                && b <= 150
                && Math.max(r, g) > b + 6;
    }

    private static boolean[][] copyMask(boolean[][] mask) {
        boolean[][] copy = new boolean[mask.length][mask[0].length];
        for (int y = 0; y < mask.length; y++) {
            System.arraycopy(mask[y], 0, copy[y], 0, mask[y].length);
        }
        return copy;
    }

    private static List<TextLineBox> groupTextLines(boolean[][] mask) {
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
        components.sort(Comparator.comparingInt(ComponentBox::centerY).thenComparingInt(ComponentBox::minX));

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
        lines = mergeDuplicateLineBoxes(lines);
        lines.sort(Comparator.comparingInt(TextLineBox::centerY).thenComparingInt(TextLineBox::minX));
        return lines;
    }

    /**
     * Merge overlapping line boxes that describe the same visible yellow label.
     *
     * <p>The probe builds line boxes from color components. A decorated yellow sign can split into
     * several horizontal components, and their padded crops may heavily overlap. Without this pass,
     * {@link #writePackedRawLines(BufferedImage, List, Path)} can pack the same sign twice and make
     * OCR return duplicated text such as "任务任务". This is debug-probe cleanup only; it does not
     * change production task clicking.</p>
     *
     * @param input candidate line boxes in raw crop coordinates.
     * @return line boxes with obvious duplicate/overlapping entries merged before OCR packing.
     */
    private static List<TextLineBox> mergeDuplicateLineBoxes(List<TextLineBox> input) {
        if (input == null || input.size() <= 1) {
            return input == null ? List.of() : input;
        }
        List<TextLineBox> merged = new ArrayList<>();
        for (TextLineBox candidate : input) {
            TextLineBox target = null;
            for (TextLineBox existing : merged) {
                if (existing.isDuplicateCropOf(candidate)) {
                    target = existing;
                    break;
                }
            }
            if (target == null) {
                merged.add(candidate.copy());
            } else {
                target.include(candidate);
            }
        }
        return merged;
    }

    private static int writePackedLineMask(boolean[][] mask, List<TextLineBox> lines, Path outputPath) throws Exception {
        int height = mask.length;
        int width = mask[0].length;
        if (lines.isEmpty()) {
            BufferedImage blank = new BufferedImage(1, 1, BufferedImage.TYPE_BYTE_BINARY);
            blank.setRGB(0, 0, 0xffffff);
            ImageIO.write(blank, "png", outputPath.toFile());
            blank.flush();
            return 0;
        }

        List<PackedLineBox> packedLines = new ArrayList<>();
        int outputWidth = 1;
        int outputHeight = 0;
        for (TextLineBox line : lines) {
            int sourceX = clamp(line.minX() - LINE_CROP_MARGIN, 0, width - 1);
            int sourceY = clamp(line.minY() - LINE_CROP_MARGIN, 0, height - 1);
            int sourceRight = clamp(line.maxX() + LINE_CROP_MARGIN, 0, width - 1);
            int sourceBottom = clamp(line.maxY() + LINE_CROP_MARGIN, 0, height - 1);
            int lineWidth = sourceRight - sourceX + 1;
            int lineHeight = sourceBottom - sourceY + 1;
            int packedWidth = lineWidth * SCALE;
            int packedHeight = lineHeight * SCALE;
            outputWidth = Math.max(outputWidth, packedWidth);
            packedLines.add(new PackedLineBox(sourceX, sourceY, lineWidth, lineHeight,
                    0, outputHeight, packedWidth, packedHeight));
            outputHeight += packedHeight + LINE_PACK_GAP;
        }
        outputHeight = Math.max(1, outputHeight - LINE_PACK_GAP);

        BufferedImage output = new BufferedImage(outputWidth, outputHeight, BufferedImage.TYPE_BYTE_BINARY);
        int black = 0;
        Graphics2D graphics = output.createGraphics();
        try {
            graphics.setColor(java.awt.Color.WHITE);
            graphics.fillRect(0, 0, outputWidth, outputHeight);
        } finally {
            graphics.dispose();
        }
        for (PackedLineBox line : packedLines) {
            for (int y = 0; y < line.packedHeight(); y++) {
                for (int x = 0; x < line.packedWidth(); x++) {
                    int sourceX = line.sourceX() + x / SCALE;
                    int sourceY = line.sourceY() + y / SCALE;
                    if (mask[sourceY][sourceX]) {
                        black++;
                        output.setRGB(line.packedX() + x, line.packedY() + y, 0x000000);
                    }
                }
            }
        }
        ImageIO.write(output, "png", outputPath.toFile());
        output.flush();
        return black;
    }

    private static int writePackedRawLines(BufferedImage raw, List<TextLineBox> lines, Path outputPath) throws Exception {
        int imageHeight = raw.getHeight();
        int imageWidth = raw.getWidth();
        if (lines.isEmpty()) {
            BufferedImage blank = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
            blank.setRGB(0, 0, 0xffffff);
            ImageIO.write(blank, "png", outputPath.toFile());
            blank.flush();
            return 0;
        }

        List<PackedLineBox> packedLines = new ArrayList<>();
        int outputWidth = 1;
        int outputHeight = 0;
        for (TextLineBox line : lines) {
            int sourceX = clamp(line.minX() - LINE_CROP_MARGIN, 0, imageWidth - 1);
            int sourceY = clamp(line.minY() - LINE_CROP_MARGIN, 0, imageHeight - 1);
            int sourceRight = clamp(line.maxX() + LINE_CROP_MARGIN, 0, imageWidth - 1);
            int sourceBottom = clamp(line.maxY() + LINE_CROP_MARGIN, 0, imageHeight - 1);
            int lineWidth = sourceRight - sourceX + 1;
            int lineHeight = sourceBottom - sourceY + 1;
            int packedWidth = lineWidth * SCALE;
            int packedHeight = lineHeight * SCALE;
            outputWidth = Math.max(outputWidth, packedWidth);
            packedLines.add(new PackedLineBox(sourceX, sourceY, lineWidth, lineHeight,
                    0, outputHeight, packedWidth, packedHeight));
            outputHeight += packedHeight + LINE_PACK_GAP;
        }
        outputHeight = Math.max(1, outputHeight - LINE_PACK_GAP);

        BufferedImage output = new BufferedImage(outputWidth, outputHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = output.createGraphics();
        try {
            graphics.setColor(java.awt.Color.WHITE);
            graphics.fillRect(0, 0, outputWidth, outputHeight);
        } finally {
            graphics.dispose();
        }
        int copiedPixels = 0;
        for (PackedLineBox line : packedLines) {
            for (int y = 0; y < line.packedHeight(); y++) {
                for (int x = 0; x < line.packedWidth(); x++) {
                    int sourceX = line.sourceX() + x / SCALE;
                    int sourceY = line.sourceY() + y / SCALE;
                    output.setRGB(line.packedX() + x, line.packedY() + y, raw.getRGB(sourceX, sourceY));
                    copiedPixels++;
                }
            }
        }
        ImageIO.write(output, "png", outputPath.toFile());
        output.flush();
        return copiedPixels;
    }

    private static ComponentBox collectComponent(boolean[][] mask, boolean[][] visited, int startX, int startY) {
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

    private static boolean shouldKeepComponent(ComponentBox component) {
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

    private static OcrText ocr(Path imagePath) throws Exception {
        String json = MAPPER.writeValueAsString(java.util.Map.of(
                "imagePath", imagePath.toString().replace('\\', '/')));
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:18761/ocr/words"))
                .header("Content-Type", "application/json; charset=utf-8")
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                .build();
        HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        JsonNode root = MAPPER.readTree(response.body());
        String text = root.path("text").asText("");
        int count = root.path("words").isArray() ? root.path("words").size() : 0;
        return new OcrText(text, count);
    }

    private static int score(Result result, String expected) {
        TargetMatch match = targetMatch(result.text(), expected);
        if (match.hit()) {
            return 1000 - match.editDistance() * 100 + match.longestCommonSubstring() * 10;
        }
        String text = normalizeName(result.text());
        String target = normalizeName(expected);
        int score = 0;
        for (int i = 0; i < target.length(); i++) {
            if (text.indexOf(target.charAt(i)) >= 0) {
                score += 10;
            }
        }
        score += Math.min(result.wordCount(), 4);
        return score;
    }

    private static void printResult(String name, TargetMatch match, Result result) {
        System.out.printf("%-20s hit=%-5s dist=%-2d common=%-2d black=%-6d words=%-2d text=%s path=%s%n",
                name, match.hit(), match.editDistance(), match.longestCommonSubstring(),
                result.blackPixels(), result.wordCount(),
                result.text().isBlank() ? "-" : result.text(), result.path());
    }

    private static TargetMatch targetMatch(String ocrText, String expected) {
        String text = normalizeName(ocrText);
        String target = normalizeName(expected);
        if (target.isBlank() || text.isBlank()) {
            return new TargetMatch(false, 999, 0);
        }
        if (text.contains(target)) {
            return new TargetMatch(true, 0, target.length());
        }
        if (target.length() <= 2) {
            return new TargetMatch(false, editDistance(text, target), longestCommonSubstring(text, target));
        }

        int maxDistance = target.length() >= 6 ? 2 : 1;
        int minWindow = Math.max(1, target.length() - maxDistance);
        int maxWindow = Math.min(text.length(), target.length() + maxDistance);
        int bestDistance = 999;
        int bestCommon = 0;
        for (int start = 0; start < text.length(); start++) {
            for (int len = minWindow; len <= maxWindow && start + len <= text.length(); len++) {
                String window = text.substring(start, start + len);
                int distance = editDistance(window, target);
                int common = longestCommonSubstring(window, target);
                if (distance < bestDistance || (distance == bestDistance && common > bestCommon)) {
                    bestDistance = distance;
                    bestCommon = common;
                }
            }
        }

        int minCommon = Math.max(2, target.length() - maxDistance);
        boolean hit = bestDistance <= maxDistance && bestCommon >= minCommon;
        return new TargetMatch(hit, bestDistance, bestCommon);
    }

    private static String normalizeName(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (char c : value.toCharArray()) {
            if ((c >= '\u4e00' && c <= '\u9fff') || Character.isLetterOrDigit(c)) {
                builder.append(Character.toLowerCase(c));
            }
        }
        return builder.toString();
    }

    private static int editDistance(String left, String right) {
        int[][] dp = new int[left.length() + 1][right.length() + 1];
        for (int i = 0; i <= left.length(); i++) {
            dp[i][0] = i;
        }
        for (int j = 0; j <= right.length(); j++) {
            dp[0][j] = j;
        }
        for (int i = 1; i <= left.length(); i++) {
            for (int j = 1; j <= right.length(); j++) {
                int cost = left.charAt(i - 1) == right.charAt(j - 1) ? 0 : 1;
                dp[i][j] = Math.min(
                        Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                        dp[i - 1][j - 1] + cost);
            }
        }
        return dp[left.length()][right.length()];
    }

    private static int longestCommonSubstring(String left, String right) {
        int best = 0;
        int[][] dp = new int[left.length() + 1][right.length() + 1];
        for (int i = 1; i <= left.length(); i++) {
            for (int j = 1; j <= right.length(); j++) {
                if (left.charAt(i - 1) == right.charAt(j - 1)) {
                    dp[i][j] = dp[i - 1][j - 1] + 1;
                    best = Math.max(best, dp[i][j]);
                }
            }
        }
        return best;
    }

    private static Rect centeredScanRect(int imageWidth, int imageHeight) {
        int width = Math.min(SCAN_WIDTH, Math.max(1, imageWidth));
        int height = Math.min(SCAN_HEIGHT, Math.max(1, imageHeight));
        int x = clamp((imageWidth - width) / 2, 0, Math.max(0, imageWidth - width));
        int y = clamp((imageHeight - height) / 2, 0, Math.max(0, imageHeight - height));
        return new Rect(x, y, width, height);
    }

    private static BufferedImage copy(BufferedImage source) {
        BufferedImage copy = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = copy.createGraphics();
        try {
            graphics.drawImage(source, 0, 0, null);
        } finally {
            graphics.dispose();
        }
        return copy;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private record Variant(String name, YellowPredicate predicate, int bridgeGap, int dilateX) {
    }

    private record LineVariant(String name, YellowPredicate predicate, boolean shadow) {
    }

    private record Result(String variantName, Path path, int blackPixels, int wordCount, String text) {
    }

    private record OcrText(String text, int wordCount) {
    }

    private record TargetMatch(boolean hit, int editDistance, int longestCommonSubstring) {
    }

    private record Rect(int x, int y, int width, int height) {
    }

    private record PackedLineBox(int sourceX, int sourceY, int sourceWidth, int sourceHeight,
                                 int packedX, int packedY, int packedWidth, int packedHeight) {
    }

    private record ComponentBox(int minX, int minY, int maxX, int maxY, List<Point> points) {
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

        void include(TextLineBox line) {
            minX = Math.min(minX, line.minX());
            minY = Math.min(minY, line.minY());
            maxX = Math.max(maxX, line.maxX());
            maxY = Math.max(maxY, line.maxY());
            pixelCount += line.pixelCount();
        }

        TextLineBox copy() {
            return new TextLineBox(minX, minY, maxX, maxY, pixelCount);
        }

        boolean isDuplicateCropOf(TextLineBox other) {
            int overlapWidth = Math.max(0, Math.min(maxX, other.maxX()) - Math.max(minX, other.minX()) + 1);
            int overlapHeight = Math.max(0, Math.min(maxY, other.maxY()) - Math.max(minY, other.minY()) + 1);
            if (overlapWidth <= 0 || overlapHeight <= 0) {
                return false;
            }
            int overlapArea = overlapWidth * overlapHeight;
            int smallerArea = Math.max(1, Math.min(width() * height(), other.width() * other.height()));
            double overlapRatio = overlapArea / (double) smallerArea;
            boolean similarCenter = Math.abs(centerY() - other.centerY())
                    <= Math.max(LINE_MERGE_Y_TOLERANCE * 2, Math.min(height(), other.height()));
            return overlapRatio >= DUPLICATE_LINE_OVERLAP_RATIO && similarCenter;
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

    @FunctionalInterface
    private interface YellowPredicate {
        boolean test(int r, int g, int b);
    }
}
