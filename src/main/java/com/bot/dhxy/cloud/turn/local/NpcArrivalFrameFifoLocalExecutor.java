package com.bot.dhxy.cloud.turn.local;

import com.bot.dhxy.core.MatchEvidenceStore;
import com.bot.dhxy.cloud.task.NpcClickSmartCloudSession;
import com.bot.dhxy.cloud.task.NpcClickSmartQueueMessage;
import com.bot.dhxy.cloud.task.NpcClickSmartQueueOutcome;
import com.bot.dhxy.cloud.turn.TurnClient;
import com.bot.dhxy.cloud.turn.TurnTransportException;
import com.bot.dhxy.cloud.turn.protocol.TurnNpcArrivalFrameFifoSpec;
import com.bot.dhxy.cloud.turn.protocol.TurnWholeTaskRuntimeArguments;
import com.bot.dhxy.core.GameClientTracker;
import com.bot.dhxy.core.ImageFinder;
import com.bot.dhxy.input.InputProvider;
import com.bot.dhxy.input.InputSequences;
import com.bot.dhxy.input.action.InputAction;
import com.bot.dhxy.input.action.InputActionScope;
import com.bot.dhxy.model.dialog.DialogType;
import com.bot.dhxy.runner.context.TaskExecutionContext;
import com.bot.dhxy.runner.context.TaskExecutionContextHolder;
import com.bot.dhxy.runner.stop.TaskCheckpoint;
import com.bot.dhxy.runner.stop.TaskSleep;
import com.bot.dhxy.runner.stop.TaskStopRequestedException;
import com.bot.dhxy.service.DialogService;
import com.bot.dhxy.service.UICleanerService;
import com.bot.dhxy.tools.CoordinateHelper;
import com.bot.dhxy.tools.ImagePreprocessor;
import com.bot.dhxy.window.model.WindowDialogSnapshot;
import com.bot.dhxy.window.model.WindowNativeBinding;
import com.bot.dhxy.window.runtime.WindowRuntimeContext;
import com.bot.dhxy.window.runtime.WindowTaskContextHolder;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

/** Exact-window adaptation of the validated 59b85e0b NPC smart-click FIFO consumer safety shell. */
@Component
public final class NpcArrivalFrameFifoLocalExecutor {

    private static final Logger log = LoggerFactory.getLogger(NpcArrivalFrameFifoLocalExecutor.class);
    /** defer 模式下等任务对话探针答复的上限;超时即判这一下点空,交给下一个候选。 */
    private static final long DEFERRED_DIALOG_ANSWER_WAIT_MS = 2_500L;
    private static final long DEFERRED_DIALOG_ANSWER_POLL_MS = 100L;
    /*
     * 2026-08-22 10:01 事故修正:只有客户端本地对话管线(采样器 tianting/ghost-king 选项机制)
     * 会打 markTaskDialogOptionAnswered 这个戳。五环的接任务选项是云端 prepared 绿模板点的,
     * 客户端永远打不出戳——昨天的等待逻辑对五环就成了"每个候选必等满 2.5s 再判失败",
     * 五个窗口全部卡死在 ACCEPT_TASK。所以这个等待只对会打戳的任务开;其他任务 defer 模式
     * 恢复原语义(点完即过,验证归任务层)。
     */
    /*
     * G122 P1-3（2026-08-29）：这个集合是"任务侧会打 markTaskDialogOptionAnswered 戳"的注册表,
     * **不是**"需要真实验证"的注册表——两者别再混为一谈。加进来的前提是客户端确实有一条本地
     * 对话探针会为该任务打戳。大理寺(dalisi_quiz)**故意不在这里**:客户端唯一的打戳点是
     * WindowObservationSampler.sampleTiantingDialogProbe,它的 TiantingOptionSet.supports() 把
     * 任务类型硬限死在 TIANTING / GHOST_KING,而大理寺本身也从不安装 localTemplateProbeOnly 的
     * 对话兴趣——把 dalisi_quiz 塞进来只会让每个候选等满 2.5s 再判 miss、12 个候选全烧光,
     * 与 2026-08-23 双倍维护流那次一模一样的死法。大理寺的 accept.png 合同因此落在任务侧
     * (DalisiQuizTask.openOfficialDialog),不在这里。
     */
    private static final java.util.Set<String> DEFERRED_ANSWER_WAIT_TASK_CODES =
            java.util.Set.of("ghost_king", "tianting");
    /*
     * 2026-08-23 13:43 事故修正（队长领不到双倍）：ghost_king/tianting 的“领双倍”维护流也走
     * 本 FIFO（NPC=一品侍卫），但它点开的是双倍领取选项框——客户端本地任务选项探针只答复
     * 接任务选项，永远不会为双倍框打 markTaskDialogOptionAnswered 戳。上面的等答复验证对它
     * 必然 2.5s 超时 ×4 候选全判 miss → 云端报 “double-experience NPC click failed” → cleanup
     * 点“告别”把框关掉，领取选项识别根本没有机会运行。对这个维护 NPC 恢复旧语义（点完即过，
     * 验证归维护流自己的对话处理）。抓鬼也用一品侍卫接任务，但 zhuagui 不在上面的任务集里，
     * 不受影响。
     */
    private static final String DOUBLE_EXPERIENCE_MAINTENANCE_NPC = "一品侍卫";
    private static final int WINDOW_WIDTH = 1024;
    private static final int WINDOW_HEIGHT = 768;
    private static final int CANDIDATE_LIMIT = 12;
    private static final long WAIT_SLEEP_MS = 500L;

    /**
     * G125 修 A：会话静默总时限。2026-08-30 20:29:58 云端一边发布 FIFO 开会话动作、一边翻页去
     * maintenance check（生产者与 INSPECT 互相等死），本循环收 WAIT 只睡 500ms 再问、无总时限、
     * 零日志，turn 线程被扣 13.5 分钟，期间队长所有取图命令全部 TIMED_OUT_UNCERTAIN。
     * 收到任何"当前会话的非 WAIT 消息"即重置；到点则上报 FINAL_FAILED 并交还 turn 线程——
     * 宁可这次点击失败走既有恢复，也不许无声扣住整条命令通道。
     */
    private static final long SESSION_QUIET_LIMIT_MS = 60_000L;
    private static final int CTRL_MENU_SCAN_W = 150;
    private static final int CTRL_MENU_SCAN_H = 120;
    private static final String CTRL_TEMPLATE = "images/calibrate/npc_menu_clean_sample.png";
    private static final double CTRL_TEMPLATE_THRESHOLD = 0.80d;
    private static final String GHOST_KING_TASK_CODE = "ghost_king";
    private static final String GHOST_KING_ACCEPT_NPC = "地藏王";
    private static final String GHOST_KING_COMPLETE_STORY_TEMPLATE =
            "images/template/dialog/guiwang/complete.png";
    private static final double GHOST_KING_COMPLETE_STORY_THRESHOLD = 0.85d;
    private static final int[][] CTRL_OFFSETS = {
            {0, 0}, {0, -18}, {18, 0}, {0, 18}, {-18, 0}
    };

