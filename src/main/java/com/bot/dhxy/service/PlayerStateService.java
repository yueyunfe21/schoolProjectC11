package com.bot.dhxy.service;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.Accessors;



import com.bot.dhxy.model.ocr.LocationInfo;
import com.bot.dhxy.cloud.task.SheyaoxiangStatusCloudDecision;
import com.bot.dhxy.cloud.task.SheyaoxiangStatusCloudDecisionService;
import com.bot.dhxy.cloud.task.SheyaoxiangStatusCloudRequest;
import com.bot.dhxy.core.GameClientTracker;
import com.bot.dhxy.core.GameContext;
import com.bot.dhxy.config.BotProperties;
import com.bot.dhxy.input.InputProvider;
import com.bot.dhxy.input.InputSequences;
import com.bot.dhxy.input.action.InputAction;
import com.bot.dhxy.input.action.InputActionScope;
import com.bot.dhxy.model.PlayerCharacter;
import com.bot.dhxy.runner.context.TaskExecutionContext;
import com.bot.dhxy.runner.stop.TaskCheckpoint;
import com.bot.dhxy.runner.stop.TaskSleep;
import com.bot.dhxy.runner.stop.TaskStopRequestedException;
import com.bot.dhxy.tools.CoordinateHelper;
import com.bot.dhxy.tools.LatencyMetrics;
import com.bot.dhxy.vision.LocationVisionService;
import com.bot.dhxy.window.runtime.WindowScopedTempPath;
import com.bot.dhxy.window.runtime.WindowTaskContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import javax.imageio.ImageIO;

