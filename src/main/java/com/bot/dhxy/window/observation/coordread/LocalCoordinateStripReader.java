package com.bot.dhxy.window.observation.coordread;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Mechanical client-side port of the cloud MiniMapRecognizer coordinate-digit pipeline
 * (dhxy-cloud-brain com.yueyunfe.dhxy.cloudbrain.MiniMapRecognizer), trimmed to the
 * bracketed "[x,y]" digit-template path only. Map-label recognition and the OCR fallback
 * were removed; everything on the digit path is byte-faithful to the cloud source,
 * including ImageAlgorithms.cleanCoordinateText/binary inlined as private statics.
 */
public final class LocalCoordinateStripReader {

    private static final int CHAR_PAD = 1;
    private static final int MAX_COORD_DIGITS = 3;
    private static final int COORD_BRACKET_MIN_WIDTH = 30;
    private static final int COORD_BRACKET_MAX_WIDTH = 80;
    private static final int MAX_DIGIT_RUNS_TO_PARTITION = 8;
    private static final double TEMPLATE_MATCH_THRESHOLD = 0.45d;
    private static final List<DigitTemplate> DIGIT_TEMPLATES = readDigitTemplates();

    static {
        // 审查修正：字模静默加载失败会退化成"永远不可读"且无任何日志——启动即自报数量。
        org.slf4j.Logger initLog = org.slf4j.LoggerFactory.getLogger(LocalCoordinateStripReader.class);
        if (DIGIT_TEMPLATES.size() != 10) {
            initLog.error("coordinate digit templates incomplete: loaded={} expected=10 "
                    + "(templates/coord_digits missing from classpath? value stability will report "
                    + "STRIP_UNAVAILABLE forever)", DIGIT_TEMPLATES.size());
        } else {
            initLog.info("coordinate digit templates loaded: count={}", DIGIT_TEMPLATES.size());
        }
    }

    private LocalCoordinateStripReader() {
    }

    /** One value read of the raw coordinate strip. */
    public record Reading(boolean valid, int x, int y, double score) {
        public static Reading invalid() {
            return new Reading(false, 0, 0, 0.0d);
        }
    }

    public static Reading read(BufferedImage rawStrip) {
        if (rawStrip == null) {
            return Reading.invalid();
        }
        BufferedImage clean = cleanCoordinateText(rawStrip);
        try {
            CoordinateRecognition c = recognizeCoordinate(clean);
            if (c.coordinate().isEmpty()) {
                return Reading.invalid();
            }
            Point p = c.coordinate().get();
            return new Reading(true, p.x, p.y, c.score());
        } finally {
            clean.flush();
        }
    }

    private static CoordinateRecognition recognizeCoordinate(BufferedImage clean) {
        List<GlyphBox> glyphs = segmentGlyphs(clean);
        if (glyphs.isEmpty()) {
            return new CoordinateRecognition(Optional.empty(), List.of(), null, 0.0d);
        }
        Optional<BracketSpan> span = findBracketSpan(clean, glyphs);
        if (span.isEmpty()) {
            return new CoordinateRecognition(Optional.empty(), glyphs, null, 0.0d);
        }
        Optional<GlyphBox> comma = findCommaGlyph(clean, glyphs, span.get());
        if (comma.isEmpty()) {
            return new CoordinateRecognition(Optional.empty(), glyphs, span.get(), 0.0d);
        }

        DigitRecognition leftDigits = recognizeDigitRange(clean,
                span.get().leftMaxX + 2, comma.get().minX - 1, span.get().minY - 1, span.get().maxY + 1);
        DigitRecognition rightDigits = recognizeDigitRange(clean,
                comma.get().maxX + 1, span.get().rightMinX - 2, span.get().minY - 1, span.get().maxY + 1);
        if (!isPlausibleCoordinateSide(leftDigits) || !isPlausibleCoordinateSide(rightDigits)) {
            return new CoordinateRecognition(Optional.empty(), glyphs, span.get(), 0.0d);
        }
        try {
            int x = Integer.parseInt(leftDigits.text());
            int y = Integer.parseInt(rightDigits.text());
            if (x < 0 || x > 999 || y < 0 || y > 999) {
                return new CoordinateRecognition(Optional.empty(), glyphs, span.get(), 0.0d);
            }
            return new CoordinateRecognition(Optional.of(new Point(x, y)), glyphs, span.get(),
                    Math.max(0.0d, (leftDigits.score() + rightDigits.score()) / 2.0d));
        } catch (NumberFormatException e) {
            return new CoordinateRecognition(Optional.empty(), glyphs, span.get(), 0.0d);
        }
    }

