package com.bot.dhxy.model.job;

import lombok.Builder;
import lombok.Value;

/**
 * CR253 identity of the currently parked 修罗 green-chain attempt.
 *
 * <p>The foreground writes this when it clicks (or re-presses) the tracker green link; background
 * producers stamp and gate {@link PreparedActionJob}s against it, and the foreground re-validates
 * the same identity on consume (the double invalidation gate). A new attempt, round restart, stop,
 * binding/session loss, or confirmed combat entry clears it together with all pending jobs.</p>
 *
 * @param windowId owning window runtime id.
 * @param hwnd native window handle at green-click time.
 * @param taskRunId task run the attempt belongs to.
 * @param round one-based 修罗 round.
 * @param attemptId pathing intent id of the current green click.
 * @param openedAtMs epoch millis when this attempt's schedule was opened.
 */
@Value
@Builder(toBuilder = true)
public class XiuluoGreenChainSchedule {
    String windowId;
    String hwnd;
    String observationRunId;
    String taskRunId;
    int round;
    String attemptId;
    long openedAtMs;

    public boolean sameIdentity(PreparedActionJob job) {
        if (job == null) {
            return false;
        }
        return taskRunId != null && taskRunId.equals(job.getTaskRunId())
                && round == job.getRound()
                && windowId != null && windowId.equals(job.getWindowId())
                && hwnd != null && hwnd.equals(job.getHwnd())
                && attemptId != null && attemptId.equals(job.getAttemptId());
    }

    /**
     * TURN-40G review#5: the full five-field attempt identity ({@code windowId}, {@code hwnd},
     * {@code taskRunId}, {@code round}, {@code attemptId}). {@code openedAtMs} is a diagnostic timestamp,
     * not an identity field. A same-run replacement that changes {@code round} or {@code hwnd} (even while
     * loose ids collide or reuse an attemptId) is a different identity, so a stale holder is fenced out.
     */
    public boolean sameFullIdentity(XiuluoGreenChainSchedule other) {
        return other != null
                && taskRunId != null && taskRunId.equals(other.taskRunId)
                && java.util.Objects.equals(observationRunId, other.observationRunId)
                && round == other.round
                && windowId != null && windowId.equals(other.windowId)
                && hwnd != null && hwnd.equals(other.hwnd)
                && attemptId != null && attemptId.equals(other.attemptId);
    }

    public String identityText() {
        return "windowId=" + windowId + " hwnd=" + hwnd + " observationRunId=" + observationRunId
                + " taskRunId=" + taskRunId
                + " round=" + round + " attemptId=" + attemptId;
    }
}