    private final TurnClient turnClient;
    private final WindowTaskContextHolder windowContextHolder;
    private final TaskExecutionContextHolder taskContextHolder;
    private final GameClientTracker tracker;
    private final InputSequences inputSequences;
    private final InputProvider inputProvider;
    private final UICleanerService uiCleanerService;
    private final DialogService dialogService;
    private final CoordinateHelper coordinateHelper;
    /** 真的被本地验证过的点击点。G122 P1-3 起这里只收 VERIFIED，defer 的待定点进不来。 */
    private final Map<ReplayPointKey, Point> verifiedReplayPoints = new ConcurrentHashMap<>();
    /*
     * G122 P1-3：defer 的**未验证**待重放点，与已验证点严格分家。
     *
     * 为什么要分家：defer 的点从来没被证明过（大理寺 (481,619) 就是点空的坐标），把它塞进
     * verifiedReplayPoints 等于伪造证据，日志还会照着喊 "verified point retained"。
     * 为什么还要留：五环的"临时不可用"重试环（FiveRingTaskV3，用户口述定案）会显式带
     * reuseLastVerifiedPoint=true 回来重放同一个点——它要的就是"刚才点的那一下再来一次"，
     * 前提是任务侧自己判断没发生过移动。只有显式索要重放的调用方才会读到这张表；
     * 大理寺走 clickNpcSmart，从不索要重放，所以这张表对它是死的。
     */
    private final Map<ReplayPointKey, Point> deferredReplayPoints = new ConcurrentHashMap<>();

    public NpcArrivalFrameFifoLocalExecutor(
            TurnClient turnClient,
            WindowTaskContextHolder windowContextHolder,
            TaskExecutionContextHolder taskContextHolder,
            GameClientTracker tracker,
            InputSequences inputSequences,
            InputProvider inputProvider,
            UICleanerService uiCleanerService,
            DialogService dialogService,
            CoordinateHelper coordinateHelper) {
        this.turnClient = turnClient;
        this.windowContextHolder = windowContextHolder;
        this.taskContextHolder = taskContextHolder;
        this.tracker = tracker;
        this.inputSequences = inputSequences;
        this.inputProvider = inputProvider;
        this.uiCleanerService = uiCleanerService;
        this.dialogService = dialogService;
        this.coordinateHelper = coordinateHelper;
    }

    /**
     * Consumes at most two FIFO sessions. Only first-session END exhaustion performs cleanup; a
     * fresh story blocker may consume the same single replacement budget without cleanup.
     */
    public boolean execute(TurnWholeTaskRuntimeArguments arguments) {
        TurnNpcArrivalFrameFifoSpec spec = arguments == null ? null : arguments.npcArrivalFifo();
        WindowRuntimeContext runtime = windowContextHolder.rawCurrent().orElse(null);
        WindowNativeBinding binding = runtime == null ? null : runtime.getNativeBinding();
        if (spec == null || runtime == null || binding == null
                || !sameIdentity(spec.windowId(), runtime.getWindowId())
                || !sameNativeHandle(spec.hwnd(), binding.getNativeHandle())) {
            log.warn("NPC arrival FIFO rejected before open: specPresent={} runtimePresent={} bindingPresent={} "
                            + "specWindowId={} runtimeWindowId={} specHwnd={} runtimeHwnd={} intentId={}",
                    spec != null,
                    runtime != null,
                    binding != null,
                    spec == null ? null : spec.windowId(),
                    runtime == null ? null : runtime.getWindowId(),
                    spec == null ? null : spec.hwnd(),
                    binding == null ? null : binding.getNativeHandle(),
                    arguments == null ? null : arguments.intentId());
            return false;
        }
        if (GHOST_KING_TASK_CODE.equalsIgnoreCase(arguments.taskCode())
                && GHOST_KING_ACCEPT_NPC.equals(arguments.targetKeyword())
                && !dismissGhostKingCompletionStoryIfPresent(arguments.source())) {
            return false;
        }
        if (spec.reuseLastVerifiedPoint()) {
            return replayLastVerifiedPoint(arguments, spec, binding);
        }

        long storyAnchor = spec.consumeStoryDialogVisibleEvents()
                ? runtime.getStoryDialogVisibleSequence() : Long.MAX_VALUE;
        long lastConsumedStorySequence = 0L;
        for (int attempt = 1; attempt <= 2; attempt++) {
            TaskCheckpoint.throwIfStopRequested(
                    taskContextHolder, "NPC arrival FIFO stopped before session open");
            NpcClickSmartCloudSession session;
            try {
                session = turnClient.openNpcArrivalFrame(
                        spec.tenantId(), spec.deviceId(), spec.windowId(), spec.hwnd(),
                        spec.observationRunId(), spec.businessTaskRunId(), arguments.intentId());
            } catch (TurnTransportException failure) {
                log.warn("NPC arrival FIFO open transport failed: windowId={} taskRunId={} intentId={} kind={} message={}",
                        spec.windowId(), spec.businessTaskRunId(), arguments.intentId(),
                        failure.kind(), failure.getMessage());
                return false;
            }
            if (!isCurrentSession(spec, session)) {
                log.warn("NPC arrival FIFO open rejected: windowId={} taskRunId={} intentId={} "
                                + "sessionStatus={} sessionId={} sessionWindowId={} sessionTaskRunId={} reason={}",
                        spec.windowId(), spec.businessTaskRunId(), arguments.intentId(),
                        session == null ? null : session.getStatus(),
                        session == null ? null : session.getSessionId(),
                        session == null ? null : session.getWindowId(),
                        session == null ? null : session.getTaskRunId(),
                        session == null ? null : session.getReason());
                /*
                 * G125 修 C：session-missing 不是终局。2026-08-30 医宝宝三连（20:12/20:16/20:19）：
                 * demand 在云端登记成功、坐标一路刷新，但本程零停稳帧，会话从未建成；fresh
                 * in-tolerance 瞬判到达后 38ms 强制开会话必然拒开，任务把这次点击当 exhausted 跳过，
                 * 每轮白跑一趟巫医。人此刻就站在 NPC 前——现拍一帧走既有 replace 通道给云端当首帧
                 * （云端放行 preparedAttempts==0 的 replacement，会话天生解锁），重开一次。
                 * 仅限第一次尝试：第二次仍拒说明云端连 demand 都没有，按原路失败。
                 */
                if (attempt == 1 && isSessionMissingRejection(session)
                        && replaceWithFreshFrame(arguments, spec, binding)) {
                    log.warn("NPC arrival FIFO sent fresh first frame after session-missing; retrying open: "
                                    + "windowId={} taskRunId={} intentId={}",
                            spec.windowId(), spec.businessTaskRunId(), arguments.intentId());
                    continue;
                }
                return false;
            }
            log.info("NPC arrival FIFO session opened: windowId={} taskRunId={} intentId={} sessionId={} attempt={}",
                    spec.windowId(), spec.businessTaskRunId(), arguments.intentId(),
                    session.getSessionId(), attempt);

            SessionResult result = consumeOne(
                    arguments, spec, runtime, binding, session, storyAnchor,
                    lastConsumedStorySequence);
            if (result.outcome() == SessionOutcome.VERIFIED) {
                return true;
            }
            if (attempt == 2) {
                return false;
            }
            if (result.outcome() == SessionOutcome.STORY_BLOCKED) {
                if (!fastClickKnownSmallStoryDialog(arguments.source())) {
                    return false;
                }
                lastConsumedStorySequence = result.storySequence();
            } else if (result.outcome() == SessionOutcome.EXHAUSTED) {
                uiCleanerService.cleanUpAll();
            } else {
                return false;
            }
            if (!replaceWithFreshFrame(arguments, spec, binding)) {
                return false;
            }
        }
        return false;
    }

