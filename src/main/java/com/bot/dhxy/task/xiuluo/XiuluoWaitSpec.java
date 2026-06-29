package com.bot.dhxy.task.xiuluo;

import com.bot.dhxy.window.model.WindowReadyEventType;
import lombok.Builder;
import lombok.Value;

import java.util.Set;

/**
 * Scheduling policy returned by 修罗 when it can park after releasing the task turn.
 *
 * @param reason diagnostic reason for the wait.
 * @param wakeTypes window ready event types that should wake the wait.
 * @param afterSequence ignore ready events at or below this sequence; capture this before returning
 *                      the wait outcome to avoid missing events between turn release and parking.
 * @param timeoutMs maximum wait duration in milliseconds; negative waits until event/interrupt.
 * @param pathingIntentId expected pathing intent id for PATHING_TERMINAL waits, nullable for
 *                        non-pathing waits.
 * @param pathingSourcePrefix expected pathing source prefix for diagnostics/fallback filtering.
 * @param pathingTargetMapName expected target map for diagnostics/fallback filtering.
 */
@Value
@Builder(toBuilder = true)
public class XiuluoWaitSpec {
    XiuluoWaitReason reason;

    @Builder.Default
    Set<WindowReadyEventType> wakeTypes = Set.of();

    @Builder.Default
    long afterSequence = 0L;

    @Builder.Default
    long timeoutMs = 0L;

    String pathingIntentId;
    String pathingSourcePrefix;
    String pathingTargetMapName;
}
