package com.bot.dhxy.cloud.turn;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * G004 的战斗态硬闸只管鼠标：自动战斗的 {@code Alt+8} 面板维护是键盘，必须免闸。
 *
 * <p>这条维护存在的意义就是**在战斗当中**把自动战斗续上；把它塞进鼠标闸，等于恰好在它唯一有用的时候
 * 把它拦掉。2026-07-31 19:07–19:09 队员窗口 {@code hwnd-5600D66} 实锤：云端连发
 * {@code refresh auto combat panel}，客户端连拒 {@code safetyReason=COMBAT_ACTIVE
 * reason=local-combat-active:before-enqueue}，整场战斗一次 {@code ALT_8} 都没发出去。</p>
 */
class CombatGateKeyboardExemptionContractTest {

    private static final Path EXECUTOR =
            Path.of("src/main/java/com/bot/dhxy/cloud/turn/TurnInputStepExecutor.java");

    @Test
    void altShortcutsDoNotGoThroughTheMouseCombatGate() throws Exception {
        String source = Files.readString(EXECUTOR, StandardCharsets.UTF_8);

        int altBlockStart = source.indexOf("BoundWindowKeyboardService.AltShortcut alt =");
        assertTrue(altBlockStart > 0, "找不到 Alt 快捷键分支；改了结构就更新这条合同");
        String altBlock = source.substring(altBlockStart, altBlockStart + 700);

        assertTrue(altBlock.contains("submitKeyboardActions"),
                "Alt 维护必须走免闸的键盘提交");
        assertFalse(altBlock.contains("submitMouseActions("),
                "Alt 维护一旦走回鼠标提交，就会在战斗中被 COMBAT_ACTIVE 拦掉——正是它该干活的时候");
    }

    @Test
    void mouseInputKeepsTheCombatGate() throws Exception {
        String source = Files.readString(EXECUTOR, StandardCharsets.UTF_8);

        int rawStart = source.indexOf("private InputActionExecutionResult submitMouseActionsRaw");
        assertTrue(rawStart > 0);
        String mouseSubmit = source.substring(rawStart, rawStart + 500);

        assertTrue(mouseSubmit.contains("isLocalCombatVisible")
                        && mouseSubmit.contains("COMBAT_ACTIVE"),
                "鼠标动作必须保留战斗硬闸：排队中的走位点击不能打进已经开始的战斗");
    }

    @Test
    void theKeyboardPathStillSharesTheSerializedExactWindowQueue() throws Exception {
        String source = Files.readString(EXECUTOR, StandardCharsets.UTF_8);

        int keyboardStart = source.indexOf("private Result submitKeyboardActions");
        assertTrue(keyboardStart > 0);
        String keyboardSubmit = source.substring(keyboardStart, keyboardStart + 500);

        assertTrue(keyboardSubmit.contains("submitThroughQueue"),
                "免闸只免闸：键盘仍要走同一条序列化的精确窗口队列，不能另开输入通道");
    }
}
