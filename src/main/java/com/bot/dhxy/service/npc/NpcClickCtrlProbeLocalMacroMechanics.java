package com.bot.dhxy.service.npc;

import com.bot.dhxy.core.ImageFinder;
import com.bot.dhxy.core.TextRecognizer;
import com.bot.dhxy.driver.BoundWindowCaptureService;
import com.bot.dhxy.input.InputProvider;
import com.bot.dhxy.model.dialog.DialogType;
import com.bot.dhxy.model.ocr.OcrWordResult;
import com.bot.dhxy.runner.stop.TaskSleep;
import com.bot.dhxy.service.battleradar.BattleRadarLocalObservationMechanics;
import com.bot.dhxy.service.dialog.DialogDetectionLocalMechanics;
import com.bot.dhxy.tools.ImagePreprocessor;
import com.bot.dhxy.vision.OcrTextMatcher;
import com.bot.dhxy.window.model.WindowNativeBinding;
import com.bot.dhxy.window.runtime.WindowScopedTempPath;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * The single continuous local macro between Ctrl press and release, extracted from {@code 696a12b0}
 * {@code NpcClickService.clickNpcByCtrlMenuScan}'s exclusive-input callback plus its
 * {@code scanMenuAndVerifyKeywordDirect} keyword segment.
 *
 * <p>This is the part that can never cross the network. It captures a before frame against one exact
 * caller-supplied {@link WindowNativeBinding}, holds Ctrl, sleeps, moves the real mouse, captures the
 * after frame against the same binding, decides whether the Ctrl menu appeared, locally binarizes and
 * OCRs the menu against the {@link TextRecognizer} local sidecar, fuzzy-matches the target keyword, and
 * on the first hit moves, sleeps {@code 100ms}, left-clicks and verifies, then always releases Ctrl.</p>
 *
 * <p>It runs on the exclusive {@code dhxy-input-action-worker} thread and submits no nested input. The
 * caller keeps ownership of probe origin/offset/clamp and screen-absolute scan-rect construction and
 * passes only a closed immutable {@link CtrlProbeIntent}. All screen bases derive from the same
 * {@code binding} geometry; there is no separate window-base authority. Both click verifiers stay fully
 * local and never call {@code DialogService}/{@code BattleRadarService} or any Cloud path during the
 * Ctrl hold: the dialog verifier read-only calls {@link DialogDetectionLocalMechanics} and the combat
 * verifier read-only calls {@link BattleRadarLocalObservationMechanics}. No stop checkpoint, retry, TTL,
 * owner, session or ledger is added; the release-finally and the {@code 80/280/100/800/1000ms} +
 * one-retry cadence and the {@code 4x350ms} combat verify timing are unchanged. Interruption is
 * expressed only through the input-worker thread interrupt flag and the existing {@link TaskSleep}
 * return values; a produced click is never downgraded to a plain not-found.</p>
 */
@Slf4j
@Service
public final class NpcClickCtrlProbeLocalMacroMechanics {

    private static final String INPUT_ACTION_WORKER_THREAD = "dhxy-input-action-worker";
    private static final String NPC_TAG_REGEX = "(?i).*(NPC|IPC|PC|NP).*";
    private static final int NPC_LEFT_CLICK_HOLD_MS = 150;
    private static final double MENU_CHANGE_MATCH_TOLERANCE = 0.05;
    private static final int CTRL_HOLD_SETTLE_MS = 80;
    private static final int CTRL_MOVE_SETTLE_MS = 280;
    private static final int MENU_CLICK_SETTLE_MS = 100;
    private static final int CTRL_RELEASE_SETTLE_MS = 100;
    private static final long CLICK_FIRST_VERIFY_WAIT_MS = 800L;
    private static final int CLICK_MAX_RETRIES = 1;
    private static final long CLICK_RETRY_WAIT_MS = 1000L;
    private static final int COMBAT_VERIFY_ATTEMPTS = 4;
    private static final long COMBAT_VERIFY_INTERVAL_MS = 350L;
    // Baseline DialogService.WHITE_STORY_TEMPLATE_THRESHOLD used by verifyGreenTemplateOption's first-hit.
    private static final double DIALOG_GREEN_TEMPLATE_MATCH_THRESHOLD = 0.85D;

