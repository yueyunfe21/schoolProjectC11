package com.bot.dhxy.model.pause;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class TaskPauseResumeReconcileResult {
    TaskPauseResumeDecision decision;
    long pauseBlockedMs;
    boolean fingerprintMatched;
    String mismatchReason;
    @Builder.Default
    List<String> compensatedTimers = List.of();
    @Builder.Default
    List<String> clearedVolatileState = List.of();

    public static TaskPauseResumeReconcileResult noPause() {
        return TaskPauseResumeReconcileResult.builder()
                .decision(TaskPauseResumeDecision.CONTINUE_ORIGINAL_PHASE)
                .fingerprintMatched(true)
                .mismatchReason("no-pause")
                .build();
    }

    public static TaskPauseResumeReconcileResult matched(long pauseBlockedMs, List<String> compensatedTimers) {
        return TaskPauseResumeReconcileResult.builder()
                .decision(TaskPauseResumeDecision.CONTINUE_ORIGINAL_PHASE)
                .pauseBlockedMs(Math.max(0L, pauseBlockedMs))
                .fingerprintMatched(true)
                .mismatchReason("matched")
                .compensatedTimers(compensatedTimers == null ? List.of() : List.copyOf(compensatedTimers))
                .build();
    }

    public static TaskPauseResumeReconcileResult fallback(long pauseBlockedMs,
                                                          String mismatchReason,
                                                          List<String> clearedVolatileState) {
        return TaskPauseResumeReconcileResult.builder()
                .decision(TaskPauseResumeDecision.FALLBACK_TASK_HOT_START)
                .pauseBlockedMs(Math.max(0L, pauseBlockedMs))
                .fingerprintMatched(false)
                .mismatchReason(mismatchReason == null || mismatchReason.isBlank() ? "mismatch" : mismatchReason)
                .clearedVolatileState(clearedVolatileState == null ? List.of() : List.copyOf(clearedVolatileState))
                .build();
    }

    public boolean isFallbackTaskHotStart() {
        return decision == TaskPauseResumeDecision.FALLBACK_TASK_HOT_START;
    }
}
