package com.bot.dhxy.window.execution;

import com.bot.dhxy.input.InputSequences;
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
    private final InputSequences inputSequences;
    private volatile boolean remoteRunning;
    private volatile boolean remotePaused;
    private volatile WindowTaskQueue remoteQueue = WindowTaskQueue.empty();
    private volatile LocalDateTime remoteStartedAt;
    private volatile RemoteTaskHandle remoteTaskHandle;
    private volatile boolean shutdown;

    public WindowTaskRunner(WindowRuntimeContext windowContext, InputSequences inputSequences) {
        this.windowContext = Objects.requireNonNull(windowContext, "windowContext");
        this.inputSequences = Objects.requireNonNull(inputSequences, "inputSequences");
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

    /**
     * Clears the previous task-owned runtime state before a new remote start is sent to Cloud.
     *
     * <p>This must happen before the new loop can receive its start acknowledgement: that acknowledgement starts
     * the observation runner, whose first sample must describe the visible screen rather than an old dialog interest
     * or green-chain schedule. This method never sends physical input.</p>
     *
     * @param reason lifecycle diagnostic explaining why a fresh screen boundary is being created.
     */
    public synchronized void prepareRemoteFreshStart(String reason) {
        RemoteTaskHandle staleHandle = remoteTaskHandle;
        if (staleHandle != null) {
            staleHandle.requestStop(reason);
            inputSequences.cancelQueuedRequests(staleHandle.getStopToken(), "remote-fresh-start: " + reason);
        }
        remoteRunning = false;
        remotePaused = false;
        remoteQueue = WindowTaskQueue.empty();
        remoteStartedAt = null;
        remoteTaskHandle = null;
        windowContext.clearTaskExecutionState("remote fresh start: " + reason);
    }

    public void markRemoteStarted(WindowTaskQueue queue) {
        WindowTaskQueue safeQueue = queue == null ? WindowTaskQueue.empty() : queue;
        // prepareRemoteFreshStart() already cleared the task-owned context before the start ACK could create this
        // run's observation runner. Clearing here would erase fresh first-frame state after that runner begins.
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
        abortRemoteRun(message, WindowRuntimeStatus.STOPPED);
    }

    /**
     * Invalidate one remote run before a fresh start or terminal lifecycle transition.
     *
     * <p>The handle identity is the thin-client equivalent of the local observer generation: once
     * cleared, late turn terminals and old observation work can no longer project state into this
     * window. Only queued input holding this exact run's stop token is cancelled; an already-open
     * atomic input transaction deliberately remains intact.</p>
     *
     * @param message diagnostic lifecycle reason.
     * @param terminalStatus lifecycle UI status to project; {@code PAUSED} keeps a clean restart boundary,
     *                       while terminal values finish the run; null is only for internal replacement cleanup.
     */
    public synchronized void abortRemoteRun(String message, WindowRuntimeStatus terminalStatus) {
        RemoteTaskHandle handle = remoteTaskHandle;
        if (handle != null) {
            handle.requestStop(message);
            inputSequences.cancelQueuedRequests(handle.getStopToken(), "remote-run-aborted: " + message);
        }
        // Clear ownership before context state so old terminal callbacks fail the handle-identity fence.
        boolean pauseBoundary = terminalStatus == WindowRuntimeStatus.PAUSED;
        remoteRunning = false;
        remotePaused = pauseBoundary;
        remoteQueue = WindowTaskQueue.empty();
        remoteStartedAt = null;
        remoteTaskHandle = null;
        if (!pauseBoundary && terminalStatus != null) {
            windowContext.markFinished(terminalStatus, message);
        }
        windowContext.clearTaskExecutionState("remote turn aborted: " + message);
        if (pauseBoundary) {
            // Pause discards the old run exactly like stop, but remains a user-resumable screen boundary.
            // Do not use markFinished(PAUSED): that would fabricate a terminal result/timestamp.
            windowContext.markPauseRequested(message);
        }
    }

    public void markRemoteFailed(Throwable failure) {
        String message = "remote turn failed: "
                + (failure == null ? "unknown" : failure.getClass().getSimpleName() + ": " + failure.getMessage());
        abortRemoteRun(message, WindowRuntimeStatus.ERROR);
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
