package com.bot.dhxy.cloud.task;

import com.bot.dhxy.cloud.decision.CloudDecisionResult;
import lombok.Builder;
import lombok.Value;

/**
 * TASK_RECOVERY execute envelope for a locally built recovery candidate.
 *
 * <p>The cloud may authorize only the exact candidate that local task code already built. It cannot
 * invent arbitrary phases, input, or recovery actions.</p>
 */
@Value
@Builder
public class TaskRecoveryCloudDecision<P extends Enum<P>> {

    public enum Status {
        LOCAL_PASSTHROUGH,
        CLOUD_EXECUTED,
        CLOUD_REQUIRED_FAILURE
    }

    @Builder.Default
    Status status = Status.LOCAL_PASSTHROUGH;
    CloudDecisionResult cloudResult;
    String localDecision;
    String localAction;
    P localNextPhase;
    String effectiveAction;
    P effectiveNextPhase;
    String rejectReason;

    static <P extends Enum<P>> TaskRecoveryCloudDecision<P> localPassthrough(
            CloudDecisionResult cloudResult,
            String localDecision,
            String localAction,
            P localNextPhase,
            String reason) {
        return TaskRecoveryCloudDecision.<P>builder()
                .status(Status.LOCAL_PASSTHROUGH)
                .cloudResult(cloudResult)
                .localDecision(localDecision)
                .localAction(localAction)
                .localNextPhase(localNextPhase)
                .effectiveAction(localAction)
                .effectiveNextPhase(localNextPhase)
                .rejectReason(reason)
                .build();
    }

    static <P extends Enum<P>> TaskRecoveryCloudDecision<P> cloudExecuted(
            CloudDecisionResult cloudResult,
            String localDecision,
            String localAction,
            P localNextPhase) {
        return TaskRecoveryCloudDecision.<P>builder()
                .status(Status.CLOUD_EXECUTED)
                .cloudResult(cloudResult)
                .localDecision(localDecision)
                .localAction(localAction)
                .localNextPhase(localNextPhase)
                .effectiveAction(localAction)
                .effectiveNextPhase(localNextPhase)
                .build();
    }

    static <P extends Enum<P>> TaskRecoveryCloudDecision<P> cloudRequiredFailure(
            CloudDecisionResult cloudResult,
            String localDecision,
            String localAction,
            P localNextPhase,
            String rejectReason) {
        return TaskRecoveryCloudDecision.<P>builder()
                .status(Status.CLOUD_REQUIRED_FAILURE)
                .cloudResult(cloudResult)
                .localDecision(localDecision)
                .localAction(localAction)
                .localNextPhase(localNextPhase)
                .rejectReason(rejectReason)
                .build();
    }

    public boolean isCloudExecuted() {
        return status == Status.CLOUD_EXECUTED;
    }

    public boolean isCloudRequiredFailure() {
        return status == Status.CLOUD_REQUIRED_FAILURE;
    }

    public boolean isRecoveryAllowed() {
        return isCloudExecuted();
    }
}