    private SessionResult consumeOne(
            TurnWholeTaskRuntimeArguments arguments,
            TurnNpcArrivalFrameFifoSpec spec,
            WindowRuntimeContext runtime,
            WindowNativeBinding binding,
            NpcClickSmartCloudSession session,
            long storyAnchor,
            long lastConsumedStorySequence) {
        int candidateMessageCount = 0;
        long quietSinceMs = System.currentTimeMillis();
        try {
            while (candidateMessageCount < CANDIDATE_LIMIT) {
                TaskCheckpoint.throwIfStopRequested(
                        taskContextHolder, "NPC arrival FIFO stopped before poll");
                long quietMs = System.currentTimeMillis() - quietSinceMs;
                if (quietMs > SESSION_QUIET_LIMIT_MS) {
                    // G125 修 A：见 SESSION_QUIET_LIMIT_MS。到点必须交还 turn 线程并留痕。
                    log.warn("NPC arrival FIFO quiet limit exceeded; releasing turn thread: windowId={} "
                                    + "taskRunId={} intentId={} sessionId={} quietMs={}",
                            spec.windowId(), spec.businessTaskRunId(), arguments.intentId(),
                            session.getSessionId(), quietMs);
                    reportOutcomeAsync(
                            arguments, spec, terminalMessage(spec, session),
                            NpcClickSmartQueueOutcome.FINAL_FAILED,
                            "no actionable cloud FIFO message within " + SESSION_QUIET_LIMIT_MS + "ms");
                    return SessionResult.terminal();
                }
                long storySequence = freshStorySequence(
                        spec, runtime, storyAnchor, lastConsumedStorySequence);
                if (storySequence > 0L) {
                    reportOutcomeAsync(
                            arguments, spec, terminalMessage(spec, session),
                            NpcClickSmartQueueOutcome.CANCELLED,
                            "story dialog blocker observed at FIFO boundary");
                    return new SessionResult(SessionOutcome.STORY_BLOCKED, storySequence);
                }

                NpcClickSmartQueueMessage message;
                try {
                    message = turnClient.pollNpcArrivalFrame(
                            spec.tenantId(), spec.deviceId(), spec.windowId(), spec.hwnd(),
                            spec.observationRunId(), spec.businessTaskRunId(), arguments.intentId());
                } catch (TurnTransportException failure) {
                    log.warn("NPC arrival FIFO poll transport failed: windowId={} taskRunId={} intentId={} "
                                    + "sessionId={} kind={} message={}",
                            spec.windowId(), spec.businessTaskRunId(), arguments.intentId(),
                            session.getSessionId(), failure.kind(), failure.getMessage());
                    reportOutcomeAsync(
                            arguments, spec, terminalMessage(spec, session),
                            NpcClickSmartQueueOutcome.FINAL_FAILED,
                            "NPC arrival FIFO poll transport failed");
                    return SessionResult.terminal();
                }
                TaskCheckpoint.throwIfStopRequested(
                        taskContextHolder, "NPC arrival FIFO stopped after poll");
                if (message != null
                        && message.getType() == NpcClickSmartQueueMessage.Type.INVALID
                        && sameIdentity(spec.windowId(), message.getWindowId())
                        && sameIdentity(spec.businessTaskRunId(), message.getTaskRunId())) {
                    // 云端答"会话无效/不存在"（如 session-missing）时 sessionId 是空的，
                    // 永远过不了下面的防串号门；必须先于该门终止本会话，否则轮询热循环
                    // （2026-08-23 21:19 事故：一分钟 2.6 万条 stale 日志、接任务腿卡死）。
                    log.warn("NPC arrival FIFO terminated by cloud INVALID: windowId={} taskRunId={} "
                                    + "sessionId={} reason={}",
                            spec.windowId(), spec.businessTaskRunId(),
                            session.getSessionId(), message.getReason());
                    reportOutcomeAsync(
                            arguments, spec, message,
                            NpcClickSmartQueueOutcome.FINAL_FAILED,
                            "invalid cloud FIFO queue message: " + message.getReason());
                    return SessionResult.terminal();
                }
                if (!isCurrentQueueMessage(spec, session, message)) {
                    log.warn("NPC arrival FIFO stale message ignored: expectedSessionId={} actualSessionId={} "
                                    + "expectedWindowId={} actualWindowId={} expectedTaskRunId={} actualTaskRunId={} "
                                    + "type={} decisionId={} reason={}",
                            session.getSessionId(), message == null ? null : message.getSessionId(),
                            spec.windowId(), message == null ? null : message.getWindowId(),
                            spec.businessTaskRunId(), message == null ? null : message.getTaskRunId(),
                            message == null ? null : message.getType(),
                            message == null ? null : message.getDecisionId(),
                            message == null ? null : message.getReason());
                    reportOutcomeAsync(
                            arguments, spec, message,
                            NpcClickSmartQueueOutcome.STALE_IGNORED,
                            "stale session/window/task mismatch ignored");
                    // 扔掉重问必须歇一拍：立即 continue 曾把串号应答刷成热循环。
                    if (!TaskSleep.sleep(WAIT_SLEEP_MS)) {
                        break;
                    }
                    continue;
                }
                if (message.getType() == NpcClickSmartQueueMessage.Type.WAIT) {
                    if (!TaskSleep.sleep(WAIT_SLEEP_MS)) {
                        break;
                    }
                    TaskCheckpoint.throwIfStopRequested(
                            taskContextHolder, "NPC arrival FIFO stopped after WAIT");
                    continue;
                }
                quietSinceMs = System.currentTimeMillis();
                log.info("NPC arrival FIFO message received: sessionId={} type={} decisionId={} strategy={} "
                                + "point={} ctrlCandidates={} confidence={} reason={}",
                        message.getSessionId(), message.getType(), message.getDecisionId(),
                        message.getStrategy(), message.getWindowRelativeClickPoint(),
                        message.getCtrlProbePoints() == null ? 0 : message.getCtrlProbePoints().size(),
                        message.getConfidence(), message.getReason());
                if (message.getType() == NpcClickSmartQueueMessage.Type.END) {
                    reportOutcomeAsync(
                            arguments, spec, message,
                            NpcClickSmartQueueOutcome.FINAL_FAILED,
                            "cloud FIFO queue ended without verified NPC click");
                    return SessionResult.exhausted();
                }
                if (message.getType() == NpcClickSmartQueueMessage.Type.INVALID) {
                    reportOutcomeAsync(
                            arguments, spec, message,
                            NpcClickSmartQueueOutcome.FINAL_FAILED,
                            "invalid cloud FIFO queue message: " + message.getReason());
                    return SessionResult.terminal();
                }
                if (message.getType() == NpcClickSmartQueueMessage.Type.CONTINUATION) {
                    /*
                     * G122 P1-2：显式续帧状态。这不是 miss——该阶段（strategy）明确说"给我一张新帧
                     * 或后续动作我才能出点"，证据（candidateBox/reason）原样保留在消息里。这里
                     * 不烧候选槽、不推进 miss 计数，剩余候选级（紫名/CTRL）继续在当前帧上尝试；
                     * 若本会话最终 END，既有的 EXHAUSTED -> replaceWithFreshFrame -> attempt 2 就是
                     * 同族续帧闭环（同 intent、新帧、新 session、generation 随 demand.frameKey 前进）。
                     * 此前这类结果被云端归一化成 NO_ACTION/point=null 当普通 miss 烧掉（五开事故）。
                     */
                    log.info("NPC arrival FIFO continuation requested by cloud stage: sessionId={} "
                                    + "stage={} continuedAction={} candidateBox={} reason={}",
                            message.getSessionId(), message.getStrategy(), message.getMatchedText(),
                            message.getCandidateBox(), message.getReason());
                    reportOutcomeAsync(
                            arguments, spec, message,
                            NpcClickSmartQueueOutcome.SKIPPED,
                            "continuation noted; fresh-frame replacement completes this stage");
                    continue;
                }

                candidateMessageCount++;
                NpcClickSmartQueueOutcome localOutcome;
                if (message.getType() == NpcClickSmartQueueMessage.Type.MEMORY) {
                    localOutcome = message.hasClickPoint()
                            ? executeQueueCandidate(arguments, spec, binding, message)
                            : NpcClickSmartQueueOutcome.SKIPPED;
                } else if (message.isOrdinaryClickCandidate()) {
                    localOutcome = message.hasClickPoint()
                            ? executeQueueCandidate(arguments, spec, binding, message)
                            : NpcClickSmartQueueOutcome.SKIPPED;
                } else if (message.getType() == NpcClickSmartQueueMessage.Type.CTRL_CANDIDATES) {
                    localOutcome = executeCtrlCandidates(arguments, spec, binding, message);
                } else {
                    localOutcome = NpcClickSmartQueueOutcome.FINAL_FAILED;
                }
                log.info("NPC arrival FIFO candidate handled: sessionId={} type={} decisionId={} point={} outcome={}",
                        message.getSessionId(), message.getType(), message.getDecisionId(),
                        message.getWindowRelativeClickPoint(), localOutcome);
                reportOutcomeAsync(
                        arguments, spec, message, localOutcome,
                        "local verifier outcome after FIFO candidate");
                if (localOutcome == NpcClickSmartQueueOutcome.VERIFIED
                        || (spec.deferDialogVerificationToTask()
                        && localOutcome == NpcClickSmartQueueOutcome.DIALOG_OPEN_UNVERIFIED)) {
                    if (localOutcome == NpcClickSmartQueueOutcome.DIALOG_OPEN_UNVERIFIED) {
                        // G122 P1-3：这条不是"已验证"。本地未验证（defer 第三态或框在但模板没中），
                        // 会话按 defer 语义收工，证明责任整体在任务侧；点位不留。
                        log.info("NPC arrival FIFO handed UNVERIFIED click to task-owned classifier: "
                                        + "windowId={} taskRunId={} intentId={} decisionId={} retainedPoint=false",
                                spec.windowId(), spec.businessTaskRunId(), arguments.intentId(),
                                message.getDecisionId());
                    }
                    return SessionResult.verified();
                }
                if (localOutcome == NpcClickSmartQueueOutcome.SKIPPED
                        || localOutcome == NpcClickSmartQueueOutcome.VERIFICATION_FAILED) {
                    continue;
                }
                return SessionResult.terminal();
            }
        } catch (TaskStopRequestedException stopped) {
            reportOutcomeAsync(
                    arguments, spec, terminalMessage(spec, session),
                    NpcClickSmartQueueOutcome.CANCELLED,
                    "stop requested while consuming NPC arrival FIFO");
            throw stopped;
        }
        boolean stopped = Thread.currentThread().isInterrupted()
                || taskContextHolder.current().map(TaskExecutionContext::isStopRequested).orElse(false);
        reportOutcomeAsync(
                arguments, spec, terminalMessage(spec, session),
                stopped ? NpcClickSmartQueueOutcome.CANCELLED : NpcClickSmartQueueOutcome.FINAL_FAILED,
                stopped
                        ? "stop requested while consuming NPC arrival FIFO"
                        : "NPC arrival FIFO candidate budget reached");
        return SessionResult.terminal();
    }

