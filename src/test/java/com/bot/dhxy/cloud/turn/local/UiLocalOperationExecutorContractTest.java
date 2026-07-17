package com.bot.dhxy.cloud.turn.local;

import com.bot.dhxy.cloud.turn.LocalServiceExecution;
import com.bot.dhxy.cloud.turn.protocol.TurnBagOperationArguments;
import com.bot.dhxy.cloud.turn.protocol.TurnLocalOperation;
import com.bot.dhxy.cloud.turn.protocol.TurnLocalServiceCall;
import com.bot.dhxy.cloud.turn.protocol.TurnStepResult;
import com.bot.dhxy.cloud.turn.protocol.TurnUiOperationArguments;
import com.bot.dhxy.input.InputSequences;
import com.bot.dhxy.service.UICleanerService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class UiLocalOperationExecutorContractTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private FakeUiCleanerService uiCleanerService;
    private RecordingInputSequences inputSequences;
    private UiLocalOperationExecutor executor;

    @BeforeEach
    void setUp() {
        uiCleanerService = new FakeUiCleanerService();
        inputSequences = new RecordingInputSequences();
        executor = new UiLocalOperationExecutor(uiCleanerService, inputSequences, objectMapper);
    }

    @Test
    void threeServiceOwnedQueueOperationsAreNotWrappedByAdapter() throws Exception {
        uiCleanerService.genericResult = true;
        uiCleanerService.lightweightResult = false;

        LocalServiceExecution cleanAll = executor.execute(call(TurnLocalOperation.UI_CLEAN_ALL, null));
        assertHandled(cleanAll, TurnLocalOperation.UI_CLEAN_ALL, true);
        assertEquals(1, uiCleanerService.cleanAllCalls);
        assertEquals(0, inputSequences.exclusiveCalls);

        LocalServiceExecution generic = executor.execute(call(
                TurnLocalOperation.UI_CLOSE_GENERIC_WINDOWS, null));
        assertHandled(generic, TurnLocalOperation.UI_CLOSE_GENERIC_WINDOWS, true);
        assertEquals(1, uiCleanerService.genericCalls);
        assertEquals(0, inputSequences.exclusiveCalls);

        LocalServiceExecution lightweight = executor.execute(call(
                TurnLocalOperation.UI_CLEAN_LIGHTWEIGHT,
                new TurnUiOperationArguments("maintenance")));
        assertHandled(lightweight, TurnLocalOperation.UI_CLEAN_LIGHTWEIGHT, false);
        assertEquals(1, uiCleanerService.lightweightCalls);
        assertEquals("maintenance", uiCleanerService.lastSource);
        assertEquals(0, inputSequences.exclusiveCalls);
    }

    @Test
    void x2UsesExactlyOneExclusiveBoundaryAndOneDirectServiceCall() throws Exception {
        uiCleanerService.x2Result = true;

        LocalServiceExecution result = executor.execute(call(
                TurnLocalOperation.UI_CLOSE_MAP_SEARCH_INPUT_BY_X2,
                new TurnUiOperationArguments("navigation")));

        assertHandled(result, TurnLocalOperation.UI_CLOSE_MAP_SEARCH_INPUT_BY_X2, true);
        assertEquals(1, inputSequences.exclusiveCalls);
        assertEquals(1, inputSequences.callbackCalls);
        assertEquals(0, inputSequences.nestedExclusiveCalls);
        assertEquals("turn:ui-close-map-search-x2:navigation", inputSequences.lastDescription);
        assertEquals(1, uiCleanerService.x2Calls);
        assertEquals("navigation", uiCleanerService.lastSource);
    }

    @Test
    void x2QueueFailureReturnsTypedFalseWithoutInvokingService() throws Exception {
        inputSequences.completeRequest = false;

        LocalServiceExecution result = executor.execute(call(
                TurnLocalOperation.UI_CLOSE_MAP_SEARCH_INPUT_BY_X2,
                new TurnUiOperationArguments("navigation")));

        assertHandled(result, TurnLocalOperation.UI_CLOSE_MAP_SEARCH_INPUT_BY_X2, false);
        assertEquals(1, inputSequences.exclusiveCalls);
        assertEquals(0, inputSequences.callbackCalls);
        assertEquals(0, uiCleanerService.x2Calls);
    }

    @Test
    void invalidOrUnsupportedCallsFailClosedBeforeQueueAndService() {
        assertFailed(executor.execute(null), "INVALID_LOCAL_SERVICE_CALL");
        assertFailed(executor.execute(new TurnLocalServiceCall(null, null, null, null, null)),
                "INVALID_LOCAL_SERVICE_CALL");
        assertFailed(executor.execute(call(
                        TurnLocalOperation.UI_CLEAN_LIGHTWEIGHT,
                        new TurnUiOperationArguments(" "))),
                "INVALID_UI_ARGUMENTS");
        assertFailed(executor.execute(new TurnLocalServiceCall(
                        TurnLocalOperation.UI_CLEAN_ALL,
                        new TurnBagOperationArguments(null, null, null, null, null),
                        null, null, null)),
                "INVALID_UI_ARGUMENTS");
        assertFailed(executor.execute(call(TurnLocalOperation.BAG_USE_INCENSE, null)),
                "UNSUPPORTED_LOCAL_OPERATION");

        assertEquals(0, inputSequences.exclusiveCalls);
        assertEquals(0, uiCleanerService.totalCalls());
    }

    private void assertHandled(LocalServiceExecution result,
                               TurnLocalOperation operation,
                               boolean handled) throws Exception {
        assertEquals(TurnStepResult.Status.COMPLETED, result.status());
        assertEquals("OK", result.code());
        assertNull(result.frame());
        JsonNode json = objectMapper.readTree(result.localResultJson());
        assertEquals(operation.name(), json.get("operation").asText());
        assertEquals(handled, json.get("handled").asBoolean());
    }

    private static TurnLocalServiceCall call(TurnLocalOperation operation, TurnUiOperationArguments arguments) {
        return new TurnLocalServiceCall(operation, null, arguments, null, null);
    }

    private static void assertFailed(LocalServiceExecution result, String code) {
        assertEquals(TurnStepResult.Status.FAILED, result.status());
        assertEquals(code, result.code());
        assertNull(result.frame());
    }

    private static final class FakeUiCleanerService extends UICleanerService {
        private int cleanAllCalls;
        private int genericCalls;
        private int lightweightCalls;
        private int x2Calls;
        private boolean genericResult;
        private boolean lightweightResult;
        private boolean x2Result;
        private String lastSource;

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
            return genericResult;
        }

        @Override
        public boolean cleanLightweightInterruptions(String sourceTask) {
            lightweightCalls++;
            lastSource = sourceTask;
            return lightweightResult;
        }

        @Override
        public boolean closeMapSearchInputByX2Direct(String description) {
            x2Calls++;
            lastSource = description;
            return x2Result;
        }

        private int totalCalls() {
            return cleanAllCalls + genericCalls + lightweightCalls + x2Calls;
        }
    }

    private static final class RecordingInputSequences extends InputSequences {
        private int exclusiveCalls;
        private int callbackCalls;
        private int nestedExclusiveCalls;
        private int depth;
        private boolean completeRequest = true;
        private String lastDescription;

        private RecordingInputSequences() {
            super(null);
        }

        @Override
        public boolean submitExclusiveAndWait(String description, Supplier<Boolean> callback) {
            exclusiveCalls++;
            lastDescription = description;
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
}
