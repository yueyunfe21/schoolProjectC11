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
 * Replays 五环 completion story templates against saved testcase screenshots.
 *
 * <p>This verifies the same white-template path used by {@code DialogOperation.VERIFY_WHITE_TEMPLATE}
 * without focusing or touching the game client. Raw cases are washed first; historical cases that
 * only retained the washed source are matched directly.</p>
 */
public class WuhuanCompletionStoryReplayDebug {
    private static final double THRESHOLD = 0.85;
    private static final Path OUTPUT_DIR = Path.of("images/test-cases/dialog/wuhuan/output");

    private static final List<CaseSpec> CASES = List.of(
            new CaseSpec(
                    "once-20260613",
                    Path.of("images/test-cases/dialog/wuhuan/output/once-20260613_white.png"),
                    true,
                    Path.of("images/template/dialog/wuhuan/wuhuan_task_finished_once_story.png"),
                    true),
            new CaseSpec(
                    "final-today-20260618",
                    Path.of("images/test-cases/dialog/wuhuan/output/final-today-20260618_white.png"),
                    true,
                    Path.of("images/template/dialog/wuhuan/wuhuan_task_finished_story.png"),
                    true)
    );

    public static void main(String[] args) throws Exception {
        Files.createDirectories(OUTPUT_DIR);
        int passed = 0;
        for (CaseSpec spec : CASES) {
            if (!Files.exists(spec.source())) {
                System.out.printf("skip missing case: name=%s source=%s%n",
                        spec.name(), spec.source().toAbsolutePath());
                continue;
            }
            if (!Files.exists(spec.template())) {
                System.out.printf("skip missing template: name=%s template=%s%n",
                        spec.name(), spec.template().toAbsolutePath());
                continue;
            }
            ReplayResult result = replay(spec);
            if (result.passed()) {
                passed++;
            }
            System.out.printf("case=%s expected=%s matched=%s score=%.4f point=(%.1f,%.1f) passed=%s marked=%s washed=%s%n",
                    spec.name(), spec.expectedMatch(), result.matched(), result.score(),
                    result.centerX(), result.centerY(), result.passed(),
                    result.markedPath().toAbsolutePath(), result.washedPath().toAbsolutePath());
        }
        System.out.printf("wuhuan completion story replay complete: passed=%d total=%d output=%s%n",
                passed, CASES.size(), OUTPUT_DIR.toAbsolutePath());
        if (passed != CASES.size()) {
            throw new AssertionError("Some Wuhuan completion story replay cases failed");
        }
    }

    private static ReplayResult replay(CaseSpec spec) throws Exception {
        Path washed = OUTPUT_DIR.resolve(spec.name() + "_white.png");
        Path marked = OUTPUT_DIR.resolve(spec.name() + "_marked.png");
        if (spec.sourceAlreadyWashed()) {
            Files.copy(spec.source(), washed, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        } else {
            ImagePreprocessor.washThinWhiteTextToBlackAndWhite(spec.source().toString(), washed.toString());
        }
        double[] match = ImageFinder.find(washed.toString(), spec.template().toString(), THRESHOLD);
        boolean matched = match != null;
        writeMarked(spec.source(), spec.template(), match, spec.expectedMatch(), marked);
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
                                    Path template,
                                    double[] match,
                                    boolean expectedMatch,
                                    Path outputPath) throws Exception {
        BufferedImage raw = ImageIO.read(source.toFile());
        BufferedImage templateImage = ImageIO.read(template.toFile());
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

    private record CaseSpec(String name,
                            Path source,
                            boolean sourceAlreadyWashed,
                            Path template,
                            boolean expectedMatch) {
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
