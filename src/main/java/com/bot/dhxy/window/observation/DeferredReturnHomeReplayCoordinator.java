package com.bot.dhxy.window.observation;

import com.bot.dhxy.cloud.turn.protocol.TurnBagOperationArguments;
import com.bot.dhxy.cloud.turn.protocol.TurnReturnItemCachePoint;
import com.bot.dhxy.cloud.turn.protocol.observation.ObservationKeyEvent;
import com.bot.dhxy.cloud.turn.protocol.observation.ObservationKeyEventType;
import com.bot.dhxy.input.InputSequences;
import com.bot.dhxy.input.action.InputActionExecutionResult;
import com.bot.dhxy.input.action.InputActionSafetyReason;
import com.bot.dhxy.model.bag.BagReturnItemMacroIntent;
import com.bot.dhxy.model.bag.BagReturnItemMacroResult;
import com.bot.dhxy.model.bag.ReturnItemCachePoint;
import com.bot.dhxy.service.BagService;
import com.bot.dhxy.window.model.WindowNativeBinding;
import com.bot.dhxy.window.model.WindowRetainedReturnHomeReplay;
import com.bot.dhxy.window.runtime.WindowNativeBindingRefreshService;
import com.bot.dhxy.window.runtime.WindowRuntimeContext;
import com.bot.dhxy.window.runtime.WindowTaskContextHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.function.Consumer;

/** Executes one retained return-home command asynchronously for each exact window. */
@Component
public final class DeferredReturnHomeReplayCoordinator {

    private static final Logger log = LoggerFactory.getLogger(DeferredReturnHomeReplayCoordinator.class);

    private final WindowTaskContextHolder contextHolder;
    private final WindowNativeBindingRefreshService bindingRefreshService;
    private final InputSequences inputSequences;
    private final BagService bagService;
    private final ConcurrentHashMap<String, ExecutorService> replayExecutors = new ConcurrentHashMap<>();

    public DeferredReturnHomeReplayCoordinator(WindowTaskContextHolder contextHolder,
                                               WindowNativeBindingRefreshService bindingRefreshService,
                                               InputSequences inputSequences,
                                               BagService bagService) {
        this.contextHolder = Objects.requireNonNull(contextHolder, "contextHolder");
        this.bindingRefreshService = Objects.requireNonNull(bindingRefreshService, "bindingRefreshService");
        this.inputSequences = Objects.requireNonNull(inputSequences, "inputSequences");
        this.bagService = Objects.requireNonNull(bagService, "bagService");
    }

    public void retainExecuted(WindowRuntimeContext context, TurnBagOperationArguments arguments) {
        if (context == null || arguments == null || !isSupportedTask(arguments.retainedReplayTaskCode())
                || blank(arguments.retainedReplayObservationRunId())
                || blank(arguments.retainedReplayBusinessTaskRunId())) {
            return;
        }
        if (arguments.intent() != TurnBagOperationArguments.ReturnItemIntent.USE_CACHED_RETURN_ITEM
                && arguments.intent() != TurnBagOperationArguments.ReturnItemIntent.FIND_AND_USE_TASK_PAGE) {
            return;
        }
        WindowNativeBinding binding = context.getNativeBinding();
        if (binding == null || !binding.hasNativeHandle() || !binding.hasGeometry()) {
            context.clearRetainedReturnHomeReplay("return action completed without exact native geometry");
            return;
        }
        context.retainReturnHomeReplay(new WindowRetainedReturnHomeReplay(
                arguments.retainedReplayTaskCode().toUpperCase(Locale.ROOT),
                UUID.randomUUID().toString(),
                context.currentReturnHomeReplayLifecycleGeneration(),
                arguments.retainedReplayObservationRunId(),
                arguments.retainedReplayBusinessTaskRunId(),
                arguments,
                context.getWindowId(),
                binding.getNativeHandle(),
                binding.getX(),
                binding.getY(),
                binding.getWidth(),
                binding.getHeight(),
                WindowRetainedReturnHomeReplay.State.RETAINED));
    }

    public WindowRuntimeContext.ReplayArmResult arm(
            WindowRuntimeContext context,
            String taskCode,
            String observationRunId,
            String businessTaskRunId,
            String exactWindowId,
            String exactHwnd) {
        if (context == null || !isSupportedTask(taskCode) || blank(observationRunId)
                || blank(businessTaskRunId) || blank(exactWindowId) || blank(exactHwnd)) {
            return WindowRuntimeContext.ReplayArmResult.IDENTITY_REJECTED;
        }
        return context.armRetainedReturnHomeReplay(
                taskCode, observationRunId, businessTaskRunId, exactWindowId, exactHwnd);
    }

