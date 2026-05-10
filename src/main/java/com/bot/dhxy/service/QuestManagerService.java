package com.bot.dhxy.service;

import com.bot.dhxy.config.InputProvider;
import com.bot.dhxy.core.GameClientTracker;
import com.bot.dhxy.core.GameContext;
import com.bot.dhxy.core.TextRecognizer;
import com.bot.dhxy.model.QuestTargetInfo;
import com.bot.dhxy.tools.CoordinateHelper;
import com.bot.dhxy.tools.ImagePreprocessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.awt.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 🧠 任务情报总管 (Task Manager)
 * 纯净重构版：消灭魔法数字，彻底拥抱 DRY 原则！
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
    // 📐 第一战区：面板测绘与锚点数据
    // ==========================================
    private static final String ANCHOR_TEMPLATE_PATH = "images/template/task_fenxiang.png";

    private static final int OFFSET_TO_EDGE_X = -497;
    private static final int OFFSET_TO_EDGE_Y = 8;
    private static final int DETAIL_H = 295;
    private static final int DETAIL_W_TOTAL = 513;
    private static final int DETAIL_W_RIGHT = 290;
    private static final int DETAIL_W_LEFT = DETAIL_W_TOTAL - DETAIL_W_RIGHT;
    private static final int OFFSET_TO_EDGE_X_RIGHT = OFFSET_TO_EDGE_X + DETAIL_W_LEFT;

    private static final int P1_LINK_OFFSET_X = -209;
    private static final int P1_LINK_OFFSET_Y = 37;

    private static final Pattern QUEST_PATTERN = Pattern.compile("([^\\(]+).*?在\\s*([\\u4e00-\\u9fa5]+)\\s*\\((\\d+)\\s*,\\s*(\\d+)\\)");

    private static final String[] P2_MONSTER_TEMPLATES = {
            "images/template/wuhuan/p2_guanpian.png",
            "images/template/wuhuan/p2_daohaozei.png",
            "images/template/wuhuan/p2_wuchi.png",
            "images/template/wuhuan/p2_shiyinggui.png",
            "images/template/wuhuan/p2_xie.png"
    };

    public enum PathingResult { SUCCESS, FINISHED, UI_ERROR }

    // ==========================================
    // ⚙️ 第二战区：全局调控参数 (魔法数字终结者)
    // ==========================================

    // 1. 匹配阈值
    private static final double THRESHOLD_STRICT = 0.85;
    private static final double THRESHOLD_NORMAL = 0.80;

    // 2. 拟人化延时参数 (毫秒)
    private static final long DELAY_SHORT = 200;
    private static final long DELAY_NORMAL = 500;
    private static final long DELAY_LONG = 800;

    // 3. 字体发光检测参数 (白像素探针)
    private static final int GLOW_RGB_MIN = 220;         // 发光字体RGB最低下限
    private static final int GLOW_PIXELS_TARGET = 15;    // 判定为高亮的白像素个数阈值
    private static final int PROBE_OFFSET_X = -40;       // 探针截取框相对中心的X偏移
    private static final int PROBE_OFFSET_Y = -10;       // 探针截取框相对中心的Y偏移
    private static final int PROBE_W = 80;               // 探针截取框宽度
    private static final int PROBE_H = 20;               // 探针截取框高度

    // 4. 滚轮与翻页参数
    private static final int SCROLL_HOVER_OFFSET_X = -400; // 鼠标悬停滚动区的X偏移
    private static final int SCROLL_HOVER_OFFSET_Y = 174;  // 鼠标悬停滚动区的Y偏移
    private static final int SCROLL_STEPS_EXPAND = 2;      // 展开文件夹后向下滚动的格数
    private static final int SCROLL_STEPS_PAGE = 3;        // 找不到时翻页滚动的格数
    private static final int MAX_SCROLL_PAGES = 2;         // 最多向下翻几页


    // =========================================================================================
    // 🚀 轻武器引擎：原生寻路劫持 (专攻五环)
    // =========================================================================================

    public PathingResult triggerWuHuanNativePathingP1() {
        log.info("📡 [五环制导] 准备劫持 P1 阶段内置寻路...");

        if (!activateTaskIfPresent("wuhuan", true)) {
            log.info("🎉 [五环制导] 通用雷达扫描完毕，未发现五环任务，判定五环全部结束！");
            return PathingResult.FINISHED;
        }

        Point anchor = findFenXiangAnchor();
        if (anchor == null) {
            return closePanelAndReturn(PathingResult.UI_ERROR);
        }

        executeSafeClick(anchor.x + P1_LINK_OFFSET_X, anchor.y + P1_LINK_OFFSET_Y, 30, 8, DELAY_NORMAL);
        return closePanelAndReturn(PathingResult.SUCCESS);
    }

    public PathingResult triggerWuHuanNativePathingP2() {
        log.info("📡 [五环P2制导] 准备劫持 P2 阶段打怪内置寻路...");

        if (!activateTaskIfPresent("wuhuan", true)) {
            log.warn("⚠️ [五环P2制导] 未找到五环任务！");
            return PathingResult.UI_ERROR;
        }

        Point anchor = findFenXiangAnchor();
        if (anchor == null) {
            return closePanelAndReturn(PathingResult.UI_ERROR);
        }

        int[] rightRect = coordinateHelper.getAbsoluteRectByAnchor(anchor, OFFSET_TO_EDGE_X_RIGHT, OFFSET_TO_EDGE_Y, DETAIL_W_RIGHT, DETAIL_H);
        String rawScanPath = "images/temp/p2_right_panel_scan_raw.png";

        if (!tracker.captureToFile("P2右侧扫描", rawScanPath, rightRect[0], rightRect[1], rightRect[2], rightRect[3])) {
            return closePanelAndReturn(PathingResult.UI_ERROR);
        }

        String washedScanPath = "images/temp/p2_right_panel_scan_washed.png";
        ImagePreprocessor.washGreenTextToBlackAndWhite(rawScanPath, washedScanPath);
        log.info("🚿 [五环P2制导] 右侧面板洗图完成！");

        String foundMonsterName = "";
        double[] localMatchResult = null;

        for (String monsterTemplate : P2_MONSTER_TEMPLATES) {
            localMatchResult = com.bot.dhxy.core.ImageFinder.find(washedScanPath, monsterTemplate, THRESHOLD_STRICT);
            if (localMatchResult != null && localMatchResult.length >= 2) {
                foundMonsterName = monsterTemplate;
                break;
            }
        }

        if (localMatchResult != null) {
            int absoluteClickX = rightRect[0] + (int) Math.round(localMatchResult[0]);
            int absoluteClickY = rightRect[1] + (int) Math.round(localMatchResult[1]);

            log.info("🖱️ [五环P2制导] 锁定怪物 [{}]", foundMonsterName);
            executeSafeClick(absoluteClickX, absoluteClickY, 8, 4, DELAY_NORMAL);
            return closePanelAndReturn(PathingResult.SUCCESS);
        }

        log.warn("⚠️ [五环P2制导] 黑白雷达扫描失败！");
        return closePanelAndReturn(PathingResult.UI_ERROR);
    }

    // =========================================================================================
    // 🕵️ 核心引擎：发光文字探针雷达
    // =========================================================================================

    public boolean activateTaskIfPresent(String taskBaseName) {
        return activateTaskIfPresent(taskBaseName, false);
    }

    public boolean activateTaskIfPresent(String taskBaseName, boolean keepPanelOpen) {
        log.info("🕵️ [任务总管] 启动雷达扫描任务: [{}], 阅后即焚: {}", taskBaseName, !keepPanelOpen);

        Point anchor = ensurePanelOpenAndGetAnchor();
        if (anchor == null) {
            log.warn("⚠️ [任务总管] 呼出任务面板失败，退出！");
            return false;
        }

        int[] leftListRect = coordinateHelper.getAbsoluteRectByAnchor(anchor, OFFSET_TO_EDGE_X, OFFSET_TO_EDGE_Y, DETAIL_W_LEFT, DETAIL_H);
        String taskPath = "images/template/task/" + taskBaseName + ".png";
        String titlePath = "images/template/task/" + taskBaseName + "_title.png";

        for (int page = 0; page < MAX_SCROLL_PAGES; page++) {
            if (page > 0) log.info("👁️ [任务总管] 正在扫描 第 {} 页...", page + 1);

            // 👉 动作 1：找子任务 & 发光探测
            Point taskPt = coordinateHelper.findImageInRegion(taskPath, leftListRect, THRESHOLD_STRICT);
            if (taskPt != null) {
                log.info("🎯 [任务总管] 找到目标中心点: ({}, {})", taskPt.x, taskPt.y);

                int textX1 = taskPt.x + PROBE_OFFSET_X;
                int textY1 = taskPt.y + PROBE_OFFSET_Y;
                int textX2 = textX1 + PROBE_W;
                int textY2 = textY1 + PROBE_H;

                java.awt.image.BufferedImage textImg = tracker.captureToMemory("text_probe", textX1, textY1, textX2, textY2);
                if (textImg != null) {
                    int whitePixelCount = 0;
                    for (int x = 0; x < textImg.getWidth(); x++) {
                        for (int y = 0; y < textImg.getHeight(); y++) {
                            int rgb = textImg.getRGB(x, y);
                            int r = (rgb >> 16) & 0xFF;
                            int g = (rgb >> 8) & 0xFF;
                            int b = rgb & 0xFF;

                            if (r > GLOW_RGB_MIN && g > GLOW_RGB_MIN && b > GLOW_RGB_MIN) {
                                whitePixelCount++;
                            }
                        }
                    }
                    textImg.flush();

                    if (whitePixelCount > GLOW_PIXELS_TARGET) {
                        log.info("✅ [任务总管] 侦测到发光白像素: {}个，【已高亮】", whitePixelCount);
                    } else {
                        log.info("🖱️ [任务总管] 侦测到发光白像素: {}个，【未高亮】，执行激活...", whitePixelCount);
                        executeSafeClick(taskPt.x, taskPt.y, 20, 5, DELAY_NORMAL);
                    }

                    if (!keepPanelOpen) inputProvider.pressAltQ();
                    return true;
                }
            }

            // 👉 动作 2：找大标题(文件夹) 处理折叠
            Point titlePt = coordinateHelper.findImageInRegion(titlePath, leftListRect, THRESHOLD_STRICT);
            if (titlePt != null) {
                log.info("📁 [任务总管] 发现标题，点击展开并下滚...");
                executeSafeClick(titlePt.x + 30, titlePt.y + 5, 20, 5, DELAY_LONG);

                Point hoverPt = coordinateHelper.getRandomizedPoint(anchor.x + SCROLL_HOVER_OFFSET_X, anchor.y + SCROLL_HOVER_OFFSET_Y, 50, 100);
                inputProvider.moveMouse(hoverPt.x, hoverPt.y);
                sleep(DELAY_SHORT);
                inputProvider.scrollDown(SCROLL_STEPS_EXPAND);
                sleep(DELAY_NORMAL);

                page--; // 重搜当前页
                continue;
            }

            // 👉 动作 3：滚轮翻页
            if (page < MAX_SCROLL_PAGES - 1) {
                log.info("⏬ [任务总管] 当前页未找到，向下翻页...");
                Point hoverPt = coordinateHelper.getRandomizedPoint(anchor.x + SCROLL_HOVER_OFFSET_X, anchor.y + SCROLL_HOVER_OFFSET_Y, 50, 100);
                inputProvider.moveMouse(hoverPt.x, hoverPt.y);
                sleep(DELAY_SHORT);
                inputProvider.scrollDown(SCROLL_STEPS_PAGE);
                sleep(DELAY_LONG); // 翻页稍微多等一下渲染
            }
        }

        log.info("⏭️ [任务总管] 翻遍所有页未发现任务。");
        inputProvider.pressAltQ();
        return false;
    }

    // =========================================================================================
    // 🛠️ 重武器引擎：深度 OCR 坐标解析 (修罗/抓鬼保留)
    // =========================================================================================

    public QuestTargetInfo fetchCurrentQuestInfo(String expectedTaskImage) {
        log.info("📡 [深度解析] 启动重型 OCR 引擎获取任务情报...");

        // 强制初始化面板
        inputProvider.pressAltQ();
        sleep(DELAY_LONG);
        Point anchor = ensurePanelOpenAndGetAnchor();
        if (anchor == null) return null;

        String taskTemplatePath = "images/template/" + expectedTaskImage;
        Point taskLabelPoint = coordinateHelper.findImageAbsoluteCoordinate(taskTemplatePath, THRESHOLD_STRICT);

        if (taskLabelPoint == null) {
            log.info("🎉 [深度解析] 未发现目标任务标签 [{}]，判定任务已完成！", expectedTaskImage);
            inputProvider.pressAltQ();
            return null;
        }

        executeSafeClick(taskLabelPoint.x, taskLabelPoint.y, 20, 5, DELAY_NORMAL);

        int[] rightRect = coordinateHelper.getAbsoluteRectByAnchor(anchor, OFFSET_TO_EDGE_X_RIGHT, OFFSET_TO_EDGE_Y, DETAIL_W_RIGHT, DETAIL_H);
        String rightScanPath = "images/temp/quest_detail_scan.png";
         if (!tracker.captureToFile("任务详情扫描", rightScanPath, rightRect[0], rightRect[1], rightRect[2], rightRect[3])) {
             inputProvider.pressAltQ();
             return null;
         }

        inputProvider.pressAltQ();
        sleep(DELAY_NORMAL);

        List<TextRecognizer.OcrWordResult> results = ocr.getAllTextResults(rightScanPath);
        if (results == null || results.isEmpty()) return null;

        StringBuilder fullTextBuilder = new StringBuilder();
        for (TextRecognizer.OcrWordResult word : results) {
            fullTextBuilder.append(word.getText());
        }
        String fullText = fullTextBuilder.toString();
        log.info("🔍 [深度解析] OCR 原始文本: {}", fullText);

        Matcher matcher = QUEST_PATTERN.matcher(fullText);
        if (matcher.find()) {
            String npcName = matcher.group(1).trim();
            String mapName = matcher.group(2).trim();
            int x = Integer.parseInt(matcher.group(3));
            int y = Integer.parseInt(matcher.group(4));

            log.info("✅ [深度解析] 情报解析成功 -> NPC: [{}], 地图: [{}], 坐标: ({}, {})", npcName, mapName, x, y);
            context.setCurrentTaskName("修罗/抓鬼目标: " + npcName);
            return new QuestTargetInfo(npcName, mapName, x, y, fullText);
        }

        return null;
    }

    // =========================================================================================
    // 🧰 DRY 共用工具库 (底层辅助)
    // =========================================================================================

    /**
     * 获取面板锚点，如果未打开则自动按 Alt+Q 尝试呼出
     */
    private Point ensurePanelOpenAndGetAnchor() {
        Point anchor = findFenXiangAnchor();
        if (anchor == null) {
            log.info("🪟 [面板调控] 任务面板未开启，尝试 Alt+Q 呼出...");
            inputProvider.pressAltQ();
            sleep(DELAY_LONG);
            anchor = findFenXiangAnchor();
        }
        return anchor;
    }

    /**
     * 封装带随机漂移的拟人化点击，并强制包含后续动作等待
     */
    private void executeSafeClick(int baseX, int baseY, int randRadiusX, int randRadiusY, long postDelayMs) {
        Point safePt = coordinateHelper.getRandomizedPoint(baseX, baseY, randRadiusX, randRadiusY);
        inputProvider.clickLeft(safePt.x, safePt.y, 100);
        sleep(postDelayMs);
    }

    /**
     * 简化关闭面板并返回结果的操作
     */
    private <T> T closePanelAndReturn(T returnValue) {
        inputProvider.pressAltQ();
        return returnValue;
    }

    private Point findFenXiangAnchor() {
        return coordinateHelper.findImageAbsoluteCoordinate(ANCHOR_TEMPLATE_PATH, THRESHOLD_NORMAL);
    }

    private void sleep(long ms) {
        try { Thread.sleep(ms); } catch (Exception ignored) {}
    }
}