package com.bot.dhxy.window.observation;

import com.bot.dhxy.cloud.turn.protocol.observation.ObservationInterest;
import com.bot.dhxy.cloud.turn.protocol.observation.ObservationPreparedFrame;
import com.bot.dhxy.cloud.turn.protocol.observation.ObservationPreparedFrameDemand;
import com.bot.dhxy.cloud.turn.protocol.observation.ObservationProtocolValidator;
import com.bot.dhxy.cloud.turn.protocol.observation.ObservationResponse;
import com.bot.dhxy.config.WindowIsolationProperties;
import com.bot.dhxy.core.GameClientTracker;
import com.bot.dhxy.core.GameContext;
import com.bot.dhxy.input.InputSequences;
import com.bot.dhxy.service.DialogService;
import com.bot.dhxy.tools.CoordinateHelper;
import com.bot.dhxy.window.model.WindowNativeBinding;
import com.bot.dhxy.window.runtime.WindowRuntimeContext;
import com.bot.dhxy.window.runtime.WindowTaskContextHolder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * G103-CR/CR2 合同（2026-08-25）：观察循环起搏的纪律。
 *
 * <ul>
 *   <li>绝对采样 deadline（单调时钟）：等待时长=距"上次共享帧捕获+一个周期"的剩余量。</li>
 *   <li>唤醒收窄：兴趣集只有【变快】才唤醒；prepared demand 只有【新到/换代】才唤醒。</li>
 *   <li>动态合同：Cloud 即时回包连续运行时，整窗捕获数与 transport 请求数都必须被采样节奏
 *       上限约束，而不是回包速度（复审 P2① 场景按测试时长缩放）。</li>
 * </ul>
 */
class WindowObservationPaceContractTest {

    @BeforeAll
    static void warmUpOpenCvNativeInit() {
        // 五审 P2③：首轮 OpenCV 本地库初始化实测 ~3.6s，会打穿竞态合同的时间门。
        // 在计时开始前完成一次微型模板匹配，把静态初始化开销挡在所有合同之外。
        com.bot.dhxy.core.ImageFinder.find(
                new BufferedImage(8, 8, BufferedImage.TYPE_INT_RGB),
                new BufferedImage(4, 4, BufferedImage.TYPE_INT_RGB),
                -1.0D);
    }

    private static final String TENANT = "tenant";
    private static final String DEVICE = "device";
    private static final String WINDOW = "hwnd-PACE";
    private static final String HWND = "12345";
    private static final String TASK_CODE = "wuhuan_v3";
    private static final String TASK_RUN = "remote-turn-pace-contract";

    // ---- 绝对 deadline 数学 ----

    @Test
    void paceWaitsFullPeriodWhenNoSharedFrameExists() {
        assertEquals(1_000L, WindowObservationRunner.paceWaitMs(1_000L, 0L, 50_000L));
        assertEquals(300L, WindowObservationRunner.paceWaitMs(300L, -1L, 50_000L));
    }

    @Test
    void paceWaitsOnlyRemainingTimeToCaptureDeadline() {
        assertEquals(100L, WindowObservationRunner.paceWaitMs(1_000L, 10_000L, 10_900L));
        assertEquals(20L, WindowObservationRunner.paceWaitMs(1_000L, 10_000L, 10_980L));
    }

    @Test
    void paceReturnsDueImmediatelyWhenDeadlinePassed() {
        assertTrue(WindowObservationRunner.paceWaitMs(1_000L, 10_000L, 11_000L) <= 0L,
                "capture deadline reached must not wait another period");
        assertTrue(WindowObservationRunner.paceWaitMs(1_000L, 10_000L, 12_500L) <= 0L);
    }

    @Test
    void paceNeverWaitsLongerThanOnePeriod() {
        assertEquals(1_000L, WindowObservationRunner.paceWaitMs(1_000L, 99_000L, 10_000L));
    }

