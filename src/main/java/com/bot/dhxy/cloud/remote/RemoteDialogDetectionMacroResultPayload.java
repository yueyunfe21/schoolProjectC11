package com.bot.dhxy.cloud.remote;

import com.bot.dhxy.model.dialog.DialogType;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

/**
 * Strict typed EXECUTED result for {@code LOCAL_MACRO / DIALOG_DETECTION}. Mirrors the Cloud closed
 * {@code DialogDetectionMacroResult}: {@code state} is one of the five committed mechanical outcomes, and
 * only a {@link State#CAPTURED} terminal carries the dialog payload (classified {@code dialogType}, the
 * screen-absolute rectangle, the PNG frame bytes plus SHA-256 and width/height, and the classification
 * metrics with the story text-line-pattern stats flattened into five fields). Every non-{@code CAPTURED}
 * terminal carries no dialog field, byte-for-byte matching the local {@code DialogDetectionResult}
 * invariant so the Cloud can rebuild an identical {@code DialogDetection} or {@code none()}.
 *
 * <p>The optional metric fields may be {@code null} even on a {@code CAPTURED} terminal, exactly as the
 * committed classifier leaves them {@code null} for an early {@code OPTION}/{@code NONE} decision.</p>
 */
@Value
@Jacksonized
public class RemoteDialogDetectionMacroResultPayload implements RemoteLocalMacroResultPayload {
    RemoteLocalMacroKind macroKind;
    State state;
    DialogType dialogType;
    Integer dialogLeft;
    Integer dialogTop;
    Integer dialogRight;
    Integer dialogBottom;
    byte[] framePngBytes;
    String frameSha256;
    Integer frameWidth;
    Integer frameHeight;
    Double maskStddev;
    Integer optionGreenCount;
    Integer storyThinWhiteCount;
    Integer storyGreenCount;
    Boolean storyTextMatched;
    Integer storyQualifyingRows;
    Integer storyMaxWhitePixelsInRow;
    Integer storyMaxClustersInRow;
    Integer storyMaxSpanInRow;

    @Builder
    public RemoteDialogDetectionMacroResultPayload(
            RemoteLocalMacroKind macroKind,
            State state,
            DialogType dialogType,
            Integer dialogLeft,
            Integer dialogTop,
            Integer dialogRight,
            Integer dialogBottom,
            byte[] framePngBytes,
            String frameSha256,
            Integer frameWidth,
            Integer frameHeight,
            Double maskStddev,
            Integer optionGreenCount,
            Integer storyThinWhiteCount,
            Integer storyGreenCount,
            Boolean storyTextMatched,
            Integer storyQualifyingRows,
            Integer storyMaxWhitePixelsInRow,
            Integer storyMaxClustersInRow,
            Integer storyMaxSpanInRow) {
        if (macroKind != RemoteLocalMacroKind.DIALOG_DETECTION) {
            throw new IllegalArgumentException("macroKind must be DIALOG_DETECTION");
        }
        if (state == null) {
            throw new IllegalArgumentException("state must not be null");
        }
        byte[] frameCopy = framePngBytes == null ? null : framePngBytes.clone();
        boolean captured = state == State.CAPTURED;
        boolean hasAllCoreFields = dialogType != null
                && dialogLeft != null && dialogTop != null && dialogRight != null && dialogBottom != null
                && frameCopy != null && frameSha256 != null
                && frameWidth != null && frameHeight != null
                && maskStddev != null;
        boolean hasAnyField = dialogType != null
                || dialogLeft != null || dialogTop != null || dialogRight != null || dialogBottom != null
                || frameCopy != null || frameSha256 != null
                || frameWidth != null || frameHeight != null
                || maskStddev != null || optionGreenCount != null
                || storyThinWhiteCount != null || storyGreenCount != null
                || storyTextMatched != null || storyQualifyingRows != null
                || storyMaxWhitePixelsInRow != null || storyMaxClustersInRow != null
                || storyMaxSpanInRow != null;
        if (captured && !hasAllCoreFields) {
            throw new IllegalArgumentException("CAPTURED result must carry all core dialog fields");
        }
        if (!captured && hasAnyField) {
            throw new IllegalArgumentException("non-CAPTURED result must not carry any dialog field");
        }
        if (captured && (frameCopy.length == 0 || frameSha256.isBlank()
                || frameWidth <= 0 || frameHeight <= 0)) {
            throw new IllegalArgumentException("invalid CAPTURED frame image, dimensions, or hash");
        }
        this.macroKind = macroKind;
        this.state = state;
        this.dialogType = dialogType;
        this.dialogLeft = dialogLeft;
        this.dialogTop = dialogTop;
        this.dialogRight = dialogRight;
        this.dialogBottom = dialogBottom;
        this.framePngBytes = frameCopy;
        this.frameSha256 = frameSha256;
        this.frameWidth = frameWidth;
        this.frameHeight = frameHeight;
        this.maskStddev = maskStddev;
        this.optionGreenCount = optionGreenCount;
        this.storyThinWhiteCount = storyThinWhiteCount;
        this.storyGreenCount = storyGreenCount;
        this.storyTextMatched = storyTextMatched;
        this.storyQualifyingRows = storyQualifyingRows;
        this.storyMaxWhitePixelsInRow = storyMaxWhitePixelsInRow;
        this.storyMaxClustersInRow = storyMaxClustersInRow;
        this.storyMaxSpanInRow = storyMaxSpanInRow;
    }

    public byte[] getFramePngBytes() {
        return framePngBytes == null ? null : framePngBytes.clone();
    }

    public enum State {
        CAPTURED,
        CAPTURE_UNAVAILABLE,
        PRE_CAPTURE_INTERRUPTED,
        NON_INPUT_WORKER,
        MECHANICS_FAILED
    }
}