    private final BoundWindowCaptureService boundWindowCaptureService;
    private final InputProvider inputProvider;
    private final TextRecognizer ocr;
    private final DialogDetectionLocalMechanics dialogDetectionLocalMechanics;
    private final BattleRadarLocalObservationMechanics battleRadarLocalObservationMechanics;
    private final WindowScopedTempPath windowScopedTempPath;

    public NpcClickCtrlProbeLocalMacroMechanics(
            BoundWindowCaptureService boundWindowCaptureService,
            InputProvider inputProvider,
            TextRecognizer ocr,
            DialogDetectionLocalMechanics dialogDetectionLocalMechanics,
            BattleRadarLocalObservationMechanics battleRadarLocalObservationMechanics,
            WindowScopedTempPath windowScopedTempPath) {
        this.boundWindowCaptureService = Objects.requireNonNull(boundWindowCaptureService, "boundWindowCaptureService");
        this.inputProvider = Objects.requireNonNull(inputProvider, "inputProvider");
        this.ocr = Objects.requireNonNull(ocr, "ocr");
        this.dialogDetectionLocalMechanics = Objects.requireNonNull(
                dialogDetectionLocalMechanics, "dialogDetectionLocalMechanics");
        this.battleRadarLocalObservationMechanics = Objects.requireNonNull(
                battleRadarLocalObservationMechanics, "battleRadarLocalObservationMechanics");
        this.windowScopedTempPath = Objects.requireNonNull(windowScopedTempPath, "windowScopedTempPath");
    }

    /** The closed local click-verify operation, using only existing local dialog/combat mechanics. */
    public enum VerifierOperation {
        EXPECTED_DIALOG,
        COMBAT
    }

    /**
     * Closed, serializable Ctrl-probe intent: only primitive/String data and an immutable template list.
     * The caller resolves probe origin/offset/clamp and builds the screen-absolute scan rectangle before
     * handing it here.
     *
     * @param testX screen-absolute X where Ctrl is held and the mouse is moved.
     * @param testY screen-absolute Y where Ctrl is held and the mouse is moved.
     * @param scanLeft/scanTop/scanRight/scanBottom screen-absolute scan rectangle.
     * @param targetKeyword closed non-blank target keyword.
     * @param verifierOperation closed local verify operation (dialog / combat).
     * @param dialogTemplatePaths closed expected green-option template paths for the dialog verify.
     */
    public record CtrlProbeIntent(
            int testX,
            int testY,
            int scanLeft,
            int scanTop,
            int scanRight,
            int scanBottom,
            String targetKeyword,
            VerifierOperation verifierOperation,
            List<String> dialogTemplatePaths) {

        public CtrlProbeIntent {
            if (targetKeyword == null || targetKeyword.isBlank()) {
                throw new IllegalArgumentException("targetKeyword must be non-blank");
            }
            Objects.requireNonNull(verifierOperation, "verifierOperation");
            if (scanRight <= scanLeft || scanBottom <= scanTop) {
                throw new IllegalArgumentException("scan rectangle must be a positive-area rect");
            }
            // List.copyOf gives an immutable copy and rejects any null element, so no partial/null
            // template path can enter the closed intent.
            dialogTemplatePaths = dialogTemplatePaths == null ? List.of() : List.copyOf(dialogTemplatePaths);
        }
    }

    /**
     * Terminal for one Ctrl probe. Distinguishes a verified click, a produced-but-unverified click, an
     * absent keyword/menu, an interrupt, an unavailable binding/capture and a mechanics failure. A
     * produced click is never disguised as a plain {@code NOT_FOUND}; interruptions and mechanics
     * failures are never disguised as an absence.
     */
    public enum Status {
        VERIFIED,
        CLICK_NOT_VERIFIED,
        NOT_FOUND,
        INTERRUPTED,
        BINDING_UNAVAILABLE,
        MECHANICS_FAILED
    }

