package com.bot.dhxy.service.dialog;

import com.bot.dhxy.core.TextRecognizer;
import com.bot.dhxy.model.ocr.OcrWordResult;
import com.bot.dhxy.window.runtime.WindowScopedTempPath;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Exact-scope local mechanics for the committed {@code 696a12b0} dialog-option OCR word read of one
 * already-selected color variant. Behavior authority is {@code GameTextLineOcrService.readDialogOptionWords}
 * ({@code 696a12b0:.../vision/GameTextLineOcrService.java:120+}) and
 * {@code DialogService.processOptionsWithOCRDetailed} ({@code 696a12b0:.../service/DialogService.java:1792-1895}),
 * but this class owns only a single-variant local observation: the caller supplies one GREEN or YELLOW
 * immutable PNG (bytes + SHA-256 + dimensions), its screen-absolute rect and a diagnostic label; this class
 * strictly verifies bytes/hash/dimensions/rect, writes the exact bytes to a window-scoped artifact, calls the
 * existing local OCR provider ({@link TextRecognizer#getAllTextResultsLocalOnly(String)}) exactly once, and
 * returns image-local, caller-order, immutable word boxes.
 *
 * <p>The Cloud caller keeps every business decision: green-first then yellow-on-miss color selection,
 * alias/keyword matching, green/yellow merge, fallback, action/click and fingerprinting. This class never
 * selects a color, alias, target or fallback and never sends input. Word-box coordinates stay image-local to
 * the supplied variant PNG; a caller that needs screen-absolute points adds the supplied rect origin itself.</p>
 *
 * <p>Every terminal is a closed {@link Status}. Only {@link Status#WORDS} carries word boxes.
 * {@link Status#NO_WORDS} is a genuine successful-but-empty OCR pass; a provider that is unavailable
 * ({@link Status#OCR_UNAVAILABLE}) or throws ({@link Status#MECHANICS_FAILED}) is never disguised as a visual
 * {@code NO_WORDS}, and there is no retry. Supplied bytes are defensively copied on entry, the PNG-validation
 * image is owned and flushed exactly once, and the artifact is written to a window-scoped temp path.</p>
 */
@Slf4j
@Service
public final class DialogOptionOcrWordsLocalObservationMechanics {

    private static final String VARIANT_ARTIFACT_FILE = "dialog_option_ocr_variant.png";

    private final TextRecognizer textRecognizer;
    private final WindowScopedTempPath windowScopedTempPath;

    public DialogOptionOcrWordsLocalObservationMechanics(TextRecognizer textRecognizer,
                                                        WindowScopedTempPath windowScopedTempPath) {
        this.textRecognizer = Objects.requireNonNull(textRecognizer, "textRecognizer");
        this.windowScopedTempPath = Objects.requireNonNull(windowScopedTempPath, "windowScopedTempPath");
    }

    /**
     * Observe the option words in one already-selected color variant.
     *
     * @param variant         diagnostic color label for the already-selected variant; this class does not select it
     * @param variantPngBytes immutable variant PNG bytes (defensively copied here); the OCR authority for this call
     * @param variantSha256   declared SHA-256 of {@code variantPngBytes}; strictly re-verified
     * @param imageWidth      declared variant width in pixels; strictly re-verified against the decoded PNG
     * @param imageHeight     declared variant height in pixels; strictly re-verified against the decoded PNG
     * @param screenRect      screen-absolute {@code [left, top, right, bottom]} of the variant; positive area whose span equals the dimensions
     * @param source          diagnostic label written into OCR logs
     * @return a non-null closed observation; only {@link Status#WORDS} carries image-local, caller-order word boxes
     */
    public OptionOcrWordsObservation observeOptionWords(ColorVariant variant,
                                                        byte[] variantPngBytes,
                                                        String variantSha256,
                                                        int imageWidth,
                                                        int imageHeight,
                                                        int[] screenRect,
                                                        String source) {
        String safeSource = safeSource(source);

        // The public contract only accepts an already-selected GREEN/YELLOW variant. A null variant is a
        // caller contract violation and fails closed as INVALID_IMAGE; the enum has no third state to add.
        if (variant == null) {
            log.warn("dialog option ocr words invalid image: source={} reason=null color variant", safeSource);
            return OptionOcrWordsObservation.terminal(Status.INVALID_IMAGE);
        }
        String variantLabel = variant.name();

        // Defensive copy of the borrowed inputs before any validation or use.
        byte[] bytesCopy = variantPngBytes == null ? null : variantPngBytes.clone();
        int[] rectCopy = screenRect == null ? null : screenRect.clone();

        // Strict bytes/hash/dimensions/rect validation. A supplied variant that is not a self-consistent,
        // decodable image is a closed INVALID_IMAGE, never an OCR pass.
        try {
            validateVariant(bytesCopy, variantSha256, imageWidth, imageHeight, rectCopy);
        } catch (IllegalArgumentException e) {
            log.warn("dialog option ocr words invalid image: source={} variant={} reason={}",
                    safeSource, variantLabel, e.getMessage());
            return OptionOcrWordsObservation.terminal(Status.INVALID_IMAGE);
        }

        // Materialize the exact validated bytes to a window-scoped artifact; the OCR provider reads a path.
        // Resolving the window-scoped path, a null/blank path and any resolve/Path/write RuntimeException all
        // converge to the closed MECHANICS_FAILED terminal.
        String artifactPath;
        try {
            artifactPath = windowScopedTempPath.resolve(VARIANT_ARTIFACT_FILE);
            if (artifactPath == null || artifactPath.isBlank()) {
                throw new IllegalStateException("window-scoped artifact path is null or blank");
            }
            Files.write(Path.of(artifactPath), bytesCopy);
        } catch (IOException | RuntimeException e) {
            log.warn("dialog option ocr words artifact write failed: source={} variant={} reason={}",
                    safeSource, variantLabel, e.getMessage(), e);
            return OptionOcrWordsObservation.terminal(Status.MECHANICS_FAILED);
        }

        // Single call to the existing local OCR provider. Empty optional = provider unavailable/failed
        // (OCR_UNAVAILABLE); a present list = a successful pass whose emptiness is a genuine NO_WORDS.
        Optional<List<OcrWordResult>> ocr;
        try {
            ocr = textRecognizer.getAllTextResultsLocalOnly(artifactPath);
        } catch (RuntimeException e) {
            log.warn("dialog option ocr words provider threw: source={} variant={} reason={}",
                    safeSource, variantLabel, e.getMessage(), e);
            return OptionOcrWordsObservation.terminal(Status.MECHANICS_FAILED);
        }
        if (ocr == null || ocr.isEmpty()) {
            log.info("dialog option ocr words provider unavailable: source={} variant={}", safeSource, variantLabel);
            return OptionOcrWordsObservation.terminal(Status.OCR_UNAVAILABLE);
        }

        List<OcrWordResult> rawWords = ocr.get();
        if (rawWords == null || rawWords.isEmpty()) {
            log.info("dialog option ocr words empty pass: source={} variant={}", safeSource, variantLabel);
            return OptionOcrWordsObservation.terminal(Status.NO_WORDS);
        }

        // Preserve provider/caller order; copy each box into the closed image-local type.
        List<WordBox> boxes = new ArrayList<>(rawWords.size());
        for (OcrWordResult word : rawWords) {
            if (word == null) {
                continue;
            }
            boxes.add(new WordBox(
                    word.getText(),
                    word.getX(),
                    word.getY(),
                    word.getLeft(),
                    word.getTop(),
                    word.getWidth(),
                    word.getHeight(),
                    word.getScore()));
        }
        if (boxes.isEmpty()) {
            // The pass returned only null entries; still a successful empty visual read, not a failure.
            log.info("dialog option ocr words empty after copy: source={} variant={}", safeSource, variantLabel);
            return OptionOcrWordsObservation.terminal(Status.NO_WORDS);
        }
        log.info("dialog option ocr words read: source={} variant={} count={}",
                safeSource, variantLabel, boxes.size());
        return OptionOcrWordsObservation.words(boxes);
    }

    // Standard 8-byte PNG file signature (RFC 2083): the bytes must be a real PNG, not merely any format
    // that ImageIO can decode (JPEG/GIF/etc are rejected).
    private static final byte[] PNG_SIGNATURE = {
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A
    };

    /**
     * Strict variant validation: present non-empty bytes and hash, a standard PNG signature, positive declared
     * dimensions, a positive-area screen-absolute rect (computed with long arithmetic so extreme endpoints
     * cannot overflow) whose span equals the dimensions, a decodable PNG whose dimensions match the
     * declaration, and a recomputed SHA-256 equal to the declared one. The decoded validation image is owned
     * here and flushed exactly once; ImageIO IOException and RuntimeException both become INVALID_IMAGE.
     */
    private static void validateVariant(byte[] bytes, String declaredSha256,
                                        int width, int height, int[] rect) {
        if (bytes == null || bytes.length == 0) {
            throw new IllegalArgumentException("variant bytes must be present");
        }
        if (declaredSha256 == null || declaredSha256.isBlank()) {
            throw new IllegalArgumentException("variant SHA-256 must be present");
        }
        if (bytes.length < PNG_SIGNATURE.length) {
            throw new IllegalArgumentException("variant bytes are too short to be a PNG");
        }
        for (int i = 0; i < PNG_SIGNATURE.length; i++) {
            if (bytes[i] != PNG_SIGNATURE[i]) {
                throw new IllegalArgumentException("variant bytes do not carry the standard PNG signature");
            }
        }
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("variant dimensions must be positive");
        }
        if (rect == null || rect.length != 4) {
            throw new IllegalArgumentException("variant rect must be [left, top, right, bottom]");
        }
        long rectWidth = (long) rect[2] - (long) rect[0];
        long rectHeight = (long) rect[3] - (long) rect[1];
        if (rectWidth <= 0 || rectHeight <= 0) {
            throw new IllegalArgumentException("variant rect must enclose a positive area");
        }
        if (rectWidth != (long) width || rectHeight != (long) height) {
            throw new IllegalArgumentException("variant dimensions must equal the rect span");
        }
        BufferedImage decoded;
        try {
            decoded = ImageIO.read(new ByteArrayInputStream(bytes));
        } catch (IOException | RuntimeException e) {
            throw new IllegalArgumentException("variant bytes are not a decodable image", e);
        }
        if (decoded == null) {
            throw new IllegalArgumentException("variant bytes are not a decodable image");
        }
        try {
            if (decoded.getWidth() != width || decoded.getHeight() != height) {
                throw new IllegalArgumentException("variant PNG dimensions do not match the declared dimensions");
            }
            String actualSha256;
            try {
                actualSha256 = sha256Hex(bytes);
            } catch (NoSuchAlgorithmException e) {
                throw new IllegalArgumentException("cannot verify variant SHA-256", e);
            }
            if (!actualSha256.equals(declaredSha256)) {
                throw new IllegalArgumentException("variant SHA-256 does not match the image bytes");
            }
        } finally {
            decoded.flush();
        }
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
        return source == null || source.isBlank() ? "dialog-option-ocr-words" : source;
    }

    /**
     * Diagnostic variant label for the already-selected image; this class never selects it. {@code RAW} is
     * the committed baseline pass that OCRs the raw frame when the green wash is unavailable.
     */
    public enum ColorVariant {
        GREEN,
        YELLOW,
        RAW
    }

    public enum Status {
        WORDS,
        NO_WORDS,
        OCR_UNAVAILABLE,
        INVALID_IMAGE,
        MECHANICS_FAILED
    }

    /** One immutable image-local OCR word box for the supplied variant. */
    public record WordBox(String text, int x, int y, int left, int top, int width, int height, double score) {

        public WordBox {
            text = text == null ? "" : text;
        }
    }

    /**
     * Closed immutable observation. Only {@link Status#WORDS} carries a non-empty, immutable, caller-order
     * list of image-local word boxes; every other status carries no boxes.
     */
    public record OptionOcrWordsObservation(Status status, List<WordBox> wordBoxes) {

        public OptionOcrWordsObservation {
            Objects.requireNonNull(status, "status");
            wordBoxes = wordBoxes == null ? null : List.copyOf(wordBoxes);
            boolean words = status == Status.WORDS;
            if (words && (wordBoxes == null || wordBoxes.isEmpty())) {
                throw new IllegalArgumentException("WORDS observation must carry at least one word box");
            }
            if (!words && wordBoxes != null) {
                throw new IllegalArgumentException("non-WORDS observation must not carry word boxes");
            }
        }

        private static OptionOcrWordsObservation words(List<WordBox> wordBoxes) {
            return new OptionOcrWordsObservation(Status.WORDS, wordBoxes);
        }

        private static OptionOcrWordsObservation terminal(Status status) {
            return new OptionOcrWordsObservation(status, null);
        }
    }
}