    private NpcClickSmartQueueOutcome executeQueueCandidate(
            TurnWholeTaskRuntimeArguments arguments,
            TurnNpcArrivalFrameFifoSpec spec,
            WindowNativeBinding binding,
            NpcClickSmartQueueMessage message) {
        Point click = message.getWindowRelativeClickPoint();
        if (!insideAllowedRegion(click, spec)) {
            log.warn("NPC arrival FIFO candidate rejected by safety region: sessionId={} type={} point={} "
                            + "allowed=({},{} {}x{})",
                    message.getSessionId(), message.getType(), click,
                    spec.allowedLeft(), spec.allowedTop(), spec.allowedWidth(), spec.allowedHeight());
            return NpcClickSmartQueueOutcome.SAFETY_REJECTED;
        }
        NpcClickSmartQueueOutcome outcome = executePointAndVerify(
                arguments,
                spec,
                binding,
                click,
                "fifoCandidate:" + message.getType(),
                "npc-click-smart-fifo:" + message.getType() + ":" + message.getDecisionId());
        if (shouldRetainVerifiedPoint(outcome)) {
            rememberVerifiedPoint(arguments, spec, click);
        } else if (shouldRetainDeferredPoint(spec, outcome)) {
            rememberDeferredPoint(arguments, spec, click);
        }
        return outcome;
    }