    @Test
    void staleness_boundIsOnePeriod_notNearlyTwo() {
        // 一审 P1② 场景重放：捕获 t=1，周期 1s；t=999 被定点 wake 吵醒（帧复用），旧行为会再睡满
        // 1s → 捕获间隔 ~1.999s。合同：下一拍等待收缩到剩余量，捕获间隔上界=一个周期。
        long nextWait = WindowObservationRunner.paceWaitMs(1_000L, 1L, 999L);
        assertTrue(nextWait <= 2L, "wait after period-end wake must shrink to the remaining time, got " + nextWait);
    }

    // ---- 唤醒收窄（CR2 P2②） ----

    @Test
    void onlyEffectiveCadenceAccelerationWakesTheLoop() {
        long parkedMs = 5_000L;
        long noLocalLane = Long.MAX_VALUE;
        List<ObservationInterest> parked = List.of();
        List<ObservationInterest> combat = List.of(new ObservationInterest("combat-signal", 1_000L, null));
        List<ObservationInterest> faster = List.of(new ObservationInterest("wubei-prepare", 100L, null));

        assertTrue(WindowObservationRunner.shouldWakeForAppliedInterests(parked, combat, parkedMs, noLocalLane),
                "parked 5s heartbeat -> combat 1s accelerates the effective cadence and must wake");
        assertTrue(WindowObservationRunner.shouldWakeForAppliedInterests(combat, faster, parkedMs, noLocalLane));
        assertFalse(WindowObservationRunner.shouldWakeForAppliedInterests(combat, parked, parkedMs, noLocalLane),
                "deceleration must NOT wake: sleeping out the old shorter period transitions naturally");
        assertFalse(WindowObservationRunner.shouldWakeForAppliedInterests(combat, combat, parkedMs, noLocalLane),
                "unchanged cadence must NOT wake (revision-only churn cannot drive the loop)");
        assertFalse(WindowObservationRunner.shouldWakeForAppliedInterests(faster, combat, parkedMs, noLocalLane));
    }

    @Test
    void effectiveCadenceUsesParkedHeartbeatPathingLaneAndMinClamp() {
        List<ObservationInterest> combat = List.of(new ObservationInterest("combat-signal", 1_000L, null));
        // 空兴趣 = parked 与本地车道取快者。
        assertEquals(5_000L, WindowObservationRunner.effectiveCadenceMs(List.of(), 5_000L, Long.MAX_VALUE));
        assertEquals(300L, WindowObservationRunner.effectiveCadenceMs(List.of(), 5_000L, 300L));
        // 非空 = min(本地车道, 最快兴趣)，再按 MIN_SAMPLE_PERIOD_MS 钳制。
        assertEquals(300L, WindowObservationRunner.effectiveCadenceMs(combat, 5_000L, 300L));
        assertEquals(1_000L, WindowObservationRunner.effectiveCadenceMs(combat, 5_000L, Long.MAX_VALUE));
        assertEquals(WindowObservationRunner.MIN_SAMPLE_PERIOD_MS,
                WindowObservationRunner.effectiveCadenceMs(
                        List.of(new ObservationInterest("too-fast", 100L, null)), 5_000L, Long.MAX_VALUE));
        // CR5 P2①：五倍 100ms 车道与寻路一样走统一入口。
        assertEquals(100L, WindowObservationRunner.effectiveCadenceMs(combat, 5_000L, 100L));
        assertEquals(100L, WindowObservationRunner.effectiveCadenceMs(List.of(), 5_000L, 100L));
        // 复审场景：活跃寻路（300ms 车道）期间新到战斗兴趣（1s）不改变有效节奏 → 不唤醒。
        assertFalse(WindowObservationRunner.shouldWakeForAppliedInterests(
                        List.of(), combat, 5_000L, 300L),
                "combat interest arriving while pathing already drives 300ms must not wake");
    }

    @Test
    void onlyNewOrRegeneratedPreparedDemandWakesTheLoop() {
        ObservationPreparedFrameDemand demand = demand("demand-1", 5L);
        assertTrue(WindowObservationRunner.isNewPreparedFrameDemand(null, demand),
                "a first demand is new and must wake");
        assertFalse(WindowObservationRunner.isNewPreparedFrameDemand(demand, demand("demand-1", 5L)),
                "the same demand repeated on every response must NOT wake (retry rides normal cadence)");
        assertTrue(WindowObservationRunner.isNewPreparedFrameDemand(demand, demand("demand-1", 6L)),
                "a regenerated demand is new work");
        assertTrue(WindowObservationRunner.isNewPreparedFrameDemand(demand, demand("demand-2", 5L)));
        assertFalse(WindowObservationRunner.isNewPreparedFrameDemand(demand, null));
    }

