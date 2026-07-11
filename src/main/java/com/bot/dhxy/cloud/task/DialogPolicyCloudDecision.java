package com.bot.dhxy.cloud.task;

import com.bot.dhxy.cloud.decision.CloudDecisionResult;
import com.bot.dhxy.model.dialog.DialogResult;
import lombok.Builder;
import lombok.Value;

/**
 * DIALOG_POLICY cloud execute envelope for one local dialog result.
 *
 * <p>The effective result may only be the original local {@link DialogResult}, a future
 * locally-built safe candidate, or an explicit failed/no-click result for cloud-required failures.
 * The cloud response never carries executable coordinates or input instructions.</p>
 */
@Value
@Builder
public class DialogPolicyCloudDecision {

    public enum Status {
        LOCAL_PASSTHROUGH,
        CLOUD_EXECUTED,
        CLOUD_REJECTED_LOCAL,
        CLOUD_REJECTED_NO_CLICK
    }

    @Builder.Default
    Status status = Status.LOCAL_PASSTHROUGH;
    CloudDecisionResult cloudResult;
    String localDecision;
    DialogResult localResult;
    DialogResult effectiveResult;
    String rejectReason;

    static DialogPolicyCloudDecision localOnly(String localDecision, DialogResult localResult) {
        return localPassthrough(null, localDecision, localResult);
    }

    static DialogPolicyCloudDecision localPassthrough(
            CloudDecisionResult cloudResult,
            String localDecision,
            DialogResult localResult) {
        return DialogPolicyCloudDecision.builder()
                .status(Status.LOCAL_PASSTHROUGH)
                .cloudResult(cloudResult)
                .localDecision(localDecision)
                .localResult(localResult)
                .effectiveResult(localResult)
                .build();
    }

    static DialogPolicyCloudDecision cloudExecuted(
            CloudDecisionResult cloudResult,
            String localDecision,
            DialogResult localResult,
            DialogResult effectiveResult) {
        return DialogPolicyCloudDecision.builder()
                .status(Status.CLOUD_EXECUTED)
                .cloudResult(cloudResult)
                .localDecision(localDecision)
                .localResult(localResult)
                .effectiveResult(effectiveResult)
                .build();
    }

    static DialogPolicyCloudDecision cloudRejectedLocal(
            CloudDecisionResult cloudResult,
            String localDecision,
            DialogResult localResult,
            String rejectReason) {
        return DialogPolicyCloudDecision.builder()
                .status(Status.CLOUD_REJECTED_LOCAL)
                .cloudResult(cloudResult)
                .localDecision(localDecision)
                .localResult(localResult)
                .effectiveResult(localResult)
                .rejectReason(rejectReason)
                .build();
    }

    static DialogPolicyCloudDecision cloudRejectedNoClick(
            CloudDecisionResult cloudResult,
            String localDecision,
            DialogResult localResult,
            DialogResult failedResult,
            String rejectReason) {
        return DialogPolicyCloudDecision.builder()
                .status(Status.CLOUD_REJECTED_NO_CLICK)
                .cloudResult(cloudResult)
                .localDecision(localDecision)
                .localResult(localResult)
                .effectiveResult(failedResult)
                .rejectReason(rejectReason)
                .build();
    }

    public boolean isCloudExecuted() {
        return status == Status.CLOUD_EXECUTED;
    }

    public boolean isNoClick() {
        return status == Status.CLOUD_REJECTED_NO_CLICK;
    }
}
