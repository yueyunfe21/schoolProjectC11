package com.bot.dhxy.input.action;

import com.bot.dhxy.runner.stop.TaskPauseToken;
import com.bot.dhxy.runner.stop.TaskStopToken;
import com.bot.dhxy.window.model.WindowNativeBinding;
import com.bot.dhxy.window.runtime.WindowRuntimeContext;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * One input request captured for the worker queue.
 *
 * <p>The request stores both the {@link WindowRuntimeContext} and its native binding at submission
 * time, so the worker focuses/sends input to the same window even if another task thread later binds
 * a different context. It contains either a list of {@link InputAction}s or one exclusive callback.</p>
 */
public class InputActionRequest {

    private final WindowRuntimeContext windowContext;
    private final String windowId;
    private final WindowNativeBinding nativeBinding;
    private final long playerIdentityEpoch;
    private final TaskPauseToken pauseToken;
    private final TaskStopToken stopToken;
    private final String description;
    private final List<InputAction> actions;
    private final Supplier<Boolean> exclusiveCallback;
    private final CompletableFuture<Boolean> result = new CompletableFuture<>();
    private final AtomicBoolean cancelled = new AtomicBoolean(false);
    private final AtomicReference<String> cancellationReason = new AtomicReference<>();

    /**
     * Create a normal action-list request.
     *
     * @param windowContext submitting window context; should have a native binding.
     * @param description diagnostic label.
     * @param actions ordered physical actions. The list is copied and null becomes empty.
     */
    public InputActionRequest(WindowRuntimeContext windowContext,
                              String description,
                              List<InputAction> actions) {
        this(windowContext, description, actions, null, null);
    }

    /**
     * Create a normal action-list request with the submitting task's pause token.
     *
     * @param windowContext submitting window context; should have a native binding.
     * @param description diagnostic label.
     * @param actions ordered physical actions. The list is copied and null becomes empty.
     * @param pauseToken pause token captured on the submitting task thread; nullable for debug paths.
     */
    public InputActionRequest(WindowRuntimeContext windowContext,
                              String description,
                              List<InputAction> actions,
                              TaskPauseToken pauseToken) {
        this(windowContext, description, actions, pauseToken, null);
    }

    /**
     * Create a normal action-list request with the submitting task's pause/stop tokens.
     *
     * @param windowContext submitting window context; should have a native binding.
     * @param description diagnostic label.
     * @param actions ordered physical actions. The list is copied and null becomes empty.
     * @param pauseToken pause token captured on the submitting task thread; nullable for debug paths.
     * @param stopToken stop token captured on the submitting task thread; nullable for debug paths.
     */
    public InputActionRequest(WindowRuntimeContext windowContext,
                              String description,
                              List<InputAction> actions,
                              TaskPauseToken pauseToken,
                              TaskStopToken stopToken) {
        this(windowContext, description, actions, null, pauseToken, stopToken);
    }

    /**
     * Create an exclusive callback request.
     *
     * @param windowContext submitting window context; should have a native binding.
     * @param description diagnostic label.
     * @param exclusiveCallback callback executed on the input worker thread.
     */
    public InputActionRequest(WindowRuntimeContext windowContext,
                              String description,
                              Supplier<Boolean> exclusiveCallback) {
        this(windowContext, description, List.of(), exclusiveCallback, null, null);
    }

    /**
     * Create an exclusive callback request with the submitting task's pause token.
     *
     * @param windowContext submitting window context; should have a native binding.
     * @param description diagnostic label.
     * @param exclusiveCallback callback executed on the input worker thread.
     * @param pauseToken pause token captured on the submitting task thread; nullable for debug paths.
     */
    public InputActionRequest(WindowRuntimeContext windowContext,
                              String description,
                              Supplier<Boolean> exclusiveCallback,
                              TaskPauseToken pauseToken) {
        this(windowContext, description, exclusiveCallback, pauseToken, null);
    }

