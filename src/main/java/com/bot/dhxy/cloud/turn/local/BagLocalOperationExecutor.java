package com.bot.dhxy.cloud.turn.local;

import com.bot.dhxy.cloud.turn.LocalServiceExecution;
import com.bot.dhxy.cloud.turn.TurnFrame;
import com.bot.dhxy.cloud.turn.protocol.TurnContinuationDecision;
import com.bot.dhxy.cloud.turn.protocol.TurnContinuationRequest;
import com.bot.dhxy.cloud.turn.protocol.TurnBagOperationArguments;
import com.bot.dhxy.cloud.turn.protocol.TurnLocalServiceCall;
import com.bot.dhxy.cloud.turn.protocol.TurnReturnItemCachePoint;
import com.bot.dhxy.model.bag.ReturnItemCachePoint;
import com.bot.dhxy.runner.stop.TaskStopRequestedException;
import com.bot.dhxy.runner.stop.TaskStopToken;
import com.bot.dhxy.runner.stop.TaskSleep;
import com.bot.dhxy.service.BagService;
import com.bot.dhxy.model.bag.BagReturnItemMacroIntent;
import com.bot.dhxy.model.bag.BagReturnItemMacroResult;
import com.bot.dhxy.window.observation.DeferredReturnHomeReplayCoordinator;
import com.bot.dhxy.window.runtime.WindowTaskContextHolder;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.function.BooleanSupplier;

/**
 * Closed adapter for the permanent-local BagService turn operations.
 *
 * <p>Two entry surfaces with distinct queue ownership: {@link #execute} runs INSIDE the
 * dispatcher's exclusive input callback for the legacy direct-macro operations, while
 * {@link #executeQueueOwning} is dispatched UNWRAPPED for the three TURN-40B-C2 operations whose
 * public BagService entries acquire the single input queue themselves. This adapter alone maps
 * the guarded {@code TaskStopRequestedException} to the closed typed local stop.</p>
 */
@Component
public final class BagLocalOperationExecutor {

    private final BagService bagService;
    private final FiveRingIncenseObservationLocalMechanics incenseObservationMechanics;
    private final WindowTaskContextHolder windowTaskContextHolder;
    private final DeferredReturnHomeReplayCoordinator returnHomeReplayCoordinator;
    private final ObjectMapper objectMapper;

    public BagLocalOperationExecutor(BagService bagService,
                                     FiveRingIncenseObservationLocalMechanics incenseObservationMechanics,
                                     WindowTaskContextHolder windowTaskContextHolder,
                                     DeferredReturnHomeReplayCoordinator returnHomeReplayCoordinator,
                                     ObjectMapper objectMapper) {
        this.bagService = Objects.requireNonNull(bagService, "bagService");
        this.incenseObservationMechanics = Objects.requireNonNull(
                incenseObservationMechanics, "incenseObservationMechanics");
        this.windowTaskContextHolder = Objects.requireNonNull(windowTaskContextHolder, "windowTaskContextHolder");
        this.returnHomeReplayCoordinator = Objects.requireNonNull(
                returnHomeReplayCoordinator, "returnHomeReplayCoordinator");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
    }

    /**
     * Execute one validated Bag local-Service call from inside the existing exclusive input callback.
     *
     * <p>This adapter never acquires the input queue. The two BagService entry points enforce that the
     * caller is already on the input worker, preserving the existing indivisible bag macro boundary.</p>
     *
     * @param call typed local-Service call; only BAG_RETURN_ITEM and BAG_USE_INCENSE are supported
     * @return completed mechanical result with typed JSON, or a fail-closed result before physical input
     */
    public LocalServiceExecution execute(TurnLocalServiceCall call) {
        if (call == null || call.operation() == null) {
            return LocalServiceExecution.failed("INVALID_LOCAL_SERVICE_CALL", null);
        }
        return switch (call.operation()) {
            case BAG_RETURN_ITEM -> executeReturnItem(call);
            case BAG_USE_INCENSE -> executeUseIncense(call);
            default -> LocalServiceExecution.failed("UNSUPPORTED_LOCAL_OPERATION", null);
        };
    }

