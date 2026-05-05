package com.bot.dhxy.service;

import com.bot.dhxy.config.BotProperties;
import com.bot.dhxy.core.GameClientTracker;
import com.bot.dhxy.core.TextRecognizer;
import com.bot.dhxy.tools.CoordinateHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.awt.Point;
import java.util.List;
/**
 * 位置视觉服务
 * 纯视觉流：只负责截图、OCR认字、返回坐标
 */
@Service
@RequiredArgsConstructor
@Slf4j // 🌟 加个日志注解，方便打印识别结果
public class LocationVisionService {
    private final GameClientTracker tracker;
    private final TextRecognizer ocr;
    private final BotProperties botProperties;
    private final CoordinateHelper coordinateHelper;

    private static final int ANCHOR_DIFF_X = 46;
    private static final int ANCHOR_DIFF_Y = 59;

    private static final int height = 35;
    private static final int width = 178;

    /**
     * 动作：看一眼屏幕并返回坐标信息
     */
    public TextRecognizer.LocationInfo scanCurrentLocation() {
        boolean ready = tracker.bringWindowToFront();
        if (!ready) {
            System.out.println("❌ 无法唤醒游戏，停止任务。");
            return null;
        }
        String path = "images/tmp_pos.png";

        // 1. 算好的绝对坐标数组
        int[] pics = coordinateHelper.getScaledRect(ANCHOR_DIFF_X, ANCHOR_DIFF_Y, width, height);

        // 2. 解包数组，显式传入 x1, y1, x2, y2
        if (tracker.captureToFile("坐标区域", path, pics[0], pics[1], pics[2], pics[3])) {
            return ocr.parseLocation(path);
        }
        return null;
    }

    // ========================================================================
    // 🧮 字符宽度物理引擎：根据首领的测量数据映射
    // ========================================================================
    private int getCharPixelWidth(char c) {
        if (String.valueOf(c).matches("[\u4e00-\u9fa5]")) {
            return 15; // 汉字
        } else if (c >= 'a' && c <= 'z') {
            return 8;  // 小写字母
        } else if (c >= 'A' && c <= 'Z') {
            return 10; // 大写字母（暂时预估为10，测出后可改）
        } else if (c >= '0' && c <= '9') {
            return 8;  // 数字（通常和小写字母差不多，预估为8）
        } else {
            return 10; // 符号及其他
        }
    }

    private int calculateStringPixelWidth(String str) {
        int width = 0;
        for (char c : str.toCharArray()) {
            width += getCharPixelWidth(c);
        }
        return width;
    }

    // ========================================================================
    // 🛡️ 终极降噪引擎：找出 OCR 乱码中最长的有效名字碎片
    // ========================================================================
    private String findLongestValidFragment(String fullName, String ocrText) {
        String cleanOcr = ocrText.replace(" ", ""); // 基础降噪：砍掉瞎加的空格

        // 从长到短，暴力穷举全名的所有子串（最短必须是2个字防误判）
        for (int len = fullName.length(); len >= 2; len--) {
            for (int i = 0; i <= fullName.length() - len; i++) {
                String sub = fullName.substring(i, i + len);
                if (cleanOcr.contains(sub)) {
                    return sub; // 找到了最长的幸存纯净碎片！
                }
            }
        }
        return null; // 连2个字的碎片都没活下来
    }

    // ========================================================================
    // 👑 [Master 方法] 核心锚点雷达：抗乱码终极版
    // ========================================================================
    public Point extractPlayerPhysicalAnchor(List<TextRecognizer.OcrWordResult> ocrResults,
                                             String fullName,
                                             int scanStartX,
                                             int scanStartY,
                                             int heightOffset) {
        if (ocrResults == null || fullName == null || fullName.isEmpty()) {
            return null;
        }

        String cleanFullName = fullName.replace(" ", "");

        for (TextRecognizer.OcrWordResult w : ocrResults) {
            String text = w.getText();
            if (text == null) continue;

            // 🌟🌟🌟 召唤碎片提取器，无视 [] 和 . 这些乱码！
            String matchedFragment = findLongestValidFragment(cleanFullName, text);

            if (matchedFragment != null) {
                log.info("🎯 [Master雷达] 从OCR乱码 [{}] 中成功提纯出有效碎片: [{}]", text, matchedFragment);

                // 🌟 使用提纯出来的 matchedFragment 进行物理像素计算
                int startIndex = cleanFullName.indexOf(matchedFragment);
                String prefix = cleanFullName.substring(0, startIndex);

                double fullCenterPixel = calculateStringPixelWidth(cleanFullName) / 2.0;
                double prefixPixelWidth = calculateStringPixelWidth(prefix);
                double fragmentCenterPixel = prefixPixelWidth + (calculateStringPixelWidth(matchedFragment) / 2.0);

                int compensationX = (int) Math.round(fullCenterPixel - fragmentCenterPixel);

                int absoluteX = scanStartX + w.getX() + compensationX;
                int absoluteY = scanStartY + w.getY() + heightOffset;

                log.info("📐 [纠偏计算] 全名[{}], 锁定碎片[{}]. 必须补偿偏移: {} 像素",
                        cleanFullName, matchedFragment, compensationX);
                log.info("📍 [Master雷达] 斩断乱码，强行锁定脚底绝对坐标: {}, {}", absoluteX, absoluteY);

                return new Point(absoluteX, absoluteY);
            }
        }
        return null;
    }
}
