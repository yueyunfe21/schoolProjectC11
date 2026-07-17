package com.bot.dhxy.service.dialog;

import com.bot.dhxy.core.ImageFinder;
import com.bot.dhxy.driver.BoundWindowCaptureService;
import com.bot.dhxy.input.InputProvider;
import com.bot.dhxy.model.dialog.DialogType;
import com.bot.dhxy.tools.CoordinateHelper;
import com.bot.dhxy.tools.ImagePreprocessor;
import com.bot.dhxy.window.model.WindowNativeBinding;
import com.bot.dhxy.window.runtime.WindowNativeBindingRefreshService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * W-696-DIALOG-GREEN-TEMPLATE-OPTION-WHOLE-MECHANICS-1: closed local mechanical boundary for the
 * committed green-template dialog-option pass.
 *
 * <p>Byte-behaviour authority is {@code 696a12b0 DialogService} for both real callers: the prepare
 * caller ({@code #prepareGreenTemplateOption} 2178-2281, {@link Operation#MATCH_ONLY}) may reuse a
 * caller-supplied detection frame/rect and never issues input; the direct click caller
 * ({@code #handleGreenTemplateOptionDirect} 2283-2378, {@link Operation#MATCH_AND_CLICK}) runs its
 * dialog-type gate and then always matches against a fresh dialog capture before one {@code 150ms}
 * click. Every real capture first refreshes the exact HWND geometry; the single wash, caller-ordered
 * {@code 0.85} first-hit short-circuit with a {@code -1.0} best diagnostic on each miss, and the
 * per-candidate continuation are preserved from the baseline. No sorting, parallelism, retry, TTL, or
 * new input queue is introduced. Business spec ordering, fallback, GiveItem decisions, and
 * {@code DialogResult} construction stay in the caller.</p>
 */
@Slf4j
@Service
public final class DialogGreenTemplateOptionLocalMacroMechanics {

    // Baseline large-dialog ROI (696a12b0 DialogService DIALOG_LARGE_*), applied to fresh binding origin.
    private static final int DIALOG_LARGE_X = 250;
    private static final int DIALOG_LARGE_Y = 312;
    private static final int DIALOG_LARGE_W = 529;
    private static final int DIALOG_LARGE_H = 208;

    private static final double TEMPLATE_MATCH_THRESHOLD = 0.85D;
    private static final double DIAGNOSTIC_BEST_THRESHOLD = -1.0D;
    private static final int CLICK_DELAY_MS = 150;
    private static final String INPUT_WORKER_THREAD_MARKER = "dhxy-input-action-worker";

    private final DialogDetectionLocalMechanics dialogDetectionLocalMechanics;
    private final BoundWindowCaptureService captureService;
    private final WindowNativeBindingRefreshService bindingRefreshService;
    private final CoordinateHelper coordinateHelper;
    private final InputProvider inputProvider;

    public DialogGreenTemplateOptionLocalMacroMechanics(
            DialogDetectionLocalMechanics dialogDetectionLocalMechanics,
            BoundWindowCaptureService captureService,
            WindowNativeBindingRefreshService bindingRefreshService,
            CoordinateHelper coordinateHelper,
            InputProvider inputProvider) {
        this.dialogDetectionLocalMechanics = Objects.requireNonNull(
                dialogDetectionLocalMechanics, "dialogDetectionLocalMechanics");
        this.captureService = Objects.requireNonNull(captureService, "captureService");
        this.bindingRefreshService = Objects.requireNonNull(
                bindingRefreshService, "bindingRefreshService");
        this.coordinateHelper = Objects.requireNonNull(coordinateHelper, "coordinateHelper");
        this.inputProvider = Objects.requireNonNull(inputProvider, "inputProvider");
    }

    /**
     * Runs one closed green-template option pass for the exact binding.
     *
     * @param binding exact native-window binding whose refreshed origin anchors any fresh dialog rect
     * @param command closed caller-ordered specs, operation, verify flag, and optional supplied frame
     * @return closed typed result; only {@code MATCHED}/{@code CLICKED} carry the matched point and
     *         raw/washed image evidence
     */
    public Result execute(WindowNativeBinding binding, Command command) {
        Objects.requireNonNull(command, "command");
        // MATCH_AND_CLICK requires the existing input worker before any observation or input.
        if (command.operation() == Operation.MATCH_AND_CLICK && !isInputWorkerThread()) {
            return Result.state(Terminal.NON_INPUT_WORKER);
        }

        BufferedImage frame = null;
        BufferedImage washed = null;
        try {
            FrameObservation observation = observe(binding, command);
            if (observation.terminal() != null) {
                return Result.state(observation.terminal());
            }
            frame = observation.frame();
            int[] rect = observation.rect();

            ImageEvidence rawEvidence = ImageEvidence.of(frame);
            washed = ImagePreprocessor.washDialogOptionTemplateTextToBlackAndWhite(frame);
            if (washed == null) {
                return Result.state(Terminal.MECHANICS_FAILED);
            }
            ImageEvidence washedEvidence = ImageEvidence.of(washed);

            for (Spec spec : command.specs()) {
                if (spec == null) {
                    log.info("dialog green template skip null candidate, continue caller order");
                    continue;
                }
                String templatePath = spec.templatePath();
                if (templatePath == null || templatePath.isBlank()) {
                    log.info("dialog green template skip null/blank template, continue caller order: name={}",
                            spec.name());
                    continue;
                }
                BufferedImage template = loadTemplate(templatePath);
                if (template == null) {
                    log.info("dialog green template candidate unreadable, continue caller order: name={} template={}",
                            spec.name(), templatePath);
                    continue;
                }
                double[] match;
                try {
                    double[] found = ImageFinder.find(washed, template, TEMPLATE_MATCH_THRESHOLD);
                    if (found == null || found.length < 2) {
                        double[] best = ImageFinder.find(washed, template, DIAGNOSTIC_BEST_THRESHOLD);
                        log.info("dialog green template miss, continue caller order: name={} template={} threshold=0.85 best={}",
                                spec.name(), templatePath, formatMatch(best));
                        match = null;
                    } else {
                        match = found;
                    }
                } finally {
                    template.flush();
                }
                if (match == null) {
                    continue;
                }

                Point optionPoint = coordinateHelper.resolveMatchedPointInRect(rect, match);
                Point safeClick = coordinateHelper.getRandomizedPoint(
                        optionPoint, spec.minOffsetX(), spec.maxOffsetX(), spec.randomRadiusY());
                int relativeX = safeClick.x - rect[0];
                int relativeY = safeClick.y - rect[1];
                if (command.operation() == Operation.MATCH_ONLY) {
                    return Result.matched(Terminal.MATCHED, spec.name(), templatePath,
                            relativeX, relativeY, safeClick.x, safeClick.y, rawEvidence, washedEvidence);
                }
                inputProvider.clickLeft(safeClick.x, safeClick.y, CLICK_DELAY_MS);
                return Result.matched(Terminal.CLICKED, spec.name(), templatePath,
                        relativeX, relativeY, safeClick.x, safeClick.y, rawEvidence, washedEvidence);
            }
            return Result.state(Terminal.NOT_FOUND);
        } catch (RuntimeException e) {
            log.warn("dialog green template mechanics failed: reason={}", e.getMessage(), e);
            return Result.state(Terminal.MECHANICS_FAILED);
        } finally {
            if (washed != null) {
                washed.flush();
            }
            if (frame != null) {
                frame.flush();
            }
        }
    }

    /**
     * Resolves the single matching frame + screen-absolute rect, or a short-circuit terminal.
     *
     * <ul>
     *   <li>MATCH_ONLY with a supplied frame reuses it verbatim (zero detect, zero capture);</li>
     *   <li>verify=true runs the committed dialog-type gate: MATCH_ONLY reuses the detection frame,
     *       MATCH_AND_CLICK matches against a fresh dialog capture taken after the gate;</li>
     *   <li>verify=false matches against a fresh dialog capture.</li>
     * </ul>
     */
    private FrameObservation observe(WindowNativeBinding binding, Command command) {
        if (command.suppliedFramePng() != null) {
            BufferedImage frame;
            try {
                frame = ImageIO.read(new ByteArrayInputStream(command.suppliedFramePng()));
            } catch (IOException e) {
                log.warn("dialog green template supplied frame decode failed: reason={}", e.getMessage(), e);
                return FrameObservation.terminal(Terminal.MECHANICS_FAILED);
            }
            if (frame == null) {
                return FrameObservation.terminal(Terminal.MECHANICS_FAILED);
            }
            int[] rect = command.suppliedRect();
            if (frame.getWidth() != rect[2] - rect[0] || frame.getHeight() != rect[3] - rect[1]) {
                frame.flush();
                log.warn("dialog green template supplied frame size mismatch: frame={}x{} rect=({}, {})-({}, {})",
                        frame.getWidth(), frame.getHeight(), rect[0], rect[1], rect[2], rect[3]);
                return FrameObservation.terminal(Terminal.MECHANICS_FAILED);
            }
            return FrameObservation.frame(frame, rect);
        }

        if (!isCapturableBinding(binding)) {
            return FrameObservation.terminal(Terminal.BINDING_UNAVAILABLE);
        }

        if (command.verifyDialogType()) {
            DialogDetectionLocalMechanics.DialogDetectionResult detection =
                    dialogDetectionLocalMechanics.detectDialog(binding, false, 0L, "green-template-option");
            return switch (detection.state()) {
                case CAPTURED -> {
                    if (detection.dialogType() != DialogType.OPTION) {
                        yield FrameObservation.terminal(Terminal.NOT_OPTION);
                    }
                    if (command.operation() == Operation.MATCH_ONLY) {
                        BufferedImage frame;
                        try {
                            frame = ImageIO.read(new ByteArrayInputStream(detection.framePngBytes()));
                        } catch (IOException e) {
                            log.warn("dialog green template detection frame decode failed: reason={}",
                                    e.getMessage(), e);
                            yield FrameObservation.terminal(Terminal.MECHANICS_FAILED);
                        }
                        if (frame == null) {
                            yield FrameObservation.terminal(Terminal.MECHANICS_FAILED);
                        }
                        int[] rect = new int[]{
                                detection.dialogLeft(), detection.dialogTop(),
                                detection.dialogRight(), detection.dialogBottom()};
                        yield FrameObservation.frame(frame, rect);
                    }
                    // MATCH_AND_CLICK: type gate passed, but click matches a fresh post-gate capture.
                    yield freshCapture(binding);
                }
                case CAPTURE_UNAVAILABLE -> FrameObservation.terminal(Terminal.CAPTURE_UNAVAILABLE);
                case PRE_CAPTURE_INTERRUPTED -> FrameObservation.terminal(Terminal.INTERRUPTED);
                case NON_INPUT_WORKER -> FrameObservation.terminal(Terminal.NON_INPUT_WORKER);
                case MECHANICS_FAILED -> FrameObservation.terminal(Terminal.MECHANICS_FAILED);
            };
        }

        return freshCapture(binding);
    }

    /**
     * Refreshes the exact HWND geometry before the single capture so a window moved during command or
     * input queuing is captured from its fresh region; a failed refresh is a closed
     * {@code CAPTURE_UNAVAILABLE} and no stale coordinates or title search is used.
     */
    private FrameObservation freshCapture(WindowNativeBinding binding) {
        Optional<WindowNativeBinding> refreshed = bindingRefreshService.refreshGeometry(binding);
        if (refreshed.isEmpty()) {
            return FrameObservation.terminal(Terminal.CAPTURE_UNAVAILABLE);
        }
        WindowNativeBinding fresh = refreshed.get();
        if (!isCapturableBinding(fresh)) {
            return FrameObservation.terminal(Terminal.CAPTURE_UNAVAILABLE);
        }
        int left;
        int top;
        int right;
        int bottom;
        try {
            left = Math.addExact(fresh.getX(), DIALOG_LARGE_X);
            top = Math.addExact(fresh.getY(), DIALOG_LARGE_Y);
            right = Math.addExact(left, DIALOG_LARGE_W);
            bottom = Math.addExact(top, DIALOG_LARGE_H);
        } catch (ArithmeticException e) {
            return FrameObservation.terminal(Terminal.MECHANICS_FAILED);
        }
        Optional<BoundWindowCaptureService.CaptureResult> captured;
        try {
            captured = captureService.captureRegion(fresh, fresh.getX(), fresh.getY(), left, top, right, bottom);
        } catch (RuntimeException e) {
            log.warn("dialog green template fresh capture failed: hwnd={} reason={}",
                    fresh.getNativeHandle(), e.getMessage(), e);
            return FrameObservation.terminal(Terminal.MECHANICS_FAILED);
        }
        if (captured == null || captured.isEmpty() || captured.get().image() == null) {
            return FrameObservation.terminal(Terminal.CAPTURE_UNAVAILABLE);
        }
        return FrameObservation.frame(captured.get().image(), new int[]{left, top, right, bottom});
    }

    private static boolean isCapturableBinding(WindowNativeBinding binding) {
        return binding != null
                && binding.hasNativeHandle()
                && binding.hasGeometry()
                && binding.getWidth() >= DIALOG_LARGE_X + DIALOG_LARGE_W
                && binding.getHeight() >= DIALOG_LARGE_Y + DIALOG_LARGE_H;
    }

    private static BufferedImage loadTemplate(String templatePath) {
        try {
            return ImageIO.read(Path.of(templatePath).toFile());
        } catch (IOException | RuntimeException e) {
            // IOException, or Path.of InvalidPathException / file SecurityException: treat this
            // candidate as unreadable and let the caller order continue to the next spec.
            return null;
        }
    }

    private static String formatMatch(double[] match) {
        if (match == null || match.length < 3) {
            return "unreadable";
        }
        return "(" + Math.round(match[0]) + "," + Math.round(match[1]) + ") score="
                + String.format("%.4f", match[2]);
    }

    private boolean isInputWorkerThread() {
        return Thread.currentThread().getName().contains(INPUT_WORKER_THREAD_MARKER);
    }

    private static byte[] encodePng(BufferedImage image) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            if (!ImageIO.write(image, "png", out)) {
                throw new IllegalStateException("no PNG writer available");
            }
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("failed to encode PNG evidence", e);
        }
    }

    private static String sha256Hex(byte[] bytes) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(bytes);
            StringBuilder hex = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                hex.append(Character.forDigit((b >> 4) & 0xF, 16));
                hex.append(Character.forDigit(b & 0xF, 16));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    // ===================== closed immutable nested types =====================

    /** Two closed operations; MATCH_ONLY issues zero input. */
    public enum Operation {
        MATCH_ONLY,
        MATCH_AND_CLICK
    }

    /** Ten closed terminals; only MATCHED/CLICKED carry a point and image evidence. */
    public enum Terminal {
        MATCHED,
        CLICKED,
        NOT_OPTION,
        NOT_FOUND,
        CAPTURE_UNAVAILABLE,
        TEMPLATE_UNAVAILABLE,
        BINDING_UNAVAILABLE,
        INTERRUPTED,
        NON_INPUT_WORKER,
        MECHANICS_FAILED
    }

    /** One caller-ordered template spec, mirroring the committed GreenTemplateClickSpec fields. */
    public record Spec(
            String name,
            String templatePath,
            int minOffsetX,
            int maxOffsetX,
            int randomRadiusY) {
        // Baseline-nullable: a null spec, name, or templatePath is skipped by the caller-order loop and
        // never rejected here, so an invalid early candidate cannot truncate the first-hit fallback.
    }

    /**
     * Closed command/intent: operation, verify flag, caller-ordered non-empty specs, and an optional
     * supplied dialog frame PNG plus its screen-absolute rect. A supplied frame is only valid for
     * {@code MATCH_ONLY}; frame and rect must appear together.
     */
    public record Command(
            Operation operation,
            boolean verifyDialogType,
            List<Spec> specs,
            byte[] suppliedFramePng,
            int[] suppliedRect) {

        public Command {
            Objects.requireNonNull(operation, "operation");
            Objects.requireNonNull(specs, "specs");
            // Order-preserving defensive copy that ALLOWS null candidates; the caller-order loop skips
            // a null spec / null-or-blank templatePath exactly like the baseline and never rejects them
            // here, so an invalid early candidate cannot truncate the first-hit fallback.
            specs = Collections.unmodifiableList(new ArrayList<>(specs));
            if (specs.isEmpty()) {
                throw new IllegalArgumentException("green template command requires at least one spec");
            }
            boolean hasFrame = suppliedFramePng != null;
            boolean hasRect = suppliedRect != null;
            if (hasFrame != hasRect) {
                throw new IllegalArgumentException("supplied frame and rect must be present together");
            }
            if (hasFrame) {
                if (operation != Operation.MATCH_ONLY) {
                    throw new IllegalArgumentException("supplied frame is only valid for MATCH_ONLY");
                }
                if (suppliedFramePng.length == 0) {
                    throw new IllegalArgumentException("supplied frame must not be empty");
                }
                if (suppliedRect.length != 4
                        || suppliedRect[2] <= suppliedRect[0] || suppliedRect[3] <= suppliedRect[1]) {
                    throw new IllegalArgumentException("supplied rect must be a positive screen-absolute box");
                }
                suppliedFramePng = suppliedFramePng.clone();
                suppliedRect = suppliedRect.clone();
            }
        }

        public static Command match(Operation operation, boolean verifyDialogType, List<Spec> specs) {
            return new Command(operation, verifyDialogType, specs, null, null);
        }

        public static Command suppliedMatchOnly(List<Spec> specs, byte[] suppliedFramePng, int[] suppliedRect) {
            return new Command(Operation.MATCH_ONLY, false, specs, suppliedFramePng, suppliedRect);
        }

        @Override
        public byte[] suppliedFramePng() {
            return suppliedFramePng == null ? null : suppliedFramePng.clone();
        }

        @Override
        public int[] suppliedRect() {
            return suppliedRect == null ? null : suppliedRect.clone();
        }
    }

    /**
     * Raw or washed PNG evidence. The canonical constructor is structurally closed: it recomputes the
     * SHA-256 and dimensions from the actual PNG bytes and rejects any inconsistent caller-built value,
     * so no untrusted authority can carry contradictory bytes/hash/size.
     */
    public record ImageEvidence(byte[] pngBytes, String sha256, int width, int height) {

        public ImageEvidence {
            Objects.requireNonNull(pngBytes, "pngBytes");
            Objects.requireNonNull(sha256, "sha256");
            pngBytes = pngBytes.clone();
            if (pngBytes.length == 0) {
                throw new IllegalArgumentException("image evidence requires PNG bytes");
            }
            BufferedImage decoded;
            try {
                decoded = ImageIO.read(new ByteArrayInputStream(pngBytes));
            } catch (IOException e) {
                throw new IllegalArgumentException("image evidence bytes are not decodable PNG", e);
            }
            if (decoded == null) {
                throw new IllegalArgumentException("image evidence bytes are not a PNG image");
            }
            try {
                if (decoded.getWidth() != width || decoded.getHeight() != height) {
                    throw new IllegalArgumentException(
                            "image evidence dimensions do not match the PNG bytes");
                }
                if (!sha256Hex(pngBytes).equalsIgnoreCase(sha256)) {
                    throw new IllegalArgumentException("image evidence SHA-256 does not match the PNG bytes");
                }
            } finally {
                decoded.flush();
            }
        }

        private static ImageEvidence of(BufferedImage image) {
            byte[] bytes = encodePng(image);
            return new ImageEvidence(bytes, sha256Hex(bytes), image.getWidth(), image.getHeight());
        }

        @Override
        public byte[] pngBytes() {
            return pngBytes.clone();
        }
    }

    /**
     * Closed result. {@code MATCHED}/{@code CLICKED} carry the matched spec, relative/absolute point,
     * and both raw and washed image evidence; every other terminal carries none of them.
     */
    public record Result(
            Terminal terminal,
            String specName,
            String templatePath,
            Integer relativeX,
            Integer relativeY,
            Integer absoluteX,
            Integer absoluteY,
            ImageEvidence rawEvidence,
            ImageEvidence washedEvidence) {

        public Result {
            Objects.requireNonNull(terminal, "terminal");
            boolean matched = terminal == Terminal.MATCHED || terminal == Terminal.CLICKED;
            boolean hasAllMatchFields = specName != null && templatePath != null
                    && relativeX != null && relativeY != null
                    && absoluteX != null && absoluteY != null
                    && rawEvidence != null && washedEvidence != null;
            boolean hasAnyMatchField = specName != null || templatePath != null
                    || relativeX != null || relativeY != null
                    || absoluteX != null || absoluteY != null
                    || rawEvidence != null || washedEvidence != null;
            if (matched && !hasAllMatchFields) {
                throw new IllegalArgumentException(
                        "MATCHED/CLICKED result must carry the point and raw/washed evidence");
            }
            if (!matched && hasAnyMatchField) {
                throw new IllegalArgumentException(
                        "non-MATCHED/CLICKED result must not carry point or image fields");
            }
        }

        private static Result state(Terminal terminal) {
            return new Result(terminal, null, null, null, null, null, null, null, null);
        }

        private static Result matched(
                Terminal terminal, String specName, String templatePath,
                int relativeX, int relativeY, int absoluteX, int absoluteY,
                ImageEvidence rawEvidence, ImageEvidence washedEvidence) {
            return new Result(terminal, specName, templatePath,
                    relativeX, relativeY, absoluteX, absoluteY, rawEvidence, washedEvidence);
        }
    }

    /** Internal single-frame observation: either a usable frame+rect or a short-circuit terminal. */
    private record FrameObservation(BufferedImage frame, int[] rect, Terminal terminal) {

        private static FrameObservation frame(BufferedImage frame, int[] rect) {
            return new FrameObservation(frame, rect, null);
        }

        private static FrameObservation terminal(Terminal terminal) {
            return new FrameObservation(null, null, terminal);
        }
    }
}
