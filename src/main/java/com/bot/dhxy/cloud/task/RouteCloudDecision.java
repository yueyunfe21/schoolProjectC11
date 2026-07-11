package com.bot.dhxy.cloud.task;

import com.bot.dhxy.cloud.decision.CloudDecisionResult;
import lombok.Builder;
import lombok.Value;

import java.awt.Point;

/**
 * Route memory/candidate cloud decision envelope.
 *
 * <p>In execute mode, accepted cloud route clicks are window-relative and become the effective
 * click. Rejected execute decisions are explicit no-click outcomes so navigation cannot silently
 * report a local route click as if the cloud response succeeded.</p>
 */
@Value
@Builder
public class RouteCloudDecision {

    public enum Status {
        LOCAL_PASSTHROUGH,
        CLOUD_EXECUTED,
        CLOUD_NO_CLICK,
        CLOUD_REJECTED_NO_CLICK
    }

    @Builder.Default
    Status status = Status.LOCAL_PASSTHROUGH;
    CloudDecisionResult cloudResult;
    String localShadowDecision;
    Point localWindowRelativeClickPoint;
    Point cloudWindowRelativeClickPoint;
    String routeDecisionId;
    String rejectReason;

    static RouteCloudDecision localOnly(String localShadowDecision, Point localWindowRelativeClickPoint) {
        return localPassthrough(null, localShadowDecision, localWindowRelativeClickPoint);
    }

    static RouteCloudDecision localPassthrough(CloudDecisionResult cloudResult,
                                               String localShadowDecision,
                                               Point localWindowRelativeClickPoint) {
        return RouteCloudDecision.builder()
                .status(Status.LOCAL_PASSTHROUGH)
                .cloudResult(cloudResult)
                .localShadowDecision(localShadowDecision)
                .localWindowRelativeClickPoint(copy(localWindowRelativeClickPoint))
                .build();
    }

    static RouteCloudDecision cloudExecuted(CloudDecisionResult cloudResult,
                                            String localShadowDecision,
                                            Point localWindowRelativeClickPoint,
                                            Point cloudWindowRelativeClickPoint,
                                            String routeDecisionId) {
        return RouteCloudDecision.builder()
                .status(Status.CLOUD_EXECUTED)
                .cloudResult(cloudResult)
                .localShadowDecision(localShadowDecision)
                .localWindowRelativeClickPoint(copy(localWindowRelativeClickPoint))
                .cloudWindowRelativeClickPoint(copy(cloudWindowRelativeClickPoint))
                .routeDecisionId(routeDecisionId)
                .build();
    }

    static RouteCloudDecision cloudNoClick(CloudDecisionResult cloudResult,
                                           String localShadowDecision,
                                           Point localWindowRelativeClickPoint,
                                           String routeDecisionId,
                                           String reason) {
        return RouteCloudDecision.builder()
                .status(Status.CLOUD_NO_CLICK)
                .cloudResult(cloudResult)
                .localShadowDecision(localShadowDecision)
                .localWindowRelativeClickPoint(copy(localWindowRelativeClickPoint))
                .routeDecisionId(routeDecisionId)
                .rejectReason(reason)
                .build();
    }

    static RouteCloudDecision cloudRejectedNoClick(CloudDecisionResult cloudResult,
                                                   String localShadowDecision,
                                                   Point localWindowRelativeClickPoint,
                                                   String routeDecisionId,
                                                   String rejectReason) {
        return RouteCloudDecision.builder()
                .status(Status.CLOUD_REJECTED_NO_CLICK)
                .cloudResult(cloudResult)
                .localShadowDecision(localShadowDecision)
                .localWindowRelativeClickPoint(copy(localWindowRelativeClickPoint))
                .routeDecisionId(routeDecisionId)
                .rejectReason(rejectReason)
                .build();
    }

    public boolean isLocalPassthrough() {
        return status == Status.LOCAL_PASSTHROUGH;
    }

    public boolean isCloudExecuted() {
        return status == Status.CLOUD_EXECUTED;
    }

    public boolean isCloudNoClick() {
        return status == Status.CLOUD_NO_CLICK;
    }

    public boolean isNoClick() {
        return status == Status.CLOUD_NO_CLICK || status == Status.CLOUD_REJECTED_NO_CLICK;
    }

    public Point getLocalWindowRelativeClickPoint() {
        return copy(localWindowRelativeClickPoint);
    }

    public Point getCloudWindowRelativeClickPoint() {
        return copy(cloudWindowRelativeClickPoint);
    }

    public Point effectiveWindowRelativeClickPoint() {
        return isCloudExecuted() ? getCloudWindowRelativeClickPoint() : null;
    }

    private static Point copy(Point point) {
        return point == null ? null : new Point(point);
    }
}
