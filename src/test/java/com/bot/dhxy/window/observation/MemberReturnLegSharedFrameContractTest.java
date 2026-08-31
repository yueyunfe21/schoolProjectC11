package com.bot.dhxy.window.observation;

import com.bot.dhxy.config.WindowIsolationProperties;
import com.bot.dhxy.core.GameClientTracker;
import com.bot.dhxy.core.GameContext;
import com.bot.dhxy.input.InputSequences;
import com.bot.dhxy.service.DialogService;
import com.bot.dhxy.tools.CoordinateHelper;
import com.bot.dhxy.window.model.WindowNativeBinding;
import com.bot.dhxy.window.model.WindowPathingIntent;
import com.bot.dhxy.window.model.WindowPathingIntentType;
import com.bot.dhxy.window.model.WindowRole;
import com.bot.dhxy.window.runtime.WindowRuntimeContext;
import com.bot.dhxy.window.runtime.WindowTaskContextHolder;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * E70 合同（2026-08-29）：成员窗口的 G002 断帧门必须给"归队/寻路腿"让路。
 *
 * <p>事故事实（2026-08-28 云端日志 cloud-brain-console.20260829-004319.log，19:22→00:38）：成员归队腿
 * 39 次 {@code CLICKED_NO_MOVEMENT}、0 次移动确认、0 次到达上报。客户端日志同期实锤该腿确实注册了
 * （{@code source=team-return:auto-battle:local-team-return-release}，成员窗 hwnd-2CA0ECE/hwnd-480B38
 * 各十余次），所以"0 次移动"不是"腿没起"，而是断帧门把位置条带冻住了：
 * {@code sharedPositionStripFrame} 的唯一写入方 {@code refreshSharedPositionStripFrame} 只在本门放行
 * 之后被调用，成员被断帧后每一次差值都在拿同一张陈帧和自己比 → 结构性永远"没动"。</p>
 *
 * <p>本合同锁两件事：①门的纯判定真值表（第三豁免＝寻路腿活跃）；②接线——成员静默且腿活跃时，
 * 门必须真的不 clearSharedCycleFrame() 返回，且位置条带每拍前进一帧。</p>
 */
class MemberReturnLegSharedFrameContractTest {

    private static final String WINDOW = "hwnd-E70";
    private static final String HWND = "46796494";
    private static final String TASK_RUN = "remote-turn-e70-contract";

    // ---- ① 纯判定真值表 ----

    @Test
    void onlyAQuietMemberWithNoConsumerLosesTheSharedFrame() {
        // 非成员静默（队长/单开/角色未定，或云端重发了战斗兴趣）：任何组合都不断帧。
        assertFalse(WindowObservationSampler.isQuietMemberFrameSuppressed(false, false, false));
        assertFalse(WindowObservationSampler.isQuietMemberFrameSuppressed(false, false, true));
        assertFalse(WindowObservationSampler.isQuietMemberFrameSuppressed(false, true, false));
        assertFalse(WindowObservationSampler.isQuietMemberFrameSuppressed(false, true, true));
        // 成员静默且无任何消费者：这才是 G002 想省下的整窗 PrintWindow。
        assertTrue(WindowObservationSampler.isQuietMemberFrameSuppressed(true, false, false),
                "quiet member with no consumer must still skip the whole-window redraw (G002)");
    }

    @Test
    void aLivePathingLegIsTheThirdExemption() {
        // E70 正身：成员静默、面板看守不要帧，但腿活着 —— 必须给帧。
        assertFalse(WindowObservationSampler.isQuietMemberFrameSuppressed(true, false, true),
                "a live team-return/pathing leg must exempt a quiet member from the frame gate (E70)");
        // 既有两个豁免不因新豁免而失效。
        assertFalse(WindowObservationSampler.isQuietMemberFrameSuppressed(true, true, false),
                "auto-panel watch exemption must survive");
        assertFalse(WindowObservationSampler.isQuietMemberFrameSuppressed(true, true, true));
    }

    // ---- ② 接线：真门 + 真位置条带 ----

    @Test
    void quietMemberWithLiveReturnLegKeepsRefreshingThePositionStrip() throws Exception {
        AtomicInteger fullWindowCaptures = new AtomicInteger();
        WindowRuntimeContext context = memberContext();
        WindowObservationSampler sampler = sampler(context, fullWindowCaptures);
        silenceCombatInterest(sampler);

        // 归队腿正身：云端动作完成后 markPathingStarted 注册的那条 UNTARGETED_TRACKER 腿。
        context.markPathingStarted(WindowPathingIntent.builder()
                .source("team-return:auto-battle:local-team-return-release")
                .type(WindowPathingIntentType.UNTARGETED_TRACKER)
                .build());
        assertTrue(sampler.hasActivePathingIntent(), "harness precondition: the leg must read as live");

        sampler.refreshSharedFramesIfNeeded(1_000L);
        assertTrue(sampler.sharedCycleFrameCapturedAtMs() > 0L,
                "the gate must not clear the shared frame while a return leg is live");
        assertEquals(1, fullWindowCaptures.get(), "exactly one whole-window capture per tick");
        BufferedImage firstStrip = positionStrip(sampler);
        assertNotNull(firstStrip, "position strip must be cropped for the live leg");

        sampler.refreshSharedFramesIfNeeded(1_300L);
        BufferedImage secondStrip = positionStrip(sampler);
        assertNotNull(secondStrip);
        // 位移判定就是拿前后两帧比 —— 条带必须真的前进，不能是同一张。
        assertNotSame(firstStrip, secondStrip,
                "the position strip must advance every tick, otherwise movement is compared against itself");
        assertSame(firstStrip, previousPositionStrip(sampler),
                "the previous strip must be the earlier frame, forming a real before/after pair");
        assertEquals(2, fullWindowCaptures.get());
    }

