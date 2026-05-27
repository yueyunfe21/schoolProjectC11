package com.bot.dhxy.tools;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.Accessors;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
public class CoordinateDigitTemplateBuilder {

    private static final Path SAMPLE_DIR = Path.of("images", "temp", "coord_samples");
    private static final Path OUTPUT_DIR = Path.of("images", "temp", "coord_samples_out");
    private static final Path TEMPLATE_DIR = Path.of("images", "template", "coord_digits");
    private static final int CHAR_PAD = 1;
    private static final double TEMPLATE_MATCH_THRESHOLD = 0.45;
    private static final Pattern COORDINATE_FILE_NAME = Pattern.compile("(\\d{1,4})\\D+(\\d{1,4})");

    public static void main(String[] args) {
        new CoordinateDigitTemplateBuilder().buildTemplatesFromNamedSamples();
    }

    public void buildDebugGlyphsFromSamples() {
        if (!Files.exists(SAMPLE_DIR)) {
            log.warn("[coord-template] sample dir does not exist: {}", SAMPLE_DIR.toAbsolutePath());
            return;
        }

        try {
            Files.createDirectories(OUTPUT_DIR);
            for (Path sample : listSampleImages()) {
                processSample(sample);
            }
        } catch (Exception e) {
            log.error("[coord-template] failed to slice samples", e);
        }
    }

    public void buildTemplatesFromNamedSamples() {
        if (!Files.exists(SAMPLE_DIR)) {
            log.warn("[coord-template] sample dir does not exist: {}", SAMPLE_DIR.toAbsolutePath());
            return;
        }

        try {
            Files.createDirectories(OUTPUT_DIR);
            Files.createDirectories(TEMPLATE_DIR);
            Map<String, Path> written = new HashMap<>();
            for (Path sample : listSampleImages()) {
                Optional<String> expectedText = expectedTextFromFileName(sample);
                if (expectedText.isEmpty()) {
                    log.warn("[coord-template] skip sample without coordinate in file name: {}", sample.getFileName());
                    processSample(sample);
                    continue;
                }
                buildTemplateFromSample(sample, expectedText.get(), written);
            }
            for (Path sample : listSampleImages()) {
                expectedTextFromFileName(sample).ifPresent(expected -> verifySample(sample, expected));
            }
            log.info("[coord-template] generated symbols: {}", written.keySet());
        } catch (Exception e) {
            log.error("[coord-template] failed to build templates", e);
        }
    }

    private List<Path> listSampleImages() throws Exception {
        try (var stream = Files.list(SAMPLE_DIR)) {
            return stream
                    .filter(path -> {
                        String name = path.getFileName().toString().toLowerCase();
                        return name.endsWith(".png") || name.endsWith(".jpg") || name.endsWith(".jpeg");
                    })
                    .sorted()
                    .toList();
        }
    }

    private void processSample(Path sample) {
        try {
            BufferedImage raw = ImageIO.read(sample.toFile());
            if (raw == null) {
                log.warn("[coord-template] cannot read sample: {}", sample);
                return;
            }

            BufferedImage clean = cleanCoordinateText(raw);
            String baseName = stripExtension(sample.getFileName().toString());
            ImageIO.write(clean, "png", OUTPUT_DIR.resolve(baseName + "_clean.png").toFile());

            List<GlyphBox> glyphs = segmentGlyphs(clean);
            log.info("[coord-template] sliced sample={} glyphs={}", sample.getFileName(), glyphs.size());
            for (int i = 0; i < glyphs.size(); i++) {
                BufferedImage glyph = crop(clean, glyphs.get(i));
                ImageIO.write(glyph, "png", OUTPUT_DIR.resolve(baseName + "_glyph_" + String.format("%02d", i) + ".png").toFile());
                glyph.flush();
            }

            raw.flush();
            clean.flush();
        } catch (Exception e) {
            log.warn("[coord-template] failed to process sample: {}", sample, e);
        }
    }

