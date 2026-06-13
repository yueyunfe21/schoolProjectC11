package com.bot.dhxy.debug;

import com.bot.dhxy.tools.ImagePreprocessor;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * 离线回放五环任务追踪绿字点击算法。
 *
 * <p>输入是历史保存的 {@code wuhuan_tracker*_block_raw.png} 原始任务追踪块截图，
 * 输出 CSV 和失败标注图。这个工具只用于验证点击点算法，不发送任何鼠标/键盘输入。</p>
 */
public class WuhuanTrackerGreenReplayDebugMain {

    private static final Path ROOT = Path.of("D:/mavenProject/DHXY");
    private static final Path IMAGE_ROOT = ROOT.resolve("images/test-cases/task-tracker/wuhuan-task-panel-block/raw");
    private static final Path REPORT = ROOT.resolve("logs/wuhuan-tracker-green-replay.csv");
    private static final Path FAILURE_DIR = ROOT.resolve("images/temp/wuhuan_tracker_replay_failures");
    private static final Path OUTPUT_DIR = ROOT.resolve("images/test-cases/task-tracker/wuhuan-task-panel-block/output");
    private static final Path REJECTED_DIR = ROOT.resolve("images/test-cases/task-tracker/wuhuan-task-panel-block/rejected-non-wuhuan");

    private static final int TRACKER_LINK_MIN_PIXELS = 20;
    private static final int TRACKER_LINK_SPLIT_GAP = 8;
    private static final int TRACKER_LINK_DELIMITER_MAX_WIDTH = 5;
    private static final int TRACKER_LINK_DELIMITER_MAX_PIXELS = 18;
    private static final int TRACKER_COORD_GLYPH_MAX_WIDTH = 5;
    private static final int TRACKER_COORD_GLYPH_MIN_RUN = 5;

    public static void main(String[] args) throws Exception {
        Files.createDirectories(REPORT.getParent());
        Files.createDirectories(FAILURE_DIR);
        Files.createDirectories(OUTPUT_DIR);
        Files.createDirectories(REJECTED_DIR);
        clearPngFiles(OUTPUT_DIR);
        clearPngFiles(REJECTED_DIR);

        List<Path> samples;
        try (Stream<Path> stream = Files.walk(IMAGE_ROOT)) {
            samples = stream
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().matches("wuhuan_tracker.*_block_raw\\.png"))
                    .sorted(Comparator.comparing(Path::toString))
                    .toList();
        }

        int ok = 0;
        int skipped = 0;
        int failed = 0;
        int rejected = 0;
        int warned = 0;
        try (BufferedWriter writer = Files.newBufferedWriter(REPORT, StandardCharsets.UTF_8)) {
            writer.write("status,warning,file,width,height,band,segments,selected,click,greenNearClick,reason");
            writer.newLine();
            for (Path sample : samples) {
                ReplayResult result = replay(sample);
                if (result.status == ReplayStatus.OK) {
                    ok++;
                } else if (result.status == ReplayStatus.SKIP) {
                    skipped++;
                    if ("NOT_WUHUAN_TITLE".equals(result.reason)) {
                        rejected++;
                        saveRejectedImage(sample, result);
                    }
                } else {
                    failed++;
                    saveFailureImage(sample, result);
                }
                if (result.status == ReplayStatus.OK) {
                    saveOutputImage(sample, result);
                }
                if (!result.warning.isBlank()) {
                    warned++;
                }
                writer.write(result.toCsv());
                writer.newLine();
            }
        }

