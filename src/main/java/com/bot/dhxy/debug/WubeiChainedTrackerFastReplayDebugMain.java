package com.bot.dhxy.debug;

import com.bot.dhxy.config.BotProperties;
import com.bot.dhxy.core.TextRecognizer;
import com.bot.dhxy.model.dialog.PreparedDialogAction;
import com.bot.dhxy.model.ocr.OcrWordResult;
import com.bot.dhxy.model.tasktracker.TaskTrackerGreenLink;
import com.bot.dhxy.model.tasktracker.TaskTrackerPanelReadResult;
import com.bot.dhxy.service.MapNameCanonicalizer;
import com.bot.dhxy.service.TaskTrackerPanelService;
import com.bot.dhxy.tools.ImagePreprocessor;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Offline replay for CR54 黄袍续战 tracker green-link fast cache.
 *
 * <p>This tool reads saved tracker-detail screenshots only. It verifies that a first full tracker
 * read can prepare a green-link fingerprint/click cache and that the same small area matches
 * without rerunning title/yellow/green-map OCR.</p>
 */
public class WubeiChainedTrackerFastReplayDebugMain {

    private static final Path ROOT = Path.of("").toAbsolutePath().normalize();
    private static final Path IMAGE_ROOT = ROOT.resolve("images/test-cases/task-tracker/wubei-task-panel/raw");
    private static final Path OUTPUT_DIR = ROOT.resolve("images/test-cases/task-tracker/wubei-task-panel/output/chained-fast");
    private static final Path REPORT = ROOT.resolve("logs/wubei-chained-tracker-fast-replay.csv");
    private static final int MAX_DISTANCE = 8;

    public static void main(String[] args) throws Exception {
        Files.createDirectories(OUTPUT_DIR);
        Files.createDirectories(REPORT.getParent());
        int maxSamples = args.length > 0 ? Integer.parseInt(args[0]) : 80;

        BotProperties botProperties = new BotProperties();
        botProperties.getOcr().setProvider("local");
        TextRecognizer textRecognizer = new TextRecognizer(botProperties);
        TaskTrackerPanelService service = new TaskTrackerPanelService(
                null, null, textRecognizer, null, null, new MapNameCanonicalizer());

        List<Path> samples;
        try (Stream<Path> stream = Files.walk(IMAGE_ROOT)) {
            samples = stream
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().contains("wubei_tracker_panel"))
                    .filter(path -> path.getFileName().toString().endsWith("_raw.png"))
                    .filter(path -> !path.getFileName().toString().endsWith("_wide_raw.png"))
                    .sorted(Comparator
                            .comparing((Path path) -> !path.getFileName().toString().contains("post-combat-chained"))
                            .thenComparing(Path::toString))
                    .limit(maxSamples)
                    .toList();
        }

