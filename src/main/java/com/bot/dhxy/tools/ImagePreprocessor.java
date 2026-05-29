package com.bot.dhxy.tools;

import com.bot.dhxy.core.OpenCvNativeLoader;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.opencv.core.CvType;

import java.io.IOException;

@Slf4j
public class ImagePreprocessor {
    public static boolean ENABLE_DEBUG_SAVE = true;
    private static final String DEBUG_DIR = "images/temp/";

    static {
        OpenCvNativeLoader.ensureLoaded();
    }

    public static void washPurpleTextToBlackAndWhite(String inputPath, String outputPath) {
        try {
            Mat src = Imgcodecs.imread(inputPath);
            if (src.empty()) {
                log.warn("wash purple failed, input not found: {}", inputPath);
                return;
            }

            Mat hsv = new Mat();
            Imgproc.cvtColor(src, hsv, Imgproc.COLOR_BGR2HSV);

            Scalar lowerPurple = new Scalar(120, 50, 50);
            Scalar upperPurple = new Scalar(160, 255, 255);

            Mat mask = new Mat();
            Core.inRange(hsv, lowerPurple, upperPurple, mask);

            Mat invertedMask = new Mat();
            Core.bitwise_not(mask, invertedMask);

            Imgcodecs.imwrite(outputPath, invertedMask);

            src.release();
            hsv.release();
            mask.release();
            invertedMask.release();

        } catch (Exception e) {
            log.error("wash purple text failed", e);
        }
    }

    public static boolean isOptionGreen(int rgb) {
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        return g > 80 && (g - r) > 40 && (g - b) > 40;
    }

    public static void washGreenTextToBlackAndWhite(String inputPath, String outputPath) {
        try {
            BufferedImage img = ImageIO.read(new File(inputPath));
            if (img == null) return;

            BufferedImage out = washGreenTextToBlackAndWhite(img);
            ImageIO.write(out, "png", new File(outputPath));
            img.flush();
            out.flush();
        } catch (Exception e) {
            log.error("wash green text failed", e);
        }
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

    /**
     * Wash task option text for template matching only.
     *
     * <p>Standard option text is green, but the currently selected/hovered option can render as
     * yellow. Keep this separate from the generic green wash so route dialogs with yellow
     * recommendations do not pollute ordinary green-text OCR.</p>
     */
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

    private static boolean isHighlightedOptionYellow(int rgb) {
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        return r > 180 && g > 180 && b < 80 && Math.abs(r - g) < 45;
    }

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

    /**
     * Locate horizontal green option-text bands in an already captured image.
     *
     * @param frame source image in image-local pixels; ownership stays with caller.
     * @return green text bands in top-to-bottom order. Empty means no stable option text row was
     * found.
     */
    public static List<GreenTextBand> findGreenTextBands(BufferedImage frame) {
        int height = frame.getHeight();
        int width = frame.getWidth();
        int[] rowCounts = new int[height];
        for (int y = 0; y < height; y++) {
            int count = 0;
            for (int x = 0; x < width; x++) {
                if (isOptionGreen(frame.getRGB(x, y))) {
                    count++;
                }
            }
            rowCounts[y] = count;
        }

        List<GreenTextBand> bands = new ArrayList<>();
        int startY = -1;
        int endY = -1;
        int gap = 0;
        for (int y = 0; y < height; y++) {
            if (rowCounts[y] >= 3) {
                if (startY < 0) {
                    startY = y;
                }
                endY = y;
                gap = 0;
            } else if (startY >= 0) {
                gap++;
                if (gap > 2) {
                    GreenTextBand band = buildGreenTextBand(frame, startY, endY);
                    if (band != null) {
                        bands.add(band);
                    }
                    startY = -1;
                    endY = -1;
                    gap = 0;
                }
            }
        }
        if (startY >= 0) {
            GreenTextBand band = buildGreenTextBand(frame, startY, endY);
            if (band != null) {
                bands.add(band);
            }
        }

        return bands;
    }

    public static GreenTextBand pickGreenTextBand(List<GreenTextBand> bands, boolean first) {
        if (bands == null || bands.isEmpty()) {
            return null;
        }
        return first ? bands.get(0) : bands.get(bands.size() - 1);
    }

    private static GreenTextBand buildGreenTextBand(BufferedImage frame, int startY, int endY) {
        int minX = frame.getWidth();
        int maxX = -1;
        int greenPixels = 0;
        for (int y = startY; y <= endY; y++) {
            for (int x = 0; x < frame.getWidth(); x++) {
                if (isOptionGreen(frame.getRGB(x, y))) {
                    minX = Math.min(minX, x);
                    maxX = Math.max(maxX, x);
                    greenPixels++;
                }
            }
        }

        if (greenPixels < 30 || maxX < minX) {
            return null;
        }
        return GreenTextBand.builder()
                .minX(minX)
                .minY(startY)
                .maxX(maxX)
                .maxY(endY)
                .pixels(greenPixels)
                .build();
    }

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

        saveDebugImage(mask, debugOutputPath);

        int count = Core.countNonZero(mask);

        src.release();
        hsv.release();
        mask.release();

        return count;
    }

