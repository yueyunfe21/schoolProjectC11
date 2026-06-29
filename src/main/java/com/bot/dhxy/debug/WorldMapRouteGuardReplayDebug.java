package com.bot.dhxy.debug;

import com.bot.dhxy.config.BotProperties;
import com.bot.dhxy.core.TextRecognizer;
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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Offline replay for world-map route-result screenshots.
 *
 * <p>This tool does not capture windows or send input. It reuses the production destination guard
 * and legacy coordinate finder on saved route screenshots, then writes a red-marked image so the
 * CR99 yellow destination click candidate can be reviewed before changing live navigation behavior.</p>
 */
public class WorldMapRouteGuardReplayDebug {

    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    private static final Path DEFAULT_FAILURE_ROOT = Path.of("images", "failure-cases", "world-map-route");
    private static final Path ONLINE_DRY_RUN_ROOT = Path.of("images", "temp", "world_map_route_online_dry_run");
    private static final Path TESTCASE_RAW_ROOT = Path.of("images", "test-cases", "world-map-route", "raw");
    private static final List<String> UNSUPPORTED_DIRECT_ROUTE_MAPS =
            List.of("冰窟", "波月洞", "莲花洞", "金兜洞", "火云洞", "潮狮驼岭");
    private static final Pattern SUMMARY_ROW = Pattern.compile(
            "^\\d+,\"([^\"]+)\",(true|false),\"[^\"]*\",\"[^\"]*\",\"[^\"]*\",\"([^\"]+)\".*$");

    public static void main(String[] args) throws Exception {
        boolean failureAll = args.length > 0 && "--failure-all".equalsIgnoreCase(args[0]);
        boolean summaryAll = args.length > 0 && "--summary-all".equalsIgnoreCase(args[0]);
        boolean testcaseAll = args.length > 0 && "--testcase-all".equalsIgnoreCase(args[0]);
        String expected = failureAll || args.length == 0 ? System.getProperty("worldmap.route.expected", "长安") : args[0];
        List<RouteReplayCase> cases = summaryAll || testcaseAll
                ? summaryCases(testcaseAll)
                : toReplayCases(failureAll ? defaultImages(0) : (args.length <= 1 ? defaultImages(5) : explicitImages(args)),
                image -> failureAll ? expectedFromFailureCase(image, expected) : expected);
        Path outputDir = Path.of("images", "temp", "world_map_route_guard_replay",
                LocalDateTime.now().format(STAMP)).toAbsolutePath().normalize();
        Files.createDirectories(outputDir);

        BotProperties botProperties = new BotProperties();
        botProperties.getOcr().setProvider("local");
        GameTextLineOcrService service = new GameTextLineOcrService(new TextRecognizer(botProperties));

        int passed = 0;
        int failed = 0;
        for (RouteReplayCase replayCase : cases) {
            Path image = replayCase.image();
            String caseExpected = replayCase.expected();
            String raw = image.toString();
            GameTextLineOcrService.WorldMapRouteDestinationResult destination =
                    service.verifyWorldMapRouteDestination(raw, caseExpected);
            GameTextLineOcrService.WorldMapRouteCoordinateResult coordinate =
                    destination.allowClick()
                            ? service.findLastWorldMapRouteCoordinate(raw, destination)
                            : GameTextLineOcrService.WorldMapRouteCoordinateResult.builder()
                            .found(false)
                            .message("destination guard failed")
                            .build();
            Point yellowPoint = yellowDestinationPoint(destination);
            Path marked = outputDir.resolve(fileStem(image) + "_marked.png");
            writeMarkedImage(image, yellowPoint, destination, coordinate, marked);
            boolean ok = destination.allowClick() && yellowPoint != null;
            if (ok) {
                passed++;
            } else {
                failed++;
            }
            System.out.println("case=" + image
                    + " ok=" + ok
                    + " expected=" + caseExpected
                    + " actual=" + destination.rawActual()
                    + " allowClick=" + destination.allowClick()
                    + " yellowPoint=" + pointText(yellowPoint)
                    + " legacyGreenPoint=" + pointText(coordinate.relativeCenter())
                    + " marked=" + marked);
        }
        System.out.println("summary expected=" + expected
                + " total=" + cases.size()
                + " passed=" + passed
                + " failed=" + failed
                + " outputDir=" + outputDir);
    }

