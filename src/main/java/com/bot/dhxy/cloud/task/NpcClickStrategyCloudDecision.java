package com.bot.dhxy.cloud.task;

import com.bot.dhxy.cloud.decision.CloudDecisionResult;
import lombok.Builder;
import lombok.Value;

/**
 * Result envelope for the legacy NPC_CLICK_STRATEGY bridge.
 *
 * <p>Current CR165 production coverage must use {@code NPC_CLICK_SMART}. This envelope remains only
 * for disabled/uncovered local strategy diagnostics. It intentionally has no local or cloud success
 * status; production NPC clicks must use {@code NPC_CLICK_SMART}.</p>
 */
@Value
@Builder
public class NpcClickStrategyCloudDecision {

    public enum Status {
        CLOUD_REJECTED_NO_CLICK
    }

    @Builder.Default
    Status status = Status.CLOUD_REJECTED_NO_CLICK;
    CloudDecisionResult cloudResult;
    String localDecision;
    String strategy;
    String rejectReason;

    static NpcClickStrategyCloudDecision cloudRejectedNoClick(
            CloudDecisionResult cloudResult,
            String localDecision,
            String strategy,
            String rejectReason) {
        return NpcClickStrategyCloudDecision.builder()
                .status(Status.CLOUD_REJECTED_NO_CLICK)
                .cloudResult(cloudResult)
                .localDecision(localDecision)
                .strategy(strategy)
                .rejectReason(rejectReason)
                .build();
    }

    public boolean isAuthorized() {
        return false;
    }

    public boolean isCloudExecuted() {
        return false;
    }

    public boolean isNoClick() {
        return status == Status.CLOUD_REJECTED_NO_CLICK;
    }
}
