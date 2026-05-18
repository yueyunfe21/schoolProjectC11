package com.bot.dhxy.task.wuhuan;

public class FiveRingRuntimeState {

    private Integer shoeBagIndex;
    private FiveRingHandoverState handoverState;
    private boolean needTaskSync = true;
    private int uiErrorCount = 0;
    private FiveRingLoopDecision loopDecision = FiveRingLoopDecision.CONTINUE;

    public Integer getShoeBagIndex() {
        return shoeBagIndex;
    }

    public void setShoeBagIndex(Integer shoeBagIndex) {
        this.shoeBagIndex = shoeBagIndex;
    }

    public FiveRingHandoverState getHandoverState() {
        return handoverState;
    }

    public void setHandoverState(FiveRingHandoverState handoverState) {
        this.handoverState = handoverState;
    }

    public boolean isNeedTaskSync() {
        return needTaskSync;
    }

    public void setNeedTaskSync(boolean needTaskSync) {
        this.needTaskSync = needTaskSync;
    }

    public int getUiErrorCount() {
        return uiErrorCount;
    }

    public void resetUiErrorCount() {
        this.uiErrorCount = 0;
    }

    public int increaseUiErrorCount() {
        uiErrorCount++;
        return uiErrorCount;
    }

    public FiveRingLoopDecision getLoopDecision() {
        return loopDecision;
    }

    public void setLoopDecision(FiveRingLoopDecision loopDecision) {
        this.loopDecision = loopDecision;
    }

    public void resetLoopDecision() {
        this.loopDecision = FiveRingLoopDecision.CONTINUE;
    }
}
