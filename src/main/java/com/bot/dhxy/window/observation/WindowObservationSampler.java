package com.bot.dhxy.window.observation;

import com.bot.dhxy.cloud.turn.protocol.observation.ObservationFact;
import com.bot.dhxy.cloud.turn.protocol.observation.ObservationFactType;
import com.bot.dhxy.cloud.turn.protocol.observation.ObservationAnalysisResult;
import com.bot.dhxy.cloud.turn.protocol.observation.ObservationDialogInterestFact;
import com.bot.dhxy.cloud.turn.protocol.observation.ObservationInterest;
import com.bot.dhxy.cloud.turn.protocol.observation.ObservationKeyEvent;
import com.bot.dhxy.cloud.turn.protocol.observation.ObservationKeyEventType;
import com.bot.dhxy.cloud.turn.protocol.observation.ObservationPathingFact;
import com.bot.dhxy.cloud.turn.protocol.observation.ObservationPathingState;
import com.bot.dhxy.cloud.turn.protocol.observation.ObservationPathingTransition;
import com.bot.dhxy.cloud.turn.protocol.observation.ObservationPathingType;
import com.bot.dhxy.cloud.turn.protocol.observation.ObservationPreparedDialogFact;
import com.bot.dhxy.cloud.turn.protocol.observation.ObservationProtocolValidator;
import com.bot.dhxy.cloud.turn.protocol.observation.ObservationRoi;
import com.bot.dhxy.cloud.turn.local.XinshouCombatLocalMechanics;
import com.bot.dhxy.core.GameClientTracker;
import com.bot.dhxy.model.PlayerCharacter;
import com.bot.dhxy.core.ImageFinder;
import com.bot.dhxy.core.MatchEvidenceStore;
import com.bot.dhxy.input.InputSequences;
import com.bot.dhxy.input.action.InputAction;
import com.bot.dhxy.model.dialog.DialogOperation;
import com.bot.dhxy.model.job.XiuluoGreenChainSchedule;
import com.bot.dhxy.service.DialogService;
import com.bot.dhxy.service.UICleanerService;
import com.bot.dhxy.task.model.TaskType;
import com.bot.dhxy.tools.CoordinateHelper;
import com.bot.dhxy.window.observation.coordread.LocalCoordinateStripReader;
import com.bot.dhxy.window.model.WindowDialogInterest;
import com.bot.dhxy.window.model.WindowNativeBinding;
import com.bot.dhxy.window.model.WindowPathingIntent;
import com.bot.dhxy.window.model.WindowPathingSnapshot;
import com.bot.dhxy.window.model.WindowPathingState;
import com.bot.dhxy.window.model.WindowRole;
import com.bot.dhxy.window.runtime.WindowRuntimeContext;
import com.bot.dhxy.window.runtime.WindowTaskContextHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * Per-window local sampling for the observation runner. A geometry-bearing interest captures and uploads exactly
 * its small exact-HWND window-relative ROI for Cloud recognition. Two geometry-free duties are local mechanics:
 * the pre-battle timer edge and the baseline combat-template state machine. The latter owns the exact-run
 * {@code IN_COMBAT}/{@code COMBAT_EXITED} mechanical edges, including miss hysteresis and the local visible-mini-map
 * fail-closed gate; Cloud only updates the parked task from those edges. The sampler never interprets business
 * phases or publishes ready events. Its only input-producing
 * exceptions are the pre-existing, separately gated local-kanda atomic click path in
 * {@link #sampleXiuluoLocalKanda(long, List)} and G057's exact-intent Ghost King flight assist.
 */
public final class WindowObservationSampler {

    private static final Logger log = LoggerFactory.getLogger(WindowObservationSampler.class);

    /** Cloud-issued interest key for the local pre-battle timer edge duty (no ROI geometry). */
    public static final String INTEREST_PREBATTLE_TIMER = "prebattle-timer";

    /**
     * Local pre-battle timeout threshold; byte-equivalent to the frozen local operation executor's
     * {@code WHOLE_TASK_PRE_BATTLE_TIMEOUT_MARK} predicate (5 minutes from the ordinary pre-battle timer start,
     * pause-compensated by the runtime context itself).
     */
    static final long PRE_BATTLE_TIMEOUT_MS = 300_000L;

    /** Local-kanda probe pacing (mirrors the baseline observer's ~1s tick). */
    static final String KANDA_PROBE_KEY = "xiuluo-local-kanda";
    static final long KANDA_PROBE_PERIOD_MS = 1_000L;
    /**
     * 天庭 option probe pacing. The probe both captures a fresh frame and can click, so it must never
     * ride the raw collect cycle — one second matches the kanda probe it is modelled on.
     */
    static final String TIANTING_PROBE_KEY = "tianting-dialog-option";
    static final long TIANTING_PROBE_PERIOD_MS = 1_000L;

    static final String WUBEI_PREPARE_KEY = "wubei-enter-battle-local-prepare";
    static final long WUBEI_PREPARE_PERIOD_MS = 100L;
    static final String COORDINATE_STRIP_INTEREST = "coordinate-strip";
    static final String PRE_COMBAT_COORDINATE_STRIP_ROI = "pre-combat-coordinate-strip";
    static final String XINSHOU_TRACKER_INTEREST = "xinshou-tracker";
    static final String XINSHOU_DIALOG_INTEREST = "xinshou-dialog";
    static final long XINSHOU_NO_PROGRESS_REFRESH_MS = 10_000L;
    private static final int LOCAL_COMBAT_FRAME_UNSET = -1;
    private static final int LOCAL_COMBAT_FRAME_UNKNOWN = 0;
    private static final int LOCAL_COMBAT_FRAME_WORLD_CONFIRMED = 1;
    private static final int LOCAL_COMBAT_FRAME_COMBAT_CONFIRMED = 2;
    /** One incomplete PrintWindow frame must not fabricate a combat exit. */
    private static final int LOCAL_COMBAT_DUAL_ABSENT_CONFIRMATIONS = 2;
    /*
     * 2026-08-21 用户定案（21:51 修罗队长单人出发事故）：脱战确认后 3 秒内忽略一切进战信号。
     * 战斗收尾过渡画面上战斗模板仍会高分命中（21:46:30.3 实证 0.962），曾抖出
     * 出→假进→出 三条边沿；多出的那封脱战信 2 分钟后在第二场战斗刚开打时被云端消费，
     * 导致回程链在战斗中执行、跳过死亡门与归队等待。真实再进战必然要先移动或点进战
     * 对话框，物理上不可能在 3 秒内完成，冷却窗口内的进战信号只能是过渡残影。
     */
    private static final long LOCAL_COMBAT_REENTRY_COOLDOWN_MS = 3_000L;
    private static final String RUNNER_SOURCE = "window-observation-runner";
    /*
     * 2026-08-20 用户定案:恢复 V1 基线 300ms(commit 49cc45ce 曾改 1500ms,把停下判定从 ~8s
     * 拖到 ~18s——每个静止/消费门都按采样粒度跳动)。数字条 diff 是共享帧内存裁剪,300ms 廉价。
     */
    static final long LOCAL_PATHING_SAMPLE_PERIOD_MS = 300L;
    /** 走路基线预热节拍:寻路 ACTIVE 期间每 4s 解析一次坐标,停下时基线已在手(一轮即判)。 */
    private static final long LOCAL_PATHING_WALKING_BASELINE_REFRESH_MS = 4_000L;
    private static final long LOCAL_PATHING_BASELINE_TIMEOUT_MS = 10_000L;
    static final long LOCAL_PATHING_ARRIVAL_STATIONARY_MS = 600L;
    private static final long LOCAL_PATHING_COORDINATE_PROBE_MIN_INTERVAL_MS = 2_000L;
    private static final long LOCAL_PATHING_STOPPED_AWAY_MS = 2_200L;
    private static final double LOCAL_PATHING_DIFF_RATIO = 0.05D;
    private static final int PATHING_COORDINATE_STRIP_X = 46;
    private static final int PATHING_COORDINATE_STRIP_Y = 59;
    private static final int PATHING_COORDINATE_STRIP_WIDTH = 178;
    private static final int PATHING_COORDINATE_STRIP_HEIGHT = 35;
    /*
     * Movement detection compares only the coordinate digits. The horizontal range is derived from
     * the bracket pair in each complete strip because map names and one-to-three digit coordinates
     * change its absolute position and width. The vertical digit band remains stable in the 178x35
     * strip. Brackets and map-name pixels must never become movement evidence.
     */
    private static final int PATHING_MOVEMENT_DIFF_TOP = 11;
    private static final int PATHING_MOVEMENT_DIFF_HEIGHT = 12;
    private static final int COORD_BRACKET_MIN_WIDTH = 30;
    private static final int COORD_BRACKET_MAX_WIDTH = 80;
    private final WindowRuntimeContext context;
    private final WindowTaskContextHolder contextHolder;
    private final GameClientTracker tracker;
    private final CoordinateHelper coordinateHelper;
    private final DialogService dialogService;
    private final InputSequences inputSequences;
    /** Optional local UI mechanics used by passive scene probes and task-specific cleanup paths. */
    private volatile UICleanerService uiCleanerService;
    private final boolean localKandaEnabled;
    private final LocalCombatSignalMechanics combatSignalMechanics;
    private final XinshouAnchorLocalMechanics xinshouAnchorMechanics;
    private final WuhuanPresenceLocalMechanics wuhuanPresenceMechanics;
    private final UnknownPhasePresenceLocalMechanics unknownPhasePresenceMechanics;
    private final FlyingSaturationLocalMechanics flyingSaturationMechanics;
    private final DialogFramePresenceMechanics dialogFramePresenceMechanics = new DialogFramePresenceMechanics();
    /** Last Cloud-acknowledged content per new-player ROI key; sampler-thread confined. */
    private final Map<String, XinshouRoiVersion> deliveredXinshouRois = new HashMap<>();
    /** Candidate content carried by the current request, committed only after a successful response. */
    private final Map<String, XinshouRoiVersion> sampledXinshouRois = new HashMap<>();
    /** Last content seen by the local tick, independent of Cloud acknowledgement. */
    private final Map<String, String> observedXinshouRoiHashes = new HashMap<>();
    /** Last Cloud-acknowledged new-player fact values; sampler-thread confined. */
    private final Map<ObservationFactType, XinshouFactVersion> deliveredXinshouFacts =
            new EnumMap<>(ObservationFactType.class);
    /** Fact values carried by the current request; committed only after exact sequence acknowledgement. */
    private final Map<ObservationFactType, XinshouFactVersion> sampledXinshouFacts =
            new EnumMap<>(ObservationFactType.class);
    /** Last fact values seen by the local tick, independent of transport delivery. */
    private final Map<ObservationFactType, String> observedXinshouFacts =
            new EnumMap<>(ObservationFactType.class);
    private long lastXinshouEffectiveProgressAtMs;
    private long lastXinshouRefreshAcknowledgedAtMs;
    private boolean xinshouRefreshPending;
    private long xinshouRefreshObserverSeq;
    private boolean localCombatVisible;
    /**
     * In-combat auto-panel watch: shared-frame template match cadence and repair throttles. The
     * watch is a safety net — combat entry already confirmed the panel — so a relaxed 15s period is
     * enough to catch a mid-combat loss without paying a template match every combat tick.
     */
    private static final String AUTO_PANEL_WATCH_KEY = "combat:auto-panel-watch";
    private static final long AUTO_PANEL_WATCH_PERIOD_MS = 15_000L;
    private static final long AUTO_PANEL_REPAIR_SUCCESS_COOLDOWN_MS = 5_000L;
    private static final long AUTO_PANEL_REPAIR_FAILURE_COOLDOWN_MS = 2_000L;
    /** Baseline panel geometry: auto_remaining center -> panel drag grip, and the safe drop point. */
    private static final int AUTO_PANEL_CENTER_OFFSET_X = 43;
    private static final int AUTO_PANEL_CENTER_OFFSET_Y = 28;
    private static final int AUTO_PANEL_SAFE_OFFSET_X = 489;
    private static final int AUTO_PANEL_SAFE_OFFSET_Y = 726;
    private static final double AUTO_PANEL_ALIGN_TOLERANCE_PX = 20.0;
    private XinshouCombatLocalMechanics autoPanelMechanics;
    private BufferedImage autoPanelTemplate;
    private boolean autoPanelTemplateLoadFailed;
    private LocalLeaderCombatBroadcast leaderCombatBroadcast;
    /**
     * G002 W1 client half: whether the Cloud currently wants this window to sample its own combat
     * signal. The Cloud suppresses the {@code combat-signal} interest for members while a locally
     * controlled leader is present; sampling and the shared whole-frame refresh follow that
     * decision instead of running unconditionally. Defaults to true so the pre-interest startup
     * phase keeps legacy self-detection.
     */
    private volatile boolean combatSignalInterestActive = true;
    private final AtomicBoolean autoPanelRepairInFlight = new AtomicBoolean();
    private volatile long autoPanelRepairCooldownUntilMs;
    private volatile long autoPanelAlignedGeneration = -1L;
    private int lastLocalCombatFrameState = LOCAL_COMBAT_FRAME_UNSET;
    private int localCombatDualAbsentStreak;
    private long localCombatGeneration;
    /** 上一次脱战确认时刻；进战冷却门（LOCAL_COMBAT_REENTRY_COOLDOWN_MS）的基准。 */
    private long lastLocalCombatExitConfirmedAtMs;
    private boolean localCombatEntryPublished;
    private com.bot.dhxy.window.model.WindowExpectedCombatEnterClaim boundExpectedCombatClaim;
    private final DeferredReturnHomeReplayCoordinator returnHomeReplayCoordinator;
    private volatile Consumer<ObservationKeyEvent> asyncEventPublisher;
    /** Completion payloads produced by the input worker; exact successful transport ACK removes them. */
    private final Map<String, ObservationRoi> pendingAsyncRois = new ConcurrentHashMap<>();
    /** Fallback mailbox for tests that construct a sampler without a bound runner publisher. */
    private final ConcurrentLinkedQueue<ObservationKeyEvent> pendingAsyncEvents = new ConcurrentLinkedQueue<>();
    /**
     * TURN-40G review#4: the authoritative observation run identity this sampler belongs to (its runner's
     * taskRunId). The local-kanda fast path acts ONLY on a green-chain schedule whose taskRunId equals this
     * value, so an old runner surviving a stop/restart overlap can never consume, click for, or publish on the
     * new run's paired schedule — a mismatched run is a clean no-op (no matcher, no input, no event, no claim).
     */
    private final String taskRunId;
    /** Last sample time per interest key so each interest honors its own Cloud-issued period. */
    private final Map<String, Long> lastSampledAtMs = new HashMap<>();
    /** Last exact identity mirror, retained only to make a later clear/replacement explicit on the wire. */
    private ObservationPathingFact lastPathingFact;
    /** True once the current CLEARED fact was delivered (or Cloud-rejected); stops the resend loop. */
    private boolean clearedPathingFactDelivered;
    /** Last consumed value of the context's recovery-reset generation counter. */
    private long consumedPathingFactResetGeneration;
    /** Last dialog-interest identity, retained only long enough to emit one explicit clear. */
    private ObservationDialogInterestFact lastDialogInterestFact;
    /** Successfully captured coordinate-strip frames; this is transport input, not a recognized position. */
    private long coordinateFramesCaptured;
    /** Coordinate-strip captures/encodes that produced no frame for Cloud recognition. */
    private long coordinateFramesUnavailable;
    /** Current and previous full map-name/coordinate strips, each cropped once from one G002 shared frame. */
    private BufferedImage sharedPositionStripFrame;
    private long sharedPositionStripCapturedAtMs;
    private BufferedImage previousSharedPositionStripFrame;
    private long previousSharedPositionStripCapturedAtMs;
    /** Previous nested coordinate-digit crop for the current local pathing intent. Runner-thread confined. */
    private BufferedImage localPathingFrame;
    private String localPathingIntentId;
    private String terminalCoordinateAcknowledgedIntentId;
    private boolean localPathingCoordinatePending;
    /** 走路基线预热在途标志(独立于终局判定 pending,回包只更新基线不做分类)。 */
    private boolean localPathingBaselinePending;
    private long localPathingBaselineRequestedAtMs;
    private long localPathingCoordinateRequestedChangedAtMs;
    private long localPathingCoordinateRequestedAtMs;
    private long localPathingCoordinateRequestedStableMs;
    private long localPathingCoordinateRequestedIntentAgeMs;
    private long localPathingCoordinateResolvedAtMs;
    private long localPathingArrivalCheckedChangedAtMs;
    private String localPathingRecognizedMapName;
    private Integer localPathingRecognizedX;
    private Integer localPathingRecognizedY;
    private long localPathingRecognizedChangedAtMs;
    private long localPathingLastSampleAtMs;
    private long localPathingLastChangedAtMs;
    private boolean localPathingCoordinateMovementObserved;
    /** Monotonic stationary-evidence generation; advanced on intent/movement/verdict invalidation. */
    private long localPathingGeneration;
    private long nextTerminalFrameId = 1L;
    private TerminalCandidateFrame pendingTerminalFrame;
    private ObservationRoi terminalCoordinateRoi;
    private Long lastTerminalFrameId;
    private Long lastTerminalFrameGeneration;
    private String lastTerminalFrameIntentId;
    // ==== 2026-08-23 停稳事实重设计（五环首批）：数值判稳状态 ====
    /** 进入停稳所需的连续有效同值时长。 */
    private static final long VALUE_STABLE_ENTER_MS = 900L;
    /** 连续不可读多久后明报 STRIP_UNAVAILABLE（并保守撤销既有停稳）。 */
    private static final long VALUE_UNREADABLE_REPORT_MS = 2_000L;
    /** 共享条帧超过此龄仍没有新帧=本拍按不可读处理（成员静默期共享帧可能停更，审查 P1）。 */
    private static final long VALUE_STRIP_FRESH_MS = 1_000L;
    private Integer valueLastX;
    private Integer valueLastY;
    private long valueSameSinceMs;
    private boolean valueStableActive;
    private boolean valueStableFramePending;
    private long valueUnreadableSinceMs;
    /** 上一有效拍之后出现过不可读缺口：停稳计时须重新累计（不可读不推进任何计时）。 */
    private boolean valueHadUnreadableGap;
    /** Last local 五环 presence state; facts are edge/terminal driven, never a recurring image stream. */
    private Boolean observedWuhuanTitlePresent;
    private Boolean observedWuhuanDialogPresent;
    /** Last local completion-story verdict published while the title was absent. */
    private String observedWuhuanCompletionVerdict;
    private String lastWuhuanTitleSnapshotKey;
    private String lastWuhuanDialogInterestId;
    private String lastWuhuanTerminalKey;
    /** Exact Ghost King tracker intent armed after its first in-motion Changshou map-label hit. */
    private String pendingGhostKingChangshouFlightIntentId;
    /** Last intent whose definitive flight state was consumed; prevents repeated Alt+C on later ticks. */
    private String handledGhostKingChangshouFlightIntentId;

    public WindowObservationSampler(WindowRuntimeContext context,
                                    WindowTaskContextHolder contextHolder,
                                    GameClientTracker tracker,
                                    CoordinateHelper coordinateHelper,
                                    DialogService dialogService,
                                    InputSequences inputSequences,
                                    String taskRunId) {
        this(context, contextHolder, tracker, coordinateHelper, dialogService, inputSequences,
                taskRunId, true, new LocalCombatSignalMechanics(tracker, coordinateHelper), null);
    }

    public WindowObservationSampler(WindowRuntimeContext context,
                                    WindowTaskContextHolder contextHolder,
                                    GameClientTracker tracker,
                                    CoordinateHelper coordinateHelper,
                                    DialogService dialogService,
                                    InputSequences inputSequences,
                                    String taskRunId,
                                    boolean localKandaEnabled) {
        this(context, contextHolder, tracker, coordinateHelper, dialogService, inputSequences,
                taskRunId, localKandaEnabled, new LocalCombatSignalMechanics(tracker, coordinateHelper), null);
    }

    WindowObservationSampler(WindowRuntimeContext context,
                             WindowTaskContextHolder contextHolder,
                             GameClientTracker tracker,
                             CoordinateHelper coordinateHelper,
                             DialogService dialogService,
                             InputSequences inputSequences,
                             String taskRunId,
                             boolean localKandaEnabled,
                             LocalCombatSignalMechanics combatSignalMechanics) {
        this(context, contextHolder, tracker, coordinateHelper, dialogService, inputSequences,
                taskRunId, localKandaEnabled, combatSignalMechanics, null);
    }

    WindowObservationSampler(WindowRuntimeContext context,
                             WindowTaskContextHolder contextHolder,
                             GameClientTracker tracker,
                             CoordinateHelper coordinateHelper,
                             DialogService dialogService,
                             InputSequences inputSequences,
                             String taskRunId,
                             boolean localKandaEnabled,
                             LocalCombatSignalMechanics combatSignalMechanics,
                             DeferredReturnHomeReplayCoordinator returnHomeReplayCoordinator) {
        this.context = Objects.requireNonNull(context, "context");
        this.contextHolder = Objects.requireNonNull(contextHolder, "contextHolder");
        this.tracker = Objects.requireNonNull(tracker, "tracker");
        this.coordinateHelper = Objects.requireNonNull(coordinateHelper, "coordinateHelper");
        this.dialogService = Objects.requireNonNull(dialogService, "dialogService");
        this.inputSequences = Objects.requireNonNull(inputSequences, "inputSequences");
        this.taskRunId = requireIdentity(taskRunId, "taskRunId");
        this.localKandaEnabled = localKandaEnabled;
        this.combatSignalMechanics = Objects.requireNonNull(combatSignalMechanics, "combatSignalMechanics");
        this.xinshouAnchorMechanics = new XinshouAnchorLocalMechanics(tracker, coordinateHelper);
        this.wuhuanPresenceMechanics = new WuhuanPresenceLocalMechanics(coordinateHelper);
        this.unknownPhasePresenceMechanics = new UnknownPhasePresenceLocalMechanics(coordinateHelper);
        this.flyingSaturationMechanics = new FlyingSaturationLocalMechanics(coordinateHelper);
        this.returnHomeReplayCoordinator = returnHomeReplayCoordinator;
        this.combatSignalMechanics.bindCycleFrameCropper(this::cropSharedCycleFrame);
        this.xinshouAnchorMechanics.bindCycleFrameCropper(this::cropSharedCycleFrame);
        this.wuhuanPresenceMechanics.bindCycleFrameCropper(this::cropSharedCycleFrame);
        this.unknownPhasePresenceMechanics.bindCycleFrameCropper(this::cropSharedCycleFrame);
        this.flyingSaturationMechanics.bindCycleFrameCropper(this::cropSharedCycleFrame);
    }

    /** Binds passive UI probes and explicit task-owned cleanup; terminal-frame capture never cleans business UI. */
    public void bindUiCleanerService(UICleanerService uiCleanerService) {
        this.uiCleanerService = uiCleanerService;
    }

    /** Enables the in-combat auto-panel watcher; without this the sampler never presses Alt+8 itself. */
    public void bindAutoPanelMechanics(XinshouCombatLocalMechanics autoPanelMechanics) {
        this.autoPanelMechanics = autoPanelMechanics;
    }

    /** Binds the in-process leader combat fan-out (G002 W1); absent in fixtures that predate it. */
    public void bindLeaderCombatBroadcast(LocalLeaderCombatBroadcast leaderCombatBroadcast) {
        this.leaderCombatBroadcast = leaderCombatBroadcast;
    }

    /**
     * Bidirectional auto-panel contract, local half: during combat only the local runner watches the
     * auto-combat panel (matched on the already-captured shared cycle frame — no extra capture), and
     * a lost panel is repaired immediately with one Alt+8 through the shared input queue. Every
     * physical repair is reported upward as one retained {@code AUTO_PANEL_MAINTAINED} key event so
     * the Cloud can reset its remaining-rounds ledger; the Cloud half never matches panel pixels and
     * only commands a refresh from that ledger (entry threshold / five-minute combat).
     */
    private void watchAutoPanelDuringCombat(long now) {
        // The context flag covers both sources of combat truth: this sampler's own signal and the
        // in-process leader broadcast that keeps quiet member windows informed (G002 W1).
        if (autoPanelMechanics == null || !context.isLocalCombatVisible()) {
            return;
        }
        // 2026-08-17 user report: pause must silence the watcher completely — no Alt+8, no drag.
        // The sampler keeps observing through a pause, so the input-side reflexes gate here.
        com.bot.dhxy.window.model.WindowRuntimeStatus runtimeStatus = context.getStatus();
        if (runtimeStatus == com.bot.dhxy.window.model.WindowRuntimeStatus.PAUSED
                || runtimeStatus == com.bot.dhxy.window.model.WindowRuntimeStatus.STOPPING) {
            return;
        }
        if (!isDue(AUTO_PANEL_WATCH_KEY, AUTO_PANEL_WATCH_PERIOD_MS, now)) {
            return;
        }
        BufferedImage frame = sharedCycleFrame;
        if (frame == null) {
            // No shared frame this cycle (busy input lock / capture failure): try again next tick
            // without consuming the watch slot — absence of evidence is not panel absence.
            return;
        }
        markSampled(AUTO_PANEL_WATCH_KEY, now);
        BufferedImage template = autoPanelTemplate();
        if (template == null) {
            return;
        }
        double[] panelMatch = ImageFinder.find(frame, template,
                XinshouCombatLocalMechanics.AUTO_REMAINING_THRESHOLD);
        if (panelMatch != null) {
            maybeAlignAutoPanel(panelMatch);
            return;
        }
        if (now < autoPanelRepairCooldownUntilMs
                || !autoPanelRepairInFlight.compareAndSet(false, true)) {
            return;
        }
        long generation = localCombatGeneration;
        log.warn("[local-runner] auto-combat panel missing on shared frame; repairing with one Alt+8: "
                        + "windowId={} generation={}",
                context.getWindowId(), generation);
        Thread.startVirtualThread(() -> {
            XinshouCombatLocalMechanics.Result result = null;
            RuntimeException failure = null;
            try {
                result = contextHolder.callWith(context,
                        () -> autoPanelMechanics.maintainAutoPanelOnce(false));
            } catch (RuntimeException caught) {
                failure = caught;
            }
            boolean completed = failure == null && result != null
                    && result.status() == XinshouCombatLocalMechanics.Status.COMPLETED;
            autoPanelRepairCooldownUntilMs = System.currentTimeMillis()
                    + (completed
                            ? AUTO_PANEL_REPAIR_SUCCESS_COOLDOWN_MS
                            : AUTO_PANEL_REPAIR_FAILURE_COOLDOWN_MS);
            autoPanelRepairInFlight.set(false);
            if (completed && XinshouCombatLocalMechanics.DETAIL_ALT8_PRESSED.equals(result.detail())) {
                publishAsyncEvent(new ObservationKeyEvent(
                        UUID.randomUUID().toString(),
                        ObservationKeyEventType.AUTO_PANEL_MAINTAINED,
                        System.currentTimeMillis(),
                        null, null, null,
                        "runner-auto-panel-watch",
                        "alt8-after-panel-lost:generation-" + generation));
                log.info("[local-runner] auto-combat panel repaired and reported: windowId={} generation={}",
                        context.getWindowId(), generation);
            } else if (!completed) {
                log.warn("[local-runner] auto-combat panel repair did not complete: windowId={} generation={} "
                                + "status={} failure={}",
                        context.getWindowId(), generation,
                        result == null ? null : result.status(),
                        failure == null ? null : failure.getMessage());
            }
        });
    }

    /**
     * Baseline safe-area alignment, local half: when the visible panel's inferred center has
     * drifted more than 20px from the safe drop point (window base + 489,726), one drag pulls it
     * back — at most once per combat generation, and never concurrently with an Alt+8 repair.
     */
    private void maybeAlignAutoPanel(double[] panelMatch) {
        int[] frameRect = sharedCycleFrameRect;
        long generation = localCombatGeneration;
        if (frameRect == null || generation == autoPanelAlignedGeneration) {
            return;
        }
        int panelX = frameRect[0] + (int) Math.round(panelMatch[0]) + AUTO_PANEL_CENTER_OFFSET_X;
        int panelY = frameRect[1] + (int) Math.round(panelMatch[1]) + AUTO_PANEL_CENTER_OFFSET_Y;
        int targetX = frameRect[0] + AUTO_PANEL_SAFE_OFFSET_X;
        int targetY = frameRect[1] + AUTO_PANEL_SAFE_OFFSET_Y;
        if (Math.hypot(panelX - targetX, panelY - targetY) <= AUTO_PANEL_ALIGN_TOLERANCE_PX) {
            autoPanelAlignedGeneration = generation;
            return;
        }
        if (!autoPanelRepairInFlight.compareAndSet(false, true)) {
            return;
        }
        log.info("[local-runner] auto-combat panel off safe area; dragging back: windowId={} "
                        + "from=({}, {}) to=({}, {}) generation={}",
                context.getWindowId(), panelX, panelY, targetX, targetY, generation);
        Thread.startVirtualThread(() -> {
            boolean dragged = false;
            try {
                dragged = Boolean.TRUE.equals(contextHolder.callWith(context, () ->
                        inputSequences.submitAndWait("battle:dragAutoPanel:watch", List.of(
                                InputAction.dragAndDrop(panelX, panelY, targetX, targetY),
                                InputAction.sleep(500)))));
            } catch (RuntimeException ignored) {
            }
            if (dragged) {
                autoPanelAlignedGeneration = generation;
                log.info("[local-runner] auto-combat panel dragged into safe area: windowId={} generation={}",
                        context.getWindowId(), generation);
            } else {
                log.warn("[local-runner] auto-combat panel drag did not execute: windowId={} generation={}",
                        context.getWindowId(), generation);
            }
            autoPanelRepairInFlight.set(false);
        });
    }

    /**
     * Clears the physical cursor off the dialog option band right before a dialog-identification
     * capture, and only then. Replaces the retired per-request tail sweep (2026-08-17 user
     * decision): the cursor is touched only when it is over THIS window AND inside the zone, and
     * it leaves by the nearest edge in a short 3-step glide — never a cross-screen teleport.
     */
    /** Overlapping windows can ping-pong one physical cursor between their zones; rate limits break the loop. */
    private static final java.util.concurrent.atomic.AtomicLong NUDGE_LAST_GLOBAL_MS =
            new java.util.concurrent.atomic.AtomicLong();
    private volatile long nudgeLastWindowMs;

    void nudgeCursorOutOfDialogZone(String reason) {
        try {
            long now = System.currentTimeMillis();
            if (now - nudgeLastWindowMs < 5_000L) {
                return;
            }
            long lastGlobal = NUDGE_LAST_GLOBAL_MS.get();
            if (now - lastGlobal < 2_500L || !NUDGE_LAST_GLOBAL_MS.compareAndSet(lastGlobal, now)) {
                return;
            }
            nudgeLastWindowMs = now;
            WindowNativeBinding binding = context.getNativeBinding();
            if (binding == null || !binding.hasGeometry()) {
                return;
            }
            java.awt.PointerInfo pointerInfo = java.awt.MouseInfo.getPointerInfo();
            java.awt.Point pointer = pointerInfo == null ? null : pointerInfo.getLocation();
            if (pointer == null
                    || pointer.x < binding.getX() || pointer.y < binding.getY()
                    || pointer.x >= binding.getX() + binding.getWidth()
                    || pointer.y >= binding.getY() + binding.getHeight()) {
                return;
            }
            if (!com.bot.dhxy.input.action.DialogMouseNoParkZone.containsScreenPoint(
                    pointer.x, pointer.y, binding.getX(), binding.getY())) {
                return;
            }
            java.awt.Point exit = com.bot.dhxy.input.action.DialogMouseNoParkZone.nearestExitTarget(
                    pointer.x, pointer.y, binding.getX(), binding.getY());
            List<InputAction> glide = new ArrayList<>();
            for (int step = 1; step <= 3; step++) {
                glide.add(InputAction.moveMouse(
                        pointer.x + (exit.x - pointer.x) * step / 3,
                        pointer.y + (exit.y - pointer.y) * step / 3));
                glide.add(InputAction.sleep(25));
            }
            boolean moved = inputSequences.submitAndWait("dialog-zone-nudge:" + reason, glide);
            log.info("[INPUT_TRACE] dialog-zone nudge: windowId={} reason={} from=({}, {}) to=({}, {}) moved={}",
                    context.getWindowId(), reason, pointer.x, pointer.y, exit.x, exit.y, moved);
        } catch (RuntimeException nudgeFailure) {
            log.debug("dialog-zone nudge skipped: windowId={} reason={} cause={}",
                    context.getWindowId(), reason, nudgeFailure.toString());
        }
    }

    private BufferedImage autoPanelTemplate() {
        if (autoPanelTemplate != null) {
            return autoPanelTemplate;
        }
        if (autoPanelTemplateLoadFailed) {
            return null;
        }
        try {
            autoPanelTemplate = ImageIO.read(
                    java.nio.file.Path.of(XinshouCombatLocalMechanics.AUTO_REMAINING_TEMPLATE_PATH).toFile());
        } catch (IOException ignored) {
            autoPanelTemplate = null;
        }
        if (autoPanelTemplate == null) {
            autoPanelTemplateLoadFailed = true;
            log.warn("[local-runner] auto-panel watch disabled: template unreadable at {}",
                    XinshouCombatLocalMechanics.AUTO_REMAINING_TEMPLATE_PATH);
        }
        return autoPanelTemplate;
    }

    /**
     * G002 shared cycle frame: {@code PrintWindow(PW_RENDERFULLCONTENT)} has no region parameter —
     * every capture forces a full-window render regardless of the requested crop. So each cycle
     * captures the whole window ONCE (via tryLock, never blocking real input) and every local
     * consumer (combat stages, magnifier anchor, pathing strip probe, ROI duties) crops from it in
     * memory. A missing frame (busy lock / capture failure) makes consumers observe UNAVAILABLE.
     */
    private BufferedImage sharedCycleFrame;
    private int[] sharedCycleFrameRect;
    /**
     * G103：当前共享帧的捕获时刻；限频复用与 pace 绝对 deadline 的共同基准。写在 transport
     * 线程（sendOnce 采集）或 runLoop 线程（物理采样），读在 runLoop 的 pace()——volatile。
     * clearSharedCycleFrame 一并清零。
     */
    private volatile long sharedCycleFrameCapturedAtMs;

    /**
     * G103-CR：pace 绝对 deadline 读取口。0 = 当前无共享帧（清屏/捕获失败/静默成员）。
     * 值为 {@link #monotonicMillis()} 时基（nanoTime 派生），与墙钟无关、NTP 跳变免疫。
     */
    long sharedCycleFrameCapturedAtMs() {
        return sharedCycleFrameCapturedAtMs;
    }

    /** G103-CR：采样 deadline 专用单调毫秒钟；只可与同源值相减，绝不能当墙钟用。 */
    static long monotonicMillis() {
        return System.nanoTime() / 1_000_000L;
    }
    /**
     * G103（2026-08-25 用户确认）：Runner 每拍写入的有效采样周期（currentPeriodMs()——战斗 1s /
     * 寻路 300ms / 五倍进战 100ms）。周期内的额外 tick（定点 wake、transport 并行采样）复用
     * 现有共享帧，不再多付一次整窗 PrintWindow 重绘。0 = 不限频（未接线的旧路径/测试保持原行为）。
     */
    private volatile long effectiveSharedFramePeriodMs;

    void setEffectiveSharedFramePeriodMs(long periodMs) {
        this.effectiveSharedFramePeriodMs = periodMs;
    }

    private void refreshSharedCycleFrame() {
        clearSharedCycleFrame();
        int[] rect = coordinateHelper.getScaledRect(0, 0, 1024, 768);
        BufferedImage frame = tracker.captureToMemory(
                "observe:shared-cycle-frame", rect[0], rect[1], rect[2], rect[3]);
        if (frame != null) {
            sharedCycleFrame = frame;
            sharedCycleFrameRect = rect;
            sharedCycleFrameCapturedAtMs = monotonicMillis();
        }
    }

    private void clearSharedCycleFrame() {
        if (sharedCycleFrame != null) {
            sharedCycleFrame.flush();
            sharedCycleFrame = null;
            sharedCycleFrameRect = null;
        }
        sharedCycleFrameCapturedAtMs = 0L;
    }

    /**
     * G002 member-quiet: a member window whose {@code combat-signal} interest the Cloud suppressed
     * refreshes the shared whole-window frame only when the auto-panel watch is about to consume
     * it; every other tick skips the PrintWindow redraw entirely and leaves no stale frame behind.
     * Leaders, solo and role-unresolved windows keep the per-cycle shared frame, as does a member
     * whenever the Cloud republishes its combat interest (leader gone -> self-detection resumes).
     */
    private void refreshSharedFramesIfNeeded(long now) {
        boolean memberQuiet = context.getRole() == WindowRole.MEMBER && !combatSignalInterestActive;
        boolean panelWatchWantsFrame = autoPanelMechanics != null
                && context.isLocalCombatVisible()
                && isDue(AUTO_PANEL_WATCH_KEY, AUTO_PANEL_WATCH_PERIOD_MS, now);
        if (memberQuiet && !panelWatchWantsFrame) {
            clearSharedCycleFrame();
            return;
        }
        /*
         * G103：共享帧按有效采样周期限频。周期内到达的额外 tick 复用现帧——同时跳过位置条带
         * 刷新（同一物理样本不得二次推进停稳/位移判定的前后帧对）。周期边界照常重拍。
         */
        long periodMs = effectiveSharedFramePeriodMs;
        if (periodMs > 0L
                && sharedCycleFrame != null
                && sharedCycleFrameCapturedAtMs > 0L
                && monotonicMillis() - sharedCycleFrameCapturedAtMs < periodMs) {
            return;
        }
        refreshSharedCycleFrame();
        refreshSharedPositionStripFrame(now);
    }

    /**
     * Crops the complete map-name/coordinate strip once for this shared-frame cycle. Movement diff,
     * Cloud location recognition and pre-combat cache refresh must all reuse these exact pixels.
     *
     * @param capturedAtMs wall-clock time of the current shared frame, in epoch milliseconds.
     */
    private void refreshSharedPositionStripFrame(long capturedAtMs) {
        if (previousSharedPositionStripFrame != null) {
            previousSharedPositionStripFrame.flush();
        }
        previousSharedPositionStripFrame = sharedPositionStripFrame;
        previousSharedPositionStripCapturedAtMs = sharedPositionStripCapturedAtMs;
        sharedPositionStripFrame = cropSharedCycleFrame(coordinateHelper.getScaledRect(
                PATHING_COORDINATE_STRIP_X, PATHING_COORDINATE_STRIP_Y,
                PATHING_COORDINATE_STRIP_WIDTH, PATHING_COORDINATE_STRIP_HEIGHT));
        sharedPositionStripCapturedAtMs = sharedPositionStripFrame == null ? 0L : capturedAtMs;
    }

    /**
     * Derives the coordinate-digit movement box from the already-cropped complete position strip.
     * The bracket pair is located from this exact frame; failure returns no movement evidence.
     */
    private static BufferedImage cropMovementDigits(BufferedImage positionStrip) {
        if (positionStrip == null) {
            return null;
        }
        CoordinateBracketSpan span = findCoordinateBracketSpan(positionStrip);
        if (span == null) {
            return null;
        }
        double scaleX = positionStrip.getWidth() / (double) PATHING_COORDINATE_STRIP_WIDTH;
        double scaleY = positionStrip.getHeight() / (double) PATHING_COORDINATE_STRIP_HEIGHT;
        int bracketPadding = Math.max(1, (int) Math.round(2 * scaleX));
        int left = span.leftMaxX() + bracketPadding;
        int rightExclusive = span.rightMinX() - Math.max(1, (int) Math.round(scaleX));
        int top = (int) Math.round(PATHING_MOVEMENT_DIFF_TOP * scaleY);
        int width = rightExclusive - left;
        int height = Math.max(1, (int) Math.round(PATHING_MOVEMENT_DIFF_HEIGHT * scaleY));
        if (left < 0 || top < 0 || width <= 0 || height <= 0
                || left + width > positionStrip.getWidth()
                || top + height > positionStrip.getHeight()) {
            return null;
        }
        return copyImage(positionStrip.getSubimage(left, top, width, height));
    }

    /** Finds the rightmost structurally valid [digits,digits] envelope in the complete strip. */
    private static CoordinateBracketSpan findCoordinateBracketSpan(BufferedImage positionStrip) {
        BufferedImage clean = cleanCoordinateText(positionStrip);
        try {
            List<CoordinateGlyphBox> glyphs = segmentCoordinateGlyphs(clean);
            double scaleX = positionStrip.getWidth() / (double) PATHING_COORDINATE_STRIP_WIDTH;
            double scaleY = positionStrip.getHeight() / (double) PATHING_COORDINATE_STRIP_HEIGHT;
            int maxBracketWidth = Math.max(2, (int) Math.ceil(6 * scaleX));
            int minBracketHeight = Math.max(2, (int) Math.floor(8 * scaleY));
            int maxBracketHeight = Math.max(minBracketHeight, (int) Math.ceil(16 * scaleY));
            int minSpanWidth = Math.max(2, (int) Math.floor(COORD_BRACKET_MIN_WIDTH * scaleX));
            int maxSpanWidth = Math.max(minSpanWidth, (int) Math.ceil(COORD_BRACKET_MAX_WIDTH * scaleX));
            int maxVerticalDelta = Math.max(2, (int) Math.ceil(3 * scaleY));
            List<CoordinateGlyphBox> leftBrackets = glyphs.stream()
                    .filter(box -> box.width() <= maxBracketWidth
                            && box.height() >= minBracketHeight
                            && box.height() <= maxBracketHeight
                            && isBracketShape(clean, box, true))
                    .sorted(Comparator.comparingInt(CoordinateGlyphBox::minX))
                    .toList();
            List<CoordinateGlyphBox> rightBrackets = glyphs.stream()
                    .filter(box -> box.width() <= maxBracketWidth
                            && box.height() >= minBracketHeight
                            && box.height() <= maxBracketHeight
                            && isBracketShape(clean, box, false))
                    .sorted(Comparator.comparingInt(CoordinateGlyphBox::minX))
                    .toList();

            CoordinateBracketSpan best = null;
            for (CoordinateGlyphBox left : leftBrackets) {
                for (CoordinateGlyphBox right : rightBrackets) {
                    if (right.minX() <= left.maxX()) {
                        continue;
                    }
                    int spanWidth = right.maxX() - left.minX() + 1;
                    if (spanWidth < minSpanWidth || spanWidth > maxSpanWidth
                            || Math.abs(left.minY() - right.minY()) > maxVerticalDelta
                            || Math.abs(left.maxY() - right.maxY()) > maxVerticalDelta
                            || !hasCoordinateCommaAndDigits(glyphs, left, right, scaleX, scaleY)) {
                        continue;
                    }
                    CoordinateBracketSpan candidate = new CoordinateBracketSpan(
                            left.minX(), left.maxX(), right.minX(), right.maxX());
                    if (best == null
                            || candidate.rightMinX() > best.rightMinX()
                            || (candidate.rightMinX() == best.rightMinX()
                            && candidate.leftMinX() < best.leftMinX())) {
                        best = candidate;
                    }
                }
            }
            return best;
        } finally {
            clean.flush();
        }
    }

    /** Distinguishes square brackets from narrow map-name strokes and the digit 1. */
    private static boolean isBracketShape(BufferedImage clean,
                                          CoordinateGlyphBox box,
                                          boolean leftBracket) {
        int requiredHorizontal = Math.max(2, (int) Math.ceil(box.width() * 0.66D));
        if (countWhitePixelsOnRow(clean, box.minX(), box.maxX(), box.minY()) < requiredHorizontal
                || countWhitePixelsOnRow(clean, box.minX(), box.maxX(), box.maxY()) < requiredHorizontal) {
            return false;
        }
        int edgeX = leftBracket ? box.minX() : box.maxX();
        int oppositeX = leftBracket ? box.maxX() : box.minX();
        int requiredVertical = Math.max(2, (int) Math.ceil(box.height() * 0.70D));
        int allowedOpposite = Math.max(2, (int) Math.ceil(Math.max(1, box.height() - 2) * 0.35D));
        return countWhitePixelsOnColumn(clean, edgeX, box.minY(), box.maxY()) >= requiredVertical
                && countWhitePixelsOnColumn(clean, oppositeX, box.minY() + 1, box.maxY() - 1)
                <= allowedOpposite;
    }

    private static int countWhitePixelsOnRow(BufferedImage clean, int fromX, int toX, int y) {
        int count = 0;
        for (int x = fromX; x <= toX; x++) {
            if ((clean.getRGB(x, y) & 0xFFFFFF) == 0xFFFFFF) {
                count++;
            }
        }
        return count;
    }

    private static int countWhitePixelsOnColumn(BufferedImage clean, int x, int fromY, int toY) {
        int count = 0;
        for (int y = fromY; y <= toY; y++) {
            if ((clean.getRGB(x, y) & 0xFFFFFF) == 0xFFFFFF) {
                count++;
            }
        }
        return count;
    }

    private static boolean hasCoordinateCommaAndDigits(List<CoordinateGlyphBox> glyphs,
                                                       CoordinateGlyphBox left,
                                                       CoordinateGlyphBox right,
                                                       double scaleX,
                                                       double scaleY) {
        int maxCommaWidth = Math.max(1, (int) Math.ceil(4 * scaleX));
        int maxCommaHeight = Math.max(2, (int) Math.ceil(5 * scaleY));
        int commaMinY = left.minY() + Math.max(2, (int) Math.floor(6 * scaleY));
        for (CoordinateGlyphBox comma : glyphs) {
            if (comma.minX() <= left.maxX() || comma.maxX() >= right.minX()
                    || comma.minY() < commaMinY
                    || comma.width() > maxCommaWidth || comma.height() > maxCommaHeight) {
                continue;
            }
            boolean hasLeftDigits = glyphs.stream().anyMatch(box ->
                    box.minX() > left.maxX() && box.maxX() < comma.minX() && box.height() >= 2);
            boolean hasRightDigits = glyphs.stream().anyMatch(box ->
                    box.minX() > comma.maxX() && box.maxX() < right.minX() && box.height() >= 2);
            if (hasLeftDigits && hasRightDigits) {
                return true;
            }
        }
        return false;
    }

    private static BufferedImage cleanCoordinateText(BufferedImage source) {
        BufferedImage clean = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_BYTE_BINARY);
        for (int y = 0; y < source.getHeight(); y++) {
            for (int x = 0; x < source.getWidth(); x++) {
                int rgb = source.getRGB(x, y);
                int red = (rgb >> 16) & 0xFF;
                int green = (rgb >> 8) & 0xFF;
                int blue = rgb & 0xFF;
                int max = Math.max(red, Math.max(green, blue));
                int min = Math.min(red, Math.min(green, blue));
                float[] hsb = Color.RGBtoHSB(red, green, blue, null);
                boolean coordinatePixel = max >= 145 && min >= 100
                        && hsb[1] <= 0.32F && hsb[2] >= 0.56F && max - min <= 85;
                clean.setRGB(x, y, coordinatePixel ? 0xFFFFFF : 0x000000);
            }
        }
        return clean;
    }

    private static List<CoordinateGlyphBox> segmentCoordinateGlyphs(BufferedImage clean) {
        boolean[][] visited = new boolean[clean.getHeight()][clean.getWidth()];
        List<CoordinateGlyphBox> glyphs = new ArrayList<>();
        for (int y = 0; y < clean.getHeight(); y++) {
            for (int x = 0; x < clean.getWidth(); x++) {
                if (visited[y][x] || (clean.getRGB(x, y) & 0xFFFFFF) != 0xFFFFFF) {
                    continue;
                }
                CoordinateGlyphBox box = floodFillCoordinateGlyph(clean, visited, x, y);
                if (box.width() <= 18 && box.height() <= 18 && box.pixelCount() >= 2) {
                    glyphs.add(box);
                }
            }
        }
        glyphs.sort(Comparator.comparingInt(CoordinateGlyphBox::minX));
        return glyphs;
    }

    private static CoordinateGlyphBox floodFillCoordinateGlyph(BufferedImage clean,
                                                               boolean[][] visited,
                                                               int startX,
                                                               int startY) {
        List<int[]> points = new ArrayList<>();
        points.add(new int[]{startX, startY});
        visited[startY][startX] = true;
        int minX = startX;
        int minY = startY;
        int maxX = startX;
        int maxY = startY;
        for (int i = 0; i < points.size(); i++) {
            int[] point = points.get(i);
            minX = Math.min(minX, point[0]);
            minY = Math.min(minY, point[1]);
            maxX = Math.max(maxX, point[0]);
            maxY = Math.max(maxY, point[1]);
            for (int direction = 0; direction < 4; direction++) {
                int nextX = point[0] + (direction == 0 ? 1 : direction == 1 ? -1 : 0);
                int nextY = point[1] + (direction == 2 ? 1 : direction == 3 ? -1 : 0);
                if (nextX < 0 || nextY < 0 || nextX >= clean.getWidth() || nextY >= clean.getHeight()
                        || visited[nextY][nextX]
                        || (clean.getRGB(nextX, nextY) & 0xFFFFFF) != 0xFFFFFF) {
                    continue;
                }
                visited[nextY][nextX] = true;
                points.add(new int[]{nextX, nextY});
            }
        }
        return new CoordinateGlyphBox(minX, minY, maxX, maxY, points.size());
    }

    BufferedImage cropSharedCycleFrame(int[] scaledRect) {
        BufferedImage frame = sharedCycleFrame;
        int[] full = sharedCycleFrameRect;
        if (frame == null || full == null || scaledRect == null) {
            return null;
        }
        int left = Math.max(0, scaledRect[0] - full[0]);
        int top = Math.max(0, scaledRect[1] - full[1]);
        int right = Math.min(frame.getWidth(), scaledRect[2] - full[0]);
        int bottom = Math.min(frame.getHeight(), scaledRect[3] - full[1]);
        int width = right - left;
        int height = bottom - top;
        if (width <= 0 || height <= 0) {
            return null;
        }
        // Deep copy: template matching converts rasters assuming zero offset, and consumers flush.
        BufferedImage copy = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        java.awt.Graphics2D g = copy.createGraphics();
        g.drawImage(frame, 0, 0, width, height, left, top, right, bottom, null);
        g.dispose();
        return copy;
    }

    void bindAsyncEventPublisher(Consumer<ObservationKeyEvent> publisher) {
        this.asyncEventPublisher = Objects.requireNonNull(publisher, "publisher");
    }

    private static String requireIdentity(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must be a nonblank identity");
        }
        return value;
    }

    /**
     * A production schedule carries the explicit observation-run id acknowledged for this sampler. The exact
     * business task id is retained separately for Cloud ownership and diagnostics; it is never parsed here.
     * The legacy equality branch exists only for old in-process fixtures that used one id for both planes.
     */
    private boolean scheduleBelongsToThisRun(XiuluoGreenChainSchedule schedule) {
        return schedule != null
                && ((schedule.getObservationRunId() != null
                        && schedule.getObservationRunId().equals(taskRunId))
                    || (schedule.getObservationRunId() == null
                        && taskRunId.equals(schedule.getTaskRunId())))
                && context.getWindowId().equals(schedule.getWindowId());
    }

    /** One collected batch: mechanical facts, key edges and interest-selected ROI frames. */
    public record SampleBatch(List<ObservationPathingFact> pathingFacts,
                              List<ObservationDialogInterestFact> dialogInterests,
                              List<ObservationPreparedDialogFact> preparedDialogs,
                              List<ObservationFact> facts,
                              List<ObservationKeyEvent> events,
                              List<ObservationRoi> rois,
                              List<TerminalCandidateFrame> terminalFrames) {
    }

    /**
     * Identity-light terminal frame captured by the bound sampler. The runner adds transport identity
     * (tenant/device/window/hwnd/taskRun) without touching or re-encoding these exact PNG bytes.
     */
    public record TerminalCandidateFrame(
            long frameId,
            long pathingGeneration,
            String intentId,
            int width,
            int height,
            long capturedAtMs,
            byte[] pngBytes) {
    }

    /**
     * Executes every due Cloud-issued interest once plus the local-kanda duty (whose gate is the window's own
     * registered xiuluo dialog interest, not a Cloud observation interest). ROI duties yield no frame on capture
     * failure; combat capture or template failure is {@code UNAVAILABLE} and therefore cannot count as a local
     * combat miss or produce an exit edge.
     */
    public SampleBatch collect(List<ObservationInterest> interests) {
        return collect(interests, 0L, false, false);
    }

    public SampleBatch collect(List<ObservationInterest> interests, long observerSeq) {
        return collect(interests, observerSeq, false, false);
    }

    /**
     * Collects one exact-window observation batch.
     *
     * @param interests Cloud-issued observation duties; never interpreted as local business permission.
     * @param observerSeq monotonic sequence that will identify the outgoing observation request.
     * @param startupWuhuanPresence true only for a newly acknowledged 五环 run's first screen observation; it
     *                               enables the existing read-only title/dialog presence facts before Cloud has
     *                               re-issued its normal interests.
     * @return mechanical facts, events and requested evidence from this one shared-frame cycle.
     */
    public SampleBatch collect(List<ObservationInterest> interests,
                               long observerSeq,
                               boolean startupWuhuanPresence) {
        return collect(interests, observerSeq, false, startupWuhuanPresence);
    }

    /**
     * Collects one exact-window batch with independent startup duties.
     *
     * @param interests Cloud-issued observation duties; never interpreted as local business permission.
     * @param observerSeq monotonic sequence that will identify the outgoing observation request.
     * @param startupCombatObservation true while the runner owes Cloud its startup combat state and, after a
     *                                 VISIBLE result, the first ABSENT exit frame; UNAVAILABLE proves neither.
     * @param startupWuhuanPresence true only for a newly acknowledged 五环 run's first screen observation.
     * @return mechanical facts, events and requested evidence from this one shared-frame cycle.
     */
    public SampleBatch collect(List<ObservationInterest> interests,
                               long observerSeq,
                               boolean startupCombatObservation,
                               boolean startupWuhuanPresence) {
        List<ObservationInterest> safeInterests = interests == null ? List.of() : interests;
        return contextHolder.callWith(context, () -> collectBound(
                safeInterests, observerSeq, startupCombatObservation, startupWuhuanPresence));
    }

    /**
     * Continues exact-window physical observation while an earlier HTTP batch is still in flight.
     *
     * <p>This lane never consumes Cloud interest slots and never creates a second transport request.
     * It only advances local combat/pathing state, local-template actions and critical event edges;
     * those edges enter the runner's retained event mailbox through {@link #publishAsyncEvent}.</p>
     *
     * @param observerSeq latest assigned observation sequence, used only for local diagnostics
     */
    void collectPhysicalStateOnly(long observerSeq) {
        contextHolder.runWith(context, () -> {
            long now = System.currentTimeMillis();
            refreshSharedFramesIfNeeded(now);
            sampleGhostKingChangshouFlightAssist();
            armGhostKingChangshouFlightAssistFromMovingFrame();
            List<ObservationKeyEvent> events = new ArrayList<>();
            List<ObservationRoi> ignoredRois = new ArrayList<>();
            if (localKandaEnabled && isDue(KANDA_PROBE_KEY, KANDA_PROBE_PERIOD_MS, now)) {
                markSampled(KANDA_PROBE_KEY, now);
                sampleXiuluoLocalKanda(now, events, ignoredRois);
            }
            if (isDue(TIANTING_PROBE_KEY, TIANTING_PROBE_PERIOD_MS, now)) {
                markSampled(TIANTING_PROBE_KEY, now);
                sampleTiantingDialogProbe(now, events, ignoredRois);
            }
            refreshLocalPathingTerminal(now);
            samplePreBattleTimerEdge(now, events);
            if (combatSignalInterestActive
                    && isDue(LocalCombatSignalMechanics.INTEREST_KEY,
                    LocalCombatSignalMechanics.SAMPLE_PERIOD_MS, now)) {
                markSampled(LocalCombatSignalMechanics.INTEREST_KEY, now);
                LocalCombatSignalMechanics.Signal signal = combatSignalMechanics.sample();
                observeLocalCombatTransition(signal, now, observerSeq, events);
            }
            watchAutoPanelDuringCombat(now);
            events.forEach(this::publishAsyncEvent);
        });
    }

    /** Whether the frozen 100ms WUBEI_ENTER_BATTLE preparation cadence currently applies. */
    boolean hasActiveWubeiEnterBattleInterest() {
        WindowDialogInterest interest = context.getDialogInterest().orElse(null);
        return interest != null
                && interest.getTaskType() == TaskType.WUBEI
                && interest.getOperations() != null
                && interest.getOperations().contains(DialogOperation.WUBEI_ENTER_BATTLE);
    }

    /** Whether this exact window currently needs the local CR142 pathing cadence. */
    boolean hasActivePathingIntent() {
        WindowPathingSnapshot snapshot = context.getPathingSnapshot();
        return snapshot != null
                && snapshot.getIntent() != null
                && (snapshot.getState() == WindowPathingState.ACTIVE
                || snapshot.getState() == WindowPathingState.UNKNOWN
                // 数值判稳新态也算"腿还活着"：场景探针缓存要继续保鲜，否则 STABLE 期间
                // 到达帧清障会拿 12 秒陈缓存判 blocked、反复 cleanUpAll（审查修正）。
                || snapshot.getState() == WindowPathingState.STABLE
                || snapshot.getState() == WindowPathingState.STRIP_UNAVAILABLE);
    }

    private SampleBatch collectBound(List<ObservationInterest> interests,
                                     long observerSeq,
                                     boolean startupCombatObservation,
                                     boolean startupWuhuanPresence) {
        long now = System.currentTimeMillis();
        // Interest-driven (G002): the Cloud publishes what it wants sampled. A fully suppressed
        // member arrives here with an EMPTY interest list, so empty must mean quiet, not legacy-on.
        combatSignalInterestActive = startupCombatObservation
                || interests.stream().anyMatch(
                        i -> LocalCombatSignalMechanics.INTEREST_KEY.equals(i.interestKey()));
        refreshSharedFramesIfNeeded(now);
        // Consume only an arm created by an earlier shared-frame tick. The current frame may arm below.
        sampleGhostKingChangshouFlightAssist();
        armGhostKingChangshouFlightAssistFromMovingFrame();
        boolean forceXinshouRefresh = beginXinshouObservationCycle(interests, now);
        List<ObservationFact> facts = new ArrayList<>();
        List<ObservationKeyEvent> events = new ArrayList<>();
        List<ObservationRoi> rois = new ArrayList<>();
        ObservationKeyEvent completedInputEvent;
        while ((completedInputEvent = pendingAsyncEvents.poll()) != null) {
            events.add(completedInputEvent);
        }
        rois.addAll(pendingAsyncRois.values());
        List<TerminalCandidateFrame> terminalFrames = new ArrayList<>(1);
        // Local kanda owns first refusal for this observation cycle. A successful click publishes its claim in
        // the dialog fact below and prevents a stale Cloud ROI from being captured in the same request.
        if (localKandaEnabled && isDue(KANDA_PROBE_KEY, KANDA_PROBE_PERIOD_MS, now)) {
            markSampled(KANDA_PROBE_KEY, now);
            try {
                sampleXiuluoLocalKanda(now, events, rois);
            } catch (RuntimeException kandaFailure) {
                // A swallowed probe exception is indistinguishable from a template miss in a live run.
                // Keep this at INFO until the local-kanda path is stable; no synthetic fact is emitted.
                log.info("Local-kanda probe failed (no fact fabricated): windowId={} type={} message={}",
                        context.getWindowId(), kandaFailure.getClass().getName(), kandaFailure.getMessage(),
                        kandaFailure);
            }
        }
        try {
            if (isDue(TIANTING_PROBE_KEY, TIANTING_PROBE_PERIOD_MS, now)) {
                markSampled(TIANTING_PROBE_KEY, now);
                // nudge disabled 2026-08-17: misplaced hook fired for ALL tasks/windows every cycle,
                // flooding the focused input queue (starved real dialog clicks + focused idle windows).
                // nudgeCursorOutOfDialogZone("tianting-dialog-probe");
                sampleTiantingDialogProbe(now, events, rois);
            }
        } catch (RuntimeException probeFailure) {
            log.debug("Task-local dialog probe failed (no fact fabricated): windowId={} message={}",
                    context.getWindowId(), probeFailure.getMessage());
        }
        refreshLocalPathingTerminal(now);
        List<ObservationPathingFact> pathingFacts = sampleCurrentPathingFact();
        String pathingIntentId = pathingFacts.isEmpty() ? null : pathingFacts.getFirst().intentId();
        sampleTerminalCoordinateFrame(pathingFacts, rois, terminalFrames);
        sampleWalkingBaselineCoordinateRoi(pathingFacts, rois, now);
        List<ObservationDialogInterestFact> dialogInterests = sampleCurrentDialogInterestFact();
        List<ObservationPreparedDialogFact> preparedDialogs =
                sampleWubeiEnterBattlePrepared(dialogInterests, observerSeq, now);
        if (combatSignalInterestActive
                && isDue(LocalCombatSignalMechanics.INTEREST_KEY, LocalCombatSignalMechanics.SAMPLE_PERIOD_MS, now)) {
            markSampled(LocalCombatSignalMechanics.INTEREST_KEY, now);
            try {
                LocalCombatSignalMechanics.Signal signal = combatSignalMechanics.sample();
                observeLocalCombatTransition(signal, now, observerSeq, events);
                if (startupCombatObservation
                        || interests.stream().anyMatch(
                                i -> LocalCombatSignalMechanics.INTEREST_KEY.equals(i.interestKey()))) {
                    facts.add(new ObservationFact(ObservationFactType.COMBAT_SIGNAL, signal.wireValue(), now));
                }
            } catch (RuntimeException sampleFailure) {
                log.debug("Local combat mechanics failed (no fact fabricated): windowId={} message={}",
                        context.getWindowId(), sampleFailure.getMessage());
            }
        }
        watchAutoPanelDuringCombat(now);
        sampleWuhuanPresence(interests, dialogInterests, pathingFacts, events, observerSeq, now, facts, rois,
                startupWuhuanPresence);
        sampleUnknownPhasePresence(interests, now, facts, rois);
        for (ObservationInterest interest : interests) {
            // The two 天庭 daily-count interests are fulfilled exactly by the shared/fresh frames
            // retained in sampleTiantingDialogProbe. Sampling them again here would add captures
            // after the dialog is already closed and would overwrite the paired evidence.
            if ("tianting-daily-count-first".equals(interest.interestKey())
                    || "tianting-daily-count-fresh".equals(interest.interestKey())
                    || WuhuanPresenceLocalMechanics.TITLE_INTEREST.equals(interest.interestKey())
                    || WuhuanPresenceLocalMechanics.DIALOG_INTEREST.equals(interest.interestKey())
                    || UnknownPhasePresenceLocalMechanics.TITLE_INTEREST.equals(interest.interestKey())
                    || UnknownPhasePresenceLocalMechanics.DIALOG_INTEREST.equals(interest.interestKey())) {
                continue;
            }
            if (!isDue(interest.interestKey(), interest.samplePeriodMs(), now)) {
                continue;
            }
            try {
                if (INTEREST_PREBATTLE_TIMER.equals(interest.interestKey())) {
                    samplePreBattleTimerEdge(now, events);
                    markSampled(interest.interestKey(), now);
                } else if (FlyingSaturationLocalMechanics.TIANTING_DARK_THUNDER_INTEREST_KEY.equals(
                        interest.interestKey())
                        && context.getSelectedTaskType() == TaskType.TIANTING) {
                    // This is deliberately interest-driven. Ordinary 天庭 frames never pay the
                    // saturation cost; only a Cloud-confirmed 暗雷 attempt asks for this fact.
                    FlyingSaturationLocalMechanics.Sample sample = flyingSaturationMechanics.sample();
                    facts.add(new ObservationFact(
                            ObservationFactType.TIANTING_DARK_THUNDER_FLIGHT_STATE,
                            Double.isFinite(sample.meanSaturation())
                                    ? Double.toString(sample.meanSaturation())
                                    : "unavailable",
                            now));
                    markSampled(interest.interestKey(), now);
                    log.info("Tianting dark-thunder flight saturation queued: windowId={} taskRunId={} "
                                    + "observerSeq={} state={} meanSaturation={} pixels={} detail={}",
                            context.getWindowId(), taskRunId, observerSeq, sample.state(),
                            Double.isFinite(sample.meanSaturation())
                                    ? String.format(java.util.Locale.ROOT, "%.4f", sample.meanSaturation())
                                    : "unavailable",
                            sample.pixelCount(), interest.detail());
                } else if (XinshouAnchorLocalMechanics.INTEREST_KEY.equals(interest.interestKey())) {
                    XinshouAnchorLocalMechanics.AnchorSample xinshou = xinshouAnchorMechanics.sample();
                    sampleXinshouFact(observerSeq,
                            ObservationFactType.XINSHOU_ANCHOR,
                            xinshou.primaryAnchor() == null ? "absent" : xinshou.primaryAnchor(),
                            now, facts);
                    sampleXinshouFact(observerSeq,
                            ObservationFactType.XINSHOU_ESC_VISIBLE,
                            xinshou.escVisible() ? "present" : "absent",
                            now, facts);
                    sampleXinshouFact(observerSeq,
                            ObservationFactType.XINSHOU_SKIP_VISIBLE,
                            xinshou.skipVisible() ? "present" : "absent",
                            now, facts);
                    sampleXinshouFact(observerSeq,
                            ObservationFactType.XINSHOU_ESC_BOT,
                            xinshou.escBotVisible() ? "present" : "absent",
                            now, facts);
                    sampleXinshouFact(observerSeq,
                            ObservationFactType.XINSHOU_ADOPTION,
                            xinshou.adoptionTarget() != null ? "present" : "absent",
                            now, facts);
                    String recoveryStatus = xinshou.recoveryTarget() == null
                            ? "absent"
                            : switch (xinshou.recoveryTarget().templateName()) {
                                case "quedingguan_.png" -> "quedingguan_.png";
                                case "confirm.png" -> "confirm.png";
                                default -> "absent";
                            };
                    sampleXinshouFact(observerSeq,
                            ObservationFactType.XINSHOU_RECOVERY_STATUS,
                            recoveryStatus, now, facts);
                    markSampled(interest.interestKey(), now);
                } else if (!LocalCombatSignalMechanics.INTEREST_KEY.equals(interest.interestKey())
                        && interest.hasRoi()) {
                    ObservationDialogInterestFact dialogInterest =
                            dialogInterests.isEmpty() ? null : dialogInterests.getFirst();
                    // A cycle whose shared frame was unavailable produced no ROI at all, so it must not
                    // consume this interest's slot: the next cycle retries immediately instead of going
                    // blind for another full period.
                    if (sampleRoi(interest, dialogInterest, pathingIntentId,
                            observerSeq, now, facts, rois, forceXinshouRefresh)) {
                        markSampled(interest.interestKey(), now);
                    }
                } else {
                    markSampled(interest.interestKey(), now);
                }
                // An unknown geometry-free interest is ignored: this sampler executes only duties it truthfully
                // owns and never fabricates a sample for an unrecognized key.
            } catch (RuntimeException sampleFailure) {
                // A throwing duty still consumes its slot: retrying it every cycle would only repeat the
                // same failure at the observation cadence.
                markSampled(interest.interestKey(), now);
                log.debug("Observation sample failed (no fact fabricated): windowId={} interest={} message={}",
                        context.getWindowId(), interest.interestKey(), sampleFailure.getMessage());
            }
        }
        return new SampleBatch(pathingFacts, dialogInterests, preparedDialogs,
                List.copyOf(facts), List.copyOf(events), List.copyOf(rois),
                List.copyOf(terminalFrames));
    }

    /**
     * Samples only local 五环 presence mechanics. A title frame is emitted with the fact that made it
     * relevant (first/changed title, a new pathing terminal, or a combat edge); it is never an
     * independently scheduled Tracker upload. Dialog pixels never leave here: Cloud must issue an
     * exact one-shot prepared-frame demand after receiving the presence fact.
     */
    private void sampleWuhuanPresence(List<ObservationInterest> interests,
                                      List<ObservationDialogInterestFact> dialogInterests,
                                      List<ObservationPathingFact> pathingFacts,
                                      List<ObservationKeyEvent> events,
                                      long observerSeq,
                                      long now,
                                      List<ObservationFact> facts,
                                      List<ObservationRoi> rois,
                                      boolean startupScreenObservation) {
        ObservationInterest titleInterest = interests.stream()
                .filter(interest -> WuhuanPresenceLocalMechanics.TITLE_INTEREST.equals(interest.interestKey()))
                .findFirst()
                .orElse(null);
        boolean titleRequested = titleInterest != null || startupScreenObservation;
        String titleSnapshotKey = titleInterest == null ? null : titleInterest.detail();
        boolean newTitleSnapshot = titleSnapshotKey != null
                && !Objects.equals(titleSnapshotKey, lastWuhuanTitleSnapshotKey);
        boolean dialogRequested = interests.stream().anyMatch(interest ->
                WuhuanPresenceLocalMechanics.DIALOG_INTEREST.equals(interest.interestKey()))
                || startupScreenObservation;
        boolean terminalEdge = observeWuhuanTerminal(pathingFacts);
        boolean combatEdge = events.stream().anyMatch(event ->
                event.eventType() == ObservationKeyEventType.IN_COMBAT
                        || event.eventType() == ObservationKeyEventType.COMBAT_EXITED);
        String dialogInterestId = dialogInterests.stream()
                .filter(ObservationDialogInterestFact::active)
                .map(ObservationDialogInterestFact::interestId)
                .findFirst()
                .orElse(null);
        boolean newDialogInterest = dialogInterestId != null
                && !Objects.equals(dialogInterestId, lastWuhuanDialogInterestId);
        // A pathing terminal is an exact Cloud-visible transition. Do not let a title sample from
        // the preceding sub-second tick defer its one required terminal-frame observation.
        boolean sampleTitle = titleRequested && (startupScreenObservation || newTitleSnapshot || terminalEdge || isDue(
                WuhuanPresenceLocalMechanics.TITLE_INTEREST,
                WuhuanPresenceLocalMechanics.SAMPLE_PERIOD_MS,
                now));
        boolean sampleDialog = dialogRequested && (startupScreenObservation || newDialogInterest || terminalEdge || isDue(
                WuhuanPresenceLocalMechanics.DIALOG_INTEREST,
                WuhuanPresenceLocalMechanics.SAMPLE_PERIOD_MS,
                now));
        if (!sampleTitle && !sampleDialog) {
            return;
        }

        WuhuanPresenceLocalMechanics.Sample sample = wuhuanPresenceMechanics.sample(sampleTitle, sampleDialog);
        if (sample.titleSampled()) {
            boolean changed = observedWuhuanTitlePresent == null
                    || observedWuhuanTitlePresent != sample.titlePresent();
            observedWuhuanTitlePresent = sample.titlePresent();
            if (newTitleSnapshot) {
                lastWuhuanTitleSnapshotKey = titleSnapshotKey;
            }
            if (!sample.titlePresent() && changed) {
                WuhuanTitleMatchMissDump.write(sample.trackerMissPng(), context.getWindowId(), taskRunId,
                        observerSeq, sample.titleScore());
            }
            if (changed || newTitleSnapshot || terminalEdge || combatEdge) {
                facts.add(new ObservationFact(
                        ObservationFactType.WUHUAN_TITLE_PRESENCE,
                        sample.titlePresent() ? "present" : "absent",
                        now));
                log.info("Wuhuan title presence queued: windowId={} taskRunId={} observerSeq={} snapshotKey={} newSnapshot={} present={} score={} terminalEdge={} combatEdge={}",
                        context.getWindowId(), taskRunId, observerSeq, titleSnapshotKey,
                        newTitleSnapshot, sample.titlePresent(), sample.titleScore(), terminalEdge, combatEdge);
                if (sample.titlePresent() && sample.trackerPng() != null) {
                    rois.add(new ObservationRoi(
                            WuhuanPresenceLocalMechanics.TITLE_FRAME_ROI,
                            0,
                            100,
                            280,
                            604,
                            sample.trackerPng(),
                            "wuhuan-title-event:" + observerSeq,
                            pathingFacts.isEmpty() ? null : pathingFacts.getFirst().intentId(),
                            null,
                            null));
                }
            }
            // Completion verdict travels with the title fact stream: matched locally in the dialog
            // ROI whenever the title sampled absent, so Cloud never round-trips a frame to learn it.
            if (sample.titlePresent()) {
                observedWuhuanCompletionVerdict = null;
            } else if (sample.completionVerdict() != null) {
                boolean completionChanged =
                        !Objects.equals(observedWuhuanCompletionVerdict, sample.completionVerdict());
                observedWuhuanCompletionVerdict = sample.completionVerdict();
                /*
                 * 2026-08-18 19:33 完成误判事故:完成事实曾是边沿上报(值不变不发),而云端
                 * resolveCompletionAfterTitleGone 只认判定窗入口前后新鲜采出的事实——故事早弹、
                 * 边沿事实超龄被滤,窗内又无新事实,连续 completion undecidable→FAILED→重启后
                 * 误去重接任务。完成结论改为电平上报:title 不在且本地判到结论,每次采样(1s)
                 * 都发,仅完成窗口期间有流量,云端判定窗内永远有新鲜事实。
                 */
                facts.add(new ObservationFact(
                        ObservationFactType.WUHUAN_COMPLETION_PRESENCE,
                        sample.completionVerdict(),
                        now));
                if (completionChanged) {
                    log.info("Wuhuan completion presence queued: windowId={} taskRunId={} observerSeq={} verdict={}",
                            context.getWindowId(), taskRunId, observerSeq, sample.completionVerdict());
                }
            }
            markSampled(WuhuanPresenceLocalMechanics.TITLE_INTEREST, now);
        }
        if (sample.dialogSampled()) {
            boolean changed = observedWuhuanDialogPresent == null
                    || observedWuhuanDialogPresent != sample.dialogPresent();
            observedWuhuanDialogPresent = sample.dialogPresent();
            if (newDialogInterest) {
                lastWuhuanDialogInterestId = dialogInterestId;
            }
            if (changed || newDialogInterest || terminalEdge || combatEdge) {
                facts.add(new ObservationFact(
                        ObservationFactType.WUHUAN_DIALOG_PRESENCE,
                        sample.dialogPresent() ? "present" : "absent",
                        now));
                log.info("Wuhuan dialog presence queued: windowId={} taskRunId={} observerSeq={} interestId={} newInterest={} present={} terminalEdge={} combatEdge={}",
                        context.getWindowId(), taskRunId, observerSeq, dialogInterestId,
                        newDialogInterest, sample.dialogPresent(), terminalEdge, combatEdge);
            }
            markSampled(WuhuanPresenceLocalMechanics.DIALOG_INTEREST, now);
        }
    }

    /** Emits both BR-DIALOG-001 facts together; Cloud rejects any pair from different envelopes. */
    private void sampleUnknownPhasePresence(List<ObservationInterest> interests,
                                            long now,
                                            List<ObservationFact> facts,
                                            List<ObservationRoi> rois) {
        boolean titleRequested = interests.stream().anyMatch(interest ->
                UnknownPhasePresenceLocalMechanics.TITLE_INTEREST.equals(interest.interestKey()));
        boolean dialogRequested = interests.stream().anyMatch(interest ->
                UnknownPhasePresenceLocalMechanics.DIALOG_INTEREST.equals(interest.interestKey()));
        boolean due = isDue(UnknownPhasePresenceLocalMechanics.TITLE_INTEREST,
                UnknownPhasePresenceLocalMechanics.SAMPLE_PERIOD_MS, now)
                || isDue(UnknownPhasePresenceLocalMechanics.DIALOG_INTEREST,
                UnknownPhasePresenceLocalMechanics.SAMPLE_PERIOD_MS, now);
        if (!titleRequested || !dialogRequested || !due) {
            return;
        }
        UnknownPhasePresenceLocalMechanics.Sample sample = unknownPhasePresenceMechanics.sample();
        facts.add(new ObservationFact(
                ObservationFactType.UNKNOWN_PHASE_TITLE_PRESENCE, sample.titlePresence(), now));
        facts.add(new ObservationFact(
                ObservationFactType.UNKNOWN_PHASE_DIALOG_PRESENCE, sample.dialogPresence(), now));
        byte[] dialogPng = sample.dialogPng();
        if (dialogPng != null) {
            rois.add(new ObservationRoi(
                    UnknownPhasePresenceLocalMechanics.DIALOG_FRAME_ROI,
                    200, 250, 640, 300, dialogPng));
        }
        markSampled(UnknownPhasePresenceLocalMechanics.TITLE_INTEREST, now);
        markSampled(UnknownPhasePresenceLocalMechanics.DIALOG_INTEREST, now);
        log.info("G017 paired presence queued: windowId={} taskRunId={} title={} dialog={}",
                context.getWindowId(), taskRunId, sample.titlePresence(), sample.dialogPresence());
    }

    private boolean observeWuhuanTerminal(List<ObservationPathingFact> pathingFacts) {
        ObservationPathingFact terminal = pathingFacts.stream()
                .filter(fact -> fact.state() == ObservationPathingState.ARRIVED
                        || fact.state() == ObservationPathingState.STOPPED_AWAY
                        // 数值判稳：STABLE=停下，同样触发标题/对话的即时采样边沿（审查修正）。
                        || fact.state() == ObservationPathingState.STABLE)
                .findFirst()
                .orElse(null);
        if (terminal == null) {
            lastWuhuanTerminalKey = null;
            return false;
        }
        // STABLE 是电平（updatedAtMs 每拍都变），边沿键改用停稳代号，一次停稳只触发一次。
        String key = terminal.state() == ObservationPathingState.STABLE
                ? terminal.intentId() + ":" + terminal.state() + ":" + terminal.terminalFrameGeneration()
                : terminal.intentId() + ":" + terminal.state() + ":" + terminal.pathingUpdatedAtMs();
        boolean changed = !Objects.equals(lastWuhuanTerminalKey, key);
        lastWuhuanTerminalKey = key;
        return changed;
    }

    /**
     * Applies the 59b combat-state mechanics locally. Entry is the first visible template stage.
     * Once combat is visible, local world and combat evidence are reciprocal: a visible mini-map
     * anchor confirms exit, a visible combat template confirms combat, and consecutive explicit misses
     * from both confirm exit. A single dual miss can be an incomplete PrintWindow frame and is retained.
     * An unavailable capture remains unknown and retains the current state.
     * Only exact task-run edges cross the wire.
     */
    void observeLocalCombatTransition(
            LocalCombatSignalMechanics.Signal signal,
            long now,
            long observerSeq,
            List<ObservationKeyEvent> events) {
        if (localCombatVisible) {
            LocalCombatSignalMechanics.Signal minimapSignal = combatSignalMechanics.sampleMinimap();
            int frameState;
            if (minimapSignal.state() == LocalCombatSignalMechanics.State.VISIBLE) {
                finishLocalCombat(now, events, "minimap-visible");
                return;
            }
            if (minimapSignal.state() == LocalCombatSignalMechanics.State.ABSENT
                    && signal != null
                    && signal.state() == LocalCombatSignalMechanics.State.ABSENT) {
                localCombatDualAbsentStreak++;
                // PrintWindow can briefly return the world while omitting the complete HUD. Require a
                // second independently sampled miss so that one partial frame cannot fabricate exit.
                if (localCombatDualAbsentStreak >= LOCAL_COMBAT_DUAL_ABSENT_CONFIRMATIONS) {
                    finishLocalCombat(now, events, "minimap-and-combat-absent-confirmed");
                    return;
                }
                log.info("Local combat dual-absent retained for confirmation: windowId={} generation={} streak={}/{}",
                        context.getWindowId(), localCombatGeneration, localCombatDualAbsentStreak,
                        LOCAL_COMBAT_DUAL_ABSENT_CONFIRMATIONS);
            } else {
                localCombatDualAbsentStreak = 0;
            }
            if (signal != null && signal.state() == LocalCombatSignalMechanics.State.VISIBLE) {
                frameState = LOCAL_COMBAT_FRAME_COMBAT_CONFIRMED;
            } else {
                frameState = LOCAL_COMBAT_FRAME_UNKNOWN;
            }
            if (frameState != lastLocalCombatFrameState) {
                log.debug("Local combat evidence changed: windowId={} state={} combatSignal={} minimapSignal={}",
                        context.getWindowId(), frameState,
                        signal == null ? "null" : signal.wireValue(), minimapSignal.wireValue());
                lastLocalCombatFrameState = frameState;
            }
        }
        if (signal == null || signal.state() == LocalCombatSignalMechanics.State.UNAVAILABLE) {
            return;
        }
        if (signal.state() == LocalCombatSignalMechanics.State.VISIBLE) {
            if (!localCombatVisible) {
                // 脱战后 3 秒进战冷却：过渡画面的战斗模板残影不得铸造新战斗（见常量处事故注释）。
                long sinceExitMs = now - lastLocalCombatExitConfirmedAtMs;
                if (lastLocalCombatExitConfirmedAtMs > 0L
                        && sinceExitMs >= 0L
                        && sinceExitMs < LOCAL_COMBAT_REENTRY_COOLDOWN_MS) {
                    log.info("Local combat entry ignored inside post-exit cooldown: "
                                    + "windowId={} sinceExitMs={} cooldownMs={} generation={}",
                            context.getWindowId(), sinceExitMs,
                            LOCAL_COMBAT_REENTRY_COOLDOWN_MS, localCombatGeneration);
                    return;
                }
                localCombatGeneration++;
                localCombatEntryPublished = false;
                context.updateLocalCombatGeneration(localCombatGeneration, true);
                boundExpectedCombatClaim =
                        context.bindExpectedCombatEnterClaim(taskRunId, localCombatGeneration);
                context.confirmLocalTemplateCombatEntry(boundExpectedCombatClaim);
                endActivePathingLegOnCombatEntry(now);
                if (leaderCombatBroadcast != null) {
                    leaderCombatBroadcast.publishLeaderCombatEdge(context, true);
                }
            }
            localCombatVisible = true;
            localCombatDualAbsentStreak = 0;
            lastLocalCombatFrameState = LOCAL_COMBAT_FRAME_COMBAT_CONFIRMED;
            if (!localCombatEntryPublished) {
                localCombatEntryPublished = publishCombatEdge(
                        ObservationKeyEventType.IN_COMBAT, now, events, "combat-signal-visible", false);
            }
            return;
        }
    }

    private void finishLocalCombat(
            long occurredAtMs,
            List<ObservationKeyEvent> events,
            String source) {
        log.info("Local combat exit confirmed: windowId={} generation={} source={} entryPublished={}",
                context.getWindowId(), localCombatGeneration, source, localCombatEntryPublished);
        lastLocalCombatExitConfirmedAtMs = occurredAtMs;
        localCombatVisible = false;
        localCombatDualAbsentStreak = 0;
        lastLocalCombatFrameState = LOCAL_COMBAT_FRAME_UNSET;
        context.updateLocalCombatGeneration(localCombatGeneration, false);
        if (leaderCombatBroadcast != null) {
            leaderCombatBroadcast.publishLeaderCombatEdge(context, false);
        }
        if (localCombatEntryPublished) {
            publishCombatEdge(
                    ObservationKeyEventType.COMBAT_EXITED, occurredAtMs, events, source, true);
            // Combat hides the normal Tracker/Dialog scene. Reprove that scene immediately after the exit edge
            // instead of waiting for the ordinary ten-second no-progress heartbeat.
            xinshouRefreshPending = true;
            xinshouRefreshObserverSeq = 0L;
        } else {
            submitArmedReturnHomeReplayWithoutBusinessExit();
        }
        localCombatEntryPublished = false;
    }

    void observeLocalCombatTransition(
            LocalCombatSignalMechanics.Signal signal,
            long now,
            List<ObservationKeyEvent> events) {
        observeLocalCombatTransition(signal, now, Long.MAX_VALUE, events);
    }

    private boolean publishCombatEdge(
            ObservationKeyEventType eventType,
            long occurredAtMs,
            List<ObservationKeyEvent> events,
            String source,
            boolean terminal) {
        com.bot.dhxy.window.model.WindowExpectedCombatEnterClaim claim = boundExpectedCombatClaim;
        if (claim == null) {
            claim = context.currentExpectedCombatEnterClaim(taskRunId, localCombatGeneration);
            boundExpectedCombatClaim = claim;
        }
        /*
         * Combat enter/exit is a physical fact and is ALWAYS reported. The claim no longer gates
         * publication: it only classifies the edge as expected (claim-bound) vs unexpected. An
         * unexpected edge carries no task identity; Cloud resolves identity from the observation
         * run binding it already fences on.
         */
        boolean expected = claim != null && Objects.equals(claim.combatGeneration(), localCombatGeneration);
        boolean passiveCombat = !expected && context.getSelectedTaskType().isSinglePlayer();
        ObservationKeyEvent edge = new ObservationKeyEvent(
                (eventType == ObservationKeyEventType.IN_COMBAT ? "combat-enter:" : "combat-exit:")
                        + (expected ? claim.claimId() : passiveCombat ? "passive" : "unexpected")
                        + ":" + localCombatGeneration,
                eventType,
                occurredAtMs,
                null,
                expected ? claim.attemptId() : null,
                null,
                "window-observation-runner:" + source,
                expected ? "explicit-enter-claim=" + claim.source()
                        : passiveCombat ? "passive-single-player-combat" : "unexpected-combat",
                expected ? claim.claimId() : null,
                localCombatGeneration,
                expected ? claim.taskCode() : null,
                expected ? claim.businessTaskRunId() : null);
        Consumer<ObservationKeyEvent> publisher = asyncEventPublisher;
        // The local Runner is the sole authority for a physical combat edge. A retained replay is
        // a later local input action; it must never delay or replace COMBAT_EXITED on the wire.
        events.add(edge);
        if (expected && terminal && returnHomeReplayCoordinator != null && publisher != null
                && context.hasArmedReturnHomeReplay(
                        claim.taskCode(), taskRunId, claim.businessTaskRunId())) {
            returnHomeReplayCoordinator.submitOnLocalExit(
                    context, claim.taskCode(), taskRunId, claim.businessTaskRunId(), null, publisher);
        }
        if (terminal && expected) {
            boundExpectedCombatClaim = null;
            context.clearExpectedCombatEnterClaim("expected combat generation exited");
        }
        return true;
    }

    /**
     * Submits an exact armed replay after a correction generation reaches a true local exit.
     *
     * <p>The early fast-exit generation already published its business terminal and cleared its
     * claim. The correction generation must therefore publish only the retained replay terminal,
     * never a second synthetic {@code COMBAT_EXITED} edge.</p>
     */
    private void submitArmedReturnHomeReplayWithoutBusinessExit() {
        Consumer<ObservationKeyEvent> publisher = asyncEventPublisher;
        if (returnHomeReplayCoordinator == null || publisher == null) {
            return;
        }
        com.bot.dhxy.window.model.WindowRetainedReturnHomeReplay replay =
                context.currentArmedReturnHomeReplay(taskRunId);
        if (replay == null) {
            return;
        }
        returnHomeReplayCoordinator.submitOnLocalExit(
                context,
                replay.taskCode(),
                taskRunId,
                replay.businessTaskRunId(),
                null,
                publisher);
    }

    private List<ObservationPreparedDialogFact> sampleWubeiEnterBattlePrepared(
            List<ObservationDialogInterestFact> dialogInterests,
            long observerSeq,
            long now) {
        if (observerSeq <= 0L || dialogInterests.size() != 1
                || !isDue(WUBEI_PREPARE_KEY, WUBEI_PREPARE_PERIOD_MS, now)) {
            return List.of();
        }
        markSampled(WUBEI_PREPARE_KEY, now);
        ObservationDialogInterestFact interest = dialogInterests.getFirst();
        if (!interest.active()
                || !TaskType.WUBEI.getCode().equals(interest.taskCode())
                || interest.operations() == null
                || !interest.operations().contains(DialogOperation.WUBEI_ENTER_BATTLE.name())) {
            return List.of();
        }
        WindowNativeBinding binding = context.getNativeBinding();
        if (binding == null || !binding.hasNativeHandle() || !binding.hasGeometry()) {
            return List.of();
        }
        DialogService.LocalPreparedDialogMatch matched =
                dialogService.prepareWubeiEnterBattleLocal(RUNNER_SOURCE + ":wubei").orElse(null);
        if (matched == null) {
            return List.of();
        }
        com.bot.dhxy.model.dialog.PreparedDialogAction action = matched.action();
        long capturedAtMs = Math.max(action.getPreparedAtMs(), System.currentTimeMillis());
        return List.of(new ObservationPreparedDialogFact(
                taskRunId,
                context.getWindowId(),
                binding.getNativeHandle(),
                interest.interestId(),
                interest.taskCode(),
                action.getOperation().name(),
                action.getTargetKeyword(),
                action.getMatchedText(),
                matched.matchLeft(),
                matched.matchTop(),
                matched.matchRight(),
                matched.matchBottom(),
                action.getAbsoluteX() - binding.getX(),
                action.getAbsoluteY() - binding.getY(),
                action.getValidationLeft() - binding.getX(),
                action.getValidationTop() - binding.getY(),
                action.getValidationRight() - binding.getX(),
                action.getValidationBottom() - binding.getY(),
                action.getWashMode().name(),
                action.getFingerprint(),
                action.isClickRequired(),
                action.getPreparedAtMs(),
                capturedAtMs,
                observerSeq,
                action.getSource()));
    }

    /** Releases identity-scoped image state when the owning observation runner stops. */
    public void reset() {
        lastSampledAtMs.clear();
        deliveredXinshouRois.clear();
        sampledXinshouRois.clear();
        observedXinshouRoiHashes.clear();
        deliveredXinshouFacts.clear();
        sampledXinshouFacts.clear();
        observedXinshouFacts.clear();
        lastXinshouEffectiveProgressAtMs = 0L;
        lastXinshouRefreshAcknowledgedAtMs = 0L;
        xinshouRefreshPending = false;
        xinshouRefreshObserverSeq = 0L;
        lastPathingFact = null;
        clearedPathingFactDelivered = false;
        lastDialogInterestFact = null;
        terminalCoordinateAcknowledgedIntentId = null;
        localCombatVisible = false;
        lastLocalCombatFrameState = LOCAL_COMBAT_FRAME_UNSET;
        localCombatGeneration = 0L;
        localCombatEntryPublished = false;
        boundExpectedCombatClaim = null;
        resetLocalPathingObservation();
        if (sharedPositionStripFrame != null) {
            sharedPositionStripFrame.flush();
            sharedPositionStripFrame = null;
        }
        sharedPositionStripCapturedAtMs = 0L;
        if (previousSharedPositionStripFrame != null) {
            previousSharedPositionStripFrame.flush();
            previousSharedPositionStripFrame = null;
        }
        previousSharedPositionStripCapturedAtMs = 0L;
        localPathingGeneration = 0L;
        nextTerminalFrameId = 1L;
        combatSignalMechanics.reset();
        xinshouAnchorMechanics.reset();
        wuhuanPresenceMechanics.reset();
        unknownPhasePresenceMechanics.reset();
        flyingSaturationMechanics.reset();
        observedWuhuanTitlePresent = null;
        observedWuhuanDialogPresent = null;
        observedWuhuanCompletionVerdict = null;
        lastWuhuanTitleSnapshotKey = null;
        lastWuhuanDialogInterestId = null;
        lastWuhuanTerminalKey = null;
        pendingGhostKingChangshouFlightIntentId = null;
        handledGhostKingChangshouFlightIntentId = null;
        pendingAsyncEvents.clear();
        pendingAsyncRois.clear();
        if (returnHomeReplayCoordinator != null) {
            returnHomeReplayCoordinator.clear(context, "observation runner closed");
        }
    }

    long coordinateFramesCaptured() {
        return coordinateFramesCaptured;
    }

    long coordinateFramesUnavailable() {
        return coordinateFramesUnavailable;
    }

    /**
     * Owns the exact-intent movement and terminal facts. The complete 178x35 map-name/coordinate strip
     * is cropped once from the G002 shared frame; the legacy 45x12 digit ROI is then cropped in memory.
     * Any valid digit-frame change latches movement for this intent. Cloud-recognized map/X/Y remains
     * terminal/cache evidence only and can never manufacture movement from stale logical coordinates.
     */
    private void refreshLocalPathingTerminal(long now) {
        WindowPathingSnapshot snapshot = context.getPathingSnapshot();
        WindowPathingIntent intent = snapshot == null ? null : snapshot.getIntent();
        if (intent == null || snapshot.getState() == WindowPathingState.NONE
                || snapshot.getState() == WindowPathingState.STOPPED_AWAY) {
            resetLocalPathingObservation();
            return;
        }
        if (snapshot.getState() == WindowPathingState.ARRIVED) {
            resetLocalPathingObservation(true);
            return;
        }
        // The task thread may register a new intent after this observation cycle captured its
        // initial timestamp. Never stamp that newer intent with the cycle's older time.
        long observedAtMs = Math.max(System.currentTimeMillis(), intent.getCreatedAtMs());
        if (observedAtMs - localPathingLastSampleAtMs < LOCAL_PATHING_SAMPLE_PERIOD_MS) {
            return;
        }
        localPathingLastSampleAtMs = observedAtMs;

        boolean newIntent = !Objects.equals(localPathingIntentId, intent.getIntentId());
        if (newIntent) {
            resetLocalPathingObservation();
            localPathingIntentId = intent.getIntentId();
            if (previousSharedPositionStripFrame != null
                    && previousSharedPositionStripCapturedAtMs > 0L
                    && previousSharedPositionStripCapturedAtMs <= intent.getCreatedAtMs()) {
                localPathingFrame = cropMovementDigits(previousSharedPositionStripFrame);
            }
            advanceLocalPathingGeneration();
            localPathingLastSampleAtMs = observedAtMs;
            localPathingLastChangedAtMs = observedAtMs;
        }
        /*
         * 2026-08-23 停稳事实重设计（五环首批）：五环走数值判稳——本地字模逐位读出坐标值，
         * 停/动在数值上判定，OCR 问答循环整个不走。其余任务仍走下方的像素差值老路，
         * 分批迁移（设计卡 dhxy-cloud-brain/docs/2026-08-22-local-stability-fact-redesign.md）。
         */
        if (isValueStabilityMode()) {
            refreshValueStability(intent, observedAtMs);
            return;
        }
        BufferedImage current = cropMovementDigits(sharedPositionStripFrame);
        if (current == null) {
            return;
        }

        boolean changed = localPathingFrame != null
                && !ImageFinder.isMatch(localPathingFrame, current, LOCAL_PATHING_DIFF_RATIO);
        /*
         * 2026-08-19 22:12 呆站取证(3511 停稳判定拖 32s/52s):这里每一次"判变"都会把静止
         * 计时清零——若数字框被飘字/特效扫过误判为变化,停稳永远攒不满 2.2s。用户铁律:判定
         * 点必须留证——前后小图落盘(images/temp/match-evidence/pathing-strip-diff,变/没变
         * 翻转时另存时间戳对,平时只覆盖 latest),下轮直接看图定罪。
         */
        MatchEvidenceStore.saveOnChange("pathing-strip-diff", context.getWindowId(),
                current, localPathingFrame == null ? current : localPathingFrame,
                changed ? null : new double[]{0D, 0D, 1D});
        if (changed) {
            localPathingCoordinateMovementObserved = true;
            advanceLocalPathingGeneration();
            localPathingLastChangedAtMs = observedAtMs;
            localPathingCoordinatePending = false;
            localPathingCoordinateRequestedChangedAtMs = 0L;
            localPathingCoordinateRequestedAtMs = 0L;
            localPathingCoordinateRequestedStableMs = 0L;
            localPathingCoordinateRequestedIntentAgeMs = 0L;
            localPathingArrivalCheckedChangedAtMs = 0L;
            terminalCoordinateAcknowledgedIntentId = null;
        }
        if (localPathingFrame != null) {
            localPathingFrame.flush();
        }
        localPathingFrame = current;

        WindowPathingSnapshot latest = context.getPathingSnapshot();
        if (latest == null || latest.getIntent() == null
                || !Objects.equals(intent.getIntentId(), latest.getIntent().getIntentId())) {
            return;
        }
        boolean coordinateTarget = intent.getType()
                != com.bot.dhxy.window.model.WindowPathingIntentType.UNTARGETED_TRACKER
                && intent.getTargetX() != null
                && intent.getTargetY() != null;
        boolean stableForArrival = coordinateTarget
                && !newIntent
                && observedAtMs - localPathingLastChangedAtMs >= LOCAL_PATHING_ARRIVAL_STATIONARY_MS
                && observedAtMs - intent.getCreatedAtMs() >= LOCAL_PATHING_ARRIVAL_STATIONARY_MS;
        boolean stableForStoppedAway = !newIntent
                && observedAtMs - localPathingLastChangedAtMs >= LOCAL_PATHING_STOPPED_AWAY_MS
                && observedAtMs - intent.getCreatedAtMs() >= LOCAL_PATHING_STOPPED_AWAY_MS;
        boolean stoppedAwayCoordinateProbeDue = stableForStoppedAway
                && (localPathingCoordinateResolvedAtMs <= 0L
                || observedAtMs - localPathingCoordinateResolvedAtMs
                >= LOCAL_PATHING_COORDINATE_PROBE_MIN_INTERVAL_MS);
        boolean coordinateVerdictNewlyPending = !localPathingCoordinatePending
                && (stoppedAwayCoordinateProbeDue
                || (stableForArrival
                && localPathingArrivalCheckedChangedAtMs != localPathingLastChangedAtMs));
        if (coordinateVerdictNewlyPending) {
            localPathingCoordinatePending = true;
        }
        context.updatePathingSnapshot(latest.toBuilder()
                .state(WindowPathingState.ACTIVE)
                .message(coordinateVerdictNewlyPending
                        ? "local runner stable; awaiting coordinate verdict"
                        : changed ? "local runner coordinate strip changed" : "local runner pathing active")
                .locationChangedAtMs(localPathingLastChangedAtMs)
                .coordinateMovementObserved(localPathingCoordinateMovementObserved)
                .updatedAtMs(observedAtMs)
                .probeStartedAtMs(observedAtMs)
                .probeFinishedAtMs(observedAtMs)
                .probeInProgress(false)
                .build());
        if (coordinateVerdictNewlyPending) {
            log.info("Local pathing coordinate verdict requested: windowId={} intentId={} source={} stableMs={}",
                    context.getWindowId(), intent.getIntentId(), intent.getSource(),
                    observedAtMs - localPathingLastChangedAtMs);
        }
    }

    private void resetLocalPathingObservation() {
        resetLocalPathingObservation(false);
    }

    private void resetLocalPathingObservation(boolean retainTerminalLineage) {
        if (localPathingFrame != null) {
            localPathingFrame.flush();
            localPathingFrame = null;
        }
        localPathingIntentId = null;
        localPathingLastSampleAtMs = 0L;
        localPathingLastChangedAtMs = 0L;
        localPathingCoordinateMovementObserved = false;
        localPathingCoordinatePending = false;
        localPathingBaselinePending = false;
        localPathingBaselineRequestedAtMs = 0L;
        localPathingCoordinateRequestedChangedAtMs = 0L;
        localPathingCoordinateRequestedAtMs = 0L;
        localPathingCoordinateRequestedStableMs = 0L;
        localPathingCoordinateRequestedIntentAgeMs = 0L;
        localPathingCoordinateResolvedAtMs = 0L;
        localPathingArrivalCheckedChangedAtMs = 0L;
        localPathingRecognizedMapName = null;
        localPathingRecognizedX = null;
        localPathingRecognizedY = null;
        localPathingRecognizedChangedAtMs = 0L;
        invalidateTerminalFrameEvidence();
        if (!retainTerminalLineage) {
            clearTerminalFrameLineage();
        }
        valueLastX = null;
        valueLastY = null;
        valueSameSinceMs = 0L;
        valueStableActive = false;
        valueStableFramePending = false;
        valueUnreadableSinceMs = 0L;
        valueHadUnreadableGap = false;
    }

    /** 数值判稳的启用面：五环先行（另含专测任务），验证后分批扩到其余任务。 */
    private boolean isValueStabilityMode() {
        return context.getSelectedTaskType() == TaskType.WUHUAN_V3
                || context.getSelectedTaskType() == TaskType.PATHING_TEST;
    }

    /**
     * 数值判稳核心（替代像素差值+云端 OCR 问答）：每拍对整条坐标条做字模读值。
     * 有效变值=动了（单拍铁证，撤销既有停稳=整代作废）；连续同值≥{@link #VALUE_STABLE_ENTER_MS}
     * =停稳（新一代+拍到达帧）；不可读=第三态，不推进也不撤销（撤销只认有效变值——
     * 2026-08-23 21:19 事故：输入宏占全局锁造成全窗口条子断供，旧的"保守撤稳+清基线"
     * 让每次断供都拆掉停稳、恢复时又伪造"新基线 ACTIVE"把云端已备好的到达点击作废），
     * 未进停稳时持续≥{@link #VALUE_UNREADABLE_REPORT_MS} 明报 STRIP_UNAVAILABLE；
     * 已进停稳时电平续报 STABLE。到达/半路的业务判定全部移交云端（翻译层）。
     */
    private void refreshValueStability(WindowPathingIntent intent, long observedAtMs) {
        /*
         * 审查 P1 修正：共享条帧在成员静默等场景可能停更——陈帧读出"值没变"不是停稳证据。
         * 帧龄超过一拍半就按本拍不可读处理，绝不用旧条子作证。
         */
        boolean stripFresh = sharedPositionStripFrame != null
                && sharedPositionStripCapturedAtMs > 0L
                && observedAtMs - sharedPositionStripCapturedAtMs <= VALUE_STRIP_FRESH_MS;
        LocalCoordinateStripReader.Reading reading = stripFresh
                ? LocalCoordinateStripReader.read(sharedPositionStripFrame)
                : LocalCoordinateStripReader.Reading.invalid();
        if (!reading.valid()) {
            valueHadUnreadableGap = true;
            if (valueUnreadableSinceMs == 0L) {
                valueUnreadableSinceMs = observedAtMs;
            }
            if (observedAtMs - valueUnreadableSinceMs < VALUE_UNREADABLE_REPORT_MS) {
                return;
            }
            if (valueStableActive) {
                // 第三态不撤销停稳、不清基线：停稳主张一直站着，只有之后读到"有效且不同的值"
                // 才算被推翻。电平续报 STABLE（云端按新鲜度过滤，断供期也不能让事实过期）。
                updateValueSnapshot(intent, WindowPathingState.STABLE, valueLastX, valueLastY,
                        observedAtMs, "stable claim held through unreadable strip (third state)");
                return;
            }
            updateValueSnapshot(intent, WindowPathingState.STRIP_UNAVAILABLE, null, null,
                    observedAtMs, "coordinate strip unreadable (occlusion/black-frame/anchor-miss)");
            return;
        }
        valueUnreadableSinceMs = 0L;
        boolean changed = valueLastX == null || valueLastY == null
                || valueLastX != reading.x() || valueLastY != reading.y();
        /*
         * 审查修正（设计契约"不可读不推进任何计时"）：有效拍之间隔着不可读缺口时，
         * 未进停稳的同值不许把缺口时间算进 900ms——重新开始累计。已进停稳的不受影响
         * （第三态不撤销，撤销只认有效变值）。
         */
        if (valueHadUnreadableGap) {
            valueHadUnreadableGap = false;
            if (!changed && !valueStableActive) {
                valueSameSinceMs = observedAtMs;
            }
        }
        if (changed) {
            boolean hadBaseline = valueLastX != null;
            if (valueStableActive) {
                valueStableActive = false;
                valueStableFramePending = false;
                advanceLocalPathingGeneration();
                log.info("Value stability RETRACTED by value change: windowId={} intentId={} value=({}, {})",
                        context.getWindowId(), intent.getIntentId(), reading.x(), reading.y());
            }
            if (hadBaseline) {
                localPathingCoordinateMovementObserved = true;
                localPathingLastChangedAtMs = observedAtMs;
            }
            valueLastX = reading.x();
            valueLastY = reading.y();
            valueSameSinceMs = observedAtMs;
            updateValueSnapshot(intent, WindowPathingState.ACTIVE, reading.x(), reading.y(),
                    observedAtMs, hadBaseline
                            ? "coordinate value changed; moving"
                            : "coordinate value baseline established");
            return;
        }
        if (!valueStableActive && observedAtMs - valueSameSinceMs >= VALUE_STABLE_ENTER_MS) {
            valueStableActive = true;
            valueStableFramePending = true;
            advanceLocalPathingGeneration();
            log.info("Value stability ENTERED: windowId={} intentId={} value=({}, {}) stableMs={} score={}",
                    context.getWindowId(), intent.getIntentId(), reading.x(), reading.y(),
                    observedAtMs - valueSameSinceMs, reading.score());
        }
        updateValueSnapshot(intent,
                valueStableActive ? WindowPathingState.STABLE : WindowPathingState.ACTIVE,
                reading.x(), reading.y(), observedAtMs,
                valueStableActive ? "coordinate value stable" : "coordinate value holding");
    }

    private void updateValueSnapshot(WindowPathingIntent intent,
                                     WindowPathingState state,
                                     Integer x,
                                     Integer y,
                                     long observedAtMs,
                                     String message) {
        WindowPathingSnapshot latest = context.getPathingSnapshot();
        if (latest == null || latest.getIntent() == null
                || !Objects.equals(intent.getIntentId(), latest.getIntent().getIntentId())) {
            return;
        }
        String mapName = null;
        com.bot.dhxy.model.PlayerCharacter me = context.getGameState().getMe();
        if (me != null && me.getCurrentMapName() != null && !me.getCurrentMapName().isBlank()) {
            mapName = me.getCurrentMapName();
        }
        // 协议约束：非 STABLE 状态带坐标必须带地图名；STABLE 例外（字模只出数字）。
        boolean allowCoordinates = x != null && y != null
                && (state == WindowPathingState.STABLE || mapName != null);
        context.updatePathingSnapshot(latest.toBuilder()
                .state(state)
                .currentMapName(allowCoordinates ? mapName : null)
                .currentX(allowCoordinates ? x : null)
                .currentY(allowCoordinates ? y : null)
                .message(message)
                .locationChangedAtMs(localPathingLastChangedAtMs)
                .coordinateMovementObserved(localPathingCoordinateMovementObserved)
                .updatedAtMs(observedAtMs)
                .probeFinishedAtMs(observedAtMs)
                .probeInProgress(false)
                .build());
    }

    /**
     * Captures one fresh exact-window frame for the current stationary generation and derives the
     * coordinate strip from those same in-memory pixels. The full frame keeps one frame id across
     * transport uncertainty and is removed only after a successful observation response.
     */
    private void sampleTerminalCoordinateFrame(List<ObservationPathingFact> pathingFacts,
                                               List<ObservationRoi> rois,
                                               List<TerminalCandidateFrame> terminalFrames) {
        if (isValueStabilityMode()) {
            sampleValueStableTerminalFrame(pathingFacts, terminalFrames);
            return;
        }
        if (pathingFacts.isEmpty() || localPathingFrame == null) {
            return;
        }
        ObservationPathingFact fact = pathingFacts.getFirst();
        if (!localPathingCoordinatePending
                || fact.state() != ObservationPathingState.ACTIVE
                || Objects.equals(terminalCoordinateAcknowledgedIntentId, fact.intentId())) {
            return;
        }
        if (terminalCoordinateRoi == null) {
            captureTerminalFrameEvidence(fact);
        }
        if (terminalCoordinateRoi == null) {
            return;
        }
        rois.add(terminalCoordinateRoi);
        if (pendingTerminalFrame != null) {
            terminalFrames.add(pendingTerminalFrame);
        }
        localPathingCoordinateRequestedChangedAtMs = localPathingLastChangedAtMs;
        localPathingCoordinateRequestedAtMs = System.currentTimeMillis();
        localPathingCoordinateRequestedStableMs = Math.max(
                0L, localPathingLastSampleAtMs - localPathingLastChangedAtMs);
        WindowPathingSnapshot snapshot = context.getPathingSnapshot();
        WindowPathingIntent intent = snapshot == null ? null : snapshot.getIntent();
        localPathingCoordinateRequestedIntentAgeMs = intent == null
                ? 0L
                : Math.max(0L, localPathingLastSampleAtMs - intent.getCreatedAtMs());
    }

    /*
     * G102 收口（2026-08-24）：scene-presence 缓存整体删除。它的最后一个读者（终局帧前
     * 的自动清场钩子）已下线，此后它每 5 秒白跑一次旧的方差门 Dialog 判定纯烧 CPU。
     */

    /**
     * 走路基线预热(用户批准 2026-08-20):寻路 ACTIVE 期间每 4s 从共享周期帧内存裁一张坐标
     * 数字条上云解析,让"识别坐标基线"在停下之前就位——停下判定从两轮往返(首轮只建基线
     * 必返 ACTIVE)缩为一轮。纯旁路:独立 pending、不走终局帧通道(零清屏/零整帧上传副作用)、
     * 回包只更新基线不做终局分类。
     */
    private void sampleWalkingBaselineCoordinateRoi(List<ObservationPathingFact> pathingFacts,
                                                    List<ObservationRoi> rois,
                                                    long now) {
        if (isValueStabilityMode()) {
            // 数值判稳模式：本地自己读值，不再走"基线预热上云 OCR"的旁路。
            return;
        }
        if (localPathingBaselinePending
                && now - localPathingBaselineRequestedAtMs > LOCAL_PATHING_BASELINE_TIMEOUT_MS) {
            localPathingBaselinePending = false;
        }
        if (pathingFacts.isEmpty() || localPathingBaselinePending || localPathingCoordinatePending) {
            return;
        }
        ObservationPathingFact fact = pathingFacts.getFirst();
        if (fact.state() != ObservationPathingState.ACTIVE || fact.intentId() == null) {
            return;
        }
        if (localPathingCoordinateResolvedAtMs > 0L
                && now - localPathingCoordinateResolvedAtMs < LOCAL_PATHING_WALKING_BASELINE_REFRESH_MS) {
            return;
        }
        WindowPathingSnapshot snapshot = context.getPathingSnapshot();
        WindowPathingIntent intent = snapshot == null ? null : snapshot.getIntent();
        if (intent == null || !Objects.equals(intent.getIntentId(), fact.intentId())
                || now - intent.getCreatedAtMs() < 2_000L) {
            return;
        }
        if (sharedCycleFrame == null || sharedCycleFrameRect == null) {
            return;
        }
        int[] stripRect = coordinateHelper.getScaledRect(
                PATHING_COORDINATE_STRIP_X, PATHING_COORDINATE_STRIP_Y,
                PATHING_COORDINATE_STRIP_WIDTH, PATHING_COORDINATE_STRIP_HEIGHT);
        int cropX = stripRect[0] - sharedCycleFrameRect[0];
        int cropY = stripRect[1] - sharedCycleFrameRect[1];
        int cropWidth = stripRect[2] - stripRect[0];
        int cropHeight = stripRect[3] - stripRect[1];
        if (cropX < 0 || cropY < 0 || cropWidth <= 0 || cropHeight <= 0
                || cropX + cropWidth > sharedCycleFrame.getWidth()
                || cropY + cropHeight > sharedCycleFrame.getHeight()) {
            return;
        }
        try {
            BufferedImage strip = sharedCycleFrame.getSubimage(cropX, cropY, cropWidth, cropHeight);
            ByteArrayOutputStream png = new ByteArrayOutputStream(4096);
            ImageIO.write(strip, "png", png);
            rois.add(new ObservationRoi(
                    COORDINATE_STRIP_INTEREST,
                    cropX,
                    cropY,
                    cropWidth,
                    cropHeight,
                    png.toByteArray(),
                    null,
                    fact.intentId(),
                    null,
                    null));
            localPathingBaselinePending = true;
            localPathingBaselineRequestedAtMs = now;
            log.info("Local pathing walking baseline requested: windowId={} intentId={}",
                    context.getWindowId(), fact.intentId());
        } catch (IOException encodeFailure) {
            log.debug("Walking baseline strip encode failed: windowId={} message={}",
                    context.getWindowId(), encodeFailure.getMessage());
        }
    }

    /**
     * 数值判稳的到达帧交付：一次停稳=一张图=一个 generation。进入停稳后拍一次整窗到达帧
     * （沿用既有清障探针+捕获通道），随停稳事实上云；撤销停稳时 generation 推进已把
     * pending 帧和 lineage 作废。不再交付 OCR 数字条 ROI——云端无需读数（值在事实里）。
     */
    private void sampleValueStableTerminalFrame(List<ObservationPathingFact> pathingFacts,
                                                List<TerminalCandidateFrame> terminalFrames) {
        if (pathingFacts.isEmpty()) {
            return;
        }
        ObservationPathingFact fact = pathingFacts.getFirst();
        if (fact.state() != ObservationPathingState.STABLE) {
            return;
        }
        if (valueStableFramePending) {
            captureTerminalFrameEvidence(fact);
            if (lastTerminalFrameId != null) {
                valueStableFramePending = false;
            }
        }
        if (pendingTerminalFrame != null) {
            terminalFrames.add(pendingTerminalFrame);
        }
    }

    private void captureTerminalFrameEvidence(ObservationPathingFact fact) {
        /*
         * Tracker 基址是 ThreadLocal（记忆条目 tracker-threadlocal-state-trap 的第三次复发，
         * 2026-08-21 17:35 实锤）：本方法跑在 dhxy-observe-transport-* 线程上，该线程从不走常规
         * 采样入口，ThreadLocal 里的 base 永远是初始值 -1。拿 (-1,-1) 去抓一个和真实窗口毫无重叠
         * 的矩形，每拍必失败、失败只计数就重来——三个窗口同时以 ~20 次/秒白烧整窗 PrintWindow，
         * 正好落在走路腿收尾时（用户"散步中移动会卡"的直接原因）。抓屏前必须先刷新本线程的
         * 窗口状态；刷不出来说明窗口暂不可用，弃掉本拍即可，下一拍自然重试。
         */
        if (!tracker.refreshWindowState()) {
            coordinateFramesUnavailable++;
            return;
        }
        int baseX = tracker.getWindowBaseX();
        int baseY = tracker.getWindowBaseY();
        BufferedImage fullFrame = tracker.captureToMemory(
                "observe:terminal-candidate-frame",
                baseX,
                baseY,
                baseX + ObservationProtocolValidator.TERMINAL_FRAME_WIDTH,
                baseY + ObservationProtocolValidator.TERMINAL_FRAME_HEIGHT);
        if (fullFrame == null
                || fullFrame.getWidth() != ObservationProtocolValidator.TERMINAL_FRAME_WIDTH
                || fullFrame.getHeight() != ObservationProtocolValidator.TERMINAL_FRAME_HEIGHT) {
            if (fullFrame != null) {
                fullFrame.flush();
            }
            coordinateFramesUnavailable++;
            return;
        }
        try {
            int[] coordinateRect = coordinateHelper.getScaledRect(
                    PATHING_COORDINATE_STRIP_X, PATHING_COORDINATE_STRIP_Y,
                    PATHING_COORDINATE_STRIP_WIDTH, PATHING_COORDINATE_STRIP_HEIGHT);
            int cropX = coordinateRect[0] - baseX;
            int cropY = coordinateRect[1] - baseY;
            int cropWidth = coordinateRect[2] - coordinateRect[0];
            int cropHeight = coordinateRect[3] - coordinateRect[1];
            if (cropX < 0 || cropY < 0 || cropWidth <= 0 || cropHeight <= 0
                    || cropX + cropWidth > fullFrame.getWidth()
                    || cropY + cropHeight > fullFrame.getHeight()) {
                coordinateFramesUnavailable++;
                return;
            }
            BufferedImage coordinate = fullFrame.getSubimage(cropX, cropY, cropWidth, cropHeight);
            ByteArrayOutputStream fullPng = new ByteArrayOutputStream(512 * 1024);
            ByteArrayOutputStream coordinatePng = new ByteArrayOutputStream(4096);
            ImageIO.write(fullFrame, "png", fullPng);
            ImageIO.write(coordinate, "png", coordinatePng);
            long capturedAtMs = System.currentTimeMillis();
            long frameId = nextTerminalFrameId++;
            if (nextTerminalFrameId <= 0L) {
                nextTerminalFrameId = 1L;
            }
            pendingTerminalFrame = new TerminalCandidateFrame(
                    frameId,
                    localPathingGeneration,
                    fact.intentId(),
                    fullFrame.getWidth(),
                    fullFrame.getHeight(),
                    capturedAtMs,
                    fullPng.toByteArray());
            lastTerminalFrameId = frameId;
            lastTerminalFrameGeneration = localPathingGeneration;
            lastTerminalFrameIntentId = fact.intentId();
            terminalCoordinateRoi = new ObservationRoi(
                    COORDINATE_STRIP_INTEREST,
                    cropX,
                    cropY,
                    cropWidth,
                    cropHeight,
                    coordinatePng.toByteArray(),
                    null,
                    fact.intentId(),
                    null,
                    null);
            coordinateFramesCaptured++;
        } catch (IOException encodeFailure) {
            coordinateFramesUnavailable++;
            invalidateTerminalFrameEvidence();
            log.debug("Terminal candidate frame encode failed: windowId={} intentId={}",
                    context.getWindowId(), fact.intentId());
        } finally {
            fullFrame.flush();
        }
    }

    /** Successful transport acknowledgement removes only the full-frame upload, never recaptures it. */
    void acknowledgeTerminalFrames(List<Long> frameIds) {
        if (pendingTerminalFrame != null && frameIds != null
                && frameIds.contains(pendingTerminalFrame.frameId())) {
            pendingTerminalFrame = null;
        }
    }

    /** A retained runner may resume after pause, but terminal image evidence must start a fresh generation. */
    void invalidateTerminalFrameForSuspend() {
        advanceLocalPathingGeneration();
        deliveredXinshouRois.clear();
        sampledXinshouRois.clear();
        // 审查修正：暂停恢复落在停稳期内时，本代到达帧已被上面作废——重臂拍帧旗，
        // 恢复后同一停稳补拍一张，否则该代 STABLE 事实永远无 lineage。
        valueStableFramePending = valueStableActive;
    }

    private void advanceLocalPathingGeneration() {
        localPathingGeneration++;
        if (localPathingGeneration <= 0L) {
            localPathingGeneration = 1L;
        }
        invalidateTerminalFrameEvidence();
        clearTerminalFrameLineage();
    }

    private void invalidateTerminalFrameEvidence() {
        pendingTerminalFrame = null;
        terminalCoordinateRoi = null;
    }

    private void clearTerminalFrameLineage() {
        lastTerminalFrameId = null;
        lastTerminalFrameGeneration = null;
        lastTerminalFrameIntentId = null;
    }

    /**
     * Applies a Cloud-recognized pathing coordinate to the exact window's local state, then uses
     * the same result for the CR142 arrival/stopped-away classifier.
     *
     * <p>The map/X/Y are logical game coordinates, not screen pixels. Updating the per-window
     * {@link PlayerCharacter} here serves Runner pathing mechanics and local diagnostics. Cloud
     * commits its own task-position cache in the request that performed recognition, so this
     * Client must not echo the result as a later {@code POSITION_SAMPLE}. This update is data-only:
     * it neither wakes a task nor changes the terminal classifier's existing intent/generation guards.</p>
     */
    void acceptAnalysisResults(List<ObservationAnalysisResult> results) {
        if (results == null) {
            return;
        }
        for (ObservationAnalysisResult result : results) {
            if (result == null || result.mapName() == null
                    || result.coordinateX() == null || result.coordinateY() == null) {
                continue;
            }
            if ("PRE_COMBAT_COORDINATE_RESOLVED".equals(result.resultType())) {
                updateWindowPlayerLocation(result, "pre-combat coordinate frame");
                continue;
            }
            if (!"PATHING_COORDINATE_RESOLVED".equals(result.resultType()) || result.intentId() == null) {
                continue;
            }
            WindowPathingSnapshot snapshot = context.getPathingSnapshot();
            WindowPathingIntent intent = snapshot == null ? null : snapshot.getIntent();
            /*
             * 走路基线预热旁路(用户批准 2026-08-20):只更新识别坐标基线,不做终局分类,
             * 也不受"请求时刻静止戳"门约束(走路中戳必然前移)。
             */
            if (localPathingBaselinePending
                    && !localPathingCoordinatePending
                    && intent != null
                    && Objects.equals(intent.getIntentId(), result.intentId())) {
                localPathingBaselinePending = false;
                updateWindowPlayerLocation(result, "pathing walking baseline");
                long baselineAtMs = System.currentTimeMillis();
                if (hasRecognizedPathingCoordinateChanged(
                        localPathingRecognizedX, localPathingRecognizedY,
                        result.coordinateX(), result.coordinateY())) {
                    localPathingRecognizedMapName = result.mapName();
                    localPathingRecognizedX = result.coordinateX();
                    localPathingRecognizedY = result.coordinateY();
                    localPathingRecognizedChangedAtMs = baselineAtMs;
                }
                localPathingCoordinateResolvedAtMs = baselineAtMs;
                log.info("Local pathing walking baseline refreshed: windowId={} intentId={} map={} coord=({}, {})",
                        context.getWindowId(), result.intentId(), result.mapName(),
                        result.coordinateX(), result.coordinateY());
                continue;
            }
            if (intent == null
                    || !Objects.equals(intent.getIntentId(), result.intentId())
                    || !localPathingCoordinatePending
                    || localPathingCoordinateRequestedChangedAtMs != localPathingLastChangedAtMs) {
                continue;
            }
            updateWindowPlayerLocation(result, "pathing terminal coordinate");
            long resolvedAtMs = System.currentTimeMillis();
            long stableMs = localPathingCoordinateRequestedStableMs;
            long intentAgeMs = localPathingCoordinateRequestedIntentAgeMs;
            long responseLatencyMs = localPathingCoordinateRequestedAtMs <= 0L
                    ? 0L
                    : Math.max(0L, resolvedAtMs - localPathingCoordinateRequestedAtMs);
            boolean recognizedLocationChanged = hasRecognizedPathingCoordinateChanged(
                    localPathingRecognizedX, localPathingRecognizedY,
                    result.coordinateX(), result.coordinateY());
            if (recognizedLocationChanged) {
                localPathingRecognizedMapName = result.mapName();
                localPathingRecognizedX = result.coordinateX();
                localPathingRecognizedY = result.coordinateY();
                localPathingRecognizedChangedAtMs = resolvedAtMs;
            }
            localPathingCoordinateResolvedAtMs = resolvedAtMs;
            long recognizedStationaryMs = localPathingRecognizedChangedAtMs <= 0L
                    ? 0L
                    : Math.max(0L, resolvedAtMs - localPathingRecognizedChangedAtMs);
            WindowPathingState terminal = classifyRecognizedPathingState(
                    intent, result.mapName(), result.coordinateX(), result.coordinateY(),
                    stableMs, intentAgeMs, recognizedLocationChanged, recognizedStationaryMs);
            if (terminal == WindowPathingState.ACTIVE) {
                context.updatePathingSnapshot(snapshot.toBuilder()
                        .state(WindowPathingState.ACTIVE)
                        .currentMapName(result.mapName())
                        .currentX(result.coordinateX())
                        .currentY(result.coordinateY())
                        .message("local runner recognized coordinate changed; pathing remains active")
                        .locationChangedAtMs(localPathingRecognizedChangedAtMs)
                        .coordinateMovementObserved(localPathingCoordinateMovementObserved)
                        .updatedAtMs(resolvedAtMs)
                        .probeFinishedAtMs(resolvedAtMs)
                        .probeInProgress(false)
                        .build());
                localPathingArrivalCheckedChangedAtMs = localPathingLastChangedAtMs;
                localPathingCoordinatePending = false;
                localPathingCoordinateRequestedChangedAtMs = 0L;
                localPathingCoordinateRequestedAtMs = 0L;
                localPathingCoordinateRequestedStableMs = 0L;
                localPathingCoordinateRequestedIntentAgeMs = 0L;
                advanceLocalPathingGeneration();
                log.info("Local pathing arrival check remains active: windowId={} intentId={} map={} "
                                + "coord=({}, {}) coordinateChanged={} recognizedStationaryMs={} "
                                + "capturedStableMs={} responseLatencyMs={} stoppedAwayAtMs={}",
                        context.getWindowId(), result.intentId(), result.mapName(),
                        result.coordinateX(), result.coordinateY(), recognizedLocationChanged,
                        recognizedStationaryMs, stableMs, responseLatencyMs,
                        LOCAL_PATHING_STOPPED_AWAY_MS);
                continue;
            }
            context.updatePathingSnapshot(snapshot.toBuilder()
                    .state(terminal)
                    .currentMapName(result.mapName())
                    .currentX(result.coordinateX())
                    .currentY(result.coordinateY())
                    .message(terminal == WindowPathingState.ARRIVED
                            ? "local runner confirmed arrival from recognized coordinate"
                            : "local runner confirmed stopped away from recognized coordinate")
                    .coordinateMovementObserved(localPathingCoordinateMovementObserved)
                    .updatedAtMs(resolvedAtMs)
                    .probeFinishedAtMs(resolvedAtMs)
                    .probeInProgress(false)
                    .build());
            terminalCoordinateAcknowledgedIntentId = result.intentId();
            localPathingCoordinatePending = false;
            invalidateTerminalFrameEvidence();
            log.info("Local pathing terminal classified: windowId={} intentId={} state={} map={} coord=({}, {}) "
                            + "capturedStableMs={} responseLatencyMs={}",
                    context.getWindowId(), result.intentId(), terminal,
                    result.mapName(), result.coordinateX(), result.coordinateY(),
                    stableMs, responseLatencyMs);
        }
    }

    /**
     * G057 detects Changshou while the exact Ghost King tracker leg is still moving. It arms only;
     * input is intentionally deferred until the next shared-frame collection tick.
     */
    private void armGhostKingChangshouFlightAssistFromMovingFrame() {
        if (context.getSelectedTaskType() != TaskType.GHOST_KING) {
            return;
        }
        WindowPathingSnapshot snapshot = context.getPathingSnapshot();
        WindowPathingIntent intent = snapshot == null ? null : snapshot.getIntent();
        if (intent == null
                || (snapshot.getState() != WindowPathingState.ACTIVE
                && snapshot.getState() != WindowPathingState.UNKNOWN)
                || intent.getSource() == null
                || !intent.getSource().startsWith("ghost-king:tracker-shortcut")
                || Objects.equals(handledGhostKingChangshouFlightIntentId, intent.getIntentId())
                || Objects.equals(pendingGhostKingChangshouFlightIntentId, intent.getIntentId())) {
            return;
        }
        FlyingSaturationLocalMechanics.MapLabelSample mapLabel =
                flyingSaturationMechanics.sampleChangshouMapLabel();
        if (mapLabel.state() != FlyingSaturationLocalMechanics.MapLabelState.VISIBLE) {
            return;
        }
        pendingGhostKingChangshouFlightIntentId = intent.getIntentId();
        log.info("Ghost King Changshou map label matched during pathing; flight assist armed for next runner tick: "
                        + "windowId={} taskRunId={} intentId={} source={} score={}",
                context.getWindowId(), taskRunId, intent.getIntentId(), intent.getSource(),
                String.format(java.util.Locale.ROOT, "%.4f", mapLabel.score()));
    }

    /**
     * Consumes G057 on a later shared-frame tick. UNKNOWN retains the arm; a definitive state consumes it.
     */
    private void sampleGhostKingChangshouFlightAssist() {
        String intentId = pendingGhostKingChangshouFlightIntentId;
        if (intentId == null) {
            return;
        }
        WindowPathingSnapshot snapshot = context.getPathingSnapshot();
        WindowPathingIntent currentIntent = snapshot == null ? null : snapshot.getIntent();
        if (currentIntent == null
                || !Objects.equals(intentId, currentIntent.getIntentId())
                || (snapshot.getState() != WindowPathingState.ACTIVE
                && snapshot.getState() != WindowPathingState.UNKNOWN)) {
            pendingGhostKingChangshouFlightIntentId = null;
            log.info("Ghost King Changshou flight assist discarded after pathing changed: windowId={} "
                            + "armedIntentId={} currentIntentId={} state={}",
                    context.getWindowId(), intentId,
                    currentIntent == null ? null : currentIntent.getIntentId(),
                    snapshot == null ? null : snapshot.getState());
            return;
        }
        FlyingSaturationLocalMechanics.Sample sample = flyingSaturationMechanics.sample();
        log.info("Ghost King Changshou flight saturation sampled: windowId={} taskRunId={} intentId={} "
                        + "state={} meanSaturation={} pixels={}",
                context.getWindowId(), taskRunId, intentId, sample.state(),
                Double.isFinite(sample.meanSaturation())
                        ? String.format(java.util.Locale.ROOT, "%.4f", sample.meanSaturation())
                        : "unavailable",
                sample.pixelCount());
        if (sample.state() == FlyingSaturationLocalMechanics.State.UNKNOWN) {
            return;
        }

        pendingGhostKingChangshouFlightIntentId = null;
        handledGhostKingChangshouFlightIntentId = intentId;
        if (sample.state() == FlyingSaturationLocalMechanics.State.FLYING) {
            log.info("Ghost King Changshou flight assist already flying; no input: windowId={} intentId={}",
                    context.getWindowId(), intentId);
            return;
        }

        // The global input worker remains the sole physical-input owner. The sampler only submits
        // the intent and immediately returns to combat/pathing observation.
        inputSequences.submitAsync(
                        "ghost-king:changshou-enable-flight",
                        List.of(InputAction.pressAltC()))
                .whenComplete((pressed, inputFailure) -> {
                    if (inputFailure != null) {
                        log.warn("Ghost King Changshou Alt+C failed: windowId={} intentId={} type={} message={}",
                                context.getWindowId(), intentId, inputFailure.getClass().getSimpleName(),
                                inputFailure.getMessage());
                    }
                    log.info("Ghost King Changshou flight assist Alt+C completed: windowId={} taskRunId={} "
                                    + "intentId={} focusedSerialized=true executed={}",
                            context.getWindowId(), taskRunId, intentId,
                            inputFailure == null && Boolean.TRUE.equals(pressed));
                });
    }

    /**
     * A fight starting is the end of whatever walk was under way — the character cannot walk in combat.
     *
     * <p>Without this, a leg interrupted by its own destination's fight (the ordinary 天庭 shape: green
     * link → walk → dialog answered → combat) stayed ACTIVE all the way through the battle and long past
     * it: the coordinate strip is hidden in combat so the arrival check cannot classify anything, and the
     * post-combat re-render keeps resetting its stability window. A real run held the intent ACTIVE for 61
     * seconds after the fight ended. All of that time the Cloud correctly refused to release an ACTIVE
     * intent, and the observer correctly published nothing while an intent was on record — so the whole
     * post-combat recovery (fresh green link included) sat behind a walk that had in fact ended the moment
     * the fight began. The combat entry edge is this window's own physical fact, known right here, so the
     * leg is closed on it rather than guessed from coordinates a minute later.</p>
     */
    private void endActivePathingLegOnCombatEntry(long now) {
        WindowPathingSnapshot snapshot = context.getPathingSnapshot();
        WindowPathingIntent intent = snapshot == null ? null : snapshot.getIntent();
        if (intent == null) {
            return;
        }
        WindowPathingState state = snapshot.getState();
        if (state != WindowPathingState.ACTIVE && state != WindowPathingState.UNKNOWN) {
            return;
        }
        context.updatePathingSnapshot(snapshot.toBuilder()
                .state(WindowPathingState.STOPPED_AWAY)
                .message("combat entry ended the walk")
                .updatedAtMs(now)
                .probeFinishedAtMs(now)
                .probeInProgress(false)
                .build());
        terminalCoordinateAcknowledgedIntentId = intent.getIntentId();
        localPathingCoordinatePending = false;
        invalidateTerminalFrameEvidence();
        log.info("Local pathing terminal classified by combat entry: windowId={} intentId={} source={}",
                context.getWindowId(), intent.getIntentId(), intent.getSource());
    }

    /** Updates only this bound window's local logical position from Cloud's analysis response. */
    private void updateWindowPlayerLocation(ObservationAnalysisResult result, String source) {
        long observedAtMs = System.currentTimeMillis();
        context.updateRecognizedPlayerLocation(
                result.mapName(), result.coordinateX(), result.coordinateY(), observedAtMs, source);
        log.info("Window player location memory updated from {}: windowId={} map={} coord=({}, {})",
                source, context.getWindowId(), result.mapName(), result.coordinateX(), result.coordinateY());
    }

    static WindowPathingState classifyRecognizedPathingState(WindowPathingIntent intent,
                                                             String currentMapName,
                                                             int currentX,
                                                             int currentY,
                                                             long stableMs,
                                                             long intentAgeMs,
                                                             boolean recognizedLocationChanged,
                                                             long recognizedStationaryMs) {
        boolean arrived = intent.getType()
                != com.bot.dhxy.window.model.WindowPathingIntentType.UNTARGETED_TRACKER
                && hasArrived(intent, currentMapName, currentX, currentY);
        boolean coordinateTarget = intent.getTargetX() != null && intent.getTargetY() != null;
        if (arrived && (!coordinateTarget
                || (stableMs >= LOCAL_PATHING_ARRIVAL_STATIONARY_MS
                && intentAgeMs >= LOCAL_PATHING_ARRIVAL_STATIONARY_MS))) {
            return WindowPathingState.ARRIVED;
        }
        if (recognizedLocationChanged) {
            return WindowPathingState.ACTIVE;
        }
        if (stableMs >= LOCAL_PATHING_STOPPED_AWAY_MS
                && intentAgeMs >= LOCAL_PATHING_STOPPED_AWAY_MS
                && recognizedStationaryMs >= LOCAL_PATHING_STOPPED_AWAY_MS) {
            return WindowPathingState.STOPPED_AWAY;
        }
        return WindowPathingState.ACTIVE;
    }

    /** Logical coordinate changes keep terminal classification ACTIVE; they are not movement proof. */
    static boolean hasRecognizedPathingCoordinateChanged(
            Integer previousX, Integer previousY, int currentX, int currentY) {
        return previousX == null
                || previousY == null
                || previousX != currentX
                || previousY != currentY;
    }

    private static boolean hasArrived(WindowPathingIntent intent,
                                      String currentMapName,
                                      int currentX,
                                      int currentY) {
        if (intent.getTargetMapName() != null
                && currentMapName != null
                && !intent.getTargetMapName().equals(currentMapName)) {
            return false;
        }
        if (intent.getTargetX() == null || intent.getTargetY() == null) {
            return intent.getTargetMapName() == null || intent.getTargetMapName().equals(currentMapName);
        }
        int tolerance = Math.max(0, intent.getTolerance());
        return Math.abs(currentX - intent.getTargetX()) <= tolerance
                && Math.abs(currentY - intent.getTargetY()) <= tolerance;
    }

    /**
     * Maps the exact current runtime snapshot into a geometry-free typed fact. No capture, service call or action is
     * performed. The previous identity is retained only as lineage so clear/replacement cannot be ambiguous.
     */
    private List<ObservationPathingFact> sampleCurrentPathingFact() {
        long resetGeneration = context.getObservationPathingFactResetGeneration();
        if (resetGeneration != consumedPathingFactResetGeneration) {
            consumedPathingFactResetGeneration = resetGeneration;
            if (lastPathingFact != null) {
                log.info("[observation] pathing-fact lineage dropped by recovery reset: windowId={} droppedIntentId={} transition={}",
                        context.getWindowId(), lastPathingFact.intentId(), lastPathingFact.transition());
            }
            lastPathingFact = null;
            clearedPathingFactDelivered = false;
        }
        WindowPathingSnapshot snapshot = context.getPathingSnapshot();
        WindowNativeBinding binding = context.getNativeBinding();
        if (snapshot == null || binding == null || !binding.hasNativeHandle()) {
            return List.of();
        }
        WindowPathingIntent intent = snapshot.getIntent();
        if (intent == null || snapshot.getState() == WindowPathingState.NONE) {
            if (lastPathingFact == null) {
                return List.of();
            }
            if (clearedPathingFactDelivered
                    && lastPathingFact.transition() == ObservationPathingTransition.CLEARED) {
                // CLEARED handshake completed: the Cloud accepted this clear once, so stop
                // resending it. The fact object itself is retained as lineage for the next
                // REPLACED edge.
                return List.of();
            }
            if (lastPathingFact.transition() != ObservationPathingTransition.CLEARED) {
                clearedPathingFactDelivered = false;
                long clearedAtMs = Math.max(lastPathingFact.pathingStartedAtMs(),
                        Math.max(snapshot.getUpdatedAtMs(), System.currentTimeMillis()));
                lastPathingFact = new ObservationPathingFact(
                        taskRunId,
                        context.getWindowId(),
                        binding.getNativeHandle(),
                        lastPathingFact.intentId(),
                        null,
                        lastPathingFact.source(),
                        lastPathingFact.targetMapName(),
                        lastPathingFact.targetX(),
                        lastPathingFact.targetY(),
                        lastPathingFact.tolerance(),
                        lastPathingFact.pathingType(),
                        lastPathingFact.pathingStartedAtMs(),
                        clearedAtMs,
                        ObservationPathingState.NONE,
                        ObservationPathingTransition.CLEARED,
                        null,
                        null,
                        null,
                        0L,
                        false,
                        false,
                        null,
                        0L);
            }
            return List.of(lastPathingFact);
        }

        String previousIntentId = lastPathingFact == null ? null : lastPathingFact.intentId();
        boolean sameIntent = intent.getIntentId() != null && intent.getIntentId().equals(previousIntentId);
        /*
         * REPLACED is the one-frame lineage edge that introduces a new intent. Once that edge has
         * been sent, later ACTIVE/terminal observations for the same intent are CURRENT updates.
         * Repeating REPLACED would ask Cloud to replace the already-replaced predecessor again,
         * causing the terminal fact for the new intent to be rejected by its forward-only guard.
         */
        String replacedIntentId = sameIntent ? null : previousIntentId;
        ObservationPathingTransition transition = replacedIntentId == null
                ? ObservationPathingTransition.CURRENT
                : ObservationPathingTransition.REPLACED;
        long mappedAtMs = System.currentTimeMillis();
        long pathingStartedAtMs = sameIntent && lastPathingFact != null
                ? lastPathingFact.pathingStartedAtMs()
                : Math.max(1L, Math.min(intent.getCreatedAtMs(), mappedAtMs));
        long pathingUpdatedAtMs = Math.max(pathingStartedAtMs,
                Math.min(snapshot.getUpdatedAtMs(), mappedAtMs));
        long locationChangedAtMs = normalizeOptionalPathingTimestamp(
                snapshot.getLocationChangedAtMs(), pathingStartedAtMs, pathingUpdatedAtMs);
        long dialogBlockingDetectedAtMs = snapshot.isDialogBlocking()
                ? normalizeRequiredPathingTimestamp(
                        snapshot.getDialogBlockingDetectedAtMs(), pathingStartedAtMs, pathingUpdatedAtMs)
                : 0L;
        ObservationPathingFact current = new ObservationPathingFact(
                taskRunId,
                context.getWindowId(),
                binding.getNativeHandle(),
                intent.getIntentId(),
                replacedIntentId,
                intent.getSource(),
                intent.getTargetMapName(),
                intent.getTargetX(),
                intent.getTargetY(),
                intent.getTolerance(),
                ObservationPathingType.valueOf(intent.getType().name()),
                pathingStartedAtMs,
                pathingUpdatedAtMs,
                ObservationPathingState.valueOf(snapshot.getState().name()),
                transition,
                snapshot.getCurrentMapName(),
                snapshot.getCurrentX(),
                snapshot.getCurrentY(),
                locationChangedAtMs,
                snapshot.isCoordinateMovementObserved(),
                snapshot.isDialogBlocking(),
                snapshot.isDialogBlocking() ? snapshot.getDialogBlockingReason() : null,
                dialogBlockingDetectedAtMs,
                (snapshot.getState() == WindowPathingState.ARRIVED
                                || snapshot.getState() == WindowPathingState.STABLE)
                                && Objects.equals(intent.getIntentId(), lastTerminalFrameIntentId)
                        ? lastTerminalFrameId : null,
                (snapshot.getState() == WindowPathingState.ARRIVED
                                || snapshot.getState() == WindowPathingState.STABLE)
                                && Objects.equals(intent.getIntentId(), lastTerminalFrameIntentId)
                        ? lastTerminalFrameGeneration : null);
        lastPathingFact = current;
        clearedPathingFactDelivered = false;
        return List.of(current);
    }

    /**
     * Completes the CLEARED handshake after one successful transport delivery: the Cloud has
     * recorded this clear, so the sampler stops resending it while keeping the fact as lineage
     * for the next REPLACED edge.
     */
    public void acknowledgeDeliveredPathingFacts(List<ObservationPathingFact> delivered) {
        if (delivered == null || delivered.isEmpty() || lastPathingFact == null
                || lastPathingFact.transition() != ObservationPathingTransition.CLEARED
                || clearedPathingFactDelivered) {
            return;
        }
        for (ObservationPathingFact fact : delivered) {
            if (fact != null
                    && fact.transition() == ObservationPathingTransition.CLEARED
                    && Objects.equals(fact.intentId(), lastPathingFact.intentId())) {
                clearedPathingFactDelivered = true;
                return;
            }
        }
    }

    /**
     * Suppresses the retained CLEARED fact after the Cloud semantically rejected the batch
     * (HTTP 400): resending identical content can never succeed, and one poisoned fact must not
     * dead-lock the whole observation plane. Lineage is retained.
     */
    public void suppressRejectedPathingFact(String reason) {
        if (lastPathingFact == null
                || lastPathingFact.transition() != ObservationPathingTransition.CLEARED
                || clearedPathingFactDelivered) {
            return;
        }
        log.warn("[observation] suppressing Cloud-rejected CLEARED pathing fact: windowId={} intentId={} reason={}",
                context.getWindowId(), lastPathingFact.intentId(),
                reason == null ? "" : reason.substring(0, Math.min(200, reason.length())));
        clearedPathingFactDelivered = true;
    }

    private static long normalizeOptionalPathingTimestamp(long value, long startedAtMs, long updatedAtMs) {
        if (value <= 0L) {
            return 0L;
        }
        return Math.max(startedAtMs, Math.min(value, updatedAtMs));
    }

    private static long normalizeRequiredPathingTimestamp(long value, long startedAtMs, long updatedAtMs) {
        long normalized = normalizeOptionalPathingTimestamp(value, startedAtMs, updatedAtMs);
        return normalized == 0L ? startedAtMs : normalized;
    }

    /**
     * Maps the exact current local dialog declaration into a typed observation fact. This is a
     * read-only snapshot: no detector, local service, task transition or action is invoked.
     */
    private List<ObservationDialogInterestFact> sampleCurrentDialogInterestFact() {
        WindowRuntimeContext.XiuluoKandaProbeView view = context.getXiuluoKandaProbeView();
        WindowDialogInterest interest = view.interest();
        WindowNativeBinding binding = context.getNativeBinding();
        if (binding == null || !binding.hasNativeHandle()) {
            return List.of();
        }
        if (interest == null) {
            if (lastDialogInterestFact == null || !lastDialogInterestFact.active()) {
                return List.of();
            }
            lastDialogInterestFact = new ObservationDialogInterestFact(
                    taskRunId, context.getWindowId(), binding.getNativeHandle(),
                    lastDialogInterestFact.interestId(), false, null, List.of(), null,
                    0L, 0L, 0L, 0L, false, null, null, null, false);
            return List.of(lastDialogInterestFact);
        }

        WindowPathingSnapshot pathing = context.getPathingSnapshot();
        String intentId = pathing == null || pathing.getIntent() == null
                ? null : pathing.getIntent().getIntentId();
        XiuluoGreenChainSchedule schedule = view.schedule();
        boolean exactSchedule = schedule != null && scheduleBelongsToThisRun(schedule)
                && Objects.equals(schedule.getHwnd(), binding.getNativeHandle());
        String source = interest.getSource() == null || interest.getSource().isBlank()
                ? "dialog-interest" : interest.getSource();
        String interestId = source + ":" + interest.getCreatedAtMs();
        List<String> operations = interest.getOperations() == null
                ? List.of()
                : interest.getOperations().stream().map(Enum::name).distinct().toList();
        lastDialogInterestFact = new ObservationDialogInterestFact(
                taskRunId,
                context.getWindowId(),
                binding.getNativeHandle(),
                interestId,
                true,
                interest.getTaskType() == null ? null : interest.getTaskType().getCode(),
                operations,
                source,
                interest.getCreatedAtMs(),
                interest.getExpiresAtMs(),
                interest.getAbsentAllowedAtMs(),
                interest.getProbeStartAtMs(),
                interest.isLocalTemplateProbeOnly(),
                exactSchedule ? schedule.getAttemptId() : null,
                exactSchedule ? schedule.getRound() : null,
                intentId,
                exactSchedule && view.enterBattleClaimed());
        return List.of(lastDialogInterestFact);
    }

    /**
     * TURN-40G §6: the restored xiuluo local-kanda fast path — the sole approved active-input exception. Enabled
     * only while the window's own registered xiuluo dialog interest carries {@code XIULUO_ENTER_BATTLE} in
     * probe-only mode past the CR253 timing anchor AND an open green-chain attempt exists. An ordinary miss does
     * nothing (no Cloud request, no event, no interest change). A raw hit is revalidated on a fresh frame, must
     * win the attempt-scoped one-shot CAS, and then submits exactly one atomic move+click+delay request through
     * the single input queue. Click success waits for Runner-confirmed {@code IN_COMBAT}; no combat within four
     * seconds re-arms the same attempt for at most two more local clicks. Only the final local exhaustion is
     * reported to Cloud, which alone may decide a saved-green fallback.
     */
    private void sampleXiuluoLocalKanda(long now,
                                        List<ObservationKeyEvent> events,
                                        List<ObservationRoi> rois) {
        // TURN-40G review#3 P1: the interest+schedule pair is read as ONE consistent snapshot (same monitor as
        // the paired install/replace transition) — never a new interest with the previous attempt's schedule.
        WindowRuntimeContext.XiuluoKandaProbeView view = context.getXiuluoKandaProbeView();
        WindowDialogInterest interest = view.interest();
        if (interest == null) {
            return;
        }
        if (interest.getTaskType() != TaskType.XIULUO_V2
                && interest.getTaskType() != TaskType.XINSHOU_TRAINING
                && interest.getTaskType() != TaskType.CATCH_GHOST
                && interest.getTaskType() != TaskType.GHOST_KING) {
            return;
        }
        if (interest.getOperations() == null
                || !interest.getOperations().contains(DialogOperation.XIULUO_ENTER_BATTLE)) {
            return;
        }
        if (!interest.isLocalTemplateProbeOnly()) {
            log.info("Local-kanda probe blocked: windowId={} task={} reason=not-probe-only source={}",
                    context.getWindowId(), interest.getTaskType(), interest.getSource());
            return;
        }
        if (!interest.isProbeStartReached(now)) {
            log.info("Local-kanda probe blocked: windowId={} task={} reason=probe-delay nowMs={} probeStartAtMs={} remainingMs={} source={}",
                    context.getWindowId(), interest.getTaskType(), now, interest.getProbeStartAtMs(),
                    interest.getProbeStartAtMs() - now, interest.getSource());
            return;
        }
        XiuluoGreenChainSchedule schedule = view.schedule();
        if (schedule == null) {
            log.info("Local-kanda probe blocked: windowId={} task={} reason=no-schedule source={}",
                    context.getWindowId(), interest.getTaskType(), interest.getSource());
            return;
        }
        // TURN-40G review#4: BEFORE the matcher runs, fence to this sampler's authoritative run identity. An
        // old runner that outlives its task run and sees the new run's paired schedule must do nothing here —
        // no matcher, no capture, no input.
        if (!scheduleBelongsToThisRun(schedule)) {
            log.info("Local-kanda skipped: schedule belongs to a different run: windowId={} samplerTaskRunId={} scheduleTaskRunId={} scheduleWindowId={}",
                    context.getWindowId(), taskRunId, schedule.getTaskRunId(), schedule.getWindowId());
            return;
        }
        WindowNativeBinding binding = context.getNativeBinding();
        if (binding == null || !binding.hasNativeHandle()
                || !binding.getNativeHandle().equals(schedule.getHwnd())) {
            log.info("Local-kanda probe blocked: windowId={} task={} reason=hwnd-mismatch bindingHwnd={} scheduleHwnd={} schedule={}",
                    context.getWindowId(), interest.getTaskType(),
                    binding == null ? null : binding.getNativeHandle(), schedule.getHwnd(), schedule.identityText());
            return;
        }
        WindowRuntimeContext.XiuluoKandaRetryState retryState = context.evaluateXiuluoLocalKandaRetry(
                schedule, now, localCombatVisible);
        if (retryState == WindowRuntimeContext.XiuluoKandaRetryState.WAITING_FOR_COMBAT
                || retryState == WindowRuntimeContext.XiuluoKandaRetryState.COMBAT_CONFIRMED
                || retryState == WindowRuntimeContext.XiuluoKandaRetryState.EXHAUSTED_REPORTED
                || retryState == WindowRuntimeContext.XiuluoKandaRetryState.STALE) {
            log.info("Local-kanda probe blocked: windowId={} task={} reason=retry-state state={} schedule={}",
                    context.getWindowId(), interest.getTaskType(), retryState, schedule.identityText());
            return;
        }
        if (retryState == WindowRuntimeContext.XiuluoKandaRetryState.EXHAUSTED_NEW) {
            events.add(new ObservationKeyEvent(
                    "enter-battle-click-failed-" + schedule.getAttemptId(),
                    ObservationKeyEventType.ENTER_BATTLE_CLICK_FAILED,
                    now, null, schedule.getAttemptId(), schedule.getRound(), "local-kanda",
                    "reason=no-in-combat-after-three-executed-clicks", null, null, null, null));
            return;
        }
        long epochBefore = context.getPlayerIdentityEpoch();
        log.info("Local-kanda probe matching: windowId={} task={} templateTask={} schedule={} identityEpoch={}",
                context.getWindowId(), interest.getTaskType(), interest.getTaskType(),
                schedule.identityText(), epochBefore);
        if (dialogService.findTaskEnterBattleLocalTemplate(
                interest.getTaskType(), RUNNER_SOURCE, "probe:round-" + schedule.getRound(),
                sharedCycleFrame, sharedCycleFrameRect).isEmpty()) {
            log.info("Local-kanda probe miss: windowId={} task={} schedule={} template-match=none",
                    context.getWindowId(), interest.getTaskType(), schedule.identityText());
            UICleanerService cleaner = uiCleanerService;
            if (interest.getTaskType() == TaskType.GHOST_KING && cleaner != null && sharedCycleFrame != null) {
                Boolean mapPresent = cleaner.probeMapWindowPresent(
                        sharedCycleFrame, "ghost-king:local-kanda:" + schedule.getAttemptId());
                if (Boolean.TRUE.equals(mapPresent)) {
                    boolean closed = cleaner.closeMapIfPresent(
                            "ghost-king:local-kanda:" + schedule.getAttemptId());
                    log.info("Local-kanda probe deferred for map cleanup: windowId={} task={} schedule={} closed={}",
                            context.getWindowId(), interest.getTaskType(), schedule.identityText(), closed);
                    return;
                }
            }
            if (retryState == WindowRuntimeContext.XiuluoKandaRetryState.RETRY_AVAILABLE) {
                events.add(new ObservationKeyEvent(
                        "enter-battle-click-failed-" + schedule.getAttemptId(),
                        ObservationKeyEventType.ENTER_BATTLE_CLICK_FAILED,
                        now, null, schedule.getAttemptId(), schedule.getRound(), "local-kanda",
                        "reason=template-gone-without-combat-after-executed-click",
                        null, null, null, null));
            } else if (retryState == WindowRuntimeContext.XiuluoKandaRetryState.AVAILABLE
                    && context.tryClaimXiuluoMissingKandaAfterPathingTerminal(schedule)) {
                events.add(new ObservationKeyEvent(
                        "enter-battle-click-failed-" + schedule.getAttemptId(),
                        ObservationKeyEventType.ENTER_BATTLE_CLICK_FAILED,
                        now, null, schedule.getAttemptId(), schedule.getRound(), "local-kanda",
                        "reason=template-never-appeared-after-pathing-terminal",
                        null, null, null, null));
            }
            return;
        }
        // Consume-time revalidation on a fresh frame plus binding/attempt/interest consistency re-checks.
        DialogService.LocalDialogTemplateMatch validated = dialogService.revalidateTaskEnterBattleLocalTemplate(
                interest.getTaskType(), RUNNER_SOURCE, "round-" + schedule.getRound()).orElse(null);
        if (validated == null) {
            log.info("Local-kanda probe abandoned: windowId={} task={} reason=fresh-revalidation-miss schedule={}",
                    context.getWindowId(), interest.getTaskType(), schedule.identityText());
            return;
        }
        WindowRuntimeContext.XiuluoKandaProbeView liveView = context.getXiuluoKandaProbeView();
        XiuluoGreenChainSchedule liveSchedule = liveView.schedule();
        WindowDialogInterest liveInterest = liveView.interest();
        // TURN-40G review#5: AFTER the fresh-frame revalidation, re-fence the FULL five-field schedule identity
        // (windowId, hwnd, taskRunId, round, attemptId) against the exact schedule the probe started on. A
        // schedule replaced by a different run OR a same-run round/hwnd change (even while ids collide/reuse)
        // between the probe and the revalidation aborts with no click.
        if (liveSchedule == null
                || !liveSchedule.sameFullIdentity(schedule)
                || liveInterest == null
                || !liveInterest.isLocalTemplateProbeOnly()
                || context.getPlayerIdentityEpoch() != epochBefore) {
            log.info("Local-kanda probe abandoned: windowId={} task={} reason=post-match-fence"
                            + " expectedSchedule={} liveSchedule={} expectedProbeOnly=true liveProbeOnly={}"
                            + " identityEpochBefore={} identityEpochNow={}",
                    context.getWindowId(), interest.getTaskType(), schedule.identityText(),
                    liveSchedule == null ? null : liveSchedule.identityText(),
                    liveInterest != null && liveInterest.isLocalTemplateProbeOnly(),
                    epochBefore, context.getPlayerIdentityEpoch());
            return;
        }
        // The claim compares the live schedule's full identity to this exact captured schedule atomically under
        // the kanda monitor, so a stale/replaced schedule can never win the CAS.
        if (!context.tryClaimXiuluoEnterBattleClick(schedule, "local-kanda-hit")) {
            log.info("Local-kanda hit superseded (attempt already claimed or schedule replaced): windowId={} expected=[{}]",
                    context.getWindowId(), schedule.identityText());
            return;
        }
        ObservationRoi preCombatRoi = capturePreCombatCoordinateFrame();
        String claimId = UUID.randomUUID().toString();
        if (!context.registerExpectedCombatEnterClaim(new com.bot.dhxy.window.model.WindowExpectedCombatEnterClaim(
                claimId,
                taskRunId,
                schedule.getTaskRunId(),
                interest.getTaskType().getCode(),
                schedule.getAttemptId(),
                context.getWindowId(),
                binding.getNativeHandle(),
                "local-template",
                null))) {
            context.releaseXiuluoEnterBattleClick(schedule, "expected-combat-claim-rejected");
            return;
        }
        try {
            inputSequences.moveAndClickLeftAsync(
                            "xiuluo-40g:local-kanda:" + schedule.getRound(),
                            validated.absoluteX(), validated.absoluteY(), 80, 150)
                    .whenComplete((clicked, inputFailure) -> {
                        if (inputFailure != null || !Boolean.TRUE.equals(clicked)) {
                            // Release only this exact asynchronous attempt. A newer expected claim or
                            // schedule must never be cleared by a late completion.
                            context.clearExpectedCombatEnterClaim(claimId, "local-kanda-input-failed");
                            context.releaseXiuluoEnterBattleClick(schedule, "click-not-executed");
                            log.warn("Local-kanda async click failed: windowId={} schedule={} type={} message={}",
                                    context.getWindowId(), schedule.identityText(),
                                    inputFailure == null ? null : inputFailure.getClass().getSimpleName(),
                                    inputFailure == null ? null : inputFailure.getMessage());
                            return;
                        }
                        long clickedAtMs = System.currentTimeMillis();
                        context.recordXiuluoLocalKandaClick(schedule, clickedAtMs);
                        if (preCombatRoi != null) {
                            pendingAsyncRois.put(preCombatRoi.roiKey(), preCombatRoi);
                        }
                        publishAsyncEvent(new ObservationKeyEvent(
                                "enter-battle-clicked-" + schedule.getAttemptId(),
                                ObservationKeyEventType.ENTER_BATTLE_CLICKED,
                                clickedAtMs,
                                null,
                                schedule.getAttemptId(),
                                schedule.getRound(),
                                "local-kanda",
                                "clickX=" + validated.absoluteX() + "|clickY=" + validated.absoluteY()
                                        + "|score=" + validated.score() + "|executed=true",
                                claimId,
                                null,
                                interest.getTaskType().getCode(),
                                schedule.getTaskRunId()));
                    });
        } catch (RuntimeException inputFailure) {
            context.clearExpectedCombatEnterClaim(claimId, "local-kanda-submit-failed");
            context.releaseXiuluoEnterBattleClick(schedule, "click-submit-failed");
            log.warn("Local-kanda async submit failed: windowId={} schedule={} type={} message={}",
                    context.getWindowId(), schedule.identityText(), inputFailure.getClass().getSimpleName(),
                    inputFailure.getMessage());
        }
    }

    /**
     * Reuses this cycle's complete position strip immediately before an already-confirmed local
     * enter-battle click. Encoding/upload happens after the click and never requests another HWND capture.
     */
    private ObservationRoi capturePreCombatCoordinateFrame() {
        byte[] pngBytes = encodePng(sharedPositionStripFrame);
        if (pngBytes == null) {
            return null;
        }
        return new ObservationRoi(
                PRE_COMBAT_COORDINATE_STRIP_ROI,
                PATHING_COORDINATE_STRIP_X,
                PATHING_COORDINATE_STRIP_Y,
                PATHING_COORDINATE_STRIP_WIDTH,
                PATHING_COORDINATE_STRIP_HEIGHT,
                pngBytes);
    }

    /**
     * The local pre-battle timer edge: same predicate and same one-shot CAS
     * ({@link WindowRuntimeContext#markOrdinaryPreBattleTimeoutPublished(long)}) the frozen local operation
     * executor uses, so the edge publishes exactly once per timer start regardless of which side evaluates it.
     */
    private void samplePreBattleTimerEdge(long now, List<ObservationKeyEvent> events) {
        long startedAt = context.getOrdinaryPreBattleStartedAtMs();
        if (startedAt <= 0L || now - startedAt < PRE_BATTLE_TIMEOUT_MS) {
            return;
        }
        if (!context.markOrdinaryPreBattleTimeoutPublished(now)) {
            return;
        }
        long publishedAt = context.getOrdinaryPreBattleTimeoutPublishedAtMs();
        String taskType = context.getOrdinaryPreBattleTaskType() == null
                ? "" : context.getOrdinaryPreBattleTaskType().name();
        events.add(new ObservationKeyEvent(
                "prebattle-timeout-" + context.getWindowId() + "-" + publishedAt,
                ObservationKeyEventType.PRE_BATTLE_TIMEOUT,
                publishedAt,
                null,
                null,
                null,
                context.getOrdinaryPreBattleSource(),
                "taskType=" + taskType
                        + "|targetKeyword=" + safe(context.getOrdinaryPreBattleTargetKeyword())
                        + "|startedAtMs=" + startedAt
                        + "|publishedAtMs=" + publishedAt));
    }

    /**
     * Captures the interest's exact window-relative ROI and encodes it as a PNG upload.
     *
     * @return whether this duty's sampling slot was consumed — {@code true} for an uploaded frame or a
     *         deliberate local-kanda suppression, {@code false} when no frame existed to sample, so the
     *         caller retries on the next cycle instead of charging a full period for nothing.
     */
    private boolean sampleRoi(ObservationInterest interest,
                              ObservationDialogInterestFact dialogInterest,
                              String pathingIntentId,
                              long observerSeq,
                              long observedAt,
                              List<ObservationFact> facts,
                              List<ObservationRoi> rois,
                              boolean forceXinshouRefresh) {
        if ("xiuluo-dialog".equals(interest.interestKey())
                && dialogInterest != null && dialogInterest.enterBattleClaimed()) {
            return true;
        }
        int[] rect = coordinateHelper.getScaledRect(
                interest.roiLeft(), interest.roiTop(), interest.roiWidth(), interest.roiHeight());
        boolean coordinateStrip = COORDINATE_STRIP_INTEREST.equals(interest.interestKey());
        BufferedImage image = cropSharedCycleFrame(rect);
        if (image == null) {
            if (coordinateStrip) {
                coordinateFramesUnavailable++;
            }
            return false;
        }
        try {
            if (XINSHOU_DIALOG_INTEREST.equals(interest.interestKey())) {
                boolean present = dialogFramePresenceMechanics.isPresent(image);
                sampleXinshouFact(
                        observerSeq,
                        ObservationFactType.XINSHOU_DIALOG_PRESENCE,
                        present ? "present" : "absent",
                        observedAt,
                        facts);
                if (!present) {
                    // Absence is explicit structural evidence, but carries no image. Forget the previous
                    // dialog hash so an identical dialog reopening is uploaded as a fresh event.
                    deliveredXinshouRois.remove(interest.interestKey());
                    sampledXinshouRois.remove(interest.interestKey());
                    observedXinshouRoiHashes.remove(interest.interestKey());
                    return true;
                }
            }
            if (isXinshouChangeInterest(interest.interestKey())) {
                String contentHash = hashImageContent(image);
                String previousObserved =
                        observedXinshouRoiHashes.put(interest.interestKey(), contentHash);
                if (!Objects.equals(previousObserved, contentHash)) {
                    markXinshouEffectiveProgress(observedAt);
                }
                XinshouRoiVersion delivered = deliveredXinshouRois.get(interest.interestKey());
                if (!forceXinshouRefresh
                        && delivered != null
                        && delivered.contentHash().equals(contentHash)) {
                    sampledXinshouRois.remove(interest.interestKey());
                    return true;
                }
                sampledXinshouRois.put(
                        interest.interestKey(), new XinshouRoiVersion(contentHash, observerSeq));
            }
            ByteArrayOutputStream png = new ByteArrayOutputStream(4096);
            ImageIO.write(image, "png", png);
            boolean dialogRoi = "xiuluo-dialog".equals(interest.interestKey())
                    && dialogInterest != null && dialogInterest.active();
            rois.add(new ObservationRoi(
                    interest.interestKey(),
                    interest.roiLeft(),
                    interest.roiTop(),
                    interest.roiWidth(),
                    interest.roiHeight(),
                    png.toByteArray(),
                    // A Cloud-issued ROI detail is an opaque correlation id. Echo it on ordinary
                    // shared-cycle ROI evidence too, so a post-input verifier can reject frames that
                    // belong to another decision without requesting a second capture.
                    dialogRoi ? dialogInterest.interestId() : interest.detail(),
                    dialogRoi
                            ? dialogInterest.intentId()
                            : coordinateStrip ? pathingIntentId : null,
                    dialogRoi ? dialogInterest.attemptId() : null,
                    dialogRoi ? dialogInterest.round() : null));
            if (forceXinshouRefresh && xinshouRefreshPending
                    && isXinshouChangeInterest(interest.interestKey())) {
                xinshouRefreshObserverSeq = observerSeq;
                if (facts.stream().noneMatch(fact ->
                        fact.factType() == ObservationFactType.XINSHOU_NO_PROGRESS_REFRESH)) {
                    facts.add(new ObservationFact(
                            ObservationFactType.XINSHOU_NO_PROGRESS_REFRESH,
                            "no-progress",
                            observedAt));
                }
            }
            if (coordinateStrip) {
                coordinateFramesCaptured++;
            }
            return true;
        } catch (IOException encodeFailure) {
            if (coordinateStrip) {
                coordinateFramesUnavailable++;
            }
            log.debug("Observation ROI encode failed (no frame uploaded): windowId={} interest={}",
                    context.getWindowId(), interest.interestKey());
            return false;
        } finally {
            image.flush();
        }
    }

    /**
     * Commits only the new-player ROI versions carried by a successfully accepted observation request.
     * A failed transport leaves the candidate uncommitted, so the next cycle resends the same or newer frame.
     */
    void acknowledgeDeliveredRois(long observerSeq, List<ObservationRoi> deliveredRois) {
        if (deliveredRois == null || deliveredRois.isEmpty()) {
            return;
        }
        for (ObservationRoi roi : deliveredRois) {
            if (roi != null) {
                pendingAsyncRois.remove(roi.roiKey(), roi);
            }
            if (roi == null || !isXinshouChangeInterest(roi.roiKey())) {
                continue;
            }
            XinshouRoiVersion sampled = sampledXinshouRois.get(roi.roiKey());
            if (sampled != null && sampled.observerSeq() == observerSeq) {
                deliveredXinshouRois.put(roi.roiKey(), sampled);
                sampledXinshouRois.remove(roi.roiKey());
            }
        }
        if (xinshouRefreshPending
                && xinshouRefreshObserverSeq == observerSeq
                && deliveredRois.stream().anyMatch(roi ->
                        roi != null && isXinshouChangeInterest(roi.roiKey()))) {
            xinshouRefreshPending = false;
            xinshouRefreshObserverSeq = 0L;
            lastXinshouRefreshAcknowledgedAtMs = System.currentTimeMillis();
        }
    }

    /**
     * Commits versioned facts only when Cloud accepted the exact request that carried them.
     * A transport failure or stale acknowledgement leaves candidates uncommitted, so the next
     * observation cycle resends the current values.
     */
    void acknowledgeDeliveredFacts(long observerSeq, List<ObservationFact> deliveredFacts) {
        if (deliveredFacts == null || deliveredFacts.isEmpty()) {
            return;
        }
        for (ObservationFact fact : deliveredFacts) {
            if (fact == null) {
                continue;
            }
            if (!isVersionedXinshouFact(fact.factType())) {
                continue;
            }
            XinshouFactVersion sampled = sampledXinshouFacts.get(fact.factType());
            if (sampled != null
                    && sampled.observerSeq() == observerSeq
                    && sampled.value().equals(fact.value())) {
                deliveredXinshouFacts.put(fact.factType(), sampled);
                sampledXinshouFacts.remove(fact.factType());
            }
        }
    }

    /**
     * Match an explicitly armed 天庭/鬼王 dialog option on this cycle's shared frame and click locally.
     *
     * <p>The whole point of keeping these templates on the client is that a visible option needs no
     * cloud round trip. Nothing happens until the task installs one explicit local-template interest,
     * so this duty is inert for every other task and for quiet phases.</p>
     *
     * <p>A hit is re-matched on a fresh capture immediately before the click, the same guard the 修罗
     * 看打 probe uses: the shared frame may be up to one cycle old, and clicking a dialog that has
     * already closed would land on whatever replaced it.</p>
     */
    private void sampleTiantingDialogProbe(long now,
                                           List<ObservationKeyEvent> events,
                                           List<ObservationRoi> rois) {
        WindowDialogInterest interest = context.getDialogInterest().orElse(null);
        if (interest == null
                || interest.getOperations() == null
                || !interest.isLocalTemplateProbeOnly()
                || !interest.isProbeStartReached(now)
                || interest.isExpired(now)) {
            return;
        }
        /*
         * One duty, one explicitly selected option set. Narrow sets own their normal phases; the broad
         * recovery set is armed only after the exact tracker click started no movement.
         */
        TiantingOptionSet optionSet = TiantingOptionSet.from(interest.getOperations());
        if (optionSet == null || !optionSet.supports(interest.getTaskType())) {
            return;
        }
        /*
         * 引妖香 is owned by one prepared Tracker task. A miss leaves no claim and is retried on later
         * frames; once that option has matched and claimed this interest, stop before capture/matching.
         * The next Tracker task installs a new interest with a new creation identity and re-arms naturally.
         */
        if (optionSet == TiantingOptionSet.YINYAO
                && context.hasTiantingDialogOptionClaim(
                        interest.getCreatedAtMs() + "|" + TiantingDialogLocalMechanics.ACTION_YINYAO)) {
            return;
        }
        /*
         * The interest carries no run id, so the run fence available here is the window binding plus
         * the player-identity epoch: a sampler whose window was rebound or whose character changed
         * under it must not click for whatever now owns that window.
         */
        WindowNativeBinding binding = context.getNativeBinding();
        if (binding == null || !binding.hasNativeHandle()) {
            return;
        }
        long epochBefore = context.getPlayerIdentityEpoch();
        int[] rect = coordinateHelper.getScaledRect(
                TiantingDialogLocalMechanics.DIALOG_ROI_LEFT,
                TiantingDialogLocalMechanics.DIALOG_ROI_TOP,
                TiantingDialogLocalMechanics.DIALOG_ROI_WIDTH,
                TiantingDialogLocalMechanics.DIALOG_ROI_HEIGHT);
        BufferedImage shared = cropSharedCycleFrame(rect);
        if (shared == null) {
            return;
        }
        TiantingDialogLocalMechanics.OptionHit hit;
        BufferedImage firstAcceptFrame = null;
        try {
            /*
             * 使用封妖符 is offered after 多谢 is answered, and it is matched here on top of whatever set
             * the cloud armed. It has to be: the answer to 多谢 needs a sampling cycle and
             * two round trips to reach the cloud, so an interest armed from there arrives after the option
             * has gone — the branch would then never start, and nothing in the log would say so. The
             * window is opened below by the very click that causes the option to appear.
             */
            boolean allowFengyaoFollowUp = context.isTiantingFengyaoPending()
                    && optionSet != TiantingOptionSet.ACCEPT
                    && optionSet != TiantingOptionSet.CANCEL
                    && optionSet != TiantingOptionSet.YINYAO;
            hit = allowFengyaoFollowUp
                    ? TiantingDialogLocalMechanics.matchFengyaoOption(shared)
                            .or(() -> optionSet.match(shared))
                            .orElse(null)
                    : optionSet.match(shared).orElse(null);
            if (hit != null && TiantingDialogLocalMechanics.ACCEPT.equals(hit.templatePath())) {
                firstAcceptFrame = copyImage(shared);
            }
        } finally {
            shared.flush();
        }
        if (hit == null && optionSet == TiantingOptionSet.RECOVERY
                && !context.isTiantingFengyaoPending()) {
            /*
             * A timeout is not proof that all known options missed. Re-read the exact bound HWND and
             * publish a retained terminal only when a real frame was available and the whole recovery
             * set missed again. Cloud may run its generic fallback only after this event.
             */
            BufferedImage freshMissFrame = tracker.captureToMemory(
                    "tianting-dialog-recovery:all-miss", rect[0], rect[1], rect[2], rect[3]);
            if (freshMissFrame == null) {
                return;
            }
            try {
                hit = optionSet.match(freshMissFrame).orElse(null);
            } finally {
                freshMissFrame.flush();
            }
            if (hit == null) {
                events.add(new ObservationKeyEvent(
                        "tianting-recovery-all-missed-" + interest.getCreatedAtMs(),
                        ObservationKeyEventType.TIANTING_RECOVERY_ALL_MISSED,
                        now,
                        null,
                        null,
                        0,
                        "tianting-dialog-recovery",
                        "probeCorrelation=" + interest.getSource()));
                context.clearTiantingDialogOptionClaim();
                return;
            }
        }
        if (hit == null) {
            // No option on screen means the dialog this probe last answered is gone, so the next one
            // may be answered again. This is the only place the one-shot claim re-arms.
            context.clearTiantingDialogOptionClaim();
            return;
        }
        /*
         * The interest deliberately stays installed across the click (the task keeps it armed for the
         * whole移动 leg), so nothing upstream stops a second click while the dialog is still closing.
         * Without this latch the same 看打 gets clicked several times, and the extra clicks land on
         * the combat screen that replaced it.
         */
        String optionKey = interest.getCreatedAtMs() + "|" + hit.actionKey();
        if (context.hasTiantingDialogOptionClaim(optionKey)) {
            return;
        }
        BufferedImage fresh = tracker.captureToMemory(
                "tianting-dialog-option:revalidate", rect[0], rect[1], rect[2], rect[3]);
        if (fresh == null) {
            return;
        }
        TiantingDialogLocalMechanics.OptionHit validated;
        BufferedImage freshAcceptFrame = null;
        try {
            // Revalidated against the same rule the shared frame was matched with, or a 封妖符 hit would
            // be dropped here as "the dialog changed".
            boolean allowFengyaoFollowUp = context.isTiantingFengyaoPending()
                    && optionSet != TiantingOptionSet.ACCEPT
                    && optionSet != TiantingOptionSet.CANCEL
                    && optionSet != TiantingOptionSet.YINYAO;
            validated = allowFengyaoFollowUp
                    ? TiantingDialogLocalMechanics.matchFengyaoOption(fresh)
                            .or(() -> optionSet.match(fresh))
                            .orElse(null)
                    : optionSet.match(fresh).orElse(null);
            if (validated != null && TiantingDialogLocalMechanics.ACCEPT.equals(validated.templatePath())) {
                freshAcceptFrame = copyImage(fresh);
            }
        } finally {
            fresh.flush();
        }
        if (validated == null || !validated.actionKey().equals(hit.actionKey())
                || !validated.templatePath().equals(hit.templatePath())
                || !interest.equals(context.getDialogInterest().orElse(null))
                || context.getPlayerIdentityEpoch() != epochBefore
                || !binding.equals(context.getNativeBinding())) {
            // The dialog, owning interest, or window changed between the shared frame and now. Report
            // nothing and let the current interest decide on a frame that still belongs to it.
            log.info("Tianting dialog option abandoned before click: windowId={} shared={} fresh={}",
                    context.getWindowId(), templateName(hit.templatePath()),
                    validated == null ? "-" : templateName(validated.templatePath()));
            return;
        }
        /*
         * Claim immediately before the click and only after the fresh frame agreed: a failed
         * revalidation must not burn the claim (the dialog is still there and still unanswered),
         * while an input that throws must still consume it — a retry loop on a dialog that may have
         * already accepted the click is worse than one lost report.
         */
        if (!context.tryClaimTiantingDialogOption(optionKey)) {
            return;
        }
        byte[] firstPng;
        byte[] freshPng;
        try {
            firstPng = encodePng(firstAcceptFrame);
            freshPng = encodePng(freshAcceptFrame);
        } finally {
            if (firstAcceptFrame != null) {
                firstAcceptFrame.flush();
            }
            if (freshAcceptFrame != null) {
                freshAcceptFrame.flush();
            }
        }
        int optionX = rect[0] + validated.roiOffsetX();
        int optionY = rect[1] + validated.roiOffsetY();
        boolean ghostKingAccept = optionSet == TiantingOptionSet.GHOST_KING_ACCEPT;
        try {
            /*
             * Clicking 引妖香 redraws the Tracker panel. The global input worker owns the atomic
             * move/click, while this sampler returns immediately and keeps observing combat/pathing.
             */
            inputSequences.moveAndClickLeftAsync(
                            ghostKingAccept ? "ghost-king:dialog-option" : "tianting:dialog-option",
                            optionX, optionY, 80, 150)
                    .whenComplete((clicked, inputFailure) -> {
                        boolean executed = inputFailure == null && Boolean.TRUE.equals(clicked);
                        if (executed && firstPng != null && freshPng != null) {
                            ObservationRoi firstRoi = new ObservationRoi("tianting-daily-count-first",
                                    TiantingDialogLocalMechanics.DIALOG_ROI_LEFT,
                                    TiantingDialogLocalMechanics.DIALOG_ROI_TOP,
                                    TiantingDialogLocalMechanics.DIALOG_ROI_WIDTH,
                                    TiantingDialogLocalMechanics.DIALOG_ROI_HEIGHT,
                                    firstPng);
                            ObservationRoi freshRoi = new ObservationRoi("tianting-daily-count-fresh",
                                    TiantingDialogLocalMechanics.DIALOG_ROI_LEFT,
                                    TiantingDialogLocalMechanics.DIALOG_ROI_TOP,
                                    TiantingDialogLocalMechanics.DIALOG_ROI_WIDTH,
                                    TiantingDialogLocalMechanics.DIALOG_ROI_HEIGHT,
                                    freshPng);
                            pendingAsyncRois.put(firstRoi.roiKey(), firstRoi);
                            pendingAsyncRois.put(freshRoi.roiKey(), freshRoi);
                        }
                        log.info("{} dialog option {}: windowId={} template={} score={} click=({}, {})",
                                ghostKingAccept ? "Ghost King" : "Tianting",
                                executed ? "clicked" : "click failed", context.getWindowId(),
                                templateName(validated.templatePath()), validated.score(), optionX, optionY);
                        if (executed && TiantingDialogLocalMechanics.DUOXIE.equals(validated.templatePath())) {
                            context.markTiantingFengyaoPending(optionKey);
                            log.info("Tianting 封妖符 local follow-up pending: windowId={} sourceOptionKey={}",
                                    context.getWindowId(), optionKey);
                        } else if (executed
                                && TiantingDialogLocalMechanics.FENGYAO.equals(validated.templatePath())) {
                            context.clearTiantingFengyaoPending();
                            log.info("Tianting 封妖符 local follow-up consumed: windowId={}",
                                    context.getWindowId());
                        }
                        if (executed) {
                            // NPC 点击 FIFO 用这一刻判"上一下点中 NPC 没有";见 WindowRuntimeContext 注释。
                            context.markTaskDialogOptionAnswered(validated.actionKey());
                        }
                        publishAsyncEvent(new ObservationKeyEvent(
                                (ghostKingAccept ? "ghost-king-dialog-" : "tianting-dialog-")
                                        + interest.getCreatedAtMs() + "-" + validated.actionKey(),
                                ghostKingAccept
                                        ? ObservationKeyEventType.GHOST_KING_DIALOG_CLICKED
                                        : ObservationKeyEventType.TIANTING_DIALOG_CLICKED,
                                System.currentTimeMillis(),
                                null,
                                null,
                                0,
                                ghostKingAccept ? "ghost-king-dialog-option" : "tianting-dialog-option",
                                validated.actionKey()
                                        + "|score=" + validated.score()
                                        + "|executed=" + executed
                                        + "|trackerChained=false"
                                        + "|probeCorrelation=" + interest.getSource()));
                    });
        } catch (RuntimeException inputFailure) {
            log.warn("{} dialog option async submit failed: windowId={} template={} type={} message={}",
                    ghostKingAccept ? "Ghost King" : "Tianting", context.getWindowId(),
                    templateName(validated.templatePath()), inputFailure.getClass().getSimpleName(),
                    inputFailure.getMessage());
            publishAsyncEvent(new ObservationKeyEvent(
                    (ghostKingAccept ? "ghost-king-dialog-" : "tianting-dialog-")
                            + interest.getCreatedAtMs() + "-" + validated.actionKey(),
                    ghostKingAccept
                            ? ObservationKeyEventType.GHOST_KING_DIALOG_CLICKED
                            : ObservationKeyEventType.TIANTING_DIALOG_CLICKED,
                    System.currentTimeMillis(), null, null, 0,
                    ghostKingAccept ? "ghost-king-dialog-option" : "tianting-dialog-option",
                    validated.actionKey() + "|score=" + validated.score()
                            + "|executed=false|trackerChained=false|probeCorrelation=" + interest.getSource()));
        }
    }

    /** Encodes an already-captured observation ROI without requesting another window capture. */
    private static byte[] encodePng(BufferedImage image) {
        if (image == null) {
            return null;
        }
        try {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream(4096);
            ImageIO.write(image, "png", buffer);
            return buffer.toByteArray();
        } catch (IOException ignored) {
            return null;
        }
    }

    /** Publishes an input-worker completion without making that worker depend on the next sample cycle. */
    private void publishAsyncEvent(ObservationKeyEvent event) {
        if (event == null) {
            return;
        }
        Consumer<ObservationKeyEvent> publisher = asyncEventPublisher;
        if (publisher != null) {
            publisher.accept(event);
        } else {
            pendingAsyncEvents.add(event);
        }
    }

    private static BufferedImage copyImage(BufferedImage source) {
        BufferedImage copy = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
        var graphics = copy.createGraphics();
        try {
            graphics.drawImage(source, 0, 0, null);
        } finally {
            graphics.dispose();
        }
        return copy;
    }

    /**
     * Which 天庭 option set one interest arms.
     *
     * <p>Exactly one set is live at a time, and the task decides which by the operation it installs.
     * That exclusivity is the point: answering 为民除害 during a fight leg, or a combat option during
     * the post-combat pass, would answer a dialog the flow is not in.</p>
     */
    private enum TiantingOptionSet {
        /** 为民除害 on 李靖's dialog. */
        ACCEPT(DialogOperation.TIANTING_ACCEPT_TASK),
        /** 取消任务 on 李靖's dialog, only after Cloud explicitly requests cancellation. */
        CANCEL(DialogOperation.TIANTING_CANCEL_TASK),
        /** 使用引妖香 when its own narrow phase explicitly asks for it. */
        YINYAO(DialogOperation.TIANTING_YINYAO),
        /** The four resident combat-entry options. */
        COMBAT(DialogOperation.TIANTING_COMBAT_OPTION),
        /**
         * 使用封妖符, offered only in the short window that follows 多谢.
         *
         * <p>Its own set rather than a fifth resident option: it is conditional, and having it live
         * during a normal leg would answer a dialog the flow is not in.</p>
         */
        FENGYAO(DialogOperation.TIANTING_FENGYAO),
        /** All ordinary non-post-combat options after a tracker click starts no movement. */
        RECOVERY(DialogOperation.TIANTING_RECOVERY_OPTION),
        /** Same recovery set after this accepted cycle already consumed 引妖香. */
        RECOVERY_NO_YINYAO(DialogOperation.TIANTING_RECOVERY_OPTION_NO_YINYAO),
        /** 鬼王在下愿为三; uses this mature shared-frame/fresh-frame click pipeline. */
        GHOST_KING_ACCEPT(DialogOperation.GHOST_KING_ACCEPT_TASK);

        private final DialogOperation operation;

        TiantingOptionSet(DialogOperation operation) {
            this.operation = operation;
        }

        /**
         * @param operations the live interest's operations.
         * @return the single set to match, or null when this interest arms none. An interest carrying
         *         more than one 天庭 operation is refused rather than silently resolved to whichever
         *         happens to be checked first.
         */
        static TiantingOptionSet from(List<DialogOperation> operations) {
            TiantingOptionSet found = null;
            for (TiantingOptionSet candidate : values()) {
                if (operations.contains(candidate.operation)) {
                    if (found != null) {
                        return null;
                    }
                    found = candidate;
                }
            }
            return found;
        }

        boolean supports(TaskType taskType) {
            return this == GHOST_KING_ACCEPT
                    ? taskType == TaskType.GHOST_KING
                    : taskType == TaskType.TIANTING;
        }

        Optional<TiantingDialogLocalMechanics.OptionHit> match(BufferedImage roi) {
            return switch (this) {
                case ACCEPT -> TiantingDialogLocalMechanics.matchAcceptOption(roi);
                case CANCEL -> TiantingDialogLocalMechanics.matchCancelOption(roi);
                case YINYAO -> TiantingDialogLocalMechanics.matchYinyaoOption(roi);
                case COMBAT -> TiantingDialogLocalMechanics.matchResidentOption(roi);
                case FENGYAO -> TiantingDialogLocalMechanics.matchFengyaoOption(roi);
                case RECOVERY -> TiantingDialogLocalMechanics.matchRecoveryOption(roi);
                case RECOVERY_NO_YINYAO ->
                        TiantingDialogLocalMechanics.matchRecoveryOptionWithoutYinyao(roi);
                case GHOST_KING_ACCEPT -> TiantingDialogLocalMechanics.matchGhostKingAcceptOption(roi);
            };
        }
    }

    private static String templateName(String templatePath) {
        return templatePath == null ? "-" : templatePath.substring(templatePath.lastIndexOf('/') + 1);
    }

    private void sampleXinshouFact(long observerSeq,
                                   ObservationFactType factType,
                                   String value,
                                   long observedAtMs,
                                   List<ObservationFact> facts) {
        String previousObserved = observedXinshouFacts.put(factType, value);
        if (!Objects.equals(previousObserved, value)) {
            markXinshouEffectiveProgress(observedAtMs);
        }
        XinshouFactVersion delivered = deliveredXinshouFacts.get(factType);
        if (delivered != null && value.equals(delivered.value())) {
            sampledXinshouFacts.remove(factType);
            return;
        }
        sampledXinshouFacts.put(factType, new XinshouFactVersion(value, observerSeq));
        facts.add(new ObservationFact(factType, value, observedAtMs));
    }

    /**
     * Starts one local Xinshou observation tick. The timeout grants only permission to resend the
     * current shared-frame evidence; it never grants permission to choose or execute an action.
     */
    private boolean beginXinshouObservationCycle(
            List<ObservationInterest> interests, long now) {
        boolean observesXinshouScene = interests.stream().anyMatch(interest ->
                XINSHOU_TRACKER_INTEREST.equals(interest.interestKey())
                        || XINSHOU_DIALOG_INTEREST.equals(interest.interestKey()));
        if (!observesXinshouScene) {
            return false;
        }
        if (lastXinshouEffectiveProgressAtMs <= 0L) {
            lastXinshouEffectiveProgressAtMs = now;
        }
        if (hasActivePathingIntent()) {
            return false;
        }
        long referenceAt = Math.max(
                lastXinshouEffectiveProgressAtMs,
                lastXinshouRefreshAcknowledgedAtMs);
        if (!xinshouRefreshPending
                && now - referenceAt >= XINSHOU_NO_PROGRESS_REFRESH_MS) {
            xinshouRefreshPending = true;
            xinshouRefreshObserverSeq = 0L;
        }
        return xinshouRefreshPending;
    }

    private void markXinshouEffectiveProgress(long observedAtMs) {
        lastXinshouEffectiveProgressAtMs =
                Math.max(lastXinshouEffectiveProgressAtMs, observedAtMs);
        /*
         * Once a scene reproof is armed, a different fact changing earlier in the same observation
         * cycle must not cancel it. Combat exit is the important case: ESC/skip commonly disappears
         * before the unchanged Tracker ROI is sampled. Only an ACK for the reproof ROI closes it.
         */
    }

    private static boolean isVersionedXinshouFact(ObservationFactType factType) {
        return factType == ObservationFactType.XINSHOU_ANCHOR
                || factType == ObservationFactType.XINSHOU_ESC_VISIBLE
                || factType == ObservationFactType.XINSHOU_SKIP_VISIBLE
                || factType == ObservationFactType.XINSHOU_ESC_BOT
                || factType == ObservationFactType.XINSHOU_ADOPTION
                || factType == ObservationFactType.XINSHOU_RECOVERY_STATUS
                || factType == ObservationFactType.XINSHOU_DIALOG_PRESENCE;
    }

    private static boolean isXinshouChangeInterest(String interestKey) {
        return XINSHOU_TRACKER_INTEREST.equals(interestKey)
                || XINSHOU_DIALOG_INTEREST.equals(interestKey);
    }

    /** Exact ARGB pixel hash; PNG encoder details cannot create false content changes. */
    private static String hashImageContent(BufferedImage image) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            updateDigestInt(digest, image.getWidth());
            updateDigestInt(digest, image.getHeight());
            int[] row = new int[image.getWidth()];
            for (int y = 0; y < image.getHeight(); y++) {
                image.getRGB(0, y, image.getWidth(), 1, row, 0, image.getWidth());
                for (int argb : row) {
                    updateDigestInt(digest, argb);
                }
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 is unavailable", impossible);
        }
    }

    private static void updateDigestInt(MessageDigest digest, int value) {
        digest.update((byte) (value >>> 24));
        digest.update((byte) (value >>> 16));
        digest.update((byte) (value >>> 8));
        digest.update((byte) value);
    }

    /**
     * Whether this duty's own sampling period has elapsed. This is a pure read: the caller decides
     * when the slot is actually consumed by calling {@link #markSampled(String, long)}.
     *
     * <p>Stamping here (the previous behaviour) charged a full period for a cycle that produced
     * nothing. When the shared cycle frame was unavailable — five windows contending for the input
     * worker starved the capture to ~6% success — every wasted attempt still burned its 2s dialog-ROI
     * slot, halving the retry opportunities and leaving Cloud with no frame at all for 30 seconds.</p>
     */
    private boolean isDue(String key, long periodMs, long now) {
        Long last = lastSampledAtMs.get(key);
        return last == null || now - last >= periodMs;
    }

    /** Consume this duty's sampling slot: the next {@link #isDue} is one full period from {@code now}. */
    private void markSampled(String key, long now) {
        lastSampledAtMs.put(key, now);
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private record XinshouRoiVersion(String contentHash, long observerSeq) {
    }

    private record XinshouFactVersion(String value, long observerSeq) {
    }

    private record CoordinateBracketSpan(int leftMinX, int leftMaxX, int rightMinX, int rightMaxX) {
    }

    private record CoordinateGlyphBox(int minX, int minY, int maxX, int maxY, int pixelCount) {
        private int width() {
            return maxX - minX + 1;
        }

        private int height() {
            return maxY - minY + 1;
        }
    }
}
