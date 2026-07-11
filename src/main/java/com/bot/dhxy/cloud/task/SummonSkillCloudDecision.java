package com.bot.dhxy.cloud.task;

import com.bot.dhxy.cloud.decision.CloudDecisionResult;
import com.bot.dhxy.model.maintenance.SummonSkillSlotStatus;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class SummonSkillCloudDecision {
    @Builder.Default
    Status status = Status.REQUIRED_FAILURE;
    @Builder.Default
    SummonSkillSlotStatus slotStatus = SummonSkillSlotStatus.UNKNOWN;
    String action;
    String reason;
    String debugToken;
    @Builder.Default
    double confidence = 0.0d;
    CloudDecisionResult cloudResult;

    public boolean isCloudExecuted() {
        return status == Status.CLOUD_EXECUTED;
    }

    public boolean isRequiredFailure() {
        return status == Status.REQUIRED_FAILURE;
    }

    public enum Status {
        CLOUD_EXECUTED,
        DISABLED,
        REQUIRED_FAILURE
    }
}
