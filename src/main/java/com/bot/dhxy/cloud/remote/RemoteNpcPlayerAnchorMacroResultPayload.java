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
import java.util.HexFormat;

/**
 * Strict typed player-anchor result payload. Mirrors the Cloud closed {@code NpcPlayerAnchorMacroResult}:
 * {@code state} is one of the six committed mechanical terminals, and both {@link State#CAPTURED} and
 * {@link State#NO_PURPLE_BLOB} carry the same-frame evidence (the raw post-mask source PNG and the purple
 * washed-mask PNG, each with SHA-256 and width/height) plus the screen-absolute scan rect; only
 * {@code CAPTURED} additionally carries the screen-absolute purple blob. Every other terminal carries no
 * blob, evidence or rect, byte-for-byte matching the local
 * {@code NpcClickPlayerAnchorLocalObservationMechanics.Result} invariant.
 *
 * <p>This is a standalone contract that self-proves its payload: the constructor strictly decodes each PNG
 * (rejecting non-PNG or undecodable bytes), verifies the decoded dimensions equal the declared width/height,
 * recomputes and matches the SHA-256, and enforces every blob/scan-rect invariant, flushing each decoded
 * image in a {@code finally}. It deliberately does not implement the shared
 * {@code RemoteLocalMacroResultPayload} sealed hierarchy or carry a {@code RemoteLocalMacroKind}; wiring it
 * into the transport is a separate downstream integration step documented in the cohort report.</p>
 */
@Value
@Jacksonized
public class RemoteNpcPlayerAnchorMacroResultPayload {

    // Baseline NpcClickService purple-blob dark-pixel bounds, value-for-value.
    private static final int PURPLE_BLOB_MIN_PIXELS = 20;
    private static final int PURPLE_BLOB_MAX_PIXELS = 6000;
    // 8-byte PNG signature (89 50 4E 47 0D 0A 1A 0A).
    private static final byte[] PNG_MAGIC = {
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A
    };

