package com.bot.dhxy.model.ocr;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.Accessors;

import java.awt.Point;

/**
 * Shape-based candidate for an NPC-name-like text line in a washed image.
 *
 * @param region image-local rectangle around the candidate text line.
 * @param clickPoint image-local suggested click point below the candidate name.
 * @param score higher means the shape looks more like a text line and less like noise/UI frame.
 * @param pixelCount foreground black pixels inside {@code region}.
 * @param componentCount connected foreground fragments inside {@code region}.
 * @param density foreground density inside {@code region}.
 * @param longRowCount number of long horizontal runs, used to penalize frame lines.
 * @param longColumnCount number of long vertical runs, used to penalize frame lines.
 * @param reason compact score explanation for logs/debug UI.
 */
@Value
@Builder
@AllArgsConstructor(access = AccessLevel.PUBLIC)
@Accessors(fluent = true)
public class TextCandidate {
    OcrWindowRegion region;
    Point clickPoint;
    int score;
    int pixelCount;
    int componentCount;
    double density;
    int longRowCount;
    int longColumnCount;
    String reason;

    /**
     * @return compact debug text for logs.
     */
    public String toSummaryText() {
        return "region=" + region.toShortText()
                + ", click=(" + clickPoint.x + "," + clickPoint.y + ")"
                + ", score=" + score
                + ", " + reason;
    }

}
