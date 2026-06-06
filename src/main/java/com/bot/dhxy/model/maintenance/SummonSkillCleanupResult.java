package com.bot.dhxy.model.maintenance;

import lombok.Builder;
import lombok.Value;

import java.util.Collections;
import java.util.Map;

/**
 * Result for one summon-skill cleanup pass.
 *
 * @param success true when the pass reached a safe stopping point and maintenance cooldown may be
 *                refreshed.
 * @param skillCount detected slot count, usually 6 or 8.
 * @param nextStartIndex zero-based slot index where the next pass should resume. It may equal
 *                       skillCount when all tail slots are known to be keep skills.
 * @param observedStatusesByIndex zero-based slot statuses observed during this pass.
 * @param ultimateGenerateClicked true when the right-corner "点击可" point was clicked.
 * @param ultimateGenerateSucceeded true only when clicking "点击可" changed an empty slot into a
 *                                  real generated skill.
 * @param inspectedCount number of center-hover slot inspections performed.
 * @param deletedCount number of normal skills deleted during this pass.
 * @param message short diagnostic message.
 */
@Value
@Builder
public class SummonSkillCleanupResult {
    boolean success;
    int skillCount;
    int nextStartIndex;

    @Builder.Default
    Map<Integer, SummonSkillSlotStatus> observedStatusesByIndex = Collections.emptyMap();

    boolean ultimateGenerateClicked;
    boolean ultimateGenerateSucceeded;
    int inspectedCount;
    int deletedCount;
    String message;

    public static SummonSkillCleanupResult failed(String message) {
        return SummonSkillCleanupResult.builder()
                .success(false)
                .message(message)
                .build();
    }
}
