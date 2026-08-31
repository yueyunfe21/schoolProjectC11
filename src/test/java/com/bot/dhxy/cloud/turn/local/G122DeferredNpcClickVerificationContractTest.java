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
import com.bot.dhxy.model.dialog.DialogType;
import com.bot.dhxy.runner.context.TaskExecutionContextHolder;
import com.bot.dhxy.service.DialogService;
import com.bot.dhxy.tools.CoordinateHelper;
import com.bot.dhxy.window.model.WindowNativeBinding;
import com.bot.dhxy.window.runtime.WindowRuntimeContext;
import com.bot.dhxy.window.runtime.WindowTaskContextHolder;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * G122 P1-3：{@code DEFERRED_TO_TASK} 必须是**非成功的独立状态**。
 *
 * <p>事故（2026-08-29 大理寺答题，五开）：候选点 (481,619) 点空，屏幕上只有启动前遗留的旧
 * Dialog，{@code accept.png} 从头到尾没出现过——日志却报 {@code VERIFIED}，而且那个空坐标被
 * 存成"已验证点击点"污染了 retained-point replay。根因是
 * {@code DialogService.verifyNpcArrivalExpectedDialog(..., deferToTask=true, ...)} 硬编码返回
 * {@code new NpcClickVerification(true, ...)}：不拍图、不判 Dialog presence、不匹配模板，
 * 直接借用成功态。</p>
 *
 * <p>验收判据（用户指定，两条是两件事，分别钉住）：旧 Dialog 存在、NPC 点空、accept.png 未出现时
 * ①结果绝不能是 {@code VERIFIED}；②绝不能把该点击点保存为"已验证点击点"。</p>
 */
class G122DeferredNpcClickVerificationContractTest {

    private static final String WINDOW = "hwnd-F42196";
    private static final String HWND_DECIMAL = "15999382";
    private static final String BUSINESS_RUN = "remote-turn-real-frame:0:DALISI_QUIZ";
    private static final String SESSION = "g122-deferred-session";

    private static final Path EXECUTOR_SOURCE = Path.of(
            "src/main/java/com/bot/dhxy/cloud/turn/local/NpcArrivalFrameFifoLocalExecutor.java");
    private static final Path DIALOG_SERVICE_SOURCE = Path.of(
            "src/main/java/com/bot/dhxy/service/DialogService.java");

    // ---------------------------------------------------------------- 判据 ①：绝不能是 VERIFIED

    /**
     * 事故现场的完整重放：屏幕上确实有一个旧 Dialog（tracker 随时能交出一张真实框图），
     * defer=true。DEFERRED 终态必须既不是 verified 也不是 optionDialogVisible，
     * 并且**一次图都没拍**——它凭什么说"验证过了"。
     */
    @Test
    void deferredVerificationIsNotSuccessAndNeverLooksAtTheScreen() {
        AtomicInteger captureCount = new AtomicInteger();
        DialogService service = new DialogService(
                new StaleDialogTracker(captureCount),
                new CoordinateHelper(null, null) {
                    @Override
                    public int[] getScaledRect(int x, int y, int width, int height) {
                        return new int[]{x, y, x + width, y + height};
                    }
                });

        DialogService.NpcClickVerification verdict = service.verifyNpcArrivalExpectedDialog(
                List.of(), null, true, "g122:dalisi:empty-click-on-stale-dialog");

        assertFalse(verdict.verified(),
                "DEFERRED 是待定，不是成功；verified=true 就是 G122 P1-3 的硬编码假成功");
        assertFalse(verdict.optionDialogVisible(),
                "defer 分支一眼都没看过屏幕，不许声称看见了框（禁止为了编译过随便翻一个布尔）");
        assertTrue(verdict.deferredToTask(), "第三态必须有自己的表达，而不是借用别人的字段");
        assertEquals("DEFERRED_TO_TASK", verdict.status());
        assertEquals(DialogType.NONE, verdict.dialogType());
        assertEquals(0, captureCount.get(),
                "defer 不拍图是既有语义（G102：defer 任务不许被踹回旧截图方差判断），必须保持");
    }

