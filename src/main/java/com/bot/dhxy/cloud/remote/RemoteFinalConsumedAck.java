package com.bot.dhxy.cloud.remote;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/** Cloud-to-client proof that one exact final outcome was consumed by cloud business logic. */
@Value
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "contractVersion",
        "tenantId",
        "userId",
        "deviceId",
        "clientSessionId",
        "taskRunId",
        "runRevision",
        "observationMode",
        "window",
        "stopEpoch",
        "operation",
        "semanticAddress",
        "requestId",
        "actionId",
        "captureId",
        "requestDigest",
        "outcomeDigest",
        "executionState",
        "outcomeCode",
        "disposition",
        "trackerArtifactControl",
        "ackDigest"
})
public class RemoteFinalConsumedAck {
    private static final Pattern SHA256_HEX = Pattern.compile("[0-9a-fA-F]{64}");

    int contractVersion;
    String tenantId;
    String userId;
    String deviceId;
    String clientSessionId;
    String taskRunId;
    long runRevision;
    @JsonSetter(nulls = Nulls.FAIL)
    RemoteObservationMode observationMode;
    RemoteWindowBindingRef window;
    long stopEpoch;
    RemoteGameOperation operation;
    RemoteSemanticAddress semanticAddress;
    String requestId;
    String actionId;
    @JsonSetter(nulls = Nulls.FAIL)
    String captureId;
    String requestDigest;
    String outcomeDigest;
    RemoteExecutionState executionState;
    RemoteOutcomeCode outcomeCode;
    Disposition disposition;
    @JsonSetter(nulls = Nulls.FAIL)
    RemoteTaskTrackerFinalConsumedAttachment trackerArtifactControl;
    String ackDigest;

    @Builder
    @Jacksonized
    public RemoteFinalConsumedAck(
            int contractVersion,
            String tenantId,
            String userId,
            String deviceId,
            String clientSessionId,
            String taskRunId,
            long runRevision,
            RemoteObservationMode observationMode,
            RemoteWindowBindingRef window,
            long stopEpoch,
            RemoteGameOperation operation,
            RemoteSemanticAddress semanticAddress,
            String requestId,
            String actionId,
            String captureId,
            String requestDigest,
            String outcomeDigest,
            RemoteExecutionState executionState,
            RemoteOutcomeCode outcomeCode,
            Disposition disposition,
            RemoteTaskTrackerFinalConsumedAttachment trackerArtifactControl,
            String ackDigest) {
        if (contractVersion != 1) {
            throw new IllegalArgumentException("contractVersion must be 1");
        }
        this.contractVersion = contractVersion;
        this.tenantId = requiredText(tenantId, "tenantId");
        this.userId = requiredText(userId, "userId");
        this.deviceId = requiredText(deviceId, "deviceId");
        this.clientSessionId = requiredText(clientSessionId, "clientSessionId");
        this.taskRunId = requiredText(taskRunId, "taskRunId");
        if (runRevision < 0L) {
            throw new IllegalArgumentException("runRevision must be non-negative");
        }
        this.runRevision = runRevision;
        this.observationMode = observationMode;
        this.window = Objects.requireNonNull(window, "window must not be null");
        if (stopEpoch < 0L) {
            throw new IllegalArgumentException("stopEpoch must be non-negative");
        }
        this.stopEpoch = stopEpoch;
        this.operation = Objects.requireNonNull(operation, "operation must not be null");
        this.semanticAddress = Objects.requireNonNull(
                semanticAddress, "semanticAddress must not be null");
        this.requestId = requiredText(requestId, "requestId");
        this.actionId = requiredText(actionId, "actionId");
        if (operation == RemoteGameOperation.CAPTURE
                || operation == RemoteGameOperation.TASK_TRACKER_READ) {
            this.captureId = requiredText(captureId, "captureId");
        } else {
            require(captureId == null,
                    "captureId is only allowed for CAPTURE and TASK_TRACKER_READ");
            this.captureId = null;
        }
        this.requestDigest = sha256(requestDigest, "requestDigest");
        this.outcomeDigest = sha256(outcomeDigest, "outcomeDigest");
        this.executionState = Objects.requireNonNull(
                executionState, "executionState must not be null");
        require(executionState != RemoteExecutionState.UNKNOWN,
                "executionState must not be UNKNOWN");
        this.outcomeCode = Objects.requireNonNull(outcomeCode, "outcomeCode must not be null");
        this.disposition = Objects.requireNonNull(disposition, "disposition must not be null");
        this.trackerArtifactControl = trackerArtifactControl;
        this.ackDigest = sha256(ackDigest, "ackDigest");

        require(observationMode == null
                        || operation == RemoteGameOperation.WINDOW_FACT
                        || operation == RemoteGameOperation.CAPTURE,
                "observationMode is only allowed for WINDOW_FACT and CAPTURE");
        if (observationMode != null) {
            require(observationMode == RemoteObservationMode.PAUSED_READ_ONLY,
                    "observationMode must be PAUSED_READ_ONLY when present");
            require(semanticAddress.getAttempt() == 0,
                    "PAUSED_READ_ONLY semanticAddress attempt must be 0");
            require(disposition == Disposition.OCCURRENCE_COMPLETE,
                    "PAUSED_READ_ONLY disposition must be OCCURRENCE_COMPLETE");
        }
        if (disposition == Disposition.ATTEMPT_RETIRED_FOR_RENEWAL) {
            require(observationMode == null,
                    "ATTEMPT_RETIRED_FOR_RENEWAL is only allowed for ACTIVE requests");
            require(executionState == RemoteExecutionState.NOT_EXECUTED,
                    "ATTEMPT_RETIRED_FOR_RENEWAL requires NOT_EXECUTED");
        }
        validateTrackerArtifactControl(operation, semanticAddress, actionId, executionState,
                disposition, trackerArtifactControl);
    }

