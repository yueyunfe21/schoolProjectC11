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

    /** {@link #awaitAndForceRemoveStoppedRemote} 的结果，供调用方决定告警措辞。 */
    public enum ForcedRemoval {
        /** 当前没有注册的远程 loop，无需处理。 */
        NOT_REGISTERED,
        /** loop 已停止且云端确认了终止，按常规移除。 */
        REMOVED_CONFIRMED,
        /** loop 已停止但云端未确认终止，按停止语义强制移除（记 warn）。 */
        REMOVED_UNCONFIRMED
    }

    /**
     * 停止路径专用：等待该 loop 真正停止后移除它，**即使云端没有确认终止**。
     *
     * <p>与 {@link #awaitAndRemoveStoppedRemote} 的唯一差别是最后那道"云端确认"门。启动路径必须
     * 保持 fail-closed（拿不到确认就不敢新建 run，防双主）；但停止是用户手里最后的出路，它的语义是
     * "我不要它了"，不该反过来要求云端先点头。</p>
     *
     * <p>2026-08-21 实锤：PAUSE_RESUME 建的新 loop 在 27ms 后于检查点干净停止——从未启动成功、
     * 从未拿到云端终态、也没有失败，恰好落在 {@link #canRemoveStoppedLoop} 四个豁免条件之外。
     * 此后该窗口启动被拒（云端未确认终止）、停止也被拒（同一道门），两条路都堵死，只能重启客户端。
     * 两个窗口因此静止了 13 分钟。</p>
     *
     * <p>强制移除的安全性：①本地 loop 已确认停止（awaitStopped 成功），不存在还在跑的本地执行体；
     * ②云端 {@code CloudTurnTaskRuntime} 自己会跑完并终结它的队列，不依赖客户端；③即便云端那边
     * 仍有活跃 RunSlot，后续新 start 会带着不同的 startRequestId 撞上云端的 typed CONFLICT，
     * 云端"绝不替换、排队或停止活跃 run"——双主由云端侧兜底，不需要客户端用死锁来防。</p>
     *
     * @param windowId 精确窗口标识
     * @return 移除结果；未注册返回 {@link ForcedRemoval#NOT_REGISTERED}
     */
    public ForcedRemoval awaitAndForceRemoveStoppedRemote(String windowId) {
        String exactWindowId = requireExactIdentity(windowId, "windowId");
        WindowTurnLoop loop;
        synchronized (modeMonitor) {
            loop = loopRegistry.find(exactWindowId).orElse(null);
            if (loop == null) {
                return ForcedRemoval.NOT_REGISTERED;
            }
        }
        try {
            if (!loop.awaitStopped(Duration.ofMillis(longWaitTimeoutMs))) {
                // 本地 loop 还没停就强移，等于放任一个仍在发指令的执行体脱离登记 —— 这个门必须留着。
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
                return ForcedRemoval.NOT_REGISTERED;
            }
            boolean confirmed = canRemoveStoppedLoop(loop);
            loopRegistry.remove(exactWindowId);
            return confirmed ? ForcedRemoval.REMOVED_CONFIRMED : ForcedRemoval.REMOVED_UNCONFIRMED;
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
        /*
         * 2026-08-25 21:44 五环轮间死锁(473 案,四窗逐个卡死):轮间自动重启的新 loop 因 guard
         * 拒绝而被停掉——它"发过 start、无云端终态、未被显式拒、无失败",落在上面四条豁免之外,
         * 滞留注册表后每 4 秒的下一次重试又被它自己挡住,死局自我复制(与 08-21 PAUSE_RESUME
         * 27ms 干净停止同款)。第五豁免:云端从未 ACK 过这个 start(startAckAccepted 恒 false)
         * = 云端从未为该 startRequestId 建立 RunSlot,移除它零双主风险——比下方 force-remove
         * 注释里的安全论证③更强(那条还允许云端有活跃 RunSlot 靠 typed CONFLICT 兜底)。
         * 拿到过 ACK 而无终态的 loop 仍保持 fail-closed。
         */
        return !loop.hasTaskStartRequest()
                || !loop.hasAcceptedStartAck()
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
