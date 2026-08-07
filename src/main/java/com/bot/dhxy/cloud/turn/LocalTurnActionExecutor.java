package com.bot.dhxy.cloud.turn;

import com.bot.dhxy.cloud.turn.protocol.TurnAction;
import com.bot.dhxy.cloud.turn.protocol.TurnCaptureSpec;
import com.bot.dhxy.cloud.turn.protocol.TurnContinuationDecision;
import com.bot.dhxy.cloud.turn.protocol.TurnContinuationRequest;
import com.bot.dhxy.cloud.turn.protocol.TurnFramePurpose;
import com.bot.dhxy.cloud.turn.protocol.TurnInputAction;
import com.bot.dhxy.cloud.turn.protocol.TurnInputSpec;
import com.bot.dhxy.cloud.turn.protocol.TurnMatchResult;
import com.bot.dhxy.cloud.turn.protocol.TurnProtocolValidator;
import com.bot.dhxy.cloud.turn.protocol.TurnRequest;
import com.bot.dhxy.cloud.turn.protocol.TurnResponse;
import com.bot.dhxy.cloud.turn.protocol.TurnStep;
import com.bot.dhxy.cloud.turn.protocol.TurnStepResult;
import com.bot.dhxy.cloud.turn.protocol.TurnStepType;
import com.bot.dhxy.cloud.turn.protocol.TurnPathingIntent;
import com.bot.dhxy.window.execution.MultiWindowTaskManager;
import com.bot.dhxy.window.model.WindowPathingIntent;
import com.bot.dhxy.window.model.WindowPathingIntentType;
import com.bot.dhxy.window.runtime.WindowNativeBindingRefreshService;
import com.bot.dhxy.window.runtime.WindowRuntimeContext;
import com.bot.dhxy.window.runtime.WindowTaskContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Executes one validated Cloud action strictly against one refreshed local window snapshot. */
@Component
@Slf4j
public final class LocalTurnActionExecutor {

    private final MultiWindowTaskManager taskManager;
    private final WindowNativeBindingRefreshService bindingRefreshService;
    private final WindowTaskContextHolder contextHolder;
    private final TurnCaptureStepExecutor captureExecutor;
    private final TurnMatchStepExecutor matchExecutor;
    private final TurnInputStepExecutor inputExecutor;
    private final LocalServiceStepDispatcher localServiceDispatcher;
    private final TurnClient turnClient;
    private final TurnOutcomeAssembler outcomeAssembler;

    public LocalTurnActionExecutor(MultiWindowTaskManager taskManager,
                                   WindowNativeBindingRefreshService bindingRefreshService,
                                   WindowTaskContextHolder contextHolder,
                                   TurnCaptureStepExecutor captureExecutor,
                                   TurnMatchStepExecutor matchExecutor,
                                   TurnInputStepExecutor inputExecutor,
                                   LocalServiceStepDispatcher localServiceDispatcher,
                                   TurnClient turnClient,
                                   TurnOutcomeAssembler outcomeAssembler) {
        this.taskManager = Objects.requireNonNull(taskManager, "taskManager");
        this.bindingRefreshService = Objects.requireNonNull(bindingRefreshService, "bindingRefreshService");
        this.contextHolder = Objects.requireNonNull(contextHolder, "contextHolder");
        this.captureExecutor = Objects.requireNonNull(captureExecutor, "captureExecutor");
        this.matchExecutor = Objects.requireNonNull(matchExecutor, "matchExecutor");
        this.inputExecutor = Objects.requireNonNull(inputExecutor, "inputExecutor");
        this.localServiceDispatcher = Objects.requireNonNull(localServiceDispatcher, "localServiceDispatcher");
        this.turnClient = Objects.requireNonNull(turnClient, "turnClient");
        this.outcomeAssembler = Objects.requireNonNull(outcomeAssembler, "outcomeAssembler");
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
        log.info("[turn-action-evidence] received actionId={} windowId={} hwnd={} steps={}",
                validated.actionId(), window.metadata().windowId(), window.binding().getNativeHandle(),
                describeSteps(validated.steps()));
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
                if (mouseSequenceEnd > index + 1
                        || (isMouseInput(step) && window.context().currentPendingDirectCombatEnterClaim() != null)) {
                    List<TurnStep> mouseSequence = validated.steps().subList(index, mouseSequenceEnd);
                    com.bot.dhxy.window.model.WindowExpectedCombatEnterClaim pendingDirectCombatClaim =
                            window.context().currentPendingDirectCombatEnterClaim();
                    boolean markedDirectTarget = pendingDirectCombatClaim != null
                            && isDirectCombatTargetMouseSequence(mouseSequence);
                    // A pending Alt+A ticket may span Cloud turns.  A later ordinary left click must never
                    // consume it; instead it proves that the promised direct-combat target action was replaced.
                    if (pendingDirectCombatClaim != null && !markedDirectTarget
                            && containsLeftClick(mouseSequence)) {
                        window.context().clearPendingDirectCombatEnterClaim("alt-a target click marker mismatch");
                        pendingDirectCombatClaim = null;
                    }
                    TurnInputStepExecutor.MouseSequenceResult sequenceResult;
                    try {
                        sequenceResult = inputExecutor.executeMouseSequence(window, mouseSequence);
                    } catch (Exception mechanicalFailure) {
                        sequenceResult = new TurnInputStepExecutor.MouseSequenceResult(
                                new TurnInputStepExecutor.Result(
                                        TurnInputStepExecutor.Status.FAILED,
                                        TurnInputStepExecutor.Code.INPUT_QUEUE_FAILED,
                                        diagnostic(mechanicalFailure)),
                                0);
                    }

                    executions.addAll(expandMouseSequenceExecutions(mouseSequence, sequenceResult));
                    if (markedDirectTarget) {
                        if (sequenceResult.result().status() == TurnInputStepExecutor.Status.COMPLETED) {
                            window.context().consumePendingDirectCombatEnterClaim(pendingDirectCombatClaim.claimId());
                        } else {
                            window.context().clearPendingDirectCombatEnterClaim("alt-a target click not executed");
                        }
                    }
                    terminal = sequenceResult.result().status() != TurnInputStepExecutor.Status.COMPLETED;
                    index = mouseSequenceEnd - 1;
                    continue;
                }

                TurnStepExecution execution = executeStep(validated, window, step);
                executions.add(execution);
                if (execution.frame() != null) {
                    candidateFrame = execution.frame();
                }
                terminal = execution.stopped() || execution.result().status() == TurnStepResult.Status.FAILED;
            }

