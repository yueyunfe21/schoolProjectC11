package com.bot.dhxy.service;

import com.bot.dhxy.config.InputProvider;
import com.bot.dhxy.core.GameClientTracker;
import com.bot.dhxy.core.GameContext;
import com.bot.dhxy.core.TextRecognizer;
import com.bot.dhxy.model.QuestTargetInfo;
import com.bot.dhxy.tools.CoordinateHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.awt.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 🧠 任务情报总管 (Task Manager)
 * 武器库版：同时搭载【零 OCR 原生盲狙】与【深度 OCR 坐标解析】双引擎！
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
    // 📐 面板测绘数据 (以“分享”按钮为锚点)
    // ==========================================
    private static final String ANCHOR_TEMPLATE_PATH = "images/template/task_fenxiang.png";

    // 左右双切切割数据
    private static final int OFFSET_TO_EDGE_X = -497;
    private static final int OFFSET_TO_EDGE_Y = 8;
    private static final int DETAIL_H = 295;
    private static final int DETAIL_W_TOTAL = 513;
    private static final int DETAIL_W_RIGHT = 290;
    private static final int DETAIL_W_LEFT = DETAIL_W_TOTAL - DETAIL_W_RIGHT;
    private static final int OFFSET_TO_EDGE_X_RIGHT = OFFSET_TO_EDGE_X + DETAIL_W_LEFT;

    // 🎯 五环专属：P1 右侧超链接盲狙点 (首领测绘)
    private static final int P1_LINK_OFFSET_X = -209;
    private static final int P1_LINK_OFFSET_Y = 37;

    // 🧠 修罗专属：OCR 正则提取器
    private static final Pattern QUEST_PATTERN = Pattern.compile("([^\\(]+).*?在\\s*([\\u4e00-\\u9fa5]+)\\s*\\((\\d+)\\s*,\\s*(\\d+)\\)");

    // =========================================================================================
    // 🚀 轻武器引擎：原生寻路劫持 (专攻五环，0 OCR成本)
    // =========================================================================================
