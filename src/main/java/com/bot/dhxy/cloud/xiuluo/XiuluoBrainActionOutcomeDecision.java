package com.bot.dhxy.cloud.xiuluo;

import com.bot.dhxy.cloud.decision.CloudDecisionResult;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class XiuluoBrainActionOutcomeDecision {

    public enum Status {
        ACCEPTED_OUTCOME,
        DUPLICATE_REPLAY,
        RESET_REQUIRED,
        LOCAL_SAFETY_DENIED,
        CLOUD_REQUIRED_FAILURE
    }

    @Builder.Default
    Status status = Status.CLOUD_REQUIRED_FAILURE;
    CloudDecisionResult cloudResult;
    String outcomeStatus;
    String rejectReason;
    String resetReason;

    static XiuluoBrainActionOutcomeDecision accepted(CloudDecisionResult cloudResult, String outcomeStatus) {
        Status status = "DUPLICATE_REPLAY".equals(outcomeStatus) ? Status.DUPLICATE_REPLAY : Status.ACCEPTED_OUTCOME;
        return XiuluoBrainActionOutcomeDecision.builder()
                .status(status)
                .cloudResult(cloudResult)
                .outcomeStatus(outcomeStatus)
                .build();
    }

    static XiuluoBrainActionOutcomeDecision localSafetyDenied(CloudDecisionResult cloudResult, String rejectReason) {
        return XiuluoBrainActionOutcomeDecision.builder()
                .status(Status.LOCAL_SAFETY_DENIED)
                .cloudResult(cloudResult)
                .rejectReason(rejectReason)
                .build();
    }

    static XiuluoBrainActionOutcomeDecision resetRequired(
            CloudDecisionResult cloudResult,
            String resetReason) {
        return XiuluoBrainActionOutcomeDecision.builder()
                .status(Status.RESET_REQUIRED)
                .cloudResult(cloudResult)
                .outcomeStatus("RESET_REQUIRED")
                .resetReason(resetReason)
                .build();
    }

    static XiuluoBrainActionOutcomeDecision cloudRequiredFailure(
            CloudDecisionResult cloudResult,
            String rejectReason) {
        return XiuluoBrainActionOutcomeDecision.builder()
                .status(Status.CLOUD_REQUIRED_FAILURE)
                .cloudResult(cloudResult)
                .rejectReason(rejectReason)
                .build();
    }

    public boolean isAcceptedOutcome() {
        return status == Status.ACCEPTED_OUTCOME || status == Status.DUPLICATE_REPLAY;
    }

    public boolean isResetRequired() {
        return status == Status.RESET_REQUIRED;
    }
}
