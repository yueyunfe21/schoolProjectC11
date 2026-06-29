package com.bot.dhxy.debug;

import com.bot.dhxy.config.BotProperties;
import com.bot.dhxy.core.TextRecognizer;
import com.bot.dhxy.model.ocr.OcrWordResult;
import com.bot.dhxy.vision.OcrTextMatcher;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Offline replay for the yellow-target OCR word-selection step.
 *
 * <p>This tool does not capture windows or send input. It consumes a saved OCR/debug image, runs
 * local OCR, chooses the OCR word span that actually represents the requested NPC name, and writes a
 * marked image showing both OCR boxes and the final click point.</p>
 */
public class YellowTargetWordSelectionReplayDebug {

    private static final String STRICT_YELLOW_TARGET_JIANGMO_SHIWEI = "降魔侍卫";
    private static final int STRICT_YELLOW_TARGET_JIANGMO_SHIWEI_MIN_COMMON = 3;

    public static void main(String[] args) throws Exception {
        Path input = args.length > 0 ? Path.of(args[0]) : Path.of("images", "img.png");
        String target = args.length > 1 ? args[1] : "白龙马";
        Path output = args.length > 2
                ? Path.of(args[2])
                : Path.of("images", "temp", "yellow_target_word_selection_marked.png");
        Files.createDirectories(output.toAbsolutePath().getParent());

        BotProperties botProperties = new BotProperties();
        botProperties.getOcr().setProvider("local");
        TextRecognizer recognizer = new TextRecognizer(botProperties);
        List<OcrWordResult> words = recognizer.getAllTextResultsLocalOnly(input.toString());
        List<OcrWordResult> selected = selectTargetWords(words, OcrTextMatcher.normalizeName(target));
        boolean accepted = !selected.isEmpty();
        Point center = centerOfWords(selected);
        Point click = center == null ? null : new Point(center.x, center.y - 50);

        writeMarked(input, output, words, selected, center, click, accepted);
        System.out.println("input=" + input.toAbsolutePath());
        System.out.println("target=" + target);
        System.out.println("words=" + summarize(words));
        System.out.println("selected=" + summarize(selected));
        System.out.println("accepted=" + accepted);
        System.out.println("center=" + pointText(center));
        System.out.println("click=" + pointText(click));
        System.out.println("marked=" + output.toAbsolutePath());
    }

    private static List<OcrWordResult> selectTargetWords(List<OcrWordResult> words, String normalizedTarget) {
        if (words == null || words.isEmpty() || normalizedTarget == null || normalizedTarget.isBlank()) {
            return words == null ? List.of() : words;
        }
        List<OcrWordResult> best = List.of();
        int bestTier = Integer.MAX_VALUE;
        int bestExtra = Integer.MAX_VALUE;
        for (int start = 0; start < words.size(); start++) {
            StringBuilder builder = new StringBuilder();
            for (int end = start; end < words.size(); end++) {
                OcrWordResult word = words.get(end);
                if (word != null) {
                    builder.append(word.getText());
                }
                String normalizedSpan = OcrTextMatcher.normalizeName(builder.toString());
                if (normalizedSpan.isBlank()) {
                    continue;
                }
                int tier = matchTier(normalizedSpan, normalizedTarget);
                int extra = Math.abs(normalizedSpan.length() - normalizedTarget.length());
                int span = end - start + 1;
                if (tier < Integer.MAX_VALUE
                        && (best.isEmpty()
                        || tier < bestTier
                        || (tier == bestTier && extra < bestExtra)
                        || (tier == bestTier && extra == bestExtra && span < best.size()))) {
                    best = new ArrayList<>(words.subList(start, end + 1));
                    bestTier = tier;
                    bestExtra = extra;
                }
            }
        }
        if (!best.isEmpty()) {
            return best;
        }
        return strictMinCommon(normalizedTarget) > 0 ? List.of() : words;
    }

