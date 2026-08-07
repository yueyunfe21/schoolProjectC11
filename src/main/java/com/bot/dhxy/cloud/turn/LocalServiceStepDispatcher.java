package com.bot.dhxy.cloud.turn;

import com.bot.dhxy.cloud.turn.local.BagLocalOperationExecutor;
import com.bot.dhxy.cloud.turn.local.GiveItemLocalOperationExecutor;
import com.bot.dhxy.cloud.turn.local.MetricsLocalOperationExecutor;
import com.bot.dhxy.cloud.turn.local.MapSurveyPointerLocalOperationExecutor;
import com.bot.dhxy.cloud.turn.local.LeftTopStatusLocalOperationExecutor;
import com.bot.dhxy.cloud.turn.local.QuestLocalOperationExecutor;
import com.bot.dhxy.cloud.turn.local.TaskTrackerLocalOperationExecutor;
import com.bot.dhxy.cloud.turn.local.UiLocalOperationExecutor;
import com.bot.dhxy.cloud.turn.local.WholeTaskRuntimeLocalOperationExecutor;
import com.bot.dhxy.cloud.turn.local.TurnContinuationGateway;
import com.bot.dhxy.cloud.turn.local.XinshouCombatLocalMechanics;
import com.bot.dhxy.cloud.turn.local.XinshouTitleMechanicalExecutor;
import com.bot.dhxy.cloud.turn.local.host.HostLocalOperationExecutor;
import com.bot.dhxy.cloud.turn.protocol.TurnLocalServiceCall;
import com.bot.dhxy.cloud.turn.protocol.TurnXinshouMechanicalAction;
import com.bot.dhxy.cloud.turn.protocol.TurnXinshouMechanicalArguments;
import com.bot.dhxy.input.InputProvider;
import com.bot.dhxy.input.InputSequences;
import com.bot.dhxy.input.action.InputActionExecutionResult;
import com.bot.dhxy.input.action.InputActionSafetyReason;
import com.bot.dhxy.input.action.InputActionScope;
import com.bot.dhxy.runner.stop.TaskSleep;
import com.bot.dhxy.runner.stop.TaskPauseToken;
import com.bot.dhxy.runner.stop.TaskStopRequestedException;
import com.bot.dhxy.runner.stop.TaskStopToken;
import com.bot.dhxy.window.observation.XinshouRecoveryLocalMechanics;
import com.bot.dhxy.window.observation.XinshouRunnerAutoCombatState;
import com.bot.dhxy.window.model.WindowNativeBinding;
import com.bot.dhxy.window.runtime.WindowRuntimeContext;
import com.bot.dhxy.window.runtime.WindowTaskContextHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;

/** Routes each closed local-Service operation through its approved queue ownership boundary. */
@Component
public final class LocalServiceStepDispatcher {

    private final BagLocalOperationExecutor bagAdapter;
    private final UiLocalOperationExecutor uiAdapter;
    private final GiveItemLocalOperationExecutor giveItemAdapter;
    private final QuestLocalOperationExecutor questAdapter;
    private final TaskTrackerLocalOperationExecutor taskTrackerAdapter;
    private final WholeTaskRuntimeLocalOperationExecutor wholeTaskAdapter;
    private final MetricsLocalOperationExecutor metricsAdapter;
    private final HostLocalOperationExecutor hostAdapter;
    private final MapSurveyPointerLocalOperationExecutor mapSurveyPointerAdapter;
    private final LeftTopStatusLocalOperationExecutor leftTopStatusAdapter;
    private final com.bot.dhxy.cloud.turn.local.XinshouDragLocalOperationExecutor xinshouDragAdapter;
    private final com.bot.dhxy.cloud.turn.local.XinshouTrackerLinkChainLocalOperationExecutor xinshouTrackerChainAdapter;
    private final XinshouTitleMechanicalExecutor xinshouTitleMechanicalExecutor;
    private final XinshouCombatLocalMechanics xinshouCombatLocalMechanics;
    private final XinshouRecoveryLocalMechanics xinshouRecoveryLocalMechanics;
    private final InputProvider inputProvider;
    private final InputSequences inputSequences;
    private final WindowTaskContextHolder contextHolder;
    private XinshouRunnerAutoCombatState xinshouRunnerAutoCombatState;