    /** Source-compatible constructor for the three pre-tracker operations. */
    public RemoteFinalConsumedAck(
            int contractVersion,
            String tenantId,
            String userId,
            String deviceId,
            String clientSessionId,
            String taskRunId,
            long runRevision,
            RemoteObservationMode observationMode,
            RemoteWindowBindingRef window,
            long stopEpoch,
            RemoteGameOperation operation,
            RemoteSemanticAddress semanticAddress,
            String requestId,
            String actionId,
            String captureId,
            String requestDigest,
            String outcomeDigest,
            RemoteExecutionState executionState,
            RemoteOutcomeCode outcomeCode,
            Disposition disposition,
            String ackDigest) {
        this(contractVersion, tenantId, userId, deviceId, clientSessionId, taskRunId,
                runRevision, observationMode, window, stopEpoch, operation, semanticAddress,
                requestId, actionId, captureId, requestDigest, outcomeDigest, executionState,
                outcomeCode, disposition, null, ackDigest);
    }

    private static void validateTrackerArtifactControl(
            RemoteGameOperation operation,
            RemoteSemanticAddress semanticAddress,
            String actionId,
            RemoteExecutionState executionState,
            Disposition disposition,
            RemoteTaskTrackerFinalConsumedAttachment control) {
        if (operation == RemoteGameOperation.CAPTURE
                || operation == RemoteGameOperation.WINDOW_FACT
                || operation == RemoteGameOperation.EXECUTE_INPUT_BUNDLE
                || operation == RemoteGameOperation.LOCAL_MACRO) {
            require(control == null,
                    "pre-tracker and local-macro operations forbid trackerArtifactControl");
            return;
        }
        if (operation == RemoteGameOperation.TASK_TRACKER_READ) {
            if (executionState == RemoteExecutionState.OBSERVED) {
                Objects.requireNonNull(control, "trackerArtifactControl must not be null");
                require(control.getDirective()
                                == RemoteTaskTrackerFinalConsumedAttachment.Directive.RELEASE_AFTER_READ
                                || control.getDirective()
                                == RemoteTaskTrackerFinalConsumedAttachment.Directive.RETAIN_FOR_MATERIALIZE,
                        "OBSERVED tracker read requires a read final directive");
                require(control.getSourceReadActionId().equals(actionId)
                                && control.getSourceReadSemanticAddress().equals(semanticAddress),
                        "tracker read control must identify the outer read action");
            } else {
                require(control == null,
                        "non-OBSERVED tracker read forbids trackerArtifactControl");
            }
            return;
        }
        Objects.requireNonNull(control, "trackerArtifactControl must not be null");
        require(control.getMaterializeActionId().equals(actionId)
                        && control.getMaterializeSemanticAddress().equals(semanticAddress),
                "tracker materialize control must identify the outer materialize action");
        if (disposition == Disposition.ATTEMPT_RETIRED_FOR_RENEWAL) {
            require(control.getDirective()
                            == RemoteTaskTrackerFinalConsumedAttachment.Directive.KEEP_FOR_MATERIALIZE_RENEWAL,
                    "materialize renewal requires KEEP_FOR_MATERIALIZE_RENEWAL");
        } else {
            require(control.getDirective()
                            == RemoteTaskTrackerFinalConsumedAttachment.Directive.RELEASE_AFTER_MATERIALIZE
                            || control.getDirective()
                            == RemoteTaskTrackerFinalConsumedAttachment.Directive.RELEASE_TRUSTED_CANCEL,
                    "materialize final requires a release directive");
            require(control.getDirective()
                            != RemoteTaskTrackerFinalConsumedAttachment.Directive.RELEASE_TRUSTED_CANCEL
                            || executionState == RemoteExecutionState.NOT_EXECUTED,
                    "RELEASE_TRUSTED_CANCEL requires NOT_EXECUTED");
        }
    }

    private static String requiredText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value.trim();
    }

    private static String sha256(String value, String field) {
        if (value == null || !SHA256_HEX.matcher(value).matches()) {
            throw new IllegalArgumentException(field + " must be a SHA-256 hex string");
        }
        return value.toLowerCase(Locale.ROOT);
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalArgumentException(message);
        }
    }

    public static class RemoteFinalConsumedAckBuilder {
        @JsonSetter(value = "observationMode", nulls = Nulls.FAIL)
        public RemoteFinalConsumedAckBuilder observationMode(
                RemoteObservationMode observationMode) {
            this.observationMode = observationMode;
            return this;
        }

        @JsonSetter(value = "captureId", nulls = Nulls.FAIL)
        public RemoteFinalConsumedAckBuilder captureId(String captureId) {
            this.captureId = captureId;
            return this;
        }

        @JsonSetter(value = "trackerArtifactControl", nulls = Nulls.FAIL)
        public RemoteFinalConsumedAckBuilder trackerArtifactControl(
                RemoteTaskTrackerFinalConsumedAttachment trackerArtifactControl) {
            this.trackerArtifactControl = trackerArtifactControl;
            return this;
        }
    }

    public enum Disposition {
        OCCURRENCE_COMPLETE,
        ATTEMPT_RETIRED_FOR_RENEWAL
    }
}
