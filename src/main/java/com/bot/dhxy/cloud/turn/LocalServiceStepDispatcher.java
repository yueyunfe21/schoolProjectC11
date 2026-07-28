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
import com.bot.dhxy.cloud.turn.local.host.HostLocalOperationExecutor;
import com.bot.dhxy.cloud.turn.protocol.TurnLocalServiceCall;
import com.bot.dhxy.input.InputSequences;
import com.bot.dhxy.runner.stop.TaskStopToken;
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
    private final InputSequences inputSequences;

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
                                      InputSequences inputSequences) {
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
        this.inputSequences = Objects.requireNonNull(inputSequences, "inputSequences");
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
     * @param actionStopToken live stop token captured at action resolution; null when no task
     *                        owned the window then (queue-owning bag ops then fail closed).
     * @param actionTaskStillCurrent live reference-identity predicate over the captured handle.
     * @return the adapter result, or a typed input failure when an exclusive callback does not complete.
     */
    public LocalServiceExecution execute(TurnLocalServiceCall call,
                                         int sourceStepIndex,
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
            case UI_CLEAN_ALL, UI_CLOSE_GENERIC_WINDOWS, UI_CLEAN_LIGHTWEIGHT,
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
            // Whole-task runtime facts (pathing/timer/dialog-interest/progress/flying/map/near) are
            // AtomicReference state or read-only local observations owned by the bound runtime, so they
            // do not acquire the input worker here. WUHUAN_ACCEPT_DIALOG_EXCLUSIVE carries its own
            // indivisible input-queue exclusivity inside FiveRingAcceptDialogLocalOperation.
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
                    WUHUAN_ACCEPT_DIALOG_EXCLUSIVE -> wholeTaskAdapter.execute(
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
}