    /** 判据①的下半段：这个终态映射到队列终态时，绝不能落成 VERIFIED。 */
    @Test
    void deferredVerificationNeverMapsToTheVerifiedQueueOutcome() {
        NpcClickSmartQueueOutcome outcome = NpcArrivalFrameFifoLocalExecutor
                .queueOutcomeForVerification(DialogService.NpcClickVerification.deferredPending());

        assertNotEquals(NpcClickSmartQueueOutcome.VERIFIED, outcome,
                "旧 Dialog 存在、NPC 点空、accept.png 未出现——结果绝不能是 VERIFIED");
        assertEquals(NpcClickSmartQueueOutcome.DIALOG_OPEN_UNVERIFIED, outcome,
                "defer 只能落在'未验证、交给任务侧'这一态");
    }

    // ------------------------------------------------------- 判据 ②：绝不能保存为已验证点击点

    /** 判据②：同一次 defer 终态，不得被保留成 retained replay 点。 */
    @Test
    void deferredVerificationNeverRetainsTheClickPoint() {
        NpcClickSmartQueueOutcome outcome = NpcArrivalFrameFifoLocalExecutor
                .queueOutcomeForVerification(DialogService.NpcClickVerification.deferredPending());

        assertFalse(NpcArrivalFrameFifoLocalExecutor.shouldRetainVerifiedPoint(outcome),
                "没验证过的点绝不能存成'已验证点击点'——(481,619) 就是这样污染 replay 的");
        assertFalse(NpcArrivalFrameFifoLocalExecutor.shouldRetainVerifiedPoint(
                        NpcClickSmartQueueOutcome.DIALOG_OPEN_UNVERIFIED));
        assertFalse(NpcArrivalFrameFifoLocalExecutor.shouldRetainVerifiedPoint(
                        NpcClickSmartQueueOutcome.VERIFICATION_FAILED));
        assertTrue(NpcArrivalFrameFifoLocalExecutor.shouldRetainVerifiedPoint(
                        NpcClickSmartQueueOutcome.VERIFIED),
                "真验证过的点仍然要留，否则 reuseLastVerifiedPoint 整条路就废了");
    }

    /**
     * 判据②的端到端实证：跑一次真实的 defer FIFO 会话（真 executor、真 DialogService、真映射），
     * 事后 {@code verifiedReplayPoints} 必须是空的——那个点绝不允许被记成"已验证点击点"。
     * 它只能落进显式标注未验证的 {@code deferredReplayPoints}（五环显式重放专用）。
     */
    @Test
    void aRealDeferredSessionLeavesTheVerifiedPointStoreEmpty() throws Exception {
        RecordingInput input = new RecordingInput();
        WindowTaskContextHolder windowHolder =
                new WindowTaskContextHolder(new WindowIsolationProperties());
        WindowRuntimeContext runtime = new WindowRuntimeContext(WINDOW, new GameContext());
        runtime.setNativeBinding(new WindowNativeBinding(
                "hwnd-F42196", "game", "DHXYJYMainFrame", 1L, 167, 45, 1036, 783));
        windowHolder.bind(runtime);
        AtomicInteger captureCount = new AtomicInteger();
        DialogService verifier = new DialogService(
                new StaleDialogTracker(captureCount), new CoordinateHelper(null, null));
        StubTurnClient turnClient = new StubTurnClient();
        NpcArrivalFrameFifoLocalExecutor executor = new NpcArrivalFrameFifoLocalExecutor(
                turnClient, windowHolder, new TaskExecutionContextHolder(), null,
                input, null, null, verifier, null);

        assertTrue(executor.execute(deferArguments(false)),
                "defer 会话仍然把这一下交给任务侧收工，语义不变（五环/鬼王/天庭依赖它）");

        assertTrue(replayStore(executor, "verifiedReplayPoints").isEmpty(),
                "NPC 点空、accept.png 未出现——这个点绝不能被保存为已验证点击点");
        assertEquals(1, replayStore(executor, "deferredReplayPoints").size(),
                "它只能进显式标注未验证的待定表");
        turnClient.awaitReport();
        assertEquals(NpcClickSmartQueueOutcome.DIALOG_OPEN_UNVERIFIED, turnClient.reportedOutcome,
                "复审 P1 正名：defer 待定态如实上报 DIALOG_OPEN_UNVERIFIED，不得再伪装成 VERIFIED"
                        + "（云端 enrichArrivalOutcome/registerDeferredPending 已同单认可如实编码）");
        assertEquals(0, captureCount.get(), "defer 依旧不拍图（G102 语义不变）");
    }

