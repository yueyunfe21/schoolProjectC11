package com.bot.dhxy.cloud.turn;

import com.bot.dhxy.cloud.turn.protocol.TurnTaskStartRequest;
import com.bot.dhxy.cloud.turn.protocol.TurnMapSurveyCommand;
import com.bot.dhxy.cloud.turn.protocol.TurnMapSurveyResult;
import com.bot.dhxy.cloud.turn.protocol.TurnWindowMetadata;
import com.bot.dhxy.window.execution.MultiWindowTaskManager;
import com.bot.dhxy.window.execution.WindowTaskRunner;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.concurrent.CompletableFuture;

/** Exact-window policy boundary preventing simultaneous local and remote execution modes. */
public final class TurnModeGuard {

    private final Object modeMonitor = new Object();
    private final MultiWindowTaskManager taskManager;
    private final TurnLoopRegistry loopRegistry;
    private final long longWaitTimeoutMs;
    private final String deviceId;

    public TurnModeGuard(MultiWindowTaskManager taskManager,
                         TurnLoopRegistry loopRegistry,
                         long longWaitTimeoutMs) {
        this(taskManager, loopRegistry, longWaitTimeoutMs, "dhxy-client");
    }

    public TurnModeGuard(MultiWindowTaskManager taskManager,
                         TurnLoopRegistry loopRegistry,
                         long longWaitTimeoutMs,
                         String deviceId) {
        this.taskManager = Objects.requireNonNull(taskManager, "taskManager");
        this.loopRegistry = Objects.requireNonNull(loopRegistry, "loopRegistry");
        if (longWaitTimeoutMs <= 0L) {
            throw new IllegalArgumentException("longWaitTimeoutMs must be positive");
        }
        this.longWaitTimeoutMs = longWaitTimeoutMs;
        this.deviceId = requireExactIdentity(deviceId, "deviceId");
    }

    public String deviceId() {
        return deviceId;
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
        return startRemoteInternal(deviceId, windowId, windowMetadataSupplier, null);
    }

    /**
     * TURN-40D remote overload: same exact-window mutex and runner gating, additionally carrying the one immutable
     * {@link TurnTaskStartRequest} into the created loop so the remote start rides every turn until acknowledged.
     * The three-argument form and its callers are unchanged.
     *
     * @param startRequest non-null immutable remote start request.
     */
    public WindowTurnLoop startRemote(String deviceId,
                                      String windowId,
                                      Supplier<TurnWindowMetadata> windowMetadataSupplier,
                                      TurnTaskStartRequest startRequest) {
        return startRemoteInternal(deviceId, windowId, windowMetadataSupplier,
                Objects.requireNonNull(startRequest, "startRequest"));
    }

