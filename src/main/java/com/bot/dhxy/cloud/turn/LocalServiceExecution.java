package com.bot.dhxy.cloud.turn;

import com.bot.dhxy.cloud.turn.protocol.TurnFrameMetadata;
import com.bot.dhxy.cloud.turn.protocol.TurnFramePurpose;
import com.bot.dhxy.cloud.turn.protocol.TurnRegion;
import com.bot.dhxy.cloud.turn.protocol.TurnStepResult;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Iterator;
import java.util.Objects;

/**
 * Immutable mechanical result returned by one permanent local-Service adapter.
 *
 * <p>TURN-40B-C2 closed typed-stop invariant: {@code stopRequested == true} if and only if the
 * result is the exact typed stop representation {@code status == FAILED && code == "STOPPED"}.
 * The {@code STOPPED} code is reserved — a generic failure must never carry it, and a stop result
 * can never carry any other code — so two representations of the same stop can never disagree.
 * Only {@link #stopped(String)} mints the stop form.</p>
 *
 * @param status completed or failed step status; {@code NOT_RUN} is rejected.
 * @param code nonblank stable mechanical result code.
 * @param localResultJson optional immutable JSON text, limited by UTF-8 byte size.
 * @param frame optional single Quest-detail or task-tracker-panel raw PNG frame with matching metadata.
 * @param stopRequested true only for the typed stop representation defined above.
 */
