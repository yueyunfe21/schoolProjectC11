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
import com.bot.dhxy.core.GameClientTracker;
import com.bot.dhxy.core.GameContext;
import com.bot.dhxy.input.InputSequences;
import com.bot.dhxy.input.action.InputAction;
import com.bot.dhxy.input.action.InputActionQueue;
import com.bot.dhxy.runner.context.TaskExecutionContextHolder;
import com.bot.dhxy.service.DialogService;
import com.bot.dhxy.service.UICleanerService;
import com.bot.dhxy.tools.CoordinateHelper;
import com.bot.dhxy.window.model.WindowNativeBinding;
import com.bot.dhxy.window.runtime.WindowRuntimeContext;
import com.bot.dhxy.window.runtime.WindowTaskContextHolder;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * G122 复审返修 P2-2：客户端消费 {@code Type.CONTINUATION} 的行为合同。此前只有 Cloud 侧的
 * 消息塑形合同（{@code G122ContinuationQueueContractTest}）——把客户端 FIFO 的 CONTINUATION
 * 分支整个删掉，云端 6/6 照样全绿。本类用真实 executor + 真实 defer 验证链钉住三件事：
 *
 * <ol>
 *   <li>CONTINUATION 不烧候选槽：连续 {@code CANDIDATE_LIMIT}（12）条 CONTINUATION 之后的
 *       真实候选仍然被消费执行——若 CONTINUATION 计入候选预算，第 13 条消息永远轮不到；</li>
 *   <li>CONTINUATION 之后继续消费：它答复 SKIPPED、不判 miss、不终止会话；</li>
 *   <li>会话 END 后走既有 {@code EXHAUSTED -> replaceWithFreshFrame -> attempt 2} 闭环，
 *       换帧恰好一次、第二次会话 END 即有界结束（不出现第三次开会话）。</li>
 * </ol>
 */
class G122ContinuationClientConsumptionContractTest {

    private static final String WINDOW = "hwnd-F42196";
    private static final String HWND_DECIMAL = "15999382";
    private static final String BUSINESS_RUN = "remote-turn-real-frame:0:DALISI_QUIZ";

    /** 合同①+②：12 条 CONTINUATION 烧不掉任何候选槽，其后的真实候选照常点击并按 defer 收工。 */
    @Test
    void twelveContinuationsDoNotBurnCandidateSlotsAndTheRealCandidateStillExecutes()
            throws Exception {
        Queue<NpcClickSmartQueueMessage> queue = new ArrayDeque<>();
        for (int i = 0; i < 12; i++) {
            queue.add(continuation("s1", "g122-cont-" + i));
        }
        queue.add(fixedPoint("s1", "g122-real-candidate"));
        StubTurnClient turnClient = new StubTurnClient(List.of(queue));
        RecordingInput input = new RecordingInput();
        NpcArrivalFrameFifoLocalExecutor executor = executor(
                turnClient, input, null, null);

        assertTrue(executor.execute(deferArguments("g122-cont-slots")),
                "12 条 CONTINUATION 之后的真实候选必须仍被消费——CONTINUATION 不占候选预算");

        assertEquals(1, input.submissions.size(),
                "真实候选恰好点击一次（CONTINUATION 自己绝不产生输入）");
        turnClient.awaitReports(13);
        assertEquals(12, turnClient.outcomesOf(NpcClickSmartQueueMessage.Type.CONTINUATION).size());
        assertTrue(turnClient.outcomesOf(NpcClickSmartQueueMessage.Type.CONTINUATION).stream()
                        .allMatch(outcome -> outcome == NpcClickSmartQueueOutcome.SKIPPED),
                "每条 CONTINUATION 都以 SKIPPED 答复：不是 miss，也不是终态");
        assertEquals(List.of(NpcClickSmartQueueOutcome.DIALOG_OPEN_UNVERIFIED),
                turnClient.outcomesOf(NpcClickSmartQueueMessage.Type.FIXED_POINT),
                "defer 候选按复审正名如实上报待定态");
    }

