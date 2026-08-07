package com.bot.dhxy.window.runtime;

import com.bot.dhxy.core.GameContext;
import com.bot.dhxy.model.dialog.DialogOperation;
import com.bot.dhxy.model.job.XiuluoGreenChainSchedule;
import com.bot.dhxy.task.model.TaskType;
import com.bot.dhxy.window.model.WindowDialogInterest;
import com.bot.dhxy.window.model.WindowNativeBinding;
import com.bot.dhxy.window.model.WindowPathingSnapshot;
import com.bot.dhxy.window.model.WindowPathingState;
import com.bot.dhxy.window.model.WindowRole;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

class WindowRuntimeContextNativeBindingGenerationTest {

    @Test
    void equivalentRefreshPreservesExactGenerationButGeometryChangeReplacesIt() {
        WindowRuntimeContext context = new WindowRuntimeContext("window-generation", new GameContext());
        WindowNativeBinding original = binding(10, 20, 1024, 768);
        context.setNativeBinding(original);

        context.setNativeBinding(binding(10, 20, 1024, 768));
        assertSame(original, context.getNativeBinding(),
                "an equivalent capture refresh must preserve the frozen input generation");

        context.setNativeBinding(binding(11, 20, 1024, 768));
        assertNotSame(original, context.getNativeBinding(),
                "a real geometry change must create a new generation and invalidate stale input");
    }

    @Test
    void firstNativeBindingImmediatelyPopulatesPlayerIdentity() {
        WindowRuntimeContext context = new WindowRuntimeContext("window-identity", new GameContext());

        context.setNativeBinding(new WindowNativeBinding(
                "4379326",
                "大话西游2经典版 $Revision: 2039941 - 江山如画 - 乌龟的黑头° (ID: 67555)",
                "xy2",
                42L,
                0,
                0,
                1024,
                768));

        assertEquals("乌龟的黑头°", context.getGameState().getMe().getName());
        assertEquals("江山如画", context.getGameState().getMe().getGameServerName());
        assertEquals("67555", context.getGameState().getMe().getId());
    }

    @Test
    void taskExecutionResetDropsPathingButRetainsWindowRegistrationAndUserSelection() {
        WindowRuntimeContext context = new WindowRuntimeContext("window-reset", new GameContext());
        WindowNativeBinding binding = binding(10, 20, 1024, 768);
        context.setNativeBinding(binding);
        context.updateRole(WindowRole.LEADER, "队长");
        context.setSelectedTaskType(TaskType.TIANTING);
        context.updatePathingSnapshot(WindowPathingSnapshot.builder()
                .state(WindowPathingState.ACTIVE)
                .message("old task pathing")
                .build());
        context.markDialogFrameObserved(System.currentTimeMillis());

        context.clearTaskExecutionState("test stop boundary");

        assertEquals(WindowPathingState.NONE, context.getPathingSnapshot().getState());
        assertEquals(0L, context.getDialogFrameObservedAtMs());
        assertSame(binding, context.getNativeBinding(), "stop must not unregister the bound HWND");
        assertEquals(WindowRole.LEADER, context.getRole(), "stop must preserve the user role selection");
        assertEquals(TaskType.TIANTING, context.getSelectedTaskType(), "stop must preserve selected task configuration");
    }

    @Test
    void lateProbeStartIsNormalizedForOrdinaryDialogInterest() {
        WindowRuntimeContext context = new WindowRuntimeContext("window-dialog-timing", new GameContext());
        WindowDialogInterest staleInterest = staleProbeInterest();

        context.updateDialogInterest(staleInterest, "late cloud delivery");

        WindowDialogInterest installed = context.getDialogInterest().orElseThrow();
        assertEquals(installed.getCreatedAtMs(), installed.getProbeStartAtMs(),
                "a stale Cloud probe start must not invalidate local observation batches");
    }

    @Test
    void lateProbeStartIsNormalizedForScheduledGreenChainInterest() {
        WindowRuntimeContext context = new WindowRuntimeContext("window-dialog-schedule-timing", new GameContext());

        context.updateDialogInterestWithXiuluoGreenChainSchedule(
                staleProbeInterest(),
                XiuluoGreenChainSchedule.builder()
                        .windowId("window-dialog-schedule-timing")
                        .hwnd("4379326")
                        .observationRunId("observation-run")
                        .taskRunId("task-run:0:XIULUO_V2")
                        .round(1)
                        .attemptId("attempt")
                        .openedAtMs(System.currentTimeMillis())
                        .build(),
                "late scheduled cloud delivery");

        WindowDialogInterest installed = context.getDialogInterest().orElseThrow();
        assertEquals(installed.getCreatedAtMs(), installed.getProbeStartAtMs(),
                "the paired schedule path must uphold the same observation timing contract");
    }

    private static WindowDialogInterest staleProbeInterest() {
        return WindowDialogInterest.builder()
                .taskType(TaskType.XIULUO_V2)
                .operations(List.of(DialogOperation.XIULUO_ENTER_BATTLE))
                .source("timing-contract-test")
                .localTemplateProbeOnly(true)
                .probeStartAtMs(1L)
                .build();
    }

    private static WindowNativeBinding binding(int x, int y, int width, int height) {
        return new WindowNativeBinding(
                "4379326",
                "大话西游2经典版 - 测试窗口",
                "xy2",
                42L,
                x,
                y,
                width,
                height);
    }
}
