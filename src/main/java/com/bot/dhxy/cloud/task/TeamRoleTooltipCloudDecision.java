package com.bot.dhxy.cloud.task;

import com.bot.dhxy.cloud.decision.CloudDecisionResult;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class TeamRoleTooltipCloudDecision {

    public enum Status {
        CLOUD_FOUND,
        CLOUD_NO_RESULT,
        DISABLED,
        REQUIRED_FAILURE
    }

    public enum Role {
        LEADER,
        MEMBER,
        UNKNOWN
    }

    @Builder.Default
    Status status = Status.REQUIRED_FAILURE;
    @Builder.Default
    Role role = Role.UNKNOWN;
    String leaderClientId;
    String currentPlayerId;
    String reason;
    String debugToken;
    @Builder.Default
    double confidence = 0.0d;
    CloudDecisionResult cloudResult;

    public boolean isFound() {
        return status == Status.CLOUD_FOUND;
    }
}