    private NpcClickSmartQueueOutcome executePointAndVerify(
            TurnWholeTaskRuntimeArguments arguments,
            TurnNpcArrivalFrameFifoSpec spec,
            WindowNativeBinding binding,
            Point click,
            String actionSource,
            String verificationSource) {
        int absoluteX = binding.getX() + click.x;
        int absoluteY = binding.getY() + click.y;
        log.info("NPC arrival FIFO submitting point: source={} relative=({}, {}) absolute=({}, {})",
                actionSource, click.x, click.y, absoluteX, absoluteY);
        long clickAtMs = System.currentTimeMillis();
        boolean submitted = inputSequences.submitAndWait(
                "npcClick:" + actionSource + ":" + arguments.targetKeyword(),
                List.of(
                        InputAction.moveMouse(absoluteX, absoluteY),
                        InputAction.sleep(150),
                        InputAction.clickLeft(absoluteX, absoluteY, 150),
                        InputAction.sleep(1_500)));
        if (!submitted) {
            return taskContextHolder.current().map(TaskExecutionContext::isStopRequested).orElse(false)
                    || Thread.currentThread().isInterrupted()
                    ? NpcClickSmartQueueOutcome.CANCELLED
                    : NpcClickSmartQueueOutcome.INPUT_SUBMIT_FAILED;
        }
        if (taskContextHolder.current().map(TaskExecutionContext::isStopRequested).orElse(false)
                || Thread.currentThread().isInterrupted()) {
            return NpcClickSmartQueueOutcome.CANCELLED;
        }
        if (spec.deferDialogVerificationToTask()
                && DEFERRED_ANSWER_WAIT_TASK_CODES.contains(
                        arguments.taskCode() == null ? "" : arguments.taskCode().toLowerCase())
                && !DOUBLE_EXPERIENCE_MAINTENANCE_NPC.equals(arguments.targetKeyword())) {
            /*
             * 2026-08-21 用户拍板(18:12 鬼王空点事故):defer 模式以前是"点出去就算成功",于是 FIFO
             * 在第一个候选(固定点)之后立刻收工,后面的记忆点/tooltip/黄名永远轮不到——浮窗把 NPC
             * 挡住时就一直空点同一个坐标,只能等云端 10 秒 park 超时整轮重来(实测每轮 ~28 秒)。
             * 任务侧装的对话兴趣本来就每个采样周期在匹配"接任务"选项、匹配到就点,所以这里改成
             * 等这条已有事实:等到=这一下确实点中了 NPC,收工;等不到=点空,交给下一个候选。
             * 不新拍图、不新开校验,也不会和那个探针抢——用的就是它自己的答复。
             */
            return awaitDeferredDialogAnswer(clickAtMs, verificationSource);
        }
        /*
         * G102 收口（2026-08-24 review P1）：这里曾被硬编码 false，把 defer 任务（五环）重新
         * 踹回旧标准差截图判断——接任务框六行绿字必爆方差门 → VERIFICATION_FAILED → 重复
         * 点 NPC 误开"恢复抽取"页（3465 实锤）。恢复 pushed 语义：defer 任务点完即过，
         * 对话验证归任务自己的 presence/分类链。
         *
         * G122 P1-3（2026-08-29）：这个坑没有被重新挖开——defer 依旧不在这里拍图判方差，
         * 依旧"点完即过"。变的只有 defer 的**终态语义**：它从"假 VERIFIED"降级成
         * DIALOG_OPEN_UNVERIFIED（非成功的待定态），因此不再留 retained point。
         */
        return queueOutcomeForVerification(dialogService.verifyNpcArrivalExpectedDialog(
                spec.expectedDialogTemplatePaths(),
                spec.expectedDialogRawTemplatePath(),
                spec.deferDialogVerificationToTask(),
                verificationSource));
    }

    /**
     * Wait a bounded time for the task-owned dialog interest to report that it clicked its option.
     *
     * @param clickAtMs moment this candidate's click was submitted; only a later answer counts.
     * @return VERIFIED once the option was answered, VERIFICATION_FAILED when the wait expires.
     */
    private NpcClickSmartQueueOutcome awaitDeferredDialogAnswer(long clickAtMs, String verificationSource) {
        WindowRuntimeContext runtime = windowContextHolder.rawCurrent().orElse(null);
        if (runtime == null) {
            // 没有窗口上下文就拿不到这条事实:保持旧语义,不把正常流程判失败。
            return NpcClickSmartQueueOutcome.VERIFIED;
        }
        long deadlineAtMs = System.currentTimeMillis() + DEFERRED_DIALOG_ANSWER_WAIT_MS;
        while (System.currentTimeMillis() < deadlineAtMs) {
            if (taskContextHolder.current().map(TaskExecutionContext::isStopRequested).orElse(false)
                    || Thread.currentThread().isInterrupted()) {
                return NpcClickSmartQueueOutcome.CANCELLED;
            }
            if (runtime.lastTaskDialogOptionAnsweredAtMs() >= clickAtMs) {
                log.info("NPC arrival FIFO deferred verification satisfied by task dialog answer: source={} waitedMs={}",
                        verificationSource, System.currentTimeMillis() - clickAtMs);
                return NpcClickSmartQueueOutcome.VERIFIED;
            }
            try {
                Thread.sleep(DEFERRED_DIALOG_ANSWER_POLL_MS);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                return NpcClickSmartQueueOutcome.CANCELLED;
            }
        }
        log.info("NPC arrival FIFO deferred verification found no task dialog answer; treat as miss: source={} waitedMs={}",
                verificationSource, DEFERRED_DIALOG_ANSWER_WAIT_MS);
        return NpcClickSmartQueueOutcome.VERIFICATION_FAILED;
    }

    private NpcClickSmartQueueOutcome executeCtrlCandidates(
            TurnWholeTaskRuntimeArguments arguments,
            TurnNpcArrivalFrameFifoSpec spec,
            WindowNativeBinding binding,
            NpcClickSmartQueueMessage message) {
        if (message.getCtrlProbePoints() == null || message.getCtrlProbePoints().isEmpty()) {
            return NpcClickSmartQueueOutcome.SAFETY_REJECTED;
        }
        for (int index = 0; index < message.getCtrlProbePoints().size(); index++) {
            TaskCheckpoint.throwIfStopRequested(
                    taskContextHolder, "NPC arrival FIFO stopped before Ctrl candidate");
            Point probeRel = message.getCtrlProbePoints().get(index);
            if (!insideAllowedRegion(probeRel, spec)) {
                continue;
            }
            AtomicReference<NpcClickSmartQueueOutcome> outcomeRef =
                    new AtomicReference<>(NpcClickSmartQueueOutcome.VERIFICATION_FAILED);
            int candidateIndex = index;
            boolean submitted = inputSequences.submitExclusiveAndWait(
                    "npcClick:fifoCtrlMenuScan:" + candidateIndex + ":" + arguments.targetKeyword(),
                    () -> {
                        Point probeAbs = new Point(
                                binding.getX() + probeRel.x,
                                binding.getY() + probeRel.y);
                        NpcClickSmartQueueOutcome outcome = executeCtrlMenuProbeDirect(
                                spec, binding, message, probeAbs);
                        outcomeRef.set(outcome);
                        return outcome == NpcClickSmartQueueOutcome.VERIFIED;
                    });
            if (!submitted && outcomeRef.get() == NpcClickSmartQueueOutcome.INPUT_SUBMIT_FAILED) {
                return NpcClickSmartQueueOutcome.INPUT_SUBMIT_FAILED;
            }
            if (outcomeRef.get() == NpcClickSmartQueueOutcome.VERIFIED
                    || outcomeRef.get() == NpcClickSmartQueueOutcome.DIALOG_OPEN_UNVERIFIED
                    || outcomeRef.get() == NpcClickSmartQueueOutcome.CANCELLED
                    || outcomeRef.get() == NpcClickSmartQueueOutcome.SAFETY_REJECTED) {
                if (shouldRetainVerifiedPoint(outcomeRef.get())) {
                    rememberVerifiedPoint(arguments, spec, probeRel);
                } else if (shouldRetainDeferredPoint(spec, outcomeRef.get())) {
                    rememberDeferredPoint(arguments, spec, probeRel);
                }
                return outcomeRef.get();
            }
        }
        return NpcClickSmartQueueOutcome.VERIFICATION_FAILED;
    }

