package com.bot.dhxy.task.xiuluo;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.Accessors;

import com.bot.dhxy.model.tasktracker.TaskTrackerPanelReadResult;
import com.bot.dhxy.model.npc.NpcTarget;

import java.awt.Point;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Mutable-by-copy state for one Xiuluo round.
 *
 * @param phase current phase to execute next.
 * @param objective parsed combat target; null before the task target is known.
 * @param objectiveParseFuture accept-time background objective parse result; null unless the
 *                             non-fast route has already captured a story objective snapshot.
 * @param shortcutTrackerParseFuture accept-time background 修罗 tracker read result. This is used
 *                                   only before formal maintenance; maintenance clears it so the
 *                                   shortcut does a fresh tracker read after UI detours.
 * @param preCombatStartedAtMs wall-clock timestamp when this accepted/pre-combat round attempt
 *                             started. It is preserved across same-round retry/recovery jumps so
 *                             the 修罗 watchdog can abandon a stale objective without using the
 *                             return item before task completion.
 * @param round one-based round number.
 * @param source diagnostic source describing how this state was produced.
 * @param waitingPathing true when the previous navigation call already started pathing and this
 *                       phase should observe movement before submitting another navigation command.
 * @param startExitPrepathStarted true when the leader already clicked the start-map exit before
 *                                reading the accepted task objective.
 * @param enteredBattleByXiuluo true only after WAIT_COMBAT sees battle state following this task's
 *                              Xiuluo "看打" option. Clicking the option itself is only a pending
 *                              confirmation because the game can drop or delay that click.
 * @param routeMode current route family for this round; defaults to the existing objective route.
 * @param combatSource source that caused the current battle entry.
 * @param shortcutTrackerDetailPath latest cropped 修罗 tracker detail image used by shortcut mode.
 * @param shortcutTrackerClickX latest screen-absolute tracker green click X in shortcut mode.
 * @param shortcutTrackerClickY latest screen-absolute tracker green click Y in shortcut mode.
 * @param firstTrackerGreenClickAtMs first successful shortcut tracker green click time; this starts
 *                                   the shortcut pre-combat watchdog and must not be reset by re-clicks.
 * @param shortcutTrackerRetryCount retry count for tracker re-read/re-click inside shortcut mode.
 * @param shortcutPathingIntentId optional pathing intent id associated with shortcut waits.
 * @param phaseRetryCount retry count for the current phase after local cleanup.
 * @param enterBattleConfirmRetryCount bounded retry count after a pending "看打" click fails to
 *                                     enter battle, mainly for prepared shortcut dialog re-registration.
 * @param recoveryCount number of broader recovery jumps already used in this round.
 */
@Value
@Builder
@AllArgsConstructor(access = AccessLevel.PUBLIC)
@Accessors(fluent = true)
public class XiuluoRoundContext {
    private static final long PRE_COMBAT_PAUSE_COMPENSATION_THRESHOLD_MS = 500L;

    XiuluoPhase phase;
    NpcTarget objective;
    CompletableFuture<Optional<NpcTarget>> objectiveParseFuture;
    CompletableFuture<TaskTrackerPanelReadResult> shortcutTrackerParseFuture;
    @Builder.Default
    long preCombatStartedAtMs = System.currentTimeMillis();
    int round;
    String source;
    boolean waitingPathing;
    boolean startExitPrepathStarted;
    boolean enteredBattleByXiuluo;
    @Builder.Default
    XiuluoRouteMode routeMode = XiuluoRouteMode.OBJECTIVE_NAVIGATION;
    @Builder.Default
    XiuluoCombatSource combatSource = XiuluoCombatSource.NONE;
    String shortcutTrackerDetailPath;
    Integer shortcutTrackerClickX;
    Integer shortcutTrackerClickY;
    long firstTrackerGreenClickAtMs;
    int shortcutTrackerRetryCount;
    String shortcutPathingIntentId;
    int phaseRetryCount;
    int enterBattleConfirmRetryCount;
    int recoveryCount;

    public static XiuluoRoundContext start(int round) {
        return new XiuluoRoundContext(XiuluoPhase.PREPARE_ROUND, null, null, null, System.currentTimeMillis(), round,
                "normal-start", false, false, false, XiuluoRouteMode.OBJECTIVE_NAVIGATION,
                XiuluoCombatSource.NONE, null, null, null, 0L, 0, null, 0, 0, 0);
    }

