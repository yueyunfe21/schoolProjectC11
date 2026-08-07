package com.bot.dhxy.core;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ImageFinder {

    static {
        OpenCvNativeLoader.ensureLoaded();
    }

    /**
     * 在大图中查找小图，命中后返回 [centerX, centerY, score]。
     */
    public static double[] find(String sourcePath, String targetPath, double threshold) {
        Mat source = Imgcodecs.imread(sourcePath);
        Mat target = Imgcodecs.imread(targetPath);
        if (source.empty() || target.empty()) {
            return null;
        }

        Mat result = new Mat();

        // OpenCV returns a response matrix where each cell is the template score at one
        // top-left position in source. We only need the strongest candidate here.
        Imgproc.matchTemplate(source, target, result, Imgproc.TM_CCOEFF_NORMED);
        Core.MinMaxLocResult mmr = Core.minMaxLoc(result);
        double[] coordsAndSim = null;
        if (mmr.maxVal >= threshold) {
            double centerX = mmr.maxLoc.x + target.cols() / 2.0;
            double centerY = mmr.maxLoc.y + target.rows() / 2.0;
            coordsAndSim = new double[]{centerX, centerY, mmr.maxVal};
        }

        source.release();
        target.release();
        result.release();
        return coordsAndSim;
    }

    /**
     * 在内存大图中查找内存小图，命中后返回 [centerX, centerY, score]。
     */
    public static double[] find(BufferedImage sourceImage, BufferedImage targetImage, double threshold) {
        if (sourceImage == null || targetImage == null) {
            return null;
        }
        Mat source = bufferedImageToMat(sourceImage);
        Mat target = bufferedImageToMat(targetImage);
        if (source.empty() || target.empty()) {
            source.release();
            target.release();
            return null;
        }

        Mat result = new Mat();
        Imgproc.matchTemplate(source, target, result, Imgproc.TM_CCOEFF_NORMED);
        Core.MinMaxLocResult mmr = Core.minMaxLoc(result);
        double[] coordsAndSim = null;
        if (mmr.maxVal >= threshold) {
            double centerX = mmr.maxLoc.x + target.cols() / 2.0;
            double centerY = mmr.maxLoc.y + target.rows() / 2.0;
            coordsAndSim = new double[]{centerX, centerY, mmr.maxVal};
        }

        source.release();
        target.release();
        result.release();
        return coordsAndSim;
    }

    /**
     * Returns the strongest normalized template score without imposing a business threshold.
     *
     * <p>This is intentionally the same {@code TM_CCOEFF_NORMED} calculation as
     * {@link #find(BufferedImage, BufferedImage, double)}. It exists for bounded miss diagnostics;
     * callers still own their threshold and must not treat a score as a match by itself.</p>
     */
    public static double bestMatchScore(BufferedImage sourceImage, BufferedImage targetImage) {
        if (sourceImage == null || targetImage == null) {
            return Double.NaN;
        }
        Mat source = bufferedImageToMat(sourceImage);
        Mat target = bufferedImageToMat(targetImage);
        if (source.empty() || target.empty()) {
            source.release();
            target.release();
            return Double.NaN;
        }
        Mat result = new Mat();
        try {
            Imgproc.matchTemplate(source, target, result, Imgproc.TM_CCOEFF_NORMED);
            return Core.minMaxLoc(result).maxVal;
        } finally {
            source.release();
            target.release();
            result.release();
        }
    }

    /**
     * 在大图中查找所有超过阈值的小图候选点，返回 [centerX, centerY, score]。
     * 结果会按相似度从高到低做一次近邻去重，避免同一个目标周围连续命中多个像素。
     */
    public static List<double[]> findAll(String sourcePath, String targetPath, double threshold, double minDistance) {
        Mat source = Imgcodecs.imread(sourcePath);
        Mat target = Imgcodecs.imread(targetPath);
        List<double[]> matches = new ArrayList<>();
        if (source.empty() || target.empty()) {
            source.release();
            target.release();
            return matches;
        }

        Mat result = new Mat();
        Imgproc.matchTemplate(source, target, result, Imgproc.TM_CCOEFF_NORMED);

        // Collect every raw hit over threshold first. Adjacent pixels around the same
        // visual target often all pass the threshold, so they are de-duplicated below.
        List<double[]> rawMatches = new ArrayList<>();
        for (int y = 0; y < result.rows(); y++) {
            for (int x = 0; x < result.cols(); x++) {
                double score = result.get(y, x)[0];
                if (score >= threshold) {
                    double centerX = x + target.cols() / 2.0;
                    double centerY = y + target.rows() / 2.0;
                    rawMatches.add(new double[]{centerX, centerY, score});
                }
            }
        }

        // Keep strongest hits first and discard nearby duplicates so callers receive one
        // candidate per visual object instead of a cluster of almost-identical pixels.
        rawMatches.sort(Comparator.comparingDouble((double[] item) -> item[2]).reversed());
        for (double[] candidate : rawMatches) {
            boolean duplicate = false;
            for (double[] accepted : matches) {
                double dx = candidate[0] - accepted[0];
                double dy = candidate[1] - accepted[1];
                if (Math.sqrt(dx * dx + dy * dy) < minDistance) {
                    duplicate = true;
                    break;
                }
            }
            if (!duplicate) {
                matches.add(candidate);
            }
        }

        source.release();
        target.release();
        result.release();
        return matches;
    }

    /**
     * 计算像素差异率：0.0 完全一致，1.0 完全不同。
     */
    private static double getDiffRatio(BufferedImage img1, BufferedImage img2) {
        if (img1 == null || img2 == null) {
            return 1.0;
        }
        if (img1.getWidth() != img2.getWidth() || img1.getHeight() != img2.getHeight()) {
            return 1.0;
        }

        int width = img1.getWidth();
        int height = img1.getHeight();
        int totalPixels = width * height;
        long diffCount = 0;
        int colorTolerance = 15;

        // Count pixels whose RGB channels differ beyond tolerance. This is intended for
        // same-size stability checks, not for locating a template inside a larger image.
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb1 = img1.getRGB(x, y);
                int rgb2 = img2.getRGB(x, y);

                int r1 = (rgb1 >> 16) & 0xFF;
                int g1 = (rgb1 >> 8) & 0xFF;
                int b1 = rgb1 & 0xFF;
                int r2 = (rgb2 >> 16) & 0xFF;
                int g2 = (rgb2 >> 8) & 0xFF;
                int b2 = rgb2 & 0xFF;

                if (Math.abs(r1 - r2) > colorTolerance
                        || Math.abs(g1 - g2) > colorTolerance
                        || Math.abs(b1 - b2) > colorTolerance) {
                    diffCount++;
                }
            }
        }
        return (double) diffCount / totalPixels;
    }

    public static boolean isMatch(BufferedImage target, BufferedImage template, double tolerance) {
        return getDiffRatio(target, template) <= tolerance;
    }

    private static Mat bufferedImageToMat(BufferedImage bi) {
        BufferedImage convertedImg =
                new BufferedImage(bi.getWidth(), bi.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
        convertedImg.getGraphics().drawImage(bi, 0, 0, null);

        byte[] data = ((DataBufferByte) convertedImg.getRaster().getDataBuffer()).getData();
        Mat mat = new Mat(bi.getHeight(), bi.getWidth(), CvType.CV_8UC3);
        mat.put(0, 0, data);
        return mat;
    }
}