    private boolean replayLastVerifiedPoint(
            TurnWholeTaskRuntimeArguments arguments,
            TurnNpcArrivalFrameFifoSpec spec,
            WindowNativeBinding binding) {
        // G122 P1-3：已验证点优先；没有已验证点时才回落到 defer 的**未验证**待定点（五环的
        // "临时不可用"重放要的就是它）。两张表分家，日志如实说明这次重放的是哪一种。
        Point retained = verifiedReplayPoints.get(ReplayPointKey.from(arguments, spec));
        boolean proven = retained != null;
        if (retained == null) {
            retained = deferredReplayPoints.get(ReplayPointKey.from(arguments, spec));
        }
        if (!insideAllowedRegion(retained, spec)) {
            log.warn("NPC arrival retained-point replay rejected: windowId={} taskRunId={} intentId={} point={}",
                    spec.windowId(), spec.businessTaskRunId(), arguments.intentId(), retained);
            return false;
        }
        log.info("NPC arrival retained-point replay source: windowId={} taskRunId={} intentId={} proven={}",
                spec.windowId(), spec.businessTaskRunId(), arguments.intentId(), proven);
        NpcClickSmartQueueOutcome outcome = executePointAndVerify(
                arguments,
                spec,
                binding,
                new Point(retained),
                "fifoRetainedPointReplay",
                "npc-click-smart-fifo:retained-point-replay");
        log.info("NPC arrival retained-point replay finished: windowId={} taskRunId={} intentId={} point={} outcome={}",
                spec.windowId(), spec.businessTaskRunId(), arguments.intentId(), retained, outcome);
        return outcome == NpcClickSmartQueueOutcome.VERIFIED
                || (spec.deferDialogVerificationToTask()
                && outcome == NpcClickSmartQueueOutcome.DIALOG_OPEN_UNVERIFIED);
    }

    /** 2026-08-23 用户契约（停止=彻底清空）：清该窗口累积的已验证点击点（键自带 run 围栏，此处只治泄漏）。 */
    public void forgetWindowRealityMemory(String windowId) {
        if (windowId != null && !windowId.isBlank()) {
            verifiedReplayPoints.keySet().removeIf(key -> windowId.equals(key.windowId()));
            deferredReplayPoints.keySet().removeIf(key -> windowId.equals(key.windowId()));
        }
    }

    private void rememberVerifiedPoint(
            TurnWholeTaskRuntimeArguments arguments,
            TurnNpcArrivalFrameFifoSpec spec,
            Point point) {
        ReplayPointKey key = ReplayPointKey.from(arguments, spec);
        verifiedReplayPoints.put(key, new Point(point));
        log.info("NPC arrival verified point retained for local replay: windowId={} taskRunId={} intentId={} point={}",
                spec.windowId(), spec.businessTaskRunId(), arguments.intentId(), point);
    }

    /** G122 P1-3：待定点单独记账，日志明确写 UNVERIFIED——不许再冒充"已验证点击点"。 */
    private void rememberDeferredPoint(
            TurnWholeTaskRuntimeArguments arguments,
            TurnNpcArrivalFrameFifoSpec spec,
            Point point) {
        ReplayPointKey key = ReplayPointKey.from(arguments, spec);
        deferredReplayPoints.put(key, new Point(point));
        log.info("NPC arrival UNVERIFIED deferred point retained for explicit replay only: "
                        + "windowId={} taskRunId={} intentId={} point={}",
                spec.windowId(), spec.businessTaskRunId(), arguments.intentId(), point);
    }

    private NpcClickSmartQueueOutcome executeCtrlMenuProbeDirect(
            TurnNpcArrivalFrameFifoSpec spec,
            WindowNativeBinding binding,
            NpcClickSmartQueueMessage message,
            Point probeAbs) {
        if (probeAbs == null || !InputActionScope.checkpoint()) {
            return probeAbs == null
                    ? NpcClickSmartQueueOutcome.SAFETY_REJECTED
                    : NpcClickSmartQueueOutcome.CANCELLED;
        }
        for (int[] offset : CTRL_OFFSETS) {
            int testX = clamp(
                    probeAbs.x + offset[0], binding.getX(), binding.getX() + WINDOW_WIDTH - 1);
            int testY = clamp(
                    probeAbs.y + offset[1], binding.getY(), binding.getY() + WINDOW_HEIGHT - 1);
            int[] scanRect = buildCtrlMenuScanRect(testX, testY, binding);
            inputProvider.holdCtrl();
            try {
                if (!TaskSleep.sleep(80) || !InputActionScope.checkpoint()) {
                    return NpcClickSmartQueueOutcome.CANCELLED;
                }
                inputProvider.moveMouse(testX, testY);
                if (!TaskSleep.sleep(280) || !InputActionScope.checkpoint()) {
                    return NpcClickSmartQueueOutcome.CANCELLED;
                }
                NpcClickSmartQueueOutcome outcome = scanCtrlMenuAndVerifyDirect(
                        spec, message, scanRect);
                if (outcome == NpcClickSmartQueueOutcome.VERIFIED
                        || outcome == NpcClickSmartQueueOutcome.DIALOG_OPEN_UNVERIFIED
                        || outcome == NpcClickSmartQueueOutcome.CANCELLED
                        || outcome == NpcClickSmartQueueOutcome.SAFETY_REJECTED) {
                    return outcome;
                }
            } finally {
                inputProvider.releaseCtrl();
                TaskSleep.sleep(100);
            }
        }
        return NpcClickSmartQueueOutcome.VERIFICATION_FAILED;
    }

    private NpcClickSmartQueueOutcome scanCtrlMenuAndVerifyDirect(
            TurnNpcArrivalFrameFifoSpec spec,
            NpcClickSmartQueueMessage message,
            int[] scanRect) {
        BufferedImage raw = tracker.captureToMemory(
                "npc-arrival-fifo-ctrl-menu",
                scanRect[0], scanRect[1], scanRect[2], scanRect[3]);
        BufferedImage template = ImagePreprocessor.pathToBufferedImage(CTRL_TEMPLATE);
        if (raw == null || template == null) {
            if (raw != null) {
                raw.flush();
            }
            if (template != null) {
                template.flush();
            }
            return NpcClickSmartQueueOutcome.VERIFICATION_FAILED;
        }
        double[] match;
        try {
            match = ImageFinder.find(raw, template, CTRL_TEMPLATE_THRESHOLD);
            MatchEvidenceStore.save("npc-ctrl-verify", null, raw, template, match);
        } finally {
            raw.flush();
            template.flush();
        }
        if (match == null || match.length < 3) {
            return NpcClickSmartQueueOutcome.VERIFICATION_FAILED;
        }
        int clickX = scanRect[0] + (int) Math.round(match[0]);
        int clickY = scanRect[1] + (int) Math.round(match[1]);
        inputProvider.moveMouse(clickX, clickY);
        if (!TaskSleep.sleep(100) || !InputActionScope.checkpoint()) {
            return NpcClickSmartQueueOutcome.CANCELLED;
        }
        inputProvider.clickLeft(clickX, clickY, 150);
        if (!TaskSleep.sleep(900) || !InputActionScope.checkpoint()) {
            return NpcClickSmartQueueOutcome.CANCELLED;
        }
        return queueOutcomeForVerification(dialogService.verifyNpcArrivalExpectedDialog(
                spec.expectedDialogTemplatePaths(),
                spec.expectedDialogRawTemplatePath(),
                spec.deferDialogVerificationToTask(),
                "npc-click-smart-fifo:CTRL_CANDIDATES:" + message.getDecisionId()));
    }

