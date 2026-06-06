package com.bot.dhxy.service.dialog;

import com.bot.dhxy.model.dialog.DialogResultStatus;
import com.bot.dhxy.model.dialog.PreparedDialogAction;
import lombok.Builder;
import lombok.Value;

/**
 * Result for a dialog option click that also exposes the clicked point.
 *
 * @param result coarse dialog handling status.
 * @param relativeX clicked X relative to the captured dialog rectangle; null when no concrete point
 *                  was clicked or the click came from a fallback that should not be learned.
 * @param relativeY clicked Y relative to the captured dialog rectangle; null when no concrete point
 *                  was clicked or the click came from a fallback that should not be learned.
 * @param absoluteX screen-absolute clicked X; diagnostic only.
 * @param absoluteY screen-absolute clicked Y; diagnostic only.
 * @param matchedText OCR text that produced the click, if any.
 * @param preparedAction optional reusable dialog click candidate. Null for fallback clicks and
 *                       failed matches.
 */
@Value
@Builder
public class DialogOptionClickResult {
    DialogResultStatus result;
    Integer relativeX;
    Integer relativeY;
    Integer absoluteX;
    Integer absoluteY;
    String matchedText;
    PreparedDialogAction preparedAction;

    public static DialogOptionClickResult of(DialogResultStatus result) {
        return DialogOptionClickResult.builder()
                .result(result)
                .build();
    }

    public boolean hasLearnableClickPoint() {
        return result == DialogResultStatus.OPTION_KEYWORD_CLICKED
                && relativeX != null
                && relativeY != null;
    }
}