    private static List<Path> explicitImages(String[] args) {
        List<Path> images = new ArrayList<>();
        for (int i = 1; i < args.length; i++) {
            images.add(Path.of(args[i]).toAbsolutePath().normalize());
        }
        return images;
    }

    private static List<RouteReplayCase> summaryCases(boolean useTestcaseRaw) throws Exception {
        List<RouteReplayCase> cases = new ArrayList<>();
        if (!Files.isDirectory(ONLINE_DRY_RUN_ROOT)) {
            return cases;
        }
        Set<String> seen = new HashSet<>();
        try (var stream = Files.walk(ONLINE_DRY_RUN_ROOT, 2)) {
            List<Path> summaries = stream.filter(path -> path.getFileName() != null)
                    .filter(path -> "summary.csv".equalsIgnoreCase(path.getFileName().toString()))
                    .sorted()
                    .toList();
            for (Path summary : summaries) {
                for (String line : Files.readAllLines(summary)) {
                    Matcher matcher = SUMMARY_ROW.matcher(line);
                    if (!matcher.matches() || !"true".equalsIgnoreCase(matcher.group(2))) {
                        continue;
                    }
                    if (UNSUPPORTED_DIRECT_ROUTE_MAPS.contains(matcher.group(1))) {
                        continue;
                    }
                    Path raw = useTestcaseRaw ? testcaseRawPath(Path.of(matcher.group(3))) : Path.of(matcher.group(3));
                    if (Files.exists(raw)) {
                        String key = raw.toAbsolutePath().normalize() + "|" + matcher.group(1);
                        if (seen.add(key)) {
                            cases.add(new RouteReplayCase(raw.toAbsolutePath().normalize(), matcher.group(1)));
                        }
                    }
                }
            }
        }
        Path manual = TESTCASE_RAW_ROOT.resolve("img.png");
        if (useTestcaseRaw && Files.exists(manual)) {
            String manualExpected = System.getProperty("worldmap.route.manualExpected", "长安");
            String key = manual.toAbsolutePath().normalize() + "|" + manualExpected;
            if (seen.add(key)) {
                cases.add(new RouteReplayCase(manual.toAbsolutePath().normalize(), manualExpected));
            }
        }
        return cases;
    }

    private static Path testcaseRawPath(Path raw) {
        Path normalized = raw.toAbsolutePath().normalize();
        Path base = Path.of("").toAbsolutePath().normalize();
        String name;
        try {
            name = base.relativize(normalized).toString();
        } catch (Exception ignored) {
            name = normalized.toString();
        }
        name = name.replace(':', '_')
                .replace("\\", "__")
                .replace("/", "__");
        return TESTCASE_RAW_ROOT.resolve(name);
    }

    private interface ExpectedResolver {
        String expected(Path image);
    }

    private static List<RouteReplayCase> toReplayCases(List<Path> images, ExpectedResolver resolver) {
        List<RouteReplayCase> cases = new ArrayList<>();
        for (Path image : images) {
            cases.add(new RouteReplayCase(image, resolver.expected(image)));
        }
        return cases;
    }

    private static List<Path> defaultImages(int limit) throws Exception {
        List<Path> images = new ArrayList<>();
        if (!Files.isDirectory(DEFAULT_FAILURE_ROOT)) {
            return images;
        }
        try (var stream = Files.walk(DEFAULT_FAILURE_ROOT, 2)) {
            var filtered = stream.filter(path -> path.getFileName() != null)
                    .filter(path -> "raw.png".equalsIgnoreCase(path.getFileName().toString()))
                    .sorted((a, b) -> {
                        try {
                            return Files.getLastModifiedTime(b).compareTo(Files.getLastModifiedTime(a));
                        } catch (Exception e) {
                            return 0;
                        }
                    });
            (limit > 0 ? filtered.limit(limit) : filtered).forEach(images::add);
        }
        return images;
    }

