package com.bot.dhxy.window.observation;

import com.bot.dhxy.core.GameClientTracker;
import com.bot.dhxy.runner.context.TaskStartupMode;
import com.bot.dhxy.task.model.TaskType;
import com.bot.dhxy.tools.CoordinateHelper;
import com.bot.dhxy.window.runtime.WindowRuntimeContext;
import com.bot.dhxy.window.runtime.WindowTaskContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.Function;

/**
 * Blocks cold-start UI preparation while an exact selected window is already in combat.
 *
 * <p>The gate deliberately reuses {@link LocalCombatSignalMechanics}, which is also the observation runner's sole
 * local combat authority. It performs no input and asks no Cloud radar. Once combat is observed, the selected batch
 * remains queued until the same local mechanics prove exit or the owning start command is cancelled.</p>
 */
@Service
@Slf4j
public class StartupCombatGateService {

    private static final long POLL_INTERVAL_MS = 1_000L;
    private static final long INITIAL_PROBE_RETRY_MS = 500L;
    private static final long INITIAL_PROBE_TIMEOUT_MS = 5_000L;

    private final Function<WindowRuntimeContext, CombatProbe> probeFactory;
    private final Sleeper sleeper;

    @Autowired
    public StartupCombatGateService(GameClientTracker tracker,
                                    CoordinateHelper coordinateHelper,
                                    WindowTaskContextHolder contextHolder) {
        Objects.requireNonNull(tracker, "tracker");
        Objects.requireNonNull(coordinateHelper, "coordinateHelper");
        Objects.requireNonNull(contextHolder, "contextHolder");
        this.probeFactory = context -> new LocalMechanicsProbe(
                context, contextHolder, tracker, coordinateHelper);
        this.sleeper = Thread::sleep;
    }

    StartupCombatGateService(Function<WindowRuntimeContext, CombatProbe> probeFactory, Sleeper sleeper) {
        this.probeFactory = Objects.requireNonNull(probeFactory, "probeFactory");
        this.sleeper = Objects.requireNonNull(sleeper, "sleeper");
    }

    /**
     * Wait for every combat-visible candidate to return to the world before startup preflight may send input.
     *
     * @param candidates exact window contexts mapped to their requested first task; only caller-approved main-task
     *                   candidates belong here
     * @param cancelled true after pause, stop, replacement, or thread interruption invalidates this start command
     * @return {@code AFTER_COMBAT_EXIT_STARTUP} when any candidate waited for exit; otherwise {@code NORMAL}
     */
    public TaskStartupMode awaitCombatExit(Map<WindowRuntimeContext, TaskType> candidates,
                                           BooleanSupplier cancelled) {
        if (candidates == null || candidates.isEmpty()) {
            return TaskStartupMode.NORMAL;
        }
        BooleanSupplier cancellation = Objects.requireNonNull(cancelled, "cancelled");
        Map<WindowRuntimeContext, CombatProbe> probes = new LinkedHashMap<>();
        try {
            candidates.keySet().stream().filter(Objects::nonNull)
                    .forEach(context -> probes.put(context, probeFactory.apply(context)));
            List<WindowRuntimeContext> inCombat = new ArrayList<>();
            for (Map.Entry<WindowRuntimeContext, CombatProbe> entry : probes.entrySet()) {
                LocalCombatSignalMechanics.Signal initial = awaitInitialSignal(
                        entry.getKey(), entry.getValue(), cancellation);
                if (cancellation.getAsBoolean()) {
                    return TaskStartupMode.NORMAL;
                }
                if (initial.state() == LocalCombatSignalMechanics.State.VISIBLE) {
                    inCombat.add(entry.getKey());
                }
            }
            if (inCombat.isEmpty()) {
                return TaskStartupMode.NORMAL;
            }

            long startedAt = System.currentTimeMillis();
            candidates.forEach((context, taskType) -> {
                context.markQueued(taskType);
                context.markRuntimeWarning("战斗中启动：等待战斗结束后继续");
            });
            log.info("Startup combat defer started: windows={} tasks={}",
                    inCombat.stream().map(WindowRuntimeContext::getWindowId).toList(), candidates.values());

            int polls = 0;
            while (!inCombat.isEmpty() && !cancellation.getAsBoolean()) {
                for (WindowRuntimeContext context : List.copyOf(inCombat)) {
                    CombatProbe probe = probes.get(context);
                    LocalCombatSignalMechanics.Signal combat = probe.combat();
                    LocalCombatSignalMechanics.Signal minimap = probe.minimap();
                    if (confirmsExit(combat, minimap)) {
                        inCombat.remove(context);
                        log.info("Startup combat exit confirmed: windowId={} source={}/{}",
                                context.getWindowId(), combat.wireValue(), minimap.wireValue());
                    }
                }
                polls++;
                if (!inCombat.isEmpty() && !cancellation.getAsBoolean()) {
                    sleep(POLL_INTERVAL_MS);
                }
            }
            if (cancellation.getAsBoolean()) {
                return TaskStartupMode.NORMAL;
            }
            log.info("Startup combat defer finished: windows={} elapsedMs={} polls={}",
                    candidates.keySet().stream().map(WindowRuntimeContext::getWindowId).toList(),
                    Math.max(0L, System.currentTimeMillis() - startedAt), polls);
            return TaskStartupMode.AFTER_COMBAT_EXIT_STARTUP;
        } finally {
            probes.values().forEach(CombatProbe::close);
        }
    }

