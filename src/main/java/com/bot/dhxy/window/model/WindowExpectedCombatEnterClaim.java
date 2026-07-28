package com.bot.dhxy.window.model;

import java.util.Objects;

/**
 * Explicit expected-combat claim waiting to bind to the next local visible combat generation.
 */
public record WindowExpectedCombatEnterClaim(
        String claimId,
        String observationRunId,
        String businessTaskRunId,
        String taskCode,
        String attemptId,
        String windowId,
        String hwnd,
        String source,
        Long combatGeneration) {

    public WindowExpectedCombatEnterClaim {
        Objects.requireNonNull(claimId, "claimId");
        Objects.requireNonNull(observationRunId, "observationRunId");
        Objects.requireNonNull(businessTaskRunId, "businessTaskRunId");
        Objects.requireNonNull(taskCode, "taskCode");
        Objects.requireNonNull(attemptId, "attemptId");
        Objects.requireNonNull(windowId, "windowId");
        Objects.requireNonNull(hwnd, "hwnd");
        Objects.requireNonNull(source, "source");
    }

    public WindowExpectedCombatEnterClaim bind(long generation) {
        return new WindowExpectedCombatEnterClaim(
                claimId, observationRunId, businessTaskRunId, taskCode, attemptId,
                windowId, hwnd, source, generation);
    }
}
