package com.bot.dhxy.cloud.remote;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

/**
 * Strict typed result for the NPC yellow-target local observation, mirroring the Cloud
 * {@code NpcYellowTargetMacroResult} and the already source-approved
 * {@code NpcClickYellowTargetLocalObservationMechanics.Result} field-for-field. {@code status} is one of the
 * six committed mechanical terminals.
 *
 * <p>{@link Status#CAPTURED} and {@link Status#NO_YELLOW_CANDIDATE} carry same-frame evidence (raw and mask
 * PNG bytes each with its own SHA-256 and dimensions, plus the screen-absolute scan rect); {@code CAPTURED}
 * additionally carries a non-empty, candidate-order {@link RemoteYellowCandidate} list while
 * {@code NO_YELLOW_CANDIDATE} carries an empty list. Every other terminal carries none of them. Raw and mask
 * evidence dimensions are identical and equal the scan rect span. Byte arrays are defensively copied.</p>
 *
 * <p>This is one of the standalone yellow-target contract-cohort types; it is not yet a
 * {@code RemoteLocalMacroResultPayload} variant, so the generic LOCAL_MACRO sealed permits and codec are left
 * untouched until the shared wiring lands.</p>
 */
@Value
@Jacksonized
public class RemoteNpcYellowTargetMacroResultPayload {
    Status status;
    List<RemoteYellowCandidate> candidates;
    byte[] rawPngBytes;
    String rawSha256;
    Integer rawWidth;
    Integer rawHeight;
    byte[] maskPngBytes;
    String maskSha256;
    Integer maskWidth;
    Integer maskHeight;
    Integer scanLeft;
    Integer scanTop;
    Integer scanRight;
    Integer scanBottom;

    @Builder
    public RemoteNpcYellowTargetMacroResultPayload(
            Status status,
            List<RemoteYellowCandidate> candidates,
            byte[] rawPngBytes,
            String rawSha256,
            Integer rawWidth,
            Integer rawHeight,
            byte[] maskPngBytes,
            String maskSha256,
            Integer maskWidth,
            Integer maskHeight,
            Integer scanLeft,
            Integer scanTop,
            Integer scanRight,
            Integer scanBottom) {
        if (status == null) {
            throw new IllegalArgumentException("status must not be null");
        }
        List<RemoteYellowCandidate> candidateCopy = candidates == null ? List.of() : List.copyOf(candidates);
        byte[] rawCopy = rawPngBytes == null ? null : rawPngBytes.clone();
        byte[] maskCopy = maskPngBytes == null ? null : maskPngBytes.clone();
        boolean carriesEvidence = status == Status.CAPTURED || status == Status.NO_YELLOW_CANDIDATE;
        if (carriesEvidence) {
            if (rawCopy == null || rawSha256 == null || rawWidth == null || rawHeight == null
                    || maskCopy == null || maskSha256 == null || maskWidth == null || maskHeight == null
                    || scanLeft == null || scanTop == null || scanRight == null || scanBottom == null) {
                throw new IllegalArgumentException(
                        "evidence-carrying result requires raw/mask bytes, SHA, dimensions and scan rect");
            }
            if (rawCopy.length == 0 || rawSha256.isBlank() || maskCopy.length == 0 || maskSha256.isBlank()) {
                throw new IllegalArgumentException("evidence bytes and SHA must be non-empty");
            }
            int spanWidth = scanRight - scanLeft;
            int spanHeight = scanBottom - scanTop;
            if (spanWidth <= 0 || spanHeight <= 0) {
                throw new IllegalArgumentException("scan rect must be a positive-area span");
            }
            if (!rawWidth.equals(maskWidth) || !rawHeight.equals(maskHeight)) {
                throw new IllegalArgumentException("raw and mask evidence dimensions must be identical");
            }
            if (rawWidth != spanWidth || rawHeight != spanHeight) {
                throw new IllegalArgumentException("evidence dimensions must equal the scan rect span");
            }
            // Mirror the released ImageEvidence self-validation: each evidence variant must decode as a PNG
            // whose actual dimensions equal the declared ones and whose recomputed SHA-256 equals the wire
            // hash, so corrupt bytes or a spoofed dimension fail here rather than downstream.
            verifyEvidencePng(rawCopy, rawSha256, rawWidth, rawHeight, "raw");
            verifyEvidencePng(maskCopy, maskSha256, maskWidth, maskHeight, "mask");
            if (status == Status.CAPTURED && candidateCopy.isEmpty()) {
                throw new IllegalArgumentException("CAPTURED must carry a non-empty candidate list");
            }
            if (status == Status.NO_YELLOW_CANDIDATE && !candidateCopy.isEmpty()) {
                throw new IllegalArgumentException("NO_YELLOW_CANDIDATE must not carry candidates");
            }
        } else if (!candidateCopy.isEmpty() || rawCopy != null || rawSha256 != null
                || rawWidth != null || rawHeight != null
                || maskCopy != null || maskSha256 != null || maskWidth != null || maskHeight != null
                || scanLeft != null || scanTop != null || scanRight != null || scanBottom != null) {
            throw new IllegalArgumentException("non-evidence terminal must not carry candidates, evidence or rect");
        }
        this.status = status;
        this.candidates = candidateCopy;
        this.rawPngBytes = rawCopy;
        this.rawSha256 = rawSha256;
        this.rawWidth = rawWidth;
        this.rawHeight = rawHeight;
        this.maskPngBytes = maskCopy;
        this.maskSha256 = maskSha256;
        this.maskWidth = maskWidth;
        this.maskHeight = maskHeight;
        this.scanLeft = scanLeft;
        this.scanTop = scanTop;
        this.scanRight = scanRight;
        this.scanBottom = scanBottom;
    }