    /**
     * TURN-40B-C2 queue-owning entry, dispatched WITHOUT an outer exclusive callback: each case
     * calls one public {@code BagService} entry that owns the single input queue itself. Guarded
     * admission (identity predicate then captured token) runs inside {@code withMainBagOpenGuarded}'s
     * exclusive callback before any physical input; this adapter alone converts the resulting
     * {@link TaskStopRequestedException} into the closed typed local stop.
     *
     * @param call validated queue-owning bag call.
     * @param stopToken captured live token of the action-owning task; nullable.
     * @param actionTaskStillCurrent live reference-identity predicate over the captured handle.
     */
    public LocalServiceExecution executeQueueOwning(TurnLocalServiceCall call,
                                                    TaskStopToken stopToken,
                                                    BooleanSupplier actionTaskStillCurrent,
                                                    String actionId,
                                                    int sourceStepIndex,
                                                    TurnContinuationGateway continuationGateway) {
        if (call == null || call.operation() == null || call.bag() == null
                || actionTaskStillCurrent == null) {
            return LocalServiceExecution.failed("INVALID_LOCAL_SERVICE_CALL", null);
        }
        TurnBagOperationArguments bag = call.bag();
        try {
            return switch (call.operation()) {
                case BAG_FIVERING_SUPPLY_CHECK -> {
                    // One guarded open/close session in the frozen baseline order: existing
                    // incense activation, frozen stop checkpoint, then the bounded item count.
                    int requiredCount = bag.maxBagIndex();
                    SupplyCheckResult supply = bagService.withMainBagOpenGuarded(
                            bag.source(), actionTaskStillCurrent, stopToken, mainBag -> {
                                boolean incenseRefilled = continueIncenseInsideOpenBag(
                                        mainBag, actionId, sourceStepIndex, continuationGateway);
                                if (stopToken != null) {
                                    stopToken.throwIfStopRequested();
                                }
                                BagService.ItemCountResult count =
                                        mainBag.countItemUpTo(bag.targetItemTemplate(), requiredCount);
                                return new SupplyCheckResult(
                                        incenseRefilled, count.firstPageIndex(), count.count());
                            });
                    yield supply == null
                            ? LocalServiceExecution.failed("BAG_SESSION_UNAVAILABLE", null)
                            : LocalServiceExecution.completed("OK", json(supply), null);
                }
                case BAG_FIND_AND_USE_FROM_BACK -> {
                    boolean used = bagService.findAndUseItemFromBack(
                            BagService.MAIN_BAG, bag.targetItemTemplate(), bag.maxBagIndex(), null);
                    yield LocalServiceExecution.completed(
                            "OK", json(new BooleanResult(used)), null);
                }
                case BAG_FIND_ITEM_PAGE_INDEX -> {
                    Integer pageIndex = bagService.findItemPageIndex(
                            BagService.MAIN_BAG, bag.targetItemTemplate(), null);
                    yield LocalServiceExecution.completed(
                            "OK", json(new PageIndexResult(pageIndex)), null);
                }
                default -> LocalServiceExecution.failed("UNSUPPORTED_LOCAL_OPERATION", null);
            };
        } catch (TaskStopRequestedException stop) {
            // Sole mapping site: guarded identity/stop rejection becomes the closed typed stop.
            return LocalServiceExecution.stopped(json(new FailureResult(stop.getMessage())));
        }
    }