    /**
     * Create an exclusive callback request with the submitting task's pause/stop tokens.
     *
     * @param windowContext submitting window context; should have a native binding.
     * @param description diagnostic label.
     * @param exclusiveCallback callback executed on the input worker thread.
     * @param pauseToken pause token captured on the submitting task thread; nullable for debug paths.
     * @param stopToken stop token captured on the submitting task thread; nullable for debug paths.
     */
    public InputActionRequest(WindowRuntimeContext windowContext,
                              String description,
                              Supplier<Boolean> exclusiveCallback,
                              TaskPauseToken pauseToken,
                              TaskStopToken stopToken) {
        this(windowContext, description, List.of(), exclusiveCallback, pauseToken, stopToken);
    }

    private InputActionRequest(WindowRuntimeContext windowContext,
                               String description,
                               List<InputAction> actions,
                               Supplier<Boolean> exclusiveCallback,
                               TaskPauseToken pauseToken,
                               TaskStopToken stopToken) {
        this.windowContext = windowContext;
        this.windowId = windowContext == null ? null : windowContext.getWindowId();
        this.nativeBinding = windowContext == null ? null : windowContext.getNativeBinding();
        this.playerIdentityEpoch = windowContext == null ? -1L : windowContext.getPlayerIdentityEpoch();
        this.pauseToken = pauseToken;
        this.stopToken = stopToken;
        this.description = description == null ? "" : description;
        this.actions = actions == null ? List.of() : List.copyOf(actions);
        this.exclusiveCallback = exclusiveCallback;
    }

    /** @return submitting window context captured at queue time. */
    public WindowRuntimeContext getWindowContext() { return windowContext; }

    /** @return submitting window id, or null when no context was supplied. */
    public String getWindowId() { return windowId; }

    /** @return native binding captured at queue time, possibly null for rejected/debug paths. */
    public WindowNativeBinding getNativeBinding() { return nativeBinding; }

    /** @return player identity epoch captured at queue time. */
    public long getPlayerIdentityEpoch() { return playerIdentityEpoch; }

    /** @return task pause token captured at queue time, or null outside a managed task. */
    public TaskPauseToken getPauseToken() { return pauseToken; }

    /** @return task stop token captured at queue time, or null outside a managed task. */
    public TaskStopToken getStopToken() { return stopToken; }

    /**
     * @return true when the request still belongs to the same player identity epoch.
     */
    public boolean isPlayerIdentityEpochCurrent() {
        return windowContext == null || playerIdentityEpoch == windowContext.getPlayerIdentityEpoch();
    }

    /**
     * @return true when the submitting task has been paused after this request was queued.
     */
    public boolean isPauseRequested() {
        return pauseToken != null && pauseToken.isPauseRequested();
    }

    /** @return diagnostic label for logs. */
    public String getDescription() { return description; }

    /** @return ordered immutable action list. Empty when this is an exclusive callback request. */
    public List<InputAction> getActions() { return actions; }

    /** @return exclusive callback, or null for normal action-list requests. */
    public Supplier<Boolean> getExclusiveCallback() { return exclusiveCallback; }

    /** @return true when this request should run a callback instead of replaying action objects. */
    public boolean hasExclusiveCallback() { return exclusiveCallback != null; }

    /** @return completion future used by the submitting task thread. */
    public CompletableFuture<Boolean> getResult() { return result; }

    /** @return first recorded cancellation reason, or null when the request has not been cancelled. */
    public String getCancellationReason() { return cancellationReason.get(); }

    /**
     * Mark the request cancelled and unblock the submitter.
     *
     * @param reason diagnostic reason stored for dead-letter logs.
     */
    public void cancel(String reason) {
        cancellationReason.compareAndSet(null, reason == null || reason.isBlank() ? "cancelled" : reason);
        cancelled.set(true);
        result.complete(false);
    }

    /**
     * @return true when the request was cancelled or its completion future was cancelled.
     */
    public boolean isCancelled() {
        return cancelled.get() || result.isCancelled();
    }
}