    private static int matchTier(String normalizedSpan, String normalizedTarget) {
        int strictMinCommon = strictMinCommon(normalizedTarget);
        if (normalizedSpan.equals(normalizedTarget)) {
            return 0;
        }
        if (normalizedSpan.contains(normalizedTarget)) {
            return 1;
        }
        if (strictMinCommon > 0) {
            int almostFull = Math.max(strictMinCommon, normalizedTarget.length() - 1);
            return normalizedTarget.contains(normalizedSpan)
                    && normalizedSpan.length() >= almostFull
                    ? 2
                    : Integer.MAX_VALUE;
        }
        if (normalizedTarget.contains(normalizedSpan)
                && normalizedSpan.length() >= Math.min(2, normalizedTarget.length())) {
            return 2;
        }
        return OcrTextMatcher.isShortNameMatch(normalizedSpan, normalizedTarget) ? 3 : Integer.MAX_VALUE;
    }

    private static int strictMinCommon(String normalizedTarget) {
        return STRICT_YELLOW_TARGET_JIANGMO_SHIWEI.equals(normalizedTarget)
                ? STRICT_YELLOW_TARGET_JIANGMO_SHIWEI_MIN_COMMON
                : 0;
    }

    private static Point centerOfWords(List<OcrWordResult> words) {
        if (words == null || words.isEmpty()) {
            return null;
        }
        int left = Integer.MAX_VALUE;
        int top = Integer.MAX_VALUE;
        int right = Integer.MIN_VALUE;
        int bottom = Integer.MIN_VALUE;
        for (OcrWordResult word : words) {
            left = Math.min(left, word.getLeft());
            top = Math.min(top, word.getTop());
            right = Math.max(right, word.getLeft() + word.getWidth());
            bottom = Math.max(bottom, word.getTop() + word.getHeight());
        }
        return new Point((left + right) / 2, (top + bottom) / 2);
    }

    private static void writeMarked(Path input,
                                    Path output,
                                    List<OcrWordResult> words,
                                    List<OcrWordResult> selected,
                                    Point center,
                                    Point click,
                                    boolean accepted) throws Exception {
        BufferedImage image = ImageIO.read(input.toFile());
        Graphics2D g = image.createGraphics();
        try {
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 22));
            g.setStroke(new BasicStroke(3));
            g.setColor(Color.ORANGE);
            for (OcrWordResult word : words) {
                g.drawRect(word.getLeft(), word.getTop(), word.getWidth(), word.getHeight());
            }
            g.setColor(Color.GREEN);
            for (OcrWordResult word : selected) {
                g.drawRect(word.getLeft(), word.getTop(), word.getWidth(), word.getHeight());
            }
            if (center != null) {
                g.setColor(Color.BLUE);
                g.drawOval(center.x - 8, center.y - 8, 16, 16);
                g.drawString("selected center " + pointText(center), 20, image.getHeight() - 65);
            }
            if (click != null) {
                g.setColor(Color.RED);
                g.drawLine(click.x - 12, click.y, click.x + 12, click.y);
                g.drawLine(click.x, click.y - 12, click.x, click.y + 12);
                g.drawString("click " + pointText(click), 20, image.getHeight() - 35);
            } else if (!accepted) {
                g.setColor(Color.RED);
                g.drawString("strict target rejected, no direct click", 20, image.getHeight() - 35);
            }
        } finally {
            g.dispose();
        }
        ImageIO.write(image, "png", output.toFile());
    }

    private static String summarize(List<OcrWordResult> words) {
        if (words == null || words.isEmpty()) {
            return "[]";
        }
        List<String> parts = new ArrayList<>();
        for (OcrWordResult word : words) {
            parts.add(word.getText() + "@(" + word.getX() + "," + word.getY()
                    + "," + word.getLeft() + "," + word.getTop()
                    + "," + word.getWidth() + "," + word.getHeight() + ")");
        }
        return parts.toString();
    }

    private static String pointText(Point point) {
        return point == null ? "-" : "(" + point.x + "," + point.y + ")";
    }
}
