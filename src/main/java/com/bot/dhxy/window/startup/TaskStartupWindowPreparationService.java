package com.bot.dhxy.window.startup;

import com.bot.dhxy.core.GameClientTracker;
import com.bot.dhxy.core.ImageFinder;
import com.bot.dhxy.config.BotProperties;
import com.bot.dhxy.input.InputProvider;
import com.bot.dhxy.input.InputSequences;
import com.bot.dhxy.runner.stop.TaskSleep;
import com.bot.dhxy.tools.CoordinateHelper;
import com.bot.dhxy.window.runtime.WindowScopedTempPath;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.awt.Point;

/**
 * Runs the focused UI checks needed before a leader window starts a task.
 *
 * <p>This service owns startup-only window preparation such as Alt+1 map option checks and Alt+6
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
    private static final String ALT6_VISIBILITY_TEMPLATE = "images/template/status/blacklist_crowd.png";
    private static final int ALT6_VISIBILITY_RECT_X_OFFSET = 359;
    private static final int ALT6_VISIBILITY_RECT_Y_OFFSET = 271;
    private static final int ALT6_VISIBILITY_RECT_WIDTH = 317;
    private static final int ALT6_VISIBILITY_RECT_HEIGHT = 288;
    private static final double ALT6_VISIBILITY_MATCH_RATE = 0.85;
    private static final int ALT6_VISIBILITY_MAX_ATTEMPTS = 3;
    private static final long ALT6_VISIBILITY_RECHECK_DELAY_MS = 500L;
    private static final long ALT6_OVERLAY_FADEOUT_WAIT_MS = 1000L;
    private static final String EXPAND_CHECKED_TEMPLATE = "images/template/status/expand_checked.png";
    private static final String EXPAND_UNCHECKED_TEMPLATE = "images/template/status/expand_unchecked.png";
    private static final int EXPAND_OPTION_RECT_X_OFFSET = 346;
    private static final int EXPAND_OPTION_RECT_Y_OFFSET = 587;
    private static final int EXPAND_OPTION_RECT_WIDTH = 106;
    private static final int EXPAND_OPTION_RECT_HEIGHT = 23;
    private static final double EXPAND_OPTION_MATCH_RATE = 0.95;

    private final GameClientTracker tracker;
    private final InputProvider inputProvider;
    private final InputSequences inputSequences;
    private final CoordinateHelper coordinateHelper;
    private final WindowScopedTempPath windowScopedTempPath;
    private final BotProperties botProperties;

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
     * Run the leader task startup visibility preparation.
     *
     * <p>This performs mini-map tracking setup and Alt+6 visibility setup in one exclusive input
     * section. The final wait lets the game's "hide players" overlay fade out before later dialog
     * detection captures screenshots.</p>
     *
     * @return true when both map tracking and Alt+6 visibility are confirmed.
     */
    public boolean prepareTaskStartupWindow() {
        if (!botProperties.isTaskStartupPreparationEnabled()) {
            log.info("task startup preparation skipped: develop switch disabled");
            return true;
        }
        return inputSequences.submitExclusiveAndWait("taskStartup:mapTrackingAndAlt6", () -> {
            boolean mapReady = ensureMapTrackingOptionDirect();
            if (!TaskSleep.sleep(200)) {
                return false;
            }
            boolean expandReady = ensureExpandOptionUncheckedDirect();
            if (!TaskSleep.sleep(200)) {
                return false;
            }
            boolean visibilityReady = ensureAlt6VisibilityDirect();
            if (visibilityReady) {
                log.info("task startup visibility: waiting overlay fadeout ms={}", ALT6_OVERLAY_FADEOUT_WAIT_MS);
                if (!TaskSleep.sleep(ALT6_OVERLAY_FADEOUT_WAIT_MS)) {
                    return false;
                }
            }
            return mapReady && expandReady && visibilityReady;
        });
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
        if (!TaskSleep.sleep(400)) {
            return false;
        }

        try {
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

            Point click = new Point(checked.x - 35, checked.y);
            log.info("task startup expand option: disabling checked option click=({}, {}) matched=({}, {}) offsetX=-35",
                    click.x, click.y, checked.x, checked.y);
            inputProvider.clickLeft(click.x, click.y, 150);
            return TaskSleep.sleep(500);
        } finally {
            inputProvider.pressAltU();
        }
    }

    /**
     * Confirm the Alt+6 player-visibility state and press Alt+6 until the configured template appears.
     *
     * <p>This method is called only from an exclusive input callback; it uses direct
     * {@link InputProvider} calls to avoid queue-in-queue deadlock.</p>
     */
    private boolean ensureAlt6VisibilityDirect() {
        if (isAlt6VisibilityConfirmed()) {
            log.info("task startup visibility: already confirmed by template={}", ALT6_VISIBILITY_TEMPLATE);
            return true;
        }

        for (int attempt = 1; attempt <= ALT6_VISIBILITY_MAX_ATTEMPTS; attempt++) {
            log.info("task startup visibility: press Alt+6 attempt={}/{}",
                    attempt, ALT6_VISIBILITY_MAX_ATTEMPTS);
            inputProvider.pressAlt6();
            if (!TaskSleep.sleep(ALT6_VISIBILITY_RECHECK_DELAY_MS)) {
                return false;
            }
            if (isAlt6VisibilityConfirmed()) {
                log.info("task startup visibility: confirmed after Alt+6 attempt={}", attempt);
                return true;
            }
        }

        log.warn("task startup visibility: template confirmation failed after {} attempts template={}",
                ALT6_VISIBILITY_MAX_ATTEMPTS, ALT6_VISIBILITY_TEMPLATE);
        return false;
    }

    private boolean isAlt6VisibilityConfirmed() {
        int[] rect = coordinateHelper.getScaledRect(
                ALT6_VISIBILITY_RECT_X_OFFSET,
                ALT6_VISIBILITY_RECT_Y_OFFSET,
                ALT6_VISIBILITY_RECT_WIDTH,
                ALT6_VISIBILITY_RECT_HEIGHT);
        Point matched = coordinateHelper.findImageInRegion(
                ALT6_VISIBILITY_TEMPLATE,
                rect,
                ALT6_VISIBILITY_MATCH_RATE);
        boolean confirmed = matched != null;
        log.info("task startup visibility check: confirmed={} template={} rect=({}, {})-({}, {}) match={}",
                confirmed, ALT6_VISIBILITY_TEMPLATE, rect[0], rect[1], rect[2], rect[3],
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
}
