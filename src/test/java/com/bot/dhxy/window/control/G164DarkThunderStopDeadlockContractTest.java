package com.bot.dhxy.window.control;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * G164（2026-09-07 用户裁定，暗雷不动死锁）：一条已物理停稳的暗雷腿必须能进 RUN_DARK_THUNDER，
 * 不许被 recovery episode 合成的 pathingActive 挡在 PARK_PATHING 上。
 *
 * <p>事故（01:24~01:26）：darkThunder=true, stopped=true, pathingActive=true 三者同时为真，
 * 决策表 pathingActive 门排在 stopped 分支之前无条件返回 PARK_PATHING，暗雷巡游永远够不着；
 * pathingActive 由 trackerLinkRecoveryEpisode 合成，而暗雷腿"点绿链不移动"是正常形态却被
 * 当成卡住去 arm 恢复，episode 不清→pathingActive 恒真→死锁。5 次重按+回家重接全程一步没做。</p>
 */
class G164DarkThunderStopDeadlockContractTest {

    private static final Path TASK = Path.of(
            "../dhxy-cloud-brain/src/main/java/com/bot/dhxy/task/tianting/TiantingTask.java");

    /** 事故 facts 的纯函数回放：stopped+darkThunder 必须判 RUN_DARK_THUNDER。 */
    @Test
    void stoppedDarkThunderLegRunsPatrolNotPark() throws Exception {
        Class<?> decision = Class.forName("com.bot.dhxy.task.tianting.TiantingSubtaskDecision");
        Class<?> factsCls = Class.forName("com.bot.dhxy.task.tianting.TiantingSubtaskDecision$Facts");
        Constructor<?> ctor = factsCls.getDeclaredConstructors()[0];
        Method decide = decision.getMethod("decide", factsCls);

        // 事故当时的 facts：inCombat=F combatJustEnded=F titlePresent=T panelRead=F darkThunder=T
        // pathingActive=T stopped=T dialogOutcomePending=F trackerLinkReady=F awaitingDialogSinceMs=0 fengyaofu=F
        Object facts = ctor.newInstance(false, false, true, false, true, true, true, false, false, 0L, false);
        Object action = decide.invoke(null, facts);
        assertEquals("RUN_DARK_THUNDER", String.valueOf(action),
                "停稳的暗雷腿必须进巡游，不许被合成 pathingActive 卡在 PARK_PATHING");
    }

    /** 普通腿（darkThunder=false）在 pathingActive 时语义不变，仍 PARK_PATHING。 */
    @Test
    void normalLegStillParksWhilePathingActive() throws Exception {
        Class<?> decision = Class.forName("com.bot.dhxy.task.tianting.TiantingSubtaskDecision");
        Class<?> factsCls = Class.forName("com.bot.dhxy.task.tianting.TiantingSubtaskDecision$Facts");
        Constructor<?> ctor = factsCls.getDeclaredConstructors()[0];
        Method decide = decision.getMethod("decide", factsCls);
        // darkThunder=F, pathingActive=T, stopped=F
        Object facts = ctor.newInstance(false, false, true, false, false, true, false, false, false, 0L, false);
        assertEquals("PARK_PATHING", String.valueOf(decide.invoke(null, facts)),
                "普通腿寻路中仍必须 PARK_PATHING（G164 只放行停稳的暗雷腿）");
    }

    /** 战斗态永远优先，暗雷兜底不得抢占 PARK_COMBAT。 */
    @Test
    void combatStillOutranksDarkThunderGuard() throws Exception {
        Class<?> decision = Class.forName("com.bot.dhxy.task.tianting.TiantingSubtaskDecision");
        Class<?> factsCls = Class.forName("com.bot.dhxy.task.tianting.TiantingSubtaskDecision$Facts");
        Constructor<?> ctor = factsCls.getDeclaredConstructors()[0];
        Method decide = decision.getMethod("decide", factsCls);
        // inCombat=T, darkThunder=T, stopped=T
        Object facts = ctor.newInstance(true, false, true, false, true, true, true, false, false, 0L, false);
        assertEquals("PARK_COMBAT", String.valueOf(decide.invoke(null, facts)),
                "战斗态必须优先于 G164 暗雷兜底");
    }

    @Test
    void physicalStopSuppressesSynthesizedPathingActive() throws Exception {
        String source = Files.readString(TASK, StandardCharsets.UTF_8);
        assertTrue(source.contains("boolean physicallyStopped = pathing != null"),
                "必须计算真实物理停稳");
        assertTrue(source.contains("&& !physicallyStopped)"),
                "合成 pathingActive 必须让位于物理停稳——否则陈旧 episode 永久挟持决策");
    }

    @Test
    void darkThunderNoMovementRetiresEpisodeInsteadOfArmingRecovery() throws Exception {
        String source = Files.readString(TASK, StandardCharsets.UTF_8);
        int gate = source.indexOf("if (episode.darkThunder()) {");
        assertTrue(gate > 0, "暗雷腿 NO_MOVEMENT 分支必须存在");
        // arm 恢复的调用点：以传入的 source 字符串锚定（避开方法定义处的同名标识符）。
        int arm = source.indexOf("\"tianting:tracker-terminal-no-movement\",");
        assertTrue(arm > gate,
                "暗雷退休门必须在 arm 恢复之前——NO_MOVEMENT 对暗雷是正常巡游态不是卡死");
        String body = source.substring(gate, arm);
        assertTrue(body.contains("trackerLinkRecoveryEpisode = null"),
                "暗雷 NO_MOVEMENT 必须退休 episode，解开 pathingActive 合成");
        assertTrue(body.contains("return;"),
                "暗雷退休后必须 return，不得继续走到 arm 恢复");
    }
}
