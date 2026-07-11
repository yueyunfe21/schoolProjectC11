package com.bot.dhxy.cloud.task;

import com.bot.dhxy.cloud.decision.CloudDecisionResult;
import com.bot.dhxy.task.transaction.TaskTransactionResult;
import com.bot.dhxy.task.transaction.TaskYieldPolicy;
import lombok.Builder;
import lombok.Value;

/**
 * Task-policy cloud execute envelope for one already-computed local phase outcome.
 *
 * <p>Accepted cloud policy may replace only the enum-like phase outcome fields: transaction result,
 * yield policy, and next phase. The caller keeps its existing task state/source/message/wait context
 * and falls back to the local outcome whenever this envelope is not {@link Status#CLOUD_EXECUTED}.</p>
 */
@Value
@Builder
public class TaskPolicyCloudDecision<P extends Enum<P>> {

    public enum Status {
        LOCAL_PASSTHROUGH,
        CLOUD_EXECUTED,
        CLOUD_REJECTED_LOCAL,
        CLOUD_REQUIRED_FAILURE
    }

    @Builder.Default
    Status status = Status.LOCAL_PASSTHROUGH;
    CloudDecisionResult cloudResult;
    String localDecision;
    TaskTransactionResult localResult;
    TaskYieldPolicy localYieldPolicy;
    P localNextPhase;
    TaskTransactionResult effectiveResult;
    TaskYieldPolicy effectiveYieldPolicy;
    P effectiveNextPhase;
    String rejectReason;

    static <P extends Enum<P>> TaskPolicyCloudDecision<P> localOnly(
            String localDecision,
            TaskTransactionResult localResult,
            TaskYieldPolicy localYieldPolicy,
            P localNextPhase) {
        return localPassthrough(null, localDecision, localResult, localYieldPolicy, localNextPhase);
    }

    static <P extends Enum<P>> TaskPolicyCloudDecision<P> localPassthrough(
            CloudDecisionResult cloudResult,
            String localDecision,
            TaskTransactionResult localResult,
            TaskYieldPolicy localYieldPolicy,
            P localNextPhase) {
        return TaskPolicyCloudDecision.<P>builder()
                .status(Status.LOCAL_PASSTHROUGH)
                .cloudResult(cloudResult)
                .localDecision(localDecision)
                .localResult(localResult)
                .localYieldPolicy(localYieldPolicy)
                .localNextPhase(localNextPhase)
                .effectiveResult(localResult)
                .effectiveYieldPolicy(localYieldPolicy)
                .effectiveNextPhase(localNextPhase)
                .build();
    }

    static <P extends Enum<P>> TaskPolicyCloudDecision<P> localPassthrough(
            CloudDecisionResult cloudResult,
            String localDecision,
            TaskTransactionResult localResult,
            TaskYieldPolicy localYieldPolicy,
            P localNextPhase,
            String reason) {
        return TaskPolicyCloudDecision.<P>builder()
                .status(Status.LOCAL_PASSTHROUGH)
                .cloudResult(cloudResult)
                .localDecision(localDecision)
                .localResult(localResult)
                .localYieldPolicy(localYieldPolicy)
                .localNextPhase(localNextPhase)
                .effectiveResult(localResult)
                .effectiveYieldPolicy(localYieldPolicy)
                .effectiveNextPhase(localNextPhase)
                .rejectReason(reason)
                .build();
    }

    static <P extends Enum<P>> TaskPolicyCloudDecision<P> cloudExecuted(
            CloudDecisionResult cloudResult,
            String localDecision,
            TaskTransactionResult localResult,
            TaskYieldPolicy localYieldPolicy,
            P localNextPhase,
            AppliedOutcome<P> cloudOutcome) {
        return TaskPolicyCloudDecision.<P>builder()
                .status(Status.CLOUD_EXECUTED)
                .cloudResult(cloudResult)
                .localDecision(localDecision)
                .localResult(localResult)
                .localYieldPolicy(localYieldPolicy)
                .localNextPhase(localNextPhase)
                .effectiveResult(cloudOutcome.transactionResult())
                .effectiveYieldPolicy(cloudOutcome.yieldPolicy())
                .effectiveNextPhase(cloudOutcome.nextPhase())
                .build();
    }

    static <P extends Enum<P>> TaskPolicyCloudDecision<P> cloudRejectedLocal(
            CloudDecisionResult cloudResult,
            String localDecision,
            TaskTransactionResult localResult,
            TaskYieldPolicy localYieldPolicy,
            P localNextPhase,
            String rejectReason) {
        return TaskPolicyCloudDecision.<P>builder()
                .status(Status.CLOUD_REJECTED_LOCAL)
                .cloudResult(cloudResult)
                .localDecision(localDecision)
                .localResult(localResult)
                .localYieldPolicy(localYieldPolicy)
                .localNextPhase(localNextPhase)
                .effectiveResult(localResult)
                .effectiveYieldPolicy(localYieldPolicy)
                .effectiveNextPhase(localNextPhase)
                .rejectReason(rejectReason)
                .build();
    }

    static <P extends Enum<P>> TaskPolicyCloudDecision<P> cloudRequiredFailure(
            CloudDecisionResult cloudResult,
            String localDecision,
            TaskTransactionResult localResult,
            TaskYieldPolicy localYieldPolicy,
            P localNextPhase,
            P failurePhase,
            String rejectReason) {
        return TaskPolicyCloudDecision.<P>builder()
                .status(Status.CLOUD_REQUIRED_FAILURE)
                .cloudResult(cloudResult)
                .localDecision(localDecision)
                .localResult(localResult)
                .localYieldPolicy(localYieldPolicy)
                .localNextPhase(localNextPhase)
                .effectiveResult(TaskTransactionResult.RETRYABLE_ERROR)
                .effectiveYieldPolicy(TaskYieldPolicy.MUST_YIELD)
                .effectiveNextPhase(failurePhase)
                .rejectReason(rejectReason)
                .build();
    }

    public boolean isCloudExecuted() {
        return status == Status.CLOUD_EXECUTED;
    }

    public boolean isCloudRequiredFailure() {
        return status == Status.CLOUD_REQUIRED_FAILURE;
    }

    public AppliedOutcome<P> appliedOutcome() {
        return new AppliedOutcome<>(effectiveResult, effectiveYieldPolicy, effectiveNextPhase);
    }

    public record AppliedOutcome<P extends Enum<P>>(
            TaskTransactionResult transactionResult,
            TaskYieldPolicy yieldPolicy,
            P nextPhase) {
    }
}
