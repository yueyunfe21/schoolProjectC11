package com.bot.dhxy.service.playerstate;

import com.bot.dhxy.driver.BoundWindowCaptureService;
import com.bot.dhxy.input.InputProvider;
import com.bot.dhxy.runner.context.TaskExecutionContext;
import com.bot.dhxy.runner.stop.TaskCheckpoint;
import com.bot.dhxy.runner.stop.TaskSleep;
import com.bot.dhxy.tools.CoordinateHelper;
import com.bot.dhxy.window.model.WindowNativeBinding;
import com.bot.dhxy.window.runtime.WindowNativeBindingRefreshService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.PointerInfo;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

/**
 * W-696-PLAYERSTATE-FIRST-AID-LOCAL-MECHANICS-1: closed local first-aid mechanics extracted verbatim
 * from the committed {@code 696a12b0} {@code PlayerStateService}.
 *
 * <p>This class exposes the two real baseline mechanical boundaries as separate entries and never
 * merges them into a third business flow:</p>
 * <ul>
 *   <li>{@link #probeSupplyNoFocus} mirrors {@code probeFirstAidSupplyNoFocus}: one no-focus bars
 *       capture (no mouse move, no input, no stored plan/cooldown) that reports the ordered per-bar
 *       supply observation.</li>
 *   <li>{@link #healAllDirect} mirrors {@code healAllDirect} wrapped by {@code healAll(taskContext)}:
 *       it must run inside the serialized {@code dhxy-input-action-worker}; it takes one direct
 *       {@link TaskCheckpoint} before and after the whole segment and then heals the four bars in the
 *       fixed order 人物血 -> 人物法 -> 宝宝血 -> 宝宝法, each one running initial judge -> {@code +10}
 *       counter-check -> {@code 350ms} confirm capture -> in-place right-click {@code 100ms} ->
 *       {@code 800ms} settle before moving to the next bar. There is no per-target stop gate and no
 *       nested submit.</li>
 * </ul>
 *
 * <p>Constants, colour formulas, sample radius, {@code +10}, and the 350/100/800ms delays are
 * preserved byte-for-byte. Only the capture surface moves to the binding-scoped
 * {@link BoundWindowCaptureService} (raw window-client ROI, no separate scale step) and the input to
 * the direct {@link InputProvider}. This class stores no cooldown, task phase, team strategy, or
 * cross-call state, and owns no session/ledger/TTL/retry. Stop and interruption are expressed only
 * through {@link TaskCheckpoint}; the mechanics carries no local STOPPED status.</p>
 */
@Slf4j
@Service
public final class PlayerStateFirstAidLocalMacroMechanics {

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
    private static final int HEAL_RIGHT_CLICK_DELAY_MS = 100;
    private static final int HEAL_SETTLE_DELAY_MS = 800;

    private static final int GAME_CLIENT_WIDTH = 1024;
    private static final int GAME_CLIENT_HEIGHT = 768;
    private static final int SAFE_MOUSE_FORBIDDEN_LEFT_REL_X = 761;
    private static final int SAFE_MOUSE_FORBIDDEN_BOTTOM_REL_Y = 147;
    private static final int SAFE_MOUSE_HOVER_CLEAR_DELAY_MS = 300;
    private static final int PLAYER_STATE_MOUSE_OBSTRUCTION_PADDING = 12;

    private static final String INPUT_WORKER_THREAD_MARK = "dhxy-input-action-worker";

    private final BoundWindowCaptureService captureService;
    private final InputProvider inputProvider;
    private final CoordinateHelper coordinateHelper;
    private final WindowNativeBindingRefreshService bindingRefreshService;

    public PlayerStateFirstAidLocalMacroMechanics(BoundWindowCaptureService captureService,
                                                  InputProvider inputProvider,
                                                  CoordinateHelper coordinateHelper,
                                                  WindowNativeBindingRefreshService bindingRefreshService) {
        this.captureService = Objects.requireNonNull(captureService, "captureService");
        this.inputProvider = Objects.requireNonNull(inputProvider, "inputProvider");
        this.coordinateHelper = Objects.requireNonNull(coordinateHelper, "coordinateHelper");
        this.bindingRefreshService = Objects.requireNonNull(bindingRefreshService, "bindingRefreshService");
    }

