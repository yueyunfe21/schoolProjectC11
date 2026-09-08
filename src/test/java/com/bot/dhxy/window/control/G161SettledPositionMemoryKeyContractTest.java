package com.bot.dhxy.window.control;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * G161（2026-09-06 用户裁定，医宝宝连误点事故）：NPC 点击记忆的场景键必须是"停稳到达后"的位置。
 *
 * <p>根因=G130 值判稳上线后老 OCR 位置链失效，getMe() 停留在出发点，2026-09-02 起记忆键
 * 全部归错档（player:120,83/415,235/14,19…），同一抽屉混入不同落脚格的答案。
 * 三半修：①停稳事实喂位置桥（治本）②加载隔离出发点条目（用户令删，G097 修正案）
 * ③写入拒学越界键。实库回放：839 条保留 769/丢 70，保留最大距离 6、被丢最小 12，中间空带。</p>
 */
class G161SettledPositionMemoryKeyContractTest {

    private static final Path HANDLER = Path.of(
            "../dhxy-cloud-brain/src/main/java/com/yueyunfe/dhxy/cloudbrain/observation/CloudObservationHttpHandler.java");
    private static final Path MEMORY_STORE = Path.of(
            "../dhxy-cloud-brain/src/main/java/com/yueyunfe/dhxy/cloudbrain/NpcClickMemoryStore.java");

    @Test
    void stableFactsFeedThePositionBridge() throws Exception {
        String source = Files.readString(HANDLER, StandardCharsets.UTF_8);
        int loop = source.indexOf("G161 stable-value settled position");
        assertTrue(loop > 0, "停稳事实必须喂位置桥——否则 getMe() 永远停在出发点（G130 漏档治本）");
        int block = source.indexOf("for (ObservationPathingFact fact : request.pathingFacts()) {");
        assertTrue(block > 0 && block < loop, "喂桥必须遍历本批 pathingFacts");
        String vicinity = source.substring(block, loop);
        assertTrue(vicinity.contains("ObservationPathingState.ARRIVED")
                        && vicinity.contains("ObservationPathingState.STOPPED_AWAY"),
                "ARRIVED 与 STOPPED_AWAY 两种停稳终局都必须喂桥");
        assertTrue(vicinity.contains("lastStableValuePositionByWindow"),
                "电平重报必须去噪——同窗同值只发一次");
        assertTrue(source.contains("\"PATHING_COORDINATE_RESOLVED\",\n                    null,\n                    fact.intentId()"),
                "必须复用既有桥类型，observerSeq 栅栏照旧");
    }

    @Test
    void settledKeyGateGuardsBothLoadAndIngest() throws Exception {
        String source = Files.readString(MEMORY_STORE, StandardCharsets.UTF_8);
        assertTrue(source.contains("SETTLED_KEY_MAX_TILE_DISTANCE = 6"),
                "阈值 6 来自实库回放（保留最大 6/被丢最小 12 的空带），改动须重新回放并更新本合同");
        assertTrue(source.contains("G161 dropped departure-keyed NPC click memory at load"),
                "加载期必须整条丢弃出发点归档条目并留日志（用户令删，G097 修正案）");
        assertTrue(source.contains("G161 player coordinate is not a settled-by-target position"),
                "写入期必须拒学越界键——点照点，只是不记");
        int gate = source.indexOf("if (!isSettledSceneKey(playerMapX, playerMapY, targetMapX, targetMapY)) {");
        int reject = source.indexOf("return OutcomeUpdate.rejected(\"missing old-style NPC click identity");
        assertTrue(gate > 0 && reject > 0 && gate > reject,
                "拒学门必须在身份完整性检查之后（缺坐标先按缺失拒，不进距离判定）");
    }

    @Test
    void settledKeyBoundaryIsExact() throws Exception {
        Method m = Class.forName("com.yueyunfe.dhxy.cloudbrain.NpcClickMemoryStore")
                .getDeclaredMethod("isSettledSceneKey",
                        Integer.class, Integer.class, Integer.class, Integer.class);
        m.setAccessible(true);
        assertTrue((boolean) m.invoke(null, 116, 70, 116, 70), "同格必须通过");
        assertTrue((boolean) m.invoke(null, 110, 70, 116, 70), "切比雪夫 6 必须通过（实库保留上界）");
        assertFalse((boolean) m.invoke(null, 109, 70, 116, 70), "切比雪夫 7 必须拒绝");
        assertFalse((boolean) m.invoke(null, 120, 83, 116, 70), "事故键 player:120,83 必须被拒（dist=13）");
        assertFalse((boolean) m.invoke(null, 415, 235, 116, 70), "窗口坐标泄漏键必须被拒");
        assertFalse((boolean) m.invoke(null, null, 70, 116, 70), "缺坐标必须拒绝（fail-closed）");
    }
}
