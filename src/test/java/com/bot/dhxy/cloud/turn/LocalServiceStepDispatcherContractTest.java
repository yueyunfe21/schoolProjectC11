package com.bot.dhxy.cloud.turn;

import com.bot.dhxy.cloud.turn.local.BagLocalOperationExecutor;
import com.bot.dhxy.cloud.turn.local.GiveItemLocalOperationExecutor;
import com.bot.dhxy.cloud.turn.local.QuestLocalOperationExecutor;
import com.bot.dhxy.cloud.turn.local.UiLocalOperationExecutor;
import com.bot.dhxy.cloud.turn.protocol.TurnBagOperationArguments;
import com.bot.dhxy.cloud.turn.protocol.TurnGiveItemOperationArguments;
import com.bot.dhxy.cloud.turn.protocol.TurnLocalOperation;
import com.bot.dhxy.cloud.turn.protocol.TurnLocalServiceCall;
import com.bot.dhxy.cloud.turn.protocol.TurnQuestOperationArguments;
import com.bot.dhxy.cloud.turn.protocol.TurnStepResult;
import com.bot.dhxy.cloud.turn.protocol.TurnUiOperationArguments;
import com.bot.dhxy.input.InputSequences;
import com.bot.dhxy.model.quest.QuestDetailCapture;
import com.bot.dhxy.runner.context.TaskExecutionContext;
import com.bot.dhxy.service.BagService;
import com.bot.dhxy.service.GiveItemService;
import com.bot.dhxy.service.GiveItemService.OpenDialogGiveState;
import com.bot.dhxy.service.QuestManagerService;
import com.bot.dhxy.service.UICleanerService;
import com.bot.dhxy.service.bag.BagReturnItemMacroIntent;
import com.bot.dhxy.service.bag.BagReturnItemMacroResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.util.EnumSet;
import java.util.Map;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LocalServiceStepDispatcherContractTest {

    private static final EnumSet<TurnLocalOperation> EXCLUSIVE_OPERATIONS = EnumSet.of(
            TurnLocalOperation.BAG_RETURN_ITEM,
            TurnLocalOperation.BAG_USE_INCENSE,
            TurnLocalOperation.UI_CLOSE_MAP_SEARCH_INPUT_BY_X2,
            TurnLocalOperation.GIVE_ITEM_FROM_OPEN_DIALOG);

    @Test
    void routesAllNineClosedOperationsToOnlyFourPermanentLocalServices() {
        assertEquals(9, TurnLocalOperation.values().length,
                "a new local operation must receive an explicit dispatcher contract");

        for (TurnLocalOperation operation : TurnLocalOperation.values()) {
            Fixture fixture = new Fixture();

            LocalServiceExecution result = fixture.dispatcher.execute(validCall(operation), 7);

            assertEquals(TurnStepResult.Status.COMPLETED, result.status(), operation.name());
            assertEquals("OK", result.code(), operation.name());
            assertNotNull(result.localResultJson(), operation.name());
            assertEquals(1, fixture.totalServiceCalls(), operation.name());
            assertExpectedOwner(fixture, operation);
            assertEquals(EXCLUSIVE_OPERATIONS.contains(operation) ? 1 : 0,
                    fixture.inputSequences.exclusiveCalls, operation.name());
            assertEquals(0, fixture.inputSequences.nestedExclusiveCalls, operation.name());
            if (operation == TurnLocalOperation.QUEST_CAPTURE_DETAIL) {
                assertNotNull(result.frame());
                assertEquals(710, result.frame().metadata().region().x());
                assertEquals(255, result.frame().metadata().region().y());
                assertEquals(7, result.frame().metadata().sourceStepIndex());
            } else {
                assertNull(result.frame(), operation.name());
            }
        }
    }

    @Test
    void giveItemDispatcherPreservesAllFourWholeApiStatesInsideOneExclusiveRequest() {
        Map<OpenDialogGiveState, String> expectedJsonByState = Map.of(
                OpenDialogGiveState.GIVEN, "{\"state\":\"GIVEN\"}",
                OpenDialogGiveState.GIVE_OPTION_NOT_FOUND,
                "{\"state\":\"GIVE_OPTION_NOT_FOUND\"}",
                OpenDialogGiveState.GIVE_ITEM_FAILED, "{\"state\":\"GIVE_ITEM_FAILED\"}",
                OpenDialogGiveState.INTERRUPTED, "{\"state\":\"INTERRUPTED\"}");
        assertEquals(4, OpenDialogGiveState.values().length,
                "the dispatcher contract must explicitly review every whole-API state");

        for (Map.Entry<OpenDialogGiveState, String> expectation : expectedJsonByState.entrySet()) {
            OpenDialogGiveState state = expectation.getKey();
            Fixture fixture = new Fixture();
            fixture.giveService.result = state;

            LocalServiceExecution result = fixture.dispatcher.execute(
                    validCall(TurnLocalOperation.GIVE_ITEM_FROM_OPEN_DIALOG), 5);

            assertEquals(TurnStepResult.Status.COMPLETED, result.status(), state.name());
            assertEquals("OK", result.code(), state.name());
            assertEquals(expectation.getValue(), result.localResultJson(), state.name());
            assertNull(result.frame(), state.name());
            assertEquals(1, fixture.inputSequences.exclusiveCalls, state.name());
            assertEquals(1, fixture.inputSequences.callbackCalls, state.name());
            assertEquals(0, fixture.inputSequences.nestedExclusiveCalls, state.name());
            assertEquals(1, fixture.giveService.wholeApiCalls, state.name());
            assertEquals(0, fixture.giveService.legacyDirectCalls, state.name());
            assertEquals(0, fixture.bagService.totalCalls(), state.name());
            assertEquals(0, fixture.uiService.totalCalls(), state.name());
            assertEquals(0, fixture.questService.totalCalls(), state.name());
        }
    }

    @Test
    void bagAndGiveQueueFailuresDoNotInvokeAdaptersOrServices() {
        for (TurnLocalOperation operation : EnumSet.of(
                TurnLocalOperation.BAG_RETURN_ITEM,
                TurnLocalOperation.BAG_USE_INCENSE,
                TurnLocalOperation.GIVE_ITEM_FROM_OPEN_DIALOG)) {
            Fixture fixture = new Fixture();
            fixture.inputSequences.completeRequest = false;

            LocalServiceExecution result = fixture.dispatcher.execute(validCall(operation), 3);

            assertEquals(TurnStepResult.Status.FAILED, result.status());
            assertEquals("LOCAL_SERVICE_INPUT_FAILED", result.code());
            assertEquals(1, fixture.inputSequences.exclusiveCalls);
            assertEquals(0, fixture.inputSequences.callbackCalls);
            assertEquals(0, fixture.totalServiceCalls());
        }
    }

    @Test
    void nullUnknownAndMalformedCallsFailClosedWithZeroServiceCalls() {
        Fixture fixture = new Fixture();

        LocalServiceExecution nullCall = fixture.dispatcher.execute(null, 0);
        LocalServiceExecution unknownOperation = fixture.dispatcher.execute(
                new TurnLocalServiceCall(null, null, null, null, null), 0);
        LocalServiceExecution malformedUi = fixture.dispatcher.execute(
                new TurnLocalServiceCall(
                        TurnLocalOperation.UI_CLEAN_LIGHTWEIGHT,
                        null, new TurnUiOperationArguments(" "), null, null), 0);
        LocalServiceExecution malformedQuest = fixture.dispatcher.execute(
                new TurnLocalServiceCall(
                        TurnLocalOperation.QUEST_ACTIVATE,
                        null, null, null, new TurnQuestOperationArguments("task", null)), 0);

        assertFailed(nullCall, "INVALID_LOCAL_SERVICE_CALL");
        assertFailed(unknownOperation, "INVALID_LOCAL_SERVICE_CALL");
        assertFailed(malformedUi, "INVALID_UI_ARGUMENTS");
        assertFailed(malformedQuest, "INVALID_QUEST_ARGUMENTS");
        assertEquals(0, fixture.totalServiceCalls());
        assertEquals(0, fixture.inputSequences.exclusiveCalls);
    }

    @Test
    void unknownWireOperationCannotReachDispatcherOrAnyLocalService() {
        Fixture fixture = new Fixture();

        assertThrows(JsonProcessingException.class, () -> fixture.objectMapper.readValue(
                "{\"operation\":\"UNKNOWN_LOCAL_OPERATION\",\"bag\":null,\"ui\":null,"
                        + "\"giveItem\":null,\"quest\":null}",
                TurnLocalServiceCall.class));

        assertEquals(0, fixture.totalServiceCalls());
        assertEquals(0, fixture.inputSequences.exclusiveCalls);
    }

    private static void assertExpectedOwner(Fixture fixture, TurnLocalOperation operation) {
        int bag = switch (operation) {
            case BAG_RETURN_ITEM, BAG_USE_INCENSE -> 1;
            default -> 0;
        };
        int ui = switch (operation) {
            case UI_CLEAN_ALL, UI_CLOSE_GENERIC_WINDOWS, UI_CLEAN_LIGHTWEIGHT,
                    UI_CLOSE_MAP_SEARCH_INPUT_BY_X2 -> 1;
            default -> 0;
        };
        int give = operation == TurnLocalOperation.GIVE_ITEM_FROM_OPEN_DIALOG ? 1 : 0;
        int quest = switch (operation) {
            case QUEST_ACTIVATE, QUEST_CAPTURE_DETAIL -> 1;
            default -> 0;
        };
        assertEquals(bag, fixture.bagService.totalCalls(), operation.name());
        assertEquals(ui, fixture.uiService.totalCalls(), operation.name());
        assertEquals(give, fixture.giveService.wholeApiCalls, operation.name());
        assertEquals(0, fixture.giveService.legacyDirectCalls, operation.name());
        assertEquals(quest, fixture.questService.totalCalls(), operation.name());
    }

    private static TurnLocalServiceCall validCall(TurnLocalOperation operation) {
        return switch (operation) {
            case BAG_RETURN_ITEM -> new TurnLocalServiceCall(operation,
                    new TurnBagOperationArguments(
                            TurnBagOperationArguments.ReturnItemIntent.PRESCAN_FROM_BACK,
                            "return/item.png", 4, null, "dispatcher"),
                    null, null, null);
            case BAG_USE_INCENSE -> new TurnLocalServiceCall(operation, null, null, null, null);
            case UI_CLEAN_ALL, UI_CLOSE_GENERIC_WINDOWS ->
                    new TurnLocalServiceCall(operation, null, null, null, null);
            case UI_CLEAN_LIGHTWEIGHT, UI_CLOSE_MAP_SEARCH_INPUT_BY_X2 ->
                    new TurnLocalServiceCall(operation, null,
                            new TurnUiOperationArguments("dispatcher"), null, null);
            case GIVE_ITEM_FROM_OPEN_DIALOG -> new TurnLocalServiceCall(operation, null, null,
                    new TurnGiveItemOperationArguments("give/item.png", 2), null);
            case QUEST_ACTIVATE -> new TurnLocalServiceCall(operation, null, null, null,
                    new TurnQuestOperationArguments("xiuluo", false));
            case QUEST_CAPTURE_DETAIL -> new TurnLocalServiceCall(operation, null, null, null,
                    new TurnQuestOperationArguments("xiuluo", null));
        };
    }

    private static void assertFailed(LocalServiceExecution result, String code) {
        assertEquals(TurnStepResult.Status.FAILED, result.status());
        assertEquals(code, result.code());
        assertNull(result.frame());
    }

    private static final class Fixture {
        private final ObjectMapper objectMapper = new ObjectMapper();
        private final FakeBagService bagService = new FakeBagService();
        private final FakeUiCleanerService uiService = new FakeUiCleanerService();
        private final FakeGiveItemService giveService = new FakeGiveItemService();
        private final FakeQuestManagerService questService = new FakeQuestManagerService();
        private final RecordingInputSequences inputSequences = new RecordingInputSequences();
        private final LocalServiceStepDispatcher dispatcher;

        private Fixture() {
            BufferedImage image = new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);
            image.setRGB(0, 0, 0x123456);
            questService.capture = new QuestDetailCapture(image, "", 710, 255);
            dispatcher = new LocalServiceStepDispatcher(
                    new BagLocalOperationExecutor(bagService, objectMapper),
                    new UiLocalOperationExecutor(uiService, inputSequences, objectMapper),
                    new GiveItemLocalOperationExecutor(giveService, objectMapper),
                    new QuestLocalOperationExecutor(questService, new TurnPngCodec(), objectMapper),
                    inputSequences);
        }

        private int totalServiceCalls() {
            return bagService.totalCalls()
                    + uiService.totalCalls()
                    + giveService.totalCalls()
                    + questService.totalCalls();
        }
    }

    private static final class RecordingInputSequences extends InputSequences {
        private int exclusiveCalls;
        private int callbackCalls;
        private int nestedExclusiveCalls;
        private int depth;
        private boolean completeRequest = true;

        private RecordingInputSequences() {
            super(null);
        }

        @Override
        public boolean submitExclusiveAndWait(String description, Supplier<Boolean> callback) {
            exclusiveCalls++;
            if (depth > 0) {
                nestedExclusiveCalls++;
            }
            if (!completeRequest) {
                return false;
            }
            depth++;
            try {
                callbackCalls++;
                return callback.get();
            } finally {
                depth--;
            }
        }
    }

    private static final class FakeBagService extends BagService {
        private int returnCalls;
        private int incenseCalls;

        private FakeBagService() {
            super(null, null, null, null, null, null);
        }

        @Override
        public BagReturnItemMacroResult runReturnItemMacroDirectForExclusive(
                BagReturnItemMacroIntent intent, TaskExecutionContext context) {
            returnCalls++;
            return BagReturnItemMacroResult.notFound();
        }

        @Override
        public boolean runUseIncenseMacroDirectForExclusive(TaskExecutionContext context) {
            incenseCalls++;
            return true;
        }

        private int totalCalls() {
            return returnCalls + incenseCalls;
        }
    }

    private static final class FakeUiCleanerService extends UICleanerService {
        private int cleanAllCalls;
        private int genericCalls;
        private int lightweightCalls;
        private int x2Calls;

        private FakeUiCleanerService() {
            super(null, null, null, null, null, null, null, null, null);
        }

        @Override
        public void cleanUpAll() {
            cleanAllCalls++;
        }

        @Override
        public boolean closeAllGenericWindows() {
            genericCalls++;
            return true;
        }

        @Override
        public boolean cleanLightweightInterruptions(String sourceTask) {
            lightweightCalls++;
            return true;
        }

        @Override
        public boolean closeMapSearchInputByX2Direct(String description) {
            x2Calls++;
            return true;
        }

        private int totalCalls() {
            return cleanAllCalls + genericCalls + lightweightCalls + x2Calls;
        }
    }

    private static final class FakeGiveItemService extends GiveItemService {
        private int wholeApiCalls;
        private int legacyDirectCalls;
        private OpenDialogGiveState result = OpenDialogGiveState.GIVEN;

        private FakeGiveItemService() {
            super(null, null, null, null);
        }

        @Override
        public OpenDialogGiveState executeGiveFromOpenDialogDirectForExclusive(String targetItemTemplate,
                                                                                Integer knownBagIndex) {
            wholeApiCalls++;
            return result;
        }

        @Override
        public boolean executeGiveDirectForExclusive(String targetItemTemplate, Integer knownBagIndex) {
            legacyDirectCalls++;
            return true;
        }

        private int totalCalls() {
            return wholeApiCalls + legacyDirectCalls;
        }
    }

    private static final class FakeQuestManagerService extends QuestManagerService {
        private int activateCalls;
        private int captureCalls;
        private QuestDetailCapture capture;

        private FakeQuestManagerService() {
            super(null, null, null, null, null, null, null);
        }

        @Override
        public boolean activateTaskIfPresent(String task, boolean keepOpen) {
            activateCalls++;
            return true;
        }

        @Override
        public QuestDetailCapture captureCurrentQuestDetailForTask(String task) {
            captureCalls++;
            return capture;
        }

        private int totalCalls() {
            return activateCalls + captureCalls;
        }
    }
}
