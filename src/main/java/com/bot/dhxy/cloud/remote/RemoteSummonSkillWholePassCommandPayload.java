package com.bot.dhxy.cloud.remote;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

/** Closed business intent for one retained summon-skill whole pass. */
@Value
@Builder
@Jacksonized
public class RemoteSummonSkillWholePassCommandPayload {

    Integer expectedSkillCount;
    boolean trustExpectedSkillCount;
    Integer startSlotIndex;
    boolean skipUltimateCornerCheck;
    String exclusiveSessionId;
    long bindingGeneration;

    public RemoteSummonSkillWholePassCommandPayload(
            Integer expectedSkillCount,
            boolean trustExpectedSkillCount,
            Integer startSlotIndex,
            boolean skipUltimateCornerCheck,
            String exclusiveSessionId,
            long bindingGeneration) {
        if (expectedSkillCount != null && expectedSkillCount <= 0) {
            throw new IllegalArgumentException("expectedSkillCount must be positive when present");
        }
        if (startSlotIndex != null && startSlotIndex < 0) {
            throw new IllegalArgumentException("startSlotIndex must not be negative");
        }
        if (exclusiveSessionId == null || exclusiveSessionId.isBlank()
                || !exclusiveSessionId.equals(exclusiveSessionId.trim())) {
            throw new IllegalArgumentException("exclusiveSessionId must be canonical non-blank text");
        }
        if (bindingGeneration < 0L) {
            throw new IllegalArgumentException("bindingGeneration must not be negative");
        }
        this.expectedSkillCount = expectedSkillCount;
        this.trustExpectedSkillCount = trustExpectedSkillCount;
        this.startSlotIndex = startSlotIndex;
        this.skipUltimateCornerCheck = skipUltimateCornerCheck;
        this.exclusiveSessionId = exclusiveSessionId;
        this.bindingGeneration = bindingGeneration;
    }
}
