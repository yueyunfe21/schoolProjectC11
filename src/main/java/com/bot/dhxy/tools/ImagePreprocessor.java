package com.bot.dhxy.tools;

import lombok.extern.slf4j.Slf4j;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Slf4j
public class ImagePreprocessor {
    public static boolean ENABLE_DEBUG_SAVE = true;
    private static final String DEBUG_DIR = "images/temp/";

    /**
     * Copy a rectangular region from an image into an independent RGB image.
     *
     * @param source source image; ownership stays with caller.
     * @param x image-local left coordinate.
     * @param y image-local top coordinate.
     * @param width crop width in pixels.
     * @param height crop height in pixels.
     * @return independent crop copy, or null when the requested rectangle is outside the image.
     */
    public static BufferedImage cropCopy(BufferedImage source, int x, int y, int width, int height) {
        if (source == null || width <= 0 || height <= 0
                || x < 0 || y < 0
                || x + width > source.getWidth()
                || y + height > source.getHeight()) {
            return null;
        }
        BufferedImage subimage = source.getSubimage(x, y, width, height);
        BufferedImage copy = new BufferedImage(subimage.getWidth(), subimage.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = copy.createGraphics();
        try {
            graphics.drawImage(subimage, 0, 0, null);
        } finally {
            graphics.dispose();
        }
        return copy;
    }

    /**
     * Crop a screen-absolute rectangle from an already captured screen-absolute source rectangle.
     *
     * @param source screenshot image whose bounds are described by {@code sourceRect}.
     * @param sourceRect screen-absolute source bounds as {@code [left, top, right, bottom]}.
     * @param targetRect screen-absolute requested crop bounds as {@code [left, top, right, bottom]}.
     * @return independent crop copy, or null when the rectangles do not overlap.
     */
    public static BufferedImage cropAbsoluteRect(BufferedImage source, int[] sourceRect, int[] targetRect) {
        if (source == null || sourceRect == null || sourceRect.length < 4
                || targetRect == null || targetRect.length < 4) {
            return null;
        }
        int left = Math.max(targetRect[0], sourceRect[0]);
        int top = Math.max(targetRect[1], sourceRect[1]);
        int right = Math.min(targetRect[2], sourceRect[2]);
        int bottom = Math.min(targetRect[3], sourceRect[3]);
        if (right <= left || bottom <= top) {
            log.warn("image crop outside source: source={} target={}", rectToString(sourceRect), rectToString(targetRect));
            return null;
        }
        return cropCopy(source, left - sourceRect[0], top - sourceRect[1], right - left, bottom - top);
    }

    public static String rectToString(int[] rect) {
        if (rect == null || rect.length < 4) {
            return "-";
        }
        return "[" + rect[0] + "," + rect[1] + "," + rect[2] + "," + rect[3] + "]";
    }

    public static void saveDebugImage(BufferedImage img, String fileName) {
        if (!ENABLE_DEBUG_SAVE || img == null) {
            return;
        }
        try {
            File output = resolveDebugOutput(fileName);
            ImageIO.write(img, "png", output);
        } catch (Exception e) {
            log.error("save debug image failed: {}", fileName, e);
        }
    }

    /**
     * Write an image to an explicit filesystem path.
     *
     * @param image source image; ownership stays with caller.
     * @param path destination path. Parent directories are created when needed.
     * @return true when the PNG was written successfully.
     */
    public static boolean saveImage(BufferedImage image, String path) {
        if (image == null || path == null || path.isBlank()) {
            return false;
        }
        try {
            File file = new File(path);
            File parent = file.getParentFile();
            if (parent != null && !parent.exists() && !parent.mkdirs()) {
                log.warn("image output directory could not be created: {}", parent);
                return false;
            }
            ImageIO.write(image, "png", file);
            return true;
        } catch (IOException e) {
            log.warn("image save failed: path={}", path, e);
            return false;
        }
    }

    public static BufferedImage pathToBufferedImage(String imagePath) {
        try {
            return ImageIO.read(new File(imagePath));
        } catch (IOException e) {
            log.error("read image failed: {}", imagePath, e);
            return null;
        }
    }

    private static File resolveDebugOutput(String fileName) throws IOException {
        String value = fileName == null || fileName.isBlank() ? "debug.png" : fileName.trim();
        Path path = Path.of(value);
        if (path.getParent() == null) {
            path = Path.of(DEBUG_DIR, value);
        }
        if (path.getParent() != null) {
            Files.createDirectories(path.getParent());
        }
        return path.toFile();
    }
}
