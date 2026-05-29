package com.bot.dhxy.service;

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
import com.bot.dhxy.model.npc.NpcClickRequest;
import com.bot.dhxy.model.npc.NpcTooltipType;
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
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Clicks NPCs in the currently bound game window through one public request-based entry.
 *
 * <p>All real mouse/keyboard operations must be serialized through {@link InputSequences}, except
 * code already running inside an exclusive input callback where direct {@link InputProvider} calls
 * are required to avoid queue-in-queue deadlock. Coordinates passed to public click methods are
 * logical game map coordinates unless explicitly described as screen-absolute or window-relative.</p>
 *
 * <p>The smart-click order is deliberately conservative:</p>
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
public class NpcClickService {

    private final InputProvider inputProvider;
    private final InputSequences inputSequences;
    private final GameClientTracker tracker;
    private final GameStateUtil gameStateUtil;
    private final TextRecognizer ocr;
    private final LocationVisionService locationVisionService;
    private final PlayerStateService playerStateService;
    private final CoordinateHelper coordinateHelper;
    private final DialogService dialogService;
    private final WindowScopedTempPath windowScopedTempPath;
    private final WindowTaskContextHolder windowTaskContextHolder;
    private final OcrRoiMemoryService ocrRoiMemoryService;
    private final GameTextLineOcrService gameTextLineOcrService;

    private static final double UX = 20.0;
    private static final double UY = 0.0;
    private static final double VX = 0.0;
    private static final double VY = -20.0;
    private static final int WINDOW_WIDTH = 1024;
    private static final int WINDOW_HEIGHT = 768;
    private static final int YELLOW_TARGET_CLICK_RETRIES = 1;
    private static final int PURPLE_BLOB_MIN_PIXELS = 20;
    private static final int PURPLE_BLOB_MIN_WIDTH = 8;
    private static final int PURPLE_BLOB_MIN_HEIGHT = 4;
    private static final int PURPLE_BLOB_MAX_PIXELS = 6000;
    private static final int PURPLE_BLOB_MAX_WIDTH = 360;
    private static final int PURPLE_BLOB_MAX_HEIGHT = 140;

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
    private static final int CTRL_MENU_SCAN_W = 150;
    private static final int CTRL_MENU_SCAN_H = 120;

    private boolean executeMoveClickAndVerify(String description, int x, int y, long firstWaitMs, int maxRetries) {
        return executeMoveClickAndVerify(description, x, y, firstWaitMs, maxRetries, null);
    }

    /**
     * Submit one atomic move+click request and verify the expected dialog.
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
            String expectedDialogTemplatePath) {
        if (shouldStop()) return false;
        log.info("NPC move+click sequence: {} point=({}, {})", description, x, y);
        boolean queued = inputSequences.submitAndWait(description, List.of(
                InputAction.moveMouse(x, y),
                InputAction.sleep(150),
                InputAction.clickLeft(x, y, 100),
                InputAction.sleep((int) firstWaitMs)
        ));
        if (!queued) {
            log.warn("NPC move+click sequence failed in input queue: {} point=({}, {})", description, x, y);
            return false;
        }

        if (shouldStop()) return false;
        if (isExpectedDialogVisible(expectedDialogTemplatePath, description + ":firstVerify")) return true;

        for (int i = 1; i <= maxRetries; i++) {
            if (shouldStop()) return false;
            log.warn("NPC move+click retry {} point=({}, {})", i, x, y);
            queued = inputSequences.submitAndWait(description + ":retry", List.of(
                    InputAction.moveMouse(x, y),
                    InputAction.sleep(150),
                    InputAction.clickLeft(x, y, 100),
                    InputAction.sleep(1000)
            ));
            if (!queued) {
                log.warn("NPC move+click retry failed in input queue: {} retry={} point=({}, {})", description, i, x, y);
                return false;
            }
            if (shouldStop()) return false;
            if (isExpectedDialogVisible(expectedDialogTemplatePath, description + ":retryVerify:" + i)) return true;
        }
        return false;
    }

    private boolean executeClickAndVerifyDirect(int x, int y, long firstWaitMs, int maxRetries) {
        return executeClickAndVerifyDirect(x, y, firstWaitMs, maxRetries, null);
    }

    private boolean executeClickAndVerifyDirect(
            int x,
            int y,
            long firstWaitMs,
            int maxRetries,
            String expectedDialogTemplatePath) {
        if (shouldStop()) return false;
        inputProvider.clickLeft(x, y, 100);
        if (!TaskSleep.sleep(firstWaitMs)) return false;
        if (shouldStop()) return false;
        if (isExpectedDialogVisible(expectedDialogTemplatePath, "npcClick:direct:firstVerify")) return true;

        for (int i = 1; i <= maxRetries; i++) {
            if (shouldStop()) return false;
            log.warn("NPC direct click retry {}", i);
            inputProvider.clickLeft(x, y, 100);
            if (!TaskSleep.sleep(1000)) return false;
            if (shouldStop()) return false;
            if (isExpectedDialogVisible(expectedDialogTemplatePath, "npcClick:direct:retryVerify:" + i)) return true;
        }
        return false;
    }

    /**
     * Check whether the NPC click opened the expected dialog.
     *
     * <p>When the task provides a template, verification is template-based and does not click any
     * dialog option. Without a template, this falls back to generic OPTION dialog detection.</p>
     */
    private boolean isExpectedDialogVisible(String expectedDialogTemplatePath, String reason) {
        DialogResult result = dialogService.handleDialog(DialogHandleRequest.verifyExpectedOptionDialog(
                "npc-click:expected-dialog:" + reason, expectedDialogTemplatePath));
        return result.getStatus() == DialogResultStatus.OPTION_VISIBLE
                || result.getStatus() == DialogResultStatus.GREEN_TEMPLATE_VISIBLE;
    }

