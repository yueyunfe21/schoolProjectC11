package com.bot.dhxy.task.wubei;

import com.bot.dhxy.window.model.WindowReadyEventType;
import lombok.Builder;
import lombok.Value;

import java.util.Set;

/**
 * Scheduling policy returned by a 五倍 phase when it can safely park after releasing the task turn.
 *
 * @param reason scheduling reason for the wait; never a business success/failure result.
 * @param wakeTypes window ready event types that may wake this wait; empty means timeout-only.
 * @param timeoutMs maximum park duration in milliseconds before the phase rechecks runtime state;
 *                  negative means wait until a runner event or task interruption.
 * @param minParkMs optional minimum park duration in milliseconds to avoid immediate churn.
 * @param currentWindowOnly whether wake events should be scoped to the current bound window.
 * @param allowOpportunisticMaintenance whether maintenance may run while this wait is parked.
 */
@Value
@Builder(toBuilder = true)
public class WubeiWaitSpec {
    WubeiWaitReason reason;

    @Builder.Default
    Set<WindowReadyEventType> wakeTypes = Set.of();

    @Builder.Default
    long timeoutMs = 0L;

    @Builder.Default
    long minParkMs = 0L;

    @Builder.Default
    boolean currentWindowOnly = true;

    @Builder.Default
    boolean allowOpportunisticMaintenance = true;
}
