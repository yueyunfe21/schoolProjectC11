package com.bot.dhxy.service.dialog;

import com.bot.dhxy.core.ImageFinder;
import com.bot.dhxy.model.dialog.DialogDetection;
import com.bot.dhxy.model.dialog.DialogType;
import com.bot.dhxy.model.dialog.WhiteTemplateSpec;
import com.bot.dhxy.tools.CoordinateHelper;
import com.bot.dhxy.tools.ImagePreprocessor;
import com.bot.dhxy.window.model.WindowNativeBinding;
import com.bot.dhxy.window.runtime.WindowScopedTempPath;
import lombok.extern.slf4j.Slf4j;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Objects;

/**
 * Exact-window local mechanics for the committed {@code 696a12b0} white story-template observation. It
 * reproduces the continuous read of {@code DialogService.prepareWhiteStoryTemplateOrAbsent}
 * ({@code :971-1018}: supplied story-detection reuse, single fresh-detection fallback, STORY gate)
 * together with {@code DialogService.verifyWhiteStoryTemplate} ({@code :451-499}: same-frame thin-white
 * wash, caller-order {@code 0.85} first-hit template match, relative/screen-absolute matched point) as one
 * pure-local closed observation.
 *
 * <p>Frame source, baseline order: a usable supplied STORY frame (image and screen-absolute
 * {@code dialogRect} present) is reused verbatim, and when {@code absentAllowed} a usable supplied NONE
 * frame is reused too; any other supplied frame (e.g. OPTION) or a missing/unusable one falls back to
 * <b>exactly one</b> fresh detection through the committed detection owner
 * {@link DialogDetectionLocalMechanics#detectDialog(WindowNativeBinding, boolean, long, String)} with
 * {@code hidePlayerNames=false, waitBeforeCaptureMs=0}. The exact-window binding/geometry gate applies
 * only to that fallback capture; a caller that already holds the same-frame image and rect is never
 * re-bound or re-captured. The fallback's single CAPTURED PNG/rect/type is consumed once, with zero
 * second capture. This class never modifies the detection owner or its shared wire.</p>
 *
 * <p>Zero input and zero target business verdict: this entry sends no keyboard/mouse action and does not
 * build any negative/absent/miss {@code PreparedDialogAction}. Every terminal is a closed {@link State}.
 * Only {@link State#MATCHED} carries the matched template (name is nullable, exactly as the baseline
 * {@code actionKey(spec.name())}), the window-rect-relative point ({@code point - rect[left/top]}), the
 * screen-absolute point and the same-frame defensive evidence (detection rect, frame PNG bytes, SHA-256
 * and dimensions) so a Cloud caller can verify that the matched point belongs to that exact observation.
 * The supplied frame image is caller-owned ({@link DialogDetection} javadoc) and is never flushed here;
 * an image decoded from fallback bytes is owned and flushed exactly once on every path, and every OpenCV
 * mat in the inlined wash is released exactly once on success, empty and exception paths.</p>
 */
@Slf4j
@Service
public final class DialogWhiteStoryTemplateLocalObservationMechanics {

    // Committed white story-template match threshold from DialogService (WHITE_STORY_TEMPLATE_THRESHOLD).
    private static final double WHITE_STORY_TEMPLATE_THRESHOLD = 0.85;
    private static final String RAW_TEMP_FILE = "dialog_white_template_raw.png";
    private static final String WASHED_TEMP_FILE = "dialog_white_template_washed.png";

    private final WindowScopedTempPath windowScopedTempPath;
    private final CoordinateHelper coordinateHelper;
    private final DialogDetectionLocalMechanics dialogDetectionMechanics;

    public DialogWhiteStoryTemplateLocalObservationMechanics(WindowScopedTempPath windowScopedTempPath,
                                                             CoordinateHelper coordinateHelper,
                                                             DialogDetectionLocalMechanics dialogDetectionMechanics) {
        this.windowScopedTempPath = Objects.requireNonNull(windowScopedTempPath, "windowScopedTempPath");
        this.coordinateHelper = Objects.requireNonNull(coordinateHelper, "coordinateHelper");
        this.dialogDetectionMechanics =
                Objects.requireNonNull(dialogDetectionMechanics, "dialogDetectionMechanics");
    }

