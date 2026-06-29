package com.bot.dhxy.service;

import com.bot.dhxy.model.maintenance.SummonSkillCleanupRequest;

public class SummonSkillStartIndexPolicyTest {

    public static void main(String[] args) {
        cachedStartBeforeDefaultTailIsRespected();
        defaultTailStartIsUsedWhenNoCacheExists();
        outOfRangeCachedStartStopsAtSlotLength();
    }

    private static void cachedStartBeforeDefaultTailIsRespected() {
        SummonSkillCleanupRequest request = SummonSkillCleanupRequest.builder()
                .expectedSkillCount(6)
                .startSlotIndex(0)
                .build();

        int start = SummonSkillService.resolveStartIndex(request, 6, 3, 6);

        assertEquals("cached start before default tail should be respected", 0, start);
    }

    private static void defaultTailStartIsUsedWhenNoCacheExists() {
        int start = SummonSkillService.resolveStartIndex(SummonSkillCleanupRequest.defaults(), 6, 3, 6);

        assertEquals("missing cache uses default tail start", 3, start);
    }

    private static void outOfRangeCachedStartStopsAtSlotLength() {
        SummonSkillCleanupRequest request = SummonSkillCleanupRequest.builder()
                .expectedSkillCount(6)
                .startSlotIndex(9)
                .build();

        int start = SummonSkillService.resolveStartIndex(request, 6, 3, 6);

        assertEquals("cached start past slot length clamps to slot length", 6, start);
    }

    private static void assertEquals(String label, int expected, int actual) {
        if (expected != actual) {
            throw new AssertionError(label + " expected=" + expected + " actual=" + actual);
        }
    }
}