    /** 合同③：CONTINUATION 后会话 END → 换帧恰好一次 → 第二次会话 END 即有界结束。 */
    @Test
    void continuationSessionEndReplacesFreshFrameOnceAndTerminatesBounded() throws Exception {
        Queue<NpcClickSmartQueueMessage> first = new ArrayDeque<>(List.of(
                continuation("s1", "g122-cont-end"),
                end("s1")));
        Queue<NpcClickSmartQueueMessage> second = new ArrayDeque<>(List.of(end("s2")));
        StubTurnClient turnClient = new StubTurnClient(List.of(first, second));
        RecordingInput input = new RecordingInput();
        AtomicInteger cleanups = new AtomicInteger();
        UICleanerService cleaner = new UICleanerService(null, null, null, null, null, null) {
            @Override
            public void cleanUpAll() {
                cleanups.incrementAndGet();
            }
        };
        NpcArrivalFrameFifoLocalExecutor executor = executor(
                turnClient, input, new FrameTracker(), cleaner);

        assertFalse(executor.execute(deferArguments("g122-cont-bounded")),
                "第二次会话仍 END 时必须有界失败，不得无限续帧");

        assertEquals(2, turnClient.opens.get(),
                "恰好两次会话：首帧 + 一次 fresh-frame replacement，绝无第三次");
        assertEquals(1, turnClient.replacements.get(),
                "EXHAUSTED 后 replaceWithFreshFrame 恰好一次（attempt 2 的同族续帧闭环）");
        assertEquals(1, cleanups.get(), "首次 END 耗尽才做一次清障");
        assertEquals(0, input.submissions.size(), "全程没有可点击候选，绝不产生点击输入");
        // 三条上报：CONTINUATION 的 SKIPPED + 两次会话各一条 END 终态。
        turnClient.awaitReports(3);
        assertEquals(List.of(NpcClickSmartQueueOutcome.SKIPPED),
                turnClient.outcomesOf(NpcClickSmartQueueMessage.Type.CONTINUATION),
                "CONTINUATION 不因随后的 END 被追改成 miss/终态");
        assertEquals(List.of(NpcClickSmartQueueOutcome.FINAL_FAILED,
                        NpcClickSmartQueueOutcome.FINAL_FAILED),
                turnClient.outcomesOf(NpcClickSmartQueueMessage.Type.END),
                "两次会话的 END 都如实报 FINAL_FAILED——没有第三次");
    }

    // ---------------------------------------------------------------------------------- 工具

    private static NpcArrivalFrameFifoLocalExecutor executor(
            StubTurnClient turnClient,
            RecordingInput input,
            GameClientTracker tracker,
            UICleanerService cleaner) {
        WindowTaskContextHolder windowHolder =
                new WindowTaskContextHolder(new WindowIsolationProperties());
        WindowRuntimeContext runtime = new WindowRuntimeContext(WINDOW, new GameContext());
        runtime.setNativeBinding(new WindowNativeBinding(
                "hwnd-F42196", "game", "DHXYJYMainFrame", 1L, 167, 45, 1036, 783));
        windowHolder.bind(runtime);
        // defer 链一眼都不看屏幕（G102 合同钉死），tracker 只为满足构造器非空约束。
        DialogService verifier = new DialogService(new FrameTracker(), new CoordinateHelper(null, null));
        return new NpcArrivalFrameFifoLocalExecutor(
                turnClient, windowHolder, new TaskExecutionContextHolder(), tracker,
                input, null, cleaner, verifier, null);
    }

    private static NpcClickSmartQueueMessage continuation(String sessionId, String decisionId) {
        return NpcClickSmartQueueMessage.builder()
                .type(NpcClickSmartQueueMessage.Type.CONTINUATION)
                .sessionId(sessionId)
                .windowId(WINDOW)
                .taskRunId(BUSINESS_RUN)
                .decisionId(decisionId)
                .strategy("YELLOW_TARGET_RAW")
                .matchedText("REQUEST_NEW_SCREENSHOT")
                .candidateBox("12,34,56,78")
                .reason("cloud-brain-npc-raw-yellow-target-ambiguous")
                .build();
    }

    private static NpcClickSmartQueueMessage fixedPoint(String sessionId, String decisionId) {
        return NpcClickSmartQueueMessage.builder()
                .type(NpcClickSmartQueueMessage.Type.FIXED_POINT)
                .sessionId(sessionId)
                .windowId(WINDOW)
                .taskRunId(BUSINESS_RUN)
                .decisionId(decisionId)
                .strategy("TARGET_METADATA")
                .windowRelativeClickPoint(new Point(481, 619))
                .build();
    }

    private static NpcClickSmartQueueMessage end(String sessionId) {
        return NpcClickSmartQueueMessage.builder()
                .type(NpcClickSmartQueueMessage.Type.END)
                .sessionId(sessionId)
                .windowId(WINDOW)
                .taskRunId(BUSINESS_RUN)
                .decisionId("g122-end-" + sessionId)
                .strategy("END")
                .build();
    }

