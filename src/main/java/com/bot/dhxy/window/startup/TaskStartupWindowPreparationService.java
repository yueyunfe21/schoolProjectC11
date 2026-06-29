package com.bot.dhxy.window.startup;

import com.bot.dhxy.core.GameClientTracker;
import com.bot.dhxy.core.ImageFinder;
import com.bot.dhxy.config.BotProperties;
import com.bot.dhxy.driver.BoundWindowKeyboardService;
import com.bot.dhxy.input.InputProvider;
import com.bot.dhxy.input.InputSequences;
import com.bot.dhxy.runner.stop.TaskSleep;
import com.bot.dhxy.tools.CoordinateHelper;
import com.bot.dhxy.tools.GameStateUtil;
import com.bot.dhxy.window.runtime.WindowRuntimeContext;
import com.bot.dhxy.window.runtime.WindowScopedTempPath;
import com.bot.dhxy.window.runtime.WindowTaskContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.awt.Point;

/**
 * Runs the focused UI checks needed before a leader window starts a task.
 *
 * <p>This service owns startup-only window preparation such as Alt+1 map option checks and Alt+5/Alt+6
 * visibility setup. It deliberately keeps those pre-checks outside {@code NavigationService} because
 * they prepare the client UI rather than calculating or executing navigation routes.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TaskStartupWindowPreparationService {

    private static final int MAP_POPUP_RECT_X_OFFSET = 278;
    private static final int MAP_POPUP_RECT_Y_OFFSET = 595;
    private static final int MAP_POPUP_RECT_WIDTH = 406;
    private static final int MAP_POPUP_RECT_HEIGHT = 137;
    private static final double MAP_STARTUP_OPTION_MATCH_RATE = 0.95;
    private static final String MAP_TRACKING_CHECKED_TEMPLATE = "images/template/map/checkbox_checked.png";
    private static final String MAP_TRACKING_UNCHECKED_TEMPLATE = "images/template/map/checkbox_unchecked.png";
    private static final String AUTO_CLOSE_MAP_CHECKED_TEMPLATE = "images/template/map/auto_close_map_checked.png";
    private static final String AUTO_CLOSE_MAP_UNCHECKED_TEMPLATE = "images/template/map/auto_close_map_unchecked.png";
    private static final String OPEN_FLY_CHECKED_TEMPLATE = "images/template/map/open_fly_checked.png";
    private static final String OPEN_FLY_UNCHECKED_TEMPLATE = "images/template/map/open_fly_unchecked.png";
    private static final String ALT5_SHOPPING_TEMPLATE = "images/template/status/blacklist_shopping.png";
    private static final String ALT6_VISIBILITY_TEMPLATE = "images/template/status/blacklist_crowd.png";
    private static final int STATUS_VISIBILITY_RECT_X_OFFSET = 359;
    private static final int STATUS_VISIBILITY_RECT_Y_OFFSET = 271;
    private static final int STATUS_VISIBILITY_RECT_WIDTH = 317;
    private static final int STATUS_VISIBILITY_RECT_HEIGHT = 288;
    private static final double STATUS_VISIBILITY_MATCH_RATE = 0.85;
    private static final int STATUS_VISIBILITY_MAX_ATTEMPTS = 3;
    private static final long STATUS_VISIBILITY_RECHECK_DELAY_MS = 500L;
    private static final long STATUS_OVERLAY_FADEOUT_WAIT_MS = 1000L;
    private static final String EXPAND_CHECKED_TEMPLATE = "images/template/status/expand_checked.png";
    private static final String EXPAND_UNCHECKED_TEMPLATE = "images/template/status/expand_unchecked.png";
    private static final int EXPAND_OPTION_RECT_X_OFFSET = 352;
    private static final int EXPAND_OPTION_RECT_Y_OFFSET = 590;
    private static final int EXPAND_OPTION_RECT_WIDTH = 18;
    private static final int EXPAND_OPTION_RECT_HEIGHT = 20;
    private static final double EXPAND_OPTION_MATCH_RATE = 0.95;
    private static final String FLYING_STATUS_TEMPLATE = "images/template/status/flying.png";
    private static final String UNFLYING_STATUS_TEMPLATE = "images/template/status/unflying.png";
    private static final int FLYING_STATUS_RECT_X_OFFSET = 660;
    private static final int FLYING_STATUS_RECT_Y_OFFSET = 573;
    private static final int FLYING_STATUS_RECT_WIDTH = 52;
    private static final int FLYING_STATUS_RECT_HEIGHT = 24;
    private static final double FLYING_STATUS_MATCH_RATE = 0.84;

    private final GameClientTracker tracker;
    private final InputProvider inputProvider;
    private final InputSequences inputSequences;
    private final CoordinateHelper coordinateHelper;
    private final WindowScopedTempPath windowScopedTempPath;
    private final BotProperties botProperties;
    private final BoundWindowKeyboardService boundWindowKeyboardService;
    private final WindowTaskContextHolder windowTaskContextHolder;

    /**
     * Ensure the mini-map tracking checkbox is enabled.
     *
     * <p>The complete open/check/click/close flow runs as one exclusive input callback so another
     * window cannot focus or click between opening the map and closing it.</p>
     *
     * @return true when the checked state is confirmed or successfully enabled; false when the checkbox
     *         templates cannot be found or the operation is interrupted.
     */
    public boolean ensureMapTrackingOption() {
        return inputSequences.submitExclusiveAndWait("ensureMapTrackingOption", this::ensureMapTrackingOptionDirect);
    }

    /**
     * Run the leader task startup preparation.
     *
     * <p>Only the map/expand panel work needs the exclusive foreground input section. Alt+5 and Alt+6
     * are keyboard-only visibility hotkeys, so they are sent through the bound HWND by
     * {@link #ensureStartupVisibilityOverlays()} instead of using direct focused input.</p>
     *
     * @return true when map tracking, expand state, and startup visibility overlays are confirmed.
     */
    public boolean prepareTaskStartupWindow() {
        if (!botProperties.isTaskStartupPreparationEnabled()) {
            log.info("task startup preparation skipped: develop switch disabled");
            return true;
        }
        return inputSequences.submitExclusiveAndWait("taskStartup:mapTrackingAndVisibility", () -> {
            boolean mapReady = ensureMapTrackingOptionDirect();
            if (!TaskSleep.sleep(200)) {
                return false;
            }
            boolean expandReady = ensureExpandOptionUncheckedDirect();
            if (!TaskSleep.sleep(200)) {
                return false;
            }
            boolean visibilityReady = ensureStartupVisibilityOverlays();
            return mapReady && expandReady && visibilityReady;
        });
    }

    /**
     * Run the same startup checks with background probes first.
     *
     * <p>五环五开启动时，正常路径必须能让多个窗口各自用 HWND 快捷键和截图完成检查，
     * 不能为了确认状态就把窗口逐个切到前台。只有后台截图明确看到某个选项处于错误状态
     * 时，才进入窄前台修正事务去点击对应 checkbox。前台修正不能串入 Alt+U、Alt+5、Alt+6，
     * 否则一个窗口会长时间占住真实输入，其他五环窗口无法并行完成后台准备。</p>
     *
     * @return true when every background probe is ready, or when a required foreground repair succeeds.
     */
    public boolean prepareTaskStartupWindowBackgroundFirst() {
        if (!botProperties.isTaskStartupPreparationEnabled()) {
            log.info("task startup background-first preparation skipped: develop switch disabled");
            return true;
        }
        StartupProbeResult mapProbe = probeStartupMapOptionsBackground();
        if (!TaskSleep.sleep(200)) {
            return false;
        }
        StartupProbeResult expandProbe = probeExpandOptionBackground();
        boolean mapReady = mapProbe.ready();
        boolean expandReady = expandProbe.ready();
        if (mapProbe.foregroundCorrectionNeeded()) {
            log.warn("task startup background-first map probe found corrective click needed: mapReady={} mapCorrection={}",
                    mapProbe.ready(), mapProbe.foregroundCorrectionNeeded());
            mapReady = repairStartupMapOptionsForeground();
        }
        if (expandProbe.foregroundCorrectionNeeded()) {
            log.warn("task startup background-first expand probe found corrective click needed: expandReady={} expandCorrection={}",
                    expandProbe.ready(), expandProbe.foregroundCorrectionNeeded());
            expandReady = repairStartupExpandOptionForeground();
        }
        boolean visibilityReady = ensureStartupVisibilityOverlays();
        boolean ready = mapReady && expandReady && visibilityReady;
        log.info("task startup background-first preparation finished: mapReady={} expandReady={} visibilityReady={} ready={}",
                mapReady, expandReady, visibilityReady, ready);
        return ready;
    }

    private boolean repairStartupMapOptionsForeground() {
        return inputSequences.submitExclusiveAndWait(
                "taskStartup:mapOptionsCorrection",
                this::ensureMapTrackingOptionDirect);
    }

    private boolean repairStartupExpandOptionForeground() {
        return inputSequences.submitExclusiveAndWait(
                "taskStartup:expandOptionCorrection",
                this::ensureExpandOptionUncheckedDirect);
    }

    /**
     * Ensure startup visibility hotkeys in the order verified by live HWND probing.
     *
     * <p>Alt+5 hides player stalls first, then Alt+6 hides nearby player overlays. Keeping this order
     * in one method prevents the narrow 五环/五倍 startup paths from drifting away from the full
     * leader preparation sequence.</p>
     *
     * @return true when both Alt+5 shopping/stall blacklist and Alt+6 player visibility confirmation
     *         templates are observed.
     */
    public boolean ensureStartupVisibilityOverlays() {
        boolean shoppingReady = ensureAlt5ShoppingBlacklist();
        boolean crowdReady = ensureAlt6Visibility();
        return shoppingReady && crowdReady;
    }

    /**
     * Press Alt+5 through the bound HWND, then verify the stall/shopping blacklist overlay.
     *
     * <p>This mirrors the existing Alt+6 startup confirmation shape but uses the newly validated
     * {@code blacklist_shopping.png} template. It remains background-HWND only; no foreground focus or
     * mouse click is needed.</p>
     *
     * @return true when the shopping blacklist state is confirmed by template.
     */
    public boolean ensureAlt5ShoppingBlacklist() {
        if (isAlt5ShoppingBlacklistConfirmed()) {
            log.info("task startup shopping blacklist: already confirmed before pressing Alt+5 template={}",
                    ALT5_SHOPPING_TEMPLATE);
            return true;
        }
        for (int attempt = 1; attempt <= STATUS_VISIBILITY_MAX_ATTEMPTS; attempt++) {
            log.info("task startup shopping blacklist: send Alt+5 to bound HWND without foreground focus attempt={}/{}",
                    attempt, STATUS_VISIBILITY_MAX_ATTEMPTS);
            BoundWindowKeyboardService.ShortcutAttempt backgroundAlt5 =
                    boundWindowKeyboardService.pressShortcut(BoundWindowKeyboardService.AltShortcut.ALT_5);
            if (!backgroundAlt5.attempted() || !backgroundAlt5.success()) {
                log.warn("task startup shopping blacklist: background Alt+5 failed attempt={}/{} attempted={} reason={}",
                        attempt, STATUS_VISIBILITY_MAX_ATTEMPTS, backgroundAlt5.attempted(), backgroundAlt5.reason());
                continue;
            }
            if (!TaskSleep.sleep(STATUS_VISIBILITY_RECHECK_DELAY_MS)) {
                return false;
            }
            if (isAlt5ShoppingBlacklistConfirmed()) {
                log.info("task startup shopping blacklist: confirmed by template after background Alt+5 attempt={}/{}",
                        attempt, STATUS_VISIBILITY_MAX_ATTEMPTS);
                log.info("task startup shopping blacklist: waiting overlay fadeout ms={}", STATUS_OVERLAY_FADEOUT_WAIT_MS);
                return TaskSleep.sleep(STATUS_OVERLAY_FADEOUT_WAIT_MS);
            }
            log.warn("task startup shopping blacklist: template confirmation failed after background Alt+5 attempt={}/{} template={}",
                    attempt, STATUS_VISIBILITY_MAX_ATTEMPTS, ALT5_SHOPPING_TEMPLATE);
        }
        log.warn("task startup shopping blacklist: Alt+5 confirmation failed after {} attempts template={}",
                STATUS_VISIBILITY_MAX_ATTEMPTS, ALT5_SHOPPING_TEMPLATE);
        return false;
    }

    /**
     * Press Alt+6 through the bound HWND, then verify the hidden-player overlay state by screenshot.
     *
     * <p>五环/五倍启动前只需要屏蔽其他玩家名字，不需要打开任何面板，也不需要鼠标点击。
     * 所以这里不走 exclusive/focus 输入事务；否则五开启动时会为了 Alt+6 把五个窗口轮流切到前台。
     * 如果 HWND 后台快捷键失败，本方法直接失败并让后续真实鼠标动作再按需抢前台。</p>
     *
     * @return true when the hidden-player state is already confirmed, or when a background Alt+6
     *         retry makes the confirmation template appear.
     */
    public boolean ensureAlt6Visibility() {
        if (isAlt6VisibilityConfirmed()) {
            log.info("task startup visibility: already confirmed before pressing Alt+6 template={}",
                    ALT6_VISIBILITY_TEMPLATE);
            return true;
        }
        for (int attempt = 1; attempt <= STATUS_VISIBILITY_MAX_ATTEMPTS; attempt++) {
            log.info("task startup visibility: send Alt+6 to bound HWND without foreground focus attempt={}/{}",
                    attempt, STATUS_VISIBILITY_MAX_ATTEMPTS);
            BoundWindowKeyboardService.ShortcutAttempt backgroundAlt6 =
                    boundWindowKeyboardService.pressShortcut(BoundWindowKeyboardService.AltShortcut.ALT_6);
            if (!backgroundAlt6.attempted() || !backgroundAlt6.success()) {
                log.warn("task startup visibility: background Alt+6 failed attempt={}/{} attempted={} reason={}",
                        attempt, STATUS_VISIBILITY_MAX_ATTEMPTS, backgroundAlt6.attempted(), backgroundAlt6.reason());
                continue;
            }
            if (!TaskSleep.sleep(STATUS_VISIBILITY_RECHECK_DELAY_MS)) {
                return false;
            }
            if (isAlt6VisibilityConfirmed()) {
                log.info("task startup visibility: confirmed by template after background Alt+6 attempt={}/{}",
                        attempt, STATUS_VISIBILITY_MAX_ATTEMPTS);
                log.info("task startup visibility: waiting overlay fadeout ms={}", STATUS_OVERLAY_FADEOUT_WAIT_MS);
                return TaskSleep.sleep(STATUS_OVERLAY_FADEOUT_WAIT_MS);
            }
            log.warn("task startup visibility: template confirmation failed after background Alt+6 attempt={}/{} template={}",
                    attempt, STATUS_VISIBILITY_MAX_ATTEMPTS, ALT6_VISIBILITY_TEMPLATE);
        }
        log.warn("task startup visibility: Alt+6 visibility confirmation failed after {} attempts template={}",
                STATUS_VISIBILITY_MAX_ATTEMPTS, ALT6_VISIBILITY_TEMPLATE);
        return false;
    }

    private StartupProbeResult probeStartupMapOptionsBackground() {
        BoundWindowKeyboardService.ShortcutAttempt open =
                boundWindowKeyboardService.pressShortcut(BoundWindowKeyboardService.AltShortcut.ALT_1);
        if (!open.attempted() || !open.success()) {
            log.warn("task startup background map probe skipped: Alt+1 failed attempted={} reason={}",
                    open.attempted(), open.reason());
            return StartupProbeResult.unknown();
        }
        if (!TaskSleep.sleep(400)) {
            closeStartupPanelBackground(BoundWindowKeyboardService.AltShortcut.ALT_1, "map-probe-interrupted");
            return StartupProbeResult.unknown();
        }
        try {
            int[] rect = coordinateHelper.getScaledRect(MAP_POPUP_RECT_X_OFFSET, MAP_POPUP_RECT_Y_OFFSET,
                    MAP_POPUP_RECT_WIDTH, MAP_POPUP_RECT_HEIGHT);
            String scanPath = windowScopedTempPath.resolve("map_startup_options_background_probe.png");
            if (!tracker.captureToFile("map startup options background probe", scanPath,
                    rect[0], rect[1], rect[2], rect[3])) {
                log.warn("task startup background map probe failed to capture option region: rect=({}, {})-({}, {})",
                        rect[0], rect[1], rect[2], rect[3]);
                return StartupProbeResult.unknown();
            }
            StartupOptionProbe tracking = probeStartupMapPanelOptionBackground(
                    "map-tracking", MAP_TRACKING_CHECKED_TEMPLATE, MAP_TRACKING_UNCHECKED_TEMPLATE,
                    rect, scanPath);
            StartupOptionProbe autoClose = probeStartupMapPanelOptionBackground(
                    "auto-close-map", AUTO_CLOSE_MAP_CHECKED_TEMPLATE, AUTO_CLOSE_MAP_UNCHECKED_TEMPLATE,
                    rect, scanPath);
            StartupOptionProbe openFly = probeStartupMapPanelOptionBackground(
                    "open-fly", OPEN_FLY_CHECKED_TEMPLATE, OPEN_FLY_UNCHECKED_TEMPLATE,
                    rect, scanPath);
            boolean ready = tracking.ready() && autoClose.ready() && openFly.ready();
            boolean correctionNeeded = tracking.foregroundCorrectionNeeded()
                    || autoClose.foregroundCorrectionNeeded()
                    || openFly.foregroundCorrectionNeeded();
            log.info("task startup background map probe result: tracking={} autoClose={} openFly={} ready={} correctionNeeded={}",
                    tracking, autoClose, openFly, ready, correctionNeeded);
            return new StartupProbeResult(ready, correctionNeeded);
        } finally {
            closeStartupPanelBackground(BoundWindowKeyboardService.AltShortcut.ALT_1, "map-probe-close");
        }
    }

    private StartupOptionProbe probeStartupMapPanelOptionBackground(String optionName,
                                                                    String checkedTemplate,
                                                                    String uncheckedTemplate,
                                                                    int[] rect,
                                                                    String scanPath) {
        Point checked = findStartupMapOptionInCapturedRegion(checkedTemplate, rect, scanPath);
        if (checked != null) {
            log.info("task startup background map probe option already checked: option={} point=({}, {})",
                    optionName, checked.x, checked.y);
            return StartupOptionProbe.ok();
        }
        Point unchecked = findStartupMapOptionInCapturedRegion(uncheckedTemplate, rect, scanPath);
        if (unchecked != null) {
            log.warn("task startup background map probe option requires foreground correction: option={} point=({}, {})",
                    optionName, unchecked.x, unchecked.y);
            return StartupOptionProbe.correction();
        }
        log.warn("task startup background map probe option unknown: option={} checked={} unchecked={}",
                optionName, checkedTemplate, uncheckedTemplate);
        return StartupOptionProbe.unknown();
    }

    private StartupProbeResult probeExpandOptionBackground() {
        BoundWindowKeyboardService.ShortcutAttempt open =
                boundWindowKeyboardService.pressShortcut(BoundWindowKeyboardService.AltShortcut.ALT_U);
        if (!open.attempted() || !open.success()) {
            log.warn("task startup background expand probe skipped: Alt+U failed attempted={} reason={}",
                    open.attempted(), open.reason());
            return StartupProbeResult.unknown();
        }
        if (!TaskSleep.sleep(400)) {
            closeStartupPanelBackground(BoundWindowKeyboardService.AltShortcut.ALT_U, "expand-probe-interrupted");
            return StartupProbeResult.unknown();
        }
        try {
            recordStartupFlyingStateFromOpenStatusPanel("background-expand-probe");
            int[] rect = coordinateHelper.getScaledRect(
                    EXPAND_OPTION_RECT_X_OFFSET,
                    EXPAND_OPTION_RECT_Y_OFFSET,
                    EXPAND_OPTION_RECT_WIDTH,
                    EXPAND_OPTION_RECT_HEIGHT);
            String scanPath = windowScopedTempPath.resolve("status_expand_option_background_probe.png");
            if (!tracker.captureToFile("status expand option background probe", scanPath,
                    rect[0], rect[1], rect[2], rect[3])) {
                log.warn("task startup background expand probe failed to capture region: rect=({}, {})-({}, {})",
                        rect[0], rect[1], rect[2], rect[3]);
                return StartupProbeResult.unknown();
            }
            Point unchecked = findTemplateInCapturedRegion(EXPAND_UNCHECKED_TEMPLATE, rect, scanPath, EXPAND_OPTION_MATCH_RATE);
            if (unchecked != null) {
                log.info("task startup background expand probe already unchecked: point=({}, {})",
                        unchecked.x, unchecked.y);
                return StartupProbeResult.ok();
            }
            Point checked = findTemplateInCapturedRegion(EXPAND_CHECKED_TEMPLATE, rect, scanPath, EXPAND_OPTION_MATCH_RATE);
            if (checked != null) {
                log.warn("task startup background expand probe requires foreground correction: point=({}, {})",
                        checked.x, checked.y);
                return StartupProbeResult.correction();
            }
            log.warn("task startup background expand probe unknown: checked={} unchecked={}",
                    EXPAND_CHECKED_TEMPLATE, EXPAND_UNCHECKED_TEMPLATE);
            return StartupProbeResult.unknown();
        } finally {
            closeStartupPanelBackground(BoundWindowKeyboardService.AltShortcut.ALT_U, "expand-probe-close");
        }
    }

    private void closeStartupPanelBackground(BoundWindowKeyboardService.AltShortcut shortcut, String source) {
        BoundWindowKeyboardService.ShortcutAttempt close = boundWindowKeyboardService.pressShortcut(shortcut);
        log.info("task startup background panel close: source={} shortcut={} attempted={} success={} reason={}",
                source, shortcut.displayName(), close.attempted(), close.success(), close.reason());
        TaskSleep.sleep(200);
    }

    /**
     * Ensure the Alt+U status panel's expand/zoom option is disabled before leader tasks start.
     *
     * <p>Flying mounts can change the visible scene scale when this option is enabled. NPC formula
     * clicks depend on stable purple-name-to-body geometry, so startup forces this option to the
     * unchecked state. The method runs only inside the startup exclusive input callback and therefore
     * uses direct input provider calls.</p>
     */
    private boolean ensureExpandOptionUncheckedDirect() {
        inputProvider.pressAltU();
        if (!TaskSleep.sleep(600)) {
            return false;
        }

        try {
            recordStartupFlyingStateFromOpenStatusPanel("foreground-expand-probe");
            int[] rect = coordinateHelper.getScaledRect(
                    EXPAND_OPTION_RECT_X_OFFSET,
                    EXPAND_OPTION_RECT_Y_OFFSET,
                    EXPAND_OPTION_RECT_WIDTH,
                    EXPAND_OPTION_RECT_HEIGHT);
            String scanPath = windowScopedTempPath.resolve("status_expand_option_scan.png");
            if (!tracker.captureToFile("status expand option", scanPath, rect[0], rect[1], rect[2], rect[3])) {
                log.warn("task startup expand option: failed to capture region rect=({}, {})-({}, {})",
                        rect[0], rect[1], rect[2], rect[3]);
                return false;
            }

            Point unchecked = findTemplateInCapturedRegion(EXPAND_UNCHECKED_TEMPLATE, rect, scanPath, EXPAND_OPTION_MATCH_RATE);
            if (unchecked != null) {
                log.info("task startup expand option: already unchecked point=({}, {})", unchecked.x, unchecked.y);
                return true;
            }

            Point checked = findTemplateInCapturedRegion(EXPAND_CHECKED_TEMPLATE, rect, scanPath, EXPAND_OPTION_MATCH_RATE);
            if (checked == null) {
                log.warn("task startup expand option: neither checked nor unchecked template found checked={} unchecked={}",
                        EXPAND_CHECKED_TEMPLATE, EXPAND_UNCHECKED_TEMPLATE);
                return false;
            }

            Point click = new Point(checked.x, checked.y);
            log.info("task startup expand option: disabling checked option click=({}, {}) matched=({}, {}) offsetX=-35",
                    click.x, click.y, checked.x, checked.y);
            inputProvider.clickLeft(click.x, click.y, 150);
            return TaskSleep.sleep(500);
        } finally {
            inputProvider.pressAltU();
        }
    }

    private void recordStartupFlyingStateFromOpenStatusPanel(String source) {
        GameStateUtil.FlyingState state = GameStateUtil.FlyingState.UNKNOWN;
        int[] rect = coordinateHelper.getScaledRect(
                FLYING_STATUS_RECT_X_OFFSET,
                FLYING_STATUS_RECT_Y_OFFSET,
                FLYING_STATUS_RECT_WIDTH,
                FLYING_STATUS_RECT_HEIGHT);
        String scanPath = windowScopedTempPath.resolve("status_flying_state_" + source + ".png");
        try {
            if (!tracker.captureToFile("status startup flying state probe", scanPath,
                    rect[0], rect[1], rect[2], rect[3])) {
                log.warn("task startup flying probe failed to capture region: source={} rect=({}, {})-({}, {})",
                        source, rect[0], rect[1], rect[2], rect[3]);
            } else if (findTemplateInCapturedRegion(FLYING_STATUS_TEMPLATE, rect, scanPath, FLYING_STATUS_MATCH_RATE) != null) {
                state = GameStateUtil.FlyingState.FLYING;
            } else if (findTemplateInCapturedRegion(UNFLYING_STATUS_TEMPLATE, rect, scanPath, FLYING_STATUS_MATCH_RATE) != null) {
                state = GameStateUtil.FlyingState.NOT_FLYING;
            }
        } catch (RuntimeException e) {
            log.warn("task startup flying probe failed; store UNKNOWN: source={} rect=({}, {})-({}, {})",
                    source, rect[0], rect[1], rect[2], rect[3], e);
            state = GameStateUtil.FlyingState.UNKNOWN;
        }
        GameStateUtil.FlyingState observed = state;
        windowTaskContextHolder.rawCurrent()
                .ifPresentOrElse(
                        runtime -> runtime.markTaskQueueStartupFlyingState(observed, "task-startup:" + source),
                        () -> log.warn("task startup flying probe has no window runtime: source={} state={}",
                                source, observed));
        log.info("task startup flying probe result: source={} state={} rect=({}, {})-({}, {}) flyingTemplate={} unflyingTemplate={}",
                source, observed, rect[0], rect[1], rect[2], rect[3],
                FLYING_STATUS_TEMPLATE, UNFLYING_STATUS_TEMPLATE);
    }

    private boolean isAlt6VisibilityConfirmed() {
        int[] rect = coordinateHelper.getScaledRect(
                STATUS_VISIBILITY_RECT_X_OFFSET,
                STATUS_VISIBILITY_RECT_Y_OFFSET,
                STATUS_VISIBILITY_RECT_WIDTH,
                STATUS_VISIBILITY_RECT_HEIGHT);
        Point matched = coordinateHelper.findImageInRegion(
                ALT6_VISIBILITY_TEMPLATE,
                rect,
                STATUS_VISIBILITY_MATCH_RATE);
        boolean confirmed = matched != null;
        log.info("task startup visibility check: confirmed={} template={} rect=({}, {})-({}, {}) match={}",
                confirmed, ALT6_VISIBILITY_TEMPLATE, rect[0], rect[1], rect[2], rect[3],
                matched == null ? "-" : matched.x + "," + matched.y);
        return confirmed;
    }

    private boolean isAlt5ShoppingBlacklistConfirmed() {
        int[] rect = coordinateHelper.getScaledRect(
                STATUS_VISIBILITY_RECT_X_OFFSET,
                STATUS_VISIBILITY_RECT_Y_OFFSET,
                STATUS_VISIBILITY_RECT_WIDTH,
                STATUS_VISIBILITY_RECT_HEIGHT);
        Point matched = coordinateHelper.findImageInRegion(
                ALT5_SHOPPING_TEMPLATE,
                rect,
                STATUS_VISIBILITY_MATCH_RATE);
        boolean confirmed = matched != null;
        log.info("task startup shopping blacklist check: confirmed={} template={} rect=({}, {})-({}, {}) match={}",
                confirmed, ALT5_SHOPPING_TEMPLATE, rect[0], rect[1], rect[2], rect[3],
                matched == null ? "-" : matched.x + "," + matched.y);
        return confirmed;
    }

    /**
     * Direct implementation of mini-map tracking setup.
     *
     * <p>The method opens the mini-map with Alt+1, searches a window-relative option area, toggles
     * required startup options when needed, and always attempts to close the map. It assumes the caller
     * already owns the input queue worker, so it uses direct {@link InputProvider} calls rather than
     * submitting nested queue requests.</p>
     */
    private boolean ensureMapTrackingOptionDirect() {
        inputProvider.pressAlt1();
        if (!TaskSleep.sleep(400)) {
            return false;
        }

        int[] rect = coordinateHelper.getScaledRect(MAP_POPUP_RECT_X_OFFSET, MAP_POPUP_RECT_Y_OFFSET,
                MAP_POPUP_RECT_WIDTH, MAP_POPUP_RECT_HEIGHT);
        String startupOptionsScanPath = windowScopedTempPath.resolve("map_startup_options_scan.png");
        if (!tracker.captureToFile("map startup options", startupOptionsScanPath, rect[0], rect[1], rect[2], rect[3])) {
            log.warn("ensureMapTrackingOption failed to capture startup option region: rect=({}, {})-({}, {})",
                    rect[0], rect[1], rect[2], rect[3]);
            inputProvider.pressAlt1();
            warnIfMapPanelStillOpenAfterAlt1Close();
            return false;
        }

        /*
         * All three options live in the same Alt+1 settings panel. Keep them in one open/close
         * sequence and one screenshot so multi-window startup cannot steal focus between option
         * toggles and template checks cannot observe different frames.
         */
        boolean trackingReady = ensureStartupMapPanelOption(
                "map-tracking",
                MAP_TRACKING_CHECKED_TEMPLATE,
                MAP_TRACKING_UNCHECKED_TEMPLATE,
                rect,
                startupOptionsScanPath);
        boolean autoCloseReady = ensureStartupMapPanelOption(
                "auto-close-map",
                AUTO_CLOSE_MAP_CHECKED_TEMPLATE,
                AUTO_CLOSE_MAP_UNCHECKED_TEMPLATE,
                rect,
                startupOptionsScanPath);
        boolean openFlyReady = ensureStartupMapPanelOption(
                "open-fly",
                OPEN_FLY_CHECKED_TEMPLATE,
                OPEN_FLY_UNCHECKED_TEMPLATE,
                rect,
                startupOptionsScanPath);

        log.info("ensureMapTrackingOption startup option check finished: tracking={} autoCloseMap={} openFly={} pressing Alt+1 to close map",
                trackingReady, autoCloseReady, openFlyReady);
        inputProvider.pressAlt1();
        warnIfMapPanelStillOpenAfterAlt1Close();
        return trackingReady && autoCloseReady && openFlyReady;
    }

    /**
     * Ensure one startup option in the Alt+1 map settings panel is enabled.
     *
     * @param optionName log label for the option being checked.
     * @param checkedTemplate template path for the already-enabled state.
     * @param uncheckedTemplate template path for the disabled state that should be clicked.
     * @param rect screen-absolute search rectangle returned by {@link CoordinateHelper#getScaledRect};
     *             it covers only the map settings option rows, not the full client.
     * @param scanPath window-scoped screenshot of {@code rect}; all startup options in the current
     *                 pass must share this same image to avoid re-capturing between template checks.
     * @return true when the option is already enabled or was clicked to enable; false when neither
     *         state template was found or the task was interrupted while waiting after the click.
     */
    private boolean ensureStartupMapPanelOption(String optionName,
                                                String checkedTemplate,
                                                String uncheckedTemplate,
                                                int[] rect,
                                                String scanPath) {
        Point checkedRes = findStartupMapOptionInCapturedRegion(checkedTemplate, rect, scanPath);
        if (checkedRes != null) {
            log.info("ensureMapTrackingOption startup option already checked: option={} point=({}, {})",
                    optionName, checkedRes.x, checkedRes.y);
            return true;
        }

        Point uncheckedRes = findStartupMapOptionInCapturedRegion(uncheckedTemplate, rect, scanPath);
        if (uncheckedRes == null) {
            log.warn("ensureMapTrackingOption startup option template missing: option={} checked={} unchecked={}",
                    optionName, checkedTemplate, uncheckedTemplate);
            return false;
        }

        /*
         * Existing checkbox templates are captured from the option text/icon area; clicking slightly
         * left of the matched point lands on the actual checkbox for all three startup options.
         */
        int clickX = uncheckedRes.x - 13;
        int clickY = uncheckedRes.y;
        log.info("ensureMapTrackingOption enabling startup option: option={} click=({}, {}) matched=({}, {})",
                optionName, clickX, clickY, uncheckedRes.x, uncheckedRes.y);
        inputProvider.clickLeft(clickX, clickY, 150);
        return TaskSleep.sleep(500);
    }

    /**
     * Match one startup option template inside the already-captured Alt+1 panel region.
     *
     * @param templatePath template to match inside {@code scanPath}.
     * @param rect screen-absolute ROI rectangle used to produce {@code scanPath}; its top-left corner
     *             is added back to the image-local match point.
     * @param scanPath window-scoped ROI image captured once by {@link #ensureMapTrackingOptionDirect()}.
     * @return screen-absolute top-left match point, or null when the template is not present.
     */
    private Point findStartupMapOptionInCapturedRegion(String templatePath, int[] rect, String scanPath) {
        return findTemplateInCapturedRegion(templatePath, rect, scanPath, MAP_STARTUP_OPTION_MATCH_RATE);
    }

    private Point findTemplateInCapturedRegion(String templatePath, int[] rect, String scanPath, double matchRate) {
        double[] imagePoint = ImageFinder.find(scanPath, templatePath, matchRate);
        if (imagePoint == null || imagePoint.length < 2) {
            return null;
        }
        int imageX = (int) Math.round(imagePoint[0]);
        int imageY = (int) Math.round(imagePoint[1]);
        Point absolutePoint = new Point(rect[0] + imageX, rect[1] + imageY);
        log.info("startup option template matched: template={} image=({}, {}) absolute=({}, {})",
                templatePath, imageX, imageY, absolutePoint.x, absolutePoint.y);
        return absolutePoint;
    }

    private void warnIfMapPanelStillOpenAfterAlt1Close() {
        if (!TaskSleep.sleep(400)) {
            return;
        }
        int[] rect = coordinateHelper.getScaledRect(MAP_POPUP_RECT_X_OFFSET, MAP_POPUP_RECT_Y_OFFSET,
                MAP_POPUP_RECT_WIDTH, MAP_POPUP_RECT_HEIGHT);
        boolean stillOpen = coordinateHelper.findImageInRegion(MAP_TRACKING_CHECKED_TEMPLATE, rect, 0.95) != null
                || coordinateHelper.findImageInRegion(MAP_TRACKING_UNCHECKED_TEMPLATE, rect, 0.95) != null;
        if (stillOpen) {
            log.warn("ensureMapTrackingOption pressed Alt+1 but map panel still appears open; later UI cleanup will handle it");
        }
    }

    private record StartupProbeResult(boolean ready, boolean foregroundCorrectionNeeded) {
        private static StartupProbeResult ok() {
            return new StartupProbeResult(true, false);
        }

        private static StartupProbeResult correction() {
            return new StartupProbeResult(false, true);
        }

        private static StartupProbeResult unknown() {
            return new StartupProbeResult(false, false);
        }
    }

    private record StartupOptionProbe(boolean ready, boolean foregroundCorrectionNeeded) {
        private static StartupOptionProbe ok() {
            return new StartupOptionProbe(true, false);
        }

        private static StartupOptionProbe correction() {
            return new StartupOptionProbe(false, true);
        }

        private static StartupOptionProbe unknown() {
            return new StartupOptionProbe(false, false);
        }
    }
}