    public XiuluoRoundContext next(XiuluoPhase nextPhase, String nextSource) {
        return new XiuluoRoundContext(nextPhase, objective, objectiveParseFuture, shortcutTrackerParseFuture,
                preCombatStartedAtMs, round,
                nextSource, false, startExitPrepathStarted, enteredBattleByXiuluo, routeMode, combatSource,
                shortcutTrackerDetailPath, shortcutTrackerClickX, shortcutTrackerClickY, firstTrackerGreenClickAtMs,
                shortcutTrackerRetryCount, shortcutPathingIntentId, 0, enterBattleConfirmRetryCount, recoveryCount);
    }

    public XiuluoRoundContext withObjective(XiuluoPhase nextPhase, NpcTarget nextObjective, String nextSource) {
        return new XiuluoRoundContext(nextPhase, nextObjective, null, null, preCombatStartedAtMs, round,
                nextSource, false, startExitPrepathStarted, enteredBattleByXiuluo, routeMode, combatSource,
                shortcutTrackerDetailPath, shortcutTrackerClickX, shortcutTrackerClickY, firstTrackerGreenClickAtMs,
                shortcutTrackerRetryCount, shortcutPathingIntentId, 0, enterBattleConfirmRetryCount, recoveryCount);
    }

    public XiuluoRoundContext withObjectiveParseFuture(XiuluoPhase nextPhase,
                                                       CompletableFuture<Optional<NpcTarget>> nextObjectiveParseFuture,
                                                       String nextSource) {
        return new XiuluoRoundContext(nextPhase, objective, nextObjectiveParseFuture, shortcutTrackerParseFuture,
                preCombatStartedAtMs, round,
                nextSource, false, startExitPrepathStarted, enteredBattleByXiuluo, routeMode, combatSource,
                shortcutTrackerDetailPath, shortcutTrackerClickX, shortcutTrackerClickY, firstTrackerGreenClickAtMs,
                shortcutTrackerRetryCount, shortcutPathingIntentId, 0, enterBattleConfirmRetryCount, recoveryCount);
    }

    public XiuluoRoundContext withAcceptParseFutures(
            XiuluoPhase nextPhase,
            CompletableFuture<Optional<NpcTarget>> nextObjectiveParseFuture,
            CompletableFuture<TaskTrackerPanelReadResult> nextShortcutTrackerParseFuture,
            String nextSource) {
        return new XiuluoRoundContext(nextPhase, objective, nextObjectiveParseFuture,
                nextShortcutTrackerParseFuture, preCombatStartedAtMs, round,
                nextSource, false, startExitPrepathStarted, enteredBattleByXiuluo, routeMode, combatSource,
                shortcutTrackerDetailPath, shortcutTrackerClickX, shortcutTrackerClickY, firstTrackerGreenClickAtMs,
                shortcutTrackerRetryCount, shortcutPathingIntentId, 0, enterBattleConfirmRetryCount, recoveryCount);
    }

    public XiuluoRoundContext clearShortcutTrackerParseFuture(XiuluoPhase nextPhase, String nextSource) {
        return new XiuluoRoundContext(nextPhase, objective, objectiveParseFuture, null, preCombatStartedAtMs, round,
                nextSource, false, startExitPrepathStarted, enteredBattleByXiuluo, routeMode, combatSource,
                shortcutTrackerDetailPath, shortcutTrackerClickX, shortcutTrackerClickY, firstTrackerGreenClickAtMs,
                shortcutTrackerRetryCount, shortcutPathingIntentId, 0, enterBattleConfirmRetryCount, recoveryCount);
    }

    public XiuluoRoundContext withStartExitPrepathStarted(XiuluoPhase nextPhase, String nextSource) {
        return new XiuluoRoundContext(nextPhase, objective, objectiveParseFuture, shortcutTrackerParseFuture,
                preCombatStartedAtMs, round,
                nextSource, false, true, enteredBattleByXiuluo, routeMode, combatSource,
                shortcutTrackerDetailPath, shortcutTrackerClickX, shortcutTrackerClickY, firstTrackerGreenClickAtMs,
                shortcutTrackerRetryCount, shortcutPathingIntentId, 0, enterBattleConfirmRetryCount, recoveryCount);
    }

    public XiuluoRoundContext withXiuluoBattleStarted(XiuluoPhase nextPhase, String nextSource) {
        return withCombatSource(nextPhase, XiuluoCombatSource.TRACKER_CONFIRM, nextSource);
    }