    // ---- 动态合同（CR2 P2①/CR3 P2①：真实 Runner+即时回包，稳态计数、紧上界） ----

    @Test
    void combatCadence_instantCloudResponsesCannotExceedOneHertz() throws Exception {
        // 三审场景：战斗兴趣 1s。稳态窗 2.6s 预算 2-3 拍；上界 4 连现场异常的 5.7Hz（≈15 拍）
        // 乃至 2Hz（≈5 拍）都会 FAIL——证明的就是"战斗 1Hz"本身，不是宽松封顶。
        DynamicRun run = runInstantCloud(new ObservationInterest("combat-signal", 1_000L, null), 2_600L);
        assertTrue(run.captures <= 4,
                "combat captures must be ~1Hz, got " + run.captures + " in " + run.elapsedMs + "ms");
        assertTrue(run.requests <= 4,
                "combat transport must be ~1Hz, got " + run.requests + " in " + run.elapsedMs + "ms");
        assertTrue(run.captures >= 1, "sampling must still advance, got " + run.captures);
    }

    @Test
    void pathingCadence_instantCloudResponsesStayOn300msBudget() throws Exception {
        // 三审门要求的寻路 300ms 档。上界 = ceil(elapsed/300)+2（明确启动补贴），
        // 2.1s 预算 7 拍 → 上界 9，拦 5.7Hz（≈12）；下界 4 同时证明限频门没有把节奏拖慢。
        DynamicRun run = runInstantCloud(new ObservationInterest("combat-signal", 300L, null), 2_100L);
        long budget = (run.elapsedMs + 299L) / 300L;
        assertTrue(run.captures <= budget + 2,
                "300ms-lane captures must stay on the period budget, got " + run.captures
                        + " in " + run.elapsedMs + "ms (budget " + budget + ")");
        assertTrue(run.requests <= budget + 2,
                "300ms-lane transport must stay on the period budget, got " + run.requests
                        + " in " + run.elapsedMs + "ms (budget " + budget + ")");
        assertTrue(run.captures >= budget - 3,
                "rate gate must not under-sample either, got " + run.captures + " (budget " + budget + ")");
    }

