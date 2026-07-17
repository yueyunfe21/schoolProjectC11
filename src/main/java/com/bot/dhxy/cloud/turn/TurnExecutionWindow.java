package com.bot.dhxy.cloud.turn;

import com.bot.dhxy.cloud.turn.protocol.TurnAction;
import com.bot.dhxy.cloud.turn.protocol.TurnPathingIntent;
import com.bot.dhxy.cloud.turn.protocol.TurnPathingSnapshot;
import com.bot.dhxy.cloud.turn.protocol.TurnWindowMetadata;
import com.bot.dhxy.cloud.turn.protocol.TurnWindowRect;
import com.bot.dhxy.runner.stop.TaskStopToken;
import com.bot.dhxy.window.execution.MultiWindowTaskManager;
import com.bot.dhxy.window.execution.RunningTaskHandle;
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

    private TurnExecutionWindow(WindowTaskRunner runner,
                                WindowRuntimeContext context,
                                WindowNativeBinding binding,
                                TurnWindowMetadata metadata) {
        this.runner = runner;
        this.context = context;
        this.binding = binding;
        this.metadata = metadata;
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
        TurnWindowMetadata metadata = new TurnWindowMetadata(
                deviceId,
                context.getWindowId(),
                binding.getTitle(),
                binding.getNativeHandle(),
                binding.getProcessId(),
                windowRect,
                false,
                isStopRequested(runner, context),
                toTurnPathingSnapshot(context.getPathingSnapshot()));
        return new TurnExecutionWindow(runner, context, binding, metadata);
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
                snapshot.getMovementObservedAtMs(),
                snapshot.getUpdatedAtMs(),
                snapshot.isDialogBlocking(),
                snapshot.getDialogBlockingReason(),
                snapshot.getDialogBlockingDetectedAtMs());
    }

    private static boolean isStopRequested(WindowTaskRunner runner, WindowRuntimeContext context) {
        RunningTaskHandle task = runner.getCurrentTask();
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
