package com.bot.dhxy.input.action;

import com.bot.dhxy.cloud.decision.CloudDecisionProperties;
import com.bot.dhxy.cloud.task.RouteCloudDecisionService;
import com.bot.dhxy.cloud.turn.TurnExecutionWindow;
import com.bot.dhxy.cloud.turn.protocol.TurnAction;
import com.bot.dhxy.config.WindowIsolationProperties;
import com.bot.dhxy.core.GameContext;
import com.bot.dhxy.driver.BoundWindowKeyboardService;
import com.bot.dhxy.input.GlobalInputLock;
import com.bot.dhxy.input.InputProvider;
import com.bot.dhxy.input.InputSequences;
import com.bot.dhxy.input.WindowAwareInputCoordinator;
import com.bot.dhxy.metrics.AutomationMetricsService;
import com.bot.dhxy.runner.context.TaskExecutionContext;
import com.bot.dhxy.runner.context.TaskExecutionContextHolder;
import com.bot.dhxy.runner.stop.TaskPauseToken;
import com.bot.dhxy.runner.stop.TaskStopToken;
import com.bot.dhxy.service.AutoCombatService;
import com.bot.dhxy.service.DialogService;
import com.bot.dhxy.service.MapNameCanonicalizer;
import com.bot.dhxy.service.MemoryService;
import com.bot.dhxy.service.TaskMaintenanceService;
import com.bot.dhxy.service.TaskTrackerPanelService;
import com.bot.dhxy.service.UICleanerService;
import com.bot.dhxy.task.startup.TaskTeamAssignmentPolicy;
import com.bot.dhxy.team.TeamRoleDetectionService;
import com.bot.dhxy.vision.MiniMapCoordinateReader;
import com.bot.dhxy.window.diagnostics.WindowInteractionMetricsService;
import com.bot.dhxy.window.execution.MultiWindowTaskManager;
import com.bot.dhxy.window.execution.WindowTaskRunner;
import com.bot.dhxy.window.interaction.WindowFocusService;
import com.bot.dhxy.window.model.WindowNativeBinding;
import com.bot.dhxy.window.runtime.WindowNativeBindingRefreshService;
import com.bot.dhxy.window.runtime.WindowReadyEventBus;
import com.bot.dhxy.window.runtime.WindowRuntimeContext;
import com.bot.dhxy.window.runtime.WindowTaskContextHolder;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InputActionFrozenExclusiveContractTest {

    @Test
    void resolvedSnapshotRunsOnTheRealWorkerWithoutASecondRefresh() {
        Harness harness = new Harness();
        TurnExecutionWindow resolved = harness.resolve("window-exact", "12345", 101L);
        WindowRuntimeContext context = resolved.context();
        WindowNativeBinding binding = resolved.binding();
        AtomicInteger callbacks = new AtomicInteger();

        InputActionExecutionResult result = harness.queue.submitFrozenExactWindowExclusiveAndWait(
                "frozen-no-refresh",
                context,
                binding,
                () -> {
                    callbacks.incrementAndGet();
                    return true;
                });

        assertTrue(result.isCompleted());
        assertEquals(InputActionSafetyReason.CLEAR, result.getSafetyReason());
        assertEquals(1, callbacks.get());
        assertEquals(1, harness.focusedBindings.size());
        assertSame(binding, harness.focusedBindings.get(0),
                "focus must receive the resolver's frozen binding object itself");
        assertEquals(1, harness.refresh.calls.get(),
                "the action resolver owns the one and only refresh; the frozen path adds no second one");
    }

    @Test
    void queuedCancellationRemovesTheRequestAndRunsZeroCallbackMechanics() throws Exception {
        Harness harness = new Harness();
        BlockingRequest blocker = harness.blockWorker();
        WindowRuntimeContext context = harness.context("window-queued", "22345", 102L);
        WindowNativeBinding binding = context.getNativeBinding();
        AtomicInteger callbacks = new AtomicInteger();
        AtomicReference<Boolean> completed = new AtomicReference<>();
        Thread waiter = new Thread(() -> completed.set(
                harness.queue.submitFrozenExactWindowExclusiveAndWait(
                        "frozen-queued-cancel",
                        context,
                        binding,
                        () -> {
                            callbacks.incrementAndGet();
                            return true;
                        }).isCompleted()), "frozen-queued-waiter");
        waiter.start();

        /*
         * No wait is needed before interrupting, and none may be used: production's own interrupt handler
         * removes the request from the queue and cancels it (InputActionQueue.await catch InterruptedException),
         * and the interrupt flag is honoured wherever the waiter happens to be — before the offer, or already
         * parked in the poll. Every interleaving therefore ends in the same terminal, so the assertions below
         * are facts about the contract rather than about when this thread happened to fire.
         */
        waiter.interrupt();
        waiter.join(TimeUnit.SECONDS.toMillis(2));

        assertFalse(waiter.isAlive());
        assertEquals(Boolean.FALSE, completed.get());
        assertEquals(0, callbacks.get());
        blocker.release.countDown();
        blocker.waiter.join(TimeUnit.SECONDS.toMillis(2));
    }

    @Test
    void startedCancellationWaitsForWorkerCtrlReleaseAndSettleBarrier() throws Exception {
        Harness harness = new Harness();
        WindowRuntimeContext context = harness.context("window-started", "32345", 103L);
        WindowNativeBinding binding = context.getNativeBinding();
        List<String> events = new CopyOnWriteArrayList<>();
        CountDownLatch ctrlDown = new CountDownLatch(1);
        CountDownLatch ctrlUp = new CountDownLatch(1);
        CountDownLatch allowSettleCompletion = new CountDownLatch(1);
        /*
         * Proving "zero mechanics after the closed result returns" by sleeping only proves that nothing
         * happened within the nap. Instead the callback publishes its own last-mechanic watermark and
         * counts down when its finally is done; the waiter may only return after that, so comparing the
         * watermark to the event log after the join is an ordering fact, not a timing guess.
         */
        CountDownLatch mechanicsFinished = new CountDownLatch(1);
        AtomicInteger lastMechanicAt = new AtomicInteger(-1);
        AtomicReference<Boolean> completed = new AtomicReference<>();
        Thread waiter = new Thread(() -> completed.set(
                harness.queue.submitFrozenExactWindowExclusiveAndWait(
                        "frozen-started-cancel",
                        context,
                        binding,
                        () -> {
                            events.add("CTRL_DOWN");
                            ctrlDown.countDown();
                            try {
                                while (InputActionScope.checkpoint()) {
                                    Thread.onSpinWait();
                                }
                                return false;
                            } finally {
                                events.add("CTRL_UP");
                                ctrlUp.countDown();
                                await(allowSettleCompletion);
                                events.add("CTRL_UP_SETTLED");
                                lastMechanicAt.set(events.size());
                                mechanicsFinished.countDown();
                            }
                        }).isCompleted()), "frozen-started-waiter");
        waiter.start();
        assertTrue(ctrlDown.await(2, TimeUnit.SECONDS));

        waiter.interrupt();
        assertTrue(ctrlUp.await(2, TimeUnit.SECONDS));
        waiter.join(50L);
        assertTrue(waiter.isAlive(),
                "the waiter must remain behind the worker-owned Ctrl UP settle barrier");

        allowSettleCompletion.countDown();
        waiter.join(TimeUnit.SECONDS.toMillis(2));

        assertFalse(waiter.isAlive());
        assertEquals(Boolean.FALSE, completed.get());
        assertEquals(List.of("CTRL_DOWN", "CTRL_UP", "CTRL_UP_SETTLED"), events);
        assertTrue(mechanicsFinished.await(2, TimeUnit.SECONDS),
                "the callback's finally must have completed before the waiter returned");
        assertEquals(events.size(), lastMechanicAt.get(),
                "the last mechanic the worker ran must be the last event in the log: the closed waiter "
                        + "result may not return while any mechanic is still outstanding");
    }

    /**
     * The drift is published from inside the production {@code take()}, on the worker thread, after the request
     * has been handed over and before the frozen boundary inspects it. That is an ordering fact: no wait, no
     * poll and no second thread decide it. Waiting for the request to appear in the queue and mutating from the
     * test thread would have proven only that the test guessed the timing right.
     */
    @Test
    void bindingDriftAfterTakeRunsZeroCallbackAndAddsNoRefresh() {
        Harness harness = new Harness();
        TurnExecutionWindow resolved = harness.resolve("window-drift", "42345", 104L);
        WindowRuntimeContext context = resolved.context();
        WindowNativeBinding binding = resolved.binding();
        AtomicInteger callbacks = new AtomicInteger();
        harness.queue.onTake = takenRequest -> {
            if ("frozen-binding-drift".equals(takenRequest.getDescription())) {
                context.setNativeBinding(binding.withGeometry(
                        binding.getX() + 1,
                        binding.getY(),
                        binding.getWidth(),
                        binding.getHeight()));
            }
        };

        InputActionExecutionResult result = harness.queue.submitFrozenExactWindowExclusiveAndWait(
                "frozen-binding-drift",
                context,
                binding,
                () -> {
                    callbacks.incrementAndGet();
                    return true;
                });

        assertFalse(result.isCompleted());
        assertEquals(InputActionSafetyReason.WINDOW_BINDING_CHANGED, result.getSafetyReason());
        assertEquals(1, harness.queue.taken.size(), "the request really reached the worker");
        assertEquals(0, callbacks.get(), "binding drift must reject before callback/input");
        assertEquals(0, harness.inputCalls.get(), "binding drift must send zero physical input");
        assertEquals(0, harness.focusCalls.get(), "the drifted request must fail before focus");
        assertEquals(1, harness.refresh.calls.get(),
                "drift rejection adds no refresh of its own: the resolver's single refresh is still the only one");
    }

    /**
     * P2-1 seam: a synchronous fake that invokes the callback in the caller's thread lets a non-Runtime
     * throwable fly straight back to the caller, so it never proves anything about
     * {@link InputActionWorker}'s outer {@code catch (Throwable)}. Only the real worker normalizes such a
     * throwable into a closed terminal result instead of leaking it or leaving the waiter hanging.
     */
    @Test
    void nonRuntimeThrowableEscapingTheCallbackIsNormalizedByTheRealWorkerIntoAClosedResult() {
        Harness harness = new Harness();
        TurnExecutionWindow resolved = harness.resolve("window-throwable", "62345", 106L);
        AtomicInteger callbacks = new AtomicInteger();

        InputActionExecutionResult result = harness.queue.submitFrozenExactWindowExclusiveAndWait(
                "frozen-non-runtime-throwable",
                resolved.context(),
                resolved.binding(),
                () -> {
                    callbacks.incrementAndGet();
                    throw new AssertionError("callback-non-runtime-throwable");
                });

        assertEquals(1, callbacks.get(), "the callback must have run exactly once on the real worker");
        assertFalse(result.isCompleted(),
                "a throwable escaping the callback must never be reported as a completed request");
        assertTrue(result.getReason() != null && result.getReason().contains("AssertionError"),
                "the worker must normalize the non-Runtime throwable into the closed result reason, "
                        + "instead of leaking it to the caller or hiding it behind a bare false");
        assertEquals(1, harness.refresh.calls.get(),
                "normalizing the throwable must not re-resolve or re-refresh the window");
    }

    /**
     * P1-1 seam: one geometry-x drift only proves that <em>a</em> comparison exists. Each exact-window
     * field must independently reject, otherwise a single unchecked field is a silent hole through which a
     * stale snapshot reaches real input. Every case runs the public frozen boundary against the real queue
     * and worker.
     */
    @Test
    void everyExactWindowFieldDriftIndependentlyRejectsBeforeCallbackOrInput() throws Exception {
        record FieldDrift(String name, UnaryOperator<WindowNativeBinding> mutate) {
        }
        List<FieldDrift> drifts = List.of(
                new FieldDrift("nativeHandle", b -> new WindowNativeBinding(
                        b.getNativeHandle() + "9", b.getTitle(), b.getClassName(), b.getProcessId(),
                        b.getX(), b.getY(), b.getWidth(), b.getHeight())),
                new FieldDrift("processId", b -> new WindowNativeBinding(
                        b.getNativeHandle(), b.getTitle(), b.getClassName(), b.getProcessId() + 1,
                        b.getX(), b.getY(), b.getWidth(), b.getHeight())),
                new FieldDrift("x", b -> b.withGeometry(b.getX() + 1, b.getY(), b.getWidth(), b.getHeight())),
                new FieldDrift("y", b -> b.withGeometry(b.getX(), b.getY() + 1, b.getWidth(), b.getHeight())),
                new FieldDrift("width", b -> b.withGeometry(b.getX(), b.getY(), b.getWidth() + 1, b.getHeight())),
                new FieldDrift("height", b -> b.withGeometry(b.getX(), b.getY(), b.getWidth(), b.getHeight() + 1)));

        for (FieldDrift drift : drifts) {
            Harness harness = new Harness();
            TurnExecutionWindow resolved = harness.resolve("window-" + drift.name(), "52345", 105L);
            WindowRuntimeContext context = resolved.context();
            WindowNativeBinding binding = resolved.binding();
            AtomicInteger callbacks = new AtomicInteger();
            // Each field drifts on the worker thread, between take and the frozen boundary: ordering, not timing.
            harness.queue.onTake = takenRequest -> {
                if (("frozen-drift-" + drift.name()).equals(takenRequest.getDescription())) {
                    context.setNativeBinding(drift.mutate().apply(binding));
                }
            };

            InputActionExecutionResult result = harness.queue.submitFrozenExactWindowExclusiveAndWait(
                    "frozen-drift-" + drift.name(),
                    context,
                    binding,
                    () -> {
                        callbacks.incrementAndGet();
                        return true;
                    });

            assertFalse(result.isCompleted(), drift.name() + " drift must never be admitted");
            assertEquals(InputActionSafetyReason.WINDOW_BINDING_CHANGED, result.getSafetyReason(),
                    drift.name() + " drift must be typed as a window change");
            assertEquals(1, harness.queue.taken.size(), drift.name() + ": the request must reach the worker");
            assertEquals(0, callbacks.get(), drift.name() + " drift must reject before the callback");
            assertEquals(0, harness.inputCalls.get(), drift.name() + " drift must send zero physical input");
            assertEquals(0, harness.focusCalls.get(), drift.name() + " drift must fail before focus");
            assertEquals(1, harness.refresh.calls.get(),
                    drift.name() + ": the resolver's single refresh stays the only one; rejection adds none");
        }
    }

    /**
     * P1-1 seam: an {@code A -> B -> A} rebind restores every compared field to its original value, so a
     * pure value comparator sees no drift and would wrongly admit the request. The frozen path therefore
     * witnesses the binding <em>object</em> the context still publishes: the restored binding is a new
     * generation object, so the stale action snapshot can never be recombined with it.
     */
    @Test
    void valueEqualRebindIsANewGenerationAndStillRejectsBeforeCallback() {
        Harness harness = new Harness();
        TurnExecutionWindow resolved = harness.resolve("window-aba", "42345", 104L);
        WindowRuntimeContext context = resolved.context();
        WindowNativeBinding binding = resolved.binding();
        long epochBeforeRebind = context.getPlayerIdentityEpoch();
        AtomicInteger callbacks = new AtomicInteger();
        AtomicReference<WindowNativeBinding> restoredBinding = new AtomicReference<>();
        // The rebind happens on the worker thread, after take and before the frozen boundary looks.
        harness.queue.onTake = takenRequest -> {
            if ("frozen-aba-rebind".equals(takenRequest.getDescription())) {
                // A -> B, then back to an A' that is field-for-field identical to A but a different object.
                context.setNativeBinding(binding.withGeometry(
                        binding.getX() + 7, binding.getY(), binding.getWidth(), binding.getHeight()));
                context.setNativeBinding(binding.withGeometry(
                        binding.getX(), binding.getY(), binding.getWidth(), binding.getHeight()));
                restoredBinding.set(context.getNativeBinding());
            }
        };

        InputActionExecutionResult result = harness.queue.submitFrozenExactWindowExclusiveAndWait(
                "frozen-aba-rebind",
                context,
                binding,
                () -> {
                    callbacks.incrementAndGet();
                    return true;
                });

        WindowNativeBinding restored = restoredBinding.get();
        assertNotNull(restored, "the rebind must have run on the worker thread");
        assertNotSame(binding, restored, "the A->B->A rebind must publish a different binding object");
        assertEquals(binding.getNativeHandle(), restored.getNativeHandle());
        assertEquals(binding.getProcessId(), restored.getProcessId());
        assertEquals(binding.getX(), restored.getX());
        assertEquals(binding.getY(), restored.getY());
        assertEquals(binding.getWidth(), restored.getWidth());
        assertEquals(binding.getHeight(), restored.getHeight());
        assertEquals(epochBeforeRebind, context.getPlayerIdentityEpoch(),
                "the epoch is unchanged too, so only object generation can reveal the rebind");

        assertFalse(result.isCompleted(),
                "a value-equal rebind is a new generation and must never be admitted");
        assertEquals(InputActionSafetyReason.WINDOW_BINDING_CHANGED, result.getSafetyReason());
        assertEquals(1, harness.queue.taken.size(), "the request must reach the worker");
        assertEquals(0, callbacks.get(), "A->B->A must reject before callback/input");
        assertEquals(0, harness.inputCalls.get(), "A->B->A must send zero physical input");
        assertEquals(0, harness.focusCalls.get(),
                "the rebound request must fail before focus");
        assertEquals(1, harness.refresh.calls.get(),
                "the resolver refreshed once; the rebind must not trigger a re-refresh that would launder it");
    }

    /**
     * TURN-28Q seam: the frozen boundary previously accepted only a callback, so a caller needing its
     * complete action list under that boundary had to nest {@code submitAndWait} inside the callback
     * (queue-in-queue) or leave the list on the legacy path, which refreshes again. This drives the public
     * {@link InputSequences} facade and observes what the worker actually took off the real queue: exactly
     * one taken request, carrying the immutable baseline {@code CLICK_LEFT(delay=150) -> SLEEP(500)}, run
     * under one exact focus with no refresh at all.
     */
    @Test
    void frozenActionListTravelsAsOneTakenRequestThroughTheInputSequencesFacade() {
        MouseRecorder recorder = new MouseRecorder();
        Harness harness = new Harness(recorder);
        WindowRuntimeContext context = harness.context("window-actions", "72345", 107L);
        WindowNativeBinding binding = context.getNativeBinding();

        InputActionExecutionResult result = harness.sequences.submitFrozenExactWindowActionsAndWait(
                "frozen-action-list",
                context,
                binding,
                List.of(InputAction.clickLeft(300, 400, 150), InputAction.sleep(500)));

        assertTrue(result.isCompleted());
        assertEquals(InputActionSafetyReason.CLEAR, result.getSafetyReason());
        assertEquals(1, harness.queue.taken.size(),
                "the worker must take exactly ONE request for the whole list: a second taken request would "
                        + "mean the list was split into more than one queue submission");
        InputActionRequest taken = harness.queue.taken.get(0);
        assertEquals(result.getRequestId(), taken.getRequestId(),
                "the taken request must be the one the caller's result reports");
        List<InputAction> carried = taken.getActions();
        assertEquals(2, carried.size(), "that single request must carry the complete list");
        assertEquals(InputActionType.CLICK_LEFT, carried.get(0).getType());
        assertEquals(300, carried.get(0).getX());
        assertEquals(400, carried.get(0).getY());
        assertEquals(150, carried.get(0).getDelayMs(), "baseline 696a12b0 click delay");
        assertEquals(InputActionType.SLEEP, carried.get(1).getType());
        assertEquals(500, carried.get(1).getDelayMs(), "baseline 696a12b0 post-click hold");
        assertThrows(UnsupportedOperationException.class,
                () -> carried.add(InputAction.clickLeft(1, 1, 1)),
                "the request's list must be immutable once frozen");
        assertEquals(0, result.getStartedStepIndex());
        assertEquals(1, result.getLastCompletedStepIndex(),
                "both elements completed: a prefix of only the click would fail here");
        assertEquals(List.of("clickLeft(300,400,150)"), recorder.calls,
                "exactly one physical click, no second click, no preceding move, no no-op mouse action");
        assertEquals(1, harness.focusCalls.get(),
                "one frozen focus owns the whole list; a per-element focus would mean it was split");
        assertEquals(binding, harness.focusedBinding.get(),
                "focus must use the exact frozen binding, never a re-resolved one");
        assertEquals(0, harness.refresh.calls.get(), "the frozen path must never refresh");
    }

    /**
     * TURN-28Q seam: an atomic list is only worth anything if a closed gate stops it mid-list, and the stop
     * must arrive as the real typed {@code STOP_REQUESTED}, not as a bare false. The stop is requested by the
     * worker itself from inside the first click, so by the time the worker reaches the second element the
     * gate is already closed: an ordering fact, not a timing guess.
     */
    @Test
    void stopRequestedMidFrozenActionListProjectsTypedStopAndStartsNoLaterAction() {
        MouseRecorder recorder = new MouseRecorder();
        Harness harness = new Harness(recorder);
        WindowRuntimeContext context = harness.context("window-actions-stop", "82345", 108L);
        WindowNativeBinding binding = context.getNativeBinding();
        TaskPauseToken pauseToken = new TaskPauseToken();
        TaskStopToken stopToken = new TaskStopToken();
        TaskExecutionContext taskContext = TaskExecutionContext.builder()
                .taskCode("frozen-action-list-stop")
                .windowId(context.getWindowId())
                .pauseToken(pauseToken)
                .stopToken(stopToken)
                .build();
        recorder.onFirstClick(() -> {
            pauseToken.requestPause("stop-test");
            stopToken.requestStop("stop-test");
        });

        InputActionExecutionResult result = harness.taskContextHolder.callWith(taskContext, () ->
                harness.sequences.submitFrozenExactWindowActionsAndWait(
                        "frozen-action-list-stop",
                        context,
                        binding,
                        List.of(
                                InputAction.clickLeft(300, 400, 150),
                                InputAction.sleep(500),
                                InputAction.clickLeft(500, 600, 150))));

        assertEquals(List.of("clickLeft(300,400,150)"), recorder.calls,
                "once the stop is observed no later action may start: neither the hold nor the trailing "
                        + "click may reach the provider");
        assertFalse(result.isCompleted(), "a stopped list must never be reported as completed");
        assertEquals(InputActionSafetyReason.STOP_REQUESTED, result.getSafetyReason(),
                "the stop must be projected as the typed frozen safety reason, not flattened to a boolean");
        assertEquals(InputActionExecutionResult.Status.PARTIALLY_COMPLETED, result.getStatus());
        assertEquals(0, result.getStartedStepIndex());
        assertEquals(0, result.getLastCompletedStepIndex(),
                "the known completed prefix stays truthful: step 0 completed, nothing later did");
        assertTrue(result.getReason() != null && result.getReason().contains("task-stop"),
                "the terminal reason must carry the real task stop, not a generic worker-returned-false");
        assertEquals(1, harness.queue.taken.size(), "a stop must not re-enqueue or replay the request");
    }

    /**
     * TURN-28Q seam: a pause arriving mid-list must stop the list where it is, and a resume must continue the
     * very same request rather than re-enqueue or replay it. The pause is requested by the worker itself from
     * inside the first click, so the pause gate is already closed before the worker can reach element two —
     * the "zero later action" assertion below is an ordering fact and needs no sleep.
     */
    @Test
    void pauseMidFrozenActionListStartsNoLaterActionAndResumeContinuesTheSameRequest() throws Exception {
        MouseRecorder recorder = new MouseRecorder();
        Harness harness = new Harness(recorder);
        WindowRuntimeContext context = harness.context("window-actions-pause", "83345", 110L);
        WindowNativeBinding binding = context.getNativeBinding();
        BarrierPauseToken pauseToken = new BarrierPauseToken();
        TaskStopToken stopToken = new TaskStopToken();
        TaskExecutionContext taskContext = TaskExecutionContext.builder()
                .taskCode("frozen-action-list-pause")
                .windowId(context.getWindowId())
                .pauseToken(pauseToken)
                .stopToken(stopToken)
                .build();
        recorder.onFirstClick(() -> pauseToken.requestPause("pause-test"));
        AtomicReference<InputActionExecutionResult> result = new AtomicReference<>();
        Thread waiter = new Thread(() -> result.set(
                harness.taskContextHolder.callWith(taskContext, () ->
                        harness.sequences.submitFrozenExactWindowActionsAndWait(
                                "frozen-action-list-pause",
                                context,
                                binding,
                                List.of(
                                        InputAction.clickLeft(300, 400, 150),
                                        InputAction.sleep(1),
                                        InputAction.clickLeft(500, 600, 150))))),
                "frozen-action-list-pause-waiter");
        waiter.start();
        assertTrue(pauseToken.entered.await(2, TimeUnit.SECONDS),
                "the worker must actually reach the per-action pause gate; requesting a pause is not proof "
                        + "that anything waited on it");

        assertEquals(List.of("clickLeft(300,400,150)"), recorder.calls,
                "zero later action while paused: the worker is inside the pause wait, so nothing after the "
                        + "first click can have started");

        pauseToken.resume();
        waiter.join(TimeUnit.SECONDS.toMillis(5));

        assertFalse(waiter.isAlive());
        assertTrue(result.get().isCompleted(), "resume must continue the list to completion");
        assertEquals(1, harness.queue.taken.size(),
                "resume must continue the SAME request: a second taken request would be a re-enqueue/replay");
        assertEquals(harness.queue.taken.get(0).getRequestId(), result.get().getRequestId());
        assertEquals(2, result.get().getLastCompletedStepIndex(), "every element completed after resume");
        assertEquals(List.of("clickLeft(300,400,150)", "clickLeft(500,600,150)"), recorder.calls,
                "the trailing click runs only after resume, and exactly once");
        assertEquals(1, harness.focusCalls.get(),
                "one focus owns the paused-and-resumed list; a refocus would mean it was split");
        assertEquals(0, harness.refresh.calls.get(), "pause/resume must never add a refresh");
    }

    /**
     * TURN-28QT1 seam: an Alt shortcut inside a frozen list must be delivered to the EXACT frozen binding. The
     * background path used to call the context-resolving overload, which re-reads the window from mutable
     * current state and can refresh/commit inside the monitor this request holds — the exact thing the frozen
     * boundary exists to prevent.
     */
    @Test
    void frozenAltShortcutUsesOnlyTheExactBindingOverloadAndNeverRefreshes() {
        MouseRecorder recorder = new MouseRecorder();
        Harness harness = new Harness(recorder);
        WindowRuntimeContext context = harness.context("window-alt-exact", "78345", 116L);
        WindowNativeBinding binding = context.getNativeBinding();

        InputActionExecutionResult result = harness.sequences.submitFrozenExactWindowActionsAndWait(
                "frozen-alt-exact",
                context,
                binding,
                List.of(InputAction.pressAlt1()));

        assertTrue(result.isCompleted());
        assertEquals(InputActionSafetyReason.CLEAR, result.getSafetyReason());
        assertEquals(1, harness.keyboard.exactCalls.size(), "exactly one background delivery");
        ExactShortcutCall call = harness.keyboard.exactCalls.get(0);
        assertSame(binding, call.binding(), "delivery must target the exact frozen binding object");
        assertEquals("window-alt-exact", call.windowId());
        assertEquals(BoundWindowKeyboardService.AltShortcut.ALT_1, call.shortcut());
        assertEquals(0, harness.keyboard.mutableCalls.get(),
                "the context-resolving overload must never be reached by a frozen request");
        assertEquals(0, harness.refresh.calls.get(), "zero refresh inside the frozen monitor");
        assertEquals(List.of(), recorder.calls, "a successful background delivery sends no real input");
        assertEquals(1, harness.focusedBindings.size(), "exactly one frozen focus");
        assertSame(binding, harness.focusedBindings.get(0),
                "the focus must receive the frozen binding object itself, not a value-equal copy");
    }

    /**
     * TURN-28QT1 seam: {@code attempted=false} is a distinct production branch — the background delivery was
     * never even tried — and it falls through to the same focused real-input fallback as a failed attempt.
     * Covering only the failed-attempt branch leaves the not-attempted path unproven, and it is the branch a
     * misconfigured background keyboard actually takes.
     */
    @Test
    void frozenAltNotAttemptedFallsBackOnceThroughTheExactFrozenFocus() {
        MouseRecorder recorder = new MouseRecorder();
        Harness harness = new Harness(recorder);
        WindowRuntimeContext context = harness.context("window-alt-unattempted", "81345", 119L);
        WindowNativeBinding binding = context.getNativeBinding();
        // never attempted, not terminal -> production must still fall back to focused real input.
        harness.keyboard.nextAttempt = new BoundWindowKeyboardService.ShortcutAttempt(
                false, false, "hwnd-unavailable", false);

        InputActionExecutionResult result = harness.sequences.submitFrozenExactWindowActionsAndWait(
                "frozen-alt-unattempted",
                context,
                binding,
                List.of(InputAction.pressAlt1()));

        assertTrue(result.isCompleted());
        assertEquals(1, harness.queue.taken.size(), "one frozen request");
        assertEquals(1, harness.keyboard.exactCalls.size(), "exactly one exact background attempt");
        assertSame(binding, harness.keyboard.exactCalls.get(0).binding());
        assertEquals(0, harness.keyboard.mutableCalls.get(),
                "a frozen request must never reach the context-resolving overload");
        assertEquals(List.of("pressAlt1()"), recorder.calls,
                "exactly one real fallback input, with no retry");
        assertEquals(2, harness.focusedBindings.size(), "one focus for the list, one for the fallback");
        assertSame(binding, harness.focusedBindings.get(0));
        assertSame(binding, harness.focusedBindings.get(1),
                "the fallback focus must target the frozen binding object, not the mutable current window");
        assertEquals(0, harness.refresh.calls.get(), "zero refresh");
    }

    /**
     * TURN-28QT1 seam: when background delivery fails non-terminally the worker falls back to focused real
     * input. That fallback must focus the FROZEN binding, not the mutable current window.
     */
    @Test
    void frozenAltFallbackFocusesTheFrozenBindingAndStillNeverRefreshes() {
        MouseRecorder recorder = new MouseRecorder();
        Harness harness = new Harness(recorder);
        WindowRuntimeContext context = harness.context("window-alt-fallback", "79345", 117L);
        WindowNativeBinding binding = context.getNativeBinding();
        // attempted, unsuccessful, NOT terminal -> production falls back to focused real input.
        harness.keyboard.nextAttempt = new BoundWindowKeyboardService.ShortcutAttempt(
                true, false, "post-message-failed", false);

        InputActionExecutionResult result = harness.sequences.submitFrozenExactWindowActionsAndWait(
                "frozen-alt-fallback",
                context,
                binding,
                List.of(InputAction.pressAlt1()));

        assertTrue(result.isCompleted());
        assertEquals(1, harness.keyboard.exactCalls.size());
        assertEquals(0, harness.keyboard.mutableCalls.get());
        assertEquals(List.of("pressAlt1()"), recorder.calls,
                "the non-terminal failure must fall back to exactly one real Alt input");
        assertEquals(2, harness.focusedBindings.size(),
                "the frozen focus runs once for the list and once for the fallback");
        assertSame(binding, harness.focusedBindings.get(0),
                "the list focus must target the frozen binding object");
        assertSame(binding, harness.focusedBindings.get(1),
                "the fallback focus must target the frozen binding object too; checking only the last focus "
                        + "would let a wrong first focus be overwritten, and value equality would accept a copy");
        assertEquals(0, harness.refresh.calls.get(), "the fallback must not refresh the binding");
    }

    /**
     * TURN-28QT1 seam: real input is unconditional and irreversible, so drift discovered between the failed
     * background delivery and the fallback must abort BEFORE any key reaches the desktop. The rebind happens
     * from inside the keyboard call itself, so the ordering is deterministic.
     */
    @Test
    void frozenAltDriftBeforeFallbackSendsZeroRealInput() {
        MouseRecorder recorder = new MouseRecorder();
        Harness harness = new Harness(recorder);
        WindowRuntimeContext context = harness.context("window-alt-drift", "80345", 118L);
        WindowNativeBinding binding = context.getNativeBinding();
        harness.keyboard.nextAttempt = new BoundWindowKeyboardService.ShortcutAttempt(
                true, false, "post-message-failed", false);
        // A -> B -> A': value-equal, different object, published while the worker is mid-action.
        harness.keyboard.onExactCall = () -> {
            context.setNativeBinding(binding.withGeometry(
                    binding.getX() + 7, binding.getY(), binding.getWidth(), binding.getHeight()));
            context.setNativeBinding(binding.withGeometry(
                    binding.getX(), binding.getY(), binding.getWidth(), binding.getHeight()));
        };

        InputActionExecutionResult result = harness.sequences.submitFrozenExactWindowActionsAndWait(
                "frozen-alt-drift",
                context,
                binding,
                List.of(InputAction.pressAlt1()));

        assertFalse(result.isCompleted(), "drift must never be reported as a completed shortcut");
        assertEquals(InputActionSafetyReason.WINDOW_BINDING_CHANGED, result.getSafetyReason());
        assertEquals(List.of(), recorder.calls,
                "zero real input: the drifted generation must abort before the focused fallback");
        assertEquals(1, harness.keyboard.exactCalls.size(), "no retry of the background delivery");
        assertEquals(0, harness.keyboard.mutableCalls.get());
        assertEquals(1, harness.focusedBindings.size(),
                "only the initial frozen focus may have run; the fallback focus must not run on a drifted "
                        + "generation");
        assertSame(binding, harness.focusedBindings.get(0));
        assertEquals(0, harness.refresh.calls.get());
    }

    /**
     * TURN-28QT1 seam: a waiter that gives up while the worker sits in a pause must be able to end the request
     * cooperatively, with no {@code resume()} anywhere in this test.
     *
     * <p>The frozen path carries no deadline, so it used to wait in {@code TaskPauseToken.waitIfPaused(stopToken)},
     * which observes only the stop token. A cancelled waiter therefore blocked forever on a terminal future
     * nobody would publish, while the worker held the global input transaction. The barrier proves the worker is
     * genuinely inside the paused loop before the waiter is interrupted, so this is an ordering fact; the final
     * follow-up submission proves the transaction really was released rather than leaked.</p>
     */
    @Test
    void cancelledWaiterEscapesAPauseThatNeverResumesAndLeavesTheWorkerUsable() throws Exception {
        MouseRecorder recorder = new MouseRecorder();
        Harness harness = new Harness(recorder);
        WindowRuntimeContext context = harness.context("window-pause-cancel", "76345", 114L);
        WindowNativeBinding binding = context.getNativeBinding();
        BarrierPauseToken pauseToken = new BarrierPauseToken();
        TaskStopToken stopToken = new TaskStopToken();
        TaskExecutionContext taskContext = TaskExecutionContext.builder()
                .taskCode("frozen-pause-cancel")
                .windowId(context.getWindowId())
                .pauseToken(pauseToken)
                .stopToken(stopToken)
                .build();
        recorder.onFirstClick(() -> pauseToken.requestPause("pause-cancel-test"));
        AtomicReference<InputActionExecutionResult> result = new AtomicReference<>();
        Thread waiter = new Thread(() -> result.set(
                harness.taskContextHolder.callWith(taskContext, () ->
                        harness.sequences.submitFrozenExactWindowActionsAndWait(
                                "frozen-pause-cancel",
                                context,
                                binding,
                                List.of(
                                        InputAction.clickLeft(300, 400, 150),
                                        InputAction.sleep(500),
                                        InputAction.clickLeft(500, 600, 150))))),
                "frozen-pause-cancel-waiter");
        waiter.start();
        assertTrue(pauseToken.entered.await(2, TimeUnit.SECONDS),
                "the worker must be genuinely inside the paused wait before the waiter gives up");

        waiter.interrupt();
        waiter.join(TimeUnit.SECONDS.toMillis(5));

        assertFalse(waiter.isAlive(),
                "a cancelled waiter must not hang behind a pause that is never resumed");
        assertTrue(pauseToken.isPauseRequested(),
                "this test never calls resume(): the pause must still be in effect, so the request can only "
                        + "have ended through cooperative cancellation");
        assertEquals(List.of("clickLeft(300,400,150)"), recorder.calls,
                "no later action may run after the cancellation");
        assertFalse(result.get().isCompleted(), "a cancelled request must never report success");
        assertEquals(0, result.get().getLastCompletedStepIndex(), "truthful prefix");
        assertEquals(1, harness.queue.taken.size(), "no re-enqueue or replay");

        // The worker must still own a usable global transaction: a fresh unpaused request completes normally.
        WindowRuntimeContext nextContext = harness.context("window-after-cancel", "77345", 115L);
        InputActionExecutionResult after = harness.sequences.submitFrozenExactWindowActionsAndWait(
                "frozen-after-cancel",
                nextContext,
                nextContext.getNativeBinding(),
                List.of(InputAction.clickLeft(310, 410, 150)));

        assertTrue(after.isCompleted(),
                "the frozen transaction must have been released, not leaked by the cancelled pause");
        assertEquals(List.of("clickLeft(300,400,150)", "clickLeft(310,410,150)"), recorder.calls,
                "the follow-up click is the only input after the cancellation");
        assertEquals(2, harness.queue.taken.size());
    }

    /**
     * TURN-28Q Repair #3 seam: STOP and {@code A -> B -> A'} closing together. Both gates can answer, and they
     * disagree: the detector says the task stopped, the generation witness says the window changed. The queue
     * used to compare the binding object before constructing the request at all, so the witness always won and
     * the caller was told its window drifted when what actually ended it was a stop. Typed safety therefore
     * runs first and reports the detector's own reason.
     */
    @Test
    void stopAndValueEqualRebindTogetherReportTypedStopNotBindingDriftBeforeEnqueue() {
        MouseRecorder recorder = new MouseRecorder();
        Harness harness = new Harness(recorder);
        WindowRuntimeContext context = harness.context("window-stop-and-drift", "84345", 120L);
        WindowNativeBinding binding = context.getNativeBinding();
        TaskPauseToken pauseToken = new TaskPauseToken();
        TaskStopToken stopToken = new TaskStopToken();
        stopToken.requestStop("stopped-and-drifted-before-submit");
        TaskExecutionContext taskContext = TaskExecutionContext.builder()
                .taskCode("frozen-stop-and-drift")
                .windowId(context.getWindowId())
                .pauseToken(pauseToken)
                .stopToken(stopToken)
                .build();
        // A -> B -> A': value-equal, different object. On its own this is real drift the witness must reject.
        context.setNativeBinding(binding.withGeometry(
                binding.getX() + 7, binding.getY(), binding.getWidth(), binding.getHeight()));
        context.setNativeBinding(binding.withGeometry(
                binding.getX(), binding.getY(), binding.getWidth(), binding.getHeight()));
        assertNotSame(binding, context.getNativeBinding(),
                "the rebind must publish a different binding object, so the witness alone would reject");

        InputActionExecutionResult result = harness.taskContextHolder.callWith(taskContext, () ->
                harness.sequences.submitFrozenExactWindowActionsAndWait(
                        "frozen-stop-and-drift",
                        context,
                        binding,
                        List.of(InputAction.clickLeft(300, 400, 150), InputAction.sleep(500))));

        assertFalse(result.isCompleted());
        assertEquals(InputActionSafetyReason.STOP_REQUESTED, result.getSafetyReason(),
                "with both closed, the typed stop outranks the generation witness: reporting "
                        + "WINDOW_BINDING_CHANGED here would hide the stop that actually ended the request");
        assertEquals(InputActionExecutionResult.Status.NOT_STARTED, result.getStatus());
        assertEquals(-1, result.getStartedStepIndex());
        assertTrue(result.getReason() != null && result.getReason().contains("task-stop"),
                "the reason must be the detector's own stop reason, not a window-change reason");
        assertEquals(0, harness.queue.taken.size(), "zero take");
        assertEquals(List.of(), recorder.calls, "zero input");
        assertEquals(0, harness.focusCalls.get(), "zero focus");
        assertEquals(0, harness.refresh.calls.get(), "zero refresh");
    }

    /**
     * TURN-28Q Repair #3 seam: the same collision against the worker's frozen preamble, which used to run the
     * generic identity-epoch comparator before frozen typed safety.
     *
     * <p>The gates must close while the target is <em>already taken</em>, otherwise the preamble is never the
     * thing under test. Closing them from the test thread while the request sits queued cannot prove that:
     * whichever way the queue then behaves, the evidence is about queueing, not about
     * {@code InputActionWorker}'s preamble. So they are closed from inside the production {@code take()}, on the
     * worker thread, in the window between hand-off and the preamble — an ordering fact with no polling, no
     * sleep and no race. The blocker exists only to guarantee the target is genuinely queued and later taken;
     * both takes are counted separately below so neither can stand in for the other.</p>
     */
    @Test
    void takenStopAndIdentityDriftTogetherReportTypedStopNotBindingDrift() throws Exception {
        MouseRecorder recorder = new MouseRecorder();
        Harness harness = new Harness(recorder);
        BlockingRequest blocker = harness.blockWorker();
        WindowRuntimeContext context = harness.context("window-taken-stop-drift", "85345", 121L);
        WindowNativeBinding binding = context.getNativeBinding();
        TaskPauseToken pauseToken = new TaskPauseToken();
        TaskStopToken stopToken = new TaskStopToken();
        TaskExecutionContext taskContext = TaskExecutionContext.builder()
                .taskCode("frozen-taken-stop-drift")
                .windowId(context.getWindowId())
                .pauseToken(pauseToken)
                .stopToken(stopToken)
                .build();
        /*
         * Rebinding to a different native handle is a hard native change, so production itself bumps
         * playerIdentityEpoch and publishes a new binding object: one act closes the identity-epoch comparator
         * and the generation witness together, which is exactly the collision that used to outrank the stop.
         */
        harness.queue.onTake = takenRequest -> {
            if ("frozen-taken-stop-drift".equals(takenRequest.getDescription())) {
                stopToken.requestStop("stopped-after-take");
                context.setNativeBinding(new WindowNativeBinding(
                        "95345", "game-window-taken-stop-drift", "GameWindow", 121L, 100, 200, 800, 600));
            }
        };
        AtomicReference<InputActionExecutionResult> result = new AtomicReference<>();
        Thread waiter = new Thread(() -> result.set(
                harness.taskContextHolder.callWith(taskContext, () ->
                        harness.sequences.submitFrozenExactWindowActionsAndWait(
                                "frozen-taken-stop-drift",
                                context,
                                binding,
                                List.of(InputAction.clickLeft(300, 400, 150), InputAction.sleep(500))))),
                "frozen-taken-stop-drift-waiter");
        waiter.start();

        // The worker blocks inside take() until the target arrives, so releasing now needs no polling.
        blocker.release.countDown();
        blocker.waiter.join(TimeUnit.SECONDS.toMillis(2));
        waiter.join(TimeUnit.SECONDS.toMillis(5));

        assertFalse(waiter.isAlive());
        assertEquals(2, harness.queue.taken.size(),
                "the blocker and the target must each have been taken exactly once");
        assertEquals("frozen-blocker", harness.queue.taken.get(0).getDescription(),
                "first take is the blocker");
        assertEquals("frozen-taken-stop-drift", harness.queue.taken.get(1).getDescription(),
                "the target must really reach the worker; a request that never got taken would prove nothing "
                        + "about the frozen preamble this repair changed");
        assertEquals(result.get().getRequestId(), harness.queue.taken.get(1).getRequestId(),
                "and it must be the very request the caller is waiting on");
        assertEquals(InputActionSafetyReason.STOP_REQUESTED, result.get().getSafetyReason(),
                "the taken request must report the typed stop, not the identity/generation drift that closed "
                        + "with it");
        assertFalse(result.get().isCompleted());
        assertEquals(InputActionExecutionResult.Status.NOT_STARTED, result.get().getStatus());
        assertEquals(-1, result.get().getStartedStepIndex(), "no step may start");
        assertEquals(List.of(), recorder.calls, "zero input");
        assertEquals(1, harness.focusCalls.get(),
                "only the blocker may focus; the stopped target must fail before its own frozen focus");
        assertEquals(0, harness.refresh.calls.get(), "zero refresh");
    }

    /**
     * TURN-28QT1 seam: a stop already closed before enqueue must terminate as a typed stop. It previously came
     * back as {@code WINDOW_BINDING_CHANGED} because the generation witness silently folded the stop check in
     * and the queue relabeled its every {@code false} as binding drift. The rejection is synchronous, so this
     * needs no worker, latch or timing.
     */
    @Test
    void stopClosedBeforeEnqueueTerminatesTypedAndNeverEntersTheQueue() {
        MouseRecorder recorder = new MouseRecorder();
        Harness harness = new Harness(recorder);
        WindowRuntimeContext context = harness.context("window-stop-preenqueue", "73345", 111L);
        WindowNativeBinding binding = context.getNativeBinding();
        TaskPauseToken pauseToken = new TaskPauseToken();
        TaskStopToken stopToken = new TaskStopToken();
        stopToken.requestStop("stopped-before-submit");
        TaskExecutionContext taskContext = TaskExecutionContext.builder()
                .taskCode("frozen-stop-preenqueue")
                .windowId(context.getWindowId())
                .pauseToken(pauseToken)
                .stopToken(stopToken)
                .build();

        InputActionExecutionResult result = harness.taskContextHolder.callWith(taskContext, () ->
                harness.sequences.submitFrozenExactWindowActionsAndWait(
                        "frozen-stop-preenqueue",
                        context,
                        binding,
                        List.of(InputAction.clickLeft(300, 400, 150), InputAction.sleep(500))));

        assertFalse(result.isCompleted());
        assertEquals(InputActionSafetyReason.STOP_REQUESTED, result.getSafetyReason(),
                "a stop closed before enqueue is a stop, not binding drift");
        assertEquals(InputActionExecutionResult.Status.NOT_STARTED, result.getStatus());
        assertEquals(-1, result.getStartedStepIndex(), "no step may start");
        assertEquals(-1, result.getLastCompletedStepIndex());
        assertTrue(result.getReason() != null && result.getReason().contains("task-stop"),
                "the terminal reason must carry the real stop, not a window-change reason");
        assertEquals(0, harness.queue.taken.size(), "a stopped request must never reach the worker");
        assertEquals(List.of(), recorder.calls, "zero physical input");
        assertEquals(0, harness.focusCalls.get(), "zero focus");
        assertEquals(0, harness.refresh.calls.get(), "the frozen path must never refresh");
    }

    /**
     * TURN-28QT1 seam: a stop that closes with NO pause outstanding, after an earlier action completed, must
     * still be typed. This is the case the old code could only observe through the conflated witness, so it
     * was reported as binding drift; and if it closed during the LAST action there was no gate left at all and
     * the request published success. The stop is requested by the worker from inside its own first click, so
     * ordering is deterministic without any pause, latch or sleep.
     */
    @Test
    void stopOnlyAfterFirstActionIsTypedAndStartsNoLaterAction() {
        MouseRecorder recorder = new MouseRecorder();
        Harness harness = new Harness(recorder);
        WindowRuntimeContext context = harness.context("window-stop-only", "74345", 112L);
        WindowNativeBinding binding = context.getNativeBinding();
        TaskPauseToken pauseToken = new TaskPauseToken();
        TaskStopToken stopToken = new TaskStopToken();
        TaskExecutionContext taskContext = TaskExecutionContext.builder()
                .taskCode("frozen-stop-only")
                .windowId(context.getWindowId())
                .pauseToken(pauseToken)
                .stopToken(stopToken)
                .build();
        // No pause is ever requested: the stop alone must be observed by the next per-action gate.
        recorder.onFirstClick(() -> stopToken.requestStop("stop-only-after-action-0"));

        InputActionExecutionResult result = harness.taskContextHolder.callWith(taskContext, () ->
                harness.sequences.submitFrozenExactWindowActionsAndWait(
                        "frozen-stop-only",
                        context,
                        binding,
                        List.of(
                                InputAction.clickLeft(300, 400, 150),
                                InputAction.sleep(500),
                                InputAction.clickLeft(500, 600, 150))));

        assertEquals(List.of("clickLeft(300,400,150)"), recorder.calls,
                "no later action may start once the stop is observed");
        assertFalse(result.isCompleted());
        assertEquals(InputActionSafetyReason.STOP_REQUESTED, result.getSafetyReason(),
                "a stop-only closure must be typed STOP_REQUESTED, never WINDOW_BINDING_CHANGED");
        assertEquals(0, result.getStartedStepIndex());
        assertEquals(0, result.getLastCompletedStepIndex(), "truthful prefix: only step 0 completed");
        assertEquals(1, harness.queue.taken.size(), "exactly one taken request; no replay");
        assertEquals(1, harness.focusCalls.get());
        assertEquals(0, harness.refresh.calls.get());
    }

    /**
     * TURN-28QT1 seam: a stop closing during the FINAL action has no later action to block, so the only thing
     * that can catch it is the final gate before success. Without that gate the request publishes
     * {@code COMPLETED} for a list that finished under a task lifecycle that no longer holds.
     */
    @Test
    void stopClosedDuringFinalActionNeverFabricatesSuccess() {
        MouseRecorder recorder = new MouseRecorder();
        Harness harness = new Harness(recorder);
        WindowRuntimeContext context = harness.context("window-stop-final", "75345", 113L);
        WindowNativeBinding binding = context.getNativeBinding();
        TaskPauseToken pauseToken = new TaskPauseToken();
        TaskStopToken stopToken = new TaskStopToken();
        TaskExecutionContext taskContext = TaskExecutionContext.builder()
                .taskCode("frozen-stop-final")
                .windowId(context.getWindowId())
                .pauseToken(pauseToken)
                .stopToken(stopToken)
                .build();
        // The list's only action is the click, so this stop closes during the final action.
        recorder.onFirstClick(() -> stopToken.requestStop("stop-during-final-action"));

        InputActionExecutionResult result = harness.taskContextHolder.callWith(taskContext, () ->
                harness.sequences.submitFrozenExactWindowActionsAndWait(
                        "frozen-stop-final",
                        context,
                        binding,
                        List.of(InputAction.clickLeft(300, 400, 150))));

        assertEquals(List.of("clickLeft(300,400,150)"), recorder.calls, "the final action itself still ran");
        assertFalse(result.isCompleted(),
                "the final gate must deny success for a list whose stop closed during its last action");
        assertNotEquals(InputActionExecutionResult.Status.COMPLETED, result.getStatus());
        assertEquals(InputActionSafetyReason.STOP_REQUESTED, result.getSafetyReason());
        assertEquals(0, result.getStartedStepIndex());
        assertEquals(0, result.getLastCompletedStepIndex(),
                "the prefix stays truthful: the step really did complete, the request did not");
        assertEquals(1, harness.queue.taken.size());
    }

    /**
     * TURN-28Q seam: the action-list boundary must inherit the callback path's generation witness, not a
     * weaker value comparison. An {@code A -> B -> A} rebind restores every compared field, so a pure value
     * comparator sees no drift and would drive real input at a stale snapshot. The rejection is synchronous
     * and pre-enqueue, so it is proven by the returned typed result rather than by any timing.
     */
    @Test
    void valueEqualRebindRejectsTheFrozenActionListBeforeEnqueueWithTypedBindingChange() {
        MouseRecorder recorder = new MouseRecorder();
        Harness harness = new Harness(recorder);
        WindowRuntimeContext context = harness.context("window-actions-aba", "92345", 109L);
        WindowNativeBinding binding = context.getNativeBinding();

        // A -> B, then back to an A' that is field-for-field identical to A but a different object.
        context.setNativeBinding(binding.withGeometry(
                binding.getX() + 7, binding.getY(), binding.getWidth(), binding.getHeight()));
        context.setNativeBinding(binding.withGeometry(
                binding.getX(), binding.getY(), binding.getWidth(), binding.getHeight()));
        WindowNativeBinding restored = context.getNativeBinding();
        assertNotSame(binding, restored, "the A->B->A rebind must publish a different binding object");
        assertEquals(binding.getNativeHandle(), restored.getNativeHandle());
        assertEquals(binding.getProcessId(), restored.getProcessId());
        assertEquals(binding.getX(), restored.getX());
        assertEquals(binding.getY(), restored.getY());
        assertEquals(binding.getWidth(), restored.getWidth());
        assertEquals(binding.getHeight(), restored.getHeight());

        InputActionExecutionResult result = harness.sequences.submitFrozenExactWindowActionsAndWait(
                "frozen-action-list-aba",
                context,
                binding,
                List.of(InputAction.clickLeft(300, 400, 150), InputAction.sleep(500)));

        assertFalse(result.isCompleted(),
                "a value-equal rebind is a new generation and must never be admitted");
        assertEquals(InputActionExecutionResult.Status.NOT_STARTED, result.getStatus(),
                "the stale list must be rejected before any step starts");
        assertEquals(InputActionSafetyReason.WINDOW_BINDING_CHANGED, result.getSafetyReason(),
                "the rejection must carry the typed safety reason, not a bare false");
        assertEquals(0, harness.queue.taken.size(), "a stale generation must never reach the worker");
        assertEquals(List.of(), recorder.calls, "A->B->A must send zero physical input");
        assertEquals(0, harness.inputCalls.get(), "A->B->A must send zero physical input");
        assertEquals(0, harness.focusCalls.get(), "the rebound list must fail before focus");
        assertEquals(0, harness.refresh.calls.get(), "the frozen action-list path must never add a refresh");
    }

    private static boolean await(CountDownLatch latch) {
        try {
            return latch.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private static final class Harness {
        private final CountingRefreshService refresh = new CountingRefreshService();
        private final AtomicInteger inputCalls = new AtomicInteger();
        private final AtomicInteger focusCalls = new AtomicInteger();
        private final AtomicReference<WindowNativeBinding> focusedBinding = new AtomicReference<>();
        private final List<WindowNativeBinding> focusedBindings = new CopyOnWriteArrayList<>();
        private final TaskExecutionContextHolder taskContextHolder = new TaskExecutionContextHolder();
        private final CountingKeyboardService keyboard = new CountingKeyboardService();
        private final CountingQueue queue;
        private final InputSequences sequences;
        private final WindowTaskContextHolder contextHolder;

        private Harness() {
            this(null);
        }

        /**
         * @param recorder when null the worker keeps the callback tests' provider, which fails on any
         *                 physical input; when non-null the action-list tests observe the real dispatcher's
         *                 calls in order. No desktop input is performed either way.
         */
        private Harness(MouseRecorder recorder) {
            WindowIsolationProperties properties = new WindowIsolationProperties();
            properties.setIsolationEnabled(true);
            properties.setInputFocusEnabled(true);
            contextHolder = new WindowTaskContextHolder(properties);
            GlobalInputLock inputLock = new GlobalInputLock();
            queue = new CountingQueue(contextHolder, refresh, taskContextHolder);
            sequences = new InputSequences(queue);
            WindowAwareInputCoordinator coordinator = new WindowAwareInputCoordinator(
                    inputLock,
                    contextHolder,
                    new CountingFocusService(inputLock, focusCalls, focusedBinding, focusedBindings),
                    properties,
                    new NoOpInteractionMetricsService(),
                    refresh);
            InputActionWorker worker = new InputActionWorker(
                    queue,
                    new InputActionDeadLetter(),
                    recorder == null ? noInputProvider(inputCalls) : recorder.provider(inputCalls),
                    coordinator,
                    contextHolder,
                    keyboard);
            worker.start();
        }

        private WindowRuntimeContext context(String windowId, String handle, long processId) {
            WindowRuntimeContext context = new WindowRuntimeContext(windowId, new GameContext());
            context.setNativeBinding(new WindowNativeBinding(
                    handle, "game-" + windowId, "GameWindow", processId,
                    100, 200, 800, 600));
            return context;
        }

        /**
         * Resolve one exact window through the real public {@link TurnExecutionWindow#resolveForAction} against
         * a real registry and a real runner, exactly as {@code LocalTurnActionExecutor} does.
         *
         * <p>This is the resolver-owned refresh: {@code resolveForAction} calls {@code refreshAndCommit} once
         * and the frozen queue path adds none, which is why the cases built on this helper assert
         * {@code refresh.calls == 1} rather than zero. Handing the frozen boundary a hand-made
         * {@code (context, binding)} pair instead would assert nothing about the resolver at all.</p>
         */
        private TurnExecutionWindow resolve(String windowId, String handle, long processId) {
            WindowRuntimeContext context = context(windowId, handle, processId);
            refresh.binding = context.getNativeBinding();
            TestTaskManager taskManager = new TestTaskManager(
                    windowId,
                    new BareWindowTaskRunner(context, contextHolder, taskContextHolder, sequences));
            TurnAction action = new TurnAction(1, "action-" + windowId, "device-1", windowId, List.of(), false);
            return TurnExecutionWindow.resolveForAction(action, taskManager, refresh);
        }

        private BlockingRequest blockWorker() throws InterruptedException {
            WindowRuntimeContext context = context("window-blocker", "99999", 999L);
            WindowNativeBinding binding = context.getNativeBinding();
            CountDownLatch entered = new CountDownLatch(1);
            CountDownLatch release = new CountDownLatch(1);
            Thread waiter = new Thread(() -> queue.submitFrozenExactWindowExclusiveAndWait(
                    "frozen-blocker",
                    context,
                    binding,
                    () -> {
                        entered.countDown();
                        return await(release);
                    }), "frozen-blocker-waiter");
            waiter.start();
            assertTrue(entered.await(2, TimeUnit.SECONDS));
            return new BlockingRequest(waiter, release);
        }
    }

    /**
     * Records every request the real worker actually took off the real queue, by delegating to the
     * production {@code take()}. "One queue submission" is then an observed fact rather than something
     * inferred from a request id: a split list would show up here as a second taken request.
     */
    private static final class CountingQueue extends InputActionQueue {
        private final List<InputActionRequest> taken = new CopyOnWriteArrayList<>();
        private volatile Consumer<InputActionRequest> onTake;

        private CountingQueue(
                WindowTaskContextHolder windowTaskContextHolder,
                WindowNativeBindingRefreshService bindingRefreshService,
                TaskExecutionContextHolder taskExecutionContextHolder) {
            super(windowTaskContextHolder, bindingRefreshService, taskExecutionContextHolder);
        }

        /**
         * {@code onTake} runs on the worker thread itself, after the production {@code take()} has handed the
         * request over and before the worker reaches its frozen preamble. That is the only window in which a
         * test can close a gate against an already-taken request without polling or guessing at timing.
         */
        @Override
        InputActionRequest take() throws InterruptedException {
            InputActionRequest request = super.take();
            taken.add(request);
            Consumer<InputActionRequest> hook = onTake;
            if (hook != null) {
                hook.accept(request);
            }
            return request;
        }
    }

    /**
     * Distinguishes the two production keyboard overloads.
     *
     * <p>A frozen request owns an exact binding, so it must reach {@code pressShortcut(binding, windowId,
     * shortcut)}. The mutable {@code pressShortcut(shortcut)} overload resolves the window from current state
     * and can refresh inside the frozen monitor, so any call to it is a defect — counting it separately is what
     * makes "the exact overload was used" an observed fact rather than an inference from a success flag.</p>
     */
    private static final class CountingKeyboardService extends BoundWindowKeyboardService {
        private final List<ExactShortcutCall> exactCalls = new CopyOnWriteArrayList<>();
        private final AtomicInteger mutableCalls = new AtomicInteger();
        private volatile ShortcutAttempt nextAttempt = new ShortcutAttempt(true, true, null, false);
        private volatile Runnable onExactCall;

        private CountingKeyboardService() {
            super(null, null, null, null);
        }

        @Override
        public ShortcutAttempt pressShortcut(AltShortcut shortcut) {
            mutableCalls.incrementAndGet();
            return nextAttempt;
        }

        @Override
        public ShortcutAttempt pressShortcut(
                WindowNativeBinding binding, String windowId, AltShortcut shortcut) {
            exactCalls.add(new ExactShortcutCall(binding, windowId, shortcut));
            Runnable hook = onExactCall;
            if (hook != null) {
                hook.run();
            }
            return nextAttempt;
        }
    }

    private record ExactShortcutCall(
            WindowNativeBinding binding,
            String windowId,
            BoundWindowKeyboardService.AltShortcut shortcut) {
    }

    /**
     * The registry {@link TurnExecutionWindow#resolveForAction} asks for the exact logical window's runner.
     *
     * <p>{@code MultiWindowTaskManager}'s only constructor assigns all twenty-two collaborators directly and
     * null-checks none of them (it merely normalizes the provider list), so an inert registry needs no
     * unsafe allocation and no reflection: nulls are legal here, and this subclass answers the one lookup
     * the resolver performs. {@code getRunner} is public and non-final, so overriding it is ordinary Java.</p>
     */
    private static final class TestTaskManager extends MultiWindowTaskManager {
        private final String windowId;
        private final WindowTaskRunner runner;

        private TestTaskManager(String windowId, WindowTaskRunner runner) {
            super(null, null, null, null, null, null, null, null, null, null, null,
                    null, null, null, null, null, null, null, null, null, List.of(), null);
            this.windowId = windowId;
            this.runner = runner;
        }

        @Override
        public Optional<WindowTaskRunner> getRunner(String requestedWindowId) {
            return windowId.equals(requestedWindowId) ? Optional.of(runner) : Optional.empty();
        }
    }

    /**
     * A real {@link WindowTaskRunner} that owns the exact window context and runs no task.
     *
     * <p>Unlike the registry, {@code WindowTaskRunner}'s only constructor {@code requireNonNull}s every
     * collaborator except the provider list, so each one is constructed legally instead of allocated. Every
     * arity below was counted from that collaborator's own declaration rather than assumed:
     * Lombok's {@code @RequiredArgsConstructor} takes only the top-level {@code final} fields that have no
     * initializer, so initialized fields ({@code new Random()}, the state maps) and nested-class fields are
     * not constructor parameters. None of the six Lombok collaborators declares {@code @NonNull}, so the
     * generated constructors perform no runtime null check. {@code RouteCloudDecisionService} is the one
     * exception to the all-null shape: its constructor body dereferences {@code properties.getTimeoutMs()},
     * so it is given a real defaulted properties object. {@code TaskMaintenanceService} declares an eight-arg
     * and a ten-arg public constructor; the arities differ, so the ten-null call below is unambiguous.</p>
     *
     * <p>The runner's own executors are created but never submitted to, so no task thread starts.</p>
     */
    private static final class BareWindowTaskRunner extends WindowTaskRunner {
        private BareWindowTaskRunner(WindowRuntimeContext windowContext,
                                     WindowTaskContextHolder contextHolder,
                                     TaskExecutionContextHolder taskExecutionContextHolder,
                                     InputSequences inputSequences) {
            super(windowContext,
                    (context, taskType) -> null,
                    contextHolder,
                    (context, executionContext) -> false,
                    taskExecutionContextHolder,
                    inputSequences,
                    new TeamRoleDetectionService(null, null, null, null, null, null,
                            null, null, null, null, null, null),
                    new TaskTeamAssignmentPolicy(null),
                    new AutomationMetricsService(null),
                    new AutoCombatService(null, null, null, null, null, null, null, null, null, null, null),
                    new MiniMapCoordinateReader(null, null, null, null, null),
                    new DialogService(null, null, null, null, null, null, null, null, null, null, null),
                    new UICleanerService(null, null, null, null, null, null, null, null, null),
                    new TaskTrackerPanelService(null, null, null, null, null, null, null, null, null),
                    new MapNameCanonicalizer(),
                    new MemoryService(null),
                    new RouteCloudDecisionService(null, null, new CloudDecisionProperties()),
                    new TaskMaintenanceService(null, null, null, null, null, null, null, null, null, null),
                    List.of(),
                    new WindowReadyEventBus());
        }
    }

    private static final class CountingRefreshService extends WindowNativeBindingRefreshService {
        private final AtomicInteger calls = new AtomicInteger();
        private WindowNativeBinding binding;

        @Override
        public Optional<WindowNativeBinding> refreshAndCommit(WindowRuntimeContext context) {
            calls.incrementAndGet();
            if (context == null || binding == null) {
                return Optional.empty();
            }
            context.setNativeBinding(binding);
            return Optional.of(context.getNativeBinding());
        }
    }

    /**
     * Records every focused binding in order, by identity.
     *
     * <p>A single "last focus" reference is not evidence when a request focuses more than once: the fallback
     * focus would overwrite a wrong first focus and the assertion would still pass. Value equality is not
     * evidence either, because a value-equal copy is a different generation — exactly what the frozen boundary
     * exists to reject. Keeping the ordered list lets each call be checked with {@code assertSame} against the
     * binding object frozen at claim time.</p>
     */
    private static final class CountingFocusService extends WindowFocusService {
        private final AtomicInteger calls;
        private final AtomicReference<WindowNativeBinding> binding;
        private final List<WindowNativeBinding> focused;

        private CountingFocusService(
                GlobalInputLock inputLock,
                AtomicInteger calls,
                AtomicReference<WindowNativeBinding> binding,
                List<WindowNativeBinding> focused) {
            super(inputLock);
            this.calls = calls;
            this.binding = binding;
            this.focused = focused;
        }

        @Override
        public boolean focusWithoutLock(WindowNativeBinding exactBinding) {
            calls.incrementAndGet();
            binding.set(exactBinding);
            focused.add(exactBinding);
            return true;
        }
    }

    private static final class NoOpInteractionMetricsService extends WindowInteractionMetricsService {
        @Override
        public void recordFocus(String windowId, String actionName, boolean success) {
        }
    }

    private static InputProvider noInputProvider(AtomicInteger inputCalls) {
        return (InputProvider) Proxy.newProxyInstance(
                InputProvider.class.getClassLoader(),
                new Class<?>[] {InputProvider.class},
                (proxy, method, args) -> {
                    if (method.getDeclaringClass() == Object.class) {
                        return switch (method.getName()) {
                            case "toString" -> "NoInputProvider";
                            case "hashCode" -> System.identityHashCode(proxy);
                            case "equals" -> proxy == args[0];
                            default -> null;
                        };
                    }
                    inputCalls.incrementAndGet();
                    throw new AssertionError("frozen callback test sent unexpected physical input: "
                            + method.getName());
                });
    }

    /**
     * A real {@link TaskPauseToken} that additionally announces when a waiter has actually entered the
     * production pause wait.
     *
     * <p>Counting down inside the first click only proves the pause was <em>requested</em>. Counting down on
     * entry to this method is no better: the frozen worker calls it at its pre-focus pause check too, while
     * nothing is paused, so the latch would fire before the first click and still prove nothing.</p>
     *
     * <p>Production evaluates {@code wakeCondition} only from inside {@code while (pauseRequested)}, after the
     * stop check and before {@code monitor.wait(...)}. Announcing from within the wrapped condition therefore
     * fires if and only if a waiter is genuinely sitting in the paused loop. Once {@code entered} fires, the
     * worker is inside that wait and cannot have started any later action, so the assertions that follow are
     * ordering facts. The real condition is delegated to verbatim, preserving production's null semantics.</p>
     */
    private static final class BarrierPauseToken extends TaskPauseToken {
        private final CountDownLatch entered = new CountDownLatch(1);

        @Override
        public PauseWaitSnapshot waitIfPausedRevision(
                TaskStopToken stopToken, java.util.function.BooleanSupplier wakeCondition) {
            return super.waitIfPausedRevision(stopToken, () -> {
                entered.countDown();
                return wakeCondition != null && wakeCondition.getAsBoolean();
            });
        }
    }

    /**
     * Records what the worker's real action dispatcher actually sent, in order, without touching a desktop.
     *
     * <p>The action-list boundary must be proven by observation rather than inference: {@code SLEEP} is
     * executed by {@code TaskSleep} and never reaches an {@link InputProvider}, so a list element after the
     * sleep is what makes "no later action started" directly observable, while the sleep's own completion is
     * proven by the request's typed step progress.</p>
     */
    private static final class MouseRecorder {
        private final List<String> calls = new CopyOnWriteArrayList<>();
        private final AtomicReference<Runnable> onFirstClick = new AtomicReference<>();

        private void onFirstClick(Runnable hook) {
            onFirstClick.set(hook);
        }

        private InputProvider provider(AtomicInteger inputCalls) {
            return (InputProvider) Proxy.newProxyInstance(
                    InputProvider.class.getClassLoader(),
                    new Class<?>[] {InputProvider.class},
                    (proxy, method, args) -> {
                        if (method.getDeclaringClass() == Object.class) {
                            return switch (method.getName()) {
                                case "toString" -> "MouseRecorder";
                                case "hashCode" -> System.identityHashCode(proxy);
                                case "equals" -> proxy == args[0];
                                default -> null;
                            };
                        }
                        inputCalls.incrementAndGet();
                        StringBuilder call = new StringBuilder(method.getName()).append('(');
                        for (int i = 0; args != null && i < args.length; i++) {
                            call.append(i == 0 ? "" : ",").append(args[i]);
                        }
                        calls.add(call.append(')').toString());
                        if ("clickLeft".equals(method.getName())) {
                            Runnable hook = onFirstClick.getAndSet(null);
                            if (hook != null) {
                                hook.run();
                            }
                        }
                        return method.getReturnType() == boolean.class ? Boolean.TRUE : null;
                    });
        }
    }

    private record BlockingRequest(Thread waiter, CountDownLatch release) {
    }
}