    @Test
    void newDemandArrivingWhileTransportRunsIsNotLostWhenTransportEnds() throws Exception {
        // 三审 P1 竞态合同（动态，审查原场景）：新 prepared demand 在回包处理中被应用并唤醒——
        // 此刻 transport 仍 running，被吵醒的 runLoop 只能做 physical-only；transport 结束后
        // followupAfterTransport 必须补一次唤醒，让携带截图的下一次上传立即发生。
        // 无该机制时，parked 5s 档最坏拖满 ~5 秒。合同：demand 帧上传须在远小于 parked 的时间内发出。
        CountDownLatch firstSendEntered = new CountDownLatch(1);
        CountDownLatch releaseFirstSend = new CountDownLatch(1);
        AtomicInteger requests = new AtomicInteger();
        CountDownLatch secondRequest = new CountDownLatch(2);
        ObservationPreparedFrameDemand frameDemand = demand("demand-race", 3L);
        byte[] png = fullWindowPng();
        PreparedFrameCapture capture = requested -> new ObservationPreparedFrame(
                requested.demandId(), requested.purpose(), requested.generation(),
                0, 0,
                ObservationProtocolValidator.TERMINAL_FRAME_WIDTH,
                ObservationProtocolValidator.TERMINAL_FRAME_HEIGHT,
                "PNG", System.currentTimeMillis(), png);
        ObservationClient gatedClient = request -> {
            int sequence = requests.incrementAndGet();
            secondRequest.countDown();
            if (sequence == 1) {
                firstSendEntered.countDown();
                try {
                    if (!releaseFirstSend.await(3, TimeUnit.SECONDS)) {
                        throw new AssertionError("test gate timed out");
                    }
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                }
            }
            return ObservationProtocolValidator.requireValid(
                    new ObservationResponse(
                            ObservationProtocolValidator.CONTRACT_VERSION,
                            request.observerSeq(),
                            0L,
                            List.copyOf(request.events().stream()
                                    .map(event -> event.eventId()).toList()),
                            List.of(),
                            List.of(),
                            sequence == 1 ? List.of(frameDemand) : List.of()),
                    request);
        };
        WindowRuntimeContext context = new WindowRuntimeContext(WINDOW, new GameContext());
        context.setNativeBinding(new WindowNativeBinding(HWND, "title", "class", 7L, 0, 0, 1024, 768));
        WindowObservationRunner runner = new WindowObservationRunner(
                gatedClient, TENANT, DEVICE, WINDOW, HWND, TASK_CODE, TASK_RUN,
                sampler(context, new AtomicInteger()), null, capture, 5_000L);
        try {
            runner.start();
            assertTrue(firstSendEntered.await(2, TimeUnit.SECONDS), "first transport must start");
            long releasedAtMs = System.nanoTime() / 1_000_000L;
            releaseFirstSend.countDown();
            boolean arrived = secondRequest.await(4_000, TimeUnit.MILLISECONDS);
            long elapsedMs = System.nanoTime() / 1_000_000L - releasedAtMs;
            assertTrue(arrived, "demand-carrying second request never arrived; requests=" + requests.get());
            assertTrue(elapsedMs < 2_500L,
                    "follow-up wake after transport completion must beat the parked 5s sleep, took "
                            + elapsedMs + "ms (requests=" + requests.get() + ")");
            // 五审 P2③：新 demand 恰好两轮——demand 已交付且被清除后，稳定状态不得出现第三轮。
            Thread.sleep(800L);
            assertEquals(2, requests.get(),
                    "a delivered-and-cleared demand must produce exactly two transport rounds");
        } finally {
            runner.requestStop();
            assertTrue(runner.awaitStopped(Duration.ofSeconds(3)), "runner must stop within the bound");
        }
    }

    @Test
    void heartbeatDeadlineIsAnchoredToLastSuccessfulSend() {
        // 双锚合同：捕获锚起搏的 tick 比"上次成功发送+parked"早几十毫秒时，skip-send 守卫会拦下
        // 该拍——等待必须补到心跳锚，否则 parked 心跳被系统性拖向 2×周期。
        assertEquals(5_000L, WindowObservationRunner.heartbeatWaitMs(5_000L, 0L, 123_456L));
        assertEquals(60L, WindowObservationRunner.heartbeatWaitMs(5_000L, 10_000L, 14_940L));
        assertTrue(WindowObservationRunner.heartbeatWaitMs(5_000L, 10_000L, 15_000L) <= 0L);
        assertEquals(5_000L, WindowObservationRunner.heartbeatWaitMs(5_000L, 99_999L, 10_000L));
    }

    private record DynamicRun(int captures, int requests, long elapsedMs) {
    }