    /**
     * G122 P1-3：三态映射。DEFERRED 是非成功的独立状态，绝不许再折叠成 VERIFIED。
     *
     * <p>defer 的第三态在本地什么都没看过（不拍图、不判 presence、不匹配模板），所以它只能落到
     * {@code DIALOG_OPEN_UNVERIFIED}——"这一下点出去了，验证归任务侧，本地没有证据"。会话结论
     * 仍由 {@code consumeOne} 的 defer 分支负责（点完即过，语义不变），但**它不再是 VERIFIED，
     * 于是也不再触发 {@link #rememberVerifiedPoint}**：点空的坐标从此不会被存成"已验证点击点"
     * 去污染 retained-point replay（大理寺 (481,619) 实锤）。</p>
     */
    static NpcClickSmartQueueOutcome queueOutcomeForVerification(
            DialogService.NpcClickVerification verification) {
        if (verification != null && verification.deferredToTask()) {
            return NpcClickSmartQueueOutcome.DIALOG_OPEN_UNVERIFIED;
        }
        if (verification != null && verification.verified()) {
            return NpcClickSmartQueueOutcome.VERIFIED;
        }
        if (verification != null && verification.optionDialogVisible()) {
            return NpcClickSmartQueueOutcome.DIALOG_OPEN_UNVERIFIED;
        }
        return NpcClickSmartQueueOutcome.VERIFICATION_FAILED;
    }

    /**
     * G122 P1-3：只有**真的被验证过**的点才配当 retained replay 点。
     *
     * <p>这是两条独立合同里的第二条（第一条=不得判 VERIFIED）。defer/待定、看见框但没验上、
     * 验证失败——三者一律不留点。留一个没证据的点，下一轮 {@code reuseLastVerifiedPoint} 重放
     * 就会照着同一个空坐标再点一次。</p>
     */
    static boolean shouldRetainVerifiedPoint(NpcClickSmartQueueOutcome outcome) {
        return outcome == NpcClickSmartQueueOutcome.VERIFIED;
    }

    /**
     * G122 P1-3：defer 的待定点只进 {@link #deferredReplayPoints}，永远进不了已验证表。
     *
     * <p>它存在的唯一理由是五环"临时不可用"重试环显式带 {@code reuseLastVerifiedPoint=true}
     * 回来重放同一下点击（用户口述定案）。任何没有显式索要重放的调用方都读不到它。</p>
     */
    static boolean shouldRetainDeferredPoint(
            TurnNpcArrivalFrameFifoSpec spec, NpcClickSmartQueueOutcome outcome) {
        return spec != null
                && spec.deferDialogVerificationToTask()
                && outcome == NpcClickSmartQueueOutcome.DIALOG_OPEN_UNVERIFIED;
    }

    private long freshStorySequence(
            TurnNpcArrivalFrameFifoSpec spec,
            WindowRuntimeContext runtime,
            long storyAnchor,
            long lastConsumedStorySequence) {
        if (!spec.consumeStoryDialogVisibleEvents()) {
            return 0L;
        }
        long sequence = runtime.getStoryDialogVisibleSequence();
        WindowDialogSnapshot snapshot = runtime.getVisibleDialogSnapshot().orElse(null);
        return sequence > storyAnchor
                && sequence > lastConsumedStorySequence
                && snapshot != null
                && snapshot.getType() == DialogType.STORY
                ? sequence : 0L;
    }

    /**
     * Clears the known post-combat 鬼王 Story entirely on the bound Client before the NPC click.
     * The raw exact-window ROI is matched directly; no image or result is sent to Cloud.
     */
    private boolean dismissGhostKingCompletionStoryIfPresent(String source) {
        int[] rect = coordinateHelper.getScaledRect(250, 345, 529, 143);
        BufferedImage raw = tracker.captureToMemory(
                "ghost-king-pre-accept-complete-story",
                rect[0], rect[1], rect[2], rect[3]);
        BufferedImage template = ImagePreprocessor.pathToBufferedImage(
                GHOST_KING_COMPLETE_STORY_TEMPLATE);
        if (raw == null || template == null) {
            log.warn("Ghost King local completion-story probe unavailable; continue NPC flow: "
                            + "source={} rawPresent={} templatePresent={}",
                    source, raw != null, template != null);
            if (raw != null) {
                raw.flush();
            }
            if (template != null) {
                template.flush();
            }
            return true;
        }

        double[] match;
        try {
            match = ImageFinder.find(raw, template, GHOST_KING_COMPLETE_STORY_THRESHOLD);
            MatchEvidenceStore.save("ghost-king-complete-story", null, raw, template, match);
        } finally {
            raw.flush();
            template.flush();
        }
        if (match == null || match.length < 3) {
            log.info("Ghost King local completion story absent; continue NPC flow: source={}", source);
            return true;
        }

        log.info("Ghost King local completion story matched; dismiss before NPC click: "
                        + "source={} score={} match=({}, {})",
                source, match[2], match[0], match[1]);
        return fastClickKnownSmallStoryDialog(source);
    }

    private boolean fastClickKnownSmallStoryDialog(String source) {
        int[] rect = coordinateHelper.getScaledRect(250, 345, 529, 143);
        Point clickPoint = coordinateHelper.getRandomizedPoint(
                new Point(
                        rect[0] + (rect[2] - rect[0]) / 2,
                        rect[1] + Math.max(0, (rect[3] - rect[1]) - 40)),
                100,
                18);
        return inputSequences.moveAndClickLeft(
                "dialog:fastStoryClick:" + (source == null ? "-" : source),
                clickPoint.x,
                clickPoint.y,
                80,
                350);
    }

    private boolean replaceWithFreshFrame(
            TurnWholeTaskRuntimeArguments arguments,
            TurnNpcArrivalFrameFifoSpec spec,
            WindowNativeBinding binding) {
        byte[] fresh = captureFreshExactFrame(binding);
        if (fresh == null) {
            return false;
        }
        long id = System.nanoTime() & Long.MAX_VALUE;
        if (id == 0L) {
            id = 1L;
        }
        try {
            turnClient.replaceNpcArrivalFrame(
                    spec.tenantId(), spec.deviceId(), spec.windowId(), spec.hwnd(),
                    spec.observationRunId(), spec.businessTaskRunId(), arguments.intentId(),
                    id, id, System.currentTimeMillis(), fresh);
            return true;
        } catch (TurnTransportException failure) {
            return false;
        }
    }

    private byte[] captureFreshExactFrame(WindowNativeBinding binding) {
        BufferedImage frame = tracker.captureToMemory(
                "npc-arrival-fifo-replacement",
                binding.getX(), binding.getY(),
                binding.getX() + WINDOW_WIDTH, binding.getY() + WINDOW_HEIGHT);
        if (frame == null) {
            return null;
        }
        try (ByteArrayOutputStream output = new ByteArrayOutputStream(512 * 1024)) {
            ImageIO.write(frame, "png", output);
            return output.toByteArray();
        } catch (IOException failure) {
            return null;
        } finally {
            frame.flush();
        }
    }