    /**
     * Claims an armed replay and schedules it off the observation thread.
     *
     * <p>The successful business edge, when present, is published only after the retained input
     * macro completes. A correction generation without a business claim passes {@code null} and
     * publishes only the replay terminal. Every rejected or failed replay publishes a typed
     * terminal so Cloud can take its fallback.</p>
     */
    public SubmitResult submitOnLocalExit(
            WindowRuntimeContext context,
            String taskCode,
            String observationRunId,
            String businessTaskRunId,
            ObservationKeyEvent businessExit,
            Consumer<ObservationKeyEvent> publisher) {
        Objects.requireNonNull(publisher, "publisher");
        if (context == null || !isSupportedTask(taskCode)
                || blank(observationRunId) || blank(businessTaskRunId)) {
            return SubmitResult.IDENTITY_REJECTED;
        }
        WindowNativeBinding binding = context.getNativeBinding();
        String hwnd = binding == null ? null : binding.getNativeHandle();
        WindowRuntimeContext.ReplayClaim claim = context.claimArmedReturnHomeReplay(
                taskCode, observationRunId, businessTaskRunId, context.getWindowId(), hwnd);
        if (claim.status() == WindowRuntimeContext.ReplayClaimStatus.NONE) {
            publishBusinessExitIfPresent(publisher, businessExit);
            return SubmitResult.NO_REPLAY;
        }
        if (claim.status() == WindowRuntimeContext.ReplayClaimStatus.IDENTITY_REJECTED) {
            publishTerminal(publisher, claim.replay(), ObservationKeyEventType.RETURN_HOME_REPLAY_IDENTITY_REJECTED,
                    "armed replay identity does not match the current observation/window");
            return SubmitResult.IDENTITY_REJECTED;
        }
        WindowRetainedReturnHomeReplay replay = claim.replay();
        ExecutorService executor = executorFor(context.getWindowId());
        try {
            executor.execute(() -> executeClaimed(
                    context, replay, businessExit, publisher, executor));
        } catch (RejectedExecutionException rejected) {
            publishTerminal(publisher, replay, ObservationKeyEventType.RETURN_HOME_REPLAY_IDENTITY_REJECTED,
                    "replay executor closed before submission");
            context.completeRetainedReturnHomeReplay(replay, "executor rejected submission");
            releaseExecutor(context.getWindowId(), executor);
            return SubmitResult.IDENTITY_REJECTED;
        }
        return SubmitResult.SUBMITTED;
    }

    public void clear(WindowRuntimeContext context, String reason) {
        if (context != null) {
            context.invalidateReturnHomeReplayLifecycle(reason);
            ExecutorService executor = replayExecutors.remove(context.getWindowId());
            if (executor != null) {
                executor.shutdownNow();
            }
        }
    }

    private void executeClaimed(
            WindowRuntimeContext context,
            WindowRetainedReturnHomeReplay replay,
            ObservationKeyEvent businessExit,
            Consumer<ObservationKeyEvent> publisher,
            ExecutorService executor) {
        try {
            if (!context.isReturnHomeReplayActive(replay)) {
                publishTerminal(publisher, replay,
                        ObservationKeyEventType.RETURN_HOME_REPLAY_IDENTITY_REJECTED,
                        "replay lifecycle became stale before execution");
                return;
            }
            WindowNativeBinding live = bindingRefreshService.refreshAndCommit(context).orElse(null);
            if (!context.isReturnHomeReplayActive(replay) || !sameReplayWindow(replay, live)) {
                publishTerminal(publisher, replay, ObservationKeyEventType.RETURN_HOME_REPLAY_IDENTITY_REJECTED,
                        "exact lifecycle/HWND/size fence rejected retained replay");
                return;
            }
            BagReturnItemMacroIntent intent = toReplayIntent(replay, live);
            InputActionExecutionResult result = contextHolder.callWith(context,
                    () -> inputSequences.submitFrozenExactWindowExclusiveAndWait(
                            "local-runner:return-home-replay:" + replay.taskCode(),
                            context,
                            live,
                            () -> context.isReturnHomeReplayActive(replay)
                                    && bagService.runReturnItemMacroDirectForExclusive(intent, null).getStatus()
                                    == BagReturnItemMacroResult.Status.USED,
                            () -> context.isReturnHomeReplayActive(replay)
                                    ? InputActionSafetyReason.CLEAR
                                    : InputActionSafetyReason.TASK_RUN_MISMATCH));
            if (result != null && result.isCompleted()) {
                publishTerminal(publisher, replay, ObservationKeyEventType.RETURN_HOME_REPLAY_SUCCEEDED,
                        "retained return-home replay completed");
                publishBusinessExitIfPresent(publisher, businessExit);
            } else {
                publishTerminal(publisher, replay, ObservationKeyEventType.RETURN_HOME_REPLAY_FAILED,
                        "retained return-home replay did not complete");
            }
        } catch (RuntimeException failure) {
            publishTerminal(publisher, replay, ObservationKeyEventType.RETURN_HOME_REPLAY_FAILED,
                    failure.getClass().getSimpleName() + ": " + failure.getMessage());
            log.error("[local-runner] deferred return replay failed: windowId={} task={} observationRunId={} businessTaskRunId={}",
                    context.getWindowId(), replay.taskCode(), replay.observationRunId(),
                    replay.businessTaskRunId(), failure);
        } finally {
            context.completeRetainedReturnHomeReplay(replay, "async replay reached typed terminal");
            releaseExecutor(context.getWindowId(), executor);
        }
    }

