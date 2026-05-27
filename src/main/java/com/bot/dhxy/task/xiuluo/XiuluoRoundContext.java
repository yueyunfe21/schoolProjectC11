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

    public static XiuluoRoundContext start(int round) {
        return new XiuluoRoundContext(XiuluoPhase.PREPARE_ROUND, null, round, "normal-start");
    }

    public XiuluoRoundContext next(XiuluoPhase nextPhase, String nextSource) {
        return new XiuluoRoundContext(nextPhase, objective, round, nextSource);
    }

    public XiuluoRoundContext withObjective(XiuluoPhase nextPhase, NpcTarget nextObjective, String nextSource) {
        return new XiuluoRoundContext(nextPhase, nextObjective, round, nextSource);
    }

}