    private void reportOutcomeAsync(
            TurnWholeTaskRuntimeArguments arguments,
            TurnNpcArrivalFrameFifoSpec spec,
            NpcClickSmartQueueMessage message,
            NpcClickSmartQueueOutcome outcome,
            String reason) {
        /*
         * G122 复审返修（2026-08-29 P1）：上报编码正名。此前 wireOutcomeForCloud 把 defer 的
         * DIALOG_OPEN_UNVERIFIED 改回 VERIFIED 上报，云端 enrichArrivalOutcome 随即派生
         * success=true——违反卡上合同门④"无真实 accept.png 不得 VERIFIED"。现在本地终态
         * **原样**上报：defer 待定=DIALOG_OPEN_UNVERIFIED（云端 enrichArrivalOutcome 对 defer
         * 需求的这个编码打 verificationStrength=TASK_PHASE_DEFERRED 且 success=false，
         * registerDeferredPending 同单改为认这套如实编码；五环/天庭
         * confirmArrivalFrameClickMemory 的结算链不变）。天庭/鬼王打戳等待验证出的 VERIFIED
         * 本来就是本地真实证据（任务对话答复戳），不受本次正名影响。
         */
        CompletableFuture.runAsync(() -> {
            try {
                turnClient.reportNpcArrivalFrameOutcome(
                        spec.tenantId(), spec.deviceId(), spec.windowId(), spec.hwnd(),
                        spec.observationRunId(), spec.businessTaskRunId(), arguments.intentId(),
                        message, outcome, reason);
            } catch (TurnTransportException ignored) {
                log.warn("NPC arrival FIFO outcome report failed: windowId={} taskRunId={} intentId={} "
                                + "sessionId={} type={} outcome={} kind={} message={}",
                        spec.windowId(), spec.businessTaskRunId(), arguments.intentId(),
                        message == null ? null : message.getSessionId(),
                        message == null ? null : message.getType(),
                        outcome, ignored.kind(), ignored.getMessage());
            }
        });
    }

    private static NpcClickSmartQueueMessage terminalMessage(
            TurnNpcArrivalFrameFifoSpec spec,
            NpcClickSmartCloudSession session) {
        return NpcClickSmartQueueMessage.builder()
                .type(NpcClickSmartQueueMessage.Type.INVALID)
                .sessionId(session == null ? "" : session.getSessionId())
                .windowId(spec.windowId())
                .taskRunId(spec.businessTaskRunId())
                .decisionId("local-terminal")
                .strategy("INVALID")
                .build();
    }

    /** G125 修 C：只有"云端答了、但会话缺失/过期"这一种拒开才补首帧重试。 */
    private static boolean isSessionMissingRejection(NpcClickSmartCloudSession session) {
        return session != null
                && session.getReason() != null
                && session.getReason().contains("session-missing");
    }

    private static boolean isCurrentSession(
            TurnNpcArrivalFrameFifoSpec spec,
            NpcClickSmartCloudSession session) {
        return session != null
                && session.accepted()
                && sameIdentity(spec.windowId(), session.getWindowId())
                && sameIdentity(spec.businessTaskRunId(), session.getTaskRunId());
    }

    private static boolean isCurrentQueueMessage(
            TurnNpcArrivalFrameFifoSpec spec,
            NpcClickSmartCloudSession session,
            NpcClickSmartQueueMessage message) {
        return message != null
                && session != null
                && sameIdentity(session.getSessionId(), message.getSessionId())
                && sameIdentity(spec.windowId(), message.getWindowId())
                && sameIdentity(spec.businessTaskRunId(), message.getTaskRunId());
    }

    private static boolean sameIdentity(String expected, String actual) {
        return expected != null && actual != null && expected.trim().equals(actual.trim());
    }

    private static boolean sameNativeHandle(String expected, String actual) {
        if (sameIdentity(expected, actual)) {
            return true;
        }
        Long expectedValue = parseNativeHandle(expected);
        Long actualValue = parseNativeHandle(actual);
        return expectedValue != null && expectedValue.equals(actualValue);
    }

    private static Long parseNativeHandle(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim();
        try {
            if (normalized.regionMatches(true, 0, "0x", 0, 2)) {
                return Long.parseUnsignedLong(normalized.substring(2), 16);
            }
            if (normalized.regionMatches(true, 0, "hwnd-", 0, 5)) {
                return Long.parseUnsignedLong(normalized.substring(5), 16);
            }
            return Long.parseUnsignedLong(normalized, 10);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static boolean insideAllowedRegion(
            Point point, TurnNpcArrivalFrameFifoSpec spec) {
        return point != null
                && point.x >= 0 && point.x < WINDOW_WIDTH
                && point.y >= 0 && point.y < WINDOW_HEIGHT
                && point.x >= spec.allowedLeft()
                && point.x < spec.allowedLeft() + spec.allowedWidth()
                && point.y >= spec.allowedTop()
                && point.y < spec.allowedTop() + spec.allowedHeight();
    }

    private static int[] buildCtrlMenuScanRect(
            int testX, int testY, WindowNativeBinding binding) {
        int left = Math.max(binding.getX(), testX - CTRL_MENU_SCAN_W);
        int top = Math.max(binding.getY(), testY - CTRL_MENU_SCAN_H);
        int right = Math.min(binding.getX() + WINDOW_WIDTH, testX + CTRL_MENU_SCAN_W);
        int bottom = Math.min(binding.getY() + WINDOW_HEIGHT, testY + CTRL_MENU_SCAN_H);
        if (right <= left) {
            right = Math.min(binding.getX() + WINDOW_WIDTH, left + CTRL_MENU_SCAN_W);
        }
        if (bottom <= top) {
            bottom = Math.min(binding.getY() + WINDOW_HEIGHT, top + CTRL_MENU_SCAN_H);
        }
        return new int[]{left, top, right, bottom};
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private enum SessionOutcome {
        VERIFIED,
        EXHAUSTED,
        STORY_BLOCKED,
        TERMINAL_FAILURE
    }

    private record ReplayPointKey(
            String tenantId,
            String deviceId,
            String windowId,
            String hwnd,
            String observationRunId,
            String businessTaskRunId,
            String intentId) {

        private static ReplayPointKey from(
                TurnWholeTaskRuntimeArguments arguments,
                TurnNpcArrivalFrameFifoSpec spec) {
            return new ReplayPointKey(
                    spec.tenantId(),
                    spec.deviceId(),
                    spec.windowId(),
                    spec.hwnd(),
                    spec.observationRunId(),
                    spec.businessTaskRunId(),
                    arguments.intentId());
        }
    }

    private record SessionResult(SessionOutcome outcome, long storySequence) {
        private static SessionResult verified() {
            return new SessionResult(SessionOutcome.VERIFIED, 0L);
        }

        private static SessionResult exhausted() {
            return new SessionResult(SessionOutcome.EXHAUSTED, 0L);
        }

        private static SessionResult terminal() {
            return new SessionResult(SessionOutcome.TERMINAL_FAILURE, 0L);
        }
    }
}
