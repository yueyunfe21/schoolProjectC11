package com.bot.dhxy.window.control;

import com.bot.dhxy.cloud.turn.protocol.TurnTaskCode;
import com.bot.dhxy.core.GameContext;
import com.bot.dhxy.runner.context.TaskStartupMode;
import com.bot.dhxy.task.model.TaskType;
import com.bot.dhxy.window.model.WindowTaskRunProgress;
import com.bot.dhxy.window.runtime.WindowRuntimeContext;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class G077PauseResumeProgressContractTest {

    @Test
    void pauseClearsExecutableStateButKeepsTheExactNumericCounterVisible() {
        WindowRuntimeContext context = context();
        context.markStarted(TaskType.TIANTING, null);
        context.updateTaskRunProgress(34, 60);

        context.retainTaskRunProgressForPause();
        context.clearTaskExecutionState("test pause cleanup");
        context.markPauseRequested("test paused");

        assertEquals("34/60", context.getRunningTaskProgressText());
        assertEquals(TaskType.TIANTING, context.getPausedTaskRunProgress().getTaskType());
        assertEquals(34, context.getPausedTaskRunProgress().getCompletedRuns());
        assertEquals(60, context.getPausedTaskRunProgress().getTotalRuns());
        assertEquals(0, context.getGameState().getCurrentTaskProgress(),
                "pause must still clear the executable Client game state");
    }

    @Test
    void onlyPauseResumeOfTheSameTaskAndTotalMaySeedCloud() {
        WindowTaskRunProgress paused = WindowTaskRunProgress.builder()
                .taskType(TaskType.TIANTING)
                .completedRuns(34)
                .totalRuns(60)
                .build();

        assertSame(paused, WindowTaskControlService.resolvePauseResumeProgress(
                TaskStartupMode.PAUSE_RESUME, List.of(TurnTaskCode.TIANTING), List.of(60), paused));
        assertNull(WindowTaskControlService.resolvePauseResumeProgress(
                TaskStartupMode.NORMAL, List.of(TurnTaskCode.TIANTING), List.of(60), paused));
        assertNull(WindowTaskControlService.resolvePauseResumeProgress(
                TaskStartupMode.PAUSE_RESUME, List.of(TurnTaskCode.WUBEI), List.of(60), paused));
        assertNull(WindowTaskControlService.resolvePauseResumeProgress(
                TaskStartupMode.PAUSE_RESUME, List.of(TurnTaskCode.TIANTING), List.of(100), paused));
        assertEquals(List.of(0, 34, 0), WindowTaskControlService.toTaskInitialCompletedRuns(
                List.of(TurnTaskCode.WUBEI, TurnTaskCode.TIANTING, TurnTaskCode.TIANTING), paused));
    }

    @Test
    void acceptedResumeStartsWithTheSameUiCounterAndConsumesThePauseSnapshot() {
        WindowRuntimeContext context = context();
        context.markStarted(TaskType.TIANTING, null);
        context.updateTaskRunProgress(34, 60);
        context.retainTaskRunProgressForPause();
        WindowTaskRunProgress paused = context.getPausedTaskRunProgress();
        context.clearTaskExecutionState("test resume cleanup");

        context.markStarted(TaskType.TIANTING, paused);

        assertEquals("34/60", context.getRunningTaskProgressText());
        assertNull(context.getPausedTaskRunProgress());
    }

    private static WindowRuntimeContext context() {
        WindowRuntimeContext context = new WindowRuntimeContext("g077-window", new GameContext());
        context.setSelectedTaskType(TaskType.TIANTING);
        return context;
    }
}
