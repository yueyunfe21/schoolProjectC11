package com.bot.dhxy.runner;

import lombok.Builder;
import lombok.Getter;

/**
 * 任务步骤重试策略。
 */
@Getter
@Builder
public class TaskRetryPolicy {

    /** 最大重试次数。 */
    private final int maxRetries;

    /** 每次重试前等待毫秒数。 */
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
