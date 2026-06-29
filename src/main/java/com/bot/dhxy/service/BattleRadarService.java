package com.bot.dhxy.service;

import com.bot.dhxy.core.GameClientTracker;
import com.bot.dhxy.core.GameContext;
import com.bot.dhxy.core.ImageFinder;
import com.bot.dhxy.config.TeamTaskProperties;
import com.bot.dhxy.tools.CoordinateHelper;
import com.bot.dhxy.vision.MiniMapCoordinateReader;
import com.bot.dhxy.window.runtime.WindowScopedTempPath;
import com.bot.dhxy.window.runtime.WindowTaskContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.awt.image.BufferedImage;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Detects and synchronizes combat state for the currently bound game window.
 *
 * <p>The radar checks three independent screen signals before it decides that the player is still
 * in combat: the auto-combat flag, the right-side command buttons, and the top combat icons. Leaving
 * combat is deliberately conservative: the service requires repeated missing combat signals and a
 * readable minimap coordinate before emitting a combat-exit event. This protects task flows from
 * temporary capture failures, overlays, and battle-start/battle-end animation jitter.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BattleRadarService {

    private static final String BATTLE_FLAG_PATH = "images/template/battle/flag_battle.png";
    private static final String ZHAOHUAN_PATH = "images/template/battle/zhaohuan.png";
    private static final String CHEHUI_PATH = "images/template/battle/chehui.png";
    private static final String NU_PATH = "images/template/battle/nu.png";
    private static final String YUAN_PATH = "images/template/battle/yuan.png";

    private static final int AUTO_BTN_AREA_X = 974;
    private static final int AUTO_BTN_AREA_Y = 630;
    private static final int AUTO_BTN_AREA_W = 51;
    private static final int AUTO_BTN_AREA_H = 20;

    private static final int SELECTION_BTN_AREA_X = 927;
    private static final int SELECTION_BTN_AREA_Y = 302;
    private static final int SELECTION_BTN_AREA_W = 100;
    private static final int SELECTION_BTN_AREA_H = 225;

    private static final int TOP_BTN_AREA_X = 456;
    private static final int TOP_BTN_AREA_Y = 62;
    private static final int TOP_BTN_AREA_W = 123;
    private static final int TOP_BTN_AREA_H = 39;

    private static final int REQUIRED_COMBAT_EXIT_MISSES = 2;
    private static final long FAST_EXPECTED_EXIT_PROBE_DELAY_MS = 15_000L;
    private static final long FAST_EXPECTED_EXIT_PROBE_INTERVAL_MS = 1_000L;
    private static final long FAST_EXPECTED_EXIT_FULL_RADAR_INTERVAL_MS = 4_000L;
    private static final int FAST_EXPECTED_EXIT_AVATAR_ROI_SIZE = 20;
    private static final double FAST_EXPECTED_EXIT_DIFF_RATIO_THRESHOLD = 0.35;

    private final GameClientTracker tracker;
    private final CoordinateHelper coordinateHelper;
    private final GameContext context;
    private final WindowScopedTempPath windowScopedTempPath;
    private final WindowTaskContextHolder windowTaskContextHolder;
    private final MiniMapCoordinateReader miniMapCoordinateReader;
    private final TeamTaskProperties teamTaskProperties;

    private final Map<String, BattleRuntimeState> runtimeStates = new ConcurrentHashMap<>();

    /**
     * Refresh combat state from current window screenshots and emit enter/exit signals.
     *
     * @return true when combat is currently detected or when a combat state transition was just
     * synchronized; false when the window is confidently outside combat and no transition occurred.
     */
    public boolean checkAndSyncCombatState() {
        // Stage 1: fastest signal, the auto-combat flag in the bottom-right battle UI.
        int[] autoRect = coordinateHelper.getScaledRect(AUTO_BTN_AREA_X, AUTO_BTN_AREA_Y, AUTO_BTN_AREA_W, AUTO_BTN_AREA_H);
        if (coordinateHelper.findImageInRegion(BATTLE_FLAG_PATH, autoRect, 0.85) != null) {
            markCombatSignalSeen("autoFlag");
            updateCombatState(true);
            return true;
        }

        // Stage 2: right-side combat command buttons such as summon/withdraw.
        int[] selectRect = coordinateHelper.getScaledRect(SELECTION_BTN_AREA_X, SELECTION_BTN_AREA_Y, SELECTION_BTN_AREA_W, SELECTION_BTN_AREA_H);
        String selectScanPath = windowScopedTempPath.resolve("select_scan.png");
        boolean selectCaptured = tracker.captureToFile("battle-option-scan", selectScanPath,
                selectRect[0], selectRect[1], selectRect[2], selectRect[3]);
        if (!selectCaptured && context.getCurrentActionState() == GameContext.ActionState.IN_COMBAT) {
            log.warn("[battle-radar] selection-region capture failed while in combat; keep IN_COMBAT to avoid false exit");
            return true;
        }
        boolean hasSelection = ImageFinder.find(selectScanPath, ZHAOHUAN_PATH, 0.8) != null
                || ImageFinder.find(selectScanPath, CHEHUI_PATH, 0.8) != null;
        if (hasSelection) {
            markCombatSignalSeen("selection");
            updateCombatState(true);
            return true;
        }

        // Stage 3: top combat icons. This is independent from the command-button area.
        int[] topRect = coordinateHelper.getScaledRect(TOP_BTN_AREA_X, TOP_BTN_AREA_Y, TOP_BTN_AREA_W, TOP_BTN_AREA_H);
        String topScanPath = windowScopedTempPath.resolve("top_scan.png");
        boolean topCaptured = tracker.captureToFile("battle-top-scan", topScanPath,
                topRect[0], topRect[1], topRect[2], topRect[3]);
        if (!topCaptured && context.getCurrentActionState() == GameContext.ActionState.IN_COMBAT) {
            log.warn("[battle-radar] top-region capture failed while in combat; keep IN_COMBAT to avoid false exit");
            return true;
        }
        boolean hasTopIcons = ImageFinder.find(topScanPath, NU_PATH, 0.8) != null
                && ImageFinder.find(topScanPath, YUAN_PATH, 0.8) != null;
        if (hasTopIcons) {
            markCombatSignalSeen("topIcons");
            updateCombatState(true);
            return true;
        }

        // Stage 4: if we remember being in combat, require repeated misses and a readable minimap
        // before allowing the exit transition.
        if (context.getCurrentActionState() == GameContext.ActionState.IN_COMBAT) {
            BattleRuntimeState state = state();
            state.combatExitMisses++;
            if (state.combatExitMisses < REQUIRED_COMBAT_EXIT_MISSES) {
                log.warn("[battle-radar] combat signals briefly missing: miss={}/{}; keep IN_COMBAT",
                        state.combatExitMisses, REQUIRED_COMBAT_EXIT_MISSES);
                return true;
            }
            if (!isMapViewVisibleForCombatExit()) {
                log.warn("[battle-radar] combat signals missing: miss={}/{} but minimap is unreadable; keep IN_COMBAT",
                        state.combatExitMisses, REQUIRED_COMBAT_EXIT_MISSES);
                return true;
            }
            log.warn("[battle-radar] combat exit confirmed after repeated misses: miss={}/{}",
                    state.combatExitMisses, REQUIRED_COMBAT_EXIT_MISSES);
        }

        return updateCombatState(false);
    }

    /**
     * Run the lightweight expected-combat exit probe against the leader-avatar hover area.
     *
     * <p>This probe is only for task-owned expected combat waits. It deliberately looks at one tiny
     * 20x20 region around the configured team-role hover point instead of running the full battle
     * template stack every second. The normal radar remains the fallback; this method only
     * short-circuits exit when the avatar area clearly changes after combat has been running for at
     * least 15 seconds.</p>
     *
     * @param source diagnostic source such as {@code xiuluo-v2}.
     * @return true when the probe confidently marked combat as finished.
     */
    public boolean checkFastExpectedCombatExitByAvatarDiff(String source) {
        if (context.getCurrentActionState() != GameContext.ActionState.IN_COMBAT) {
            return false;
        }
        BattleRuntimeState state = state();
        long now = System.currentTimeMillis();
        if (state.combatStartedAtMs <= 0L) {
            state.combatStartedAtMs = now;
        }
        if (state.fastExpectedExitBaselineImage == null) {
            BufferedImage baseline = captureFastExpectedExitAvatar(source);
            if (baseline == null) {
                return false;
            }
            state.fastExpectedExitBaselineImage = baseline;
            state.lastFastExpectedExitProbeAtMs = now;
            log.info("[battle-radar] fast expected exit avatar baseline captured: source={} delayMs={} intervalMs={} roiSize={} hover=({}, {})",
                    source, FAST_EXPECTED_EXIT_PROBE_DELAY_MS, FAST_EXPECTED_EXIT_PROBE_INTERVAL_MS,
                    FAST_EXPECTED_EXIT_AVATAR_ROI_SIZE, teamTaskProperties.getTeamHoverX(),
                    teamTaskProperties.getTeamHoverY());
            return false;
        }
        long combatAgeMs = now - state.combatStartedAtMs;
        if (combatAgeMs < FAST_EXPECTED_EXIT_PROBE_DELAY_MS) {
            return false;
        }
        if (state.lastFastExpectedExitProbeAtMs > 0L
                && now - state.lastFastExpectedExitProbeAtMs < FAST_EXPECTED_EXIT_PROBE_INTERVAL_MS) {
            return false;
        }
        state.lastFastExpectedExitProbeAtMs = now;

        BufferedImage current = captureFastExpectedExitAvatar(source);
        if (current == null) {
            return false;
        }
        if (!ImageFinder.isMatch(state.fastExpectedExitBaselineImage, current,
                FAST_EXPECTED_EXIT_DIFF_RATIO_THRESHOLD)) {
            log.info("[battle-radar] fast expected combat exit detected by avatar diff: source={} combatAgeMs={} diffRatioThreshold={} roiSize={} hover=({}, {})",
                    source, combatAgeMs, FAST_EXPECTED_EXIT_DIFF_RATIO_THRESHOLD,
                    FAST_EXPECTED_EXIT_AVATAR_ROI_SIZE,
                    teamTaskProperties.getTeamHoverX(), teamTaskProperties.getTeamHoverY());
            return updateCombatState(false);
        }
        log.debug("[battle-radar] fast expected combat exit avatar unchanged: source={} combatAgeMs={} diffRatioThreshold={}",
                source, combatAgeMs, FAST_EXPECTED_EXIT_DIFF_RATIO_THRESHOLD);
        return false;
    }

    /**
     * Arm the current task-owned expected-combat wait.
     *
     * <p>五倍/修罗 can enter a new expected battle while the previous combat's one-shot exit signal
     * is still pending. The arm timestamp is the boundary: FAST_EXPECTED_EXIT may only consume exit
     * signals produced after this point. Arming does not touch the avatar baseline, because trusted
     * in-combat correction may have just refreshed it.</p>
     *
     * @param source diagnostic task/source label.
     */
    public void armExpectedCombatExitWait(String source) {
        BattleRuntimeState state = state();
        long now = System.currentTimeMillis();
        state.expectedCombatExitWaitArmedAtMs = now;
        if (state.combatExitPending
                && (state.combatExitPendingAtMs <= 0L || state.combatExitPendingAtMs <= now)) {
            log.warn("[battle-radar] discard stale combat-exit signal when expected wait arms: source={} battleCount={} pendingAtMs={} armedAtMs={}",
                    source, state.battleCount, state.combatExitPendingAtMs, now);
            state.combatExitPending = false;
            state.combatExitPendingAtMs = 0L;
            state.combatExitPendingBattleCount = 0;
        }
    }

    /**
     * Replace the fast expected-exit avatar baseline with the current trusted in-combat frame.
     *
     * <p>This is used after a return-item false positive is corrected by a read-only trusted combat
     * probe. The fast-exit mechanism remains enabled; only the stale/incorrect comparison baseline
     * is replaced.</p>
     *
     * @param source diagnostic task/source label.
     * @return true when a new baseline image was captured.
     */
    public boolean refreshFastExpectedCombatExitAvatarBaseline(String source) {
        BattleRuntimeState state = state();
        state.fastExpectedExitBaselineImage = null;
        if (context.getCurrentActionState() != GameContext.ActionState.IN_COMBAT) {
            log.warn("[battle-radar] fast expected exit baseline refresh skipped: source={} actionState={}",
                    source, context.getCurrentActionState());
            return false;
        }
        BufferedImage baseline = captureFastExpectedExitAvatar(source);
        if (baseline == null) {
            log.warn("[battle-radar] fast expected exit baseline refresh failed: source={}", source);
            return false;
        }
        long now = System.currentTimeMillis();
        if (state.combatStartedAtMs <= 0L) {
            state.combatStartedAtMs = now;
        }
        state.fastExpectedExitBaselineImage = baseline;
        state.lastFastExpectedExitProbeAtMs = now;
        log.info("[battle-radar] fast expected exit avatar baseline refreshed after trusted IN_COMBAT: source={} roiSize={} hover=({}, {})",
                source, FAST_EXPECTED_EXIT_AVATAR_ROI_SIZE,
                teamTaskProperties.getTeamHoverX(), teamTaskProperties.getTeamHoverY());
        return true;
    }

    /**
     * @return milliseconds until the next lightweight expected-combat exit probe should run; -1 when
     *         the current bound window is not in combat.
     */
    public long nextFastExpectedCombatExitProbeDelayMs() {
        if (context.getCurrentActionState() != GameContext.ActionState.IN_COMBAT) {
            return -1L;
        }
        BattleRuntimeState state = state();
        long now = System.currentTimeMillis();
        long startedAt = state.combatStartedAtMs > 0L ? state.combatStartedAtMs : now;
        long delayGateAt = startedAt + FAST_EXPECTED_EXIT_PROBE_DELAY_MS;
        long intervalGateAt = state.lastFastExpectedExitProbeAtMs <= 0L
                ? now
                : state.lastFastExpectedExitProbeAtMs + FAST_EXPECTED_EXIT_PROBE_INTERVAL_MS;
        return Math.max(0L, Math.max(delayGateAt, intervalGateAt) - now);
    }

    /**
     * Keep the old full radar fallback sparse while the task also runs the 20x20 fast expected-exit
     * probe every second.
     *
     * @return true when the full radar should run now.
     */
    public boolean shouldRunFullRadarForFastExpectedExitFallback() {
        BattleRuntimeState state = state();
        long now = System.currentTimeMillis();
        if (state.lastFastExpectedFullRadarAtMs <= 0L
                || now - state.lastFastExpectedFullRadarAtMs >= FAST_EXPECTED_EXIT_FULL_RADAR_INTERVAL_MS) {
            state.lastFastExpectedFullRadarAtMs = now;
            return true;
        }
        return false;
    }

    private BufferedImage captureFastExpectedExitAvatar(String source) {
        int hoverX = teamTaskProperties.getTeamHoverX();
        int hoverY = teamTaskProperties.getTeamHoverY();
        if (hoverX <= 0 || hoverY <= 0) {
            log.warn("[battle-radar] fast expected exit avatar probe skipped: team hover point not configured source={} hover=({}, {})",
                    source, hoverX, hoverY);
            return null;
        }
        int half = FAST_EXPECTED_EXIT_AVATAR_ROI_SIZE / 2;
        int[] rect = coordinateHelper.getScaledRect(
                hoverX - half, hoverY - half,
                FAST_EXPECTED_EXIT_AVATAR_ROI_SIZE, FAST_EXPECTED_EXIT_AVATAR_ROI_SIZE);
        BufferedImage image = tracker.captureToMemory("battle-fast-expected-exit-avatar",
                rect[0], rect[1], rect[2], rect[3]);
        if (image == null) {
            log.warn("[battle-radar] fast expected exit avatar capture failed: source={} rect=[{},{} -> {},{}]",
                    source, rect[0], rect[1], rect[2], rect[3]);
        }
        return image;
    }

    private void markCombatSignalSeen(String source) {
        BattleRuntimeState state = state();
        if (state.combatExitMisses > 0) {
            log.info("[battle-radar] combat signal recovered: source={} clearExitMisses={}",
                    source, state.combatExitMisses);
        }
        state.combatExitMisses = 0;
    }

    private boolean isMapViewVisibleForCombatExit() {
        try {
            boolean readable = miniMapCoordinateReader.readCurrentCoordinate().isPresent();
            log.info("[battle-radar] minimap readability before combat exit: readable={}", readable);
            return readable;
        } catch (Exception e) {
            log.warn("[battle-radar] minimap readability check failed before combat exit; keep IN_COMBAT", e);
            return false;
        }
    }

    private boolean updateCombatState(boolean isCurrentlyInCombat) {
        GameContext.ActionState rememberedState = context.getCurrentActionState();

        if (isCurrentlyInCombat && rememberedState != GameContext.ActionState.IN_COMBAT) {
            state().combatExitMisses = 0;
            log.warn("[battle-radar] combat screen detected; force action state to IN_COMBAT");
            context.setCurrentActionState(GameContext.ActionState.IN_COMBAT);
            onEnterCombat();
            return true;
        } else if (!isCurrentlyInCombat && rememberedState == GameContext.ActionState.IN_COMBAT) {
            state().combatExitMisses = 0;
            log.info("[battle-radar] combat finished; restore action state to FREE and emit exit signal");
            context.setCurrentActionState(GameContext.ActionState.FREE);
            BattleRuntimeState state = state();
            state.combatExitPending = true;
            state.combatExitPendingAtMs = System.currentTimeMillis();
            state.combatExitPendingBattleCount = state.battleCount;
            onExitCombat();
            return true;
        }
        return false;
    }

    private void onEnterCombat() {
        BattleRuntimeState state = state();
        state.battleCount++;
        state.combatStartedAtMs = System.currentTimeMillis();
        state.lastFastExpectedExitProbeAtMs = 0L;
        state.lastFastExpectedFullRadarAtMs = 0L;
        state.fastExpectedExitBaselineImage = null;
        if (state.combatExitPending) {
            log.warn("[battle-radar] discard stale combat-exit signal on combat enter: battleCount={}",
                    state.battleCount);
            state.combatExitPending = false;
            state.combatExitPendingAtMs = 0L;
            state.combatExitPendingBattleCount = 0;
        }
        state.combatEnterPending = true;
        log.info("battle radar detected combat enter: battleCount={}", state.battleCount);
    }

    private void onExitCombat() {
        BattleRuntimeState state = state();
        state.combatStartedAtMs = 0L;
        state.lastFastExpectedExitProbeAtMs = 0L;
        state.lastFastExpectedFullRadarAtMs = 0L;
        state.fastExpectedExitBaselineImage = null;
    }

    /**
     * Consume the one-shot combat-enter event for the current window.
     *
     * @return true once after a new combat entry is detected; false until the next entry.
     */
    public boolean consumeCombatEnterSignal() {
        BattleRuntimeState state = state();
        if (!state.combatEnterPending) {
            return false;
        }
        state.combatEnterPending = false;
        return true;
    }

    /**
     * Consume the one-shot combat-exit event for the current window.
     *
     * @return true once after a confirmed combat exit; false until the next exit.
     */
    public boolean consumeCombatExitSignal() {
        BattleRuntimeState state = state();
        if (!state.combatExitPending) {
            return false;
        }
        state.combatExitPending = false;
        state.combatExitPendingAtMs = 0L;
        state.combatExitPendingBattleCount = 0;
        return true;
    }

    /**
     * Consume a combat-exit event only if it was produced after the current expected wait armed.
     *
     * @param source diagnostic task/source label.
     * @return true once for a fresh current expected-combat exit; false for absent or stale exits.
     */
    public boolean consumeCombatExitSignalForExpectedWait(String source) {
        BattleRuntimeState state = state();
        if (!state.combatExitPending) {
            return false;
        }
        if (state.expectedCombatExitWaitArmedAtMs <= 0L
                || state.combatExitPendingAtMs < state.expectedCombatExitWaitArmedAtMs) {
            log.warn("[battle-radar] discard stale expected combat-exit signal: source={} battleCount={} pendingBattleCount={} pendingAtMs={} armedAtMs={}",
                    source, state.battleCount, state.combatExitPendingBattleCount,
                    state.combatExitPendingAtMs, state.expectedCombatExitWaitArmedAtMs);
            state.combatExitPending = false;
            state.combatExitPendingAtMs = 0L;
            state.combatExitPendingBattleCount = 0;
            return false;
        }
        state.combatExitPending = false;
        state.combatExitPendingAtMs = 0L;
        state.combatExitPendingBattleCount = 0;
        return true;
    }

    /**
     * Drop a stale exit event when the current bound window is already known to be in combat.
     *
     * @param source diagnostic caller label.
     * @return true when a stale exit signal was cleared.
     */
    public boolean discardStaleCombatExitSignalIfInCombat(String source) {
        if (context.getCurrentActionState() != GameContext.ActionState.IN_COMBAT) {
            return false;
        }
        BattleRuntimeState state = state();
        if (!state.combatExitPending) {
            return false;
        }
        state.combatExitPending = false;
        state.combatExitPendingAtMs = 0L;
        state.combatExitPendingBattleCount = 0;
        log.warn("[battle-radar] discard stale combat-exit signal while still IN_COMBAT: source={} battleCount={}",
                source, state.battleCount);
        return true;
    }

    /**
     * Choose the polling interval that callers should use for the current action state.
     *
     * @return milliseconds between radar ticks; combat/free states intentionally use different
     * cadences to reduce background capture cost.
     */
    public int getDynamicPollingIntervalMs() {
        GameContext.ActionState state = context.getCurrentActionState();
        switch (state) {
            case IN_COMBAT:
                return 4000;
            case NAVIGATING:
            case INTERACTING:
                return 2000;
            case FREE:
            default:
                return 10000;
        }
    }

    private BattleRuntimeState state() {
        String key = windowTaskContextHolder.rawCurrent()
                .map(windowContext -> windowContext.getWindowId())
                .orElse("default");
        return runtimeStates.computeIfAbsent(key, ignored -> new BattleRuntimeState());
    }

    private static class BattleRuntimeState {
        private int battleCount = 0;
        private int combatExitMisses = 0;
        private long combatStartedAtMs = 0L;
        private long lastFastExpectedExitProbeAtMs = 0L;
        private long lastFastExpectedFullRadarAtMs = 0L;
        private BufferedImage fastExpectedExitBaselineImage = null;
        private long expectedCombatExitWaitArmedAtMs = 0L;
        private boolean combatEnterPending = false;
        private boolean combatExitPending = false;
        private long combatExitPendingAtMs = 0L;
        private int combatExitPendingBattleCount = 0;
    }
}
