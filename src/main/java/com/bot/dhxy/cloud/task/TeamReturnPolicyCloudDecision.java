package com.bot.dhxy.cloud.task;

import com.bot.dhxy.cloud.decision.CloudDecisionResult;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class TeamReturnPolicyCloudDecision {

    public enum Status {
        LOCAL_PASSTHROUGH,
        CLOUD_EXECUTED,
        CLOUD_REQUIRED_FAILURE_DENY
    }

    @Builder.Default
    Status status = Status.LOCAL_PASSTHROUGH;
    CloudDecisionResult cloudResult;
    String localDecision;
    String effectiveDecision;
    boolean localAllowed;
    boolean cloudAllowed;
    boolean allowed;
    String rejectReason;

    public static TeamReturnPolicyCloudDecision localOnly(String localDecision, boolean localAllowed) {
        return TeamReturnPolicyCloudDecision.builder()
                .status(Status.LOCAL_PASSTHROUGH)
                .localDecision(localDecision)
                .effectiveDecision(localDecision)
                .localAllowed(localAllowed)
                .cloudAllowed(localAllowed)
                .allowed(localAllowed)
                .build();
    }

    static TeamReturnPolicyCloudDecision cloudExecuted(
            CloudDecisionResult cloudResult,
            String localDecision,
            boolean localAllowed,
            boolean cloudAllowed) {
        return TeamReturnPolicyCloudDecision.builder()
                .status(Status.CLOUD_EXECUTED)
                .cloudResult(cloudResult)
                .localDecision(localDecision)
                .effectiveDecision(cloudResult.getEffectiveDecision())
                .localAllowed(localAllowed)
                .cloudAllowed(cloudAllowed)
                .allowed(localAllowed && cloudAllowed)
                .build();
    }

    static TeamReturnPolicyCloudDecision requiredFailureDeny(
            CloudDecisionResult cloudResult,
            String localDecision,
            boolean localAllowed,
            String rejectReason) {
        return TeamReturnPolicyCloudDecision.builder()
                .status(Status.CLOUD_REQUIRED_FAILURE_DENY)
                .cloudResult(cloudResult)
                .localDecision(localDecision)
                .effectiveDecision("action=DENY;reason=cloud-required-failure")
                .localAllowed(localAllowed)
                .cloudAllowed(false)
                .allowed(false)
                .rejectReason(rejectReason)
                .build();
    }
}
