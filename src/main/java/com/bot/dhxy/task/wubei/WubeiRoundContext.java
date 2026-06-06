package com.bot.dhxy.task.wubei;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.Accessors;

/**
 * Mutable-by-copy state for one 五倍 round.
 *
 * @param phase current phase to execute next.
 * @param round one-based round number.
 * @param source diagnostic source describing how this state was produced.
 * @param phaseRetryCount retry count for the current phase after local cleanup.
 * @param recoveryCount number of broad "restart this round from accept task" recoveries.
 */
@Value
@Builder
@AllArgsConstructor(access = AccessLevel.PUBLIC)
@Accessors(fluent = true)
public class WubeiRoundContext {
    WubeiPhase phase;
    int round;
    String source;
    int phaseRetryCount;
    int recoveryCount;

    public static WubeiRoundContext hotStart(int round) {
        return new WubeiRoundContext(WubeiPhase.HOT_START_DETECT, round, "startup-hot-start", 0, 0);
    }

    public static WubeiRoundContext normalStart(int round) {
        return new WubeiRoundContext(WubeiPhase.ACCEPT_TASK, round, "normal-round-start", 0, 0);
    }

    public WubeiRoundContext next(WubeiPhase nextPhase, String nextSource) {
        return new WubeiRoundContext(nextPhase, round, nextSource, 0, recoveryCount);
    }

    public WubeiRoundContext retrySamePhase(String nextSource) {
        return new WubeiRoundContext(phase, round, nextSource, phaseRetryCount + 1, recoveryCount);
    }

    public WubeiRoundContext recoverTo(WubeiPhase nextPhase, String nextSource) {
        return new WubeiRoundContext(nextPhase, round, nextSource, 0, recoveryCount + 1);
    }
}
