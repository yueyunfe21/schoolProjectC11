package com.bot.dhxy.window.execution;

import com.bot.dhxy.model.PlayerCharacter;
import com.bot.dhxy.task.model.TaskType;
import com.bot.dhxy.window.model.WindowRuntimeStatus;
import com.bot.dhxy.window.runtime.WindowRegistrationRequest;
import com.bot.dhxy.window.runtime.WindowRuntimeContext;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Registered-window runtime handle for the HTTPS turn client.
 *
 * <p>This class deliberately owns no task factory, executor, phase machine, watcher, OCR decision,
 * or local {@code GameTask.execute} path. It retains the exact window context and projects the one
 * remote loop's lifecycle into the existing UI snapshot.</p>
 */
public final class WindowTaskRunner {

    private final WindowRuntimeContext windowContext;
    private volatile boolean remoteRunning;
    private volatile boolean remotePaused;
    private volatile WindowTaskQueue remoteQueue = WindowTaskQueue.empty();
    private volatile LocalDateTime remoteStartedAt;
    private volatile RemoteTaskHandle remoteTaskHandle;
    private volatile boolean shutdown;

    public WindowTaskRunner(WindowRuntimeContext windowContext) {
        this.windowContext = Objects.requireNonNull(windowContext, "windowContext");
    }

    public WindowRuntimeContext getWindowContext() {
        return windowContext;
    }

    public void refreshRegistration(WindowRegistrationRequest request) {
        if (request != null) {
            windowContext.applyRegistration(request, !isRunning());
        }
    }

    public boolean isRunning() {
        return remoteRunning;
    }

    public boolean canAcceptTaskQueue() {
        return !shutdown && !remoteRunning;
    }

    public boolean isShutdown() {
        return shutdown;
    }

    public void markRemoteStarted(WindowTaskQueue queue) {
        WindowTaskQueue safeQueue = queue == null ? WindowTaskQueue.empty() : queue;
        remoteQueue = safeQueue;
        remoteRunning = true;
        remotePaused = false;
        remoteStartedAt = LocalDateTime.now();
        remoteTaskHandle = new RemoteTaskHandle();
        windowContext.markStarted(safeQueue.firstTaskType());
    }

    public void markRemotePaused() {
        if (remoteRunning) {
            remotePaused = true;
            RemoteTaskHandle handle = remoteTaskHandle;
            if (handle != null) {
                handle.requestPause();
            }
            windowContext.markPauseRequested("remote turn paused");
        }
    }

    public void markRemoteResumed() {
        if (remoteRunning) {
            remotePaused = false;
            RemoteTaskHandle handle = remoteTaskHandle;
            if (handle != null) {
                handle.resume();
            }
            windowContext.markResumed("remote turn resumed");
        }
    }

    public void markRemoteStopped(String message) {
        RemoteTaskHandle handle = remoteTaskHandle;
        if (handle != null) {
            handle.requestStop(message);
        }
        remoteRunning = false;
        remotePaused = false;
        remoteQueue = WindowTaskQueue.empty();
        remoteStartedAt = null;
        remoteTaskHandle = null;
        windowContext.markFinished(WindowRuntimeStatus.STOPPED, message);
    }

    public void markRemoteFailed(Throwable failure) {
        RemoteTaskHandle handle = remoteTaskHandle;
        if (handle != null) {
            handle.requestStop("remote turn failed");
        }
        remoteRunning = false;
        remotePaused = false;
        remoteQueue = WindowTaskQueue.empty();
        remoteStartedAt = null;
        remoteTaskHandle = null;
        windowContext.markError("remote turn failed: "
                + (failure == null ? "unknown" : failure.getClass().getSimpleName() + ": " + failure.getMessage()));
    }

    public boolean isRemoteRunning() {
        return remoteRunning;
    }

    public boolean isRemotePaused() {
        return remotePaused;
    }

    public RemoteTaskHandle getRemoteTaskHandle() {
        return remoteTaskHandle;
    }

    public WindowTaskSnapshot snapshot() {
        PlayerCharacter me = windowContext.getGameState().getMe();
        TaskType runningTask = remoteRunning ? remoteQueue.firstTaskType() : TaskType.UNKNOWN;
        return new WindowTaskSnapshot(
                windowContext.getWindowId(),
                windowContext.getRoleName(),
                windowContext.getRole(),
                windowContext.getStatus(),
                windowContext.getSelectedTaskType(),
                runningTask,
                windowContext.getLastTaskType(),
                windowContext.getLastResult(),
                remoteRunning,
                remoteStartedAt,
                windowContext.getLastStartedAt(),
                windowContext.getLastFinishedAt(),
                windowContext.getLastMessage(),
                windowContext.getLastResultMessage(),
                windowContext.getNativeBinding(),
                windowContext.getLastQueueDisplayText(),
                windowContext.getLastQueueResult(),
                windowContext.getLastQueueMessage(),
                windowContext.getLastQueueFailurePolicy(),
                remoteRunning ? remoteQueue.toDisplayText() : "-",
                remoteRunning ? "Cloud HTTPS turn" : "-",
                windowContext.getRunningTaskProgressText(),
                remoteRunning ? remoteQueue.size() : 0,
                remoteRunning ? remoteQueue.getFailurePolicy() : null,
                canAcceptTaskQueue(),
                me == null ? null : me.getName(),
                me == null ? null : me.getId(),
                me == null ? null : me.getGameServerName());
    }

    public void shutdownNow() {
        shutdown = true;
        remoteRunning = false;
        remotePaused = false;
        remoteQueue = WindowTaskQueue.empty();
        remoteStartedAt = null;
        RemoteTaskHandle handle = remoteTaskHandle;
        if (handle != null) {
            handle.requestStop("window unregistered");
        }
        remoteTaskHandle = null;
    }
}
