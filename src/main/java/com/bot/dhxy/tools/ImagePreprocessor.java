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

import org.opencv.core.CvType;

import java.awt.image.DataBufferByte;

@Slf4j
public class ImagePreprocessor {
    // ==========================================
    // 📸 Debug 探头总控开关
    // ==========================================
    // 设为 true 时，各种过程图才会存到硬盘；发布生产环境时改为 false，极大地节省 CPU 和硬盘 I/O！
    public static boolean ENABLE_DEBUG_SAVE = true;
    private static final String DEBUG_DIR = "images/temp/";

    static {
        OpenCvNativeLoader.ensureLoaded();
    }

    /**
     * 将游戏截图中的“紫色”文字提纯，输出为白底黑字图供 OCR 识别
     */
    public static void washPurpleTextToBlackAndWhite(String inputPath, String outputPath) {
        try {
            Mat src = Imgcodecs.imread(inputPath);
            if (src.empty()) {
                log.warn("⚠️ 洗图失败：找不到原图 {}", inputPath);
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
            log.error("❌ 洗图过程发生异常！", e);
        }
    }

    // =========================================================
    // 🌟 终极绿字识别引擎 (内存+文件 双驱动)
    // =========================================================

    /**
     * 核心算法：判断一个像素是不是大话特有的“选项绿”
     * 逻辑：G通道足够亮，且比RB高出一定范围，完美免疫黄绿色草地和抗锯齿边缘
     */
    public static boolean isOptionGreen(int rgb) {
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        return g > 80 && (g - r) > 40 && (g - b) > 40;
    }


    /**
     * 🟢 洗图神技：只保留对话框里的【亮绿色选项文字】，其他全涂黑 (供OCR寻图使用)
     */
    public static void washGreenTextToBlackAndWhite(String inputPath, String outputPath) {
        try {
            BufferedImage img = ImageIO.read(new File(inputPath));
            if (img == null) return;

            int width = img.getWidth();
            int height = img.getHeight();

            // 为了生成纯正的黑白图，新建一个二值化画布
            BufferedImage out = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_BINARY);

            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    // 🌟 统一调用核心颜色判断方法！
                    if (isOptionGreen(img.getRGB(x, y))) {
                        out.setRGB(x, y, 0xFFFFFF); // 是绿字，涂白
                    } else {
                        out.setRGB(x, y, 0x000000); // 是废话或背景，涂黑
                    }
                }
            }
            ImageIO.write(out, "png", new File(outputPath));
        } catch (Exception e) {
            log.error("❌ 洗绿字异常", e);
        }
    }



    /**
     * 🌟 终极洗图算法：利用 HSV 色彩空间，彻底洗掉背景！(带调试存图)
     */
    public static int countGreenPixelsHSV(BufferedImage img) {
        if (img == null) return 0;

        // 1. 转为 OpenCV 的 Mat
        BufferedImage convertedImg = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
        convertedImg.getGraphics().drawImage(img, 0, 0, null);
        byte[] data = ((java.awt.image.DataBufferByte) convertedImg.getRaster().getDataBuffer()).getData();
        Mat src = new Mat(img.getHeight(), img.getWidth(), CvType.CV_8UC3);
        src.put(0, 0, data);

        Mat hsv = new Mat();
        Imgproc.cvtColor(src, hsv, Imgproc.COLOR_BGR2HSV);

        // 2. HSV 绿色区间
// 🌟 核心修复：大幅提高亮度(V)和饱和度(S)门槛，并收窄色相(H)！
        // H: 50~75 (剔除35附近的黄绿色草地，只留纯正绿色)
        // S: 150~255 (必须是极其鲜艳的颜色，剔除灰暗的植物)
        // V: 180~255 (必须是像灯管一样发光的亮度，剔除阴影里的草丛)
        Scalar lowerGreen = new Scalar(50, 150, 180);
        Scalar upperGreen = new Scalar(75, 255, 255);

        Mat mask = new Mat();
        Core.inRange(hsv, lowerGreen, upperGreen, mask);

        // ==========================================
        // 🚨 探头 2：把洗出来的黑白遮罩图存下来！
        // 纯白的就是它认定的绿字，纯黑的就是被干掉的背景。
        // ==========================================
//        try {
//            java.io.File debugDir = new java.io.File("images/temp");
//            if (!debugDir.exists()) debugDir.mkdirs();
//            org.opencv.imgcodecs.Imgcodecs.imwrite("images/temp/debug_hsv_mask_green.png", mask);
//        } catch (Exception e) {
//            log.error("❌ 保存HSV遮罩图失败", e);
//        }

        int count = Core.countNonZero(mask);

        src.release();
        hsv.release();
        mask.release();

        return count;
    }

    /**
     * ⚪ 雷达2号核心：利用 HSV 过滤背景，只提取高亮的白色剧情文字
     */
    public static int countWhitePixelsHSV(BufferedImage img) {
        if (img == null) return 0;

        // BufferedImage 转 Mat
        BufferedImage convertedImg = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
        convertedImg.getGraphics().drawImage(img, 0, 0, null);
        byte[] data = ((java.awt.image.DataBufferByte) convertedImg.getRaster().getDataBuffer()).getData();
        Mat src = new Mat(img.getHeight(), img.getWidth(), org.opencv.core.CvType.CV_8UC3);
        src.put(0, 0, data);

        Mat hsv = new Mat();
        Imgproc.cvtColor(src, hsv, Imgproc.COLOR_BGR2HSV);

        // 🌟 白色在 HSV 中的特征：
        // H (色相): 0~180 (白色没有色相，全放开)
        // S (饱和度): 0~40 (数值越低越接近纯白/灰色，过滤掉鲜艳的颜色)
        // V (明度): 200~255 (数值越高越亮，过滤掉昏暗的背景)
        Scalar lowerWhite = new Scalar(0, 0, 200);
        Scalar upperWhite = new Scalar(180, 40, 255);

        Mat mask = new Mat();
        Core.inRange(hsv, lowerWhite, upperWhite, mask);

        // 调试用：保存洗出来的白字遮罩
        org.opencv.imgcodecs.Imgcodecs.imwrite("images/temp/debug_hsv_mask_white.png", mask);

        int count = Core.countNonZero(mask);

        src.release();
        hsv.release();
        mask.release();

        return count;
    }

    /**
     * 📊 雷达2号终极版：测算画面的“纹理平滑度 (标准差)”
     */
    public static double getImageStandardDeviation(BufferedImage img) {
        if (img == null) return 100.0;

        // 转为 OpenCV Mat
        BufferedImage convertedImg = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
        convertedImg.getGraphics().drawImage(img, 0, 0, null);
        byte[] data = ((java.awt.image.DataBufferByte) convertedImg.getRaster().getDataBuffer()).getData();
        Mat src = new Mat(img.getHeight(), img.getWidth(), org.opencv.core.CvType.CV_8UC3);
        src.put(0, 0, data);

        // 1. 转成灰度图（我们不看颜色了，只看纹理深浅）
        Mat gray = new Mat();
        Imgproc.cvtColor(src, gray, Imgproc.COLOR_BGR2GRAY);

        // ==========================================
        // 🚨 探头：把机器眼里的灰度图存下来！
        // 没开框时：这里面应该有各种深浅不一的花纹。
        // 开框时：这里面应该是一片均匀的灰色。
        // ==========================================
//        try {
//            java.io.File debugDir = new java.io.File("images/temp");
//            if (!debugDir.exists()) debugDir.mkdirs();
//            org.opencv.imgcodecs.Imgcodecs.imwrite("images/temp/debug_smoothness_gray.png", gray);
//        } catch (Exception e) {
//            log.error("保存灰度图失败", e);
//        }

        // 2. 算标准差
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

    // =========================================================
    // 🛠️ 通用 Debug 存图工具库
    // =========================================================

    /**
     * 保存 OpenCV 的 Mat 图像 (自动创建目录并加上 try-catch)
     * @param mat OpenCV图像对象
     * @param fileName 文件名，例如 "debug_hsv_mask.png"
     */
    public static void saveDebugImage(Mat mat, String fileName) {
        if (!ENABLE_DEBUG_SAVE || mat == null || mat.empty()) return;

        try {
            File dir = new File(DEBUG_DIR);
            if (!dir.exists()) dir.mkdirs();

            String fullPath = DEBUG_DIR + fileName;
            Imgcodecs.imwrite(fullPath, mat);
            // log.debug("📸 [Debug探头] 已保存: {}", fullPath); // 嫌吵可以注释掉
        } catch (Exception e) {
            log.error("❌ 保存 Debug 图片 (Mat) 失败: {}", fileName, e);
        }
    }

    /**
     * 保存 Java 原生的 BufferedImage
     * @param img Java 图像对象
     * @param fileName 文件名，例如 "debug_raw_crop.png"
     */
    public static void saveDebugImage(BufferedImage img, String fileName) {
        if (!ENABLE_DEBUG_SAVE || img == null) return;

        try {
            File dir = new File(DEBUG_DIR);
            if (!dir.exists()) dir.mkdirs();

            String fullPath = DEBUG_DIR + fileName;
            ImageIO.write(img, "png", new File(fullPath));
            // log.debug("📸 [Debug探头] 已保存: {}", fullPath); // 嫌吵可以注释掉
        } catch (Exception e) {
            log.error("❌ 保存 Debug 图片 (BufferedImage) 失败: {}", fileName, e);
        }
    }

    public static void washYellowText(String inputPath, String outputPath) {
        try {
            Mat src = Imgcodecs.imread(inputPath);
            if (src.empty()) return;

            Mat hsv = new Mat();
            Imgproc.cvtColor(src, hsv, Imgproc.COLOR_BGR2HSV);

            // 1. 🌟 只抓核心黄色，不要试图去抓边缘虚化的部分
            Scalar lowerYellow = new Scalar(20, 100, 100);
            Scalar upperYellow = new Scalar(35, 255, 255);

            Mat mask = new Mat();
            Core.inRange(hsv, lowerYellow, upperYellow, mask);

            // 🌟 核心：不要 dilate，不要 erode！它们会破坏字体结构。
            // 直接转成白底黑字。
            Mat result = new Mat();
            Core.bitwise_not(mask, result);

            Imgcodecs.imwrite(outputPath, result);

            src.release(); hsv.release(); mask.release(); result.release();
        } catch (Exception e) {
            log.error("❌ 极简洗字失败", e);
        }
    }
}