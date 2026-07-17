package com.bot.dhxy.cloud.remote;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

/** Mirror payload for a typed prepared-action publication result. */
@Value
@Builder
@Jacksonized
public class RemoteTaskTrackerMaterializeOutcomePayload {
    RemoteTaskTrackerReadOutcomePayload.ArtifactRef artifact;
    String observationDigest;
    String preparedActionId;
    PublishDisposition publishDisposition;
    String validationFingerprintDigest;
    RemoteObservedWindowBinding observedWindow;

    public RemoteTaskTrackerMaterializeOutcomePayload(
            RemoteTaskTrackerReadOutcomePayload.ArtifactRef artifact,
            String observationDigest,
            String preparedActionId,
            PublishDisposition publishDisposition,
            String validationFingerprintDigest,
            RemoteObservedWindowBinding observedWindow) {
        this.artifact = artifact;
        this.observationDigest = observationDigest == null ? null
                : RemoteTaskTrackerReadCommandPayload.sha256(observationDigest, "observationDigest");
        this.preparedActionId = preparedActionId == null ? null
                : RemoteTaskTrackerReadCommandPayload.requiredText(
                        preparedActionId, "preparedActionId");
        this.publishDisposition = publishDisposition;
        this.validationFingerprintDigest = validationFingerprintDigest == null ? null
                : RemoteTaskTrackerReadCommandPayload.sha256(
                        validationFingerprintDigest, "validationFingerprintDigest");
        this.observedWindow = observedWindow;
    }

    public enum PublishDisposition {
        PUBLISHED,
        ALREADY_PUBLISHED,
        DEPENDENCY_NOT_READY,
        STALE,
        SAFETY_REJECTED
    }
}
