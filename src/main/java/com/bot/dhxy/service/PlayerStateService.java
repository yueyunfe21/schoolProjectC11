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
import com.bot.dhxy.window.runtime.WindowScopedTempPath;
import com.bot.dhxy.window.runtime.WindowTaskContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.concurrent.ConcurrentHashMap;
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

    private final Map<String, PlayerRuntimeState> runtimeStates = new ConcurrentHashMap<>();
    private static final long INCENSE_DURATION_MS = 59 * 60 * 1000L;
    private static final long INCENSE_REFRESH_REMAINING_MS = 20 * 60 * 1000L;
    private static final long INCENSE_REFRESH_AFTER_MS = INCENSE_DURATION_MS - INCENSE_REFRESH_REMAINING_MS;
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
    private static final int SAFE_MOUSE_REL_X = 20;
    private static final int SAFE_MOUSE_REL_Y = 20;

    /*
     * 摄妖香剩余时间文本颜色。游戏中 RGB=(0,255,255) 表示剩余时间至少还有一小时；
     * 后续判断只需要确认是否仍为这个颜色，不是这个颜色就按一小时以下处理。
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
    private static final int STATUS_PANEL_X = 1001;
    private static final int STATUS_PANEL_Y = 123;
    private static final int STATUS_PANEL_W = 23;
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

    public void performFirstAidCheck() {
        performFirstAidCheck(false);
    }

    public void performFirstAidCheckNow() {
        performFirstAidCheck(true, null);
    }

    public void performFirstAidCheckNow(TaskExecutionContext taskContext) {
        performFirstAidCheck(true, taskContext);
    }

    /**
     * Check whether player/pet HP or MP appears below configured thresholds without focusing.
     *
     * @param taskContext optional task stop token; null is allowed for legacy callers.
     * @return true when at least one enabled bar appears below threshold; false when no supply is
     * needed, the check has already run this round, or the screenshot is unavailable.
     */
    public boolean needsFirstAidSupplyNoFocus(TaskExecutionContext taskContext) {
        checkpoint(taskContext);
        PlayerRuntimeState state = state();
        if (state.checksDoneThisRound >= MAX_CHECKS_BETWEEN_BATTLES) {
            log.info("first-aid no-focus precheck skipped: checks already done {}/{}",
                    state.checksDoneThisRound, MAX_CHECKS_BETWEEN_BATTLES);
            return false;
        }
        if (tracker.getWindowBaseX() == -1) {
            log.warn("first-aid no-focus precheck skipped: window base unavailable");
            return false;
        }

        BufferedImage bars = captureBarsSnapshotNoFocus();
        if (bars == null) {
            log.warn("first-aid no-focus precheck failed: bars snapshot unavailable");
            return false;
        }
        try {
            boolean needed = isSupplyNeededFromSnapshotIfEnabled(bars, "人物血量", CHAR_BAR_LEFT_X, CHAR_BAR_RIGHT_X,
                    BAR_HP_Y, true, config.isPlayerHpSupplyEnabled(), config.getPlayerHpSupplyThreshold())
                    || isSupplyNeededFromSnapshotIfEnabled(bars, "人物法力", CHAR_BAR_LEFT_X, CHAR_BAR_RIGHT_X,
                    BAR_MP_Y, false, config.isPlayerMpSupplyEnabled(), config.getPlayerMpSupplyThreshold())
                    || isSupplyNeededFromSnapshotIfEnabled(bars, "宝宝血量", PET_BAR_LEFT_X, PET_BAR_RIGHT_X,
                    BAR_HP_Y, true, config.isPetHpSupplyEnabled(), config.getPetHpSupplyThreshold())
                    || isSupplyNeededFromSnapshotIfEnabled(bars, "宝宝法力", PET_BAR_LEFT_X, PET_BAR_RIGHT_X,
                    BAR_MP_Y, false, config.isPetMpSupplyEnabled(), config.getPetMpSupplyThreshold());
            log.info("first-aid no-focus precheck result: needed={}", needed);
            return needed;
        } finally {
            bars.flush();
        }
    }

    private void performFirstAidCheck(boolean ignoreTimeInterval) {
        performFirstAidCheck(ignoreTimeInterval, null);
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

    public void ensureSheYaoXiangActive() {
        ensureSheYaoXiangActive(null);
    }

    /**
     * Ensure the incense buff is active or refresh it when the configured cooldown window opens.
     *
     * @param taskContext optional stop token. The method may open the bag and click an item, so it
     * should only be called when the current task is allowed to use physical input.
     */
    public void ensureSheYaoXiangActive(TaskExecutionContext taskContext) {
        long latencyStart = LatencyMetrics.start();
        try {
            checkpoint(taskContext);
            PlayerRuntimeState state = state();
            long now = System.currentTimeMillis();
            if (state.lastIncenseUsedTime > 0 && now - state.lastIncenseUsedTime < INCENSE_REFRESH_AFTER_MS) {
                long elapsedMinutes = Math.max(0, (now - state.lastIncenseUsedTime) / 60000);
                long refreshAfterMinutes = INCENSE_REFRESH_AFTER_MS / 60000;
                log.info("🕯️ 摄妖香由本程序补充后仅过去 {} 分钟，未达到 {} 分钟主动补香线，跳过包裹检查。",
                        elapsedMinutes, refreshAfterMinutes);
                return;
            }

            if (state.lastIncenseUsedTime > 0) {
                long elapsedMinutes = Math.max(0, (now - state.lastIncenseUsedTime) / 60000);
                log.info("🕯️ 摄妖香由本程序补充后已过去 {} 分钟，进入剩余约 20 分钟主动补香窗口。", elapsedMinutes);
            } else {
                log.info("🕯️ 摄妖香没有本程序补充时间记录，开始执行安全校验...");
            }

            if (now < state.nextIncenseRetryTime) {
                long remainingSeconds = Math.max(1, (state.nextIncenseRetryTime - now + 999) / 1000);
                log.warn("⚠️ 摄妖香上次补充失败，仍在重试冷却中，剩余 {} 秒，跳过包裹检查。", remainingSeconds);
                return;
            }

            int[] statusRect = coordinateHelper.getScaledRect(STATUS_PANEL_X, STATUS_PANEL_Y, STATUS_PANEL_W, STATUS_PANEL_H);
            IncenseStatusProbe statusProbe = probeIncenseStatus(statusRect);
            java.awt.Point buffIcon = statusProbe.iconPoint();

            /*
             * Template match means the incense buff exists. A cyan hour number in the same crop means
             * the remaining time is at least that many hours; use it to calibrate the in-memory watch
             * and skip the bag. If the icon exists but no cyan number is readable, treat it as below
             * one hour and refill, which is the conservative behavior the user requested.
             */
            if (buffIcon != null && statusProbe.remainingHours().isPresent()) {
                int remainingHours = statusProbe.remainingHours().getAsInt();
                state.lastIncenseUsedTime = incenseLastUsedTimeForRemainingHours(now, remainingHours);
                state.nextIncenseRetryTime = 0;
                log.info("sheyaoxiang status matched with remainingHours={}; calibrate watch and skip refill. point=({}, {})",
                        remainingHours, buffIcon.x, buffIcon.y);
                return;
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
            boolean used = bagService.findAndUseItem(
                    BagService.MAIN_BAG, "bag/sheyaoxiang_item.png", null, taskContext);
            checkpoint(taskContext);

            if (used) {
                log.info("✅ 成功使用摄妖香，怀表已重置为 1 小时。等待吃香动画...");
                state.lastIncenseUsedTime = System.currentTimeMillis();
                state.nextIncenseRetryTime = 0;
                TaskSleep.sleep(1000);
            } else {
                log.error("❌ 包裹内未找到摄妖香，请及时购买补充。1 分钟后才会再试。 ");
                state.nextIncenseRetryTime = System.currentTimeMillis() + 60000;
            }
        } finally {
            LatencyMetrics.info(log, "player.sheyaoxiang.ensure", latencyStart,
                    "context=" + (taskContext == null ? "-" : taskContext.getTaskCode()));
        }
    }

    public void ensureSheYaoXiangActiveForLeaderTask(String source) {
        ensureSheYaoXiangActiveForLeaderTask(source, null);
    }

    /**
     * Leader-only wrapper for incense refresh.
     *
     * @param source diagnostic caller name.
     * @param taskContext optional stop token.
     */
    public void ensureSheYaoXiangActiveForLeaderTask(String source, TaskExecutionContext taskContext) {
        checkpoint(taskContext);
        String caller = source == null || source.isBlank() ? "unknown" : source;
        var currentWindow = windowTaskContextHolder.rawCurrent();
        if (currentWindow.isPresent() && currentWindow.get().isMember()) {
            log.info("摄妖香检查跳过：source={} windowId={} role={}，队员窗口不负责摄妖香",
                    caller, currentWindow.get().getWindowId(), currentWindow.get().getRole().getDisplayName());
            return;
        }
        if (currentWindow.isPresent()) {
            log.info("摄妖香检查允许：source={} windowId={} role={}",
                    caller, currentWindow.get().getWindowId(), currentWindow.get().getRole().getDisplayName());
        } else {
            log.info("摄妖香检查允许：source={} 无窗口上下文，按单窗口/队长任务兼容处理", caller);
        }
        ensureSheYaoXiangActive(taskContext);
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
            moveMouseAwayBeforeBarsSnapshotDirect();
        } else {
            moveMouseAwayBeforeBarsSnapshot();
        }
        int[] rect = coordinateHelper.getScaledRect(BARS_SCAN_LEFT_X, BARS_SCAN_TOP_Y, BARS_SCAN_W, BARS_SCAN_H);
        return tracker.captureToMemory("player-state-bars", rect[0], rect[1], rect[2], rect[3]);
    }

    private BufferedImage captureBarsSnapshotNoFocus() {
        int[] rect = coordinateHelper.getScaledRect(BARS_SCAN_LEFT_X, BARS_SCAN_TOP_Y, BARS_SCAN_W, BARS_SCAN_H);
        return tracker.captureToMemory("player-state-bars-precheck", rect[0], rect[1], rect[2], rect[3]);
    }

    private void moveMouseAwayBeforeBarsSnapshot() {
        if (tracker.getWindowBaseX() == -1 || tracker.getWindowBaseY() == -1) {
            return;
        }
        inputSequences.submitAndWait("playerState:moveMouseAwayBeforeBarsSnapshot", List.of(
                InputAction.moveMouse(tracker.getWindowBaseX() + SAFE_MOUSE_REL_X, tracker.getWindowBaseY() + SAFE_MOUSE_REL_Y),
                InputAction.sleep(80)
        ));
    }

    private void moveMouseAwayBeforeBarsSnapshotDirect() {
        if (tracker.getWindowBaseX() == -1 || tracker.getWindowBaseY() == -1) {
            return;
        }
        inputProvider.moveMouse(tracker.getWindowBaseX() + SAFE_MOUSE_REL_X, tracker.getWindowBaseY() + SAFE_MOUSE_REL_Y);
        TaskSleep.sleep(80);
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
             * Stage 2: read only the cyan hour number from the same status crop. This keeps the
             * extra OCR cost behind a successful template match and avoids a second screenshot.
             */
            OptionalInt remainingHours = readSheyaoxiangRemainingHours(statusImage);
            log.info("sheyaoxiang status matched: point=({}, {}) remainingHours={}",
                    iconPoint.x, iconPoint.y, remainingHours.isPresent() ? remainingHours.getAsInt() : "none");
            return new IncenseStatusProbe(iconPoint, remainingHours);
        } finally {
            statusImage.flush();
        }
    }

    /**
     * Wash the incense status crop into a black-on-white cyan-digit image and OCR it locally.
     *
     * @param statusImage captured incense status crop. Ownership stays with the caller.
     * @return positive hour number when OCR sees digits such as {@code 02}, {@code 03}, or
     * {@code 11}; empty when no cyan digit text is readable.
     */
    private OptionalInt readSheyaoxiangRemainingHours(BufferedImage statusImage) {
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
                return OptionalInt.empty();
            }
            int hours = Integer.parseInt(matcher.group());
            if (hours <= 0) {
                log.info("sheyaoxiang cyan digit OCR ignored non-positive hour value: text='{}'", text);
                return OptionalInt.empty();
            }
            return OptionalInt.of(hours);
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

    private long incenseLastUsedTimeForRemainingHours(long now, int remainingHours) {
        long remainingMs = Math.max(1L, remainingHours) * ONE_HOUR_MS;
        return now - (INCENSE_DURATION_MS - remainingMs);
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
        return runtimeStates.computeIfAbsent(key, ignored -> new PlayerRuntimeState());
    }

    private void checkpoint(TaskExecutionContext taskContext) {
        TaskCheckpoint.throwIfStopRequested(taskContext, "Player state sync interrupted");
    }

    private boolean isInputWorkerThread() {
        return Thread.currentThread().getName().contains("dhxy-input-action-worker");
    }

    private String safeLatencyValue(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    /**
     * Result of a single incense-status screenshot probe.
     *
     * @param iconPoint screen-absolute point where the incense status template matched; null when
     *                  the buff icon was not found.
     * @param remainingHours OCR-read remaining hour number. Empty means either no icon matched or
     *                       the icon had no cyan hour number visible.
     */
    @Value

    @Builder

    @AllArgsConstructor(access = AccessLevel.PUBLIC)

    @Accessors(fluent = true)

    private static class IncenseStatusProbe {

        java.awt.Point iconPoint;

        OptionalInt remainingHours;

        private static IncenseStatusProbe notFound() {
            return new IncenseStatusProbe(null, OptionalInt.empty());
        }
    

    }

    private static class PlayerRuntimeState {
        private long lastIncenseUsedTime = 0;
        private long nextIncenseRetryTime = 0;
        private int checksDoneThisRound = 0;
        private long lastCombatExitTime = 0;
    }
}
