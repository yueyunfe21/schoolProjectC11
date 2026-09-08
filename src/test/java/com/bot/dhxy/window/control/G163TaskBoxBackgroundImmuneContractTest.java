package com.bot.dhxy.window.control;

import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * G163（2026-09-06 用户定性"原因是背景被带入到了匹配中"，天庭暗雷巡游自杀事故）。
 *
 * <p>任务追踪框背景半透明，游戏世界照穿过来；raw 比对把整块 RGB（含背景）算相关系数，
 * 角色一走动背景就滚 → 判 CHANGED＝"任务推进" → 掐断暗雷巡游腿，改走普通绿链空转，
 * 日常完成数同步虚增 24→28。三条修：①任务框类判定全改亮度掩膜比对（背景免疫）
 * ②全仓同族调用点一并切换 ③同一任务框不得重复计数。</p>
 */
class G163TaskBoxBackgroundImmuneContractTest {

    private static final Path COMPARATOR = Path.of(
            "../dhxy-cloud-brain/src/main/java/com/bot/dhxy/service/TrackerTaskBoxContentComparator.java");
    private static final Path TIANTING_TASK = Path.of(
            "../dhxy-cloud-brain/src/main/java/com/bot/dhxy/task/tianting/TiantingTask.java");
    private static final Path ROUND_CONTEXT = Path.of(
            "../dhxy-cloud-brain/src/main/java/com/bot/dhxy/task/tianting/TiantingRoundContext.java");
    private static final Path OBSERVER = Path.of(
            "../dhxy-cloud-brain/src/main/java/com/yueyunfe/dhxy/cloudbrain/turn/runtime/CloudWholeTaskObserver.java");
    private static final Path PREPARED_STATE = Path.of(
            "../dhxy-cloud-brain/src/main/java/com/bot/dhxy/service/dialog/CloudDialogPreparedActionState.java");
    private static final Path EVIDENCE_DIR = Path.of("images/temp/match-evidence/taskbox-compare");

    @Test
    void maskIsBrightnessBasedNotHueBased() throws Exception {
        String source = Files.readString(COMPARATOR, StandardCharsets.UTF_8);
        assertTrue(source.contains("TEXT_MASK_MIN_BRIGHTNESS = 190"),
                "掩膜必须按亮度——色相判据 isTrackerTextPixel 对透出的世界像素无效（首版实测失败）");
        assertTrue(source.contains("TEXT_MASK_CHANGED_RATIO = 0.05d"),
                "阈值 0.05 落在语料空带(0 与 0.294)正中；改动须重跑 1064 张语料回放");
        int mask = source.indexOf("private static BitSet textMask(RawImage image)");
        assertTrue(mask > 0, "找不到掩膜构造；改了结构就更新本合同");
        String body = source.substring(mask, Math.min(source.length(), mask + 900));
        assertFalse(body.contains("isTrackerTextPixel"),
                "掩膜不许退回色相判据——那正是首版无效的原因");
        assertTrue(body.contains("TEXT_MASK_MIN_BRIGHTNESS"), "掩膜必须用亮度门");
    }

    @Test
    void everyTaskBoxJudgementUsesTheMaskedEntry() throws Exception {
        // 任务框类判定（六处天庭 + 两处观察者 + 一处 prepared）必须全部走掩膜入口。
        for (Path p : List.of(TIANTING_TASK, ROUND_CONTEXT, OBSERVER, PREPARED_STATE)) {
            String source = Files.readString(p, StandardCharsets.UTF_8);
            assertFalse(source.contains("compareRawEvidenced("),
                    "任务框判定不许再走带背景的 raw 比对: " + p.getFileName());
            assertTrue(source.contains("compareTextMaskedEvidenced("),
                    "必须走掩膜入口: " + p.getFileName());
        }
    }

    @Test
    void sameTaskBoxCannotBeCountedTwice() throws Exception {
        String source = Files.readString(TIANTING_TASK, StandardCharsets.UTF_8);
        assertTrue(source.contains("lastCountedSubtaskDigest"),
                "必须记住上次计数的任务框——事故里 count=27/28 落在同一哈希连计两次");
        assertTrue(source.contains("G163 天庭 subtask completion ignored (same task box already counted)"),
                "同框重复计数必须被拦下并留日志");
        int guard = source.indexOf("if (completedDigest.equals(lastCountedSubtaskDigest)) {");
        int count = source.indexOf("int cumulativeCompleted = progress.recordRoundCompleted();", guard);
        assertTrue(guard > 0 && count > guard, "去重闸必须在真正计数之前");
    }

    /**
     * 语料回放：把落盘的并排合成图（左|4px|右）拆开重跑判据。
     * 断言=旧 raw 判 UNCHANGED 的一律不许被翻成 CHANGED（零误翻），
     * 且事故当日那批纯背景差异必须判 UNCHANGED。
     */
    @Test
    void corpusReplayKeepsBackgroundOnlyDifferencesUnchanged() throws Exception {
        File dir = EVIDENCE_DIR.toFile();
        File[] files = dir.listFiles((d, n) -> n.endsWith(".png") && !n.startsWith("dialog-fingerprint-raw"));
        if (files == null || files.length == 0) {
            return; // 语料不在此机器上时跳过；源码锚仍然守门。
        }
        Class<?> cmp = Class.forName("com.bot.dhxy.service.TrackerTaskBoxContentComparator");
        var rawFp = cmp.getMethod("rawFingerprint", BufferedImage.class);
        var masked = cmp.getMethod("compareTextMasked", String.class, String.class);
        var changeOf = Class.forName("com.bot.dhxy.service.TrackerTaskBoxContentComparator$RawComparison")
                .getMethod("change");
        List<String> flipped = new ArrayList<>();
        int checked = 0;
        for (File f : files) {
            BufferedImage both = ImageIO.read(f);
            if (both == null || both.getWidth() < 8) {
                continue;
            }
            int w = (both.getWidth() - 4) / 2;
            int h = both.getHeight();
            String left = (String) rawFp.invoke(null, both.getSubimage(0, 0, w, h));
            String right = (String) rawFp.invoke(null, both.getSubimage(w + 4, 0, w, h));
            String verdict = String.valueOf(changeOf.invoke(masked.invoke(null, left, right)));
            checked++;
            if (f.getName().contains("_UNCHANGED_") && "CHANGED".equals(verdict)) {
                flipped.add(f.getName());
            }
        }
        assertTrue(checked > 100, "语料量过小，回放无意义: " + checked);
        assertEquals(List.of(), flipped,
                "旧判 UNCHANGED 的绝不许被新判据翻成 CHANGED（零误翻是本次修复的硬边界）");
    }
}
