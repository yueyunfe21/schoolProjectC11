package com.bot.dhxy.input.action;

import com.bot.dhxy.window.model.WindowNativeBinding;
import com.bot.dhxy.window.runtime.WindowRuntimeContext;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
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
    private final String description;
    private final List<InputAction> actions;
    private final Supplier<Boolean> exclusiveCallback;
    private final CompletableFuture<Boolean> result = new CompletableFuture<>();
    private final AtomicBoolean cancelled = new AtomicBoolean(false);

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
        this(windowContext, description, actions, null);
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
        this(windowContext, description, List.of(), exclusiveCallback);
    }

    private InputActionRequest(WindowRuntimeContext windowContext,
                               String description,
                               List<InputAction> actions,
                               Supplier<Boolean> exclusiveCallback) {
        this.windowContext = windowContext;
        this.windowId = windowContext == null ? null : windowContext.getWindowId();
        this.nativeBinding = windowContext == null ? null : windowContext.getNativeBinding();
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

    /**
     * Mark the request cancelled and unblock the submitter.
     *
     * @param reason diagnostic reason; currently stored only in logs by the caller.
     */
    public void cancel(String reason) {
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
