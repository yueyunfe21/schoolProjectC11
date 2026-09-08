package com.bot.dhxy.cloud.turn.local;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * G152（2026-09-04 用户质疑实证）：NPC 点击落点禁止零抖动。
 * 日志实锤：同一像素连点 5 次（TOOLTIP relative=(371,239)×5）+候选公式格点分布
 * （x∈{371,490,689,691}）。三处执行面全部加 ±5/±4 均匀抖动；学习/记忆仍存基准点。
 */
class G152NpcClickJitterContractTest {

    private static final Path FIFO_EXECUTOR = Path.of(
            "src/main/java/com/bot/dhxy/cloud/turn/local/NpcArrivalFrameFifoLocalExecutor.java");
    private static final Path CLOUD_NPC_CLICK = Path.of(
            "../dhxy-cloud-brain/src/main/java/com/bot/dhxy/service/NpcClickService.java");

    @Test
    void fifoCandidateClicksAreJitteredAndVerifiedHitsBecomeTheNewBase() throws Exception {
        String source = Files.readString(FIFO_EXECUTOR, StandardCharsets.UTF_8);
        assertTrue(source.contains("Point execClick = jitterForExecution(click, spec);"),
                "FIFO 候选执行必须走抖动点");
        assertTrue(source.contains("insideAllowedRegion(jittered, spec) ? jittered : base"),
                "抖出安全区必须回落基准点，不许裸拒绝");
        // G152 v2（用户设计裁定）：验证成功=真实命中，抖动点接管基点（验证门自带纠错，游走有界）；
        // 未证实/deferred 不接管（miss 点不能当新基准）。
        assertTrue(source.contains("rememberVerifiedPoint(arguments, spec, execClick)"),
                "验证成功必须存实际执行点——成功的抖动点就是新基点");
        assertTrue(source.contains("rememberDeferredPoint(arguments, spec, click)"),
                "未证实的点不许接管基点");
        assertTrue(source.contains("rememberVerifiedPoint(arguments, spec, replayExec)"),
                "重放命中同样接管基点");
        assertTrue(source.contains("base=({}, {}) jittered=({}, {})"),
                "日志必须同时落基准点与抖后点（验收对账用）");
        assertFalse(source.contains("relative=({}, {}) absolute=({}, {})"),
                "旧的零抖动日志格式必须消失");
    }

    @Test
    void ctrlCandidateClicksAreJittered() throws Exception {
        String source = Files.readString(FIFO_EXECUTOR, StandardCharsets.UTF_8);
        int ctrlMatch = source.indexOf("int clickX = scanRect[0] + (int) Math.round(match[0])");
        assertTrue(ctrlMatch > 0, "找不到 Ctrl 候选落点计算；改了结构就更新本合同");
        String vicinity = source.substring(ctrlMatch, Math.min(source.length(), ctrlMatch + 400));
        assertTrue(vicinity.contains("NPC_CLICK_JITTER_X"),
                "Ctrl 候选匹配点不许原样点击");
    }

    @Test
    void cloudLearnedMemoryClickIsJittered() throws Exception {
        String source = Files.readString(CLOUD_NPC_CLICK, StandardCharsets.UTF_8);
        // G157-P1 演进：均匀抖动升级为截断高斯，抖动本身仍是硬合同。
        assertTrue(source.contains("point.x() + gaussianClickOffset"),
                "云端学习点严禁原样重放——同一 NPC 永远同一像素是格点签名");
        assertFalse(source.contains("int clickX = windowBase.x() + point.x();"),
                "旧的零抖动学习点重放必须消失");
    }
}
