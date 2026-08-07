package com.bot.dhxy.cloud.turn.local;

import com.bot.dhxy.cloud.turn.LocalServiceExecution;
import com.bot.dhxy.cloud.turn.protocol.TurnLocalOperation;
import com.bot.dhxy.cloud.turn.protocol.TurnLocalServiceCall;
import com.bot.dhxy.cloud.turn.protocol.TurnXinshouTrackerChainArguments;
import com.bot.dhxy.cloud.turn.protocol.TurnXinshouTrackerLink;
import com.bot.dhxy.input.InputProvider;
import com.bot.dhxy.input.InputSequences;
import com.bot.dhxy.input.action.InputActionExecutionResult;
import com.bot.dhxy.input.action.InputActionSafetyReason;
import com.bot.dhxy.input.action.InputActionScope;
import com.bot.dhxy.runner.stop.TaskSleep;
import com.bot.dhxy.runner.stop.TaskStopRequestedException;
import com.bot.dhxy.window.model.WindowPathingIntent;
import com.bot.dhxy.window.model.WindowPathingIntentType;
import com.bot.dhxy.window.model.WindowNativeBinding;
import com.bot.dhxy.window.runtime.WindowRuntimeContext;
import com.bot.dhxy.window.runtime.WindowTaskContextHolder;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

/** Executes one Cloud-authorized Tracker link click against the currently bound window. */
@Component
public final class XinshouTrackerLinkChainLocalOperationExecutor {

    private static final int CLICK_SETTLE_MS = 80;
    private static final int CLICK_DELAY_MS = 250;

    private final InputProvider inputProvider;
    private final InputSequences inputSequences;
    private final WindowTaskContextHolder contextHolder;

    public XinshouTrackerLinkChainLocalOperationExecutor(InputProvider inputProvider,
                                                          InputSequences inputSequences,
                                                          WindowTaskContextHolder contextHolder) {
        this.inputProvider = Objects.requireNonNull(inputProvider, "inputProvider");
        this.inputSequences = Objects.requireNonNull(inputSequences, "inputSequences");
        this.contextHolder = Objects.requireNonNull(contextHolder, "contextHolder");
    }

