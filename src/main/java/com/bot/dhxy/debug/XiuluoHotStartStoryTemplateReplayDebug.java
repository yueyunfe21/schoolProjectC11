package com.bot.dhxy.debug;

import com.bot.dhxy.core.ImageFinder;
import com.bot.dhxy.tools.ImagePreprocessor;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Replays the Xiuluo hot-start story confirmation template against saved screenshots.
 *
 * <p>This tool verifies the exact production path used by {@code VERIFY_WHITE_TEMPLATE}: wash the
 * source image as thin white text, match the washed Xiuluo template, then write a marked image for
 * visual review. It does not touch the game client.</p>
 */
public class XiuluoHotStartStoryTemplateReplayDebug {
    private static final double THRESHOLD = 0.85;
    private static final Path TEMPLATE =
            Path.of("images/template/dialog/xiuluo/xiuluo_story_miexiu_confirm.png");
    private static final Path OUTPUT_DIR =
            Path.of("images/test-cases/dialog/xiuluo-hotstart-story/output");

    private static final List<CaseSpec> CASES = List.of(
            new CaseSpec(
                    "positive-template-source",
                    Path.of("images/test-cases/dialog/xiuluo-hotstart-story/raw/positive_story_raw.png"),
                    true),
            new CaseSpec(
                    "false-story-labels",
                    Path.of("images/test-cases/dialog/xiuluo-hotstart-story/raw/false_story_labels_raw.png"),
                    false)
    );

    public static void main(String[] args) throws Exception {
        Files.createDirectories(OUTPUT_DIR);
        BufferedImage templateImage = ImageIO.read(TEMPLATE.toFile());
        int passed = 0;
        for (CaseSpec spec : CASES) {
            if (!Files.exists(spec.source())) {
                System.out.printf("skip missing case: name=%s source=%s%n", spec.name(), spec.source().toAbsolutePath());
                continue;
            }
            ReplayResult result = replay(spec, templateImage);
            if (result.passed()) {
                passed++;
            }
            System.out.printf("case=%s expected=%s matched=%s score=%.4f point=(%.1f,%.1f) passed=%s marked=%s washed=%s%n",
                    spec.name(), spec.expectedMatch(), result.matched(), result.score(),
                    result.centerX(), result.centerY(), result.passed(),
                    result.markedPath().toAbsolutePath(), result.washedPath().toAbsolutePath());
        }
        System.out.printf("xiuluo hot-start story replay complete: passed=%d total=%d output=%s%n",
                passed, CASES.size(), OUTPUT_DIR.toAbsolutePath());
    }

    private static ReplayResult replay(CaseSpec spec, BufferedImage templateImage) throws Exception {
        Path washed = OUTPUT_DIR.resolve(spec.name() + "_white.png");
        Path marked = OUTPUT_DIR.resolve(spec.name() + "_marked.png");
        ImagePreprocessor.washThinWhiteTextToBlackAndWhite(spec.source().toString(), washed.toString());
        double[] match = ImageFinder.find(washed.toString(), TEMPLATE.toString(), THRESHOLD);
        boolean matched = match != null;
        writeMarked(spec.source(), templateImage, match, spec.expectedMatch(), marked);
        return new ReplayResult(
                matched,
                match == null ? 0.0 : match[2],
                match == null ? -1.0 : match[0],
                match == null ? -1.0 : match[1],
                matched == spec.expectedMatch(),
                washed,
                marked);
    }

    private static void writeMarked(Path source,
                                    BufferedImage templateImage,
                                    double[] match,
                                    boolean expectedMatch,
                                    Path outputPath) throws Exception {
        BufferedImage raw = ImageIO.read(source.toFile());
        BufferedImage marked = new BufferedImage(raw.getWidth(), raw.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = marked.createGraphics();
        try {
            g.drawImage(raw, 0, 0, null);
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
            if (match != null) {
                int left = (int) Math.round(match[0] - templateImage.getWidth() / 2.0);
                int top = (int) Math.round(match[1] - templateImage.getHeight() / 2.0);
                g.setColor(Color.RED);
                g.setStroke(new BasicStroke(2));
                g.drawRect(left, top, templateImage.getWidth(), templateImage.getHeight());
                g.drawLine((int) match[0] - 8, (int) match[1], (int) match[0] + 8, (int) match[1]);
                g.drawLine((int) match[0], (int) match[1] - 8, (int) match[0], (int) match[1] + 8);
                g.drawString("MATCH score=" + String.format("%.4f", match[2]), 8, 18);
            } else {
                g.setColor(expectedMatch ? Color.RED : Color.GREEN);
                g.drawString("NO MATCH", 8, 18);
            }
        } finally {
            g.dispose();
        }
        ImageIO.write(marked, "png", outputPath.toFile());
    }

    private record CaseSpec(String name, Path source, boolean expectedMatch) {
    }

    private record ReplayResult(boolean matched,
                                double score,
                                double centerX,
                                double centerY,
                                boolean passed,
                                Path washedPath,
                                Path markedPath) {
    }
}
