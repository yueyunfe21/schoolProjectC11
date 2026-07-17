package com.bot.dhxy.cloud.remote;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/** Closed wire payload for one mechanical tracker-panel read. */
@Value
@Jacksonized
public class RemoteTaskTrackerReadCommandPayload {
    private static final Pattern ARTIFACT_ID = Pattern.compile("tpa1:[0-9a-fA-F]{64}");

    String captureId;
    ReadProfile readProfile;
    Source source;
    boolean allowPanelReposition;

    @Builder
    public RemoteTaskTrackerReadCommandPayload(
            String captureId,
            ReadProfile readProfile,
            Source source,
            boolean allowPanelReposition) {
        this.captureId = requiredText(captureId, "captureId");
        this.readProfile = requireNonNull(readProfile, "readProfile");
        this.source = requireNonNull(source, "source");
        require(!allowPanelReposition || readProfile.allowsPanelReposition(),
                "readProfile does not allow panel reposition");
        require(source.getKind() != SourceKind.EXISTING_CAPTURE_ARTIFACT
                        || !allowPanelReposition,
                "EXISTING_CAPTURE_ARTIFACT forbids panel reposition");
        this.allowPanelReposition = allowPanelReposition;
    }

    public enum ReadProfile {
        WUHUAN_TITLE_GATE(65, 3_683_356L),
        WUHUAN_PATHING(65, 3_683_356L),
        WUBEI_DETAIL(65, 3_683_356L),
        XIULUO_DETAIL(40, 3_665_856L);

        private final List<FrameBound> retainedFrameBounds;
        private final long maxLocalWorkingArgbBytes;

        ReadProfile(int detailHeight, long maxLocalWorkingArgbBytes) {
            this.retainedFrameBounds = List.of(
                    new FrameBound(FrameRole.PRIMARY_PANEL, 182, 338, 524_288),
                    new FrameBound(FrameRole.DETAIL_BLOCK, 175, detailHeight, 524_288));
            this.maxLocalWorkingArgbBytes = maxLocalWorkingArgbBytes;
        }

        public int maxArtifactCount() {
            return 1;
        }

        public int maxRetainedFrameCount() {
            return 2;
        }

        public List<FrameBound> retainedFrameBounds() {
            return retainedFrameBounds;
        }

        public long maxRetainedEncodedBytes() {
            return 1_048_576L;
        }

        public int maxLocalWorkingImageCount() {
            return 4;
        }

        public long maxLocalWorkingArgbBytes() {
            return maxLocalWorkingArgbBytes;
        }

        public int maxCloudTransientCopyCount() {
            return 4;
        }

        public long maxCloudTransientBytes() {
            return 7_820_640L;
        }

        public boolean allowsPanelReposition() {
            return true;
        }

        public FrameBound boundFor(FrameRole role) {
            return retainedFrameBounds.stream()
                    .filter(bound -> bound.role() == role)
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("unsupported frame role " + role));
        }
    }

    public record FrameBound(FrameRole role, int maxWidth, int maxHeight, int maxEncodedBytes) {
        public FrameBound {
            requireNonNull(role, "frameBound.role");
            require(maxWidth > 0 && maxHeight > 0 && maxEncodedBytes > 0,
                    "frame bounds must be positive");
        }
    }

    public enum FrameRole {
        PRIMARY_PANEL,
        DETAIL_BLOCK
    }

    public enum SourceKind {
        LIVE_BOUND_WINDOW,
        EXISTING_CAPTURE_ARTIFACT
    }

    @Value
    @Builder
    @Jacksonized
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Source {
        SourceKind kind;
        @JsonSetter(nulls = Nulls.FAIL)
        ExistingCaptureArtifact sourceArtifact;

        public Source(SourceKind kind, ExistingCaptureArtifact sourceArtifact) {
            this.kind = requireNonNull(kind, "source.kind");
            if (kind == SourceKind.LIVE_BOUND_WINDOW) {
                require(sourceArtifact == null, "LIVE_BOUND_WINDOW forbids sourceArtifact");
                this.sourceArtifact = null;
            } else {
                this.sourceArtifact = requireNonNull(sourceArtifact, "source.sourceArtifact");
            }
        }

        public static class SourceBuilder {
            @JsonSetter(value = "sourceArtifact", nulls = Nulls.FAIL)
            public SourceBuilder sourceArtifact(ExistingCaptureArtifact sourceArtifact) {
                this.sourceArtifact = sourceArtifact;
                return this;
            }
        }
    }

    @Value
    @Builder
    @Jacksonized
    public static class ExistingCaptureArtifact {
        String captureId;
        String imageSha256;
        String artifactId;

        public ExistingCaptureArtifact(String captureId, String imageSha256, String artifactId) {
            this.captureId = requiredText(captureId, "sourceArtifact.captureId");
            this.imageSha256 = sha256(imageSha256, "sourceArtifact.imageSha256");
            this.artifactId = requireArtifactId(artifactId, "sourceArtifact.artifactId");
        }
    }

    static String requireArtifactId(String value, String field) {
        String normalized = requiredText(value, field);
        if (!ARTIFACT_ID.matcher(normalized).matches()) {
            throw new IllegalArgumentException(field + " must be tpa1 followed by a SHA-256 hex string");
        }
        return normalized.toLowerCase(Locale.ROOT);
    }

    static String sha256(String value, String field) {
        if (value == null || !value.matches("[0-9a-fA-F]{64}")) {
            throw new IllegalArgumentException(field + " must be a SHA-256 hex string");
        }
        return value.toLowerCase(Locale.ROOT);
    }

    static String requiredText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value.trim();
    }

    static <T> T requireNonNull(T value, String field) {
        if (value == null) {
            throw new IllegalArgumentException(field + " must not be null");
        }
        return value;
    }

    static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalArgumentException(message);
        }
    }
}