    private void buildTemplateFromSample(Path sample, String expectedText, Map<String, Path> written) {
        try {
            BufferedImage raw = ImageIO.read(sample.toFile());
            if (raw == null) {
                log.warn("[coord-template] cannot read sample: {}", sample);
                return;
            }

            BufferedImage clean = cleanCoordinateText(raw);
            String baseName = stripExtension(sample.getFileName().toString());
            ImageIO.write(clean, "png", OUTPUT_DIR.resolve(baseName + "_clean.png").toFile());

            List<GlyphBox> glyphs = segmentGlyphs(clean);
            for (int i = 0; i < glyphs.size(); i++) {
                BufferedImage glyph = crop(clean, glyphs.get(i));
                ImageIO.write(glyph, "png", OUTPUT_DIR.resolve(baseName + "_glyph_" + String.format("%02d", i) + ".png").toFile());
                glyph.flush();
            }

            Optional<BracketSpan> span = findBracketSpan(glyphs);
            if (span.isEmpty()) {
                log.warn("[coord-template] sample={} expected='{}' glyphs={} bracket span not found",
                        sample.getFileName(), expectedText, glyphs.size());
                raw.flush();
                clean.flush();
                return;
            }

            List<LabeledGlyph> labeledGlyphs = extractCoordinateGlyphs(clean, glyphs, span.get(), expectedText);
            if (labeledGlyphs.size() != expectedText.length()) {
                log.warn("[coord-template] sample={} expected='{}' extracted={}",
                        sample.getFileName(), expectedText, labeledGlyphs.size());
                raw.flush();
                clean.flush();
                return;
            }

            for (LabeledGlyph labeledGlyph : labeledGlyphs) {
                String symbol = symbolName(labeledGlyph.symbol());
                BufferedImage glyph = trimToForeground(crop(clean, labeledGlyph.box()), 1);
                Path template = TEMPLATE_DIR.resolve(symbol + ".png");
                if (!Files.exists(template)) {
                    ImageIO.write(glyph, "png", template.toFile());
                }
                ImageIO.write(glyph, "png", OUTPUT_DIR.resolve(baseName + "_template_" + symbol + "_" + labeledGlyph.index() + ".png").toFile());
                written.put(symbol, template);
                glyph.flush();
            }

            log.info("[coord-template] sample={} expected='{}' span={} templates refreshed",
                    sample.getFileName(), expectedText, span.get());
            raw.flush();
            clean.flush();
        } catch (Exception e) {
            log.warn("[coord-template] failed to build template from sample: {}", sample, e);
        }
    }

    private void verifySample(Path sample, String expectedText) {
        try {
            BufferedImage raw = ImageIO.read(sample.toFile());
            if (raw == null) {
                log.warn("[coord-template] cannot read sample for verify: {}", sample);
                return;
            }

            BufferedImage clean = cleanCoordinateText(raw);
            Optional<BracketSpan> span = findBracketSpan(segmentGlyphs(clean));
            if (span.isEmpty()) {
                log.info("[coord-template] verify sample={} expected='{}' recognized='' ok=false",
                        sample.getFileName(), expectedText);
                raw.flush();
                clean.flush();
                return;
            }
            List<LabeledGlyph> labeledGlyphs = extractCoordinateGlyphs(clean, segmentGlyphs(clean), span.get(), expectedText);
            StringBuilder recognized = new StringBuilder();
            for (LabeledGlyph labeledGlyph : labeledGlyphs) {
                BufferedImage glyph = trimToForeground(crop(clean, labeledGlyph.box()), 1);
                String symbol = labeledGlyph.symbol() == ','
                        ? recognizeOneGlyph(glyph, List.of("comma"))
                        : recognizeOneGlyph(glyph, List.of("0", "1", "2", "3", "4", "5", "6", "7", "8", "9"));
                recognized.append(symbol == null ? "?" : symbol);
                glyph.flush();
            }
            boolean ok = expectedText.equals(recognized.toString());
            log.info("[coord-template] verify sample={} expected='{}' recognized='{}' ok={}",
                    sample.getFileName(), expectedText, recognized, ok);
            raw.flush();
            clean.flush();
        } catch (Exception e) {
            log.warn("[coord-template] failed to verify sample: {}", sample, e);
        }
    }

