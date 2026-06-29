package com.bot.dhxy.service;

import com.bot.dhxy.model.maintenance.SummonSkillSlotStatus;

import java.util.function.BooleanSupplier;

/**
 * Restores the old summon-skill tail-boundary rule for locked tail slots.
 *
 * <p>A locked tail slot does not by itself prove the tail is safe. The cleanup pass must scan
 * backward until it reaches the nearest opened slot. A normal skill there should be deleted and the
 * emptied slot still needs the caller's forced ultimate-corner check. An empty slot also needs that
 * same ultimate-corner check. A keep skill or no opened previous slot is a safe stop. Unknown state,
 * timeout, or delete failure is not safe and must not refresh the long maintenance cooldown.</p>
 */
final class SummonSkillTailBoundaryScanner {

    private SummonSkillTailBoundaryScanner() {
    }

    static Result scanLockedBoundary(int lockedIndex,
                                     SlotInspector inspector,
                                     SlotDeleter deleter,
                                     BooleanSupplier shouldAbort) {
        int inspectedCount = 0;
        for (int i = lockedIndex - 1; i >= 0; i--) {
            if (shouldAbort.getAsBoolean()) {
                return Result.failed(i, inspectedCount, 0, "locked boundary backward scan timed out");
            }

            SummonSkillSlotStatus status = inspector.inspect(i);
            inspectedCount++;
            if (status == SummonSkillSlotStatus.NORMAL_SKILL) {
                if (shouldAbort.getAsBoolean()) {
                    return Result.failed(i, inspectedCount, 0, "locked boundary delete timed out");
                }
                if (!deleter.delete(i)) {
                    return Result.failed(i, inspectedCount, 0, "locked boundary previous normal delete failed");
                }
                return Result.deleted(i, inspectedCount, i);
            }
            if (status == SummonSkillSlotStatus.KEEP_SKILL) {
                return Result.safeStop(i + 1, inspectedCount, "locked boundary stopped at previous keep skill");
            }
            if (status == SummonSkillSlotStatus.EMPTY_SLOT) {
                return Result.needsUltimateCheck(i, inspectedCount, "locked boundary stopped at previous empty slot");
            }
            if (status == SummonSkillSlotStatus.LOCKED_SLOT) {
                continue;
            }
            return Result.failed(i, inspectedCount, 0, "locked boundary previous slot unknown");
        }
        return Result.safeStop(0, inspectedCount, "locked boundary found no previous opened slot");
    }

    interface SlotInspector {
        SummonSkillSlotStatus inspect(int index);
    }

    interface SlotDeleter {
        boolean delete(int index);
    }

    record Result(boolean success,
                  int nextStartIndex,
                  int inspectedCount,
                  int deletedCount,
                  Integer deletedIndex,
                  Integer ultimateCheckIndex,
                  String message) {
        private static Result deleted(int index, int inspectedCount, int ultimateCheckIndex) {
            return new Result(true, index, inspectedCount, 1, index, ultimateCheckIndex,
                    "locked boundary previous normal deleted");
        }

        private static Result needsUltimateCheck(int index, int inspectedCount, String message) {
            return new Result(true, index, inspectedCount, 0, null, index, message);
        }

        private static Result safeStop(int nextStartIndex, int inspectedCount, String message) {
            return new Result(true, nextStartIndex, inspectedCount, 0, null, null, message);
        }

        private static Result failed(int nextStartIndex,
                                     int inspectedCount,
                                     int deletedCount,
                                     String message) {
            return new Result(false, nextStartIndex, inspectedCount, deletedCount, null, null, message);
        }
    }
}
