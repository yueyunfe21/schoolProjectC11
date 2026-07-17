package com.bot.dhxy.service.dialog;

import com.bot.dhxy.driver.BoundWindowCaptureService;
import com.bot.dhxy.tools.ImagePreprocessor;
import com.bot.dhxy.window.model.WindowNativeBinding;
import com.bot.dhxy.window.runtime.WindowNativeBindingRefreshService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;
import java.util.Optional;

/**
 * Whole continuous local image preparation for the committed {@code 696a12b0} dialog-option OCR path,
 * extracted from the down-shiftable parts of {@code DialogService.java:1792+} +
 * {@code GameTextLineOcrService.java:120-162} (frame/rect resolution plus the green and yellow OCR
 * image variants).
 *
 * <p>It resolves a supplied detection frame/rect when present, otherwise takes one exact-window
 * fresh-geometry single capture, then generates the two OCR image variants in the baseline order (green
 * first via {@code washGreenTextToBlackAndWhite}, yellow second via {@code washYellowTextToBlackAndWhite})
 * and returns each variant's PNG bytes + SHA-256 plus the shared dimensions and screen-absolute rect as a
 * single self-verifying observation. It renders no verdict: OCR word interpretation, target alias/name
 * matching, fallback option, prepared-action/result construction and the click decision (including the
 * baseline green-first / green-hit-skips-yellow order) all remain with the future Cloud
 * {@code DialogService}. It sends no input and adds no retry, calls the existing pure-local
 * {@code ImagePreprocessor} washes without modifying them, and gives every owned {@link BufferedImage} a
 * single owner and one flush.</p>
 */
@Slf4j
@Service
public final class DialogOptionOcrImageLocalObservationMechanics {

    // Committed dialog-area geometry (window-client pixels) from DialogService; screen-absolute rects
    // add only the window origin (no DPI scaling), matching the approved dialog local mechanics.
    private static final int DIALOG_LARGE_X = 250;
    private static final int DIALOG_LARGE_Y = 312;
    private static final int DIALOG_LARGE_W = 529;
    private static final int DIALOG_LARGE_H = 208;

    private final BoundWindowCaptureService captureService;
    private final WindowNativeBindingRefreshService bindingRefreshService;

    public DialogOptionOcrImageLocalObservationMechanics(
            BoundWindowCaptureService captureService,
            WindowNativeBindingRefreshService bindingRefreshService) {
        this.captureService = Objects.requireNonNull(captureService, "captureService");
        this.bindingRefreshService = Objects.requireNonNull(bindingRefreshService, "bindingRefreshService");
    }

    /** Closed terminal. Only {@link #CAPTURED} carries any image evidence or rect. */
    public enum Status {
        CAPTURED,
        CAPTURE_UNAVAILABLE,
        BINDING_UNAVAILABLE,
        INVALID_SUPPLIED_FRAME,
        MECHANICS_FAILED
    }

