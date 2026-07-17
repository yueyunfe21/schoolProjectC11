package com.bot.dhxy.cloud.remote;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

/** DHXY mirror of the tracker artifact retain/release control bound into ackDigest. */
@Value
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "attachmentVersion",
        "directive",
        "artifactId",
        "artifactDigest",
        "sourceReadActionId",
        "sourceReadSemanticAddress",
        "materializeActionId",
        "materializeSemanticAddress",
        "leaseDigest"
})
public class RemoteTaskTrackerFinalConsumedAttachment {
    int attachmentVersion;
    Directive directive;
    String artifactId;
    String artifactDigest;
    String sourceReadActionId;
    RemoteSemanticAddress sourceReadSemanticAddress;
    @JsonSetter(nulls = Nulls.FAIL)
    String materializeActionId;
    @JsonSetter(nulls = Nulls.FAIL)
    RemoteSemanticAddress materializeSemanticAddress;
    @JsonSetter(nulls = Nulls.FAIL)
    String leaseDigest;

    @Builder
    @Jacksonized
    public RemoteTaskTrackerFinalConsumedAttachment(
            int attachmentVersion,
            Directive directive,
            String artifactId,
            String artifactDigest,
            String sourceReadActionId,
            RemoteSemanticAddress sourceReadSemanticAddress,
            String materializeActionId,
            RemoteSemanticAddress materializeSemanticAddress,
            String leaseDigest) {
        RemoteTaskTrackerReadCommandPayload.require(attachmentVersion == 1,
                "attachmentVersion must be 1");
        this.attachmentVersion = attachmentVersion;
        this.directive = RemoteTaskTrackerReadCommandPayload.requireNonNull(directive, "directive");
        this.artifactId = RemoteTaskTrackerReadCommandPayload.requireArtifactId(
                artifactId, "artifactId");
        this.artifactDigest = RemoteTaskTrackerReadCommandPayload.sha256(
                artifactDigest, "artifactDigest");
        this.sourceReadActionId = RemoteTaskTrackerReadCommandPayload.requiredText(
                sourceReadActionId, "sourceReadActionId");
        this.sourceReadSemanticAddress = RemoteTaskTrackerReadCommandPayload.requireNonNull(
                sourceReadSemanticAddress, "sourceReadSemanticAddress");
        if (directive == Directive.RELEASE_AFTER_READ) {
            RemoteTaskTrackerReadCommandPayload.require(materializeActionId == null
                            && materializeSemanticAddress == null && leaseDigest == null,
                    "RELEASE_AFTER_READ forbids materialize lease fields");
            this.materializeActionId = null;
            this.materializeSemanticAddress = null;
            this.leaseDigest = null;
        } else {
            this.materializeActionId = RemoteTaskTrackerReadCommandPayload.requiredText(
                    materializeActionId, "materializeActionId");
            this.materializeSemanticAddress = RemoteTaskTrackerReadCommandPayload.requireNonNull(
                    materializeSemanticAddress, "materializeSemanticAddress");
            this.leaseDigest = RemoteTaskTrackerReadCommandPayload.sha256(
                    leaseDigest, "leaseDigest");
            String expected = new RemoteProtocolDigests().computeTaskTrackerLeaseDigest(
                    this.artifactId, this.artifactDigest, this.sourceReadActionId,
                    this.sourceReadSemanticAddress, this.materializeActionId,
                    this.materializeSemanticAddress);
            RemoteTaskTrackerReadCommandPayload.require(expected.equals(this.leaseDigest),
                    "leaseDigest does not match the exact artifact/read/materialize identities");
        }
    }

    public static class RemoteTaskTrackerFinalConsumedAttachmentBuilder {
        @JsonSetter(value = "materializeActionId", nulls = Nulls.FAIL)
        public RemoteTaskTrackerFinalConsumedAttachmentBuilder materializeActionId(String value) {
            this.materializeActionId = value;
            return this;
        }

        @JsonSetter(value = "materializeSemanticAddress", nulls = Nulls.FAIL)
        public RemoteTaskTrackerFinalConsumedAttachmentBuilder materializeSemanticAddress(
                RemoteSemanticAddress value) {
            this.materializeSemanticAddress = value;
            return this;
        }

        @JsonSetter(value = "leaseDigest", nulls = Nulls.FAIL)
        public RemoteTaskTrackerFinalConsumedAttachmentBuilder leaseDigest(String value) {
            this.leaseDigest = value;
            return this;
        }
    }

    public enum Directive {
        RELEASE_AFTER_READ,
        RETAIN_FOR_MATERIALIZE,
        KEEP_FOR_MATERIALIZE_RENEWAL,
        RELEASE_AFTER_MATERIALIZE,
        RELEASE_TRUSTED_CANCEL
    }
}
