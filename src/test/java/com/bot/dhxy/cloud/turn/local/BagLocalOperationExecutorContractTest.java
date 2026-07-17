package com.bot.dhxy.cloud.turn.local;

import com.bot.dhxy.cloud.turn.LocalServiceExecution;
import com.bot.dhxy.cloud.turn.protocol.TurnBagOperationArguments;
import com.bot.dhxy.cloud.turn.protocol.TurnGiveItemOperationArguments;
import com.bot.dhxy.cloud.turn.protocol.TurnLocalOperation;
import com.bot.dhxy.cloud.turn.protocol.TurnLocalServiceCall;
import com.bot.dhxy.cloud.turn.protocol.TurnReturnItemCachePoint;
import com.bot.dhxy.cloud.turn.protocol.TurnStepResult;
import com.bot.dhxy.model.bag.ReturnItemCachePoint;
import com.bot.dhxy.runner.context.TaskExecutionContext;
import com.bot.dhxy.service.BagService;
import com.bot.dhxy.service.bag.BagReturnItemMacroIntent;
import com.bot.dhxy.service.bag.BagReturnItemMacroResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class BagLocalOperationExecutorContractTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private FakeBagService bagService;
    private BagLocalOperationExecutor executor;

    @BeforeEach
    void setUp() {
        bagService = new FakeBagService();
        executor = new BagLocalOperationExecutor(bagService, objectMapper);
    }

    @Test
    void mapsPrescanFromBackArgumentsAndFoundResultExactly() throws Exception {
        ReturnItemCachePoint point = ReturnItemCachePoint.builder()
                .templatePath("return/item.png")
                .clickX(417)
                .clickY(286)
                .learnedAtMs(1234L)
                .source("prescan-result")
                .build();
        bagService.returnItemResult = BagReturnItemMacroResult.found(point);
        TurnBagOperationArguments arguments = new TurnBagOperationArguments(
                TurnBagOperationArguments.ReturnItemIntent.PRESCAN_FROM_BACK,
                "return/item.png", 4, null, "xiuluo");

        LocalServiceExecution result = executor.execute(call(TurnLocalOperation.BAG_RETURN_ITEM, arguments));

        assertCompleted(result);
        assertEquals(1, bagService.returnItemCalls);
        assertEquals(BagReturnItemMacroIntent.Kind.PRESCAN_FROM_BACK, bagService.lastIntent.getKind());
        assertEquals("return/item.png", bagService.lastIntent.getTargetItemTemplate());
        assertEquals(4, bagService.lastIntent.getMaxBagIndex());
        assertEquals("xiuluo", bagService.lastIntent.getSource());
        JsonNode json = objectMapper.readTree(result.localResultJson());
        assertEquals("PRESCAN_FROM_BACK", json.get("intent").asText());
        assertEquals("FOUND", json.get("state").asText());
        assertEquals(417, json.at("/cachePoint/clickX").asInt());
        assertEquals(286, json.at("/cachePoint/clickY").asInt());
        assertEquals("prescan-result", json.at("/cachePoint/source").asText());
    }

    @Test
    void mapsTaskPageAndCachedPointIntentsWithoutChangingCoordinates() {
        bagService.returnItemResult = BagReturnItemMacroResult.notFound();
        executor.execute(call(TurnLocalOperation.BAG_RETURN_ITEM, new TurnBagOperationArguments(
                TurnBagOperationArguments.ReturnItemIntent.PRESCAN_TASK_PAGE,
                "task/item.png", -1, null, "wuhuan")));

        assertEquals(1, bagService.returnItemCalls);
        assertEquals(BagReturnItemMacroIntent.Kind.PRESCAN_TASK_PAGE, bagService.lastIntent.getKind());
        assertEquals(-1, bagService.lastIntent.getMaxBagIndex());

        TurnReturnItemCachePoint cachedPoint = new TurnReturnItemCachePoint(
                "cached/item.png", 109, 208, 9876L, "cache-source");
        bagService.returnItemResult = BagReturnItemMacroResult.used();
        LocalServiceExecution cachedResult = executor.execute(call(
                TurnLocalOperation.BAG_RETURN_ITEM,
                new TurnBagOperationArguments(
                        TurnBagOperationArguments.ReturnItemIntent.USE_CACHED_RETURN_ITEM,
                        null, -1, cachedPoint, "five-ring")));

        assertEquals(2, bagService.returnItemCalls);
        assertEquals(BagReturnItemMacroIntent.Kind.USE_CACHED_RETURN_ITEM, bagService.lastIntent.getKind());
        assertEquals(109, bagService.lastIntent.getCachedPoint().getClickX());
        assertEquals(208, bagService.lastIntent.getCachedPoint().getClickY());
        assertEquals(9876L, bagService.lastIntent.getCachedPoint().getLearnedAtMs());
        assertEquals("cache-source", bagService.lastIntent.getCachedPoint().getSource());
        assertCompleted(cachedResult);
    }

    @Test
    void mapsIncenseBooleanToTypedCompletedResultWithOneServiceCall() throws Exception {
        bagService.incenseResult = true;
        LocalServiceExecution used = executor.execute(call(TurnLocalOperation.BAG_USE_INCENSE, null));

        assertCompleted(used);
        assertEquals(1, bagService.incenseCalls);
        assertEquals("USED", objectMapper.readTree(used.localResultJson()).get("state").asText());

        bagService.incenseResult = false;
        LocalServiceExecution absent = executor.execute(call(TurnLocalOperation.BAG_USE_INCENSE, null));

        assertCompleted(absent);
        assertEquals(2, bagService.incenseCalls);
        assertEquals("NOT_FOUND", objectMapper.readTree(absent.localResultJson()).get("state").asText());
    }

    @Test
    void invalidOrUnsupportedCallsFailClosedBeforeBagService() {
        assertFailed(executor.execute(null), "INVALID_LOCAL_SERVICE_CALL");
        assertFailed(executor.execute(new TurnLocalServiceCall(null, null, null, null, null)),
                "INVALID_LOCAL_SERVICE_CALL");
        assertFailed(executor.execute(new TurnLocalServiceCall(
                        TurnLocalOperation.BAG_RETURN_ITEM,
                        new TurnBagOperationArguments(
                                TurnBagOperationArguments.ReturnItemIntent.PRESCAN_FROM_BACK,
                                "item.png", null, null, "source"),
                        null, null, null)),
                "INVALID_BAG_ARGUMENTS");
        assertFailed(executor.execute(new TurnLocalServiceCall(
                        TurnLocalOperation.BAG_RETURN_ITEM,
                        new TurnBagOperationArguments(
                                TurnBagOperationArguments.ReturnItemIntent.PRESCAN_TASK_PAGE,
                                "item.png", -1, null, "source"),
                        null, new TurnGiveItemOperationArguments("other.png", null), null)),
                "INVALID_BAG_ARGUMENTS");
        assertFailed(executor.execute(call(TurnLocalOperation.UI_CLEAN_ALL, null)),
                "UNSUPPORTED_LOCAL_OPERATION");

        assertEquals(0, bagService.returnItemCalls);
        assertEquals(0, bagService.incenseCalls);
    }

    private static TurnLocalServiceCall call(TurnLocalOperation operation, TurnBagOperationArguments arguments) {
        return new TurnLocalServiceCall(operation, arguments, null, null, null);
    }

    private static void assertCompleted(LocalServiceExecution result) {
        assertEquals(TurnStepResult.Status.COMPLETED, result.status());
        assertEquals("OK", result.code());
        assertNull(result.frame());
    }

    private static void assertFailed(LocalServiceExecution result, String code) {
        assertEquals(TurnStepResult.Status.FAILED, result.status());
        assertEquals(code, result.code());
        assertNull(result.frame());
    }

    private static final class FakeBagService extends BagService {
        private int returnItemCalls;
        private int incenseCalls;
        private BagReturnItemMacroIntent lastIntent;
        private BagReturnItemMacroResult returnItemResult = BagReturnItemMacroResult.notFound();
        private boolean incenseResult;

        private FakeBagService() {
            super(null, null, null, null, null, null);
        }

        @Override
        public BagReturnItemMacroResult runReturnItemMacroDirectForExclusive(
                BagReturnItemMacroIntent intent, TaskExecutionContext context) {
            returnItemCalls++;
            lastIntent = intent;
            return returnItemResult;
        }

        @Override
        public boolean runUseIncenseMacroDirectForExclusive(TaskExecutionContext context) {
            incenseCalls++;
            return incenseResult;
        }
    }
}