    /** 判据②的接线：生产里两个 rememberVerifiedPoint 调用点都必须走这道闸，不能各写各的。 */
    @Test
    void bothProductionRetentionSitesGoThroughTheSameGate() throws Exception {
        String source = Files.readString(EXECUTOR_SOURCE, StandardCharsets.UTF_8);

        assertEquals(2, occurrences(source, "if (shouldRetainVerifiedPoint("),
                "普通候选与 Ctrl 候选两个保留点必须都走 shouldRetainVerifiedPoint");
        assertEquals(2, occurrences(source, "rememberVerifiedPoint(arguments, spec,"));
        assertFalse(source.contains("if (outcome == NpcClickSmartQueueOutcome.VERIFIED) {\n"
                        + "            rememberVerifiedPoint"),
                "不许绕过闸门直接按 outcome 判断保留");
    }

    // ------------------------------------------ 非 defer 路径回归：三态映射的其余两态一字未改

    @Test
    void nonDeferredVerdictsKeepTheirExactBaselineMapping() {
        assertEquals(NpcClickSmartQueueOutcome.VERIFIED,
                NpcArrivalFrameFifoLocalExecutor.queueOutcomeForVerification(
                        new DialogService.NpcClickVerification(
                                true, true, "GREEN_TEMPLATE_VISIBLE", DialogType.OPTION)));
        assertEquals(NpcClickSmartQueueOutcome.DIALOG_OPEN_UNVERIFIED,
                NpcArrivalFrameFifoLocalExecutor.queueOutcomeForVerification(
                        new DialogService.NpcClickVerification(
                                false, true, "OPTION_VISIBLE", DialogType.OPTION)));
        assertEquals(NpcClickSmartQueueOutcome.VERIFICATION_FAILED,
                NpcArrivalFrameFifoLocalExecutor.queueOutcomeForVerification(
                        new DialogService.NpcClickVerification(
                                false, false, "NO_DIALOG", DialogType.NONE)));
        assertEquals(NpcClickSmartQueueOutcome.VERIFICATION_FAILED,
                NpcArrivalFrameFifoLocalExecutor.queueOutcomeForVerification(null));
        // 四元构造是旧调用点的兼容入口，它永远不许自己变成第三态。
        assertFalse(new DialogService.NpcClickVerification(
                true, true, "GREEN_TEMPLATE_VISIBLE", DialogType.OPTION).deferredToTask());
    }

    // --------------------------- 复审 P1 正名回归：上报编码=本地终态，掩码路径必须死透

