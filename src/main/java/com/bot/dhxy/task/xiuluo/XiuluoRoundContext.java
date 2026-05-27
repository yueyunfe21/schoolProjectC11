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

    public static XiuluoRoundContext start(int round) {
        return new XiuluoRoundContext(XiuluoPhase.PREPARE_ROUND, null, round, "normal-start", false);
    }

    public XiuluoRoundContext next(XiuluoPhase nextPhase, String nextSource) {
        return new XiuluoRoundContext(nextPhase, objective, round, nextSource, false);
    }

    public XiuluoRoundContext withObjective(XiuluoPhase nextPhase, NpcTarget nextObjective, String nextSource) {
        return new XiuluoRoundContext(nextPhase, nextObjective, round, nextSource, false);
    }

    public XiuluoRoundContext waitForPathing(String nextSource) {
        return new XiuluoRoundContext(phase, objective, round, nextSource, true);
    }

    public XiuluoRoundContext clearPathingWait(String nextSource) {
        return new XiuluoRoundContext(phase, objective, round, nextSource, false);
    }

}
