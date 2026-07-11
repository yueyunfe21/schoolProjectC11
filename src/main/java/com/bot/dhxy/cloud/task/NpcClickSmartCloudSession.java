package com.bot.dhxy.cloud.task;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class NpcClickSmartCloudSession {
    @Builder.Default
    Status status = Status.REQUIRED_FAILURE;
    String sessionId;
    String windowId;
    String taskRunId;
    String reason;

    public boolean accepted() {
        return status == Status.STARTED
                && sessionId != null && !sessionId.isBlank()
                && windowId != null && !windowId.isBlank()
                && taskRunId != null && !taskRunId.isBlank();
    }

    public enum Status {
        STARTED,
        DISABLED,
        REQUIRED_FAILURE
    }
}
