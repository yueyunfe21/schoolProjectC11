package com.bot.dhxy.tools.manual;

import com.bot.dhxy.core.ImageFinder;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Replays the G067 user-calibrated team-return ROI and writes inspectable marked evidence. */
public final class G067TeamReturnPanelReplayTool {

    private static final int ROI_X = 314;
    private static final int ROI_Y = 279;
    private static final int ROI_WIDTH = 561;
    private static final int ROI_HEIGHT = 40;
    private static final double THRESHOLD = 0.85D;

    private G067TeamReturnPanelReplayTool() {
    }

    /**
     * Replay one full exact-HWND screenshot.
     *
     * @param args input PNG, template PNG, marked full-frame PNG, and raw ROI PNG.
     */
    public static void main(String[] args) throws IOException {
        if (args.length != 4) {
            throw new IllegalArgumentException(
                    "usage: <input.png> <template.png> <marked.png> <roi.png>");
        }
        Path inputPath = Path.of(args[0]);
        Path templatePath = Path.of(args[1]);
        Path markedPath = Path.of(args[2]);
        Path roiPath = Path.of(args[3]);
        BufferedImage input = requireImage(inputPath);
        BufferedImage template = requireImage(templatePath);
        if (ROI_X + ROI_WIDTH > input.getWidth() || ROI_Y + ROI_HEIGHT > input.getHeight()) {
            throw new IllegalArgumentException("G067 ROI lies outside replay input");
        }
        BufferedImage roi = copy(input.getSubimage(ROI_X, ROI_Y, ROI_WIDTH, ROI_HEIGHT));
        BufferedImage marked = copy(input);
        try {
            double[] strongest = ImageFinder.find(roi, template, -1.0D);
            if (strongest == null || strongest.length < 3 || !Double.isFinite(strongest[2])) {
                throw new IllegalStateException("G067 matcher did not return a strongest candidate");
            }
            boolean matched = strongest[2] >= THRESHOLD;
            Graphics2D graphics = marked.createGraphics();
            try {
                graphics.setStroke(new BasicStroke(2.0F));
                graphics.setColor(Color.GREEN);
                graphics.drawRect(ROI_X, ROI_Y, ROI_WIDTH, ROI_HEIGHT);
                int centerX = ROI_X + (int) Math.round(strongest[0]);
                int centerY = ROI_Y + (int) Math.round(strongest[1]);
                int left = centerX - template.getWidth() / 2;
                int top = centerY - template.getHeight() / 2;
                graphics.setColor(matched ? Color.RED : Color.ORANGE);
                graphics.drawRect(left, top, template.getWidth(), template.getHeight());
                graphics.fillOval(centerX - 3, centerY - 3, 7, 7);
            } finally {
                graphics.dispose();
            }
            write(markedPath, marked);
            write(roiPath, roi);
            System.out.printf(
                    "G067_TEAM_RETURN_REPLAY matched=%s score=%.6f threshold=%.2f roi=(%d,%d,%d,%d) center=(%d,%d)%n",
                    matched, strongest[2], THRESHOLD,
                    ROI_X, ROI_Y, ROI_WIDTH, ROI_HEIGHT,
                    ROI_X + (int) Math.round(strongest[0]),
                    ROI_Y + (int) Math.round(strongest[1]));
            if (!matched) {
                throw new IllegalStateException("G067 not-returned marker did not meet threshold");
            }
        } finally {
            input.flush();
            template.flush();
            roi.flush();
            marked.flush();
        }
    }

    private static BufferedImage requireImage(Path path) throws IOException {
        BufferedImage image = ImageIO.read(path.toFile());
        if (image == null) {
            throw new IOException("cannot decode image: " + path);
        }
        return image;
    }

    private static BufferedImage copy(BufferedImage source) {
        BufferedImage copy = new BufferedImage(
                source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = copy.createGraphics();
        try {
            graphics.drawImage(source, 0, 0, null);
        } finally {
            graphics.dispose();
        }
        return copy;
    }

    private static void write(Path path, BufferedImage image) throws IOException {
        Path parent = path.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        if (!ImageIO.write(image, "png", path.toFile())) {
            throw new IOException("no PNG writer available for " + path);
        }
    }
}
