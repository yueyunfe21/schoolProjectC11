package com.bot.dhxy.window.runtime;

import com.bot.dhxy.cloud.turn.protocol.TurnBagOperationArguments;
import com.bot.dhxy.core.GameContext;
import com.bot.dhxy.model.dialog.DialogOperation;
import com.bot.dhxy.model.dialog.PreparedDialogAction;
import com.bot.dhxy.model.job.PreparedActionJob;
import com.bot.dhxy.model.job.PreparedActionJobType;
import com.bot.dhxy.model.job.XiuluoGreenChainSchedule;
import com.bot.dhxy.window.model.WindowExpectedCombatEnterClaim;
import com.bot.dhxy.window.model.WindowNativeBinding;
import com.bot.dhxy.window.model.WindowPathingIntent;
import com.bot.dhxy.window.model.WindowRetainedReturnHomeReplay;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LocalRunnerIdentityContractTest {

    @Test
    void exactAttemptAbandonClearsEveryOwnedSlotAndObservationLineage() {
        WindowRuntimeContext context = context();
        XiuluoGreenChainSchedule schedule = openAttempt(context, "biz-old", 2, "attempt-old");
        context.markPathingStarted(WindowPathingIntent.builder()
                .intentId("attempt-old").source("xiuluo-v2:tracker-shortcut:2").build());
        assertTrue(context.tryClaimXiuluoEnterBattleClick(schedule, "test"));
        context.recordXiuluoLocalKandaClick(schedule, 1_000L);
        assertTrue(context.registerExpectedCombatEnterClaim(expectedClaim(
                "expected-old", "biz-old", "attempt-old", "local-template")));
        assertTrue(context.armPendingDirectCombatEnterClaim(expectedClaim(
                "pending-old", "biz-old", "attempt-old", "local-alt-a")));
        context.updatePreparedDialogAction(PreparedDialogAction.builder()
                .windowId("window-1").hwnd("100").intentId("attempt-old")
                .operation(DialogOperation.XIULUO_ENTER_BATTLE).source("xiuluo-v2:test").build());
        assertTrue(context.publishPreparedActionJob(preparedJob(
                "biz-old", 2, "attempt-old"), "test"));
        long lineageBefore = context.getObservationPathingFactResetGeneration();

        WindowRuntimeContext.ExactAttemptAbandonResult ack = context.abandonExactXiuluoAttempt(
                "biz-old", 2, "attempt-old", "test reset");

        assertTrue(ack.exactAttemptMatched());
        assertTrue(ack.pathingCleared());
        assertTrue(ack.observationLineageCleared());
        assertTrue(ack.scheduleCleared());
        assertTrue(ack.clickClaimCleared());
        assertTrue(ack.clickProgressCleared());
        assertTrue(ack.expectedCombatClaimCleared());
        assertTrue(ack.pendingCombatTicketCleared());
        assertTrue(ack.preparedDialogActionCleared());
        assertTrue(ack.preparedActionJobCleared());
        assertFalse(ack.combatAlreadyConfirmed());
        assertTrue(context.getXiuluoGreenChainSchedule().isEmpty());
        assertTrue(context.getActivePathingIntent().isEmpty());
        assertNull(context.getPreparedDialogAction());
        assertNull(context.peekPreparedActionJob(PreparedActionJobType.XIULUO_ENTER_BATTLE));
        assertNull(context.currentPendingDirectCombatEnterClaim());
        assertEquals(lineageBefore + 1L, context.getObservationPathingFactResetGeneration());
    }

    @Test
    void staleOrWrongExactAttemptCannotClearReplacementAttempt() {
        WindowRuntimeContext context = context();
        XiuluoGreenChainSchedule current = openAttempt(context, "biz-new", 3, "attempt-new");
        context.markPathingStarted(WindowPathingIntent.builder()
                .intentId("attempt-new").source("xiuluo-v2:tracker-shortcut:3").build());
        assertTrue(context.tryClaimXiuluoEnterBattleClick(current, "test"));
        assertTrue(context.publishPreparedActionJob(preparedJob(
                "biz-new", 3, "attempt-new"), "test"));
        long lineageBefore = context.getObservationPathingFactResetGeneration();

        WindowRuntimeContext.ExactAttemptAbandonResult stale = context.abandonExactXiuluoAttempt(
                "biz-old", 2, "attempt-old", "stale reset");
        WindowRuntimeContext.ExactAttemptAbandonResult wrongRound = context.abandonExactXiuluoAttempt(
                "biz-new", 2, "attempt-new", "wrong-round reset");

        assertFalse(stale.exactAttemptMatched());
        assertFalse(wrongRound.exactAttemptMatched());
        assertEquals("attempt-new", context.getXiuluoGreenChainSchedule().orElseThrow().getAttemptId());
        assertEquals("attempt-new", context.getActivePathingIntent().orElseThrow().getIntentId());
        assertNotNull(context.peekPreparedActionJob(PreparedActionJobType.XIULUO_ENTER_BATTLE));
        assertEquals(lineageBefore, context.getObservationPathingFactResetGeneration());
    }

    @Test
    void confirmedInCombatWinsAgainstLateExactAttemptReset() {
        WindowRuntimeContext context = context();
        XiuluoGreenChainSchedule schedule = openAttempt(context, "biz-combat", 4, "attempt-combat");
        context.recordXiuluoLocalKandaClick(schedule, 1_000L);
        assertTrue(context.registerExpectedCombatEnterClaim(expectedClaim(
                "combat", "biz-combat", "attempt-combat", "local-template")));
        context.updateLocalCombatGeneration(8L, true);
        WindowExpectedCombatEnterClaim bound = context.bindExpectedCombatEnterClaim(
                "obs-1", "XIULUO_V2", 8L);
        context.confirmLocalTemplateCombatEntry(bound);
        long lineageBefore = context.getObservationPathingFactResetGeneration();

        WindowRuntimeContext.ExactAttemptAbandonResult ack = context.abandonExactXiuluoAttempt(
                "biz-combat", 4, "attempt-combat", "late timeout");

        assertTrue(ack.exactAttemptMatched());
        assertTrue(ack.combatAlreadyConfirmed());
        assertFalse(ack.observationLineageCleared());
        assertFalse(ack.expectedCombatClaimCleared());
        assertEquals(lineageBefore, context.getObservationPathingFactResetGeneration());
        assertNotNull(context.currentExpectedCombatEnterClaim("obs-1", "XIULUO_V2", 8L));
    }

    @Test
    void explicitObservationAndBusinessIdentitiesFenceArmAndClaim() {
        WindowRuntimeContext context = context();
        TurnBagOperationArguments arguments = new TurnBagOperationArguments(
                TurnBagOperationArguments.ReturnItemIntent.FIND_AND_USE_TASK_PAGE,
                "return.png", null, null, "test", "XIULUO_V2", "obs-1", "biz-1");
        context.retainReturnHomeReplay(new WindowRetainedReturnHomeReplay(
                "XIULUO_V2", "token-1", context.currentReturnHomeReplayLifecycleGeneration(),
                "obs-1", "biz-1", arguments, "window-1", "100",
                10, 20, 800, 600, WindowRetainedReturnHomeReplay.State.RETAINED));

        assertEquals(WindowRuntimeContext.ReplayArmResult.IDENTITY_REJECTED,
                context.armRetainedReturnHomeReplay("XIULUO_V2", "obs-wrong", "biz-1", "window-1", "100"));
        assertEquals(WindowRuntimeContext.ReplayArmResult.ARMED,
                context.armRetainedReturnHomeReplay("XIULUO_V2", "obs-1", "biz-1", "window-1", "100"));
        assertEquals(WindowRuntimeContext.ReplayClaimStatus.IDENTITY_REJECTED,
                context.claimArmedReturnHomeReplay(
                        "XIULUO_V2", "obs-1", "biz-wrong", "window-1", "100").status());
        assertEquals(WindowRuntimeContext.ReplayClaimStatus.IDENTITY_REJECTED,
                context.claimArmedReturnHomeReplay(
                        "XIULUO_V2", "obs-1", "biz-1", "window-1", "999").status());
        assertEquals(WindowRuntimeContext.ReplayClaimStatus.CLAIMED,
                context.claimArmedReturnHomeReplay(
                        "XIULUO_V2", "obs-1", "biz-1", "window-1", "100").status());
        assertEquals(WindowRuntimeContext.ReplayClaimStatus.NONE,
                context.claimArmedReturnHomeReplay(
                        "XIULUO_V2", "obs-1", "biz-1", "window-1", "100").status(),
                "replay claim is single-consumer");
    }

    @Test
    void correctionExitCanDiscoverOnlyTheExactArmedObservationRun() {
        WindowRuntimeContext context = context();
        WindowRetainedReturnHomeReplay replay =
                retainedReplay(context, "token-correction", "obs-correction", "biz-correction");
        context.retainReturnHomeReplay(replay);
        assertEquals(WindowRuntimeContext.ReplayArmResult.ARMED,
                context.armRetainedReturnHomeReplay(
                        "XIULUO_V2", "obs-correction", "biz-correction", "window-1", "100"));

        assertNull(context.currentArmedReturnHomeReplay("obs-stale"));
        WindowRetainedReturnHomeReplay exact =
                context.currentArmedReturnHomeReplay("obs-correction");
        assertNotNull(exact);
        assertEquals("biz-correction", exact.businessTaskRunId());
        assertEquals(WindowRetainedReturnHomeReplay.State.ARMED, exact.state());
    }

    @Test
    void nextVisibleGenerationAtomicallyBindsOnlyExactEnterClaim() {
        WindowRuntimeContext context = context();
        assertTrue(context.registerExpectedCombatEnterClaim(new WindowExpectedCombatEnterClaim(
                "claim-1", "obs-1", "biz-1", "XIULUO_V2", "attempt-1",
                "window-1", "100", "local-template", null)));
        assertNull(context.bindExpectedCombatEnterClaim("obs-wrong", 1L));
        WindowExpectedCombatEnterClaim bound = context.bindExpectedCombatEnterClaim("obs-1", 1L);
        assertNotNull(bound);
        assertEquals(1L, bound.combatGeneration());
        assertNull(context.bindExpectedCombatEnterClaim("obs-1", 2L),
                "one explicit enter claim cannot classify another combat generation");
    }

    @Test
    void exactCloudTaskDialogClaimArrivingAfterVisibleEdgeBindsCurrentGenerationWithoutTimeGuessing() {
        WindowRuntimeContext context = context();
        context.updateLocalCombatGeneration(9L, true);
        assertTrue(context.registerExpectedCombatEnterClaim(new WindowExpectedCombatEnterClaim(
                "claim-cloud", "obs-1", "biz-1", "WUBEI", "attempt-cloud",
                "window-1", "100", "cloud-task-dialog", null)));
        assertEquals(9L, context.currentExpectedCombatEnterClaim("obs-1", "WUBEI", 9L)
                .combatGeneration());
    }

    @Test
    void incidentalVisibleGenerationHasNoExpectedBusinessClaim() {
        WindowRuntimeContext context = context();
        context.updateLocalCombatGeneration(11L, true);

        assertNull(context.bindExpectedCombatEnterClaim("obs-1", 11L));
        assertNull(context.currentExpectedCombatEnterClaim("obs-1", "XIULUO_V2", 11L),
                "mechanical combat state remains local, but no expected business edge is authorized");
    }

    @Test
    void expectedClaimSourcesAreLimitedToSuccessfulLocalOrExactTaskDialogEnterClicks() {
        WindowRuntimeContext context = context();
        assertTrue(context.registerExpectedCombatEnterClaim(new WindowExpectedCombatEnterClaim(
                "local", "obs-1", "biz-1", "XIULUO_V2", "attempt-local",
                "window-1", "100", "local-template", null)));
        context.clearExpectedCombatEnterClaim("test source boundary");
        assertTrue(context.registerExpectedCombatEnterClaim(new WindowExpectedCombatEnterClaim(
                "cloud", "obs-1", "biz-1", "WUBEI", "attempt-cloud",
                "window-1", "100", "cloud-task-dialog", null)));
        context.clearExpectedCombatEnterClaim("test source boundary");
        assertFalse(context.registerExpectedCombatEnterClaim(new WindowExpectedCombatEnterClaim(
                "fallback", "obs-1", "biz-1", "WUBEI", "attempt-fallback",
                "window-1", "100", "cloud-fallback", null)));
        assertFalse(context.registerExpectedCombatEnterClaim(new WindowExpectedCombatEnterClaim(
                "guessed", "obs-1", "biz-1", "XIULUO_V2", "attempt-guessed",
                "window-1", "100", "pathing-time-guess", null)));
    }

    @Test
    void replacingAttemptClearsOnlyTheOldUnboundExpectedClaim() {
        WindowRuntimeContext context = context();
        assertTrue(context.registerExpectedCombatEnterClaim(new WindowExpectedCombatEnterClaim(
                "old-claim", "obs-1", "biz-1", "XIULUO_V2", "attempt-old",
                "window-1", "100", "local-template", null)));

        context.updateXiuluoGreenChainSchedule(XiuluoGreenChainSchedule.builder()
                .windowId("window-1")
                .hwnd("100")
                .observationRunId("obs-1")
                .taskRunId("biz-1")
                .round(1)
                .attemptId("attempt-new")
                .openedAtMs(1L)
                .build(), "attempt replacement test");

        assertNull(context.bindExpectedCombatEnterClaim("obs-1", "XIULUO_V2", 7L),
                "an abandoned click attempt cannot classify a later incidental generation");

        assertTrue(context.registerExpectedCombatEnterClaim(new WindowExpectedCombatEnterClaim(
                "new-claim", "obs-1", "biz-1", "XIULUO_V2", "attempt-new",
                "window-1", "100", "local-template", null)));
        context.updateXiuluoGreenChainSchedule(XiuluoGreenChainSchedule.builder()
                .windowId("window-1")
                .hwnd("100")
                .observationRunId("obs-1")
                .taskRunId("biz-1")
                .round(1)
                .attemptId("attempt-new")
                .openedAtMs(2L)
                .build(), "same attempt refresh test");
        assertNotNull(context.bindExpectedCombatEnterClaim("obs-1", "XIULUO_V2", 8L),
                "refreshing the same exact attempt must preserve its successful enter claim");
    }

    @Test
    void confirmedLocalKandaCannotRearmAfterItsCombatEnds() {
        WindowRuntimeContext context = context();
        XiuluoGreenChainSchedule schedule = XiuluoGreenChainSchedule.builder()
                .windowId("window-1")
                .hwnd("100")
                .observationRunId("obs-1")
                .taskRunId("biz-1")
                .round(3)
                .attemptId("attempt-3")
                .openedAtMs(1L)
                .build();
        context.updateXiuluoGreenChainSchedule(schedule, "test");
        context.recordXiuluoLocalKandaClick(schedule, 1_000L);
        assertTrue(context.registerExpectedCombatEnterClaim(new WindowExpectedCombatEnterClaim(
                "claim-3", "obs-1", "biz-1", "XIULUO_V2", "attempt-3",
                "window-1", "100", "local-template", null)));
        WindowExpectedCombatEnterClaim bound = context.bindExpectedCombatEnterClaim("obs-1", "XIULUO_V2", 3L);
        context.confirmLocalTemplateCombatEntry(bound);

        assertEquals(WindowRuntimeContext.XiuluoKandaRetryState.COMBAT_CONFIRMED,
                context.evaluateXiuluoLocalKandaRetry(schedule, 9_000L, false),
                "a completed 看打 battle must never become a post-exit retry candidate");
    }

    @Test
    void confirmedJianghuLocalKandaCannotRearmAfterItsCombatEnds() {
        WindowRuntimeContext context = context();
        XiuluoGreenChainSchedule schedule = XiuluoGreenChainSchedule.builder()
                .windowId("window-1")
                .hwnd("100")
                .observationRunId("obs-1")
                .taskRunId("biz-training-1")
                .round(4)
                .attemptId("training-attempt-4")
                .openedAtMs(1L)
                .build();
        context.updateXiuluoGreenChainSchedule(schedule, "jianghu training test");
        context.recordXiuluoLocalKandaClick(schedule, 1_000L);
        assertTrue(context.registerExpectedCombatEnterClaim(new WindowExpectedCombatEnterClaim(
                "training-claim-4", "obs-1", "biz-training-1", "XINSHOU_TRAINING", "training-attempt-4",
                "window-1", "100", "local-template", null)));
        WindowExpectedCombatEnterClaim bound = context.bindExpectedCombatEnterClaim(
                "obs-1", "XINSHOU_TRAINING", 4L);
        context.confirmLocalTemplateCombatEntry(bound);

        assertEquals(WindowRuntimeContext.XiuluoKandaRetryState.COMBAT_CONFIRMED,
                context.evaluateXiuluoLocalKandaRetry(schedule, 9_000L, false),
                "a completed 江湖历练看打 battle must not become a post-exit retry candidate");
    }

    @Test
    void oldReplayCompletionCannotClearReplacementRunSlot() {
        WindowRuntimeContext context = context();
        WindowRetainedReturnHomeReplay oldReplay =
                retainArmAndClaim(context, "old-token", "old-observation", "old-business");

        context.invalidateReturnHomeReplayLifecycle("new task replacement");
        WindowRetainedReturnHomeReplay newReplay =
                retainedReplay(context, "new-token", "new-observation", "new-business");
        context.retainReturnHomeReplay(newReplay);

        assertFalse(context.completeRetainedReturnHomeReplay(
                oldReplay, "old async finally reached terminal"));
        assertEquals(WindowRuntimeContext.ReplayArmResult.ARMED,
                context.armRetainedReturnHomeReplay(
                        "XIULUO_V2", "new-observation", "new-business", "window-1", "100"),
                "old replay completion must not delete the replacement run's retained command");
    }

    private static WindowRetainedReturnHomeReplay retainArmAndClaim(
            WindowRuntimeContext context,
            String tokenId,
            String observationRunId,
            String businessTaskRunId) {
        WindowRetainedReturnHomeReplay replay =
                retainedReplay(context, tokenId, observationRunId, businessTaskRunId);
        context.retainReturnHomeReplay(replay);
        assertEquals(WindowRuntimeContext.ReplayArmResult.ARMED,
                context.armRetainedReturnHomeReplay(
                        "XIULUO_V2", observationRunId, businessTaskRunId, "window-1", "100"));
        WindowRuntimeContext.ReplayClaim claim = context.claimArmedReturnHomeReplay(
                "XIULUO_V2", observationRunId, businessTaskRunId, "window-1", "100");
        assertEquals(WindowRuntimeContext.ReplayClaimStatus.CLAIMED, claim.status());
        return claim.replay();
    }

    private static WindowRetainedReturnHomeReplay retainedReplay(
            WindowRuntimeContext context,
            String tokenId,
            String observationRunId,
            String businessTaskRunId) {
        TurnBagOperationArguments arguments = new TurnBagOperationArguments(
                TurnBagOperationArguments.ReturnItemIntent.FIND_AND_USE_TASK_PAGE,
                "return.png", null, null, "test", "XIULUO_V2",
                observationRunId, businessTaskRunId);
        return new WindowRetainedReturnHomeReplay(
                "XIULUO_V2", tokenId, context.currentReturnHomeReplayLifecycleGeneration(),
                observationRunId, businessTaskRunId, arguments, "window-1", "100",
                10, 20, 800, 600, WindowRetainedReturnHomeReplay.State.RETAINED);
    }

    private static WindowRuntimeContext context() {
        WindowRuntimeContext context = new WindowRuntimeContext("window-1", new GameContext());
        context.setNativeBinding(new WindowNativeBinding(
                "100", "game", "class", 1L, 10, 20, 800, 600));
        return context;
    }

    private static XiuluoGreenChainSchedule openAttempt(
            WindowRuntimeContext context, String taskRunId, int round, String attemptId) {
        XiuluoGreenChainSchedule schedule = XiuluoGreenChainSchedule.builder()
                .windowId("window-1").hwnd("100").observationRunId("obs-1")
                .taskRunId(taskRunId).round(round).attemptId(attemptId).openedAtMs(1L).build();
        context.updateXiuluoGreenChainSchedule(schedule, "test");
        return schedule;
    }

    private static WindowExpectedCombatEnterClaim expectedClaim(
            String claimId, String taskRunId, String attemptId, String source) {
        return new WindowExpectedCombatEnterClaim(
                claimId, "obs-1", taskRunId, "XIULUO_V2", attemptId,
                "window-1", "100", source, null);
    }

    private static PreparedActionJob preparedJob(String taskRunId, int round, String attemptId) {
        return PreparedActionJob.builder()
                .type(PreparedActionJobType.XIULUO_ENTER_BATTLE)
                .windowId("window-1").hwnd("100").taskRunId(taskRunId)
                .round(round).attemptId(attemptId).source("test").preparedAtMs(1L).build();
    }
}