    private static List<GlyphBox> segmentGlyphs(BufferedImage clean) {
        boolean[][] visited = new boolean[clean.getHeight()][clean.getWidth()];
        List<GlyphBox> glyphs = new ArrayList<>();
        for (int y = 0; y < clean.getHeight(); y++) {
            for (int x = 0; x < clean.getWidth(); x++) {
                if (visited[y][x] || !isWhite(clean, x, y)) {
                    continue;
                }
                GlyphBox box = floodFill(clean, visited, x, y);
                if (isUsefulGlyph(box)) {
                    glyphs.add(box.expand(clean.getWidth(), clean.getHeight(), CHAR_PAD));
                }
            }
        }
        glyphs.sort(Comparator.comparingInt(g -> g.minX));
        return glyphs;
    }

    private static GlyphBox floodFill(BufferedImage image, boolean[][] visited, int startX, int startY) {
        List<int[]> stack = new ArrayList<>();
        stack.add(new int[]{startX, startY});
        visited[startY][startX] = true;
        GlyphBox box = new GlyphBox(startX, startY, startX, startY);
        for (int i = 0; i < stack.size(); i++) {
            int[] point = stack.get(i);
            int x = point[0];
            int y = point[1];
            box.include(x, y);
            for (int dy = -1; dy <= 1; dy++) {
                for (int dx = -1; dx <= 1; dx++) {
                    if (Math.abs(dx) + Math.abs(dy) != 1) {
                        continue;
                    }
                    int nx = x + dx;
                    int ny = y + dy;
                    if (nx < 0 || ny < 0 || nx >= image.getWidth() || ny >= image.getHeight()) {
                        continue;
                    }
                    if (!visited[ny][nx] && isWhite(image, nx, ny)) {
                        visited[ny][nx] = true;
                        stack.add(new int[]{nx, ny});
                    }
                }
            }
        }
        return box;
    }

    private static boolean isUsefulGlyph(GlyphBox box) {
        return box.width() >= 1
                && box.height() >= 2
                && box.pixelCount >= 2
                && box.width() <= 18
                && box.height() <= 18;
    }

