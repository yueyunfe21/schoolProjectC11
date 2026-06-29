package com.bot.dhxy.runner.context;

import com.bot.dhxy.runner.policy.TaskRetryPolicy;
import com.bot.dhxy.runner.stop.TaskPauseToken;
import com.bot.dhxy.runner.stop.TaskStopToken;
import com.bot.dhxy.window.runtime.WindowRuntimeContext;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class TaskExecutionContext {

    private final String taskCode;
    private final String taskName;
    private final String requestedTaskCode;
    private final String requestedTaskName;
    private final String windowId;
    private final String windowRole;
    private final String nativeWindowHandle;
    private final String nativeWindowTitle;
    private final String nativeWindowClassName;
    private final long nativeWindowProcessId;
    private final int nativeWindowX;
    private final int nativeWindowY;
    private final int nativeWindowWidth;
    private final int nativeWindowHeight;
    private final TaskStopToken stopToken;
    private final TaskPauseToken pauseToken;
    private final TaskRetryPolicy retryPolicy;
    private final WindowRuntimeContext windowRuntimeContext;
    @Builder.Default
    private final long taskRunId = 0L;
    @Builder.Default
    private final TaskStartupMode startupMode = TaskStartupMode.NORMAL;
    private final LocalDateTime startedAt;

    public boolean isStopRequested() {
        return stopToken != null && stopToken.isStopRequested();
    }

    /**
     * Applies the shared cooperative stop/pause checkpoint for this task.
     *
     * @return milliseconds spent blocked by a user pause during this checkpoint, or {@code 0} when
     *         no pause wait occurred. Callers may ignore the value unless they own a wall-clock
     *         business timeout that should exclude user pause time.
     */
    public long throwIfStopRequested() {
        long pauseBlockedMs = 0L;
        if (stopToken != null) {
            stopToken.throwIfStopRequested();
        }
        if (pauseToken != null) {
            pauseBlockedMs = pauseToken.waitIfPaused(stopToken);
        }
        if (windowRuntimeContext != null) {
            pauseBlockedMs += windowRuntimeContext.waitIfIdentitySuspended(stopToken);
        }
        if (stopToken != null) {
            stopToken.throwIfStopRequested();
        }
        return pauseBlockedMs;
    }

    public boolean isPauseRequested() {
        return pauseToken != null && pauseToken.isPauseRequested();
    }

    public boolean hasWindow() {
        return windowId != null && !windowId.isBlank();
    }

    public boolean hasNativeWindow() {
        return nativeWindowHandle != null && !nativeWindowHandle.isBlank();
    }

    public boolean hasNativeWindowGeometry() {
        return nativeWindowWidth > 0 && nativeWindowHeight > 0;
    }

    public boolean isAfterCombatExitStartup() {
        return startupMode == TaskStartupMode.AFTER_COMBAT_EXIT_STARTUP;
    }

    public String getNativeWindowGeometryText() {
        if (!hasNativeWindowGeometry()) {
            return "-";
        }
        return nativeWindowX + "," + nativeWindowY + " " + nativeWindowWidth + "x" + nativeWindowHeight;
    }

    public String getLogPrefix() {
        if (!hasWindow()) {
            return "[window=unknown]";
        }
        String roleText = windowRole == null ? "UNKNOWN" : windowRole;
        if (!hasNativeWindow()) {
            return "[window=" + windowId + ", role=" + roleText + "]";
        }
        return "[window=" + windowId + ", role=" + roleText + ", hwnd=" + nativeWindowHandle + "]";
    }
}
