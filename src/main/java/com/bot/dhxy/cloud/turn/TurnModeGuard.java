package com.bot.dhxy.cloud.turn;

import com.bot.dhxy.cloud.turn.protocol.TurnWindowMetadata;
import com.bot.dhxy.window.execution.MultiWindowTaskManager;
import com.bot.dhxy.window.execution.WindowTaskRunner;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/** Exact-window policy boundary preventing simultaneous local and remote execution modes. */
public final class TurnModeGuard {

    private final Object modeMonitor = new Object();
    private final MultiWindowTaskManager taskManager;
    private final TurnLoopRegistry loopRegistry;
    private final long longWaitTimeoutMs;

    public TurnModeGuard(MultiWindowTaskManager taskManager,
                         TurnLoopRegistry loopRegistry,
                         long longWaitTimeoutMs) {
        this.taskManager = Objects.requireNonNull(taskManager, "taskManager");
        this.loopRegistry = Objects.requireNonNull(loopRegistry, "loopRegistry");
        if (longWaitTimeoutMs <= 0L) {
            throw new IllegalArgumentException("longWaitTimeoutMs must be positive");
        }
        this.longWaitTimeoutMs = longWaitTimeoutMs;
    }

    /**
     * Checks every exact window and performs the real local submission in the same synchronized boundary.
     *
     * @param windowIds exact nonblank window identities that the local operation may submit
     * @param localStart real local registration/submission work; invoked once while the mode boundary is held
     * @param <T> local command result type
     * @return the local command result
     * @throws ModeConflictException when any exact window already has a registered remote loop
     */
    public <T> T startLocal(Collection<String> windowIds, Supplier<T> localStart) {
        List<String> exactWindowIds = requireExactWindowIds(windowIds);
        Objects.requireNonNull(localStart, "localStart");
        synchronized (modeMonitor) {
            for (String windowId : exactWindowIds) {
                if (loopRegistry.find(windowId).isPresent()) {
                    throw new ModeConflictException(
                            windowId,
                            "local start rejected because a remote turn loop is registered for windowId=" + windowId);
                }
            }
            return localStart.get();
        }
    }

    /**
     * Creates and starts one remote loop only while the exact local runner is not running.
     *
     * @param deviceId immutable nonblank device identity
     * @param windowId exact nonblank window identity
     * @param windowMetadataSupplier live metadata supplier passed unchanged to the per-window loop
     * @return the newly registered and started loop
     * @throws ModeConflictException when the exact local runner is absent, shut down, or currently running
     */
    public WindowTurnLoop startRemote(String deviceId,
                                      String windowId,
                                      Supplier<TurnWindowMetadata> windowMetadataSupplier) {
        String exactDeviceId = requireExactIdentity(deviceId, "deviceId");
        String exactWindowId = requireExactIdentity(windowId, "windowId");
        Objects.requireNonNull(windowMetadataSupplier, "windowMetadataSupplier");
        synchronized (modeMonitor) {
            WindowTaskRunner runner = taskManager.getRunner(exactWindowId).orElse(null);
            if (runner == null) {
                throw new ModeConflictException(
                        exactWindowId,
                        "remote start rejected because no local runner is registered for windowId=" + exactWindowId);
            }
            if (runner.isShutdown()) {
                throw new ModeConflictException(
                        exactWindowId,
                        "remote start rejected because the local runner is shut down for windowId=" + exactWindowId);
            }
            if (runner.isRunning()) {
                throw new ModeConflictException(
                        exactWindowId,
                        "remote start rejected because the local runner is active for windowId=" + exactWindowId);
            }
            WindowTurnLoop loop = loopRegistry.create(
                    exactDeviceId,
                    exactWindowId,
                    longWaitTimeoutMs,
                    windowMetadataSupplier);
            try {
                loop.start();
                return loop;
            } catch (RuntimeException | Error startFailure) {
                removeStoppedLoopCreatedByThisStart(exactWindowId, loop, startFailure);
                throw startFailure;
            }
        }
    }

    private void removeStoppedLoopCreatedByThisStart(String windowId,
                                                      WindowTurnLoop createdLoop,
                                                      Throwable startFailure) {
        if (createdLoop.isRunning() || loopRegistry.find(windowId).orElse(null) != createdLoop) {
            return;
        }
        try {
            loopRegistry.remove(windowId);
        } catch (RuntimeException cleanupFailure) {
            startFailure.addSuppressed(cleanupFailure);
        }
    }

    private static List<String> requireExactWindowIds(Collection<String> windowIds) {
        if (windowIds == null || windowIds.isEmpty()) {
            throw new IllegalArgumentException("windowIds must not be empty");
        }
        List<String> exactWindowIds = new ArrayList<>(windowIds.size());
        for (String windowId : windowIds) {
            exactWindowIds.add(requireExactIdentity(windowId, "windowId"));
        }
        return List.copyOf(exactWindowIds);
    }

    private static String requireExactIdentity(String value, String fieldName) {
        if (value == null || value.isBlank() || !value.equals(value.trim())) {
            throw new IllegalArgumentException(fieldName + " must be nonblank without surrounding whitespace");
        }
        return value;
    }

    /** Typed rejection carrying only the exact conflicting window identity. */
    public static final class ModeConflictException extends IllegalStateException {

        private final String windowId;

        private ModeConflictException(String windowId, String message) {
            super(message);
            this.windowId = windowId;
        }

        public String windowId() {
            return windowId;
        }
    }
}