    /**
     * No-focus supply probe mirroring baseline {@code probeFirstAidSupplyNoFocus}: one no-mouse-move,
     * no-input bars snapshot that reports the ordered per-bar supply observation. Stores nothing.
     *
     * @param binding exact native-window binding; screen-absolute base with a handle and geometry
     * @param context stop-aware context; may be null (mirrors the legacy nullable checkpoint)
     * @param intent closed four-target enable/threshold intent
     * @return non-null closed result; snapshot status plus one observation per bar in fixed order
     */
    public NoFocusProbeResult probeSupplyNoFocus(WindowNativeBinding binding,
                                                 TaskExecutionContext context,
                                                 FirstAidIntent intent) {
        Objects.requireNonNull(intent, "intent");
        TaskCheckpoint.throwIfStopRequested(context, "player-state first-aid no-focus probe");
        if (binding == null || !binding.hasNativeHandle() || !binding.hasGeometry()) {
            return new NoFocusProbeResult(ProbeSnapshotStatus.CAPTURE_UNAVAILABLE, List.of(), null, null);
        }
        // S1: refresh the live window geometry before the no-focus bars capture and report the capture-time
        // base, so the cloud can anchor a cached plan on the exact base these bars were read from (no
        // old-base/new-bars race). A failed refresh keeps the existing capture-unavailable result.
        Optional<WindowNativeBinding> refreshed = bindingRefreshService.refreshGeometry(binding);
        if (refreshed.isEmpty() || !refreshed.get().hasGeometry()) {
            return new NoFocusProbeResult(ProbeSnapshotStatus.CAPTURE_UNAVAILABLE, List.of(), null, null);
        }
        WindowNativeBinding freshBinding = refreshed.get();
        if (freshBinding.getX() == -1) {
            // Baseline PlayerStateService:267-270 returns UNKNOWN (no bars capture, no health-check
            // consumed) when the window base X is the -1 unavailable sentinel. hasGeometry() only checks
            // width/height, so add the missing base-unavailable gate here before any capture.
            return new NoFocusProbeResult(ProbeSnapshotStatus.CAPTURE_UNAVAILABLE, List.of(), null, null);
        }
        BufferedImage bars = captureBarsSnapshotNoFocus(freshBinding);
        if (bars == null) {
            return new NoFocusProbeResult(ProbeSnapshotStatus.CAPTURE_UNAVAILABLE, List.of(), null, null);
        }
        List<ProbeObservation> observations = new ArrayList<>();
        try {
            for (BarTarget bar : orderedBars(intent)) {
                if (!bar.toggle().enabled()) {
                    observations.add(new ProbeObservation(bar.name(), ProbeStatus.DISABLED, null, null));
                    continue;
                }
                observations.add(probeBar(bars, bar));
            }
        } finally {
            bars.flush();
        }
        return new NoFocusProbeResult(ProbeSnapshotStatus.READABLE, List.copyOf(observations),
                freshBinding.getX(), freshBinding.getY());
    }

    /**
     * Direct heal-all mirroring baseline {@code healAllDirect} wrapped by {@code healAll(taskContext)}:
     * one direct checkpoint before and after the whole segment, then the four bars in fixed order, each
     * fully judged/confirmed/clicked before the next. Must run inside the serialized input worker.
     *
     * @param binding exact native-window binding; screen-absolute base with a handle and geometry
     * @param context stop-aware context; may be null (mirrors the legacy nullable checkpoint)
     * @param intent closed four-target enable/threshold intent
     * @return non-null closed result; snapshot status plus one outcome per bar in fixed order
     */
    public HealAllResult healAllDirect(WindowNativeBinding binding,
                                       TaskExecutionContext context,
                                       FirstAidIntent intent) {
        Objects.requireNonNull(intent, "intent");
        if (!isInputWorkerThread()) {
            throw new IllegalStateException(
                    "player-state first-aid heal-all must run inside the exclusive input worker section");
        }
        TaskCheckpoint.throwIfStopRequested(context, "player-state first-aid heal-all before");
        // P1-1: the command may have queued before this exclusive callback ran, so read the live window
        // geometry here — on the input worker, right before the first-frame capture — mirroring 696
        // captureBarsSnapshot -> getScaledRect -> refreshWindowState, whose geometry is read at execution
        // time. A failed refresh keeps the existing capture-failed business result (pass a null binding
        // into the unchanged segment) so heal never captures/clicks with stale coordinates; no retry/TTL.
        WindowNativeBinding freshBinding = binding;
        if (binding != null) {
            Optional<WindowNativeBinding> refreshed = bindingRefreshService.refreshGeometry(binding);
            freshBinding = refreshed.isPresent() && refreshed.get().hasGeometry() ? refreshed.get() : null;
        }
        HealAllResult result = healAllDirectSegment(freshBinding, intent);
        TaskCheckpoint.throwIfStopRequested(context, "player-state first-aid heal-all after");
        return result;
    }

