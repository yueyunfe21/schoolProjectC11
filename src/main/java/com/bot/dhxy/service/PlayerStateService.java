package com.bot.dhxy.service;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.Accessors;



import com.bot.dhxy.model.ocr.LocationInfo;
import com.bot.dhxy.model.ocr.OcrWordResult;
import com.bot.dhxy.core.GameClientTracker;
import com.bot.dhxy.core.GameContext;
import com.bot.dhxy.core.ImageFinder;
import com.bot.dhxy.core.TextRecognizer;
import com.bot.dhxy.config.BotProperties;
import com.bot.dhxy.input.InputProvider;
import com.bot.dhxy.input.InputSequences;
import com.bot.dhxy.input.action.InputAction;
import com.bot.dhxy.model.PlayerCharacter;
import com.bot.dhxy.runner.context.TaskExecutionContext;
import com.bot.dhxy.runner.stop.TaskCheckpoint;
import com.bot.dhxy.runner.stop.TaskSleep;
import com.bot.dhxy.tools.CoordinateHelper;
import com.bot.dhxy.tools.LatencyMetrics;
import com.bot.dhxy.vision.LocationVisionService;
import com.bot.dhxy.vision.SheyaoxiangDigitTemplateReader;
import com.bot.dhxy.window.runtime.WindowScopedTempPath;
import com.bot.dhxy.window.runtime.WindowTaskContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.OptionalLong;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
    private final TextRecognizer textRecognizer;
    private final WindowScopedTempPath windowScopedTempPath;
    private final SheyaoxiangDigitTemplateReader sheyaoxiangDigitTemplateReader = new SheyaoxiangDigitTemplateReader();

    private final Map<String, PlayerRuntimeState> runtimeStates = new ConcurrentHashMap<>();
    private static final long INCENSE_DURATION_MS = 59 * 60 * 1000L;
    private static final long INCENSE_REFRESH_REMAINING_MS = 20 * 60 * 1000L;
    private static final long INCENSE_MEMORY_TRUST_MS = 50 * 60 * 1000L;
    private static final long ONE_HOUR_MS = 60 * 60 * 1000L;

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
    private static final int HEAL_CONFIRM_DELAY_MS = 350;
    /*
     * Player-state snapshots read the status bars and 摄妖香 area. Move the cursor to a random
     * in-window point before capture, but never into the user-confirmed top-right forbidden area:
     * absolute (2076,180) was measured on base=(1315,33), so its window-relative left-bottom corner
     * is approximately (761,147). The forbidden rectangle is relX>=761 && relY<=147.
     */
    private static final int GAME_CLIENT_WIDTH = 1024;
    private static final int GAME_CLIENT_HEIGHT = 768;
    private static final int SAFE_MOUSE_FORBIDDEN_LEFT_REL_X = 761;
    private static final int SAFE_MOUSE_FORBIDDEN_BOTTOM_REL_Y = 147;
    private static final int SAFE_MOUSE_HOVER_CLEAR_DELAY_MS = 300;

    /*
     * 摄妖香剩余时间文本颜色。游戏中 RGB=(0,255,255) 的青色数字表示剩余小时；
     * 如果没有青色小时，再读取绿色数字并按剩余分钟处理。
     */
    private static final int SHEYAOXIANG_AT_LEAST_ONE_HOUR_RGB = 0x00FFFF;
    private static final String SHEYAOXIANG_STATUS_TEMPLATE = "images/template/status/sheyaoxiang_buff.png";
    private static final double SHEYAOXIANG_STATUS_MATCH_RATE = 0.85;
    private static final int SHEYAOXIANG_DIGIT_OCR_SCALE = 6;
    private static final Pattern SHEYAOXIANG_REMAINING_HOUR_PATTERN = Pattern.compile("\\d{1,2}");

    /*
     * 摄妖香状态图标检测框。这个区域是窗口相对坐标，当前按用户实测的
     * base=(379,154)、absolute=(1380,277)-(1403,311) 回推得到。
     * 只截状态栏右侧的摄妖香图标窄框，避免把周围按钮/背景也拿去做模板匹配。
     */
    private static final int STATUS_PANEL_X = 901;
    private static final int STATUS_PANEL_Y = 123;
    private static final int STATUS_PANEL_W = 123;
    private static final int STATUS_PANEL_H = 34;
    private static final int INCENSE_CACHED_ICON_PROBE_WIDTH = 48;
    private static final int INCENSE_CACHED_ICON_PROBE_LEFT_PADDING = 6;

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
        log.info("🩺 启动急救检查：重置本窗口急救计数，准备检查人物/宝宝血法");
        performFirstAidCheck(true, taskContext);
    }

    /**
     * Run the quiet HP/MP probe and consume the post-combat check only when the window is healthy.
     *
     * <p>Follower windows call this immediately after a combat-exit signal. The probe uses a
     * no-focus HWND screenshot only. A healthy result is enough to avoid joining the task-turn
     * queue; a low or unknown result still lets the caller defer real recovery until it can safely
     * own physical input.</p>
     *
     * @param taskContext optional task stop token; null is allowed for legacy callers.
     * @param source short diagnostic label for logs.
     * @return precise no-focus probe outcome so callers can decide whether to defer real input.
     */
    public FirstAidNoFocusProbeResult probeAndConsumeHealthyFirstAidNoFocus(TaskExecutionContext taskContext,
                                                                           String source) {
        FirstAidNoFocusProbeResult result = probeFirstAidSupplyNoFocus(taskContext);
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
     * cannot be read.
     */
    public FirstAidNoFocusProbeResult probeFirstAidSupplyNoFocus(TaskExecutionContext taskContext) {
        checkpoint(taskContext);
        PlayerRuntimeState state = state();
        if (state.checksDoneThisRound >= MAX_CHECKS_BETWEEN_BATTLES) {
            log.info("first-aid no-focus precheck skipped: checks already done {}/{}",
                    state.checksDoneThisRound, MAX_CHECKS_BETWEEN_BATTLES);
            return FirstAidNoFocusProbeResult.ALREADY_DONE;
        }
        if (tracker.getWindowBaseX() == -1) {
            log.warn("first-aid no-focus precheck skipped: window base unavailable");
            state.pendingNoFocusFirstAidPlan = null;
            return FirstAidNoFocusProbeResult.UNKNOWN;
        }

        BufferedImage bars = captureBarsSnapshotNoFocus();
        if (bars == null) {
            log.warn("first-aid no-focus precheck failed: bars snapshot unavailable");
            state.pendingNoFocusFirstAidPlan = null;
            return FirstAidNoFocusProbeResult.UNKNOWN;
        }
        int planBaseX = tracker.getWindowBaseX();
        int planBaseY = tracker.getWindowBaseY();
        try {
            List<FirstAidTarget> targets = findSupplyTargetsFromSnapshot(bars);
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
            log.info("first-aid no-focus precheck result: needed={} targets={} planBase=({}, {})",
                    !targets.isEmpty(), describeFirstAidTargets(targets), planBaseX, planBaseY);
            return targets.isEmpty() ? FirstAidNoFocusProbeResult.HEALTHY : FirstAidNoFocusProbeResult.SUPPLY_NEEDED;
        } finally {
            bars.flush();
        }
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

        Point safePoint = randomMouseAwayPoint(baseX, baseY);
        inputProvider.moveMouse(safePoint.x, safePoint.y);
        TaskSleep.sleep(SAFE_MOUSE_HOVER_CLEAR_DELAY_MS);
        for (FirstAidTarget target : plan.targets()) {
            int absX = baseX + target.relX();
            int absY = baseY + target.relY();
            log.warn("🚨 后台预计算命中 [{}]，按当前窗口补给：base=({}, {}) rel=({}, {}) abs=({}, {}) threshold={}%",
                    target.name(), baseX, baseY, target.relX(), target.relY(), absX, absY, target.threshold());
            inputProvider.clickRight(absX, absY, 100);
            TaskSleep.sleep(800);
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

        try {
            checkAndHealFromSnapshotIfEnabled(bars, "人物血量", CHAR_BAR_LEFT_X, CHAR_BAR_RIGHT_X,
                    BAR_HP_Y, true, config.isPlayerHpSupplyEnabled(), config.getPlayerHpSupplyThreshold());
            checkAndHealFromSnapshotIfEnabled(bars, "人物法力", CHAR_BAR_LEFT_X, CHAR_BAR_RIGHT_X,
                    BAR_MP_Y, false, config.isPlayerMpSupplyEnabled(), config.getPlayerMpSupplyThreshold());
            checkAndHealFromSnapshotIfEnabled(bars, "宝宝血量", PET_BAR_LEFT_X, PET_BAR_RIGHT_X,
                    BAR_HP_Y, true, config.isPetHpSupplyEnabled(), config.getPetHpSupplyThreshold());
            checkAndHealFromSnapshotIfEnabled(bars, "宝宝法力", PET_BAR_LEFT_X, PET_BAR_RIGHT_X,
                    BAR_MP_Y, false, config.isPetMpSupplyEnabled(), config.getPetMpSupplyThreshold());
        } finally {
            bars.flush();
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
                        BagService.MAIN_BAG, targetItemTemplate, null, context));
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
        return ensureSheYaoXiangActive(taskContext, (targetItemTemplate, context) -> mainBag.useItem(targetItemTemplate, null));
    }

    private boolean ensureSheYaoXiangActive(TaskExecutionContext taskContext, IncenseItemUser itemUser) {
        long latencyStart = LatencyMetrics.start();
        try {
            checkpoint(taskContext);
            PlayerRuntimeState state = state();
            long now = System.currentTimeMillis();
            int[] statusRect = coordinateHelper.getScaledRect(STATUS_PANEL_X, STATUS_PANEL_Y, STATUS_PANEL_W, STATUS_PANEL_H);
            IncenseStatusProbe statusProbe = null;
            if (state.lastIncenseUsedTime > 0 && now - state.lastIncenseUsedTime < INCENSE_MEMORY_TRUST_MS) {
                long elapsedMinutes = Math.max(0, (now - state.lastIncenseUsedTime) / 60000);
                long trustMinutes = INCENSE_MEMORY_TRUST_MS / 60000;
                IncenseIconProbe iconProbe = probeIncenseIconPresence(state, statusRect);
                if (iconProbe.presence() == IncenseIconPresence.PRESENT) {
                    java.awt.Point iconPoint = iconProbe.iconPoint();
                    rememberIncenseIconPoint(state, statusRect, iconPoint);
                    state.nextIncenseRetryTime = 0;
                    log.info("🕯️ memory-gate-icon-present: 摄妖香由本程序补充后仅过去 {} 分钟，未超过 {} 分钟内存信任窗口；状态栏图标仍在，跳过包裹检查。point=({}, {})",
                            elapsedMinutes, trustMinutes, iconPoint.x, iconPoint.y);
                    return false;
                }
                if (iconProbe.presence() == IncenseIconPresence.UNKNOWN) {
                    log.warn("⚠️ memory-gate-icon-unknown-full-probe: fresh memory age={}m < {}m, reason={}，改走完整状态探测。",
                            elapsedMinutes, trustMinutes, iconProbe.reason());
                    statusProbe = probeIncenseStatus(statusRect);
                    if (statusProbe.iconPoint() != null) {
                        rememberIncenseIconPoint(state, statusRect, statusProbe.iconPoint());
                        state.nextIncenseRetryTime = 0;
                        if (statusProbe.remainingMs().isPresent()) {
                            state.lastIncenseUsedTime = incenseLastUsedTimeForRemainingMs(now, statusProbe.remainingMs().getAsLong());
                        }
                        log.info("🕯️ memory-gate-icon-unknown-full-probe-present: 完整探测证明摄妖香图标仍在，跳过包裹检查。point=({}, {}) remaining={}",
                                statusProbe.iconPoint().x, statusProbe.iconPoint().y, statusProbe.remainingText());
                        return false;
                    }
                    log.warn("⚠️ memory-gate-icon-unknown-full-probe-unproven: 完整探测仍不能证明摄妖香存在，准备补香。");
                } else {
                    log.warn("⚠️ memory-gate-icon-absent-refill: fresh memory age={}m < {}m, 但状态栏摄妖香图标不存在，准备补香。",
                            elapsedMinutes, trustMinutes);
                    statusProbe = IncenseStatusProbe.notFound();
                }
            }

            if (state.lastIncenseUsedTime > 0) {
                long elapsedMinutes = Math.max(0, (now - state.lastIncenseUsedTime) / 60000);
                log.info("🕯️ 摄妖香由本程序补充后已过去 {} 分钟，进入状态栏/补香校验流程。", elapsedMinutes);
            } else {
                log.info("🕯️ 摄妖香没有本程序补充时间记录，开始执行安全校验...");
            }

            if (now < state.nextIncenseRetryTime) {
                long remainingSeconds = Math.max(1, (state.nextIncenseRetryTime - now + 999) / 1000);
                log.warn("⚠️ 摄妖香上次补充失败，仍在重试冷却中，剩余 {} 秒，跳过包裹检查。", remainingSeconds);
                return false;
            }

            if (statusProbe == null) {
                statusProbe = probeIncenseStatus(statusRect);
            }
            java.awt.Point buffIcon = statusProbe.iconPoint();

            /*
             * Template match means the incense buff exists. Cyan text is an hour count; green text is
             * a minute count. If neither color yields a readable number, refill conservatively so the
             * in-memory watch gets rebuilt from a known use time.
             */
            if (buffIcon != null && statusProbe.remainingMs().isPresent()) {
                long remainingMs = statusProbe.remainingMs().getAsLong();
                rememberIncenseIconPoint(state, statusRect, buffIcon);
                state.lastIncenseUsedTime = incenseLastUsedTimeForRemainingMs(now, remainingMs);
                state.nextIncenseRetryTime = 0;
                if (remainingMs > INCENSE_REFRESH_REMAINING_MS) {
                    log.info("sheyaoxiang status matched with {}; remainingMinutes={} > refreshLineMinutes={}; skip refill. point=({}, {})",
                            statusProbe.remainingText(), Math.max(0, remainingMs / 60000),
                            INCENSE_REFRESH_REMAINING_MS / 60000, buffIcon.x, buffIcon.y);
                    return false;
                }
                log.info("sheyaoxiang status matched with {}; remainingMinutes={} <= refreshLineMinutes={}; refill now. point=({}, {})",
                        statusProbe.remainingText(), Math.max(0, remainingMs / 60000),
                        INCENSE_REFRESH_REMAINING_MS / 60000, buffIcon.x, buffIcon.y);
            }
            if (buffIcon != null && statusProbe.remainingText().startsWith("green-digits-learning")) {
                rememberIncenseIconPoint(state, statusRect, buffIcon);
                log.info("sheyaoxiang status matched but minute digits are still learning; skip refill to avoid partial OCR refill. remaining={} point=({}, {})",
                        statusProbe.remainingText(), buffIcon.x, buffIcon.y);
                return false;
            }

            if (state.lastIncenseUsedTime > 0 && buffIcon == null) {
                log.warn("⚠️ 摄妖香状态图标未发现，准备打开包裹补充...");
            } else if (state.lastIncenseUsedTime > 0) {
                log.info("🕯️ 摄妖香状态仍在，但已经进入主动补香窗口，准备打开包裹补充。");
            } else if (buffIcon != null) {
                log.info("✅ 发现摄妖香状态图标还在，但没有本程序补香时间记录，主动补一根以重建计时基准。point=({}, {})",
                        buffIcon.x, buffIcon.y);
            } else {
                log.warn("⚠️ 未发现摄妖香状态，准备打开包裹补充...");
            }
            boolean used = itemUser.use("bag/sheyaoxiang_item.png", taskContext);
            checkpoint(taskContext);

            if (used) {
                log.info("✅ 成功使用摄妖香，怀表已重置为 1 小时。等待吃香动画...");
                state.lastIncenseUsedTime = System.currentTimeMillis();
                state.nextIncenseRetryTime = 0;
                TaskSleep.sleep(1000);
                return true;
            } else {
                log.error("❌ 包裹内未找到摄妖香，请及时购买补充。1 分钟后才会再试。 ");
                state.nextIncenseRetryTime = System.currentTimeMillis() + 60000;
                return false;
            }
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

    /**
     * Capture only the current incense-status probe rectangle and save it for visual inspection.
     *
     * <p>This is a debug-only read path. It saves the raw probe, matches the 摄妖香 icon, crops the
     * matched icon column, and runs the same cyan-hour/green-minute reader used by normal logic. It
     * does not open the bag or use physical input.</p>
     *
     * @param taskContext optional stop token for the debug task.
     * @param source short diagnostic label written into the output filename and logs.
     * @return window-scoped PNG path when capture/save succeeds, or {@code null} when capture fails.
     */
    public String captureSheYaoXiangStatusDebugImage(TaskExecutionContext taskContext, String source) {
        checkpoint(taskContext);
        String safeSource = safeReason(source);
        int[] statusRect = coordinateHelper.getScaledRect(STATUS_PANEL_X, STATUS_PANEL_Y, STATUS_PANEL_W, STATUS_PANEL_H);
        BufferedImage statusImage = tracker.captureToMemory(
                "sheyaoxiang-status-debug", statusRect[0], statusRect[1], statusRect[2], statusRect[3]);
        if (statusImage == null) {
            log.warn("sheyaoxiang debug status capture failed: source={} rect=({}, {})-({}, {})",
                    safeSource, statusRect[0], statusRect[1], statusRect[2], statusRect[3]);
            return null;
        }

        try {
            String rawPath = windowScopedTempPath.resolve("sheyaoxiang_status_debug_" + safeSource + "_raw.png");
            if (!com.bot.dhxy.tools.ImagePreprocessor.saveImage(statusImage, rawPath)) {
                log.warn("sheyaoxiang debug status save failed: source={} path={} rect=({}, {})-({}, {}) size={}x{}",
                        safeSource, rawPath, statusRect[0], statusRect[1], statusRect[2], statusRect[3],
                        statusImage.getWidth(), statusImage.getHeight());
                return null;
            }
            log.info("sheyaoxiang debug status saved: source={} path={} rect=({}, {})-({}, {}) size={}x{}",
                    safeSource, rawPath, statusRect[0], statusRect[1], statusRect[2], statusRect[3],
                    statusImage.getWidth(), statusImage.getHeight());
            double[] match = ImageFinder.find(rawPath, SHEYAOXIANG_STATUS_TEMPLATE, SHEYAOXIANG_STATUS_MATCH_RATE);
            if (match != null && match.length >= 2) {
                BufferedImage matchedColumn = cropSheyaoxiangMatchedColumn(statusImage, match, statusRect, "debug:" + safeSource);
                if (matchedColumn != null) {
                    try {
                        String columnPath = windowScopedTempPath.resolve(
                                "sheyaoxiang_status_debug_" + safeSource + "_matched_column_raw.png");
                        if (com.bot.dhxy.tools.ImagePreprocessor.saveImage(matchedColumn, columnPath)) {
                            log.info("sheyaoxiang debug matched column saved: source={} path={}",
                                    safeSource, columnPath);
                        }
                        IncenseRemainingTime remainingTime = readSheyaoxiangRemainingTime(matchedColumn);
                        log.info("sheyaoxiang debug matched column read result: source={} remaining={}",
                                safeSource, remainingTime.describe());
                    } finally {
                        matchedColumn.flush();
                    }
                }
            }
            return rawPath;
        } finally {
            statusImage.flush();
        }
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
        return healIfUnhealthy(name, absX, absY, rgb, expectRed, normalizedThreshold);
    }

    private BufferedImage captureBarsSnapshot() {
        if (isInputWorkerThread()) {
            moveMouseAwayBeforePlayerStateSnapshotDirect();
        } else {
            moveMouseAwayBeforePlayerStateSnapshot();
        }
        int[] rect = coordinateHelper.getScaledRect(BARS_SCAN_LEFT_X, BARS_SCAN_TOP_Y, BARS_SCAN_W, BARS_SCAN_H);
        return tracker.captureToMemory("player-state-bars", rect[0], rect[1], rect[2], rect[3]);
    }

    private BufferedImage captureBarsSnapshotNoFocus() {
        int[] rect = coordinateHelper.getScaledRect(BARS_SCAN_LEFT_X, BARS_SCAN_TOP_Y, BARS_SCAN_W, BARS_SCAN_H);
        return tracker.captureToMemory("player-state-bars-precheck", rect[0], rect[1], rect[2], rect[3]);
    }

    private void moveMouseAwayBeforePlayerStateSnapshot() {
        if (tracker.getWindowBaseX() == -1 || tracker.getWindowBaseY() == -1) {
            return;
        }
        Point safePoint = randomMouseAwayPoint(tracker.getWindowBaseX(), tracker.getWindowBaseY());
        inputSequences.submitAndWait("playerState:moveMouseAwayBeforeSnapshot", List.of(
                InputAction.moveMouse(safePoint.x, safePoint.y),
                InputAction.sleep(SAFE_MOUSE_HOVER_CLEAR_DELAY_MS)
        ));
    }

    private void moveMouseAwayBeforePlayerStateSnapshotDirect() {
        if (tracker.getWindowBaseX() == -1 || tracker.getWindowBaseY() == -1) {
            return;
        }
        Point safePoint = randomMouseAwayPoint(tracker.getWindowBaseX(), tracker.getWindowBaseY());
        inputProvider.moveMouse(safePoint.x, safePoint.y);
        TaskSleep.sleep(SAFE_MOUSE_HOVER_CLEAR_DELAY_MS);
    }

    private Point randomMouseAwayPoint(int baseX, int baseY) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        int relX;
        int relY;
        do {
            relX = random.nextInt(GAME_CLIENT_WIDTH);
            relY = random.nextInt(GAME_CLIENT_HEIGHT);
        } while (relX >= SAFE_MOUSE_FORBIDDEN_LEFT_REL_X && relY <= SAFE_MOUSE_FORBIDDEN_BOTTOM_REL_Y);
        return new Point(baseX + relX, baseY + relY);
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

    private boolean isSupplyNeededFromSnapshotIfEnabled(BufferedImage bars, String name,
                                                        int leftX, int rightX, int relY, boolean expectRed,
                                                        boolean enabled, int threshold) {
        if (!enabled) {
            return false;
        }
        int normalizedThreshold = normalizeThreshold(threshold);
        int relX = calculateX(leftX, rightX, normalizedThreshold);
        if (!isSupplyNeededFromSnapshot(bars, name, relX, relY, expectRed, normalizedThreshold)) {
            return false;
        }

        TaskSleep.sleep(HEAL_CONFIRM_DELAY_MS);
        BufferedImage confirmBars = captureBarsSnapshotNoFocus();
        if (confirmBars == null) {
            log.warn("[{}] no-focus precheck confirm failed, skip supply to avoid false click", name);
            return false;
        }
        try {
            return isSupplyNeededFromSnapshot(confirmBars, name, relX, relY, expectRed, normalizedThreshold);
        } finally {
            confirmBars.flush();
        }
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
            return healIfUnhealthy(name, absX, absY, confirmRgb, expectRed, normalizedThreshold);
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

    private boolean healIfUnhealthy(String name, int absX, int absY, int rgb, boolean expectRed, int threshold) {
        int normalizedThreshold = normalizeThreshold(threshold);
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;

        boolean isHealthy = isHealthyColor(rgb, expectRed);

        if (!isHealthy) {
            log.warn("🚨 警报！[{}] 未达 {}% 警戒线，执行原位右键补充！rgb=({}, {}, {})",
                    name, normalizedThreshold, r, g, b);
            if (isInputWorkerThread()) {
                inputProvider.clickRight(absX, absY, 100);
                TaskSleep.sleep(800);
            } else {
                inputSequences.submitAndWait("playerState:heal:" + name, List.of(
                        InputAction.clickRight(absX, absY, 100),
                        InputAction.sleep(800)
                ));
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
     * Capture the incense status crop once, match the buff template, and optionally read its
     * cyan remaining-hour number.
     *
     * <p>The rectangle is screen-absolute and normally comes from {@link #STATUS_PANEL_X} etc.
     * This method has no input side effects: it only captures the current bound window, writes
     * window-scoped diagnostic images, performs template matching, and calls the local OCR sidecar
     * on a washed digit image. Empty OCR means "buff may exist, but remaining time is below one
     * visible hour" for our business logic.</p>
     *
     * @param statusRect screen-absolute crop rectangle as {@code [x1,y1,x2,y2]}.
     * @return icon point plus optional remaining hours. Null icon means the buff template was not
     * matched; empty hours means the icon matched but no cyan hour number was readable.
     */
    private IncenseStatusProbe probeIncenseStatus(int[] statusRect) {
        if (isInputWorkerThread()) {
            moveMouseAwayBeforePlayerStateSnapshotDirect();
        } else {
            moveMouseAwayBeforePlayerStateSnapshot();
        }
        BufferedImage statusImage = tracker.captureToMemory(
                "sheyaoxiang-status", statusRect[0], statusRect[1], statusRect[2], statusRect[3]);
        if (statusImage == null) {
            log.warn("sheyaoxiang status capture failed: rect=({}, {})-({}, {})",
                    statusRect[0], statusRect[1], statusRect[2], statusRect[3]);
            return IncenseStatusProbe.notFound();
        }

        try {
            String rawPath = windowScopedTempPath.resolve("sheyaoxiang_status_raw.png");
            writeImage(statusImage, rawPath, "sheyaoxiang raw status");

            /*
             * Stage 1: keep the existing icon-template decision as the gate. OCR is deliberately
             * not used to decide whether the buff exists because the tiny number alone can be
             * missing when less than one hour remains.
             */
            double[] match = ImageFinder.find(rawPath, SHEYAOXIANG_STATUS_TEMPLATE, SHEYAOXIANG_STATUS_MATCH_RATE);
            if (match == null || match.length < 2) {
                log.info("sheyaoxiang status template not matched: path={} template={}",
                        rawPath, SHEYAOXIANG_STATUS_TEMPLATE);
                return IncenseStatusProbe.notFound();
            }
            java.awt.Point iconPoint = new java.awt.Point(
                    statusRect[0] + (int) Math.round(match[0]),
                    statusRect[1] + (int) Math.round(match[1]));

            /*
             * Stage 2 reads only the vertical strip under the matched icon. Cyan digits are hours;
             * if no cyan number exists, green digits are treated as minutes.
             */
            BufferedImage matchedColumn = cropSheyaoxiangMatchedColumn(statusImage, match, statusRect, "probe");
            IncenseRemainingTime remainingTime;
            if (matchedColumn != null) {
                try {
                    String columnPath = windowScopedTempPath.resolve("sheyaoxiang_status_matched_column_raw.png");
                    writeImage(matchedColumn, columnPath, "sheyaoxiang matched column raw");
                    remainingTime = readSheyaoxiangRemainingTime(matchedColumn);
                } finally {
                    matchedColumn.flush();
                }
            } else {
                remainingTime = readSheyaoxiangRemainingTime(statusImage);
            }
            log.info("sheyaoxiang status matched: point=({}, {}) remaining={}",
                    iconPoint.x, iconPoint.y, remainingTime.describe());
            return new IncenseStatusProbe(iconPoint, remainingTime.remainingMs(), remainingTime.describe());
        } finally {
            statusImage.flush();
        }
    }

    private IncenseIconProbe probeIncenseIconPresence(PlayerRuntimeState state, int[] statusRect) {
        if (state.incenseIconOffsetX >= 0 && state.incenseIconOffsetY >= 0) {
            int[] cachedRect = cachedIncenseIconProbeRect(state, statusRect);
            IncenseIconProbe cachedProbe = probeIncenseIconPresenceInRect(statusRect, cachedRect, "cached-point");
            if (cachedProbe.presence() != IncenseIconPresence.ABSENT) {
                return cachedProbe;
            }
            log.info("sheyaoxiang cached icon probe missed; fallback to full status rect. cachedOffset=({}, {}) cachedRect=({}, {})-({}, {})",
                    state.incenseIconOffsetX, state.incenseIconOffsetY,
                    cachedRect[0], cachedRect[1], cachedRect[2], cachedRect[3]);
        }
        return probeIncenseIconPresenceInRect(statusRect, statusRect, "status-rect");
    }

    private int[] cachedIncenseIconProbeRect(PlayerRuntimeState state, int[] statusRect) {
        int panelLeft = statusRect[0];
        int panelTop = statusRect[1];
        int panelRight = statusRect[2];
        int panelBottom = statusRect[3];
        int panelWidth = Math.max(1, panelRight - panelLeft);
        int probeWidth = Math.min(panelWidth, INCENSE_CACHED_ICON_PROBE_WIDTH);
        int cachedAbsX = panelLeft + state.incenseIconOffsetX;
        int left = cachedAbsX - INCENSE_CACHED_ICON_PROBE_LEFT_PADDING;
        left = Math.max(panelLeft, Math.min(left, panelRight - probeWidth));
        return new int[]{left, panelTop, left + probeWidth, panelBottom};
    }

    private IncenseIconProbe probeIncenseIconPresenceInRect(int[] statusRect, int[] probeRect, String mode) {
        if (isInputWorkerThread()) {
            moveMouseAwayBeforePlayerStateSnapshotDirect();
        } else {
            moveMouseAwayBeforePlayerStateSnapshot();
        }
        BufferedImage statusImage = tracker.captureToMemory(
                "sheyaoxiang-status-icon-gate-" + mode, probeRect[0], probeRect[1], probeRect[2], probeRect[3]);
        if (statusImage == null) {
            log.warn("sheyaoxiang memory-gate icon capture failed: mode={} rect=({}, {})-({}, {})",
                    mode, probeRect[0], probeRect[1], probeRect[2], probeRect[3]);
            return IncenseIconProbe.unknown("capture-failed");
        }

        try {
            String rawPath = windowScopedTempPath.resolve("sheyaoxiang_status_icon_gate_" + mode + ".png");
            writeImage(statusImage, rawPath, "sheyaoxiang memory-gate icon");
            double[] match = ImageFinder.find(rawPath, SHEYAOXIANG_STATUS_TEMPLATE, SHEYAOXIANG_STATUS_MATCH_RATE);
            if (match == null || match.length < 2) {
                log.info("sheyaoxiang memory-gate icon template absent: mode={} path={} template={}",
                        mode, rawPath, SHEYAOXIANG_STATUS_TEMPLATE);
                return IncenseIconProbe.absent("template-miss");
            }
            java.awt.Point iconPoint = new java.awt.Point(
                    probeRect[0] + (int) Math.round(match[0]),
                    probeRect[1] + (int) Math.round(match[1]));
            log.info("sheyaoxiang memory-gate icon present: mode={} point=({}, {}) offset=({}, {}) score={}",
                    mode, iconPoint.x, iconPoint.y, iconPoint.x - statusRect[0], iconPoint.y - statusRect[1],
                    match.length >= 3 ? match[2] : -1);
            return IncenseIconProbe.present(iconPoint);
        } catch (RuntimeException e) {
            log.warn("sheyaoxiang memory-gate icon probe failed: mode={} rect=({}, {})-({}, {}) reason={}",
                    mode, probeRect[0], probeRect[1], probeRect[2], probeRect[3], e.getMessage(), e);
            return IncenseIconProbe.unknown("exception");
        } finally {
            statusImage.flush();
        }
    }

    private void rememberIncenseIconPoint(PlayerRuntimeState state, int[] statusRect, java.awt.Point iconPoint) {
        state.incenseIconOffsetX = Math.max(0, iconPoint.x - statusRect[0]);
        state.incenseIconOffsetY = Math.max(0, iconPoint.y - statusRect[1]);
    }

    private BufferedImage cropSheyaoxiangMatchedColumn(BufferedImage statusImage,
                                                       double[] match,
                                                       int[] statusRect,
                                                       String source) {
        BufferedImage template = null;
        try {
            template = ImageIO.read(new File(SHEYAOXIANG_STATUS_TEMPLATE));
            if (template == null) {
                log.warn("sheyaoxiang matched column crop skipped: source={} template not readable path={}",
                        source, SHEYAOXIANG_STATUS_TEMPLATE);
                return null;
            }

            int left = Math.max(0, (int) Math.round(match[0] - template.getWidth() / 2.0));
            int right = Math.min(statusImage.getWidth(), left + template.getWidth());
            if (right <= left) {
                log.warn("sheyaoxiang matched column crop skipped: source={} localLeft={} localRight={} imageSize={}x{}",
                        source, left, right, statusImage.getWidth(), statusImage.getHeight());
                return null;
            }

            int absLeft = statusRect[0] + left;
            int absRight = statusRect[0] + right;
            log.info("sheyaoxiang matched column crop: source={} localX=({}, {}) absX=({}, {}) height={} templateSize={}x{}",
                    source, left, right, absLeft, absRight, statusImage.getHeight(),
                    template.getWidth(), template.getHeight());
            return com.bot.dhxy.tools.ImagePreprocessor.cropCopy(
                    statusImage, left, 0, right - left, statusImage.getHeight());
        } catch (IOException e) {
            log.warn("sheyaoxiang matched column crop failed: source={} reason={}", source, e.getMessage(), e);
            return null;
        } finally {
            if (template != null) {
                template.flush();
            }
        }
    }

    /**
     * Wash the incense status crop into a black-on-white digit image and OCR it locally.
     *
     * @param statusImage captured incense status crop. Ownership stays with the caller.
     * @return remaining time. Cyan digits are hours; green digits are minutes.
     */
    private IncenseRemainingTime readSheyaoxiangRemainingTime(BufferedImage statusImage) {
        BufferedImage washed = new BufferedImage(
                statusImage.getWidth() * SHEYAOXIANG_DIGIT_OCR_SCALE,
                statusImage.getHeight() * SHEYAOXIANG_DIGIT_OCR_SCALE,
                BufferedImage.TYPE_INT_RGB);
        try {
            for (int y = 0; y < statusImage.getHeight(); y++) {
                for (int x = 0; x < statusImage.getWidth(); x++) {
                    int rgb = statusImage.getRGB(x, y) & 0xFFFFFF;
                    int outputRgb = isSheyaoxiangCyanDigitPixel(rgb) ? 0x000000 : 0xFFFFFF;
                    for (int dy = 0; dy < SHEYAOXIANG_DIGIT_OCR_SCALE; dy++) {
                        for (int dx = 0; dx < SHEYAOXIANG_DIGIT_OCR_SCALE; dx++) {
                            washed.setRGB(
                                    x * SHEYAOXIANG_DIGIT_OCR_SCALE + dx,
                                    y * SHEYAOXIANG_DIGIT_OCR_SCALE + dy,
                                    outputRgb);
                        }
                    }
                }
            }

            String washedPath = windowScopedTempPath.resolve("sheyaoxiang_status_cyan_digits.png");
            writeImage(washed, washedPath, "sheyaoxiang cyan digit OCR");
            List<OcrWordResult> words = textRecognizer.getAllTextResultsLocalOnly(washedPath);
            String text = words.stream()
                    .map(OcrWordResult::getText)
                    .filter(value -> value != null && !value.isBlank())
                    .reduce("", String::concat);
            Matcher matcher = SHEYAOXIANG_REMAINING_HOUR_PATTERN.matcher(text == null ? "" : text);
            if (!matcher.find()) {
                log.info("sheyaoxiang cyan digit OCR returned no hour digits: path={} text='{}'",
                        washedPath, text);
                return readSheyaoxiangRemainingMinutesGreen(statusImage);
            }
            int hours = Integer.parseInt(matcher.group());
            if (hours <= 0) {
                log.info("sheyaoxiang cyan digit OCR ignored non-positive hour value: text='{}'", text);
                return readSheyaoxiangRemainingMinutesGreen(statusImage);
            }
            long remainingMs = hours * ONE_HOUR_MS;
            log.info("sheyaoxiang cyan digit OCR matched remainingHours={} remainingMinutes={} path={} text='{}'",
                    hours, remainingMs / 60000, washedPath, text);
            return IncenseRemainingTime.found(remainingMs, "cyan-hours=" + hours);
        } finally {
            washed.flush();
        }
    }

    private IncenseRemainingTime readSheyaoxiangRemainingMinutesGreen(BufferedImage statusImage) {
        BufferedImage washed = new BufferedImage(
                statusImage.getWidth() * SHEYAOXIANG_DIGIT_OCR_SCALE,
                statusImage.getHeight() * SHEYAOXIANG_DIGIT_OCR_SCALE,
                BufferedImage.TYPE_INT_RGB);
        try {
            for (int y = 0; y < statusImage.getHeight(); y++) {
                for (int x = 0; x < statusImage.getWidth(); x++) {
                    int rgb = statusImage.getRGB(x, y) & 0xFFFFFF;
                    int outputRgb = isSheyaoxiangGreenDigitPixel(rgb) ? 0x000000 : 0xFFFFFF;
                    for (int dy = 0; dy < SHEYAOXIANG_DIGIT_OCR_SCALE; dy++) {
                        for (int dx = 0; dx < SHEYAOXIANG_DIGIT_OCR_SCALE; dx++) {
                            washed.setRGB(
                                    x * SHEYAOXIANG_DIGIT_OCR_SCALE + dx,
                                    y * SHEYAOXIANG_DIGIT_OCR_SCALE + dy,
                                    outputRgb);
                        }
                    }
                }
            }

            String washedPath = windowScopedTempPath.resolve("sheyaoxiang_status_green_digits.png");
            writeImage(washed, washedPath, "sheyaoxiang green digit OCR");
            List<OcrWordResult> words = textRecognizer.getAllTextResultsLocalOnly(washedPath);
            String text = words.stream()
                    .map(OcrWordResult::getText)
                    .filter(value -> value != null && !value.isBlank())
                    .reduce("", String::concat);
            SheyaoxiangDigitTemplateReader.Result templateResult =
                    sheyaoxiangDigitTemplateReader.recognizeAndLearn(washed, words, washedPath);
            if (!templateResult.learnedSymbols().isEmpty()) {
                log.info("sheyaoxiang green digit template learned symbols={} digitCount={} path={} ocrText='{}'",
                        templateResult.learnedSymbols(), templateResult.digitCount(), washedPath, text);
            }
            if (templateResult.reliable() && templateResult.text() != null && !templateResult.text().isBlank()) {
                int minutes = Integer.parseInt(templateResult.text());
                if (minutes > 0) {
                    long remainingMs = minutes * 60000L;
                    log.info("sheyaoxiang green digit template matched remainingMinutes={} path={} text='{}' ocrText='{}'",
                            minutes, washedPath, templateResult.text(), text);
                    return IncenseRemainingTime.found(remainingMs, "green-minutes-template=" + minutes);
                }
            }
            String digitsOnly = text == null ? "" : text.replaceAll("\\D+", "");
            if (templateResult.digitCount() > 1 && digitsOnly.length() < templateResult.digitCount()) {
                log.info("sheyaoxiang green digit OCR partial while templates are learning: digitCount={} path={} text='{}'",
                        templateResult.digitCount(), washedPath, text);
                return IncenseRemainingTime.empty("green-digits-learning=" + templateResult.digitCount());
            }
            Matcher matcher = SHEYAOXIANG_REMAINING_HOUR_PATTERN.matcher(text == null ? "" : text);
            if (!matcher.find()) {
                log.info("sheyaoxiang green digit OCR returned no minute digits: path={} text='{}'",
                        washedPath, text);
                return IncenseRemainingTime.empty();
            }
            int minutes = Integer.parseInt(matcher.group());
            if (minutes <= 0) {
                log.info("sheyaoxiang green digit OCR ignored non-positive minute value: text='{}'", text);
                return IncenseRemainingTime.empty();
            }
            long remainingMs = minutes * 60000L;
            log.info("sheyaoxiang green digit OCR matched remainingMinutes={} path={} text='{}'",
                    minutes, washedPath, text);
            return IncenseRemainingTime.found(remainingMs, "green-minutes=" + minutes);
        } finally {
            washed.flush();
        }
    }

    private boolean isSheyaoxiangCyanDigitPixel(int rgb) {
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        return r <= 120 && g >= 130 && b >= 130 && Math.abs(g - b) <= 80;
    }

    private boolean isSheyaoxiangGreenDigitPixel(int rgb) {
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        return g >= 120 && r <= 120 && b <= 120 && g >= r + 50 && g >= b + 50;
    }

    private long incenseLastUsedTimeForRemainingMs(long now, long remainingMs) {
        // Cyan "1 hour" display can exceed the internal 59-minute duration; never move the memory clock into the future.
        long boundedRemainingMs = Math.min(INCENSE_DURATION_MS, Math.max(1L, remainingMs));
        return now - (INCENSE_DURATION_MS - boundedRemainingMs);
    }

    private void writeImage(BufferedImage image, String path, String label) {
        try {
            ImageIO.write(image, "png", new File(path));
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

    private enum IncenseIconPresence {
        PRESENT,
        ABSENT,
        UNKNOWN
    }

    @Value
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    @Accessors(fluent = true)
    private static class IncenseIconProbe {

        IncenseIconPresence presence;

        java.awt.Point iconPoint;

        String reason;

        private static IncenseIconProbe present(java.awt.Point iconPoint) {
            return new IncenseIconProbe(IncenseIconPresence.PRESENT, iconPoint, "template-hit");
        }

        private static IncenseIconProbe absent(String reason) {
            return new IncenseIconProbe(IncenseIconPresence.ABSENT, null, reason);
        }

        private static IncenseIconProbe unknown(String reason) {
            return new IncenseIconProbe(IncenseIconPresence.UNKNOWN, null, reason);
        }
    }

    /**
     * Result of a single incense-status screenshot probe.
     *
     * @param iconPoint screen-absolute point where the incense status template matched; null when
     *                  the buff icon was not found.
     * @param remainingMs OCR-read remaining time in milliseconds. Empty means either no icon
     *                    matched or neither cyan-hour nor green-minute digits were readable.
     * @param remainingText human-readable diagnostic source such as {@code cyan-hours=1}.
     */
    @Value

    @Builder

    @AllArgsConstructor(access = AccessLevel.PUBLIC)

    @Accessors(fluent = true)

    private static class IncenseStatusProbe {

        java.awt.Point iconPoint;

        OptionalLong remainingMs;

        String remainingText;

        private static IncenseStatusProbe notFound() {
            return new IncenseStatusProbe(null, OptionalLong.empty(), "none");
        }
    

    }

    @Value
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    @Accessors(fluent = true)
    private static class IncenseRemainingTime {

        OptionalLong remainingMs;

        String describe;

        private static IncenseRemainingTime found(long remainingMs, String describe) {
            return new IncenseRemainingTime(OptionalLong.of(remainingMs), describe);
        }

        private static IncenseRemainingTime empty() {
            return new IncenseRemainingTime(OptionalLong.empty(), "none");
        }

        private static IncenseRemainingTime empty(String describe) {
            return new IncenseRemainingTime(OptionalLong.empty(), describe);
        }
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
    }

    private record FirstAidPlan(List<FirstAidTarget> targets, long createdAtMs, int baseX, int baseY) {
    }

    private record FirstAidTarget(String name, int relX, int relY, boolean expectRed, int threshold) {
    }

    public enum FirstAidNoFocusProbeResult {
        SUPPLY_NEEDED,
        HEALTHY,
        ALREADY_DONE,
        UNKNOWN
    }
}
