package com.bot.dhxy.window.runtime;

import com.bot.dhxy.core.GameContext;

/**
 * Verifies the UI-facing task run progress stored on a window runtime.
 */
public class WindowRuntimeTaskRunProgressTest {

    public static void main(String[] args) {
        WindowRuntimeContext runtime = new WindowRuntimeContext("window-progress", new GameContext());

        require("-".equals(runtime.getRunningTaskProgressText()),
                "task run progress should be blank until a task reports its own count");

        runtime.updateTaskRunProgress(3, 100);
        require("3/100".equals(runtime.getRunningTaskProgressText()),
                "task run progress should show completed runs over configured total");

        runtime.clearTaskRunProgress();
        require("-".equals(runtime.getRunningTaskProgressText()),
                "task run progress should clear back to dash");
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