    /**
     * Scan around the predicted target point with Ctrl held, click a detected menu candidate, and verify a dialog.
     *
     * <p>The method first searches for the explicit NPC tag template, then falls back to OCR keyword
     * matching in the Ctrl menu. It only clicks and verifies that the expected business dialog is
     * visible; it does not click any task option inside that dialog.</p>
     *
     * @param targetKeyword NPC name or task keyword expected in the Ctrl menu OCR result.
     * @param npcTagTemplatePath template path for the "(NPC)" marker; blank disables the template path.
     * @param expectedDialogTemplatePath green-option template that proves the right dialog opened.
     * @param preferredProbePoints ordered screen-absolute target predictions with source-specific
     *                             scan profiles. The method prepends the game-window center as the
     *                             safest generic fallback before de-duplication.
     * @return structured Ctrl-menu evidence for the coordinator recorder.
     */
    private NpcClickStrategyResult clickNpcByCtrlMenuScan(
            String targetKeyword,
            String npcTagTemplatePath,
            String expectedDialogTemplatePath,
            List<CtrlProbeOrigin> preferredProbePoints) {
        return clickNpcByCtrlMenuScan(
                targetKeyword, npcTagTemplatePath, expectedDialogTemplatePath, preferredProbePoints, true);
    }

    private NpcClickStrategyResult clickNpcByCtrlMenuScan(
            String targetKeyword,
            String npcTagTemplatePath,
            String expectedDialogTemplatePath,
            List<CtrlProbeOrigin> preferredProbePoints,
            boolean includeWindowCenterFallback) {
        if (targetKeyword == null || targetKeyword.isBlank()) {
            log.warn("NPC ctrl menu scan requested without target keyword");
            return NpcClickStrategyResult.skipped(NpcClickStrategySource.CTRL_MENU, "missing-target-keyword");
        }
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
                                        expectedDialogTemplatePath,
                                        new Point(testX, testY));
                        resultRef.set(result);
                        return result.verified();
                    } finally {
                        inputProvider.releaseCtrl();
                        TaskSleep.sleep(100);
                    }
                });

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

        log.warn("NPC ctrl menu scan failed: keyword={} npcTagTemplate={} expectedDialog={}",
                targetKeyword, npcTagTemplatePath, expectedDialogTemplatePath);
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

    /**
     * OCR the visible Ctrl-menu text and click a fuzzy name/tag match.
     *
     * <p>Coordinates are screen-absolute because this method runs inside the Ctrl exclusive input
     * callback. The crop is washed for yellow text, OCR results are matched through the shared short
     * name matcher, and success still requires the expected dialog template after clicking.</p>
     *
     * @param scanRect screen-absolute [left, top, right, bottom] crop that covers every possible
     *                 Ctrl-menu quadrant around the cursor probe.
     * @param targetKeyword expected NPC/monster keyword.
     * @param expectedDialogTemplatePath green-option template that verifies the target dialog.
     * @return structured evidence for the matching/click attempt.
     */
    private NpcClickStrategyResult scanMenuAndVerifyKeywordDirect(
            int[] scanRect,
            String targetKeyword,
            String expectedDialogTemplatePath,
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
                expectedDialogTemplatePath,
                ctrlHoverPointAbs);
    }

    private NpcClickStrategyResult scanMenuAndVerifyKeywordDirect(
            int scanX,
            int scanY,
            int scanRight,
            int scanBottom,
            String targetKeyword,
            String expectedDialogTemplatePath,
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
                boolean verified = executeClickAndVerifyDirect(clickX, clickY, 800, 1, expectedDialogTemplatePath);
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
     * Debug-only first-shot probe for the player-anchor coordinate formula.
     *
     * <p>This path captures the center player-name region, derives the current player anchor from
     * purple-name OCR, computes the predicted NPC screen point, and sends one direct click. It does
     * not run the Ctrl-menu fallback and does not treat the click as independently measured ground
     * truth in vision memory.</p>
     *
     * @param player current player identity; its name is used as the purple-anchor OCR target.
     * @param mapName target NPC map name for logging/memory only.
     * @param mapX target NPC logical in-game X coordinate.
     * @param mapY target NPC logical in-game Y coordinate.
     * @param npcName target NPC name for logging/memory.
     * @param tuneX screen-pixel X correction added to the formula result.
     * @param tuneY screen-pixel Y correction added to the formula result.
     * @return true after the debug click is sent; false if current location or player anchor is missing.
     */
    public boolean debugClickNpcSmartFirstShot(PlayerCharacter player, String mapName, int mapX, int mapY, String npcName, int tuneX, int tuneY) {
        log.info("[npc-first-shot-debug] start map={} targetNpc={} targetCoord=({}, {}) tune=({}, {})",
                mapName, npcName, mapX, mapY, tuneX, tuneY);

        if (shouldStop()) return false;
        WindowBase windowBase = currentWindowBase("debug-first-shot");
        int gameBaseX = windowBase.x();
        int gameBaseY = windowBase.y();
        int screenCenterX = gameBaseX + (1024 / 2);
        int screenCenterY = gameBaseY + (768 / 2);
        log.info("[npc-first-shot-debug] windowBase=({}, {}) screenCenter=({}, {})",
                gameBaseX, gameBaseY, screenCenterX, screenCenterY);

        LocationInfo locInfo = playerStateService.syncMyPosition();
        if (locInfo == null) {
            log.warn("[npc-first-shot-debug] current location unavailable; cannot compute map delta");
            return false;
        }
        log.info("[npc-first-shot-debug] currentLocation map={} coord=({}, {})", locInfo.mapName, locInfo.x, locInfo.y);

        int scanWidth = 350;
        int scanHeight = 200;
        int scanStartX = screenCenterX - (scanWidth / 2);
        int scanStartY = screenCenterY - (scanHeight / 2);
        log.info("[npc-first-shot-debug] playerAnchor scanRect=({}, {})-({}, {}) size={}x{}",
                scanStartX, scanStartY, scanStartX + scanWidth, scanStartY + scanHeight, scanWidth, scanHeight);

        String centerScanPath = windowScopedTempPath.resolve("debug_npc_firstshot_center_raw.png");
        String playerScanPath = windowScopedTempPath.resolve("debug_npc_firstshot_player_washed.png");

        captureCleanNameToFile("NPC first-shot debug player-anchor capture", centerScanPath,
                scanStartX, scanStartY, scanStartX + scanWidth, scanStartY + scanHeight);
        ImagePreprocessor.washPurpleTextToBlackAndWhite(centerScanPath, playerScanPath);

        List<OcrWordResult> playerWords = ocr.getAllTextResultsForMatch(
                playerScanPath,
                "npc-first-shot-debug-player-anchor:" + (player == null ? "-" : player.getName()),
                words -> canExtractPlayerAnchor(words, player, scanStartX, scanStartY));
        Point playerAnchor = null;
        if (playerWords != null && player != null && player.getName() != null) {
            playerAnchor = locationVisionService.extractPlayerPhysicalAnchor(
                    playerWords, player.getName(), scanStartX, scanStartY, 0);
        }
        if (playerAnchor == null) {
            log.warn("[npc-first-shot-debug] playerAnchor unavailable: playerName={} washedPath={}",
                    player == null ? null : player.getName(), playerScanPath);
            return false;
        }
        log.info("[npc-first-shot-debug] playerAnchor=({}, {}) playerName={}",
                playerAnchor.x, playerAnchor.y, player == null ? null : player.getName());

        int deltaLogicX = mapX - locInfo.x;
        int deltaLogicY = mapY - locInfo.y;
        int deltaPhysX = (int) Math.round(deltaLogicX * UX + deltaLogicY * VX);
        int deltaPhysY = (int) Math.round(deltaLogicX * UY + deltaLogicY * VY);
        int targetX = playerAnchor.x + deltaPhysX + tuneX;
        int targetY = playerAnchor.y + deltaPhysY - 50 + tuneY;

        log.info("[npc-first-shot-debug] deltaLogic=({}, {}) deltaPhys=({}, {}) formula=playerAnchor+deltaPhys+tune+(0,-50)",
                deltaLogicX, deltaLogicY, deltaPhysX, deltaPhysY);
        log.info("[npc-first-shot-debug] FINAL_CLICK_POINT=({}, {})", targetX, targetY);

        inputProvider.moveMouse(targetX, targetY);
        TaskSleep.sleep(500);
        inputProvider.clickLeft(targetX, targetY, 100);
        TaskSleep.sleep(800);
        recordNpcClickMemory(
                "NPC_FIRST_SHOT_DEBUG",
                mapName,
                locInfo,
                npcName,
                mapX,
                mapY,
                new Point(gameBaseX, gameBaseY),
                playerAnchor,
                new Point(targetX, targetY),
                new Point(targetX, targetY),
                tuneX,
                tuneY,
                true,
                true,
                "DEBUG_CLICK_SENT_UNVERIFIED",
                "manual debug click path");
        log.info("[npc-first-shot-debug] direct debug click sent: point=({}, {})", targetX, targetY);
        return true;
    }

    /**
     * Debug only the production purple-name player-anchor formula path.
     *
     * <p>This intentionally bypasses learned memory, task-tooltip templates, yellow target OCR, and
     * Ctrl-menu probing. Use it when the formula itself looks suspicious: the method reuses the same
     * cached player coordinate, ROI recommendation, purple-name wash/OCR, formula math, and dialog
     * verification used by production {@link #clickNpcSmart(NpcClickRequest)}.</p>
     *
     * @param request target facts. mapX/mapY are logical game coordinates; tuneX/tuneY are
     *                screen-pixel formula corrections; expectedDialogTemplatePath may be blank, in
     *                which case generic OPTION-dialog detection is used.
     * @return true only when the purple formula click opens/verifies a dialog.
     */
    public boolean debugClickNpcByPurpleAnchorOnly(NpcClickRequest request) {
        if (request == null) {
            log.warn("[npc-purple-debug] skipped: request is null");
            return false;
        }
        long latencyStart = LatencyMetrics.start();
        boolean result = false;
        try {
            tracker.updateGlobalVision();
            LocationInfo playerLocation = cachedPlayerLocation(request);
            List<ResolvedNpcClickRegion> targetScanRegions = resolveNpcScanRegions(request, playerLocation);
            log.info("[npc-purple-debug] start npc={} map={} target=({}, {}) player=({}, {}) tune=({}, {}) regions={} expectedTemplate={}",
                    request.npcName(), request.mapName(), request.mapX(), request.mapY(),
                    playerLocation == null ? null : playerLocation.x,
                    playerLocation == null ? null : playerLocation.y,
                    request.tuneX(), request.tuneY(), summarizeRegions(targetScanRegions),
                    request.expectedDialogTemplatePath());
            if (targetScanRegions == null || targetScanRegions.isEmpty()) {
                log.warn("[npc-purple-debug] no ROI region available; cannot run purple formula");
                return false;
            }

            FormulaClickPrediction prediction = calculatePlayerAnchorFormulaPoint(
                    request.player(), request.mapName(), request.mapX(), request.mapY(),
                    request.npcName(), request.tuneX(), request.tuneY(), targetScanRegions.get(0), playerLocation);
            NpcClickStrategyResult formulaResult =
                    clickNpcByPlayerAnchorFormula(prediction, request.expectedDialogTemplatePath());
            recordSmartClickEvidence(request, formulaResult, playerLocation);
            result = formulaResult.verified();
            log.info("[npc-purple-debug] result={} status={} message={}",
                    result, formulaResult.status(), formulaResult.message());
            return result;
        } finally {
            LatencyMetrics.info(log, "npc.click.purpleDebug", latencyStart,
                    "result=" + result + " target=" + request.npcName() + "@"
                            + request.mapName() + "(" + request.mapX() + "," + request.mapY() + ")");
        }
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
            tracker.updateGlobalVision();
            LocationInfo playerLocation = cachedPlayerLocation(request);
            if (playerLocation == null) {
                log.warn("NPC smart click has no cached player coordinate; learning/recommendation will use full-window fallback only: npcName={} map={} target=({}, {})",
                        request.npcName(), request.mapName(), request.mapX(), request.mapY());
            }
            List<ResolvedNpcClickRegion> targetScanRegions = resolveNpcScanRegions(request, playerLocation);
            List<CtrlProbeOrigin> ctrlProbeOrigins = new ArrayList<>();
            log.info("NPC smart click request: npcName={} map={} target=({}, {}) player=({}, {}) roaming={} regions={} expectedTemplate={}",
                    request.npcName(), request.mapName(), request.mapX(), request.mapY(),
                    playerLocation == null ? null : playerLocation.x,
                    playerLocation == null ? null : playerLocation.y,
                    request.roamingTarget(), summarizeRegions(targetScanRegions), request.expectedDialogTemplatePath());

            /*
             * 1. Fast remembered path: reuse a conservative learned click cluster for the same map/name/point.
             * If it misses, keep the attempted physical point as a Ctrl origin instead of throwing away
             * that evidence. Running this before tooltip matching makes it obvious in logs whether the
             * database/memory point is mature enough to replace screenshot matching for stable NPCs.
             */
            NpcClickStrategyResult learnedResult = clickNpcByLearnedMemory(request.mapName(), request.mapX(), request.mapY(),
                    request.npcName(), playerLocation, request.expectedDialogTemplatePath());
            recordSmartClickEvidence(request, learnedResult, playerLocation);
            if (learnedResult.verified()) {
                result = true;
                return true;
            }
            addCtrlProbeOrigin(ctrlProbeOrigins, learnedResult.clickPointAbs(),
                    "learned-memory", CtrlProbeScanProfile.SMALL_RING);

            /*
             * 2. Visible tooltip path: only targets that can show the standard task tooltip should
             * try this. Fixed transfer NPCs such as 张闻 do not have that tooltip; probing it only
             * delays the formula path that is already calibrated for them.
             */
            NpcClickStrategyResult tooltipResult = clickNpcByTaskTooltipTemplate(request, targetScanRegions);
            recordSmartClickEvidence(request, tooltipResult, playerLocation);
            if (tooltipResult.verified()) {
                result = true;
                return true;
            }

            /*
             * 3. Use the historical player-anchor formula before paying for yellow-name OCR. A
             * roaming request still carries the current task coordinate; for Xiuluo this coordinate
             * is refreshed from the task panel after reaching the target map, so it remains useful
             * as a fixed point until a future roaming-specific strategy is introduced.
             */
            FormulaClickPrediction formulaPrediction = targetScanRegions.isEmpty()
                    ? null
                    : calculatePlayerAnchorFormulaPoint(
                    request.player(), request.mapName(), request.mapX(), request.mapY(),
                    request.npcName(), request.tuneX(), request.tuneY(), targetScanRegions.get(0), playerLocation);
            NpcClickStrategyResult formulaResult = clickNpcByPlayerAnchorFormula(
                    formulaPrediction, request.expectedDialogTemplatePath());
            recordSmartClickEvidence(request, formulaResult, playerLocation);
            if (formulaResult.verified()) {
                result = true;
                return true;
            }
            /*
             * If the player-anchor formula lands near the target but misses the direct left click,
             * pay for a very small Ctrl probe immediately. Recent Xiuluo logs showed this fixes the
             * common "off by a few pixels" case faster than running broad yellow-name OCR first.
             */
            if (formulaPrediction != null && formulaPrediction.predictedClickAbs() != null) {
                List<CtrlProbeOrigin> formulaCtrlOrigins = new ArrayList<>();
                addCtrlProbeOrigin(formulaCtrlOrigins, formulaPrediction.predictedClickAbs(),
                        "formula-target:immediate", CtrlProbeScanProfile.SMALL_RING);
                NpcClickStrategyResult formulaCtrlResult = clickNpcByCtrlMenuScan(request.npcName(), NPC_TAG_TEMPLATE_PATH,
                        request.expectedDialogTemplatePath(), formulaCtrlOrigins, false);
                recordSmartClickEvidence(request, formulaCtrlResult, playerLocation);
                if (formulaCtrlResult.verified()) {
                    result = true;
                    return true;
                }
            }

            /*
             * 4. Yellow-name visual path: search vision-memory recommended regions in order. Region
             * expansion is only allowed when the smaller region does not contain the target text. If
             * the target text is found but the resulting click does not verify the dialog, a larger
             * region would only add noise and may click another candidate, so the service moves to the
             * Ctrl fallback instead.
             */
            for (int i = 0; i < targetScanRegions.size(); i++) {
                ResolvedNpcClickRegion region = targetScanRegions.get(i);
                log.info("NPC yellow target strategy region {}/{}: {}",
                        i + 1, targetScanRegions.size(), region.toShortText());
                YellowTargetClickResult yellowResult = clickNpcByYellowTargetName(
                        request, region);
                recordSmartClickEvidence(request, yellowResult.evidence(), playerLocation);
                if (yellowResult.status() == YellowTargetClickStatus.CLICK_VERIFIED) {
                    result = true;
                    return true;
                }
                addCtrlProbeOrigin(ctrlProbeOrigins, yellowResult.attemptedClickPointAbs(),
                        "yellow-target:" + yellowResult.status(), CtrlProbeScanProfile.SMALL_RING);
                addCtrlProbeOrigins(ctrlProbeOrigins, yellowResult.ctrlProbePointsAbs(),
                        "yellow-candidate:" + yellowResult.status(), CtrlProbeScanProfile.DIRECT);
                if (!yellowResult.allowsRegionExpansion()) {
                    log.info("NPC yellow target region expansion stopped: npcName={} result={} region={}",
                            request.npcName(), yellowResult.status(), region.toShortText());
                    break;
                }
            }

            /*
             * 5. Last resort: hold Ctrl and inspect the game's nearby-NPC menu. This requires real input
             * inside one exclusive transaction, so it stays after the cheaper screenshot/OCR attempts.
             * This consumes every credible physical point produced above, regardless of which strategy
             * produced it; yellow OCR, learned memory, formula, and future visual candidates all share
             * the same fallback queue.
             */
            NpcClickStrategyResult ctrlResult = clickNpcByCtrlMenuScan(request.npcName(), NPC_TAG_TEMPLATE_PATH,
                    request.expectedDialogTemplatePath(), ctrlProbeOrigins);
            recordSmartClickEvidence(request, ctrlResult, playerLocation);
            if (ctrlResult.verified()) {
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
                    "result=" + result + " target=" + target);
        }
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
            List<ResolvedNpcClickRegion> targetScanRegions) {
        if (request.tooltipType() == NpcTooltipType.NONE) {
            log.info("NPC task-tooltip template skipped: npcName={} tooltipType={}",
                    request.npcName(), request.tooltipType());
            return NpcClickStrategyResult.skipped(
                    NpcClickStrategySource.TASK_TOOLTIP_TEMPLATE,
                    "tooltip-disabled");
        }
        if (targetScanRegions == null || targetScanRegions.isEmpty()) {
            log.info("NPC task-tooltip template skipped: npcName={} reason=no-recommended-regions", request.npcName());
            return NpcClickStrategyResult.skipped(
                    NpcClickStrategySource.TASK_TOOLTIP_TEMPLATE,
                    "no-recommended-regions");
        }

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
            Point matchedPoint = coordinateHelper.findImageInRegion(
                    NPC_TASK_TOOLTIP_TEMPLATE_PATH, rect, NPC_TASK_TOOLTIP_MATCH_RATE);
            log.info("NPC task-tooltip template region {}/{}: npcName={} region={} rect=[{},{},{},{}] match={}",
                    i + 1, targetScanRegions.size(), request.npcName(), region.toShortText(),
                    rect[0], rect[1], rect[2], rect[3], matchedPoint);
            if (matchedPoint == null) {
                continue;
            }

            /*
             * A template hit is a strong visual signal, but we still verify the business dialog after
             * clicking. If the click does not open the expected dialog, later strategies still get a
             * chance to recover through yellow OCR, formula, and finally Ctrl.
             */
            boolean verified = executeMoveClickAndVerify(
                    "npcClick:taskTooltipTemplate",
                    matchedPoint.x,
                    matchedPoint.y,
                    1200,
                    0,
                    request.expectedDialogTemplatePath());
            log.info("NPC task-tooltip template click result: npcName={} point=({}, {}) verified={}",
                    request.npcName(), matchedPoint.x, matchedPoint.y, verified);
            return NpcClickStrategyResult.fromClick(
                    NpcClickStrategySource.TASK_TOOLTIP_TEMPLATE,
                    verified ? NpcClickStrategyStatus.VERIFIED : NpcClickStrategyStatus.CLICK_NOT_VERIFIED,
                    region.windowRegion(),
                    templateMatchedRegion(matchedPoint, windowBase(region), NPC_TASK_TOOLTIP_TEMPLATE_PATH),
                    directNpcPointFromTooltipCenter(matchedPoint),
                    windowRelativePoint(directNpcPointFromTooltipCenter(matchedPoint), windowBase(region)),
                    true,
                    verified,
                    verified ? "task-tooltip template verified; recordPoint=tooltipCenterY+90"
                            : "task-tooltip clicked but expected dialog not verified; recordPoint=tooltipCenterY+90");
        }
        return NpcClickStrategyResult.notFound(
                NpcClickStrategySource.TASK_TOOLTIP_TEMPLATE,
                "task-tooltip template not found");
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
        if (playerLocation == null) {
            log.warn("[vision-memory] smart-click evidence skipped because player coordinate is missing: source={} npc={} target=({}, {}) message={}",
                    result.source(), request.npcName(), request.mapX(), request.mapY(), result.message());
            return;
        }
        WindowBase windowBase = currentWindowBase("smart-click-evidence");
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
        log.info("[vision-memory] smart-click evidence gate: source={} status={} npc={} target=({}, {}) "
                        + "clicked={} verified={} clickSample={} roiEvidence={} clickPointRel={} scanRegion={} message={}",
                result.source(), result.status(), request.npcName(), request.mapX(), request.mapY(),
                result.clicked(), result.verified(), shouldRecordClickSample,
                result.source() != NpcClickStrategySource.CTRL_MENU
                        && result.scanRegion() != null
                        && (result.matchedRect() != null
                        || result.clickPointRel() != null
                        || result.source() == NpcClickStrategySource.YELLOW_TARGET_OCR),
                result.clickPointRel(),
                result.scanRegion() == null ? "-" : result.scanRegion().toShortText(),
                result.message());
        if (shouldRecordClickSample) {
            try {
                ocrRoiMemoryService.recordNpcClickAttempt(
                        result.source().memorySource(),
                        request.mapName(),
                        playerLocation.x,
                        playerLocation.y,
                        request.npcName(),
                        request.mapX(),
                        request.mapY(),
                        new Point(windowBase.x(), windowBase.y()),
                        null,
                        result.clickPointAbs(),
                        result.clickPointAbs(),
                        request.tuneX(),
                        request.tuneY(),
                        "npc-smart-click:" + result.source().memorySource(),
                        true,
                        result.verified(),
                        result.status().name(),
                        result.message(),
                        result.source().actualClickMeasured(),
                        result.source().memorySource(),
                        result.verified() ? "DIALOG_TEMPLATE" : "NONE");
            } catch (Exception e) {
                log.warn("[vision-memory] record smart NPC click attempt failed: source={} npc={} target=({}, {}) reason={}",
                        result.source(), request.npcName(), request.mapX(), request.mapY(), e.getMessage(), e);
            }
        }

        /*
         * ROI policy learning is about where scene-level visual cues live. Do not feed Ctrl-menu
         * text into this stream because its rectangle belongs to the popup menu, not the in-scene
         * NPC/monster label. Yellow OCR misses are still useful here: repeated misses can mark the
         * current ROI stale without poisoning learned direct-click samples above.
         */
        boolean shouldRecordRoiEvidence =
                result.source() != NpcClickStrategySource.CTRL_MENU
                        && result.scanRegion() != null
                        && (result.matchedRect() != null
                        || result.clickPointRel() != null
                        || result.source() == NpcClickStrategySource.YELLOW_TARGET_OCR);
        if (shouldRecordRoiEvidence) {
            try {
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
                        result.verified(),
                        result.source().memorySource(),
                        result.message());
            } catch (Exception e) {
                log.warn("[vision-memory] record smart NPC ROI evidence failed: source={} npc={} region={} reason={}",
                        result.source(), request.npcName(), result.scanRegion().toShortText(), e.getMessage(), e);
            }
        }
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
    private Point directNpcPointFromTooltipCenter(Point tooltipCenterAbs) {
        if (tooltipCenterAbs == null) {
            return null;
        }
        return new Point(tooltipCenterAbs.x, tooltipCenterAbs.y + 90);
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
     * Reconstruct the window-relative rectangle of a template match from its center point.
     *
     * <p>{@link CoordinateHelper#findImageInRegion(String, int[], double)} returns only the
     * screen-absolute center. For learning, the ROI policy wants the matched cue rectangle. Reading
     * the template size here is cheap and keeps the strategy result self-contained.</p>
     *
     * @param centerAbs screen-absolute template center returned by the matcher.
     * @param windowBase screen-absolute game-window origin.
     * @param templatePath template image path used for the match.
     * @return window-relative matched rectangle, or null when the template cannot be read.
     */
    private OcrWindowRegion templateMatchedRegion(Point centerAbs, WindowBase windowBase, String templatePath) {
        if (centerAbs == null || windowBase == null || templatePath == null || templatePath.isBlank()) {
            return null;
        }
        try {
            BufferedImage template = ImageIO.read(Path.of(templatePath).toFile());
            if (template == null) {
                return null;
            }
            try {
                int relCenterX = centerAbs.x - windowBase.x();
                int relCenterY = centerAbs.y - windowBase.y();
                int halfW = Math.max(1, template.getWidth()) / 2;
                int halfH = Math.max(1, template.getHeight()) / 2;
                return new OcrWindowRegion(
                        relCenterX - halfW,
                        relCenterY - halfH,
                        relCenterX - halfW + template.getWidth(),
                        relCenterY - halfH + template.getHeight())
                        .clamp(WINDOW_WIDTH, WINDOW_HEIGHT);
            } finally {
                template.flush();
            }
        } catch (IOException e) {
            log.warn("NPC template matched-region reconstruction failed: template={} reason={}",
                    templatePath, e.getMessage(), e);
            return null;
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

        String message;

        static NpcClickStrategyResult skipped(NpcClickStrategySource source, String message) {
            return new NpcClickStrategyResult(source, NpcClickStrategyStatus.SKIPPED,
                    null, null, null, null, false, false, false, message);
        }

        static NpcClickStrategyResult notFound(NpcClickStrategySource source, String message) {
            return new NpcClickStrategyResult(source, NpcClickStrategyStatus.NOT_FOUND,
                    null, null, null, null, false, false, false, message);
        }

        static NpcClickStrategyResult notFound(NpcClickStrategySource source,
                                               OcrWindowRegion scanRegion,
                                               String message) {
            return new NpcClickStrategyResult(source, NpcClickStrategyStatus.NOT_FOUND,
                    scanRegion, null, null, null, false, false, false, message);
        }

        static NpcClickStrategyResult failed(NpcClickStrategySource source, String message) {
            return new NpcClickStrategyResult(source, NpcClickStrategyStatus.FAILED,
                    null, null, null, null, false, false, false, message);
        }

        static NpcClickStrategyResult failed(NpcClickStrategySource source,
                                             OcrWindowRegion scanRegion,
                                             String message) {
            return new NpcClickStrategyResult(source, NpcClickStrategyStatus.FAILED,
                    scanRegion, null, null, null, false, false, false, message);
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
            return new NpcClickStrategyResult(source, status, scanRegion, matchedRect,
                    clickPointAbs, clickPointRel, true, clicked, verified, message);
        }

        boolean attempted() {
            return status != NpcClickStrategyStatus.SKIPPED;
        }
    

    }

    /**
     * Debug-only Ctrl-menu probe that bypasses yellow OCR, learned-memory, and formula clicks.
     *
     * <p>This method sends real mouse/Ctrl input through the normal exclusive input queue, writes
     * the same window-scoped {@code npc_menu_*} diagnostic images as production, and verifies the
     * expected green-option dialog template after a menu candidate click. The supplied points are
     * screen-absolute coordinates, not window-relative coordinates. It is intentionally not called
     * from any production task path; use it from local debug mains to isolate whether Ctrl probing
     * itself is valid for a chosen origin.</p>
     *
     * @param targetKeyword NPC or monster name expected in the Ctrl menu.
     * @param expectedDialogTemplatePath green-option template proving the target dialog opened.
     * @param screenAbsoluteProbePoints ordered screen-absolute Ctrl origins to test.
     * @param includeWindowCenterFallback whether to prepend the generic window-center fallback.
     * @return true when one supplied Ctrl origin opens and verifies the expected dialog.
     */
    public boolean debugClickNpcCtrlMenuAtPoints(String targetKeyword,
                                                 String expectedDialogTemplatePath,
                                                 List<Point> screenAbsoluteProbePoints,
                                                 boolean includeWindowCenterFallback) {
        List<CtrlProbeOrigin> origins = new ArrayList<>();
        if (screenAbsoluteProbePoints != null) {
            for (int i = 0; i < screenAbsoluteProbePoints.size(); i++) {
                Point point = screenAbsoluteProbePoints.get(i);
                addCtrlProbeOrigin(origins, point, "debug-point#" + (i + 1), CtrlProbeScanProfile.DIRECT);
            }
        }
        log.info("NPC ctrl debug requested: keyword={} includeWindowCenter={} points={}",
                targetKeyword, includeWindowCenterFallback, screenAbsoluteProbePoints);
        return clickNpcByCtrlMenuScan(targetKeyword, NPC_TAG_TEMPLATE_PATH,
                expectedDialogTemplatePath, origins, includeWindowCenterFallback).verified();
    }

    private OcrWindowRegion normalizeNpcScanRegion(OcrWindowRegion region) {
        if (region == null) {
            return null;
        }
        OcrWindowRegion normalized = region;
        normalized = normalized.clamp(WINDOW_WIDTH, WINDOW_HEIGHT);
        return normalized.isValid() ? normalized : null;
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
     *         stands beside the monster. The center fallback is appended only as the final broad scan.
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
                                                           String expectedDialogTemplatePath) {
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
                clickX, clickY, 1200, 0, expectedDialogTemplatePath);
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
     * @return detailed result. Only {@link YellowTargetClickStatus#TARGET_NOT_FOUND} permits the
     * next larger region to be searched; other failures mean the target path was attempted and
     * should fall through to coordinate/Ctrl strategies.
     */
    private YellowTargetClickResult clickNpcByYellowTargetName(
            NpcClickRequest request,
            ResolvedNpcClickRegion scanRegion) {
        if (shouldStop()) return YellowTargetClickResult.scanFailed();
        OcrWindowRegion targetScanRegion = scanRegion == null ? null : scanRegion.windowRegion();
        String outputPath = windowScopedTempPath.resolve("npc_yellow_target.png");
        String npcName = request.npcName();
        if (npcName == null || npcName.isBlank()) {
            log.info("NPC yellow target scan skipped: npcName is blank");
            return YellowTargetClickResult.scanFailed();
        }

        BufferedImage raw = captureCleanNameRegionToMemory("NPC yellow target scan", scanRegion);
        if (raw == null) {
            log.warn("NPC yellow target scan capture failed: npcName={} region={}",
                    npcName, targetScanRegion.toShortText());
            return YellowTargetClickResult.scanFailed();
        }
        BufferedImage scanImage = prepareNpcOcrScanImage(raw, targetScanRegion, "yellow target");
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
                Point targetInScan = centerOfWords(result.lineResult().words());
                if (targetInScan == null) {
                    return YellowTargetClickResult.scanFailed(
                            targetScanRegion, "yellow target center unavailable text=" + result.normalizedText());
                }
                OcrWindowRegion textRect = windowRegionOfWords(result.lineResult().words(), targetScanRegion);
                WindowBase windowBase = currentWindowBase("yellow-target-click");
                int clickX = windowBase.x() + targetScanRegion.x1() + targetInScan.x;
                int clickY = windowBase.y() + targetScanRegion.y1() + targetInScan.y - 50;
                Point textCenterAbs = new Point(
                        windowBase.x() + targetScanRegion.x1() + targetInScan.x,
                        windowBase.y() + targetScanRegion.y1() + targetInScan.y);
                Point clickPointRel = new Point(clickX - windowBase.x(), clickY - windowBase.y());
                log.info("NPC yellow target matched: npcName={} click=({}, {}) targetInScan=({}, {})",
                        npcName, clickX, clickY, targetInScan.x, targetInScan.y);
                /*
                 * We already have a concrete yellow-name candidate here. A short same-point retry is
                 * cheaper and safer than widening the scan region, because the first miss can simply be
                 * a delayed dialog/network response while a wider region may introduce another NPC.
                 */
                boolean verified = executeMoveClickAndVerify("npcClick:yellowTargetMoveClick",
                        clickX, clickY, 2000, YELLOW_TARGET_CLICK_RETRIES, request.expectedDialogTemplatePath());
                return new YellowTargetClickResult(
                        verified ? YellowTargetClickStatus.CLICK_VERIFIED : YellowTargetClickStatus.CLICK_NOT_VERIFIED,
                        new Point(clickX, clickY),
                        List.of(),
                        verified ? List.of() : List.of(textCenterAbs),
                        NpcClickStrategyResult.fromClick(
                                NpcClickStrategySource.YELLOW_TARGET_OCR,
                                verified ? NpcClickStrategyStatus.VERIFIED : NpcClickStrategyStatus.CLICK_NOT_VERIFIED,
                                targetScanRegion,
                                textRect,
                                new Point(clickX, clickY),
                                clickPointRel,
                                true,
                                verified,
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
        if (raw == null) {
            return null;
        }
        if (!OcrWindowScanService.isDefaultMaskedWindowRegion(scanRegion)) {
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

    /**
     * Yellow-name strategy result plus physical evidence for later Ctrl probing.
     *
     * @param status semantic outcome of the yellow-name path.
     * @param attemptedClickPointAbs screen-absolute point used by the yellow strategy, or null when
     *                               no concrete candidate was available. Non-null failed attempts
     *                               are reused as Ctrl probe origins.
     * @param fallbackCandidates ranked screen-absolute visual candidates from shape scanning. They
     *                           are used only as Ctrl-menu probe origins when target OCR did not match.
     * @param exactMatchCtrlProbePointsAbs screen-absolute hover points from an exact yellow-name
     *                                     match. These deliberately use the text center, not the
     *                                     direct-click point, because Ctrl menu hover needs to start
     *                                     on/near the visible name while direct click uses a
     *                                     different NPC-body offset.
     */
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

    /**
     * Ranked yellow-text visual candidate converted to screen-absolute coordinates.
     *
     * @param textCenterAbs screen-absolute center of the candidate text rectangle.
     * @param clickPointAbs screen-absolute point to use as a Ctrl-menu probe origin; this already
     *                      includes the candidate extractor's click offset from the text box.
     * @param textRectAbs screen-absolute rectangle around the visual text candidate.
     * @param score shape score from {@link GameTextLineOcrService}; higher ranks earlier.
     * @param sourceText optional recognized text. Shape-only candidates currently leave this blank.
     * @param reason compact diagnostic reason including geometry and penalty metrics.
     */
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
                                                                    LocationInfo cachedPlayerLocation) {
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

        BufferedImage rawPlayerAnchor = captureCleanNameRegionToMemory("NPC first-shot player anchor raw", scanRegion);
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
                rawPlayerAnchor, targetScanRegion, "purple player-anchor");
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
                                                                 String expectedDialogTemplatePath) {
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
        boolean firstShotOk = executeMoveClickAndVerify("npcClick:firstShotMoveClick",
                target.x, target.y, 1500, 0, expectedDialogTemplatePath);
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

    private boolean hasWordContaining(List<OcrWordResult> words, String keyword) {
        if (words == null || words.isEmpty() || keyword == null || keyword.isBlank()) {
            return false;
        }
        for (OcrWordResult word : words) {
            if (word != null && word.getText() != null && word.getText().contains(keyword)) {
                return true;
            }
        }
        return false;
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

    private boolean canExtractPlayerAnchor(List<OcrWordResult> words,
                                           PlayerCharacter player,
                                           int scanStartX,
                                           int scanStartY) {
        return extractPlayerAnchorMatchFromWords(words, player, scanStartX, scanStartY) != null;
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
     * Record a vision-memory NPC click sample without making it authoritative ground truth.
     *
     * <p>Normal task runs know the predicted click point and whether verification succeeded; they do
     * not independently measure the true NPC point. The memory entry therefore marks
     * actualClickMeasured=false so later learning code can filter samples safely.</p>
     */
    private void recordNpcClickMemory(String source,
                                      String targetMapName,
                                      LocationInfo locInfo,
                                      String npcName,
                                      int targetMapX,
                                      int targetMapY,
                                      Point windowBase,
                                      Point playerAnchorAbs,
                                      Point predictedClickAbs,
                                      Point actualClickAbs,
                                      int tuneX,
                                      int tuneY,
                                      boolean clicked,
                                      boolean success,
                                      String outcome,
                                      String verification) {
        try {
            String sampleMapName = locInfo != null && locInfo.mapName != null && !locInfo.mapName.isBlank()
                    ? locInfo.mapName
                    : targetMapName;
            ocrRoiMemoryService.recordNpcClickAttempt(
                    source,
                    sampleMapName,
                    locInfo == null ? null : locInfo.x,
                    locInfo == null ? null : locInfo.y,
                    npcName,
                    targetMapX,
                    targetMapY,
                    windowBase,
                    playerAnchorAbs,
                    predictedClickAbs,
                    actualClickAbs,
                    tuneX,
                    tuneY,
                    "npc-first-shot-v1:playerAnchor+mapDelta20px+tuneY-50",
                    clicked,
                    success,
                    outcome,
                    verification,
                    false,
                    clicked ? "predicted-click-point" : "not-clicked",
                    success ? "DIALOG_OPTION" : "NONE");
        } catch (Exception e) {
            log.warn("[vision-memory] record NPC click attempt failed: source={} npc={} target=({}, {}) reason={}",
                    source, npcName, targetMapX, targetMapY, e.getMessage(), e);
        }
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

    private boolean captureCleanNameToFile(String elementName, String savePath, int x1, int y1, int x2, int y2) {
        return inputSequences.submitExclusiveAndWait("npcClick:cleanNameCapture:" + elementName, () -> {
            return captureCleanNameToFileDirect(elementName, savePath, x1, y1, x2, y2, true);
        });
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

    /**
     * Capture a resolved name/OCR region after pressing Alt+4.
     *
     * <p>The whole preparation and capture runs as one exclusive input callback because Alt+4 changes
     * the visible name layer for the bound window. The region has already been resolved to
     * screen-absolute bounds by {@link #resolveNpcScanRegions(NpcClickRequest)}, so OCR callers do not
     * repeat window-base conversions.</p>
     */
    private BufferedImage captureCleanNameRegionToMemory(String elementName, ResolvedNpcClickRegion region) {
        AtomicReference<BufferedImage> imageRef = new AtomicReference<>();
        boolean ok = inputSequences.submitExclusiveAndWait("npcClick:cleanNameMemoryCapture:" + elementName, () -> {
            if (shouldStop()) {
                return false;
            }
            if (region == null) {
                return false;
            }
            inputProvider.pressAlt4();
            if (!TaskSleep.sleep(400)) {
                return false;
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

    /**
     * Result of converting an in-game target coordinate into a screen-absolute click point.
     *
     * <p>The same prediction is reused by the first-shot click and by the Ctrl-menu fallback probe,
     * so Ctrl starts near the expected target instead of falling back to the visual window center.</p>
     *
     * @param targetMapName map name supplied by the task.
     * @param locInfo current map/coordinate read while calculating the formula.
     * @param npcName target name used for logging and memory.
     * @param targetMapX target logical X coordinate.
     * @param targetMapY target logical Y coordinate.
     * @param windowBaseAbs screen-absolute top-left of the bound game window.
     * @param playerAnchorAbs screen-absolute player anchor detected from purple-name OCR.
     * @param predictedClickAbs screen-absolute click/probe point predicted by the formula.
     * @param tuneX screen-pixel X correction applied to the prediction.
     * @param tuneY screen-pixel Y correction applied to the prediction.
     */
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

    /**
     * Screen-absolute top-left point of the currently targeted game window.
     *
     * @param x screen-absolute X coordinate.
     * @param y screen-absolute Y coordinate.
     */
    @Value

    @Builder

    @AllArgsConstructor(access = AccessLevel.PUBLIC)

    @Accessors(fluent = true)

    private static class WindowBase {

        int x;

        int y;

    }
}
