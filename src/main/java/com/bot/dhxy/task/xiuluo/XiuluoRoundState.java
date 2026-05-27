package com.bot.dhxy.task.xiuluo;

/**
 * Mutable-by-copy state for one Xiuluo round.
 *
 * @param phase current phase to execute next.
 * @param objective parsed objective; null before the task target is known.
 * @param round one-based round number.
 * @param source diagnostic source describing how this state was produced.
 */
public record XiuluoRoundState(
        XiuluoPhase phase,
        XiuluoObjective objective,
        int round,
        String source
) {
    public static XiuluoRoundState start(int round) {
        return new XiuluoRoundState(XiuluoPhase.PREPARE_ROUND, null, round, "normal-start");
    }

    public XiuluoRoundState next(XiuluoPhase nextPhase, String nextSource) {
        return new XiuluoRoundState(nextPhase, objective, round, nextSource);
    }

    public XiuluoRoundState withObjective(XiuluoPhase nextPhase, XiuluoObjective nextObjective, String nextSource) {
        return new XiuluoRoundState(nextPhase, nextObjective, round, nextSource);
    }
}
