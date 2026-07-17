package com.bot.dhxy.cloud.turn;

import com.bot.dhxy.cloud.turn.protocol.TurnAction;
import com.bot.dhxy.cloud.turn.protocol.TurnCaptureSpec;
import com.bot.dhxy.cloud.turn.protocol.TurnFramePurpose;
import com.bot.dhxy.cloud.turn.protocol.TurnInputAction;
import com.bot.dhxy.cloud.turn.protocol.TurnInputSpec;
import com.bot.dhxy.cloud.turn.protocol.TurnMatchResult;
import com.bot.dhxy.cloud.turn.protocol.TurnProtocolValidator;
import com.bot.dhxy.cloud.turn.protocol.TurnStep;
import com.bot.dhxy.cloud.turn.protocol.TurnStepResult;
import com.bot.dhxy.cloud.turn.protocol.TurnStepType;
import com.bot.dhxy.model.MapCoordinate;
import com.bot.dhxy.window.execution.MultiWindowTaskManager;
import com.bot.dhxy.window.runtime.WindowNativeBindingRefreshService;
import com.bot.dhxy.window.runtime.WindowTaskContextHolder;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Executes one validated Cloud action strictly against one refreshed local window snapshot. */
@Component
public final class LocalTurnActionExecutor {

    private final MultiWindowTaskManager taskManager;
    private final WindowNativeBindingRefreshService bindingRefreshService;
    private final WindowTaskContextHolder contextHolder;
    private final TurnCaptureStepExecutor captureExecutor;
    private final TurnMatchStepExecutor matchExecutor;
    private final TurnInputStepExecutor inputExecutor;
    private final LocalServiceStepDispatcher localServiceDispatcher;
    private final TurnOutcomeAssembler outcomeAssembler;
    private final LocalPathingStartProofMechanics pathingStartProofMechanics;

    public LocalTurnActionExecutor(MultiWindowTaskManager taskManager,
                                   WindowNativeBindingRefreshService bindingRefreshService,
                                   WindowTaskContextHolder contextHolder,
                                   TurnCaptureStepExecutor captureExecutor,
                                   TurnMatchStepExecutor matchExecutor,
                                   TurnInputStepExecutor inputExecutor,
                                   LocalServiceStepDispatcher localServiceDispatcher,
                                   TurnOutcomeAssembler outcomeAssembler,
                                   LocalPathingStartProofMechanics pathingStartProofMechanics) {
        this.taskManager = Objects.requireNonNull(taskManager, "taskManager");
        this.bindingRefreshService = Objects.requireNonNull(bindingRefreshService, "bindingRefreshService");
        this.contextHolder = Objects.requireNonNull(contextHolder, "contextHolder");
        this.captureExecutor = Objects.requireNonNull(captureExecutor, "captureExecutor");
        this.matchExecutor = Objects.requireNonNull(matchExecutor, "matchExecutor");
        this.inputExecutor = Objects.requireNonNull(inputExecutor, "inputExecutor");
        this.localServiceDispatcher = Objects.requireNonNull(localServiceDispatcher, "localServiceDispatcher");
        this.outcomeAssembler = Objects.requireNonNull(outcomeAssembler, "outcomeAssembler");
        this.pathingStartProofMechanics =
                Objects.requireNonNull(pathingStartProofMechanics, "pathingStartProofMechanics");
    }