    /**
     * Immutable, closed result of one Ctrl probe.
     *
     * @param status closed terminal status.
     * @param clickProduced true when a real left-click was issued at {@code (clickX, clickY)} before
     *                      this result was produced, preserved across every sleep/interrupt exit.
     * @param clickX/clickY screen-absolute click point, or -1 when no click was issued.
     * @param scanLeft/scanTop/scanRight/scanBottom screen-absolute scan rectangle used.
     * @param reason baseline diagnostic reason string.
     */
    public record CtrlProbeLocalResult(
            Status status,
            boolean clickProduced,
            int clickX,
            int clickY,
            int scanLeft,
            int scanTop,
            int scanRight,
            int scanBottom,
            String reason) {

        public CtrlProbeLocalResult {
            Objects.requireNonNull(status, "status");
        }
    }

    /** Closed outcome of one click-verify pass, mapped by the caller into a {@link Status}. */
    private enum VerifyOutcome {
        VERIFIED,
        NOT_VERIFIED,
        INTERRUPTED,
        MECHANICS_FAILED
    }

    /**
     * Run the continuous Ctrl-hold local macro for one probe point against one exact window binding.
     *
     * <p>Order mirrors the baseline callback: before capture -> hold Ctrl -> sleep 80ms ->
     * screen-absolute move -> sleep 280ms -> after capture -> {@code ImageFinder.isMatch(...,0.05)}
     * change check -> capture/binarize/OCR/fuzzy -> first hit move + sleep 100ms + click + verify ->
     * {@code finally} release Ctrl -> sleep 100ms. Every owned frame is released exactly once through
     * its own {@code finally}.</p>
     *
     * @throws IllegalStateException when not invoked on the exclusive input-action-worker thread.
     */
    public CtrlProbeLocalResult probe(WindowNativeBinding binding, CtrlProbeIntent intent) {
        Objects.requireNonNull(intent, "intent");
        if (!isInputWorkerThread()) {
            throw new IllegalStateException(
                    "npc ctrl probe local mechanics must run on the " + INPUT_ACTION_WORKER_THREAD + " thread");
        }
        if (binding == null || !binding.hasNativeHandle()) {
            return result(Status.BINDING_UNAVAILABLE, false, -1, -1, intent, "ctrl-probe-binding-unavailable");
        }
        if (isInterrupted()) {
            return result(Status.INTERRUPTED, false, -1, -1, intent, "interrupted-inside-ctrl-callback");
        }

        Optional<BoundWindowCaptureService.CaptureResult> before = captureScanRegion(binding, intent);
        if (before.isEmpty() || before.get().image() == null) {
            return result(Status.BINDING_UNAVAILABLE, false, -1, -1, intent, "menu-before-capture-unavailable");
        }
        BufferedImage frameBefore = before.get().image();
        try {
            inputProvider.holdCtrl();
            try {
                /*
                 * Hold Ctrl before moving so the mouse motion itself is the event that creates the Ctrl
                 * menu; moving first can leave a normal tooltip and pressing Ctrl may not refresh hover.
                 */
                if (!TaskSleep.sleep(CTRL_HOLD_SETTLE_MS)) {
                    return result(Status.INTERRUPTED, false, -1, -1, intent, "interrupted-ctrl-hold-settle");
                }
                inputProvider.moveMouse(intent.testX(), intent.testY());
                if (!TaskSleep.sleep(CTRL_MOVE_SETTLE_MS)) {
                    return result(Status.INTERRUPTED, false, -1, -1, intent, "interrupted-ctrl-move-settle");
                }
                if (isInterrupted()) {
                    return result(Status.INTERRUPTED, false, -1, -1, intent, "interrupted-before-menu-after-capture");
                }
                Optional<BoundWindowCaptureService.CaptureResult> after = captureScanRegion(binding, intent);
                if (after.isEmpty() || after.get().image() == null) {
                    return result(Status.BINDING_UNAVAILABLE, false, -1, -1, intent, "menu-after-capture-unavailable");
                }
                BufferedImage frameAfter = after.get().image();
                try {
                    boolean changed = !ImageFinder.isMatch(frameBefore, frameAfter, MENU_CHANGE_MATCH_TOLERANCE);
                    if (!changed) {
                        return result(Status.NOT_FOUND, false, -1, -1, intent, "ctrl menu did not visually change");
                    }
                } finally {
                    frameAfter.flush();
                }
                return scanMenuAndVerifyKeyword(binding, intent);
            } finally {
                inputProvider.releaseCtrl();
                TaskSleep.sleep(CTRL_RELEASE_SETTLE_MS);
            }
        } finally {
            frameBefore.flush();
        }
    }

