package com.bot.dhxy.service;

import com.bot.dhxy.cloud.task.NpcClickSmartCloudDecision;
import com.bot.dhxy.cloud.task.NpcClickSmartCloudDecisionService;
import com.bot.dhxy.cloud.task.NpcClickSmartCloudRequest;
import com.bot.dhxy.cloud.task.NpcClickSmartCloudSession;
import com.bot.dhxy.cloud.task.NpcClickSmartDirectCombatAuthorization;
import com.bot.dhxy.cloud.task.NpcClickSmartQueueMessage;
import com.bot.dhxy.cloud.task.NpcClickSmartQueueOutcome;
import com.bot.dhxy.cloud.task.ImageProcessorService;
import com.bot.dhxy.core.ImageFinder;
import com.bot.dhxy.input.InputProvider;
import com.bot.dhxy.model.npc.NpcTargetEvidence;
import com.bot.dhxy.model.ocr.OcrWindowRegion;
import com.bot.dhxy.model.ocr.ResolvedNpcClickRegion;
import com.bot.dhxy.task.model.TaskType;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.Accessors;
import com.bot.dhxy.core.GameClientTracker;
import com.bot.dhxy.input.InputSequences;
import com.bot.dhxy.input.action.InputAction;
import com.bot.dhxy.input.action.InputActionScope;
import com.bot.dhxy.model.dialog.DialogResult;
import com.bot.dhxy.model.dialog.DialogResultStatus;
import com.bot.dhxy.model.dialog.DialogType;
import com.bot.dhxy.model.npc.DirectCombatClickResult;
import com.bot.dhxy.model.npc.NpcClickRequest;
import com.bot.dhxy.model.npc.NpcSmartClickOutcome;
import com.bot.dhxy.model.npc.NpcTooltipType;
import com.bot.dhxy.runner.stop.TaskCheckpoint;
import com.bot.dhxy.runner.stop.TaskSleep;
import com.bot.dhxy.runner.stop.TaskStopRequestedException;
import com.bot.dhxy.runner.context.TaskExecutionContext;
import com.bot.dhxy.runner.context.TaskExecutionContextHolder;
import com.bot.dhxy.service.dialog.DialogHandleRequest;
import com.bot.dhxy.tools.GameStateUtil;
import com.bot.dhxy.tools.ImagePreprocessor;
import com.bot.dhxy.window.model.WindowNativeBinding;
import com.bot.dhxy.window.model.WindowReadyEvent;
import com.bot.dhxy.window.model.WindowReadyEventType;
import com.bot.dhxy.window.runtime.WindowReadyEventBus;
import com.bot.dhxy.window.runtime.WindowRuntimeContext;
import com.bot.dhxy.window.runtime.WindowScopedTempPath;
import com.bot.dhxy.window.runtime.WindowTaskContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Clicks NPCs in the currently bound game window through one public request-based entry.
 *
 * <p>All real mouse/keyboard operations must be serialized through {@link InputSequences}. Coordinates
 * passed to public click methods are logical game map coordinates unless explicitly described as
 * screen-absolute or window-relative.</p>
 *
 * <p>The production smart-click entry is cloud-owned: local code captures the bound window, builds
 * the request payload, validates a returned window-relative action, submits physical input, and
 * verifies the result. It must not fall back to the older learned-memory, tooltip, yellow OCR,
 * formula, or Ctrl-menu strategy chain when cloud is inactive or returns no executable action.</p>
 *
 * <p>Task classes should build a {@link NpcClickRequest} and call {@link #clickNpcSmart(NpcClickRequest)}
 * instead of choosing local visual strategies.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NpcClickService {

    private final InputSequences inputSequences;
    private final InputProvider inputProvider;
    private final GameClientTracker tracker;
    private final GameStateUtil gameStateUtil;
    private final BattleRadarService battleRadarService;
    private final DialogService dialogService;
    private final WindowScopedTempPath windowScopedTempPath;
    private final WindowTaskContextHolder windowTaskContextHolder;
    private final TaskExecutionContextHolder taskExecutionContextHolder;
    private final ImageProcessorService imageProcessorService;
    private final NpcClickSmartCloudDecisionService npcClickSmartCloudDecisionService;
    private final WindowReadyEventBus windowReadyEventBus;

    private static final int WINDOW_WIDTH = 1024;
    private static final int WINDOW_HEIGHT = 768;
    private static final String NPC_TASK_TOOLTIP_TEMPLATE_PATH = "images/template/npc/npc_task_tooltip.png";
    private static final String NPC_CLICK_START = "NPC_CLICK_START";
    private static final String NPC_CLICK_POLL = "NPC_CLICK_POLL";
    private static final int NPC_LEFT_CLICK_HOLD_MS = 150;
    private static final int NPC_CLICK_SMART_QUEUE_CANDIDATE_LIMIT = 12;
    private static final long NPC_CLICK_SMART_QUEUE_WAIT_TIMEOUT_MS = 30_000L;
    private static final long NPC_CLICK_SMART_QUEUE_WAIT_SLEEP_MS = 100L;
    /*
     * CR255 fail-closed guard on the new mechanism only: a story blocker that survives this many
     * fast-click + restart cycles within ONE smart click invocation returns to the existing
     * failure/recovery chain instead of looping. The normal no-story path never reads this.
     */
    private static final int NPC_CLICK_SMART_STORY_BLOCKER_RESTART_LIMIT = 3;
    private static final int CTRL_MENU_SCAN_W = 150;
    private static final int CTRL_MENU_SCAN_H = 120;
    private static final String CTRL_MENU_NPC_TAG_TEMPLATE_PATH = "images/calibrate/npc_menu_clean_sample.png";
    private static final double CTRL_MENU_NPC_TAG_MATCH_THRESHOLD = 0.80;
    private static final int[][] CTRL_MENU_SMALL_RING_OFFSETS = {
            {0, 0}, {0, -18}, {18, 0}, {0, 18}, {-18, 0}
    };


    private NpcClickVerifier dialogClickVerifier(String expectedDialogTemplatePath) {
        return reason -> verifyExpectedDialogVisible(expectedDialogTemplatePath, reason);
    }

    private NpcClickVerifier dialogClickVerifier(NpcClickRequest request) {
        if (request != null && request.deferDialogVerificationToTask()) {
            return reason -> {
                log.info("NPC_CLICK_SMART defers post-click dialog verification to task phase: reason={} npcName={} task={}",
                        reason, request.npcName(), request.sourceTask());
                return NpcClickVerificationResult.deferredToTask();
            };
        }
        if (request != null && request.expectedDialogRawTemplatePath() != null
                && !request.expectedDialogRawTemplatePath().isBlank()) {
            return reason -> verifyExpectedRawDialogVisible(request.expectedDialogRawTemplatePath(), reason);
        }
        if (request == null || request.expectedDialogTemplatePaths() == null
                || request.expectedDialogTemplatePaths().isEmpty()) {
            return dialogClickVerifier(request == null ? null : request.expectedDialogTemplatePath());
        }
        return reason -> verifyExpectedDialogVisible(request.expectedDialogTemplatePaths(), reason);
    }

    private NpcClickVerifier combatClickVerifier() {
        return reason -> NpcClickVerificationResult.combat(isCombatVisibleAfterDirectClick(reason));
    }

    private boolean isCombatVisibleAfterDirectClick(String reason) {
        for (int i = 1; i <= 4; i++) {
            if (shouldStop()) {
                return false;
            }
            boolean inCombat = battleRadarService.checkAndSyncCombatState();
            log.info("NPC direct-combat verify: reason={} attempt={} inCombat={}", reason, i, inCombat);
            if (inCombat) {
                return true;
            }
            if (!TaskSleep.sleep(350)) {
                return false;
            }
        }
        return false;
    }

    /**
     * Verify that an NPC click opened the expected option dialog without consuming its option.
     *
     * @param expectedDialogTemplatePath expected green option template; nullable falls back to
     *                                   generic option-dialog visibility.
     * @param reason diagnostic source used in logs and temp screenshot names.
     * @return true only when the expected option dialog is visible.
     */
    private NpcClickVerificationResult verifyExpectedDialogVisible(String expectedDialogTemplatePath, String reason) {
        DialogResult result = dialogService.handleDialog(DialogHandleRequest.verifyExpectedOptionDialog(
                "npc-click:expected-dialog:" + reason, expectedDialogTemplatePath));
        return NpcClickVerificationResult.dialog(result);
    }

    private NpcClickVerificationResult verifyExpectedRawDialogVisible(String expectedDialogRawTemplatePath, String reason) {
        DialogResult result = dialogService.handleDialog(DialogHandleRequest.verifyExpectedRawOptionDialog(
                "npc-click:expected-dialog:" + reason, expectedDialogRawTemplatePath));
        return NpcClickVerificationResult.dialog(result);
    }

    /**
     * Verify that an NPC click opened one of the expected option dialogs without clicking it.
     *
     * @param expectedDialogTemplatePaths expected green option templates.
     * @param reason diagnostic source used in logs and temp screenshot names.
     * @return true only when one expected option dialog is visible.
     */
    private NpcClickVerificationResult verifyExpectedDialogVisible(List<String> expectedDialogTemplatePaths, String reason) {
        DialogResult result = dialogService.handleDialog(DialogHandleRequest.verifyExpectedOptionDialog(
                "npc-click:expected-dialog:" + reason, expectedDialogTemplatePaths));
        return NpcClickVerificationResult.dialog(result);
    }

    /**
     * Click an NPC or task target through the single public smart-click entry.
     *
     * <p>The caller supplies business facts only. Production authority is {@code NPC_CLICK_SMART};
     * inactive cloud, unavailable cloud, no-action, invalid response, and failed verification all
     * fail closed without entering the legacy local strategy pipeline.</p>
     *
     * @param request immutable click request. Coordinates are logical in-game map coordinates.
     *                Runtime production does not read local vision-memory ROI data; the client sends
     *                a safe full-window envelope and lets cloud canonical memory own narrowing.
     * @return true when any strategy opens and verifies the expected dialog.
     */
    public boolean clickNpcSmart(NpcClickRequest request) {
        return clickNpcSmartWithOutcome(request).isVerified();
    }

    /**
     * CR267: same smart-click entry as {@link #clickNpcSmart(NpcClickRequest)} but returns the
     * structured terminal, so direct-combat callers can pack an auditable
     * {@code directCombatNormalFifoUnverified} fact instead of assuming every {@code false} means
     * the FIFO was fully consumed.
     *
     * <p>{@code normalFifoConsumedUnverified} maps ONLY from the genuine queue {@code END}
     * terminal ({@code CLOUD_NO_ACTION}). Disabled cloud, session-start failure, invalid message,
     * stop/cancel, WAIT timeout, and candidate-budget exhaustion all map to false and therefore
     * can never authorize {@code ENTER_DIRECT_COMBAT}.</p>
     */
    public NpcSmartClickOutcome clickNpcSmartWithOutcome(NpcClickRequest request) {
        NpcClickVerifier verifier = dialogClickVerifier(request);
        NpcClickSmartExecutionResult cloudResult = tryClickNpcSmartViaCloud(request, verifier, "dialog");
        if (cloudResult.verifiedTargetAction()) {
            return NpcSmartClickOutcome.builder()
                    .verified(true)
                    .terminalStatus(cloudResult.status().name())
                    .build();
        }
        boolean fifoConsumedUnverified =
                cloudResult.status() == NpcClickSmartCloudDecision.Status.CLOUD_NO_ACTION;
        log.warn("NPC_CLICK_SMART did not produce a verified executable action; production smart click fails closed: "
                        + "npcName={} task={} status={} normalFifoConsumedUnverified={}",
                request == null ? null : request.npcName(),
                request == null ? null : request.sourceTask(),
                cloudResult.status(), fifoConsumedUnverified);
        return NpcSmartClickOutcome.builder()
                .verified(false)
                .normalFifoConsumedUnverified(fifoConsumedUnverified)
                .terminalStatus(cloudResult.status() == null ? null : cloudResult.status().name())
                .build();
    }

    /**
     * Execute the CR165 covered ordinary NPC dialog click through cloud-owned smart-click action.
     *
     * <p>Local code only captures raw pixels, sends target/template metadata, validates the returned
     * window-relative point against the request ROI, and submits one atomic move+click input
     * sequence. If cloud is unavailable, returns no executable action, or the click does not verify
     * the expected dialog, this method does not fall back to the old yellow/tooltip/formula/Ctrl
     * local pipeline.</p>
     */
    private NpcClickSmartExecutionResult tryClickNpcSmartViaCloud(
            NpcClickRequest request,
            NpcClickVerifier verifier,
            String verificationMode) {
        if (!npcClickSmartCloudDecisionService.isActive()) {
            log.warn("NPC_CLICK_SMART inactive; production smart click fails closed");
            return NpcClickSmartExecutionResult.terminal(NpcClickSmartCloudDecision.Status.DISABLED, null);
        }
        if (shouldStop()) {
            log.info("NPC_CLICK_SMART stopped before request: npcName={} task={} mode={}",
                    request == null ? null : request.npcName(),
                    request == null ? null : request.sourceTask(),
                    verificationMode);
            TaskCheckpoint.throwIfStopRequested(taskExecutionContextHolder,
                    "NPC_CLICK_SMART stopped before request");
        }
        /*
         * CR255: the anchor is captured ONCE per smart click, before the first session. Only
         * STORY_DIALOG_VISIBLE events published after it (this window, this task) are consumable,
         * and each event sequence is consumable at most once (lastConsumedStorySequence). Requests
         * without the opt-in never read the bus and never enter the restart loop more than once.
         */
        long storyEventAnchorSequence = request != null && request.consumeStoryDialogVisibleEvents()
                ? windowReadyEventBus.currentSequence()
                : Long.MAX_VALUE;
        long lastConsumedStorySequence = 0L;
        int storyBlockerRestarts = 0;
        while (true) {
            NpcClickSmartSessionResult sessionResult = consumeNpcClickSmartCloudSession(
                    request, verifier, verificationMode, storyEventAnchorSequence, lastConsumedStorySequence);
            if (sessionResult.storyEvent() == null) {
                return sessionResult.executionResult();
            }
            WindowReadyEvent storyEvent = sessionResult.storyEvent();
            if (storyBlockerRestarts >= NPC_CLICK_SMART_STORY_BLOCKER_RESTART_LIMIT) {
                log.warn("NPC_CLICK_SMART story blocker persisted beyond restart limit; fail closed to existing recovery: "
                                + "npcName={} task={} restarts={} eventSeq={}",
                        request == null ? null : request.npcName(),
                        request == null ? null : request.sourceTask(),
                        storyBlockerRestarts, storyEvent.getSequence());
                return NpcClickSmartExecutionResult.terminal(NpcClickSmartCloudDecision.Status.REQUIRED_FAILURE, null);
            }
            boolean storyClicked = dialogService.fastClickKnownSmallStoryDialog(
                    "npc-click-smart:story-blocker:" + (request == null ? null : request.sourceTask()));
            if (!storyClicked) {
                /*
                 * CR255 design #6: an unsubmitted fast click (pause/stop interruption) does NOT
                 * consume the event sequence and must not assume the story closed — return to the
                 * existing failure chain.
                 */
                log.warn("NPC_CLICK_SMART story fast-click not submitted; fail closed without consuming event: "
                                + "npcName={} task={} eventSeq={}",
                        request == null ? null : request.npcName(),
                        request == null ? null : request.sourceTask(),
                        storyEvent.getSequence());
                return NpcClickSmartExecutionResult.terminal(NpcClickSmartCloudDecision.Status.REQUIRED_FAILURE, null);
            }
            lastConsumedStorySequence = storyEvent.getSequence();
            storyBlockerRestarts++;
            log.info("NPC_CLICK_SMART story blocker fast-clicked; restarting smart session: npcName={} task={} "
                            + "consumedEventSeq={} eventSource={} restart={}/{}",
                    request == null ? null : request.npcName(),
                    request == null ? null : request.sourceTask(),
                    storyEvent.getSequence(), storyEvent.getSource(),
                    storyBlockerRestarts, NPC_CLICK_SMART_STORY_BLOCKER_RESTART_LIMIT);
        }
    }

    /**
     * Run one cloud smart-click session (request build, NPC_CLICK_START, FIFO consumption).
     *
     * <p>CR255: at the FIFO queue's natural boundary — where the loop is about to consume the next
     * queue message — one in-memory event-sequence read may detect a fresh
     * {@code STORY_DIALOG_VISIBLE} fact for this window/task. The session is then abandoned with a
     * CANCELLED terminal report and the caller performs exactly one fast story click before
     * restarting a new session. The read costs no screenshot, OCR, cloud request, sleep, or retry,
     * and never interrupts an in-flight input or HTTP call.</p>
     */
    private NpcClickSmartSessionResult consumeNpcClickSmartCloudSession(
            NpcClickRequest request,
            NpcClickVerifier verifier,
            String verificationMode,
            long storyEventAnchorSequence,
            long lastConsumedStorySequence) {
        NpcClickSmartCloudRequest cloudRequest = buildNpcClickSmartCloudRequest(
                request,
                verificationMode,
                UUID.randomUUID().toString());
        if (cloudRequest == null) {
            if (shouldStop()) {
                TaskCheckpoint.throwIfStopRequested(taskExecutionContextHolder,
                        "NPC_CLICK_SMART stopped while building cloud request");
            }
            log.warn("NPC_CLICK_SMART request build failed; covered path fails closed: npcName={} task={} map={} target=({}, {})",
                    request == null ? null : request.npcName(),
                    request == null ? null : request.sourceTask(),
                    request == null ? null : request.mapName(),
                    request == null ? null : request.mapX(),
                    request == null ? null : request.mapY());
            return NpcClickSmartSessionResult.completed(
                    NpcClickSmartExecutionResult.terminal(NpcClickSmartCloudDecision.Status.REQUIRED_FAILURE, null));
        }
        TaskCheckpoint.throwIfStopRequested(taskExecutionContextHolder,
                "NPC_CLICK_SMART stopped before NPC_CLICK_START");
        NpcClickSmartCloudSession session = npcClickSmartCloudDecisionService.startSession(cloudRequest);
        if (!session.accepted()) {
            log.warn("{} failed; production smart click fails closed: npcName={} task={} status={} reason={}",
                    NPC_CLICK_START,
                    request.npcName(), request.sourceTask(), session.getStatus(), session.getReason());
            reportQueueOutcomeAsync(cloudRequest, terminalMessage(cloudRequest, session, NpcClickSmartQueueMessage.Type.INVALID),
                    NpcClickSmartQueueOutcome.FINAL_FAILED, "NPC_CLICK_START failed: " + session.getReason());
            return NpcClickSmartSessionResult.completed(
                    NpcClickSmartExecutionResult.terminal(NpcClickSmartCloudDecision.Status.REQUIRED_FAILURE, null));
        }

        String terminalFailureReason = null;
        int candidateMessageCount = 0;
        long waitStartedAt = -1L;
        try {
            while (!shouldStop() && candidateMessageCount < NPC_CLICK_SMART_QUEUE_CANDIDATE_LIMIT) {
                TaskCheckpoint.throwIfStopRequested(taskExecutionContextHolder,
                        "NPC_CLICK_SMART stopped before NPC_CLICK_POLL");
                // CR255: one in-memory sequence read at the natural FIFO boundary — the same spot
                // that already runs shouldStop()/checkpoint before consuming the next message.
                WindowReadyEvent storyBlockerEvent = pollFreshStoryBlockerEvent(
                        request, storyEventAnchorSequence, lastConsumedStorySequence);
                if (storyBlockerEvent != null) {
                    reportQueueOutcomeAsync(cloudRequest,
                            terminalMessage(cloudRequest, session, NpcClickSmartQueueMessage.Type.INVALID),
                            NpcClickSmartQueueOutcome.CANCELLED,
                            "story dialog blocker observed; session abandoned for fast story click");
                    log.info("NPC_CLICK_SMART story blocker observed at FIFO boundary: sessionId={} npcName={} task={} eventSeq={} eventSource={} eventAgeMs={}",
                            session.getSessionId(),
                            request == null ? null : request.npcName(),
                            request == null ? null : request.sourceTask(),
                            storyBlockerEvent.getSequence(), storyBlockerEvent.getSource(),
                            Math.max(0L, System.currentTimeMillis() - storyBlockerEvent.getCreatedAtMs()));
                    return NpcClickSmartSessionResult.storyBlocked(storyBlockerEvent);
                }
                NpcClickSmartQueueMessage message = npcClickSmartCloudDecisionService.pollNext(cloudRequest, session);
                TaskCheckpoint.throwIfStopRequested(taskExecutionContextHolder,
                        "NPC_CLICK_SMART stopped after NPC_CLICK_POLL");
                log.info("{} result: sessionId={} candidateMessageCount={} type={} decisionId={}",
                        NPC_CLICK_POLL, session.getSessionId(), candidateMessageCount, message.getType(), message.getDecisionId());
                if (!isCurrentQueueMessage(cloudRequest, session, message)) {
                    reportQueueOutcomeAsync(cloudRequest, message, NpcClickSmartQueueOutcome.STALE_IGNORED,
                            "stale session/window/task mismatch ignored");
                    continue;
                }
                if (message.getType() == NpcClickSmartQueueMessage.Type.WAIT) {
                    if (waitStartedAt < 0L) {
                        waitStartedAt = System.currentTimeMillis();
                    }
                    long waitElapsedMs = System.currentTimeMillis() - waitStartedAt;
                    if (waitElapsedMs >= NPC_CLICK_SMART_QUEUE_WAIT_TIMEOUT_MS) {
                        terminalFailureReason = "NPC click queue WAIT timeout reached";
                        break;
                    }
                    log.debug("{} pending: sessionId={} waitElapsedMs={} reason={}",
                            NPC_CLICK_POLL, session.getSessionId(), waitElapsedMs, message.getReason());
                    if (!TaskSleep.sleep(NPC_CLICK_SMART_QUEUE_WAIT_SLEEP_MS)) {
                        break;
                    }
                    TaskCheckpoint.throwIfStopRequested(taskExecutionContextHolder,
                            "NPC_CLICK_SMART stopped after NPC_CLICK_POLL WAIT");
                    continue;
                }
                waitStartedAt = -1L;
                if (message.getType() == NpcClickSmartQueueMessage.Type.END) {
                    reportQueueOutcomeAsync(cloudRequest, message, NpcClickSmartQueueOutcome.FINAL_FAILED,
                            "cloud FIFO queue ended without verified NPC click");
                    return NpcClickSmartSessionResult.completed(
                            NpcClickSmartExecutionResult.terminal(NpcClickSmartCloudDecision.Status.CLOUD_NO_ACTION, null));
                }
                if (message.getType() == NpcClickSmartQueueMessage.Type.INVALID) {
                    reportQueueOutcomeAsync(cloudRequest, message, NpcClickSmartQueueOutcome.FINAL_FAILED,
                            "invalid cloud FIFO queue message: " + message.getReason());
                    return NpcClickSmartSessionResult.completed(
                            NpcClickSmartExecutionResult.terminal(NpcClickSmartCloudDecision.Status.REQUIRED_FAILURE, null));
                }

                candidateMessageCount++;
                NpcClickSmartQueueOutcome localOutcome;
                if (message.getType() == NpcClickSmartQueueMessage.Type.MEMORY) {
                    if (message.hasClickPoint()) {
                        log.info("NPC_CLICK_SMART migrated MEMORY candidate enters local safety shell: sessionId={} decisionId={} npcName={} clickRel={}",
                                message.getSessionId(), message.getDecisionId(), request.npcName(),
                                message.getWindowRelativeClickPoint());
                        localOutcome = executeNpcClickSmartQueueCandidate(request, cloudRequest, message, verifier);
                    } else {
                        log.info("NPC_CLICK_SMART disabled/no-click MEMORY skipped: sessionId={} decisionId={} npcName={} reason={}",
                                message.getSessionId(), message.getDecisionId(), request.npcName(), message.getReason());
                        localOutcome = NpcClickSmartQueueOutcome.SKIPPED;
                    }
                } else if (message.isOrdinaryClickCandidate()) {
                    if (!message.hasClickPoint()) {
                        log.info("NPC_CLICK_SMART ordinary no-click candidate skipped: sessionId={} type={} decisionId={} npcName={} reason={}",
                                message.getSessionId(), message.getType(), message.getDecisionId(),
                                request.npcName(), message.getReason());
                        localOutcome = NpcClickSmartQueueOutcome.SKIPPED;
                    } else {
                        localOutcome = executeNpcClickSmartQueueCandidate(request, cloudRequest, message, verifier);
                    }
                } else if (message.getType() == NpcClickSmartQueueMessage.Type.CTRL_CANDIDATES) {
                    localOutcome = executeNpcClickSmartCtrlCandidates(request, cloudRequest, message, verifier);
                } else {
                    localOutcome = NpcClickSmartQueueOutcome.FINAL_FAILED;
                }
                reportQueueOutcomeAsync(cloudRequest, message, localOutcome, "local verifier outcome after FIFO candidate");
                if (localOutcome == NpcClickSmartQueueOutcome.VERIFIED) {
                    return NpcClickSmartSessionResult.completed(
                            NpcClickSmartExecutionResult.verified(NpcClickSmartCloudDecision.Action.NO_ACTION));
                }
                if (localOutcome == NpcClickSmartQueueOutcome.SKIPPED
                        || localOutcome == NpcClickSmartQueueOutcome.VERIFICATION_FAILED) {
                    continue;
                }
                log.warn("NPC_CLICK_SMART FIFO terminal local outcome stops session consumption: "
                                + "sessionId={} type={} decisionId={} npcName={} outcome={}",
                        message.getSessionId(), message.getType(), message.getDecisionId(),
                        request.npcName(), localOutcome);
                return NpcClickSmartSessionResult.completed(
                        NpcClickSmartExecutionResult.terminal(NpcClickSmartCloudDecision.Status.REQUIRED_FAILURE, null));
            }
        } catch (TaskStopRequestedException e) {
            reportQueueOutcomeAsync(cloudRequest, terminalMessage(cloudRequest, session, NpcClickSmartQueueMessage.Type.INVALID),
                    NpcClickSmartQueueOutcome.CANCELLED, "stop requested while consuming NPC click queue");
            throw e;
        }
        String finalReason = shouldStop()
                ? "stop requested while consuming NPC click queue"
                : terminalFailureReason == null ? "NPC click queue candidate budget reached" : terminalFailureReason;
        reportQueueOutcomeAsync(cloudRequest, terminalMessage(cloudRequest, session, NpcClickSmartQueueMessage.Type.INVALID),
                shouldStop() ? NpcClickSmartQueueOutcome.CANCELLED : NpcClickSmartQueueOutcome.FINAL_FAILED,
                finalReason);
        return NpcClickSmartSessionResult.completed(
                NpcClickSmartExecutionResult.terminal(NpcClickSmartCloudDecision.Status.REQUIRED_FAILURE, null));
    }

    /**
     * CR255: non-blocking in-memory read of the newest {@code STORY_DIALOG_VISIBLE} fact for the
     * currently bound window. Consumable only when the request opted in, the event belongs to the
     * same task, and its sequence is newer than both the smart-click anchor and the last consumed
     * sequence (one consumption per sequence).
     */
    private WindowReadyEvent pollFreshStoryBlockerEvent(NpcClickRequest request,
                                                        long storyEventAnchorSequence,
                                                        long lastConsumedStorySequence) {
        if (request == null || !request.consumeStoryDialogVisibleEvents()) {
            return null;
        }
        WindowRuntimeContext runtime = windowTaskContextHolder.rawCurrent().orElse(null);
        if (runtime == null) {
            return null;
        }
        WindowReadyEvent event = windowReadyEventBus
                .latest(runtime.getWindowId(), WindowReadyEventType.STORY_DIALOG_VISIBLE)
                .orElse(null);
        if (event == null
                || event.getSequence() <= storyEventAnchorSequence
                || event.getSequence() <= lastConsumedStorySequence
                || event.getTaskType() != request.sourceTask()) {
            return null;
        }
        return event;
    }

    /**
     * CR255 wrapper for one cloud smart-click session run: either the session finished with a
     * normal execution result, or it was abandoned because a fresh story blocker must be
     * fast-clicked before a new session starts.
     */
    private record NpcClickSmartSessionResult(NpcClickSmartExecutionResult executionResult,
                                              WindowReadyEvent storyEvent) {
        private static NpcClickSmartSessionResult completed(NpcClickSmartExecutionResult result) {
            return new NpcClickSmartSessionResult(result, null);
        }

        private static NpcClickSmartSessionResult storyBlocked(WindowReadyEvent event) {
            return new NpcClickSmartSessionResult(null, event);
        }
    }

    private NpcClickSmartCloudRequest buildNpcClickSmartCloudRequest(
            NpcClickRequest request,
            String verificationMode,
            String sessionId) {
        if (request == null || request.npcName() == null || request.npcName().isBlank()) {
            return null;
        }
        if (!prepareNpcClickSmartCloudCaptureScene(request, verificationMode)) {
            if (shouldStop()) {
                TaskCheckpoint.throwIfStopRequested(taskExecutionContextHolder,
                        "NPC_CLICK_SMART stopped during capture preparation");
            }
            return null;
        }
        WindowBase windowBase = currentWindowBase("npc-click-smart-cloud");
        List<ResolvedNpcClickRegion> targetScanRegions = defaultNpcClickScanRegions(windowBase);
        BufferedImage image = tracker.captureToMemory(
                "npc-click-smart-cloud:" + request.npcName(),
                windowBase.x(),
                windowBase.y(),
                windowBase.x() + WINDOW_WIDTH,
                windowBase.y() + WINDOW_HEIGHT);
        if (image == null) {
            return null;
        }
        String rawPath = windowScopedTempPath.resolve("npc_click_smart_cloud_raw.png");
        try {
            byte[] pngBytes = pngBytes(image);
            ImagePreprocessor.saveImage(image, rawPath);
            WindowRuntimeContext runtime = windowTaskContextHolder.rawCurrent().orElse(null);
            List<String> templateSpecs = npcClickTemplateSpecs(request);
            return NpcClickSmartCloudRequest.builder()
                    .imagePayloadBase64(Base64.getEncoder().encodeToString(pngBytes))
                    .payloadMimeType("image/png")
                    .imageSha256(sha256Hex(pngBytes))
                    .rawImagePath(rawPath)
                    .sessionId(sessionId)
                    .debugImageId("npc-click-smart-" + UUID.randomUUID())
                    .roi(primaryScanRegionRoi(targetScanRegions))
                    .scanRegions(toCloudScanRegions(targetScanRegions))
                    .windowWidth(WINDOW_WIDTH)
                    .windowHeight(WINDOW_HEIGHT)
                    .npcRequest(request)
                    .taskCode(taskCode(request.sourceTask()))
                    .source("npc-click-smart")
                    .phase("npc-click-smart")
                    .verificationMode(verificationMode)
                    .attemptIndex(0)
                    .attemptToken("")
                    .lastOutcomeStatus("")
                    .lastOutcomeReason("")
                    .lastAction("")
                    .lastClick("")
                    .lastCandidateBox("")
                    .playerName(request.player() == null ? "" : request.player().getName())
                    .playerMapName(request.player() == null ? "" : request.player().getCurrentMapName())
                    .playerMapX(request.player() == null ? null : request.player().getX())
                    .playerMapY(request.player() == null ? null : request.player().getY())
                    .tuneX(request.tuneX())
                    .tuneY(request.tuneY())
                    .tooltipFirst(request.tooltipFirst())
                    .closeStoryBeforeDirectSceneClick(request.closeStoryBeforeDirectSceneClick())
                    .windowId(runtime == null ? null : runtime.getWindowId())
                    .taskRunId(currentTaskRunId(sessionId))
                    .hwnd(runtime == null || runtime.getNativeBinding() == null
                            ? null
                            : runtime.getNativeBinding().getNativeHandle())
                    .templateSpecs(templateSpecs)
                    .build();
        } catch (IOException | NoSuchAlgorithmException e) {
            log.warn("NPC_CLICK_SMART payload build failed: npcName={} task={} reason={}",
                    request.npcName(), request.sourceTask(), e.getMessage(), e);
            return null;
        } finally {
            image.flush();
        }
    }

    /**
     * Prepare the physical scene before the cloud-owned screenshot capture.
     *
     * @param request NPC click request; used only for diagnostics.
     * @param verificationMode cloud verification mode, for example {@code dialog} or {@code direct-combat}.
     * @return true when capture may proceed; false when serialized input preparation failed.
     */
    private boolean prepareNpcClickSmartCloudCaptureScene(
            NpcClickRequest request,
            String verificationMode) {
        if ("direct-combat".equals(verificationMode)) {
            log.info("NPC_CLICK_SMART skips Alt+4 clean-name capture prep in direct-combat mode: npcName={} task={}",
                    request.npcName(), request.sourceTask());
            return true;
        }
        boolean submitted = inputSequences.submitAndWait("npcClick:smartCloudCleanNameCapturePrep", List.of(
                InputAction.pressAlt4(),
                InputAction.sleep(180)
        ));
        if (!submitted) {
            log.warn("NPC_CLICK_SMART Alt+4 clean-name capture prep failed: npcName={} task={} mode={}",
                    request.npcName(), request.sourceTask(), verificationMode);
        }
        return submitted;
    }

    private NpcClickSmartQueueOutcome executeNpcClickSmartQueueCandidate(
            NpcClickRequest request,
            NpcClickSmartCloudRequest cloudRequest,
            NpcClickSmartQueueMessage message,
            NpcClickVerifier verifier) {
        Point clickRel = message.getWindowRelativeClickPoint();
        if (!isWindowRelativePointInsideAllowedRegion(clickRel, cloudRequest)) {
            log.warn("NPC_CLICK_SMART queue candidate rejected at local safety shell: sessionId={} type={} npcName={} clickRel=({}, {})",
                    message.getSessionId(), message.getType(), request.npcName(),
                    clickRel == null ? null : clickRel.x,
                    clickRel == null ? null : clickRel.y);
            return NpcClickSmartQueueOutcome.SAFETY_REJECTED;
        }
        WindowBase windowBase = currentWindowBase("npc-click-smart-queue-candidate");
        int clickX = windowBase.x() + clickRel.x;
        int clickY = windowBase.y() + clickRel.y;
        boolean submitted = inputSequences.submitAndWait(
                "npcClick:fifoCandidate:" + message.getType() + ":" + safeDebugName(request.npcName()),
                List.of(
                        InputAction.moveMouse(clickX, clickY),
                        InputAction.sleep(150),
                        InputAction.clickLeft(clickX, clickY, NPC_LEFT_CLICK_HOLD_MS),
                        InputAction.sleep(1500)
                ));
        log.info("NPC_CLICK_SMART FIFO candidate submitted: sessionId={} type={} decisionId={} npcName={} clickRel=({}, {}) clickAbs=({}, {}) submitted={}",
                message.getSessionId(), message.getType(), message.getDecisionId(), request.npcName(),
                clickRel.x, clickRel.y, clickX, clickY, submitted);
        if (!submitted) {
            if (shouldStop()) {
                return NpcClickSmartQueueOutcome.CANCELLED;
            }
            return NpcClickSmartQueueOutcome.INPUT_SUBMIT_FAILED;
        }
        if (shouldStop()) {
            return NpcClickSmartQueueOutcome.CANCELLED;
        }
        NpcClickVerificationResult verification = verifier.verify(
                "npc-click-smart-fifo:" + message.getType() + ":" + message.getDecisionId());
        return queueOutcomeForVerification(verification);
    }

    private NpcClickSmartQueueOutcome executeNpcClickSmartCtrlCandidates(
            NpcClickRequest request,
            NpcClickSmartCloudRequest cloudRequest,
            NpcClickSmartQueueMessage message,
            NpcClickVerifier verifier) {
        if (message.getCtrlProbePoints() == null || message.getCtrlProbePoints().isEmpty()) {
            return NpcClickSmartQueueOutcome.SAFETY_REJECTED;
        }
        WindowBase windowBase = currentWindowBase("npc-click-smart-ctrl-candidates");
        for (int index = 0; index < message.getCtrlProbePoints().size(); index++) {
            if (shouldStop()) {
                return NpcClickSmartQueueOutcome.CANCELLED;
            }
            Point probeRel = message.getCtrlProbePoints().get(index);
            if (!isWindowRelativePointInsideAllowedRegion(probeRel, cloudRequest)) {
                log.warn("NPC_CLICK_SMART Ctrl probe rejected at local safety shell: sessionId={} npcName={} index={} probeRel=({}, {})",
                        message.getSessionId(), request.npcName(), index,
                        probeRel == null ? null : probeRel.x,
                        probeRel == null ? null : probeRel.y);
                continue;
            }
            AtomicReference<NpcClickSmartQueueOutcome> outcomeRef =
                    new AtomicReference<>(NpcClickSmartQueueOutcome.VERIFICATION_FAILED);
            int candidateIndex = index;
            boolean submitted = inputSequences.submitExclusiveAndWait(
                    "npcClick:fifoCtrlMenuScan:" + candidateIndex + ":" + safeDebugName(request.npcName()),
                    () -> {
                        Point probeAbs = new Point(windowBase.x() + probeRel.x, windowBase.y() + probeRel.y);
                        NpcClickSmartQueueOutcome outcome = executeCtrlMenuProbeDirect(
                                request, cloudRequest, message, verifier, probeAbs, candidateIndex);
                        outcomeRef.set(outcome);
                        return outcome == NpcClickSmartQueueOutcome.VERIFIED;
                    });
            log.info("NPC_CLICK_SMART FIFO Ctrl menu probe finished: sessionId={} decisionId={} npcName={} index={} probeRel=({}, {}) submitted={} outcome={}",
                    message.getSessionId(), message.getDecisionId(), request.npcName(), index,
                    probeRel.x, probeRel.y, submitted, outcomeRef.get());
            if (!submitted && outcomeRef.get() == NpcClickSmartQueueOutcome.INPUT_SUBMIT_FAILED) {
                return NpcClickSmartQueueOutcome.INPUT_SUBMIT_FAILED;
            }
            if (outcomeRef.get() == NpcClickSmartQueueOutcome.VERIFIED
                    || outcomeRef.get() == NpcClickSmartQueueOutcome.DIALOG_OPEN_UNVERIFIED
                    || outcomeRef.get() == NpcClickSmartQueueOutcome.CANCELLED
                    || outcomeRef.get() == NpcClickSmartQueueOutcome.SAFETY_REJECTED) {
                return outcomeRef.get();
            }
        }
        return NpcClickSmartQueueOutcome.VERIFICATION_FAILED;
    }

    private NpcClickSmartQueueOutcome executeCtrlMenuProbeDirect(
            NpcClickRequest request,
            NpcClickSmartCloudRequest cloudRequest,
            NpcClickSmartQueueMessage message,
            NpcClickVerifier verifier,
            Point probeAbs,
            int probeIndex) {
        if (probeAbs == null || shouldStop()) {
            return shouldStop() ? NpcClickSmartQueueOutcome.CANCELLED : NpcClickSmartQueueOutcome.SAFETY_REJECTED;
        }
        WindowBase windowBase = currentWindowBase("npc-click-smart-ctrl-menu-direct");
        for (int offsetIndex = 0; offsetIndex < CTRL_MENU_SMALL_RING_OFFSETS.length; offsetIndex++) {
            int[] offset = CTRL_MENU_SMALL_RING_OFFSETS[offsetIndex];
            int testX = clamp(probeAbs.x + offset[0], windowBase.x(), windowBase.x() + WINDOW_WIDTH - 1);
            int testY = clamp(probeAbs.y + offset[1], windowBase.y(), windowBase.y() + WINDOW_HEIGHT - 1);
            int[] scanRect = buildCtrlMenuScanRect(testX, testY, windowBase);
            inputProvider.holdCtrl();
            try {
                if (!TaskSleep.sleep(80)) {
                    return NpcClickSmartQueueOutcome.CANCELLED;
                }
                inputProvider.moveMouse(testX, testY);
                if (!TaskSleep.sleep(280)) {
                    return NpcClickSmartQueueOutcome.CANCELLED;
                }
                NpcClickSmartQueueOutcome outcome = scanCtrlMenuAndVerifyKeywordDirect(
                        request, cloudRequest, message, verifier, scanRect, new Point(testX, testY),
                        probeIndex, offsetIndex);
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

    private int[] buildCtrlMenuScanRect(int testX, int testY, WindowBase windowBase) {
        int windowLeft = windowBase.x();
        int windowTop = windowBase.y();
        int windowRight = windowLeft + WINDOW_WIDTH;
        int windowBottom = windowTop + WINDOW_HEIGHT;
        int left = Math.max(windowLeft, testX - CTRL_MENU_SCAN_W);
        int top = Math.max(windowTop, testY - CTRL_MENU_SCAN_H);
        int right = Math.min(windowRight, testX + CTRL_MENU_SCAN_W);
        int bottom = Math.min(windowBottom, testY + CTRL_MENU_SCAN_H);
        if (right <= left) {
            right = Math.min(windowRight, left + CTRL_MENU_SCAN_W);
        }
        if (bottom <= top) {
            bottom = Math.min(windowBottom, top + CTRL_MENU_SCAN_H);
        }
        return new int[]{left, top, right, bottom};
    }

    private NpcClickSmartQueueOutcome scanCtrlMenuAndVerifyKeywordDirect(
            NpcClickRequest request,
            NpcClickSmartCloudRequest cloudRequest,
            NpcClickSmartQueueMessage message,
            NpcClickVerifier verifier,
            int[] scanRect,
            Point ctrlHoverPointAbs,
            int probeIndex,
            int offsetIndex) {
        if (scanRect == null || scanRect.length < 4 || request == null || request.npcName() == null) {
            return NpcClickSmartQueueOutcome.SAFETY_REJECTED;
        }
        String rawPath = windowScopedTempPath.resolve("npc_menu_scan.png");
        BufferedImage raw = tracker.captureToMemory(
                "npc-click-smart-ctrl-menu-scan",
                scanRect[0], scanRect[1], scanRect[2], scanRect[3]);
        if (raw == null) {
            return NpcClickSmartQueueOutcome.VERIFICATION_FAILED;
        }
        try {
            if (!ImagePreprocessor.saveImage(raw, rawPath)) {
                return NpcClickSmartQueueOutcome.VERIFICATION_FAILED;
            }
        } finally {
            raw.flush();
        }
        if (!Files.exists(Path.of(CTRL_MENU_NPC_TAG_TEMPLATE_PATH))) {
            log.warn("NPC_CLICK_SMART Ctrl menu template unavailable: sessionId={} decisionId={} template={}",
                    message.getSessionId(), message.getDecisionId(), CTRL_MENU_NPC_TAG_TEMPLATE_PATH);
            return NpcClickSmartQueueOutcome.VERIFICATION_FAILED;
        }
        double[] match = ImageFinder.find(rawPath, CTRL_MENU_NPC_TAG_TEMPLATE_PATH, CTRL_MENU_NPC_TAG_MATCH_THRESHOLD);
        if (match == null || match.length < 3) {
            log.info("NPC_CLICK_SMART Ctrl menu template not matched: sessionId={} decisionId={} npcName={} hover=({}, {}) template={} threshold={} scanRect=({}, {})-({}, {})",
                    message.getSessionId(), message.getDecisionId(), request.npcName(),
                    ctrlHoverPointAbs == null ? null : ctrlHoverPointAbs.x,
                    ctrlHoverPointAbs == null ? null : ctrlHoverPointAbs.y,
                    CTRL_MENU_NPC_TAG_TEMPLATE_PATH, CTRL_MENU_NPC_TAG_MATCH_THRESHOLD,
                    scanRect[0], scanRect[1], scanRect[2], scanRect[3]);
            return NpcClickSmartQueueOutcome.VERIFICATION_FAILED;
        }
        int clickX = scanRect[0] + (int) Math.round(match[0]);
        int clickY = scanRect[1] + (int) Math.round(match[1]);
        inputProvider.moveMouse(clickX, clickY);
        if (!TaskSleep.sleep(100)) {
            return NpcClickSmartQueueOutcome.CANCELLED;
        }
        inputProvider.clickLeft(clickX, clickY, NPC_LEFT_CLICK_HOLD_MS);
        if (!TaskSleep.sleep(900)) {
            return NpcClickSmartQueueOutcome.CANCELLED;
        }
        NpcClickVerificationResult verification = verifier.verify(
                "npc-click-smart-fifo:CTRL_CANDIDATES:" + message.getDecisionId());
        log.info("NPC_CLICK_SMART Ctrl menu template clicked: sessionId={} decisionId={} npcName={} score={} hover=({}, {}) click=({}, {}) template={} verified={}",
                message.getSessionId(), message.getDecisionId(), request.npcName(), match[2],
                ctrlHoverPointAbs == null ? null : ctrlHoverPointAbs.x,
                ctrlHoverPointAbs == null ? null : ctrlHoverPointAbs.y,
                clickX, clickY, CTRL_MENU_NPC_TAG_TEMPLATE_PATH, verification.verified());
        return queueOutcomeForVerification(verification);
    }

    private NpcClickSmartQueueOutcome queueOutcomeForVerification(NpcClickVerificationResult verification) {
        if (verification != null && verification.verified()) {
            return NpcClickSmartQueueOutcome.VERIFIED;
        }
        if (verification != null && verification.optionDialogVisible()) {
            log.warn("NPC_CLICK_SMART verifier saw option dialog but expected target was not confirmed; "
                            + "stop consuming NPC click candidates and let cloud/dialog recovery handle it: status={} type={}",
                    verification.status(), verification.dialogType());
            return NpcClickSmartQueueOutcome.DIALOG_OPEN_UNVERIFIED;
        }
        return NpcClickSmartQueueOutcome.VERIFICATION_FAILED;
    }

    private ImageProcessorService.RequestMetadata ctrlMenuImageProcessorMetadata(
            NpcClickSmartCloudRequest cloudRequest,
            NpcClickSmartQueueMessage message,
            int probeIndex,
            int offsetIndex) {
        return ImageProcessorService.RequestMetadata.builder()
                .rawImagePath(windowScopedTempPath.resolve("npc_menu_scan.png"))
                .debugImageId("npc-click-smart-ctrl-menu-" + probeIndex + "-" + offsetIndex)
                .source("npc-click-smart-fifo-ctrl-menu")
                .taskCode(cloudRequest == null ? "" : cloudRequest.getTaskCode())
                .phase("npc-click-smart-ctrl-menu")
                .windowId(cloudRequest == null ? "" : cloudRequest.getWindowId())
                .taskRunId(cloudRequest == null ? "" : cloudRequest.getTaskRunId())
                .hwnd(cloudRequest == null ? "" : cloudRequest.getHwnd())
                .parameters(Map.of(
                        "sessionId", message == null ? "" : safeValue(message.getSessionId()),
                        "decisionId", message == null ? "" : safeValue(message.getDecisionId())))
                .build();
    }

    private boolean isCurrentQueueMessage(
            NpcClickSmartCloudRequest request,
            NpcClickSmartCloudSession session,
            NpcClickSmartQueueMessage message) {
        if (request == null || session == null || message == null) {
            return false;
        }
        return equalsText(session.getSessionId(), message.getSessionId())
                && equalsText(request.getWindowId(), message.getWindowId())
                && equalsText(request.getTaskRunId(), message.getTaskRunId());
    }

    private void reportQueueOutcomeAsync(
            NpcClickSmartCloudRequest request,
            NpcClickSmartQueueMessage message,
            NpcClickSmartQueueOutcome outcome,
            String reason) {
        CompletableFuture.runAsync(() -> npcClickSmartCloudDecisionService.reportOutcome(
                request,
                message,
                outcome,
                verificationStrengthForOutcome(request),
                "expectedDialog",
                verificationMatchedTextForOutcome(request == null ? null : request.getNpcRequest()),
                reason));
    }

    private static NpcClickSmartQueueMessage terminalMessage(
            NpcClickSmartCloudRequest request,
            NpcClickSmartCloudSession session,
            NpcClickSmartQueueMessage.Type type) {
        return NpcClickSmartQueueMessage.builder()
                .type(type)
                .sessionId(session == null ? request == null ? "" : request.getSessionId() : session.getSessionId())
                .windowId(request == null ? "" : request.getWindowId())
                .taskRunId(request == null ? "" : request.getTaskRunId())
                .decisionId("local-terminal")
                .strategy(type.name())
                .build();
    }

    private static String verificationStrengthForOutcome(NpcClickSmartCloudRequest cloudRequest) {
        if (cloudRequest != null && "direct-combat".equalsIgnoreCase(cloudRequest.getVerificationMode())) {
            return "COMBAT_STATE";
        }
        NpcClickRequest request = cloudRequest == null ? null : cloudRequest.getNpcRequest();
        if (request != null && request.deferDialogVerificationToTask()) {
            return "TASK_PHASE_DEFERRED";
        }
        if (request != null && request.expectedDialogRawTemplatePath() != null
                && !request.expectedDialogRawTemplatePath().isBlank()) {
            return "DIALOG_RAW_TEMPLATE";
        }
        return "DIALOG_TEMPLATE";
    }

    private static String verificationMatchedTextForOutcome(NpcClickRequest request) {
        if (request == null) {
            return "";
        }
        if (request.deferDialogVerificationToTask()) {
            return "defer-dialog-verification-to-task";
        }
        if (request.expectedDialogRawTemplatePath() != null && !request.expectedDialogRawTemplatePath().isBlank()) {
            return request.expectedDialogRawTemplatePath();
        }
        if (request.expectedDialogTemplatePaths() != null && !request.expectedDialogTemplatePaths().isEmpty()) {
            return String.join("|", request.expectedDialogTemplatePaths());
        }
        return request.expectedDialogTemplatePath() == null ? "" : request.expectedDialogTemplatePath();
    }

    private String currentTaskRunId(String sessionId) {
        TaskExecutionContext context = taskExecutionContextHolder.current().orElse(null);
        if (context != null && context.getTaskRunId() > 0L) {
            return Long.toString(context.getTaskRunId());
        }
        return "npc-click-session-" + sessionId;
    }

    private static boolean isWindowRelativePointInside(Point point, NpcClickSmartCloudRequest.Roi roi) {
        return point != null
                && point.x >= 0
                && point.y >= 0
                && point.x < WINDOW_WIDTH
                && point.y < WINDOW_HEIGHT
                && roi != null
                && point.x >= roi.getX()
                && point.x < roi.getX() + roi.getWidth()
                && point.y >= roi.getY()
                && point.y < roi.getY() + roi.getHeight();
    }

    private static boolean isWindowRelativePointInsideAllowedRegion(
            Point point,
            NpcClickSmartCloudRequest request) {
        if (point == null || request == null) {
            return false;
        }
        if (request.getScanRegions() != null && !request.getScanRegions().isEmpty()) {
            return request.getScanRegions().stream().anyMatch(region -> insideScanRegion(point, region));
        }
        return isWindowRelativePointInside(point, request.getRoi());
    }

    private static boolean insideScanRegion(Point point, NpcClickSmartCloudRequest.ScanRegion region) {
        return point != null
                && region != null
                && point.x >= region.getWindowX()
                && point.x < region.getWindowX() + region.getWidth()
                && point.y >= region.getWindowY()
                && point.y < region.getWindowY() + region.getHeight();
    }

    private static List<String> npcClickTemplateSpecs(NpcClickRequest request) {
        List<String> specs = new ArrayList<>();
        if (hasText(request.expectedDialogTemplatePath()) && Files.exists(Path.of(request.expectedDialogTemplatePath()))) {
            specs.add("expectedDialogTemplatePath=" + request.expectedDialogTemplatePath());
        }
        if (hasText(request.expectedDialogRawTemplatePath()) && Files.exists(Path.of(request.expectedDialogRawTemplatePath()))) {
            specs.add("expectedDialogRawTemplatePath=" + request.expectedDialogRawTemplatePath());
        }
        if (request.expectedDialogTemplatePaths() != null) {
            for (String templatePath : request.expectedDialogTemplatePaths()) {
                if (hasText(templatePath) && Files.exists(Path.of(templatePath))) {
                    specs.add("expectedDialogTemplatePath=" + templatePath);
                }
            }
        }
        String tooltipTemplatePath = npcClickTooltipTemplatePath(request);
        if (hasText(tooltipTemplatePath) && Files.exists(Path.of(tooltipTemplatePath))) {
            specs.add("tooltipTemplatePath=" + tooltipTemplatePath);
        }
        return List.copyOf(specs);
    }

    private static String npcClickTooltipTemplatePath(NpcClickRequest request) {
        if (hasText(request.tooltipTemplatePath())) {
            return request.tooltipTemplatePath();
        }
        return request.tooltipType() == NpcTooltipType.NONE ? "" : NPC_TASK_TOOLTIP_TEMPLATE_PATH;
    }


    private static String taskCode(TaskType taskType) {
        return taskType == null ? TaskType.UNKNOWN.getCode() : taskType.getCode();
    }

    private static byte[] pngBytes(BufferedImage image) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, "png", output);
        return output.toByteArray();
    }

    private static String sha256Hex(byte[] bytes) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hashed = digest.digest(bytes);
        StringBuilder result = new StringBuilder(hashed.length * 2);
        for (byte value : hashed) {
            result.append(String.format("%02x", value));
        }
        return result.toString();
    }

    private static String safeDebugName(String value) {
        return hasText(value) ? value.trim().replaceAll("[^A-Za-z0-9_.\\-\\u4e00-\\u9fa5]", "_") : "unknown";
    }

    private static String safeValue(String value) {
        return value == null ? "" : value;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static boolean equalsText(String expected, String actual) {
        return safeValue(expected).equals(safeValue(actual));
    }

    /**
     * CR267: try to enter combat through the two-step direct combat-click contract.
     *
     * <p>Step 1 asks cloud for an independent {@code ENTER_DIRECT_COMBAT} authorization from
     * structured task facts only (no screenshot). Step 2, only after an explicit authorization,
     * presses {@code Alt+A} once through the input queue with the baseline short wait. Step 3
     * captures a fresh raw screenshot and starts a new {@code NPC_CLICK_SMART} session with
     * {@code directCombatMode=true}; cloud answers with ordinary window-relative target clicks and
     * never bundles {@code Alt+A} into a click candidate. Only a real click verified as
     * {@code IN_COMBAT} counts as success. If the task is stopped/interrupted, the method
     * intentionally does not right-click out of the mode, so the user's stop/pause command remains
     * the owner of recovery.</p>
     *
     * @param request immutable NPC/monster target request carrying the CR267 direct-combat facts
     *                ({@code directCombatProbeTargetReady}, {@code directCombatNormalFifoUnverified},
     *                {@code directCombatArrivalTolerance}). Coordinates are logical map coordinates;
     *                generated click points are screen-absolute through the existing smart-click
     *                conversion path.
     * @return structured result. Refusals before Alt+A are plain skips; failed attempts after Alt+A
     *         was entered are marked position-refresh-required because canceling direct-combat mode
     *         can move the character.
     */
    public DirectCombatClickResult tryDirectCombatTargetClick(NpcClickRequest request) {
        if (request == null) {
            log.warn("NPC direct-combat click requested with null request");
            return DirectCombatClickResult.skipped("null-request");
        }
        if (shouldStop()) {
            return DirectCombatClickResult.skipped("stop-requested-before-direct-combat");
        }

        /*
         * CR267 reviewer P1 #3: the ordinary-FIFO gate fact is a hard local precondition. When the
         * ordinary NPC_CLICK_SMART queue did not genuinely END unverified (disabled cloud, start/
         * protocol failure, cancel, budget), no DIRECT_COMBAT_AUTHORIZE request is sent at all —
         * those terminals keep the caller's existing failure path.
         */
        if (!request.directCombatNormalFifoUnverified()) {
            log.warn("NPC direct-combat skipped; ordinary smart FIFO did not genuinely END unverified, no authorize request is sent: "
                            + "npcName={} task={} map={} target=({}, {}) scenario={}",
                    request.npcName(), request.sourceTask(), request.mapName(), request.mapX(), request.mapY(),
                    request.directCombatScenario());
            return DirectCombatClickResult.skipped("direct-combat-normal-fifo-terminal-not-end");
        }

        // CR267 step 1: cloud owns the ENTER_DIRECT_COMBAT decision from structured task facts.
        // A refusal keeps the caller's existing failure path without any scene switch.
        NpcClickSmartDirectCombatAuthorization authorization = npcClickSmartCloudDecisionService
                .authorizeDirectCombat(directCombatAuthorizeCloudRequest(request));
        if (!authorization.isAuthorized()) {
            log.warn("NPC direct-combat not authorized by cloud; no Alt+A is pressed: npcName={} task={} map={} target=({}, {}) status={} reason={}",
                    request.npcName(), request.sourceTask(), request.mapName(), request.mapX(), request.mapY(),
                    authorization.getStatus(), authorization.getReason());
            return DirectCombatClickResult.skipped("direct-combat-cloud-refused");
        }
        if (shouldStop()) {
            return DirectCombatClickResult.skipped("stop-requested-before-direct-combat");
        }

        // CR267 step 2: authorized scene transition. Alt+A goes through the input queue with the
        // baseline 3f0a2e7 short wait; nothing else is pressed here.
        boolean enteredMode = inputSequences.submitAndWait("npcClick:directCombat:enterAltA", List.of(
                InputAction.pressAltA(),
                InputAction.sleep(350)
        ));
        if (!enteredMode || shouldStop()) {
            log.warn("NPC direct-combat click could not enter Alt+A mode: npcName={} enteredMode={}",
                    request.npcName(), enteredMode);
            return DirectCombatClickResult.skipped("direct-combat-alt-a-not-entered");
        }
        log.info("NPC direct-combat click mode entered: npcName={} map={} target=({}, {}) authorizeDecisionId={} modeLikely={}",
                request.npcName(), request.mapName(), request.mapX(), request.mapY(),
                authorization.getDecisionId(),
                gameStateUtil.isDirectCombatClickModeLikely("npc-direct-combat-entered"));

        // CR267 steps 3-5: fresh raw screenshot + new directCombatMode=true session; cloud returns
        // ordinary clicks and the existing combat verifier proves IN_COMBAT.
        NpcClickSmartExecutionResult result = tryClickNpcSmartViaCloud(
                request,
                combatClickVerifier(),
                "direct-combat");
        if (result.verifiedTargetAction() || shouldStop()) {
            return result.verifiedTargetAction()
                    ? DirectCombatClickResult.combatEntered("direct-combat-click-confirmed")
                    : DirectCombatClickResult.skipped("stop-requested-after-direct-combat");
        }
        if (result.verifiedRecoveryAction()) {
            log.warn("NPC direct-combat cloud recovery action verified; caller must refresh target position before retry: "
                            + "npcName={} task={} map={} target=({}, {})",
                    request.npcName(), request.sourceTask(), request.mapName(), request.mapX(), request.mapY());
            return DirectCombatClickResult.positionRefreshRequired("direct-combat-cloud-recovery-confirmed");
        }
        log.warn("NPC direct-combat cloud flow ended without combat or recovery; no local recovery action is selected: "
                        + "npcName={} task={} map={} target=({}, {}) status={} action={}",
                request.npcName(), request.sourceTask(), request.mapName(), request.mapX(), request.mapY(),
                result.status(), result.finalAction());
        return DirectCombatClickResult.positionRefreshRequired("direct-combat-cloud-failed-no-local-recovery");
    }

    /**
     * CR267: build the screenshot-free cloud request that carries only identity and structured
     * task facts for the {@code DIRECT_COMBAT_AUTHORIZE} decision. The main click chain still
     * captures exactly one base screenshot per smart session; this request must never trigger a
     * capture or input preparation.
     */
    private NpcClickSmartCloudRequest directCombatAuthorizeCloudRequest(NpcClickRequest request) {
        String sessionId = UUID.randomUUID().toString();
        WindowRuntimeContext runtime = windowTaskContextHolder.rawCurrent().orElse(null);
        return NpcClickSmartCloudRequest.builder()
                .sessionId(sessionId)
                .npcRequest(request)
                .taskCode(taskCode(request.sourceTask()))
                .source("npc-click-direct-combat")
                .phase("npc-click-direct-combat-authorize")
                .verificationMode("direct-combat-authorize")
                .playerName(request.player() == null ? "" : request.player().getName())
                .playerMapName(request.player() == null ? "" : request.player().getCurrentMapName())
                .playerMapX(request.player() == null ? null : request.player().getX())
                .playerMapY(request.player() == null ? null : request.player().getY())
                .windowWidth(WINDOW_WIDTH)
                .windowHeight(WINDOW_HEIGHT)
                .windowId(runtime == null ? null : runtime.getWindowId())
                .taskRunId(currentTaskRunId(sessionId))
                .hwnd(runtime == null || runtime.getNativeBinding() == null
                        ? null
                        : runtime.getNativeBinding().getNativeHandle())
                .build();
    }

    private record NpcClickSmartExecutionResult(
            NpcClickSmartCloudDecision.Status status,
            NpcClickSmartCloudDecision.Action finalAction) {

        static NpcClickSmartExecutionResult verified(NpcClickSmartCloudDecision.Action finalAction) {
            return new NpcClickSmartExecutionResult(
                    NpcClickSmartCloudDecision.Status.CLOUD_EXECUTED,
                    finalAction);
        }

        static NpcClickSmartExecutionResult terminal(
                NpcClickSmartCloudDecision.Status status,
                NpcClickSmartCloudDecision.Action finalAction) {
            return new NpcClickSmartExecutionResult(status, finalAction);
        }

        boolean verifiedTargetAction() {
            return status == NpcClickSmartCloudDecision.Status.CLOUD_EXECUTED;
        }

        boolean verifiedRecoveryAction() {
            return false;
        }
    }

    /**
     * Retired compatibility hook for task code that used to confirm local smart-click evidence.
     * CR169 removed local NPC click learning/strategy evidence from this service; verified cloud
     * actions are owned by NPC_CLICK_SMART and no local pending evidence is recorded here.
     */
    @Deprecated(since = "CR169", forRemoval = false)
    public void confirmPendingSmartClick(String mapName,
                                         String npcName,
                                         int mapX,
                                         int mapY,
                                         String verificationStrength,
                                         String reason) {
        log.debug("NPC smart-click local evidence confirmation ignored after CR169: npc={} map={} target=({}, {}) strength={} reason={}",
                npcName, mapName, mapX, mapY, verificationStrength, reason);
    }

    private static List<ResolvedNpcClickRegion> defaultNpcClickScanRegions(WindowBase windowBase) {
        if (windowBase == null) {
            return List.of();
        }
        return List.of(ResolvedNpcClickRegion.from(
                new OcrWindowRegion(0, 0, WINDOW_WIDTH, WINDOW_HEIGHT),
                windowBase.x(),
                windowBase.y()));
    }

    private static NpcClickSmartCloudRequest.Roi primaryScanRegionRoi(List<ResolvedNpcClickRegion> regions) {
        if (regions != null) {
            for (ResolvedNpcClickRegion region : regions) {
                OcrWindowRegion windowRegion = region == null ? null : region.windowRegion();
                if (windowRegion != null && windowRegion.isValid()) {
                    return NpcClickSmartCloudRequest.Roi.builder()
                            .x(windowRegion.x1())
                            .y(windowRegion.y1())
                            .width(windowRegion.width())
                            .height(windowRegion.height())
                            .build();
                }
            }
        }
        return NpcClickSmartCloudRequest.Roi.builder()
                .x(0)
                .y(0)
                .width(WINDOW_WIDTH)
                .height(WINDOW_HEIGHT)
                .build();
    }

    private static List<NpcClickSmartCloudRequest.ScanRegion> toCloudScanRegions(List<ResolvedNpcClickRegion> regions) {
        if (regions == null || regions.isEmpty()) {
            return List.of();
        }
        List<NpcClickSmartCloudRequest.ScanRegion> result = new ArrayList<>();
        for (int i = 0; i < regions.size(); i++) {
            ResolvedNpcClickRegion region = regions.get(i);
            OcrWindowRegion windowRegion = region == null ? null : region.windowRegion();
            if (windowRegion == null || !windowRegion.isValid()) {
                continue;
            }
            result.add(NpcClickSmartCloudRequest.ScanRegion.builder()
                    .index(i + 1)
                    .windowX(windowRegion.x1())
                    .windowY(windowRegion.y1())
                    .width(windowRegion.width())
                    .height(windowRegion.height())
                    .screenX(region.screenX1())
                    .screenY(region.screenY1())
                    .screenWidth(Math.max(0, region.screenX2() - region.screenX1()))
                    .screenHeight(Math.max(0, region.screenY2() - region.screenY1()))
                    .windowBaseX(region.windowBaseX())
                    .windowBaseY(region.windowBaseY())
                    .build());
        }
        return List.copyOf(result);
    }

    /**
     * Return the screen-absolute origin of the currently bound game window.
     *
     * <p>Multi-window tasks bind a {@link WindowRuntimeContext} before calling this service. That
     * native binding is the source of truth for mouse/capture coordinates; the legacy tracker base
     * can be stale in standalone debug mains or early task startup. Falling back to the tracker keeps
     * old single-window paths usable when no window context is bound.</p>
     *
     * @param source short diagnostic label written when the method falls back to tracker state.
     * @return screen-absolute top-left point of the game window used for capture and cloud action
     *         coordinate conversion.
     */
    private WindowBase currentWindowBase(String source) {
        Optional<WindowRuntimeContext> current = windowTaskContextHolder.rawCurrent();
        if (current.isPresent()) {
            WindowNativeBinding binding = current.get().getNativeBinding();
            if (binding != null && binding.hasGeometry()) {
                return new WindowBase(binding.getX(), binding.getY());
            }
        }

        WindowBase fallback = new WindowBase(tracker.getWindowBaseX(), tracker.getWindowBaseY());
        log.warn("NPC click using tracker window base fallback: source={} base=({}, {})",
                source, fallback.x(), fallback.y());
        return fallback;
    }

    private boolean shouldStop() {
        return Thread.currentThread().isInterrupted() || !InputActionScope.checkpoint();
    }

    @FunctionalInterface
    private interface NpcClickVerifier {
        NpcClickVerificationResult verify(String reason);
    }

    private record NpcClickVerificationResult(
            boolean verified,
            boolean optionDialogVisible,
            DialogResultStatus status,
            DialogType dialogType) {

        static NpcClickVerificationResult dialog(DialogResult result) {
            DialogResultStatus status = result == null ? DialogResultStatus.FAILED : result.getStatus();
            DialogType type = result == null ? DialogType.NONE : result.getDialogType();
            return new NpcClickVerificationResult(
                    status == DialogResultStatus.GREEN_TEMPLATE_VISIBLE,
                    type == DialogType.OPTION,
                    status,
                    type);
        }

        static NpcClickVerificationResult combat(boolean verified) {
            return new NpcClickVerificationResult(verified, false, null, DialogType.NONE);
        }

        static NpcClickVerificationResult deferredToTask() {
            return new NpcClickVerificationResult(true, false, null, DialogType.NONE);
        }
    }

    @Value

    @Builder

    @AllArgsConstructor(access = AccessLevel.PUBLIC)

    @Accessors(fluent = true)

    private static class WindowBase {

        int x;

        int y;

    }
}
