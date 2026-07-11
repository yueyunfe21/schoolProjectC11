package com.bot.dhxy.cloud.xiuluo;

import com.bot.dhxy.cloud.decision.CloudDecisionResult;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class XiuluoBrainDecision {

    public enum Status {
        ACCEPTED_CLOUD_COMMAND,
        LOCAL_SAFETY_DENIED,
        CLOUD_REQUIRED_FAILURE
    }

    @Builder.Default
    Status status = Status.CLOUD_REQUIRED_FAILURE;
    CloudDecisionResult cloudResult;
    XiuluoBrainResponse response;
    String rejectReason;

    static XiuluoBrainDecision accepted(CloudDecisionResult cloudResult, XiuluoBrainResponse response) {
        return XiuluoBrainDecision.builder()
                .status(Status.ACCEPTED_CLOUD_COMMAND)
                .cloudResult(cloudResult)
                .response(response)
                .build();
    }

    static XiuluoBrainDecision localSafetyDenied(CloudDecisionResult cloudResult, String rejectReason) {
        return XiuluoBrainDecision.builder()
                .status(Status.LOCAL_SAFETY_DENIED)
                .cloudResult(cloudResult)
                .rejectReason(rejectReason)
                .build();
    }

    static XiuluoBrainDecision cloudRequiredFailure(CloudDecisionResult cloudResult, String rejectReason) {
        return XiuluoBrainDecision.builder()
                .status(Status.CLOUD_REQUIRED_FAILURE)
                .cloudResult(cloudResult)
                .rejectReason(rejectReason)
                .build();
    }

    public boolean isAcceptedCloudCommand() {
        return status == Status.ACCEPTED_CLOUD_COMMAND;
    }

    public boolean isLocalSafetyDenied() {
        return status == Status.LOCAL_SAFETY_DENIED;
    }

    public boolean isCloudRequiredFailure() {
        return status == Status.CLOUD_REQUIRED_FAILURE;
    }
}
