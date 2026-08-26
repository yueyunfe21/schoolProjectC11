package com.bot.dhxy.tools;

import lombok.extern.slf4j.Slf4j;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import javax.imageio.ImageIO;
import java.awt.Color;
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

    // W-696-DIALOG-DETECTION-LOCAL-MECHANICS-1: targeted add-back of the committed 696a12b0 pure
    // CPU/OpenCV dialog-classification helpers required by the local dialog-detection mechanics. Bodies,
    // thresholds, and Mat release/debug behavior are copied verbatim from the baseline; no existing
    // method/comment in this file was changed.
    public static int countGreenPixelsHSV(BufferedImage img) {
        return countGreenPixelsHSV(img, "debug_hsv_mask_green.png");
    }

    public static int countGreenPixelsHSV(BufferedImage img, String debugOutputPath) {
        if (img == null) return 0;

        BufferedImage convertedImg = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
        Graphics2D graphics = convertedImg.createGraphics();
        try {
            graphics.drawImage(img, 0, 0, null);
        } finally {
            graphics.dispose();
        }
        byte[] data = ((java.awt.image.DataBufferByte) convertedImg.getRaster().getDataBuffer()).getData();
        Mat src = new Mat(img.getHeight(), img.getWidth(), CvType.CV_8UC3);
        src.put(0, 0, data);
        convertedImg.flush();

        Mat hsv = new Mat();
        Imgproc.cvtColor(src, hsv, Imgproc.COLOR_BGR2HSV);

        Scalar lowerGreen = new Scalar(50, 150, 180);
        Scalar upperGreen = new Scalar(75, 255, 255);

        Mat mask = new Mat();
        Core.inRange(hsv, lowerGreen, upperGreen, mask);

        if (debugOutputPath != null && !debugOutputPath.isBlank()) {
            saveDebugImage(mask, debugOutputPath);
        }

        int count = Core.countNonZero(mask);

        src.release();
        hsv.release();
        mask.release();

        return count;
    }

    // G102 收口（2026-08-24）：Dialog 方差门唯一调用者已删，本方法一并移除；禁止重新引入。

    public static void saveDebugImage(Mat mat, String fileName) {
        if (!ENABLE_DEBUG_SAVE || mat == null || mat.empty()) return;

        try {
            File output = resolveDebugOutput(fileName);
            Imgcodecs.imwrite(output.getPath(), mat);
        } catch (Exception e) {
            log.error("save debug image failed: {}", fileName, e);
        }
    }

    // === W-696-NPC-CTRL-PROBE-LOCAL-MECHANICS-1-R3: pure-local yellow-menu and dialog-option
    // template washes restored verbatim from 696a12b0 (no Cloud ImageProcessorService routing).
    // Only the methods required this round are added; all existing content above is preserved. ===


    /**
     * Wash task option text (green, plus the highlighted-yellow selected option) to a binary image
     * for template matching only. Restored verbatim from {@code 696a12b0}.
     */
    public static void washDialogOptionTemplateTextToBlackAndWhite(String inputPath, String outputPath) {
        try {
            BufferedImage img = ImageIO.read(new File(inputPath));
            if (img == null) return;

            BufferedImage out = washDialogOptionTemplateTextToBlackAndWhite(img);
            ImageIO.write(out, "png", new File(outputPath));
            img.flush();
            out.flush();
        } catch (Exception e) {
            log.error("wash dialog option template text failed", e);
        }
    }

    public static BufferedImage washDialogOptionTemplateTextToBlackAndWhite(BufferedImage img) {
        int width = img.getWidth();
        int height = img.getHeight();
        BufferedImage out = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_BINARY);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = img.getRGB(x, y);
                out.setRGB(x, y, isOptionGreen(rgb) || isHighlightedOptionYellow(rgb) ? 0xFFFFFF : 0x000000);
            }
        }
        return out;
    }

    public static boolean isOptionGreen(int rgb) {
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        return g > 80 && (g - r) > 40 && (g - b) > 40;
    }

    private static boolean isHighlightedOptionYellow(int rgb) {
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        return r > 180 && g > 180 && b < 80 && Math.abs(r - g) < 45;
    }

    public static BufferedImage washGreenTextToBlackAndWhite(BufferedImage img) {
        int width = img.getWidth();
        int height = img.getHeight();
        BufferedImage out = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_BINARY);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                out.setRGB(x, y, isOptionGreen(img.getRGB(x, y)) ? 0xFFFFFF : 0x000000);
            }
        }
        return out;
    }


    public static String buildBinaryFingerprint(BufferedImage image) {
        if (image == null || image.getWidth() <= 0 || image.getHeight() <= 0) {
            return "";
        }
        StringBuilder bits = new StringBuilder((image.getWidth() * image.getHeight() + 3) / 4);
        int nibble = 0;
        int bitCount = 0;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int rgb = image.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;
                int luminance = (r + g + b) / 3;
                nibble = (nibble << 1) | (luminance >= 128 ? 1 : 0);
                bitCount++;
                if (bitCount == 4) {
                    bits.append(Character.forDigit(nibble, 16));
                    nibble = 0;
                    bitCount = 0;
                }
            }
        }
        if (bitCount > 0) {
            bits.append(Character.forDigit(nibble << (4 - bitCount), 16));
        }
        return image.getWidth() + "x" + image.getHeight() + ":" + bits;
    }

}
