package com.bot.dhxy.cloud.remote;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

/** Mirror payload for one typed tracker read outcome. */
@Value
@Builder
@Jacksonized
public class RemoteTaskTrackerReadOutcomePayload {
    String captureId;
    RemoteTaskTrackerReadCommandPayload.ReadProfile readProfile;
    RemoteTaskTrackerReadCommandPayload.Source source;
    ArtifactRef artifact;
    List<Frame> frames;
    MechanicalFact mechanicalFact;
    RemoteObservedWindowBinding observedWindow;

    public RemoteTaskTrackerReadOutcomePayload(
            String captureId,
            RemoteTaskTrackerReadCommandPayload.ReadProfile readProfile,
            RemoteTaskTrackerReadCommandPayload.Source source,
            ArtifactRef artifact,
            List<Frame> frames,
            MechanicalFact mechanicalFact,
            RemoteObservedWindowBinding observedWindow) {
        this.captureId = RemoteTaskTrackerReadCommandPayload.requiredText(captureId, "captureId");
        this.readProfile = readProfile;
        this.source = source;
        this.artifact = artifact;
        this.frames = frames == null ? null : List.copyOf(frames);
        this.mechanicalFact = mechanicalFact;
        this.observedWindow = observedWindow;
        if (this.frames != null) {
            RemoteTaskTrackerReadCommandPayload.require(readProfile != null,
                    "readProfile is required when frames are present");
            validateFrames(readProfile, this.frames);
        }
    }

    private static void validateFrames(
            RemoteTaskTrackerReadCommandPayload.ReadProfile profile,
            List<Frame> values) {
        RemoteTaskTrackerReadCommandPayload.require(!values.isEmpty()
                        && values.size() <= profile.maxRetainedFrameCount(),
                "frames size must be between 1 and the readProfile maximum");
        long total = 0L;
        for (int i = 0; i < values.size(); i++) {
            Frame frame = RemoteTaskTrackerReadCommandPayload.requireNonNull(
                    values.get(i), "frames[" + i + "]");
            RemoteTaskTrackerReadCommandPayload.require(frame.getOrdinal() == i,
                    "frame ordinal must equal its ordered index");
            RemoteTaskTrackerReadCommandPayload.FrameRole expected = i == 0
                    ? RemoteTaskTrackerReadCommandPayload.FrameRole.PRIMARY_PANEL
                    : RemoteTaskTrackerReadCommandPayload.FrameRole.DETAIL_BLOCK;
            RemoteTaskTrackerReadCommandPayload.require(frame.getRole() == expected,
                    "frame role does not match its ordered index");
            RemoteTaskTrackerReadCommandPayload.FrameBound bound = profile.boundFor(frame.getRole());
            RemoteTaskTrackerReadCommandPayload.require(
                    frame.getRegion().getWidth() <= bound.maxWidth()
                            && frame.getRegion().getHeight() <= bound.maxHeight(),
                    "frame dimensions exceed readProfile bounds");
            RemoteTaskTrackerReadCommandPayload.require(
                    frame.getImageBytes().length <= bound.maxEncodedBytes(),
                    "frame encoded bytes exceed readProfile bounds");
            total = Math.addExact(total, frame.getImageBytes().length);
        }
        RemoteTaskTrackerReadCommandPayload.require(total <= profile.maxRetainedEncodedBytes(),
                "artifact encoded bytes exceed readProfile bounds");
    }

    @Value
    @Builder
    @Jacksonized
    public static class ArtifactRef {
        String artifactId;
        String artifactDigest;

        public ArtifactRef(String artifactId, String artifactDigest) {
            this.artifactId = RemoteTaskTrackerReadCommandPayload.requireArtifactId(
                    artifactId, "artifact.artifactId");
            this.artifactDigest = RemoteTaskTrackerReadCommandPayload.sha256(
                    artifactDigest, "artifact.artifactDigest");
        }
    }

