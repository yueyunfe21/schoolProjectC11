package com.bot.dhxy.service;

import com.bot.dhxy.config.BotProperties;
import com.bot.dhxy.config.InputProvider;
import com.bot.dhxy.core.GameClientTracker;
import com.bot.dhxy.core.GameContext;
import com.bot.dhxy.core.TextRecognizer;
import com.bot.dhxy.model.PlayerCharacter;
import com.bot.dhxy.tools.CoordinateHelper;
import com.bot.dhxy.tools.GameStateUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.awt.Point;
import java.util.Random;

@Slf4j
@Component
@RequiredArgsConstructor
public class NavigationService {

    private static final String XUNLU_TEMPLATE_PATH = "images/template/xunlu.png";
    private static final int DEFAULT_LOGICAL_COORDINATE = Integer.MIN_VALUE;
    private static final int DIALOG_RECT_OFFSET_X = 250;
    private static final int DIALOG_RECT_OFFSET_Y = 312;
    private static final int DIALOG_RECT_WIDTH = 529;
    private static final int DIALOG_RECT_HEIGHT = 208;
    private static final int MAP_SEARCH_RECT_WIDTH = 392;
    private static final int MAP_SEARCH_RECT_HEIGHT = 242;

    private final BotProperties config;
    private final GameContext context;
    private final LocationVisionService locationRadar;
    private final GameClientTracker tracker;
    private final InputProvider inputProvider;
    private final UITemplateLocatorService UITemplateLocatorService;
    private final TextRecognizer ocr;
    private final GameStateUtil gameStateUtil;
    private final CoordinateHelper coordinateHelper;
    private final UICleanerService uiCleanerService;
    private final DialogService dialogService;
    private final Random random = new Random();

    private int lastAbsoluteLogicalX = DEFAULT_LOGICAL_COORDINATE;
    private int lastAbsoluteLogicalY = DEFAULT_LOGICAL_COORDINATE;

    private static final java.util.Map<String, java.util.List<String>> MAP_ALIASES = new java.util.HashMap<>();
    static {
        MAP_ALIASES.put("长安", java.util.Arrays.asList("长安", "长安城", "皇宫门口", "化生寺", "去长安", "回长安"));
    }

    public boolean navigateToMap(String targetMapName) {
        PlayerCharacter me = context.getMe();
        log.info("[导航] 请求前往地图: [{}], 当前位置: [{}]", targetMapName, me.getCurrentMapName());

        if (targetMapName.equals(me.getCurrentMapName())) {
            log.info("[导航] 已在目标地图，跳过导航");
            return true;
        }

        if (!openMapAndInputTarget(targetMapName) || !clickLastNavPoint(targetMapName, false)) {
            log.warn("[导航] 首次发起导航失败，进入循环重试");
        }

        long startTime = System.currentTimeMillis();
        long timeoutMs = 180000L;
        int stuckCount = 0;

        while (System.currentTimeMillis() - startTime < timeoutMs) {
            if (dialogService.isDialogOpened()) {
                log.info("[导航] 发现传送对话框，极速处理");
                if (processDialog(targetMapName)) {
                    stuckCount = 0;
                    if (!sleepInterruptible(1500)) {
                        return false;
                    }
                    continue;
                }
            }

            boolean moving = gameStateUtil.isMovingByPixelDiff();
            if (moving) {
                stuckCount = 0;
                continue;
            }

            TextRecognizer.LocationInfo locationInfo = locationRadar.scanCurrentLocation();
            if (locationInfo != null) {
                me.setCurrentMapName(locationInfo.mapName);
                if (targetMapName.equals(locationInfo.mapName)) {
                    log.info("[导航] 成功抵达目标地图: [{}]", targetMapName);
                    uiCleanerService.cleanUpAll();
                    return true;
                }
            }

            stuckCount++;
            log.warn("[导航] 角色停顿第 {} 次", stuckCount);
            if (stuckCount >= 5) {
                if (openMapAndInputTarget(targetMapName) && clickLastNavPoint(targetMapName, false)) {
                    stuckCount = 0;
                }
            } else {
                clickLastNavPoint(targetMapName, true);
            }

            if (!sleepInterruptible(1500)) {
                return false;
            }
        }

        log.error("[导航] 导航超时");
        return false;
    }

