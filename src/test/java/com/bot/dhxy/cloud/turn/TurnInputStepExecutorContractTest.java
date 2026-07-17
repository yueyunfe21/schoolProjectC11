package com.bot.dhxy.cloud.turn;

import com.bot.dhxy.cloud.turn.protocol.TurnAction;
import com.bot.dhxy.cloud.turn.protocol.TurnInputAction;
import com.bot.dhxy.cloud.turn.protocol.TurnInputSpec;
import com.bot.dhxy.cloud.turn.protocol.TurnStep;
import com.bot.dhxy.cloud.turn.protocol.TurnStepType;
import com.bot.dhxy.config.WindowIsolationProperties;
import com.bot.dhxy.core.GameContext;
import com.bot.dhxy.driver.BoundWindowKeyboardService;
import com.bot.dhxy.input.action.InputAction;
import com.bot.dhxy.input.action.InputActionExecutionResult;
import com.bot.dhxy.input.action.InputActionQueue;
import com.bot.dhxy.input.action.InputActionSafetyReason;
import com.bot.dhxy.input.action.InputActionType;
import com.bot.dhxy.runner.context.TaskExecutionContextHolder;
import com.bot.dhxy.window.execution.MultiWindowTaskManager;
import com.bot.dhxy.window.execution.RunningTaskHandle;
import com.bot.dhxy.window.execution.WindowTaskRunner;
import com.bot.dhxy.window.model.WindowNativeBinding;
import com.bot.dhxy.window.model.WindowRuntimeStatus;
import com.bot.dhxy.window.runtime.WindowNativeBindingRefreshService;
import com.bot.dhxy.window.runtime.WindowRuntimeContext;
import com.bot.dhxy.window.runtime.WindowTaskContextHolder;
import org.junit.jupiter.api.Test;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TurnInputStepExecutorContractTest {

    private static final Unsafe UNSAFE = findUnsafe();

    @Test
    void mapsAllSevenMouseFormsWithoutScalingAndSubmitsEachAsOneAtomicRequest() {
        Harness harness = harness();
        TurnExecutionWindow window = executionWindow(false);

        assertCompleted(harness.executor.execute(
                window, TurnInputAction.MOVE_MOUSE, point(138, 242), 0));
        assertCompleted(harness.executor.execute(
                window, TurnInputAction.CLICK_LEFT, point(139, 243), 1));
        assertCompleted(harness.executor.execute(
                window, TurnInputAction.CLICK_RIGHT, point(140, 244), 2));
        assertCompleted(harness.executor.execute(
                window, TurnInputAction.DOUBLE_CLICK_LEFT, point(141, 245), 3));
        assertCompleted(harness.executor.execute(
                window, TurnInputAction.DOUBLE_CLICK_RIGHT, point(142, 246), 4));
        assertCompleted(harness.executor.execute(
                window,
                TurnInputAction.DRAG_LEFT,
                new TurnInputSpec(143, 247, 145, 249, null, null, null),
                5));
        assertCompleted(harness.executor.execute(
                window,
                TurnInputAction.SCROLL,
                new TurnInputSpec(144, 248, null, null, -3, null, null),
                6));

        assertEquals(7, harness.queue.submissions.size(), "one wire input must equal one queue request");
        assertAction(harness.queue.submissions.get(0), 0, InputActionType.MOVE_MOUSE, 138, 242);
        assertAction(harness.queue.submissions.get(1), 0, InputActionType.CLICK_LEFT, 139, 243);
        assertAction(harness.queue.submissions.get(2), 0, InputActionType.CLICK_RIGHT, 140, 244);
        assertEquals(2, harness.queue.submissions.get(3).size());
        assertAction(harness.queue.submissions.get(3), 0, InputActionType.CLICK_LEFT, 141, 245);
        assertAction(harness.queue.submissions.get(3), 1, InputActionType.CLICK_LEFT, 141, 245);
        assertAction(harness.queue.submissions.get(4), 0, InputActionType.DOUBLE_RIGHT_CLICK, 142, 246);
        InputAction drag = harness.queue.submissions.get(5).get(0);
        assertEquals(InputActionType.DRAG_AND_DROP, drag.getType());
        assertEquals(143, drag.getX());
        assertEquals(247, drag.getY());
        assertEquals(145, drag.getEndX());
        assertEquals(249, drag.getEndY());
        assertEquals(2, harness.queue.submissions.get(6).size());
        assertAction(harness.queue.submissions.get(6), 0, InputActionType.MOVE_MOUSE, 144, 248);
        assertEquals(InputActionType.SCROLL_UP, harness.queue.submissions.get(6).get(1).getType());
        assertEquals(3, harness.queue.submissions.get(6).get(1).getClicks());
        assertEquals(0, harness.keyboard.calls);
        assertTrue(harness.contextHolder.rawCurrent().isEmpty(), "temporary exact-window binding must be restored");
    }

    @Test
    void clickDelayAndQueueHoldStayInsideOneSubmissionForEachSingleClick() {
        Harness harness = harness();
        TurnExecutionWindow window = executionWindow(false);

        assertCompleted(harness.executor.execute(
                window,
                TurnInputAction.CLICK_LEFT,
                new TurnInputSpec(139, 243, null, null, null, null, null, 150, 500),
                1));
        assertCompleted(harness.executor.execute(
                window,
                TurnInputAction.CLICK_RIGHT,
                new TurnInputSpec(140, 244, null, null, null, null, null, 25, 75),
                2));

        assertEquals(2, harness.queue.submissions.size(), "each wire click must submit exactly once");
        List<InputAction> left = harness.queue.submissions.get(0);
        assertEquals(List.of(InputActionType.CLICK_LEFT, InputActionType.SLEEP),
                left.stream().map(InputAction::getType).toList());
        assertEquals(150, left.get(0).getDelayMs());
        assertEquals(500, left.get(1).getDelayMs());
        List<InputAction> right = harness.queue.submissions.get(1);
        assertEquals(List.of(InputActionType.CLICK_RIGHT, InputActionType.SLEEP),
                right.stream().map(InputAction::getType).toList());
        assertEquals(25, right.get(0).getDelayMs());
        assertEquals(75, right.get(1).getDelayMs());
    }

    @Test
    void moveWaitClickSequenceUsesOneQueueSubmissionInExactOrder() {
        Harness harness = harness();

        TurnInputStepExecutor.Result result = harness.executor.executeMouseSequence(
                executionWindow(false),
                List.of(
                        inputStep(0, TurnInputAction.MOVE_MOUSE, point(138, 242)),
                        waitStep(1, 150L),
                        inputStep(2, TurnInputAction.CLICK_LEFT, point(139, 243))));

        assertCompleted(result);
        assertEquals(1, harness.queue.submissions.size());
        List<InputAction> actions = harness.queue.submissions.get(0);
        assertEquals(List.of(InputActionType.MOVE_MOUSE, InputActionType.SLEEP, InputActionType.CLICK_LEFT),
                actions.stream().map(InputAction::getType).toList());
        assertAction(actions, 0, InputActionType.MOVE_MOUSE, 138, 242);
        assertEquals(150, actions.get(1).getDelayMs());
        assertAction(actions, 2, InputActionType.CLICK_LEFT, 139, 243);
        assertTrue(harness.contextHolder.rawCurrent().isEmpty());
    }

    @Test
    void keyTapUsesOnlyTheValidatedBackgroundKeyboardBoundary() {
        Harness harness = harness();
        TurnExecutionWindow window = executionWindow(false);

        TurnInputStepExecutor.Result result = harness.executor.execute(
                window,
                TurnInputAction.KEY_TAP,
                new TurnInputSpec(null, null, null, null, null, "Alt+Q", null),
                6);

        assertCompleted(result);
        assertEquals(0, harness.queue.submissions.size());
        assertEquals(1, harness.keyboard.calls);
        assertEquals(BoundWindowKeyboardService.AltShortcut.ALT_Q, harness.keyboard.lastShortcut);
        assertSame(window.binding(), harness.keyboard.lastBinding);
        assertEquals("window-7", harness.keyboard.lastWindowId);
        assertEquals(0, harness.keyboard.legacyCalls, "turn keyboard must not refresh through the compatibility API");
        assertTrue(harness.contextHolder.rawCurrent().isEmpty());
    }

    @Test
    void altAAndAltCUseTheSameExactHwndShortcutBoundary() {
        Harness harness = harness();
        TurnExecutionWindow window = executionWindow(false);

        assertCompleted(harness.executor.execute(
                window,
                TurnInputAction.KEY_TAP,
                new TurnInputSpec(null, null, null, null, null, "Alt+A", null),
                7));
        assertCompleted(harness.executor.execute(
                window,
                TurnInputAction.KEY_TAP,
                new TurnInputSpec(null, null, null, null, null, "ALT_C", null),
                8));

        assertEquals(List.of(
                        BoundWindowKeyboardService.AltShortcut.ALT_A,
                        BoundWindowKeyboardService.AltShortcut.ALT_C),
                harness.keyboard.shortcuts);
        assertSame(window.binding(), harness.keyboard.lastBinding);
        assertEquals(0, harness.keyboard.legacyCalls);
        assertEquals(0, harness.queue.submissions.size());
    }

    @Test
    void unsupportedKeyboardFormsReturnTypedFailureWithoutAnyForegroundFallback() {
        Harness harness = harness();
        TurnExecutionWindow window = executionWindow(false);

        for (TurnInputAction action : List.of(
                TurnInputAction.KEY_DOWN,
                TurnInputAction.KEY_UP,
                TurnInputAction.TEXT_INPUT)) {
            TurnInputStepExecutor.Result result = harness.executor.execute(
                    window,
                    action,
                    new TurnInputSpec(null, null, null, null, null, "Q", "text"),
                    7);
            assertEquals(TurnInputStepExecutor.Status.FAILED, result.status());
            assertEquals(TurnInputStepExecutor.Code.BACKGROUND_KEY_UNSUPPORTED, result.code());
        }

        TurnInputStepExecutor.Result unvalidated = harness.executor.execute(
                window,
                TurnInputAction.KEY_TAP,
                new TurnInputSpec(null, null, null, null, null, "Alt+Z", null),
                8);

        assertEquals(TurnInputStepExecutor.Status.FAILED, unvalidated.status());
        assertEquals(TurnInputStepExecutor.Code.BACKGROUND_KEY_UNSUPPORTED, unvalidated.code());
        assertEquals(0, harness.queue.submissions.size(), "unsupported keys must not enter the foreground queue");
        assertEquals(0, harness.keyboard.calls, "unvalidated keys must not reach HWND delivery");
    }

    @Test
    void queueAndBackgroundDeliveryFailuresRemainTypedMechanicalResults() {
        Harness harness = harness();
        harness.queue.complete = false;

        TurnInputStepExecutor.Result queueFailure = harness.executor.execute(
                executionWindow(false), TurnInputAction.CLICK_LEFT, point(139, 243), 9);

        assertEquals(TurnInputStepExecutor.Status.FAILED, queueFailure.status());
        assertEquals(TurnInputStepExecutor.Code.INPUT_QUEUE_FAILED, queueFailure.code());

        harness.keyboard.nextAttempt = new BoundWindowKeyboardService.ShortcutAttempt(
                true, false, "post-message-failed", true);
        TurnInputStepExecutor.Result keyboardFailure = harness.executor.execute(
                executionWindow(false),
                TurnInputAction.KEY_TAP,
                new TurnInputSpec(null, null, null, null, null, "Alt+Q", null),
                10);

        assertEquals(TurnInputStepExecutor.Status.FAILED, keyboardFailure.status());
        assertEquals(TurnInputStepExecutor.Code.BACKGROUND_KEY_FAILED, keyboardFailure.code());
        assertEquals("post-message-failed", keyboardFailure.detail());
    }

    @Test
    void stopAndInvalidWaitShortCircuitWithoutCreatingInput() {
        Harness harness = harness();

        TurnInputStepExecutor.Result stopped = harness.executor.execute(
                executionWindow(true), TurnInputAction.CLICK_LEFT, point(139, 243), 11);
        TurnInputStepExecutor.Result invalidWait = harness.executor.waitFor(0);

        assertEquals(TurnInputStepExecutor.Status.STOPPED, stopped.status());
        assertEquals(TurnInputStepExecutor.Code.STOPPED, stopped.code());
        assertEquals(TurnInputStepExecutor.Status.FAILED, invalidWait.status());
        assertEquals(TurnInputStepExecutor.Code.INVALID_INPUT, invalidWait.code());
        assertEquals(0, harness.queue.submissions.size());
        assertEquals(0, harness.keyboard.calls);
    }

    @Test
    void positiveAndInterruptedWaitRemainTypedAndNeverCreateInput() throws Exception {
        Harness harness = harness();

        TurnInputStepExecutor.Result completed = harness.executor.waitFor(1L);
        AtomicReference<TurnInputStepExecutor.Result> interruptedResult = new AtomicReference<>();
        AtomicBoolean interruptPreserved = new AtomicBoolean();
        Thread waiter = new Thread(() -> {
            Thread.currentThread().interrupt();
            interruptedResult.set(harness.executor.waitFor(10_000L));
            interruptPreserved.set(Thread.currentThread().isInterrupted());
        }, "turn-input-wait-interrupt-contract");
        waiter.start();
        waiter.join(3_000L);

        assertCompleted(completed);
        assertFalse(waiter.isAlive(), "a pre-interrupted wait must stop deterministically");
        TurnInputStepExecutor.Result stopped = interruptedResult.get();
        assertNotNull(stopped);
        assertEquals(TurnInputStepExecutor.Status.STOPPED, stopped.status());
        assertEquals(TurnInputStepExecutor.Code.STOPPED, stopped.code());
        assertTrue(interruptPreserved.get(), "TaskSleep must preserve the isolated waiter's interrupt flag");
        assertEquals(0, harness.queue.submissions.size());
        assertEquals(0, harness.keyboard.calls);
    }

    /**
     * TURN-22D1 seam: the executor now submits through the reviewed frozen exact-window boundary instead of
     * the legacy queue, which re-resolved the window from the thread-local context and refreshed the binding a
     * second time. This asserts what the boundary actually received, and it starts from a DIFFERENT sentinel
     * context on purpose: an empty-holder-to-empty-holder check would pass even if the executor never
     * established the action's exact window at all.
     */
    @Test
    void frozenSubmissionCarriesTheExactWindowAndRestoresTheCallersSentinelContext() {
        Harness harness = harness();
        TurnExecutionWindow window = executionWindow(false);
        WindowRuntimeContext sentinel = sentinelContext();
        WindowNativeBinding sentinelBinding = sentinel.getNativeBinding();
        long sentinelEpoch = sentinel.getPlayerIdentityEpoch();

        harness.contextHolder.callWith(sentinel, () -> {
            assertCompleted(harness.executor.execute(
                    window,
                    TurnInputAction.CLICK_LEFT,
                    new TurnInputSpec(139, 243, null, null, null, null, null, 150, 500),
                    1));

            // Still inside the caller's own scope: the executor must have put the sentinel back already.
            WindowRuntimeContext restored = harness.contextHolder.rawCurrent().orElse(null);
            assertSame(sentinel, restored, "the caller's bound window must be restored, not left on the action's");
            assertSame(sentinelBinding, restored.getNativeBinding(), "the sentinel binding must be untouched");
            assertEquals("window-sentinel", restored.getWindowId());
            assertEquals(sentinelEpoch, restored.getPlayerIdentityEpoch());
            return null;
        });

        assertTrue(harness.contextHolder.rawCurrent().isEmpty(),
                "leaving the caller's scope must leave no bound window behind");
        assertEquals(1, harness.queue.submissions.size(), "one wire click must be exactly one frozen submission");
        assertSame(window.context(), harness.queue.frozenContexts.get(0),
                "the boundary must receive the action resolver's exact context, never a re-resolved one");
        assertSame(window.binding(), harness.queue.frozenBindings.get(0),
                "the boundary must receive the exact frozen binding object, never a re-read one");

        ObservedWindow observed = harness.queue.observedWhileSubmitting.get(0);
        assertNotNull(observed, "the action's exact window must be bound for the duration of the submission");
        assertEquals("window-7", observed.windowId());
        assertEquals("12345", observed.nativeHandle());
        assertEquals(88L, observed.processId());
        assertEquals(137, observed.x());
        assertEquals(241, observed.y());
        assertEquals(10, observed.width());
        assertEquals(10, observed.height());
        assertEquals(window.context().getPlayerIdentityEpoch(), observed.playerIdentityEpoch());

        List<InputAction> actions = harness.queue.submissions.get(0);
        assertEquals(List.of(InputActionType.CLICK_LEFT, InputActionType.SLEEP),
                actions.stream().map(InputAction::getType).toList(),
                "the 696a12b0 order is one click then the queue hold, inside the one frozen request");
        assertEquals(150, actions.get(0).getDelayMs());
        assertEquals(500, actions.get(1).getDelayMs());
    }

    /**
     * TURN-22D1 seam: a typed {@code STOP_REQUESTED} from the frozen boundary is a stop, not a queue failure.
     * The old boolean path could only guess from the calling thread's interrupt flag, which a worker-side stop
     * never sets, so a real stop would have been misreported as {@code INPUT_QUEUE_FAILED}.
     */
    @Test
    void typedStopFromTheFrozenBoundaryMapsToStoppedRatherThanQueueFailure() {
        Harness harness = harness();
        harness.queue.complete = false;
        harness.queue.safetyReason = InputActionSafetyReason.STOP_REQUESTED;

        TurnInputStepExecutor.Result result = harness.executor.execute(
                executionWindow(false),
                TurnInputAction.CLICK_LEFT,
                new TurnInputSpec(139, 243, null, null, null, null, null, 150, 500),
                1);

        assertEquals(TurnInputStepExecutor.Status.STOPPED, result.status());
        assertEquals(TurnInputStepExecutor.Code.STOPPED, result.code());
        assertFalse(Thread.currentThread().isInterrupted(),
                "the stop must come from the typed result, not from an interrupt on this thread");
        assertEquals(1, harness.queue.submissions.size(), "a stop must not be retried or replayed");
    }

    /**
     * TURN-22D1 seam: drift rejection is the whole reason for the frozen boundary, so it is proven against the
     * real production queue rather than a stub that could never reject anything. An {@code A -> B -> A} rebind
     * restores every field, so only the published binding object reveals it. The rejection is synchronous and
     * pre-enqueue, so no worker and no timing are involved.
     */
    @Test
    void valueEqualRebindDriftIsTypedFailureAndNeverEntersTheInputQueue() {
        WindowTaskContextHolder contextHolder = new WindowTaskContextHolder(new WindowIsolationProperties());
        InputActionQueue realQueue = new InputActionQueue(
                contextHolder,
                new WindowNativeBindingRefreshService(),
                new TaskExecutionContextHolder());
        TurnInputStepExecutor executor = new TurnInputStepExecutor(
                realQueue,
                new RecordingKeyboardService(),
                contextHolder,
                new TurnInputActionMapper(),
                new TurnKeyMapper());
        TurnExecutionWindow window = executionWindow(false);
        WindowNativeBinding frozen = window.binding();

        // A -> B, then back to an A' that is field-for-field identical to A but a different object.
        window.context().setNativeBinding(frozen.withGeometry(
                frozen.getX() + 7, frozen.getY(), frozen.getWidth(), frozen.getHeight()));
        window.context().setNativeBinding(frozen.withGeometry(
                frozen.getX(), frozen.getY(), frozen.getWidth(), frozen.getHeight()));
        WindowNativeBinding restored = window.context().getNativeBinding();
        assertNotSame(frozen, restored, "the A->B->A rebind must publish a different binding object");
        assertEquals(frozen.getNativeHandle(), restored.getNativeHandle());
        assertEquals(frozen.getProcessId(), restored.getProcessId());
        assertEquals(frozen.getX(), restored.getX());
        assertEquals(frozen.getY(), restored.getY());

        TurnInputStepExecutor.Result result = executor.execute(
                window,
                TurnInputAction.CLICK_LEFT,
                new TurnInputSpec(139, 243, null, null, null, null, null, 150, 500),
                1);

        assertEquals(TurnInputStepExecutor.Status.FAILED, result.status(),
                "a stale generation must never be reported as completed");
        assertEquals(TurnInputStepExecutor.Code.INPUT_QUEUE_FAILED, result.code());
        assertEquals(0, realQueue.size(),
                "the stale list must be rejected before enqueue, so no input can ever be dispatched for it");
        assertTrue(contextHolder.rawCurrent().isEmpty(),
                "a rejected submission must still restore the caller's bound window");
    }

    private static WindowRuntimeContext sentinelContext() {
        WindowRuntimeContext sentinel = new WindowRuntimeContext("window-sentinel", new GameContext());
        sentinel.setNativeBinding(new WindowNativeBinding(
                "99999", "game-window-sentinel", "GameWindow", 4242L, 900, 800, 20, 30));
        return sentinel;
    }

    private static Harness harness() {
        RecordingInputQueue queue = new RecordingInputQueue();
        RecordingKeyboardService keyboard = new RecordingKeyboardService();
        WindowTaskContextHolder contextHolder = new WindowTaskContextHolder(new WindowIsolationProperties());
        queue.contextHolder = contextHolder;
        TurnInputStepExecutor executor = new TurnInputStepExecutor(
                queue,
                keyboard,
                contextHolder,
                new TurnInputActionMapper(),
                new TurnKeyMapper());
        return new Harness(executor, queue, keyboard, contextHolder);
    }

    private static TurnInputSpec point(int x, int y) {
        return new TurnInputSpec(x, y, null, null, null, null, null);
    }

    private static TurnStep inputStep(int index, TurnInputAction action, TurnInputSpec input) {
        return new TurnStep(index, TurnStepType.INPUT, action, input, null, null, null, null);
    }

    private static TurnStep waitStep(int index, long waitMs) {
        return new TurnStep(index, TurnStepType.WAIT, null, null, waitMs, null, null, null);
    }

    private static void assertCompleted(TurnInputStepExecutor.Result result) {
        assertEquals(TurnInputStepExecutor.Status.COMPLETED, result.status());
        assertEquals(TurnInputStepExecutor.Code.OK, result.code());
    }

    private static void assertAction(List<InputAction> actions,
                                     int index,
                                     InputActionType type,
                                     int x,
                                     int y) {
        InputAction action = actions.get(index);
        assertEquals(type, action.getType());
        assertEquals(x, action.getX());
        assertEquals(y, action.getY());
    }

    /**
     * Resolves the window through the production public seam {@link TurnExecutionWindow#resolveForAction},
     * backed by an all-memory scripted task manager/runner and binding refresh service.
     *
     * <p>The previous fixture reached the private {@code TurnExecutionWindow} constructor with
     * {@code setAccessible(true)}. That hand-built a window that the production resolver would never produce
     * and skipped the very refresh/generation behavior this integration test exists to exercise. Now the
     * scripted refresh service commits the binding onto the context exactly as production does, so the context
     * publishes the object that later serves as the frozen boundary's generation witness, and stop is
     * expressed through the real {@code WindowRuntimeStatus} the resolver reads.</p>
     */
    private static TurnExecutionWindow executionWindow(boolean stopRequested) {
        WindowRuntimeContext context = new WindowRuntimeContext("window-7", new GameContext());
        if (stopRequested) {
            context.setStatus(WindowRuntimeStatus.STOPPING);
        }
        BareWindowTaskRunner runner = allocate(BareWindowTaskRunner.class);
        runner.initialize(context);
        ScriptedRefreshService refresh = new ScriptedRefreshService(new WindowNativeBinding(
                "12345", "game-window-7", "GameWindow", 88L, 137, 241, 10, 10));
        TurnAction action = new TurnAction(
                1,
                "b7d1f0c2-9f3a-4a6b-8f21-6d2c4e5a7b90",
                "device-1",
                "window-7",
                List.of(new TurnStep(0, TurnStepType.WAIT, null, null, 1L, null, null, null)),
                false);
        return TurnExecutionWindow.resolveForAction(action, new TestTaskManager(runner), refresh);
    }

    /** In-memory binding source: commits the scripted binding exactly as the production refresh does. */
    private static final class ScriptedRefreshService extends WindowNativeBindingRefreshService {
        private final WindowNativeBinding binding;
        private int calls;

        private ScriptedRefreshService(WindowNativeBinding binding) {
            this.binding = binding;
        }

        @Override
        public Optional<WindowNativeBinding> refreshAndCommit(WindowRuntimeContext context) {
            calls++;
            if (context == null || binding == null) {
                return Optional.empty();
            }
            context.setNativeBinding(binding);
            return Optional.of(context.getNativeBinding());
        }
    }

    /** In-memory registry returning only the scripted runner for its own window id. */
    private static final class TestTaskManager extends MultiWindowTaskManager {
        private final WindowTaskRunner runner;

        private TestTaskManager(WindowTaskRunner runner) {
            super(
                    null, null, null, null, null, null, null, null, null, null, null,
                    null, null, null, null, null, null, null, null, null, List.of(), null);
            this.runner = runner;
        }

        @Override
        public Optional<WindowTaskRunner> getRunner(String windowId) {
            return runner != null && runner.getWindowContext().getWindowId().equals(windowId)
                    ? Optional.of(runner)
                    : Optional.empty();
        }
    }

    /**
     * Inert scripted runner. {@link WindowTaskRunner}'s only constructor requires eighteen non-null production
     * collaborators, so the instance is allocated without running a constructor; this test class is the thing
     * being allocated, and no private member of production code is read or written.
     */
    private static final class BareWindowTaskRunner extends WindowTaskRunner {
        private WindowRuntimeContext context;

        private BareWindowTaskRunner() {
            super(
                    null, null, null, null, null, null, null, null, null, null,
                    null, null, null, null, null, null, null, null, List.of(), null);
        }

        private void initialize(WindowRuntimeContext context) {
            this.context = context;
        }

        @Override
        public WindowRuntimeContext getWindowContext() {
            return context;
        }

        @Override
        public RunningTaskHandle getCurrentTask() {
            return null;
        }
    }

    private static <T> T allocate(Class<T> type) {
        try {
            return type.cast(UNSAFE.allocateInstance(type));
        } catch (InstantiationException failure) {
            throw new AssertionError("cannot allocate inert test double " + type.getName(), failure);
        }
    }

    private static Unsafe findUnsafe() {
        try {
            Field field = Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            return (Unsafe) field.get(null);
        } catch (ReflectiveOperationException failure) {
            throw new ExceptionInInitializerError(failure);
        }
    }

    private record Harness(TurnInputStepExecutor executor,
                           RecordingInputQueue queue,
                           RecordingKeyboardService keyboard,
                           WindowTaskContextHolder contextHolder) {
    }

    /**
     * Records what the executor handed to the reviewed frozen exact-window boundary, and what the bound
     * window looked like at that exact moment.
     *
     * <p>Only {@code submitFrozenExactWindowActionsAndWait} is overridden: if the executor ever regressed to
     * the legacy {@code submitAndWait} path, the production method would run against this queue's null
     * collaborators instead of silently recording a submission, so the regression cannot pass unnoticed.</p>
     */
    private static final class RecordingInputQueue extends InputActionQueue {
        private final List<List<InputAction>> submissions = new ArrayList<>();
        private final List<String> descriptions = new ArrayList<>();
        private final List<WindowRuntimeContext> frozenContexts = new ArrayList<>();
        private final List<WindowNativeBinding> frozenBindings = new ArrayList<>();
        private final List<ObservedWindow> observedWhileSubmitting = new ArrayList<>();
        private WindowTaskContextHolder contextHolder;
        private boolean complete = true;
        private InputActionSafetyReason safetyReason = InputActionSafetyReason.CLEAR;

        private RecordingInputQueue() {
            super(null, null, null);
        }

        @Override
        public InputActionExecutionResult submitFrozenExactWindowActionsAndWait(
                String description,
                WindowRuntimeContext context,
                WindowNativeBinding binding,
                List<InputAction> actions) {
            descriptions.add(description);
            frozenContexts.add(context);
            frozenBindings.add(binding);
            submissions.add(List.copyOf(actions));
            observedWhileSubmitting.add(ObservedWindow.of(contextHolder));
            return InputActionExecutionResult.builder()
                    .requestId("recorded-" + submissions.size())
                    .started(complete)
                    .startedStepIndex(complete ? 0 : -1)
                    .lastCompletedStepIndex(complete ? actions.size() - 1 : -1)
                    .status(complete
                            ? InputActionExecutionResult.Status.COMPLETED
                            : InputActionExecutionResult.Status.NOT_STARTED)
                    .safetyReason(safetyReason)
                    .reason(complete ? "completed" : "recorded-not-completed")
                    .build();
        }
    }

    /** The exact bound-window identity sampled from inside one submission. */
    private record ObservedWindow(String windowId,
                                  String nativeHandle,
                                  long processId,
                                  int x,
                                  int y,
                                  int width,
                                  int height,
                                  long playerIdentityEpoch) {

        private static ObservedWindow of(WindowTaskContextHolder holder) {
            WindowRuntimeContext context = holder.rawCurrent().orElse(null);
            if (context == null) {
                return null;
            }
            WindowNativeBinding binding = context.getNativeBinding();
            return new ObservedWindow(
                    context.getWindowId(),
                    binding == null ? null : binding.getNativeHandle(),
                    binding == null ? -1L : binding.getProcessId(),
                    binding == null ? -1 : binding.getX(),
                    binding == null ? -1 : binding.getY(),
                    binding == null ? -1 : binding.getWidth(),
                    binding == null ? -1 : binding.getHeight(),
                    context.getPlayerIdentityEpoch());
        }
    }

    private static final class RecordingKeyboardService extends BoundWindowKeyboardService {
        private int calls;
        private int legacyCalls;
        private AltShortcut lastShortcut;
        private WindowNativeBinding lastBinding;
        private String lastWindowId;
        private final List<AltShortcut> shortcuts = new ArrayList<>();
        private ShortcutAttempt nextAttempt = new ShortcutAttempt(true, true, null, false);

        private RecordingKeyboardService() {
            super(null, null, null, null);
        }

        @Override
        public ShortcutAttempt pressShortcut(AltShortcut shortcut) {
            legacyCalls++;
            return nextAttempt;
        }

        @Override
        public ShortcutAttempt pressShortcut(WindowNativeBinding binding,
                                              String windowId,
                                              AltShortcut shortcut) {
            calls++;
            lastShortcut = shortcut;
            lastBinding = binding;
            lastWindowId = windowId;
            shortcuts.add(shortcut);
            return nextAttempt;
        }
    }
}