    private DynamicRun runInstantCloud(ObservationInterest interest, long steadyWindowMs) throws Exception {
        WindowRuntimeContext context = new WindowRuntimeContext(WINDOW, new GameContext());
        context.setNativeBinding(new WindowNativeBinding(HWND, "title", "class", 7L, 0, 0, 1024, 768));
        AtomicInteger fullWindowCaptures = new AtomicInteger();
        AtomicInteger requests = new AtomicInteger();
        CountDownLatch firstResponse = new CountDownLatch(1);
        List<ObservationInterest> interests = List.of(interest);
        ObservationClient instantClient = request -> {
            requests.incrementAndGet();
            firstResponse.countDown();
            return ObservationProtocolValidator.requireValid(
                    new ObservationResponse(
                            ObservationProtocolValidator.CONTRACT_VERSION,
                            request.observerSeq(),
                            7L,
                            List.copyOf(request.events().stream()
                                    .map(event -> event.eventId()).toList()),
                            interests,
                            List.of()),
                    request);
        };
        WindowObservationRunner runner = new WindowObservationRunner(
                instantClient, TENANT, DEVICE, WINDOW, HWND, TASK_CODE, TASK_RUN,
                sampler(context, fullWindowCaptures), 5_000L);
        try {
            runner.start();
            assertTrue(firstResponse.await(2, TimeUnit.SECONDS), "transport must reach the fake cloud");
            // 稳态化：等首个兴趣应用（加速唤醒）尘埃落定后清零计数，只量稳态窗。parked=真实 5s，
            // 心跳锚不会在测量窗内触发，请求节奏只可能来自兴趣 cadence（或 bug 下的回包驱动）。
            Thread.sleep(Math.min(400L, interest.samplePeriodMs()));
            fullWindowCaptures.set(0);
            requests.set(0);
            long startedAtMs = System.nanoTime() / 1_000_000L;
            Thread.sleep(steadyWindowMs);
            long elapsedMs = System.nanoTime() / 1_000_000L - startedAtMs;
            return new DynamicRun(fullWindowCaptures.get(), requests.get(), elapsedMs);
        } finally {
            runner.requestStop();
            assertTrue(runner.awaitStopped(Duration.ofSeconds(3)), "runner must stop within the bound");
        }
    }

    // ---- 失败路径动态合同（五审门①）----

    @Test
    void consecutiveTransportExceptionsStayOnCadence_noHotLoop() throws Exception {
        // 首轮成功建立成功锚；此后 HTTP 连续抛异常。合同：请求/捕获都不超过
        // ceil(elapsed/parked)+启动补贴——旧实现里过期成功锚会让 pace 心跳 deadline 永久 <=0，
        // physical-only/collect/HTTP 全速热循环（每秒数百）。
        FailureRun run = runFailingCloud(false);
        long bound = (run.elapsedMs + 799L) / 800L + 2L;
        assertTrue(run.requests <= bound,
                "failing transport must retry on cadence, got " + run.requests
                        + " in " + run.elapsedMs + "ms (bound " + bound + ")");
        assertTrue(run.captures <= bound,
                "captures must stay cadence-capped during failures, got " + run.captures
                        + " (bound " + bound + ")");
        assertTrue(run.requests >= 1, "retry must still happen, got " + run.requests);
    }

    @Test
    void consecutiveLowAcceptedSeqResponsesStayOnCadence_noHotLoop() throws Exception {
        // 连续"2xx 但 acceptedObserverSeq 低于请求"（不覆盖）。旧实现该分支每个回包
        // wakeForLocalStateChange() 立即重试，绕过全部 deadline。合同同上。
        FailureRun run = runFailingCloud(true);
        long bound = (run.elapsedMs + 799L) / 800L + 2L;
        assertTrue(run.requests <= bound,
                "uncovered responses must retry on cadence, got " + run.requests
                        + " in " + run.elapsedMs + "ms (bound " + bound + ")");
        assertTrue(run.captures <= bound,
                "captures must stay cadence-capped during uncovered responses, got " + run.captures
                        + " (bound " + bound + ")");
        assertTrue(run.requests >= 1, "retry must still happen, got " + run.requests);
    }

    private record FailureRun(int requests, int captures, long elapsedMs) {
    }

