package com.bot.dhxy.input.action;

import com.bot.dhxy.window.model.WindowNativeBinding;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class InputActionRequest {

    private final String windowId;
    private final WindowNativeBinding nativeBinding;
    private final String description;
    private final List<InputAction> actions;
    private final CompletableFuture<Boolean> result = new CompletableFuture<>();

    public InputActionRequest(String windowId,
                              WindowNativeBinding nativeBinding,
                              String description,
                              List<InputAction> actions) {
        this.windowId = windowId;
        this.nativeBinding = nativeBinding;
        this.description = description == null ? "" : description;
        this.actions = actions == null ? List.of() : List.copyOf(actions);
    }

    public String getWindowId() { return windowId; }

    public WindowNativeBinding getNativeBinding() { return nativeBinding; }

    public String getDescription() { return description; }

    public List<InputAction> getActions() { return actions; }

    public CompletableFuture<Boolean> getResult() { return result; }
}
