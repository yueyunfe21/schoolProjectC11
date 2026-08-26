package com.bot.dhxy.input;

import com.bot.dhxy.input.action.InputAction;
import com.bot.dhxy.input.action.InputActionExecutionResult;
import com.bot.dhxy.input.action.InputActionQueue;
import com.bot.dhxy.runner.stop.TaskStopToken;
import com.bot.dhxy.window.model.WindowNativeBinding;
import com.bot.dhxy.window.runtime.WindowRuntimeContext;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.function.Supplier;

/**
 * Convenience API for submitting atomic physical input sequences.
 *
 * <p>Every method captures the current bound window and waits for the single input worker to finish.
 * Coordinates are screen-absolute pixels. Use {@link #submitAndWait(String, List)} when move/click or
 * key/click steps must stay together; splitting them into separate requests allows another window to
 * interleave focus or mouse movement.</p>
 */
@Component
public class InputSequences {

    private final InputActionQueue inputActionQueue;

    /**
     * @param inputActionQueue serialized queue backing all physical input.
     */
    public InputSequences(InputActionQueue inputActionQueue) {
        this.inputActionQueue = inputActionQueue;
    }

    /**
     * Submit an ordered physical input sequence.
     *
     * @param description diagnostic label for logs.
     * @param actions ordered actions with screen-absolute coordinates where applicable.
     * @return true when the worker completes the whole sequence successfully.
     */
    public boolean submitAndWait(String description, List<InputAction> actions) {
        return inputActionQueue.submitAndWait(description, actions);
    }

    /**
     * Submits an atomic input sequence while leaving the observation caller free to keep sampling.
     *
     * @param description diagnostic label for logs
     * @param actions ordered actions with screen-absolute coordinates where applicable
     * @return worker completion; the caller must react through the stage and must not wait on it
     */
    public CompletionStage<Boolean> submitAsync(String description, List<InputAction> actions) {
        return inputActionQueue.submitAsync(description, actions);
    }

    /**
     * Run a callback inside exclusive input-worker ownership.
     *
     * @param description diagnostic label for logs.
     * @param callback direct-input callback; it must not submit nested input queue requests.
     * @return true when the callback returns true and the worker completes it.
     */
    public boolean submitExclusiveAndWait(String description, Supplier<Boolean> callback) {
        return inputActionQueue.submitExclusiveAndWait(description, callback);
    }

    /** Cancel queued, not-yet-started input for one terminal task run. */
    public int cancelQueuedRequests(TaskStopToken stopToken, String reason) {
        return inputActionQueue.cancelQueuedRequests(stopToken, reason);
    }

    /** Run an exact-HWND capture callback without focusing unless the callback explicitly reaches mouse input. */
    public boolean submitBackgroundExclusiveAndWait(String description, Supplier<Boolean> callback) {
        return inputActionQueue.submitBackgroundExclusiveAndWait(description, callback);
    }

    /**
     * Run a direct-input callback against one immutable action-resolver window snapshot.
     *
     * <p>No caller-supplied identity epoch is accepted: the queue freezes the {@code (binding, epoch)}
     * generation itself under the context monitor. The worker's typed terminal result is returned
     * verbatim so callers can project a real {@code STOP_REQUESTED} instead of guessing from a boolean.</p>
     *
     * @param description diagnostic label for logs
     * @param context exact resolved window context
     * @param binding exact HWND/process/screen-rectangle snapshot; must still be the context's current
     *                generation object
     * @param callback worker callback; it must not submit nested input requests
     * @return the worker's typed terminal result: status, safety reason and detail, never flattened
     */
    public InputActionExecutionResult submitFrozenExactWindowExclusiveAndWait(
            String description,
            WindowRuntimeContext context,
            WindowNativeBinding binding,
            Supplier<Boolean> callback) {
        return inputActionQueue.submitFrozenExactWindowExclusiveAndWait(
                description, context, binding, callback);
    }

    public InputActionExecutionResult submitFrozenExactWindowExclusiveAndWait(
            String description,
            WindowRuntimeContext context,
            WindowNativeBinding binding,
            Supplier<Boolean> callback,
            java.util.function.Supplier<com.bot.dhxy.input.action.InputActionSafetyReason>
                    externalSafetyReason) {
        return inputActionQueue.submitFrozenExactWindowExclusiveAndWait(
                description, context, binding, callback, externalSafetyReason);
    }

    /**
     * Run an HWND-background callback against one immutable exact-window generation.
     *
     * <p>The global worker still owns serialization and preserves binding, epoch, stop and pause
     * safety, but it does not foreground the window. Callers must not perform real mouse input in
     * this callback.</p>
     *
     * @param description diagnostic label for logs
     * @param context exact resolved window context
     * @param binding immutable current binding object used as the generation witness
     * @param callback background keyboard/capture callback; must not submit nested input requests
     * @return typed worker terminal result
     */
    public InputActionExecutionResult submitFrozenExactWindowBackgroundExclusiveAndWait(
            String description,
            WindowRuntimeContext context,
            WindowNativeBinding binding,
            Supplier<Boolean> callback) {
        return inputActionQueue.submitFrozenExactWindowBackgroundExclusiveAndWait(
                description, context, binding, callback);
    }

    /**
     * Move to a known screen point and left-click it in one queued request.
     *
     * @param description diagnostic label.
     * @param x screen-absolute X pixel.
     * @param y screen-absolute Y pixel.
     * @param settleMs delay after moving before clicking, in milliseconds.
     * @param delayMs post-click delay in milliseconds.
     * @return true when the move and click complete without interruption.
     */
    public boolean moveAndClickLeft(String description, int x, int y, int settleMs, int delayMs) {
        return submitAndWait(description, List.of(
                InputAction.moveMouse(x, y),
                InputAction.sleep(settleMs),
                InputAction.clickLeft(x, y, delayMs)
        ));
    }

    /**
     * Enqueues one atomic move/click transaction without blocking the observation thread.
     *
     * @param description diagnostic label
     * @param x screen-absolute X pixel
     * @param y screen-absolute Y pixel
     * @param settleMs delay after moving before clicking, in milliseconds
     * @param delayMs post-click delay in milliseconds
     * @return completion stage owned by the global input worker
     */
    public CompletionStage<Boolean> moveAndClickLeftAsync(
            String description, int x, int y, int settleMs, int delayMs) {
        return submitAsync(description, List.of(
                InputAction.moveMouse(x, y),
                InputAction.sleep(settleMs),
                InputAction.clickLeft(x, y, delayMs)
        ));
    }
}
