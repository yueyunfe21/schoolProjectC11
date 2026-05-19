package com.bot.dhxy.tools;

import com.bot.dhxy.core.GameClientTracker;
import com.bot.dhxy.core.ImageFinder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.awt.image.BufferedImage;

@Slf4j
@Component
@RequiredArgsConstructor
public class GameStateUtil {

    private static final long MOVE_DETECT_WINDOW_MS = 2500;
    private static final long MOVE_SAMPLE_INTERVAL_MS = 300;
    private static final int FAST_PASS_HITS = 2;
    private static final double MOVE_DIFF_RATIO = 0.05;

    /**
     * 防止某些动态 UI / 动态背景导致永远误判为移动，从而让任务主循环一直不执行输入动作。
     */
    private static final int MAX_CONSECUTIVE_MOVING_TRUE = 6;

    private final GameClientTracker tracker;
    private final CoordinateHelper coordinateHelper;
    private final ThreadLocal<Integer> consecutiveMovingTrue = ThreadLocal.withInitial(() -> 0);

    /**
     * 判断是否在移动（通过边缘像素差异防抖识别）。
     */
    public boolean isMovingByPixelDiff() {
        int[] pics = coordinateHelper.getScaledRect(20, 400, 30, 30);
        int x1 = pics[0], y1 = pics[1], x2 = pics[2], y2 = pics[3];

        long deadline = System.currentTimeMillis() + MOVE_DETECT_WINDOW_MS;
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

            boolean changed = !ImageFinder.isMatch(frame1, frame2, MOVE_DIFF_RATIO);

            frame1.flush();
            frame2.flush();

            if (changed) {
                changedHits++;
                if (changedHits >= FAST_PASS_HITS) {
                    return handleMovingTrue(changedHits);
                }
            }
        }

        consecutiveMovingTrue.set(0);
        log.info("🛑 [移动侦测] {}秒内仅变动 {} 次，判定为：已停下",
                MOVE_DETECT_WINDOW_MS / 1000.0, changedHits);
        return false;
    }

    private boolean handleMovingTrue(int changedHits) {
        int streak = consecutiveMovingTrue.get() + 1;
        consecutiveMovingTrue.set(streak);

        if (streak >= MAX_CONSECUTIVE_MOVING_TRUE) {
            log.warn("⚠️ [移动侦测] 连续 {} 次判定为移动，疑似动态画面误判；本次强制放行，避免任务主循环卡死。changedHits={}",
                    streak, changedHits);
            consecutiveMovingTrue.set(0);
            return false;
        }

        log.debug("🏃 [移动侦测] 判定为移动，连续次数={} changedHits={}", streak, changedHits);
        return true;
    }

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
