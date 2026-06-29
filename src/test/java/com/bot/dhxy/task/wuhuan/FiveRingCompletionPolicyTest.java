package com.bot.dhxy.task.wuhuan;

public class FiveRingCompletionPolicyTest {

    public static void main(String[] args) {
        assertDecision("single run accepts final template",
                FiveRingCompletionPolicy.Decision.STOP_ALL_RUNS,
                FiveRingCompletionPolicy.decide(1, 1, true, false));
        assertDecision("single run accepts once template",
                FiveRingCompletionPolicy.Decision.STOP_ALL_RUNS,
                FiveRingCompletionPolicy.decide(1, 1, false, true));
        assertDecision("two runs stop on final template during first round",
                FiveRingCompletionPolicy.Decision.STOP_ALL_RUNS,
                FiveRingCompletionPolicy.decide(2, 1, true, false));
        assertDecision("two runs continue after once template during first round",
                FiveRingCompletionPolicy.Decision.FINISH_CURRENT_RUN,
                FiveRingCompletionPolicy.decide(2, 1, false, true));
        assertDecision("two runs ignore stale once template during second round",
                FiveRingCompletionPolicy.Decision.NO_MATCH,
                FiveRingCompletionPolicy.decide(2, 2, false, true));
        assertDecision("unlimited runs continue after once template during first round",
                FiveRingCompletionPolicy.Decision.FINISH_CURRENT_RUN,
                FiveRingCompletionPolicy.decide(0, 1, false, true));
        assertDecision("no template means no completion",
                FiveRingCompletionPolicy.Decision.NO_MATCH,
                FiveRingCompletionPolicy.decide(2, 2, false, false));
    }

    private static void assertDecision(String caseName,
                                       FiveRingCompletionPolicy.Decision expected,
                                       FiveRingCompletionPolicy.Decision actual) {
        if (actual != expected) {
            throw new AssertionError(caseName + ": expected=" + expected + " actual=" + actual);
        }
    }
}
