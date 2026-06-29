package com.bot.dhxy.debug;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

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
import java.util.Map;

/**
 * Offline replay for CR99 destination mini-map coordinate mapping.
 *
 * <p>The live CR99 path clicks a yellow world-map route destination link first; the game then opens
 * that destination map's mini-map. This tool replays the second click without window input by using
 * the same {@code config/maps.json} transform math in window-relative pixels and writing a marked
 * 1024x768 testcase image.</p>
 */
public class WorldMapYellowMiniMapCoordinateReplayDebug {

    private static final int GAME_CLIENT_WIDTH = 1024;
    private static final int GAME_CLIENT_HEIGHT = 768;
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    public static void main(String[] args) throws Exception {
        String mapName = args.length > 0 ? args[0] : System.getProperty("worldmap.yellow.minimap.map", "长安");
        int targetX = args.length > 1 ? Integer.parseInt(args[1])
                : Integer.getInteger("worldmap.yellow.minimap.x", 224);
        int targetY = args.length > 2 ? Integer.parseInt(args[2])
                : Integer.getInteger("worldmap.yellow.minimap.y", 100);
        Path output = args.length > 3
                ? Path.of(args[3]).toAbsolutePath().normalize()
                : Path.of("images", "test-cases", "minimap", "world-map-yellow-output",
                LocalDateTime.now().format(STAMP),
                sanitize(mapName) + "_" + targetX + "_" + targetY + "_marked.png")
                .toAbsolutePath().normalize();

        Map<String, MapTransform> transforms = new ObjectMapper().readValue(
                Path.of("config", "maps.json").toFile(),
                new TypeReference<>() {
                });
        MapTransform transform = transforms.get(mapName);
        if (transform == null) {
            throw new IllegalArgumentException("Missing map transform: " + mapName);
        }
        Point point = new Point(
                (int) Math.round(transform.zeroOffsetX + targetX * transform.scaleX),
                (int) Math.round(transform.zeroOffsetY + targetY * transform.scaleY));
        writeMarkedImage(mapName, targetX, targetY, point, output);
        System.out.println("map=" + mapName
                + " target=(" + targetX + "," + targetY + ")"
                + " windowRelativePoint=(" + point.x + "," + point.y + ")"
                + " output=" + output);
    }

    private static void writeMarkedImage(String mapName,
                                         int targetX,
                                         int targetY,
                                         Point point,
                                         Path output) throws Exception {
        BufferedImage image = new BufferedImage(GAME_CLIENT_WIDTH, GAME_CLIENT_HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        try {
            g.setColor(new Color(236, 238, 231));
            g.fillRect(0, 0, image.getWidth(), image.getHeight());
            g.setColor(new Color(185, 190, 180));
            for (int x = 0; x < image.getWidth(); x += 64) {
                g.drawLine(x, 0, x, image.getHeight());
            }
            for (int y = 0; y < image.getHeight(); y += 64) {
                g.drawLine(0, y, image.getWidth(), y);
            }
            g.setStroke(new BasicStroke(4));
            g.setColor(Color.DARK_GRAY);
            g.drawRect(2, 2, image.getWidth() - 5, image.getHeight() - 5);
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
            g.drawString("CR99 destination mini-map coordinate replay", 18, 28);
            g.drawString("map=" + mapName + " target=(" + targetX + "," + targetY + ")", 18, 52);

            g.setColor(Color.RED);
            g.setStroke(new BasicStroke(3));
            g.drawOval(point.x - 10, point.y - 10, 20, 20);
            g.drawLine(point.x - 18, point.y, point.x + 18, point.y);
            g.drawLine(point.x, point.y - 18, point.x, point.y + 18);
            g.drawString("FINAL MINI-MAP CLICK", Math.min(point.x + 14, image.getWidth() - 180),
                    Math.max(20, point.y - 14));
            g.drawString("windowRelative=(" + point.x + "," + point.y + ")", 18, image.getHeight() - 18);
        } finally {
            g.dispose();
        }
        Files.createDirectories(output.getParent());
        ImageIO.write(image, "png", output.toFile());
    }

    private static String sanitize(String value) {
        return value == null || value.isBlank()
                ? "unknown"
                : value.replaceAll("[\\\\/:*?\"<>|\\s]+", "_");
    }

    public static class MapTransform {
        public int zeroOffsetX;
        public int zeroOffsetY;
        public double scaleX;
        public double scaleY;
    }
}
