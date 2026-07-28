package com.bot.dhxy.cloud.turn.local;

import com.bot.dhxy.cloud.turn.LocalServiceExecution;
import com.bot.dhxy.cloud.turn.TurnPngCodec;
import com.bot.dhxy.cloud.turn.protocol.TurnBagOperationArguments;
import com.bot.dhxy.cloud.turn.protocol.TurnFramePurpose;
import com.bot.dhxy.cloud.turn.protocol.TurnLocalOperation;
import com.bot.dhxy.cloud.turn.protocol.TurnLocalServiceCall;
import com.bot.dhxy.cloud.turn.protocol.TurnQuestOperationArguments;
import com.bot.dhxy.cloud.turn.protocol.TurnStepResult;
import com.bot.dhxy.model.quest.QuestDetailCapture;
import com.bot.dhxy.service.QuestManagerService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class QuestLocalOperationExecutorContractTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private FakeQuestManagerService questManagerService;
    private QuestLocalOperationExecutor executor;

    @BeforeEach
    void setUp() {
        questManagerService = new FakeQuestManagerService();
        executor = new QuestLocalOperationExecutor(questManagerService, new TurnPngCodec(), objectMapper);
    }

    @Test
    void activateMapsTypedArgumentsAndResultWithOneServiceCall() throws Exception {
        questManagerService.activateResult = true;

        LocalServiceExecution result = executor.execute(call(
                TurnLocalOperation.QUEST_ACTIVATE,
                new TurnQuestOperationArguments("xiuluo", true)), 6);

        assertCompleted(result);
        assertNull(result.frame());
        assertEquals(1, questManagerService.activateCalls);
        assertEquals("xiuluo", questManagerService.lastTask);
        assertEquals(true, questManagerService.lastKeepOpen);
        JsonNode json = objectMapper.readTree(result.localResultJson());
        assertEquals(true, json.get("activated").asBoolean());
    }

    @Test
    void detailReturnsExactSameCallPixelsAndAbsoluteOrigin() throws Exception {
        BufferedImage source = new BufferedImage(2, 2, BufferedImage.TYPE_INT_ARGB);
        source.setRGB(0, 0, 0xff102030);
        source.setRGB(1, 0, 0xff405060);
        source.setRGB(0, 1, 0xff708090);
        source.setRGB(1, 1, 0xffa0b0c0);
        questManagerService.capture = new QuestDetailCapture(source, "ignored.png", 345, 678);

        LocalServiceExecution result = executor.execute(call(
                TurnLocalOperation.QUEST_CAPTURE_DETAIL,
                new TurnQuestOperationArguments("wuhuan", null)), 9);

        assertCompleted(result);
        assertEquals(1, questManagerService.captureCalls);
        assertEquals("wuhuan", questManagerService.lastTask);
        assertNotNull(result.frame());
        assertEquals(TurnFramePurpose.QUEST_DETAIL, result.frame().metadata().purpose());
        assertEquals(345, result.frame().metadata().region().x());
        assertEquals(678, result.frame().metadata().region().y());
        assertEquals(2, result.frame().metadata().region().width());
        assertEquals(2, result.frame().metadata().region().height());
        assertEquals(9, result.frame().metadata().sourceStepIndex());
        BufferedImage decoded = ImageIO.read(new ByteArrayInputStream(result.frame().pngBytes()));
        assertNotNull(decoded);
        try {
            assertEquals(0xff102030, decoded.getRGB(0, 0));
            assertEquals(0xffa0b0c0, decoded.getRGB(1, 1));
        } finally {
            decoded.flush();
        }
        assertEquals(true, objectMapper.readTree(result.localResultJson()).get("captured").asBoolean());
    }

    @Test
    void failedDetailHasNoFrameAndDoesNotCaptureAgain() throws Exception {
        questManagerService.capture = QuestDetailCapture.empty();

        LocalServiceExecution result = executor.execute(call(
                TurnLocalOperation.QUEST_CAPTURE_DETAIL,
                new TurnQuestOperationArguments("wubei", null)), 4);

        assertEquals(TurnStepResult.Status.FAILED, result.status());
        assertEquals("QUEST_DETAIL_CAPTURE_FAILED", result.code());
        assertNull(result.frame());
        assertEquals(1, questManagerService.captureCalls);
        assertEquals(false, objectMapper.readTree(result.localResultJson()).get("captured").asBoolean());
    }

    @Test
    void invalidOrUnsupportedCallsFailClosedBeforeQuestService() {
        assertFailed(executor.execute(null, 0), "INVALID_LOCAL_SERVICE_CALL");
        assertFailed(executor.execute(new TurnLocalServiceCall(null, null, null, null, null), 0),
                "INVALID_LOCAL_SERVICE_CALL");
        assertFailed(executor.execute(call(
                        TurnLocalOperation.QUEST_ACTIVATE,
                        new TurnQuestOperationArguments("task", true)), -1),
                "INVALID_SOURCE_STEP_INDEX");
        assertFailed(executor.execute(call(
                        TurnLocalOperation.QUEST_ACTIVATE,
                        new TurnQuestOperationArguments(" ", true)), 0),
                "INVALID_QUEST_ARGUMENTS");
        assertFailed(executor.execute(new TurnLocalServiceCall(
                        TurnLocalOperation.QUEST_CAPTURE_DETAIL,
                        new TurnBagOperationArguments(null, null, null, null, null),
                        null, null, new TurnQuestOperationArguments("task", null)), 0),
                "INVALID_QUEST_ARGUMENTS");
        assertFailed(executor.execute(call(TurnLocalOperation.UI_CLEAN_ALL, null), 0),
                "UNSUPPORTED_LOCAL_OPERATION");

        assertEquals(0, questManagerService.activateCalls);
        assertEquals(0, questManagerService.captureCalls);
    }

    private static TurnLocalServiceCall call(TurnLocalOperation operation,
                                              TurnQuestOperationArguments arguments) {
        return new TurnLocalServiceCall(operation, null, null, null, arguments);
    }

    private static void assertCompleted(LocalServiceExecution result) {
        assertEquals(TurnStepResult.Status.COMPLETED, result.status());
        assertEquals("OK", result.code());
    }

    private static void assertFailed(LocalServiceExecution result, String code) {
        assertEquals(TurnStepResult.Status.FAILED, result.status());
        assertEquals(code, result.code());
        assertNull(result.frame());
    }

    private static final class FakeQuestManagerService extends QuestManagerService {
        private int activateCalls;
        private int captureCalls;
        private String lastTask;
        private Boolean lastKeepOpen;
        private boolean activateResult;
        private QuestDetailCapture capture;

        private FakeQuestManagerService() {
            super(null, null, null, null, null, null, null, null, null);
        }

        @Override
        public boolean activateTaskIfPresent(String task, boolean keepOpen) {
            activateCalls++;
            lastTask = task;
            lastKeepOpen = keepOpen;
            return activateResult;
        }

        @Override
        public QuestDetailCapture captureCurrentQuestDetailForTask(String task) {
            captureCalls++;
            lastTask = task;
            return capture;
        }
    }
}