    private FailureRun runFailingCloud(boolean lowAcceptedSeq) throws Exception {
        WindowRuntimeContext context = new WindowRuntimeContext(WINDOW, new GameContext());
        context.setNativeBinding(new WindowNativeBinding(HWND, "title", "class", 7L, 0, 0, 1024, 768));
        AtomicInteger fullWindowCaptures = new AtomicInteger();
        AtomicInteger requests = new AtomicInteger();
        // 六审 P2：分支判定不得用可被稳态清零的测量计数器——否则清零后的下一请求又被当成
        // "首轮成功"，成功锚被刷新造成 false-green（日志 successfulSends=2 实锤）。
        AtomicInteger totalCalls = new AtomicInteger();
        CountDownLatch firstResponse = new CountDownLatch(1);
        ObservationClient client = request -> {
            requests.incrementAndGet();
            if (totalCalls.incrementAndGet() == 1) {
                firstResponse.countDown();
                return ObservationProtocolValidator.requireValid(
                        new ObservationResponse(
                                ObservationProtocolValidator.CONTRACT_VERSION,
                                request.observerSeq(),
                                0L,
                                List.copyOf(request.events().stream()
                                        .map(event -> event.eventId()).toList()),
                                List.of(),
                                List.of()),
                        request);
            }
            if (lowAcceptedSeq) {
                // 有效 2xx 形状、但 acceptedObserverSeq 永远落后（不覆盖本请求）。
                return new ObservationResponse(
                        ObservationProtocolValidator.CONTRACT_VERSION,
                        0L,
                        0L,
                        List.of(),
                        List.of(),
                        List.of());
            }
            throw new ObservationTransportException(
                    ObservationTransportException.Kind.HTTP_STATUS, "simulated 502");
        };
        // parked=800ms：把"心跳逾期后的失败重试"压进测试时长；生产语义相同（parked=5000）。
        WindowObservationRunner runner = new WindowObservationRunner(
                client, TENANT, DEVICE, WINDOW, HWND, TASK_CODE, TASK_RUN,
                sampler(context, fullWindowCaptures), 800L);
        try {
            runner.start();
            assertTrue(firstResponse.await(2, TimeUnit.SECONDS), "first transport must succeed");
            Thread.sleep(200L);
            fullWindowCaptures.set(0);
            requests.set(0);
            long startedAtMs = System.nanoTime() / 1_000_000L;
            Thread.sleep(2_500L);
            long elapsedMs = System.nanoTime() / 1_000_000L - startedAtMs;
            return new FailureRun(requests.get(), fullWindowCaptures.get(), elapsedMs);
        } finally {
            runner.requestStop();
            assertTrue(runner.awaitStopped(Duration.ofSeconds(3)), "runner must stop within the bound");
        }
    }

    // ---- 慢失败完成→下一次发送 下界合同（六审门）----

    @Test
    void slowTransportExceptionCompletionToNextSendHasBackoffLowerBound() throws Exception {
        SendGapRun run = runSlowFailingCloud(false);
        assertTrue(run.minGapMs >= 600L,
                "completion-to-next-send gap must honor the parked backoff (>=600ms of parked 800ms), min gap was "
                        + run.minGapMs + "ms over " + run.gaps + " gaps");
        assertTrue(run.gaps >= 1, "at least one retry gap must be observed");
    }

    @Test
    void slowLowAcceptedSeqCompletionToNextSendHasBackoffLowerBound() throws Exception {
        SendGapRun run = runSlowFailingCloud(true);
        assertTrue(run.minGapMs >= 600L,
                "completion-to-next-send gap must honor the parked backoff (>=600ms of parked 800ms), min gap was "
                        + run.minGapMs + "ms over " + run.gaps + " gaps");
        assertTrue(run.gaps >= 1, "at least one retry gap must be observed");
    }

    private record SendGapRun(long minGapMs, int gaps) {
    }

