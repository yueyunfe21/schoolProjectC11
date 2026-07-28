package com.bot.dhxy.window.execution;

import com.bot.dhxy.runner.stop.TaskPauseToken;
import com.bot.dhxy.runner.stop.TaskStopToken;

/** Live local checkpoint identity for one Cloud-owned task queue. */
public final class RemoteTaskHandle {

    private final TaskStopToken stopToken = new TaskStopToken();
    private final TaskPauseToken pauseToken = new TaskPauseToken();

    public TaskStopToken getStopToken() {
        return stopToken;
    }

    public TaskPauseToken getPauseToken() {
        return pauseToken;
    }

    public void requestPause() {
        pauseToken.requestPause("remote turn paused");
    }

    public void resume() {
        pauseToken.resume();
    }

    public void requestStop(String reason) {
        stopToken.requestStop(reason);
        pauseToken.resume();
    }
}
