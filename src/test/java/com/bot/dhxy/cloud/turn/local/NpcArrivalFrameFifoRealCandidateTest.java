package com.bot.dhxy.cloud.turn.local;

import com.bot.dhxy.cloud.task.NpcClickSmartCloudSession;
import com.bot.dhxy.cloud.task.NpcClickSmartQueueMessage;
import com.bot.dhxy.cloud.task.NpcClickSmartQueueOutcome;
import com.bot.dhxy.cloud.turn.TurnClient;
import com.bot.dhxy.cloud.turn.TurnExchangeResult;
import com.bot.dhxy.cloud.turn.TurnTemplateDownload;
import com.bot.dhxy.cloud.turn.protocol.TurnNpcArrivalFrameFifoSpec;
import com.bot.dhxy.cloud.turn.protocol.TurnRequest;
import com.bot.dhxy.cloud.turn.protocol.TurnWholeTaskRuntimeArguments;
import com.bot.dhxy.config.WindowIsolationProperties;
import com.bot.dhxy.core.GameContext;
import com.bot.dhxy.core.GameClientTracker;
import com.bot.dhxy.input.InputSequences;
import com.bot.dhxy.input.action.InputAction;
import com.bot.dhxy.input.action.InputActionQueue;
import com.bot.dhxy.input.action.InputActionType;
import com.bot.dhxy.runner.context.TaskExecutionContextHolder;
import com.bot.dhxy.service.DialogService;
import com.bot.dhxy.tools.CoordinateHelper;
import com.bot.dhxy.window.model.WindowNativeBinding;
import com.bot.dhxy.window.runtime.WindowRuntimeContext;
import com.bot.dhxy.window.runtime.WindowTaskContextHolder;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.awt.Point;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NpcArrivalFrameFifoRealCandidateTest {

    private static final String WINDOW = "hwnd-F42196";
    private static final String HWND_DECIMAL = "15999382";
    private static final String BUSINESS_RUN = "remote-turn-real-frame:0:XIULUO_V2";
    private static final String SESSION = "npc-arrival-real-frame";

    @Test
    void strictCloudTooltipJsonReachesAtomicLocalInputSequence() throws Exception {
        ObjectMapper mapper = new ObjectMapper()
                .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
                .disable(DeserializationFeature.ACCEPT_FLOAT_AS_INT)
                .disable(MapperFeature.ALLOW_COERCION_OF_SCALARS);
        String tooltipJson = cloudTooltipJson();
        String pipelineInput = System.getProperty("npc.pipeline.input");
        if (pipelineInput != null && !pipelineInput.isBlank()) {
            List<String> wireMessages = Files.readAllLines(Path.of(pipelineInput));
            List<NpcClickSmartQueueMessage> parsedMessages = new ArrayList<>();
            for (String wireMessage : wireMessages) {
                parsedMessages.add(mapper.readValue(wireMessage, NpcClickSmartQueueMessage.class));
            }
            List<NpcClickSmartQueueMessage.Type> types = parsedMessages.stream()
                    .map(NpcClickSmartQueueMessage::getType)
                    .toList();
            assertEquals(List.of(
                    NpcClickSmartQueueMessage.Type.MEMORY,
                    NpcClickSmartQueueMessage.Type.TOOLTIP,
                    NpcClickSmartQueueMessage.Type.YELLOW_NAME,
                    NpcClickSmartQueueMessage.Type.PURPLE_FORMULA,
                    NpcClickSmartQueueMessage.Type.CTRL_CANDIDATES,
                    NpcClickSmartQueueMessage.Type.END), types);
            tooltipJson = wireMessages.get(types.indexOf(NpcClickSmartQueueMessage.Type.TOOLTIP));
        }
        NpcClickSmartQueueMessage tooltip =
                mapper.readValue(tooltipJson, NpcClickSmartQueueMessage.class);
        StubTurnClient turnClient = new StubTurnClient(tooltip);
        RecordingInputSequences input = new RecordingInputSequences();
        WindowIsolationProperties isolation = new WindowIsolationProperties();
        WindowTaskContextHolder windowHolder = new WindowTaskContextHolder(isolation);
        WindowRuntimeContext runtime = new WindowRuntimeContext(WINDOW, new GameContext());
        runtime.setNativeBinding(new WindowNativeBinding(
                "hwnd-F42196", "game", "DHXYJYMainFrame", 1L,
                167, 45, 1036, 783));
        windowHolder.bind(runtime);

        DialogService verifier = new DialogService(
                new GameClientTracker(null, null, null, null, null, null, null, null, null, null, null, null),
                new CoordinateHelper(null, null));
        NpcArrivalFrameFifoLocalExecutor executor = new NpcArrivalFrameFifoLocalExecutor(
                turnClient,
                windowHolder,
                new TaskExecutionContextHolder(),
                null,
                input,
                null,
                null,
                verifier,
                null);

        assertTrue(executor.execute(arguments()));
        assertEquals(1, input.submissions.size());
        List<InputAction> actions = input.submissions.get(0);
        assertEquals(List.of(
                        InputActionType.MOVE_MOUSE,
                        InputActionType.SLEEP,
                        InputActionType.CLICK_LEFT,
                        InputActionType.SLEEP),
                actions.stream().map(InputAction::getType).toList());
        assertEquals(557, actions.get(0).getX());
        assertEquals(245, actions.get(0).getY());
        assertEquals(557, actions.get(2).getX());
        assertEquals(245, actions.get(2).getY());
        assertTrue(turnClient.reported.await(2, TimeUnit.SECONDS));
        assertEquals(NpcClickSmartQueueOutcome.VERIFIED, turnClient.reportedOutcome);
    }

    private static String cloudTooltipJson() {
        return """
                {
                  "type":"TOOLTIP",
                  "sessionId":"npc-arrival-real-frame",
                  "windowId":"hwnd-F42196",
                  "taskRunId":"remote-turn-real-frame:0:XIULUO_V2",
                  "decisionId":"cloud-npc-real-frame",
                  "strategy":"TOOLTIP_TEMPLATE",
                  "windowRelativeClickPoint":{"x":390,"y":200},
                  "candidateBox":"374,192,32,16",
                  "matchedText":null,
                  "ctrlProbePoints":[],
                  "reason":"cloud-brain-npc-tooltip-template-click",
                  "confidence":0.9996389746665955
                }
                """;
    }

    private static TurnWholeTaskRuntimeArguments arguments() {
        TurnNpcArrivalFrameFifoSpec spec = new TurnNpcArrivalFrameFifoSpec(
                "dhxy-local", "dhxy-client", WINDOW, HWND_DECIMAL,
                "remote-turn-real-frame", BUSINESS_RUN,
                0, 0, 1024, 768, List.of(), null, true, false);
        return new TurnWholeTaskRuntimeArguments(
                "test", null, "npc-real-frame", null, null,
                null, null, null, null, null, null, null, null,
                "XIULUO_V2", "灵兽村使者", null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, spec);
    }

    private static final class RecordingInputSequences extends InputSequences {
        private final List<List<InputAction>> submissions = new ArrayList<>();

        private RecordingInputSequences() {
            super((InputActionQueue) null);
        }

        @Override
        public boolean submitAndWait(String description, List<InputAction> actions) {
            submissions.add(List.copyOf(actions));
            return true;
        }
    }

    private static final class StubTurnClient implements TurnClient {
        private final Queue<NpcClickSmartQueueMessage> messages = new ArrayDeque<>();
        private final CountDownLatch reported = new CountDownLatch(1);
        private final String sessionId;
        private volatile NpcClickSmartQueueOutcome reportedOutcome;

        private StubTurnClient(NpcClickSmartQueueMessage tooltip) {
            sessionId = tooltip.getSessionId();
            messages.add(NpcClickSmartQueueMessage.builder()
                    .type(NpcClickSmartQueueMessage.Type.MEMORY)
                    .sessionId(sessionId)
                    .windowId(WINDOW)
                    .taskRunId(BUSINESS_RUN)
                    .decisionId("memory-miss")
                    .strategy("MEMORY")
                    .build());
            messages.add(tooltip);
        }

        @Override
        public NpcClickSmartCloudSession openNpcArrivalFrame(
                String tenantId, String deviceId, String windowId, String hwnd,
                String observationRunId, String businessTaskRunId, String intentId) {
            return NpcClickSmartCloudSession.builder()
                    .status(NpcClickSmartCloudSession.Status.STARTED)
                    .sessionId(sessionId)
                    .windowId(WINDOW)
                    .taskRunId(BUSINESS_RUN)
                    .build();
        }

        @Override
        public NpcClickSmartQueueMessage pollNpcArrivalFrame(
                String tenantId, String deviceId, String windowId, String hwnd,
                String observationRunId, String businessTaskRunId, String intentId) {
            return messages.remove();
        }

        @Override
        public void reportNpcArrivalFrameOutcome(
                String tenantId, String deviceId, String windowId, String hwnd,
                String observationRunId, String businessTaskRunId, String intentId,
                NpcClickSmartQueueMessage message,
                NpcClickSmartQueueOutcome outcome,
                String reason) {
            reportedOutcome = outcome;
            reported.countDown();
        }

        @Override
        public TurnExchangeResult exchange(TurnRequest request, byte[] optionalPng) {
            throw new AssertionError("not used");
        }

        @Override
        public TurnTemplateDownload downloadTemplate(String templateKey, String ifNoneMatch) {
            throw new AssertionError("not used");
        }
    }
}
