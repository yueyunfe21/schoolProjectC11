package com.bot.dhxy.window.runtime;

import com.bot.dhxy.cloud.turn.protocol.TurnBagOperationArguments;
import com.bot.dhxy.core.GameContext;
import com.bot.dhxy.model.job.XiuluoGreenChainSchedule;
import com.bot.dhxy.window.model.WindowExpectedCombatEnterClaim;
import com.bot.dhxy.window.model.WindowNativeBinding;
import com.bot.dhxy.window.model.WindowRetainedReturnHomeReplay;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LocalRunnerIdentityContractTest {

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
    void claimArrivingAfterVisibleEdgeBindsCurrentGenerationWithoutTimeGuessing() {
        WindowRuntimeContext context = context();
        context.updateLocalCombatGeneration(9L, true);
        assertTrue(context.registerExpectedCombatEnterClaim(new WindowExpectedCombatEnterClaim(
                "claim-cloud", "obs-1", "biz-1", "WUBEI", "attempt-cloud",
                "window-1", "100", "cloud-fallback", null)));
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
    void expectedClaimSourcesAreLimitedToSuccessfulLocalOrCloudEnterClicks() {
        WindowRuntimeContext context = context();
        assertTrue(context.registerExpectedCombatEnterClaim(new WindowExpectedCombatEnterClaim(
                "local", "obs-1", "biz-1", "XIULUO_V2", "attempt-local",
                "window-1", "100", "local-template", null)));
        context.clearExpectedCombatEnterClaim("test source boundary");
        assertTrue(context.registerExpectedCombatEnterClaim(new WindowExpectedCombatEnterClaim(
                "cloud", "obs-1", "biz-1", "WUBEI", "attempt-cloud",
                "window-1", "100", "cloud-fallback", null)));
        context.clearExpectedCombatEnterClaim("test source boundary");
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
}
