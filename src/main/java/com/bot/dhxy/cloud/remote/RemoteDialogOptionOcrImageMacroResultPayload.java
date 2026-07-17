package com.bot.dhxy.cloud.remote;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

/**
 * Strict typed EXECUTED result for {@code LOCAL_MACRO / DIALOG_OPTION_OCR_IMAGE}. Mirrors the Cloud closed
 * {@code DialogOptionOcrImageMacroResult} field-for-field: {@code status} is one of the five committed
 * mechanical outcomes, and only a {@link Status#CAPTURED} terminal carries same-frame image evidence — the
 * mandatory raw PNG bytes with their SHA-256, the shared width/height and the screen-absolute scan rect, plus
 * the OPTIONAL green and yellow washed variants (each present-together with its own SHA-256, or both absent
 * when that wash was unavailable). Every non-{@code CAPTURED} terminal carries only a {@code reason},
 * byte-for-byte matching the local result invariant so the Cloud can rebuild an identical result. Bytes are
 * defensively copied.
 */
@Value
@Jacksonized
public class RemoteDialogOptionOcrImageMacroResultPayload implements RemoteLocalMacroResultPayload {
    RemoteLocalMacroKind macroKind;
    Status status;
    byte[] rawPngBytes;
    String rawSha256;
    byte[] greenPngBytes;
    String greenSha256;
    byte[] yellowPngBytes;
    String yellowSha256;
    Integer imageWidth;
    Integer imageHeight;
    Integer scanLeft;
    Integer scanTop;
    Integer scanRight;
    Integer scanBottom;
    String reason;

    @Builder
    public RemoteDialogOptionOcrImageMacroResultPayload(
            RemoteLocalMacroKind macroKind,
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
        if (macroKind != RemoteLocalMacroKind.DIALOG_OPTION_OCR_IMAGE) {
            throw new IllegalArgumentException("macroKind must be DIALOG_OPTION_OCR_IMAGE");
        }
        if (status == null) {
            throw new IllegalArgumentException("status must not be null");
        }
        byte[] rawCopy = rawPngBytes == null ? null : rawPngBytes.clone();
        byte[] greenCopy = greenPngBytes == null ? null : greenPngBytes.clone();
        byte[] yellowCopy = yellowPngBytes == null ? null : yellowPngBytes.clone();
        boolean captured = status == Status.CAPTURED;
        // Raw is the single mandatory variant; green and yellow are optional (a wash can be unavailable).
        boolean hasRawCore = rawCopy != null && rawSha256 != null
                && imageWidth != null && imageHeight != null
                && scanLeft != null && scanTop != null && scanRight != null && scanBottom != null;
        boolean anyEvidence = rawCopy != null || greenCopy != null || yellowCopy != null
                || rawSha256 != null || greenSha256 != null || yellowSha256 != null
                || imageWidth != null || imageHeight != null
                || scanLeft != null || scanTop != null || scanRight != null || scanBottom != null;
        if (captured && !hasRawCore) {
            throw new IllegalArgumentException(
                    "CAPTURED result must carry the raw frame, its SHA, dimensions and scan rect");
        }
        if (!captured && anyEvidence) {
            throw new IllegalArgumentException("non-CAPTURED result must carry only a reason");
        }
        if (captured) {
            if (rawCopy.length == 0 || rawSha256.isBlank() || imageWidth <= 0 || imageHeight <= 0) {
                throw new IllegalArgumentException("invalid CAPTURED raw bytes, hash or dimensions");
            }
            long scanWidth = (long) scanRight - (long) scanLeft;
            long scanHeight = (long) scanBottom - (long) scanTop;
            if (scanWidth <= 0 || scanHeight <= 0) {
                throw new IllegalArgumentException("CAPTURED scan rect must enclose a positive area");
            }
            if (scanWidth != (long) imageWidth || scanHeight != (long) imageHeight) {
                throw new IllegalArgumentException("CAPTURED image dimensions must equal the scan rect span");
            }
            // Green and yellow are optional; each present variant carries a non-blank SHA and non-empty bytes.
            if ((greenCopy != null) != (greenSha256 != null)) {
                throw new IllegalArgumentException("green bytes and SHA-256 must be present or absent together");
            }
            if (greenCopy != null && (greenCopy.length == 0 || greenSha256.isBlank())) {
                throw new IllegalArgumentException("green variant, when present, must be non-empty");
            }
            if ((yellowCopy != null) != (yellowSha256 != null)) {
                throw new IllegalArgumentException("yellow bytes and SHA-256 must be present or absent together");
            }
            if (yellowCopy != null && (yellowCopy.length == 0 || yellowSha256.isBlank())) {
                throw new IllegalArgumentException("yellow variant, when present, must be non-empty");
            }
        }
        this.macroKind = macroKind;
        this.status = status;
        this.rawPngBytes = rawCopy;
        this.rawSha256 = rawSha256;
        this.greenPngBytes = greenCopy;
        this.greenSha256 = greenSha256;
        this.yellowPngBytes = yellowCopy;
        this.yellowSha256 = yellowSha256;
        this.imageWidth = imageWidth;
        this.imageHeight = imageHeight;
        this.scanLeft = scanLeft;
        this.scanTop = scanTop;
        this.scanRight = scanRight;
        this.scanBottom = scanBottom;
        this.reason = reason;
    }

    public byte[] getRawPngBytes() {
        return rawPngBytes == null ? null : rawPngBytes.clone();
    }

    public byte[] getGreenPngBytes() {
        return greenPngBytes == null ? null : greenPngBytes.clone();
    }

    public byte[] getYellowPngBytes() {
        return yellowPngBytes == null ? null : yellowPngBytes.clone();
    }

    public enum Status {
        CAPTURED,
        CAPTURE_UNAVAILABLE,
        BINDING_UNAVAILABLE,
        INVALID_SUPPLIED_FRAME,
        MECHANICS_FAILED
    }
}
