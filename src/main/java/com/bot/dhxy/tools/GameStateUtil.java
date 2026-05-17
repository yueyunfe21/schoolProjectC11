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

    private static final long MOVE_DETECT_WINDOW_MS = 2500; // 🌟 延长耐心：最高容忍 3.2 秒的假停或过图黑屏
    private static final long MOVE_SAMPLE_INTERVAL_MS = 300; // 🌟 加快采样：每 300 毫秒看一眼
    private static final int FAST_PASS_HITS = 2; // 🌟 极速放行：只要累计看到 2 次画面变动，立刻判定为跑动
    private static final double MOVE_DIFF_RATIO = 0.05; // 默认两帧匹配阈值// 默认两帧匹配阈值

    private final GameClientTracker tracker;
    private final CoordinateHelper coordinateHelper;

    /**
     * 判断是否在移动（通过边缘像素差异防抖识别）
     */
    public boolean isMovingByPixelDiff() {
        int[] pics = coordinateHelper.getScaledRect(20, 400, 30, 30);
        int x1 = pics[0], y1 = pics[1], x2 = pics[2], y2 = pics[3];

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
            boolean changed = !ImageFinder.isMatch(frame1, frame2, MOVE_DIFF_RATIO);

            if (changed) {
                changedHits++;
                // 🌟 核心改造：极速放行！一旦满 2 次立刻打断循环，绝不傻等！
                if (changedHits >= FAST_PASS_HITS) {
                    // log.info("🏃 [移动侦测] 提前确认跑动！(耗时: {}ms)", System.currentTimeMillis() - (deadline - MOVE_DETECT_WINDOW_MS));
                    frame1.flush();
                    frame2.flush();
                    return true;
                }
            }

            frame1.flush();
            frame2.flush();
        }

        // 如果死等了 3.2 秒，变动次数还是没达到 2 次，那就是彻彻底底的真停了！
        log.info("🛑 [移动侦测] 侦测结束 -> {}秒内仅变动 {} 次，判定为：已停下",
                MOVE_DETECT_WINDOW_MS / 1000.0, changedHits);
        return false;
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