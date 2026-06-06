package com.bot.dhxy.task.wuhuan;

import com.bot.dhxy.model.ocr.OcrWindowRegion;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.Accessors;

/**
 * Immutable-by-copy state for one Five-ring V2 run.
 *
 * @param phase current phase to execute next.
 * @param round one-based five-ring run number inside this task execution.
 * @param source diagnostic source describing how this state was produced.
 * @param shoeBagIndex remembered main-bag page index for the shoe item; null means unknown.
 * @param taskAccepted true once this round has confirmed that the character already owns a 五环
 *                     task, either by clicking the accept option or by finding the task panel entry.
 * @param trackerPanelRegion cached task-tracker panel region in window-relative pixels.
 * @param wuhuanTrackerBlockRegion cached 五环 task block region in window-relative pixels.
 * @param waitingAcceptNpcPathing true after accept-NPC navigation has yielded while the character is
 *                                walking toward 云游大师; the same ACCEPT_TASK phase should only
 *                                observe movement until this clears.
 * @param pathingMovementObserved true after WAIT_PATHING has seen at least one real movement tick.
 * @param combatObservedSincePathing true once combat was seen after the latest tracker/pathing click.
 * @param phaseRetryCount retry count for the current phase after local cleanup/retry.
 * @param uiErrorCount consecutive task-panel/pathing UI errors.
 */
@Value
@Builder
@AllArgsConstructor(access = AccessLevel.PUBLIC)
@Accessors(fluent = true)
public class FiveRingPhaseContext {
    FiveRingPhase phase;
    int round;
    String source;
    Integer shoeBagIndex;
    boolean taskAccepted;
    OcrWindowRegion trackerPanelRegion;
    OcrWindowRegion wuhuanTrackerBlockRegion;
    boolean waitingAcceptNpcPathing;
    boolean pathingMovementObserved;
    boolean combatObservedSincePathing;
    int phaseRetryCount;
    int uiErrorCount;

    public static FiveRingPhaseContext start(int round) {
        return new FiveRingPhaseContext(FiveRingPhase.PREPARE, round, "normal-start", null,
                false, null, null, false, false, false, 0, 0);
    }

    public FiveRingPhaseContext next(FiveRingPhase nextPhase, String nextSource) {
        return new FiveRingPhaseContext(nextPhase, round, nextSource, shoeBagIndex,
                taskAccepted, trackerPanelRegion, wuhuanTrackerBlockRegion, false, false, false, 0, uiErrorCount);
    }

    public FiveRingPhaseContext withShoeBagIndex(Integer nextShoeBagIndex, String nextSource) {
        return new FiveRingPhaseContext(phase, round, nextSource, nextShoeBagIndex,
                taskAccepted, trackerPanelRegion, wuhuanTrackerBlockRegion,
                waitingAcceptNpcPathing, pathingMovementObserved, combatObservedSincePathing,
                phaseRetryCount, uiErrorCount);
    }

    public FiveRingPhaseContext withTaskAccepted(String nextSource) {
        return new FiveRingPhaseContext(phase, round, nextSource, shoeBagIndex,
                true, trackerPanelRegion, wuhuanTrackerBlockRegion,
                waitingAcceptNpcPathing, pathingMovementObserved, combatObservedSincePathing,
                phaseRetryCount, uiErrorCount);
    }

    public FiveRingPhaseContext withTrackerRegions(OcrWindowRegion nextTrackerPanelRegion,
                                                   OcrWindowRegion nextWuhuanTrackerBlockRegion,
                                                   String nextSource) {
        return new FiveRingPhaseContext(phase, round, nextSource, shoeBagIndex,
                taskAccepted, nextTrackerPanelRegion, nextWuhuanTrackerBlockRegion,
                waitingAcceptNpcPathing, pathingMovementObserved, combatObservedSincePathing,
                phaseRetryCount, uiErrorCount);
    }

    public FiveRingPhaseContext clearWuhuanTrackerBlockRegion(String nextSource) {
        return new FiveRingPhaseContext(phase, round, nextSource, shoeBagIndex,
                taskAccepted, trackerPanelRegion, null,
                waitingAcceptNpcPathing, pathingMovementObserved, combatObservedSincePathing,
                phaseRetryCount, uiErrorCount);
    }

    public FiveRingPhaseContext waitForAcceptNpcPathing(String nextSource) {
        return new FiveRingPhaseContext(phase, round, nextSource, shoeBagIndex,
                taskAccepted, trackerPanelRegion, wuhuanTrackerBlockRegion,
                true, pathingMovementObserved, combatObservedSincePathing, phaseRetryCount, uiErrorCount);
    }

    public FiveRingPhaseContext clearAcceptNpcPathingWait(String nextSource) {
        return new FiveRingPhaseContext(phase, round, nextSource, shoeBagIndex,
                taskAccepted, trackerPanelRegion, wuhuanTrackerBlockRegion,
                false, pathingMovementObserved, combatObservedSincePathing, phaseRetryCount, uiErrorCount);
    }

    public FiveRingPhaseContext withPathingMovementObserved(String nextSource) {
        return new FiveRingPhaseContext(phase, round, nextSource, shoeBagIndex,
                taskAccepted, trackerPanelRegion, wuhuanTrackerBlockRegion,
                waitingAcceptNpcPathing, true, combatObservedSincePathing, phaseRetryCount, uiErrorCount);
    }

    public FiveRingPhaseContext withCombatObservedSincePathing(String nextSource) {
        return new FiveRingPhaseContext(phase, round, nextSource, shoeBagIndex,
                taskAccepted, trackerPanelRegion, wuhuanTrackerBlockRegion,
                waitingAcceptNpcPathing, pathingMovementObserved, true, phaseRetryCount, uiErrorCount);
    }

    public FiveRingPhaseContext retrySamePhase(String nextSource) {
        return new FiveRingPhaseContext(phase, round, nextSource, shoeBagIndex,
                taskAccepted, trackerPanelRegion, wuhuanTrackerBlockRegion,
                waitingAcceptNpcPathing, pathingMovementObserved, combatObservedSincePathing,
                phaseRetryCount + 1, uiErrorCount);
    }

    public FiveRingPhaseContext resetUiErrorCount(String nextSource) {
        return new FiveRingPhaseContext(phase, round, nextSource, shoeBagIndex,
                taskAccepted, trackerPanelRegion, wuhuanTrackerBlockRegion,
                waitingAcceptNpcPathing, pathingMovementObserved, combatObservedSincePathing,
                phaseRetryCount, 0);
    }

    public FiveRingPhaseContext increaseUiErrorCount(String nextSource) {
        return new FiveRingPhaseContext(phase, round, nextSource, shoeBagIndex,
                taskAccepted, trackerPanelRegion, wuhuanTrackerBlockRegion,
                waitingAcceptNpcPathing, pathingMovementObserved, combatObservedSincePathing,
                phaseRetryCount, uiErrorCount + 1);
    }
}