        System.out.printf("WUHUAN_TRACKER_REPLAY samples=%d ok=%d skipped=%d rejected=%d failed=%d warned=%d report=%s failures=%s output=%s rejectedDir=%s%n",
                samples.size(), ok, skipped, rejected, failed, warned, REPORT, FAILURE_DIR, OUTPUT_DIR, REJECTED_DIR);
        if (failed > 0) {
            throw new IllegalStateException("五环 tracker 绿字回放存在失败样本: " + failed);
        }
    }

    private static ReplayResult replay(Path sample) {
        try {
            BufferedImage image = ImageIO.read(sample.toFile());
            if (image == null) {
                return ReplayResult.fail(sample, 0, 0, "", "", null, null, 0, "IMAGE_READ_NULL");
            }
            if (!looksLikeWuhuanTitle(image)) {
                return ReplayResult.skip(sample, image.getWidth(), image.getHeight(), "", "", "NOT_WUHUAN_TITLE");
            }
            List<ImagePreprocessor.GreenTextBand> bands = ImagePreprocessor.findGreenTextBands(image);
            ImagePreprocessor.GreenTextBand band = ImagePreprocessor.pickGreenTextBand(bands, true);
            if (band == null) {
                return ReplayResult.skip(sample, image.getWidth(), image.getHeight(), "", "", "NO_GREEN_BAND");
            }

            List<TrackerGreenLinkSegment> segments = splitTrackerGreenLinkSegments(image, band);
            Optional<TrackerGreenLinkSegment> selected = findTrackerPathingNameSegment(segments);
            if (selected.isEmpty()) {
                return ReplayResult.skip(sample, image.getWidth(), image.getHeight(), bandText(band),
                        segments.toString(), "NO_LINK_SEGMENT");
            }

            TrackerGreenLinkSegment segment = selected.get();
            Point click = resolveTrackerGreenClickPoint(image, segment);
            int greenNearClick = countGreenNear(image, click, 4);
            boolean clickLooksValid = click.x == segmentCenterX(segment)
                    && click.y == segmentCenterY(segment);
            String warning = "";
            if (segment.width() > 80) {
                warning = "WIDE_SELECTED_SEGMENT";
            }
            if (!clickLooksValid) {
                return ReplayResult.fail(sample, image.getWidth(), image.getHeight(), bandText(band),
                        segments.toString(), segment, click, greenNearClick, "CLICK_NOT_ON_SELECTED_GREEN");
            }
            return ReplayResult.ok(sample, image.getWidth(), image.getHeight(), bandText(band),
                    segments.toString(), segment, click, greenNearClick, warning);
        } catch (Exception e) {
            return ReplayResult.fail(sample, 0, 0, "", "", null, null, 0,
                    "EXCEPTION:" + e.getClass().getSimpleName() + ":" + e.getMessage());
        }
    }

    private static boolean looksLikeWuhuanTitle(BufferedImage image) {
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        int pixels = 0;
        for (int y = 0; y < Math.min(25, image.getHeight()); y++) {
            for (int x = 0; x < Math.min(90, image.getWidth()); x++) {
                int rgb = image.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;
                if (ImagePreprocessor.isYellowTextPixel(r, g, b)) {
                    pixels++;
                    minX = Math.min(minX, x);
                    minY = Math.min(minY, y);
                    maxX = Math.max(maxX, x);
                    maxY = Math.max(maxY, y);
                }
            }
        }
        if (pixels < 20 || minX == Integer.MAX_VALUE) {
            return false;
        }
        int width = maxX - minX + 1;
        int height = maxY - minY + 1;
        /*
         * Five-ring block titles are the short two-character yellow label "五环".
         * Longer titles such as "浮生半日闲" are deliberately rejected from this replay set.
         */
        return width >= 18 && width <= 40 && height >= 4 && height <= 18;
    }

    private static Optional<TrackerGreenLinkSegment> findTrackerPathingNameSegment(List<TrackerGreenLinkSegment> segments) {
        if (segments.size() == 1) {
            TrackerGreenLinkSegment only = segments.get(0);
            return looksLikePathingLinkSegment(only) ? Optional.of(only) : Optional.empty();
        }
        if (segments.size() == 2) {
            TrackerGreenLinkSegment last = segments.get(1);
            if (looksLikePathingLinkSegment(last)) {
                return Optional.of(last);
            }
            TrackerGreenLinkSegment first = segments.get(0);
            return looksLikePathingLinkSegment(first) ? Optional.of(first) : Optional.empty();
        }
        if (segments.size() < 3) {
            return Optional.empty();
        }

        TrackerGreenLinkSegment last = segments.get(segments.size() - 1);
        TrackerGreenLinkSegment beforeProgress = segments.get(segments.size() - 2);
        if (looksLikeProgressTailSegment(last) && looksLikePathingLinkSegment(beforeProgress)) {
            return Optional.of(beforeProgress);
        }
        if (looksLikePathingLinkSegment(last)) {
            return Optional.of(last);
        }
        return Optional.empty();
    }

    private static boolean looksLikePathingLinkSegment(TrackerGreenLinkSegment segment) {
        return segment.width() >= 18 && segment.pixels >= 50;
    }

    private static boolean looksLikeProgressTailSegment(TrackerGreenLinkSegment segment) {
        return segment.width() <= 18 && segment.pixels <= 70;
    }

    private static List<TrackerGreenLinkSegment> splitTrackerGreenLinkSegments(BufferedImage frame,
                                                                               ImagePreprocessor.GreenTextBand band) {
        List<TrackerGreenLinkSegment> targets = new ArrayList<>();
        for (ImagePreprocessor.GreenTextBand line : splitTrackerGreenLines(frame, band)) {
            List<TrackerGreenGlyph> lineGlyphs = collectTrackerGreenGlyphs(frame, line);
            Optional<TrackerGreenLinkSegment> target = resolveTrackerTargetNameSegment(lineGlyphs, line);
            target.ifPresent(targets::add);
        }
        if (!targets.isEmpty()) {
            return targets;
        }

        List<TrackerGreenGlyph> glyphs = collectTrackerGreenGlyphs(frame, band);
        List<TrackerGreenLinkSegment> segments = new ArrayList<>();
        int startX = -1;
        int endX = -1;
        int pixels = 0;
        TrackerGreenGlyph previous = null;
        for (int i = 0; i < glyphs.size(); i++) {
            TrackerGreenGlyph glyph = glyphs.get(i);
            boolean delimiter = isTrackerLinkDelimiter(glyph, pixels, remainingPixels(glyphs, i + 1));
            boolean largeGap = startX >= 0
                    && previous != null
                    && glyph.minX - previous.maxX - 1 >= TRACKER_LINK_SPLIT_GAP;
            if (delimiter) {
                addTrackerSegment(segments, startX, endX, band, pixels);
                startX = -1;
                endX = -1;
                pixels = 0;
                previous = glyph;
                continue;
            }
            if (largeGap) {
                addTrackerSegment(segments, startX, endX, band, pixels);
                startX = -1;
                endX = -1;
                pixels = 0;
            }
            if (startX < 0) {
                startX = glyph.minX;
            }
            endX = glyph.maxX;
            pixels += glyph.pixels;
            previous = glyph;
        }
        addTrackerSegment(segments, startX, endX, band, pixels);
        return segments;
    }

    private static List<ImagePreprocessor.GreenTextBand> splitTrackerGreenLines(BufferedImage frame,
                                                                                ImagePreprocessor.GreenTextBand band) {
        List<ImagePreprocessor.GreenTextBand> lines = new ArrayList<>();
        int startY = -1;
        int endY = -1;
        for (int y = band.minY(); y <= band.maxY(); y++) {
            int minX = Integer.MAX_VALUE;
            int maxX = -1;
            int pixels = 0;
            for (int x = band.minX(); x <= band.maxX(); x++) {
                if (ImagePreprocessor.isOptionGreen(frame.getRGB(x, y))) {
                    minX = Math.min(minX, x);
                    maxX = Math.max(maxX, x);
                    pixels++;
                }
            }
            if (pixels >= 4) {
                if (startY < 0) {
                    startY = y;
                }
                endY = y;
            } else if (startY >= 0) {
                lines.add(cropGreenBandToRows(frame, band, startY, endY));
                startY = -1;
                endY = -1;
            }
        }
        if (startY >= 0) {
            lines.add(cropGreenBandToRows(frame, band, startY, endY));
        }
        return lines;
    }

    private static ImagePreprocessor.GreenTextBand cropGreenBandToRows(BufferedImage frame,
                                                                       ImagePreprocessor.GreenTextBand band,
                                                                       int minY,
                                                                       int maxY) {
        int minX = Integer.MAX_VALUE;
        int maxX = -1;
        int pixels = 0;
        for (int y = minY; y <= maxY; y++) {
            for (int x = band.minX(); x <= band.maxX(); x++) {
                if (ImagePreprocessor.isOptionGreen(frame.getRGB(x, y))) {
                    minX = Math.min(minX, x);
                    maxX = Math.max(maxX, x);
                    pixels++;
                }
            }
        }
        return new ImagePreprocessor.GreenTextBand(minX, minY, maxX, maxY, pixels);
    }

    private static Optional<TrackerGreenLinkSegment> resolveTrackerTargetNameSegment(List<TrackerGreenGlyph> glyphs,
                                                                                    ImagePreprocessor.GreenTextBand line) {
        if (glyphs.isEmpty()) {
            return Optional.empty();
        }
        int endIndex = findProgressTailStart(glyphs).orElse(glyphs.size()) - 1;
        if (endIndex < 0) {
            return Optional.empty();
        }
        Optional<Integer> afterCoordinate = findGlyphAfterCoordinateRun(glyphs, endIndex);
        if (afterCoordinate.isPresent()) {
            return buildSegmentFromGlyphRange(glyphs, afterCoordinate.get(), endIndex, line);
        }
        Optional<Integer> progressStart = findProgressTailStart(glyphs);
        if (progressStart.isPresent()) {
            return buildSegmentFromGlyphRange(glyphs, 0, progressStart.get() - 1, line);
        }
        return Optional.empty();
    }

    private static Optional<Integer> findProgressTailStart(List<TrackerGreenGlyph> glyphs) {
        if (glyphs.size() < 2) {
            return Optional.empty();
        }
        TrackerGreenGlyph last = glyphs.get(glyphs.size() - 1);
        TrackerGreenGlyph beforeLast = glyphs.get(glyphs.size() - 2);
        int minX = beforeLast.minX;
        int maxX = last.maxX;
        int pixels = beforeLast.pixels + last.pixels;
        if (maxX - minX + 1 <= 24 && pixels <= 80 && beforeLast.width() <= TRACKER_COORD_GLYPH_MAX_WIDTH) {
            return Optional.of(glyphs.size() - 2);
        }
        if (last.width() <= 18 && last.pixels <= 70) {
            return Optional.of(glyphs.size() - 1);
        }
        return Optional.empty();
    }

    private static Optional<Integer> findGlyphAfterCoordinateRun(List<TrackerGreenGlyph> glyphs, int endIndex) {
        int bestAfter = -1;
        int runStart = -1;
        for (int i = 0; i <= endIndex; i++) {
            boolean narrow = glyphs.get(i).width() <= TRACKER_COORD_GLYPH_MAX_WIDTH;
            if (narrow) {
                if (runStart < 0) {
                    runStart = i;
                }
                continue;
            }
            if (runStart >= 0 && i - runStart >= TRACKER_COORD_GLYPH_MIN_RUN && i <= endIndex) {
                bestAfter = i;
            }
            runStart = -1;
        }
        if (runStart >= 0 && endIndex + 1 - runStart >= TRACKER_COORD_GLYPH_MIN_RUN
                && endIndex + 1 < glyphs.size()) {
            bestAfter = endIndex + 1;
        }
        return bestAfter >= 0 ? Optional.of(bestAfter) : Optional.empty();
    }

    private static Optional<TrackerGreenLinkSegment> buildSegmentFromGlyphRange(List<TrackerGreenGlyph> glyphs,
                                                                                int startIndex,
                                                                                int endIndex,
                                                                                ImagePreprocessor.GreenTextBand line) {
        if (startIndex < 0 || endIndex < startIndex || endIndex >= glyphs.size()) {
            return Optional.empty();
        }
        int minX = glyphs.get(startIndex).minX;
        int maxX = glyphs.get(endIndex).maxX;
        int pixels = 0;
        for (int i = startIndex; i <= endIndex; i++) {
            pixels += glyphs.get(i).pixels;
        }
        if (pixels < TRACKER_LINK_MIN_PIXELS) {
            return Optional.empty();
        }
        return Optional.of(new TrackerGreenLinkSegment(minX, line.minY(), maxX, line.maxY(), pixels));
    }

    private static List<TrackerGreenGlyph> collectTrackerGreenGlyphs(BufferedImage frame,
                                                                     ImagePreprocessor.GreenTextBand band) {
        List<TrackerGreenGlyph> glyphs = new ArrayList<>();
        int startX = -1;
        int endX = -1;
        int pixels = 0;
        for (int x = band.minX(); x <= band.maxX(); x++) {
            int columnPixels = 0;
            for (int y = band.minY(); y <= band.maxY(); y++) {
                if (ImagePreprocessor.isOptionGreen(frame.getRGB(x, y))) {
                    columnPixels++;
                }
            }
            if (columnPixels > 0) {
                if (startX < 0) {
                    startX = x;
                }
                endX = x;
                pixels += columnPixels;
            } else if (startX >= 0) {
                glyphs.add(new TrackerGreenGlyph(startX, endX, pixels));
                startX = -1;
                endX = -1;
                pixels = 0;
            }
        }
        if (startX >= 0) {
            glyphs.add(new TrackerGreenGlyph(startX, endX, pixels));
        }
        return glyphs;
    }

    private static boolean isTrackerLinkDelimiter(TrackerGreenGlyph glyph, int leftPixels, int rightPixels) {
        return glyph.width() <= TRACKER_LINK_DELIMITER_MAX_WIDTH
                && glyph.pixels <= TRACKER_LINK_DELIMITER_MAX_PIXELS
                && leftPixels >= TRACKER_LINK_MIN_PIXELS
                && rightPixels >= TRACKER_LINK_MIN_PIXELS;
    }

    private static int remainingPixels(List<TrackerGreenGlyph> glyphs, int fromIndex) {
        int total = 0;
        for (int i = fromIndex; i < glyphs.size(); i++) {
            total += glyphs.get(i).pixels;
        }
        return total;
    }

    private static void addTrackerSegment(List<TrackerGreenLinkSegment> segments,
                                          int startX,
                                          int endX,
                                          ImagePreprocessor.GreenTextBand band,
                                          int pixels) {
        if (pixels < TRACKER_LINK_MIN_PIXELS || endX < startX) {
            return;
        }
        segments.add(new TrackerGreenLinkSegment(startX, band.minY(), endX, band.maxY(), pixels));
    }

    private static Point resolveTrackerGreenClickPoint(BufferedImage image, TrackerGreenLinkSegment segment) {
        return new Point(segmentCenterX(segment), segmentCenterY(segment));
    }

    private static int segmentCenterX(TrackerGreenLinkSegment segment) {
        return (segment.minX + segment.maxX) / 2;
    }

    private static int segmentCenterY(TrackerGreenLinkSegment segment) {
        return (segment.minY + segment.maxY) / 2;
    }

    private static int countGreenNear(BufferedImage image, Point click, int radius) {
        int count = 0;
        for (int y = Math.max(0, click.y - radius); y <= Math.min(image.getHeight() - 1, click.y + radius); y++) {
            for (int x = Math.max(0, click.x - radius); x <= Math.min(image.getWidth() - 1, click.x + radius); x++) {
                if (ImagePreprocessor.isOptionGreen(image.getRGB(x, y))) {
                    count++;
                }
            }
        }
        return count;
    }

    private static void saveFailureImage(Path sample, ReplayResult result) {
        saveAnnotatedImage(sample, result, FAILURE_DIR, "_failure.png");
    }

    private static void saveOutputImage(Path sample, ReplayResult result) {
        saveAnnotatedImage(sample, result, OUTPUT_DIR, "_output.png");
    }

    private static void saveRejectedImage(Path sample, ReplayResult result) {
        saveAnnotatedImage(sample, result, REJECTED_DIR, "_rejected.png");
    }

    private static void saveAnnotatedImage(Path sample, ReplayResult result, Path outputDir, String suffix) {
        try {
            BufferedImage image = ImageIO.read(sample.toFile());
            if (image == null) {
                return;
            }
            Graphics2D g = image.createGraphics();
            g.setStroke(new BasicStroke(1));
            if (result.selected != null) {
                g.setColor(Color.ORANGE);
                g.drawRect(result.selected.minX, result.selected.minY,
                        result.selected.width(), result.selected.maxY - result.selected.minY + 1);
            }
            if (result.clickPoint != null) {
                g.setColor(Color.RED);
                g.fillOval(result.clickPoint.x - 2, result.clickPoint.y - 2, 5, 5);
            }
            g.dispose();
            String safeName = sample.getFileName().toString().replace(".png", suffix);
            ImageIO.write(image, "png", outputDir.resolve(safeName).toFile());
        } catch (IOException ignored) {
        }
    }

    private static void clearPngFiles(Path directory) throws IOException {
        if (!Files.isDirectory(directory)) {
            return;
        }
        try (Stream<Path> stream = Files.list(directory)) {
            for (Path path : stream.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().toLowerCase().endsWith(".png"))
                    .toList()) {
                Files.deleteIfExists(path);
            }
        }
    }

    private static String bandText(ImagePreprocessor.GreenTextBand band) {
        return "(%d,%d)-(%d,%d):%d".formatted(band.minX(), band.minY(), band.maxX(), band.maxY(), band.pixels());
    }

    private static String pointText(Point point) {
        return point == null ? "" : "(%d,%d)".formatted(point.x, point.y);
    }

    private record TrackerGreenGlyph(int minX, int maxX, int pixels) {
        int width() {
            return maxX - minX + 1;
        }
    }

    private record TrackerGreenLinkSegment(int minX, int minY, int maxX, int maxY, int pixels) {
        int width() {
            return maxX - minX + 1;
        }
    }

    private enum ReplayStatus {
        OK,
        SKIP,
        FAIL
    }

    private record ReplayResult(Path file,
                                ReplayStatus status,
                                String warning,
                                int width,
                                int height,
                                String band,
                                String segments,
                                TrackerGreenLinkSegment selected,
                                Point clickPoint,
                                int greenNearClick,
                                String reason) {

        static ReplayResult ok(Path file,
                               int width,
                               int height,
                               String band,
                               String segments,
                               TrackerGreenLinkSegment selected,
                               Point click,
                               int greenNearClick,
                               String warning) {
            return new ReplayResult(file, ReplayStatus.OK, warning, width, height, band, segments,
                    selected, click, greenNearClick, "");
        }

        static ReplayResult skip(Path file,
                                 int width,
                                 int height,
                                 String band,
                                 String segments,
                                 String reason) {
            return new ReplayResult(file, ReplayStatus.SKIP, "", width, height, band, segments,
                    null, null, 0, reason);
        }

        static ReplayResult fail(Path file,
                                 int width,
                                 int height,
                                 String band,
                                 String segments,
                                 TrackerGreenLinkSegment selected,
                                 Point click,
                                 int greenNearClick,
                                 String reason) {
            return new ReplayResult(file, ReplayStatus.FAIL, "", width, height, band, segments,
                    selected, click, greenNearClick, reason);
        }

        String toCsv() {
            return "%s,%s,%s,%d,%d,\"%s\",\"%s\",\"%s\",\"%s\",%d,\"%s\"".formatted(
                    status,
                    escape(warning),
                    escape(file.toString()),
                    width,
                    height,
                    escape(band),
                    escape(segments),
                    escape(selected == null ? "" : selected.toString()),
                    escape(pointText(clickPoint)),
                    greenNearClick,
                    escape(reason));
        }

        private static String escape(String value) {
            return value == null ? "" : value.replace("\"", "\"\"");
        }

    }
}
