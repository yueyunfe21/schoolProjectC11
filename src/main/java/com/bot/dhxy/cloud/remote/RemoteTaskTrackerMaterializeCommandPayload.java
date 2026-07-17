package com.bot.dhxy.cloud.remote;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

/** Closed materialization command; contains no image bytes, local path, HWND, or mint field. */
@Value
@Builder
@Jacksonized
public class RemoteTaskTrackerMaterializeCommandPayload {
    RemoteTaskTrackerReadOutcomePayload.ArtifactRef artifact;
    String observationDigest;
    DependencyLease dependencyLease;
    SelectedLink selectedLink;
    PreparedOperation preparedOperation;
    String targetKeyword;
    ValidationPolicy validationPolicy;

    public RemoteTaskTrackerMaterializeCommandPayload(
            RemoteTaskTrackerReadOutcomePayload.ArtifactRef artifact,
            String observationDigest,
            DependencyLease dependencyLease,
            SelectedLink selectedLink,
            PreparedOperation preparedOperation,
            String targetKeyword,
            ValidationPolicy validationPolicy) {
        this.artifact = RemoteTaskTrackerReadCommandPayload.requireNonNull(artifact, "artifact");
        this.observationDigest = RemoteTaskTrackerReadCommandPayload.sha256(
                observationDigest, "observationDigest");
        this.dependencyLease = RemoteTaskTrackerReadCommandPayload.requireNonNull(
                dependencyLease, "dependencyLease");
        this.selectedLink = RemoteTaskTrackerReadCommandPayload.requireNonNull(
                selectedLink, "selectedLink");
        this.preparedOperation = RemoteTaskTrackerReadCommandPayload.requireNonNull(
                preparedOperation, "preparedOperation");
        this.targetKeyword = RemoteTaskTrackerReadCommandPayload.requiredText(
                targetKeyword, "targetKeyword");
        this.validationPolicy = RemoteTaskTrackerReadCommandPayload.requireNonNull(
                validationPolicy, "validationPolicy");
    }

    @Value
    @Builder
    @Jacksonized
    public static class DependencyLease {
        String sourceReadActionId;
        RemoteSemanticAddress sourceReadSemanticAddress;
        String leaseDigest;

        public DependencyLease(
                String sourceReadActionId,
                RemoteSemanticAddress sourceReadSemanticAddress,
                String leaseDigest) {
            this.sourceReadActionId = RemoteTaskTrackerReadCommandPayload.requiredText(
                    sourceReadActionId, "dependencyLease.sourceReadActionId");
            this.sourceReadSemanticAddress = RemoteTaskTrackerReadCommandPayload.requireNonNull(
                    sourceReadSemanticAddress, "dependencyLease.sourceReadSemanticAddress");
            this.leaseDigest = RemoteTaskTrackerReadCommandPayload.sha256(
                    leaseDigest, "dependencyLease.leaseDigest");
        }
    }

    @Value
    @Builder
    @Jacksonized
    public static class SelectedLink {
        int stableIndex;
        RemoteCaptureRegion rect;
        WindowPoint click;

        public SelectedLink(int stableIndex, RemoteCaptureRegion rect, WindowPoint click) {
            RemoteTaskTrackerReadCommandPayload.require(stableIndex >= 0,
                    "selectedLink.stableIndex must be non-negative");
            this.stableIndex = stableIndex;
            this.rect = RemoteTaskTrackerReadCommandPayload.requireNonNull(rect, "selectedLink.rect");
            this.click = RemoteTaskTrackerReadCommandPayload.requireNonNull(click, "selectedLink.click");
            RemoteTaskTrackerReadCommandPayload.require(
                    rect.getCoordinateSpace() == RemoteCoordinateSpace.WINDOW_CLIENT_PX,
                    "selectedLink.rect must use WINDOW_CLIENT_PX");
            RemoteTaskTrackerReadCommandPayload.require(rect.getWidth() > 0 && rect.getHeight() > 0,
                    "selectedLink.rect dimensions must be positive");
            long right = Math.addExact((long) rect.getX(), rect.getWidth());
            long bottom = Math.addExact((long) rect.getY(), rect.getHeight());
            RemoteTaskTrackerReadCommandPayload.require(click.getX() >= rect.getX()
                            && click.getX() < right && click.getY() >= rect.getY()
                            && click.getY() < bottom,
                    "selectedLink.click must be inside selectedLink.rect");
        }
    }

    @Value
    @Builder
    @Jacksonized
    public static class WindowPoint {
        int x;
        int y;
    }

    public enum PreparedOperation {
        TASK_TRACKER_PATHING
    }

    public enum ValidationPolicy {
        SAME_FRAME_GREEN_FINGERPRINT
    }
}
