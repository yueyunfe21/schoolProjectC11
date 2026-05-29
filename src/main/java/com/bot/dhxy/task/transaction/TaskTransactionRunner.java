package com.bot.dhxy.task.transaction;

import com.bot.dhxy.input.InputSequences;
import com.bot.dhxy.runner.stop.TaskStopRequestedException;
import com.bot.dhxy.tools.LatencyMetrics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * Runs task-level transactions under task-turn ownership.
 *
 * <p>Use this wrapper around multi-step task operations that must not be interleaved with other
 * windows, such as accepting a task, reading a story objective, or clicking a business dialog.
 * {@link #runExclusive(String, TaskTransactionResult, TaskYieldPolicy, Supplier)} additionally owns
 * the physical input queue for the duration of the callback. Do not call queued input APIs inside an
 * exclusive callback; use direct input providers there.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TaskTransactionRunner {

    private final InputSequences inputSequences;
    private final TaskTurnCoordinator taskTurnCoordinator;

    /**
     * Run a transaction while holding the task turn.
     *
     * @param name diagnostic transaction name.
     * @param expectedResult result that the caller considers successful.
     * @param yieldPolicy caller's requested handoff behavior after completion.
     * @param action business action to run on the current task thread. It should honor stop tokens and
     *               return a non-null {@link TaskTransactionResult}; null is treated as FAILED.
     * @return transaction outcome. The task turn is released or retained according to
     * {@link TaskTurnCoordinator#leave(TaskTransactionOutcome)}.
     */
    public TaskTransactionOutcome run(String name,
                                      TaskTransactionResult expectedResult,
                                      TaskYieldPolicy yieldPolicy,
                                      Supplier<TaskTransactionResult> action) {
        long latencyStart = LatencyMetrics.start();
        taskTurnCoordinator.enter(name);
        TaskTransactionOutcome outcome = null;
        try {
            TaskTransactionResult result = safeRun(name, action);
            outcome = new TaskTransactionOutcome(name, expectedResult, yieldPolicy, result, true);
            log.info("task transaction finished: name={} expected={} result={} yieldPolicy={} completed=true",
                    name, expectedResult, result, yieldPolicy);
            return outcome;
        } finally {
            LatencyMetrics.info(log, "task.transaction", latencyStart,
                    "name=" + name + " result=" + (outcome == null ? "EXCEPTION" : outcome.result())
                            + " completed=" + (outcome != null && outcome.completed())
                            + " exclusive=false");
            taskTurnCoordinator.leave(outcome);
        }
    }

    /**
     * Run a transaction while holding both the task turn and the serialized input worker.
     *
     * @param name diagnostic transaction name and input-queue description.
     * @param expectedResult result that the caller considers successful.
     * @param yieldPolicy caller's requested handoff behavior after completion.
     * @param action business action to run inside exclusive input ownership. If already on the input
     *               worker thread, it runs directly to avoid queue-in-queue deadlock.
     * @return transaction outcome. completed=false means the exclusive input request failed or was
     * interrupted before the callback could finish.
     */
    public TaskTransactionOutcome runExclusive(String name,
                                               TaskTransactionResult expectedResult,
                                               TaskYieldPolicy yieldPolicy,
                                               Supplier<TaskTransactionResult> action) {
        long latencyStart = LatencyMetrics.start();
        taskTurnCoordinator.enter(name);
        TaskTransactionOutcome outcome = null;
        try {
            AtomicReference<TaskTransactionResult> result = new AtomicReference<>(TaskTransactionResult.FAILED);
            boolean completed;
            if (isInputWorkerThread()) {
                result.set(safeRun(name, action));
                completed = true;
            } else {
                completed = inputSequences.submitExclusiveAndWait(name, () -> {
                    result.set(safeRun(name, action));
                    return true;
                });
            }

            TaskTransactionResult finalResult = completed ? result.get() : interruptedResult();
            outcome = new TaskTransactionOutcome(name, expectedResult, yieldPolicy, finalResult, completed);
            log.info("task transaction finished: name={} expected={} result={} yieldPolicy={} completed={}",
                    name, expectedResult, finalResult, yieldPolicy, completed);
            return outcome;
        } finally {
            LatencyMetrics.info(log, "task.transaction", latencyStart,
                    "name=" + name + " result=" + (outcome == null ? "EXCEPTION" : outcome.result())
                            + " completed=" + (outcome != null && outcome.completed())
                            + " exclusive=true");
            taskTurnCoordinator.leave(outcome);
        }
    }

    /**
     * Force-release the current task turn for cleanup/error exits.
     *
     * @param reason diagnostic reason written to logs.
     */
    public void forceReleaseTurn(String reason) {
        taskTurnCoordinator.forceRelease(reason);
    }

    private TaskTransactionResult safeRun(String name, Supplier<TaskTransactionResult> action) {
        try {
            TaskTransactionResult result = action.get();
            return result == null ? TaskTransactionResult.FAILED : result;
        } catch (TaskStopRequestedException e) {
            log.info("task transaction stopped: name={}", name);
            return TaskTransactionResult.STOPPED;
        } catch (RuntimeException e) {
            if (Thread.currentThread().isInterrupted()) {
                log.info("task transaction stopped: name={}", name);
                return TaskTransactionResult.STOPPED;
            }
            log.error("task transaction exception: name={}", name, e);
            throw e;
        }
    }

    private TaskTransactionResult interruptedResult() {
        return Thread.currentThread().isInterrupted()
                ? TaskTransactionResult.STOPPED
                : TaskTransactionResult.FAILED;
    }

    private boolean isInputWorkerThread() {
        return Thread.currentThread().getName().contains("dhxy-input-action-worker");
    }
}
