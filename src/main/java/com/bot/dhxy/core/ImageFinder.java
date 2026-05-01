package com.bot.dhxy.core;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;

public class ImageFinder {

    static {
        nu.pattern.OpenCV.loadLocally();
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
     * 计算两张图片路径的相似度（0.0 ~ 1.0）。
     */
    public static double calculateSimilarity(String path1, String path2) {
        Mat img1 = Imgcodecs.imread(path1);
        Mat img2 = Imgcodecs.imread(path2);
        if (img1.empty() || img2.empty()) {
            return 0.0;
        }

        Imgproc.resize(img2, img2, img1.size());
        Mat result = new Mat();
        Imgproc.matchTemplate(img1, img2, result, Imgproc.TM_CCOEFF_NORMED);
        Core.MinMaxLocResult mmr = Core.minMaxLoc(result);

        double val = mmr.maxVal;
        img1.release();
        img2.release();
        result.release();
        return val;
    }

    /**
     * 计算两张内存图的相似度（OpenCV 模板相关性）。
     */
    public static double calculateSimilarity(BufferedImage img1, BufferedImage img2) {
        if (img1 == null || img2 == null) {
            return 0.0;
        }

        Mat mat1 = bufferedImageToMat(img1);
        Mat mat2 = bufferedImageToMat(img2);
        Imgproc.resize(mat2, mat2, mat1.size());

        Mat result = new Mat();
        Imgproc.matchTemplate(mat1, mat2, result, Imgproc.TM_CCOEFF_NORMED);
        Core.MinMaxLocResult mmr = Core.minMaxLoc(result);

        double val = mmr.maxVal;
        mat1.release();
        mat2.release();
        result.release();
        return val;
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

    /**
     * 纯像素滑窗模板匹配：在 source 中查找 template。
     */
    public static boolean findTemplateInImage(BufferedImage source, BufferedImage template, double tolerance) {
        if (source == null || template == null) {
            return false;
        }

        int sWidth = source.getWidth();
        int sHeight = source.getHeight();
        int tWidth = template.getWidth();
        int tHeight = template.getHeight();
        if (tWidth > sWidth || tHeight > sHeight) {
            return false;
        }

        int colorTolerance = 15;
        int maxAllowedDiffs = (int) (tWidth * tHeight * tolerance);

        for (int y = 0; y <= sHeight - tHeight; y++) {
            for (int x = 0; x <= sWidth - tWidth; x++) {
                int diffCount = 0;
                boolean matched = true;

                for (int ty = 0; ty < tHeight; ty++) {
                    for (int tx = 0; tx < tWidth; tx++) {
                        int rgbS = source.getRGB(x + tx, y + ty);
                        int rgbT = template.getRGB(tx, ty);

                        int r1 = (rgbS >> 16) & 0xFF;
                        int g1 = (rgbS >> 8) & 0xFF;
                        int b1 = rgbS & 0xFF;
                        int r2 = (rgbT >> 16) & 0xFF;
                        int g2 = (rgbT >> 8) & 0xFF;
                        int b2 = rgbT & 0xFF;

                        if (Math.abs(r1 - r2) > colorTolerance
                                || Math.abs(g1 - g2) > colorTolerance
                                || Math.abs(b1 - b2) > colorTolerance) {
                            diffCount++;
                            if (diffCount > maxAllowedDiffs) {
                                matched = false;
                                break;
                            }
                        }
                    }
                    if (!matched) {
                        break;
                    }
                }

                if (matched) {
                    return true;
                }
            }
        }
        return false;
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