    private BufferedImage cleanCoordinateText(BufferedImage raw) {
        BufferedImage clean = new BufferedImage(raw.getWidth(), raw.getHeight(), BufferedImage.TYPE_BYTE_BINARY);
        for (int y = 0; y < raw.getHeight(); y++) {
            for (int x = 0; x < raw.getWidth(); x++) {
                int rgb = raw.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;
                clean.setRGB(x, y, isCoordinateTextPixel(r, g, b) ? 0xFFFFFF : 0x000000);
            }
        }
        return clean;
    }

    private boolean isCoordinateTextPixel(int r, int g, int b) {
        int max = Math.max(r, Math.max(g, b));
        int min = Math.min(r, Math.min(g, b));
        float[] hsb = Color.RGBtoHSB(r, g, b, null);
        return max >= 145
                && min >= 100
                && hsb[1] <= 0.32f
                && hsb[2] >= 0.56f
                && (max - min) <= 85;
    }

    private List<GlyphBox> segmentGlyphs(BufferedImage clean) {
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

    private GlyphBox floodFill(BufferedImage image, boolean[][] visited, int startX, int startY) {
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

    private boolean isUsefulGlyph(GlyphBox box) {
        int w = box.width();
        int h = box.height();
        return w >= 1 && h >= 2 && box.pixelCount >= 2 && w <= 18 && h <= 18;
    }

    private List<GlyphBox> pickCoordinateGlyphs(List<GlyphBox> glyphs, int expectedLength) {
        List<GlyphBox> candidates = glyphs.stream()
                .filter(this::isLikelyCoordinateGlyph)
                .sorted(Comparator.comparingInt(g -> g.minX))
                .toList();
        if (candidates.size() <= expectedLength) {
            return candidates;
        }

        int bestStart = -1;
        double bestScore = Double.NEGATIVE_INFINITY;
        for (int start = 0; start <= candidates.size() - expectedLength; start++) {
            List<GlyphBox> window = candidates.subList(start, start + expectedLength);
            double score = scoreCoordinateWindow(window);
            if (score > bestScore) {
                bestScore = score;
                bestStart = start;
            }
        }
        if (bestStart < 0) {
            return List.of();
        }
        return new ArrayList<>(candidates.subList(bestStart, bestStart + expectedLength));
    }

    private Optional<BracketSpan> findBracketSpan(List<GlyphBox> glyphs) {
        List<GlyphBox> candidates = glyphs.stream()
                .filter(g -> g.width() <= 6 && g.height() >= 8 && g.height() <= 16 && g.pixelCount >= 8)
                .sorted(Comparator.comparingInt(g -> g.minX))
                .toList();
        BracketSpan best = null;
        int bestWidth = 0;
        for (int i = 0; i < candidates.size(); i++) {
            for (int j = i + 1; j < candidates.size(); j++) {
                GlyphBox left = candidates.get(i);
                GlyphBox right = candidates.get(j);
                int width = right.maxX - left.minX + 1;
                if (width < 35 || width > 80) {
                    continue;
                }
                boolean hasComma = glyphs.stream().anyMatch(g ->
                        g.minX > left.maxX
                                && g.maxX < right.minX
                                && g.minY >= left.minY + 6
                                && g.width() <= 3
                                && g.height() <= 4);
                if (hasComma && width > bestWidth) {
                    int minY = Math.min(left.minY, right.minY);
                    int maxY = Math.max(left.maxY, right.maxY);
                    best = new BracketSpan(left.minX, right.maxX, minY, maxY);
                    bestWidth = width;
                }
            }
        }
        return Optional.ofNullable(best);
    }

    private List<LabeledGlyph> extractCoordinateGlyphs(BufferedImage clean, List<GlyphBox> glyphs, BracketSpan span, String expectedText) {
        int commaIndex = expectedText.indexOf(',');
        if (commaIndex < 0) {
            return List.of();
        }
        String leftText = expectedText.substring(0, commaIndex);
        String rightText = expectedText.substring(commaIndex + 1);
        Optional<GlyphBox> comma = findCommaGlyph(glyphs, span, leftText.length(), rightText.length());
        if (comma.isEmpty()) {
            return List.of();
        }

        List<GlyphBox> leftDigits = projectionDigitBoxes(clean,
                span.minX + 5, comma.get().minX - 1, span.minY - 1, span.maxY + 1, leftText.length());
        List<GlyphBox> rightDigits = projectionDigitBoxes(clean,
                comma.get().maxX + 1, span.maxX - 5, span.minY - 1, span.maxY + 1, rightText.length());
        log.info("[coord-template] extract expected='{}' span={} comma={} leftDigits={} rightDigits={}",
                expectedText, span, comma.get(), leftDigits.size(), rightDigits.size());
        if (leftDigits.size() != leftText.length() || rightDigits.size() != rightText.length()) {
            return List.of();
        }

        List<LabeledGlyph> result = new ArrayList<>();
        int index = 0;
        for (int i = 0; i < leftText.length(); i++) {
            result.add(new LabeledGlyph(leftText.charAt(i), index++, leftDigits.get(i)));
        }
        result.add(new LabeledGlyph(',', index++, comma.get()));
        for (int i = 0; i < rightText.length(); i++) {
            result.add(new LabeledGlyph(rightText.charAt(i), index++, rightDigits.get(i)));
        }
        return result;
    }

    private Optional<GlyphBox> findCommaGlyph(List<GlyphBox> glyphs, BracketSpan span, int leftDigits, int rightDigits) {
        double expectedRatio = (leftDigits + 1.0) / (leftDigits + rightDigits + 3.0);
        int expectedX = (int) Math.round(span.minX + (span.maxX - span.minX) * expectedRatio);
        return glyphs.stream()
                .filter(g -> g.minX > span.minX + 5 && g.maxX < span.maxX - 5)
                .filter(g -> g.minY >= span.minY + 6 && g.width() <= 4 && g.height() <= 5)
                .min(Comparator.comparingInt(g -> Math.abs(g.minX - expectedX)));
    }

    private List<GlyphBox> projectionDigitBoxes(BufferedImage clean, int startX, int endX, int startY, int endY, int expectedCount) {
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

        runs = runs.stream()
                .filter(box -> box.width() >= 1 && box.height() >= 5)
                .sorted(Comparator.comparingInt(box -> box.minX))
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));

        while (runs.size() > expectedCount) {
            int bestIndex = -1;
            int bestGap = Integer.MAX_VALUE;
            for (int i = 0; i < runs.size() - 1; i++) {
                int gap = runs.get(i + 1).minX - runs.get(i).maxX;
                if (gap < bestGap) {
                    bestGap = gap;
                    bestIndex = i;
                }
            }
            if (bestIndex < 0) {
                break;
            }
            GlyphBox merged = runs.get(bestIndex).copy();
            merged.include(runs.get(bestIndex + 1));
            runs.set(bestIndex, merged);
            runs.remove(bestIndex + 1);
        }
        return runs;
    }

