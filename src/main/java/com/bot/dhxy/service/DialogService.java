package com.bot.dhxy.service;

import com.bot.dhxy.config.InputProvider;
import com.bot.dhxy.config.TeleportConfig; // 🌟 引入你的地图数据仓库
import com.bot.dhxy.core.GameClientTracker;
import com.bot.dhxy.core.TextRecognizer;
import com.bot.dhxy.tools.CoordinateHelper;
import com.bot.dhxy.tools.ImagePreprocessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

@Slf4j
@Component
@RequiredArgsConstructor
public class DialogService {

    private final InputProvider inputProvider;
    private final GameClientTracker tracker;
    private final CoordinateHelper coordinateHelper;
    private final TextRecognizer ocr;
    private final Random random = new Random();

    // 默认对话框区域坐标（可根据需要微调）
    private static final int DIALOG_X = 250;
    private static final int DIALOG_Y = 312;
    private static final int DIALOG_W = 529;
    private static final int DIALOG_H = 208;

    /**
     * 🚀 物理盲狙：五环专用的“暴力美学”
     */
    public void acceptTaskByFixedCoordinates(int offsetX, int offsetY) {
        int targetX = tracker.getWindowBaseX() + offsetX;
        int targetY = tracker.getWindowBaseY() + offsetY;
        inputProvider.moveMouse(targetX, targetY);
        inputProvider.clickLeft(targetX, targetY, 150);
    }

    /**
     * 🟢 雷达1：是否有带选项的对话框 (绿字)
     */
    public boolean hasOptionDialog() {
        int[] searchArea = coordinateHelper.getScaledRect(250, 312, 529, 208);
        BufferedImage frame = tracker.captureToMemory(
                "dialog-green-scan",
                searchArea[0], searchArea[1], searchArea[2], searchArea[3]
        );

        if (frame == null) return false;

        // ==========================================
        // 🚨 探头 1：把相机截到的原始彩色画面存下来！
        // ==========================================
//        try {
//            java.io.File debugDir = new java.io.File("images/temp");
//            if (!debugDir.exists()) debugDir.mkdirs();
//            javax.imageio.ImageIO.write(frame, "png", new java.io.File("images/temp/debug_raw_green_crop.png"));
//        } catch (Exception e) {
//            log.error("❌ 保存原图失败", e);
//        }

        // 去洗图并数点
        int greenCount = ImagePreprocessor.countGreenPixelsHSV(frame);

        frame.flush();

        boolean opened = greenCount > 250;

        if (opened) {
            log.info("🎯 [状态监测] 暴力洗图完成！剩余纯绿字像素: {}", greenCount);
        } else {
            log.debug("👀 [状态监测] 未达标。当前绿字像素仅有: {}", greenCount);
        }

        return opened;
    }

    /**
     * ⚪ 雷达2（方差探测版）：闲聊对话框侦测
     */
    public boolean hasStoryDialog() {
        // 🌟 只取宽 300，高 100 的核心空白区 (坐标你可以微调)
        int[] searchArea = coordinateHelper.getScaledRect(255, 418, 525, 70);
        BufferedImage frame = tracker.captureToMemory("scan-smoothness", searchArea[0], searchArea[1], searchArea[2], searchArea[3]);

        if (frame == null) return false;

        // ==========================================
        // 🚨 探头：把相机截到的彩色原图存下来！
        // 确认这个框是不是完美的避开了所有 UI 白字！
        // ==========================================
//        try {
//            java.io.File debugDir = new java.io.File("images/temp");
//            if (!debugDir.exists()) debugDir.mkdirs();
//            javax.imageio.ImageIO.write(frame, "png", new java.io.File("images/temp/debug_smoothness_crop.png"));
//        } catch (Exception e) {
//            log.error("保存方差原图失败", e);
//        }

        // 算标准差
        double stddev = ImagePreprocessor.getImageStandardDeviation(frame);
        frame.flush();

        // 阈值初定 25.0，你可以根据控制台实际打印的数据调整
        boolean isOpened = stddev < 20.0;

        if (isOpened) {
            log.info("🎯 [状态监测-雷达2] 发现平滑UI玻璃！粗糙度暴跌至: {}", String.format("%.2f", stddev));
        } else {
            log.info("👀 [状态监测-雷达2] 当前地面粗糙度为: {}", String.format("%.2f", stddev));
        }

        return isOpened;
    }

    /**
     * 🚩 总控雷达：兼容原有逻辑
     * 无论是有选项还是只有废话，只要屏幕上有框，就返回 true
     */
    public boolean isDialogOpened() {
        return hasOptionDialog() || hasStoryDialog();
    }


}