    public LocalServiceStepDispatcher(BagLocalOperationExecutor bagAdapter,
                                      UiLocalOperationExecutor uiAdapter,
                                      GiveItemLocalOperationExecutor giveItemAdapter,
                                      QuestLocalOperationExecutor questAdapter,
                                      TaskTrackerLocalOperationExecutor taskTrackerAdapter,
                                      WholeTaskRuntimeLocalOperationExecutor wholeTaskAdapter,
                                      MetricsLocalOperationExecutor metricsAdapter,
                                      HostLocalOperationExecutor hostAdapter,
                                      MapSurveyPointerLocalOperationExecutor mapSurveyPointerAdapter,
                                      LeftTopStatusLocalOperationExecutor leftTopStatusAdapter,
                                      com.bot.dhxy.cloud.turn.local.XinshouDragLocalOperationExecutor xinshouDragAdapter,
                                       com.bot.dhxy.cloud.turn.local.XinshouTrackerLinkChainLocalOperationExecutor xinshouTrackerChainAdapter,
                                       XinshouTitleMechanicalExecutor xinshouTitleMechanicalExecutor,
                                       XinshouCombatLocalMechanics xinshouCombatLocalMechanics,
                                       XinshouRecoveryLocalMechanics xinshouRecoveryLocalMechanics,
                                      InputProvider inputProvider,
                                      InputSequences inputSequences,
                                      WindowTaskContextHolder contextHolder) {
        this.bagAdapter = Objects.requireNonNull(bagAdapter, "bagAdapter");
        this.uiAdapter = Objects.requireNonNull(uiAdapter, "uiAdapter");
        this.giveItemAdapter = Objects.requireNonNull(giveItemAdapter, "giveItemAdapter");
        this.questAdapter = Objects.requireNonNull(questAdapter, "questAdapter");
        this.taskTrackerAdapter = Objects.requireNonNull(taskTrackerAdapter, "taskTrackerAdapter");
        this.wholeTaskAdapter = Objects.requireNonNull(wholeTaskAdapter, "wholeTaskAdapter");
        this.metricsAdapter = Objects.requireNonNull(metricsAdapter, "metricsAdapter");
        this.hostAdapter = Objects.requireNonNull(hostAdapter, "hostAdapter");
        this.mapSurveyPointerAdapter = Objects.requireNonNull(mapSurveyPointerAdapter, "mapSurveyPointerAdapter");
        this.leftTopStatusAdapter = Objects.requireNonNull(leftTopStatusAdapter, "leftTopStatusAdapter");
        this.xinshouDragAdapter = Objects.requireNonNull(xinshouDragAdapter, "xinshouDragAdapter");
        this.xinshouTrackerChainAdapter = Objects.requireNonNull(xinshouTrackerChainAdapter, "xinshouTrackerChainAdapter");
        this.xinshouTitleMechanicalExecutor = Objects.requireNonNull(
                xinshouTitleMechanicalExecutor, "xinshouTitleMechanicalExecutor");
        this.xinshouCombatLocalMechanics = Objects.requireNonNull(
                xinshouCombatLocalMechanics, "xinshouCombatLocalMechanics");
        this.xinshouRecoveryLocalMechanics = Objects.requireNonNull(
                xinshouRecoveryLocalMechanics, "xinshouRecoveryLocalMechanics");
        this.inputProvider = Objects.requireNonNull(inputProvider, "inputProvider");
        this.inputSequences = Objects.requireNonNull(inputSequences, "inputSequences");
        this.contextHolder = Objects.requireNonNull(contextHolder, "contextHolder");
    }

