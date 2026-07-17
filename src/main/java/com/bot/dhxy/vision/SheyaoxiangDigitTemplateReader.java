package com.bot.dhxy.vision;

import com.bot.dhxy.model.ocr.OcrWordResult;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * Reads the tiny 摄妖香 status-bar remaining-time digits from the already washed black-on-white
 * image. RapidOCR is unreliable for this fixed game font, so OCR is used only as a bootstrap label
 * source for per-digit templates; complete reads prefer the learned templates.
 */
public class SheyaoxiangDigitTemplateReader {
    private static final Path DEFAULT_TEMPLATE_DIR = Path.of(
            "images", "template", "status", "sheyaoxiang_digits");
    private static final int MIN_COMPONENT_PIXELS = 80;
    private static final int MIN_COMPONENT_WIDTH = 8;
    private static final int MIN_COMPONENT_HEIGHT = 18;
    private static final double MATCH_THRESHOLD = 0.88;
    private static final double OCR_BOX_OVERLAP_THRESHOLD = 0.35;

    private final Path templateDir;

    public SheyaoxiangDigitTemplateReader() {
        this(DEFAULT_TEMPLATE_DIR);
    }

    public SheyaoxiangDigitTemplateReader(Path templateDir) {
        this.templateDir = templateDir;
    }

    /**
     * Recognize a washed 摄妖香 digit image, then opportunistically learn missing digit templates
     * from OCR boxes that can be mapped to exact segmented digit glyphs.
     *
     * @param washed black-on-white status digit image.
     * @param ocrWords OCR words returned for the same image, with image-local boxes.
     * @param source diagnostic label, usually the image path.
     * @return reliable full digit text when templates or complete OCR can explain all segmented
     * digits; otherwise empty text plus the symbols learned during this call.
     */
    public Result recognizeAndLearn(BufferedImage washed, List<OcrWordResult> ocrWords, String source) {
        List<DigitGlyph> glyphs = segmentDigits(washed);
        if (glyphs.isEmpty()) {
            return new Result("", 0, List.of(), false);
        }

        Optional<String> templateText = recognizeWithTemplates(washed, glyphs);
        if (templateText.isPresent()) {
            return new Result(templateText.get(), glyphs.size(), List.of(), true);
        }

        List<String> learned = learnFromOcr(washed, glyphs, ocrWords, source);
        Optional<String> afterLearnText = recognizeWithTemplates(washed, glyphs);
        if (afterLearnText.isPresent()) {
            return new Result(afterLearnText.get(), glyphs.size(), learned, true);
        }

        Optional<String> completeOcrText = completeOcrText(glyphs, ocrWords);
        return completeOcrText
                .map(text -> new Result(text, glyphs.size(), learned, true))
                .orElseGet(() -> new Result("", glyphs.size(), learned, false));
    }