    /**
     * Closed immutable intent with three legal forms so a fresh capture uses the caller's detection rect even
     * when no frame is available:
     * <ul>
     *   <li>SUPPLIED: frame + its SHA-256 + its full positive-area screen-absolute rect — the supplied frame
     *       is reused after its integrity is re-verified;</li>
     *   <li>FRESH_AT_RECT: no frame/SHA, but a full positive-area rect — capture once at that exact caller
     *       detection rect;</li>
     *   <li>FRESH_DEFAULT: no frame/SHA/rect — capture once at the committed default dialog rect.</li>
     * </ul>
     * The frame and its SHA are strictly present-together and, when present, require the rect; a rect may
     * stand alone for a fresh capture. Bytes are defensively copied.
     */
    public record DialogOcrImageIntent(
            byte[] suppliedFramePngBytes,
            String suppliedFrameSha256,
            Integer rectLeft,
            Integer rectTop,
            Integer rectRight,
            Integer rectBottom,
            String source) {

        public DialogOcrImageIntent {
            suppliedFramePngBytes = suppliedFramePngBytes == null ? null : suppliedFramePngBytes.clone();
            boolean hasFrame = suppliedFramePngBytes != null;
            boolean hasSha = suppliedFrameSha256 != null && !suppliedFrameSha256.isBlank();
            boolean anySha = suppliedFrameSha256 != null;
            boolean hasRect = rectLeft != null && rectTop != null
                    && rectRight != null && rectBottom != null;
            boolean anyRect = rectLeft != null || rectTop != null
                    || rectRight != null || rectBottom != null;
            if (anyRect != hasRect) {
                throw new IllegalArgumentException("rect must be a full quad or fully absent");
            }
            // A supplied frame binds a non-blank SHA-256; the SHA never appears without the frame.
            if (hasFrame != (anySha && hasSha)) {
                throw new IllegalArgumentException(
                        "a supplied frame and its non-blank SHA-256 must both be present or both absent");
            }
            // A supplied frame requires its rect; a rect may stand alone for a fresh capture at that rect.
            if (hasFrame && !hasRect) {
                throw new IllegalArgumentException("a supplied frame requires its screen-absolute rect");
            }
            if (hasRect && (rectRight <= rectLeft || rectBottom <= rectTop)) {
                throw new IllegalArgumentException("rect must be a positive-area rect");
            }
        }

        @Override
        public byte[] suppliedFramePngBytes() {
            return suppliedFramePngBytes == null ? null : suppliedFramePngBytes.clone();
        }

        private boolean hasSuppliedFrame() {
            return suppliedFramePngBytes != null;
        }

        private boolean hasRect() {
            return rectLeft != null && rectTop != null && rectRight != null && rectBottom != null;
        }
    }