    public boolean clickLastNavPoint(String targetMapName, boolean reclick) {
        if (reclick) {
            if (lastAbsoluteLogicalX != DEFAULT_LOGICAL_COORDINATE
                    && lastAbsoluteLogicalY != DEFAULT_LOGICAL_COORDINATE) {
                int offsetX = random.nextInt(7) - 3;
                int offsetY = random.nextInt(7) - 3;
                int clickX = lastAbsoluteLogicalX + offsetX;
                int clickY = lastAbsoluteLogicalY + offsetY;

                log.info("[点击追踪] 来源: clickLastNavPoint(重试防卡死) -> 坐标: {}, {}", clickX, clickY);
                if (openMap()) {
                    inputProvider.clickLeft(clickX, clickY, 150);
                    if (!sleepInterruptible(2000)) {
                        return false;
                    }
                    closeMapByDoubleRightClick();
                }
                return true;
            }
            if (targetMapName == null || targetMapName.isBlank()) {
                return false;
            }
            return openMapAndInputTarget(targetMapName);
        }

        int[] mapRect = coordinateHelper.getScaledRect(
                config.getAnchor_windowTo_map_search_X(), config.getAnchor_windowTo_map_search_Y(),
                MAP_SEARCH_RECT_WIDTH, MAP_SEARCH_RECT_HEIGHT);

        String mapResultImagePath = "images/temp/map_result_scan.png";
        if (!tracker.captureToFile("地图寻路结果", mapResultImagePath, mapRect[0],
                mapRect[1], mapRect[2], mapRect[3])) {
            return false;
        }

        Point relativeCenter = ocr.findLastCoordinateLink(mapResultImagePath);
        if (relativeCenter == null) {
            return false;
        }

        lastAbsoluteLogicalX = mapRect[0] + relativeCenter.x;
        lastAbsoluteLogicalY = mapRect[1] + relativeCenter.y;

        log.info("[点击追踪] 来源: clickLastNavPoint(解析寻路链接成功) -> 坐标: {}, {}", lastAbsoluteLogicalX, lastAbsoluteLogicalY);
        inputProvider.clickLeft(lastAbsoluteLogicalX, lastAbsoluteLogicalY, 150);
        if (!sleepInterruptible(2000)) {
            return false;
        }

        closeMapByDoubleRightClick();
        return true;
    }

    private boolean openMapAndInputTarget(String targetMapName) {
        if (!openMap()) {
            return false;
        }
        return inputTarget(targetMapName);
    }

    private boolean openMap() {
        if (!tracker.bringWindowToFront()) {
            log.warn("[导航] 无法激活游戏窗口");
            return false;
        }

        if (!isWorldMapOpened()) {
            inputProvider.pressAlt2();
            if (!sleepInterruptible(500)) {
                return false;
            }
        }

        Point xunluPoint = UITemplateLocatorService.findTemplateCenter(XUNLU_TEMPLATE_PATH);
        if (xunluPoint == null) {
            log.warn("[导航] 未找到寻路按钮模板 xunlu.png");
            return false;
        }

        log.info("[点击追踪] 来源: openMap(点击寻路按钮) -> 坐标: {}, {}", xunluPoint.x, xunluPoint.y);
        inputProvider.clickLeft(xunluPoint.x, xunluPoint.y, 120);
        return sleepInterruptible(250);
    }

    private boolean inputTarget(String targetMapName) {
        log.info("[导航] 正在输入目标地图: {}", targetMapName);
        inputProvider.typeTextUnicode(targetMapName);
        if (!sleepInterruptible(100)) {
            return false;
        }
        inputProvider.pressEnter();
        forceScrollToBottom(
                tracker.getWindowBaseX() + config.getAnchor_windowTo_map_scroll_X(),
                tracker.getWindowBaseY() + config.getAnchor_windowTo_map_scroll_Y()
        );
        return sleepInterruptible(500);
    }

    private boolean isWorldMapOpened() {
        Point titlePoint = UITemplateLocatorService.findTemplateCenter("images/template/world_map_title.png");
        return titlePoint != null;
    }

    public void forceScrollToBottom(int targetX, int targetY) {
        log.info("[点击追踪] 来源: forceScrollToBottom(点击列表获取焦点) -> 坐标: {}, {}", targetX, targetY);
        inputProvider.clickLeft(targetX, targetY, 50);

        for (int i = 0; i < 2; i++) {
            inputProvider.scrollDown(2);
            if (!sleepInterruptible(100)) {
                return;
            }
        }
    }

