package com.bot.dhxy.cloud.turn.local;

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
    private static final int WINDOW_WIDTH = 1024;
    private static final int WINDOW_HEIGHT = 768;
    private static final int CANDIDATE_LIMIT = 12;
    private static final long WAIT_SLEEP_MS = 500L;
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
    private final Map<ReplayPointKey, Point> verifiedReplayPoints = new ConcurrentHashMap<>();

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
        try {
            while (candidateMessageCount < CANDIDATE_LIMIT) {
                TaskCheckpoint.throwIfStopRequested(
                        taskContextHolder, "NPC arrival FIFO stopped before poll");
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
                if (localOutcome == NpcClickSmartQueueOutcome.VERIFIED) {
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
        if (outcome == NpcClickSmartQueueOutcome.VERIFIED) {
            rememberVerifiedPoint(arguments, spec, click);
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
        return queueOutcomeForVerification(dialogService.verifyNpcArrivalExpectedDialog(
                spec.expectedDialogTemplatePaths(),
                spec.expectedDialogRawTemplatePath(),
                spec.deferDialogVerificationToTask(),
                verificationSource));
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
                if (outcomeRef.get() == NpcClickSmartQueueOutcome.VERIFIED) {
                    rememberVerifiedPoint(arguments, spec, probeRel);
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
        Point retained = verifiedReplayPoints.get(ReplayPointKey.from(arguments, spec));
        if (!insideAllowedRegion(retained, spec)) {
            log.warn("NPC arrival retained-point replay rejected: windowId={} taskRunId={} intentId={} point={}",
                    spec.windowId(), spec.businessTaskRunId(), arguments.intentId(), retained);
            return false;
        }
        NpcClickSmartQueueOutcome outcome = executePointAndVerify(
                arguments,
                spec,
                binding,
                new Point(retained),
                "fifoRetainedPointReplay",
                "npc-click-smart-fifo:retained-point-replay");
        log.info("NPC arrival retained-point replay finished: windowId={} taskRunId={} intentId={} point={} outcome={}",
                spec.windowId(), spec.businessTaskRunId(), arguments.intentId(), retained, outcome);
        return outcome == NpcClickSmartQueueOutcome.VERIFIED;
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

    private static NpcClickSmartQueueOutcome queueOutcomeForVerification(
            DialogService.NpcClickVerification verification) {
        if (verification != null && verification.verified()) {
            return NpcClickSmartQueueOutcome.VERIFIED;
        }
        if (verification != null && verification.optionDialogVisible()) {
            return NpcClickSmartQueueOutcome.DIALOG_OPEN_UNVERIFIED;
        }
        return NpcClickSmartQueueOutcome.VERIFICATION_FAILED;
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
