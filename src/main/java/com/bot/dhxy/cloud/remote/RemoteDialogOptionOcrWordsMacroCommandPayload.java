package com.bot.dhxy.cloud.remote;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

/**
 * Closed wire command for the {@code LOCAL_MACRO / DIALOG_OPTION_OCR_WORDS} macro. Mirrors the Cloud
 * {@code DialogOptionOcrWordsMacroCommand} field-for-field so the handler can rebuild the committed
 * {@code DialogOptionOcrWordsLocalObservationMechanics} single-variant entry: one already-selected
 * {@link ColorVariant} PNG (bytes + SHA-256 + dimensions) plus its screen-absolute rect and a diagnostic
 * label. The Cloud caller owns green-first/yellow-fallback selection, alias/keyword matching, merge,
 * fallback, action and click; this command never selects a color, alias, target or fallback.
 *
 * <p>The variant PNG bytes carry their own SHA-256 and width/height, and the rect is a full positive-area
 * screen-absolute quad whose span equals the dimensions. Bytes are defensively copied on construction and on
 * read; no owner/session/queue/retry lives here.</p>
 */
@Value
@Jacksonized
public class RemoteDialogOptionOcrWordsMacroCommandPayload implements RemoteLocalMacroCommandPayload {
    RemoteLocalMacroKind macroKind;
    ColorVariant variant;
    byte[] variantPngBytes;
    String variantSha256;
    int imageWidth;
    int imageHeight;
    int rectLeft;
    int rectTop;
    int rectRight;
    int rectBottom;
    String source;

    @Builder
    public RemoteDialogOptionOcrWordsMacroCommandPayload(
            RemoteLocalMacroKind macroKind,
            ColorVariant variant,
            byte[] variantPngBytes,
            String variantSha256,
            int imageWidth,
            int imageHeight,
            int rectLeft,
            int rectTop,
            int rectRight,
            int rectBottom,
            String source) {
        if (macroKind != RemoteLocalMacroKind.DIALOG_OPTION_OCR_WORDS) {
            throw new IllegalArgumentException("macroKind must be DIALOG_OPTION_OCR_WORDS");
        }
        if (variant == null) {
            throw new IllegalArgumentException("variant must not be null");
        }
        byte[] variantCopy = variantPngBytes == null ? null : variantPngBytes.clone();
        if (variantCopy == null || variantCopy.length == 0) {
            throw new IllegalArgumentException("variant PNG bytes must be present");
        }
        if (variantSha256 == null || variantSha256.isBlank()) {
            throw new IllegalArgumentException("variantSha256 must be present");
        }
        if (imageWidth <= 0 || imageHeight <= 0) {
            throw new IllegalArgumentException("variant dimensions must be positive");
        }
        long rectWidth = (long) rectRight - (long) rectLeft;
        long rectHeight = (long) rectBottom - (long) rectTop;
        if (rectWidth <= 0 || rectHeight <= 0) {
            throw new IllegalArgumentException("variant rect must enclose a positive area");
        }
        if (rectWidth != (long) imageWidth || rectHeight != (long) imageHeight) {
            throw new IllegalArgumentException("variant dimensions must equal the rect span");
        }
        this.macroKind = macroKind;
        this.variant = variant;
        this.variantPngBytes = variantCopy;
        this.variantSha256 = variantSha256;
        this.imageWidth = imageWidth;
        this.imageHeight = imageHeight;
        this.rectLeft = rectLeft;
        this.rectTop = rectTop;
        this.rectRight = rectRight;
        this.rectBottom = rectBottom;
        this.source = source;
    }

    public byte[] getVariantPngBytes() {
        return variantPngBytes == null ? null : variantPngBytes.clone();
    }

    /**
     * Already-selected variant label; the Cloud selects it, never this command's producer. {@code RAW} is the
     * committed baseline pass that OCRs the raw frame when the green wash is unavailable.
     */
    public enum ColorVariant {
        GREEN,
        YELLOW,
        RAW
    }
}
