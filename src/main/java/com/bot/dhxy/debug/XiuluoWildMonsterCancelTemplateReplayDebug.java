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

/**
 * Replays the CR79 修罗 wild-monster cancel option template against a saved testcase image.
 *
 * <p>The replay uses the same green/yellow dialog-option wash and template matcher as the runtime
 * `handleGreenTemplateOption` path. It writes a marked image showing the matched text box and the
 * representative click point without touching the live game client.</p>
 */
public class XiuluoWildMonsterCancelTemplateReplayDebug {
    private static final double THRESHOLD = 0.85;
    private static final Path SOURCE =
            Path.of("images/template/cancel/Snipaste_2026-06-21_23-02-19.png");
    private static final Path TEMPLATE =
            Path.of("images/template/dialog/xiuluo/xiuluo_wild_monster_cancel.png");
    private static final Path RAW_CASE =
            Path.of("images/test-cases/dialog/xiuluo-wild-monster-cancel/raw/wild_monster_cancel_option_raw.png");
    private static final Path OUTPUT_DIR =
            Path.of("images/test-cases/dialog/xiuluo-wild-monster-cancel/output");

    public static void main(String[] args) throws Exception {
        Files.createDirectories(RAW_CASE.getParent());
        Files.createDirectories(OUTPUT_DIR);
        Files.copy(SOURCE, RAW_CASE, java.nio.file.StandardCopyOption.REPLACE_EXISTING);

        ImagePreprocessor.washDialogOptionTemplateTextToBlackAndWhite(
                RAW_CASE.toString(), TEMPLATE.toString());
        Path washed = OUTPUT_DIR.resolve("wild_monster_cancel_option_washed.png");
        ImagePreprocessor.washDialogOptionTemplateTextToBlackAndWhite(
                RAW_CASE.toString(), washed.toString());

        double[] match = ImageFinder.find(washed.toString(), TEMPLATE.toString(), THRESHOLD);
        if (match == null) {
            throw new AssertionError("CR79 wild-monster cancel template did not match testcase at " + THRESHOLD);
        }

        Path marked = OUTPUT_DIR.resolve("wild_monster_cancel_option_marked.png");
        writeMarked(RAW_CASE, TEMPLATE, match, marked);
        System.out.printf(
                "xiuluo wild-monster cancel replay matched: score=%.4f click=(%.1f,%.1f) raw=%s washed=%s marked=%s%n",
                match[2], match[0], match[1],
                RAW_CASE.toAbsolutePath(), washed.toAbsolutePath(), marked.toAbsolutePath());
    }

    private static void writeMarked(Path rawPath, Path templatePath, double[] match, Path outputPath) throws Exception {
        BufferedImage raw = ImageIO.read(rawPath.toFile());
        BufferedImage template = ImageIO.read(templatePath.toFile());
        BufferedImage marked = new BufferedImage(raw.getWidth(), raw.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = marked.createGraphics();
        try {
            g.drawImage(raw, 0, 0, null);
            int left = (int) Math.round(match[0] - template.getWidth() / 2.0);
            int top = (int) Math.round(match[1] - template.getHeight() / 2.0);
            int clickX = (int) Math.round(match[0]);
            int clickY = (int) Math.round(match[1]);

            g.setColor(Color.RED);
            g.setStroke(new BasicStroke(2));
            g.drawRect(left, top, template.getWidth(), template.getHeight());
            g.drawLine(clickX - 7, clickY, clickX + 7, clickY);
            g.drawLine(clickX, clickY - 7, clickX, clickY + 7);
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
            g.drawString("score=" + String.format("%.4f", match[2]), 4, 14);
            g.drawString("click=(" + clickX + "," + clickY + ")", 4, Math.min(raw.getHeight() - 4, 30));
        } finally {
            g.dispose();
            raw.flush();
            template.flush();
        }
        ImageIO.write(marked, "png", outputPath.toFile());
        marked.flush();
    }
}