    private List<DigitGlyph> segmentDigits(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        boolean[][] foreground = new boolean[height][width];
        boolean[][] seen = new boolean[height][width];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                foreground[y][x] = isForeground(image.getRGB(x, y));
            }
        }

        List<DigitGlyph> glyphs = new ArrayList<>();
        int[] dx = {-1, 0, 1, -1, 1, -1, 0, 1};
        int[] dy = {-1, -1, -1, 0, 0, 1, 1, 1};
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (!foreground[y][x] || seen[y][x]) {
                    continue;
                }
                ArrayDeque<int[]> queue = new ArrayDeque<>();
                queue.add(new int[]{x, y});
                seen[y][x] = true;
                int count = 0;
                int left = x;
                int right = x;
                int top = y;
                int bottom = y;
                while (!queue.isEmpty()) {
                    int[] point = queue.remove();
                    int px = point[0];
                    int py = point[1];
                    count++;
                    left = Math.min(left, px);
                    right = Math.max(right, px);
                    top = Math.min(top, py);
                    bottom = Math.max(bottom, py);
                    for (int i = 0; i < dx.length; i++) {
                        int nx = px + dx[i];
                        int ny = py + dy[i];
                        if (nx < 0 || nx >= width || ny < 0 || ny >= height
                                || !foreground[ny][nx] || seen[ny][nx]) {
                            continue;
                        }
                        seen[ny][nx] = true;
                        queue.add(new int[]{nx, ny});
                    }
                }
                int glyphWidth = right - left + 1;
                int glyphHeight = bottom - top + 1;
                if (count >= MIN_COMPONENT_PIXELS
                        && glyphWidth >= MIN_COMPONENT_WIDTH
                        && glyphHeight >= MIN_COMPONENT_HEIGHT) {
                    glyphs.add(new DigitGlyph(left, top, right + 1, bottom + 1, count));
                }
            }
        }
        glyphs.sort(Comparator.comparingInt(DigitGlyph::left));
        return glyphs;
    }

    private Optional<String> recognizeWithTemplates(BufferedImage image, List<DigitGlyph> glyphs) {
        List<DigitTemplate> templates = loadTemplates();
        if (templates.isEmpty()) {
            return Optional.empty();
        }
        StringBuilder text = new StringBuilder();
        for (DigitGlyph glyph : glyphs) {
            BufferedImage glyphImage = glyph.crop(image);
            try {
                DigitTemplate best = null;
                double bestScore = 0.0;
                for (DigitTemplate template : templates) {
                    double score = compare(glyphImage, template.image());
                    if (score > bestScore) {
                        bestScore = score;
                        best = template;
                    }
                }
                if (best == null || bestScore < MATCH_THRESHOLD) {
                    return Optional.empty();
                }
                text.append(best.symbol());
            } finally {
                glyphImage.flush();
            }
        }
        return Optional.of(text.toString());
    }

    private List<String> learnFromOcr(BufferedImage image,
                                      List<DigitGlyph> glyphs,
                                      List<OcrWordResult> ocrWords,
                                      String source) {
        if (hasAllDigitTemplates()) {
            return List.of();
        }
        Set<String> learned = new HashSet<>();
        if (ocrWords == null || ocrWords.isEmpty()) {
            return List.of();
        }
        for (OcrWordResult word : ocrWords) {
            String digits = digitsOnly(word == null ? null : word.getText());
            if (digits.isEmpty()) {
                continue;
            }
            if (digits.length() == glyphs.size() && overlapsAllGlyphs(word, glyphs)) {
                for (int i = 0; i < glyphs.size(); i++) {
                    saveTemplateIfMissing(digits.substring(i, i + 1), glyphs.get(i), image, source, learned);
                }
            } else if (digits.length() == 1) {
                bestOcrCoveredGlyph(word, glyphs)
                        .ifPresent(glyph -> saveTemplateIfMissing(digits, glyph, image, source, learned));
            }
        }
        return learned.stream().sorted().toList();
    }

    private Optional<DigitGlyph> bestOcrCoveredGlyph(OcrWordResult word, List<DigitGlyph> glyphs) {
        DigitGlyph best = null;
        double bestRatio = 0.0;
        for (DigitGlyph glyph : glyphs) {
            double ratio = overlapRatio(word, glyph);
            if (ratio > bestRatio) {
                bestRatio = ratio;
                best = glyph;
            }
        }
        return bestRatio >= OCR_BOX_OVERLAP_THRESHOLD ? Optional.of(best) : Optional.empty();
    }

    private boolean overlapsAllGlyphs(OcrWordResult word, List<DigitGlyph> glyphs) {
        for (DigitGlyph glyph : glyphs) {
            if (overlapRatio(word, glyph) < OCR_BOX_OVERLAP_THRESHOLD) {
                return false;
            }
        }
        return true;
    }

    private double overlapRatio(OcrWordResult word, DigitGlyph glyph) {
        if (word == null) {
            return 0.0;
        }
        int wordLeft = word.getLeft();
        int wordTop = word.getTop();
        int wordRight = wordLeft + word.getWidth();
        int wordBottom = wordTop + word.getHeight();
        int left = Math.max(wordLeft, glyph.left());
        int top = Math.max(wordTop, glyph.top());
        int right = Math.min(wordRight, glyph.right());
        int bottom = Math.min(wordBottom, glyph.bottom());
        if (right <= left || bottom <= top) {
            return 0.0;
        }
        return ((right - left) * (double) (bottom - top)) / glyph.area();
    }

    private Optional<String> completeOcrText(List<DigitGlyph> glyphs, List<OcrWordResult> ocrWords) {
        if (ocrWords == null || ocrWords.isEmpty()) {
            return Optional.empty();
        }
        StringBuilder text = new StringBuilder();
        for (OcrWordResult word : ocrWords) {
            text.append(digitsOnly(word.getText()));
        }
        String value = text.toString();
        return value.length() == glyphs.size() ? Optional.of(value) : Optional.empty();
    }

    private void saveTemplateIfMissing(String symbol,
                                       DigitGlyph glyph,
                                       BufferedImage sourceImage,
                                       String source,
                                       Set<String> learned) {
        if (symbol == null || symbol.length() != 1 || !Character.isDigit(symbol.charAt(0))) {
            return;
        }
        Path output = templateDir.resolve(symbol + ".png");
        if (Files.exists(output)) {
            return;
        }
        BufferedImage glyphImage = glyph.crop(sourceImage);
        try {
            Files.createDirectories(templateDir);
            ImageIO.write(glyphImage, "png", output.toFile());
            learned.add(symbol);
        } catch (IOException e) {
            // Learning is opportunistic; a write failure must not break the supply check.
        } finally {
            glyphImage.flush();
        }
    }

    private List<DigitTemplate> loadTemplates() {
        List<DigitTemplate> templates = new ArrayList<>();
        for (int digit = 0; digit <= 9; digit++) {
            Path path = templateDir.resolve(digit + ".png");
            if (!Files.exists(path)) {
                continue;
            }
            try {
                BufferedImage image = ImageIO.read(path.toFile());
                if (image != null) {
                    templates.add(new DigitTemplate(String.valueOf(digit), image));
                }
            } catch (IOException e) {
                // Ignore unreadable learned templates and continue with the remaining digits.
            }
        }
        return templates;
    }

    private boolean hasAllDigitTemplates() {
        for (int digit = 0; digit <= 9; digit++) {
            if (!Files.exists(templateDir.resolve(digit + ".png"))) {
                return false;
            }
        }
        return true;
    }

    private double compare(BufferedImage glyph, BufferedImage template) {
        BufferedImage scaledTemplate = scale(template, glyph.getWidth(), glyph.getHeight());
        try {
            int same = 0;
            int total = glyph.getWidth() * glyph.getHeight();
            for (int y = 0; y < glyph.getHeight(); y++) {
                for (int x = 0; x < glyph.getWidth(); x++) {
                    if (isForeground(glyph.getRGB(x, y)) == isForeground(scaledTemplate.getRGB(x, y))) {
                        same++;
                    }
                }
            }
            return same / (double) total;
        } finally {
            scaledTemplate.flush();
        }
    }

    private BufferedImage scale(BufferedImage source, int width, int height) {
        BufferedImage output = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = output.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            graphics.drawImage(source, 0, 0, width, height, null);
        } finally {
            graphics.dispose();
        }
        return output;
    }

    private boolean isForeground(int rgb) {
        int red = (rgb >> 16) & 0xFF;
        int green = (rgb >> 8) & 0xFF;
        int blue = rgb & 0xFF;
        return red + green + blue < 384;
    }

    private String digitsOnly(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (Character.isDigit(ch)) {
                builder.append(ch);
            }
        }
        return builder.toString();
    }

    public record Result(String text, int digitCount, List<String> learnedSymbols, boolean reliable) {
    }

    private record DigitTemplate(String symbol, BufferedImage image) {
    }

    private record DigitGlyph(int left, int top, int right, int bottom, int foregroundPixels) {

        int area() {
            return (right - left) * (bottom - top);
        }

        BufferedImage crop(BufferedImage image) {
            return image.getSubimage(left, top, right - left, bottom - top);
        }

        @Override
        public String toString() {
            return String.format(Locale.ROOT, "(%d,%d)-(%d,%d)", left, top, right, bottom);
        }
    }
}
