package com.bot.dhxy.input.action;

import com.bot.dhxy.window.model.WindowNativeBinding;
import com.bot.dhxy.window.runtime.WindowRuntimeContext;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

public class InputActionRequest {

    private final WindowRuntimeContext windowContext;
    private final String windowId;
    private final WindowNativeBinding nativeBinding;
    private final String description;
    private final List<InputAction> actions;
    private final Supplier<Boolean> exclusiveCallback;
    private final CompletableFuture<Boolean> result = new CompletableFuture<>();
    private final AtomicBoolean cancelled = new AtomicBoolean(false);

    public InputActionRequest(WindowRuntimeContext windowContext,
                              String description,
                              List<InputAction> actions) {
        this(windowContext, description, actions, null);
    }

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

    public WindowRuntimeContext getWindowContext() { return windowContext; }

    public String getWindowId() { return windowId; }

    public WindowNativeBinding getNativeBinding() { return nativeBinding; }

    public String getDescription() { return description; }

    public List<InputAction> getActions() { return actions; }

    public Supplier<Boolean> getExclusiveCallback() { return exclusiveCallback; }

    public boolean hasExclusiveCallback() { return exclusiveCallback != null; }

    public CompletableFuture<Boolean> getResult() { return result; }

    public void cancel(String reason) {
        cancelled.set(true);
        result.complete(false);
    }

    public boolean isCancelled() {
        return cancelled.get() || result.isCancelled();
    }
}
