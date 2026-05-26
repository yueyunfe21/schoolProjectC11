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

/**
 * Producer-side queue for serialized physical input requests.
 *
 * <p>Requests are bound to the current {@link WindowRuntimeContext} at submission time. This
 * captures the native hwnd/title before another task thread changes its context. The single
 * {@link InputActionWorker} consumes requests in order and performs best-effort focus plus the actual
 * mouse/keyboard actions.</p>
 */
@Slf4j
@Component
public class InputActionQueue {

    private final BlockingQueue<InputActionRequest> queue = new LinkedBlockingQueue<>();
    private final WindowTaskContextHolder windowTaskContextHolder;

    /**
     * @param windowTaskContextHolder thread-local holder used to capture the submitting window.
     */
    public InputActionQueue(WindowTaskContextHolder windowTaskContextHolder) {
        this.windowTaskContextHolder = windowTaskContextHolder;
    }

    /**
     * Submit a finite list of physical input actions and wait for completion.
     *
     * @param description diagnostic label for logs/dead-letter records.
     * @param actions immutable-ish list of actions; coordinates must already be screen-absolute.
     * @return true when the worker completed the request successfully, false when no bound window or
     * native binding exists, the waiter is interrupted, or the worker reports failure.
     */
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

    /**
     * Submit an exclusive callback to run on the input worker thread.
     *
     * @param description diagnostic label for logs/dead-letter records.
     * @param callback callback that may use direct input provider calls. It must not submit nested
     *                 input queue requests because the worker is already executing it.
     * @return true when the callback completes with true; false on missing binding, interruption, or
     * worker failure.
     */
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
        } catch (InterruptedException e) {
            request.cancel("waiter interrupted");
            Thread.currentThread().interrupt();
            log.warn("Input action wait interrupted: windowId={} description={}",
                    request.getWindowId(), request.getDescription());
            return false;
        } catch (Exception e) {
            request.cancel("wait failed");
            log.warn("Input action wait failed: windowId={} description={} reason={}",
                    request.getWindowId(), request.getDescription(), e.getMessage());
            return false;
        }
    }

    InputActionRequest take() throws InterruptedException {
        return queue.take();
    }

    /**
     * @return current queued request count, primarily for diagnostics.
     */
    public int size() {
        return queue.size();
    }
}