    @Test
    void quietMemberWithoutAnyLegStillLosesTheFrame() throws Exception {
        AtomicInteger fullWindowCaptures = new AtomicInteger();
        WindowRuntimeContext context = memberContext();
        WindowObservationSampler sampler = sampler(context, fullWindowCaptures);
        silenceCombatInterest(sampler);
        assertFalse(sampler.hasActivePathingIntent(), "harness precondition: no leg registered");

        sampler.refreshSharedFramesIfNeeded(1_000L);
        sampler.refreshSharedFramesIfNeeded(1_300L);

        assertEquals(0, fullWindowCaptures.get(),
                "G002 must still hold: a quiet member with no consumer pays no PrintWindow redraw");
        assertEquals(0L, sampler.sharedCycleFrameCapturedAtMs());
        assertNull(positionStrip(sampler), "no leg -> no strip refresh");
    }

    @Test
    void anEndedLegReturnsTheMemberToQuiet() throws Exception {
        AtomicInteger fullWindowCaptures = new AtomicInteger();
        WindowRuntimeContext context = memberContext();
        WindowObservationSampler sampler = sampler(context, fullWindowCaptures);
        silenceCombatInterest(sampler);

        context.markPathingStarted(WindowPathingIntent.builder()
                .source("team-return:auto-battle:local-team-return-release")
                .type(WindowPathingIntentType.UNTARGETED_TRACKER)
                .build());
        sampler.refreshSharedFramesIfNeeded(1_000L);
        assertEquals(1, fullWindowCaptures.get());

        // 腿结束（到达/撤销）后豁免必须立刻收回 —— 豁免不得变成永久开销。
        context.markPathingStarted(null);
        assertFalse(sampler.hasActivePathingIntent());
        sampler.refreshSharedFramesIfNeeded(1_300L);
        assertEquals(1, fullWindowCaptures.get(),
                "the exemption must end with the leg, never leak into steady-state member quiet");
        assertEquals(0L, sampler.sharedCycleFrameCapturedAtMs());
    }

    // ---- harness ----

    private static WindowRuntimeContext memberContext() {
        WindowRuntimeContext context = new WindowRuntimeContext(WINDOW, new GameContext());
        context.setNativeBinding(new WindowNativeBinding(HWND, "title", "class", 7L, 0, 0, 1024, 768));
        context.setRole(WindowRole.MEMBER);
        return context;
    }

    /** 云端压制了 combat-signal 兴趣的成员窗（collectBound 里由空兴趣集推出的同一状态）。 */
    private static void silenceCombatInterest(WindowObservationSampler sampler) throws Exception {
        Field field = WindowObservationSampler.class.getDeclaredField("combatSignalInterestActive");
        field.setAccessible(true);
        field.setBoolean(sampler, false);
    }

    private static BufferedImage positionStrip(WindowObservationSampler sampler) throws Exception {
        return readFrame(sampler, "sharedPositionStripFrame");
    }

    private static BufferedImage previousPositionStrip(WindowObservationSampler sampler) throws Exception {
        return readFrame(sampler, "previousSharedPositionStripFrame");
    }

    private static BufferedImage readFrame(WindowObservationSampler sampler, String name) throws Exception {
        Field field = WindowObservationSampler.class.getDeclaredField(name);
        field.setAccessible(true);
        return (BufferedImage) field.get(sampler);
    }

    private static WindowObservationSampler sampler(WindowRuntimeContext context,
                                                    AtomicInteger fullWindowCaptures) {
        WindowTaskContextHolder holder = new WindowTaskContextHolder(new WindowIsolationProperties());
        GameClientTracker tracker = new GameClientTracker(
                null, null, null, null, null, null, null, null, null, null, null) {
            @Override
            public boolean refreshWindowState() {
                return true;
            }

            @Override
            public int getWindowBaseX() {
                return 0;
            }

            @Override
            public int getWindowBaseY() {
                return 0;
            }

            @Override
            public BufferedImage captureToMemory(String elementName, int x1, int y1, int x2, int y2) {
                if (x2 - x1 >= 1024) {
                    fullWindowCaptures.incrementAndGet();
                }
                return new BufferedImage(Math.max(1, x2 - x1), Math.max(1, y2 - y1),
                        BufferedImage.TYPE_INT_RGB);
            }
        };
        CoordinateHelper coordinateHelper = new CoordinateHelper(tracker, null);
        return new WindowObservationSampler(
                context,
                holder,
                tracker,
                coordinateHelper,
                new DialogService(tracker, coordinateHelper),
                new InputSequences(null),
                TASK_RUN,
                false);
    }
}
