package com.bot.dhxy.cloud.turn;

import com.bot.dhxy.cloud.turn.protocol.TurnAction;
import com.bot.dhxy.cloud.turn.protocol.TurnPathingIntent;
import com.bot.dhxy.cloud.turn.protocol.TurnPathingSnapshot;
import com.bot.dhxy.cloud.turn.protocol.TurnWindowMetadata;
import com.bot.dhxy.cloud.turn.protocol.TurnWindowRect;
import com.bot.dhxy.runner.context.TaskStartupMode;
import com.bot.dhxy.runner.stop.TaskPauseToken;
import com.bot.dhxy.runner.stop.TaskStopToken;
import com.bot.dhxy.window.execution.MultiWindowTaskManager;
import com.bot.dhxy.window.execution.RemoteTaskHandle;
import com.bot.dhxy.window.execution.WindowTaskRunner;
import com.bot.dhxy.window.model.WindowNativeBinding;
import com.bot.dhxy.window.model.WindowPathingIntent;
import com.bot.dhxy.window.model.WindowPathingSnapshot;
import com.bot.dhxy.window.model.WindowPathingState;
import com.bot.dhxy.window.model.WindowRuntimeStatus;
import com.bot.dhxy.window.runtime.WindowNativeBindingRefreshService;
import com.bot.dhxy.window.runtime.WindowRuntimeContext;

import java.util.Objects;

/**
 * Immutable exact-window execution snapshot resolved once for one turn action.
 *
 * <p>The protocol-owned {@link TurnWindowMetadata} is the sole wire metadata object. The additional
 * runner/context references remain local so later mechanical steps can use the same refreshed HWND
 * binding without resolving or refreshing it again.</p>
 */
public final class TurnExecutionWindow {

    private final WindowTaskRunner runner;
    private final WindowRuntimeContext context;
    private final WindowNativeBinding binding;
    private final TurnWindowMetadata metadata;

    /**
     * TURN-40B-C2 capture-at-resolve: the exact action-owning task handle and its live stop token,
     * frozen with the other snapshot fields. The token object stays live (a later
     * {@code requestStop()} is observable without re-resolving the runner), and the identity
     * predicate compares the runner's current handle against this exact captured reference — same
     * window/task ids are never identity. No TTL, cache, or second authority.
     */
    private final RemoteTaskHandle actionTaskHandle;
    private final TaskStopToken actionStopToken;
    /**
     * The exact action-owning task's live pause token, frozen at the same resolve point as the stop token so a
     * direct keyboard post can honor the running task's pause without a thread-local holder (production turn
     * threads never bind {@code TaskExecutionContextHolder}). The token object stays live; no TTL or second store.
     */
    private final TaskPauseToken actionPauseToken;

    private TurnExecutionWindow(WindowTaskRunner runner,
                                WindowRuntimeContext context,
                                WindowNativeBinding binding,
                                TurnWindowMetadata metadata,
                                RemoteTaskHandle actionTaskHandle,
                                TaskStopToken actionStopToken,
                                TaskPauseToken actionPauseToken) {
        this.runner = runner;
        this.context = context;
        this.binding = binding;
        this.metadata = metadata;
        this.actionTaskHandle = actionTaskHandle;
        this.actionStopToken = actionStopToken;
        this.actionPauseToken = actionPauseToken;
    }