    /**
     * Direct mechanical-success callback into the already acknowledged Xinshou observation run.
     * Test fixtures that construct this dispatcher manually remain intentionally unarmed.
     */
    @Autowired
    void bindXinshouRunnerAutoCombatState(
            XinshouRunnerAutoCombatState xinshouRunnerAutoCombatState) {
        this.xinshouRunnerAutoCombatState = Objects.requireNonNull(
                xinshouRunnerAutoCombatState, "xinshouRunnerAutoCombatState");
    }

    /**
     * Execute one local-Service step from outside the input worker.
     *
     * <p>Legacy Bag and Give operations acquire one exclusive callback here because their adapters
     * invoke direct macros. UI and Quest adapters retain their own existing queue boundaries and
     * must not be wrapped here. The three TURN-40B-C2 queue-owning bag operations call public
     * {@code BagService} entries that own their own queue, so they are dispatched unwrapped with
     * the captured action stop token and the live identity predicate — admission is evaluated
     * inside the exclusive callback, never here.</p>
     *
     * @param call typed closed local-Service request.
     * @param sourceStepIndex zero-based action step index forwarded only to the Quest adapter.
     * @param actionPauseToken live pause token captured from the exact action-owning task.
     * @param actionStopToken live stop token captured at action resolution; null when no task
     *                        owned the window then (queue-owning bag ops then fail closed).
     * @param actionTaskStillCurrent live reference-identity predicate over the captured handle.
     * @return the adapter result, or a typed input failure when an exclusive callback does not complete.
     */
    public LocalServiceExecution execute(TurnLocalServiceCall call,
                                         int sourceStepIndex,
                                         TaskPauseToken actionPauseToken,
                                         TaskStopToken actionStopToken,
                                         BooleanSupplier actionTaskStillCurrent,
                                         String actionId,
                                         String deviceId,
                                         String windowId,
                                         TurnContinuationGateway continuationGateway) {
        if (call == null || call.operation() == null) {
            return LocalServiceExecution.failed("INVALID_LOCAL_SERVICE_CALL", null);
        }

        return switch (call.operation()) {
            case BAG_RETURN_ITEM, BAG_USE_INCENSE -> {
                AtomicReference<LocalServiceExecution> result = new AtomicReference<>();
                boolean completed = inputSequences.submitExclusiveAndWait(
                        "turn:local-service:" + call.operation(),
                        () -> {
                            result.set(bagAdapter.execute(call));
                            return true;
                        });
                yield completed && result.get() != null
                        ? result.get()
                        : LocalServiceExecution.failed("LOCAL_SERVICE_INPUT_FAILED", null);
            }
            case GIVE_ITEM_FROM_OPEN_DIALOG -> {
                AtomicReference<LocalServiceExecution> result = new AtomicReference<>();
                boolean completed = inputSequences.submitExclusiveAndWait(
                        "turn:local-service:" + call.operation(),
                        () -> {
                            result.set(giveItemAdapter.execute(call));
                            return true;
                        });
                yield completed && result.get() != null
                        ? result.get()
                        : LocalServiceExecution.failed("LOCAL_SERVICE_INPUT_FAILED", null);
            }
            // The adapter opens one retained worker request on the first sweep. Later HTTPS rounds
            // only submit callbacks to that request; wrapping either branch here would deadlock.
            case XINSHOU_DRAG_SWEEP -> xinshouDragAdapter.sweep(
                    call, actionPauseToken, actionStopToken, actionTaskStillCurrent);
            case XINSHOU_DRAG_RELEASE -> xinshouDragAdapter.release(actionStopToken);
            case UI_CLEAN_ALL, UI_CLOSE_GENERIC_WINDOWS, UI_PROBE_GENERIC_CLOSE, UI_CLEAN_LIGHTWEIGHT,
                    UI_CLOSE_MAP_SEARCH_INPUT_BY_X2 -> uiAdapter.execute(call);
            case QUEST_ACTIVATE, QUEST_CAPTURE_DETAIL -> questAdapter.execute(call, sourceStepIndex);
            case TASK_TRACKER_CAPTURE_PANEL -> {
                AtomicReference<LocalServiceExecution> result = new AtomicReference<>();
                boolean completed = inputSequences.submitBackgroundExclusiveAndWait(
                        "turn:local-service:" + call.operation(),
                        () -> {
                            result.set(taskTrackerAdapter.execute(call, sourceStepIndex));
                            return true;
                        });
                yield completed && result.get() != null
                        ? result.get()
                        : LocalServiceExecution.failed("LOCAL_SERVICE_INPUT_FAILED", null);
            }
            case XINSHOU_TRACKER_LINK_CHAIN -> xinshouTrackerChainAdapter.execute(call);
            // Each mechanical collaborator already owns its exact input/capture boundary. Wrapping
            // this route in another exclusive callback would create queue-in-queue deadlocks.
            case XINSHOU_MECHANICAL_ACTION -> executeXinshouMechanical(call.xinshouMechanical());
            // Whole-task runtime facts (pathing/timer/dialog-interest/progress/flying/map/near) are
            // AtomicReference state or read-only local observations owned by the bound runtime, so they
            // do not acquire the input worker here.
            case WHOLE_TASK_PATHING_REGISTER, WHOLE_TASK_PATHING_READ,
                    WHOLE_TASK_PATHING_CLEAR_INTENT,
                    WHOLE_TASK_PATHING_CLEAR_SOURCE_PREFIX, WHOLE_TASK_PATHING_CLEAR,
                    WHOLE_TASK_RECOVERY_RESET,
                    WHOLE_TASK_PATHING_UPGRADE_TARGET_MAP,
                    WHOLE_TASK_MOVEMENT_INTENT_RECORD,
                    WHOLE_TASK_TARGET_MAP_GATE_START, WHOLE_TASK_TARGET_MAP_GATE_OPEN,
                    WHOLE_TASK_TARGET_MAP_GATE_OPEN_AND_DIALOG_INTEREST,
                    WHOLE_TASK_PRE_BATTLE_TIMER_READ, WHOLE_TASK_PRE_BATTLE_FACT_READ,
                    WHOLE_TASK_PRE_BATTLE_TIMEOUT_MARK,
                    WHOLE_TASK_PRE_BATTLE_TIMER_START,
                    WHOLE_TASK_PRE_BATTLE_TIMER_PAUSE, WHOLE_TASK_PRE_BATTLE_TIMER_CLEAR,
                    WHOLE_TASK_DIALOG_INTEREST_UPDATE, WHOLE_TASK_DIALOG_INTEREST_CLEAR,
                    WHOLE_TASK_PROGRESS_UPDATE, WHOLE_TASK_STARTUP_FLYING_STATE_CONSUME,
                    WHOLE_TASK_STARTUP_FLYING_STATE_UPDATE,
                    WHOLE_TASK_DIALOG_RUNTIME_READ, WHOLE_TASK_COMBAT_ENTRY_CLEANUP,
                    WHOLE_TASK_PENDING_TRANSFER_CHOICE_UPDATE,
                    WHOLE_TASK_PENDING_TRANSFER_CHOICE_CONSUME,
                    WHOLE_TASK_PENDING_ROUTE_OUTCOME_READ,
                    WHOLE_TASK_PENDING_ROUTE_OUTCOME_REPLACE,
                    WHOLE_TASK_PENDING_ROUTE_OUTCOME_CONSUME,
                    WHOLE_TASK_RETURN_HOME_REPLAY_ARM,
                    WHOLE_TASK_EXPECTED_COMBAT_ENTER_CLAIM,
                    WHOLE_TASK_NPC_ARRIVAL_FIFO_CONSUME,
                    XIULUO_ACCEPT_DIALOG_TEMPLATE,
                    JIANGHU_LILIAN_ACCEPT_DIALOG_TEMPLATE,
                    CATCH_GHOST_ACCEPT_DIALOG_TEMPLATE,
                    CATCH_GHOST_CANCEL_DIALOG_TEMPLATE -> wholeTaskAdapter.execute(
                            call, actionId, sourceStepIndex, continuationGateway);
            // Metric records are pure diagnostics: no input, no capture, no queue ownership, so
            // they are never wrapped in an exclusive input callback.
            case METRIC_RECORD_ROUND_STARTED,
                    METRIC_RECORD_ROUND_FINISHED,
                    METRIC_RECORD_XIULUO_FAILURE_CASE -> metricsAdapter.execute(call);
            case HOST_SLEEP_COMPUTER -> hostAdapter.execute(call, actionStopToken);
            case MAP_SURVEY_POINTER_SAMPLE -> mapSurveyPointerAdapter.execute(deviceId, windowId);
            case LEFT_TOP_STATUS_OBSERVE -> leftTopStatusAdapter.execute(deviceId, windowId);
            // Queue-owning bag operations: the public BagService entries acquire the single input
            // queue themselves, so wrapping them here would deadlock queue-in-queue. The captured
            // token and live identity predicate are forwarded for in-callback guarded admission.
            case BAG_FIVERING_SUPPLY_CHECK,
                    BAG_FIND_AND_USE_FROM_BACK,
                    BAG_FIND_ITEM_PAGE_INDEX -> bagAdapter.executeQueueOwning(
                            call, actionStopToken, actionTaskStillCurrent,
                            actionId, sourceStepIndex, continuationGateway);
        };
    }

