package com.bot.dhxy.service;

import com.bot.dhxy.input.InputSequences;
import com.bot.dhxy.runner.context.TaskExecutionContext;
import com.bot.dhxy.runner.stop.TaskStopRequestedException;
import com.bot.dhxy.runner.stop.TaskStopToken;
import org.junit.jupiter.api.Test;

import java.awt.Point;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * TURN-40B-C2: {@link BagService#withMainBagOpenGuarded} evaluates the live identity predicate and
 * the captured stop token as the FIRST exclusive-callback action, before {@code ensureBagOpened}
 * and before any physical input, and keeps four outcomes distinct:
 *
 * <ul>
 *   <li>identity replacement → typed stop, zero input (the caller operation never runs);</li>
 *   <li>a captured token stop → typed stop, zero input;</li>
 *   <li>an ordinary queue failure with an unstopped token → generic null (not a stop);</li>
 *   <li>an admitted session that cannot open the bag → generic null (not a stop);</li>
 *   <li>successful admission opens and closes the main bag exactly once around the caller operation.</li>
 * </ul>
 *
 * <p>The rejection paths return before {@code ensureBagOpened}, so a scripted {@link InputSequences}
 * fully drives them without any real bag mechanics, native window, or input. TURN-40B-C2 Amendment #1
 * widened {@code ensureBagOpened}/{@code closeBagIfNeeded}/{@code countItemUpToInOpenMainBag} to
 * {@code protected} (bodies unchanged), so the admitted bag-open-failure and success open/close-once
 * outcomes are proven directly here through the real inherited guarded/exclusive session path with
 * only those seams overridden — no live bag mechanics, window, or input.</p>
 */
class BagServiceGuardedAdmissionTest {

    @Test
    void identityReplacementYieldsTypedStopWithZeroInput() {
        AtomicBoolean operationRan = new AtomicBoolean(false);
        BagService bagService = bagServiceWith(new CallbackRunningInputSequences());

        // admission=false models the runner replacing/clearing the action-owning handle after resolve.
        assertThrows(TaskStopRequestedException.class, () -> bagService.withMainBagOpenGuarded(
                "wuhuan-v2:prepare-supplies", () -> false, new TaskStopToken(),
                mainBag -> { operationRan.set(true); return Boolean.TRUE; }));

        assertFalse(operationRan.get(),
                "a rejected admission never runs the caller operation, so no bag is opened or clicked");
    }

    @Test
    void capturedTokenStopYieldsTypedStopWithZeroInput() {
        AtomicBoolean operationRan = new AtomicBoolean(false);
        BagService bagService = bagServiceWith(new CallbackRunningInputSequences());
        TaskStopToken stopToken = new TaskStopToken();
        stopToken.requestStop("stop before admission");

        assertThrows(TaskStopRequestedException.class, () -> bagService.withMainBagOpenGuarded(
                "wuhuan-v2:prepare-supplies", () -> true, stopToken,
                mainBag -> { operationRan.set(true); return Boolean.TRUE; }));

        assertFalse(operationRan.get(), "a stopped captured token rejects before any input");
    }

    @Test
    void ordinaryQueueFailureWithAnUnstoppedTokenStaysGenericNull() {
        AtomicBoolean operationRan = new AtomicBoolean(false);
        // The queue never runs the callback and the token is not stopped: an ordinary queue failure.
        BagService bagService = bagServiceWith(new QueueFailingInputSequences());

        Object result = bagService.withMainBagOpenGuarded(
                "wuhuan-v2:prepare-supplies", () -> true, new TaskStopToken(),
                mainBag -> { operationRan.set(true); return Boolean.TRUE; });

        assertNull(result, "an ordinary queue failure returns generic null, never a typed stop");
        assertFalse(operationRan.get());
    }

    @Test
    void aStopRequestedDuringTheQueueWaitStillConvertsToTypedStop() {
        // The queue returns false and the captured token was stopped during the wait (no callback
        // flag): the post-wait re-check converts it to the typed stop.
        TaskStopToken stopToken = new TaskStopToken();
        BagService bagService = bagServiceWith(new QueueFailingInputSequences(stopToken));

        assertThrows(TaskStopRequestedException.class, () -> bagService.withMainBagOpenGuarded(
                "wuhuan-v2:prepare-supplies", () -> true, stopToken,
                mainBag -> Boolean.TRUE));
    }

    @Test
    void ordinaryBagOpenFailureStaysNonStoppedGenericNull() {
        // Admission passes and the token is unstopped, but the admitted exclusive session cannot open
        // the bag (null anchor). That is an ordinary failure: generic null, never a typed stop.
        AtomicBoolean operationRan = new AtomicBoolean(false);
        OpenTrackingBagService bagService =
                new OpenTrackingBagService(new CallbackRunningInputSequences(), null);

        Object result = bagService.withMainBagOpenGuarded(
                "wuhuan-v2:prepare-supplies", () -> true, new TaskStopToken(),
                mainBag -> { operationRan.set(true); return Boolean.TRUE; });

        assertNull(result, "an admitted session that cannot open the bag returns generic null, not a stop");
        assertEquals(1, bagService.openCalls, "the admitted session tries to open the bag exactly once");
        assertEquals(0, bagService.closeCalls, "a bag that never opened is not closed");
        assertFalse(operationRan.get(), "the caller operation never runs when the bag cannot be opened");
    }

    @Test
    void successfulAdmissionOpensAndClosesTheBagExactlyOnce() {
        // Admission passes, the token is unstopped, and the bag opens: the caller operation runs once
        // inside a single open/close of the real inherited exclusive session.
        AtomicInteger operationRuns = new AtomicInteger();
        OpenTrackingBagService bagService =
                new OpenTrackingBagService(new CallbackRunningInputSequences(), new Point(120, 240));

        String result = bagService.withMainBagOpenGuarded(
                "wuhuan-v2:prepare-supplies", () -> true, new TaskStopToken(),
                mainBag -> { operationRuns.incrementAndGet(); return "ok"; });

        assertEquals("ok", result, "a successful session returns the caller operation result");
        assertEquals(1, operationRuns.get(), "the caller operation runs exactly once");
        assertEquals(1, bagService.openCalls, "the bag opens exactly once");
        assertEquals(1, bagService.closeCalls, "the bag closes exactly once");
    }

    private static BagService bagServiceWith(InputSequences inputSequences) {
        return new BagService(inputSequences, null, null, null, null, null, null, null);
    }

    /**
     * Overrides only the TURN-40B-C2 Amendment #1 protected open/close seams (bodies elsewhere
     * unchanged) so the admitted exclusive session runs with a deterministic anchor and no live bag
     * mechanics; a null anchor models an ordinary bag-open failure.
     */
    private static final class OpenTrackingBagService extends BagService {
        private final Point openAnchor;
        private int openCalls;
        private int closeCalls;

        private OpenTrackingBagService(InputSequences inputSequences, Point openAnchor) {
            super(inputSequences, null, null, null, null, null, null, null);
            this.openAnchor = openAnchor;
        }

        @Override
        protected Point ensureBagOpened(BagLayout layout, TaskExecutionContext context) {
            openCalls++;
            return openAnchor;
        }

        @Override
        protected void closeBagIfNeeded(BagLayout layout, TaskExecutionContext context) {
            closeCalls++;
        }
    }

    /** Runs the exclusive callback and returns its boolean, exactly like the real queue on success. */
    private static final class CallbackRunningInputSequences extends InputSequences {
        private CallbackRunningInputSequences() {
            super(null);
        }

        @Override
        public boolean submitExclusiveAndWait(String description, Supplier<Boolean> callback) {
            return callback.get();
        }
    }

    /** Never runs the callback and returns false, modelling an ordinary queue admission failure. */
    private static final class QueueFailingInputSequences extends InputSequences {
        private final TaskStopToken stopDuringWait;

        private QueueFailingInputSequences() {
            this(null);
        }

        private QueueFailingInputSequences(TaskStopToken stopDuringWait) {
            super(null);
            this.stopDuringWait = stopDuringWait;
        }

        @Override
        public boolean submitExclusiveAndWait(String description, Supplier<Boolean> callback) {
            if (stopDuringWait != null) {
                stopDuringWait.requestStop("stop raised during the queue wait");
            }
            return false;
        }
    }
}