    private SendGapRun runSlowFailingCloud(boolean lowAcceptedSeq) throws Exception {
        WindowRuntimeContext context = new WindowRuntimeContext(WINDOW, new GameContext());
        context.setNativeBinding(new WindowNativeBinding(HWND, "title", "class", 7L, 0, 0, 1024, 768));
        java.util.List<long[]> sendWindows = java.util.Collections.synchronizedList(new java.util.ArrayList<>());
        AtomicInteger totalCalls = new AtomicInteger();
        CountDownLatch firstResponse = new CountDownLatch(1);
        ObservationClient client = request -> {
            long enteredAtMs = System.nanoTime() / 1_000_000L;
            int sequence = totalCalls.incrementAndGet();
            if (sequence == 1) {
                firstResponse.countDown();
                sendWindows.add(new long[]{enteredAtMs, System.nanoTime() / 1_000_000L});
                return ObservationProtocolValidator.requireValid(
                        new ObservationResponse(
                                ObservationProtocolValidator.CONTRACT_VERSION,
                                request.observerSeq(),
                                0L,
                                List.copyOf(request.events().stream()
                                        .map(event -> event.eventId()).toList()),
                                List.of(),
                                List.of()),
                        request);
            }
            try {
                // 慢失败：完成时刻晚于 pace 的计算时刻，专打"pace 锚来不及约束"的时序窗。
                Thread.sleep(250L);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
            }
            sendWindows.add(new long[]{enteredAtMs, System.nanoTime() / 1_000_000L});
            if (lowAcceptedSeq) {
                return new ObservationResponse(
                        ObservationProtocolValidator.CONTRACT_VERSION,
                        0L,
                        0L,
                        List.of(),
                        List.of(),
                        List.of());
            }
            throw new ObservationTransportException(
                    ObservationTransportException.Kind.HTTP_STATUS, "simulated slow 502");
        };
        WindowObservationRunner runner = new WindowObservationRunner(
                client, TENANT, DEVICE, WINDOW, HWND, TASK_CODE, TASK_RUN,
                sampler(context, new AtomicInteger()), 800L);
        try {
            runner.start();
            assertTrue(firstResponse.await(2, TimeUnit.SECONDS), "first transport must succeed");
            Thread.sleep(3_200L);
        } finally {
            runner.requestStop();
            assertTrue(runner.awaitStopped(Duration.ofSeconds(3)), "runner must stop within the bound");
        }
        long minGap = Long.MAX_VALUE;
        int gapCount = 0;
        synchronized (sendWindows) {
            // gap = 上一次发送【完成】(含慢失败耗时) 到下一次发送【进入】的间隔；跳过首轮成功对。
            for (int i = 2; i < sendWindows.size(); i++) {
                long gap = sendWindows.get(i)[0] - sendWindows.get(i - 1)[1];
                minGap = Math.min(minGap, gap);
                gapCount++;
            }
        }
        return new SendGapRun(gapCount == 0 ? Long.MAX_VALUE : minGap, gapCount);
    }

    // ---- harness ----

    private static byte[] fullWindowPng() throws Exception {
        BufferedImage image = new BufferedImage(
                ObservationProtocolValidator.TERMINAL_FRAME_WIDTH,
                ObservationProtocolValidator.TERMINAL_FRAME_HEIGHT,
                BufferedImage.TYPE_INT_RGB);
        try (java.io.ByteArrayOutputStream output = new java.io.ByteArrayOutputStream()) {
            javax.imageio.ImageIO.write(image, "PNG", output);
            return output.toByteArray();
        } finally {
            image.flush();
        }
    }

    private static ObservationPreparedFrameDemand demand(String demandId, long generation) {
        return new ObservationPreparedFrameDemand(
                demandId, "wuhuan-dialog", "episode-1", WINDOW, HWND, TASK_RUN,
                generation, System.currentTimeMillis());
    }

    private static WindowObservationSampler sampler(
            WindowRuntimeContext context,
            AtomicInteger fullWindowCaptures) {
        WindowTaskContextHolder holder = new WindowTaskContextHolder(new WindowIsolationProperties());
        GameClientTracker tracker = new GameClientTracker(
                null, null, null, null, null, null, null, null, null, null, null) {
            @Override
            public boolean refreshWindowState() {
                return true;
            }

            @Override
            public int getWindowBaseX() {
                return 0;
            }

            @Override
            public int getWindowBaseY() {
                return 0;
            }

            @Override
            public BufferedImage captureToMemory(String elementName, int x1, int y1, int x2, int y2) {
                if (x2 - x1 >= 1024) {
                    // 只统计整窗捕获——G103 的物理代价就是 PrintWindow 整窗重绘。
                    fullWindowCaptures.incrementAndGet();
                }
                return new BufferedImage(Math.max(1, x2 - x1), Math.max(1, y2 - y1),
                        BufferedImage.TYPE_INT_RGB);
            }
        };
        CoordinateHelper coordinateHelper = new CoordinateHelper(tracker, null);
        return new WindowObservationSampler(
                context,
                holder,
                tracker,
                coordinateHelper,
                new DialogService(tracker, coordinateHelper),
                new InputSequences(null),
                TASK_RUN,
                false);
    }
}
