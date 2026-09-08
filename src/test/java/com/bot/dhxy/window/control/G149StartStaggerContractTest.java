package com.bot.dhxy.window.control;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * G149（G145 P3，用户拍板 2026-09-04：窗间错峰上限 20 秒）：
 * 批量启动禁止"同秒连发+恒定顺序+恒定滞后"；战斗中 UI 清理禁止精确 40s 节拍器。
 */
class G149StartStaggerContractTest {

    private static final Path CONTROL_SERVICE = Path.of(
            "src/main/java/com/bot/dhxy/window/control/WindowTaskControlService.java");
    private static final Path CLOUD_AUTO_COMBAT = Path.of(
            "../dhxy-cloud-brain/src/main/java/com/bot/dhxy/service/AutoCombatService.java");

    @Test
    void staggerStaysWithinUserApprovedBounds() throws Exception {
        Method next = WindowTaskControlService.class.getDeclaredMethod("nextStartStaggerMs");
        next.setAccessible(true);
        long min = Long.MAX_VALUE;
        long max = Long.MIN_VALUE;
        for (int i = 0; i < 300; i++) {
            long value = (long) next.invoke(null);
            assertTrue(value >= 5_000L && value <= 20_000L,
                    "窗间错峰必须落在用户拍板的 5~20 秒内: " + value);
            min = Math.min(min, value);
            max = Math.max(max, value);
        }
        assertTrue(max > min, "错峰必须真随机，不许退化成常数");
    }

    @Test
    void shuffleKeepsGroupMembership() throws Exception {
        Method shuffle = WindowTaskControlService.class.getDeclaredMethod("shuffleGroup", List.class);
        shuffle.setAccessible(true);
        List<String> group = List.of("w1", "w2", "w3", "w4", "w5");
        boolean orderChangedAtLeastOnce = false;
        for (int i = 0; i < 50; i++) {
            @SuppressWarnings("unchecked")
            List<String> shuffled = (List<String>) shuffle.invoke(null, group);
            assertEquals(group.size(), shuffled.size());
            assertTrue(shuffled.containsAll(group), "洗牌只许换顺序，不许丢窗/加窗");
            if (!shuffled.equals(group)) {
                orderChangedAtLeastOnce = true;
            }
        }
        assertTrue(orderChangedAtLeastOnce, "50 次洗牌必须至少一次改变顺序");
    }

    @Test
    void batchStartSourceUsesShuffledGroupsAndStagger() throws Exception {
        String source = Files.readString(CONTROL_SERVICE, StandardCharsets.UTF_8);
        assertEquals(2, countOf(source, "List<String> leaderOrder = shuffleGroup("),
                "两个批量启动入口都必须洗牌队长组");
        assertEquals(2, countOf(source, "List<String> memberOrder = shuffleGroup("),
                "两个批量启动入口都必须洗牌成员组");
        assertEquals(2, countOf(source, "staggerBetweenWindowStarts(startEpoch)"),
                "两个入口的队长串行组都必须窗间错峰");
        assertEquals(2, countOf(source, "CompletableFuture.delayedExecutor(memberStaggerMs"),
                "两个入口的成员异步组都必须累计延迟错峰");
        assertFalse(source.contains("List<String> startOrder = "),
                "同一排序列表双循环背靠背拉起的旧结构必须消失");
    }

    @Test
    void cloudInCombatUiCleanIsJittered() throws Exception {
        String source = Files.readString(CLOUD_AUTO_COMBAT, StandardCharsets.UTF_8);
        assertFalse(source.contains("COMBAT_UI_CLEAN_INTERVAL_MS ="),
                "精确 40s 清理常量必须消失");
        assertTrue(source.contains("COMBAT_UI_CLEAN_MIN_INTERVAL_MS = 30_000L")
                        && source.contains("COMBAT_UI_CLEAN_JITTER_MS = 25_000L"),
                "续拍必须是 30~55s 随机");
        assertTrue(source.contains("COMBAT_UI_CLEAN_ENTRY_JITTER_MAX_MS"),
                "首拍必须带进战随机偏置（五窗同瞬进战，锚点是游戏给的）");
        assertTrue(countOf(source, "state.runnerUiCleanJitterMs = 0L;") >= 4,
                "随机阈值必须在消费与全部复位点清零重掷（3 复位点+清理收尾）");
    }

    private static int countOf(String source, String needle) {
        int count = 0;
        int index = 0;
        while ((index = source.indexOf(needle, index)) >= 0) {
            count++;
            index += needle.length();
        }
        return count;
    }
}
