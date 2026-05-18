package com.bot.dhxy.input.action;

import com.bot.dhxy.window.model.WindowNativeBinding;
import com.bot.dhxy.window.runtime.WindowRuntimeContext;
import com.bot.dhxy.window.runtime.WindowTaskContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Slf4j
@Component
public class InputActionQueue {

    private final BlockingQueue<InputActionRequest> queue = new LinkedBlockingQueue<>();
    private final WindowTaskContextHolder windowTaskContextHolder;

    public InputActionQueue(WindowTaskContextHolder windowTaskContextHolder) {
        this.windowTaskContextHolder = windowTaskContextHolder;
    }

    public boolean submitAndWait(String description, List<InputAction> actions) {
        Optional<WindowRuntimeContext> current = windowTaskContextHolder.rawCurrent();
        if (current.isEmpty()) {
            log.warn("Input action rejected because no window context exists: {}", description);
            return false;
        }
        WindowRuntimeContext context = current.get();
        if (!hasNativeBinding(context, description)) {
            return false;
        }
        return await(new InputActionRequest(context, description, actions));
    }

    public boolean submitExclusiveAndWait(String description, Supplier<Boolean> callback) {
        Optional<WindowRuntimeContext> current = windowTaskContextHolder.rawCurrent();
        if (current.isEmpty()) {
            log.warn("Input exclusive action rejected because no window context exists: {}", description);
            return false;
        }
        WindowRuntimeContext context = current.get();
        if (!hasNativeBinding(context, description)) {
            return false;
        }
        return await(new InputActionRequest(context, description, callback));
    }

    private boolean hasNativeBinding(WindowRuntimeContext context, String description) {
        WindowNativeBinding binding = context.getNativeBinding();
        if (binding == null || !binding.hasNativeHandle()) {
            log.warn("Input action rejected because no native binding exists: windowId={} description={}",
                    context.getWindowId(), description);
            return false;
        }
        return true;
    }

    private boolean await(InputActionRequest request) {
        queue.offer(request);
        try {
            return request.getResult().get(120, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("Input action timed out or interrupted: windowId={} description={} reason={}",
                    request.getWindowId(), request.getDescription(), e.getMessage());
            return false;
        }
    }

    InputActionRequest take() throws InterruptedException {
        return queue.take();
    }

    public int size() {
        return queue.size();
    }
}
