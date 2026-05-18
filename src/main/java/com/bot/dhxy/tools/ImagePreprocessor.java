package com.bot.dhxy.tools;

import com.bot.dhxy.core.OpenCvNativeLoader;
import lombok.extern.slf4j.Slf4j;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

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

            int width = img.getWidth();
            int height = img.getHeight();
            BufferedImage out = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_BINARY);

            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    if (isOptionGreen(img.getRGB(x, y))) {
                        out.setRGB(x, y, 0xFFFFFF);
                    } else {
                        out.setRGB(x, y, 0x000000);
                    }
                }
            }
            ImageIO.write(out, "png", new File(outputPath));
        } catch (Exception e) {
            log.error("wash green text failed", e);
        }
    }

    public static int countGreenPixelsHSV(BufferedImage img) {
        if (img == null) return 0;

        BufferedImage convertedImg = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
        convertedImg.getGraphics().drawImage(img, 0, 0, null);
        byte[] data = ((java.awt.image.DataBufferByte) convertedImg.getRaster().getDataBuffer()).getData();
        Mat src = new Mat(img.getHeight(), img.getWidth(), CvType.CV_8UC3);
        src.put(0, 0, data);

        Mat hsv = new Mat();
        Imgproc.cvtColor(src, hsv, Imgproc.COLOR_BGR2HSV);

        Scalar lowerGreen = new Scalar(50, 150, 180);
        Scalar upperGreen = new Scalar(75, 255, 255);

        Mat mask = new Mat();
        Core.inRange(hsv, lowerGreen, upperGreen, mask);

        saveDebugImage(mask, "debug_hsv_mask_green.png");

        int count = Core.countNonZero(mask);

        src.release();
        hsv.release();
        mask.release();

        return count;
    }

    public static int countWhitePixelsHSV(BufferedImage img) {
        if (img == null) return 0;

        BufferedImage convertedImg = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
        convertedImg.getGraphics().drawImage(img, 0, 0, null);
        byte[] data = ((java.awt.image.DataBufferByte) convertedImg.getRaster().getDataBuffer()).getData();
        Mat src = new Mat(img.getHeight(), img.getWidth(), org.opencv.core.CvType.CV_8UC3);
        src.put(0, 0, data);

        Mat hsv = new Mat();
        Imgproc.cvtColor(src, hsv, Imgproc.COLOR_BGR2HSV);

        Scalar lowerWhite = new Scalar(0, 0, 200);
        Scalar upperWhite = new Scalar(180, 40, 255);

        Mat mask = new Mat();
        Core.inRange(hsv, lowerWhite, upperWhite, mask);

        saveDebugImage(mask, "debug_hsv_mask_white.png");

        int count = Core.countNonZero(mask);

        src.release();
        hsv.release();
        mask.release();

        return count;
    }

    public static int countThinWhitePixelsHSV(BufferedImage img) {
        if (img == null) return 0;

        BufferedImage convertedImg = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
        convertedImg.getGraphics().drawImage(img, 0, 0, null);
        byte[] data = ((java.awt.image.DataBufferByte) convertedImg.getRaster().getDataBuffer()).getData();
        Mat src = new Mat(img.getHeight(), img.getWidth(), CvType.CV_8UC3);
        src.put(0, 0, data);

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

        saveDebugImage(textOnlyMask, "debug_thin_white_text.png");

        int thinWhiteCount = Core.countNonZero(textOnlyMask);

        src.release(); hsv.release(); allWhiteMask.release();
        thickWhiteMask.release(); kernel.release(); textOnlyMask.release();

        return thinWhiteCount;
    }

    public static double getImageStandardDeviation(BufferedImage img) {
        if (img == null) return 100.0;

        BufferedImage convertedImg = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
        convertedImg.getGraphics().drawImage(img, 0, 0, null);
        byte[] data = ((java.awt.image.DataBufferByte) convertedImg.getRaster().getDataBuffer()).getData();
        Mat src = new Mat(img.getHeight(), img.getWidth(), org.opencv.core.CvType.CV_8UC3);
        src.put(0, 0, data);

        Mat gray = new Mat();
        Imgproc.cvtColor(src, gray, Imgproc.COLOR_BGR2GRAY);

        saveDebugImage(gray, "debug_smoothness_gray.png");

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

    public static void washYellowText(String inputPath, String outputPath) {
        try {
            Mat src = Imgcodecs.imread(inputPath);
            if (src.empty()) return;

            Mat hsv = new Mat();
            Imgproc.cvtColor(src, hsv, Imgproc.COLOR_BGR2HSV);

            Scalar lowerYellow = new Scalar(20, 100, 100);
            Scalar upperYellow = new Scalar(35, 255, 255);

            Mat mask = new Mat();
            Core.inRange(hsv, lowerYellow, upperYellow, mask);

            Mat result = new Mat();
            Core.bitwise_not(mask, result);

            Imgcodecs.imwrite(outputPath, result);

            src.release(); hsv.release(); mask.release(); result.release();
        } catch (Exception e) {
            log.error("wash yellow text failed", e);
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
}