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
    long taskRunId;
    int round;
    String attemptId;
    long openedAtMs;

    public boolean sameIdentity(PreparedActionJob job) {
        if (job == null) {
            return false;
        }
        return taskRunId == job.getTaskRunId()
                && round == job.getRound()
                && windowId != null && windowId.equals(job.getWindowId())
                && hwnd != null && hwnd.equals(job.getHwnd())
                && attemptId != null && attemptId.equals(job.getAttemptId());
    }

    public String identityText() {
        return "windowId=" + windowId + " hwnd=" + hwnd + " taskRunId=" + taskRunId
                + " round=" + round + " attemptId=" + attemptId;
    }
}