    /**
     * Direct cached-plan first-aid mirroring baseline {@code performCachedFirstAidPlanDirect}. The
     * no-focus detection already happened upstream; this only replays the precomputed ordered
     * right-clicks by the current window base. It must run inside the serialized input worker. It
     * refreshes the live window base after the worker bound its context and keeps the stored plan base as
     * a fallback so it never clicks another window's stale coordinates, moves the mouse away, then
     * right-clicks every target in order with the committed {@code 100ms} click + {@code 800ms} settle.
     * Stores nothing; interruption is expressed only through the input-worker thread interrupt flag.
     *
     * @param binding exact native-window binding used only to refresh the live window base; may be stale
     * @param plan    closed cached plan: stored base plus the ordered targets to right-click
     * @return {@link CachedPlanStatus#COMPLETED} unless the input worker thread was interrupted
     */
    public CachedPlanStatus executeCachedFirstAidPlanDirect(WindowNativeBinding binding,
                                                            CachedFirstAidPlan plan) {
        Objects.requireNonNull(plan, "plan");
        if (!isInputWorkerThread()) {
            throw new IllegalStateException(
                    "player-state first-aid cached plan must run inside the exclusive input worker section");
        }
        // 696 performCachedFirstAidPlanDirect: refresh the live window base after the worker bound its
        // context, but keep the precheck/stored plan base as a fallback so cached healing never clicks
        // with another window's stale coordinates.
        int baseX = plan.baseX();
        int baseY = plan.baseY();
        Optional<WindowNativeBinding> refreshed =
                binding == null ? Optional.empty() : bindingRefreshService.refreshGeometry(binding);
        if (refreshed.isPresent()
                && refreshed.get().hasGeometry()
                && refreshed.get().getX() != -1
                && refreshed.get().getY() != -1) {
            int refreshedBaseX = refreshed.get().getX();
            int refreshedBaseY = refreshed.get().getY();
            if (refreshedBaseX != baseX || refreshedBaseY != baseY) {
                log.info("first-aid cached plan base refreshed: planBase=({}, {}) refreshedBase=({}, {})",
                        baseX, baseY, refreshedBaseX, refreshedBaseY);
            }
            baseX = refreshedBaseX;
            baseY = refreshedBaseY;
        } else {
            log.warn("first-aid cached plan using stored base: refreshed={} storedBase=({}, {})",
                    refreshed.isPresent(), baseX, baseY);
        }

        Point safePoint = randomMouseAwayPoint(baseX, baseY);
        inputProvider.moveMouse(safePoint.x, safePoint.y);
        TaskSleep.sleep(SAFE_MOUSE_HOVER_CLEAR_DELAY_MS);
        for (CachedFirstAidTarget target : plan.targets()) {
            int absX = baseX + target.relX();
            int absY = baseY + target.relY();
            log.warn("🚨 后台预计算命中 [{}]，按当前窗口补给：base=({}, {}) rel=({}, {}) abs=({}, {}) threshold={}%",
                    target.name(), baseX, baseY, target.relX(), target.relY(), absX, absY, target.threshold());
            inputProvider.clickRight(absX, absY, HEAL_RIGHT_CLICK_DELAY_MS);
            TaskSleep.sleep(HEAL_SETTLE_DELAY_MS);
        }
        return Thread.currentThread().isInterrupted()
                ? CachedPlanStatus.INTERRUPTED
                : CachedPlanStatus.COMPLETED;
    }

