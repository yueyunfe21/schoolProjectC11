package com.bot.dhxy.window.runtime;

import com.bot.dhxy.core.GameContext;
import com.bot.dhxy.model.job.XiuluoGreenChainSchedule;
import com.bot.dhxy.window.model.WindowNativeBinding;
import com.bot.dhxy.window.model.WindowPathingIntent;
import com.bot.dhxy.window.model.WindowPathingSnapshot;
import com.bot.dhxy.window.model.WindowPathingState;

/** Standalone G065 contract for a 看打 click that replaces the option with a rejection story. */
public final class G065LocalKandaDialogGoneContractTest {

    private G065LocalKandaDialogGoneContractTest() {
    }

    public static void main(String[] args) {
        retryStateOpensOnlyAfterConfirmationWindow();
        staleAttemptCannotBecomeReplacementRetry();
        unclickedAttemptIsNotAPostClickRetry();
        terminalPathingWithoutTemplateFailsExactlyOnce();
        activeOrUnrelatedPathingCannotFailTheAttempt();
        System.out.println("G065 local-kanda dialog-gone contract passed");
    }

    private static void retryStateOpensOnlyAfterConfirmationWindow() {
        WindowRuntimeContext context = context();
        XiuluoGreenChainSchedule schedule = schedule("run-1", 1, "attempt-1");
        context.updateXiuluoGreenChainSchedule(schedule, "g065 dialog-gone contract");
        context.recordXiuluoLocalKandaClick(schedule, 1_000L);

        require(context.evaluateXiuluoLocalKandaRetry(schedule, 4_999L, false)
                        == WindowRuntimeContext.XiuluoKandaRetryState.WAITING_FOR_COMBAT,
                "the four-second combat confirmation window must remain intact");
        require(context.evaluateXiuluoLocalKandaRetry(schedule, 5_000L, false)
                        == WindowRuntimeContext.XiuluoKandaRetryState.RETRY_AVAILABLE,
                "after the window, an executed click must expose the sampler retry decision");
    }

    private static void staleAttemptCannotBecomeReplacementRetry() {
        WindowRuntimeContext context = context();
        XiuluoGreenChainSchedule stale = schedule("run-1", 1, "attempt-old");
        context.updateXiuluoGreenChainSchedule(stale, "g065 stale setup");
        context.recordXiuluoLocalKandaClick(stale, 1_000L);

        XiuluoGreenChainSchedule replacement = schedule("run-1", 2, "attempt-new");
        context.updateXiuluoGreenChainSchedule(replacement, "g065 replacement setup");
        require(context.evaluateXiuluoLocalKandaRetry(stale, 9_000L, false)
                        == WindowRuntimeContext.XiuluoKandaRetryState.STALE,
                "an old attempt must not be exposed as a replacement retry");
        require(context.evaluateXiuluoLocalKandaRetry(replacement, 9_000L, false)
                        == WindowRuntimeContext.XiuluoKandaRetryState.AVAILABLE,
                "the replacement attempt must remain untouched and available");
    }

    private static void unclickedAttemptIsNotAPostClickRetry() {
        WindowRuntimeContext context = context();
        XiuluoGreenChainSchedule schedule = schedule("run-2", 1, "attempt-2");
        context.updateXiuluoGreenChainSchedule(schedule, "g065 unclicked setup");
        require(context.evaluateXiuluoLocalKandaRetry(schedule, 20_000L, false)
                        == WindowRuntimeContext.XiuluoKandaRetryState.AVAILABLE,
                "an attempt with no executed click cannot become a post-click retry");
    }

    private static void terminalPathingWithoutTemplateFailsExactlyOnce() {
        WindowRuntimeContext context = context();
        XiuluoGreenChainSchedule schedule = schedule("run-3", 1, "attempt-3");
        context.updateXiuluoGreenChainSchedule(schedule, "g065 terminal setup");
        context.updatePathingSnapshot(pathing(schedule.getAttemptId(), WindowPathingState.STOPPED_AWAY));
        context.clearPathingSignal("cloud consumed exact terminal");

        require(context.tryClaimXiuluoMissingKandaAfterPathingTerminal(schedule),
                "the exact retained terminal must fail a no-template hot start");
        context.updateXiuluoGreenChainSchedule(schedule.toBuilder().openedAtMs(10_000L).build(),
                "cloud resent the same exact schedule after consuming pathing");
        require(!context.tryClaimXiuluoMissingKandaAfterPathingTerminal(schedule),
                "the no-template failure must remain one-shot across an exact schedule resend");
    }

    private static void activeOrUnrelatedPathingCannotFailTheAttempt() {
        WindowRuntimeContext context = context();
        XiuluoGreenChainSchedule schedule = schedule("run-4", 1, "attempt-4");
        context.updateXiuluoGreenChainSchedule(schedule, "g065 active setup");
        context.updatePathingSnapshot(pathing(schedule.getAttemptId(), WindowPathingState.ACTIVE));
        require(!context.tryClaimXiuluoMissingKandaAfterPathingTerminal(schedule),
                "an active exact route must never be mistaken for a missing template");

        context.updatePathingSnapshot(pathing("other-attempt", WindowPathingState.STOPPED_AWAY));
        context.clearPathingSignal("unrelated terminal consumed");
        require(!context.tryClaimXiuluoMissingKandaAfterPathingTerminal(schedule),
                "an unrelated retained terminal must not fail this attempt");
    }

    private static WindowPathingSnapshot pathing(String intentId, WindowPathingState state) {
        return WindowPathingSnapshot.builder()
                .state(state)
                .intent(WindowPathingIntent.builder()
                        .intentId(intentId)
                        .source("ghost-king:tracker-shortcut:1:0")
                        .createdAtMs(2L)
                        .build())
                .currentMapName("白骨山")
                .currentX(42)
                .currentY(145)
                .updatedAtMs(3L)
                .build();
    }

    private static WindowRuntimeContext context() {
        WindowRuntimeContext context = new WindowRuntimeContext("window-1", new GameContext());
        context.setNativeBinding(new WindowNativeBinding(
                "100", "game", "class", 1L, 10, 20, 800, 600));
        return context;
    }

    private static XiuluoGreenChainSchedule schedule(String taskRunId, int round, String attemptId) {
        return XiuluoGreenChainSchedule.builder()
                .windowId("window-1")
                .hwnd("100")
                .observationRunId("observation-1")
                .taskRunId(taskRunId)
                .round(round)
                .attemptId(attemptId)
                .openedAtMs(1L)
                .build();
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
