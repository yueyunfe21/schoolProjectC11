package com.bot.dhxy.tools;

import com.bot.dhxy.core.GameClientTracker;
import com.bot.dhxy.core.TextRecognizer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.awt.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
public class OcrHoverCalibrator implements CommandLineRunner {

    @Autowired
    private TextRecognizer ocr;
    @Autowired
    private GameClientTracker tracker;
    @Autowired
    private CoordinateHelper coordinateHelper;

    private static final String TARGET_MAP_NAME = "大雁塔六层";

    private static final int ABS_X1 = 1473;
    private static final int ABS_Y1 = 409
            ;
    private static final int ABS_X2 = 2034;
    private static final int ABS_Y2 = 829;

    private static final boolean ENABLE_CALIBRATOR = false;

    @Override
    public void run(String... args) throws Exception {
        if (!ENABLE_CALIBRATOR) return;
        tracker.locateWindow();

        System.out.println("\n=================================================");
        System.out.println("🚀 坐标加固版雷达启动！正在排除 [等级] 干扰...");
        System.out.println("=================================================");

        SamplePoint p1 = capturePointWithOcr("点 A");
        if (p1 == null) return;
        System.out.println("✅ 点 A 抓取成功！逻辑坐标: " + p1.logicX + "," + p1.logicY);

        System.out.println("\n⏳ 请移动鼠标到另一个位置，等待 3 秒...");
        Thread.sleep(3000);

        SamplePoint p2 = capturePointWithOcr("点 B");
        if (p2 == null) return;
        System.out.println("✅ 点 B 抓取成功！逻辑坐标: " + p2.logicX + "," + p2.logicY);

        double scaleX = (double) (p2.relativeX - p1.relativeX) / (p2.logicX - p1.logicX);
        double scaleY = (double) (p2.relativeY - p1.relativeY) / (p2.logicY - p1.logicY);
        int zeroOffsetX = (int) Math.round(p1.relativeX - p1.logicX * scaleX);
        int zeroOffsetY = (int) Math.round(p1.relativeY - p1.logicY * scaleY);

        CoordinateHelper.MapTransform transform = new CoordinateHelper.MapTransform(zeroOffsetX, zeroOffsetY, scaleX, scaleY);
        coordinateHelper.saveNewMapConfig(TARGET_MAP_NAME, transform);

        System.out.println("\n========================================================");
        System.out.println("🎉 测绘完成！这次坐标绝对准了！数据已存入 config/maps.json");
        System.out.println("========================================================\n");
    }

    private SamplePoint capturePointWithOcr(String name) throws InterruptedException {
        Point lastPos = MouseInfo.getPointerInfo().getLocation();
        long stableStartTime = System.currentTimeMillis();

        while (true) {
            Thread.sleep(50);
            Point currentPos = MouseInfo.getPointerInfo().getLocation();

            if (!currentPos.equals(lastPos)) {
                lastPos = currentPos;
                stableStartTime = System.currentTimeMillis();
            } else {
                if (System.currentTimeMillis() - stableStartTime >= 3000) {
                    Toolkit.getDefaultToolkit().beep();

                    double ratio = coordinateHelper.getScaleRatio();
                    int relativeX = (int)(currentPos.x / ratio) - tracker.getWindowBaseX();
                    int relativeY = (int)(currentPos.y / ratio) - tracker.getWindowBaseY();

                    String imgPath = "images/temp/coord_scan.png";
                    int scanX1 = (int)(ABS_X1 / ratio);
                    int scanY1 = (int)(ABS_Y1 / ratio);
                    int scanX2 = (int)(ABS_X2 / ratio);
                    int scanY2 = (int)(ABS_Y2 / ratio);

                    tracker.captureToFile("坐标识别区", imgPath, scanX1, scanY1, scanX2, scanY2);

                    List<TextRecognizer.OcrWordResult> words = ocr.getAllTextResults(imgPath);

                    // 🌟 核心改进：更严格的坐标筛选算法
                    Pattern coordPattern = Pattern.compile("(\\d+)[^\\d]+(\\d+)");

                    for (TextRecognizer.OcrWordResult w : words) {
                        String text = w.getText();
                        System.out.println("🔍 [分析] 正在检查: [" + text + "]");

                        // 1. 如果包含“等级”，直接跳过，这绝对不是坐标
                        if (text.contains("等级") || text.contains("级")) continue;

                        Matcher m = coordPattern.matcher(text);
                        if (m.find()) {
                            int logicX = Integer.parseInt(m.group(1));
                            int logicY = Integer.parseInt(m.group(2));

                            // 2. 简单的合理性校验（坐标通常不会只有一位数，且不会是 15-40 这种连号）
                            // 如果你发现误判还在，我们这里可以加逻辑

                            System.out.println("🎯 [命中] 成功锁定坐标数字: " + logicX + "," + logicY);
                            Toolkit.getDefaultToolkit().beep();
                            return new SamplePoint(relativeX, relativeY, logicX, logicY);
                        }
                    }

                    log.error("❌ 没能识别出正确的坐标。请检查截图是否包含了过多的【等级】干扰文字。");
                    return null;
                }
            }
        }
    }

    public static class SamplePoint {
        public int relativeX, relativeY, logicX, logicY;
        public SamplePoint(int rx, int ry, int lx, int ly) {
            this.relativeX = rx; this.relativeY = ry; this.logicX = lx; this.logicY = ly;
        }
    }
}