    private static String expectedFromFailureCase(Path image, String fallback) {
        Path parent = image.getParent();
        if (parent == null || parent.getFileName() == null) {
            return fallback;
        }
        String name = parent.getFileName().toString();
        int suffix = name.indexOf("_destination-mismatch");
        if (suffix <= 0) {
            return fallback;
        }
        int start = name.lastIndexOf('_', suffix - 1);
        if (start < 0 || start + 1 >= suffix) {
            return fallback;
        }
        String expected = name.substring(start + 1, suffix);
        return expected.isBlank() ? fallback : expected;
    }

    private static void writeMarkedImage(Path rawPath,
                                         Point yellowPoint,
                                         GameTextLineOcrService.WorldMapRouteDestinationResult destination,
                                         GameTextLineOcrService.WorldMapRouteCoordinateResult coordinate,
                                         Path outputPath) throws Exception {
        BufferedImage image = ImageIO.read(rawPath.toFile());
        if (image == null) {
            return;
        }
        try {
            Graphics2D g = image.createGraphics();
            try {
                g.setColor(Color.RED);
                g.setStroke(new BasicStroke(3));
                g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
                if (yellowPoint != null) {
                    g.drawOval(yellowPoint.x - 9, yellowPoint.y - 9, 18, 18);
                    g.drawLine(yellowPoint.x - 14, yellowPoint.y, yellowPoint.x + 14, yellowPoint.y);
                    g.drawLine(yellowPoint.x, yellowPoint.y - 14, yellowPoint.x, yellowPoint.y + 14);
                    g.drawString("YELLOW CLICK", Math.min(yellowPoint.x + 12, image.getWidth() - 95),
                            Math.max(14, yellowPoint.y - 12));
                } else {
                    g.drawString("NO POINT", 8, Math.max(16, image.getHeight() - 8));
                }
                if (destination.destinationCenterX() != null && destination.destinationCenterY() != null) {
                    int destX = destination.destinationCenterX();
                    int destY = destination.destinationCenterY();
                    g.drawRect(destX - 14, destY - 12, 28, 24);
                    g.drawLine(destX - 18, destY, destX + 18, destY);
                    g.drawLine(destX, destY - 16, destX, destY + 16);
                    g.drawString("DEST", Math.min(destX + 16, image.getWidth() - 40),
                            Math.max(14, destY - 14));
                }
                if (coordinate.relativeCenter() != null) {
                    Point legacyPoint = coordinate.relativeCenter();
                    g.setColor(new Color(0, 150, 0));
                    g.drawOval(legacyPoint.x - 7, legacyPoint.y - 7, 14, 14);
                    g.drawLine(legacyPoint.x - 10, legacyPoint.y, legacyPoint.x + 10, legacyPoint.y);
                    g.drawLine(legacyPoint.x, legacyPoint.y - 10, legacyPoint.x, legacyPoint.y + 10);
                    g.drawString("LEGACY GREEN", Math.min(legacyPoint.x + 10, image.getWidth() - 95),
                            Math.max(14, legacyPoint.y - 10));
                    g.setColor(Color.RED);
                }
                g.drawString("dest=" + nullToDash(destination.rawActual())
                                + " allow=" + destination.allowClick()
                                + " yellowPoint=" + (yellowPoint != null)
                                + " legacyCoord=" + coordinate.found(),
                        8, image.getHeight() - 8);
            } finally {
                g.dispose();
            }
            Files.createDirectories(outputPath.getParent());
            ImageIO.write(image, "png", outputPath.toFile());
        } finally {
            image.flush();
        }
    }

    private static String fileStem(Path path) {
        String name = path.getFileName().toString();
        int dot = name.lastIndexOf('.');
        return dot > 0 ? name.substring(0, dot) : name;
    }

    private static String pointText(Point point) {
        return point == null ? "-" : "(" + point.x + "," + point.y + ")";
    }

    private static Point yellowDestinationPoint(GameTextLineOcrService.WorldMapRouteDestinationResult destination) {
        return destination.destinationCenterX() == null || destination.destinationCenterY() == null
                ? null
                : new Point(destination.destinationCenterX(), destination.destinationCenterY());
    }

    private static String nullToDash(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private static final class RouteReplayCase {
        private final Path image;
        private final String expected;

        private RouteReplayCase(Path image, String expected) {
            this.image = image;
            this.expected = expected;
        }

        private Path image() {
            return image;
        }

        private String expected() {
            return expected;
        }
    }
}
