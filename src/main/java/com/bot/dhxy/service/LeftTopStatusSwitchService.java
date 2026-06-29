package com.bot.dhxy.service;

import com.bot.dhxy.core.GameClientTracker;
import com.bot.dhxy.core.OpenCvNativeLoader;
import com.bot.dhxy.input.InputSequences;
import com.bot.dhxy.runner.context.TaskExecutionContext;
import com.bot.dhxy.tools.CoordinateHelper;
import com.bot.dhxy.window.runtime.WindowRuntimeContext;
import com.bot.dhxy.window.runtime.WindowScopedTempPath;
import com.bot.dhxy.window.runtime.WindowTaskContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.springframework.stereotype.Service;

import java.awt.Point;
import java.util.Optional;

/**
 * Owns the small left-top status switch used only by 修罗/五倍/五环 startup and combat maintenance.
 *
 * <p>The search rectangle is window-relative pixels, while click points are converted to
 * screen-absolute pixels before entering the serialized input queue. Only an {@link SwitchState#OPEN}
 * template match is actionable; closed, unknown, or capture failures are logged and never clicked.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LeftTopStatusSwitchService {

    public static final int LEFT_TOP_STATUS_RECT_X_OFFSET = 8;
    public static final int LEFT_TOP_STATUS_RECT_Y_OFFSET = 147;
    public static final int LEFT_TOP_STATUS_RECT_WIDTH = 11;
    public static final int LEFT_TOP_STATUS_RECT_HEIGHT = 19;
    public static final String LEFT_TOP_OPEN_TEMPLATE = "images/template/status/left_top_open.png";
    public static final String LEFT_TOP_CLOSED_TEMPLATE = "images/template/status/left_top_closed.png";

    private static final double LEFT_TOP_STATUS_MATCH_RATE = 0.90;
    private static final double LEFT_TOP_STATUS_MATCH_MARGIN = 0.02;
    private static final int CLICK_SETTLE_MS = 120;
    private static final int CLICK_DELAY_MS = 250;

    private final GameClientTracker tracker;
    private final CoordinateHelper coordinateHelper;
    private final WindowScopedTempPath windowScopedTempPath;
    private final WindowTaskContextHolder windowTaskContextHolder;
    private final InputSequences inputSequences;

    /**
     * Run the leader startup close check for 修罗/五倍/五环.
     *
     * @param context current window task context, nullable only for legacy/debug calls.
     * @param taskCode task code being started; only {@code xiuluo_v2}, {@code wubei}, and
     *                 {@code wuhuan_v2} are enabled.
     * @return result of the probe/click attempt; unsupported tasks return {@link SwitchState#SKIPPED}.
     */
    public SwitchActionResult handleLeaderStartup(TaskExecutionContext context, String taskCode) {
        if (!isSupportedTaskCode(taskCode)) {
            return SwitchActionResult.skipped("unsupported-task");
        }
        SwitchActionResult result = checkAndMaybeClose(context, taskCode, "leader-startup", true);
        clearPendingIfResolved(result);
        return result;
    }

    /**
     * Probe member startup without sending mouse input.
     *
     * @param context current member auto-battle context.
     * @param requestedTaskCode original leader task code assigned to this member window.
     * @return no-click probe result; an open state marks a per-window pending close flag.
     */
    public SwitchActionResult probeMemberStartup(TaskExecutionContext context, String requestedTaskCode) {
        if (!isSupportedTaskCode(requestedTaskCode)) {
            return SwitchActionResult.skipped("unsupported-task");
        }
        SwitchActionResult result = checkAndMaybeClose(context, requestedTaskCode, "member-startup-probe", false);
        Optional<WindowRuntimeContext> runtime = windowTaskContextHolder.rawCurrent();
        if (result.state() == SwitchState.OPEN) {
            runtime.ifPresent(window -> window.markLeftTopStatusSwitchClosePending("member-startup-probe"));
        } else if (result.state() == SwitchState.CLOSED) {
            runtime.ifPresent(window -> window.clearLeftTopStatusSwitchClosePending("member-startup-closed"));
        }
        return result;
    }

    /**
     * Consume or lightly re-check the member pending close while the team pathing maintenance window is open.
     *
     * @param context current auto-battle member context.
     * @param requestedTaskCode leader task code that opened the maintenance window.
     * @return result of the safe-window probe/click attempt.
     */
    public SwitchActionResult consumeFollowerSafeWindow(TaskExecutionContext context, String requestedTaskCode) {
        if (!isSupportedTaskCode(requestedTaskCode)) {
            return SwitchActionResult.skipped("unsupported-task");
        }
        Optional<WindowRuntimeContext> runtime = windowTaskContextHolder.rawCurrent();
        boolean pending = runtime.map(WindowRuntimeContext::isLeftTopStatusSwitchClosePending).orElse(false);
        SwitchActionResult result = checkAndMaybeClose(context, requestedTaskCode, "member-team-window", true);
        if (result.state() == SwitchState.OPEN && result.clicked()) {
            runtime.ifPresent(window -> window.consumeLeftTopStatusSwitchClosePending("member-team-window-clicked"));
        } else if (result.state() == SwitchState.CLOSED) {
            runtime.ifPresent(window -> window.consumeLeftTopStatusSwitchClosePending("member-team-window-closed"));
        } else if (pending) {
            runtime.ifPresent(window -> window.markLeftTopStatusSwitchClosePending("member-team-window-still-pending"));
        }
        return result;
    }

    /**
     * Run the sparse in-combat close check for 修罗/五倍/五环 leader/member windows.
     *
     * @param context current task context.
     * @param source combat maintenance source label.
     * @return result of the probe/click attempt; unsupported tasks return {@link SwitchState#SKIPPED}.
     */
    public SwitchActionResult handleCombatMaintenance(TaskExecutionContext context, String source) {
        String taskCode = resolveTaskCode(context);
        if (!isSupportedTaskCode(taskCode)) {
            return SwitchActionResult.skipped("unsupported-task");
        }
        SwitchActionResult result = checkAndMaybeClose(context, taskCode, "combat-maintenance:" + safe(source), true);
        clearPendingIfResolved(result);
        return result;
    }

    /**
     * @param taskCode task code to test.
     * @return true only for 修罗 V2, 五倍, and 五环 V2.
     */
    public boolean isSupportedTaskCode(String taskCode) {
        return "xiuluo_v2".equalsIgnoreCase(taskCode)
                || "wubei".equalsIgnoreCase(taskCode)
                || "wuhuan_v2".equalsIgnoreCase(taskCode);
    }

    private SwitchActionResult checkAndMaybeClose(TaskExecutionContext context,
                                                  String taskCode,
                                                  String source,
                                                  boolean allowClick) {
        DetectionResult detection = detect(source);
        String windowId = context == null ? "-" : context.getWindowId();
        String role = context == null ? "-" : context.getWindowRole();
        log.info("[left-top-status] probe source={} task={} role={} windowId={} state={} openScore={} closedScore={} rect={} match={} clickAllowed={}",
                source, taskCode, role, windowId, detection.state(), detection.openScore(), detection.closedScore(),
                formatRect(detection.rect()), formatPoint(detection.openCenter()), allowClick);

        if (detection.state() != SwitchState.OPEN || !allowClick || detection.openCenter() == null) {
            return SwitchActionResult.fromDetection(detection, false);
        }

        Point click = detection.openCenter();
        boolean clicked = inputSequences.moveAndClickLeft(
                "leftTopStatusSwitch:" + safe(source),
                click.x,
                click.y,
                CLICK_SETTLE_MS,
                CLICK_DELAY_MS);
        log.info("[left-top-status] close click submitted source={} task={} role={} windowId={} clicked={} point=({}, {})",
                source, taskCode, role, windowId, clicked, click.x, click.y);
        return SwitchActionResult.fromDetection(detection, clicked);
    }

    private DetectionResult detect(String source) {
        int[] rect = coordinateHelper.getScaledRect(
                LEFT_TOP_STATUS_RECT_X_OFFSET,
                LEFT_TOP_STATUS_RECT_Y_OFFSET,
                LEFT_TOP_STATUS_RECT_WIDTH,
                LEFT_TOP_STATUS_RECT_HEIGHT);
        String scanPath = windowScopedTempPath.resolve("left_top_status_switch_" + safe(source) + ".png");
        boolean captured = tracker.captureToFile("left top status switch " + source,
                scanPath, rect[0], rect[1], rect[2], rect[3]);
        if (!captured) {
            return new DetectionResult(SwitchState.CAPTURE_FAILED, rect, -1.0, -1.0, null, scanPath);
        }

        TemplateScore open = scoreTemplate(scanPath, LEFT_TOP_OPEN_TEMPLATE, rect);
        TemplateScore closed = scoreTemplate(scanPath, LEFT_TOP_CLOSED_TEMPLATE, rect);
        SwitchState state = resolveState(open.score(), closed.score());
        Point openCenter = state == SwitchState.OPEN ? open.center() : null;
        return new DetectionResult(state, rect, open.score(), closed.score(), openCenter, scanPath);
    }

    private TemplateScore scoreTemplate(String scanPath, String templatePath, int[] rect) {
        OpenCvNativeLoader.ensureLoaded();
        Mat source = Imgcodecs.imread(scanPath, Imgcodecs.IMREAD_COLOR);
        Mat template = Imgcodecs.imread(templatePath, Imgcodecs.IMREAD_COLOR);
        try {
            if (source.empty() || template.empty()
                    || source.width() < template.width()
                    || source.height() < template.height()) {
                return new TemplateScore(-1.0, null);
            }
            Mat result = new Mat();
            try {
                Imgproc.matchTemplate(source, template, result, Imgproc.TM_CCOEFF_NORMED);
                Core.MinMaxLocResult minMax = Core.minMaxLoc(result);
                int centerX = rect[0] + (int) Math.round(minMax.maxLoc.x + template.width() / 2.0);
                int centerY = rect[1] + (int) Math.round(minMax.maxLoc.y + template.height() / 2.0);
                return new TemplateScore(minMax.maxVal, new Point(centerX, centerY));
            } finally {
                result.release();
            }
        } finally {
            source.release();
            template.release();
        }
    }

    private SwitchState resolveState(double openScore, double closedScore) {
        if (openScore >= LEFT_TOP_STATUS_MATCH_RATE
                && openScore >= closedScore + LEFT_TOP_STATUS_MATCH_MARGIN) {
            return SwitchState.OPEN;
        }
        if (closedScore >= LEFT_TOP_STATUS_MATCH_RATE
                && closedScore > openScore) {
            return SwitchState.CLOSED;
        }
        return SwitchState.UNKNOWN;
    }

    private void clearPendingIfResolved(SwitchActionResult result) {
        if (result.state() == SwitchState.OPEN && result.clicked()
                || result.state() == SwitchState.CLOSED) {
            windowTaskContextHolder.rawCurrent()
                    .ifPresent(window -> window.consumeLeftTopStatusSwitchClosePending("left-top-status-resolved"));
        }
    }

    private String resolveTaskCode(TaskExecutionContext context) {
        if (context == null) {
            return null;
        }
        if (context.getRequestedTaskCode() != null && !context.getRequestedTaskCode().isBlank()) {
            return context.getRequestedTaskCode();
        }
        return context.getTaskCode();
    }

    private static String safe(String value) {
        if (value == null || value.isBlank()) {
            return "unknown";
        }
        return value.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private static String formatRect(int[] rect) {
        if (rect == null || rect.length < 4) {
            return "-";
        }
        return rect[0] + "," + rect[1] + " -> " + rect[2] + "," + rect[3];
    }

    private static String formatPoint(Point point) {
        return point == null ? "-" : point.x + "," + point.y;
    }

    public enum SwitchState {
        OPEN,
        CLOSED,
        UNKNOWN,
        CAPTURE_FAILED,
        SKIPPED
    }

    public record SwitchActionResult(SwitchState state,
                                     boolean clicked,
                                     double openScore,
                                     double closedScore,
                                     Point openCenter,
                                     String rawPath) {

        static SwitchActionResult skipped(String reason) {
            return new SwitchActionResult(SwitchState.SKIPPED, false, -1.0, -1.0, null, reason);
        }

        static SwitchActionResult fromDetection(DetectionResult detection, boolean clicked) {
            return new SwitchActionResult(detection.state(), clicked, detection.openScore(), detection.closedScore(),
                    detection.openCenter(), detection.rawPath());
        }
    }

    private record DetectionResult(SwitchState state,
                                   int[] rect,
                                   double openScore,
                                   double closedScore,
                                   Point openCenter,
                                   String rawPath) {
    }

    private record TemplateScore(double score, Point center) {
    }
}
