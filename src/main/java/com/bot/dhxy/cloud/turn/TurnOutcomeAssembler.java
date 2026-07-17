package com.bot.dhxy.cloud.turn;

import com.bot.dhxy.cloud.turn.protocol.TurnAction;
import com.bot.dhxy.cloud.turn.protocol.TurnOutcome;
import com.bot.dhxy.cloud.turn.protocol.TurnProtocolValidator;
import com.bot.dhxy.cloud.turn.protocol.TurnStep;
import com.bot.dhxy.cloud.turn.protocol.TurnStepResult;
import com.bot.dhxy.cloud.turn.protocol.TurnWindowMetadata;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Assembles one validator-valid terminal outcome from a complete ordered mechanical execution list. */
@Component
public final class TurnOutcomeAssembler {

    /**
     * Assemble a COMPLETED, FAILED, or STOPPED outcome without duplicate/uncertain policy.
     *
     * @param action validated action whose list indexes define execution order.
     * @param window immutable metadata from the action's single refreshed window snapshot.
     * @param executions one execution for every declared step, including later NOT_RUN entries.
     * @param frame optional final candidate frame whose metadata is placed in the outcome.
     * @return a protocol-validator-valid terminal outcome.
     */
    public TurnOutcome assemble(TurnAction action,
                                TurnWindowMetadata window,
                                List<TurnStepExecution> executions,
                                TurnFrame frame) {
        Objects.requireNonNull(action, "action");
        Objects.requireNonNull(window, "window");
        Objects.requireNonNull(executions, "executions");
        if (executions.size() != action.steps().size()) {
            throw new IllegalArgumentException("execution count must equal declared step count");
        }

        List<TurnStepResult> results = new ArrayList<>(executions.size());
        int terminalIndex = -1;
        boolean stopped = false;
        for (int index = 0; index < executions.size(); index++) {
            TurnStep step = action.steps().get(index);
            TurnStepExecution execution = Objects.requireNonNull(executions.get(index), "step execution");
            TurnStepResult result = execution.result();
            if (result.index() != index || step.index() != index || result.type() != step.type()) {
                throw new IllegalArgumentException("step execution identity does not match action index " + index);
            }
            if (terminalIndex < 0 && result.status() == TurnStepResult.Status.FAILED) {
                terminalIndex = index;
                stopped = execution.stopped();
            }
            results.add(result);
        }

        requireTerminalShape(executions, terminalIndex, stopped);
        TurnOutcome.Status status;
        Integer failedStepIndex;
        String code;
        String message;
        if (terminalIndex < 0) {
            status = TurnOutcome.Status.COMPLETED;
            failedStepIndex = null;
            code = "OK";
            message = null;
        } else {
            TurnStepExecution terminal = executions.get(terminalIndex);
            status = stopped ? TurnOutcome.Status.STOPPED : TurnOutcome.Status.FAILED;
            failedStepIndex = stopped ? null : terminalIndex;
            code = terminal.result().code();
            message = terminal.diagnosticMessage() == null || terminal.diagnosticMessage().isBlank()
                    ? code
                    : terminal.diagnosticMessage();
        }

        TurnOutcome outcome = new TurnOutcome(
                action.contractVersion(),
                action.actionId(),
                window,
                status,
                failedStepIndex,
                code,
                message,
                List.copyOf(results),
                frame == null ? null : frame.metadata());
        return TurnProtocolValidator.requireValid(outcome);
    }

    private static void requireTerminalShape(List<TurnStepExecution> executions,
                                             int terminalIndex,
                                             boolean stopped) {
        for (int index = 0; index < executions.size(); index++) {
            TurnStepExecution execution = executions.get(index);
            TurnStepResult.Status status = execution.result().status();
            if (terminalIndex < 0) {
                if (status != TurnStepResult.Status.COMPLETED || execution.stopped()) {
                    throw new IllegalArgumentException("completed outcome requires every step to complete");
                }
            } else if (index < terminalIndex) {
                if (status != TurnStepResult.Status.COMPLETED || execution.stopped()) {
                    throw new IllegalArgumentException("terminal outcome requires a completed prefix");
                }
            } else if (index == terminalIndex) {
                if (status != TurnStepResult.Status.FAILED || execution.stopped() != stopped) {
                    throw new IllegalArgumentException("terminal step shape is inconsistent");
                }
            } else if (status != TurnStepResult.Status.NOT_RUN || execution.stopped()) {
                throw new IllegalArgumentException("steps after the terminal step must be NOT_RUN");
            }
        }
    }
}
