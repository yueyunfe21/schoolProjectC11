package com.bot.dhxy.task.xiuluo;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.Accessors;

import com.bot.dhxy.model.npc.NpcTarget;

/**
 * Mutable-by-copy state for one Xiuluo round.
 *
 * @param phase current phase to execute next.
 * @param objective parsed combat target; null before the task target is known.
 * @param round one-based round number.
 * @param source diagnostic source describing how this state was produced.
 * @param waitingPathing true when the previous navigation call already started pathing and this
 *                       phase should observe movement before submitting another navigation command.
 * @param enteredBattleByXiuluo true only after this task clicked the Xiuluo "看打" option. Hot
 *                              starts from an already-open battle keep this false until task-panel
 *                              evidence proves the objective is gone.
 * @param phaseRetryCount retry count for the current phase after local cleanup.
 * @param recoveryCount number of broader recovery jumps already used in this round.
 */
@Value
@Builder
@AllArgsConstructor(access = AccessLevel.PUBLIC)
@Accessors(fluent = true)
public class XiuluoRoundContext {
    XiuluoPhase phase;
    NpcTarget objective;
    int round;
    String source;
    boolean waitingPathing;
    boolean enteredBattleByXiuluo;
    int phaseRetryCount;
    int recoveryCount;

    public static XiuluoRoundContext start(int round) {
        return new XiuluoRoundContext(XiuluoPhase.PREPARE_ROUND, null, round, "normal-start", false, false, 0, 0);
    }

    public XiuluoRoundContext next(XiuluoPhase nextPhase, String nextSource) {
        return new XiuluoRoundContext(nextPhase, objective, round, nextSource, false, enteredBattleByXiuluo, 0, recoveryCount);
    }

    public XiuluoRoundContext withObjective(XiuluoPhase nextPhase, NpcTarget nextObjective, String nextSource) {
        return new XiuluoRoundContext(nextPhase, nextObjective, round, nextSource, false, enteredBattleByXiuluo, 0, recoveryCount);
    }

    public XiuluoRoundContext withXiuluoBattleStarted(XiuluoPhase nextPhase, String nextSource) {
        return new XiuluoRoundContext(nextPhase, objective, round, nextSource, false, true, 0, recoveryCount);
    }

    public XiuluoRoundContext waitForPathing(String nextSource) {
        return new XiuluoRoundContext(phase, objective, round, nextSource, true, enteredBattleByXiuluo, phaseRetryCount, recoveryCount);
    }

    public XiuluoRoundContext clearPathingWait(String nextSource) {
        return new XiuluoRoundContext(phase, objective, round, nextSource, false, enteredBattleByXiuluo, phaseRetryCount, recoveryCount);
    }

    public XiuluoRoundContext retrySamePhase(String nextSource) {
        return new XiuluoRoundContext(phase, objective, round, nextSource, false, enteredBattleByXiuluo, phaseRetryCount + 1, recoveryCount);
    }

    public XiuluoRoundContext recoverTo(XiuluoPhase nextPhase, String nextSource) {
        return new XiuluoRoundContext(nextPhase, objective, round, nextSource, false, enteredBattleByXiuluo, 0, recoveryCount + 1);
    }

    public XiuluoRoundContext recoverToWithObjective(XiuluoPhase nextPhase, NpcTarget nextObjective, String nextSource) {
        return new XiuluoRoundContext(nextPhase, nextObjective, round, nextSource, false, enteredBattleByXiuluo, 0, recoveryCount + 1);
    }

}