    /**
     * G122 复审返修（2026-08-29 P1）：此前 {@code wireOutcomeForCloud} 把 defer 的
     * {@code DIALOG_OPEN_UNVERIFIED} 改回 {@code VERIFIED} 上报，云端
     * {@code enrichArrivalOutcome} 随即派生 {@code success=true}——违反卡上合同门④
     * "无真实 accept.png 不得 VERIFIED"。正名后：客户端如实上报本地终态（掩码方法删除，
     * {@code reportOutcomeAsync} 直传 outcome）；云端 {@code enrichArrivalOutcome} 对 defer
     * 需求的 {@code DIALOG_OPEN_UNVERIFIED} 打 {@code TASK_PHASE_DEFERRED} 且
     * {@code success=false}，{@code registerDeferredPending} 认可如实编码，五环/天庭
     * {@code confirmArrivalFrameClickMemory} 的结算链不断。运行态实证见
     * {@link #aRealDeferredSessionLeavesTheVerifiedPointStoreEmpty}（真实 defer 会话上报的
     * 就是 DIALOG_OPEN_UNVERIFIED）。
     */
    @Test
    void deferredOutcomeIsReportedTruthfullyWithoutAnyWireMask() throws Exception {
        for (Method method : NpcArrivalFrameFifoLocalExecutor.class.getDeclaredMethods()) {
            assertNotEquals("wireOutcomeForCloud", method.getName(),
                    "上报掩码方法必须保持删除状态——defer 待定不得再被改写成 VERIFIED 上报");
        }
        String source = Files.readString(EXECUTOR_SOURCE, StandardCharsets.UTF_8);
        assertFalse(source.contains("wireOutcomeForCloud("),
                "生产源不得再出现任何上报改写调用");
        assertTrue(source.contains("message, outcome, reason);"),
                "reportOutcomeAsync 必须把本地终态原样交给 turnClient.reportNpcArrivalFrameOutcome");
    }

    /**
     * "新任务漏登记白名单"这个错误家族本项目已经犯到第三次了。大理寺**故意不在**打戳等待集里：
     * 客户端唯一的 markTaskDialogOptionAnswered 写入点在
     * {@code WindowObservationSampler.sampleTiantingDialogProbe}，其 {@code TiantingOptionSet
     * .supports()} 把任务类型硬限死在 TIANTING/GHOST_KING。塞进去=每个候选等满 2.5s 判 miss、
     * 12 个候选全烧光（2026-08-23 双倍维护流同款死法）。这条合同把这个判断钉死在案。
     */
    @Test
    @SuppressWarnings("unchecked")
    void deferredAnswerWaitRegistryStillOnlyHoldsTasksThatActuallyStamp() throws Exception {
        java.lang.reflect.Field field = NpcArrivalFrameFifoLocalExecutor.class
                .getDeclaredField("DEFERRED_ANSWER_WAIT_TASK_CODES");
        field.setAccessible(true);
        Set<String> codes = (Set<String>) field.get(null);

        assertEquals(Set.of("ghost_king", "tianting"), codes);
        assertFalse(codes.contains("dalisi_quiz"),
                "大理寺没有打戳方，登记进来只会让每个候选等满超时；它的 accept.png 合同在任务侧");
    }

    /** G102 的坑不许重新挖开：defer 依旧不走截图方差判断，DEFERRED 分支必须先于任何 capture 返回。 */
    @Test
    void g102DeferredCaptureBypassIsStillIntact() throws Exception {
        String source = Files.readString(DIALOG_SERVICE_SOURCE, StandardCharsets.UTF_8);
        int deferReturn = source.indexOf("return NpcClickVerification.deferredPending();");
        int firstCapture = source.indexOf("tracker.captureToMemory(");

        assertTrue(deferReturn > 0, "defer 第三态入口必须存在");
        assertTrue(firstCapture > deferReturn,
                "defer 必须在任何截图之前返回（G102：把 defer 踹回截图判断会必爆方差门）");
        // 只认真实声明，不认注释里的禁令原文（G102 那段禁令本身就写着这两个名字）。
        assertFalse(source.contains("boolean isOptionDialog("), "禁止重新引入 G102 已删除的方差门");
        assertFalse(source.contains("boolean probeOptionDialogPresent("),
                "禁止重新引入 G102 已删除的第二套 presence 判定");
        assertTrue(source.contains("dialogFramePresence.isPresent(raw)"),
                "非 defer 路径仍必须走全站唯一的结构化边框 presence 判定");
    }