    private boolean continueIncenseInsideOpenBag(
            BagService.MainBagSession mainBag,
            String actionId,
            int sourceStepIndex,
            TurnContinuationGateway continuationGateway) {
        Objects.requireNonNull(continuationGateway, "continuationGateway");
        TurnContinuationDecision decision = continuationGateway.exchange(
                continuation(actionId, sourceStepIndex, TurnContinuationRequest.Stage.TICK, null, null),
                null);
        if (decision.directive() == TurnContinuationDecision.Directive.CAPTURE_STATUS) {
            TurnFrame frame = incenseObservationMechanics.capture(sourceStepIndex);
            decision = continuationGateway.exchange(
                    continuation(actionId, sourceStepIndex,
                            TurnContinuationRequest.Stage.STATUS_IMAGE, frame, null),
                    frame);
        }
        if (decision.directive() == TurnContinuationDecision.Directive.KEEP_INCENSE) {
            return false;
        }
        if (decision.directive() != TurnContinuationDecision.Directive.USE_INCENSE
                || decision.decisionId() == null) {
            throw new IllegalStateException("unexpected incense continuation directive: " + decision.directive());
        }

        boolean used = mainBag.useItem("bag/sheyaoxiang_item.png", null);
        if (used && !TaskSleep.sleep(1_000L)) {
            throw new TaskStopRequestedException("Five-ring incense use wait interrupted");
        }
        TurnContinuationRequest.Stage outcomeStage = used
                ? TurnContinuationRequest.Stage.OUTCOME_USED
                : TurnContinuationRequest.Stage.OUTCOME_NOT_FOUND;
        TurnContinuationDecision completed = continuationGateway.exchange(
                continuation(actionId, sourceStepIndex, outcomeStage, null, decision.decisionId()),
                null);
        if (completed.directive() != TurnContinuationDecision.Directive.COMPLETE) {
            throw new IllegalStateException("incense continuation outcome was not completed");
        }
        return used;
    }

    private static TurnContinuationRequest continuation(
            String actionId,
            int sourceStepIndex,
            TurnContinuationRequest.Stage stage,
            TurnFrame frame,
            String decisionId) {
        return new TurnContinuationRequest(
                Objects.requireNonNull(actionId, "actionId"),
                sourceStepIndex,
                TurnContinuationRequest.Kind.FIVERING_INCENSE,
                stage,
                frame == null ? null : frame.metadata(),
                decisionId);
    }

    private LocalServiceExecution executeReturnItem(TurnLocalServiceCall call) {
        if (call.bag() == null || call.ui() != null || call.giveItem() != null || call.quest() != null) {
            return LocalServiceExecution.failed("INVALID_BAG_ARGUMENTS", null);
        }
        if (windowTaskContextHolder.rawCurrent()
                .map(context -> context.isLocalCombatVisible())
                .orElse(false)) {
            // This is the final physical-input gate. Cloud phase recovery may lag behind the exact
            // Runner state, but a return-item macro must never open the bag during combat.
            return LocalServiceExecution.failed("LOCAL_COMBAT_ACTIVE", null);
        }

        BagReturnItemMacroIntent intent;
        try {
            intent = toIntent(call.bag());
        } catch (IllegalArgumentException invalid) {
            return LocalServiceExecution.failed(
                    "INVALID_BAG_ARGUMENTS", json(new FailureResult(invalid.getMessage())));
        }

        BagReturnItemMacroResult result = bagService.runReturnItemMacroDirectForExclusive(intent, null);
        if (result.getStatus() == BagReturnItemMacroResult.Status.USED) {
            windowTaskContextHolder.rawCurrent().ifPresent(
                    context -> returnHomeReplayCoordinator.retainExecuted(context, call.bag()));
        }
        TurnReturnItemCachePoint cachePoint = result.getCachePoint() == null
                ? null
                : toTurnCachePoint(result.getCachePoint());
        BagReturnItemResult localResult = new BagReturnItemResult(
                call.bag().intent(), result.getStatus(), cachePoint);
        return LocalServiceExecution.completed("OK", json(localResult), null);
    }

    private LocalServiceExecution executeUseIncense(TurnLocalServiceCall call) {
        if (call.bag() != null || call.ui() != null || call.giveItem() != null || call.quest() != null) {
            return LocalServiceExecution.failed("INVALID_BAG_ARGUMENTS", null);
        }

        boolean used = bagService.runUseIncenseMacroDirectForExclusive(null);
        BagUseIncenseResult localResult = new BagUseIncenseResult(
                used ? BagUseIncenseState.USED : BagUseIncenseState.NOT_FOUND);
        return LocalServiceExecution.completed("OK", json(localResult), null);
    }

