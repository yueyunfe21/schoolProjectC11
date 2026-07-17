package com.bot.dhxy.cloud.turn;

import com.bot.dhxy.cloud.turn.protocol.TurnMatchResult;
import com.bot.dhxy.cloud.turn.protocol.TurnStep;
import com.bot.dhxy.cloud.turn.protocol.TurnStepResult;
import com.bot.dhxy.cloud.turn.protocol.TurnStepType;

import java.util.Objects;

/** One closed mechanical step result plus its optional candidate frame and stop classification. */
public record TurnStepExecution(
        TurnStepResult result,
        TurnFrame frame,
        boolean stopped,
        String diagnosticMessage) {

    public TurnStepExecution {
        Objects.requireNonNull(result, "result");
        if (stopped && (result.status() != TurnStepResult.Status.FAILED || !"STOPPED".equals(result.code()))) {
            throw new IllegalArgumentException("stopped execution requires a FAILED step result with STOPPED code");
        }
        if (result.status() == TurnStepResult.Status.NOT_RUN && (frame != null || stopped)) {
            throw new IllegalArgumentException("NOT_RUN execution must not carry a frame or stop marker");
        }
        if (result.type() != TurnStepType.MATCH_TEMPLATE && result.match() != null) {
            throw new IllegalArgumentException("only MATCH_TEMPLATE execution may carry match data");
        }
        if (result.type() != TurnStepType.LOCAL_SERVICE && result.localResultJson() != null) {
            throw new IllegalArgumentException("only LOCAL_SERVICE execution may carry local result JSON");
        }
        if (frame != null
                && result.type() != TurnStepType.CAPTURE
                && result.type() != TurnStepType.MATCH_TEMPLATE
                && result.type() != TurnStepType.LOCAL_SERVICE) {
            throw new IllegalArgumentException("this step type cannot produce a frame");
        }
    }

    public static TurnStepExecution completed(TurnStep step,
                                              String code,
                                              TurnMatchResult match,
                                              String localResultJson,
                                              TurnFrame frame) {
        return create(step, TurnStepResult.Status.COMPLETED, code, match, localResultJson, frame, false, null);
    }

    public static TurnStepExecution failed(TurnStep step,
                                           String code,
                                           TurnMatchResult match,
                                           String localResultJson,
                                           TurnFrame frame,
                                           String diagnosticMessage) {
        return create(step, TurnStepResult.Status.FAILED, code, match, localResultJson, frame, false,
                diagnosticMessage);
    }

    public static TurnStepExecution stopped(TurnStep step,
                                            TurnMatchResult match,
                                            TurnFrame frame,
                                            String diagnosticMessage) {
        return create(step, TurnStepResult.Status.FAILED, "STOPPED", match, null, frame, true,
                diagnosticMessage);
    }

    public static TurnStepExecution notRun(TurnStep step) {
        return create(step, TurnStepResult.Status.NOT_RUN, "NOT_RUN", null, null, null, false, null);
    }

    private static TurnStepExecution create(TurnStep step,
                                            TurnStepResult.Status status,
                                            String code,
                                            TurnMatchResult match,
                                            String localResultJson,
                                            TurnFrame frame,
                                            boolean stopped,
                                            String diagnosticMessage) {
        Objects.requireNonNull(step, "step");
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("step execution code must not be blank");
        }
        TurnStepResult result = new TurnStepResult(
                step.index(), step.type(), status, code, match, localResultJson);
        return new TurnStepExecution(result, frame, stopped, diagnosticMessage);
    }
}
