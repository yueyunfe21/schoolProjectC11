package com.bot.dhxy.service;

import com.bot.dhxy.config.BotProperties;
import com.bot.dhxy.config.InputProvider;
import com.bot.dhxy.core.GameClientTracker;
import com.bot.dhxy.core.GameContext;
import com.bot.dhxy.core.ImageFinder;
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
    private static final int MAP_SEARCH_RECT_WIDTH = 392;
    private static final int MAP_SEARCH_RECT_HEIGHT = 242;
    private static double THRESHOLD_NORMAL = 0.8;
    private static final int MAP_POPUP_RECT_X_OFFSET = 278;
    private static final int MAP_POPUP_RECT_Y_OFFSET = 595;
    private static final int MAP_POPUP_RECT_WIDTH = 406;
    private static final int MAP_POPUP_RECT_HEIGHT = 137;

    private final BotProperties config;
    private final GameContext context;
    private final LocationVisionService locationRadar;
    private final GameClientTracker tracker;
    private final InputProvider inputProvider;
    private final TextRecognizer ocr;
    private final GameStateUtil gameStateUtil;
    private final CoordinateHelper coordinateHelper;
    private final UICleanerService uiCleanerService;
    private final DialogService dialogService;
    private final Random random = new Random();
    private final PlayerStateService playerStateService;
    private final BattleRadarService battleRadarService;

    private int lastAbsoluteLogicalX = DEFAULT_LOGICAL_COORDINATE;
    private int lastAbsoluteLogicalY = DEFAULT_LOGICAL_COORDINATE;


    public boolean navigateToNPC(String targetMapName, int targetX, int targetY){
        if (!navigateToMap(targetMapName)) {
            return false;
        }
        boolean result = navigateInCurrentMap(targetX, targetY);
        uiCleanerService.cleanUpAll();
        return result;
    }

    /**
     * 📍 本地小地图精准制导 (首领架构版：单循环极简流)
     */
    public boolean navigateInCurrentMap(int targetX, int targetY) {
        String mapName = context.getMe().getCurrentMapName();
        log.info("🚀 [战术制导] 目标地图: {}, 目标坐标: ({}, {})", mapName, targetX, targetY);

        java.awt.Point pixelPoint = coordinateHelper.getPhysicalMapPoint(mapName, targetX, targetY);
        if (pixelPoint == null) {
            log.error("❌ [战术制导] 转换失败！缺少 [{}] 的测绘数据", mapName);
            return false;
        }

        long startTime = System.currentTimeMillis();
        long timeoutMs = 60000; // 最多寻路 60 秒

        // 🌟 首次发射：打开地图点一下
        log.info("🎯 [战术制导] 发起首次寻路点击...");
        inputProvider.pressAlt1();
        sleepInterruptible(800);
        inputProvider.clickLeft(pixelPoint.x, pixelPoint.y, 200);
        sleepInterruptible(500);
        inputProvider.pressAlt1();
        sleepInterruptible(1500); // 等待角色转身起步

        // ==========================================
        // 🔄 唯一的监控循环 (外偶)
        // ==========================================
        while (System.currentTimeMillis() - startTime < timeoutMs) {

            // 🌟 1. 神经反射：看一眼有没有遇暗雷打起来？
            if (battleRadarService.checkAndSyncCombatState()) {
                log.warn("⚔️ [战术制导] 突遇暗雷！全员挂起，等待战斗结束...");
                sleepInterruptible(battleRadarService.getDynamicPollingIntervalMs());
                // 战斗中强制重置超时时间，防止战斗太久导致寻路超时失败
                startTime = System.currentTimeMillis();
                continue;
            }

            // 🌟 2. 状态同步：告诉大脑我现在在赶路
            context.setCurrentActionState(GameContext.ActionState.NAVIGATING);

            // ... (下面保留您原有的 if (!gameStateUtil.isMovingByPixelDiff()) 等逻辑)

            // 🌟 就是这个 IF (衣服)！直接在循环里处理停下的逻辑！
            if (!gameStateUtil.isMovingByPixelDiff()) {

                // 1. 停下了，立刻查坐标
                log.info("🛑 [战术制导] 侦测到停步，呼叫状态中枢核对...");
                playerStateService.syncMyPosition();
                PlayerCharacter me = context.getMe();

                // 2. 到了吗？
                if (Math.abs(me.getX() - targetX) <= 2 && Math.abs(me.getY() - targetY) <= 2) {
                    log.info("✅ [战术制导] 精确命中目标！当前坐标: ({}, {})", me.getX(), me.getY());
                    return true;
                }

                // 3. 没到？直接在这里补点！点完循环会自动继续监控！
                log.warn("⚠️ [战术制导] 中途卡住，未达目标！重新打开小地图补点...");
                inputProvider.pressAlt1();
                sleepInterruptible(800);
                inputProvider.clickLeft(pixelPoint.x, pixelPoint.y, 200);
                sleepInterruptible(500);
                inputProvider.pressAlt1();

                sleepInterruptible(1500); // 补点后同样等起步
            }

            // 如果还在跑，稍作歇息，继续下一次 while 循环监控
            sleepInterruptible(500);
        }

        log.error("⏳ [战术制导] 寻路总时间超时 (60秒)！放弃任务。");
        return false;
    }

    private boolean navigateToMap(String targetMapName) {
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
            if (dialogService.processDialog(targetMapName)) {
                stuckCount = 0;
                if (!sleepInterruptible(1500)) {
                    return false;
                }
                continue;
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

        Point xunluPoint = coordinateHelper.findImageAbsoluteCoordinate(XUNLU_TEMPLATE_PATH, 0.8);
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
        Point titlePoint = coordinateHelper.findImageAbsoluteCoordinate("images/template/world_map_title.png", 0.8);
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
     * 🗺️ 初始化检测：确保小地图的“点任务追踪不弹小地图”已被勾选
     */
    public void ensureMapTrackingOption() {
        log.info("🔍 正在检查小地图追踪选项状态...");
        // 1. 打开小地图
        inputProvider.pressAlt1();
        sleepInterruptible(400);
        // 2. 侦测 A：是否【已经勾选】？
        String checkedTemplate = "images/template/map/checkbox_checked.png";
        int[] rect = coordinateHelper.getScaledRect(MAP_POPUP_RECT_X_OFFSET, MAP_POPUP_RECT_Y_OFFSET,
                MAP_POPUP_RECT_WIDTH, MAP_POPUP_RECT_HEIGHT);

        // 🌟 核心修复：使用极其严苛的 0.95 阈值！逼迫雷达分辨出那个微小的白色“√”
        double STRICT_THRESHOLD = 0.95;
        Point checkedRes = coordinateHelper.findImageInRegion(checkedTemplate, rect, STRICT_THRESHOLD);

        if (checkedRes != null) {
            log.info("✅ 【点任务追踪不弹小地图】已是开启状态！无需操作，准备退出...");
            inputProvider.pressAlt1();
            return;
        }

        // 3. 侦测 B：是否【未勾选】？
        String uncheckedTemplate = "images/template/map/checkbox_unchecked.png";
        Point uncheckedRes = coordinateHelper.findImageInRegion(uncheckedTemplate, rect, STRICT_THRESHOLD);

        if (uncheckedRes != null) {
            log.warn("⚠️ 发现选项未勾选！正在执行物理点击...");

            // ImageFinder 返回的是模板中心点，只要您截的图包含方框，点中心大概率能触发
            // 为了绝对稳妥，您可以往左侧方框的位置稍微偏移几个像素

            inputProvider.clickLeft(uncheckedRes.x - 13, uncheckedRes.y, 150);

            // 给游戏引擎半秒钟的时间来渲染那个白色的“√”
            try { Thread.sleep(500); } catch (Exception ignored) {}

            log.info("✅ 勾选动作完成！准备退出...");
            inputProvider.pressAlt1();
            return;
        }

        // 4. 彻底没找到（可能没打开小地图界面）
        log.error("❌ 屏幕上未发现该选项框！请确认当前是否处于大地图/小地图界面。");
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