package com.bot.dhxy.task.transaction;

import com.bot.dhxy.window.runtime.WindowRuntimeContext;
import com.bot.dhxy.window.runtime.WindowTaskContextHolder;
import com.bot.dhxy.runner.stop.TaskStopRequestedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

/**
 * Coordinates task-level handoff between windows.
 *
 * <p>The physical input queue serializes individual mouse/keyboard sequences. This coordinator owns
 * the larger task "turn": a window keeps the turn while transactions return CONTINUE_CHAIN, and
 * releases it when a transaction reaches a yield state such as PATHING_STARTED, RETRYABLE_ERROR,
 * TASK_FINISHED, or STOPPED. The lock is fair so queued windows get a predictable chance to run
 * maintenance when the leader releases the turn.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TaskTurnCoordinator {

    private final WindowTaskContextHolder windowTaskContextHolder;
    private final ReentrantLock turnLock = new ReentrantLock(true);
    private final ThreadLocal<Integer> holdDepth = ThreadLocal.withInitial(() -> 0);
    private final ThreadLocal<String> heldWindowId = new ThreadLocal<>();
    private final ThreadLocal<Boolean> optionalTryRunHold = ThreadLocal.withInitial(() -> false);

    /**
     * Acquire the task turn for the current bound window.
     *
     * @param transactionName diagnostic transaction name. Nested calls from the same task thread
     *                        increase hold depth. Calls from the input worker are ignored because the
     *                        task turn must already have been acquired before entering exclusive input.
     * @throws TaskStopRequestedException when the thread is interrupted while waiting.
     */
    public void enter(String transactionName) {
        if (isInputWorkerThread()) {
            return;
        }
        int depth = holdDepth.get();
        if (depth > 0) {
            holdDepth.set(depth + 1);
            return;
        }

        String windowId = currentWindowId();
        log.info("task turn waiting: windowId={} transaction={}", windowId, transactionName);
        try {
            turnLock.lockInterruptibly();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new TaskStopRequestedException("Interrupted while waiting for task turn: " + transactionName);
        }
        holdDepth.set(1);
        heldWindowId.set(windowId);
        log.info("task turn acquired: windowId={} transaction={}", windowId, transactionName);
    }

    /**
     * Leave one task-turn scope and possibly release ownership.
     *
     * @param outcome transaction outcome. Null is treated as an exceptional path and releases all
     * held depth for safety.
     */
    public void leave(TaskTransactionOutcome outcome) {
        if (isInputWorkerThread()) {
            return;
        }
        if (outcome == null || shouldYield(outcome)) {
            releaseAll(outcome == null ? "exception" : outcome.name(), outcome);
            return;
        }

        int depth = holdDepth.get();
        if (depth > 1) {
            holdDepth.set(depth - 1);
        }
        log.info("task turn kept: windowId={} transaction={} result={} yieldPolicy={}",
                heldWindowId.get(), outcome.name(), outcome.result(), outcome.yieldPolicy());
    }

    /**
     * Release all task-turn hold depth for the current thread.
     *
     * @param reason diagnostic reason for logs.
     */
    public void forceRelease(String reason) {
        releaseAll(reason, null);
    }

    /**
     * Run a short optional maintenance action only if the task turn is immediately available.
     *
     * @param transactionName diagnostic transaction name.
     * @param action action to run under the turn. Returning null is treated as false.
     * @return false when another window currently owns the turn or the action returns false.
     */
    public boolean tryRun(String transactionName, Supplier<Boolean> action) {
        if (isInputWorkerThread()) {
            return Boolean.TRUE.equals(action.get());
        }
        int depth = holdDepth.get();
        if (depth > 0) {
            return Boolean.TRUE.equals(action.get());
        }

        String windowId = currentWindowId();
        log.debug("task turn try: windowId={} transaction={}", windowId, transactionName);
        if (!turnLock.tryLock()) {
            log.debug("task turn busy: windowId={} transaction={}", windowId, transactionName);
            return false;
        }

        holdDepth.set(1);
        heldWindowId.set(windowId);
        optionalTryRunHold.set(true);
        log.debug("task turn acquired: windowId={} transaction={}", windowId, transactionName);
        try {
            return Boolean.TRUE.equals(action.get());
        } finally {
            releaseAll(transactionName, null);
        }
    }

    private boolean shouldYield(TaskTransactionOutcome outcome) {
        if (!outcome.completed()) {
            return true;
        }
        if (outcome.result() == TaskTransactionResult.STOPPED
                || outcome.result() == TaskTransactionResult.FAILED
                || outcome.result() == TaskTransactionResult.RETRYABLE_ERROR
                || outcome.result() == TaskTransactionResult.TASK_FINISHED
                || outcome.result() == TaskTransactionResult.PATHING_STARTED
                || outcome.result() == TaskTransactionResult.SHARED_STATE_TRIGGERED) {
            return true;
        }
        return outcome.yieldPolicy() != TaskYieldPolicy.CONTINUE_CHAIN;
    }

    private void releaseAll(String reason, TaskTransactionOutcome outcome) {
        int depth = holdDepth.get();
        if (depth <= 0) {
            return;
        }
        String windowId = heldWindowId.get();
        boolean optionalTryRun = Boolean.TRUE.equals(optionalTryRunHold.get());
        holdDepth.remove();
        heldWindowId.remove();
        optionalTryRunHold.remove();
        turnLock.unlock();
        if (outcome == null) {
            if (optionalTryRun) {
                log.debug("task turn released: windowId={} reason={}", windowId, reason);
            } else {
                log.info("task turn released: windowId={} reason={}", windowId, reason);
            }
        } else {
            log.info("task turn released: windowId={} transaction={} result={} yieldPolicy={}",
                    windowId, outcome.name(), outcome.result(), outcome.yieldPolicy());
        }
    }

    private String currentWindowId() {
        Optional<WindowRuntimeContext> context = windowTaskContextHolder.rawCurrent();
        return context.map(WindowRuntimeContext::getWindowId).orElse("unknown");
    }

    private boolean isInputWorkerThread() {
        return Thread.currentThread().getName().contains("dhxy-input-action-worker");
    }
}