    /**
     * Observe the committed white story-template signal on one caller-supplied exact native-window binding.
     *
     * @param binding          exact binding; used only when a fresh fallback detection is required
     * @param suppliedDetection already-classified dialog detection to reuse when usable; may be null
     * @param absentAllowed    when true a usable supplied NONE frame is reused and reported as STORY_ABSENT
     * @param specs            ordered white-template candidates; first {@code 0.85} match wins
     * @param source           caller label for diagnostics
     * @return a non-null closed observation; only {@link State#MATCHED} carries the template, point and evidence
     */
    public WhiteStoryTemplateObservation observeWhiteStoryTemplate(WindowNativeBinding binding,
                                                                   DialogDetection suppliedDetection,
                                                                   boolean absentAllowed,
                                                                   List<WhiteTemplateSpec> specs,
                                                                   String source) {
        String safeSource = safeSource(source);

        // Step 1: resolve the single observation frame. Baseline usableSuppliedStoryDetection: reuse a
        // usable STORY frame, or (only when absentAllowed) a usable NONE frame, verbatim with no binding
        // work. Everything else takes exactly one fresh detection below.
        boolean usableSupplied = suppliedDetection != null
                && suppliedDetection.image() != null
                && suppliedDetection.dialogRect() != null
                && (suppliedDetection.type() == DialogType.STORY
                        || (absentAllowed && suppliedDetection.type() == DialogType.NONE));

        BufferedImage frameImage;
        boolean borrowedFrame;
        int[] rect;
        DialogType frameType;

        if (usableSupplied) {
            frameImage = suppliedDetection.image();
            borrowedFrame = true;
            rect = suppliedDetection.dialogRect();
            frameType = suppliedDetection.type();
            log.info("dialog white story observe reuse supplied: source={} type={} absentAllowed={}",
                    safeSource, frameType, absentAllowed);
        } else {
            // Fallback fresh detection: the exact-window binding/geometry gate applies ONLY here.
            if (binding == null || !binding.hasNativeHandle() || !binding.hasGeometry()) {
                return WhiteStoryTemplateObservation.terminal(State.BINDING_UNAVAILABLE);
            }
            DialogDetectionLocalMechanics.DialogDetectionResult detected;
            try {
                detected = dialogDetectionMechanics.detectDialog(binding, false, 0L, safeSource);
            } catch (RuntimeException e) {
                // P2-1: converge the fresh-detection collaborator's RuntimeException to the closed
                // MECHANICS_FAILED terminal. No owned frame exists yet, so nothing to flush here.
                log.warn("dialog white story observe fresh detect failed: source={} reason={}",
                        safeSource, e.getMessage(), e);
                return WhiteStoryTemplateObservation.terminal(State.MECHANICS_FAILED);
            }
            switch (detected.state()) {
                case CAPTURED -> {
                    // consume the single capture below
                }
                case MECHANICS_FAILED -> {
                    return WhiteStoryTemplateObservation.terminal(State.MECHANICS_FAILED);
                }
                default -> {
                    // CAPTURE_UNAVAILABLE / PRE_CAPTURE_INTERRUPTED / NON_INPUT_WORKER: no usable frame.
                    return WhiteStoryTemplateObservation.terminal(State.CAPTURE_UNAVAILABLE);
                }
            }
            BufferedImage decoded;
            try {
                decoded = decodePng(detected.framePngBytes());
            } catch (IOException | RuntimeException e) {
                log.warn("dialog white story observe fallback decode failed: source={} reason={}",
                        safeSource, e.getMessage(), e);
                return WhiteStoryTemplateObservation.terminal(State.MECHANICS_FAILED);
            }
            if (decoded == null) {
                return WhiteStoryTemplateObservation.terminal(State.MECHANICS_FAILED);
            }
            frameImage = decoded;
            borrowedFrame = false;
            rect = new int[]{detected.dialogLeft(), detected.dialogTop(),
                    detected.dialogRight(), detected.dialogBottom()};
            frameType = detected.dialogType();
            log.info("dialog white story observe fresh detect: source={} type={}", safeSource, frameType);
        }

        // From here frameImage may be owned (fallback); guarantee a single flush on every path.
        try {
            // Step 2: STORY gate. A reused NONE (absentAllowed) and a fresh OPTION/NONE all land here.
            if (frameType != DialogType.STORY) {
                log.info("dialog white story observe story absent: source={} actualType={}",
                        safeSource, frameType);
                // Directive #2 (gap #1 Option A): report this detection's frame rect and dimensions so the
                // Cloud can rebuild the committed rect-centred absent marker. Dimensions come from the same
                // authoritative frame; a rect that does not span it converges to the closed MECHANICS_FAILED.
                return frameOnlyOrFailed(State.STORY_ABSENT, rect, frameImage, safeSource);
            }
            if (rect == null || rect.length != 4) {
                return WhiteStoryTemplateObservation.terminal(State.MECHANICS_FAILED);
            }

            // Step 3+4: from the single selected frame, take same-frame defensive evidence (PNG bytes +
            // SHA-256 + dimensions) AND always write that same frame to a fresh window-scoped raw artifact.
            // Scope Amendment #1: there is no supplied rawPath reuse and no second capture; wash/template
            // match below read ONLY this artifact, so the evidence PNG and the matched pixels are guaranteed
            // to come from one authoritative frame.
            int frameWidth;
            int frameHeight;
            byte[] framePng;
            String frameSha;
            String rawPath = windowScopedTempPath.resolve(RAW_TEMP_FILE);
            try {
                frameWidth = frameImage.getWidth();
                frameHeight = frameImage.getHeight();
                framePng = pngBytes(frameImage);
                frameSha = sha256Hex(framePng);
                // saveImage reads the frame and never flushes it; a failed save leaves no raw file to wash.
                if (!ImagePreprocessor.saveImage(frameImage, rawPath)) {
                    log.warn("dialog white story observe raw materialize failed: source={}", safeSource);
                    return WhiteStoryTemplateObservation.terminal(State.MECHANICS_FAILED);
                }
            } catch (IOException | NoSuchAlgorithmException | RuntimeException e) {
                // P2-1: dimensions read, PNG encode/hash and saveImage RuntimeExceptions all converge to the
                // closed MECHANICS_FAILED terminal; the owned fallback frame is still flushed by the outer
                // finally, and a borrowed supplied frame stays with the caller.
                log.warn("dialog white story observe frame prepare failed: source={} reason={}",
                        safeSource, e.getMessage(), e);
                return WhiteStoryTemplateObservation.terminal(State.MECHANICS_FAILED);
            }
            if (frameWidth <= 0 || frameHeight <= 0 || framePng.length == 0) {
                return WhiteStoryTemplateObservation.terminal(State.MECHANICS_FAILED);
            }

            String washedPath = windowScopedTempPath.resolve(WASHED_TEMP_FILE);
            washThinWhiteTextToBlackAndWhite(rawPath, washedPath);

            // Step 5: caller-order, skip null/blank path, 0.85 first-hit. A nullable spec.name() on a hit
            // is a valid visible result (baseline actionKey(spec.name())), never a mechanics failure.
            List<WhiteTemplateSpec> orderedSpecs = specs == null ? List.of() : specs;
            for (WhiteTemplateSpec spec : orderedSpecs) {
                if (spec == null || spec.templatePath() == null || spec.templatePath().isBlank()) {
                    continue;
                }
                double[] result;
                try {
                    result = ImageFinder.find(washedPath, spec.templatePath(), WHITE_STORY_TEMPLATE_THRESHOLD);
                } catch (RuntimeException e) {
                    log.warn("dialog white story observe template scan failed: source={} template={} reason={}",
                            safeSource, spec.templatePath(), e.getMessage(), e);
                    return WhiteStoryTemplateObservation.terminal(State.MECHANICS_FAILED);
                }
                if (result == null || result.length < 2) {
                    continue;
                }
                Point point;
                try {
                    point = coordinateHelper.resolveMatchedPointInRect(rect, result);
                } catch (RuntimeException e) {
                    // P2-1: converge the coordinate collaborator's RuntimeException to MECHANICS_FAILED;
                    // the owned fallback frame (if any) is still flushed by the outer finally.
                    log.warn("dialog white story observe coordinate resolve failed: source={} template={} reason={}",
                            safeSource, spec.templatePath(), e.getMessage(), e);
                    return WhiteStoryTemplateObservation.terminal(State.MECHANICS_FAILED);
                }
                if (point == null) {
                    continue;
                }
                log.info("dialog white story observe matched: source={} template={} point=({}, {})",
                        safeSource, spec.templatePath(), point.x, point.y);
                try {
                    return WhiteStoryTemplateObservation.matched(
                            spec.name(),
                            spec.templatePath(),
                            point.x - rect[0],
                            point.y - rect[1],
                            point.x,
                            point.y,
                            rect,
                            framePng,
                            frameSha,
                            frameWidth,
                            frameHeight);
                } catch (RuntimeException e) {
                    // The MATCHED evidence authority self-check (constructor) failed for this same-frame
                    // observation; keep the public entry closed by mapping it to MECHANICS_FAILED rather
                    // than letting the IllegalArgumentException escape. Outer finally still flushes.
                    log.warn("dialog white story observe evidence authority failed: source={} template={} reason={}",
                            safeSource, spec.templatePath(), e.getMessage(), e);
                    return WhiteStoryTemplateObservation.terminal(State.MECHANICS_FAILED);
                }
            }

            log.info("dialog white story observe miss: source={} candidates={}",
                    safeSource, orderedSpecs.size());
            // Directive #2 (gap #1 Option A): STORY frame present but no template matched — report this
            // detection's frame rect and dimensions so the Cloud rebuilds the committed rect-centred miss
            // marker (never empty). Dimensions are the same-frame values validated in step 3.
            return frameOnlyOrFailed(State.STORY_MISS, rect, frameWidth, frameHeight, safeSource);
        } finally {
            // Borrowed supplied image stays with the caller; a fallback-decoded owned image is flushed once.
            if (!borrowedFrame && frameImage != null) {
                frameImage.flush();
            }
        }
    }

