package com.bot.dhxy.window.policy;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 多窗口容量策略。
 *
 * 后续不同版本/不同马可以通过配置限制最大可运行窗口数。
 */
@Component
public class WindowCapacityPolicy {

    private final int maxWindowCount;

    public WindowCapacityPolicy(@Value("${dhxy.window.max-count:5}") int maxWindowCount) {
        this.maxWindowCount = Math.max(maxWindowCount, 1);
    }

    public int getMaxWindowCount() {
        return maxWindowCount;
    }

    public boolean canRegister(int currentCount) {
        return remainingCapacity(currentCount) > 0;
    }

    public int remainingCapacity(int currentCount) {
        return Math.max(maxWindowCount - Math.max(currentCount, 0), 0);
    }

    public WindowCapacityDecision evaluate(String windowId, int currentCount, boolean alreadyRegistered) {
        if (alreadyRegistered) {
            return WindowCapacityDecision.allowed(windowId, "窗口已存在，允许刷新");
        }
        if (canRegister(currentCount)) {
            return WindowCapacityDecision.allowed(windowId, "容量允许注册");
        }
        return WindowCapacityDecision.rejected(windowId, "已达到最大窗口容量：" + maxWindowCount);
    }
}