    private static TurnWholeTaskRuntimeArguments deferArguments(String intentId) {
        TurnNpcArrivalFrameFifoSpec spec = new TurnNpcArrivalFrameFifoSpec(
                "dhxy-local", "dhxy-client", WINDOW, HWND_DECIMAL,
                "remote-turn-real-frame", BUSINESS_RUN,
                0, 0, 1024, 768, List.of(), null, true, false, false);
        return new TurnWholeTaskRuntimeArguments(
                "test", null, intentId, null, null,
                null, null, null, null, null, null, null, null,
                "DALISI_QUIZ", "大理寺官员", null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, spec);
    }

    private static final class RecordingInput extends InputSequences {
        private final List<List<InputAction>> submissions = new CopyOnWriteArrayList<>();

        private RecordingInput() {
            super((InputActionQueue) null);
        }

        @Override
        public boolean submitAndWait(String description, List<InputAction> actions) {
            submissions.add(List.copyOf(actions));
            return true;
        }
    }

    /** 每次 openNpcArrivalFrame 递进一个会话；replace 记数；上报按消息类型归档。 */
    private static final class StubTurnClient implements TurnClient {
        private final List<Queue<NpcClickSmartQueueMessage>> sessions;
        private final AtomicInteger opens = new AtomicInteger();
        private final AtomicInteger replacements = new AtomicInteger();
        private final List<NpcClickSmartQueueMessage> reportedMessages = new CopyOnWriteArrayList<>();
        private final List<NpcClickSmartQueueOutcome> reportedOutcomes = new CopyOnWriteArrayList<>();

        private StubTurnClient(List<Queue<NpcClickSmartQueueMessage>> sessions) {
            this.sessions = sessions;
        }

        @Override
        public NpcClickSmartCloudSession openNpcArrivalFrame(
                String tenantId, String deviceId, String windowId, String hwnd,
                String observationRunId, String businessTaskRunId, String intentId) {
            int index = opens.incrementAndGet();
            return NpcClickSmartCloudSession.builder()
                    .status(NpcClickSmartCloudSession.Status.STARTED)
                    .sessionId("s" + index)
                    .windowId(WINDOW)
                    .taskRunId(BUSINESS_RUN)
                    .build();
        }

        @Override
        public NpcClickSmartQueueMessage pollNpcArrivalFrame(
                String tenantId, String deviceId, String windowId, String hwnd,
                String observationRunId, String businessTaskRunId, String intentId) {
            return sessions.get(opens.get() - 1).remove();
        }

        @Override
        public void replaceNpcArrivalFrame(
                String tenantId, String deviceId, String windowId, String hwnd,
                String observationRunId, String businessTaskRunId, String intentId,
                long frameId, long generation, long capturedAtMs, byte[] pngBytes) {
            replacements.incrementAndGet();
        }

        @Override
        public void reportNpcArrivalFrameOutcome(
                String tenantId, String deviceId, String windowId, String hwnd,
                String observationRunId, String businessTaskRunId, String intentId,
                NpcClickSmartQueueMessage message,
                NpcClickSmartQueueOutcome outcome,
                String reason) {
            reportedMessages.add(message);
            reportedOutcomes.add(outcome);
        }

        private List<NpcClickSmartQueueOutcome> outcomesOf(NpcClickSmartQueueMessage.Type type) {
            List<NpcClickSmartQueueOutcome> matched = new ArrayList<>();
            for (int i = 0; i < reportedMessages.size(); i++) {
                NpcClickSmartQueueMessage message = reportedMessages.get(i);
                if (message != null && message.getType() == type) {
                    matched.add(reportedOutcomes.get(i));
                }
            }
            return matched;
        }

        /** 上报是异步的（CompletableFuture.runAsync）——给一个有界窗口等它们全部落账。 */
        private void awaitReports(int expected) throws InterruptedException {
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
            while (reportedOutcomes.size() < expected && System.nanoTime() < deadline) {
                Thread.sleep(20);
            }
            assertEquals(expected, reportedOutcomes.size(), "全部结果必须都被上报");
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

    /** replaceWithFreshFrame 需要一张真实可编码的整窗帧。 */
    private static final class FrameTracker extends GameClientTracker {
        private FrameTracker() {
            super(null, null, null, null, null, null, null, null, null, null, null);
        }

        @Override
        public BufferedImage captureToMemory(String elementName, int x1, int y1, int x2, int y2) {
            BufferedImage frame = new BufferedImage(
                    Math.max(1, x2 - x1), Math.max(1, y2 - y1), BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = frame.createGraphics();
            try {
                graphics.setColor(new Color(40, 42, 46));
                graphics.fillRect(0, 0, frame.getWidth(), frame.getHeight());
            } finally {
                graphics.dispose();
            }
            return frame;
        }
    }
}
