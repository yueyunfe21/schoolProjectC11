package com.bot.dhxy.window.control;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * G159（2026-09-05 用户拍板，长尾上限 10s）：绿链后补给的人化延迟。
 * 实测绿链→补给 16 次里 10 次落 2.1~3.0s（5 次精确 2.2s）——流水线底噪形成的窄带谱线。
 * 分布 70% +0.2~1.5s / 25% +2~6s / 5% +8~10s；不睡线程（pending 顺延，既有重试节拍驱动），
 * 到点前每拍重过 capability 闸——到达关窗自然滚到下一安全缝，不新增互斥机制。
 */
class G159FirstAidHumanDelayContractTest {

    private static final Path AUTO_COMBAT = Path.of(
            "../dhxy-cloud-brain/src/main/java/com/bot/dhxy/service/AutoCombatService.java");

    @Test
    void delayIsDrawnOncePerEpisodeAndGatesBeforeTheTaskTurnQueue() throws Exception {
        String source = Files.readString(AUTO_COMBAT, StandardCharsets.UTF_8);
        int gate = source.indexOf("if (state.pendingFollowerFirstAidNotBeforeMs == 0L) {");
        assertTrue(gate > 0, "找不到人化延迟掷骰点；改了结构就更新本合同");
        int queue = source.indexOf("String transactionName = source + \":pending-follower-first-aid\";");
        assertTrue(queue > gate, "延迟门必须在进任务回合队列之前——排进队列再等会占住回合");
        assertTrue(source.substring(gate, queue).contains("return false;"),
                "未到点必须保持 pending 顺延，不许在线程里睡觉");
        assertTrue(source.contains("state.pendingFollowerFirstAidNotBeforeMs = 0L; // G159：新一轮待办重掷"),
                "新一轮待办必须重掷——沿用旧骰子会让延迟退化成常量");
        assertTrue(source.contains("state.pendingFollowerFirstAidNotBeforeMs = 0L; // G159：本轮已消费"),
                "执行完成必须归零");
    }

    @Test
    void delayDistributionRespectsTheTenSecondCap() throws Exception {
        Method m = Class.forName("com.bot.dhxy.service.AutoCombatService")
                .getDeclaredMethod("nextFirstAidHumanDelayMs");
        m.setAccessible(true);
        int quick = 0;
        int walk = 0;
        int slow = 0;
        int samples = 20_000;
        for (int i = 0; i < samples; i++) {
            long v = (long) m.invoke(null);
            if (v >= 200L && v <= 1_500L) {
                quick++;
            } else if (v >= 2_000L && v <= 6_000L) {
                walk++;
            } else if (v >= 8_000L && v <= 10_000L) {
                slow++;
            } else {
                throw new AssertionError("延迟落在三档之外或超过用户拍板的 10s 上限: " + v);
            }
        }
        assertTrue(Math.abs(quick / (double) samples - 0.70D) < 0.03D, "顺手补占比须≈70%: " + quick);
        assertTrue(Math.abs(walk / (double) samples - 0.25D) < 0.03D, "走一段再补占比须≈25%: " + walk);
        assertTrue(Math.abs(slow / (double) samples - 0.05D) < 0.02D, "磨蹭档占比须≈5%: " + slow);
    }
}
