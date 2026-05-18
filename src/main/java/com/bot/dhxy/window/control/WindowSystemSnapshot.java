package com.bot.dhxy.window.control;

import com.bot.dhxy.window.execution.WindowTaskSnapshot;

import java.util.Collections;
import java.util.List;

/**
 * 多窗口系统整体状态快照。
 */
public class WindowSystemSnapshot {

    private final int registeredWindowCount;
    private final int runningWindowCount;
    private final int maxWindowCount;
    private final int remainingWindowCapacity;
    private final boolean capacityFull;
    private final List<WindowTaskSnapshot> windows;

    public WindowSystemSnapshot(int registeredWindowCount,
                                int runningWindowCount,
                                int maxWindowCount,
                                int remainingWindowCapacity,
                                List<WindowTaskSnapshot> windows) {
        this.registeredWindowCount = Math.max(registeredWindowCount, 0);
        this.runningWindowCount = Math.max(runningWindowCount, 0);
        this.maxWindowCount = Math.max(maxWindowCount, 0);
        this.remainingWindowCapacity = Math.max(remainingWindowCapacity, 0);
        this.capacityFull = this.maxWindowCount > 0 && this.registeredWindowCount >= this.maxWindowCount;
        this.windows = windows == null ? Collections.emptyList() : List.copyOf(windows);
    }

    public int getRegisteredWindowCount() { return registeredWindowCount; }

    public int getRunningWindowCount() { return runningWindowCount; }

    public int getIdleWindowCount() { return Math.max(registeredWindowCount - runningWindowCount, 0); }

    public int getMaxWindowCount() { return maxWindowCount; }

    public int getRemainingWindowCapacity() { return remainingWindowCapacity; }

    public boolean isCapacityFull() { return capacityFull; }

    public boolean hasWindows() { return !windows.isEmpty(); }

    public List<WindowTaskSnapshot> getWindows() { return windows; }
}
