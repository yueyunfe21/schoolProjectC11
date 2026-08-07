package com.bot.dhxy.cloud.turn.protocol;

import java.util.List;

/**
 * Exact transport and safety-shell inputs for one NPC arrival-frame FIFO local operation.
 */
public record TurnNpcArrivalFrameFifoSpec(
        String tenantId,
        String deviceId,
        String windowId,
        String hwnd,
        String observationRunId,
        String businessTaskRunId,
        int allowedLeft,
        int allowedTop,
        int allowedWidth,
        int allowedHeight,
        List<String> expectedDialogTemplatePaths,
        String expectedDialogRawTemplatePath,
        boolean deferDialogVerificationToTask,
        boolean consumeStoryDialogVisibleEvents,
        boolean reuseLastVerifiedPoint) {
}