    /**
     * Execute one complete action without local retry or later window refresh.
     *
     * @param action Cloud action whose step indexes define the exact local execution order.
     * @return validator-valid outcome plus the exact optional raw PNG named by its frame metadata.
     */
    public ExecutedTurn execute(TurnAction action) {
        TurnAction validated = TurnProtocolValidator.requireValid(action);
        TurnExecutionWindow window = TurnExecutionWindow.resolveForAction(
                validated, taskManager, bindingRefreshService);
        // Local Pathing Fact Bridge: read the pre-input coordinate baseline once for a start action that
        // carries a pathing intent, so the local proof can compare it after the action completes.
        MapCoordinate pathingStartBaseline = validated.pathingIntent() == null
                ? null
                : pathingStartProofMechanics.readBaseline();

        List<TurnStepExecution> executions = new ArrayList<>(validated.steps().size());
        TurnFrame candidateFrame = null;
        boolean terminal = false;
        for (int index = 0; index < validated.steps().size(); index++) {
            TurnStep step = validated.steps().get(index);
            if (terminal) {
                executions.add(TurnStepExecution.notRun(step));
                continue;
            }

            int mouseSequenceEnd = findMouseSequenceEndExclusive(validated.steps(), index);
            if (mouseSequenceEnd > index + 1) {
                List<TurnStep> mouseSequence = validated.steps().subList(index, mouseSequenceEnd);
                TurnStepExecution firstExecution;
                try {
                    firstExecution = fromInputResult(
                            step,
                            inputExecutor.executeMouseSequence(window, mouseSequence),
                            null,
                            null);
                } catch (Exception mechanicalFailure) {
                    firstExecution = TurnStepExecution.failed(
                            step,
                            "STEP_EXECUTION_EXCEPTION",
                            null,
                            null,
                            null,
                            diagnostic(mechanicalFailure));
                }

                if (firstExecution.result().status() == TurnStepResult.Status.COMPLETED) {
                    for (TurnStep sequenceStep : mouseSequence) {
                        executions.add(TurnStepExecution.completed(sequenceStep, "OK", null, null, null));
                    }
                } else {
                    executions.add(firstExecution);
                    for (int sequenceIndex = 1; sequenceIndex < mouseSequence.size(); sequenceIndex++) {
                        executions.add(TurnStepExecution.notRun(mouseSequence.get(sequenceIndex)));
                    }
                    terminal = true;
                }
                index = mouseSequenceEnd - 1;
                continue;
            }

            TurnStepExecution execution = executeStep(window, step);
            executions.add(execution);
            if (execution.frame() != null) {
                candidateFrame = execution.frame();
            }
            terminal = execution.stopped() || execution.result().status() == TurnStepResult.Status.FAILED;
        }

        boolean stopped = executions.stream().anyMatch(TurnStepExecution::stopped);
        boolean failed = !stopped && executions.stream()
                .anyMatch(execution -> execution.result().status() == TurnStepResult.Status.FAILED);
        if (failed && validated.fullWindowFailureEvidence()) {
            candidateFrame = null;
            try {
                candidateFrame = captureExecutor.capture(
                        window, null, TurnFramePurpose.FAILURE_EVIDENCE, null);
            } catch (RuntimeException failureEvidenceUnavailable) {
                // Keep the original failed execution without frame evidence; never retry or fabricate metadata.
            }
        }

        // Local Pathing Fact Bridge: only a COMPLETED start action (no stop, no failure) with a positive
        // local movement proof registers its intent; the proof runs its own baseline-vs-current check and
        // registers nothing on a double-negative. Cloud never observes movement itself.
        if (validated.pathingIntent() != null && !stopped && !failed) {
            pathingStartProofMechanics.proveAndRegister(
                    window.context(), validated.pathingIntent(), pathingStartBaseline);
        }

        return new ExecutedTurn(
                outcomeAssembler.assemble(validated, window.metadata(), executions, candidateFrame),
                candidateFrame == null ? null : candidateFrame.pngBytes());
    }

    /**
     * Finds the maximal prefix that ends on mouse input; a trailing WAIT stays outside the queue transaction.
     */
    private int findMouseSequenceEndExclusive(List<TurnStep> steps, int startIndex) {
        if (!isMouseInput(steps.get(startIndex))) {
            return startIndex + 1;
        }

        int cursor = startIndex + 1;
        int lastMouseIndex = startIndex;
        while (cursor < steps.size()) {
            TurnStep candidate = steps.get(cursor);
            if (isMouseInput(candidate)) {
                lastMouseIndex = cursor;
                cursor++;
                continue;
            }
            if (candidate.type() == TurnStepType.WAIT
                    && candidate.waitMs() != null
                    && candidate.waitMs() > 0L) {
                cursor++;
                continue;
            }
            break;
        }
        return lastMouseIndex + 1;
    }

    private boolean isMouseInput(TurnStep step) {
        if (step.type() != TurnStepType.INPUT
                || step.inputAction() == null) {
            return false;
        }
        return switch (step.inputAction()) {
            case MOVE_MOUSE, CLICK_LEFT, CLICK_RIGHT, DOUBLE_CLICK_LEFT, DOUBLE_CLICK_RIGHT, DRAG_LEFT, SCROLL ->
                    true;
            case KEY_TAP, KEY_DOWN, KEY_UP, TEXT_INPUT -> false;
        };
    }

