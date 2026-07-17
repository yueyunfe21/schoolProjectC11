package com.bot.dhxy.cloud.turn.local;

import com.bot.dhxy.cloud.turn.LocalServiceExecution;
import com.bot.dhxy.cloud.turn.protocol.TurnBagOperationArguments;
import com.bot.dhxy.cloud.turn.protocol.TurnLocalServiceCall;
import com.bot.dhxy.cloud.turn.protocol.TurnReturnItemCachePoint;
import com.bot.dhxy.model.bag.ReturnItemCachePoint;
import com.bot.dhxy.service.BagService;
import com.bot.dhxy.service.bag.BagReturnItemMacroIntent;
import com.bot.dhxy.service.bag.BagReturnItemMacroResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.Objects;

/** Closed adapter for the two permanent-local BagService turn operations. */
@Component
public final class BagLocalOperationExecutor {

    private final BagService bagService;
    private final ObjectMapper objectMapper;

    public BagLocalOperationExecutor(BagService bagService, ObjectMapper objectMapper) {
        this.bagService = Objects.requireNonNull(bagService, "bagService");
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

    private LocalServiceExecution executeReturnItem(TurnLocalServiceCall call) {
        if (call.bag() == null || call.ui() != null || call.giveItem() != null || call.quest() != null) {
            return LocalServiceExecution.failed("INVALID_BAG_ARGUMENTS", null);
        }

        BagReturnItemMacroIntent intent;
        try {
            intent = toIntent(call.bag());
        } catch (IllegalArgumentException invalid) {
            return LocalServiceExecution.failed(
                    "INVALID_BAG_ARGUMENTS", json(new FailureResult(invalid.getMessage())));
        }

        BagReturnItemMacroResult result = bagService.runReturnItemMacroDirectForExclusive(intent, null);
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

    private record FailureResult(String reason) {
    }

    private enum BagUseIncenseState {
        USED,
        NOT_FOUND
    }
}