    State state;
    Integer blobRectLeft;
    Integer blobRectTop;
    Integer blobRectRight;
    Integer blobRectBottom;
    Integer blobAnchorX;
    Integer blobAnchorY;
    Integer blobDarkPixels;
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
    public RemoteNpcPlayerAnchorMacroResultPayload(
            State state,
            Integer blobRectLeft,
            Integer blobRectTop,
            Integer blobRectRight,
            Integer blobRectBottom,
            Integer blobAnchorX,
            Integer blobAnchorY,
            Integer blobDarkPixels,
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
        if (state == null) {
            throw new IllegalArgumentException("state must not be null");
        }
        byte[] rawCopy = rawPngBytes == null ? null : rawPngBytes.clone();
        byte[] maskCopy = maskPngBytes == null ? null : maskPngBytes.clone();

        boolean carriesEvidence = state == State.CAPTURED || state == State.NO_PURPLE_BLOB;
        boolean hasAllEvidence = rawCopy != null && rawSha256 != null && rawWidth != null && rawHeight != null
                && maskCopy != null && maskSha256 != null && maskWidth != null && maskHeight != null
                && scanLeft != null && scanTop != null && scanRight != null && scanBottom != null;
        boolean hasAnyEvidence = rawCopy != null || rawSha256 != null || rawWidth != null || rawHeight != null
                || maskCopy != null || maskSha256 != null || maskWidth != null || maskHeight != null
                || scanLeft != null || scanTop != null || scanRight != null || scanBottom != null;
        boolean hasAllBlob = blobRectLeft != null && blobRectTop != null && blobRectRight != null
                && blobRectBottom != null && blobAnchorX != null && blobAnchorY != null && blobDarkPixels != null;
        boolean hasAnyBlob = blobRectLeft != null || blobRectTop != null || blobRectRight != null
                || blobRectBottom != null || blobAnchorX != null || blobAnchorY != null || blobDarkPixels != null;

        if (carriesEvidence) {
            if (!hasAllEvidence) {
                throw new IllegalArgumentException(
                        "evidence-carrying result must carry both same-frame evidences and the scan rect");
            }
            // Strict per-frame self-proof: PNG signature, decodable image, decoded dims == declared, SHA match.
            validatePngEvidence(rawCopy, rawSha256, rawWidth, rawHeight, "raw");
            validatePngEvidence(maskCopy, maskSha256, maskWidth, maskHeight, "mask");
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
            if (state == State.CAPTURED) {
                if (!hasAllBlob) {
                    throw new IllegalArgumentException("CAPTURED result must carry a purple blob");
                }
                if (blobRectRight < blobRectLeft || blobRectBottom < blobRectTop) {
                    throw new IllegalArgumentException("purple blob rect must be a non-negative-area box");
                }
                if (blobAnchorX < blobRectLeft || blobAnchorX > blobRectRight
                        || blobAnchorY < blobRectTop || blobAnchorY > blobRectBottom) {
                    throw new IllegalArgumentException("purple blob anchor must lie inside the blob rect");
                }
                if (blobDarkPixels < PURPLE_BLOB_MIN_PIXELS || blobDarkPixels > PURPLE_BLOB_MAX_PIXELS) {
                    throw new IllegalArgumentException("purple blob dark-pixel count is outside the baseline bounds");
                }
                // scanRight/scanBottom are exclusive spans; the inclusive blob rect must stay strictly below them.
                if (blobRectLeft < scanLeft || blobRectTop < scanTop
                        || blobRectRight >= scanRight || blobRectBottom >= scanBottom) {
                    throw new IllegalArgumentException("CAPTURED blob rect must lie inside the scan rect");
                }
            } else if (hasAnyBlob) {
                throw new IllegalArgumentException("NO_PURPLE_BLOB result must not carry a blob");
            }
        } else if (hasAnyBlob || hasAnyEvidence) {
            throw new IllegalArgumentException("non-evidence terminal must not carry a blob, evidence, or rect");
        }

        this.state = state;
        this.blobRectLeft = blobRectLeft;
        this.blobRectTop = blobRectTop;
        this.blobRectRight = blobRectRight;
        this.blobRectBottom = blobRectBottom;
        this.blobAnchorX = blobAnchorX;
        this.blobAnchorY = blobAnchorY;
        this.blobDarkPixels = blobDarkPixels;
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

    private static void validatePngEvidence(byte[] png, String sha256, int width, int height, String label) {
        if (png.length == 0 || sha256.isBlank() || width <= 0 || height <= 0) {
            throw new IllegalArgumentException("invalid " + label + " evidence bytes, dimensions, or hash");
        }
        if (!hasPngMagic(png)) {
            throw new IllegalArgumentException(label + " evidence bytes do not carry the PNG signature");
        }
        BufferedImage decoded;
        try {
            decoded = ImageIO.read(new ByteArrayInputStream(png));
        } catch (IOException e) {
            throw new IllegalArgumentException(label + " evidence bytes are not decodable PNG", e);
        }
        if (decoded == null) {
            throw new IllegalArgumentException(label + " evidence bytes are not a PNG image");
        }
        try {
            if (decoded.getWidth() != width || decoded.getHeight() != height) {
                throw new IllegalArgumentException(label + " evidence dimensions do not match the PNG bytes");
            }
            if (!sha256Hex(png).equalsIgnoreCase(sha256)) {
                throw new IllegalArgumentException(label + " evidence SHA-256 does not match the PNG bytes");
            }
        } finally {
            decoded.flush();
        }
    }

    private static boolean hasPngMagic(byte[] bytes) {
        if (bytes.length < PNG_MAGIC.length) {
            return false;
        }
        for (int i = 0; i < PNG_MAGIC.length; i++) {
            if (bytes[i] != PNG_MAGIC[i]) {
                return false;
            }
        }
        return true;
    }

    private static String sha256Hex(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(bytes));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable while verifying player anchor frame", e);
        }
    }

    public byte[] getRawPngBytes() {
        return rawPngBytes == null ? null : rawPngBytes.clone();
    }

    public byte[] getMaskPngBytes() {
        return maskPngBytes == null ? null : maskPngBytes.clone();
    }

    public enum State {
        CAPTURED,
        NO_PURPLE_BLOB,
        CAPTURE_UNAVAILABLE,
        BINDING_UNAVAILABLE,
        INTERRUPTED,
        MECHANICS_FAILED
    }
}