    private CtrlProbeLocalResult scanMenuAndVerifyKeyword(WindowNativeBinding binding, CtrlProbeIntent intent) {
        String menuScanPath = windowScopedTempPath.resolve("npc_menu_scan.png");
        String cleanPath = windowScopedTempPath.resolve("npc_menu_clean.png");
        // Baseline: one exact-binding raw menu capture to file (base derived from binding). A false
        // capture-to-file is a closed terminal; no stale file is ever washed or OCR'd.
        boolean captured = boundWindowCaptureService.captureRegionToFile(
                binding, binding.getX(), binding.getY(), menuScanPath,
                intent.scanLeft(), intent.scanTop(), intent.scanRight(), intent.scanBottom());
        if (!captured) {
            return result(Status.BINDING_UNAVAILABLE, false, -1, -1, intent, "menu-scan-capture-unavailable");
        }
        if (isInterrupted()) {
            return result(Status.INTERRUPTED, false, -1, -1, intent, "interrupted-before-menu-ocr");
        }
        // Exact baseline 696 pure-local yellow wash (white-on-black yellow mask + OpenCV horizontal-line
        // removal and connected-component cleanup); never the Cloud ImageProcessorService.
        ImagePreprocessor.washYellowText(menuScanPath, cleanPath);

        Optional<List<OcrWordResult>> menuWordsOptional = ocr.getAllTextResultsLocalOnly(cleanPath);
        if (menuWordsOptional.isEmpty()) {
            // Sidecar unavailable is a real mechanical failure, never a faked keyword miss.
            return result(Status.MECHANICS_FAILED, false, -1, -1, intent, "menu-ocr-unavailable");
        }
        for (OcrWordResult w : menuWordsOptional.get()) {
            if (isInterrupted()) {
                return result(Status.INTERRUPTED, false, -1, -1, intent, "interrupted-during-menu-ocr");
            }
            if (w == null) {
                continue;
            }
            String text = w.getText();
            if (text == null) {
                continue;
            }
            boolean isNameMatch = OcrTextMatcher.isShortNameMatch(text, intent.targetKeyword());
            boolean isTagMatch = text.matches(NPC_TAG_REGEX);
            if (!isNameMatch && !isTagMatch) {
                continue;
            }
            int nameScore = OcrTextMatcher.shortNameMatchScore(text, intent.targetKeyword());
            int clickX = intent.scanLeft() + w.getX();
            int clickY = intent.scanTop() + w.getY();
            log.info("NPC keyword menu matched text={} keyword={} nameMatch={} nameScore={} tagMatch={} click=({}, {})",
                    text, intent.targetKeyword(), isNameMatch, nameScore, isTagMatch, clickX, clickY);
            inputProvider.moveMouse(clickX, clickY);
            if (!TaskSleep.sleep(MENU_CLICK_SETTLE_MS)) {
                return result(Status.INTERRUPTED, false, -1, -1, intent, "interrupted-before-menu-click");
            }
            return runClickAndVerify(binding, clickX, clickY, intent, text);
        }
        return result(Status.NOT_FOUND, false, -1, -1, intent, "ctrl menu keyword not found");
    }

    /**
     * Issue the click and verify. Every terminal from here carries {@code clickProduced=true} because
     * the left-click has already been issued; a produced-but-unverified click never becomes NOT_FOUND.
     */
    private CtrlProbeLocalResult runClickAndVerify(
            WindowNativeBinding binding, int clickX, int clickY, CtrlProbeIntent intent, String matchedText) {
        VerifyOutcome outcome = executeClickAndVerify(binding, clickX, clickY, intent);
        return switch (outcome) {
            case VERIFIED -> result(Status.VERIFIED, true, clickX, clickY, intent,
                    "ctrl menu keyword verified text=" + matchedText);
            case NOT_VERIFIED -> result(Status.CLICK_NOT_VERIFIED, true, clickX, clickY, intent,
                    "ctrl menu keyword click not verified text=" + matchedText);
            case INTERRUPTED -> result(Status.INTERRUPTED, true, clickX, clickY, intent,
                    "interrupted after produced click text=" + matchedText);
            case MECHANICS_FAILED -> result(Status.MECHANICS_FAILED, true, clickX, clickY, intent,
                    "verify mechanics failed after produced click text=" + matchedText);
        };
    }

