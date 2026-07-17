package com.bot.dhxy.cloud.remote;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.List;

/**
 * Strict typed EXECUTED result for {@code LOCAL_MACRO / DIALOG_OPTION_OCR_WORDS}. Mirrors the Cloud closed
 * {@code DialogOptionOcrWordsMacroResult} field-for-field: {@code status} is one of the five committed
 * mechanical outcomes and never folds a provider-unavailable or provider-exception terminal into a visual
 * {@link Status#NO_WORDS}. Only a {@link Status#WORDS} terminal carries a non-empty, provider-order,
 * image-local {@link RemoteWordBox} list; every other status carries no boxes. The list is defensively
 * copied to an immutable list.
 */
@Value
@Jacksonized
public class RemoteDialogOptionOcrWordsMacroResultPayload implements RemoteLocalMacroResultPayload {
    RemoteLocalMacroKind macroKind;
    Status status;
    List<RemoteWordBox> wordBoxes;

    @Builder
    public RemoteDialogOptionOcrWordsMacroResultPayload(
            RemoteLocalMacroKind macroKind,
            Status status,
            List<RemoteWordBox> wordBoxes) {
        if (macroKind != RemoteLocalMacroKind.DIALOG_OPTION_OCR_WORDS) {
            throw new IllegalArgumentException("macroKind must be DIALOG_OPTION_OCR_WORDS");
        }
        if (status == null) {
            throw new IllegalArgumentException("status must not be null");
        }
        List<RemoteWordBox> boxCopy = wordBoxes == null ? null : List.copyOf(wordBoxes);
        boolean words = status == Status.WORDS;
        if (words && (boxCopy == null || boxCopy.isEmpty())) {
            throw new IllegalArgumentException("WORDS result must carry at least one word box");
        }
        if (!words && boxCopy != null) {
            throw new IllegalArgumentException("non-WORDS result must not carry word boxes");
        }
        this.macroKind = macroKind;
        this.status = status;
        this.wordBoxes = boxCopy;
    }

    public enum Status {
        WORDS,
        NO_WORDS,
        OCR_UNAVAILABLE,
        INVALID_IMAGE,
        MECHANICS_FAILED
    }

    /** One immutable image-local OCR word box, mirroring the local mechanics {@code WordBox} field-for-field. */
    @Value
    @Jacksonized
    public static class RemoteWordBox {
        String text;
        int x;
        int y;
        int left;
        int top;
        int width;
        int height;
        double score;

        @Builder
        public RemoteWordBox(String text, int x, int y, int left, int top, int width, int height, double score) {
            this.text = text == null ? "" : text;
            this.x = x;
            this.y = y;
            this.left = left;
            this.top = top;
            this.width = width;
            this.height = height;
            this.score = score;
        }
    }
}
