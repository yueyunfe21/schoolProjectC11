package com.bot.dhxy.model.ocr;

import java.awt.Point;

/**
 * Matched player-name anchor extracted from OCR words.
 *
 * @param anchor screen/window coordinate supplied by the caller's scan coordinate system.
 * @param matchedText raw OCR text that matched the player name or name fragment.
 * @param matchedFragment player-name fragment used for the match.
 * @param matchMode matching mode, for example whole-name core or fragment.
 * @param compensationX horizontal correction applied when only a fragment matched.
 * @param textRect OCR text rectangle in the same coordinate space as {@code anchor}.
 * @param score OCR confidence score when available.
 */
public record PlayerAnchorMatch(Point anchor,
                                String matchedText,
                                String matchedFragment,
                                String matchMode,
                                int compensationX,
                                OcrWindowRegion textRect,
                                double score) {
    /**
     * @return compact diagnostic text for click-formula and vision-memory logs.
     */
    public String toDetailText() {
        return "anchor=" + (anchor == null ? "null" : anchor.x + "," + anchor.y)
                + ", text=" + matchedText
                + ", fragment=" + matchedFragment
                + ", mode=" + matchMode
                + ", compensationX=" + compensationX
                + ", textRect=" + (textRect == null ? "-" : textRect.toShortText())
                + ", score=" + String.format("%.3f", score);
    }
}
