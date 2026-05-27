package com.bot.dhxy.model.ocr;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.Accessors;

import java.nio.file.Path;
import java.util.List;

/**
 * OCR output for one packed colored-text scan.
 *
 * @param path debug image path for the packed OCR input.
 * @param variantName preprocessing variant name.
 * @param blackPixelCount number of foreground pixels written to the packed image.
 * @param wordCount number of OCR words mapped back to source coordinates.
 * @param wordsSummary compact diagnostic string for logs/UI.
 * @param words OCR words in the original source image coordinate space.
 */
@Value
@Builder
@AllArgsConstructor(access = AccessLevel.PUBLIC)
@Accessors(fluent = true)
public class OcrLineResult {
    String path;
    String variantName;
    int blackPixelCount;
    int wordCount;
    String wordsSummary;
    List<OcrWordResult> words;

    /**
     * Build an empty scan result after no foreground line was detected.
     *
     * @param outputPath debug image path where a 1x1 blank image was written.
     * @param variantName preprocessing variant name.
     * @return empty OCR result with zero words.
     */
    public static OcrLineResult empty(Path outputPath, String variantName) {
        return new OcrLineResult(outputPath == null ? null : outputPath.toString(),
                variantName, 0, 0, "-", List.of());
    }

    /**
     * @return all OCR word texts concatenated in OCR order.
     */
    public String joinedText() {
        if (words == null || words.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (OcrWordResult word : words) {
            if (word != null && word.getText() != null) {
                builder.append(word.getText());
            }
        }
        return builder.toString();
    }

    /**
     * @return compact diagnostic text for logs and debug UI.
     */
    public String toDetailText() {
        return "variant=" + variantName
                + ", path=" + path
                + ", blackPixels=" + blackPixelCount
                + ", words=" + wordCount
                + ", text=" + (wordsSummary == null || wordsSummary.isBlank() ? "-" : wordsSummary);
    }

}
