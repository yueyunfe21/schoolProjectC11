package com.bot.dhxy.debug;

import com.bot.dhxy.config.BotProperties;
import com.bot.dhxy.core.TextRecognizer;
import com.bot.dhxy.model.ocr.OcrWordResult;
import com.bot.dhxy.model.tasktracker.TaskTrackerGreenLink;
import com.bot.dhxy.service.MapNameCanonicalizer;
import com.bot.dhxy.service.TaskTrackerPanelService;
import com.bot.dhxy.tools.ImagePreprocessor;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Offline replay for 五倍 tracker green-link map-name parsing.
 *
 * <p>This debug entry reads saved left tracker panel images only. It does not capture windows,
 * focus windows, register dialog interest, or send input.</p>
 */
public class WubeiTrackerGreenMapReplayDebugMain {

    private static final Path ROOT = Path.of("").toAbsolutePath().normalize();
    private static final Path IMAGE_ROOT = ROOT.resolve("images/test-cases/task-tracker/wubei-task-panel/raw");
    private static final Path REPORT = ROOT.resolve("logs/wubei-tracker-green-map-replay.csv");
    private static final Path OUTPUT_DIR = ROOT.resolve("images/test-cases/task-tracker/wubei-task-panel/output/green-map");

    public static void main(String[] args) throws Exception {
        Files.createDirectories(REPORT.getParent());
        Files.createDirectories(OUTPUT_DIR);
        int maxSamples = args.length > 0 ? Integer.parseInt(args[0]) : 60;

        List<Path> samples;
        try (Stream<Path> stream = Files.walk(IMAGE_ROOT)) {
            samples = stream
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().contains("wubei_tracker_panel"))
                    .filter(path -> path.getFileName().toString().endsWith("_raw.png"))
                    .filter(path -> !path.getFileName().toString().endsWith("_wide_raw.png"))
                    .sorted(Comparator.comparing(Path::toString))
                    .limit(maxSamples)
                    .toList();
        }

        BotProperties botProperties = new BotProperties();
        botProperties.getOcr().setProvider("local");
        TextRecognizer textRecognizer = new TextRecognizer(botProperties);
        TaskTrackerPanelService service = new TaskTrackerPanelService(
                null, null, textRecognizer, null, null, new MapNameCanonicalizer());
        int ok = 0;
        int failed = 0;
        try (BufferedWriter writer = Files.newBufferedWriter(REPORT, StandardCharsets.UTF_8)) {
            writer.write('\ufeff');
            writer.write("status,file,yellow,links,targetMaps,reason");
            writer.newLine();
            for (Path sample : samples) {
                BufferedImage image = ImageIO.read(sample.toFile());
                if (image == null) {
                    failed++;
                    writer.write(csv("FAIL", sample, "", List.of(), "IMAGE_READ_NULL"));
                    writer.newLine();
                    continue;
                }

                String yellowText = readYellowTextForReplay(textRecognizer, sample);
                List<TaskTrackerGreenLink> links = service.scanWubeiTrackerGreenLinksForReplay(
                        image, 0, 0, sample.getFileName().toString(), yellowText);
                boolean hasMap = links.stream()
                        .anyMatch(link -> link.getTargetMapName() != null && !link.getTargetMapName().isBlank());
                boolean darkThunder = isDarkThunder(yellowText);
                if ((darkThunder && !hasMap) || (!darkThunder && hasMap)) {
                    ok++;
                    writeMarked(sample, OUTPUT_DIR.resolve(sample.getFileName().toString()), image, links);
                    writer.write(csv("OK", sample, yellowText, links, ""));
                } else {
                    failed++;
                    writer.write(csv("FAIL", sample, yellowText, links, darkThunder ? "DARK_THUNDER_HAS_MAP" : "NO_TARGET_MAP"));
                }
                writer.newLine();
            }
        }

        System.out.printf("WUBEI_TRACKER_GREEN_MAP_REPLAY samples=%d maxSamples=%d ok=%d failed=%d report=%s output=%s%n",
                samples.size(), maxSamples, ok, failed, REPORT, OUTPUT_DIR);
        if (ok == 0 || failed > 0) {
            throw new IllegalStateException("五倍 tracker 绿字地图名 replay 未全部通过: ok=" + ok + " failed=" + failed);
        }
    }

    private static String readYellowTextForReplay(TextRecognizer textRecognizer, Path sample) {
        try {
            Path yellowPath = OUTPUT_DIR.resolve(sample.getFileName().toString().replace("_raw.png", "_yellow.png"));
            ImagePreprocessor.washYellowText(sample.toString(), yellowPath.toString());
            List<OcrWordResult> words = textRecognizer.getAllTextResultsForMatch(
                    yellowPath.toString(),
                    "wubei-tracker-green-map-replay-yellow:" + sample.getFileName(),
                    result -> !result.isEmpty());
            return words.stream()
                    .map(OcrWordResult::getText)
                    .filter(text -> text != null && !text.isBlank())
                    .collect(Collectors.joining("|"));
        } catch (Exception e) {
            return "";
        }
    }

    private static boolean isDarkThunder(String yellowText) {
        return yellowText != null && yellowText.replaceAll("\\s+", "").contains("暗雷怪");
    }

    private static String csv(String status, Path file, String yellowText, List<TaskTrackerGreenLink> links, String reason) {
        String targetMaps = links.stream()
                .map(link -> link.getTargetMapName() == null ? "" : link.getTargetMapName())
                .filter(text -> !text.isBlank())
                .collect(Collectors.joining("|"));
        return quote(status) + "," + quote(file.toString()) + "," + quote(yellowText)
                + "," + quote(String.valueOf(links.size()))
                + "," + quote(targetMaps) + "," + quote(reason);
    }

    private static String quote(String value) {
        return "\"" + (value == null ? "" : value.replace("\"", "\"\"")) + "\"";
    }

    private static void writeMarked(Path sample,
                                    Path output,
                                    BufferedImage image,
                                    List<TaskTrackerGreenLink> links) throws Exception {
        BufferedImage marked = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = marked.createGraphics();
        try {
            g.drawImage(image, 0, 0, null);
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
            g.setStroke(new BasicStroke(2));
            for (TaskTrackerGreenLink link : links) {
                boolean hasMap = link.getTargetMapName() != null && !link.getTargetMapName().isBlank();
                g.setColor(hasMap ? Color.GREEN : Color.RED);
                g.drawRect(link.getMinX(), link.getMinY(), link.width(), link.height());
                g.drawString(link.getTargetMapName() == null ? "-" : link.getTargetMapName(),
                        Math.max(0, link.getMinX()), Math.max(12, link.getMinY() - 3));
            }
            g.setColor(Color.WHITE);
            g.drawString(sample.getFileName().toString(), 4, image.getHeight() - 4);
        } finally {
            g.dispose();
        }
        ImageIO.write(marked, "png", output.toFile());
    }
}
