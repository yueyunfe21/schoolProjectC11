package com.bot.dhxy.tools;

import com.bot.dhxy.core.GameClientTracker;
import com.bot.dhxy.core.TextRecognizer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.List;

/**
 * 🗣️ 智能 NPC 交互助手 (带遮挡检测)
 */
@Slf4j
@Component
public class NpcInteractHelper {

    @Autowired
    private CoordinateHelper coordinateHelper;
    @Autowired
    private GameClientTracker tracker;
    @Autowired
    private TextRecognizer ocr;

    /**
     * 智能交互逻辑：先普攻，被挡住了再放 Ctrl 大招
     *
     * @param mapName 地图名字
     * @param logicX  NPC 逻辑 X
     * @param logicY  NPC 逻辑 Y
     * @param npcName NPC 的名字（用于 OCR 校验对话框是否成功打开）
     */
    public void talkToNpc(String mapName, int logicX, int logicY, String npcName) {
        try {
            tracker.locateWindow();
            Point targetPoint = coordinateHelper.getPhysicalMapPoint(mapName, logicX, logicY);

            if (targetPoint == null) {
                log.error("❌ 交互失败：未找到地图 [{}] 数据", mapName);
                return;
            }

            log.info("📍 锁定 NPC [{}], 坐标: {},{}", npcName, targetPoint.x, targetPoint.y);
            Robot robot = new Robot();

            // ==========================================
            // 第一阶段：尝试普通点击
            // ==========================================
            robot.mouseMove(targetPoint.x, targetPoint.y);
            Thread.sleep(150);

            log.info("🖱️ 尝试普通左键点击...");
            robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
            Thread.sleep(50);
            robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);

            // 给游戏 0.8 秒的时间弹出对话框并渲染文字
            Thread.sleep(800);

            // ==========================================
            // 第二阶段：检查对话框是否出现
            // ==========================================
            if (isDialogOpened(npcName)) {
                log.info("✅ 普通点击成功！对话框已弹出。");
                return; // 成功了，直接收工！
            }

            // ==========================================
            // 第三阶段：被遮挡了！触发 Ctrl 连招
            // ==========================================
            log.warn("⚠️ 未检测到对话框，可能被玩家遮挡。启动 Ctrl 防遮挡协议！");

            // 重新把鼠标移回去（防止手抖碰到了）
            robot.mouseMove(targetPoint.x, targetPoint.y);
            Thread.sleep(100);

            // 唤出重叠列表
            robot.keyPress(KeyEvent.VK_CONTROL);
            Thread.sleep(50);
            robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
            Thread.sleep(50);
            robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
            Thread.sleep(50);
            robot.keyRelease(KeyEvent.VK_CONTROL);

            Thread.sleep(300); // 等待列表出现

            // 偏移点击黄字 NPC
            int OFFSET_X = 40;
            int OFFSET_Y = 20;
            robot.mouseMove(targetPoint.x + OFFSET_X, targetPoint.y + OFFSET_Y);
            Thread.sleep(100);

            robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
            Thread.sleep(50);
            robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);

            log.info("✅ Ctrl 防遮挡连招执行完毕！");

        } catch (Exception e) {
            log.error("❌ 交互异常", e);
        }
    }

    /**
     * 🧠 视觉检测：对话框有没有打开？
     */
    private boolean isDialogOpened(String npcName) {
        String imgPath = "images/temp/dialog_check.png";

        // 🌟 重点：这里你要根据你的游戏窗口，截取“对话框”通常出现的区域。
        // 为了速度快，千万别全屏截图！只截底部或右侧对话框的区域。
        // 下面是一个粗略的估算区域 (假设对话框在游戏窗口中下部)：
        int dialogX = 100; // 相对窗口的 X
        int dialogY = 400; // 相对窗口的 Y (往底部靠)
        int dialogW = 600; // 宽度
        int dialogH = 300; // 高度

        // 使用你的 tracker 截图
        tracker.captureToFile("对话框检测", imgPath,
                tracker.getWindowBaseX() + dialogX,
                tracker.getWindowBaseY() + dialogY,
                dialogW, dialogH);

        // 调用 OCR
        List<TextRecognizer.OcrWordResult> words = ocr.getAllTextResults(imgPath);

        // 如果 OCR 在这个区域内看到了 NPC 的名字，说明对话框 100% 打开了
        for (TextRecognizer.OcrWordResult w : words) {
            if (w.getText().contains(npcName)) {
                return true;
            }
        }
        return false;
    }
}