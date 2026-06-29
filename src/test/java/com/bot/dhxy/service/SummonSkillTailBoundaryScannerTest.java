package com.bot.dhxy.service;

import com.bot.dhxy.model.maintenance.SummonSkillSlotStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SummonSkillTailBoundaryScannerTest {

    public static void main(String[] args) {
        lockedBoundaryDeletesNearestPreviousNormal();
        lockedBoundaryUsesNearestPreviousEmptyForUltimateCheck();
        lockedBoundaryDeletesNearestPreviousNormalForUltimateCheck();
        lockedBoundaryStopsSafelyOnPreviousKeep();
        lockedBoundaryFailsOnUnknownPreviousSlot();
        lockedBoundaryFailsWhenDeleteFails();
        lockedBoundaryFailsWhenDeadlineAlreadyExpired();
    }

    private static void lockedBoundaryDeletesNearestPreviousNormal() {
        FakeSlots slots = new FakeSlots(Map.of(
                3, SummonSkillSlotStatus.LOCKED_SLOT,
                2, SummonSkillSlotStatus.NORMAL_SKILL));

        SummonSkillTailBoundaryScanner.Result result = SummonSkillTailBoundaryScanner.scanLockedBoundary(
                3, slots::inspect, slots::delete, () -> false);

        assertTrue("locked boundary with previous normal succeeds", result.success());
        assertEquals("inspects previous normal", 1, result.inspectedCount());
        assertEquals("deletes previous normal", 1, result.deletedCount());
        assertEquals("deleted slot index", Integer.valueOf(2), result.deletedIndex());
        assertEquals("deleted normal slot needs ultimate check", Integer.valueOf(2), result.ultimateCheckIndex());
        assertEquals("delete calls", List.of(2), slots.deletedIndexes);
    }

    private static void lockedBoundaryStopsSafelyOnPreviousKeep() {
        FakeSlots slots = new FakeSlots(Map.of(
                3, SummonSkillSlotStatus.LOCKED_SLOT,
                2, SummonSkillSlotStatus.KEEP_SKILL));

        SummonSkillTailBoundaryScanner.Result result = SummonSkillTailBoundaryScanner.scanLockedBoundary(
                3, slots::inspect, slots::delete, () -> false);

        assertTrue("locked boundary with previous keep succeeds", result.success());
        assertEquals("inspects previous keep", 1, result.inspectedCount());
        assertEquals("does not delete keep skill", 0, result.deletedCount());
        assertEquals("safe stop index", Integer.valueOf(3), result.nextStartIndex());
        assertEquals("keep boundary has no ultimate check", null, result.ultimateCheckIndex());
        assertEquals("no delete calls", List.of(), slots.deletedIndexes);
    }

    private static void lockedBoundaryUsesNearestPreviousEmptyForUltimateCheck() {
        FakeSlots slots = new FakeSlots(Map.of(
                3, SummonSkillSlotStatus.LOCKED_SLOT,
                2, SummonSkillSlotStatus.EMPTY_SLOT,
                1, SummonSkillSlotStatus.NORMAL_SKILL));

        SummonSkillTailBoundaryScanner.Result result = SummonSkillTailBoundaryScanner.scanLockedBoundary(
                3, slots::inspect, slots::delete, () -> false);

        assertTrue("locked boundary stops at nearest previous empty", result.success());
        assertEquals("inspects nearest empty only", 1, result.inspectedCount());
        assertEquals("does not skip empty to delete earlier normal", 0, result.deletedCount());
        assertEquals("empty slot needs ultimate check", Integer.valueOf(2), result.ultimateCheckIndex());
        assertEquals("no delete calls after empty boundary", List.of(), slots.deletedIndexes);
    }

    private static void lockedBoundaryDeletesNearestPreviousNormalForUltimateCheck() {
        FakeSlots slots = new FakeSlots(Map.of(
                3, SummonSkillSlotStatus.LOCKED_SLOT,
                2, SummonSkillSlotStatus.NORMAL_SKILL));

        SummonSkillTailBoundaryScanner.Result result = SummonSkillTailBoundaryScanner.scanLockedBoundary(
                3, slots::inspect, slots::delete, () -> false);

        assertTrue("locked boundary deletes previous normal", result.success());
        assertEquals("deleted normal can become ultimate-check slot", Integer.valueOf(2), result.ultimateCheckIndex());
        assertEquals("delete calls", List.of(2), slots.deletedIndexes);
    }

    private static void lockedBoundaryFailsOnUnknownPreviousSlot() {
        FakeSlots slots = new FakeSlots(Map.of(
                3, SummonSkillSlotStatus.LOCKED_SLOT,
                2, SummonSkillSlotStatus.UNKNOWN));

        SummonSkillTailBoundaryScanner.Result result = SummonSkillTailBoundaryScanner.scanLockedBoundary(
                3, slots::inspect, slots::delete, () -> false);

        assertFalse("unknown previous slot fails", result.success());
        assertEquals("unknown inspected once", 1, result.inspectedCount());
        assertEquals("unknown does not delete", 0, result.deletedCount());
    }

    private static void lockedBoundaryFailsWhenDeleteFails() {
        FakeSlots slots = new FakeSlots(Map.of(
                3, SummonSkillSlotStatus.LOCKED_SLOT,
                2, SummonSkillSlotStatus.NORMAL_SKILL), false);

        SummonSkillTailBoundaryScanner.Result result = SummonSkillTailBoundaryScanner.scanLockedBoundary(
                3, slots::inspect, slots::delete, () -> false);

        assertFalse("delete failure fails the pass", result.success());
        assertEquals("attempted delete once", List.of(2), slots.deletedIndexes);
    }

    private static void lockedBoundaryFailsWhenDeadlineAlreadyExpired() {
        FakeSlots slots = new FakeSlots(Map.of(
                3, SummonSkillSlotStatus.LOCKED_SLOT,
                2, SummonSkillSlotStatus.NORMAL_SKILL));

        SummonSkillTailBoundaryScanner.Result result = SummonSkillTailBoundaryScanner.scanLockedBoundary(
                3, slots::inspect, slots::delete, () -> true);

        assertFalse("deadline failure fails the pass", result.success());
        assertEquals("deadline blocks inspection", 0, result.inspectedCount());
        assertEquals("deadline blocks delete", 0, result.deletedCount());
    }

    private static void assertTrue(String label, boolean value) {
        if (!value) {
            throw new AssertionError(label + " expected true");
        }
    }

    private static void assertFalse(String label, boolean value) {
        if (value) {
            throw new AssertionError(label + " expected false");
        }
    }

    private static void assertEquals(String label, Object expected, Object actual) {
        if (expected == null ? actual != null : !expected.equals(actual)) {
            throw new AssertionError(label + " expected=" + expected + " actual=" + actual);
        }
    }

    private static class FakeSlots {
        private final Map<Integer, SummonSkillSlotStatus> statuses;
        private final boolean deleteSucceeds;
        private final List<Integer> deletedIndexes = new ArrayList<>();

        private FakeSlots(Map<Integer, SummonSkillSlotStatus> statuses) {
            this(statuses, true);
        }

        private FakeSlots(Map<Integer, SummonSkillSlotStatus> statuses, boolean deleteSucceeds) {
            this.statuses = statuses;
            this.deleteSucceeds = deleteSucceeds;
        }

        private SummonSkillSlotStatus inspect(int index) {
            return statuses.getOrDefault(index, SummonSkillSlotStatus.EMPTY_SLOT);
        }

        private boolean delete(int index) {
            deletedIndexes.add(index);
            return deleteSucceeds;
        }
    }
}
