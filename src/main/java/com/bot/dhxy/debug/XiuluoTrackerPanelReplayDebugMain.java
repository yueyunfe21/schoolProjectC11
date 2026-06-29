package com.bot.dhxy.debug;

import com.bot.dhxy.model.tasktracker.TaskTrackerGreenLink;
import com.bot.dhxy.model.tasktracker.TaskTrackerPanelReadResult;
import com.bot.dhxy.service.TaskTrackerPanelService;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * Offline replay for CR81 修罗 tracker title/green-link read-only detection.
 *
 * <p>This tool reads saved task-tracker panel images only. It does not capture windows,
 * send input, register pathing intent, or touch 修罗 task state.</p>
 */
public class XiuluoTrackerPanelReplayDebugMain {

    private static final Path ROOT = Path.of("").toAbsolutePath().normalize();
    private static final Path RAW_DIR = ROOT.resolve("images/test-cases/task-tracker/xiuluo-task-panel/raw");
    private static final Path OUTPUT_DIR = ROOT.resolve("images/test-cases/task-tracker/xiuluo-task-panel/output");
    private static final Path REPORT = ROOT.resolve("logs/xiuluo-tracker-panel-replay.csv");
    private static final Path SOURCE_TITLE = ROOT.resolve("images/template/xiuluo/Snipaste_2026-06-23_12-57-46.png");

    public static void main(String[] args) throws Exception {
        Files.createDirectories(RAW_DIR);
        Files.createDirectories(OUTPUT_DIR);
        Files.createDirectories(REPORT.getParent());
        ensureSyntheticSample();

        List<Path> samples;
        try (Stream<Path> stream = Files.walk(RAW_DIR)) {
            samples = stream
                .filter(Files::isRegularFile)
                .filter(path -> path.getFileName().toString().endsWith("_raw.png"))
                .sorted(Comparator.comparing(Path::toString))
                .toList();
        }

        TaskTrackerPanelService service = new TaskTrackerPanelService(null, null, null, null, null, null);
        int ok = 0;
        int failed = 0;
        try (BufferedWriter writer = Files.newBufferedWriter(REPORT, StandardCharsets.UTF_8)) {
            writer.write('\ufeff');
            writer.write("status,file,found,links,click,marked,reason");
            writer.newLine();
            for (Path sample : samples) {
                Path marked = OUTPUT_DIR.resolve(sample.getFileName().toString().replace("_raw.png", "_marked.png"));
                TaskTrackerPanelReadResult result = service.readXiuluoTrackerPanelForReplay(
                    sample, 0, 0, sample.getFileName().toString(), marked);
                if (result.isFound() && !result.getGreenLinks().isEmpty()) {
                    ok++;
                    Point click = service.resolveXiuluoTrackerGreenClickPoint(result)
                        .orElse(result.getGreenLinks().get(0).centerPoint());
                    writer.write(csv("OK", sample, true, result.getGreenLinks().size(),
                        click.x + ":" + click.y, marked, ""));
                } else {
                    failed++;
                    writer.write(csv("FAIL", sample, result.isFound(), result.getGreenLinks().size(),
                        "", marked, "NO_TITLE_OR_GREEN_LINK"));
                }
                writer.newLine();
            }
        }

        System.out.printf("XIULUO_TRACKER_PANEL_REPLAY samples=%d ok=%d failed=%d report=%s output=%s%n",
            samples.size(), ok, failed, REPORT, OUTPUT_DIR);
        if (ok == 0 || failed > 0) {
            throw new IllegalStateException("CR81 修罗 tracker replay 未全部通过: ok=" + ok + " failed=" + failed);
        }
    }

    private static void ensureSyntheticSample() throws Exception {
        Path sample = RAW_DIR.resolve("xiuluo_tracker_panel_synthetic_raw.png");
        if (Files.isRegularFile(sample)) {
            return;
        }
        BufferedImage title = ImageIO.read(SOURCE_TITLE.toFile());
        if (title == null) {
            throw new IllegalStateException("修罗 tracker title source unreadable: " + SOURCE_TITLE);
        }

        BufferedImage image = new BufferedImage(190, 76, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        try {
            g.setColor(new Color(18, 24, 18));
            g.fillRect(0, 0, image.getWidth(), image.getHeight());
            g.drawImage(title, 8, 4, null);
            g.setFont(new Font("Microsoft YaHei", Font.BOLD, 13));
            g.setColor(new Color(0, 255, 0));
            g.drawString("前往长安找修罗。", 8, 42);
        } finally {
            g.dispose();
        }
        ImageIO.write(image, "png", sample.toFile());
    }

    private static String csv(String status,
                              Path file,
                              boolean found,
                              int links,
                              String click,
                              Path marked,
                              String reason) {
        return quote(status) + "," + quote(file.toString()) + "," + quote(String.valueOf(found))
            + "," + quote(String.valueOf(links)) + "," + quote(click)
            + "," + quote(marked.toString()) + "," + quote(reason);
    }

    private static String quote(String value) {
        return "\"" + (value == null ? "" : value.replace("\"", "\"\"")) + "\"";
    }
}
