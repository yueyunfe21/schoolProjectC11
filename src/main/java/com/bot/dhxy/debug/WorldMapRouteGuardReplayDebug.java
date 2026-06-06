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
import java.util.List;

/**
 * Offline replay for world-map route-result screenshots.
 *
 * <p>This tool does not capture windows or send input. It reuses the production destination guard
 * and coordinate finder on saved route screenshots, then writes a red-marked image so route-click
 * candidates can be reviewed before changing live navigation behavior.</p>
 */
public class WorldMapRouteGuardReplayDebug {

    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    private static final Path DEFAULT_FAILURE_ROOT = Path.of("images", "failure-cases", "world-map-route");

    public static void main(String[] args) throws Exception {
        String expected = args.length == 0 ? System.getProperty("worldmap.route.expected", "长安") : args[0];
        List<Path> images = args.length <= 1 ? defaultImages() : explicitImages(args);
        Path outputDir = Path.of("images", "temp", "world_map_route_guard_replay",
                LocalDateTime.now().format(STAMP)).toAbsolutePath().normalize();
        Files.createDirectories(outputDir);

        BotProperties botProperties = new BotProperties();
        botProperties.getOcr().setProvider("local");
        GameTextLineOcrService service = new GameTextLineOcrService(new TextRecognizer(botProperties));

        int passed = 0;
        int failed = 0;
        for (Path image : images) {
            String raw = image.toString();
            GameTextLineOcrService.WorldMapRouteDestinationResult destination =
                    service.verifyWorldMapRouteDestination(raw, expected);
            GameTextLineOcrService.WorldMapRouteCoordinateResult coordinate =
                    destination.allowClick()
                            ? service.findLastWorldMapRouteCoordinate(raw, destination)
                            : GameTextLineOcrService.WorldMapRouteCoordinateResult.builder()
                            .found(false)
                            .message("destination guard failed")
                            .build();
            Path marked = outputDir.resolve(fileStem(image) + "_marked.png");
            writeMarkedImage(image, coordinate.relativeCenter(), destination, coordinate, marked);
            boolean ok = destination.allowClick() && coordinate.found();
            if (ok) {
                passed++;
            } else {
                failed++;
            }
            System.out.println("case=" + image
                    + " ok=" + ok
                    + " expected=" + expected
                    + " actual=" + destination.rawActual()
                    + " allowClick=" + destination.allowClick()
                    + " point=" + pointText(coordinate.relativeCenter())
                    + " marked=" + marked);
        }
        System.out.println("summary expected=" + expected
                + " total=" + images.size()
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

    private static List<Path> defaultImages() throws Exception {
        List<Path> images = new ArrayList<>();
        if (!Files.isDirectory(DEFAULT_FAILURE_ROOT)) {
            return images;
        }
        try (var stream = Files.walk(DEFAULT_FAILURE_ROOT, 2)) {
            stream.filter(path -> path.getFileName() != null)
                    .filter(path -> "raw.png".equalsIgnoreCase(path.getFileName().toString()))
                    .sorted((a, b) -> {
                        try {
                            return Files.getLastModifiedTime(b).compareTo(Files.getLastModifiedTime(a));
                        } catch (Exception e) {
                            return 0;
                        }
                    })
                    .limit(5)
                    .forEach(images::add);
        }
        return images;
    }

    private static void writeMarkedImage(Path rawPath,
                                         Point point,
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
                if (point != null) {
                    g.drawOval(point.x - 9, point.y - 9, 18, 18);
                    g.drawLine(point.x - 14, point.y, point.x + 14, point.y);
                    g.drawLine(point.x, point.y - 14, point.x, point.y + 14);
                    g.drawString("CLICK", Math.min(point.x + 12, image.getWidth() - 45),
                            Math.max(14, point.y - 12));
                } else {
                    g.drawString("NO POINT", 8, Math.max(16, image.getHeight() - 8));
                }
                g.drawString("dest=" + nullToDash(destination.rawActual())
                                + " allow=" + destination.allowClick()
                                + " coord=" + coordinate.found(),
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
        String name = path.getParent() == null ? path.getFileName().toString() : path.getParent().getFileName().toString();
        int dot = name.lastIndexOf('.');
        return dot > 0 ? name.substring(0, dot) : name;
    }

    private static String pointText(Point point) {
        return point == null ? "-" : "(" + point.x + "," + point.y + ")";
    }

    private static String nullToDash(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }
}
