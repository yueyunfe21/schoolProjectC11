package com.bot.dhxy.window.observation;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * G130 停稳判定全任务统一为数值判稳（用户裁定 2026-08-31：判"停没停"必须读坐标数字，不能比图片）。
 *
 * <p>事故：2026-08-31 14:17，hwnd-BD0D1E 归队传送到长寿村后站着不动三分钟。判定原图铁证：
 * 两帧坐标条都写 55,22（一个数字没变），但背景飘过一团红色使 34.9% 像素超过 5% 差值阈值，
 * 750 次判定反复判"在动"，每次都把 2.2 秒停稳计时清零——STOPPED_AWAY 永远触发不了，
 * 队长归队门等满 180 秒被迫停任务。根因=08-22 数值判稳重设计只在五环落地（"五环首批"），
 * 其余任务一直走像素差值老路。</p>
 */
class G130ValueStabilityAllTasksContractTest {

    // ---------- 门①：数值判稳对全部任务无条件生效 ----------

    @Test
    void valueStabilityModeIsUnconditionallyOnForEveryTask() throws Exception {
        String body = modeMethodBody();
        assertTrue(body.contains("return true;"),
                "isValueStabilityMode 必须恒真——停稳判定全任务读数字：" + body);
        assertFalse(body.contains("TaskType"),
                "不许再按任务类型开关判稳方式（分批迁移已收口，禁止回退出任务白名单）：" + body);
        assertFalse(body.contains("getSelectedTaskType"),
                "同上——任务类型不得参与停稳判定方式的选择：" + body);
    }

    // ---------- 门②：像素差值老路必须仍被模式门锁死（防悄悄复活） ----------

    @Test
    void everyLegacyPixelPathStaysBehindTheModeGate() throws Exception {
        String source = samplerSource();
        // 终局判定：数值分支必须在像素 crop 之前短路
        int terminalAt = source.indexOf("if (isValueStabilityMode()) {\n            refreshValueStability(intent, observedAtMs);\n            return;\n        }");
        assertTrue(terminalAt > 0, "refreshLocalPathingTerminal 的数值短路门丢失");
        int cropAt = source.indexOf("cropMovementDigits(sharedPositionStripFrame)", terminalAt);
        assertTrue(cropAt > terminalAt, "像素差值 crop 必须位于数值短路门之后（死路）");

        // 到达帧采集：数值分支短路
        assertTrue(source.contains("if (isValueStabilityMode()) {\n            sampleValueStableTerminalFrame(pathingFacts, terminalFrames);\n            return;\n        }"),
                "sampleTerminalCoordinateFrame 的数值短路门丢失");

        // 走路基线预热（问云端 OCR 的旁路）：数值模式下整段不走
        int baselineAt = source.indexOf("private void sampleWalkingBaselineCoordinateRoi");
        assertTrue(baselineAt > 0);
        String baselineHead = source.substring(baselineAt, baselineAt + 400);
        assertTrue(baselineHead.contains("isValueStabilityMode()"),
                "走路基线预热必须被模式门挡死（数值判稳不走云端 OCR 问答）");
    }

    // ---------- 门③：数值核心的两条铁律不得丢失 ----------

    @Test
    void theValueCoreKeepsItsIronRules() throws Exception {
        String source = samplerSource();
        // 铁律一（08-23 事故）：撤稳只认有效变值——第三态(不可读)不撤销停稳
        assertTrue(source.contains("stable claim held through unreadable strip (third state)"),
                "第三态不撤稳的铁律丢失（08-23 21:19 事故：断供撤稳+清基线→假 ACTIVE 作废点击）");
        // 铁律二（评审 P1）：陈帧不作证——超过新鲜窗的条帧按不可读处理
        assertTrue(source.contains("observedAtMs - sharedPositionStripCapturedAtMs <= VALUE_STRIP_FRESH_MS"),
                "条帧新鲜度门丢失（陈帧读出'值没变'不是停稳证据）");
    }

    // ---------- 门④：云端翻译层对无目标腿(归队)的停稳映射存在 ----------

    @Test
    void cloudTranslationMapsUntargetedStableToStoppedAway() throws Exception {
        Path cloud = Path.of("..", "dhxy-cloud-brain", "src", "main", "java",
                "com", "yueyunfe", "dhxy", "cloudbrain", "observation", "CloudObservationHttpHandler.java");
        assertTrue(Files.exists(cloud), "找不到云端翻译层：" + cloud.toAbsolutePath());
        String source = Files.readString(cloud, StandardCharsets.UTF_8);
        int at = source.indexOf("private ObservationPathingFact translateStabilityFact(");
        assertTrue(at > 0, "云端停稳翻译层丢失");
        String body = source.substring(at, Math.min(source.length(), at + 3000));
        assertTrue(body.contains("ObservationPathingType.UNTARGETED_TRACKER"),
                "无目标腿必须有专门映射——归队腿正是 UNTARGETED_TRACKER");
        assertTrue(body.contains("ObservationPathingState.STOPPED_AWAY"),
                "无目标腿数值停稳必须映射为 STOPPED_AWAY（归队后站住=停下了，Runner 不得沉默）");
    }

    private static String samplerSource() throws Exception {
        Path path = Path.of("src", "main", "java", "com", "bot", "dhxy",
                "window", "observation", "WindowObservationSampler.java");
        assertTrue(Files.exists(path), "找不到生产源码：" + path.toAbsolutePath());
        return Files.readString(path, StandardCharsets.UTF_8);
    }

    private static String modeMethodBody() throws Exception {
        String source = samplerSource();
        int at = source.indexOf("private boolean isValueStabilityMode()");
        assertTrue(at > 0, "isValueStabilityMode 不存在");
        int end = source.indexOf("\n    }", at);
        return source.substring(at, end);
    }
}
