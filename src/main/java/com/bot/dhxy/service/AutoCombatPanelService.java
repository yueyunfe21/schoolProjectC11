package com.bot.dhxy.service;

import com.bot.dhxy.core.GameClientTracker;
import com.bot.dhxy.input.InputSequences;
import com.bot.dhxy.input.action.InputAction;
import com.bot.dhxy.tools.CoordinateHelper;
import com.bot.dhxy.tools.ImagePreprocessor;
import com.bot.dhxy.window.runtime.WindowScopedTempPath;
import com.bot.dhxy.window.runtime.WindowTaskContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.awt.Point;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class AutoCombatPanelService {
    private static final String QUXIAO_ZIDONG_PATH = "images/template/battle/quxiao_zidong_green.png";
    private static final String ZIDONGHAI_PATH = "images/template/battle/zidonghai_white.png";

    private static final int TARGET_PANEL_X_OFFSET = 448;
    private static final int TARGET_PANEL_Y_OFFSET = 735;

    private final GameClientTracker tracker;
    private final CoordinateHelper coordinateHelper;
    private final InputSequences inputSequences;
    private final WindowScopedTempPath windowScopedTempPath;
    private final WindowTaskContextHolder windowTaskContextHolder;

    private final Map<String, AutoCombatPanelRuntimeState> runtimeStates = new ConcurrentHashMap<>();

    public void verifyAndAlignPanel() {
        AutoCombatPanelRuntimeState state = state();
        log.info("auto-combat panel: verify and align");
        Point panelPoint = findAutoCombatBox();

        if (panelPoint == null) {
            log.warn("auto-combat panel not found; press Alt+8 and retry");
            inputSequences.submitAndWait("battle:openAutoPanel", List.of(
                    InputAction.pressAlt8(),
                    InputAction.sleep(1000)
            ));
            panelPoint = findAutoCombatBox();
        }

        if (panelPoint == null) {
            log.error("auto-combat panel still not found after Alt+8");
            return;
        }

        int dropX = tracker.getWindowBaseX() + TARGET_PANEL_X_OFFSET;
        int dropY = tracker.getWindowBaseY() + TARGET_PANEL_Y_OFFSET;
        if (panelPoint.distance(dropX, dropY) > 20.0) {
            log.info("auto-combat panel is outside safe area; drag from=({}, {}) to=({}, {})",
                    panelPoint.x, panelPoint.y, dropX, dropY);
            inputSequences.submitAndWait("battle:dragAutoPanel", List.of(
                    InputAction.dragAndDrop(panelPoint.x, panelPoint.y, dropX, dropY),
                    InputAction.sleep(500)
            ));
        } else {
            log.info("auto-combat panel is already in safe area");
        }

        state.panelAligned = true;
        state.estimatedRounds = 25;
    }

    public void recordCombatExit() {
        AutoCombatPanelRuntimeState state = state();
        if (state.estimatedRounds > 0) {
            state.estimatedRounds -= 3;
            log.info("auto-combat panel rounds estimate after combat exit: {}", state.estimatedRounds);
        }
    }

    private Point findAutoCombatBox() {
        tracker.updateGlobalVision();
        String rawPath = tracker.getLatestVisionPath();

        BufferedImage rawImage = ImagePreprocessor.pathToBufferedImage(rawPath);
        if (rawImage == null) {
            log.error("auto-combat panel scan failed: cannot read screenshot path={}", rawPath);
            return null;
        }

        String washedGreenPath = windowScopedTempPath.resolve("debug_hsv_mask_green.png");
        ImagePreprocessor.countGreenPixelsHSV(rawImage, washedGreenPath);
        Point greenPoint = coordinateHelper.findImageAbsoluteCoordinateByImagePath(QUXIAO_ZIDONG_PATH, washedGreenPath, 0.80);
        if (greenPoint != null) {
            log.info("auto-combat panel green marker matched: point=({}, {})", greenPoint.x, greenPoint.y);
            rawImage.flush();
            return new Point(greenPoint.x + 20, greenPoint.y - 28);
        }
        log.warn("auto-combat panel green marker not matched: path={}", washedGreenPath);

        String washedWhitePath = windowScopedTempPath.resolve("debug_thin_white_text.png");
        ImagePreprocessor.countThinWhitePixelsHSV(rawImage, washedWhitePath);
        Point whitePoint = coordinateHelper.findImageAbsoluteCoordinateByImagePath(ZIDONGHAI_PATH, washedWhitePath, 0.80);
        rawImage.flush();

        if (whitePoint == null) {
            log.warn("auto-combat panel white marker not matched: path={}", washedWhitePath);
            return null;
        }
        log.info("auto-combat panel white marker matched: point=({}, {})", whitePoint.x, whitePoint.y);
        return new Point(whitePoint.x + 43, whitePoint.y + 28);
    }

    private AutoCombatPanelRuntimeState state() {
        String key = windowTaskContextHolder.rawCurrent()
                .map(windowContext -> windowContext.getWindowId())
                .orElse("default");
        return runtimeStates.computeIfAbsent(key, ignored -> new AutoCombatPanelRuntimeState());
    }

    private static class AutoCombatPanelRuntimeState {
        private boolean panelAligned = false;
        private int estimatedRounds = -1;
    }
}