    private GlyphBox trimBoxToForeground(BufferedImage clean, int startX, int endX, int startY, int endY) {
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

    private boolean isLikelyCoordinateGlyph(GlyphBox box) {
        int w = box.width();
        int h = box.height();
        boolean looksLikeSearchIcon = w >= 9 && h >= 9 && box.pixelCount >= 18;
        return !looksLikeSearchIcon && w <= 10 && h <= 16 && box.pixelCount >= 2;
    }

    private double scoreCoordinateWindow(List<GlyphBox> window) {
        int maxX = window.stream().mapToInt(g -> g.maxX).max().orElse(0);
        int minX = window.stream().mapToInt(g -> g.minX).min().orElse(0);
        int span = Math.max(1, maxX - minX);
        int maxGap = 0;
        for (int i = 1; i < window.size(); i++) {
            maxGap = Math.max(maxGap, window.get(i).minX - window.get(i - 1).maxX);
        }
        return maxX * 2.0 - span * 0.25 - Math.max(0, maxGap - 8) * 10.0;
    }

    private BufferedImage crop(BufferedImage source, GlyphBox box) {
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

    private BufferedImage trimToForeground(BufferedImage source, int pad) {
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
                Math.min(source.getHeight() - 1, maxY + pad)
        );
        BufferedImage trimmed = crop(source, box);
        source.flush();
        return trimmed;
    }

    private boolean isWhite(BufferedImage image, int x, int y) {
        return (image.getRGB(x, y) & 0x00FFFFFF) == 0x00FFFFFF;
    }

    private String recognizeOneGlyph(BufferedImage glyphImage, List<String> symbols) {
        double bestScore = 0.0;
        String bestSymbol = null;

        for (String symbol : symbols) {
            Path templateFile = TEMPLATE_DIR.resolve(symbol + ".png");
            if (!Files.exists(templateFile)) {
                continue;
            }

            try {
                BufferedImage template = ImageIO.read(templateFile.toFile());
                if (template == null) {
                    continue;
                }
                double score = foregroundSimilarity(glyphImage, template);
                template.flush();

                if (score > bestScore) {
                    bestScore = score;
                    bestSymbol = symbol;
                }
            } catch (Exception e) {
                log.warn("[coord-template] cannot read template: {}", templateFile, e);
            }
        }

        if (bestScore < TEMPLATE_MATCH_THRESHOLD) {
            return null;
        }
        return "comma".equals(bestSymbol) ? "," : bestSymbol;
    }

    private double foregroundSimilarity(BufferedImage a, BufferedImage b) {
        int whiteA = 0;
        int whiteB = 0;
        for (int y = 0; y < a.getHeight(); y++) {
            for (int x = 0; x < a.getWidth(); x++) {
                if (isWhite(a, x, y)) {
                    whiteA++;
                }
            }
        }
        for (int y = 0; y < b.getHeight(); y++) {
            for (int x = 0; x < b.getWidth(); x++) {
                if (isWhite(b, x, y)) {
                    whiteB++;
                }
            }
        }
        if (whiteA == 0 || whiteB == 0) {
            return 0.0;
        }

        double best = 0.0;
        for (int dy = -2; dy <= 2; dy++) {
            for (int dx = -2; dx <= 2; dx++) {
                int overlap = 0;
                for (int y = 0; y < a.getHeight(); y++) {
                    for (int x = 0; x < a.getWidth(); x++) {
                        int bx = x + dx;
                        int by = y + dy;
                        if (bx < 0 || by < 0 || bx >= b.getWidth() || by >= b.getHeight()) {
                            continue;
                        }
                        if (isWhite(a, x, y) && isWhite(b, bx, by)) {
                            overlap++;
                        }
                    }
                }
                best = Math.max(best, (2.0 * overlap) / (whiteA + whiteB));
            }
        }
        double sizePenalty = Math.abs(a.getWidth() - b.getWidth()) * 0.08
                + Math.abs(a.getHeight() - b.getHeight()) * 0.04;
        return Math.max(0.0, best - sizePenalty);
    }

    private Optional<String> expectedTextFromFileName(Path sample) {
        String baseName = stripExtension(sample.getFileName().toString());
        Matcher matcher = COORDINATE_FILE_NAME.matcher(baseName);
        if (!matcher.find()) {
            return Optional.empty();
        }
        return Optional.of(matcher.group(1) + "," + matcher.group(2));
    }

    private String symbolName(char c) {
        return switch (c) {
            case ',' -> "comma";
            default -> String.valueOf(c);
        };
    }

    private String stripExtension(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot < 0 ? fileName : fileName.substring(0, dot);
    }

    private static class GlyphBox {
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

        private GlyphBox expand(int imageWidth, int imageHeight, int pad) {
            GlyphBox expanded = new GlyphBox(
                    Math.max(0, minX - pad),
                    Math.max(0, minY - pad),
                    Math.min(imageWidth - 1, maxX + pad),
                    Math.min(imageHeight - 1, maxY + pad)
            );
            expanded.pixelCount = pixelCount;
            return expanded;
        }

        private GlyphBox copy() {
            GlyphBox copy = new GlyphBox(minX, minY, maxX, maxY);
            copy.pixelCount = pixelCount;
            return copy;
        }

        @Override
        public String toString() {
            return "GlyphBox[x=" + minX + ",y=" + minY + ",w=" + width() + ",h=" + height() + ",p=" + pixelCount + "]";
        }
    }

    @Value

    @Builder

    @AllArgsConstructor(access = AccessLevel.PUBLIC)

    @Accessors(fluent = true)

    private static class BracketSpan {

        int minX;

        int maxX;

        int minY;

        int maxY;

    }

    @Value

    @Builder

    @AllArgsConstructor(access = AccessLevel.PUBLIC)

    @Accessors(fluent = true)

    private static class LabeledGlyph {

        char symbol;

        int index;

        GlyphBox box;

    }

}