    private LocalServiceExecution executeXinshouMechanical(TurnXinshouMechanicalArguments arguments) {
        if (!hasValidXinshouMechanicalShape(arguments)) {
            return LocalServiceExecution.failed("INVALID_XINSHOU_MECHANICAL_CALL", null);
        }
        TurnXinshouMechanicalAction action = arguments.action();
        try {
            return switch (action) {
                case CONFIRM_ADOPTION,
                     USE_UPGRADE_ITEM_AND_CLOSE_GENERIC_WINDOWS,
                     USE_SHELL_AND_BLOW,
                     HAND_IN_MATERIALS,
                     REPAIR_ITEMS_ONCE,
                     CLOSE_REPAIR_WINDOW,
                     USE_LUNHUI_ITEM_AND_START -> {
                    XinshouTitleMechanicalExecutor.ExecutionResult result =
                            xinshouTitleMechanicalExecutor.execute(action);
                    String code = "XINSHOU_MECHANICAL_" + action + '_' + result.code();
                    yield result.completed()
                            ? LocalServiceExecution.completed(code, null, null)
                            : LocalServiceExecution.failed(code, null);
                }
                case PRESS_ESCAPE -> mapRecoveryResult(
                        action, xinshouRecoveryLocalMechanics.pressEscapeOnce());
                case CLICK_RECOVERY_TEMPLATE -> mapRecoveryResult(
                        action,
                        xinshouRecoveryLocalMechanics.matchAndClickOnce(
                                arguments.recoveryTemplateName()));
                case CLICK_PREPARED_POINT -> clickPreparedPoint(arguments);
                case PRESS_ORDINARY_AUTO_COMBAT -> mapCombatResult(
                        action,
                        xinshouCombatLocalMechanics.pressOrdinaryAutoCombatOnce());
                case CAPTURE_COMBAT -> mapCombatResult(
                        action,
                        xinshouCombatLocalMechanics.captureCombatOnce(
                                arguments.screenX(),
                                arguments.screenY(),
                                arguments.sourceWindowLeft(),
                                arguments.sourceWindowTop(),
                                arguments.sourceWindowWidth(),
                                arguments.sourceWindowHeight()));
                case RESTORE_AUTO_COMBAT -> {
                    XinshouCombatLocalMechanics.Result result =
                            xinshouCombatLocalMechanics.restoreAutoCombatOnce();
                    if (result.status() == XinshouCombatLocalMechanics.Status.COMPLETED
                            && xinshouRunnerAutoCombatState != null) {
                        xinshouRunnerAutoCombatState.arm(
                                contextHolder.rawCurrent().orElse(null));
                    }
                    yield mapCombatResult(action, result);
                }
            };
        } catch (RuntimeException failure) {
            return LocalServiceExecution.failed(
                    "XINSHOU_MECHANICAL_" + action + "_FAILED", null);
        }
    }