    public static int countThinWhitePixelsHSV(BufferedImage img, String debugOutputPath) {
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

        Scalar lowerWhite = new Scalar(0, 0, 225);
        Scalar upperWhite = new Scalar(180, 15, 255);
        Mat allWhiteMask = new Mat();
        Core.inRange(hsv, lowerWhite, upperWhite, allWhiteMask);

        Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new org.opencv.core.Size(3, 3));
        Mat thickWhiteMask = new Mat();
        Imgproc.erode(allWhiteMask, thickWhiteMask, kernel);

        Mat textOnlyMask = new Mat();
        Core.subtract(allWhiteMask, thickWhiteMask, textOnlyMask);

        saveDebugImage(textOnlyMask, debugOutputPath);

        int thinWhiteCount = Core.countNonZero(textOnlyMask);

        src.release(); hsv.release(); allWhiteMask.release();
        thickWhiteMask.release(); kernel.release(); textOnlyMask.release();

        return thinWhiteCount;
    }

    /**
     * Keep thin white dialog glyphs and turn everything else black.
     *
     * @param inputPath source screenshot path.
     * @param outputPath output black/white image path; matching templates should be white glyphs on
     *                   black background.
     */
    public static void washThinWhiteTextToBlackAndWhite(String inputPath, String outputPath) {
        try {
            Mat src = Imgcodecs.imread(inputPath);
            if (src.empty()) {
                log.warn("wash thin white text failed, input not found: {}", inputPath);
                return;
            }

            Mat hsv = new Mat();
            Imgproc.cvtColor(src, hsv, Imgproc.COLOR_BGR2HSV);

            Scalar lowerWhite = new Scalar(0, 0, 225);
            Scalar upperWhite = new Scalar(180, 15, 255);
            Mat allWhiteMask = new Mat();
            Core.inRange(hsv, lowerWhite, upperWhite, allWhiteMask);

            Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new org.opencv.core.Size(3, 3));
            Mat thickWhiteMask = new Mat();
            Imgproc.erode(allWhiteMask, thickWhiteMask, kernel);

            Mat textOnlyMask = new Mat();
            Core.subtract(allWhiteMask, thickWhiteMask, textOnlyMask);
            Imgcodecs.imwrite(outputPath, textOnlyMask);

            src.release(); hsv.release(); allWhiteMask.release();
            thickWhiteMask.release(); kernel.release(); textOnlyMask.release();
        } catch (Exception e) {
            log.error("wash thin white text failed", e);
        }
    }

    public static double getImageStandardDeviation(BufferedImage img, String debugOutputPath) {
        if (img == null) return 100.0;

        BufferedImage convertedImg = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
        Graphics2D graphics = convertedImg.createGraphics();
        try {
            graphics.drawImage(img, 0, 0, null);
        } finally {
            graphics.dispose();
        }
        byte[] data = ((java.awt.image.DataBufferByte) convertedImg.getRaster().getDataBuffer()).getData();
        Mat src = new Mat(img.getHeight(), img.getWidth(), org.opencv.core.CvType.CV_8UC3);
        src.put(0, 0, data);
        convertedImg.flush();

        Mat gray = new Mat();
        Imgproc.cvtColor(src, gray, Imgproc.COLOR_BGR2GRAY);

        saveDebugImage(gray, debugOutputPath);

        org.opencv.core.MatOfDouble mean = new org.opencv.core.MatOfDouble();
        org.opencv.core.MatOfDouble stddev = new org.opencv.core.MatOfDouble();
        Core.meanStdDev(gray, mean, stddev);

        double dev = stddev.get(0, 0)[0];

        src.release();
        gray.release();
        mean.release();
        stddev.release();

        return dev;
    }

    public static void saveDebugImage(Mat mat, String fileName) {
        if (!ENABLE_DEBUG_SAVE || mat == null || mat.empty()) return;

        try {
            File output = resolveDebugOutput(fileName);
            Imgcodecs.imwrite(output.getPath(), mat);
        } catch (Exception e) {
            log.error("save debug image failed: {}", fileName, e);
        }
    }

    public static void saveDebugImage(BufferedImage img, String fileName) {
        if (!ENABLE_DEBUG_SAVE || img == null) return;

        try {
            File output = resolveDebugOutput(fileName);
            ImageIO.write(img, "png", output);
        } catch (Exception e) {
            log.error("save debug image failed: {}", fileName, e);
        }
    }

    /**
     * Write an image to an explicit filesystem path without applying the debug-directory fallback.
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

    /**
     * Wash yellow in-game text into a cleaned black/white OCR image.
     *
     * <p>This is the single public yellow-text washing entry point. It writes {@code outputPath}
     * as a binary image where retained yellow text pixels are white and everything else is black,
     * then removes long horizontal noise and tiny connected components. Both paths are filesystem
     * paths; callers are responsible for passing window-scoped output paths when running multiple
     * game windows.</p>
     *
     * @param inputPath source screenshot path.
     * @param outputPath destination cleaned binary image path.
     */
    public static void washYellowText(String inputPath, String outputPath) {
        washYellowTextToCleanBlackAndWhite(inputPath, outputPath);
    }

    public static boolean isYellowTextPixel(int r, int g, int b) {
        return r >= 150
                && g >= 110
                && b <= 110
                && Math.abs(r - g) <= 110
                && r > b + 60
                && g > b + 40;
    }

    public static int countYellowPixels(BufferedImage image) {
        if (image == null) {
            return 0;
        }

        int count = 0;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int rgb = image.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;
                if (isYellowTextPixel(r, g, b)) {
                    count++;
                }
            }
        }
        return count;
    }


    /**
     * Build the cleaned yellow-text OCR image used by all yellow-text readers.
     *
     * <p>The method first extracts yellow pixels with the shared RGB predicate, then removes long
     * horizontal artifacts and very small connected components. It writes a temporary mask beside
     * {@code outputPath}, so concurrent window-scoped calls do not share a global temp file.</p>
     *
     * @param inputPath source screenshot path.
     * @param outputPath destination cleaned binary image path.
     */
    private static void washYellowTextToCleanBlackAndWhite(String inputPath, String outputPath) {
        try {
            BufferedImage img = ImageIO.read(new File(inputPath));
            if (img == null) {
                log.warn("wash yellow clean failed, input not readable: {}", inputPath);
                return;
            }

            int width = img.getWidth();
            int height = img.getHeight();
            BufferedImage yellowMask = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_BINARY);

            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int rgb = img.getRGB(x, y);
                    int r = (rgb >> 16) & 0xFF;
                    int g = (rgb >> 8) & 0xFF;
                    int b = rgb & 0xFF;
                    yellowMask.setRGB(x, y, isYellowTextPixel(r, g, b) ? 0xFFFFFF : 0x000000);
                }
            }

            File output = new File(outputPath);
            File parent = output.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            String tempPath = outputPath + ".yellow-mask.tmp.png";
            ImageIO.write(yellowMask, "png", new File(tempPath));
            img.flush();
            yellowMask.flush();

            Mat src = Imgcodecs.imread(tempPath, Imgcodecs.IMREAD_GRAYSCALE);
            if (src.empty()) {
                log.warn("wash yellow clean failed, temp not readable: {}", tempPath);
                return;
            }

            Mat horizontal = new Mat();
            Mat horizontalKernel = Imgproc.getStructuringElement(
                    Imgproc.MORPH_RECT,
                    new org.opencv.core.Size(35, 1)
            );
            Imgproc.morphologyEx(src, horizontal, Imgproc.MORPH_OPEN, horizontalKernel);

            Mat noLines = new Mat();
            Core.subtract(src, horizontal, noLines);

            Mat labels = new Mat();
            Mat stats = new Mat();
            Mat centroids = new Mat();
            int numLabels = Imgproc.connectedComponentsWithStats(noLines, labels, stats, centroids);
            Mat cleaned = Mat.zeros(noLines.size(), CvType.CV_8UC1);

            for (int i = 1; i < numLabels; i++) {
                int area = (int) stats.get(i, Imgproc.CC_STAT_AREA)[0];
                int componentWidth = (int) stats.get(i, Imgproc.CC_STAT_WIDTH)[0];
                int componentHeight = (int) stats.get(i, Imgproc.CC_STAT_HEIGHT)[0];
                if (area < 3 || (componentWidth > 40 && componentHeight <= 3)) {
                    continue;
                }
                Mat mask = new Mat();
                Core.compare(labels, new Scalar(i), mask, Core.CMP_EQ);
                cleaned.setTo(new Scalar(255), mask);
                mask.release();
            }

            Imgcodecs.imwrite(outputPath, cleaned);
            Files.deleteIfExists(Path.of(tempPath));

            src.release();
            horizontal.release();
            horizontalKernel.release();
            noLines.release();
            labels.release();
            stats.release();
            centroids.release();
            cleaned.release();
        } catch (Exception e) {
            log.error("wash yellow clean text failed", e);
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

    @Value
    @Builder
    @AllArgsConstructor(access = AccessLevel.PUBLIC)
    @Accessors(fluent = true)
    public static class GreenTextBand {
        int minX;
        int minY;
        int maxX;
        int maxY;
        int pixels;
    }
}
