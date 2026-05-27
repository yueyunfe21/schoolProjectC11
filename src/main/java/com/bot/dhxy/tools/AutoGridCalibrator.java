package com.bot.dhxy.tools;



import com.bot.dhxy.model.ocr.LocationInfo;
import com.bot.dhxy.model.ocr.OcrWordResult;
import com.bot.dhxy.runner.stop.TaskSleep;

import com.bot.dhxy.core.GameClientTracker;
import com.bot.dhxy.core.TextRecognizer;
import com.bot.dhxy.model.PlayerCharacter;
import com.bot.dhxy.vision.LocationVisionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.awt.Point;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class AutoGridCalibrator {

    private final LocationVisionService locationVisionService;
    private final GameClientTracker tracker;
    private final TextRecognizer ocr;

    // 内部数据结构，用来存抓到的数据
    static class CalibPoint {
        int logicX, logicY;
        int physX, physY;

        public CalibPoint(int logicX, int logicY, Point phys) {
            this.logicX = logicX;
            this.logicY = logicY;
            this.physX = phys.x;
            this.physY = phys.y;
        }
    }

    /**
     * 🎮 首领专用：傻瓜式全自动跑动测绘
     */
    public void startAutoCalibration(PlayerCharacter player) {
        log.info("🚀 启动【全自动网格测绘雷达】！");
        log.info("👉 请将游戏置顶，确保左上角坐标和你的紫色名字清晰可见！");

        // 1. 抓取基准点 A
        log.info("🎯 请站在原地不要动！正在抓取点A...");
        CalibPoint pointA = capturePoint(player);
        if (pointA == null) return;
        log.info("✅ 点A抓取成功: 逻辑[{},{}], 物理脚底[{},{}]", pointA.logicX, pointA.logicY, pointA.physX, pointA.physY);

        // 2. 让首领跑动，抓取点 B
        log.info("🏃‍♂️ 【行动指令】: 请往右下方或者右边跑两步！5秒后自动抓拍...");
        TaskSleep.sleep(10000);
        CalibPoint pointB = capturePoint(player);
        if (pointB == null) return;
        log.info("✅ 点B抓取成功: 逻辑[{},{}], 物理脚底[{},{}]", pointB.logicX, pointB.logicY, pointB.physX, pointB.physY);

        // 3. 让首领换个方向跑，抓取点 C (解二元一次方程必须要有3个不在一条直线上的点)
        log.info("🏃‍♂️ 【行动指令】: 请换个方向，往左下方或者左边跑两步！5秒后自动抓拍...");
        TaskSleep.sleep(10000);
        CalibPoint pointC = capturePoint(player);
        if (pointC == null) return;
        log.info("✅ 点C抓取成功: 逻辑[{},{}], 物理脚底[{},{}]", pointC.logicX, pointC.logicY, pointC.physX, pointC.physY);

        // 4. 三点确立，开始解构底层引擎矩阵！
        calculateAndPrintMatrix(pointA, pointB, pointC);
    }

    /**
     * 核心动作：同时获取逻辑坐标和物理坐标
     */
    private CalibPoint capturePoint(PlayerCharacter player) {

        tracker.updateGlobalVision();

        // 获取逻辑坐标 (调你的左上角 OCR)
        LocationInfo locInfo = locationVisionService.scanCurrentLocation();
        if (locInfo == null || locInfo.mapName == null) {
            log.error("❌ 抓取失败：未识别到左上角逻辑坐标！");
            return null;
        }

        // 获取物理脚底坐标 (调我们昨天写的 Master 雷达)
        int scanWidth = 350;
        int scanHeight = 200;
        int scanStartX = tracker.getWindowBaseX() + (1024 / 2) - (scanWidth / 2);
        int scanStartY = tracker.getWindowBaseY() + (768 / 2) - (scanHeight / 2);

        String centerScanPath = "images/temp/calib_scan.png";
        String playerScanPath = "images/temp/calib_player.png";

        tracker.captureToFile("中心抓拍", centerScanPath, scanStartX, scanStartY, scanStartX + scanWidth, scanStartY + scanHeight);
        ImagePreprocessor.washPurpleTextToBlackAndWhite(centerScanPath, playerScanPath);

        List<OcrWordResult> playerWords = ocr.getAllTextResults(playerScanPath);
        Point physicalAnchor = locationVisionService.extractPlayerPhysicalAnchor(playerWords, player.getName(), scanStartX, scanStartY, 0);

        if (physicalAnchor == null) {
            log.error("❌ 抓取失败：未识别到玩家脚底物理坐标！");
            return null;
        }

        return new CalibPoint(locInfo.x, locInfo.y, physicalAnchor);
    }

    /**
     * 用克莱姆法则解构矩阵
     */
    private void calculateAndPrintMatrix(CalibPoint p1, CalibPoint p2, CalibPoint p3) {
        log.info("🧮 数据采集成组，正在逆向解析 2.5D 引擎代码...");

        double dx1 = p2.physX - p1.physX, dy1 = p2.physY - p1.physY;
        double dlx1 = p2.logicX - p1.logicX, dly1 = p2.logicY - p1.logicY;

        double dx2 = p3.physX - p1.physX, dy2 = p3.physY - p1.physY;
        double dlx2 = p3.logicX - p1.logicX, dly2 = p3.logicY - p1.logicY;

        double det = dlx1 * dly2 - dlx2 * dly1;
        if (det == 0) {
            log.error("❌ 解析失败：你这两次跑动的方向在一条直线上！请确保两次跑动呈夹角（比如先往右，再往左下）！");
            return;
        }

        double ux = (dx1 * dly2 - dx2 * dly1) / det;
        double vx = (dlx1 * dx2 - dlx2 * dx1) / det;
        double uy = (dy1 * dly2 - dy2 * dly1) / det;
        double vy = (dlx1 * dy2 - dlx2 * dy1) / det;

        log.info("========================================");
        log.info("🎯 【引擎逆向破解成功】得出终极转换参数：");
        log.info("常量 ux (X逻辑变化导致屏幕X移动) = {}", String.format("%.4f", ux));
        log.info("常量 vx (Y逻辑变化导致屏幕X移动) = {}", String.format("%.4f", vx));
        log.info("常量 uy (X逻辑变化导致屏幕Y移动) = {}", String.format("%.4f", uy));
        log.info("常量 vy (Y逻辑变化导致屏幕Y移动) = {}", String.format("%.4f", vy));
        log.info("========================================");
    }
}