    /**
     * Resolve an action's registered window and refresh its native binding exactly once.
     *
     * @param action validated turn action containing nonblank device and logical window ids.
     * @param taskManager registry that owns the exact logical window runner.
     * @param bindingRefreshService native HWND metadata/geometry refresher.
     * @return one immutable execution snapshot whose rectangle uses screen-absolute pixels.
     * @throws IllegalArgumentException when the action identity is invalid.
     * @throws IllegalStateException when the window or a live HWND binding is unavailable.
     */
    public static TurnExecutionWindow resolveForAction(TurnAction action,
                                                       MultiWindowTaskManager taskManager,
                                                       WindowNativeBindingRefreshService bindingRefreshService) {
        Objects.requireNonNull(action, "action");
        Objects.requireNonNull(taskManager, "taskManager");
        Objects.requireNonNull(bindingRefreshService, "bindingRefreshService");

        String deviceId = requireText(action.deviceId(), "action.deviceId");
        String windowId = requireText(action.windowId(), "action.windowId");
        WindowTaskRunner runner = taskManager.getRunner(windowId)
                .orElseThrow(() -> new IllegalStateException("Turn window is not registered: " + windowId));
        WindowRuntimeContext context = Objects.requireNonNull(
                runner.getWindowContext(), "registered runner window context");
        if (!windowId.equals(context.getWindowId())) {
            throw new IllegalStateException("Turn window id does not match registered context: " + windowId);
        }

        WindowNativeBinding binding = bindingRefreshService.refreshAndCommit(context)
                .orElseThrow(() -> new IllegalStateException("Turn window native binding refresh failed: " + windowId));
        if (!binding.hasNativeHandle() || !binding.hasGeometry()) {
            throw new IllegalStateException("Turn window has no live native handle or geometry: " + windowId);
        }

        TurnWindowRect windowRect = new TurnWindowRect(
                binding.getX(),
                binding.getY(),
                binding.getWidth(),
                binding.getHeight());
        // TURN-40B-C2 single-snapshot resolve: read the action-owning handle exactly once, then derive the
        // metadata stop state, the frozen handle, and the stop/pause tokens all from this one reference. A task
        // replacement during resolve can no longer split the snapshot between the original handle (metadata stop
        // state) and its successor (frozen handle); previously that split let the later identity predicate pass
        // and the queue-owning bag path return generic FAILED instead of typed STOPPED.
        RemoteTaskHandle actionTaskHandle = runner.getRemoteTaskHandle();
        TaskStopToken actionStopToken = actionTaskHandle == null ? null : actionTaskHandle.getStopToken();
        TaskPauseToken actionPauseToken = actionTaskHandle == null ? null : actionTaskHandle.getPauseToken();
        TurnWindowMetadata metadata = new TurnWindowMetadata(
                deviceId,
                context.getWindowId(),
                binding.getTitle(),
                binding.getNativeHandle(),
                binding.getProcessId(),
                windowRect,
                actionPauseToken != null && actionPauseToken.isPauseRequested(),
                isStopRequested(actionTaskHandle, context),
                toTurnPathingSnapshot(context.getPathingSnapshot()),
                context.getRole().name(),
                null,
                null,
                false,
                false,
                TaskStartupMode.NORMAL.name());
        return new TurnExecutionWindow(
                runner, context, binding, metadata, actionTaskHandle, actionStopToken, actionPauseToken);
    }

    public WindowTaskRunner runner() {
        return runner;
    }

    public WindowRuntimeContext context() {
        return context;
    }

    public WindowNativeBinding binding() {
        return binding;
    }

    public TurnWindowMetadata metadata() {
        return metadata;
    }

    /**
     * @return the live stop token of the exact task that owned this window at action resolution,
     *         or null when no task owned it then. The token is never re-resolved: a successor
     *         task's token is unreachable through this snapshot.
     */
    public TaskStopToken actionStopToken() {
        return actionStopToken;
    }

    /**
     * @return the live pause token of the exact task that owned this window at action resolution, or null when
     *         no task owned it then. Never re-resolved: a successor task's token is unreachable through this
     *         snapshot. A direct keyboard post honors this token so a running-task pause blocks it without a queue.
     */
    public TaskPauseToken actionPauseToken() {
        return actionPauseToken;
    }

    /**
     * Live identity predicate for queue-owning local-service admission: true only while the runner
     * still owns the exact captured {@link RemoteTaskHandle} by reference identity. Evaluated at
     * call time (never a snapshot); a resolve-time no-owner stays false, so queue-owning
     * operations fail closed as stopped.
     */
    public boolean isActionTaskStillCurrent() {
        return actionTaskHandle != null && runner.getRemoteTaskHandle() == actionTaskHandle;
    }

    /**
     * Map the DHXY-authoritative local pathing snapshot into the typed wire {@link TurnPathingSnapshot}
     * carried back on {@link TurnWindowMetadata}. Cloud reads this fact mirror read-only and never
     * observes movement itself; an idle snapshot with no intent maps to null so no wire fact is sent.
     */
    private static TurnPathingSnapshot toTurnPathingSnapshot(WindowPathingSnapshot snapshot) {
        if (snapshot == null || snapshot.getIntent() == null) {
            return null;
        }
        WindowPathingIntent intent = snapshot.getIntent();
        TurnPathingIntent turnIntent = new TurnPathingIntent(
                intent.getSource(),
                intent.getIntentId(),
                intent.getTargetMapName(),
                intent.getTargetX(),
                intent.getTargetY(),
                intent.getTolerance(),
                intent.getType() == null ? null : intent.getType().name());
        WindowPathingState state = snapshot.getState() == null
                ? WindowPathingState.UNKNOWN
                : snapshot.getState();
        return new TurnPathingSnapshot(
                state.name(),
                turnIntent,
                snapshot.getCurrentMapName(),
                snapshot.getCurrentX(),
                snapshot.getCurrentY(),
                snapshot.getLocationChangedAtMs(),
                snapshot.isCoordinateMovementObserved(),
                snapshot.getUpdatedAtMs(),
                snapshot.isDialogBlocking(),
                snapshot.getDialogBlockingReason(),
                snapshot.getDialogBlockingDetectedAtMs());
    }

    private static boolean isStopRequested(RemoteTaskHandle task, WindowRuntimeContext context) {
        TaskStopToken stopToken = task == null ? null : task.getStopToken();
        WindowRuntimeStatus status = context.getStatus();
        return (stopToken != null && stopToken.isStopRequested())
                || status == WindowRuntimeStatus.STOPPING
                || status == WindowRuntimeStatus.STOPPED;
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value.trim();
    }
}
