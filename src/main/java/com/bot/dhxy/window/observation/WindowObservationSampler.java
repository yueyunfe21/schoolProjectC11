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
import com.bot.dhxy.core.GameClientTracker;
import com.bot.dhxy.model.PlayerCharacter;
import com.bot.dhxy.core.ImageFinder;
import com.bot.dhxy.input.InputSequences;
import com.bot.dhxy.model.dialog.DialogOperation;
import com.bot.dhxy.model.job.XiuluoGreenChainSchedule;
import com.bot.dhxy.service.DialogService;
import com.bot.dhxy.task.model.TaskType;
import com.bot.dhxy.tools.CoordinateHelper;
import com.bot.dhxy.window.model.WindowDialogInterest;
import com.bot.dhxy.window.model.WindowNativeBinding;
import com.bot.dhxy.window.model.WindowPathingIntent;
import com.bot.dhxy.window.model.WindowPathingSnapshot;
import com.bot.dhxy.window.model.WindowPathingState;
import com.bot.dhxy.window.runtime.WindowRuntimeContext;
import com.bot.dhxy.window.runtime.WindowTaskContextHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Per-window local sampling for the observation runner. A geometry-bearing interest captures and uploads exactly
 * its small exact-HWND window-relative ROI for Cloud recognition. Two geometry-free duties are local mechanics:
 * the pre-battle timer edge and the baseline combat-template state machine. The latter owns the exact-run
 * {@code IN_COMBAT}/{@code COMBAT_EXITED} mechanical edges, including miss hysteresis and the local visible-mini-map
 * fail-closed gate; Cloud only updates the parked task from those edges. The sampler never interprets business
 * phases or publishes ready events. Its only input-producing
 * exception is the pre-existing, separately gated local-kanda atomic click path in
 * {@link #sampleXiuluoLocalKanda(long, List)}.
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
    static final String WUBEI_PREPARE_KEY = "wubei-enter-battle-local-prepare";
    static final long WUBEI_PREPARE_PERIOD_MS = 100L;
    static final String COORDINATE_STRIP_INTEREST = "coordinate-strip";
    static final String PRE_COMBAT_COORDINATE_STRIP_ROI = "pre-combat-coordinate-strip";
    private static final String RUNNER_SOURCE = "window-observation-runner";
    static final long LOCAL_PATHING_SAMPLE_PERIOD_MS = 300L;
    static final long LOCAL_PATHING_ARRIVAL_STATIONARY_MS = 600L;
    private static final long LOCAL_PATHING_COORDINATE_PROBE_MIN_INTERVAL_MS = 2_000L;
    private static final long LOCAL_PATHING_STOPPED_AWAY_MS = 2_200L;
    private static final double LOCAL_PATHING_DIFF_RATIO = 0.05D;
    private static final int PATHING_COORDINATE_STRIP_X = 46;
    private static final int PATHING_COORDINATE_STRIP_Y = 59;
    private static final int PATHING_COORDINATE_STRIP_WIDTH = 178;
    private static final int PATHING_COORDINATE_STRIP_HEIGHT = 35;
    /*
     * Movement detection compares ONLY the digits between the coordinate brackets, not the whole
     * strip. On the full 178x35 strip a final-glide step changes just one or two glyphs (~2% of
     * pixels), which slipped under the old 5% ratio: the sampler declared "stationary" while the
     * character was still sliding the last tiles into the arrival tolerance ring, so ARRIVED and
     * the CR273 terminal frame fired on a pre-settle screen and the Cloud clicked a shifted scene
     * (2026-07-27 16:34 hwnd-30F14C4). Inside this 45x12 digit box a single digit change is
     * ~7% of pixels and a one-tile step ~22%, above the unchanged 5% ratio, while a truly idle
     * window diffs at exactly 0%.
     * The OCR upload keeps the full strip (Cloud needs the map name); only the diff is narrowed.
     */
    private static final int PATHING_MOVEMENT_DIFF_X = 118;
    private static final int PATHING_MOVEMENT_DIFF_Y = 70;
    private static final int PATHING_MOVEMENT_DIFF_WIDTH = 45;
    private static final int PATHING_MOVEMENT_DIFF_HEIGHT = 12;

    private final WindowRuntimeContext context;
    private final WindowTaskContextHolder contextHolder;
    private final GameClientTracker tracker;
    private final CoordinateHelper coordinateHelper;
    private final DialogService dialogService;
    private final InputSequences inputSequences;
    private final boolean localKandaEnabled;
    private final LocalCombatSignalMechanics combatSignalMechanics;
    private boolean localCombatVisible;
    private LocalCombatFrameState lastLocalCombatFrameState;
    private long localCombatGeneration;
    private boolean localCombatEntryPublished;
    private com.bot.dhxy.window.model.WindowExpectedCombatEnterClaim boundExpectedCombatClaim;
    private final DeferredReturnHomeReplayCoordinator returnHomeReplayCoordinator;
    private volatile Consumer<ObservationKeyEvent> asyncEventPublisher;
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
    /** Previous exact-HWND coordinate strip for the current local pathing intent. Runner-thread confined. */
    private BufferedImage localPathingFrame;
    private String localPathingIntentId;
    private String terminalCoordinateAcknowledgedIntentId;
    private boolean localPathingCoordinatePending;
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
    private long localPathingMovementObservedAtMs;
    /** Monotonic stationary-evidence generation; advanced on intent/movement/verdict invalidation. */
    private long localPathingGeneration;
    private long nextTerminalFrameId = 1L;
    private TerminalCandidateFrame pendingTerminalFrame;
    private ObservationRoi terminalCoordinateRoi;
    private Long lastTerminalFrameId;
    private Long lastTerminalFrameGeneration;
    private String lastTerminalFrameIntentId;

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
        this.returnHomeReplayCoordinator = returnHomeReplayCoordinator;
        this.combatSignalMechanics.bindCycleFrameCropper(this::cropSharedCycleFrame);
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

    private void refreshSharedCycleFrame() {
        if (sharedCycleFrame != null) {
            sharedCycleFrame.flush();
            sharedCycleFrame = null;
            sharedCycleFrameRect = null;
        }
        int[] rect = coordinateHelper.getScaledRect(0, 0, 1024, 768);
        BufferedImage frame = tracker.captureToMemoryIfIdle(
                "observe:shared-cycle-frame", rect[0], rect[1], rect[2], rect[3]);
        if (frame != null) {
            sharedCycleFrame = frame;
            sharedCycleFrameRect = rect;
        }
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
        public SampleBatch(List<ObservationPathingFact> pathingFacts,
                           List<ObservationFact> facts,
                           List<ObservationKeyEvent> events,
                           List<ObservationRoi> rois) {
            this(pathingFacts, List.of(), List.of(), facts, events, rois, List.of());
        }

        public static SampleBatch empty() {
            return new SampleBatch(
                    List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of());
        }
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
        return collect(interests, 0L);
    }

    public SampleBatch collect(List<ObservationInterest> interests, long observerSeq) {
        List<ObservationInterest> safeInterests = interests == null ? List.of() : interests;
        return contextHolder.callWith(context, () -> collectBound(safeInterests, observerSeq));
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
                || snapshot.getState() == WindowPathingState.UNKNOWN);
    }

    private SampleBatch collectBound(List<ObservationInterest> interests, long observerSeq) {
        long now = System.currentTimeMillis();
        refreshSharedCycleFrame();
        List<ObservationFact> facts = new ArrayList<>();
        List<ObservationKeyEvent> events = new ArrayList<>();
        List<ObservationRoi> rois = new ArrayList<>();
        List<TerminalCandidateFrame> terminalFrames = new ArrayList<>(1);
        // Local kanda owns first refusal for this observation cycle. A successful click publishes its claim in
        // the dialog fact below and prevents a stale Cloud ROI from being captured in the same request.
        if (localKandaEnabled && isDue(KANDA_PROBE_KEY, KANDA_PROBE_PERIOD_MS, now)) {
            markSampled(KANDA_PROBE_KEY, now);
            try {
                sampleXiuluoLocalKanda(now, events, rois);
            } catch (RuntimeException kandaFailure) {
                log.debug("Local-kanda probe failed (no fact fabricated): windowId={} message={}",
                        context.getWindowId(), kandaFailure.getMessage());
            }
        }
        refreshLocalPathingTerminal(now);
        List<ObservationPathingFact> pathingFacts = sampleCurrentPathingFact();
        String pathingIntentId = pathingFacts.isEmpty() ? null : pathingFacts.getFirst().intentId();
        sampleTerminalCoordinateFrame(pathingFacts, rois, terminalFrames);
        List<ObservationDialogInterestFact> dialogInterests = sampleCurrentDialogInterestFact();
        List<ObservationPreparedDialogFact> preparedDialogs =
                sampleWubeiEnterBattlePrepared(dialogInterests, observerSeq, now);
        if (isDue(LocalCombatSignalMechanics.INTEREST_KEY, LocalCombatSignalMechanics.SAMPLE_PERIOD_MS, now)) {
            markSampled(LocalCombatSignalMechanics.INTEREST_KEY, now);
            try {
                LocalCombatSignalMechanics.Signal signal = combatSignalMechanics.sample();
                observeLocalCombatTransition(signal, now, observerSeq, events);
                if (interests.stream().anyMatch(i -> LocalCombatSignalMechanics.INTEREST_KEY.equals(i.interestKey()))) {
                    facts.add(new ObservationFact(ObservationFactType.COMBAT_SIGNAL, signal.wireValue(), now));
                }
            } catch (RuntimeException sampleFailure) {
                log.debug("Local combat mechanics failed (no fact fabricated): windowId={} message={}",
                        context.getWindowId(), sampleFailure.getMessage());
            }
        }
        for (ObservationInterest interest : interests) {
            if (!isDue(interest.interestKey(), interest.samplePeriodMs(), now)) {
                continue;
            }
            try {
                if (INTEREST_PREBATTLE_TIMER.equals(interest.interestKey())) {
                    samplePreBattleTimerEdge(now, events);
                    markSampled(interest.interestKey(), now);
                } else if (!LocalCombatSignalMechanics.INTEREST_KEY.equals(interest.interestKey())
                        && interest.hasRoi()) {
                    ObservationDialogInterestFact dialogInterest =
                            dialogInterests.isEmpty() ? null : dialogInterests.getFirst();
                    // A cycle whose shared frame was unavailable produced no ROI at all, so it must not
                    // consume this interest's slot: the next cycle retries immediately instead of going
                    // blind for another full period.
                    if (sampleRoi(interest, dialogInterest, pathingIntentId, rois)) {
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
     * Applies the 59b combat-state mechanics locally. Entry is the first visible template stage.
     * Once combat is visible, positive world and combat evidence form a three-state decision:
     * a visible mini-map anchor confirms exit, a visible combat template confirms combat, and
     * all remaining combinations are unknown and retain the current state. Only exact task-run
     * edges cross the wire.
     */
    void observeLocalCombatTransition(
            LocalCombatSignalMechanics.Signal signal,
            long now,
            long observerSeq,
            List<ObservationKeyEvent> events) {
        if (localCombatVisible) {
            LocalCombatSignalMechanics.Signal minimapSignal = combatSignalMechanics.sampleMinimap();
            LocalCombatFrameState frameState;
            if (minimapSignal.state() == LocalCombatSignalMechanics.State.VISIBLE) {
                frameState = LocalCombatFrameState.WORLD_CONFIRMED;
            } else if (signal != null && signal.state() == LocalCombatSignalMechanics.State.VISIBLE) {
                frameState = LocalCombatFrameState.COMBAT_CONFIRMED;
            } else {
                frameState = LocalCombatFrameState.UNKNOWN;
            }
            if (frameState != lastLocalCombatFrameState) {
                log.debug("Local combat evidence changed: windowId={} state={} combatSignal={} minimapSignal={}",
                        context.getWindowId(), frameState,
                        signal == null ? "null" : signal.wireValue(), minimapSignal.wireValue());
                lastLocalCombatFrameState = frameState;
            }
            if (frameState == LocalCombatFrameState.WORLD_CONFIRMED) {
                finishLocalCombat(now, events, "minimap-visible");
                return;
            }
        }
        if (signal == null || signal.state() == LocalCombatSignalMechanics.State.UNAVAILABLE) {
            return;
        }
        if (signal.state() == LocalCombatSignalMechanics.State.VISIBLE) {
            if (!localCombatVisible) {
                localCombatGeneration++;
                localCombatEntryPublished = false;
                context.updateLocalCombatGeneration(localCombatGeneration, true);
                boundExpectedCombatClaim =
                        context.bindExpectedCombatEnterClaim(taskRunId, localCombatGeneration);
            }
            localCombatVisible = true;
            lastLocalCombatFrameState = LocalCombatFrameState.COMBAT_CONFIRMED;
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
        localCombatVisible = false;
        lastLocalCombatFrameState = null;
        context.updateLocalCombatGeneration(localCombatGeneration, false);
        if (localCombatEntryPublished) {
            publishCombatEdge(
                    ObservationKeyEventType.COMBAT_EXITED, occurredAtMs, events, source, true);
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
        ObservationKeyEvent edge = new ObservationKeyEvent(
                (eventType == ObservationKeyEventType.IN_COMBAT ? "combat-enter:" : "combat-exit:")
                        + (expected ? claim.claimId() : "unexpected") + ":" + localCombatGeneration,
                eventType,
                occurredAtMs,
                null,
                expected ? claim.attemptId() : null,
                null,
                "window-observation-runner:" + source,
                expected ? "explicit-enter-claim=" + claim.source() : "unexpected-combat",
                expected ? claim.claimId() : null,
                localCombatGeneration,
                expected ? claim.taskCode() : null,
                expected ? claim.businessTaskRunId() : null);
        Consumer<ObservationKeyEvent> publisher = asyncEventPublisher;
        if (expected && terminal && returnHomeReplayCoordinator != null && publisher != null
                && context.hasArmedReturnHomeReplay(
                        claim.taskCode(), taskRunId, claim.businessTaskRunId())) {
            returnHomeReplayCoordinator.submitOnLocalExit(
                    context, claim.taskCode(), taskRunId, claim.businessTaskRunId(), edge, publisher);
        } else {
            events.add(edge);
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
        lastPathingFact = null;
        clearedPathingFactDelivered = false;
        lastDialogInterestFact = null;
        terminalCoordinateAcknowledgedIntentId = null;
        localCombatVisible = false;
        lastLocalCombatFrameState = null;
        localCombatGeneration = 0L;
        localCombatEntryPublished = false;
        boundExpectedCombatClaim = null;
        resetLocalPathingObservation();
        localPathingGeneration = 0L;
        nextTerminalFrameId = 1L;
        combatSignalMechanics.reset();
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
     * Restores the baseline local runner's stopped-pathing authority without moving map/OCR business
     * recognition back to the Client. The small coordinate strip is captured from the exact bound HWND;
     * pixel changes prove continued movement. CR142 permits a targeted coordinate intent to request
     * recognition after 600ms of stable pixels. The narrowed 45x12 digit ROI remains the cheap movement
     * trigger, but STOPPED_AWAY follows the baseline recognized-location contract: the first coordinate
     * establishes a baseline, changes remain ACTIVE, and only a later unchanged coordinate may satisfy
     * the separate 2.2-second boundary.
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
            advanceLocalPathingGeneration();
            localPathingLastSampleAtMs = observedAtMs;
            localPathingLastChangedAtMs = observedAtMs;
        }
        int[] rect = coordinateHelper.getScaledRect(
                PATHING_MOVEMENT_DIFF_X, PATHING_MOVEMENT_DIFF_Y,
                PATHING_MOVEMENT_DIFF_WIDTH, PATHING_MOVEMENT_DIFF_HEIGHT);
        BufferedImage current = cropSharedCycleFrame(rect);
        if (current == null) {
            return;
        }

        boolean changed = localPathingFrame != null
                && !ImageFinder.isMatch(localPathingFrame, current, LOCAL_PATHING_DIFF_RATIO);
        if (changed) {
            advanceLocalPathingGeneration();
            localPathingLastChangedAtMs = observedAtMs;
            localPathingMovementObservedAtMs = observedAtMs;
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
                        : changed ? "local runner movement observed" : "local runner pathing active")
                .locationChangedAtMs(localPathingLastChangedAtMs)
                .movementObservedAtMs(localPathingMovementObservedAtMs)
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
        localPathingMovementObservedAtMs = 0L;
        localPathingCoordinatePending = false;
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
    }

    /**
     * Captures one fresh exact-window frame for the current stationary generation and derives the
     * coordinate strip from those same in-memory pixels. The full frame keeps one frame id across
     * transport uncertainty and is removed only after a successful observation response.
     */
    private void sampleTerminalCoordinateFrame(List<ObservationPathingFact> pathingFacts,
                                               List<ObservationRoi> rois,
                                               List<TerminalCandidateFrame> terminalFrames) {
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

    private void captureTerminalFrameEvidence(ObservationPathingFact fact) {
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
     * the same fact for the CR142 arrival/stopped-away classifier.
     *
     * <p>The map/X/Y are logical game coordinates, not screen pixels. Updating the per-window
     * {@link PlayerCharacter} here keeps later Cloud task decisions from reusing a location that
     * predates a completed local pathing terminal. This update is data-only: it neither wakes a
     * task nor changes the terminal classifier's existing intent/generation guards.</p>
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
            boolean recognizedLocationChanged = hasRecognizedPathingLocationChanged(
                    result.mapName(), result.coordinateX(), result.coordinateY());
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

    /** Updates only this bound window's logical player location from a Cloud-recognized coordinate. */
    private void updateWindowPlayerLocation(ObservationAnalysisResult result, String source) {
        PlayerCharacter me = context.getGameState().getMe();
        me.setCurrentMapName(result.mapName());
        me.setX(result.coordinateX());
        me.setY(result.coordinateY());
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

    private boolean hasRecognizedPathingLocationChanged(String currentMapName, int currentX, int currentY) {
        return localPathingRecognizedMapName == null
                || localPathingRecognizedX == null
                || localPathingRecognizedY == null
                || !Objects.equals(localPathingRecognizedMapName, currentMapName)
                || localPathingRecognizedX != currentX
                || localPathingRecognizedY != currentY;
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
                        0L,
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
        long movementObservedAtMs = normalizeOptionalPathingTimestamp(
                snapshot.getMovementObservedAtMs(), pathingStartedAtMs, pathingUpdatedAtMs);
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
                movementObservedAtMs,
                snapshot.isDialogBlocking(),
                snapshot.isDialogBlocking() ? snapshot.getDialogBlockingReason() : null,
                dialogBlockingDetectedAtMs,
                snapshot.getState() == WindowPathingState.ARRIVED
                                && Objects.equals(intent.getIntentId(), lastTerminalFrameIntentId)
                        ? lastTerminalFrameId : null,
                snapshot.getState() == WindowPathingState.ARRIVED
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
     * the single input queue. Click success publishes {@code ENTER_BATTLE_CLICKED} and deliberately leaves the
     * interest, schedule and probe open — only real {@code IN_COMBAT} tears the attempt down.
     */
    private void sampleXiuluoLocalKanda(long now,
                                        List<ObservationKeyEvent> events,
                                        List<ObservationRoi> rois) {
        // TURN-40G review#3 P1: the interest+schedule pair is read as ONE consistent snapshot (same monitor as
        // the paired install/replace transition) — never a new interest with the previous attempt's schedule.
        WindowRuntimeContext.XiuluoKandaProbeView view = context.getXiuluoKandaProbeView();
        WindowDialogInterest interest = view.interest();
        if (interest == null
                || interest.getTaskType() != TaskType.XIULUO_V2
                || interest.getOperations() == null
                || !interest.getOperations().contains(DialogOperation.XIULUO_ENTER_BATTLE)
                || !interest.isLocalTemplateProbeOnly()
                || !interest.isProbeStartReached(now)) {
            return;
        }
        XiuluoGreenChainSchedule schedule = view.schedule();
        if (schedule == null) {
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
            return;
        }
        long epochBefore = context.getPlayerIdentityEpoch();
        if (dialogService.findXiuluoEnterBattleLocalTemplate(
                RUNNER_SOURCE, "probe:round-" + schedule.getRound()).isEmpty()) {
            return;
        }
        // Consume-time revalidation on a fresh frame plus binding/attempt/interest consistency re-checks.
        DialogService.LocalDialogTemplateMatch validated = dialogService.revalidateXiuluoEnterBattleLocalTemplate(
                RUNNER_SOURCE, "round-" + schedule.getRound()).orElse(null);
        if (validated == null) {
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
            return;
        }
        // The claim compares the live schedule's full identity to this exact captured schedule atomically under
        // the kanda monitor, so a stale/replaced schedule can never win the CAS.
        if (!context.tryClaimXiuluoEnterBattleClick(schedule, "local-kanda-hit")) {
            log.info("Local-kanda hit superseded (attempt already claimed or schedule replaced): windowId={} expected=[{}]",
                    context.getWindowId(), schedule.identityText());
            return;
        }
        capturePreCombatCoordinateFrame(rois);
        boolean clicked;
        try {
            clicked = inputSequences.moveAndClickLeft(
                    "xiuluo-40g:local-kanda:" + schedule.getRound(),
                    validated.absoluteX(), validated.absoluteY(), 80, 150);
        } catch (RuntimeException inputFailure) {
            clicked = false;
        }
        if (!clicked) {
            // An unexecuted click consumes nothing: release the claim so the open attempt keeps its fast path.
            // The release is fenced to this exact schedule's full identity, so a stale/replaced schedule can
            // never release (and thereby re-arm) a different schedule's claim.
            context.releaseXiuluoEnterBattleClick(schedule, "click-not-executed");
            return;
        }
        String claimId = UUID.randomUUID().toString();
        if (!context.registerExpectedCombatEnterClaim(new com.bot.dhxy.window.model.WindowExpectedCombatEnterClaim(
                claimId,
                taskRunId,
                schedule.getTaskRunId(),
                TaskType.XIULUO_V2.getCode(),
                schedule.getAttemptId(),
                context.getWindowId(),
                binding.getNativeHandle(),
                "local-template",
                null))) {
            context.releaseXiuluoEnterBattleClick(schedule, "expected-combat-claim-rejected");
            return;
        }
        events.add(new ObservationKeyEvent(
                "enter-battle-clicked-" + schedule.getAttemptId(),
                ObservationKeyEventType.ENTER_BATTLE_CLICKED,
                System.currentTimeMillis(),
                null,
                schedule.getAttemptId(),
                schedule.getRound(),
                "local-kanda",
                "clickX=" + validated.absoluteX() + "|clickY=" + validated.absoluteY()
                        + "|score=" + validated.score() + "|executed=true",
                claimId,
                null,
                TaskType.XIULUO_V2.getCode(),
                schedule.getTaskRunId()));
    }

    /**
     * Captures the exact small minimap-coordinate strip immediately before an already-confirmed
     * local enter-battle click. Encoding/upload happens with the normal observation batch after
     * the click, so coordinate recognition never blocks or reorders physical input.
     */
    private void capturePreCombatCoordinateFrame(List<ObservationRoi> rois) {
        int[] rect = coordinateHelper.getScaledRect(
                PATHING_COORDINATE_STRIP_X, PATHING_COORDINATE_STRIP_Y,
                PATHING_COORDINATE_STRIP_WIDTH, PATHING_COORDINATE_STRIP_HEIGHT);
        BufferedImage image = tracker.captureToMemory(
                "observe:pre-combat-coordinate", rect[0], rect[1], rect[2], rect[3]);
        if (image == null) {
            return;
        }
        try {
            ByteArrayOutputStream png = new ByteArrayOutputStream(4096);
            ImageIO.write(image, "png", png);
            rois.add(new ObservationRoi(
                    PRE_COMBAT_COORDINATE_STRIP_ROI,
                    PATHING_COORDINATE_STRIP_X,
                    PATHING_COORDINATE_STRIP_Y,
                    PATHING_COORDINATE_STRIP_WIDTH,
                    PATHING_COORDINATE_STRIP_HEIGHT,
                    png.toByteArray()));
        } catch (IOException encodeFailure) {
            log.debug("Pre-combat coordinate frame encode failed: windowId={}", context.getWindowId());
        } finally {
            image.flush();
        }
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
                              List<ObservationRoi> rois) {
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
                    dialogRoi ? dialogInterest.interestId() : null,
                    dialogRoi
                            ? dialogInterest.intentId()
                            : coordinateStrip ? pathingIntentId : null,
                    dialogRoi ? dialogInterest.attemptId() : null,
                    dialogRoi ? dialogInterest.round() : null));
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

    private enum LocalCombatFrameState {
        WORLD_CONFIRMED,
        COMBAT_CONFIRMED,
        UNKNOWN
    }
}
