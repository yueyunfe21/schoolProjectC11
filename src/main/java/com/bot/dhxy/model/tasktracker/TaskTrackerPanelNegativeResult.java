package com.bot.dhxy.model.tasktracker;

import com.bot.dhxy.model.ocr.OcrWindowRegion;
import com.bot.dhxy.task.model.TaskType;
import lombok.Builder;
import lombok.Value;

import java.util.Locale;
import java.util.Objects;

/**
 * Fresh negative result from a Runner-owned task-tracker read.
 *
 * @param windowId bound runtime window id that owns this read; nullable before Runner binds it.
 * @param taskType running task type that requested the tracker read; nullable before Runner binds it.
 * @param taskCode tracker-reader task code such as {@code wuhuan}; not the Java task code
 *                 {@code wuhuan_v2}. Used to prevent cross-task negative consumption.
 * @param status old 五环 tracker miss status equivalent produced by a successful no-action read.
 * @param source diagnostic source of the read.
 * @param reason cloud/local diagnostic reason that explains the no-action result.
 * @param trackerPanelRegion tracker panel crop in window-relative pixels; nullable when unavailable.
 * @param wuhuanTrackerBlockRegion 五环 task block ROI in window-relative pixels; nullable when the
 *                                 negative is task-not-found or the block could not be localized.
 * @param observedAtMs epoch millis when the tracker read produced this negative.
 * @param sequence runtime-local sequence assigned when stored; zero before storage.
 */
@Value
@Builder(toBuilder = true)
public class TaskTrackerPanelNegativeResult {
    String windowId;
    TaskType taskType;
    String taskCode;
    Status status;
    String source;
    String reason;
    OcrWindowRegion trackerPanelRegion;
    OcrWindowRegion wuhuanTrackerBlockRegion;
    @Builder.Default
    long observedAtMs = System.currentTimeMillis();
    long sequence;

    public boolean matches(String expectedWindowId, TaskType expectedTaskType, String expectedTaskCode) {
        return status != null
                && Objects.equals(windowId, expectedWindowId)
                && taskType == expectedTaskType
                && Objects.equals(normalize(taskCode), normalize(expectedTaskCode));
    }

    public boolean freshWithin(long nowMs, long maxAgeMs) {
        if (observedAtMs <= 0L) {
            return false;
        }
        if (maxAgeMs < 0L) {
            return true;
        }
        return Math.max(0L, nowMs - observedAtMs) <= maxAgeMs;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    public enum Status {
        TASK_NOT_FOUND,
        TASK_FOUND_NO_GREEN,
        TASK_FOUND_NO_LINK
    }
}
