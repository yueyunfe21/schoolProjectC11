package com.bot.dhxy.service;

import com.bot.dhxy.config.BotProperties;
import com.bot.dhxy.core.GameClientTracker;
import com.bot.dhxy.core.ImageFinder;
import com.bot.dhxy.input.InputSequences;
import com.bot.dhxy.input.action.InputAction;
import com.bot.dhxy.runner.context.TaskExecutionContext;
import com.bot.dhxy.runner.stop.TaskSleep;
import com.bot.dhxy.tools.CoordinateHelper;
import com.bot.dhxy.window.model.WindowPathingIntent;
import com.bot.dhxy.window.model.WindowPathingSnapshot;
import com.bot.dhxy.window.runtime.WindowRuntimeContext;
import com.bot.dhxy.window.runtime.WindowTitleIdentity;
import com.bot.dhxy.window.runtime.WindowTitleIdentityParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Detects and handles the in-game "return to team" signal after a member leaves the team.
 *
 * <p>Members may click the return button when they see it. Leaders do not click this signal; after
 * confirmed return-to-town flows they wait for the signal to disappear so dead members have time to
 * rejoin. Template matching is screenshot-only, while clicking is submitted through the global input
 * queue.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TeamReturnService {
    private static final String MEMBER_RETURN_BUTTON_PATH = "images/template/status/gui.png";
    private static final String LEADER_RETURN_SIGNAL_PATH = "images/template/status/zhao.png";
    private static final long DEFAULT_LEADER_WAIT_TIMEOUT_MS = 120_000L;
    private static final long DEFAULT_LEADER_WAIT_POLL_MS = 3_000L;
    private static final long NO_MATCH_LOG_INTERVAL_MS = 10_000L;

    private final CoordinateHelper coordinateHelper;
    private final InputSequences inputSequences;
    private final BotProperties botProperties;
    private final PlayerStateService playerStateService;
    @Lazy
    private final GameClientTracker tracker;
    private final Map<String, Long> lastNoMatchLogAtByWindow = new ConcurrentHashMap<>();
    private final Map<String, Long> lastReturnButtonFoundAtByWindow = new ConcurrentHashMap<>();
    private final Map<String, Long> lastReturnButtonClickedAtByWindow = new ConcurrentHashMap<>();

    /**
     * Click the return-team button if it is visible for this window.
     *
     * @param context current window execution context; used for logs and stop ownership.
     * @param source caller label for logs.
     * @return true if the button was found and a click was queued; false when no signal is visible.
     */
    public boolean clickReturnTeamIfPresent(TaskExecutionContext context, String source) {
        Point buttonPoint = findReturnTeamButton();
        if (buttonPoint == null) {
            logReturnButtonNoMatch(context, source);
            return false;
        }
        lastReturnButtonFoundAtByWindow.put(windowKey(context), System.currentTimeMillis());

        log.info("{} team return: return button found by {}, ensure sheyaoxiang before clicking return",
                context.getLogPrefix(), source);
        boolean incenseUsed = playerStateService.ensureSheYaoXiangActive(context);
        Point refreshedButtonPoint = findReturnTeamButton();
        if (refreshedButtonPoint == null) {
            log.warn("{} team return: return button disappeared after sheyaoxiang check source={} incenseUsed={}",
                    context.getLogPrefix(), source, incenseUsed);
            return false;
        }

        Point clickPoint = coordinateHelper.getRandomizedPoint(refreshedButtonPoint, 3, 3);
        log.info("{} team return: return button ready after sheyaoxiang check source={} incenseUsed={} click=({}, {})",
                context.getLogPrefix(), source, incenseUsed, clickPoint.x, clickPoint.y);
        inputSequences.submitAndWait("teamReturn:" + source, List.of(
                InputAction.clickLeft(clickPoint.x, clickPoint.y, 150),
                InputAction.sleep(500)
        ));
        lastReturnButtonClickedAtByWindow.put(windowKey(context), System.currentTimeMillis());
        return true;
    }

    /**
     * Leader-side wait when a return-team signal is visible after returning to town.
     *
     * @param context current leader execution context. Stop requests are honored during polling.
     * @param source caller label for logs.
     * @return false when there was no signal and no wait was needed; true after the signal disappears
     * or the configured timeout expires.
     */
    public boolean waitForMembersReturnIfNeeded(TaskExecutionContext context, String source) {
        if (!isReturnTeamSignalPresent()) {
            return false;
        }

        long timeoutMs = leaderWaitTimeoutMs();
        long pollMs = leaderWaitPollMs();
        long deadlineAtMs = System.currentTimeMillis() + timeoutMs;
        log.warn("{} team return: leader sees return signal after {}, wait up to {} ms for member return",
                context.getLogPrefix(), source, timeoutMs);

        while (System.currentTimeMillis() < deadlineAtMs) {
            context.throwIfStopRequested();
            TaskSleep.sleep(pollMs);
            if (!isReturnTeamSignalPresent()) {
                log.info("{} team return: return signal disappeared, continue leader task",
                        context.getLogPrefix());
                return true;
            }
        }

        log.warn("{} team return: return signal still present after {} ms, let task-specific flow continue",
                context.getLogPrefix(), timeoutMs);
        return true;
    }

    /**
     * Check whether the leader-side return-team signal is visible.
     *
     * <p>Members click the {@code 归} button, but leaders wait on the teammate status marker in the
     * same right-side team area. The leader marker currently uses the {@code 招} template because
     * that is what appears in the leader's team-status panel when a member needs to return.</p>
     *
     * @return true when the leader signal template is found inside the configured team-status area.
     */
    public boolean isReturnTeamSignalPresent() {
        return findLeaderReturnSignal() != null;
    }

    /**
     * Capture the leader-side team-return area before the task opens the bag, then analyze that
     * immutable image in the background while the return item flow continues. The returned handle is
     * read-only: it never sends input, changes focus, or captures another screenshot.
     *
     * @param context current leader execution context, used to scope the precheck result.
     * @param source caller label for diagnostics.
     * @return scoped precheck handle. Failed captures return a completed failed handle so callers can
     *         fall back to the normal live detector.
     */
    public LeaderSignalPrecheck beginLeaderSignalPrecheck(TaskExecutionContext context, String source) {
        String safeSource = source == null || source.isBlank() ? "unknown" : source;
        int[] rect = leaderReturnSignalRect();
        BufferedImage snapshot = tracker.captureToMemory("team-return-precheck:" + safeSource,
                rect[0], rect[1], rect[2], rect[3]);
        LeaderSignalScope scope = LeaderSignalScope.from(context, safeSource, System.currentTimeMillis());
        if (snapshot == null) {
            log.warn("{} team return precheck capture failed before bag: source={} rect=({}, {})-({}, {})",
                    logPrefix(context), safeSource, rect[0], rect[1], rect[2], rect[3]);
            return LeaderSignalPrecheck.completed(scope, LeaderSignalPrecheckResult.failed("capture-failed"));
        }

        CompletableFuture<LeaderSignalPrecheckResult> future = CompletableFuture.supplyAsync(() ->
                analyzeLeaderSignalSnapshot(snapshot, rect, safeSource));
        log.info("{} team return precheck captured before bag: source={} size={}x{} rect=({}, {})-({}, {})",
                logPrefix(context), safeSource, snapshot.getWidth(), snapshot.getHeight(),
                rect[0], rect[1], rect[2], rect[3]);
        return new LeaderSignalPrecheck(scope, future);
    }

    /**
     * Consume a scoped pre-return screenshot analysis if it is complete and belongs to the same task
     * run/window. Stale, failed, or unfinished handles are intentionally treated as inconclusive.
     *
     * @param context current leader context.
     * @param precheck handle returned by {@link #beginLeaderSignalPrecheck(TaskExecutionContext, String)}.
     * @param source caller label for logs.
     * @return conclusive precheck status or an inconclusive status that tells the caller to fall back.
     */
    public LeaderSignalPrecheckStatus consumeLeaderSignalPrecheck(TaskExecutionContext context,
                                                                  LeaderSignalPrecheck precheck,
                                                                  String source) {
        String safeSource = source == null || source.isBlank() ? "unknown" : source;
        if (precheck == null) {
            return LeaderSignalPrecheckStatus.inconclusive("missing");
        }
        if (!precheck.scope().matches(context)) {
            log.warn("{} team return precheck stale: source={} capturedSource={} capturedWindow={} currentWindow={} capturedRun={} currentRun={}",
                    logPrefix(context), safeSource, precheck.scope().source(), precheck.scope().windowId(),
                    context == null ? null : context.getWindowId(), precheck.scope().taskRunId(),
                    context == null ? 0L : context.getTaskRunId());
            return LeaderSignalPrecheckStatus.inconclusive("stale");
        }
        if (!precheck.future().isDone()) {
            log.info("{} team return precheck not ready; fall back to live detection: source={} capturedAtMs={}",
                    logPrefix(context), safeSource, precheck.scope().capturedAtMs());
            return LeaderSignalPrecheckStatus.inconclusive("not-ready");
        }
        try {
            LeaderSignalPrecheckResult result = precheck.future().getNow(
                    LeaderSignalPrecheckResult.failed("not-ready"));
            if (result.status() == LeaderSignalPrecheckResultStatus.FAILED) {
                log.warn("{} team return precheck failed; fall back to live detection: source={} reason={}",
                        logPrefix(context), safeSource, result.reason());
                return LeaderSignalPrecheckStatus.inconclusive(result.reason());
            }
            log.info("{} team return precheck consumed: source={} status={} point=({}, {}) reason={}",
                    logPrefix(context), safeSource, result.status(), result.absoluteX(), result.absoluteY(),
                    result.reason());
            return result.status() == LeaderSignalPrecheckResultStatus.SIGNAL_PRESENT
                    ? LeaderSignalPrecheckStatus.withSignal()
                    : LeaderSignalPrecheckStatus.noSignal();
        } catch (Exception e) {
            log.warn("{} team return precheck consume failed; fall back to live detection: source={} reason={}",
                    logPrefix(context), safeSource, e.getMessage(), e);
            return LeaderSignalPrecheckStatus.inconclusive("consume-error");
        }
    }

    /**
     * Template-match the return-team button in the configured client area.
     *
     * @return screen-absolute center point returned by {@link CoordinateHelper}, or null when the
     * template is not present.
     */
    private Point findReturnTeamButton() {
        int[] rect = coordinateHelper.getScaledRect(
                botProperties.getReturnTeamAreaX(),
                botProperties.getReturnTeamAreaY(),
                botProperties.getReturnTeamAreaW(),
                botProperties.getReturnTeamAreaH()
        );
        return coordinateHelper.findImageInRegion(MEMBER_RETURN_BUTTON_PATH, rect, botProperties.getReturnTeamMatchRate());
    }

    private void logReturnButtonNoMatch(TaskExecutionContext context, String source) {
        String windowKey = windowKey(context);
        long now = System.currentTimeMillis();
        Long lastLogAt = lastNoMatchLogAtByWindow.get(windowKey);
        if (lastLogAt != null && now - lastLogAt < NO_MATCH_LOG_INTERVAL_MS) {
            return;
        }
        lastNoMatchLogAtByWindow.put(windowKey, now);
        int[] rect = coordinateHelper.getScaledRect(
                botProperties.getReturnTeamAreaX(),
                botProperties.getReturnTeamAreaY(),
                botProperties.getReturnTeamAreaW(),
                botProperties.getReturnTeamAreaH()
        );
        boolean currentWindowReturnMarkerPresent = isReturnTeamSignalPresent();
        ReturnButtonNoMatchScan memberScan = scanReturnButtonNoMatch(rect, source);
        Long lastFoundAt = lastReturnButtonFoundAtByWindow.get(windowKey);
        Long lastClickedAt = lastReturnButtonClickedAtByWindow.get(windowKey);
        WindowTitleIdentity titleIdentity = parseNativeTitle(context);
        log.info("{} team return: return button not found source={} task={} requested={} windowId={} role={} hwnd={} nativeTitle={} player={}/{} server={} localSession={} localSupportMember={} leaderPresent={} leaderWindow={} currentWindowReturnMarkerPresent={} runtime={} memberScanCapture={} memberScanElement={} memberScanSize={} bestScore={} bestPoint=({}, {}) bestRect={} area=({}, {}) {}x{} template={} threshold={} lastFoundAgeMs={} lastClickedAgeMs={}",
                logPrefix(context), source,
                context == null ? null : context.getTaskCode(),
                context == null ? null : context.getRequestedTaskCode(),
                context == null ? null : context.getWindowId(),
                context == null ? null : context.getWindowRole(),
                context == null ? null : context.getNativeWindowHandle(),
                context == null ? null : context.getNativeWindowTitle(),
                titleIdentity == null ? null : titleIdentity.playerName(),
                titleIdentity == null ? null : titleIdentity.playerId(),
                titleIdentity == null ? null : titleIdentity.server(),
                context == null ? null : context.getLocalTeamSessionKey(),
                context != null && context.isLocalSupportMember(),
                context != null && context.isLocalLeaderPresent(),
                context == null ? null : context.getLocalLeaderWindowId(),
                currentWindowReturnMarkerPresent,
                runtimeStateText(context),
                memberScan.captureStatus(), memberScan.elementName(), memberScan.imageSizeText(),
                memberScan.bestScore(), memberScan.bestPointX(), memberScan.bestPointY(),
                memberScan.bestRectText(),
                rect[0], rect[1], rect[2], rect[3],
                MEMBER_RETURN_BUTTON_PATH, botProperties.getReturnTeamMatchRate(),
                lastFoundAt == null ? -1L : now - lastFoundAt,
                lastClickedAt == null ? -1L : now - lastClickedAt);
    }

    private ReturnButtonNoMatchScan scanReturnButtonNoMatch(int[] rect, String source) {
        String elementName = "team-return-no-match-member:" + safeSource(source);
        BufferedImage snapshot = tracker.captureToMemory(elementName, rect[0], rect[1], rect[2], rect[3]);
        if (snapshot == null) {
            return ReturnButtonNoMatchScan.captureFailed(elementName);
        }
        try {
            BufferedImage template = ImageIO.read(Path.of(MEMBER_RETURN_BUTTON_PATH).toFile());
            double[] best = ImageFinder.find(snapshot, template, -1.0);
            if (best == null || best.length < 3 || template == null) {
                return ReturnButtonNoMatchScan.capturedNoBest(
                        elementName, snapshot.getWidth(), snapshot.getHeight());
            }
            Point bestPoint = coordinateHelper.resolveMatchedPointInRect(rect, best);
            String bestRect = bestPoint == null
                    ? "-"
                    : bestRectText(bestPoint, template.getWidth(), template.getHeight());
            return new ReturnButtonNoMatchScan(
                    "memory", elementName, snapshot.getWidth(), snapshot.getHeight(),
                    best[2], bestPoint == null ? -1 : bestPoint.x, bestPoint == null ? -1 : bestPoint.y,
                    bestRect);
        } catch (Exception e) {
            log.warn("team return no-match debug scan failed: source={} element={} template={} reason={}",
                    source, elementName, MEMBER_RETURN_BUTTON_PATH, e.getMessage(), e);
            return ReturnButtonNoMatchScan.analysisFailed(elementName, snapshot.getWidth(), snapshot.getHeight());
        }
    }

    private String bestRectText(Point center, int width, int height) {
        int left = center.x - width / 2;
        int top = center.y - height / 2;
        return left + "," + top + " " + width + "x" + height;
    }

    private WindowTitleIdentity parseNativeTitle(TaskExecutionContext context) {
        if (context == null) {
            return null;
        }
        return WindowTitleIdentityParser.parse(context.getNativeWindowTitle()).orElse(null);
    }

    private String runtimeStateText(TaskExecutionContext context) {
        if (context == null || context.getWindowRuntimeContext() == null) {
            return "-";
        }
        WindowRuntimeContext runtime = context.getWindowRuntimeContext();
        WindowPathingSnapshot pathing = runtime.getPathingSnapshot();
        String pathingText = pathing == null ? "pathing=null" : pathingText(pathing);
        String actionText = runtime.getGameState() == null ? "action=null"
                : "action=" + runtime.getGameState().getCurrentActionState();
        String mapText = runtime.getGameState() == null || runtime.getGameState().getMe() == null
                ? "map=null"
                : "map=" + runtime.getGameState().getMe().getCurrentMapName()
                + "@" + runtime.getGameState().getMe().getX()
                + "," + runtime.getGameState().getMe().getY();
        return "status=" + runtime.getStatus()
                + "," + actionText
                + "," + mapText
                + "," + pathingText
                + ",identitySuspended=" + runtime.isIdentitySuspended()
                + ",ownerPlayerId=" + runtime.getTaskOwnerPlayerId()
                + ",visiblePlayerId=" + runtime.getVisiblePlayerId();
    }

    private String pathingText(WindowPathingSnapshot pathing) {
        WindowPathingIntent intent = pathing.getIntent();
        return "pathing=" + pathing.getState()
                + ",pathingTarget=" + (intent == null ? null : intent.getTargetMapName())
                + ",pathingSource=" + (intent == null ? null : intent.getSource())
                + ",pathingMessage=" + pathing.getMessage();
    }

    private String safeSource(String source) {
        if (source == null || source.isBlank()) {
            return "unknown";
        }
        return source.replaceAll("[^a-zA-Z0-9_.:-]", "_");
    }

    /**
     * Template-match the leader-side team-return signal in the configured client area.
     *
     * @return screen-absolute center point for the signal, or null when no waiting member marker is
     * visible. This method is screenshot-only and never sends input.
     */
    private Point findLeaderReturnSignal() {
        int[] rect = leaderReturnSignalRect();
        return coordinateHelper.findImageInRegion(LEADER_RETURN_SIGNAL_PATH, rect, botProperties.getReturnTeamMatchRate());
    }

    private int[] leaderReturnSignalRect() {
        return coordinateHelper.getScaledRect(
                botProperties.getReturnTeamAreaX(),
                botProperties.getReturnTeamAreaY(),
                botProperties.getReturnTeamAreaW(),
                botProperties.getReturnTeamAreaH()
        );
    }

    private LeaderSignalPrecheckResult analyzeLeaderSignalSnapshot(BufferedImage snapshot, int[] rect, String source) {
        try {
            BufferedImage template = ImageIO.read(Path.of(LEADER_RETURN_SIGNAL_PATH).toFile());
            double[] match = ImageFinder.find(snapshot, template, botProperties.getReturnTeamMatchRate());
            if (match == null || match.length < 2) {
                return LeaderSignalPrecheckResult.noSignal();
            }
            int absoluteX = rect[0] + (int) Math.round(match[0]);
            int absoluteY = rect[1] + (int) Math.round(match[1]);
            log.info("team return precheck signal matched: source={} click=({}, {}) score={}",
                    source, absoluteX, absoluteY, match.length >= 3 ? match[2] : -1);
            return LeaderSignalPrecheckResult.signalPresent(absoluteX, absoluteY);
        } catch (Exception e) {
            log.warn("team return precheck analysis failed: source={} reason={}", source, e.getMessage(), e);
            return LeaderSignalPrecheckResult.failed(e.getClass().getSimpleName());
        }
    }

    private String logPrefix(TaskExecutionContext context) {
        return context == null ? "[window=unknown]" : context.getLogPrefix();
    }

    private String windowKey(TaskExecutionContext context) {
        if (context != null && context.getWindowId() != null && !context.getWindowId().isBlank()) {
            return context.getWindowId();
        }
        return "default";
    }

    /**
     * @return configured leader wait timeout in milliseconds, or the production default.
     */
    private long leaderWaitTimeoutMs() {
        long configured = botProperties.getReturnTeamLeaderWaitTimeoutMs();
        return configured > 0 ? configured : DEFAULT_LEADER_WAIT_TIMEOUT_MS;
    }

    /**
     * @return configured signal polling interval in milliseconds, or the production default.
     */
    private long leaderWaitPollMs() {
        long configured = botProperties.getReturnTeamLeaderWaitPollMs();
        return configured > 0 ? configured : DEFAULT_LEADER_WAIT_POLL_MS;
    }

    public enum LeaderSignalPrecheckResultStatus {
        NO_SIGNAL,
        SIGNAL_PRESENT,
        FAILED
    }

    public record LeaderSignalPrecheck(LeaderSignalScope scope,
                                       CompletableFuture<LeaderSignalPrecheckResult> future) {
        private static LeaderSignalPrecheck completed(LeaderSignalScope scope, LeaderSignalPrecheckResult result) {
            return new LeaderSignalPrecheck(scope, CompletableFuture.completedFuture(result));
        }
    }

    public record LeaderSignalPrecheckStatus(boolean conclusive,
                                             boolean signalPresent,
                                             String reason) {
        public static LeaderSignalPrecheckStatus noSignal() {
            return new LeaderSignalPrecheckStatus(true, false, "no-signal");
        }

        public static LeaderSignalPrecheckStatus withSignal() {
            return new LeaderSignalPrecheckStatus(true, true, "signal-present");
        }

        public static LeaderSignalPrecheckStatus inconclusive(String reason) {
            return new LeaderSignalPrecheckStatus(false, false, reason);
        }
    }

    private record LeaderSignalPrecheckResult(LeaderSignalPrecheckResultStatus status,
                                              int absoluteX,
                                              int absoluteY,
                                              String reason) {
        private static LeaderSignalPrecheckResult noSignal() {
            return new LeaderSignalPrecheckResult(LeaderSignalPrecheckResultStatus.NO_SIGNAL, -1, -1, "no-signal");
        }

        private static LeaderSignalPrecheckResult signalPresent(int absoluteX, int absoluteY) {
            return new LeaderSignalPrecheckResult(LeaderSignalPrecheckResultStatus.SIGNAL_PRESENT,
                    absoluteX, absoluteY, "signal-present");
        }

        private static LeaderSignalPrecheckResult failed(String reason) {
            return new LeaderSignalPrecheckResult(LeaderSignalPrecheckResultStatus.FAILED, -1, -1, reason);
        }
    }

    private record LeaderSignalScope(String windowId,
                                     String nativeWindowHandle,
                                     long taskRunId,
                                     String source,
                                     long capturedAtMs) {
        private static LeaderSignalScope from(TaskExecutionContext context, String source, long capturedAtMs) {
            return new LeaderSignalScope(
                    context == null ? null : context.getWindowId(),
                    context == null ? null : context.getNativeWindowHandle(),
                    context == null ? 0L : context.getTaskRunId(),
                    source,
                    capturedAtMs);
        }

        private boolean matches(TaskExecutionContext context) {
            if (context == null) {
                return windowId == null && nativeWindowHandle == null && taskRunId == 0L;
            }
            return equalsNullable(windowId, context.getWindowId())
                    && equalsNullable(nativeWindowHandle, context.getNativeWindowHandle())
                    && taskRunId == context.getTaskRunId();
        }

        private boolean equalsNullable(String left, String right) {
            if (left == null || left.isBlank()) {
                return right == null || right.isBlank();
            }
            return left.equals(right);
        }
    }

    private record ReturnButtonNoMatchScan(String captureStatus,
                                           String elementName,
                                           int imageWidth,
                                           int imageHeight,
                                           double bestScore,
                                           int bestPointX,
                                           int bestPointY,
                                           String bestRectText) {
        private static ReturnButtonNoMatchScan captureFailed(String elementName) {
            return new ReturnButtonNoMatchScan("capture-failed", elementName, -1, -1, -1.0, -1, -1, "-");
        }

        private static ReturnButtonNoMatchScan capturedNoBest(String elementName, int width, int height) {
            return new ReturnButtonNoMatchScan("memory-no-best", elementName, width, height, -1.0, -1, -1, "-");
        }

        private static ReturnButtonNoMatchScan analysisFailed(String elementName, int width, int height) {
            return new ReturnButtonNoMatchScan("analysis-failed", elementName, width, height, -1.0, -1, -1, "-");
        }

        private String imageSizeText() {
            return imageWidth <= 0 || imageHeight <= 0 ? "-" : imageWidth + "x" + imageHeight;
        }
    }

}
