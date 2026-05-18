package com.bot.dhxy.runner.policy;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TaskRetryPolicy {

    private final int maxRetries;
    private final long delayMillis;

    public static TaskRetryPolicy none() {
        return TaskRetryPolicy.builder()
                .maxRetries(0)
                .delayMillis(0)
                .build();
    }

    public static TaskRetryPolicy defaultPolicy() {
        return TaskRetryPolicy.builder()
                .maxRetries(2)
                .delayMillis(500)
                .build();
    }

    public boolean canRetry(int attemptedRetries) {
        return attemptedRetries < maxRetries;
    }
}