    private TurnStepExecution executeStep(TurnExecutionWindow window, TurnStep step) {
        try {
            return switch (step.type()) {
                case CAPTURE -> executeCapture(window, step);
                case MATCH_TEMPLATE -> executeMatch(window, step);
                case INPUT -> fromInputResult(
                        step,
                        inputExecutor.execute(window, step.inputAction(), step.input(), step.index()),
                        null,
                        null);
                case WAIT -> executeWait(window, step);
                case LOCAL_SERVICE -> executeLocalService(window, step);
            };
        } catch (Exception mechanicalFailure) {
            return TurnStepExecution.failed(
                    step,
                    "STEP_EXECUTION_EXCEPTION",
                    null,
                    null,
                    null,
                    diagnostic(mechanicalFailure));
        }
    }

    private TurnStepExecution executeCapture(TurnExecutionWindow window, TurnStep step) {
        TurnCaptureStepExecutor.Execution captured = captureExecutor.execute(
                window, step.capture(), step.index());
        return switch (captured.status()) {
            case COMPLETED -> {
                TurnFrame returned = step.capture().resultMode() == TurnCaptureSpec.ResultMode.UPLOAD_IMAGE
                        ? captured.frame()
                        : null;
                yield TurnStepExecution.completed(
                        step, captured.code().name(), null, null, returned);
            }
            case FAILED -> TurnStepExecution.failed(
                    step, captured.code().name(), null, null, null, captured.detail());
            case STOPPED -> TurnStepExecution.stopped(step, null, null, captured.detail());
        };
    }

    private TurnStepExecution executeMatch(TurnExecutionWindow window, TurnStep step) throws TurnTransportException {
        TurnMatchStepExecutor.Execution match = matchExecutor.execute(window, step);
        if (!match.clickRequested()) {
            return TurnStepExecution.completed(step, "OK", match.match(), null, match.frame());
        }

        TurnMatchResult result = match.match();
        TurnInputSpec click = new TurnInputSpec(
                result.centerX(), result.centerY(), null, null, null, null, null);
        try {
            return fromInputResult(
                    step,
                    inputExecutor.execute(window, TurnInputAction.CLICK_LEFT, click, step.index()),
                    result,
                    match.frame());
        } catch (RuntimeException clickFailure) {
            return TurnStepExecution.failed(
                    step,
                    "STEP_EXECUTION_EXCEPTION",
                    result,
                    null,
                    match.frame(),
                    diagnostic(clickFailure));
        }
    }

    private TurnStepExecution executeWait(TurnExecutionWindow window, TurnStep step) {
        if (window.metadata().stopRequested() || Thread.currentThread().isInterrupted()) {
            return TurnStepExecution.stopped(step, null, null, "stop requested before wait step");
        }
        return fromInputResult(step, inputExecutor.waitFor(step.waitMs()), null, null);
    }

    private TurnStepExecution executeLocalService(TurnExecutionWindow window, TurnStep step) {
        LocalServiceExecution local = contextHolder.callWith(
                window.context(),
                () -> localServiceDispatcher.execute(step.localService(), step.index()));
        return local.status() == TurnStepResult.Status.COMPLETED
                ? TurnStepExecution.completed(step, local.code(), null, local.localResultJson(), local.frame())
                : TurnStepExecution.failed(
                        step, local.code(), null, local.localResultJson(), local.frame(), local.code());
    }

    private TurnStepExecution fromInputResult(TurnStep step,
                                              TurnInputStepExecutor.Result input,
                                              TurnMatchResult match,
                                              TurnFrame frame) {
        return switch (input.status()) {
            case COMPLETED -> TurnStepExecution.completed(step, input.code().name(), match, null, frame);
            case FAILED -> TurnStepExecution.failed(
                    step, input.code().name(), match, null, frame, input.detail());
            case STOPPED -> TurnStepExecution.stopped(step, match, frame, input.detail());
        };
    }

    private static String diagnostic(Exception failure) {
        String message = failure.getMessage();
        return message == null || message.isBlank()
                ? failure.getClass().getSimpleName()
                : failure.getClass().getSimpleName() + ": " + message;
    }
}
