package com.bot.dhxy.input.action;

import com.bot.dhxy.window.model.WindowNativeBinding;
import com.bot.dhxy.window.runtime.WindowRuntimeContext;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class InputActionRequest {

    private final WindowRuntimeContext windowContext;
    private final String windowId;
    private final WindowNativeBinding nativeBinding;
    private final String description;
    private final List<InputAction> actions;
    private final CompletableFuture<Boolean> result = new CompletableFuture<>();

    public InputActionRequest(WindowRuntimeContext windowContext,
                              String description,
                              List<InputAction> actions) {
        this.windowContext = windowContext;
        this.windowId = windowContext == null ? null : windowContext.getWindowId();
        this.nativeBinding = windowContext == null ? null : windowContext.getNativeBinding();
        this.description = description == null ? "" : description;
        this.actions = actions == null ? List.of() : List.copyOf(actions);
    }

    public WindowRuntimeContext getWindowContext() { return windowContext; }

    public String getWindowId() { return windowId; }

    public WindowNativeBinding getNativeBinding() { return nativeBinding; }

    public String getDescription() { return description; }

    public List<InputAction> getActions() { return actions; }

    public CompletableFuture<Boolean> getResult() { return result; }
}
