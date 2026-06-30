package com.bot.dhxy.service;

import com.bot.dhxy.model.npc.NpcTargetEvidence;
import com.bot.dhxy.task.model.TaskType;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.Accessors;



import com.bot.dhxy.model.ocr.LearnedNpcClickPoint;
import com.bot.dhxy.model.ocr.LocationInfo;
import com.bot.dhxy.model.ocr.OcrWordResult;
import com.bot.dhxy.model.ocr.OcrWindowRegion;
import com.bot.dhxy.model.ocr.PlayerAnchorMatch;
import com.bot.dhxy.model.ocr.ResolvedNpcClickRegion;
import com.bot.dhxy.model.ocr.TargetOcrResult;
import com.bot.dhxy.model.ocr.TextCandidate;
import com.bot.dhxy.model.ocr.TextCandidateScanResult;
import com.bot.dhxy.core.GameClientTracker;
import com.bot.dhxy.core.ImageFinder;
import com.bot.dhxy.core.TextRecognizer;
import com.bot.dhxy.input.InputProvider;
import com.bot.dhxy.input.InputSequences;
import com.bot.dhxy.input.action.InputAction;
import com.bot.dhxy.model.PlayerCharacter;
import com.bot.dhxy.model.dialog.DialogResult;
import com.bot.dhxy.model.dialog.DialogResultStatus;
import com.bot.dhxy.model.dialog.DialogType;
import com.bot.dhxy.model.npc.DirectCombatClickResult;
import com.bot.dhxy.model.npc.NpcClickRequest;
import com.bot.dhxy.model.npc.NpcRole;
import com.bot.dhxy.model.npc.NpcTooltipType;
import com.bot.dhxy.runner.context.TaskExecutionContextHolder;
import com.bot.dhxy.runner.stop.TaskCheckpoint;
import com.bot.dhxy.runner.stop.TaskSleep;
import com.bot.dhxy.service.dialog.DialogHandleRequest;
import com.bot.dhxy.tools.GameStateUtil;
import com.bot.dhxy.tools.CoordinateHelper;
import com.bot.dhxy.tools.ImagePreprocessor;
import com.bot.dhxy.tools.LatencyMetrics;
import com.bot.dhxy.vision.GameTextLineOcrService;
import com.bot.dhxy.vision.LocationVisionService;
import com.bot.dhxy.vision.OcrRoiMemoryService;
import com.bot.dhxy.vision.OcrTextMatcher;
import com.bot.dhxy.vision.OcrWindowScanService;
import com.bot.dhxy.window.model.WindowDialogSnapshot;
import com.bot.dhxy.window.model.WindowNativeBinding;
import com.bot.dhxy.window.runtime.WindowRuntimeContext;
import com.bot.dhxy.window.runtime.WindowScopedTempPath;
import com.bot.dhxy.window.runtime.WindowTaskContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Clicks NPCs in the currently bound game window through one public request-based entry.
 *
 * <p>All real mouse/keyboard operations must be serialized through {@link InputSequences}, except
 * code already running inside an exclusive input callback where direct {@link InputProvider} calls
 * are required to avoid queue-in-queue deadlock. Coordinates passed to public click methods are
 * logical game map coordinates unless explicitly described as screen-absolute or window-relative.</p>
 *
 * <p>The smart-click order is deliberately conservative. A request may opt into tooltip-first mode
 * for targets where the body/name can be hidden behind a story dialog while the tooltip is still
 * visible.</p>
 * <ol>
 *     <li>verified learned click point for the exact target;</li>
 *     <li>visible NPC task tooltip template in the recommended ROI;</li>
 *     <li>vision-memory recommended yellow-name search regions;</li>
 *     <li>fixed-target player-anchor formula;</li>
 *     <li>Ctrl nearby-NPC menu fallback.</li>
 * </ol>
 *
 * <p>Task classes should build a {@link NpcClickRequest} and call {@link #clickNpcSmart(NpcClickRequest)}
 * instead of directly choosing one of the private strategies. OCR scan regions are resolved from
 * the vision-memory recommendation path, not from task-local hardcoded rectangles.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NpcClickService implements SmartClickEvidenceConfirmationService {

    private final InputProvider inputProvider;
    private final InputSequences inputSequences;
    private final GameClientTracker tracker;
    private final GameStateUtil gameStateUtil;
    private final BattleRadarService battleRadarService;
    private final TextRecognizer ocr;
    private final LocationVisionService locationVisionService;
    private final PlayerStateService playerStateService;
    private final CoordinateHelper coordinateHelper;
    private final DialogService dialogService;
    private final WindowScopedTempPath windowScopedTempPath;
    private final WindowTaskContextHolder windowTaskContextHolder;
    private final TaskExecutionContextHolder taskExecutionContextHolder;
    private final OcrRoiMemoryService ocrRoiMemoryService;
    private final GameTextLineOcrService gameTextLineOcrService;
    private final ConcurrentMap<String, PendingSmartClickEvidence> pendingSmartClickEvidence = new ConcurrentHashMap<>();

    private static final double UX = 20.0;
    private static final double UY = 0.0;
    private static final double VX = 0.0;
    private static final double VY = -20.0;
    private static final int WINDOW_WIDTH = 1024;
    private static final int WINDOW_HEIGHT = 768;
    private static final int YELLOW_TARGET_CLICK_RETRIES = 1;
    private static final String STRICT_YELLOW_TARGET_JIANGMO_SHIWEI = "降魔侍卫";
    private static final int STRICT_YELLOW_TARGET_JIANGMO_SHIWEI_MIN_COMMON = 3;
    private static final int PURPLE_BLOB_MIN_PIXELS = 20;
    private static final int PURPLE_BLOB_MIN_WIDTH = 8;
    private static final int PURPLE_BLOB_MIN_HEIGHT = 4;
    private static final int PURPLE_BLOB_MAX_PIXELS = 6000;
    private static final int PURPLE_BLOB_MAX_WIDTH = 360;
    private static final int PURPLE_BLOB_MAX_HEIGHT = 140;
    private static final int NON_COMBAT_CTRL_PROMPT_MAX_SCREEN_DISTANCE = 15;
    private static final long NPC_PRE_CLICK_DIALOG_SNAPSHOT_MAX_AGE_MS = 3_000L;
    private static final int NPC_PIPELINE_HIDE_PLAYER_NAMES_SETTLE_MS = 400;

    private static final int[][] CTRL_OFFSETS_DIRECT = {
            {0, 0}
    };
    private static final int[][] CTRL_OFFSETS_SMALL_RING = {
            {0, 0},
            {8, -8}, {8, 0}, {0, -8},
            {-8, 0}, {0, 8}, {-8, -8}, {-8, 8}, {8, 8}
    };
    /*
     * Ctrl-hover menus in DHXY usually open to the right/up of the current cursor when the player
     * stands near the target. Keep the same coverage as before, but try right/up offsets first so a
     * successful fallback does not spend ten-plus probes on less likely quadrants.
     */
    private static final int[][] CTRL_OFFSETS_FULL_RING = {
            {0, 0},
            {16, -16}, {16, 0}, {0, -16},
            {8, -8}, {8, 0}, {0, -8},
            {16, 16}, {0, 16}, {-16, 0},
            {-16, -16}, {-8, -8}, {-8, 0},
            {-16, 16}, {-8, 8}, {8, 8}, {0, 8}
    };

    private static final String NPC_TAG_REGEX = "(?i).*(NPC|IPC|PC|NP).*";
    private static final String NPC_TAG_TEMPLATE_PATH = "images/template/npc/npc_tag.png";
    private static final String NPC_TASK_TOOLTIP_TEMPLATE_PATH = "images/template/npc/npc_task_tooltip.png";
    private static final double NPC_TASK_TOOLTIP_MATCH_RATE = 0.82;
    private static final double NPC_TASK_TOOLTIP_DEDUP_DISTANCE_PX = 36.0;
    private static final int CTRL_MENU_SCAN_W = 150;
    private static final int CTRL_MENU_SCAN_H = 120;
    private static final int NPC_LEFT_CLICK_HOLD_MS = 150;
    private static final int DIRECT_COMBAT_EXIT_ATTEMPTS = 3;


    /**
     * Submit one atomic move+click request and verify the caller-defined success signal.
     *
     * <p>Move and click must stay in one input-queue request; splitting them allows another window
     * to insert focus/mouse actions between the move and click. Coordinates are screen-absolute.</p>
     */
    private boolean executeMoveClickAndVerify(
            String description,
            int x,
            int y,
            long firstWaitMs,
            int maxRetries,
            NpcClickVerifier verifier) {
        if (shouldStop()) return false;
        log.info("NPC move+click sequence: {} point=({}, {})", description, x, y);
        boolean queued = inputSequences.submitAndWait(description, List.of(
                InputAction.moveMouse(x, y),
                InputAction.sleep(150),
                InputAction.clickLeft(x, y, NPC_LEFT_CLICK_HOLD_MS),
                InputAction.sleep((int) firstWaitMs)
        ));
        if (!queued) {
            log.warn("NPC move+click sequence failed in input queue: {} point=({}, {})", description, x, y);
            return false;
        }

        if (shouldStop()) return false;
        if (verifier.verify(description + ":firstVerify")) return true;

        for (int i = 1; i <= maxRetries; i++) {
            if (shouldStop()) return false;
            log.warn("NPC move+click retry {} point=({}, {})", i, x, y);
            queued = inputSequences.submitAndWait(description + ":retry", List.of(
                    InputAction.moveMouse(x, y),
                    InputAction.sleep(150),
                    InputAction.clickLeft(x, y, NPC_LEFT_CLICK_HOLD_MS),
                    InputAction.sleep(1000)
            ));
            if (!queued) {
                log.warn("NPC move+click retry failed in input queue: {} retry={} point=({}, {})", description, i, x, y);
                return false;
            }
            if (shouldStop()) return false;
            if (verifier.verify(description + ":retryVerify:" + i)) return true;
        }
        return false;
    }

    private boolean executeClickAndVerifyDirect(
            int x,
            int y,
            long firstWaitMs,
            int maxRetries,
            NpcClickVerifier verifier) {
        if (shouldStop()) return false;
        inputProvider.clickLeft(x, y, NPC_LEFT_CLICK_HOLD_MS);
        if (!TaskSleep.sleep(firstWaitMs)) return false;
        if (shouldStop()) return false;
        if (verifier.verify("npcClick:direct:firstVerify")) return true;

        for (int i = 1; i <= maxRetries; i++) {
            if (shouldStop()) return false;
            log.warn("NPC direct click retry {}", i);
            inputProvider.clickLeft(x, y, NPC_LEFT_CLICK_HOLD_MS);
            if (!TaskSleep.sleep(1000)) return false;
            if (shouldStop()) return false;
            if (verifier.verify("npcClick:direct:retryVerify:" + i)) return true;
        }
        return false;
    }

    private NpcClickVerifier dialogClickVerifier(String expectedDialogTemplatePath) {
        return reason -> isExpectedDialogVisible(expectedDialogTemplatePath, reason);
    }

    private NpcClickVerifier dialogClickVerifier(NpcClickRequest request) {
        if (request == null || request.expectedDialogTemplatePaths() == null
                || request.expectedDialogTemplatePaths().isEmpty()) {
            return dialogClickVerifier(request == null ? null : request.expectedDialogTemplatePath());
        }
        return reason -> isExpectedDialogVisible(request.expectedDialogTemplatePaths(), reason);
    }

    private NpcClickVerifier combatClickVerifier() {
        return this::isCombatVisibleAfterDirectClick;
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
    private boolean isExpectedDialogVisible(String expectedDialogTemplatePath, String reason) {
        DialogResult result = dialogService.handleDialog(DialogHandleRequest.verifyExpectedOptionDialog(
                "npc-click:expected-dialog:" + reason, expectedDialogTemplatePath));
        return result.getStatus() == DialogResultStatus.OPTION_VISIBLE
                || result.getStatus() == DialogResultStatus.GREEN_TEMPLATE_VISIBLE;
    }

    /**
     * Verify that an NPC click opened one of the expected option dialogs without clicking it.
     *
     * @param expectedDialogTemplatePaths expected green option templates.
     * @param reason diagnostic source used in logs and temp screenshot names.
     * @return true only when one expected option dialog is visible.
     */
    private boolean isExpectedDialogVisible(List<String> expectedDialogTemplatePaths, String reason) {
        DialogResult result = dialogService.handleDialog(DialogHandleRequest.verifyExpectedOptionDialog(
                "npc-click:expected-dialog:" + reason, expectedDialogTemplatePaths));
        return result.getStatus() == DialogResultStatus.OPTION_VISIBLE
                || result.getStatus() == DialogResultStatus.GREEN_TEMPLATE_VISIBLE;
    }

    private NpcClickStrategyResult clickNpcByCtrlMenuScan(
            String targetKeyword,
            String npcTagTemplatePath,
            NpcClickVerifier verifier,
            List<CtrlProbeOrigin> preferredProbePoints) {
        return clickNpcByCtrlMenuScan(
                targetKeyword, npcTagTemplatePath, verifier, preferredProbePoints, false);
    }

    private NpcClickStrategyResult clickNpcByCtrlMenuScan(
            String targetKeyword,
            String npcTagTemplatePath,
            String expectedDialogTemplatePath,
            List<CtrlProbeOrigin> preferredProbePoints,
            boolean includeWindowCenterFallback) {
        return clickNpcByCtrlMenuScan(
                targetKeyword, npcTagTemplatePath, dialogClickVerifier(expectedDialogTemplatePath),
                preferredProbePoints, includeWindowCenterFallback);
    }

    private NpcClickStrategyResult clickNpcByCtrlMenuScan(
            String targetKeyword,
            String npcTagTemplatePath,
            NpcClickVerifier verifier,
            List<CtrlProbeOrigin> preferredProbePoints,
            boolean includeWindowCenterFallback) {
        if (targetKeyword == null || targetKeyword.isBlank()) {
            log.warn("NPC ctrl menu scan requested without target keyword");
            return NpcClickStrategyResult.skipped(NpcClickStrategySource.CTRL_MENU, "missing-target-keyword");
        }
        TaskCheckpoint.throwIfStopRequested(taskExecutionContextHolder, "npc ctrl menu scan interrupted before start");
        if (shouldStop()) {
            return NpcClickStrategyResult.failed(NpcClickStrategySource.CTRL_MENU, "interrupted-before-ctrl-scan");
        }

        /*
         * Ctrl-menu probing is intentionally last-resort. Try task-derived origins first: formula
         * origins already account for the difference between the NPC coordinate and the approach
         * coordinate the task navigated to. The window center remains as the broad generic fallback
         * after those higher-confidence points.
         */
        WindowBase windowBase = currentWindowBase("ctrl-menu-scan");
        int gameBaseX = windowBase.x();
        int gameBaseY = windowBase.y();
        List<CtrlProbeOrigin> probeOrigins = normalizeCtrlProbeOrigins(
                preferredProbePoints,
                new Point(gameBaseX + (WINDOW_WIDTH / 2), gameBaseY + (WINDOW_HEIGHT / 2) + 20),
                windowBase,
                includeWindowCenterFallback);
        log.info("NPC ctrl menu probe origins: keyword={} origins={}", targetKeyword, summarizeCtrlProbeOrigins(probeOrigins));

        for (CtrlProbeOrigin origin : probeOrigins) {
            int originIndex = probeOrigins.indexOf(origin) + 1;
            int[][] offsets = origin.profile().offsets();
            for (int offsetIndex = 0; offsetIndex < offsets.length; offsetIndex++) {
                int[] offset = offsets[offsetIndex];
                TaskCheckpoint.throwIfStopRequested(taskExecutionContextHolder, "npc ctrl menu scan interrupted before probe");
                if (shouldStop()) {
                    log.info("NPC ctrl menu scan stopped before probe");
                    return NpcClickStrategyResult.failed(NpcClickStrategySource.CTRL_MENU, "interrupted-before-ctrl-probe");
                }
                int testX = clamp(origin.point().x + offset[0], gameBaseX, gameBaseX + WINDOW_WIDTH - 1);
                int testY = clamp(origin.point().y + offset[1], gameBaseY, gameBaseY + WINDOW_HEIGHT - 1);
                log.info("NPC ctrl probe attempt: keyword={} originIndex={} source={} profile={} offsetIndex={} offset=({}, {}) test=({}, {})",
                        targetKeyword, originIndex, origin.source(), origin.profile(),
                        offsetIndex + 1, offset[0], offset[1], testX, testY);
                AtomicReference<NpcClickStrategyResult> resultRef = new AtomicReference<>();
                boolean success = inputSequences.submitExclusiveAndWait("npcClick:ctrlMenuScan:" + targetKeyword, () -> {
                    if (shouldStop()) {
                        resultRef.set(NpcClickStrategyResult.failed(
                                NpcClickStrategySource.CTRL_MENU, "interrupted-inside-ctrl-callback"));
                        return false;
                    }
                    /*
                     * This callback already owns the global input worker. Use direct InputProvider calls
                     * here; submitting a nested InputSequences request would deadlock behind this callback.
                     */
                    int[] changeRect = buildCtrlMenuScanRect(testX, testY);
                    BufferedImage frameBefore = tracker.captureToMemory(
                            "menu_before", changeRect[0], changeRect[1], changeRect[2], changeRect[3]);
                    ImagePreprocessor.saveDebugImage(frameBefore, windowScopedTempPath.resolve("menu_before.png"));
                    inputProvider.holdCtrl();
                    try {
                        /*
                         * The game opens the nearby-name menu from a Ctrl+hover transition. Moving
                         * first can leave a normal tooltip under the cursor, then pressing Ctrl may
                         * not refresh the hover state. Hold Ctrl before moving so the mouse motion
                         * itself is the event that creates the Ctrl menu.
                         */
                        if (!TaskSleep.sleep(80)) return false;
                        inputProvider.moveMouse(testX, testY);
                        if (!TaskSleep.sleep(280)) return false;
                        if (shouldStop()) return false;
                        BufferedImage frameAfter = tracker.captureToMemory(
                                "menu_after", changeRect[0], changeRect[1], changeRect[2], changeRect[3]);
                        ImagePreprocessor.saveDebugImage(frameAfter, windowScopedTempPath.resolve("menu_after.png"));
                        if (frameBefore != null && frameAfter != null) {
                            boolean changed = !ImageFinder.isMatch(frameBefore, frameAfter, 0.05);
                            frameBefore.flush();
                            frameAfter.flush();
                            if (!changed) {
                                resultRef.set(NpcClickStrategyResult.notFound(
                                        NpcClickStrategySource.CTRL_MENU,
                                        screenRectToWindowRegion(changeRect, windowBase),
                                        "ctrl menu did not visually change"));
                                return false;
                            }
                        }
                        /*
                         * Use OCR/fuzzy-name matching as the active Ctrl-menu decision path. The older
                         * "(NPC)" tag-template candidate scanner remains private below, but a tag alone
                         * cannot prove which nearby NPC is the task target.
                         */
                        log.info("NPC ctrl tag-template shortcut skipped for OCR validation: keyword={}", targetKeyword);
                        NpcClickStrategyResult result =
                                scanMenuAndVerifyKeywordDirect(
                                        changeRect,
                                        targetKeyword,
                                        verifier,
                                        new Point(testX, testY));
                        resultRef.set(result);
                        return result.verified();
                    } finally {
                        inputProvider.releaseCtrl();
                        TaskSleep.sleep(100);
                    }
                });

                TaskCheckpoint.throwIfStopRequested(taskExecutionContextHolder, "npc ctrl menu scan interrupted after probe");
                if (shouldStop()) {
                    log.info("NPC ctrl menu scan stopped after probe");
                    return NpcClickStrategyResult.failed(NpcClickStrategySource.CTRL_MENU, "interrupted-after-ctrl-probe");
                }
                if (success) {
                    return resultRef.get() == null
                            ? NpcClickStrategyResult.failed(NpcClickStrategySource.CTRL_MENU, "ctrl callback succeeded without result")
                            : resultRef.get();
                }
            }
        }

        log.warn("NPC ctrl menu scan failed: keyword={} npcTagTemplate={}",
                targetKeyword, npcTagTemplatePath);
        return NpcClickStrategyResult.notFound(NpcClickStrategySource.CTRL_MENU, "ctrl menu scan exhausted");
    }

    /**
     * Build one Ctrl-menu scan rectangle that covers all four possible menu quadrants.
     *
     * <p>The old implementation only captured the right-up quadrant from the cursor. That missed
     * menus when the game had to place the Ctrl menu to the left or below the cursor. Instead of
     * running four separate OCR scans, this method captures the union rectangle around the probe
     * point: left/right by {@link #CTRL_MENU_SCAN_W} and up/down by {@link #CTRL_MENU_SCAN_H}. The
     * result is screen-absolute and clamped to the active game window.</p>
     *
     * @param testX screen-absolute X coordinate where Ctrl is held.
     * @param testY screen-absolute Y coordinate where Ctrl is held.
     * @return screen-absolute rectangle [left, top, right, bottom] for one OCR pass.
     */
    private int[] buildCtrlMenuScanRect(int testX, int testY) {
        int left = testX - CTRL_MENU_SCAN_W;
        int top = testY - CTRL_MENU_SCAN_H;
        int right = testX + CTRL_MENU_SCAN_W;
        int bottom = testY + CTRL_MENU_SCAN_H;

        WindowBase windowBase = currentWindowBase("ctrl-menu-rect");
        int windowLeft = windowBase.x();
        int windowTop = windowBase.y();
        int windowRight = windowLeft + 1024;
        int windowBottom = windowTop + 768;

        left = Math.max(windowLeft, left);
        top = Math.max(windowTop, top);
        right = Math.min(windowRight, right);
        bottom = Math.min(windowBottom, bottom);
        if (right <= left) {
            right = Math.min(windowRight, left + CTRL_MENU_SCAN_W);
        }
        if (bottom <= top) {
            bottom = Math.min(windowBottom, top + CTRL_MENU_SCAN_H);
        }
        return new int[]{left, top, right, bottom};
    }



    private NpcClickStrategyResult scanMenuAndVerifyKeywordDirect(
            int[] scanRect,
            String targetKeyword,
            NpcClickVerifier verifier,
            Point ctrlHoverPointAbs) {
        if (scanRect == null || scanRect.length < 4) {
            return NpcClickStrategyResult.failed(NpcClickStrategySource.CTRL_MENU, "invalid-ctrl-scan-rect");
        }
        return scanMenuAndVerifyKeywordDirect(
                scanRect[0],
                scanRect[1],
                scanRect[2],
                scanRect[3],
                targetKeyword,
                verifier,
                ctrlHoverPointAbs);
    }

    private NpcClickStrategyResult scanMenuAndVerifyKeywordDirect(
            int scanX,
            int scanY,
            int scanRight,
            int scanBottom,
            String targetKeyword,
            NpcClickVerifier verifier,
            Point ctrlHoverPointAbs) {
        String menuScanPath = windowScopedTempPath.resolve("npc_menu_scan.png");
        String cleanPath = windowScopedTempPath.resolve("npc_menu_clean.png");
        WindowBase windowBase = currentWindowBase("ctrl-menu-keyword");
        OcrWindowRegion scanRegion = new OcrWindowRegion(
                scanX - windowBase.x(),
                scanY - windowBase.y(),
                scanRight - windowBase.x(),
                scanBottom - windowBase.y()).clamp(WINDOW_WIDTH, WINDOW_HEIGHT);

        captureCleanNameToFileDirect("NPC keyword menu scan", menuScanPath, scanX, scanY, scanRight, scanBottom, false);
        if (shouldStop()) {
            return NpcClickStrategyResult.failed(NpcClickStrategySource.CTRL_MENU, scanRegion, "interrupted-before-menu-ocr");
        }
        ImagePreprocessor.washYellowText(menuScanPath, cleanPath);

        List<OcrWordResult> menuWords = ocr.getAllTextResultsForMatch(
                cleanPath,
                "npc-menu-keyword:" + targetKeyword,
                words -> hasNpcMenuMatch(words, targetKeyword));
        if (menuWords != null) {
            for (OcrWordResult w : menuWords) {
                if (shouldStop()) {
                    return NpcClickStrategyResult.failed(NpcClickStrategySource.CTRL_MENU, scanRegion, "interrupted-during-menu-ocr");
                }
                String text = w.getText();
                if (text == null) {
                    continue;
                }
                boolean isNameMatch = OcrTextMatcher.isShortNameMatch(text, targetKeyword);
                boolean isTagMatch = text.matches(NPC_TAG_REGEX);
                if (!isNameMatch && !isTagMatch) {
                    continue;
                }
                int nameScore = OcrTextMatcher.shortNameMatchScore(text, targetKeyword);
                int clickX = scanX + w.getX();
                int clickY = scanY + w.getY();
                Point menuClickPointAbs = new Point(clickX, clickY);
                Point recordPointAbs = ctrlHoverPointAbs == null ? menuClickPointAbs : new Point(ctrlHoverPointAbs);
                Point recordPointRel = windowRelativePoint(recordPointAbs, windowBase);
                OcrWindowRegion matchedRect = new OcrWindowRegion(
                        scanX - windowBase.x() + w.getLeft(),
                        scanY - windowBase.y() + w.getTop(),
                        scanX - windowBase.x() + w.getLeft() + Math.max(1, w.getWidth()),
                        scanY - windowBase.y() + w.getTop() + Math.max(1, w.getHeight()))
                        .clamp(WINDOW_WIDTH, WINDOW_HEIGHT);
                log.info("NPC keyword menu matched text={} keyword={} nameMatch={} nameScore={} tagMatch={} click=({}, {})",
                        text, targetKeyword, isNameMatch, nameScore, isTagMatch, clickX, clickY);
                inputProvider.moveMouse(clickX, clickY);
                if (!TaskSleep.sleep(100)) {
                    return NpcClickStrategyResult.failed(NpcClickStrategySource.CTRL_MENU, scanRegion, "interrupted-before-menu-click");
                }
                boolean verified = executeClickAndVerifyDirect(clickX, clickY, 800, 1, verifier);
                return NpcClickStrategyResult.fromClick(
                        NpcClickStrategySource.CTRL_MENU,
                        verified ? NpcClickStrategyStatus.VERIFIED : NpcClickStrategyStatus.CLICK_NOT_VERIFIED,
                        scanRegion,
                        matchedRect,
                        recordPointAbs,
                        recordPointRel,
                        true,
                        verified,
                        verified ? "ctrl menu keyword verified text=" + text + " recordPoint=ctrlHover"
                                : "ctrl menu keyword click not verified text=" + text + " recordPoint=ctrlHover");
            }
        }
        return NpcClickStrategyResult.notFound(
                NpcClickStrategySource.CTRL_MENU,
                scanRegion,
                "ctrl menu keyword not found");
    }

    /**
     * Click an NPC or task target through the single public smart-click entry.
     *
     * <p>The caller supplies business facts only: target name, logical map coordinate, an optional
     * caller-chosen OCR search region, expected dialog template, and whether the target is roaming.
     * Strategy details such as learned-memory, region OCR, player-anchor formula,
     * and Ctrl-menu probing stay inside this service so task code does not grow separate click pipelines.</p>
     *
     * @param request immutable click request. Coordinates are logical in-game map coordinates. OCR
     *                regions are resolved from {@link OcrRoiMemoryService}; if memory has no
     *                justified recommendation, region-dependent strategies are skipped.
     * @return true when any strategy opens and verifies the expected dialog.
     */
    public boolean clickNpcSmart(NpcClickRequest request) {
        NpcClickVerifier verifier = dialogClickVerifier(request);
        if (runNpcClickPipeline(request, verifier, "dialog")) {
            return true;
        }
        if (shouldStop()) {
            return false;
        }
        if (request != null && request.targetRole() == NpcRole.COMBAT_TARGET) {
            /*
             * Combat targets have their own recovery path: first inspect an already-open "看打"
             * dialog, then try Alt+A direct-combat click, and only the task phase retry toggles
             * mount with Alt+C. Keeping the generic NPC retry here would add an extra full click
             * pipeline before the direct-combat fallback.
             */
            log.info("NPC smart click skips Alt+C retry for combat target: npcName={} task={} map={} target=({}, {})",
                    request.npcName(), request.sourceTask(), request.mapName(), request.mapX(), request.mapY());
            return false;
        }
        log.info("NPC smart click first attempt failed; press Alt+C before retry: npcName={} task={} map={} target=({}, {})",
                request == null ? null : request.npcName(),
                request == null ? null : request.sourceTask(),
                request == null ? null : request.mapName(),
                request == null ? null : request.mapX(),
                request == null ? null : request.mapY());
        boolean dismountSubmitted = inputSequences.submitAndWait("npcClick:retry:altC-dismount", List.of(
                InputAction.pressAltC(),
                InputAction.sleep(700)
        ));
        if (!dismountSubmitted || shouldStop()) {
            log.warn("NPC smart click retry skipped after Alt+C submit failed/stopped: npcName={} submitted={}",
                    request == null ? null : request.npcName(), dismountSubmitted);
            return false;
        }
        return runNpcClickPipeline(request, verifier, "dialog-after-alt-c");
    }

    /**
     * Try to enter combat by switching to the game's direct combat-click mode, then running the
     * same target-click strategy chain as {@link #clickNpcSmart(NpcClickRequest)}.
     *
     * <p>This path is for 修罗/monster targets whose yellow tooltip or dialog trigger is blocked by
     * fixed UI. It does not invent a second targeting algorithm: learned points, tooltip matching,
     * player-anchor formula, yellow OCR, and Ctrl-menu probing all stay in the normal pipeline. The
     * only policy changes are the pre-click Alt+A mode switch and combat-radar verification. If the
     * task is stopped/interrupted, the method intentionally does not right-click out of the mode, so
     * the user's stop/pause command remains the owner of recovery.</p>
     *
     * @param request immutable NPC/monster target request. Coordinates are logical map coordinates;
     *                generated click points are screen-absolute through the existing smart-click
     *                conversion path.
     * @return structured result. Failed attempts after Alt+A was entered are marked
     *         position-refresh-required because canceling direct-combat mode can move the character.
     */
    public DirectCombatClickResult tryDirectCombatTargetClick(NpcClickRequest request) {
        if (request == null) {
            log.warn("NPC direct-combat click requested with null request");
            return DirectCombatClickResult.skipped("null-request");
        }
        if (shouldStop()) {
            return DirectCombatClickResult.skipped("stop-requested-before-direct-combat");
        }

        GameStateUtil.FlyingState flyingState = gameStateUtil.detectFlyingState(
                "npc-direct-combat:pre-alt-a:" + request.npcName());
        log.info("NPC direct-combat preflight flying state: npcName={} task={} map={} target=({}, {}) state={}",
                request.npcName(), request.sourceTask(), request.mapName(), request.mapX(), request.mapY(), flyingState);
        if (flyingState == GameStateUtil.FlyingState.FLYING) {
            boolean dismountSubmitted = inputSequences.submitAndWait("npcClick:directCombat:altC-dismount", List.of(
                    InputAction.pressAltC(),
                    InputAction.sleep(700)
            ));
            log.info("NPC direct-combat confirmed flying; Alt+C dismount submitted: npcName={} submitted={}",
                    request.npcName(), dismountSubmitted);
            if (!dismountSubmitted || shouldStop()) {
                return DirectCombatClickResult.skipped("direct-combat-dismount-failed-or-stopped");
            }
        } else if (flyingState == GameStateUtil.FlyingState.UNKNOWN) {
            log.warn("NPC direct-combat skipped because flying state is unknown: npcName={} task={} map={} target=({}, {})",
                    request.npcName(), request.sourceTask(), request.mapName(), request.mapX(), request.mapY());
            return DirectCombatClickResult.skipped("direct-combat-flying-state-unknown");
        }

        boolean enteredMode = inputSequences.submitAndWait("npcClick:directCombat:enterAltA", List.of(
                InputAction.pressAltA(),
                InputAction.sleep(350)
        ));
        if (!enteredMode || shouldStop()) {
            log.warn("NPC direct-combat click could not enter Alt+A mode: npcName={} enteredMode={}",
                    request.npcName(), enteredMode);
            return DirectCombatClickResult.skipped("direct-combat-alt-a-not-entered");
        }
        log.info("NPC direct-combat click mode entered: npcName={} map={} target=({}, {}) modeLikely={}",
                request.npcName(), request.mapName(), request.mapX(), request.mapY(),
                gameStateUtil.isDirectCombatClickModeLikely("npc-direct-combat-entered"));

        boolean clickedIntoCombat = runNpcClickPipeline(request, combatClickVerifier(), "direct-combat");
        if (clickedIntoCombat || shouldStop()) {
            return clickedIntoCombat
                    ? DirectCombatClickResult.combatEntered("direct-combat-click-confirmed")
                    : DirectCombatClickResult.skipped("stop-requested-after-direct-combat");
        }

        boolean exited = exitDirectCombatClickModeAfterFailure(request);
        if (!exited) {
            throw new IllegalStateException("Direct combat click mode exit was not confirmed; abort follow-up cleanup/retry");
        }
        log.warn("NPC direct-combat failed after Alt+A and exited; caller must refresh target position before retry: npcName={} task={} map={} target=({}, {})",
                request.npcName(), request.sourceTask(), request.mapName(), request.mapX(), request.mapY());
        return DirectCombatClickResult.positionRefreshRequired("direct-combat-failed-after-alt-a");
    }

    private boolean exitDirectCombatClickModeAfterFailure(NpcClickRequest request) {
        if (shouldStop()) {
            return false;
        }

        for (int attempt = 1; attempt <= DIRECT_COMBAT_EXIT_ATTEMPTS; attempt++) {
            if (shouldStop()) {
                return false;
            }
            Point exitPoint = findPlayerAnchorForDirectCombatExit(request);
            if (exitPoint == null) {
                WindowBase base = currentWindowBase("direct-combat-exit-fallback");
                exitPoint = new Point(base.x() + WINDOW_WIDTH / 2, base.y() + WINDOW_HEIGHT / 2 + 40);
                log.warn("NPC direct-combat exit attempt {} uses window-center fallback: npcName={} point=({}, {})",
                        attempt, request.npcName(), exitPoint.x, exitPoint.y);
            } else {
                log.info("NPC direct-combat exit attempt {} uses purple/player anchor: npcName={} point=({}, {})",
                        attempt, request.npcName(), exitPoint.x, exitPoint.y);
            }
            boolean submitted = inputSequences.submitAndWait("npcClick:directCombat:exitRightClick", List.of(
                    InputAction.moveMouse(exitPoint.x, exitPoint.y),
                    InputAction.sleep(120),
                    InputAction.clickRight(exitPoint.x, exitPoint.y, 120),
                    InputAction.sleep(600)
            ));
            if (!submitted) {
                log.warn("NPC direct-combat exit right-click was not submitted: attempt={} npcName={}",
                        attempt, request.npcName());
                return false;
            }
            if (shouldStop()) {
                return false;
            }
            boolean modeLikely = gameStateUtil.isDirectCombatClickModeLikely(
                    "npc-direct-combat-exit-attempt-" + attempt);
            log.info("NPC direct-combat exit verification: attempt={} modeLikely={}", attempt, modeLikely);
            if (!modeLikely) {
                return true;
            }
            TaskSleep.sleep(300);
        }
        log.error("NPC direct-combat exit not confirmed after {} attempts: npcName={} map={} target=({}, {})",
                DIRECT_COMBAT_EXIT_ATTEMPTS, request.npcName(), request.mapName(), request.mapX(), request.mapY());
        return false;
    }

    private Point findPlayerAnchorForDirectCombatExit(NpcClickRequest request) {
        try {
            LocationInfo playerLocation = cachedPlayerLocation(request);
            List<ResolvedNpcClickRegion> regions = resolveNpcScanRegions(request, playerLocation);
            if (regions == null || regions.isEmpty()) {
                return null;
            }
            FormulaClickPrediction prediction = calculatePlayerAnchorFormulaPoint(
                    request.player(), request.mapName(), request.mapX(), request.mapY(),
                    request.npcName(), request.tuneX(), request.tuneY(),
                    regions.get(0), playerLocation, true);
            return prediction == null || prediction.playerAnchorAbs() == null
                    ? null
                    : new Point(prediction.playerAnchorAbs());
        } catch (RuntimeException e) {
            log.warn("NPC direct-combat exit anchor probe failed: npcName={} reason={}",
                    request == null ? null : request.npcName(), e.getMessage(), e);
            return null;
        }
    }

    private boolean runNpcClickPipeline(NpcClickRequest request,
                                        NpcClickVerifier verifier,
                                        String verificationMode) {
        long latencyStart = LatencyMetrics.start();
        boolean result = false;
        try {
            if (shouldStop()) return false;
            if (request == null || request.npcName() == null || request.npcName().isBlank()) {
                log.warn("NPC smart click requested without a target name");
                return false;
            }

            /*
             * Keep this method as a strategy pipeline. Task code supplies target facts only; learned
             * OCR/ROI memory supplies scan regions so old task-local rectangles do not bypass policy.
             */
            LocationInfo playerLocation = cachedPlayerLocation(request);
            if (playerLocation == null) {
                log.warn("NPC smart click has no cached player coordinate; learning/recommendation will use full-window fallback only: npcName={} map={} target=({}, {})",
                        request.npcName(), request.mapName(), request.mapX(), request.mapY());
            }
            List<ResolvedNpcClickRegion> targetScanRegions = resolveNpcScanRegions(request, playerLocation);
            boolean directCombatClickMode = "direct-combat".equals(verificationMode);
            NpcClickPipelineState pipelineState = new NpcClickPipelineState(
                    playerLocation, targetScanRegions, directCombatClickMode);
            log.info("NPC smart click request: npcName={} task={} role={} evidence={} map={} target=({}, {}) player=({}, {}) roaming={} regions={} expectedTemplate={}",
                    request.npcName(), request.sourceTask(), request.targetRole(), request.targetEvidence(),
                    request.mapName(), request.mapX(), request.mapY(),
                    playerLocation == null ? null : playerLocation.x,
                    playerLocation == null ? null : playerLocation.y,
                    request.roamingTarget(), summarizeRegions(targetScanRegions), request.expectedDialogTemplatePath());

            boolean learnedMemoryTriedBeforeNameLayer = false;
            boolean canTryLearnedMemoryBeforeNameLayer = !directCombatClickMode
                    && !request.sourceTask().equals(TaskType.WUBEI)
                    && request.targetRole() != NpcRole.COMBAT_TARGET;
            if (canTryLearnedMemoryBeforeNameLayer) {
                DialogType dialogType = currentPreClickDialogType(request, "before-early-learned-memory");
                if (dialogType == DialogType.STORY) {
                    DialogResult cleanupResult = dialogService.handleDialog(
                            DialogHandleRequest.clickStory("npc-click:pre-clean-story:" + request.npcName()));
                    log.info("NPC smart click pre-cleaned blocking story dialog: npcName={} status={}",
                            request.npcName(), cleanupResult.getStatus());
                    dialogType = dialogService.detectDialogTypeNoFocus("after-pre-clean-story", false, 0);
                    if (dialogType != DialogType.NONE) {
                        log.warn("NPC smart click still has blocking dialog after story cleanup; skip target click: npcName={} remainingType={}",
                                request.npcName(), dialogType);
                        return false;
                    }
                }
                if (dialogType == DialogType.OPTION) {
                    log.warn("NPC smart click found existing option dialog before target click; skip generic cleanup: npcName={} map={} target=({}, {})",
                            request.npcName(), request.mapName(), request.mapX(), request.mapY());
                    return false;
                }

                learnedMemoryTriedBeforeNameLayer = true;
                if (dialogType == DialogType.NONE && tryLearnedMemoryStrategy(request, verifier, pipelineState)) {
                    result = true;
                    return true;
                }
            }

            if (directCombatClickMode) {
                log.info("NPC smart click skips name-layer preparation in direct-combat mode: npcName={} reason=skip repeated Alt+4",
                        request.npcName());
            } else if (!prepareNpcPipelineNameLayerOnce(request, verificationMode)) {
                return false;
            }

            boolean lightScan = request.targetEvidence().equals(NpcTargetEvidence.TENTATIVE);
            if (request.sourceTask().equals(TaskType.WUBEI)) {
                if (tryNormalTooltipStrategy(request, verifier, pipelineState)) {
                    result = true;
                    return true;
                }
            }
            DialogType dialogType = DialogType.NONE;
            if (directCombatClickMode) {
                /*
                 * Once Alt+A has entered target-pick mode, visible dialog snapshots can be stale or
                 * unrelated to the combat target. CR82 keeps normal NPC safety gates intact, but this
                 * path must proceed to tooltip/yellow/formula/Ctrl target strategies without dialog
                 * cleanup or pre-click gating.
                 */
                log.info("NPC smart click skips pre-click dialog gate in direct-combat mode: npcName={} map={} target=({}, {})",
                        request.npcName(), request.mapName(), request.mapX(), request.mapY());
            } else {
                dialogType = currentPreClickDialogType(request, "before-learned-memory");
                if (dialogType == DialogType.STORY) {
                    DialogResult cleanupResult = dialogService.handleDialog(
                            DialogHandleRequest.clickStory("npc-click:pre-clean-story:" + request.npcName()));
                    log.info("NPC smart click pre-cleaned blocking story dialog: npcName={} status={}",
                            request.npcName(), cleanupResult.getStatus());
                    dialogType = dialogService.detectDialogTypeNoFocus("after-pre-clean-story", false, 0);
                    if (dialogType != DialogType.NONE) {
                        log.warn("NPC smart click still has blocking dialog after story cleanup; skip target click: npcName={} remainingType={}",
                                request.npcName(), dialogType);
                        return false;
                    }
                }
                if (dialogType == DialogType.OPTION) {
                    log.warn("NPC smart click found existing option dialog before target click; skip generic cleanup: npcName={} map={} target=({}, {})",
                            request.npcName(), request.mapName(), request.mapX(), request.mapY());
                    return false;
                }
            }

            if (!learnedMemoryTriedBeforeNameLayer
                    && dialogType == DialogType.NONE
                    && tryLearnedMemoryStrategy(request, verifier, pipelineState)) {
                result = true;
                return true;
            }

            if (!request.sourceTask().equals(TaskType.WUBEI) && tryNormalTooltipStrategy(request, verifier, pipelineState)) {
                result = true;
                return true;
            }

            if (!directCombatClickMode) {
                dialogType = dialogService.detectDialogTypeNoFocus("after-tooltip", false, 0);
            }

            if (!lightScan && dialogType == DialogType.NONE && tryYellowTargetStrategy(request, verifier, pipelineState)) {
                result = true;
                return true;
            }

            if (!lightScan && dialogType == DialogType.NONE && tryPlayerAnchorFormulaStrategy(request, verifier, pipelineState)) {
                result = true;
                return true;
            }

            if (!lightScan && dialogType == DialogType.NONE && tryCtrlMenuStrategy(request, verifier, pipelineState)) {
                result = true;
                return true;
            }

            log.error("NPC click failed: {}", request.npcName());
            return false;
        } catch (RuntimeException e) {
            log.error("NPC smart click exception: npcName={} map={} target=({}, {}) expectedTemplate={}",
                    request == null ? null : request.npcName(),
                    request == null ? null : request.mapName(),
                    request == null ? null : request.mapX(),
                    request == null ? null : request.mapY(),
                    request == null ? null : request.expectedDialogTemplatePath(),
                    e);
            throw e;
        } finally {
            String target = request == null ? "-" : request.npcName() + "@" + request.mapName()
                    + "(" + request.mapX() + "," + request.mapY() + ")";
            LatencyMetrics.info(log, "npc.click.smart", latencyStart,
                    "result=" + result + " verification=" + verificationMode + " target=" + target);
        }
    }

    /**
     * Prepare the visible name layer once for a single smart-click pipeline.
     *
     * <p>All strategies in {@link #runNpcClickPipeline(NpcClickRequest, NpcClickVerifier, String)}
     * inspect the same stationary scene. Pressing Alt+4 inside each detector creates duplicate input
     * and extra focus opportunities, so the pipeline owns this one-time preparation and downstream
     * dialog/OCR captures only read the already-prepared scene.</p>
     */
    private boolean prepareNpcPipelineNameLayerOnce(NpcClickRequest request, String verificationMode) {
        if (shouldStop()) {
            return false;
        }
        boolean ok = inputSequences.submitAndWait("npcClick:pipeline-hide-player-names:" + request.npcName(), List.of(
                InputAction.pressAlt4(),
                InputAction.sleep(NPC_PIPELINE_HIDE_PLAYER_NAMES_SETTLE_MS)
        ));
        log.info("NPC smart click prepared name layer once: npcName={} verification={} result={}",
                request.npcName(), verificationMode, ok);
        return ok && !shouldStop();
    }


    private boolean tryLearnedMemoryStrategy(NpcClickRequest request,
                                             NpcClickVerifier verifier,
                                             NpcClickPipelineState state) {
        /*
         * 1. Fast remembered path: reuse a conservative learned click cluster for the same map/name/point.
         * If it misses, keep the attempted physical point as a Ctrl origin instead of throwing away
         * that evidence. Running this before tooltip matching makes it obvious in logs whether the
         * database/memory point is mature enough to replace screenshot matching for stable NPCs.
         */
        if (!hasKnownTargetCoordinate(request)) {
            log.info("NPC learned-memory strategy skipped: npcName={} reason=unknown-target-coordinate target=({}, {})",
                    request.npcName(), request.mapX(), request.mapY());
            return false;
        }
        NpcClickStrategyResult learnedResult = clickNpcByLearnedMemory(
                request.mapName(), request.mapX(), request.mapY(),
                request.npcName(), state.playerLocation, verifier);
        recordSmartClickEvidence(request, learnedResult, state.playerLocation);
        if (learnedResult.verified()) {
            return true;
        }
        addCtrlProbeOrigin(state.ctrlProbeOrigins, learnedResult.clickPointAbs(),
                "learned-memory", CtrlProbeScanProfile.SMALL_RING);
        return false;
    }

    private boolean tryNormalTooltipStrategy(NpcClickRequest request,
                                             NpcClickVerifier verifier,
                                             NpcClickPipelineState state) {
        /*
         * 2. Visible tooltip path: only targets that can show the standard task tooltip should try this.
         * Fixed transfer NPCs such as 张闻 do not have that tooltip; probing it only delays the formula
         * path that is already calibrated for them.
         */
        NpcClickStrategyResult tooltipResult =
                clickNpcByTaskTooltipTemplate(request, state.targetScanRegions, verifier);
        recordSmartClickEvidence(request, tooltipResult, state.playerLocation);
        return tooltipResult.verified();
    }

    private boolean tryPlayerAnchorFormulaStrategy(NpcClickRequest request,
                                                   NpcClickVerifier verifier,
                                                   NpcClickPipelineState state) {
        /*
         * 4. Player-anchor formula is a fallback after yellow-name OCR. The ROI recommendation is
         * derived from yellow target evidence, so it must not preempt a direct yellow match; otherwise
         * a yellow-only work region can be mistaken for a validated purple-name anchor region.
         */
        if (!hasKnownTargetCoordinate(request)) {
            log.info("NPC player-anchor formula strategy skipped: npcName={} reason=unknown-target-coordinate target=({}, {})",
                    request.npcName(), request.mapX(), request.mapY());
            return false;
        }
        FormulaClickPrediction formulaPrediction = state.targetScanRegions.isEmpty()
                ? null
                : calculatePlayerAnchorFormulaPoint(
                request.player(), request.mapName(), request.mapX(), request.mapY(),
                request.npcName(), request.tuneX(), request.tuneY(),
                state.targetScanRegions.get(0), state.playerLocation, state.skipDefaultOcrMask, false);
        if (formulaPrediction != null
                && formulaPrediction.predictedClickAbs() != null
                && !isInsideWindow(formulaPrediction.predictedClickAbs(), formulaPrediction.windowBaseAbs())) {
            NpcClickStrategyResult formulaResult = clickNpcByPlayerAnchorFormula(formulaPrediction, verifier);
            recordSmartClickEvidence(request, formulaResult, state.playerLocation);
            return false;
        }
        if (formulaPrediction != null && formulaPrediction.predictedClickAbs() != null) {
            state.ctrlProbeReferenceAbs = new Point(formulaPrediction.predictedClickAbs());
            addCtrlProbeOrigin(state.ctrlProbeOrigins, formulaPrediction.predictedClickAbs(),
                    "purple-formula-target", CtrlProbeScanProfile.SMALL_RING);
        }
        NpcClickStrategyResult formulaResult = clickNpcByPlayerAnchorFormula(formulaPrediction, verifier);
        recordSmartClickEvidence(request, formulaResult, state.playerLocation);
        if (formulaResult.verified()) {
            return true;
        }

        /*
         * If the player-anchor formula lands near the target but misses the direct left click, pay for
         * a very small Ctrl probe immediately. Recent Xiuluo logs showed this fixes the common
         * "off by a few pixels" case faster than running broad yellow-name OCR first.
         */
        if (formulaPrediction == null || formulaPrediction.predictedClickAbs() == null) {
            return false;
        }
        List<CtrlProbeOrigin> formulaCtrlOrigins = new ArrayList<>();
        addCtrlProbeOrigin(formulaCtrlOrigins, formulaPrediction.predictedClickAbs(),
                "formula-target:immediate", CtrlProbeScanProfile.SMALL_RING);
        NpcClickStrategyResult formulaCtrlResult = clickNpcByCtrlMenuScan(
                request.npcName(), NPC_TAG_TEMPLATE_PATH, verifier, formulaCtrlOrigins, false);
        recordSmartClickEvidence(request, formulaCtrlResult, state.playerLocation);
        return formulaCtrlResult.verified();
    }

    private boolean tryYellowTargetStrategy(NpcClickRequest request,
                                            NpcClickVerifier verifier,
                                            NpcClickPipelineState state) {
        /*
         * 3. Yellow-name visual path: search vision-memory recommended regions in order. Region
         * expansion is only allowed when the smaller region does not contain the target text. If the
         * target text is found but the resulting click does not verify the dialog, a larger region
         * would only add noise and may click another candidate, so the service moves to Ctrl fallback.
         */
        for (int i = 0; i < state.targetScanRegions.size(); i++) {
            ResolvedNpcClickRegion region = state.targetScanRegions.get(i);
            log.info("NPC yellow target strategy region {}/{}: {}",
                    i + 1, state.targetScanRegions.size(), region.toShortText());
            YellowTargetClickResult yellowResult = clickNpcByYellowTargetName(
                    request, region, verifier, state.skipDefaultOcrMask);
            recordSmartClickEvidence(request, yellowResult.evidence(), state.playerLocation);
            if (yellowResult.status() == YellowTargetClickStatus.CLICK_VERIFIED) {
                return true;
            }
            addCtrlProbeOrigin(state.ctrlProbeOrigins, yellowResult.attemptedClickPointAbs(),
                    "yellow-target:" + yellowResult.status(), CtrlProbeScanProfile.SMALL_RING);
            addCtrlProbeOrigins(state.ctrlProbeOrigins, yellowResult.ctrlProbePointsAbs(),
                    "yellow-candidate:" + yellowResult.status(), CtrlProbeScanProfile.DIRECT);
            if (!yellowResult.allowsRegionExpansion()) {
                log.info("NPC yellow target region expansion stopped: npcName={} result={} region={}",
                        request.npcName(), yellowResult.status(), region.toShortText());
                break;
            }
        }
        return false;
    }

    private boolean tryCtrlMenuStrategy(NpcClickRequest request,
                                        NpcClickVerifier verifier,
                                        NpcClickPipelineState state) {
        /*
         * 5. Last resort: hold Ctrl and inspect the game's nearby-NPC menu. This requires real input
         * inside one exclusive transaction, so it stays after the cheaper screenshot/OCR attempts.
         * It deliberately uses only task-derived origins such as learned memory, yellow target
         * points, and formula-derived target points; the old window-center origin was too imprecise.
         */
        List<CtrlProbeOrigin> ctrlOrigins = state.ctrlProbeOrigins;
        if (request.targetRole() != NpcRole.COMBAT_TARGET) {
            if (state.ctrlProbeReferenceAbs == null) {
                log.info("NPC ctrl menu skipped for non-combat target: npcName={} reason=missing-reference-point origins={}",
                        request.npcName(), summarizeCtrlProbeOrigins(state.ctrlProbeOrigins));
                return false;
            }
            int max = NON_COMBAT_CTRL_PROMPT_MAX_SCREEN_DISTANCE;
            List<CtrlProbeOrigin> filtered = new ArrayList<>();
            for (CtrlProbeOrigin origin : state.ctrlProbeOrigins) {
                if (origin == null || origin.point() == null) {
                    continue;
                }
                int dx = origin.point().x - state.ctrlProbeReferenceAbs.x;
                int dy = origin.point().y - state.ctrlProbeReferenceAbs.y;
                if ((dx * dx) + (dy * dy) <= max * max) {
                    filtered.add(origin);
                } else {
                    log.info("NPC ctrl menu origin skipped for non-combat target: npcName={} source={} point=({}, {}) reference=({}, {}) maxDistancePx={}",
                            request.npcName(), origin.source(), origin.point().x, origin.point().y,
                            state.ctrlProbeReferenceAbs.x, state.ctrlProbeReferenceAbs.y, max);
                }
            }
            ctrlOrigins = List.copyOf(filtered);
            if (ctrlOrigins.isEmpty()) {
                log.info("NPC ctrl menu skipped for non-combat target: npcName={} reason=no-nearby-origin reference=({}, {}) originalOrigins={}",
                        request.npcName(), state.ctrlProbeReferenceAbs.x, state.ctrlProbeReferenceAbs.y,
                        summarizeCtrlProbeOrigins(state.ctrlProbeOrigins));
                return false;
            }
        }
        NpcClickStrategyResult ctrlResult = clickNpcByCtrlMenuScan(
                request.npcName(), NPC_TAG_TEMPLATE_PATH, verifier, ctrlOrigins);
        recordSmartClickEvidence(request, ctrlResult, state.playerLocation);
        return ctrlResult.verified();
    }

    /**
     * Try the fastest Xiuluo-style NPC accept path by matching the visible task tooltip template.
     *
     * <p>The search regions are the same window-relative OCR ROI recommendations used by the later
     * yellow-name strategy. Each region is converted to a screen-absolute rectangle before calling
     * {@link CoordinateHelper#findImageInRegion(String, int[], double)}, because that helper captures
     * absolute desktop pixels through the current bound tracker. No mouse input is sent on a miss.
     * On a hit, the matched template center is clicked with one atomic move+click sequence, then the
     * caller-provided dialog template verifies that the right NPC dialog opened.</p>
     *
     * @param request smart-click request. Its expected dialog template is used for verification; if
     *                absent, verification falls back to a generic option-dialog check.
     * @param targetScanRegions normalized regions resolved for this target, with screen-absolute
     *                          rectangles already attached.
     * @return structured strategy result. {@link NpcClickStrategyStatus#VERIFIED} means this strategy
     *         finished the smart-click request; other statuses allow later strategies to run.
     */
    private NpcClickStrategyResult clickNpcByTaskTooltipTemplate(
            NpcClickRequest request,
            List<ResolvedNpcClickRegion> targetScanRegions,
            NpcClickVerifier verifier) {
        if (request.tooltipType() == NpcTooltipType.NONE) {
            log.info("NPC task-tooltip template skipped: npcName={} tooltipType={}",
                    request.npcName(), request.tooltipType());
            log.info("[npc-tooltip] skipped: task={} npc={} map={} target=({}, {}) reason=tooltip-disabled tooltipType={}",
                    request.sourceTask(), request.npcName(), request.mapName(), request.mapX(), request.mapY(),
                    request.tooltipType());
            return NpcClickStrategyResult.skipped(
                    NpcClickStrategySource.TASK_TOOLTIP_TEMPLATE,
                    "tooltip-disabled");
        }
        if (targetScanRegions == null || targetScanRegions.isEmpty()) {
            log.info("NPC task-tooltip template skipped: npcName={} reason=no-recommended-regions", request.npcName());
            log.info("[npc-tooltip] skipped: task={} npc={} map={} target=({}, {}) reason=no-recommended-regions",
                    request.sourceTask(), request.npcName(), request.mapName(), request.mapX(), request.mapY());
            return NpcClickStrategyResult.skipped(
                    NpcClickStrategySource.TASK_TOOLTIP_TEMPLATE,
                    "no-recommended-regions");
        }

        NpcClickStrategyResult lastMiss = null;
        for (int i = 0; i < targetScanRegions.size(); i++) {
            if (shouldStop()) {
                return NpcClickStrategyResult.failed(
                        NpcClickStrategySource.TASK_TOOLTIP_TEMPLATE,
                        "interrupted-before-template-match");
            }
            ResolvedNpcClickRegion region = targetScanRegions.get(i);
            if (region == null) {
                continue;
            }

            /*
             * Match the small tooltip image only inside the recommended ROI. This keeps the fast path
             * cheap and prevents a generic tooltip elsewhere in the client from becoming a click target.
             */
            int[] rect = region.screenRect();
            String tooltipTemplatePath = tooltipTemplatePath(request);
            List<Point> matchedPoints = coordinateHelper.findImagesInRegion(
                    tooltipTemplatePath, rect, NPC_TASK_TOOLTIP_MATCH_RATE, NPC_TASK_TOOLTIP_DEDUP_DISTANCE_PX);
            log.info("NPC task-tooltip template region {}/{}: npcName={} template={} region={} rect=[{},{},{},{}] matches={}",
                    i + 1, targetScanRegions.size(), request.npcName(), tooltipTemplatePath,
                    region.toShortText(), rect[0], rect[1], rect[2], rect[3], matchedPoints);
            log.info("[npc-tooltip] scan: task={} npc={} map={} target=({}, {}) regionIndex={}/{} template={} rect=[{},{},{},{}] matches={}",
                    request.sourceTask(), request.npcName(), request.mapName(), request.mapX(), request.mapY(),
                    i + 1, targetScanRegions.size(), tooltipTemplatePath,
                    rect[0], rect[1], rect[2], rect[3], matchedPoints);
            if (matchedPoints.isEmpty()) {
                continue;
            }

            /*
             * A template hit is a strong visual signal, but adjacent NPC/task tooltips can coexist
             * around 五倍 targets. Try every de-duplicated hit in score order and only fall through
             * after all visible tooltip candidates fail the expected dialog verification.
             */
            for (int matchIndex = 0; matchIndex < matchedPoints.size(); matchIndex++) {
                Point matchedPoint = matchedPoints.get(matchIndex);
                WindowBase regionWindowBase = windowBase(region);
                Point directNpcPoint = directNpcPointFromTooltipCenter(matchedPoint);
                Point directNpcPointRel = windowRelativePoint(directNpcPoint, regionWindowBase);
                OcrWindowRegion tooltipDerivedRoi = tooltipLearnedRoiFromTooltipCenter(
                        matchedPoint,
                        new Point(regionWindowBase.x(), regionWindowBase.y()));
                log.info("[npc-tooltip] click-start: task={} npc={} map={} target=({}, {}) candidate={}/{} point=({}, {}) regionIndex={}/{}",
                        request.sourceTask(), request.npcName(), request.mapName(), request.mapX(), request.mapY(),
                        matchIndex + 1, matchedPoints.size(), matchedPoint.x, matchedPoint.y,
                        i + 1, targetScanRegions.size());
                boolean verified = executeMoveClickAndVerify(
                        "npcClick:taskTooltipTemplate#" + (matchIndex + 1),
                        matchedPoint.x,
                        matchedPoint.y,
                        1200,
                        0,
                        verifier);
                log.info("NPC task-tooltip template click result: npcName={} candidate={}/{} point=({}, {}) verified={}",
                        request.npcName(), matchIndex + 1, matchedPoints.size(), matchedPoint.x, matchedPoint.y, verified);
                log.info("[npc-tooltip] click-result: task={} npc={} map={} target=({}, {}) candidate={}/{} point=({}, {}) verified={} recordPoint=({}, {})",
                        request.sourceTask(), request.npcName(), request.mapName(), request.mapX(), request.mapY(),
                        matchIndex + 1, matchedPoints.size(), matchedPoint.x, matchedPoint.y, verified,
                        directNpcPoint.x,
                        directNpcPoint.y);
                lastMiss = NpcClickStrategyResult.fromClick(
                        NpcClickStrategySource.TASK_TOOLTIP_TEMPLATE,
                        verified ? NpcClickStrategyStatus.VERIFIED : NpcClickStrategyStatus.CLICK_NOT_VERIFIED,
                        region.windowRegion(),
                        tooltipDerivedRoi,
                        directNpcPoint,
                        directNpcPointRel,
                        true,
                        verified,
                        verified ? "task-tooltip template verified; recordPoint=tooltipCenterY+90; roi=tooltip[-150,-100,+150,+200]"
                                : "task-tooltip clicked but expected dialog not verified; recordPoint=tooltipCenterY+90");
                if (verified) {
                    return lastMiss;
                }
            }
        }
        if (lastMiss != null) {
            log.info("[npc-tooltip] exhausted-after-clicks: task={} npc={} map={} target=({}, {}) status={} message={}",
                    request.sourceTask(), request.npcName(), request.mapName(), request.mapX(), request.mapY(),
                    lastMiss.status(), lastMiss.message());
            return lastMiss;
        }
        log.info("[npc-tooltip] not-found: task={} npc={} map={} target=({}, {}) regions={} template={} threshold={} dedupDistance={}",
                request.sourceTask(), request.npcName(), request.mapName(), request.mapX(), request.mapY(),
                targetScanRegions.size(), tooltipTemplatePath(request),
                NPC_TASK_TOOLTIP_MATCH_RATE, NPC_TASK_TOOLTIP_DEDUP_DISTANCE_PX);
        return NpcClickStrategyResult.notFound(
                NpcClickStrategySource.TASK_TOOLTIP_TEMPLATE,
                "task-tooltip template not found");
    }

    private String tooltipTemplatePath(NpcClickRequest request) {
        if (request.tooltipTemplatePath() == null || request.tooltipTemplatePath().isBlank()) {
            return NPC_TASK_TOOLTIP_TEMPLATE_PATH;
        }
        return request.tooltipTemplatePath();
    }

    /**
     * Central recording boundary for evidence produced by {@link #clickNpcSmart(NpcClickRequest)}.
     *
     * <p>This method is the production learning gate for NPC/monster clicking. Individual strategies
     * return evidence instead of writing vision memory themselves, so all production smart-click
     * learning uses one auditable boundary.</p>
     *
     * <p>All points saved here are converted to window-relative coordinates before becoming reusable
     * learning data. Strong recommendations are only built from verified results; unverified attempts
     * are stored as diagnostics and should not become learned click points.</p>
     *
     * @param request smart-click target facts supplied by the task.
     * @param result strategy evidence produced inside {@code clickNpcSmart}; null or not-attempted
     *               results are ignored.
     */
    private void recordSmartClickEvidence(NpcClickRequest request,
                                          NpcClickStrategyResult result,
                                          LocationInfo playerLocation) {
        if (request == null || result == null || !result.attempted()) {
            return;
        }
        if (!hasKnownTargetCoordinate(request)) {
            log.info("[vision-memory] smart-click evidence skipped because target coordinate is unknown: source={} npc={} target=({}, {}) message={}",
                    result.source(), request.npcName(), request.mapX(), request.mapY(), result.message());
            return;
        }
        if (playerLocation == null) {
            log.warn("[vision-memory] smart-click evidence skipped because player coordinate is missing: source={} npc={} target=({}, {}) message={}",
                    result.source(), request.npcName(), request.mapX(), request.mapY(), result.message());
            return;
        }
        WindowBase windowBase = currentWindowBase("smart-click-evidence");
        boolean runnerOwnsDialogVerification = hasExpectedDialogTemplate(request);
        /*
         * Click samples are the source used by learned direct-click recommendations. Keep this
         * stream strict: only a strategy that actually sent a click and can provide a reusable
         * direct-click point may write here. Pure OCR/template misses, scan failures, exhausted Ctrl
         * probes, and interrupted attempts remain log/ROI diagnostics only; otherwise one miss can
         * become the latest sample and suppress an older verified learned point.
         */
        boolean shouldRecordClickSample =
                result.clicked()
                        && result.clickPointAbs() != null
                        && result.clickPointRel() != null;
        /*
         * Tooltip evidence is not a yellow-name/purple-anchor joint proof. It is a separate visual
         * cue that tells us where the yellow NPC/monster-name work region should be.
         */
        boolean tooltipRoiEvidence =
                result.source() == NpcClickStrategySource.TASK_TOOLTIP_TEMPLATE
                        && result.scanRegion() != null
                        && result.matchedRect() != null
                        && result.matched()
                        && result.verified();
        boolean jointVisualRoiEvidence =
                result.source() != NpcClickStrategySource.CTRL_MENU
                        && result.scanRegion() != null
                        && result.roiAnchorMatched()
                        && result.matched()
                        && (result.matchedRect() != null
                        || result.clickPointRel() != null
                        || result.source() == NpcClickStrategySource.YELLOW_TARGET_OCR);
        boolean shouldRecordRoiEvidence = tooltipRoiEvidence || jointVisualRoiEvidence;
        log.info("[vision-memory] smart-click evidence gate: source={} status={} npc={} target=({}, {}) "
                        + "clicked={} verified={} runnerOwned={} clickSample={} roiEvidence={} jointAnchor={} tooltipRoi={} clickPointRel={} scanRegion={} message={}",
                result.source(), result.status(), request.npcName(), request.mapX(), request.mapY(),
                result.clicked(), result.verified(), runnerOwnsDialogVerification, shouldRecordClickSample,
                shouldRecordRoiEvidence,
                result.roiAnchorMatched(),
                tooltipRoiEvidence,
                result.clickPointRel(),
                result.scanRegion() == null ? "-" : result.scanRegion().toShortText(),
                result.message());
        if (shouldRecordClickSample) {
            PendingSmartClickEvidence pending = PendingSmartClickEvidence.from(
                    request, result, playerLocation, new Point(windowBase.x(), windowBase.y()));
            if (runnerOwnsDialogVerification) {
                if (result.verified()) {
                    recordConfirmedSmartClickEvidence(pending, true,
                            "DIALOG_TEMPLATE", "inline expected option proof");
                } else {
                    String key = currentPendingEvidenceKey();
                    pendingSmartClickEvidence.put(key, pending);
                    publishPendingSmartClickProofToken(pending.proofToken);
                    log.info("[vision-memory] smart-click evidence pending runner confirmation: key={} source={} npc={} target=({}, {}) clickRel={} message={}",
                            key, result.source(), request.npcName(), request.mapX(), request.mapY(),
                            result.clickPointRel(), result.message());
                }
            } else {
                recordConfirmedSmartClickEvidence(pending, result.verified(),
                        result.verified() ? "DIALOG_TEMPLATE" : "NONE",
                        "immediate verifier");
            }
        }

        /*
         * ROI policy learning is about where scene-level visual cues live. Do not feed Ctrl-menu
         * text into this stream because its rectangle belongs to the popup menu, not the in-scene
         * NPC/monster label. Yellow OCR misses are still useful here: repeated misses can mark the
         * current ROI stale without poisoning learned direct-click samples above.
         */
        if (shouldRecordRoiEvidence) {
            try {
                boolean roiVerified = tooltipRoiEvidence
                        || (runnerOwnsDialogVerification
                        ? result.matched() && result.roiAnchorMatched()
                        : result.verified());
                String evidencePrefix = tooltipRoiEvidence
                        ? "tooltip-derived yellow ROI verified; "
                        : (runnerOwnsDialogVerification ? "joint visual ROI verified; " : "");
                ocrRoiMemoryService.recordNpcTargetOcrObservation(
                        result.source().memorySource(),
                        request.mapName(),
                        request.mapX(),
                        request.mapY(),
                        playerLocation.x,
                        playerLocation.y,
                        request.npcName(),
                        request.roamingTarget(),
                        result.scanRegion(),
                        result.matchedRect(),
                        result.clickPointRel(),
                        result.matched(),
                        roiVerified,
                        result.source().memorySource(),
                        evidencePrefix + result.message());
            } catch (Exception e) {
                log.warn("[vision-memory] record smart NPC ROI evidence failed: source={} npc={} region={} reason={}",
                        result.source(), request.npcName(), result.scanRegion().toShortText(), e.getMessage(), e);
            }
        }
    }

    private static boolean hasKnownTargetCoordinate(NpcClickRequest request) {
        return request != null && request.mapX() >= 0 && request.mapY() >= 0;
    }

    private static boolean hasExpectedDialogTemplate(NpcClickRequest request) {
        if (request == null) {
            return false;
        }
        if (request.expectedDialogTemplatePath() != null && !request.expectedDialogTemplatePath().isBlank()) {
            return true;
        }
        return request.expectedDialogTemplatePaths() != null
                && request.expectedDialogTemplatePaths().stream().anyMatch(path -> path != null && !path.isBlank());
    }

    /**
     * Convert a screen-absolute click point to a 1024x768 window-relative point.
     *
     * @param screenPoint screen-absolute point; nullable.
     * @param windowBase screen-absolute game-window origin.
     * @return window-relative point, or null when no screen point exists.
     */
    private Point windowRelativePoint(Point screenPoint, WindowBase windowBase) {
        if (screenPoint == null || windowBase == null) {
            return null;
        }
        return new Point(screenPoint.x - windowBase.x(), screenPoint.y - windowBase.y());
    }

    /**
     * Convert an NPC tooltip template center into the reusable direct-click point for the NPC body.
     *
     * <p>The tooltip itself is a hover/clickable UI cue above the actor. The learning memory should
     * store a point that can later be left-clicked directly on the NPC/monster, not merely the
     * tooltip center used by this fast path. Current Xiuluo samples show the body click point is
     * approximately 90 pixels below the tooltip center.</p>
     *
     * @param tooltipCenterAbs screen-absolute center of {@code npc_task_tooltip.png}.
     * @return screen-absolute direct NPC click point to store in vision memory.
     */
    static Point directNpcPointFromTooltipCenter(Point tooltipCenterAbs) {
        if (tooltipCenterAbs == null) {
            return null;
        }
        return new Point(tooltipCenterAbs.x, tooltipCenterAbs.y + 90);
    }

    /**
     * Convert an NPC tooltip template center into the yellow-name work ROI for future OCR/template
     * scans. This region is intentionally about the NPC/monster yellow label area only; it does not
     * assert that the current player's purple name anchor is inside the same rectangle.
     *
     * @param tooltipCenterAbs screen-absolute center of the matched tooltip template.
     * @param windowBaseAbs screen-absolute game-window origin.
     * @return window-relative yellow-name ROI, clamped to the 1024x768 game window.
     */
    static OcrWindowRegion tooltipLearnedRoiFromTooltipCenter(Point tooltipCenterAbs, Point windowBaseAbs) {
        if (tooltipCenterAbs == null || windowBaseAbs == null) {
            return null;
        }
        OcrWindowRegion region = new OcrWindowRegion(
                tooltipCenterAbs.x - windowBaseAbs.x - 150,
                tooltipCenterAbs.y - windowBaseAbs.y - 100,
                tooltipCenterAbs.x - windowBaseAbs.x + 150,
                tooltipCenterAbs.y - windowBaseAbs.y + 200)
                .clamp(WINDOW_WIDTH, WINDOW_HEIGHT);
        return region.isValid() ? region : null;
    }

    /**
     * Convert a screen-absolute rectangle into a clamped window-relative OCR region.
     *
     * @param rect screen-absolute rectangle in [left, top, right, bottom] form.
     * @param windowBase screen-absolute game-window origin.
     * @return valid window-relative region, or null when the input rectangle is invalid.
     */
    private OcrWindowRegion screenRectToWindowRegion(int[] rect, WindowBase windowBase) {
        if (rect == null || rect.length < 4 || windowBase == null) {
            return null;
        }
        OcrWindowRegion region = new OcrWindowRegion(
                rect[0] - windowBase.x(),
                rect[1] - windowBase.y(),
                rect[2] - windowBase.x(),
                rect[3] - windowBase.y())
                .clamp(WINDOW_WIDTH, WINDOW_HEIGHT);
        return region.isValid() ? region : null;
    }

    /**
     * Mutable state shared by one {@link #runNpcClickPipeline(NpcClickRequest, NpcClickVerifier, String)} call.
     *
     * <p>The strategy methods mutate only Ctrl probe origins and the story-prepared flag. Keeping
     * those fields in one object makes the pipeline order explicit without changing click behavior.</p>
     */
    private static class NpcClickPipelineState {
        final LocationInfo playerLocation;
        final List<ResolvedNpcClickRegion> targetScanRegions;
        final boolean skipDefaultOcrMask;
        final List<CtrlProbeOrigin> ctrlProbeOrigins = new ArrayList<>();
        Point ctrlProbeReferenceAbs;
        boolean storyPreparedForDirectSceneClick;

        NpcClickPipelineState(LocationInfo playerLocation,
                              List<ResolvedNpcClickRegion> targetScanRegions,
                              boolean skipDefaultOcrMask) {
            this.playerLocation = playerLocation;
            this.targetScanRegions = targetScanRegions == null ? List.of() : targetScanRegions;
            this.skipDefaultOcrMask = skipDefaultOcrMask;
        }
    }

    /**
     * Strategy source for {@link NpcClickStrategyResult}.
     *
     * <p>The enum values are stable diagnostic/JSON source names. They describe which strategy
     * produced the evidence, not which task requested the click.</p>
     */
    private enum NpcClickStrategySource {
        TASK_TOOLTIP_TEMPLATE("NPC_TASK_TOOLTIP_TEMPLATE", true),
        LEARNED_MEMORY("NPC_LEARNED_MEMORY", false),
        YELLOW_TARGET_OCR("NPC_YELLOW_TARGET", true),
        PLAYER_ANCHOR_FORMULA("NPC_PLAYER_ANCHOR_FORMULA", false),
        CTRL_MENU("NPC_CTRL_MENU", true);

        private final String memorySource;
        private final boolean actualClickMeasured;

        NpcClickStrategySource(String memorySource, boolean actualClickMeasured) {
            this.memorySource = memorySource;
            this.actualClickMeasured = actualClickMeasured;
        }

        String memorySource() {
            return memorySource;
        }

        boolean actualClickMeasured() {
            return actualClickMeasured;
        }
    }

    /**
     * Normalized outcome status for one smart-click strategy attempt.
     */
    private enum NpcClickStrategyStatus {
        SKIPPED,
        NOT_FOUND,
        CLICK_NOT_VERIFIED,
        VERIFIED,
        FAILED
    }

    /**
     * Structured evidence produced by one {@link #clickNpcSmart(NpcClickRequest)} strategy.
     *
     * @param source strategy that produced this result.
     * @param status normalized outcome.
     * @param scanRegion window-relative scan region used by the strategy, nullable.
     * @param matchedRect window-relative visual cue rectangle, nullable.
     * @param clickPointAbs screen-absolute reusable direct-click point to learn from, nullable. This
     *                      is not always the exact UI point clicked by the strategy: tooltip clicks
     *                      store the NPC-body point below the tooltip, and Ctrl clicks store the
     *                      original hover point that opened the Ctrl menu.
     * @param clickPointRel window-relative reusable direct-click point to learn from, nullable.
     * @param matched true when the strategy matched its expected visual cue.
     * @param clicked true when physical input was sent.
     * @param verified true when the expected dialog/battle verification succeeded.
     * @param message concise diagnostic text for logs and vision memory.
     */
    @Value

    @Builder

    @AllArgsConstructor(access = AccessLevel.PUBLIC)

    @Accessors(fluent = true)

    private static class NpcClickStrategyResult {

        NpcClickStrategySource source;

        NpcClickStrategyStatus status;

        OcrWindowRegion scanRegion;

        OcrWindowRegion matchedRect;

        Point clickPointAbs;

        Point clickPointRel;

        boolean matched;

        boolean clicked;

        boolean verified;

        boolean roiAnchorMatched;

        String message;

        static NpcClickStrategyResult skipped(NpcClickStrategySource source, String message) {
            return new NpcClickStrategyResult(source, NpcClickStrategyStatus.SKIPPED,
                    null, null, null, null, false, false, false, false, message);
        }

        static NpcClickStrategyResult notFound(NpcClickStrategySource source, String message) {
            return new NpcClickStrategyResult(source, NpcClickStrategyStatus.NOT_FOUND,
                    null, null, null, null, false, false, false, false, message);
        }

        static NpcClickStrategyResult notFound(NpcClickStrategySource source,
                                               OcrWindowRegion scanRegion,
                                               String message) {
            return new NpcClickStrategyResult(source, NpcClickStrategyStatus.NOT_FOUND,
                    scanRegion, null, null, null, false, false, false, false, message);
        }

        static NpcClickStrategyResult failed(NpcClickStrategySource source, String message) {
            return new NpcClickStrategyResult(source, NpcClickStrategyStatus.FAILED,
                    null, null, null, null, false, false, false, false, message);
        }

        static NpcClickStrategyResult failed(NpcClickStrategySource source,
                                             OcrWindowRegion scanRegion,
                                             String message) {
            return new NpcClickStrategyResult(source, NpcClickStrategyStatus.FAILED,
                    scanRegion, null, null, null, false, false, false, false, message);
        }

        static NpcClickStrategyResult fromClick(NpcClickStrategySource source,
                                                NpcClickStrategyStatus status,
                                                OcrWindowRegion scanRegion,
                                                OcrWindowRegion matchedRect,
                                                Point clickPointAbs,
                                                Point clickPointRel,
                                                boolean clicked,
                                                boolean verified,
                                                String message) {
            return fromClick(source, status, scanRegion, matchedRect, clickPointAbs, clickPointRel,
                    clicked, verified, false, message);
        }

        static NpcClickStrategyResult fromClick(NpcClickStrategySource source,
                                                NpcClickStrategyStatus status,
                                                OcrWindowRegion scanRegion,
                                                OcrWindowRegion matchedRect,
                                                Point clickPointAbs,
                                                Point clickPointRel,
                                                boolean clicked,
                                                boolean verified,
                                                boolean roiAnchorMatched,
                                                String message) {
            return new NpcClickStrategyResult(source, status, scanRegion, matchedRect,
                    clickPointAbs, clickPointRel, true, clicked, verified, roiAnchorMatched, message);
        }

        boolean attempted() {
            return status != NpcClickStrategyStatus.SKIPPED;
        }
    

    }

    /**
     * Read the task-maintained player coordinate without triggering another minimap OCR pass.
     *
     * <p>Navigation/hot-start flows are responsible for refreshing {@link PlayerCharacter#getX()}
     * and {@link PlayerCharacter#getY()} through {@link PlayerStateService#syncMyPosition()} before
     * smart-click runs. This method deliberately treats an all-zero coordinate as unavailable so
     * vision memory does not learn or reuse samples anchored to the default PlayerCharacter state.</p>
     */
    private LocationInfo cachedPlayerLocation(NpcClickRequest request) {
        PlayerCharacter player = request == null ? null : request.player();
        if (player == null) {
            return null;
        }
        int x = player.getX();
        int y = player.getY();
        if (x == 0 && y == 0) {
            return null;
        }
        String mapName = player.getCurrentMapName();
        if (mapName == null || mapName.isBlank()) {
            mapName = request.mapName();
        }
        return new LocationInfo(mapName, x, y);
    }

    private List<ResolvedNpcClickRegion> resolveNpcScanRegions(NpcClickRequest request, LocationInfo playerLocation) {
        return ocrRoiMemoryService.recommendNpcClickRegions(
                request.mapName(),
                request.mapX(),
                request.mapY(),
                playerLocation == null ? null : playerLocation.x,
                playerLocation == null ? null : playerLocation.y,
                request.npcName(),
                request.roamingTarget());
    }

    private String summarizeRegions(List<ResolvedNpcClickRegion> regions) {
        if (regions == null || regions.isEmpty()) {
            return "-";
        }
        return regions.stream().map(ResolvedNpcClickRegion::toShortText).toList().toString();
    }

    private void addCtrlProbeOrigin(List<CtrlProbeOrigin> origins,
                                    Point point,
                                    String source,
                                    CtrlProbeScanProfile profile) {
        if (origins == null || point == null) {
            return;
        }
        CtrlProbeScanProfile safeProfile = profile == null ? CtrlProbeScanProfile.SMALL_RING : profile;
        origins.add(new CtrlProbeOrigin(new Point(point), source, safeProfile));
        log.info("NPC ctrl probe origin candidate: source={} profile={} point=({}, {})",
                source, safeProfile, point.x, point.y);
    }

    /**
     * Append ranked visual fallback candidates to the Ctrl-probe origin list without clicking them.
     *
     * <p>Yellow text candidates are only shape-ranked evidence. They are useful starting points for
     * the later Ctrl-menu probe, but they are not strong enough to left-click directly when target
     * OCR failed. Points are screen-absolute and remain ordered by the extractor score.</p>
     *
     * @param origins mutable screen-absolute Ctrl origin list owned by {@link #clickNpcSmart(NpcClickRequest)}.
     * @param points sorted screen-absolute candidate click points; null/empty is ignored.
     * @param source diagnostic source label written to logs.
     * @param profile scan strength applied to every appended point.
     */
    private void addCtrlProbeOrigins(List<CtrlProbeOrigin> origins,
                                     List<Point> points,
                                     String source,
                                     CtrlProbeScanProfile profile) {
        if (points == null || points.isEmpty()) {
            return;
        }
        for (int i = 0; i < points.size(); i++) {
            addCtrlProbeOrigin(origins, points.get(i), source + "#" + (i + 1), profile);
        }
    }

    /**
     * Normalize Ctrl probe origins to an ordered, de-duplicated, window-clamped list.
     *
     * @param preferredOrigins screen-absolute points produced by earlier click strategies.
     * @param centerFallback screen-absolute fallback point, usually the game-window center.
     * @param windowBase screen-absolute top-left of the bound game window.
     * @param includeWindowCenterFallback true for production smart-click fallback, false for
     *                                    point-isolation debug runs where the caller wants to test
     *                                    only the supplied origins.
     * @return ordered screen-absolute origins. Caller-provided origins are kept first because they
     *         can encode task-specific offsets, such as a 修罗 approach point that deliberately
     *         stands beside the monster. The center fallback is only kept for explicit debug calls.
     */
    private List<CtrlProbeOrigin> normalizeCtrlProbeOrigins(List<CtrlProbeOrigin> preferredOrigins,
                                                            Point centerFallback,
                                                            WindowBase windowBase,
                                                            boolean includeWindowCenterFallback) {
        List<CtrlProbeOrigin> normalized = new ArrayList<>();
        if (preferredOrigins != null) {
            for (CtrlProbeOrigin origin : preferredOrigins) {
                addNormalizedCtrlProbeOrigin(normalized, origin, windowBase);
            }
        }
        if (includeWindowCenterFallback) {
            addNormalizedCtrlProbeOrigin(normalized,
                    new CtrlProbeOrigin(centerFallback, "window-center", CtrlProbeScanProfile.FULL_RING), windowBase);
        }
        return List.copyOf(normalized);
    }

    private void addNormalizedCtrlProbeOrigin(List<CtrlProbeOrigin> normalized,
                                              CtrlProbeOrigin origin,
                                              WindowBase windowBase) {
        if (origin == null || origin.point() == null || windowBase == null) {
            return;
        }
        Point safe = new Point(
                clamp(origin.point().x, windowBase.x(), windowBase.x() + WINDOW_WIDTH - 1),
                clamp(origin.point().y, windowBase.y(), windowBase.y() + WINDOW_HEIGHT - 1));
        for (CtrlProbeOrigin existing : normalized) {
            if (Math.abs(existing.point().x - safe.x) <= 3 && Math.abs(existing.point().y - safe.y) <= 3) {
                return;
            }
        }
        normalized.add(new CtrlProbeOrigin(safe, origin.source(), origin.profile()));
    }

    private String summarizeCtrlProbeOrigins(List<CtrlProbeOrigin> origins) {
        if (origins == null || origins.isEmpty()) {
            return "[]";
        }
        return origins.stream()
                .map(origin -> origin.source() + ":" + origin.profile()
                        + "@(" + origin.point().x + "," + origin.point().y + ")")
                .toList()
                .toString();
    }

    /**
     * Scan strength for a Ctrl fallback origin.
     *
     * <p>The screenshot around each probe now covers all menu quadrants, so not every origin needs
     * a dense mouse-offset search. Stable generic origins can afford a wider ring, while noisy
     * shape-only candidates should only test their own point.</p>
     */
    private enum CtrlProbeScanProfile {
        DIRECT(CTRL_OFFSETS_DIRECT),
        SMALL_RING(CTRL_OFFSETS_SMALL_RING),
        FULL_RING(CTRL_OFFSETS_FULL_RING);

        private final int[][] offsets;

        CtrlProbeScanProfile(int[][] offsets) {
            this.offsets = offsets;
        }

        private int[][] offsets() {
            return offsets;
        }
    }

    /**
     * Screen-absolute Ctrl probe origin plus diagnostic source and scan strength.
     *
     * @param point screen-absolute cursor point to hold Ctrl on.
     * @param source human-readable source such as window-center, learned-memory, formula-target, or
     *               yellow-candidate.
     * @param profile offset profile used around this origin.
     */
    @Value

    @Builder

    @AllArgsConstructor(access = AccessLevel.PUBLIC)

    @Accessors(fluent = true)

    private static class CtrlProbeOrigin {

        Point point;

        String source;

        CtrlProbeScanProfile profile;

    }

    /**
     * Try a previously verified direct click point for this exact NPC target.
     *
     * <p>The learned point is window-relative and comes from {@link OcrRoiMemoryService}, which only
     * returns conservative clusters of successful samples. This method still converts through the
     * current bound window base, performs one serialized move+click, and requires the expected dialog
     * template to verify. A miss simply falls through to OCR/formula/Ctrl strategies.</p>
     *
     * @param mapName target NPC map name used to look up the memory key.
     * @param mapX target NPC logical X coordinate.
     * @param mapY target NPC logical Y coordinate.
     * @param npcName target NPC name.
     * @param expectedDialogTemplatePath green-option template that confirms the click target.
     * @return structured evidence with the learned point and verification status.
     */
    private NpcClickStrategyResult clickNpcByLearnedMemory(String mapName,
                                                           int mapX,
                                                           int mapY,
                                                           String npcName,
                                                           LocationInfo playerLocation,
                                                           NpcClickVerifier verifier) {
        if (shouldStop()) {
            return NpcClickStrategyResult.failed(NpcClickStrategySource.LEARNED_MEMORY, "interrupted-before-learned-click");
        }
        Optional<LearnedNpcClickPoint> learned =
                ocrRoiMemoryService.recommendedNpcClickPoint(
                        mapName,
                        npcName,
                        mapX,
                        mapY,
                        playerLocation == null ? null : playerLocation.x,
                        playerLocation == null ? null : playerLocation.y);
        if (learned.isEmpty()) {
            return NpcClickStrategyResult.skipped(NpcClickStrategySource.LEARNED_MEMORY, "no-learned-point");
        }

        LearnedNpcClickPoint point = learned.get();
        WindowBase windowBase = currentWindowBase("learned-npc-click");
        int clickX = windowBase.x() + point.x();
        int clickY = windowBase.y() + point.y();
        log.info("NPC learned click candidate: npcName={} {}", npcName, point.toSummaryText());
        boolean verified = executeMoveClickAndVerify("npcClick:learnedMemoryMoveClick",
                clickX, clickY, 1200, 0, verifier);
        if (!verified) {
            log.info("NPC learned click missed; fallback to OCR/formula strategies: npcName={} key={}",
                    npcName, point.key());
        }
        Point clickPointAbs = new Point(clickX, clickY);
        return NpcClickStrategyResult.fromClick(
                NpcClickStrategySource.LEARNED_MEMORY,
                verified ? NpcClickStrategyStatus.VERIFIED : NpcClickStrategyStatus.CLICK_NOT_VERIFIED,
                null,
                null,
                clickPointAbs,
                windowRelativePoint(clickPointAbs, windowBase),
                true,
                verified,
                verified ? "learned memory verified: " + point.toSummaryText()
                        : "learned memory click not verified: " + point.toSummaryText());
    }

    /**
     * Click an NPC by recognizing yellow target-name text in a caller-supplied scan region.
     *
     * <p>The method captures the window-relative region after pressing Alt+4, masks/segments yellow
     * text with {@link GameTextLineOcrService}, clicks below the matched text center, and verifies
     * the expected dialog template. The output debug image is window-scoped.</p>
     *
     * @param npcName target NPC name or expected fragment.
     * @param scanRegion resolved visual work region with window-relative and screen-absolute bounds.
     * @param expectedDialogTemplatePath green-option template that confirms success.
     * @param skipDefaultOcrMask true only for Alt+A direct-combat mode, where the game has already
     *                           hidden HUD/panels and the edge monster may live inside the normal
     *                           masked area.
     * @return detailed result. Only {@link YellowTargetClickStatus#TARGET_NOT_FOUND} permits the
     * next larger region to be searched; other failures mean the target path was attempted and
     * should fall through to coordinate/Ctrl strategies.
     */
    private YellowTargetClickResult clickNpcByYellowTargetName(
            NpcClickRequest request,
            ResolvedNpcClickRegion scanRegion,
            NpcClickVerifier verifier,
            boolean skipDefaultOcrMask) {
        if (shouldStop()) return YellowTargetClickResult.scanFailed();
        OcrWindowRegion targetScanRegion = scanRegion == null ? null : scanRegion.windowRegion();
        String outputPath = windowScopedTempPath.resolve("npc_yellow_target.png");
        String npcName = request.npcName();
        if (npcName == null || npcName.isBlank()) {
            log.info("NPC yellow target scan skipped: npcName is blank");
            return YellowTargetClickResult.scanFailed();
        }

        BufferedImage raw = captureCleanNameRegionToMemory("NPC yellow target scan", scanRegion, false);
        if (raw == null) {
            log.warn("NPC yellow target scan capture failed: npcName={} region={}",
                    npcName, targetScanRegion.toShortText());
            return YellowTargetClickResult.scanFailed();
        }
        BufferedImage scanImage = prepareNpcOcrScanImage(
                raw, targetScanRegion, "yellow target", skipDefaultOcrMask);
        if (scanImage == null) {
            raw.flush();
            return YellowTargetClickResult.scanFailed();
        }
        try {
                /*
                 * Primary NPC evidence is the yellow target name itself. Even on the broad masked
                 * fallback region, do the exact target-name OCR first so clickNpcSmart keeps its
                 * original strategy order: yellow name -> player-anchor formula -> Ctrl menu.
                 * Shape-only candidates are only evidence for Ctrl probing after exact yellow matching
                 * fails; they must not replace the direct yellow-name click path.
                 */
                TargetOcrResult result =
                        gameTextLineOcrService.findYellowTarget(scanImage, npcName, Path.of(outputPath));
                log.info("NPC yellow target scan result: npcName={} region={} detail={}",
                        npcName, targetScanRegion.toShortText(), result.toDetailText());
                if (!result.hit() || result.lineResult() == null || result.lineResult().words().isEmpty()) {
                    List<YellowTextCandidate> fallbackCandidates =
                            findYellowTextFallbackCandidates(scanImage, targetScanRegion, npcName);
                    return fallbackCandidates.isEmpty()
                            ? YellowTargetClickResult.targetNotFound(targetScanRegion, result.normalizedText())
                            : YellowTargetClickResult.targetNotFoundWithCandidates(
                                    targetScanRegion, fallbackCandidates, result.normalizedText());
                }
                if (!isStrongYellowTargetHit(result)) {
                    List<YellowTextCandidate> fallbackCandidates =
                            findYellowTextFallbackCandidates(scanImage, targetScanRegion, npcName);
                    log.info("NPC yellow target rejected by strict direct-click rule: npcName={} normalizedText={} "
                                    + "target={} common={} distance={} fallbackCandidates={}",
                            npcName, result.normalizedText(), result.normalizedTarget(),
                            result.longestCommonSubstring(), result.editDistance(), fallbackCandidates.size());
                    return fallbackCandidates.isEmpty()
                            ? YellowTargetClickResult.targetNotFound(targetScanRegion, result.normalizedText())
                            : YellowTargetClickResult.targetNotFoundWithCandidates(
                                    targetScanRegion, fallbackCandidates, result.normalizedText());
                }
                List<OcrWordResult> targetWords = selectYellowTargetWords(
                        result.lineResult().words(), result.normalizedTarget());
                Point targetInScan = centerOfWords(targetWords);
                if (targetInScan == null) {
                    return YellowTargetClickResult.scanFailed(
                            targetScanRegion, "yellow target center unavailable text=" + result.normalizedText());
                }
                OcrWindowRegion textRect = windowRegionOfWords(targetWords, targetScanRegion);
                WindowBase windowBase = currentWindowBase("yellow-target-click");
                int clickX = windowBase.x() + targetScanRegion.x1() + targetInScan.x;
                int clickY = windowBase.y() + targetScanRegion.y1() + targetInScan.y - 50;
                Point textCenterAbs = new Point(
                        windowBase.x() + targetScanRegion.x1() + targetInScan.x,
                        windowBase.y() + targetScanRegion.y1() + targetInScan.y);
                Point clickPointRel = new Point(clickX - windowBase.x(), clickY - windowBase.y());
                log.info("NPC yellow target matched: npcName={} click=({}, {}) targetInScan=({}, {}) targetWords={}",
                        npcName, clickX, clickY, targetInScan.x, targetInScan.y, summarizeWords(targetWords));
                JointAnchorResult jointAnchor = findPlayerAnchorInYellowTargetRegion(
                        scanImage, request, scanRegion, targetScanRegion);
                OcrWindowRegion learnableTextRect = jointAnchor.matched()
                        ? unionRegions(textRect, jointAnchor.anchorRect())
                        : textRect;
                /*
                 * We already have a concrete yellow-name candidate here. A short same-point retry is
                 * cheaper and safer than widening the scan region, because the first miss can simply be
                 * a delayed dialog/network response while a wider region may introduce another NPC.
                 */
                boolean verified = executeMoveClickAndVerify("npcClick:yellowTargetMoveClick",
                        clickX, clickY, 800, YELLOW_TARGET_CLICK_RETRIES, verifier);
                return new YellowTargetClickResult(
                        verified ? YellowTargetClickStatus.CLICK_VERIFIED : YellowTargetClickStatus.CLICK_NOT_VERIFIED,
                        new Point(clickX, clickY),
                        List.of(),
                        verified ? List.of() : List.of(textCenterAbs),
                        NpcClickStrategyResult.fromClick(
                                NpcClickStrategySource.YELLOW_TARGET_OCR,
                                verified ? NpcClickStrategyStatus.VERIFIED : NpcClickStrategyStatus.CLICK_NOT_VERIFIED,
                                targetScanRegion,
                                learnableTextRect,
                                new Point(clickX, clickY),
                                clickPointRel,
                                true,
                                verified,
                                jointAnchor.matched(),
                                verified ? "yellow target verified text=" + result.normalizedText()
                                        : "yellow target click not verified text=" + result.normalizedText()));
            } catch (Exception e) {
                log.warn("NPC yellow target scan failed: npcName={} region={} reason={}",
                        npcName, targetScanRegion.toShortText(), e.getMessage(), e);
                return YellowTargetClickResult.scanFailed(targetScanRegion, "yellow target exception: " + e.getMessage());
        } finally {
            if (scanImage != raw) {
                scanImage.flush();
            }
            raw.flush();
        }
    }

    private int strictYellowTargetMinCommon(String normalizedTarget) {
        if (STRICT_YELLOW_TARGET_JIANGMO_SHIWEI.equals(normalizedTarget)) {
            return STRICT_YELLOW_TARGET_JIANGMO_SHIWEI_MIN_COMMON;
        }
        return 0;
    }

    private static class PendingSmartClickEvidence {
        final NpcClickStrategySource source;
        final String mapName;
        final Integer playerMapX;
        final Integer playerMapY;
        final String npcName;
        final int mapX;
        final int mapY;
        final Point windowBaseAbs;
        final Point clickPointAbs;
        final int tuneX;
        final int tuneY;
        final List<String> expectedDialogTemplatePaths;
        final String proofToken;
        final String message;

        private PendingSmartClickEvidence(NpcClickStrategySource source,
                                          String mapName,
                                          Integer playerMapX,
                                          Integer playerMapY,
                                          String npcName,
                                          int mapX,
                                          int mapY,
                                          Point windowBaseAbs,
                                          Point clickPointAbs,
                                          int tuneX,
                                          int tuneY,
                                          List<String> expectedDialogTemplatePaths,
                                          String proofToken,
                                          String message) {
            this.source = source;
            this.mapName = mapName;
            this.playerMapX = playerMapX;
            this.playerMapY = playerMapY;
            this.npcName = npcName;
            this.mapX = mapX;
            this.mapY = mapY;
            this.windowBaseAbs = windowBaseAbs == null ? null : new Point(windowBaseAbs);
            this.clickPointAbs = clickPointAbs == null ? null : new Point(clickPointAbs);
            this.tuneX = tuneX;
            this.tuneY = tuneY;
            this.expectedDialogTemplatePaths = expectedDialogTemplatePaths == null
                    ? List.of()
                    : expectedDialogTemplatePaths.stream()
                    .filter(path -> path != null && !path.isBlank())
                    .toList();
            this.proofToken = proofToken;
            this.message = message;
        }

        static PendingSmartClickEvidence from(NpcClickRequest request,
                                              NpcClickStrategyResult result,
                                              LocationInfo playerLocation,
                                              Point windowBaseAbs) {
            return new PendingSmartClickEvidence(
                    result.source(),
                    request.mapName(),
                    playerLocation == null ? null : playerLocation.x,
                    playerLocation == null ? null : playerLocation.y,
                    request.npcName(),
                    request.mapX(),
                    request.mapY(),
                    windowBaseAbs,
                    result.clickPointAbs(),
                    request.tuneX(),
                    request.tuneY(),
                    expectedDialogTemplatePaths(request),
                    UUID.randomUUID().toString(),
                    result.message());
        }

        private static List<String> expectedDialogTemplatePaths(NpcClickRequest request) {
            if (request == null) {
                return List.of();
            }
            if (request.expectedDialogTemplatePaths() != null
                    && request.expectedDialogTemplatePaths().stream().anyMatch(path -> path != null && !path.isBlank())) {
                return request.expectedDialogTemplatePaths();
            }
            if (request.expectedDialogTemplatePath() == null || request.expectedDialogTemplatePath().isBlank()) {
                return List.of();
            }
            return List.of(request.expectedDialogTemplatePath());
        }

        boolean matches(String expectedMapName, String expectedNpcName, int expectedMapX, int expectedMapY) {
            return Objects.equals(normalizeNullable(mapName), normalizeNullable(expectedMapName))
                    && Objects.equals(normalizeNullable(npcName), normalizeNullable(expectedNpcName))
                    && mapX == expectedMapX
                    && mapY == expectedMapY;
        }

        boolean matchesExpectedOptionProof(String actionKey, String matchedText) {
            if (expectedDialogTemplatePaths.isEmpty()) {
                return matchedText != null && !matchedText.isBlank()
                        || actionKey != null && !actionKey.isBlank();
            }
            return expectedDialogTemplatePaths.stream()
                    .anyMatch(path -> Objects.equals(normalizeNullable(path), normalizeNullable(matchedText)));
        }

        boolean matchesProofToken(String candidateProofToken) {
            return proofToken != null
                    && !proofToken.isBlank()
                    && Objects.equals(proofToken, normalizeNullable(candidateProofToken));
        }

        private static String normalizeNullable(String value) {
            return value == null ? null : value.trim();
        }
    }

    /**
     * Verify that a yellow-target ROI also contains the current player's purple name anchor.
     *
     * <p>NPC ROI memory is reused as a visual work region by both yellow-name OCR and the
     * player-anchor formula. A yellow-only crop is therefore not valid learned ROI: it may contain
     * the NPC name while excluding the player name, which makes the later formula anchor to the
     * wrong purple text. This check uses the exact same pre-click screenshot region as yellow OCR
     * and only marks the strategy result learnable when the player's own name is found there too.</p>
     *
     * @param scanImage region-local screenshot already used for yellow target OCR.
     * @param request current NPC click request; supplies the player identity.
     * @param scanRegion resolved region with screen-absolute origin for OCR word conversion.
     * @param targetScanRegion window-relative region, used for diagnostics.
     * @return true when the current player's purple name anchor is recognized inside the same ROI.
     */
    private JointAnchorResult findPlayerAnchorInYellowTargetRegion(BufferedImage scanImage,
                                                                   NpcClickRequest request,
                                                                   ResolvedNpcClickRegion scanRegion,
                                                                   OcrWindowRegion targetScanRegion) {
        if (scanImage == null || request == null || scanRegion == null
                || request.player() == null
                || request.player().getName() == null
                || request.player().getName().isBlank()) {
            log.info("NPC yellow ROI joint-anchor check skipped: npcName={} reason=missing-player-identity region={}",
                    request == null ? null : request.npcName(),
                    targetScanRegion == null ? "-" : targetScanRegion.toShortText());
            return JointAnchorResult.miss();
        }
        String sourcePath = windowScopedTempPath.resolve("npc_yellow_joint_player_source.png");
        String washedPath = windowScopedTempPath.resolve("npc_yellow_joint_player_washed.png");
        try {
            ImageIO.write(scanImage, "png", Path.of(sourcePath).toFile());
            ImagePreprocessor.washPurpleTextToBlackAndWhite(sourcePath, washedPath);
            AtomicReference<PlayerAnchorMatch> playerAnchorMatchRef = new AtomicReference<>();
            List<OcrWordResult> playerWords = ocr.getAllTextResultsForMatch(
                    washedPath,
                    "npc-yellow-joint-player-anchor:" + request.player().getName(),
                    words -> {
                        PlayerAnchorMatch match = extractPlayerAnchorMatchFromWords(
                                words, request.player(), scanRegion.screenX1(), scanRegion.screenY1());
                        playerAnchorMatchRef.set(match);
                        return match != null;
                    });
            PlayerAnchorMatch match = playerAnchorMatchRef.get();
            if (match == null) {
                match = extractPlayerAnchorMatchFromWords(
                        playerWords, request.player(), scanRegion.screenX1(), scanRegion.screenY1());
            }
            boolean matched = match != null && match.anchor() != null;
            OcrWindowRegion anchorRect = matched
                    ? playerAnchorWindowRegion(match, new WindowBase(scanRegion.windowBaseX(), scanRegion.windowBaseY()))
                    : null;
            log.info("NPC yellow ROI joint-anchor check: npcName={} player={} matched={} anchor={} region={} words={}",
                    request.npcName(), request.player().getName(), matched,
                    matched ? "(" + match.anchor().x + "," + match.anchor().y + ")" : "-",
                    targetScanRegion == null ? "-" : targetScanRegion.toShortText(),
                    summarizeWords(playerWords));
            return matched ? JointAnchorResult.hit(anchorRect) : JointAnchorResult.miss();
        } catch (Exception e) {
            log.warn("NPC yellow ROI joint-anchor check failed: npcName={} region={} reason={}",
                    request.npcName(),
                    targetScanRegion == null ? "-" : targetScanRegion.toShortText(),
                    e.getMessage(), e);
            return JointAnchorResult.miss();
        }
    }

    /**
     * Confirm the latest pending smart-click for the current bound window after the task/runner has
     * consumed the expected dialog action. This is the commit point for learned direct-click memory
     * when dialog verification is owned by the window runner rather than by {@code clickNpcSmart}.
     *
     * @param mapName NPC map name expected by the task; nullable but stronger when present.
     * @param npcName NPC name expected by the task.
     * @param mapX target logical X coordinate.
     * @param mapY target logical Y coordinate.
     * @param verificationStrength dialog proof type such as {@code DIALOG_TEMPLATE}.
     * @param reason diagnostic source for logs.
     */
    public void confirmPendingSmartClick(String mapName,
                                         String npcName,
                                         int mapX,
                                         int mapY,
                                         String verificationStrength,
                                         String reason) {
        String key = currentPendingEvidenceKey();
        PendingSmartClickEvidence pending = pendingSmartClickEvidence.get(key);
        if (pending == null) {
            log.info("[vision-memory] pending smart-click confirmation skipped: key={} npc={} target=({}, {}) reason=no-pending source={}",
                    key, npcName, mapX, mapY, reason);
            return;
        }
        if (!pending.matches(mapName, npcName, mapX, mapY)) {
            removePendingSmartClickEvidence(key, pending.proofToken, "explicit confirmation mismatch");
            log.warn("[vision-memory] pending smart-click confirmation ignored: key={} expectedNpc={} expectedTarget=({}, {}) pendingNpc={} pendingTarget=({}, {}) source={}",
                    key, npcName, mapX, mapY, pending.npcName, pending.mapX, pending.mapY, reason);
            return;
        }
        removePendingSmartClickEvidence(key, pending.proofToken, reason);
        recordConfirmedSmartClickEvidence(pending, true, verificationStrength, reason);
    }

    @Override
    public void confirmExpectedOptionProof(String sourceTask,
                                           String actionKey,
                                           String matchedText,
                                           String proofToken,
                                           String verificationStrength,
                                           String reason) {
        String key = currentPendingEvidenceKey();
        PendingSmartClickEvidence pending = pendingSmartClickEvidence.get(key);
        if (pending == null) {
            return;
        }
        if (!pending.matchesProofToken(proofToken)) {
            log.debug("[vision-memory] pending smart-click expected-option proof ignored: key={} proofToken={} pendingProofToken={} pendingNpc={} source={} reason={}",
                    key, proofToken, pending.proofToken, pending.npcName, sourceTask, reason);
            return;
        }
        if (!pending.matchesExpectedOptionProof(actionKey, matchedText)) {
            removePendingSmartClickEvidence(key, pending.proofToken, "expected option proof mismatch");
            log.debug("[vision-memory] pending smart-click expected-option proof ignored: key={} actionKey={} matchedText={} pendingNpc={} source={} reason={}",
                    key, actionKey, matchedText, pending.npcName, sourceTask, reason);
            return;
        }
        removePendingSmartClickEvidence(key, pending.proofToken, reason);
        recordConfirmedSmartClickEvidence(pending, true, verificationStrength, reason);
    }

    private void publishPendingSmartClickProofToken(String proofToken) {
        windowTaskContextHolder.rawCurrent()
                .ifPresent(context -> context.setPendingSmartClickEvidenceProofToken(proofToken));
    }

    private void removePendingSmartClickEvidence(String key, String proofToken, String reason) {
        pendingSmartClickEvidence.remove(key);
        windowTaskContextHolder.rawCurrent()
                .ifPresent(context -> context.clearPendingSmartClickEvidenceProofToken(proofToken, reason));
    }

    private void recordConfirmedSmartClickEvidence(PendingSmartClickEvidence pending,
                                                   boolean success,
                                                   String verificationStrength,
                                                   String reason) {
        if (pending == null) {
            return;
        }
        try {
            ocrRoiMemoryService.recordNpcClickAttempt(
                    pending.source.memorySource(),
                    pending.mapName,
                    pending.playerMapX,
                    pending.playerMapY,
                    pending.npcName,
                    pending.mapX,
                    pending.mapY,
                    new Point(pending.windowBaseAbs),
                    null,
                    new Point(pending.clickPointAbs),
                    new Point(pending.clickPointAbs),
                    pending.tuneX,
                    pending.tuneY,
                    "npc-smart-click:" + pending.source.memorySource(),
                    true,
                    success,
                    success ? NpcClickStrategyStatus.VERIFIED.name() : NpcClickStrategyStatus.CLICK_NOT_VERIFIED.name(),
                    pending.message + "; confirmedBy=" + reason,
                    success,
                    pending.source.memorySource(),
                    success ? verificationStrength : "NONE");
        } catch (Exception e) {
            log.warn("[vision-memory] record confirmed smart NPC click attempt failed: source={} npc={} target=({}, {}) reason={}",
                    pending.source, pending.npcName, pending.mapX, pending.mapY, e.getMessage(), e);
        }
    }

    private String currentPendingEvidenceKey() {
        return windowTaskContextHolder.rawCurrent()
                .map(WindowRuntimeContext::getWindowId)
                .orElse("global");
    }

    private OcrWindowRegion playerAnchorWindowRegion(PlayerAnchorMatch match, WindowBase windowBase) {
        if (match == null || windowBase == null) {
            return null;
        }
        if (match.textRect() != null) {
            OcrWindowRegion region = new OcrWindowRegion(
                    match.textRect().x1() - windowBase.x(),
                    match.textRect().y1() - windowBase.y(),
                    match.textRect().x2() - windowBase.x(),
                    match.textRect().y2() - windowBase.y())
                    .clamp(WINDOW_WIDTH, WINDOW_HEIGHT);
            if (region.isValid()) {
                return region;
            }
        }
        Point anchor = match.anchor();
        if (anchor == null) {
            return null;
        }
        return new OcrWindowRegion(
                anchor.x - windowBase.x() - 8,
                anchor.y - windowBase.y() - 8,
                anchor.x - windowBase.x() + 9,
                anchor.y - windowBase.y() + 9)
                .clamp(WINDOW_WIDTH, WINDOW_HEIGHT);
    }

    private OcrWindowRegion unionRegions(OcrWindowRegion first, OcrWindowRegion second) {
        if (first == null || !first.isValid()) {
            return second;
        }
        if (second == null || !second.isValid()) {
            return first;
        }
        return new OcrWindowRegion(
                Math.min(first.x1(), second.x1()),
                Math.min(first.y1(), second.y1()),
                Math.max(first.x2(), second.x2()),
                Math.max(first.y2(), second.y2()))
                .clamp(WINDOW_WIDTH, WINDOW_HEIGHT);
    }

    private boolean isStrongYellowTargetHit(TargetOcrResult result) {
        if (result == null || !result.hit()) {
            return false;
        }
        String normalizedTarget = result.normalizedTarget();
        String normalizedText = result.normalizedText();
        int strictMinCommon = strictYellowTargetMinCommon(normalizedTarget);
        if (strictMinCommon <= 0) {
            return true;
        }
        if (normalizedText == null || normalizedText.isBlank()) {
            return false;
        }
        if (normalizedText.equals(normalizedTarget) || normalizedText.contains(normalizedTarget)) {
            return true;
        }
        /*
         * Strict NPCs such as 降魔侍卫 must not be clicked from loose two-character or short-name
         * matches. Allow only an almost-complete contiguous OCR hit, then let dialog verification
         * remain the final success gate after the click.
         */
        int targetLength = normalizedTarget == null ? 0 : normalizedTarget.length();
        int almostFull = Math.max(strictMinCommon, targetLength - 1);
        return targetLength > 0
                && normalizedTarget.contains(normalizedText)
                && normalizedText.length() >= almostFull
                && result.longestCommonSubstring() >= almostFull
                && result.editDistance() <= 1;
    }

    /**
     * Build ranked Ctrl-probe origins from yellow text-like shapes when exact target OCR misses.
     *
     * <p>This method is deliberately non-clicking. It reuses the same masked scan image that the
     * exact yellow OCR path saw, asks {@link GameTextLineOcrService} for immutable score-sorted
     * text candidates, converts image-local points to screen-absolute coordinates, and returns them
     * for the later Ctrl-menu probe. Debug washed/overlay images are window-scoped.</p>
     *
     * @param scanImage preprocessed yellow scan image owned by the caller.
     * @param targetScanRegion window-relative region that produced {@code scanImage}.
     * @param npcName target name used only for diagnostics.
     * @return immutable ranked candidates in screen-absolute coordinates; empty when extraction
     *         fails or no stable yellow text-like shape exists.
     */
    private List<YellowTextCandidate> findYellowTextFallbackCandidates(BufferedImage scanImage,
                                                                       OcrWindowRegion targetScanRegion,
                                                                       String npcName) {
        if (scanImage == null || targetScanRegion == null) {
            return List.of();
        }
        Path washedPath = Path.of(windowScopedTempPath.resolve("npc_yellow_candidates_washed.png"));
        Path overlayPath = Path.of(windowScopedTempPath.resolve("npc_yellow_candidates_overlay.png"));
        try {
            TextCandidateScanResult result =
                    gameTextLineOcrService.findYellowTextCandidateResult(scanImage, washedPath, overlayPath);
            WindowBase windowBase = currentWindowBase("yellow-candidate-fallback");
            List<YellowTextCandidate> mapped = new ArrayList<>();
            for (TextCandidate candidate : result.candidates()) {
                mapped.add(toScreenYellowTextCandidate(candidate, targetScanRegion, windowBase));
            }
            List<YellowTextCandidate> immutable = List.copyOf(mapped);
            log.info("NPC yellow fallback candidates: npcName={} region={} status={} count={} overlay={} candidates={}",
                    npcName, targetScanRegion.toShortText(), result.status(), immutable.size(),
                    result.overlayPath(), immutable.stream().map(YellowTextCandidate::toSummaryText).toList());
            return immutable;
        } catch (Exception e) {
            log.warn("NPC yellow fallback candidate scan failed: npcName={} region={} reason={}",
                    npcName, targetScanRegion.toShortText(), e.getMessage(), e);
            return List.of();
        }
    }

    /**
     * Convert an image-local yellow text candidate into a screen-absolute Ctrl-probe candidate.
     *
     * @param candidate image-local candidate returned by {@link GameTextLineOcrService}.
     * @param scanRegion window-relative scan origin used to capture the candidate image.
     * @param windowBase screen-absolute top-left of the bound game window.
     * @return candidate whose rectangle, text center, and click point are screen-absolute.
     */
    private YellowTextCandidate toScreenYellowTextCandidate(TextCandidate candidate,
                                                            OcrWindowRegion scanRegion,
                                                            WindowBase windowBase) {
        OcrWindowRegion localRegion = candidate.region();
        OcrWindowRegion absRegion = new OcrWindowRegion(
                windowBase.x() + scanRegion.x1() + localRegion.x1(),
                windowBase.y() + scanRegion.y1() + localRegion.y1(),
                windowBase.x() + scanRegion.x1() + localRegion.x2(),
                windowBase.y() + scanRegion.y1() + localRegion.y2());
        Point textCenterAbs = new Point(
                (absRegion.x1() + absRegion.x2()) / 2,
                (absRegion.y1() + absRegion.y2()) / 2);
        Point clickPointAbs = new Point(
                windowBase.x() + scanRegion.x1() + candidate.clickPoint().x,
                windowBase.y() + scanRegion.y1() + candidate.clickPoint().y);
        return new YellowTextCandidate(textCenterAbs, clickPointAbs, absRegion,
                candidate.score(), "", candidate.reason());
    }

    /**
     * Prepare a captured NPC OCR image with the shared default-region mask policy.
     *
     * <p>Most learned regions are already tight crops and can be scanned as-is. The default fallback
     * region represents the whole game client, so yellow-name and purple-player-anchor OCR must both
     * hide HUD, chat, and shortcut bars before color washing or OCR. Returning a separate copy keeps
     * ownership simple: the caller still owns and flushes both images.</p>
     *
     * @param raw captured image for {@code scanRegion}; nullable only on capture failure.
     * @param scanRegion window-relative region that produced {@code raw}.
     * @param purpose short log label, for example {@code yellow target} or {@code purple player-anchor}.
     * @return image to send into OCR preprocessing; caller owns and must flush it when different from
     * {@code raw}.
     */
    private BufferedImage prepareNpcOcrScanImage(BufferedImage raw, OcrWindowRegion scanRegion, String purpose) {
        return prepareNpcOcrScanImage(raw, scanRegion, purpose, false);
    }

    private BufferedImage prepareNpcOcrScanImage(BufferedImage raw,
                                                 OcrWindowRegion scanRegion,
                                                 String purpose,
                                                 boolean skipDefaultMask) {
        if (raw == null) {
            return null;
        }
        if (!OcrWindowScanService.isDefaultMaskedWindowRegion(scanRegion)) {
            return raw;
        }
        if (skipDefaultMask) {
            log.info("NPC {} default mask skipped for direct-combat mode: region={}",
                    purpose, scanRegion.toShortText());
            return raw;
        }
        BufferedImage masked = OcrWindowScanService.copyWithDefaultMasks(raw);
        if (masked == null) {
            log.warn("NPC {} default masked scan failed: could not create masked image", purpose);
            return null;
        }
        log.info("NPC {} default masked scan image prepared: region={}", purpose, scanRegion.toShortText());
        return masked;
    }

    /**
     * Convert OCR word boxes from scan-image local coordinates into a window-relative text region.
     *
     * @param words OCR words whose boxes are local to the scanned crop.
     * @param scanRegion window-relative crop region that produced the words.
     * @return window-relative rectangle covering all usable OCR boxes, or null when none exist.
     */
    private OcrWindowRegion windowRegionOfWords(List<OcrWordResult> words,
                                                OcrWindowRegion scanRegion) {
        if (words == null || words.isEmpty() || scanRegion == null) {
            return null;
        }
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        for (OcrWordResult word : words) {
            if (word == null) {
                continue;
            }
            minX = Math.min(minX, word.getLeft());
            minY = Math.min(minY, word.getTop());
            maxX = Math.max(maxX, word.getLeft() + Math.max(1, word.getWidth()));
            maxY = Math.max(maxY, word.getTop() + Math.max(1, word.getHeight()));
        }
        if (minX == Integer.MAX_VALUE) {
            return null;
        }
        return new OcrWindowRegion(
                scanRegion.x1() + minX,
                scanRegion.y1() + minY,
                scanRegion.x1() + maxX,
                scanRegion.y1() + maxY)
                .clamp(WINDOW_WIDTH, WINDOW_HEIGHT);
    }

    /**
     * Pick only OCR boxes that actually form the accepted yellow target name.
     *
     * <p>{@link GameTextLineOcrService#findYellowTarget(BufferedImage, String, Path)} may return
     * the whole accepted yellow line. Dense game screenshots can include unrelated yellow OCR
     * blocks on the same packed scan, so using every word would drag the click point away from the
     * target NPC. This method keeps the smallest contiguous word span whose normalized text matches
     * the target; if no such span exists, it falls back to all words to preserve the old behavior.</p>
     *
     * @param words OCR boxes in image-local coordinates from the selected yellow scan.
     * @param normalizedTarget expected target name after OCR normalization.
     * @return OCR boxes used for the click-point rectangle.
     */
    private List<OcrWordResult> selectYellowTargetWords(List<OcrWordResult> words, String normalizedTarget) {
        if (words == null || words.isEmpty() || normalizedTarget == null || normalizedTarget.isBlank()) {
            return words;
        }
        List<OcrWordResult> best = List.of();
        int bestTier = Integer.MAX_VALUE;
        int bestExtra = Integer.MAX_VALUE;
        for (int start = 0; start < words.size(); start++) {
            StringBuilder joined = new StringBuilder();
            List<OcrWordResult> span = new ArrayList<>();
            for (int end = start; end < words.size(); end++) {
                OcrWordResult word = words.get(end);
                if (word == null || word.getText() == null) {
                    continue;
                }
                span.add(word);
                joined.append(word.getText());
                String normalizedSpan = OcrTextMatcher.normalizeName(joined.toString());
                if (normalizedSpan.isBlank()) {
                    continue;
                }
                int tier = yellowTargetMatchTier(normalizedSpan, normalizedTarget);
                if (tier < Integer.MAX_VALUE) {
                    int extra = Math.abs(normalizedSpan.length() - normalizedTarget.length());
                    if (best.isEmpty()
                            || tier < bestTier
                            || (tier == bestTier && extra < bestExtra)
                            || (tier == bestTier && extra == bestExtra && span.size() < best.size())) {
                        best = List.copyOf(span);
                        bestTier = tier;
                        bestExtra = extra;
                    }
                }
            }
        }
        if (!best.isEmpty()) {
            return best;
        }
        if (strictYellowTargetMinCommon(normalizedTarget) > 0) {
            log.warn("NPC strict yellow target word selection rejected all OCR words: target={} words={}",
                    normalizedTarget, summarizeWords(words));
            return List.of();
        }
        log.warn("NPC yellow target word selection fell back to all words: target={} words={}",
                normalizedTarget, summarizeWords(words));
        return words;
    }

    private int yellowTargetMatchTier(String normalizedSpan, String normalizedTarget) {
        int strictMinCommon = strictYellowTargetMinCommon(normalizedTarget);
        if (normalizedSpan.equals(normalizedTarget)) {
            return 0;
        }
        if (normalizedSpan.contains(normalizedTarget)) {
            return 1;
        }
        if (strictMinCommon > 0) {
            int almostFull = Math.max(strictMinCommon, normalizedTarget.length() - 1);
            return normalizedTarget.contains(normalizedSpan)
                    && normalizedSpan.length() >= almostFull
                    ? 2
                    : Integer.MAX_VALUE;
        }
        if (normalizedSpan.length() >= Math.min(2, normalizedTarget.length())
                && normalizedTarget.contains(normalizedSpan)) {
            return 2;
        }
        return OcrTextMatcher.isShortNameMatch(normalizedSpan, normalizedTarget) ? 3 : Integer.MAX_VALUE;
    }

    private String summarizeWords(List<OcrWordResult> words) {
        if (words == null || words.isEmpty()) {
            return "[]";
        }
        StringBuilder builder = new StringBuilder("[");
        for (int i = 0; i < words.size(); i++) {
            OcrWordResult word = words.get(i);
            if (i > 0) {
                builder.append(", ");
            }
            if (word == null) {
                builder.append("null");
            } else {
                builder.append("'")
                        .append(word.getText())
                        .append("'@(")
                        .append(word.getLeft())
                        .append(",")
                        .append(word.getTop())
                        .append(",")
                        .append(word.getWidth())
                        .append("x")
                        .append(word.getHeight())
                        .append(")");
            }
        }
        return builder.append("]").toString();
    }

    /**
     * Yellow-target scan outcome used to keep region expansion conservative.
     *
     * <p>The important distinction is between "the target text was absent from this region" and
     * "the target text was found but the click did not verify." Target-miss states may still carry
     * ranked visual fallback candidates for Ctrl probing; those candidates are not clicked directly.</p>
     */
    private enum YellowTargetClickStatus {
        /** The yellow OCR path found and clicked the target, and the expected dialog verified. */
        CLICK_VERIFIED,
        /** The region was readable, but no matching yellow target text was found. */
        TARGET_NOT_FOUND,
        /** No exact target OCR match, but shape-ranked yellow text candidates are available. */
        TARGET_NOT_FOUND_WITH_CANDIDATES,
        /** A matching target was found and clicked, but the expected dialog did not verify. */
        CLICK_NOT_VERIFIED,
        /** Capture/OCR plumbing failed, so the region result is not trustworthy. */
        SCAN_FAILED
    }

    private static class JointAnchorResult {
        private final boolean matched;
        private final OcrWindowRegion anchorRect;

        private JointAnchorResult(boolean matched, OcrWindowRegion anchorRect) {
            this.matched = matched;
            this.anchorRect = anchorRect;
        }

        static JointAnchorResult hit(OcrWindowRegion anchorRect) {
            return new JointAnchorResult(anchorRect != null && anchorRect.isValid(), anchorRect);
        }

        static JointAnchorResult miss() {
            return new JointAnchorResult(false, null);
        }

        boolean matched() {
            return matched;
        }

        OcrWindowRegion anchorRect() {
            return anchorRect;
        }
    }

    @Value

    @Builder

    @AllArgsConstructor(access = AccessLevel.PUBLIC)

    @Accessors(fluent = true)

    private static class YellowTargetClickResult {

        YellowTargetClickStatus status;

        Point attemptedClickPointAbs;

        List<YellowTextCandidate> fallbackCandidates;

        List<Point> exactMatchCtrlProbePointsAbs;

        NpcClickStrategyResult evidence;

        static YellowTargetClickResult targetNotFound(OcrWindowRegion scanRegion, String observedText) {
            return new YellowTargetClickResult(
                    YellowTargetClickStatus.TARGET_NOT_FOUND,
                    null,
                    List.of(),
                    List.of(),
                    NpcClickStrategyResult.notFound(
                            NpcClickStrategySource.YELLOW_TARGET_OCR,
                            scanRegion,
                            "yellow target not found text=" + observedText));
        }

        static YellowTargetClickResult targetNotFoundWithCandidates(OcrWindowRegion scanRegion,
                                                                    List<YellowTextCandidate> fallbackCandidates,
                                                                    String observedText) {
            return new YellowTargetClickResult(
                    YellowTargetClickStatus.TARGET_NOT_FOUND_WITH_CANDIDATES,
                    null,
                    fallbackCandidates == null ? List.of() : List.copyOf(fallbackCandidates),
                    List.of(),
                    NpcClickStrategyResult.notFound(
                            NpcClickStrategySource.YELLOW_TARGET_OCR,
                            scanRegion,
                            "yellow target not found with candidates text=" + observedText));
        }

        static YellowTargetClickResult scanFailed() {
            return scanFailed(null, "yellow target scan failed");
        }

        static YellowTargetClickResult scanFailed(OcrWindowRegion scanRegion, String message) {
            return new YellowTargetClickResult(
                    YellowTargetClickStatus.SCAN_FAILED,
                    null,
                    List.of(),
                    List.of(),
                    NpcClickStrategyResult.failed(NpcClickStrategySource.YELLOW_TARGET_OCR, scanRegion, message));
        }

        boolean allowsRegionExpansion() {
            return status == YellowTargetClickStatus.TARGET_NOT_FOUND
                    || status == YellowTargetClickStatus.TARGET_NOT_FOUND_WITH_CANDIDATES;
        }

        List<Point> ctrlProbePointsAbs() {
            List<Point> points = new ArrayList<>();
            if (exactMatchCtrlProbePointsAbs != null) {
                points.addAll(exactMatchCtrlProbePointsAbs);
            }
            if (fallbackCandidates != null && !fallbackCandidates.isEmpty()) {
                points.addAll(fallbackCandidates.stream()
                        .map(YellowTextCandidate::clickPointAbs)
                        .toList());
            }
            return List.copyOf(points);
        }
    

    }

    @Value

    @Builder

    @AllArgsConstructor(access = AccessLevel.PUBLIC)

    @Accessors(fluent = true)

    private static class YellowTextCandidate {

        Point textCenterAbs;

        Point clickPointAbs;

        OcrWindowRegion textRectAbs;

        double score;

        String sourceText;

        String reason;

        String toSummaryText() {
            return "textCenter=(" + textCenterAbs.x + "," + textCenterAbs.y + ")"
                    + ", click=(" + clickPointAbs.x + "," + clickPointAbs.y + ")"
                    + ", score=" + score
                    + ", rect=" + textRectAbs.toShortText()
                    + ", reason=" + reason;
        }
    

    }

    /**
     * Click an NPC by combining current player anchor OCR with logical map-coordinate delta.
     *
     * <p>This is the historical first-shot formula path. It reads the current logical location,
     * extracts the current player's purple-name anchor from a window-relative scan region, converts
     * the target map-coordinate delta into screen pixels, applies tune offsets, sends one atomic
     * move+click sequence, and records a vision-memory sample. Failed clicks are recorded as
     * prediction+verification outcomes, not as measured ground-truth coordinates.</p>
     *
     * @param player current player identity; name is used to find the purple anchor.
     * @param mapName target NPC map name.
     * @param mapX target NPC logical in-game X coordinate.
     * @param mapY target NPC logical in-game Y coordinate.
     * @param npcName target NPC name for logging/memory.
     * @param tuneX screen-pixel X correction.
     * @param tuneY screen-pixel Y correction.
     * @param scanRegion resolved visual work region for purple-name OCR.
     * @param cachedPlayerLocation current player logical coordinate already maintained by the
     *                             task/navigation flow; this method does not perform a fresh OCR
     *                             location sync.
     * @param skipDefaultOcrMask true only for Alt+A direct-combat mode, where HUD masking would hide
     *                           valid edge monsters/player names after the game has already cleaned
     *                           the screen.
     * @return true only when the computed click opens the expected dialog.
     */
    private FormulaClickPrediction calculatePlayerAnchorFormulaPoint(PlayerCharacter player,
                                                                    String mapName,
                                                                    int mapX,
                                                                    int mapY,
                                                                    String npcName,
                                                                    int tuneX,
                                                                    int tuneY,
                                                                    ResolvedNpcClickRegion scanRegion,
                                                                    LocationInfo cachedPlayerLocation,
                                                                    boolean skipDefaultOcrMask) {
        return calculatePlayerAnchorFormulaPoint(player, mapName, mapX, mapY, npcName, tuneX, tuneY,
                scanRegion, cachedPlayerLocation, skipDefaultOcrMask, true);
    }

    private FormulaClickPrediction calculatePlayerAnchorFormulaPoint(PlayerCharacter player,
                                                                    String mapName,
                                                                    int mapX,
                                                                    int mapY,
                                                                    String npcName,
                                                                    int tuneX,
                                                                    int tuneY,
                                                                    ResolvedNpcClickRegion scanRegion,
                                                                    LocationInfo cachedPlayerLocation,
                                                                    boolean skipDefaultOcrMask,
                                                                    boolean prepareAlt4) {
        if (shouldStop()) return null;
        /*
         * The formula path needs the current player's identity to verify the purple name anchor.
         * Without it, the old blob fallback could treat a huge masked region as "the player name"
         * and produce a bad first-shot point. Skip the whole formula path until task startup has
         * synced identity correctly.
         */
        if (player == null || player.getName() == null || player.getName().isBlank()) {
            log.warn("NPC player-anchor formula skipped: missing player identity npc={} map={} coord=({}, {})",
                    npcName, mapName, mapX, mapY);
            return null;
        }
        OcrWindowRegion targetScanRegion = scanRegion == null ? null : scanRegion.windowRegion();
        WindowBase windowBase = windowBase(scanRegion);
        int gameBaseX = windowBase.x();
        int gameBaseY = windowBase.y();
        int scanStartX = scanRegion.screenX1();
        int scanStartY = scanRegion.screenY1();
        LocationInfo locInfo = cachedPlayerLocation;

        String centerScanPath = windowScopedTempPath.resolve("center_scan_layer1.png");
        String playerScanPath = windowScopedTempPath.resolve("center_scan_player.png");

        BufferedImage rawPlayerAnchor = captureCleanNameRegionToMemory("NPC first-shot player anchor raw", scanRegion, prepareAlt4);
        if (rawPlayerAnchor == null) {
            log.warn("NPC player-anchor formula skipped: capture failed npc={} region={}",
                    npcName, targetScanRegion.toShortText());
            return null;
        }
        if (shouldStop()) {
            rawPlayerAnchor.flush();
            return null;
        }
        BufferedImage playerAnchorScan = prepareNpcOcrScanImage(
                rawPlayerAnchor, targetScanRegion, "purple player-anchor", skipDefaultOcrMask);
        if (playerAnchorScan == null) {
            rawPlayerAnchor.flush();
            return null;
        }
        try {
            ImageIO.write(playerAnchorScan, "png", Path.of(centerScanPath).toFile());
        } catch (IOException e) {
            log.warn("NPC purple player-anchor source write failed: path={} reason={}",
                    centerScanPath, e.getMessage(), e);
            return null;
        } finally {
            if (playerAnchorScan != rawPlayerAnchor) {
                playerAnchorScan.flush();
            }
            rawPlayerAnchor.flush();
        }
        if (shouldStop()) return null;
        ImagePreprocessor.washPurpleTextToBlackAndWhite(centerScanPath, playerScanPath);

        AtomicReference<PlayerAnchorMatch> playerAnchorMatchRef = new AtomicReference<>();
        List<OcrWordResult> playerWords = ocr.getAllTextResultsForMatch(
                playerScanPath,
                "npc-first-shot-player-anchor:" + (player == null ? "-" : player.getName()),
                words -> {
                    PlayerAnchorMatch match =
                            extractPlayerAnchorMatchFromWords(words, player, scanStartX, scanStartY);
                    playerAnchorMatchRef.set(match);
                    return match != null;
                });
        PlayerAnchorMatch playerAnchorMatch = playerAnchorMatchRef.get();
        if (playerAnchorMatch == null) {
            /*
             * getAllTextResultsForMatch may return the best unmatched diagnostic OCR result when
             * both providers fail the matcher. Re-check that returned list once before falling back
             * to the raw purple blob; this still reuses the same screenshot and OCR output.
             */
            playerAnchorMatch = extractPlayerAnchorMatchFromWords(playerWords, player, scanStartX, scanStartY);
        }
        Point playerAnchor = playerAnchorMatch == null ? null : playerAnchorMatch.anchor();
        if (playerAnchor == null) {
            playerAnchor = extractPurpleBlobAnchor(playerScanPath, scanStartX, scanStartY);
        }

        if (locInfo != null && playerAnchor != null) {
            int deltaLogicX = mapX - locInfo.x;
            int deltaLogicY = mapY - locInfo.y;

            int deltaPhysX = (int) Math.round(deltaLogicX * UX + deltaLogicY * VX);
            int deltaPhysY = (int) Math.round(deltaLogicX * UY + deltaLogicY * VY);

            int targetX = playerAnchor.x + deltaPhysX + tuneX;
            int targetY = playerAnchor.y + deltaPhysY - 50 + tuneY;
            log.info("NPC first shot formal: currentLocation={} ({}, {}) playerAnchor=({}, {}) playerName={}",
                    locInfo.mapName, locInfo.x, locInfo.y, playerAnchor.x, playerAnchor.y,
                    player == null ? null : player.getName());
            log.info("NPC first shot formal: targetNpc={} targetCoord=({}, {}) tune=({}, {}) deltaLogic=({}, {}) deltaPhys=({}, {}) finalClick=({}, {})",
                    npcName, mapX, mapY, tuneX, tuneY, deltaLogicX, deltaLogicY, deltaPhysX, deltaPhysY, targetX, targetY);
            return new FormulaClickPrediction(
                    mapName,
                    locInfo,
                    npcName,
                    mapX,
                    mapY,
                    new Point(gameBaseX, gameBaseY),
                    playerAnchor,
                    new Point(targetX, targetY),
                    tuneX,
                    tuneY);
        }

        if (locInfo != null) {
            log.info("playerAnchor is null, give up the first shot");
        } else {
            log.info("locInfo is null, give up the first shot");
        }
        return null;
    }

    /**
     * Execute a previously calculated player-anchor formula click and record the verification result.
     *
     * @param prediction formula output containing screen-absolute click point and the OCR context
     *                   used to produce it; null means the formula path is unavailable.
     * @param expectedDialogTemplatePath green-option template that confirms success.
     * @return structured evidence. A skipped result means formula input was unavailable.
     */
    private NpcClickStrategyResult clickNpcByPlayerAnchorFormula(FormulaClickPrediction prediction,
                                                                 NpcClickVerifier verifier) {
        if (shouldStop()) {
            return NpcClickStrategyResult.failed(
                    NpcClickStrategySource.PLAYER_ANCHOR_FORMULA,
                    "interrupted-before-formula-click");
        }
        if (prediction == null) {
            return NpcClickStrategyResult.skipped(
                    NpcClickStrategySource.PLAYER_ANCHOR_FORMULA,
                    "formula prediction unavailable");
        }
        Point target = prediction.predictedClickAbs();
        if (!isInsideWindow(target, prediction.windowBaseAbs())) {
            log.warn("NPC player-anchor formula skipped: predicted click outside window npc={} point=({}, {}) windowBase=({}, {})",
                    prediction.npcName(), target.x, target.y,
                    prediction.windowBaseAbs().x, prediction.windowBaseAbs().y);
            return NpcClickStrategyResult.skipped(
                    NpcClickStrategySource.PLAYER_ANCHOR_FORMULA,
                    "formula predicted click outside window");
        }
        boolean firstShotOk = executeMoveClickAndVerify("npcClick:firstShotMoveClick",
                target.x, target.y, 1500, 0, verifier);
        if (!firstShotOk) {
            TaskSleep.sleep(1500);
        }
        return NpcClickStrategyResult.fromClick(
                NpcClickStrategySource.PLAYER_ANCHOR_FORMULA,
                firstShotOk ? NpcClickStrategyStatus.VERIFIED : NpcClickStrategyStatus.CLICK_NOT_VERIFIED,
                null,
                null,
                prediction.predictedClickAbs(),
                windowRelativePoint(prediction.predictedClickAbs(),
                        new WindowBase(prediction.windowBaseAbs().x, prediction.windowBaseAbs().y)),
                true,
                firstShotOk,
                firstShotOk ? "formula click verified" : "formula click not verified");
    }

    /**
     * Calculate the center of OCR word boxes in image-local coordinates.
     *
     * @param words OCR boxes whose left/top/width/height are relative to the scanned image.
     * @return image-local center point, or null when no usable OCR box exists.
     */
    private Point centerOfWords(List<OcrWordResult> words) {
        if (words == null || words.isEmpty()) {
            return null;
        }
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        for (OcrWordResult word : words) {
            if (word == null) {
                continue;
            }
            minX = Math.min(minX, word.getLeft());
            minY = Math.min(minY, word.getTop());
            maxX = Math.max(maxX, word.getLeft() + Math.max(1, word.getWidth()));
            maxY = Math.max(maxY, word.getTop() + Math.max(1, word.getHeight()));
        }
        if (minX == Integer.MAX_VALUE) {
            return null;
        }
        return new Point((minX + maxX) / 2, (minY + maxY) / 2);
    }

    private boolean isInsideWindow(Point point, Point windowBaseAbs) {
        if (point == null || windowBaseAbs == null) {
            return false;
        }
        int relX = point.x - windowBaseAbs.x;
        int relY = point.y - windowBaseAbs.y;
        return relX >= 0 && relX < WINDOW_WIDTH && relY >= 0 && relY < WINDOW_HEIGHT;
    }

    private boolean hasNpcMenuMatch(List<OcrWordResult> words, String targetName) {
        if (words == null || words.isEmpty()) {
            return false;
        }
        for (OcrWordResult word : words) {
            if (word == null || word.getText() == null) {
                continue;
            }
            String text = word.getText();
            if ((targetName != null && !targetName.isBlank()
                    && OcrTextMatcher.isShortNameMatch(text, targetName))
                    || text.matches(NPC_TAG_REGEX)) {
                return true;
            }
        }
        return false;
    }

    private PlayerAnchorMatch extractPlayerAnchorMatchFromWords(
            List<OcrWordResult> words,
            PlayerCharacter player,
            int scanStartX,
            int scanStartY) {
        if (words == null || player == null || player.getName() == null) {
            return null;
        }
        return locationVisionService.extractPlayerAnchorMatch(words, player.getName(), scanStartX, scanStartY, 0);
    }

    /**
     * Locate the player's purple-name blob without recognizing the actual text.
     *
     * <p>This is intentionally a fallback after exact player-name OCR fails. The caller has already
     * pressed Alt+4 before capturing the source region, so other player names should be hidden by
     * the game client. The washed image keeps purple text as dark pixels on a light background; this
     * method takes the bounding box of those dark pixels and uses its center as the player-name
     * anchor. It does not send input or call OCR.</p>
     *
     * @param washedPurplePath filesystem path to the purple-washed binary image.
     * @param scanStartX screen-absolute X coordinate of the scanned region's left edge.
     * @param scanStartY screen-absolute Y coordinate of the scanned region's top edge.
     * @return screen-absolute approximate player-name anchor, or null when the washed image has no
     *         plausible purple-name blob.
     */
    private Point extractPurpleBlobAnchor(String washedPurplePath, int scanStartX, int scanStartY) {
        if (washedPurplePath == null || washedPurplePath.isBlank()) {
            return null;
        }
        try {
            BufferedImage image = ImageIO.read(Path.of(washedPurplePath).toFile());
            if (image == null) {
                log.info("NPC purple blob fallback skipped: image not readable path={}", washedPurplePath);
                return null;
            }
            try {
                int minX = Integer.MAX_VALUE;
                int minY = Integer.MAX_VALUE;
                int maxX = Integer.MIN_VALUE;
                int maxY = Integer.MIN_VALUE;
                int darkPixels = 0;
                for (int y = 0; y < image.getHeight(); y++) {
                    for (int x = 0; x < image.getWidth(); x++) {
                        int rgb = image.getRGB(x, y) & 0xFFFFFF;
                        if (rgb < 0x303030) {
                            minX = Math.min(minX, x);
                            minY = Math.min(minY, y);
                            maxX = Math.max(maxX, x);
                            maxY = Math.max(maxY, y);
                            darkPixels++;
                        }
                    }
                }
                if (darkPixels < PURPLE_BLOB_MIN_PIXELS || minX == Integer.MAX_VALUE) {
                    log.info("NPC purple blob fallback miss: path={} darkPixels={}", washedPurplePath, darkPixels);
                    return null;
                }
                int width = maxX - minX + 1;
                int height = maxY - minY + 1;
                /*
                 * A real player-name blob is a compact text run. Very wide/tall masks mean the
                 * purple washer captured UI/chat/background noise, so using the bounding-box center
                 * would create a fake player anchor and send the first shot to an unrelated point.
                 */
                if (width < PURPLE_BLOB_MIN_WIDTH || height < PURPLE_BLOB_MIN_HEIGHT
                        || width > PURPLE_BLOB_MAX_WIDTH || height > PURPLE_BLOB_MAX_HEIGHT
                        || darkPixels > PURPLE_BLOB_MAX_PIXELS) {
                    log.info("NPC purple blob fallback rejected: path={} darkPixels={} rect=({}, {})-({}, {}) size={}x{}",
                            washedPurplePath, darkPixels, minX, minY, maxX, maxY, width, height);
                    return null;
                }
                Point anchor = new Point(scanStartX + (minX + maxX) / 2, scanStartY + (minY + maxY) / 2);
                log.info("NPC purple blob fallback anchor: path={} darkPixels={} rect=({}, {})-({}, {}) anchor=({}, {})",
                        washedPurplePath, darkPixels, minX, minY, maxX, maxY, anchor.x, anchor.y);
                return anchor;
            } finally {
                image.flush();
            }
        } catch (IOException e) {
            log.warn("NPC purple blob fallback failed: path={} reason={}", washedPurplePath, e.getMessage(), e);
            return null;
        }
    }

    /**
     * Resolve the current blocking dialog before an NPC click, preferring the window runner's
     * recent no-focus observation over an immediate screenshot pass.
     *
     * @param request NPC click request being prepared; used only for diagnostic context.
     * @param fallbackReason reason passed to the slower detector when no fresh runner snapshot exists.
     * @return latest dialog type for the bound window, or {@link DialogType#NONE} when no dialog is
     *         visible.
     */
    private DialogType currentPreClickDialogType(NpcClickRequest request, String fallbackReason) {
        Optional<WindowRuntimeContext> current = windowTaskContextHolder.rawCurrent();
        if (current.isPresent()) {
            Optional<WindowDialogSnapshot> snapshot = current.get()
                    .getVisibleDialogSnapshot(NPC_PRE_CLICK_DIALOG_SNAPSHOT_MAX_AGE_MS);
            if (snapshot.isPresent()) {
                WindowDialogSnapshot visible = snapshot.get();
                long ageMs = Math.max(0L, System.currentTimeMillis() - visible.getDetectedAtMs());
                log.info("NPC smart click uses runner dialog snapshot before target click: npcName={} type={} source={} ageMs={}",
                        request == null ? null : request.npcName(),
                        visible.getType(),
                        visible.getSource(),
                        ageMs);
                return visible.getType();
            }
        }

        log.info("NPC smart click has no fresh runner dialog snapshot; fallback detect before target click: npcName={} reason={}",
                request == null ? null : request.npcName(), fallbackReason);
        return dialogService.detectDialogTypeNoFocus(fallbackReason, false, 0);
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
     * @return screen-absolute top-left point of the game window used by NPC click formulas and
     *         Ctrl-menu scan rectangles.
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

    private WindowBase windowBase(ResolvedNpcClickRegion region) {
        return new WindowBase(region.windowBaseX(), region.windowBaseY());
    }

    private boolean shouldStop() {
        return Thread.currentThread().isInterrupted();
    }

    private boolean captureCleanNameToFileDirect(String elementName, String savePath, int x1, int y1, int x2, int y2) {
        return captureCleanNameToFileDirect(elementName, savePath, x1, y1, x2, y2, true);
    }

    private boolean captureCleanNameToFileDirect(
            String elementName,
            String savePath,
            int x1,
            int y1,
            int x2,
            int y2,
            boolean prepareAlt4) {
        if (shouldStop()) return false;
        if (prepareAlt4) {
            inputProvider.pressAlt4();
            if (!TaskSleep.sleep(400)) return false;
        }
        return tracker.captureToFile(elementName, savePath, x1, y1, x2, y2);
    }

    private BufferedImage captureCleanNameRegionToMemory(String elementName, ResolvedNpcClickRegion region) {
        return captureCleanNameRegionToMemory(elementName, region, true);
    }

    /**
     * Capture a resolved name/OCR region, optionally preparing the name layer first.
     *
     * <p>Normal smart-click pipeline calls pass {@code prepareAlt4=false} because
     * {@link #runNpcClickPipeline(NpcClickRequest, NpcClickVerifier, String)} already pressed Alt+4
     * once for the stationary scene. Standalone debug/probe callers keep {@code true} so they remain
     * self-contained.</p>
     */
    private BufferedImage captureCleanNameRegionToMemory(
            String elementName,
            ResolvedNpcClickRegion region,
            boolean prepareAlt4) {
        AtomicReference<BufferedImage> imageRef = new AtomicReference<>();
        boolean ok = inputSequences.submitExclusiveAndWait("npcClick:cleanNameMemoryCapture:" + elementName, () -> {
            if (shouldStop()) {
                return false;
            }
            if (region == null) {
                return false;
            }
            if (prepareAlt4) {
                inputProvider.pressAlt4();
                if (!TaskSleep.sleep(NPC_PIPELINE_HIDE_PLAYER_NAMES_SETTLE_MS)) {
                    return false;
                }
            }
            BufferedImage image = tracker.captureToMemory(elementName,
                    region.screenX1(),
                    region.screenY1(),
                    region.screenX2(),
                    region.screenY2());
            imageRef.set(image);
            return image != null;
        });
        return ok ? imageRef.get() : null;
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(value, max));
    }

    @Value

    @Builder

    @AllArgsConstructor(access = AccessLevel.PUBLIC)

    @Accessors(fluent = true)

    private static class FormulaClickPrediction {

        String targetMapName;

        LocationInfo locInfo;

        String npcName;

        int targetMapX;

        int targetMapY;

        Point windowBaseAbs;

        Point playerAnchorAbs;

        Point predictedClickAbs;

        int tuneX;

        int tuneY;

    }

    @FunctionalInterface
    private interface NpcClickVerifier {
        boolean verify(String reason);
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
