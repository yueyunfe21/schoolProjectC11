package com.bot.dhxy.cloud.turn.local;

import com.bot.dhxy.cloud.turn.LocalServiceExecution;
import com.bot.dhxy.cloud.turn.protocol.TurnLocalServiceCall;
import com.bot.dhxy.cloud.turn.protocol.TurnWholeTaskRuntimeResult;
import com.bot.dhxy.cloud.turn.TurnFrame;
import com.bot.dhxy.cloud.turn.protocol.TurnContinuationDecision;
import com.bot.dhxy.cloud.turn.protocol.TurnContinuationRequest;
import com.bot.dhxy.input.InputProvider;
import com.bot.dhxy.input.InputSequences;
import com.bot.dhxy.input.action.InputActionScope;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Physical accept-dialog mechanics for the five-ring accept operation (TURN-35 Amendment #6).
 *
 * <p>This moves the {@code 696a12b0} {@code FiveRingTaskV2.acceptInitialDialogAndTriggerPathing}
 * exclusive body — the effective single accept-option click path — into one existing input-queue
 * exclusive callback. After the click this operation returns immediately. Tracker-anchor confirmation
 * and any post-click dialog recheck run outside the input queue so a stale pre-transition OPTION frame
 * can never be submitted as a completion STORY.</p>
 */
@Component
public final class FiveRingAcceptDialogLocalOperation {

    private static final String RESULT_NOT_ACCEPTED = "NOT_ACCEPTED";
    private static final String RESULT_TASK_ACCEPTED_NEEDS_SYNC = "TASK_ACCEPTED_NEEDS_SYNC";

    private final InputSequences inputSequences;
    private final FiveRingDialogObservationLocalMechanics observationMechanics;
    private final InputProvider inputProvider;
    private final ObjectMapper objectMapper;

    public FiveRingAcceptDialogLocalOperation(InputSequences inputSequences,
                                              FiveRingDialogObservationLocalMechanics observationMechanics,
                                              InputProvider inputProvider,
                                              ObjectMapper objectMapper) {
        this.inputSequences = Objects.requireNonNull(inputSequences, "inputSequences");
        this.observationMechanics = Objects.requireNonNull(observationMechanics, "observationMechanics");
        this.inputProvider = Objects.requireNonNull(inputProvider, "inputProvider");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
    }

    /**
     * Execute the closed {@code WUHUAN_ACCEPT_DIALOG_EXCLUSIVE} accept-dialog flow.
     *
     * @param call typed local-Service call; only the five-ring exclusive accept operation is served.
     * @return completed result whose enum names the baseline accept outcome, or a fail-closed result
     *         when the exclusive input callback does not run.
     */
    public LocalServiceExecution execute(TurnLocalServiceCall call,
                                         String actionId,
                                         int sourceStepIndex,
                                         TurnContinuationGateway gateway) {
        if (call == null || call.wholeTaskRuntime() == null || gateway == null) {
            return LocalServiceExecution.failed("INVALID_WHOLE_TASK_CALL", null);
        }
        AtomicReference<String> flowResult = new AtomicReference<>(RESULT_NOT_ACCEPTED);
        boolean ran = inputSequences.submitExclusiveAndWait(
                "turn:local-service:WUHUAN_ACCEPT_DIALOG_EXCLUSIVE",
                () -> {
                    flowResult.set(runAcceptFlow(actionId, sourceStepIndex, gateway));
                    return true;
                });
        if (!ran) {
            return LocalServiceExecution.failed("LOCAL_SERVICE_INPUT_FAILED", null);
        }
        return LocalServiceExecution.completed(
                "WHOLE_TASK_WUHUAN_ACCEPT_DIALOG",
                json(new TurnWholeTaskRuntimeResult(null, flowResult.get(), null, null)),
                null);
    }

    /**
     * Baseline effective accept body: the baseline loop returns from every first-attempt branch, so
     * exactly one accept-option observation/click can occur. A successful click is intentionally only
     * a provisional accept: Cloud must wait for the tracker anchor and, on timeout, classify a fresh
     * dialog before deciding whether to retry or handle a legitimate post-click story.
     */
    private String runAcceptFlow(String actionId, int stepIndex, TurnContinuationGateway gateway) {
        TurnFrame option = observationMechanics.captureOption(stepIndex);
        TurnContinuationDecision optionDecision = gateway.exchange(new TurnContinuationRequest(
                actionId, stepIndex, TurnContinuationRequest.Kind.FIVERING_ACCEPT_DIALOG,
                TurnContinuationRequest.Stage.DIALOG_OPTION_IMAGE, option.metadata(), null), option);
        if (optionDecision.directive() == TurnContinuationDecision.Directive.COMPLETE_NOT_ACCEPTED) {
            return RESULT_NOT_ACCEPTED;
        }
        if (optionDecision.directive() != TurnContinuationDecision.Directive.CLICK_ACCEPT) {
            throw new IllegalStateException("unexpected accept option directive: " + optionDecision.directive());
        }
        requireInputMayContinue("before accept click");
        inputProvider.clickLeft(optionDecision.clickX(), optionDecision.clickY(), 150);
        requireInputMayContinue("after accept click");
        return RESULT_TASK_ACCEPTED_NEEDS_SYNC;
    }

    private void requireInputMayContinue(String stage) {
        if (!InputActionScope.checkpoint()) {
            throw new IllegalStateException("five-ring accept input cancelled " + stage);
        }
    }

    private String json(TurnWholeTaskRuntimeResult result) {
        try {
            return objectMapper.writeValueAsString(result);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("five-ring accept result serialization failed", e);
        }
    }
}