    private WindowTurnLoop startRemoteInternal(String deviceId,
                                               String windowId,
                                               Supplier<TurnWindowMetadata> windowMetadataSupplier,
                                               TurnTaskStartRequest startRequest) {
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
            WindowTurnLoop existingLoop = loopRegistry.find(exactWindowId).orElse(null);
            if (existingLoop != null) {
                if (existingLoop.isRunning()) {
                    throw new ModeConflictException(
                            exactWindowId,
                            "remote start rejected because a remote turn loop is already running for windowId="
                                    + exactWindowId);
                }
                if (existingLoop.lastFailure() == null) {
                    throw new ModeConflictException(
                            exactWindowId,
                            "remote start rejected because a stopped remote turn loop is still registered for windowId="
                                    + exactWindowId);
                }
                // An uncertain transport restart must reuse the exact loop, start request and retained outcome.
                // Creating a replacement request here could start the same Cloud task twice.
                existingLoop.start();
                return existingLoop;
            }
            WindowTurnLoop loop = startRequest == null
                    ? loopRegistry.create(exactDeviceId, exactWindowId, longWaitTimeoutMs, windowMetadataSupplier)
                    : loopRegistry.create(
                            exactDeviceId, exactWindowId, longWaitTimeoutMs, windowMetadataSupplier, startRequest);
            try {
                loop.start();
                return loop;
            } catch (RuntimeException | Error startFailure) {
                removeStoppedLoopCreatedByThisStart(exactWindowId, loop, startFailure);
                throw startFailure;
            }
        }
    }

    /**
     * TURN-40D: exact-created-loop start-failure cleanup policy. When {@link #startRemote} creates and registers a loop
     * but {@code loop.start()} throws, this retires only the exact loop this start created and only while it is stopped
     * and still the registered one for the window; a still-running loop or a non-identical registered loop is left
     * untouched. A cleanup failure never masks the original start failure — it is attached as a suppressed exception.
     * Package-visible so the loop-package contract test can exercise the real-registry removal/non-removal policy
     * directly without fabricating a start() failure.
     */
    void removeStoppedLoopCreatedByThisStart(String windowId,
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

    /**
     * TURN-40D: stops and unregisters the exact remote loop under the same mode monitor, so no local start can
     * interleave with the teardown. The loop is interrupted, joined within a bounded wait, and only then removed —
     * {@link TurnLoopRegistry#remove} itself refuses to retire a still-running loop, so a loop that survives the
     * bounded wait is never silently removed. Returns false when no remote loop is registered for the window.
     *
     * @param windowId exact nonblank window identity.
     * @return true when a registered remote loop was stopped and removed; false when none was registered.
     */
    public boolean stopRemote(String windowId) {
        if (!requestRemoteStop(windowId)) {
            return false;
        }
        return awaitAndRemoveStoppedRemote(windowId);
    }

    /**
     * Broadcasts the graceful stop checkpoint for one exact loop without waiting for Cloud termination.
     * Batch callers must invoke this for every selected window before awaiting any one of them.
     *
     * @param windowId exact nonblank window identity
     * @return true when a live registered loop accepted the stop request; false when none is registered
     */
    public boolean requestRemoteStop(String windowId) {
        String exactWindowId = requireExactIdentity(windowId, "windowId");
        synchronized (modeMonitor) {
            WindowTurnLoop loop = loopRegistry.find(exactWindowId).orElse(null);
            if (loop == null) {
                return false;
            }
            loop.requestStop();
            return true;
        }
    }

    /**
     * Waits for the exact loop already asked to stop, then removes it only after Cloud accepted its terminal result.
     * The wait happens outside the mode mutex so other windows can receive their own stop signal immediately.
     *
     * @param windowId exact nonblank window identity
     * @return true when the requested loop stopped and was removed; false when no loop is currently registered
     */
    public boolean awaitAndRemoveStoppedRemote(String windowId) {
        String exactWindowId = requireExactIdentity(windowId, "windowId");
        WindowTurnLoop loop;
        synchronized (modeMonitor) {
            loop = loopRegistry.find(exactWindowId).orElse(null);
            if (loop == null) {
                return false;
            }
        }
        try {
            if (!loop.awaitStopped(Duration.ofMillis(longWaitTimeoutMs))) {
                throw new IllegalStateException(
                        "远程 turn loop 未在时限内停止：windowId=" + exactWindowId);
            }
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(
                    "等待远程 turn loop 停止时被中断：windowId=" + exactWindowId, interrupted);
        }
        synchronized (modeMonitor) {
            if (loopRegistry.find(exactWindowId).orElse(null) != loop) {
                return false;
            }
            if (!canRemoveStoppedLoop(loop)) {
                // A stopped client loop alone is not proof that Cloud released the exact RunSlot. Keeping the loop
                // registered prevents the UI from minting a conflicting startRequestId against a still-active run.
                // Transport-only loops (for example map survey) own no Cloud task RunSlot and have no task terminal.
                throw new IllegalStateException(
                        "云端未确认终止，保留远程 turn loop：windowId=" + exactWindowId);
            }
            loopRegistry.remove(exactWindowId);
            return true;
        }
    }

    static boolean canRemoveStoppedLoop(WindowTurnLoop loop) {
        /*
         * A loop that died on a local failure can never satisfy hasAcceptedTaskTerminal: fetching the
         * Cloud terminal is that same loop's job, and it is dead. Holding it registered anyway wedged the
         * whole window — every later start was refused with 云端未确认终止 while the stop button answered
         * 当前没有远程 turn loop, and the only way out was restarting the client. A wild-battle run hit
         * this on five windows at once: each loop NPE'd seconds after start, and from then on every start
         * click just replayed the team-role hover sweep across the windows and failed again. The Cloud
         * runtime finishes its own queue regardless (its startup turn times out and fails the run), so
         * removing the dead loop risks no double-started RunSlot.
         */
        return !loop.hasTaskStartRequest()
                || loop.hasAcceptedTaskTerminal()
                || loop.wasTaskStartExplicitlyRejected()
                || loop.lastFailure() != null;
    }

    /**
     * TURN-40D: flips the exact remote loop's live pause checkpoint. Pause only changes the Cloud checkpoint flag the
     * loop projects onto its metadata; the long-wait loop stays alive and no local mechanic is parked. Returns false
     * when no remote loop is registered for the window.
     */
    public boolean pauseRemote(String windowId) {
        String exactWindowId = requireExactIdentity(windowId, "windowId");
        synchronized (modeMonitor) {
            WindowTurnLoop loop = loopRegistry.find(exactWindowId).orElse(null);
            if (loop == null || !loop.isRunning()) {
                return false;
            }
            loop.requestPause();
            return true;
        }
    }

    /**
     * TURN-40D: clears the exact remote loop's live pause checkpoint. Resume mints no new start request. Returns
     * false when no remote loop is registered for the window.
     */
    public boolean resumeRemote(String windowId) {
        String exactWindowId = requireExactIdentity(windowId, "windowId");
        synchronized (modeMonitor) {
            WindowTurnLoop loop = loopRegistry.find(exactWindowId).orElse(null);
            if (loop == null || !loop.isRunning()) {
                return false;
            }
            loop.requestResume();
            return true;
        }
    }

    /** Returns the live transport state for UI/runtime projection without creating a second state store. */
    public RemoteLoopState remoteState(String windowId) {
        String exactWindowId = requireExactIdentity(windowId, "windowId");
        synchronized (modeMonitor) {
            WindowTurnLoop loop = loopRegistry.find(exactWindowId).orElse(null);
            return loop == null
                    ? RemoteLoopState.absent()
                    : new RemoteLoopState(true, loop.isRunning(), loop.isPauseRequested(),
                    loop.hasAcceptedTaskTerminal(), loop.lastFailure());
        }
    }

    /** Attach one manual survey command to an existing task-free remote loop for the exact window. */
    public CompletableFuture<TurnMapSurveyResult> submitMapSurvey(
            String windowId, TurnMapSurveyCommand command) {
        String exactWindowId = requireExactIdentity(windowId, "windowId");
        synchronized (modeMonitor) {
            WindowTaskRunner runner = taskManager.getRunner(exactWindowId).orElse(null);
            if (runner == null || runner.isRunning()) {
                throw new ModeConflictException(
                        exactWindowId, "MapSurvey rejected because the exact local window is active or absent");
            }
            WindowTurnLoop loop = loopRegistry.find(exactWindowId).orElseThrow(() ->
                    new ModeConflictException(exactWindowId,
                            "MapSurvey requires an existing remote loop for windowId=" + exactWindowId));
            return loop.attachMapSurveyCommand(command);
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

    public record RemoteLoopState(boolean registered,
                                  boolean running,
                                  boolean paused,
                                  boolean terminalAcknowledged,
                                  Throwable lastFailure) {
        private static RemoteLoopState absent() {
            return new RemoteLoopState(false, false, false, false, null);
        }
    }
}
