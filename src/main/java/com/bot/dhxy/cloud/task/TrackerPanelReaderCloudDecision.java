package com.bot.dhxy.cloud.task;

import com.bot.dhxy.cloud.decision.CloudDecisionResult;
import lombok.Builder;
import lombok.Value;

import java.awt.Point;
import java.util.List;

@Value
@Builder
public class TrackerPanelReaderCloudDecision {

    public enum Status {
        DISABLED,
        CLOUD_FOUND,
        CLOUD_NO_ACTION,
        REQUIRED_FAILURE
    }

    @Builder.Default
    Status status = Status.DISABLED;
    String action;
    String taskKey;
    String targetName;
    String yellowText;
    Point clickWindowRelative;
    @Builder.Default
    List<Link> links = List.of();
    String reason;
    CloudDecisionResult cloudResult;

    public boolean found() {
        return status == Status.CLOUD_FOUND;
    }

    public boolean noAction() {
        return status == Status.CLOUD_NO_ACTION
                || status == Status.REQUIRED_FAILURE
                || status == Status.DISABLED;
    }

    public boolean clickAction() {
        return found() && "CLICK_TRACKER_LINK".equals(action) && clickWindowRelative != null;
    }

    public Point clickWindowRelative() {
        return clickWindowRelative == null ? null : new Point(clickWindowRelative);
    }

    public List<Link> links() {
        return links == null ? List.of() : List.copyOf(links);
    }

    public String taskKey() {
        return taskKey;
    }

    public String reason() {
        return reason;
    }

    public CloudDecisionResult cloudResult() {
        return cloudResult;
    }

    @Value
    @Builder
    public static class Link {
        int index;
        Point clickWindowRelative;
        String windowRelativeRect;
        String targetMapName;

        public Point clickWindowRelative() {
            return clickWindowRelative == null ? null : new Point(clickWindowRelative);
        }
    }
}
