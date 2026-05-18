package com.bot.dhxy.service;

import com.bot.dhxy.input.InputProvider;
import com.bot.dhxy.core.GameClientTracker;
import com.bot.dhxy.core.GameContext;
import com.bot.dhxy.core.ImageFinder;
import com.bot.dhxy.core.TextRecognizer;
import com.bot.dhxy.model.QuestTargetInfo;
import com.bot.dhxy.tools.CoordinateHelper;
import com.bot.dhxy.tools.ImagePreprocessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.awt.Point;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 🧠 任务情报总管 (Task Manager)
 * 修复版：已修复洗图方法的类型不匹配 (String vs BufferedImage) 以及所有红线问题！
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class QuestManagerService {

    private final InputProvider inputProvider;
    private final GameClientTracker tracker;
    private final CoordinateHelper coordinateHelper;
    private final TextRecognizer ocr;
    private final GameContext context;

    // ==========================================
    // 📐 第一部分：面板测绘与 UI 常量
    // ==========================================
    private static final String ANCHOR_PATH = "images/template/task_fenxiang.png";

    private static final int OFFSET_X = -497;
    private static final int OFFSET_Y = 8;
    private static final int PANEL_H = 295;
    private static final int W_LEFT = 223;
    private static final int W_RIGHT = 290;
    private static final int OFFSET_X_RIGHT = OFFSET_X + W_LEFT;

    private static final int P1_X = -209;
    private static final int P1_Y = 37;

    private static final double THRESHOLD_STRICT = 0.85;
    private static final double THRESHOLD_NORMAL = 0.80;

    // ==========================================
    // ⚙️ 第二部分：发光探测与时间常量
    // ==========================================
    private static final int GLOW_RGB_MIN = 220;
    private static final int GLOW_TARGET = 15;

    private static final long SLOW = 800;
    private static final long MID = 500;
    private static final long FAST = 200;

    private static final Pattern QUEST_PATTERN = Pattern.compile("([^\\(]+).*?在\\s*([\\u4e00-\\u9fa5]+)\\s*\\((\\d+)\\s*,\\s*(\\d+)\\)");

    private static final String[] MONSTERS = {
            "images/template/wuhuan/p2_guanpian.png",
            "images/template/wuhuan/p2_daohaozei.png",
            "images/template/wuhuan/p2_wuchi.png",
            "images/template/wuhuan/p2_shiyinggui.png",
            "images/template/wuhuan/p2_xie.png",
    };

    public enum PathingResult { SUCCESS, FINISHED, UI_ERROR }

    // =========================================================================================
    // 🚀 核心逻辑：五环极速制导
    // =========================================================================================

    public PathingResult triggerWuHuanNativePathingP1() { return triggerWuHuanNativePathingP1(false); }

    public PathingResult triggerWuHuanNativePathingP1(boolean skipScan) {
        if (!skipScan && !activateTaskIfPresent("wuhuan", true)) return PathingResult.FINISHED;

        Point anchor = ensurePanel();
        if (anchor == null) return PathingResult.UI_ERROR;

        click(anchor.x + P1_X, anchor.y + P1_Y, 30, 8, MID);
        inputProvider.pressAltQ();
        return PathingResult.SUCCESS;
    }

    public PathingResult triggerWuHuanNativePathingP2() { return triggerWuHuanNativePathingP2(false); }

    public PathingResult triggerWuHuanNativePathingP2(boolean skipScan) {
        if (!skipScan && !activateTaskIfPresent("wuhuan", true)) return PathingResult.UI_ERROR;

        Point anchor = ensurePanel();
        if (anchor == null) return PathingResult.UI_ERROR;

        int[] rect = coordinateHelper.getAbsoluteRectByAnchor(anchor, OFFSET_X_RIGHT, OFFSET_Y, W_RIGHT, PANEL_H);

        // 🌟 已修复红线死穴：恢复为生成本地文件，因为洗图工具需要读取硬盘文件路径！
        String rawPath = "images/temp/p2_raw.png";
        if (!tracker.captureToFile("P2右侧", rawPath, rect[0], rect[1], rect[2], rect[3])) {
            inputProvider.pressAltQ();
            return PathingResult.UI_ERROR;
        }

        String washedPath = "images/temp/p2_washed.png";
        ImagePreprocessor.washGreenTextToBlackAndWhite(rawPath, washedPath);

        for (String m : MONSTERS) {
            double[] res = ImageFinder.find(washedPath, m, THRESHOLD_NORMAL);
            if (res != null && res.length >= 2) {
                log.info("P2识图已经匹配成功");
                click(rect[0] + (int)res[0], rect[1] + (int)res[1], 8, 4, MID);
                inputProvider.pressAltQ();
                return PathingResult.SUCCESS;
            }
        }
        log.info("P2识图匹配失败");
        return PathingResult.UI_ERROR;
    }

    // =========================================================================================
    // 🕵️ 雷达引擎：发光文字探测
    // =========================================================================================

    public boolean activateTaskIfPresent(String task) { return activateTaskIfPresent(task, false); }

    public boolean activateTaskIfPresent(String task, boolean keepOpen) {
        Point anchor = ensurePanel();
        if (anchor == null) return false;

        int[] rect = coordinateHelper.getAbsoluteRectByAnchor(anchor, OFFSET_X, OFFSET_Y, W_LEFT, PANEL_H);
        String taskImg = "images/template/task/" + task + ".png";
        String titleImg = "images/template/task/" + task + "_title.png";

        for (int p = 0; p < 2; p++) {
            Point taskPt = coordinateHelper.findImageInRegion(taskImg, rect, THRESHOLD_STRICT);
            if (taskPt != null) {
                if (isTextGlowing(taskPt)) {
                    log.info("✅ 任务 [{}] 已高亮", task);
                } else {
                    log.info("🖱️ 激活任务 [{}]", task);
                    click(taskPt.x, taskPt.y, 20, 5, MID);
                }
                if (!keepOpen) inputProvider.pressAltQ();
                return true;
            }

            Point titlePt = coordinateHelper.findImageInRegion(titleImg, rect, THRESHOLD_STRICT);
            if (titlePt != null) {
                click(titlePt.x + 30, titlePt.y + 5, 20, 5, SLOW);
                scroll(anchor, 2);
                p--; continue;
            }

            if (p < 1) scroll(anchor, 3);
        }

        inputProvider.pressAltQ();
        return false;
    }

    // =========================================================================================
    // 📖 深度解析：OCR 坐标情报
    // =========================================================================================

    public QuestTargetInfo fetchCurrentQuestInfo(String expectedTaskImage) {
        log.info("📡 [深度解析] 启动重型 OCR 引擎获取任务情报...");

        inputProvider.pressAltQ();
        sleep(SLOW);
        Point anchor = ensurePanel();
        if (anchor == null) return null;

        int[] leftRect = coordinateHelper.getAbsoluteRectByAnchor(anchor, OFFSET_X, OFFSET_Y, W_LEFT, PANEL_H);
        String taskTemplatePath = "images/template/" + expectedTaskImage;
        Point taskLabelPoint = coordinateHelper.findImageAbsoluteCoordinate(taskTemplatePath, THRESHOLD_STRICT);

        if (taskLabelPoint == null) {
            log.warn("⏭️ 未发现任务标签 [{}]，判定可能已完成", expectedTaskImage);
            inputProvider.pressAltQ();
            return null;
        }

        click(taskLabelPoint.x, taskLabelPoint.y, 20, 5, MID);

        int[] rightRect = coordinateHelper.getAbsoluteRectByAnchor(anchor, OFFSET_X_RIGHT, OFFSET_Y, W_RIGHT, PANEL_H);
        String detailPath = "images/temp/quest_detail_scan.png";

        tracker.captureToFile("任务详情", detailPath, rightRect[0], rightRect[1], rightRect[2], rightRect[3]);

        List<TextRecognizer.OcrWordResult> results = ocr.getAllTextResults(detailPath);
        if (results == null || results.isEmpty()) {
            inputProvider.pressAltQ();
            return null;
        }

        StringBuilder fullText = new StringBuilder();
        for (TextRecognizer.OcrWordResult word : results) {
            fullText.append(word.getText());
        }

        String cleanText = fullText.toString();
        log.info("🔍 OCR 结果: {}", cleanText);

        Matcher matcher = QUEST_PATTERN.matcher(cleanText);
        if (matcher.find()) {
            String npc = matcher.group(1).trim();
            String map = matcher.group(2).trim();
            int x = Integer.parseInt(matcher.group(3));
            int y = Integer.parseInt(matcher.group(4));

            log.info("✅ 情报解锁 -> NPC: [{}], 地图: [{}], 坐标: ({}, {})", npc, map, x, y);
            context.setCurrentTaskName(npc);

            inputProvider.pressAltQ();
            return new QuestTargetInfo(npc, map, x, y, cleanText);
        }

        inputProvider.pressAltQ();
        return null;
    }

    // =========================================================================================
    // 🧰 私有辅助工具
    // =========================================================================================

    private Point ensurePanel() {
        Point a = findAnchor();
        if (a == null) {
            inputProvider.pressAltQ();
            sleep(SLOW);
            a = findAnchor();
        }
        return a;
    }

    private boolean isTextGlowing(Point pt) {
        BufferedImage img = tracker.captureToMemory("probe", pt.x - 40, pt.y - 10, pt.x + 40, pt.y + 10);
        if (img == null) return false;
        int count = 0;
        for (int x = 0; x < img.getWidth(); x++) {
            for (int y = 0; y < img.getHeight(); y++) {
                int c = img.getRGB(x, y);
                if (((c>>16)&0xFF)>GLOW_RGB_MIN && ((c>>8)&0xFF)>GLOW_RGB_MIN && (c&0xFF)>GLOW_RGB_MIN) count++;
            }
        }
        img.flush();
        return count > GLOW_TARGET;
    }

    private void click(int x, int y, int rx, int ry, long delay) {
        Point p = coordinateHelper.getRandomizedPoint(x, y, rx, ry);
        inputProvider.clickLeft(p.x, p.y, 100);
        sleep(delay);
    }

    private void scroll(Point a, int steps) {
        Point h = coordinateHelper.getRandomizedPoint(a.x - 400, a.y + 174, 50, 100);
        inputProvider.moveMouse(h.x, h.y);
        sleep(FAST);
        inputProvider.scrollDown(steps);
        sleep(MID);
    }

    private Point findAnchor() { return coordinateHelper.findImageAbsoluteCoordinate(ANCHOR_PATH, THRESHOLD_NORMAL); }

    private void sleep(long ms) { try { Thread.sleep(ms); } catch (Exception ignored) {} }
}