    /**
     * Immutable closed result. {@link Status#CAPTURED} is the single authority for one observation: it always
     * carries the raw PNG bytes with their recomputed SHA-256, the shared image dimensions and the
     * screen-absolute rect, plus the OPTIONAL green and yellow washed variants (each present-together with its
     * own recomputed SHA-256, or both absent when that wash was unavailable). The constructor decodes and
     * SHA-verifies the raw variant and any present optional variant, and checks every present image's
     * dimensions equal each other and the rect. Every non-CAPTURED status carries only a reason. Byte arrays
     * are defensively copied.
     */
    public record DialogOptionOcrImageResult(
            Status status,
            byte[] rawPngBytes,
            String rawSha256,
            byte[] greenPngBytes,
            String greenSha256,
            byte[] yellowPngBytes,
            String yellowSha256,
            Integer imageWidth,
            Integer imageHeight,
            Integer scanLeft,
            Integer scanTop,
            Integer scanRight,
            Integer scanBottom,
            String reason) {

        public DialogOptionOcrImageResult {
            Objects.requireNonNull(status, "status");
            rawPngBytes = rawPngBytes == null ? null : rawPngBytes.clone();
            greenPngBytes = greenPngBytes == null ? null : greenPngBytes.clone();
            yellowPngBytes = yellowPngBytes == null ? null : yellowPngBytes.clone();
            boolean captured = status == Status.CAPTURED;
            boolean anyEvidence = rawPngBytes != null || greenPngBytes != null || yellowPngBytes != null
                    || rawSha256 != null || greenSha256 != null || yellowSha256 != null
                    || imageWidth != null || imageHeight != null
                    || scanLeft != null || scanTop != null || scanRight != null || scanBottom != null;
            if (captured) {
                verifyCapturedEvidence(rawPngBytes, rawSha256, greenPngBytes, greenSha256,
                        yellowPngBytes, yellowSha256, imageWidth, imageHeight,
                        scanLeft, scanTop, scanRight, scanBottom);
            } else if (anyEvidence) {
                throw new IllegalArgumentException("non-CAPTURED result must carry only a reason");
            }
        }

        private static void verifyCapturedEvidence(
                byte[] rawPngBytes, String rawSha256, byte[] greenPngBytes, String greenSha256,
                byte[] yellowPngBytes, String yellowSha256, Integer imageWidth, Integer imageHeight,
                Integer scanLeft, Integer scanTop, Integer scanRight, Integer scanBottom) {
            // Raw is the single mandatory variant; green and yellow are optional (a wash can be unavailable).
            if (rawPngBytes == null || rawSha256 == null
                    || imageWidth == null || imageHeight == null
                    || scanLeft == null || scanTop == null || scanRight == null || scanBottom == null) {
                throw new IllegalArgumentException("CAPTURED must carry the raw frame, its SHA, dims and rect");
            }
            if (rawPngBytes.length == 0 || rawSha256.isBlank()) {
                throw new IllegalArgumentException("CAPTURED raw evidence must be non-empty");
            }
            int rectWidth = scanRight - scanLeft;
            int rectHeight = scanBottom - scanTop;
            if (rectWidth <= 0 || rectHeight <= 0) {
                throw new IllegalArgumentException("CAPTURED rect must be a positive-area rect");
            }
            if (imageWidth != rectWidth || imageHeight != rectHeight) {
                throw new IllegalArgumentException("image dimensions must equal the rect dimensions");
            }
            verifyVariant(rawPngBytes, rawSha256, imageWidth, imageHeight, "raw");
            requireOptionalVariant(greenPngBytes, greenSha256, imageWidth, imageHeight, "green");
            requireOptionalVariant(yellowPngBytes, yellowSha256, imageWidth, imageHeight, "yellow");
        }

        // An optional variant's bytes and SHA-256 are present-together; when present they are non-empty and
        // decode to the shared dimensions with a matching recomputed SHA-256.
        private static void requireOptionalVariant(byte[] bytes, String sha256, int width, int height, String name) {
            boolean hasBytes = bytes != null;
            boolean hasSha = sha256 != null;
            if (hasBytes != hasSha) {
                throw new IllegalArgumentException(name + " bytes and SHA-256 must be present or absent together");
            }
            if (hasBytes) {
                if (bytes.length == 0 || sha256.isBlank()) {
                    throw new IllegalArgumentException(name + " evidence must be non-empty when present");
                }
                verifyVariant(bytes, sha256, width, height, name);
            }
        }

        private static void verifyVariant(byte[] bytes, String declaredSha256, int width, int height, String name) {
            BufferedImage decoded;
            try {
                decoded = ImageIO.read(new ByteArrayInputStream(bytes));
            } catch (IOException e) {
                throw new IllegalArgumentException(name + " bytes are not a decodable image", e);
            }
            if (decoded == null) {
                throw new IllegalArgumentException(name + " bytes are not a decodable image");
            }
            // Own the decoded validation image through a single finally so it is released exactly once
            // even if a dimension/SHA check throws.
            try {
                int decodedWidth = decoded.getWidth();
                int decodedHeight = decoded.getHeight();
                if (decodedWidth != width || decodedHeight != height) {
                    throw new IllegalArgumentException(name + " dimensions do not match the rect dimensions");
                }
                String actualSha256;
                try {
                    actualSha256 = sha256Hex(bytes);
                } catch (NoSuchAlgorithmException e) {
                    throw new IllegalArgumentException("cannot verify " + name + " SHA-256", e);
                }
                if (!actualSha256.equals(declaredSha256)) {
                    throw new IllegalArgumentException(name + " SHA-256 does not match the image bytes");
                }
            } finally {
                decoded.flush();
            }
        }

        @Override
        public byte[] rawPngBytes() {
            return rawPngBytes == null ? null : rawPngBytes.clone();
        }

        @Override
        public byte[] greenPngBytes() {
            return greenPngBytes == null ? null : greenPngBytes.clone();
        }

        @Override
        public byte[] yellowPngBytes() {
            return yellowPngBytes == null ? null : yellowPngBytes.clone();
        }
    }

