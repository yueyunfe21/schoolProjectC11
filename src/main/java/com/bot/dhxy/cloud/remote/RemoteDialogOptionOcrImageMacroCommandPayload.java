package com.bot.dhxy.cloud.remote;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

/**
 * Closed wire command for the {@code LOCAL_MACRO / DIALOG_OPTION_OCR_IMAGE} macro, mirroring the Cloud
 * {@code DialogOptionOcrImageMacroCommand} field-for-field. It has three legal forms so a fresh capture can
 * use the caller's detection rect even without a frame:
 * <ul>
 *   <li>SUPPLIED: {@code suppliedFramePngBytes} + its {@code suppliedFrameSha256} + its full positive-area
 *       screen-absolute rect — reused after the handler re-verifies its hash before the mechanics;</li>
 *   <li>FRESH_AT_RECT: no frame/SHA but a full positive-area rect — capture once at that exact rect;</li>
 *   <li>FRESH_DEFAULT: no frame/SHA/rect — capture once at the committed default dialog rect.</li>
 * </ul>
 *
 * <p>The frame and its non-blank {@code suppliedFrameSha256} are present-together and require the rect; a
 * rect may stand alone for a fresh capture. {@code source} is the optional diagnostic label. Bytes are
 * defensively copied on construction and on read; no owner/session/queue/retry, no color selection and no
 * target lives here.</p>
 */
@Value
@Jacksonized
public class RemoteDialogOptionOcrImageMacroCommandPayload implements RemoteLocalMacroCommandPayload {
    RemoteLocalMacroKind macroKind;
    byte[] suppliedFramePngBytes;
    String suppliedFrameSha256;
    Integer rectLeft;
    Integer rectTop;
    Integer rectRight;
    Integer rectBottom;
    String source;

    @Builder
    public RemoteDialogOptionOcrImageMacroCommandPayload(
            RemoteLocalMacroKind macroKind,
            byte[] suppliedFramePngBytes,
            String suppliedFrameSha256,
            Integer rectLeft,
            Integer rectTop,
            Integer rectRight,
            Integer rectBottom,
            String source) {
        if (macroKind != RemoteLocalMacroKind.DIALOG_OPTION_OCR_IMAGE) {
            throw new IllegalArgumentException("macroKind must be DIALOG_OPTION_OCR_IMAGE");
        }
        byte[] frameCopy = suppliedFramePngBytes == null ? null : suppliedFramePngBytes.clone();
        boolean hasFrame = frameCopy != null;
        boolean hasSha = suppliedFrameSha256 != null && !suppliedFrameSha256.isBlank();
        boolean anySha = suppliedFrameSha256 != null;
        boolean hasRect = rectLeft != null && rectTop != null
                && rectRight != null && rectBottom != null;
        boolean anyRect = rectLeft != null || rectTop != null
                || rectRight != null || rectBottom != null;
        if (anyRect != hasRect) {
            throw new IllegalArgumentException("rect must be a full quad or fully absent");
        }
        if (hasFrame != (anySha && hasSha)) {
            throw new IllegalArgumentException(
                    "a supplied frame and its non-blank SHA-256 must both be present or both absent");
        }
        if (hasFrame && !hasRect) {
            throw new IllegalArgumentException("a supplied frame requires its screen-absolute rect");
        }
        if (hasRect && (rectRight <= rectLeft || rectBottom <= rectTop)) {
            throw new IllegalArgumentException("rect must be a positive-area rect");
        }
        this.macroKind = macroKind;
        this.suppliedFramePngBytes = frameCopy;
        this.suppliedFrameSha256 = suppliedFrameSha256;
        this.rectLeft = rectLeft;
        this.rectTop = rectTop;
        this.rectRight = rectRight;
        this.rectBottom = rectBottom;
        this.source = source;
    }

    public byte[] getSuppliedFramePngBytes() {
        return suppliedFramePngBytes == null ? null : suppliedFramePngBytes.clone();
    }
}
