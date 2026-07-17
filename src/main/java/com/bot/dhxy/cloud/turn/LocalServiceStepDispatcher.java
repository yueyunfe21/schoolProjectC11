package com.bot.dhxy.cloud.turn;

import com.bot.dhxy.cloud.turn.local.BagLocalOperationExecutor;
import com.bot.dhxy.cloud.turn.local.GiveItemLocalOperationExecutor;
import com.bot.dhxy.cloud.turn.local.QuestLocalOperationExecutor;
import com.bot.dhxy.cloud.turn.local.UiLocalOperationExecutor;
import com.bot.dhxy.cloud.turn.protocol.TurnLocalServiceCall;
import com.bot.dhxy.input.InputSequences;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/** Routes each closed local-Service operation through its approved queue ownership boundary. */
@Component
public final class LocalServiceStepDispatcher {

    private final BagLocalOperationExecutor bagAdapter;
    private final UiLocalOperationExecutor uiAdapter;
    private final GiveItemLocalOperationExecutor giveItemAdapter;
    private final QuestLocalOperationExecutor questAdapter;
    private final InputSequences inputSequences;

    public LocalServiceStepDispatcher(BagLocalOperationExecutor bagAdapter,
                                      UiLocalOperationExecutor uiAdapter,
                                      GiveItemLocalOperationExecutor giveItemAdapter,
                                      QuestLocalOperationExecutor questAdapter,
                                      InputSequences inputSequences) {
        this.bagAdapter = Objects.requireNonNull(bagAdapter, "bagAdapter");
        this.uiAdapter = Objects.requireNonNull(uiAdapter, "uiAdapter");
        this.giveItemAdapter = Objects.requireNonNull(giveItemAdapter, "giveItemAdapter");
        this.questAdapter = Objects.requireNonNull(questAdapter, "questAdapter");
        this.inputSequences = Objects.requireNonNull(inputSequences, "inputSequences");
    }

    /**
     * Execute one local-Service step from outside the input worker.
     *
     * <p>Bag and Give operations acquire one exclusive callback here because their adapters invoke direct
     * macros. UI and Quest adapters retain their own existing queue boundaries and must not be wrapped here.</p>
     *
     * @param call typed closed local-Service request.
     * @param sourceStepIndex zero-based action step index forwarded only to the Quest adapter.
     * @return the adapter result, or a typed input failure when an exclusive callback does not complete.
     */
    public LocalServiceExecution execute(TurnLocalServiceCall call, int sourceStepIndex) {
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
        };
    }
}