    private static Optional<BracketSpan> findBracketSpan(BufferedImage clean, List<GlyphBox> glyphs) {
        List<GlyphBox> candidates = glyphs.stream()
                .filter(g -> g.width() <= 6 && g.height() >= 8 && g.height() <= 16 && g.pixelCount >= 8)
                .sorted(Comparator.comparingInt(g -> g.minX))
                .toList();
        BracketSpan best = null;
        double bestScore = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < candidates.size(); i++) {
            for (int j = i + 1; j < candidates.size(); j++) {
                GlyphBox left = candidates.get(i);
                GlyphBox right = candidates.get(j);
                int width = right.maxX - left.minX + 1;
                if (width < COORD_BRACKET_MIN_WIDTH || width > COORD_BRACKET_MAX_WIDTH) {
                    continue;
                }
                BracketSpan span = new BracketSpan(left.minX, left.maxX, right.minX, right.maxX,
                        Math.min(left.minY, right.minY), Math.max(left.maxY, right.maxY));
                List<GlyphBox> commas = glyphs.stream()
                        .filter(g -> g.minX > left.maxX
                                && g.maxX < right.minX
                                && g.minY >= left.minY + 6
                                && g.width() <= 4
                                && g.height() <= 5)
                        .sorted(Comparator.comparingInt(g -> g.minX))
                        .toList();
                for (GlyphBox comma : commas) {
                    DigitRecognition leftDigits = recognizeDigitRange(clean,
                            span.leftMaxX + 2, comma.minX - 1, span.minY - 1, span.maxY + 1);
                    DigitRecognition rightDigits = recognizeDigitRange(clean,
                            comma.maxX + 1, span.rightMinX - 2, span.minY - 1, span.maxY + 1);
                    if (!isPlausibleCoordinateSide(leftDigits) || !isPlausibleCoordinateSide(rightDigits)) {
                        continue;
                    }
                    int digitCount = leftDigits.text().length() + rightDigits.text().length();
                    double score = digitCount * 0.12d
                            + leftDigits.score() + rightDigits.score()
                            - Math.abs(comma.minX - (span.minX + width * 0.42d)) * 0.005d;
                    if (score > bestScore) {
                        best = span;
                        bestScore = score;
                    }
                }
            }
        }
        return Optional.ofNullable(best);
    }

    private static Optional<GlyphBox> findCommaGlyph(BufferedImage clean, List<GlyphBox> glyphs, BracketSpan span) {
        int expectedX = (int) Math.round(span.minX + (span.maxX - span.minX) * 0.48d);
        List<GlyphBox> candidates = glyphs.stream()
                .filter(g -> g.minX > span.leftMaxX + 1 && g.maxX < span.rightMinX - 1)
                .filter(g -> g.minY >= span.minY + 6 && g.width() <= 4 && g.height() <= 5)
                .sorted(Comparator.comparingInt(g -> g.minX))
                .toList();
        GlyphBox best = null;
        double bestScore = Double.NEGATIVE_INFINITY;
        for (GlyphBox comma : candidates) {
            DigitRecognition leftDigits = recognizeDigitRange(clean,
                    span.leftMaxX + 2, comma.minX - 1, span.minY - 1, span.maxY + 1);
            DigitRecognition rightDigits = recognizeDigitRange(clean,
                    comma.maxX + 1, span.rightMinX - 2, span.minY - 1, span.maxY + 1);
            if (!isPlausibleCoordinateSide(leftDigits) || !isPlausibleCoordinateSide(rightDigits)) {
                continue;
            }
            double score = leftDigits.score() + rightDigits.score()
                    - Math.abs(comma.minX - expectedX) * 0.001d;
            if (score > bestScore) {
                best = comma;
                bestScore = score;
            }
        }
        return Optional.ofNullable(best);
    }

    private static boolean isPlausibleCoordinateSide(DigitRecognition recognition) {
        int len = recognition.text().length();
        return len >= 1
                && len <= MAX_COORD_DIGITS
                && countUnknowns(recognition.text()) == 0
                && recognition.score() >= TEMPLATE_MATCH_THRESHOLD;
    }

    private static DigitRecognition recognizeDigitRange(BufferedImage clean, int startX, int endX, int startY, int endY) {
        List<GlyphBox> runs = projectionDigitBoxes(clean, startX, endX, startY, endY);
        if (runs.isEmpty()) {
            return new DigitRecognition("", List.of(), 0.0d);
        }
        if (runs.size() > MAX_DIGIT_RUNS_TO_PARTITION) {
            runs = mergeClosestRunsUntilWithinLimit(runs, MAX_DIGIT_RUNS_TO_PARTITION);
        }
        DigitRecognition best = null;
        int maxDigits = Math.min(MAX_COORD_DIGITS, runs.size());
        int splitSlots = Math.max(0, runs.size() - 1);
        int maxMask = 1 << splitSlots;
        for (int digitCount = 1; digitCount <= maxDigits; digitCount++) {
            for (int mask = 0; mask < maxMask; mask++) {
                if (Integer.bitCount(mask) != digitCount - 1) {
                    continue;
                }
                DigitRecognition candidate = recognizePartition(clean, runs, mask);
                if (isBetterDigitRecognition(candidate, best)) {
                    best = candidate;
                }
            }
        }
        return best == null ? new DigitRecognition("", List.of(), 0.0d) : best;
    }

    private static List<GlyphBox> projectionDigitBoxes(BufferedImage clean, int startX, int endX, int startY, int endY) {
        int left = Math.max(0, Math.min(startX, endX));
        int right = Math.min(clean.getWidth() - 1, Math.max(startX, endX));
        int top = Math.max(0, Math.min(startY, endY));
        int bottom = Math.min(clean.getHeight() - 1, Math.max(startY, endY));
        List<GlyphBox> runs = new ArrayList<>();
        int runStart = -1;
        for (int x = left; x <= right; x++) {
            boolean hasWhite = false;
            for (int y = top; y <= bottom; y++) {
                if (isWhite(clean, x, y)) {
                    hasWhite = true;
                    break;
                }
            }
            if (hasWhite && runStart < 0) {
                runStart = x;
            } else if (!hasWhite && runStart >= 0) {
                runs.add(trimBoxToForeground(clean, runStart, x - 1, top, bottom));
                runStart = -1;
            }
        }
        if (runStart >= 0) {
            runs.add(trimBoxToForeground(clean, runStart, right, top, bottom));
        }
        return runs.stream()
                .filter(box -> box.width() >= 1 && box.height() >= 5)
                .sorted(Comparator.comparingInt(box -> box.minX))
                .toList();
    }

    private static DigitRecognition recognizePartition(BufferedImage clean, List<GlyphBox> runs, int splitMask) {
        StringBuilder text = new StringBuilder();
        List<GlyphBox> boxes = new ArrayList<>();
        double totalScore = 0.0d;
        int unknownCount = 0;
        int start = 0;
        for (int i = 0; i < runs.size(); i++) {
            boolean splitAfter = i == runs.size() - 1 || ((splitMask & (1 << i)) != 0);
            if (!splitAfter) {
                continue;
            }
            GlyphBox merged = mergeRuns(runs, start, i);
            BufferedImage glyphImage = trimToForeground(crop(clean, merged), 1);
            GlyphMatch match = recognizeOneGlyphScored(glyphImage);
            glyphImage.flush();
            text.append(match.symbol());
            boxes.add(merged);
            totalScore += match.score();
            if ("?".equals(match.symbol())) {
                unknownCount++;
            }
            start = i + 1;
        }
        double averageScore = boxes.isEmpty() ? 0.0d : totalScore / boxes.size();
        return new DigitRecognition(text.toString(), boxes, averageScore - unknownCount * 0.5d);
    }

    private static GlyphMatch recognizeOneGlyphScored(BufferedImage glyphImage) {
        double bestScore = 0.0d;
        String bestSymbol = null;
        WhitePixelSet glyphPixels = collectWhitePixels(glyphImage);
        for (DigitTemplate template : DIGIT_TEMPLATES) {
            double score = foregroundSimilarity(glyphPixels, template.pixels());
            if (score > bestScore) {
                bestScore = score;
                bestSymbol = template.symbol();
            }
        }
        if (bestSymbol == null || bestScore < TEMPLATE_MATCH_THRESHOLD) {
            return new GlyphMatch("?", bestScore);
        }
        return new GlyphMatch(bestSymbol, bestScore);
    }

    private static boolean isBetterDigitRecognition(DigitRecognition candidate, DigitRecognition best) {
        if (best == null) {
            return true;
        }
        int candidateUnknowns = countUnknowns(candidate.text());
        int bestUnknowns = countUnknowns(best.text());
        if (candidateUnknowns != bestUnknowns) {
            return candidateUnknowns < bestUnknowns;
        }
        return candidate.score() > best.score();
    }

    private static List<GlyphBox> mergeClosestRunsUntilWithinLimit(List<GlyphBox> runs, int limit) {
        List<GlyphBox> merged = new ArrayList<>(runs);
        while (merged.size() > limit) {
            int bestIndex = 0;
            int bestGap = Integer.MAX_VALUE;
            for (int i = 0; i < merged.size() - 1; i++) {
                int gap = Math.max(0, merged.get(i + 1).minX - merged.get(i).maxX - 1);
                if (gap < bestGap) {
                    bestGap = gap;
                    bestIndex = i;
                }
            }
            GlyphBox combined = merged.get(bestIndex).copy();
            combined.include(merged.get(bestIndex + 1));
            merged.set(bestIndex, combined);
            merged.remove(bestIndex + 1);
        }
        return merged;
    }

    private static GlyphBox trimBoxToForeground(BufferedImage clean, int startX, int endX, int startY, int endY) {
        int minX = clean.getWidth();
        int minY = clean.getHeight();
        int maxX = -1;
        int maxY = -1;
        int count = 0;
        for (int y = startY; y <= endY; y++) {
            for (int x = startX; x <= endX; x++) {
                if (isWhite(clean, x, y)) {
                    minX = Math.min(minX, x);
                    minY = Math.min(minY, y);
                    maxX = Math.max(maxX, x);
                    maxY = Math.max(maxY, y);
                    count++;
                }
            }
        }
        if (maxX < minX || maxY < minY) {
            return new GlyphBox(startX, startY, endX, endY);
        }
        GlyphBox box = new GlyphBox(minX, minY, maxX, maxY);
        box.pixelCount = count;
        return box.expand(clean.getWidth(), clean.getHeight(), 1);
    }

    private static GlyphBox mergeRuns(List<GlyphBox> runs, int startInclusive, int endInclusive) {
        GlyphBox merged = runs.get(startInclusive).copy();
        for (int i = startInclusive + 1; i <= endInclusive; i++) {
            merged.include(runs.get(i));
        }
        return merged;
    }

    private static BufferedImage trimToForeground(BufferedImage source, int pad) {
        int minX = source.getWidth();
        int minY = source.getHeight();
        int maxX = -1;
        int maxY = -1;
        for (int y = 0; y < source.getHeight(); y++) {
            for (int x = 0; x < source.getWidth(); x++) {
                if (isWhite(source, x, y)) {
                    minX = Math.min(minX, x);
                    minY = Math.min(minY, y);
                    maxX = Math.max(maxX, x);
                    maxY = Math.max(maxY, y);
                }
            }
        }
        if (maxX < minX || maxY < minY) {
            return source;
        }
        GlyphBox box = new GlyphBox(
                Math.max(0, minX - pad),
                Math.max(0, minY - pad),
                Math.min(source.getWidth() - 1, maxX + pad),
                Math.min(source.getHeight() - 1, maxY + pad));
        BufferedImage trimmed = crop(source, box);
        source.flush();
        return trimmed;
    }

    private static double foregroundSimilarity(WhitePixelSet a, WhitePixelSet b) {
        if (a.whitePixels() == 0 || b.whitePixels() == 0) {
            return 0.0d;
        }
        double best = 0.0d;
        for (int dy = -2; dy <= 2; dy++) {
            for (int dx = -2; dx <= 2; dx++) {
                int overlap = 0;
                for (int i = 0; i < a.whitePixels(); i++) {
                    if (b.isWhite(a.xs()[i] + dx, a.ys()[i] + dy)) {
                        overlap++;
                    }
                }
                best = Math.max(best, (2.0d * overlap) / (a.whitePixels() + b.whitePixels()));
            }
        }
        double sizePenalty = Math.abs(a.width() - b.width()) * 0.08d
                + Math.abs(a.height() - b.height()) * 0.04d;
        return Math.max(0.0d, best - sizePenalty);
    }

    private static WhitePixelSet collectWhitePixels(BufferedImage image) {
        List<Point> points = new ArrayList<>();
        boolean[] mask = new boolean[image.getWidth() * image.getHeight()];
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                if (isWhite(image, x, y)) {
                    points.add(new Point(x, y));
                    mask[y * image.getWidth() + x] = true;
                }
            }
        }
        int[] xs = new int[points.size()];
        int[] ys = new int[points.size()];
        for (int i = 0; i < points.size(); i++) {
            xs[i] = points.get(i).x;
            ys[i] = points.get(i).y;
        }
        return new WhitePixelSet(image.getWidth(), image.getHeight(), mask, xs, ys);
    }

    private static BufferedImage crop(BufferedImage source, GlyphBox box) {
        BufferedImage out = new BufferedImage(box.width(), box.height(), BufferedImage.TYPE_BYTE_BINARY);
        Graphics2D g = out.createGraphics();
        try {
            g.drawImage(source, 0, 0, box.width(), box.height(),
                    box.minX, box.minY, box.maxX + 1, box.maxY + 1, null);
            return out;
        } finally {
            g.dispose();
        }
    }

    private static boolean isWhite(BufferedImage image, int x, int y) {
        return (image.getRGB(x, y) & 0x00FFFFFF) == 0x00FFFFFF;
    }

    private static int countUnknowns(String text) {
        int count = 0;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == '?') {
                count++;
            }
        }
        return count;
    }

    private static List<DigitTemplate> readDigitTemplates() {
        return StripTemplateMatcher.loadTemplates("templates/coord_digits", name -> !name.equals("comma.png"))
                .stream()
                .map(template -> {
                    BufferedImage clean = trimToForeground(cleanCoordinateText(template.image()), 1);
                    try {
                        return new DigitTemplate(template.name(), collectWhitePixels(clean));
                    } finally {
                        clean.flush();
                    }
                })
                .toList();
    }

    /** Inlined byte-faithful copy of cloud ImageAlgorithms.cleanCoordinateText. */
    private static BufferedImage cleanCoordinateText(BufferedImage raw) {
        return binary(raw, rgb -> {
            int r = (rgb >> 16) & 0xFF;
            int g = (rgb >> 8) & 0xFF;
            int b = rgb & 0xFF;
            int max = Math.max(r, Math.max(g, b));
            int min = Math.min(r, Math.min(g, b));
            float[] hsb = Color.RGBtoHSB(r, g, b, null);
            return max >= 145
                    && min >= 100
                    && hsb[1] <= 0.32f
                    && hsb[2] >= 0.56f
                    && (max - min) <= 85;
        }, false);
    }

    /** Inlined byte-faithful copy of cloud ImageAlgorithms.binary. */
    private static BufferedImage binary(BufferedImage source, PixelPredicate predicate, boolean blackForeground) {
        BufferedImage out = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_BYTE_BINARY);
        for (int y = 0; y < source.getHeight(); y++) {
            for (int x = 0; x < source.getWidth(); x++) {
                boolean hit = predicate.test(source.getRGB(x, y));
                out.setRGB(x, y, hit == blackForeground ? 0x000000 : 0xFFFFFF);
            }
        }
        return out;
    }

    private interface PixelPredicate {
        boolean test(int rgb);
    }

    private record CoordinateRecognition(Optional<Point> coordinate,
                                         List<GlyphBox> glyphs,
                                         BracketSpan bracketSpan,
                                         double score) {
    }

    private record DigitRecognition(String text, List<GlyphBox> boxes, double score) {
    }

    private record GlyphMatch(String symbol, double score) {
    }

    private record DigitTemplate(String symbol, WhitePixelSet pixels) {
    }

    private record BracketSpan(int minX, int leftMaxX, int rightMinX, int maxX, int minY, int maxY) {
    }

    private record WhitePixelSet(int width, int height, boolean[] mask, int[] xs, int[] ys) {
        private int whitePixels() {
            return xs.length;
        }

        private boolean isWhite(int x, int y) {
            return x >= 0 && y >= 0 && x < width && y < height && mask[y * width + x];
        }
    }

    private static final class GlyphBox {
        private int minX;
        private int minY;
        private int maxX;
        private int maxY;
        private int pixelCount;

        private GlyphBox(int minX, int minY, int maxX, int maxY) {
            this.minX = minX;
            this.minY = minY;
            this.maxX = maxX;
            this.maxY = maxY;
        }

        private int width() {
            return maxX - minX + 1;
        }

        private int height() {
            return maxY - minY + 1;
        }

        private void include(int x, int y) {
            minX = Math.min(minX, x);
            minY = Math.min(minY, y);
            maxX = Math.max(maxX, x);
            maxY = Math.max(maxY, y);
            pixelCount++;
        }

        private void include(GlyphBox other) {
            minX = Math.min(minX, other.minX);
            minY = Math.min(minY, other.minY);
            maxX = Math.max(maxX, other.maxX);
            maxY = Math.max(maxY, other.maxY);
            pixelCount += other.pixelCount;
        }

        private GlyphBox copy() {
            GlyphBox copy = new GlyphBox(minX, minY, maxX, maxY);
            copy.pixelCount = pixelCount;
            return copy;
        }

        private GlyphBox expand(int imageWidth, int imageHeight, int pad) {
            GlyphBox expanded = new GlyphBox(
                    Math.max(0, minX - pad),
                    Math.max(0, minY - pad),
                    Math.min(imageWidth - 1, maxX + pad),
                    Math.min(imageHeight - 1, maxY + pad));
            expanded.pixelCount = pixelCount;
            return expanded;
        }
    }
}
