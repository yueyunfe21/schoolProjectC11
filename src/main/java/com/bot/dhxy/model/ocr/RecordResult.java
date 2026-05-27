package com.bot.dhxy.model.ocr;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.Accessors;

/**
 * Result returned after writing or skipping a vision-memory observation.
 *
 * @param recorded true when the observation was persisted.
 * @param key stable memory key that was used or skipped.
 * @param summary human-readable diagnostic summary.
 * @param recommendedRoi current recommended ROI text after the write, or {@code "-"} when skipped.
 */
@Value
@Builder
@AllArgsConstructor(access = AccessLevel.PUBLIC)
@Accessors(fluent = true)
public class RecordResult {
    boolean recorded;
    String key;
    String summary;
    String recommendedRoi;

    /**
     * Build a skipped result without changing memory.
     *
     * @param key memory key that could not be recorded.
     * @param reason skip reason for logs/UI.
     * @return skipped result.
     */
    public static RecordResult skipped(String key, String reason) {
        return new RecordResult(false, key, reason == null ? "" : reason, "-");
    }

}
