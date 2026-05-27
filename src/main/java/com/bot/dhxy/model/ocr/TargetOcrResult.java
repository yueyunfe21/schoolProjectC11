package com.bot.dhxy.model.ocr;

/**
 * Result of matching yellow OCR lines against a target NPC name.
 *
 * @param lineResult selected line scan result in source-image coordinates.
 * @param hit true when fuzzy matching accepted the target.
 * @param editDistance normalized-name edit distance between OCR and expected text.
 * @param longestCommonSubstring longest common substring length between OCR and expected text.
 * @param normalizedTarget normalized expected name.
 * @param normalizedText normalized OCR text for the selected line.
 */
public record TargetOcrResult(OcrLineResult lineResult,
                              boolean hit,
                              int editDistance,
                              int longestCommonSubstring,
                              String normalizedTarget,
                              String normalizedText) {
    /**
     * @return compact diagnostic text including selected line, normalized OCR text, and match score.
     */
    public String toDetailText() {
        return (lineResult == null ? "-" : lineResult.toDetailText())
                + ", target=" + normalizedTarget
                + ", normalizedText=" + normalizedText
                + ", hit=" + hit
                + ", dist=" + editDistance
                + ", common=" + longestCommonSubstring;
    }
}
