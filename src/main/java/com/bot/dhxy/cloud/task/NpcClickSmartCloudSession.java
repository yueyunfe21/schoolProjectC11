package com.bot.dhxy.cloud.task;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;

@Value
public class NpcClickSmartCloudSession {
    Status status;
    String sessionId;
    String windowId;
    String taskRunId;
    String reason;

    @Builder
    @JsonCreator
    public NpcClickSmartCloudSession(
            @JsonProperty("status") Status status,
            @JsonProperty("sessionId") String sessionId,
            @JsonProperty("windowId") String windowId,
            @JsonProperty("taskRunId") String taskRunId,
            @JsonProperty("reason") String reason
    ) {
        this.status = status == null ? Status.REQUIRED_FAILURE : status;
        this.sessionId = sessionId;
        this.windowId = windowId;
        this.taskRunId = taskRunId;
        this.reason = reason;
    }

    public boolean accepted() {
        return status == Status.STARTED
                && sessionId != null
                && !sessionId.isBlank()
                && windowId != null
                && !windowId.isBlank()
                && taskRunId != null
                && !taskRunId.isBlank();
    }

    public enum Status {
        STARTED,
        DISABLED,
        REQUIRED_FAILURE
    }
}
