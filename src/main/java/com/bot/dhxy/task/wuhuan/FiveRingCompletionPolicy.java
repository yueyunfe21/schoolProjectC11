package com.bot.dhxy.task.wuhuan;

final class FiveRingCompletionPolicy {

    private FiveRingCompletionPolicy() {
    }

    static Decision decide(int configuredRuns, int currentRound, boolean finalTemplateVisible, boolean onceTemplateVisible) {
        if (finalTemplateVisible) {
            return Decision.STOP_ALL_RUNS;
        }
        if (!onceTemplateVisible) {
            return Decision.NO_MATCH;
        }
        if (currentRound > 1) {
            return Decision.NO_MATCH;
        }
        return configuredRuns == 1 ? Decision.STOP_ALL_RUNS : Decision.FINISH_CURRENT_RUN;
    }

    enum Decision {
        NO_MATCH,
        FINISH_CURRENT_RUN,
        STOP_ALL_RUNS
    }
}
