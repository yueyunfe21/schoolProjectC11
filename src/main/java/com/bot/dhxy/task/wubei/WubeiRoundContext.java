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
 * @param waitingPathing whether this phase already submitted navigation and should wait for it to settle.
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
    boolean waitingPathing;

    public static WubeiRoundContext hotStart(int round) {
        return new WubeiRoundContext(WubeiPhase.HOT_START_DETECT, round, "startup-hot-start", 0, 0, false);
    }

    public static WubeiRoundContext normalStart(int round) {
        return new WubeiRoundContext(WubeiPhase.ACCEPT_TASK, round, "normal-round-start", 0, 0, false);
    }

    public WubeiRoundContext next(WubeiPhase nextPhase, String nextSource) {
        return new WubeiRoundContext(nextPhase, round, nextSource, 0, recoveryCount, false);
    }

    public WubeiRoundContext retrySamePhase(String nextSource) {
        return new WubeiRoundContext(phase, round, nextSource, phaseRetryCount + 1, recoveryCount, waitingPathing);
    }

    public WubeiRoundContext recoverTo(WubeiPhase nextPhase, String nextSource) {
        return new WubeiRoundContext(nextPhase, round, nextSource, 0, recoveryCount + 1, false);
    }

    public WubeiRoundContext waitForPathing(String nextSource) {
        return new WubeiRoundContext(phase, round, nextSource, phaseRetryCount, recoveryCount, true);
    }

    public WubeiRoundContext clearPathingWait(String nextSource) {
        return new WubeiRoundContext(phase, round, nextSource, phaseRetryCount, recoveryCount, false);
    }
}
