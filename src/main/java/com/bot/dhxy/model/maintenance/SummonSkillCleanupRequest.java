package com.bot.dhxy.model.maintenance;

import lombok.Builder;
import lombok.Value;

/**
 * Request for one focused summon-skill cleanup pass.
 *
 * @param expectedSkillCount cached slot count for the current window; null means detect normally.
 * @param trustExpectedSkillCount true when {@code expectedSkillCount} was refreshed recently enough
 *                                that the cleanup pass may skip the physical 6/8-slot hover probe.
 * @param startSlotIndex zero-based slot index to start scanning from after slot-count detection.
 *                       Null uses the service default tail start for the detected layout.
 * @param skipUltimateCornerCheck true when a recent successful "点击可" generation is still on
 *                                cooldown, so the right-corner hover/template check should be
 *                                skipped for this pass.
 */
@Value
@Builder(toBuilder = true)
public class SummonSkillCleanupRequest {
    Integer expectedSkillCount;
    boolean trustExpectedSkillCount;
    Integer startSlotIndex;

    @Builder.Default
    boolean skipUltimateCornerCheck = false;

    public static SummonSkillCleanupRequest defaults() {
        return SummonSkillCleanupRequest.builder().build();
    }
}
