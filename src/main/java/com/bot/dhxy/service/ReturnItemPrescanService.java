package com.bot.dhxy.service;

import com.bot.dhxy.model.bag.ReturnItemCachePoint;
import com.bot.dhxy.runner.context.TaskExecutionContext;
import com.bot.dhxy.runner.stop.TaskCheckpoint;
import com.bot.dhxy.window.model.WindowNativeBinding;
import com.bot.dhxy.window.runtime.WindowRuntimeContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Coordinates per-round return-item prescans before the task reaches RETURN_HOME.
 *
 * <p>The service deliberately does not decide whether a round has returned home. It only learns a
 * screen-absolute bag item point and lets the task's existing return-map verification accept or
 * reject that click.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReturnItemPrescanService {
    private static final long COMBAT_ENTRY_MAINTENANCE_MS = 4_000L;
    private static final long COMBAT_RANDOM_MIN_MS = 8_000L;
    private static final long COMBAT_RANDOM_MAX_MS = 18_000L;

    private final BagService bagService;
    private final Map<PrescanKey, PrescanState> states = new ConcurrentHashMap<>();

    public enum Mode {
        MAIN_BAG_TASK_PAGE,
        MAIN_BAG_FROM_BACK
    }

    private enum Strategy {
        AFTER_TRACKER_GREEN,
        BACKGROUND_PATHING,
        IN_COMBAT_RANDOM,
        SKIP
    }

    /**
     * Run the prescan chosen for the current round immediately after a tracker green-link click.
     *
     * @param context current task context, used for taskRun/window identity and stop checks.
     * @param taskCode task namespace such as {@code xiuluo_v2} or {@code wubei}.
     * @param round one-based task round number.
     * @param template item template path relative to {@code images/template/}.
     * @param mode bag scan mode that matches the task's old full-search return-item logic.
     * @param maxBackPage zero-based highest page for {@link Mode#MAIN_BAG_FROM_BACK}; ignored for
     *                    task-page mode.
     * @param source diagnostic source.
     */
    public void afterTrackerGreen(TaskExecutionContext context,
                                  String taskCode,
                                  int round,
                                  String template,
                                  Mode mode,
                                  int maxBackPage,
                                  String source) {
        PrescanState state = stateFor(context, taskCode, round, template, mode, maxBackPage,
                true, round > 1, null);
        if (state.strategy != Strategy.AFTER_TRACKER_GREEN || state.done || state.inProgress) {
            return;
        }
        runPrescan(context, state, source + ":after-tracker-green", true);
    }

    /**
     * Run an immediate tracker-green prescan for an item whose slot must be learned before the item
     * disappears. Unlike the normal randomized return-item strategy, this source-owned slot learn
     * is deliberately forced to the current after-tracker-green window and does not downgrade into
     * combat fallback.
     */
    public void afterTrackerGreenRequired(TaskExecutionContext context,
                                          String taskCode,
                                          int round,
                                          String template,
                                          Mode mode,
                                          int maxBackPage,
                                          String source) {
        PrescanState state = stateFor(context, taskCode, round, template, mode, maxBackPage,
                true, false, Strategy.AFTER_TRACKER_GREEN);
        if (state.done || state.inProgress) {
            return;
        }
        runPrescan(context, state, source + ":after-tracker-green", false);
    }

    /**
     * Run the prescan while the task is already pathing, only when the per-round random strategy
     * selected the background/pathing slot.
     */
    public void whilePathing(TaskExecutionContext context,
                             String taskCode,
                             int round,
                             String template,
                             Mode mode,
                             int maxBackPage,
                             String source) {
        PrescanState state = stateFor(context, taskCode, round, template, mode, maxBackPage,
                true, round > 1, null);
        if (state.strategy != Strategy.BACKGROUND_PATHING || state.done || state.inProgress) {
            return;
        }
        runPrescan(context, state, source + ":background-pathing", true);
    }

    /**
     * Run the combat-window prescan after the existing auto-combat entry maintenance window plus a
     * random jitter. Failed earlier strategies are also downgraded into this combat slot.
     */
    public void whileInCombat(TaskExecutionContext context,
                              String taskCode,
                              int round,
                              String template,
                              Mode mode,
                              int maxBackPage,
                              String source) {
        PrescanState state = stateFor(context, taskCode, round, template, mode, maxBackPage,
                false, false, null);
        if (state.done || state.inProgress) {
            return;
        }
        if (state.strategy == Strategy.BACKGROUND_PATHING && state.cachePoint == null && !state.combatFallback) {
            state.combatFallback = true;
            log.info("[return-item-prescan] background opportunity missed; downgrade to combat prescan: key={} source={}",
                    state.key, source);
        }
        if (state.strategy != Strategy.IN_COMBAT_RANDOM && !state.combatFallback) {
            return;
        }
        long now = System.currentTimeMillis();
        if (state.combatDueAtMs <= 0L) {
            state.combatDueAtMs = now + COMBAT_ENTRY_MAINTENANCE_MS
                    + ThreadLocalRandom.current().nextLong(COMBAT_RANDOM_MIN_MS, COMBAT_RANDOM_MAX_MS + 1L);
            log.info("[return-item-prescan] combat prescan scheduled: key={} dueInMs={} strategy={} fallback={}",
                    state.key, state.combatDueAtMs - now, state.strategy, state.combatFallback);
            return;
        }
        if (now < state.combatDueAtMs) {
            return;
        }
        runPrescan(context, state, source + ":combat", false);
    }

    /**
     * Try using a cached return item point for this task round.
     *
     * @return true when a cached click was submitted; false when no valid cache exists or the
     *         cached click could not be sent.
     */
    public boolean useCached(TaskExecutionContext context,
                             String taskCode,
                             int round,
                             String template,
                             Mode mode,
                             int maxBackPage,
                             String source) {
        PrescanState state = states.get(keyFor(context, taskCode, round, template));
        if (state == null || state.cachePoint == null) {
            return false;
        }
        boolean used = bagService.useCachedMainBagReturnItem(state.cachePoint, source, context);
        log.info("[return-item-prescan] cached use result: key={} used={} source={} point=({}, {})",
                state.key, used, source, state.cachePoint.getClickX(), state.cachePoint.getClickY());
        if (!used) {
            invalidate(context, taskCode, round, template, "cached-click-failed:" + source);
        }
        return used;
    }

    /**
     * Reports whether a learned return-item point is currently available without clicking it.
     *
     * @param context current task context used only for the scoped cache key; no input is sent.
     * @param taskCode task namespace such as {@code xiuluo_v2} or {@code wubei}.
     * @param round one-based task round number.
     * @param template item template path relative to {@code images/template/}.
     * @return true when the per-window/per-round prescan cache has a click point ready.
     */
    public boolean hasCached(TaskExecutionContext context, String taskCode, int round, String template) {
        PrescanState state = states.get(keyFor(context, taskCode, round, template));
        return state != null && state.cachePoint != null;
    }

    public void invalidate(TaskExecutionContext context, String taskCode, int round, String template, String reason) {
        PrescanState state = states.get(keyFor(context, taskCode, round, template));
        if (state == null) {
            return;
        }
        state.cachePoint = null;
        state.done = false;
        state.combatFallback = true;
        log.info("[return-item-prescan] cache invalidated: key={} reason={}", state.key, reason);
    }

    public void completeRound(TaskExecutionContext context, String taskCode, int round, String template, String source) {
        PrescanKey key = keyFor(context, taskCode, round, template);
        PrescanState removed = states.remove(key);
        if (removed != null) {
            log.info("[return-item-prescan] round state cleared: key={} source={} hadCache={}",
                    key, source, removed.cachePoint != null);
        }
    }

    private PrescanState stateFor(TaskExecutionContext context,
                                  String taskCode,
                                  int round,
                                  String template,
                                  Mode mode,
                                  int maxBackPage,
                                  boolean trackerGreenAvailable,
                                  boolean backgroundAllowed,
                                  Strategy forcedStrategy) {
        PrescanKey key = keyFor(context, taskCode, round, template);
        return states.computeIfAbsent(key, ignored -> {
            Strategy strategy = forcedStrategy == null ? chooseStrategy(trackerGreenAvailable, backgroundAllowed)
                    : forcedStrategy;
            PrescanState state = new PrescanState(key, mode, maxBackPage, strategy);
            log.info("[return-item-prescan] strategy selected: key={} strategy={} trackerGreen={} backgroundAllowed={} mode={} maxBackPage={}",
                    key, strategy, trackerGreenAvailable, backgroundAllowed, mode, maxBackPage);
            return state;
        });
    }

    private Strategy chooseStrategy(boolean trackerGreenAvailable, boolean backgroundAllowed) {
        List<Strategy> candidates = new ArrayList<>();
        if (trackerGreenAvailable) {
            candidates.add(Strategy.AFTER_TRACKER_GREEN);
        }
        if (backgroundAllowed) {
            candidates.add(Strategy.BACKGROUND_PATHING);
        }
        candidates.add(Strategy.IN_COMBAT_RANDOM);
        candidates.add(Strategy.SKIP);
        return candidates.get(ThreadLocalRandom.current().nextInt(candidates.size()));
    }

    private void runPrescan(TaskExecutionContext context, PrescanState state, String source, boolean fallbackToCombat) {
        TaskCheckpoint.throwIfStopRequested(context, "Return item prescan interrupted");
        state.inProgress = true;
        if (state.mode == Mode.MAIN_BAG_TASK_PAGE) {
            BagService.ReturnItemPrescanSnapshots snapshots = bagService.captureMainBagTaskPagePrescanSnapshots(source, context);
            if (snapshots == null) {
                finishPrescan(state, null, source, fallbackToCombat);
                return;
            }
            CompletableFuture.supplyAsync(() -> bagService.matchMainBagTaskPagePrescanSnapshots(
                            snapshots, state.key.template, source))
                    .whenComplete((point, error) -> {
                        if (error != null) {
                            log.warn("[return-item-prescan] async match failed: key={} source={} reason={}",
                                    state.key, source, error.getMessage());
                            finishPrescan(state, null, source, fallbackToCombat);
                            return;
                        }
                        finishPrescan(state, point, source, fallbackToCombat);
                    });
            return;
        }
        try {
            ReturnItemCachePoint point = switch (state.mode) {
                case MAIN_BAG_TASK_PAGE -> throw new IllegalStateException("task-page prescan is asynchronous");
                case MAIN_BAG_FROM_BACK -> bagService.prescanMainBagItemFromBack(
                        state.key.template, state.maxBackPage, source, context);
            };
            finishPrescan(state, point, source, fallbackToCombat);
        } finally {
            state.inProgress = false;
        }
    }

    private void finishPrescan(PrescanState state,
                               ReturnItemCachePoint point,
                               String source,
                               boolean fallbackToCombat) {
        try {
            if (point != null) {
                state.cachePoint = point;
                state.done = true;
                state.combatFallback = false;
                log.info("[return-item-prescan] prescan success: key={} source={} point=({}, {})",
                        state.key, source, point.getClickX(), point.getClickY());
            } else {
                state.done = false;
                state.combatFallback = fallbackToCombat;
                log.warn("[return-item-prescan] prescan failed: key={} source={} fallbackToCombat={}",
                        state.key, source, fallbackToCombat);
            }
        } finally {
            state.inProgress = false;
        }
    }

    private PrescanKey keyFor(TaskExecutionContext context, String taskCode, int round, String template) {
        WindowRuntimeContext runtime = context == null ? null : context.getWindowRuntimeContext();
        WindowNativeBinding binding = runtime == null ? null : runtime.getNativeBinding();
        String windowId = runtime == null ? "no-window" : runtime.getWindowId();
        String hwnd = binding == null || !binding.hasNativeHandle() ? "no-hwnd" : binding.getNativeHandle();
        long taskRunId = context == null ? 0L : context.getTaskRunId();
        return new PrescanKey(
                normalize(taskCode, "unknown-task"),
                normalize(windowId, "no-window"),
                hwnd,
                taskRunId,
                round,
                normalize(template, "unknown-template"));
    }

    private String normalize(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }

    private static final class PrescanState {
        private final PrescanKey key;
        private final Mode mode;
        private final int maxBackPage;
        private final Strategy strategy;
        private volatile boolean inProgress;
        private volatile boolean done;
        private volatile boolean combatFallback;
        private volatile long combatDueAtMs;
        private volatile ReturnItemCachePoint cachePoint;

        private PrescanState(PrescanKey key, Mode mode, int maxBackPage, Strategy strategy) {
            this.key = Objects.requireNonNull(key, "key");
            this.mode = mode == null ? Mode.MAIN_BAG_TASK_PAGE : mode;
            this.maxBackPage = maxBackPage;
            this.strategy = Objects.requireNonNull(strategy, "strategy");
        }
    }

    private record PrescanKey(String taskCode,
                              String windowId,
                              String hwnd,
                              long taskRunId,
                              int round,
                              String template) {
    }
}