    private VerifyOutcome executeClickAndVerify(WindowNativeBinding binding, int x, int y, CtrlProbeIntent intent) {
        inputProvider.clickLeft(x, y, NPC_LEFT_CLICK_HOLD_MS);
        if (!TaskSleep.sleep(CLICK_FIRST_VERIFY_WAIT_MS)) {
            return VerifyOutcome.INTERRUPTED;
        }
        if (isInterrupted()) {
            return VerifyOutcome.INTERRUPTED;
        }
        VerifyOutcome first = verify(binding, intent, "firstVerify");
        if (first != VerifyOutcome.NOT_VERIFIED) {
            return first;
        }
        for (int i = 1; i <= CLICK_MAX_RETRIES; i++) {
            if (isInterrupted()) {
                return VerifyOutcome.INTERRUPTED;
            }
            log.warn("NPC direct click retry {}", i);
            inputProvider.clickLeft(x, y, NPC_LEFT_CLICK_HOLD_MS);
            if (!TaskSleep.sleep(CLICK_RETRY_WAIT_MS)) {
                return VerifyOutcome.INTERRUPTED;
            }
            if (isInterrupted()) {
                return VerifyOutcome.INTERRUPTED;
            }
            VerifyOutcome retry = verify(binding, intent, "retryVerify:" + i);
            if (retry != VerifyOutcome.NOT_VERIFIED) {
                return retry;
            }
        }
        return VerifyOutcome.NOT_VERIFIED;
    }

    private VerifyOutcome verify(WindowNativeBinding binding, CtrlProbeIntent intent, String reason) {
        return switch (intent.verifierOperation()) {
            case EXPECTED_DIALOG -> verifyExpectedDialog(binding, intent.dialogTemplatePaths(), reason);
            case COMBAT -> verifyCombatVisible(binding, reason);
        };
    }

    /**
     * Read-only single-frame expected-option verify through {@link DialogDetectionLocalMechanics},
     * matching the baseline {@code verifyExpectedOptionDialog} exactly. The detection type must be
     * {@code OPTION}. With no caller template it is the baseline generic {@code VERIFY_OPTION}
     * (OPTION_VISIBLE). With a non-empty template list it is the baseline {@code VERIFY_GREEN_TEMPLATE}:
     * the captured frame is washed with the pure-local {@code washDialogOptionTemplateTextToBlackAndWhite}
     * and templates are matched in the caller's order at {@code 0.85}, only a real first-hit verifies.
     */
    private VerifyOutcome verifyExpectedDialog(WindowNativeBinding binding, List<String> templates, String reason) {
        DialogDetectionLocalMechanics.DialogDetectionResult detection =
                dialogDetectionLocalMechanics.detectDialog(binding, false, 0L, "npc-click:expected-dialog:" + reason);
        return switch (detection.state()) {
            case CAPTURED -> {
                if (detection.dialogType() != DialogType.OPTION) {
                    yield VerifyOutcome.NOT_VERIFIED;
                }
                if (templates.isEmpty()) {
                    yield VerifyOutcome.VERIFIED;
                }
                yield anyExpectedGreenTemplateVisible(detection.framePngBytes(), templates)
                        ? VerifyOutcome.VERIFIED
                        : VerifyOutcome.NOT_VERIFIED;
            }
            case CAPTURE_UNAVAILABLE -> VerifyOutcome.NOT_VERIFIED;
            case PRE_CAPTURE_INTERRUPTED -> VerifyOutcome.INTERRUPTED;
            case NON_INPUT_WORKER, MECHANICS_FAILED -> VerifyOutcome.MECHANICS_FAILED;
        };
    }

