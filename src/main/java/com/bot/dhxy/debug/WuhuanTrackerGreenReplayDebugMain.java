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
    private static final Path IMAGE_ROOT = ROOT.resolve("images/temp");
    private static final Path REPORT = ROOT.resolve("logs/wuhuan-tracker-green-replay.csv");
    private static final Path FAILURE_DIR = ROOT.resolve("images/temp/wuhuan_tracker_replay_failures");

    private static final int TRACKER_LINK_MIN_PIXELS = 20;
    private static final int TRACKER_LINK_SPLIT_GAP = 8;
    private static final int TRACKER_LINK_DELIMITER_MAX_WIDTH = 5;
    private static final int TRACKER_LINK_DELIMITER_MAX_PIXELS = 18;

    public static void main(String[] args) throws Exception {
        Files.createDirectories(REPORT.getParent());
        Files.createDirectories(FAILURE_DIR);

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
                } else {
                    failed++;
                    saveFailureImage(sample, result);
                }
                if (!result.warning.isBlank()) {
                    warned++;
                }
                writer.write(result.toCsv());
                writer.newLine();
            }
        }

        System.out.printf("WUHUAN_TRACKER_REPLAY samples=%d ok=%d skipped=%d failed=%d warned=%d report=%s failures=%s%n",
                samples.size(), ok, skipped, failed, warned, REPORT, FAILURE_DIR);
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
            boolean clickLooksValid = greenNearClick > 0
                    && click.x >= segment.minX + 1
                    && click.x <= segment.maxX - 1
                    && click.y >= segment.minY
                    && click.y <= segment.maxY;
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

    private static Optional<TrackerGreenLinkSegment> findTrackerPathingNameSegment(List<TrackerGreenLinkSegment> segments) {
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
        int[] primaryRun = resolvePrimaryTrackerGreenRun(image, segment.minX, segment.maxX, segment.minY, segment.maxY);
        int totalPixels = 0;
        long weightedX = 0L;
        for (int y = segment.minY; y <= segment.maxY; y++) {
            for (int x = primaryRun[0]; x <= primaryRun[1]; x++) {
                if (ImagePreprocessor.isOptionGreen(image.getRGB(x, y))) {
                    totalPixels++;
                    weightedX += x;
                }
            }
        }
        int x = totalPixels > 0 ? (int) Math.round(weightedX / (double) totalPixels)
                : (primaryRun[0] + primaryRun[1]) / 2;
        int y = (segment.minY + segment.maxY) / 2;
        return new Point(x, y);
    }

    private static int[] resolvePrimaryTrackerGreenRun(BufferedImage image, int localX1, int localX2, int localY1, int localY2) {
        int runStart = -1;
        int runEnd = -1;
        int bestStart = localX1;
        int bestEnd = localX2;
        int bestPixels = -1;
        int pixels = 0;
        for (int x = localX1; x <= localX2; x++) {
            int columnPixels = 0;
            for (int y = localY1; y <= localY2; y++) {
                if (ImagePreprocessor.isOptionGreen(image.getRGB(x, y))) {
                    columnPixels++;
                }
            }
            if (columnPixels > 0) {
                if (runStart < 0) {
                    runStart = x;
                }
                runEnd = x;
                pixels += columnPixels;
            } else if (runStart >= 0) {
                if (pixels > bestPixels) {
                    bestStart = runStart;
                    bestEnd = runEnd;
                    bestPixels = pixels;
                }
                break;
            }
        }
        if (runStart >= 0 && bestPixels < 0) {
            bestStart = runStart;
            bestEnd = runEnd;
        }
        return new int[]{bestStart, bestEnd};
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
        try {
            BufferedImage image = ImageIO.read(sample.toFile());
            if (image == null) {
                return;
            }
            Graphics2D g = image.createGraphics();
            g.setStroke(new BasicStroke(2));
            if (result.selected != null) {
                g.setColor(Color.ORANGE);
                g.drawRect(result.selected.minX, result.selected.minY,
                        result.selected.width(), result.selected.maxY - result.selected.minY + 1);
            }
            if (result.clickPoint != null) {
                g.setColor(Color.RED);
                g.drawLine(result.clickPoint.x - 5, result.clickPoint.y, result.clickPoint.x + 5, result.clickPoint.y);
                g.drawLine(result.clickPoint.x, result.clickPoint.y - 5, result.clickPoint.x, result.clickPoint.y + 5);
            }
            g.dispose();
            String safeName = sample.getFileName().toString().replace(".png", "_failure.png");
            ImageIO.write(image, "png", FAILURE_DIR.resolve(safeName).toFile());
        } catch (IOException ignored) {
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
