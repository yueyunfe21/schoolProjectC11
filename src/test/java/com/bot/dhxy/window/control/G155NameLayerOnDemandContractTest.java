package com.bot.dhxy.window.control;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * G155（2026-09-05 用户裁定"直接点击/记忆点/tooltip 本来都不需要 Alt+4，只有洗黄字那里的截图需要"）：
 * 名字层准备改为按需触发。取证：今日 199 个 FIFO 候选里 MEMORY/TOOLTIP/FIXED_POINT 占 191（96%），
 * YELLOW_NAME 仅 6（3%），而 Alt+4 按了 152 次、云端流水线名字层与黄字扫描各 0 次。
 * 不变量保持：黄名判定永远只在备好名字层的帧上进行（改由替换帧准备，走既有 REQUEST_NEW_SCREENSHOT 闭环）。
 */
class G155NameLayerOnDemandContractTest {

    private static final Path FIFO_EXECUTOR = Path.of(
            "src/main/java/com/bot/dhxy/cloud/turn/local/NpcArrivalFrameFifoLocalExecutor.java");
    private static final Path NPC_CLICK_SERVICE = Path.of(
            "../dhxy-cloud-brain/src/main/java/com/bot/dhxy/service/NpcClickService.java");
    private static final Path RECOGNIZER = Path.of(
            "../dhxy-cloud-brain/src/main/java/com/yueyunfe/dhxy/cloudbrain/SmartClickRecognizer.java");

    @Test
    void singleFramePlanNoLongerPressesAltFour() throws Exception {
        String source = Files.readString(NPC_CLICK_SERVICE, StandardCharsets.UTF_8);
        int plan = source.indexOf("private boolean runSingleFrameNpcClickPlan(");
        assertTrue(plan > 0, "找不到单帧点击计划；改了结构就更新本合同");
        String body = source.substring(plan, Math.min(source.length(), plan + 3000));
        assertFalse(body.contains("keyStep(0, \"ALT_4\")"),
                "首帧不许再无条件按 Alt+4——它服务的固定点/记忆点/tooltip 与名字层无关");
        assertTrue(body.contains("List.of(captureStep(0, fullClientRegion))"),
                "首帧必须是纯截图，不带按键步");
    }

    @Test
    void yellowStageRefusesToJudgeOnAnUnpreparedFrame() throws Exception {
        String source = Files.readString(RECOGNIZER, StandardCharsets.UTF_8);
        int yellow = source.indexOf("private static Result clickUniqueYellowTarget(");
        assertTrue(yellow > 0, "找不到黄名判定入口；改了结构就更新本合同");
        String body = source.substring(yellow, Math.min(source.length(), yellow + 1600));
        assertTrue(body.contains("attemptIndex(context) <= 0"),
                "首帧（attemptIndex=0）名字层未备，黄名不许在脏场景硬判");
        assertTrue(body.contains("cloud-brain-npc-yellow-needs-name-layer"),
                "必须走既有 REQUEST_NEW_SCREENSHOT 通道要一张备好名字层的新帧");
    }

    @Test
    void replacementFramePreparesTheNameLayer() throws Exception {
        String source = Files.readString(FIFO_EXECUTOR, StandardCharsets.UTF_8);
        int capture = source.indexOf("private byte[] captureFreshExactFrame(");
        assertTrue(capture > 0, "找不到替换帧截图；改了结构就更新本合同");
        String body = source.substring(capture, Math.min(source.length(), capture + 1200));
        assertTrue(body.contains("InputAction.pressAlt4()"),
                "替换帧＝黄名真正洗图的那一帧，名字层必须在这里备");
        assertTrue(body.contains("npcClick:replacement-hide-player-names"),
                "名字层准备必须可按来源对账");
        assertTrue(body.contains("NAME_LAYER_SETTLE_MS"),
                "Alt+4 后必须沉降再截图");
    }
}