    private static BagReturnItemMacroIntent toIntent(TurnBagOperationArguments arguments) {
        requireText(arguments.source(), "bag.source");
        if (arguments.intent() == null) {
            throw new IllegalArgumentException("bag.intent must not be null");
        }
        return switch (arguments.intent()) {
            case PRESCAN_TASK_PAGE -> {
                requireText(arguments.targetItemTemplate(), "bag.targetItemTemplate");
                require(arguments.maxBagIndex() != null && arguments.maxBagIndex() == -1,
                        "PRESCAN_TASK_PAGE requires maxBagIndex=-1");
                require(arguments.cachedPoint() == null,
                        "PRESCAN_TASK_PAGE must not contain cachedPoint");
                yield BagReturnItemMacroIntent.prescanTaskPage(
                        arguments.targetItemTemplate(), arguments.source());
            }
            case PRESCAN_FROM_BACK -> {
                requireText(arguments.targetItemTemplate(), "bag.targetItemTemplate");
                require(arguments.maxBagIndex() != null,
                        "PRESCAN_FROM_BACK requires maxBagIndex");
                require(arguments.cachedPoint() == null,
                        "PRESCAN_FROM_BACK must not contain cachedPoint");
                yield BagReturnItemMacroIntent.prescanFromBack(
                        arguments.targetItemTemplate(), arguments.maxBagIndex(), arguments.source());
            }
            case USE_CACHED_RETURN_ITEM -> {
                require(arguments.targetItemTemplate() == null,
                        "USE_CACHED_RETURN_ITEM must not contain targetItemTemplate");
                require(arguments.maxBagIndex() != null && arguments.maxBagIndex() == -1,
                        "USE_CACHED_RETURN_ITEM requires maxBagIndex=-1");
                yield BagReturnItemMacroIntent.useCachedReturnItem(
                        toDomainCachePoint(arguments.cachedPoint()), arguments.source());
            }
            case FIND_AND_USE_TASK_PAGE -> {
                requireText(arguments.targetItemTemplate(), "bag.targetItemTemplate");
                require(arguments.maxBagIndex() != null && arguments.maxBagIndex() == -1,
                        "FIND_AND_USE_TASK_PAGE requires maxBagIndex=-1");
                require(arguments.cachedPoint() == null,
                        "FIND_AND_USE_TASK_PAGE must not contain cachedPoint");
                yield BagReturnItemMacroIntent.findAndUseTaskPage(
                        arguments.targetItemTemplate(), arguments.source());
            }
        };
    }

    private static ReturnItemCachePoint toDomainCachePoint(TurnReturnItemCachePoint point) {
        if (point == null) {
            return null;
        }
        requireText(point.templatePath(), "cachedPoint.templatePath");
        require(point.learnedAtMs() >= 0L, "cachedPoint.learnedAtMs must not be negative");
        requireText(point.source(), "cachedPoint.source");
        return ReturnItemCachePoint.builder()
                .templatePath(point.templatePath())
                .clickX(point.clickX())
                .clickY(point.clickY())
                .learnedAtMs(point.learnedAtMs())
                .source(point.source())
                .build();
    }

    private static TurnReturnItemCachePoint toTurnCachePoint(ReturnItemCachePoint point) {
        return new TurnReturnItemCachePoint(
                point.getTemplatePath(),
                point.getClickX(),
                point.getClickY(),
                point.getLearnedAtMs(),
                point.getSource());
    }

    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Bag local result cannot be serialized", e);
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalArgumentException(message);
        }
    }

    private static void requireText(String value, String field) {
        require(value != null && !value.isBlank(), field + " must not be blank");
    }

    private record BagReturnItemResult(
            TurnBagOperationArguments.ReturnItemIntent intent,
            BagReturnItemMacroResult.Status state,
            TurnReturnItemCachePoint cachePoint) {
    }

    private record BagUseIncenseResult(BagUseIncenseState state) {
    }

    /** Frozen three-field one-session supply result: incense, first page index, bounded count. */
    private record SupplyCheckResult(boolean incenseRefilled, Integer firstPageIndex, int count) {
    }

    private record BooleanResult(boolean result) {
    }

    private record PageIndexResult(Integer pageIndex) {
    }

    private record FailureResult(String reason) {
    }

    private enum BagUseIncenseState {
        USED,
        NOT_FOUND
    }
}