    /**
     * Prepare the raw + green + yellow OCR image variants for one dialog. A supplied detection frame is
     * used as-is (its decoded dimensions must equal the supplied rect); otherwise the exact binding is
     * captured once at the committed dialog rect. No OCR, verdict, input or retry.
     */
    public DialogOptionOcrImageResult prepareOptionOcrImages(WindowNativeBinding binding, DialogOcrImageIntent intent) {
        Objects.requireNonNull(intent, "intent");
        String safeSource = safeSource(intent.source());

        int[] rect;
        BufferedImage raw;
        if (intent.hasSuppliedFrame()) {
            // P1-1: re-verify the supplied frame's SHA-256 before any decode/reuse so a frame whose pixels do
            // not match its declared hash (e.g. a different frame with the same rect/dimensions) is rejected
            // as INVALID_SUPPLIED_FRAME and never reaches the wash/OCR path.
            String recomputedSha256;
            try {
                recomputedSha256 = sha256Hex(intent.suppliedFramePngBytes());
            } catch (NoSuchAlgorithmException e) {
                log.warn("dialog option ocr supplied frame sha unavailable: source={} reason={}", safeSource, e.getMessage(), e);
                return failure(Status.MECHANICS_FAILED, "supplied-frame-sha-unavailable source=" + safeSource);
            }
            if (!recomputedSha256.equals(intent.suppliedFrameSha256())) {
                log.warn("dialog option ocr supplied frame sha mismatch: source={} declared={} recomputed={}",
                        safeSource, intent.suppliedFrameSha256(), recomputedSha256);
                return failure(Status.INVALID_SUPPLIED_FRAME, "supplied-frame-sha-mismatch source=" + safeSource);
            }
            rect = new int[]{intent.rectLeft(), intent.rectTop(), intent.rectRight(), intent.rectBottom()};
            try {
                // A malformed supplied frame can fail decode with a checked IOException or an ImageIO
                // provider/runtime exception; both converge to the closed INVALID_SUPPLIED_FRAME terminal.
                raw = ImageIO.read(new ByteArrayInputStream(intent.suppliedFramePngBytes()));
            } catch (IOException | RuntimeException e) {
                log.warn("dialog option ocr supplied frame decode failed: source={} reason={}", safeSource, e.getMessage(), e);
                return failure(Status.INVALID_SUPPLIED_FRAME, "supplied-frame-decode-failed source=" + safeSource);
            }
            if (raw == null) {
                return failure(Status.INVALID_SUPPLIED_FRAME, "supplied-frame-null source=" + safeSource);
            }
            // The dimension read/compare on the decoded frame may itself throw; converge to the closed
            // terminal and flush the owned raw exactly once. Only a valid frame hands raw to the outer
            // finally.
            boolean dimensionsMatch;
            try {
                int rectWidth = rect[2] - rect[0];
                int rectHeight = rect[3] - rect[1];
                dimensionsMatch = raw.getWidth() == rectWidth && raw.getHeight() == rectHeight;
            } catch (RuntimeException e) {
                raw.flush();
                log.warn("dialog option ocr supplied frame dimension read failed: source={} reason={}", safeSource, e.getMessage(), e);
                return failure(Status.INVALID_SUPPLIED_FRAME, "supplied-frame-dims-exception source=" + safeSource);
            }
            if (!dimensionsMatch) {
                raw.flush();
                return failure(Status.INVALID_SUPPLIED_FRAME, "supplied-frame-dims-mismatch source=" + safeSource);
            }
        } else {
            Optional<WindowNativeBinding> fresh;
            try {
                fresh = bindingRefreshService.refreshGeometry(binding);
            } catch (RuntimeException e) {
                log.warn("dialog option ocr fresh-geometry mechanics failed: source={} reason={}", safeSource, e.getMessage(), e);
                return failure(Status.MECHANICS_FAILED, "refresh-mechanics-failed source=" + safeSource);
            }
            if (fresh.isEmpty()) {
                return failure(Status.BINDING_UNAVAILABLE, "binding-unavailable source=" + safeSource);
            }
            WindowNativeBinding live = fresh.get();
            // P1-3: fresh capture uses the caller's detection rect when one was supplied (even without a
            // frame), matching the baseline which selects detection.dialogRect() independently; only when the
            // caller has no rect at all does it fall back to the committed default dialog rect.
            if (intent.hasRect()) {
                rect = new int[]{intent.rectLeft(), intent.rectTop(), intent.rectRight(), intent.rectBottom()};
            } else {
                rect = new int[]{
                        live.getX() + DIALOG_LARGE_X,
                        live.getY() + DIALOG_LARGE_Y,
                        live.getX() + DIALOG_LARGE_X + DIALOG_LARGE_W,
                        live.getY() + DIALOG_LARGE_Y + DIALOG_LARGE_H};
            }
            Optional<BoundWindowCaptureService.CaptureResult> captured;
            try {
                captured = captureService.captureRegion(
                        live, live.getX(), live.getY(), rect[0], rect[1], rect[2], rect[3]);
            } catch (RuntimeException e) {
                log.warn("dialog option ocr capture mechanics failed: source={} reason={}", safeSource, e.getMessage(), e);
                return failure(Status.MECHANICS_FAILED, "capture-mechanics-failed source=" + safeSource);
            }
            if (captured == null || captured.isEmpty() || captured.get().image() == null) {
                return failure(Status.CAPTURE_UNAVAILABLE, "capture-unavailable source=" + safeSource);
            }
            raw = captured.get().image();
        }

        try {
            byte[] rawBytes;
            String rawSha256;
            try {
                rawBytes = pngBytes(raw);
                rawSha256 = sha256Hex(rawBytes);
            } catch (IOException | NoSuchAlgorithmException e) {
                log.warn("dialog option ocr raw encode failed: source={} reason={}", safeSource, e.getMessage(), e);
                return failure(Status.MECHANICS_FAILED, "raw-encode-failed source=" + safeSource);
            }

            // Baseline order: green variant first, yellow second. A wash or encode failure for a color makes
            // that variant UNAVAILABLE (null bytes/SHA) rather than failing the whole capture, mirroring the
            // baseline which OCRs the raw frame when the green wash is unavailable and keeps the green result
            // when the yellow wash is unavailable. The raw frame is always present on a CAPTURED terminal.
            // The whole green wash+encode is wrapped so a wash RuntimeException (not only an encode
            // IOException) makes only the green variant unavailable; the washed owner is flushed if obtained.
            // The raw frame is already captured, so a green failure never fails the chain.
            byte[] greenBytes = null;
            String greenSha256 = null;
            try {
                BufferedImage green = ImagePreprocessor.washGreenTextToBlackAndWhite(raw);
                if (green != null) {
                    try {
                        greenBytes = pngBytes(green);
                        greenSha256 = sha256Hex(greenBytes);
                    } finally {
                        green.flush();
                    }
                }
            } catch (IOException | NoSuchAlgorithmException | RuntimeException e) {
                log.warn("dialog option ocr green wash/encode failed (variant unavailable): source={} reason={}",
                        safeSource, e.getMessage(), e);
                greenBytes = null;
                greenSha256 = null;
            }

            byte[] yellowBytes = null;
            String yellowSha256 = null;
            try {
                BufferedImage yellow = ImagePreprocessor.washYellowTextToBlackAndWhite(raw);
                if (yellow != null) {
                    try {
                        yellowBytes = pngBytes(yellow);
                        yellowSha256 = sha256Hex(yellowBytes);
                    } finally {
                        yellow.flush();
                    }
                }
            } catch (IOException | NoSuchAlgorithmException | RuntimeException e) {
                log.warn("dialog option ocr yellow wash/encode failed (variant unavailable): source={} reason={}",
                        safeSource, e.getMessage(), e);
                yellowBytes = null;
                yellowSha256 = null;
            }

            int width = raw.getWidth();
            int height = raw.getHeight();
            log.info("dialog option ocr images prepared: source={} supplied={} rect=({}, {})-({}, {}) dims={}x{}",
                    safeSource, intent.hasSuppliedFrame(), rect[0], rect[1], rect[2], rect[3], width, height);
            return new DialogOptionOcrImageResult(Status.CAPTURED,
                    rawBytes, rawSha256, greenBytes, greenSha256, yellowBytes, yellowSha256,
                    width, height, rect[0], rect[1], rect[2], rect[3], "captured source=" + safeSource);
        } catch (RuntimeException e) {
            log.warn("dialog option ocr wash mechanics failed: source={} reason={}", safeSource, e.getMessage(), e);
            return failure(Status.MECHANICS_FAILED, "wash-mechanics-failed source=" + safeSource);
        } finally {
            raw.flush();
        }
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

    private static DialogOptionOcrImageResult failure(Status status, String reason) {
        return new DialogOptionOcrImageResult(status,
                null, null, null, null, null, null, null, null, null, null, null, null, reason);
    }

    private static String safeSource(String source) {
        if (source == null || source.isBlank()) {
            return "unknown";
        }
        String value = source.trim().replaceAll("[^a-zA-Z0-9._-]", "_");
        return value.length() <= 120 ? value : value.substring(0, 120);
    }
}
