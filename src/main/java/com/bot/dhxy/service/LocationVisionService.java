package com.bot.dhxy.service;

import com.bot.dhxy.config.BotProperties;
import com.bot.dhxy.core.GameClientTracker;
import com.bot.dhxy.core.TextRecognizer;
import com.bot.dhxy.tools.CoordinateHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 位置视觉服务
 * 纯视觉流：只负责截图、OCR认字、返回坐标
 */
@Service
@RequiredArgsConstructor
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
}