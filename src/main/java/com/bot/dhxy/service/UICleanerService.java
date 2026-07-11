package com.bot.dhxy.service;


import com.bot.dhxy.core.GameClientTracker;
import com.bot.dhxy.core.GameContext;
import com.bot.dhxy.core.ImageFinder;
import com.bot.dhxy.input.InputProvider;
import com.bot.dhxy.input.InputSequences;
import com.bot.dhxy.input.action.InputAction;
import com.bot.dhxy.input.action.InputActionScope;
import com.bot.dhxy.model.dialog.DialogResultStatus;
import com.bot.dhxy.runner.stop.TaskSleep;
import com.bot.dhxy.service.dialog.DialogHandleRequest;
import com.bot.dhxy.tools.CoordinateHelper;
import com.bot.dhxy.tools.GameStateUtil;
import com.bot.dhxy.tools.ImagePreprocessor;
import com.bot.dhxy.window.runtime.WindowTaskContextHolder;
import com.bot.dhxy.window.runtime.WindowScopedTempPath;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.stream.Stream;

/**
 * Handles non-task UI interruptions for the currently bound game window.
 *
 * <p>This service is intentionally conservative: it may close world-map/generic windows and click
 * known leave/cancel style options. Mouse clicks are serialized through {@link InputSequences};
 * methods ending in {@code Direct} are only used while already inside an exclusive input section.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UICleanerService {
    private static final int MAP_POPUP_RECT_X_OFFSET = 278;
    private static final int MAP_POPUP_RECT_Y_OFFSET = 595;
    private static final int MAP_POPUP_RECT_WIDTH = 406;
    private static final int MAP_POPUP_RECT_HEIGHT = 137;
    private static final Path GENERIC_CLOSE_TEMPLATE_DIR = Path.of("images/template/cancel");


    private final InputSequences inputSequences;
    private final InputProvider inputProvider;
    private final GameClientTracker tracker;
    private final CoordinateHelper coordinateHelper;
    private final GameStateUtil gameStateUtil;
    private final DialogService dialogService;
    private final WindowScopedTempPath windowScopedTempPath;
    private final GameContext gameContext;
    private final WindowTaskContextHolder windowTaskContextHolder;

    private final Random random = new Random();

    /**
     * Run the broad cleanup used before/after generic task transitions.
     *
     * <p>Side effects: may press Alt+1, click dialog options, and click generic close buttons in the
     * current game window. Do not use this immediately after opening a business NPC dialog unless the
     * caller has first confirmed the dialog is safe to close.</p>
     */
    public void cleanUpAll() {
        log.info("UI cleanup started");
        boolean cleanedAny = false;
        CleanupPass cleanupPass = CleanupPass.start();

        if (isWorldMapOpened(cleanupPass)) {
            cleanedAny = closeMapWindow(cleanupPass) || cleanedAny;
        }

        if (forceCloseDialog()) {
            cleanedAny = true;
        }

        if (closeAllGenericWindows(cleanupPass)) {
            cleanedAny = true;
        }

        log.info(cleanedAny ? "UI cleanup finished" : "UI already clean");
    }

    /**
     * Probe whether startup cleanup would be a no-op without clicking or sending hotkeys.
     *
     * <p>五环多窗口启动会在后台先做这次截图/对话框探测。只有这里确认没有地图窗口、
     * 没有业务对话框、也没有通用关闭按钮时，前台 PREPARE 才能跳过重扫；任何未知或
     * 可疑状态都保留给原来的 {@link #cleanUpAll()} 前台兜底处理。</p>
     *
     * @param source diagnostic source for logs.
     * @return true only when cleanup is clearly unnecessary.
     */
    public boolean probeStartupCleanNoInput(String source) {
        CleanupPass cleanupPass = CleanupPass.start();
        boolean worldMapOpen = isWorldMapOpened(cleanupPass);
        DialogResultStatus dialogStatus = dialogService
                .handleDialog(DialogHandleRequest.inspect(safeSource(source) + ":dialog"))
                .getStatus();
        Point closePoint = findGenericCloseButtonPoint(safeSource(source) + ":generic-close-probe", cleanupPass);
        boolean clean = !worldMapOpen && dialogStatus == DialogResultStatus.NO_DIALOG && closePoint == null;
        log.info("UI startup cleanup precheck: source={} clean={} worldMapOpen={} dialogStatus={} genericClosePoint={}",
                safeSource(source), clean, worldMapOpen, dialogStatus,
                closePoint == null ? "none" : closePoint.x + "," + closePoint.y);
        return clean;
    }

    private boolean isWorldMapOpened(CleanupPass cleanupPass) {
        String screenPath = cleanupPass.screenPath(tracker);
        if (screenPath == null || screenPath.isBlank()) {
            return false;
        }
        if (coordinateHelper.findImageAbsoluteCoordinateByImagePath(
                "images/template/map/world_map_title.png", screenPath, 0.8) != null) {
            return true;
        }

        int[] rect = coordinateHelper.getScaledRect(MAP_POPUP_RECT_X_OFFSET, MAP_POPUP_RECT_Y_OFFSET,
                MAP_POPUP_RECT_WIDTH, MAP_POPUP_RECT_HEIGHT);
        return findImageInCachedRegion(screenPath, "images/template/map/checkbox_checked.png", rect, 0.95) != null
                || findImageInCachedRegion(screenPath, "images/template/map/checkbox_unchecked.png", rect, 0.95) != null;
    }

    private Point findImageInCachedRegion(String screenPath, String templatePath, int[] rect, double matchRate) {
        try {
            BufferedImage frame = ImageIO.read(new File(screenPath));
            if (frame == null) {
                return null;
            }
            int[] frameRect = new int[]{
                    tracker.getWindowBaseX(),
                    tracker.getWindowBaseY(),
                    tracker.getWindowBaseX() + frame.getWidth(),
                    tracker.getWindowBaseY() + frame.getHeight()
            };
            BufferedImage crop = ImagePreprocessor.cropAbsoluteRect(frame, frameRect, rect);
            if (crop == null) {
                return null;
            }
            String roiPath = windowScopedTempPath.resolve("ui_cleanup_cached_roi_scan.png");
            ImageIO.write(crop, "png", new File(roiPath));
            double[] result = ImageFinder.find(roiPath, templatePath, matchRate);
            return coordinateHelper.resolveMatchedPointInRect(rect, result);
        } catch (Exception e) {
            log.warn("UI cleanup cached ROI match failed: template={} rect=({}, {})-({}, {}) reason={}",
                    templatePath, rect[0], rect[1], rect[2], rect[3], e.getMessage(), e);
            return null;
        }
    }

    private boolean closeMapWindow(CleanupPass cleanupPass) {
        boolean submitted = inputSequences.submitAndWait("uiCleanup:closeMapAlt1", List.of(
                InputAction.pressAlt1(),
                InputAction.sleep(500)
        ));
        cleanupPass.invalidateFrame("map window closed");
        if (submitted && !isWorldMapOpened(cleanupPass)) {
            return true;
        }

        if (submitted) {
            log.warn("UI cleanup pressed Alt+1 but map still appears open; falling back to close button");
        }
        return clickCloseButtonOnce("uiCleanup:closeMapButton", cleanupPass);
    }

    /**
     * Close generic X-button windows without processing business dialogs.
     *
     * @return true when at least one close button was clicked; false when no generic close button was
     * found or the thread was interrupted.
     */
    public boolean closeAllGenericWindows() {
        return closeAllGenericWindows(CleanupPass.start());
    }

    private boolean closeAllGenericWindows(CleanupPass cleanupPass) {
        boolean closedAny = false;
        if (isWorldMapOpened(cleanupPass)) {
            closedAny = closeMapWindow(cleanupPass) || closedAny;
        }
        for (int i = 0; i < 3; i++) {
            if (!clickCloseButtonOnce("uiCleanup:closeGenericWindow", cleanupPass)) {
                break;
            }
            closedAny = true;
            cleanupPass.invalidateFrame("generic window closed");
        }
        return closedAny;
    }

    /**
     * Handle lightweight interruptions while an idle/member window is allowed to stay mostly quiet.
     *
     * @param sourceTask diagnostic task name written to logs.
     * @return true when a known business option or generic close button was handled; false when there
     * was nothing actionable or cleanup was interrupted.
     */
    public boolean cleanLightweightInterruptions(String sourceTask) {
        DialogResultStatus dialogResult = dialogService
                .handleDialog(DialogHandleRequest.handleBusinessOption(sourceTask))
                .getStatus();
        if (dialogResult == DialogResultStatus.BUSINESS_OPTION_CLICKED) {
            log.info("UI lightweight cleanup handled business dialog: source={}", sourceTask);
            return true;
        }
        if (dialogResult == DialogResultStatus.INTERRUPTED) {
            log.info("UI lightweight cleanup interrupted: source={}", sourceTask);
            return false;
        }
        if (dialogResult == DialogResultStatus.FAILED) {
            log.warn("UI lightweight cleanup business dialog scan failed: source={}", sourceTask);
            return false;
        }

        if (closeAllGenericWindows()) {
            log.info("UI lightweight cleanup closed generic window: source={}", sourceTask);
            return true;
        }
        return false;
    }

    private boolean clickCloseButtonOnce(String description, CleanupPass cleanupPass) {
        Point closeBtnPoint = findGenericCloseButtonPoint(description, cleanupPass);
        if (closeBtnPoint == null) {
            return false;
        }
        return inputSequences.submitExclusiveAndWait(description, () -> clickCloseButtonOnceDirect(description, closeBtnPoint));
    }

    /**
     * Close the world-map search/input overlay by clicking only the {@code x2.png} close button.
     *
     * <p>This is intentionally narrower than {@link #closeAllGenericWindows()}: route navigation calls
     * it immediately after clicking a world-map route result, where using other generic close-button
     * templates could close an unrelated panel or dialog. The method sends direct mouse input and must
     * therefore only be called by code that already owns the input worker's exclusive callback.</p>
     *
     * @param description diagnostic source written to input and cleanup logs.
     * @return true when the {@code x2.png} button was found and clicked; false when it was not visible
     *         or the capture failed.
     */
    public boolean closeMapSearchInputByX2Direct(String description) {
        if (!tracker.updateGlobalVision()) {
            log.warn("UI cleanup x2-only close skipped: capture failed description={}", description);
            return false;
        }
        String screenPath = tracker.getLatestVisionPath();
        Point closeBtnPoint = coordinateHelper.findImageAbsoluteCoordinateByImagePath(
                "images/template/cancel/x2.png", screenPath, 0.8);
        if (closeBtnPoint == null) {
            log.info("UI cleanup x2-only close skipped: x2 not found description={}", description);
            return false;
        }

        int clickX = closeBtnPoint.x + 4 + random.nextInt(5);
        int clickY = closeBtnPoint.y + 4 + random.nextInt(5);
        log.info("UI cleanup x2-only close matched: description={} click=({}, {})", description, clickX, clickY);
        if (!InputActionScope.checkpoint()) {
            return false;
        }
        inputProvider.clickLeft(clickX, clickY, 80);
        return TaskSleep.sleep(250) && InputActionScope.checkpoint();
    }

    private Point findGenericCloseButtonPoint(String description, CleanupPass cleanupPass) {
        String screenPath = cleanupPass.screenPath(tracker);
        if (screenPath == null || screenPath.isBlank()) {
            log.warn("UI cleanup close button scan skipped: capture failed description={}", description);
            return null;
        }
        Point closeBtnPoint = null;
        String[] closeButtonTemplates = {
                "images/template/cancel/x1.png",
                "images/template/cancel/x2.png",
                "images/template/cancel/x3.png",
                "images/template/cancel/npc_busy_cancel.png"
        };
        for (String templatePath : closeButtonTemplates) {
            closeBtnPoint = coordinateHelper.findImageAbsoluteCoordinateByImagePath(templatePath, screenPath, 0.8);
            if (closeBtnPoint != null) {
                break;
            }
        }

        if (closeBtnPoint == null) {
            log.info("UI cleanup close button not found: description={} screenPath={} templates=x1,x2,x3,npc_busy_cancel",
                    description, screenPath);
            return null;
        }
        return closeBtnPoint;
    }

    private boolean clickCloseButtonOnceDirect(String description, Point closeBtnPoint) {
        int clickX = closeBtnPoint.x + 4 + random.nextInt(5);
        int clickY = closeBtnPoint.y + 4 + random.nextInt(5);
        log.info("UI cleanup close button matched: description={} click=({}, {})", description, clickX, clickY);

        if (!InputActionScope.checkpoint()) {
            return false;
        }
        inputProvider.clickLeft(clickX, clickY, 80);
        return TaskSleep.sleep(250) && InputActionScope.checkpoint();
    }

    private String safeSource(String source) {
        return source == null || source.isBlank() ? "unknown" : source.trim();
    }

    private static class CleanupPass {
        private String screenPath;

        static CleanupPass start() {
            return new CleanupPass();
        }

        String screenPath(GameClientTracker tracker) {
            if (screenPath == null || screenPath.isBlank()) {
                if (!tracker.updateGlobalVision()) {
                    return null;
                }
                screenPath = tracker.getLatestVisionPath();
            }
            return screenPath;
        }

        void invalidateFrame(String reason) {
            screenPath = null;
        }
    }

    /**
     * Close the currently visible dialog only when it matches a generic safe-close pattern.
     *
     * <p>Story dialogs are only fast-clicked when the current window/task state allows it. Option
     * dialogs first try known close/cancel keywords, then fall back to the last green option. The
     * method can send real clicks through the input queue.</p>
     *
     * @return true when a dialog was closed; false when no dialog was present or it was unsafe to
     * close automatically.
     */
    public boolean forceCloseDialog() {
        var dialogInspect = dialogService.handleDialog(DialogHandleRequest.inspect("ui-cleaner:force-close"));
        if (dialogInspect.getStatus() == DialogResultStatus.NO_DIALOG) {
            return false;
        }

        if (dialogInspect.getStatus() == DialogResultStatus.STORY_IGNORED) {
            if (!canFastClickStoryDialog()) {
                log.info("UI cleanup story dialog fast-click skipped: role={} actionState={}",
                        currentWindowRoleText(), gameContext.getCurrentActionState());
                return false;
            }
            dialogService.handleDialog(DialogHandleRequest.clickStory("ui-cleaner:force-close-story"));
            TaskSleep.sleep(350);
            return true;
        }

        DialogResultStatus fallbackResult = dialogService.handleDialog(DialogHandleRequest.fallbackLastOption("ui-cleaner")).getStatus();
        log.info("UI cleanup dialog fallback last option result={}", fallbackResult);
        return fallbackResult == DialogResultStatus.FALLBACK_CLICKED;
    }

    private boolean canFastClickStoryDialog() {
        boolean member = windowTaskContextHolder.rawCurrent()
                .map(windowContext -> windowContext.isMember())
                .orElse(false);
        if (!member) {
            return true;
        }
        return gameContext.getCurrentActionState() == GameContext.ActionState.IN_COMBAT;
    }

    private String currentWindowRoleText() {
        return windowTaskContextHolder.rawCurrent()
                .map(windowContext -> windowContext.getRole().name())
                .orElse("UNKNOWN");
    }

    private void clickAbsolutePoint(int x, int y, String description) {
        inputSequences.clickLeft(description, x + (random.nextInt(5) - 2), y + (random.nextInt(5) - 2), 80);
    }

}