public record LocalServiceExecution(
        TurnStepResult.Status status,
        String code,
        String localResultJson,
        TurnFrame frame,
        boolean stopRequested) {

    public static final int MAX_LOCAL_RESULT_JSON_BYTES = 64 * 1024;
    public static final int MAX_QUEST_FRAME_BYTES = 8 * 1024 * 1024;

    /** The single reserved typed-stop code; hardcoded by {@link #stopped(String)}. */
    public static final String STOPPED_CODE = "STOPPED";

    /** Compatibility shape for every non-stop construction site. */
    public LocalServiceExecution(TurnStepResult.Status status,
                                 String code,
                                 String localResultJson,
                                 TurnFrame frame) {
        this(status, code, localResultJson, frame, false);
    }

    public LocalServiceExecution {
        Objects.requireNonNull(status, "status");
        if (status != TurnStepResult.Status.COMPLETED && status != TurnStepResult.Status.FAILED) {
            throw new IllegalArgumentException("local Service status must be COMPLETED or FAILED");
        }
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("code must not be blank");
        }
        if (stopRequested) {
            if (status != TurnStepResult.Status.FAILED || !STOPPED_CODE.equals(code)) {
                throw new IllegalArgumentException(
                        "a stop result must be exactly FAILED with the reserved STOPPED code");
            }
        } else if (STOPPED_CODE.equals(code)) {
            throw new IllegalArgumentException(
                    "a generic result must not carry the reserved STOPPED code");
        }
        if (!code.equals(code.trim())) {
            throw new IllegalArgumentException("code must not contain leading or trailing whitespace");
        }
        if (localResultJson != null) {
            if (localResultJson.isBlank()) {
                throw new IllegalArgumentException("localResultJson must be null or nonblank JSON text");
            }
            int jsonBytes = localResultJson.getBytes(StandardCharsets.UTF_8).length;
            if (jsonBytes > MAX_LOCAL_RESULT_JSON_BYTES) {
                throw new IllegalArgumentException(
                        "localResultJson exceeds " + MAX_LOCAL_RESULT_JSON_BYTES + " UTF-8 bytes");
            }
        }
        if (frame != null) {
            if (status != TurnStepResult.Status.COMPLETED) {
                throw new IllegalArgumentException("only a completed local Service result may carry a Quest frame");
            }
            requireValidLocalFrame(frame);
        }
    }

    /**
     * Create a completed local-Service result.
     *
     * @param code nonblank stable completion code.
     * @param localResultJson optional JSON text no larger than 64 KiB when UTF-8 encoded.
     * @param frame optional single Quest-detail frame; null for operations without image output.
     * @return validated immutable completed result.
     */
    public static LocalServiceExecution completed(String code, String localResultJson, TurnFrame frame) {
        return new LocalServiceExecution(TurnStepResult.Status.COMPLETED, code, localResultJson, frame);
    }

    /**
     * Create a failed local-Service result without an image slot.
     *
     * @param code nonblank stable failure code.
     * @param localResultJson optional JSON failure detail no larger than 64 KiB when UTF-8 encoded.
     * @return validated immutable failed result.
     */
    public static LocalServiceExecution failed(String code, String localResultJson) {
        return new LocalServiceExecution(TurnStepResult.Status.FAILED, code, localResultJson, null);
    }

    /**
     * Create the sole typed stop result: FAILED with the hardcoded reserved {@code STOPPED} code
     * and {@code stopRequested=true}. Callers cannot choose another code, so the stop discriminator
     * and the code can never disagree.
     *
     * @param localResultJson optional JSON stop detail no larger than 64 KiB when UTF-8 encoded.
     * @return validated immutable typed stop result.
     */
    public static LocalServiceExecution stopped(String localResultJson) {
        return new LocalServiceExecution(
                TurnStepResult.Status.FAILED, STOPPED_CODE, localResultJson, null, true);
    }

    private static void requireValidLocalFrame(TurnFrame frame) {
        TurnFrameMetadata metadata = frame.metadata();
        if (metadata.purpose() != TurnFramePurpose.QUEST_DETAIL
                && metadata.purpose() != TurnFramePurpose.TASK_TRACKER_PANEL) {
            throw new IllegalArgumentException(
                    "local Service frame purpose must be QUEST_DETAIL or TASK_TRACKER_PANEL");
        }
        if (!"image/png".equalsIgnoreCase(metadata.contentType())) {
            throw new IllegalArgumentException("local Service frame contentType must be image/png");
        }
        if (metadata.width() <= 0 || metadata.height() <= 0) {
            throw new IllegalArgumentException("local Service frame dimensions must be positive");
        }
        TurnRegion region = metadata.region();
        if (region == null || region.width() != metadata.width() || region.height() != metadata.height()) {
            throw new IllegalArgumentException("local Service frame dimensions must match its region");
        }
        if (metadata.sourceStepIndex() != null && metadata.sourceStepIndex() < 0) {
            throw new IllegalArgumentException("local Service frame sourceStepIndex must be nonnegative");
        }

        byte[] pngBytes = frame.pngBytes();
        if (pngBytes.length > MAX_QUEST_FRAME_BYTES) {
            throw new IllegalArgumentException("local Service frame exceeds " + MAX_QUEST_FRAME_BYTES + " bytes");
        }
        if (!isPng(pngBytes)) {
            throw new IllegalArgumentException("local Service frame must contain raw PNG bytes");
        }
        requireMatchingPngDimensions(pngBytes, metadata.width(), metadata.height());
        String actualSha256 = sha256(pngBytes);
        if (!actualSha256.equalsIgnoreCase(metadata.sha256())) {
            throw new IllegalArgumentException("local Service frame SHA-256 does not match its raw PNG bytes");
        }
    }

    private static boolean isPng(byte[] bytes) {
        byte[] signature = {(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a};
        if (bytes.length < signature.length) {
            return false;
        }
        for (int index = 0; index < signature.length; index++) {
            if (bytes[index] != signature[index]) {
                return false;
            }
        }
        return true;
    }

    private static void requireMatchingPngDimensions(byte[] bytes, int expectedWidth, int expectedHeight) {
        try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            if (input == null) {
                throw new IllegalArgumentException("Quest frame PNG metadata cannot be read");
            }
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) {
                throw new IllegalArgumentException("Quest frame PNG metadata cannot be read");
            }
            ImageReader reader = readers.next();
            BufferedImage decoded = null;
            try {
                reader.setInput(input, true, true);
                decoded = reader.read(0);
                if (decoded == null) {
                    throw new IllegalArgumentException("Quest frame PNG pixels cannot be decoded");
                }
                if (decoded.getWidth() != expectedWidth || decoded.getHeight() != expectedHeight) {
                    throw new IllegalArgumentException("Quest frame dimensions do not match its raw PNG bytes");
                }
            } finally {
                if (decoded != null) {
                    decoded.flush();
                }
                reader.dispose();
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("Quest frame PNG metadata cannot be read", e);
        }
    }

    private static String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }
}