    private LocalCombatSignalMechanics.Signal awaitInitialSignal(WindowRuntimeContext context,
                                                                 CombatProbe probe,
                                                                 BooleanSupplier cancelled) {
        long deadline = System.nanoTime() + Duration.ofMillis(INITIAL_PROBE_TIMEOUT_MS).toNanos();
        LocalCombatSignalMechanics.Signal signal = probe.combat();
        while (signal.state() == LocalCombatSignalMechanics.State.UNAVAILABLE
                && !cancelled.getAsBoolean()
                && System.nanoTime() < deadline) {
            sleep(INITIAL_PROBE_RETRY_MS);
            signal = probe.combat();
        }
        if (signal.state() == LocalCombatSignalMechanics.State.UNAVAILABLE && !cancelled.getAsBoolean()) {
            throw new StartupCombatProbeException(
                    "启动前无法读取本地战斗画面，未执行队伍预检：" + context.getWindowId());
        }
        return signal;
    }

    private static boolean confirmsExit(LocalCombatSignalMechanics.Signal combat,
                                        LocalCombatSignalMechanics.Signal minimap) {
        if (minimap.state() == LocalCombatSignalMechanics.State.VISIBLE) {
            return true;
        }
        return combat.state() == LocalCombatSignalMechanics.State.ABSENT
                && minimap.state() == LocalCombatSignalMechanics.State.ABSENT;
    }

    private void sleep(long millis) {
        try {
            sleeper.sleep(millis);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        }
    }

    interface CombatProbe extends AutoCloseable {
        LocalCombatSignalMechanics.Signal combat();

        LocalCombatSignalMechanics.Signal minimap();

        @Override
        default void close() {
        }
    }

    @FunctionalInterface
    interface Sleeper {
        void sleep(long millis) throws InterruptedException;
    }

    public static final class StartupCombatProbeException extends IllegalStateException {
        public StartupCombatProbeException(String message) {
            super(message);
        }
    }

    /** One exact-HWND full frame per tick; every combat/minimap stage is an in-memory crop of that frame. */
    private static final class LocalMechanicsProbe implements CombatProbe {
        private final WindowRuntimeContext context;
        private final WindowTaskContextHolder contextHolder;
        private final GameClientTracker tracker;
        private final CoordinateHelper coordinateHelper;
        private final LocalCombatSignalMechanics mechanics;
        private BufferedImage cycleFrame;
        private int[] cycleFrameRect;

        private LocalMechanicsProbe(WindowRuntimeContext context,
                                    WindowTaskContextHolder contextHolder,
                                    GameClientTracker tracker,
                                    CoordinateHelper coordinateHelper) {
            this.context = context;
            this.contextHolder = contextHolder;
            this.tracker = tracker;
            this.coordinateHelper = coordinateHelper;
            this.mechanics = new LocalCombatSignalMechanics(tracker, coordinateHelper);
            this.mechanics.bindCycleFrameCropper(this::cropCycleFrame);
        }

        @Override
        public LocalCombatSignalMechanics.Signal combat() {
            refreshCycleFrame();
            return contextHolder.callWith(context, mechanics::sample);
        }

        @Override
        public LocalCombatSignalMechanics.Signal minimap() {
            return contextHolder.callWith(context, mechanics::sampleMinimap);
        }

        private void refreshCycleFrame() {
            if (cycleFrame != null) {
                cycleFrame.flush();
            }
            contextHolder.callWith(context, () -> {
                cycleFrameRect = coordinateHelper.getScaledRect(0, 0, 1024, 768);
                cycleFrame = tracker.captureToMemory(
                        "startup-combat:shared-cycle-frame",
                        cycleFrameRect[0], cycleFrameRect[1], cycleFrameRect[2], cycleFrameRect[3]);
                return null;
            });
        }

        private BufferedImage cropCycleFrame(int[] scaledRect) {
            if (cycleFrame == null || cycleFrameRect == null || scaledRect == null) {
                return null;
            }
            int left = Math.max(0, scaledRect[0] - cycleFrameRect[0]);
            int top = Math.max(0, scaledRect[1] - cycleFrameRect[1]);
            int right = Math.min(cycleFrame.getWidth(), scaledRect[2] - cycleFrameRect[0]);
            int bottom = Math.min(cycleFrame.getHeight(), scaledRect[3] - cycleFrameRect[1]);
            int width = right - left;
            int height = bottom - top;
            if (width <= 0 || height <= 0) {
                return null;
            }
            BufferedImage crop = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = crop.createGraphics();
            graphics.drawImage(cycleFrame, 0, 0, width, height, left, top, right, bottom, null);
            graphics.dispose();
            return crop;
        }

        @Override
        public void close() {
            mechanics.reset();
            if (cycleFrame != null) {
                cycleFrame.flush();
                cycleFrame = null;
            }
            cycleFrameRect = null;
        }
    }
}