    @Value
    @Jacksonized
    public static class Frame {
        int ordinal;
        RemoteTaskTrackerReadCommandPayload.FrameRole role;
        byte[] imageBytes;
        String imageSha256;
        RemoteCaptureRegion region;

        @Builder
        public Frame(
                int ordinal,
                RemoteTaskTrackerReadCommandPayload.FrameRole role,
                byte[] imageBytes,
                String imageSha256,
                RemoteCaptureRegion region) {
            RemoteTaskTrackerReadCommandPayload.require(ordinal >= 0,
                    "frame.ordinal must be non-negative");
            this.ordinal = ordinal;
            this.role = RemoteTaskTrackerReadCommandPayload.requireNonNull(role, "frame.role");
            RemoteTaskTrackerReadCommandPayload.require(imageBytes != null && imageBytes.length > 0,
                    "frame.imageBytes must not be empty");
            this.imageBytes = imageBytes.clone();
            this.imageSha256 = RemoteTaskTrackerReadCommandPayload.sha256(
                    imageSha256, "frame.imageSha256");
            RemoteTaskTrackerReadCommandPayload.require(
                    sha256Hex(this.imageBytes).equals(this.imageSha256),
                    "frame.imageSha256 does not match imageBytes");
            this.region = RemoteTaskTrackerReadCommandPayload.requireNonNull(region, "frame.region");
            RemoteTaskTrackerReadCommandPayload.require(
                    region.getCoordinateSpace() == RemoteCoordinateSpace.WINDOW_CLIENT_PX,
                    "frame.region must use WINDOW_CLIENT_PX");
            RemoteTaskTrackerReadCommandPayload.require(
                    region.getWidth() > 0 && region.getHeight() > 0,
                    "frame.region dimensions must be positive");
        }

        public byte[] getImageBytes() {
            return imageBytes.clone();
        }
    }

    @Value
    @Builder
    @Jacksonized
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class MechanicalFact {
        String templateId;
        String taskKey;
        double templateScore;
        TitleDisposition titleDisposition;
        String panelFingerprint;
        long captureOccurrence;

        public MechanicalFact(
                String templateId,
                String taskKey,
                double templateScore,
                TitleDisposition titleDisposition,
                String panelFingerprint,
                long captureOccurrence) {
            this.templateId = RemoteTaskTrackerReadCommandPayload.requiredText(
                    templateId, "mechanicalFact.templateId");
            this.taskKey = taskKey == null ? null
                    : RemoteTaskTrackerReadCommandPayload.requiredText(
                            taskKey, "mechanicalFact.taskKey");
            RemoteTaskTrackerReadCommandPayload.require(Double.isFinite(templateScore)
                            && templateScore >= 0.0d && templateScore <= 1.0d,
                    "mechanicalFact.templateScore must be finite and between 0 and 1");
            this.templateScore = templateScore;
            this.titleDisposition = RemoteTaskTrackerReadCommandPayload.requireNonNull(
                    titleDisposition, "mechanicalFact.titleDisposition");
            this.panelFingerprint = RemoteTaskTrackerReadCommandPayload.sha256(
                    panelFingerprint, "mechanicalFact.panelFingerprint");
            RemoteTaskTrackerReadCommandPayload.require(captureOccurrence >= 0L,
                    "mechanicalFact.captureOccurrence must be non-negative");
            this.captureOccurrence = captureOccurrence;
            RemoteTaskTrackerReadCommandPayload.require(
                    titleDisposition != TitleDisposition.MISSED || taskKey == null,
                    "MISSED title disposition forbids taskKey");
        }
    }

    public enum TitleDisposition {
        HIT,
        MISSED,
        NOT_APPLICABLE
    }

    private static String sha256Hex(byte[] bytes) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(bytes);
            StringBuilder hex = new StringBuilder(digest.length * 2);
            for (byte value : digest) {
                hex.append(Character.forDigit((value >>> 4) & 0x0f, 16));
                hex.append(Character.forDigit(value & 0x0f, 16));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }
}