    private HealAllResult healAllDirectSegment(WindowNativeBinding binding, FirstAidIntent intent) {
        if (binding == null || !binding.hasNativeHandle() || !binding.hasGeometry()) {
            return new HealAllResult(HealSnapshotStatus.CAPTURE_FAILED, List.of());
        }
        BufferedImage bars = captureBarsSnapshot(binding);
        if (bars == null) {
            log.warn("战后体检截图失败，跳过本轮自动补给，避免误点血法条");
            return new HealAllResult(HealSnapshotStatus.CAPTURE_FAILED, List.of());
        }
        List<HealOutcome> outcomes = new ArrayList<>();
        try {
            for (BarTarget bar : orderedBars(intent)) {
                if (!bar.toggle().enabled()) {
                    log.info("🩺 [{}] 补给未启用，跳过检查", bar.name());
                    outcomes.add(new HealOutcome(bar.name(), HealStatus.DISABLED, null, null, null, null));
                    continue;
                }
                outcomes.add(checkAndHealBar(binding, bars, bar));
            }
        } finally {
            bars.flush();
        }
        return new HealAllResult(HealSnapshotStatus.CAPTURED, List.copyOf(outcomes));
    }

    private List<BarTarget> orderedBars(FirstAidIntent intent) {
        List<BarTarget> bars = new ArrayList<>(4);
        bars.add(new BarTarget("人物血量", CHAR_BAR_LEFT_X, CHAR_BAR_RIGHT_X, BAR_HP_Y, true, intent.playerHp()));
        bars.add(new BarTarget("人物法力", CHAR_BAR_LEFT_X, CHAR_BAR_RIGHT_X, BAR_MP_Y, false, intent.playerMp()));
        bars.add(new BarTarget("宝宝血量", PET_BAR_LEFT_X, PET_BAR_RIGHT_X, BAR_HP_Y, true, intent.petHp()));
        bars.add(new BarTarget("宝宝法力", PET_BAR_LEFT_X, PET_BAR_RIGHT_X, BAR_MP_Y, false, intent.petMp()));
        return bars;
    }

    private BufferedImage captureBarsSnapshot(WindowNativeBinding binding) {
        int baseX = binding.getX();
        int baseY = binding.getY();
        int x1 = baseX + BARS_SCAN_LEFT_X;
        int y1 = baseY + BARS_SCAN_TOP_Y;
        int x2 = x1 + BARS_SCAN_W;
        int y2 = y1 + BARS_SCAN_H;
        moveMouseAwayBeforePlayerStateSnapshotIfNeeded(binding, "player-state-bars", new int[]{x1, y1, x2, y2});
        return captureBarsRegion(binding, baseX, baseY, x1, y1, x2, y2);
    }

    private BufferedImage captureBarsSnapshotNoFocus(WindowNativeBinding binding) {
        int baseX = binding.getX();
        int baseY = binding.getY();
        int x1 = baseX + BARS_SCAN_LEFT_X;
        int y1 = baseY + BARS_SCAN_TOP_Y;
        int x2 = x1 + BARS_SCAN_W;
        int y2 = y1 + BARS_SCAN_H;
        return captureBarsRegion(binding, baseX, baseY, x1, y1, x2, y2);
    }

    private BufferedImage captureBarsRegion(WindowNativeBinding binding,
                                            int baseX, int baseY, int x1, int y1, int x2, int y2) {
        Optional<BoundWindowCaptureService.CaptureResult> captured;
        try {
            captured = captureService.captureRegion(binding, baseX, baseY, x1, y1, x2, y2);
        } catch (RuntimeException e) {
            log.warn("[player-state] bars snapshot capture failed: hwnd={} title={} reason={}",
                    binding.getNativeHandle(), binding.getTitle(), e.getMessage(), e);
            return null;
        }
        if (captured == null || captured.isEmpty() || captured.get().image() == null) {
            return null;
        }
        return captured.get().image();
    }

