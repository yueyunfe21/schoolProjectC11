package com.bot.dhxy.debug;

import com.bot.dhxy.config.BotProperties;
import com.bot.dhxy.core.TextRecognizer;
import com.bot.dhxy.model.ocr.OcrWordResult;
import com.bot.dhxy.model.ocr.TargetOcrResult;
import com.bot.dhxy.vision.GameTextLineOcrService;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Offline replay for {@link GameTextLineOcrService#findYellowTarget(BufferedImage, String, Path)}.
 *
 * <p>This debug entry reads a saved screenshot, runs the production yellow-target OCR selector, and
 * writes a marked source image. It does not capture windows, focus windows, or send input.</p>
 */
public class YellowTargetFindReplayDebug {

    public static void main(String[] args) throws Exception {
        Path input = args.length > 0
                ? Path.of(args[0])
                : Path.of("images", "test-cases", "npc-click", "jiangmo-guard-yellow-target", "input.png");
        String target = args.length > 1 ? args[1] : "降魔侍卫";
        Path lineOutput = args.length > 2
                ? Path.of(args[2])
                : Path.of("images", "temp", "yellow_target_find_replay", "selected_line.png");
        Path markedOutput = args.length > 3
                ? Path.of(args[3])
                : Path.of("images", "temp", "yellow_target_find_replay", "marked.png");
        Path washedOutput = args.length > 4
                ? Path.of(args[4])
                : Path.of("images", "temp", "yellow_target_find_replay", "yellow_washed.png");
        Path overlayOutput = args.length > 5
                ? Path.of(args[5])
                : Path.of("images", "temp", "yellow_target_find_replay", "yellow_overlay.png");

        Files.createDirectories(lineOutput.toAbsolutePath().getParent());
        Files.createDirectories(markedOutput.toAbsolutePath().getParent());
        Files.createDirectories(washedOutput.toAbsolutePath().getParent());
        Files.createDirectories(overlayOutput.toAbsolutePath().getParent());

        BufferedImage image = ImageIO.read(input.toFile());
        if (image == null) {
            throw new IllegalArgumentException("Unreadable image: " + input.toAbsolutePath());
        }

        BotProperties botProperties = new BotProperties();
        botProperties.getOcr().setProvider("local");
        GameTextLineOcrService service = new GameTextLineOcrService(new TextRecognizer(botProperties));
        service.findYellowTextCandidateResult(image, washedOutput, overlayOutput);
        TargetOcrResult result = service.findYellowTarget(image, target, lineOutput);
        writeMarked(input, markedOutput, result);
        writeMarked(washedOutput, overlayOutput, result);

        System.out.println("input=" + input.toAbsolutePath());
        System.out.println("target=" + target);
        System.out.println("hit=" + result.hit());
        System.out.println("normalizedText=" + result.normalizedText());
        System.out.println("detail=" + result.toDetailText());
        System.out.println("yellowWashed=" + washedOutput.toAbsolutePath());
        System.out.println("yellowMarked=" + overlayOutput.toAbsolutePath());
        System.out.println("selectedLine=" + lineOutput.toAbsolutePath());
        System.out.println("marked=" + markedOutput.toAbsolutePath());
    }

    private static void writeMarked(Path input, Path output, TargetOcrResult result) throws Exception {
        BufferedImage image = ImageIO.read(input.toFile());
        Graphics2D g = image.createGraphics();
        try {
            g.setStroke(new BasicStroke(3));
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 22));
            List<OcrWordResult> words = result.lineResult() == null ? List.of() : result.lineResult().words();
            if (!result.hit()) {
                g.setColor(Color.RED);
                g.drawString("no accepted yellow target text=" + result.normalizedText(),
                        20, image.getHeight() - 35);
            } else {
                g.setColor(Color.GREEN);
                for (OcrWordResult word : words) {
                    g.drawRect(word.getLeft(), word.getTop(), word.getWidth(), word.getHeight());
                }
                Point center = centerOf(words);
                if (center != null) {
                    Point click = new Point(center.x, center.y - 50);
                    g.setColor(Color.BLUE);
                    g.drawOval(center.x - 8, center.y - 8, 16, 16);
                    g.drawString("target center (" + center.x + "," + center.y + ")", 20, image.getHeight() - 65);
                    g.setColor(Color.RED);
                    g.drawLine(click.x - 14, click.y, click.x + 14, click.y);
                    g.drawLine(click.x, click.y - 14, click.x, click.y + 14);
                    g.drawString("click (" + click.x + "," + click.y + ") text=" + result.normalizedText(),
                            20, image.getHeight() - 35);
                } else {
                    g.setColor(Color.RED);
                    g.drawString("no yellow target selected text=" + result.normalizedText(),
                            20, image.getHeight() - 35);
                }
            }
        } finally {
            g.dispose();
        }
        ImageIO.write(image, "png", output.toFile());
    }

    private static Point centerOf(List<OcrWordResult> words) {
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
            right = Math.max(right, word.getLeft() + Math.max(1, word.getWidth()));
            bottom = Math.max(bottom, word.getTop() + Math.max(1, word.getHeight()));
        }
        return new Point((left + right) / 2, (top + bottom) / 2);
    }
}
