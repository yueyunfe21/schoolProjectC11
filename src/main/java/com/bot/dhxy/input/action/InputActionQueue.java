package com.bot.dhxy.input.action;

import com.bot.dhxy.runner.context.TaskExecutionContext;
import com.bot.dhxy.runner.context.TaskExecutionContextHolder;
import com.bot.dhxy.runner.stop.TaskPauseToken;
import com.bot.dhxy.runner.stop.TaskStopToken;
import com.bot.dhxy.window.model.WindowNativeBinding;
import com.bot.dhxy.window.runtime.WindowNativeBindingRefreshService;
import com.bot.dhxy.window.runtime.WindowRuntimeContext;
import com.bot.dhxy.window.runtime.WindowTaskContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeoutException;
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
    private final WindowNativeBindingRefreshService bindingRefreshService;
    private final TaskExecutionContextHolder taskExecutionContextHolder;

    /**
     * @param windowTaskContextHolder thread-local holder used to capture the submitting window.
     * @param bindingRefreshService live HWND/title/geometry refresher used before input is queued.
     * @param taskExecutionContextHolder thread-local holder used to capture the submitting pause token.
     */
    public InputActionQueue(WindowTaskContextHolder windowTaskContextHolder,
                            WindowNativeBindingRefreshService bindingRefreshService,
                            TaskExecutionContextHolder taskExecutionContextHolder) {
        this.windowTaskContextHolder = windowTaskContextHolder;
        this.bindingRefreshService = bindingRefreshService;
        this.taskExecutionContextHolder = taskExecutionContextHolder;
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
        if (!refreshAndValidateNativeBinding(context, description)) {
            return false;
        }
        CapturedTaskTokens taskTokens = captureTaskTokens();
        return await(new InputActionRequest(context, description, actions,
                taskTokens.pauseToken(), taskTokens.stopToken()));
    }

    /**
     * Submit a finite list of physical input actions without blocking the task thread.
     *
     * <p>The request still captures the current window binding and task pause/stop tokens exactly
     * like {@link #submitAndWait(String, List)}. Use this only for cheap cleanup that must stay
     * window-bound but should not hold the foreground task turn, such as closing a mini-map after a
     * fire-and-handoff navigation click.</p>
     *
     * @param description diagnostic label for logs/dead-letter records.
     * @param actions ordered actions with screen-absolute coordinates where applicable.
     * @return true when the request was accepted into the input queue.
     */
    public boolean submit(String description, List<InputAction> actions) {
        Optional<WindowRuntimeContext> current = windowTaskContextHolder.rawCurrent();
        if (current.isEmpty()) {
            log.warn("Input action rejected because no window context exists: {}", description);
            return false;
        }
        WindowRuntimeContext context = current.get();
        if (!refreshAndValidateNativeBinding(context, description)) {
            return false;
        }
        CapturedTaskTokens taskTokens = captureTaskTokens();
        queue.offer(new InputActionRequest(context, description, actions,
                taskTokens.pauseToken(), taskTokens.stopToken()));
        return true;
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
        if (!refreshAndValidateNativeBinding(context, description)) {
            return false;
        }
        CapturedTaskTokens taskTokens = captureTaskTokens();
        return await(new InputActionRequest(context, description, callback,
                taskTokens.pauseToken(), taskTokens.stopToken()));
    }

    private CapturedTaskTokens captureTaskTokens() {
        return taskExecutionContextHolder.current()
                .map(context -> new CapturedTaskTokens(context.getPauseToken(), context.getStopToken()))
                .orElseGet(() -> new CapturedTaskTokens(null, null));
    }

    private boolean refreshAndValidateNativeBinding(WindowRuntimeContext context, String description) {
        WindowNativeBinding binding = context.getNativeBinding();
        if (binding == null || !binding.hasNativeHandle()) {
            log.warn("Input action rejected because no native binding exists: windowId={} description={}",
                    context.getWindowId(), description);
            return false;
        }
        /*
         * Pure input paths can be the first action after the same HWND is logged into another player.
         * Refresh before enqueueing so the worker carries the live title/player epoch, not a stale
         * task-start binding that only later screenshot/tracker code would update.
         */
        Optional<WindowNativeBinding> refreshedBinding = bindingRefreshService.refreshAndCommit(context);
        if (refreshedBinding.isEmpty()) {
            log.warn("Input action rejected because live binding refresh is unavailable: windowId={} description={}",
                    context.getWindowId(), description);
            return false;
        }
        WindowNativeBinding refreshed = context.getNativeBinding();
        if (refreshed == null || !refreshed.hasNativeHandle()) {
            log.warn("Input action rejected after live binding refresh: windowId={} description={}",
                    context.getWindowId(), description);
            return false;
        }
        context.waitIfIdentitySuspended(null);
        return true;
    }

    private boolean await(InputActionRequest request) {
        queue.offer(request);
        long remainingWaitMs = TimeUnit.SECONDS.toMillis(120);
        long lastCheckMs = System.currentTimeMillis();
        try {
            while (true) {
                long pollMs = Math.max(1L, Math.min(1000L, remainingWaitMs));
                try {
                    return request.getResult().get(pollMs, TimeUnit.MILLISECONDS);
                } catch (TimeoutException e) {
                    long now = System.currentTimeMillis();
                    if (request.isPauseRequested()) {
                        lastCheckMs = now;
                        continue;
                    }
                    remainingWaitMs -= Math.max(1L, now - lastCheckMs);
                    lastCheckMs = now;
                    if (remainingWaitMs <= 0L) {
                        request.cancel("wait timed out");
                        boolean removed = queue.remove(request);
                        log.warn("Input action wait timed out: windowId={} description={} removedFromQueue={}",
                                request.getWindowId(), request.getDescription(), removed);
                        return false;
                    }
                }
            }
        } catch (InterruptedException e) {
            request.cancel("waiter interrupted");
            boolean removed = queue.remove(request);
            Thread.currentThread().interrupt();
            log.warn("Input action wait interrupted: windowId={} description={} removedFromQueue={}",
                    request.getWindowId(), request.getDescription(), removed);
            return false;
        } catch (Exception e) {
            request.cancel("wait failed");
            boolean removed = queue.remove(request);
            log.warn("Input action wait failed: windowId={} description={} removedFromQueue={} reason={}",
                    request.getWindowId(), request.getDescription(), removed, e.getMessage());
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

    private record CapturedTaskTokens(TaskPauseToken pauseToken, TaskStopToken stopToken) {
    }
}
