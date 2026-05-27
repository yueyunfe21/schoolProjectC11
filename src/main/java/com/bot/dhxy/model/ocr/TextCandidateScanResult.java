package com.bot.dhxy.model.ocr;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.Accessors;

import java.awt.Point;
import java.util.List;

/**
 * Result object for washed-image text candidate extraction.
 *
 * @param status high-level scan status. A non-empty candidate list currently implies
 *               {@link TextCandidateScanStatus#FOUND_CANDIDATES}.
 * @param candidates sorted immutable image-local candidates.
 * @param overlayPath optional debug overlay path written by the scan.
 * @param message diagnostic message for empty or failed scans.
 */
@Value
@Builder
@AllArgsConstructor(access = AccessLevel.PUBLIC)
@Accessors(fluent = true)
public class TextCandidateScanResult {
    TextCandidateScanStatus status;
    List<TextCandidate> candidates;
    String overlayPath;
    String message;

    /**
     * Build a result from mutable local candidates.
     *
     * @param candidates already sorted mutable candidates owned by the extractor.
     * @param overlayPath optional overlay image path.
     * @return result whose candidate list is immutable and owned by the result.
     */
    public static TextCandidateScanResult of(List<TextCandidate> candidates, String overlayPath) {
        List<TextCandidate> immutable = candidates == null ? List.of() : List.copyOf(candidates);
        TextCandidateScanStatus status = immutable.isEmpty()
                ? TextCandidateScanStatus.NO_CANDIDATES
                : TextCandidateScanStatus.FOUND_CANDIDATES;
        return new TextCandidateScanResult(status, immutable, overlayPath,
                immutable.isEmpty() ? "no text-like candidate found" : "ok");
    }

    /**
     * Build an empty failed/invalid scan result.
     *
     * @param message reason for the empty result.
     * @return immutable empty result.
     */
    public static TextCandidateScanResult empty(String message) {
        return new TextCandidateScanResult(TextCandidateScanStatus.SCAN_FAILED, List.of(), null, message);
    }

    /**
     * @return sorted immutable candidate click points in image-local coordinates.
     */
    public List<Point> fallbackClickPoints() {
        return candidates.stream()
                .map(TextCandidate::clickPoint)
                .toList();
    }

}
