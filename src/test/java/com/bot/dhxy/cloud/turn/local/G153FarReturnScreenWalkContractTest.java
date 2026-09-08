package com.bot.dhxy.cloud.turn.local;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * G153（2026-09-04 用户设计）：修罗回程第五落点 (132,92) 改一次屏幕右键走位，
 * 替代"开世界地图搜索→点小地图"的反人类短距导航。
 */
class G153FarReturnScreenWalkContractTest {

    private static final Path XIULUO = Path.of(
            "../dhxy-cloud-brain/src/main/java/com/bot/dhxy/task/xiuluo/XiuluoTaskV2.java");
    private static final Path NAVIGATION = Path.of(
            "../dhxy-cloud-brain/src/main/java/com/bot/dhxy/service/NavigationService.java");

    @Test
    void farLandingWalkIsWiredWithUserApprovedShape() throws Exception {
        String source = Files.readString(XIULUO, StandardCharsets.UTF_8);
        assertTrue(source.contains("FAR_RETURN_LANDING_X = 132")
                        && source.contains("FAR_RETURN_LANDING_Y = 92"),
                "触发门必须钉在 443 次统计实证的第五落点 (132,92)");
        assertTrue(source.contains("walkOffFarReturnLanding(context, result.location(), source"),
                "回城验证成功后必须先过落点走位判定");
        assertTrue(source.contains("navigateByScreenRightClickWalk("),
                "走位必须走 NavigationService 的意图搭车右键路，不许自造 TurnStep");
        // 用户纠偏：A 点已在 NPC 行位偏下，Y 只许向上抖——源码里 Y 必须是减随机、无 +Y 抖动。
        assertTrue(source.contains("FAR_RETURN_WALK_BASE_Y - random.nextInt(FAR_RETURN_WALK_JITTER_Y_UP + 1)"),
                "Y 抖动必须只向上（向下=背着 NPC 走）");
        // 用户裁定：判停沿用现有链（意图+本地 runner 终局），禁固定睡眠。
        int walkStart = source.indexOf("private void walkOffFarReturnLanding");
        int walkEnd = source.indexOf("private ReturnHomeResult useReturnItemAndVerifyStartMap", walkStart);
        String walkBody = source.substring(walkStart, walkEnd);
        assertFalse(walkBody.contains("TaskSleep.sleep"),
                "走位后禁止固定睡眠——判停必须靠本地 runner 的 ARRIVED/STOPPED_AWAY 终局");
        assertTrue(source.contains("G153 far-return screen walk failed (non-fatal"),
                "走位失败必须非致命——世界地图导航路保持兜底");
    }

    @Test
    void screenWalkReusesTheStandardPathingStopChain() throws Exception {
        String source = Files.readString(NAVIGATION, StandardCharsets.UTF_8);
        int method = source.indexOf("public NavigationResult navigateByScreenRightClickWalk");
        assertTrue(method > 0, "NavigationService 必须提供意图搭车的右键走位");
        String body = source.substring(method, source.indexOf("private boolean executeInputTurn", method));
        assertTrue(body.contains("buildTurnPathingIntent("),
                "意图必须随点击下发（Local Pathing Fact Bridge 同款）");
        assertTrue(body.contains("awaitNewerPathingTerminalOrPreparedRoute("),
                "判停必须等本地 runner 终局——与小地图路同一套，零新机制");
        assertTrue(body.contains("rightClickStep(") && body.contains("TurnInputCoordinateSpace.WINDOW_RELATIVE"),
                "物理动作=右键+窗口相对坐标");
        assertTrue(body.contains("WindowPathingState.ARRIVED") && body.contains("WindowPathingState.STOPPED_AWAY"),
                "终局两态必须分别映射（到达/停偏）");
        assertFalse(body.contains("TaskSleep.sleep"),
                "本方法内禁止固定睡眠");
    }
}
