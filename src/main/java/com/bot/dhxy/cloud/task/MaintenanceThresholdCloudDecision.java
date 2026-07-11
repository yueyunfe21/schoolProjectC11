package com.bot.dhxy.cloud.task;

import com.bot.dhxy.cloud.decision.CloudDecisionResult;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class MaintenanceThresholdCloudDecision {

    public enum Status {
        LOCAL_PASSTHROUGH,
        CLOUD_EXECUTED,
        CLOUD_REQUIRED_FAILURE
    }

    public enum Action {
        ALLOW,
        SKIP,
        NO_ACTION,
        REQUIRED_FAILURE
    }

    @Builder.Default
    Status status = Status.LOCAL_PASSTHROUGH;
    @Builder.Default
    Action action = Action.NO_ACTION;
    CloudDecisionResult cloudResult;
    String localDecision;
    String effectiveDecision;
    String rejectReason;

    public static MaintenanceThresholdCloudDecision localOnly(String localDecision, Action action) {
        return MaintenanceThresholdCloudDecision.builder()
                .status(Status.LOCAL_PASSTHROUGH)
                .action(action)
                .localDecision(localDecision)
                .effectiveDecision(localDecision)
                .build();
    }

    static MaintenanceThresholdCloudDecision cloudExecuted(
            CloudDecisionResult cloudResult,
            String localDecision,
            Action action) {
        return MaintenanceThresholdCloudDecision.builder()
                .status(Status.CLOUD_EXECUTED)
                .action(action)
                .cloudResult(cloudResult)
                .localDecision(localDecision)
                .effectiveDecision(cloudResult.getEffectiveDecision())
                .build();
    }

    static MaintenanceThresholdCloudDecision requiredFailure(
            CloudDecisionResult cloudResult,
            String localDecision,
            String rejectReason) {
        return MaintenanceThresholdCloudDecision.builder()
                .status(Status.CLOUD_REQUIRED_FAILURE)
                .action(Action.REQUIRED_FAILURE)
                .cloudResult(cloudResult)
                .localDecision(localDecision)
                .effectiveDecision("action=REQUIRED_FAILURE;reason=cloud-required-failure")
                .rejectReason(rejectReason)
                .build();
    }

    public boolean shouldRunMaintenance() {
        return action == Action.ALLOW;
    }

    public boolean isRequiredFailure() {
        return status == Status.CLOUD_REQUIRED_FAILURE;
    }
}
