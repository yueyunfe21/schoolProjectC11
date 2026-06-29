package com.bot.dhxy.model.tasktracker;

import lombok.Builder;
import lombok.Value;

/**
 * Result of a small-area task-tracker cache verification.
 *
 * @param matched true when the current small crop still matches the cached tracker-green
 *                fingerprint.
 * @param distance bit-level fingerprint distance; {@link Integer#MAX_VALUE} means no comparable
 *                 current fingerprint was produced.
 * @param maxDistance maximum accepted distance for this fast path.
 * @param score diagnostic score in [0, 1], derived from the fingerprint distance.
 * @param elapsedMs verification elapsed time in milliseconds.
 * @param debugImagePath optional marked image path for replay/debug inspection.
 * @param reason short diagnostic reason for hit/miss.
 */
@Value
@Builder
public class TaskTrackerFastMatchResult {
    boolean matched;
    int distance;
    int maxDistance;
    double score;
    long elapsedMs;
    String debugImagePath;
    String reason;
}