// ==========================================
    // 🚦 专为原生寻路定义的三态信号灯
    // ==========================================
    public enum PathingResult {
        SUCCESS,    // ✅ 成功找到并点击了盲狙点
        FINISHED,   // 🎉 确认任务列表里没有该任务了（真做完了）
        UI_ERROR    // ⚠️ 界面卡顿、没找到锚点（需要重试）
    }

    /**
     * 🚀 P1 阶段：五环专属原生寻路引擎 (写死特征图，极致精简)
     */
    public PathingResult triggerWuHuanNativePathingP1() {
        log.info("📡 [五环制导] 准备劫持 P1 阶段内置寻路...");

        inputProvider.pressAltQ();
        sleep(800);

        Point anchor = findFenXiangAnchor();
        if (anchor == null) {
            log.warn("⚠️ [五环制导] 未找到任务面板锚点，可能是UI延迟！");
            inputProvider.pressAltQ();
            return PathingResult.UI_ERROR;
        }

        // ==========================================
        // 🧠 阶段 1：拟人化焦点感知 (直接写死五环的图)
        // ==========================================
        int[] leftRect = coordinateHelper.getAbsoluteRectByAnchor(anchor, OFFSET_TO_EDGE_X, OFFSET_TO_EDGE_Y, DETAIL_W_LEFT, DETAIL_H);
        String leftScanPath = "images/temp/quest_list_scan.png";
        tracker.captureToFile("任务列表扫描", leftScanPath, leftRect[0], leftRect[1], leftRect[2], leftRect[3]);

        // 👁️ 第一眼：找五环高亮状态
        String selectedPath = "images/template/wuhuan/wuhuan_selected.png";
        Point selectedPt = coordinateHelper.findImageAbsoluteCoordinate(selectedPath, 0.85);

        if (selectedPt != null) {
            log.info("🎯 [拟人化] 发现五环任务已高亮选中，直接进入盲狙环节！");
        } else {
            // 👁️ 第二眼：找五环普通状态
            String unselectedPath = "images/template/wuhuan/wuhuan_unselected.png";
            Point unselectedPt = coordinateHelper.findImageAbsoluteCoordinate(unselectedPath, 0.85);

            if (unselectedPt != null) {
                log.info("🖱️ [拟人化] 焦点偏移，正在手动选中五环任务...");
                inputProvider.clickLeft(unselectedPt.x, unselectedPt.y, 100);
                sleep(500);
            } else {
                // 👁️ 第三眼：都没找到，五环彻底做完了
                log.info("🎉 [五环制导] 左右侦测均未发现五环任务，判定五环全部结束！");
                inputProvider.pressAltQ();
                return PathingResult.FINISHED;
            }
        }

        // ==========================================
        // 🚀 阶段 2：安全盲狙！
        // ==========================================
        int baseClickX = anchor.x + P1_LINK_OFFSET_X;
        int baseClickY = anchor.y + P1_LINK_OFFSET_Y;

        // 生成抖动坐标
        Point safeClick = coordinateHelper.getRandomizedPoint(baseClickX, baseClickY, 30, 8);

        log.info("🖱️ [五环制导] 坐标锁定: ({}, {}), 开火！", safeClick.x, safeClick.y);
        inputProvider.clickLeft(safeClick.x, safeClick.y, 100);
        sleep(500);

        inputProvider.pressAltQ();
        return PathingResult.SUCCESS;
    }

    // ==========================================
    // 🎯 五环专属：P2 怪物特征图阵列
    // ==========================================
    private static final String[] P2_MONSTER_TEMPLATES = {
            "images/template/wuhuan/p2_guanpian.png",      // 惯骗
            "images/template/wuhuan/p2_daohaozei.png",     // 盗号贼
            "images/template/wuhuan/p2_wuchi.png",         // 武痴
            "images/template/wuhuan/p2_shiyinggui.png"     // 食婴鬼手下的手下
    };

    /**
     * 🚀 P2 阶段：五环专属打怪寻路引擎 (ROI 局部阵列扫描)
     */
    public PathingResult triggerWuHuanNativePathingP2() {
        log.info("📡 [五环P2制导] 准备劫持 P2 阶段打怪内置寻路...");

        inputProvider.pressAltQ();
        sleep(800);

        // 1. 找锚点 (这一步底层已经刷新了全局视野)
        Point anchor = findFenXiangAnchor();
        if (anchor == null) {
            log.warn("⚠️ [五环P2制导] 未找到任务面板锚点，UI可能未加载！");
            inputProvider.pressAltQ();
            return PathingResult.UI_ERROR;
        }

        // ==========================================
        // ✂️ 阶段 1：精准切割右侧详情面板 (绝不全屏瞎找)
        // ==========================================
        int[] rightRect = coordinateHelper.getAbsoluteRectByAnchor(anchor, OFFSET_TO_EDGE_X_RIGHT, OFFSET_TO_EDGE_Y, DETAIL_W_RIGHT, DETAIL_H);
        String rightScanPath = "images/temp/p2_right_panel_scan.png";

        // 把右侧截取下来
        if (!tracker.captureToFile("P2右侧扫描", rightScanPath, rightRect[0], rightRect[1], rightRect[2], rightRect[3])) {
            inputProvider.pressAltQ();
            return PathingResult.UI_ERROR;
        }

        // ==========================================
        // 🚀 阶段 2：局部特征图匹配 (极速，不到 5 毫秒)
        // ==========================================
        log.info("👁️ [五环P2制导] 启动局部雷达，在右侧面板内扫描怪物...");

        String foundMonsterName = "";
        double[] localMatchResult = null;

        // 遍历怪物图库，直接在刚才切下来的右侧小图里找！
        for (String monsterTemplate : P2_MONSTER_TEMPLATES) {
            // 调用底层的纯核心方法，不涉及任何全屏坐标系
            localMatchResult = com.bot.dhxy.core.ImageFinder.find(rightScanPath, monsterTemplate, 0.85);
            if (localMatchResult != null && localMatchResult.length >= 2) {
                foundMonsterName = monsterTemplate;
                break; // 找到了！立刻跳出
            }
        }

        if (localMatchResult != null) {
            // 🌟 核心计算：局部坐标 -> 全局绝对物理坐标
            // localMatchResult 给出的是在右侧小图里的位置，我们要把它加上右侧小图的起点坐标
            int absoluteClickX = rightRect[0] + (int) Math.round(localMatchResult[0]);
            int absoluteClickY = rightRect[1] + (int) Math.round(localMatchResult[1]);

            // 注入拟人化灵魂 (因为字较小，X轴抖动范围缩小到 8，Y轴 4)
            Point safeClick = coordinateHelper.getRandomizedPoint(absoluteClickX, absoluteClickY, 8, 4);

            log.info("🖱️ [五环P2制导] 锁定怪物 [{}], 防封坐标: ({}, {}), 开火！", foundMonsterName, safeClick.x, safeClick.y);
            inputProvider.clickLeft(safeClick.x, safeClick.y, 100);
            sleep(500);

            inputProvider.pressAltQ();
            return PathingResult.SUCCESS;
        }

        // 走到这里，说明是真没找到怪 (UI卡了，或者刷了新怪)
        // 注意：这里绝不返回 FINISHED，因为 P2 阶段不存在任务做完的可能，只可能是识别失败！
        log.warn("⚠️ [五环P2制导] 未在右侧面板找到任何已知怪物！如果是新怪，请截图添加到图库！");
        inputProvider.pressAltQ();
        return PathingResult.UI_ERROR;
    }

    // =========================================================================================
    // 🛠️ 重武器引擎：深度 OCR 坐标解析 (专攻修罗/抓鬼，消耗 Token，但情报详尽)
    // =========================================================================================
    /**
     * 获取当前任务的具体坐标情报 (保留修罗使用)
     * @param expectedTaskImage 左侧任务列表中目标任务的名字截图 (例如："task_name_xiuluo.png")
     * @return 包含地图、坐标、NPC名字的实体对象
     */
    public QuestTargetInfo fetchCurrentQuestInfo(String expectedTaskImage) {
        log.info("📡 [深度解析] 启动重型 OCR 引擎获取任务情报...");

        inputProvider.pressAltQ();
        sleep(800);
        Point anchor = findFenXiangAnchor();
        if (anchor == null) {
            inputProvider.pressAltQ();
            return null;
        }

        // 🛡️ 阶段 1：左侧安全扫描
        int[] leftRect = coordinateHelper.getAbsoluteRectByAnchor(anchor, OFFSET_TO_EDGE_X, OFFSET_TO_EDGE_Y, DETAIL_W_LEFT, DETAIL_H);
        String leftScanPath = "images/temp/quest_list_scan.png";
        tracker.captureToFile("任务列表扫描", leftScanPath, leftRect[0], leftRect[1], leftRect[2], leftRect[3]);

        String taskTemplatePath = "images/template/" + expectedTaskImage;
        Point taskLabelPoint = coordinateHelper.findImageAbsoluteCoordinate(taskTemplatePath, 0.85);

        if (taskLabelPoint == null) {
            log.info("🎉 [深度解析] 未发现目标任务标签 [{}]，判定任务已完成！", expectedTaskImage);
            inputProvider.pressAltQ();
            return null;
        }

        // 🎯 阶段 2：强制纠正焦点
        inputProvider.clickLeft(taskLabelPoint.x, taskLabelPoint.y, 100);
        sleep(500);

        // 💸 阶段 3：调用 OCR 提纯右侧
        int[] rightRect = coordinateHelper.getAbsoluteRectByAnchor(anchor, OFFSET_TO_EDGE_X_RIGHT, OFFSET_TO_EDGE_Y, DETAIL_W_RIGHT, DETAIL_H);
        String rightScanPath = "images/temp/quest_detail_scan.png";
        if (!tracker.captureToFile("任务详情扫描", rightScanPath, rightRect[0], rightRect[1], rightRect[2], rightRect[3])) {
            inputProvider.pressAltQ();
            return null;
        }

        inputProvider.pressAltQ();
        sleep(500);

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

    private Point findFenXiangAnchor() {
        return coordinateHelper.findImageAbsoluteCoordinate(ANCHOR_TEMPLATE_PATH, 0.8);
    }

    private void sleep(long ms) { try { Thread.sleep(ms); } catch (Exception ignored) {} }
}