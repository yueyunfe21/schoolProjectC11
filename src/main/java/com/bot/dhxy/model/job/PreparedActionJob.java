package com.bot.dhxy.model.job;

import lombok.Builder;
import lombok.Value;

/**
 * CR253 typed prepared job published by background observation for the 修罗 green-chain foreground.
 *
 * <p>This is deliberately NOT a dialog action: it carries no fingerprint or dialog frame data,
 * only the work type, the full scheduling identity, and an optional window-relative click payload.
 * Publication and consumption both validate the same identity
 * ({@code windowId/hwnd + taskRunId + round + attemptId + type}); any mismatch means the job
 * belongs to an older attempt/round/run and must be discarded without physical input.</p>
 *
 * @param type typed work kind; one producer and one consumer each.
 * @param windowId owning window runtime id.
 * @param hwnd native window handle at publish time.
 * @param taskRunId task run the green-chain attempt belongs to.
 * @param round one-based 修罗 round.
 * @param attemptId pathing intent id of the green click this job answers.
 * @param windowRelativeX window-relative click X for click jobs; null otherwise.
 * @param windowRelativeY window-relative click Y for click jobs; null otherwise.
 * @param matchedText recognition text/template label when produced by 看打 recognition.
 * @param reason producer-side decision reason (for example the cloud verdict reason).
 * @param source producing pipeline label for logs.
 * @param preparedAtMs epoch millis when the producer published this job.
 */
@Value
@Builder(toBuilder = true)
public class PreparedActionJob {
    PreparedActionJobType type;
    String windowId;
    String hwnd;
    long taskRunId;
    int round;
    String attemptId;
    Integer windowRelativeX;
    Integer windowRelativeY;
    String matchedText;
    String reason;
    String source;
    long preparedAtMs;

    public String identityText() {
        return "type=" + type + " windowId=" + windowId + " hwnd=" + hwnd
                + " taskRunId=" + taskRunId + " round=" + round + " attemptId=" + attemptId;
    }
}