        int ok = 0;
        int skipped = 0;
        int failed = 0;
        StringBuilder report = new StringBuilder("\ufeffstatus,file,yellow,links,distance,click,marked,reason\n");
        for (Path sample : samples) {
            BufferedImage image = ImageIO.read(sample.toFile());
            if (image == null) {
                failed++;
                report.append(csv("FAIL", sample, "", 0, -1, "", "", "IMAGE_READ_NULL")).append('\n');
                continue;
            }
            String yellowText = readYellowTextForReplay(textRecognizer, sample);
            if (!normalize(yellowText).contains("黄袍")) {
                skipped++;
                report.append(csv("SKIP", sample, yellowText, 0, -1, "", "", "NOT_HUANGPAO")).append('\n');
                image.flush();
                continue;
            }

            List<TaskTrackerGreenLink> links = service.scanWubeiTrackerGreenLinksForReplay(
                    image, 0, 0, sample.getFileName().toString(), yellowText);
            TaskTrackerPanelReadResult panel = TaskTrackerPanelReadResult.builder()
                    .found(true)
                    .detailRawPath(sample.toString())
                    .detailAbsoluteLeft(0)
                    .detailAbsoluteTop(0)
                    .yellowText(yellowText)
                    .greenLinks(links)
                    .build();
            Optional<PreparedDialogAction> action = service.prepareWubeiChainedTrackerFastAction(
                    panel, "replay-" + sample.getFileName());
            if (action.isEmpty()) {
                failed++;
                report.append(csv("FAIL", sample, yellowText, links.size(), -1, "", "", "NO_CACHE")).append('\n');
                image.flush();
                continue;
            }

            PreparedDialogAction cached = action.get();
            int distance = verifySameImage(image, cached);
            boolean matched = distance <= MAX_DISTANCE;
            Path marked = OUTPUT_DIR.resolve(sample.getFileName().toString().replace("_raw.png", "_chained_fast.png"));
            writeMarked(sample, marked, image, cached, matched, distance);
            if (matched) {
                ok++;
                report.append(csv("OK", sample, yellowText, links.size(), distance,
                        cached.getAbsoluteX() + ":" + cached.getAbsoluteY(), marked.toString(), "")).append('\n');
            } else {
                failed++;
                report.append(csv("FAIL", sample, yellowText, links.size(), distance,
                        cached.getAbsoluteX() + ":" + cached.getAbsoluteY(), marked.toString(), "DISTANCE")).append('\n');
            }
            image.flush();
        }
        Files.writeString(REPORT, report.toString(), StandardCharsets.UTF_8);
        System.out.printf("WUBEI_CHAINED_TRACKER_FAST_REPLAY samples=%d ok=%d skipped=%d failed=%d report=%s output=%s%n",
                samples.size(), ok, skipped, failed, REPORT, OUTPUT_DIR);
        if (ok == 0 || failed > 0) {
            throw new IllegalStateException("CR54 黄袍续战 tracker fast replay 未通过: ok=" + ok
                    + " skipped=" + skipped + " failed=" + failed);
        }
    }

    private static int verifySameImage(BufferedImage image, PreparedDialogAction cached) {
        BufferedImage crop = ImagePreprocessor.cropCopy(image,
                cached.getValidationLeft(),
                cached.getValidationTop(),
                cached.getValidationRight() - cached.getValidationLeft(),
                cached.getValidationBottom() - cached.getValidationTop());
        if (crop == null) {
            return Integer.MAX_VALUE;
        }
        BufferedImage washed = ImagePreprocessor.washGreenTextToBlackAndWhite(crop);
        try {
            return ImagePreprocessor.binaryFingerprintDistance(
                    cached.getFingerprint(),
                    ImagePreprocessor.buildBinaryFingerprint(washed));
        } finally {
            crop.flush();
            if (washed != crop) {
                washed.flush();
            }
        }
    }

    private static String readYellowTextForReplay(TextRecognizer textRecognizer, Path sample) {
        try {
            Path yellowPath = OUTPUT_DIR.resolve(sample.getFileName().toString().replace("_raw.png", "_yellow.png"));
            ImagePreprocessor.washYellowText(sample.toString(), yellowPath.toString());
            List<OcrWordResult> words = textRecognizer.getAllTextResultsForMatch(
                    yellowPath.toString(),
                    "wubei-chained-fast-replay-yellow:" + sample.getFileName(),
                    result -> !result.isEmpty());
            return words.stream()
                    .map(OcrWordResult::getText)
                    .filter(text -> text != null && !text.isBlank())
                    .collect(Collectors.joining("|"));
        } catch (Exception e) {
            return "";
        }
    }

    private static String normalize(String value) {
        return value == null ? "" : value.replaceAll("\\s+", "");
    }

    private static void writeMarked(Path sample,
                                    Path output,
                                    BufferedImage image,
                                    PreparedDialogAction cached,
                                    boolean matched,
                                    int distance) throws Exception {
        BufferedImage marked = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = marked.createGraphics();
        try {
            g.drawImage(image, 0, 0, null);
            g.setStroke(new BasicStroke(2));
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
            g.setColor(matched ? Color.GREEN : Color.RED);
            g.drawRect(cached.getValidationLeft(), cached.getValidationTop(),
                    cached.getValidationRight() - cached.getValidationLeft(),
                    cached.getValidationBottom() - cached.getValidationTop());
            g.setColor(Color.RED);
            int x = cached.getAbsoluteX();
            int y = cached.getAbsoluteY();
            g.drawLine(x - 5, y, x + 5, y);
            g.drawLine(x, y - 5, x, y + 5);
            g.drawString("CR54 d=" + distance + " click=(" + x + "," + y + ")",
                    4, Math.max(14, image.getHeight() - 18));
            g.drawString(sample.getFileName().toString(), 4, image.getHeight() - 4);
        } finally {
            g.dispose();
        }
        ImageIO.write(marked, "png", output.toFile());
        marked.flush();
    }

    private static String csv(String status,
                              Path file,
                              String yellow,
                              int links,
                              int distance,
                              String click,
                              String marked,
                              String reason) {
        return quote(status) + "," + quote(file.toString()) + "," + quote(yellow) + ","
                + links + "," + distance + "," + quote(click) + "," + quote(marked) + ","
                + quote(reason);
    }

    private static String quote(String value) {
        return "\"" + (value == null ? "" : value.replace("\"", "\"\"")) + "\"";
    }
}
