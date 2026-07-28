package com.bot.dhxy.input.action;

import com.bot.dhxy.cloud.turn.protocol.TurnBagOperationArguments;
import com.bot.dhxy.core.GameContext;
import com.bot.dhxy.window.model.WindowNativeBinding;
import com.bot.dhxy.window.model.WindowRetainedReturnHomeReplay;
import com.bot.dhxy.window.runtime.WindowRuntimeContext;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InputActionReplayLifecycleCheckpointTest {

    @Test
    void lifecycleInvalidationInsideRunningCallbackStopsBeforeNextPhysicalAction() {
        WindowRuntimeContext context = new WindowRuntimeContext("window-1", new GameContext());
        WindowNativeBinding binding = new WindowNativeBinding(
                "100", "game", "class", 1L, 10, 20, 800, 600);
        context.setNativeBinding(binding);
        WindowRetainedReturnHomeReplay replay = claimReplay(context, binding, "token-1", "obs-1", "biz-1");
        AtomicInteger physicalActions = new AtomicInteger();

        InputActionRequest request = InputActionRequest.frozenExactWindowExclusive(
                context,
                binding,
                context.getPlayerIdentityEpoch(),
                "replay-lifecycle-test",
                () -> true,
                null,
                null,
                () -> context.isReturnHomeReplayActive(replay)
                        ? InputActionSafetyReason.CLEAR
                        : InputActionSafetyReason.TASK_RUN_MISMATCH);

        InputActionScope.callWith(request, () -> {
            assertTrue(InputActionScope.checkpoint(), "callback was admitted while replay was active");
            context.invalidateReturnHomeReplayLifecycle("replacement after first bag checkpoint");
            if (InputActionScope.checkpoint()) {
                physicalActions.incrementAndGet();
            }
            return true;
        });

        assertTrue(request.isCancelled());
        assertTrue(physicalActions.get() == 0,
                "no later direct input may execute after the first invalidated checkpoint");
    }

    @Test
    void queuedReplayInvalidatedBeforeCallbackProducesZeroPhysicalActions() {
        WindowRuntimeContext context = new WindowRuntimeContext("window-1", new GameContext());
        WindowNativeBinding binding = new WindowNativeBinding(
                "100", "game", "class", 1L, 10, 20, 800, 600);
        context.setNativeBinding(binding);
        WindowRetainedReturnHomeReplay replay = claimReplay(context, binding, "token-queued", "obs-1", "biz-1");
        AtomicInteger physicalActions = new AtomicInteger();
        InputActionRequest request = InputActionRequest.frozenExactWindowExclusive(
                context,
                binding,
                context.getPlayerIdentityEpoch(),
                "queued-replay-lifecycle-test",
                () -> {
                    physicalActions.incrementAndGet();
                    return true;
                },
                null,
                null,
                () -> context.isReturnHomeReplayActive(replay)
                        ? InputActionSafetyReason.CLEAR
                        : InputActionSafetyReason.TASK_RUN_MISMATCH);

        context.invalidateReturnHomeReplayLifecycle("stopped before input worker callback");
        assertTrue(!request.checkDetailedSafety("before-callback"));
        assertTrue(request.isCancelled());
        assertEquals(0, physicalActions.get(),
                "an invalidated queued replay must not enter its physical-input callback");
    }

    private static WindowRetainedReturnHomeReplay claimReplay(
            WindowRuntimeContext context,
            WindowNativeBinding binding,
            String tokenId,
            String observationRunId,
            String businessTaskRunId) {
        TurnBagOperationArguments arguments = new TurnBagOperationArguments(
                TurnBagOperationArguments.ReturnItemIntent.FIND_AND_USE_TASK_PAGE,
                "return.png", null, null, "test", "XIULUO_V2",
                observationRunId, businessTaskRunId);
        context.retainReturnHomeReplay(new WindowRetainedReturnHomeReplay(
                "XIULUO_V2", tokenId, context.currentReturnHomeReplayLifecycleGeneration(),
                observationRunId, businessTaskRunId, arguments, context.getWindowId(),
                binding.getNativeHandle(), binding.getX(), binding.getY(),
                binding.getWidth(), binding.getHeight(),
                WindowRetainedReturnHomeReplay.State.RETAINED));
        assertEquals(WindowRuntimeContext.ReplayArmResult.ARMED,
                context.armRetainedReturnHomeReplay(
                        "XIULUO_V2", observationRunId, businessTaskRunId,
                        context.getWindowId(), binding.getNativeHandle()));
        WindowRuntimeContext.ReplayClaim claim = context.claimArmedReturnHomeReplay(
                "XIULUO_V2", observationRunId, businessTaskRunId,
                context.getWindowId(), binding.getNativeHandle());
        assertEquals(WindowRuntimeContext.ReplayClaimStatus.CLAIMED, claim.status());
        return claim.replay();
    }
}
