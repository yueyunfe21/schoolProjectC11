package com.bot.dhxy.cloud.turn.local.host;

import com.bot.dhxy.cloud.turn.LocalServiceExecution;
import com.bot.dhxy.cloud.turn.protocol.TurnLocalOperation;
import com.bot.dhxy.cloud.turn.protocol.TurnLocalServiceCall;
import com.bot.dhxy.runner.stop.TaskSleep;
import com.bot.dhxy.runner.stop.TaskStopRequestedException;
import com.bot.dhxy.runner.stop.TaskStopToken;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;

/** Executes explicit host side effects without creating a fifth local business Service. */
@Slf4j
@Component
public final class HostLocalOperationExecutor {

    private static final long BEFORE_SLEEP_LOG_FLUSH_MS = 1_500L;

    /**
     * Execute one validated host operation for the action-owning task.
     *
     * @param call closed host operation with no business arguments.
     * @param stopToken live token captured from the exact action owner; required.
     * @return completed only after the Windows sleep process was started; stopped before side effect otherwise.
     */
    public LocalServiceExecution execute(TurnLocalServiceCall call, TaskStopToken stopToken) {
        if (call == null || call.operation() != TurnLocalOperation.HOST_SLEEP_COMPUTER) {
            return LocalServiceExecution.failed("INVALID_HOST_OPERATION", null);
        }
        if (stopToken == null) {
            return LocalServiceExecution.stopped(null);
        }
        try {
            stopToken.throwIfStopRequested();
            log.warn("[sleep-computer] task selected; Windows sleep will be requested after {}ms",
                    BEFORE_SLEEP_LOG_FLUSH_MS);
            if (!TaskSleep.sleep(BEFORE_SLEEP_LOG_FLUSH_MS)) {
                throw new TaskStopRequestedException("Sleep computer task interrupted");
            }
            stopToken.throwIfStopRequested();
            log.warn("system sleep requested: source=task-queue");
            new ProcessBuilder(
                    "rundll32.exe",
                    "powrprof.dll,SetSuspendState",
                    "0,1,0").start();
            return LocalServiceExecution.completed("HOST_SLEEP_REQUESTED", null, null);
        } catch (TaskStopRequestedException stopped) {
            log.info("[sleep-computer] stop requested before system sleep");
            return LocalServiceExecution.stopped(null);
        } catch (IOException failure) {
            throw new IllegalStateException("Failed to request Windows sleep", failure);
        }
    }
}
