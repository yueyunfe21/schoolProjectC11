package com.bot.dhxy.tools;

import com.bot.dhxy.core.GameClientTracker;
import com.bot.dhxy.core.ImageFinder;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;

@Slf4j
@Component
@RequiredArgsConstructor
public class GameStateUtil {

    private static final long MOVE_DETECT_WINDOW_MS = 3000;
    private static final long MOVE_SAMPLE_INTERVAL_MS = 250;
    private static final double MOVE_HIT_RATIO_THRESHOLD = 0.2; // 1/5
    private static final double MOVE_DIFF_RATIO = 0.05; // 默认两帧匹配阈值

    private final GameClientTracker tracker;
    private final CoordinateHelper coordinateHelper;

    private BufferedImage dialogTemplate;

    @PostConstruct
    public void initTemplates() {
        try {
            File file = new File("images/template/dialog.png");
            if (file.exists()) {
                dialogTemplate = ImageIO.read(file);
                log.info("Loaded dialog template: {}", file.getAbsolutePath());
            } else {
                log.error("Dialog template not found: {}", file.getAbsolutePath());
            }
        } catch (Exception e) {
            log.error("Failed to load dialog template", e);
        }
    }

    public boolean isDialogOpened() {
        if (dialogTemplate == null) {
            return false;
        }

        int[] searchArea = coordinateHelper.getScaledRect(635, 312, 150, 200);
        BufferedImage largeFrame = tracker.captureToMemory(
                "dialog-scan-area",
                searchArea[0],
                searchArea[1],
                searchArea[2],
                searchArea[3]
        );
        if (largeFrame == null) {
            return false;
        }

        boolean opened = ImageFinder.findTemplateInImage(largeFrame, dialogTemplate, 0.05);
        largeFrame.flush();

        if (opened) {
            log.info("NPC dialog detected");
        }
        return opened;
    }

    /**
     * 判断是否在移动（通过边缘像素差异防抖识别）
     */
    public boolean isMovingByPixelDiff() {
        // ==========================================
        // 🌟 回归初心：侦测左侧边缘风景！
        // X 取 20：避开游戏窗口最外层的 UI 边框。
        // Y 取 300：大概在屏幕中上部，避开了底部的聊天框和顶部的导航栏。
        // 宽高 30x30：切一小块“干净”的地砖或风景。
        // ==========================================
        int[] pics = coordinateHelper.getScaledRect(20, 400, 30, 30);

// 🌟 核心修复：直接使用 pics 里的绝对坐标，绝不二次相加！
        int x1 = pics[0];
        int y1 = pics[1];
        int x2 = pics[2];  // 起点 + 宽度
        int y2 = pics[3];  // 起点 + 高度

        // 🚨 移动日志：记录截取的绝对物理坐标
        //log.info("🏃 [移动侦测] 开始物理侦测，截取区域绝对坐标: x1={}, y1={}, x2={}, y2={}", x1, y1, x2, y2);

        long deadline = System.currentTimeMillis() + MOVE_DETECT_WINDOW_MS;
        int attempts = 0;
        int changedHits = 0;

        while (System.currentTimeMillis() < deadline) {
            BufferedImage frame1 = tracker.captureToMemory("moving-check-frame1", x1, y1, x2, y2);
            if (frame1 == null) {
                if (!sleepQuietly(MOVE_SAMPLE_INTERVAL_MS)) return false;
                continue;
            }

            if (!sleepQuietly(MOVE_SAMPLE_INTERVAL_MS)) {
                frame1.flush();
                return false;
            }

            BufferedImage frame2 = tracker.captureToMemory("moving-check-frame2", x1, y1, x2, y2);
            if (frame2 == null) {
                frame1.flush();
                continue;
            }

            attempts++;

            // 只要地砖发生 5% 以上的位移，就算是在跑动
            boolean changed = !ImageFinder.isMatch(frame1, frame2, MOVE_DIFF_RATIO);
            if (changed) {
                changedHits++;
                // 🚨 移动日志：记录画面发生位移
                //log.info("🏃 [移动侦测] 第 {} 次采样: 画面发生位移 (diff > {})", attempts, MOVE_DIFF_RATIO);
//            } else {
//                // 🚨 移动日志：记录画面静止
//                log.info("🏃 [移动侦测] 第 {} 次采样: 画面静止", attempts);
            }

            frame1.flush();
            frame2.flush();
        }

        if (attempts == 0) {
            log.warn("🏃 [移动侦测] 采样失败，采样次数为 0");
            return false;
        }

        double changedRatio = (double) changedHits / attempts;
        boolean isMoving = changedRatio > MOVE_HIT_RATIO_THRESHOLD;

        // 🚨 移动日志：输出最终统计结论
        log.info("🏃 [移动侦测] 侦测结束 -> 采样次数: {}, 变动次数: {}, 变动占比: {}% (阈值: {}%), 最终判定: {}",
                attempts,
                changedHits,
                String.format("%.1f", changedRatio * 100),
                MOVE_HIT_RATIO_THRESHOLD * 100,
                isMoving ? "✅ 跑动中" : "🛑 已停下");

        return isMoving;
    }

    /**
     * 封装 sleep 和中断处理，避免主流程重复 try/catch。
     */
    private boolean sleepQuietly(long sleepMs) {
        try {
            Thread.sleep(sleepMs);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    public boolean isInBattle() {
        log.warn("isInBattle() is not implemented yet, defaulting to false");
        return false;
    }
}