    /**
     * 🧠 极速版智能对话框处理器
     */
    private boolean processDialog(String targetMapName) {
        int[] dialogRect = getDialogRect();
        String dialogImagePath = "images/temp/dialog_active_scan.png";

        if (!captureDialogImage("对话框综合扫描", dialogImagePath, dialogRect)) {
            return false;
        }

        java.util.List<TextRecognizer.OcrWordResult> allWords = ocr.getAllTextResults(dialogImagePath);

        // 🚨 触发点 A：把原因传进去
        if (allWords == null || allWords.isEmpty()) {
            return doFallbackClick(dialogRect, "OCR 未识别到任何文字 (可能是残影或误判)");
        }

        java.util.List<String> targetKeywords = MAP_ALIASES.getOrDefault(targetMapName, java.util.Collections.singletonList(targetMapName));
        for (String keyword : targetKeywords) {
            for (TextRecognizer.OcrWordResult word : allWords) {
                if (word.getText().contains(keyword)) {
                    log.info("[导航] 发现目标传送选项 [{}] (触发别名匹配: {})", targetMapName, keyword);
                    clickAbsolutePoint(dialogRect[0] + word.getX(), dialogRect[1] + word.getY());
                    sleepInterruptible(1500);
                    return true;
                }
            }
        }

        java.util.List<String> closeKeywords = java.util.Arrays.asList(
                "取消", "离开", "看一看", "哪儿也", "以后再说", "需要再找", "原来你",
                "我更喜欢", "看看", "我还有事", "人家还没", "不", "没什么", "算了",
                "暂时", "路过", "再会", "没好感", "我还是", "收起来"
        );
        for (String keyword : closeKeywords) {
            for (TextRecognizer.OcrWordResult word : allWords) {
                if (word.getText().contains(keyword)) {
                    log.info("[导航] 非目标 NPC，匹配到关闭词 [{}]，执行关闭", keyword);
                    clickAbsolutePoint(dialogRect[0] + word.getX(), dialogRect[1] + word.getY());
                    return true;
                }
            }
        }

        // 🚨 触发点 B：把原因传进去
        return doFallbackClick(dialogRect, "有文字，但未匹配到目标地名或关闭词");
    }

    /**
     * 🛡️ 独立的物理兜底方法 (带原因溯源)
     */
    private boolean doFallbackClick(int[] dialogRect, String reason) {
        // 🌟 日志里直接打印出具体的触发原因！
        log.warn("🛡️ [导航兜底] 触发原因: [{}] -> 执行物理中心点击！", reason);

        int centerX = dialogRect[0] + ((dialogRect[2] - dialogRect[0]) / 2);
        int centerY = dialogRect[1] + ((dialogRect[3] - dialogRect[1]) / 2);
        clickAbsolutePoint(centerX, centerY);
        return true;
    }

    private void clickAbsolutePoint(int x, int y) {
        int randomX = x + (random.nextInt(5) - 2);
        int randomY = y + (random.nextInt(5) - 2);
        log.info("[点击追踪] 来源: clickAbsolutePoint(处理对话框) -> 原始:{},{} | 随机后:{},{}", x, y, randomX, randomY);
        inputProvider.clickLeft(randomX, randomY, 150);
    }

    private int[] getDialogRect() {
        return coordinateHelper.getScaledRect(
                DIALOG_RECT_OFFSET_X,
                DIALOG_RECT_OFFSET_Y,
                DIALOG_RECT_WIDTH,
                DIALOG_RECT_HEIGHT
        );
    }

    private boolean captureDialogImage(String sceneName, String imagePath, int[] dialogRect) {
        return tracker.captureToFile(
                sceneName,
                imagePath,
                dialogRect[0],
                dialogRect[1],
                dialogRect[2],
                dialogRect[3]
        );
    }

    private void closeMapByDoubleRightClick() {
        int closeX = tracker.getWindowBaseX() + config.getAnchor_windowTo_map_scroll_X();
        int closeY = tracker.getWindowBaseY() + config.getAnchor_windowTo_map_scroll_Y();
        log.info("[点击追踪] 来源: clickLastNavPoint(双击右键关闭地图) -> 坐标: {}, {}", closeX, closeY);
        inputProvider.doubleRightClick(closeX, closeY, 150, 500);
    }

    private boolean sleepInterruptible(long ms) {
        try {
            Thread.sleep(ms);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
}