package com.bot.dhxy.cloud.remote;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

/**
 * Strict typed EXECUTED result for {@code LOCAL_MACRO / DIALOG_WHITE_STORY_TEMPLATE}. Mirrors the Cloud closed
 * {@code DialogWhiteStoryTemplateMacroResult} field-for-field so the Cloud can rebuild the exact baseline
 * {@code prepareWhiteStoryTemplateOrAbsent} return without re-running any vision.
 *
 * <p>Only {@link State#MATCHED} carries the matched template ({@code matchedTemplateName} nullable, exactly
 * as {@code actionKey(spec.name())}), the window-rect-relative point, the screen-absolute point and the
 * same-frame evidence (frame PNG bytes + SHA-256 + dimensions). {@link State#STORY_MISS} and
 * {@link State#STORY_ABSENT} carry ONLY the detection rect and its frame dimensions so the Cloud rebuilds
 * the committed rect-centred miss/absent marker; they carry no match point/template/frame bytes. Every other
 * terminal carries no payload. Frame bytes are defensively copied and excluded from the outcome digest (the
 * SHA-256 covers integrity, canonical JSON forbids binary nodes).</p>
 */
@Value
@Jacksonized
public class RemoteDialogWhiteStoryTemplateMacroResultPayload implements RemoteLocalMacroResultPayload {
    RemoteLocalMacroKind macroKind;
    State state;
    String matchedTemplateName;
    String matchedTemplatePath;
    Integer relativeX;
    Integer relativeY;
    Integer absoluteX;
    Integer absoluteY;
    Integer frameLeft;
    Integer frameTop;
    Integer frameRight;
    Integer frameBottom;
    byte[] framePngBytes;
    String frameSha256;
    Integer frameWidth;
    Integer frameHeight;

    @Builder
    public RemoteDialogWhiteStoryTemplateMacroResultPayload(
            RemoteLocalMacroKind macroKind,
            State state,
            String matchedTemplateName,
            String matchedTemplatePath,
            Integer relativeX,
            Integer relativeY,
            Integer absoluteX,
            Integer absoluteY,
            Integer frameLeft,
            Integer frameTop,
            Integer frameRight,
            Integer frameBottom,
            byte[] framePngBytes,
            String frameSha256,
            Integer frameWidth,
            Integer frameHeight) {
        if (macroKind != RemoteLocalMacroKind.DIALOG_WHITE_STORY_TEMPLATE) {
            throw new IllegalArgumentException("macroKind must be DIALOG_WHITE_STORY_TEMPLATE");
        }
        if (state == null) {
            throw new IllegalArgumentException("state must not be null");
        }
        byte[] frameCopy = framePngBytes == null ? null : framePngBytes.clone();
        boolean matched = state == State.MATCHED;
        boolean frameOnly = state == State.STORY_MISS || state == State.STORY_ABSENT;

        boolean hasRect = frameLeft != null && frameTop != null && frameRight != null && frameBottom != null;
        boolean anyRect = frameLeft != null || frameTop != null || frameRight != null || frameBottom != null;
        boolean hasDims = frameWidth != null && frameHeight != null;
        boolean anyDims = frameWidth != null || frameHeight != null;
        boolean hasMatchPoint = relativeX != null && relativeY != null
                && absoluteX != null && absoluteY != null;
        boolean anyMatchPoint = relativeX != null || relativeY != null
                || absoluteX != null || absoluteY != null;
        boolean anyMatchedOnly = matchedTemplateName != null || matchedTemplatePath != null
                || anyMatchPoint || frameCopy != null || frameSha256 != null;

        if (anyRect != hasRect) {
            throw new IllegalArgumentException("frame rect must be a full quad or fully absent");
        }
        if (anyDims != hasDims) {
            throw new IllegalArgumentException("frame dimensions must be a full pair or fully absent");
        }

        if (matched) {
            if (matchedTemplatePath == null || !hasMatchPoint || !hasRect || frameCopy == null
                    || frameSha256 == null || !hasDims) {
                throw new IllegalArgumentException(
                        "MATCHED result requires template path, match point, rect, frame bytes, hash and dimensions");
            }
            if (matchedTemplatePath.isBlank()) {
                throw new IllegalArgumentException("MATCHED result requires a non-blank template path");
            }
            if (frameCopy.length == 0 || frameSha256.isBlank() || frameWidth <= 0 || frameHeight <= 0) {
                throw new IllegalArgumentException(
                        "MATCHED result requires present frame bytes, hash and positive dimensions");
            }
            requireRectSpansDimensions(frameLeft, frameTop, frameRight, frameBottom, frameWidth, frameHeight);
            if (relativeX != absoluteX - frameLeft || relativeY != absoluteY - frameTop) {
                throw new IllegalArgumentException(
                        "MATCHED relative point must equal the absolute point minus the rect origin");
            }
        } else if (frameOnly) {
            if (!hasRect || !hasDims) {
                throw new IllegalArgumentException(
                        "STORY_MISS/STORY_ABSENT result requires the detection rect and frame dimensions");
            }
            if (matchedTemplateName != null || matchedTemplatePath != null || anyMatchPoint
                    || frameCopy != null || frameSha256 != null) {
                throw new IllegalArgumentException(
                        "STORY_MISS/STORY_ABSENT result must not carry any matched-only payload");
            }
            if (frameWidth <= 0 || frameHeight <= 0) {
                throw new IllegalArgumentException(
                        "STORY_MISS/STORY_ABSENT result requires positive frame dimensions");
            }
            requireRectSpansDimensions(frameLeft, frameTop, frameRight, frameBottom, frameWidth, frameHeight);
        } else {
            if (anyMatchedOnly || anyRect || anyDims) {
                throw new IllegalArgumentException(
                        "CAPTURE_UNAVAILABLE/BINDING_UNAVAILABLE/MECHANICS_FAILED result must carry no payload");
            }
        }

        this.macroKind = macroKind;
        this.state = state;
        this.matchedTemplateName = matchedTemplateName;
        this.matchedTemplatePath = matchedTemplatePath;
        this.relativeX = relativeX;
        this.relativeY = relativeY;
        this.absoluteX = absoluteX;
        this.absoluteY = absoluteY;
        this.frameLeft = frameLeft;
        this.frameTop = frameTop;
        this.frameRight = frameRight;
        this.frameBottom = frameBottom;
        this.framePngBytes = frameCopy;
        this.frameSha256 = frameSha256;
        this.frameWidth = frameWidth;
        this.frameHeight = frameHeight;
    }

    private static void requireRectSpansDimensions(int left, int top, int right, int bottom,
                                                   int width, int height) {
        long rectWidth = (long) right - (long) left;
        long rectHeight = (long) bottom - (long) top;
        if (rectWidth <= 0 || rectHeight <= 0) {
            throw new IllegalArgumentException("frame rect must enclose a positive area");
        }
        if (rectWidth != (long) width || rectHeight != (long) height) {
            throw new IllegalArgumentException("frame dimensions must equal the detection rect span");
        }
    }

    public byte[] getFramePngBytes() {
        return framePngBytes == null ? null : framePngBytes.clone();
    }

    public enum State {
        MATCHED,
        STORY_MISS,
        STORY_ABSENT,
        CAPTURE_UNAVAILABLE,
        BINDING_UNAVAILABLE,
        MECHANICS_FAILED
    }
}