    /**
     * Inlined committed thin-white CPU wash ({@code ImagePreprocessor.washThinWhiteTextToBlackAndWhite}
     * path overload), kept self-contained in this class per the extraction contract: HSV white band, 3x3
     * erosion of the thick fill, and the (all - thick) thin-glyph mask written to {@code outputPath}. Every
     * owned mat is released exactly once in {@code finally} on the empty-input return, the success path and
     * any OpenCV exception; a failed wash leaves a missing washed file that the template scan reads as
     * no-match, exactly like the baseline.
     */
    private void washThinWhiteTextToBlackAndWhite(String inputPath, String outputPath) {
        Mat src = null;
        Mat hsv = null;
        Mat allWhiteMask = null;
        Mat thickWhiteMask = null;
        Mat kernel = null;
        Mat textOnlyMask = null;
        try {
            src = Imgcodecs.imread(inputPath);
            if (src.empty()) {
                log.warn("wash thin white text failed, input not found: {}", inputPath);
                return;
            }

            hsv = new Mat();
            Imgproc.cvtColor(src, hsv, Imgproc.COLOR_BGR2HSV);

            Scalar lowerWhite = new Scalar(0, 0, 225);
            Scalar upperWhite = new Scalar(180, 15, 255);
            allWhiteMask = new Mat();
            Core.inRange(hsv, lowerWhite, upperWhite, allWhiteMask);

            kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new org.opencv.core.Size(3, 3));
            thickWhiteMask = new Mat();
            Imgproc.erode(allWhiteMask, thickWhiteMask, kernel);

            textOnlyMask = new Mat();
            Core.subtract(allWhiteMask, thickWhiteMask, textOnlyMask);
            Imgcodecs.imwrite(outputPath, textOnlyMask);
        } catch (Exception e) {
            log.error("wash thin white text failed", e);
        } finally {
            if (src != null) {
                src.release();
            }
            if (hsv != null) {
                hsv.release();
            }
            if (allWhiteMask != null) {
                allWhiteMask.release();
            }
            if (thickWhiteMask != null) {
                thickWhiteMask.release();
            }
            if (kernel != null) {
                kernel.release();
            }
            if (textOnlyMask != null) {
                textOnlyMask.release();
            }
        }
    }

    private static BufferedImage decodePng(byte[] bytes) throws IOException {
        if (bytes == null || bytes.length == 0) {
            return null;
        }
        return ImageIO.read(new ByteArrayInputStream(bytes));
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

    private static String safeSource(String source) {
        return source == null || source.isBlank() ? "white-story-observe" : source;
    }

    /**
     * Build a Directive #2 STORY_MISS / STORY_ABSENT frame-only terminal from the same authoritative frame,
     * reading its dimensions off the image. A missing/invalid rect or a frame the rect does not exactly span
     * (the {@code frameOnly} evidence authority) converges to the closed {@link State#MECHANICS_FAILED}.
     */
    private WhiteStoryTemplateObservation frameOnlyOrFailed(State state, int[] rect,
                                                            BufferedImage frameImage, String source) {
        if (rect == null || rect.length != 4 || frameImage == null) {
            return WhiteStoryTemplateObservation.terminal(State.MECHANICS_FAILED);
        }
        return frameOnlyOrFailed(state, rect, frameImage.getWidth(), frameImage.getHeight(), source);
    }

    private WhiteStoryTemplateObservation frameOnlyOrFailed(State state, int[] rect,
                                                            int frameWidth, int frameHeight, String source) {
        if (rect == null || rect.length != 4 || frameWidth <= 0 || frameHeight <= 0) {
            return WhiteStoryTemplateObservation.terminal(State.MECHANICS_FAILED);
        }
        try {
            return WhiteStoryTemplateObservation.frameOnly(state, rect, frameWidth, frameHeight);
        } catch (RuntimeException e) {
            log.warn("dialog white story observe frame-only authority failed: source={} state={} reason={}",
                    source, state, e.getMessage(), e);
            return WhiteStoryTemplateObservation.terminal(State.MECHANICS_FAILED);
        }
    }

    public enum State {
        MATCHED,
        STORY_MISS,
        STORY_ABSENT,
        CAPTURE_UNAVAILABLE,
        BINDING_UNAVAILABLE,
        MECHANICS_FAILED
    }

    /**
     * Closed white story-template observation. Only {@link State#MATCHED} carries payload; the matched
     * template name is intentionally nullable (baseline {@code actionKey(spec.name())}), while the template
     * path, the window-rect-relative point ({@code point - rect[left/top]}), the screen-absolute point and
     * the same-frame defensive evidence (detection rect, frame PNG bytes, SHA-256 and dimensions) are
     * required and internally consistent. The rect and byte accessors defensively clone. Every non-matched
     * terminal carries no payload.
     */
    public record WhiteStoryTemplateObservation(
            State state,
            String matchedTemplateName,
            String matchedTemplatePath,
            Integer relativeX,
            Integer relativeY,
            Integer absoluteX,
            Integer absoluteY,
            int[] frameRect,
            byte[] framePngBytes,
            String frameSha256,
            Integer frameWidth,
            Integer frameHeight) {

        public WhiteStoryTemplateObservation {
            Objects.requireNonNull(state, "state");
            frameRect = frameRect == null ? null : frameRect.clone();
            framePngBytes = framePngBytes == null ? null : framePngBytes.clone();
            boolean matched = state == State.MATCHED;
            // Directive #2 (W-696 gap #1 Option A): STORY_MISS / STORY_ABSENT additionally carry ONLY this
            // detection's frame rect and dimensions so the Cloud can rebuild the committed rect-centred
            // miss/absent PreparedDialogAction; they never carry the match point, template or frame bytes.
            boolean frameOnly = state == State.STORY_MISS || state == State.STORY_ABSENT;
            // matchedTemplateName is intentionally NOT required; a matched template may carry a null name.
            boolean hasRequired = matchedTemplatePath != null
                    && relativeX != null && relativeY != null && absoluteX != null && absoluteY != null
                    && frameRect != null && framePngBytes != null && frameSha256 != null
                    && frameWidth != null && frameHeight != null;
            boolean hasMatchedOnly = matchedTemplateName != null || matchedTemplatePath != null
                    || relativeX != null || relativeY != null || absoluteX != null || absoluteY != null
                    || framePngBytes != null || frameSha256 != null;
            boolean hasAnyPayload = hasMatchedOnly || frameRect != null
                    || frameWidth != null || frameHeight != null;
            if (matched && !hasRequired) {
                throw new IllegalArgumentException(
                        "MATCHED observation must carry template path, point, rect, frame bytes, hash and dimensions");
            }
            if (frameOnly && !(frameRect != null && frameWidth != null && frameHeight != null)) {
                throw new IllegalArgumentException(
                        "STORY_MISS/STORY_ABSENT observation must carry this detection's frame rect and dimensions");
            }
            if (frameOnly && hasMatchedOnly) {
                throw new IllegalArgumentException(
                        "STORY_MISS/STORY_ABSENT observation must not carry any matched-only payload");
            }
            if (!matched && !frameOnly && hasAnyPayload) {
                throw new IllegalArgumentException(
                        "CAPTURE_UNAVAILABLE/BINDING_UNAVAILABLE/MECHANICS_FAILED observation must carry no payload");
            }
            if (matched) {
                if (matchedTemplatePath.isBlank()) {
                    throw new IllegalArgumentException("MATCHED observation requires a non-blank template path");
                }
                if (framePngBytes.length == 0 || frameSha256.isBlank()) {
                    throw new IllegalArgumentException(
                            "MATCHED observation requires present frame bytes and hash");
                }
            }
            if (matched || frameOnly) {
                // Same-frame evidence authority (validated on the already-defensively-cloned rect): the rect
                // must enclose a positive area whose span equals the frame dimensions. MATCHED additionally
                // binds the relative point to the absolute point minus the rect origin.
                if (frameRect.length != 4) {
                    throw new IllegalArgumentException("frame rect must be [left, top, right, bottom]");
                }
                if (frameWidth <= 0 || frameHeight <= 0) {
                    throw new IllegalArgumentException("frame dimensions must be positive");
                }
                int rectWidth = frameRect[2] - frameRect[0];
                int rectHeight = frameRect[3] - frameRect[1];
                if (rectWidth <= 0 || rectHeight <= 0) {
                    throw new IllegalArgumentException("frame rect must enclose a positive area");
                }
                if (rectWidth != frameWidth || rectHeight != frameHeight) {
                    throw new IllegalArgumentException("frame dimensions must equal the detection rect span");
                }
            }
            if (matched) {
                if (relativeX != absoluteX - frameRect[0] || relativeY != absoluteY - frameRect[1]) {
                    throw new IllegalArgumentException(
                            "MATCHED relative point must equal the absolute point minus the rect origin");
                }
                // Decode the cloned PNG once to prove it is real image bytes whose dimensions match, and
                // recompute the SHA-256 over those exact bytes; the probe image is flushed exactly once.
                try {
                    BufferedImage probe = decodePng(framePngBytes);
                    try {
                        if (probe == null) {
                            throw new IllegalArgumentException("MATCHED frame bytes are not decodable PNG");
                        }
                        if (probe.getWidth() != frameWidth || probe.getHeight() != frameHeight) {
                            throw new IllegalArgumentException(
                                    "MATCHED PNG dimensions disagree with the declared dimensions");
                        }
                    } finally {
                        if (probe != null) {
                            probe.flush();
                        }
                    }
                    if (!sha256Hex(framePngBytes).equals(frameSha256)) {
                        throw new IllegalArgumentException("MATCHED SHA-256 disagrees with the frame bytes");
                    }
                } catch (IOException | NoSuchAlgorithmException e) {
                    throw new IllegalArgumentException("MATCHED evidence could not be validated", e);
                }
            }
        }

        private static WhiteStoryTemplateObservation matched(String matchedTemplateName,
                                                             String matchedTemplatePath,
                                                             int relativeX, int relativeY,
                                                             int absoluteX, int absoluteY,
                                                             int[] frameRect, byte[] framePngBytes,
                                                             String frameSha256,
                                                             int frameWidth, int frameHeight) {
            return new WhiteStoryTemplateObservation(State.MATCHED, matchedTemplateName, matchedTemplatePath,
                    relativeX, relativeY, absoluteX, absoluteY,
                    frameRect, framePngBytes, frameSha256, frameWidth, frameHeight);
        }

        private static WhiteStoryTemplateObservation terminal(State state) {
            return new WhiteStoryTemplateObservation(state, null, null, null, null, null, null,
                    null, null, null, null, null);
        }

        /**
         * Directive #2 (W-696 gap #1 Option A) frame-only terminal for {@link State#STORY_MISS} and
         * {@link State#STORY_ABSENT}: carries ONLY this detection's frame rect and dimensions so the Cloud
         * can rebuild the committed rect-centred miss/absent {@code PreparedDialogAction}; no match point,
         * template or frame bytes ride here.
         */
        private static WhiteStoryTemplateObservation frameOnly(State state, int[] frameRect,
                                                               int frameWidth, int frameHeight) {
            return new WhiteStoryTemplateObservation(state, null, null, null, null, null, null,
                    frameRect, null, null, frameWidth, frameHeight);
        }

        @Override
        public int[] frameRect() {
            return frameRect == null ? null : frameRect.clone();
        }

        @Override
        public byte[] framePngBytes() {
            return framePngBytes == null ? null : framePngBytes.clone();
        }
    }
}