            boolean stopped = executions.stream().anyMatch(TurnStepExecution::stopped);
            boolean failed = !stopped && executions.stream()
                    .anyMatch(execution -> execution.result().status() == TurnStepResult.Status.FAILED);
            if ((stopped || failed) && window.context().currentPendingDirectCombatEnterClaim() != null) {
                window.context().clearPendingDirectCombatEnterClaim(
                        stopped ? "turn stopped before direct-combat target click" : "turn failed before direct-combat target click");
            }
            if (failed && validated.fullWindowFailureEvidence()) {
                candidateFrame = null;
                try {
                    candidateFrame = captureExecutor.capture(
                            window, null, TurnFramePurpose.FAILURE_EVIDENCE, null);
                } catch (RuntimeException failureEvidenceUnavailable) {
                    // Keep the original failed execution without frame evidence; never retry or fabricate metadata.
                }
            }

            // A completed action only arms the exact window runner with its route. The long-lived
            // runner, not a second synchronous screenshot probe, is the sole source of motion facts.
            if (validated.pathingIntent() != null && !stopped && !failed) {
                contextHolder.callWith(window.context(), () -> {
                    window.context().markPathingStarted(toWindowPathingIntent(validated.pathingIntent()));
                    return null;
                });
            }

            var outcome = outcomeAssembler.assemble(validated, window.metadata(), executions, candidateFrame);
            log.info("[turn-action-evidence] completed actionId={} windowId={} status={} steps={} frameEvidence={}",
                    validated.actionId(), window.metadata().windowId(), outcome.status(),
                    executions.stream().map(execution -> execution.result().status().name()).toList(),
                    candidateFrame == null ? "none" : candidateFrame.metadata());
            return new ExecutedTurn(outcome, candidateFrame == null ? null : candidateFrame.pngBytes());
    }

    /** Compact, coordinate-preserving action transcript used to join Cloud decisions to physical-input logs. */
    private static List<String> describeSteps(List<TurnStep> steps) {
        if (steps == null) {
            return List.of();
        }
        return steps.stream().map(step -> {
            if (step == null) {
                return "null";
            }
            if (step.type() == TurnStepType.INPUT && step.input() != null) {
                return step.index() + ":" + step.inputAction() + "@"
                        + step.input().x() + "," + step.input().y();
            }
            return step.index() + ":" + step.type();
        }).toList();
    }

    private WindowPathingIntent toWindowPathingIntent(TurnPathingIntent intent) {
        WindowPathingIntentType type;
        try {
            type = WindowPathingIntentType.valueOf(intent.type());
        } catch (IllegalArgumentException | NullPointerException invalidType) {
            type = WindowPathingIntentType.TARGETED;
        }
        return WindowPathingIntent.builder()
                .source(intent.source())
                .intentId(intent.intentId())
                .targetMapName(intent.targetMapName())
                .targetX(intent.targetX())
                .targetY(intent.targetY())
                .tolerance(intent.tolerance())
                .type(type)
                .build();
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

    /** The marker is consumed only by one atomic target sequence: move(s), waits and one marked left click. */
    private static boolean isDirectCombatTargetMouseSequence(List<TurnStep> steps) {
        int leftClicks = 0;
        for (TurnStep step : steps) {
            if (step.type() == TurnStepType.WAIT) continue;
            if (step.type() != TurnStepType.INPUT || step.inputAction() == null) return false;
            if (step.inputAction() == TurnInputAction.CLICK_LEFT) {
                if (step.input() == null || !Boolean.TRUE.equals(step.input().directCombatTargetClick())) {
                    return false;
                }
                leftClicks++;
            }
            else if (step.inputAction() != TurnInputAction.MOVE_MOUSE) return false;
        }
        return leftClicks == 1;
    }

    private static boolean containsLeftClick(List<TurnStep> steps) {
        return steps.stream().anyMatch(step -> step.type() == TurnStepType.INPUT
                && step.inputAction() == TurnInputAction.CLICK_LEFT);
    }

    private TurnStepExecution executeStep(TurnAction action, TurnExecutionWindow window, TurnStep step) {
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
                case LOCAL_SERVICE -> executeLocalService(action, window, step);
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

    private TurnStepExecution executeLocalService(TurnAction action,
                                                  TurnExecutionWindow window,
                                                  TurnStep step) {
        // TURN-40B-C2: the captured action-owning token and the live identity predicate ride the
        // local-service path so queue-owning bag admission is evaluated inside the exclusive
        // callback, never as a pre-queue snapshot. The predicate closes over the window's exact
        // captured handle; the token object is the one captured at resolveForAction.
        LocalServiceExecution local = contextHolder.callWith(
                window.context(),
                () -> localServiceDispatcher.execute(
                        step.localService(),
                        step.index(),
                        window.actionPauseToken(),
                        window.actionStopToken(),
                        window::isActionTaskStillCurrent,
                        action.actionId(),
                        action.deviceId(),
                        action.windowId(),
                        (request, frame) -> exchangeContinuation(window, request, frame)));
        if (local.stopRequested()) {
            // The sole typed local stop (FAILED + reserved STOPPED code) maps to the existing
            // typed stopped step outcome; it never degrades to a generic failure.
            return TurnStepExecution.stopped(step, null, null, local.code());
        }
        return local.status() == TurnStepResult.Status.COMPLETED
                ? TurnStepExecution.completed(step, local.code(), null, local.localResultJson(), local.frame())
                : TurnStepExecution.failed(
                        step, local.code(), null, local.localResultJson(), local.frame(), local.code());
    }

    /** Expand one atomic worker result without turning its completed click prefix into NOT_RUN. */
    static List<TurnStepExecution> expandMouseSequenceExecutions(
            List<TurnStep> mouseSequence,
            TurnInputStepExecutor.MouseSequenceResult sequenceResult) {
        List<TurnStepExecution> expanded = new ArrayList<>(mouseSequence.size());
        if (sequenceResult.result().status() == TurnInputStepExecutor.Status.COMPLETED) {
            for (TurnStep step : mouseSequence) {
                expanded.add(TurnStepExecution.completed(step, "OK", null, null, null));
            }
            return expanded;
        }
        int failedOffset = Math.min(
                sequenceResult.completedTurnStepCount(), mouseSequence.size() - 1);
        for (int index = 0; index < failedOffset; index++) {
            expanded.add(TurnStepExecution.completed(
                    mouseSequence.get(index), "OK", null, null, null));
        }
        TurnStep failedStep = mouseSequence.get(failedOffset);
        TurnInputStepExecutor.Result result = sequenceResult.result();
        expanded.add(switch (result.status()) {
            case FAILED -> TurnStepExecution.failed(
                    failedStep, result.code().name(), null, null, null, result.detail());
            case STOPPED -> TurnStepExecution.stopped(failedStep, null, null, result.detail());
            case COMPLETED -> throw new IllegalStateException("completed mouse result handled above");
        });
        for (int index = failedOffset + 1; index < mouseSequence.size(); index++) {
            expanded.add(TurnStepExecution.notRun(mouseSequence.get(index)));
        }
        return expanded;
    }

    private TurnContinuationDecision exchangeContinuation(TurnExecutionWindow window,
                                                          TurnContinuationRequest continuation,
                                                          TurnFrame frame) {
        try {
            TurnRequest request = TurnProtocolValidator.requireValid(new TurnRequest(
                    1,
                    window.metadata(),
                    0L,
                    null,
                    null,
                    continuation));
            TurnExchangeResult exchange = turnClient.exchange(
                    request,
                    frame == null ? null : frame.pngBytes());
            TurnResponse response = exchange.response();
            if (response.status() != TurnResponse.Status.CONTINUATION
                    || response.continuationDecision() == null) {
                throw new IllegalStateException("Cloud did not return a continuation decision");
            }
            return response.continuationDecision();
        } catch (TurnTransportException transportFailure) {
            throw new IllegalStateException("Turn continuation transport failed", transportFailure);
        }
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
