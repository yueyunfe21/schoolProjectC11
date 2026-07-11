package com.bot.dhxy.task.transaction;

import com.bot.dhxy.input.InputSequences;
import com.bot.dhxy.metrics.AutomationMetricsService;
import com.bot.dhxy.runner.context.TaskExecutionContextHolder;
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
    private final TaskExecutionContextHolder taskExecutionContextHolder;
    private final AutomationMetricsService automationMetricsService;

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
        log.info("task transaction started: name={} expected={} yieldPolicy={} exclusive=false",
                name, expectedResult, yieldPolicy);
        TaskTransactionOutcome outcome = null;
        try {
            TaskTransactionResult result = safeRun(name, action);
            outcome = new TaskTransactionOutcome(name, expectedResult, yieldPolicy, result, true);
            log.info("task transaction finished: name={} expected={} result={} yieldPolicy={} completed=true",
                    name, expectedResult, result, yieldPolicy);
            return outcome;
        } finally {
            long elapsedMs = LatencyMetrics.elapsedMs(latencyStart);
            LatencyMetrics.info(log, "task.transaction", latencyStart,
                    "name=" + name + " result=" + (outcome == null ? "EXCEPTION" : outcome.result())
                            + " completed=" + (outcome != null && outcome.completed())
                            + " exclusive=false");
            automationMetricsService.recordTransaction(taskExecutionContextHolder.current().orElse(null),
                    name, expectedResult, yieldPolicy, outcome, elapsedMs, false);
            taskTurnCoordinator.leave(outcome);
        }
    }

    /**
     * Run a transaction whose callback decides both the transaction result and effective yield policy.
     *
     * <p>This is used by cloud task-policy execute after the local oracle has been computed. Task-turn
     * ownership depends on both result and yield, so the callback must return the final pair before
     * {@link TaskTurnCoordinator#leave(TaskTransactionOutcome)} sees the outcome.</p>
     *
     * @param name diagnostic transaction name.
     * @param expectedResult result that the caller considers successful.
     * @param fallbackYieldPolicy fallback yield policy when the callback returns null or STOP is caught.
     * @param action business action that returns the effective result/yield decision.
     * @return transaction outcome carrying the effective result and effective yield policy.
     */
    public TaskTransactionOutcome runDynamic(String name,
                                             TaskTransactionResult expectedResult,
                                             TaskYieldPolicy fallbackYieldPolicy,
                                             Supplier<TaskTransactionDecision> action) {
        long latencyStart = LatencyMetrics.start();
        TaskYieldPolicy safeFallbackYieldPolicy = fallbackYieldPolicy == null
                ? TaskYieldPolicy.CONTINUE_CHAIN
                : fallbackYieldPolicy;
        taskTurnCoordinator.enter(name);
        log.info("task transaction started: name={} expected={} yieldPolicy={} exclusive=false dynamic=true",
                name, expectedResult, safeFallbackYieldPolicy);
        TaskTransactionOutcome outcome = null;
        try {
            TaskTransactionDecision decision = safeRunDecision(name, action, safeFallbackYieldPolicy);
            outcome = new TaskTransactionOutcome(name, expectedResult, decision.yieldPolicy(),
                    decision.result(), true);
            log.info("task transaction finished: name={} expected={} result={} yieldPolicy={} completed=true dynamic=true",
                    name, expectedResult, decision.result(), decision.yieldPolicy());
            return outcome;
        } finally {
            long elapsedMs = LatencyMetrics.elapsedMs(latencyStart);
            LatencyMetrics.info(log, "task.transaction", latencyStart,
                    "name=" + name + " result=" + (outcome == null ? "EXCEPTION" : outcome.result())
                            + " completed=" + (outcome != null && outcome.completed())
                            + " exclusive=false dynamic=true");
            automationMetricsService.recordTransaction(taskExecutionContextHolder.current().orElse(null),
                    name, expectedResult, outcome == null ? safeFallbackYieldPolicy : outcome.yieldPolicy(),
                    outcome, elapsedMs, false);
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
        log.info("task transaction started: name={} expected={} yieldPolicy={} exclusive=true",
                name, expectedResult, yieldPolicy);
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
            long elapsedMs = LatencyMetrics.elapsedMs(latencyStart);
            LatencyMetrics.info(log, "task.transaction", latencyStart,
                    "name=" + name + " result=" + (outcome == null ? "EXCEPTION" : outcome.result())
                            + " completed=" + (outcome != null && outcome.completed())
                            + " exclusive=true");
            automationMetricsService.recordTransaction(taskExecutionContextHolder.current().orElse(null),
                    name, expectedResult, yieldPolicy, outcome, elapsedMs, true);
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
        } catch (Error e) {
            log.error("task transaction fatal error: name={}", name, e);
            throw e;
        }
    }

    private TaskTransactionDecision safeRunDecision(String name,
                                                    Supplier<TaskTransactionDecision> action,
                                                    TaskYieldPolicy fallbackYieldPolicy) {
        try {
            TaskTransactionDecision decision = action.get();
            return decision == null
                    ? TaskTransactionDecision.of(TaskTransactionResult.FAILED, fallbackYieldPolicy)
                    : decision;
        } catch (TaskStopRequestedException e) {
            log.info("task transaction stopped: name={}", name);
            return TaskTransactionDecision.of(TaskTransactionResult.STOPPED, fallbackYieldPolicy);
        } catch (RuntimeException e) {
            if (Thread.currentThread().isInterrupted()) {
                log.info("task transaction stopped: name={}", name);
                return TaskTransactionDecision.of(TaskTransactionResult.STOPPED, fallbackYieldPolicy);
            }
            log.error("task transaction exception: name={}", name, e);
            throw e;
        } catch (Error e) {
            log.error("task transaction fatal error: name={}", name, e);
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

    public record TaskTransactionDecision(TaskTransactionResult result, TaskYieldPolicy yieldPolicy) {
        public TaskTransactionDecision {
            result = result == null ? TaskTransactionResult.FAILED : result;
            yieldPolicy = yieldPolicy == null ? TaskYieldPolicy.CONTINUE_CHAIN : yieldPolicy;
        }

        public static TaskTransactionDecision of(TaskTransactionResult result, TaskYieldPolicy yieldPolicy) {
            return new TaskTransactionDecision(result, yieldPolicy);
        }
    }
}