    private static void verifyEvidencePng(byte[] bytes, String sha256, int declaredWidth, int declaredHeight,
                                          String name) {
        // Strict PNG: verify the standard 8-byte PNG signature (89 50 4E 47 0D 0A 1A 0A) before decoding so a
        // non-PNG format that ImageIO could still decode is rejected here.
        if (bytes.length < 8
                || (bytes[0] & 0xFF) != 0x89 || bytes[1] != 0x50 || bytes[2] != 0x4E || bytes[3] != 0x47
                || bytes[4] != 0x0D || bytes[5] != 0x0A || bytes[6] != 0x1A || bytes[7] != 0x0A) {
            throw new IllegalArgumentException(name + " evidence bytes do not carry the standard PNG signature");
        }
        BufferedImage decoded;
        try {
            decoded = ImageIO.read(new ByteArrayInputStream(bytes));
        } catch (IOException e) {
            throw new IllegalArgumentException(name + " evidence bytes are not decodable PNG", e);
        }
        if (decoded == null) {
            throw new IllegalArgumentException(name + " evidence bytes are not a PNG image");
        }
        try {
            if (decoded.getWidth() != declaredWidth || decoded.getHeight() != declaredHeight) {
                throw new IllegalArgumentException(name + " evidence dimensions do not match the PNG bytes");
            }
            if (!sha256Hex(bytes).equalsIgnoreCase(sha256)) {
                throw new IllegalArgumentException(name + " evidence SHA-256 does not match the PNG bytes");
            }
        } finally {
            decoded.flush();
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
            throw new IllegalArgumentException("SHA-256 unavailable while verifying npc yellow-target evidence", e);
        }
    }

    public byte[] getRawPngBytes() {
        return rawPngBytes == null ? null : rawPngBytes.clone();
    }

    public byte[] getMaskPngBytes() {
        return maskPngBytes == null ? null : maskPngBytes.clone();
    }

    public enum Status {
        CAPTURED,
        NO_YELLOW_CANDIDATE,
        CAPTURE_UNAVAILABLE,
        BINDING_UNAVAILABLE,
        INTERRUPTED,
        MECHANICS_FAILED
    }

    /**
     * One closed screen-absolute yellow-text candidate, mirroring the local mechanics {@code YellowCandidate}
     * field-for-field: pure geometry, no OCR text and no verdict.
     */
    @Value
    @Jacksonized
    public static class RemoteYellowCandidate {
        int rectLeft;
        int rectTop;
        int rectRight;
        int rectBottom;
        int textCenterX;
        int textCenterY;
        int clickX;
        int clickY;
        int score;
        String reason;

        @Builder
        public RemoteYellowCandidate(int rectLeft, int rectTop, int rectRight, int rectBottom,
                                     int textCenterX, int textCenterY, int clickX, int clickY,
                                     int score, String reason) {
            this.rectLeft = rectLeft;
            this.rectTop = rectTop;
            this.rectRight = rectRight;
            this.rectBottom = rectBottom;
            this.textCenterX = textCenterX;
            this.textCenterY = textCenterY;
            this.clickX = clickX;
            this.clickY = clickY;
            this.score = score;
            this.reason = reason;
        }
    }
}