    /**
     * Baseline {@code verifyGreenTemplateOption} first-hit: wash the captured option frame with the
     * pure-local dialog-option template wash, then match each caller template in order at {@code 0.85};
     * the first real hit verifies. All owned images are flushed exactly once.
     */
    private boolean anyExpectedGreenTemplateVisible(byte[] frameBytes, List<String> templates) {
        if (frameBytes == null || templates == null || templates.isEmpty()) {
            return false;
        }
        BufferedImage frame;
        try {
            frame = ImageIO.read(new ByteArrayInputStream(frameBytes));
        } catch (IOException e) {
            log.warn("npc expected-dialog frame decode failed: reason={}", e.getMessage(), e);
            return false;
        }
        if (frame == null) {
            return false;
        }
        try {
            BufferedImage washed = ImagePreprocessor.washDialogOptionTemplateTextToBlackAndWhite(frame);
            if (washed == null) {
                return false;
            }
            try {
                for (String templatePath : templates) {
                    if (templatePath == null || templatePath.isBlank()) {
                        continue;
                    }
                    BufferedImage template;
                    try {
                        template = ImageIO.read(new File(templatePath));
                    } catch (IOException e) {
                        log.warn("npc expected-dialog template read failed: path={} reason={}",
                                templatePath, e.getMessage());
                        continue;
                    }
                    if (template == null) {
                        continue;
                    }
                    try {
                        if (ImageFinder.find(washed, template, DIALOG_GREEN_TEMPLATE_MATCH_THRESHOLD) != null) {
                            return true;
                        }
                    } finally {
                        template.flush();
                    }
                }
                return false;
            } finally {
                washed.flush();
            }
        } finally {
            frame.flush();
        }
    }

    /**
     * Read-only combat verify through {@link BattleRadarLocalObservationMechanics}, preserving the
     * baseline {@code 4x350ms} timing and the auto-flag -> selection -> top short-circuit order. No
     * candidate selection, retry or business fallback is added; a capture/mechanics failure of one
     * signal simply means "not visible this attempt".
     */
    private VerifyOutcome verifyCombatVisible(WindowNativeBinding binding, String reason) {
        for (int i = 1; i <= COMBAT_VERIFY_ATTEMPTS; i++) {
            if (isInterrupted()) {
                return VerifyOutcome.INTERRUPTED;
            }
            boolean inCombat = isCombatSignalVisible(binding);
            log.info("NPC direct-combat verify: reason={} attempt={} inCombat={}", reason, i, inCombat);
            if (inCombat) {
                return VerifyOutcome.VERIFIED;
            }
            if (!TaskSleep.sleep(COMBAT_VERIFY_INTERVAL_MS)) {
                return VerifyOutcome.INTERRUPTED;
            }
        }
        return VerifyOutcome.NOT_VERIFIED;
    }

    private boolean isCombatSignalVisible(WindowNativeBinding binding) {
        // Baseline stage order with real OR short-circuit: auto-combat flag, then selection buttons
        // (zhaohuan OR chehui), then top icons (nu AND yuan). Each is the exact-binding local fact.
        if (battleRadarLocalObservationMechanics.observeAutoFlag(binding).status()
                == BattleRadarLocalObservationMechanics.SignalStatus.VISIBLE) {
            return true;
        }
        if (battleRadarLocalObservationMechanics.observeSelectionSignal(binding).status()
                == BattleRadarLocalObservationMechanics.SignalStatus.VISIBLE) {
            return true;
        }
        return battleRadarLocalObservationMechanics.observeTopSignal(binding).status()
                == BattleRadarLocalObservationMechanics.SignalStatus.VISIBLE;
    }

    private Optional<BoundWindowCaptureService.CaptureResult> captureScanRegion(
            WindowNativeBinding binding, CtrlProbeIntent intent) {
        // All screen bases derive from the same binding geometry; there is no separate window base.
        return boundWindowCaptureService.captureRegion(
                binding, binding.getX(), binding.getY(),
                intent.scanLeft(), intent.scanTop(), intent.scanRight(), intent.scanBottom());
    }

    private static boolean isInputWorkerThread() {
        return Thread.currentThread().getName().contains(INPUT_ACTION_WORKER_THREAD);
    }

    private static boolean isInterrupted() {
        return Thread.currentThread().isInterrupted();
    }

    private CtrlProbeLocalResult result(
            Status status, boolean clickProduced, int clickX, int clickY, CtrlProbeIntent intent, String reason) {
        return new CtrlProbeLocalResult(status, clickProduced, clickX, clickY,
                intent.scanLeft(), intent.scanTop(), intent.scanRight(), intent.scanBottom(), reason);
    }
}
