package com.bot.dhxy.cloud.task;

import com.bot.dhxy.cloud.decision.CloudDecisionResult;
import com.bot.dhxy.model.tasktracker.TaskTrackerGreenLink;
import lombok.Builder;
import lombok.Value;

import java.awt.Point;

/**
 * Result envelope for tracker-link-ranker cloud shadow/execute decisions.
 *
 * <p>Execute mode can now produce a cloud-owned window-relative click point. Rejected execute
 * decisions are represented as explicit no-click results so task code cannot accidentally continue
 * with the old local green-link click during cloud execute tests.</p>
 */
@Value
@Builder
public class TrackerLinkRankerCloudDecision {

    public enum Status {
        LOCAL_PASSTHROUGH,
        CLOUD_EXECUTED,
        CLOUD_REJECTED_NO_CLICK
    }

    @Builder.Default
    Status status = Status.LOCAL_PASSTHROUGH;
    CloudDecisionResult cloudResult;
    int localSelectedIndex;
    TaskTrackerGreenLink localSelectedLink;
    Point cloudWindowRelativeClickPoint;
    String rejectReason;

    static TrackerLinkRankerCloudDecision localOnly(int localSelectedIndex, TaskTrackerGreenLink localSelectedLink) {
        return localPassthrough(null, localSelectedIndex, localSelectedLink);
    }

    static TrackerLinkRankerCloudDecision localPassthrough(CloudDecisionResult cloudResult,
                                                           int localSelectedIndex,
                                                           TaskTrackerGreenLink localSelectedLink) {
        return TrackerLinkRankerCloudDecision.builder()
                .status(Status.LOCAL_PASSTHROUGH)
                .cloudResult(cloudResult)
                .localSelectedIndex(localSelectedIndex)
                .localSelectedLink(localSelectedLink)
                .build();
    }

    static TrackerLinkRankerCloudDecision cloudExecuted(CloudDecisionResult cloudResult,
                                                        int localSelectedIndex,
                                                        TaskTrackerGreenLink localSelectedLink,
                                                        Point cloudWindowRelativeClickPoint) {
        return TrackerLinkRankerCloudDecision.builder()
                .status(Status.CLOUD_EXECUTED)
                .cloudResult(cloudResult)
                .localSelectedIndex(localSelectedIndex)
                .localSelectedLink(localSelectedLink)
                .cloudWindowRelativeClickPoint(cloudWindowRelativeClickPoint == null
                        ? null
                        : new Point(cloudWindowRelativeClickPoint))
                .build();
    }

    static TrackerLinkRankerCloudDecision cloudRejectedNoClick(CloudDecisionResult cloudResult,
                                                               int localSelectedIndex,
                                                               TaskTrackerGreenLink localSelectedLink,
                                                               String rejectReason) {
        return TrackerLinkRankerCloudDecision.builder()
                .status(Status.CLOUD_REJECTED_NO_CLICK)
                .cloudResult(cloudResult)
                .localSelectedIndex(localSelectedIndex)
                .localSelectedLink(localSelectedLink)
                .rejectReason(rejectReason)
                .build();
    }

    public boolean isLocalPassthrough() {
        return status == Status.LOCAL_PASSTHROUGH;
    }

    public boolean isCloudExecuted() {
        return status == Status.CLOUD_EXECUTED;
    }

    public boolean isNoClick() {
        return status == Status.CLOUD_REJECTED_NO_CLICK;
    }

    public boolean hasEffectiveLocalLink() {
        return isLocalPassthrough() && localSelectedLink != null;
    }

    public Point getCloudWindowRelativeClickPoint() {
        return cloudWindowRelativeClickPoint == null ? null : new Point(cloudWindowRelativeClickPoint);
    }
}