    /**
     * Execute exactly one parser-selected link from one Cloud PreparedAction.
     *
     * <p>The move and click stay in one input-queue request. This executor never retries, waits for
     * a guessed business effect, or creates a second click on its own. The Runner observes all
     * resulting Tracker/Dialog/pathing changes and Cloud alone decides whether another action is
     * warranted.</p>
     *
     * @param call Cloud-authorized local-service call containing one screen-absolute click point
     * @return completed only when that exact atomic input request succeeds; otherwise failed
     */
    public LocalServiceExecution execute(TurnLocalServiceCall call) {
        TurnXinshouTrackerChainArguments chain = call == null ? null : call.xinshouTrackerChain();
        if (call == null || call.operation() != TurnLocalOperation.XINSHOU_TRACKER_LINK_CHAIN
                || chain == null || chain.source() == null || chain.source().isBlank()
                || chain.links().size() != 1) {
            return LocalServiceExecution.failed("INVALID_XINSHOU_TRACKER_CHAIN", null);
        }
        WindowRuntimeContext context = contextHolder.rawCurrent().orElse(null);
        WindowNativeBinding binding = context == null ? null : context.getNativeBinding();
        if (binding == null || !binding.hasNativeHandle() || !binding.hasGeometry()) {
            return LocalServiceExecution.failed("XINSHOU_TRACKER_CHAIN_WINDOW_UNAVAILABLE", null);
        }
        TurnXinshouTrackerLink link = chain.links().get(0);
        if (link == null
                || chain.sourceWindowWidth() <= 0
                || chain.sourceWindowHeight() <= 0
                || link.x() < chain.sourceWindowLeft()
                || link.y() < chain.sourceWindowTop()
                || link.x() >= (long) chain.sourceWindowLeft() + chain.sourceWindowWidth()
                || link.y() >= (long) chain.sourceWindowTop() + chain.sourceWindowHeight()) {
            return LocalServiceExecution.failed("INVALID_XINSHOU_TRACKER_CHAIN", null);
        }
        String pathingSource = chain.source() + ":cloud-authorized";
        AtomicReference<LocalServiceExecution> callbackResult = new AtomicReference<>();
        InputActionExecutionResult terminal;
        try {
            terminal = inputSequences.submitFrozenExactWindowExclusiveAndWait(
                    "xinshou:tracker-link",
                    context,
                    binding,
                    () -> {
                        /*
                         * Translation and direct input belong to the same frozen generation witnessed by
                         * the worker. No queue call is allowed inside this callback.
                         */
                        if (binding.getWidth() != chain.sourceWindowWidth()
                                || binding.getHeight() != chain.sourceWindowHeight()) {
                            callbackResult.set(LocalServiceExecution.failed(
                                    "XINSHOU_TRACKER_CHAIN_WINDOW_SIZE_CHANGED", null));
                            return true;
                        }
                        long translatedX = (long) link.x()
                                + binding.getX() - chain.sourceWindowLeft();
                        long translatedY = (long) link.y()
                                + binding.getY() - chain.sourceWindowTop();
                        if (translatedX < binding.getX()
                                || translatedY < binding.getY()
                                || translatedX >= (long) binding.getX() + binding.getWidth()
                                || translatedY >= (long) binding.getY() + binding.getHeight()
                                || translatedX < Integer.MIN_VALUE
                                || translatedY < Integer.MIN_VALUE
                                || translatedX > Integer.MAX_VALUE
                                || translatedY > Integer.MAX_VALUE) {
                            callbackResult.set(LocalServiceExecution.failed(
                                    "XINSHOU_TRACKER_CHAIN_TRANSLATED_POINT_OUTSIDE_WINDOW", null));
                            return true;
                        }
                        try {
                            if (!InputActionScope.checkpoint()) {
                                return false;
                            }
                            inputProvider.moveMouse((int) translatedX, (int) translatedY);
                            if (!TaskSleep.sleep(CLICK_SETTLE_MS)
                                    || !InputActionScope.checkpoint()) {
                                return false;
                            }
                            inputProvider.clickLeft(
                                    (int) translatedX, (int) translatedY, CLICK_DELAY_MS);
                            /*
                             * The provider has completed the real click, and the worker still holds the
                             * exact context generation monitor. Register the intent here so queue wait is
                             * excluded from its age and a newer binding can never receive this old click.
                             */
                            context.markPathingStarted(WindowPathingIntent.builder()
                                    .source(pathingSource)
                                    .intentId(UUID.randomUUID().toString())
                                    .type(WindowPathingIntentType.UNTARGETED_TRACKER)
                                    .createdAtMs(System.currentTimeMillis())
                                    .build());
                            callbackResult.set(LocalServiceExecution.completed(
                                    "XINSHOU_TRACKER_LINK_DISPATCHED", null, null));
                            return true;
                        } catch (TaskStopRequestedException stopped) {
                            throw stopped;
                        } catch (RuntimeException inputFailure) {
                            callbackResult.set(LocalServiceExecution.failed(
                                    "XINSHOU_TRACKER_LINK_INPUT_FAILED", null));
                            return true;
                        }
                    });
        } catch (RuntimeException submissionFailure) {
            return LocalServiceExecution.failed("XINSHOU_TRACKER_LINK_INPUT_FAILED", null);
        }
        if (terminal == null) {
            return LocalServiceExecution.failed("XINSHOU_TRACKER_LINK_INPUT_FAILED", null);
        }
        if (!terminal.isCompleted()) {
            if (terminal.getSafetyReason() == InputActionSafetyReason.STOP_REQUESTED) {
                return LocalServiceExecution.stopped(null);
            }
            if (terminal.getSafetyReason() == InputActionSafetyReason.WINDOW_BINDING_CHANGED
                    || terminal.getSafetyReason() == InputActionSafetyReason.TASK_RUN_MISMATCH) {
                return LocalServiceExecution.failed("XINSHOU_TRACKER_LINK_STALE", null);
            }
            return LocalServiceExecution.failed("XINSHOU_TRACKER_LINK_INPUT_FAILED", null);
        }
        LocalServiceExecution exactResult = callbackResult.get();
        if (exactResult == null) {
            return LocalServiceExecution.failed("XINSHOU_TRACKER_LINK_CALLBACK_MISSING", null);
        }
        if (exactResult.status()
                != com.bot.dhxy.cloud.turn.protocol.TurnStepResult.Status.COMPLETED) {
            return exactResult;
        }
        return exactResult;
    }
}
