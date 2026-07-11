package com.bot.dhxy.task.wuhuan;

import com.bot.dhxy.model.ocr.OcrWindowRegion;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.Accessors;

import java.awt.image.BufferedImage;

/**
 * Immutable-by-copy state for one Five-ring V2 run.
 *
 * @param phase current phase to execute next.
 * @param round one-based five-ring run number inside this task execution.
 * @param source diagnostic source describing how this state was produced.
 * @param shoeBagIndex remembered main-bag page index for the shoe item; null means unknown.
 * @param shoePurchaseCount number of shoes the BUY_SHOES phase should select before clicking buy.
 * @param taskAccepted true only after this round has been confirmed by the left 五环 tracker title /
 *                     task block. Clicking the accept option alone is unconfirmed, and runner
 *                     not-ready / no-green / no-link statuses are not business proof that the task
 *                     exists.
 * @param trackerPanelRegion cached task-tracker panel region in window-relative pixels.
 * @param wuhuanTrackerBlockRegion cached 五环 task block region in window-relative pixels.
 * @param waitingAcceptNpcPathing true after accept-NPC navigation has yielded while the character is
 *                                walking toward 云游大师; the same ACCEPT_TASK phase should only
 *                                observe movement until this clears.
 * @param pathingStartedAtMs wall-clock timestamp when the current pathing wait started; 0 when not
 *                           currently waiting for pathing.
 * @param pathingIntentExpected true when the previous action published a window-level pathing
 *                              intent and WAIT_PATHING may consume the watcher snapshot.
 * @param pathingIntentSource diagnostic source expected on the active watcher intent; null means no
 *                            watcher intent should be consumed for this wait.
 * @param pathingMovementObserved true after WAIT_PATHING has seen at least one real movement tick.
 * @param combatObservedSincePathing true once combat was seen after the latest tracker/pathing click.
 * @param wuhuanTrackerCombatBaselineImage in-combat snapshot of the cached 五环 tracker task block
 *                                         ROI; null until the window is actually observed in combat.
 * @param wuhuanTrackerCombatBaselineCapturedAtMs wall-clock timestamp for the in-combat tracker ROI
 *                                                baseline; 0 when no baseline is held.
 * @param phaseRetryCount retry count for the current phase after local cleanup/retry.
 * @param uiErrorCount consecutive task-panel/pathing UI errors.
 * @param cleanTransitionStartup true only for the first 五环 run after a clean queued cross-task
 *                               transition; preparation remains required, but handover is skipped.
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
    int shoePurchaseCount;
    boolean taskAccepted;
    OcrWindowRegion trackerPanelRegion;
    OcrWindowRegion wuhuanTrackerBlockRegion;
    boolean waitingAcceptNpcPathing;
    long pathingStartedAtMs;
    boolean pathingIntentExpected;
    String pathingIntentSource;
    boolean pathingMovementObserved;
    boolean combatObservedSincePathing;
    BufferedImage wuhuanTrackerCombatBaselineImage;
    long wuhuanTrackerCombatBaselineCapturedAtMs;
    int phaseRetryCount;
    int uiErrorCount;
    boolean cleanTransitionStartup;

    public static FiveRingPhaseContext start(int round) {
        return new FiveRingPhaseContext(FiveRingPhase.PREPARE, round, "normal-start", null,
                0, false, null, null, false, 0L, false, null, false, false,
                null, 0L, 0, 0, false);
    }

    public static FiveRingPhaseContext cleanTransitionStart(int round) {
        return new FiveRingPhaseContext(FiveRingPhase.PREPARE, round, "clean-transition-start", null,
                0, false, null, null, false, 0L, false, null, false, false,
                null, 0L, 0, 0, true);
    }

    public FiveRingPhaseContext pauseResumeHotStart(String nextSource) {
        return new FiveRingPhaseContext(FiveRingPhase.PREPARE, round, nextSource, null,
                0, false, null, null, false, 0L, false, null, false, false,
                null, 0L, 0, 0,
                cleanTransitionStartup);
    }

    public FiveRingPhaseContext pauseInternalAutomationTimers(long blockedMs, String nextSource) {
        if (blockedMs <= 0L || pathingStartedAtMs <= 0L) {
            return new FiveRingPhaseContext(phase, round, nextSource, shoeBagIndex,
                    shoePurchaseCount, taskAccepted, trackerPanelRegion, wuhuanTrackerBlockRegion,
                    waitingAcceptNpcPathing, pathingStartedAtMs, pathingIntentExpected, pathingIntentSource,
                    pathingMovementObserved, combatObservedSincePathing,
                    wuhuanTrackerCombatBaselineImage, wuhuanTrackerCombatBaselineCapturedAtMs,
                    phaseRetryCount, uiErrorCount, cleanTransitionStartup);
        }
        return new FiveRingPhaseContext(phase, round, nextSource, shoeBagIndex,
                shoePurchaseCount, taskAccepted, trackerPanelRegion, wuhuanTrackerBlockRegion,
                waitingAcceptNpcPathing, pathingStartedAtMs + blockedMs, pathingIntentExpected,
                pathingIntentSource, pathingMovementObserved, combatObservedSincePathing,
                wuhuanTrackerCombatBaselineImage, wuhuanTrackerCombatBaselineCapturedAtMs,
                phaseRetryCount, uiErrorCount, cleanTransitionStartup);
    }

    public FiveRingPhaseContext next(FiveRingPhase nextPhase, String nextSource) {
        flushWuhuanTrackerCombatBaselineIfReplacing(null);
        long nextPathingStartedAtMs = nextPhase == FiveRingPhase.WAIT_PATHING
                ? System.currentTimeMillis()
                : 0L;
        return new FiveRingPhaseContext(nextPhase, round, nextSource, shoeBagIndex,
                shoePurchaseCount, taskAccepted, trackerPanelRegion, wuhuanTrackerBlockRegion, false,
                nextPathingStartedAtMs, false, null, false, false,
                null, 0L, 0, uiErrorCount,
                cleanTransitionStartup);
    }

    public FiveRingPhaseContext nextAfterPreparation(String nextSource) {
        return next(cleanTransitionStartup ? FiveRingPhase.ACCEPT_TASK : FiveRingPhase.HANDOVER_DETECT,
                nextSource);
    }

    public FiveRingPhaseContext withShoeBagIndex(Integer nextShoeBagIndex, String nextSource) {
        return new FiveRingPhaseContext(phase, round, nextSource, nextShoeBagIndex,
                shoePurchaseCount, taskAccepted, trackerPanelRegion, wuhuanTrackerBlockRegion,
                waitingAcceptNpcPathing, pathingStartedAtMs, pathingIntentExpected, pathingIntentSource,
                pathingMovementObserved, combatObservedSincePathing,
                wuhuanTrackerCombatBaselineImage, wuhuanTrackerCombatBaselineCapturedAtMs,
                phaseRetryCount, uiErrorCount, cleanTransitionStartup);
    }

    public FiveRingPhaseContext withShoePurchaseCount(int nextShoePurchaseCount, String nextSource) {
        return new FiveRingPhaseContext(phase, round, nextSource, shoeBagIndex,
                Math.max(0, nextShoePurchaseCount), taskAccepted, trackerPanelRegion, wuhuanTrackerBlockRegion,
                waitingAcceptNpcPathing, pathingStartedAtMs, pathingIntentExpected, pathingIntentSource,
                pathingMovementObserved, combatObservedSincePathing,
                wuhuanTrackerCombatBaselineImage, wuhuanTrackerCombatBaselineCapturedAtMs,
                phaseRetryCount, uiErrorCount, cleanTransitionStartup);
    }

    public FiveRingPhaseContext withTaskAccepted(String nextSource) {
        return new FiveRingPhaseContext(phase, round, nextSource, shoeBagIndex,
                shoePurchaseCount, true, trackerPanelRegion, wuhuanTrackerBlockRegion,
                waitingAcceptNpcPathing, pathingStartedAtMs, pathingIntentExpected, pathingIntentSource,
                pathingMovementObserved, combatObservedSincePathing,
                wuhuanTrackerCombatBaselineImage, wuhuanTrackerCombatBaselineCapturedAtMs,
                phaseRetryCount, uiErrorCount, cleanTransitionStartup);
    }

    public FiveRingPhaseContext withTrackerRegions(OcrWindowRegion nextTrackerPanelRegion,
                                                   OcrWindowRegion nextWuhuanTrackerBlockRegion,
                                                   String nextSource) {
        return new FiveRingPhaseContext(phase, round, nextSource, shoeBagIndex,
                shoePurchaseCount, taskAccepted, nextTrackerPanelRegion, nextWuhuanTrackerBlockRegion,
                waitingAcceptNpcPathing, pathingStartedAtMs, pathingIntentExpected, pathingIntentSource,
                pathingMovementObserved, combatObservedSincePathing,
                wuhuanTrackerCombatBaselineImage, wuhuanTrackerCombatBaselineCapturedAtMs,
                phaseRetryCount, uiErrorCount, cleanTransitionStartup);
    }

    public FiveRingPhaseContext clearWuhuanTrackerBlockRegion(String nextSource) {
        flushWuhuanTrackerCombatBaselineIfReplacing(null);
        return new FiveRingPhaseContext(phase, round, nextSource, shoeBagIndex,
                shoePurchaseCount, taskAccepted, trackerPanelRegion, null,
                waitingAcceptNpcPathing, pathingStartedAtMs, pathingIntentExpected, pathingIntentSource,
                pathingMovementObserved, combatObservedSincePathing,
                null, 0L,
                phaseRetryCount, uiErrorCount, cleanTransitionStartup);
    }

    public FiveRingPhaseContext waitForAcceptNpcPathing(String nextSource, String expectedIntentSource) {
        return new FiveRingPhaseContext(phase, round, nextSource, shoeBagIndex,
                shoePurchaseCount, taskAccepted, trackerPanelRegion, wuhuanTrackerBlockRegion,
                true, System.currentTimeMillis(), true, expectedIntentSource,
                pathingMovementObserved, combatObservedSincePathing,
                wuhuanTrackerCombatBaselineImage, wuhuanTrackerCombatBaselineCapturedAtMs,
                phaseRetryCount, uiErrorCount, cleanTransitionStartup);
    }

    public FiveRingPhaseContext clearAcceptNpcPathingWait(String nextSource) {
        return new FiveRingPhaseContext(phase, round, nextSource, shoeBagIndex,
                shoePurchaseCount, taskAccepted, trackerPanelRegion, wuhuanTrackerBlockRegion,
                false, 0L, false, null, pathingMovementObserved, combatObservedSincePathing,
                wuhuanTrackerCombatBaselineImage, wuhuanTrackerCombatBaselineCapturedAtMs,
                phaseRetryCount, uiErrorCount, cleanTransitionStartup);
    }

    public FiveRingPhaseContext withPathingStarted(String nextSource) {
        return withPathingStarted(nextSource, false, null);
    }

    public FiveRingPhaseContext withWatcherPathingStarted(String nextSource, String expectedIntentSource) {
        return withPathingStarted(nextSource, true, expectedIntentSource);
    }

    public FiveRingPhaseContext withNewWatcherPathingStarted(String nextSource, String expectedIntentSource) {
        return withPathingStarted(nextSource, true, expectedIntentSource, true);
    }

    private FiveRingPhaseContext withPathingStarted(String nextSource,
                                                   boolean nextPathingIntentExpected,
                                                   String nextPathingIntentSource) {
        return withPathingStarted(nextSource, nextPathingIntentExpected, nextPathingIntentSource, false);
    }

    private FiveRingPhaseContext withPathingStarted(String nextSource,
                                                   boolean nextPathingIntentExpected,
                                                   String nextPathingIntentSource,
                                                   boolean forceNewStart) {
        long nextPathingStartedAtMs = pathingStartedAtMs > 0L
                && !forceNewStart
                ? pathingStartedAtMs
                : System.currentTimeMillis();
        return new FiveRingPhaseContext(phase, round, nextSource, shoeBagIndex,
                shoePurchaseCount, taskAccepted, trackerPanelRegion, wuhuanTrackerBlockRegion,
                waitingAcceptNpcPathing, nextPathingStartedAtMs, nextPathingIntentExpected,
                nextPathingIntentSource, false, combatObservedSincePathing,
                wuhuanTrackerCombatBaselineImage, wuhuanTrackerCombatBaselineCapturedAtMs,
                phaseRetryCount, uiErrorCount, cleanTransitionStartup);
    }

    public FiveRingPhaseContext withPathingMovementObserved(String nextSource) {
        return new FiveRingPhaseContext(phase, round, nextSource, shoeBagIndex,
                shoePurchaseCount, taskAccepted, trackerPanelRegion, wuhuanTrackerBlockRegion,
                waitingAcceptNpcPathing, pathingStartedAtMs, pathingIntentExpected, pathingIntentSource,
                true, combatObservedSincePathing,
                wuhuanTrackerCombatBaselineImage, wuhuanTrackerCombatBaselineCapturedAtMs,
                phaseRetryCount, uiErrorCount, cleanTransitionStartup);
    }

    public FiveRingPhaseContext withCombatObservedSincePathing(String nextSource) {
        return new FiveRingPhaseContext(phase, round, nextSource, shoeBagIndex,
                shoePurchaseCount, taskAccepted, trackerPanelRegion, wuhuanTrackerBlockRegion,
                waitingAcceptNpcPathing, pathingStartedAtMs, pathingIntentExpected, pathingIntentSource,
                pathingMovementObserved, true,
                wuhuanTrackerCombatBaselineImage, wuhuanTrackerCombatBaselineCapturedAtMs,
                phaseRetryCount, uiErrorCount, cleanTransitionStartup);
    }

    public FiveRingPhaseContext withWuhuanTrackerCombatBaseline(BufferedImage nextBaselineImage,
                                                               long capturedAtMs,
                                                               String nextSource) {
        flushWuhuanTrackerCombatBaselineIfReplacing(nextBaselineImage);
        return new FiveRingPhaseContext(phase, round, nextSource, shoeBagIndex,
                shoePurchaseCount, taskAccepted, trackerPanelRegion, wuhuanTrackerBlockRegion,
                waitingAcceptNpcPathing, pathingStartedAtMs, pathingIntentExpected, pathingIntentSource,
                pathingMovementObserved, combatObservedSincePathing,
                nextBaselineImage, Math.max(0L, capturedAtMs),
                phaseRetryCount, uiErrorCount, cleanTransitionStartup);
    }

    public FiveRingPhaseContext clearWuhuanTrackerCombatBaseline(String nextSource) {
        flushWuhuanTrackerCombatBaselineIfReplacing(null);
        return new FiveRingPhaseContext(phase, round, nextSource, shoeBagIndex,
                shoePurchaseCount, taskAccepted, trackerPanelRegion, wuhuanTrackerBlockRegion,
                waitingAcceptNpcPathing, pathingStartedAtMs, pathingIntentExpected, pathingIntentSource,
                pathingMovementObserved, combatObservedSincePathing,
                null, 0L,
                phaseRetryCount, uiErrorCount, cleanTransitionStartup);
    }

    public FiveRingPhaseContext retrySamePhase(String nextSource) {
        return new FiveRingPhaseContext(phase, round, nextSource, shoeBagIndex,
                shoePurchaseCount, taskAccepted, trackerPanelRegion, wuhuanTrackerBlockRegion,
                waitingAcceptNpcPathing, pathingStartedAtMs, pathingIntentExpected, pathingIntentSource,
                pathingMovementObserved, combatObservedSincePathing,
                wuhuanTrackerCombatBaselineImage, wuhuanTrackerCombatBaselineCapturedAtMs,
                phaseRetryCount + 1, uiErrorCount, cleanTransitionStartup);
    }

    public FiveRingPhaseContext resetUiErrorCount(String nextSource) {
        return new FiveRingPhaseContext(phase, round, nextSource, shoeBagIndex,
                shoePurchaseCount, taskAccepted, trackerPanelRegion, wuhuanTrackerBlockRegion,
                waitingAcceptNpcPathing, pathingStartedAtMs, pathingIntentExpected, pathingIntentSource,
                pathingMovementObserved, combatObservedSincePathing,
                wuhuanTrackerCombatBaselineImage, wuhuanTrackerCombatBaselineCapturedAtMs,
                phaseRetryCount, 0, cleanTransitionStartup);
    }

    public FiveRingPhaseContext increaseUiErrorCount(String nextSource) {
        return new FiveRingPhaseContext(phase, round, nextSource, shoeBagIndex,
                shoePurchaseCount, taskAccepted, trackerPanelRegion, wuhuanTrackerBlockRegion,
                waitingAcceptNpcPathing, pathingStartedAtMs, pathingIntentExpected, pathingIntentSource,
                pathingMovementObserved, combatObservedSincePathing,
                wuhuanTrackerCombatBaselineImage, wuhuanTrackerCombatBaselineCapturedAtMs,
                phaseRetryCount, uiErrorCount + 1, cleanTransitionStartup);
    }

    private void flushWuhuanTrackerCombatBaselineIfReplacing(BufferedImage nextBaselineImage) {
        if (wuhuanTrackerCombatBaselineImage != null
                && wuhuanTrackerCombatBaselineImage != nextBaselineImage) {
            wuhuanTrackerCombatBaselineImage.flush();
        }
    }
}
