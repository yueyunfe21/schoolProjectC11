package com.bot.dhxy.window.control;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * G150（G145 P5 收窄版，用户拍板 2026-09-04）：轮间衔接必须是 80/17/3 重尾混合分布
 * （顺手连开 2~8s / 小歇 20~60s / 走开 90s~3min），不许退回恒定 250ms，也不许退化成
 * "每轮必歇固定区间"的窄带假分布。
 * v2 用户纠偏：真正的作息点=修罗轮与轮之间（云端 XiuluoTaskV2），run 级衔接只是兜底——
 * 首版只装在 run 级，按每 run 几十轮的配置几乎永不触发。
 */
class G150InterRoundRestContractTest {

    private static final Path CONTROL_SERVICE = Path.of(
            "src/main/java/com/bot/dhxy/window/control/WindowTaskControlService.java");
    private static final Path XIULUO_TASK = Path.of(
            "../dhxy-cloud-brain/src/main/java/com/bot/dhxy/task/xiuluo/XiuluoTaskV2.java");

    @Test
    void restSamplesFollowApprovedMixture() throws Exception {
        Method next = WindowTaskControlService.class.getDeclaredMethod("nextInterRoundRestMs");
        next.setAccessible(true);
        int quick = 0;
        int shortRest = 0;
        int longRest = 0;
        int samples = 20_000;
        for (int i = 0; i < samples; i++) {
            long value = (long) next.invoke(null);
            if (value >= 2_000L && value <= 8_000L) {
                quick++;
            } else if (value >= 20_000L && value <= 60_000L) {
                shortRest++;
            } else if (value >= 90_000L && value <= 180_000L) {
                longRest++;
            } else {
                throw new AssertionError("轮间歇落在三档区间之外: " + value);
            }
        }
        double quickRatio = quick / (double) samples;
        double shortRatio = shortRest / (double) samples;
        double longRatio = longRest / (double) samples;
        assertTrue(Math.abs(quickRatio - 0.80D) < 0.03D, "顺手连开占比须≈80%: " + quickRatio);
        assertTrue(Math.abs(shortRatio - 0.17D) < 0.03D, "小歇占比须≈17%: " + shortRatio);
        assertTrue(Math.abs(longRatio - 0.03D) < 0.015D, "走开占比须≈3%（用户拍板压低防拖节奏）: " + longRatio);
    }

    @Test
    void successContinuationUsesRestNotConstantRetry() throws Exception {
        String source = Files.readString(CONTROL_SERVICE, StandardCharsets.UTF_8);
        int method = source.indexOf("private boolean scheduleNextConfiguredRun(");
        assertTrue(method > 0, "找不到成功续跑方法；改了结构就更新本合同");
        String body = source.substring(method, source.indexOf("private static long nextInterRoundRestMs", method));
        assertTrue(body.contains("nextInterRoundRestMs()"),
                "成功续跑的轮间衔接必须走混合分布休息");
        assertFalse(body.contains("failureRetryDelayMs(windowId, 1)"),
                "成功续跑不许退回恒定 ~250ms 衔接");
    }

    @Test
    void xiuluoRoundSeamRestsWithApprovedMixture() throws Exception {
        String source = Files.readString(XIULUO_TASK, StandardCharsets.UTF_8);
        // 掷骰点必须在"本轮收尾且还有下一轮"的缝上——最后一轮之后不歇（run 级另有兜底）。
        int seam = source.indexOf("skeleton finished, completed={}");
        assertTrue(seam > 0, "找不到修罗轮收尾日志；改了结构就更新本合同");
        String vicinity = source.substring(seam, Math.min(source.length(), seam + 400));
        assertTrue(vicinity.contains("if (progress.shouldStartNextRound())"),
                "轮间歇必须只在还有下一轮时触发");
        assertTrue(vicinity.contains("restBetweenRounds(context, round)"),
                "修罗轮与轮之间必须掷骰休息——这是 G150 的真正作息点");
        // 分布常量与客户端 run 级兜底一致（80/17/3）。
        assertTrue(source.contains("REST_QUICK_PROBABILITY = 0.80D"), "顺手连开占比须 80%");
        assertTrue(source.contains("REST_SHORT_PROBABILITY = 0.17D"), "小歇占比须 17%");
        assertTrue(source.contains("REST_LONG_MIN_MS = 90_000L"), "走开档下界须 90s");
        // 长歇必须可被暂停/停止即时打断——切片睡眠，不许一觉 3 分钟。
        int restMethod = source.indexOf("private void restBetweenRounds(");
        assertTrue(restMethod > 0, "找不到 restBetweenRounds 方法");
        String restBody = source.substring(restMethod, Math.min(source.length(), restMethod + 700));
        assertTrue(restBody.contains("TaskSleep.sleepOrStop"), "轮间歇必须走可停止睡眠");
        assertTrue(restBody.contains("Math.min(1_000L, remaining)"), "轮间歇必须切 1s 片响应暂停/停止");
    }
}
