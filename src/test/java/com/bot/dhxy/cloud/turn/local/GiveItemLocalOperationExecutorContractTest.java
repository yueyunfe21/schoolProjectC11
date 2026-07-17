package com.bot.dhxy.cloud.turn.local;

import com.bot.dhxy.cloud.turn.LocalServiceExecution;
import com.bot.dhxy.cloud.turn.protocol.TurnBagOperationArguments;
import com.bot.dhxy.cloud.turn.protocol.TurnGiveItemOperationArguments;
import com.bot.dhxy.cloud.turn.protocol.TurnLocalOperation;
import com.bot.dhxy.cloud.turn.protocol.TurnLocalServiceCall;
import com.bot.dhxy.cloud.turn.protocol.TurnStepResult;
import com.bot.dhxy.service.GiveItemService;
import com.bot.dhxy.service.GiveItemService.OpenDialogGiveState;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class GiveItemLocalOperationExecutorContractTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private FakeGiveItemService giveItemService;
    private GiveItemLocalOperationExecutor executor;

    @BeforeEach
    void setUp() {
        giveItemService = new FakeGiveItemService();
        executor = new GiveItemLocalOperationExecutor(giveItemService, objectMapper);
    }

    @Test
    void delegatesTheWholeOpenDialogGiveMacroOnceAndReturnsGivenState() {
        giveItemService.result = OpenDialogGiveState.GIVEN;

        LocalServiceExecution result = executor.execute(call("items/dragon.png", 3));

        assertCompleted(result);
        assertEquals(1, giveItemService.wholeMacroCalls);
        assertEquals(0, giveItemService.legacyDirectCalls);
        assertEquals("items/dragon.png", giveItemService.lastTemplate);
        assertEquals(3, giveItemService.lastBagIndex);
        assertEquals("{\"state\":\"GIVEN\"}", result.localResultJson());
    }

    @Test
    void preservesGiveOptionNotFoundAsTypedCompletedState() {
        giveItemService.result = OpenDialogGiveState.GIVE_OPTION_NOT_FOUND;

        LocalServiceExecution result = executor.execute(call("items/missing.png", null));

        assertCompleted(result);
        assertEquals(1, giveItemService.wholeMacroCalls);
        assertEquals(0, giveItemService.legacyDirectCalls);
        assertEquals("{\"state\":\"GIVE_OPTION_NOT_FOUND\"}", result.localResultJson());
    }

    @Test
    void preservesGiveItemFailedAsTypedCompletedState() {
        giveItemService.result = OpenDialogGiveState.GIVE_ITEM_FAILED;

        LocalServiceExecution result = executor.execute(call("items/missing.png", 4));

        assertCompleted(result);
        assertEquals(1, giveItemService.wholeMacroCalls);
        assertEquals(0, giveItemService.legacyDirectCalls);
        assertEquals("{\"state\":\"GIVE_ITEM_FAILED\"}", result.localResultJson());
    }

    @Test
    void preservesInterruptedAsTypedCompletedState() {
        giveItemService.result = OpenDialogGiveState.INTERRUPTED;

        LocalServiceExecution result = executor.execute(call("items/shoe.png", 1));

        assertCompleted(result);
        assertEquals(1, giveItemService.wholeMacroCalls);
        assertEquals(0, giveItemService.legacyDirectCalls);
        assertEquals("{\"state\":\"INTERRUPTED\"}", result.localResultJson());
    }

    @Test
    void invalidOrUnsupportedCallsFailClosedWithoutSplittingOrInvokingMacro() {
        assertFailed(executor.execute(null), "INVALID_LOCAL_SERVICE_CALL");
        assertFailed(executor.execute(new TurnLocalServiceCall(null, null, null, null, null)),
                "INVALID_LOCAL_SERVICE_CALL");
        assertFailed(executor.execute(call(" ", 1)), "INVALID_GIVE_ITEM_ARGUMENTS");
        assertFailed(executor.execute(new TurnLocalServiceCall(
                        TurnLocalOperation.GIVE_ITEM_FROM_OPEN_DIALOG,
                        new TurnBagOperationArguments(null, null, null, null, null),
                        null,
                        new TurnGiveItemOperationArguments("item.png", 1),
                        null)),
                "INVALID_GIVE_ITEM_ARGUMENTS");
        assertFailed(executor.execute(new TurnLocalServiceCall(
                        TurnLocalOperation.QUEST_ACTIVATE, null, null,
                        new TurnGiveItemOperationArguments("item.png", 1), null)),
                "UNSUPPORTED_LOCAL_OPERATION");

        assertEquals(0, giveItemService.wholeMacroCalls);
        assertEquals(0, giveItemService.legacyDirectCalls);
    }

    private static TurnLocalServiceCall call(String template, Integer bagIndex) {
        return new TurnLocalServiceCall(
                TurnLocalOperation.GIVE_ITEM_FROM_OPEN_DIALOG,
                null, null, new TurnGiveItemOperationArguments(template, bagIndex), null);
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

    private static final class FakeGiveItemService extends GiveItemService {
        private int wholeMacroCalls;
        private int legacyDirectCalls;
        private String lastTemplate;
        private Integer lastBagIndex;
        private OpenDialogGiveState result;

        private FakeGiveItemService() {
            super(null, null, null, null);
        }

        @Override
        public OpenDialogGiveState executeGiveFromOpenDialogDirectForExclusive(String targetItemTemplate,
                                                                                Integer knownBagIndex) {
            wholeMacroCalls++;
            lastTemplate = targetItemTemplate;
            lastBagIndex = knownBagIndex;
            return result;
        }

        @Override
        public boolean executeGiveDirectForExclusive(String targetItemTemplate, Integer knownBagIndex) {
            legacyDirectCalls++;
            return result == OpenDialogGiveState.GIVEN;
        }
    }
}