/**
 * Maintains the current window's player identity, position, supplies, and incense state.
 *
 * <p>Runtime counters are scoped by {@link WindowTaskContextHolder} so multi-window idle/member
 * loops do not share first-aid or incense cooldowns. Read-only supply checks can use background
 * screenshots; actual healing and bag usage submit an exclusive input sequence so one character
 * finishes its full person/pet HP/MP recovery before another window can click.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PlayerStateService {

    private final GameContext context;
    private final ClientIdentityService identityService;
    private final LocationVisionService locationRadar;
    private final GameClientTracker tracker;
    private final InputProvider inputProvider;
    private final InputSequences inputSequences;
    private final CoordinateHelper coordinateHelper;
    private final BagService bagService;
    private final WindowTaskContextHolder windowTaskContextHolder;
    private final BotProperties config;
    private final WindowScopedTempPath windowScopedTempPath;
    private final SheyaoxiangStatusCloudDecisionService sheyaoxiangStatusCloudDecisionService;

    private final Map<String, PlayerRuntimeState> runtimeStates = new ConcurrentHashMap<>();
    private static final long INCENSE_DURATION_MS = 59 * 60 * 1000L;
    private static final long INCENSE_REFRESH_REMAINING_MS = 20 * 60 * 1000L;
    /**
     * CR231: safety margin before the cloud refresh threshold. The post-use quiet window ends this
     * much earlier than (duration - refresh threshold) so the first resumed cloud check still sees
     * a positive remaining budget instead of racing the 20-minute boundary.
     */
    private static final long INCENSE_QUIET_MARGIN_MS = 2 * 60 * 1000L;

    private static final int MAX_CHECKS_BETWEEN_BATTLES = 1;

    private static final int HEAL_TIME_INTERVAL = 5000;

    private static final int CHAR_BAR_LEFT_X = 949;
    private static final int CHAR_BAR_RIGHT_X = 1020;
    private static final int PET_BAR_LEFT_X = 823;
    private static final int PET_BAR_RIGHT_X = 876;

    private static final int BAR_HP_Y = 85;
    private static final int BAR_MP_Y = 101;
    private static final int BARS_SCAN_LEFT_X = PET_BAR_LEFT_X;
    private static final int BARS_SCAN_TOP_Y = BAR_HP_Y;
    private static final int BARS_SCAN_W = CHAR_BAR_RIGHT_X - PET_BAR_LEFT_X + 1;
    private static final int BARS_SCAN_H = BAR_MP_Y - BAR_HP_Y + 1;
    private static final int BAR_SAMPLE_RADIUS_X = 2;
    private static final int BAR_SAMPLE_RADIUS_Y = 1;
    private static final int HIGHER_HEALTH_PROBE_OFFSET = 10;
    private static final int NEAR_THRESHOLD_HEALTH_MARGIN_PERCENT = 3;
    private static final int HEAL_CONFIRM_DELAY_MS = 350;
    /*
     * First-aid HP/MP clicks can leave a client-relative hover tooltip in the game window. CR150
     * clears that hover at the end of the real supply input, and deliberately avoids the
     * user-confirmed top-right forbidden area: absolute (2076,180) was measured on base=(1315,33),
     * so its window-relative left-bottom corner is approximately (761,147). The forbidden rectangle
     * is relX>=761 && relY<=147.
     */
    private static final int GAME_CLIENT_WIDTH = 1024;
    private static final int GAME_CLIENT_HEIGHT = 768;
    private static final int SAFE_MOUSE_FORBIDDEN_LEFT_REL_X = 761;
    private static final int SAFE_MOUSE_FORBIDDEN_BOTTOM_REL_Y = 147;
    private static final int SAFE_MOUSE_HOVER_CLEAR_DELAY_MS = 300;

    /*
     * 摄妖香状态图标检测框。这个区域是窗口相对坐标，当前按用户实测的
     * base=(379,154)、absolute=(1380,277)-(1403,311) 回推得到。
     * 只截状态栏右侧的摄妖香图标窄框，避免把周围按钮/背景也拿去做模板匹配。
     */
    private static final int STATUS_PANEL_X = 901;
    private static final int STATUS_PANEL_Y = 123;
    private static final int STATUS_PANEL_W = 123;
    private static final int STATUS_PANEL_H = 34;
    /**
     * Refresh the current character identity from the bound window title and/or OCR fallback.
     *
     * <p>Side effect: mutates {@link GameContext#getMe()} for the current runtime context.</p>
     */
    public void syncMyIdentity() {
        log.info("🤖 [状态中枢] 请求读取角色档案...");
        PlayerCharacter me = context.getMe();
        identityService.scanAndSyncIdentity(me);
        log.info("📋 当前上线角色: {}", me.toString());
    }

    /**
     * Refresh the current map name and logical coordinates, then return the fresh scan result.
     *
     * <p>This is the business-layer gateway for current-position scans. Callers that need a fresh
     * location should use this method instead of calling {@code LocationVisionService} directly, so
     * future caching, fallback tuning, and per-window state updates stay centralized.</p>
     *
     * @return latest recognized map/coordinate, or {@code null} when all no-input readers miss.
     */
    public LocationInfo syncMyPosition() {
        long latencyStart = LatencyMetrics.start();
        boolean updated = false;
        String mapName = null;
        Integer x = null;
        Integer y = null;
        LocationInfo info = null;
        log.info("🤖 [状态中枢] 请求雷达扫描当前位置...");
        try {
            info = locationRadar.scanCurrentLocation();

            if (info != null) {
                PlayerCharacter me = context.getMe();
                me.setCurrentMapName(info.mapName);
                me.setX(info.x);
                me.setY(info.y);
                updated = true;
                mapName = info.mapName;
                x = info.x;
                y = info.y;
                log.info("🔄 全局记忆已更新: {}", me.toString());
            } else {
                log.warn("⚠️ [状态中枢] 雷达未能看清当前位置，记忆未更新。");
            }
        } finally {
            LatencyMetrics.info(log, "player.position.sync", latencyStart,
                    "updated=" + updated + " map=" + safeLatencyValue(mapName)
                            + " coord=" + (x == null || y == null ? "-" : x + "," + y));
        }
        return info;
    }

    public void syncAll() {
        syncMyIdentity();
        syncMyPosition();
    }

    /**
     * Reset post-combat first-aid throttling for the current window.
     *
     * <p>Called after a confirmed battle exit so the next idle grant can run one supply pass.</p>
     */
    public void resetCheckCounter() {
        PlayerRuntimeState state = state();
        state.checksDoneThisRound = 0;
        state.lastCombatExitTime = System.currentTimeMillis();
        log.info("🔄 战斗结束，急救检查计数器已重置，准备进行战后体检！");
    }

    /**
     * Run a task-start supply check regardless of the previous post-combat throttle state.
     *
     * <p>Startup preparation is a different safety boundary from post-combat idle checks: a window
     * may have been sitting with low MP before the task starts, so the first task phase must inspect
     * HP/MP once even if the last idle/combat pass already consumed the normal between-battle quota.</p>
     *
     * @param taskContext optional task stop token for the current window task.
     */
    public void performStartupFirstAidCheck(TaskExecutionContext taskContext) {
        checkpoint(taskContext);
        PlayerRuntimeState state = state();
        state.checksDoneThisRound = 0;
        state.lastCombatExitTime = 0;
        log.info("🩺 启动急救检查：重置本窗口急救计数，准备 no-focus 预检人物/宝宝血法");
        FirstAidNoFocusProbeResult result =
                probeAndConsumeHealthyFirstAidNoFocus(taskContext, "startup");
        if (result == FirstAidNoFocusProbeResult.SUPPLY_NEEDED
                || result == FirstAidNoFocusProbeResult.UNKNOWN) {
            if (!performCachedFirstAidPlanNow(taskContext)) {
                log.warn("startup first-aid skipped: cached plan unavailable after no-focus precheck result={}",
                        result);
            }
        }
    }

    /**
     * Precompute the startup HP/MP decision while the window is still off-turn.
     *
     * <p>The probe is read-only and no-focus. A healthy result lets the later focused PREPARE skip
     * the same scan; a low/unknown result keeps the existing cached first-aid plan for the focused
     * turn to consume. This does not click or change bag state.</p>
     *
     * @param taskContext optional stop token for the current task.
     * @param source diagnostic source for logs.
     */
    public void prepareStartupFirstAidNoFocus(TaskExecutionContext taskContext, String source) {
        checkpoint(taskContext);
        PlayerRuntimeState state = state();
        state.checksDoneThisRound = 0;
        state.lastCombatExitTime = 0;
        FirstAidNoFocusProbeResult result =
                probeAndConsumeHealthyFirstAidNoFocus(taskContext, safeReason(source));
        state.startupFirstAidPrecheckResult = result;
        state.startupFirstAidPrecheckAtMs = System.currentTimeMillis();
        log.info("startup first-aid background precheck stored: source={} result={} pendingPlan={}",
                safeReason(source), result, state.pendingNoFocusFirstAidPlan != null);
    }

    /**
     * Consume a fresh startup first-aid precheck or fall back to the original foreground startup check.
     *
     * @param taskContext optional stop token for the current task.
     * @param maxAgeMs maximum age for trusting a read-only startup precheck.
     */
    public void performStartupFirstAidCheckFromPrecheckOrRun(TaskExecutionContext taskContext, long maxAgeMs) {
        checkpoint(taskContext);
        PlayerRuntimeState state = state();
        FirstAidNoFocusProbeResult result = state.startupFirstAidPrecheckResult;
        long ageMs = state.startupFirstAidPrecheckAtMs <= 0L
                ? -1L
                : Math.max(0L, System.currentTimeMillis() - state.startupFirstAidPrecheckAtMs);
        boolean fresh = result != null && (maxAgeMs <= 0L || ageMs <= maxAgeMs);
        state.startupFirstAidPrecheckResult = null;
        state.startupFirstAidPrecheckAtMs = 0L;
        if (!fresh) {
            log.info("startup first-aid precheck unavailable/stale: result={} ageMs={} maxAgeMs={}; run foreground check",
                    result, ageMs, maxAgeMs);
            performStartupFirstAidCheck(taskContext);
            return;
        }
        if (result == FirstAidNoFocusProbeResult.HEALTHY
                || result == FirstAidNoFocusProbeResult.ALREADY_DONE) {
            log.info("startup first-aid skipped by fresh no-focus precheck: result={} ageMs={}", result, ageMs);
            return;
        }
        if (!performCachedFirstAidPlanNow(taskContext)) {
            log.warn("startup first-aid precheck requires supply but cached plan unavailable: result={} ageMs={}; run foreground check",
                    result, ageMs);
            performStartupFirstAidCheck(taskContext);
        }
    }

    /**
     * Run the quiet HP/MP probe and consume the post-combat check only when the window is healthy.
     *
     * <p>Follower windows call this immediately after a combat-exit signal. The probe uses a
     * no-focus HWND screenshot only. A healthy result is enough to avoid joining the task-turn
     * queue; a low or unknown result leaves a cached exact or conservative plan for the caller to
     * execute later. Pending callers must consume that plan instead of probing again.</p>
     *
     * @param taskContext optional task stop token; null is allowed for legacy callers.
     * @param source short diagnostic label for logs.
     * @return precise no-focus probe outcome so callers can decide whether to defer real input.
     */
    public FirstAidNoFocusProbeResult probeAndConsumeHealthyFirstAidNoFocus(TaskExecutionContext taskContext,
                                                                           String source) {
        FirstAidNoFocusProbeResult result = probeFirstAidSupplyNoFocus(taskContext);
        return consumeHealthyFirstAidProbeResult(result, source);
    }

    private FirstAidNoFocusProbeResult consumeHealthyFirstAidProbeResult(FirstAidNoFocusProbeResult result,
                                                                          String source) {
        if (result == FirstAidNoFocusProbeResult.HEALTHY) {
            PlayerRuntimeState state = state();
            state.checksDoneThisRound++;
            state.pendingNoFocusFirstAidPlan = null;
            log.info("🩺 战后体检 no-focus 预检健康，跳过 pending 补给：source={} 当前空闲期已查次数: {}/{}",
                    safeReason(source), state.checksDoneThisRound, MAX_CHECKS_BETWEEN_BATTLES);
        }
        return result;
    }

    /**
     * Check whether player/pet HP or MP appears below configured thresholds without focusing.
     *
     * @param taskContext optional task stop token; null is allowed for legacy callers.
     * @return SUPPLY_NEEDED only when a visible enabled bar is below threshold; HEALTHY when the
     * no-focus screenshot is readable and all enabled bars pass; UNKNOWN when the safe screenshot
     * cannot be read. UNKNOWN with a known window base caches a conservative plan that supplements
     * all enabled HP/MP targets.
     */
    public FirstAidNoFocusProbeResult probeFirstAidSupplyNoFocus(TaskExecutionContext taskContext) {
        BufferedImage bars = captureBarsSnapshotNoFocus();
        return probeFirstAidSupplyFromBars(taskContext, bars, tracker.getLastCaptureAudit());
    }

    private FirstAidNoFocusProbeResult probeFirstAidSupplyFromBars(TaskExecutionContext taskContext,
                                                                     BufferedImage bars,
                                                                     GameClientTracker.CaptureAudit captureAudit) {
        checkpoint(taskContext);
        PlayerRuntimeState state = state();
        if (state.checksDoneThisRound >= MAX_CHECKS_BETWEEN_BATTLES) {
            log.info("first-aid no-focus precheck skipped: checks already done {}/{}",
                    state.checksDoneThisRound, MAX_CHECKS_BETWEEN_BATTLES);
            return FirstAidNoFocusProbeResult.ALREADY_DONE;
        }
        int planBaseX = tracker.getWindowBaseX();
        int planBaseY = tracker.getWindowBaseY();
        if (planBaseX == -1 || planBaseY == -1) {
            log.warn("first-aid no-focus precheck skipped: window base unavailable base=({}, {})",
                    planBaseX, planBaseY);
            state.pendingNoFocusFirstAidPlan = null;
            return FirstAidNoFocusProbeResult.UNKNOWN;
        }

        if (bars == null) {
            log.warn("first-aid no-focus precheck failed: bars snapshot unavailable windowId={} player={} base=({}, {}) roiRel=({}, {}) {}x{} capture={}",
                    currentWindowId(), currentPlayerForLog(), planBaseX, planBaseY,
                    BARS_SCAN_LEFT_X, BARS_SCAN_TOP_Y, BARS_SCAN_W, BARS_SCAN_H,
                    captureAudit.toLogText());
            cacheConservativeFirstAidPlan(state, planBaseX, planBaseY, "bars-snapshot-unavailable");
            return FirstAidNoFocusProbeResult.UNKNOWN;
        }
        try {
            FirstAidProbeSummary summary = inspectSupplyTargetsFromSnapshot(bars, planBaseX, planBaseY, captureAudit);
            if (summary.unknown()) {
                cacheConservativeFirstAidPlan(state, planBaseX, planBaseY, summary.reason());
                log.warn("first-aid no-focus precheck result: decision=UNKNOWN needed=true reason={} targets={} windowId={} player={} planBase=({}, {}) roiRel=({}, {}) {}x{} capture={} bars={}",
                        summary.reason(), describeFirstAidTargets(state.pendingNoFocusFirstAidPlan == null
                                ? List.of() : state.pendingNoFocusFirstAidPlan.targets()),
                        currentWindowId(), currentPlayerForLog(), planBaseX, planBaseY,
                        BARS_SCAN_LEFT_X, BARS_SCAN_TOP_Y, BARS_SCAN_W, BARS_SCAN_H,
                        captureAudit.toLogText(), summary.describe());
                return FirstAidNoFocusProbeResult.UNKNOWN;
            }
            List<FirstAidTarget> targets = summary.targets();
            if (!targets.isEmpty()) {
                /*
                 * This is the 医宝宝-style precompute path: watcher/no-focus work decides exactly
                 * which bars need supply, so the later focused turn only performs clicks.
                 */
                state.pendingNoFocusFirstAidPlan = new FirstAidPlan(
                        targets, System.currentTimeMillis(), planBaseX, planBaseY);
            } else {
                state.pendingNoFocusFirstAidPlan = null;
            }
            log.info("first-aid no-focus precheck result: decision={} needed={} reason={} targets={} windowId={} player={} planBase=({}, {}) roiRel=({}, {}) {}x{} capture={} bars={}",
                    targets.isEmpty() ? "HEALTHY" : "SUPPLY_NEEDED", !targets.isEmpty(), summary.reason(),
                    describeFirstAidTargets(targets), currentWindowId(), currentPlayerForLog(), planBaseX, planBaseY,
                    BARS_SCAN_LEFT_X, BARS_SCAN_TOP_Y, BARS_SCAN_W, BARS_SCAN_H,
                    captureAudit.toLogText(), summary.describe());
            return targets.isEmpty() ? FirstAidNoFocusProbeResult.HEALTHY : FirstAidNoFocusProbeResult.SUPPLY_NEEDED;
        } finally {
            bars.flush();
        }
    }

    /**
     * @return true when the current window has a precomputed first-aid plan waiting for real input.
     */
    public boolean hasPendingNoFocusFirstAidPlanForCurrentWindow() {
        FirstAidPlan plan = state().pendingNoFocusFirstAidPlan;
        return plan != null && !plan.targets().isEmpty();
    }

    /**
     * Execute the no-focus precomputed first-aid clicks without rescanning every bar.
     *
     * @param taskContext optional stop token for the current task.
     * @return true when a cached plan was consumed; false when no cached no-focus plan is available.
     */
    public boolean performCachedFirstAidPlanNow(TaskExecutionContext taskContext) {
        checkpoint(taskContext);
        PlayerRuntimeState state = state();
        FirstAidPlan plan = state.pendingNoFocusFirstAidPlan;
        state.pendingNoFocusFirstAidPlan = null;
        if (plan == null || plan.targets().isEmpty()) {
            return false;
        }
        if (plan.baseX() == -1 || plan.baseY() == -1) {
            log.warn("first-aid cached plan skipped: plan window base unavailable targets={}",
                    describeFirstAidTargets(plan.targets()));
            return false;
        }

        log.info("🩺 执行后台预计算补给计划：targets={} ageMs={} planBase=({}, {})",
                describeFirstAidTargets(plan.targets()), System.currentTimeMillis() - plan.createdAtMs(),
                plan.baseX(), plan.baseY());
        /*
         * Detection stays fully no-focus/background. Real input deliberately uses the same execution
         * style as the old working healAll path: an exclusive input callback, focus before callback,
         * direct InputProvider right-click, then the old 800ms settle delay per click.
         */
        boolean completed = inputSequences.submitExclusiveAndWait("playerState:healCachedPlan",
                () -> performCachedFirstAidPlanDirect(plan));
        if (!completed) {
            log.warn("player-state cached first-aid transaction did not complete");
        }
        state.checksDoneThisRound++;
        log.info("✅ 后台预计算补给计划结束。当前空闲期已查次数: {}/{}",
                state.checksDoneThisRound, MAX_CHECKS_BETWEEN_BATTLES);
        return true;
    }

    private boolean performCachedFirstAidPlanDirect(FirstAidPlan plan) {
        /*
         * The input worker has its own tracker ThreadLocal. Refresh it after the window context has
         * been bound, but keep the precheck base as a fallback so cached healing never clicks using
         * another window's stale coordinates.
         */
        int baseX = plan.baseX();
        int baseY = plan.baseY();
        boolean refreshed = tracker.refreshWindowState();
        int refreshedBaseX = tracker.getWindowBaseX();
        int refreshedBaseY = tracker.getWindowBaseY();
        if (refreshed && refreshedBaseX != -1 && refreshedBaseY != -1) {
            if (refreshedBaseX != baseX || refreshedBaseY != baseY) {
                log.info("first-aid cached plan base refreshed: planBase=({}, {}) refreshedBase=({}, {})",
                        baseX, baseY, refreshedBaseX, refreshedBaseY);
            }
            baseX = refreshedBaseX;
            baseY = refreshedBaseY;
        } else {
            log.warn("first-aid cached plan using stored base: refreshSuccess={} storedBase=({}, {}) refreshedBase=({}, {})",
                    refreshed, baseX, baseY, refreshedBaseX, refreshedBaseY);
        }

        boolean supplied = false;
        for (FirstAidTarget target : plan.targets()) {
            int absX = baseX + target.relX();
            int absY = baseY + target.relY();
            log.warn("🚨 后台预计算命中 [{}]，按当前窗口补给：base=({}, {}) rel=({}, {}) abs=({}, {}) threshold={}%",
                    target.name(), baseX, baseY, target.relX(), target.relY(), absX, absY, target.threshold());
            if (!InputActionScope.checkpoint()) {
                return false;
            }
            inputProvider.clickRight(absX, absY, 100);
            if (!TaskSleep.sleep(800) || !InputActionScope.checkpoint()) {
                return false;
            }
            supplied = true;
        }
        if (supplied) {
            moveMouseAwayAfterFirstAidSupplyDirect("playerState:healCachedPlan", baseX, baseY);
        }
        return !Thread.currentThread().isInterrupted();
    }

    /**
     * Check whether the top-right player/pet HP/MP bars are still visible without sending input.
     *
     * <p>This is a lightweight state probe for Alt+A direct-combat mode detection. It only captures
     * the small status-bar strip and counts red/blue bar pixels; it does not move the mouse, open UI,
     * heal, or run OCR. In direct-combat mode the normal bars disappear together with other UI, so a
     * missing bar strip is supporting evidence that the mode is active.</p>
     *
     * @param reason log label describing the caller.
     * @return true when enough red/blue status-bar pixels are visible in the normal bar area.
     */
    public boolean areStatusBarsVisibleNoFocus(String reason) {
        BufferedImage bars = captureBarsSnapshotNoFocus();
        if (bars == null) {
            log.warn("[player-bars] visibility probe capture failed: reason={}", safeReason(reason));
            return false;
        }
        try {
            int redPixels = 0;
            int bluePixels = 0;
            for (int y = 0; y < bars.getHeight(); y++) {
                for (int x = 0; x < bars.getWidth(); x++) {
                    int rgb = bars.getRGB(x, y);
                    if (isHealthyColor(rgb, true)) {
                        redPixels++;
                    } else if (isHealthyColor(rgb, false)) {
                        bluePixels++;
                    }
                }
            }
            boolean visible = redPixels + bluePixels >= 16 && (redPixels >= 4 || bluePixels >= 4);
            log.info("[player-bars] visibility probe: reason={} visible={} redPixels={} bluePixels={} size={}x{}",
                    safeReason(reason), visible, redPixels, bluePixels, bars.getWidth(), bars.getHeight());
            return visible;
        } finally {
            bars.flush();
        }
    }

    private void performFirstAidCheck(boolean ignoreTimeInterval, TaskExecutionContext taskContext) {
        checkpoint(taskContext);
        PlayerRuntimeState state = state();
        if (state.checksDoneThisRound >= MAX_CHECKS_BETWEEN_BATTLES) {
            return;
        }

        if (tracker.getWindowBaseX() == -1) return;

        if (!ignoreTimeInterval && System.currentTimeMillis() - state.lastCombatExitTime < HEAL_TIME_INTERVAL) {
            return;
        }

        log.info("🩺 开始执行战后体检：人物血={}({}%) 人物法={}({}%) 宝宝血={}({}%) 宝宝法={}({}%)",
                config.isPlayerHpSupplyEnabled(), normalizeThreshold(config.getPlayerHpSupplyThreshold()),
                config.isPlayerMpSupplyEnabled(), normalizeThreshold(config.getPlayerMpSupplyThreshold()),
                config.isPetHpSupplyEnabled(), normalizeThreshold(config.getPetHpSupplyThreshold()),
                config.isPetMpSupplyEnabled(), normalizeThreshold(config.getPetMpSupplyThreshold()));
        healAll(taskContext);
        state.checksDoneThisRound++;
        log.info("✅ 本轮体检结束。当前空闲期已查次数: {}/{}", state.checksDoneThisRound, MAX_CHECKS_BETWEEN_BATTLES);
    }

    /**
     * Heal enabled player and pet HP/MP bars as one exclusive input transaction.
     *
     * <p>This is intentionally all-or-nothing from the input scheduler's perspective: another window
     * should not interleave between player HP, player MP, pet HP, and pet MP clicks.</p>
     */
    public void healAll() {
        boolean completed = inputSequences.submitExclusiveAndWait("playerState:healAll", this::healAllDirect);
        if (!completed) {
            log.warn("player-state healAll exclusive transaction did not complete");
        }
    }

    private boolean healAllDirect() {
        BufferedImage bars = captureBarsSnapshot();
        if (bars == null) {
            log.warn("战后体检截图失败，跳过本轮自动补给，避免误点血法条");
            return true;
        }

        boolean supplied = false;
        try {
            supplied |= checkAndHealFromSnapshotIfEnabled(bars, "人物血量", CHAR_BAR_LEFT_X, CHAR_BAR_RIGHT_X,
                    BAR_HP_Y, true, config.isPlayerHpSupplyEnabled(), config.getPlayerHpSupplyThreshold());
            supplied |= checkAndHealFromSnapshotIfEnabled(bars, "人物法力", CHAR_BAR_LEFT_X, CHAR_BAR_RIGHT_X,
                    BAR_MP_Y, false, config.isPlayerMpSupplyEnabled(), config.getPlayerMpSupplyThreshold());
            supplied |= checkAndHealFromSnapshotIfEnabled(bars, "宝宝血量", PET_BAR_LEFT_X, PET_BAR_RIGHT_X,
                    BAR_HP_Y, true, config.isPetHpSupplyEnabled(), config.getPetHpSupplyThreshold());
            supplied |= checkAndHealFromSnapshotIfEnabled(bars, "宝宝法力", PET_BAR_LEFT_X, PET_BAR_RIGHT_X,
                    BAR_MP_Y, false, config.isPetMpSupplyEnabled(), config.getPetMpSupplyThreshold());
        } finally {
            bars.flush();
        }
        if (supplied) {
            moveMouseAwayAfterFirstAidSupply("playerState:healAllDirect");
        }
        return !Thread.currentThread().isInterrupted();
    }

    public void healAll(TaskExecutionContext taskContext) {
        checkpoint(taskContext);
        healAll();
        checkpoint(taskContext);
    }

    public void healPlayer() {
        if (config.isPlayerHpSupplyEnabled()) {
            int hpThreshold = normalizeThreshold(config.getPlayerHpSupplyThreshold());
            int hpX = calculateX(CHAR_BAR_LEFT_X, CHAR_BAR_RIGHT_X, hpThreshold);
            checkAndHeal("人物血量", hpX, BAR_HP_Y, true, hpThreshold);
        }
        if (config.isPlayerMpSupplyEnabled()) {
            int mpThreshold = normalizeThreshold(config.getPlayerMpSupplyThreshold());
            int mpX = calculateX(CHAR_BAR_LEFT_X, CHAR_BAR_RIGHT_X, mpThreshold);
            checkAndHeal("人物法力", mpX, BAR_MP_Y, false, mpThreshold);
        }
    }

    public void healPet() {
        if (config.isPetHpSupplyEnabled()) {
            int hpThreshold = normalizeThreshold(config.getPetHpSupplyThreshold());
            int hpX = calculateX(PET_BAR_LEFT_X, PET_BAR_RIGHT_X, hpThreshold);
            checkAndHeal("宝宝血量", hpX, BAR_HP_Y, true, hpThreshold);
        }
        if (config.isPetMpSupplyEnabled()) {
            int mpThreshold = normalizeThreshold(config.getPetMpSupplyThreshold());
            int mpX = calculateX(PET_BAR_LEFT_X, PET_BAR_RIGHT_X, mpThreshold);
            checkAndHeal("宝宝法力", mpX, BAR_MP_Y, false, mpThreshold);
        }
    }

    public boolean ensureSheYaoXiangActive() {
        return ensureSheYaoXiangActive(null);
    }

    /**
     * Ensure the incense buff is active or refresh it when the configured cooldown window opens.
     *
     * @param taskContext optional stop token. The method may open the bag and click an item, so it
     * should only be called when the current task is allowed to use physical input.
     */
    public boolean ensureSheYaoXiangActive(TaskExecutionContext taskContext) {
        return ensureSheYaoXiangActive(taskContext,
                (targetItemTemplate, context) -> bagService.findAndUseItem(
                        BagService.MAIN_BAG, targetItemTemplate, null, context),
                false);
    }

    /**
     * Ensure the incense buff while the caller already owns an opened main-bag session.
     *
     * @param mainBag opened main-bag session from {@link BagService#withMainBagOpen}.
     * @param taskContext optional stop token.
     * @return true only when this call actually used a 摄妖香 item.
     */
    public boolean ensureSheYaoXiangActiveInOpenMainBag(BagService.MainBagSession mainBag,
                                                        TaskExecutionContext taskContext) {
        return ensureSheYaoXiangActive(taskContext,
                (targetItemTemplate, context) -> mainBag.useItem(targetItemTemplate, null),
                true);
    }

    private boolean ensureSheYaoXiangActive(TaskExecutionContext taskContext,
                                            IncenseItemUser itemUser,
                                            boolean openMainBagSession) {
        long latencyStart = LatencyMetrics.start();
        try {
            checkpoint(taskContext);
            PlayerRuntimeState state = state();
            long now = System.currentTimeMillis();
            /*
             * CR231 quiet period: incense presence/remaining/refresh is still decided by the cloud,
             * but a trusted lastIncenseUsedTime (written only after a successful local use, or
             * back-computed from a trusted cloud remainingMs) proves the refresh threshold cannot
             * be due yet. During that window skip the whole chain — no TICK, no status capture, no
             * upload, no bag. lastIncenseUsedTime==0 (unknown/never/identity drift reset) never
             * enters the quiet period, so failure and unknown states keep the full cloud check.
             */
            long quietWindowMs = INCENSE_DURATION_MS - INCENSE_REFRESH_REMAINING_MS - INCENSE_QUIET_MARGIN_MS;
            long sinceLastUseMs = now - state.lastIncenseUsedTime;
            if (state.lastIncenseUsedTime > 0 && sinceLastUseMs >= 0 && sinceLastUseMs < quietWindowMs) {
                log.info("sheyaoxiang quiet period; skip cloud check: windowId={} sinceLastUseMs={} quietWindowMs={} quietRemainingMs={} estimatedIncenseRemainingMs={}",
                        currentWindowId(), sinceLastUseMs, quietWindowMs, quietWindowMs - sinceLastUseMs,
                        Math.max(0L, INCENSE_DURATION_MS - sinceLastUseMs));
                return false;
            }
            int[] statusRect = coordinateHelper.getScaledRect(STATUS_PANEL_X, STATUS_PANEL_Y, STATUS_PANEL_W, STATUS_PANEL_H);
            SheyaoxiangStatusCloudDecision decision = sheyaoxiangStatusCloudDecisionService.decide(
                    buildSheyaoxiangCloudRequest(state, taskContext, statusRect, openMainBagSession,
                            SheyaoxiangStatusCloudRequest.Hook.TICK, null, null, null));
            if (decision.shouldCaptureStatus()) {
                decision = captureSheyaoxiangStatusAndAskCloud(state, taskContext, statusRect,
                        openMainBagSession, decision);
            }

            applySheyaoxiangCloudFacts(state, statusRect, decision);
            if (!decision.shouldUseIncense()) {
                if (decision.failClosed()) {
                    log.warn("sheyaoxiang cloud fail-closed: windowId={} action={} reason={} decisionId={}",
                            currentWindowId(), decision.getAction(), decision.getReason(), decision.getDecisionId());
                } else {
                    log.info("sheyaoxiang cloud no-use: windowId={} action={} present={} remainingMs={} source={} reason={} decisionId={}",
                            currentWindowId(), decision.getAction(), decision.getPresent(), decision.getRemainingMs(),
                            decision.getRemainingSource(), decision.getReason(), decision.getDecisionId());
                }
                return false;
            }

            log.warn("sheyaoxiang cloud requested USE_INCENSE: windowId={} present={} remainingMs={} source={} reason={} decisionId={}",
                    currentWindowId(), decision.getPresent(), decision.getRemainingMs(), decision.getRemainingSource(),
                    decision.getReason(), decision.getDecisionId());
            return executeCloudRequestedIncenseUse(taskContext, itemUser, state, statusRect,
                    openMainBagSession, decision);
        } finally {
            LatencyMetrics.info(log, "player.sheyaoxiang.ensure", latencyStart,
                    "context=" + (taskContext == null ? "-" : taskContext.getTaskCode()));
        }
    }

    public boolean ensureSheYaoXiangActiveForLeaderTask(String source) {
        return ensureSheYaoXiangActiveForLeaderTask(source, null);
    }

    /**
     * Leader-only wrapper for incense refresh.
     *
     * @param source diagnostic caller name.
     * @param taskContext optional stop token.
     */
    public boolean ensureSheYaoXiangActiveForLeaderTask(String source, TaskExecutionContext taskContext) {
        checkpoint(taskContext);
        String caller = source == null || source.isBlank() ? "unknown" : source;
        var currentWindow = windowTaskContextHolder.rawCurrent();
        if (currentWindow.isPresent() && currentWindow.get().isMember()) {
            log.info("摄妖香检查跳过：source={} windowId={} role={}，队员窗口不负责摄妖香",
                    caller, currentWindow.get().getWindowId(), currentWindow.get().getRole().getDisplayName());
            return false;
        }
        if (currentWindow.isPresent()) {
            log.info("摄妖香检查允许：source={} windowId={} role={}",
                    caller, currentWindow.get().getWindowId(), currentWindow.get().getRole().getDisplayName());
        } else {
            log.info("摄妖香检查允许：source={} 无窗口上下文，按单窗口/队长任务兼容处理", caller);
        }
        return ensureSheYaoXiangActive(taskContext);
    }

    public boolean checkAndHeal(String name, int relX, int relY, boolean expectRed) {
        return checkAndHeal(name, relX, relY, expectRed, 70);
    }

    public boolean checkAndHeal(String name, int relX, int relY, boolean expectRed, int threshold) {
        int normalizedThreshold = normalizeThreshold(threshold);
        int[] absoluteRect = coordinateHelper.getScaledRect(relX, relY, 1, 1);
        int absX = absoluteRect[0];
        int absY = absoluteRect[1];

        BufferedImage pixelImg = tracker.captureToMemory(name, absX, absY, absX + 1, absY + 1);
        if (pixelImg == null) return false;

        int rgb = pixelImg.getRGB(0, 0);
        pixelImg.flush();
        return healIfUnhealthy(name, absX, absY, rgb, expectRed, normalizedThreshold, true);
    }

    private BufferedImage captureBarsSnapshot() {
        int[] rect = coordinateHelper.getScaledRect(BARS_SCAN_LEFT_X, BARS_SCAN_TOP_Y, BARS_SCAN_W, BARS_SCAN_H);
        return tracker.captureToMemory("player-state-bars", rect[0], rect[1], rect[2], rect[3]);
    }

    private BufferedImage captureBarsSnapshotNoFocus() {
        int[] rect = coordinateHelper.getScaledRect(BARS_SCAN_LEFT_X, BARS_SCAN_TOP_Y, BARS_SCAN_W, BARS_SCAN_H);
        return tracker.captureToMemory("player-state-bars-precheck", rect[0], rect[1], rect[2], rect[3]);
    }

    private void moveMouseAwayAfterFirstAidSupply(String source) {
        int baseX = tracker.getWindowBaseX();
        int baseY = tracker.getWindowBaseY();
        if (baseX == -1 || baseY == -1) {
            log.warn("first-aid hover cleanup skipped: source={} windowId={} base unavailable base=({}, {})",
                    safeReason(source), currentWindowId(), baseX, baseY);
            return;
        }
        if (isInputWorkerThread()) {
            moveMouseAwayAfterFirstAidSupplyDirect(source, baseX, baseY);
            return;
        }
        SafeMousePoint safePoint = randomFirstAidHoverSafePoint(baseX, baseY);
        logFirstAidHoverCleanupMove(source, baseX, baseY, safePoint, "queued-input-worker");
        inputSequences.submitAndWait("playerState:firstAidHoverCleanup:" + safeReason(source), List.of(
                InputAction.moveMouse(safePoint.absX(), safePoint.absY()),
                InputAction.sleep(SAFE_MOUSE_HOVER_CLEAR_DELAY_MS)
        ));
    }

    private void moveMouseAwayAfterFirstAidSupplyDirect(String source, int baseX, int baseY) {
        if (baseX == -1 || baseY == -1) {
            log.warn("first-aid hover cleanup skipped: source={} windowId={} base unavailable base=({}, {})",
                    safeReason(source), currentWindowId(), baseX, baseY);
            return;
        }
        SafeMousePoint safePoint = randomFirstAidHoverSafePoint(baseX, baseY);
        logFirstAidHoverCleanupMove(source, baseX, baseY, safePoint, "direct-input-worker");
        if (!InputActionScope.checkpoint()) {
            return;
        }
        inputProvider.moveMouse(safePoint.absX(), safePoint.absY());
        TaskSleep.sleep(SAFE_MOUSE_HOVER_CLEAR_DELAY_MS);
    }

    private void logFirstAidHoverCleanupMove(String source,
                                             int baseX,
                                             int baseY,
                                             SafeMousePoint safePoint,
                                             String inputPath) {
        log.info("first-aid hover cleanup safe move: source={} windowId={} base=({}, {}) safeRel=({}, {}) safeAbs=({}, {}) inputPath={}",
                safeReason(source), currentWindowId(), baseX, baseY, safePoint.relX(), safePoint.relY(),
                safePoint.absX(), safePoint.absY(), inputPath);
    }

    private SafeMousePoint randomFirstAidHoverSafePoint(int baseX, int baseY) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        int relX;
        int relY;
        do {
            relX = random.nextInt(GAME_CLIENT_WIDTH);
            relY = random.nextInt(GAME_CLIENT_HEIGHT);
        } while (relX >= SAFE_MOUSE_FORBIDDEN_LEFT_REL_X && relY <= SAFE_MOUSE_FORBIDDEN_BOTTOM_REL_Y);
        return new SafeMousePoint(relX, relY, baseX + relX, baseY + relY);
    }

    private boolean checkAndHealFromSnapshotIfEnabled(BufferedImage bars, String name,
                                                      int leftX, int rightX, int relY, boolean expectRed,
                                                      boolean enabled, int threshold) {
        if (!enabled) {
            log.info("🩺 [{}] 补给未启用，跳过检查", name);
            return false;
        }
        int normalizedThreshold = normalizeThreshold(threshold);
        int relX = calculateX(leftX, rightX, normalizedThreshold);
        return checkAndHealFromSnapshot(bars, name, relX, relY, expectRed, normalizedThreshold);
    }

    private List<FirstAidTarget> findSupplyTargetsFromSnapshot(BufferedImage bars) {
        List<FirstAidTarget> candidates = new ArrayList<>();
        addSupplyTargetIfNeeded(candidates, bars, "人物血量", CHAR_BAR_LEFT_X, CHAR_BAR_RIGHT_X,
                BAR_HP_Y, true, config.isPlayerHpSupplyEnabled(), config.getPlayerHpSupplyThreshold());
        addSupplyTargetIfNeeded(candidates, bars, "人物法力", CHAR_BAR_LEFT_X, CHAR_BAR_RIGHT_X,
                BAR_MP_Y, false, config.isPlayerMpSupplyEnabled(), config.getPlayerMpSupplyThreshold());
        addSupplyTargetIfNeeded(candidates, bars, "宝宝血量", PET_BAR_LEFT_X, PET_BAR_RIGHT_X,
                BAR_HP_Y, true, config.isPetHpSupplyEnabled(), config.getPetHpSupplyThreshold());
        addSupplyTargetIfNeeded(candidates, bars, "宝宝法力", PET_BAR_LEFT_X, PET_BAR_RIGHT_X,
                BAR_MP_Y, false, config.isPetMpSupplyEnabled(), config.getPetMpSupplyThreshold());
        return candidates;
    }

    private FirstAidProbeSummary inspectSupplyTargetsFromSnapshot(BufferedImage bars,
                                                                  int baseX,
                                                                  int baseY,
                                                                  GameClientTracker.CaptureAudit captureAudit) {
        List<FirstAidTarget> targets = new ArrayList<>();
        List<FirstAidBarProbe> probes = new ArrayList<>();
        probes.add(probeFirstAidBar(bars, "人物血量", CHAR_BAR_LEFT_X, CHAR_BAR_RIGHT_X,
                BAR_HP_Y, true, config.isPlayerHpSupplyEnabled(), config.getPlayerHpSupplyThreshold()));
        probes.add(probeFirstAidBar(bars, "人物法力", CHAR_BAR_LEFT_X, CHAR_BAR_RIGHT_X,
                BAR_MP_Y, false, config.isPlayerMpSupplyEnabled(), config.getPlayerMpSupplyThreshold()));
        probes.add(probeFirstAidBar(bars, "宝宝血量", PET_BAR_LEFT_X, PET_BAR_RIGHT_X,
                BAR_HP_Y, true, config.isPetHpSupplyEnabled(), config.getPetHpSupplyThreshold()));
        probes.add(probeFirstAidBar(bars, "宝宝法力", PET_BAR_LEFT_X, PET_BAR_RIGHT_X,
                BAR_MP_Y, false, config.isPetMpSupplyEnabled(), config.getPetMpSupplyThreshold()));

        boolean enabledSeen = false;
        boolean readableSeen = false;
        for (FirstAidBarProbe probe : probes) {
            log.info("first-aid no-focus bar probe: windowId={} player={} base=({}, {}) target={} enabled={} decision={} reason={} threshold={} observedPercent={} healthyColumns={}/{} thresholdSample={}/{} higherSample={}/{} sampleRel=({}, {}) higherRel=({}, {}) rgb={} roiRel=({}, {})-({}, {}) capture={}",
                    currentWindowId(), currentPlayerForLog(), baseX, baseY, probe.name(), probe.enabled(),
                    probe.decision(), probe.reason(), probe.threshold(), probe.observedPercent(),
                    probe.healthyColumns(), probe.totalColumns(), probe.thresholdHealthyCount(), probe.sampleAreaPixels(),
                    probe.higherHealthyCount(), probe.sampleAreaPixels(), probe.relX(), probe.relY(),
                    probe.higherRelX(), probe.relY(), probe.rgbText(),
                    probe.leftX(), probe.relY() - BAR_SAMPLE_RADIUS_Y,
                    probe.rightX(), probe.relY() + BAR_SAMPLE_RADIUS_Y, captureAudit.toLogText());
            if (!probe.enabled()) {
                continue;
            }
            enabledSeen = true;
            if (probe.readable()) {
                readableSeen = true;
            }
            if (probe.supplyNeeded()) {
                targets.add(new FirstAidTarget(probe.name(), probe.relX(), probe.relY(),
                        probe.expectRed(), probe.threshold()));
            }
        }
        if (!enabledSeen) {
            return new FirstAidProbeSummary(List.of(), probes, false, "all-targets-disabled");
        }
        if (!readableSeen) {
            return new FirstAidProbeSummary(List.of(), probes, true, "no-enabled-bar-readable");
        }
        String reason = targets.isEmpty() ? "all-enabled-bars-at-or-above-threshold" : "enabled-bar-below-threshold";
        return new FirstAidProbeSummary(targets, probes, false, reason);
    }

    private void cacheConservativeFirstAidPlan(PlayerRuntimeState state, int baseX, int baseY, String reason) {
        List<FirstAidTarget> targets = buildConservativeFirstAidTargets();
        if (targets.isEmpty()) {
            state.pendingNoFocusFirstAidPlan = null;
            log.warn("first-aid no-focus UNKNOWN has no enabled conservative targets: reason={}", reason);
            return;
        }
        state.pendingNoFocusFirstAidPlan = new FirstAidPlan(targets, System.currentTimeMillis(), baseX, baseY);
        log.warn("first-aid no-focus UNKNOWN cached conservative plan: reason={} targets={} planBase=({}, {})",
                reason, describeFirstAidTargets(targets), baseX, baseY);
    }

    private List<FirstAidTarget> buildConservativeFirstAidTargets() {
        List<FirstAidTarget> targets = new ArrayList<>();
        addConservativeFirstAidTarget(targets, "人物血量", CHAR_BAR_LEFT_X, CHAR_BAR_RIGHT_X,
                BAR_HP_Y, true, config.isPlayerHpSupplyEnabled(), config.getPlayerHpSupplyThreshold());
        addConservativeFirstAidTarget(targets, "人物法力", CHAR_BAR_LEFT_X, CHAR_BAR_RIGHT_X,
                BAR_MP_Y, false, config.isPlayerMpSupplyEnabled(), config.getPlayerMpSupplyThreshold());
        addConservativeFirstAidTarget(targets, "宝宝血量", PET_BAR_LEFT_X, PET_BAR_RIGHT_X,
                BAR_HP_Y, true, config.isPetHpSupplyEnabled(), config.getPetHpSupplyThreshold());
        addConservativeFirstAidTarget(targets, "宝宝法力", PET_BAR_LEFT_X, PET_BAR_RIGHT_X,
                BAR_MP_Y, false, config.isPetMpSupplyEnabled(), config.getPetMpSupplyThreshold());
        return targets;
    }

    private void addConservativeFirstAidTarget(List<FirstAidTarget> targets,
                                               String name,
                                               int leftX,
                                               int rightX,
                                               int relY,
                                               boolean expectRed,
                                               boolean enabled,
                                               int threshold) {
        if (!enabled) {
            return;
        }
        int normalizedThreshold = normalizeThreshold(threshold);
        targets.add(new FirstAidTarget(name, calculateX(leftX, rightX, normalizedThreshold),
                relY, expectRed, normalizedThreshold));
    }

    private void addSupplyTargetIfNeeded(List<FirstAidTarget> targets,
                                         BufferedImage bars,
                                         String name,
                                         int leftX,
                                         int rightX,
                                         int relY,
                                         boolean expectRed,
                                         boolean enabled,
                                         int threshold) {
        if (!enabled) {
            return;
        }
        int normalizedThreshold = normalizeThreshold(threshold);
        int relX = calculateX(leftX, rightX, normalizedThreshold);
        if (isSupplyNeededFromSnapshot(bars, name, relX, relY, expectRed, normalizedThreshold)) {
            targets.add(new FirstAidTarget(name, relX, relY, expectRed, normalizedThreshold));
        }
    }

    private FirstAidBarProbe probeFirstAidBar(BufferedImage bars,
                                              String name,
                                              int leftX,
                                              int rightX,
                                              int relY,
                                              boolean expectRed,
                                              boolean enabled,
                                              int threshold) {
        int normalizedThreshold = normalizeThreshold(threshold);
        int relX = calculateX(leftX, rightX, normalizedThreshold);
        int higherThreshold = Math.min(95, normalizedThreshold + HIGHER_HEALTH_PROBE_OFFSET);
        int higherRelX = calculateX(leftX, rightX, higherThreshold);
        int totalColumns = Math.max(1, rightX - leftX + 1);
        int sampleAreaPixels = (BAR_SAMPLE_RADIUS_X * 2 + 1) * (BAR_SAMPLE_RADIUS_Y * 2 + 1);
        if (!enabled) {
            return new FirstAidBarProbe(name, false, expectRed, normalizedThreshold, leftX, rightX, relY,
                    relX, higherRelX, 0, totalColumns, 0, 0, sampleAreaPixels,
                    "SKIPPED", "disabled", 0, 0, 0);
        }
        if (bars == null) {
            return new FirstAidBarProbe(name, true, expectRed, normalizedThreshold, leftX, rightX, relY,
                    relX, higherRelX, 0, totalColumns, 0, 0, sampleAreaPixels,
                    "UNKNOWN", "snapshot-null", 0, 0, 0);
        }

        int localY = relY - BARS_SCAN_TOP_Y;
        int localLeftX = leftX - BARS_SCAN_LEFT_X;
        int localRightX = rightX - BARS_SCAN_LEFT_X;
        if (localY < 0 || localY >= bars.getHeight()
                || localRightX < 0 || localLeftX >= bars.getWidth()) {
            return new FirstAidBarProbe(name, true, expectRed, normalizedThreshold, leftX, rightX, relY,
                    relX, higherRelX, 0, totalColumns, 0, 0, sampleAreaPixels,
                    "UNKNOWN", "bar-roi-out-of-bounds", 0, 0, 0);
        }

        int healthyColumns = countHealthyColumns(bars, leftX, rightX, relY, expectRed);
        int observedPercent = Math.round(healthyColumns * 100.0f / totalColumns);
        int thresholdHealthyCount = countHealthySamples(bars, relX, relY, expectRed);
        int higherHealthyCount = countHealthySamples(bars, higherRelX, relY, expectRed);
        int[] rgb = sampleRgb(bars, relX, relY);
        boolean thresholdHealthy = thresholdHealthyCount >= 2;
        boolean higherHealthy = higherHealthyCount >= 2;
        /*
         * Single-pixel threshold probes are vulnerable to bar borders and hover artifacts. Treat the
         * bar as healthy only when the full strip reaches the configured percentage, or when the
         * strip is within a tiny margin of the threshold and the threshold sample itself agrees.
         * When a point sample says "healthy" but the full strip is far below the threshold, the
         * sample contradicts the bar-level evidence and must not suppress first-aid.
         */
        if (observedPercent >= normalizedThreshold) {
            return new FirstAidBarProbe(name, true, expectRed, normalizedThreshold, leftX, rightX, relY,
                    relX, higherRelX, healthyColumns, totalColumns, thresholdHealthyCount, higherHealthyCount,
                    sampleAreaPixels, "HEALTHY", "filled-ratio-at-threshold", rgb[0], rgb[1], rgb[2]);
        }
        if (thresholdHealthy
                && observedPercent >= normalizedThreshold - NEAR_THRESHOLD_HEALTH_MARGIN_PERCENT) {
            return new FirstAidBarProbe(name, true, expectRed, normalizedThreshold, leftX, rightX, relY,
                    relX, higherRelX, healthyColumns, totalColumns, thresholdHealthyCount, higherHealthyCount,
                    sampleAreaPixels, "HEALTHY", "near-threshold-strip-and-sample", rgb[0], rgb[1], rgb[2]);
        }
        String reason = (thresholdHealthy || higherHealthy)
                ? "inconsistent-sample-strip"
                : observedPercent == 0
                ? "no-matching-bar-color"
                : "filled-ratio-below-threshold";
        return new FirstAidBarProbe(name, true, expectRed, normalizedThreshold, leftX, rightX, relY,
                relX, higherRelX, healthyColumns, totalColumns, thresholdHealthyCount, higherHealthyCount,
                sampleAreaPixels, "SUPPLY_NEEDED", reason, rgb[0], rgb[1], rgb[2]);
    }

    private int countHealthyColumns(BufferedImage bars, int leftX, int rightX, int relY, boolean expectRed) {
        int healthyColumns = 0;
        for (int relX = leftX; relX <= rightX; relX++) {
            boolean healthyColumn = false;
            int centerX = relX - BARS_SCAN_LEFT_X;
            int centerY = relY - BARS_SCAN_TOP_Y;
            for (int dy = -BAR_SAMPLE_RADIUS_Y; dy <= BAR_SAMPLE_RADIUS_Y; dy++) {
                int y = centerY + dy;
                if (centerX < 0 || y < 0 || centerX >= bars.getWidth() || y >= bars.getHeight()) {
                    continue;
                }
                if (isHealthyColor(bars.getRGB(centerX, y), expectRed)) {
                    healthyColumn = true;
                    break;
                }
            }
            if (healthyColumn) {
                healthyColumns++;
            }
        }
        return healthyColumns;
    }

    private int countHealthySamples(BufferedImage bars, int relX, int relY, boolean expectRed) {
        int centerX = relX - BARS_SCAN_LEFT_X;
        int centerY = relY - BARS_SCAN_TOP_Y;
        int healthyCount = 0;
        for (int dx = -BAR_SAMPLE_RADIUS_X; dx <= BAR_SAMPLE_RADIUS_X; dx++) {
            for (int dy = -BAR_SAMPLE_RADIUS_Y; dy <= BAR_SAMPLE_RADIUS_Y; dy++) {
                int x = centerX + dx;
                int y = centerY + dy;
                if (x < 0 || y < 0 || x >= bars.getWidth() || y >= bars.getHeight()) {
                    continue;
                }
                if (isHealthyColor(bars.getRGB(x, y), expectRed)) {
                    healthyCount++;
                }
            }
        }
        return healthyCount;
    }

    private int[] sampleRgb(BufferedImage bars, int relX, int relY) {
        int localX = relX - BARS_SCAN_LEFT_X;
        int localY = relY - BARS_SCAN_TOP_Y;
        if (localX < 0 || localY < 0 || localX >= bars.getWidth() || localY >= bars.getHeight()) {
            return new int[]{-1, -1, -1};
        }
        int rgb = bars.getRGB(localX, localY);
        return new int[]{(rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF};
    }

    private boolean isSupplyNeededFromSnapshot(BufferedImage bars, String name, int relX, int relY,
                                               boolean expectRed, int threshold) {
        if (bars == null) {
            return false;
        }
        int localX = relX - BARS_SCAN_LEFT_X;
        int localY = relY - BARS_SCAN_TOP_Y;
        if (localX < 0 || localY < 0 || localX >= bars.getWidth() || localY >= bars.getHeight()) {
            log.warn("first-aid no-focus sample out of bounds: name={} rel=({}, {}) snapshot={}x{}",
                    name, relX, relY, bars.getWidth(), bars.getHeight());
            return false;
        }
        if (isHealthyInSnapshotArea(bars, relX, relY, expectRed)) {
            return false;
        }

        int normalizedThreshold = normalizeThreshold(threshold);
        int higherThreshold = Math.min(95, normalizedThreshold + HIGHER_HEALTH_PROBE_OFFSET);
        int higherRelX = calculateX(name.contains("宝宝") ? PET_BAR_LEFT_X : CHAR_BAR_LEFT_X,
                name.contains("宝宝") ? PET_BAR_RIGHT_X : CHAR_BAR_RIGHT_X, higherThreshold);
        if (isHealthyInSnapshotArea(bars, higherRelX, relY, expectRed)) {
            log.info("[{}] no-focus precheck: {}% sample low but {}% still healthy, skip supply",
                    name, normalizedThreshold, higherThreshold);
            return false;
        }

        int rgb = bars.getRGB(localX, localY);
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        log.warn("[{}] no-focus precheck: below {}%, supply needed rgb=({}, {}, {})",
                name, normalizedThreshold, r, g, b);
        return true;
    }

    private boolean checkAndHealFromSnapshot(BufferedImage bars, String name, int relX, int relY,
                                             boolean expectRed, int threshold) {
        int normalizedThreshold = normalizeThreshold(threshold);
        if (bars == null) {
            return false;
        }
        int localX = relX - BARS_SCAN_LEFT_X;
        int localY = relY - BARS_SCAN_TOP_Y;
        if (localX < 0 || localY < 0 || localX >= bars.getWidth() || localY >= bars.getHeight()) {
            log.warn("战后体检采样点越界：name={} rel=({}, {}) snapshot={}x{}",
                    name, relX, relY, bars.getWidth(), bars.getHeight());
            return false;
        }

        int rgb = bars.getRGB(localX, localY);
        int absX = tracker.getWindowBaseX() + relX;
        int absY = tracker.getWindowBaseY() + relY;
        if (isHealthyInSnapshotArea(bars, relX, relY, expectRed)) {
            return false;
        }

        int higherThreshold = Math.min(95, normalizedThreshold + HIGHER_HEALTH_PROBE_OFFSET);
        int higherRelX = calculateX(name.contains("宝宝") ? PET_BAR_LEFT_X : CHAR_BAR_LEFT_X,
                name.contains("宝宝") ? PET_BAR_RIGHT_X : CHAR_BAR_RIGHT_X, higherThreshold);
        if (isHealthyInSnapshotArea(bars, higherRelX, relY, expectRed)) {
            log.info("[{}] {}% 采样疑似误判，但 {}% 位置仍有有效颜色，跳过补给", name, normalizedThreshold, higherThreshold);
            return false;
        }

        TaskSleep.sleep(HEAL_CONFIRM_DELAY_MS);
        BufferedImage confirmBars = captureBarsSnapshot();
        if (confirmBars == null) {
            log.warn("[{}] 疑似低于 {}%，但二次截图失败，跳过补给以避免误点", name, normalizedThreshold);
            return false;
        }
        try {
            if (isHealthyInSnapshotArea(confirmBars, relX, relY, expectRed)) {
                log.info("[{}] 二次确认发现 {}% 位置已有有效颜色，判定首次采样误判，跳过补给", name, normalizedThreshold);
                return false;
            }
            if (isHealthyInSnapshotArea(confirmBars, higherRelX, relY, expectRed)) {
                log.info("[{}] 二次确认发现 {}% 位置仍有有效颜色，判定首次采样误判，跳过补给", name, higherThreshold);
                return false;
            }
            int confirmLocalX = relX - BARS_SCAN_LEFT_X;
            int confirmLocalY = relY - BARS_SCAN_TOP_Y;
            int confirmRgb = confirmBars.getRGB(confirmLocalX, confirmLocalY);
            return healIfUnhealthy(name, absX, absY, confirmRgb, expectRed, normalizedThreshold, false);
        } finally {
            confirmBars.flush();
        }
    }

    private boolean isHealthyInSnapshotArea(BufferedImage bars, int relX, int relY, boolean expectRed) {
        int centerX = relX - BARS_SCAN_LEFT_X;
        int centerY = relY - BARS_SCAN_TOP_Y;
        int healthyCount = 0;
        for (int dx = -BAR_SAMPLE_RADIUS_X; dx <= BAR_SAMPLE_RADIUS_X; dx++) {
            for (int dy = -BAR_SAMPLE_RADIUS_Y; dy <= BAR_SAMPLE_RADIUS_Y; dy++) {
                int x = centerX + dx;
                int y = centerY + dy;
                if (x < 0 || y < 0 || x >= bars.getWidth() || y >= bars.getHeight()) {
                    continue;
                }
                if (isHealthyColor(bars.getRGB(x, y), expectRed)) {
                    healthyCount++;
                }
            }
        }
        return healthyCount >= 2;
    }

    private boolean isHealthyColor(int rgb, boolean expectRed) {
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;

        if (expectRed) {
            return (r > 150) && (r > g + 80) && (r > b + 80);
        }
        return (b > 150) && (g > 120) && (b > r + 80);
    }

    private boolean healIfUnhealthy(String name,
                                    int absX,
                                    int absY,
                                    int rgb,
                                    boolean expectRed,
                                    int threshold,
                                    boolean moveMouseAwayAfterClick) {
        int normalizedThreshold = normalizeThreshold(threshold);
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;

        boolean isHealthy = isHealthyColor(rgb, expectRed);

        if (!isHealthy) {
            log.warn("🚨 警报！[{}] 未达 {}% 警戒线，执行原位右键补充！rgb=({}, {}, {})",
                    name, normalizedThreshold, r, g, b);
            if (isInputWorkerThread()) {
                if (!InputActionScope.checkpoint()) {
                    return false;
                }
                inputProvider.clickRight(absX, absY, 100);
                if (!TaskSleep.sleep(800) || !InputActionScope.checkpoint()) {
                    return false;
                }
                if (moveMouseAwayAfterClick) {
                    moveMouseAwayAfterFirstAidSupply("playerState:heal:" + name);
                }
            } else {
                if (moveMouseAwayAfterClick
                        && tracker.getWindowBaseX() != -1
                        && tracker.getWindowBaseY() != -1) {
                    int baseX = tracker.getWindowBaseX();
                    int baseY = tracker.getWindowBaseY();
                    SafeMousePoint safePoint = randomFirstAidHoverSafePoint(baseX, baseY);
                    logFirstAidHoverCleanupMove("playerState:heal:" + name,
                            baseX, baseY, safePoint, "queued-input-worker");
                    inputSequences.submitAndWait("playerState:heal:" + name, List.of(
                            InputAction.clickRight(absX, absY, 100),
                            InputAction.sleep(800),
                            InputAction.moveMouse(safePoint.absX(), safePoint.absY()),
                            InputAction.sleep(SAFE_MOUSE_HOVER_CLEAR_DELAY_MS)
                    ));
                } else {
                    if (moveMouseAwayAfterClick) {
                        log.warn("first-aid hover cleanup skipped: source={} windowId={} base unavailable base=({}, {})",
                                "playerState:heal:" + name, currentWindowId(),
                                tracker.getWindowBaseX(), tracker.getWindowBaseY());
                    }
                    inputSequences.submitAndWait("playerState:heal:" + name, List.of(
                            InputAction.clickRight(absX, absY, 100),
                            InputAction.sleep(800)
                    ));
                }
            }
            return true;
        }

        return false;
    }

    private String describeFirstAidTargets(List<FirstAidTarget> targets) {
        if (targets == null || targets.isEmpty()) {
            return "[]";
        }
        return targets.stream()
                .map(target -> target.name() + "@" + target.relX() + "," + target.relY()
                        + "/" + target.threshold() + "%")
                .toList()
                .toString();
    }

    /**
     * Cloud-owned 摄妖香 tick. Local code may capture/upload the status ROI and execute an explicit
     * `USE_INCENSE`, but it must not infer buff presence, remaining time, or refill policy locally.
     */
    private SheyaoxiangStatusCloudDecision captureSheyaoxiangStatusAndAskCloud(
            PlayerRuntimeState state,
            TaskExecutionContext taskContext,
            int[] statusRect,
            boolean openMainBagSession,
            SheyaoxiangStatusCloudDecision captureDecision) {
        checkpoint(taskContext);
        BufferedImage statusImage = tracker.captureToMemory(
                "sheyaoxiang-status-cloud", statusRect[0], statusRect[1], statusRect[2], statusRect[3]);
        if (statusImage == null) {
            log.warn("sheyaoxiang cloud status capture failed: windowId={} rect=({}, {})-({}, {}) decisionId={}",
                    currentWindowId(), statusRect[0], statusRect[1], statusRect[2], statusRect[3],
                    captureDecision == null ? null : captureDecision.getDecisionId());
            return SheyaoxiangStatusCloudDecision.builder()
                    .status(SheyaoxiangStatusCloudDecision.Status.REQUIRED_FAILURE)
                    .action(SheyaoxiangStatusCloudDecision.Action.FAIL_CLOSED)
                    .reason("status capture failed")
                    .build();
        }
        try {
            String rawPath = windowScopedTempPath.resolve("sheyaoxiang_status_cloud_raw.png");
            writeImage(statusImage, rawPath, "sheyaoxiang cloud raw status");
            TransferableImage payload = transferablePng(statusImage);
            return sheyaoxiangStatusCloudDecisionService.decide(
                    buildSheyaoxiangCloudRequest(state, taskContext, statusRect, openMainBagSession,
                            SheyaoxiangStatusCloudRequest.Hook.STATUS_IMAGE, payload, rawPath,
                            captureDecision == null ? null : captureDecision.getDecisionId()));
        } catch (RuntimeException e) {
            log.warn("sheyaoxiang cloud status upload failed closed: windowId={} reason={}",
                    currentWindowId(), e.getMessage(), e);
            return SheyaoxiangStatusCloudDecision.builder()
                    .status(SheyaoxiangStatusCloudDecision.Status.REQUIRED_FAILURE)
                    .action(SheyaoxiangStatusCloudDecision.Action.FAIL_CLOSED)
                    .reason("status upload failed: " + e.getClass().getSimpleName())
                    .build();
        } finally {
            statusImage.flush();
        }
    }

    private boolean executeCloudRequestedIncenseUse(TaskExecutionContext taskContext,
                                                    IncenseItemUser itemUser,
                                                    PlayerRuntimeState state,
                                                    int[] statusRect,
                                                    boolean openMainBagSession,
                                                    SheyaoxiangStatusCloudDecision decision) {
        try {
            boolean used = itemUser.use("bag/sheyaoxiang_item.png", taskContext);
            checkpoint(taskContext);
            if (used) {
                log.info("✅ 云端显式要求补香且使用成功。decisionId={} 等待吃香动画...", decision.getDecisionId());
                state.lastIncenseUsedTime = System.currentTimeMillis();
                state.nextIncenseRetryTime = 0;
                reportSheyaoxiangOutcome(state, taskContext, statusRect, decision,
                        openMainBagSession, SheyaoxiangStatusCloudRequest.Outcome.USED, "used");
                TaskSleep.sleep(1000);
                return true;
            }
            log.error("❌ 云端显式要求补香，但包裹内未找到摄妖香。decisionId={}", decision.getDecisionId());
            reportSheyaoxiangOutcome(state, taskContext, statusRect, decision,
                    openMainBagSession, SheyaoxiangStatusCloudRequest.Outcome.ITEM_NOT_FOUND, "item-not-found");
            return false;
        } catch (TaskStopRequestedException e) {
            reportSheyaoxiangOutcome(state, taskContext, statusRect, decision,
                    openMainBagSession, SheyaoxiangStatusCloudRequest.Outcome.STOPPED, e.getMessage());
            throw e;
        } catch (RuntimeException e) {
            reportSheyaoxiangOutcome(state, taskContext, statusRect, decision,
                    openMainBagSession, SheyaoxiangStatusCloudRequest.Outcome.FAILED, e.getClass().getSimpleName());
            throw e;
        }
    }

    private void reportSheyaoxiangOutcome(PlayerRuntimeState state,
                                          TaskExecutionContext taskContext,
                                          int[] statusRect,
                                          SheyaoxiangStatusCloudDecision decision,
                                          boolean openMainBagSession,
                                          SheyaoxiangStatusCloudRequest.Outcome outcome,
                                          String reason) {
        try {
            sheyaoxiangStatusCloudDecisionService.decide(
                    buildSheyaoxiangCloudRequest(state, taskContext, statusRect, openMainBagSession,
                            SheyaoxiangStatusCloudRequest.Hook.OUTCOME, null, null,
                            decision == null ? null : decision.getDecisionId())
                            .toBuilder()
                            .outcome(outcome)
                            .reason(reason)
                            .build());
        } catch (RuntimeException e) {
            log.warn("sheyaoxiang outcome report failed: windowId={} outcome={} decisionId={} reason={}",
                    currentWindowId(), outcome, decision == null ? null : decision.getDecisionId(),
                    e.getMessage(), e);
        }
    }

    private void applySheyaoxiangCloudFacts(PlayerRuntimeState state,
                                            int[] statusRect,
                                            SheyaoxiangStatusCloudDecision decision) {
        if (decision == null) {
            return;
        }
        if (decision.getIconBox() != null) {
            state.incenseIconOffsetX = Math.max(0, decision.getIconBox().getX());
            state.incenseIconOffsetY = Math.max(0, decision.getIconBox().getY());
        }
        if (decision.getRemainingMs() != null && decision.getRemainingMs() > 0L) {
            state.lastIncenseUsedTime = incenseLastUsedTimeForRemainingMs(System.currentTimeMillis(),
                    decision.getRemainingMs());
        }
        if (decision.getAction() == SheyaoxiangStatusCloudDecision.Action.USE_INCENSE
                || decision.getAction() == SheyaoxiangStatusCloudDecision.Action.NO_ACTION) {
            state.nextIncenseRetryTime = 0;
        }
        log.info("sheyaoxiang cloud decision applied: windowId={} action={} present={} remainingMs={} source={} iconOffset=({}, {}) statusRect=({}, {})-({}, {}) reason={} decisionId={}",
                currentWindowId(), decision.getAction(), decision.getPresent(), decision.getRemainingMs(),
                decision.getRemainingSource(), state.incenseIconOffsetX, state.incenseIconOffsetY,
                statusRect[0], statusRect[1], statusRect[2], statusRect[3],
                decision.getReason(), decision.getDecisionId());
    }

    private SheyaoxiangStatusCloudRequest buildSheyaoxiangCloudRequest(
            PlayerRuntimeState state,
            TaskExecutionContext taskContext,
            int[] statusRect,
            boolean openMainBagSession,
            SheyaoxiangStatusCloudRequest.Hook hook,
            TransferableImage payload,
            String rawImagePath,
            String decisionId) {
        return SheyaoxiangStatusCloudRequest.builder()
                .hook(hook)
                .imagePayloadBase64(payload == null ? null : payload.base64())
                .payloadMimeType(payload == null ? null : "image/png")
                .imageSha256(payload == null ? null : payload.sha256())
                .rawImagePath(rawImagePath)
                .windowRelativeRoi(SheyaoxiangStatusCloudRequest.Roi.builder()
                        .x(STATUS_PANEL_X)
                        .y(STATUS_PANEL_Y)
                        .width(STATUS_PANEL_W)
                        .height(STATUS_PANEL_H)
                        .build())
                .screenAbsoluteRoi(SheyaoxiangStatusCloudRequest.Roi.builder()
                        .x(statusRect[0])
                        .y(statusRect[1])
                        .width(Math.max(1, statusRect[2] - statusRect[0]))
                        .height(Math.max(1, statusRect[3] - statusRect[1]))
                        .build())
                .windowWidth(windowWidth(taskContext))
                .windowHeight(windowHeight(taskContext))
                .nowMs(System.currentTimeMillis())
                .lastIncenseUsedTimeMs(state.lastIncenseUsedTime)
                .nextIncenseRetryTimeMs(state.nextIncenseRetryTime)
                .incenseIconOffsetX(state.incenseIconOffsetX)
                .incenseIconOffsetY(state.incenseIconOffsetY)
                .openMainBagSession(openMainBagSession)
                .taskCode(taskContext == null ? null : taskContext.getTaskCode())
                .source("player-state")
                .phase("sheyaoxiang-status")
                .windowId(taskContext == null ? currentWindowId() : taskContext.getWindowId())
                .taskRunId(taskRunId(taskContext))
                .hwnd(hwnd(taskContext))
                .decisionId(decisionId)
                .build();
    }

    private int windowWidth(TaskExecutionContext taskContext) {
        if (taskContext != null && taskContext.getNativeWindowWidth() > 0) {
            return taskContext.getNativeWindowWidth();
        }
        return windowTaskContextHolder.rawCurrent()
                .map(window -> window.getNativeBinding().getWidth())
                .filter(width -> width > 0)
                .orElse(GAME_CLIENT_WIDTH);
    }

    private int windowHeight(TaskExecutionContext taskContext) {
        if (taskContext != null && taskContext.getNativeWindowHeight() > 0) {
            return taskContext.getNativeWindowHeight();
        }
        return windowTaskContextHolder.rawCurrent()
                .map(window -> window.getNativeBinding().getHeight())
                .filter(height -> height > 0)
                .orElse(GAME_CLIENT_HEIGHT);
    }

    private String taskRunId(TaskExecutionContext taskContext) {
        return taskContext == null || taskContext.getTaskRunId() <= 0L
                ? ""
                : Long.toString(taskContext.getTaskRunId());
    }

    private String hwnd(TaskExecutionContext taskContext) {
        if (taskContext != null && taskContext.getNativeWindowHandle() != null
                && !taskContext.getNativeWindowHandle().isBlank()) {
            return taskContext.getNativeWindowHandle();
        }
        return windowTaskContextHolder.rawCurrent()
                .map(window -> window.getNativeBinding().getNativeHandle())
                .orElse("");
    }

    private long incenseLastUsedTimeForRemainingMs(long now, long remainingMs) {
        // Cyan "1 hour" display can exceed the internal 59-minute duration; never move the memory clock into the future.
        long boundedRemainingMs = Math.min(INCENSE_DURATION_MS, Math.max(1L, remainingMs));
        return now - (INCENSE_DURATION_MS - boundedRemainingMs);
    }

    private TransferableImage transferablePng(BufferedImage image) {
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            ImageIO.write(image, "png", output);
            byte[] bytes = output.toByteArray();
            return new TransferableImage(Base64.getEncoder().encodeToString(bytes), sha256Hex(bytes));
        } catch (IOException e) {
            throw new IllegalStateException("encode png failed", e);
        }
    }

    private static String sha256Hex(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(bytes);
            StringBuilder result = new StringBuilder(hashed.length * 2);
            for (byte value : hashed) {
                result.append(String.format("%02x", value));
            }
            return result.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    private void writeImage(BufferedImage image, String path, String label) {
        try {
            ImageIO.write(image, "png", new java.io.File(path));
        } catch (IOException e) {
            log.warn("write {} image failed: path={} reason={}", label, path, e.getMessage(), e);
        }
    }

    private int calculateX(int leftX, int rightX, int threshold) {
        int normalizedThreshold = normalizeThreshold(threshold);
        double ratio = normalizedThreshold / 100.0;
        int targetX = leftX + (int) Math.round((rightX - leftX) * ratio);
        log.debug("🧮 坐标计算：左界{} 右界{} 阈值{}% -> 目标X坐标:{}", leftX, rightX, normalizedThreshold, targetX);
        return targetX;
    }

    private int normalizeThreshold(int threshold) {
        if (threshold <= 40) {
            return 30;
        }
        if (threshold <= 60) {
            return 50;
        }
        return 70;
    }

    private PlayerRuntimeState state() {
        String key = windowTaskContextHolder.rawCurrent()
                .map(windowContext -> windowContext.getWindowId())
                .orElse("default");
        long epoch = windowTaskContextHolder.rawCurrent()
                .map(windowContext -> windowContext.getPlayerIdentityEpoch())
                .orElse(0L);
        return runtimeStates.compute(key, (ignored, existing) -> {
            if (existing == null) {
                PlayerRuntimeState created = new PlayerRuntimeState();
                created.playerIdentityEpoch = epoch;
                return created;
            }
            if (existing.playerIdentityEpoch != epoch) {
                log.warn("player-state runtime cache invalidated by player identity drift: windowKey={} oldEpoch={} newEpoch={} lastIncenseUsedTime={} pendingFirstAid={}",
                        key, existing.playerIdentityEpoch, epoch, existing.lastIncenseUsedTime,
                        existing.pendingNoFocusFirstAidPlan != null);
                PlayerRuntimeState reset = new PlayerRuntimeState();
                reset.playerIdentityEpoch = epoch;
                return reset;
            }
            return existing;
        });
    }

    private void checkpoint(TaskExecutionContext taskContext) {
        TaskCheckpoint.throwIfStopRequested(taskContext, "Player state sync interrupted");
    }

    private boolean isInputWorkerThread() {
        return Thread.currentThread().getName().contains("dhxy-input-action-worker");
    }

    private String currentWindowId() {
        return windowTaskContextHolder.rawCurrent()
                .map(windowContext -> windowContext.getWindowId())
                .orElse("default");
    }

    private String currentPlayerForLog() {
        PlayerCharacter me = context.getMe();
        if (me == null) {
            return "-";
        }
        String name = me.getName() == null || me.getName().isBlank() ? "-" : me.getName();
        String id = me.getId() == null || me.getId().isBlank() ? "-" : me.getId();
        return name + "/" + id;
    }

    @FunctionalInterface
    private interface IncenseItemUser {
        boolean use(String targetItemTemplate, TaskExecutionContext context);
    }

    private String safeReason(String reason) {
        return reason == null || reason.isBlank() ? "-" : reason;
    }

    private String safeLatencyValue(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private static class PlayerRuntimeState {
        private long playerIdentityEpoch;
        private long lastIncenseUsedTime = 0;
        private long nextIncenseRetryTime = 0;
        private int incenseIconOffsetX = -1;
        private int incenseIconOffsetY = -1;
        private int checksDoneThisRound = 0;
        private long lastCombatExitTime = 0;
        private FirstAidPlan pendingNoFocusFirstAidPlan;
        private FirstAidNoFocusProbeResult startupFirstAidPrecheckResult;
        private long startupFirstAidPrecheckAtMs = 0;
    }

    private record FirstAidPlan(List<FirstAidTarget> targets, long createdAtMs, int baseX, int baseY) {
    }

    private record FirstAidTarget(String name, int relX, int relY, boolean expectRed, int threshold) {
    }

    private record FirstAidProbeSummary(List<FirstAidTarget> targets,
                                        List<FirstAidBarProbe> probes,
                                        boolean unknown,
                                        String reason) {

        private String describe() {
            List<String> parts = new ArrayList<>();
            for (FirstAidBarProbe probe : probes) {
                parts.add(probe.describe());
            }
            return parts.toString();
        }
    }

    private record FirstAidBarProbe(String name,
                                    boolean enabled,
                                    boolean expectRed,
                                    int threshold,
                                    int leftX,
                                    int rightX,
                                    int relY,
                                    int relX,
                                    int higherRelX,
                                    int healthyColumns,
                                    int totalColumns,
                                    int thresholdHealthyCount,
                                    int higherHealthyCount,
                                    int sampleAreaPixels,
                                    String decision,
                                    String reason,
                                    int r,
                                    int g,
                                    int b) {

        private boolean readable() {
            return enabled && ("HEALTHY".equals(decision) || "SUPPLY_NEEDED".equals(decision));
        }

        private boolean supplyNeeded() {
            return enabled && "SUPPLY_NEEDED".equals(decision);
        }

        private int observedPercent() {
            if (totalColumns <= 0) {
                return 0;
            }
            return Math.round(healthyColumns * 100.0f / totalColumns);
        }

        private String rgbText() {
            return "(" + r + "," + g + "," + b + ")";
        }

        private String describe() {
            return name + "{enabled=" + enabled
                    + ",decision=" + decision
                    + ",reason=" + reason
                    + ",threshold=" + threshold
                    + ",observedPercent=" + observedPercent()
                    + ",healthyColumns=" + healthyColumns + "/" + totalColumns
                    + ",thresholdSample=" + thresholdHealthyCount + "/" + sampleAreaPixels
                    + ",higherSample=" + higherHealthyCount + "/" + sampleAreaPixels
                    + ",sampleRel=(" + relX + "," + relY + ")"
                    + ",higherRel=(" + higherRelX + "," + relY + ")"
                    + ",rgb=" + rgbText()
                    + "}";
        }
    }

    private record SafeMousePoint(int relX, int relY, int absX, int absY) {
    }

    private record TransferableImage(String base64, String sha256) {
    }

    public enum FirstAidNoFocusProbeResult {
        SUPPLY_NEEDED,
        HEALTHY,
        ALREADY_DONE,
        UNKNOWN
    }
}
