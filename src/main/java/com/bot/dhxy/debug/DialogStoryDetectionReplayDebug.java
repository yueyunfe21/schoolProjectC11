package com.bot.dhxy.debug;

import com.bot.dhxy.tools.ImagePreprocessor;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * Replays story-upper dialog crops and marks rows that satisfy the thin-white text-line rule.
 *
 * <p>This is a local visual regression tool for {@link ImagePreprocessor#detectThinWhiteTextLinePattern(BufferedImage)}.
 * It does not touch the game client.</p>
 */
public class DialogStoryDetectionReplayDebug {
    private static final Path RAW_DIR = Path.of("images/test-cases/dialog/story-detection/raw");
    private static final Path OUTPUT_DIR = Path.of("images/test-cases/dialog/story-detection/output");

    public static void main(String[] args) throws Exception {
        Files.createDirectories(OUTPUT_DIR);
        try (Stream<Path> paths = Files.list(RAW_DIR)) {
            List<Path> images = paths
                    .filter(path -> path.getFileName().toString().toLowerCase().endsWith(".png"))
                    .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                    .toList();
            for (Path imagePath : images) {
                replayOne(imagePath);
            }
            System.out.printf("dialog story replay complete: total=%d output=%s%n", images.size(), OUTPUT_DIR.toAbsolutePath());
        }
    }

    private static void replayOne(Path imagePath) throws Exception {
        BufferedImage image = ImageIO.read(imagePath.toFile());
        ImagePreprocessor.TextLinePatternStats stats = ImagePreprocessor.detectThinWhiteTextLinePattern(image);
        BufferedImage marked = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = marked.createGraphics();
        try {
            graphics.drawImage(image, 0, 0, null);
            graphics.setColor(Color.RED);
            for (int y = 0; y < image.getHeight(); y++) {
                if (isQualifyingTextRow(image, y)) {
                    graphics.drawLine(0, y, image.getWidth() - 1, y);
                }
            }
            graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
            graphics.setColor(stats.matched() ? Color.GREEN : Color.RED);
            graphics.drawString("story=" + stats.matched()
                    + " rows=" + stats.qualifyingRows()
                    + " maxWhite=" + stats.maxWhitePixelsInRow()
                    + " clusters=" + stats.maxClustersInRow()
                    + " span=" + stats.maxSpanInRow(), 4, 14);
        } finally {
            graphics.dispose();
        }

        Path outputPath = OUTPUT_DIR.resolve(imagePath.getFileName().toString().replace(".png", "_story_replay.png"));
        ImageIO.write(marked, "png", outputPath.toFile());
        System.out.printf("%s -> story=%s rows=%d maxWhite=%d clusters=%d span=%d output=%s%n",
                imagePath.getFileName(), stats.matched(), stats.qualifyingRows(),
                stats.maxWhitePixelsInRow(), stats.maxClustersInRow(), stats.maxSpanInRow(),
                outputPath.toAbsolutePath());
    }

    private static boolean isQualifyingTextRow(BufferedImage image, int y) {
        int whitePixels = 0;
        int clusters = 0;
        int firstWhiteX = -1;
        int lastWhiteX = -1;
        boolean inWhiteRun = false;
        for (int x = 0; x < image.getWidth(); x++) {
            boolean white = isThinWhitePixel(image.getRGB(x, y));
            if (white) {
                whitePixels++;
                if (firstWhiteX < 0) {
                    firstWhiteX = x;
                }
                lastWhiteX = x;
                if (!inWhiteRun) {
                    clusters++;
                    inWhiteRun = true;
                }
            } else {
                inWhiteRun = false;
            }
        }
        int span = firstWhiteX < 0 ? 0 : lastWhiteX - firstWhiteX + 1;
        return whitePixels >= 12 && clusters >= 3 && span >= 60;
    }

    private static boolean isThinWhitePixel(int rgb) {
        int r = (rgb >> 16) & 0xff;
        int g = (rgb >> 8) & 0xff;
        int b = rgb & 0xff;
        float[] hsb = Color.RGBtoHSB(r, g, b, null);
        return hsb[1] <= (18f / 255f) && hsb[2] >= (225f / 255f);
    }
}