    public XiuluoRoundContext withCombatSource(XiuluoPhase nextPhase,
                                               XiuluoCombatSource nextCombatSource,
                                               String nextSource) {
        return new XiuluoRoundContext(nextPhase, objective, objectiveParseFuture, shortcutTrackerParseFuture,
                preCombatStartedAtMs, round,
                nextSource, false, startExitPrepathStarted,
                nextCombatSource != null && nextCombatSource != XiuluoCombatSource.NONE,
                routeMode, nextCombatSource == null ? XiuluoCombatSource.NONE : nextCombatSource,
                shortcutTrackerDetailPath, shortcutTrackerClickX, shortcutTrackerClickY, firstTrackerGreenClickAtMs,
                shortcutTrackerRetryCount, shortcutPathingIntentId, 0, enterBattleConfirmRetryCount, recoveryCount);
    }

    /**
     * Record that a known "看打" option was clicked without treating that click as battle entry.
     *
     * @param nextPhase phase that should observe combat state next.
     * @param nextCombatSource source of the pending battle option click.
     * @param nextSource diagnostic source for the returned copy.
     * @return a copy that keeps the combat source but requires WAIT_COMBAT evidence before marking
     *         the Xiuluo battle as entered.
     */
    public XiuluoRoundContext withPendingEnterBattleConfirm(XiuluoPhase nextPhase,
                                                            XiuluoCombatSource nextCombatSource,
                                                            String nextSource) {
        return new XiuluoRoundContext(nextPhase, objective, objectiveParseFuture, shortcutTrackerParseFuture,
                preCombatStartedAtMs, round,
                nextSource, false, startExitPrepathStarted, false,
                routeMode, nextCombatSource == null ? XiuluoCombatSource.NONE : nextCombatSource,
                shortcutTrackerDetailPath, shortcutTrackerClickX, shortcutTrackerClickY, firstTrackerGreenClickAtMs,
                shortcutTrackerRetryCount, shortcutPathingIntentId, 0, enterBattleConfirmRetryCount, recoveryCount);
    }

    public XiuluoRoundContext incrementEnterBattleConfirmRetry(XiuluoPhase nextPhase, String nextSource) {
        return new XiuluoRoundContext(nextPhase, objective, objectiveParseFuture, shortcutTrackerParseFuture,
                preCombatStartedAtMs, round,
                nextSource, false, startExitPrepathStarted, false,
                routeMode, combatSource, shortcutTrackerDetailPath, shortcutTrackerClickX, shortcutTrackerClickY,
                firstTrackerGreenClickAtMs, shortcutTrackerRetryCount, shortcutPathingIntentId,
                0, enterBattleConfirmRetryCount + 1, recoveryCount);
    }

    public XiuluoRoundContext waitForPathing(String nextSource) {
        return new XiuluoRoundContext(phase, objective, objectiveParseFuture, shortcutTrackerParseFuture,
                preCombatStartedAtMs, round,
                nextSource, true, startExitPrepathStarted, enteredBattleByXiuluo, routeMode, combatSource,
                shortcutTrackerDetailPath, shortcutTrackerClickX, shortcutTrackerClickY, firstTrackerGreenClickAtMs,
                shortcutTrackerRetryCount, shortcutPathingIntentId, phaseRetryCount, enterBattleConfirmRetryCount,
                recoveryCount);
    }

    public XiuluoRoundContext clearPathingWait(String nextSource) {
        return new XiuluoRoundContext(phase, objective, objectiveParseFuture, shortcutTrackerParseFuture,
                preCombatStartedAtMs, round,
                nextSource, false, startExitPrepathStarted, enteredBattleByXiuluo, routeMode, combatSource,
                shortcutTrackerDetailPath, shortcutTrackerClickX, shortcutTrackerClickY, firstTrackerGreenClickAtMs,
                shortcutTrackerRetryCount, shortcutPathingIntentId, phaseRetryCount, enterBattleConfirmRetryCount,
                recoveryCount);
    }

    public XiuluoRoundContext retrySamePhase(String nextSource) {
        return new XiuluoRoundContext(phase, objective, objectiveParseFuture, shortcutTrackerParseFuture,
                preCombatStartedAtMs, round,
                nextSource, false, startExitPrepathStarted, enteredBattleByXiuluo, routeMode, combatSource,
                shortcutTrackerDetailPath, shortcutTrackerClickX, shortcutTrackerClickY, firstTrackerGreenClickAtMs,
                shortcutTrackerRetryCount, shortcutPathingIntentId, phaseRetryCount + 1,
                enterBattleConfirmRetryCount, recoveryCount);
    }

