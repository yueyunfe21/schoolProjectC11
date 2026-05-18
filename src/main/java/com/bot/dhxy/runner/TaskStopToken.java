package com.bot.dhxy.runner;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 任务停止信号。
 *
 * 后面五环、抓鬼、修罗等任务内部循环都应该检查这个对象，
 * 而不是直接依赖 UI 或 TaskRunner。
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
