package com.bot.dhxy.tools;

import com.bot.dhxy.core.GameClientTracker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.awt.*;

/**
 * 🎯 坐标精准度验证器
 * 专门用来测试 maps.json 里的数据准不准
 */
@Slf4j
@Component
public class MapVerifyTester implements CommandLineRunner {

    @Autowired
    private CoordinateHelper coordinateHelper;

    @Autowired
    private GameClientTracker tracker;

    @Override
    public void run(String... args) throws Exception {
        // 🌟 核心开关：测绘完长安后，取消下面这行的注释，运行程序验证！
        //verifyMapPoint("长安东", 289, 19);
    }

    public void verifyMapPoint(String mapName, int logicX, int logicY) throws Exception {
        log.info("🚀 启动坐标验证：地图={}, 逻辑点=({}, {})", mapName, logicX, logicY);

        // 1. 强制刷新窗口位置，防止你刚才拖动了窗口
        tracker.locateWindow();

        // 2. 调用 CoordinateHelper 计算屏幕物理像素点
        Point targetPoint = coordinateHelper.getPhysicalMapPoint(mapName, logicX, logicY);

        if (targetPoint != null) {
            log.info("📍 矩阵计算成功！目标物理像素坐标: X={}, Y={}", targetPoint.x, targetPoint.y);

            // TODO: Route this dev-only physical mouse move through the input layer
            // once InputProvider exposes a physical-coordinate move API.
            // 3. 模拟鼠标移动（不点击，只悬停）
            Robot robot = new Robot();
            robot.mouseMove(targetPoint.x, targetPoint.y);

            log.info("✅ 鼠标已移动。请切回游戏查看：鼠标是否指在了大地图的 ({}, {}) 处？", logicX, logicY);

            // 4. 滴一声提示完成
            Toolkit.getDefaultToolkit().beep();
        } else {
            log.error("❌ 验证失败：在 maps.json 中没找到地图 [{}] 的配置数据！", mapName);
        }
    }
}
