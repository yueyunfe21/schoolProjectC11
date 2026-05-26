package com.bot.dhxy.service;

import com.bot.dhxy.core.GameClientTracker;
import com.bot.dhxy.core.GameContext;
import com.bot.dhxy.core.ImageFinder;
import com.bot.dhxy.tools.CoordinateHelper;
import com.bot.dhxy.vision.MiniMapCoordinateReader;
import com.bot.dhxy.window.runtime.WindowScopedTempPath;
import com.bot.dhxy.window.runtime.WindowTaskContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

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

    private final GameClientTracker tracker;
    private final CoordinateHelper coordinateHelper;
    private final GameContext context;
    private final WindowScopedTempPath windowScopedTempPath;
    private final WindowTaskContextHolder windowTaskContextHolder;
    private final MiniMapCoordinateReader miniMapCoordinateReader;

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
            state().combatExitPending = true;
            onExitCombat();
            return true;
        }
        return false;
    }

    private void onEnterCombat() {
        BattleRuntimeState state = state();
        state.battleCount++;
        state.combatEnterPending = true;
        log.info("battle radar detected combat enter: battleCount={}", state.battleCount);
    }

    private void onExitCombat() {
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
                return 3000;
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
        private boolean combatEnterPending = false;
        private boolean combatExitPending = false;
    }
}
