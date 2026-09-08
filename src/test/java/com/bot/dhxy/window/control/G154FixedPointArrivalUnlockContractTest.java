package com.bot.dhxy.window.control;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * G154（2026-09-05 领双 05:08 事故，用户拍板方案 1）：无帧 ARRIVED 事实对固定点需求必须当场开锁。
 * 事故链：云端稳定事实分类 ARRIVED 不带帧号/代号 → unlockArrivalFrame 永远没被调用 → 会话锁死,
 * 固定点候选已入队却发不出去,客户端 FIFO 干等 60s 熔断（今晚 4 次领双仅 1 次靠偶发二次稳定事实抢开）。
 */
class G154FixedPointArrivalUnlockContractTest {

    private static final Path DECISION_ENGINE = Path.of(
            "../dhxy-cloud-brain/src/main/java/com/yueyunfe/dhxy/cloudbrain/DecisionEngine.java");
    private static final Path QUEUE_STORE = Path.of(
            "../dhxy-cloud-brain/src/main/java/com/yueyunfe/dhxy/cloudbrain/NpcClickSmartQueueStore.java");

    @Test
    void framelessArrivedRoutesToFixedPointUnlock() throws Exception {
        String source = Files.readString(DECISION_ENGINE, StandardCharsets.UTF_8);
        int gate = source.indexOf("public String applyNpcArrivalFrameGate(");
        assertTrue(gate > 0, "找不到到达门方法；改了结构就更新本合同");
        String body = source.substring(gate, Math.min(source.length(), gate + 2500));
        assertTrue(body.contains("unlockArrivalFrame("),
                "带帧 ARRIVED 的精确帧开锁不许被移除");
        assertTrue(body.contains("unlockArrivalFixedPoint("),
                "无帧 ARRIVED 必须走固定点开锁，不许再静默落 NO_GATE_CHANGE");
    }

    @Test
    void fixedPointUnlockOnlyActsOnPresetCandidateDemands() throws Exception {
        String source = Files.readString(QUEUE_STORE, StandardCharsets.UTF_8);
        int method = source.indexOf("String unlockArrivalFixedPoint(");
        assertTrue(method > 0, "找不到 unlockArrivalFixedPoint；改了结构就更新本合同");
        String body = source.substring(method, Math.min(source.length(), method + 2200));
        assertTrue(body.contains("targetCandidates"),
                "只有注册时注入了预置固定候选的需求才许无帧开锁——视觉需求照旧等精确帧");
        assertTrue(body.contains("session.unlock()"),
                "会话已建成时必须当场开锁（unlock 自带 ready 发布）");
        assertTrue(body.contains("fixedPointArrivedPending = true"),
                "会话未建成时必须置 pending，备帧时天生解锁——不许丢开锁事实");
    }

    @Test
    void pendingFixedPointSessionIsBornUnlocked() throws Exception {
        String source = Files.readString(QUEUE_STORE, StandardCharsets.UTF_8);
        assertTrue(source.contains("boolean bornUnlocked = replacement || demand.fixedPointArrivedPending;"),
                "备帧建会话必须尊重 pending 天生解锁语义");
        assertTrue(source.contains("G154 fixed-point arrival session born unlocked"),
                "pending 消费必须留日志（验收对账用）");
    }
}
