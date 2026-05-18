package com.bot.dhxy.service;

import com.bot.dhxy.config.BotProperties;
import com.bot.dhxy.core.GameClientTracker;
import com.bot.dhxy.core.GameContext;
import com.bot.dhxy.core.TextRecognizer;
import com.bot.dhxy.input.InputProvider;
import com.bot.dhxy.input.InputSequences;
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
    private static final double THRESHOLD_NORMAL = 0.8;
    private static final int MAP_POPUP_RECT_X_OFFSET = 278;
    private static final int MAP_POPUP_RECT_Y_OFFSET = 595;
    private static final int MAP_POPUP_RECT_WIDTH = 406;
    private static final int MAP_POPUP_RECT_HEIGHT = 137;

    private final BotProperties config;
    private final GameContext context;
    private final LocationVisionService locationRadar;
    private final GameClientTracker tracker;
    private final InputProvider inputProvider;
    private final InputSequences inputSequences;
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

    public boolean navigateToNPC(String targetMapName, int targetX, int targetY) {
        if (!navigateToMap(targetMapName)) {
            return false;
        }
        boolean result = navigateInCurrentMap(targetX, targetY);
        uiCleanerService.cleanUpAll();
        return result;
    }

    public boolean navigateInCurrentMap(int targetX, int targetY) {
        String mapName = context.getMe().getCurrentMapName();
        log.info("navigate in map: {} target=({}, {})", mapName, targetX, targetY);

        Point pixelPoint = coordinateHelper.getPhysicalMapPoint(mapName, targetX, targetY);
        if (pixelPoint == null) {
            log.error("map transform missing: {}", mapName);
            return false;
        }

        long startTime = System.currentTimeMillis();
        long timeoutMs = 60000;

        inputProvider.pressAlt1();
        sleepInterruptible(800);
        inputProvider.clickLeft(pixelPoint.x, pixelPoint.y, 200);
        sleepInterruptible(500);
        inputProvider.pressAlt1();
        sleepInterruptible(1500);

        while (System.currentTimeMillis() - startTime < timeoutMs) {
            if (battleRadarService.checkAndSyncCombatState()) {
                sleepInterruptible(battleRadarService.getDynamicPollingIntervalMs());
                startTime = System.currentTimeMillis();
                continue;
            }

            context.setCurrentActionState(GameContext.ActionState.NAVIGATING);

            if (!gameStateUtil.isMovingByPixelDiff()) {
                playerStateService.syncMyPosition();
                PlayerCharacter me = context.getMe();

                if (Math.abs(me.getX() - targetX) <= 2 && Math.abs(me.getY() - targetY) <= 2) {
                    log.info("arrived: ({}, {})", me.getX(), me.getY());
                    return true;
                }

                inputProvider.pressAlt1();
                sleepInterruptible(800);
                inputProvider.clickLeft(pixelPoint.x, pixelPoint.y, 200);
                sleepInterruptible(500);
                inputProvider.pressAlt1();
                sleepInterruptible(1500);
            }

            sleepInterruptible(500);
        }

        log.error("navigate timeout");
        return false;
    }

    private boolean navigateToMap(String targetMapName) {
        PlayerCharacter me = context.getMe();
        log.info("navigate to map: {} current={}", targetMapName, me.getCurrentMapName());

        if (targetMapName.equals(me.getCurrentMapName())) {
            return true;
        }

        if (!openMapAndInputTarget(targetMapName) || !clickLastNavPoint(targetMapName, false)) {
            log.warn("first navigate attempt failed, entering retry loop");
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

            if (gameStateUtil.isMovingByPixelDiff()) {
                stuckCount = 0;
                continue;
            }

            TextRecognizer.LocationInfo locationInfo = locationRadar.scanCurrentLocation();
            if (locationInfo != null) {
                me.setCurrentMapName(locationInfo.mapName);
                if (targetMapName.equals(locationInfo.mapName)) {
                    log.info("arrived map: {}", targetMapName);
                    return true;
                }
            }

            stuckCount++;
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

        log.error("map navigation timeout");
        return false;
    }

    public boolean clickLastNavPoint(String targetMapName, boolean reclick) {
        if (reclick) {
            if (lastAbsoluteLogicalX != DEFAULT_LOGICAL_COORDINATE
                    && lastAbsoluteLogicalY != DEFAULT_LOGICAL_COORDINATE) {
                int clickX = lastAbsoluteLogicalX + random.nextInt(7) - 3;
                int clickY = lastAbsoluteLogicalY + random.nextInt(7) - 3;
                if (openMap()) {
                    inputProvider.clickLeft(clickX, clickY, 150);
                    if (!sleepInterruptible(2000)) {
                        return false;
                    }
                    closeMapByDoubleRightClick();
                }
                return true;
            }
            return targetMapName != null && !targetMapName.isBlank() && openMapAndInputTarget(targetMapName);
        }

        int[] mapRect = coordinateHelper.getScaledRect(
                config.getAnchor_windowTo_map_search_X(), config.getAnchor_windowTo_map_search_Y(),
                MAP_SEARCH_RECT_WIDTH, MAP_SEARCH_RECT_HEIGHT);

        String mapResultImagePath = "images/temp/map_result_scan.png";
        if (!tracker.captureToFile("map result", mapResultImagePath, mapRect[0], mapRect[1], mapRect[2], mapRect[3])) {
            return false;
        }

        Point relativeCenter = ocr.findLastCoordinateLink(mapResultImagePath);
        if (relativeCenter == null) {
            return false;
        }

        lastAbsoluteLogicalX = mapRect[0] + relativeCenter.x;
        lastAbsoluteLogicalY = mapRect[1] + relativeCenter.y;

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
            return false;
        }

        if (!isWorldMapOpened()) {
            inputProvider.pressAlt2();
            if (!sleepInterruptible(500)) {
                return false;
            }
        }

        Point xunluPoint = coordinateHelper.findImageAbsoluteCoordinate(XUNLU_TEMPLATE_PATH, THRESHOLD_NORMAL);
        if (xunluPoint == null) {
            return false;
        }

        inputProvider.clickLeft(xunluPoint.x, xunluPoint.y, 120);
        return sleepInterruptible(250);
    }

    private boolean inputTarget(String targetMapName) {
        int scrollFocusX = tracker.getWindowBaseX() + config.getAnchor_windowTo_map_scroll_X();
        int scrollFocusY = tracker.getWindowBaseY() + config.getAnchor_windowTo_map_scroll_Y();
        return inputSequences.typeTextEnterAndScroll(targetMapName, scrollFocusX, scrollFocusY);
    }

    private boolean isWorldMapOpened() {
        Point titlePoint = coordinateHelper.findImageAbsoluteCoordinate("images/template/world_map_title.png", THRESHOLD_NORMAL);
        return titlePoint != null;
    }

    public void forceScrollToBottom(int targetX, int targetY) {
        inputProvider.clickLeft(targetX, targetY, 50);
        for (int i = 0; i < 2; i++) {
            inputProvider.scrollDown(2);
            if (!sleepInterruptible(100)) {
                return;
            }
        }
    }

    public void ensureMapTrackingOption() {
        inputProvider.pressAlt1();
        sleepInterruptible(400);

        int[] rect = coordinateHelper.getScaledRect(MAP_POPUP_RECT_X_OFFSET, MAP_POPUP_RECT_Y_OFFSET,
                MAP_POPUP_RECT_WIDTH, MAP_POPUP_RECT_HEIGHT);
        double strictThreshold = 0.95;

        Point checkedRes = coordinateHelper.findImageInRegion("images/template/map/checkbox_checked.png", rect, strictThreshold);
        if (checkedRes != null) {
            inputProvider.pressAlt1();
            return;
        }

        Point uncheckedRes = coordinateHelper.findImageInRegion("images/template/map/checkbox_unchecked.png", rect, strictThreshold);
        if (uncheckedRes != null) {
            inputProvider.clickLeft(uncheckedRes.x - 13, uncheckedRes.y, 150);
            sleepInterruptible(500);
            inputProvider.pressAlt1();
        }
    }

    private void closeMapByDoubleRightClick() {
        int closeX = tracker.getWindowBaseX() + config.getAnchor_windowTo_map_scroll_X();
        int closeY = tracker.getWindowBaseY() + config.getAnchor_windowTo_map_scroll_Y();
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