    private LocalServiceExecution clickPreparedPoint(TurnXinshouMechanicalArguments arguments) {
        WindowRuntimeContext context = contextHolder.rawCurrent().orElse(null);
        WindowNativeBinding binding = context == null ? null : context.getNativeBinding();
        if (binding == null || !binding.hasNativeHandle() || !binding.hasGeometry()) {
            return LocalServiceExecution.failed(
                    "XINSHOU_MECHANICAL_CLICK_PREPARED_POINT_WINDOW_UNAVAILABLE", null);
        }
        AtomicReference<LocalServiceExecution> callbackResult = new AtomicReference<>();
        InputActionExecutionResult terminal;
        try {
            terminal = inputSequences.submitFrozenExactWindowExclusiveAndWait(
                    "xinshou:prepared-point",
                    context,
                    binding,
                    () -> {
                        /*
                         * The worker has already witnessed this exact binding generation and holds the
                         * context monitor. Keep translation and direct input inside that same boundary so a
                         * refresh cannot splice a newer window onto this source-frame point.
                         */
                        if (binding.getWidth() != arguments.sourceWindowWidth()
                                || binding.getHeight() != arguments.sourceWindowHeight()) {
                            callbackResult.set(LocalServiceExecution.failed(
                                    "XINSHOU_MECHANICAL_CLICK_PREPARED_POINT_WINDOW_SIZE_CHANGED",
                                    null));
                            return true;
                        }
                        long translatedX = (long) arguments.screenX()
                                + binding.getX() - arguments.sourceWindowLeft();
                        long translatedY = (long) arguments.screenY()
                                + binding.getY() - arguments.sourceWindowTop();
                        if (translatedX < binding.getX()
                                || translatedY < binding.getY()
                                || translatedX >= (long) binding.getX() + binding.getWidth()
                                || translatedY >= (long) binding.getY() + binding.getHeight()
                                || translatedX < Integer.MIN_VALUE
                                || translatedY < Integer.MIN_VALUE
                                || translatedX > Integer.MAX_VALUE
                                || translatedY > Integer.MAX_VALUE) {
                            callbackResult.set(LocalServiceExecution.failed(
                                    "XINSHOU_MECHANICAL_CLICK_PREPARED_POINT_OUTSIDE_WINDOW", null));
                            return true;
                        }
                        try {
                            if (!InputActionScope.checkpoint()) {
                                return false;
                            }
                            inputProvider.moveMouse((int) translatedX, (int) translatedY);
                            if (!TaskSleep.sleep(80) || !InputActionScope.checkpoint()) {
                                return false;
                            }
                            inputProvider.clickLeft((int) translatedX, (int) translatedY, 250);
                            callbackResult.set(LocalServiceExecution.completed(
                                    "XINSHOU_MECHANICAL_CLICK_PREPARED_POINT_COMPLETED",
                                    null,
                                    null));
                            return true;
                        } catch (TaskStopRequestedException stopped) {
                            throw stopped;
                        } catch (RuntimeException inputFailure) {
                            callbackResult.set(LocalServiceExecution.failed(
                                    "XINSHOU_MECHANICAL_CLICK_PREPARED_POINT_INPUT_FAILED", null));
                            return true;
                        }
                    });
        } catch (RuntimeException submissionFailure) {
            return LocalServiceExecution.failed(
                    "XINSHOU_MECHANICAL_CLICK_PREPARED_POINT_INPUT_FAILED", null);
        }
        if (terminal == null) {
            return LocalServiceExecution.failed(
                    "XINSHOU_MECHANICAL_CLICK_PREPARED_POINT_INPUT_FAILED", null);
        }
        if (terminal.isCompleted()) {
            LocalServiceExecution exactResult = callbackResult.get();
            return exactResult == null
                    ? LocalServiceExecution.failed(
                            "XINSHOU_MECHANICAL_CLICK_PREPARED_POINT_CALLBACK_MISSING", null)
                    : exactResult;
        }
        if (terminal.getSafetyReason() == InputActionSafetyReason.STOP_REQUESTED) {
            return LocalServiceExecution.stopped(null);
        }
        if (terminal.getSafetyReason() == InputActionSafetyReason.WINDOW_BINDING_CHANGED
                || terminal.getSafetyReason() == InputActionSafetyReason.TASK_RUN_MISMATCH) {
            return LocalServiceExecution.failed(
                    "XINSHOU_MECHANICAL_CLICK_PREPARED_POINT_STALE", null);
        }
        return LocalServiceExecution.failed(
                "XINSHOU_MECHANICAL_CLICK_PREPARED_POINT_INPUT_FAILED", null);
    }

