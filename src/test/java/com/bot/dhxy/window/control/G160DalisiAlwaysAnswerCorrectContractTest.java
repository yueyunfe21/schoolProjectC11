package com.bot.dhxy.window.control;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * G160（2026-09-06 用户裁定，撤销 G147 的 3% 故意答错）：大理寺答题**已知答案必须一律答对**。
 *
 * <p>业务铁律：本任务的奖励判据是全部答对，答错直接损失奖励。G147 采信了反检测评审
 * "100% 正确率单独即签名"的通用假设，与本任务领域事实直接冲突——用户领域判据优先于
 * 工程/评审推断，人化只能加在不损失收益的面上（读题延迟、首见长考、悬停不归位等）。</p>
 *
 * <p>本合同是防复发闸：任何以"人化/降低正确率/模拟失误"为名重新引入故意答错的改动都必须变红。</p>
 */
class G160DalisiAlwaysAnswerCorrectContractTest {

    private static final Path DALISI = Path.of(
            "../dhxy-cloud-brain/src/main/java/com/bot/dhxy/task/dalisi/DalisiQuizTask.java");

    @Test
    void noDeliberateWrongAnswerPathExists() throws Exception {
        String source = Files.readString(DALISI, StandardCharsets.UTF_8);
        assertFalse(source.contains("deliberateWrongClick("),
                "严禁故意答错：本任务全部答对才有奖励（G160 用户裁定）");
        assertFalse(source.contains("DELIBERATE_WRONG_RATE"),
                "故意答错的概率常量必须不存在——留着就会被误当可开启开关接回去");
        assertFalse(source.contains("deliberate WRONG clicked"),
                "故意答错的日志文案必须不存在");
    }

    @Test
    void knownAnswerPathHasNoProbabilisticBranchAtAll() throws Exception {
        String source = Files.readString(DALISI, StandardCharsets.UTF_8);
        /*
         * 检查窗口必须覆盖**整个已知答案分支**（readAnswer 起，到答对点击止），
         * 不能只卡"读题延迟→点击"那一小段：2026-09-06 变异自证实测，把概率分支插在读题延迟
         * 之上就能整段绕开窄窗口检查，合同假绿。窗口取宽是这条合同的关键。
         */
        int blockStart = source.indexOf("BufferedImage answer = library.readAnswer(questionNumber);");
        assertTrue(blockStart > 0, "找不到已知答案读取；改了结构就更新本合同");
        int click = source.indexOf("if (!clickRoiPoint(hit[0], hit[1])) {", blockStart);
        assertTrue(click > 0, "找不到已知答案点击；改了结构就更新本合同");
        String knownAnswerPath = source.substring(blockStart, click);
        assertFalse(knownAnswerPath.contains("nextDouble()"),
                "已知答案分支内严禁任何概率取值——那是故意答错的复发形态");
        assertFalse(knownAnswerPath.contains("nextInt("),
                "已知答案分支内严禁任何随机选行——同上");
        assertFalse(knownAnswerPath.contains("ThreadLocalRandom"),
                "已知答案分支内严禁引入随机源：答对是确定性行为");
    }

    @Test
    void harmlessHumanizationIsKept() throws Exception {
        String source = Files.readString(DALISI, StandardCharsets.UTF_8);
        // 不损失收益的人化必须保留：撤销故意答错不等于把答题打回机读节拍。
        assertTrue(source.contains("sleepReadingDelay("), "读题延迟（随题宽正相关）必须保留");
        assertTrue(source.contains("FIRST_SIGHT_EXTRA_MIN_MS"), "首见新题长考必须保留");
        assertTrue(source.contains("ANSWER_SETTLE_MIN_MS = 3_000L"), "用户 08-26 的 3 秒 settle 底线必须保留");
    }
}