    // ---------------------------------------------------------------------------------- 工具

    private static int occurrences(String text, String token) {
        int count = 0;
        int offset = 0;
        while ((offset = text.indexOf(token, offset)) >= 0) {
            count++;
            offset += token.length();
        }
        return count;
    }


    @SuppressWarnings("unchecked")
    private static Map<?, Point> replayStore(
            NpcArrivalFrameFifoLocalExecutor executor, String fieldName) throws Exception {
        Field field = NpcArrivalFrameFifoLocalExecutor.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        return (Map<?, Point>) field.get(executor);
    }

    private static TurnWholeTaskRuntimeArguments deferArguments(boolean reuseLastVerifiedPoint) {
        TurnNpcArrivalFrameFifoSpec spec = new TurnNpcArrivalFrameFifoSpec(
                "dhxy-local", "dhxy-client", WINDOW, HWND_DECIMAL,
                "remote-turn-real-frame", BUSINESS_RUN,
                0, 0, 1024, 768, List.of(), null, true, false,
                reuseLastVerifiedPoint);
        return new TurnWholeTaskRuntimeArguments(
                "test", null, "g122-deferred", null, null,
                null, null, null, null, null, null, null, null,
                "DALISI_QUIZ", "大理寺官员", null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, spec);
    }

    private static final class RecordingInput extends InputSequences {
        private final List<List<InputAction>> submissions = new ArrayList<>();

        private RecordingInput() {
            super((InputActionQueue) null);
        }

        @Override
        public boolean submitAndWait(String description, List<InputAction> actions) {
            submissions.add(List.copyOf(actions));
            return true;
        }
    }

    /** 云端只发一个固定点候选（就是事故里那一下空点）。 */
    private static final class StubTurnClient implements TurnClient {
        private final Queue<NpcClickSmartQueueMessage> messages = new ArrayDeque<>(List.of(
                NpcClickSmartQueueMessage.builder()
                        .type(NpcClickSmartQueueMessage.Type.FIXED_POINT)
                        .sessionId(SESSION)
                        .windowId(WINDOW)
                        .taskRunId(BUSINESS_RUN)
                        .decisionId("g122-fixed-point")
                        .strategy("TARGET_METADATA")
                        .windowRelativeClickPoint(new Point(481, 619))
                        .build()));
        private final CountDownLatch reported = new CountDownLatch(1);
        private volatile NpcClickSmartQueueOutcome reportedOutcome;

        @Override
        public NpcClickSmartCloudSession openNpcArrivalFrame(
                String tenantId, String deviceId, String windowId, String hwnd,
                String observationRunId, String businessTaskRunId, String intentId) {
            return NpcClickSmartCloudSession.builder()
                    .status(NpcClickSmartCloudSession.Status.STARTED)
                    .sessionId(SESSION)
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

        private void awaitReport() throws InterruptedException {
            reported.await(5, TimeUnit.SECONDS);
        }
    }

    /** 事故现场：屏幕上有一个（旧的、别家的）Dialog。 */
    private static final class StaleDialogTracker extends GameClientTracker {
        private final AtomicInteger captureCount;

        private StaleDialogTracker(AtomicInteger captureCount) {
            super(null, null, null, null, null, null, null, null, null, null, null);
            this.captureCount = captureCount;
        }

        @Override
        public BufferedImage captureToMemory(String elementName, int x1, int y1, int x2, int y2) {
            captureCount.incrementAndGet();
            BufferedImage frame = new BufferedImage(
                    Math.max(1, x2 - x1), Math.max(1, y2 - y1), BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = frame.createGraphics();
            try {
                graphics.setColor(new Color(210, 205, 180));
                graphics.fillRect(0, 0, frame.getWidth(), frame.getHeight());
            } finally {
                graphics.dispose();
            }
            return frame;
        }
    }
}
