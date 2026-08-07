package com.bot.dhxy.service;

import com.bot.dhxy.core.ImageFinder;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class XinshouUpgradeItemReplayTest {

    private static final Path CASE_DIR =
            Path.of("images", "test-cases", "xinshou", "shanhu-upgrade-20260730");
    private static final Path SOURCE = CASE_DIR.resolve("bag_scan.png");
    private static final Path TEMPLATE =
            Path.of("images", "template", "xinshou", "shengji.png");
    private static final Path MARKED = CASE_DIR.resolve("bag_scan_marked.png");

    @Test
    void reportsAndMarksTheStrongestUpgradeItemCandidate() throws Exception {
        double[] match = ImageFinder.find(SOURCE.toString(), TEMPLATE.toString(), 0.0);
        assertNotNull(match);

        BufferedImage source = ImageIO.read(SOURCE.toFile());
        BufferedImage template = ImageIO.read(TEMPLATE.toFile());
        Graphics2D graphics = source.createGraphics();
        try {
            int left = (int) Math.round(match[0] - template.getWidth() / 2.0);
            int top = (int) Math.round(match[1] - template.getHeight() / 2.0);
            graphics.setColor(Color.RED);
            graphics.setStroke(new BasicStroke(2.0f));
            graphics.drawRect(left, top, template.getWidth(), template.getHeight());
            graphics.drawLine((int) match[0] - 5, (int) match[1], (int) match[0] + 5, (int) match[1]);
            graphics.drawLine((int) match[0], (int) match[1] - 5, (int) match[0], (int) match[1] + 5);
        } finally {
            graphics.dispose();
        }
        Files.createDirectories(MARKED.getParent());
        ImageIO.write(source, "png", MARKED.toFile());
        System.out.printf(
                "XINSHOU_UPGRADE_REPLAY center=(%.1f,%.1f) score=%.6f marked=%s%n",
                match[0], match[1], match[2], MARKED.toAbsolutePath());
    }
}
