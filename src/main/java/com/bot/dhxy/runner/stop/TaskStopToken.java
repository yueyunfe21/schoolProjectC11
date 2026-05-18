package com.bot.dhxy.runner.stop;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 任务停止信号。
 */
public class TaskStopToken {

    private final AtomicBoolean stopRequested = new AtomicBoolean(false);
    private volatile LocalDateTime requestedAt;
    private volatile String reason;

    public void requestStop(String reason) {
        if (stopRequested.compareAndSet(false, true)) {
            this.requestedAt = LocalDateTime.now();
            this.reason = reason;
        }
    }

    public boolean isStopRequested() {
        return stopRequested.get();
    }

    public LocalDateTime getRequestedAt() {
        return requestedAt;
    }

    public String getReason() {
        return reason;
    }

    public void throwIfStopRequested() {
        if (isStopRequested()) {
            throw new TaskStopRequestedException(reason == null ? "任务已请求停止" : reason);
        }
    }
}