    private ExecutorService executorFor(String windowId) {
        return replayExecutors.computeIfAbsent(windowId, id -> Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "dhxy-return-replay-" + id);
            thread.setDaemon(true);
            return thread;
        }));
    }

    private void releaseExecutor(String windowId, ExecutorService executor) {
        if (replayExecutors.remove(windowId, executor)) {
            executor.shutdown();
        }
    }

    private static void publishTerminal(
            Consumer<ObservationKeyEvent> publisher,
            WindowRetainedReturnHomeReplay replay,
            ObservationKeyEventType type,
            String detail) {
        if (replay == null) {
            return;
        }
        publisher.accept(new ObservationKeyEvent(
                UUID.randomUUID().toString(), type, System.currentTimeMillis(),
                null, null, null, "local-runner:return-home-replay", detail,
                null, null, replay.taskCode(), replay.businessTaskRunId()));
    }

    private static void publishBusinessExitIfPresent(
            Consumer<ObservationKeyEvent> publisher,
            ObservationKeyEvent businessExit) {
        if (businessExit != null) {
            publisher.accept(businessExit);
        }
    }

    private static BagReturnItemMacroIntent toReplayIntent(
            WindowRetainedReturnHomeReplay replay,
            WindowNativeBinding live) {
        TurnBagOperationArguments arguments = replay.arguments();
        String replaySource = arguments.source() + ":local-true-exit-replay";
        return switch (arguments.intent()) {
            case FIND_AND_USE_TASK_PAGE -> BagReturnItemMacroIntent.findAndUseTaskPage(
                    arguments.targetItemTemplate(), replaySource);
            case USE_CACHED_RETURN_ITEM -> {
                TurnReturnItemCachePoint point = Objects.requireNonNull(arguments.cachedPoint(), "cachedPoint");
                int dx = live.getX() - replay.sourceX();
                int dy = live.getY() - replay.sourceY();
                yield BagReturnItemMacroIntent.useCachedReturnItem(ReturnItemCachePoint.builder()
                        .templatePath(point.templatePath())
                        .clickX(point.clickX() + dx)
                        .clickY(point.clickY() + dy)
                        .learnedAtMs(point.learnedAtMs())
                        .source(point.source() + ":translated")
                        .build(), replaySource);
            }
            default -> throw new IllegalStateException("Unsupported retained return replay intent: " + arguments.intent());
        };
    }

    private static boolean sameReplayWindow(WindowRetainedReturnHomeReplay replay, WindowNativeBinding live) {
        return live != null
                && live.hasNativeHandle()
                && replay.sourceHwnd().equals(live.getNativeHandle())
                && replay.sourceWidth() == live.getWidth()
                && replay.sourceHeight() == live.getHeight();
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private static boolean isSupportedTask(String taskCode) {
        return "XIULUO_V2".equalsIgnoreCase(taskCode)
                || "XINSHOU_TRAINING".equalsIgnoreCase(taskCode)
                || "CATCH_GHOST".equalsIgnoreCase(taskCode)
                || "GHOST_KING".equalsIgnoreCase(taskCode)
                || "WUBEI".equalsIgnoreCase(taskCode);
    }

    public enum SubmitResult {
        NO_REPLAY,
        SUBMITTED,
        IDENTITY_REJECTED
    }
}
