package com.bot.dhxy.debug;

import com.bot.dhxy.model.navigation.ObjectiveTextResult;
import com.bot.dhxy.tools.CoordinateHelper;
import com.bot.dhxy.vision.ObjectiveTextRecognitionService;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Replays saved objective story screenshots without touching a live game window.
 *
 * <p>Use this after changing objective map/coordinate recognition. It verifies that known raw
 * screenshots resolve to the expected map and coordinate, then writes a marked output image for
 * visual inspection.</p>
 */
public class ObjectiveTextRecognitionReplayDebugMain {

    private static final Path ROOT = Path.of("images", "test-cases", "objective-text");
    private static final Path RAW_DIR = ROOT.resolve("raw");
    private static final Path OUTPUT_DIR = ROOT.resolve("output");
    private static final Path MANIFEST = ROOT.resolve("manifest.csv");

    public static void main(String[] args) throws Exception {
        CoordinateHelper coordinateHelper = new CoordinateHelper(null, null);
        coordinateHelper.loadMapConfig();
        ObjectiveTextRecognitionService service = new ObjectiveTextRecognitionService(null, coordinateHelper);

        Files.createDirectories(OUTPUT_DIR);
        List<TestCase> cases = readManifest();
        int failed = 0;
        for (TestCase testCase : cases) {
            ReplayResult result = replayOne(service, testCase);
            System.out.println(result.summary());
            if (!result.passed()) {
                failed++;
            }
        }
        System.out.printf("OBJECTIVE_TEXT_REPLAY total=%d passed=%d failed=%d%n",
                cases.size(), cases.size() - failed, failed);
        if (failed > 0) {
            System.exit(1);
        }
    }

    private static List<TestCase> readManifest() throws IOException {
        List<TestCase> cases = new ArrayList<>();
        if (!Files.isRegularFile(MANIFEST)) {
            return cases;
        }
        for (String line : Files.readAllLines(MANIFEST, StandardCharsets.UTF_8)) {
            if (line.isBlank() || line.startsWith("#") || line.startsWith("file,")) {
                continue;
            }
            String[] parts = line.split(",", 5);
            if (parts.length < 5) {
                throw new IllegalArgumentException("Bad manifest line: " + line);
            }
            cases.add(new TestCase(
                    RAW_DIR.resolve(parts[0].trim()),
                    parts[1].trim(),
                    Integer.parseInt(parts[2].trim()),
                    Integer.parseInt(parts[3].trim()),
                    parts[4].trim()
            ));
        }
        return cases;
    }

    private static ReplayResult replayOne(ObjectiveTextRecognitionService service, TestCase testCase) throws IOException {
        Optional<ObjectiveTextResult> actual = service.recognize(testCase.imagePath(), "objective-replay:" + testCase.source());
        boolean passed = actual
                .filter(result -> testCase.mapName().equals(result.mapName()))
                .filter(result -> testCase.x() == result.x())
                .filter(result -> testCase.y() == result.y())
                .isPresent();
        Path marked = OUTPUT_DIR.resolve(stripExtension(testCase.imagePath().getFileName().toString()) + "_marked.png");
        writeMarkedImage(testCase, actual, passed, marked);
        return new ReplayResult(testCase, actual, passed, marked);
    }

    private static void writeMarkedImage(TestCase testCase,
                                         Optional<ObjectiveTextResult> actual,
                                         boolean passed,
                                         Path outputPath) throws IOException {
        BufferedImage raw = ImageIO.read(testCase.imagePath().toFile());
        if (raw == null) {
            throw new IOException("Cannot read " + testCase.imagePath());
        }
        BufferedImage marked = new BufferedImage(raw.getWidth(), raw.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = marked.createGraphics();
        try {
            g.drawImage(raw, 0, 0, null);
            g.setColor(passed ? new Color(0, 190, 0) : Color.RED);
            g.setStroke(new BasicStroke(2));
            g.drawRect(1, 1, marked.getWidth() - 3, marked.getHeight() - 3);
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
            g.drawString(passed ? "PASS" : "FAIL", 8, Math.min(marked.getHeight() - 8, 18));

            String expected = "expected=" + testCase.mapName() + "(" + testCase.x() + "," + testCase.y() + ")";
            String actualText = actual
                    .map(result -> "actual=" + result.mapName() + "(" + result.x() + "," + result.y() + ")")
                    .orElse("actual=<empty>");
            g.setColor(Color.RED);
            g.drawString(expected, 8, Math.min(marked.getHeight() - 24, 36));
            g.drawString(actualText, 8, Math.min(marked.getHeight() - 8, 54));
        } finally {
            g.dispose();
            raw.flush();
        }
        ImageIO.write(marked, "png", outputPath.toFile());
        marked.flush();
    }

    private static String stripExtension(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot > 0 ? fileName.substring(0, dot) : fileName;
    }

    private record TestCase(Path imagePath, String mapName, int x, int y, String source) {
    }

    private record ReplayResult(TestCase testCase,
                                Optional<ObjectiveTextResult> actual,
                                boolean passed,
                                Path markedPath) {
        private String summary() {
            String actualText = actual
                    .map(result -> result.mapName() + "(" + result.x() + "," + result.y() + ")")
                    .orElse("<empty>");
            return (passed ? "PASS " : "FAIL ")
                    + testCase.imagePath().getFileName()
                    + " expected=" + testCase.mapName() + "(" + testCase.x() + "," + testCase.y() + ")"
                    + " actual=" + actualText
                    + " marked=" + markedPath;
        }
    }
}