    private static LocalServiceExecution mapRecoveryResult(
            TurnXinshouMechanicalAction action,
            XinshouRecoveryLocalMechanics.Result result) {
        String code = "XINSHOU_MECHANICAL_" + action + '_' + result.status();
        return result.status() == XinshouRecoveryLocalMechanics.Status.INPUT_APPLIED
                ? LocalServiceExecution.completed(code, null, null)
                : LocalServiceExecution.failed(code, null);
    }

    private static LocalServiceExecution mapCombatResult(
            TurnXinshouMechanicalAction action,
            XinshouCombatLocalMechanics.Result result) {
        String code = "XINSHOU_MECHANICAL_" + action + '_' + result.status();
        return result.status() == XinshouCombatLocalMechanics.Status.COMPLETED
                ? LocalServiceExecution.completed(code, null, null)
                : LocalServiceExecution.failed(code, null);
    }

    private static boolean hasValidXinshouMechanicalShape(
            TurnXinshouMechanicalArguments arguments) {
        if (arguments == null || arguments.action() == null) {
            return false;
        }
        return switch (arguments.action()) {
            case CLICK_RECOVERY_TEMPLATE -> (
                    "tiaoguo.png".equals(arguments.recoveryTemplateName())
                            || "quedingguan_.png".equals(arguments.recoveryTemplateName())
                            || "confirm.png".equals(arguments.recoveryTemplateName()))
                    && hasNoPreparedPoint(arguments);
            case CLICK_PREPARED_POINT, CAPTURE_COMBAT ->
                    arguments.recoveryTemplateName() == null
                    && hasCompletePreparedPoint(arguments)
                    && arguments.sourceWindowWidth() > 0
                    && arguments.sourceWindowHeight() > 0
                    && arguments.screenX() >= arguments.sourceWindowLeft()
                    && arguments.screenY() >= arguments.sourceWindowTop()
                    && arguments.screenX()
                    < (long) arguments.sourceWindowLeft() + arguments.sourceWindowWidth()
                    && arguments.screenY()
                    < (long) arguments.sourceWindowTop() + arguments.sourceWindowHeight();
            case CONFIRM_ADOPTION,
                 USE_UPGRADE_ITEM_AND_CLOSE_GENERIC_WINDOWS,
                 USE_SHELL_AND_BLOW,
                 HAND_IN_MATERIALS,
                 REPAIR_ITEMS_ONCE,
                 CLOSE_REPAIR_WINDOW,
                 USE_LUNHUI_ITEM_AND_START,
                 PRESS_ESCAPE,
                 PRESS_ORDINARY_AUTO_COMBAT,
                 RESTORE_AUTO_COMBAT -> arguments.recoveryTemplateName() == null
                    && hasNoPreparedPoint(arguments);
        };
    }

    private static boolean hasCompletePreparedPoint(TurnXinshouMechanicalArguments arguments) {
        return arguments.screenX() != null
                && arguments.screenY() != null
                && arguments.sourceWindowLeft() != null
                && arguments.sourceWindowTop() != null
                && arguments.sourceWindowWidth() != null
                && arguments.sourceWindowHeight() != null;
    }

    private static boolean hasNoPreparedPoint(TurnXinshouMechanicalArguments arguments) {
        return arguments.screenX() == null
                && arguments.screenY() == null
                && arguments.sourceWindowLeft() == null
                && arguments.sourceWindowTop() == null
                && arguments.sourceWindowWidth() == null
                && arguments.sourceWindowHeight() == null;
    }
}