    private void moveMouseAwayBeforePlayerStateSnapshotIfNeeded(WindowNativeBinding binding,
                                                               String source,
                                                               int[] captureRect) {
        Point mouse = currentLogicalMousePoint();
        if (!mouseOverCaptureRect(mouse, captureRect)) {
            log.debug("player-state snapshot mouse clear: source={} mouse={} rect={}",
                    source, formatPoint(mouse), formatRect(captureRect));
            return;
        }
        if (binding.getX() == -1 || binding.getY() == -1) {
            return;
        }
        Point safePoint = randomMouseAwayPoint(binding.getX(), binding.getY());
        log.info("player-state snapshot mouse overlaps capture; move away directly before snapshot: source={} mouse={} rect={} target={}",
                source, formatPoint(mouse), formatRect(captureRect), formatPoint(safePoint));
        inputProvider.moveMouse(safePoint.x, safePoint.y);
        TaskSleep.sleep(SAFE_MOUSE_HOVER_CLEAR_DELAY_MS);
    }

    private boolean mouseOverCaptureRect(Point mouse, int[] captureRect) {
        if (mouse == null || captureRect == null || captureRect.length < 4) {
            return false;
        }
        int left = Math.min(captureRect[0], captureRect[2]) - PLAYER_STATE_MOUSE_OBSTRUCTION_PADDING;
        int right = Math.max(captureRect[0], captureRect[2]) + PLAYER_STATE_MOUSE_OBSTRUCTION_PADDING;
        int top = Math.min(captureRect[1], captureRect[3]) - PLAYER_STATE_MOUSE_OBSTRUCTION_PADDING;
        int bottom = Math.max(captureRect[1], captureRect[3]) + PLAYER_STATE_MOUSE_OBSTRUCTION_PADDING;
        return mouse.x >= left && mouse.x <= right && mouse.y >= top && mouse.y <= bottom;
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

    private Point currentLogicalMousePoint() {
        PointerInfo pointerInfo = MouseInfo.getPointerInfo();
        if (pointerInfo == null) {
            return null;
        }
        double scale = coordinateHelper.getScaleRatio();
        Point physical = pointerInfo.getLocation();
        return new Point((int) Math.round(physical.x / scale), (int) Math.round(physical.y / scale));
    }

    private String formatPoint(Point point) {
        return point == null ? "unknown" : "(" + point.x + ", " + point.y + ")";
    }

    private String formatRect(int[] rect) {
        if (rect == null || rect.length < 4) {
            return "unknown";
        }
        return "(" + rect[0] + ", " + rect[1] + ")-(" + rect[2] + ", " + rect[3] + ")";
    }

    private ProbeObservation probeBar(BufferedImage bars, BarTarget bar) {
        int normalizedThreshold = normalizeThreshold(bar.toggle().threshold());
        int relX = calculateX(bar.leftX(), bar.rightX(), normalizedThreshold);
        int relY = bar.relY();
        String name = bar.name();
        int localX = relX - BARS_SCAN_LEFT_X;
        int localY = relY - BARS_SCAN_TOP_Y;
        if (localX < 0 || localY < 0 || localX >= bars.getWidth() || localY >= bars.getHeight()) {
            log.warn("first-aid no-focus sample out of bounds: name={} rel=({}, {}) snapshot={}x{}",
                    name, relX, relY, bars.getWidth(), bars.getHeight());
            return new ProbeObservation(name, ProbeStatus.UNREADABLE, relX, relY);
        }
        if (isHealthyInSnapshotArea(bars, relX, relY, bar.expectRed())) {
            return new ProbeObservation(name, ProbeStatus.HEALTHY, relX, relY);
        }

        int higherThreshold = Math.min(95, normalizedThreshold + HIGHER_HEALTH_PROBE_OFFSET);
        int higherRelX = calculateX(bar.leftX(), bar.rightX(), higherThreshold);
        if (isHealthyInSnapshotArea(bars, higherRelX, relY, bar.expectRed())) {
            log.info("[{}] no-focus precheck: {}% sample low but {}% still healthy, skip supply",
                    name, normalizedThreshold, higherThreshold);
            return new ProbeObservation(name, ProbeStatus.HEALTHY, relX, relY);
        }

        int rgb = bars.getRGB(localX, localY);
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        log.warn("[{}] no-focus precheck: below {}%, supply needed rgb=({}, {}, {})",
                name, normalizedThreshold, r, g, b);
        return new ProbeObservation(name, ProbeStatus.SUPPLY_NEEDED, relX, relY);
    }

    private HealOutcome checkAndHealBar(WindowNativeBinding binding, BufferedImage bars, BarTarget bar) {
        int normalizedThreshold = normalizeThreshold(bar.toggle().threshold());
        int relX = calculateX(bar.leftX(), bar.rightX(), normalizedThreshold);
        int relY = bar.relY();
        boolean expectRed = bar.expectRed();
        String name = bar.name();
        int localX = relX - BARS_SCAN_LEFT_X;
        int localY = relY - BARS_SCAN_TOP_Y;
        if (localX < 0 || localY < 0 || localX >= bars.getWidth() || localY >= bars.getHeight()) {
            log.warn("战后体检采样点越界：name={} rel=({}, {}) snapshot={}x{}",
                    name, relX, relY, bars.getWidth(), bars.getHeight());
            return new HealOutcome(name, HealStatus.UNREADABLE, relX, relY, null, null);
        }

        int absX = binding.getX() + relX;
        int absY = binding.getY() + relY;
        if (isHealthyInSnapshotArea(bars, relX, relY, expectRed)) {
            return new HealOutcome(name, HealStatus.HEALTHY, relX, relY, null, null);
        }

        int higherThreshold = Math.min(95, normalizedThreshold + HIGHER_HEALTH_PROBE_OFFSET);
        int higherRelX = calculateX(bar.leftX(), bar.rightX(), higherThreshold);
        if (isHealthyInSnapshotArea(bars, higherRelX, relY, expectRed)) {
            log.info("[{}] {}% 采样疑似误判，但 {}% 位置仍有有效颜色，跳过补给", name, normalizedThreshold, higherThreshold);
            return new HealOutcome(name, HealStatus.HEALTHY, relX, relY, null, null);
        }

        TaskSleep.sleep(HEAL_CONFIRM_DELAY_MS);
        BufferedImage confirmBars = captureBarsSnapshot(binding);
        if (confirmBars == null) {
            log.warn("[{}] 疑似低于 {}%，但二次截图失败，跳过补给以避免误点", name, normalizedThreshold);
            return new HealOutcome(name, HealStatus.CAPTURE_FAILED, relX, relY, null, null);
        }
        try {
            if (isHealthyInSnapshotArea(confirmBars, relX, relY, expectRed)) {
                log.info("[{}] 二次确认发现 {}% 位置已有有效颜色，判定首次采样误判，跳过补给", name, normalizedThreshold);
                return new HealOutcome(name, HealStatus.HEALTHY, relX, relY, null, null);
            }
            if (isHealthyInSnapshotArea(confirmBars, higherRelX, relY, expectRed)) {
                log.info("[{}] 二次确认发现 {}% 位置仍有有效颜色，判定首次采样误判，跳过补给", name, higherThreshold);
                return new HealOutcome(name, HealStatus.HEALTHY, relX, relY, null, null);
            }
            int confirmLocalX = relX - BARS_SCAN_LEFT_X;
            int confirmLocalY = relY - BARS_SCAN_TOP_Y;
            int confirmRgb = confirmBars.getRGB(confirmLocalX, confirmLocalY);
            if (healIfUnhealthy(name, absX, absY, confirmRgb, expectRed, normalizedThreshold)) {
                return new HealOutcome(name, HealStatus.EXECUTED, relX, relY, absX, absY);
            }
            return new HealOutcome(name, HealStatus.NO_ACTION, relX, relY, null, null);
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
            inputProvider.clickRight(absX, absY, HEAL_RIGHT_CLICK_DELAY_MS);
            TaskSleep.sleep(HEAL_SETTLE_DELAY_MS);
            return true;
        }

        return false;
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

    private boolean isInputWorkerThread() {
        return Thread.currentThread().getName().contains(INPUT_WORKER_THREAD_MARK);
    }

    /** Closed four-target first-aid intent: enable + threshold per bar, no other business state. */
    public record FirstAidIntent(TargetToggle playerHp,
                                 TargetToggle playerMp,
                                 TargetToggle petHp,
                                 TargetToggle petMp) {

        public FirstAidIntent {
            Objects.requireNonNull(playerHp, "playerHp");
            Objects.requireNonNull(playerMp, "playerMp");
            Objects.requireNonNull(petHp, "petHp");
            Objects.requireNonNull(petMp, "petMp");
        }
    }

    /** One bar toggle: whether the bar is enabled and its raw (pre-normalization) threshold percent. */
    public record TargetToggle(boolean enabled, int threshold) {
    }

    public enum ProbeSnapshotStatus {
        READABLE,
        CAPTURE_UNAVAILABLE
    }

    public enum ProbeStatus {
        DISABLED,
        HEALTHY,
        SUPPLY_NEEDED,
        UNREADABLE
    }

    public enum HealSnapshotStatus {
        CAPTURED,
        CAPTURE_FAILED
    }

    public enum HealStatus {
        DISABLED,
        HEALTHY,
        NO_ACTION,
        EXECUTED,
        UNREADABLE,
        CAPTURE_FAILED
    }

    /**
     * Ordered no-focus probe result; observations follow the fixed bar order. A {@code READABLE} snapshot
     * also carries the capture-time window base ({@code observedBaseX/observedBaseY}); a
     * {@code CAPTURE_UNAVAILABLE} snapshot leaves both null.
     */
    public record NoFocusProbeResult(ProbeSnapshotStatus snapshotStatus,
                                     List<ProbeObservation> observations,
                                     Integer observedBaseX,
                                     Integer observedBaseY) {

        public NoFocusProbeResult {
            Objects.requireNonNull(snapshotStatus, "snapshotStatus");
            observations = List.copyOf(observations);
            boolean readable = snapshotStatus == ProbeSnapshotStatus.READABLE;
            boolean hasBase = observedBaseX != null && observedBaseY != null;
            if ((observedBaseX == null) != (observedBaseY == null)) {
                throw new IllegalArgumentException("observed base coordinates must be present or absent as a pair");
            }
            if (readable != hasBase) {
                throw new IllegalArgumentException("observed base must be present only for a READABLE probe");
            }
        }
    }

    /** One closed per-bar no-focus observation; sample coordinates are window-client pixels. */
    public record ProbeObservation(String name, ProbeStatus status, Integer sampleRelX, Integer sampleRelY) {

        public ProbeObservation {
            Objects.requireNonNull(name, "name");
            Objects.requireNonNull(status, "status");
        }
    }

    /** Ordered heal-all result; outcomes follow the fixed bar order. */
    public record HealAllResult(HealSnapshotStatus snapshotStatus, List<HealOutcome> outcomes) {

        public HealAllResult {
            Objects.requireNonNull(snapshotStatus, "snapshotStatus");
            outcomes = List.copyOf(outcomes);
        }
    }

    /**
     * One closed per-bar heal outcome. Sample coordinates are window-client pixels; click coordinates
     * are screen-absolute pixels and present only for {@link HealStatus#EXECUTED}.
     */
    public record HealOutcome(String name,
                              HealStatus status,
                              Integer sampleRelX,
                              Integer sampleRelY,
                              Integer clickAbsX,
                              Integer clickAbsY) {

        public HealOutcome {
            Objects.requireNonNull(name, "name");
            Objects.requireNonNull(status, "status");
            if ((clickAbsX == null) != (clickAbsY == null)) {
                throw new IllegalArgumentException("click coordinates must be present or absent as a pair");
            }
            boolean executed = status == HealStatus.EXECUTED;
            boolean hasClick = clickAbsX != null;
            if (executed != hasClick) {
                throw new IllegalArgumentException("click coordinates must be present only for EXECUTED");
            }
        }
    }

    /** Closed cached first-aid plan: the stored window base plus the ordered right-click targets. */
    public record CachedFirstAidPlan(int baseX, int baseY, List<CachedFirstAidTarget> targets) {

        public CachedFirstAidPlan {
            targets = List.copyOf(targets);
        }
    }

    /** One ordered cached target: window-relative right-click point and its raw threshold percent. */
    public record CachedFirstAidTarget(String name, int relX, int relY, int threshold) {

        public CachedFirstAidTarget {
            Objects.requireNonNull(name, "name");
        }
    }

    /** Closed cached-plan terminal; mirrors the baseline {@code !isInterrupted()} completion boolean. */
    public enum CachedPlanStatus {
        COMPLETED,
        INTERRUPTED
    }

    private record BarTarget(String name, int leftX, int rightX, int relY, boolean expectRed, TargetToggle toggle) {
    }
}
