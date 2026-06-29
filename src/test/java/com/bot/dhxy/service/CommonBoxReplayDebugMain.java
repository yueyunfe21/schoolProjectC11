package com.bot.dhxy.service;

import com.bot.dhxy.core.ImageFinder;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * No-click replay for CR120 common box template calibration.
 */
public final class CommonBoxReplayDebugMain {

    private static final Path RAW = Path.of(
            "images/test-cases/common-box/raw/leader_box_marker_roi_raw.png");
    private static final Path TEMPLATE = Path.of(
            "images/template/common/leader_box_marker.png");
    private static final Path OUTPUT = Path.of(
            "images/test-cases/common-box/output/leader_box_marker_roi_raw_output.png");

    private CommonBoxReplayDebugMain() {
    }

    public static void main(String[] args) throws Exception {
        BufferedImage raw = ImageIO.read(RAW.toFile());
        BufferedImage template = ImageIO.read(TEMPLATE.toFile());
        double[] match = ImageFinder.find(raw, template, 0.86);
        if (match == null) {
            throw new AssertionError("common box marker did not match raw testcase");
        }
        BufferedImage output = new BufferedImage(raw.getWidth(), raw.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = output.createGraphics();
        try {
            graphics.drawImage(raw, 0, 0, null);
            int x = (int) Math.round(match[0]);
            int y = (int) Math.round(match[1]);
            graphics.setColor(Color.RED);
            graphics.setStroke(new BasicStroke(2));
            graphics.drawOval(x - 4, y - 4, 8, 8);
            graphics.fillOval(x - 2, y - 2, 4, 4);
        } finally {
            graphics.dispose();
        }
        Files.createDirectories(OUTPUT.getParent());
        ImageIO.write(output, "png", OUTPUT.toFile());
        System.out.println("common box replay matched score=" + match[2]
                + " click=(" + Math.round(match[0]) + "," + Math.round(match[1]) + ")"
                + " output=" + OUTPUT);
    }
}
