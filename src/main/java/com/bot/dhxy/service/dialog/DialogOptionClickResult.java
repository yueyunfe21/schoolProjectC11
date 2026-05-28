package com.bot.dhxy.service.dialog;

import lombok.Builder;
import lombok.Value;

/**
 * Result for a dialog option click that also exposes the clicked point.
 *
 * @param result coarse dialog handling result.
 * @param relativeX clicked X relative to the captured dialog rectangle; null when no concrete point
 *                  was clicked or the click came from a fallback that should not be learned.
 * @param relativeY clicked Y relative to the captured dialog rectangle; null when no concrete point
 *                  was clicked or the click came from a fallback that should not be learned.
 * @param absoluteX screen-absolute clicked X; diagnostic only.
 * @param absoluteY screen-absolute clicked Y; diagnostic only.
 * @param matchedText OCR text that produced the click, if any.
 */
@Value
@Builder
public class DialogOptionClickResult {
    DialogHandleResult result;
    Integer relativeX;
    Integer relativeY;
    Integer absoluteX;
    Integer absoluteY;
    String matchedText;

    public static DialogOptionClickResult of(DialogHandleResult result) {
        return DialogOptionClickResult.builder()
                .result(result)
                .build();
    }

    public boolean hasLearnableClickPoint() {
        return result == DialogHandleResult.OPTION_KEYWORD_CLICKED
                && relativeX != null
                && relativeY != null;
    }
}