    public XiuluoRoundContext recoverTo(XiuluoPhase nextPhase, String nextSource) {
        return new XiuluoRoundContext(nextPhase, objective, null, null, preCombatStartedAtMs, round,
                nextSource, false, startExitPrepathStarted, enteredBattleByXiuluo,
                XiuluoRouteMode.OBJECTIVE_NAVIGATION, XiuluoCombatSource.NONE,
                null, null, null, 0L, 0, null, 0, 0, recoveryCount + 1);
    }

    public XiuluoRoundContext recoverToWithObjective(XiuluoPhase nextPhase, NpcTarget nextObjective, String nextSource) {
        return new XiuluoRoundContext(nextPhase, nextObjective, null, null, preCombatStartedAtMs, round,
                nextSource, false, startExitPrepathStarted, enteredBattleByXiuluo,
                XiuluoRouteMode.OBJECTIVE_NAVIGATION, XiuluoCombatSource.NONE,
                null, null, null, 0L, 0, null, 0, 0, recoveryCount + 1);
    }

    public XiuluoRoundContext withShortcutTrackerClick(XiuluoPhase nextPhase,
                                                       String detailPath,
                                                       Point clickPoint,
                                                       String pathingIntentId,
                                                       String nextSource) {
        long firstClickAt = firstTrackerGreenClickAtMs > 0L
                ? firstTrackerGreenClickAtMs
                : System.currentTimeMillis();
        long nextPreCombatStartedAtMs = firstTrackerGreenClickAtMs > 0L
                ? preCombatStartedAtMs
                : firstClickAt;
        return new XiuluoRoundContext(nextPhase, objective, objectiveParseFuture, shortcutTrackerParseFuture,
                nextPreCombatStartedAtMs, round,
                nextSource, false, startExitPrepathStarted, enteredBattleByXiuluo,
                XiuluoRouteMode.TRACKER_SHORTCUT, combatSource,
                detailPath,
                clickPoint == null ? null : clickPoint.x,
                clickPoint == null ? null : clickPoint.y,
                firstClickAt, shortcutTrackerRetryCount, pathingIntentId, 0,
                enterBattleConfirmRetryCount, recoveryCount);
    }

    public XiuluoRoundContext incrementShortcutTrackerRetry(XiuluoPhase nextPhase, String nextSource) {
        return new XiuluoRoundContext(nextPhase, objective, objectiveParseFuture, shortcutTrackerParseFuture,
                preCombatStartedAtMs, round,
                nextSource, false, startExitPrepathStarted, enteredBattleByXiuluo,
                routeMode, combatSource, shortcutTrackerDetailPath, shortcutTrackerClickX, shortcutTrackerClickY,
                firstTrackerGreenClickAtMs, shortcutTrackerRetryCount + 1, shortcutPathingIntentId,
                0, enterBattleConfirmRetryCount, recoveryCount);
    }

    public XiuluoRoundContext toObjectiveRoute(XiuluoPhase nextPhase, String nextSource) {
        return new XiuluoRoundContext(nextPhase, objective, objectiveParseFuture, null, preCombatStartedAtMs, round,
                nextSource, false, startExitPrepathStarted, false,
                XiuluoRouteMode.OBJECTIVE_NAVIGATION, XiuluoCombatSource.NONE,
                null, null, null, 0L, 0, null, 0, 0, recoveryCount);
    }

    /**
     * Shift the round-local pre-combat watchdog start after intentional formal maintenance.
     *
     * @param blockedMs maintenance wall-clock duration in milliseconds; values below the
     *                  compensation threshold are ignored so normal tiny checkpoints do not move
     *                  the watchdog.
     * @param nextSource diagnostic source for the returned copy.
     * @return a copy with the watchdog start shifted by {@code blockedMs}, or the same state when
     *         there is no active pre-combat timer to compensate.
     */
    public XiuluoRoundContext pausePreCombatTimer(long blockedMs, String nextSource) {
        if (blockedMs < PRE_COMBAT_PAUSE_COMPENSATION_THRESHOLD_MS || preCombatStartedAtMs <= 0L) {
            return this;
        }
        return new XiuluoRoundContext(phase, objective, objectiveParseFuture, shortcutTrackerParseFuture,
                preCombatStartedAtMs + blockedMs, round,
                nextSource == null || nextSource.isBlank() ? source : nextSource,
                waitingPathing, startExitPrepathStarted, enteredBattleByXiuluo,
                routeMode, combatSource, shortcutTrackerDetailPath, shortcutTrackerClickX, shortcutTrackerClickY,
                firstTrackerGreenClickAtMs, shortcutTrackerRetryCount, shortcutPathingIntentId,
                phaseRetryCount, enterBattleConfirmRetryCount, recoveryCount);
    }

}
